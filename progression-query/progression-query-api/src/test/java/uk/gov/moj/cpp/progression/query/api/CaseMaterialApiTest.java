
package uk.gov.moj.cpp.progression.query.api;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.progression.query.MaterialBulkQueryView;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonObject;
import java.util.UUID;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.justice.services.messaging.JsonEnvelope.metadataBuilder;

@ExtendWith(MockitoExtension.class)
class CaseMaterialApiTest {

    @Mock
    private MaterialBulkQueryView materialBulkQueryView;

    @InjectMocks
    private CaseMaterialApi caseMaterialApi;

    @Test
    void shouldReturnEnvelopeForBulkMaterialQuery() {
        // Given
        final UUID metadataId = UUID.randomUUID();
        final String metadataName = "progression.query.material-content-bulk";

        final JsonArray materialIdsArray = Json.createArrayBuilder()
                .add(Json.createObjectBuilder().add("materialId", UUID.randomUUID().toString()))
                .add(Json.createObjectBuilder().add("materialId", UUID.randomUUID().toString()))
                .build();

        final JsonObject payload = Json.createObjectBuilder()
                .add("materialIds", materialIdsArray)
                .build();

        final JsonEnvelope envelope = envelopeFrom(
                metadataBuilder().withId(metadataId).withName(metadataName),
                payload
        );

        final JsonEnvelope expectedResponse = envelopeFrom(
                metadataBuilder().withId(metadataId).withName(metadataName),
                payload
        );

        when(materialBulkQueryView.findMaterialIdMappingsInBulk(envelope)).thenReturn(expectedResponse);

        // When
        final JsonEnvelope result = caseMaterialApi.findMaterialIdMappingsInBulk(envelope);

        // Then
        assertThat(result, is(notNullValue()));
        assertThat(result.metadata().id(), equalTo(metadataId));
        assertThat(result.metadata().name(), equalTo(metadataName));
        verify(materialBulkQueryView).findMaterialIdMappingsInBulk(envelope);
    }

    @Test
    void shouldPreserveEnvelopeMetadataAndPayload() {
        // Given
        final UUID metadataId = UUID.randomUUID();
        final String metadataName = "progression.query.material-content-bulk";
        final UUID materialId1 = UUID.fromString("a1b2c3d4-e5f6-4a7b-8c9d-0e1f2a3b4c5d");
        final UUID materialId2 = UUID.fromString("f6a7b8c9-d0e1-4f2a-3b4c-5d6e7f8a9b0c");

        final JsonArray materialIdsArray = Json.createArrayBuilder()
                .add(Json.createObjectBuilder().add("materialId", materialId1.toString()))
                .add(Json.createObjectBuilder().add("materialId", materialId2.toString()))
                .build();

        final JsonObject payload = Json.createObjectBuilder()
                .add("materialIds", materialIdsArray)
                .build();

        final JsonEnvelope envelope = envelopeFrom(
                metadataBuilder().withId(metadataId).withName(metadataName),
                payload
        );

        final JsonObject responsePayload = Json.createObjectBuilder()
                .add("materialIds", Json.createArrayBuilder()
                        .add(Json.createObjectBuilder()
                                .add("materialId", materialId1.toString())
                                .add("courtDocumentId", UUID.randomUUID().toString())
                                .add("caseId", UUID.randomUUID().toString())
                                .add("caseUrn", "39GD1116822"))
                        .add(Json.createObjectBuilder()
                                .add("materialId", materialId2.toString())
                                .add("courtDocumentId", UUID.randomUUID().toString())
                                .add("caseId", UUID.randomUUID().toString())
                                .add("caseUrn", "TFL122222"))
                        .build())
                .build();

        final JsonEnvelope expectedResponse = envelopeFrom(
                metadataBuilder().withId(metadataId).withName(metadataName),
                responsePayload
        );

        when(materialBulkQueryView.findMaterialIdMappingsInBulk(envelope)).thenReturn(expectedResponse);

        // When
        final JsonEnvelope result = caseMaterialApi.findMaterialIdMappingsInBulk(envelope);

        // Then
        assertThat(result, is(notNullValue()));
        assertThat(result.metadata(), is(notNullValue()));
        assertThat(result.metadata().id(), equalTo(metadataId));
        assertThat(result.metadata().name(), equalTo(metadataName));

        final JsonObject resultPayload = result.payloadAsJsonObject();
        assertThat(resultPayload.containsKey("materialIds"), is(true));

        final JsonArray resultMaterialIds = resultPayload.getJsonArray("materialIds");
        assertThat(resultMaterialIds.size(), equalTo(2));

        verify(materialBulkQueryView).findMaterialIdMappingsInBulk(envelope);
    }

    @Test
    void shouldHandleEmptyMaterialIdsArray() {
        // Given
        final JsonArray emptyMaterialIdsArray = Json.createArrayBuilder().build();

        final JsonObject payload = Json.createObjectBuilder()
                .add("materialIds", emptyMaterialIdsArray)
                .build();

        final JsonEnvelope envelope = envelopeFrom(
                metadataBuilder().withId(UUID.randomUUID()).withName("progression.query.material-content-bulk"),
                payload
        );

        final JsonObject responsePayload = Json.createObjectBuilder()
                .add("materialIds", Json.createArrayBuilder().build())
                .build();

        final JsonEnvelope expectedResponse = envelopeFrom(
                metadataBuilder().withId(envelope.metadata().id()).withName("progression.query.material-content-bulk"),
                responsePayload
        );

        when(materialBulkQueryView.findMaterialIdMappingsInBulk(envelope)).thenReturn(expectedResponse);

        // When
        final JsonEnvelope result = caseMaterialApi.findMaterialIdMappingsInBulk(envelope);

        // Then
        assertThat(result, is(notNullValue()));
        assertThat(result.payloadAsJsonObject().getJsonArray("materialIds").size(), equalTo(0));
        verify(materialBulkQueryView).findMaterialIdMappingsInBulk(envelope);
    }

    @Test
    void shouldDelegateToMaterialBulkQueryView() {
        // Given
        final JsonObject payload = Json.createObjectBuilder()
                .add("materialIds", Json.createArrayBuilder()
                        .add(Json.createObjectBuilder().add("materialId", UUID.randomUUID().toString()))
                        .build())
                .build();

        final JsonEnvelope envelope = envelopeFrom(
                metadataBuilder().withId(UUID.randomUUID()).withName("progression.query.material-content-bulk"),
                payload
        );

        when(materialBulkQueryView.findMaterialIdMappingsInBulk(envelope)).thenReturn(envelope);

        // When
        final JsonEnvelope result = caseMaterialApi.findMaterialIdMappingsInBulk(envelope);

        // Then
        assertThat(result, is(notNullValue()));
        verify(materialBulkQueryView).findMaterialIdMappingsInBulk(envelope);
    }

    @Test
    void shouldHandleSingleMaterialId() {
        // Given
        final UUID materialId = UUID.fromString("a1b2c3d4-e5f6-4a7b-8c9d-0e1f2a3b4c5d");

        final JsonArray materialIdsArray = Json.createArrayBuilder()
                .add(Json.createObjectBuilder().add("materialId", materialId.toString()))
                .build();

        final JsonObject payload = Json.createObjectBuilder()
                .add("materialIds", materialIdsArray)
                .build();

        final JsonEnvelope envelope = envelopeFrom(
                metadataBuilder().withId(UUID.randomUUID()).withName("progression.query.material-content-bulk"),
                payload
        );

        final JsonObject responsePayload = Json.createObjectBuilder()
                .add("materialIds", Json.createArrayBuilder()
                        .add(Json.createObjectBuilder()
                                .add("materialId", materialId.toString())
                                .add("courtDocumentId", UUID.randomUUID().toString())
                                .add("caseId", UUID.randomUUID().toString())
                                .add("caseUrn", "39GD1116822"))
                        .build())
                .build();

        final JsonEnvelope expectedResponse = envelopeFrom(
                metadataBuilder().withId(envelope.metadata().id()).withName("progression.query.material-content-bulk"),
                responsePayload
        );

        when(materialBulkQueryView.findMaterialIdMappingsInBulk(envelope)).thenReturn(expectedResponse);

        // When
        final JsonEnvelope result = caseMaterialApi.findMaterialIdMappingsInBulk(envelope);

        // Then
        assertThat(result, is(notNullValue()));
        final JsonArray resultMaterialIds = result.payloadAsJsonObject().getJsonArray("materialIds");
        assertThat(resultMaterialIds.size(), equalTo(1));
        verify(materialBulkQueryView).findMaterialIdMappingsInBulk(envelope);
    }
}