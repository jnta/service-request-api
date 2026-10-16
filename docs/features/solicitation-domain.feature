Feature: Solicitation domain

  Scenario: Creating a solicitation
    Given a valid client
    When a solicitation is created
    Then its status should be DRAFT
    And its current step should be BASIC_DATA

  Scenario: Completing basic data
    Given a solicitation in DRAFT at BASIC_DATA
    When valid basic data is provided
    Then the current step should become ADDRESS

  Scenario: Rejecting invalid basic data
    Given a solicitation in DRAFT at BASIC_DATA
    When a title shorter than 3 characters is provided
    Then basic data should be rejected
    And the current step should remain BASIC_DATA

  Scenario: Completing address
    Given a solicitation whose basic data is complete
    When a valid address is provided
    Then the current step should become CONFIRMATION

  Scenario: Rejecting address before basic data
    Given a solicitation whose current step is BASIC_DATA
    When address data is provided
    Then the operation should be rejected

  Scenario: Completing confirmation
    Given a solicitation whose address is complete
    When valid confirmation data is provided
    Then the solicitation should be ready for submission

  Scenario: Rejecting a preferred date in the past
    Given a fixed clock for today
    When a preferred date before today is provided
    Then confirmation should be rejected

  Scenario: Rejecting low estimated value for high priority
    Given priority HIGH is selected
    When estimated value is lower than 100
    Then confirmation should be rejected

  Scenario: Saving confirmation without accepting terms
    Given all other confirmation fields are valid
    When terms are not accepted
    Then confirmation may be saved on the draft
    But submission should be rejected

  Scenario: Submitting a complete solicitation
    Given all solicitation steps are complete
    And terms have been accepted
    When the client submits the solicitation
    Then its status should become SUBMITTED

  Scenario: Rejecting incomplete submission
    Given a solicitation missing address information
    When the client submits the solicitation
    Then submission should fail
    And status should remain DRAFT

  Scenario: Preventing modifications after submission
    Given a solicitation with status SUBMITTED
    When basic data is updated
    Then the update should be rejected

  Scenario: Starting review
    Given a solicitation with status SUBMITTED
    When an analyst starts review
    Then its status should become IN_REVIEW

  Scenario: Approving a solicitation
    Given a solicitation with status IN_REVIEW
    When an analyst approves it
    Then its status should become APPROVED

  Scenario: Rejecting a solicitation
    Given a solicitation with status IN_REVIEW
    When an analyst rejects it
    Then its status should become REJECTED

  Scenario: Preventing approval of a draft
    Given a solicitation with status DRAFT
    When an analyst attempts to approve it
    Then the operation should be rejected
