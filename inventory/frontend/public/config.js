// Development defaults. In the container this file is generated from environment variables
// (see docker/40-runtime-config.sh). Keycloak is required: all services validate the access token.
window.__APP_CONFIG__ = {
  oidc: {
    authority: 'http://localhost:8180/realms/home',
    clientId: 'inventory-frontend',
  },
};
