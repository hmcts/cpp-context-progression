package uk.gov.moj.cpp.progression.query.view.service;

import uk.gov.justice.services.core.requester.Requester;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.JsonEnvelope;

import static java.util.UUID.randomUUID;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import javax.json.Json;
import javax.json.JsonObject;

@ExtendWith(MockitoExtension.class)
class HearingServiceTest {

    @Mock
    private Requester requester;

    @InjectMocks
    private HearingService hearingService;

    @Test
    void shouldGetApplicationHearing() {
        final ArgumentCaptor<JsonEnvelope> argumentCaptor = ArgumentCaptor.forClass(JsonEnvelope.class);
        final UUID applicationId = randomUUID();
        final UUID hearingId1 = randomUUID();
        final UUID hearingId2 = randomUUID();

        final JsonObject responsePayload = Json.createObjectBuilder()
                .add("hearingSummaries",Json.createArrayBuilder()
                        .add(Json.createObjectBuilder().add("hearingId", hearingId1.toString()).build())
                        .add(Json.createObjectBuilder().add("hearingId", hearingId2.toString()).build()))
                .build();


        final Envelope response = Envelope.envelopeFrom(Envelope.metadataBuilder().withId(UUID.randomUUID()).withName("name").build(), responsePayload);

        when(requester.requestAsAdmin(any(), any())).thenReturn(response);

        final List<UUID> applicationHearingIds = hearingService.getApplicationHearings(applicationId);

        verify(requester).requestAsAdmin(argumentCaptor.capture(), eq(JsonObject.class));

        assertThat(argumentCaptor.getValue().payloadAsJsonObject().getString("id"), is(applicationId.toString()));

        assertThat(applicationHearingIds.equals(Arrays.asList(hearingId1, hearingId2)), is(true));
    }
}
