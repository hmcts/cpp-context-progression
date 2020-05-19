package uk.gov.moj.cpp.progression.processor;

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
import uk.gov.moj.cpp.progression.events.MasterDefendantIdUpdated;
import uk.gov.moj.cpp.progression.events.MatchedDefendants;
import uk.gov.moj.cpp.progression.service.ProgressionService;
import javax.inject.Inject;
import javax.json.Json;
import javax.json.JsonObject;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

@ServiceComponent(EVENT_PROCESSOR)
public class DefendantMatchingEventProcessor {

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

    @Handles("progression.event.defendant-unmatched")
    public void handleDefendantUnmatchedEvent(final JsonEnvelope envelope) {
        final DefendantUnmatched defendantUnmatched = jsonObjectToObjectConverter.convert(envelope.payloadAsJsonObject(), DefendantUnmatched.class);
        sender.send(Enveloper.envelop(createUnmatchingPayload(defendantUnmatched))
                .withName("public.progression.defendant-unmatched")
                .withMetadataFrom(envelope));
        associateMasterDefendantToDefendant(envelope, defendantUnmatched.getDefendantId(), defendantUnmatched.getDefendantId(), defendantUnmatched.getProsecutionCaseId());
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

        if (defendant.isPresent()) {
            final JsonObject publicEventPayload = Json.createObjectBuilder()
                    .add("defendant", objectToJsonObjectConverter.convert(updateDefendant(defendant.get(), masterDefendantId))).build();
            sender.send(Enveloper.envelop(publicEventPayload)
                    .withName("public.progression.case-defendant-changed")
                    .withMetadataFrom(envelope));
           }
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
        return Json.createObjectBuilder()
                .add("hasDefendantAlreadyBeenDeleted", hasBeenDeleted)
                .build();
    }

    private JsonObject createUnmatchingPayload(final DefendantUnmatched defendantUnmatched) {
        return Json.createObjectBuilder()
                .add("defendantId", defendantUnmatched.getDefendantId().toString())
                .add("prosecutionCaseId", defendantUnmatched.getProsecutionCaseId().toString())
                .build();
    }
}
