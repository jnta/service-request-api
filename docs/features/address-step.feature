Feature: Solicitation address step

  Scenario: Completing address using ViaCEP
    Given a solicitation whose current step is ADDRESS
    And ViaCEP returns a valid address for the provided CEP
    When the client submits the CEP and number
    Then the address should be populated using ViaCEP data
    And currentStep should become CONFIRMATION

  Scenario: ViaCEP returns an invalid CEP
    Given a solicitation whose current step is ADDRESS
    And ViaCEP reports the CEP does not exist
    When the client submits that CEP
    Then the address step should fail
    And no partial address should be persisted
    And currentStep should remain ADDRESS

  Scenario: ViaCEP is unavailable
    Given ViaCEP is unavailable
    When the client attempts to complete the address
    Then the API should return an integration error
    And the solicitation should remain unchanged

  Scenario: ViaCEP exceeds the configured read timeout
    Given a solicitation whose current step is ADDRESS
    And ViaCEP does not respond within the configured read timeout
    When the client attempts to complete the address
    Then the API should return an integration error
    And no partial address should be persisted
    And currentStep should remain ADDRESS

  Scenario: Rejecting address step before basic data
    Given a solicitation whose current step is BASIC_DATA
    When the client attempts to submit address data
    Then the operation should be rejected
