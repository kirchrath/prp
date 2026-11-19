import { fireEvent, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { describe, expect, it } from 'vitest';
import { apiError, fakeApi, renderWith } from '../test/utils';
import { CookPage } from './CookPage';

const suggestions = [
  { recipeId: 'r1', title: 'Pasta', categoryName: 'Hauptgang', totalMinutes: 20, cookable: true, missing: [] },
  {
    recipeId: 'r2', title: 'Lasagne', cookable: false, missing: [
      { name: 'Tomaten', amount: 1000, unit: 'GRAM', articleId: 'a1', articleName: 'Tomaten Dose', packages: 2 },
      { name: 'Basilikum', packages: 1 },
    ],
  },
];

describe('CookPage', () => {
  it('shows what can be cooked and puts missing ingredients on the list', async () => {
    const api = fakeApi();
    api.recipeCategories.listRecipeCategories.mockResolvedValue([{ id: 'c1', name: 'Hauptgang' }]);
    api.cooking.listSuggestions.mockResolvedValue(suggestions);
    renderWith(<CookPage />, api);

    expect(await screen.findByText('kochbar')).toBeInTheDocument();
    expect(screen.getByText('Hauptgang · 20 min')).toBeInTheDocument();
    expect(screen.getByText('2 fehlt')).toBeInTheDocument();
    expect(screen.getByText(/Tomaten 1.000 g/)).toBeInTheDocument();
    expect(screen.getByText('· 2× Tomaten Dose')).toBeInTheDocument();
    expect(api.cooking.listSuggestions).toHaveBeenCalledWith({ servings: 2, categoryId: undefined });

    fireEvent.change(screen.getByLabelText('Personen'), { target: { value: '4' } });
    await userEvent.selectOptions(screen.getByLabelText('Rezeptkategorie'), 'c1');
    await waitFor(() => expect(api.cooking.listSuggestions).toHaveBeenLastCalledWith({ servings: 4, categoryId: 'c1' }));
    fireEvent.change(screen.getByLabelText('Personen'), { target: { value: '0' } });
    await waitFor(() => expect(api.cooking.listSuggestions).toHaveBeenLastCalledWith({ servings: 1, categoryId: 'c1' }));
    fireEvent.change(screen.getByLabelText('Personen'), { target: { value: '4' } });

    await userEvent.click(await screen.findByText('Fehlendes auf die Einkaufsliste'));
    expect(api.cooking.createShoppingRequest).toHaveBeenCalledWith({ id: 'r2', servings: 4 });
    expect(await screen.findByText('Fehlendes für Lasagne steht auf der Einkaufsliste')).toBeInTheDocument();
    api.cooking.createShoppingRequest.mockRejectedValueOnce(apiError(409, 'Für Lasagne fehlt nichts'));
    await userEvent.click(screen.getByText('Fehlendes auf die Einkaufsliste'));
    expect(await screen.findByText('Für Lasagne fehlt nichts')).toBeInTheDocument();
  });

  it('shows empty state and errors', async () => {
    const api = fakeApi();
    api.cooking.listSuggestions.mockRejectedValueOnce(new Error('weg'));
    renderWith(<CookPage />, api);
    await userEvent.click(await screen.findByText('Erneut versuchen'));
    expect(await screen.findByText('Noch keine Rezepte vorhanden.')).toBeInTheDocument();
  });
});
