package uk.gov.moj.cpp.prosecutioncase.event.listener;

import static java.util.Objects.nonNull;
import static uk.gov.justice.services.core.annotation.Component.EVENT_LISTENER;
import static uk.gov.moj.cpp.progression.domain.constant.LegalAidStatusEnum.PENDING;
import static uk.gov.moj.cpp.progression.domain.constant.LegalAidStatusEnum.WITHDRAWN;

import uk.gov.justice.core.courts.Defendant;
import uk.gov.justice.core.courts.DefendantCaseOffences;
import uk.gov.justice.core.courts.Hearing;
import uk.gov.justice.core.courts.Offence;
import uk.gov.justice.core.courts.ProsecutionCase;
import uk.gov.justice.core.courts.ProsecutionCaseOffencesUpdated;
import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.common.converter.ObjectToJsonObjectConverter;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.prosecutioncase.persistence.entity.CaseDefendantHearingEntity;
import uk.gov.moj.cpp.prosecutioncase.persistence.entity.HearingEntity;
import uk.gov.moj.cpp.prosecutioncase.persistence.entity.ProsecutionCaseEntity;
import uk.gov.moj.cpp.prosecutioncase.persistence.repository.CaseDefendantHearingRepository;
import uk.gov.moj.cpp.prosecutioncase.persistence.repository.HearingRepository;
import uk.gov.moj.cpp.prosecutioncase.persistence.repository.ProsecutionCaseRepository;

import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonReader;

@SuppressWarnings("squid:S3655")
@ServiceComponent(EVENT_LISTENER)
public class ProsecutionCaseOffencesUpdatedEventListener {

    private static final String EMPTY = "";
    @Inject
    private JsonObjectToObjectConverter jsonObjectConverter;

    @Inject
    private ObjectToJsonObjectConverter objectToJsonObjectConverter;

    @Inject
    private ProsecutionCaseRepository repository;

    @Inject
    private CaseDefendantHearingRepository caseDefendantHearingRepository;

    @Inject
    private HearingRepository hearingRepository;

    @Handles("progression.event.prosecution-case-offences-updated")
    public void processProsecutionCaseOffencesUpdated(final JsonEnvelope event) {
        final ProsecutionCaseOffencesUpdated prosecutionCaseOffencesUpdated =
                jsonObjectConverter.convert(event.payloadAsJsonObject(), ProsecutionCaseOffencesUpdated.class);
        final DefendantCaseOffences defendantCaseOffences = prosecutionCaseOffencesUpdated.getDefendantCaseOffences();
        if (defendantCaseOffences != null) {
            final ProsecutionCaseEntity prosecutionCaseEntity = repository.findByCaseId(defendantCaseOffences.getProsecutionCaseId());
            final JsonObject prosecutionCaseJson = jsonFromString(prosecutionCaseEntity.getPayload());
            final ProsecutionCase prosecutionCase = jsonObjectConverter.convert(prosecutionCaseJson, ProsecutionCase.class);
            updateOffenceForDefendant(defendantCaseOffences, prosecutionCase);
            repository.save(getProsecutionCaseEntity(prosecutionCase));
            final List<CaseDefendantHearingEntity> caseDefendantHearingEntities = caseDefendantHearingRepository.findByDefendantId(defendantCaseOffences.getDefendantId());
            caseDefendantHearingEntities.stream().forEach(caseDefendantHearingEntity -> {
                final HearingEntity hearingEntity = caseDefendantHearingEntity.getHearing();
                final JsonObject hearingJson = jsonFromString(hearingEntity.getPayload());
                final Hearing hearing = jsonObjectConverter.convert(hearingJson, Hearing.class);
                hearing.getProsecutionCases().stream().forEach(hearingProsecutionCase ->
                        updateOffenceForDefendant(defendantCaseOffences, hearingProsecutionCase)
                );
                hearingEntity.setPayload(objectToJsonObjectConverter.convert(hearing).toString());
                hearingRepository.save(hearingEntity);
            });
        }
    }

    private void updateOffenceForDefendant(DefendantCaseOffences defendantCaseOffences, final ProsecutionCase prosecutionCase) {

        final Optional<Defendant> optionalDefendant = prosecutionCase.getDefendants().stream()
                .filter(d -> d.getId().equals(defendantCaseOffences.getDefendantId()))
                .findFirst();
        if (optionalDefendant.isPresent() && !defendantCaseOffences.getOffences().isEmpty()) {
            final Defendant defendant = optionalDefendant.get();

            final List<Offence> persistedOffences = new ArrayList<>(optionalDefendant.get().getOffences());

            //Delete
            final List<Offence> offenceDetailListDel = getDeletedOffences(defendantCaseOffences.getOffences(), defendant.getOffences());
            defendant.getOffences().removeAll(offenceDetailListDel);

            //amend
            mergeOffence(persistedOffences, defendantCaseOffences.getOffences(), defendant.getOffences());

            //Add
            final List<Offence> offenceDetailListAdd = getAddedOffences(defendantCaseOffences.getOffences(), defendant.getOffences());
            optionalDefendant.get().getOffences().addAll(offenceDetailListAdd);

            //Add Legal Aid Status and proceeding concluded
            final Defendant updatedDefendant = Defendant.defendant()
                    .withId(defendant.getId())
                    .withMasterDefendantId(defendant.getId())
                    .withCourtProceedingsInitiated(defendant.getCourtProceedingsInitiated())
                    .withAliases(defendant.getAliases())
                    .withAssociatedPersons(defendant.getAssociatedPersons())
                    .withCroNumber(defendant.getCroNumber())
                    .withDefenceOrganisation(defendant.getDefenceOrganisation())
                    .withJudicialResults(defendant.getJudicialResults())
                    .withLegalEntityDefendant(defendant.getLegalEntityDefendant())
                    .withMitigation(defendant.getMitigation())
                    .withMitigationWelsh(defendant.getMitigationWelsh())
                    .withNumberOfPreviousConvictionsCited(defendant.getNumberOfPreviousConvictionsCited())
                    .withOffences(defendant.getOffences())
                    .withPersonDefendant(defendant.getPersonDefendant())
                    .withProsecutionAuthorityReference(defendant.getProsecutionAuthorityReference())
                    .withWitnessStatement(defendant.getWitnessStatement())
                    .withWitnessStatementWelsh(defendant.getWitnessStatementWelsh())
                    .withPncId(defendant.getPncId())
                    .withWitnessStatementWelsh(defendant.getWitnessStatementWelsh())
                    .withProsecutionCaseId(defendant.getProsecutionCaseId())
                    .withLegalAidStatus(getLegalAidStatus(defendantCaseOffences.getLegalAidStatus()))
                    .withProceedingsConcluded(defendant.getProceedingsConcluded())
                    .withAssociatedDefenceOrganisation(defendant.getAssociatedDefenceOrganisation())
                    .withAssociationLockedByRepOrder(defendant.getAssociationLockedByRepOrder())
                    .build();
            prosecutionCase.getDefendants().remove(defendant);
            prosecutionCase.getDefendants().add(updatedDefendant);
        }
    }

    private String getLegalAidStatus(final String offenceLegalAidStatus) {
        return WITHDRAWN.getDescription().equals(offenceLegalAidStatus) || PENDING.getDescription().equals(offenceLegalAidStatus)
                ? EMPTY
                : offenceLegalAidStatus;
    }

    private static List<Offence> getDeletedOffences(
            final List<Offence> commandOffences,
            final List<Offence> existingOffences) {
        return existingOffences.stream()
                .filter(existingOffence -> !commandOffences.stream()
                        .map(Offence::getId)
                        .collect(Collectors.toList())
                        .contains(existingOffence.getId()))
                .collect(Collectors.toList());
    }

    private static List<Offence> getAddedOffences(
            final List<Offence> commandOffences,
            final List<Offence> existingOffences) {
        return commandOffences.stream()
                .filter(commandOffence -> !existingOffences.stream()
                        .map(Offence::getId)
                        .collect(Collectors.toList())
                        .contains(commandOffence.getId()))
                .collect(Collectors.toList());
    }

    private Offence updateOffence(final Offence persistedOffence, final Offence updatedOffence) {
        return Offence.offence()
                .withId(persistedOffence.getId())
                .withOffenceCode(updatedOffence.getOffenceCode())
                .withStartDate(updatedOffence.getStartDate())
                .withArrestDate(updatedOffence.getArrestDate())
                .withChargeDate(updatedOffence.getChargeDate())
                .withConvictionDate(persistedOffence.getConvictionDate())
                .withEndDate(updatedOffence.getEndDate())
                .withIndicatedPlea(persistedOffence.getIndicatedPlea())
                .withPlea(persistedOffence.getPlea())
                .withAllocationDecision(persistedOffence.getAllocationDecision())
                .withOffenceTitle(updatedOffence.getOffenceTitle())
                .withOffenceTitleWelsh(updatedOffence.getOffenceTitleWelsh())
                .withWording(updatedOffence.getWording())
                .withWordingWelsh(updatedOffence.getWordingWelsh())
                .withOffenceLegislation(updatedOffence.getOffenceLegislation())
                .withOffenceLegislationWelsh(updatedOffence.getOffenceLegislationWelsh())
                .withOrderIndex(persistedOffence.getOrderIndex())
                .withOffenceFacts(persistedOffence.getOffenceFacts())
                .withOffenceDefinitionId(persistedOffence.getOffenceDefinitionId())
                .withNotifiedPlea(persistedOffence.getNotifiedPlea())
                .withModeOfTrial(persistedOffence.getModeOfTrial())
                .withCount(updatedOffence.getCount())
                .withLaaApplnReference(nonNull(updatedOffence.getLaaApplnReference())
                        ? updatedOffence.getLaaApplnReference() : persistedOffence.getLaaApplnReference())
                .withProceedingsConcluded(persistedOffence.getProceedingsConcluded())
                .withIntroducedAfterInitialProceedings(persistedOffence.getIntroducedAfterInitialProceedings())
                .withIsDiscontinued(persistedOffence.getIsDiscontinued())
                .build();
    }

    private ProsecutionCaseEntity getProsecutionCaseEntity(final ProsecutionCase prosecutionCase) {
        final ProsecutionCaseEntity pCaseEntity = new ProsecutionCaseEntity();
        pCaseEntity.setCaseId(prosecutionCase.getId());
        pCaseEntity.setPayload(objectToJsonObjectConverter.convert(prosecutionCase).toString());
        return pCaseEntity;
    }

    private static JsonObject jsonFromString(String jsonObjectStr) {

        final JsonReader jsonReader = Json.createReader(new StringReader(jsonObjectStr));
        final JsonObject object = jsonReader.readObject();
        jsonReader.close();

        return object;
    }

    private void mergeOffence(final List<Offence> persistedOffences, final List<Offence> updatedOffences, List<Offence> originalOffences) {
        persistedOffences.forEach(offencePersisted ->
                updatedOffences.forEach(offenceDetail -> {
                    if (offencePersisted.getId().equals(offenceDetail.getId())) {
                        final Offence updatedOffence = updateOffence(offencePersisted, offenceDetail);
                        originalOffences.remove(offencePersisted);
                        originalOffences.add(updatedOffence);
                    }
                })
        );


    }
}
