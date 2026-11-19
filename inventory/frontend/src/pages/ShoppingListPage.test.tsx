import { fireEvent, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { describe, expect, it } from 'vitest';
import { apiError, audit, fakeApi, renderWith } from '../test/utils';
import { ShoppingListPage } from './ShoppingListPage';

const items = [
  { id: 'i1', articleId: 'a1', label: 'Spülmittel', brand: 'Pril', quantity: 2, automatic: true, done: false },
  { id: 'i2', label: 'Brot', quantity: 1, automatic: false, done: true, note: 'für Toast (2 Pers.)' },
];

describe('ShoppingListPage', () => {
  it('adds, toggles and removes entries', async () => {
    const api = fakeApi();
    api.shopping.listShoppingItems.mockResolvedValue(items);
    api.shopping.getShoppingItem.mockResolvedValue({ audit: audit(1), data: { text: 'Brot', quantity: 1, automatic: false, done: true } });
    renderWith(<ShoppingListPage />, api);

    expect(await screen.findByText('Spülmittel')).toBeInTheDocument();
    expect(screen.getByText('auto')).toBeInTheDocument();
    expect(screen.getByText('· für Toast (2 Pers.)')).toBeInTheDocument();
    expect(screen.getByText('Brot')).toHaveClass('has-text-grey');

    fireEvent.change(screen.getByLabelText('Menge'), { target: { value: '3' } });
    await userEvent.type(screen.getByPlaceholderText('Eintrag hinzufügen…'), ' Milch {Enter}');
    expect(api.shopping.createShoppingItem).toHaveBeenCalledWith({
      shoppingItemSaveDto: { data: { text: 'Milch', quantity: 3, automatic: false, done: false } },
    });
    expect(screen.getByPlaceholderText('Eintrag hinzufügen…')).toHaveValue('');

    await userEvent.click(screen.getAllByRole('checkbox')[1]);
    expect(api.shopping.updateShoppingItem).toHaveBeenCalledWith({
      id: 'i2', shoppingItemSaveDto: { audit: audit(1), data: { text: 'Brot', quantity: 1, automatic: false, done: false } },
    });
    await userEvent.click(screen.getByLabelText('Brot entfernen'));
    expect(api.shopping.deleteShoppingItem).toHaveBeenCalledWith({ id: 'i2', version: 1 });
    await userEvent.click(screen.getByText('Erledigte entfernen'));
    expect(api.shopping.deleteDoneShoppingItems).toHaveBeenCalled();

    api.shopping.deleteDoneShoppingItems.mockRejectedValueOnce(apiError(500));
    await userEvent.click(screen.getByText('Erledigte entfernen'));
    expect(await screen.findByText('Fehler 500')).toBeInTheDocument();
  });

  it('shows empty list and errors', async () => {
    const api = fakeApi();
    api.shopping.listShoppingItems.mockRejectedValueOnce(new Error('weg'));
    renderWith(<ShoppingListPage />, api);
    expect(screen.getByText('Erledigte entfernen')).toBeDisabled();
    await userEvent.click(await screen.findByText('Erneut versuchen'));
    await waitFor(() => expect(screen.getByText('Die Einkaufsliste ist leer.')).toBeInTheDocument());
  });
});
