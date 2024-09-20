package uk.gov.moj.cpp.progression;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.isJson;
import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static java.util.Collections.singletonList;
import static java.util.UUID.randomUUID;
import static javax.ws.rs.core.Response.Status.OK;
import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static uk.gov.justice.services.common.http.HeaderConstants.USER_ID;
import static uk.gov.justice.services.integrationtest.utils.jms.JmsMessageConsumerClientProvider.newPrivateJmsMessageConsumerClientProvider;
import static uk.gov.justice.services.integrationtest.utils.jms.JmsMessageConsumerClientProvider.newPublicJmsMessageConsumerClientProvider;
import static uk.gov.justice.services.integrationtest.utils.jms.JmsMessageProducerClientProvider.newPublicJmsMessageProducerClientProvider;
import static uk.gov.justice.services.test.utils.core.http.RequestParamsBuilder.requestParams;
import static uk.gov.justice.services.test.utils.core.http.RestPoller.poll;
import static uk.gov.justice.services.test.utils.core.matchers.ResponsePayloadMatcher.payload;
import static uk.gov.justice.services.test.utils.core.matchers.ResponseStatusMatcher.status;
import static uk.gov.moj.cpp.progression.applications.applicationHelper.ApplicationHelper.initiateCourtProceedingsForCourtApplication;
import static uk.gov.moj.cpp.progression.helper.AbstractTestHelper.getReadUrl;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.addProsecutionCaseToCrownCourt;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.addProsecutionCaseToCrownCourtWithDefendantAsAdult;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.addStandaloneCourtApplication;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.pollForApplication;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.pollForApplicationStatus;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.pollProsecutionCasesProgressionFor;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.updateCourtApplication;
import static uk.gov.moj.cpp.progression.helper.QueueUtil.buildMetadata;
import static uk.gov.moj.cpp.progression.helper.QueueUtil.retrieveMessageAsJsonPath;
import static uk.gov.moj.cpp.progression.helper.QueueUtil.retrieveMessageBody;
import static uk.gov.moj.cpp.progression.helper.RestHelper.getJsonObject;
import static uk.gov.moj.cpp.progression.it.framework.ContextNameProvider.CONTEXT_NAME;
import static uk.gov.moj.cpp.progression.util.ReferProsecutionCaseToCrownCourtHelper.getProsecutionCaseMatchers;

import uk.gov.justice.services.common.converter.StringToJsonObjectConverter;
import uk.gov.justice.services.integrationtest.utils.jms.JmsMessageConsumerClient;
import uk.gov.justice.services.integrationtest.utils.jms.JmsMessageProducerClient;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.progression.helper.CourtApplicationsHelper;
import uk.gov.moj.cpp.progression.stub.HearingStub;

import java.nio.charset.Charset;
import java.util.Optional;
import java.util.UUID;

import javax.json.JsonObject;

import com.google.common.io.Resources;
import com.jayway.jsonpath.ReadContext;
import io.restassured.path.json.JsonPath;
import org.hamcrest.CoreMatchers;
import org.hamcrest.Matcher;
import org.hamcrest.Matchers;
import org.junit.Assert;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

@SuppressWarnings({"squid:S1607", "squid:S2925"})
public class UpdateCourtApplicationIT extends AbstractIT {
    private static final String COURT_APPLICATION_CREATED = "public.progression.court-application-created";
    private static final String COURT_APPLICATION_UPDATED = "progression.event.court-application-proceedings-edited";
    private static final String PUBLIC_LISTING_HEARING_CONFIRMED = "public.listing.hearing-confirmed";

    private static final JmsMessageProducerClient messageProducerClientPublic = newPublicJmsMessageProducerClientProvider().getMessageProducerClient();
    private final StringToJsonObjectConverter stringToJsonObjectConverter = new StringToJsonObjectConverter();
    private static final JmsMessageConsumerClient messageConsumerHearingPopulatedToProbationCaseWorker = newPrivateJmsMessageConsumerClientProvider(CONTEXT_NAME).withEventNames("progression.events.hearing-populated-to-probation-caseworker").getMessageConsumerClient();
    private static final JmsMessageConsumerClient consumerForCourtApplicationCreated = newPublicJmsMessageConsumerClientProvider().withEventNames(COURT_APPLICATION_CREATED).getMessageConsumerClient();
    private static final JmsMessageConsumerClient consumerForCourtApplicationUpdated = newPrivateJmsMessageConsumerClientProvider(CONTEXT_NAME).withEventNames(COURT_APPLICATION_UPDATED).getMessageConsumerClient();

    private String caseId;
    private String defendantId;

    @BeforeEach
    public void setUp() {
        caseId = randomUUID().toString();
        defendantId = randomUUID().toString();


    }

    @Test
    public void shouldUpdateCourtApplicationAndGetConfirmation() throws Exception {

        // when
        addProsecutionCaseToCrownCourt(caseId, defendantId);

        final Matcher[] initialMatchers = getProsecutionCaseMatchers(caseId, defendantId, singletonList(withJsonPath("$.prosecutionCase.defendants[0].witnessStatement", is("he did not do it"))));
        String response = pollProsecutionCasesProgressionFor(caseId, initialMatchers);
        JsonObject prosecutionCasesJsonObject = getJsonObject(response);

        // assert defendant witness statement
        String reference = prosecutionCasesJsonObject.getJsonObject("prosecutionCase").getJsonObject("prosecutionCaseIdentifier").getString("prosecutionAuthorityReference");

        String applicationId = randomUUID().toString();
        String applicantId = UUID.fromString("88cdf36e-93e4-41b0-8277-17d9dba7f06f").toString();

        initiateCourtProceedingsForCourtApplication(applicationId, caseId, "applications/progression.initiate-court-proceedings-for-court-order-linked-application.json");

        verifyInMessagingQueueForCourtApplicationCreated(applicationId);

        Matcher[] applicationMatchers = {
                withJsonPath("$.courtApplication.id", is(applicationId)),
                withJsonPath("$.courtApplication.applicationStatus", is("UN_ALLOCATED")),
                withJsonPath("$.courtApplication.outOfTimeReasons", is("Out of times reasons for linked application test")),
                withJsonPath("$.courtApplication.applicationReference", notNullValue()),
        };

        pollForApplication(applicationId, applicationMatchers);

        updateCourtApplication(applicationId, applicantId, caseId, defendantId, "", "progression.command.update-court-application.json");

        verifyInMessagingQueueForCourtApplicationUpdated();

        Matcher[] updatedApplicationMatchers = {
                withJsonPath("$.courtApplication.id", is(applicationId)),
                withJsonPath("$.courtApplication.applicationStatus", is("DRAFT")),
                withJsonPath("$.courtApplication.outOfTimeReasons", is("b"))
        };

        verifyInitiateCourtProceedingsViewStoreUpdated(applicationId, updatedApplicationMatchers);

    }

    @Test
    public void shouldRaiseProbationEventWhenUpdateCourtApplication() throws Exception {
        HearingStub.stubInitiateHearing();
        final String applicationId = randomUUID().toString();
        final String hearingId;
        addStandaloneCourtApplication(applicationId, UUID.randomUUID().toString(), new CourtApplicationsHelper.CourtApplicationRandomValues(), "progression.command.create-standalone-court-application.json");
        pollForApplicationStatus(applicationId, "DRAFT");

        final JmsMessageConsumerClient messageConsumerProsecutionCaseDefendantListingStatusChanged = newPrivateJmsMessageConsumerClientProvider(CONTEXT_NAME).withEventNames("progression.event.prosecutionCase-defendant-listing-status-changed-v2").getMessageConsumerClient();

        addProsecutionCaseToCrownCourtWithDefendantAsAdult(caseId, defendantId);
        hearingId = doVerifyProsecutionCaseDefendantListingStatusChanged(messageConsumerProsecutionCaseDefendantListingStatusChanged);
        final String courtCentreId = UUID.fromString("111bdd2a-6b7a-4002-bc8c-5c6f93844f40").toString();

        final JsonEnvelope publicEventEnvelope = JsonEnvelope.envelopeFrom(buildMetadata(PUBLIC_LISTING_HEARING_CONFIRMED, randomUUID()), getHearingWithStandAloneApplicationJsonObject("public.listing.hearing-confirmed-application-with-linked-case.json",
                applicationId, hearingId, caseId, defendantId, courtCentreId));
        messageProducerClientPublic.sendMessage(PUBLIC_LISTING_HEARING_CONFIRMED, publicEventEnvelope);

        pollForApplicationStatus(applicationId, "LISTED");
        JsonPath messageDaysMatchers = retrieveMessageAsJsonPath(messageConsumerHearingPopulatedToProbationCaseWorker, isJson(Matchers.allOf(
                withJsonPath("$.hearing.id", CoreMatchers.is(hearingId)),
                withJsonPath("$.hearing.courtApplications[0].outOfTimeReasons", is("a")))));

        Assert.assertNotNull(messageDaysMatchers);


        String applicantId = UUID.fromString("88cdf36e-93e4-41b0-8277-17d9dba7f06f").toString();
        updateCourtApplication(applicationId, applicantId, caseId, defendantId, hearingId, "progression.command.update-court-application-with-hearing.json");
        messageDaysMatchers = retrieveMessageAsJsonPath(messageConsumerHearingPopulatedToProbationCaseWorker, isJson(Matchers.allOf(
                withJsonPath("$.hearing.id", CoreMatchers.is(hearingId)),
                withJsonPath("$.hearing.courtApplications[0].outOfTimeReasons", is("b")))));
        Assert.assertNotNull(messageDaysMatchers);
    }

    @SafeVarargs
    private final void verifyInitiateCourtProceedingsViewStoreUpdated(final String applicationId, final Matcher<? super ReadContext>... matchers) {
        poll(requestParams(getReadUrl("/court-proceedings/application/" + applicationId),
                "application/vnd.progression.query.court-proceedings-for-application+json").withHeader(USER_ID, randomUUID()))
                .until(status().is(OK), payload().isJson(allOf(matchers)));

    }

    private void verifyInMessagingQueueForCourtApplicationCreated(String applicationId) {
        final Optional<JsonObject> message = retrieveMessageBody(consumerForCourtApplicationCreated);
        assertTrue(message.isPresent());
        String idResponse = message.get().getJsonObject("courtApplication").getString("id");
        assertThat(idResponse, equalTo(applicationId));
    }

    private void verifyInMessagingQueueForCourtApplicationUpdated() {
        final Optional<JsonObject> message = retrieveMessageBody(consumerForCourtApplicationUpdated);
        assertTrue(message.isPresent());
        String outOfTimeReasons = message.get().getJsonObject("courtApplication").getString("outOfTimeReasons");
        assertThat(outOfTimeReasons, equalTo("b"));
    }

    private String doVerifyProsecutionCaseDefendantListingStatusChanged(final JmsMessageConsumerClient messageConsumerProsecutionCaseDefendantListingStatusChanged) {
        final Optional<JsonObject> message = retrieveMessageBody(messageConsumerProsecutionCaseDefendantListingStatusChanged);
        final JsonObject prosecutionCaseDefendantListingStatusChanged = message.get();
        return prosecutionCaseDefendantListingStatusChanged.getJsonObject("hearing").getString("id");
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
}

