# Tagging Strategy & Gherkin Style Guide

Standardizes how `.feature` files are written and how scenarios are tagged
and filtered across Web, API, and Mobile suites.

## Tags

Every scenario carries **exactly one platform tag** and **zero or more suite tags**.

### Platform tags (pick exactly one per scenario)

| Tag | Meaning | Driver behavior |
|---|---|---|
| `@web` | Browser/Selenium scenario | `Hooks.setUpWeb()` starts/quits the browser |
| `@android` | Android/Appium scenario | `Hooks.setUpMobile()` starts an Android driver |
| `@ios` | iOS/Appium scenario | `Hooks.setUpMobile()` starts an iOS driver |
| `@api` | API-only scenario (REST Assured) | No driver is started — `@Before`/`@After` are scoped to `@web`/`@android`/`@ios` only |

A scenario must always carry one of these so hooks know what (if anything)
to spin up. Never combine two platform tags on the same scenario (e.g.
`@web @android`) — split into separate scenarios instead.

### Suite tags (combine freely with a platform tag)

| Tag | Meaning |
|---|---|
| `@smoke` | Small, fast, critical-path set — run on every PR |
| `@regression` | Full coverage — run on a schedule / before release |
| `@wip` | Work in progress — excluded from CI by default |
| `@flaky` | Known-unstable — excluded from the pass/fail gate, tracked for fixing |

Example:

```gherkin
@web @smoke
Feature: Login

  Scenario: Successful login with valid credentials
    Given the user is on the login page
    When the user logs in with username "tomsmith" and password "SuperSecretPassword!"
    Then the user should land on the secure area
```

### Running by tag

```bash
# Smoke only
mvn test -Dcucumber.filter.tags="@smoke"

# Web regression, excluding anything flaky
mvn test -Dcucumber.filter.tags="@web and @regression and not @flaky"

# Everything except work-in-progress (CI default)
mvn test -Dcucumber.filter.tags="not @wip"
```

## Gherkin writing rules

1. **Declarative, not imperative.** Describe *what* the user does, not *how* the UI does it.
   - ✅ `When the user logs in with valid credentials`
   - ❌ `When the user clicks the username field and types "tomsmith" and clicks the password field...`
2. **One behavior per scenario.** A scenario should verify a single outcome. If you find yourself
   writing "and also check that...", split it into another scenario.
3. **Given/When/Then order.** Use `Given` for preconditions, `When` for the action under test,
   `Then` for the outcome. Use `And`/`But` to continue the previous step type, not to start a new phase.
4. **No conjunctions in step text.** Avoid steps like `When the user logs in and navigates to settings`
   — split into two steps so each is independently reusable.
5. **Background for shared setup only.** Use `Background:` for steps every scenario in the feature
   needs (e.g. `Given the user is on the login page`), not for one-off setup used by a single scenario.
6. **Scenario Outline for data variation.** Use `Scenario Outline` + `Examples` when the same behavior
   is tested with different data, instead of copy-pasting near-identical scenarios.
7. **No implementation detail in step text.** Step text must stay stable even if the underlying
   page/locator/API client changes — that logic belongs in the step definition, not the feature file.
8. **Scenario titles describe the behavior, not the mechanics.** Title = "what is being verified",
   not "what buttons get clicked."
9. **Keep feature files platform-pure.** Don't mix `@web` and `@api` scenarios in the same feature
   file unless they're testing the same business behavior from two angles — prefer separate files
   under `features/web/`, `features/api/`, `features/mobile/` as the suite grows.

## Naming conventions

- Feature file name: `kebab-case.feature`, matching the feature's subject (e.g. `login.feature`).
- `Feature:` title: short noun phrase (e.g. `Feature: Login`).
- `Scenario:` title: full sentence describing the behavior (e.g.
  `Scenario: Successful login with valid credentials`).
