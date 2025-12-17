package uk.gov.moj.cpp.prosecutioncase.persistence;

import static java.util.UUID.randomUUID;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.core.Is.is;

import uk.gov.moj.cpp.prosecutioncase.persistence.entity.COTRDetailsEntity;
import uk.gov.moj.cpp.prosecutioncase.persistence.repository.COTRDetailsRepository;

import java.util.List;
import java.util.UUID;

import javax.inject.Inject;

import org.apache.deltaspike.testcontrol.api.junit.CdiTestRunner;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(CdiTestRunner.class)
public class COTRDetailsRepositoryTest {

    @Inject
    private COTRDetailsRepository cotrDetailsRepository;

    @Test
    public void shouldSaveAndReadCOTRDetails() {

        final UUID cotrId = randomUUID();
        final UUID hearingId = randomUUID();
        final UUID prosecutionCaseId = randomUUID();
        final Boolean isArchived = false;
        final String prosecutionFormData = "prosecutionFormData";
        final String caseProgressionReviewNote = "caseProgressionReviewNote";
        final String listingReviewNotes = "listingReviewNotes";
        final String judgeReviewNotes = "judgeReviewNotes";

        COTRDetailsEntity entity = new COTRDetailsEntity(cotrId, hearingId, prosecutionCaseId, isArchived, prosecutionFormData, caseProgressionReviewNote, listingReviewNotes, judgeReviewNotes);

        cotrDetailsRepository.save(entity);

        final COTRDetailsEntity cotrDetailsEntity = cotrDetailsRepository.findBy(cotrId);
        verifyCOTRDetails(cotrDetailsEntity, entity);

    }

    @Test
    public void shouldFindCotrDetailsByHearingId() {

        final UUID cotrId1 = randomUUID();
        final UUID cotrId2 = randomUUID();
        final UUID hearingId1 = randomUUID();
        final UUID hearingId2 = randomUUID();
        final UUID prosecutionCaseId = randomUUID();
        final Boolean isArchived = false;
        final String prosecutionFormData = "prosecutionFormData";
        final String caseProgressionReviewNote = "caseProgressionReviewNote";
        final String listingReviewNotes = "listingReviewNotes";
        final String judgeReviewNotes = "judgeReviewNotes";

        COTRDetailsEntity entity1 = new COTRDetailsEntity(cotrId1, hearingId1, prosecutionCaseId, isArchived, prosecutionFormData, caseProgressionReviewNote, listingReviewNotes, judgeReviewNotes);
        cotrDetailsRepository.save(entity1);

        COTRDetailsEntity entity2 = new COTRDetailsEntity();
        entity2.setId(cotrId2);
        entity2.setHearingId(hearingId2);
        entity2.setProsecutionCaseId(prosecutionCaseId);
        entity2.setArchived(isArchived);
        entity2.setProsecutionFormData(prosecutionFormData);
        entity2.setCaseProgressionReviewNote(caseProgressionReviewNote);
        entity2.setListingReviewNotes(listingReviewNotes);
        entity2.setJudgeReviewNotes(judgeReviewNotes);
        cotrDetailsRepository.save(entity2);

        final List<COTRDetailsEntity> entities = cotrDetailsRepository.findByHearingId(hearingId1);
        assertThat(entities.size(), is(1));
        assertThat(entities.get(0).getHearingId(), is(hearingId1));


    }

    private void verifyCOTRDetails(final COTRDetailsEntity actual, final COTRDetailsEntity expected) {
        assertThat(actual.getId(), equalTo(expected.getId()));
        assertThat(actual.getHearingId(), equalTo(expected.getHearingId()));
        assertThat(actual.getArchived(), equalTo(expected.getArchived()));
        assertThat(actual.getProsecutionFormData(), equalTo(expected.getProsecutionFormData()));
        assertThat(actual.getCaseProgressionReviewNote(), equalTo(expected.getCaseProgressionReviewNote()));
        assertThat(actual.getListingReviewNotes(), equalTo(expected.getListingReviewNotes()));
        assertThat(actual.getJudgeReviewNotes(), equalTo(expected.getJudgeReviewNotes()));
    }
}