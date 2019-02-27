package uk.gov.moj.cpp.prosecutioncase.persistence;

import org.apache.deltaspike.testcontrol.api.junit.CdiTestRunner;
import org.junit.Test;
import org.junit.runner.RunWith;
import uk.gov.moj.cpp.prosecutioncase.persistence.entity.CourtDocumentEntity;
import uk.gov.moj.cpp.prosecutioncase.persistence.entity.CourtDocumentIndexEntity;
import uk.gov.moj.cpp.prosecutioncase.persistence.entity.ProsecutionCaseEntity;
import uk.gov.moj.cpp.prosecutioncase.persistence.repository.CourtDocumentRepository;
import uk.gov.moj.cpp.prosecutioncase.persistence.repository.ProsecutionCaseRepository;

import javax.inject.Inject;
import java.util.HashSet;
import java.util.List;
import java.util.UUID;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * DB integration tests for {@link CourtDocumentEntity} class
 */


@RunWith(CdiTestRunner.class)
public class CourtDocumentRepositoryTest {

    private static final String PAYLOAD = "{\"removed\":false,\"courtDocumentId\":\"7aa6d35d-70c3-45fb-a05e-bfedbce16412\",\"name\":\"SJP Notice\",\"documentTypeId\":\"0bb7b276-9dc0-4af2-83b9-f4acef0c7898\",\"documentTypeDescription\":\"SJP Notice\",\"mimeType\":\"pdf\",\"isRemoved\":false}";
    private static final UUID CASE_ID = UUID.randomUUID();
    private static final UUID COURT_DOCUMENT_ID = UUID.randomUUID();

    @Inject
    private CourtDocumentRepository repository;

    @Test
    public void shouldFindOptionalBy() throws Exception {
        //given
        repository.save(getProsecutionCase());

        final List<CourtDocumentEntity> actual = repository.findByProsecutionCaseId(CASE_ID);
        assertNotNull("Should not be null", actual);
        assertEquals(CASE_ID, actual.get(0).getIndices().iterator().next().getProsecutionCaseId());
        assertEquals(COURT_DOCUMENT_ID, actual.get(0).getCourtDocumentId());
    }


    private CourtDocumentEntity getProsecutionCase() {
        final CourtDocumentEntity courtDocumentEntity = new CourtDocumentEntity();
        courtDocumentEntity.setCourtDocumentId(COURT_DOCUMENT_ID);
        courtDocumentEntity.setPayload(PAYLOAD);
        final CourtDocumentIndexEntity courtDocumentIndexEntity = new CourtDocumentIndexEntity();
        courtDocumentIndexEntity.setId(UUID.randomUUID());
        courtDocumentIndexEntity.setProsecutionCaseId(CASE_ID);
        courtDocumentIndexEntity.setCourtDocument(courtDocumentEntity);
        courtDocumentEntity.setIndices(new HashSet<>());
        courtDocumentEntity.getIndices().add(courtDocumentIndexEntity);
        return courtDocumentEntity;
    }


}
