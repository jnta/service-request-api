Feature: Analyst review workflow

  Scenario: Analyst reads a solicitation in a covered state
    Given a solicitation belongs to a state covered by the analyst
    When the analyst requests GET /analyst/solicitations/{id}
    Then the solicitation should be returned

  Scenario: Analyst cannot read a solicitation outside coverage
    Given a solicitation belongs to a state not covered by the analyst
    When the analyst requests GET /analyst/solicitations/{id}
    Then access should be denied

  Scenario: Analyst starts review
    Given a SUBMITTED solicitation in an analyst's coverage
    When the analyst starts the review
    Then status should become IN_REVIEW

  Scenario: Analyst approves a solicitation
    Given an IN_REVIEW solicitation
    And the analyst covers its state
    When the analyst approves it with a comment
    Then status should become APPROVED

  Scenario: Decision comment has the required length
    Given a solicitation in review
    When the analyst decides with a comment shorter than 10 characters
    Then the decision should be rejected

  Scenario: Analyst rejects a solicitation
    Given an IN_REVIEW solicitation
    When the analyst rejects it with a valid comment
    Then status should become REJECTED

  Scenario: Auditing an approval
    Given an analyst approves a solicitation
    Then an audit record should identify the analyst
    And the solicitation
    And the APPROVED action

  Scenario: Auditing a rejection
    Given an analyst rejects a solicitation
    Then an audit record should identify the analyst
    And the solicitation
    And the REJECTED action

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
