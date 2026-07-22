package uk.gov.moj.cpp.prosecutioncase.persistence;

import static java.util.UUID.randomUUID;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertTrue;

import uk.gov.justice.core.courts.HearingListingStatus;
import uk.gov.moj.cpp.prosecutioncase.persistence.entity.HearingEntity;
import uk.gov.moj.cpp.prosecutioncase.persistence.entity.HearingResultLineEntity;
import uk.gov.moj.cpp.prosecutioncase.persistence.entity.MatchDefendantCaseHearingEntity;
import uk.gov.moj.cpp.prosecutioncase.persistence.entity.ProsecutionCaseEntity;
import uk.gov.moj.cpp.prosecutioncase.persistence.repository.HearingRepository;
import uk.gov.moj.cpp.prosecutioncase.persistence.repository.MatchDefendantCaseHearingRepository;
import uk.gov.moj.cpp.prosecutioncase.persistence.repository.ProsecutionCaseRepository;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

import javax.inject.Inject;
import uk.gov.justice.services.messaging.JsonObjects;
import javax.persistence.NonUniqueResultException;

import org.apache.deltaspike.testcontrol.api.junit.CdiTestRunner;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(CdiTestRunner.class)
public class MatchDefendantCaseHearingRepositoryTest {

    private static final UUID PROSECUTION_CASE_ID = randomUUID();
    private static final UUID DEFENDANT_ID = randomUUID();
    private static final UUID MASTER_DEFENDANT_ID = randomUUID();
    private static final UUID HEARING_ID = randomUUID();
    private static final UUID NEW_HEARING_ID = randomUUID();
    private static final UUID RESULT_ID = randomUUID();

    @Inject
    private MatchDefendantCaseHearingRepository matchDefendantCaseHearingRepository;

    @Inject
    private ProsecutionCaseRepository prosecutionCaseRepository;

    @Inject
    private HearingRepository hearingRepository;

    @Before
    public void setup() {
        saveEntity(DEFENDANT_ID, MASTER_DEFENDANT_ID, PROSECUTION_CASE_ID, HEARING_ID, RESULT_ID);
    }

    @After
    public void tearDown() {
        removeEntity(HEARING_ID, PROSECUTION_CASE_ID, DEFENDANT_ID);
        removeEntity(NEW_HEARING_ID, PROSECUTION_CASE_ID, DEFENDANT_ID);
    }


    @Test
    public void shouldFindByProsecutionCaseIdAndDefendantId() {

        final List<MatchDefendantCaseHearingEntity> entities = matchDefendantCaseHearingRepository.findByProsecutionCaseIdAndDefendantId(PROSECUTION_CASE_ID, DEFENDANT_ID);
        assertThat(entities.size(), is(1));
        final MatchDefendantCaseHearingEntity actual = entities.get(0);
        assertThat(actual, is(notNullValue()));
        assertThat(actual.getDefendantId(), is(DEFENDANT_ID));
        assertThat(actual.getMasterDefendantId(), is(MASTER_DEFENDANT_ID));
        assertThat(actual.getProsecutionCaseId(), is(PROSECUTION_CASE_ID));
        assertThat(actual.getHearingId(), is(HEARING_ID));
    }

    @Test
    public void shouldFindByProsecutionCaseIdAndDefendantId_WhenMultipleHearingExists() {

        saveEntity(DEFENDANT_ID, MASTER_DEFENDANT_ID, PROSECUTION_CASE_ID, NEW_HEARING_ID, randomUUID());
        final List<MatchDefendantCaseHearingEntity> entities = matchDefendantCaseHearingRepository.findByProsecutionCaseIdAndDefendantId(PROSECUTION_CASE_ID, DEFENDANT_ID);

        assertThat(entities.size(), is(2));

        final MatchDefendantCaseHearingEntity firstEntity = entities.get(0);
        assertThat(firstEntity, is(notNullValue()));
        assertThat(firstEntity.getDefendantId(), is(DEFENDANT_ID));
        assertThat(firstEntity.getMasterDefendantId(), is(MASTER_DEFENDANT_ID));
        assertThat(firstEntity.getProsecutionCaseId(), is(PROSECUTION_CASE_ID));

        final MatchDefendantCaseHearingEntity secondEntity = entities.get(1);
        assertThat(secondEntity, is(notNullValue()));
        assertThat(secondEntity.getDefendantId(), is(DEFENDANT_ID));
        assertThat(secondEntity.getMasterDefendantId(), is(MASTER_DEFENDANT_ID));
        assertThat(secondEntity.getProsecutionCaseId(), is(PROSECUTION_CASE_ID));

        final List<UUID> actualHearings = Arrays.asList(firstEntity.getHearingId(), secondEntity.getHearingId());
        final List<UUID> expectedHearings = Arrays.asList(HEARING_ID, NEW_HEARING_ID);
        assertThat(actualHearings, equalTo(expectedHearings));
    }

    @Test
    public void shouldFindByHearingIdProsecutionCaseIdAndDefendantId() {
        final MatchDefendantCaseHearingEntity entity = matchDefendantCaseHearingRepository.findByHearingIdAndProsecutionCaseIdAndDefendantId(HEARING_ID, PROSECUTION_CASE_ID, DEFENDANT_ID);

        assertThat(entity, is(notNullValue()));
        assertThat(entity.getDefendantId(), is(DEFENDANT_ID));
        assertThat(entity.getMasterDefendantId(), is(MASTER_DEFENDANT_ID));
        assertThat(entity.getProsecutionCaseId(), is(PROSECUTION_CASE_ID));
        assertThat(entity.getHearingId(), is(HEARING_ID));
    }

    @Test
    public void shouldNotFindByHearingIdProsecutionCaseIdAndDefendantIdWhenRecordNotExists() {
        final MatchDefendantCaseHearingEntity entity = matchDefendantCaseHearingRepository.findByHearingIdAndProsecutionCaseIdAndDefendantId(randomUUID(), PROSECUTION_CASE_ID, DEFENDANT_ID);

        assertThat(entity, is(nullValue()));
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

    @Test
    public void shouldListByMasterDefendantIdWithNoDuplicates() {
        saveEntity(DEFENDANT_ID, MASTER_DEFENDANT_ID, PROSECUTION_CASE_ID, HEARING_ID, RESULT_ID);
        final List<MatchDefendantCaseHearingEntity> actual = matchDefendantCaseHearingRepository.findByMasterDefendantId(MASTER_DEFENDANT_ID);
        final HashSet<MatchDefendantCaseHearingEntity> matchDefendantCaseHearingEntities = new HashSet<>(actual);
        assertThat(matchDefendantCaseHearingEntities, is(notNullValue()));
        assertThat(matchDefendantCaseHearingEntities.size(), is(1));
    }

    @Test
    public void shouldRemoveByHearingIdAndCaseIdAndDefendantId() {
        matchDefendantCaseHearingRepository.removeByHearingIdAndCaseIdAndDefendantId(HEARING_ID, PROSECUTION_CASE_ID, DEFENDANT_ID);
        final List<MatchDefendantCaseHearingEntity> matchDefendantCaseHearingEntities = matchDefendantCaseHearingRepository.findByProsecutionCaseIdAndDefendantId(PROSECUTION_CASE_ID, DEFENDANT_ID);
        assertThat(matchDefendantCaseHearingEntities.size(), is(0));
    }

    @Test
    public void shouldRemoveByHearingId() {
        matchDefendantCaseHearingRepository.removeByHearingId(HEARING_ID);
        final List<MatchDefendantCaseHearingEntity> matchDefendantCaseHearingEntities = matchDefendantCaseHearingRepository.findByProsecutionCaseIdAndDefendantId(PROSECUTION_CASE_ID, DEFENDANT_ID);
        assertThat(matchDefendantCaseHearingEntities.size(), is(0));
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
        hearingResultLineEntity.setPayload(JsonObjects.createObjectBuilder().build().toString());
        hearingResultLineEntity.setId(resultId);
        final HearingEntity hearingEntity = new HearingEntity();
        hearingEntity.setHearingId(hearingId);
        hearingEntity.setPayload(JsonObjects.createObjectBuilder().build().toString());
        hearingEntity.setListingStatus(HearingListingStatus.HEARING_INITIALISED);
        hearingEntity.addResultLine(hearingResultLineEntity);
        return hearingEntity;
    }

    private ProsecutionCaseEntity getProsecutionCaseEntity(UUID prosecutionCaseId) {
        final ProsecutionCaseEntity prosecutionCaseEntity = new ProsecutionCaseEntity();
        prosecutionCaseEntity.setCaseId(prosecutionCaseId);
        prosecutionCaseEntity.setPayload(JsonObjects.createObjectBuilder().build().toString());
        return prosecutionCaseEntity;
    }

    private void removeEntity(final UUID hearingId, final UUID prosecutionCaseId, final UUID defendantId) {
        try {
            final MatchDefendantCaseHearingEntity entity = matchDefendantCaseHearingRepository.findByHearingIdAndProsecutionCaseIdAndDefendantId(hearingId, prosecutionCaseId, defendantId);
            if (Objects.nonNull(entity)) {
                matchDefendantCaseHearingRepository.remove(entity);
            }
        } catch (NonUniqueResultException e) {
            // do nothing
        }
    }
}
