@api @database @seed-orders @smoke
Feature: DB-based test data setup and cleanup

  The @seed-orders tag (hooks.DbSeedHooks) seeds one order row directly in the database before
  each scenario below and removes it afterward via db.DbTestDataManager — fast, isolated setup
  with no UI/API round-trip, and no leftover data regardless of whether the scenario passes.

  Scenario: A seeded order is visible to the scenario and backed by a real database row
    Then the seeded order should exist in the "orders" table with status "PENDING"

  Scenario: Updating the seeded order is reflected immediately in the database
    When the seeded order's status is updated to "SHIPPED"
    Then the seeded order should exist in the "orders" table with status "SHIPPED"
