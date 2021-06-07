package uk.gov.moj.cpp.progression.processor;

import static javax.json.Json.createObjectBuilder;
import static uk.gov.justice.services.core.annotation.Component.EVENT_PROCESSOR;

import uk.gov.justice.core.courts.Defendant;
import uk.gov.justice.core.courts.DefendantUpdate;
import uk.gov.justice.core.courts.ProsecutionCaseUpdateDefendantsWithMatchedRequestedV2;
import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.common.converter.ObjectToJsonObjectConverter;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.progression.service.DefendantUpdateDifferenceService;

import java.util.List;
import java.util.UUID;

import javax.inject.Inject;
import javax.json.JsonObject;

@ServiceComponent(EVENT_PROCESSOR)
public class UpdateDefendantsWithMatchedRequestedProcessor {

    @Inject
    private JsonObjectToObjectConverter jsonObjectToObjectConverter;

    @Inject
    private ObjectToJsonObjectConverter objectToJsonObjectConverter;

    @Inject
    private DefendantUpdateDifferenceService defendantUpdateDifferenceService;

    @Inject
    @ServiceComponent(EVENT_PROCESSOR)
    private Sender sender;

    @Inject
    private Enveloper enveloper;

    @Handles("progression.event.prosecution-case-update-defendants-with-matched-requested-v2")
    public void handleUpdateDefendantWithMatchedRequestedEvent(final JsonEnvelope envelope) {
        final ProsecutionCaseUpdateDefendantsWithMatchedRequestedV2 defendantsWithMatchedRequested = jsonObjectToObjectConverter.convert(envelope.payloadAsJsonObject(), ProsecutionCaseUpdateDefendantsWithMatchedRequestedV2.class);
        final Defendant originalDefendantPreviousVersion = defendantsWithMatchedRequested.getDefendant();
        final List<Defendant> matchedDefendants = defendantsWithMatchedRequested.getMatchedDefendants();
        final DefendantUpdate originalDefendantNextVersion = defendantsWithMatchedRequested.getDefendantUpdate();
        final UUID prosecutionCaseId = originalDefendantNextVersion.getProsecutionCaseId();
        final UUID defendantId = originalDefendantNextVersion.getId();

        sendDefendantUpdate(envelope, originalDefendantNextVersion, prosecutionCaseId, defendantId);

        for (final Defendant matchedDefendant : matchedDefendants) {
            final DefendantUpdate calculatedDefendantUpdate = defendantUpdateDifferenceService.calculateDefendantUpdate(
                    originalDefendantPreviousVersion,
                    originalDefendantNextVersion,
                    matchedDefendant
            );

            sendDefendantUpdate(envelope, calculatedDefendantUpdate, matchedDefendant.getProsecutionCaseId(), matchedDefendant.getId());
        }
    }

    public void sendDefendantUpdate(final JsonEnvelope envelope, final DefendantUpdate defendantUpdate, final UUID prosecutionCaseId, final UUID defendantId) {
        final JsonObject updateDefendantPayload = createObjectBuilder()
                .add("defendant", objectToJsonObjectConverter.convert(defendantUpdate))
                .add("id", defendantId.toString())
                .add("prosecutionCaseId", prosecutionCaseId.toString())
                .build();
        sender.send(enveloper.withMetadataFrom(envelope, "progression.command.update-defendant-for-prosecution-case").apply(updateDefendantPayload));
    }

}
