package uk.gov.moj.cpp.prosecutioncase.persistence;

import static java.util.UUID.randomUUID;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;

import uk.gov.moj.cpp.progression.domain.event.link.LinkType;
import uk.gov.moj.cpp.prosecutioncase.persistence.entity.CaseLinkSplitMergeEntity;
import uk.gov.moj.cpp.prosecutioncase.persistence.entity.ProsecutionCaseEntity;
import uk.gov.moj.cpp.prosecutioncase.persistence.repository.CaseLinkSplitMergeRepository;
import uk.gov.moj.cpp.prosecutioncase.persistence.repository.ProsecutionCaseRepository;

import java.util.List;
import java.util.UUID;

import javax.inject.Inject;
import uk.gov.justice.services.messaging.JsonObjects;

import org.apache.deltaspike.testcontrol.api.junit.CdiTestRunner;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(CdiTestRunner.class)
public class CaseLinkSplitMergeRepositoryTest {

    @Inject
    private CaseLinkSplitMergeRepository repository;

    @Inject
    private ProsecutionCaseRepository prosecutionCaseRepository;

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
        prosecutionCaseEntity.setPayload(JsonObjects.createObjectBuilder().build().toString());
        prosecutionCaseRepository.save(prosecutionCaseEntity);
        return prosecutionCaseEntity;
    }

}
