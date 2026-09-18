package uk.gov.moj.cpp.prosecutioncase.persistence;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import uk.gov.justice.services.test.utils.persistence.HibernateTestEntityManagerProvider;
import uk.gov.moj.cpp.prosecutioncase.persistence.entity.CourtDocumentMaterialEntity;
import uk.gov.moj.cpp.prosecutioncase.persistence.repository.CourtDocumentMaterialRepository;

import java.util.ArrayList;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

public class CourtDocumentMaterialRepositoryTest {

    @RegisterExtension
    static HibernateTestEntityManagerProvider hibernateTestEntityManagerProvider =
            new HibernateTestEntityManagerProvider("progression-test-persistence-unit");

    private CourtDocumentMaterialRepository repository;

    @BeforeEach
    void createRepositoriesWithATestEntityManager() {
        repository = new CourtDocumentMaterialRepository();
        hibernateTestEntityManagerProvider.injectEntityManagerInto(repository);
    }

    @Test
    public void shouldFindByCourtDocumentId() {

        final UUID courtDocumentId1 = UUID.randomUUID();
        final UUID courtDocumentId2 = UUID.randomUUID();
        final UUID materialId1 = UUID.randomUUID();
        final UUID materialId2 = UUID.randomUUID();

        repository.save(getCourtDocumentMaterialEntity(courtDocumentId1, materialId1));
        repository.save(getCourtDocumentMaterialEntity(courtDocumentId2, materialId2));

        CourtDocumentMaterialEntity byCourtDocumentId = repository.findOptionalByCourtDocumentId(courtDocumentId2);
        assertEquals(materialId2, byCourtDocumentId.getMaterialId());

        byCourtDocumentId = repository.findOptionalByCourtDocumentId(UUID.randomUUID());
        assertNull(byCourtDocumentId);
    }

    private CourtDocumentMaterialEntity getCourtDocumentMaterialEntity(final UUID courtDocumentId, final UUID materialId) {
        CourtDocumentMaterialEntity courtDocumentMaterialEntity = new CourtDocumentMaterialEntity();
        courtDocumentMaterialEntity.setCourtDocumentId(courtDocumentId);
        courtDocumentMaterialEntity.setMaterialId(materialId);
        courtDocumentMaterialEntity.setUserGroups(new ArrayList<>());
        return courtDocumentMaterialEntity;
    }

}
