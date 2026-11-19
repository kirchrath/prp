import { fireEvent, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { describe, expect, it, vi } from 'vitest';
import { apiError, audit, fakeApi, renderWith } from '../test/utils';
import { RecipeEditor } from './RecipeEditor';

const categories = [{ id: 'c1', name: 'Hauptgang' }];
const articles = [{ id: 'a1', name: 'Spaghetti', minStock: 0, status: 'ACTIVE', eans: [] }] as never[];
const data = {
  title: 'Pasta', description: 'Schnell', servings: 2, prepMinutes: 5, cookMinutes: 10, imageUrl: 'img',
  sourceUrl: 'https://rezepte.test/p', categoryId: 'c1', tags: ['schnell'], steps: ['Kochen', 'Essen'],
  ingredients: [
    { name: 'Spaghetti', amount: 250, unit: 'GRAM', articleId: 'a1', basic: false },
    { name: 'Salz', basic: true },
  ],
};

function open(api: ReturnType<typeof fakeApi>, props: object) {
  const onSaved = vi.fn();
  renderWith(<RecipeEditor categories={categories} articles={articles} onClose={vi.fn()} onSaved={onSaved} {...props} />, api);
  return onSaved;
}

describe('RecipeEditor', () => {
  it('creates a new recipe from scratch', async () => {
    const api = fakeApi();
    const onSaved = open(api, {});
    expect(screen.getByText('Neues Rezept')).toBeInTheDocument();
    expect(screen.getByText('Speichern')).toBeDisabled();
    await userEvent.type(screen.getByLabelText('Titel'), 'Salat');
    fireEvent.change(screen.getByLabelText('Personen'), { target: { value: '0' } });
    expect(screen.getByText('Speichern')).toBeDisabled();
    fireEvent.change(screen.getByLabelText('Personen'), { target: { value: '3' } });
    await userEvent.click(screen.getByText('+ Zutat'));
    await userEvent.click(screen.getByText('+ Zutat'));
    await userEvent.type(screen.getByLabelText('Zutat 1'), 'Gurke');
    await userEvent.type(screen.getByLabelText('Menge 1'), '1');
    await userEvent.selectOptions(screen.getByLabelText('Einheit 1'), 'PIECE');
    await userEvent.type(screen.getByLabelText('Zutat 2'), 'Essig');
    await userEvent.selectOptions(screen.getByLabelText('Einheit 2'), 'TABLESPOON'); // without amount: no unit
    await userEvent.click(screen.getByText('+ Zutat')); // empty row is ignored
    await userEvent.click(screen.getByText('Speichern'));
    expect(api.recipes.createRecipe).toHaveBeenCalledWith({
      recipeSaveDto: {
        data: {
          title: 'Salat', description: undefined, servings: 3, prepMinutes: undefined, cookMinutes: undefined,
          imageUrl: undefined, sourceUrl: undefined, categoryId: undefined, tags: [], steps: [],
          ingredients: [
            { name: 'Gurke', amount: 1, unit: 'PIECE', articleId: undefined, basic: false },
            { name: 'Essig', amount: undefined, unit: undefined, articleId: undefined, basic: false },
          ],
        },
      },
    });
    expect(onSaved).toHaveBeenCalled();
  });

  it('edits an imported draft', async () => {
    const api = fakeApi();
    api.recipes.createRecipe.mockRejectedValueOnce(apiError(404, 'Kategorie fehlt'));
    open(api, { draft: data });
    expect(screen.getByText('https://rezepte.test/p')).toBeInTheDocument();
    expect(screen.getByLabelText('Zutat 1')).toHaveValue('Spaghetti');
    expect(screen.getByLabelText('Artikel 1')).toHaveValue('a1');
    await userEvent.click(screen.getAllByRole('checkbox')[0]);
    await userEvent.click(screen.getByLabelText('Zutat 2 entfernen'));
    await userEvent.selectOptions(screen.getByLabelText('Artikel 1'), '');
    await userEvent.clear(screen.getByLabelText('Tags'));
    await userEvent.type(screen.getByLabelText('Tags'), 'schnell, , pasta');
    await userEvent.clear(screen.getByLabelText('Zubereitung'));
    await userEvent.type(screen.getByLabelText('Zubereitung'), 'Kochen{Enter}{Enter}Essen');
    await userEvent.clear(screen.getByLabelText('Vorbereitung (min)'));
    await userEvent.type(screen.getByLabelText('Kochen/Backen (min)'), '5');
    await userEvent.clear(screen.getByLabelText('Beschreibung'));
    await userEvent.clear(screen.getByLabelText('Bild-URL'));
    await userEvent.selectOptions(screen.getByLabelText('Kategorie'), '');
    await userEvent.click(screen.getByText('Speichern'));
    expect(await screen.findByText('Kategorie fehlt')).toBeInTheDocument();
    expect(api.recipes.createRecipe).toHaveBeenCalledWith({
      recipeSaveDto: {
        data: {
          title: 'Pasta', description: undefined, servings: 2, prepMinutes: undefined, cookMinutes: 105, imageUrl: undefined,
          sourceUrl: 'https://rezepte.test/p', categoryId: undefined, tags: ['schnell', 'pasta'], steps: ['Kochen', 'Essen'],
          ingredients: [{ name: 'Spaghetti', amount: 250, unit: 'GRAM', articleId: undefined, basic: true }],
        },
      },
    });
  });

  it('updates and deletes a saved recipe', async () => {
    const api = fakeApi();
    const onSaved = open(api, { existing: { id: 'r1', recipe: { audit: audit(3), data: { ...data, sourceUrl: undefined } } } });
    expect(screen.getByText('Rezept bearbeiten')).toBeInTheDocument();
    expect(screen.queryByText(/Quelle/)).toBeNull();
    await userEvent.click(screen.getByText('Speichern'));
    expect(api.recipes.updateRecipe).toHaveBeenCalledWith(expect.objectContaining({ id: 'r1' }));
    const confirm = vi.spyOn(window, 'confirm').mockReturnValueOnce(false).mockReturnValue(true);
    await userEvent.click(screen.getByText('Löschen'));
    expect(api.recipes.deleteRecipe).not.toHaveBeenCalled();
    await userEvent.click(screen.getByText('Löschen'));
    expect(api.recipes.deleteRecipe).toHaveBeenCalledWith({ id: 'r1', version: 3 });
    expect(onSaved).toHaveBeenCalledTimes(2);
    expect(confirm).toHaveBeenCalledTimes(2);
  });
});
