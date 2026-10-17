Feature: User authentication and roles

  Scenario: Registering a client
    Given an email that is not registered
    When a user registers
    Then a CLIENT account should be created
    And the password should not be stored in plain text

  Scenario: Rejecting duplicate email registration
    Given an existing user with the same email
    When another registration uses that email
    Then registration should fail with conflict

  Scenario: Authenticating valid credentials
    Given a registered client
    When the client submits the correct credentials
    Then a signed JWT should be returned

  Scenario: Rejecting invalid credentials
    Given a registered user
    When an incorrect password is provided
    Then authentication should fail

  Scenario: Admin creates an analyst
    Given an authenticated ADMIN
    When the admin creates an ANALYST with DF and GO coverage
    Then the analyst should be created
    And the coverage should be persisted

  Scenario: Preventing privilege escalation
    Given an authenticated CLIENT
    When the client calls the admin user creation endpoint
    Then access should be denied
