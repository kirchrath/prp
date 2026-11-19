import { StrictMode } from 'react';
import { createRoot } from 'react-dom/client';
import { AuthProvider } from 'react-oidc-context';
import { WebStorageStateStore } from 'oidc-client-ts';
import { BrowserRouter } from 'react-router';
import 'bulma/css/bulma.min.css';
import './styles.css';
import { config } from './config';
import { App } from './App';
import { AnonymousApiProvider, AuthenticatedApiProvider } from './api/ApiContext';
import { AuthGate } from './components/AuthGate';
import { NotificationProvider } from './components/Notifications';

const root = createRoot(document.getElementById('root')!);

const app = (
  <BrowserRouter>
    <NotificationProvider>
      <App />
    </NotificationProvider>
  </BrowserRouter>
);

if (config.oidc) {
  root.render(
    <StrictMode>
      <AuthProvider
        authority={config.oidc.authority}
        client_id={config.oidc.clientId}
        redirect_uri={window.location.origin + '/'}
        post_logout_redirect_uri={window.location.origin + '/'}
        scope="openid profile"
        userStore={new WebStorageStateStore({ store: window.localStorage })}
        onSigninCallback={() => window.history.replaceState({}, document.title, window.location.pathname)}
      >
        <AuthGate>
          <AuthenticatedApiProvider>{app}</AuthenticatedApiProvider>
        </AuthGate>
      </AuthProvider>
    </StrictMode>,
  );
} else {
  root.render(
    <StrictMode>
      <AnonymousApiProvider>{app}</AnonymousApiProvider>
    </StrictMode>,
  );
}
