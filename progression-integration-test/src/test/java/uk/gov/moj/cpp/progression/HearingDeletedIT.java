package uk.gov.moj.cpp.progression;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.isJson;
import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static java.util.Collections.singletonList;
import static java.util.UUID.randomUUID;
import static org.apache.commons.lang3.RandomStringUtils.randomAlphanumeric;
import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertTrue;
import static uk.gov.justice.services.messaging.JsonEnvelope.metadataBuilder;
import static uk.gov.moj.cpp.progression.applications.applicationHelper.ApplicationHelper.initiateCourtProceedingsForCourtApplicationWithCourtHearing;
import static uk.gov.moj.cpp.progression.domain.constant.CaseStatusEnum.ACTIVE;
import static uk.gov.moj.cpp.progression.domain.constant.CaseStatusEnum.INACTIVE;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.addProsecutionCaseToCrownCourt;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.addProsecutionCaseToCrownCourtWithOneGrownDefendantAndTwoOffences;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.generateUrn;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.pollForApplication;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.pollProsecutionCasesProgressionAndReturnHearingId;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.pollProsecutionCasesProgressionFor;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.verifyHearingIsEmpty;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.verifyInMessagingQueueForHearingPopulatedToProbationCaseWorker;
import static uk.gov.moj.cpp.progression.helper.QueueUtil.privateEvents;
import static uk.gov.moj.cpp.progression.helper.QueueUtil.publicEvents;
import static uk.gov.moj.cpp.progression.helper.QueueUtil.sendMessage;
import static uk.gov.moj.cpp.progression.stub.DocumentGeneratorStub.stubDocumentCreate;
import static uk.gov.moj.cpp.progression.util.FileUtil.getPayload;
import static uk.gov.moj.cpp.progression.util.ReferProsecutionCaseToCrownCourtHelper.getProsecutionCaseMatchers;

import uk.gov.justice.services.common.converter.StringToJsonObjectConverter;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.messaging.Metadata;
import uk.gov.moj.cpp.progression.helper.QueueUtil;
import uk.gov.moj.cpp.progression.stub.HearingStub;

import java.io.IOException;
import java.util.Optional;
import java.util.UUID;

import javax.jms.JMSException;
import javax.jms.MessageConsumer;
import javax.jms.MessageProducer;
import javax.json.JsonObject;

import com.jayway.restassured.path.json.JsonPath;
import org.hamcrest.Matcher;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HearingDeletedIT extends AbstractIT {

    private final static Logger LOGGER = LoggerFactory.getLogger(HearingDeletedIT.class);

    private static final String PUBLIC_EVENTS_LISTING_ALLOCATED_HEARING_DELETED = "public.events.listing.allocated-hearing-deleted";
    private static final String PUBLIC_EVENTS_LISTING_UNALLOCATED_HEARING_DELETED = "public.events.listing.unallocated-hearing-deleted";
    private static final String PUBLIC_EVENTS_LISTING_HEARING_DELETED = "public.events.listing.hearing-deleted";
    private static final String PUBLIC_LISTING_HEARING_CONFIRMED = "public.listing.hearing-confirmed";
    private static final String PUBLIC_HEARING_RESULTED = "public.hearing.resulted";
    private static final String PUBLIC_HEARING_RESULTED_CASE_UPDATED = "public.hearing.resulted-case-updated";

    private MessageProducer messageProducerClientPublic ;
    private MessageConsumer messageConsumerProsecutionCaseDefendantListingStatusChanged ;
    private MessageConsumer messageConsumerHearingDeleted ;
    private MessageConsumer messageConsumerHearingDeletedForProsecutionCase ;
    private MessageConsumer messageConsumerHearingPopulatedToProbationCaseWorker ;
    private MessageConsumer messageConsumerDeletedHearingPopulatedToProbationCaseWorker ;

    private final StringToJsonObjectConverter stringToJsonObjectConverter = new StringToJsonObjectConverter();

    @Before
    public void setUp() {
        messageProducerClientPublic = publicEvents.createPublicProducer();
        messageConsumerProsecutionCaseDefendantListingStatusChanged = privateEvents.createPrivateConsumer("progression.event.prosecutionCase-defendant-listing-status-changed-v2");
        messageConsumerHearingDeleted = privateEvents.createPrivateConsumer("progression.event.hearing-deleted");
        messageConsumerHearingDeletedForProsecutionCase = privateEvents.createPrivateConsumer("progression.event.hearing-deleted-for-prosecution-case");
        messageConsumerHearingPopulatedToProbationCaseWorker = privateEvents.createPrivateConsumer("progression.events.hearing-populated-to-probation-caseworker");
        messageConsumerDeletedHearingPopulatedToProbationCaseWorker = privateEvents.createPrivateConsumer("progression.events.deleted-hearing-populated-to-probation-caseworker");
        HearingStub.stubInitiateHearing();
        stubDocumentCreate(randomAlphanumeric(20));
    }

    @After
    public void tearDown() throws JMSException {
        messageProducerClientPublic.close();
        messageConsumerHearingPopulatedToProbationCaseWorker.close();
        messageConsumerDeletedHearingPopulatedToProbationCaseWorker.close();
        messageConsumerProsecutionCaseDefendantListingStatusChanged.close();
        messageConsumerHearingDeleted.close();
        messageConsumerHearingDeletedForProsecutionCase.close();
    }

    @Test
    public void shouldDeleteHearingWhenHandlingAllocatedHearingDeleted() throws IOException, JMSException {
        final String userId = randomUUID().toString();
        final String caseId = randomUUID().toString();
        final String defendantId = randomUUID().toString();
        final String hearingId = createHearingOneGrownDefendantAndReturnHearingId(caseId, defendantId);
        LOGGER.info("hearingId : {}", hearingId);

        Metadata metadata = JsonEnvelope.metadataBuilder()
                .withId(randomUUID())
                .withName(PUBLIC_LISTING_HEARING_CONFIRMED)
                .withUserId(userId)
                .build();

        final JsonObject hearingConfirmedJson = getHearingJsonObject("public.listing.hearing-confirmed.json", caseId, hearingId, defendantId, randomUUID().toString(), "Lavender Hill Magistrate's Court", randomUUID().toString());


        try (final MessageConsumer messageConsumerProsecutionCaseDefendantListingStatusChanged = privateEvents
                .createPrivateConsumer("progression.event.prosecutionCase-defendant-listing-status-changed-v2")) {

            sendMessage(messageProducerClientPublic,
                    PUBLIC_LISTING_HEARING_CONFIRMED, hearingConfirmedJson, metadata);

            doVerifyProsecutionCaseDefendantListingStatusChanged();
        }

        metadata = metadataBuilder()
                .withId(randomUUID())
                .withName(PUBLIC_EVENTS_LISTING_ALLOCATED_HEARING_DELETED)
                .withUserId(userId)
                .build();

        final JsonObject hearingDeletedJson = getHearingMarkedAsDeletedObject(hearingId);

        sendMessage(messageProducerClientPublic,
                PUBLIC_EVENTS_LISTING_ALLOCATED_HEARING_DELETED, hearingDeletedJson, metadata);

        verifyInMessagingQueueForHearingDeleted();
        verifyInMessagingQueueForHearingDeletedForProsecutionCase();
        verifyHearingIsEmpty(hearingId);
        verifyInMessagingQueueForHearingPopulatedToProbationCaseWorker(hearingId, messageConsumerDeletedHearingPopulatedToProbationCaseWorker);
    }

    @Test
    public void shouldDeleteHearingWhenHandlingUnallocatedHearingDeleted() throws IOException {
        final String userId = randomUUID().toString();
        final String caseId = randomUUID().toString();
        final String defendantId = randomUUID().toString();
        final String urn = generateUrn();
        createHearingAndReturnHearingId(caseId, defendantId, urn);
        final String hearingId = pollProsecutionCasesProgressionAndReturnHearingId(caseId, defendantId, getProsecutionCaseMatchers(caseId, defendantId));
        LOGGER.info("hearingId : {}", hearingId);

        final Metadata metadata = metadataBuilder()
                .withId(randomUUID())
                .withName(PUBLIC_EVENTS_LISTING_UNALLOCATED_HEARING_DELETED)
                .withUserId(userId)
                .build();

        final JsonObject hearingDeletedJson = getHearingMarkedAsDeletedObject(hearingId);

        sendMessage(messageProducerClientPublic,
                PUBLIC_EVENTS_LISTING_UNALLOCATED_HEARING_DELETED, hearingDeletedJson, metadata);

        verifyInMessagingQueueForHearingDeleted();
        verifyInMessagingQueueForHearingDeletedForProsecutionCase();
        verifyHearingIsEmpty(hearingId);
    }

    @Test
    public void shouldDeleteHearingWhenHandlingHearingDeleted() throws IOException, JMSException {
        final String userId = randomUUID().toString();
        final String caseId = randomUUID().toString();
        final String defendantId = randomUUID().toString();
        final String hearingId = createHearingOneGrownDefendantAndReturnHearingId(caseId, defendantId);
        LOGGER.info("hearingId : {}", hearingId);

        Metadata metadata = JsonEnvelope.metadataBuilder()
                .withId(randomUUID())
                .withName(PUBLIC_LISTING_HEARING_CONFIRMED)
                .withUserId(userId)
                .build();

        final JsonObject hearingConfirmedJson = getHearingJsonObject("public.listing.hearing-confirmed.json", caseId, hearingId, defendantId, randomUUID().toString(), "Lavender Hill Magistrate's Court", randomUUID().toString());
        sendMessage(messageProducerClientPublic,
                    PUBLIC_LISTING_HEARING_CONFIRMED, hearingConfirmedJson, metadata);

        doVerifyProsecutionCaseDefendantListingStatusChanged();

        metadata = metadataBuilder()
                .withId(randomUUID())
                .withName(PUBLIC_EVENTS_LISTING_HEARING_DELETED)
                .withUserId(userId)
                .build();

        final JsonObject hearingDeletedJson = getHearingMarkedAsDeletedObject(hearingId);

        sendMessage(messageProducerClientPublic,
                PUBLIC_EVENTS_LISTING_HEARING_DELETED, hearingDeletedJson, metadata);

        verifyInMessagingQueueForHearingDeleted();
        verifyInMessagingQueueForHearingDeletedForProsecutionCase();
        verifyHearingIsEmpty(hearingId);
        verifyInMessagingQueueForHearingPopulatedToProbationCaseWorker(hearingId, messageConsumerDeletedHearingPopulatedToProbationCaseWorker);
    }

    @Test
    public void shouldReopenCaseWhenAnewApplicationAddedAndHasFutureHearingsAndDeleteHearing() throws IOException, InterruptedException {
        final String caseId = randomUUID().toString();
        final String defendantId = randomUUID().toString();
        final String courtCentreId = UUID.randomUUID().toString();
        final String applicationId = UUID.randomUUID().toString();
        final String userId = UUID.randomUUID().toString();
        final String courtCentreName = "Lavender Hill Magistrate's Court";
        addProsecutionCaseToCrownCourt(caseId, defendantId);
        pollProsecutionCasesProgressionFor(caseId, getProsecutionCaseMatchers(caseId, defendantId));
        final String hearingId = doVerifyProsecutionCaseDefendantListingStatusChanged();
        LOGGER.info("hearingId : {}", hearingId);

        sendMessage(messageProducerClientPublic,
                PUBLIC_HEARING_RESULTED, getHearingWithSingleCaseJsonObject(PUBLIC_HEARING_RESULTED_CASE_UPDATED + ".json", caseId,
                        hearingId, defendantId, courtCentreId, "C", "Remedy", "2593cf09-ace0-4b7d-a746-0703a29f33b5"), JsonEnvelope.metadataBuilder()
                        .withId(randomUUID())
                        .withName(PUBLIC_HEARING_RESULTED)
                        .withUserId(userId)
                        .build());
        doVerifyProsecutionCaseDefendantListingStatusChanged();

        pollProsecutionCasesProgressionFor(caseId, getCaseStatusMatchers(INACTIVE.getDescription(), caseId));

        initiateCourtProceedingsForCourtApplicationWithCourtHearing(applicationId, caseId, hearingId, "applications/progression.initiate-court-proceedings-for-generic-linked-application.json");
        pollForApplication(applicationId);

        sendMessage(messageProducerClientPublic,
                PUBLIC_LISTING_HEARING_CONFIRMED, getHearingJsonObject("public.listing.hearing-confirmed-case-reopen.json",
                        caseId, hearingId, defendantId, courtCentreId, courtCentreName, applicationId), JsonEnvelope.metadataBuilder()
                        .withId(randomUUID())
                        .withName(PUBLIC_LISTING_HEARING_CONFIRMED)
                        .withUserId(userId)
                        .build());
        pollProsecutionCasesProgressionFor(caseId, getCaseStatusMatchers(ACTIVE.getDescription(), caseId));

        final Metadata metadata = metadataBuilder()
                .withId(randomUUID())
                .withName(PUBLIC_EVENTS_LISTING_ALLOCATED_HEARING_DELETED)
                .withUserId(userId)
                .build();

        final JsonObject hearingDeletedJson = getHearingMarkedAsDeletedObject(hearingId);

        sendMessage(messageProducerClientPublic,
                PUBLIC_EVENTS_LISTING_ALLOCATED_HEARING_DELETED, hearingDeletedJson, metadata);

        verifyInMessagingQueueForHearingDeleted();
        verifyInMessagingQueueForHearingDeletedForProsecutionCase();
        verifyHearingIsEmpty(hearingId);
    }

    private String createHearingAndReturnHearingId(final String caseId, final String defendantId, final String urn) throws IOException {
        addProsecutionCaseToCrownCourt(caseId, defendantId, urn);

        pollProsecutionCasesProgressionFor(caseId, getProsecutionCaseMatchers(caseId, defendantId,
                singletonList(withJsonPath("$.prosecutionCase.id", is(caseId)))));

        final JsonPath message = QueueUtil.retrieveMessage(messageConsumerProsecutionCaseDefendantListingStatusChanged, isJson(allOf(withJsonPath("$.hearing.prosecutionCases[0].id", is(caseId)))));
        return message.getJsonObject("hearing.id");
    }

    private String createHearingOneGrownDefendantAndReturnHearingId(final String caseId, final String defendantId) throws IOException {
        addProsecutionCaseToCrownCourtWithOneGrownDefendantAndTwoOffences(caseId, defendantId);

        pollProsecutionCasesProgressionFor(caseId, getProsecutionCaseMatchers(caseId, defendantId,
                singletonList(withJsonPath("$.prosecutionCase.id", is(caseId)))));

        final JsonPath message = QueueUtil.retrieveMessage(messageConsumerProsecutionCaseDefendantListingStatusChanged, isJson(allOf(withJsonPath("$.hearing.prosecutionCases[0].id", is(caseId)))));
        return message.getJsonObject("hearing.id");
    }

    private JsonObject getHearingMarkedAsDeletedObject(final String hearingId) {
        return new StringToJsonObjectConverter().convert(
                getPayload("public.events.listing.hearing-deleted.json")
                        .replaceAll("HEARING_ID", hearingId)
        );
    }

    private JsonObject getHearingWithSingleCaseJsonObject(final String path, final String caseId, final String hearingId,
                                                          final String defendantId, final String courtCentreId, final String bailStatusCode,
                                                          final String bailStatusDescription, final String bailStatusId) {
        return stringToJsonObjectConverter.convert(
                getPayload(path)
                        .replaceAll("CASE_ID", caseId)
                        .replaceAll("HEARING_ID", hearingId)
                        .replaceAll("DEFENDANT_ID", defendantId)
                        .replaceAll("COURT_CENTRE_ID", courtCentreId)
                        .replaceAll("BAIL_STATUS_ID", bailStatusId)
                        .replaceAll("BAIL_STATUS_CODE", bailStatusCode)
                        .replaceAll("BAIL_STATUS_DESCRIPTION", bailStatusDescription)
        );
    }

    private JsonObject getHearingJsonObject(final String path, final String caseId, final String hearingId,
                                            final String defendantId, final String courtCentreId, final String courtCentreName, final String applicationId) {
        final String strPayload = getPayload(path)
                .replaceAll("HEARING_ID", hearingId)
                .replaceAll("COURT_CENTRE_ID", courtCentreId)
                .replaceAll("COURT_CENTRE_NAME", courtCentreName)
                .replaceAll("APPLICATION_ID", applicationId)
                .replaceAll("CASE_ID", caseId)
                .replaceAll("DEFENDANT_ID", defendantId);
        return stringToJsonObjectConverter.convert(strPayload);
    }

    private String doVerifyProsecutionCaseDefendantListingStatusChanged() {
        final Optional<JsonObject> message = QueueUtil.retrieveMessageAsJsonObject(messageConsumerProsecutionCaseDefendantListingStatusChanged);
        final JsonObject prosecutionCaseDefendantListingStatusChanged = message.get();
        return prosecutionCaseDefendantListingStatusChanged.getJsonObject("hearing").getString("id");
    }

    private void verifyInMessagingQueueForHearingDeleted() {
        final Optional<JsonObject> message = QueueUtil.retrieveMessageAsJsonObject(messageConsumerHearingDeleted);
        assertTrue(message.isPresent());
    }

    private  void verifyInMessagingQueueForHearingDeletedForProsecutionCase() {
        final Optional<JsonObject> message = QueueUtil.retrieveMessageAsJsonObject(messageConsumerHearingDeletedForProsecutionCase);
        assertTrue(message.isPresent());
    }

    private Matcher[] getCaseStatusMatchers(final String caseStatus, final String caseId) {
        return new Matcher[]{
                withJsonPath("$.prosecutionCase.id", equalTo(caseId)),
                withJsonPath("$.prosecutionCase.caseStatus", equalTo(caseStatus))

        };
    }
}
