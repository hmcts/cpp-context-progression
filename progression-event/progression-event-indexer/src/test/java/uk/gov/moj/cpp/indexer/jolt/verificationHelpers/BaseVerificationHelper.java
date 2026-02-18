package uk.gov.moj.cpp.indexer.jolt.verificationHelpers;

import static com.jayway.jsonassert.JsonAssert.with;
import static java.lang.Boolean.valueOf;
import static java.lang.String.format;
import static java.util.logging.Level.WARNING;
import static java.util.stream.IntStream.range;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.hamcrest.core.IsNot.not;
import static uk.gov.moj.cpp.indexer.jolt.verificationHelpers.AddressVerificationHelper.addressLines;

import java.util.logging.Logger;

import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.JsonString;

import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.PathNotFoundException;

public class BaseVerificationHelper extends BaseVerificationCountHelper {
    private static final Logger logger = Logger.getLogger(BaseVerificationHelper.class.getName());

    private static final String OUTPUT_PARTIES_JSON_PATH_FOR_SIZE = "$.caseDocuments[%d].parties[*]";
    private static final String INPUT_OFFENCES_JSON_PATH_FOR_SIZE = "$.defendants[%d].offences";

    private static final String INPUT_DEFENDANTS_JSON_PATH = "$.defendants[%d]";
    private static final String INPUT_OFFENCES_JSON_PATH = "$.defendants[%d].offences[%d]";
    private static final String INPUT_ALIASES_JSON_PATH = "$.defendants[%d].aliases[%d]";

    private static final String OUTPUT_CASE_JSON_PATH = "$.caseDocuments[%d]";
    private static final String OUTPUT_ALIASES_JSON_PATH = "$.caseDocuments[%d].parties[%d].aliases[%d]";
    private static final String OUTPUT_PARTIES_JSON_PATH = "$.caseDocuments[%d].parties[%d]";
    private static final String OUTPUT_OFFENCES_JSON_PATH = "$.caseDocuments[%d].parties[%d].offences[%d]";


    public void verifyProsecutionCase(final DocumentContext inputProsecutionCase,
                                      final JsonObject outputCase,
                                      final int caseIndex,
                                      final String inputDefendantPath) {
        try {
            final String outputCaseDocumentsPath = format(OUTPUT_CASE_JSON_PATH, caseIndex);

            String caseUrn = null;
            final JsonObject prosecutionCaseIdentifier = inputProsecutionCase.read(inputDefendantPath + ".prosecutionCaseIdentifier");
            if (prosecutionCaseIdentifier.get("caseURN") == null) {
                caseUrn = prosecutionCaseIdentifier.getString("prosecutionAuthorityReference");
            } else {
                caseUrn = prosecutionCaseIdentifier.getString("caseURN");
            }
            final String prosecutingAuthority = ((JsonString) inputProsecutionCase.read(inputDefendantPath + ".prosecutionCaseIdentifier.prosecutionAuthorityCode")).getString();
            with(outputCase.toString())
                    .assertThat(outputCaseDocumentsPath + ".caseId", equalTo(((JsonString) inputProsecutionCase.read(inputDefendantPath + ".id")).getString()))
                    .assertNotDefined(outputCaseDocumentsPath + ".caseStatus")
                    .assertThat(outputCaseDocumentsPath + "._case_type", equalTo("PROSECUTION"))
                    .assertThat(outputCaseDocumentsPath + ".caseReference", equalTo(caseUrn))
                    .assertThat(outputCaseDocumentsPath + ".prosecutingAuthority", equalTo(prosecutingAuthority));

            if(caseIndex == 0){
                with(outputCase.toString())
                        .assertThat(outputCaseDocumentsPath + ".parties[0].offences[0].verdict.verdictType.verdictTypeId", equalTo(((JsonString) inputProsecutionCase.read(inputDefendantPath + ".defendants[0].offences[0].verdict.verdictType.id")).getString()));
            }else{
                with(outputCase.toString())
                        .assertNotDefined(outputCaseDocumentsPath + ".defendants[0].offences[0].verdict.verdictType.verdictTypeId");
            }

            incrementCaseDocumentsCount();
        } catch (final Exception e) {
            incrementExceptionCount();
            logger.log(WARNING, format("Exception validating ProsecutionCase ", e.getMessage()));
        }
    }

    public void verifyApplication(final DocumentContext inputCourtApplication,
                                  final JsonObject outputCase,
                                  final int caseIndex,
                                  final String inputApplicationPath) {
        verifyApplication(inputCourtApplication, outputCase, caseIndex, inputApplicationPath, 0);
    }

    public void verifyApplication(final DocumentContext inputCourtApplication,
                                  final JsonObject outputCase, final int caseIndex, final String inputApplicationPath, final int inputApplicationIndex) {
        final String outputCaseDocumentsPath = format(OUTPUT_CASE_JSON_PATH, caseIndex);

        final String applicationId = ((JsonString) inputCourtApplication.read(inputApplicationPath + "id")).getString();
        final String applicationReference = ((JsonString) inputCourtApplication.read(inputApplicationPath + ".applicationReference")).getString();
        final String applicationType = ((JsonString) inputCourtApplication.read(inputApplicationPath + ".type.type")).getString();
        final String applicationReceivedDate = ((JsonString) inputCourtApplication.read(inputApplicationPath + ".applicationReceivedDate")).getString();
        final String applicationDecisionSoughtByDate = ((JsonString) inputCourtApplication.read(inputApplicationPath + ".applicationDecisionSoughtByDate")).getString();
        final boolean isLinkedApplication = inputCourtApplication.read(inputApplicationPath).toString().contains("courtApplicationCases");
        final boolean isBreachApplication = !inputCourtApplication.read(inputApplicationPath).toString().contains("courtApplicationCases") && inputCourtApplication.read(inputApplicationPath).toString().contains("courtOrder");

        try {
            if(isLinkedApplication){
                with(outputCase.toString()).assertThat(outputCaseDocumentsPath + ".caseId", not(equalTo(((JsonString) inputCourtApplication.read(inputApplicationPath + ".id")).getString())))
                        .assertNotDefined(outputCaseDocumentsPath + ".caseStatus")
                        .assertThat(outputCaseDocumentsPath + "._case_type", equalTo("PROSECUTION"));
            } else if(isBreachApplication){
                with(outputCase.toString()).assertThat(outputCaseDocumentsPath + ".caseId", not(equalTo(((JsonString) inputCourtApplication.read(inputApplicationPath + "courtOrder.courtOrderOffences[0].prosecutionCaseId")))))
                        .assertNotDefined(outputCaseDocumentsPath + ".caseStatus")
                        .assertThat(outputCaseDocumentsPath + "._case_type", equalTo("PROSECUTION"));
            } else {
                with(outputCase.toString()).assertThat(outputCaseDocumentsPath + ".caseId", equalTo(((JsonString) inputCourtApplication.read(inputApplicationPath + ".id")).getString()))
                        .assertNotDefined(outputCaseDocumentsPath + ".caseStatus")
                        .assertThat(outputCaseDocumentsPath + "._case_type", equalTo("APPLICATION"));
            }
            with(outputCase.toString())
                    .assertThat(outputCaseDocumentsPath + ".applications["+ inputApplicationIndex +"].applicationId", is(applicationId))
                    .assertThat(outputCaseDocumentsPath + ".applications["+ inputApplicationIndex +"].applicationReference", is(applicationReference))
                    .assertThat(outputCaseDocumentsPath + ".applications["+ inputApplicationIndex +"].applicationType", is(applicationType))
                    .assertThat(outputCaseDocumentsPath + ".applications["+ inputApplicationIndex +"].receivedDate", is(applicationReceivedDate))
                    .assertThat(outputCaseDocumentsPath + ".applications["+ inputApplicationIndex +"].decisionDate", is(applicationDecisionSoughtByDate));

            if(!isLinkedApplication) {
                incrementCaseDocumentsCount();
            }

        } catch (final Exception e) {
            incrementExceptionCount();
            logger.log(WARNING, format("Exception validating ProsecutionCase ", e.getMessage()));
        }
    }

    public void verifyProsecutionCase(final DocumentContext inputProsecutionCase,
                                      final JsonObject outputCase,
                                      final int caseIndex,
                                      final int defendantIndex) {
        try {
            final String outputCaseDocumentsPath = format(OUTPUT_CASE_JSON_PATH, caseIndex);
            final String inputDefendantPath = format(INPUT_DEFENDANTS_JSON_PATH, defendantIndex);
            with(outputCase.toString())
                    .assertThat(outputCaseDocumentsPath + ".caseId", equalTo(((JsonString) inputProsecutionCase.read(inputDefendantPath + ".prosecutionCaseId")).getString()))
                    .assertThat(outputCaseDocumentsPath + ".caseStatus", equalTo("ACTIVE"))
                    .assertThat(outputCaseDocumentsPath + "._case_type", equalTo("PROSECUTION"));
            incrementCaseDocumentsCount();
        } catch (final Exception e) {
            incrementExceptionCount();
            logger.log(WARNING, format("Exception validating ProsecutionCase ", e.getMessage()));
        }
    }

    public void verifyCaseAndPartyCount(final JsonObject outputCase,
                                        final int caseIndex,
                                        final int partyCount) {

        try {
            final String outputPartiesIndexPathCreated = format(OUTPUT_PARTIES_JSON_PATH_FOR_SIZE, caseIndex);
            with(outputCase.toString()).assertThat(outputPartiesIndexPathCreated, hasSize(partyCount));
        } catch (final Exception e) {
            incrementExceptionCount();
            logger.log(WARNING, format("Exception validating Defendants for Multi Cases", e.getMessage()));
        }
    }

    public void verifyDefendant(final DocumentContext inputProsecutionCase,
                                final JsonObject outputCase,
                                final int caseIndex,
                                final int outputPartyIndex,
                                final int inputPartyIndex) {
        final String outputPartiesIndexPathCreated = format(OUTPUT_PARTIES_JSON_PATH, caseIndex, outputPartyIndex);
        final String inputDefendantIndexPathCreated = format(INPUT_DEFENDANTS_JSON_PATH, inputPartyIndex);

        validateLegalEntity(inputProsecutionCase, outputCase, outputPartiesIndexPathCreated, inputDefendantIndexPathCreated);
        validatePersonDefendant(inputProsecutionCase, outputCase, outputPartiesIndexPathCreated, inputDefendantIndexPathCreated);
        validateOffences(inputProsecutionCase, outputCase, caseIndex, inputPartyIndex, outputPartyIndex);
        incrementPartiesCount();
    }

    public void validateAliases(final DocumentContext inputParties,
                                final JsonObject outputParties,
                                final int caseIndex,
                                final int inputPartyIndex,
                                final int outputPartyIndex,
                                final int aliasIndex) {
        try {
            final String aliasPath = format(OUTPUT_ALIASES_JSON_PATH, caseIndex, outputPartyIndex, aliasIndex);
            final String inputAliasPath = format(INPUT_ALIASES_JSON_PATH, inputPartyIndex, aliasIndex);

            with(outputParties.toString())
                    .assertThat(aliasPath + ".firstName", equalTo(((JsonString) inputParties.read(inputAliasPath + "firstName")).getString()))
                    .assertThat(aliasPath + ".middleName", equalTo(((JsonString) inputParties.read(inputAliasPath + ".middleName")).getString()))
                    .assertThat(aliasPath + ".lastName", equalTo(((JsonString) inputParties.read(inputAliasPath + ".lastName")).getString()))
                    .assertThat(aliasPath + ".title", equalTo(((JsonString) inputParties.read(inputAliasPath + ".title")).getString()))
                    .assertThat(aliasPath + ".organisationName", equalTo(((JsonString) inputParties.read(inputAliasPath + ".legalEntityName")).getString()));
            incrementAliasesCount();
        } catch (final PathNotFoundException e) {
            incrementExceptionCount();
            logger.log(WARNING, format("Exception validating aliases", e.getMessage()));
        }

    }
    protected void validateOffences(final DocumentContext inputParties,
                                    final JsonObject outputParties,
                                    final int caseIndex,
                                    final int inputPartyIndex,
                                    final int outputPartyIndex) {

        try {
            final JsonArray offenceCourt = inputParties.read(format(INPUT_OFFENCES_JSON_PATH_FOR_SIZE, inputPartyIndex));
            final int offenceSize = offenceCourt.size();
            range(0, offenceSize)
                    .forEach(offenceIndex -> {
                        final String offenceInputIndexPath = format(INPUT_OFFENCES_JSON_PATH, inputPartyIndex, offenceIndex);
                        final String offenceOutputIndexPath = format(OUTPUT_OFFENCES_JSON_PATH, caseIndex, outputPartyIndex, offenceIndex);
                        validateOffence(inputParties, outputParties, offenceInputIndexPath, offenceOutputIndexPath);
                    });
        } catch (final Exception e) {
            incrementExceptionCount();
            logger.log(WARNING, format("Exception validating Offences", e.getMessage()));
        }
    }

    private void validateOffence(final DocumentContext inputParties,
                                 final JsonObject outputParties,
                                 final String offenceInputIndexPath,
                                 final String offenceOutputIndexPath) {
        try {
            with(outputParties.toString())
                    .assertThat(offenceOutputIndexPath + ".offenceId", equalTo(((JsonString) inputParties.read(offenceInputIndexPath + ".id")).getString()))
                    .assertThat(offenceOutputIndexPath + ".offenceCode", equalTo(((JsonString) inputParties.read(offenceInputIndexPath + ".offenceCode")).getString()))
                    .assertThat(offenceOutputIndexPath + ".endDate", equalTo(((JsonString) inputParties.read(offenceInputIndexPath + ".endDate")).getString()))
                    .assertThat(offenceOutputIndexPath + ".arrestDate", equalTo(((JsonString) inputParties.read(offenceInputIndexPath + ".arrestDate")).getString()))
                    .assertThat(offenceOutputIndexPath + ".chargeDate", equalTo(((JsonString) inputParties.read(offenceInputIndexPath + ".chargeDate")).getString()))
                    .assertThat(offenceOutputIndexPath + ".offenceTitle", equalTo(((JsonString) inputParties.read(offenceInputIndexPath + ".offenceTitle")).getString()))
                    .assertThat(offenceOutputIndexPath + ".startDate", equalTo(((JsonString) inputParties.read(offenceInputIndexPath + ".startDate")).getString()))
                    .assertThat(offenceOutputIndexPath + ".offenceLegislation", equalTo(((JsonString) inputParties.read(offenceInputIndexPath + ".offenceLegislation")).getString()))
                    .assertThat(offenceOutputIndexPath + ".proceedingsConcluded", equalTo(valueOf(inputParties.read(offenceInputIndexPath + ".proceedingsConcluded").toString()).booleanValue()))
                    .assertThat(offenceOutputIndexPath + ".dateOfInformation", equalTo(((JsonString) inputParties.read(offenceInputIndexPath + ".dateOfInformation")).getString()))
                    .assertThat(offenceOutputIndexPath + ".modeOfTrial", equalTo(((JsonString) inputParties.read(offenceInputIndexPath + ".modeOfTrial")).getString()))
                    .assertThat(offenceOutputIndexPath + ".orderIndex", equalTo(Integer.valueOf(inputParties.read(offenceInputIndexPath + ".orderIndex").toString())))
                    .assertThat(offenceOutputIndexPath + ".laaReference.applicationReference", equalTo(((JsonString) inputParties.read(offenceInputIndexPath + ".laaApplnReference.applicationReference")).getString()))
                    .assertThat(offenceOutputIndexPath + ".laaReference.statusId", equalTo(((JsonString) inputParties.read(offenceInputIndexPath + ".laaApplnReference.statusId")).getString()))
                    .assertThat(offenceOutputIndexPath + ".laaReference.statusCode", equalTo(((JsonString) inputParties.read(offenceInputIndexPath + ".laaApplnReference.statusCode")).getString()))
                    .assertThat(offenceOutputIndexPath + ".laaReference.statusDescription", equalTo(((JsonString) inputParties.read(offenceInputIndexPath + ".laaApplnReference.statusDescription")).getString()));
            incrementOffencesCount();
        } catch (Exception e) {
            incrementExceptionCount();
            logger.log(WARNING, format("Exception validating Offence", e.getMessage()));
        }
    }

    protected void validatePersonDefendant(final DocumentContext inputParties,
                                           final JsonObject outputParties,
                                           final String partiesIndexPath,
                                           final String defendantIndexPath) {
        try {

            //final String formattedCourtProceedingsInitiated = courtProceedingsInitiated.substring(0,courtProceedingsInitiated.length()-1)+"Z[UTC]";
            with(outputParties.toString())
                    .assertThat(partiesIndexPath + ".title", equalTo(((JsonString) inputParties.read(defendantIndexPath + ".personDefendant.personDetails.title")).getString()))
                    .assertThat(partiesIndexPath + "._party_type", equalTo("DEFENDANT"))
                    .assertThat(partiesIndexPath + ".partyId", equalTo(((JsonString) inputParties.read(defendantIndexPath + ".id")).getString()))
                    .assertThat(partiesIndexPath + ".masterPartyId", equalTo(((JsonString) inputParties.read(defendantIndexPath + ".masterDefendantId")).getString()))
                    .assertThat(partiesIndexPath + ".courtProceedingsInitiated", is(((JsonString) inputParties.read(defendantIndexPath + ".courtProceedingsInitiated")).getString()))
                    .assertThat(partiesIndexPath + ".pncId", equalTo(((JsonString) inputParties.read(defendantIndexPath + ".pncId")).getString()))
                    .assertThat(partiesIndexPath + ".croNumber", equalTo(((JsonString) inputParties.read(defendantIndexPath + ".croNumber")).getString()))
                    .assertThat(partiesIndexPath + ".firstName", equalTo(((JsonString) inputParties.read(defendantIndexPath + ".personDefendant.personDetails.firstName")).getString()))
                    .assertThat(partiesIndexPath + ".middleName", equalTo(((JsonString) inputParties.read(defendantIndexPath + ".personDefendant.personDetails.middleName")).getString()))
                    .assertThat(partiesIndexPath + ".lastName", equalTo(((JsonString) inputParties.read(defendantIndexPath + ".personDefendant.personDetails.lastName")).getString()))
                    .assertThat(partiesIndexPath + ".dateOfBirth", equalTo(((JsonString) inputParties.read(defendantIndexPath + ".personDefendant.personDetails.dateOfBirth")).getString()))
                    .assertThat(partiesIndexPath + ".gender", equalTo(((JsonString) inputParties.read(defendantIndexPath + ".personDefendant.personDetails.gender")).getString()))
                    .assertThat(partiesIndexPath + ".postCode", equalTo(((JsonString) inputParties.read(defendantIndexPath + ".personDefendant.personDetails.address.postcode")).getString()))
                    .assertThat(partiesIndexPath + ".addressLines", equalTo(addressLines(inputParties, defendantIndexPath + ".personDefendant.personDetails.address")))
                    .assertThat(partiesIndexPath + ".pncId", equalTo(((JsonString) inputParties.read(defendantIndexPath + ".pncId")).getString()))
                    .assertThat(partiesIndexPath + ".arrestSummonsNumber", equalTo(((JsonString) inputParties.read(defendantIndexPath + ".personDefendant.arrestSummonsNumber")).getString()))
                    .assertThat(partiesIndexPath + ".nationalInsuranceNumber", equalTo(((JsonString) inputParties.read(defendantIndexPath + ".personDefendant.personDetails.nationalInsuranceNumber")).getString()));
            incrementPersonDefendantCount();
        } catch (final PathNotFoundException e) {
            incrementExceptionCount();
            logger.log(WARNING, format("Exception validating PersonDefendant", e.getMessage()));
        }
    }

    protected void validateLegalEntity(final DocumentContext inputParties,
                                       final JsonObject outputParties,
                                       final String partiesIndexPath,
                                       final String defendantIndexPath) {
        try {
            with(outputParties.toString())
                    .assertThat(partiesIndexPath + ".organisationName", equalTo(((JsonString) inputParties.read(defendantIndexPath + ".legalEntityDefendant.organisation.name")).getString()));
            incrementLegalEntityCount();
        } catch (final PathNotFoundException e) {
            incrementExceptionCount();
            logger.log(WARNING, format("Exception validating LegalEntity", e.getMessage()));
        }
    }


}
