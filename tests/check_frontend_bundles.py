"""Inspect real production JS assets; do not hide oversized bundles by changing Vite limits."""
from pathlib import Path
import sys

ROOT = Path(__file__).resolve().parents[1]
LIMIT = 1_000_000

def check(app):
    assets = ROOT / app / "dist" / "assets"
    files = list(assets.glob("*.js"))
    if not files:
        raise SystemExit(f"{app}: production assets are missing; run npm run build first")
    largest = max(files, key=lambda p: p.stat().st_size)
    print(f"{app}: largest JS chunk {largest.name}: {largest.stat().st_size:,} bytes")
    oversized = [p for p in files if p.stat().st_size > LIMIT]
    if oversized:
        raise SystemExit(f"{app}: {len(oversized)} chunk(s) exceed the {LIMIT:,}-byte budget")

if __name__ == "__main__":
    for app in sys.argv[1:] or ["client", "admin"]:
        check(app)
