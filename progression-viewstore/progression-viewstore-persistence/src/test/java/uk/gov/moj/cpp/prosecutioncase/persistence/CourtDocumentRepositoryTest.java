package uk.gov.moj.cpp.prosecutioncase.persistence;

import static com.google.common.collect.Lists.newArrayList;
import static java.util.Collections.singletonList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import uk.gov.moj.cpp.prosecutioncase.persistence.entity.CourtDocumentEntity;
import uk.gov.moj.cpp.prosecutioncase.persistence.entity.CourtDocumentIndexEntity;
import uk.gov.moj.cpp.prosecutioncase.persistence.repository.CourtDocumentIndexRepository;
import uk.gov.moj.cpp.prosecutioncase.persistence.repository.CourtDocumentRepository;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.UUID;

import javax.inject.Inject;

import org.apache.deltaspike.testcontrol.api.junit.CdiTestRunner;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * DB integration tests for {@link CourtDocumentEntity} class
 */


@RunWith(CdiTestRunner.class)
public class CourtDocumentRepositoryTest {

    private static final String PAYLOAD = "{\"removed\":false,\"courtDocumentId\":\"7aa6d35d-70c3-45fb-a05e-bfedbce16412\",\"name\":\"SJP Notice\",\"documentTypeId\":\"0bb7b276-9dc0-4af2-83b9-f4acef0c7898\",\"documentTypeDescription\":\"SJP Notice\",\"mimeType\":\"pdf\",\"isRemoved\":false}";
    private static final UUID CASE_ID = UUID.randomUUID();
    private static final UUID CASE_ID_1 = UUID.randomUUID();
    private static final UUID CASE_ID_2 = UUID.randomUUID();
    private static final UUID CASE_ID_3 = UUID.randomUUID();
    private static final UUID COURT_DOCUMENT_ID = UUID.randomUUID();
    private static final UUID APPLICATION_ID = UUID.randomUUID();
    private static final UUID HEARING_ID = UUID.randomUUID();
    private static final UUID DEFENDANT_ID_1 = UUID.randomUUID();
    private static final UUID DEFENDANT_ID_2 = UUID.randomUUID();


    @Inject
    private CourtDocumentRepository repository;

    @Inject
    private CourtDocumentIndexRepository courtDocumentIndexRepository;

    @Test
    public void shouldFindOptionalBy() throws Exception {
        //given
        repository.save(getProsecutionCase(true));

        final List<CourtDocumentEntity> actual = repository.findByProsecutionCaseId(CASE_ID);
        assertNotNull(actual, "Should not be null");
        assertEquals(CASE_ID, actual.get(0).getIndices().iterator().next().getProsecutionCaseId());
        assertEquals(COURT_DOCUMENT_ID, actual.get(0).getCourtDocumentId());
        assertEquals(true, actual.get(0).getContainsFinancialMeans());

    }

    @Test
    public void shouldFindByCaseIds() throws Exception {
        repository.save(getProsecutionCase(true));
        final List<CourtDocumentEntity> actual = repository.findByProsecutionCaseIds(singletonList(CASE_ID));
        assertNotNull(actual, "Should not be null");
        assertEquals(CASE_ID, actual.get(0).getIndices().iterator().next().getProsecutionCaseId());
        assertEquals(COURT_DOCUMENT_ID, actual.get(0).getCourtDocumentId());
        assertEquals(true, actual.get(0).getContainsFinancialMeans());
    }

    @Test
    public void testFindByApplicationId() {
        repository.save(getProsecutionCaseForApplication(null));

        final List<CourtDocumentEntity> actual = repository.findByApplicationId(APPLICATION_ID);
        assertEquals(APPLICATION_ID, actual.get(0).getIndices().iterator().next().getApplicationId());
        assertEquals(COURT_DOCUMENT_ID, actual.get(0).getCourtDocumentId());
        assertEquals(DocumentCategoryEnum.APPLICATION_DOCUMENT.toString(), actual.get(0).getIndices().iterator().next().getDocumentCategory());
        assertEquals(false, actual.get(0).getContainsFinancialMeans());
    }

    @Test
    public void shouldTestFindByDefendantId() {
        repository.save(getProsecutioncaseForDefendantDocument(CASE_ID_1, DEFENDANT_ID_1));
        List<CourtDocumentEntity> actual = repository.findByDefendantId(DEFENDANT_ID_1);
        assertEquals(CASE_ID_1, actual.get(0).getIndices().iterator().next().getProsecutionCaseId());
        assertEquals(DEFENDANT_ID_1, actual.get(0).getIndices().iterator().next().getDefendantId());

    }

    @Test
    public void testFindByApplicationIds() {
        repository.save(getProsecutionCaseForApplication(null));

        final List<CourtDocumentEntity> actual = repository.findByApplicationIds(singletonList(APPLICATION_ID));
        assertEquals(APPLICATION_ID, actual.get(0).getIndices().iterator().next().getApplicationId());
        assertEquals(COURT_DOCUMENT_ID, actual.get(0).getCourtDocumentId());
        assertEquals(DocumentCategoryEnum.APPLICATION_DOCUMENT.toString(), actual.get(0).getIndices().iterator().next().getDocumentCategory());
        assertEquals(false, actual.get(0).getContainsFinancialMeans());
    }


    @Test
    public void testFindByApplicationIdOrderingSeqNumASC() {

        repository.save(getProsecutionCaseForApplicationWithSeqNum(CASE_ID_3, DEFENDANT_ID_1, 20));
        repository.save(getProsecutionCaseForApplicationWithSeqNum(CASE_ID_3, DEFENDANT_ID_2, 10));
        repository.save(getProsecutionCaseForApplicationWithSeqNum(CASE_ID_3, DEFENDANT_ID_2, 30));

        final List<CourtDocumentEntity> actual = repository.findByProsecutionCaseId(CASE_ID_3);
        assertEquals(10, actual.get(0).getSeqNum().intValue());
        assertEquals(20, actual.get(1).getSeqNum().intValue());
        assertEquals(30, actual.get(2).getSeqNum().intValue());
    }

    @Test
    public void testFindByHearingId() {
        repository.save(getProsecutionCaseForNowDocument(false));
        final List<CourtDocumentEntity> actual = repository.findCourtDocumentForNow(HEARING_ID, "NOW_DOCUMENT", DEFENDANT_ID_1);
        assertNotNull(actual.get(0).getIndices().iterator().next().getHearingId());
        assertEquals(HEARING_ID, actual.get(0).getIndices().iterator().next().getHearingId());
        assertEquals(DocumentCategoryEnum.NOW_DOCUMENT.toString(), actual.get(0).getIndices().iterator().next().getDocumentCategory());
    }


    @Test
    public void testFindByCaseIdAndDefendantid() {

        repository.save(getProsecutioncaseForDefendantDocument(CASE_ID_1, DEFENDANT_ID_1));
        repository.save(getProsecutioncaseForDefendantDocument(CASE_ID_2, DEFENDANT_ID_2));

        List<CourtDocumentEntity> actual = repository.findByProsecutionCaseIdAndDefendantId(newArrayList(CASE_ID_1), newArrayList(DEFENDANT_ID_1));
        assertEquals(CASE_ID_1, actual.get(0).getIndices().iterator().next().getProsecutionCaseId());
        assertEquals(DEFENDANT_ID_1, actual.get(0).getIndices().iterator().next().getDefendantId());

        actual = repository.findByProsecutionCaseIdAndDefendantId(newArrayList(CASE_ID_1), newArrayList(DEFENDANT_ID_2));
        assertEquals(0, actual.size());
    }

    @Test
    public void ensure_that_when_a_court_document_is_deleted_the_orphan_indexes_are_too() {

        //Given
        final CourtDocumentEntity courtDocument = getProsecutionCase(true);
        repository.save(courtDocument);
        //ensure that this saves both the court document and the index and that these are
        //both retrievable
        //court document
        final List<CourtDocumentEntity> courtDocumentsByCaseAndDefendant = repository.findByProsecutionCaseIdAndDefendantId(newArrayList(CASE_ID), newArrayList(DEFENDANT_ID_1));
        assertThat(courtDocumentsByCaseAndDefendant, hasSize(1));
        assertThat(courtDocumentsByCaseAndDefendant.get(0).getCourtDocumentId(), is(COURT_DOCUMENT_ID));
        final List<CourtDocumentEntity> courtDocumentsByCase = repository.findByProsecutionCaseId(CASE_ID);
        assertThat(courtDocumentsByCase, hasSize(1));
        assertThat(courtDocumentsByCase.get(0).getCourtDocumentId(), is(COURT_DOCUMENT_ID));
        //court document index
        final CourtDocumentIndexEntity courtDocumentIndexEntity = courtDocumentIndexRepository.findByCaseIdDefendantIdAndCaseDocumentId(CASE_ID, DEFENDANT_ID_1, COURT_DOCUMENT_ID);
        assertThat(courtDocumentIndexEntity, notNullValue());
        assertThat(courtDocumentIndexEntity.getCourtDocument().getCourtDocumentId(), is(COURT_DOCUMENT_ID));
        final UUID COURT_DOCUMENT_INDEX_ID = courtDocumentIndexEntity.getId();
        assertThat(courtDocumentIndexRepository.findBy(COURT_DOCUMENT_INDEX_ID), notNullValue());
        assertThat(courtDocumentIndexRepository.findBy(COURT_DOCUMENT_INDEX_ID).getId(), is(COURT_DOCUMENT_INDEX_ID));

        //When
        repository.remove(courtDocumentsByCaseAndDefendant.get(0));

        //Then
        assertThat(repository.findByProsecutionCaseIdAndDefendantId(newArrayList(CASE_ID), newArrayList(DEFENDANT_ID_1)), hasSize(0));
        assertThat(repository.findByProsecutionCaseId(CASE_ID), hasSize(0));
        assertThat(courtDocumentIndexRepository.findBy(COURT_DOCUMENT_INDEX_ID), nullValue());
        assertThat(courtDocumentIndexRepository
                        .findByCaseIdDefendantIdAndCaseDocumentId(CASE_ID, DEFENDANT_ID_1, COURT_DOCUMENT_ID),
                nullValue());
    }


    @Test
    public void shouldTestFindByCourtDocumentsWhenOneOfTheRecordIsNotRemoved() {
        final UUID caseId5 = UUID.randomUUID();
        final UUID defendantId5 = UUID.randomUUID();
        final UUID courtDocumentId1 = UUID.randomUUID();
        final UUID courtDocumentId5 = UUID.randomUUID();
        repository.save(getCourtDocument(courtDocumentId1, CASE_ID_1, DEFENDANT_ID_1, false));
        repository.save(getCourtDocument(courtDocumentId5, caseId5, defendantId5, true));

        final List<UUID> uuidList = Arrays.asList(courtDocumentId1, courtDocumentId5);
        List<CourtDocumentEntity> actual = repository.findByCourtDocumentIdsAndAreNotRemoved(uuidList);
        assertEquals(1, actual.size());
        assertEquals(1, actual.get(0).getIndices().size());
        assertEquals(CASE_ID_1, actual.get(0).getIndices().iterator().next().getProsecutionCaseId());
        assertEquals(DEFENDANT_ID_1, actual.get(0).getIndices().iterator().next().getDefendantId());
    }

    private CourtDocumentEntity getProsecutionCase(Boolean financialMeansFlag) {
        final CourtDocumentEntity courtDocumentEntity = new CourtDocumentEntity();
        courtDocumentEntity.setCourtDocumentId(COURT_DOCUMENT_ID);
        courtDocumentEntity.setIsRemoved(false);
        courtDocumentEntity.setPayload(PAYLOAD);
        final CourtDocumentIndexEntity courtDocumentIndexEntity = new CourtDocumentIndexEntity();
        courtDocumentIndexEntity.setId(UUID.randomUUID());
        courtDocumentIndexEntity.setProsecutionCaseId(CASE_ID);
        courtDocumentIndexEntity.setDefendantId(DEFENDANT_ID_1);
        courtDocumentIndexEntity.setCourtDocument(courtDocumentEntity);
        courtDocumentEntity.setIndices(new HashSet<>());
        courtDocumentEntity.getIndices().add(courtDocumentIndexEntity);
        courtDocumentEntity.setContainsFinancialMeans(financialMeansFlag);
        return courtDocumentEntity;
    }

    private CourtDocumentEntity getProsecutionCaseForApplication(Boolean financialMeansFlag) {
        final CourtDocumentEntity courtDocumentEntity = new CourtDocumentEntity();
        courtDocumentEntity.setCourtDocumentId(COURT_DOCUMENT_ID);
        courtDocumentEntity.setIsRemoved(false);
        courtDocumentEntity.setPayload(PAYLOAD);
        final CourtDocumentIndexEntity courtDocumentIndexEntity = new CourtDocumentIndexEntity();
        courtDocumentIndexEntity.setId(UUID.randomUUID());
        courtDocumentIndexEntity.setProsecutionCaseId(CASE_ID);
        courtDocumentIndexEntity.setApplicationId(APPLICATION_ID);
        courtDocumentIndexEntity.setDocumentCategory(DocumentCategoryEnum.APPLICATION_DOCUMENT.toString());
        courtDocumentIndexEntity.setCourtDocument(courtDocumentEntity);
        courtDocumentEntity.setIndices(new HashSet<>());
        courtDocumentEntity.getIndices().add(courtDocumentIndexEntity);
        courtDocumentEntity.setContainsFinancialMeans(financialMeansFlag);
        return courtDocumentEntity;
    }

    private CourtDocumentEntity getProsecutionCaseForApplicationWithSeqNum(UUID caseId, UUID defendantId, Integer seqNumber) {
        final CourtDocumentEntity courtDocumentEntity = new CourtDocumentEntity();
        courtDocumentEntity.setCourtDocumentId(UUID.randomUUID());
        courtDocumentEntity.setIsRemoved(false);
        courtDocumentEntity.setPayload(PAYLOAD);
        courtDocumentEntity.setContainsFinancialMeans(true);

        final CourtDocumentIndexEntity courtDocumentIndexEntity = new CourtDocumentIndexEntity();
        courtDocumentIndexEntity.setId(UUID.randomUUID());
        courtDocumentIndexEntity.setDocumentCategory(DocumentCategoryEnum.DEFENDANT_DOCUMENT.toString());
        courtDocumentIndexEntity.setDefendantId(defendantId);
        courtDocumentIndexEntity.setProsecutionCaseId(caseId);
        courtDocumentIndexEntity.setCourtDocument(courtDocumentEntity);
        courtDocumentEntity.setIndices(new HashSet<>());
        courtDocumentEntity.getIndices().add(courtDocumentIndexEntity);
        courtDocumentEntity.setSeqNum(seqNumber);
        return courtDocumentEntity;
    }


    private CourtDocumentEntity getProsecutionCaseForNowDocument(Boolean financialMeansFlag) {
        final CourtDocumentEntity courtDocumentEntity = new CourtDocumentEntity();
        courtDocumentEntity.setCourtDocumentId(COURT_DOCUMENT_ID);
        courtDocumentEntity.setIsRemoved(false);
        courtDocumentEntity.setPayload(PAYLOAD);
        final CourtDocumentIndexEntity courtDocumentIndexEntity = new CourtDocumentIndexEntity();
        courtDocumentIndexEntity.setId(UUID.randomUUID());
        courtDocumentIndexEntity.setDocumentCategory(DocumentCategoryEnum.NOW_DOCUMENT.toString());
        courtDocumentIndexEntity.setHearingId(HEARING_ID);
        courtDocumentIndexEntity.setDefendantId(DEFENDANT_ID_1);
        courtDocumentIndexEntity.setCourtDocument(courtDocumentEntity);
        courtDocumentEntity.setIndices(new HashSet<>());
        courtDocumentEntity.getIndices().add(courtDocumentIndexEntity);
        courtDocumentEntity.setContainsFinancialMeans(financialMeansFlag);
        return courtDocumentEntity;
    }

    private CourtDocumentEntity getCourtDocument(UUID courtDocumentId, UUID caseId, UUID defendantId, boolean removed) {
        final CourtDocumentEntity courtDocumentEntity = new CourtDocumentEntity();
        courtDocumentEntity.setCourtDocumentId(courtDocumentId);
        courtDocumentEntity.setIsRemoved(removed);
        courtDocumentEntity.setPayload(PAYLOAD);
        courtDocumentEntity.setContainsFinancialMeans(true);

        final CourtDocumentIndexEntity courtDocumentIndexEntity = new CourtDocumentIndexEntity();
        courtDocumentIndexEntity.setId(UUID.randomUUID());
        courtDocumentIndexEntity.setDocumentCategory(DocumentCategoryEnum.DEFENDANT_DOCUMENT.toString());
        courtDocumentIndexEntity.setDefendantId(defendantId);
        courtDocumentIndexEntity.setProsecutionCaseId(caseId);
        courtDocumentIndexEntity.setCourtDocument(courtDocumentEntity);
        courtDocumentEntity.setIndices(new HashSet<>());
        courtDocumentEntity.getIndices().add(courtDocumentIndexEntity);
        return courtDocumentEntity;
    }

    private CourtDocumentEntity getProsecutioncaseForDefendantDocument(UUID caseId, UUID defendantId) {
        final CourtDocumentEntity courtDocumentEntity = new CourtDocumentEntity();
        courtDocumentEntity.setCourtDocumentId(UUID.randomUUID());
        courtDocumentEntity.setIsRemoved(false);
        courtDocumentEntity.setPayload(PAYLOAD);
        courtDocumentEntity.setContainsFinancialMeans(true);

        final CourtDocumentIndexEntity courtDocumentIndexEntity = new CourtDocumentIndexEntity();
        courtDocumentIndexEntity.setId(UUID.randomUUID());
        courtDocumentIndexEntity.setDocumentCategory(DocumentCategoryEnum.DEFENDANT_DOCUMENT.toString());
        courtDocumentIndexEntity.setDefendantId(defendantId);
        courtDocumentIndexEntity.setProsecutionCaseId(caseId);
        courtDocumentIndexEntity.setCourtDocument(courtDocumentEntity);
        courtDocumentEntity.setIndices(new HashSet<>());
        courtDocumentEntity.getIndices().add(courtDocumentIndexEntity);
        return courtDocumentEntity;
    }

    private enum DocumentCategoryEnum {

        NOW_DOCUMENT("NOW_DOCUMENT"),
        APPLICATION_DOCUMENT("APPLICATION_DOCUMENT"),
        DEFENDANT_DOCUMENT("DEFENDANT_DOCUMENT"),
        CASE_DOCUMENT("CASE_DOCUMENT");

        private String description;

        private DocumentCategoryEnum(final String description) {
            this.description = description;
        }

        public static DocumentCategoryEnum getCaseStatus(final String value) {
            final DocumentCategoryEnum[] caseStatusArray = DocumentCategoryEnum.values();
            for (final DocumentCategoryEnum caseStatus : caseStatusArray) {
                if (caseStatus.getDescription().equals(value)) {
                    return caseStatus;
                }
            }
            return null;
        }

        public String getDescription() {
            return description;
        }

    }
}
