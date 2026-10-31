import json
import glob
import os
from pathlib import Path
from typing import Dict, Any

from validate_quran import validate_quran_file
from validate_hadith import validate_hadith_file
from validate_tafseer import validate_tafseer_file
from generate_report import write_reports


def expand_glob_list(patterns):
    files = []
    for p in patterns:
        # allow workspace-relative globs
        files.extend(sorted(glob.glob(p, recursive=True)))
    return files


def main():
    root = Path(__file__).resolve().parents[1]
    cfg_path = root / "config" / "sources.json"
    with cfg_path.open("r", encoding="utf-8") as f:
        cfg = json.load(f)

    output_dir = Path(cfg.get("output_dir", "content_pipeline/output"))
    report: Dict[str, Any] = {"files_processed": [], "output_dir": str(output_dir)}
    output_dir.mkdir(parents=True, exist_ok=True)

    # process quran
    q_patterns = cfg.get("quran", [])
    q_files = expand_glob_list(q_patterns)
    for q in q_files:
        if not os.path.exists(q):
            report.setdefault("missing_files", []).append(q)
            continue
        report.setdefault("files_processed", []).append(q)
        print(f"Validating Quran file: {q}")
        validate_quran_file(q, report)

    # tafseers
    t_patterns = cfg.get("tafseers", [])
    t_files = expand_glob_list(t_patterns)
    for t in t_files:
        if not os.path.exists(t):
            report.setdefault("missing_files", []).append(t)
            continue
        report.setdefault("files_processed", []).append(t)
        print(f"Validating Tafseer file: {t}")
        validate_tafseer_file(t, report)

    # hadiths
    h_patterns = cfg.get("hadiths", [])
    h_files = expand_glob_list(h_patterns)
    for h in h_files:
        if not os.path.exists(h):
            report.setdefault("missing_files", []).append(h)
            continue
        report.setdefault("files_processed", []).append(h)
        print(f"Validating Hadith file: {h}")
        validate_hadith_file(h, report)

    write_reports(report, str(output_dir / "reports"))


if __name__ == "__main__":
    main()
