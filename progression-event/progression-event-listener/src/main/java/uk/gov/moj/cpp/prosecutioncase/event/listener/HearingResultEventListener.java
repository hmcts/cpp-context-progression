package uk.gov.moj.cpp.prosecutioncase.event.listener;

import static java.lang.Boolean.FALSE;
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
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import javax.inject.Inject;
import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonReader;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

    private static final Logger LOGGER = LoggerFactory.getLogger(HearingResultEventListener.class);

    @Handles("progression.event.hearing-resulted")
    public void updateHearingResult(final JsonEnvelope event) {
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("received event progression.event.hearing-resulted {} ", event.toObfuscatedDebugString());
        }

        //To save hearing result in current hearing and removing the defendant and case proceeding flag in it.
        final HearingResulted hearingResulted = jsonObjectConverter.convert(event.payloadAsJsonObject(), HearingResulted.class);
        final HearingEntity currentHearingEntity = hearingRepository.findBy(hearingResulted.getHearing().getId());
        final JsonObject currentHearingJson = jsonFromString(currentHearingEntity.getPayload());
        final Hearing originalCurrentHearing = jsonObjectConverter.convert(currentHearingJson, Hearing.class);
        final Hearing updatedHearing = getUpdatedHearingForResulted(hearingResulted.getHearing(), originalCurrentHearing);
        final String resultedHearingPayload = objectToJsonObjectConverter.convert(updatedHearing).toString();
        currentHearingEntity.setPayload(resultedHearingPayload);
        currentHearingEntity.setListingStatus(HearingListingStatus.HEARING_RESULTED);
        hearingRepository.save(currentHearingEntity);
        LOGGER.info("Hearing: {} has been updated with listing status {}", currentHearingEntity.getHearingId(), HearingListingStatus.HEARING_RESULTED);

        //TO deal with setting of case and defendant proceeding flag for future hearing.
        final List<ProsecutionCase> prosecutionCases = ofNullable(updatedHearing.getProsecutionCases()).orElse(new ArrayList<>());
        for (final ProsecutionCase prosecutionCase: prosecutionCases) {
            final List<CaseDefendantHearingEntity> caseDefendantEntities = caseDefendantHearingRepository.findByCaseId(prosecutionCase.getId());
            for (final CaseDefendantHearingEntity caseDefendantHearingEntity : caseDefendantEntities) {
                final HearingEntity originalHearingEntity = caseDefendantHearingEntity.getHearing();
                final JsonObject hearingJson = jsonFromString(originalHearingEntity.getPayload());
                final Hearing originalHearing = jsonObjectConverter.convert(hearingJson, Hearing.class);
                final Boolean hasSharedResults = originalHearing.getHasSharedResults();
                if (FALSE.equals(hasSharedResults)) {
                    final Hearing updatedNonResultedHearing = getUpdatedHearingForNonResulted(originalHearing, hearingResulted.getHearing());
                    final String payload = objectToJsonObjectConverter.convert(updatedNonResultedHearing).toString();
                    originalHearingEntity.setPayload(payload);
                    hearingRepository.save(originalHearingEntity);
                    LOGGER.info("Original hearing without shared results: {} has been updated.", originalHearingEntity.getHearingId());
                }
            }
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
                    .withReportingRestrictions(originalOffence.getReportingRestrictions())
                    .build();
        } else {
            return originalOffence;
        }
    }

    private Hearing getUpdatedHearingForResulted(final Hearing hearingFromPayload, final Hearing hearingFromDatabase) {

        final Hearing.Builder builder = Hearing.hearing();
        if (nonNull(hearingFromPayload.getProsecutionCases()) && !hearingFromPayload.getProsecutionCases().isEmpty()) {
            builder.withProsecutionCases(getUpdatedProsecutionCases(hearingFromPayload, hearingFromDatabase));
        }
        return builder.withIsBoxHearing(hearingFromPayload.getIsBoxHearing())
                .withId(hearingFromPayload.getId())
                .withHearingDays(hearingFromPayload.getHearingDays())
                .withCourtCentre(hearingFromPayload.getCourtCentre())
                .withJurisdictionType(hearingFromPayload.getJurisdictionType())
                .withType(hearingFromPayload.getType())
                .withHearingLanguage(hearingFromPayload.getHearingLanguage())
                .withCourtApplications(getUpdateCourtApplications(hearingFromPayload.getCourtApplications()))
                .withReportingRestrictionReason(hearingFromPayload.getReportingRestrictionReason())
                .withJudiciary(hearingFromPayload.getJudiciary())
                .withDefendantJudicialResults(hearingFromPayload.getDefendantJudicialResults())
                .withDefendantAttendance(hearingFromPayload.getDefendantAttendance())
                .withDefendantReferralReasons(hearingFromPayload.getDefendantReferralReasons())
                .withHasSharedResults(hearingFromPayload.getHasSharedResults())
                .withDefenceCounsels(hearingFromPayload.getDefenceCounsels())
                .withProsecutionCounsels(hearingFromPayload.getProsecutionCounsels())
                .withRespondentCounsels(hearingFromPayload.getRespondentCounsels())
                .withApplicationPartyCounsels(hearingFromPayload.getApplicationPartyCounsels())
                .withCrackedIneffectiveTrial(hearingFromPayload.getCrackedIneffectiveTrial())
                .withReportingRestrictionReason(hearingFromPayload.getReportingRestrictionReason())
                .withHearingCaseNotes(hearingFromPayload.getHearingCaseNotes())
                .withCourtApplicationPartyAttendance(hearingFromPayload.getCourtApplicationPartyAttendance())
                .withCompanyRepresentatives(hearingFromPayload.getCompanyRepresentatives())
                .withIntermediaries(hearingFromPayload.getIntermediaries())
                .withIsEffectiveTrial(hearingFromPayload.getIsEffectiveTrial())
                .build();
    }

    private List<CourtApplication> getUpdateCourtApplications(final List<CourtApplication> courtApplicationList) {
        ofNullable(courtApplicationList).ifPresent(
                courtApplications -> courtApplications.stream().filter(Objects::nonNull).forEach(HearingResultEventListener::getCourtApplicationJudResultsforNonNows
                )
        );

        return courtApplicationList;
    }

    private static void getCourtApplicationJudResultsforNonNows(CourtApplication courtApplication) {
        ofNullable(courtApplication.getJudicialResults()).ifPresent(
                judicialResults -> {
                    final List<JudicialResult> caJudicialResults = judicialResults.stream().filter(Objects::nonNull).filter(jr -> !jr.getPublishedForNows().equals(Boolean.TRUE)).collect(toList());
                    courtApplication.getJudicialResults().clear();
                    courtApplication.getJudicialResults().addAll(caJudicialResults);
                }
        );
    }

    private List<ProsecutionCase> getUpdatedProsecutionCases(final Hearing hearingFromPayload, final Hearing hearingFromDatabase) {

        return hearingFromPayload.getProsecutionCases().stream().map(prosecutionCaseFromPayload ->
                getUpdatedProsecutionCase(prosecutionCaseFromPayload, hearingFromDatabase)
        ).collect(toList());
    }

    private ProsecutionCase getUpdatedProsecutionCase(final ProsecutionCase prosecutionCaseFromPayload, final Hearing hearingFromDatabase) {
        final Optional<ProsecutionCase> optionalResultedCase = hearingFromDatabase.getProsecutionCases().stream()
                .filter(resultedCase -> resultedCase.getId().equals(prosecutionCaseFromPayload.getId()))
                .findFirst();
        if (optionalResultedCase.isPresent()) {
            final ProsecutionCase originalProsecutionCase = optionalResultedCase.get();
            return ProsecutionCase.prosecutionCase()
                    .withPoliceOfficerInCase(prosecutionCaseFromPayload.getPoliceOfficerInCase())
                    .withProsecutionCaseIdentifier(prosecutionCaseFromPayload.getProsecutionCaseIdentifier())
                    .withId(prosecutionCaseFromPayload.getId())
                    .withDefendants(getUpdatedDefendants(prosecutionCaseFromPayload))
                    .withInitiationCode(prosecutionCaseFromPayload.getInitiationCode())
                    .withOriginatingOrganisation(prosecutionCaseFromPayload.getOriginatingOrganisation())
                    .withCpsOrganisation(prosecutionCaseFromPayload.getCpsOrganisation())
                    .withIsCpsOrgVerifyError(prosecutionCaseFromPayload.getIsCpsOrgVerifyError())
                    .withStatementOfFacts(prosecutionCaseFromPayload.getStatementOfFacts())
                    .withStatementOfFactsWelsh(prosecutionCaseFromPayload.getStatementOfFactsWelsh())
                    .withCaseMarkers(prosecutionCaseFromPayload.getCaseMarkers())
                    .withAppealProceedingsPending(prosecutionCaseFromPayload.getAppealProceedingsPending())
                    .withBreachProceedingsPending(prosecutionCaseFromPayload.getBreachProceedingsPending())
                    .withRemovalReason(prosecutionCaseFromPayload.getRemovalReason())
                    .withCaseStatus(originalProsecutionCase.getCaseStatus())
                    .build();
        } else {
            return prosecutionCaseFromPayload;
        }
    }

    private List<Defendant> getUpdatedDefendants(final ProsecutionCase prosecutionCaseFromPayload) {
        return prosecutionCaseFromPayload.getDefendants().stream().map(defendant ->
                getUpdatedDefendant(defendant)
        ).collect(toList());
    }

    private Defendant getUpdatedDefendant(final Defendant defendantFromPayload) {

        return Defendant.defendant()
                .withOffences(getUpdatedOffences(defendantFromPayload))
                .withPersonDefendant(defendantFromPayload.getPersonDefendant())
                .withLegalEntityDefendant(defendantFromPayload.getLegalEntityDefendant())
                .withAssociatedPersons(defendantFromPayload.getAssociatedPersons())
                .withId(defendantFromPayload.getId())
                .withMasterDefendantId(defendantFromPayload.getMasterDefendantId())
                .withCourtProceedingsInitiated(defendantFromPayload.getCourtProceedingsInitiated())
                .withLegalAidStatus(defendantFromPayload.getLegalAidStatus())
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
                .withReportingRestrictions(originOffence.getReportingRestrictions())
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
