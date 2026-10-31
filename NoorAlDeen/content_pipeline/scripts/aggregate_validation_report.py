#!/usr/bin/env python3
"""Aggregate numeric diagnostics from validation_report.json into per-file counts.

Usage: python aggregate_validation_report.py path/to/validation_report.json
Writes: content_pipeline/output/reports/validation_aggregates.json and .md
"""
import json
import os
import re
import sys
from collections import defaultdict


def looks_like_mojibake(s: str) -> bool:
    if not s:
        return False
    # replacement char
    if "�" in s:
        return True
    # common mojibake marker seen in this corpus
    if "®" in s or "?d" in s:
        return True
    # mixed Arabic + long Latin runs -> likely encoding corruption
    arabic = re.search(r"[\u0600-\u06FF]", s)
    latin_run = re.search(r"[A-Za-z]{4,}", s)
    if arabic and latin_run:
        return True
    return False


def safe_get_string(val):
    if val is None:
        return ""
    if isinstance(val, str):
        return val
    try:
        return str(val)
    except Exception:
        return ""


def main(path):
    outdir = os.path.normpath(os.path.join(os.path.dirname(path), "..", "reports"))
    os.makedirs(outdir, exist_ok=True)

    with open(path, "r", encoding="utf-8") as f:
        data = json.load(f)

    files_processed = data.get("files_processed", [])

    per_file = defaultdict(lambda: defaultdict(int))

    # Count files_processed baseline
    for fn in files_processed:
        per_file[fn]["files_processed"] = 1

    # Inspect top-level arrays that contain diagnostic entries
    for key, val in data.items():
        if isinstance(val, list):
            for item in val:
                if not isinstance(item, dict):
                    continue
                file = item.get("file") or item.get("source")
                if not file:
                    # some arrays may hold raw records with nested file path
                    # attempt to find file field in nested dicts
                    file = None
                    for v in item.values():
                        if isinstance(v, dict) and "file" in v:
                            file = v.get("file")
                            break
                if not file:
                    # skip items without file provenance
                    continue
                per_file[file]["total_diagnostics"] += 1
                per_file[file][f"diag_category::{key}"] += 1

                # record-level heuristics
                record = item.get("record")
                if isinstance(record, dict):
                    # Basmalah / ayat_number == 0
                    ay = record.get("ayat_number")
                    if isinstance(ay, int) and ay == 0:
                        per_file[file]["basmalah_ayat_number_zero"] += 1

                    # encoding heuristics across translations/tafseers
                    # check translations dict
                    translations = record.get("translations") or {}
                    if isinstance(translations, dict):
                        for k, v in translations.items():
                            s = safe_get_string(v)
                            if looks_like_mojibake(s):
                                per_file[file]["encoding_anomalies"] += 1
                                break

                    # check tafseers text blobs
                    tafs = record.get("tafseers") or {}
                    if isinstance(tafs, dict):
                        for k, v in tafs.items():
                            s = safe_get_string(v)
                            if looks_like_mojibake(s):
                                per_file[file]["encoding_anomalies"] += 1
                                break

    # Prepare output
    agg_path = os.path.join(outdir, "validation_aggregates.json")
    md_path = os.path.join(outdir, "validation_aggregates.md")

    # Convert defaultdict to normal dict for JSON
    out = {fn: dict(counts) for fn, counts in per_file.items()}

    with open(agg_path, "w", encoding="utf-8") as of:
        json.dump(out, of, ensure_ascii=False, indent=2)

    # Write a compact markdown table
    lines = []
    lines.append("# Validation Aggregates\n")
    lines.append("| file | total_diagnostics | basmalah_ayat_number_zero | encoding_anomalies | other_categories... |")
    lines.append("|---|---:|---:|---:|---:|")
    for fn, counts in sorted(out.items()):
        total = counts.get("total_diagnostics", 0)
        bas = counts.get("basmalah_ayat_number_zero", 0)
        enc = counts.get("encoding_anomalies", 0)
        lines.append(f"| {fn} | {total} | {bas} | {enc} | {len([k for k in counts.keys() if k.startswith('diag_category::')])} categories |")

    with open(md_path, "w", encoding="utf-8") as mf:
        mf.write("\n".join(lines))

    print("WROTE:", agg_path)
    print("WROTE:", md_path)


if __name__ == "__main__":
    if len(sys.argv) < 2:
        print("Usage: aggregate_validation_report.py path/to/validation_report.json")
        sys.exit(1)
    main(sys.argv[1])
