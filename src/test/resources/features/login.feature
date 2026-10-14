@web @smoke
Feature: Login

  Scenario: Successful login with valid credentials
    Given the user is on the login page
    When the user logs in with username "tomsmith" and password "SuperSecretPassword!"
    Then the user should land on the secure area
