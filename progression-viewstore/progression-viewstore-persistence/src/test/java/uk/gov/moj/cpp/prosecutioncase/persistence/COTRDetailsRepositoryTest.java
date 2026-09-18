package uk.gov.moj.cpp.prosecutioncase.persistence;

import static java.util.UUID.randomUUID;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.core.Is.is;

import uk.gov.justice.services.test.utils.persistence.HibernateTestEntityManagerProvider;
import uk.gov.moj.cpp.prosecutioncase.persistence.entity.COTRDetailsEntity;
import uk.gov.moj.cpp.prosecutioncase.persistence.repository.COTRDetailsRepository;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

public class COTRDetailsRepositoryTest {

    @RegisterExtension
    static HibernateTestEntityManagerProvider hibernateTestEntityManagerProvider =
            new HibernateTestEntityManagerProvider("progression-test-persistence-unit");

    private COTRDetailsRepository cotrDetailsRepository;

    @BeforeEach
    void createRepositoriesWithATestEntityManager() {
        cotrDetailsRepository = new COTRDetailsRepository();
        hibernateTestEntityManagerProvider.injectEntityManagerInto(cotrDetailsRepository);
    }

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

    private COTRDetailsEntity cotrFor(final UUID prosecutionCaseId) {
        return new COTRDetailsEntity(randomUUID(), randomUUID(), prosecutionCaseId, false,
                "prosecutionFormData", "caseProgressionReviewNote", "listingReviewNotes", "judgeReviewNotes");
    }

    @Test
    public void shouldFindEveryCotrRecordedAgainstAProsecutionCase() {
        final UUID prosecutionCaseId = randomUUID();
        cotrDetailsRepository.saveAndFlush(cotrFor(prosecutionCaseId));
        cotrDetailsRepository.saveAndFlush(cotrFor(prosecutionCaseId));
        cotrDetailsRepository.saveAndFlush(cotrFor(randomUUID()));

        final List<COTRDetailsEntity> found = cotrDetailsRepository.findByProsecutionCaseId(prosecutionCaseId);

        assertThat(found.size(), equalTo(2));
        assertThat(found.stream().map(COTRDetailsEntity::getProsecutionCaseId).distinct().toList(),
                equalTo(List.of(prosecutionCaseId)));
    }

    @Test
    public void shouldReturnNothingWhenTheProsecutionCaseHasNoCotr() {
        assertThat(cotrDetailsRepository.findByProsecutionCaseId(randomUUID()).isEmpty(), equalTo(true));
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