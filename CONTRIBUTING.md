# Contributing

Everything a new team member needs to get set up and start adding coverage.
For CLI flags/profiles see [`README.md`](README.md); for tagging/writing
conventions see [`GHERKIN_GUIDE.md`](GHERKIN_GUIDE.md); for credentials see
[`SECRETS.md`](SECRETS.md); for unstable tests see
[`FLAKY_TESTS.md`](FLAKY_TESTS.md). This doc focuses on what those don't
cover: environment setup, Grid/cloud execution, the full config-key
reference, and how to add a new page/screen/API client/step.

## Prerequisites

- **JDK 21** and **Maven 3.9+**
- **Git**
- **Google Chrome** installed, for `@web` scenarios (the default suite) —
  [WebDriverManager](https://github.com/bonigarcia/webdrivermanager) (already
  a dependency) downloads a matching chromedriver automatically; you only
  need the browser binary itself
- Optional, only if you'll touch the areas they cover:
  - **Node.js** — to install the Allure CLI (`npm install -g
    allure-commandline`) for generating/viewing reports locally
  - **Docker** — to run the suite containerized (see README.md "Running in
    Docker"), or to spin up a local Selenium Grid (see below)
  - **Appium + Android SDK/emulator** (or a real device) — only for
    `@android` scenarios; no iOS environment exists on this team yet (see
    "Mobile screens" below)
  - **Windows host + Appium NovaWindows driver** — only for `@desktop`
    scenarios; see `DESKTOP_AUTOMATION_SETUP.md`

## Setup

1. Clone the repo and `cd` into it.
2. `cp .env.example .env`, fill in real values (see
   [`SECRETS.md`](SECRETS.md) for what each key means and where to get it).
   `.env` is gitignored — it never gets committed.
3. `mvn -q clean compile test-compile` — confirms the toolchain, dependencies,
   and Checkstyle rules are all happy before you touch anything.
4. `mvn test -Psmoke` — runs the fast `@smoke` suite end to end as a sanity
   check that your machine can actually drive a browser.

## Running the tests

### Locally

See [`README.md` "Running the suite"](README.md#running-the-suite) for the
full CLI flag table, Maven profiles, and examples — `-Denv`, `-Dbrowser`,
`-Dheadless`, `-Dcucumber.filter.tags`, `-Dparallel.mode`/`-Dthread.count`,
and `-Psmoke`/`-Pregression`/`-Papi`/`-Pweb`/`-Pmobile`.

### On a Selenium Grid

Web only (`executionMode=grid`/`remote` — both names are accepted as
aliases, see `DriverFactory.createRemoteDriver`). **There's no grid running
by default** — spin one up first, e.g. locally via Docker:

```bash
docker run -d -p 4444:4444 --shm-size=2g selenium/standalone-chrome:latest
```

Then point the suite at it:

```bash
mvn test -DexecutionMode=grid -DgridUrl=http://localhost:4444/wd/hub
```

None of `config/{env}.properties` ship a `gridUrl` today (qa/staging set
`executionMode=remote` but fall back to the hardcoded
`http://localhost:4444/wd/hub` default in `DriverFactory`) — until a real
shared grid exists, always pass `-DgridUrl=...` explicitly rather than
assuming the qa/staging default points anywhere real.

### On the cloud (BrowserStack)

- **Mobile** (`@android`/`@ios`): set `executionMode=cloud` plus the
  `BROWSERSTACK_USERNAME`/`BROWSERSTACK_ACCESS_KEY` secrets (see
  `SECRETS.md`). `MobileDriverFactory` then builds the BrowserStack hub URL,
  auto-uploads your local `appPath` via `CloudAppUploader` (unless `appUrl`
  is already set), and reports pass/fail back to BrowserStack at scenario
  end (`CloudStatusReporter`, wired in `Hooks.tearDownUi`).

  ```bash
  mvn test -Pmobile -DexecutionMode=cloud -DappPath=/path/to/app.apk
  ```

- **Web**: there's no BrowserStack-specific code path for web — it reuses
  the same grid/remote path as above. Point `-DgridUrl` at BrowserStack's
  hub URL with embedded credentials
  (`https://<user>:<key>@hub-cloud.browserstack.com/wd/hub`) and set
  `-DexecutionMode=cloud` (or `grid`/`remote` — identical behavior for web
  today).

### In CI

GitHub Actions (`.github/workflows/ci.yml`) runs on push/PR, builds the
image in `.github/workflows/docker.yml` on Dockerfile/pom.xml changes, and
publishes the Allure report to GitHub Pages — see `README.md` for the full
pipeline parameters (`env`/`platform`/`tags`/`browser`/`min_pass_rate`/`db`
`workflow_dispatch` inputs). A separate `desktop` job runs `@desktop`
scenarios on a self-hosted Windows runner, manual-only (`workflow_dispatch`
with `platform: desktop`/`all`) and `continue-on-error: true` until that
runner is provisioned — see `DESKTOP_AUTOMATION_SETUP.md` ("CI: self-hosted
Windows runner") for the provisioning checklist.

The main `test` job runs on GitHub-hosted `ubuntu-latest`, which ships a
Docker daemon out of the box — so manually dispatching the workflow with
`db: container` (see "Testing against a fresh container database" below)
works there with no extra provisioning. Any future self-hosted runner added
for this job would need Docker confirmed available the same way; the
`desktop` job's self-hosted Windows runner never needs it, since `@desktop`
scenarios don't touch `db.*`.

## Configuration key reference

Every `ConfigReader.get("key")` actually read in code today (see
`config.ConfigReader` for the full sysprop → env var → `.env` → properties
file → default resolution order). Keys use camelCase in code; the
equivalent env var is `SCREAMING_SNAKE_CASE` (e.g. `apiClientId` →
`API_CLIENT_ID`).

| Key | Used by | Notes |
|---|---|---|
| `env` | `TestDataReader`, `AllureReportSetup` | Selects `config/{env}.properties` and `testdata/{env}/...` |
| `platform` | `AllureReportSetup`, `DriverFactory.initDesktopDriver` | Reported into the Allure environment widget always; for `@desktop` scenarios it also selects the desktop OS driver (`windows` today) — defaults to `web` in the properties files, so `@desktop` runs need an explicit `-Dplatform=windows` override. Which platform family (web/android/ios/desktop) actually runs is still tag-based, see `GHERKIN_GUIDE.md` |
| `browser` | `Hooks.setUpWeb`, `DriverFactory` | `chrome`/`firefox`/`edge` |
| `headless` | `DriverFactory` | `true`/`false` |
| `executionMode` | `DriverFactory`, `MobileDriverFactory`, `Hooks.tearDownUi`, `AllureReportSetup` | `local` (default) / `grid`/`remote` (web) / `cloud` (BrowserStack, web or mobile) |
| `gridUrl` | `DriverFactory` | Web Grid/cloud hub URL; defaults to `http://localhost:4444/wd/hub` if unset — **not shipped** in any `.properties` file, always pass explicitly |
| `platformName` | `DriverFactory` | Optional `platformName` capability for remote web sessions |
| `baseUrl` | `DriverFactory.launchApp` | Web app URL `@web` scenarios open |
| `apiBaseUrl` | `RequestSpecFactory.baseSpec()` | Default base URI for API clients built off the no-arg `baseSpec()` |
| `restfulApiBaseUrl` | `ObjectClient` | Base URI for the `restful-api.dev` sandbox specifically |
| `apiToken` | `Hooks.globalSetup`, `AuthManager` | Static bearer token, used only if `authUrl` is blank |
| `authUrl` | `AuthManager` | If set, switches `AuthManager` to OAuth2 `client_credentials` instead of a static token |
| `apiClientId` / `apiClientSecret` | `AuthManager` | OAuth2 credentials, only relevant when `authUrl` is set |
| `explicitTimeout` | `WaitUtils` | Seconds; the only one of the four timeout-looking keys that's actually read — `timeout`/`implicitTimeout`/`pageLoadTimeout` exist in `ConfigReader`'s `DEFAULTS` but aren't consumed anywhere today |
| `appiumServerUrl` | `MobileDriverFactory` | Local Appium server, default `http://127.0.0.1:4723` |
| `deviceName`, `platformVersion`, `udid`, `avd`, `appPackage`, `appActivity`, `bundleId` | `MobileDriverFactory` | Optional Appium capabilities, blank-skipped; `avd`/`appPackage`/`appActivity` are Android-only, `bundleId` iOS-only |
| `appPath` | `MobileDriverFactory` | Local `.apk`/`.ipa` path; auto-uploaded to BrowserStack when `executionMode=cloud` and `appUrl` isn't already set |
| `appUrl` | `MobileDriverFactory` | Pre-uploaded cloud app URL (`bs://...`) — skips the upload step if set |
| `browserstackUsername` / `browserstackAccessKey` | `MobileDriverFactory`, `CloudAppUploader` | **Not** a generic `cloud*` key — BrowserStack-specific; see `SECRETS.md` |
| `cloudProjectName` / `cloudBuildName` / `cloudSessionName` | `MobileDriverFactory` (`bstack:options`) | Optional BrowserStack dashboard grouping/labels |
| `desktopAppPath` | `DriverFactory.initDesktopDriver` | `.exe` path or UWP AUMID for `@desktop` scenarios; see `DESKTOP_AUTOMATION_SETUP.md` |
| `desktopAppArguments` / `desktopAppWorkingDir` | `DriverFactory.initDesktopDriver` | Optional NovaWindows capabilities, blank-skipped |
| `desktopDelayBeforeClick` / `desktopDelayAfterClick` | `DriverFactory.initDesktopDriver` | Milliseconds; NovaWindows' own documented fix for its PowerShell-backed input occasionally landing before a control has focus — default `150` each |
| `dbUrl` | `db.DbClient` | JDBC URL (`jdbc:postgresql://...`, `jdbc:mysql://...`, `jdbc:sqlserver://...`, `jdbc:oracle:thin:@...`); scheme selects the driver, all four ship on the classpath |
| `dbUsername` / `dbPassword` | `db.DbClient` | **Secrets** — never put these in a `.properties` file, see `SECRETS.md` |
| `dbMaxPoolSize` / `dbMinIdle` / `dbConnectionTimeoutMs` / `dbIdleTimeoutMs` / `dbMaxLifetimeMs` | `db.DbClient` | Optional HikariCP pool tuning; defaults are `10` / `2` / `30000` / `600000` / `1800000` |
| `mongoUri` / `mongoDatabase` | `db.MongoClientWrapper` | Left blank on dev — `hooks.NoSqlTestInfraHooks` then auto-starts an embedded fake MongoDB instead; see "Validating NoSQL stores" |
| `redisHost` / `redisPort` / `redisMaxPoolSize` | `db.RedisClient` | Same auto-fallback as `mongoUri` when `redisHost` is blank |
| `redisPassword` | `db.RedisClient` | **Secret** — never put this in a `.properties` file, see `SECRETS.md` |
| `buildNumber` | `AllureReportSetup` | Defaults to `"local"`; CI should override so Allure's history/trend graphs line up builds correctly |
| `loginUsername` / `loginPassword` | `tests.LoginTest` (plain TestNG only) | Not used by the Cucumber `LoginSteps` flow, which reads test accounts from `testdata/{env}/users.json` instead |

Known unused/placeholder config, called out so you don't go looking for
where they're wired in: `browsers` (comma-list, intended for a
not-yet-built cross-browser parallel runner), `timeout`/`implicitTimeout`/
`pageLoadTimeout` (reserved, only `explicitTimeout` is live), and the
root-level `src/test/resources/config.properties` file (a stale leftover —
`ConfigReader` only ever loads `config/{env}.properties`, never this file).

For tags (`@web`/`@android`/`@ios`/`@desktop`/`@api` platform tags,
`@smoke`/`@regression`/`@wip`/`@flaky` suite tags), see
[`GHERKIN_GUIDE.md`](GHERKIN_GUIDE.md) — that's the canonical reference,
not repeated here.

## Project structure

| Package | Purpose |
|---|---|
| `base` | `BasePage` (web POM base), `BaseScreen` (mobile Appium screen base), `DesktopBaseScreen` (desktop/NovaWindows screen base), `BaseTest` (plain TestNG base) |
| `pages` | Web Page Objects, extend `base.BasePage` |
| `screens.mobile` | Mobile Screen Objects, extend `base.BaseScreen` |
| `screens.desktop` | Desktop Screen Objects, extend `base.DesktopBaseScreen` |
| `api.clients` | REST Assured clients returning typed POJOs |
| `api.specs` | Reusable RestAssured request/response spec builders |
| `api.filters` | `ApiLoggingFilter` — auto-logs every API call to Log4j + Allure |
| `api.models(.request/.response)` | API request/response POJOs |
| `config` | `ConfigReader` |
| `db` | `DbClient` (HikariCP-pooled JDBC query/update utility), `RowMapper` (functional interface for mapping a row to a POJO), `QueryLoader` (loads `.sql` files from `resources/queries`), `SqlIdentifiers` (validates table/column names before templating), `ProductionWriteGuard` (blocks writes that look like production), `DbTestDataManager` (tracks/cleans up seeded rows per scenario), `MongoClientWrapper` (MongoDB document queries/writes), `RedisClient` (Redis key/cache queries/writes) |
| `context` | `ScenarioContext` (per-scenario, DI'd state), `GlobalContext` (process-wide static state) |
| `drivers` | `MobileDriverFactory`, `CloudAppUploader` |
| `utils` | `DriverFactory`, `WaitUtils`, `ScreenshotUtil`, `RetryHelper`, `SoftAssertionsWrapper`, `StringHelper`, `DateHelper`, `FileHelper`, `CloudStatusReporter` |
| `testdata` | `TestDataReader` (JSON/CSV/Excel), `TestDataGenerator` (Datafaker), `ApiTestDataManager` (cleanup tracking); `testdata.model` holds data POJOs |
| `exceptions` | Custom runtime exceptions, also drive Allure's `categories.json` |
| `reporting` | `AllureReportSetup` |
| `listeners` | `TestListener` (plain-TestNG Allure wiring) |

Test-side (`src/test/java`): `hooks` (`Hooks` — Cucumber hooks;
`DbSeedHooks` — tag-driven DB seed/cleanup; `NoSqlTestInfraHooks` —
starts/stops the embedded fake MongoDB/Redis for `@nosql`), `runners`
(`CucumberTestRunner`/`RerunTestRunner`), `steps` (step definitions,
including `DbSteps` and `NoSqlSteps`), `tests` (the plain-TestNG
`LoginTest`), `db` (`DbClientTest`, `ProductionWriteGuardTest` — plain
TestNG; `ContainerDatabaseSuiteListener` — TestNG `ISuiteListener`,
registered in `testng.xml`, starts/stops a throwaway Postgres via
Testcontainers under `-Ddb=container`; `ContainerDatabaseTest` — asserts
the container's schema/seed actually applied, self-skips otherwise).

## Adding things

### A web page

Add a class under `pages/` extending `base.BasePage`:

```java
public class SearchResultsPage extends BasePage {
    public SearchResultsPage(WebDriver driver) {
        super(driver);
    }

    @FindBy(css = ".result-item")
    private List<WebElement> results;

    public int getResultCount() {
        return results.size();
    }
}
```

Prefer `@FindBy` for elements with a stable `id`/attribute; fall back to a
`private static final By` constant only for shared/dynamic elements (see
`LoginPage`'s `FLASH_MESSAGE` for the pattern). Use `BasePage`'s inherited
`click`/`type`/`getText`/etc. — they're all wait-backed; never add
`Thread.sleep()` (Checkstyle will fail the build).

### A mobile screen

Add a class under `screens/mobile/` extending `base.BaseScreen`. Annotate every
field with **both** `@AndroidFindBy` and `@iOSXCUITFindBy` even if you only
have Android locators verified right now (see `LoginScreen` — its iOS
locators are explicitly documented as unverified placeholders, since no iOS
environment exists yet):

```java
public class CheckoutScreen extends BaseScreen {
    @AndroidFindBy(id = "checkoutBtn")
    @iOSXCUITFindBy(accessibility = "Checkout")
    private WebElement checkoutButton;

    public CheckoutScreen(WebDriver driver) {
        super(driver);
    }

    public OrderConfirmationScreen checkout() {
        tap(checkoutButton);
        return new OrderConfirmationScreen(driver);
    }
}
```

Methods that navigate return the next screen object (fluent navigation),
same idiom as the web pages.

### A desktop screen

Add a class under `screens/desktop/` extending `base.DesktopBaseScreen`.
There's no Windows-specific `@FindBy` annotation (unlike mobile's
`@AndroidFindBy`/`@iOSXCUITFindBy`), so locate elements via plain `By`
fields built from `automationId(...)` instead of annotated `WebElement`
fields (see `DesktopLoginScreen`):

```java
public class SettingsScreen extends DesktopBaseScreen {
    private final By saveButton = automationId("btnSave");

    public SettingsScreen(WebDriver driver) {
        super(driver);
    }

    public void save() {
        click(saveButton);
    }
}
```

Same fluent-navigation idiom as web/mobile. See `DESKTOP_AUTOMATION_SETUP.md`
for the window-handling/keyboard-shortcut helpers `DesktopBaseScreen`
provides, and its "Known limitations" section before relying on per-step
screenshots or rapid click/type sequences.

### An API client

Add a class under `api.clients`. Build requests off
`RequestSpecFactory.baseSpec()` (or `authenticatedSpec()` for bearer-token
endpoints); return a typed POJO on the happy path, plus a `...Raw` twin
returning the unasserted `Response` for negative-path tests (see
`ObjectClient`):

```java
public class WidgetClient {
    private RequestSpecification spec() {
        return RequestSpecFactory.baseSpec(ConfigReader.get("widgetsApiBaseUrl"));
    }

    public WidgetResponse getWidget(String id) {
        return RestAssured.given().spec(spec())
                .get("/widgets/{id}", id)
                .then().statusCode(200)
                .extract().as(WidgetResponse.class);
    }

    public Response getWidgetRaw(String id) {
        return RestAssured.given().spec(spec()).get("/widgets/{id}", id);
    }
}
```

Add request/response POJOs under `api.models.request`/`api.models.response`.
`ApiLoggingFilter` is already wired into `baseSpec()` — every call through
it gets logged to Log4j and attached to the live Allure report
automatically; you don't add logging yourself.

### Querying the database

Use `db.DbClient` directly from a step/test — it's a static utility backed
by one HikariCP-pooled `DataSource`, lazily initialized from the `dbUrl` /
`dbUsername` / `dbPassword` config on first use (see "Configuration key
reference" above and `SECRETS.md` for the credentials):

```java
// Rows as List<Map<columnLabel, value>> — fine for ad hoc assertions
List<Map<String, Object>> rows = DbClient.query(
        "SELECT id, status FROM orders WHERE customer_id = ?", customerId);

// Or map straight to a POJO via a RowMapper
List<Order> orders = DbClient.query(
        "SELECT id, status FROM orders WHERE customer_id = ?",
        rs -> new Order(rs.getLong("id"), rs.getString("status")),
        customerId);

// Single-row convenience
Optional<Map<String, Object>> order = DbClient.queryForSingleRow(
        "SELECT * FROM orders WHERE id = ?", orderId);

// INSERT/UPDATE/DELETE — returns the affected row count
DbClient.update("UPDATE orders SET status = ? WHERE id = ?", "SHIPPED", orderId);
```

Always use the `?` parameterised form above — never string-concatenate
values into SQL. Don't call `DbClient.closeAll()` yourself; the pool is
shared process-wide and is already closed once, in `Hooks.globalTeardown`'s
`@AfterAll`. See `db.DbClientTest` for a runnable example backed by an
in-memory H2 database (no live DB server needed to run it).

Generic, schema-agnostic assertion steps already exist in `steps.DbSteps` —
`the "<table>" table should contain a record where "<col>" is "<value>"`,
a `DataTable` variant for multiple conditions, a column-equality step, and a
step comparing an API response field (any POJO stashed in `ScenarioContext`)
against a DB column. Reuse those from a new feature before writing another
one-off assertion step. The underlying SQL lives in `.sql` files under
`resources/queries` (loaded via `db.QueryLoader`), not as Java string
literals — add new query files there rather than inlining SQL in a step.

Every `DbClient.update(...)` call is blocked by `db.ProductionWriteGuard`
if `env` or `dbUrl` looks like production (a case-insensitive `"prod"`
substring match on either) — see `db.ProductionWriteGuardTest`. There's no
override; tests must never write to a production database.

#### Seeding and cleaning up test data directly in the database

For fast, isolated setup that skips the UI/API entirely, seed fixture rows
straight into the database with a `@seed-<thing>` tag instead of a `Given`
step. `hooks.DbSeedHooks` is the pattern to copy — see its `@seed-orders`
tag and `database_seed_cleanup.feature`:

```java
@Before("@seed-orders")
public void seedOrders() {
    DbClient.update(QueryLoader.load("create_orders_table"));
    String orderId = "seed-" + UUID.randomUUID();
    DbClient.update(QueryLoader.load("seed_order"), orderId, "Seeded Customer", "PENDING");
    dbTestDataManager.track(scenarioContext, "orders", "id", orderId);
    scenarioContext.set("seededOrderId", orderId);
}

@After("@seed-orders")
public void cleanupSeededData() {
    dbTestDataManager.cleanup(scenarioContext);
}
```

`db.DbTestDataManager.track(scenarioContext, table, idColumn, idValue)`
records every row a scenario seeds; its `cleanup(scenarioContext)` deletes
them all (most-recently-seeded first) regardless of whether the scenario
passed or failed, the same pattern `testdata.ApiTestDataManager` already
uses for API-created entities. Adding a new `@seed-<thing>` tag means: add
its own `@Before` here, track what it inserts, and extend the `@After` tag
expression to include it. `Hooks.globalTeardown` also logs a warning if
`orders` isn't empty at the very end of the run, as a last-resort leak
detector — it should never fire if every seed hook's cleanup ran.

#### Validating NoSQL stores (MongoDB / Redis)

`db.MongoClientWrapper` and `db.RedisClient` extend the same pattern to
document stores and caches — lazily-initialized, process-wide clients, used
from `steps.NoSqlSteps`'s generic `the "<collection>" collection should
contain a document where ...` / `the "<key>" key should exist in Redis`
style steps (see `database_nosql.feature` for the full set, tagged
`@nosql`). Every write (`insertOne`/`deleteMany`/`set`/`delete`) goes
through the same `db.ProductionWriteGuard` as `DbClient.update`.

Tagging a scenario `@nosql` also activates `hooks.NoSqlTestInfraHooks`,
which — only when `mongoUri`/`redisHost` are blank — starts an in-process,
pure-Java fake MongoDB (`mongo-java-server`) and fake Redis (`jedis-mock`)
once per run and points the clients at them via system properties, so these
steps run with zero external setup on dev, the same idea as `dbUrl`'s H2
default. Point `mongoUri`/`redisHost`/`redisPort` at a real instance via
config (dev.properties, `-D`, or env vars) to run the exact same steps
against it instead — `NoSqlTestInfraHooks` then does nothing.

#### Testing against a fresh container database (CI / pipeline runs)

By default every `db.*` test/scenario shares one process-wide `DbClient`
pool pointed at the H2 in-memory default (`dev.properties`) — fast, but not
a fully isolated, repeatable environment for a pipeline. Pass `-Ddb=container`
to instead run against a throwaway, fully isolated Postgres container, fresh
for every run:

```bash
mvn test -Pdatabase -Ddb=container
```

`db.ContainerDatabaseSuiteListener` (registered as a suite-level
`ISuiteListener` in `testng.xml`, not a `@BeforeSuite` method, so it's
guaranteed to run once before every `<test>` block regardless of the
suite's `parallel="methods"` mode) does the work when it sees `-Ddb=container`:

1. Starts a `postgres:16-alpine` Testcontainers container.
2. Points `dbUrl`/`dbUsername`/`dbPassword` at it via `System.setProperty`
   (highest-priority source in `ConfigReader`'s resolution order — see
   "Configuration key reference" above), before `DbClient`'s lazily-built
   pool can be reached by anything else in the suite.
3. Applies schema (`CREATE TABLE IF NOT EXISTS ...`, reusing the same
   `.sql` files `DbSeedHooks`/`database_validation.feature` already use)
   and seeds one row into a dedicated `container_health_check` table, so
   `db.ContainerDatabaseTest` has something real to assert on as proof the
   container path actually ran.
4. Stops the container at the end of the suite (`onFinish`).

Needs a Docker daemon reachable from wherever `mvn test` runs — see "In CI"
above. Without `-Ddb=container`, this listener (and `ContainerDatabaseTest`,
which self-skips) does nothing, so a bare `mvn test` never needs Docker.

### Step definitions

Add a `<Feature>Steps.java` class under `steps/` — it's auto-discovered
(the `glue` array in `CucumberTestRunner`/`RerunTestRunner` already covers
the whole `steps` package). Constructor-inject `context.ScenarioContext` to
share state with other step classes in the same scenario (PicoContainer
gives every class that asks for it the same instance, per scenario):

```java
public class WidgetSteps {
    private final ScenarioContext scenarioContext;

    public WidgetSteps(ScenarioContext scenarioContext) {
        this.scenarioContext = scenarioContext;
    }

    @When("the user fetches widget {string}")
    public void theUserFetchesWidget(String id) {
        scenarioContext.set("lastResponse", widgetClient.getWidgetRaw(id));
    }
}
```

Method names mirror the Gherkin step text 1:1, camelCased. There's no typed
shared-state class beyond `ScenarioContext`'s string-keyed map today — pick
a key name that doesn't collide with existing ones in other step classes.

### Test data

Add a file under `testdata/{env}/` (same filename across `dev`/`qa`/`staging`
— `TestDataReader` resolves the path from the active `env`). JSON keyed by
record name, CSV/Excel keyed by a `key` column — read it via
`TestDataReader.readJsonEntry`/`readCsvEntry`/`readExcelEntry`. Add a
matching POJO under `testdata.model`. For generated (not fixed) data, use
`TestDataGenerator` (wraps Datafaker) to avoid colliding with other runs
against a shared sandbox.

### Hooks / new platform tags

If you're introducing an entirely new platform (not just a new
page/screen/client under the existing `@web`/`@android`/`@ios`/`@desktop`/`@api`
tags), you'll need to add `@Before`/`@After` bindings in
`hooks/Hooks.java` for the new tag, following the existing
`setUpWeb`/`setUpMobile` pattern — and add the tag to
`GHERKIN_GUIDE.md`'s platform tags table.
