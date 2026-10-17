Feature: Reliable search indexing

  Scenario: Domain mutation creates an outbox event in the same transaction
    Given a solicitation exists
    When it is updated
    Then the solicitation change should be committed
    And an outbox event should be committed in the same transaction

  Scenario: Elasticsearch is temporarily unavailable
    Given Elasticsearch is unavailable
    When a solicitation is updated
    Then the PostgreSQL transaction should still succeed
    And the outbox event should remain pending

  Scenario: Pending event is retried
    Given an unprocessed outbox event exists
    And Elasticsearch becomes available
    When the outbox processor runs
    Then the Elasticsearch document should be updated
    And the event should be marked as processed

  Scenario: Processing the same event more than once
    Given an already indexed solicitation event
    When the same event is processed again
    Then the final Elasticsearch document should remain correct
