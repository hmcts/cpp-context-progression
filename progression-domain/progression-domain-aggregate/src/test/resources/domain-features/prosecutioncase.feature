Feature: CaseAggregate

  Scenario: Create a prosecution case whenever the prosecution refers a case to court

   Given no previous events
   When you createProsecutionCase on a CaseAggregate with a referred case details
   Then prosecution case created

  Scenario: Update prosecution case defendant whenever the prosecution defendant updated

    Given prosecution case created
    When you updateDefendantDetails on a CaseAggregate with a update defendant for prosecution case
    Then prosecution case defendant updated

  Scenario: Update prosecution case offences whenever the prosecution defendant offences updated

    Given prosecution case created
    When you updateOffences on a CaseAggregate with a update offences for prosecution case
    Then prosecution case offences updated

  Scenario: Update Defendant listing status updated whenever the hearing confirmed

    Given no previous events
    When you updateDefendantListingStatus on a HearingAggregate with a update defendant listing status
    Then prosecution case defendant listing status changed

  Scenario: Update Defendant hearing results updated whenever the hearing confirmed

    Given no previous events
    When you updateDefendantHearingResult on a HearingAggregate with a update defendant hearing result
    Then prosecution case defendant hearing result updated

  Scenario: Update Hearing Listing Status to Hearing Resulted whenever hearing is resulted already

    Given hearing is resulted
    When you updateDefendantListingStatus on a HearingAggregate with a update hearing listing status with status hearing initialised
    Then hearing listing status with status resulted updated

  Scenario: Update Hearing Listing Status to Hearing Initialised whenever hearing is not resulted already

    Given no previous events
    When you updateDefendantListingStatus on a HearingAggregate with a update hearing listing status with status hearing initialised
    Then hearing listing status with status initialised updated

  Scenario: Create Hearing Defendant Request

    Given no previous events
    When you createHearingDefendantRequest on a HearingAggregate with a create hearing defendant request
    Then hearing-defendant-request-created

  Scenario: Create Summons Data for a defendant

    Given hearing defendant request created
    When you createSummonsData on a HearingAggregate with a create summons data
    Then summons data created

  Scenario: Enrich Initiate Hearing whenever there is no defendant request

    Given no previous events
    When you enrichInitiateHearing on a HearingAggregate with a enrich initiate hearing
    Then initiate hearing enriched

  Scenario: Enrich Initiate Hearing whenever there is defendant request

    Given hearing defendant request created
    When you enrichInitiateHearing on a HearingAggregate with a enrich initiate hearing
    Then initiate hearing with defendant referral reason enriched

  Scenario: Match defendant for incoming prosecution case

    Given no previous events
    When you matchPartiallyMatchedDefendants on a CaseAggregate with a match defendant
    Then defendant matched

  Scenario: Defendant has been not matched

    Given no previous events
    When you matchPartiallyMatchedDefendants on a CaseAggregate with a match defendant
    Then defendant not already matched

  Scenario: Defendant already been matched

    Given defendant matched
    When you matchPartiallyMatchedDefendants on a CaseAggregate with a match defendant again
    Then defendant already matched

  Scenario: Assign Defendant Request from current hearing to extend hearing

    Given hearing defendant request created
    When you assignDefendantRequestFromCurrentHearingToExtendHearing on a HearingAggregate with a assign defendant request from current hearing to extend hearing
    Then defendant request from current hearing to extend hearing created

  Scenario: Assign Defendant Request to extend hearing

    Given no previous events
    When you assignDefendantRequestToExtendHearing on a HearingAggregate with a assign defendant request to extend hearing
    Then defendant request to extend hearing created