# Desktop Automation Setup (Windows / NovaWindows)

Status: fully wired up locally — driver installed, `DriverFactory`/`Hooks`
support `@desktop` scenarios via `-Dplatform=windows`, and a sample
`DesktopBaseScreen`/screen objects/feature file exist and pass consistently
(two consecutive clean runs, 4/4 scenarios, `mvn test -Pdesktop
-Dplatform=windows`) against the sample TestLoginApp. A `desktop` CI job
exists in `.github/workflows/ci.yml` but needs a provisioned self-hosted
Windows runner before it can actually execute — see "CI: self-hosted
Windows runner" and "Known limitations" below.

## Why NovaWindows instead of WinAppDriver

WinAppDriver is no longer maintained. [NovaWindows
driver](https://github.com/AppiumTestDistribution/appium-novawindows-driver)
is the actively developed replacement — same WinAppDriver-style capability
model (`platformName=Windows`, `app=<exe path or AUMID>`), but runs as a
regular Appium driver (PowerShell-backed) instead of a separate background
service, and supports Win32, WinForms, WPF, and UWP apps.

## Prerequisites

- Windows 10/11 host (this is a host requirement, not emulated).
- Node.js + Appium 2 or 3 server installed.

**Important finding:** NovaWindows driver requires **Appium 3**
(`^3.0.0-rc.2` or later). Installing it against the Appium 2.x line fails
outright:

```
Error: 'appium-novawindows-driver' cannot be installed because the server
version it requires (^3.0.0-rc.2) does not meet the currently installed one
(2.19.0). Please install a compatible server version first.
```

This machine had Appium 2.19.0 installed (used for the existing
Android/iOS mobile layer — `appium-java-client` in `pom.xml`). It was
upgraded globally to Appium 3.9.0 (`npm install -g appium@latest`) to
unblock the NovaWindows install.

**Side effect to be aware of:** after the upgrade, the existing
`xcuitest` driver (iOS) logs a compatibility warning on every server start,
since it declares a peer dependency on Appium `^2.4.1`:

```
WARN Appium Driver "xcuitest" has 1 potential problem:
WARN Appium   - Driver "xcuitest" (package `appium-xcuitest-driver`) may be
incompatible with the current version of Appium (v3.9.0) due to its peer
dependency on Appium ^2.4.1. Please install a compatible version of the
driver.
```

It still loaded and the server started fine in this validation, but before
rolling the Appium 3 upgrade out to CI/other machines, re-validate the
Android (`uiautomator2`) and iOS (`xcuitest`) mobile suites against Appium 3,
or pin a newer `xcuitest`/`uiautomator2` driver version known to support it.

## Install steps

```bash
# 1. Confirm/upgrade the Appium server to 3.x
appium -v
npm install -g appium@latest

# 2. Install the NovaWindows driver
appium driver install --source=npm appium-novawindows-driver

# 3. Confirm it's registered
appium driver list --installed
# - uiautomator2@4.2.9 [installed (npm)]
# - xcuitest@5.16.1 [installed (npm)]
# - novawindows@1.5.0 [installed (npm)]
```

No separate WinAppDriver.exe-style service needs to be installed or running
— NovaWindows drives the OS directly via PowerShell from within the Appium
server process itself.

## Validation performed

1. Started the Appium server: `appium server -p 4725`
2. Created a session against Notepad:

```json
{
  "capabilities": {
    "alwaysMatch": {
      "platformName": "Windows",
      "appium:automationName": "NovaWindows",
      "appium:app": "C:/Windows/System32/notepad.exe"
    }
  }
}
```

   `POST /session` with that body returned a `sessionId`, and `Notepad.exe`
   appeared in the process list immediately after — confirming the driver
   can launch and attach to a classic Win32 app.
3. The session auto-closed (and Notepad exited with it) after Appium's
   default 60s idle (`newCommandTimeout`), which is expected behavior:
   `shouldCloseApp` defaults to `true`.

For Calculator (UWP), use its AUMID instead of a file path, e.g.:

```json
"appium:app": "Microsoft.WindowsCalculator_8wekyb3d8bbwe!App"
```

## Framework integration

Desktop is now a supported platform, plugged into the existing framework the
same way web/mobile are:

- `utils.DriverFactory.initDesktopDriver()` — builds a `WindowsOptions`
  capability set (`platformName=Windows`, `automationName=NovaWindows`) and a
  `WindowsDriver`, connecting to the same local Appium server as mobile
  (`appiumServerUrl`). Registers into the same `ThreadLocal<WebDriver>` as
  every other platform, so `DriverFactory.getDriver()`/`quitDriver()` work
  unchanged.
- Platform selection for `@desktop` scenarios is config-driven via
  `-Dplatform=windows` (only `windows` is implemented; unknown values throw).
  `dev.properties`/`qa.properties`/`staging.properties` all default
  `platform=web` (used for Allure reporting), so **`-Dplatform=windows` must
  be passed explicitly** for `@desktop` runs — leaving it off falls through
  to the `web` branch, which fails loudly rather than silently targeting the
  wrong driver.
- Capabilities are externalized, not hardcoded: `desktopAppPath` (`.exe`
  path or a UWP AUMID), `desktopAppArguments`, `desktopAppWorkingDir` — all
  optional/blank-skipped, same pattern as `MobileDriverFactory`.
- `hooks.Hooks`: `@Before("@desktop")` calls `initDesktopDriver()`. `@desktop`
  was added to the `@After` teardown/failure-screenshot hook (screenshot on
  failure, driver quit) but **deliberately left out of `@AfterStep`**
  (screenshot after every step) — see "Known limitations" below for why.
- New `-Pdesktop` Maven profile sets `cucumber.filter.tags=@desktop` (same
  shape as `-Pweb`/`-Pmobile`/`-Papi`), so a full run looks like:
  `mvn test -Pdesktop -Dplatform=windows`.

## Screen objects and sample feature

`base.DesktopBaseScreen` is the desktop equivalent of `BasePage`/`BaseScreen`
— there's no Windows-specific `AppiumFieldDecorator` annotation (unlike
`@AndroidFindBy`/`@iOSXCUITFindBy`), so desktop screens locate everything via
plain `By` locators rather than annotated fields. It provides:

- Core actions: `click(By)`, `type(By, text)`, `getText(By)`, plus
  `waitForVisibility`/`waitForClickability`/`waitForInvisibility`/`isDisplayed`
- **`automationId(String)`** — the primary locator helper, wrapping
  `AppiumBy.accessibilityId(...)`. Every control's `Name` in a WinForms/WPF
  app (or AutomationId in general) should be used this way, not index/xpath.
- Window handling: `getWindowHandles()`, `switchToWindow(handle)`,
  `switchToWindowByTitle(title)`, `maximizeWindow()`, `closeCurrentWindow()`
- Keyboard shortcuts/modifiers: `pressKey(Keys)`, `pressKeyCombo(modifier,
  key)` (e.g. Ctrl+A), a two-modifier overload (e.g. Ctrl+Shift+S), and
  `pressAltF4()`

`screens.desktop.DesktopLoginScreen` / `screens.desktop.DesktopDashboardScreen` are the
sample screen objects (template for future ones), and
`src/test/resources/features/desktop_login.feature` +
`steps.DesktopLoginSteps` are the sample feature — 3 scenarios (successful
login, invalid login, logout) covering the TestLoginApp's full happy/error
path. **Validated as passing consistently**: two consecutive clean runs via
`mvn test -Pdesktop -Dplatform=windows`, 4/4 tests (3 Cucumber scenarios +
the unrelated plain-TestNG `LoginTest`) each time.

## CI: self-hosted Windows runner

`.github/workflows/ci.yml` has a `desktop` job wired up (manual-only, via
`workflow_dispatch` with `platform: desktop` or `all`) that builds, starts
Appium, runs `mvn test -Pdesktop -Dplatform=windows`, and publishes an Allure
+ Cucumber HTML report as pipeline artifacts. It targets
`runs-on: [self-hosted, windows, desktop]` and is `continue-on-error: true`
until a runner with those labels actually exists — this section is that
runner's provisioning checklist.

### Why GitHub-hosted `windows-latest` doesn't work

NovaWindows drives the OS through actual UI Automation (click/type/window
focus), which requires a real, logged-in, **unlocked interactive desktop
session** (Session 1+). GitHub/Azure-hosted Windows runners execute the job
as a non-interactive service account with no desktop to automate — this is
the same reason WinAppDriver-based CI has always needed a self-hosted agent.
A Linux container is a non-starter for the same reason, just more obviously
(no Windows UI stack at all).

### Provisioning checklist (one-time, per machine)

1. **Windows 10/11 VM or physical machine**, a dedicated account for CI (not
   someone's interactive daily-use login).
2. **Enable auto-logon** so the machine boots straight into an unlocked
   desktop with no one needing to physically log in — e.g. Sysinternals
   [Autologon](https://learn.microsoft.com/sysinternals/downloads/autologon),
   or the `AutoAdminLogon`/`DefaultUserName`/`DefaultPassword` registry
   values under `HKLM\SOFTWARE\Microsoft\Windows
   NT\CurrentVersion\Winlogon`.
3. **Disable lock screen / sleep / screensaver** for that account (`powercfg
   /change standby-timeout-ac 0`, Group Policy or registry equivalents) — a
   locked session is as unusable to NovaWindows as no session at all.
4. **Install prerequisites once**: Node.js, `npm install -g appium@3`,
   `appium driver install --source=npm appium-novawindows-driver`, JDK 21,
   Maven (or let `actions/setup-java` handle JDK/Maven per run, as the
   pipeline does) — see "Install steps" above for the exact commands/gotchas
   (notably the Appium 2→3 requirement).
5. **Pre-stage the sample app** at `C:\apps\TestLoginApp\TestLoginApp.exe`
   (`dotnet publish -c Release -o C:\apps\TestLoginApp` from the
   TestLoginApp project) — same precedent as the mobile emulator's Sauce
   Labs demo app being installed manually rather than built by the
   pipeline; the TestLoginApp source isn't part of this repo.
6. **Install the GitHub Actions self-hosted runner so it runs *inside* the
   interactive session, not as a Windows service.** A service-mode runner
   lives in Session 0 and has the same no-desktop problem as the hosted
   runner — defeating the entire point. Instead:
   - Run `config.cmd --labels self-hosted,windows,desktop` to register it.
   - Start it with `run.cmd` from inside the auto-logged-on interactive
     session — e.g. a Task Scheduler task set to "Run only when user is
     logged on", triggered at logon, so it attaches to the real desktop and
     relaunches automatically after a reboot.
7. **Verify** by RDP'ing in (or console access) and confirming the screen is
   genuinely unlocked and showing a desktop — not just "machine is powered
   on" — then trigger the `desktop` job via `workflow_dispatch` and confirm
   `Tests run: 4, Failures: 0` in the job log (what two consecutive local
   runs produced during development, see above).

## Known limitations

- **Don't screenshot mid-scenario on desktop.** NovaWindows' PowerShell-backed
  `SendKeys` is reproducibly destabilized by a screenshot capture landing
  between two keyboard/click actions — observed as `SendWait ... Access is
  denied` on the next `sendKeys`, or a button click silently not registering
  (UI stayed on the login screen with no error, as if the click never
  happened). This is why `@desktop` is excluded from `Hooks.attachStepScreenshot`
  (`@AfterStep`) even though `@web`/`@android`/`@ios` get one after every
  step — desktop only gets a screenshot on failure (`@After`), which is also
  all the original Done criteria asked for.
- **Click reliability.** `DriverFactory.initDesktopDriver()` sets
  `delayBeforeClick`/`delayAfterClick` (default 150ms each, overridable via
  `desktopDelayBeforeClick`/`desktopDelayAfterClick`) — NovaWindows' own
  documented mitigation for clicks/input landing before a control has focus.
  `DesktopBaseScreen.type()` also explicitly clicks the target control before
  `clear()`/`sendKeys()` for the same reason. Even with both, expect this
  driver (early-stage, PowerShell-backed) to be less rock-solid than
  Selenium/mobile Appium drivers — budget for occasional reruns in CI once
  this is wired up there.
- The `desktop` CI job is defined but unproven in a real pipeline run — no
  self-hosted runner carrying the `self-hosted, windows, desktop` labels is
  registered yet, so `continue-on-error: true` keeps it from blocking the
  rest of the workflow until one is provisioned (see "CI: self-hosted
  Windows runner" above). The PowerShell `Start-Process`/readiness-polling
  steps are modeled directly on the manual validation that did run
  successfully, but haven't themselves executed inside an actual GitHub
  Actions job.
- Azure Pipelines (`azure-pipelines.yml`) has no desktop stage yet — only
  GitHub Actions does.
- Re-validate `uiautomator2`/`xcuitest` against Appium 3.9.0 before the
  Appium 3 upgrade reaches shared/CI machines (see warning above).
