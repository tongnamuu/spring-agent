#!/usr/bin/env python3
"""Prevent commands from bypassing the approval rule for `gh pr merge`."""

import json
import re
import sys


DIRECT_MERGE = re.compile(r"^\s*gh\s+pr\s+merge(?:\s|$)")
ANY_MERGE = re.compile(r"\bgh\s+pr\s+merge(?:\s|$)")
GH_API = re.compile(r"\bgh\s+api(?:\s|$)")
API_MERGE = re.compile(r"(?:/pulls/[^\s]+/merge\b|\bmergePullRequest\b)", re.IGNORECASE)


def deny(reason: str) -> None:
    print(
        json.dumps(
            {
                "hookSpecificOutput": {
                    "hookEventName": "PreToolUse",
                    "permissionDecision": "deny",
                    "permissionDecisionReason": reason,
                }
            }
        )
    )


def main() -> None:
    payload = json.load(sys.stdin)
    tool_input = payload.get("tool_input") or {}
    command = tool_input.get("command") or tool_input.get("cmd") or ""

    if not isinstance(command, str):
        return

    if GH_API.search(command) and API_MERGE.search(command):
        deny(
            "PR merge through `gh api` is blocked. Use `gh pr merge ...` directly "
            "so the repository approval rule can ask the user for explicit approval."
        )
        return

    if not ANY_MERGE.search(command):
        return

    if DIRECT_MERGE.match(command):
        # The exact command is handled by `.codex/rules/pr-merge.rules`, which
        # forces a user approval prompt for every invocation.
        return

    deny(
        "PR merge blocked: run `gh pr merge ...` directly so the repository "
        "approval rule can ask the user for explicit approval."
    )


if __name__ == "__main__":
    main()
