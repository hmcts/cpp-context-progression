package uk.gov.moj.cpp.progression;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.isJson;
import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static com.jayway.jsonpath.matchers.JsonPathMatchers.withoutJsonPath;
import static java.util.UUID.randomUUID;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.allOf;
import static uk.gov.justice.services.integrationtest.utils.jms.JmsMessageConsumerClientProvider.newPrivateJmsMessageConsumerClientProvider;
import static uk.gov.justice.services.integrationtest.utils.jms.JmsMessageConsumerClientProvider.newPublicJmsMessageConsumerClientProvider;
import static uk.gov.justice.services.integrationtest.utils.jms.JmsMessageProducerClientProvider.newPublicJmsMessageProducerClientProvider;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.addProsecutionCaseToCrownCourtWithDefendantAsAdult;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.initiateCourtProceedings;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.pollProsecutionCasesProgressionFor;
import static uk.gov.moj.cpp.progression.helper.QueueUtil.buildMetadata;
import static uk.gov.moj.cpp.progression.helper.QueueUtil.retrieveMessageAsJsonPath;
import static uk.gov.moj.cpp.progression.helper.QueueUtil.retrieveMessageBody;
import static uk.gov.moj.cpp.progression.it.framework.ContextNameProvider.CONTEXT_NAME;
import static uk.gov.moj.cpp.progression.util.FileUtil.getPayload;
import static uk.gov.moj.cpp.progression.util.ReferProsecutionCaseToCrownCourtHelper.getProsecutionCaseMatchers;

import uk.gov.justice.services.common.converter.StringToJsonObjectConverter;
import uk.gov.justice.services.common.converter.ZonedDateTimes;
import uk.gov.justice.services.integrationtest.utils.jms.JmsMessageConsumerClient;
import uk.gov.justice.services.integrationtest.utils.jms.JmsMessageProducerClient;
import uk.gov.justice.services.integrationtest.utils.jms.JmsResourceManagementExtension;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.progression.stub.HearingStub;
import uk.gov.moj.cpp.progression.util.ProsecutionCaseUpdateCaseMarkersHelper;

import java.time.LocalDate;
import java.util.Optional;

import javax.json.JsonObject;

import io.restassured.path.json.JsonPath;
import org.hamcrest.CoreMatchers;
import org.hamcrest.Matcher;
import org.junit.Assert;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(JmsResourceManagementExtension.class)
public class UpdateCaseMarkersIT {
    private static final String PROGRESSION_COMMAND_INITIATE_COURT_PROCEEDINGS = "progression.command.initiate-court-proceedings.json";
    private static final String PUBLIC_LISTING_HEARING_CONFIRMED = "public.listing.hearing-confirmed";

    private String caseId;
    private String materialIdActive;
    private String materialIdDeleted;
    private String defendantId;
    private String referralReasonId;
    private String listedStartDateTime;
    private String earliestStartDateTime;
    private String defendantDOB;

    private ProsecutionCaseUpdateCaseMarkersHelper helper;
    private static final JmsMessageConsumerClient messageConsumerHearingPopulatedToProbationCaseWorker = newPrivateJmsMessageConsumerClientProvider(CONTEXT_NAME).withEventNames("progression.events.hearing-populated-to-probation-caseworker").getMessageConsumerClient();
    private static final JmsMessageConsumerClient privateEventsConsumer = newPrivateJmsMessageConsumerClientProvider(CONTEXT_NAME).withEventNames("progression.event.case-markers-updated").getMessageConsumerClient();
    private static final JmsMessageConsumerClient publicEventsCaseMarkersUpdated = newPublicJmsMessageConsumerClientProvider().withEventNames("public.progression.case-markers-updated").getMessageConsumerClient();
    private final StringToJsonObjectConverter stringToJsonObjectConverter = new StringToJsonObjectConverter();
    private static final JmsMessageProducerClient messageProducerClientPublic = newPublicJmsMessageProducerClientProvider().getMessageProducerClient();

    @BeforeEach
    public void setUp() {
        caseId = randomUUID().toString();
        materialIdActive = randomUUID().toString();
        materialIdDeleted = randomUUID().toString();
        defendantId = randomUUID().toString();
        referralReasonId = randomUUID().toString();
        listedStartDateTime = ZonedDateTimes.fromString("2019-06-30T18:32:04.238Z").toString();
        earliestStartDateTime = ZonedDateTimes.fromString("2019-05-30T18:32:04.238Z").toString();
        defendantDOB = LocalDate.now().minusYears(15).toString();
        helper = new ProsecutionCaseUpdateCaseMarkersHelper(caseId);
    }

    @Test
    public void shouldUpdateProsecutionCaseMarkers() throws Exception {
        // given

        //given
        initiateCourtProceedings(PROGRESSION_COMMAND_INITIATE_COURT_PROCEEDINGS, caseId, defendantId, materialIdActive, materialIdDeleted, referralReasonId, listedStartDateTime, earliestStartDateTime, defendantDOB);

        pollProsecutionCasesProgressionFor(caseId, getCaseMarkersMatchers("WP", "Prohibited Weapons"));

        helper.updateCaseMarkers(privateEventsConsumer);

        pollProsecutionCasesProgressionFor(caseId, getCaseMarkersMatchers("DD", "Child Abuse"));

        helper.verifyInMessagingQueueForCaseMarkersUpdated(publicEventsCaseMarkersUpdated);
    }

    @Test
    public void shouldUpdateHearingCaseMarkers() throws Exception {
        HearingStub.stubInitiateHearing();
        //given
        final String hearingId;
        final JmsMessageConsumerClient messageConsumerProsecutionCaseDefendantListingStatusChanged = newPrivateJmsMessageConsumerClientProvider(CONTEXT_NAME).withEventNames("progression.event.prosecutionCase-defendant-listing-status-changed-v2").getMessageConsumerClient();

        addProsecutionCaseToCrownCourtWithDefendantAsAdult(caseId, defendantId);
        pollProsecutionCasesProgressionFor(caseId, getProsecutionCaseMatchers(caseId, defendantId));

        hearingId = doVerifyProsecutionCaseDefendantListingStatusChanged(messageConsumerProsecutionCaseDefendantListingStatusChanged);

        final JsonObject hearingConfirmedJson = getHearingJsonObject("public.listing.hearing-confirmed.json", caseId, hearingId, defendantId, randomUUID().toString(), "Lavender Hill Magistrate's Court");

        final JmsMessageConsumerClient messageConsumerProsecutionCaseDefendantListingStatusChanged2 = newPrivateJmsMessageConsumerClientProvider(CONTEXT_NAME).withEventNames("progression.event.prosecutionCase-defendant-listing-status-changed-v2").getMessageConsumerClient();

        final JsonEnvelope publicEventEnvelope = JsonEnvelope.envelopeFrom(buildMetadata(PUBLIC_LISTING_HEARING_CONFIRMED, randomUUID()), hearingConfirmedJson);
        messageProducerClientPublic.sendMessage(PUBLIC_LISTING_HEARING_CONFIRMED, publicEventEnvelope);

        doVerifyProsecutionCaseDefendantListingStatusChanged(messageConsumerProsecutionCaseDefendantListingStatusChanged2);
        helper.updateCaseMarkers(privateEventsConsumer);

        pollProsecutionCasesProgressionFor(caseId, getCaseMarkersMatchers("DD", "Child Abuse"));

        helper.verifyInMessagingQueueForCaseMarkersUpdated(publicEventsCaseMarkersUpdated);

        final JsonPath messageDaysMatchers = retrieveMessageAsJsonPath(messageConsumerHearingPopulatedToProbationCaseWorker, isJson(allOf(
                withJsonPath("$.hearing.id", CoreMatchers.is(hearingId)),
                withJsonPath("$.hearing.prosecutionCases[0].caseMarkers[0].markerTypeCode", is("DD")),
                withJsonPath("$.hearing.prosecutionCases[0].caseMarkers[0].markerTypeDescription", is("Child Abuse")))));
        Assert.assertNotNull(messageDaysMatchers);
    }

    @Test
    public void shouldRemoveProsecutionCaseMarkers() throws Exception {
        //given
        initiateCourtProceedings(PROGRESSION_COMMAND_INITIATE_COURT_PROCEEDINGS, caseId, defendantId, materialIdActive, materialIdDeleted, referralReasonId, listedStartDateTime, earliestStartDateTime, defendantDOB);

        pollProsecutionCasesProgressionFor(caseId, getCaseMarkersMatchers("WP", "Prohibited Weapons"));

        helper.removeCaseMarkers(privateEventsConsumer);

        pollProsecutionCasesProgressionFor(caseId, withoutJsonPath("$.prosecutionCase.caseMarkers"));

        helper.verifyInMessagingQueueForCaseMarkersUpdated(publicEventsCaseMarkersUpdated);
    }

    private Matcher[] getCaseMarkersMatchers(final String caseMarkerCode, final String caseMarkerDesc) {
        return new Matcher[]{
                withJsonPath("$.prosecutionCase.caseMarkers[0].markerTypeCode", is(caseMarkerCode)),
                withJsonPath("$.prosecutionCase.caseMarkers[0].markerTypeDescription", is(caseMarkerDesc))
        };

    }

    private String doVerifyProsecutionCaseDefendantListingStatusChanged(final JmsMessageConsumerClient messageConsumerProsecutionCaseDefendantListingStatusChanged) {
        final Optional<JsonObject> message = retrieveMessageBody(messageConsumerProsecutionCaseDefendantListingStatusChanged);
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
}
