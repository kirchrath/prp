import { screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { describe, expect, it } from 'vitest';
import { apiError, audit, fakeApi, renderWith } from '../test/utils';
import { RecipesPage } from './RecipesPage';

const recipes = [
  { id: 'r1', title: 'Pasta', imageUrl: 'img', categoryName: 'Hauptgang', tags: ['schnell'], servings: 2, totalMinutes: 20 },
  { id: 'r2', title: 'Tee', tags: [], servings: 1 },
];

describe('RecipesPage', () => {
  it('lists, filters, opens and imports recipes', async () => {
    const api = fakeApi();
    api.recipes.listRecipes.mockResolvedValue(recipes);
    api.recipes.listRecipeTags.mockResolvedValue(['schnell']);
    api.recipeCategories.listRecipeCategories.mockResolvedValue([{ id: 'c1', name: 'Hauptgang' }]);
    api.recipes.getRecipe.mockResolvedValue({ audit: audit(1), data: { title: 'Pasta', servings: 2, tags: [], steps: [], ingredients: [] } });
    api.recipeImports.importRecipeFromUrl.mockResolvedValue({ title: 'Importiert', servings: 4, tags: [], steps: [], ingredients: [] });
    renderWith(<RecipesPage />, api);

    expect(await screen.findByText('Hauptgang · 2 Pers. · 20 min')).toBeInTheDocument();
    expect(screen.getByText('1 Pers.')).toBeInTheDocument();
    await userEvent.type(screen.getByPlaceholderText('Rezept oder Zutat suchen…'), 'x');
    await userEvent.selectOptions(screen.getByLabelText('Rezeptkategorie'), 'c1');
    await userEvent.selectOptions(screen.getByLabelText('Tag'), 'schnell');
    await waitFor(() => expect(api.recipes.listRecipes).toHaveBeenLastCalledWith({ search: 'x', categoryId: 'c1', tag: 'schnell' }));

    await userEvent.click(screen.getByRole('button', { name: 'Pasta' }));
    expect(await screen.findByText('Rezept bearbeiten')).toBeInTheDocument();
    await userEvent.click(screen.getByText('Speichern'));
    await waitFor(() => expect(screen.queryByText('Rezept bearbeiten')).toBeNull());

    api.recipes.getRecipe.mockRejectedValueOnce(apiError(404, 'Rezept weg'));
    await userEvent.click(screen.getByRole('button', { name: 'Tee' }));
    expect(await screen.findByText('Rezept weg')).toBeInTheDocument();

    await userEvent.click(screen.getByText('+ Neues Rezept'));
    expect(screen.getByText('Neues Rezept')).toBeInTheDocument();
    await userEvent.click(screen.getByText('Abbrechen'));

    await userEvent.click(screen.getByRole('button', { name: 'Importieren' }));
    await userEvent.click(screen.getByText('Abbrechen'));
    await userEvent.click(screen.getByRole('button', { name: 'Importieren' }));
    await userEvent.type(screen.getByLabelText('Link zum Rezept'), 'https://rezepte.test/x');
    await userEvent.click(screen.getAllByRole('button', { name: 'Importieren' })[1]);
    expect(await screen.findByDisplayValue('Importiert')).toBeInTheDocument();
  });

  it('shows empty list and errors', async () => {
    const api = fakeApi();
    api.recipes.listRecipes.mockRejectedValueOnce(new Error('weg'));
    renderWith(<RecipesPage />, api);
    await userEvent.click(await screen.findByText('Erneut versuchen'));
    expect(await screen.findByText(/Keine Rezepte gefunden/)).toBeInTheDocument();
  });

  it('opens the editor while side lists are still loading', async () => {
    const api = fakeApi();
    const never = () => new Promise(() => undefined);
    api.recipeCategories.listRecipeCategories.mockImplementation(never);
    api.articles.listArticles.mockImplementation(never);
    renderWith(<RecipesPage />, api);
    await userEvent.click(screen.getByText('+ Neues Rezept'));
    await userEvent.click(screen.getByText('+ Zutat'));
    expect(screen.getByRole('option', { name: '– kein Artikel –' })).toBeInTheDocument();
  });
});
