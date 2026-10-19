@api @smoke
Feature: Objects CRUD

  Scenario: Full CRUD lifecycle for an object
    Given the user retrieves all objects
    Then the list of objects should not be empty
    When the user creates an object using test data "macbookPro16"
    Then the object should be created successfully
    When the user retrieves the created object
    Then the retrieved object should have the expected name
    When the user updates the created object using test data "macbookPro16Updated"
    Then the object should be updated successfully
    When the user deletes the created object
    Then the object should be deleted successfully
    And the deleted object should no longer exist
