package uk.gov.moj.cpp.prosecutioncase.persistence;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import uk.gov.moj.cpp.prosecutioncase.persistence.entity.CourtDocumentEntity;
import uk.gov.moj.cpp.prosecutioncase.persistence.entity.CourtDocumentIndexEntity;
import uk.gov.moj.cpp.prosecutioncase.persistence.entity.CourtDocumentMaterialEntity;
import uk.gov.moj.cpp.prosecutioncase.persistence.repository.CourtDocumentIndexRepository;
import uk.gov.moj.cpp.prosecutioncase.persistence.repository.CourtDocumentMaterialRepository;
import uk.gov.moj.cpp.prosecutioncase.persistence.repository.CourtDocumentRepository;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import javax.inject.Inject;

import org.apache.deltaspike.testcontrol.api.junit.CdiTestRunner;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(CdiTestRunner.class)
public class CourtDocumentIndexRepositoryTest  {

    @Inject
    private CourtDocumentIndexRepository courtDocumentIndexRepository;

    @Inject
    private CourtDocumentRepository courtDocumentRepository;

    @Inject
    private CourtDocumentMaterialRepository courtDocumentMaterialRepository;

    @Test
    public void shouldFindByCaseIdDefendantIdAndCourtDocumentId() {

        final UUID courtDocumentId = UUID.randomUUID();
        final UUID caseId = UUID.randomUUID();
        final UUID defendantId = UUID.randomUUID();

        CourtDocumentEntity courtDocumentEntity = new CourtDocumentEntity();
        courtDocumentEntity.setCourtDocumentId(courtDocumentId);
        courtDocumentEntity.setIsRemoved(false);
        final Set<CourtDocumentIndexEntity> indices = new HashSet<>();
        indices.add(getCourtDocumentIndexEntity(courtDocumentEntity, caseId, defendantId, courtDocumentId));
        courtDocumentEntity.setIndices(indices);
        courtDocumentRepository.save(courtDocumentEntity);

        CourtDocumentIndexEntity byCaseIdDefendantIdAndCaseDocumentId = courtDocumentIndexRepository.findByCaseIdDefendantIdAndCaseDocumentId(caseId, defendantId, courtDocumentId);

        assertEquals(courtDocumentId, byCaseIdDefendantIdAndCaseDocumentId.getCourtDocument().getCourtDocumentId());
        assertEquals(caseId, byCaseIdDefendantIdAndCaseDocumentId.getProsecutionCaseId());
        assertEquals(defendantId, byCaseIdDefendantIdAndCaseDocumentId.getDefendantId());


        final Set<CourtDocumentIndexEntity> courtDocumentIndexEntities = courtDocumentEntity.getIndices();
        courtDocumentIndexEntities.remove(courtDocumentIndexRepository.findByCaseIdDefendantIdAndCaseDocumentId(caseId, defendantId, courtDocumentId));
        courtDocumentEntity.setIndices(courtDocumentIndexEntities);
        courtDocumentRepository.save(courtDocumentEntity);

        byCaseIdDefendantIdAndCaseDocumentId = courtDocumentIndexRepository.findByCaseIdDefendantIdAndCaseDocumentId(caseId, defendantId, courtDocumentId);
        assertNull(byCaseIdDefendantIdAndCaseDocumentId);

    }


    @Test
    public void shouldFindByMaterialId() {

        final UUID courtDocumentId = UUID.randomUUID();
        final UUID materialId = UUID.randomUUID();
        final UUID applicationId = UUID.randomUUID();

        final CourtDocumentMaterialEntity courtDocumentMaterial = new CourtDocumentMaterialEntity();
        courtDocumentMaterial.setMaterialId(materialId);
        courtDocumentMaterial.setCourtDocumentId(courtDocumentId);

        final CourtDocumentEntity courtDocument = new CourtDocumentEntity();
        courtDocument.setIsRemoved(false);
        courtDocument.setCourtDocumentId(courtDocumentId);

        final CourtDocumentIndexEntity courtDocumentIndex = new CourtDocumentIndexEntity();
        courtDocumentIndex.setId(UUID.randomUUID());
        courtDocumentIndex.setApplicationId(applicationId);
        courtDocumentIndex.setCourtDocument(courtDocument);

        final Set<CourtDocumentIndexEntity> indices = new HashSet<>();
        indices.add(courtDocumentIndex);
        courtDocument.setIndices(indices);
        courtDocumentRepository.save(courtDocument);
        courtDocumentMaterialRepository.save(courtDocumentMaterial);

        Optional<CourtDocumentIndexEntity> indexEntity = courtDocumentIndexRepository.findByMaterialId(materialId);

        assertThat(indexEntity.isPresent(), is(true));
        assertThat(indexEntity.get().getCourtDocument().getCourtDocumentId(), is(courtDocumentId));
        assertThat(indexEntity.get().getApplicationId(), is(applicationId));

    }

    @Test
    public void shouldFindByCaseIdDefendantId() {

        final UUID courtDocumentId = UUID.randomUUID();
        final UUID caseId = UUID.randomUUID();
        final UUID defendantId = UUID.randomUUID();
        final UUID hearingId = UUID.randomUUID();

        final CourtDocumentEntity courtDocumentEntity = new CourtDocumentEntity();
        courtDocumentEntity.setCourtDocumentId(courtDocumentId);
        courtDocumentEntity.setIsRemoved(false);
        final Set<CourtDocumentIndexEntity> indices = new HashSet<>();
        indices.add(getCourtDocumentIndexEntity(courtDocumentEntity, caseId, defendantId, courtDocumentId, hearingId));
        courtDocumentEntity.setIndices(indices);
        courtDocumentRepository.save(courtDocumentEntity);

        final List<CourtDocumentIndexEntity> result = courtDocumentIndexRepository.findByProsecutionCaseIdAndDefendantId(caseId, defendantId);

        assertThat(result.size(), is(1));
        final CourtDocumentIndexEntity courtDocumentIndexEntity = result.get(0);
        assertEquals(courtDocumentId, courtDocumentIndexEntity.getCourtDocument().getCourtDocumentId());
        assertEquals(caseId, courtDocumentIndexEntity.getProsecutionCaseId());
        assertEquals(defendantId, courtDocumentIndexEntity.getDefendantId());
        assertEquals(hearingId, courtDocumentIndexEntity.getHearingId());

    }

    @Test
    public void shouldUpdateApplicationIdByApplicationId() {
        final UUID courtDocumentId = UUID.randomUUID();
        final UUID applicationId = UUID.randomUUID();
        final UUID newApplicationId = UUID.randomUUID();
        CourtDocumentEntity courtDocumentEntity = new CourtDocumentEntity();
        courtDocumentEntity.setCourtDocumentId(courtDocumentId);
        courtDocumentEntity.setIsRemoved(false);
        final CourtDocumentIndexEntity courtDocumentIndexEntity = new CourtDocumentIndexEntity();
        courtDocumentIndexEntity.setCourtDocument(courtDocumentEntity);
        courtDocumentIndexEntity.setId(UUID.randomUUID());
        courtDocumentIndexEntity.setApplicationId(applicationId);
        final Set<CourtDocumentIndexEntity> indices = new HashSet<>();
        indices.add(courtDocumentIndexEntity);
        courtDocumentEntity.setIndices(indices);
        courtDocumentRepository.save(courtDocumentEntity);

        courtDocumentIndexRepository.updateApplicationIdByApplicationId(newApplicationId, applicationId);

        final List<CourtDocumentEntity> result = courtDocumentRepository.findByApplicationId(newApplicationId);

        assertThat(result.size(), is(1));
        assertThat(result.get(0).getCourtDocumentId(), is(courtDocumentId));

    }

    private CourtDocumentIndexEntity getCourtDocumentIndexEntity(final CourtDocumentEntity courtDocumentEntity, final UUID caseId, final UUID defendantId, final UUID courtDocumentId) {
        CourtDocumentIndexEntity courtDocumentIndexEntity = new CourtDocumentIndexEntity();
        courtDocumentIndexEntity.setId(UUID.randomUUID());
        courtDocumentIndexEntity.setDefendantId(defendantId);
        courtDocumentIndexEntity.setProsecutionCaseId(caseId);
        courtDocumentIndexEntity.setCourtDocument(courtDocumentEntity);
        return courtDocumentIndexEntity;
    }

    private CourtDocumentIndexEntity getCourtDocumentIndexEntity(final CourtDocumentEntity courtDocumentEntity, final UUID caseId, final UUID defendantId, final UUID courtDocumentId, final UUID hearingId) {
        final CourtDocumentIndexEntity courtDocumentIndexEntity = getCourtDocumentIndexEntity(courtDocumentEntity, caseId, defendantId, courtDocumentId);
        courtDocumentIndexEntity.setHearingId(hearingId);
        return courtDocumentIndexEntity;
    }
}
