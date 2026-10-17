#!/bin/bash
set -euo pipefail
cd /srv/zhixun/repo
test -z "$(git status --porcelain)" || { echo 'Deployment checkout is dirty; refusing to overwrite.' >&2; exit 1; }
git pull --ff-only origin master
cd /srv/zhixun/deployment
cp -u /srv/zhixun/repo/deploy/production/ClientSubpath.Dockerfile /srv/zhixun/deployment/ClientSubpath.Dockerfile 2>/dev/null || true
export RELEASE_TAG="$(git -C ../repo rev-parse --short HEAD)"
compose=(docker compose --env-file ../secrets/production.env -f compose.production.json)
"${compose[@]}" config --quiet
"${compose[@]}" build server client admin opensearch
"${compose[@]}" up -d --wait --wait-timeout 360
sudo -n nginx -t
sudo -n systemctl reload nginx
printf 'Deployed revision: '
git -C ../repo rev-parse HEAD

