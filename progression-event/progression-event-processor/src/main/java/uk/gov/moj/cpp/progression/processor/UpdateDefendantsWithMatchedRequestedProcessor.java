package uk.gov.moj.cpp.progression.processor;

import static javax.json.Json.createObjectBuilder;
import static uk.gov.justice.services.core.annotation.Component.EVENT_PROCESSOR;

import uk.gov.justice.core.courts.Defendant;
import uk.gov.justice.core.courts.DefendantUpdate;
import uk.gov.justice.core.courts.Hearing;
import uk.gov.justice.core.courts.ProsecutionCase;
import uk.gov.justice.core.courts.ProsecutionCaseUpdateDefendantsWithMatchedRequested;
import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.common.converter.ObjectToJsonObjectConverter;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.progression.service.DefendantUpdateDifferenceService;
import uk.gov.moj.cpp.progression.service.ProgressionService;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.json.JsonObject;

@ServiceComponent(EVENT_PROCESSOR)
public class UpdateDefendantsWithMatchedRequestedProcessor {

    @Inject
    private JsonObjectToObjectConverter jsonObjectToObjectConverter;

    @Inject
    private ObjectToJsonObjectConverter objectToJsonObjectConverter;

    @Inject
    private ProgressionService progressionService;

    @Inject
    private DefendantUpdateDifferenceService defendantUpdateDifferenceService;

    @Inject
    @ServiceComponent(EVENT_PROCESSOR)
    private Sender sender;

    @Inject
    private Enveloper enveloper;

    @Handles("progression.event.prosecution-case-update-defendants-with-matched-requested")
    public void handleUpdateDefendantWithMatchedRequestedEvent(final JsonEnvelope envelope) {
        final ProsecutionCaseUpdateDefendantsWithMatchedRequested defendantsWithMatchedRequested = jsonObjectToObjectConverter.convert(envelope.payloadAsJsonObject(), ProsecutionCaseUpdateDefendantsWithMatchedRequested.class);

        final DefendantUpdate originalDefendantNextVersion = defendantsWithMatchedRequested.getDefendant();
        final UUID prosecutionCaseId = originalDefendantNextVersion.getProsecutionCaseId();
        final UUID defendantId = originalDefendantNextVersion.getId();
        final UUID matchedDefendantHearingId = defendantsWithMatchedRequested.getMatchedDefendantHearingId();

        final Defendant originalDefendantPreviousVersion = getDefendantFromProsecutionCase(envelope, prosecutionCaseId, defendantId);

        final List<Defendant> matchedDefendants = findMatchedDefendants(envelope, defendantId, matchedDefendantHearingId);

        sendDefendantUpdate(envelope, originalDefendantNextVersion, prosecutionCaseId, defendantId);

        for (final Defendant matchedDefendant : matchedDefendants) {
            final Defendant caseDefendant = getDefendantFromProsecutionCase(envelope, matchedDefendant.getProsecutionCaseId(), matchedDefendant.getId());

            final DefendantUpdate calculatedDefendantUpdate = defendantUpdateDifferenceService.calculateDefendantUpdate(
                    originalDefendantPreviousVersion,
                    originalDefendantNextVersion,
                    caseDefendant
            );
            sendDefendantUpdate(envelope, calculatedDefendantUpdate, caseDefendant.getProsecutionCaseId(), caseDefendant.getId());
        }
    }

    public Defendant getDefendantFromProsecutionCase(final JsonEnvelope envelope, final UUID prosecutionCaseId, final UUID defendantId) {
        final ProsecutionCase prosecutionCase = getProsecutionCase(envelope, prosecutionCaseId);
        return prosecutionCase.getDefendants().stream()
                .filter(defendant -> Objects.equals(defendant.getId(), defendantId))
                .findFirst().orElseThrow(() -> new RuntimeException("Defendant not found"));
    }

    public ProsecutionCase getProsecutionCase(final JsonEnvelope envelope, final UUID prosecutionCaseId) {
        final Optional<JsonObject> prosecutionCaseDetailOptional = progressionService.getProsecutionCaseDetailById(envelope, prosecutionCaseId.toString());
        final JsonObject prosecutionCaseJson = prosecutionCaseDetailOptional.orElseThrow(() -> new RuntimeException("Prosecution Case not found")).getJsonObject("prosecutionCase");
        return jsonObjectToObjectConverter.convert(prosecutionCaseJson, ProsecutionCase.class);
    }

    public void sendDefendantUpdate(final JsonEnvelope envelope, final DefendantUpdate defendantUpdate, final UUID prosecutionCaseId, final UUID defendantId) {
        final JsonObject updateDefendantPayload = createObjectBuilder()
                .add("defendant", objectToJsonObjectConverter.convert(defendantUpdate))
                .add("id", defendantId.toString())
                .add("prosecutionCaseId", prosecutionCaseId.toString())
                .build();
        sender.send(enveloper.withMetadataFrom(envelope, "progression.command.update-defendant-for-prosecution-case").apply(updateDefendantPayload));
    }


    public UUID findMasterDefendantId(final UUID defendantId, final Hearing hearing) {
        return hearing.getProsecutionCases().stream()
                .flatMap((ProsecutionCase prosecutionCase) -> prosecutionCase.getDefendants().stream())
                .filter(defendant -> Objects.equals(defendantId, defendant.getId()))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Cannot Find Defendant in Hearing Stream " + defendantId))
                .getMasterDefendantId();
    }

    public List<Defendant> findMatchedDefendants(final JsonEnvelope envelope, final UUID defendantId, final UUID matchedDefendantHearingId) {
        final Optional<JsonObject> hearingPayloadOptional = progressionService.getHearing(envelope, matchedDefendantHearingId.toString());
        final JsonObject hearingJson = hearingPayloadOptional.orElseThrow(() -> new RuntimeException("Hearing not found")).getJsonObject("hearing");
        final Hearing hearing = jsonObjectToObjectConverter.convert(hearingJson, Hearing.class);
        final UUID masterDefendantId = findMasterDefendantId(defendantId, hearing);
        return hearing.getProsecutionCases().stream()
                .flatMap((ProsecutionCase prosecutionCase) -> prosecutionCase.getDefendants().stream())
                .filter(defendant ->
                        Objects.equals(defendant.getMasterDefendantId(), masterDefendantId)
                                && !Objects.equals(defendant.getId(), defendantId)
                )
                .collect(Collectors.toList());
    }
}
