package uk.gov.moj.cpp.prosecutioncase.event.listener;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static uk.gov.justice.services.core.annotation.Component.EVENT_LISTENER;
import static uk.gov.moj.cpp.progression.domain.constant.LegalAidStatusEnum.PENDING;
import static uk.gov.moj.cpp.progression.domain.constant.LegalAidStatusEnum.WITHDRAWN;
import static uk.gov.moj.cpp.progression.util.ReportingRestrictionHelper.dedupAllReportingRestrictionsForDefendantCaseOffences;

import uk.gov.justice.core.courts.Defendant;
import uk.gov.justice.core.courts.DefendantCaseOffences;
import uk.gov.justice.core.courts.Hearing;
import uk.gov.justice.core.courts.HearingListingStatus;
import uk.gov.justice.core.courts.JudicialResult;
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
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonReader;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

    private static final Logger LOGGER = LoggerFactory.getLogger(ProsecutionCaseOffencesUpdatedEventListener.class);


    @Handles("progression.event.prosecution-case-offences-updated")
    public void processProsecutionCaseOffencesUpdated(final JsonEnvelope event) {
        final ProsecutionCaseOffencesUpdated prosecutionCaseOffencesUpdated =
                jsonObjectConverter.convert(event.payloadAsJsonObject(), ProsecutionCaseOffencesUpdated.class);

        final DefendantCaseOffences defendantCaseOffences = dedupAllReportingRestrictionsForDefendantCaseOffences((prosecutionCaseOffencesUpdated.getDefendantCaseOffences()));
        filterDuplicateOffencesById(defendantCaseOffences.getOffences());

        final ProsecutionCaseEntity prosecutionCaseEntity = repository.findByCaseId(defendantCaseOffences.getProsecutionCaseId());
        final JsonObject prosecutionCaseJson = jsonFromString(prosecutionCaseEntity.getPayload());
        final ProsecutionCase prosecutionCase = jsonObjectConverter.convert(prosecutionCaseJson, ProsecutionCase.class);
        prosecutionCase.getDefendants().forEach(defendant ->
                filterDuplicateOffencesById(defendant.getOffences())
        );

        final List<UUID> caseOffencesIds = prosecutionCase.getDefendants().stream()
                .flatMap(d -> d.getOffences().stream())
                .map(Offence::getId)
                .distinct()
                .collect(Collectors.toList());
        updateOffenceForDefendant(defendantCaseOffences, prosecutionCase);
        repository.save(getProsecutionCaseEntity(prosecutionCase));
        final List<CaseDefendantHearingEntity> caseDefendantHearingEntities = caseDefendantHearingRepository.findByDefendantId(defendantCaseOffences.getDefendantId());

        caseDefendantHearingEntities.stream().filter(entity-> entity.getHearing().getListingStatus()!= HearingListingStatus.HEARING_RESULTED).forEach(caseDefendantHearingEntity -> {
            final HearingEntity hearingEntity = caseDefendantHearingEntity.getHearing();
            final JsonObject hearingJson = jsonFromString(hearingEntity.getPayload());
            final Hearing hearing = jsonObjectConverter.convert(hearingJson, Hearing.class);
            if (hearing.getProsecutionCases() != null) {
                final DefendantCaseOffences defendantCaseOffencesForHearing = getDefendantCaseOffencesForHearing(defendantCaseOffences, caseOffencesIds, hearing);
                hearing.getProsecutionCases().stream().forEach(hearingProsecutionCase ->
                        updateOffenceForDefendant(defendantCaseOffencesForHearing, hearingProsecutionCase)
                );
            }
            hearingEntity.setPayload(objectToJsonObjectConverter.convert(hearing).toString());
            hearingRepository.save(hearingEntity);
        });
    }

    private DefendantCaseOffences getDefendantCaseOffencesForHearing(final DefendantCaseOffences defendantCaseOffences, final List<UUID> caseOffencesIds, final Hearing hearing) {
        final List<UUID> hearingOffenceIds = hearing.getProsecutionCases().stream()
                .flatMap(pc -> pc.getDefendants().stream())
                .flatMap(d -> d.getOffences().stream())
                .map(Offence::getId)
                .collect(Collectors.toList());

        final DefendantCaseOffences defendantCaseOffencesForHearing = DefendantCaseOffences.defendantCaseOffences()
                .withDefendantId(defendantCaseOffences.getDefendantId())
                .withOffences(new ArrayList<>())
                .withProsecutionCaseId(defendantCaseOffences.getProsecutionCaseId())
                .withLegalAidStatus(defendantCaseOffences.getLegalAidStatus())
                .build();

        defendantCaseOffences.getOffences().forEach(offence ->
                addDefendantCaseOffencesForHearing(caseOffencesIds, hearingOffenceIds, defendantCaseOffencesForHearing, offence)
        );

        return defendantCaseOffencesForHearing;
    }

    private void addDefendantCaseOffencesForHearing(final List<UUID> caseOffencesIds, final List<UUID> hearingOffenceIds, final DefendantCaseOffences defendantCaseOffencesForHearing, final Offence offence) {
        if (hearingOffenceIds.contains(offence.getId()) || !caseOffencesIds.contains(offence.getId())) {
            defendantCaseOffencesForHearing.getOffences().add(offence);
        }
    }

    private void updateOffenceForDefendant(DefendantCaseOffences defendantCaseOffences, final ProsecutionCase prosecutionCase) {

        final Optional<Defendant> optionalDefendant = prosecutionCase.getDefendants().stream()
                .filter(d -> d.getId().equals(defendantCaseOffences.getDefendantId()))
                .findFirst();

        if (optionalDefendant.isPresent() && !defendantCaseOffences.getOffences().isEmpty()) {
            final Defendant defendant = optionalDefendant.get();

            final List<Offence> persistedOffences = new ArrayList<>(optionalDefendant.get().getOffences());


            //amend
            mergeOffence(persistedOffences, defendantCaseOffences.getOffences(), defendant.getOffences());

            //Add
            final List<Offence> offenceDetailListAdd = getAddedOffences(defendantCaseOffences.getOffences(), defendant.getOffences());
            optionalDefendant.get().getOffences().addAll(offenceDetailListAdd);

            //Add Legal Aid Status and proceeding concluded
            final Defendant updatedDefendant = Defendant.defendant()
                    .withId(defendant.getId())
                    .withMasterDefendantId(defendant.getMasterDefendantId())
                    .withCourtProceedingsInitiated(defendant.getCourtProceedingsInitiated())
                    .withAliases(defendant.getAliases())
                    .withAssociatedPersons(defendant.getAssociatedPersons())
                    .withCroNumber(defendant.getCroNumber())
                    .withDefenceOrganisation(defendant.getDefenceOrganisation())
                    .withDefendantCaseJudicialResults(extractNonNowsResults(defendant.getDefendantCaseJudicialResults()))
                    .withLegalEntityDefendant(defendant.getLegalEntityDefendant())
                    .withMitigation(defendant.getMitigation())
                    .withMitigationWelsh(defendant.getMitigationWelsh())
                    .withNumberOfPreviousConvictionsCited(defendant.getNumberOfPreviousConvictionsCited())
                    .withOffences(filterOffences(defendant.getOffences()))
                    .withCpsDefendantId(defendant.getCpsDefendantId())
                    .withPersonDefendant(defendant.getPersonDefendant())
                    .withProsecutionAuthorityReference(defendant.getProsecutionAuthorityReference())
                    .withWitnessStatement(defendant.getWitnessStatement())
                    .withWitnessStatementWelsh(defendant.getWitnessStatementWelsh())
                    .withPncId(defendant.getPncId())
                    .withProsecutionCaseId(defendant.getProsecutionCaseId())
                    .withLegalAidStatus(getLegalAidStatus(defendantCaseOffences.getLegalAidStatus()))
                    .withProceedingsConcluded(defendant.getProceedingsConcluded())
                    .withAssociatedDefenceOrganisation(defendant.getAssociatedDefenceOrganisation())
                    .withAssociationLockedByRepOrder(defendant.getAssociationLockedByRepOrder())
                    .withIsYouth(defendant.getIsYouth())
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
                .withValuesFrom(persistedOffence)
                .withOffenceCode(updatedOffence.getOffenceCode())
                .withStartDate(updatedOffence.getStartDate())
                .withArrestDate(updatedOffence.getArrestDate())
                .withChargeDate(updatedOffence.getChargeDate())
                .withEndDate(updatedOffence.getEndDate())
                .withConvictingCourt(nonNull(updatedOffence.getConvictingCourt()) ? updatedOffence.getConvictingCourt() : persistedOffence.getConvictingCourt())
                .withConvictionDate(nonNull(updatedOffence.getConvictionDate()) ? updatedOffence.getConvictionDate() : persistedOffence.getConvictionDate())
                .withOffenceTitle(updatedOffence.getOffenceTitle())
                .withOffenceTitleWelsh(updatedOffence.getOffenceTitleWelsh())
                .withWording(updatedOffence.getWording())
                .withWordingWelsh(updatedOffence.getWordingWelsh())
                .withOffenceLegislation(updatedOffence.getOffenceLegislation())
                .withOffenceLegislationWelsh(updatedOffence.getOffenceLegislationWelsh())
                .withCount(updatedOffence.getCount())
                .withIndictmentParticular(updatedOffence.getIndictmentParticular())
                .withLaaApplnReference(nonNull(updatedOffence.getLaaApplnReference())
                        ? updatedOffence.getLaaApplnReference() : persistedOffence.getLaaApplnReference())
                .withOffenceDateCode(updatedOffence.getOffenceDateCode())
                .withCommittingCourt(updatedOffence.getCommittingCourt())
                .withReportingRestrictions(nonNull(updatedOffence.getReportingRestrictions()) ? updatedOffence.getReportingRestrictions() : persistedOffence.getReportingRestrictions())
                .withDvlaOffenceCode(updatedOffence.getDvlaOffenceCode())
                .withOffenceFacts(nonNull(updatedOffence.getOffenceFacts()) ? updatedOffence.getOffenceFacts() : persistedOffence.getOffenceFacts())
                .build();
    }

    private ProsecutionCaseEntity getProsecutionCaseEntity(final ProsecutionCase prosecutionCase) {
        final ProsecutionCaseEntity pCaseEntity = new ProsecutionCaseEntity();
        pCaseEntity.setCaseId(prosecutionCase.getId());
        if (nonNull(prosecutionCase.getGroupId())) {
            pCaseEntity.setGroupId(prosecutionCase.getGroupId());
        }
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


    private void filterDuplicateOffencesById(List<Offence> offences) {
        if (isNull(offences) || offences.isEmpty()) {
            return;
        }
        final Set<UUID> offenceIds = new HashSet<>();
        offences.removeIf(e -> !offenceIds.add(e.getId()));
        LOGGER.info("Removing duplicate offence, offences count:{} and offences count after filtering:{} ", offences.size(), offenceIds.size());
    }

    private List<Offence> filterOffences(final List<Offence> offences) {
        if (isNull(offences) || offences.isEmpty()) {
            return offences;
        }
        offences.stream().filter(Objects::nonNull).forEach(offence -> {
            final List<JudicialResult> judicialResults = extractNonNowsResults(offence.getJudicialResults());
            if (nonNull(judicialResults) && !judicialResults.isEmpty()) {
                offence.getJudicialResults().clear();
                offence.getJudicialResults().addAll(judicialResults);
            }
        });

        return offences;
    }

    private List<JudicialResult> extractNonNowsResults(final List<JudicialResult> judicialResults) {
        if (isNull(judicialResults) || judicialResults.isEmpty()) {
            return judicialResults;
        }

        return judicialResults.stream()
                .filter(Objects::nonNull)
                .filter(jr -> !Boolean.TRUE.equals(jr.getPublishedForNows()))
                .collect(Collectors.toList());
    }
}
