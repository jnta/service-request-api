Feature: Solicitation ownership

  Scenario: Client creates a solicitation
    Given an authenticated CLIENT
    When the client creates a solicitation
    Then a new solicitation should be created
    And the authenticated client should own it
    And its status should be DRAFT

  Scenario: Client reads own solicitation
    Given a CLIENT owns a solicitation
    When the client requests that solicitation
    Then the solicitation should be returned

  Scenario: Client cannot read another client's solicitation
    Given client A owns a solicitation
    And client B is authenticated
    When client B requests client A's solicitation
    Then access should be denied

  Scenario: Unknown solicitation
    Given an authenticated client
    When a non-existing solicitation is requested
    Then the API should return 404

  Scenario: Owner completes basic data
    Given the client owns a DRAFT solicitation
    When valid basic data is submitted
    Then the basic data should be persisted
    And currentStep should become ADDRESS

  Scenario: Rejecting short title
    Given the client owns a DRAFT solicitation
    When a title shorter than 3 characters is submitted
    Then the API should return a validation error
    And currentStep should remain BASIC_DATA

  Scenario: Rejecting modification by another client
    Given client A owns a solicitation
    When client B tries to update its basic data
    Then access should be denied

  Scenario: Rejecting modification after submission
    Given a solicitation is SUBMITTED
    When its owner attempts to change basic data
    Then the API should return a business conflict
