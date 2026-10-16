Feature: Solicitation persistence

  Scenario: Persisting a draft solicitation
    Given a newly created solicitation
    When the solicitation is saved
    And the solicitation is loaded again
    Then its status should still be DRAFT
    And its current step should still be BASIC_DATA

  Scenario: Persisting partially completed workflow
    Given a solicitation with completed basic data
    When the solicitation is saved
    And later loaded
    Then the basic data should be preserved
    And the current step should be ADDRESS

  Scenario: Detecting concurrent modifications
    Given two transactions load the same solicitation version
    When the first transaction updates and commits it
    And the second transaction attempts to save its stale version
    Then the second save should fail with a concurrency conflict
