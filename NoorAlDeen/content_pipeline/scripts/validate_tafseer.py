import json
import ijson
from pathlib import Path
from typing import Dict, Any
from detect_encoding_issues import detect_text_issues, make_diagnostic


def validate_tafseer_file(path: str, report: Dict[str, Any]):
    report.setdefault("tafseer_files", []).append(path)
    total = 0
    missing_fields = 0
    encoding_diagnostics = []

    out_norm_dir = Path(report.get("output_dir", "content_pipeline/output/normalized"))
    out_norm_dir.mkdir(parents=True, exist_ok=True)
    base = Path(path).stem
    raw_out = out_norm_dir / f"raw_tafseer_{base}.ndjson"
    norm_out = out_norm_dir / f"tafseer_{base}.ndjson"

    with open(path, "rb") as fh, raw_out.open("w", encoding="utf-8") as rawf, norm_out.open("w", encoding="utf-8") as normf:
        try:
            items = ijson.items(fh, "item")
        except Exception:
            fh.seek(0)
            data = json.load(fh)
            items = iter(data)

        for idx, rec in enumerate(items):
            total += 1
            rawf.write(json.dumps(rec, ensure_ascii=False) + "\n")
            # Expect mapping to surah/ayah or generic translation mapping
            if not (rec.get("surat_id") or rec.get("surah_id") or rec.get("reference")):
                missing_fields += 1
                report.setdefault("tafseer_missing_examples", []).append({"file": path, "index": idx, "record": rec})
            # detect encoding in large text fields
            for k, v in rec.items():
                if isinstance(v, str) and len(v) > 50:
                    issues = detect_text_issues(v[:500])
                    for iss in issues:
                        encoding_diagnostics.append(make_diagnostic(path, idx, k, v[:200], iss))
            normf.write(json.dumps({"_source_file": path, "_source_index": idx, "record": rec}, ensure_ascii=False) + "\n")

    report.setdefault("tafseer_stats", {})[path] = {"total": total, "missing_fields": missing_fields}
    if encoding_diagnostics:
        report.setdefault("encoding_diagnostics", []).extend(encoding_diagnostics)


if __name__ == "__main__":
    print("validate_tafseer.py module")
