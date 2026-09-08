#!/usr/bin/env python3
"""
Bridges the Android build to the PR as a readable, AI-friendly comment.

- On failure: extracts compile errors, the Gradle failure summary and a bounded
  log tail from the raw build log and posts/updates ONE canonical PR comment
  (identified by an HTML marker) so repeated pushes never spam the PR.
- On success: updates the same comment to a success state (or creates it), so a
  stale failure report can never mislead the next reader.

Env inputs:
  GITHUB_TOKEN, GITHUB_REPOSITORY
  PR_NUMBER        - PR to comment on; empty = skip commenting (e.g. push to main)
  BUILD_STATUS     - "success" | "failure"
  RUN_URL          - URL of the Actions run (log link)
  COMMIT_SHA, COMMIT_SUBJECT
  LOG_FILE         - path to the raw gradle log
"""

import os
import re
import subprocess
import sys

MARKER = "<!-- shouze-android-ci-report -->"
MAX_REPORT_CHARS = 35000          # stay well under GitHub's 65536 comment limit
MAX_COMPILE_ERRORS = 40
MAX_TAIL_LINES = 60
MAX_FAILURE_BLOCK_LINES = 40

# ---------------------------------------------------------------------------
# Log parsing
# ---------------------------------------------------------------------------

# Kotlin 2.x:  e: file:///path/File.kt:12:5 Unresolved reference: foo
KOTLIN_ERROR_RE = re.compile(
    r"^e:\s+(?:file://)?(?P<file>.+?):(?P<line>\d+):(?P<col>\d+)\s+(?P<msg>.+)$"
)
# Kotlin 1.x style:  e: file:///path/File.kt: (12, 5): Unresolved reference
KOTLIN_ERROR_LEGACY_RE = re.compile(
    r"^e:\s+(?:file://)?(?P<file>.+?):\s*\((?P<line>\d+),\s*(?P<col>\d+)\):\s+(?P<msg>.+)$"
)
# KSP (Room etc.):  e: [ksp] file:///path/File.kt:12:5 The error text
KSP_ERROR_RE = re.compile(
    r"^e:\s+\[ksp\]\s+(?:file://)?(?P<file>.+?):(?P<line>\d+):(?P<col>\d+)\s+(?P<msg>.+)$"
)
# javac:  path/File.java:42: error: message
JAVA_ERROR_RE = re.compile(
    r"^(?P<file>\S+\.java):(?P<line>\d+):\s*error:\s*(?P<msg>.+)$"
)
FAILED_TASK_RE = re.compile(r"^>\s+Task\s+(?P<task>:\S+)\s+FAILED")
STACK_FRAME_RE = re.compile(r"^\s*at\s+[\w$.$]+\(")
MORE_MARKER_RE = re.compile(r"^\s*\.\.\. \d+ more")

def strip_runners_prefix(path: str) -> str:
    """Collapse absolute CI paths to repo-relative for readability."""
    for marker in ("/Shouze/", "/app/", "app/src/"):
        if marker in path:
            if marker == "/Shouze/":
                # rsplit: repo checkouts nest as .../work/<repo>/<repo>/...
                return path.rsplit("/Shouze/", 1)[1]
            if marker == "/app/":
                return "app/" + path.rsplit("/app/", 1)[1]
    return path

def extract_compile_errors(log_lines: list[str]) -> list[dict]:
    errors: list[dict] = []
    seen: set[tuple] = set()
    for line in log_lines:
        match = (
            KSP_ERROR_RE.match(line)
            or KOTLIN_ERROR_RE.match(line)
            or KOTLIN_ERROR_LEGACY_RE.match(line)
            or JAVA_ERROR_RE.match(line)
        )
        if not match:
            continue
        file = strip_runners_prefix(match.group("file"))
        line_no = match.group("line")
        msg = match.group("msg").strip()
        key = (file, line_no, msg)
        if key in seen:
            continue
        seen.add(key)
        errors.append({"file": file, "line": line_no, "message": msg})
        if len(errors) >= MAX_COMPILE_ERRORS * 3:  # hard stop to bound work
            break
    return errors[:MAX_COMPILE_ERRORS]

def extract_gradle_failure_block(log_lines: list[str]) -> str:
    """The 'FAILURE: Build failed with an exception.' explanation block."""
    try:
        start = next(
            i for i, l in enumerate(log_lines)
            if l.strip().startswith("FAILURE: Build failed with an exception")
        )
    except StopIteration:
        return ""
    block = []
    for line in log_lines[start:]:
        if line.strip() in ("* Try:", "BUILD FAILED", "* Exception is:"):
            break
        block.append(line)
        if len(block) >= MAX_FAILURE_BLOCK_LINES:
            break
    return "\n".join(block).strip()

def extract_failed_tasks(log_lines: list[str]) -> list[str]:
    tasks = []
    for line in log_lines:
        m = FAILED_TASK_RE.match(line)
        if m and m.group("task") not in tasks:
            tasks.append(m.group("task"))
    return tasks

def extract_build_result_line(log_lines: list[str]) -> str:
    for line in reversed(log_lines):
        if line.strip().startswith(("BUILD FAILED", "BUILD SUCCESSFUL")):
            return line.strip()
    return ""

def extract_log_tail(log_lines: list[str], count: int = MAX_TAIL_LINES) -> str:
    """Last interesting lines: stack frames from --stacktrace are noise, drop them."""
    interesting = [
        l for l in log_lines
        if not STACK_FRAME_RE.match(l) and not MORE_MARKER_RE.match(l)
    ]
    return "\n".join(interesting[-count:]).strip()


def extract_raw_error_lines(log_lines: list[str], limit: int = 40) -> list[str]:
    """Catch-all for error-ish lines in formats the structured parsers don't know
    (KSP variants, Room validation errors, dependency failures, ...)."""
    results: list[str] = []
    seen: set[str] = set()
    for line in log_lines:
        stripped = line.strip()
        if not stripped or STACK_FRAME_RE.match(line) or MORE_MARKER_RE.match(line):
            continue
        lowered = stripped.lower()
        if ("error" in lowered or lowered.startswith("e:") or lowered.startswith("w: [ksp]")) \
                and "warning" not in lowered[:12]:
            if stripped not in seen:
                seen.add(stripped)
                results.append(stripped)
        if len(results) >= limit:
            break
    return results

# ---------------------------------------------------------------------------
# Report rendering
# ---------------------------------------------------------------------------

def render_failure_report(
    errors: list[dict],
    raw_error_lines: list[str],
    failure_block: str,
    failed_tasks: list[str],
    result_line: str,
    log_tail: str,
    run_url: str,
    commit_sha: str,
    commit_subject: str,
) -> str:
    short_sha = commit_sha[:8] if commit_sha else "unknown"
    parts = [MARKER, "## ❌ Android CI — build failed", ""]
    parts.append(f"**Commit:** `{short_sha}` — {commit_subject or '(no subject)'}")
    if failed_tasks:
        parts.append(f"**Failing task(s):** {', '.join(f'`{t}`' for t in failed_tasks)}")
    if result_line:
        parts.append(f"**Gradle result:** `{result_line}`")
    parts.append("")

    if errors:
        parts.append(f"### 🔍 Extracted errors ({len(errors)} shown)")
        parts.append("")
        for err in errors:
            parts.append(f"- `{err['file']}:{err['line']}` — {err['message']}")
        parts.append("")
    else:
        parts.append(
            "_No compile errors were parsed from the log. The failure is likely a "
            "task/dependency/configuration failure — see the Gradle summary below._"
        )
        parts.append("")

    if raw_error_lines:
        parts.append("<details>")
        parts.append("<summary><strong>Other error lines from the log</strong></summary>")
        parts.append("")
        parts.append("```")
        parts.append("\n".join(raw_error_lines))
        parts.append("```")
        parts.append("")
        parts.append("</details>")
        parts.append("")

    if failure_block:
        parts.append("<details>")
        parts.append("<summary><strong>Gradle failure summary</strong> (What went wrong)</summary>")
        parts.append("")
        parts.append("```")
        parts.append(failure_block)
        parts.append("```")
        parts.append("")
        parts.append("</details>")
        parts.append("")

    if log_tail:
        parts.append("<details>")
        parts.append("<summary><strong>Last log lines</strong></summary>")
        parts.append("")
        parts.append("```")
        parts.append(log_tail)
        parts.append("```")
        parts.append("")
        parts.append("</details>")
        parts.append("")

    if run_url:
        parts.append(f"🔗 [Full CI run log]({run_url})")
    parts.append("")
    parts.append(
        "🛠 **Debugging hints:** errors above are ordered by appearance — fix the "
        "first ones first (later failures are often cascades). Files/lines are "
        "repo-relative. Pushing a new commit automatically re-runs this build and "
        "updates this comment."
    )
    return "\n".join(parts)

def render_success_report(
    run_url: str, commit_sha: str, commit_subject: str, had_previous_failure: bool
) -> str:
    short_sha = commit_sha[:8] if commit_sha else "unknown"
    parts = [MARKER, "## ✅ Android CI — build passed", ""]
    parts.append(f"**Commit:** `{short_sha}` — {commit_subject or '(no subject)'}")
    parts.append("")
    parts.append(
        "Debug APK and unit tests built successfully. "
        "Download the APK from this run's **Artifacts** section (`Shouze-debug-apk`)."
    )
    if had_previous_failure:
        parts.append("")
        parts.append("_Earlier failure report in this thread has been resolved._")
    if run_url:
        parts.append("")
        parts.append(f"🔗 [CI run]({run_url})")
    return "\n".join(parts)

# ---------------------------------------------------------------------------
# GitHub API
# ---------------------------------------------------------------------------

def gh(*args: str) -> subprocess.CompletedProcess:
    env = os.environ.copy()
    return subprocess.run(
        ["gh", *args], capture_output=True, text=True, check=False, env=env
    )

def find_existing_comment(repo: str, pr_number: str) -> str | None:
    result = gh(
        "api", "--paginate",
        f"repos/{repo}/issues/{pr_number}/comments",
        "--jq",
        f'.[] | select(.body | contains("{MARKER}")) | .id',
    )
    if result.returncode != 0:
        return None
    ids = [line.strip() for line in result.stdout.splitlines() if line.strip()]
    return ids[-1] if ids else None

def create_comment(repo: str, pr_number: str, body: str) -> bool:
    result = gh("api", f"repos/{repo}/issues/{pr_number}/comments", "-f", f"body={body}")
    if result.returncode != 0:
        print(f"Failed to create comment: {result.stderr}", file=sys.stderr)
        return False
    return True

def update_comment(repo: str, comment_id: str, body: str) -> bool:
    result = gh("api", "-X", "PATCH", f"repos/{repo}/issues/comments/{comment_id}", "-f", f"body={body}")
    if result.returncode != 0:
        print(f"Failed to update comment: {result.stderr}", file=sys.stderr)
        return False
    return True

# ---------------------------------------------------------------------------

def main() -> int:
    repo = os.environ["GITHUB_REPOSITORY"]
    pr_number = os.environ.get("PR_NUMBER", "").strip()
    build_status = os.environ.get("BUILD_STATUS", "failure").strip()
    run_url = os.environ.get("RUN_URL", "").strip()
    commit_sha = os.environ.get("COMMIT_SHA", "").strip()
    # head_commit.message can be multi-line; keep only the subject line.
    commit_subject = os.environ.get("COMMIT_SUBJECT", "").strip().splitlines()
    commit_subject = commit_subject[0].strip() if commit_subject else ""
    log_file = os.environ.get("LOG_FILE", "").strip()

    try:
        with open(log_file, "r", errors="replace") as fh:
            log_lines = fh.read().splitlines()
    except OSError:
        log_lines = ["(build log unavailable)"]

    if not pr_number:
        print("No associated open PR — skipping PR comment (build status is still enforced).")
        return 0

    existing_id = find_existing_comment(repo, pr_number)

    if build_status == "success":
        body = render_success_report(run_url, commit_sha, commit_subject, existing_id is not None)
    else:
        errors = extract_compile_errors(log_lines)
        raw_error_lines = extract_raw_error_lines(log_lines)
        failure_block = extract_gradle_failure_block(log_lines)
        failed_tasks = extract_failed_tasks(log_lines)
        result_line = extract_build_result_line(log_lines)
        log_tail = extract_log_tail(log_lines)
        body = render_failure_report(
            errors, raw_error_lines, failure_block, failed_tasks, result_line,
            log_tail, run_url, commit_sha, commit_subject,
        )

    if len(body) > MAX_REPORT_CHARS:
        body = body[:MAX_REPORT_CHARS] + "\n\n…(report truncated — see the full CI log)"

    if existing_id:
        ok = update_comment(repo, existing_id, body)
        action = "updated" if ok else "FAILED TO update"
    else:
        ok = create_comment(repo, pr_number, body)
        action = "created" if ok else "FAILED TO create"
    print(f"CI report comment {action} on PR #{pr_number}.")
    return 0

if __name__ == "__main__":
    sys.exit(main())
