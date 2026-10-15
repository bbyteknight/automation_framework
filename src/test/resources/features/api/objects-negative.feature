@api @smoke
Feature: Objects negative scenarios

  Scenario: Requesting a non-existent object returns 404
    When the user requests an object with id "does-not-exist-123456"
    Then the response status code should be 404
    And the response should contain an error message

  Scenario: Creating an object with a malformed request body returns 400
    When the user submits a malformed create-object request
    Then the response status code should be 400

  Scenario: Creating an object with a missing request body returns 400
    When the user submits a create-object request with no body
    Then the response status code should be 400

  # api.restful-api.dev has no authentication of its own — this checks the
  # framework handles a 401 correctly using a dedicated status-code test endpoint.
  Scenario: An unauthorized request returns 401
    When the user makes a request to a protected endpoint without credentials
    Then the response status code should be 401
