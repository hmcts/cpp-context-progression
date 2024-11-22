package uk.gov.moj.cpp.progression.processor;

import static javax.json.Json.createObjectBuilder;
import static uk.gov.justice.services.core.annotation.Component.EVENT_PROCESSOR;

import uk.gov.justice.core.courts.Defendant;
import uk.gov.justice.core.courts.DefendantUpdate;
import uk.gov.justice.core.courts.ProsecutionCase;
import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.common.converter.ObjectToJsonObjectConverter;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.progression.events.DefendantMatched;
import uk.gov.moj.cpp.progression.events.DefendantUnmatched;
import uk.gov.moj.cpp.progression.events.DefendantUnmatchedV2;
import uk.gov.moj.cpp.progression.events.DefendantsMasterDefendantIdUpdated;
import uk.gov.moj.cpp.progression.events.MasterDefendantIdUpdated;
import uk.gov.moj.cpp.progression.events.MasterDefendantIdUpdatedIntoHearings;
import uk.gov.moj.cpp.progression.events.MasterDefendantIdUpdatedV2;
import uk.gov.moj.cpp.progression.events.MatchedDefendants;
import uk.gov.moj.cpp.progression.service.ProgressionService;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

import javax.inject.Inject;
import javax.json.JsonObject;

@ServiceComponent(EVENT_PROCESSOR)
public class DefendantMatchingEventProcessor {

    private static final String DEFENDANT_ID_FIELD = "defendantId";
    private static final String PROSECUTION_CASE_ID_FIELD = "prosecutionCaseId";

    @Inject
    private JsonObjectToObjectConverter jsonObjectToObjectConverter;

    @Inject
    private ObjectToJsonObjectConverter objectToJsonObjectConverter;

    @Inject
    private ProgressionService progressionService;

    @Inject
    @ServiceComponent(EVENT_PROCESSOR)
    private Sender sender;

    @Handles("progression.event.defendant-matched")
    public void handleDefendantMatchedEvent(final JsonEnvelope envelope) {
        final DefendantMatched defendantMatched = jsonObjectToObjectConverter.convert(envelope.payloadAsJsonObject(), DefendantMatched.class);
        sender.send(Enveloper.envelop(createMatchingPayload(defendantMatched.getHasDefendantAlreadyBeenDeleted()))
                .withName("public.progression.defendant-matched")
                .withMetadataFrom(envelope));
    }

    /**
     * @param envelope - the event envelope to be processed.
     * @deprecated Replaced with newer version of event that contains fully updated Defendant
     * information in event payload. See: {@link #handleDefendantUnmatchedV2Event}
     */
    @Deprecated
    @Handles("progression.event.defendant-unmatched")
    public void handleDefendantUnmatchedEvent(final JsonEnvelope envelope) {
        final DefendantUnmatched defendantUnmatched = jsonObjectToObjectConverter.convert(envelope.payloadAsJsonObject(), DefendantUnmatched.class);
        sender.send(Enveloper.envelop(createUnmatchingPayload(defendantUnmatched))
                .withName("public.progression.defendant-unmatched")
                .withMetadataFrom(envelope));

        associateMasterDefendantToDefendant(envelope, defendantUnmatched.getDefendantId(), defendantUnmatched.getDefendantId(), defendantUnmatched.getProsecutionCaseId());
    }

    @Handles("progression.event.defendant-unmatched-v2")
    public void handleDefendantUnmatchedV2Event(final JsonEnvelope envelope) {
        final DefendantUnmatchedV2 defendantUnmatched = jsonObjectToObjectConverter.convert(envelope.payloadAsJsonObject(), DefendantUnmatchedV2.class);

        sender.send(Enveloper.envelop(createUnmatchingPayload(defendantUnmatched))
                .withName("public.progression.defendant-unmatched")
                .withMetadataFrom(envelope));

        sendPublicCaseDefendantChangedEvent(envelope, defendantUnmatched.getDefendant().getMasterDefendantId(), defendantUnmatched.getDefendant());
    }

    @Handles("progression.event.master-defendant-id-updated")
    public void handleMasterDefendantIdUpdatedEvent(final JsonEnvelope envelope) {
        final MasterDefendantIdUpdated masterDefendantIdUpdated = jsonObjectToObjectConverter.convert(envelope.payloadAsJsonObject(), MasterDefendantIdUpdated.class);
        final MatchedDefendants masterDefendant = getMasterDefendant(masterDefendantIdUpdated.getMatchedDefendants());

        if (Objects.nonNull(masterDefendant)) {
            associateMasterDefendantToDefendant(envelope, masterDefendantIdUpdated.getDefendantId(), masterDefendant.getMasterDefendantId(), masterDefendantIdUpdated.getProsecutionCaseId());

            masterDefendantIdUpdated.getMatchedDefendants()
                    .forEach(matchedDefendant -> {
                        if (!masterDefendant.getMasterDefendantId().equals(matchedDefendant.getMasterDefendantId())) {
                            associateMasterDefendantToDefendant(envelope, matchedDefendant.getDefendantId(), masterDefendant.getMasterDefendantId(), matchedDefendant.getProsecutionCaseId());
                        }
                    });
        }
    }

    @Handles("progression.event.master-defendant-id-updated-v2")
    public void handleMasterDefendantIdUpdatedEventV2(final JsonEnvelope envelope) {
        final MasterDefendantIdUpdatedV2 masterDefendantIdUpdated = jsonObjectToObjectConverter.convert(envelope.payloadAsJsonObject(), MasterDefendantIdUpdatedV2.class);
        final MatchedDefendants masterDefendant = getMasterDefendant(masterDefendantIdUpdated.getMatchedDefendants());

        if (Objects.nonNull(masterDefendant)) {
            sendPublicCaseDefendantChangedEvent(envelope, masterDefendant.getMasterDefendantId(), masterDefendantIdUpdated.getDefendant());
        }
    }

    @Handles("progression.event.master-defendant-id-updated-into-hearings")
    public void handleMasterDefendantIdUpdatedEventForHearing(final JsonEnvelope envelope) {
        final MasterDefendantIdUpdatedIntoHearings masterDefendantIdUpdated = jsonObjectToObjectConverter.convert(envelope.payloadAsJsonObject(), MasterDefendantIdUpdatedIntoHearings.class);
        final MatchedDefendants masterDefendant = getMasterDefendant(masterDefendantIdUpdated.getMatchedDefendants());

        if (Objects.nonNull(masterDefendant)) {
            masterDefendantIdUpdated.getHearingIds().forEach(hearingId ->
                    updateHearing(envelope, masterDefendant.getMasterDefendantId(), masterDefendantIdUpdated.getDefendant(), hearingId));
        }
    }

    @Handles("progression.event.defendants-master-defendant-id-updated")
    public void handleDefendantsMasterDefendantIdUpdatedEvent(final JsonEnvelope envelope) {
        final DefendantsMasterDefendantIdUpdated masterDefendantIdUpdated = jsonObjectToObjectConverter.convert(envelope.payloadAsJsonObject(), DefendantsMasterDefendantIdUpdated.class);
        sendPublicCaseDefendantChangedEvent(envelope, masterDefendantIdUpdated.getDefendant().getMasterDefendantId(), masterDefendantIdUpdated.getDefendant());
    }

    private static MatchedDefendants getMasterDefendant(final List<MatchedDefendants> matchedDefendants) {
        final Comparator<MatchedDefendants> comparator = Comparator.comparing(MatchedDefendants::getCourtProceedingsInitiated);
        return matchedDefendants.stream()
                .filter(def -> Objects.nonNull(def.getCourtProceedingsInitiated()))
                .min(comparator).orElse(null);
    }

    private void associateMasterDefendantToDefendant(final JsonEnvelope envelope, final UUID defendantId, final UUID masterDefendantId, final UUID prosecutionCaseId) {
        final Optional<JsonObject> prosecutionCaseOptional = progressionService.getProsecutionCaseDetailById(envelope, prosecutionCaseId.toString());
        final JsonObject prosecutionCaseJson = prosecutionCaseOptional.orElseThrow(() -> new RuntimeException("Prosecution Case not found")).getJsonObject("prosecutionCase");
        final ProsecutionCase prosecutionCase = jsonObjectToObjectConverter.convert(prosecutionCaseJson, ProsecutionCase.class);

        final Optional<Defendant> defendant = prosecutionCase.getDefendants().stream()
                .filter(def -> def.getId().equals(defendantId))
                .findFirst();

        defendant.ifPresent(d -> sendPublicCaseDefendantChangedEvent(envelope, masterDefendantId, d));
    }

    /**
     * Sends 'public.progression.case-defendant-changed' event with the defendant in the payload,
     * but overwrites the masterDefendantId
     *
     * @param envelope          - the envelope to create the outbound envelope
     * @param masterDefendantId - the new master defendant id
     * @param defendant         - the defendant being updated
     */
    private void sendPublicCaseDefendantChangedEvent(final JsonEnvelope envelope, final UUID masterDefendantId, final Defendant defendant) {
        final JsonObject publicEventPayload = createObjectBuilder()
                .add("defendant", objectToJsonObjectConverter.convert(updateDefendant(defendant, masterDefendantId))).build();

        sender.send(Enveloper.envelop(publicEventPayload)
                .withName("public.progression.case-defendant-changed")
                .withMetadataFrom(envelope));
    }

    private void updateHearing(final JsonEnvelope envelope, final UUID masterDefendantId, final Defendant defendant, final UUID hearingId) {
        final JsonObject publicEventPayload = createObjectBuilder()
                .add("hearingId", hearingId.toString())
                .add("defendant", objectToJsonObjectConverter.convert(updateDefendant(defendant, masterDefendantId)))
                .build();

        sender.send(Enveloper.envelop(publicEventPayload)
                .withName("progression.command.update-defendant-for-hearing")
                .withMetadataFrom(envelope));
    }

    private DefendantUpdate updateDefendant(final Defendant defendant, final UUID masterDefendantId) {
        return DefendantUpdate.defendantUpdate()
                .withId(defendant.getId())
                .withProsecutionCaseId(defendant.getProsecutionCaseId())
                .withMasterDefendantId(masterDefendantId)
                .withNumberOfPreviousConvictionsCited(defendant.getNumberOfPreviousConvictionsCited())
                .withProsecutionAuthorityReference(defendant.getProsecutionAuthorityReference())
                .withWitnessStatement(defendant.getWitnessStatement())
                .withWitnessStatementWelsh(defendant.getWitnessStatementWelsh())
                .withMitigation(defendant.getMitigation())
                .withMitigationWelsh(defendant.getMitigationWelsh())
                .withAssociatedPersons(defendant.getAssociatedPersons())
                .withDefenceOrganisation(defendant.getDefenceOrganisation())
                .withPersonDefendant(defendant.getPersonDefendant())
                .withLegalEntityDefendant(defendant.getLegalEntityDefendant())
                .withPncId(defendant.getPncId())
                .withAliases(defendant.getAliases())
                .withIsYouth(defendant.getIsYouth())
                .build();
    }

    private JsonObject createMatchingPayload(final boolean hasBeenDeleted) {
        return createObjectBuilder()
                .add("hasDefendantAlreadyBeenDeleted", hasBeenDeleted)
                .build();
    }

    private JsonObject createUnmatchingPayload(final DefendantUnmatched defendantUnmatched) {
        return createObjectBuilder()
                .add(DEFENDANT_ID_FIELD, defendantUnmatched.getDefendantId().toString())
                .add(PROSECUTION_CASE_ID_FIELD, defendantUnmatched.getProsecutionCaseId().toString())
                .build();
    }

    private JsonObject createUnmatchingPayload(final DefendantUnmatchedV2 defendantUnmatched) {
        return createObjectBuilder()
                .add(DEFENDANT_ID_FIELD, defendantUnmatched.getDefendantId().toString())
                .add(PROSECUTION_CASE_ID_FIELD, defendantUnmatched.getProsecutionCaseId().toString())
                .build();
    }
}
