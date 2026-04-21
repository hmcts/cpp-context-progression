package uk.gov.moj.cpp.progression.summons;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.isJson;
import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static java.lang.String.format;
import static java.util.Optional.ofNullable;
import static java.util.UUID.randomUUID;
import static javax.json.Json.createArrayBuilder;
import static javax.json.Json.createObjectBuilder;
import static org.apache.commons.lang3.RandomStringUtils.randomAlphanumeric;
import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static uk.gov.justice.services.common.converter.ZonedDateTimes.fromJsonString;
import static uk.gov.justice.services.integrationtest.utils.jms.JmsMessageConsumerClientProvider.newPrivateJmsMessageConsumerClientProvider;
import static uk.gov.justice.services.integrationtest.utils.jms.JmsMessageConsumerClientProvider.newPublicJmsMessageConsumerClientProvider;
import static uk.gov.justice.services.integrationtest.utils.jms.JmsMessageProducerClientProvider.newPublicJmsMessageProducerClientProvider;
import static uk.gov.justice.services.test.utils.core.random.RandomGenerator.BOOLEAN;
import static uk.gov.moj.cpp.progression.applications.SummonsResultUtil.getSummonsApprovedResult;
import static uk.gov.moj.cpp.progression.applications.SummonsResultUtil.getSummonsRejectedResult;
import static uk.gov.moj.cpp.progression.applications.applicationHelper.ApplicationHelper.initiateCourtProceedingsForCourtApplication;
import static uk.gov.moj.cpp.progression.helper.CaseHearingsQueryHelper.pollForHearing;
import static uk.gov.moj.cpp.progression.helper.MaterialHelper.sendEventToConfirmMaterialAdded;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.addProsecutionCaseToCrownCourt;
import static uk.gov.moj.cpp.progression.helper.QueueUtil.buildMetadata;
import static uk.gov.moj.cpp.progression.helper.QueueUtil.retrieveMessageAsJsonPath;
import static uk.gov.moj.cpp.progression.it.framework.ContextNameProvider.CONTEXT_NAME;
import static uk.gov.moj.cpp.progression.stub.NotificationServiceStub.verifyEmailNotificationIsRaisedWithoutAttachment;
import static uk.gov.moj.cpp.progression.stub.ReferenceDataStub.stubGetDocumentsTypeAccess;
import static uk.gov.moj.cpp.progression.summons.SummonsHelper.getSubjectDateOfBirth;
import static uk.gov.moj.cpp.progression.summons.SummonsHelper.verifyMaterialRequestRecordedAndExtractMaterialId;
import static uk.gov.moj.cpp.progression.util.FileUtil.getPayload;

import uk.gov.justice.core.courts.Hearing;
import uk.gov.justice.core.courts.JudicialResult;
import uk.gov.justice.core.courts.SummonsTemplateType;
import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.common.converter.ObjectToJsonObjectConverter;
import uk.gov.justice.services.common.converter.StringToJsonObjectConverter;
import uk.gov.justice.services.common.converter.jackson.ObjectMapperProducer;
import uk.gov.justice.services.integrationtest.utils.jms.JmsMessageConsumerClient;
import uk.gov.justice.services.integrationtest.utils.jms.JmsMessageProducerClient;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.progression.AbstractIT;
import uk.gov.moj.cpp.progression.stub.ReferenceDataStub;

import java.io.IOException;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonString;

import io.restassured.path.json.JsonPath;
import org.json.JSONException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * Integration tests for AC1: First hearing summons approved and later amended to Summons rejected.
 *
 * Flow under test (CHD-HEARING_SUMMONS):
 *   1. First hearing application initiated in boxwork via SPI IN / CPPI / MCC route
 *   2. Court user results the boxwork hearing with Summons Approved (SA) and shares — this creates
 *      a summons document and lists a First Hearing
 *   3. Court user goes back to the same boxwork hearing, amends SA to Summons Rejected (SR)
 *      and re-shares the hearing
 *   4. System must notify the prosecutor of the latest boxwork hearing outcome via email
 */
public class SummonsApprovedAmendedToRejectedIT extends AbstractIT {

    private static final String PUBLIC_HEARING_RESULTED_V2 = "public.events.hearing.hearing-resulted";
    private static final String INITIATE_COURT_HEARING_AFTER_SUMMONS_APPROVED = "progression.event.initiate-court-hearing-after-summons-approved";
    private static final String PUBLIC_PROGRESSION_BOXWORK_APPLICATION_REFERRED = "public.progression.boxwork-application-referred";
    private static final String PUBLIC_HEARING_CONFIRMED = "public.listing.hearing-confirmed";
    private static final String PRIVATE_EVENT_NOWS_MATERIAL_REQUEST_RECORDED = "progression.event.nows-material-request-recorded";

    private static final String INITIAL_PROSECUTOR_COST = "£245.56";
    private static final String REJECTION_REASON = "Summons issued in error";

    private static final JsonObjectToObjectConverter JSON_OBJECT_TO_OBJECT_CONVERTER =
            new JsonObjectToObjectConverter(new ObjectMapperProducer().objectMapper());
    private static final ObjectToJsonObjectConverter OBJECT_TO_JSON_OBJECT_CONVERTER =
            new ObjectToJsonObjectConverter(new ObjectMapperProducer().objectMapper());
    private static final StringToJsonObjectConverter STRING_TO_JSON_OBJECT_CONVERTER =
            new StringToJsonObjectConverter();

    private final JmsMessageProducerClient messageProducerClientPublic =
            newPublicJmsMessageProducerClientProvider().getMessageProducerClient();

    private String caseId;
    private String applicationId;
    private String defendantId;
    private boolean personalService;
    private String prosecutorEmailAddress;

    private String firstName;
    private String middleName;
    private String lastName;

    public static Stream<Arguments> summonsApprovedAmendedToRejectedParameters() {
        return Stream.of(
                // summonsTemplateType, isWelsh
                Arguments.of(SummonsTemplateType.BREACH, false),
                Arguments.of(SummonsTemplateType.BREACH, true)
        );
    }

    @BeforeEach
    public void setUp() {
        stubGetDocumentsTypeAccess("/restResource/get-all-document-type-access.json");

        caseId = randomUUID().toString();
        applicationId = randomUUID().toString();
        defendantId = randomUUID().toString();
        personalService = BOOLEAN.next();
        prosecutorEmailAddress = randomAlphanumeric(20) + "@prosecutor.com";

        firstName = "F_" + randomAlphanumeric(8);
        middleName = "M_" + randomAlphanumeric(8);
        lastName = "L_" + randomAlphanumeric(8);
    }

    /**
     * AC1: Given summons approved in boxwork (creating summons + First Hearing),
     * when the court user amends SA to SR and re-shares,
     * then the prosecutor must receive a rejection email notification.
     */
    @ParameterizedTest
    @MethodSource("summonsApprovedAmendedToRejectedParameters")
    public void shouldNotifyProsecutorWhenSummonsApprovedIsAmendedToRejected(
            final SummonsTemplateType summonsTemplateType,
            final boolean isWelsh) throws IOException, JSONException {

        final JmsMessageConsumerClient materialRequestConsumer =
                newPrivateJmsMessageConsumerClientProvider(CONTEXT_NAME)
                        .withEventNames(PRIVATE_EVENT_NOWS_MATERIAL_REQUEST_RECORDED)
                        .getMessageConsumerClient();

        // Given: application created in boxwork via SPI IN / CPPI / MCC route
        final Hearing boxworkHearing = givenApplicationInitiatedInBoxWork(summonsTemplateType, isWelsh);

        // And: court user results boxwork with Summons Approved — creates summons and lists First Hearing
        whenSummonsIsApprovedAndFirstHearingConfirmed(boxworkHearing, isWelsh);

        final UUID initialMaterialId = verifyMaterialRequestRecordedAndExtractMaterialId(materialRequestConsumer);
        sendEventToConfirmMaterialAdded(initialMaterialId);

        // When: court user amends SA result to SR and re-shares the boxwork hearing
        whenSummonsIsAmendedToRejected(boxworkHearing);

        // Then: system notifies the prosecutor of the rejection outcome
        final List<String> expectedEmailContent =
                List.of(prosecutorEmailAddress, format("%s %s %s", firstName, middleName, lastName));
        verifyEmailNotificationIsRaisedWithoutAttachment(expectedEmailContent);
    }

    /**
     * AC1 — edge case: re-sharing a boxwork hearing with SR when no prior approval exists
     * (straight rejection without a previous SA) must also notify the prosecutor.
     */
    @ParameterizedTest
    @MethodSource("summonsApprovedAmendedToRejectedParameters")
    public void shouldNotifyProsecutorWhenSummonsIsRejectedWithoutPriorApproval(
            final SummonsTemplateType summonsTemplateType,
            final boolean isWelsh) throws IOException, JSONException {

        // Given: application created in boxwork
        final Hearing boxworkHearing = givenApplicationInitiatedInBoxWork(summonsTemplateType, isWelsh);

        // When: court user results boxwork directly with SR (no prior SA)
        whenSummonsIsAmendedToRejected(boxworkHearing);

        // Then: system notifies the prosecutor of the rejection outcome
        final List<String> expectedEmailContent =
                List.of(prosecutorEmailAddress, format("%s %s %s", firstName, middleName, lastName));
        verifyEmailNotificationIsRaisedWithoutAttachment(expectedEmailContent);
    }

    // --- Setup orchestration ---

    private Hearing givenApplicationInitiatedInBoxWork(
            final SummonsTemplateType summonsTemplateType,
            final boolean isWelsh) throws IOException, JSONException {

        final JmsMessageConsumerClient boxworkReferredConsumer =
                newPublicJmsMessageConsumerClientProvider()
                        .withEventNames(PUBLIC_PROGRESSION_BOXWORK_APPLICATION_REFERRED)
                        .getMessageConsumerClient();

        addProsecutionCaseToCrownCourt(caseId, defendantId);
        initiateCourtProceedingsForCourtApplication(buildInitiateApplicationPayload(summonsTemplateType, isWelsh));

        final Hearing boxworkHearing = extractHearingFromBoxworkReferredEvent(boxworkReferredConsumer);
        pollForHearing(boxworkHearing.getId().toString(),
                withJsonPath("$.hearing.id", is(boxworkHearing.getId().toString())));
        return boxworkHearing;
    }

    private void whenSummonsIsApprovedAndFirstHearingConfirmed(
            final Hearing boxworkHearing, final boolean isWelsh) {

        final JmsMessageConsumerClient initiateCourtHearingConsumer =
                newPrivateJmsMessageConsumerClientProvider(CONTEXT_NAME)
                        .withEventNames(INITIATE_COURT_HEARING_AFTER_SUMMONS_APPROVED)
                        .getMessageConsumerClient();

        final JudicialResult summonsApprovedResult =
                buildSummonsApprovedJudicialResult(false, INITIAL_PROSECUTOR_COST, prosecutorEmailAddress);
        sendHearingResultedEvent(boxworkHearing, summonsApprovedResult);

        final String firstHearingId = extractNewHearingId(
                initiateCourtHearingConsumer, boxworkHearing.getCourtApplications().get(0).getId());
        pollForHearing(firstHearingId, withJsonPath("$.hearing.id", is(firstHearingId)));

        sendHearingConfirmedEvent(applicationId, firstHearingId, isWelsh);
    }

    private void whenSummonsIsAmendedToRejected(final Hearing boxworkHearing) {
        final JudicialResult summonsRejectedResult =
                buildSummonsRejectedJudicialResult(REJECTION_REASON, prosecutorEmailAddress);
        sendHearingResultedEvent(boxworkHearing, summonsRejectedResult);
    }

    // --- Event sending ---

    private void sendHearingResultedEvent(final Hearing boxworkHearing, final JudicialResult judicialResult) {
        final JsonObject hearingJson = OBJECT_TO_JSON_OBJECT_CONVERTER.convert(boxworkHearing);
        final JsonObject judicialResultJson = OBJECT_TO_JSON_OBJECT_CONVERTER.convert(judicialResult);
        final JsonObject payload = buildHearingResultedPayload(hearingJson, judicialResultJson);
        final JsonEnvelope envelope = JsonEnvelope.envelopeFrom(
                buildMetadata(PUBLIC_HEARING_RESULTED_V2, randomUUID()), payload);
        messageProducerClientPublic.sendMessage(PUBLIC_HEARING_RESULTED_V2, envelope);
    }

    private void sendHearingConfirmedEvent(final String applicationId, final String hearingId, final boolean isWelsh) {
        final String payloadStr = getPayload("public.listing.hearing-confirmed-applications-only.json")
                .replaceAll("APPLICATION_ID", applicationId)
                .replaceAll("COURT_CENTRE_ID", isWelsh ? ReferenceDataStub.WELSH_COURT_ID : ReferenceDataStub.ENGLISH_COURT_ID)
                .replaceAll("HEARING_ID", hearingId);
        final JsonObject payload = STRING_TO_JSON_OBJECT_CONVERTER.convert(payloadStr);
        final JsonEnvelope envelope = JsonEnvelope.envelopeFrom(
                buildMetadata(PUBLIC_HEARING_CONFIRMED, randomUUID()), payload);
        messageProducerClientPublic.sendMessage(PUBLIC_HEARING_CONFIRMED, envelope);
    }

    // --- Payload construction ---

    private JsonObject buildHearingResultedPayload(final JsonObject hearing, final JsonObject judicialResultJson) {
        final JsonObject courtApplication = hearing.getJsonArray("courtApplications").getJsonObject(0);
        final JsonString sittingDay = hearing.getJsonArray("hearingDays").getJsonObject(0).getJsonString("sittingDay");
        return Json.createObjectBuilder()
                .add("isReshare", true)
                .add("hearingDay", fromJsonString(sittingDay).toLocalDate().toString())
                .add("hearing", createObjectBuilder()
                        .add("courtCentre", hearing.getJsonObject("courtCentre"))
                        .add("hearingDays", hearing.getJsonArray("hearingDays"))
                        .add("type", hearing.getJsonObject("type"))
                        .add("id", hearing.getString("id"))
                        .add("isBoxHearing", hearing.getBoolean("isBoxHearing"))
                        .add("isVirtualBoxHearing", hearing.getBoolean("isVirtualBoxHearing"))
                        .add("jurisdictionType", hearing.getString("jurisdictionType"))
                        .add("courtApplications", createArrayBuilder()
                                .add(createObjectBuilder()
                                        .add("applicant", courtApplication.getJsonObject("applicant"))
                                        .add("courtApplicationCases", courtApplication.getJsonArray("courtApplicationCases"))
                                        .add("respondents", courtApplication.getJsonArray("respondents"))
                                        .add("subject", courtApplication.getJsonObject("subject"))
                                        .add("futureSummonsHearing", courtApplication.getJsonObject("futureSummonsHearing"))
                                        .add("thirdParties", courtApplication.getJsonArray("thirdParties"))
                                        .add("type", courtApplication.getJsonObject("type"))
                                        .add("allegationOrComplaintEndDate", courtApplication.getString("allegationOrComplaintEndDate"))
                                        .add("allegationOrComplaintStartDate", courtApplication.getString("allegationOrComplaintStartDate"))
                                        .add("applicationReceivedDate", courtApplication.getString("applicationReceivedDate"))
                                        .add("applicationReference", courtApplication.getString("applicationReference"))
                                        .add("applicationStatus", courtApplication.getString("applicationStatus"))
                                        .add("commissionerOfOath", courtApplication.getBoolean("commissionerOfOath"))
                                        .add("hasSummonsSupplied", courtApplication.getBoolean("hasSummonsSupplied"))
                                        .add("id", courtApplication.getString("id"))
                                        .add("judicialResults", createArrayBuilder().add(judicialResultJson)))))
                .add("sharedTime", "2021-02-03T19:37:24Z")
                .build();
    }

    private JudicialResult buildSummonsApprovedJudicialResult(
            final boolean summonsSuppressed,
            final String prosecutorCost,
            final String prosecutorEmail) {
        final String resultJson = getSummonsApprovedResult(prosecutorCost, personalService, summonsSuppressed, prosecutorEmail);
        return JSON_OBJECT_TO_OBJECT_CONVERTER.convert(
                STRING_TO_JSON_OBJECT_CONVERTER.convert(resultJson), JudicialResult.class);
    }

    private JudicialResult buildSummonsRejectedJudicialResult(
            final String rejectionReason,
            final String prosecutorEmail) {
        final String resultJson = getSummonsRejectedResult(rejectionReason, prosecutorEmail);
        return JSON_OBJECT_TO_OBJECT_CONVERTER.convert(
                STRING_TO_JSON_OBJECT_CONVERTER.convert(resultJson), JudicialResult.class);
    }

    private String buildInitiateApplicationPayload(
            final SummonsTemplateType summonsTemplateType,
            final boolean isWelsh) {
        return getPayload("applications/progression.initiate-court-proceedings-for-summons-generation.json")
                .replace("SUMMONS_TEMPLATE_TYPE", summonsTemplateType.toString())
                .replace("APPLICATION_ID", applicationId)
                .replace("CASE_ID", caseId)
                .replace("SUBJECT_DOB", getSubjectDateOfBirth(false))
                .replaceAll("COURT_CENTRE_ID", isWelsh ? ReferenceDataStub.WELSH_COURT_ID : ReferenceDataStub.ENGLISH_COURT_ID)
                .replace("FIRST_NAME", firstName)
                .replace("MIDDLE_NAME", middleName)
                .replace("LAST_NAME", lastName);
    }

    // --- Message extraction ---

    private Hearing extractHearingFromBoxworkReferredEvent(final JmsMessageConsumerClient consumer) {
        final JsonPath message = retrieveMessageAsJsonPath(consumer,
                isJson(allOf(withJsonPath("$.hearing.courtApplications[0].id", is(applicationId)))));
        assertThat(ofNullable(message).isPresent(), is(true));
        return JSON_OBJECT_TO_OBJECT_CONVERTER.convert(
                STRING_TO_JSON_OBJECT_CONVERTER.convert(message.prettify()).getJsonObject("hearing"),
                Hearing.class);
    }

    private String extractNewHearingId(final JmsMessageConsumerClient consumer, final UUID applicationId) {
        final JsonPath message = retrieveMessageAsJsonPath(consumer,
                isJson(allOf(withJsonPath("$.application.id", is(applicationId.toString())))));
        assertThat(ofNullable(message).isPresent(), is(true));
        return message.getJsonObject("courtHearing.id");
    }
}
