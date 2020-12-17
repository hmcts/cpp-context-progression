package uk.gov.moj.cpp.prosecutioncase.persistence;

import org.apache.deltaspike.testcontrol.api.junit.CdiTestRunner;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import uk.gov.justice.core.courts.HearingListingStatus;
import uk.gov.moj.cpp.prosecutioncase.persistence.entity.*;
import uk.gov.moj.cpp.prosecutioncase.persistence.repository.CaseDefendantHearingRepository;
import uk.gov.moj.cpp.prosecutioncase.persistence.repository.HearingApplicationRepository;

import javax.inject.Inject;
import javax.json.Json;
import java.util.List;
import java.util.UUID;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * DB integration tests for {@link HearingApplicationRepositoryTest} class
 */


@RunWith(CdiTestRunner.class)
public class HearingApplicationRepositoryTest {

    private static final UUID HEARING_ID = UUID.randomUUID();
    private static final UUID RESULT_ID = UUID.randomUUID();
    private static final UUID APPLICATION_ID = UUID.randomUUID();

    @Inject
    private HearingApplicationRepository hearingApplicationRepository;

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

        final HearingApplicationEntity hearingApplicationEntity = new HearingApplicationEntity();
        hearingApplicationEntity.setId(new HearingApplicationKey(APPLICATION_ID, HEARING_ID));
        hearingApplicationEntity.setHearing(hearingEntity);

        hearingApplicationRepository.save(hearingApplicationEntity);
    }

    @Test
    public void shouldFindHearingApplicationEntityByHearingId() throws Exception {

        final List<HearingApplicationEntity> actual = hearingApplicationRepository.findByHearingId(HEARING_ID);
        assertThat(actual.size(), is(1));
        assertThat(actual.get(0).getId().getApplicationId() , is(APPLICATION_ID));
        assertThat(actual.get(0).getId().getHearingId(), is(HEARING_ID));
        assertThat(actual.get(0).getHearing().getHearingId(), is(HEARING_ID));
        assertThat(actual.get(0).getHearing().getResultLines().size(), is(1));
        assertThat(actual.get(0).getHearing().getResultLines().iterator().next().getId(), is(RESULT_ID));
    }

    @Test
    public void shouldFindHearingApplicationEntityByApplicationId() throws Exception {

        final List<HearingApplicationEntity> actual = hearingApplicationRepository.findByApplicationId(APPLICATION_ID);

        assertThat(actual.size(), is(1));
        assertThat(actual.get(0).getId().getApplicationId() , is(APPLICATION_ID));
        assertThat(actual.get(0).getId().getHearingId(), is(HEARING_ID));
        assertThat(actual.get(0).getHearing().getHearingId(), is(HEARING_ID));
        assertThat(actual.get(0).getHearing().getResultLines().size(), is(1));
        assertThat(actual.get(0).getHearing().getResultLines().iterator().next().getId(), is(RESULT_ID));
    }

    @Test
    public void shouldNotFailWhenHearingHasNoApplication() throws Exception {
        final List<HearingApplicationEntity> actual = hearingApplicationRepository.findByApplicationId(UUID.randomUUID());
        assertThat(actual.size(), is(0));
    }
}
