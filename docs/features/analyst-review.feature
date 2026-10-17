Feature: Analyst review workflow

  Scenario: Analyst starts review
    Given a SUBMITTED solicitation in an analyst's coverage
    When the analyst starts the review
    Then status should become IN_REVIEW

  Scenario: Analyst approves a solicitation
    Given an IN_REVIEW solicitation
    And the analyst covers its state
    When the analyst approves it with a comment
    Then status should become APPROVED

  Scenario: Analyst rejects a solicitation
    Given an IN_REVIEW solicitation
    When the analyst rejects it with a valid comment
    Then status should become REJECTED

  Scenario: Analyst cannot decide outside coverage
    Given an analyst covers DF
    And a solicitation belongs to SP
    When the analyst attempts to decide it
    Then access should be denied

  Scenario: Analyst cannot start review of a draft
    Given a DRAFT solicitation in an analyst's coverage
    When the analyst starts the review
    Then the operation should be rejected

  Scenario: Concurrent analyst decisions
    Given two analysts load the same IN_REVIEW solicitation version
    When analyst A approves it
    And analyst B rejects the stale version
    Then analyst A's decision should succeed
    And analyst B should receive a conflict response
