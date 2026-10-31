import os
import json
import ijson
import unicodedata
from typing import Dict, Any
from pathlib import Path
from detect_encoding_issues import detect_text_issues, make_diagnostic


def safe_unicode_norm(s: str) -> str:
    if s is None:
        return s
    return unicodedata.normalize("NFC", s).replace('\r\n', '\n').strip()


def validate_quran_file(path: str, report: Dict[str, Any]):
    report.setdefault("quran_files", []).append(path)
    seen = set()
    basmalah_records = []
    per_surah_last = {}
    total = 0
    duplicates = 0
    missing_fields = 0
    encoding_diagnostics = []

    out_norm_dir = Path(report.get("output_dir", "content_pipeline/output/normalized"))
    out_norm_dir.mkdir(parents=True, exist_ok=True)
    base = Path(path).stem
    raw_out = out_norm_dir / f"raw_quran_{base}.ndjson"
    norm_out = out_norm_dir / f"quran_{base}.ndjson"

    with open(path, "rb") as fh:
        try:
            items = ijson.items(fh, "item")
        except Exception:
            # fallback: try reading as text JSON
            fh.seek(0)
            data = json.load(fh)
            items = iter(data)

        with raw_out.open("w", encoding="utf-8") as rawf, norm_out.open("w", encoding="utf-8") as normf:
            for idx, rec in enumerate(items):
                total += 1
                rawf.write(json.dumps(rec, ensure_ascii=False) + "\n")
                # Basic required fields
                sid = rec.get("surat_id") or rec.get("surah_id")
                ay = rec.get("ayat_number") or rec.get("ayah_number")
                if sid is None or ay is None:
                    missing_fields += 1
                    report.setdefault("missing_field_examples", []).append({"file": path, "index": idx, "record": rec})
                key = (sid, ay)
                if key in seen:
                    duplicates += 1
                    report.setdefault("duplicate_records", []).append({"file": path, "index": idx, "key": key})
                else:
                    seen.add(key)

                # basmalah
                if ay == 0:
                    basmalah_records.append({"file": path, "index": idx, "record": rec})

                # ordering
                if sid is not None:
                    last = per_surah_last.get(sid, -1)
                    if ay is not None and ay != 0:
                        if last != -1 and ay != last + 1:
                            report.setdefault("ordering_issues", []).append({"file": path, "index": idx, "surah": sid, "expected_after": last, "found": ay})
                        per_surah_last[sid] = ay

                # detect encoding issues in text fields
                for field in ("arabic", "arabic_tajweed"):
                    val = rec.get(field)
                    if isinstance(val, str):
                        issues = detect_text_issues(val)
                        for iss in issues:
                            encoding_diagnostics.append(make_diagnostic(path, idx, field, val[:200], iss))

                # deterministic normalization
                rec_norm = dict(rec)
                if isinstance(rec_norm.get("arabic"), str):
                    rec_norm["arabic"] = safe_unicode_norm(rec_norm["arabic"])
                rawf.flush()
                normf.write(json.dumps({"_source_file": path, "_source_index": idx, "record": rec_norm}, ensure_ascii=False) + "\n")

    report.setdefault("quran_stats", {})[path] = {"total_records": total, "duplicates": duplicates, "missing_fields": missing_fields, "basmalah_count": len(basmalah_records)}
    if encoding_diagnostics:
        report.setdefault("encoding_diagnostics", []).extend(encoding_diagnostics)
    if basmalah_records:
        report.setdefault("basmalah_records", []).extend(basmalah_records)


if __name__ == "__main__":
    print("validate_quran.py as module")
