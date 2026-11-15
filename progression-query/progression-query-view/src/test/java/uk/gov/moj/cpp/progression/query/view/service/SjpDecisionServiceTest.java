package uk.gov.moj.cpp.progression.query.view.service;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.justice.services.messaging.JsonObjects.createArrayBuilder;
import static uk.gov.justice.services.messaging.JsonObjects.createObjectBuilder;
import static uk.gov.justice.services.test.utils.core.messaging.MetadataBuilderFactory.metadataWithRandomUUIDAndName;
import static uk.gov.justice.services.test.utils.core.reflection.ReflectionUtil.setField;

import uk.gov.justice.core.courts.CourtApplication;
import uk.gov.justice.core.courts.CourtApplicationType;
import uk.gov.justice.core.courts.InitiationCode;
import uk.gov.justice.core.courts.JudicialResult;
import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.common.converter.jackson.ObjectMapperProducer;
import uk.gov.justice.services.messaging.JsonEnvelope;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import javax.json.JsonObject;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class SjpDecisionServiceTest {

    private static final UUID CASE_ID = UUID.randomUUID();
    private static final UUID FO = UUID.randomUUID();
    private static final JsonEnvelope QUERY = envelopeFrom(metadataWithRandomUUIDAndName(), createObjectBuilder().build());
    private static final ZonedDateTime DAY_1 = ZonedDateTime.parse("2026-01-01T10:00:00Z");
    private static final String SHARED_TIME = "2026-01-03T11:00:00.000Z";
    private static final List<CourtApplication> IN_SCOPE_APPLICATIONS = singletonList(application("MC80528"));

    @Mock
    private SjpQueryService sjpQueryService;

    @Mock
    private SjpDecisionResultsSource sjpDecisionResultsSource;

    @Mock
    private ReferenceDataService referenceDataService;

    @Spy
    private JsonObjectToObjectConverter jsonObjectToObjectConverter;

    @InjectMocks
    private SjpDecisionService sjpDecisionService;

    @BeforeEach
    void setUp() {
        setField(jsonObjectToObjectConverter, "objectMapper", new ObjectMapperProducer().objectMapper());
    }

    @Test
    void shouldReturnDecisionsAfterTheLatestSetAsideWithShortCodesAndSharedTime() {
        final SjpCaseDecision original = new SjpCaseDecision(UUID.randomUUID(), DAY_1, false);
        final SjpCaseDecision setAside = new SjpCaseDecision(UUID.randomUUID(), DAY_1.plusDays(1), true);
        final SjpCaseDecision reDecision = new SjpCaseDecision(UUID.randomUUID(), DAY_1.plusDays(2), false);
        when(sjpQueryService.getCaseDecisions(QUERY, CASE_ID)).thenReturn(asList(original, setAside, reDecision));
        when(sjpDecisionResultsSource.getDecisionPayloads(QUERY, singletonList(reDecision.id()))).thenReturn(singletonList(payload(reDecision.id(), SHARED_TIME)));
        when(referenceDataService.getResultShortCodes(QUERY, Set.of(FO))).thenReturn(Map.of(FO, "FO"));

        final List<SjpDecisionHearing> hearings = sjpDecisionService.getDecisionHearings(QUERY, CASE_ID, InitiationCode.J, IN_SCOPE_APPLICATIONS);

        assertThat(hearings.size(), is(1));
        assertThat(hearings.get(0).hearing().getId(), is(reDecision.id()));
        assertThat(hearings.get(0).sharedTime(), is(ZonedDateTime.parse(SHARED_TIME)));
        final JudicialResult fine = hearings.get(0).hearing().getDefendantJudicialResults().get(0).getJudicialResult();
        assertThat(fine.getResultText(), is("FO - Fine\nAmount of fine 100.00"));
        assertThat(fine.getLastSharedDateTime(), is(SHARED_TIME));
    }

    @Test
    void shouldShowResultsWithoutShortCodesWhenReferenceDataFails() {
        final UUID decisionId = UUID.randomUUID();
        when(sjpQueryService.getCaseDecisions(QUERY, CASE_ID)).thenReturn(singletonList(new SjpCaseDecision(decisionId, DAY_1, false)));
        when(sjpDecisionResultsSource.getDecisionPayloads(QUERY, singletonList(decisionId))).thenReturn(singletonList(payload(decisionId, SHARED_TIME)));
        when(referenceDataService.getResultShortCodes(any(), any())).thenThrow(new IllegalStateException("reference data unavailable"));

        final List<SjpDecisionHearing> hearings = sjpDecisionService.getDecisionHearings(QUERY, CASE_ID, InitiationCode.J, IN_SCOPE_APPLICATIONS);

        assertThat(hearings.get(0).hearing().getDefendantJudicialResults().get(0).getJudicialResult().getResultText(), is("Fine\nAmount of fine 100.00"));
    }

    @Test
    void shouldLeaveSharedTimeEmptyWhenNotStored() {
        final UUID decisionId = UUID.randomUUID();
        when(sjpQueryService.getCaseDecisions(QUERY, CASE_ID)).thenReturn(singletonList(new SjpCaseDecision(decisionId, DAY_1, false)));
        when(sjpDecisionResultsSource.getDecisionPayloads(QUERY, singletonList(decisionId))).thenReturn(singletonList(payload(decisionId, null)));
        when(referenceDataService.getResultShortCodes(any(), any())).thenReturn(Map.of());

        final List<SjpDecisionHearing> hearings = sjpDecisionService.getDecisionHearings(QUERY, CASE_ID, InitiationCode.J, IN_SCOPE_APPLICATIONS);

        assertThat(hearings.get(0).sharedTime(), is(nullValue()));
        assertThat(hearings.get(0).hearing().getDefendantJudicialResults().get(0).getJudicialResult().getLastSharedDateTime(), is(nullValue()));
    }

    @Test
    void shouldNotCallSjpOrResultsWhenCaseIsOutOfScope() {
        assertThat(sjpDecisionService.getDecisionHearings(QUERY, CASE_ID, InitiationCode.C, IN_SCOPE_APPLICATIONS), is(empty()));
        assertThat(sjpDecisionService.getDecisionHearings(QUERY, CASE_ID, InitiationCode.J, singletonList(application("MC80527"))), is(empty()));

        verifyNoInteractions(sjpQueryService, sjpDecisionResultsSource, referenceDataService);
    }

    @Test
    void shouldNotCallResultsWhenThereAreNoDecisionsToShow() {
        when(sjpQueryService.getCaseDecisions(QUERY, CASE_ID)).thenReturn(emptyList());

        assertThat(sjpDecisionService.getDecisionHearings(QUERY, CASE_ID, InitiationCode.J, IN_SCOPE_APPLICATIONS), is(empty()));
        verifyNoInteractions(sjpDecisionResultsSource, referenceDataService);
    }

    @Test
    void shouldReturnNoDecisionsWhenSjpFails() {
        when(sjpQueryService.getCaseDecisions(QUERY, CASE_ID)).thenThrow(new IllegalStateException("SJP unavailable"));

        assertThat(sjpDecisionService.getDecisionHearings(QUERY, CASE_ID, InitiationCode.J, IN_SCOPE_APPLICATIONS), is(empty()));
    }

    @Test
    void shouldReturnNoDecisionsWhenResultsFails() {
        when(sjpQueryService.getCaseDecisions(QUERY, CASE_ID)).thenReturn(singletonList(new SjpCaseDecision(UUID.randomUUID(), DAY_1, false)));
        when(sjpDecisionResultsSource.getDecisionPayloads(any(), any())).thenThrow(new IllegalStateException("Results unavailable"));

        assertThat(sjpDecisionService.getDecisionHearings(QUERY, CASE_ID, InitiationCode.J, IN_SCOPE_APPLICATIONS), is(empty()));
    }

    private static JsonObject payload(final UUID decisionId, final String sharedTime) {
        final javax.json.JsonObjectBuilder payload = createObjectBuilder()
                .add("hearing", createObjectBuilder()
                        .add("id", decisionId.toString())
                        .add("defendantJudicialResults", createArrayBuilder().add(createObjectBuilder()
                                .add("judicialResult", createObjectBuilder()
                                        .add("judicialResultId", FO.toString())
                                        .add("judicialResultTypeId", FO.toString())
                                        .add("label", "Fine")
                                        .add("resultText", "Fine\nAmount of fine 100.00")))));
        if (sharedTime != null) {
            payload.add("sharedTime", sharedTime);
        }
        return payload.build();
    }

    private static CourtApplication application(final String typeCode) {
        return CourtApplication.courtApplication()
                .withId(UUID.randomUUID())
                .withType(CourtApplicationType.courtApplicationType().withCode(typeCode).build())
                .build();
    }
}
