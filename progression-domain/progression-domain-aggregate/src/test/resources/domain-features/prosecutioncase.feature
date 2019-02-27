Feature: CaseAggregate

  Scenario: Create a prosecution case whenever the prosecution refers a case to court

   Given no previous events
   When you createProsecutionCase on a CaseAggregate with a referred case details
   Then prosecution case created

  Scenario: Update prosecution case defendant whenever the prosecution defendant updated

    Given prosecution case created
    When you updateDefedantDetails on a CaseAggregate with a update defendant for prosecution case
    Then prosecution case defendant updated

  Scenario: Update prosecution case offences whenever the prosecution defendant offences updated

    Given prosecution case created
    When you updateOffences on a CaseAggregate with a update offences for prosecution case
    Then prosecution case offences updated

  Scenario: Update Defedent listing status updated whenever the hearing confirmed

    Given no previous events
    When you updateDefedantListingStatus on a HearingAggregate with a update defendant listing status
    Then prosecution case defendant listing status changed

  Scenario: Update Defedent hearing reulsts updated whenever the hearing confirmed

    Given no previous events
    When you updateDefedantHearingResult on a HearingAggregate with a update defendant hearing result
    Then prosecution case defendant hearing result updated