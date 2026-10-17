Feature: Solicitation confirmation step

  Scenario: Completing confirmation
    Given a solicitation whose current step is CONFIRMATION
    When valid confirmation data is submitted
    Then the confirmation should be persisted
    And the solicitation should become ready for submission

  Scenario: Preferred date is in the past
    Given a fixed clock for today
    When a preferred date before today is submitted
    Then confirmation should be rejected

  Scenario: High priority requires a minimum estimated value
    Given priority is HIGH
    When estimated value is below 100 BRL
    Then confirmation should be rejected

  Scenario: Terms are not accepted
    Given all other confirmation fields are valid
    When termsAccepted is false
    Then confirmation may be saved on the draft
    But submission must not be allowed
