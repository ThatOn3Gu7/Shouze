#!/usr/bin/env python3
"""Generate grouped release notes from git commits.

Usage: gen_release_notes.py [<git-range>]
If no range is given, all commits are used.
Commits are grouped by conventional-commit prefix into:
  Added (feat/add), Fixed (fix), Improved (perf/refactor/improve),
  Removed (remove/chore), Docs (docs), Other.
"""
import subprocess
import sys


def git_log(rng: str) -> list[str]:
    args = ["git", "log", "--pretty=format:%s (%h)"]
    if rng:
        args.append(rng)
    out = subprocess.run(args, capture_output=True, text=True).stdout
    return [l.strip() for l in out.splitlines() if l.strip()]


def classify(lines: list[str]) -> dict:
    sections = {
        "Added": [],
        "Fixed": [],
        "Improved": [],
        "Removed": [],
        "Docs": [],
        "Other": [],
    }
    for line in lines:
        low = line.lower()
        if low.startswith("feat") or low.startswith("add"):
            sections["Added"].append(line)
        elif low.startswith("fix"):
            sections["Fixed"].append(line)
        elif low.startswith("perf") or low.startswith("refactor") or low.startswith("improve"):
            sections["Improved"].append(line)
        elif low.startswith("remove") or low.startswith("chore"):
            sections["Removed"].append(line)
        elif low.startswith("docs"):
            sections["Docs"].append(line)
        else:
            sections["Other"].append(line)
    return sections


ICONS = {
    "Added": "✨",
    "Fixed": "🐛",
    "Improved": "♻️",
    "Removed": "🔥",
    "Docs": "📝",
    "Other": "🔧",
}


def main() -> None:
    rng = sys.argv[1] if len(sys.argv) > 1 and sys.argv[1] else ""
    lines = git_log(rng)
    sections = classify(lines)

    print("## What's Changed\n")
    any_ = False
    for name, items in sections.items():
        if items:
            any_ = True
            print(f"### {ICONS[name]} {name}\n")
            for it in items:
                print(f"- {it}")
            print()
    if not any_:
        print("_No notable changes in this release._\n")

    print("---\n")
    print("Full changelog is available by comparing this release with the previous tag.")


if __name__ == "__main__":
    main()
