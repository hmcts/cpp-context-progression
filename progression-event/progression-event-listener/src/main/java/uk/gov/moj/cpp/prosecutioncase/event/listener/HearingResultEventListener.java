package uk.gov.moj.cpp.prosecutioncase.event.listener;

import static java.lang.Boolean.FALSE;
import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static java.util.Optional.ofNullable;
import static java.util.stream.Collectors.collectingAndThen;
import static java.util.stream.Collectors.toList;
import static org.apache.commons.collections.CollectionUtils.isNotEmpty;
import static uk.gov.justice.services.core.annotation.Component.EVENT_LISTENER;
import static uk.gov.moj.cpp.progression.util.ReportingRestrictionHelper.dedupAllReportingRestrictions;

import uk.gov.justice.core.courts.CourtApplication;
import uk.gov.justice.core.courts.CourtApplicationCase;
import uk.gov.justice.core.courts.CourtOrder;
import uk.gov.justice.core.courts.CourtOrderOffence;
import uk.gov.justice.core.courts.Defendant;
import uk.gov.justice.core.courts.DefendantJudicialResult;
import uk.gov.justice.core.courts.Hearing;
import uk.gov.justice.core.courts.HearingListingStatus;
import uk.gov.justice.core.courts.JudicialResult;
import uk.gov.justice.core.courts.Offence;
import uk.gov.justice.core.courts.ProsecutionCase;
import uk.gov.justice.progression.courts.HearingResulted;
import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.common.converter.ObjectToJsonObjectConverter;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.prosecutioncase.persistence.entity.CaseDefendantHearingEntity;
import uk.gov.moj.cpp.prosecutioncase.persistence.entity.HearingEntity;
import uk.gov.moj.cpp.prosecutioncase.persistence.repository.CaseDefendantHearingRepository;
import uk.gov.moj.cpp.prosecutioncase.persistence.repository.HearingRepository;

import java.io.StringReader;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.function.UnaryOperator;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.inject.Inject;
import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonReader;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@SuppressWarnings({"squid:S3655", "squid:S1135", "squid:S1612"})
@ServiceComponent(EVENT_LISTENER)
public class HearingResultEventListener {

    private static final Logger LOGGER = LoggerFactory.getLogger(HearingResultEventListener.class);

    @Inject
    private JsonObjectToObjectConverter jsonObjectConverter;

    @Inject
    private ObjectToJsonObjectConverter objectToJsonObjectConverter;

    @Inject
    private HearingRepository hearingRepository;

    @Inject
    private CaseDefendantHearingRepository caseDefendantHearingRepository;

    private static JsonObject jsonFromString(String jsonObjectStr) {

        final JsonReader jsonReader = Json.createReader(new StringReader(jsonObjectStr));
        final JsonObject object = jsonReader.readObject();
        jsonReader.close();

        return object;
    }

    @Handles("progression.event.hearing-resulted")
    public void updateHearingResult(final JsonEnvelope event) {
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("received event progression.event.hearing-resulted {} ", event.toObfuscatedDebugString());
        }

        //To save hearing result in current hearing and removing the defendant and case proceeding flag in it.
        final HearingResulted hearingResulted = jsonObjectConverter.convert(event.payloadAsJsonObject(), HearingResulted.class);

        final Hearing hearing = hearingResulted.getHearing();

        final Hearing dedupedHearing = Hearing.hearing().
                withValuesFrom(hearing).
                withProsecutionCases(hearing.getProsecutionCases()).
                withCourtApplications(dedupAllCourtApplications(hearing.getCourtApplications())).
                build();

        final HearingResulted updatedHearingResulted = HearingResulted.hearingResulted()
                .withValuesFrom(hearingResulted)
                .withHearing(dedupedHearing)
                .build();

        if(updatedHearingResulted.getHearing().getProsecutionCases() != null) {
            updatedHearingResulted.getHearing().getProsecutionCases().stream().forEach(c -> deDupAllOffencesForProsecutionCase(c));
        }
        final HearingEntity currentHearingEntity = hearingRepository.findBy(updatedHearingResulted.getHearing().getId());
        final JsonObject currentHearingJson = jsonFromString(currentHearingEntity.getPayload());
        final Hearing originalCurrentHearing = jsonObjectConverter.convert(currentHearingJson, Hearing.class);
        final Hearing updatedHearing = dedupAllReportingRestrictions(getUpdatedHearingForResulted(updatedHearingResulted.getHearing(), originalCurrentHearing, updatedHearingResulted.getHearingDay()));
        if(updatedHearing.getProsecutionCases() != null) {
            updatedHearing.getProsecutionCases().stream().forEach(c -> deDupAllOffencesForProsecutionCase(c));
        }
        final String resultedHearingPayload = objectToJsonObjectConverter.convert(updatedHearing).toString();
        currentHearingEntity.setPayload(resultedHearingPayload);
        currentHearingEntity.setListingStatus(HearingListingStatus.HEARING_RESULTED);
        hearingRepository.save(currentHearingEntity);
        LOGGER.info("Hearing: {} has been updated with listing status {}", currentHearingEntity.getHearingId(), HearingListingStatus.HEARING_RESULTED);

        //TO deal with setting of case and defendant proceeding flag for future hearing.
        final List<ProsecutionCase> prosecutionCases = ofNullable(updatedHearing.getProsecutionCases()).orElse(new ArrayList<>());
        for (final ProsecutionCase prosecutionCase : prosecutionCases) {
            final List<CaseDefendantHearingEntity> caseDefendantEntities = caseDefendantHearingRepository.findByCaseId(prosecutionCase.getId());
            for (final CaseDefendantHearingEntity caseDefendantHearingEntity : caseDefendantEntities) {
                final HearingEntity originalHearingEntity = caseDefendantHearingEntity.getHearing();
                final JsonObject hearingJson = jsonFromString(originalHearingEntity.getPayload());
                final Hearing originalHearing = jsonObjectConverter.convert(hearingJson, Hearing.class);
                final Boolean hasSharedResults = originalHearing.getHasSharedResults();
                if (FALSE.equals(hasSharedResults)) {
                    final Hearing updatedNonResultedHearing = getUpdatedHearingForNonResulted(originalHearing, updatedHearingResulted.getHearing());
                    final String payload = objectToJsonObjectConverter.convert(updatedNonResultedHearing).toString();
                    originalHearingEntity.setPayload(payload);
                    hearingRepository.save(originalHearingEntity);
                    LOGGER.info("Original hearing without shared results: {} has been updated.", originalHearingEntity.getHearingId());
                }
            }
        }
    }

    public static List<CourtApplication> dedupAllCourtApplications(final List<CourtApplication> courtApplications) {
        if (isNull(courtApplications)) {
            return courtApplications;
        }

        final Set<CourtApplication> uniqueCourtApplications = courtApplications.stream().collect(Collectors.toSet());
        final List<CourtApplication> updatedCourtApplications = uniqueCourtApplications.stream().collect(toList());

        updatedCourtApplications.stream().forEach(courtApplication -> {
            final List<JudicialResult> judicialResults = courtApplication.getJudicialResults();
            if (nonNull(judicialResults)) {
                final Set<JudicialResult> uniqueJudicialResults = judicialResults.stream().collect(Collectors.toSet());

                judicialResults.clear();
                judicialResults.addAll(uniqueJudicialResults.stream().collect(toList()));
            }
        });

        return updatedCourtApplications;
    }

    private void deDupAllOffencesForProsecutionCase(final ProsecutionCase prosecutionCase) {

        if (prosecutionCase != null) {
            prosecutionCase.getDefendants().stream().forEach(def -> filterDuplicateOffencesById(def.getOffences()));
        }
    }

    private void filterDuplicateOffencesById(final List<Offence> offences) {
        if (isNull(offences) || offences.isEmpty()) {
            return;
        }
        final Set<UUID> offenceIds = new HashSet<>();
        offences.removeIf(e -> !offenceIds.add(e.getId()));
        LOGGER.info("Removing duplicate offence, offences count:{} and offences count after filtering:{} ", offences.size(), offenceIds.size());
    }

    private Hearing getUpdatedHearingForNonResulted(final Hearing originalHearing, final Hearing resultedHearing) {
        return Hearing.hearing()
                .withIsBoxHearing(originalHearing.getIsBoxHearing())
                .withId(originalHearing.getId())
                .withProsecutionCases(getUpdatedProsecutionCasesForNonResultedHearing(originalHearing, resultedHearing))
                .withHearingDays(originalHearing.getHearingDays())
                .withCourtCentre(originalHearing.getCourtCentre())
                .withJurisdictionType(originalHearing.getJurisdictionType())
                .withType(originalHearing.getType())
                .withHearingLanguage(originalHearing.getHearingLanguage())
                .withCourtApplications(originalHearing.getCourtApplications())
                .withReportingRestrictionReason(originalHearing.getReportingRestrictionReason())
                .withJudiciary(originalHearing.getJudiciary())
                .withDefendantAttendance(originalHearing.getDefendantAttendance())
                .withDefendantReferralReasons(originalHearing.getDefendantReferralReasons())
                .withHasSharedResults(originalHearing.getHasSharedResults())
                .withDefenceCounsels(originalHearing.getDefenceCounsels())
                .withProsecutionCounsels(originalHearing.getProsecutionCounsels())
                .withRespondentCounsels(originalHearing.getRespondentCounsels())
                .withApplicationPartyCounsels(originalHearing.getApplicationPartyCounsels())
                .withCrackedIneffectiveTrial(originalHearing.getCrackedIneffectiveTrial())
                .withReportingRestrictionReason(originalHearing.getReportingRestrictionReason())
                .withHearingCaseNotes(originalHearing.getHearingCaseNotes())
                .withCourtApplicationPartyAttendance(originalHearing.getCourtApplicationPartyAttendance())
                .withCompanyRepresentatives(originalHearing.getCompanyRepresentatives())
                .withIntermediaries(originalHearing.getIntermediaries())
                .withIsEffectiveTrial(originalHearing.getIsEffectiveTrial())
                .withYouthCourt(originalHearing.getYouthCourt())
                .withYouthCourtDefendantIds(originalHearing.getYouthCourtDefendantIds())
                .build();
    }

    private List<ProsecutionCase> getUpdatedProsecutionCasesForNonResultedHearing(final Hearing originalHearing,
                                                                                  final Hearing resultedHearing) {
        return originalHearing.getProsecutionCases().stream().map(prosecutionCase ->
                getUpdatedProsecutionCaseForNonResultedHearing(prosecutionCase, resultedHearing)
        ).collect(toList());
    }

    private ProsecutionCase getUpdatedProsecutionCaseForNonResultedHearing(final ProsecutionCase prosecutionCase, final Hearing resultedHearing) {
        final Optional<ProsecutionCase> optionalResultedCase = resultedHearing.getProsecutionCases().stream()
                .filter(resultedCase -> resultedCase.getId().equals(prosecutionCase.getId()))
                .findFirst();
        if (optionalResultedCase.isPresent()) {
            final ProsecutionCase resultedCase = optionalResultedCase.get();
            return ProsecutionCase.prosecutionCase()
                    .withPoliceOfficerInCase(prosecutionCase.getPoliceOfficerInCase())
                    .withProsecutionCaseIdentifier(prosecutionCase.getProsecutionCaseIdentifier())
                    .withId(prosecutionCase.getId())
                    .withDefendants(getUpdatedDefendantsForNonResultedHearing(prosecutionCase, resultedCase))
                    .withInitiationCode(prosecutionCase.getInitiationCode())
                    .withOriginatingOrganisation(prosecutionCase.getOriginatingOrganisation())
                    .withCpsOrganisation(prosecutionCase.getCpsOrganisation())
                    .withCpsOrganisationId(prosecutionCase.getCpsOrganisationId())
                    .withIsCpsOrgVerifyError(prosecutionCase.getIsCpsOrgVerifyError())
                    .withStatementOfFacts(prosecutionCase.getStatementOfFacts())
                    .withStatementOfFactsWelsh(prosecutionCase.getStatementOfFactsWelsh())
                    .withCaseMarkers(prosecutionCase.getCaseMarkers())
                    .withAppealProceedingsPending(prosecutionCase.getAppealProceedingsPending())
                    .withBreachProceedingsPending(prosecutionCase.getBreachProceedingsPending())
                    .withRemovalReason(prosecutionCase.getRemovalReason())
                    .withCaseStatus(resultedCase.getCaseStatus())
                    .build();
        }
        return prosecutionCase;
    }

    private List<Defendant> getUpdatedDefendantsForNonResultedHearing(final ProsecutionCase prosecutionCase, final ProsecutionCase resultedCase) {
        return prosecutionCase.getDefendants().stream().map(defendant ->
                getUpdatedDefendantForNonResultedHearing(defendant, resultedCase)
        ).collect(toList());
    }

    private Defendant getUpdatedDefendantForNonResultedHearing(final Defendant originDefendant, final ProsecutionCase resultedCase) {
        final Optional<Defendant> optionalResultedDefendant = resultedCase.getDefendants().stream().filter(resultedDefendant -> resultedDefendant.getId().equals(originDefendant.getId()))
                .findFirst();
        if (optionalResultedDefendant.isPresent()) {
            final Defendant resultedDefendant = optionalResultedDefendant.get();
            return Defendant.defendant()
                    .withOffences(getUpdatedOffencesForNonResultedHearing(originDefendant.getOffences(), resultedDefendant))
                    .withCpsDefendantId(originDefendant.getCpsDefendantId())
                    .withPersonDefendant(originDefendant.getPersonDefendant())
                    .withLegalEntityDefendant(originDefendant.getLegalEntityDefendant())
                    .withAssociatedPersons(originDefendant.getAssociatedPersons())
                    .withId(originDefendant.getId())
                    .withMasterDefendantId(originDefendant.getMasterDefendantId())
                    .withCourtProceedingsInitiated(originDefendant.getCourtProceedingsInitiated())
                    .withLegalAidStatus(originDefendant.getLegalAidStatus())
                    .withMitigation(originDefendant.getMitigation())
                    .withMitigationWelsh(originDefendant.getMitigationWelsh())
                    .withNumberOfPreviousConvictionsCited(originDefendant.getNumberOfPreviousConvictionsCited())
                    .withProsecutionAuthorityReference(originDefendant.getProsecutionAuthorityReference())
                    .withProsecutionCaseId(originDefendant.getProsecutionCaseId())
                    .withWitnessStatement(originDefendant.getWitnessStatement())
                    .withWitnessStatementWelsh(originDefendant.getWitnessStatementWelsh())
                    .withDefenceOrganisation(originDefendant.getDefenceOrganisation())
                    .withPncId(originDefendant.getPncId())
                    .withAliases(originDefendant.getAliases())
                    .withIsYouth(originDefendant.getIsYouth())
                    .withCroNumber(originDefendant.getCroNumber())
                    .withDefendantCaseJudicialResults(getNonNowsResults(originDefendant.getDefendantCaseJudicialResults()))
                    .withAssociatedDefenceOrganisation(originDefendant.getAssociatedDefenceOrganisation())
                    .withProceedingsConcluded(resultedDefendant.getProceedingsConcluded())
                    .withAssociationLockedByRepOrder(originDefendant.getAssociationLockedByRepOrder())
                    .build();
        }
        return originDefendant;
    }

    private List<Offence> getUpdatedOffencesForNonResultedHearing(final List<Offence> originalOffences, final Defendant resultedDefendant) {
        return originalOffences.stream().map(offence ->
                getUpdatedOffenceForNonResultedHearing(offence, resultedDefendant)
        ).collect(toList());
    }

    private Offence getUpdatedOffenceForNonResultedHearing(final Offence originalOffence, final Defendant resultedDefendant) {
        final Optional<Offence> optionalResultedOffence = resultedDefendant.getOffences().stream().filter(offence -> offence.getId().equals(originalOffence.getId()))
                .findFirst();
        if (optionalResultedOffence.isPresent()) {
            final Offence resultedOffence = optionalResultedOffence.get();
            return Offence.offence().withValuesFrom(originalOffence)
                    .withJudicialResults(getNonNowsResults(originalOffence.getJudicialResults()))
                    .withProceedingsConcluded(resultedOffence.getProceedingsConcluded())
                    .build();
        } else {
            return originalOffence;
        }
    }

    private Hearing getUpdatedHearingForResulted(final Hearing hearingFromPayload, final Hearing hearingFromDatabase, final LocalDate hearingDay) {

        List<DefendantJudicialResult> resultsToBeAdded = null;

        if (isNotEmpty(hearingFromDatabase.getDefendantJudicialResults())) {

            resultsToBeAdded = getUpdatedDefendantJudicialResults(hearingFromPayload.getDefendantJudicialResults(), hearingFromDatabase.getDefendantJudicialResults(), hearingDay);

        } else if (isNotEmpty(hearingFromPayload.getDefendantJudicialResults())) {

            resultsToBeAdded = new ArrayList<>();
            resultsToBeAdded.addAll(hearingFromPayload.getDefendantJudicialResults());

        }

        final Hearing.Builder builder = Hearing.hearing().withValuesFrom(hearingFromPayload);
        if (isNotEmpty(hearingFromPayload.getProsecutionCases())) {
            builder.withProsecutionCases(getUpdatedProsecutionCases(hearingFromPayload, hearingFromDatabase, hearingDay));
        }
        if (isNotEmpty(hearingFromPayload.getCourtApplications())) {
            builder.withCourtApplications(getUpdatedCourtApplications(hearingFromPayload.getCourtApplications()));
        }
        return builder.withDefendantJudicialResults(getNonNowsDefendantJudicialResult(resultsToBeAdded))
                .build();
    }

    private List<CourtApplication> getUpdatedCourtApplications(final List<CourtApplication> courtApplications) {
        return courtApplications.stream()
                .map(getCourtApplicationCourtApplicationWithOutNowResults())
                .collect(toList());
    }

    private Function<CourtApplication, CourtApplication> getCourtApplicationCourtApplicationWithOutNowResults() {
        return courtApplication -> CourtApplication.courtApplication()
                .withValuesFrom(courtApplication)
                .withJudicialResults(getNonNowsResults(courtApplication.getJudicialResults()))
                .withCourtApplicationCases(getCourtApplicationCasesWithOutNowResults(courtApplication))
                .withCourtOrder(ofNullable(courtApplication.getCourtOrder()).map(courtOrder ->
                        CourtOrder.courtOrder().withValuesFrom(courtOrder)
                                .withCourtOrderOffences(courtOrder.getCourtOrderOffences().stream()
                                        .map(courtOrderOffence -> CourtOrderOffence.courtOrderOffence()
                                                .withValuesFrom(courtOrderOffence)
                                                .withOffence(getOffenceWithoutNowResults(courtOrderOffence.getOffence()))
                                                .build())
                                        .collect(toList()))
                                .build()).orElse(null))
                .build();
    }

    private List<CourtApplicationCase> getCourtApplicationCasesWithOutNowResults(final CourtApplication courtApplication) {
        return ofNullable(courtApplication.getCourtApplicationCases()).map(Collection::stream).orElseGet(Stream::empty)
                .map(courtApplicationCase -> CourtApplicationCase.courtApplicationCase()
                        .withValuesFrom(courtApplicationCase)
                        .withOffences(ofNullable(courtApplicationCase.getOffences()).map(Collection::stream).orElseGet(Stream::empty)
                                .map(offence -> getOffenceWithoutNowResults(offence))
                                .collect(Collectors.collectingAndThen(toList(), list -> list.isEmpty() ? null : list)))
                        .build())
                .collect(Collectors.collectingAndThen(toList(), list -> list.isEmpty() ? null : list));
    }

    private Offence getOffenceWithoutNowResults(final Offence offence) {
        return Offence.offence().withValuesFrom(offence)
                .withJudicialResults(getNonNowsResults(offence.getJudicialResults()))
                .build();
    }

    private <T> UnaryOperator<List<T>> getListOrNull() {
        return list -> list.isEmpty() ? null : list;
    }

    private List<ProsecutionCase> getUpdatedProsecutionCases(final Hearing hearingFromPayload, final Hearing hearingFromDatabase, final LocalDate hearingDay) {
        return ofNullable(hearingFromPayload.getProsecutionCases()).map(Collection::stream).orElseGet(Stream::empty)
                .map(prosecutionCaseFromPayload -> getUpdatedProsecutionCase(prosecutionCaseFromPayload, hearingFromDatabase, hearingDay))
                .collect(collectingAndThen(Collectors.toList(), getListOrNull()));
    }

    private ProsecutionCase getUpdatedProsecutionCase(final ProsecutionCase prosecutionCaseFromPayload, final Hearing hearingFromDatabase, final LocalDate hearingDay) {
        final Optional<ProsecutionCase> optionalResultedCase = ofNullable(hearingFromDatabase.getProsecutionCases()).map(Collection::stream).orElseGet(Stream::empty)
                .filter(resultedCase -> resultedCase.getId().equals(prosecutionCaseFromPayload.getId()))
                .findFirst();
        if (optionalResultedCase.isPresent()) {
            final ProsecutionCase originalProsecutionCase = optionalResultedCase.get();
            return ProsecutionCase.prosecutionCase().withValuesFrom(prosecutionCaseFromPayload)
                    .withDefendants(getUpdatedDefendants(prosecutionCaseFromPayload, originalProsecutionCase, hearingDay))
                    .withCpsOrganisation(originalProsecutionCase.getCpsOrganisation())
                    .withCpsOrganisationId(originalProsecutionCase.getCpsOrganisationId())
                    .withCaseStatus(originalProsecutionCase.getCaseStatus())
                    .build();
        } else {
            return prosecutionCaseFromPayload;
        }
    }

    private List<Defendant> getUpdatedDefendants(final ProsecutionCase prosecutionCaseFromPayload, final ProsecutionCase prosecutionCaseFromDatabase, final LocalDate hearingDay) {
        return prosecutionCaseFromPayload.getDefendants().stream()
                .map(defendant -> getUpdatedDefendant(defendant, prosecutionCaseFromDatabase.getDefendants(), hearingDay))
                .collect(toList());
    }

    private Defendant getUpdatedDefendant(final Defendant defendantFromPayload, final List<Defendant> defendantsFromDatabase, final LocalDate hearingDay) {

        final Optional<Defendant> defendantFromDatabaseOptional = defendantsFromDatabase.stream()
                .filter(defendant -> defendant.getId().equals(defendantFromPayload.getId()))
                .findFirst();

        if (!defendantFromDatabaseOptional.isPresent()) {
            return defendantFromPayload;
        }

        final Defendant defendantFromDatabase = defendantFromDatabaseOptional.get();
        return Defendant.defendant()
                .withOffences(getUpdatedOffences(defendantFromPayload, defendantFromDatabase, hearingDay))
                .withCpsDefendantId(defendantFromPayload.getCpsDefendantId())
                .withPersonDefendant(defendantFromPayload.getPersonDefendant())
                .withLegalEntityDefendant(defendantFromPayload.getLegalEntityDefendant())
                .withAssociatedPersons(defendantFromPayload.getAssociatedPersons())
                .withId(defendantFromPayload.getId())
                .withMasterDefendantId(defendantFromPayload.getMasterDefendantId())
                .withCourtProceedingsInitiated(defendantFromPayload.getCourtProceedingsInitiated())
                .withLegalAidStatus(defendantFromDatabase.getLegalAidStatus())
                .withMitigation(defendantFromPayload.getMitigation())
                .withMitigationWelsh(defendantFromPayload.getMitigationWelsh())
                .withNumberOfPreviousConvictionsCited(defendantFromPayload.getNumberOfPreviousConvictionsCited())
                .withProsecutionAuthorityReference(defendantFromPayload.getProsecutionAuthorityReference())
                .withProsecutionCaseId(defendantFromPayload.getProsecutionCaseId())
                .withWitnessStatement(defendantFromPayload.getWitnessStatement())
                .withWitnessStatementWelsh(defendantFromPayload.getWitnessStatementWelsh())
                .withDefenceOrganisation(defendantFromPayload.getDefenceOrganisation())
                .withPncId(defendantFromPayload.getPncId())
                .withAliases(defendantFromPayload.getAliases())
                .withIsYouth(defendantFromPayload.getIsYouth())
                .withCroNumber(defendantFromPayload.getCroNumber())
                .withDefendantCaseJudicialResults(getNonNowsResults(defendantFromPayload.getDefendantCaseJudicialResults()))
                .withAssociatedDefenceOrganisation(defendantFromPayload.getAssociatedDefenceOrganisation())
                .withAssociationLockedByRepOrder(defendantFromPayload.getAssociationLockedByRepOrder())
                .build();
    }

    private List<Offence> getUpdatedOffences(final Defendant defendantFromPayload, final Defendant defendantFromDatabase, final LocalDate hearingDay) {

        return defendantFromPayload.getOffences().stream()
                .map(offence -> getUpdatedOffence(offence, defendantFromDatabase.getOffences(), hearingDay))
                .collect(toList());
    }

    private Offence getUpdatedOffence(final Offence offenceFromPayload, final List<Offence> offencesFromDatabase, final LocalDate hearingDay) {

        final Optional<Offence> offenceFromDatabaseOptional = offencesFromDatabase.stream()
                .filter(offence -> offence.getId().equals(offenceFromPayload.getId()))
                .findFirst();

        if (!offenceFromDatabaseOptional.isPresent()) {
            return offenceFromPayload;
        }

        final Offence offenceFromDatabase = offenceFromDatabaseOptional.get();

        List<JudicialResult> resultsToBeAdded = null;

        if (isNotEmpty(offenceFromDatabase.getJudicialResults())) {

            resultsToBeAdded = getUpdatedJudicialResults(offenceFromPayload.getJudicialResults(), offenceFromDatabase.getJudicialResults(), hearingDay);

        } else if (isNotEmpty(offenceFromPayload.getJudicialResults())) {

            resultsToBeAdded = new ArrayList<>();
            resultsToBeAdded.addAll(offenceFromPayload.getJudicialResults());
        }
        return Offence.offence().withValuesFrom(offenceFromPayload)
                .withLaaApplnReference(offenceFromDatabase.getLaaApplnReference())
                .withJudicialResults(getNonNowsResults(resultsToBeAdded))
                .build();
    }

    /**
     * Remove judicial results from the view store and add new judicial results from the payload for
     * the given day.
     *
     * @param judicialResultsFromPayload  judicial results from the payload.
     * @param judicialResultsFromDatabase judicial results from the view store.
     * @param hearingDay                  for newer events the hearingDay will not be null and for
     *                                    older events the hearingDay will be null. The below
     *                                    filtering logic will be applicable only for newer events.
     *                                    For older events, the entire list of judicial results from
     *                                    the payload will be stored in view store.
     * @return
     */
    private List<JudicialResult> getUpdatedJudicialResults(final List<JudicialResult> judicialResultsFromPayload, final List<JudicialResult> judicialResultsFromDatabase, final LocalDate hearingDay) {

        if (isNull(hearingDay)) {
            return judicialResultsFromPayload;
        }

        final List<JudicialResult> existingResultsFromOtherDays = judicialResultsFromDatabase.stream()
                .filter(judicialResult -> !hearingDay.isEqual(judicialResult.getOrderedDate()))
                .collect(toList());

        if (isNotEmpty(judicialResultsFromPayload)) {
            existingResultsFromOtherDays.addAll(judicialResultsFromPayload);
        }

        return isNotEmpty(existingResultsFromOtherDays) ? existingResultsFromOtherDays : null;

    }

    /**
     * Remove defendant judicial results from the view store and add new defendant judicial results
     * from the payload for the given day.
     *
     * @param resultsFromPayload  defendant judicial results from the payload.
     * @param resultsFromDatabase defendant judicial results from the view store.
     * @param hearingDay          for newer events the hearingDay will not be null and for older
     *                            events the hearingDay will be null. The below filtering logic will
     *                            be applicable only for newer events. For older events, the entire
     *                            list of judicial results from the payload will be stored in view
     *                            store.
     * @return
     */
    private List<DefendantJudicialResult> getUpdatedDefendantJudicialResults(final List<DefendantJudicialResult> resultsFromPayload, final List<DefendantJudicialResult> resultsFromDatabase, final LocalDate hearingDay) {

        if (isNull(hearingDay)) {
            return resultsFromPayload;
        }

        final List<DefendantJudicialResult> existingResultsFromOtherDays = resultsFromDatabase.stream()
                .filter(defendantJudicialResult -> !hearingDay.isEqual(defendantJudicialResult.getJudicialResult().getOrderedDate()))
                .collect(toList());

        if (isNotEmpty(resultsFromPayload)) {
            existingResultsFromOtherDays.addAll(resultsFromPayload);
        }

        return isNotEmpty(existingResultsFromOtherDays) ? existingResultsFromOtherDays : null;

    }

    private List<JudicialResult> getNonNowsResults(final List<JudicialResult> judicialResults) {
        if (isNull(judicialResults) || judicialResults.isEmpty()) {
            return judicialResults;
        }

        return judicialResults.stream()
                .filter(Objects::nonNull)
                .filter(jr -> !Boolean.TRUE.equals(jr.getPublishedForNows()))
                .collect(toList());
    }

    private List<DefendantJudicialResult> getNonNowsDefendantJudicialResult(final List<DefendantJudicialResult> judicialResults) {
        if (isNull(judicialResults) || judicialResults.isEmpty()) {
            return judicialResults;
        }

        return judicialResults.stream()
                .filter(Objects::nonNull)
                .filter(jr -> !Boolean.TRUE.equals(jr.getJudicialResult().getPublishedForNows()))
                .collect(collectingAndThen(toList(), l -> l.isEmpty() ? null : l));
    }
}