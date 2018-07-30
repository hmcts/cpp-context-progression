package uk.gov.moj.cpp.progression.event;

import static uk.gov.justice.services.core.annotation.Component.EVENT_PROCESSOR;

import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.external.domain.listing.ListingCase;
import uk.gov.moj.cpp.progression.activiti.common.ProcessMapConstant;
import uk.gov.moj.cpp.progression.domain.event.completedsendingsheet.Plea;
import uk.gov.moj.cpp.progression.domain.event.completedsendingsheet.SendingSheetCompleted;
import uk.gov.moj.cpp.progression.activiti.workflow.listhearing.converter.SendingSheetCompleteToListingCaseConverter;
import uk.gov.moj.cpp.progression.activiti.workflow.listhearing.ListHearingService;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.json.Json;
import javax.json.JsonObject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


@SuppressWarnings({"WeakerAccess", "squid:S3655"})
@ServiceComponent(EVENT_PROCESSOR)
public class ProgressionEventProcessor {

    private static final Logger LOGGER = LoggerFactory.getLogger(ProgressionEventProcessor.class.getCanonicalName());
    private static final String PUBLIC_PROGRESSION_EVENTS_SENTENCE_HEARING_DATE_ADDED = "public.progression.events.sentence-hearing-date-added";
    private static final String PUBLIC_PROGRESSION_EVENTS_CASE_ADDED_TO_CROWN_COURT = "public.progression.events.case-added-to-crown-court";
    private static final String PUBLIC_PROGRESSION_EVENTS_CASE_ADDED_TO_CROWN_COURT_EXISTS = "public.progression.events.case-already-exists-in-crown-court";
    private static final String PUBLIC_PROGRESSION_EVENTS_SENDING_SHEET_COMPLETED = "public.progression.events.sending-sheet-completed";
    private static final String PUBLIC_PROGRESSION_EVENTS_SENDING_SHEET_PREVIOUSLY_COMPLETED = "public.progression.events.sending-sheet-previously-completed";
    private static final String PUBLIC_PROGRESSION_EVENTS_SENDING_SHEET_INVALIDATED = "public.progression.events.sending-sheet-invalidated";
    private static final String SENDING_SHEET_COMPLETE = "Sending Sheet Complete";
    public static final String CASE_ID = "caseId";
    public static final String COURT_CENTRE_ID = "courtCentreId";
    public static final String STATUS = "status";


    @Inject
    private Enveloper enveloper;

    @Inject
    private Sender sender;

    @Inject
    private ListHearingService listHearingService;

    @Inject
    private JsonObjectToObjectConverter jsonObjectToObjectConverter;

    @Inject
    private SendingSheetCompleteToListingCaseConverter sendingSheetCompleteToListingCaseConverter;

    @Handles("progression.events.sentence-hearing-date-added")
    public void publishSentenceHearingAddedPublicEvent(final JsonEnvelope event) {
        final String caseId = event.payloadAsJsonObject().getString(CASE_ID);
        final JsonObject payload = Json.createObjectBuilder().add(CASE_ID, caseId).build();
        this.sender.send(this.enveloper.withMetadataFrom(event, PUBLIC_PROGRESSION_EVENTS_SENTENCE_HEARING_DATE_ADDED).apply(payload));
    }

    @Handles("progression.events.case-added-to-crown-court")
    public void publishCaseAddedToCrownCourtPublicEvent(final JsonEnvelope event) {
        final String caseId = event.payloadAsJsonObject().getString(CASE_ID);
        LOGGER.debug("Raising public event for case added to crown court for caseId: ", caseId);
        final JsonObject payload = Json.createObjectBuilder().add(CASE_ID, caseId).
                add(COURT_CENTRE_ID, event.payloadAsJsonObject().getString(COURT_CENTRE_ID)).build();
        this.sender.send(this.enveloper.withMetadataFrom(event, PUBLIC_PROGRESSION_EVENTS_CASE_ADDED_TO_CROWN_COURT).apply(payload));
    }

    @Handles("progression.events.case-already-exists-in-crown-court")
    public void publishCaseAlreadyExistsInCrownCourtEvent(final JsonEnvelope event) {
        final String caseId = event.payloadAsJsonObject().getString(CASE_ID);
        final JsonObject payload = Json.createObjectBuilder().add(CASE_ID, caseId).build();
        this.sender.send(this.enveloper.withMetadataFrom(event, PUBLIC_PROGRESSION_EVENTS_CASE_ADDED_TO_CROWN_COURT_EXISTS).apply(payload));
    }


    @Handles("progression.events.sending-sheet-completed")
    public void publishSendingSheetCompletedEvent(final JsonEnvelope event) {
        this.sender.send(this.enveloper.withMetadataFrom(event, PUBLIC_PROGRESSION_EVENTS_SENDING_SHEET_COMPLETED)
                .apply(event.payloadAsJsonObject()));
        startListHearing(event);
    }

    @Handles("progression.events.sending-sheet-previously-completed")
    public void publishSendingSheetPreviouslyCompletedEvent(final JsonEnvelope event) {
        final String caseId = event.payloadAsJsonObject().getString(CASE_ID);
        final JsonObject payload = Json.createObjectBuilder().add(CASE_ID, caseId).build();
        this.sender.send(this.enveloper.withMetadataFrom(event, PUBLIC_PROGRESSION_EVENTS_SENDING_SHEET_PREVIOUSLY_COMPLETED).apply(payload));
    }

    @Handles("progression.events.sending-sheet-invalidated")
    public void publishSendingSheetInvalidatedEvent(final JsonEnvelope event) {
        final String caseId = event.payloadAsJsonObject().getString(CASE_ID);
        final JsonObject payload = Json.createObjectBuilder().add(CASE_ID, caseId).build();
        this.sender.send(this.enveloper.withMetadataFrom(event, PUBLIC_PROGRESSION_EVENTS_SENDING_SHEET_INVALIDATED).apply(payload));
    }


    private void startListHearing(final JsonEnvelope jsonEnvelope){
        LOGGER.debug("Sending sheet completed to case progression .");

        final SendingSheetCompleted sendingSheetCompleted = jsonObjectToObjectConverter.convert(jsonEnvelope.payloadAsJsonObject(), SendingSheetCompleted.class);
        final ListingCase listingCase = sendingSheetCompleteToListingCaseConverter.convert(sendingSheetCompleted);

        final Map<String, Object> processMap = new HashMap<>();
        processMap.put(ProcessMapConstant.CASE_ID, listingCase.getCaseId());
        processMap.put(ProcessMapConstant.USER_ID, jsonEnvelope.metadata().userId().get());
        processMap.put(ProcessMapConstant.HEARING_ID, UUID.randomUUID().toString());
        processMap.put(ProcessMapConstant.WHEN, SENDING_SHEET_COMPLETE);
        processMap.put(ProcessMapConstant.SEND_CASE_FOR_LISTING_PAYLOAD, listingCase);
        final List<Plea> pleasObj = sendingSheetCompleted.getHearing().getDefendants().stream()
                .map(d -> d.getOffences().stream().map(o -> o.getPlea()).filter(Objects::nonNull).collect(Collectors.toList()))
                .flatMap(Collection::stream)
                .collect(Collectors.toList());
        final List<String>  pleas = pleasObj.stream().map(p -> p.getValue()).collect(Collectors.toList());
        pleas.replaceAll(String::toUpperCase);
        processMap.put(ProcessMapConstant.PLEA, pleas);

        listHearingService.startProcess(processMap);
    }



}


