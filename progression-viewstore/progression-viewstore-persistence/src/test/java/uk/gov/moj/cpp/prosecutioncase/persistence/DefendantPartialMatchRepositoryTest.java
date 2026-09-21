package uk.gov.moj.cpp.prosecutioncase.persistence;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;

import uk.gov.justice.services.test.utils.persistence.HibernateTestEntityManagerProvider;
import uk.gov.moj.cpp.prosecutioncase.persistence.entity.DefendantPartialMatchEntity;
import uk.gov.moj.cpp.prosecutioncase.persistence.repository.DefendantPartialMatchRepository;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

public class DefendantPartialMatchRepositoryTest {

    @RegisterExtension
    static HibernateTestEntityManagerProvider hibernateTestEntityManagerProvider =
            new HibernateTestEntityManagerProvider("progression-test-persistence-unit");

    private static final UUID PROSECUTION_CASE_ID = UUID.randomUUID();
    private static final UUID DEFENDANT_ID = UUID.randomUUID();
    private static final String DEFENDANT_NAME = "Steve Austin Waugh";
    private static final String CASE_REFERENCE = "GB23222SDS";
    private static final String PAYLOAD = "matched json payload";
    private static final ZonedDateTime CASE_RECEIVED_DATETIME = ZonedDateTime.now();

    private static final UUID PROSECUTION_CASE_ID_2 = UUID.randomUUID();
    private static final UUID DEFENDANT_ID_2 = UUID.randomUUID();
    private static final String DEFENDANT_NAME_2 = "Austin Steve Waugh";
    private static final String CASE_REFERENCE_2 = "XGB23222SDS";
    private static final ZonedDateTime CASE_RECEIVED_DATETIME_2 = ZonedDateTime.now().plusDays(1);

    private DefendantPartialMatchRepository repository;

    @BeforeEach
    void createRepositoriesWithATestEntityManager() {
        repository = new DefendantPartialMatchRepository();
        hibernateTestEntityManagerProvider.injectEntityManagerInto(repository);
    }

    @BeforeEach
    public void setUp() {
        saveEntity(DEFENDANT_ID, PROSECUTION_CASE_ID, DEFENDANT_NAME, CASE_REFERENCE, CASE_RECEIVED_DATETIME);
        saveEntity(DEFENDANT_ID_2, PROSECUTION_CASE_ID_2, DEFENDANT_NAME_2, CASE_REFERENCE_2, CASE_RECEIVED_DATETIME_2);
    }

    private void saveEntity(UUID defendantId, UUID prosecutionCaseId, String defendantName, String caseReference, ZonedDateTime time) {
        final DefendantPartialMatchEntity entity = new DefendantPartialMatchEntity();
        entity.setDefendantId(defendantId);
        entity.setProsecutionCaseId(prosecutionCaseId);
        entity.setDefendantName(defendantName);
        entity.setCaseReference(caseReference);
        entity.setPayload(PAYLOAD);
        entity.setCaseReceivedDatetime(time);
        repository.save(entity);
    }

    @Test
    public void shouldFindPartialMatchedDefendantByDefendantId() {
        final DefendantPartialMatchEntity actual = repository.findByDefendantId(DEFENDANT_ID);
        assertThat(actual, is(notNullValue()));
        assertThat(actual.getDefendantId(), is(DEFENDANT_ID));
        assertThat(actual.getProsecutionCaseId(), is(PROSECUTION_CASE_ID));
        assertThat(actual.getCaseReference(), is(CASE_REFERENCE));
        assertThat(actual.getDefendantName(), is(DEFENDANT_NAME));
        assertThat(actual.getCaseReceivedDatetime(), is(CASE_RECEIVED_DATETIME));
        assertThat(actual.getPayload(), is(PAYLOAD));
    }

    @Test
    public void shouldFindPartialMatchedDefendantByDefendantIdNoRecordOnDB() {
        final DefendantPartialMatchEntity actual = repository.findByDefendantId(UUID.randomUUID());
        assertThat(actual, is(nullValue()));
    }

    @Test
    public void shouldFindPartialMatchedDefendantByProsecutionCaseId() {
        final DefendantPartialMatchEntity actual = repository.findByProsecutionCaseId(PROSECUTION_CASE_ID);
        assertThat(actual, is(notNullValue()));
        assertThat(actual.getDefendantId(), is(DEFENDANT_ID));
        assertThat(actual.getProsecutionCaseId(), is(PROSECUTION_CASE_ID));
        assertThat(actual.getCaseReference(), is(CASE_REFERENCE));
        assertThat(actual.getDefendantName(), is(DEFENDANT_NAME));
        assertThat(actual.getCaseReceivedDatetime(), is(CASE_RECEIVED_DATETIME));
        assertThat(actual.getPayload(), is(PAYLOAD));
    }

    @Test
    public void shouldFindPartialMatchedDefendantByCaseReference() {
        final DefendantPartialMatchEntity actual = repository.findByCaseReference(CASE_REFERENCE);
        assertThat(actual, is(notNullValue()));
        assertThat(actual.getDefendantId(), is(DEFENDANT_ID));
        assertThat(actual.getProsecutionCaseId(), is(PROSECUTION_CASE_ID));
        assertThat(actual.getCaseReference(), is(CASE_REFERENCE));
        assertThat(actual.getDefendantName(), is(DEFENDANT_NAME));
        assertThat(actual.getCaseReceivedDatetime(), is(CASE_RECEIVED_DATETIME));
        assertThat(actual.getPayload(), is(PAYLOAD));
    }

    @Test
    public void shouldOrderByDefendantNameAsc() {
        List<DefendantPartialMatchEntity> defendantPartialMatches = repository.findAllOrderByDefendantNameAsc(1, 0);
        assertEquals(DEFENDANT_NAME_2, defendantPartialMatches.get(0).getDefendantName());

        defendantPartialMatches = repository.findAllOrderByDefendantNameAsc(1, 1);
        assertEquals(DEFENDANT_NAME, defendantPartialMatches.get(0).getDefendantName());
    }

    @Test
    public void shouldOrderByDefendantNameDesc() {
        List<DefendantPartialMatchEntity> defendantPartialMatches = repository.findAllOrderByDefendantNameDesc(1, 0);
        assertEquals(DEFENDANT_NAME, defendantPartialMatches.get(0).getDefendantName());

        defendantPartialMatches = repository.findAllOrderByDefendantNameDesc(1, 1);
        assertEquals(DEFENDANT_NAME_2, defendantPartialMatches.get(0).getDefendantName());
    }

    @Test
    public void shouldOrderByReceivedDateAsc() {
        List<DefendantPartialMatchEntity> defendantPartialMatches = repository.findAllOrderByCaseReceivedDatetimeAsc(1, 0);
        assertEquals(DEFENDANT_NAME, defendantPartialMatches.get(0).getDefendantName());

        defendantPartialMatches = repository.findAllOrderByCaseReceivedDatetimeAsc(1, 1);
        assertEquals(DEFENDANT_NAME_2, defendantPartialMatches.get(0).getDefendantName());
    }

    @Test
    public void shouldOrderByReceivedDateDesc() {
        List<DefendantPartialMatchEntity> defendantPartialMatches = repository.findAllOrderByCaseReceivedDatetimeDesc(1, 0);
        assertEquals(DEFENDANT_NAME_2, defendantPartialMatches.get(0).getDefendantName());

        defendantPartialMatches = repository.findAllOrderByCaseReceivedDatetimeDesc(1, 1);
        assertEquals(DEFENDANT_NAME, defendantPartialMatches.get(0).getDefendantName());
    }
}
