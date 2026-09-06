Feature: Student Sync — Vendor Visibility

  Scenario: STUDENT sees only campus vendors
    Given a STUDENT account is logged in
    When the student syncs
    Then the sync response includes a valid profile
    And the sync returns only campus vendors

  Scenario: GENERAL user sees only general vendors
    Given a GENERAL account is logged in
    When the student syncs
    Then the sync response includes a valid profile
    And the sync returns only general vendors
