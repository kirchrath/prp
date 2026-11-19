import { act, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { describe, expect, it, vi } from 'vitest';
import { apiError, audit, fakeApi, loc, renderWith } from '../test/utils';
import { ScannersPage } from './ScannersPage';

const scanners = [
  { id: 's1', deviceId: 'scanner-1', name: 'Vorrat', locationId: 'k', online: true, firmwareVersion: '0.2.0', ipAddress: '10.0.0.5', lastSeen: new Date() },
  { id: 's2', deviceId: 'scanner-2', online: false },
];

describe('ScannersPage', () => {
  it('assigns locations and removes scanners', async () => {
    vi.useFakeTimers({ shouldAdvanceTime: true });
    const api = fakeApi();
    api.scanners.listScanners.mockResolvedValue(scanners);
    api.locations.listLocations.mockResolvedValue([loc('k', 'Keller'), loc('b', 'Bad')]);
    api.scanners.getScanner.mockResolvedValue({ audit: audit(5), data: { deviceId: 'scanner-2', online: false } });
    renderWith(<ScannersPage />, api);

    expect(await screen.findByText('online')).toBeInTheDocument();
    expect(screen.getByText('offline')).toBeInTheDocument();
    const [first, second] = screen.getAllByText('Speichern');
    expect(first).toBeDisabled();
    expect(second).toBeDisabled();

    await userEvent.type(screen.getByPlaceholderText('scanner-2'), 'Küche');
    await userEvent.selectOptions(screen.getAllByLabelText('Lagerort')[1], 'b');
    await userEvent.click(second);
    expect(api.scanners.updateScanner).toHaveBeenCalledWith({
      id: 's2', scannerSaveDto: { audit: audit(5), data: { deviceId: 'scanner-2', online: false, name: 'Küche', locationId: 'b' } },
    });
    expect(await screen.findByText('Scanner gespeichert')).toBeInTheDocument();

    await userEvent.clear(screen.getByDisplayValue('Vorrat'));
    await userEvent.click(first);
    expect(api.scanners.updateScanner).toHaveBeenLastCalledWith({
      id: 's1', scannerSaveDto: { audit: audit(5), data: { deviceId: 'scanner-2', online: false, name: undefined, locationId: 'k' } },
    });

    vi.spyOn(window, 'confirm').mockReturnValueOnce(false).mockReturnValue(true);
    await userEvent.click(screen.getAllByText('Entfernen')[0]);
    expect(api.scanners.deleteScanner).not.toHaveBeenCalled();
    await userEvent.click(screen.getAllByText('Entfernen')[0]);
    expect(api.scanners.deleteScanner).toHaveBeenCalledWith({ id: 's1', version: 5 });
    api.scanners.deleteScanner.mockRejectedValueOnce(apiError(409));
    await userEvent.click(screen.getAllByText('Entfernen')[1]);
    expect(await screen.findByText(/neu laden/)).toBeInTheDocument();

    const calls = api.scanners.listScanners.mock.calls.length;
    await act(async () => vi.advanceTimersByTime(15_000));
    expect(api.scanners.listScanners.mock.calls.length).toBeGreaterThan(calls);
    vi.useRealTimers();
  });

  it('shows empty list and errors', async () => {
    const api = fakeApi();
    api.scanners.listScanners.mockRejectedValueOnce(new Error('weg'));
    renderWith(<ScannersPage />, api);
    await userEvent.click(await screen.findByText('Erneut versuchen'));
    expect(await screen.findByText('Noch kein Scanner hat sich gemeldet.')).toBeInTheDocument();
  });

  it('shows scanners while locations are loading', async () => {
    const api = fakeApi();
    api.scanners.listScanners.mockResolvedValue(scanners);
    api.locations.listLocations.mockImplementation(() => new Promise(() => undefined));
    renderWith(<ScannersPage />, api);
    expect(await screen.findByText('online')).toBeInTheDocument();
  });
});
