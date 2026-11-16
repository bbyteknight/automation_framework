@web @bidi @smoke
Feature: Capture console logs and JavaScript errors via WebDriver BiDi

  Uses WebDriver BiDi's LogInspector module (utils.JsErrorMonitor) to subscribe to the browser's
  own console output and uncaught JavaScript exceptions for every @web scenario (wired in
  hooks.Hooks#setUpWeb/#captureJsErrors) — no page instrumentation required. The full console log
  is attached to Allure whenever a scenario fails. Whether a captured JS error should also fail the
  scenario itself is controlled by the failOnJsError config flag (false by default — see
  config/dev.properties — so an existing suite doesn't start failing the moment this is enabled);
  jsErrorAllowList filters out known-harmless errors by substring before either check runs.

  These scenarios deliberately trigger their own console.error()/uncaught exception via JS
  (the-internet.herokuapp.com has no page of its own that throws) purely to prove capture works;
  they assert against utils.JsErrorMonitor's in-memory results rather than relying on
  failOnJsError=true, so they stay green under the suite's default config. To see a scenario
  actually fail the build because of a captured JS error, run e.g.:
    mvn test -Dcucumber.filter.name="A console error is captured" -DfailOnJsError=true
  (reset or omit -DfailOnJsError afterwards — it is not meant to be left on for the whole suite
  until every page in scope is known to be clean).

  Scenario: A console error is captured by JsErrorMonitor
    When the page logs a console error "Checkout totals API returned malformed JSON"
    Then the browser console should report a JS error containing "Checkout totals API returned malformed JSON"

  Scenario: An uncaught JavaScript exception is captured by JsErrorMonitor
    # Chrome's BiDi bridge reports uncaught exceptions as a bare "Uncaught Error", without the
    # thrown Error's own message (observed on a real run — see ConsoleLogSteps' own comment) —
    # another instance of this card's "BiDi API still evolving" checklist item, so this only
    # asserts an uncaught exception was captured at all, not its message content.
    When the page throws an uncaught JavaScript error "Cannot read properties of undefined (reading 'total')"
    Then the browser console should report an uncaught JS error

  Scenario: An allow-listed console error is logged but not counted as a failure
    When the page logs a console error "favicon.ico failed to load"
    Then the browser console log should contain "favicon.ico failed to load"
    And the browser console should report no JS errors
