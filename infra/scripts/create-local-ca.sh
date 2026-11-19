#!/usr/bin/env bash
# Creates a local certificate authority and a wildcard certificate for *.$DOMAIN.
# Install traefik/certs/ca.pem on your devices (phone, PC) as trusted CA -
# the camera scan and the Keycloak login need a trusted HTTPS connection.
set -euo pipefail
cd "$(dirname "$0")/.."
# shellcheck disable=SC1091
source .env
dir=traefik/certs
mkdir -p "$dir"

if [[ ! -f "$dir/ca.pem" ]]; then
  openssl req -x509 -newkey rsa:4096 -sha256 -days 3650 -nodes \
    -keyout "$dir/ca-key.pem" -out "$dir/ca.pem" -subj "/CN=Home CA ${DOMAIN}"
fi

openssl req -newkey rsa:2048 -nodes -keyout "$dir/key.pem" -out "$dir/cert.csr" -subj "/CN=*.${DOMAIN}"
cat > "$dir/cert.ext" <<EXT
basicConstraints=CA:FALSE
keyUsage=digitalSignature,keyEncipherment
extendedKeyUsage=serverAuth
subjectAltName=DNS:${DOMAIN},DNS:*.${DOMAIN}
EXT
# 825 days is the maximum lifetime accepted by Apple devices
openssl x509 -req -in "$dir/cert.csr" -CA "$dir/ca.pem" -CAkey "$dir/ca-key.pem" -CAcreateserial \
  -out "$dir/cert.pem" -days 825 -sha256 -extfile "$dir/cert.ext"
rm -f "$dir/cert.csr" "$dir/cert.ext"
echo "Created $dir/cert.pem for *.${DOMAIN}. Trust $dir/ca.pem on your devices."
