"""
export_vitals.py – download *all* vitals for one user and store them in vitals_dump.csv
"""

import csv
import sys
from pathlib import Path

import requests

# ─────────────────────────────────────────────────────────────
# CONFIG – these three constants are all you usually change
API_BASE   = "https://beewell.blestod.com"   # ← your backend URL (NO trailing slash)
USER_EMAIL = "test"                          # ← user you want to export
OUT_PATH   = Path("/Users/thomas/Documents/GitHub/TFG-BeeWell/BBDDserver/BBDD/saved_data/vitals_dump_test.csv")         # ← destination CSV file
# ─────────────────────────────────────────────────────────────


def main() -> None:
    url = f"{API_BASE}/vital/all/{USER_EMAIL}"
    print(f"📡  GET {url} …")

    try:
        response = requests.get(url, timeout=15)
        response.raise_for_status()
    except requests.RequestException as exc:
        sys.exit(f"❌ HTTP request failed: {exc}")

    vitals = response.json()
    if not vitals:
        sys.exit("ℹ️  No vitals returned – nothing to write.")

    # Write CSV with the exact keys the API returns
    fieldnames = vitals[0].keys()
    with OUT_PATH.open("w", newline="", encoding="utf-8") as fh:
        writer = csv.DictWriter(fh, fieldnames=fieldnames)
        writer.writeheader()
        writer.writerows(vitals)

    print(f"✅  Saved {len(vitals)} rows to {OUT_PATH.resolve()}")


if __name__ == "__main__":
    main()
