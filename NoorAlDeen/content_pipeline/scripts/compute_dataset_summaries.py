#!/usr/bin/env python3
"""Compute dataset summaries (record counts, duplicates, missing content) and infer severity buckets.
Reads: content_pipeline/output/reports/validation_aggregates.json
Writes: prints a Markdown table and top-10 issues to stdout.
"""
import json
import os
import sys
from collections import Counter, defaultdict


def load_json(path):
    with open(path, 'r', encoding='utf-8') as f:
        return json.load(f)


def count_records(path):
    try:
        data = load_json(path)
        if isinstance(data, list):
            return len(data), data
        # common alternative: object with 'records' or 'data'
        for k in ('records', 'data', 'items'):
            if k in data and isinstance(data[k], list):
                return len(data[k]), data[k]
        # fallback: 1 (single object)
        return 1, data
    except Exception:
        return None, None


def summarize_quran_records(records):
    dup_counter = Counter()
    missing_content = 0
    basmalah = 0
    for r in records:
        sid = r.get('surat_id')
        ay = r.get('ayat_number')
        if ay == 0:
            basmalah += 1
        if sid is not None and ay is not None:
            dup_counter[(sid, ay)] += 1
        # missing arabic or empty translations
        if not r.get('arabic'):
            missing_content += 1
        translations = r.get('translations') or {}
        if isinstance(translations, dict) and all((v in (None, '', []) for v in translations.values())):
            missing_content += 1
    duplicates = sum(c-1 for c in dup_counter.values() if c > 1)
    return duplicates, missing_content, basmalah


def summarize_hadith_records(records):
    dup_counter = Counter()
    missing_content = 0
    for r in records:
        hid = r.get('hadith_id') or r.get('id')
        text = r.get('hadith_text') or r.get('text')
        if hid:
            dup_counter[hid] += 1
        if not text:
            missing_content += 1
    duplicates = sum(c-1 for c in dup_counter.values() if c > 1)
    return duplicates, missing_content


def main():
    base = os.path.join(os.path.dirname(__file__), '..', 'output', 'reports')
    agg_path = os.path.join(base, 'validation_aggregates.json')
    if not os.path.exists(agg_path):
        print('Missing aggregates:', agg_path, file=sys.stderr)
        sys.exit(1)

    aggregates = load_json(agg_path)

    results = {}
    issue_scores = []

    for fn in sorted(aggregates.keys()):
        src_path = os.path.normpath(os.path.join(os.path.dirname(__file__), '..', '..', fn))
        total_records, data = count_records(src_path)
        counts = aggregates.get(fn, {})
        total_diags = counts.get('total_diagnostics', 0)
        enc = counts.get('encoding_anomalies', 0)
        bas = counts.get('basmalah_ayat_number_zero', 0)

        duplicates = 0
        missing_content = 0

        if isinstance(data, list):
            # heuristics by folder name
            lower = fn.lower()
            if 'quran' in lower:
                duplicates, missing_content, bas_calc = summarize_quran_records(data)
                # prefer reported bas if present
                bas = bas or bas_calc
            elif 'kuttub' in lower or 'abu_dawood' in lower or 'bukhari' in lower or 'muslim' in lower:
                duplicates, missing_content = summarize_hadith_records(data)
        # infer severity buckets
        errors = counts.get('diag_category::missing_field_examples', 0)
        warnings = counts.get('diag_category::encoding_diagnostics', 0) or enc
        critical = counts.get('diag_category::critical', 0)
        info = counts.get('diag_category::info', 0)

        results[fn] = {
            'file': fn,
            'total_records': total_records if total_records is not None else 'N/A',
            'diagnostics': total_diags,
            'critical': critical,
            'errors': errors,
            'warnings': warnings,
            'info': info,
            'encoding_anomalies': enc,
            'duplicates': duplicates,
            'missing_content': missing_content,
            'basmalah_ayat_number_zero': bas,
        }

        # issue scoring (simple heuristic)
        score = errors * 5 + bas * 4 + warnings * 3 + duplicates * 5 + missing_content * 5
        issue_scores.append((score, fn, results[fn]))

    # print markdown table
    md_lines = []
    md_lines.append('| File | Total records | Diagnostics | Critical | Errors | Warnings | Info | Encoding anomalies | Duplicates | Missing content | Basmalah (ayat==0) |')
    md_lines.append('|---|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|')
    for fn, r in results.items():
        md_lines.append(f"| {fn} | {r['total_records']} | {r['diagnostics']} | {r['critical']} | {r['errors']} | {r['warnings']} | {r['info']} | {r['encoding_anomalies']} | {r['duplicates']} | {r['missing_content']} | {r['basmalah_ayat_number_zero']} |")

    print('\n'.join(md_lines))

    # top 10 issues
    issue_scores.sort(reverse=True, key=lambda x: x[0])
    print('\n\n# Top 10 Issues (scored)\n')
    for i, (score, fn, r) in enumerate(issue_scores[:10], start=1):
        desc = []
        if r['errors']:
            desc.append(f"{r['errors']} missing-field errors")
        if r['basmalah_ayat_number_zero']:
            desc.append(f"{r['basmalah_ayat_number_zero']} Basmalah (ayat_number==0)")
        if r['encoding_anomalies']:
            desc.append(f"{r['encoding_anomalies']} encoding anomalies")
        if r['duplicates']:
            desc.append(f"{r['duplicates']} duplicate records")
        if r['missing_content']:
            desc.append(f"{r['missing_content']} missing-content records")

        summary = '; '.join(desc) or 'No high-impact issues'
        print(f"{i}. {fn} — score={score}: {summary}")


if __name__ == '__main__':
    main()
