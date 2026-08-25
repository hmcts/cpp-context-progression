package uk.gov.moj.cpp.progression.aggregate.helper;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static java.util.UUID.randomUUID;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static uk.gov.justice.core.courts.CourtApplication.courtApplication;
import static uk.gov.justice.core.courts.CourtApplicationCase.courtApplicationCase;
import static uk.gov.justice.core.courts.CourtApplicationType.courtApplicationType;
import static uk.gov.justice.core.courts.JudicialResult.judicialResult;
import static uk.gov.justice.core.courts.JudicialResultCategory.FINAL;

import uk.gov.justice.core.courts.CourtApplication;
import uk.gov.justice.core.courts.JudicialResult;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;

public class CaseReactivationOnApplicationGrantHelperTest {

    @Test
    void shouldReactivateWhenReopenCaseGranted() {
        final UUID caseId = randomUUID();
        final CourtApplication reopenGranted = reopenApplication(caseId, singletonList(ResultConstants.G));

        assertThat(CaseReactivationOnApplicationGrantHelper.shouldReactivateCase(caseId, singletonList(reopenGranted)), is(true));
    }

    @Test
    void shouldReactivateWhenReopenCaseGrantedEvenWithoutCourtApplicationCases() {
        final UUID caseId = randomUUID();
        final CourtApplication reopenGranted = courtApplication()
                .withId(randomUUID())
                .withType(courtApplicationType().withCode(ApplicationTypeConstants.APP_TYPE_REOPEN_CASE_ID).build())
                .withJudicialResults(singletonList(grantedResult()))
                .build();

        assertThat(CaseReactivationOnApplicationGrantHelper.shouldReactivateCase(caseId, singletonList(reopenGranted)), is(true));
    }

    @Test
    void shouldNotReactivateWhenGrantedButDifferentApplicationType() {
        final UUID caseId = randomUUID();
        final CourtApplication statutoryGranted = courtApplication()
                .withId(randomUUID())
                .withType(courtApplicationType().withCode(ApplicationTypeConstants.APP_TYPE_STATUTORY_DECLARATION_ID).build())
                .withCourtApplicationCases(singletonList(courtApplicationCase().withProsecutionCaseId(caseId).build()))
                .withJudicialResults(singletonList(grantedResult()))
                .build();

        assertThat(CaseReactivationOnApplicationGrantHelper.shouldReactivateCase(caseId, singletonList(statutoryGranted)), is(false));
    }

    @Test
    void shouldNotReactivateWhenReopenCaseRefused() {
        final UUID caseId = randomUUID();
        final CourtApplication reopenRefused = reopenApplication(caseId, singletonList(ResultConstants.RFSD));

        assertThat(CaseReactivationOnApplicationGrantHelper.shouldReactivateCase(caseId, singletonList(reopenRefused)), is(false));
    }

    @Test
    void shouldNotReactivateWhenReopenGrantIsForDifferentCase() {
        final UUID caseId = randomUUID();
        final CourtApplication reopenForOtherCase = reopenApplication(randomUUID(), singletonList(ResultConstants.G));

        assertThat(CaseReactivationOnApplicationGrantHelper.shouldReactivateCase(caseId, singletonList(reopenForOtherCase)), is(false));
    }

    @Test
    void shouldNotReactivateWhenApplicationsEmptyOrNull() {
        final UUID caseId = randomUUID();

        assertThat(CaseReactivationOnApplicationGrantHelper.shouldReactivateCase(caseId, emptyList()), is(false));
        assertThat(CaseReactivationOnApplicationGrantHelper.shouldReactivateCase(caseId, null), is(false));
        assertThat(CaseReactivationOnApplicationGrantHelper.shouldReactivateCase(null, singletonList(reopenApplication(randomUUID(), singletonList(ResultConstants.G)))), is(false));
    }

    private CourtApplication reopenApplication(final UUID caseId, final List<UUID> resultTypeIds) {
        final List<JudicialResult> judicialResults = resultTypeIds.stream()
                .map(id -> judicialResult()
                        .withJudicialResultTypeId(id)
                        .withRootJudicialResultTypeId(id)
                        .withCategory(FINAL)
                        .build())
                .toList();

        return courtApplication()
                .withId(randomUUID())
                .withType(courtApplicationType().withCode(ApplicationTypeConstants.APP_TYPE_REOPEN_CASE_ID).build())
                .withCourtApplicationCases(singletonList(courtApplicationCase().withProsecutionCaseId(caseId).build()))
                .withJudicialResults(judicialResults)
                .build();
    }

    private JudicialResult grantedResult() {
        return judicialResult()
                .withJudicialResultTypeId(ResultConstants.G)
                .withRootJudicialResultTypeId(ResultConstants.G)
                .withCategory(FINAL)
                .build();
    }
}
