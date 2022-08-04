package uk.gov.moj.cpp.progression.applications;

import static com.github.tomakehurst.wiremock.client.WireMock.reset;
import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static java.util.UUID.randomUUID;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertTrue;
import static uk.gov.moj.cpp.progression.applications.applicationHelper.ApplicationHelper.addBreachApplicationForExistingHearing;
import static uk.gov.moj.cpp.progression.applications.applicationHelper.ApplicationHelper.initiateCourtProceedingsForCourtApplication;
import static uk.gov.moj.cpp.progression.applications.applicationHelper.ApplicationHelper.pollForCourtApplication;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.addProsecutionCaseToCrownCourt;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.pollProsecutionCasesProgressionFor;
import static uk.gov.moj.cpp.progression.helper.QueueUtil.privateEvents;
import static uk.gov.moj.cpp.progression.stub.HearingStub.stubInitiateHearing;
import static uk.gov.moj.cpp.progression.util.ReferProsecutionCaseToCrownCourtHelper.getProsecutionCaseMatchers;

import uk.gov.moj.cpp.progression.AbstractIT;
import uk.gov.moj.cpp.progression.helper.QueueUtil;

import java.util.Optional;

import javax.jms.MessageConsumer;
import javax.json.JsonObject;

import org.hamcrest.Matcher;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class BreachApplicationIT extends AbstractIT {

    private static final String COURT_APPLICATION_CREATED_PRIVATE_EVENT = "progression.event.court-application-created";
    private static final String COURT_APPLICATION_PROCEEDINGS_INITIATED_PRIVATE_EVENT = "progression.event.court-application-proceedings-initiated";
    private static final String PROSECUTION_CASE_DEFENDANT_LISTING_STATUS_CHANGED = "progression.event.prosecutionCase-defendant-listing-status-changed-v2";
    private static MessageConsumer consumerForCourtApplicationCreated;
    private static MessageConsumer consumerForCourtApplicationProceedingsInitiated;
    private static MessageConsumer messageConsumerProsecutionCaseDefendantListingStatusChanged;

    /**
     * This test suffers due to a unified search stub which creates exact matches
     * So the smallest fix is to reset wiremock stubs and put the default stubs back
     */
    static {
        reset();
        defaultStubs();
    }

    @Before
    public void setUp() {
        consumerForCourtApplicationCreated = privateEvents.createPrivateConsumer(COURT_APPLICATION_CREATED_PRIVATE_EVENT);
        consumerForCourtApplicationProceedingsInitiated = privateEvents.createPrivateConsumer(COURT_APPLICATION_PROCEEDINGS_INITIATED_PRIVATE_EVENT);
        messageConsumerProsecutionCaseDefendantListingStatusChanged = privateEvents.createPrivateConsumer(PROSECUTION_CASE_DEFENDANT_LISTING_STATUS_CHANGED);
        stubInitiateHearing();
    }

    @After
    public void tearDown() throws Exception {
        consumerForCourtApplicationCreated.close();
    }

    @Test
    public void shouldCreateLinkedApplicationWithBreachOrder() throws Exception {
        final String applicationId = randomUUID().toString();

        initiateCourtProceedingsForCourtApplication(applicationId, "applications/progression.initiate-court-proceedings-for-breach-application.json");

        verifyCourtApplicationCreatedPrivateEvent();

        final Matcher[] applicationMatchers = {
                withJsonPath("$.courtApplication.id", is(applicationId)),
                withJsonPath("$.courtApplication.type.code", is("AS14518")),
                withJsonPath("$.courtApplication.type.linkType", is("LINKED")),
                withJsonPath("$.courtApplication.type.breachType", is("GENERIC_BREACH")),
                withJsonPath("$.courtApplication.applicationStatus", is("DRAFT")),
                withJsonPath("$.courtApplication.applicant.id", notNullValue()),
                withJsonPath("$.courtApplication.subject.id", notNullValue()),
                withJsonPath("$.courtApplication.courtApplicationCases[0].prosecutionCaseId", notNullValue()),
                withJsonPath("$.courtApplication.courtApplicationCases[0].prosecutionCaseIdentifier.caseURN", is("TFL4359536")),
                withJsonPath("$.courtApplication.outOfTimeReasons", is("Out time reasons for Breach order"))
        };

        pollForCourtApplication(applicationId, applicationMatchers);
    }

    @Test
    public void shouldCreateApplicationWithBreachOrderFromExistingHearing() throws Exception {


        final String caseId = randomUUID().toString();
        final String masterDefendantId = randomUUID().toString();


        addProsecutionCaseToCrownCourt(caseId, masterDefendantId);
        pollProsecutionCasesProgressionFor(caseId, getProsecutionCaseMatchers(caseId, masterDefendantId));

        final String hearingId = doVerifyProsecutionCaseDefendantListingStatusChanged();

        addBreachApplicationForExistingHearing(hearingId, masterDefendantId, "applications/progression.add-breach-application.json");

        final String applicationId = verifyCourtApplicationProceedingsInitiatedPrivateEvent();

        final Matcher[] applicationMatchers = {
                withJsonPath("$.courtApplication.id", is(applicationId)),
                withJsonPath("$.courtApplication.type.code", is("CJ03506")),
                withJsonPath("$.courtApplication.type.linkType", is("LINKED")),
                withJsonPath("$.courtApplication.type.breachType", is("COMMISSION_OF_NEW_OFFENCE_BREACH")),
                withJsonPath("$.courtApplication.applicationStatus", is("DRAFT")),
                withJsonPath("$.courtApplication.applicant.id", notNullValue()),
                withJsonPath("$.courtApplication.subject.id", notNullValue()),
                withJsonPath("$.courtApplication.respondents[0].id", notNullValue()),
                withJsonPath("$.courtApplication.courtOrder", notNullValue()),
        };

        pollForCourtApplication(applicationId, applicationMatchers);
    }

    @Test
    public void shouldCreateStandaloneApplicationWithBreachOrder() throws Exception {
        final String applicationId = randomUUID().toString();

        initiateCourtProceedingsForCourtApplication(applicationId, "applications/progression.initiate-court-proceedings-for-stand-alone-breach-application.json");

        verifyCourtApplicationCreatedPrivateEvent();

        final Matcher[] applicationMatchers = {
                withJsonPath("$.courtApplication.id", is(applicationId)),
                withJsonPath("$.courtApplication.type.code", is("AS14518")),
                withJsonPath("$.courtApplication.type.linkType", is("STANDALONE")),
                withJsonPath("$.courtApplication.type.breachType", is("GENERIC_BREACH")),
                withJsonPath("$.courtApplication.applicationStatus", is("DRAFT")),
                withJsonPath("$.courtApplication.applicant.id", notNullValue()),
                withJsonPath("$.courtApplication.subject.id", notNullValue()),
                withJsonPath("$.courtApplication.outOfTimeReasons", is("Out time reasons for Breach order"))
        };

        pollForCourtApplication(applicationId, applicationMatchers);
    }

    private String doVerifyProsecutionCaseDefendantListingStatusChanged() {
        final Optional<JsonObject> message = QueueUtil.retrieveMessageAsJsonObject(messageConsumerProsecutionCaseDefendantListingStatusChanged);
        final JsonObject prosecutionCaseDefendantListingStatusChanged = message.get();
        return prosecutionCaseDefendantListingStatusChanged.getJsonObject("hearing").getString("id");
    }

    private void verifyCourtApplicationCreatedPrivateEvent() {
        final Optional<JsonObject> message = QueueUtil.retrieveMessageAsJsonObject(consumerForCourtApplicationCreated);
        assertTrue(message.isPresent());
        final String applicationReference = message.get().getJsonObject("courtApplication").getString("applicationReference");
        assertThat(applicationReference.length(), is(10));
    }

    private String verifyCourtApplicationProceedingsInitiatedPrivateEvent() {
        final Optional<JsonObject> message = QueueUtil.retrieveMessageAsJsonObject(consumerForCourtApplicationProceedingsInitiated);
        assertTrue(message.isPresent());
        final String applicationReference = message.get().getJsonObject("courtApplication").getString("applicationReference");
        assertThat(applicationReference.length(), is(8));
        return message.get().getJsonObject("courtApplication").getString("id");
    }
}
