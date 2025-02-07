package uk.gov.moj.cpp.progression.processor;

import static uk.gov.justice.services.core.annotation.Component.EVENT_PROCESSOR;
import static uk.gov.justice.services.messaging.Envelope.metadataFrom;


import java.util.Collections;
import javax.inject.Inject;
import javax.json.Json;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.gov.justice.core.courts.AddedDefendantsMovedToHearing;
import uk.gov.justice.core.courts.AddedOffencesMovedToHearing;
import uk.gov.justice.core.courts.DefendantsAddedToCourtProceedings;
import uk.gov.justice.core.courts.Hearing;
import uk.gov.justice.core.courts.ListDefendantRequest;
import uk.gov.justice.core.courts.ListHearingRequest;
import uk.gov.justice.core.courts.Offence;
import uk.gov.justice.core.courts.OffencesMovedToNewNextHearing;
import uk.gov.justice.listing.events.OffencesMovedToNextHearing;
import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.common.converter.ObjectToJsonObjectConverter;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.progression.service.ProgressionService;

@ServiceComponent(EVENT_PROCESSOR)
public class NextHearingEventProcessor {
    private static final Logger LOGGER = LoggerFactory.getLogger(NextHearingEventProcessor.class);

    @Inject
    @ServiceComponent(EVENT_PROCESSOR)
    private Sender sender;

    @Inject
    private Enveloper enveloper;


    @Inject
    private ProgressionService progressionService;

    @Inject
    private JsonObjectToObjectConverter jsonObjectToObjectConverter;

    @Inject
    private ObjectToJsonObjectConverter objectToJsonObjectConverter;


    @Handles("public.listing.offences-moved-to-next-hearing")
    public void processPublicEvent(final Envelope<OffencesMovedToNextHearing> event) {
        OffencesMovedToNextHearing offencesMovedToNextHearing = event.payload();

        offencesMovedToNextHearing.getOldHearingIds().forEach(oldHearingId ->
                this.sender.send(Envelope.envelopeFrom(metadataFrom(event.metadata())
                        .withName("progression.command.move-offences-from-old-next-hearing")
                        .build(), Json.createObjectBuilder()
                        .add("oldHearingId", oldHearingId.toString())
                        .add("newHearingId", offencesMovedToNextHearing.getNewHearingId().toString())
                        .add("seedingHearingId", offencesMovedToNextHearing.getSeedingHearingId().toString())
                        .build()))
        );
    }

    @Handles("progression.event.offences-moved-to-new-next-hearing")
    public void processOffencesMovedToNewNextHearing(final Envelope<OffencesMovedToNewNextHearing> event){
        this.sender.send(Envelope.envelopeFrom(metadataFrom(event.metadata())
                .withName("progression.command.move-offences-to-new-next-hearing"), event.payload()));

    }

    @Handles("progression.event.added-offences-moved-to-hearing")
    public void processAddedOffencesMovedToHearing(final Envelope<AddedOffencesMovedToHearing> event){
        if(event.payload().getIsHearingInitiateEnriched()) {
            this.sender.send(Envelope.envelopeFrom(metadataFrom(event.metadata())
                    .withName("progression.command.update-offences-for-hearing"), AddedOffencesMovedToHearing.addedOffencesMovedToHearing()
                            .withValuesFrom(event.payload())
                            .withIsHearingInitiateEnriched(null)
                    .build()));
        }
    }

    @Handles("progression.event.added-defendants-moved-to-hearing")
    public void processAddedDefendantsMovedToHearing(final JsonEnvelope event){
        LOGGER.info("progression.event.added-defendants-moved-to-hearing");
        final AddedDefendantsMovedToHearing addedDefendantsMovedToHearing = jsonObjectToObjectConverter.convert(event.payloadAsJsonObject(), AddedDefendantsMovedToHearing.class);

        this.sender.send(Envelope.envelopeFrom(metadataFrom(event.metadata())
                .withName("progression.command.update-hearing-with-new-defendant"), event.payload()));

        final Hearing hearing = progressionService.retrieveHearing(event, addedDefendantsMovedToHearing.getHearingId() );

        final DefendantsAddedToCourtProceedings defendantsAddedToCourtProceedings = DefendantsAddedToCourtProceedings.defendantsAddedToCourtProceedings()
                .withDefendants(addedDefendantsMovedToHearing.getDefendants())
                .withListHearingRequests(Collections.singletonList(ListHearingRequest.listHearingRequest()
                        .withHearingType(hearing.getType())
                        .withCourtCentre(hearing.getCourtCentre())
                        .withJurisdictionType(hearing.getJurisdictionType())
                        .withListDefendantRequests(addedDefendantsMovedToHearing.getDefendants().stream()
                                .map(def -> ListDefendantRequest.listDefendantRequest()
                                        .withDefendantId(def.getId())
                                        .withProsecutionCaseId(addedDefendantsMovedToHearing.getProsecutionCaseId())
                                        .withDefendantOffences(def.getOffences().stream().map(Offence::getId).toList())
                                        .build())
                                .toList())
                        .build()))
                .build();

        LOGGER.info("public.progression.defendants-added-to-court-proceedings");
        this.sender.send(Envelope.envelopeFrom(metadataFrom(event.metadata())
                .withName("public.progression.defendants-added-to-court-proceedings"), objectToJsonObjectConverter.convert(defendantsAddedToCourtProceedings)));
        this.sender.send(Envelope.envelopeFrom(metadataFrom(event.metadata())
                .withName("public.progression.defendants-added-to-case"), objectToJsonObjectConverter.convert(defendantsAddedToCourtProceedings)));
        this.sender.send(Envelope.envelopeFrom(metadataFrom(event.metadata())
                .withName("public.progression.defendants-added-to-hearing"), objectToJsonObjectConverter.convert(defendantsAddedToCourtProceedings)));
    }
}
