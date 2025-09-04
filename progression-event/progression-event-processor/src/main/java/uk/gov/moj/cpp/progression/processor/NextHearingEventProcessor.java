package uk.gov.moj.cpp.progression.processor;

import static javax.json.Json.createObjectBuilder;
import static uk.gov.justice.services.core.annotation.Component.EVENT_PROCESSOR;
import static uk.gov.justice.services.messaging.Envelope.metadataFrom;


import javax.inject.Inject;
import javax.json.Json;
import javax.json.JsonObject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.gov.justice.core.courts.AddedOffencesMovedToHearing;
import uk.gov.justice.core.courts.Defendant;
import uk.gov.justice.core.courts.OffencesMovedToNewNextHearing;
import uk.gov.justice.core.courts.ProsecutionCase;
import uk.gov.justice.listing.courts.RelatedHearingUpdatedForAdhocHearing;
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

import java.util.Optional;
import java.util.stream.Stream;

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
                        .build(), createObjectBuilder()
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
            // add new offence to the hearing in progression context.
            this.sender.send(Envelope.envelopeFrom(metadataFrom(event.metadata())
                    .withName("progression.command.update-offences-for-hearing"), AddedOffencesMovedToHearing.addedOffencesMovedToHearing()
                            .withValuesFrom(event.payload())
                            .withCaseId(null)
                            .withIsHearingInitiateEnriched(null)
                    .build()));

            final JsonObject pcFromViewStore = progressionService.getProsecutionCaseDetailById(JsonEnvelope.envelopeFrom(event.metadata(), createObjectBuilder().build()), event.payload().getCaseId().toString()).orElseThrow();
            final ProsecutionCase orgProsecutionCase = jsonObjectToObjectConverter.convert(pcFromViewStore.getJsonObject("prosecutionCase"), ProsecutionCase.class);
            final ProsecutionCase prosecutionCase = ProsecutionCase.prosecutionCase()
                    .withValuesFrom(orgProsecutionCase)
                    .withDefendants(orgProsecutionCase.getDefendants().stream().filter( def -> def.getId().equals(event.payload().getDefendantId()))
                            .map(def -> Defendant.defendant().withValuesFrom(def)
                                    .withOffences(event.payload().getNewOffences())
                                    .build())
                            .toList())
                    .build();

            // inform other context to add new offence to the hearing
            this.sender.send(Envelope.envelopeFrom(metadataFrom(event.metadata())
                    .withName("public.progression.related-hearing-updated-for-adhoc-hearing"), RelatedHearingUpdatedForAdhocHearing.relatedHearingUpdatedForAdhocHearing()
                    .withHearingId(event.payload().getHearingId())
                    .withProsecutionCases(Stream.of(prosecutionCase).toList())
                    .build()));
        }
    }

}
