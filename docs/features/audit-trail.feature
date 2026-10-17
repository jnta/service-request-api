Feature: Audit trail

  Scenario: Auditing a submission
    Given a client owns a complete solicitation
    When the client submits the solicitation
    Then an audit record should be created
    And it should contain the client id
    And action should be SOLICITATION_SUBMITTED
    And it should identify the solicitation and timestamp
    And success should be true

  Scenario: Auditing a failed submission
    Given a client owns an incomplete solicitation
    When the client submits the solicitation
    Then an audit record should be created with success false

  Scenario: Read operations are not audited
    Given a client reads their solicitation
    Then no mutation audit entry should be created

  Scenario: Audit records do not contain request data
    When a solicitation is changed
    Then the audit record should contain only actor, action, aggregate, timestamp, and outcome
