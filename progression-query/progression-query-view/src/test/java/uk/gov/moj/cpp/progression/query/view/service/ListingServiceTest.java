package uk.gov.moj.cpp.progression.query.view.service;

import static java.util.UUID.randomUUID;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
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

import uk.gov.justice.services.messaging.JsonObjects;
import javax.json.JsonObject;

import org.junit.jupiter.api.Test;

import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;import org.mockito.Mock;
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

        final JsonObject listingPayload = JsonObjects.createObjectBuilder()
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
}
