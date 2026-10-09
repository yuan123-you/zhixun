# 知讯生产部署计划 — 2026-10-07

**Goal:** Deploy the latest committed master revision to Oracle and expose only https://glint.novo.ccwu.cc/.
**Architecture:** Use /srv/zhixun/repo for Git checkout, /srv/zhixun/deployment for deployment files, and /srv/zhixun/secrets for restricted credentials. Separate Compose project and persistent volumes; host Nginx handles HTTPS. Do not modify local uncommitted application changes or existing sites.
**Source:** User deployment request; GitHub origin/master d9dbaaca540f3124d4e4d329b80b6503baba0369 verified against origin/main.

- [x] Inspect source, branches, SSH access, server capacity, ports, DNS and existing certificate.
- [x] Write failing checks for production port exposure and credentials/config separation.
- [x] Generate isolated production Compose configuration, Nginx site and safe update script.
- [x] Use fresh private service credentials and disable public seed/demo logins; preserve content for display.
- [x] Pull latest master on the server, build application images and start isolated services.
- [x] Validate API health, pages, static assets, authorization boundaries and HTTPS before enabling the domain.
- [x] Configure and test glint-specific certificate renewal without altering other certificates.
- [x] Add only the public website link to the personal homepage after deployment verification.
- [x] Record revision, directories, backup and known verification limits.
