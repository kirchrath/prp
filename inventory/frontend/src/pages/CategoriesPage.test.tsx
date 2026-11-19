import { screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { describe, expect, it, vi } from 'vitest';
import { apiError, audit, fakeApi, renderWith } from '../test/utils';
import { CategoriesPage } from './CategoriesPage';

describe('CategoriesPage', () => {
  it('creates, renames and deletes categories', async () => {
    const api = fakeApi();
    api.categories.listCategories.mockResolvedValue([{ id: 'c1', name: 'Vorrat' }, { id: 'c2', name: 'Bad' }]);
    api.categories.getCategory.mockResolvedValue({ audit: audit(1), data: { name: 'Vorrat' } });
    renderWith(<CategoriesPage />, api);
    expect(await screen.findByText('Vorrat')).toBeInTheDocument();

    expect(screen.getByText('Anlegen')).toBeDisabled();
    await userEvent.type(screen.getByPlaceholderText(/Neue Kategorie/), 'Putzmittel{Enter}');
    expect(api.categories.createCategory).toHaveBeenCalledWith({ categorySaveDto: { data: { name: 'Putzmittel' } } });
    expect(screen.getByPlaceholderText(/Neue Kategorie/)).toHaveValue('');

    await userEvent.click(screen.getByLabelText('Vorrat umbenennen'));
    await userEvent.clear(screen.getByLabelText('Neuer Name'));
    await userEvent.type(screen.getByLabelText('Neuer Name'), 'Lebensmittel');
    await userEvent.click(screen.getByText('OK'));
    expect(api.categories.updateCategory).toHaveBeenCalledWith({ id: 'c1', categorySaveDto: { audit: audit(1), data: { name: 'Lebensmittel' } } });
    expect(screen.queryByLabelText('Neuer Name')).toBeNull();

    api.categories.createCategory.mockRejectedValueOnce(apiError(409, 'existiert bereits'));
    await userEvent.type(screen.getByPlaceholderText(/Neue Kategorie/), 'Bad{Enter}');
    expect(await screen.findByText('existiert bereits')).toBeInTheDocument();

    vi.spyOn(window, 'confirm').mockReturnValueOnce(false).mockReturnValue(true);
    await userEvent.click(screen.getByLabelText('Bad löschen'));
    expect(api.categories.deleteCategory).not.toHaveBeenCalled();
    await userEvent.click(screen.getByLabelText('Bad löschen'));
    expect(api.categories.deleteCategory).toHaveBeenCalledWith({ id: 'c2', version: 1 });
  });

  it('shows empty list and errors', async () => {
    const api = fakeApi();
    api.categories.listCategories.mockRejectedValueOnce(new Error('weg'));
    renderWith(<CategoriesPage />, api);
    await userEvent.click(await screen.findByText('Erneut versuchen'));
    expect(await screen.findByText('Noch keine Kategorien.')).toBeInTheDocument();
  });
});
