import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { AuthGate, UserMenu } from './AuthGate';

const auth = vi.hoisted(() => ({ state: {} as Record<string, unknown>, params: false }));

vi.mock('react-oidc-context', () => ({
  useAuth: () => auth.state,
  hasAuthParams: () => auth.params,
}));

function state(overrides: Record<string, unknown>) {
  auth.state = {
    isAuthenticated: false, isLoading: false, activeNavigator: undefined, error: undefined,
    signinRedirect: vi.fn().mockResolvedValue(undefined), signoutRedirect: vi.fn().mockResolvedValue(undefined),
    user: { profile: { preferred_username: 'alice' } },
    ...overrides,
  };
  return auth.state as { signinRedirect: ReturnType<typeof vi.fn>; signoutRedirect: ReturnType<typeof vi.fn> };
}

describe('AuthGate', () => {
  beforeEach(() => {
    auth.params = false;
  });

  it('redirects to the login once', () => {
    const s = state({});
    const { rerender } = render(<AuthGate>geheim</AuthGate>);
    rerender(<AuthGate>geheim</AuthGate>);
    expect(s.signinRedirect).toHaveBeenCalledTimes(1);
    expect(screen.getByText('Anmeldung…')).toBeInTheDocument();
  });

  it('waits while loading, navigating or returning from the login', () => {
    const loading = state({ isLoading: true });
    render(<AuthGate>x</AuthGate>);
    const navigating = state({ activeNavigator: 'signinRedirect' });
    render(<AuthGate>x</AuthGate>);
    auth.params = true;
    const returning = state({});
    render(<AuthGate>x</AuthGate>);
    for (const s of [loading, navigating, returning]) expect(s.signinRedirect).not.toHaveBeenCalled();
  });

  it('shows errors and allows a new login', async () => {
    const s = state({ error: new Error('abgelehnt') });
    render(<AuthGate>x</AuthGate>);
    expect(screen.getByText(/abgelehnt/)).toBeInTheDocument();
    await userEvent.click(screen.getByText('Erneut anmelden'));
    expect(s.signinRedirect).toHaveBeenCalled();
  });

  it('renders the app when logged in', async () => {
    const s = state({ isAuthenticated: true });
    render(<AuthGate>geheim</AuthGate>);
    expect(screen.getByText('geheim')).toBeInTheDocument();
    render(<UserMenu />);
    expect(screen.getByText('alice')).toBeInTheDocument();
    await userEvent.click(screen.getByText('Abmelden'));
    expect(s.signoutRedirect).toHaveBeenCalled();
  });
});
