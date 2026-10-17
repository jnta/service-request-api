Feature: Complete solicitation lifecycle

  Scenario: Client creates a solicitation and an analyst approves it
    Given a registered CLIENT
    And a registered ANALYST with coverage for DF
    When the client authenticates
    And creates a solicitation
    And fills valid basic data
    And fills an address with a valid DF CEP
    And fills valid confirmation data
    And submits the solicitation
    Then the solicitation status should be SUBMITTED
    When the outbox processor updates the search projection
    And the analyst authenticates
    And searches for submitted solicitations in DF
    Then the created solicitation should be present
    When the analyst starts the review
    And approves the solicitation
    Then the solicitation status should be APPROVED
    And the PostgreSQL record should be APPROVED
    And the Elasticsearch projection should be APPROVED
    And the approval should be present in the audit log
