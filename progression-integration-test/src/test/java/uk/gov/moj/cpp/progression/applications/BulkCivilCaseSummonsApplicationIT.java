package uk.gov.moj.cpp.progression.applications;

import static java.util.Collections.singletonList;
import static java.util.UUID.randomUUID;
import static org.apache.commons.lang3.RandomStringUtils.randomAlphanumeric;
import static uk.gov.justice.services.integrationtest.utils.jms.JmsMessageProducerClientProvider.newPrivateJmsMessageProducerClientProvider;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.justice.core.courts.CourtApplicationPartyListingNeeds.courtApplicationPartyListingNeeds;
import static uk.gov.justice.core.courts.CourtCentre.courtCentre;
import static uk.gov.justice.core.courts.SummonsApprovedOutcome.summonsApprovedOutcome;
import static uk.gov.justice.core.courts.SummonsData.summonsData;
import static uk.gov.justice.core.courts.SummonsDataPrepared.summonsDataPrepared;
import static uk.gov.justice.core.courts.SummonsType.APPLICATION;
import static uk.gov.moj.cpp.progression.applications.applicationHelper.ApplicationHelper.initiateCourtProceedingsForCourtApplication;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.initiateCourtProceedingsForGroupCases;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.pollProsecutionCasesProgressionFor;
import static uk.gov.moj.cpp.progression.helper.QueueUtil.buildMetadata;
import static uk.gov.moj.cpp.progression.it.framework.ContextNameProvider.CONTEXT_NAME;
import static uk.gov.moj.cpp.progression.stub.NotificationServiceStub.verifyEmailNotificationIsRaisedWithoutAttachment;
import static uk.gov.moj.cpp.progression.stub.NotificationServiceStub.verifyNoLetterNotificationIsRaisedWithContent;
import static uk.gov.moj.cpp.progression.stub.ReferenceDataStub.ENGLISH_COURT_ID;

import uk.gov.justice.core.courts.CourtCentre;
import uk.gov.justice.core.courts.SummonsApprovedOutcome;
import uk.gov.justice.core.courts.SummonsData;
import uk.gov.justice.core.courts.SummonsDataPrepared;
import uk.gov.justice.services.common.converter.ObjectToJsonObjectConverter;
import uk.gov.justice.services.common.converter.jackson.ObjectMapperProducer;
import uk.gov.justice.services.integrationtest.utils.jms.JmsMessageProducerClient;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.progression.AbstractIT;
import uk.gov.moj.cpp.progression.util.Pair;

import java.io.IOException;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.json.JsonObject;

import org.json.JSONException;
import org.junit.jupiter.api.Test;

/**
 * Covers CAD-1275: a bulk civil case (initiation code S, summons code A) with a linked
 * "Application for first hearing summons" must not generate a summons when the application is
 * resulted with SA (summons approved) and the hearing is shared - instead a single
 * bulk-summons-approved email is sent to the prosecutor, without attachment.
 * <p>
 * The real end-to-end trigger (box-work referral -> judicial SA result -> hearing-resulted v2 ->
 * court-application-summons-approved -> initiate-court-hearing-after-summons-approved ->
 * hearing-confirmed) is exercised by other application-summons ITs. Here the final private event
 * that BulkCivilCaseSummonsNotificationService reacts to (progression.event.summons-data-prepared)
 * is sent directly, which is an established pattern in this test suite for isolating a single
 * event processor from a long, unrelated upstream chain.
 */
public class BulkCivilCaseSummonsApplicationIT extends AbstractIT {

    private static final String PROGRESSION_EVENT_SUMMONS_DATA_PREPARED = "progression.event.summons-data-prepared";
    // matches the hardcoded subject id in applications/progression.initiate-court-proceedings-for-bulk-civil-group-application.json
    private static final UUID SUBJECT_ID = UUID.fromString("c2222222-2222-2222-2222-222222222222");
    private static final String COURT_ROOM_ID = "9e4932f7-97b2-3010-b942-ddd2624e4dd8";

    private final JmsMessageProducerClient privateMessageProducer = newPrivateJmsMessageProducerClientProvider(CONTEXT_NAME).getMessageProducerClient();
    private final ObjectToJsonObjectConverter objectToJsonObjectConverter = new ObjectToJsonObjectConverter(new ObjectMapperProducer().objectMapper());

    @Test
    public void shouldSuppressSummonsAndSendBulkNotificationForBulkCivilCaseApplication() throws IOException, JSONException {
        final UUID caseId = randomUUID();
        final String groupId = randomUUID().toString();
        final String applicationId = randomUUID().toString();
        final String prosecutorEmailAddress = randomAlphanumeric(20) + "@random.com";
        final String listedStartDateTime = "2026-09-10T09:00:00.000Z";

        // Given a bulk civil case request (initiation code S, summons code A) was received via
        // CPCI, as the (sole) group master case for its group. Reuses the proven group-cases
        // fixture/helper, whose master-case template is civil and carries a fixed case
        // reference ("CIVILURN") and lead defendant ("Harry Jack Kane Junior, TFL12345-ABC").
        initiateCourtProceedingsForGroupCases(caseId, Map.of(caseId, new Pair<>(randomUUID(), randomUUID())),
                listedStartDateTime, listedStartDateTime, groupId, ENGLISH_COURT_ID, "Lavender Hill Magistrates' Court");
        pollProsecutionCasesProgressionFor(caseId.toString());

        // Given a corresponding Application for first hearing summons is created on CP for this
        // bulk case request.
        initiateCourtProceedingsForCourtApplication(applicationId, caseId.toString(),
                "applications/progression.initiate-court-proceedings-for-bulk-civil-group-application.json");

        // When this application is resulted with SA results and hearing is shared - injected
        // directly as the private event the real event chain ultimately produces, to isolate
        // BulkCivilCaseSummonsNotificationService from that unrelated upstream chain.
        sendSummonsDataPreparedForApplication(applicationId, prosecutorEmailAddress);

        // Then this must NOT generate any Summons, but the prosecutor must receive the
        // bulk-summons-approved email notification, without any attachment, using the lead
        // defendant (Harry Jack Kane Junior, TFL12345-ABC - from the group master case fixture).
        verifyEmailNotificationIsRaisedWithoutAttachment(List.of(prosecutorEmailAddress, "CIVILURN", "Harry Jack Kane Junior, TFL12345-ABC"));
        verifyNoLetterNotificationIsRaisedWithContent("CIVILURN");
    }

    private void sendSummonsDataPreparedForApplication(final String applicationId, final String prosecutorEmailAddress) {
        final SummonsApprovedOutcome summonsApprovedOutcome = summonsApprovedOutcome()
                .withProsecutorCost("£300.00")
                .withPersonalService(true)
                .withSummonsSuppressed(false)
                .withProsecutorEmailAddress(prosecutorEmailAddress)
                .build();

        final CourtCentre courtCentre = courtCentre()
                .withId(UUID.fromString(ENGLISH_COURT_ID))
                .withName("Croydon Magistrate's Court")
                .withRoomId(UUID.fromString(COURT_ROOM_ID))
                .build();

        final SummonsData summonsData = summonsData()
                .withConfirmedApplicationIds(singletonList(UUID.fromString(applicationId)))
                .withCourtApplicationPartyListingNeeds(singletonList(courtApplicationPartyListingNeeds()
                        .withCourtApplicationId(UUID.fromString(applicationId))
                        .withCourtApplicationPartyId(SUBJECT_ID)
                        .withSummonsRequired(APPLICATION)
                        .withSummonsApprovedOutcome(summonsApprovedOutcome)
                        .build()))
                .withCourtCentre(courtCentre)
                .withHearingDateTime(ZonedDateTime.now().plusWeeks(2))
                .build();

        final SummonsDataPrepared summonsDataPrepared = summonsDataPrepared().withSummonsData(summonsData).build();
        final JsonObject payload = objectToJsonObjectConverter.convert(summonsDataPrepared);

        final JsonEnvelope envelope = envelopeFrom(buildMetadata(PROGRESSION_EVENT_SUMMONS_DATA_PREPARED, randomUUID()), payload);
        privateMessageProducer.sendMessage(PROGRESSION_EVENT_SUMMONS_DATA_PREPARED, envelope);
    }
}
