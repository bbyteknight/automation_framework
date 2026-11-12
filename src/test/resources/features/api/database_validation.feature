@api @database @smoke
Feature: Database validation against the Objects API

  Validates that db.DbClient's generic table/column steps can confirm a record exists and that
  its values match what the API returned — standing in for the real case of asserting an API
  call's effect on a backend database. synced_objects is a sample table (created on the fly by
  "the synced_objects table is ready"), not a real downstream service; see CONTRIBUTING.md
  "Querying the database" for using these steps against a real application table/column.

  Background:
    Given the synced_objects table is ready

  Scenario: A created object is synced correctly to the database
    When the user creates an object with name "Apple MacBook Pro 16" and data
      | price | 1849.99 |
    Then the object should be created successfully
    And the created object is synced to the database
    And the "synced_objects" table should contain a synced record for "createdObject"
    And the "object_name" column in the "synced_objects" table should equal the "name" field of "createdObject"

  Scenario: The synced record can be queried by its individual column values
    When the user creates an object with name "Dell XPS 13" and data
      | price | 999.99 |
    Then the object should be created successfully
    And the created object is synced to the database
    And the "synced_objects" table should contain a record where "object_name" is "Dell XPS 13"
    And the "synced_objects" table should contain a record where:
      | object_name | Dell XPS 13 |
    And the "object_name" column in the "synced_objects" table should be "Dell XPS 13" where "object_name" is "Dell XPS 13"
