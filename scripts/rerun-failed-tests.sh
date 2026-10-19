#!/usr/bin/env bash
# Runs the suite, automatically reruns any failed Cucumber scenarios once,
# then merges both reports into a single reconciled HTML report.
#
# Exit code reflects the FINAL (post-rerun) result, not the first run —
# a scenario that fails on the first try and passes on rerun counts as passed
# (one-off failure); a scenario that fails both times is a real failure.
set -uo pipefail

RERUN_FILE="target/rerun.txt"
REPORTS_DIR="target/cucumber-reports"

echo "== Run 1: full suite =="
rm -f "$RERUN_FILE"
mvn test "$@"
# testFailureIgnore=true in pom.xml means this always exits 0; real pass/fail
# is decided below by inspecting the JSON output directly.

FINAL_JSON="$REPORTS_DIR/run1.json"

if [ -s "$RERUN_FILE" ]; then
    echo "== Run 2: rerunning failed scenarios listed in $RERUN_FILE =="
    mvn test -Dsuite.file=src/testng-rerun.xml "$@"
    FINAL_JSON="$REPORTS_DIR/run2.json"
else
    echo "== No failures on run 1 — skipping rerun =="
    # Merge step always expects both files; treat run1 as final when no rerun happened.
    cp "$REPORTS_DIR/run1.json" "$REPORTS_DIR/run2.json"
fi

echo "== Merging reports (for human-readable HTML output) =="
mvn net.masterthought:maven-cucumber-reporting:5.8.1:generate
echo "Merged report: target/cucumber-reports/merged/cucumber-html-reports/overview-features.html"

echo "== Determining final result from $FINAL_JSON =="
if grep -q '"status":"failed"' "$FINAL_JSON"; then
    echo "RESULT: FAILED — scenario(s) still failing after rerun."
    exit 1
fi

echo "RESULT: PASSED (one-off failures, if any, were resolved by the rerun)."
exit 0
