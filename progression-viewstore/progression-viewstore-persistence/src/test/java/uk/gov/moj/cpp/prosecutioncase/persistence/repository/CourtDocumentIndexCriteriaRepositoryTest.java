package uk.gov.moj.cpp.prosecutioncase.persistence.repository;

import static java.util.Optional.ofNullable;
import static java.util.UUID.randomUUID;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

import uk.gov.moj.cpp.progression.domain.pojo.SearchCriteria;
import uk.gov.moj.cpp.prosecutioncase.persistence.entity.CourtDocumentEntity;
import uk.gov.moj.cpp.prosecutioncase.persistence.entity.CourtDocumentIndexEntity;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import javax.inject.Inject;

import org.apache.deltaspike.testcontrol.api.junit.CdiTestRunner;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(CdiTestRunner.class)
public class CourtDocumentIndexCriteriaRepositoryTest {

    final static UUID courtDocumentId1 = randomUUID();
    final static UUID courtDocumentId2 = randomUUID();
    final static UUID courtDocumentId3 = randomUUID();
    final static UUID caseId = randomUUID();
    final static UUID defendantId1 = randomUUID();
    final static UUID defendantId2 = randomUUID();
    final static String courtDocumentName1 = "AbcDocument Name1";
    final static String courtDocumentName2 = "BcdDocument Name2";
    final static String courtDocumentName3 = "CdeDocument Name3";

    @Inject
    private CourtDocumentIndexCriteriaRepository courtDocumentIndexCriteriaRepository;

    @Inject
    private CourtDocumentRepository courtDocumentRepository;

    @Before
    public void init() {
        persistCourtDocument(courtDocumentId1, caseId, defendantId1, courtDocumentName1);
        persistCourtDocument(courtDocumentId2, caseId, defendantId1, courtDocumentName2);
        persistCourtDocument(courtDocumentId3, caseId, defendantId2, courtDocumentName3);
    }

    @After
    public void clear() {
        courtDocumentIndexCriteriaRepository.findAll().forEach(correspondenceLog -> courtDocumentIndexCriteriaRepository.removeAndFlush(correspondenceLog));
    }


    @Test
    public void shouldGetCountByDefendantId() {
        final Long recordCount = courtDocumentIndexCriteriaRepository.countByCriteria(buildCriteria(defendantId1, null));
        assertThat(recordCount, is(2L));
    }

    @Test
    public void shouldGetDocumentByDefendantId() {
        final List<CourtDocumentIndexEntity> docList = courtDocumentIndexCriteriaRepository.getCourtDocumentIndexByCriteria(buildCriteria(defendantId1, null));
        assertThat(docList.size(), is(2));
        CourtDocumentIndexEntity courtDocumentIndexEntity = docList.get(0);
        assertThat(courtDocumentIndexEntity.getCourtDocument().getName(), is(courtDocumentName1));
        assertThat(courtDocumentIndexEntity.getDefendantId(), is(defendantId1));
        courtDocumentIndexEntity = docList.get(1);
        assertThat(courtDocumentIndexEntity.getCourtDocument().getName(), is(courtDocumentName2));
        assertThat(courtDocumentIndexEntity.getDefendantId(), is(defendantId1));
    }


    @Test
    public void shouldGetCountWithNoFiltering() {
        final Long recordCount = courtDocumentIndexCriteriaRepository.countByCriteria(buildCriteria(null, null));
        assertThat(recordCount, is(3L));
    }

    @Test
    public void shouldGetDocumentWithNoFiltering() {
        final List<CourtDocumentIndexEntity> docList = courtDocumentIndexCriteriaRepository.getCourtDocumentIndexByCriteria(buildCriteria(null, null));
        assertThat(docList.size(), is(3));
        CourtDocumentIndexEntity courtDocumentIndexEntity = docList.get(0);
        assertThat(courtDocumentIndexEntity.getCourtDocument().getName(), is(courtDocumentName1));
        assertThat(courtDocumentIndexEntity.getDefendantId(), is(defendantId1));
        courtDocumentIndexEntity = docList.get(1);
        assertThat(courtDocumentIndexEntity.getCourtDocument().getName(), is(courtDocumentName2));
        assertThat(courtDocumentIndexEntity.getDefendantId(), is(defendantId1));
        courtDocumentIndexEntity = docList.get(2);
        assertThat(courtDocumentIndexEntity.getCourtDocument().getName(), is(courtDocumentName3));
        assertThat(courtDocumentIndexEntity.getDefendantId(), is(defendantId2));
    }

    private SearchCriteria buildCriteria(final UUID defendantId, final String name) {
        return SearchCriteria.searchCriteria()
                .withCaseId(caseId)
                .withDefendantId(ofNullable(defendantId))
                .withDocumentName(ofNullable(name))
                .build();
    }

    private void persistCourtDocument(final UUID courtDocumentId, final UUID caseId, final UUID defendantId, final String courtDocumentName) {
        CourtDocumentEntity courtDocumentEntity = new CourtDocumentEntity();
        courtDocumentEntity.setCourtDocumentId(courtDocumentId);
        courtDocumentEntity.setName(courtDocumentName);
        courtDocumentEntity.setIsRemoved(false);
        courtDocumentEntity.setPayload("{\"documentTypeId\":\"460fbc00-c002-11e8-a355-529269fb1459\"," +
                "\"name\":\"" + courtDocumentName + "\"}");
        final Set<CourtDocumentIndexEntity> indices = new HashSet<>();
        indices.add(getCourtDocumentIndexEntity(courtDocumentEntity, caseId, defendantId, courtDocumentId));
        courtDocumentEntity.setIndices(indices);
        courtDocumentRepository.save(courtDocumentEntity);
    }

    private CourtDocumentIndexEntity getCourtDocumentIndexEntity(final CourtDocumentEntity courtDocumentEntity, final UUID caseId, final UUID defendantId, final UUID courtDocumentId) {
        CourtDocumentIndexEntity courtDocumentIndexEntity = new CourtDocumentIndexEntity();
        courtDocumentIndexEntity.setId(randomUUID());
        courtDocumentIndexEntity.setDefendantId(defendantId);
        courtDocumentIndexEntity.setProsecutionCaseId(caseId);
        courtDocumentIndexEntity.setCourtDocument(courtDocumentEntity);
        return courtDocumentIndexEntity;
    }

}
