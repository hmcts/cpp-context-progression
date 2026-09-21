package uk.gov.moj.cpp.prosecutioncase.persistence;

import static org.hamcrest.MatcherAssert.assertThat;
import static java.util.UUID.randomUUID;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static uk.gov.justice.services.messaging.JsonObjects.createObjectBuilder;

import uk.gov.justice.services.test.utils.persistence.HibernateTestEntityManagerProvider;
import uk.gov.justice.core.courts.HearingListingStatus;
import uk.gov.moj.cpp.prosecutioncase.persistence.entity.CaseDefendantHearingEntity;
import uk.gov.moj.cpp.prosecutioncase.persistence.entity.CaseDefendantHearingKey;
import uk.gov.moj.cpp.prosecutioncase.persistence.entity.HearingEntity;
import uk.gov.moj.cpp.prosecutioncase.persistence.entity.HearingResultLineEntity;
import uk.gov.moj.cpp.prosecutioncase.persistence.repository.CaseDefendantHearingRepository;
import uk.gov.moj.cpp.prosecutioncase.persistence.repository.HearingRepository;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

/**
 * DB integration tests for {@link CaseDefendantHearingRepository} class
 */

public class CaseDefendantHearingRepositoryTest {

    @RegisterExtension
    static HibernateTestEntityManagerProvider hibernateTestEntityManagerProvider =
            new HibernateTestEntityManagerProvider("progression-test-persistence-unit");

    private static final UUID CASE_ID = randomUUID();
    private static final UUID DEFENDANT_ID = randomUUID();
    private static final UUID HEARING_ID = randomUUID();
    private static final UUID RESULT_ID = randomUUID();

    private CaseDefendantHearingRepository caseDefendantHearingRepository;

    private HearingRepository hearingRepository;

    @BeforeEach
    void createRepositoriesWithATestEntityManager() {
        caseDefendantHearingRepository = new CaseDefendantHearingRepository();
        hibernateTestEntityManagerProvider.injectEntityManagerInto(caseDefendantHearingRepository);
        hearingRepository = new HearingRepository();
        hibernateTestEntityManagerProvider.injectEntityManagerInto(hearingRepository);
    }

    @BeforeEach
    public void setUp() {
        //given
        saveEntity(HEARING_ID, CASE_ID, DEFENDANT_ID, RESULT_ID);

        final CaseDefendantHearingKey caseDefendantHearingKey = new CaseDefendantHearingKey();
        caseDefendantHearingKey.setCaseId(randomUUID());
        caseDefendantHearingKey.setDefendantId(randomUUID());
        caseDefendantHearingKey.setHearingId(randomUUID());

        final HearingResultLineEntity hearingResultLineEntity = new HearingResultLineEntity();
        hearingResultLineEntity.setPayload(createObjectBuilder().build().toString());
        hearingResultLineEntity.setId(randomUUID());

        final Set<HearingResultLineEntity> resultLines = new HashSet<>();
        resultLines.add(new HearingResultLineEntity(randomUUID(), createObjectBuilder().build().toString(), null));

        final HearingEntity hearingEntity = new HearingEntity();
        hearingEntity.setHearingId(caseDefendantHearingKey.getHearingId());
        hearingEntity.setPayload(createObjectBuilder().build().toString());
        hearingEntity.setListingStatus(HearingListingStatus.HEARING_INITIALISED);
        hearingEntity.addResultLine(hearingResultLineEntity);
        hearingEntity.setResultLines(resultLines);
        hearingEntity.setResultLines(resultLines);
        hearingRepository.save(hearingEntity);

        final CaseDefendantHearingEntity caseDefendantHearingEntity = new CaseDefendantHearingEntity();
        caseDefendantHearingEntity.setId(caseDefendantHearingKey);
        caseDefendantHearingEntity.setHearing(hearingEntity);

        caseDefendantHearingRepository.save(caseDefendantHearingEntity);
    }

    @Test
    public void shouldFindCaseDefendantHearingEntityByCaseId() throws Exception {

        final List<CaseDefendantHearingEntity> actual = caseDefendantHearingRepository.findByCaseId(CASE_ID);
        assertThat(actual.size(), is(1));
        assertThat(actual.get(0).getId().getCaseId(), is(CASE_ID));
        assertThat(actual.get(0).getId().getDefendantId(), is(DEFENDANT_ID));
        assertThat(actual.get(0).getId().getHearingId(), is(HEARING_ID));
        assertThat(actual.get(0).getHearing().getHearingId(), is(HEARING_ID));
        assertThat(actual.get(0).getHearing().getListingStatus(), is(HearingListingStatus.HEARING_INITIALISED));
        assertThat(actual.get(0).getHearing().getResultLines().size(), is(1));
        assertThat(actual.get(0).getHearing().getResultLines().iterator().next().getId(), is(RESULT_ID));
    }

    @Test
    public void shouldFindCaseDefendantHearingEntityByHearingId() throws Exception {

        final List<CaseDefendantHearingEntity> actual = caseDefendantHearingRepository.findByHearingId(HEARING_ID);
        assertThat(actual.size(), is(1));
        assertThat(actual.get(0).getId().getCaseId(), is(CASE_ID));
        assertThat(actual.get(0).getId().getDefendantId(), is(DEFENDANT_ID));
        assertThat(actual.get(0).getId().getHearingId(), is(HEARING_ID));
        assertThat(actual.get(0).getHearing().getHearingId(), is(HEARING_ID));
        assertThat(actual.get(0).getHearing().getListingStatus(), is(HearingListingStatus.HEARING_INITIALISED));
        assertThat(actual.get(0).getHearing().getResultLines().size(), is(1));
        assertThat(actual.get(0).getHearing().getResultLines().iterator().next().getId(), is(RESULT_ID));
    }

    @Test
    public void shouldFindCaseDefendantHearingEntityByDefendantId() throws Exception {

        final List<CaseDefendantHearingEntity> actual = caseDefendantHearingRepository.findByDefendantId(DEFENDANT_ID);
        assertThat(actual.size(), is(1));
        assertThat(actual.get(0).getId().getCaseId(), is(CASE_ID));
        assertThat(actual.get(0).getId().getDefendantId(), is(DEFENDANT_ID));
        assertThat(actual.get(0).getId().getHearingId(), is(HEARING_ID));
        assertThat(actual.get(0).getHearing().getHearingId(), is(HEARING_ID));
        assertThat(actual.get(0).getHearing().getListingStatus(), is(HearingListingStatus.HEARING_INITIALISED));
        assertThat(actual.get(0).getHearing().getResultLines().size(), is(1));
        assertThat(actual.get(0).getHearing().getResultLines().iterator().next().getId(), is(RESULT_ID));
    }

    @Test
    public void shouldFindCaseDefendantHearingEntityByCaseIdAndDefendantId() {

        final List<CaseDefendantHearingEntity> actual = caseDefendantHearingRepository.findByCaseIdAndDefendantId(CASE_ID, DEFENDANT_ID);
        assertThat(actual.size(), is(1));
        assertThat(actual.get(0).getId().getCaseId(), is(CASE_ID));
        assertThat(actual.get(0).getId().getDefendantId(), is(DEFENDANT_ID));
        assertThat(actual.get(0).getId().getHearingId(), is(HEARING_ID));
        assertThat(actual.get(0).getHearing().getHearingId(), is(HEARING_ID));
        assertThat(actual.get(0).getHearing().getListingStatus(), is(HearingListingStatus.HEARING_INITIALISED));
        assertThat(actual.get(0).getHearing().getResultLines().size(), is(1));
        assertThat(actual.get(0).getHearing().getResultLines().iterator().next().getId(), is(RESULT_ID));
    }

    @Test
    public void shouldRemoveByHearingIdAndCaseIdAndDefendantId() {
        caseDefendantHearingRepository.removeByHearingIdAndCaseIdAndDefendantId(HEARING_ID, CASE_ID, DEFENDANT_ID);
        final List<CaseDefendantHearingEntity> actual = caseDefendantHearingRepository.findByCaseIdAndDefendantId(CASE_ID, DEFENDANT_ID);
        assertThat(actual.size(), is(0));
    }

    @Test
    public void shouldRemoveByHearingId() {
        final UUID hearingId = randomUUID();
        saveEntity(hearingId, randomUUID(), randomUUID(), randomUUID());
        caseDefendantHearingRepository.removeByHearingId(hearingId);
        final List<CaseDefendantHearingEntity> actual = caseDefendantHearingRepository.findByHearingId(hearingId);
        assertThat(actual.size(), is(0));
    }

    @Test
    public void shouldFindTheRowForOneHearingCaseAndDefendant() {
        final CaseDefendantHearingEntity found = caseDefendantHearingRepository
                .findByHearingIdAndCaseIdAndDefendantId(HEARING_ID, CASE_ID, DEFENDANT_ID);

        assertThat(found, is(notNullValue()));
        assertThat(found.getId().getHearingId(), is(HEARING_ID));
        assertThat(found.getId().getCaseId(), is(CASE_ID));
        assertThat(found.getId().getDefendantId(), is(DEFENDANT_ID));
    }

    @Test
    public void shouldRemoveOnlyTheRowsForThatHearingAndCase() {
        final UUID hearingId = randomUUID();
        final UUID caseId = randomUUID();
        final UUID otherCaseId = randomUUID();
        saveEntity(hearingId, caseId, randomUUID(), randomUUID());
        saveAnotherRowOnTheSameHearing(hearingId, otherCaseId, randomUUID());

        caseDefendantHearingRepository.removeByHearingIdAndCaseId(hearingId, caseId);

        assertThat(caseDefendantHearingRepository.findByCaseId(caseId).size(), is(0));
        assertThat(caseDefendantHearingRepository.findByCaseId(otherCaseId).size(), is(1));
    }

    @Test
    public void shouldRemoveOnlyTheRowsForThatHearingAndDefendant() {
        final UUID hearingId = randomUUID();
        final UUID defendantId = randomUUID();
        final UUID otherDefendantId = randomUUID();
        saveEntity(hearingId, randomUUID(), defendantId, randomUUID());
        saveAnotherRowOnTheSameHearing(hearingId, randomUUID(), otherDefendantId);

        caseDefendantHearingRepository.removeByHearingIdAndDefendantId(hearingId, defendantId);

        final List<UUID> remainingDefendantIds = caseDefendantHearingRepository.findByHearingId(hearingId).stream()
                .map(entity -> entity.getId().getDefendantId())
                .toList();
        assertThat(remainingDefendantIds.contains(defendantId), is(false));
        assertThat(remainingDefendantIds.contains(otherDefendantId), is(true));
    }

    private void saveEntity(final UUID hearingId, final UUID caseId, final UUID defendantId, final UUID resultId) {
        final HearingResultLineEntity hearingResultLineEntity = new HearingResultLineEntity();
        hearingResultLineEntity.setPayload(createObjectBuilder().build().toString());
        hearingResultLineEntity.setId(resultId);

        final HearingEntity hearingEntity = new HearingEntity();
        hearingEntity.setHearingId(hearingId);
        hearingEntity.setPayload(createObjectBuilder().build().toString());
        hearingEntity.setListingStatus(HearingListingStatus.HEARING_INITIALISED);
        hearingEntity.addResultLine(hearingResultLineEntity);
        hearingRepository.save(hearingEntity);

        final CaseDefendantHearingEntity caseDefendantHearingEntity = new CaseDefendantHearingEntity();
        caseDefendantHearingEntity.setId(new CaseDefendantHearingKey(caseId, defendantId, hearingId));
        caseDefendantHearingEntity.setHearing(hearingEntity);

        caseDefendantHearingRepository.save(caseDefendantHearingEntity);
    }

    /**
     * Adds another case/defendant to a hearing that has already been saved. saveEntity builds a new
     * HearingEntity every time, so calling it twice with one hearing id puts two instances with the
     * same identity in the session; the removeBy... queries need several rows under one hearing.
     */
    private void saveAnotherRowOnTheSameHearing(final UUID hearingId, final UUID caseId, final UUID defendantId) {
        final CaseDefendantHearingEntity caseDefendantHearingEntity = new CaseDefendantHearingEntity();
        caseDefendantHearingEntity.setId(new CaseDefendantHearingKey(caseId, defendantId, hearingId));
        caseDefendantHearingEntity.setHearing(hearingRepository.findBy(hearingId));

        caseDefendantHearingRepository.saveAndFlush(caseDefendantHearingEntity);
    }

}
