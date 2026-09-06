Feature: Authentication

  Scenario: STUDENT account logs in successfully
    Given a STUDENT account is logged in
    Then the response status is 200
    And the response contains a valid token

  Scenario: GENERAL account logs in successfully
    Given a GENERAL account is logged in
    Then the response status is 200
    And the response contains a valid token
