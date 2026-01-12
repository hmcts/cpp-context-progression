package uk.gov.moj.cpp.progression;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.isJson;
import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static java.util.UUID.randomUUID;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.awaitility.Awaitility.await;
import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static uk.gov.justice.services.integrationtest.utils.jms.JmsMessageConsumerClientProvider.newPublicJmsMessageConsumerClientProvider;
import static uk.gov.justice.services.integrationtest.utils.jms.JmsMessageProducerClientProvider.newPublicJmsMessageProducerClientProvider;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
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
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.pollCaseAndGetHearingForDefendant;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.pollForApplicationStatus;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.pollProsecutionCasesProgressionFor;
import static uk.gov.moj.cpp.progression.helper.QueueUtil.buildMetadata;
import static uk.gov.moj.cpp.progression.helper.QueueUtil.retrieveMessageBody;
import static uk.gov.moj.cpp.progression.helper.RestHelper.INITIAL_INTERVAL_IN_MILLISECONDS;
import static uk.gov.moj.cpp.progression.helper.RestHelper.INTERVAL_IN_MILLISECONDS;
import static uk.gov.moj.cpp.progression.helper.RestHelper.pollForResponse;
import static uk.gov.moj.cpp.progression.helper.RestHelper.postCommand;
import static uk.gov.moj.cpp.progression.helper.StubUtil.setupHmctsUsersGroupQueryStub;
import static uk.gov.moj.cpp.progression.helper.StubUtil.setupMaterialStub;
import static uk.gov.moj.cpp.progression.stub.CourtSchedulerServiceStub.stubGetProvisionalBookedSlotsForExistingBookingId;
import static uk.gov.moj.cpp.progression.stub.DocumentGeneratorStub.getHearingEventTemplate;
import static uk.gov.moj.cpp.progression.stub.MaterialStub.verifyMaterialCreated;
import static uk.gov.moj.cpp.progression.stub.ReferenceDataStub.stubGetDocumentsTypeAccess;
import static uk.gov.moj.cpp.progression.stub.ReferenceDataStub.stubQueryDocumentTypeData;
import static uk.gov.moj.cpp.progression.util.FileUtil.getPayload;
import static uk.gov.moj.cpp.progression.util.ReferProsecutionCaseToCrownCourtHelper.getProsecutionCaseMatchers;
import static uk.gov.moj.cpp.progression.util.WireMockStubUtils.stubAaagHearingEventLogs;
import static uk.gov.moj.cpp.progression.util.WireMockStubUtils.stubGetUserOrganisation;
import static uk.gov.moj.cpp.progression.util.WireMockStubUtils.stubHearingEventLogs;

import uk.gov.justice.services.common.converter.StringToJsonObjectConverter;
import uk.gov.justice.services.common.converter.ZonedDateTimes;
import uk.gov.justice.services.integrationtest.utils.jms.JmsMessageConsumerClient;
import uk.gov.justice.services.integrationtest.utils.jms.JmsMessageProducerClient;
import uk.gov.justice.services.messaging.JsonEnvelope;

import java.io.IOException;
import java.time.Duration;
import java.util.Optional;
import java.util.UUID;

import javax.json.JsonObject;

import io.restassured.response.Response;
import org.apache.http.HttpStatus;
import org.hamcrest.Matcher;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.gov.justice.services.test.utils.core.http.FibonacciPollWithStartAndMax;

public class HearingEventLogIT extends AbstractIT {

    private static final Logger LOGGER = LoggerFactory.getLogger(HearingEventLogIT.class);
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

    private JmsMessageProducerClient messageProducerClientPublic;
    private JmsMessageConsumerClient messageConsumerClientPublicForReferToCourtOnHearingInitiated;
    private JmsMessageConsumerClient messageConsumerClientPublicForHearingEventsLogsDocumentSucess;
    private JmsMessageConsumerClient messageConsumerClientPublicForHearingEventsLogsDocumentFailed;

    @BeforeEach
    public void setUp() {
        messageProducerClientPublic = newPublicJmsMessageProducerClientProvider().getMessageProducerClient();
        messageConsumerClientPublicForReferToCourtOnHearingInitiated = newPublicJmsMessageConsumerClientProvider().withEventNames(PUBLIC_PROGRESSION_EVENT_PROSECUTION_CASES_REFERRED_TO_COURT).getMessageConsumerClient();
        messageConsumerClientPublicForHearingEventsLogsDocumentSucess = newPublicJmsMessageConsumerClientProvider().withEventNames(PUBLIC_PROGRESSION_EVENT_HEARING_EVENTS_LOGS_DOCUMENT_SUCCESS).getMessageConsumerClient();
        messageConsumerClientPublicForHearingEventsLogsDocumentFailed = newPublicJmsMessageConsumerClientProvider().withEventNames(PUBLIC_PROGRESSION_EVENT_HEARING_EVENTS_LOGS_DOCUMENT_FAILED).getMessageConsumerClient();
        stubQueryDocumentTypeData("/restResource/ref-data-document-type.json", documentTypeId.toString());
        stubGetDocumentsTypeAccess("/restResource/get-all-document-type-access.json");
        givenCaseIsReferredToMags(null, TEMPLATE_NAME);
    }

    @AfterEach
    public void tearDown() {
        stubQueryDocumentTypeData("/restResource/ref-data-document-type.json");
        stubGetDocumentsTypeAccess("/restResource/get-all-document-type-access.json");
        givenCaseIsReferredToMags(null, TEMPLATE_NAME);
    }

    @Test
    public void shouldGenerateCAAGHearingEventLogDocumentForInActiveCaseIfNoApplicationExists() throws Exception {
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
        Optional<String> applicationId = Optional.empty();

        stubGetProvisionalBookedSlotsForExistingBookingId();
        givenCaseIsReferredToMags(null, TEMPLATE_NAME);
        setupMaterialStub(materialId);

        final String hearingEventLogResponsePayload = getPayload("stub-data/hearing.get-hearing-event-log-document.json")
                .replace("%CASE_ID%", caseId);
        stubHearingEventLogs(caseId, hearingEventLogResponsePayload);

        final String listedStartDateTime = ZonedDateTimes.fromString("2020-12-15T18:32:04.238Z").toString();
        final String earliestStartDateTime = ZonedDateTimes.fromString("2020-12-15T18:32:04.238Z").toString();

        initiateCourtProceedingsWithCommittingCourt(caseId, defendantId, listedStartDateTime, earliestStartDateTime);
        String hearingId = pollCaseAndGetHearingForDefendant(caseId, defendantId);

        final JsonObject hearingConfirmedJson = getHearingJsonObject("public.listing.hearing-confirmed.json", caseId, hearingId, defendantId, courtCentreId, courtCentreName);

        JsonEnvelope publicEventEnvelope = envelopeFrom(buildMetadata(PUBLIC_LISTING_HEARING_CONFIRMED, userId), hearingConfirmedJson);
        messageProducerClientPublic.sendMessage(PUBLIC_LISTING_HEARING_CONFIRMED, publicEventEnvelope);

        verifyPublicEventCasesReferredToCourts();

        publicEventEnvelope = envelopeFrom(buildMetadata(PUBLIC_HEARING_RESULTED, userId), getHearingJsonObject("public.hearing.resulted-with-crown-committing-court.json", caseId,
                hearingId, defendantId, newCourtCentreId, newCourtCentreName, reportingRestrictionId));
        messageProducerClientPublic.sendMessage(PUBLIC_HEARING_RESULTED, publicEventEnvelope);

        pollProsecutionCasesProgressionFor(caseId, getCaseStatusMatchers(INACTIVE.getDescription(), caseId));

        verifyHearingEventsLogsDocumentRequested(courtDocumentId, caseId, defendantId, materialId, applicationId, "INACTIVE");
        verifyMaterialCreated();
        verifyPublicEventHearingEventLogsDocumentSuccess();
    }

    @Test
    public void shouldGenerateCAAGHearingEventLogDocumentForActiveCaseIfNoApplicationExists() throws Exception {
        final String TEMPLATE_NAME = "HearingEventLog";
        final String userId = randomUUID().toString();
        final String caseId = randomUUID().toString();
        final String defendantId = randomUUID().toString();
        final String courtCentreId = UUID.fromString("111bdd2a-6b7a-4002-bc8c-5c6f93844f40").toString();
        final String courtCentreName = "Croydon Crown Court";
        final String materialId = randomUUID().toString();
        final String courtDocumentId = randomUUID().toString();

        Optional<String> applicationId = Optional.empty();
        stubGetProvisionalBookedSlotsForExistingBookingId();

        givenCaseIsReferredToMags(null, TEMPLATE_NAME);
        //search for the document by application id
        stubQueryDocumentTypeData("/restResource/ref-data-document-type.json", documentTypeId.toString());
        stubGetDocumentsTypeAccess("/restResource/get-all-document-type-access.json");
        setupMaterialStub(materialId);
        final String loggedInUsersResponsePayload = getPayload("stub-data/usersgroups.get-groups-by-hmcts-user.json");
        setupHmctsUsersGroupQueryStub(getPayload("stub-data/usersgroups.get-groups-by-hmcts-user.json").replace("%USER_ID%", userId));
        final JsonObject loggedInUserObject = stringToJsonObjectConverter.convert(loggedInUsersResponsePayload);
        final String organisation = getPayload("stub-data/usersgroups.get-hmcts-organisation-details.json")
                .replace("%ORGANISATION_ID%", loggedInUserObject.getString("organisationId"));
        stubGetUserOrganisation(loggedInUserObject.getString("organisationId"), organisation);

        final String hearingEventLogResponsePayload = getPayload("stub-data/hearing.get-hearing-event-log-document.json")
                .replace("%CASE_ID%", caseId);
        stubHearingEventLogs(caseId, hearingEventLogResponsePayload);

        addProsecutionCaseToCrownCourt(caseId, defendantId);
        pollProsecutionCasesProgressionFor(caseId, getProsecutionCaseMatchers(caseId, defendantId));

        final String listedStartDateTime = ZonedDateTimes.fromString("2020-12-15T18:32:04.238Z").toString();
        final String earliestStartDateTime = ZonedDateTimes.fromString("2020-12-15T18:32:04.238Z").toString();
        initiateCourtProceedingsWithCommittingCourt(caseId, defendantId, listedStartDateTime, earliestStartDateTime);
        String hearingId = pollCaseAndGetHearingForDefendant(caseId, defendantId);

        final JsonObject hearingConfirmedJson = getHearingJsonObject("public.listing.hearing-confirmed.json", caseId, hearingId, defendantId, courtCentreId, courtCentreName);

        final JsonEnvelope publicEventEnvelope = envelopeFrom(buildMetadata(PUBLIC_LISTING_HEARING_CONFIRMED, userId), hearingConfirmedJson);
        messageProducerClientPublic.sendMessage(PUBLIC_LISTING_HEARING_CONFIRMED, publicEventEnvelope);

        final String payload = pollProsecutionCasesProgressionFor(caseId, getCaseStatusMatchers(ACTIVE.getDescription(), caseId));
        final JsonObject caseObject = stringToJsonObjectConverter.convert(payload);

        verifyPublicEventCasesReferredToCourts();
        verifyCaagHearingEventLog(caseId);
        verifyHearingEventsLogsDocumentRequested(courtDocumentId, caseId, defendantId, materialId, applicationId, caseObject.getJsonObject("prosecutionCase").getString("caseStatus"));
        verifyPublicEventHearingEventLogsDocumentSuccess();
    }

    @Test
    public void shouldNotGenerateCAAGHearingEventLogDocumentForActiveCaseIfNoHearingEventLogs() throws Exception {

        final String caseId = randomUUID().toString();
        final String userId = randomUUID().toString();
        final String defendantId = randomUUID().toString();
        final String courtCentreId = UUID.fromString("111bdd2a-6b7a-4002-bc8c-5c6f93844f40").toString();

        final String loggedInUsersResponsePayload = getPayload("stub-data/usersgroups.get-groups-by-hmcts-user.json");
        setupHmctsUsersGroupQueryStub(getPayload("stub-data/usersgroups.get-groups-by-hmcts-user.json").replace("%USER_ID%", userId));
        final JsonObject loggedInUserObject = stringToJsonObjectConverter.convert(loggedInUsersResponsePayload);
        final String organisation = getPayload("stub-data/usersgroups.get-hmcts-organisation-details.json")
                .replace("%ORGANISATION_ID%", loggedInUserObject.getString("organisationId"));
        stubGetUserOrganisation(loggedInUserObject.getString("organisationId"), organisation);
        final String hearingEventLogResponsePayload = getPayload("stub-data/hearing.get-no-hearing-event-log-document.json");
        stubHearingEventLogs(caseId, hearingEventLogResponsePayload);

        addProsecutionCaseToCrownCourt(caseId, defendantId);
        String hearingId = pollCaseAndGetHearingForDefendant(caseId, defendantId);

        final JmsMessageConsumerClient publicHearingDetailChangedConsumer = newPublicJmsMessageConsumerClientProvider().withEventNames("public.hearing-detail-changed").getMessageConsumerClient();

        final JsonEnvelope publicEventEnvelope = envelopeFrom(buildMetadata(PUBLIC_LISTING_HEARING_UPDATED, userId), getHearingJsonObject("public.listing.hearing-updated.json", caseId,
                hearingId, defendantId, courtCentreId, "Croydon Crown Court"));
        messageProducerClientPublic.sendMessage(PUBLIC_LISTING_HEARING_UPDATED, publicEventEnvelope);

        assertThat(retrieveMessageBody(publicHearingDetailChangedConsumer).isPresent(), is(true));

        pollProsecutionCasesProgressionFor(caseId, withJsonPath("$.prosecutionCase.id", equalTo(caseId)),
                withJsonPath("$.hearingsAtAGlance.hearings[0].courtCentre.id", equalTo(courtCentreId)));
        verifyCaagHearingEventLog(caseId);
        verifyPublicEventHearingEventLogsDocumentFailed();
    }

    @Test
    public void shouldNotGenerateAAAGHearingEventLogDocumentForActiveCaseApplicationIfNoHearingEventLog() throws Exception {

        Optional<String> applicationId = Optional.of(randomUUID().toString());
        final String caseId = randomUUID().toString();
        final String userId = randomUUID().toString();
        final String defendantId = randomUUID().toString();
        final String courtCentreId = UUID.fromString("111bdd2a-6b7a-4002-bc8c-5c6f93844f40").toString();

        final String loggedInUsersResponsePayload = getPayload("stub-data/usersgroups.get-groups-by-hmcts-user.json");
        setupHmctsUsersGroupQueryStub(loggedInUsersResponsePayload.replace("%USER_ID%", userId));
        final JsonObject loggedInUserObject = stringToJsonObjectConverter.convert(loggedInUsersResponsePayload);
        final String organisation = getPayload("stub-data/usersgroups.get-hmcts-organisation-details.json")
                .replace("%ORGANISATION_ID%", loggedInUserObject.getString("organisationId"));
        stubGetUserOrganisation(loggedInUserObject.getString("organisationId"), organisation);

        final String hearinEventLogResponsePayload = getPayload("stub-data/hearing.get-no-hearing-event-log-document-application.json");
        stubAaagHearingEventLogs(applicationId.get(), hearinEventLogResponsePayload);
        addProsecutionCaseToCrownCourt(caseId, defendantId);
        String hearingId = pollCaseAndGetHearingForDefendant(caseId, defendantId);

        final JmsMessageConsumerClient publicHearingDetailChangedConsumer = newPublicJmsMessageConsumerClientProvider().withEventNames("public.hearing-detail-changed").getMessageConsumerClient();

        final JsonEnvelope publicEventEnvelope = envelopeFrom(buildMetadata(PUBLIC_LISTING_HEARING_UPDATED, userId), getHearingJsonObject("public.listing.hearing-updated.json", caseId,
                hearingId, defendantId, courtCentreId, "Croydon Crown Court"));
        messageProducerClientPublic.sendMessage(PUBLIC_LISTING_HEARING_UPDATED, publicEventEnvelope);

        final Optional<JsonObject> message = retrieveMessageBody(publicHearingDetailChangedConsumer);
        assertThat(message.isPresent(), is(true));

        pollProsecutionCasesProgressionFor(caseId, withJsonPath("$.prosecutionCase.id", equalTo(caseId)),
                withJsonPath("$.hearingsAtAGlance.hearings[0].courtCentre.id", equalTo(courtCentreId)));

        initiateCourtProceedingsForCourtApplication(applicationId.get(), caseId, randomUUID().toString(), "applications/progression.initiate-court-proceedings-for-generic-linked-application.json");

        pollForApplicationStatus(applicationId.get(), "DRAFT");

        String caseStatus = pollForResponse("/applications/" + applicationId.get() + "/case/" + caseId, PROGRESSION_QUERY_CASE_STATUS_FOR_APPLICATION);
        JsonObject caseObj = stringToJsonObjectConverter.convert(caseStatus);
        assertThat(caseObj.getString("caseStatus"), is(notNullValue()));

        verifyAaagHearingEventLog(caseId, applicationId.get());
        verifyPublicEventHearingEventLogsDocumentFailed();
    }

    @Test
    public void shouldGenerateAAAGHearingEventLogDocumentForActiveCaseIfApplicationExists() throws Exception {

        Optional<String> applicationId = Optional.of(randomUUID().toString());
        final String caseId = randomUUID().toString();
        final String userId = randomUUID().toString();
        final String defendantId = randomUUID().toString();
        final String courtCentreId = UUID.fromString("111bdd2a-6b7a-4002-bc8c-5c6f93844f40").toString();
        final String materialId = randomUUID().toString();
        final String courtDocumentId = randomUUID().toString();

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
        String hearingId = pollCaseAndGetHearingForDefendant(caseId, defendantId);

        stubQueryDocumentTypeData("/restResource/ref-data-document-type.json", documentTypeId.toString());
        stubGetDocumentsTypeAccess("/restResource/get-all-document-type-access.json");
        setupMaterialStub(materialId);

        final JmsMessageConsumerClient publicHearingDetailChangedConsumer = newPublicJmsMessageConsumerClientProvider().withEventNames("public.hearing-detail-changed").getMessageConsumerClient();

        final JsonEnvelope publicEventEnvelope = envelopeFrom(buildMetadata(PUBLIC_LISTING_HEARING_UPDATED, userId), getHearingJsonObject("public.listing.hearing-updated.json", caseId,
                hearingId, defendantId, courtCentreId, "Croydon Crown Court"));
        messageProducerClientPublic.sendMessage(PUBLIC_LISTING_HEARING_UPDATED, publicEventEnvelope);

        final Optional<JsonObject> message = retrieveMessageBody(publicHearingDetailChangedConsumer);
        assertThat(message.isPresent(), is(true));

        pollProsecutionCasesProgressionFor(caseId, withJsonPath("$.prosecutionCase.id", equalTo(caseId)),
                withJsonPath("$.hearingsAtAGlance.hearings[0].courtCentre.id", equalTo(courtCentreId)));

        initiateCourtProceedingsForCourtApplication(applicationId.get(), caseId, randomUUID().toString(), "applications/progression.initiate-court-proceedings-for-generic-linked-application.json");

        pollForApplicationStatus(applicationId.get(), "DRAFT");

        String caseStatus = pollForResponse("/applications/" + applicationId.get() + "/case/" + caseId, PROGRESSION_QUERY_CASE_STATUS_FOR_APPLICATION);
        JsonObject caseObj = stringToJsonObjectConverter.convert(caseStatus);
        assertThat(caseObj.getString("caseStatus"), is(notNullValue()));

        verifyAaagHearingEventLog(caseId, applicationId.get());
        verifyHearingEventsLogsDocumentRequested(courtDocumentId, caseId, defendantId, materialId, applicationId, caseObj.getString("caseStatus"));
        verifyPublicEventHearingEventLogsDocumentSuccess();
    }


    @Test
    public void shouldGenerateAAAGHearingEventLogDocumentForInActiveCaseIfApplicationExists() throws Exception {
        final String TEMPLATE_NAME = "HearingEventLog";
        final String userId = randomUUID().toString();
        final String caseId = randomUUID().toString();
        final String defendantId = randomUUID().toString();
        final String courtCentreId = UUID.fromString("111bdd2a-6b7a-4002-bc8c-5c6f93844f40").toString();
        final String materialId = randomUUID().toString();
        final String courtDocumentId = randomUUID().toString();

        Optional<String> applicationId = Optional.of(randomUUID().toString());
        final String loggedInUsersResponsePayload = getPayload("stub-data/usersgroups.get-groups-by-hmcts-user.json");
        setupHmctsUsersGroupQueryStub(loggedInUsersResponsePayload.replace("%USER_ID%", userId));
        final JsonObject loggedInUserObject = stringToJsonObjectConverter.convert(loggedInUsersResponsePayload);
        final String organisation = getPayload("stub-data/usersgroups.get-hmcts-organisation-details.json")
                .replace("%ORGANISATION_ID%", loggedInUserObject.getString("organisationId"));
        stubGetUserOrganisation(loggedInUserObject.getString("organisationId"), organisation);
        givenCaseIsReferredToMags(null, TEMPLATE_NAME);
        //search for the document by application id
        stubQueryDocumentTypeData("/restResource/ref-data-document-type.json", documentTypeId.toString());
        stubGetDocumentsTypeAccess("/restResource/get-all-document-type-access.json");
        setupMaterialStub(materialId);
        final String hearinEventLogResponsePayload = getPayload("stub-data/hearing.get-aaag-hearing-event-log-document.json")
                .replace("%CASE_ID%", caseId)
                .replace("%APPLICATION_ID%", applicationId.get());
        stubAaagHearingEventLogs(applicationId.get(), hearinEventLogResponsePayload);

        addProsecutionCaseToCrownCourt(caseId, defendantId);
        String hearingId = pollCaseAndGetHearingForDefendant(caseId, defendantId);

        final JsonEnvelope publicEventEnvelope = envelopeFrom(buildMetadata(PUBLIC_HEARING_RESULTED, userId), getHearingWithSingleCaseJsonObject(PUBLIC_HEARING_RESULTED_CASE_UPDATED + ".json", caseId,
                hearingId, defendantId, courtCentreId, "C", "Remedy", "2593cf09-ace0-4b7d-a746-0703a29f33b5"));
        messageProducerClientPublic.sendMessage(PUBLIC_HEARING_RESULTED, publicEventEnvelope);

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
        verifyPublicEventHearingEventLogsDocumentSuccess();
    }

    @Test
    public void shouldNotGenerateAAAGHearingEventLogDocumentForNonHmctsUser() throws Exception {

        Optional<String> applicationId = Optional.of(randomUUID().toString());
        final String caseId = randomUUID().toString();
        final String userId = randomUUID().toString();
        final String defendantId = randomUUID().toString();
        final String courtCentreId = UUID.fromString("111bdd2a-6b7a-4002-bc8c-5c6f93844f40").toString();

        final String loggedInUsersResponsePayload = getPayload("stub-data/usersgroups.get-groups-by-hmcts-user.json");
        setupHmctsUsersGroupQueryStub(loggedInUsersResponsePayload.replace("%USER_ID%", userId));
        final JsonObject loggedInUserObject = stringToJsonObjectConverter.convert(loggedInUsersResponsePayload);
        final String organisation = getPayload("stub-data/usersgroups.get-non-hmcts-organisation-details.json")
                .replace("%ORGANISATION_ID%", loggedInUserObject.getString("organisationId"));
        stubGetUserOrganisation(loggedInUserObject.getString("organisationId"), organisation);
        addProsecutionCaseToCrownCourt(caseId, defendantId);
        String hearingId = pollCaseAndGetHearingForDefendant(caseId, defendantId);

        final JmsMessageConsumerClient publicHearingDetailChangedConsumer = newPublicJmsMessageConsumerClientProvider().withEventNames("public.hearing-detail-changed").getMessageConsumerClient();

        final JsonEnvelope publicEventEnvelope = envelopeFrom(buildMetadata(PUBLIC_LISTING_HEARING_UPDATED, userId), getHearingJsonObject("public.listing.hearing-updated.json", caseId,
                hearingId, defendantId, courtCentreId, "Croydon Crown Court"));
        messageProducerClientPublic.sendMessage(PUBLIC_LISTING_HEARING_UPDATED, publicEventEnvelope);

        Optional<JsonObject> message = retrieveMessageBody(publicHearingDetailChangedConsumer);
        assertThat(message.isPresent(), is(true));

        pollProsecutionCasesProgressionFor(caseId, withJsonPath("$.prosecutionCase.id", equalTo(caseId)),
                withJsonPath("$.hearingsAtAGlance.hearings[0].courtCentre.id", equalTo(courtCentreId)));

        initiateCourtProceedingsForCourtApplication(applicationId.get(), caseId, randomUUID().toString(), "applications/progression.initiate-court-proceedings-for-generic-linked-application.json");

        pollForApplicationStatus(applicationId.get(), "DRAFT");

        String caseStatus = pollForResponse("/applications/" + applicationId.get() + "/case/" + caseId, PROGRESSION_QUERY_CASE_STATUS_FOR_APPLICATION);
        JsonObject caseObj = stringToJsonObjectConverter.convert(caseStatus);
        assertThat(caseObj.getString("caseStatus"), is(notNullValue()));

        verifyAaagHearingEventLog(caseId, applicationId.get());
    }

    @Test
    public void shouldNotGenerateCAAGHearingEventLogDocumentForNonHmctsUser() throws Exception {
        final String TEMPLATE_NAME = "HearingEventLog";
        final String userId = randomUUID().toString();
        final String caseId = randomUUID().toString();
        final String defendantId = randomUUID().toString();
        final String courtCentreId = UUID.fromString("111bdd2a-6b7a-4002-bc8c-5c6f93844f40").toString();
        final String courtCentreName = "Croydon Crown Court";

        stubGetProvisionalBookedSlotsForExistingBookingId();
        givenCaseIsReferredToMags(null, TEMPLATE_NAME);
        //search for the document by application id
        final String loggedInUsersResponsePayload = getPayload("stub-data/usersgroups.get-groups-by-hmcts-user.json");
        setupHmctsUsersGroupQueryStub(loggedInUsersResponsePayload.replace("%USER_ID%", userId));
        final JsonObject loggedInUserObject = stringToJsonObjectConverter.convert(loggedInUsersResponsePayload);
        final String organisation = getPayload("stub-data/usersgroups.get-non-hmcts-organisation-details.json")
                .replace("%ORGANISATION_ID%", loggedInUserObject.getString("organisationId"));
        stubGetUserOrganisation(loggedInUserObject.getString("organisationId"), organisation);
        final String hearingEventLogResponsePayload = getPayload("stub-data/hearing.get-hearing-event-log-document.json")
                .replace("%CASE_ID%", caseId);
        stubHearingEventLogs(caseId, hearingEventLogResponsePayload);

        addProsecutionCaseToCrownCourt(caseId, defendantId);
        pollProsecutionCasesProgressionFor(caseId, getProsecutionCaseMatchers(caseId, defendantId));

        final String listedStartDateTime = ZonedDateTimes.fromString("2020-12-15T18:32:04.238Z").toString();
        final String earliestStartDateTime = ZonedDateTimes.fromString("2020-12-15T18:32:04.238Z").toString();
        initiateCourtProceedingsWithCommittingCourt(caseId, defendantId, listedStartDateTime, earliestStartDateTime);

        String hearingId = pollCaseAndGetHearingForDefendant(caseId, defendantId);

        final JsonObject hearingConfirmedJson = getHearingJsonObject("public.listing.hearing-confirmed.json", caseId, hearingId, defendantId, courtCentreId, courtCentreName);

        final JsonEnvelope publicEventEnvelope = envelopeFrom(buildMetadata(PUBLIC_LISTING_HEARING_CONFIRMED, userId), hearingConfirmedJson);
        messageProducerClientPublic.sendMessage(PUBLIC_LISTING_HEARING_CONFIRMED, publicEventEnvelope);

        verifyPublicEventCasesReferredToCourts();
        verifyCaagHearingEventLog(caseId);
    }

    private void verifyCaagHearingEventLog(final String caseId) {
        final String commandUri = getWriteUrl("/hearingeventlog/" + caseId);
        final io.restassured.response.Response response = postCommand(commandUri,
                "application/vnd.progression.create-hearing-event-log-document+json",
                "{}");
        assertThat(response.getStatusCode(), equalTo(HttpStatus.SC_ACCEPTED));
    }

    private void verifyAaagHearingEventLog(final String caseId, final String applicationId) {
        final String commandUri = getWriteUrl("/hearingeventlog/case/" + caseId + "/application/" + applicationId);
        final io.restassured.response.Response response = postCommand(commandUri,
                "application/vnd.progression.create-aaag-hearing-event-log-document+json",
                "{}");

        assertThat(response.getStatusCode(), is(HttpStatus.SC_ACCEPTED));
    }

    private void verifyHearingEventsLogsDocumentRequested(final String courtDocumentId, final String caseId, final String defendantId, final String materialId, final Optional<String> applicationId, final String caseStatus) throws Exception {

        verifyHearingEventsLogsDocumentGenerated(TEMPLATE_NAME);
        if (applicationId.isPresent() && !applicationId.get().isEmpty()) {
            verifyAddApplicationCourtDocument(courtDocumentId, caseId, defendantId, materialId, applicationId.get());
            final String actualDocument = getCourtDocumentsByApplication(randomUUID().toString(), applicationId.get());
            verifyApplicationDocIndices(applicationId.get(), actualDocument);
        } else {
            verifyAddCourtDocument(courtDocumentId, caseId, defendantId, materialId);
            getCourtDocumentsPerCase(randomUUID().toString(), caseId, verifyCaseDocIndices(caseId));
        }
        verifyMaterialCreated();
    }

    private Matcher[] verifyCaseDocIndices(final String caseId) {

        return new Matcher[]{
                withJsonPath("$.documentIndices[*].caseIds[*]", hasItem(caseId)),
                withJsonPath("$.documentIndices[*].document.name", hasItem(containsString("HearingEventLog"))),
                withJsonPath("$.documentIndices[*].document.materials[*].userGroups[*]", hasItem(notNullValue()))
        };
    }

    private void verifyApplicationDocIndices(final String applicationId, final String actualDocument) {
        JsonObject json = stringToJsonObjectConverter.convert(actualDocument);
        assertThat(json.getJsonArray("documentIndices"), is(notNullValue()));
        assertThat(json.getJsonArray("documentIndices").size(), is(notNullValue()));
        assertThat(json.getJsonArray("documentIndices").getJsonObject(0).getJsonObject("document").getJsonObject("documentCategory").getJsonObject("applicationDocument").getString("applicationId"), is(applicationId));
        assertThat(json.getJsonArray("documentIndices").getJsonObject(0).getJsonObject("document").getString("name"), containsString("HearingEventLog"));
        assertThat(json.getJsonArray("documentIndices").getJsonObject(0).getJsonObject("document").getJsonArray("materials").getJsonObject(0).getJsonArray("userGroups"), is(notNullValue()));
    }

    private void verifyHearingEventsLogsDocumentGenerated(final String TEMPLATE_NAME) {
        await().atMost(Duration.ofSeconds(15)).pollInterval(new FibonacciPollWithStartAndMax(Duration.ofMillis(INITIAL_INTERVAL_IN_MILLISECONDS), Duration.ofMillis(INTERVAL_IN_MILLISECONDS)))
                .until(() -> {
                    try {
                        final Optional<JsonObject> documentGenerationRequest = getHearingEventTemplate(TEMPLATE_NAME);
                        assertThat(documentGenerationRequest.isPresent(), is(true));
                        assertThat(documentGenerationRequest.get(), isJson(allOf(
                                withJsonPath("$.hearings[0].courtCentre", is(notNullValue())),
                                withJsonPath("$.hearings[0].courtRoom", is(notNullValue())),
                                withJsonPath("$.hearings[0].hearingType", is(notNullValue())),
                                withJsonPath("$.hearings[0].startDate", is(notNullValue())),
                                withJsonPath("$.hearings[0].endDate", is(notNullValue())),
                                withJsonPath("$.hearings[0].judiciary[*]", hasSize(2))
                        )));
                    } catch (AssertionError e) {
                        LOGGER.error(e.getMessage());
                        return false;
                    }
                    return true;
                });
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

    private void verifyAddCourtDocument(final String courtDocumentId, final String caseId, final String defendantId, final String materialId) {
        final String body = prepareAddCourtDocumentPayload(courtDocumentId, caseId, defendantId, materialId);
        final Response writeResponse = postCommand(getWriteUrl("/courtdocument/" + courtDocumentId),
                "application/vnd.progression.add-court-document+json",
                body);
        assertThat(writeResponse.getStatusCode(), equalTo(HttpStatus.SC_ACCEPTED));

        getCourtDocumentFor(courtDocumentId, allOf(
                        withJsonPath("$.courtDocument.courtDocumentId", equalTo(courtDocumentId)),
                        withJsonPath("$.courtDocument.containsFinancialMeans", equalTo(false))
                )
        );
    }

    private void verifyAddApplicationCourtDocument(final String courtDocumentId, final String caseId, final String defendantId, final String materialId, final String applicationId) {
        final String body = prepareAddAaagCourtDocumentPayload(courtDocumentId, caseId, defendantId, materialId, applicationId);

        final Response writeResponse = postCommand(getWriteUrl("/courtdocument/" + courtDocumentId),
                "application/vnd.progression.add-court-document+json",
                body);
        assertThat(writeResponse.getStatusCode(), equalTo(HttpStatus.SC_ACCEPTED));

        getCourtDocumentFor(courtDocumentId, allOf(
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

    private void verifyPublicEventCasesReferredToCourts() {
        final Optional<JsonObject> message = retrieveMessageBody(messageConsumerClientPublicForReferToCourtOnHearingInitiated);
        assertThat(message.isPresent(), is(true));
    }

    private void verifyPublicEventHearingEventLogsDocumentSuccess() {
        final Optional<JsonObject> message = retrieveMessageBody(messageConsumerClientPublicForHearingEventsLogsDocumentSucess);
        assertTrue(message.isPresent());
    }

    private void verifyPublicEventHearingEventLogsDocumentFailed() {
        final Optional<JsonObject> message = retrieveMessageBody(messageConsumerClientPublicForHearingEventsLogsDocumentFailed);
        assertTrue(message.isPresent());
    }


}
