import { fireEvent, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { describe, expect, it } from 'vitest';
import { apiError, audit, fakeApi, renderWith } from '../test/utils';
import { NotificationsPage } from './NotificationsPage';

const never = () => new Promise(() => {});

describe('NotificationsPage', () => {
  it('shows what is due and the send log', async () => {
    const api = fakeApi();
    api.digests.getDue.mockResolvedValue({
      expired: [{ articleId: 'a1', name: 'Milch', brand: 'Weide', bestBefore: new Date(2026, 10, 17), quantity: 2, daysLeft: -2 }],
      dueToday: [{ articleId: 'a2', name: 'Joghurt', bestBefore: new Date(2026, 10, 19), quantity: 1, daysLeft: 0 }],
      dueSoon: [],
    });
    api.digests.listDigests.mockResolvedValue([
      { id: 'd1', day: new Date(2026, 10, 19), status: 'SENT', attempts: 1, expired: 1, dueToday: 1, dueSoon: 0, lastModifiedDate: new Date(2026, 10, 19, 8) },
      { id: 'd2', day: new Date(2026, 10, 18), status: 'NOTHING_DUE', attempts: 1, expired: 0, dueToday: 0, dueSoon: 0, lastModifiedDate: new Date(2026, 10, 18, 8) },
      { id: 'd3', day: new Date(2026, 10, 17), status: 'FAILED', attempts: 4, expired: 1, dueToday: 0, dueSoon: 0, error: 'ntfy: 503', lastModifiedDate: new Date(2026, 10, 17, 9) },
      { id: 'd4', day: new Date(2026, 10, 16), status: 'OTHER', attempts: 1, expired: 0, dueToday: 0, dueSoon: 0, lastModifiedDate: new Date(2026, 10, 16, 8) },
    ]);
    renderWith(<NotificationsPage />, api);
    expect(await screen.findByText(/Milch/)).toHaveTextContent('Milch (Weide) · 2×');
    expect(screen.getByText('(Weide)')).toBeInTheDocument();
    expect(screen.getByText(/Joghurt/)).toHaveTextContent('Joghurt · 1×');
    expect(screen.queryByText('Läuft bald ab')).not.toBeInTheDocument();
    expect(await screen.findByText('Gesendet')).toBeInTheDocument();
    expect(screen.getByText('Nichts fällig')).toBeInTheDocument();
    expect(screen.getByText('Fehlgeschlagen')).toBeInTheDocument();
    expect(screen.getByText('ntfy: 503')).toBeInTheDocument();
    expect(screen.getByText('OTHER')).toBeInTheDocument();
  });

  it('shows empty states, loading and errors', async () => {
    const api = fakeApi();
    api.digests.getDue.mockRejectedValueOnce(apiError(500, 'Kaputt'));
    api.digests.listDigests.mockRejectedValueOnce(apiError(500, 'Log kaputt'));
    api.notificationSettings.getNotificationSettings.mockRejectedValueOnce(apiError(500, 'Einstellungen kaputt'));
    renderWith(<NotificationsPage />, api);
    expect(await screen.findByText('Kaputt')).toBeInTheDocument();
    expect(screen.getByText('Log kaputt')).toBeInTheDocument();
    expect(screen.getByText('Einstellungen kaputt')).toBeInTheDocument();
    for (const button of screen.getAllByText('Erneut versuchen')) await userEvent.click(button);
    expect(await screen.findByText('Nichts läuft in nächster Zeit ab.')).toBeInTheDocument();
    expect(await screen.findByText('Noch nichts gesendet.')).toBeInTheDocument();
    expect(await screen.findByText('Testnachricht senden')).toBeInTheDocument();

    const loading = fakeApi();
    loading.digests.getDue.mockImplementation(never);
    loading.digests.listDigests.mockImplementation(never);
    loading.notificationSettings.getNotificationSettings.mockImplementation(never);
    renderWith(<NotificationsPage />, loading);
    expect(screen.getAllByText('Lade…').length).toBeGreaterThanOrEqual(3);
  });

  it('edits and saves the settings', async () => {
    const api = fakeApi();
    api.categories.listCategories.mockResolvedValue([{ id: 'c1', name: 'Milchprodukte' }, { id: 'c2', name: 'Konserven' }]);
    api.notificationSettings.getNotificationSettings.mockResolvedValue({
      audit: audit(3),
      data: { enabled: true, sendTime: '08:00:00', defaultDays: 3, categoryThresholds: [{ categoryId: 'gone', days: 5 }] },
    });
    api.notificationSettings.updateNotificationSettings.mockRejectedValueOnce(apiError(409, 'Veraltet'));
    renderWith(<NotificationsPage />, api);

    expect(await screen.findByDisplayValue('Gelöschte Kategorie')).toBeInTheDocument();
    await userEvent.click(screen.getByLabelText('Schwelle Gelöschte Kategorie entfernen'));
    await userEvent.click(screen.getByLabelText(/Tägliche/));
    fireEvent.change(screen.getByLabelText('Uhrzeit'), { target: { value: '19:30' } });
    fireEvent.change(screen.getByLabelText(/Tage vor dem MHD/), { target: { value: '4' } });

    await waitFor(() => expect(screen.getByLabelText('Kategorie hinzufügen')).toBeInTheDocument());
    await userEvent.selectOptions(screen.getByLabelText('Kategorie hinzufügen'), 'c1');
    await userEvent.selectOptions(screen.getByLabelText('Kategorie hinzufügen'), 'c2');
    expect(screen.queryByLabelText('Kategorie hinzufügen')).not.toBeInTheDocument();
    fireEvent.change(screen.getByLabelText('Tage für Milchprodukte'), { target: { value: '1' } });

    await userEvent.click(screen.getByText('Speichern'));
    expect(await screen.findByText('Veraltet')).toBeInTheDocument();
    await userEvent.click(screen.getByText('Speichern'));
    expect(await screen.findByText('Einstellungen gespeichert')).toBeInTheDocument();
    expect(api.notificationSettings.updateNotificationSettings).toHaveBeenLastCalledWith({
      settingsSaveDto: {
        audit: audit(3),
        data: {
          enabled: false, sendTime: '19:30', defaultDays: 4,
          categoryThresholds: [{ categoryId: 'c1', days: 1 }, { categoryId: 'c2', days: 4 }],
        },
      },
    });

    fireEvent.change(screen.getByLabelText('Uhrzeit'), { target: { value: '' } });
    expect(screen.getByText('Speichern')).toBeDisabled();
  });

  it('sends a test message', async () => {
    const api = fakeApi();
    api.notificationSettings.sendTestNotification.mockRejectedValueOnce(apiError(502, 'ntfy nicht erreichbar'));
    renderWith(<NotificationsPage />, api);
    await userEvent.click(await screen.findByText('Testnachricht senden'));
    expect(await screen.findByText('ntfy nicht erreichbar')).toBeInTheDocument();
    await userEvent.click(screen.getByText('Testnachricht senden'));
    expect(await screen.findByText('Testnachricht gesendet')).toBeInTheDocument();
  });
});
