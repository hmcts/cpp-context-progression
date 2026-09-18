package uk.gov.moj.cpp.prosecutioncase.persistence;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.CoreMatchers.is;
import static uk.gov.justice.services.messaging.JsonObjects.createObjectBuilder;

import uk.gov.justice.services.test.utils.persistence.HibernateTestEntityManagerProvider;
import uk.gov.justice.core.courts.HearingListingStatus;
import uk.gov.moj.cpp.prosecutioncase.persistence.entity.CaseDefendantHearingEntity;
import uk.gov.moj.cpp.prosecutioncase.persistence.entity.CaseDefendantHearingKey;
import uk.gov.moj.cpp.prosecutioncase.persistence.entity.HearingEntity;
import uk.gov.moj.cpp.prosecutioncase.persistence.entity.HearingResultLineEntity;
import uk.gov.moj.cpp.prosecutioncase.persistence.repository.CaseDefendantHearingRepository;
import uk.gov.moj.cpp.prosecutioncase.persistence.repository.HearingRepository;
import uk.gov.moj.cpp.prosecutioncase.persistence.repository.HearingResultLineEntityRepository;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

/**
 * DB integration tests for {@link CaseDefendantHearingRepository} class
 */

public class HearingRepositoryTest {

    @RegisterExtension
    static HibernateTestEntityManagerProvider hibernateTestEntityManagerProvider =
            new HibernateTestEntityManagerProvider("progression-test-persistence-unit");

    private static final UUID CASE_ID = UUID.randomUUID();
    private static final UUID DEFENDANT_ID = UUID.randomUUID();
    private static final UUID HEARING_ID = UUID.randomUUID();
    private static final UUID RESULT_ID_ONE = UUID.randomUUID();
    private static final UUID RESULT_ID_TWO = UUID.randomUUID();

    private HearingRepository hearingRepository;

    private CaseDefendantHearingRepository caseDefendantHearingRepository;

    private HearingResultLineEntityRepository hearingResultLineEntityRepository;

    @BeforeEach
    void createRepositoriesWithATestEntityManager() {
        hearingRepository = new HearingRepository();
        hibernateTestEntityManagerProvider.injectEntityManagerInto(hearingRepository);
        caseDefendantHearingRepository = new CaseDefendantHearingRepository();
        hibernateTestEntityManagerProvider.injectEntityManagerInto(caseDefendantHearingRepository);
        hearingResultLineEntityRepository = new HearingResultLineEntityRepository();
        hibernateTestEntityManagerProvider.injectEntityManagerInto(hearingResultLineEntityRepository);
    }

    @BeforeEach
    public void setUp() {
        final HearingEntity hearingEntity = new HearingEntity();
        hearingEntity.setHearingId(HEARING_ID);
        hearingEntity.setPayload(createObjectBuilder().build().toString());
        hearingEntity.setListingStatus(HearingListingStatus.HEARING_INITIALISED);
        hearingRepository.save(hearingEntity);

        final CaseDefendantHearingEntity caseDefendantHearingEntity = new CaseDefendantHearingEntity();
        caseDefendantHearingEntity.setId(new CaseDefendantHearingKey(CASE_ID, DEFENDANT_ID, HEARING_ID));
        caseDefendantHearingEntity.setHearing(hearingEntity);
        caseDefendantHearingRepository.save(caseDefendantHearingEntity);
    }

    @Test
    public void shouldRemoveResultLineEntityByHearingId() throws Exception {
        //given
        final HearingResultLineEntity hearingResultLineEntityOne = new HearingResultLineEntity();
        hearingResultLineEntityOne.setPayload(createObjectBuilder().build().toString());
        hearingResultLineEntityOne.setId(RESULT_ID_ONE);

        final HearingResultLineEntity hearingResultLineEntityTwo = new HearingResultLineEntity();
        hearingResultLineEntityTwo.setPayload(createObjectBuilder().build().toString());
        hearingResultLineEntityTwo.setId(RESULT_ID_TWO);

        final HearingEntity actual = hearingRepository.findBy(HEARING_ID);
        assertThat(actual.getHearingId(), is(HEARING_ID));
        assertThat(actual.getResultLines().size(), is(0));

        actual.addResultLines(Arrays.asList(hearingResultLineEntityOne, hearingResultLineEntityTwo));
        hearingRepository.save(actual);

        final HearingEntity hearingEntityAfterAdding = hearingRepository.findBy(HEARING_ID);
        assertThat(hearingEntityAfterAdding.getResultLines().size(), is(2));

        actual.getResultLines().stream().forEach(hearingResultLineEntity -> {
            hearingResultLineEntityRepository.remove(hearingResultLineEntity);
        });
        actual.getResultLines().clear();
        hearingRepository.save(actual);

        final HearingEntity hearingEntityWithoutResults = hearingRepository.findBy(HEARING_ID);
        assertThat(hearingEntityWithoutResults.getResultLines().size(), is(0));
    }

    @Test
    public void shouldRemoveByHearingId() {
        caseDefendantHearingRepository.removeByHearingId(HEARING_ID);
        hearingRepository.removeByHearingId(HEARING_ID);

        final List<HearingEntity> entities = hearingRepository.findByHearingIds(Arrays.asList(HEARING_ID));

        assertThat(entities.size(), is(0));
    }
}
