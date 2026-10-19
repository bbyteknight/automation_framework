Feature: Mobile Login

  @android
  Scenario: Successful login with demo credentials on Android
    Given the user is on the home screen
    When the user opens the login screen
    And the user submits login with username "bod@example.com" and password "10203040"
    Then the user should be logged in

  # Same scenario, targeting iOS via LoginScreen/HomeScreen's @iOSXCUITFindBy locators.
  # Tagged @wip (excluded by the runner's default "not @wip" filter) because there is no
  # iOS/macOS environment available here to verify the iOS locators or actually run it.
  @ios @wip
  Scenario: Successful login with demo credentials on iOS
    Given the user is on the home screen
    When the user opens the login screen
    And the user submits login with username "bod@example.com" and password "10203040"
    Then the user should be logged in
