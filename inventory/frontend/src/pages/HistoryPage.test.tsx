import { screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { describe, expect, it } from 'vitest';
import { fakeApi, loc, renderWith } from '../test/utils';
import { HistoryPage } from './HistoryPage';

const movement = (id: string, extra: object) => ({
  id, createdDate: new Date(2026, 10, 19, 8, 30), articleId: 'a', articleName: `Artikel ${id}`, locationId: 'k',
  locationPath: 'Keller', delta: 1, type: 'IN', source: 'UI', ...extra,
});

describe('HistoryPage', () => {
  it('lists movements with source and pages', async () => {
    const api = fakeApi();
    api.locations.listLocations.mockResolvedValue([loc('k', 'Keller')]);
    api.scanners.listScanners.mockResolvedValue([{ id: 's1', deviceId: 'scanner-1', name: 'Vorrat' }, { id: 's2', deviceId: 'scanner-2' }]);
    api.movements.listMovements.mockResolvedValue({
      items: [
        movement('1', { source: 'SCANNER', scannerId: 's1', bestBefore: new Date(2027, 0, 1) }),
        movement('2', { source: 'SCANNER', scannerId: 's2', delta: -2, type: 'OUT' }),
        movement('3', { username: 'alice', type: 'CORRECTION' }),
        movement('4', { source: 'SYSTEM', type: 'MERGE' }),
      ],
      page: 0, size: 50, totalElements: 120, totalPages: 3,
    });
    renderWith(<HistoryPage />, api);
    expect(await screen.findByText('· Vorrat')).toBeInTheDocument();
    expect(screen.getByText('· scanner-2')).toBeInTheDocument();
    expect(screen.getByText('· alice')).toBeInTheDocument();
    expect(screen.getByText('-2')).toHaveClass('has-text-link');
    expect(screen.getAllByText('+1')[0]).toHaveClass('has-text-success');
    expect(screen.getByText(/MHD 1.1.2027/)).toBeInTheDocument();
    expect(screen.getByText('Zusammengeführt')).toBeInTheDocument();

    expect(screen.getByText('Neuere')).toBeDisabled();
    await userEvent.click(screen.getByText('Ältere'));
    await waitFor(() => expect(api.movements.listMovements).toHaveBeenLastCalledWith({ locationId: undefined, page: 1, size: 50 }));
    await userEvent.click(await screen.findByText('Neuere'));
    await waitFor(() => expect(api.movements.listMovements).toHaveBeenLastCalledWith({ locationId: undefined, page: 0, size: 50 }));
    await userEvent.selectOptions(screen.getByLabelText('Lagerort'), 'k');
    await waitFor(() => expect(api.movements.listMovements).toHaveBeenLastCalledWith({ locationId: 'k', page: 0, size: 50 }));
  });

  it('shows empty history and errors', async () => {
    const api = fakeApi();
    api.movements.listMovements.mockRejectedValueOnce(new Error('weg'));
    renderWith(<HistoryPage />, api);
    await userEvent.click(await screen.findByText('Erneut versuchen'));
    expect(await screen.findByText('Keine Buchungen.')).toBeInTheDocument();
  });
});
