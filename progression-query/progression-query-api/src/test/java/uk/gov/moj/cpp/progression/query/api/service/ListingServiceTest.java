package uk.gov.moj.cpp.progression.query.api.service;

import static java.util.UUID.randomUUID;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static uk.gov.QueryClientTestBase.readJson;
import static uk.gov.justice.services.core.enveloper.Enveloper.envelop;
import static uk.gov.justice.services.messaging.Envelope.metadataBuilder;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.justice.services.test.utils.core.messaging.MetadataBuilderFactory.metadataWithRandomUUID;

import uk.gov.justice.api.resource.service.ReferenceDataService;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.core.requester.Requester;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.messaging.MetadataBuilder;
import uk.gov.justice.services.test.utils.core.enveloper.EnveloperFactory;
import uk.gov.justice.services.test.utils.core.messaging.JsonEnvelopeBuilder;

import java.util.Optional;

import javax.json.Json;
import javax.json.JsonObject;

import org.junit.jupiter.api.Test;

import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class ListingServiceTest {

    public static final String JSON_CLUSTER_ORGANISATION_RESPONSEJSON = "json/refdataQueryClusterOrganisationUnitsResponse.json";

    @Mock
    private Requester requester;

    @InjectMocks
    private ListingService listingService;

    @Mock
    ReferenceDataService referenceDataService;

    @Spy
    Enveloper enveloper = EnveloperFactory.createEnveloper();

    @Test
    public void shouldSearchTrialReadiness() {

        final MetadataBuilder metadataBuilder = metadataBuilder()
                .withName("progression.query.search-trial-readiness")
                .withId(randomUUID());

        final JsonEnvelope query = JsonEnvelopeBuilder.envelope()
                .with(metadataBuilder)
                .build();

        final JsonObject listingPayload = Json.createObjectBuilder()
                .add("id", "id-value")
                .build();

        final JsonEnvelope requestEnvelope = envelopeFrom(
                metadataWithRandomUUID("listing.cotr.search.hearings")
                        .withUserId(randomUUID().toString()), listingPayload);

        final Envelope<JsonObject> response = envelop(listingPayload)
                .withName("listing.cotr.search.hearings")
                .withMetadataFrom(requestEnvelope);

        when(requester.requestAsAdmin(any(), eq(JsonObject.class))).thenReturn(response);

        final Optional<JsonObject> result = listingService.searchTrialReadiness(query);

        assertThat(result.get().getString("id"), is("id-value"));
    }

    @Test
    public void shouldSearchTrialReadinessWithClusterId() {
        final MetadataBuilder metadataBuilder = metadataBuilder()
                .withName("progression.query.search-trial-readiness")
                .withId(randomUUID());

        final JsonEnvelope query = JsonEnvelopeBuilder.envelope()
                .with(metadataBuilder)
                .withPayloadOf("53b3c80f-57ea-3915-8b2d-457291d94d9a","clusterId")
                .build();

        final JsonObject listingPayload = Json.createObjectBuilder()
                .add("id", "id-value")
                .build();

        final JsonEnvelope requestEnvelope = envelopeFrom(
                metadataWithRandomUUID("listing.cotr.search.hearings")
                        .withUserId(randomUUID().toString()), listingPayload);

        final Envelope<JsonObject> response = envelop(listingPayload)
                .withName("listing.cotr.search.hearings")
                .withMetadataFrom(requestEnvelope);

        when(requester.requestAsAdmin(any(), eq(JsonObject.class))).thenReturn(response);

        final Optional<JsonObject> result = listingService.searchTrialReadiness(query);

        assertThat(result.get().getString("id"), is("id-value"));
    }

    private JsonEnvelope buildResponseClusterOrganisationUnit(){
        final JsonObject clusterResponse = readJson(JSON_CLUSTER_ORGANISATION_RESPONSEJSON,JsonObject.class);

        final JsonEnvelope responseEnvelope = envelopeFrom(
                metadataWithRandomUUID("referencedata.query.cluster-org-units")
                        .withUserId(randomUUID().toString()), clusterResponse);
        return responseEnvelope;
    }
}
