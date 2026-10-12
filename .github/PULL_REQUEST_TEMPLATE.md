## Summary
<!-- What does this PR do and why? -->

## Changes
- [ ] Test classes added/modified
- [ ] Framework/core classes added/modified (e.g., ReportManager, TestListener, utils)
- [ ] Config/dependency changes

## Testing
<!-- How did you verify this? e.g., ran suite locally, screenshots attached in report -->

## Checklist
- [ ] Tests pass locally
- [ ] No hardcoded credentials/secrets
- [ ] Updated relevant documentation/comments
- [ ] Linked related issue (if any)

<!--
EXAMPLE 1: Small change (few files)

## Summary
Fixed NullPointerException in ReportManager when test execution has no screenshots attached.

## Changes
- [x] Test classes added/modified
- [x] Framework/core classes added/modified (e.g., ReportManager, TestListener, utils)
- [ ] Config/dependency changes

## Testing
Ran LoginTest and CheckoutTest suites locally (12 tests), confirmed report generates correctly with and without screenshots.

## Checklist
- [x] Tests pass locally
- [x] No hardcoded credentials/secrets
- [x] Updated relevant documentation/comments
- [x] Linked related issue (if any) — Closes #14


EXAMPLE 2: Large change (many files)

## Summary
Migrated reporting module from ExtentReports v3 to v5, restructured ReportManager to support parallel execution,
and added screenshot capture on both pass/fail (previously only on fail). Touches test listeners, config, and all
existing test classes due to annotation changes required by the new ExtentReports API.

## Changes
- [x] Test classes added/modified (42 files — updated @Listeners annotations across all suites)
- [x] Framework/core classes added/modified (ReportManager, TestListener, ScreenshotUtil, ConfigReader)
- [x] Config/dependency changes (pom.xml — bumped extentreports 3.1.5 → 5.1.1, added thread-safe reporter)

## Testing
- Ran full regression suite (187 tests) locally in parallel mode (4 threads) — all passing
- Verified report output renders correctly for pass/fail/skip states
- Verified screenshots attach correctly under parallel execution (no cross-thread contamination)
- Spot-checked CI pipeline run on feature branch

## Checklist
- [x] Tests pass locally
- [x] No hardcoded credentials/secrets
- [x] Updated relevant documentation/comments
- [x] Linked related issue (if any) — Closes #21, relates to #19
-->
