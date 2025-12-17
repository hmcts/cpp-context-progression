package uk.gov.moj.cpp.progression;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static java.util.Collections.emptyList;
import static java.util.UUID.randomUUID;
import static javax.json.Json.createObjectBuilder;
import static org.apache.http.HttpStatus.SC_ACCEPTED;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static uk.gov.justice.services.integrationtest.utils.jms.JmsMessageConsumerClientProvider.newPublicJmsMessageConsumerClientProvider;
import static uk.gov.moj.cpp.progression.applications.applicationHelper.ApplicationHelper.initiateCourtProceedingsForCourtApplication;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.civilCaseInitiateCourtProceedings;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.getApplicationFor;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.pollForApplication;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.pollProsecutionCasesProgressionFor;
import static uk.gov.moj.cpp.progression.helper.QueueUtil.retrieveMessageBody;
import static uk.gov.moj.cpp.progression.stub.HearingStub.stubInitiateHearing;
import static uk.gov.moj.cpp.progression.util.ReferProsecutionCaseToCrownCourtHelper.getCivilProsecutionCaseMatchers;
import uk.gov.justice.core.courts.CourtApplicationPayment;
import uk.gov.justice.core.courts.FeeStatus;
import uk.gov.justice.services.common.converter.StringToJsonObjectConverter;
import uk.gov.justice.services.common.converter.ZonedDateTimes;
import uk.gov.justice.services.integrationtest.utils.jms.JmsMessageConsumerClient;
import uk.gov.moj.cpp.progression.applications.applicationHelper.ApplicationHelper;

import java.io.IOException;
import java.time.LocalDate;
import java.util.Optional;
import java.util.stream.IntStream;

import javax.json.JsonObject;

import com.jayway.jsonpath.ReadContext;
import io.restassured.response.Response;
import org.hamcrest.Matcher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

@SuppressWarnings("squid:S1607")
class EditCivilApplicationFeeIT extends AbstractIT {

    private JmsMessageConsumerClient publicCourtProceedingsInitiatedEventConsumer;
    private JmsMessageConsumerClient publicCaseRemovedEventConsumer;
    private JmsMessageConsumerClient consumerForCourtProceedingsInitiated;
    private JmsMessageConsumerClient consumerForCaseGroupInfoUpdated;
    private JmsMessageConsumerClient consumerForDefendantListingStatusChanged;
    private JmsMessageConsumerClient consumerForHearingResultedCaseUpdated;
    private JmsMessageConsumerClient consumerForLinkProsecutionCasesToHearing;
    private JmsMessageConsumerClient publicCivilCaseExistsEventConsumer;
    private JmsMessageConsumerClient publicLastCaseRemoveErrorEventConsumer;

    private String caseId;
    private String materialIdActive;
    private String materialIdDeleted;
    private String defendantId;
    private String referralReasonId;
    private String listedStartDateTime;
    private String earliestStartDateTime;
    private String defendantDOB;
    private String feesId;

    private final StringToJsonObjectConverter stringToJsonObjectConverter = new StringToJsonObjectConverter();


    private static final String COURT_APPLICATION_CREATED = "public.progression.court-application-created";
    //progression.event.court-fee-for-civil-application-updated
    private static final String COURT_FEE_FOR_CIVIL_APPLICATION_UPDATED = "public.progression.court-fee-for-civil-application-updated";

    private static final JmsMessageConsumerClient consumerForApplicationFeeEdited = newPublicJmsMessageConsumerClientProvider().withEventNames(COURT_FEE_FOR_CIVIL_APPLICATION_UPDATED).getMessageConsumerClient();
    private static final JmsMessageConsumerClient consumerForCourtApplicationCreated = newPublicJmsMessageConsumerClientProvider().withEventNames(COURT_APPLICATION_CREATED).getMessageConsumerClient();

    @BeforeEach
    public void setUp() {
        stubInitiateHearing();
        caseId = randomUUID().toString();
        materialIdActive = randomUUID().toString();
        materialIdDeleted = randomUUID().toString();
        defendantId = randomUUID().toString();
        referralReasonId = randomUUID().toString();
        listedStartDateTime = ZonedDateTimes.fromString("2019-06-30T18:32:04.238Z").toString();
        earliestStartDateTime = ZonedDateTimes.fromString("2019-05-30T18:32:04.238Z").toString();
        defendantDOB = LocalDate.now().minusYears(15).toString();
        feesId = randomUUID().toString();
        publicCourtProceedingsInitiatedEventConsumer = newPublicJmsMessageConsumerClientProvider().withEventNames("public.progression.prosecution-case-created").getMessageConsumerClient();
        consumerForCourtProceedingsInitiated = newPublicJmsMessageConsumerClientProvider().withEventNames("progression.event.court-proceedings-initiated").getMessageConsumerClient();
        consumerForCaseGroupInfoUpdated = newPublicJmsMessageConsumerClientProvider().withEventNames("progression.event.case-group-info-updated").getMessageConsumerClient();
        publicCaseRemovedEventConsumer = newPublicJmsMessageConsumerClientProvider().withEventNames("public.progression.case-removed-from-group-cases").getMessageConsumerClient();
        consumerForDefendantListingStatusChanged = newPublicJmsMessageConsumerClientProvider().withEventNames("progression.event.prosecutionCase-defendant-listing-status-changed-v2").getMessageConsumerClient();
        consumerForHearingResultedCaseUpdated = newPublicJmsMessageConsumerClientProvider().withEventNames("progression.event.hearing-resulted-case-updated").getMessageConsumerClient();
        consumerForLinkProsecutionCasesToHearing = newPublicJmsMessageConsumerClientProvider().withEventNames("progression.event-link-prosecution-cases-to-hearing").getMessageConsumerClient();
        this.publicCivilCaseExistsEventConsumer = newPublicJmsMessageConsumerClientProvider().withEventNames("public.progression.events.civil-case-exists").getMessageConsumerClient();
        publicLastCaseRemoveErrorEventConsumer = newPublicJmsMessageConsumerClientProvider().withEventNames("public.progression.remove-last-case-in-group-cases-rejected").getMessageConsumerClient();
    }

    @Test
    void shouldInitiateCourtProceedingsForCivilCase() throws IOException {
        //given
        civilCaseInitiateCourtProceedings(caseId, defendantId, materialIdActive, materialIdDeleted, referralReasonId, listedStartDateTime, earliestStartDateTime, defendantDOB, feesId);
        //when
        verifyInMessagingQueueForNumberOfTimes(1, publicCourtProceedingsInitiatedEventConsumer);

        final Matcher<? super ReadContext>[] prosecutionCaseMatchers = getCivilProsecutionCaseMatchers(caseId, defendantId, emptyList());

        pollProsecutionCasesProgressionFor(caseId, prosecutionCaseMatchers);

        String firstApplicationId = randomUUID().toString();
        initiateCourtProceedingsForCourtApplication(firstApplicationId, caseId, "applications/progression.initiate-court-proceedings-for-court-order-linked-civil-application-with-fee.json");

        verifyInMessagingQueueForCourtApplicationCreated(firstApplicationId);

        Matcher[] firstApplicationMatchers = {
                withJsonPath("$.courtApplication.id", is(firstApplicationId)),
                withJsonPath("$.courtApplication.applicationStatus", is("UN_ALLOCATED")),
                withJsonPath("$.courtApplication.outOfTimeReasons", is("Out of times reasons for linked application test")),
                withJsonPath("$.courtApplication.applicationReference", notNullValue()),
        };

        pollForApplication(firstApplicationId, firstApplicationMatchers);

        final CourtApplicationPayment updatedCourtApplicationPayment = CourtApplicationPayment.courtApplicationPayment()
                .withPaymentReference("first change")
                .withFeeStatus(FeeStatus.REDUCED)
                .withContestedFeeStatus(FeeStatus.NOT_APPLICABLE)
                .withContestedPaymentReference("no applicaton contested")
                .build();

        Response response = editCivilApplicationFee(firstApplicationId, updatedCourtApplicationPayment);
        assertThat(response.getStatusCode(), equalTo(SC_ACCEPTED));
        verifyInMessagingQueueForCourtApplicationFeeUpdated(updatedCourtApplicationPayment);
        verifyUpdatedApplicationFeeStatus(firstApplicationId, updatedCourtApplicationPayment);

    }

    private void verifyUpdatedApplicationFeeStatus(final String applicationId, final CourtApplicationPayment updatedCourtApplicationPayment) {

        final String payload = getApplicationFor(applicationId);
        JsonObject json = stringToJsonObjectConverter.convert(payload);
        final JsonObject jsonObject = json.getJsonObject("courtApplication").getJsonObject("courtApplicationPayment");
        assertEquals(updatedCourtApplicationPayment.getFeeStatus().toString(), jsonObject.getString("feeStatus"));
        assertEquals(updatedCourtApplicationPayment.getPaymentReference(), jsonObject.getString("paymentReference"));
        assertEquals(updatedCourtApplicationPayment.getContestedFeeStatus().toString(), jsonObject.getString("contestedFeeStatus"));
        assertEquals(updatedCourtApplicationPayment.getContestedPaymentReference(), jsonObject.getString("contestedPaymentReference"));
    }

    private static void verifyInMessagingQueueForCourtApplicationCreated(String applicationId) {
        final Optional<JsonObject> message = retrieveMessageBody(consumerForCourtApplicationCreated);
        assertTrue(message.isPresent());
        String idResponse = message.get().getJsonObject("courtApplication").getString("id");
        assertThat(idResponse, equalTo(applicationId));
    }

    private static void verifyInMessagingQueueForCourtApplicationFeeUpdated(final CourtApplicationPayment updatedCourtApplicationPayment) {
        final Optional<JsonObject> message = retrieveMessageBody(consumerForApplicationFeeEdited);
        assertTrue(message.isPresent());

        final JsonObject courtApplicationPayment = message.get().getJsonObject("courtApplicationPayment");

        assertThat(courtApplicationPayment.getString("feeStatus"), equalTo(updatedCourtApplicationPayment.getFeeStatus().toString()));
        assertThat(courtApplicationPayment.getString("paymentReference"), equalTo(updatedCourtApplicationPayment.getPaymentReference()));
        assertThat(courtApplicationPayment.getString("contestedFeeStatus"), equalTo(updatedCourtApplicationPayment.getContestedFeeStatus().toString()));
        assertThat(courtApplicationPayment.getString("contestedPaymentReference"), equalTo(updatedCourtApplicationPayment.getContestedPaymentReference()));
    }

    private void verifyInMessagingQueueForNumberOfTimes(final int times, final JmsMessageConsumerClient messageConsumer) {
        IntStream.range(0, times).forEach(i -> {
                    final Optional<JsonObject> message = retrieveMessageBody(messageConsumer);
                    assertTrue(message.isPresent());
                }
        );
    }

    private Response editCivilApplicationFee(final String applicationId, final CourtApplicationPayment courtApplicationPayment) throws IOException {
        final JsonObject editFeePayload = createObjectBuilder()
                .add("applicationId", applicationId)
                .add("courtApplicationPayment", createObjectBuilder()
                        .add("feeStatus", courtApplicationPayment.getFeeStatus().toString())
                        .add("paymentReference", courtApplicationPayment.getPaymentReference())
                        .add("contestedFeeStatus", courtApplicationPayment.getContestedFeeStatus().toString())
                        .add("contestedPaymentReference", courtApplicationPayment.getContestedPaymentReference())
                        .build())
                .build();

        return ApplicationHelper.editCivilFeeForCivilCourtApplication(editFeePayload.toString());
    }

}