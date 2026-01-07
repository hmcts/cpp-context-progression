
package uk.gov.moj.cpp.prosecutioncase.persistence.repository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.moj.cpp.prosecutioncase.persistence.entity.MaterialIdMapping;

import javax.persistence.EntityManager;
import javax.persistence.Query;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MaterialBulkRepositoryTest {

    @Mock
    private EntityManager entityManager;

    @Mock
    private Query query;

    @InjectMocks
    private MaterialBulkRepository materialBulkRepository;

    private UUID materialId1;
    private UUID materialId2;
    private UUID courtDocumentId1;
    private UUID courtDocumentId2;
    private UUID caseId1;
    private UUID caseId2;

    @BeforeEach
    void setUp() {
        materialId1 = UUID.fromString("a1b2c3d4-e5f6-4a7b-8c9d-0e1f2a3b4c5d");
        materialId2 = UUID.fromString("f6a7b8c9-d0e1-4f2a-3b4c-5d6e7f8a9b0c");
        courtDocumentId1 = UUID.fromString("b2c3d4e5-f6a7-4b8c-9d0e-1f2a3b4c5d6e");
        courtDocumentId2 = UUID.fromString("c9d0e1f2-a3b4-4c5d-6e7f-8a9b0c1d2e3f");
        caseId1 = UUID.fromString("c3d4e5f6-a7b8-4c9d-0e1f-2a3b4c5d6e7f");
        caseId2 = UUID.fromString("a7b8c9d0-e1f2-4a3b-4c5d-6e7f8a9b0c1d");
    }


    @Test
    void shouldFindMaterialIdMappingsInBulk() {
        // Given
        final List<UUID> materialIds = Arrays.asList(materialId1, materialId2);

        final MaterialIdMapping mapping1 = new MaterialIdMapping(
                materialId1, courtDocumentId1, caseId1, "39GD1116822"
        );
        final MaterialIdMapping mapping2 = new MaterialIdMapping(
                materialId2, courtDocumentId2, caseId2, "TFL122222"
        );

        final List<MaterialIdMapping> expectedMappings = Arrays.asList(mapping1, mapping2);

        when(entityManager.createNativeQuery(anyString(), eq("MaterialIdMappingResult")))
                .thenReturn(query);
        when(query.getResultList()).thenReturn(expectedMappings);

        // When
        final List<MaterialIdMapping> result = materialBulkRepository.findMaterialIdMappingsInBulk(materialIds);

        // Then
        assertThat(result, is(notNullValue()));
        assertThat(result, hasSize(2));
        assertThat(result.get(0).getMaterialId(), equalTo(materialId1));
        assertThat(result.get(0).getCourtDocumentId(), equalTo(courtDocumentId1));
        assertThat(result.get(0).getCaseId(), equalTo(caseId1));
        assertThat(result.get(0).getCaseUrn(), equalTo("39GD1116822"));

        assertThat(result.get(1).getMaterialId(), equalTo(materialId2));
        assertThat(result.get(1).getCourtDocumentId(), equalTo(courtDocumentId2));
        assertThat(result.get(1).getCaseId(), equalTo(caseId2));
        assertThat(result.get(1).getCaseUrn(), equalTo("TFL122222"));

        verify(entityManager).createNativeQuery(anyString(), eq("MaterialIdMappingResult"));
        verify(query).getResultList();
    }

    @Test
    void shouldReturnEmptyListWhenNoMappingsFound() {
        // Given
        final List<UUID> materialIds = Arrays.asList(materialId1);

        when(entityManager.createNativeQuery(anyString(), eq("MaterialIdMappingResult")))
                .thenReturn(query);
        when(query.getResultList()).thenReturn(Arrays.asList());

        // When
        final List<MaterialIdMapping> result = materialBulkRepository.findMaterialIdMappingsInBulk(materialIds);

        // Then
        assertThat(result, is(notNullValue()));
        assertThat(result, hasSize(0));

        verify(entityManager).createNativeQuery(anyString(), eq("MaterialIdMappingResult"));
        verify(query).getResultList();
    }

    @Test
    void shouldHandleSingleMaterialId() {
        // Given
        final List<UUID> materialIds = Arrays.asList(materialId1);

        final MaterialIdMapping mapping = new MaterialIdMapping(
                materialId1, courtDocumentId1, caseId1, "39GD1116822"
        );

        when(entityManager.createNativeQuery(anyString(), eq("MaterialIdMappingResult")))
                .thenReturn(query);
        when(query.getResultList()).thenReturn(Arrays.asList(mapping));

        // When
        final List<MaterialIdMapping> result = materialBulkRepository.findMaterialIdMappingsInBulk(materialIds);

        // Then
        assertThat(result, is(notNullValue()));
        assertThat(result, hasSize(1));
        assertThat(result.get(0).getMaterialId(), equalTo(materialId1));
        assertThat(result.get(0).getCaseUrn(), equalTo("39GD1116822"));

        verify(query).getResultList();
    }

    @Test
    void shouldHandleMappingsWithNullValues() {
        // Given
        final List<UUID> materialIds = Arrays.asList(materialId1, materialId2);

        final MaterialIdMapping mapping1 = new MaterialIdMapping(
                materialId1, null, null, null
        );
        final MaterialIdMapping mapping2 = new MaterialIdMapping(
                materialId2, courtDocumentId2, caseId2, null
        );

        final List<MaterialIdMapping> expectedMappings = Arrays.asList(mapping1, mapping2);

        when(entityManager.createNativeQuery(anyString(), eq("MaterialIdMappingResult")))
                .thenReturn(query);
        when(query.getResultList()).thenReturn(expectedMappings);

        // When
        final List<MaterialIdMapping> result = materialBulkRepository.findMaterialIdMappingsInBulk(materialIds);

        // Then
        assertThat(result, is(notNullValue()));
        assertThat(result, hasSize(2));
        assertThat(result.get(0).getMaterialId(), equalTo(materialId1));
        assertThat(result.get(0).getCourtDocumentId(), equalTo(null));
        assertThat(result.get(0).getCaseId(), equalTo(null));
        assertThat(result.get(0).getCaseUrn(), equalTo(null));
        assertThat(result.get(1).getMaterialId(), equalTo(materialId2));
        assertThat(result.get(1).getCourtDocumentId(), equalTo(courtDocumentId2));
    }

    @Test
    void shouldOnlyReturnMaterialsWithValidCaseReferences() {
        // Given
        final List<UUID> materialIds = Arrays.asList(materialId1);

        final MaterialIdMapping mapping = new MaterialIdMapping(
                materialId1, courtDocumentId1, caseId1, "39GD1116822"
        );

        when(entityManager.createNativeQuery(anyString(), eq("MaterialIdMappingResult")))
                .thenReturn(query);
        when(query.getResultList()).thenReturn(Arrays.asList(mapping));

        // When
        final List<MaterialIdMapping> result = materialBulkRepository.findMaterialIdMappingsInBulk(materialIds);

        // Then
        assertThat(result, is(notNullValue()));
        assertThat(result, hasSize(1));
        assertThat(result.get(0).getCaseUrn(), is(notNullValue()));
    }

    @Test
    void shouldReturnEmptyListForNullOrEmptyMaterialIds() {
        // Given
        final List<UUID> emptyMaterialIds = List.of();

        // When
        final List<MaterialIdMapping> result = materialBulkRepository.findMaterialIdMappingsInBulk(emptyMaterialIds);

        // Then
        assertThat(result, is(notNullValue()));
        assertThat(result, hasSize(0));
    }

}