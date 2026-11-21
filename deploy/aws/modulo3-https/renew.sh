#!/usr/bin/env bash
set -euo pipefail
umask 077
APP_DIR=/home/ec2-user/avicontrol-modulo2
HTTPS_DIR="$APP_DIR/https-modulo3"
cd "$APP_DIR"
exec 9>/run/lock/avicontrol-m3-https-renew.lock
flock -n 9 || exit 0
/usr/bin/docker run --rm --memory 256m \
  -v "$HTTPS_DIR/letsencrypt:/etc/letsencrypt" \
  -v "$HTTPS_DIR/work:/var/lib/letsencrypt" \
  -v "$HTTPS_DIR/logs:/var/log/letsencrypt" \
  -v "$HTTPS_DIR/webroot:/var/www/acme" \
  certbot/certbot:v5.4.0 renew --non-interactive --quiet --no-random-sleep-on-renew \
  --preferred-profile shortlived \
  --deploy-hook 'touch /var/lib/letsencrypt/reload-required'
if [[ -f "$HTTPS_DIR/work/reload-required" ]]; then
  /usr/bin/docker compose exec -T modulo3-https nginx -t
  /usr/bin/docker compose exec -T modulo3-https nginx -s reload
  rm -f -- "$HTTPS_DIR/work/reload-required"
fi
