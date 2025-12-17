package uk.gov.moj.cpp.prosecutioncase.event.listener;

import static java.util.Objects.nonNull;
import static org.apache.commons.collections.CollectionUtils.isNotEmpty;

import uk.gov.justice.core.courts.ApplicationStatus;
import uk.gov.justice.core.courts.CourtApplication;
import uk.gov.justice.core.courts.Hearing;
import uk.gov.justice.core.courts.ProsecutionCase;

import java.util.List;
import java.util.UUID;

import com.google.common.collect.Iterables;


public class HearingEntityUtil {

    private static final String CASE_STATUS_EJECTED = "EJECTED";


    private HearingEntityUtil() {

    }

    public static Hearing updateHearingWithCase(final Hearing hearing, final UUID caseId) {
        final List<ProsecutionCase> prosecutionCases = hearing.getProsecutionCases();

        if (isNotEmpty(prosecutionCases)) {
            final ProsecutionCase origProsecutionCase = prosecutionCases.stream()
                    .filter(prosecutionCase -> prosecutionCase.getId().equals(caseId))
                    .findFirst().orElse(null);
            if (nonNull(origProsecutionCase)) {
                final ProsecutionCase updatedProsecutionCase = updateProsecutionCaseWithEjectStatus(origProsecutionCase);
                prosecutionCases.replaceAll(prosecutionCase -> prosecutionCase.getId()
                        .equals(updatedProsecutionCase.getId()) ? updatedProsecutionCase : prosecutionCase);
            }
        }
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
                .withValuesFrom(courtApplication)
                .withApplicationStatus(ApplicationStatus.EJECTED)
                .build();
    }

    private static ProsecutionCase updateProsecutionCaseWithEjectStatus(final ProsecutionCase prosecutionCase) {
        return ProsecutionCase.prosecutionCase()
                .withValuesFrom(prosecutionCase)
                .withCaseStatus(CASE_STATUS_EJECTED)
                .build();
    }
}
