Feature: Solicitation search

  Scenario: Searching by status
    Given Elasticsearch contains submitted and approved solicitations
    When an analyst searches with status SUBMITTED
    Then only submitted solicitations should be returned

  Scenario: Filtering by priority and state
    Given multiple indexed solicitations
    When the analyst filters by HIGH priority and DF
    Then only matching solicitations should be returned

  Scenario: Paginating results
    Given 25 matching solicitations
    When page 1 with size 10 is requested
    Then 10 items should be returned
    And pagination metadata should indicate 25 total items

  Scenario: Analyst cannot bypass state coverage using search filters
    Given the analyst covers DF and GO
    And Elasticsearch contains solicitations from DF, GO and SP
    When the analyst searches with state SP
    Then no SP solicitation should be returned

  Scenario: Search filter is intersected with analyst coverage
    Given the analyst covers DF and GO
    When the analyst requests DF
    Then only DF results should be returned

  Scenario: Concurrent searches initialize the search index once
    Given the Elasticsearch search index does not exist
    When multiple searches start concurrently
    Then all searches should complete without an index creation conflict
