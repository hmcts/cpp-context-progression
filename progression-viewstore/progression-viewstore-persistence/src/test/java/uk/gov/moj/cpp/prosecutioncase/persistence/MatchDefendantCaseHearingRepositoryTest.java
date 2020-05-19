package uk.gov.moj.cpp.prosecutioncase.persistence;

import static java.util.UUID.randomUUID;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertTrue;

import org.apache.deltaspike.testcontrol.api.junit.CdiTestRunner;
import org.junit.Test;
import org.junit.runner.RunWith;
import uk.gov.justice.core.courts.HearingListingStatus;
import uk.gov.moj.cpp.prosecutioncase.persistence.entity.HearingEntity;
import uk.gov.moj.cpp.prosecutioncase.persistence.entity.HearingResultLineEntity;
import uk.gov.moj.cpp.prosecutioncase.persistence.entity.MatchDefendantCaseHearingEntity;
import uk.gov.moj.cpp.prosecutioncase.persistence.entity.ProsecutionCaseEntity;
import uk.gov.moj.cpp.prosecutioncase.persistence.repository.HearingRepository;
import uk.gov.moj.cpp.prosecutioncase.persistence.repository.MatchDefendantCaseHearingRepository;
import uk.gov.moj.cpp.prosecutioncase.persistence.repository.ProsecutionCaseRepository;
import javax.inject.Inject;
import javax.json.Json;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@RunWith(CdiTestRunner.class)
public class MatchDefendantCaseHearingRepositoryTest {

    private static final UUID PROSECUTION_CASE_ID = randomUUID();
    private static final UUID PROSECUTION_CASE_ID_2 = randomUUID();
    private static final UUID DEFENDANT_ID = randomUUID();
    private static final UUID MASTER_DEFENDANT_ID = randomUUID();
    private static final UUID HEARING_ID = randomUUID();
    private static final UUID RESULT_ID = randomUUID();

    @Inject
    private MatchDefendantCaseHearingRepository matchDefendantCaseHearingRepository;

    @Inject
    private ProsecutionCaseRepository prosecutionCaseRepository;

    @Inject
    private HearingRepository hearingRepository;

    @Test
    public void shouldFindByProsecutionCaseIdAndDefendantId() {

        saveEntity(DEFENDANT_ID, MASTER_DEFENDANT_ID, PROSECUTION_CASE_ID, HEARING_ID, RESULT_ID);

        final MatchDefendantCaseHearingEntity actual = matchDefendantCaseHearingRepository.findByProsecutionCaseIdAndDefendantId(PROSECUTION_CASE_ID, DEFENDANT_ID);
        assertThat(actual, is(notNullValue()));
        assertThat(actual.getDefendantId(), is(DEFENDANT_ID));
        assertThat(actual.getMasterDefendantId(), is(MASTER_DEFENDANT_ID));
        assertThat(actual.getProsecutionCaseId(), is(PROSECUTION_CASE_ID));
        assertThat(actual.getHearingId(), is(HEARING_ID));
    }

    @Test
    public void shouldListByMasterDefendantId() {
        final UUID masterDefendantId1 = randomUUID();
        final UUID masterDefendantId2 = randomUUID();

        saveEntity(randomUUID(), masterDefendantId1, randomUUID(), randomUUID(), randomUUID());
        saveEntity(randomUUID(), masterDefendantId2, randomUUID(), randomUUID(), randomUUID());
        final List<MatchDefendantCaseHearingEntity> actual = matchDefendantCaseHearingRepository.findByMasterDefendantId(Arrays.asList(masterDefendantId1, masterDefendantId2));
        assertThat(actual, is(notNullValue()));
        assertThat(actual.size(), is(2));

        Optional<MatchDefendantCaseHearingEntity> actual1 = actual.stream()
                .filter(item -> item.getMasterDefendantId() == masterDefendantId1).findFirst();

        Optional<MatchDefendantCaseHearingEntity> actual2 = actual.stream()
                .filter(item -> item.getMasterDefendantId() == masterDefendantId2).findFirst();

        assertTrue(actual1.isPresent());
        assertTrue(actual2.isPresent());
    }

    private void saveEntity(UUID defendantId, UUID masterDefendantId, UUID prosecutionCaseId, UUID hearingId, UUID resultId) {
        prosecutionCaseRepository.save(getProsecutionCaseEntity(prosecutionCaseId));
        hearingRepository.save(getHearingEntity(resultId, hearingId));
        final MatchDefendantCaseHearingEntity entity = new MatchDefendantCaseHearingEntity();
        entity.setId(randomUUID());
        entity.setDefendantId(defendantId);
        entity.setMasterDefendantId(masterDefendantId);
        entity.setProsecutionCaseId(prosecutionCaseId);
        entity.setHearingId(hearingId);
        matchDefendantCaseHearingRepository.save(entity);
    }

    private HearingEntity getHearingEntity(UUID resultId, UUID hearingId) {
        final HearingResultLineEntity hearingResultLineEntity = new HearingResultLineEntity();
        hearingResultLineEntity.setPayload(Json.createObjectBuilder().build().toString());
        hearingResultLineEntity.setId(resultId);
        final HearingEntity hearingEntity = new HearingEntity();
        hearingEntity.setHearingId(hearingId);
        hearingEntity.setPayload(Json.createObjectBuilder().build().toString());
        hearingEntity.setListingStatus(HearingListingStatus.HEARING_INITIALISED);
        hearingEntity.addResultLine(hearingResultLineEntity);
        return hearingEntity;
    }

    private ProsecutionCaseEntity getProsecutionCaseEntity(UUID prosecutionCaseId) {
        final ProsecutionCaseEntity prosecutionCaseEntity = new ProsecutionCaseEntity();
        prosecutionCaseEntity.setCaseId(prosecutionCaseId);
        prosecutionCaseEntity.setPayload(Json.createObjectBuilder().build().toString());
        return prosecutionCaseEntity;
    }
}
