import { act, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { apiError, fakeApi, loc, renderWith } from '../test/utils';
import { ScanPage } from './ScanPage';

const zxing = vi.hoisted(() => ({
  callback: undefined as undefined | ((result: { getText: () => string } | undefined) => void),
  stop: vi.fn(),
  decode: vi.fn(),
}));

vi.mock('@zxing/browser', () => ({
  BrowserMultiFormatReader: class {
    decodeFromConstraints(_c: unknown, _v: unknown, cb: typeof zxing.callback) {
      zxing.callback = cb;
      return zxing.decode();
    }
  },
}));

function setup(...results: Array<object | Error>) {
  const api = fakeApi();
  api.locations.listLocations.mockResolvedValue([loc('k', 'Keller')]);
  api.bookings.createBookingRaw.mockResolvedValue({ raw: new Response(null, { headers: { Location: '/api/stock/bookings/b1' } }) });
  for (const r of results) {
    if (r instanceof Error) api.bookings.getBooking.mockRejectedValueOnce(r);
    else api.bookings.getBooking.mockResolvedValueOnce({ data: r });
  }
  return api;
}

describe('ScanPage', () => {
  beforeEach(() => {
    window.localStorage.clear();
    zxing.decode.mockResolvedValue({ stop: zxing.stop });
  });
  afterEach(() => vi.unstubAllGlobals());

  it('books manually entered barcodes in both directions', async () => {
    vi.stubGlobal('AudioContext', class {
      currentTime = 0;
      destination = {};
      createOscillator() {
        const osc = { frequency: { value: 0 }, connect: vi.fn(), start: vi.fn(), stop: vi.fn(), onended: undefined as undefined | (() => void) };
        setTimeout(() => osc.onended?.(), 0);
        return osc;
      }
      close = vi.fn();
    });
    const vibrate = vi.fn();
    Object.defineProperty(navigator, 'vibrate', { value: vibrate, configurable: true });
    const api = setup(
      { status: 'IN_OK_UNKNOWN_ARTICLE', message: 'Unbekannt eingelagert', articleName: 'Unbekannt (1)', quantityAtLocation: 1 },
      { status: 'NOT_IN_STOCK', message: 'Nichts da' },
    );
    renderWith(<ScanPage />, api);
    await userEvent.selectOptions(await screen.findByLabelText('Lagerort'), 'k');
    expect(window.localStorage.getItem('scan.locationId')).toBe('k');

    await userEvent.type(screen.getByPlaceholderText('EAN manuell eingeben'), ' 4001 {Enter}');
    expect(await screen.findByText('Unbekannt (1)')).toBeInTheDocument();
    expect(screen.getByText('Bestand am Ort: 1')).toBeInTheDocument();
    expect(screen.getByText('In der Inbox ergänzen')).toBeInTheDocument();
    expect(api.bookings.createBookingRaw).toHaveBeenCalledWith({ scanBookingDto: { ean: '4001', locationId: 'k', direction: 'IN' } });

    await userEvent.click(screen.getByText('⬆ Auslagern'));
    await userEvent.type(screen.getByPlaceholderText('EAN manuell eingeben'), '5002');
    await userEvent.click(screen.getByText('Buchen'));
    expect(await screen.findByText('5002')).toBeInTheDocument();
    expect(screen.getByText('5002').closest('.notification')).toHaveClass('is-danger');
    expect(api.bookings.createBookingRaw).toHaveBeenLastCalledWith({ scanBookingDto: { ean: '5002', locationId: 'k', direction: 'OUT' } });
    await userEvent.click(screen.getByText('⬇ Einlagern'));
    expect(vibrate).toHaveBeenCalledWith(80);
    expect(vibrate).toHaveBeenCalledWith([100, 60, 100]);
    Object.defineProperty(navigator, 'vibrate', { value: undefined, configurable: true });
  });

  it('shows failed requests', async () => {
    window.localStorage.setItem('scan.locationId', 'k');
    const api = setup(apiError(404, 'Lagerort nicht vorhanden'));
    renderWith(<ScanPage />, api);
    await userEvent.type(screen.getByPlaceholderText('EAN manuell eingeben'), '7{Enter}');
    expect(await screen.findByText(/Lagerort nicht vorhanden/)).toBeInTheDocument();
  });

  it('scans with the camera', async () => {
    window.localStorage.setItem('scan.locationId', 'k');
    const api = setup({ status: 'OUT_OK', message: 'ok', articleName: 'Reis', quantityAtLocation: 0 },
      { status: 'IN_OK', message: 'ok', articleName: 'Mehl', quantityAtLocation: 2 });
    renderWith(<ScanPage />, api);
    await userEvent.click(screen.getByText('📷 Kamera starten'));
    await waitFor(() => expect(zxing.callback).toBeDefined());
    let now = 10_000;
    vi.spyOn(Date, 'now').mockImplementation(() => now);
    act(() => zxing.callback!(undefined));
    act(() => zxing.callback!({ getText: () => '111' }));
    act(() => zxing.callback!({ getText: () => '222' })); // another code while booking: ignored
    expect(await screen.findByText('Reis')).toBeInTheDocument();
    // the item stays in view for a long time: seen continuously, never booked again
    for (let i = 0; i < 10; i++) {
      now += 1500;
      act(() => zxing.callback!({ getText: () => '111' }));
    }
    expect(api.bookings.createBookingRaw).toHaveBeenCalledTimes(1);
    // taken away and shown again: next unit
    now += 2500;
    act(() => zxing.callback!({ getText: () => '111' }));
    expect(await screen.findByText('Mehl')).toBeInTheDocument();
    expect(api.bookings.createBookingRaw).toHaveBeenCalledTimes(2);
    vi.mocked(Date.now).mockRestore();
    await userEvent.click(screen.getByText('Kamera stoppen'));
    expect(zxing.stop).toHaveBeenCalled();
  });

  it('stops a camera that started after leaving the page', async () => {
    window.localStorage.setItem('scan.locationId', 'k');
    let resolve: (c: { stop: () => void }) => void = () => undefined;
    zxing.decode.mockReturnValue(new Promise((r) => { resolve = r; }));
    const { unmount } = renderWith(<ScanPage />, setup());
    await userEvent.click(screen.getByText('📷 Kamera starten'));
    unmount();
    await act(async () => resolve({ stop: zxing.stop }));
    expect(zxing.stop).toHaveBeenCalled();
  });

  it('reports camera errors', async () => {
    window.localStorage.setItem('scan.locationId', 'k');
    zxing.decode.mockRejectedValueOnce(new Error('Permission denied')).mockRejectedValueOnce(new Error('x'));
    Object.defineProperty(window, 'isSecureContext', { value: true, configurable: true });
    renderWith(<ScanPage />, setup());
    await userEvent.click(screen.getByText('📷 Kamera starten'));
    expect(await screen.findByText('Permission denied')).toBeInTheDocument();
    Object.defineProperty(window, 'isSecureContext', { value: false, configurable: true });
    await userEvent.click(screen.getByText('📷 Kamera starten'));
    expect(await screen.findByText(/nur über HTTPS/)).toBeInTheDocument();
    Object.defineProperty(window, 'isSecureContext', { value: true, configurable: true });
  });

  it('works without local storage', async () => {
    vi.spyOn(Storage.prototype, 'getItem').mockImplementation(() => { throw new Error('blocked'); });
    vi.spyOn(Storage.prototype, 'setItem').mockImplementation(() => { throw new Error('blocked'); });
    renderWith(<ScanPage />, setup());
    expect(screen.getByText('📷 Kamera starten')).toBeDisabled();
    await userEvent.selectOptions(await screen.findByLabelText('Lagerort'), 'k');
    expect(screen.getByText('📷 Kamera starten')).toBeEnabled();
    await userEvent.selectOptions(screen.getByLabelText('Lagerort'), '');
    expect(screen.getByText('📷 Kamera starten')).toBeDisabled();
  });
});
