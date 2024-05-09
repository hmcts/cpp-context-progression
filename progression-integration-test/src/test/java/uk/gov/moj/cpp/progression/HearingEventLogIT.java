package uk.gov.moj.cpp.progression;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static java.util.UUID.randomUUID;
import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static uk.gov.justice.services.messaging.JsonEnvelope.metadataBuilder;
import static uk.gov.moj.cpp.progression.applications.applicationHelper.ApplicationHelper.initiateCourtProceedingsForCourtApplication;
import static uk.gov.moj.cpp.progression.domain.constant.CaseStatusEnum.ACTIVE;
import static uk.gov.moj.cpp.progression.domain.constant.CaseStatusEnum.INACTIVE;
import static uk.gov.moj.cpp.progression.helper.AbstractTestHelper.getWriteUrl;
import static uk.gov.moj.cpp.progression.helper.DocumentGenerationHelper.givenCaseIsReferredToMags;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.addProsecutionCaseToCrownCourt;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.getCourtDocumentFor;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.getCourtDocumentsByApplication;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.getCourtDocumentsPerCase;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.initiateCourtProceedingsWithCommittingCourt;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.pollForApplicationStatus;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.pollProsecutionCasesProgressionFor;
import static uk.gov.moj.cpp.progression.helper.QueueUtil.privateEvents;
import static uk.gov.moj.cpp.progression.helper.QueueUtil.publicEvents;
import static uk.gov.moj.cpp.progression.helper.QueueUtil.retrieveMessageAsJsonObject;
import static uk.gov.moj.cpp.progression.helper.QueueUtil.sendMessage;
import static uk.gov.moj.cpp.progression.helper.RestHelper.pollForResponse;
import static uk.gov.moj.cpp.progression.helper.RestHelper.postCommand;
import static uk.gov.moj.cpp.progression.helper.StubUtil.setupHmctsUsersGroupQueryStub;
import static uk.gov.moj.cpp.progression.helper.StubUtil.setupMaterialStub;
import static uk.gov.moj.cpp.progression.it.framework.util.ViewStoreCleaner.cleanViewStoreTables;
import static uk.gov.moj.cpp.progression.stub.AzureScheduleServiceStub.stubGetProvisionalBookedSlotsForExistingBookingId;
import static uk.gov.moj.cpp.progression.stub.DocumentGeneratorStub.getHearingEventTemplate;
import static uk.gov.moj.cpp.progression.stub.MaterialStub.verifyMaterialCreated;
import static uk.gov.moj.cpp.progression.stub.ReferenceDataStub.stubGetDocumentsTypeAccess;
import static uk.gov.moj.cpp.progression.stub.ReferenceDataStub.stubQueryDocumentTypeData;
import static uk.gov.moj.cpp.progression.util.FileUtil.getPayload;
import static uk.gov.moj.cpp.progression.util.ReferProsecutionCaseToCrownCourtHelper.getProsecutionCaseMatchers;
import static uk.gov.moj.cpp.progression.util.WireMockStubUtils.stubAaagHearingEventLogs;
import static uk.gov.moj.cpp.progression.util.WireMockStubUtils.stubGetUserOrganisation;
import static uk.gov.moj.cpp.progression.util.WireMockStubUtils.stubHearingEventLogs;

import uk.gov.justice.core.courts.CourtDocument;
import uk.gov.justice.services.common.converter.StringToJsonObjectConverter;
import uk.gov.justice.services.common.converter.ZonedDateTimes;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.messaging.Metadata;
import uk.gov.moj.cpp.progression.helper.QueueUtil;
import uk.gov.moj.cpp.progression.stub.HearingStub;
import uk.gov.moj.cpp.progression.stub.ReferenceDataStub;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;
import java.util.UUID;

import javax.jms.JMSException;
import javax.jms.MessageConsumer;
import javax.jms.MessageProducer;
import javax.json.JsonObject;

import com.google.common.io.Resources;
import com.jayway.awaitility.Awaitility;
import com.jayway.awaitility.Duration;
import com.jayway.restassured.response.Response;
import org.apache.http.HttpStatus;
import org.hamcrest.Matcher;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

public class HearingEventLogIT extends AbstractIT {

    private static final String PUBLIC_HEARING_RESULTED_CASE_UPDATED = "public.hearing.resulted-case-updated";
    private static final String PUBLIC_LISTING_HEARING_CONFIRMED = "public.listing.hearing-confirmed";
    private static final String PUBLIC_HEARING_RESULTED = "public.hearing.resulted";
    private static final String PUBLIC_PROGRESSION_EVENT_PROSECUTION_CASES_REFERRED_TO_COURT = "public.progression.prosecution-cases-referred-to-court";
    private static final String PUBLIC_PROGRESSION_EVENT_HEARING_EVENTS_LOGS_DOCUMENT_SUCCESS = "public.progression.hearing-event-logs-document-success";
    private static final String PUBLIC_PROGRESSION_EVENT_HEARING_EVENTS_LOGS_DOCUMENT_FAILED = "public.progression.hearing-event-logs-document-failed";
    private static final String PUBLIC_LISTING_HEARING_UPDATED = "public.listing.hearing-updated";

    private final String PROGRESSION_QUERY_CASE_STATUS_FOR_APPLICATION = "application/vnd.progression.query.case.status-for-application+json";
    private final StringToJsonObjectConverter stringToJsonObjectConverter = new StringToJsonObjectConverter();
    private final UUID documentTypeId = UUID.fromString("460fae22-c002-11e8-a355-529269fb1459");
    private final String TEMPLATE_NAME = "HearingEventLog";

    private Path testResourceBasePath;

    private MessageProducer messageProducerClientPublic;
    private MessageConsumer messageConsumerClientPublicForReferToCourtOnHearingInitiated;
    private MessageConsumer messageConsumerClientPublicForHearingEventsLogsDocumentSucess;
    private MessageConsumer messageConsumerClientPublicForHearingEventsLogsDocumentFailed;
    private MessageConsumer messageConsumerHearingPopulatedToProbationCaseWorker;
    private MessageConsumer messageConsumerCaseRetentionPolicyRecorded;
    private MessageConsumer messageConsumerCaseRetentionLengthCalculated;
    private MessageConsumer messageConsumerHearingLogDocumentCreated;
    private MessageConsumer messageConsumerProsecutionCaseDefendantListingStatusChanged;

    private static String getPayloadForCreatingRequest(final String ramlPath) {
        String request = null;
        try {
            request = Resources.toString(
                    Resources.getResource(ramlPath),
                    Charset.defaultCharset()
            );
        } catch (final Exception e) {
            fail("Error consuming file from location " + ramlPath);
        }
        return request;
    }

    @Before
    public void setUp() {
        cleanViewStoreTables();
        messageProducerClientPublic = publicEvents.createPublicProducer();
        messageConsumerClientPublicForReferToCourtOnHearingInitiated = publicEvents
                .createPublicConsumer(PUBLIC_PROGRESSION_EVENT_PROSECUTION_CASES_REFERRED_TO_COURT);
        messageConsumerClientPublicForHearingEventsLogsDocumentSucess = publicEvents
                .createPublicConsumer(PUBLIC_PROGRESSION_EVENT_HEARING_EVENTS_LOGS_DOCUMENT_SUCCESS);
        messageConsumerClientPublicForHearingEventsLogsDocumentFailed = publicEvents
                .createPublicConsumer(PUBLIC_PROGRESSION_EVENT_HEARING_EVENTS_LOGS_DOCUMENT_FAILED);
        messageConsumerCaseRetentionPolicyRecorded = privateEvents.createPrivateConsumer("progression.event.case-retention-policy-recorded");
        messageConsumerCaseRetentionLengthCalculated = privateEvents.createPrivateConsumer("progression.events.case-retention-length-calculated");
        messageConsumerHearingLogDocumentCreated = privateEvents.createPrivateConsumer("progression.event.hearing-event-logs-document-created");
        messageConsumerHearingPopulatedToProbationCaseWorker = privateEvents.createPrivateConsumer("progression.events.hearing-populated-to-probation-caseworker");
        messageConsumerProsecutionCaseDefendantListingStatusChanged = privateEvents
                .createPrivateConsumer("progression.event.prosecutionCase-defendant-listing-status-changed-v2");
        HearingStub.stubInitiateHearing();
        testResourceBasePath = Paths.get("hearing-event-log");
        ReferenceDataStub.stubQueryDocumentTypeData("/restResource/ref-data-document-type.json", documentTypeId.toString());
        stubGetDocumentsTypeAccess("/restResource/get-all-document-type-access.json");
        givenCaseIsReferredToMags(null, TEMPLATE_NAME);
    }

    @After
    public void tearDown() throws JMSException {
        messageProducerClientPublic.close();
        messageConsumerClientPublicForReferToCourtOnHearingInitiated.close();
        messageConsumerClientPublicForHearingEventsLogsDocumentSucess.close();
        messageConsumerHearingPopulatedToProbationCaseWorker.close();
        messageConsumerCaseRetentionPolicyRecorded.close();
        messageConsumerCaseRetentionLengthCalculated.close();
        messageConsumerHearingLogDocumentCreated.close();
        messageConsumerClientPublicForHearingEventsLogsDocumentFailed.close();
        messageConsumerProsecutionCaseDefendantListingStatusChanged.close();
        stubQueryDocumentTypeData("/restResource/ref-data-document-type.json");
        stubGetDocumentsTypeAccess("/restResource/get-all-document-type-access.json");
        givenCaseIsReferredToMags(null, TEMPLATE_NAME);
    }

    public void verifyInMessagingQueueForCasesReferredToCourts() {
        final Optional<JsonObject> message = QueueUtil.retrieveMessageAsJsonObject(messageConsumerClientPublicForReferToCourtOnHearingInitiated);
        Awaitility.await().atMost(Duration.TEN_SECONDS).until(() -> assertThat(message.isPresent(), is(true)));
    }

    public void verifyInMessagingQueueForHearingEventLogsDocumentSuccess() {
        final Optional<JsonObject> message = QueueUtil.retrieveMessageAsJsonObject(messageConsumerClientPublicForHearingEventsLogsDocumentSucess);
        assertTrue(message.isPresent());
    }

    public void verifyInMessagingQueueForHearingEventLogsDocumentFailed() {
        final Optional<JsonObject> message = QueueUtil.retrieveMessageAsJsonObject(messageConsumerClientPublicForHearingEventsLogsDocumentFailed);
        assertTrue(message.isPresent());
    }

    @Ignore("need to fix by DD-32605")
    @Test
    public void shouldGenereateCAAGHearingEventLogDocumentForInActiveCaseIfNoApplicationExists() throws Exception {
        final String userId = randomUUID().toString();
        final String caseId = randomUUID().toString();
        final String defendantId = randomUUID().toString();
        final String courtCentreId = UUID.fromString("111bdd2a-6b7a-4002-bc8c-5c6f93844f40").toString();
        final String courtCentreName = "Lavender Hill Magistrate's Court";
        final String newCourtCentreId = UUID.fromString("999bdd2a-6b7a-4002-bc8c-5c6f93844f40").toString();
        final String newCourtCentreName = "Narnia Magistrate's Court";
        final String materialId = randomUUID().toString();
        final String reportingRestrictionId = randomUUID().toString();
        final String courtDocumentId = randomUUID().toString();
        String hearingId;
        Optional<String> applicationId = Optional.empty();

        stubGetProvisionalBookedSlotsForExistingBookingId();
        removeAnyExistingCaseRetentionMessagesFromQueue();
        givenCaseIsReferredToMags(null, TEMPLATE_NAME);
        setupMaterialStub(materialId);

        final String hearinEventLogResponsePayload = getPayload("stub-data/hearing.get-hearing-event-log-document.json")
                .replace("%CASE_ID%", caseId);
        stubHearingEventLogs(caseId, hearinEventLogResponsePayload);

        final String listedStartDateTime = ZonedDateTimes.fromString("2020-12-15T18:32:04.238Z").toString();
        final String earliestStartDateTime = ZonedDateTimes.fromString("2020-12-15T18:32:04.238Z").toString();
        try (final MessageConsumer messageConsumerProsecutionCaseDefendantListingStatusChanged = privateEvents
                .createPrivateConsumer("progression.event.prosecutionCase-defendant-listing-status-changed-v2")) {
            initiateCourtProceedingsWithCommittingCourt(caseId, defendantId, listedStartDateTime, earliestStartDateTime);
            pollProsecutionCasesProgressionFor(caseId, getProsecutionCaseMatchers(caseId, defendantId));

            hearingId = doVerifyProsecutionCaseDefendantListingStatusChanged(messageConsumerProsecutionCaseDefendantListingStatusChanged);
        }

        final Metadata metadata = metadataBuilder()
                .withId(randomUUID())
                .withName(PUBLIC_LISTING_HEARING_CONFIRMED)
                .withUserId(userId)
                .build();

        final JsonObject hearingConfirmedJson = getHearingJsonObject("public.listing.hearing-confirmed.json", caseId, hearingId, defendantId, courtCentreId, courtCentreName);

        try (final MessageConsumer messageConsumerProsecutionCaseDefendantListingStatusChanged = privateEvents
                .createPrivateConsumer("progression.event.prosecutionCase-defendant-listing-status-changed-v2")) {

            sendMessage(messageProducerClientPublic,
                    PUBLIC_LISTING_HEARING_CONFIRMED, hearingConfirmedJson, metadata);

            doVerifyProsecutionCaseDefendantListingStatusChanged(messageConsumerProsecutionCaseDefendantListingStatusChanged);
        }

        verifyInMessagingQueueForCasesReferredToCourts();
        try (final MessageConsumer messageConsumerProsecutionCaseResulted = privateEvents
                .createPrivateConsumer("progression.event.prosecution-cases-resulted")) {

            sendMessage(messageProducerClientPublic,
                    PUBLIC_HEARING_RESULTED, getHearingJsonObject("public.hearing.resulted-with-crown-committing-court.json", caseId,
                            hearingId, defendantId, newCourtCentreId, newCourtCentreName, reportingRestrictionId), metadataBuilder()
                            .withId(randomUUID())
                            .withName(PUBLIC_HEARING_RESULTED)
                            .withUserId(userId)
                            .build());
            final Optional<JsonObject> message = retrieveMessageAsJsonObject(messageConsumerProsecutionCaseResulted);
            final JsonObject prosecutionCaseResulted = message.get();
            assertThat(prosecutionCaseResulted.getJsonObject("hearing").getString("id"), is(hearingId));
        }

        final String payload = pollProsecutionCasesProgressionFor(caseId, getCaseStatusMatchers(INACTIVE.getDescription(), caseId));
        final JsonObject casePayload = stringToJsonObjectConverter.convert(payload);
        assertEquals("INACTIVE", casePayload.getJsonObject("prosecutionCase").getString("caseStatus"));

        verifyHearingEventsLogsDocumentRequested(courtDocumentId, caseId, defendantId, materialId, applicationId, "INACTIVE");
        verifyMaterialCreated();
        verifyInMessagingQueueForHearingEventLogsDocumentSuccess();
    }

    @Ignore("need to fix by DD-32605")
    @Test
    public void shouldGenereateCAAGHearingEventLogDocumentForActiveCaseIfNoApplicationExists() throws Exception {
        final String TEMPLATE_NAME = "HearingEventLog";
        final String userId = randomUUID().toString();
        final String caseId = randomUUID().toString();
        final String defendantId = randomUUID().toString();
        final String courtCentreId = UUID.fromString("111bdd2a-6b7a-4002-bc8c-5c6f93844f40").toString();
        final String courtCentreName = "Croydon Crown Court";
        final String materialId = randomUUID().toString();
        final String courtDocumentId = randomUUID().toString();
        String hearingId;


        Optional<String> applicationId = Optional.empty();
        stubGetProvisionalBookedSlotsForExistingBookingId();

        givenCaseIsReferredToMags(null, TEMPLATE_NAME);
        //search for the document by application id
        ReferenceDataStub.stubQueryDocumentTypeData("/restResource/ref-data-document-type.json", documentTypeId.toString());
        stubGetDocumentsTypeAccess("/restResource/get-all-document-type-access.json");
        setupMaterialStub(materialId);
        final String loggedInUsersResponsePayload = getPayload("stub-data/usersgroups.get-groups-by-hmcts-user.json");
        setupHmctsUsersGroupQueryStub(getPayload("stub-data/usersgroups.get-groups-by-hmcts-user.json").replace("%USER_ID%", userId));
        final JsonObject loggedInUserObject = stringToJsonObjectConverter.convert(loggedInUsersResponsePayload);
        final String organisation = getPayload("stub-data/usersgroups.get-hmcts-organisation-details.json")
                .replace("%ORGANISATION_ID%", loggedInUserObject.getString("organisationId"));
        stubGetUserOrganisation(loggedInUserObject.getString("organisationId"), organisation);

        final String hearinEventLogResponsePayload = getPayload("stub-data/hearing.get-hearing-event-log-document.json")
                .replace("%CASE_ID%", caseId);
        stubHearingEventLogs(caseId, hearinEventLogResponsePayload);

        addProsecutionCaseToCrownCourt(caseId, defendantId);
        pollProsecutionCasesProgressionFor(caseId, getProsecutionCaseMatchers(caseId, defendantId));

        final String listedStartDateTime = ZonedDateTimes.fromString("2020-12-15T18:32:04.238Z").toString();
        final String earliestStartDateTime = ZonedDateTimes.fromString("2020-12-15T18:32:04.238Z").toString();
        try (final MessageConsumer messageConsumerProsecutionCaseDefendantListingStatusChanged = privateEvents
                .createPrivateConsumer("progression.event.prosecutionCase-defendant-listing-status-changed-v2")) {
            initiateCourtProceedingsWithCommittingCourt(caseId, defendantId, listedStartDateTime, earliestStartDateTime);
            pollProsecutionCasesProgressionFor(caseId, getProsecutionCaseMatchers(caseId, defendantId));

            hearingId = doVerifyProsecutionCaseDefendantListingStatusChanged(messageConsumerProsecutionCaseDefendantListingStatusChanged);
        }

        final Metadata metadata = metadataBuilder()
                .withId(randomUUID())
                .withName(PUBLIC_LISTING_HEARING_CONFIRMED)
                .withUserId(userId)
                .build();

        final JsonObject hearingConfirmedJson = getHearingJsonObject("public.listing.hearing-confirmed.json", caseId, hearingId, defendantId, courtCentreId, courtCentreName);

        try (final MessageConsumer messageConsumerProsecutionCaseDefendantListingStatusChanged = privateEvents
                .createPrivateConsumer("progression.event.prosecutionCase-defendant-listing-status-changed-v2")) {

            sendMessage(messageProducerClientPublic,
                    PUBLIC_LISTING_HEARING_CONFIRMED, hearingConfirmedJson, metadata);

            doVerifyProsecutionCaseDefendantListingStatusChanged(messageConsumerProsecutionCaseDefendantListingStatusChanged);
        }

        final String payload = pollProsecutionCasesProgressionFor(caseId, getCaseStatusMatchers(ACTIVE.getDescription(), caseId));
        final JsonObject caseObject = stringToJsonObjectConverter.convert(payload);

        verifyInMessagingQueueForCasesReferredToCourts();
        verifyCaagHearingEventLog(caseId);
        verifyHearingEventsLogsDocumentRequested(courtDocumentId, caseId, defendantId, materialId, applicationId, caseObject.getJsonObject("prosecutionCase").getString("caseStatus"));
        verifyInMessagingQueueForHearingEventLogsDocumentSuccess();
    }

    @Test
    public void shouldNotGenereateCAAGHearingEventLogDocumentForActiveCaseIfNoHearingEventLogs() throws Exception {

        final String caseId = randomUUID().toString();
        final String userId = randomUUID().toString();
        final String defendantId = randomUUID().toString();
        final String courtCentreId = UUID.fromString("111bdd2a-6b7a-4002-bc8c-5c6f93844f40").toString();
        String hearingId;

        final String loggedInUsersResponsePayload = getPayload("stub-data/usersgroups.get-groups-by-hmcts-user.json");
        setupHmctsUsersGroupQueryStub(getPayload("stub-data/usersgroups.get-groups-by-hmcts-user.json").replace("%USER_ID%", userId));
        final JsonObject loggedInUserObject = stringToJsonObjectConverter.convert(loggedInUsersResponsePayload);
        final String organisation = getPayload("stub-data/usersgroups.get-hmcts-organisation-details.json")
                .replace("%ORGANISATION_ID%", loggedInUserObject.getString("organisationId"));
        stubGetUserOrganisation(loggedInUserObject.getString("organisationId"), organisation);
        final String hearinEventLogResponsePayload = getPayload("stub-data/hearing.get-no-hearing-event-log-document.json");
        stubHearingEventLogs(caseId, hearinEventLogResponsePayload);

        addProsecutionCaseToCrownCourt(caseId, defendantId);
        pollProsecutionCasesProgressionFor(caseId, getProsecutionCaseMatchers(caseId, defendantId));
        hearingId = doVerifyProsecutionCaseDefendantListingStatusChanged(messageConsumerProsecutionCaseDefendantListingStatusChanged);

        try (final MessageConsumer publicHearingDetailChangedConsumer = publicEvents.createPublicConsumer("public.hearing-detail-changed")) {

            sendMessage(messageProducerClientPublic,
                    PUBLIC_LISTING_HEARING_UPDATED, getHearingJsonObject("public.listing.hearing-updated.json", caseId,
                            hearingId, defendantId, courtCentreId, "Croydon Crown Court"), JsonEnvelope.metadataBuilder()
                            .withId(randomUUID())
                            .withName(PUBLIC_LISTING_HEARING_UPDATED)
                            .withUserId(userId)
                            .build());

            assertThat(retrieveMessageAsJsonObject(publicHearingDetailChangedConsumer).isPresent(), is(true));

            pollProsecutionCasesProgressionFor(caseId, new Matcher[]{
                    withJsonPath("$.prosecutionCase.id", equalTo(caseId)),
                    withJsonPath("$.hearingsAtAGlance.hearings[0].courtCentre.id", equalTo(courtCentreId))
            });
        }
        verifyCaagHearingEventLog(caseId);
        verifyInMessagingQueueForHearingEventLogsDocumentFailed();
    }

    @Test
    public void shouldNotGenereateAAAGHearingEventLogDocumentForActiveCaseApplicationIfNoHearingEventLog() throws Exception {

        Optional<String> applicationId = Optional.of(randomUUID().toString());
        final String caseId = randomUUID().toString();
        final String userId = randomUUID().toString();
        final String defendantId = randomUUID().toString();
        final String courtCentreId = UUID.fromString("111bdd2a-6b7a-4002-bc8c-5c6f93844f40").toString();
        String hearingId;

        final String loggedInUsersResponsePayload = getPayload("stub-data/usersgroups.get-groups-by-hmcts-user.json");
        setupHmctsUsersGroupQueryStub(loggedInUsersResponsePayload.replace("%USER_ID%", userId));
        final JsonObject loggedInUserObject = stringToJsonObjectConverter.convert(loggedInUsersResponsePayload);
        final String organisation = getPayload("stub-data/usersgroups.get-hmcts-organisation-details.json")
                .replace("%ORGANISATION_ID%", loggedInUserObject.getString("organisationId"));
        stubGetUserOrganisation(loggedInUserObject.getString("organisationId"), organisation);

        final String hearinEventLogResponsePayload = getPayload("stub-data/hearing.get-no-hearing-event-log-document-application.json");
        stubAaagHearingEventLogs(applicationId.get(), hearinEventLogResponsePayload);
        addProsecutionCaseToCrownCourt(caseId, defendantId);
        pollProsecutionCasesProgressionFor(caseId, getProsecutionCaseMatchers(caseId, defendantId));
        hearingId = doVerifyProsecutionCaseDefendantListingStatusChanged(messageConsumerProsecutionCaseDefendantListingStatusChanged);

        try (final MessageConsumer publicHearingDetailChangedConsumer = publicEvents.createPublicConsumer("public.hearing-detail-changed")) {

            sendMessage(messageProducerClientPublic,
                    PUBLIC_LISTING_HEARING_UPDATED, getHearingJsonObject("public.listing.hearing-updated.json", caseId,
                            hearingId, defendantId, courtCentreId, "Croydon Crown Court"), JsonEnvelope.metadataBuilder()
                            .withId(randomUUID())
                            .withName(PUBLIC_LISTING_HEARING_UPDATED)
                            .withUserId(userId)
                            .build());
            Awaitility.await().atMost(Duration.TEN_SECONDS).until(() -> assertThat(retrieveMessageAsJsonObject(publicHearingDetailChangedConsumer).isPresent(), is(true)));

            pollProsecutionCasesProgressionFor(caseId, new Matcher[]{
                    withJsonPath("$.prosecutionCase.id", equalTo(caseId)),
                    withJsonPath("$.hearingsAtAGlance.hearings[0].courtCentre.id", equalTo(courtCentreId))
            });
        }

        initiateCourtProceedingsForCourtApplication(applicationId.get(), caseId, randomUUID().toString(), "applications/progression.initiate-court-proceedings-for-generic-linked-application.json");

        pollForApplicationStatus(applicationId.get(), "DRAFT");

        String caseStatus = pollForResponse("/applications/" + applicationId.get() + "/case/" + caseId, PROGRESSION_QUERY_CASE_STATUS_FOR_APPLICATION);
        JsonObject caseObj = stringToJsonObjectConverter.convert(caseStatus);
        assertThat(caseObj.getString("caseStatus"), is(notNullValue()));

        verifyAaagHearingEventLog(caseId, applicationId.get());
        verifyInMessagingQueueForHearingEventLogsDocumentFailed();

    }


    @Test
    public void shouldGenereateAAAGHearingEventLogDocumentForActiveCaseIfApplicationExists() throws Exception {

        Optional<String> applicationId = Optional.of(randomUUID().toString());
        final String caseId = randomUUID().toString();
        final String userId = randomUUID().toString();
        final String defendantId = randomUUID().toString();
        final String courtCentreId = UUID.fromString("111bdd2a-6b7a-4002-bc8c-5c6f93844f40").toString();
        final String materialId = randomUUID().toString();
        final String courtDocumentId = randomUUID().toString();
        String hearingId;

        final String loggedInUsersResponsePayload = getPayload("stub-data/usersgroups.get-groups-by-hmcts-user.json");
        setupHmctsUsersGroupQueryStub(loggedInUsersResponsePayload.replace("%USER_ID%", userId));
        final JsonObject loggedInUserObject = stringToJsonObjectConverter.convert(loggedInUsersResponsePayload);
        final String organisation = getPayload("stub-data/usersgroups.get-hmcts-organisation-details.json")
                .replace("%ORGANISATION_ID%", loggedInUserObject.getString("organisationId"));
        stubGetUserOrganisation(loggedInUserObject.getString("organisationId"), organisation);
        final String hearinEventLogResponsePayload = getPayload("stub-data/hearing.get-aaag-hearing-event-log-document.json")
                .replace("%CASE_ID%", caseId)
                .replace("%APPLICATION_ID%", applicationId.get());
        stubAaagHearingEventLogs(applicationId.get(), hearinEventLogResponsePayload);
        addProsecutionCaseToCrownCourt(caseId, defendantId);
        pollProsecutionCasesProgressionFor(caseId, getProsecutionCaseMatchers(caseId, defendantId));
        hearingId = doVerifyProsecutionCaseDefendantListingStatusChanged(messageConsumerProsecutionCaseDefendantListingStatusChanged);
        //givenCaseIsReferredToMags(null, TEMPLATE_NAME);
        //search for the document by application id
        ReferenceDataStub.stubQueryDocumentTypeData("/restResource/ref-data-document-type.json", documentTypeId.toString());
        stubGetDocumentsTypeAccess("/restResource/get-all-document-type-access.json");
        setupMaterialStub(materialId);

        try (final MessageConsumer publicHearingDetailChangedConsumer = publicEvents.createPublicConsumer("public.hearing-detail-changed")) {

            sendMessage(messageProducerClientPublic,
                    PUBLIC_LISTING_HEARING_UPDATED, getHearingJsonObject("public.listing.hearing-updated.json", caseId,
                            hearingId, defendantId, courtCentreId, "Croydon Crown Court"), JsonEnvelope.metadataBuilder()
                            .withId(randomUUID())
                            .withName(PUBLIC_LISTING_HEARING_UPDATED)
                            .withUserId(userId)
                            .build());
            Awaitility.await().atMost(Duration.TEN_SECONDS).until(() -> assertThat(retrieveMessageAsJsonObject(publicHearingDetailChangedConsumer).isPresent(), is(true)));

            pollProsecutionCasesProgressionFor(caseId, new Matcher[]{
                    withJsonPath("$.prosecutionCase.id", equalTo(caseId)),
                    withJsonPath("$.hearingsAtAGlance.hearings[0].courtCentre.id", equalTo(courtCentreId))
            });
        }

        initiateCourtProceedingsForCourtApplication(applicationId.get(), caseId, randomUUID().toString(), "applications/progression.initiate-court-proceedings-for-generic-linked-application.json");

        pollForApplicationStatus(applicationId.get(), "DRAFT");

        String caseStatus = pollForResponse("/applications/" + applicationId.get() + "/case/" + caseId, PROGRESSION_QUERY_CASE_STATUS_FOR_APPLICATION);
        JsonObject caseObj = stringToJsonObjectConverter.convert(caseStatus);
        assertThat(caseObj.getString("caseStatus"), is(notNullValue()));

        verifyAaagHearingEventLog(caseId, applicationId.get());
        verifyHearingEventsLogsDocumentRequested(courtDocumentId, caseId, defendantId, materialId, applicationId, caseObj.getString("caseStatus"));
        verifyInMessagingQueueForHearingEventLogsDocumentSuccess();

    }


    @Test
    public void shouldGenereateAAAGHearingEventLogDocumentForInActiveCaseIfApplicationExists() throws Exception {
        final String TEMPLATE_NAME = "HearingEventLog";
        final String userId = randomUUID().toString();
        final String caseId = randomUUID().toString();
        final String defendantId = randomUUID().toString();
        final String courtCentreId = UUID.fromString("111bdd2a-6b7a-4002-bc8c-5c6f93844f40").toString();
        final String materialId = randomUUID().toString();
        final String courtDocumentId = randomUUID().toString();
        String hearingId;

        Optional<String> applicationId = Optional.of(randomUUID().toString());
        final String loggedInUsersResponsePayload = getPayload("stub-data/usersgroups.get-groups-by-hmcts-user.json");
        setupHmctsUsersGroupQueryStub(loggedInUsersResponsePayload.replace("%USER_ID%", userId));
        final JsonObject loggedInUserObject = stringToJsonObjectConverter.convert(loggedInUsersResponsePayload);
        final String organisation = getPayload("stub-data/usersgroups.get-hmcts-organisation-details.json")
                .replace("%ORGANISATION_ID%", loggedInUserObject.getString("organisationId"));
        stubGetUserOrganisation(loggedInUserObject.getString("organisationId"), organisation);
        givenCaseIsReferredToMags(null, TEMPLATE_NAME);
        //search for the document by application id
        ReferenceDataStub.stubQueryDocumentTypeData("/restResource/ref-data-document-type.json", documentTypeId.toString());
        stubGetDocumentsTypeAccess("/restResource/get-all-document-type-access.json");
        setupMaterialStub(materialId);
        final String hearinEventLogResponsePayload = getPayload("stub-data/hearing.get-aaag-hearing-event-log-document.json")
                .replace("%CASE_ID%", caseId)
                .replace("%APPLICATION_ID%", applicationId.get());
        stubAaagHearingEventLogs(applicationId.get(), hearinEventLogResponsePayload);

        addProsecutionCaseToCrownCourt(caseId, defendantId);
        pollProsecutionCasesProgressionFor(caseId, getProsecutionCaseMatchers(caseId, defendantId));
        hearingId = doVerifyProsecutionCaseDefendantListingStatusChanged(messageConsumerProsecutionCaseDefendantListingStatusChanged);

        sendMessage(messageProducerClientPublic,
                PUBLIC_HEARING_RESULTED, getHearingWithSingleCaseJsonObject(PUBLIC_HEARING_RESULTED_CASE_UPDATED + ".json", caseId,
                        hearingId, defendantId, courtCentreId, "C", "Remedy", "2593cf09-ace0-4b7d-a746-0703a29f33b5"), JsonEnvelope.metadataBuilder()
                        .withId(randomUUID())
                        .withName(PUBLIC_HEARING_RESULTED)
                        .withUserId(userId)
                        .build());


        initiateCourtProceedingsForCourtApplication(applicationId.get(), caseId, randomUUID().toString(), "applications/progression.initiate-court-proceedings-for-generic-linked-application.json");
        final String payload = pollProsecutionCasesProgressionFor(caseId, getCaseStatusMatchers(INACTIVE.getDescription(), caseId));
        final JsonObject casePayload = stringToJsonObjectConverter.convert(payload);
        assertEquals("INACTIVE", casePayload.getJsonObject("prosecutionCase").getString("caseStatus"));

        pollForApplicationStatus(applicationId.get(), "DRAFT");

        String caseStatus = pollForResponse("/applications/" + applicationId.get() + "/case/" + caseId, PROGRESSION_QUERY_CASE_STATUS_FOR_APPLICATION);
        JsonObject caseObj = stringToJsonObjectConverter.convert(caseStatus);
        assertEquals("INACTIVE", caseObj.getString("caseStatus"));

        verifyAaagHearingEventLog(caseId, applicationId.get());
        verifyHearingEventsLogsDocumentRequested(courtDocumentId, caseId, defendantId, materialId, applicationId, "INACTIVE");
        verifyInMessagingQueueForHearingEventLogsDocumentSuccess();
    }

    @Test
    public void shouldNotGenereateAAAGHearingEventLogDocumentForNonHmctsUser() throws Exception {

        Optional<String> applicationId = Optional.of(randomUUID().toString());
        final String caseId = randomUUID().toString();
        final String userId = randomUUID().toString();
        final String defendantId = randomUUID().toString();
        final String courtCentreId = UUID.fromString("111bdd2a-6b7a-4002-bc8c-5c6f93844f40").toString();
        String hearingId;

        final String loggedInUsersResponsePayload = getPayload("stub-data/usersgroups.get-groups-by-hmcts-user.json");
        setupHmctsUsersGroupQueryStub(loggedInUsersResponsePayload.replace("%USER_ID%", userId));
        final JsonObject loggedInUserObject = stringToJsonObjectConverter.convert(loggedInUsersResponsePayload);
        final String organisation = getPayload("stub-data/usersgroups.get-non-hmcts-organisation-details.json")
                .replace("%ORGANISATION_ID%", loggedInUserObject.getString("organisationId"));
        stubGetUserOrganisation(loggedInUserObject.getString("organisationId"), organisation);
        addProsecutionCaseToCrownCourt(caseId, defendantId);
        pollProsecutionCasesProgressionFor(caseId, getProsecutionCaseMatchers(caseId, defendantId));
        hearingId = doVerifyProsecutionCaseDefendantListingStatusChanged(messageConsumerProsecutionCaseDefendantListingStatusChanged);

        try (final MessageConsumer publicHearingDetailChangedConsumer = publicEvents.createPublicConsumer("public.hearing-detail-changed")) {

            sendMessage(messageProducerClientPublic,
                    PUBLIC_LISTING_HEARING_UPDATED, getHearingJsonObject("public.listing.hearing-updated.json", caseId,
                            hearingId, defendantId, courtCentreId, "Croydon Crown Court"), JsonEnvelope.metadataBuilder()
                            .withId(randomUUID())
                            .withName(PUBLIC_LISTING_HEARING_UPDATED)
                            .withUserId(userId)
                            .build());
            Awaitility.await().atMost(Duration.TEN_SECONDS).until(() -> assertThat(retrieveMessageAsJsonObject(publicHearingDetailChangedConsumer).isPresent(), is(true)));

            pollProsecutionCasesProgressionFor(caseId, new Matcher[]{
                    withJsonPath("$.prosecutionCase.id", equalTo(caseId)),
                    withJsonPath("$.hearingsAtAGlance.hearings[0].courtCentre.id", equalTo(courtCentreId))
            });
        }

        initiateCourtProceedingsForCourtApplication(applicationId.get(), caseId, randomUUID().toString(), "applications/progression.initiate-court-proceedings-for-generic-linked-application.json");

        pollForApplicationStatus(applicationId.get(), "DRAFT");

        String caseStatus = pollForResponse("/applications/" + applicationId.get() + "/case/" + caseId, PROGRESSION_QUERY_CASE_STATUS_FOR_APPLICATION);
        JsonObject caseObj = stringToJsonObjectConverter.convert(caseStatus);
        assertThat(caseObj.getString("caseStatus"), is(notNullValue()));

        verifyAaagHearingEventLog(caseId, applicationId.get());
        final Optional<JsonObject> message = retrieveMessageAsJsonObject(messageConsumerHearingLogDocumentCreated);
        assertThat(!message.isPresent(), is(true));
    }

    @Test
    public void shouldNotGenereateCAAGHearingEventLogDocumentForNonHmctsUser() throws Exception {
        final String TEMPLATE_NAME = "HearingEventLog";
        final String userId = randomUUID().toString();
        final String caseId = randomUUID().toString();
        final String defendantId = randomUUID().toString();
        final String courtCentreId = UUID.fromString("111bdd2a-6b7a-4002-bc8c-5c6f93844f40").toString();
        final String courtCentreName = "Croydon Crown Court";
        String hearingId;

        stubGetProvisionalBookedSlotsForExistingBookingId();
        givenCaseIsReferredToMags(null, TEMPLATE_NAME);
        //search for the document by application id
        final String loggedInUsersResponsePayload = getPayload("stub-data/usersgroups.get-groups-by-hmcts-user.json");
        setupHmctsUsersGroupQueryStub(loggedInUsersResponsePayload.replace("%USER_ID%", userId));
        final JsonObject loggedInUserObject = stringToJsonObjectConverter.convert(loggedInUsersResponsePayload);
        final String organisation = getPayload("stub-data/usersgroups.get-non-hmcts-organisation-details.json")
                .replace("%ORGANISATION_ID%", loggedInUserObject.getString("organisationId"));
        stubGetUserOrganisation(loggedInUserObject.getString("organisationId"), organisation);
        final String hearinEventLogResponsePayload = getPayload("stub-data/hearing.get-hearing-event-log-document.json")
                .replace("%CASE_ID%", caseId);
        stubHearingEventLogs(caseId, hearinEventLogResponsePayload);

        addProsecutionCaseToCrownCourt(caseId, defendantId);
        pollProsecutionCasesProgressionFor(caseId, getProsecutionCaseMatchers(caseId, defendantId));

        final String listedStartDateTime = ZonedDateTimes.fromString("2020-12-15T18:32:04.238Z").toString();
        final String earliestStartDateTime = ZonedDateTimes.fromString("2020-12-15T18:32:04.238Z").toString();
        try (final MessageConsumer messageConsumerProsecutionCaseDefendantListingStatusChanged = privateEvents
                .createPrivateConsumer("progression.event.prosecutionCase-defendant-listing-status-changed-v2")) {
            initiateCourtProceedingsWithCommittingCourt(caseId, defendantId, listedStartDateTime, earliestStartDateTime);
            pollProsecutionCasesProgressionFor(caseId, getProsecutionCaseMatchers(caseId, defendantId));

            hearingId = doVerifyProsecutionCaseDefendantListingStatusChanged(messageConsumerProsecutionCaseDefendantListingStatusChanged);
        }

        final Metadata metadata = metadataBuilder()
                .withId(randomUUID())
                .withName(PUBLIC_LISTING_HEARING_CONFIRMED)
                .withUserId(userId)
                .build();

        final JsonObject hearingConfirmedJson = getHearingJsonObject("public.listing.hearing-confirmed.json", caseId, hearingId, defendantId, courtCentreId, courtCentreName);

        try (final MessageConsumer messageConsumerProsecutionCaseDefendantListingStatusChanged = privateEvents
                .createPrivateConsumer("progression.event.prosecutionCase-defendant-listing-status-changed-v2")) {

            sendMessage(messageProducerClientPublic,
                    PUBLIC_LISTING_HEARING_CONFIRMED, hearingConfirmedJson, metadata);

            doVerifyProsecutionCaseDefendantListingStatusChanged(messageConsumerProsecutionCaseDefendantListingStatusChanged);
        }

        verifyInMessagingQueueForCasesReferredToCourts();
        verifyCaagHearingEventLog(caseId);
        //Then
        final Optional<JsonObject> message = retrieveMessageAsJsonObject(messageConsumerHearingLogDocumentCreated);
        assertThat(!message.isPresent(), is(true));
    }

    private void verifyCaagHearingEventLog(final String caseId) throws IOException {
        final String commandUri = getWriteUrl("/hearingeventlog/" + caseId);
        final Response response = postCommand(commandUri,
                "application/vnd.progression.create-hearing-event-log-document+json",
                "{}");
        assertThat(response.getStatusCode(), equalTo(HttpStatus.SC_ACCEPTED));
    }

    private void verifyAaagHearingEventLog(final String caseId, final String applicationId) throws IOException {
        final String commandUri = getWriteUrl("/hearingeventlog/case/" + caseId + "/application/" + applicationId);
        final Response response = postCommand(commandUri,
                "application/vnd.progression.create-aaag-hearing-event-log-document+json",
                "{}");
        Awaitility.await().atMost(Duration.TEN_SECONDS).until(() -> assertThat(response.getStatusCode(), is(HttpStatus.SC_ACCEPTED)));
    }

    private void removeAnyExistingCaseRetentionMessagesFromQueue() {
        retrieveMessageAsJsonObject(messageConsumerCaseRetentionPolicyRecorded);
        retrieveMessageAsJsonObject(messageConsumerCaseRetentionLengthCalculated);
        retrieveMessageAsJsonObject(messageConsumerHearingLogDocumentCreated);
    }

    private void verifyHearingEventsLogsDocumentRequested(final String courtDocumentId, final String caseId, final String defendantId, final String materialId, final Optional<String> applicationId, final String caseStatus) throws Exception {
        final Optional<JsonObject> message = retrieveMessageAsJsonObject(messageConsumerHearingLogDocumentCreated);
        Awaitility.await().atMost(Duration.TEN_SECONDS).until(() -> assertThat(message.isPresent(), is(true)));

        if (applicationId.isPresent() && !applicationId.get().isEmpty()) {
            assertThat(message.get().getString("caseId"), is(notNullValue()));
            assertThat(message.get().getString("applicationId"), is(notNullValue()));
            verifyHearingEventsLogsDocumentGenerated(applicationId.get(), TEMPLATE_NAME);
            verifyAddApplicationCourtDocument(courtDocumentId, caseId, defendantId, materialId, applicationId.get());
            final String actualDocument = getCourtDocumentsByApplication(UUID.randomUUID().toString(), applicationId.get());
            verifyApplicationDocIndices(applicationId.get(), actualDocument);
        } else {
            assertThat(message.get().getString("caseId"), is(notNullValue()));
            verifyHearingEventsLogsDocumentGenerated(caseId, TEMPLATE_NAME);
            verifyAddCourtDocument(courtDocumentId, caseId, defendantId, materialId);
            final String actualDocument = getCourtDocumentsPerCase(UUID.randomUUID().toString(), caseId);
            verifyCaseDocIndices(caseId, actualDocument);
        }

        verifyMaterialCreated();

    }


    private void verifyCaagHearingEventsLogsDocumentRequested(final String courtDocumentId, final String caseId, final String defendantId, final String materialId) throws Exception {
        final Optional<JsonObject> message = retrieveMessageAsJsonObject(messageConsumerHearingLogDocumentCreated);
        assertTrue(message.isPresent());
        assertThat(message.get().getString("caseId"), is(notNullValue()));

        verifyHearingEventsLogsDocumentGenerated(caseId, TEMPLATE_NAME);
        verifyAddCourtDocument(courtDocumentId, caseId, defendantId, materialId);
        final String actualDocument = getCourtDocumentsPerCase(UUID.randomUUID().toString(), caseId);
        verifyCaseDocIndices(caseId, actualDocument);

        verifyMaterialCreated();

    }

    private void verifyCaseDocIndices(final String caseId, final String actualDocument) {
        JsonObject json = stringToJsonObjectConverter.convert(actualDocument);
        assertThat(json.getJsonArray("documentIndices"), is(notNullValue()));
        assertThat(json.getJsonArray("documentIndices").size(), is(notNullValue()));
        assertThat(json.getJsonArray("documentIndices").getJsonObject(0).getJsonArray("caseIds").getString(0), is(caseId));
        assertThat(json.getJsonArray("documentIndices").getJsonObject(0).getJsonObject("document").getString("name"), containsString("HearingEventLog"));
        assertThat(json.getJsonArray("documentIndices").getJsonObject(0).getJsonObject("document").getJsonArray("materials").getJsonObject(0).getJsonArray("userGroups"), is(notNullValue()));
    }

    private void verifyApplicationDocIndices(final String applicationId, final String actualDocument) {
        JsonObject json = stringToJsonObjectConverter.convert(actualDocument);
        assertThat(json.getJsonArray("documentIndices"), is(notNullValue()));
        assertThat(json.getJsonArray("documentIndices").size(), is(notNullValue()));
        assertThat(json.getJsonArray("documentIndices").getJsonObject(0).getJsonObject("document").getJsonObject("documentCategory").getJsonObject("applicationDocument").getString("applicationId"), is(applicationId));
        assertThat(json.getJsonArray("documentIndices").getJsonObject(0).getJsonObject("document").getString("name"), containsString("HearingEventLog"));
        assertThat(json.getJsonArray("documentIndices").getJsonObject(0).getJsonObject("document").getJsonArray("materials").getJsonObject(0).getJsonArray("userGroups"), is(notNullValue()));
    }

    private void verifyHearingEventsLogsDocumentGenerated(final String caseId, final String TEMPLATE_NAME) throws Exception {
        final Optional<JsonObject> documentGenerationRequest = getHearingEventTemplate(TEMPLATE_NAME);

        assertThat(documentGenerationRequest.isPresent(), is(true));
        assertThat(documentGenerationRequest, notNullValue());
        // only high level validation done in integration test (rest covered in unit tests)

        assertThat(documentGenerationRequest.get().getJsonArray("hearings").getJsonObject(0).getString("courtCentre"), is(notNullValue()));
        assertThat(documentGenerationRequest.get().getJsonArray("hearings").getJsonObject(0).getString("courtRoom"), is(notNullValue()));
        assertThat(documentGenerationRequest.get().getJsonArray("hearings").getJsonObject(0).getString("hearingType"), is(notNullValue()));
        assertThat(documentGenerationRequest.get().getJsonArray("hearings").getJsonObject(0).getString("startDate"), is(notNullValue()));
        assertThat(documentGenerationRequest.get().getJsonArray("hearings").getJsonObject(0).getString("endDate"), is(notNullValue()));
        assertThat(documentGenerationRequest.get().getJsonArray("hearings").getJsonObject(0).getJsonArray("judiciary").size(), is(2));
    }

    private String doVerifyProsecutionCaseDefendantListingStatusChanged(final MessageConsumer messageConsumerProsecutionCaseDefendantListingStatusChanged) {
        final Optional<JsonObject> message = retrieveMessageAsJsonObject(messageConsumerProsecutionCaseDefendantListingStatusChanged);
        final JsonObject prosecutionCaseDefendantListingStatusChanged = message.get();
        return prosecutionCaseDefendantListingStatusChanged.getJsonObject("hearing").getString("id");
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
        );
    }

    private JsonObject getHearingJsonObject(final String path, final String caseId, final String hearingId,
                                            final String defendantId, final String courtCentreId, final String courtCentreName,
                                            final String reportingRestrictionId) {
        final String payload = getPayload(path)
                .replaceAll("CASE_ID", caseId)
                .replaceAll("HEARING_ID", hearingId)
                .replaceAll("DEFENDANT_ID", defendantId)
                .replaceAll("COURT_CENTRE_ID", courtCentreId)
                .replaceAll("COURT_CENTRE_NAME", courtCentreName)
                .replaceAll("REPORTING_RESTRICTION_ID", reportingRestrictionId);

        return stringToJsonObjectConverter.convert(payload);
    }

    private JsonObject getHearingWithStandAloneApplicationJsonObject(final String path, final String applicationId, final String hearingId, final String caseId, final String defendantId, final String courtCentreId) {
        final String strPayload = getPayloadForCreatingRequest(path)
                .replaceAll("HEARING_ID", hearingId)
                .replaceAll("CASE_ID", caseId)
                .replaceAll("DEFENDANT_ID", defendantId)
                .replaceAll("COURT_CENTRE_ID", courtCentreId)
                .replaceAll("APPLICATION_ID", applicationId);
        return stringToJsonObjectConverter.convert(strPayload);
    }

    private JsonObject getVerdictPublicEventPayload(final String hearingId) {
        final String strPayload = getPayloadForCreatingRequest("public.hearing.hearing-offence-verdict-updated.json")
                .replaceAll("HEARING_ID", hearingId);
        return stringToJsonObjectConverter.convert(strPayload);
    }

    private JsonObject getVerdictPublicEventPayloadForApplication(final String hearingId, final String applicationId) {
        final String strPayload = getPayloadForCreatingRequest("public.hearing.hearing-offence-verdict-updated-for-application.json")
                .replaceAll("HEARING_ID", hearingId)
                .replaceAll("APPLICATION_ID", applicationId);
        return stringToJsonObjectConverter.convert(strPayload);
    }

    private void verifyAddCourtDocument(final String courtDocumentId, final String caseId, final String defendantId, final String materialId) throws IOException {
        final String body = prepareAddCourtDocumentPayload(courtDocumentId, caseId, defendantId, materialId);
        final Response writeResponse = postCommand(getWriteUrl("/courtdocument/" + courtDocumentId),
                "application/vnd.progression.add-court-document+json",
                body);
        assertThat(writeResponse.getStatusCode(), equalTo(HttpStatus.SC_ACCEPTED));

        final String actualDocument = getCourtDocumentFor(courtDocumentId, allOf(
                        withJsonPath("$.courtDocument.courtDocumentId", equalTo(courtDocumentId)),
                        withJsonPath("$.courtDocument.containsFinancialMeans", equalTo(false))
                )
        );
    }

    private void verifyAddApplicationCourtDocument(final String courtDocumentId, final String caseId, final String defendantId, final String materialId, final String applicationId) throws IOException {
        final String body = prepareAddAaagCourtDocumentPayload(courtDocumentId, caseId, defendantId, materialId, applicationId);

        final Response writeResponse = postCommand(getWriteUrl("/courtdocument/" + courtDocumentId),
                "application/vnd.progression.add-court-document+json",
                body);
        assertThat(writeResponse.getStatusCode(), equalTo(HttpStatus.SC_ACCEPTED));

        final String actualDocument = getCourtDocumentFor(courtDocumentId, allOf(
                        withJsonPath("$.courtDocument.courtDocumentId", equalTo(courtDocumentId)),
                        withJsonPath("$.courtDocument.documentCategory.applicationDocument.applicationId", equalTo(applicationId))
                )
        );
    }

    private String prepareAddCourtDocumentPayload(final String courtDocumentId, final String caseId, final String defendantId, final String materialId) {
        String body = getPayload("progression.add-hearing-event-log-document.json");
        body = body.replaceAll("%RANDOM_DOCUMENT_ID%", courtDocumentId)
                .replaceAll("%RANDOM_CASE_ID%", caseId)
                .replaceAll("%RANDOM_DEFENDANT_ID1%", defendantId)
                .replaceAll("%RANDOM_MATERIAL_ID%", materialId);
        return body;
    }

    private String prepareAddAaagCourtDocumentPayload(final String courtDocumentId, final String caseId, final String defendantId, final String materialId, final String applicationId) {
        String body = getPayload("progression.add-aaag-hearing-event-log-document.json");
        body = body.replaceAll("%RANDOM_DOCUMENT_ID%", courtDocumentId)
                .replaceAll("%RANDOM_APPLICATION_ID%", applicationId)
                .replaceAll("%RANDOM_DEFENDANT_ID1%", defendantId)
                .replaceAll("%RANDOM_MATERIAL_ID%", materialId);
        return body;
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

    private Matcher[] getCaseStatusMatchers(final String caseStatus, final String caseId) {
        return new Matcher[]{
                withJsonPath("$.prosecutionCase.id", equalTo(caseId)),
                withJsonPath("$.prosecutionCase.caseStatus", equalTo(caseStatus))

        };
    }
}
