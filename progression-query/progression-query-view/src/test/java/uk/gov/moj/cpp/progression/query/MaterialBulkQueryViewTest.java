package uk.gov.moj.cpp.progression.query;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.prosecutioncase.persistence.entity.MaterialIdMapping;
import uk.gov.moj.cpp.prosecutioncase.persistence.repository.MaterialBulkRepository;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonObject;
import java.util.Arrays;
import java.util.UUID;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.justice.services.messaging.JsonEnvelope.metadataBuilder;

@ExtendWith(MockitoExtension.class)
class MaterialBulkQueryViewTest {

    @Mock
    private MaterialBulkRepository materialBulkRepository;

    @InjectMocks
    private MaterialBulkQueryView materialBulkQueryView;

    private UUID materialId1;
    private UUID materialId2;
    private UUID materialId3;
    private UUID courtDocumentId1;
    private UUID courtDocumentId2;
    private UUID caseId1;
    private UUID caseId2;

    @BeforeEach
    void setUp() {
        materialId1 = UUID.fromString("a1b2c3d4-e5f6-4a7b-8c9d-0e1f2a3b4c5d");
        materialId2 = UUID.fromString("f6a7b8c9-d0e1-4f2a-3b4c-5d6e7f8a9b0c");
        materialId3 = UUID.fromString("b8c9d0e1-f2a3-4b4c-5d6e-7f8a9b0c1d2e");
        courtDocumentId1 = UUID.fromString("b2c3d4e5-f6a7-4b8c-9d0e-1f2a3b4c5d6e");
        courtDocumentId2 = UUID.fromString("c9d0e1f2-a3b4-4c5d-6e7f-8a9b0c1d2e3f");
        caseId1 = UUID.fromString("c3d4e5f6-a7b8-4c9d-0e1f-2a3b4c5d6e7f");
        caseId2 = UUID.fromString("a7b8c9d0-e1f2-4a3b-4c5d-6e7f8a9b0c1d");
    }


    @Test
    void shouldProcessBulkMaterialMappingsQuery() {
        // Given
        final String materialIdsString = materialId1.toString() + "," + materialId2.toString();

        final JsonObject payload = Json.createObjectBuilder()
                .add("materialIds", materialIdsString)
                .build();

        final JsonEnvelope envelope = envelopeFrom(
                metadataBuilder().withId(UUID.randomUUID()).withName("progression.query.material-content-bulk"),
                payload
        );

        final MaterialIdMapping mapping1 = new MaterialIdMapping(
                materialId1, courtDocumentId1, caseId1, "39GD1116822"
        );
        final MaterialIdMapping mapping2 = new MaterialIdMapping(
                materialId2, courtDocumentId2, caseId2, "TFL122222"
        );

        when(materialBulkRepository.findMaterialIdMappingsInBulk(anyList()))
                .thenReturn(Arrays.asList(mapping1, mapping2));

        // When
        final JsonEnvelope result = materialBulkQueryView.findMaterialIdMappingsInBulk(envelope);

        // Then
        assertThat(result, is(notNullValue()));
        assertThat(result.metadata(), is(notNullValue()));

        final JsonObject responsePayload = result.payloadAsJsonObject();
        assertThat(responsePayload.containsKey("materialIds"), is(true));

        final JsonArray responseMaterialIds = responsePayload.getJsonArray("materialIds");
        assertThat(responseMaterialIds.size(), equalTo(2));

        final JsonObject firstMaterial = responseMaterialIds.getJsonObject(0);
        assertThat(firstMaterial.getString("materialId"), equalTo(materialId1.toString()));
        assertThat(firstMaterial.getString("courtDocumentId"), equalTo(courtDocumentId1.toString()));
        assertThat(firstMaterial.getString("caseId"), equalTo(caseId1.toString()));
        assertThat(firstMaterial.getString("caseUrn"), equalTo("39GD1116822"));

        final JsonObject secondMaterial = responseMaterialIds.getJsonObject(1);
        assertThat(secondMaterial.getString("materialId"), equalTo(materialId2.toString()));
        assertThat(secondMaterial.getString("courtDocumentId"), equalTo(courtDocumentId2.toString()));
        assertThat(secondMaterial.getString("caseId"), equalTo(caseId2.toString()));
        assertThat(secondMaterial.getString("caseUrn"), equalTo("TFL122222"));

        verify(materialBulkRepository).findMaterialIdMappingsInBulk(anyList());
    }

    @Test
    void shouldHandleNullValuesInMappings() {
        // Given
        final String materialIdsString = materialId3.toString();

        final JsonObject payload = Json.createObjectBuilder()
                .add("materialIds", materialIdsString)
                .build();

        final JsonEnvelope envelope = envelopeFrom(
                metadataBuilder().withId(UUID.randomUUID()).withName("progression.query.material-content-bulk"),
                payload
        );

        final MaterialIdMapping mapping = new MaterialIdMapping(
                materialId3, null, null, null
        );

        when(materialBulkRepository.findMaterialIdMappingsInBulk(anyList()))
                .thenReturn(Arrays.asList(mapping));

        // When
        final JsonEnvelope result = materialBulkQueryView.findMaterialIdMappingsInBulk(envelope);

        // Then
        assertThat(result, is(notNullValue()));

        final JsonObject responsePayload = result.payloadAsJsonObject();
        final JsonArray responseMaterialIds = responsePayload.getJsonArray("materialIds");
        assertThat(responseMaterialIds.size(), equalTo(1));

        final JsonObject material = responseMaterialIds.getJsonObject(0);
        assertThat(material.getString("materialId"), equalTo(materialId3.toString()));
        assertThat(material.isNull("courtDocumentId"), is(true));
        assertThat(material.isNull("caseId"), is(true));
        assertThat(material.isNull("caseUrn"), is(true));
    }

    @Test
    void shouldReturnEmptyArrayWhenNoMappingsFound() {
        // Given
        final String materialIdsString = materialId1.toString();

        final JsonObject payload = Json.createObjectBuilder()
                .add("materialIds", materialIdsString)
                .build();

        final JsonEnvelope envelope = envelopeFrom(
                metadataBuilder().withId(UUID.randomUUID()).withName("progression.query.material-content-bulk"),
                payload
        );

        when(materialBulkRepository.findMaterialIdMappingsInBulk(anyList()))
                .thenReturn(Arrays.asList());

        // When
        final JsonEnvelope result = materialBulkQueryView.findMaterialIdMappingsInBulk(envelope);

        // Then
        assertThat(result, is(notNullValue()));

        final JsonObject responsePayload = result.payloadAsJsonObject();
        final JsonArray responseMaterialIds = responsePayload.getJsonArray("materialIds");
        assertThat(responseMaterialIds.size(), equalTo(0));

        verify(materialBulkRepository).findMaterialIdMappingsInBulk(anyList());
    }

    @Test
    void shouldPreserveEnvelopeMetadata() {
        // Given
        final UUID metadataId = UUID.randomUUID();
        final String metadataName = "progression.query.material-content-bulk";

        final String materialIdsString = materialId1.toString();

        final JsonObject payload = Json.createObjectBuilder()
                .add("materialIds", materialIdsString)
                .build();

        final JsonEnvelope envelope = envelopeFrom(
                metadataBuilder().withId(metadataId).withName(metadataName),
                payload
        );

        when(materialBulkRepository.findMaterialIdMappingsInBulk(anyList()))
                .thenReturn(Arrays.asList());

        // When
        final JsonEnvelope result = materialBulkQueryView.findMaterialIdMappingsInBulk(envelope);

        // Then
        assertThat(result.metadata(), is(notNullValue()));
        assertThat(result.metadata().id(), equalTo(metadataId));
        assertThat(result.metadata().name(), equalTo(metadataName));
    }

    @Test
    void shouldHandleMultipleMaterialIdsWithMixedData() {
        // Given
        final String materialIdsString = materialId1.toString() + "," + materialId2.toString() + "," + materialId3.toString();

        final JsonObject payload = Json.createObjectBuilder()
                .add("materialIds", materialIdsString)
                .build();

        final JsonEnvelope envelope = envelopeFrom(
                metadataBuilder().withId(UUID.randomUUID()).withName("progression.query.material-content-bulk"),
                payload
        );

        final MaterialIdMapping mapping1 = new MaterialIdMapping(
                materialId1, courtDocumentId1, caseId1, "39GD1116822"
        );
        final MaterialIdMapping mapping2 = new MaterialIdMapping(
                materialId2, null, caseId2, "TFL122222"
        );
        final MaterialIdMapping mapping3 = new MaterialIdMapping(
                materialId3, courtDocumentId2, null, null
        );

        when(materialBulkRepository.findMaterialIdMappingsInBulk(anyList()))
                .thenReturn(Arrays.asList(mapping1, mapping2, mapping3));

        // When
        final JsonEnvelope result = materialBulkQueryView.findMaterialIdMappingsInBulk(envelope);

        // Then
        assertThat(result, is(notNullValue()));

        final JsonObject responsePayload = result.payloadAsJsonObject();
        final JsonArray responseMaterialIds = responsePayload.getJsonArray("materialIds");
        assertThat(responseMaterialIds.size(), equalTo(3));
    }
}