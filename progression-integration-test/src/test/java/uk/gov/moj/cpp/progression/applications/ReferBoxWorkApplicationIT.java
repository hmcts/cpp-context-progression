package uk.gov.moj.cpp.progression.applications;

import static com.google.common.collect.Lists.newArrayList;
import static com.google.common.io.Resources.getResource;
import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static java.util.UUID.fromString;
import static java.util.UUID.randomUUID;
import static javax.ws.rs.core.Response.Status.OK;
import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.skyscreamer.jsonassert.JSONAssert.assertEquals;
import static org.skyscreamer.jsonassert.JSONCompareMode.STRICT;
import static uk.gov.justice.services.common.http.HeaderConstants.USER_ID;
import static uk.gov.justice.services.test.utils.core.http.RequestParamsBuilder.requestParams;
import static uk.gov.justice.services.test.utils.core.http.RestPoller.poll;
import static uk.gov.justice.services.test.utils.core.matchers.ResponsePayloadMatcher.payload;
import static uk.gov.justice.services.test.utils.core.matchers.ResponseStatusMatcher.status;
import static uk.gov.justice.services.test.utils.core.random.RandomGenerator.STRING;
import static uk.gov.moj.cpp.progression.applications.applicationHelper.ApplicationHelper.initiateCourtProceedingsForCourtApplication;
import static uk.gov.moj.cpp.progression.helper.AbstractTestHelper.getReadUrl;
import static uk.gov.moj.cpp.progression.helper.AbstractTestHelper.getWriteUrl;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.addProsecutionCaseToCrownCourt;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.pollProsecutionCasesProgressionFor;
import static uk.gov.moj.cpp.progression.helper.QueueUtil.privateEvents;
import static uk.gov.moj.cpp.progression.helper.QueueUtil.publicEvents;
import static uk.gov.moj.cpp.progression.helper.QueueUtil.retrieveMessageAsJsonObject;
import static uk.gov.moj.cpp.progression.helper.RestHelper.pollForResponse;
import static uk.gov.moj.cpp.progression.helper.RestHelper.postCommand;
import static uk.gov.moj.cpp.progression.it.framework.util.ViewStoreCleaner.cleanEventStoreTables;
import static uk.gov.moj.cpp.progression.it.framework.util.ViewStoreCleaner.cleanViewStoreTables;
import static uk.gov.moj.cpp.progression.stub.DocumentGeneratorStub.stubDocumentCreate;
import static uk.gov.moj.cpp.progression.stub.HearingStub.stubInitiateHearing;
import static uk.gov.moj.cpp.progression.stub.NotificationServiceStub.verifyEmailNotificationIsRaisedWithAttachment;
import static uk.gov.moj.cpp.progression.stub.ReferenceDataStub.stubQueryDocumentTypeData;
import static uk.gov.moj.cpp.progression.util.ReferBoxWorkApplicationHelper.getPostBoxWorkApplicationReferredHearing;
import static uk.gov.moj.cpp.progression.util.ReferProsecutionCaseToCrownCourtHelper.getProsecutionCaseMatchers;

import uk.gov.justice.services.test.utils.core.http.ResponseData;
import uk.gov.moj.cpp.progression.AbstractIT;
import uk.gov.moj.cpp.progression.helper.QueueUtil;
import uk.gov.moj.cpp.progression.stub.NotificationServiceStub;

import java.io.IOException;
import java.nio.charset.Charset;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import javax.jms.MessageConsumer;
import javax.json.Json;
import javax.json.JsonObject;

import com.google.common.io.Resources;
import com.jayway.restassured.response.Response;
import org.hamcrest.Matchers;
import org.json.JSONObject;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.skyscreamer.jsonassert.Customization;
import org.skyscreamer.jsonassert.comparator.CustomComparator;

@SuppressWarnings("squid:S1607")
public class ReferBoxWorkApplicationIT extends AbstractIT {
    private static final String COURT_APPLICATION_CREATED_PRIVATE_EVENT = "progression.event.court-application-created";
    private static final String EMAIL_REQUESTED_PRIVATE_EVENT = "progression.event.email-requested";
    private static final String PRINT_REQUESTED_PRIVATE_EVENT = "progression.event.print-requested";
    private static final String PUBLIC_PROGRESSION_BOXWORK_APPLICATION_REFERRED = "public.progression.boxwork-application-referred";
    public static final String PUBLIC_PROGRESSION_EVENTS_HEARING_EXTENDED = "public.progression.events.hearing-extended";
    private static final String COURT_DOCUMENT_ADDED = "progression.event.court-document-added";

    private MessageConsumer consumerForCourtApplicationCreated;
    private MessageConsumer consumerForEmailRequested;
    private MessageConsumer consumerForPrintRequested;
    private MessageConsumer messageConsumerClientPublicForReferBoxWorkApplicationOnHearingInitiated;
    private MessageConsumer publicEventsConsumerForHearingExtended;
    private MessageConsumer addCourtDocument;

    private String applicationId;
    private String caseId;
    private String defendantId;

    @Before
    public void setUp() throws IOException {
        cleanEventStoreTables();
        cleanViewStoreTables();
        applicationId = randomUUID().toString();
        caseId = randomUUID().toString();
        defendantId = randomUUID().toString();
        stubInitiateHearing();
        stubDocumentCreate(STRING.next());
        NotificationServiceStub.setUp();
        addProsecutionCaseToCrownCourt(caseId, defendantId);
        pollProsecutionCasesProgressionFor(caseId, getProsecutionCaseMatchers(caseId, defendantId));
        messageConsumerClientPublicForReferBoxWorkApplicationOnHearingInitiated = publicEvents.createPublicConsumer(PUBLIC_PROGRESSION_BOXWORK_APPLICATION_REFERRED);
        consumerForCourtApplicationCreated = privateEvents.createPrivateConsumer(COURT_APPLICATION_CREATED_PRIVATE_EVENT);
        publicEventsConsumerForHearingExtended = publicEvents.createPublicConsumer(PUBLIC_PROGRESSION_EVENTS_HEARING_EXTENDED);
        consumerForEmailRequested = privateEvents.createPrivateConsumer(EMAIL_REQUESTED_PRIVATE_EVENT);
        consumerForPrintRequested = privateEvents.createPrivateConsumer(PRINT_REQUESTED_PRIVATE_EVENT);
        addCourtDocument = privateEvents.createPrivateConsumer(COURT_DOCUMENT_ADDED);
    }

    @After
    public void tearDown() throws Exception {
        consumerForCourtApplicationCreated.close();
        messageConsumerClientPublicForReferBoxWorkApplicationOnHearingInitiated.close();
        publicEventsConsumerForHearingExtended.close();
        consumerForEmailRequested.close();
        consumerForPrintRequested.close();
    }

    @Test
    public void shouldReferBoxWorkInitiateCourtApplication() throws Exception {

        initiateCourtProceedingsForCourtApplication(applicationId, caseId,"applications/progression.initiate-court-proceedings-for-standalone-application-box-hearing.json");

        verifyCourtApplicationCreatedPrivateEvent();

        final JsonObject hearing = getHearingInMessagingQueueForBoxWorkReferred();

        final String hearingId = hearing.getString("id");

        pollForResponse("/hearingSearch/" + hearingId, "application/vnd.progression.query.hearing+json", withJsonPath("$.hearing.id", Matchers.is(hearingId)));

        String eventOfHearing = Json.createObjectBuilder().add("hearing", hearing).build().toString();

        final String requestOfHearing = getPostBoxWorkApplicationReferredHearing(applicationId);
        String expectedOfHearing = Resources.toString(Resources.getResource("expected/expected.hearing.initiate.json"), Charset.defaultCharset());
        expectedOfHearing = expectedOfHearing.replace("CASE_ID", caseId).replace("DEFENDANT_ID", defendantId).replace("ORDERED_DATE", LocalDate.now().toString());
        assertEquals(expectedOfHearing, requestOfHearing, getCustomComparator(applicationId));
        assertEquals(expectedOfHearing, eventOfHearing, getCustomComparator(applicationId));

        verifyInitiateCourtProceedingsViewStoreUpdated(applicationId, "AS14518");

        editCourtProceedingsForCourtApplication(applicationId, hearingId, "applications/progression.initiate-court-proceedings-for-standalone-application-box-hearing-edit.json");
        verifyInitiateCourtProceedingsViewStoreUpdated(applicationId, "TS12345");
        verifyPublicEventForHearingExtended("2020-01-12T05:27:17.210Z", "B01LY00", "MAGISTRATES");
        final ResponseData responseData = verifyCourtApplicationViewStoreUpdated(applicationId, "2022-02-02");
        final String payload = responseData.getPayload();
        final JSONObject thirdParties = new JSONObject(payload).getJSONArray("thirdParties").getJSONObject(0);
        assertThat(thirdParties.getString("name"), is("David lloyd"));
    }

    @Test
    public void shouldSendAppointmentLetterAsEmailAttachmentForVirtualHearingWhenReferBoxWorkInitiateCourtApplicationAndDefendantHasEmailAddress() throws Exception{
        stubQueryDocumentTypeData("/restResource/ref-data-document-type-for-stat-dec.json");
        initiateCourtProceedingsForCourtApplication(applicationId, caseId,"applications/progression.initiate-court-proceedings-for-statdec-application-defendant-has-emailAddress.json");
        final Optional<JsonObject> message = QueueUtil.retrieveMessageAsJsonObject(consumerForCourtApplicationCreated);
        assertTrue(message.isPresent());
        final String applicationReference = message.get().getJsonObject("courtApplication").getString("applicationReference");
        assertThat(11, is(applicationReference.length()));
        final UUID materialId = verifyEmailRequestedPrivateEvent();
        final List<String> expectedEmailDetails = newArrayList("hallie.pollich@yahoo.com");

        final JsonObject addDocumentMessage = QueueUtil.retrieveMessageAsJsonObject(addCourtDocument).get();
        assertThat(addDocumentMessage.getJsonObject("courtDocument").getString("documentTypeId"), is("460fbe94-c002-11e8-a355-529269fb1459"));
        assertThat(addDocumentMessage.getJsonObject("courtDocument").getString("documentTypeDescription"), is("Orders, Notices & Directions"));

        verifyEmailNotificationIsRaisedWithAttachment(expectedEmailDetails, materialId);
    }

    @Test
    public void shouldNotSendAppointmentLetterAsEmailAttachmentForNonVirtualHearingWhenReferBoxWorkInitiateCourtApplicationAndDefendantHasEmailAddress() throws Exception{
        initiateCourtProceedingsForCourtApplication(applicationId, caseId,"applications/progression.initiate-court-proceedings-for-statdec-application-non-virtual-hearing-defendant-has-emailAddress.json");
        final Optional<JsonObject> message = QueueUtil.retrieveMessageAsJsonObject(consumerForCourtApplicationCreated);
        assertTrue(message.isPresent());
        final String applicationReference = message.get().getJsonObject("courtApplication").getString("applicationReference");
        assertThat(11, is(applicationReference.length()));
        verifyEmailNotRequestedPrivateEvent();
    }


    private void verifyInitiateCourtProceedingsViewStoreUpdated(final String applicationId, final String courtApplicationTypeCode) {
        poll(requestParams(getReadUrl("/court-proceedings/application/" + applicationId),
                "application/vnd.progression.query.court-proceedings-for-application+json").withHeader(USER_ID, randomUUID()))
                .until(status().is(OK), payload().isJson(allOf(withJsonPath("$.courtApplication.id", is(applicationId)),
                        withJsonPath("$.courtApplication.type.code", is(courtApplicationTypeCode)))));

    }

    private ResponseData verifyCourtApplicationViewStoreUpdated(final String applicationId, final String applicationReceivedDate) {
        return  poll(requestParams(getReadUrl("/applications/" + applicationId),
                "application/vnd.progression.query.application.aaag+json").withHeader(USER_ID, randomUUID()))
                .until(status().is(OK), payload().isJson(allOf(withJsonPath("$.applicationId", is(applicationId)),
                        withJsonPath("$.applicationDetails.applicationReceivedDate", is(applicationReceivedDate)))));

    }

    public Response editCourtProceedingsForCourtApplication(final String applicationId, final String boxHearingId, final String fileName) throws IOException {
        return postCommand(getWriteUrl("/initiate-application"),
                "application/vnd.progression.edit-court-proceedings-for-application+json",
                getCourtApplicationJson(applicationId, boxHearingId, fileName));
    }


    private String getCourtApplicationJson(final String applicationId, final String boxHearingId, final String fileName) throws IOException {
        String payloadJson;
        payloadJson = Resources.toString(getResource(fileName), Charset.defaultCharset())
                .replace("APPLICATION_ID", applicationId)
                .replace("BOX_HEARING_ID", boxHearingId);
        return payloadJson;
    }

    private CustomComparator getCustomComparator(String applicationId) {
        return new CustomComparator(STRICT,
                new Customization("hearing.id", (o1, o2) -> o1 != null && o2 != null),
                new Customization("hearing.courtApplications[0].applicationReference", (o1, o2) -> o1 != null && o2 != null),
                new Customization("hearing.prosecutionCases[0].defendants[0].courtProceedingsInitiated", (o1, o2) -> o1 != null && o2 != null),
                new Customization("hearing.prosecutionCases[0].prosecutionCaseIdentifier.prosecutionAuthorityReference", (o1, o2) -> o1 != null && o2 != null),
                new Customization("hearing.prosecutionCases[0].defendants[0].masterDefendantId", (o1, o2) -> o1 != null && o2 != null),
                new Customization("hearing.courtApplications[0].id", (o1, o2) -> applicationId.equals(o1))
        );
    }

    private void verifyCourtApplicationCreatedPrivateEvent() {
        final Optional<JsonObject> message = QueueUtil.retrieveMessageAsJsonObject(consumerForCourtApplicationCreated);
        assertTrue(message.isPresent());
        final String applicationReference = message.get().getJsonObject("courtApplication").getString("applicationReference");
        assertThat(10, is(applicationReference.length()));
    }

    private UUID verifyEmailRequestedPrivateEvent() {
        final Optional<JsonObject> message = QueueUtil.retrieveMessageAsJsonObject(consumerForEmailRequested);
        assertTrue(message.isPresent());
        final String applicationIdFromEmailRequested = message.get().getString("applicationId");
        assertThat(applicationId, is(applicationIdFromEmailRequested));
        return fromString(message.get().getString("materialId"));

    }

    private void verifyEmailNotRequestedPrivateEvent() {
        final Optional<JsonObject> message = QueueUtil.retrieveMessageAsJsonObject(consumerForEmailRequested);
        assertFalse(message.isPresent());
    }

    private void verifyPrintRequestedPrivateEvent() {
        final Optional<JsonObject> message = QueueUtil.retrieveMessageAsJsonObject(consumerForPrintRequested);
        assertTrue(message.isPresent());
        final String applicationIdFromEmailRequested = message.get().getString("applicationId");
        assertThat(applicationId, is(applicationIdFromEmailRequested));
    }

    private JsonObject getHearingInMessagingQueueForBoxWorkReferred() {
        final Optional<JsonObject> message = retrieveMessageAsJsonObject(messageConsumerClientPublicForReferBoxWorkApplicationOnHearingInitiated);
        assertTrue(message.isPresent());
        return message.get().getJsonObject("hearing");
    }

    private void verifyPublicEventForHearingExtended(final String sittingDate, final String courtCenterCode, final String jurisdictionType) {
        final Optional<JsonObject> message = QueueUtil.retrieveMessageAsJsonObject(publicEventsConsumerForHearingExtended);
        assertTrue(message.isPresent());
        final JsonObject publicHearingExtendedEvent = message.get();
        assertThat(publicHearingExtendedEvent.getString("hearingId"), is(notNullValue()));
        assertThat(publicHearingExtendedEvent.getJsonArray("hearingDays").size(), is(1));
        assertThat(publicHearingExtendedEvent.getJsonObject("courtCentre").getString("code"), is(courtCenterCode));
        assertThat(publicHearingExtendedEvent.getString("jurisdictionType"), is(jurisdictionType));
        assertThat(((JsonObject)publicHearingExtendedEvent.getJsonArray("hearingDays").get(0)).getString("sittingDay"), is(sittingDate));
    }
}

