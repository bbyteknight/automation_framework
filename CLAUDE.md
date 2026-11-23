# AI Instructions for This Framework

Read this before generating or editing any code in this repo. It exists so
AI-generated code (Claude Code, Appium MCP, Copilot, etc.) matches the
framework's existing patterns closely enough to merge with minimal rework —
that's the bar, not just "it compiles."

For full detail beyond this summary, see [`CONTRIBUTING.md`](CONTRIBUTING.md)
(onboarding, config, execution modes) and [`GHERKIN_GUIDE.md`](GHERKIN_GUIDE.md)
(tagging/feature-file rules, reproduced in condensed form below).

## Package structure

Production code lives under `src/main/java`, test/Cucumber glue under
`src/test/java`. Put new classes in the existing package that matches their
role — don't invent a new package for something that fits one already here:

| Package | Contents |
|---|---|
| `base` | Abstract base classes only: `BasePage` (web), `BaseScreen` (mobile), `DesktopBaseScreen` (Windows), `BaseTest`. Nothing app-specific goes here. |
| `pages` | Web page objects (Selenium), one class per page. |
| `screens/mobile` | Appium mobile screen objects (Android/iOS), one class per app screen. |
| `screens/desktop` | Appium NovaWindows desktop screen objects. |
| `drivers` | Driver factories (e.g. `MobileDriverFactory`) — session/capability setup, not page logic. |
| `api/clients`, `api/specs`, `api/filters` | REST Assured client wrappers, request specs, logging filters. |
| `api/models/request`, `api/models/response` | API request/response POJOs. |
| `db` | `DbClient` and other direct DB/NoSQL access (production-side, not step defs). |
| `config` | `ConfigReader` and environment/properties loading. |
| `context` | `ScenarioContext` / `GlobalContext` — cross-step state. |
| `testdata`, `testdata/model` | Test data readers and their POJOs. |
| `utils` | Cross-cutting helpers (`WaitUtils`, `DownloadUtil`, `NetworkMockUtil`, `JsErrorMonitor`, etc.) — stateless or scenario-scoped utilities, not page/screen logic. |
| `exceptions`, `listeners`, `reporting` | Custom exceptions, TestNG listeners, Allure report setup (including `reporting.FailureTriageReport`'s local failure categorisation). |
| `ai` | Optional, manual-only AI-assisted tooling (e.g. `ai.LlmFailureSummarizer`) — never wired into `hooks.Hooks` or any Maven test phase; see "Keeping AI out of CI" below. |
| `steps` (test) | Cucumber step definitions, one class per feature's domain (e.g. `LoginSteps`, `NetworkMockSteps`). |
| `hooks` (test) | `Hooks` — `@Before`/`@After`/`@AfterStep` only. Don't put step definitions here. |
| `runners` (test) | TestNG/Cucumber runner classes. |
| `db` (test) | DB-specific step definitions/test support, mirrors `db` under main. |

Rule of thumb: if it's reusable production logic, it goes under
`src/main/java`; if it's a `@Given`/`@When`/`@Then` or lifecycle hook, it goes
under `src/test/java`.

## BasePage / BaseScreen usage rules

- Every web page object **extends `base.BasePage`**; every mobile screen
  object **extends `base.BaseScreen`**; every desktop screen object extends
  `base.DesktopBaseScreen`. Never duplicate `WebDriverWait`/`Actions`/
  `JavascriptExecutor` boilerplate in a page/screen class — that belongs in
  the base class, already written once.
- The constructor takes a `WebDriver driver` and calls `super(driver)`. Do not
  add any other constructor shape.
- Use the protected helpers already on the base class — `click`, `type`,
  `getText`, `tap` (mobile), `swipe*`, `scrollToElement`, etc. — instead of
  calling Selenium/Appium APIs directly in a page/screen method. If a needed
  action doesn't exist yet on the base class, add it there (so every
  page/screen benefits), not as a one-off in a single page object.
- **Never use `Thread.sleep()`** anywhere in a page/screen/step — Checkstyle
  fails the build on it. Always wait via `WaitUtils` (through the base
  class's helpers) for visibility/clickability instead of a fixed pause.
- Elements are declared as `private WebElement` fields with `@FindBy` (web,
  Selenium `PageFactory`) or `@AndroidFindBy`/`@iOSXCUITFindBy` (mobile,
  Appium `AppiumFieldDecorator` — both annotations on the same field so one
  screen class works for both platforms; no platform `if`/`else` branching in
  screen logic). Locator priority: `id`/`accessibility id` first, `xpath`
  only as a last resort for elements with no stable id — and say why in a
  comment when xpath is used.
- Page/screen methods return **void** for actions with no navigation
  consequence, or **the next page/screen object** when the action navigates
  (e.g. `LoginScreen.login(...)` returns `HomeScreen`) — this lets steps chain
  without reaching back into `ScenarioContext` for a page they already have a
  reference to. Don't return `WebElement`, `boolean` status flags, or raw
  driver types from a page/screen method.
- Page/screen classes contain **no assertions**. Assertions belong in step
  definitions (`org.testng.Assert`), never in `base`/`pages`/`screens`.

## Gherkin style and tagging (see [`GHERKIN_GUIDE.md`](GHERKIN_GUIDE.md) for full detail)

- Exactly **one platform tag** per scenario: `@web`, `@android`, `@ios`, or
  `@desktop` — backend-only scenarios use `@api` instead (no driver started).
  Never combine two platform tags on one scenario.
- Add suite tags freely alongside the platform tag: `@smoke`, `@regression`,
  `@wip`, `@flaky`.
- Write steps **declaratively** ("the user logs in with valid credentials"),
  never imperatively ("the user clicks the username field and types..."). No
  implementation detail (locators, element names) in step text — that logic
  lives in the step definition.
- One behavior per scenario; `Given`/`When`/`Then` in that order, `And`/`But`
  to continue the same phase. Use `Background:` only for setup every scenario
  in the file needs. Use `Scenario Outline`/`Examples` for data variation
  instead of copy-pasted near-duplicate scenarios.
- Feature files are `kebab-case.feature`; keep one platform's scenarios per
  file (don't mix `@web` and `@api` in the same file).

## Step definitions

- One `steps.*Steps` class per feature's domain, named after that domain
  (e.g. `NetworkMockSteps`, not `WebSteps2`).
- A step class that needs cross-step state takes `ScenarioContext` via
  constructor injection (PicoContainer) and stores/retrieves typed objects by
  string key, e.g. `scenarioContext.set("loginPage", new LoginPage(...))` /
  `scenarioContext.get("loginPage", LoginPage.class)`. Don't use static
  fields or instance fields on the steps class itself to carry state between
  steps — PicoContainer creates a fresh steps instance per scenario, but a
  fresh `ScenarioContext` is explicitly how state is meant to cross step
  classes within one scenario.
- Steps obtain the driver via `utils.DriverFactory.getDriver()` (web/desktop)
  or the equivalent mobile factory — never construct a driver inside a step.
- Assertions use `org.testng.Assert`, with a descriptive failure message that
  includes both expected and actual values.

## Lifecycle hooks

- All `@Before`/`@After`/`@AfterStep`/`@BeforeAll`/`@AfterAll` go in
  `hooks.Hooks` (or a clearly-named sibling hooks class for a distinct
  concern, e.g. `DbSeedHooks`) — never inside a steps class.
- Scope hooks with a Cucumber tag expression (`@Before("@web")`,
  `@After("@web or @android or @ios")`) rather than an unconditional hook that
  checks the tag internally, unless the hook must run for every scenario
  regardless of platform (e.g. zip-extraction cleanup, which no-ops when
  nothing was tracked).
- Cucumber runs same-annotation `@After` hooks in **reverse declaration
  order**. When ordering matters (e.g. a cleanup hook needing the driver
  still open), say so in a comment near the hook, the way `Hooks` does.

## Examples of good page/screen objects and steps

Treat these existing files as the canonical reference pattern — match their
shape for any new page/screen/steps class:

- Web page object: [`src/main/java/pages/LoginPage.java`](src/main/java/pages/LoginPage.java)
- Mobile screen object (cross-platform): [`src/main/java/screens/mobile/LoginScreen.java`](src/main/java/screens/mobile/LoginScreen.java)
- Step definitions using `ScenarioContext` + `TestDataReader`: [`src/test/java/steps/LoginSteps.java`](src/test/java/steps/LoginSteps.java)
- Matching feature file (tags, declarative steps, `Scenario Outline`): [`src/test/resources/features/login.feature`](src/test/resources/features/login.feature)
- Lifecycle hooks with tag scoping and ordering comments: [`src/test/java/hooks/Hooks.java`](src/test/java/hooks/Hooks.java)

## Keeping AI out of CI

AI-assisted tools (Appium MCP, or any other AI agent/IDE assistant) are a
**development-time aid only** — for app exploration, locator discovery, and
drafting page objects/steps that a human then reviews and commits. They must
never run as part of the CI pipeline (Azure Pipelines) or be a runtime
dependency of any test: CI must stay deterministic (same input → same result,
no model-call variance) and cost-predictable (no per-run LLM/API spend). Any
code an AI tool generates is reviewed, edited to match the conventions above,
and committed as plain Java/Gherkin like everything else in this repo — the
AI tooling itself leaves no trace in the committed test run.
