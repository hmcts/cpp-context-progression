package uk.gov.moj.cpp.prosecutioncase.persistence.repository;

import static org.hamcrest.MatcherAssert.assertThat;
import static java.util.UUID.randomUUID;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.core.Is.is;
import static uk.gov.justice.services.messaging.JsonObjects.createArrayBuilder;
import static uk.gov.justice.services.messaging.JsonObjects.createObjectBuilder;

import uk.gov.justice.services.test.utils.persistence.HibernateTestEntityManagerProvider;
import uk.gov.moj.cpp.prosecutioncase.persistence.entity.CourtApplicationCaseEntity;
import uk.gov.moj.cpp.prosecutioncase.persistence.entity.CourtApplicationCaseKey;
import uk.gov.moj.cpp.prosecutioncase.persistence.entity.CourtApplicationEntity;
import uk.gov.moj.cpp.prosecutioncase.persistence.entity.ProsecutionCaseEntity;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
public class CourtApplicationCaseRepositoryTest {

    @RegisterExtension
    static HibernateTestEntityManagerProvider hibernateTestEntityManagerProvider =
            new HibernateTestEntityManagerProvider("progression-test-persistence-unit");

    private CourtApplicationCaseRepository courtApplicationCaseRepository;

    private ProsecutionCaseRepository prosecutionCaseRepository;

    @BeforeEach
    void createRepositoriesWithATestEntityManager() {
        courtApplicationRepository = new CourtApplicationRepository();
        hibernateTestEntityManagerProvider.injectEntityManagerInto(courtApplicationRepository);
        courtApplicationCaseRepository = new CourtApplicationCaseRepository();
        hibernateTestEntityManagerProvider.injectEntityManagerInto(courtApplicationCaseRepository);
        prosecutionCaseRepository = new ProsecutionCaseRepository();
        hibernateTestEntityManagerProvider.injectEntityManagerInto(prosecutionCaseRepository);
    }

        private CourtApplicationRepository courtApplicationRepository;

    private CourtApplicationCaseKey courtApplicationCaseKey;
    private CourtApplicationCaseEntity courtApplicationCaseEntity;
    private CourtApplicationEntity courtApplicationEntity;
    private ProsecutionCaseEntity prosecutionCaseEntity;

    private static UUID APPLICATION_ID;
    private static UUID CASE_ID;
    @BeforeEach
    public void setUp() {

        APPLICATION_ID = randomUUID();
        CASE_ID = randomUUID();

        prosecutionCaseEntity = new ProsecutionCaseEntity();
        prosecutionCaseEntity.setCaseId(CASE_ID);
        prosecutionCaseEntity.setPayload(createObjectBuilder()
                .add("defendants", createArrayBuilder().add(createObjectBuilder()
                                .add("id", randomUUID().toString()).build())
                        .build()).add("caseStatus", "INACTIVE")
                .build().toString());
        prosecutionCaseRepository.save(prosecutionCaseEntity);

        courtApplicationEntity = new CourtApplicationEntity();
        courtApplicationEntity.setPayload(createObjectBuilder().build().toString());
        courtApplicationEntity.setApplicationId(APPLICATION_ID);

        courtApplicationCaseKey = new CourtApplicationCaseKey(randomUUID(), APPLICATION_ID, CASE_ID);
        courtApplicationCaseEntity = new CourtApplicationCaseEntity();
        courtApplicationCaseEntity.setId(courtApplicationCaseKey);
        courtApplicationCaseEntity.setCourtApplication(courtApplicationEntity);
        courtApplicationCaseEntity.setCaseReference("caseReference");
        courtApplicationCaseRepository.save(courtApplicationCaseEntity);

    }

    @Test
    public void shouldFindCourtApplicationCaseEntityByApplicationId() {
        final String actual = courtApplicationCaseRepository.findCaseStatusByApplicationId(APPLICATION_ID, CASE_ID);
        assertThat(actual, is(notNullValue()));
    }

    @Test
    public void shouldDeleteByApplicationId() {
        courtApplicationCaseRepository.removeByApplicationId(APPLICATION_ID);

        final List<CourtApplicationCaseEntity> actual = courtApplicationCaseRepository.findByApplicationId(APPLICATION_ID);
        assertThat(actual.size(), is(0));
    }

    @Test
    public void shouldFindCourtApplicationCaseEntityByApplicationIdAndCaseId() {
        CourtApplicationCaseEntity byApplicationIdAndCaseId = courtApplicationCaseRepository.findByApplicationIdAndCaseId(APPLICATION_ID, CASE_ID);
        assertThat(byApplicationIdAndCaseId.getCourtApplication(), is(notNullValue()));
        assertThat(byApplicationIdAndCaseId.getCourtApplication().getApplicationId(), is(APPLICATION_ID));
    }

    /**
     * The case id is part of a composite key, so this query reads through entity.id.caseId rather
     * than a column of its own.
     */
    @Test
    public void shouldFindEveryApplicationCaseForACase() {
        final List<CourtApplicationCaseEntity> found = courtApplicationCaseRepository.findByCaseId(CASE_ID);

        assertThat(found.size(), is(1));
        assertThat(found.get(0).getId().getCaseId(), is(CASE_ID));
        assertThat(found.get(0).getCaseReference(), is("caseReference"));
    }

    @Test
    public void shouldReturnNothingWhenNoApplicationIsLinkedToThatCase() {
        assertThat(courtApplicationCaseRepository.findByCaseId(randomUUID()).size(), is(0));
    }
}
