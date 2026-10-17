Feature: Developer experience

  Scenario: Running the system from a clean machine
    Given Docker is installed
    And the repository was freshly cloned
    When docker compose up --build is executed
    Then PostgreSQL should start
    And Elasticsearch should start
    And the application should become healthy
    And migrations should be applied automatically
    And the API documentation should be accessible
