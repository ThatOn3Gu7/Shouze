#!/usr/bin/env python3
"""Generate release notes from the commits in the given range (e.g. v1.3.0..HEAD)."""
import subprocess
import sys

repo = "ThatOn3Gu7/Shouze"
range_arg = sys.argv[1] if len(sys.argv) > 1 else ""

proc = subprocess.run(
    ["git", "log", "--oneline"] + ([range_arg] if range_arg else []),
    capture_output=True,
    text=True,
)
if proc.returncode != 0:
    sys.stderr.write(proc.stderr)
    sys.exit(proc.returncode)

commits = [line for line in proc.stdout.splitlines() if line.strip()]

print("## What's Changed")
print()
if not commits:
    print("No commits in this release.")
else:
    for line in commits:
        print(f"- {line}")
print()

if range_arg and ".." in range_arg:
    base, head = range_arg.split("..", 1)
    print(f"**Full Changelog**: https://github.com/{repo}/compare/{base}...{head}")