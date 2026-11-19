import { act, fireEvent, screen } from '@testing-library/react';
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

  it('uploads firmware and updates devices', async () => {
    const api = fakeApi();
    api.scanners.listScanners.mockResolvedValue([
      { id: 's1', deviceId: 'scanner-1', online: true, firmwareVersion: '0.2.0', kind: 'SCANNER', buttons: [],
        ota: { id: 'j1', targetVersion: '0.2.0', state: 'FAILED', error: 'Prüfsumme falsch', lastModifiedDate: new Date() } },
      { id: 's2', deviceId: 'scanner-2', online: false, firmwareVersion: '0.1.0', kind: 'SCANNER', buttons: [] },
      { id: 's3', deviceId: 'scanner-3', online: true, firmwareVersion: '0.3.0', kind: 'SCANNER', buttons: [],
        ota: { id: 'j2', targetVersion: '0.3.0', state: 'SUCCESS', lastModifiedDate: new Date() } },
    ]);
    api.firmware.listFirmware.mockResolvedValue([
      { id: 'f2', kind: 'SCANNER', firmwareVersion: '0.3.0', size: 1048576, sha256: 'x', fileName: 'firmware.bin', createdDate: new Date(), version: 0 },
      { id: 'f1', kind: 'SCANNER', firmwareVersion: '0.2.0', size: 1000000, sha256: 'y', fileName: 'firmware.bin', createdDate: new Date(), version: 3 },
      { id: 'f3', kind: 'TOASTER', firmwareVersion: '1.0', size: 1, sha256: 'z', fileName: 't.bin', createdDate: new Date(), version: 0 },
    ]);
    renderWith(<ScannersPage />, api);

    expect(await screen.findByText('Update fehlgeschlagen (0.2.0)')).toBeInTheDocument();
    expect(screen.getByText('Prüfsumme falsch')).toBeInTheDocument();
    expect(screen.getByText('Update erfolgreich (0.3.0)')).toBeInTheDocument();
    expect(screen.getByText('TOASTER')).toBeInTheDocument();
    expect(screen.getByText(/1024 KB/)).toBeInTheDocument();
    const updates = screen.getAllByText('Aktualisieren auf 0.3.0');
    expect(updates).toHaveLength(2); // scanner-3 is up to date
    expect(updates[1]).toBeDisabled(); // offline

    await userEvent.click(updates[0]);
    expect(api.firmwareUpdates.startFirmwareUpdate).toHaveBeenCalledWith({ id: 's1' });
    expect(await screen.findByText('Update auf 0.3.0 gestartet')).toBeInTheDocument();

    // rollout
    api.firmwareUpdates.startFirmwareRollout.mockResolvedValueOnce({ started: 2 }).mockResolvedValueOnce({ started: 0 })
      .mockRejectedValueOnce(apiError(409, 'Keine Firmware'));
    await userEvent.click(screen.getByText('Alle Scanner aktualisieren'));
    expect(api.firmwareUpdates.startFirmwareRollout).toHaveBeenCalledWith({ kind: 'SCANNER' });
    expect(await screen.findByText('Update auf 2 Gerät(en) gestartet')).toBeInTheDocument();
    await userEvent.click(screen.getByText('Alle Scanner aktualisieren'));
    expect(await screen.findByText('Alle erreichbaren Geräte sind aktuell')).toBeInTheDocument();
    await userEvent.click(screen.getByText('Alle Scanner aktualisieren'));
    expect(await screen.findByText('Keine Firmware')).toBeInTheDocument();
    expect(screen.queryByText('Alle Knopfleisten aktualisieren')).not.toBeInTheDocument();

    // upload
    const input = screen.getByLabelText('Firmware hochladen');
    const file = new File([new Uint8Array([0xe9])], 'firmware.bin');
    await userEvent.upload(input, file);
    expect(api.firmware.uploadFirmware).toHaveBeenCalledWith({ file });
    expect(await screen.findByText('firmware.bin hochgeladen')).toBeInTheDocument();
    api.firmware.uploadFirmware.mockRejectedValueOnce(apiError(400, 'Firmware-Kennung fehlt'));
    await userEvent.upload(input, new File(['x'], 'other.bin'));
    expect(await screen.findByText('Firmware-Kennung fehlt')).toBeInTheDocument();
    fireEvent.change(input, { target: { files: [] } });
    expect(api.firmware.uploadFirmware).toHaveBeenCalledTimes(2);

    // delete
    await userEvent.click(screen.getByLabelText('Firmware 0.2.0 löschen'));
    expect(api.firmware.deleteFirmware).toHaveBeenCalledWith({ id: 'f1', version: 3 });
    api.firmware.deleteFirmware.mockRejectedValueOnce(apiError(409));
    await userEvent.click(screen.getByLabelText('Firmware 0.2.0 löschen'));
    expect(await screen.findByText(/neu laden/)).toBeInTheDocument();
  });

  it('shows when no firmware was uploaded', async () => {
    renderWith(<ScannersPage />, fakeApi());
    expect(await screen.findByText('Noch keine Firmware hochgeladen.')).toBeInTheDocument();
  });
});
