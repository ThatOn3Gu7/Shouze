#!/usr/bin/env python3
"""
Decides whether a pushed commit needs an Android build.

Strategy: allow-list of paths that can affect the app build. Everything else
(docs, CI config, markdown, ...) skips the expensive build job.

Fail-open philosophy: whenever we cannot reliably determine the changed set
(new branch, force push, missing base commit), we BUILD. A wasted run is far
cheaper than a silently skipped broken build.

Inputs (env):
  BEFORE  - github.event.before (may be empty or all zeros on branch creation)
  AFTER   - github.sha
Outputs (GitHub Actions):
  should_build - "true" | "false"
"""

import fnmatch
import os
import subprocess
import sys

# Paths whose change can affect the built application.
BUILD_RELEVANT_PATTERNS = [
    "app/src/**",            # Kotlin/Java sources, res, assets, AndroidManifest
    "app/*.gradle.kts",      # app module build script
    "app/proguard-rules.pro",
    "build.gradle.kts",      # root build script
    "settings.gradle.kts",
    "gradle.properties",
    "gradle.properties.example",
    "gradle/**",             # version catalog (libs.versions.toml) + wrapper jar/properties
    "gradlew",
    "gradlew.bat",
]

NULL_SHA_PREFIX = "0000000000000000000000000000000000000000"


def git(*args: str) -> str:
    return subprocess.run(
        ["git", *args], capture_output=True, text=True, check=False
    ).stdout.strip()


def commit_exists(sha: str) -> bool:
    if not sha or sha == NULL_SHA_PREFIX:
        return False
    result = subprocess.run(
        ["git", "cat-file", "-e", f"{sha}^{{commit}}"],
        capture_output=True, text=True, check=False,
    )
    return result.returncode == 0


def changed_files(before: str, after: str) -> list[str] | None:
    """Returns changed file names between two commits, or None if undeterminable."""
    if not commit_exists(before) or not commit_exists(after):
        return None
    result = subprocess.run(
        ["git", "diff", "--name-only", f"{before}..{after}"],
        capture_output=True, text=True, check=False,
    )
    if result.returncode != 0:
        return None
    return [line.strip() for line in result.stdout.splitlines() if line.strip()]


def is_relevant(files: list[str]) -> bool:
    for path in files:
        for pattern in BUILD_RELEVANT_PATTERNS:
            if fnmatch.fnmatch(path, pattern):
                return True
    return False


def main() -> int:
    before = os.environ.get("BEFORE", "").strip()
    after = os.environ.get("AFTER", "").strip()

    files = changed_files(before, after)
    if files is None:
        # Undeterminable diff (branch creation, force push, shallow fetch): build.
        should_build = True
        print("Could not determine changed files - fail-open: building.")
    else:
        should_build = is_relevant(files)
        print(f"Changed files ({len(files)}):")
        for path in files[:50]:
            print(f"  {path}")
        if len(files) > 50:
            print(f"  ... and {len(files) - 50} more")
        print(f"Build-relevant change detected: {should_build}")

    with open(os.environ["GITHUB_OUTPUT"], "a") as out:
        out.write(f"should_build={'true' if should_build else 'false'}\n")
    return 0


if __name__ == "__main__":
    sys.exit(main())
