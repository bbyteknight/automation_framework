@web @bidi @smoke
Feature: Network performance assertions via WebDriver BiDi

  Uses WebDriver BiDi's Network module (utils.NetworkPerformanceMonitor) to passively observe
  every request/response a @web scenario makes — timing and status alike — without intercepting
  or blocking any of them. hooks.Hooks starts one per scenario automatically (no setup step
  needed) and, after every @web scenario, attaches a "Network Performance" report to Allure
  listing the slowest requests and any 4xx/5xx/network-level failures — visible in every run,
  not only a failed one. steps.NetworkPerformanceSteps adds the explicit per-scenario assertions
  below on top of that always-on reporting.

  Because this only subscribes to plain BiDi notifications (no addIntercept), it is unaffected by
  the Chrome/Selenium BiDi-version gap documented on NetworkMockUtil/network_mocking.feature —
  that gap is specific to blocking request interception, not passive event notification.

  Scenario: All requests to load the login page complete within a reasonable time
    Then no request should take longer than 10 seconds

  Scenario: A failing page response is detected during a page flow
    When the user visits "https://the-internet.herokuapp.com/status_codes/404"
    Then a 4xx or 5xx response should have been detected

  Scenario: The suite's own pages return no failed responses
    Then no network response should be a 4xx or 5xx error
