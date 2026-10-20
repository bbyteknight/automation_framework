# Automation Framework

Selenium/Appium/REST Assured automation driven by Cucumber + TestNG. See
also [`GHERKIN_GUIDE.md`](GHERKIN_GUIDE.md) for tagging/writing conventions
and [`SECRETS.md`](SECRETS.md) for how credentials are resolved.

## Running the suite

Every run option is controllable from the command line — nothing requires
editing a file to change environment, platform, browser, headless mode,
tag scope, or thread count.

### CLI flags

| Flag | Controls | Example | Default |
|---|---|---|---|
| `-Denv` | Which `config/{env}.properties` to load | `-Denv=qa` | `dev` |
| `-Dbrowser` | Browser for `@web` scenarios | `-Dbrowser=firefox` | `chrome` |
| `-Dheadless` | Headless browser mode | `-Dheadless=true` | `false` (dev), `true` (qa/staging) |
| `-Dcucumber.filter.tags` | Cucumber tag expression | `-Dcucumber.filter.tags="@smoke and not @flaky"` | `@web` (or whatever profile is active) |
| `-Dparallel.mode` | TestNG parallel mode (`methods`/`classes`/`tests`/`instances`) | `-Dparallel.mode=classes` | `methods` |
| `-Dthread.count` | TestNG thread count | `-Dthread.count=8` | `4` |

These all flow through `config.ConfigReader` (`env`/`browser`/`headless`) or
directly into the Surefire-forked test JVM (`cucumber.filter.tags`,
`parallel.mode`, `thread.count` — the latter two are filtered into
`target/test-classes/testng.xml` before TestNG reads it, see `pom.xml`'s
`testResources`).

Platform (web/api/android/ios) is selected via Cucumber tags, not a
separate flag — use the Maven profiles below or `-Dcucumber.filter.tags`
directly (e.g. `-Dcucumber.filter.tags="@android"`).

### Maven profiles

Shortcuts for the platform/suite scopes from `GHERKIN_GUIDE.md` — each just
sets `cucumber.filter.tags`:

| Profile | Tag expression | Notes |
|---|---|---|
| `-Psmoke` | `@smoke` | Fast critical-path set, any platform |
| `-Pregression` | `not @wip` | Full coverage, excludes work-in-progress |
| `-Papi` | `@api` | REST Assured only, no browser/driver needed |
| `-Pweb` | `@web` | Browser/Selenium only (same as the default) |
| `-Pmobile` | `@android or @ios` | Needs a running Appium server + device/emulator — not available on the current CI runner |

A directly-passed `-Dcucumber.filter.tags=...` always wins over whichever
profile (or none) is active.

### Examples

```bash
# Default: @web scenarios, chrome, headed, 4 threads
mvn test

# Smoke only, across all platforms
mvn test -Psmoke

# API suite against the QA environment
mvn test -Papi -Denv=qa

# Web regression, headless Firefox, more parallelism
mvn test -Pregression -Dbrowser=firefox -Dheadless=true -Dthread.count=8 -Dparallel.mode=classes

# Mobile scenarios (requires Appium + device/emulator already running)
mvn test -Pmobile

# Fine-grained tag expression, bypassing any profile
mvn test -Dcucumber.filter.tags="@web and @smoke and not @flaky"

# Run with automatic rerun of failed scenarios once (what CI uses)
./scripts/rerun-failed-tests.sh -Dcucumber.filter.tags="@smoke"

# Generate/preview the Allure report after a run
mvn exec:exec@allure-report   # static HTML -> target/allure-report
mvn exec:exec@allure-serve    # local preview server
```

### Secrets

`LOGIN_USERNAME`/`LOGIN_PASSWORD`/`API_TOKEN`/etc. are never passed via
`-D` — see [`SECRETS.md`](SECRETS.md) for how those are provisioned
(env vars, `.env`, or CI secrets).
