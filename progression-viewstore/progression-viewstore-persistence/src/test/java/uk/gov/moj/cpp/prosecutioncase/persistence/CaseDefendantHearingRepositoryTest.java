package uk.gov.moj.cpp.prosecutioncase.persistence;

import static java.util.UUID.randomUUID;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

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

import javax.inject.Inject;
import javax.json.Json;

import org.apache.deltaspike.testcontrol.api.junit.CdiTestRunner;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * DB integration tests for {@link CaseDefendantHearingRepository} class
 */


@RunWith(CdiTestRunner.class)
public class CaseDefendantHearingRepositoryTest {

    private static final UUID CASE_ID = randomUUID();
    private static final UUID DEFENDANT_ID = randomUUID();
    private static final UUID HEARING_ID = randomUUID();
    private static final UUID RESULT_ID = randomUUID();

    @Inject
    private CaseDefendantHearingRepository caseDefendantHearingRepository;

    @Inject
    private HearingRepository hearingRepository;

    @Before
    public void setUp() {
        //given
        saveEntity(HEARING_ID, CASE_ID, DEFENDANT_ID, RESULT_ID);

        final CaseDefendantHearingKey caseDefendantHearingKey = new CaseDefendantHearingKey();
        caseDefendantHearingKey.setCaseId(randomUUID());
        caseDefendantHearingKey.setDefendantId(randomUUID());
        caseDefendantHearingKey.setHearingId(randomUUID());

        final HearingResultLineEntity hearingResultLineEntity = new HearingResultLineEntity();
        hearingResultLineEntity.setPayload(Json.createObjectBuilder().build().toString());
        hearingResultLineEntity.setId(randomUUID());

        final Set<HearingResultLineEntity> resultLines = new HashSet<>();
        resultLines.add(new HearingResultLineEntity(randomUUID(), Json.createObjectBuilder().build().toString(), null));

        final HearingEntity hearingEntity = new HearingEntity();
        hearingEntity.setHearingId(caseDefendantHearingKey.getHearingId());
        hearingEntity.setPayload(Json.createObjectBuilder().build().toString());
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

    private void saveEntity(final UUID hearingId, final UUID caseId, final UUID defendantId, final UUID resultId) {
        final HearingResultLineEntity hearingResultLineEntity = new HearingResultLineEntity();
        hearingResultLineEntity.setPayload(Json.createObjectBuilder().build().toString());
        hearingResultLineEntity.setId(resultId);

        final HearingEntity hearingEntity = new HearingEntity();
        hearingEntity.setHearingId(hearingId);
        hearingEntity.setPayload(Json.createObjectBuilder().build().toString());
        hearingEntity.setListingStatus(HearingListingStatus.HEARING_INITIALISED);
        hearingEntity.addResultLine(hearingResultLineEntity);
        hearingRepository.save(hearingEntity);

        final CaseDefendantHearingEntity caseDefendantHearingEntity = new CaseDefendantHearingEntity();
        caseDefendantHearingEntity.setId(new CaseDefendantHearingKey(caseId, defendantId, hearingId));
        caseDefendantHearingEntity.setHearing(hearingEntity);

        caseDefendantHearingRepository.save(caseDefendantHearingEntity);
    }


}
