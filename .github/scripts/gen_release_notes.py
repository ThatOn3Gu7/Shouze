#!/usr/bin/env python3
"""Generate release notes from the commits in the given range (e.g. v1.3.0..HEAD)."""
import subprocess
import sys

repo = "ThatOn3Gu7/Shouze"
range_arg = sys.argv[1] if len(sys.argv) > 1 else ""

# We use a unique separator to reliably parse multi-line commit bodies
separator = "===COMMIT_SEP==="

# %h = short hash, %s = subject (header), %b = body (description)
proc = subprocess.run(
    ["git", "log", f"--format={separator}%n%h%n%s%n%b"] + ([range_arg] if range_arg else []),
    capture_output=True,
    text=True,
)
if proc.returncode != 0:
    sys.stderr.write(proc.stderr)
    sys.exit(proc.returncode)

raw_output = proc.stdout.strip()
commits = []

if raw_output:
    # Split the output by our separator and drop the first empty item
    raw_commits = raw_output.split(f"{separator}\n")[1:]
    for raw_commit in raw_commits:
        # Split into at most 3 parts: hash, subject, and the rest (body)
        parts = raw_commit.split("\n", 2)
        if len(parts) >= 2:
            c_hash = parts[0].strip()
            c_subject = parts[1].strip()
            # The body might be empty, so we check the length
            c_body = parts[2].strip() if len(parts) > 2 else ""
            commits.append((c_hash, c_subject, c_body))

print("## What's Changed")
print()

if not commits:
    print("No commits in this release.")
else:
    for c_hash, c_subject, c_body in commits:
        print(f"- {c_hash} {c_subject}")
        
        # Only add the dropdown if there's actually a description
        if c_body:
            # We indent the HTML tags by 2 spaces so Markdown recognizes it as part of the list item
            print("  <details>")
            print("  <summary>Click to see commit description</summary>")
            print()
            # Indent each line of the body to keep it aligned within the dropdown
            for line in c_body.splitlines():
                print(f"  {line}")
            print()
            print("  </details>")

print()

if range_arg and ".." in range_arg:
    base, head = range_arg.split("..", 1)
    print(f"**Full Changelog**: https://github.com/{repo}/compare/{base}...{head}")
