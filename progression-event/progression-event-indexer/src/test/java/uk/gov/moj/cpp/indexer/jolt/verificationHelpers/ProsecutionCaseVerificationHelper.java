package uk.gov.moj.cpp.indexer.jolt.verificationHelpers;

import static com.jayway.jsonassert.JsonAssert.with;
import static java.lang.Boolean.parseBoolean;
import static java.util.stream.IntStream.range;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.hamcrest.MatcherAssert.assertThat;
import static uk.gov.moj.cpp.indexer.jolt.verificationHelpers.AddressVerificationHelper.addressLines;

import javax.json.JsonArray;
import javax.json.JsonNumber;
import javax.json.JsonObject;
import javax.json.JsonString;

import com.jayway.jsonpath.DocumentContext;

public class ProsecutionCaseVerificationHelper {

    public static void verifyProsecutionCase(final DocumentContext inputProsecutionCase, final JsonObject outputCase) {

        with(outputCase.toString())
                .assertThat("$.caseId", equalTo(((JsonString) inputProsecutionCase.read("$.prosecutionCase.id")).getString()))
                .assertThat("$.caseReference", equalTo(((JsonString) inputProsecutionCase.read("$.prosecutionCase.prosecutionCaseIdentifier.prosecutionAuthorityReference")).getString()))
                .assertThat("$.prosecutingAuthority", equalTo(((JsonString) inputProsecutionCase.read("$.prosecutionCase.prosecutionCaseIdentifier.prosecutionAuthorityCode")).getString()))
                .assertThat("$.caseStatus", equalTo("ACTIVE"))
                .assertThat("$._case_type", equalTo("PROSECUTION"));

        with(outputCase.toString()).assertNotDefined("$._is_crown");
        with(outputCase.toString()).assertNotDefined("$._is_magistrate");
    }

    public static void verifyDefendants(final DocumentContext inputProsecutionCase, final JsonObject outputCase, final int count, final boolean includeAliasAndOrganisation) {

        range(0, count)
                .forEach(index -> {

                    final String partiesIndexPath = String.format("$.parties[%d]", index);
                    final String defendantIndexPath = String.format("$.prosecutionCase.defendants[%d]", index);

                    with(outputCase.toString())
                            .assertThat("$.parties[*]", hasSize(count))
                            .assertThat(partiesIndexPath + ".partyId", equalTo(((JsonString) inputProsecutionCase.read(defendantIndexPath + ".id")).getString()))
                            .assertThat(partiesIndexPath + ".title", equalTo(((JsonString) inputProsecutionCase.read(defendantIndexPath + ".personDefendant.personDetails.title")).getString()))
                            .assertThat(partiesIndexPath + ".firstName", equalTo(((JsonString) inputProsecutionCase.read(defendantIndexPath + ".personDefendant.personDetails.firstName")).getString()))
                            .assertThat(partiesIndexPath + ".middleName", equalTo(((JsonString) inputProsecutionCase.read(defendantIndexPath + ".personDefendant.personDetails.middleName")).getString()))
                            .assertThat(partiesIndexPath + ".lastName", equalTo(((JsonString) inputProsecutionCase.read(defendantIndexPath + ".personDefendant.personDetails.lastName")).getString()))
                            .assertThat(partiesIndexPath + ".dateOfBirth", equalTo(((JsonString) inputProsecutionCase.read(defendantIndexPath + ".personDefendant.personDetails.dateOfBirth")).getString()))
                            .assertThat(partiesIndexPath + ".gender", equalTo(((JsonString) inputProsecutionCase.read(defendantIndexPath + ".personDefendant.personDetails.gender")).getString()))
                            .assertThat(partiesIndexPath + ".postCode", equalTo(((JsonString) inputProsecutionCase.read(defendantIndexPath + ".personDefendant.personDetails.address.postcode")).getString()))
                            .assertThat(partiesIndexPath + ".addressLines", equalTo(addressLines(inputProsecutionCase, defendantIndexPath + ".personDefendant.personDetails.address")))
                            .assertThat(partiesIndexPath + ".pncId", equalTo(((JsonString) inputProsecutionCase.read(defendantIndexPath + ".pncId")).getString()))
                            .assertThat(partiesIndexPath + ".arrestSummonsNumber", equalTo(((JsonString) inputProsecutionCase.read(defendantIndexPath + ".personDefendant.arrestSummonsNumber")).getString()))
                            .assertThat(partiesIndexPath + "._party_type", equalTo("DEFENDANT"))
                            .assertThat(partiesIndexPath + ".masterPartyId", equalTo(((JsonString) inputProsecutionCase.read(defendantIndexPath + ".masterDefendantId")).getString()))
                            .assertThat(partiesIndexPath + ".courtProceedingsInitiated", equalTo(((JsonString) inputProsecutionCase.read(defendantIndexPath + ".courtProceedingsInitiated")).getString()))

                            //verification for offence attributes introduced as part of LAA enhancement
                            .assertThat(partiesIndexPath + ".nationalInsuranceNumber", equalTo(((JsonString) inputProsecutionCase.read(defendantIndexPath + ".personDefendant.personDetails.nationalInsuranceNumber")).getString()))

                            .assertThat(partiesIndexPath + ".offences[0].offenceId", equalTo(((JsonString) inputProsecutionCase.read(defendantIndexPath + ".offences[0].id")).getString()))
                            .assertThat(partiesIndexPath + ".offences[0].offenceCode", equalTo(((JsonString) inputProsecutionCase.read(defendantIndexPath + ".offences[0].offenceCode")).getString()))
                            .assertThat(partiesIndexPath + ".offences[0].offenceTitle", equalTo(((JsonString) inputProsecutionCase.read(defendantIndexPath + ".offences[0].offenceTitle")).getString()))
                            .assertThat(partiesIndexPath + ".offences[0].offenceLegislation", equalTo(((JsonString) inputProsecutionCase.read(defendantIndexPath + ".offences[0].offenceLegislation")).getString()))
                            .assertThat(partiesIndexPath + ".offences[0].proceedingsConcluded", equalTo(parseBoolean((inputProsecutionCase.read(defendantIndexPath + ".offences[0].proceedingsConcluded")).toString())))
                            .assertThat(partiesIndexPath + ".offences[0].dateOfInformation", equalTo(((JsonString) inputProsecutionCase.read(defendantIndexPath + ".offences[0].dateOfInformation")).getString()))
                            .assertThat(partiesIndexPath + ".offences[0].startDate", equalTo(((JsonString) inputProsecutionCase.read(defendantIndexPath + ".offences[0].startDate")).getString()))
                            .assertThat(partiesIndexPath + ".offences[0].endDate", equalTo(((JsonString) inputProsecutionCase.read(defendantIndexPath + ".offences[0].endDate")).getString()))
                            .assertThat(partiesIndexPath + ".offences[0].arrestDate", equalTo(((JsonString) inputProsecutionCase.read(defendantIndexPath + ".offences[0].arrestDate")).getString()))
                            .assertThat(partiesIndexPath + ".offences[0].chargeDate", equalTo(((JsonString) inputProsecutionCase.read(defendantIndexPath + ".offences[0].chargeDate")).getString()))
                            .assertThat(partiesIndexPath + ".offences[0].modeOfTrial", equalTo(((JsonString) inputProsecutionCase.read(defendantIndexPath + ".offences[0].modeOfTrial")).getString()))
                            .assertThat(partiesIndexPath + ".offences[0].orderIndex", equalTo(((JsonNumber) inputProsecutionCase.read(defendantIndexPath + ".offences[0].orderIndex")).intValue()));

                    if (includeAliasAndOrganisation) {
                        with(outputCase.toString())
                                .assertThat(partiesIndexPath + ".organisationName", equalTo(((JsonString) inputProsecutionCase.read(defendantIndexPath + ".legalEntityDefendant.organisation.name")).getString()))
                                .assertThat(partiesIndexPath + ".aliases[0].firstName", equalTo(((JsonString) inputProsecutionCase.read(defendantIndexPath + ".aliases[0].firstName")).getString()))
                                .assertThat(partiesIndexPath + ".aliases[0].middleName", equalTo(((JsonString) inputProsecutionCase.read(defendantIndexPath + ".aliases[0].middleName")).getString()))
                                .assertThat(partiesIndexPath + ".aliases[0].lastName", equalTo(((JsonString) inputProsecutionCase.read(defendantIndexPath + ".aliases[0].lastName")).getString()))
                                .assertThat(partiesIndexPath + ".aliases[0].title", equalTo(((JsonString) inputProsecutionCase.read(defendantIndexPath + ".aliases[0].title")).getString()))
                                .assertThat(partiesIndexPath + ".aliases[0].organisationName", equalTo(((JsonString) inputProsecutionCase.read(defendantIndexPath + ".aliases[0].legalEntityName")).getString()));
                    }

                });
    }

    public static void verifyDefendantsForDefendantUpdated(final DocumentContext inputProsecutionCase, final JsonObject outputCase) {
        with(outputCase.toString())
                .assertThat("$.parties[*]", hasSize(1))
                .assertThat("$.parties[0].partyId", equalTo(((JsonString) inputProsecutionCase.read("$.defendant.id")).getString()))
                .assertThat("$.parties[0].title", equalTo(((JsonString) inputProsecutionCase.read("$.defendant.personDefendant.personDetails.title")).getString()))
                .assertThat("$.parties[0].firstName", equalTo(((JsonString) inputProsecutionCase.read("$.defendant.personDefendant.personDetails.firstName")).getString()))
                .assertThat("$.parties[0].middleName", equalTo(((JsonString) inputProsecutionCase.read("$.defendant.personDefendant.personDetails.middleName")).getString()))
                .assertThat("$.parties[0].lastName", equalTo(((JsonString) inputProsecutionCase.read("$.defendant.personDefendant.personDetails.lastName")).getString()))
                .assertThat("$.parties[0].dateOfBirth", equalTo(((JsonString) inputProsecutionCase.read("$.defendant.personDefendant.personDetails.dateOfBirth")).getString()))
                .assertThat("$.parties[0].gender", equalTo(((JsonString) inputProsecutionCase.read("$.defendant.personDefendant.personDetails.gender")).getString()))
                .assertThat("$.parties[0].postCode", equalTo(((JsonString) inputProsecutionCase.read("$.defendant.personDefendant.personDetails.address.postcode")).getString()))
                .assertThat("$.parties[0].addressLines", equalTo(addressLines(inputProsecutionCase, "$.defendant.personDefendant.personDetails.address")))
                .assertThat("$.parties[0].pncId", equalTo(((JsonString) inputProsecutionCase.read("$.defendant.pncId")).getString()))
                .assertThat("$.parties[0].masterPartyId", equalTo(((JsonString) inputProsecutionCase.read("$.defendant.masterDefendantId")).getString()))
                //.assertThat("$.parties[0].courtProceedingsInitiated", equalTo(((JsonString) inputProsecutionCase.read("$.defendant.courtProceedingsInitiated")).getString()))
                .assertThat("$.parties[0].croNumber", equalTo(((JsonString) inputProsecutionCase.read("$.defendant.croNumber")).getString()))

                .assertThat("$.parties[0].arrestSummonsNumber", equalTo(((JsonString) inputProsecutionCase.read("$.defendant.personDefendant.arrestSummonsNumber")).getString()))
                .assertThat("$.parties[0].organisationName", equalTo(((JsonString) inputProsecutionCase.read("$.defendant.legalEntityDefendant.organisation.name")).getString()))
                .assertThat("$.parties[0]._party_type", equalTo("DEFENDANT"))
                .assertThat("$.parties[0].aliases[0].firstName", equalTo(((JsonString) inputProsecutionCase.read("$.defendant.aliases[0].firstName")).getString()))
                .assertThat("$.parties[0].aliases[0].middleName", equalTo(((JsonString) inputProsecutionCase.read("$.defendant.aliases[0].middleName")).getString()))
                .assertThat("$.parties[0].aliases[0].lastName", equalTo(((JsonString) inputProsecutionCase.read("$.defendant.aliases[0].lastName")).getString()))
                .assertThat("$.parties[0].aliases[0].title", equalTo(((JsonString) inputProsecutionCase.read("$.defendant.aliases[0].title")).getString()))
                .assertThat("$.parties[0].aliases[0].organisationName", equalTo(((JsonString) inputProsecutionCase.read("$.defendant.aliases[0].legalEntityName")).getString()))

                //verification for defendant attributes introduced as part of LAA enhancement
                .assertThat("$.parties[0].nationalInsuranceNumber", equalTo(((JsonString) inputProsecutionCase.read("$.defendant.personDefendant.personDetails.nationalInsuranceNumber")).getString()))
                .assertThat("$.parties[0].proceedingsConcluded", equalTo(parseBoolean((inputProsecutionCase.read("$.defendant.proceedingsConcluded")).toString())))

                //Offence 1
                .assertThat("$.parties[0].offences[0].offenceId", equalTo(((JsonString) inputProsecutionCase.read("$.defendant.offences[0].id")).getString()))
                .assertThat("$.parties[0].offences[0].offenceCode", equalTo(((JsonString) inputProsecutionCase.read("$.defendant.offences[0].offenceCode")).getString()))
                .assertThat("$.parties[0].offences[0].offenceTitle", equalTo(((JsonString) inputProsecutionCase.read("$.defendant.offences[0].offenceTitle")).getString()))
                .assertThat("$.parties[0].offences[0].offenceLegislation", equalTo(((JsonString) inputProsecutionCase.read("$.defendant.offences[0].offenceLegislation")).getString()))
                .assertThat("$.parties[0].offences[0].proceedingsConcluded", equalTo(parseBoolean((inputProsecutionCase.read("$.defendant.offences[0].proceedingsConcluded")).toString())))
                .assertThat("$.parties[0].offences[0].dateOfInformation", equalTo(((JsonString) inputProsecutionCase.read("$.defendant.offences[0].dateOfInformation")).getString()))
                .assertThat("$.parties[0].offences[0].startDate", equalTo(((JsonString) inputProsecutionCase.read("$.defendant.offences[0].startDate")).getString()))
                .assertThat("$.parties[0].offences[0].endDate", equalTo(((JsonString) inputProsecutionCase.read("$.defendant.offences[0].endDate")).getString()))
                .assertThat("$.parties[0].offences[0].arrestDate", equalTo(((JsonString) inputProsecutionCase.read("$.defendant.offences[0].arrestDate")).getString()))
                .assertThat("$.parties[0].offences[0].chargeDate", equalTo(((JsonString) inputProsecutionCase.read("$.defendant.offences[0].chargeDate")).getString()))
                .assertThat("$.parties[0].offences[0].modeOfTrial", equalTo(((JsonString) inputProsecutionCase.read("$.defendant.offences[0].modeOfTrial")).getString()))
                .assertThat("$.parties[0].offences[0].orderIndex", equalTo(((JsonNumber) inputProsecutionCase.read("$.defendant.offences[0].orderIndex")).intValue()))

                //LAAReference
                .assertThat("$.parties[0].offences[0].laaReference.applicationReference", equalTo(((JsonString) inputProsecutionCase.read("$.defendant.offences[0].laaApplnReference.applicationReference")).getString()))
                .assertThat("$.parties[0].offences[0].laaReference.statusId", equalTo(((JsonString) inputProsecutionCase.read("$.defendant.offences[0].laaApplnReference.statusId")).getString()))
                .assertThat("$.parties[0].offences[0].laaReference.statusCode", equalTo(((JsonString) inputProsecutionCase.read("$.defendant.offences[0].laaApplnReference.statusCode")).getString()))
                .assertThat("$.parties[0].offences[0].laaReference.statusDescription", equalTo(((JsonString) inputProsecutionCase.read("$.defendant.offences[0].laaApplnReference.statusDescription")).getString()))

                //RepresentationOrder
                .assertThat("$.parties[0].representationOrder.applicationReference", equalTo(((JsonString) inputProsecutionCase.read("$.defendant.associatedDefenceOrganisation.applicationReference")).getString()))
                .assertThat("$.parties[0].representationOrder.effectiveFromDate", equalTo(((JsonString) inputProsecutionCase.read("$.defendant.associatedDefenceOrganisation.associationStartDate")).getString()))
                .assertThat("$.parties[0].representationOrder.effectiveToDate", equalTo(((JsonString) inputProsecutionCase.read("$.defendant.associatedDefenceOrganisation.associationEndDate")).getString()))
                .assertThat("$.parties[0].representationOrder.laaContractNumber", equalTo(((JsonString) inputProsecutionCase.read("$.defendant.associatedDefenceOrganisation.defenceOrganisation.laaContractNumber")).getString()))

                //Offence 2
                .assertThat("$.parties[0].offences[1].offenceId", equalTo(((JsonString) inputProsecutionCase.read("$.defendant.offences[1].id")).getString()))
                .assertThat("$.parties[0].offences[1].offenceCode", equalTo(((JsonString) inputProsecutionCase.read("$.defendant.offences[1].offenceCode")).getString()))
                .assertThat("$.parties[0].offences[1].offenceTitle", equalTo(((JsonString) inputProsecutionCase.read("$.defendant.offences[1].offenceTitle")).getString()))
                .assertThat("$.parties[0].offences[1].offenceLegislation", equalTo(((JsonString) inputProsecutionCase.read("$.defendant.offences[1].offenceLegislation")).getString()))
                .assertThat("$.parties[0].offences[1].proceedingsConcluded", equalTo(parseBoolean((inputProsecutionCase.read("$.defendant.offences[1].proceedingsConcluded")).toString())))
                .assertThat("$.parties[0].offences[1].dateOfInformation", equalTo(((JsonString) inputProsecutionCase.read("$.defendant.offences[1].dateOfInformation")).getString()))
                .assertThat("$.parties[0].offences[1].startDate", equalTo(((JsonString) inputProsecutionCase.read("$.defendant.offences[1].startDate")).getString()))
                .assertThat("$.parties[0].offences[1].endDate", equalTo(((JsonString) inputProsecutionCase.read("$.defendant.offences[1].endDate")).getString()))
                .assertThat("$.parties[0].offences[1].arrestDate", equalTo(((JsonString) inputProsecutionCase.read("$.defendant.offences[1].arrestDate")).getString()))
                .assertThat("$.parties[0].offences[1].chargeDate", equalTo(((JsonString) inputProsecutionCase.read("$.defendant.offences[1].chargeDate")).getString()))
                .assertThat("$.parties[0].offences[1].modeOfTrial", equalTo(((JsonString) inputProsecutionCase.read("$.defendant.offences[1].modeOfTrial")).getString()))
                .assertThat("$.parties[0].offences[1].orderIndex", equalTo(((JsonNumber) inputProsecutionCase.read("$.defendant.offences[1].orderIndex")).intValue()))

                //LAAReference
                .assertThat("$.parties[0].offences[1].laaReference.applicationReference", equalTo(((JsonString) inputProsecutionCase.read("$.defendant.offences[1].laaApplnReference.applicationReference")).getString()))
                .assertThat("$.parties[0].offences[1].laaReference.statusId", equalTo(((JsonString) inputProsecutionCase.read("$.defendant.offences[1].laaApplnReference.statusId")).getString()))
                .assertThat("$.parties[0].offences[1].laaReference.statusCode", equalTo(((JsonString) inputProsecutionCase.read("$.defendant.offences[1].laaApplnReference.statusCode")).getString()))
                .assertThat("$.parties[0].offences[1].laaReference.statusDescription", equalTo(((JsonString) inputProsecutionCase.read("$.defendant.offences[1].laaApplnReference.statusDescription")).getString()))
                //Offence 1
                .assertThat("$.parties[0].offences[0].offenceId", equalTo(((JsonString) inputProsecutionCase.read("$.defendant.offences[0].id")).getString()))
                .assertThat("$.parties[0].offences[0].offenceCode", equalTo(((JsonString) inputProsecutionCase.read("$.defendant.offences[0].offenceCode")).getString()))
                .assertThat("$.parties[0].offences[0].offenceTitle", equalTo(((JsonString) inputProsecutionCase.read("$.defendant.offences[0].offenceTitle")).getString()))
                .assertThat("$.parties[0].offences[0].offenceLegislation", equalTo(((JsonString) inputProsecutionCase.read("$.defendant.offences[0].offenceLegislation")).getString()))
                .assertThat("$.parties[0].offences[0].proceedingsConcluded", equalTo(parseBoolean((inputProsecutionCase.read("$.defendant.offences[0].proceedingsConcluded")).toString())))
                .assertThat("$.parties[0].offences[0].dateOfInformation", equalTo(((JsonString) inputProsecutionCase.read("$.defendant.offences[0].dateOfInformation")).getString()))
                .assertThat("$.parties[0].offences[0].startDate", equalTo(((JsonString) inputProsecutionCase.read("$.defendant.offences[0].startDate")).getString()))
                .assertThat("$.parties[0].offences[0].endDate", equalTo(((JsonString) inputProsecutionCase.read("$.defendant.offences[0].endDate")).getString()))
                .assertThat("$.parties[0].offences[0].arrestDate", equalTo(((JsonString) inputProsecutionCase.read("$.defendant.offences[0].arrestDate")).getString()))
                .assertThat("$.parties[0].offences[0].chargeDate", equalTo(((JsonString) inputProsecutionCase.read("$.defendant.offences[0].chargeDate")).getString()))
                .assertThat("$.parties[0].offences[0].modeOfTrial", equalTo(((JsonString) inputProsecutionCase.read("$.defendant.offences[0].modeOfTrial")).getString()))
                .assertThat("$.parties[0].offences[0].orderIndex", equalTo(((JsonNumber) inputProsecutionCase.read("$.defendant.offences[0].orderIndex")).intValue()))

                //LAAReference
                .assertThat("$.parties[0].offences[0].laaReference.applicationReference", equalTo(((JsonString) inputProsecutionCase.read("$.defendant.offences[0].laaApplnReference.applicationReference")).getString()))
                .assertThat("$.parties[0].offences[0].laaReference.statusId", equalTo(((JsonString) inputProsecutionCase.read("$.defendant.offences[0].laaApplnReference.statusId")).getString()))
                .assertThat("$.parties[0].offences[0].laaReference.statusCode", equalTo(((JsonString) inputProsecutionCase.read("$.defendant.offences[0].laaApplnReference.statusCode")).getString()))
                .assertThat("$.parties[0].offences[0].laaReference.statusDescription", equalTo(((JsonString) inputProsecutionCase.read("$.defendant.offences[0].laaApplnReference.statusDescription")).getString()))

                //RepresentationOrder
                .assertThat("$.parties[0].representationOrder.applicationReference", equalTo(((JsonString) inputProsecutionCase.read("$.defendant.associatedDefenceOrganisation.applicationReference")).getString()))
                .assertThat("$.parties[0].representationOrder.effectiveFromDate", equalTo(((JsonString) inputProsecutionCase.read("$.defendant.associatedDefenceOrganisation.associationStartDate")).getString()))
                .assertThat("$.parties[0].representationOrder.effectiveToDate", equalTo(((JsonString) inputProsecutionCase.read("$.defendant.associatedDefenceOrganisation.associationEndDate")).getString()))
                .assertThat("$.parties[0].representationOrder.laaContractNumber", equalTo(((JsonString) inputProsecutionCase.read("$.defendant.associatedDefenceOrganisation.defenceOrganisation.laaContractNumber")).getString()))

                //Offence 2
                .assertThat("$.parties[0].offences[1].offenceId", equalTo(((JsonString) inputProsecutionCase.read("$.defendant.offences[1].id")).getString()))
                .assertThat("$.parties[0].offences[1].offenceCode", equalTo(((JsonString) inputProsecutionCase.read("$.defendant.offences[1].offenceCode")).getString()))
                .assertThat("$.parties[0].offences[1].offenceTitle", equalTo(((JsonString) inputProsecutionCase.read("$.defendant.offences[1].offenceTitle")).getString()))
                .assertThat("$.parties[0].offences[1].offenceLegislation", equalTo(((JsonString) inputProsecutionCase.read("$.defendant.offences[1].offenceLegislation")).getString()))
                .assertThat("$.parties[0].offences[1].proceedingsConcluded", equalTo(parseBoolean((inputProsecutionCase.read("$.defendant.offences[1].proceedingsConcluded")).toString())))
                .assertThat("$.parties[0].offences[1].dateOfInformation", equalTo(((JsonString) inputProsecutionCase.read("$.defendant.offences[1].dateOfInformation")).getString()))
                .assertThat("$.parties[0].offences[1].startDate", equalTo(((JsonString) inputProsecutionCase.read("$.defendant.offences[1].startDate")).getString()))
                .assertThat("$.parties[0].offences[1].endDate", equalTo(((JsonString) inputProsecutionCase.read("$.defendant.offences[1].endDate")).getString()))
                .assertThat("$.parties[0].offences[1].arrestDate", equalTo(((JsonString) inputProsecutionCase.read("$.defendant.offences[1].arrestDate")).getString()))
                .assertThat("$.parties[0].offences[1].chargeDate", equalTo(((JsonString) inputProsecutionCase.read("$.defendant.offences[1].chargeDate")).getString()))
                .assertThat("$.parties[0].offences[1].modeOfTrial", equalTo(((JsonString) inputProsecutionCase.read("$.defendant.offences[1].modeOfTrial")).getString()))
                .assertThat("$.parties[0].offences[1].orderIndex", equalTo(((JsonNumber) inputProsecutionCase.read("$.defendant.offences[1].orderIndex")).intValue()))

                //LAAReference
                .assertThat("$.parties[0].offences[1].laaReference.applicationReference", equalTo(((JsonString) inputProsecutionCase.read("$.defendant.offences[1].laaApplnReference.applicationReference")).getString()))
                .assertThat("$.parties[0].offences[1].laaReference.statusId", equalTo(((JsonString) inputProsecutionCase.read("$.defendant.offences[1].laaApplnReference.statusId")).getString()))
                .assertThat("$.parties[0].offences[1].laaReference.statusCode", equalTo(((JsonString) inputProsecutionCase.read("$.defendant.offences[1].laaApplnReference.statusCode")).getString()))
                .assertThat("$.parties[0].offences[1].laaReference.statusDescription", equalTo(((JsonString) inputProsecutionCase.read("$.defendant.offences[1].laaApplnReference.statusDescription")).getString()));
    }

    public static void verifyAdditionalAliasesOnOnDefendantUpdate(final JsonObject outputCase) {
        with(outputCase.toString())
                .assertThat("$.parties[0].aliases[0].firstName", equalTo("Jim"))
                .assertThat("$.parties[0].aliases[0].middleName", equalTo("John"))
                .assertThat("$.parties[0].aliases[0].lastName", equalTo("Jefferies"))
                .assertThat("$.parties[0].aliases[0].title", equalTo("MR"))
                .assertThat("$.parties[0].aliases[0].organisationName", equalTo("Chicken & Curry Best In Town LTD"));
    }

    public static void verifyCaseCreated(final DocumentContext inputProsecutionCase, final JsonObject outputCase, final int defendantCount, final boolean includeAliasAndOrganisation) {
        final JsonArray parties = outputCase.getJsonArray("parties");
        assertNotNull(parties);
        assertThat(parties.size(), is(defendantCount));
        verifyProsecutionCase(inputProsecutionCase, outputCase);
        verifyDefendants(inputProsecutionCase, outputCase, defendantCount, includeAliasAndOrganisation);
    }

    public static void verifyCaseDefendantUpdated(final DocumentContext inputProsecutionCase, final JsonObject outputCase) {

        final JsonArray parties = outputCase.getJsonArray("parties");
        assertNotNull(parties);
        assertThat(parties.size(), is(1));
        verifyDefendantsForDefendantUpdated(inputProsecutionCase, outputCase);
        verifyAdditionalAliasesOnOnDefendantUpdate(outputCase);
    }
}
