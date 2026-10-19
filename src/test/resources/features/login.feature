@web @smoke
Feature: Login

  Scenario: Successful login with valid credentials
    Given the user is on the login page
    When the user logs in with the "standardUser" test account
    Then the user should land on the secure area

  Scenario Outline: Failed login with invalid credentials
    Given the user is on the login page
    When the user logs in with username "<username>" and password "<password>"
    Then the user should see an error message containing "<errorMessage>"

    Examples:
      | username     | password             | errorMessage               |
      | invalidUser  | SuperSecretPassword! | Your username is invalid!  |
      | tomsmith     | wrongPassword        | Your password is invalid!  |
      | invalidUser  | wrongPassword        | Your username is invalid!  |
