import json
import ijson
from pathlib import Path
from typing import Dict, Any
from detect_encoding_issues import detect_text_issues, make_diagnostic


def validate_hadith_file(path: str, report: Dict[str, Any]):
    report.setdefault("hadith_files", []).append(path)
    total = 0
    missing_fields = 0
    duplicates = 0
    seen_ids = set()
    encoding_diagnostics = []

    out_norm_dir = Path(report.get("output_dir", "content_pipeline/output/normalized"))
    out_norm_dir.mkdir(parents=True, exist_ok=True)
    base = Path(path).stem
    raw_out = out_norm_dir / f"raw_hadith_{base}.ndjson"
    norm_out = out_norm_dir / f"hadith_{base}.ndjson"

    with open(path, "rb") as fh, raw_out.open("w", encoding="utf-8") as rawf, norm_out.open("w", encoding="utf-8") as normf:
        try:
            items = ijson.items(fh, "hadeeth.item")
        except Exception:
            fh.seek(0)
            data = json.load(fh)
            items = iter(data.get("hadeeth", []))

        for idx, rec in enumerate(items):
            total += 1
            rawf.write(json.dumps(rec, ensure_ascii=False) + "\n")
            rid = rec.get("record_id") or rec.get("id")
            if rid in seen_ids:
                duplicates += 1
                report.setdefault("hadith_duplicates", []).append({"file": path, "index": idx, "record_id": rid})
            else:
                seen_ids.add(rid)
            if not rec.get("arabic_with_diacritics") and not rec.get("arabic_without_diacritics"):
                missing_fields += 1
                report.setdefault("hadith_missing_examples", []).append({"file": path, "index": idx, "record": rec})
            # encoding checks on translation texts
            trans = rec.get("translations") or []
            for t in trans:
                text = t.get("hadees") or t.get("text") or t.get("text_ur")
                if isinstance(text, str):
                    issues = detect_text_issues(text[:1000])
                    for iss in issues:
                        encoding_diagnostics.append(make_diagnostic(path, idx, "translation", text[:200], iss))
            normf.write(json.dumps({"_source_file": path, "_source_index": idx, "record": rec}, ensure_ascii=False) + "\n")

    report.setdefault("hadith_stats", {})[path] = {"total": total, "duplicates": duplicates, "missing_fields": missing_fields}
    if encoding_diagnostics:
        report.setdefault("encoding_diagnostics", []).extend(encoding_diagnostics)


if __name__ == "__main__":
    print("validate_hadith.py module")
