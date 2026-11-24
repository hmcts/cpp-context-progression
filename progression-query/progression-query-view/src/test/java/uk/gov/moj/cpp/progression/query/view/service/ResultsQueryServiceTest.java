package uk.gov.moj.cpp.progression.query.view.service;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.justice.services.messaging.JsonObjects.createArrayBuilder;
import static uk.gov.justice.services.messaging.JsonObjects.createObjectBuilder;
import static uk.gov.justice.services.test.utils.core.messaging.MetadataBuilderFactory.metadataWithRandomUUIDAndName;
import static uk.gov.justice.services.test.utils.core.reflection.ReflectionUtil.setField;
import static uk.gov.moj.cpp.progression.query.view.service.ResultsQueryService.RESULTS_GET_HEARING_DETAILS_INTERNAL;

import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.common.converter.jackson.ObjectMapperProducer;
import uk.gov.justice.services.core.requester.Requester;
import uk.gov.justice.services.messaging.JsonEnvelope;

import java.time.ZonedDateTime;
import java.util.Optional;
import java.util.UUID;

import javax.json.Json;
import javax.json.JsonObject;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ResultsQueryServiceTest {

    private static final JsonEnvelope QUERY = envelopeFrom(metadataWithRandomUUIDAndName(), createObjectBuilder().build());
    private static final String SHARED_TIME = "2026-01-01T10:15:30.000Z";

    @Mock
    private Requester requester;

    @Spy
    private JsonObjectToObjectConverter jsonObjectToObjectConverter;

    @InjectMocks
    private ResultsQueryService resultsQueryService;

    @Captor
    private ArgumentCaptor<JsonEnvelope> request;

    @BeforeEach
    void setUp() {
        setField(jsonObjectToObjectConverter, "objectMapper", new ObjectMapperProducer().objectMapper());
    }

    @Test
    void shouldFetchTheDecisionAsAdmin() {
        final UUID decisionId = UUID.randomUUID();
        when(requester.requestAsAdmin(request.capture())).thenReturn(response(storedPayload(decisionId.toString())));

        final Optional<SjpDecisionHearing> hearing = resultsQueryService.getDecisionHearing(QUERY, decisionId);

        assertThat(request.getValue().metadata().name(), is(RESULTS_GET_HEARING_DETAILS_INTERNAL));
        assertThat(request.getValue().payloadAsJsonObject().getString("hearingId"), is(decisionId.toString()));
        assertThat(hearing.orElseThrow().hearing().getId(), is(decisionId));
        assertThat(hearing.orElseThrow().sharedTime(), is(ZonedDateTime.parse(SHARED_TIME)));
    }

    @Test
    void shouldReturnNoHearingWhenNoResultsAreStoredForTheDecision() {
        when(requester.requestAsAdmin(any(JsonEnvelope.class))).thenReturn(response(createObjectBuilder().build()));

        assertThat(resultsQueryService.getDecisionHearing(QUERY, UUID.randomUUID()), is(Optional.empty()));
    }

    @Test
    void shouldLeaveSharedTimeEmptyWhenNotStored() {
        final UUID decisionId = UUID.randomUUID();
        when(requester.requestAsAdmin(any(JsonEnvelope.class))).thenReturn(response(createObjectBuilder()
                .add("hearing", createObjectBuilder().add("id", decisionId.toString()))
                .build()));

        final SjpDecisionHearing hearing = resultsQueryService.getDecisionHearing(QUERY, decisionId).orElseThrow();

        assertThat(hearing.hearing().getId(), is(decisionId));
        assertThat(hearing.sharedTime(), is(nullValue()));
    }

    private static JsonObject storedPayload(final String hearingId) {
        return createObjectBuilder()
                .add("hearing", createObjectBuilder().add("id", hearingId).add("jurisdictionType", "MAGISTRATES"))
                .add("sharedTime", SHARED_TIME)
                .build();
    }

    private static JsonEnvelope response(final JsonObject payload) {
        return envelopeFrom(metadataWithRandomUUIDAndName(), payload);
    }

    @Test
    void shouldSetLastSharedDateTimeOnEveryJudicialResultThatHasNone() {
        final String existing = "2025-12-31T09:00:00.000Z";
        final JsonObject hearing = createObjectBuilder()
                .add("id", UUID.randomUUID().toString())
                .add("defendantJudicialResults", createArrayBuilder().add(createObjectBuilder()
                        .add("judicialResult", result("FVS"))))
                .add("prosecutionCases", createArrayBuilder().add(createObjectBuilder()
                        .add("defendants", createArrayBuilder().add(createObjectBuilder()
                                .add("defendantCaseJudicialResults", createArrayBuilder().add(result("FCOST")))
                                .add("offences", createArrayBuilder().add(createObjectBuilder()
                                        .add("judicialResults", createArrayBuilder()
                                                .add(result("FO"))
                                                .add(result("FCOMP").add("lastSharedDateTime", existing))
                                                .add(result("LEP").addNull("lastSharedDateTime")))))))))
                .build();

        final JsonObject filled = (JsonObject) ResultsQueryService.withLastSharedDateTime(hearing, Json.createValue(SHARED_TIME));

        final JsonObject defendant = filled.getJsonArray("prosecutionCases").getJsonObject(0).getJsonArray("defendants").getJsonObject(0);
        final javax.json.JsonArray offenceResults = defendant.getJsonArray("offences").getJsonObject(0).getJsonArray("judicialResults");
        assertThat(filled.getJsonArray("defendantJudicialResults").getJsonObject(0).getJsonObject("judicialResult").getString("lastSharedDateTime"), is(SHARED_TIME));
        assertThat(defendant.getJsonArray("defendantCaseJudicialResults").getJsonObject(0).getString("lastSharedDateTime"), is(SHARED_TIME));
        assertThat(offenceResults.getJsonObject(0).getString("lastSharedDateTime"), is(SHARED_TIME));
        assertThat(offenceResults.getJsonObject(1).getString("lastSharedDateTime"), is(existing));
        assertThat(offenceResults.getJsonObject(2).getString("lastSharedDateTime"), is(SHARED_TIME));
        assertThat(filled.containsKey("lastSharedDateTime"), is(false));
        assertThat(defendant.containsKey("lastSharedDateTime"), is(false));
    }

    @Test
    void shouldReturnHearingWhoseResultsCarryTheSharedTime() {
        final UUID decisionId = UUID.randomUUID();
        when(requester.requestAsAdmin(any(JsonEnvelope.class))).thenReturn(response(createObjectBuilder()
                .add("hearing", createObjectBuilder()
                        .add("id", decisionId.toString())
                        .add("defendantJudicialResults", createArrayBuilder().add(createObjectBuilder()
                                .add("judicialResult", result("FVS")))))
                .add("sharedTime", SHARED_TIME)
                .build()));

        final SjpDecisionHearing hearing = resultsQueryService.getDecisionHearing(QUERY, decisionId).orElseThrow();

        assertThat(hearing.hearing().getDefendantJudicialResults().get(0).getJudicialResult().getLastSharedDateTime(), is(SHARED_TIME));
    }

    private static javax.json.JsonObjectBuilder result(final String label) {
        return createObjectBuilder().add("judicialResultId", UUID.randomUUID().toString()).add("label", label);
    }
}
