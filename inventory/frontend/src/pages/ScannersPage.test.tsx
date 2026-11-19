import { act, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { describe, expect, it, vi } from 'vitest';
import { apiError, audit, fakeApi, loc, renderWith } from '../test/utils';
import { ScannersPage } from './ScannersPage';

const scanners = [
  { id: 's1', deviceId: 'scanner-1', name: 'Vorrat', locationId: 'k', online: true, firmwareVersion: '0.2.0', ipAddress: '10.0.0.5', lastSeen: new Date(), kind: 'SCANNER', buttons: [] },
  { id: 's2', deviceId: 'scanner-2', online: false, kind: 'SCANNER', buttons: [] },
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
      id: 's2', scannerSaveDto: { audit: audit(5), data: { deviceId: 'scanner-2', online: false, name: 'Küche', locationId: 'b', buttons: [] } },
    });
    expect(await screen.findByText('Scanner gespeichert')).toBeInTheDocument();

    await userEvent.clear(screen.getByDisplayValue('Vorrat'));
    await userEvent.click(first);
    expect(api.scanners.updateScanner).toHaveBeenLastCalledWith({
      id: 's1', scannerSaveDto: { audit: audit(5), data: { deviceId: 'scanner-2', online: false, name: undefined, locationId: 'k', buttons: [] } },
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
    expect(await screen.findByText('Noch kein Gerät hat sich gemeldet.')).toBeInTheDocument();
  });

  it('shows scanners while locations are loading', async () => {
    const api = fakeApi();
    api.scanners.listScanners.mockResolvedValue(scanners);
    api.locations.listLocations.mockImplementation(() => new Promise(() => undefined));
    api.articles.listArticles.mockImplementation(() => new Promise(() => undefined));
    renderWith(<ScannersPage />, api);
    expect(await screen.findByText('online')).toBeInTheDocument();
  });

  it('assigns articles to the buttons of a button box', async () => {
    const api = fakeApi();
    api.scanners.listScanners.mockResolvedValue([
      { id: 'b1', deviceId: 'knopf-1', online: true, locationId: 'k', kind: 'BUTTONS', buttons: [{ button: 1, articleId: 'a1' }] },
    ]);
    api.locations.listLocations.mockResolvedValue([loc('k', 'Keller')]);
    api.articles.listArticles.mockResolvedValue([{ id: 'a2', name: 'Zucchini' }, { id: 'a1', name: 'Tomaten', brand: 'Bio' }]);
    api.scanners.getScanner.mockResolvedValue({ audit: audit(2), data: { deviceId: 'knopf-1', online: true, buttons: [] } });
    renderWith(<ScannersPage />, api);

    expect(await screen.findByText('Knopfleiste')).toBeInTheDocument();
    expect(await screen.findAllByRole('option', { name: 'Tomaten (Bio)' })).toHaveLength(8);
    const save = screen.getByText('Speichern');
    expect(screen.getByLabelText('Taster 1')).toHaveValue('a1');
    expect(screen.getByLabelText('Taster 8')).toHaveValue('');
    expect(save).toBeDisabled();

    await userEvent.selectOptions(screen.getByLabelText('Taster 1'), 'a2'); // same count, other article
    expect(save).toBeEnabled();
    await userEvent.selectOptions(screen.getByLabelText('Taster 1'), 'a1');
    expect(save).toBeDisabled();
    await userEvent.selectOptions(screen.getByLabelText('Taster 3'), 'a2');
    await userEvent.selectOptions(screen.getByLabelText('Taster 1'), '');
    await userEvent.click(save);
    expect(api.scanners.updateScanner).toHaveBeenCalledWith({
      id: 'b1', scannerSaveDto: { audit: audit(2), data: { deviceId: 'knopf-1', online: true, name: undefined, locationId: 'k', buttons: [{ button: 3, articleId: 'a2' }] } },
    });
    expect(await screen.findByText('Knopfleiste gespeichert')).toBeInTheDocument();
  });
});
