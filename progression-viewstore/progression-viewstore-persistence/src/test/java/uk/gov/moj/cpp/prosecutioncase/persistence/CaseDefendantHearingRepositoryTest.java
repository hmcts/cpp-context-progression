package uk.gov.moj.cpp.prosecutioncase.persistence;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

import uk.gov.justice.core.courts.HearingListingStatus;
import uk.gov.moj.cpp.prosecutioncase.persistence.entity.CaseDefendantHearingEntity;
import uk.gov.moj.cpp.prosecutioncase.persistence.entity.CaseDefendantHearingKey;
import uk.gov.moj.cpp.prosecutioncase.persistence.entity.HearingEntity;
import uk.gov.moj.cpp.prosecutioncase.persistence.entity.HearingResultLineEntity;
import uk.gov.moj.cpp.prosecutioncase.persistence.repository.CaseDefendantHearingRepository;

import java.util.List;
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

    private static final UUID CASE_ID = UUID.randomUUID();
    private static final UUID DEFENDANT_ID = UUID.randomUUID();
    private static final UUID HEARING_ID = UUID.randomUUID();
    private static final UUID RESULT_ID = UUID.randomUUID();


    @Inject
    private CaseDefendantHearingRepository caseDefendantHearingRepository;

    @Before
    public void setUp(){
        //given
        final HearingResultLineEntity hearingResultLineEntity= new HearingResultLineEntity();
        hearingResultLineEntity.setPayload(Json.createObjectBuilder().build().toString());
        hearingResultLineEntity.setId(RESULT_ID);

        final HearingEntity hearingEntity = new HearingEntity();
        hearingEntity.setHearingId(HEARING_ID);
        hearingEntity.setPayload(Json.createObjectBuilder().build().toString());
        hearingEntity.setListingStatus(HearingListingStatus.HEARING_INITIALISED);
        hearingEntity.addResultLine(hearingResultLineEntity);

        final CaseDefendantHearingEntity caseDefendantHearingEntity = new CaseDefendantHearingEntity();
        caseDefendantHearingEntity.setId(new CaseDefendantHearingKey(CASE_ID, DEFENDANT_ID, HEARING_ID));
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


}
