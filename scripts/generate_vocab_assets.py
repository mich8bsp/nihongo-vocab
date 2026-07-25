#!/usr/bin/env python3
"""One-off script: reshape elzup/jlpt-word-list CSVs into this app's Entry
asset schema (list of {"text", "meanings", "romaji"} per level). Not run at
app build time - run manually whenever the source data needs re-pulling,
and commit the generated JSON under app/src/main/assets/vocab/.

Source: https://github.com/elzup/jlpt-word-list (MIT, see
app/src/main/assets/vocab/ATTRIBUTION.md). Expects the raw CSVs
(src/{level}.csv) already downloaded into RAW_DIR via curl - fetch with:
  for l in n5 n4 n3 n2 n1; do
    curl -sL https://raw.githubusercontent.com/elzup/jlpt-word-list/master/src/$l.csv -o RAW_DIR/$l.csv
  done

Needs `pykakasi` (kana reading -> Hepburn romaji) - not a repo dependency,
just for this offline generation step: `pip install pykakasi` in a venv.
"""
import csv
import json
import os
import pathlib

import pykakasi

LEVELS = ["n5", "n4", "n3", "n2", "n1"]  # easiest first - see dedup rule below
RAW_DIR = pathlib.Path(os.environ.get(
    "JLPT_RAW_DIR",
    "/tmp/claude-1000/-home-michael-Devl-repos-nihongo-vocab/e3c73851-10f2-4371-8a3d-44ec4e474447/scratchpad/jlpt_raw",
))
OUT_DIR = pathlib.Path(__file__).parent.parent / "app/src/main/assets/vocab"


def fetch_rows(level: str) -> list[dict]:
    text = (RAW_DIR / f"{level}.csv").read_text(encoding="utf-8")
    return list(csv.DictReader(text.splitlines()))


def to_romaji(kakasi, reading: str) -> str:
    return "".join(chunk["hepburn"] for chunk in kakasi.convert(reading))


def main() -> None:
    OUT_DIR.mkdir(parents=True, exist_ok=True)
    kakasi = pykakasi.kakasi()
    claimed: set[str] = set()  # expressions already assigned to an easier level

    for level in LEVELS:
        rows = fetch_rows(level)
        entries: dict[str, list[str]] = {}  # text -> ordered unique meanings
        readings: dict[str, str] = {}  # text -> reading (first seen wins)

        for row in rows:
            expr = row["expression"].strip()
            if expr in claimed:
                continue  # already owned by an easier level's pool
            meanings = [m.strip() for m in row["meaning"].split(",") if m.strip()]
            existing = entries.setdefault(expr, [])
            for m in meanings:
                if m not in existing:
                    existing.append(m)
            readings.setdefault(expr, row["reading"].strip())

        claimed.update(entries.keys())

        out = [
            {"text": text, "meanings": meanings, "romaji": to_romaji(kakasi, readings[text])}
            for text, meanings in entries.items()
        ]
        out_path = OUT_DIR / f"{level}.json"
        out_path.write_text(json.dumps(out, ensure_ascii=False, indent=2), encoding="utf-8")
        print(f"{level}: {len(out)} entries -> {out_path}")


if __name__ == "__main__":
    main()
