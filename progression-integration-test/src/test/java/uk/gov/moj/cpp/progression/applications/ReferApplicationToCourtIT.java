package uk.gov.moj.cpp.progression.applications;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static java.util.Collections.singletonList;
import static java.util.UUID.randomUUID;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static uk.gov.justice.services.integrationtest.utils.jms.JmsMessageConsumerClientProvider.newPublicJmsMessageConsumerClientProvider;
import static uk.gov.justice.services.integrationtest.utils.jms.JmsMessageProducerClientProvider.newPublicJmsMessageProducerClientProvider;
import static uk.gov.moj.cpp.progression.applications.applicationHelper.ApplicationHelper.initiateCourtProceedingsForCourtApplication;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.addProsecutionCaseToCrownCourt;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.addProsecutionCaseToCrownCourtWithDefendantAsAdult;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.getHearingForDefendant;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.pollCaseAndGetHearingForDefendant;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.pollForApplication;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.pollProsecutionCasesProgressionFor;
import static uk.gov.moj.cpp.progression.helper.QueueUtil.buildMetadata;
import static uk.gov.moj.cpp.progression.stub.ListingStub.verifyPostListCourtHearing;
import static uk.gov.moj.cpp.progression.stub.ProbationCaseworkerStub.verifyProbationHearingCommandInvoked;
import static uk.gov.moj.cpp.progression.util.FileUtil.getPayload;
import static uk.gov.moj.cpp.progression.util.ReferApplicationToCourtHelper.verifyPublicEventForHearingExtended;
import static uk.gov.moj.cpp.progression.util.ReferProsecutionCaseToCrownCourtHelper.getProsecutionCaseMatchers;

import uk.gov.justice.services.common.converter.StringToJsonObjectConverter;
import uk.gov.justice.services.integrationtest.utils.jms.JmsMessageConsumerClient;
import uk.gov.justice.services.integrationtest.utils.jms.JmsMessageProducerClient;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.progression.AbstractIT;

import javax.json.JsonObject;

import org.hamcrest.Matcher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class ReferApplicationToCourtIT extends AbstractIT {

    private static final String PUBLIC_LISTING_HEARING_CONFIRMED = "public.listing.hearing-confirmed";

    private final StringToJsonObjectConverter stringToJsonObjectConverter = new StringToJsonObjectConverter();

    private String applicationId;
    private String caseId_1;
    private String defendantId_1;
    private String caseId_2;
    private String defendantId_2;
    private String caseId;
    private String defendantId;

    private final JmsMessageProducerClient messageProducerClientPublic = newPublicJmsMessageProducerClientProvider().getMessageProducerClient();
    private final JmsMessageConsumerClient publicEventsConsumerForHearingExtended = newPublicJmsMessageConsumerClientProvider().withEventNames("public.progression.events.hearing-extended").getMessageConsumerClient();

    @BeforeEach
    public void setUp() {
        caseId_1 = randomUUID().toString();
        defendantId_1 = randomUUID().toString();
        caseId_2 = randomUUID().toString();
        defendantId_2 = randomUUID().toString();
        applicationId = randomUUID().toString();
        caseId = randomUUID().toString();
        defendantId = randomUUID().toString();
    }

    @Test
    public void shouldReferApplicationToExistingHearing() throws Exception {
        addProsecutionCaseToCrownCourtWithDefendantAsAdult(caseId_1, defendantId_1);
        pollProsecutionCasesProgressionFor(caseId_1, getProsecutionCaseMatchers(caseId_1, defendantId_1));
        addProsecutionCaseToCrownCourtWithDefendantAsAdult(caseId_2, defendantId_2);
        pollProsecutionCasesProgressionFor(caseId_2, getProsecutionCaseMatchers(caseId_2, defendantId_2));

        String hearingId = pollCaseAndGetHearingForDefendant(caseId_1, defendantId_1);

        final JsonObject hearingConfirmedJson = getHearingJsonObject("public.listing.hearing-confirmed.json", caseId_1, hearingId, defendantId_1, randomUUID().toString(), "Lavender Hill Magistrate's Court");

        final JsonEnvelope publicEventEnvelope = JsonEnvelope.envelopeFrom(buildMetadata(PUBLIC_LISTING_HEARING_CONFIRMED, randomUUID()), hearingConfirmedJson);
        messageProducerClientPublic.sendMessage(PUBLIC_LISTING_HEARING_CONFIRMED, publicEventEnvelope);

        getHearingForDefendant(hearingId, new Matcher[]{withJsonPath("$.hearingListingStatus", is("HEARING_INITIALISED"))});

        initiateCourtProceedingsForCourtApplication(applicationId, caseId_1, caseId_2, "applications/progression.initiate-court-proceedings-for-refer-to-court.json", hearingId);
        verifyPublicEventForHearingExtended(hearingId, publicEventsConsumerForHearingExtended);
        verifyHearingPopulatedToProbationCaseWorker(hearingId);
    }

    @Test
    public void shouldListCourtHearing() throws Exception {

        addProsecutionCaseToCrownCourt(caseId, defendantId);
        pollProsecutionCasesProgressionFor(caseId, getProsecutionCaseMatchers(caseId, defendantId));

        // Create application for the case
        initiateCourtProceedingsForCourtApplication(applicationId, caseId, "applications/progression.initiate-court-proceedings-for-court-order-linked-application.json");

        final Matcher[] firstApplicationMatchers = {
                withJsonPath("$.courtApplication.id", is(applicationId)),
                withJsonPath("$.courtApplication.applicationStatus", is("UN_ALLOCATED")),
                withJsonPath("$.courtApplication.outOfTimeReasons", is("Out of times reasons for linked application test")),
                withJsonPath("$.courtApplication.applicationReference", notNullValue(String.class)),
        };

        pollForApplication(applicationId, firstApplicationMatchers);

        verifyPostListCourtHearing(applicationId);

        Matcher[] caseMatchers = {
                withJsonPath("$.prosecutionCase.id", is(caseId)),
                withJsonPath("$.linkedApplicationsSummary", hasSize(1)),
                withJsonPath("$.linkedApplicationsSummary[0].applicationStatus", is("UN_ALLOCATED"))
        };

        pollProsecutionCasesProgressionFor(caseId, caseMatchers);
    }

    private void verifyHearingPopulatedToProbationCaseWorker(final String hearingId) {
        verifyProbationHearingCommandInvoked(singletonList(hearingId));
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
}
