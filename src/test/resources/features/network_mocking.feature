@web @bidi @smoke
Feature: Network mocking via WebDriver BiDi

  Uses WebDriver BiDi's Network module (utils.NetworkMockUtil) to intercept the browser's own
  network traffic and fabricate a response — simulating a backend failure (or any edge-case
  response) without changing, or even reaching, any real backend. Since no page in this framework
  makes its own API call yet, "the browser fetches" issues one itself via fetch(); point the same
  Given/Then steps at a real page's own API call once one exists.

  Known local-environment caveat (not a bug here — see utils.NetworkMockUtil's own javadoc):
  these scenarios were implemented and verified against Selenium's own officially tested
  addIntercept/onBeforeRequestSent/provideResponse usage (java/test/.../NetworkCommandsTest.java
  in the Selenium repo), but could not be locally verified end-to-end in this dev sandbox, where
  the installed Chrome (157.x) is newer than Selenium 4.52.0's bundled CDP/BiDi support for it
  ("Unable to find an exact match for CDP version 157" at startup) — the intercepted request
  simply never fires its event on this specific browser build, which looks identical to a script
  timeout. CI's Chrome build is expected to be in step with Selenium's supported range; re-verify
  there, and re-check this note against whatever Selenium/Chrome versions are installed if it
  still doesn't trigger.

  Scenario: A mocked 500 response is returned without the request ever reaching a real backend
    Given network mocking is enabled
    And the API "https://the-internet.herokuapp.com/api/orders" returns status 500 with this mock body:
      """
      {"error": "Service unavailable"}
      """
    When the browser fetches "https://the-internet.herokuapp.com/api/orders"
    Then the fetch response status should be 500
    And the fetch response body should contain "Service unavailable"

  Scenario: A mocked 404 with no body is returned without the request ever reaching a real backend
    Given network mocking is enabled
    And the API "https://the-internet.herokuapp.com/api/missing" returns status 404
    When the browser fetches "https://the-internet.herokuapp.com/api/missing"
    Then the fetch response status should be 404

  Scenario: Basic auth is completed via BiDi without a native browser credential prompt
    Given network mocking is enabled
    And basic auth is handled with username "admin" and password "admin"
    When the user visits "https://the-internet.herokuapp.com/basic_auth"
    Then the page should show the text "Congratulations"
