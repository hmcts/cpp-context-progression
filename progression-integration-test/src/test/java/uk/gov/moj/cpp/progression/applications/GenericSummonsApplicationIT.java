package uk.gov.moj.cpp.progression.applications;

import static com.google.common.collect.Lists.newArrayList;
import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static java.util.UUID.randomUUID;
import static javax.json.Json.createArrayBuilder;
import static javax.json.Json.createObjectBuilder;
import static javax.ws.rs.core.Response.Status.OK;
import static org.apache.commons.lang.RandomStringUtils.randomAlphabetic;
import static org.apache.commons.lang3.RandomStringUtils.randomAlphanumeric;
import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.skyscreamer.jsonassert.JSONAssert.assertEquals;
import static org.skyscreamer.jsonassert.JSONCompareMode.STRICT;
import static uk.gov.justice.services.common.http.HeaderConstants.USER_ID;
import static uk.gov.justice.services.messaging.JsonEnvelope.metadataBuilder;
import static uk.gov.justice.services.test.utils.core.http.RequestParamsBuilder.requestParams;
import static uk.gov.justice.services.test.utils.core.http.RestPoller.poll;
import static uk.gov.justice.services.test.utils.core.matchers.ResponsePayloadMatcher.payload;
import static uk.gov.justice.services.test.utils.core.matchers.ResponseStatusMatcher.status;
import static uk.gov.justice.services.test.utils.core.random.RandomGenerator.BOOLEAN;
import static uk.gov.justice.services.test.utils.core.random.RandomGenerator.integer;
import static uk.gov.moj.cpp.progression.applications.SummonsResultUtil.getSummonsApprovedResult;
import static uk.gov.moj.cpp.progression.applications.SummonsResultUtil.getSummonsRejectedResult;
import static uk.gov.moj.cpp.progression.applications.applicationHelper.ApplicationHelper.initiateCourtProceedingsForCourtApplication;
import static uk.gov.moj.cpp.progression.helper.AbstractTestHelper.getReadUrl;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.addProsecutionCaseToCrownCourt;
import static uk.gov.moj.cpp.progression.helper.QueueUtil.privateEvents;
import static uk.gov.moj.cpp.progression.helper.QueueUtil.publicEvents;
import static uk.gov.moj.cpp.progression.helper.QueueUtil.retrieveMessageAsJsonObject;
import static uk.gov.moj.cpp.progression.helper.QueueUtil.sendMessage;
import static uk.gov.moj.cpp.progression.helper.RestHelper.pollForResponse;
import static uk.gov.moj.cpp.progression.stub.HearingStub.stubInitiateHearing;
import static uk.gov.moj.cpp.progression.stub.ListingStub.verifyPostListCourtHearing;
import static uk.gov.moj.cpp.progression.stub.NotificationServiceStub.verifyEmailNotificationIsRaisedWithoutAttachment;
import static uk.gov.moj.cpp.progression.util.FeatureToggleUtil.enableAmendReshareFeature;
import static uk.gov.moj.cpp.progression.util.FileUtil.getPayload;
import static uk.gov.moj.cpp.progression.util.ReferBoxWorkApplicationHelper.getPostBoxWorkApplicationReferredHearing;

import uk.gov.justice.services.common.converter.StringToJsonObjectConverter;
import uk.gov.justice.services.common.converter.ZonedDateTimes;
import uk.gov.moj.cpp.progression.AbstractIT;
import uk.gov.moj.cpp.progression.stub.IdMapperStub;
import uk.gov.moj.cpp.progression.stub.NotificationServiceStub;

import java.util.List;
import java.util.Optional;

import javax.jms.MessageConsumer;
import javax.jms.MessageProducer;
import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.JsonString;

import com.jayway.jsonpath.ReadContext;
import org.hamcrest.Matcher;
import org.hamcrest.Matchers;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.skyscreamer.jsonassert.Customization;
import org.skyscreamer.jsonassert.comparator.CustomComparator;

public class GenericSummonsApplicationIT extends AbstractIT {

    private static final MessageProducer messageProducerClientPublic = publicEvents.createPublicProducer();
    private static final String COURT_APPLICATION_CREATED_PRIVATE_EVENT = "progression.event.court-application-created";
    private static final String INITIATE_COURT_HEARING_AFTER_SUMMONS_APPROVED = "progression.event.initiate-court-hearing-after-summons-approved";
    private static final String COURT_APPLICATION_SUMMONS_REJECTED = "progression.event.court-application-summons-rejected";
    private static final String PUBLIC_PROGRESSION_BOXWORK_APPLICATION_REFERRED = "public.progression.boxwork-application-referred";
    private static final String PUBLIC_PROGRESSION_COURT_APPLICATION_SUMMONS_APPROVED = "public.progression.court-application-summons-approved";
    private static final String PUBLIC_PROGRESSION_COURT_APPLICATION_SUMMONS_REJECTED = "public.progression.court-application-summons-rejected";
    private static final String PUBLIC_HEARING_RESULTED = "public.hearing.resulted";
    private static final String PUBLIC_HEARING_RESULTED_V2 = "public.events.hearing.hearing-resulted";

    private static final String PROSECUTOR_EMAIL_ADDRESS = randomAlphanumeric(20) + "@random.com";

    private MessageConsumer consumerForCourtApplicationCreated;
    private MessageConsumer messageConsumerClientPublicForReferBoxWorkApplicationOnHearingInitiated;
    private MessageConsumer consumerForInitiateCourtHearingAfterSummonsApproved;
    private MessageConsumer consumerForSummonsRejected;
    private MessageConsumer messageConsumerClientPublicSummonsApproved;
    private MessageConsumer messageConsumerClientPublicSummonsRejected;

    private String rejectionReason;
    private String prosecutionCost;
    private boolean personalService;
    private boolean summonsSuppressed;

    @Before
    public void setUp() {
        messageConsumerClientPublicSummonsApproved = publicEvents.createPublicConsumer(PUBLIC_PROGRESSION_COURT_APPLICATION_SUMMONS_APPROVED);
        messageConsumerClientPublicSummonsRejected = publicEvents.createPublicConsumer(PUBLIC_PROGRESSION_COURT_APPLICATION_SUMMONS_REJECTED);
        messageConsumerClientPublicForReferBoxWorkApplicationOnHearingInitiated = publicEvents.createPublicConsumer(PUBLIC_PROGRESSION_BOXWORK_APPLICATION_REFERRED);
        consumerForCourtApplicationCreated = privateEvents.createPrivateConsumer(COURT_APPLICATION_CREATED_PRIVATE_EVENT);
        consumerForInitiateCourtHearingAfterSummonsApproved = privateEvents.createPrivateConsumer(INITIATE_COURT_HEARING_AFTER_SUMMONS_APPROVED);
        consumerForSummonsRejected = privateEvents.createPrivateConsumer(COURT_APPLICATION_SUMMONS_REJECTED);
        stubInitiateHearing();
        IdMapperStub.setUp();
        NotificationServiceStub.setUp();

        rejectionReason = randomAlphabetic(20);
        prosecutionCost = "£" + integer(100);
        personalService = BOOLEAN.next();
        summonsSuppressed = BOOLEAN.next();
    }

    @After
    public void tearDown() throws Exception {
        consumerForCourtApplicationCreated.close();
        messageConsumerClientPublicForReferBoxWorkApplicationOnHearingInitiated.close();
        messageConsumerClientPublicSummonsApproved.close();
        messageConsumerClientPublicSummonsRejected.close();
        consumerForInitiateCourtHearingAfterSummonsApproved.close();
        consumerForSummonsRejected.close();
    }

    @Test
    public void shouldInitiateCourtHearingAfterSummonsApproved() throws Exception {
        enableAmendReshareFeature(false);

        final String userId = randomUUID().toString();
        final String caseId = randomUUID().toString();
        final String applicationId = randomUUID().toString();
        final String defendantId = randomUUID().toString();

        addProsecutionCaseToCrownCourt(caseId, defendantId);

        initiateCourtProceedingsForCourtApplication(applicationId, caseId, "applications/progression.initiate-court-proceedings-for-summons-linked-application.json");

        verifyCourtApplicationCreatedPrivateEvent();

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

        final JsonObject publicHearingResultedJsonObject = createPublicHearingResulted(hearing, summonResultJsonObject);

        sendMessage(messageProducerClientPublic, PUBLIC_HEARING_RESULTED, publicHearingResultedJsonObject,
                metadataBuilder()
                        .withId(randomUUID())
                        .withName(PUBLIC_HEARING_RESULTED)
                        .withUserId(userId)
                        .build());

        final String newHearingId = getNewHearingId();

        verifyCourtHearingInitiate(newHearingId);

        final Matcher<ReadContext> applicationMatcher = allOf(withJsonPath("$.applicationId", is(applicationId)),
                withJsonPath("$.applicationDetails.aagResults[0].label", is("Summons approved")));
        verifyCourtApplicationViewStoreUpdated(applicationId, applicationMatcher);
        verifyPostListCourtHearing(applicationId);
    }

    @Test
    public void shouldInitiateCourtHearingAfterSummonsApprovedPublicHearingResultedV2() throws Exception {
        enableAmendReshareFeature(true);

        final String userId = randomUUID().toString();
        final String caseId = randomUUID().toString();
        final String applicationId = randomUUID().toString();
        final String defendantId = randomUUID().toString();

        addProsecutionCaseToCrownCourt(caseId, defendantId);

        initiateCourtProceedingsForCourtApplication(applicationId, caseId, "applications/progression.initiate-court-proceedings-for-summons-linked-application.json");

        verifyCourtApplicationCreatedPrivateEvent();

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

        sendMessage(messageProducerClientPublic, PUBLIC_HEARING_RESULTED_V2, publicHearingResultedJsonObject,
                metadataBuilder()
                        .withId(randomUUID())
                        .withName(PUBLIC_HEARING_RESULTED_V2)
                        .withUserId(userId)
                        .build());

        final String newHearingId = getNewHearingId();

        verifyCourtHearingInitiate(newHearingId);

        final Matcher<ReadContext> applicationMatcher = allOf(withJsonPath("$.applicationId", is(applicationId)),
                withJsonPath("$.applicationDetails.aagResults[0].label", is("Summons approved")));
        verifyCourtApplicationViewStoreUpdated(applicationId, applicationMatcher);
    }

    @Test
    public void shouldNotInitiateCourtHearingAfterSummonsRejected() throws Exception {
        enableAmendReshareFeature(false);

        final String userId = randomUUID().toString();
        final String caseId = randomUUID().toString();
        final String applicationId = randomUUID().toString();
        final String defendantId = randomUUID().toString();

        addProsecutionCaseToCrownCourt(caseId, defendantId);

        initiateCourtProceedingsForCourtApplication(applicationId, caseId, "applications/progression.initiate-court-proceedings-for-summons-linked-application.json");

        verifyCourtApplicationCreatedPrivateEvent();

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

        final JsonObject publicHearingResultedJsonObject = createPublicHearingResulted(hearing, summonResultJsonObject);

        sendMessage(messageProducerClientPublic, PUBLIC_HEARING_RESULTED, publicHearingResultedJsonObject,
                metadataBuilder()
                        .withId(randomUUID())
                        .withName(PUBLIC_HEARING_RESULTED)
                        .withUserId(userId)
                        .build());

        verifySummonsRejected(applicationId);

        final Matcher<ReadContext> applicationMatcher = allOf(withJsonPath("$.applicationId", is(applicationId)),
                withJsonPath("$.applicationDetails.aagResults[0].label", is("Summons rejected")));
        verifyCourtApplicationViewStoreUpdated(applicationId, applicationMatcher);

        final List<String> expectedEmailDetails = newArrayList(PROSECUTOR_EMAIL_ADDRESS, "Robert12 Smith12, randomreference123", rejectionReason);
        verifyEmailNotificationIsRaisedWithoutAttachment(expectedEmailDetails);
    }

    @Test
    public void shouldNotInitiateCourtHearingAfterSummonsRejectedV2() throws Exception {
        enableAmendReshareFeature(true);

        final String userId = randomUUID().toString();
        final String caseId = randomUUID().toString();
        final String applicationId = randomUUID().toString();
        final String defendantId = randomUUID().toString();

        addProsecutionCaseToCrownCourt(caseId, defendantId);

        initiateCourtProceedingsForCourtApplication(applicationId, caseId, "applications/progression.initiate-court-proceedings-for-summons-linked-application.json");

        verifyCourtApplicationCreatedPrivateEvent();

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

        sendMessage(messageProducerClientPublic, PUBLIC_HEARING_RESULTED_V2, publicHearingResultedJsonObject,
                metadataBuilder()
                        .withId(randomUUID())
                        .withName(PUBLIC_HEARING_RESULTED_V2)
                        .withUserId(userId)
                        .build());


        verifySummonsRejected(applicationId);

        final Matcher<ReadContext> applicationMatcher = allOf(withJsonPath("$.applicationId", is(applicationId)),
                withJsonPath("$.applicationDetails.aagResults[0].label", is("Summons rejected")));
        verifyCourtApplicationViewStoreUpdated(applicationId, applicationMatcher);

        final List<String> expectedEmailDetails = newArrayList(PROSECUTOR_EMAIL_ADDRESS, "Robert12 Smith12, randomreference123", rejectionReason);
        verifyEmailNotificationIsRaisedWithoutAttachment(expectedEmailDetails);
    }

    @Test
    public void shouldRaisePublicSummonsApproved() throws Exception {
        enableAmendReshareFeature(false);

        final String userId = randomUUID().toString();
        final String caseId = randomUUID().toString();
        final String applicationId = randomUUID().toString();
        final String defendantId = randomUUID().toString();

        addProsecutionCaseToCrownCourt(caseId, defendantId);

        initiateCourtProceedingsForCourtApplication(applicationId, caseId, "applications/progression.initiate-court-proceedings-for-first-hearing-summons-linked-application.json");

        verifyCourtApplicationCreatedPrivateEvent();

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

        final JsonObject publicHearingResultedJsonObject = createPublicHearingResulted(hearing, summonResultJsonObject);

        sendMessage(messageProducerClientPublic, PUBLIC_HEARING_RESULTED, publicHearingResultedJsonObject,
                metadataBuilder()
                        .withId(randomUUID())
                        .withName(PUBLIC_HEARING_RESULTED)
                        .withUserId(userId)
                        .build());

        verifyCourtApplicationSummonsApprovedPublicEvent(applicationId, caseId);
    }

    @Test
    public void shouldRaisePublicSummonsApprovedV2() throws Exception {
        enableAmendReshareFeature(true);

        final String userId = randomUUID().toString();
        final String caseId = randomUUID().toString();
        final String applicationId = randomUUID().toString();
        final String defendantId = randomUUID().toString();

        addProsecutionCaseToCrownCourt(caseId, defendantId);

        initiateCourtProceedingsForCourtApplication(applicationId, caseId, "applications/progression.initiate-court-proceedings-for-first-hearing-summons-linked-application.json");

        verifyCourtApplicationCreatedPrivateEvent();

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

        sendMessage(messageProducerClientPublic, PUBLIC_HEARING_RESULTED_V2, publicHearingResultedJsonObject,
                metadataBuilder()
                        .withId(randomUUID())
                        .withName(PUBLIC_HEARING_RESULTED_V2)
                        .withUserId(userId)
                        .build());


        verifyCourtApplicationSummonsApprovedPublicEvent(applicationId, caseId);
    }
    @Test
    public void shouldRaisePublicSummonsRejected() throws Exception {
        enableAmendReshareFeature(false);

        final String userId = randomUUID().toString();
        final String caseId = randomUUID().toString();
        final String applicationId = randomUUID().toString();
        final String defendantId = randomUUID().toString();

        addProsecutionCaseToCrownCourt(caseId, defendantId);

        initiateCourtProceedingsForCourtApplication(applicationId, caseId, "applications/progression.initiate-court-proceedings-for-first-hearing-summons-linked-application.json");

        verifyCourtApplicationCreatedPrivateEvent();

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

        final JsonObject publicHearingResultedJsonObject = createPublicHearingResulted(hearing, summonResultJsonObject);

        sendMessage(messageProducerClientPublic, PUBLIC_HEARING_RESULTED, publicHearingResultedJsonObject,
                metadataBuilder()
                        .withId(randomUUID())
                        .withName(PUBLIC_HEARING_RESULTED)
                        .withUserId(userId)
                        .build());

        verifySummonsRejected(applicationId);
        verifyCourtApplicationSummonsRejectedPublicEvent(applicationId);

        final List<String> expectedEmailDetails = newArrayList(PROSECUTOR_EMAIL_ADDRESS, "Robert12 Smith12, randomreference234", "Robert13 Smith13, randomreference345", rejectionReason);
        verifyEmailNotificationIsRaisedWithoutAttachment(expectedEmailDetails);
    }

    @Test
    public void shouldRaisePublicSummonsRejectedV2() throws Exception {
        enableAmendReshareFeature(true);
        final String userId = randomUUID().toString();
        final String caseId = randomUUID().toString();
        final String applicationId = randomUUID().toString();
        final String defendantId = randomUUID().toString();

        addProsecutionCaseToCrownCourt(caseId, defendantId);

        initiateCourtProceedingsForCourtApplication(applicationId, caseId, "applications/progression.initiate-court-proceedings-for-first-hearing-summons-linked-application.json");

        verifyCourtApplicationCreatedPrivateEvent();

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

        sendMessage(messageProducerClientPublic, PUBLIC_HEARING_RESULTED_V2, publicHearingResultedJsonObject,
                metadataBuilder()
                        .withId(randomUUID())
                        .withName(PUBLIC_HEARING_RESULTED_V2)
                        .withUserId(userId)
                        .build());

        verifySummonsRejected(applicationId);
        verifyCourtApplicationSummonsRejectedPublicEvent(applicationId);

        final List<String> expectedEmailDetails = newArrayList(PROSECUTOR_EMAIL_ADDRESS, "Robert12 Smith12, randomreference234", "Robert13 Smith13, randomreference345", rejectionReason);
        verifyEmailNotificationIsRaisedWithoutAttachment(expectedEmailDetails);
    }
    private void verifySummonsRejected(String applicationId) {
        final Optional<JsonObject> message = retrieveMessageAsJsonObject(consumerForSummonsRejected);
        assertTrue(message.isPresent());
        final String rejectedApplicationId = message.get().getJsonObject("courtApplication").getString("id");
        assertThat(rejectedApplicationId, is(applicationId));
        assertThat(message.get().getJsonObject("summonsRejectedOutcome").getJsonArray("reasons").getString(0), is(rejectionReason));
        assertThat(message.get().getJsonObject("summonsRejectedOutcome").getString("prosecutorEmailAddress"), is(PROSECUTOR_EMAIL_ADDRESS));
    }

    private JsonObject createPublicHearingResulted(final JsonObject hearing, final JsonObject summonResultJsonObject) {
        final JsonArray courtApplicationsArray = hearing.getJsonArray("courtApplications");
        final JsonObject courtApplication = courtApplicationsArray.getJsonObject(0);
        return Json.createObjectBuilder()
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

    private void verifyCourtApplicationCreatedPrivateEvent() {
        final Optional<JsonObject> message = retrieveMessageAsJsonObject(consumerForCourtApplicationCreated);
        assertTrue(message.isPresent());
        final String applicationReference = message.get().getJsonObject("courtApplication").getString("applicationReference");
        assertThat(applicationReference, is(notNullValue()));
    }

    private String getNewHearingId() {
        final Optional<JsonObject> message = retrieveMessageAsJsonObject(consumerForInitiateCourtHearingAfterSummonsApproved);
        assertTrue(message.isPresent());
        final JsonObject courtHearing = message.get().getJsonObject("courtHearing");
        return courtHearing.getString("id");
    }

    public JsonObject getHearingInMessagingQueueForBoxWorkReferred() {
        final Optional<JsonObject> message = retrieveMessageAsJsonObject(messageConsumerClientPublicForReferBoxWorkApplicationOnHearingInitiated);
        assertTrue(message.isPresent());
        return message.get().getJsonObject("hearing");
    }

    public void verifyCourtApplicationSummonsApprovedPublicEvent(final String applicationId, final String caseId) {
        final Optional<JsonObject> message = retrieveMessageAsJsonObject(messageConsumerClientPublicSummonsApproved);
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
        final Optional<JsonObject> message = retrieveMessageAsJsonObject(messageConsumerClientPublicSummonsRejected);
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
        poll(requestParams(getReadUrl("/applications/" + applicationId),
                "application/vnd.progression.query.application.aaag+json").withHeader(USER_ID, randomUUID()))
                .until(status().is(OK), payload().isJson(matcher));
    }
}
