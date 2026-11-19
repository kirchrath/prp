import { screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { describe, expect, it } from 'vitest';
import { fakeApi, renderWith } from '../test/utils';
import { DashboardPage } from './DashboardPage';

const day = (offset: number) => {
  const d = new Date();
  return new Date(d.getFullYear(), d.getMonth(), d.getDate() + offset);
};
const batch = (id: string, offset: number) => ({
  id, articleId: 'a', articleName: `Milch ${id}`, locationId: 'l', locationPath: 'Keller', bestBefore: day(offset), quantity: 2,
});

describe('DashboardPage', () => {
  it('shows key figures of all services', async () => {
    const api = fakeApi();
    api.articles.listArticles.mockResolvedValue([{ id: '1', status: 'ACTIVE' }, { id: '2', status: 'UNKNOWN' }]);
    api.stock.getSummary.mockResolvedValue({ totalUnits: 17, expiringSoon: [batch('x', -1), batch('y', 0), batch('z', 2), batch('w', 10)] });
    api.shopping.listShoppingItems.mockResolvedValue([{ automatic: true, done: false }, { automatic: false, done: true }]);
    api.scanners.listScanners.mockResolvedValue([{ online: true }, { online: false }]);
    renderWith(<DashboardPage />, api);

    expect(await screen.findByText('17')).toBeInTheDocument();
    expect(screen.getByText('1/2')).toBeInTheDocument();
    expect(screen.getByText('Inbox (unbekannt)').parentElement).toHaveClass('has-background-warning-light');
    expect(screen.getByText('Unter Mindestbestand').parentElement).toHaveClass('has-background-danger-light');
    expect(screen.getByText(/abgelaufen/)).toHaveClass('is-danger');
    expect(screen.getByText(/heute/)).toHaveClass('is-warning');
    expect(screen.getByText(/2 T/)).toHaveClass('is-warning');
    expect(screen.getByText(/10 T/)).toHaveClass('is-info');
  });

  it('shows empty state and errors', async () => {
    const api = fakeApi();
    renderWith(<DashboardPage />, api);
    expect(await screen.findByText(/Nichts läuft/)).toBeInTheDocument();
    expect(screen.getByText('Inbox (unbekannt)').parentElement).not.toHaveClass('has-background-warning-light');

    const failing = fakeApi();
    failing.scanners.listScanners.mockRejectedValueOnce(new Error('offline'));
    renderWith(<DashboardPage />, failing);
    await userEvent.click(await screen.findByText('Erneut versuchen'));
    expect(await screen.findAllByText(/Nichts läuft/)).toHaveLength(2);
  });
});
