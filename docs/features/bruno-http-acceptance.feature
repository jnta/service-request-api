Feature: Repeatable HTTP acceptance through Bruno

  Scenario: Run the happy paths and security checks against a local API
    Given the application, PostgreSQL, Elasticsearch and ViaCEP are available
    And an administrator account is configured in the selected Bruno environment
    When the Bruno collection runs sequentially from setup through round 2
    Then clients complete requests that are approved and rejected by a covered analyst
    And search eventually exposes a fixture to the covered analyst
    And an uncovered analyst cannot expand coverage through a state filter
    And other clients cannot read, edit or submit the owner's requests
    And invalid input and workflow transitions return their documented HTTP errors
    And rejected mutations preserve the relevant workflow data
    And authentication errors follow the documented API error envelope

  Scenario: Retain evidence of a contract violation
    Given a response differs from its documented HTTP status or error envelope
    When Bruno evaluates the acceptance assertions
    Then the collection reports a failure without accepting an alternate status
    And application behavior is not changed by the test collection
