package uk.gov.moj.cpp.progression.ingester;

import static com.jayway.jsonpath.JsonPath.parse;
import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static java.util.Optional.empty;
import static java.util.Optional.of;
import static java.util.UUID.randomUUID;
import static uk.gov.justice.services.messaging.JsonObjects.createObjectBuilder;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static uk.gov.justice.core.courts.Organisation.organisation;
import static uk.gov.justice.services.test.utils.core.messaging.JsonObjects.getJsonArray;
import static uk.gov.moj.cpp.progression.helper.AbstractTestHelper.getWriteUrl;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.addProsecutionCaseToCrownCourtForIngestion;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.createReferProsecutionCaseToCrownCourtJsonBody;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.generateUrn;
import static uk.gov.moj.cpp.progression.helper.RestHelper.postCommand;
import static uk.gov.moj.cpp.progression.helper.UnifiedSearchIndexSearchHelper.findBy;
import static uk.gov.moj.cpp.progression.ingester.verificationHelpers.IngesterUtil.getPoller;
import static uk.gov.moj.cpp.progression.ingester.verificationHelpers.IngesterUtil.jsonFromString;
import static uk.gov.moj.cpp.progression.ingester.verificationHelpers.ProsecutionCaseVerificationHelper.verifyCaseCreated;
import static uk.gov.moj.cpp.progression.ingester.verificationHelpers.ProsecutionCaseVerificationHelper.verifyCaseDefendant;

import uk.gov.justice.core.courts.AddDefendantsToCourtProceedings;
import uk.gov.justice.core.courts.Address;
import uk.gov.justice.core.courts.ContactNumber;
import uk.gov.justice.core.courts.CourtCentre;
import uk.gov.justice.core.courts.Defendant;
import uk.gov.justice.core.courts.HearingType;
import uk.gov.justice.core.courts.JurisdictionType;
import uk.gov.justice.core.courts.LaaReference;
import uk.gov.justice.core.courts.LegalEntityDefendant;
import uk.gov.justice.core.courts.ListDefendantRequest;
import uk.gov.justice.core.courts.ListHearingRequest;
import uk.gov.justice.core.courts.Offence;
import uk.gov.justice.core.courts.Plea;
import uk.gov.moj.cpp.progression.AbstractIT;
import uk.gov.moj.cpp.progression.ingester.verificationHelpers.BaseVerificationHelper;
import uk.gov.moj.cpp.progression.util.Utilities;

import java.io.IOException;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import javax.json.JsonObject;

import com.jayway.jsonpath.DocumentContext;
import junit.framework.TestCase;
import org.hamcrest.Matcher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

public class AddDefendantsToCourtProceedingsIT extends AbstractIT {
    private static final String REFER_TO_CROWN_COMMAND_RESOURCE_LOCATION = "ingestion/progression.command.prosecution-case-refer-to-court.json";
    private static final String PROGRESSION_ADD_DEFENDANTS_TO_COURT_PROCEEDINGS_JSON = "application/vnd.progression.add-defendants-to-court-proceedings+json";
    private String caseId;
    private String courtDocumentId;
    private String materialIdActive;
    private String materialIdDeleted;
    private String defendantId;
    private String referralReasonId;
    private final BaseVerificationHelper verificationHelper = new BaseVerificationHelper();

    @BeforeEach
    public void setUp() {
        caseId = randomUUID().toString();
        materialIdActive = randomUUID().toString();
        materialIdDeleted = randomUUID().toString();
        courtDocumentId = randomUUID().toString();
        defendantId = randomUUID().toString();
        referralReasonId = randomUUID().toString();
        deleteAndCreateIndex();
    }

    @Test
    @Disabled("Flaky tests - passed locally failed at pipeline")
    public void shouldInvokeDefendantsAddedToCaseAndListHearingRequestEvents() throws Exception {

        //Create prosecution case
        final String caseUrn = generateUrn();
        addProsecutionCaseToCrownCourtForIngestion(caseId, defendantId, materialIdActive, materialIdDeleted, courtDocumentId, referralReasonId, caseUrn, REFER_TO_CROWN_COMMAND_RESOURCE_LOCATION);

        final Matcher[] caseMatcher = {withJsonPath("$.caseReference", equalTo(caseUrn)),
                withJsonPath("$.caseId", equalTo(caseId))};

        final Optional<JsonObject> prosecussionCaseResponseJsonObject = findBy(caseMatcher);

        TestCase.assertTrue(prosecussionCaseResponseJsonObject.isPresent());

        final JsonObject outputCase = prosecussionCaseResponseJsonObject.get();
        final JsonObject prosecutionCase = documentContextProsecutionCase(caseUrn);
        final JsonObject prosecutionCase1 = prosecutionCase.getJsonObject("prosecutionCase");

        final DocumentContext inputProsecutionCase = parse(prosecutionCase);
        verifyCaseCreated(1L, inputProsecutionCase, outputCase);
        verifyCaseDefendant(inputProsecutionCase, outputCase, true);

        final String offenceId = randomUUID().toString();
        final String defendantId2 = randomUUID().toString();

        //Create payload for
        final AddDefendantsToCourtProceedings addDefendantsToCourtProceedings = buildAddDefendantsToCourtProceedings(
                true, caseId, defendantId, defendantId2, offenceId);
        final String addDefendantsToCourtProceedingsString = Utilities.JsonUtil.toJsonString(addDefendantsToCourtProceedings);

        postCommand(getWriteUrl("/adddefendantstocourtproceedings"), PROGRESSION_ADD_DEFENDANTS_TO_COURT_PROCEEDINGS_JSON, addDefendantsToCourtProceedingsString);

        final DocumentContext updatedInputDC = documentContextForDefendantAddedEvent(addDefendantsToCourtProceedingsString);

        final Optional<JsonObject> prosecutionCaseResponseJsonObject = getPoller().pollUntilFound(() -> {

            try {
                final JsonObject jsonObject = elasticSearchIndexFinderUtil.findAll("crime_case_index");
                if (jsonObject.getInt("totalResults") == 1 && isPartiesPopulated(jsonObject, 2)) {
                    return of(jsonObject);
                }
            } catch (final IOException e) {
                TestCase.fail();
            }
            return empty();
        });

        assertTrue(prosecutionCaseResponseJsonObject.isPresent());
        final JsonObject output = jsonFromString(getJsonArray(prosecutionCaseResponseJsonObject.get(), "index").get().getString(0));

        final int indexSize = prosecutionCaseResponseJsonObject.get().getJsonArray("index").size();
        assertThat(indexSize, is(1));

        final DocumentContext inputProsecutionCaseForDefendant1 = parse(documentContextProsecutionCase(caseUrn));
        verifyCaseDefendant(inputProsecutionCaseForDefendant1, output, true);

        final int verifyProsecutionCaseIdForDefendantAtIndex = 0;
        verificationHelper.verifyProsecutionCase(updatedInputDC, output, verifyProsecutionCaseIdForDefendantAtIndex);

        final int expectedPartyCount = 2;
        verificationHelper.verifyCasePartyCount(output, expectedPartyCount);

        int outputPartyIndex = 0;
        int inputDefendantIndex = 0;
        boolean referredOffence = true;

        verificationHelper.verifyDefendant(parse(prosecutionCase1), output, outputPartyIndex, inputDefendantIndex, referredOffence);

        outputPartyIndex = 1;
        inputDefendantIndex = 1;
        referredOffence = false;
        verificationHelper.verifyDefendant(updatedInputDC, output, outputPartyIndex, inputDefendantIndex, referredOffence);

        outputPartyIndex = 0;
        inputDefendantIndex = 0;
        int outputPartyAliasIndex = 0;
        int inputDefendantAliasIndex = 0;
        verificationHelper.validateAliases(parse(prosecutionCase1), output, outputPartyIndex, inputDefendantIndex, outputPartyAliasIndex, inputDefendantAliasIndex);

        final int expectedCaseCount = 1;
        final int expectedPersonDefendantCount = 1;
        final int expectedLegalEntityDefendantCount = 2;
        final int expectedAliasesCaseCount = 1;
        final int expectedOffencesCaseCount = 2;
        final int expectedExceptionsCount = 1;

        verificationHelper.verifyCounts(expectedCaseCount, expectedPartyCount, expectedPersonDefendantCount, expectedLegalEntityDefendantCount, expectedAliasesCaseCount, expectedOffencesCaseCount, expectedExceptionsCount);
    }


    private AddDefendantsToCourtProceedings buildAddDefendantsToCourtProceedings(
            final boolean forAdded, final String caseId, final String defendantId, final String defendantId2, final String offenceId) {

        final List<Defendant> defendantsList = new ArrayList<>();

        final Plea plea = Plea.plea()
                .withOriginatingHearingId(randomUUID())
                .withPleaValue("GUILTY")
                .withPleaDate(LocalDate.of(2019, 8, 12))
                .withOffenceId(UUID.fromString(offenceId))
                .withApplicationId(randomUUID())
                .build();

        final LaaReference laaReference = LaaReference.laaReference()
                .withApplicationReference("LaaReference")
                .withStatusId(randomUUID())
                .withStatusCode("withStatusCode")
                .withStatusDate(LocalDate.of(2019, 5, 1))
                .withStatusDescription("withStatusDescription").build();

        final Offence offence = Offence.offence()
                .withId(UUID.fromString(offenceId))
                .withOffenceDefinitionId(randomUUID())
                .withOffenceCode("TFL123")
                .withOffenceTitle("TFL Ticket Dodger")
                .withWording("TFL ticket dodged")
                .withStartDate(LocalDate.of(2019, 5, 1))
                .withEndDate(LocalDate.of(2020, 5, 1))
                .withArrestDate(LocalDate.of(2023, 5, 1))
                .withChargeDate(LocalDate.of(2023, 6, 1))
                .withOffenceLegislation("withOffenceLegislation")
                .withDateOfInformation(LocalDate.of(2022, 5, 1))
                .withModeOfTrial("withModeOfTrial")
                .withOrderIndex(0)
                .withProceedingsConcluded(true)
                .withLaaApplnReference(laaReference)
                .withPlea(plea)
                .build();

        //past duplicate defendant
        final Defendant defendant = Defendant.defendant()
                .withId(UUID.fromString(defendantId))
                .withMasterDefendantId(UUID.fromString(defendantId))
                .withCourtProceedingsInitiated(ZonedDateTime.now(ZoneId.of("UTC")))
                .withProsecutionCaseId(UUID.fromString(caseId))
                .withOffences(Collections.singletonList(offence))
                .build();
        defendantsList.add(defendant);

        final Address address = Address.address()
                .withAddress1("address Line1")
                .withAddress2("address Line2")
                .withAddress3("address Line3")
                .withAddress4("address Line4")
                .withAddress5("address Line5")
                .withPostcode("CR0 5NN").build();

        final ContactNumber work = ContactNumber.contactNumber().withPrimaryEmail("test@man.com").withWork("work").build();
        final LegalEntityDefendant legalEntityDefendant = LegalEntityDefendant.legalEntityDefendant()
                .withOrganisation(organisation()
                        .withName("Man and co")
                        .withAddress(address)
                        .withIncorporationNumber("1234567")
                        .withRegisteredCharityNumber("232323")
                        .withContact(work)
                        .build()).build();
        //Add defendant
        final Defendant defendant2 = Defendant.defendant()
                .withLegalEntityDefendant(legalEntityDefendant)
                .withId(UUID.fromString(defendantId2))
                .withMasterDefendantId(UUID.fromString(defendantId2))
                .withCourtProceedingsInitiated(ZonedDateTime.now(ZoneId.of("UTC")))
                .withPncId("pncId")
                .withCroNumber("croNumber")
                .withProsecutionCaseId(UUID.fromString(caseId))
                .withOffences(Collections.singletonList(offence))
                .build();


        if (forAdded) {
            defendantsList.add(defendant2);
        }


        final ListDefendantRequest listDefendantRequest2 = ListDefendantRequest.listDefendantRequest()
                .withProsecutionCaseId(UUID.fromString(caseId))
                .withDefendantOffences(Collections.singletonList(UUID.fromString(offenceId)))
                .withDefendantId(defendant2.getId())
                .build();

        final HearingType hearingType = HearingType.hearingType().withId(randomUUID()).withDescription("TO_JAIL").build();
        final CourtCentre courtCentre = CourtCentre.courtCentre().withId(randomUUID()).withName("Court Centre 1").build();

        final ListHearingRequest listHearingRequest = ListHearingRequest.listHearingRequest()
                .withCourtCentre(courtCentre).withHearingType(hearingType)
                .withJurisdictionType(JurisdictionType.MAGISTRATES)
                .withListDefendantRequests(Collections.singletonList(listDefendantRequest2))
                .withEarliestStartDateTime(ZonedDateTime.now().plusWeeks(1))
                .withEstimateMinutes(20)
                .build();

        return AddDefendantsToCourtProceedings
                .addDefendantsToCourtProceedings()
                .withDefendants(defendantsList)
                .withListHearingRequests(Collections.singletonList(listHearingRequest))
                .build();
    }

    private boolean isPartiesPopulated(final JsonObject jsonObject, final int partySize) {
        final JsonObject indexData = jsonFromString(getJsonArray(jsonObject, "index").get().getString(0));
        return indexData.containsKey("parties") && (indexData.getJsonArray("parties").size() == partySize);
    }

    private JsonObject documentContextProsecutionCase(final String caseUrn) {

        final String commandJson = createReferProsecutionCaseToCrownCourtJsonBody(caseId, defendantId, randomUUID().toString(), randomUUID().toString(),
                courtDocumentId, randomUUID().toString(), caseUrn, REFER_TO_CROWN_COMMAND_RESOURCE_LOCATION);
        final JsonObject commandJsonInputJson = jsonFromString(commandJson);
        final DocumentContext prosecutionCase = parse(commandJsonInputJson);
        final JsonObject prosecutionCaseJO = prosecutionCase.read("$.courtReferral.prosecutionCases[0]");
        return createObjectBuilder().add("prosecutionCase", prosecutionCaseJO).build();
    }

    private DocumentContext documentContextForDefendantAddedEvent(final String jsonDefendantAddedCommandString) {
        final JsonObject commandJsonInputJson = jsonFromString(jsonDefendantAddedCommandString);
        return parse(commandJsonInputJson);
    }
}
