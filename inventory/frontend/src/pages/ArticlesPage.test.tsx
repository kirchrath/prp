import { screen, waitFor, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { describe, expect, it, vi } from 'vitest';
import { apiError, audit, fakeApi, loc, renderWith } from '../test/utils';
import { ArticlesPage } from './ArticlesPage';

const list = [
  { id: 'a1', name: 'Spaghetti', brand: 'Barilla', packageSize: '500 g', imageUrl: 'img', categoryId: 'c1', categoryName: 'Vorrat', minStock: 3, status: 'ACTIVE', eans: ['111'] },
  { id: 'a2', name: 'Unbekannt (222)', minStock: 0, status: 'UNKNOWN', eans: ['222'] },
  { id: 'a3', name: 'Salz', minStock: 1, status: 'ACTIVE', eans: [] },
];
const detail = (id: string) => ({ audit: audit(4), data: { ...list.find((a) => a.id === id)! } });

function setup() {
  const api = fakeApi();
  api.articles.listArticles.mockResolvedValue(list);
  api.articles.getArticle.mockImplementation(async ({ id }: { id: string }) => detail(id));
  api.categories.listCategories.mockResolvedValue([{ id: 'c1', name: 'Vorrat' }]);
  api.locations.listLocations.mockResolvedValue([loc('k', 'Keller')]);
  api.stock.listLevels.mockResolvedValue([{ articleId: 'a1', quantity: 2 }, { articleId: 'a3', quantity: 4 }]);
  return api;
}

describe('ArticlesPage', () => {
  it('lists articles with stock and filters', async () => {
    const api = setup();
    renderWith(<ArticlesPage />, api);
    const row = (await screen.findByText('Spaghetti')).closest('tr')!;
    expect(within(row).getByText('2').closest('.tag')).toHaveClass('is-danger');
    expect(within(row).getByText('Barilla · 500 g · 111')).toBeInTheDocument();
    expect(within(screen.getByText('Salz').closest('tr')!).getByText('4').closest('.tag')).not.toHaveClass('is-danger');
    const unknownRow = screen.getByText('Unbekannt (222)').closest('tr')!;
    expect(within(unknownRow).getByText('Inbox')).toBeInTheDocument();
    expect(within(unknownRow).getByText('Auslagern')).toBeDisabled();

    await userEvent.type(screen.getByPlaceholderText('Name, Marke oder EAN…'), 'x');
    await userEvent.selectOptions(screen.getByLabelText('Kategorie'), 'c1');
    await waitFor(() => expect(api.articles.listArticles).toHaveBeenLastCalledWith({ search: 'x', categoryId: 'c1', status: undefined }));

    await userEvent.click(screen.getByText('Inbox (unbekannt)'));
    await waitFor(() => expect(api.articles.listArticles).toHaveBeenLastCalledWith({ search: 'x', categoryId: 'c1', status: 'UNKNOWN' }));
    await userEvent.click(screen.getByText('Alle'));
    await waitFor(() => expect(api.articles.listArticles).toHaveBeenLastCalledWith({ search: 'x', categoryId: 'c1', status: undefined }));
  });

  it('shows empty lists, inbox and errors', async () => {
    const api = fakeApi();
    // first call: all articles (merge targets), second call: the filtered list
    api.articles.listArticles.mockRejectedValueOnce(new Error('weg')).mockRejectedValueOnce(new Error('weg'));
    renderWith(<ArticlesPage />, api, '/artikel?status=UNKNOWN');
    await userEvent.click(await screen.findByText('Erneut versuchen'));
    expect(await screen.findByText('Die Inbox ist leer. 🎉')).toBeInTheDocument();
    renderWith(<ArticlesPage />, fakeApi());
    expect(await screen.findByText('Keine Artikel gefunden.')).toBeInTheDocument();
  });

  it('creates, edits, books and deletes', async () => {
    const api = setup();
    renderWith(<ArticlesPage />, api);
    await screen.findByText('Spaghetti');

    await userEvent.click(screen.getByText('+ Neuer Artikel'));
    expect(screen.getByText('Neuer Artikel')).toBeInTheDocument();
    await userEvent.click(screen.getByText('Abbrechen'));

    await userEvent.click(screen.getByRole('button', { name: 'Salz' }));
    expect(screen.getByDisplayValue('Salz')).toBeInTheDocument();
    await userEvent.click(screen.getByText('Speichern'));
    await waitFor(() => expect(api.articles.updateArticle).toHaveBeenCalled());
    expect(screen.queryByText('Artikel bearbeiten')).toBeNull();

    // the inbox modal offers all other articles as merge target
    await userEvent.click(screen.getByRole('button', { name: 'Unbekannt (222)' }));
    expect(within(screen.getByLabelText('Zielartikel')).queryByText('Unbekannt (222)')).toBeNull();
    await userEvent.click(screen.getByText('Abbrechen'));

    api.articles.getArticle.mockRejectedValueOnce(apiError(404, 'Artikel weg'));
    await userEvent.click(screen.getByRole('button', { name: 'Salz' }));
    expect(await screen.findByText('Artikel weg')).toBeInTheDocument();

    await userEvent.click(within(screen.getByText('Salz').closest('tr')!).getByText('Einlagern'));
    await userEvent.click(screen.getByText('Abbrechen'));
    await userEvent.click(within(screen.getByText('Salz').closest('tr')!).getByText('Auslagern'));
    await userEvent.selectOptions(screen.getByLabelText('Lagerort'), 'k');
    await userEvent.click(within(document.querySelector('.modal-card-foot') as HTMLElement).getByText('Auslagern'));
    expect(api.stock.bookOut).toHaveBeenCalled();

    const confirm = vi.spyOn(window, 'confirm').mockReturnValueOnce(false).mockReturnValue(true);
    await userEvent.click(screen.getByLabelText('Salz löschen'));
    expect(api.articles.deleteArticle).not.toHaveBeenCalled();
    await userEvent.click(screen.getByLabelText('Salz löschen'));
    expect(api.articles.deleteArticle).toHaveBeenCalledWith({ id: 'a3', version: 4 });
    expect(await screen.findByText('Artikel gelöscht')).toBeInTheDocument();
    api.articles.deleteArticle.mockRejectedValueOnce(apiError(409, 'Konflikt'));
    await userEvent.click(screen.getByLabelText('Salz löschen'));
    expect(await screen.findByText('Konflikt')).toBeInTheDocument();
    expect(confirm).toHaveBeenCalledTimes(3);
  });

  it('opens dialogs while side lists are still loading', async () => {
    const api = fakeApi();
    const never = () => new Promise(() => undefined);
    api.categories.listCategories.mockImplementation(never);
    api.locations.listLocations.mockImplementation(never);
    api.articles.listArticles.mockImplementationOnce(never).mockResolvedValue(list);
    renderWith(<ArticlesPage />, api);
    await userEvent.click(await screen.findByText('+ Neuer Artikel'));
    expect(screen.getByLabelText('Kategorie', { selector: '.modal select' })).toBeInTheDocument();
    await userEvent.click(screen.getByText('Abbrechen'));
    await userEvent.click(within(screen.getByText('Salz').closest('tr')!).getByText('Einlagern'));
    expect(screen.getByRole('option', { name: 'Bitte wählen…' })).toBeInTheDocument();
  });
});
