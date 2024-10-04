package uk.gov.moj.cpp.progression;

import static com.google.common.io.Resources.getResource;
import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static java.nio.charset.Charset.defaultCharset;
import static java.time.LocalDateTime.now;
import static java.time.temporal.ChronoUnit.DAYS;
import static java.util.Collections.singletonList;
import static java.util.Optional.ofNullable;
import static java.util.UUID.fromString;
import static java.util.UUID.randomUUID;
import static javax.json.Json.createObjectBuilder;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertTrue;
import static uk.gov.justice.services.integrationtest.utils.jms.JmsMessageConsumerClientProvider.newPrivateJmsMessageConsumerClientProvider;
import static uk.gov.justice.services.integrationtest.utils.jms.JmsMessageConsumerClientProvider.newPublicJmsMessageConsumerClientProvider;
import static uk.gov.justice.services.integrationtest.utils.jms.JmsMessageProducerClientProvider.newPublicJmsMessageProducerClientProvider;
import static uk.gov.justice.services.messaging.JsonEnvelope.metadataBuilder;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeMatcher.jsonEnvelope;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeMetadataMatcher.metadata;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeStreamMatcher.streamContaining;
import static uk.gov.moj.cpp.progression.domain.constant.CaseStatusEnum.INACTIVE;
import static uk.gov.moj.cpp.progression.helper.AbstractTestHelper.getWriteUrl;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.addProsecutionCaseToCrownCourtFirstHearing;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.getHearingForDefendant;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.pollProsecutionCasesProgressionFor;
import static uk.gov.moj.cpp.progression.helper.QueueUtil.buildMetadata;
import static uk.gov.moj.cpp.progression.helper.QueueUtil.retrieveMessage;
import static uk.gov.moj.cpp.progression.helper.QueueUtil.retrieveMessageBody;
import static uk.gov.moj.cpp.progression.helper.RestHelper.postCommand;
import static uk.gov.moj.cpp.progression.it.framework.ContextNameProvider.CONTEXT_NAME;
import static uk.gov.moj.cpp.progression.util.FileUtil.getPayload;
import static uk.gov.moj.cpp.progression.util.ReferProsecutionCaseToCrownCourtHelper.getProsecutionCaseMatchers;

import uk.gov.justice.core.courts.AddOnlinePleaAllocation;
import uk.gov.justice.progression.courts.OpaNoticeSent;
import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.common.converter.jackson.ObjectMapperProducer;
import uk.gov.justice.services.integrationtest.utils.jms.JmsMessageConsumerClient;
import uk.gov.justice.services.integrationtest.utils.jms.JmsMessageProducerClient;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.messaging.Metadata;
import uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopePayloadMatcher;
import uk.gov.moj.cpp.platform.test.feature.toggle.FeatureStubber;
import uk.gov.moj.cpp.progression.plea.json.schemas.PleasAllocationDetails;
import uk.gov.moj.cpp.progression.stub.HearingStub;

import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;

import javax.json.JsonObject;

import com.google.common.collect.ImmutableMap;
import com.google.common.io.Resources;
import org.apache.http.HttpStatus;
import org.hamcrest.Matcher;
import org.hamcrest.Matchers;
import org.json.JSONException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

@SuppressWarnings("all")
public class OnlinePleasAllocationIT extends AbstractIT {

    private static final String PUBLIC_DEFENCE_ALLOCATION_PLEAS_ADDED = "public.defence.allocation-pleas-added";
    private static final String PUBLIC_DEFENCE_ALLOCATION_PLEAS_UPDATED = "public.defence.allocation-pleas-updated";
    private static final String PROGRESSION_EVENT_ONLINE_PLEA_ALLOCATION_ADDED = "progression.event.online-plea-allocation-added";
    private static final String PROGRESSION_EVENT_ONLINE_PLEA_ALLOCATION_UPDATED = "progression.event.online-plea-allocation-updated";
    private static final String PROGRESSION_EVENT_LISTING_STATUS_CHANGED = "progression.event.prosecutionCase-defendant-listing-status-changed-v2";
    private static final String PROGRESSION_QUERY_GET_CASE_HEARING_TYPES = "application/vnd.progression.query.case.hearingtypes+json";
    private static final String ALLOCATION_PLEAS_ADDED = "public.defence.allocation-pleas-added.json";
    private static final String ALLOCATION_PLEAS_UPDATED = "public.defence.allocation-pleas-updated.json";
    private static final String GENERATE_OPA_NOTICE = "progression.command.generate-opa-notice.json";
    private static final String PUBLIC_LISTING_HEARING_CONFIRMED = "public.listing.hearing-confirmed";
    private static final String PUBLIC_HEARING_RESULTED_V2 = "public.events.hearing.hearing-resulted";
    private static final String PUBLIC_HEARING_RESULTED_CASE_UPDATED_V2 = "public.events.hearing.first-hearing-resulted-case-updated";
    private static final String userId = randomUUID().toString();
    private static final String offenceId = "3789ab16-0bb7-4ef1-87ef-c936bf0364f1";
    private static final String offenceId2 = randomUUID().toString();
    private static final String courtCentreId = "111bdd2a-6b7a-4002-bc8c-5c6f93844f40";
    private static final String newCourtCentreId = "999bdd2a-6b7a-4002-bc8c-5c6f93844f40";
    private static final String courtCentreName = "Lavender Hill Magistrate's Court";
    private static final String bailStatusCode = "C";
    private static final String bailStatusDescription = "Remanded into Custody";
    private static final String bailStatusId = "2593cf09-ace0-4b7d-a746-0703a29f33b5";
    private static final JsonObjectToObjectConverter jsonObjectToObjectConverter = new JsonObjectToObjectConverter(new ObjectMapperProducer().objectMapper());
    private static final String PROGRESSION_EVENT_PUBLIC_LIST_OPA_NOTICE_REQUESTED = "progression.event.opa-public-list-notice-requested";
    private static final String PROGRESSION_EVENT_PRESS_LIST_OPA_NOTICE_REQUESTED = "progression.event.opa-press-list-notice-requested";
    private static final String PROGRESSION_EVENT_RESULT_LIST_OPA_NOTICE_REQUESTED = "progression.event.opa-result-list-notice-requested";
    private static final String PUBLIC_PROGRESSION_PUBLIC_LIST_OPA_NOTICE_GENERATED = "public.progression.public-list-opa-notice-generated";
    private static final String PUBLIC_PROGRESSION_PRESS_LIST_OPA_NOTICE_GENERATED = "public.progression.press-list-opa-notice-generated";
    private static final String PUBLIC_PROGRESSION_RESULT_LIST_OPA_NOTICE_GENERATED = "public.progression.result-list-opa-notice-generated";
    private static final String PROGRESSION_EVENT_PUBLIC_LIST_OPA_NOTICE_DEACTIVATED = "progression.event.opa-public-list-notice-deactivated";
    private static final String PROGRESSION_EVENT_PRESS_LIST_OPA_NOTICE_DEACTIVATED = "progression.event.opa-press-list-notice-deactivated";
    private static final String PROGRESSION_EVENT_RESULT_LIST_OPA_NOTICE_DEACTIVATED = "progression.event.opa-result-list-notice-deactivated";
    private static final String PROGRESSION_EVENT_PUBLIC_LIST_OPA_NOTICE_SENT = "progression.event.opa-public-list-notice-sent";
    private static final String PROGRESSION_EVENT_PRESS_LIST_OPA_NOTICE_SENT = "progression.event.opa-press-list-notice-sent";
    private static final String PROGRESSION_EVENT_RESULT_LIST_OPA_NOTICE_SENT = "progression.event.opa-result-list-notice-sent";
    private static final String PUBLIC_PROGRESSION_PUBLIC_LIST_OPA_NOTICE_SENT = "public.stagingpubhub.opa-public-list-notice-sent";
    private static final String PUBLIC_PROGRESSION_PRESS_LIST_OPA_NOTICE_SENT = "public.stagingpubhub.opa-press-list-notice-sent";
    private static final String PUBLIC_PROGRESSION_RESULT_LIST_OPA_NOTICE_SENT = "public.stagingpubhub.opa-result-list-notice-sent";
    private static final String PUBLIC_PROGRESSION_HEARING_RESULTED_CASE_UPDATED = "public.progression.hearing-resulted-case-updated";
    private static final String PUBLIC_PROGRESSION_HEARING_RESULTED = "public.progression.hearing-resulted";
    public static final String DATE_TIME_FORMAT = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'";

    private JmsMessageProducerClient messageProducerClientPublic;
    private JmsMessageConsumerClient messageConsumerPublicNoticeSent;
    private JmsMessageConsumerClient messageConsumerPressNoticeSent;
    private JmsMessageConsumerClient messageConsumerResultNoticeSent;
    private JmsMessageConsumerClient listingStatusChangedConsumer;
    private JmsMessageConsumerClient addOnlinePleasAllocationConsumer;
    private JmsMessageConsumerClient updateOnlinePleasAllocationConsumer;
    private JmsMessageConsumerClient requestedPublicListConsumer;
    private JmsMessageConsumerClient requestedPressListConsumer;
    private JmsMessageConsumerClient requestedResultListConsumer;
    private JmsMessageConsumerClient generatedPublicListConsumer;
    private JmsMessageConsumerClient generatedPressListConsumer;
    private JmsMessageConsumerClient generatedResultListConsumer;
    private JmsMessageConsumerClient deactivatedPublicListConsumer;
    private JmsMessageConsumerClient deactivatedPressListConsumer;
    private JmsMessageConsumerClient deactivatedResultListConsumer;
    private JmsMessageConsumerClient messageConsumerClientPublicForHearingResultedCaseUpdated;
    private JmsMessageConsumerClient messageConsumerClientPublicForHearingResulted;

    @BeforeEach
    public void setUp() {
        HearingStub.stubInitiateHearing();
        messageProducerClientPublic = newPublicJmsMessageProducerClientProvider().getMessageProducerClient();
        listingStatusChangedConsumer = newPrivateJmsMessageConsumerClientProvider(CONTEXT_NAME).withEventNames(PROGRESSION_EVENT_LISTING_STATUS_CHANGED).getMessageConsumerClient();
        addOnlinePleasAllocationConsumer = newPrivateJmsMessageConsumerClientProvider(CONTEXT_NAME).withEventNames(PROGRESSION_EVENT_ONLINE_PLEA_ALLOCATION_ADDED).getMessageConsumerClient();
        updateOnlinePleasAllocationConsumer = newPrivateJmsMessageConsumerClientProvider(CONTEXT_NAME).withEventNames(PROGRESSION_EVENT_ONLINE_PLEA_ALLOCATION_UPDATED).getMessageConsumerClient();
        requestedPublicListConsumer = newPrivateJmsMessageConsumerClientProvider(CONTEXT_NAME).withEventNames(PROGRESSION_EVENT_PUBLIC_LIST_OPA_NOTICE_REQUESTED).getMessageConsumerClient();
        requestedPressListConsumer = newPrivateJmsMessageConsumerClientProvider(CONTEXT_NAME).withEventNames(PROGRESSION_EVENT_PRESS_LIST_OPA_NOTICE_REQUESTED).getMessageConsumerClient();
        requestedResultListConsumer = newPrivateJmsMessageConsumerClientProvider(CONTEXT_NAME).withEventNames(PROGRESSION_EVENT_RESULT_LIST_OPA_NOTICE_REQUESTED).getMessageConsumerClient();
        generatedPublicListConsumer = newPublicJmsMessageConsumerClientProvider().withEventNames(PUBLIC_PROGRESSION_PUBLIC_LIST_OPA_NOTICE_GENERATED).getMessageConsumerClient();
        generatedPressListConsumer = newPublicJmsMessageConsumerClientProvider().withEventNames(PUBLIC_PROGRESSION_PRESS_LIST_OPA_NOTICE_GENERATED).getMessageConsumerClient();
        generatedResultListConsumer = newPublicJmsMessageConsumerClientProvider().withEventNames(PUBLIC_PROGRESSION_RESULT_LIST_OPA_NOTICE_GENERATED).getMessageConsumerClient();
        deactivatedPublicListConsumer = newPrivateJmsMessageConsumerClientProvider(CONTEXT_NAME).withEventNames(PROGRESSION_EVENT_PUBLIC_LIST_OPA_NOTICE_DEACTIVATED).getMessageConsumerClient();
        deactivatedPressListConsumer = newPrivateJmsMessageConsumerClientProvider(CONTEXT_NAME).withEventNames(PROGRESSION_EVENT_PRESS_LIST_OPA_NOTICE_DEACTIVATED).getMessageConsumerClient();
        deactivatedResultListConsumer = newPrivateJmsMessageConsumerClientProvider(CONTEXT_NAME).withEventNames(PROGRESSION_EVENT_RESULT_LIST_OPA_NOTICE_DEACTIVATED).getMessageConsumerClient();
        messageConsumerClientPublicForHearingResultedCaseUpdated = newPublicJmsMessageConsumerClientProvider().withEventNames(PUBLIC_PROGRESSION_HEARING_RESULTED_CASE_UPDATED).getMessageConsumerClient();
        messageConsumerClientPublicForHearingResulted = newPublicJmsMessageConsumerClientProvider().withEventNames(PUBLIC_PROGRESSION_HEARING_RESULTED).getMessageConsumerClient();
        messageConsumerPublicNoticeSent = newPrivateJmsMessageConsumerClientProvider(CONTEXT_NAME).withEventNames(PROGRESSION_EVENT_PUBLIC_LIST_OPA_NOTICE_SENT).getMessageConsumerClient();
        messageConsumerPressNoticeSent = newPrivateJmsMessageConsumerClientProvider(CONTEXT_NAME).withEventNames(PROGRESSION_EVENT_PRESS_LIST_OPA_NOTICE_SENT).getMessageConsumerClient();
        messageConsumerResultNoticeSent = newPrivateJmsMessageConsumerClientProvider(CONTEXT_NAME).withEventNames(PROGRESSION_EVENT_RESULT_LIST_OPA_NOTICE_SENT).getMessageConsumerClient();
        setFeatureToggle("OPA", true);
    }


    @Test
    public void shouldAddPleasAllocation() throws Exception {
        final UUID allocationId = randomUUID();
        final String userId = randomUUID().toString();
        final String caseId = randomUUID().toString();
        final String defendantId = randomUUID().toString();

        final String hearingId = createCaseAndGetHearingId(caseId, defendantId, false);

        final JsonObject onlinePleaAllocationEvent = buildOnlinePleaAllocationPayload(allocationId.toString(), caseId, defendantId, ALLOCATION_PLEAS_ADDED);

        final JsonEnvelope publicEventEnvelope = JsonEnvelope.envelopeFrom(buildMetadata(PUBLIC_DEFENCE_ALLOCATION_PLEAS_ADDED, userId), onlinePleaAllocationEvent);
        messageProducerClientPublic.sendMessage(PUBLIC_DEFENCE_ALLOCATION_PLEAS_ADDED, publicEventEnvelope);

        verifyInMessageQueueForOnlinePleaAllocatedEvent(addOnlinePleasAllocationConsumer, PROGRESSION_EVENT_ONLINE_PLEA_ALLOCATION_ADDED, onlinePleaAllocationEvent, hearingId);
    }

    @Test
    public void shouldUpdatePleasAllocation() throws Exception {
        final UUID allocationId = randomUUID();

        final String caseId = randomUUID().toString();
        final String defendantId = randomUUID().toString();

        final String hearingId = createCaseAndGetHearingId(caseId, defendantId, false);
        final JsonObject addOnlinePleaAllocationEvent = buildOnlinePleaAllocationPayload(allocationId.toString(), caseId, defendantId, ALLOCATION_PLEAS_ADDED);
        final Metadata addMetadata = getMetadata(userId, PUBLIC_DEFENCE_ALLOCATION_PLEAS_ADDED);

        JsonEnvelope publicEventEnvelope = JsonEnvelope.envelopeFrom(buildMetadata(PUBLIC_DEFENCE_ALLOCATION_PLEAS_ADDED, userId), addOnlinePleaAllocationEvent);
        messageProducerClientPublic.sendMessage(PUBLIC_DEFENCE_ALLOCATION_PLEAS_ADDED, publicEventEnvelope);

        verifyInMessageQueueForOnlinePleaAllocatedEvent(addOnlinePleasAllocationConsumer, PROGRESSION_EVENT_ONLINE_PLEA_ALLOCATION_ADDED, addOnlinePleaAllocationEvent, hearingId);

        final JsonObject updatedOnlinePleaAllocationEvent = buildOnlinePleaAllocationPayload(allocationId.toString(), caseId, defendantId, ALLOCATION_PLEAS_UPDATED);

        publicEventEnvelope = JsonEnvelope.envelopeFrom(buildMetadata(PUBLIC_DEFENCE_ALLOCATION_PLEAS_UPDATED, userId), updatedOnlinePleaAllocationEvent);
        messageProducerClientPublic.sendMessage(PUBLIC_DEFENCE_ALLOCATION_PLEAS_UPDATED, publicEventEnvelope);

        verifyInMessageQueueForOnlinePleaAllocatedEvent(updateOnlinePleasAllocationConsumer, PROGRESSION_EVENT_ONLINE_PLEA_ALLOCATION_UPDATED, updatedOnlinePleaAllocationEvent, hearingId);
    }

    @Test
    public void shouldNotUpdatePleasAllocationWhenFeatureToggleIsOff() throws Exception {
        final UUID allocationId = randomUUID();

        final String caseId = randomUUID().toString();
        final String defendantId = randomUUID().toString();

        final String hearingId = createCaseAndGetHearingId(caseId, defendantId, false);
        final JsonObject addOnlinePleaAllocationEvent = buildOnlinePleaAllocationPayload(allocationId.toString(), caseId, defendantId, ALLOCATION_PLEAS_ADDED);

        JsonEnvelope publicEventEnvelope = JsonEnvelope.envelopeFrom(buildMetadata(PUBLIC_DEFENCE_ALLOCATION_PLEAS_ADDED, userId), addOnlinePleaAllocationEvent);
        messageProducerClientPublic.sendMessage(PUBLIC_DEFENCE_ALLOCATION_PLEAS_ADDED, publicEventEnvelope);

        verifyInMessageQueueForOnlinePleaAllocatedEvent(addOnlinePleasAllocationConsumer, PROGRESSION_EVENT_ONLINE_PLEA_ALLOCATION_ADDED, addOnlinePleaAllocationEvent, hearingId);

        final JsonObject updatedOnlinePleaAllocationEvent = buildOnlinePleaAllocationPayload(allocationId.toString(), caseId, defendantId, ALLOCATION_PLEAS_UPDATED);

        setFeatureToggle("OPA", false);
        publicEventEnvelope = JsonEnvelope.envelopeFrom(buildMetadata(PUBLIC_DEFENCE_ALLOCATION_PLEAS_UPDATED, userId), updatedOnlinePleaAllocationEvent);
        messageProducerClientPublic.sendMessage(PUBLIC_DEFENCE_ALLOCATION_PLEAS_UPDATED, publicEventEnvelope);


        verifyNoMessageQueueForOnlinePleaAllocatedEvent(updateOnlinePleasAllocationConsumer);
    }

    @Test
    public void shouldDeactivateOpaNotices() throws Exception {
        final UUID allocationId = randomUUID();
        final String caseId = randomUUID().toString();
        final String defendantId = randomUUID().toString();
        final String hearingId = createCaseAndGetHearingId(caseId, defendantId, false);

        final JsonObject addOnlinePleaAllocationEvent = buildOnlinePleaAllocationPayload(allocationId.toString(), caseId, defendantId, ALLOCATION_PLEAS_ADDED);

        final JsonEnvelope publicEventEnvelope = JsonEnvelope.envelopeFrom(buildMetadata(PUBLIC_DEFENCE_ALLOCATION_PLEAS_ADDED, userId), addOnlinePleaAllocationEvent);
        messageProducerClientPublic.sendMessage(PUBLIC_DEFENCE_ALLOCATION_PLEAS_ADDED, publicEventEnvelope);

        verifyInMessageQueueForOnlinePleaAllocatedEvent(addOnlinePleasAllocationConsumer, PROGRESSION_EVENT_ONLINE_PLEA_ALLOCATION_ADDED, addOnlinePleaAllocationEvent, hearingId);

        final io.restassured.response.Response publicResponse = postCommand(getWriteUrl("/opa-notice/request"),
                "application/vnd.progression.request-opa-public-list-notice+json", "{}");
        assertThat(publicResponse.getStatusCode(), equalTo(HttpStatus.SC_ACCEPTED));
        verifyMessageinRequestOpaNoticeQueue(deactivatedPublicListConsumer);

        final io.restassured.response.Response pressResponse = postCommand(getWriteUrl("/opa-notice/request"),
                "application/vnd.progression.request-opa-press-list-notice+json", "{}");
        assertThat(pressResponse.getStatusCode(), equalTo(HttpStatus.SC_ACCEPTED));
        verifyMessageinRequestOpaNoticeQueue(deactivatedPressListConsumer);

        final io.restassured.response.Response resultResponse = postCommand(getWriteUrl("/opa-notice/request"),
                "application/vnd.progression.request-opa-result-list-notice+json", "{}");
        assertThat(publicResponse.getStatusCode(), equalTo(HttpStatus.SC_ACCEPTED));
        verifyMessageinRequestOpaNoticeQueue(deactivatedResultListConsumer);
    }

    @Test
    public void shouldGeneratePublicAndPressOpaNotices() throws Exception {
        final UUID allocationId = randomUUID();
        final String caseId = randomUUID().toString();
        final String defendantId = randomUUID().toString();

        final String hearingId = createCaseAndGetHearingId(caseId, defendantId, false);
        final JsonObject addOnlinePleaAllocationEvent = buildOnlinePleaAllocationPayload(allocationId.toString(), caseId, defendantId, ALLOCATION_PLEAS_ADDED);
        final JsonObject hearingConfirmedJson = getHearingJsonObject("public.listing.opa-hearing-confirmed.json", caseId, hearingId, defendantId, courtCentreId, courtCentreName);


        final JmsMessageConsumerClient messageConsumer = newPrivateJmsMessageConsumerClientProvider(CONTEXT_NAME).withEventNames(PROGRESSION_EVENT_LISTING_STATUS_CHANGED).getMessageConsumerClient();
        JsonEnvelope publicEventEnvelope = JsonEnvelope.envelopeFrom(buildMetadata(PUBLIC_LISTING_HEARING_CONFIRMED, userId), hearingConfirmedJson);
        messageProducerClientPublic.sendMessage(PUBLIC_LISTING_HEARING_CONFIRMED, publicEventEnvelope);
        doVerifyProsecutionCaseDefendantListingStatusChanged(messageConsumer);

        publicEventEnvelope = JsonEnvelope.envelopeFrom(buildMetadata(PUBLIC_DEFENCE_ALLOCATION_PLEAS_ADDED, userId), addOnlinePleaAllocationEvent);
        messageProducerClientPublic.sendMessage(PUBLIC_DEFENCE_ALLOCATION_PLEAS_ADDED, publicEventEnvelope);

        verifyInMessageQueueForOnlinePleaAllocatedEvent(addOnlinePleasAllocationConsumer, PROGRESSION_EVENT_ONLINE_PLEA_ALLOCATION_ADDED, addOnlinePleaAllocationEvent, hearingId);

        final io.restassured.response.Response publicResponse = postCommand(getWriteUrl("/opa-notice/request"),
                "application/vnd.progression.request-opa-public-list-notice+json", "{}");
        assertThat(publicResponse.getStatusCode(), equalTo(HttpStatus.SC_ACCEPTED));
        verifyMessageinRequestOpaNoticeQueue(generatedPublicListConsumer);

        final io.restassured.response.Response pressResponse = postCommand(getWriteUrl("/opa-notice/request"),
                "application/vnd.progression.request-opa-press-list-notice+json", "{}");
        assertThat(pressResponse.getStatusCode(), equalTo(HttpStatus.SC_ACCEPTED));
        verifyMessageinRequestOpaNoticeQueue(generatedPressListConsumer);
    }


    @Test
    public void shouldNotGeneratePublicAndPressOpaNoticesWhenFeatureToggleIsOff() throws Exception {
        final UUID allocationId = randomUUID();
        final String caseId = randomUUID().toString();
        final String defendantId = randomUUID().toString();

        final String hearingId = createCaseAndGetHearingId(caseId, defendantId, false);
        final JsonObject addOnlinePleaAllocationEvent = buildOnlinePleaAllocationPayload(allocationId.toString(), caseId, defendantId, ALLOCATION_PLEAS_ADDED);
        final JsonObject hearingConfirmedJson = getHearingJsonObject("public.listing.opa-hearing-confirmed.json", caseId, hearingId, defendantId, courtCentreId, courtCentreName);

        final JmsMessageConsumerClient messageConsumer = newPrivateJmsMessageConsumerClientProvider(CONTEXT_NAME).withEventNames(PROGRESSION_EVENT_LISTING_STATUS_CHANGED).getMessageConsumerClient();

        JsonEnvelope publicEventEnvelope = JsonEnvelope.envelopeFrom(buildMetadata(PUBLIC_LISTING_HEARING_CONFIRMED, userId), hearingConfirmedJson);
        messageProducerClientPublic.sendMessage(PUBLIC_LISTING_HEARING_CONFIRMED, publicEventEnvelope);

        doVerifyProsecutionCaseDefendantListingStatusChanged(messageConsumer);

        setFeatureToggle("OPA", false);
        publicEventEnvelope = JsonEnvelope.envelopeFrom(buildMetadata(PUBLIC_DEFENCE_ALLOCATION_PLEAS_ADDED, userId), addOnlinePleaAllocationEvent);
        messageProducerClientPublic.sendMessage(PUBLIC_DEFENCE_ALLOCATION_PLEAS_ADDED, publicEventEnvelope);

        verifyNoMessageQueueForOnlinePleaAllocatedEvent(addOnlinePleasAllocationConsumer);

    }


    @Test
    public void shouldGenerateResultNoticeOpaNotices() throws Exception {
        final UUID allocationId = randomUUID();
        final String caseId = randomUUID().toString();
        final String defendantId = randomUUID().toString();

        final String hearingId = createCaseAndGetHearingId(caseId, defendantId, false);
        final JsonObject addOnlinePleaAllocationEvent = buildOnlinePleaAllocationPayload(allocationId.toString(), caseId, defendantId, ALLOCATION_PLEAS_ADDED);

        JsonEnvelope publicEventEnvelope = JsonEnvelope.envelopeFrom(buildMetadata(PUBLIC_DEFENCE_ALLOCATION_PLEAS_ADDED, userId), addOnlinePleaAllocationEvent);
        messageProducerClientPublic.sendMessage(PUBLIC_DEFENCE_ALLOCATION_PLEAS_ADDED, publicEventEnvelope);

        verifyInMessageQueueForOnlinePleaAllocatedEvent(addOnlinePleasAllocationConsumer, PROGRESSION_EVENT_ONLINE_PLEA_ALLOCATION_ADDED, addOnlinePleaAllocationEvent, hearingId);

        publicEventEnvelope = JsonEnvelope.envelopeFrom(buildMetadata(PUBLIC_HEARING_RESULTED_V2, userId), getHearingWithSingleCaseJsonObject(PUBLIC_HEARING_RESULTED_CASE_UPDATED_V2 + ".json", caseId,
                hearingId, defendantId, newCourtCentreId, bailStatusCode, bailStatusDescription, bailStatusId));
        messageProducerClientPublic.sendMessage(PUBLIC_HEARING_RESULTED_V2, publicEventEnvelope);

        verifyInMessagingQueueForHearingResulted();

        final io.restassured.response.Response resultResponse = postCommand(getWriteUrl("/opa-notice/request"),
                "application/vnd.progression.request-opa-result-list-notice+json", "{}");
        assertThat(resultResponse.getStatusCode(), equalTo(HttpStatus.SC_ACCEPTED));
        final Optional<JsonEnvelope> jsonEnvelope = verifyMessageinRequestOpaNoticeQueue(generatedResultListConsumer);

        jsonEnvelope.ifPresent(envelope -> {
            assertThat(Stream.of(envelope), streamContaining(
                    jsonEnvelope(
                            metadata()
                                    .withName(PUBLIC_PROGRESSION_RESULT_LIST_OPA_NOTICE_GENERATED),
                            JsonEnvelopePayloadMatcher.payload().isJson(Matchers.allOf(
                                    withJsonPath("$.defendantId", is(notNullValue())),
                                    withJsonPath("$.hearingId", is(notNullValue())),
                                    withJsonPath("$.notificationId", is(notNullValue())),
                                    withJsonPath("$.opaNotice.bailStatus", is("Conditional")),
                                    withJsonPath("$.opaNotice.caseUrn", is("PRCS232397VG")),
                                    withJsonPath("$.opaNotice.court", is("Liverpool Sreat Mags Court")),
                                    withJsonPath("$.opaNotice.firstHearingDate", is(notNullValue())),
                                    withJsonPath("$.opaNotice.firstName", is("samba")),
                                    withJsonPath("$.opaNotice.middleName", is("a")),
                                    withJsonPath("$.opaNotice.lastName", is("ramba")),
                                    withJsonPath("$.opaNotice.offences[0].allocationDecision", is("Defendant chooses trial by jury")),
                                    withJsonPath("$.opaNotice.offences[0].decisionDate", is("2023-10-08")),
                                    withJsonPath("$.opaNotice.offences[0].legislation", is("10 years punishment in jail")),
                                    withJsonPath("$.opaNotice.offences[0].title", is("Case worker murder")))))));
        });
    }

    @Test
    public void shouldOpaNoticeSent() throws Exception {
        final UUID notificationId = randomUUID();
        final String caseId = randomUUID().toString();
        final String defendantId = randomUUID().toString();
        final String hearingId = createCaseAndGetHearingId(caseId, defendantId, false);
        final OpaNoticeSent opaNoticeSent = getOpaNoticeSent(hearingId, defendantId);
        final JsonObject opaNoticeSentEvent = buildOpaNoticeSentEvent(opaNoticeSent);


        JsonEnvelope publicEventEnvelope = JsonEnvelope.envelopeFrom(buildMetadata(PUBLIC_PROGRESSION_PUBLIC_LIST_OPA_NOTICE_SENT, userId), opaNoticeSentEvent);
        messageProducerClientPublic.sendMessage(PUBLIC_PROGRESSION_PUBLIC_LIST_OPA_NOTICE_SENT, publicEventEnvelope);

        publicEventEnvelope = JsonEnvelope.envelopeFrom(buildMetadata(PUBLIC_PROGRESSION_PRESS_LIST_OPA_NOTICE_SENT, userId), opaNoticeSentEvent);
        messageProducerClientPublic.sendMessage(PUBLIC_PROGRESSION_PRESS_LIST_OPA_NOTICE_SENT, publicEventEnvelope);

        publicEventEnvelope = JsonEnvelope.envelopeFrom(buildMetadata(PUBLIC_PROGRESSION_RESULT_LIST_OPA_NOTICE_SENT, userId), opaNoticeSentEvent);
        messageProducerClientPublic.sendMessage(PUBLIC_PROGRESSION_RESULT_LIST_OPA_NOTICE_SENT, publicEventEnvelope);

        verifyMessageAddedToEventStream(opaNoticeSent);
    }

    private void verifyMessageAddedToEventStream(final OpaNoticeSent opaNoticeSent) {
        final Optional<JsonObject> publicListNotice = retrieveMessageBody(messageConsumerPublicNoticeSent);
        assertTrue(publicListNotice.isPresent());
        final Optional<JsonObject> pressListNotice = retrieveMessageBody(messageConsumerPressNoticeSent);
        assertTrue(pressListNotice.isPresent());
        final Optional<JsonObject> resultListNotice = retrieveMessageBody(messageConsumerResultNoticeSent);
        assertTrue(resultListNotice.isPresent());
        assertThat(resultListNotice.get().getString("hearingId"), equalTo(opaNoticeSent.getHearingId().toString()));
        assertThat(resultListNotice.get().getString("defendantId"), equalTo(opaNoticeSent.getDefendantId().toString()));
        assertThat(resultListNotice.get().getString("notificationId"), equalTo(opaNoticeSent.getNotificationId().toString()));
        assertThat(resultListNotice.get().getString("triggerDate"), equalTo(opaNoticeSent.getTriggerDate().toString()));
    }

    private JsonObject buildOpaNoticeSentEvent(final OpaNoticeSent opaNoticeSent) {
        return createObjectBuilder()
                .add("notificationId", opaNoticeSent.getNotificationId().toString())
                .add("hearingId", opaNoticeSent.getHearingId().toString())
                .add("defendantId", opaNoticeSent.getDefendantId().toString())
                .add("triggerDate", opaNoticeSent.getTriggerDate().toString())
                .build();
    }

    private OpaNoticeSent getOpaNoticeSent(final String hearingIdStr, final String defendantIdStr) {
        return OpaNoticeSent.opaNoticeSent()
                .withNotificationId(randomUUID())
                .withHearingId(fromString(hearingIdStr))
                .withDefendantId(fromString(defendantIdStr))
                .withTriggerDate(LocalDate.now())
                .build();
    }

    private static Metadata getMetadata(final String userId, final String event) {
        return metadataBuilder()
                .withId(randomUUID())
                .withName(event)
                .withUserId(userId)
                .build();
    }

    private String createCaseAndGetHearingId(final String caseId, final String defendantId, final boolean isYouth) throws IOException, JSONException {
        addProsecutionCaseToCrownCourtFirstHearing(caseId, defendantId, isYouth);

        pollProsecutionCasesProgressionFor(caseId, getProsecutionCaseMatchers(caseId, defendantId,
                singletonList(withJsonPath("$.prosecutionCase.defendants[0].offences[0].offenceCode", is("TTH105HY")))));

        final String hearingId = retrieveMessageBody(listingStatusChangedConsumer)
                .get().getJsonObject("hearing").getString("id");
        getHearingForDefendant(hearingId, new Matcher[0]);

        return hearingId;
    }

    private Optional<JsonEnvelope> verifyMessageinRequestOpaNoticeQueue(final JmsMessageConsumerClient consumer) {
        final Optional<JsonEnvelope> envelopeStream = retrieveMessage(consumer);
        final Optional<JsonEnvelope> jsonEnvelope = ofNullable(envelopeStream.stream().findFirst().orElse(null));

        assertThat(jsonEnvelope.isPresent(), is(true));

        return jsonEnvelope;
    }

    private void verifyInMessageQueueForOnlinePleaAllocatedEvent(final JmsMessageConsumerClient consumer,
                                                                 final String event,
                                                                 final JsonObject onlinePleaAllocationEvent,
                                                                 final String hearingId) {
        final PleasAllocationDetails pleasAllocation = jsonObjectToObjectConverter.convert(onlinePleaAllocationEvent, AddOnlinePleaAllocation.class).getPleasAllocation();
        final Optional<JsonEnvelope> envelopeStream = retrieveMessage(consumer);

        assertThat(envelopeStream.stream(), streamContaining(
                jsonEnvelope(
                        metadata()
                                .withName(event),
                        JsonEnvelopePayloadMatcher.payload().isJson(Matchers.allOf(
                                withJsonPath("$.caseId", is(pleasAllocation.getCaseId().toString())),
                                withJsonPath("$.defendantId", is(pleasAllocation.getDefendantId().toString())),
                                withJsonPath("$.hearingId", is(hearingId)),
                                withJsonPath("$.offences[0].indicatedPlea", is(pleasAllocation.getOffencePleas().get(0).getIndicatedPlea())),
                                withJsonPath("$.offences[0].offenceId", is(pleasAllocation.getOffencePleas().get(0).getOffenceId().toString())),
                                withJsonPath("$.offences[0].pleaDate", is(pleasAllocation.getOffencePleas().get(0).getPleaDate().toString())),
                                withJsonPath("$.offences[1].indicatedPlea", is(pleasAllocation.getOffencePleas().get(1).getIndicatedPlea())),
                                withJsonPath("$.offences[1].offenceId", is(pleasAllocation.getOffencePleas().get(1).getOffenceId().toString())),
                                withJsonPath("$.offences[1].pleaDate", is(pleasAllocation.getOffencePleas().get(1).getPleaDate().toString())))))));
    }

    private void verifyNoMessageQueueForOnlinePleaAllocatedEvent(final JmsMessageConsumerClient consumer) {
        final Optional<JsonObject> message = retrieveMessageBody(consumer, 1L);
        assertThat(message.isPresent(), is(false));
    }

    private JsonObject buildOnlinePleaAllocationPayload(final String allocationId,
                                                        final String caseId,
                                                        final String defendantId,
                                                        final String resourceName) throws IOException {
        final String inputEvent = Resources.toString(getResource(resourceName), defaultCharset());

        return stringToJsonObjectConverter.convert(inputEvent
                .replaceAll("ALLOCATION_ID", allocationId)
                .replaceAll("CASE_ID", caseId)
                .replaceAll("DEFENDANT_ID", defendantId)
                .replaceAll("OFFENCE_ID1", offenceId)
                .replaceAll("OFFENCE_ID2", offenceId2));
    }

    private String doVerifyProsecutionCaseDefendantListingStatusChanged(final JmsMessageConsumerClient messageConsumerProsecutionCaseDefendantListingStatusChanged) {
        final Optional<JsonObject> message = retrieveMessageBody(messageConsumerProsecutionCaseDefendantListingStatusChanged);
        final JsonObject prosecutionCaseDefendantListingStatusChanged = message.get();

        return prosecutionCaseDefendantListingStatusChanged.getJsonObject("hearing").getString("id");
    }

    private String buildOnlineGenerateOpaNoticePayload(final String caseId,
                                                       final String defendantId,
                                                       final String hearingId) throws IOException {
        final String inputEvent = Resources.toString(getResource(GENERATE_OPA_NOTICE), defaultCharset());

        return inputEvent
                .replaceAll("HEARING_ID", hearingId)
                .replaceAll("CASE_ID", caseId)
                .replaceAll("DEFENDANT_ID", defendantId);
    }

    private JsonObject getHearingJsonObject(final String path, final String caseId, final String hearingId,
                                            final String defendantId, final String courtCentreId, final String courtCentreName) {
        return stringToJsonObjectConverter.convert(
                getPayload(path)
                        .replaceAll("CASE_ID", caseId)
                        .replaceAll("HEARING_ID", hearingId)
                        .replaceAll("DEFENDANT_ID", defendantId)
                        .replaceAll("COURT_CENTRE_ID", courtCentreId)
                        .replaceAll("COURT_CENTRE_NAME", courtCentreName)
                        .replaceAll("SITTING_DAY_1", now().plus(5, DAYS).format(DateTimeFormatter.ofPattern(DATE_TIME_FORMAT)))
                        .replaceAll("SITTING_DAY_2", now().plus(6, DAYS).format(DateTimeFormatter.ofPattern(DATE_TIME_FORMAT)))
                        .replaceAll("SITTING_DAY_3", now().plus(7, DAYS).format(DateTimeFormatter.ofPattern(DATE_TIME_FORMAT)))
                        .replaceAll("SITTING_DAY_4", now().plus(8, DAYS).format(DateTimeFormatter.ofPattern(DATE_TIME_FORMAT)))
        );
    }

    private JsonObject getHearingWithSingleCaseJsonObject(final String path, final String caseId, final String hearingId,
                                                          final String defendantId, final String courtCentreId, final String bailStatusCode,
                                                          final String bailStatusDescription, final String bailStatusId) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern(DATE_TIME_FORMAT);
        return stringToJsonObjectConverter.convert(
                getPayload(path)
                        .replaceAll("CASE_ID", caseId)
                        .replaceAll("HEARING_ID", hearingId)
                        .replaceAll("DEFENDANT_ID", defendantId)
                        .replaceAll("COURT_CENTRE_ID", courtCentreId)
                        .replaceAll("BAIL_STATUS_ID", bailStatusId)
                        .replaceAll("BAIL_STATUS_CODE", bailStatusCode)
                        .replaceAll("BAIL_STATUS_DESCRIPTION", bailStatusDescription)
                        .replaceAll("SITTING_DAY", formatter.format(now().plusMonths(1)))
                        .replaceAll("SHARED_TIME", formatter.format(now().minusDays(1)))
        );
    }

    private Matcher[] getDefendantUpdatedMatchers(final String caseId) {
        return new Matcher[]{
                withJsonPath("$.prosecutionCase.id", equalTo(caseId)),
                withJsonPath("$.prosecutionCase.defendants[0].personDefendant.bailStatus.code", equalTo(bailStatusCode)),
                withJsonPath("$.prosecutionCase.defendants[0].personDefendant.bailStatus.description", equalTo(bailStatusDescription)),
                withJsonPath("$.prosecutionCase.defendants[0].personDefendant.bailStatus.id", equalTo(bailStatusId)),
                withJsonPath("$.prosecutionCase.caseStatus", equalTo(INACTIVE.getDescription())),
                withJsonPath("$.prosecutionCase.defendants[0].proceedingsConcluded", equalTo(true)),
                withJsonPath("$.prosecutionCase.defendants[0].offences[0].proceedingsConcluded", equalTo(true))
        };
    }

    private void verifyInMessagingQueueForHearingResultedCaseUpdated() {
        final Optional<JsonObject> message = retrieveMessageBody(messageConsumerClientPublicForHearingResultedCaseUpdated);
        assertTrue(message.isPresent());
        assertThat(message.get().getJsonObject("prosecutionCase").getString("caseStatus"), equalTo("INACTIVE"));
        assertThat(message.get().getJsonObject("prosecutionCase").getJsonArray("defendants").getJsonObject(0).getBoolean("proceedingsConcluded"), equalTo(true));
    }

    private void verifyInMessagingQueueForHearingResulted() {
        final Optional<JsonObject> message = retrieveMessageBody(messageConsumerClientPublicForHearingResulted);
        assertTrue(message.isPresent());
        assertThat(message.get().getJsonObject("hearing").getJsonArray("prosecutionCases").getJsonObject(0).getString("caseStatus"), equalTo("INACTIVE"));
    }

    private void setFeatureToggle(final String featureName, final boolean isToggleOn) {
        final ImmutableMap<String, Boolean> features = ImmutableMap.of(featureName, isToggleOn);
        FeatureStubber.clearCache(CONTEXT_NAME);
        FeatureStubber.stubFeaturesFor(CONTEXT_NAME, features);
    }
}
