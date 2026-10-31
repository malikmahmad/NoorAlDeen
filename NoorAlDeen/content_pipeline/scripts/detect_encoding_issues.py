import re
from typing import List, Dict, Any

MOJIBAKE_PATTERNS = [
    re.compile(r"\ufffd"),
    re.compile(r"Ã"),
    re.compile(r"Â"),
]


def detect_text_issues(text: str) -> List[Dict[str, Any]]:
    issues = []
    if text is None:
        return issues
    # Replacement character
    if "\ufffd" in text:
        issues.append({"issue": "replacement_char", "confidence": 0.9, "note": "\uFFFD present"})
    # Common mojibake markers (heuristic)
    mojibake_hits = []
    for p in MOJIBAKE_PATTERNS:
        if p.search(text):
            mojibake_hits.append(p.pattern)
    if mojibake_hits:
        issues.append({"issue": "possible_mojibake", "patterns": mojibake_hits, "confidence": 0.6})

    # Suspicious high-ascii/garble heuristic: many consecutive non-letter symbols
    if re.search(r"[\x80-\xFF]{4,}", text):
        issues.append({"issue": "high_byte_run", "confidence": 0.5})

    return issues


def make_diagnostic(file: str, record_index: int, field: str, value: str, issue: dict):
    return {
        "file": file,
        "record_index": record_index,
        "field": field,
        "original_value": value,
        "suspected_issue": issue.get("issue"),
        "details": issue,
        "suggested_fix": None,
        "confidence": issue.get("confidence", 0.5),
    }


if __name__ == "__main__":
    print("detect_encoding_issues module - helper only")
