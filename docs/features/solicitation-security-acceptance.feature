Feature: Solicitation authorization and workflow safeguards

  Scenario: Client cannot access another client's solicitation
    Given client A owns a solicitation
    And client B is authenticated
    When client B requests client A's solicitation
    Then access should be denied

  Scenario: Analyst cannot see a solicitation outside coverage
    Given an analyst covers DF
    And a solicitation belongs to SP
    When the analyst requests the solicitation
    Then access should be denied

  Scenario: Analyst cannot force another state through search filters
    Given an analyst covers DF
    And Elasticsearch contains a solicitation from SP
    When the analyst searches with state SP
    Then no SP solicitation should be returned

  Scenario: Submitted solicitation cannot be edited
    Given a solicitation has status SUBMITTED
    When its owner attempts to update basic data
    Then the operation should be rejected

  Scenario: Invalid CEP does not advance the workflow
    Given a solicitation is at the ADDRESS step
    And ViaCEP reports an unknown CEP
    When the client submits that CEP
    Then the address update should fail
    And the current step should remain ADDRESS

  Scenario: Incomplete solicitation cannot be submitted
    Given a solicitation is missing required steps
    When its owner submits it
    Then submission should fail with SOLICITATION_INCOMPLETE
    And its status should remain DRAFT
