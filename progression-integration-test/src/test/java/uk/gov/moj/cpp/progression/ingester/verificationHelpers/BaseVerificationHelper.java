package uk.gov.moj.cpp.progression.ingester.verificationHelpers;

import static com.jayway.jsonassert.JsonAssert.with;
import static java.lang.Boolean.valueOf;
import static java.lang.String.format;
import static java.util.logging.Level.WARNING;
import static java.util.stream.IntStream.range;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static uk.gov.moj.cpp.progression.ingester.verificationHelpers.AddressVerificationHelper.addressLines;

import java.util.LinkedHashMap;
import java.util.logging.Logger;

import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.JsonString;

import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.PathNotFoundException;
import net.minidev.json.JSONArray;

public class BaseVerificationHelper extends BaseVerificationCountHelper {
    private static final Logger logger = Logger.getLogger(BaseVerificationHelper.class.getName());

    private static final String OUTPUT_PARTIES_JSON_PATH_FOR_SIZE = "$.parties[*]";
    private static final String INPUT_OFFENCES_JSON_PATH_FOR_SIZE = "$.defendants[%d].offences";
    private static final String INPUT_OFFENCES_FOR_DEFENDANTS_UPDATED = "$.%s[%d].offences";
    private static final String INPUT_OFFENCES_FOR_DEFENDANTS_ALL = "$.%s[%d].offences[%d].id";
    private static final String INPUT_OFFENCE_ID = "";
    private static final String OUTPUT_OFFENCE_EXP = "$.parties[0].offences[?(@.offenceId==";
    private static final String INPUT_OFFENCE_EXP = "$.%s[%d].offences[?(@.id==";
    private static final String INPUT_OFFENCE_DELETE_EXP = "$.deletedOffences";


    private static final String INPUT_DEFENDANTS_JSON_PATH = "$.defendants[%d]";
    private static final String INPUT_OFFENCES_JSON_PATH = "$.defendants[%d].offences[%d]";
    private static final String INPUT_ALIASES_JSON_PATH = "$.defendants[%d].aliases[%d]";

    private static final String OUTPUT_CASE_JSON_PATH = "$";
    private static final String OUTPUT_ALIASES_JSON_PATH = "$.parties[%d].aliases[%d]";
    private static final String OUTPUT_PARTIES_JSON_PATH = "$.parties[%d]";
    private static final String OUTPUT_OFFENCES_JSON_PATH = "$.parties[%d].offences[%d]";

    public void verifyProsecutionCase(final DocumentContext inputProsecutionCase,
                                      final JsonObject outputCase,
                                      final String inputDefendantPath) {
        try {
            final String outputCaseDocumentsPath = format(OUTPUT_CASE_JSON_PATH);
            with(outputCase.toString())
                    .assertThat(outputCaseDocumentsPath + ".caseId", equalTo(((JsonString) inputProsecutionCase.read(inputDefendantPath + ".id")).getString()))
                    .assertThat(outputCaseDocumentsPath + "._case_type", equalTo("PROSECUTION"))
                    .assertThat(outputCaseDocumentsPath + ".parties[0].offences[0].verdict.verdictDate", equalTo("2019-01-01"))
                    .assertThat(outputCaseDocumentsPath + ".parties[0].offences[0].verdict.verdictType.category", equalTo("category"))
                    .assertThat(outputCaseDocumentsPath + ".parties[0].offences[0].verdict.verdictType.categoryType", equalTo("categoryType"))
                    .assertThat(outputCaseDocumentsPath + ".parties[0].offences[0].verdict.verdictType.sequence", equalTo(0))
                    .assertThat(outputCaseDocumentsPath + ".parties[0].offences[0].verdict.verdictType.verdictTypeId", equalTo("3789ab16-0bb7-4ef1-87ef-c936bf0364f2"));
            incrementCaseDocumentsCount();
        } catch (final Exception e) {
            incrementExceptionCount();
            logger.log(WARNING, format("Exception validating ProsecutionCase ", e.getMessage()));
        }
    }

    public void verifyProsecutionCase(final DocumentContext inputProsecutionCase,
                                      final JsonObject outputCase,
                                      final int defendantIndex) {
        try {
            final String outputCaseDocumentsPath = format(OUTPUT_CASE_JSON_PATH);
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

    public void verifyApplication(final DocumentContext inputCourtApplication,
                                  final JsonObject outputCase,
                                  final String inputApplicationPath) {
        final String outputCaseDocumentsPath = format(OUTPUT_CASE_JSON_PATH);

        final String id = ((JsonString) inputCourtApplication.read(inputApplicationPath + "id")).getString();
        final String applicationReference = ((JsonString) inputCourtApplication.read(inputApplicationPath + ".applicationReference")).getString();
        final String applicationType = ((JsonString) inputCourtApplication.read(inputApplicationPath + ".type.type")).getString();
        final String applicationReceivedDate = ((JsonString) inputCourtApplication.read(inputApplicationPath + ".applicationReceivedDate")).getString();
        final String applicationDecisionSoughtByDate = ((JsonString) inputCourtApplication.read(inputApplicationPath + ".applicationDecisionSoughtByDate")).getString();

        final boolean isLinkedApplication = inputCourtApplication.read(inputApplicationPath).toString().contains("courtApplicationCases");

        try {
            if (isLinkedApplication) {
                with(outputCase.toString()).assertThat(outputCaseDocumentsPath + ".caseId", not(equalTo(((JsonString) inputCourtApplication.read(inputApplicationPath + ".id")).getString())))
                        .assertNotDefined(outputCaseDocumentsPath + ".caseStatus")
                        .assertThat(outputCaseDocumentsPath + "._case_type", equalTo("PROSECUTION"));
            } else {
                with(outputCase.toString())
                        .assertThat(outputCaseDocumentsPath + ".caseId", equalTo(((JsonString) inputCourtApplication.read(inputApplicationPath + ".id")).getString()))
                        .assertThat(outputCaseDocumentsPath + "._case_type", equalTo("APPLICATION"));
            }

            with(outputCase.toString())
                    .assertThat(outputCaseDocumentsPath + ".applications[0].applicationId", is(id))
                    .assertThat(outputCaseDocumentsPath + ".applications[0].applicationReference", is(applicationReference))
                    .assertThat(outputCaseDocumentsPath + ".applications[0].applicationType", is(applicationType))
                    .assertThat(outputCaseDocumentsPath + ".applications[0].receivedDate", is(applicationReceivedDate))
                    .assertThat(outputCaseDocumentsPath + ".applications[0].decisionDate", is(applicationDecisionSoughtByDate));
            incrementCaseDocumentsCount();
        } catch (final Exception e) {
            incrementExceptionCount();
            logger.log(WARNING, format("Exception validating ProsecutionCase ", e.getMessage()));
        }
    }


    public void verifyCasePartyCount(final JsonObject outputCase,
                                     final int partyCount) {

        try {
            final String outputPartiesIndexPathCreated = format(OUTPUT_PARTIES_JSON_PATH_FOR_SIZE);
            with(outputCase.toString()).assertThat(outputPartiesIndexPathCreated, hasSize(partyCount));
        } catch (final Exception e) {
            incrementExceptionCount();
            logger.log(WARNING, format("Exception validating Defendants for Multi Cases", e.getMessage()));
        }
    }

    public void verifyDefendant(final DocumentContext inputProsecutionCase,
                                final JsonObject outputCase,
                                final int outputPartyIndex,
                                final int inputPartyIndex,
                                final boolean referredOffence) {
        final String outputPartiesIndexPathCreated = format(OUTPUT_PARTIES_JSON_PATH, outputPartyIndex);
        final String inputDefendantIndexPathCreated = format(INPUT_DEFENDANTS_JSON_PATH, inputPartyIndex);

        validateLegalEntity(inputProsecutionCase, outputCase, outputPartiesIndexPathCreated, inputDefendantIndexPathCreated);
        validatePersonDefendant(inputProsecutionCase, outputCase, outputPartiesIndexPathCreated, inputDefendantIndexPathCreated);
        validateOffences(inputProsecutionCase, outputCase, inputPartyIndex, outputPartyIndex, referredOffence);
        incrementPartiesCount();
    }

    public void validateAliases(final DocumentContext inputParties,
                                final JsonObject outputParties,
                                final int outputPartyIndex,
                                final int inputPartyIndex,
                                final int outputAliasIndex,
                                final int inputAliasIndex) {
        try {
            final String aliasPath = format(OUTPUT_ALIASES_JSON_PATH, outputPartyIndex, outputAliasIndex);
            final String inputAliasPath = format(INPUT_ALIASES_JSON_PATH, inputPartyIndex, inputAliasIndex);

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

    protected void validateOffences(final JsonObject inputPartiesJsonObject,
                                    final JsonObject outputParties,
                                    final int inputPartyIndex,
                                    final int outputPartyIndex,
                                    final boolean refferedOffence,
                                    final String inputPartyIdentifier) {

        try {
            final DocumentContext defendantChangedEventDocument = JsonPath.parse(inputPartiesJsonObject);
            final String inputOffencePath = format(INPUT_OFFENCES_FOR_DEFENDANTS_UPDATED, inputPartyIdentifier, 0);
            final JsonArray offenceCourt = defendantChangedEventDocument.read(inputOffencePath);

            final int offenceSize = offenceCourt.size();

            range(0, offenceSize)
                    .forEach(offenceIndex -> {
                        final String var1 = defendantChangedEventDocument.read(inputOffencePath + "[" + offenceIndex + "]" + (!inputOffencePath.contains("deletedOffences") ? ".id" : "")).toString();
                        final String var2 = OUTPUT_OFFENCE_EXP + var1 + ")]";
                        final String var3 = inputPartyIdentifier.contains("deletedOffences") ? format(INPUT_OFFENCE_DELETE_EXP) : format(INPUT_OFFENCE_EXP + var1 + ")]", inputPartyIdentifier, 0);
                        final LinkedHashMap outputOffence = (LinkedHashMap) ((JSONArray) JsonPath.read(outputParties.toString(), var2)).get(0);
                        final LinkedHashMap inputOffence = (LinkedHashMap) ((JSONArray) JsonPath.read(inputPartiesJsonObject.toString(), var3)).get(0);
                        if (inputPartyIdentifier.contains("updatedOffences"))
                            validateUpdatedOffence(inputOffence, outputOffence);
                        if (inputPartyIdentifier.contains("addedOffences"))
                            validateAddedOffence(inputOffence, outputOffence);
                        if (inputPartyIdentifier.contains("deletedOffences"))
                            validateDeletedOffence(inputOffence, outputOffence);
                    });
        } catch (final Exception e) {
            incrementExceptionCount();
            logger.log(WARNING, format("Exception validating Offences", e.getMessage()));
        }
    }


    protected void validateOffences(final DocumentContext inputParties,
                                    final JsonObject outputParties,
                                    final int inputPartyIndex,
                                    final int outputPartyIndex,
                                    final boolean refferedOffence) {

        try {
            final JsonArray offenceCourt = inputParties.read(format(INPUT_OFFENCES_JSON_PATH_FOR_SIZE, inputPartyIndex));
            final int offenceSize = offenceCourt.size();
            range(0, offenceSize)
                    .forEach(offenceIndex -> {
                        final String offenceInputIndexPath = format(INPUT_OFFENCES_JSON_PATH, inputPartyIndex, offenceIndex);
                        final String offenceOutputIndexPath = format(OUTPUT_OFFENCES_JSON_PATH, outputPartyIndex, offenceIndex);
                        if (refferedOffence) {
                            validateReferredOffence(inputParties, outputParties, offenceInputIndexPath, offenceOutputIndexPath);
                        } else {
                            validateOffence(inputParties, outputParties, offenceInputIndexPath, offenceOutputIndexPath);

                        }
                    });
        } catch (final Exception e) {
            incrementExceptionCount();
            logger.log(WARNING, format("Exception validating Offences", e.getMessage()));
        }
    }

    private void validateReferredOffence(final DocumentContext inputParties,
                                         final JsonObject outputParties,
                                         final String offenceInputIndexPath,
                                         final String offenceOutputIndexPath) {
        try {
            with(outputParties.toString())
                    .assertThat(offenceOutputIndexPath + ".offenceId", equalTo(((JsonString) inputParties.read(offenceInputIndexPath + ".id")).getString()))
                    .assertThat(offenceOutputIndexPath + ".endDate", equalTo(((JsonString) inputParties.read(offenceInputIndexPath + ".endDate")).getString()))
                    .assertThat(offenceOutputIndexPath + ".arrestDate", equalTo(((JsonString) inputParties.read(offenceInputIndexPath + ".arrestDate")).getString()))
                    .assertThat(offenceOutputIndexPath + ".chargeDate", equalTo(((JsonString) inputParties.read(offenceInputIndexPath + ".chargeDate")).getString()))
                    .assertThat(offenceOutputIndexPath + ".startDate", equalTo(((JsonString) inputParties.read(offenceInputIndexPath + ".startDate")).getString()))
                    .assertThat(offenceOutputIndexPath + ".orderIndex", equalTo(Integer.valueOf(inputParties.read(offenceInputIndexPath + ".orderIndex").toString())));
            incrementOffencesCount();
        } catch (Exception e) {
            incrementExceptionCount();
            logger.log(WARNING, format("Exception validating Offence", e.getMessage()));
        }
    }


    private void validateUpdatedOffence(final LinkedHashMap inputOffence,
                                        final LinkedHashMap outputOffence
    ) {
        try {
            assertThat(outputOffence.get("offenceId"), equalTo(inputOffence.get("id")));
            assertThat(outputOffence.get("endDate"), not(equalTo(inputOffence.get("endDate"))));
            assertThat(outputOffence.get("arrestDate"), not(equalTo(inputOffence.get("arrestDate"))));
            assertThat(outputOffence.get("chargeDate"), not(equalTo(inputOffence.get("chargeDate"))));
            assertThat(outputOffence.get("startDate"), not(equalTo(inputOffence.get("startDate"))));
            assertThat(outputOffence.get("orderIndex"), not(equalTo(inputOffence.get("orderIndex"))));
            incrementOffencesCount();
        } catch (Exception e) {
            incrementExceptionCount();
            logger.log(WARNING, format("Exception validating Offence", e.getMessage()));
        }
    }

    private void validateAddedOffence(final LinkedHashMap inputOffence,
                                      final LinkedHashMap outputOffence
    ) {
        try {
            assertThat(outputOffence.get("offenceId"), equalTo(inputOffence.get("id")));
            assertThat(outputOffence.get("endDate"), equalTo(inputOffence.get("endDate")));
            assertThat(outputOffence.get("arrestDate"), equalTo(inputOffence.get("arrestDate")));
            assertThat(outputOffence.get("chargeDate"), equalTo(inputOffence.get("chargeDate")));
            assertThat(outputOffence.get("startDate"), equalTo(inputOffence.get("startDate")));
            assertThat(outputOffence.get("orderIndex"), equalTo(inputOffence.get("orderIndex")));
            incrementOffencesCount();
        } catch (Exception e) {
            incrementExceptionCount();
            logger.log(WARNING, format("Exception validating Offence", e.getMessage()));
        }
    }

    private void validateDeletedOffence(final LinkedHashMap inputOffence,
                                        final LinkedHashMap outputOffence
    ) {
        try {
            assertThat(outputOffence.get("offenceId"), equalTo(((JSONArray) inputOffence.get("offences")).get(0)));
            incrementOffencesCount();
        } catch (Exception e) {
            incrementExceptionCount();
            logger.log(WARNING, format("Exception validating Offence", e.getMessage()));
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

            with(outputParties.toString())
                    .assertThat(partiesIndexPath + ".title", equalTo(((JsonString) inputParties.read(defendantIndexPath + ".personDefendant.personDetails.title")).getString()))
                    .assertThat(partiesIndexPath + "._party_type", equalTo("DEFENDANT"))
                    .assertThat(partiesIndexPath + ".partyId", equalTo(((JsonString) inputParties.read(defendantIndexPath + ".id")).getString()))
                    .assertThat(partiesIndexPath + ".firstName", equalTo(((JsonString) inputParties.read(defendantIndexPath + ".personDefendant.personDetails.firstName")).getString()))
                    .assertThat(partiesIndexPath + ".middleName", equalTo(((JsonString) inputParties.read(defendantIndexPath + ".personDefendant.personDetails.middleName")).getString()))
                    .assertThat(partiesIndexPath + ".lastName", equalTo(((JsonString) inputParties.read(defendantIndexPath + ".personDefendant.personDetails.lastName")).getString()))
                    .assertThat(partiesIndexPath + ".dateOfBirth", equalTo(((JsonString) inputParties.read(defendantIndexPath + ".personDefendant.personDetails.dateOfBirth")).getString()))
                    .assertThat(partiesIndexPath + ".gender", equalTo(((JsonString) inputParties.read(defendantIndexPath + ".personDefendant.personDetails.gender")).getString()))
                    .assertThat(partiesIndexPath + ".postCode", equalTo(((JsonString) inputParties.read(defendantIndexPath + ".personDefendant.personDetails.address.postcode")).getString()))
                    .assertThat(partiesIndexPath + ".addressLines", equalTo(addressLines(inputParties, defendantIndexPath + ".personDefendant.personDetails.address")))
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
                    .assertThat(partiesIndexPath + ".organisationName", equalTo(((JsonString) inputParties.read(defendantIndexPath + ".legalEntityDefendant.organisation.name")).getString()))
                    .assertThat(partiesIndexPath + ".organisationName", equalTo(((JsonString) inputParties.read(defendantIndexPath + ".legalEntityDefendant.organisation.name")).getString()));
            incrementLegalEntityCount();
        } catch (final PathNotFoundException e) {
            incrementExceptionCount();
            logger.log(WARNING, format("Exception validating LegalEntity", e.getMessage()));
        }
    }


}
