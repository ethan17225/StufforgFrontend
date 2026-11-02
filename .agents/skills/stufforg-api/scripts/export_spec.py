#!/usr/bin/env python3
"""
Helper script to fetch the latest OpenAPI spec from the running StuffOrg backend
and refresh the references in .agents/skills/stufforg-api/references/
"""

import json
import urllib.request
from pathlib import Path

DEFAULT_URL = "http://localhost:8000/api/openapi.json"
SKILL_DIR = Path(__file__).resolve().parent.parent
SPEC_OUT = SKILL_DIR / "references" / "openapi.json"

def main():
    print(f"Fetching OpenAPI spec from {DEFAULT_URL}...")
    try:
        with urllib.request.urlopen(DEFAULT_URL) as resp:
            data = json.load(resp)
        SPEC_OUT.write_text(json.dumps(data, indent=2))
        print(f"Saved {len(data.get('paths', {}))} endpoints to {SPEC_OUT}")
    except Exception as e:
        print(f"Error fetching spec: {e}")
        print("Ensure the backend container is running: docker compose up -d")

if __name__ == "__main__":
    main()

