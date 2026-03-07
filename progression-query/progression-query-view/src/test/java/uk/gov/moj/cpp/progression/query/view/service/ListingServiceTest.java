package uk.gov.moj.cpp.progression.query.view.service;

import static java.util.UUID.randomUUID;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.messaging.Envelope.metadataBuilder;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;

import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.core.requester.Requester;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.messaging.MetadataBuilder;
import uk.gov.justice.services.test.utils.core.enveloper.EnveloperFactory;
import uk.gov.justice.services.test.utils.core.messaging.JsonEnvelopeBuilder;

import java.util.Optional;
import java.util.UUID;

import javax.json.Json;
import javax.json.JsonObject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class ListingServiceTest {

    @Mock
    private Requester requester;

    @InjectMocks
    private ListingService listingService;

    @Spy
    Enveloper enveloper = EnveloperFactory.createEnveloper();

    @Test
    public void shouldSearchCourtlistDetails() {

        final MetadataBuilder metadataBuilder = metadataBuilder()
                .withName("progression.search.court.list")
                .withId(randomUUID());

        final JsonEnvelope query = JsonEnvelopeBuilder.envelope()
                .with(metadataBuilder)
                .build();

        final JsonObject listingPayload = Json.createObjectBuilder()
                .add("key", "value")
                .build();

        final JsonEnvelope response = envelopeFrom(metadataBuilder()
                .withId(UUID.randomUUID())
                .withName("listing.search.court.list.payload")
                .build(), listingPayload);

        when(requester.requestAsAdmin(any())).thenReturn(response);


        //when
        final Optional<JsonObject> result = listingService.searchCourtlist(query);

        //then
        assertThat(result.get().getString("key"), is("value"));
    }

    @Test
    public void shouldSendIncludeApplicationsTrueToListing() {
        final JsonObject queryPayload = Json.createObjectBuilder()
                .add("courtCentreId", "centre-1")
                .add("listId", "PUBLIC")
                .add("startDate", "2025-01-01")
                .add("endDate", "2025-01-31")
                .build();
        final JsonEnvelope query = envelopeFrom(
                metadataBuilder().withName("progression.search.court.list").withId(randomUUID()).build(),
                queryPayload);

        final ArgumentCaptor<JsonEnvelope> envelopeCaptor = ArgumentCaptor.forClass(JsonEnvelope.class);
        when(requester.requestAsAdmin(any())).thenReturn(envelopeFrom(
                metadataBuilder().withName("listing.search.court.list.payload").withId(randomUUID()).build(),
                Json.createObjectBuilder().build()));

        listingService.searchCourtlist(query);

        verify(requester).requestAsAdmin(envelopeCaptor.capture());
        final JsonObject sentPayload = envelopeCaptor.getValue().payloadAsJsonObject();
        assertThat(sentPayload.containsKey("includeApplications"), is(true));
        assertThat(sentPayload.getBoolean("includeApplications"), is(true));
    }
}
