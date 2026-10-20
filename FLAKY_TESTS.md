# Flaky Test Management

A process for detecting, quarantining, and eventually fixing unstable
scenarios, so flakiness degrades visibility instead of silently eroding
trust in CI (or getting "fixed" by deleting coverage).

## 1. Detect

A scenario is a flakiness **candidate** once it fails intermittently with no
code change in between — e.g. it fails on run 1, passes on the automatic
rerun (`scripts/rerun-failed-tests.sh`), with no corresponding app/test
change. A single rerun-recovery isn't proof by itself; recurring
rerun-recoveries for the *same* scenario over multiple runs are.

Two places make this visible:
- The CI job summary log — `rerun-failed-tests.sh` prints which scenarios
  were retried on run 2.
- The Allure report's **Trend** graphs (History tab / the trend charts on
  the overview page) — see [README.md "Running in Docker"](README.md) for
  where the report is published. Recurring red on the trend graph for a
  scenario that the app owner confirms is "supposed to pass" is the signal
  to quarantine it.

## 2. Quarantine — tag it `@flaky`

Add `@flaky` alongside its platform tag (see `GHERKIN_GUIDE.md`):

```gherkin
@web @flaky
Scenario: ...
```

What `@flaky` does today:
- The scenario **still runs** every time (quarantine isn't the same as
  skipping it — we want to keep collecting trend data to know when it's
  actually fixed).
- Its failures are **excluded from the pass-rate quality gate** in
  `.github/workflows/ci.yml` (reported separately in the job summary as
  "quarantined @flaky failure(s)", never blocking the build).
- It still counts toward the `@critical` check if also tagged `@critical`
  — a scenario should never carry both tags at once; if it does, that's a
  contradiction to resolve, not a gap to quietly let through.
- It's excluded from `-Pregression` runs that use `not @wip` only if also
  tagged `@wip`; otherwise it keeps running in every profile that matches
  its platform/suite tags. If a flaky scenario is disruptive enough to want
  excluded entirely while being fixed, tag it `@wip` as well.

**Quarantine is not a fix.** It buys time without blocking unrelated PRs —
it doesn't resolve the underlying instability.

## 3. Track

The Allure report (published to GitHub Pages, see `README.md`) carries
history/trend data forward between CI runs — the `test` job downloads the
previous run's `history/*.json` before generating a new report, so trend
graphs accumulate over time instead of resetting on every run. Use the
trend graphs to see whether a `@flaky` scenario's failure rate is
improving, stable, or getting worse.

## 4. Review and fix root causes

Hold a recurring review (suggested cadence: every two weeks, or monthly if
the quarantine list is short) covering every currently `@flaky`-tagged
scenario:

1. Pull the list: `grep -rn "@flaky" src/test/resources/features/`
2. For each one, check its Allure trend — is it still failing intermittently?
3. Decide, per scenario:
   - **Fixed** — root cause addressed (bad wait, test-data collision,
     environment timing, etc.) → remove `@flaky`.
   - **Still flaky, root cause identified, fix in progress** → leave tagged,
     note the owner/issue link in the scenario (a comment line above it, or
     a linked tracking issue).
   - **Still flaky, no clear cause yet** → stays quarantined; flag for
     deeper investigation (add logging, run repeatedly in isolation via
     `-Dcucumber.filter.tags="@flaky"`, etc.).
4. Scenarios that have been `@flaky` for a long time with no progress are a
   signal to escalate, not to leave quarantined indefinitely — a standing
   `@flaky` tag with no owner just becomes permanent, silent skipped
   coverage.

Common root causes worth checking first: fixed `Thread.sleep()`-style
timing assumptions (should already be impossible — see the Checkstyle ban),
shared test data/accounts colliding across parallel threads
(`parallel="methods"`/`thread-count` — see README.md), and
environment-dependent timing on `qa`/`staging` vs. `dev`.
