Feature: API error contract

  Scenario: Bean validation error
    When invalid request fields are submitted
    Then the API should return 400
    And the error should have a stable code and timestamp
    And errors should identify each invalid field

  Scenario: Malformed request body
    When a malformed JSON request body is submitted
    Then the API should return 400
    And the error should use code MALFORMED_REQUEST

  Scenario: Business invariant violation
    Given an incomplete solicitation
    When submission is attempted
    Then the API should return 422
    And the response should use code SOLICITATION_INCOMPLETE
    And errors should identify each incomplete field

  Scenario: Concurrent update
    Given the submitted entity version is stale
    When an update is attempted
    Then the API should return 409
    And the response should use code STATE_CONFLICT

  Scenario: Unauthenticated request
    When a protected endpoint is requested without a token or with an invalid bearer token
    Then the API should return 401
    And the response should use the common error format

  Scenario: Forbidden request
    Given an authenticated user without the required role
    When a protected endpoint is requested
    Then the API should return 403
    And the response should use the common error format
