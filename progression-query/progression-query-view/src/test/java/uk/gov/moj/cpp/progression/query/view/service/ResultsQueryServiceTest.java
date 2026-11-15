package uk.gov.moj.cpp.progression.query.view.service;

import static java.util.Arrays.asList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.justice.services.messaging.JsonObjects.createObjectBuilder;
import static uk.gov.justice.services.test.utils.core.messaging.MetadataBuilderFactory.metadataWithRandomUUIDAndName;
import static uk.gov.moj.cpp.progression.query.view.service.ResultsQueryService.RESULTS_GET_HEARING_DETAILS_INTERNAL;

import uk.gov.justice.services.core.requester.Requester;
import uk.gov.justice.services.messaging.JsonEnvelope;

import java.util.List;
import java.util.UUID;

import javax.json.JsonObject;
import javax.json.JsonValue;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ResultsQueryServiceTest {

    private static final JsonEnvelope QUERY = envelopeFrom(metadataWithRandomUUIDAndName(), createObjectBuilder().build());

    @Mock
    private Requester requester;

    @InjectMocks
    private ResultsQueryService resultsQueryService;

    @Test
    void shouldFetchEachDecisionAsAdminInOrderAndSkipThoseWithNoStoredResults() {
        final UUID first = UUID.randomUUID();
        final UUID missing = UUID.randomUUID();
        final UUID nullPayload = UUID.randomUUID();
        final UUID second = UUID.randomUUID();
        when(requester.requestAsAdmin(any(JsonEnvelope.class))).thenAnswer(invocation -> {
            final JsonEnvelope request = invocation.getArgument(0);
            assertThat(request.metadata().name(), is(RESULTS_GET_HEARING_DETAILS_INTERNAL));
            final String hearingId = request.payloadAsJsonObject().getString("hearingId");
            if (nullPayload.toString().equals(hearingId)) {
                return envelopeFrom(metadataWithRandomUUIDAndName(), JsonValue.NULL);
            }
            return envelopeFrom(metadataWithRandomUUIDAndName(),
                    missing.toString().equals(hearingId) ? createObjectBuilder().build() : storedPayload(hearingId));
        });

        final List<JsonObject> payloads = resultsQueryService.getDecisionPayloads(QUERY, asList(first, missing, nullPayload, second));

        assertThat(payloads.stream().map(p -> p.getJsonObject("hearing").getString("id")).toList(), contains(first.toString(), second.toString()));
        assertThat(payloads.get(0).getString("sharedTime"), is("2026-01-01T10:15:30.000Z"));
    }

    private static JsonObject storedPayload(final String hearingId) {
        return createObjectBuilder()
                .add("hearing", createObjectBuilder().add("id", hearingId))
                .add("sharedTime", "2026-01-01T10:15:30.000Z")
                .build();
    }
}
