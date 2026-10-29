Feature: Desktop login

  Sample TestLoginApp (WinForms, Card 35) workflow — proves DriverFactory/
  Hooks/DesktopBaseScreen end to end. Requires -Dplatform=windows and
  desktopAppPath pointing at the built TestLoginApp.exe (see
  DESKTOP_AUTOMATION_SETUP.md and TestLoginApp/README.md).

  @desktop @smoke
  Scenario: Successful login shows the dashboard
    Given the Test Login App is launched
    When the user logs in to the desktop app with username "admin" and password "Password123"
    Then the dashboard should be displayed
    And the welcome message should be "Welcome, admin!"
    And the status should be "Status: Logged in successfully"

  @desktop @smoke
  Scenario: Invalid login shows an error
    Given the Test Login App is launched
    When the user logs in to the desktop app with username "admin" and password "wrong" expecting an error
    Then the error message should be "Invalid username or password."

  @desktop @smoke
  Scenario: Logging out returns to the login screen
    Given the Test Login App is launched
    When the user logs in to the desktop app with username "admin" and password "Password123"
    And the user logs out
    Then the login screen should be displayed again
