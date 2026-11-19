import { useEffect, useRef, type ReactNode } from 'react';
import { hasAuthParams, useAuth } from 'react-oidc-context';

/** Redirects to Keycloak until the user is logged in. */
export function AuthGate({ children }: { children: ReactNode }) {
  const auth = useAuth();
  const redirecting = useRef(false);

  useEffect(() => {
    const idle = !auth.isAuthenticated && !auth.activeNavigator && !auth.isLoading && !auth.error;
    if (idle && !hasAuthParams() && !redirecting.current) {
      redirecting.current = true;
      void auth.signinRedirect();
    }
  }, [auth]);

  if (auth.error) {
    return (
      <section className="section">
        <div className="notification is-danger">
          Anmeldung fehlgeschlagen: {auth.error.message}
          <button className="button is-small ml-3" onClick={() => void auth.signinRedirect()}>
            Erneut anmelden
          </button>
        </div>
      </section>
    );
  }
  if (!auth.isAuthenticated) {
    return <section className="section has-text-centered">Anmeldung…</section>;
  }
  return <>{children}</>;
}

/** Name and logout button for the navbar. */
export function UserMenu() {
  const auth = useAuth();
  return (
    <div className="navbar-item">
      <span className="mr-3 has-text-grey">{auth.user?.profile.preferred_username}</span>
      <button className="button is-small is-light" onClick={() => void auth.signoutRedirect()}>
        Abmelden
      </button>
    </div>
  );
}
