package uk.gov.moj.cpp.prosecutioncase.persistence;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import uk.gov.moj.cpp.prosecutioncase.persistence.entity.CourtDocumentEntity;
import uk.gov.moj.cpp.prosecutioncase.persistence.entity.CourtDocumentIndexEntity;
import uk.gov.moj.cpp.prosecutioncase.persistence.repository.CourtDocumentIndexRepository;
import uk.gov.moj.cpp.prosecutioncase.persistence.repository.CourtDocumentRepository;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import javax.inject.Inject;

import org.apache.deltaspike.testcontrol.api.junit.CdiTestRunner;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(CdiTestRunner.class)
public class CourtDocumentIndexRepositoryTest {

    @Inject
    private CourtDocumentIndexRepository courtDocumentIndexRepository;

    @Inject
    private CourtDocumentRepository courtDocumentRepository;

    @Test
    public void shouldFindByCaseIdDefendantIdAndCourtDocumentId() {

        final UUID courtDocumentId = UUID.randomUUID();
        final UUID caseId = UUID.randomUUID();
        final UUID defendantId = UUID.randomUUID();

        CourtDocumentEntity courtDocumentEntity = new CourtDocumentEntity();
        courtDocumentEntity.setCourtDocumentId(courtDocumentId);
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

    private CourtDocumentIndexEntity getCourtDocumentIndexEntity(final CourtDocumentEntity courtDocumentEntity, final UUID caseId, final UUID defendantId, final UUID courtDocumentId) {
        CourtDocumentIndexEntity courtDocumentIndexEntity = new CourtDocumentIndexEntity();
        courtDocumentIndexEntity.setId(UUID.randomUUID());
        courtDocumentIndexEntity.setDefendantId(defendantId);
        courtDocumentIndexEntity.setProsecutionCaseId(caseId);
        courtDocumentIndexEntity.setCourtDocument(courtDocumentEntity);
        return courtDocumentIndexEntity;
    }

}
