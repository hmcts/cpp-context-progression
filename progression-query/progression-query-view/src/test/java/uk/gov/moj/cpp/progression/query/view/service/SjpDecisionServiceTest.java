package uk.gov.moj.cpp.progression.query.view.service;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
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
    private static final List<CourtApplication> IN_SCOPE_APPLICATIONS = singletonList(application("MC80528"));

    @Mock
    private SjpQueryService sjpQueryService;

    @Mock
    private SjpDecisionResultsSource sjpDecisionResultsSource;

    @InjectMocks
    private SjpDecisionService sjpDecisionService;

    @Test
    void shouldFetchResultsForTheDecisionsAfterTheLatestSetAside() {
        final SjpCaseDecision original = new SjpCaseDecision(UUID.randomUUID(), DAY_1, false);
        final SjpCaseDecision setAside = new SjpCaseDecision(UUID.randomUUID(), DAY_1.plusDays(1), true);
        final SjpCaseDecision reDecision = new SjpCaseDecision(UUID.randomUUID(), DAY_1.plusDays(2), false);
        final List<SjpDecisionHearing> expected = singletonList(new SjpDecisionHearing(Hearing.hearing().withId(reDecision.id()).build(), DAY_1));
        when(sjpQueryService.getCaseDecisions(QUERY, CASE_ID)).thenReturn(asList(original, setAside, reDecision));
        when(sjpDecisionResultsSource.getDecisionHearings(QUERY, singletonList(reDecision.id()))).thenReturn(expected);

        assertThat(sjpDecisionService.getDecisionHearings(QUERY, CASE_ID, InitiationCode.J, IN_SCOPE_APPLICATIONS), is(expected));
    }

    @Test
    void shouldNotCallSjpOrResultsWhenCaseIsOutOfScope() {
        assertThat(sjpDecisionService.getDecisionHearings(QUERY, CASE_ID, InitiationCode.C, IN_SCOPE_APPLICATIONS), is(empty()));
        assertThat(sjpDecisionService.getDecisionHearings(QUERY, CASE_ID, InitiationCode.J, singletonList(application("MC80527"))), is(empty()));

        verifyNoInteractions(sjpQueryService, sjpDecisionResultsSource);
    }

    @Test
    void shouldNotCallResultsWhenThereAreNoDecisionsToShow() {
        when(sjpQueryService.getCaseDecisions(QUERY, CASE_ID)).thenReturn(emptyList());

        assertThat(sjpDecisionService.getDecisionHearings(QUERY, CASE_ID, InitiationCode.J, IN_SCOPE_APPLICATIONS), is(empty()));
        verifyNoInteractions(sjpDecisionResultsSource);
    }

    @Test
    void shouldReturnNoDecisionsWhenSjpFails() {
        when(sjpQueryService.getCaseDecisions(QUERY, CASE_ID)).thenThrow(new IllegalStateException("SJP unavailable"));

        assertThat(sjpDecisionService.getDecisionHearings(QUERY, CASE_ID, InitiationCode.J, IN_SCOPE_APPLICATIONS), is(empty()));
    }

    @Test
    void shouldReturnNoDecisionsWhenResultsFails() {
        when(sjpQueryService.getCaseDecisions(QUERY, CASE_ID)).thenReturn(singletonList(new SjpCaseDecision(UUID.randomUUID(), DAY_1, false)));
        when(sjpDecisionResultsSource.getDecisionHearings(any(), any())).thenThrow(new IllegalStateException("Results unavailable"));

        assertThat(sjpDecisionService.getDecisionHearings(QUERY, CASE_ID, InitiationCode.J, IN_SCOPE_APPLICATIONS), is(empty()));
    }

    private static CourtApplication application(final String typeCode) {
        return CourtApplication.courtApplication()
                .withId(UUID.randomUUID())
                .withType(CourtApplicationType.courtApplicationType().withCode(typeCode).build())
                .build();
    }
}
