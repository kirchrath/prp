import { fireEvent, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { describe, expect, it, vi } from 'vitest';
import { apiError, audit, fakeApi, renderWith } from '../test/utils';
import { ArticleModal } from './ArticleModal';

const categories = [{ id: 'c1', name: 'Vorrat' }];
const others = [
  { id: 't1', name: 'Wasser', minStock: 0, status: 'ACTIVE', eans: [] },
  { id: 't2', name: 'Unbekannt (9)', minStock: 0, status: 'UNKNOWN', eans: [] },
] as never[];

function open(api: ReturnType<typeof fakeApi>, existing?: object) {
  const onSaved = vi.fn();
  renderWith(<ArticleModal existing={existing as never} categories={categories} articles={others} onClose={vi.fn()} onSaved={onSaved} />, api);
  return onSaved;
}

describe('ArticleModal', () => {
  it('creates an article with product data from the lookup', async () => {
    const api = fakeApi();
    api.articles.lookupProduct
      .mockResolvedValueOnce({ found: true, name: 'Nudeln', brand: 'Barilla', packageSize: '500 g', imageUrl: 'img' })
      .mockResolvedValueOnce({ found: true })
      .mockResolvedValueOnce({ found: false, existingArticleId: 'other' })
      .mockRejectedValueOnce(apiError(500));
    const onSaved = open(api);
    expect(screen.getByText('Speichern')).toBeDisabled();

    const ean = screen.getByPlaceholderText('EAN hinzufügen');
    await userEvent.type(ean, ' 4001 {Enter}');
    await userEvent.type(ean, '4001');
    await userEvent.click(screen.getByText('Hinzufügen'));
    await userEvent.click(screen.getByText('Hinzufügen'));
    expect(screen.getAllByText('4001')).toHaveLength(1);

    await userEvent.click(screen.getByLabelText('EAN 4001 nachschlagen'));
    expect(await screen.findByDisplayValue('Nudeln')).toBeInTheDocument();
    await userEvent.click(screen.getByLabelText('EAN 4001 nachschlagen')); // found without data keeps the values
    expect(screen.getByDisplayValue('Barilla')).toBeInTheDocument();
    await userEvent.click(screen.getByLabelText('EAN 4001 nachschlagen'));
    expect(await screen.findByText(/gehört bereits/)).toBeInTheDocument();
    expect(screen.getByText('Keine Produktdaten gefunden.')).toBeInTheDocument();
    await userEvent.click(screen.getByLabelText('EAN 4001 nachschlagen'));
    expect(await screen.findByText('Fehler 500')).toBeInTheDocument();

    await userEvent.selectOptions(screen.getByLabelText('Kategorie'), 'c1');
    fireEvent.change(screen.getByLabelText('Mindestbestand'), { target: { value: '2' } });
    await userEvent.click(screen.getByText('Speichern'));
    expect(api.articles.createArticle).toHaveBeenCalledWith({
      articleSaveDto: { data: { name: 'Nudeln', brand: 'Barilla', packageSize: '500 g', imageUrl: 'img', categoryId: 'c1', minStock: 2, eans: ['4001'] } },
    });
    expect(onSaved).toHaveBeenCalled();
  });

  it('updates an article and removes barcodes', async () => {
    const api = fakeApi();
    api.articles.lookupProduct.mockResolvedValue({ found: false, existingArticleId: 'a1' });
    api.articles.updateArticle.mockRejectedValueOnce(apiError(409, 'EAN vergeben'));
    open(api, { id: 'a1', article: { audit: audit(2), data: { name: 'Salz', minStock: 1, eans: ['1', '2'], status: 'ACTIVE' } } });
    expect(screen.getByText('Artikel bearbeiten')).toBeInTheDocument();
    await userEvent.click(screen.getByLabelText('EAN 1 nachschlagen'));
    expect(await screen.findByText('Keine Produktdaten gefunden.')).toBeInTheDocument();
    expect(screen.queryByText(/gehört bereits/)).toBeNull();
    await userEvent.click(screen.getByLabelText('EAN 1 entfernen'));
    await userEvent.type(screen.getByLabelText('Marke'), 'Bad Reichenhaller');
    await userEvent.type(screen.getByLabelText('Packungsgröße'), '1 kg');
    await userEvent.type(screen.getByLabelText('Bild-URL'), 'img');
    await userEvent.click(screen.getByText('Speichern'));
    expect(await screen.findByText('EAN vergeben')).toBeInTheDocument();
    expect(api.articles.updateArticle).toHaveBeenCalledWith({
      id: 'a1',
      articleSaveDto: { audit: audit(2), data: { name: 'Salz', brand: 'Bad Reichenhaller', packageSize: '1 kg', imageUrl: 'img', categoryId: undefined, minStock: 1, eans: ['2'] } },
    });
  });

  it('completes or merges inbox articles', async () => {
    const api = fakeApi();
    const onSaved = open(api, { id: 'u1', article: { audit: audit(0), data: { name: 'Unbekannt (9)', minStock: 0, eans: ['9'], status: 'UNKNOWN' } } });
    expect(screen.getByText('Unbekannten Artikel ergänzen')).toBeInTheDocument();
    expect(screen.getByLabelText('Name')).toHaveValue('');
    await userEvent.type(screen.getByLabelText('Name'), 'Sprudel');
    expect(screen.getByText('Speichern')).toBeEnabled();
    expect(screen.queryByRole('option', { name: 'Unbekannt (9)' })).toBeNull();
    expect(screen.getByText('Zusammenführen')).toBeDisabled();
    await userEvent.selectOptions(screen.getByLabelText('Zielartikel'), 't1');
    await userEvent.click(screen.getByText('Zusammenführen'));
    expect(api.articles.mergeArticle).toHaveBeenCalledWith({ id: 'u1', targetId: 't1', version: 0 });
    expect(onSaved).toHaveBeenCalled();
  });
});
