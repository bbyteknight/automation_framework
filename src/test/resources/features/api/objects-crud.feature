@api @smoke
Feature: Objects CRUD

  Scenario: Full CRUD lifecycle for an object
    Given the user retrieves all objects
    Then the list of objects should not be empty
    When the user creates an object with name "Apple MacBook Pro 16" and data
      | year           | 2019          |
      | price          | 1849.99       |
      | CPU model      | Intel Core i9 |
      | Hard disk size | 1 TB          |
    Then the object should be created successfully
    When the user retrieves the created object
    Then the retrieved object should have name "Apple MacBook Pro 16"
    When the user updates the created object with name "Apple MacBook Pro 16" and data
      | year           | 2019          |
      | price          | 2049.99       |
      | CPU model      | Intel Core i9 |
      | Hard disk size | 1 TB          |
      | color          | silver        |
    Then the object should be updated successfully
    When the user deletes the created object
    Then the object should be deleted successfully
    And the deleted object should no longer exist
