#!/bin/sh
# Writes the runtime configuration of the SPA from environment variables.
set -eu
target=/usr/share/nginx/html/config.js
if [ -n "${OIDC_AUTHORITY:-}" ]; then
  cat > "$target" <<JS
window.__APP_CONFIG__ = {
  oidc: { authority: '${OIDC_AUTHORITY}', clientId: '${OIDC_CLIENT_ID}' },
};
JS
else
  echo "WARNING: OIDC_AUTHORITY not set - frontend runs without login" >&2
  echo "window.__APP_CONFIG__ = {};" > "$target"
fi
