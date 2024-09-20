package uk.gov.moj.cpp.progression.transformer;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

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
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class DefendantProceedingConcludedTransformerTest {

    @InjectMocks
    private DefendantProceedingConcludedTransformer defendantProceedingConcludedTransformer;

    @Test
    public void shouldTransformToLAAPayload(){
        final ProsecutionConcludedForLAA prosecutionConcludedRequest = defendantProceedingConcludedTransformer.getProsecutionConcludedRequest(buildDefendant(), UUID.randomUUID(), UUID.randomUUID());
        assertThat(prosecutionConcludedRequest.getProsecutionConcluded().size(), is(1));
    }

    private static List<Defendant> buildDefendant() {
        return (Arrays.asList(Defendant.defendant()
                .withId(UUID.randomUUID())
                .withOffences(Arrays.asList(Offence.offence()
                        .withId(UUID.randomUUID())
                        .withJudicialResults(Arrays.asList(JudicialResult.judicialResult()
                                .withCategory(JudicialResultCategory.FINAL)
                                .withOrderedDate(LocalDate.now())
                                .build()))
                        .withReportingRestrictions(Collections.singletonList(ReportingRestriction.reportingRestriction()
                                .withId(UUID.randomUUID())
                                .build()))
                        .withProceedingsConcluded(true)
                        .build()))
                .build()));
    }

    private static ProsecutionConcludedForLAA createProsecutionConcludedRequest(){
        return ProsecutionConcludedForLAA.prosecutionConcludedForLAA()
                .withProsecutionConcluded(Arrays.asList(ProsecutionConcluded.prosecutionConcluded()
                        .withDefendantId(UUID.randomUUID())
                        .withHearingIdWhereChangeOccurred(UUID.randomUUID())
                        .withProsecutionCaseId(UUID.randomUUID())
                        .withIsConcluded(true)
                        .withOffenceSummary(Arrays.asList(OffenceSummary.offenceSummary()
                                .withOffenceId(UUID.randomUUID())
                                .withProceedingsConcluded(true)
                                .build()))
                        .build()))
                .build();
    }
}
