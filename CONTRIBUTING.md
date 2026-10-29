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
pipeline parameters (`env`/`platform`/`tags`/`browser`/`min_pass_rate`
`workflow_dispatch` inputs). A separate `desktop` job runs `@desktop`
scenarios on a self-hosted Windows runner, manual-only (`workflow_dispatch`
with `platform: desktop`/`all`) and `continue-on-error: true` until that
runner is provisioned — see `DESKTOP_AUTOMATION_SETUP.md` ("CI: self-hosted
Windows runner") for the provisioning checklist.

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
| `context` | `ScenarioContext` (per-scenario, DI'd state), `GlobalContext` (process-wide static state) |
| `drivers` | `MobileDriverFactory`, `CloudAppUploader` |
| `utils` | `DriverFactory`, `WaitUtils`, `ScreenshotUtil`, `RetryHelper`, `SoftAssertionsWrapper`, `StringHelper`, `DateHelper`, `FileHelper`, `CloudStatusReporter` |
| `testdata` | `TestDataReader` (JSON/CSV/Excel), `TestDataGenerator` (Datafaker), `ApiTestDataManager` (cleanup tracking); `testdata.model` holds data POJOs |
| `exceptions` | Custom runtime exceptions, also drive Allure's `categories.json` |
| `reporting` | `AllureReportSetup` |
| `listeners` | `TestListener` (plain-TestNG Allure wiring) |

Test-side (`src/test/java`): `hooks` (Cucumber hooks), `runners`
(`CucumberTestRunner`/`RerunTestRunner`), `steps` (step definitions),
`tests` (the one plain-TestNG test, `LoginTest`).

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
