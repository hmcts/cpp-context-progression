package uk.gov.moj.cpp.progression.query.view.service;

import static java.util.UUID.randomUUID;
import static javax.json.Json.createObjectBuilder;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;

import uk.gov.justice.services.core.requester.Requester;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.messaging.MetadataBuilder;

import java.util.UUID;

import javax.json.JsonValue;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DefenceQueryServiceTest {

    @Mock
    private JsonEnvelope jsonEnvelope;

    @Mock
    private Requester requester;

    @InjectMocks
    private DefenceQueryService defenceQueryService;

    private final String caseId = UUID.randomUUID().toString();

    @Test
    void shouldIsUserOnlyDefendingReturnFalseWhenNotHasRoleDetailsAndResponseIsNull() {
        final JsonEnvelope response = envelopeFrom(getMetadataBuilder(UUID.randomUUID()).build(), JsonValue.EMPTY_JSON_OBJECT);
        when(jsonEnvelope.metadata()).thenReturn(getMetadataBuilder(UUID.randomUUID()).build());
        when(requester.request(any())).thenReturn(response);

        assertThat(defenceQueryService.isUserOnlyDefendingCase(jsonEnvelope, caseId), is(false));
    }

    @Test
    void shouldIsUserOnlyDefendingReturnTrueWhenHasRoleHasDefending() {
        final JsonEnvelope response = envelopeFrom(getMetadataBuilder(UUID.randomUUID()).build(), createObjectBuilder().add("isAdvocateDefendingOrProsecuting", "defending").build());
        when(jsonEnvelope.metadata()).thenReturn(getMetadataBuilder(UUID.randomUUID()).build());
        when(requester.request(any())).thenReturn(response);

        assertThat(defenceQueryService.isUserOnlyDefendingCase(jsonEnvelope, caseId), is(true));
    }

    @Test
    void shouldIsUserOnlyDefendingReturnFalseWhenHasRoleHasDefending() {
        final JsonEnvelope response = envelopeFrom(getMetadataBuilder(UUID.randomUUID()).build(), createObjectBuilder().add("isAdvocateDefendingOrProsecuting", "prosecuting").build());
        when(jsonEnvelope.metadata()).thenReturn(getMetadataBuilder(UUID.randomUUID()).build());
        when(requester.request(any())).thenReturn(response);

        assertThat(defenceQueryService.isUserOnlyDefendingCase(jsonEnvelope, caseId), is(false));
    }

    @Test
    void shouldIsUserOnlyDefendingReturnFalseWhenHasRoleHasBoth() {
        final JsonEnvelope response = envelopeFrom(getMetadataBuilder(UUID.randomUUID()).build(), createObjectBuilder().add("isAdvocateDefendingOrProsecuting", "both").build());
        when(jsonEnvelope.metadata()).thenReturn(getMetadataBuilder(UUID.randomUUID()).build());
        when(requester.request(any())).thenReturn(response);

        assertThat(defenceQueryService.isUserOnlyDefendingCase(jsonEnvelope, caseId), is(false));
    }

    private MetadataBuilder getMetadataBuilder(final UUID userId) {
        return JsonEnvelope.metadataBuilder()
                .withId(randomUUID())
                .withName("progression.query.courtdocuments.for.prosecution")
                .withCausation(randomUUID())
                .withClientCorrelationId(randomUUID().toString())
                .withStreamId(randomUUID())
                .withUserId(userId.toString());
    }

}