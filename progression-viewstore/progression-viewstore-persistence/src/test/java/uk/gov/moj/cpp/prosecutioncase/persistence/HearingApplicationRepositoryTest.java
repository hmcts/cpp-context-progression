package uk.gov.moj.cpp.prosecutioncase.persistence;

import static java.util.UUID.randomUUID;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

import uk.gov.justice.core.courts.HearingListingStatus;
import uk.gov.moj.cpp.prosecutioncase.persistence.entity.HearingApplicationEntity;
import uk.gov.moj.cpp.prosecutioncase.persistence.entity.HearingApplicationKey;
import uk.gov.moj.cpp.prosecutioncase.persistence.entity.HearingEntity;
import uk.gov.moj.cpp.prosecutioncase.persistence.entity.HearingResultLineEntity;
import uk.gov.moj.cpp.prosecutioncase.persistence.repository.HearingApplicationRepository;

import java.util.List;
import java.util.UUID;

import javax.inject.Inject;
import uk.gov.justice.services.messaging.JsonObjects;

import org.apache.deltaspike.testcontrol.api.junit.CdiTestRunner;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * DB integration tests for {@link HearingApplicationRepositoryTest} class
 */

@RunWith(CdiTestRunner.class)
public class HearingApplicationRepositoryTest {

    private static UUID HEARING_ID;
    private static UUID RESULT_ID;
    private static UUID APPLICATION_ID;

    @Inject
    private HearingApplicationRepository hearingApplicationRepository;

    @Before
    public void setUp() {
        //given
        HEARING_ID = randomUUID();
        RESULT_ID = randomUUID();
        APPLICATION_ID = randomUUID();

        final HearingResultLineEntity hearingResultLineEntity = new HearingResultLineEntity();
        hearingResultLineEntity.setPayload(JsonObjects.createObjectBuilder().build().toString());
        hearingResultLineEntity.setId(RESULT_ID);

        final HearingEntity hearingEntity = new HearingEntity();
        hearingEntity.setHearingId(HEARING_ID);
        hearingEntity.setPayload(JsonObjects.createObjectBuilder().build().toString());
        hearingEntity.setListingStatus(HearingListingStatus.HEARING_INITIALISED);
        hearingEntity.addResultLine(hearingResultLineEntity);

        final HearingApplicationEntity hearingApplicationEntity = new HearingApplicationEntity();
        hearingApplicationEntity.setId(new HearingApplicationKey(APPLICATION_ID, HEARING_ID));
        hearingApplicationEntity.setHearing(hearingEntity);

        hearingApplicationRepository.save(hearingApplicationEntity);
    }

    @Test
    public void shouldFindHearingApplicationEntityByHearingId() {
        final List<HearingApplicationEntity> actual = hearingApplicationRepository.findByHearingId(HEARING_ID);
        assertThat(actual.size(), is(1));
        assertThat(actual.get(0).getId().getApplicationId(), is(APPLICATION_ID));
        assertThat(actual.get(0).getId().getHearingId(), is(HEARING_ID));
        assertThat(actual.get(0).getHearing().getHearingId(), is(HEARING_ID));
        assertThat(actual.get(0).getHearing().getResultLines().size(), is(1));
        assertThat(actual.get(0).getHearing().getResultLines().iterator().next().getId(), is(RESULT_ID));
    }

    @Test
    public void shouldFindHearingApplicationEntityByApplicationId() {
        final List<HearingApplicationEntity> actual = hearingApplicationRepository.findByApplicationId(APPLICATION_ID);

        assertThat(actual.size(), is(1));
        assertThat(actual.get(0).getId().getApplicationId(), is(APPLICATION_ID));
        assertThat(actual.get(0).getId().getHearingId(), is(HEARING_ID));
        assertThat(actual.get(0).getHearing().getHearingId(), is(HEARING_ID));
        assertThat(actual.get(0).getHearing().getResultLines().size(), is(1));
        assertThat(actual.get(0).getHearing().getResultLines().iterator().next().getId(), is(RESULT_ID));
    }

    @Test
    public void shouldNotFailWhenHearingHasNoApplication() {
        final List<HearingApplicationEntity> actual = hearingApplicationRepository.findByApplicationId(randomUUID());
        assertThat(actual.size(), is(0));
    }

    @Test
    public void shouldRemoveByHearingIdAndApplicationId() {
        List<HearingApplicationEntity> actual = hearingApplicationRepository.findByApplicationId(APPLICATION_ID);
        assertThat(actual.size(), is(1));
        hearingApplicationRepository.removeByHearingIdAndCourtApplicationId(HEARING_ID, APPLICATION_ID);
        actual = hearingApplicationRepository.findByApplicationId(APPLICATION_ID);
        assertThat(actual.size(), is(0));
    }

    @Test
    public void shouldRemoveByHearingId() {
        List<HearingApplicationEntity> actual = hearingApplicationRepository.findByHearingId(HEARING_ID);
        assertThat(actual.size(), is(1));
        hearingApplicationRepository.removeByHearingId(HEARING_ID);
        actual = hearingApplicationRepository.findByHearingId(HEARING_ID);
        assertThat(actual.size(), is(0));
    }

    @Test
    public void shouldNotThrownExceptionWhenHearingApplicationEntityNotFound() {
        final UUID hearingId = randomUUID();
        List<HearingApplicationEntity> actual = hearingApplicationRepository.findByHearingId(hearingId);
        assertThat(actual.size(), is(0));
        hearingApplicationRepository.removeByHearingId(hearingId);
        actual = hearingApplicationRepository.findByHearingId(hearingId);
        assertThat(actual.size(), is(0));
    }

    @Test
    public void shouldDeleteByApplicationId() {
        hearingApplicationRepository.removeByApplicationId(APPLICATION_ID);

        final List<HearingApplicationEntity> actual = hearingApplicationRepository.findByApplicationId(APPLICATION_ID);
        assertThat(actual.size(), is(0));
    }
}
