@api @database @nosql @smoke
Feature: NoSQL data validation (MongoDB documents and Redis keys)

  The tag above validates against an in-process, pure-Java fake MongoDB/Redis
  (hooks.NoSqlTestInfraHooks) when mongoUri/redisHost aren't configured — zero external setup.
  Point mongoUri/redisHost/redisPort at a real MongoDB/Redis instance via config to run these
  same steps against it instead; see CONTRIBUTING.md "Validating NoSQL stores".

  Scenario: A document can be seeded and validated in MongoDB
    Given a document is inserted into the "orders" collection with:
      | customerName | Jane Doe |
      | status       | PENDING  |
    Then the "orders" collection should contain a document where "customerName" is "Jane Doe"
    And the "orders" collection should contain a document where:
      | customerName | Jane Doe |
      | status       | PENDING  |
    And the "status" field of the document in "orders" where "customerName" is "Jane Doe" should be "PENDING"
    And the document is removed from the "orders" collection where "customerName" is "Jane Doe"

  Scenario: A cache key can be set and validated in Redis
    Given the "session:abc123" key is set to "active" in Redis
    Then the "session:abc123" key should exist in Redis
    And the "session:abc123" key in Redis should equal "active"
    And the "session:abc123" key is deleted from Redis
    And the "session:abc123" key should not exist in Redis
