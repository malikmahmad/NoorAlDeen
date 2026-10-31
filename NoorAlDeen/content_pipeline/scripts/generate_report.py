import json
from pathlib import Path


def write_reports(report: dict, output_dir: str):
    out = Path(output_dir)
    out.mkdir(parents=True, exist_ok=True)
    rep_json = out / "validation_report.json"
    rep_md = out / "validation_report.md"
    with rep_json.open("w", encoding="utf-8") as f:
        json.dump(report, f, ensure_ascii=False, indent=2)

    with rep_md.open("w", encoding="utf-8") as f:
        f.write("# Validation Report\n\n")
        f.write(f"Files processed: {len(report.get('files_processed', []))}\n\n")
        if report.get("quran_stats"):
            f.write("## Quran Stats\n")
            for k, v in report.get("quran_stats").items():
                f.write(f"- {k}: {v}\n")
        if report.get("hadith_stats"):
            f.write("## Hadith Stats\n")
            for k, v in report.get("hadith_stats").items():
                f.write(f"- {k}: {v}\n")
        if report.get("encoding_diagnostics"):
            f.write("\n## Encoding Diagnostics (sample)\n")
            for d in report.get("encoding_diagnostics")[:20]:
                f.write(f"- {d}\n")

    print(f"Wrote reports to {out}")


if __name__ == "__main__":
    print("generate_report module")
