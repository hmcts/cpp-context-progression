package uk.gov.moj.cpp.prosecutioncase.persistence.repository;

import static org.hamcrest.MatcherAssert.assertThat;
import static java.util.Optional.ofNullable;
import static java.util.UUID.randomUUID;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.containsInAnyOrder;

import uk.gov.justice.services.test.utils.persistence.HibernateTestEntityManagerProvider;
import uk.gov.moj.cpp.progression.domain.pojo.SearchCriteria;
import uk.gov.moj.cpp.prosecutioncase.persistence.entity.CourtDocumentEntity;
import uk.gov.moj.cpp.prosecutioncase.persistence.entity.CourtDocumentIndexEntity;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

public class CourtDocumentIndexCriteriaRepositoryTest {

    @RegisterExtension
    static HibernateTestEntityManagerProvider hibernateTestEntityManagerProvider =
            new HibernateTestEntityManagerProvider("progression-test-persistence-unit");

    final static UUID courtDocumentId1 = randomUUID();
    final static UUID courtDocumentId2 = randomUUID();
    final static UUID courtDocumentId3 = randomUUID();
    final static UUID caseId = randomUUID();
    final static UUID defendantId1 = randomUUID();
    final static UUID defendantId2 = randomUUID();
    final static String courtDocumentName1 = "AbcDocument Name1";
    final static String courtDocumentName2 = "BcdDocument Name2";
    final static String courtDocumentName3 = "CdeDocument Name3";

    private CourtDocumentIndexCriteriaRepository courtDocumentIndexCriteriaRepository;

    private CourtDocumentRepository courtDocumentRepository;

    @BeforeEach
    void createRepositoriesWithATestEntityManager() {
        courtDocumentIndexCriteriaRepository = new CourtDocumentIndexCriteriaRepository();
        hibernateTestEntityManagerProvider.injectEntityManagerInto(courtDocumentIndexCriteriaRepository);
        courtDocumentRepository = new CourtDocumentRepository();
        hibernateTestEntityManagerProvider.injectEntityManagerInto(courtDocumentRepository);
    }

    @BeforeEach
    public void init() {
        persistCourtDocument(courtDocumentId1, caseId, defendantId1, courtDocumentName1);
        persistCourtDocument(courtDocumentId2, caseId, defendantId1, courtDocumentName2);
        persistCourtDocument(courtDocumentId3, caseId, defendantId2, courtDocumentName3);
    }
    @AfterEach
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

    /**
     * The section-id filter is the one predicate here built from PostgreSQL jsonb functions, so it
     * is the one H2 cannot run as-is. Registering the two functions as H2 aliases lets the query be
     * executed rather than only built — see H2JsonbFunctions for what they do and do not cover.
     */
    @Test
    public void shouldNarrowTheSearchToOneDocumentTypeUsingTheJsonbPayload() {
        registerJsonbFunctionsOnH2();

        final SearchCriteria bySectionId = SearchCriteria.searchCriteria()
                .withCaseId(caseId)
                .withDefendantId(ofNullable(defendantId1))
                .withSectionId(ofNullable("460fbc00-c002-11e8-a355-529269fb1459"))
                .withDocumentName(ofNullable(null))
                .build();

        final List<CourtDocumentIndexEntity> found =
                courtDocumentIndexCriteriaRepository.getCourtDocumentIndexByCriteria(bySectionId);

        assertThat(found.size(), is(2));
        assertThat(found.stream().map(entity -> entity.getCourtDocument().getCourtDocumentId()).toList(),
                containsInAnyOrder(courtDocumentId1, courtDocumentId2));
    }

    @Test
    public void shouldFindNothingWhenNoDocumentHasThatDocumentType() {
        registerJsonbFunctionsOnH2();

        final SearchCriteria byAnotherSectionId = SearchCriteria.searchCriteria()
                .withCaseId(caseId)
                .withDefendantId(ofNullable(defendantId1))
                .withSectionId(ofNullable(randomUUID().toString()))
                .withDocumentName(ofNullable(null))
                .build();

        assertThat(courtDocumentIndexCriteriaRepository.getCourtDocumentIndexByCriteria(byAnotherSectionId).size(),
                is(0));
    }

    private void registerJsonbFunctionsOnH2() {
        final String functions = H2JsonbFunctions.class.getName();
        hibernateTestEntityManagerProvider.getEntityManager()
                .createNativeQuery("CREATE ALIAS IF NOT EXISTS jsonb FOR \"" + functions + ".jsonb\"")
                .executeUpdate();
        hibernateTestEntityManagerProvider.getEntityManager()
                .createNativeQuery("CREATE ALIAS IF NOT EXISTS jsonb_extract_path_text FOR \""
                        + functions + ".jsonbExtractPathText\"")
                .executeUpdate();
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

    /**
     * This repository is a search over indices, so nothing else here saves through it. Its inherited
     * save() still has to work, and idOf is what decides whether that persists or merges.
     */
    @Test
    public void shouldSaveAnIndexThroughTheCriteriaRepositoryAndReadItBack() {
        final UUID courtDocumentId = randomUUID();
        final CourtDocumentEntity courtDocumentEntity = new CourtDocumentEntity();
        courtDocumentEntity.setCourtDocumentId(courtDocumentId);
        courtDocumentEntity.setIsRemoved(false);
        courtDocumentEntity.setPayload("{}");
        courtDocumentRepository.saveAndFlush(courtDocumentEntity);

        final CourtDocumentIndexEntity index =
                getCourtDocumentIndexEntity(courtDocumentEntity, randomUUID(), randomUUID(), courtDocumentId);
        courtDocumentIndexCriteriaRepository.saveAndFlush(index);

        assertThat(courtDocumentIndexCriteriaRepository.findBy(index.getId()).getId(), is(index.getId()));
    }

}
