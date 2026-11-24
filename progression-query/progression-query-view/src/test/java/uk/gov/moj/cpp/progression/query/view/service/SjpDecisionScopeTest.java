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
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class SjpDecisionScopeTest {

    private static final UUID STAT_DEC = UUID.randomUUID();
    private static final UUID APPEAL = UUID.randomUUID();
    private static final UUID OUT_OF_SCOPE = UUID.randomUUID();
    private static final Set<UUID> IN_SCOPE_TYPE_IDS = Set.of(STAT_DEC, APPEAL);

    @Test
    void shouldBeInScopeForSjpCaseWithLiveApplicationOfAnInScopeType() {
        assertThat(SjpDecisionScope.isInScope(InitiationCode.J, singletonList(application(STAT_DEC, ApplicationStatus.LISTED)), IN_SCOPE_TYPE_IDS), is(true));
        assertThat(SjpDecisionScope.isInScope(InitiationCode.J, singletonList(application(APPEAL, ApplicationStatus.LISTED)), IN_SCOPE_TYPE_IDS), is(true));
    }

    @Test
    void shouldBeInScopeWhenApplicationStatusIsNotSet() {
        assertThat(SjpDecisionScope.isInScope(InitiationCode.J, singletonList(application(STAT_DEC, null)), IN_SCOPE_TYPE_IDS), is(true));
    }

    @Test
    void shouldNotBeInScopeWhenOnlyInScopeApplicationIsEjected() {
        assertThat(SjpDecisionScope.isInScope(InitiationCode.J, singletonList(application(STAT_DEC, ApplicationStatus.EJECTED)), IN_SCOPE_TYPE_IDS), is(false));
    }

    @Test
    void shouldBeInScopeWhenOneOfSeveralInScopeApplicationsIsNotEjected() {
        final List<CourtApplication> applications = asList(
                application(STAT_DEC, ApplicationStatus.EJECTED),
                application(APPEAL, ApplicationStatus.FINALISED));

        assertThat(SjpDecisionScope.isInScope(InitiationCode.J, applications, IN_SCOPE_TYPE_IDS), is(true));
    }

    @Test
    void shouldNotBeInScopeForSjpCaseWithOnlyOutOfScopeApplications() {
        assertThat(SjpDecisionScope.isInScope(InitiationCode.J, singletonList(application(OUT_OF_SCOPE, ApplicationStatus.LISTED)), IN_SCOPE_TYPE_IDS), is(false));
    }

    @Test
    void shouldNotBeInScopeWhenReferenceDataGaveNoInScopeTypes() {
        final List<CourtApplication> applications = singletonList(application(STAT_DEC, ApplicationStatus.LISTED));

        assertThat(SjpDecisionScope.isInScope(InitiationCode.J, applications, Set.of()), is(false));
        assertThat(SjpDecisionScope.isInScope(InitiationCode.J, applications, null), is(false));
    }

    @Test
    void shouldNotBeInScopeForSjpCaseWithNoLinkedApplications() {
        assertThat(SjpDecisionScope.isInScope(InitiationCode.J, emptyList(), IN_SCOPE_TYPE_IDS), is(false));
        assertThat(SjpDecisionScope.isInScope(InitiationCode.J, null, IN_SCOPE_TYPE_IDS), is(false));
    }

    @ParameterizedTest
    @ValueSource(strings = {"C", "O", "Q", "S"})
    void shouldNotBeInScopeForCaseNotFromSjp(final String initiationCode) {
        assertThat(SjpDecisionScope.isInScope(InitiationCode.valueOf(initiationCode), singletonList(application(STAT_DEC, ApplicationStatus.LISTED)), IN_SCOPE_TYPE_IDS), is(false));
    }

    @Test
    void shouldNotBeInScopeWhenInitiationCodeMissing() {
        assertThat(SjpDecisionScope.isInScope(null, singletonList(application(STAT_DEC, ApplicationStatus.LISTED)), IN_SCOPE_TYPE_IDS), is(false));
    }

    @Test
    void shouldIgnoreApplicationWithoutType() {
        final CourtApplication noType = CourtApplication.courtApplication().withId(UUID.randomUUID()).build();

        assertThat(SjpDecisionScope.isInScope(InitiationCode.J, singletonList(noType), IN_SCOPE_TYPE_IDS), is(false));
    }

    private static CourtApplication application(final UUID typeId, final ApplicationStatus status) {
        return CourtApplication.courtApplication()
                .withId(UUID.randomUUID())
                .withType(CourtApplicationType.courtApplicationType().withId(typeId).build())
                .withApplicationStatus(status)
                .build();
    }
}
