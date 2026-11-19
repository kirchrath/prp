import { screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { describe, expect, it, vi } from 'vitest';
import { apiError, fakeApi, renderWith } from '../test/utils';
import { ImportModal } from './ImportModal';

describe('ImportModal', () => {
  it('imports from a link or a text', async () => {
    const api = fakeApi();
    const draft = { title: 'Lasagne' };
    api.recipeImports.importRecipeFromUrl.mockRejectedValueOnce(apiError(502, 'Seite nicht erreichbar')).mockResolvedValue(draft);
    api.recipeImports.importRecipeFromText.mockResolvedValue(draft);
    const onDraft = vi.fn();
    renderWith(<ImportModal onClose={vi.fn()} onDraft={onDraft} />, api);

    expect(screen.getByText('Importieren')).toBeDisabled();
    await userEvent.type(screen.getByLabelText('Link zum Rezept'), ' https://rezepte.test/l ');
    await userEvent.click(screen.getByText('Importieren'));
    expect(await screen.findByText('Seite nicht erreichbar')).toBeInTheDocument();
    await userEvent.click(screen.getByText('Importieren'));
    expect(api.recipeImports.importRecipeFromUrl).toHaveBeenLastCalledWith({ urlImportDto: { url: 'https://rezepte.test/l' } });
    expect(onDraft).toHaveBeenCalledWith(draft);

    await userEvent.click(screen.getByText('Freitext'));
    await userEvent.type(screen.getByLabelText('Rezepttext'), '200 g Mehl');
    await userEvent.click(screen.getByText('Importieren'));
    expect(api.recipeImports.importRecipeFromText).toHaveBeenCalledWith({ textImportDto: { text: '200 g Mehl' } });
    await userEvent.click(screen.getByText('Link'));
    expect(screen.getByLabelText('Link zum Rezept')).toHaveValue('https://rezepte.test/l');
  });

  it('shows progress while the AI works', async () => {
    const api = fakeApi();
    api.recipeImports.importRecipeFromText.mockReturnValue(new Promise(() => undefined));
    renderWith(<ImportModal onClose={vi.fn()} onDraft={vi.fn()} />, api);
    await userEvent.click(screen.getByText('Freitext'));
    await userEvent.type(screen.getByLabelText('Rezepttext'), 'x');
    await userEvent.click(screen.getByText('Importieren'));
    expect(screen.getByText(/einige Sekunden/)).toBeInTheDocument();
    expect(screen.getByText('Importieren')).toBeDisabled();
  });
});
