package uk.gov.moj.cpp.progression.query.api.service;

import static java.util.UUID.randomUUID;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import uk.gov.QueryClientTestBase;
import uk.gov.justice.services.core.requester.Requester;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.Metadata;

import uk.gov.justice.services.messaging.JsonObjects;
import javax.json.JsonObject;

import org.hamcrest.CoreMatchers;
import org.junit.jupiter.api.Test;

import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class CourtOrderServiceTest {

    private static final String DEFENDANT_WITH_NO_COURT_ORDERS_JSON = "json/defendantWithCourtOrders.json";
    private static final String COURT_ORDERS_QUERY = "applicationscourtorders.query.court-order-by-defendant-id";

    @InjectMocks
    private CourtOrderService courtOrderService;

    @Mock
    private Requester requester;

    @Test
    public void shouldReturnCourtOrders() {


        final JsonObject jsonObjectPayload = QueryClientTestBase.readJson(DEFENDANT_WITH_NO_COURT_ORDERS_JSON, JsonObject.class);
        final Metadata metadata = QueryClientTestBase.metadataFor(COURT_ORDERS_QUERY, randomUUID());
        final Envelope envelope = Envelope.envelopeFrom(metadata, jsonObjectPayload);

        when(requester.request(any(), any())).thenReturn(envelope);

        final JsonObject courtOrders = courtOrderService.getCourtOrdersByDefendant(envelope, randomUUID(), requester);
        assertThat(courtOrders.getJsonArray("courtOrders").getJsonObject(0).getString("id"), is("2fc69990-bf59-4c4a-9489-d766b9abde9b"));

    }

    @Test
    public void shouldNotReturnCourtOrders() {


        final JsonObject jsonObjectPayload = JsonObjects.createObjectBuilder().build();
        final Metadata metadata = QueryClientTestBase.metadataFor(COURT_ORDERS_QUERY, randomUUID());
        final Envelope envelope = Envelope.envelopeFrom(metadata, jsonObjectPayload);

        when(requester.request(any(), any())).thenReturn(envelope);

        final JsonObject courtOrders = courtOrderService.getCourtOrdersByDefendant(envelope, randomUUID(), requester);
        assertThat(courtOrders.getJsonArray("courtOrders"), is(CoreMatchers.nullValue()));

    }
}
