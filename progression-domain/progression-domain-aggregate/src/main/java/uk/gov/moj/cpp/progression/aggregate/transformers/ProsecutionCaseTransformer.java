package uk.gov.moj.cpp.progression.aggregate.transformers;

import uk.gov.justice.core.courts.Defendant;
import uk.gov.justice.core.courts.ProsecutionCase;

import java.util.List;

public class ProsecutionCaseTransformer {

    private ProsecutionCaseTransformer(){
        //no init
    }

    public static ProsecutionCase toUpdatedProsecutionCase(final ProsecutionCase prosecutionCase,
                                                           final List<Defendant> defendants,
                                                           final String caseStatus){
        return  ProsecutionCase.prosecutionCase()
                .withPoliceOfficerInCase(prosecutionCase.getPoliceOfficerInCase())
                .withProsecutionCaseIdentifier(prosecutionCase.getProsecutionCaseIdentifier())
                .withId(prosecutionCase.getId())
                .withDefendants(defendants)
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
                .withCaseStatus(caseStatus)
                .withTrialReceiptType(prosecutionCase.getTrialReceiptType())
                .withMigrationSourceSystem(prosecutionCase.getMigrationSourceSystem())
                .build();
    }
}
