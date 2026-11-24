package uk.gov.moj.cpp.progression.query.view.service;

import static java.util.Collections.singletonList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.justice.services.messaging.JsonObjects.createObjectBuilder;
import static uk.gov.justice.services.test.utils.core.messaging.MetadataBuilderFactory.metadataWithRandomUUIDAndName;

import uk.gov.justice.core.courts.CourtApplication;
import uk.gov.justice.core.courts.CourtApplicationType;
import uk.gov.justice.core.courts.Hearing;
import uk.gov.justice.core.courts.InitiationCode;
import uk.gov.justice.services.messaging.JsonEnvelope;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class SjpDecisionServiceTest {

    private static final UUID CASE_ID = UUID.randomUUID();
    private static final JsonEnvelope QUERY = envelopeFrom(metadataWithRandomUUIDAndName(), createObjectBuilder().build());
    private static final ZonedDateTime DAY_1 = ZonedDateTime.parse("2026-01-01T10:00:00Z");
    private static final UUID STAT_DEC_TYPE_ID = UUID.randomUUID();
    private static final Set<UUID> IN_SCOPE_TYPE_IDS = Set.of(STAT_DEC_TYPE_ID);
    private static final List<CourtApplication> IN_SCOPE_APPLICATIONS = singletonList(application(STAT_DEC_TYPE_ID));

    @Mock
    private SjpQueryService sjpQueryService;

    @Mock
    private SjpDecisionResultsSource sjpDecisionResultsSource;

    @InjectMocks
    private SjpDecisionService sjpDecisionService;

    @Test
    void shouldFetchResultsForTheMostRecentDecision() {
        final SjpCaseDecision latest = new SjpCaseDecision(UUID.randomUUID(), DAY_1);
        final SjpDecisionHearing expected = new SjpDecisionHearing(Hearing.hearing().withId(latest.id()).build(), DAY_1);
        when(sjpQueryService.getLatestCaseDecision(QUERY, CASE_ID)).thenReturn(Optional.of(latest));
        when(sjpDecisionResultsSource.getDecisionHearing(QUERY, latest.id())).thenReturn(Optional.of(expected));

        assertThat(sjpDecisionService.getDecisionHearing(QUERY, CASE_ID, InitiationCode.J, IN_SCOPE_APPLICATIONS, IN_SCOPE_TYPE_IDS), is(Optional.of(expected)));
    }

    @Test
    void shouldReturnNoDecisionWhenResultsHasNoneStoredForTheMostRecentDecision() {
        final SjpCaseDecision latest = new SjpCaseDecision(UUID.randomUUID(), DAY_1);
        when(sjpQueryService.getLatestCaseDecision(QUERY, CASE_ID)).thenReturn(Optional.of(latest));
        when(sjpDecisionResultsSource.getDecisionHearing(QUERY, latest.id())).thenReturn(Optional.empty());

        assertThat(sjpDecisionService.getDecisionHearing(QUERY, CASE_ID, InitiationCode.J, IN_SCOPE_APPLICATIONS, IN_SCOPE_TYPE_IDS), is(Optional.empty()));
    }

    @Test
    void shouldNotCallSjpOrResultsWhenCaseIsOutOfScope() {
        assertThat(sjpDecisionService.getDecisionHearing(QUERY, CASE_ID, InitiationCode.C, IN_SCOPE_APPLICATIONS, IN_SCOPE_TYPE_IDS), is(Optional.empty()));
        assertThat(sjpDecisionService.getDecisionHearing(QUERY, CASE_ID, InitiationCode.J, singletonList(application(UUID.randomUUID())), IN_SCOPE_TYPE_IDS), is(Optional.empty()));

        verifyNoInteractions(sjpQueryService, sjpDecisionResultsSource);
    }

    @Test
    void shouldNotCallResultsWhenSjpHasNoDecision() {
        when(sjpQueryService.getLatestCaseDecision(QUERY, CASE_ID)).thenReturn(Optional.empty());

        assertThat(sjpDecisionService.getDecisionHearing(QUERY, CASE_ID, InitiationCode.J, IN_SCOPE_APPLICATIONS, IN_SCOPE_TYPE_IDS), is(Optional.empty()));
        verifyNoInteractions(sjpDecisionResultsSource);
    }

    @Test
    void shouldReturnNoDecisionWhenSjpFails() {
        when(sjpQueryService.getLatestCaseDecision(QUERY, CASE_ID)).thenThrow(new IllegalStateException("SJP unavailable"));

        assertThat(sjpDecisionService.getDecisionHearing(QUERY, CASE_ID, InitiationCode.J, IN_SCOPE_APPLICATIONS, IN_SCOPE_TYPE_IDS), is(Optional.empty()));
    }

    @Test
    void shouldReturnNoDecisionWhenResultsFails() {
        when(sjpQueryService.getLatestCaseDecision(QUERY, CASE_ID)).thenReturn(Optional.of(new SjpCaseDecision(UUID.randomUUID(), DAY_1)));
        when(sjpDecisionResultsSource.getDecisionHearing(any(), any())).thenThrow(new IllegalStateException("Results unavailable"));

        assertThat(sjpDecisionService.getDecisionHearing(QUERY, CASE_ID, InitiationCode.J, IN_SCOPE_APPLICATIONS, IN_SCOPE_TYPE_IDS), is(Optional.empty()));
    }

    private static CourtApplication application(final UUID typeId) {
        return CourtApplication.courtApplication()
                .withId(UUID.randomUUID())
                .withType(CourtApplicationType.courtApplicationType().withId(typeId).build())
                .build();
    }
}
