package uk.gov.moj.cpp.progression;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.isJson;
import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static com.jayway.jsonpath.matchers.JsonPathMatchers.withoutJsonPath;
import static java.util.UUID.randomUUID;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.allOf;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.addProsecutionCaseToCrownCourtWithDefendantAsAdult;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.initiateCourtProceedings;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.pollProsecutionCasesProgressionFor;
import static uk.gov.moj.cpp.progression.helper.QueueUtil.privateEvents;
import static uk.gov.moj.cpp.progression.helper.QueueUtil.publicEvents;
import static uk.gov.moj.cpp.progression.helper.QueueUtil.sendMessage;
import static uk.gov.moj.cpp.progression.util.FileUtil.getPayload;
import static uk.gov.moj.cpp.progression.util.ReferProsecutionCaseToCrownCourtHelper.getProsecutionCaseMatchers;


import com.jayway.restassured.path.json.JsonPath;
import java.util.Optional;
import javax.jms.JMSException;
import javax.jms.MessageConsumer;
import javax.jms.MessageProducer;
import javax.json.JsonObject;
import org.hamcrest.CoreMatchers;
import org.junit.After;
import org.junit.Assert;
import uk.gov.justice.services.common.converter.StringToJsonObjectConverter;
import uk.gov.justice.services.common.converter.ZonedDateTimes;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.messaging.Metadata;
import uk.gov.moj.cpp.progression.helper.QueueUtil;
import uk.gov.moj.cpp.progression.stub.HearingStub;
import uk.gov.moj.cpp.progression.util.ProsecutionCaseUpdateCaseMarkersHelper;

import java.time.LocalDate;

import org.hamcrest.Matcher;
import org.junit.Before;
import org.junit.Test;

public class UpdateCaseMarkersIT extends AbstractIT {
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
    private MessageConsumer messageConsumerHearingPopulatedToProbationCaseWorker;
    private final StringToJsonObjectConverter stringToJsonObjectConverter = new StringToJsonObjectConverter();
    private MessageProducer messageProducerClientPublic;

    @After
    public void tearDown() throws JMSException {
        messageConsumerHearingPopulatedToProbationCaseWorker.close();
        messageProducerClientPublic.close();
    }

    @Before
    public void setUp() {
        messageConsumerHearingPopulatedToProbationCaseWorker = privateEvents.createPrivateConsumer("progression.events.hearing-populated-to-probation-caseworker");
        messageProducerClientPublic = publicEvents.createPublicProducer();

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

        helper.updateCaseMarkers();

        helper.verifyInActiveMQ();

        pollProsecutionCasesProgressionFor(caseId, getCaseMarkersMatchers("DD", "Child Abuse"));

        helper.verifyInMessagingQueueForCaseMarkersUpdated();
    }

    @Test
    public void shouldUpdateHearingCaseMarkers() throws Exception {
        HearingStub.stubInitiateHearing();
        //given
        final String hearingId;
        try (final MessageConsumer messageConsumerProsecutionCaseDefendantListingStatusChanged = privateEvents
                .createPrivateConsumer("progression.event.prosecutionCase-defendant-listing-status-changed-v2")) {
            addProsecutionCaseToCrownCourtWithDefendantAsAdult(caseId, defendantId);
            pollProsecutionCasesProgressionFor(caseId, getProsecutionCaseMatchers(caseId, defendantId));

            hearingId = doVerifyProsecutionCaseDefendantListingStatusChanged(messageConsumerProsecutionCaseDefendantListingStatusChanged);
        }

        final Metadata metadata = JsonEnvelope.metadataBuilder()
                .withId(randomUUID())
                .withName(PUBLIC_LISTING_HEARING_CONFIRMED)
                .withUserId(randomUUID().toString())
                .build();

        final JsonObject hearingConfirmedJson = getHearingJsonObject("public.listing.hearing-confirmed.json", caseId, hearingId, defendantId, randomUUID().toString(), "Lavender Hill Magistrate's Court");


        try (final MessageConsumer messageConsumerProsecutionCaseDefendantListingStatusChanged = privateEvents
                .createPrivateConsumer("progression.event.prosecutionCase-defendant-listing-status-changed-v2")) {

            sendMessage(messageProducerClientPublic,
                    PUBLIC_LISTING_HEARING_CONFIRMED, hearingConfirmedJson, metadata);

            doVerifyProsecutionCaseDefendantListingStatusChanged(messageConsumerProsecutionCaseDefendantListingStatusChanged);
        }

        helper.updateCaseMarkers();

        helper.verifyInActiveMQ();

        pollProsecutionCasesProgressionFor(caseId, getCaseMarkersMatchers("DD", "Child Abuse"));

        helper.verifyInMessagingQueueForCaseMarkersUpdated();

        final JsonPath messageDaysMatchers = QueueUtil.retrieveMessage(messageConsumerHearingPopulatedToProbationCaseWorker, isJson(allOf(
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

        helper.removeCaseMarkers();

        helper.verifyInActiveMQ();

        pollProsecutionCasesProgressionFor(caseId, withoutJsonPath("$.prosecutionCase.caseMarkers"));

        helper.verifyInMessagingQueueForCaseMarkersUpdated();
    }

    private Matcher[] getCaseMarkersMatchers(final String caseMarkerCode, final String caseMarkerDesc) {
        return new Matcher[]{
                withJsonPath("$.prosecutionCase.caseMarkers[0].markerTypeCode", is(caseMarkerCode)),
                withJsonPath("$.prosecutionCase.caseMarkers[0].markerTypeDescription", is(caseMarkerDesc))
        };

    }

    private String doVerifyProsecutionCaseDefendantListingStatusChanged(final MessageConsumer messageConsumerProsecutionCaseDefendantListingStatusChanged) {
        final Optional<JsonObject> message = QueueUtil.retrieveMessageAsJsonObject(messageConsumerProsecutionCaseDefendantListingStatusChanged);
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
