package uk.gov.moj.cpp.prosecutioncase.event.listener;

import static java.util.Objects.nonNull;
import static java.util.stream.Collectors.toList;
import static uk.gov.justice.services.core.annotation.Component.EVENT_LISTENER;

import uk.gov.justice.core.courts.Hearing;
import uk.gov.justice.core.courts.HearingConfirmedCaseStatusUpdated;
import uk.gov.justice.core.courts.ProsecutionCase;
import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.common.converter.ObjectToJsonObjectConverter;
import uk.gov.justice.services.common.converter.StringToJsonObjectConverter;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.prosecutioncase.persistence.entity.CaseDefendantHearingEntity;
import uk.gov.moj.cpp.prosecutioncase.persistence.entity.HearingEntity;
import uk.gov.moj.cpp.prosecutioncase.persistence.entity.ProsecutionCaseEntity;
import uk.gov.moj.cpp.prosecutioncase.persistence.repository.CaseDefendantHearingRepository;
import uk.gov.moj.cpp.prosecutioncase.persistence.repository.HearingRepository;
import uk.gov.moj.cpp.prosecutioncase.persistence.repository.ProsecutionCaseRepository;

import java.util.List;

import javax.inject.Inject;
import javax.json.JsonObject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ServiceComponent(EVENT_LISTENER)
public class HearingConfirmedCaseUpdatedEventListener {
    private static final Logger LOGGER = LoggerFactory.getLogger(HearingConfirmedCaseUpdatedEventListener.class.getName());

    @Inject
    private JsonObjectToObjectConverter jsonObjectToObjectConverter;

    @Inject
    private StringToJsonObjectConverter stringToJsonObjectConverter;

    @Inject
    private ObjectToJsonObjectConverter objectToJsonObjectConverter;

    @Inject
    private ProsecutionCaseRepository prosecutionCaseRepository;

    @Inject
    private HearingRepository hearingRepository;

    @Inject
    private CaseDefendantHearingRepository caseDefendantHearingRepository;

    @Handles("progression.event.hearing-confirmed-case-status-updated")
    public void updateProsecutionCaseStatus(final JsonEnvelope event) {
        LOGGER.debug("progression.event.hearing-confirmed-case-status-updated {}", event.toObfuscatedDebugString());

        final HearingConfirmedCaseStatusUpdated caseStatusUpdated = jsonObjectToObjectConverter.convert(event.payloadAsJsonObject(), HearingConfirmedCaseStatusUpdated.class);
        final ProsecutionCaseEntity prosecutionCaseEntity = prosecutionCaseRepository.findByCaseId(caseStatusUpdated.getProsecutionCase().getId());
        final JsonObject prosecutionCaseJson = stringToJsonObjectConverter.convert(prosecutionCaseEntity.getPayload());
        final ProsecutionCase persistentProsecutionCase = jsonObjectToObjectConverter.convert(prosecutionCaseJson, ProsecutionCase.class);
        final ProsecutionCase updatedProsecutionCase = updateProsecutionCaseWithCaseStatus(persistentProsecutionCase, caseStatusUpdated.getCaseStatus());

        final ProsecutionCaseEntity updatedCaseEntity = new ProsecutionCaseEntity();
        updatedCaseEntity.setCaseId(updatedProsecutionCase.getId());
        if (nonNull(updatedProsecutionCase.getGroupId())) {
            updatedCaseEntity.setGroupId(updatedProsecutionCase.getGroupId());
        }
        updatedCaseEntity.setPayload(objectToJsonObjectConverter.convert(updatedProsecutionCase).toString());

        prosecutionCaseRepository.save(updatedCaseEntity);

        final List<CaseDefendantHearingEntity> caseDefendantHearingEntities =
                caseDefendantHearingRepository.findByCaseId(updatedProsecutionCase.getId());
        caseDefendantHearingEntities.forEach(caseDefendantHearingEntity -> {
            final HearingEntity originalHearingEntity = caseDefendantHearingEntity.getHearing();
            final JsonObject hearingJson = stringToJsonObjectConverter.convert(originalHearingEntity.getPayload());
            final Hearing originalHearing = jsonObjectToObjectConverter.convert(hearingJson, Hearing.class);
            if (nonNull(originalHearing.getHasSharedResults()) && !originalHearing.getHasSharedResults()) {
                final Hearing updatedNonResultedHearing = getUpdatedHearingForNonResulted(originalHearing, updatedProsecutionCase);
                originalHearingEntity.setPayload(objectToJsonObjectConverter.convert(updatedNonResultedHearing).toString());
                hearingRepository.save(originalHearingEntity);
            }
        });
    }

    private ProsecutionCase updateProsecutionCaseWithCaseStatus(final ProsecutionCase prosecutionCase, final String caseStatus) {
        return ProsecutionCase.prosecutionCase()
                .withValuesFrom(prosecutionCase)
                .withCaseStatus(caseStatus)
                .withTrialReceiptType(prosecutionCase.getTrialReceiptType())
                .build();
    }

    private Hearing getUpdatedHearingForNonResulted(final Hearing originalHearing, final ProsecutionCase prosecutionCase) {
        return Hearing.hearing()
                .withIsBoxHearing(originalHearing.getIsBoxHearing())
                .withId(originalHearing.getId())
                .withProsecutionCases(getUpdatedProsecutionCasesForNonResultedHearing(originalHearing, prosecutionCase))
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
                                                                                  final ProsecutionCase prosecutionCase) {
        return originalHearing.getProsecutionCases().stream()
                .map(originalHearingProsecutionCase ->
                        getUpdatedProsecutionCaseForNonResultedHearing(originalHearingProsecutionCase, prosecutionCase)
                ).collect(toList());
    }

    private ProsecutionCase getUpdatedProsecutionCaseForNonResultedHearing(final ProsecutionCase originalHearingProsecutionCase, final ProsecutionCase prosecutionCase) {
        return originalHearingProsecutionCase.getId().equals(prosecutionCase.getId()) ? ProsecutionCase.prosecutionCase()
                .withPoliceOfficerInCase(originalHearingProsecutionCase.getPoliceOfficerInCase())
                .withProsecutionCaseIdentifier(originalHearingProsecutionCase.getProsecutionCaseIdentifier())
                .withId(originalHearingProsecutionCase.getId())
                .withDefendants(originalHearingProsecutionCase.getDefendants())
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
                .withCaseStatus(prosecutionCase.getCaseStatus())
                .withTrialReceiptType(originalHearingProsecutionCase.getTrialReceiptType())
                .build()
                : originalHearingProsecutionCase;
    }
}
