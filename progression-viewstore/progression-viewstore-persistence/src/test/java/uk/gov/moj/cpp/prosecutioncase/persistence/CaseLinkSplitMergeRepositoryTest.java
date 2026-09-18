package uk.gov.moj.cpp.prosecutioncase.persistence;

import static org.hamcrest.MatcherAssert.assertThat;
import static java.util.UUID.randomUUID;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static uk.gov.justice.services.messaging.JsonObjects.createObjectBuilder;

import uk.gov.justice.services.test.utils.persistence.HibernateTestEntityManagerProvider;
import uk.gov.moj.cpp.progression.domain.event.link.LinkType;
import uk.gov.moj.cpp.prosecutioncase.persistence.entity.CaseLinkSplitMergeEntity;
import uk.gov.moj.cpp.prosecutioncase.persistence.entity.ProsecutionCaseEntity;
import uk.gov.moj.cpp.prosecutioncase.persistence.repository.CaseLinkSplitMergeRepository;
import uk.gov.moj.cpp.prosecutioncase.persistence.repository.ProsecutionCaseRepository;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
public class CaseLinkSplitMergeRepositoryTest {

    @RegisterExtension
    static HibernateTestEntityManagerProvider hibernateTestEntityManagerProvider =
            new HibernateTestEntityManagerProvider("progression-test-persistence-unit");

    private CaseLinkSplitMergeRepository repository;

    private ProsecutionCaseRepository prosecutionCaseRepository;

    @BeforeEach
    void createRepositoriesWithATestEntityManager() {
        repository = new CaseLinkSplitMergeRepository();
        hibernateTestEntityManagerProvider.injectEntityManagerInto(repository);
        prosecutionCaseRepository = new ProsecutionCaseRepository();
        hibernateTestEntityManagerProvider.injectEntityManagerInto(prosecutionCaseRepository);
    }

    @Test
    public void shouldListByMasterDefendantId() {
        final UUID caseId = randomUUID();
        final UUID linkedCaseId = randomUUID();
        final UUID linkGroupId = randomUUID();

        saveEntity(caseId, linkedCaseId, LinkType.LINK, linkGroupId);
        final List<CaseLinkSplitMergeEntity> actual = repository.findByCaseId(caseId);
        assertThat(actual, is(notNullValue()));
        assertThat(actual.size(), is(1));
        assertThat(actual.get(0).getCaseId(), is(caseId));
        assertThat(actual.get(0).getLinkedCaseId(), is(linkedCaseId));
        assertThat(actual.get(0).getType(), is(LinkType.LINK));
        assertThat(actual.get(0).getLinkGroupId(), is(linkGroupId));
    }

    @Test
    public void shouldListByLinkGroupId() {
        final UUID caseId = randomUUID();
        final UUID linkedCaseId = randomUUID();
        final UUID linkGroupId = randomUUID();
        saveEntity(caseId, linkedCaseId, LinkType.LINK, linkGroupId);
        final List<CaseLinkSplitMergeEntity> actual = repository.findByLinkGroupId(linkGroupId);
        assertThat(actual, is(notNullValue()));
        assertThat(actual.size(), is(1));
        assertThat(actual.get(0).getCaseId(), is(caseId));
        assertThat(actual.get(0).getLinkedCaseId(), is(linkedCaseId));
        assertThat(actual.get(0).getType(), is(LinkType.LINK));
        assertThat(actual.get(0).getLinkGroupId(), is(linkGroupId));
    }

    @Test
    public void shouldListByLinkGroupIdWhenNoRecordExists() {
        final List<CaseLinkSplitMergeEntity> actual = repository.findByLinkGroupId(randomUUID());
        assertThat(actual, is(notNullValue()));
        assertThat(actual.size(), is(0));
    }

    @Test
    public void shouldFindTheLinkBetweenTwoCasesOfAGivenType() {
        final UUID caseId = randomUUID();
        final UUID linkedCaseId = randomUUID();
        saveEntity(caseId, linkedCaseId, LinkType.LINK, randomUUID());
        saveEntity(caseId, randomUUID(), LinkType.LINK, randomUUID());

        final List<CaseLinkSplitMergeEntity> found =
                repository.findByCaseIdAndLinkedCaseIdAndType(caseId, linkedCaseId, LinkType.LINK);

        assertThat(found.size(), is(1));
        assertThat(found.get(0).getCaseId(), is(caseId));
        assertThat(found.get(0).getLinkedCaseId(), is(linkedCaseId));
    }

    @Test
    public void shouldReturnNothingWhenThoseTwoCasesAreLinkedByAnotherType() {
        final UUID caseId = randomUUID();
        final UUID linkedCaseId = randomUUID();
        saveEntity(caseId, linkedCaseId, LinkType.LINK, randomUUID());

        assertThat(repository.findByCaseIdAndLinkedCaseIdAndType(caseId, linkedCaseId, LinkType.MERGE).size(), is(0));
    }

    /**
     * Deliberately excludes the case being asked about - the point is to find the OTHER cases already
     * merged under the same reference - and only counts MERGE links.
     */
    @Test
    public void shouldFindOtherCasesMergedUnderTheSameReferenceButNotTheCaseItself() {
        final UUID caseId = randomUUID();
        final UUID otherMergedCaseId = randomUUID();
        saveEntity(caseId, randomUUID(), LinkType.MERGE, randomUUID());
        saveEntity(otherMergedCaseId, randomUUID(), LinkType.MERGE, randomUUID());
        saveEntity(randomUUID(), randomUUID(), LinkType.LINK, randomUUID());

        final List<CaseLinkSplitMergeEntity> found = repository.findPreviousMergesByReference(caseId, "reference");

        assertThat(found.stream().map(CaseLinkSplitMergeEntity::getCaseId).toList().contains(caseId), is(false));
        assertThat(found.stream().map(CaseLinkSplitMergeEntity::getCaseId).toList().contains(otherMergedCaseId), is(true));
        assertThat(found.stream().allMatch(entity -> entity.getType() == LinkType.MERGE), is(true));
    }

    @Test
    public void shouldReturnNothingWhenNoOtherCaseWasMergedUnderThatReference() {
        assertThat(repository.findPreviousMergesByReference(randomUUID(), "a-reference-nobody-used").size(), is(0));
    }

    private void saveEntity(final UUID caseId, final UUID linkedCaseId, final LinkType linkType, final UUID linkGroupId) {
        final CaseLinkSplitMergeEntity entity = new CaseLinkSplitMergeEntity();
        entity.setId(randomUUID());
        entity.setCaseId(caseId);
        entity.setLinkedCaseId(linkedCaseId);
        entity.setType(linkType);
        entity.setLinkedCase(getProsecutionCaseEntity(linkedCaseId));
        entity.setLinkGroupId(linkGroupId);
        entity.setReference("reference");
        repository.save(entity);
    }

    private ProsecutionCaseEntity getProsecutionCaseEntity(final UUID prosecutionCaseId) {
        final ProsecutionCaseEntity prosecutionCaseEntity = new ProsecutionCaseEntity();
        prosecutionCaseEntity.setCaseId(prosecutionCaseId);
        prosecutionCaseEntity.setPayload(createObjectBuilder().build().toString());
        prosecutionCaseRepository.save(prosecutionCaseEntity);
        return prosecutionCaseEntity;
    }

}
