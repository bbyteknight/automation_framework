@api @smoke
Feature: Objects data-driven creation

  Scenario Outline: Create an object with varying name and price
    When the user creates an object with name "<name>" and data
      | price | <price> |
    Then the object should be created successfully
    And the created object name should be "<name>"
    And the created object price should be <price>

    Examples:
      | name                 | price   |
      | Apple MacBook Pro 16 | 1849.99 |
      | Apple MacBook Air M2 | 1199.00 |
      | Dell XPS 13          | 999.99  |
