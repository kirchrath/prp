import { useEffect } from 'react';
import { render, screen, waitFor } from '@testing-library/react';
import { describe, expect, it, vi } from 'vitest';
import { AnonymousApiProvider, AuthenticatedApiProvider, createApi, useApi } from './ApiContext';
import { scanAndWait } from './bookings';
import { daysUntil, formatDate, formatDateTime, fromInputDate, toInputDate } from './dates';
import { createdId, errorMessage } from './errors';
import { fakeApi, apiError, type FakeApi } from '../test/utils';
import type { Api } from './ApiContext';

vi.mock('react-oidc-context', () => ({ useAuth: () => ({ user: { access_token: 'token-1' } }) }));

describe('dates', () => {
  it('converts between date inputs and dates', () => {
    expect(fromInputDate('')).toBeUndefined();
    const d = fromInputDate('2027-01-31')!;
    expect([d.getFullYear(), d.getMonth(), d.getDate()]).toEqual([2027, 0, 31]);
    expect(toInputDate(d)).toBe('2027-01-31');
    expect(toInputDate(undefined)).toBe('');
  });

  it('formats dates', () => {
    expect(formatDate(undefined)).toBe('–');
    expect(formatDate(new Date(2027, 0, 31))).toBe('31.1.2027');
    expect(formatDateTime(undefined)).toBe('–');
    expect(formatDateTime(new Date(2027, 0, 31, 8, 5))).toContain('31.01.27');
  });

  it('counts days', () => {
    expect(daysUntil(new Date(2027, 0, 3), new Date(2027, 0, 1, 15))).toBe(2);
    expect(daysUntil(new Date(new Date().setHours(0, 0, 0, 0)))).toBe(0);
  });
});

describe('errors', () => {
  it('uses the problem detail or the status', async () => {
    expect(await errorMessage(apiError(409, 'Kaputt'))).toBe('Kaputt');
    expect(await errorMessage(apiError(401))).toBe('Nicht angemeldet');
    expect(await errorMessage(apiError(403))).toBe('Keine Berechtigung');
    expect(await errorMessage(apiError(409))).toContain('neu laden');
    expect(await errorMessage(apiError(500))).toBe('Fehler 500');
    expect(await errorMessage(Object.assign(new Error('x'), { response: new Response('{}', { status: 404 }) }))).toBe('Fehler 404');
  });

  it('falls back to the error message', async () => {
    expect(await errorMessage(new Error('offline'))).toBe('offline');
    expect(await errorMessage('text')).toBe('text');
    expect(await errorMessage(null)).toBe('null');
  });

  it('reads the id of created resources', () => {
    expect(createdId({ raw: new Response(null, { headers: { Location: '/api/x/abc' } }) })).toBe('abc');
    expect(createdId({ raw: new Response(null) })).toBe('');
  });
});

describe('scanAndWait', () => {
  const created = (location?: string) => ({ raw: new Response(null, { headers: location ? { Location: location } : {} }) });

  it('polls while the article is pending', async () => {
    const api = fakeApi();
    api.bookings.createBookingRaw.mockResolvedValue(created('/api/stock/bookings/b1'));
    api.bookings.getBooking
      .mockResolvedValueOnce({ data: { status: 'PENDING_ARTICLE' } })
      .mockResolvedValueOnce({ data: { status: 'IN_OK_UNKNOWN_ARTICLE' } });
    const wait = vi.fn().mockResolvedValue(undefined);
    const result = await scanAndWait(api as unknown as Api, '1', 'l1', 'IN', 5, wait);
    expect(result.status).toBe('IN_OK_UNKNOWN_ARTICLE');
    expect(wait).toHaveBeenCalledTimes(1);
    expect(api.bookings.getBooking).toHaveBeenCalledWith({ id: 'b1' });
    expect(api.bookings.createBookingRaw).toHaveBeenCalledWith({ scanBookingDto: { ean: '1', locationId: 'l1', direction: 'IN' } });
  });

  it('gives up after the attempts', async () => {
    const api = fakeApi();
    api.bookings.createBookingRaw.mockResolvedValue(created());
    api.bookings.getBooking.mockResolvedValue({ data: { status: 'PENDING_ARTICLE' } });
    vi.useFakeTimers();
    const promise = scanAndWait(api as unknown as Api, '1', 'l1', 'OUT', 2);
    await vi.advanceTimersByTimeAsync(1000);
    expect((await promise).status).toBe('PENDING_ARTICLE');
    expect(api.bookings.getBooking).toHaveBeenCalledWith({ id: '' });
    vi.useRealTimers();
  });
});

function ShowApi() {
  const api = useApi();
  useEffect(() => {
    void api.categories.listCategories();
  }, [api]);
  return <span>{Object.keys(api).length}</span>;
}

describe('ApiContext', () => {
  it('creates clients for all services that send the token', async () => {
    const fetchMock = vi.fn().mockImplementation(async () => new Response('[]', { headers: { 'Content-Type': 'application/json' } }));
    vi.stubGlobal('fetch', fetchMock);
    const api = createApi(() => 'abc');
    await api.categories.listCategories();
    expect(fetchMock.mock.calls[0][0]).toBe('/api/catalog/categories');
    expect(fetchMock.mock.calls[0][1].headers.Authorization).toBe('Bearer abc');
    await createApi(() => undefined).scanners.listScanners();
    expect(fetchMock.mock.calls[1][1].headers.Authorization).toBeUndefined();
    vi.unstubAllGlobals();
  });

  it('provides the api', async () => {
    const fetchMock = vi.fn().mockImplementation(async () => new Response('[]', { headers: { 'Content-Type': 'application/json' } }));
    vi.stubGlobal('fetch', fetchMock);
    render(<AuthenticatedApiProvider><ShowApi /></AuthenticatedApiProvider>);
    render(<AnonymousApiProvider><ShowApi /></AnonymousApiProvider>);
    expect(screen.getAllByText('16')).toHaveLength(2);
    await waitFor(() => expect(fetchMock).toHaveBeenCalledTimes(2));
    expect(fetchMock.mock.calls.map((c) => c[1].headers.Authorization)).toEqual(['Bearer token-1', undefined]);
    vi.unstubAllGlobals();
  });

  it('fails outside a provider', () => {
    vi.spyOn(console, 'error').mockImplementation(() => undefined);
    expect(() => render(<ShowApi />)).toThrow('ApiProvider');
  });
});

export type { FakeApi };
