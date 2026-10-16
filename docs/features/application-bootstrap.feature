Feature: Application bootstrap

  Scenario: Starting the application with all dependencies
    Given PostgreSQL is available
    And Elasticsearch is available
    When the application starts
    Then the Spring context should load successfully
    And the health endpoint should report UP
