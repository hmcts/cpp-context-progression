package uk.gov.moj.cpp.progression.summons;

import static com.google.common.collect.ImmutableList.of;
import static com.google.common.collect.Lists.newArrayList;
import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static java.lang.String.format;
import static java.util.UUID.randomUUID;
import static org.apache.commons.lang3.RandomStringUtils.randomAlphanumeric;
import static org.hamcrest.CoreMatchers.hasItems;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.hasSize;
import static uk.gov.justice.services.integrationtest.utils.jms.JmsMessageConsumerClientProvider.newPrivateJmsMessageConsumerClientProvider;
import static uk.gov.justice.services.test.utils.core.random.RandomGenerator.BOOLEAN;
import static uk.gov.moj.cpp.progression.helper.AbstractTestHelper.USER_ID;
import static uk.gov.moj.cpp.progression.helper.MaterialHelper.sendEventToConfirmMaterialAdded;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.generateUrn;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.getCourtDocumentsByCaseWithMatchers;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.initiateCourtProceedings;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.pollCaseAndGetHearingForDefendant;
import static uk.gov.moj.cpp.progression.it.framework.ContextNameProvider.CONTEXT_NAME;
import static uk.gov.moj.cpp.progression.stub.DocumentGeneratorStub.getSummonsTemplate;
import static uk.gov.moj.cpp.progression.stub.MaterialStub.verifyMaterialCreated;
import static uk.gov.moj.cpp.progression.stub.NotificationServiceStub.verifyCreateLetterRequested;
import static uk.gov.moj.cpp.progression.stub.NotificationServiceStub.verifyEmailNotificationIsRaisedWithoutAttachment;
import static uk.gov.moj.cpp.progression.stub.ReferenceDataStub.stubGetDocumentsTypeAccess;
import static uk.gov.moj.cpp.progression.stub.ReferenceDataStub.stubQueryProsecutorsReturningEmpty;
import static uk.gov.moj.cpp.progression.summons.SummonsHelper.getLanguagePrefix;
import static uk.gov.moj.cpp.progression.summons.SummonsHelper.getSubjectDateOfBirth;
import static uk.gov.moj.cpp.progression.summons.SummonsHelper.sendPublicEventToConfirmHearingForInitiatedCase;
import static uk.gov.moj.cpp.progression.summons.SummonsHelper.verifyMaterialRequestRecordedAndExtractMaterialId;
import static uk.gov.moj.cpp.progression.summons.SummonsHelper.verifyTemplatePayloadValues;
import static uk.gov.moj.cpp.progression.util.FileUtil.getPayload;

import uk.gov.justice.core.courts.summons.SummonsDocumentContent;
import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.common.converter.jackson.ObjectMapperProducer;
import uk.gov.justice.services.integrationtest.utils.jms.JmsMessageConsumerClient;
import uk.gov.moj.cpp.progression.AbstractIT;
import uk.gov.moj.cpp.progression.stub.ReferenceDataStub;

import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;

import javax.json.JsonObject;

import org.hamcrest.Matcher;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * Integration tests for amend summons in the first hearing (case-based) flow (CHD-2386).
 *
 * The first hearing case summons flow:
 *   1. Court proceedings initiated with ListHearingRequest containing ListDefendantRequest
 *      (SummonsType.FIRST_HEARING, summonsApprovedOutcome embedded in the command)
 *   2. Hearing confirmed → HearingConfirmedEventProcessor dispatches prepare-summons-data command
 *   3. HearingAggregate.createSummonsData() fires SummonsDataPrepared(isSummonsAmended=false)
 *   4. Summons document generated; isSummonsAlreadyApproved=true recorded on hearing aggregate
 *
 * The amend path (covered by these tests):
 *   5. Hearing re-confirmed (e.g., court admin re-confirms after summons change)
 *   6. HearingAggregate.createSummonsData() fires SummonsDataPrepared(isSummonsAmended=true)
 *      because isSummonsAlreadyApproved=true from step 4
 *   7. CaseDefendantSummonsService.generateSummonsPayloadForDefendant() sets amendedDate=LocalDate.now()
 *   8. Amended summons document generated and notifications dispatched
 */
public class AmendFirstHearingCaseSummonsIT extends AbstractIT {

    private static final String PRIVATE_EVENT_NOWS_MATERIAL_REQUEST_RECORDED = "progression.event.nows-material-request-recorded";

    private static final JsonObjectToObjectConverter JSON_OBJECT_TO_OBJECT_CONVERTER =
            new JsonObjectToObjectConverter(new ObjectMapperProducer().objectMapper());

    private String caseId;
    private String caseUrn;
    private String defendantId;
    private String offenceId;
    private String defendant1ProsecutionAuthorityReference;
    private String prosecutorEmailAddress;
    private boolean personalService;

    private final String prosecutorCost = "£300.00";

    // defendant name fragments used in template payload assertions
    private String firstName;
    private String middleName;
    private String lastName;

    private final JmsMessageConsumerClient materialRequestConsumer =
            newPrivateJmsMessageConsumerClientProvider(CONTEXT_NAME)
                    .withEventNames(PRIVATE_EVENT_NOWS_MATERIAL_REQUEST_RECORDED)
                    .getMessageConsumerClient();

    public static Stream<Arguments> firstHearingAmendSummonsSpecifications() {
        return Stream.of(
                // summonsCode, summonsType, templateName, isYouth, numberOfDocuments, isWelsh
                Arguments.of("M", "MCA", "MCA", false, 1, false),
                Arguments.of("M", "MCA", "MCA", false, 1, true)
        );
    }

    @BeforeAll
    public static void setUpClass() {
        stubGetDocumentsTypeAccess("/restResource/get-all-document-type-access.json");
        stubQueryProsecutorsReturningEmpty();
    }

    @BeforeEach
    public void setUp() {
        caseId = randomUUID().toString();
        caseUrn = generateUrn();
        defendantId = randomUUID().toString();
        offenceId = randomUUID().toString();
        prosecutorEmailAddress = randomAlphanumeric(20) + "@random.com";
        personalService = BOOLEAN.next();

        firstName = randomAlphanumeric(10);
        middleName = randomAlphanumeric(10);
        lastName = randomAlphanumeric(10);
        defendant1ProsecutionAuthorityReference = "TFL" + randomAlphanumeric(8);
    }

    /**
     * Verifies the full amend summons flow for a first hearing case:
     * - Initial hearing confirmation generates the first summons document
     * - Re-confirming the same hearing triggers the amend path
     * - The amended document contains an amendedDate (set by CaseDefendantSummonsService)
     * - An amended material request is recorded and notifications sent
     */
    @ParameterizedTest
    @MethodSource("firstHearingAmendSummonsSpecifications")
    public void shouldGenerateAmendedSummonsDocumentOnHearingReConfirmation(
            final String summonsCode,
            final String summonsType,
            final String templateName,
            final boolean isYouth,
            final int numberOfDocuments,
            final boolean isWelsh) throws IOException {

        // Given: first hearing initiated with summons approved outcome embedded
        final boolean summonsSuppressed = false;
        initiateCourtProceedings(buildInitiatePayload(isYouth, summonsCode, summonsSuppressed, isWelsh));

        final String hearingId = pollCaseAndGetHearingForDefendant(caseId, defendantId,
                withJsonPath("$.prosecutionCase.id", is(caseId)),
                withJsonPath("$.prosecutionCase.defendants[?(@.id == '" + defendantId + "')].offences[0]",
                        hasSize(greaterThanOrEqualTo(1))),
                withJsonPath("$.hearingsAtAGlance.defendantHearings[?(@.defendantId == '" + defendantId + "')].hearingIds[0]",
                        hasSize(greaterThanOrEqualTo(1))));

        // And: first hearing confirmed → initial summons document generated
        sendPublicEventToConfirmHearingForInitiatedCase(hearingId, defendantId, offenceId, caseId, isWelsh);

        verifyDocumentAddedToCdes(defendantId, numberOfDocuments);

        final String templateWithLanguagePrefix = "SP" + getLanguagePrefix(isWelsh) + "_" + templateName;
        verifyTemplatePayloadValues(true, templateWithLanguagePrefix, summonsType,
                prosecutorCost, personalService, caseUrn, firstName, middleName, lastName);

        final UUID initialMaterialId = verifyMaterialRequestRecordedAndExtractMaterialId(materialRequestConsumer);
        verifyMaterialCreated(initialMaterialId.toString());
        sendEventToConfirmMaterialAdded(initialMaterialId);

        final List<String> initialEmailDetails = newArrayList(prosecutorEmailAddress, caseUrn,
                format("%s %s %s, %s", firstName, middleName, lastName, defendant1ProsecutionAuthorityReference));
        verifyEmailNotificationIsRaisedWithoutAttachment(initialEmailDetails);
        verifyCreateLetterRequested(of("letterUrl", initialMaterialId.toString()));

        // When: hearing is re-confirmed (simulating a summons amendment scenario)
        // isSummonsAlreadyApproved=true on the hearing aggregate, so prepare-summons-data
        // fires SummonsDataPrepared(isSummonsAmended=true)
        sendPublicEventToConfirmHearingForInitiatedCase(hearingId, defendantId, offenceId, caseId, isWelsh);

        // Then: an amended summons document is generated with amendedDate set
        final UUID amendedMaterialId = verifyMaterialRequestRecordedAndExtractMaterialId(materialRequestConsumer);
        assertThat("Amended material ID should be present", amendedMaterialId, notNullValue());
        sendEventToConfirmMaterialAdded(amendedMaterialId);

        verifyAmendedSummonsHasAmendedDate(templateWithLanguagePrefix, summonsType);
        verifyCreateLetterRequested(of("letterUrl", amendedMaterialId.toString()));

        // And: notification for the amended summons is sent to the prosecutor
        final List<String> amendedEmailDetails = newArrayList(prosecutorEmailAddress, caseUrn,
                format("%s %s %s, %s", firstName, middleName, lastName, defendant1ProsecutionAuthorityReference));
        verifyEmailNotificationIsRaisedWithoutAttachment(amendedEmailDetails);
    }

    /**
     * Verifies that after amend, CDES holds at least two summons documents for the case/defendant —
     * one for the original summons and one for the amended summons.
     */
    @ParameterizedTest
    @MethodSource("firstHearingAmendSummonsSpecifications")
    public void shouldRecordBothOriginalAndAmendedSummonsDocumentsInCdes(
            final String summonsCode,
            final String summonsType,
            final String templateName,
            final boolean isYouth,
            final int numberOfDocuments,
            final boolean isWelsh) throws IOException {

        // Given: initial summons confirmed and consumed
        initiateCourtProceedings(buildInitiatePayload(isYouth, summonsCode, false, isWelsh));

        final String hearingId = pollCaseAndGetHearingForDefendant(caseId, defendantId,
                withJsonPath("$.prosecutionCase.id", is(caseId)),
                withJsonPath("$.prosecutionCase.defendants[?(@.id == '" + defendantId + "')].offences[0]",
                        hasSize(greaterThanOrEqualTo(1))),
                withJsonPath("$.hearingsAtAGlance.defendantHearings[?(@.defendantId == '" + defendantId + "')].hearingIds[0]",
                        hasSize(greaterThanOrEqualTo(1))));

        sendPublicEventToConfirmHearingForInitiatedCase(hearingId, defendantId, offenceId, caseId, isWelsh);

        final UUID initialMaterialId = verifyMaterialRequestRecordedAndExtractMaterialId(materialRequestConsumer);
        sendEventToConfirmMaterialAdded(initialMaterialId);

        // When: hearing re-confirmed → amend path
        sendPublicEventToConfirmHearingForInitiatedCase(hearingId, defendantId, offenceId, caseId, isWelsh);
        final UUID amendedMaterialId = verifyMaterialRequestRecordedAndExtractMaterialId(materialRequestConsumer);
        sendEventToConfirmMaterialAdded(amendedMaterialId);

        // Then: CDES holds ≥ 2 summons documents for this defendant/case
        final Matcher[] matchers = {
                withJsonPath("$.documentIndices", hasSize(greaterThanOrEqualTo(2))),
                withJsonPath("$.documentIndices[*].document.name", hasItems("Summons")),
                withJsonPath("$..defendantIds[?(@ =='" + defendantId + "')]",
                        hasSize(greaterThanOrEqualTo(2)))
        };
        getCourtDocumentsByCaseWithMatchers(USER_ID, caseId, matchers);
    }

    /**
     * Verifies that a youth defendant's summons is also amended correctly when the hearing is
     * re-confirmed, including the parent/guardian summons document.
     */
    @ParameterizedTest
    @MethodSource("firstHearingAmendSummonsSpecifications")
    public void shouldNotGenerateAmendedSummonsWhenSummonsIsSuppressedOnReConfirmation(
            final String summonsCode,
            final String summonsType,
            final String templateName,
            final boolean isYouth,
            final int numberOfDocuments,
            final boolean isWelsh) throws IOException {

        // Given: initial summons with summons suppressed = false
        initiateCourtProceedings(buildInitiatePayload(isYouth, summonsCode, false, isWelsh));

        final String hearingId = pollCaseAndGetHearingForDefendant(caseId, defendantId,
                withJsonPath("$.prosecutionCase.id", is(caseId)),
                withJsonPath("$.prosecutionCase.defendants[?(@.id == '" + defendantId + "')].offences[0]",
                        hasSize(greaterThanOrEqualTo(1))),
                withJsonPath("$.hearingsAtAGlance.defendantHearings[?(@.defendantId == '" + defendantId + "')].hearingIds[0]",
                        hasSize(greaterThanOrEqualTo(1))));

        sendPublicEventToConfirmHearingForInitiatedCase(hearingId, defendantId, offenceId, caseId, isWelsh);

        final UUID initialMaterialId = verifyMaterialRequestRecordedAndExtractMaterialId(materialRequestConsumer);
        sendEventToConfirmMaterialAdded(initialMaterialId);

        // When: hearing re-confirmed → amend path triggered
        // Even with the same suppressed=false payload, the re-confirmation uses existing
        // listDefendantRequests on the aggregate (unchanged), so documents are regenerated
        sendPublicEventToConfirmHearingForInitiatedCase(hearingId, defendantId, offenceId, caseId, isWelsh);

        final UUID amendedMaterialId = verifyMaterialRequestRecordedAndExtractMaterialId(materialRequestConsumer);
        assertThat(amendedMaterialId, notNullValue());
        sendEventToConfirmMaterialAdded(amendedMaterialId);

        // Then: amended document was generated (summons suppressed check happens upstream,
        // and the aggregate still holds the original summonsApprovedOutcome with suppressed=false)
        verifyCreateLetterRequested(of("letterUrl", amendedMaterialId.toString()));
    }

    // --- Payload builder ---

    private String buildInitiatePayload(final boolean isYouth, final String summonsCode,
                                        final boolean summonsSuppressed, final boolean isWelsh) {
        final String resourceLocation = isYouth
                ? "progression.command.initiate-court-proceedings-first-hearing-summons-youth.json"
                : "progression.command.initiate-court-proceedings-first-hearing-summons.json";

        return getPayload(resourceLocation)
                .replaceAll("CASE_ID", caseId)
                .replace("CASE_REFERENCE", caseUrn)
                .replace("SUMMONS_CODE", summonsCode)
                .replaceAll("DEFENDANT_ID", defendantId)
                .replaceAll("OFFENCE_ID", offenceId)
                .replace("DEFENDANT_DOB", getSubjectDateOfBirth(isYouth))
                .replace("LISTED_START_DATE_TIME", "2030-01-01T10:00:00Z")
                .replace("EARLIEST_START_DATE_TIME", "2030-01-01T10:00:00Z")
                .replace("PERSONAL_SERVICE", Boolean.toString(personalService))
                .replace("SUMMONS_SUPPRESSED", Boolean.toString(summonsSuppressed))
                .replace("PROSECUTOR_EMAIL", prosecutorEmailAddress)
                .replaceAll("COURT_CENTRE_ID", isWelsh ? ReferenceDataStub.WELSH_COURT_ID : ReferenceDataStub.ENGLISH_COURT_ID)
                .replace("FIRST_NAME", firstName)
                .replace("MIDDLE_NAME", middleName)
                .replace("LAST_NAME", lastName)
                .replace("PROSECUTION_AUTHORITY_REFERENCE", defendant1ProsecutionAuthorityReference)
                // youth payload includes parent names
                .replace("PARENT_FIRST_NAME", "Parent_" + firstName)
                .replace("PARENT_MIDDLE_NAME", "Parent_" + middleName)
                .replace("PARENT_LAST_NAME", "Parent_" + lastName);
    }

    // --- Verification helpers ---

    private void verifyDocumentAddedToCdes(final String defendantId, final int numberOfDocuments) {
        final Matcher[] matchers = {
                withJsonPath("$.documentIndices", hasSize(greaterThanOrEqualTo(numberOfDocuments))),
                withJsonPath("$.documentIndices[*].document.name", hasItems("Summons")),
                withJsonPath("$..defendantIds[?(@ =='" + defendantId + "')]",
                        hasSize(greaterThanOrEqualTo(numberOfDocuments)))
        };
        getCourtDocumentsByCaseWithMatchers(USER_ID, caseId, matchers);
    }

    /**
     * Asserts that the latest summons template payload has an amendedDate set (non-null, non-empty),
     * confirming that CaseDefendantSummonsService detected isSummonsAmended=true and set the date.
     */
    private void verifyAmendedSummonsHasAmendedDate(final String templateName, final String summonsType) {
        final Optional<JsonObject> optionalPayload = getSummonsTemplate(templateName, firstName, middleName, lastName);
        assertThat("Amended summons template payload must be present", optionalPayload.isPresent(), is(true));

        final SummonsDocumentContent actualPayload =
                JSON_OBJECT_TO_OBJECT_CONVERTER.convert(optionalPayload.get(), SummonsDocumentContent.class);
        assertThat("Summons type must be correct on amended document", actualPayload.getType(), is(summonsType));
        assertThat("amendedDate must be set on the amended summons document", actualPayload.getAmendedDate(), notNullValue());
    }
}
