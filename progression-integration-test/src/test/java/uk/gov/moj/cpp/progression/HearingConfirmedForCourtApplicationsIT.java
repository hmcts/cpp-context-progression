package uk.gov.moj.cpp.progression;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static java.util.UUID.randomUUID;
import static javax.ws.rs.core.Response.Status.OK;
import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.hasItem;
import static org.junit.Assert.assertNotNull;
import static uk.gov.justice.services.common.http.HeaderConstants.USER_ID;
import static uk.gov.justice.services.test.utils.core.http.RequestParamsBuilder.requestParams;
import static uk.gov.justice.services.test.utils.core.http.RestPoller.poll;
import static uk.gov.justice.services.test.utils.core.matchers.ResponsePayloadMatcher.payload;
import static uk.gov.justice.services.test.utils.core.matchers.ResponseStatusMatcher.status;
import static uk.gov.moj.cpp.progression.domain.constant.CaseStatusEnum.INACTIVE;
import static uk.gov.moj.cpp.progression.domain.constant.CaseStatusEnum.ACTIVE;
import static uk.gov.moj.cpp.progression.helper.AbstractTestHelper.getReadUrl;
import static uk.gov.moj.cpp.progression.helper.DefaultRequests.PROGRESSION_QUERY_PROSECUTION_CASE_JSON;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.addCourtApplication;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.addProsecutionCaseToCrownCourt;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.pollForApplicationStatus;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.pollProsecutionCasesProgressionFor;
import static uk.gov.moj.cpp.progression.helper.QueueUtil.privateEvents;
import static uk.gov.moj.cpp.progression.helper.QueueUtil.publicEvents;
import static uk.gov.moj.cpp.progression.helper.QueueUtil.sendMessage;
import static uk.gov.moj.cpp.progression.stub.HearingStub.verifyPostInitiateCourtHearing;
import static uk.gov.moj.cpp.progression.stub.ReferenceDataStub.stubQueryDocumentTypeData;
import static uk.gov.moj.cpp.progression.test.TestUtilities.print;
import static uk.gov.moj.cpp.progression.util.FileUtil.getPayload;
import static uk.gov.moj.cpp.progression.util.ReferProsecutionCaseToCrownCourtHelper.getProsecutionCaseMatchers;

import uk.gov.justice.services.common.converter.StringToJsonObjectConverter;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.progression.helper.QueueUtil;
import uk.gov.moj.cpp.progression.helper.RestHelper;
import uk.gov.moj.cpp.progression.stub.HearingStub;
import uk.gov.moj.cpp.progression.stub.IdMapperStub;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

import javax.jms.JMSException;
import javax.jms.MessageConsumer;
import javax.jms.MessageProducer;
import javax.json.JsonObject;

import com.jayway.restassured.path.json.JsonPath;
import org.hamcrest.Matcher;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.Test;

public class HearingConfirmedForCourtApplicationsIT extends AbstractIT {
    private static final String PUBLIC_HEARING_RESULTED = "public.hearing.resulted";
    private static final String PUBLIC_HEARING_RESULTED_CASE_UPDATED = "public.hearing.resulted-case-updated";

    private static final String PUBLIC_LISTING_HEARING_CONFIRMED = "public.listing.hearing-confirmed";
    private static final MessageProducer messageProducerClientPublic = publicEvents.createProducer();
    private static final String PROGRESSION_COMMAND_CREATE_HEARING_APPLICATION_LINK = "progression.event.hearing-application-link-created";
    private static final MessageConsumer messageConsumerLink = privateEvents.createConsumer(PROGRESSION_COMMAND_CREATE_HEARING_APPLICATION_LINK);

    private final StringToJsonObjectConverter stringToJsonObjectConverter = new StringToJsonObjectConverter();
    private String userId;
    private String hearingId;
    private String caseId;
    private String defendantId;
    private String courtCentreId;
    private String courtCentreName;
    private String applicationId;

    @AfterClass
    public static void tearDown() throws JMSException {
        messageProducerClientPublic.close();
    }

    @Before
    public void setUp() {
        HearingStub.stubInitiateHearing();
        IdMapperStub.setUp();
        userId = randomUUID().toString();

    }

    @Test
    public void shouldUpdateCaseLinkedApplicationStatus() throws Exception {
        hearingId = randomUUID().toString();
        caseId = randomUUID().toString();
        defendantId = randomUUID().toString();
        courtCentreId = UUID.randomUUID().toString();
        courtCentreName = "Lavender Hill Magistrate's Court";
        applicationId = UUID.randomUUID().toString();

        stubQueryDocumentTypeData("/restResource/ref-data-document-type.json");
        addProsecutionCaseToCrownCourt(caseId, defendantId);
        addCourtApplication(caseId, applicationId, "progression.command.create-court-application.json");

        pollForApplicationStatus(applicationId, "DRAFT");

        sendMessage(messageProducerClientPublic,
                PUBLIC_LISTING_HEARING_CONFIRMED, getHearingJsonObject("public.listing.hearing-confirmed-application-with-linked-case.json",
                        caseId, hearingId, defendantId, courtCentreId, courtCentreName), JsonEnvelope.metadataBuilder()
                        .withId(randomUUID())
                        .withName(PUBLIC_LISTING_HEARING_CONFIRMED)
                        .withUserId(userId)
                        .build());

        pollForApplicationStatus(applicationId, "LISTED");

        pollForApplicationAtAGlance("LISTED");
        verifyPostInitiateCourtHearing(hearingId);
        verifyInMessagingQueue();
    }

    private JsonObject getHearingJsonObject(final String path, final String caseId, final String hearingId,
                                            final String defendantId, final String courtCentreId, final String courtCentreName) {
        final String strPayload = getPayload(path)
                .replaceAll("HEARING_ID", hearingId)
                .replaceAll("COURT_CENTRE_ID", courtCentreId)
                .replaceAll("COURT_CENTRE_NAME", courtCentreName)
                .replaceAll("APPLICATION_ID", applicationId)
                .replaceAll("CASE_ID", caseId)
                .replaceAll("DEFENDANT_ID", defendantId);
        return stringToJsonObjectConverter.convert(strPayload);
    }

    @Test
    public void shouldReopenCaseWhenAnewApplicationAddedAndHasFutureHearings() throws Exception {
        hearingId = randomUUID().toString();
        caseId = randomUUID().toString();
        defendantId = randomUUID().toString();
        courtCentreId = UUID.randomUUID().toString();
        applicationId = UUID.randomUUID().toString();
        courtCentreName = "Lavender Hill Magistrate's Court";
        stubQueryDocumentTypeData("/restResource/ref-data-document-type.json");
        addProsecutionCaseToCrownCourt(caseId, defendantId);
        pollProsecutionCasesProgressionFor(caseId, getProsecutionCaseMatchers(caseId, defendantId));

        sendMessage(messageProducerClientPublic,
                PUBLIC_HEARING_RESULTED, getHearingWithSingleCaseJsonObject(PUBLIC_HEARING_RESULTED_CASE_UPDATED + ".json", caseId,
                        hearingId, defendantId, courtCentreId, "C", "Remedy", "2593cf09-ace0-4b7d-a746-0703a29f33b5"), JsonEnvelope.metadataBuilder()
                        .withId(randomUUID())
                        .withName(PUBLIC_HEARING_RESULTED)
                        .withUserId(userId)
                        .build());

        pollProsecutionCasesProgressionFor(caseId, getCaseStatusMatchers(INACTIVE.getDescription()));

        addCourtApplication(caseId, applicationId, "progression.command.create-court-application.json");

        pollForApplicationStatus(applicationId, "DRAFT");

        sendMessage(messageProducerClientPublic,
                PUBLIC_LISTING_HEARING_CONFIRMED, getHearingJsonObject("public.listing.hearing-confirmed-case-reopen.json",
                        caseId, hearingId, defendantId, courtCentreId, courtCentreName), JsonEnvelope.metadataBuilder()
                        .withId(randomUUID())
                        .withName(PUBLIC_LISTING_HEARING_CONFIRMED)
                        .withUserId(userId)
                        .build());
        pollProsecutionCasesProgressionFor(caseId, getCaseStatusMatchers(ACTIVE.getDescription()));

    }

    private void pollForApplicationAtAGlance(final String status) {
        poll(requestParams(getReadUrl("/prosecutioncases/" + caseId), PROGRESSION_QUERY_PROSECUTION_CASE_JSON)
                .withHeader(USER_ID, UUID.randomUUID()))
                .timeout(RestHelper.TIMEOUT, TimeUnit.SECONDS)
                .until(
                        print(),
                        status().is(OK),
                        payload().isJson(allOf(
                                withJsonPath("$.prosecutionCase.id", equalTo(caseId)),
                                withJsonPath("$.hearingsAtAGlance.courtApplications.[*].linkedCaseId", hasItem(equalTo(caseId))),
                                withJsonPath("$.hearingsAtAGlance.courtApplications.[*].applicationStatus", hasItem(equalTo(status))),
                                withJsonPath("$.hearingsAtAGlance.hearings.[*].courtCentre.id", hasItem(equalTo(courtCentreId)))
                        )));
    }

    private static void verifyInMessagingQueue() {
        final JsonPath message = QueueUtil.retrieveMessage(messageConsumerLink);
        assertNotNull(message);
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

    private Matcher[] getCaseStatusMatchers(final String caseStatus) {
        return new Matcher[]{
                withJsonPath("$.prosecutionCase.id", equalTo(caseId)),
                withJsonPath("$.prosecutionCase.caseStatus", equalTo(caseStatus))

        };
    }
}

