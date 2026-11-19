import { screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { describe, expect, it } from 'vitest';
import { apiError, audit, fakeApi, loc, renderWith } from '../test/utils';
import { StockPage } from './StockPage';

const day = (offset: number) => {
  const d = new Date();
  return new Date(d.getFullYear(), d.getMonth(), d.getDate() + offset);
};
const batches = [
  { id: 'b1', articleId: 'a1', articleName: 'Milch', articleBrand: 'Weide', locationId: 'k', locationPath: 'Keller', bestBefore: day(-1), quantity: 2 },
  { id: 'b2', articleId: 'a1', articleName: 'Joghurt', locationId: 'k', locationPath: 'Keller', bestBefore: day(3), quantity: 1 },
  { id: 'b3', articleId: 'a2', articleName: 'Reis', locationId: 'k', locationPath: 'Keller', bestBefore: day(30), quantity: 1 },
  { id: 'b4', articleId: 'a3', articleName: 'Salz', locationId: 'k', locationPath: 'Keller', quantity: 5 },
];

describe('StockPage', () => {
  it('lists, filters and books', async () => {
    const api = fakeApi();
    api.locations.listLocations.mockResolvedValue([loc('k', 'Keller')]);
    api.stock.listBatches.mockResolvedValue(batches);
    renderWith(<StockPage />, api);

    expect(await screen.findByText('Milch')).toBeInTheDocument();
    expect(screen.getByText(day(-1).toLocaleDateString('de-DE'))).toHaveClass('is-danger');
    expect(screen.getByText(day(3).toLocaleDateString('de-DE'))).toHaveClass('is-warning');
    expect(screen.getByText(day(30).toLocaleDateString('de-DE'))).toHaveClass('is-light');

    await userEvent.type(screen.getByPlaceholderText('Artikel suchen…'), 'weide');
    expect(screen.queryByText('Reis')).toBeNull();
    await userEvent.clear(screen.getByPlaceholderText('Artikel suchen…'));
    await userEvent.type(screen.getByPlaceholderText('Artikel suchen…'), 'nichts');
    expect(screen.getByText('Kein Bestand gefunden.')).toBeInTheDocument();
    await userEvent.clear(screen.getByPlaceholderText('Artikel suchen…'));

    await userEvent.click(screen.getByLabelText('Salz eins hinzufügen'));
    expect(api.stock.bookIn).toHaveBeenCalledWith({ manualBookingDto: { articleId: 'a3', locationId: 'k', quantity: 1, bestBefore: undefined } });
    await userEvent.click(screen.getByLabelText('Salz eins entnehmen'));
    expect(api.stock.bookOut).toHaveBeenCalled();
    api.stock.bookOut.mockRejectedValueOnce(apiError(409, 'Nicht genug Bestand'));
    await userEvent.click(screen.getByLabelText('Salz eins entnehmen'));
    expect(await screen.findByText('Nicht genug Bestand')).toBeInTheDocument();

    await userEvent.selectOptions(screen.getByLabelText('Lagerort'), 'k');
    await waitFor(() => expect(api.stock.listBatches).toHaveBeenLastCalledWith({ locationId: 'k' }));
  });

  it('edits a batch', async () => {
    const api = fakeApi();
    api.stock.listBatches.mockResolvedValue(batches);
    api.stock.getBatch.mockResolvedValue({ audit: audit(3) });
    renderWith(<StockPage />, api);
    await userEvent.click(await screen.findByLabelText('Salz bearbeiten'));
    await userEvent.click(screen.getByLabelText('Schließen'));
    await userEvent.click(screen.getByLabelText('Salz bearbeiten'));
    await userEvent.click(screen.getByText('MHD speichern'));
    expect(api.stock.assignBestBefore).toHaveBeenCalledWith({ id: 'b4', batchBestBeforeDto: { audit: audit(3), quantity: 5, bestBefore: undefined } });
    expect(await screen.findByText('MHD gespeichert')).toBeInTheDocument();
  });

  it('shows errors', async () => {
    const api = fakeApi();
    api.stock.listBatches.mockRejectedValueOnce(new Error('weg'));
    renderWith(<StockPage />, api);
    await userEvent.click(await screen.findByText('Erneut versuchen'));
    expect(await screen.findByText('Kein Bestand gefunden.')).toBeInTheDocument();
  });
});
