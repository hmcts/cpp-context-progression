package uk.gov.moj.cpp.progression.query.view.service;

import static java.util.Arrays.asList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.justice.services.messaging.JsonObjects.createObjectBuilder;
import static uk.gov.justice.services.test.utils.core.messaging.MetadataBuilderFactory.metadataWithRandomUUIDAndName;
import static uk.gov.justice.services.test.utils.core.reflection.ReflectionUtil.setField;
import static uk.gov.moj.cpp.progression.query.view.service.ResultsQueryService.RESULTS_GET_HEARING_DETAILS_INTERNAL;

import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.common.converter.jackson.ObjectMapperProducer;
import uk.gov.justice.services.core.requester.Requester;
import uk.gov.justice.services.messaging.JsonEnvelope;

import java.time.ZonedDateTime;
import java.util.List;
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
class ResultsQueryServiceTest {

    private static final JsonEnvelope QUERY = envelopeFrom(metadataWithRandomUUIDAndName(), createObjectBuilder().build());
    private static final String SHARED_TIME = "2026-01-01T10:15:30.000Z";

    @Mock
    private Requester requester;

    @Spy
    private JsonObjectToObjectConverter jsonObjectToObjectConverter;

    @InjectMocks
    private ResultsQueryService resultsQueryService;

    @BeforeEach
    void setUp() {
        setField(jsonObjectToObjectConverter, "objectMapper", new ObjectMapperProducer().objectMapper());
    }

    @Test
    void shouldFetchEachDecisionAsAdminInOrderAndSkipThoseWithNoStoredResults() {
        final UUID first = UUID.randomUUID();
        final UUID missing = UUID.randomUUID();
        final UUID second = UUID.randomUUID();
        when(requester.requestAsAdmin(any(JsonEnvelope.class))).thenAnswer(invocation -> {
            final JsonEnvelope request = invocation.getArgument(0);
            assertThat(request.metadata().name(), is(RESULTS_GET_HEARING_DETAILS_INTERNAL));
            final String hearingId = request.payloadAsJsonObject().getString("hearingId");
            return response(missing.toString().equals(hearingId) ? createObjectBuilder().build() : storedPayload(hearingId));
        });

        final List<SjpDecisionHearing> hearings = resultsQueryService.getDecisionHearings(QUERY, asList(first, missing, second));

        assertThat(hearings.stream().map(h -> h.hearing().getId()).toList(), contains(first, second));
        assertThat(hearings.get(0).sharedTime(), is(ZonedDateTime.parse(SHARED_TIME)));
    }

    @Test
    void shouldLeaveSharedTimeEmptyWhenNotStored() {
        final UUID decisionId = UUID.randomUUID();
        when(requester.requestAsAdmin(any(JsonEnvelope.class))).thenReturn(response(createObjectBuilder()
                .add("hearing", createObjectBuilder().add("id", decisionId.toString()))
                .build()));

        final List<SjpDecisionHearing> hearings = resultsQueryService.getDecisionHearings(QUERY, List.of(decisionId));

        assertThat(hearings.get(0).hearing().getId(), is(decisionId));
        assertThat(hearings.get(0).sharedTime(), is(nullValue()));
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
}
