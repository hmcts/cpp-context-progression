package uk.gov.moj.cpp.progression.processor;

import static java.util.Optional.ofNullable;
import static java.util.UUID.fromString;
import static uk.gov.justice.services.messaging.JsonObjects.createObjectBuilder;
import static uk.gov.justice.services.core.annotation.Component.EVENT_PROCESSOR;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.justice.services.messaging.JsonEnvelope.metadataFrom;

import uk.gov.justice.core.courts.ListingNumberUpdated;
import uk.gov.justice.core.courts.ProsecutionCaseListingNumberIncreased;
import uk.gov.justice.services.common.converter.ObjectToJsonObjectConverter;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.progression.service.ProgressionService;

import java.util.Collection;
import java.util.UUID;
import java.util.stream.Stream;

import javax.inject.Inject;
import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;


@ServiceComponent(EVENT_PROCESSOR)
public class HearingListingNumberUpdatedEventProcessor {

    private static final String OFFENCE_LISTING_NUMBERS = "offenceListingNumbers";
    @Inject
    private ObjectToJsonObjectConverter objectToJsonObjectConverter;

    @Inject
    @ServiceComponent(EVENT_PROCESSOR)
    private Sender sender;

    @Inject
    private ProgressionService progressionService;

    @Handles("progression.event.listing-number-updated")
    public void handleListingNumberUpdatedEvent(final Envelope<ListingNumberUpdated> event) {

        final ListingNumberUpdated listingNumberUpdated = event.payload();
        final JsonObject jsonPayload= objectToJsonObjectConverter.convert(listingNumberUpdated);

        ofNullable(listingNumberUpdated.getProsecutionCaseIds()).map(Collection::stream).orElseGet(Stream::empty)
                .forEach(caseId -> callProsecutionCommandHandler(event, caseId, jsonPayload.getJsonArray(OFFENCE_LISTING_NUMBERS)));

    }

    @Handles("progression.event.prosecution-case-listing-number-increased")
    public void processProsecutionCaseListingNumberIncreased(final Envelope<ProsecutionCaseListingNumberIncreased> event) {
        final ProsecutionCaseListingNumberIncreased prosecutionCaseListingNumberIncreased = event.payload();
        final JsonObject jsonPayload= objectToJsonObjectConverter.convert(prosecutionCaseListingNumberIncreased);

        if (jsonPayload.containsKey(OFFENCE_LISTING_NUMBERS) && !jsonPayload.getJsonArray(OFFENCE_LISTING_NUMBERS).isEmpty()) {
            callHearingCommandHandler(event, prosecutionCaseListingNumberIncreased.getProsecutionCaseId(), prosecutionCaseListingNumberIncreased.getHearingId(), jsonPayload.getJsonArray(OFFENCE_LISTING_NUMBERS));
        }

    }

    @Handles("progression.event.hearing-listing-number-updated")
    public void handleHaeringListingNumberUpdatedEvent(final JsonEnvelope event){
        progressionService.populateHearingToProbationCaseworker(event, fromString(event.payloadAsJsonObject().getString("hearingId")));
    }

    private void callProsecutionCommandHandler(final Envelope<ListingNumberUpdated> event, final UUID caseId, final JsonArray offenceListingNumbers) {

        final JsonObjectBuilder updateCommandBuilder = createObjectBuilder()
                .add("prosecutionCaseId", caseId.toString())
                .add(OFFENCE_LISTING_NUMBERS, offenceListingNumbers);

        sender.send(envelopeFrom(metadataFrom(event.metadata()).withName("progression.command.update-listing-number-to-prosecution-case"),
                updateCommandBuilder.build()));
    }

    private void callHearingCommandHandler(final Envelope<ProsecutionCaseListingNumberIncreased> event, final UUID caseId,  final UUID hearingId, final JsonArray offenceListingNumbers) {

        final JsonObjectBuilder updateCommandBuilder = createObjectBuilder()
                .add("prosecutionCaseId", caseId.toString())
                .add("hearingId", hearingId.toString())
                .add(OFFENCE_LISTING_NUMBERS, offenceListingNumbers);

        sender.send(envelopeFrom(metadataFrom(event.metadata()).withName("progression.command.update-listing-number-to-hearing"),
                updateCommandBuilder.build()));
    }

}

