import { screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { describe, expect, it, vi } from 'vitest';
import { fakeApi, renderWith } from './test/utils';

const conf = vi.hoisted(() => ({ config: { oidc: { authority: 'a', clientId: 'c' } } as Record<string, unknown> }));
vi.mock('./config', () => conf);
vi.mock('./components/AuthGate', () => ({ UserMenu: () => <span>Benutzermenü</span> }));
vi.mock('./pages/ScanPage', () => ({ ScanPage: () => <h1>Scan-Seite</h1> }));

const { App } = await import('./App');

describe('App', () => {
  it('navigates between pages', async () => {
    renderWith(<App />, fakeApi(), '/unbekannt');
    expect(await screen.findByRole('heading', { name: 'Übersicht' })).toBeInTheDocument();
    expect(screen.getByText('Benutzermenü')).toBeInTheDocument();

    const burger = screen.getByRole('button', { name: 'Menü' });
    await userEvent.click(burger);
    expect(burger).toHaveClass('is-active');
    await userEvent.click(screen.getByRole('link', { name: 'Scannen' }));
    expect(await screen.findByText('Scan-Seite')).toBeInTheDocument();
    expect(burger).not.toHaveClass('is-active');

    for (const [link, heading] of [['Bestand', 'Bestand'], ['Artikel', 'Artikel'], ['Einkaufsliste', 'Einkaufsliste'],
      ['Historie', 'Historie'], ['Lagerorte', 'Lagerorte'], ['Artikelkategorien', 'Artikelkategorien'],
      ['Rezeptkategorien', 'Rezeptkategorien'], ['Kochen', 'Was kann ich kochen?'], ['Rezepte', 'Rezepte'], ['Scanner', 'Scanner']]) {
      await userEvent.click(screen.getByRole('link', { name: link }));
      expect(await screen.findByRole('heading', { name: heading, level: 1 })).toBeInTheDocument();
    }
    await userEvent.click(screen.getByRole('link', { name: '📦 Vorrat' }));
    expect(await screen.findByRole('heading', { name: 'Übersicht' })).toBeInTheDocument();
  });

  it('has no user menu without login', async () => {
    conf.config = {};
    vi.resetModules();
    const { App: AppWithoutLogin } = await import('./App');
    const utils = await import('./test/utils'); // same module instances as the freshly imported App
    utils.renderWith(<AppWithoutLogin />, utils.fakeApi());
    expect(await screen.findByRole('heading', { name: 'Übersicht' })).toBeInTheDocument();
    expect(screen.queryByText('Benutzermenü')).toBeNull();
  });
});
