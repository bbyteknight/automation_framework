@android
Feature: Mobile Shopping Cart

  Scenario: Add a product to the cart
    Given the user is on the products screen
    When the user adds the "classicBackpack" test product to the cart
    And the user opens the cart
    Then the cart should match the expected item count and total
