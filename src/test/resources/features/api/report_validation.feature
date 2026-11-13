@api @reports @smoke
Feature: Exported CSV/Excel report validation

  Verifies an exported report end to end: an object is created via the API, "exported" to a CSV
  and an Excel report (steps.ReportSteps, since no real report-export feature exists in this
  framework yet), and the exported file's header row, row count, and cell values are checked —
  including a direct comparison back against the API response that produced it.

  Scenario: A CSV report exported from a created object matches the API data
    When the user creates an object with name "Apple MacBook Pro 16" and data
      | price | 1849.99 |
    Then the object should be created successfully
    And the created object is exported to a CSV report
    And the exported report header row should be:
      | id | name | price |
    And the exported report should contain 1 data row
    And the "price" column of row 0 in the exported report should be "1849.99"
    And the "name" column of the exported report should equal the "name" field of "createdObject"

  Scenario: An Excel report exported from a created object matches the API data
    When the user creates an object with name "Dell XPS 13" and data
      | price | 999.99 |
    Then the object should be created successfully
    And the created object is exported to an Excel report
    And the exported report header row should be:
      | id | name | price |
    And the exported report should contain 1 data row
    And the "price" column of row 0 in the exported report should be "999.99"
    And the "name" column of the exported report should equal the "name" field of "createdObject"
