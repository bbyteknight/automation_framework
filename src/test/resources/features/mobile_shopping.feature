@android
Feature: Mobile Shopping Cart

  Scenario: Add a product to the cart
    Given the user is on the products screen
    When the user adds "Sauce Labs Backpack" to the cart
    And the user opens the cart
    Then the cart should contain "1 Items"
    And the cart total should be "$ 29.99"
