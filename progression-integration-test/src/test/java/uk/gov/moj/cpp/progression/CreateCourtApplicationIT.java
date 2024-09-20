package uk.gov.moj.cpp.progression;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.hasNoJsonPath;
import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static java.util.UUID.randomUUID;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static uk.gov.justice.services.integrationtest.utils.jms.JmsMessageConsumerClientProvider.newPublicJmsMessageConsumerClientProvider;
import static uk.gov.moj.cpp.progression.applications.applicationHelper.ApplicationHelper.initiateCourtProceedingsForCourtApplication;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.addCourtApplication;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.addProsecutionCaseToCrownCourt;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.addStandaloneCourtApplication;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.pollForApplication;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.pollProsecutionCasesProgressionFor;
import static uk.gov.moj.cpp.progression.helper.QueueUtil.retrieveMessageBody;
import static uk.gov.moj.cpp.progression.helper.RestHelper.getJsonObject;
import static uk.gov.moj.cpp.progression.util.ReferProsecutionCaseToCrownCourtHelper.getProsecutionCaseMatchers;

import uk.gov.justice.services.integrationtest.utils.jms.JmsMessageConsumerClient;
import uk.gov.moj.cpp.progression.helper.CourtApplicationsHelper;

import java.util.Optional;

import javax.json.JsonObject;

import org.hamcrest.Matcher;
import org.hamcrest.Matchers;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

@SuppressWarnings("squid:S1607")
public class CreateCourtApplicationIT extends AbstractIT {
    private static final String COURT_APPLICATION_CREATED = "public.progression.court-application-created";
    private static final String COURT_APPLICATION_REJECTED = "public.progression.court-application-rejected";

    private static final JmsMessageConsumerClient consumerForCourtApplicationCreated = newPublicJmsMessageConsumerClientProvider().withEventNames(COURT_APPLICATION_CREATED).getMessageConsumerClient();
    private static final JmsMessageConsumerClient consumerForCourtApplicationRejected = newPublicJmsMessageConsumerClientProvider().withEventNames(COURT_APPLICATION_REJECTED).getMessageConsumerClient();

    private String caseId;
    private String defendantId;

    @BeforeEach
    public void setUp() {
        caseId = randomUUID().toString();
        defendantId = randomUUID().toString();
    }

    @Test
    public void shouldCreateStandaloneCourtApplicationAndGetConfirmation() throws Exception {

        String firstApplicationId = randomUUID().toString();
        addStandaloneCourtApplication(firstApplicationId, randomUUID().toString(), new CourtApplicationsHelper.CourtApplicationRandomValues(), "progression.command.create-standalone-court-application.json");

        verifyInMessagingQueueForStandaloneCourtApplicationCreated();

        String secondApplicationId = randomUUID().toString();
        addStandaloneCourtApplication(secondApplicationId, firstApplicationId, new CourtApplicationsHelper.CourtApplicationRandomValues(), "progression.command.create-standalone-court-application.json");

        verifyInMessagingQueueForStandaloneCourtApplicationCreated();

        Matcher[] matchers = {
                withJsonPath("$.courtApplication.id", is(firstApplicationId)),
                withJsonPath("$.courtApplication.applicationStatus", is("DRAFT")),
                withJsonPath("$.courtApplication.outOfTimeReasons", is("a")),
                withJsonPath("$.courtApplication.applicationReference", notNullValue(String.class))
//                withJsonPath("$.linkedApplicationsSummary[0].applicationStatus", is("DRAFT")),
//                withJsonPath("$.linkedApplicationsSummary[0].applicationTitle", is("a")),
//                withJsonPath("$.linkedApplicationsSummary[0].applicationReference", notNullValue(String.class)),
//                withJsonPath("$.linkedApplicationsSummary[0].applicantDisplayName", notNullValue(String.class)),
//                withJsonPath("$.linkedApplicationsSummary[0].respondentDisplayNames", notNullValue(JsonArray.class))
        };

        pollForApplication(firstApplicationId, matchers);
    }

    @Test
    public void shouldCreateCourtApplicationLinkedWithCaseAndGetConfirmation() throws Exception {

        // when
        addProsecutionCaseToCrownCourt(caseId, defendantId);
        String response = pollProsecutionCasesProgressionFor(caseId, getProsecutionCaseMatchers(caseId, defendantId));
        JsonObject prosecutionCasesJsonObject = getJsonObject(response);

        String reference = prosecutionCasesJsonObject.getJsonObject("prosecutionCase").getJsonObject("prosecutionCaseIdentifier").getString("prosecutionAuthorityReference");

        // Creating first application for the case
        String firstApplicationId = randomUUID().toString();
        initiateCourtProceedingsForCourtApplication(firstApplicationId, caseId, "applications/progression.initiate-court-proceedings-for-court-order-linked-application.json");

        verifyInMessagingQueueForCourtApplicationCreated(firstApplicationId);

        Matcher[] firstApplicationMatchers = {
                withJsonPath("$.courtApplication.id", is(firstApplicationId)),
                withJsonPath("$.courtApplication.applicationStatus", is("UN_ALLOCATED")),
                withJsonPath("$.courtApplication.outOfTimeReasons", is("Out of times reasons for linked application test")),
                withJsonPath("$.courtApplication.applicationReference", notNullValue()),
        };

        pollForApplication(firstApplicationId, firstApplicationMatchers);

        // Creating second application for the case
        String secondApplicationId = randomUUID().toString();
        initiateCourtProceedingsForCourtApplication(secondApplicationId, caseId, "applications/progression.initiate-court-proceedings-for-court-order-linked-application.json");

        verifyInMessagingQueueForCourtApplicationCreated(secondApplicationId);

        Matcher[] secondApplicationMatchers = {
                withJsonPath("$.courtApplication.id", is(secondApplicationId)),
                withJsonPath("$.courtApplication.applicationStatus", is("UN_ALLOCATED")),
                withJsonPath("$.courtApplication.outOfTimeReasons", is("Out of times reasons for linked application test")),
                withJsonPath("$.courtApplication.applicationReference", notNullValue()),
        };

        pollForApplication(secondApplicationId, secondApplicationMatchers);

        Matcher[] caseMatchers = {
                withJsonPath("$.prosecutionCase.id", is(caseId)),
                withJsonPath("$.linkedApplicationsSummary", hasSize(2)),
                withJsonPath("$.linkedApplicationsSummary[0].applicationTitle", is("Application for an order of reimbursement in relation to a closure order")),
                withJsonPath("$.linkedApplicationsSummary[0].isAppeal", is(true)),
                withJsonPath("$.linkedApplicationsSummary[0].applicationStatus", is("UN_ALLOCATED"))
        };

        pollProsecutionCasesProgressionFor(caseId, caseMatchers);
    }

    @Test
    public void shouldUpdateOffenceWordingWhenCourtOrderIsNotSuspendedSentence() throws Exception {

        // when
        // addProsecutionCaseToCrownCourt(caseId, defendantId);

        // Creating first application for the case
        String firstApplicationId = randomUUID().toString();
        initiateCourtProceedingsForCourtApplication(firstApplicationId, caseId, "applications/progression.initiate-court-proceedings-with-court-order-to-update-offence-wording.json");

        verifyInMessagingQueueForCourtApplicationCreated(firstApplicationId);
        final String expectedWording = "Original CaseURN: TVL1234, Re-sentenced Original code : CA03012, Original details: On 01/11/2017 at Chelmsford intentionally obstructed a person authorised by the BBC in the exercise of a power conferred by virtue of a search warrant issued under section 366 of the Communications Act 2003 by Chelmsford Magistrates Court on 25/10/2017 ";
        final String expectedWordingWelsh = "Original CaseURN: TVL1234, Re-sentenced Original code : CA03012, Original details: On 01/11/2017 at Chelmsford intentionally obstructed a person authorised by the BBC in the exercise of a power conferred by virtue of a search warrant issued under section 366 of the Communications Act 2003 by Chelmsford Magistrates Court ";
        Matcher[] firstApplicationMatchers = {
                withJsonPath("$.courtApplication.id", is(firstApplicationId)),
                withJsonPath("$.courtApplication.applicationStatus", is("UN_ALLOCATED")),
                withJsonPath("$.courtApplication.outOfTimeReasons", is("Out of times reasons for linked application test")),
                withJsonPath("$.courtApplication.applicationReference", notNullValue()),
                withJsonPath("$.courtApplication.courtOrder.courtOrderOffences[0].offence.offenceCode", is("AO0001")),
                withJsonPath("$.courtApplication.courtOrder.courtOrderOffences[0].offence.wording", is(expectedWording)),
                withJsonPath("$.courtApplication.courtOrder.courtOrderOffences[0].offence.wordingWelsh", is(expectedWordingWelsh)),
                withJsonPath("$.courtApplication.courtOrder.courtOrderOffences[0].offence.id", is("28b3d444-ae80-4920-a70f-ef01e128188e")),
                withJsonPath("$.courtApplication.courtOrder.id", is("8ab0af4c-db0e-4535-9775-d52669d6b07f")),
                withJsonPath("$.courtApplication.courtOrder.courtOrderOffences[0].prosecutionCaseId", is(caseId)),
                withJsonPath("$.courtApplication.courtOrder.courtOrderOffences[0].prosecutionCaseIdentifier.prosecutionAuthorityCode", is("4ZKOPhuyh5")),
        };

        pollForApplication(firstApplicationId, firstApplicationMatchers);

    }

    @Test
    public void shouldUpdateOffenceWordingWhenCourtOrderNotExist() throws Exception {

        // when
        // addProsecutionCaseToCrownCourt(caseId, defendantId);

        // Creating first application for the case
        String firstApplicationId = randomUUID().toString();
        initiateCourtProceedingsForCourtApplication(firstApplicationId, caseId, "applications/progression.initiate-court-proceedings-without-court-order-to-update-offence-wording.json");

        verifyInMessagingQueueForCourtApplicationCreated(firstApplicationId);
        final String expectedWording = "Resentenced Original code : CA03012, Original details: On 01/11/2017 at Chelmsford intentionally obstructed a person authorised by the BBC in the exercise of a power conferred by virtue of a search warrant issued under section 366 of the Communications Act 2003 by Chelmsford Magistrates Court on 25/10/2017 ";
        final String expectedWordingWelsh = "Resentenced Original code : CA03012, Original details: On 01/11/2017 at Chelmsford intentionally obstructed a person authorised by the BBC in the exercise of a power conferred by virtue of a search warrant issued under section 366 of the Communications Act 2003 by Chelmsford Magistrates Court ";
        Matcher[] firstApplicationMatchers = {
                withJsonPath("$.courtApplication.id", is(firstApplicationId)),
                withJsonPath("$.courtApplication.applicationStatus", is("UN_ALLOCATED")),
                withJsonPath("$.courtApplication.outOfTimeReasons", is("Out of times reasons for linked application test")),
                withJsonPath("$.courtApplication.applicationReference", notNullValue()),
                hasNoJsonPath("$.courtApplication.courtOrder"),
                withJsonPath("$.courtApplication.courtApplicationCases[0].isSJP", Matchers.is(false)),
                withJsonPath("$.courtApplication.courtApplicationCases[0].prosecutionCaseId", Matchers.is(caseId)),
                withJsonPath("$.courtApplication.courtApplicationCases[0].offences[0].id", Matchers.is("28b3d444-ae80-4920-a70f-ef01e128188e")),
                withJsonPath("$.courtApplication.courtApplicationCases[0].offences[0].offenceCode", Matchers.is("AO0001")),
                withJsonPath("$.courtApplication.courtApplicationCases[0].offences[0].wording", Matchers.is(expectedWording)),
                withJsonPath("$.courtApplication.courtApplicationCases[0].offences[0].wordingWelsh", Matchers.is(expectedWordingWelsh))

        };

        pollForApplication(firstApplicationId, firstApplicationMatchers);

    }

    @Test
    public void shouldUpdateOffenceWordingWhenCourtOrderIsSuspendedSentence() throws Exception {

        // when
        // addProsecutionCaseToCrownCourt(caseId, defendantId);

        // Creating first application for the case
        String firstApplicationId = randomUUID().toString();
        initiateCourtProceedingsForCourtApplication(firstApplicationId, caseId, "applications/progression.initiate-court-proceedings-with-court-order-to-update-offence-wording2.json");

        verifyInMessagingQueueForCourtApplicationCreated(firstApplicationId);
        final String expectedWording = "Activation of a suspended sentence order. Original CaseURN: TVL1234, Original code : CA03012, Original details: On 01/11/2017 at Chelmsford intentionally obstructed a person authorised by the BBC in the exercise of a power conferred by virtue of a search warrant issued under section 366 of the Communications Act 2003 by Chelmsford Magistrates Court on 25/10/2017 ";
        final String expectedWordingWelsh = "Activation of a suspended sentence order. Original CaseURN: TVL1234, Original code : CA03012, Original details: On 01/11/2017 at Chelmsford intentionally obstructed a person authorised by the BBC in the exercise of a power conferred by virtue of a search warrant issued under section 366 of the Communications Act 2003 by Chelmsford Magistrates Court ";
        Matcher[] firstApplicationMatchers = {
                withJsonPath("$.courtApplication.id", is(firstApplicationId)),
                withJsonPath("$.courtApplication.applicationStatus", is("UN_ALLOCATED")),
                withJsonPath("$.courtApplication.outOfTimeReasons", is("Out of times reasons for linked application test")),
                withJsonPath("$.courtApplication.applicationReference", notNullValue()),
                withJsonPath("$.courtApplication.courtOrder.courtOrderOffences[0].offence.offenceCode", is("AO0001")),
                withJsonPath("$.courtApplication.courtOrder.courtOrderOffences[0].offence.wording", is(expectedWording)),
                withJsonPath("$.courtApplication.courtOrder.courtOrderOffences[0].offence.wordingWelsh", is(expectedWordingWelsh)),
                withJsonPath("$.courtApplication.courtOrder.courtOrderOffences[0].offence.id", is("28b3d444-ae80-4920-a70f-ef01e128188e")),
                withJsonPath("$.courtApplication.courtOrder.id", is("8ab0af4c-db0e-4535-9775-d52669d6b07f")),
                withJsonPath("$.courtApplication.courtOrder.courtOrderOffences[0].prosecutionCaseId", is(caseId)),
                withJsonPath("$.courtApplication.courtOrder.courtOrderOffences[0].prosecutionCaseIdentifier.prosecutionAuthorityCode", is("4ZKOPhuyh5"))
        };

        pollForApplication(firstApplicationId, firstApplicationMatchers);

    }

    private static void verifyInMessagingQueueForCourtApplicationCreated(String applicationId) {
        final Optional<JsonObject> message = retrieveMessageBody(consumerForCourtApplicationCreated);
        assertTrue(message.isPresent());
        String idResponse = message.get().getJsonObject("courtApplication").getString("id");
        assertThat(idResponse, equalTo(applicationId));
    }

    private static void verifyInMessagingQueueForCourtApplicationRejected() {
        final Optional<JsonObject> message = retrieveMessageBody(consumerForCourtApplicationRejected);
        assertTrue(message.isPresent());
    }

    private static void verifyInMessagingQueueForStandaloneCourtApplicationCreated() {
        final Optional<JsonObject> message = retrieveMessageBody(consumerForCourtApplicationCreated);
        assertTrue(message.isPresent());
        String referenceResponse = message.get().getJsonObject("courtApplication").getString("applicationReference");
        assertThat(10, is(referenceResponse.length()));
    }

}

