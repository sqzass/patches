#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."

./gradlew :patches:generatePatchesList

python3 - <<'PY'
import json
d = json.load(open("patches-list.json", encoding="utf-8"))
print("patches:", len(d["patches"]))
print("apps:", sorted({c["name"] for p in d["patches"] for c in (p.get("compatiblePackages") or [])}))
PY

python3 .github/scripts/generate_patches_readme.py sqzass/lustx-patches dev patches-list.json README.md

./gradlew :patches:buildAndroid