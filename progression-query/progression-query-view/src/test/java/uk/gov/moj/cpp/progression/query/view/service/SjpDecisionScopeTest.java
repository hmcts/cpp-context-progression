package uk.gov.moj.cpp.progression.query.view.service;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

import uk.gov.justice.core.courts.ApplicationStatus;
import uk.gov.justice.core.courts.CourtApplication;
import uk.gov.justice.core.courts.CourtApplicationType;
import uk.gov.justice.core.courts.InitiationCode;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class SjpDecisionScopeTest {

    private static final String STAT_DEC_SJP = "MC80528";
    private static final String STAT_DEC_NON_SJP = "MC80527";

    @ParameterizedTest
    @ValueSource(strings = {"MC80528", "MC80524", "MC80801", "MC80802", "MC80803"})
    void shouldBeInScopeForSjpCaseWithLiveInScopeApplication(final String typeCode) {
        assertThat(SjpDecisionScope.isInScope(InitiationCode.J, singletonList(application(typeCode, ApplicationStatus.LISTED))), is(true));
    }

    @Test
    void shouldBeInScopeWhenApplicationStatusIsNotSet() {
        assertThat(SjpDecisionScope.isInScope(InitiationCode.J, singletonList(application(STAT_DEC_SJP, null))), is(true));
    }

    @Test
    void shouldNotBeInScopeWhenOnlyInScopeApplicationIsEjected() {
        assertThat(SjpDecisionScope.isInScope(InitiationCode.J, singletonList(application(STAT_DEC_SJP, ApplicationStatus.EJECTED))), is(false));
    }

    @Test
    void shouldBeInScopeWhenOneOfSeveralInScopeApplicationsIsNotEjected() {
        final List<CourtApplication> applications = asList(
                application(STAT_DEC_SJP, ApplicationStatus.EJECTED),
                application("MC80802", ApplicationStatus.FINALISED));

        assertThat(SjpDecisionScope.isInScope(InitiationCode.J, applications), is(true));
    }

    @ParameterizedTest
    @ValueSource(strings = {STAT_DEC_NON_SJP, "CJ03510", "PC02541"})
    void shouldNotBeInScopeForSjpCaseWithOnlyOutOfScopeApplications(final String typeCode) {
        assertThat(SjpDecisionScope.isInScope(InitiationCode.J, singletonList(application(typeCode, ApplicationStatus.LISTED))), is(false));
    }

    @Test
    void shouldNotBeInScopeForSjpCaseWithNoLinkedApplications() {
        assertThat(SjpDecisionScope.isInScope(InitiationCode.J, emptyList()), is(false));
        assertThat(SjpDecisionScope.isInScope(InitiationCode.J, null), is(false));
    }

    @ParameterizedTest
    @ValueSource(strings = {"C", "O", "Q", "S"})
    void shouldNotBeInScopeForCaseNotFromSjp(final String initiationCode) {
        assertThat(SjpDecisionScope.isInScope(InitiationCode.valueOf(initiationCode), singletonList(application(STAT_DEC_SJP, ApplicationStatus.LISTED))), is(false));
    }

    @Test
    void shouldNotBeInScopeWhenInitiationCodeMissing() {
        assertThat(SjpDecisionScope.isInScope(null, singletonList(application(STAT_DEC_SJP, ApplicationStatus.LISTED))), is(false));
    }

    @Test
    void shouldIgnoreApplicationWithoutType() {
        final CourtApplication noType = CourtApplication.courtApplication().withId(UUID.randomUUID()).build();

        assertThat(SjpDecisionScope.isInScope(InitiationCode.J, singletonList(noType)), is(false));
    }

    private static CourtApplication application(final String typeCode, final ApplicationStatus status) {
        return CourtApplication.courtApplication()
                .withId(UUID.randomUUID())
                .withType(CourtApplicationType.courtApplicationType().withCode(typeCode).build())
                .withApplicationStatus(status)
                .build();
    }
}
