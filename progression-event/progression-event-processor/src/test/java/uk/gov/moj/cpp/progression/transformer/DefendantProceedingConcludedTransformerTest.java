package uk.gov.moj.cpp.progression.transformer;

import static java.util.Collections.singletonList;
import static java.util.UUID.randomUUID;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import uk.gov.justice.core.courts.CourtApplication;
import uk.gov.justice.core.courts.CourtApplicationCase;
import uk.gov.justice.core.courts.CourtApplicationParty;
import uk.gov.justice.core.courts.Defendant;
import uk.gov.justice.core.courts.JudicialResult;
import uk.gov.justice.core.courts.JudicialResultCategory;
import uk.gov.justice.core.courts.Offence;
import uk.gov.justice.core.courts.ReportingRestriction;
import uk.gov.justice.progression.courts.api.ProsecutionConcludedForLAA;
import uk.gov.justice.progression.courts.exract.OffenceSummary;
import uk.gov.justice.progression.courts.exract.ProsecutionConcluded;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class DefendantProceedingConcludedTransformerTest {

    @InjectMocks
    private DefendantProceedingConcludedTransformer defendantProceedingConcludedTransformer;

    @Test
    public void shouldTransformToLAAPayload() {
        final ProsecutionConcludedForLAA prosecutionConcludedRequest = defendantProceedingConcludedTransformer.getProsecutionConcludedRequest(buildDefendant(), randomUUID(), randomUUID());
        assertThat(prosecutionConcludedRequest.getProsecutionConcluded().size(), is(1));
    }

    @Test
    public void shouldTransformToLAAPayloadForApplication() {
        final CourtApplication courtApplication = buildApplication();
        final UUID hearingId = randomUUID();
        final ProsecutionConcludedForLAA prosecutionConcludedRequest = defendantProceedingConcludedTransformer.getApplicationConcludedRequest(courtApplication, hearingId);
        assertThat(prosecutionConcludedRequest.getProsecutionConcluded().size(), is(1));
        assertThat(prosecutionConcludedRequest.getProsecutionConcluded().get(0).getIsConcluded(), is(courtApplication.getProceedingsConcluded()));
        assertThat(prosecutionConcludedRequest.getProsecutionConcluded().get(0).getApplicationConcluded().getApplicationId(), is(courtApplication.getId()));
        assertThat(prosecutionConcludedRequest.getProsecutionConcluded().get(0).getApplicationConcluded().getApplicationResultCode(), is(courtApplication.getApplicationResultCodeForLaa()));
        assertThat(prosecutionConcludedRequest.getProsecutionConcluded().get(0).getApplicationConcluded().getSubjectId(), is(courtApplication.getSubject().getId()));
        assertThat(prosecutionConcludedRequest.getProsecutionConcluded().get(0).getHearingIdWhereChangeOccurred(), is(hearingId));
        assertThat(prosecutionConcludedRequest.getProsecutionConcluded().get(0).getOffenceSummary().size(), is(1));
    }

    private CourtApplication buildApplication() {
        return CourtApplication.courtApplication()
                .withId(randomUUID())
                .withApplicationResultCodeForLaa("APP_RESULT_CODE")
                .withProceedingsConcluded(true)
                .withCourtApplicationCases(singletonList(CourtApplicationCase.courtApplicationCase()
                        .withProsecutionCaseId(randomUUID())
                        .withOffences(singletonList(Offence.offence()
                                .withId(randomUUID())
                                .withJudicialResults(singletonList(JudicialResult.judicialResult()
                                        .withCategory(JudicialResultCategory.FINAL)
                                        .withOrderedDate(LocalDate.now())
                                        .build()))
                                .withReportingRestrictions(singletonList(ReportingRestriction.reportingRestriction()
                                        .withId(randomUUID())
                                        .build()))
                                .withProceedingsConcluded(true)
                                .build()))
                        .build())
                )
                .withSubject(CourtApplicationParty.courtApplicationParty()
                        .withId(randomUUID())
                        .build())
                .build();
    }


    private static List<Defendant> buildDefendant() {
        return (Arrays.asList(Defendant.defendant()
                .withId(randomUUID())
                .withOffences(Arrays.asList(Offence.offence()
                        .withId(randomUUID())
                        .withJudicialResults(Arrays.asList(JudicialResult.judicialResult()
                                .withCategory(JudicialResultCategory.FINAL)
                                .withOrderedDate(LocalDate.now())
                                .build()))
                        .withReportingRestrictions(singletonList(ReportingRestriction.reportingRestriction()
                                .withId(randomUUID())
                                .build()))
                        .withProceedingsConcluded(true)
                        .build()))
                .build()));
    }

    private static ProsecutionConcludedForLAA createProsecutionConcludedRequest() {
        return ProsecutionConcludedForLAA.prosecutionConcludedForLAA()
                .withProsecutionConcluded(Arrays.asList(ProsecutionConcluded.prosecutionConcluded()
                        .withDefendantId(randomUUID())
                        .withHearingIdWhereChangeOccurred(randomUUID())
                        .withProsecutionCaseId(randomUUID())
                        .withIsConcluded(true)
                        .withOffenceSummary(Arrays.asList(OffenceSummary.offenceSummary()
                                .withOffenceId(randomUUID())
                                .withProceedingsConcluded(true)
                                .build()))
                        .build()))
                .build();
    }
}
