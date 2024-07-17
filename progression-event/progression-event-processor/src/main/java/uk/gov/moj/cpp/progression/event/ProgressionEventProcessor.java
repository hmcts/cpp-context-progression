package uk.gov.moj.cpp.progression.event;

import static java.util.Arrays.asList;
import static java.util.Objects.isNull;
import static javax.json.Json.createObjectBuilder;
import static uk.gov.justice.services.core.annotation.Component.EVENT_PROCESSOR;

import uk.gov.justice.core.courts.CourtCentre;
import uk.gov.justice.core.courts.DefendantListingNeeds;
import uk.gov.justice.core.courts.HearingListingNeeds;
import uk.gov.justice.core.courts.HearingType;
import uk.gov.justice.core.courts.JurisdictionType;
import uk.gov.justice.core.courts.ListCourtHearing;
import uk.gov.justice.core.courts.ProsecutionCase;
import uk.gov.justice.core.courts.ProsecutionCaseCreated;
import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.common.converter.ObjectToJsonObjectConverter;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.progression.command.AddRelatedReference;
import uk.gov.moj.cpp.progression.domain.event.completedsendingsheet.SendingSheetCompleted;
import uk.gov.moj.cpp.progression.service.ListingService;
import uk.gov.moj.cpp.progression.service.ProgressionService;
import uk.gov.moj.cpp.progression.transformer.SendingSheetCompleteTransformer;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.json.JsonObject;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


@SuppressWarnings({"WeakerAccess", "squid:S3655", "squid:S3457", "squid:CallToDeprecatedMethod", "squid:S1612"})
@ServiceComponent(EVENT_PROCESSOR)
public class ProgressionEventProcessor {

    public static final String CASE_ID = "caseId";
    public static final String COURT_CENTRE_ID = "courtCentreId";
    public static final String GUILTY = "GUILTY";
    private static final Logger LOGGER = LoggerFactory.getLogger(ProgressionEventProcessor.class.getCanonicalName());
    private static final String PUBLIC_PROGRESSION_EVENTS_SENTENCE_HEARING_DATE_ADDED = "public.progression.events.sentence-hearing-date-added";
    private static final String PUBLIC_PROGRESSION_EVENTS_CASE_ADDED_TO_CROWN_COURT = "public.progression.events.case-added-to-crown-court";
    private static final String PUBLIC_PROGRESSION_EVENTS_CASE_ADDED_TO_CROWN_COURT_EXISTS = "public.progression.events.case-already-exists-in-crown-court";
    private static final String PUBLIC_PROGRESSION_EVENTS_SENDING_SHEET_COMPLETED = "public.progression.events.sending-sheet-completed";
    private static final String PUBLIC_PROGRESSION_EVENTS_SENDING_SHEET_PREVIOUSLY_COMPLETED = "public.progression.events.sending-sheet-previously-completed";
    private static final String PUBLIC_PROGRESSION_EVENTS_SENDING_SHEET_INVALIDATED = "public.progression.events.sending-sheet-invalidated";
    private static final String PROGRESSION_COMMAND_CREATE_PROSECUTION_CASE = "progression.command.create-prosecution-case";
    private static final String PUBLIC_PROGRESSION_EVENTS_PROSECUTION_CASE_CREATED = "public.progression.prosecution-case-created";
    private static final String PUBLIC_PROGRESSION_EVENTS_NOW_NOTIFICATION_SUPPRESSED = "public.progression.now-notification-suppressed";
    @Inject
    private Enveloper enveloper;

    @Inject
    private Sender sender;


    @Inject
    private JsonObjectToObjectConverter jsonObjectToObjectConverter;

    @Inject
    private ObjectToJsonObjectConverter objectToJsonObjectConverter;

    @Inject
    private ListingService listingService;

    @Inject
    private SendingSheetCompleteTransformer sendingSheetCompleteTransformer;

    @Inject
    private ProgressionService progressionService;

    @Handles("progression.events.sentence-hearing-date-added")
    public void publishSentenceHearingAddedPublicEvent(final JsonEnvelope event) {
        final String caseId = event.payloadAsJsonObject().getString(CASE_ID);
        final JsonObject payload = createObjectBuilder().add(CASE_ID, caseId).build();
        sender.send(enveloper.withMetadataFrom(event, PUBLIC_PROGRESSION_EVENTS_SENTENCE_HEARING_DATE_ADDED).apply(payload));
    }

    @Handles("progression.events.case-added-to-crown-court")
    public void publishCaseAddedToCrownCourtPublicEvent(final JsonEnvelope event) {
        final String caseId = event.payloadAsJsonObject().getString(CASE_ID);
        LOGGER.debug("Raising public event for case added to crown court for caseId: {}", caseId);
        final JsonObject payload = createObjectBuilder().add(CASE_ID, caseId).
                add(COURT_CENTRE_ID, event.payloadAsJsonObject().getString(COURT_CENTRE_ID)).build();
        sender.send(enveloper.withMetadataFrom(event, PUBLIC_PROGRESSION_EVENTS_CASE_ADDED_TO_CROWN_COURT).apply(payload));
    }

    @Handles("progression.events.case-already-exists-in-crown-court")
    public void publishCaseAlreadyExistsInCrownCourtEvent(final JsonEnvelope event) {
        final String caseId = event.payloadAsJsonObject().getString(CASE_ID);
        final JsonObject payload = createObjectBuilder().add(CASE_ID, caseId).build();
        sender.send(enveloper.withMetadataFrom(event, PUBLIC_PROGRESSION_EVENTS_CASE_ADDED_TO_CROWN_COURT_EXISTS).apply(payload));
    }


    @Handles("progression.events.sending-sheet-completed")
    public void publishSendingSheetCompletedEvent(final JsonEnvelope event) {
        final SendingSheetCompleted sendingSheetCompleted = jsonObjectToObjectConverter.convert(event.payloadAsJsonObject(), SendingSheetCompleted.class);
        final ProsecutionCase prosecutionCase = sendingSheetCompleteTransformer.transformToProsecutionCase(sendingSheetCompleted, event);
        final JsonObject pCasePayload = createObjectBuilder().add("prosecutionCase", objectToJsonObjectConverter.convert(prosecutionCase)).build();
        LOGGER.info("prosecution case is being created '{}' ", pCasePayload);

        final List<DefendantListingNeeds> listDefendantListingNeeds = prosecutionCase.getDefendants().stream().map(defendant -> DefendantListingNeeds.defendantListingNeeds()
                .withDefendantId(defendant.getId()).withProsecutionCaseId(prosecutionCase.getId()).build()).collect(Collectors.toList());


        final boolean allGuiltyPlea = prosecutionCase.getDefendants().stream().flatMap(defendant -> defendant.getOffences().stream())
                .map(offence -> offence.getPlea())
                .allMatch(plea -> Objects.nonNull(plea) && plea.getPleaValue().equals(GUILTY));

        HearingType hearingType = HearingType.hearingType()
                .withId(UUID.fromString("06b0c2bf-3f98-46ed-ab7e-56efaf9ecced"))
                .withDescription("Plea & Trial Preparation")
                .build();

        Integer estimatedMinutes = 20;

        if (allGuiltyPlea) {
            hearingType = HearingType.hearingType()
                    .withId(UUID.fromString("5ae4c090-0f70-4694-b4fc-707633d2b430"))
                    .withDescription("Sentence")
                    .build();
            estimatedMinutes = 30;
        }

        final ListCourtHearing listCourtHearing = ListCourtHearing.listCourtHearing()
                .withHearings(asList(HearingListingNeeds.hearingListingNeeds()
                        .withCourtCentre(CourtCentre.courtCentre()
                                .withId(sendingSheetCompleted.getCrownCourtHearing().getCourtCentreId())
                                .withName(sendingSheetCompleted.getCrownCourtHearing().getCourtCentreName())
                                .build())
                        .withProsecutionCases(asList(prosecutionCase))
                        .withEarliestStartDateTime(LocalDate.parse(sendingSheetCompleted.getCrownCourtHearing().getCcHearingDate()).atStartOfDay(ZoneId.systemDefault()))
                        .withId(UUID.randomUUID())
                        .withJurisdictionType(JurisdictionType.CROWN)
                        .withEstimatedMinutes(estimatedMinutes)
                        .withDefendantListingNeeds(listDefendantListingNeeds)
                        .withType(hearingType)
                        .build()))
                .build();


        listingService.listCourtHearing(event, listCourtHearing);

        sender.send(enveloper.withMetadataFrom(event, PROGRESSION_COMMAND_CREATE_PROSECUTION_CASE).apply(pCasePayload));
        sender.send(enveloper.withMetadataFrom(event, PUBLIC_PROGRESSION_EVENTS_SENDING_SHEET_COMPLETED)
                .apply(event.payloadAsJsonObject()));
    }

    @Handles("progression.events.sending-sheet-previously-completed")
    public void publishSendingSheetPreviouslyCompletedEvent(final JsonEnvelope event) {
        final String caseId = event.payloadAsJsonObject().getString(CASE_ID);
        final JsonObject payload = createObjectBuilder().add(CASE_ID, caseId).build();
        sender.send(enveloper.withMetadataFrom(event, PUBLIC_PROGRESSION_EVENTS_SENDING_SHEET_PREVIOUSLY_COMPLETED).apply(payload));
    }

    @Handles("progression.events.sending-sheet-invalidated")
    public void publishSendingSheetInvalidatedEvent(final JsonEnvelope event) {
        final String caseId = event.payloadAsJsonObject().getString(CASE_ID);
        final JsonObject payload = createObjectBuilder().add(CASE_ID, caseId).build();
        sender.send(enveloper.withMetadataFrom(event, PUBLIC_PROGRESSION_EVENTS_SENDING_SHEET_INVALIDATED).apply(payload));
    }

    @Handles("progression.event.prosecution-case-created")
    public void publishProsecutionCaseCreatedEvent(final JsonEnvelope event) {
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Received 'progression.event.prosecution-case-created:' event with payload: {}", event.toObfuscatedDebugString());
        }

        final ProsecutionCaseCreated prosecutionCaseCreated = jsonObjectToObjectConverter.convert(event.payloadAsJsonObject(), ProsecutionCaseCreated.class);
        final ProsecutionCase prosecutionCase = prosecutionCaseCreated.getProsecutionCase();

        if (isNull(prosecutionCase.getGroupId())) {
            LOGGER.info("Raising public message public.progression.prosecution-case-created for Case '{}'  ", prosecutionCase.getId());
            sender.send(Enveloper.envelop(event.payload())
                    .withName(PUBLIC_PROGRESSION_EVENTS_PROSECUTION_CASE_CREATED)
                    .withMetadataFrom(event));
        }

        final String relatedUrn = prosecutionCase.getRelatedUrn();
        if (StringUtils.isNotBlank(relatedUrn)) {
            final String caseId = prosecutionCase.getId().toString();
            LOGGER.info("fire command to add the related reference urn");
            final AddRelatedReference addRelatedReference = AddRelatedReference
                    .addRelatedReference()
                    .withRelatedReference(relatedUrn)
                    .withProsecutionCaseId(UUID.fromString(caseId))
                    .build();
            final JsonObject jsonObject = objectToJsonObjectConverter.convert(addRelatedReference);
            sender.send(Enveloper.envelop(jsonObject)
                    .withName("progression.command.add-related-reference")
                    .withMetadataFrom(event));
        }

        if (isNull(prosecutionCase.getIsGroupMember()) || !prosecutionCase.getIsGroupMember()) {
            final JsonObject jsonObject = createObjectBuilder()
                    .add("prosecutionCaseId", prosecutionCase.getId().toString())
                    .build();

            sender.send(Enveloper.envelop(jsonObject)
                    .withName("progression.command.process-matched-defendants")
                    .withMetadataFrom(event));
        }
    }

    @Handles("progression.event.now-document-notification-suppressed")
    public void publishSuppressNowDocumentNotificationEvent(final JsonEnvelope event) {
        sender.send(Enveloper.envelop(event.payloadAsJsonObject())
                .withName(PUBLIC_PROGRESSION_EVENTS_NOW_NOTIFICATION_SUPPRESSED)
                .withMetadataFrom(event));
    }

}


