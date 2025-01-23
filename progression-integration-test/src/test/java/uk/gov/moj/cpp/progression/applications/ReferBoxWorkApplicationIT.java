package uk.gov.moj.cpp.progression.applications;

import static com.google.common.collect.Lists.newArrayList;
import static com.google.common.io.Resources.getResource;
import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static java.util.UUID.randomUUID;
import static javax.json.Json.createObjectBuilder;
import static javax.ws.rs.core.Response.Status.OK;
import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.skyscreamer.jsonassert.JSONAssert.assertEquals;
import static org.skyscreamer.jsonassert.JSONCompareMode.STRICT;
import static uk.gov.justice.services.common.http.HeaderConstants.USER_ID;
import static uk.gov.justice.services.integrationtest.utils.jms.JmsMessageConsumerClientProvider.newPublicJmsMessageConsumerClientProvider;
import static uk.gov.justice.services.test.utils.core.http.RequestParamsBuilder.requestParams;
import static uk.gov.justice.services.test.utils.core.http.RestPoller.poll;
import static uk.gov.justice.services.test.utils.core.matchers.ResponsePayloadMatcher.payload;
import static uk.gov.justice.services.test.utils.core.matchers.ResponseStatusMatcher.status;
import static uk.gov.justice.services.test.utils.core.random.RandomGenerator.STRING;
import static uk.gov.moj.cpp.progression.applications.applicationHelper.ApplicationHelper.initiateCourtProceedingsForCourtApplication;
import static uk.gov.moj.cpp.progression.applications.applicationHelper.ApplicationHelper.pollForCourtApplication;
import static uk.gov.moj.cpp.progression.helper.AbstractTestHelper.getReadUrl;
import static uk.gov.moj.cpp.progression.helper.AbstractTestHelper.getWriteUrl;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.addProsecutionCaseToCrownCourt;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.getCourtDocumentsPerCase;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.pollProsecutionCasesProgressionFor;
import static uk.gov.moj.cpp.progression.helper.QueueUtil.retrieveMessageBody;
import static uk.gov.moj.cpp.progression.helper.RestHelper.pollForResponse;
import static uk.gov.moj.cpp.progression.helper.RestHelper.postCommand;
import static uk.gov.moj.cpp.progression.stub.DocumentGeneratorStub.stubDocumentCreate;
import static uk.gov.moj.cpp.progression.stub.HearingStub.stubInitiateHearing;
import static uk.gov.moj.cpp.progression.stub.NotificationServiceStub.verifyEmailNotificationIsRaisedWithAttachment;
import static uk.gov.moj.cpp.progression.stub.ReferenceDataStub.stubGetDocumentsTypeAccess;
import static uk.gov.moj.cpp.progression.stub.ReferenceDataStub.stubQueryDocumentTypeData;
import static uk.gov.moj.cpp.progression.util.ReferBoxWorkApplicationHelper.getPostBoxWorkApplicationReferredHearing;
import static uk.gov.moj.cpp.progression.util.ReferProsecutionCaseToCrownCourtHelper.getProsecutionCaseMatchers;

import uk.gov.justice.services.integrationtest.utils.jms.JmsMessageConsumerClient;
import uk.gov.justice.services.integrationtest.utils.jms.JmsResourceManagementExtension;
import uk.gov.justice.services.test.utils.core.http.ResponseData;
import uk.gov.moj.cpp.progression.stub.NotificationServiceStub;

import java.io.IOException;
import java.nio.charset.Charset;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import javax.json.JsonObject;

import com.google.common.io.Resources;
import io.restassured.response.Response;
import org.hamcrest.Matcher;
import org.hamcrest.Matchers;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.skyscreamer.jsonassert.Customization;
import org.skyscreamer.jsonassert.comparator.CustomComparator;

@ExtendWith(JmsResourceManagementExtension.class)
@SuppressWarnings("squid:S1607")
public class ReferBoxWorkApplicationIT {
    private static final String PUBLIC_PROGRESSION_BOXWORK_APPLICATION_REFERRED = "public.progression.boxwork-application-referred";
    public static final String PUBLIC_PROGRESSION_EVENTS_HEARING_EXTENDED = "public.progression.events.hearing-extended";

    private static final JmsMessageConsumerClient messageConsumerClientPublicForReferBoxWorkApplicationOnHearingInitiated = newPublicJmsMessageConsumerClientProvider().withEventNames(PUBLIC_PROGRESSION_BOXWORK_APPLICATION_REFERRED).getMessageConsumerClient();
    private static final JmsMessageConsumerClient publicEventsConsumerForHearingExtended = newPublicJmsMessageConsumerClientProvider().withEventNames(PUBLIC_PROGRESSION_EVENTS_HEARING_EXTENDED).getMessageConsumerClient();

    private String applicationId;
    private String caseId;
    private String defendantId;

    @BeforeAll
    public static void setUpClass() throws IOException, JSONException {
        stubInitiateHearing();
        stubDocumentCreate(STRING.next());
        NotificationServiceStub.setUp();
        stubGetDocumentsTypeAccess("/restResource/get-all-document-type-access.json");
    }

    @BeforeEach
    public void setUp() throws IOException, JSONException {
        applicationId = randomUUID().toString();
        caseId = randomUUID().toString();
        defendantId = randomUUID().toString();
        addProsecutionCaseToCrownCourt(caseId, defendantId);
        pollProsecutionCasesProgressionFor(caseId, getProsecutionCaseMatchers(caseId, defendantId));
    }

    @Test
    public void shouldReferBoxWorkInitiateCourtApplication() throws Exception {

        initiateCourtProceedingsForCourtApplication(applicationId, caseId, "applications/progression.initiate-court-proceedings-for-standalone-application-box-hearing.json");

        final JsonObject hearing = getHearingInMessagingQueueForBoxWorkReferred();

        final String hearingId = hearing.getString("id");

        pollForResponse("/hearingSearch/" + hearingId, "application/vnd.progression.query.hearing+json", withJsonPath("$.hearing.id", Matchers.is(hearingId)));

        String eventOfHearing = createObjectBuilder().add("hearing", hearing).build().toString();

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
    public void shouldSendAppointmentLetterAsEmailAttachmentForVirtualHearingWhenReferBoxWorkInitiateCourtApplicationAndDefendantHasEmailAddress() throws Exception {
        stubQueryDocumentTypeData("/restResource/ref-data-document-type-for-stat-dec.json");

        initiateCourtProceedingsForCourtApplication(applicationId, caseId, "applications/progression.initiate-court-proceedings-for-statdec-application-defendant-has-emailAddress.json");
        pollForCourtApplication(applicationId);

        getCourtDocumentsPerCase(randomUUID().toString(), caseId, new Matcher[]{
                withJsonPath("$.documentIndices[0].document.documentTypeId", is("460fbe94-c002-11e8-a355-529269fb1459")),
                withJsonPath("$.documentIndices[0].document.documentTypeDescription", is("Orders, Notices & Directions"))
        });

        final List<String> expectedEmailDetails = newArrayList("hallie.pollich@yahoo.com");
        verifyEmailNotificationIsRaisedWithAttachment(expectedEmailDetails);
    }

    private void verifyInitiateCourtProceedingsViewStoreUpdated(final String applicationId, final String courtApplicationTypeCode) {
        poll(requestParams(getReadUrl("/court-proceedings/application/" + applicationId),
                "application/vnd.progression.query.court-proceedings-for-application+json").withHeader(USER_ID, randomUUID()))
                .until(status().is(OK), payload().isJson(allOf(withJsonPath("$.courtApplication.id", is(applicationId)),
                        withJsonPath("$.courtApplication.type.code", is(courtApplicationTypeCode)))));

    }

    private ResponseData verifyCourtApplicationViewStoreUpdated(final String applicationId, final String applicationReceivedDate) {
        return poll(requestParams(getReadUrl("/applications/" + applicationId),
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

    private JsonObject getHearingInMessagingQueueForBoxWorkReferred() {
        final Optional<JsonObject> message = retrieveMessageBody(messageConsumerClientPublicForReferBoxWorkApplicationOnHearingInitiated);
        assertTrue(message.isPresent());
        return message.get().getJsonObject("hearing");
    }

    private void verifyPublicEventForHearingExtended(final String sittingDate, final String courtCenterCode, final String jurisdictionType) {
        final Optional<JsonObject> message = retrieveMessageBody(publicEventsConsumerForHearingExtended);
        assertTrue(message.isPresent());
        final JsonObject publicHearingExtendedEvent = message.get();
        assertThat(publicHearingExtendedEvent.getString("hearingId"), is(notNullValue()));
        assertThat(publicHearingExtendedEvent.getJsonArray("hearingDays").size(), is(1));
        assertThat(publicHearingExtendedEvent.getJsonObject("courtCentre").getString("code"), is(courtCenterCode));
        assertThat(publicHearingExtendedEvent.getString("jurisdictionType"), is(jurisdictionType));
        assertThat(((JsonObject) publicHearingExtendedEvent.getJsonArray("hearingDays").get(0)).getString("sittingDay"), is(sittingDate));
    }
}

