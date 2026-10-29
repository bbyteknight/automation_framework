# Automation Framework

Selenium/Appium/REST Assured automation driven by Cucumber + TestNG.

New to this repo? Start with [`CONTRIBUTING.md`](CONTRIBUTING.md) —
prerequisites, setup, running on a Grid/cloud, the full config-key
reference, and how to add a page/screen/API client/step. See also
[`GHERKIN_GUIDE.md`](GHERKIN_GUIDE.md) for tagging/writing conventions,
[`SECRETS.md`](SECRETS.md) for how credentials are resolved, and
[`FLAKY_TESTS.md`](FLAKY_TESTS.md) for the flaky-test quarantine process.

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

Platform (web/api/android/ios/desktop) is selected via Cucumber tags, not a
separate flag — use the Maven profiles below or `-Dcucumber.filter.tags`
directly (e.g. `-Dcucumber.filter.tags="@android"`). `-Dplatform` is a
separate switch used only by `@desktop` scenarios, to pick which desktop OS
driver `DriverFactory.initDesktopDriver()` builds (only `windows` today).

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
| `-Pdesktop` | `@desktop` | Needs a Windows host + Appium server + NovaWindows driver, and `-Dplatform=windows` (see `DESKTOP_AUTOMATION_SETUP.md`) — not available on the current CI runner |

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

# Desktop scenarios (requires Appium + NovaWindows driver, Windows host)
mvn test -Pdesktop -Dplatform=windows

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

## Running in Docker

The `Dockerfile` bundles JDK 21, Maven, and headless Chrome, so a run inside
the container needs nothing installed on the host beyond Docker itself —
same environment locally and in CI.

```bash
# Build once (Maven dependencies are baked into their own cached layer —
# only re-downloaded when pom.xml changes)
docker build -t automation-framework .

# Run the default suite; CMD is `test`, so this is `mvn test` inside the container
docker run --rm automation-framework

# Any CLI flag from the table above works the same way, appended after the image name
docker run --rm automation-framework test -Psmoke -Dbrowser=chrome -Dheadless=true

# Mount target/ so reports survive after the container exits
docker run --rm -v "$(pwd)/target:/app/target" automation-framework test -Papi

# Pass secrets as env vars (never bake them into the image)
docker run --rm \
  -e LOGIN_USERNAME="$LOGIN_USERNAME" \
  -e LOGIN_PASSWORD="$LOGIN_PASSWORD" \
  -v "$(pwd)/target:/app/target" \
  automation-framework test -Psmoke
```

Only Chrome is bundled today — `-Pmobile` and `-Dbrowser=firefox/edge`
aren't usable inside this image yet (no Appium server/device, and no
Firefox/Edge binary installed). `.github/workflows/docker.yml` builds the
image and runs the `@smoke` suite inside a container on every change to
`Dockerfile`/`pom.xml`, as a reproducibility check.

## Code quality

- **Checkstyle** (`checkstyle.xml`) runs on every `mvn compile`/`test`
  (bound to the `validate` phase) and fails the build on violations —
  currently the `Thread.sleep()` ban plus a small set of maintainability
  checks (unused/redundant imports, equals/hashcode, empty blocks, line
  length, method length). Run it standalone with `mvn checkstyle:check`.
- **SonarCloud** static analysis runs in CI (`sonar` job in
  `.github/workflows/ci.yml`, `mvn compile sonar:sonar`) but needs one-time
  setup that only a repo admin can do:
  1. Sign in to [sonarcloud.io](https://sonarcloud.io) with the GitHub org/account
     and import this repository as a new project.
  2. Confirm the generated project key/organization match `pom.xml`'s
     `sonar.projectKey`/`sonar.organization` (currently
     `bbyteknight_automation_framework` / `bbyteknight`) — update `pom.xml`
     if SonarCloud picked different values.
  3. Generate a token (My Account → Security) and add it as the `SONAR_TOKEN`
     repo secret.

  Until `SONAR_TOKEN` exists, the `sonar` job step fails harmlessly
  (`continue-on-error: true`) without blocking the pipeline.
- **Dependabot** (`.github/dependabot.yml`) opens weekly PRs for outdated
  Maven dependencies and GitHub Actions versions — no setup needed, it's
  enabled automatically once the file exists on the default branch.
