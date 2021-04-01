package uk.gov.moj.cpp.prosecutioncase.event.listener;

import static java.util.Comparator.comparing;
import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static java.util.UUID.randomUUID;
import static uk.gov.justice.services.core.annotation.Component.EVENT_LISTENER;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.gov.justice.core.courts.Defendant;
import uk.gov.justice.core.courts.DefendantPartialMatchCreated;
import uk.gov.justice.core.courts.ProsecutionCase;
import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.common.converter.ObjectToJsonObjectConverter;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.progression.events.DefendantMatched;
import uk.gov.moj.cpp.progression.events.DefendantUnmatched;
import uk.gov.moj.cpp.progression.events.MasterDefendantIdUpdated;
import uk.gov.moj.cpp.progression.events.MatchedDefendants;
import uk.gov.moj.cpp.prosecutioncase.persistence.entity.DefendantPartialMatchEntity;
import uk.gov.moj.cpp.prosecutioncase.persistence.entity.MatchDefendantCaseHearingEntity;
import uk.gov.moj.cpp.prosecutioncase.persistence.entity.ProsecutionCaseEntity;
import uk.gov.moj.cpp.prosecutioncase.persistence.repository.DefendantPartialMatchRepository;
import uk.gov.moj.cpp.prosecutioncase.persistence.repository.HearingRepository;
import uk.gov.moj.cpp.prosecutioncase.persistence.repository.MatchDefendantCaseHearingRepository;
import uk.gov.moj.cpp.prosecutioncase.persistence.repository.ProsecutionCaseRepository;
import javax.inject.Inject;
import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonReader;
import java.io.StringReader;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@ServiceComponent(EVENT_LISTENER)
public class DefendantMatchingEventListener {

    private static final Logger LOGGER = LoggerFactory.getLogger(DefendantMatchingEventListener.class);
    private static final String INACTIVE = "INACTIVE";
    private static final String CLOSED = "CLOSED";

    @Inject
    private JsonObjectToObjectConverter jsonObjectConverter;

    @Inject
    private ObjectToJsonObjectConverter objectToJsonObjectConverter;

    @Inject
    private DefendantPartialMatchRepository defendantPartialMatchRepository;

    @Inject
    private ProsecutionCaseRepository prosecutionCaseRepository;

    @Inject
    private HearingRepository hearingRepository;

    @Inject
    private MatchDefendantCaseHearingRepository matchDefendantCaseHearingRepository;

    @Handles("progression.event.defendant-partial-match-created")
    public void createDefendantPartialMatch(final JsonEnvelope event) {
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("received event progression.event.defendant-partial-match-created {} ", event.toObfuscatedDebugString());
        }
        final DefendantPartialMatchCreated defendantPartialMatchCreated = jsonObjectConverter.convert(event.payloadAsJsonObject(), DefendantPartialMatchCreated.class);
        final DefendantPartialMatchEntity entity = new DefendantPartialMatchEntity();
        entity.setDefendantId(defendantPartialMatchCreated.getDefendantId());
        entity.setProsecutionCaseId(defendantPartialMatchCreated.getProsecutionCaseId());
        entity.setDefendantName(defendantPartialMatchCreated.getDefendantName());
        entity.setCaseReference(defendantPartialMatchCreated.getCaseReference());
        entity.setPayload(defendantPartialMatchCreated.getPayload());
        entity.setCaseReceivedDatetime(defendantPartialMatchCreated.getCaseReceivedDatetime());
        defendantPartialMatchRepository.save(entity);
    }

    @Handles("progression.event.defendant-matched")
    public void deleteDefendantPartialMatch(final JsonEnvelope event) {
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("received event progression.event.defendant-matched {} ", event.toObfuscatedDebugString());
        }
        final DefendantMatched defendantMatched = jsonObjectConverter.convert(event.payloadAsJsonObject(), DefendantMatched.class);
        defendantPartialMatchRepository.remove(defendantPartialMatchRepository.findByDefendantId(defendantMatched.getDefendantId()));
    }

    @Handles("progression.event.defendant-unmatched")
    public void defendantUnmatch(final JsonEnvelope event) {
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("received event progression.event.defendant-unmatched {} ", event.toObfuscatedDebugString());
        }
        final DefendantUnmatched defendantUnmatched = jsonObjectConverter.convert(event.payloadAsJsonObject(), DefendantUnmatched.class);

        final ProsecutionCaseEntity prosecutionCaseEntity = prosecutionCaseRepository.findByCaseId(defendantUnmatched.getProsecutionCaseId());
        final ProsecutionCase prosecutionCase = jsonObjectConverter.convert(jsonFromString(prosecutionCaseEntity.getPayload()), ProsecutionCase.class);
        if (isNull(prosecutionCase.getCaseStatus()) ||
                !(CLOSED.equalsIgnoreCase(prosecutionCase.getCaseStatus()) || INACTIVE.equalsIgnoreCase(prosecutionCase.getCaseStatus()))) {
            final List<MatchDefendantCaseHearingEntity> matchDefendantCaseHearingEntities = matchDefendantCaseHearingRepository.findByProsecutionCaseIdAndDefendantId(defendantUnmatched.getProsecutionCaseId(), defendantUnmatched.getDefendantId());
            matchDefendantCaseHearingEntities.forEach(matchDefendantCaseHearingEntity ->  matchDefendantCaseHearingRepository.remove(matchDefendantCaseHearingEntity));
            updateMasterDefendant(defendantUnmatched.getDefendantId(), defendantUnmatched.getDefendantId(), prosecutionCase);
        }
    }

    @Handles("progression.event.master-defendant-id-updated")
    public void updateMasterDefendantId(final JsonEnvelope event) {
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("received event progression.event.master-defendant-id-updated {} ", event.toObfuscatedDebugString());
        }

        final MasterDefendantIdUpdated masterDefendantIdUpdated = jsonObjectConverter.convert(event.payloadAsJsonObject(), MasterDefendantIdUpdated.class);

        final MatchedDefendants masterDefendant = getMasterDefendant(masterDefendantIdUpdated.getMatchedDefendants());

        if (nonNull(masterDefendant)) {
            associateMasterDefendantToDefendant(masterDefendantIdUpdated.getDefendantId(), masterDefendant.getMasterDefendantId(), masterDefendantIdUpdated.getProsecutionCaseId(), masterDefendantIdUpdated.getHearingId());
            masterDefendantIdUpdated.getMatchedDefendants()
                    .forEach(matchedDefendant ->
                            associateMasterDefendantToDefendant(matchedDefendant.getDefendantId(), masterDefendant.getMasterDefendantId(), matchedDefendant.getProsecutionCaseId(), null)
                    );
        }
    }

    private void associateMasterDefendantToDefendant(final UUID defendantId, final UUID masterDefendantId, final UUID prosecutionCaseId, final UUID hearingId) {
        final ProsecutionCaseEntity prosecutionCaseEntity = prosecutionCaseRepository.findOptionalByCaseId(prosecutionCaseId);
        if (isNull(prosecutionCaseEntity)){
            LOGGER.warn("ProsecutionCase not found: {}", prosecutionCaseId);
            return;
        }

        final ProsecutionCase prosecutionCase = jsonObjectConverter.convert(jsonFromString(prosecutionCaseEntity.getPayload()), ProsecutionCase.class);
        if (isNull(prosecutionCase.getCaseStatus()) ||
                !(CLOSED.equalsIgnoreCase(prosecutionCase.getCaseStatus()) || INACTIVE.equalsIgnoreCase(prosecutionCase.getCaseStatus()))) {
            updateMasterDefendant(defendantId, masterDefendantId, prosecutionCase);

            MatchDefendantCaseHearingEntity matchDefendantCaseHearingEntity = matchDefendantCaseHearingRepository.findByHearingIdAndProsecutionCaseIdAndDefendantId(hearingId, prosecutionCaseId, defendantId);
            if (nonNull(matchDefendantCaseHearingEntity)) {
                matchDefendantCaseHearingEntity.setMasterDefendantId(masterDefendantId);
            } else {
                matchDefendantCaseHearingEntity = new MatchDefendantCaseHearingEntity();
                matchDefendantCaseHearingEntity.setId(randomUUID());
                matchDefendantCaseHearingEntity.setDefendantId(defendantId);
                matchDefendantCaseHearingEntity.setMasterDefendantId(masterDefendantId);
                matchDefendantCaseHearingEntity.setProsecutionCaseId(prosecutionCaseId);
                matchDefendantCaseHearingEntity.setHearingId(hearingId);
                if(nonNull(hearingId)) {
                    matchDefendantCaseHearingEntity.setHearing( hearingRepository.findBy(hearingId) );
                }
                matchDefendantCaseHearingEntity.setProsecutionCase(prosecutionCaseRepository.findByCaseId(prosecutionCaseId));
            }
            matchDefendantCaseHearingRepository.save(matchDefendantCaseHearingEntity);
        }
    }

    private void updateMasterDefendant(UUID defendantId, UUID masterDefendantId, ProsecutionCase prosecutionCase) {
        ProsecutionCaseEntity prosecutionCaseEntity;
        final Optional<Defendant> defendant = prosecutionCase.getDefendants().stream()
                .filter(def -> def.getId().equals(defendantId))
                .findFirst();
        if (defendant.isPresent()) {
            final Defendant updatedDefendant = updateDefendant(defendant.get(), masterDefendantId);
            prosecutionCase.getDefendants().remove(defendant.get());
            prosecutionCase.getDefendants().add(updatedDefendant);
        }
        prosecutionCaseEntity = getProsecutionCaseEntity(prosecutionCase);
        prosecutionCaseRepository.save(prosecutionCaseEntity);
    }

    private static MatchedDefendants getMasterDefendant(final List<MatchedDefendants> matchedDefendants) {
        final Comparator<MatchedDefendants> comparator = comparing(MatchedDefendants::getCourtProceedingsInitiated);
        return matchedDefendants.stream()
                .filter(def -> nonNull(def.getCourtProceedingsInitiated()))
                .min(comparator)
                .orElse(null);
    }

    private ProsecutionCaseEntity getProsecutionCaseEntity(final ProsecutionCase prosecutionCase) {
        final ProsecutionCaseEntity pCaseEntity = new ProsecutionCaseEntity();
        pCaseEntity.setCaseId(prosecutionCase.getId());
        pCaseEntity.setPayload(objectToJsonObjectConverter.convert(prosecutionCase).toString());
        return pCaseEntity;
    }

    private Defendant updateDefendant(final Defendant defendant, final UUID masterDefendantId) {
        return Defendant.defendant()
                .withOffences(defendant.getOffences())
                .withPersonDefendant(defendant.getPersonDefendant())
                .withLegalEntityDefendant(defendant.getLegalEntityDefendant())
                .withAssociatedPersons(defendant.getAssociatedPersons())
                .withId(defendant.getId())
                .withMasterDefendantId(masterDefendantId)
                .withCourtProceedingsInitiated(defendant.getCourtProceedingsInitiated())
                .withMitigation(defendant.getMitigation())
                .withMitigationWelsh(defendant.getMitigationWelsh())
                .withNumberOfPreviousConvictionsCited(defendant.getNumberOfPreviousConvictionsCited())
                .withProsecutionAuthorityReference(defendant.getProsecutionAuthorityReference())
                .withProsecutionCaseId(defendant.getProsecutionCaseId())
                .withWitnessStatement(defendant.getWitnessStatement())
                .withWitnessStatementWelsh(defendant.getWitnessStatementWelsh())
                .withDefenceOrganisation(defendant.getDefenceOrganisation())
                .withPncId(defendant.getPncId())
                .withDefendantCaseJudicialResults(defendant.getDefendantCaseJudicialResults())
                .withAliases(defendant.getAliases())
                .withIsYouth(defendant.getIsYouth())
                .withCroNumber(defendant.getCroNumber())
                .withLegalAidStatus(defendant.getLegalAidStatus())
                .withAssociatedDefenceOrganisation(defendant.getAssociatedDefenceOrganisation())
                .withProceedingsConcluded(defendant.getProceedingsConcluded())
                .withAssociationLockedByRepOrder(defendant.getAssociationLockedByRepOrder())
                .build();
    }

    private static JsonObject jsonFromString(final String jsonObjectStr) {
        final JsonReader jsonReader = Json.createReader(new StringReader(jsonObjectStr));
        final JsonObject object = jsonReader.readObject();
        jsonReader.close();
        return object;
    }
}
