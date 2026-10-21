#!/usr/bin/env sh
# Conventional Commits check for the commit-msg hook (and CI on PR titles).
# Format: type(scope)?: subject   — e.g. feat(core): add request wrapper
msg_file="$1"
# Only the author appears on GitHub: refuse co-author and session trailers added by any tool.
if grep -qiE 'co-authored-by|claude-session' "$msg_file"; then
  echo "commit-msg: remove the Co-Authored-By / Claude-Session line; commits carry the author only."
  exit 1
fi
first_line=$(head -n 1 "$msg_file")
pattern='^(feat|fix|docs|style|refactor|perf|test|build|ci|chore|revert)(\([a-z0-9._-]+\))?!?: .{1,100}$'
case "$first_line" in
  Merge*|Revert*) exit 0 ;;
esac
if ! printf '%s' "$first_line" | grep -Eq "$pattern"; then
  echo "commit-msg: '$first_line' is not a Conventional Commit."
  echo "            expected: type(scope): subject   e.g. feat(core): add request wrapper"
  exit 1
fi
