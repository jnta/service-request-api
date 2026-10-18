Feature: Analyst geographic authorization

  Scenario: Analyst accesses a covered state
    Given an ANALYST covers DF and GO
    And a solicitation belongs to DF
    When the analyst accesses it
    Then access should be allowed

  Scenario: Analyst accesses an uncovered state
    Given an ANALYST covers DF
    And a solicitation belongs to SP
    When the analyst accesses it
    Then access should be denied

  Scenario: Analyst cannot use the client solicitation endpoint
    Given an ANALYST covers the solicitation's state
    When the analyst reads it through GET /solicitations/{id}
    Then access should be denied
    And the analyst-specific endpoint remains available

  Scenario: Client cannot use the analyst solicitation endpoint
    Given a CLIENT owns a solicitation
    When the client reads it through GET /analyst/solicitations/{id}
    Then access should be denied

  Scenario: Admin accesses any state
    Given an ADMIN
    And a solicitation belongs to AM
    When the admin accesses it
    Then access should be allowed

  Scenario: Client accesses own solicitation
    Given a CLIENT owns a solicitation
    When the client accesses it
    Then access should be allowed
