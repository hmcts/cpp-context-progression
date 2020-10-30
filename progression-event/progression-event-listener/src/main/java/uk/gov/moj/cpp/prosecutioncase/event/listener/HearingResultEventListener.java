package uk.gov.moj.cpp.prosecutioncase.event.listener;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static java.util.Optional.ofNullable;
import static java.util.stream.Collectors.toList;
import static uk.gov.justice.services.core.annotation.Component.EVENT_LISTENER;

import uk.gov.justice.core.courts.CourtApplication;
import uk.gov.justice.core.courts.Defendant;
import uk.gov.justice.core.courts.Hearing;
import uk.gov.justice.core.courts.HearingListingStatus;
import uk.gov.justice.core.courts.JudicialResult;
import uk.gov.justice.core.courts.Offence;
import uk.gov.justice.core.courts.ProsecutionCase;
import uk.gov.justice.hearing.courts.HearingResulted;
import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.common.converter.ObjectToJsonObjectConverter;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.prosecutioncase.persistence.entity.CaseDefendantHearingEntity;
import uk.gov.moj.cpp.prosecutioncase.persistence.entity.HearingEntity;
import uk.gov.moj.cpp.prosecutioncase.persistence.repository.CaseDefendantHearingRepository;
import uk.gov.moj.cpp.prosecutioncase.persistence.repository.HearingRepository;
import uk.gov.moj.cpp.prosecutioncase.persistence.repository.ProsecutionCaseRepository;

import java.io.StringReader;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonReader;

@SuppressWarnings({"squid:S3655", "squid:S1135","squid:S1612"})
@ServiceComponent(EVENT_LISTENER)
public class HearingResultEventListener {

    @Inject
    private JsonObjectToObjectConverter jsonObjectConverter;

    @Inject
    private ObjectToJsonObjectConverter objectToJsonObjectConverter;

    @Inject
    private HearingRepository hearingRepository;

    @Inject
    private ProsecutionCaseRepository caseRepository;

    @Inject
    private CaseDefendantHearingRepository caseDefendantHearingRepository;


    @Handles("progression.event.hearing-resulted")
    public void updateHearingResult(final JsonEnvelope event) {
        //To save hearing result in current hearing and removing the defendant and case proceeding flag in it.
        final HearingResulted hearingResulted = jsonObjectConverter.convert(event.payloadAsJsonObject(), HearingResulted.class);
        final HearingEntity currentHearingEntity = hearingRepository.findBy(hearingResulted.getHearing().getId());
        final JsonObject currentHearingJson = jsonFromString(currentHearingEntity.getPayload());
        final Hearing originalCurrentHearing = jsonObjectConverter.convert(currentHearingJson, Hearing.class);
        final Hearing updatedHearing = getUpdatedHearingForResulted(hearingResulted.getHearing(), originalCurrentHearing);
        currentHearingEntity.setPayload(objectToJsonObjectConverter.convert(updatedHearing).toString());
        currentHearingEntity.setListingStatus(HearingListingStatus.HEARING_RESULTED);
        hearingRepository.save(currentHearingEntity);

        //TO deal with setting of case and defendant proceeding flag for future hearing.
        if (nonNull(updatedHearing.getProsecutionCases()) && !updatedHearing.getProsecutionCases().isEmpty()) {
            updatedHearing.getProsecutionCases().forEach(prosecutionCase -> {
                final List<CaseDefendantHearingEntity> caseDefendantHearingEntities =
                        caseDefendantHearingRepository.findByCaseId(prosecutionCase.getId());
                caseDefendantHearingEntities.stream().forEach(caseDefendantHearingEntity -> {
                    final HearingEntity originalHearingEntity = caseDefendantHearingEntity.getHearing();
                    final JsonObject hearingJson = jsonFromString(originalHearingEntity.getPayload());
                    final Hearing originalHearing = jsonObjectConverter.convert(hearingJson, Hearing.class);
                    if (nonNull(originalHearing.getHasSharedResults()) && !originalHearing.getHasSharedResults()) {
                        final Hearing updatedNonResultedHearing = getUpdatedHearingForNonResulted(originalHearing, hearingResulted.getHearing());
                        originalHearingEntity.setPayload(objectToJsonObjectConverter.convert(updatedNonResultedHearing).toString());
                        hearingRepository.save(originalHearingEntity);
                    }
                });
            });
        }
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
            return Offence.offence()
                    .withAllocationDecision(originalOffence.getAllocationDecision())
                    .withAquittalDate(originalOffence.getAquittalDate())
                    .withArrestDate(originalOffence.getArrestDate())
                    .withChargeDate(originalOffence.getChargeDate())
                    .withConvictionDate(originalOffence.getConvictionDate())
                    .withCount(originalOffence.getCount())
                    .withCustodyTimeLimit(originalOffence.getCustodyTimeLimit())
                    .withDateOfInformation(originalOffence.getDateOfInformation())
                    .withEndDate(originalOffence.getEndDate())
                    .withId(originalOffence.getId())
                    .withIndicatedPlea(originalOffence.getIndicatedPlea())
                    .withIsDiscontinued(originalOffence.getIsDiscontinued())
                    .withIntroducedAfterInitialProceedings(originalOffence.getIntroducedAfterInitialProceedings())
                    .withJudicialResults(getNonNowsResults(originalOffence.getJudicialResults()))
                    .withLaaApplnReference(originalOffence.getLaaApplnReference())
                    .withModeOfTrial(originalOffence.getModeOfTrial())
                    .withNotifiedPlea(originalOffence.getNotifiedPlea())
                    .withOffenceCode(originalOffence.getOffenceCode())
                    .withOffenceDefinitionId(originalOffence.getOffenceDefinitionId())
                    .withOffenceFacts(originalOffence.getOffenceFacts())
                    .withOffenceLegislation(originalOffence.getOffenceLegislation())
                    .withOffenceLegislationWelsh(originalOffence.getOffenceLegislationWelsh())
                    .withOffenceTitle(originalOffence.getOffenceTitle())
                    .withOffenceTitleWelsh(originalOffence.getOffenceTitleWelsh())
                    .withOrderIndex(originalOffence.getOrderIndex())
                    .withPlea(originalOffence.getPlea())
                    .withStartDate(originalOffence.getStartDate())
                    .withVerdict(originalOffence.getVerdict())
                    .withVictims(originalOffence.getVictims())
                    .withWording(originalOffence.getWording())
                    .withWordingWelsh(originalOffence.getWordingWelsh())
                    .withProceedingsConcluded(resultedOffence.getProceedingsConcluded())
                    .withOffenceDateCode(originalOffence.getOffenceDateCode())
                    .withCommittingCourt(originalOffence.getCommittingCourt())
                    .build();
        } else {
            return originalOffence;
        }
    }

    private Hearing getUpdatedHearingForResulted(final Hearing hearing, final Hearing originalHearing) {

        final Hearing.Builder builder = Hearing.hearing();
        if (nonNull(hearing.getProsecutionCases()) && !hearing.getProsecutionCases().isEmpty()) {
            builder.withProsecutionCases(getUpdatedProsecutionCases(hearing, originalHearing));
        }
        return builder.withIsBoxHearing(hearing.getIsBoxHearing())
                .withId(hearing.getId())
                .withHearingDays(hearing.getHearingDays())
                .withCourtCentre(hearing.getCourtCentre())
                .withJurisdictionType(hearing.getJurisdictionType())
                .withType(hearing.getType())
                .withHearingLanguage(hearing.getHearingLanguage())
                .withCourtApplications(getUpdateCourtApplications(hearing.getCourtApplications()))
                .withReportingRestrictionReason(hearing.getReportingRestrictionReason())
                .withJudiciary(hearing.getJudiciary())
                .withDefendantAttendance(hearing.getDefendantAttendance())
                .withDefendantReferralReasons(hearing.getDefendantReferralReasons())
                .withHasSharedResults(hearing.getHasSharedResults())
                .withDefenceCounsels(hearing.getDefenceCounsels())
                .withProsecutionCounsels(hearing.getProsecutionCounsels())
                .withRespondentCounsels(hearing.getRespondentCounsels())
                .withApplicationPartyCounsels(hearing.getApplicationPartyCounsels())
                .withCrackedIneffectiveTrial(hearing.getCrackedIneffectiveTrial())
                .withReportingRestrictionReason(hearing.getReportingRestrictionReason())
                .withHearingCaseNotes(hearing.getHearingCaseNotes())
                .withCourtApplicationPartyAttendance(hearing.getCourtApplicationPartyAttendance())
                .withCompanyRepresentatives(hearing.getCompanyRepresentatives())
                .withIntermediaries(hearing.getIntermediaries())
                .withIsEffectiveTrial(hearing.getIsEffectiveTrial())
                .build();
    }

    private List<CourtApplication> getUpdateCourtApplications(final List<CourtApplication> courtApplicationList) {
        Optional.ofNullable(courtApplicationList).ifPresent(
                courtApplications -> courtApplications.stream().filter(Objects::nonNull).forEach(HearingResultEventListener::getCourtApplicationJudResultsforNonNows
                )
        );

        return courtApplicationList;
    }

    private static void getCourtApplicationJudResultsforNonNows(CourtApplication courtApplication) {
        ofNullable(courtApplication.getJudicialResults()).ifPresent(
                judicialResults -> {
                    final List<JudicialResult> caJudicialResults = judicialResults.stream().filter(Objects::nonNull).filter(jr -> !jr.getPublishedForNows().equals(Boolean.TRUE)).collect(Collectors.toList());
                    courtApplication.getJudicialResults().clear();
                    courtApplication.getJudicialResults().addAll(caJudicialResults);
                }
        );
    }

    private List<ProsecutionCase> getUpdatedProsecutionCases(final Hearing hearing, final Hearing originalHearing) {

        return hearing.getProsecutionCases().stream().map(prosecutionCase ->
                getUpdatedProsecutionCase(prosecutionCase, originalHearing)
        ).collect(toList());
    }

    private ProsecutionCase getUpdatedProsecutionCase(final ProsecutionCase prosecutionCase, final Hearing originalHearing) {
        final Optional<ProsecutionCase> optionalResultedCase = originalHearing.getProsecutionCases().stream()
                .filter(resultedCase -> resultedCase.getId().equals(prosecutionCase.getId()))
                .findFirst();
        if (optionalResultedCase.isPresent()) {
            final ProsecutionCase originalProsecutionCase = optionalResultedCase.get();
            return ProsecutionCase.prosecutionCase()
                    .withPoliceOfficerInCase(prosecutionCase.getPoliceOfficerInCase())
                    .withProsecutionCaseIdentifier(prosecutionCase.getProsecutionCaseIdentifier())
                    .withId(prosecutionCase.getId())
                    .withDefendants(getUpdatedDefendants(prosecutionCase))
                    .withInitiationCode(prosecutionCase.getInitiationCode())
                    .withOriginatingOrganisation(prosecutionCase.getOriginatingOrganisation())
                    .withCpsOrganisation(prosecutionCase.getCpsOrganisation())
                    .withIsCpsOrgVerifyError(prosecutionCase.getIsCpsOrgVerifyError())
                    .withStatementOfFacts(prosecutionCase.getStatementOfFacts())
                    .withStatementOfFactsWelsh(prosecutionCase.getStatementOfFactsWelsh())
                    .withCaseMarkers(prosecutionCase.getCaseMarkers())
                    .withAppealProceedingsPending(prosecutionCase.getAppealProceedingsPending())
                    .withBreachProceedingsPending(prosecutionCase.getBreachProceedingsPending())
                    .withRemovalReason(prosecutionCase.getRemovalReason())
                    .withCaseStatus(originalProsecutionCase.getCaseStatus())
                    .build();
        } else {
            return prosecutionCase;
        }
    }

    private List<Defendant> getUpdatedDefendants(final ProsecutionCase prosecutionCase) {
        return prosecutionCase.getDefendants().stream().map(defendant ->
                getUpdatedDefendant(defendant)
        ).collect(toList());
    }

    private Defendant getUpdatedDefendant(final Defendant originDefendant) {

        return Defendant.defendant()
                .withOffences(getUpdatedOffences(originDefendant))
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
                .withAssociationLockedByRepOrder(originDefendant.getAssociationLockedByRepOrder())
                .build();
    }

    private List<Offence> getUpdatedOffences(final Defendant defendant) {
        return defendant.getOffences().stream().map(offence ->
                getUpdatedOffence(offence)
        ).collect(toList());
    }

    private Offence getUpdatedOffence(final Offence originOffence) {
        return Offence.offence()
                .withAllocationDecision(originOffence.getAllocationDecision())
                .withAquittalDate(originOffence.getAquittalDate())
                .withArrestDate(originOffence.getArrestDate())
                .withChargeDate(originOffence.getChargeDate())
                .withConvictionDate(originOffence.getConvictionDate())
                .withCount(originOffence.getCount())
                .withCustodyTimeLimit(originOffence.getCustodyTimeLimit())
                .withDateOfInformation(originOffence.getDateOfInformation())
                .withEndDate(originOffence.getEndDate())
                .withId(originOffence.getId())
                .withIndicatedPlea(originOffence.getIndicatedPlea())
                .withIsDiscontinued(originOffence.getIsDiscontinued())
                .withIntroducedAfterInitialProceedings(originOffence.getIntroducedAfterInitialProceedings())
                .withJudicialResults(getNonNowsResults(originOffence.getJudicialResults()))
                .withLaaApplnReference(originOffence.getLaaApplnReference())
                .withModeOfTrial(originOffence.getModeOfTrial())
                .withNotifiedPlea(originOffence.getNotifiedPlea())
                .withOffenceCode(originOffence.getOffenceCode())
                .withOffenceDefinitionId(originOffence.getOffenceDefinitionId())
                .withOffenceFacts(originOffence.getOffenceFacts())
                .withOffenceLegislation(originOffence.getOffenceLegislation())
                .withOffenceLegislationWelsh(originOffence.getOffenceLegislationWelsh())
                .withOffenceTitle(originOffence.getOffenceTitle())
                .withOffenceTitleWelsh(originOffence.getOffenceTitleWelsh())
                .withOrderIndex(originOffence.getOrderIndex())
                .withPlea(originOffence.getPlea())
                .withStartDate(originOffence.getStartDate())
                .withVerdict(originOffence.getVerdict())
                .withVictims(originOffence.getVictims())
                .withWording(originOffence.getWording())
                .withWordingWelsh(originOffence.getWordingWelsh())
                .withOffenceDateCode(originOffence.getOffenceDateCode())
                .withCommittingCourt(originOffence.getCommittingCourt())
                .build();
    }

    private static JsonObject jsonFromString(String jsonObjectStr) {

        final JsonReader jsonReader = Json.createReader(new StringReader(jsonObjectStr));
        final JsonObject object = jsonReader.readObject();
        jsonReader.close();

        return object;
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
}
