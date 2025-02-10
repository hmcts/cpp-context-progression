package uk.gov.moj.cpp.progression.applications;

import static com.google.common.collect.Lists.newArrayList;
import static com.jayway.jsonpath.matchers.JsonPathMatchers.isJson;
import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static java.util.UUID.randomUUID;
import static javax.json.Json.createArrayBuilder;
import static javax.json.Json.createObjectBuilder;
import static org.apache.commons.lang3.RandomStringUtils.randomAlphabetic;
import static org.apache.commons.lang3.RandomStringUtils.randomAlphanumeric;
import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.skyscreamer.jsonassert.JSONAssert.assertEquals;
import static org.skyscreamer.jsonassert.JSONCompareMode.STRICT;
import static uk.gov.justice.services.integrationtest.utils.jms.JmsMessageConsumerClientProvider.newPrivateJmsMessageConsumerClientProvider;
import static uk.gov.justice.services.integrationtest.utils.jms.JmsMessageConsumerClientProvider.newPublicJmsMessageConsumerClientProvider;
import static uk.gov.justice.services.integrationtest.utils.jms.JmsMessageProducerClientProvider.newPublicJmsMessageProducerClientProvider;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.justice.services.test.utils.core.random.RandomGenerator.BOOLEAN;
import static uk.gov.justice.services.test.utils.core.random.RandomGenerator.integer;
import static uk.gov.moj.cpp.progression.applications.SummonsResultUtil.getSummonsApprovedResult;
import static uk.gov.moj.cpp.progression.applications.SummonsResultUtil.getSummonsRejectedResult;
import static uk.gov.moj.cpp.progression.applications.applicationHelper.ApplicationHelper.initiateCourtProceedingsForCourtApplication;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.addProsecutionCaseToCrownCourt;
import static uk.gov.moj.cpp.progression.helper.QueueUtil.buildMetadata;
import static uk.gov.moj.cpp.progression.helper.QueueUtil.retrieveMessageAsJsonPath;
import static uk.gov.moj.cpp.progression.helper.QueueUtil.retrieveMessageBody;
import static uk.gov.moj.cpp.progression.helper.RestHelper.pollForResponse;
import static uk.gov.moj.cpp.progression.it.framework.ContextNameProvider.CONTEXT_NAME;
import static uk.gov.moj.cpp.progression.stub.HearingStub.stubInitiateHearing;
import static uk.gov.moj.cpp.progression.stub.NotificationServiceStub.verifyEmailNotificationIsRaisedWithoutAttachment;
import static uk.gov.moj.cpp.progression.util.FileUtil.getPayload;
import static uk.gov.moj.cpp.progression.util.ReferBoxWorkApplicationHelper.getPostBoxWorkApplicationReferredHearing;

import uk.gov.justice.services.common.converter.StringToJsonObjectConverter;
import uk.gov.justice.services.common.converter.ZonedDateTimes;
import uk.gov.justice.services.integrationtest.utils.jms.JmsMessageConsumerClient;
import uk.gov.justice.services.integrationtest.utils.jms.JmsMessageProducerClient;
import uk.gov.justice.services.integrationtest.utils.jms.JmsResourceManagementExtension;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.progression.stub.IdMapperStub;
import uk.gov.moj.cpp.progression.stub.NotificationServiceStub;

import java.util.List;
import java.util.Optional;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.JsonString;

import com.jayway.jsonpath.ReadContext;
import io.restassured.path.json.JsonPath;
import org.hamcrest.CoreMatchers;
import org.hamcrest.Matcher;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.skyscreamer.jsonassert.Customization;
import org.skyscreamer.jsonassert.comparator.CustomComparator;

@ExtendWith(JmsResourceManagementExtension.class)
public class GenericSummonsApplicationIT {

    private static final JmsMessageProducerClient messageProducerClientPublic = newPublicJmsMessageProducerClientProvider().getMessageProducerClient();
    private static final String INITIATE_COURT_HEARING_AFTER_SUMMONS_APPROVED = "progression.event.initiate-court-hearing-after-summons-approved";
    private static final String PUBLIC_PROGRESSION_BOXWORK_APPLICATION_REFERRED = "public.progression.boxwork-application-referred";
    private static final String PUBLIC_PROGRESSION_COURT_APPLICATION_SUMMONS_APPROVED = "public.progression.court-application-summons-approved";
    private static final String PUBLIC_PROGRESSION_COURT_APPLICATION_SUMMONS_REJECTED = "public.progression.court-application-summons-rejected";
    private static final String PUBLIC_HEARING_RESULTED_V2 = "public.events.hearing.hearing-resulted";

    private static final String PROSECUTOR_EMAIL_ADDRESS = randomAlphanumeric(20) + "@random.com";

    private static final JmsMessageConsumerClient messageConsumerClientPublicSummonsApproved = newPublicJmsMessageConsumerClientProvider().withEventNames(PUBLIC_PROGRESSION_COURT_APPLICATION_SUMMONS_APPROVED).getMessageConsumerClient();
    private static final JmsMessageConsumerClient messageConsumerClientPublicSummonsRejected = newPublicJmsMessageConsumerClientProvider().withEventNames(PUBLIC_PROGRESSION_COURT_APPLICATION_SUMMONS_REJECTED).getMessageConsumerClient();
    private static final JmsMessageConsumerClient messageConsumerClientPublicForReferBoxWorkApplicationOnHearingInitiated = newPublicJmsMessageConsumerClientProvider().withEventNames(PUBLIC_PROGRESSION_BOXWORK_APPLICATION_REFERRED).getMessageConsumerClient();
    private static final JmsMessageConsumerClient consumerForInitiateCourtHearingAfterSummonsApproved = newPrivateJmsMessageConsumerClientProvider(CONTEXT_NAME).withEventNames(INITIATE_COURT_HEARING_AFTER_SUMMONS_APPROVED).getMessageConsumerClient();

    private String rejectionReason;
    private String prosecutionCost;
    private boolean personalService;
    private boolean summonsSuppressed;

    @BeforeAll
    public static void setUpClass() {
        stubInitiateHearing();
        IdMapperStub.setUp();
        NotificationServiceStub.setUp();
    }

    @BeforeEach
    public void setUp() {
        rejectionReason = randomAlphabetic(20);
        prosecutionCost = "£" + integer(100);
        personalService = BOOLEAN.next();
        summonsSuppressed = BOOLEAN.next();
    }


    @Test
    public void shouldInitiateCourtHearingAfterSummonsApprovedPublicHearingResultedV2() throws Exception {
        final String userId = randomUUID().toString();
        final String caseId = randomUUID().toString();
        final String applicationId = randomUUID().toString();
        final String defendantId = randomUUID().toString();

        addProsecutionCaseToCrownCourt(caseId, defendantId);

        initiateCourtProceedingsForCourtApplication(applicationId, caseId, "applications/progression.initiate-court-proceedings-for-summons-linked-application.json");

        final JsonObject hearing = getHearingInMessagingQueueForBoxWorkReferred();

        final String hearingId = hearing.getString("id");

        pollForResponse("/hearingSearch/" + hearingId, "application/vnd.progression.query.hearing+json", withJsonPath("$.hearing.id", Matchers.is(hearingId)));

        final String requestOfHearing = getPostBoxWorkApplicationReferredHearing(applicationId);

        final String expectedOfHearing = getPayload("expected/expected.summons.hearing.initiate.json")
                .replaceAll("HEARING_ID", hearingId)
                .replaceAll("CASE_ID", caseId);

        assertEquals(expectedOfHearing, requestOfHearing, getCustomComparator(applicationId));

        final String summonsResults = getSummonsApprovedResult(personalService, summonsSuppressed, PROSECUTOR_EMAIL_ADDRESS);

        final JsonObject summonResultJsonObject = new StringToJsonObjectConverter().convert(summonsResults);

        final JsonObject publicHearingResultedJsonObject = createPublicHearingResultedV2(hearing, summonResultJsonObject);

        final JsonEnvelope publicEventEnvelope = envelopeFrom(buildMetadata(PUBLIC_HEARING_RESULTED_V2, userId), publicHearingResultedJsonObject);
        messageProducerClientPublic.sendMessage(PUBLIC_HEARING_RESULTED_V2, publicEventEnvelope);

        final String newHearingId = getNewHearingId(applicationId);

        verifyCourtHearingInitiate(newHearingId);

        final Matcher<ReadContext> applicationMatcher = allOf(withJsonPath("$.applicationId", is(applicationId)),
                withJsonPath("$.applicationDetails.aagResults[0].label", is("Summons approved")));
        verifyCourtApplicationViewStoreUpdated(applicationId, applicationMatcher);
    }

    @Test
    public void shouldNotInitiateCourtHearingAfterSummonsRejectedV2() throws Exception {
        final String userId = randomUUID().toString();
        final String caseId = randomUUID().toString();
        final String applicationId = randomUUID().toString();
        final String defendantId = randomUUID().toString();

        addProsecutionCaseToCrownCourt(caseId, defendantId);

        initiateCourtProceedingsForCourtApplication(applicationId, caseId, "applications/progression.initiate-court-proceedings-for-summons-linked-application.json");

        final JsonObject hearing = getHearingInMessagingQueueForBoxWorkReferred();

        final String hearingId = hearing.getString("id");

        pollForResponse("/hearingSearch/" + hearingId, "application/vnd.progression.query.hearing+json", withJsonPath("$.hearing.id", Matchers.is(hearingId)));

        final String requestOfHearing = getPostBoxWorkApplicationReferredHearing(applicationId);

        final String expectedOfHearing = getPayload("expected/expected.summons.hearing.initiate.json")
                .replaceAll("HEARING_ID", hearingId)
                .replaceAll("CASE_ID", caseId);

        assertEquals(expectedOfHearing, requestOfHearing, getCustomComparator(applicationId));

        final String summonsRejectedResults = getSummonsRejectedResult(rejectionReason, PROSECUTOR_EMAIL_ADDRESS);

        final JsonObject summonResultJsonObject = new StringToJsonObjectConverter().convert(summonsRejectedResults);

        final JsonObject publicHearingResultedJsonObject = createPublicHearingResultedV2(hearing, summonResultJsonObject);

        final JsonEnvelope publicEventEnvelope = envelopeFrom(buildMetadata(PUBLIC_HEARING_RESULTED_V2, userId), publicHearingResultedJsonObject);
        messageProducerClientPublic.sendMessage(PUBLIC_HEARING_RESULTED_V2, publicEventEnvelope);

        final Matcher<ReadContext> applicationMatcher = allOf(withJsonPath("$.applicationId", is(applicationId)),
                withJsonPath("$.applicationDetails.aagResults[0].label", is("Summons rejected")));
        verifyCourtApplicationViewStoreUpdated(applicationId, applicationMatcher);

        final List<String> expectedEmailDetails = newArrayList(PROSECUTOR_EMAIL_ADDRESS, "Robert12 Smith12, randomreference123", rejectionReason);
        verifyEmailNotificationIsRaisedWithoutAttachment(expectedEmailDetails);
    }

    @Test
    public void shouldRaisePublicSummonsApprovedV2() throws Exception {
        final String userId = randomUUID().toString();
        final String caseId = randomUUID().toString();
        final String applicationId = randomUUID().toString();
        final String defendantId = randomUUID().toString();

        addProsecutionCaseToCrownCourt(caseId, defendantId);

        initiateCourtProceedingsForCourtApplication(applicationId, caseId, "applications/progression.initiate-court-proceedings-for-first-hearing-summons-linked-application.json");

        final JsonObject hearing = getHearingInMessagingQueueForBoxWorkReferred();

        final String hearingId = hearing.getString("id");

        pollForResponse("/hearingSearch/" + hearingId, "application/vnd.progression.query.hearing+json", withJsonPath("$.hearing.id", is(hearingId)));

        final String requestOfHearing = getPostBoxWorkApplicationReferredHearing(applicationId);

        final String expectedOfHearing = getPayload("expected/expected.first-summons.hearing.initiate.json")
                .replaceAll("HEARING_ID", hearingId)
                .replaceAll("CASE_ID", caseId);

        assertEquals(expectedOfHearing, requestOfHearing, getCustomComparator(applicationId));

        final String summonsResults = getSummonsApprovedResult(prosecutionCost, personalService, summonsSuppressed, PROSECUTOR_EMAIL_ADDRESS);

        final JsonObject summonResultJsonObject = new StringToJsonObjectConverter().convert(summonsResults);

        final JsonObject publicHearingResultedJsonObject = createPublicHearingResultedV2(hearing, summonResultJsonObject);

        final JsonEnvelope publicEventEnvelope = envelopeFrom(buildMetadata(PUBLIC_HEARING_RESULTED_V2, userId), publicHearingResultedJsonObject);
        messageProducerClientPublic.sendMessage(PUBLIC_HEARING_RESULTED_V2, publicEventEnvelope);

        verifyCourtApplicationSummonsApprovedPublicEvent(applicationId, caseId);
    }

    @Test
    public void shouldRaisePublicSummonsRejectedV2() throws Exception {
        final String userId = randomUUID().toString();
        final String caseId = randomUUID().toString();
        final String applicationId = randomUUID().toString();
        final String defendantId = randomUUID().toString();

        addProsecutionCaseToCrownCourt(caseId, defendantId);

        initiateCourtProceedingsForCourtApplication(applicationId, caseId, "applications/progression.initiate-court-proceedings-for-first-hearing-summons-linked-application.json");

        final JsonObject hearing = getHearingInMessagingQueueForBoxWorkReferred();

        final String hearingId = hearing.getString("id");

        pollForResponse("/hearingSearch/" + hearingId, "application/vnd.progression.query.hearing+json", withJsonPath("$.hearing.id", Matchers.is(hearingId)));

        final String requestOfHearing = getPostBoxWorkApplicationReferredHearing(applicationId);

        final String expectedOfHearing = getPayload("expected/expected.first-summons.hearing.initiate.json")
                .replaceAll("HEARING_ID", hearingId)
                .replaceAll("CASE_ID", caseId);

        assertEquals(expectedOfHearing, requestOfHearing, getCustomComparator(applicationId));

        final String summonsRejectedResults = getSummonsRejectedResult(rejectionReason, PROSECUTOR_EMAIL_ADDRESS);

        final JsonObject summonResultJsonObject = new StringToJsonObjectConverter().convert(summonsRejectedResults);

        final JsonObject publicHearingResultedJsonObject = createPublicHearingResultedV2(hearing, summonResultJsonObject);

        final JsonEnvelope publicEventEnvelope = envelopeFrom(buildMetadata(PUBLIC_HEARING_RESULTED_V2, userId), publicHearingResultedJsonObject);
        messageProducerClientPublic.sendMessage(PUBLIC_HEARING_RESULTED_V2, publicEventEnvelope);

        verifyCourtApplicationSummonsRejectedPublicEvent(applicationId);

        final List<String> expectedEmailDetails = newArrayList(PROSECUTOR_EMAIL_ADDRESS, "Robert12 Smith12, randomreference234", "Robert13 Smith13, randomreference345", rejectionReason);
        verifyEmailNotificationIsRaisedWithoutAttachment(expectedEmailDetails);
    }

    private JsonObject createPublicHearingResultedV2(final JsonObject hearing, final JsonObject summonResultJsonObject) {
        final JsonArray courtApplicationsArray = hearing.getJsonArray("courtApplications");
        final JsonObject courtApplication = courtApplicationsArray.getJsonObject(0);
        final JsonString sittingDay = hearing.getJsonArray("hearingDays").getJsonObject(0).getJsonString("sittingDay");
        final String hearingDay = ZonedDateTimes.fromJsonString(sittingDay).toLocalDate().toString();
        return Json.createObjectBuilder()
                .add("isReshare", true)
                .add("hearingDay", hearingDay)
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
                                        .add("judicialResults", createArrayBuilder().add(summonResultJsonObject)))))
                .add("sharedTime", "2021-02-03T19:37:24Z")
                .build();
    }

    private void verifyCourtHearingInitiate(String newHearingId) {
        pollForResponse("/hearingSearch/" + newHearingId, "application/vnd.progression.query.hearing+json", withJsonPath("$.hearing.id", Matchers.is(newHearingId)));
    }

    private String getNewHearingId(final String applicationId) {
        final JsonPath message = retrieveMessageAsJsonPath(consumerForInitiateCourtHearingAfterSummonsApproved, isJson(Matchers.allOf(
                        withJsonPath("$.application.id", CoreMatchers.is(applicationId))
                )
        ));
        assertThat(message, notNullValue());
        return message.getJsonObject("courtHearing.id");
    }

    public JsonObject getHearingInMessagingQueueForBoxWorkReferred() {
        final Optional<JsonObject> message = retrieveMessageBody(messageConsumerClientPublicForReferBoxWorkApplicationOnHearingInitiated);
        assertTrue(message.isPresent());
        return message.get().getJsonObject("hearing");
    }

    public void verifyCourtApplicationSummonsApprovedPublicEvent(final String applicationId, final String caseId) {
        final Optional<JsonObject> message = retrieveMessageBody(messageConsumerClientPublicSummonsApproved);
        assertTrue(message.isPresent());
        assertThat(message.get().getString("id"), is(applicationId));
        assertThat(message.get().getString("prosecutionCaseId"), is(caseId));
        final JsonObject summonsApprovedOutcome = message.get().getJsonObject("summonsApprovedOutcome");
        assertThat(summonsApprovedOutcome.getString("prosecutorEmailAddress"), is(PROSECUTOR_EMAIL_ADDRESS));
        assertThat(summonsApprovedOutcome.getString("prosecutorCost"), is(prosecutionCost));
        assertThat(summonsApprovedOutcome.getBoolean("personalService"), is(personalService));
        assertThat(summonsApprovedOutcome.getBoolean("summonsSuppressed"), is(summonsSuppressed));
    }

    public void verifyCourtApplicationSummonsRejectedPublicEvent(String applicationId) {
        final Optional<JsonObject> message = retrieveMessageBody(messageConsumerClientPublicSummonsRejected);
        assertTrue(message.isPresent());
        assertThat(message.get().getString("id"), is(applicationId));
        assertThat(message.get().getJsonObject("summonsRejectedOutcome").getJsonArray("reasons").getString(0), is(rejectionReason));
        assertThat(message.get().getJsonObject("summonsRejectedOutcome").getString("prosecutorEmailAddress"), is(PROSECUTOR_EMAIL_ADDRESS));
    }

    private CustomComparator getCustomComparator(String applicationId) {
        return new CustomComparator(STRICT,
                new Customization("hearing.id", (o1, o2) -> o1 != null && o2 != null),
                new Customization("hearing.courtApplications[0].id", (o1, o2) -> applicationId.equals(o1))
        );
    }

    private void verifyCourtApplicationViewStoreUpdated(final String applicationId, final Matcher<ReadContext> matcher) {
        pollForResponse("/applications/" + applicationId,
                "application/vnd.progression.query.application.aaag+json",
                randomUUID().toString(),
                matcher);
    }
}
