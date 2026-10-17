Feature: Solicitation submission

  Scenario: Submitting a complete solicitation
    Given a client owns a complete DRAFT solicitation
    And terms have been accepted
    When the client submits it
    Then its status should become SUBMITTED
    And it should no longer be editable

  Scenario: Rejecting incomplete submission
    Given a solicitation does not have a completed address
    When the owner submits it
    Then the API should return 422
    And status should remain DRAFT

  Scenario: Submitting twice
    Given a solicitation is already SUBMITTED
    When the owner submits it again
    Then the API should return 409

  Scenario: Non-owner submits solicitation
    Given client A owns a complete solicitation
    When client B attempts to submit it
    Then access should be denied

  Scenario: Rejecting submission without accepted terms
    Given all solicitation steps are complete
    And terms have not been accepted
    When the client submits it
    Then submission should be rejected
    And status should remain DRAFT
