package uk.gov.moj.cpp.prosecutioncase.event.listener;

import com.google.common.collect.Iterables;
import uk.gov.justice.core.courts.ApplicationStatus;
import uk.gov.justice.core.courts.CourtApplication;
import uk.gov.justice.core.courts.Hearing;
import uk.gov.justice.core.courts.ProsecutionCase;

import java.util.List;
import java.util.UUID;



public class HearingEntityUtil {

    private static final String CASE_STATUS_EJECTED = "EJECTED";


    private HearingEntityUtil () {

    }

    public static Hearing updateHearingWithCase(final Hearing hearing, final UUID caseId) {
        final List<ProsecutionCase> prosecutionCases = hearing.getProsecutionCases();
        final ProsecutionCase origProsecutionCase = Iterables.find(prosecutionCases, pc -> pc.getId()
                .equals(caseId));
        final ProsecutionCase updatedProsecutionCase = updateProsecutionCaseWithEjectStatus(origProsecutionCase);
        prosecutionCases.replaceAll(prosecutionCase -> prosecutionCase.getId()
                .equals(updatedProsecutionCase.getId()) ? updatedProsecutionCase : prosecutionCase);

        return hearing;
    }

    public static Hearing updateHearingWithApplication(final Hearing hearing, final UUID applicationId) {
        final List<CourtApplication> courtApplications = hearing.getCourtApplications();
        final CourtApplication origCourtApplication = Iterables.find(courtApplications, ca -> ca.getId()
                .equals(applicationId));
        final CourtApplication updatedCourtApplication = updateCourtApplicationWithEjectStatus(origCourtApplication);
        courtApplications.replaceAll(courtApplication -> courtApplication.getId()
                .equals(updatedCourtApplication.getId()) ? updatedCourtApplication : courtApplication);
        return hearing;
    }

    private static CourtApplication updateCourtApplicationWithEjectStatus(final CourtApplication courtApplication) {
        return CourtApplication.courtApplication()
                .withId(courtApplication.getId())
                .withType(courtApplication.getType())
                .withApplicant(courtApplication.getApplicant())
                .withApplicationDecisionSoughtByDate(courtApplication.getApplicationDecisionSoughtByDate())
                .withOrderingCourt(courtApplication.getOrderingCourt())
                .withApplicationOutcome(courtApplication.getApplicationOutcome())
                .withApplicationParticulars(courtApplication.getApplicationParticulars())
                .withApplicationReceivedDate(courtApplication.getApplicationReceivedDate())
                .withApplicationReference(courtApplication.getApplicationReference())
                .withApplicationStatus(ApplicationStatus.EJECTED)
                .withRemovalReason(courtApplication.getRemovalReason())
                .withCourtApplicationPayment(courtApplication.getCourtApplicationPayment())
                .withJudicialResults(courtApplication.getJudicialResults())
                .withParentApplicationId(courtApplication.getParentApplicationId())
                .withLinkedCaseId(courtApplication.getLinkedCaseId())
                .withBreachedOrder(courtApplication.getBreachedOrder())
                .withBreachedOrderDate(courtApplication.getBreachedOrderDate())
                .withRespondents(courtApplication.getRespondents())
                .withOutOfTimeReasons(courtApplication.getOutOfTimeReasons())
                .build();
    }

    private static ProsecutionCase updateProsecutionCaseWithEjectStatus(final ProsecutionCase prosecutionCase) {
        return ProsecutionCase.prosecutionCase()
                .withId(prosecutionCase.getId())
                .withProsecutionCaseIdentifier(prosecutionCase.getProsecutionCaseIdentifier())
                .withInitiationCode(prosecutionCase.getInitiationCode())
                .withDefendants(prosecutionCase.getDefendants())
                .withAppealProceedingsPending(prosecutionCase.getAppealProceedingsPending())
                .withBreachProceedingsPending(prosecutionCase.getBreachProceedingsPending())
                .withCaseMarkers(prosecutionCase.getCaseMarkers())
                .withCaseStatus(CASE_STATUS_EJECTED)
                .withOriginatingOrganisation(prosecutionCase.getOriginatingOrganisation())
                .withCpsOrganisation(prosecutionCase.getCpsOrganisation())
                .withIsCpsOrgVerifyError(prosecutionCase.getIsCpsOrgVerifyError())
                .withPoliceOfficerInCase(prosecutionCase.getPoliceOfficerInCase())
                .withRemovalReason(prosecutionCase.getRemovalReason())
                .withStatementOfFacts(prosecutionCase.getStatementOfFacts())
                .withStatementOfFactsWelsh(prosecutionCase.getStatementOfFactsWelsh())
                .build();
    }
}
