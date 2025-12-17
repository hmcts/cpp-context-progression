package uk.gov.moj.cpp.progression.processor;

import static uk.gov.justice.services.core.annotation.Component.EVENT_PROCESSOR;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.justice.services.messaging.JsonEnvelope.metadataFrom;


import javax.inject.Inject;
import javax.json.Json;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;
import uk.gov.justice.core.courts.HearingUpdatedForPartialAllocation;
import uk.gov.justice.core.courts.OffencesToRemove;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.Envelope;

@ServiceComponent(EVENT_PROCESSOR)
public class HearingUpdatedForPartialAllocationEventProcessor {

    @Inject
    @ServiceComponent(EVENT_PROCESSOR)
    private Sender sender;

    @Handles("progression.event.hearing-updated-for-partial-allocation")
    public void handle(final Envelope<HearingUpdatedForPartialAllocation> event){
        final HearingUpdatedForPartialAllocation hearingUpdatedForPartialAllocation = event.payload();

        final JsonArrayBuilder offenceIdsBuilder = Json.createArrayBuilder();

        hearingUpdatedForPartialAllocation.getProsecutionCasesToRemove().stream()
                .flatMap(prosecutionCasesToRemove -> prosecutionCasesToRemove.getDefendantsToRemove().stream())
                .flatMap(defendantsToRemove -> defendantsToRemove.getOffencesToRemove().stream())
                .map(OffencesToRemove::getOffenceId)
                .forEach(offenceId -> offenceIdsBuilder.add(offenceId.toString()));

        final JsonObject payload = Json.createObjectBuilder()
                .add("hearingId", hearingUpdatedForPartialAllocation.getHearingId().toString())
                .add("offenceIds", offenceIdsBuilder.build())
                .build();

        sender.send(envelopeFrom(metadataFrom(event.metadata()).withName("public.progression.offences-removed-from-existing-allocated-hearing"),
                payload));

    }
}
