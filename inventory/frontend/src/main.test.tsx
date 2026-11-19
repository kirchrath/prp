import { screen, waitFor } from '@testing-library/react';
import { beforeEach, describe, expect, it, vi } from 'vitest';

vi.mock('./App', () => ({ App: () => <h1>App</h1> }));
vi.mock('./components/AuthGate', () => ({ AuthGate: ({ children }: { children: React.ReactNode }) => <>{children}</> }));
vi.mock('react-oidc-context', () => ({
  AuthProvider: ({ children, onSigninCallback }: { children: React.ReactNode; onSigninCallback: () => void }) => {
    onSigninCallback();
    return <>{children}</>;
  },
  useAuth: () => ({ user: undefined }),
}));

describe('main', () => {
  beforeEach(() => {
    vi.resetModules();
    document.body.innerHTML = '<div id="root"></div>';
  });

  it('starts with login', async () => {
    window.__APP_CONFIG__ = { oidc: { authority: 'https://auth/realms/home', clientId: 'c' } };
    window.history.pushState({}, '', '/artikel?code=1&state=2');
    await import('./main');
    expect(await screen.findByText('App')).toBeInTheDocument();
    await waitFor(() => expect(window.location.search).toBe(''));
  });

  it('starts without login', async () => {
    window.__APP_CONFIG__ = undefined;
    await import('./main');
    expect(await screen.findByText('App')).toBeInTheDocument();
  });
});
