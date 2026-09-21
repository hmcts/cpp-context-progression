package uk.gov.moj.cpp.prosecutioncase.persistence.repository;

import static org.hamcrest.MatcherAssert.assertThat;
import static java.util.UUID.randomUUID;
import static org.hamcrest.core.Is.is;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static uk.gov.justice.services.messaging.JsonObjects.createObjectBuilder;

import uk.gov.justice.services.test.utils.persistence.HibernateTestEntityManagerProvider;
import uk.gov.moj.cpp.prosecutioncase.persistence.entity.CourtApplicationEntity;

import java.util.List;
import java.util.UUID;

import jakarta.persistence.NoResultException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
public class CourtApplicationRepositoryTest {

    @RegisterExtension
    static HibernateTestEntityManagerProvider hibernateTestEntityManagerProvider =
            new HibernateTestEntityManagerProvider("progression-test-persistence-unit");

    private CourtApplicationEntity courtApplicationEntity;

    private CourtApplicationRepository courtApplicationRepository;

    @BeforeEach
    void createRepositoriesWithATestEntityManager() {
        courtApplicationRepository = new CourtApplicationRepository();
        hibernateTestEntityManagerProvider.injectEntityManagerInto(courtApplicationRepository);
    }

    @BeforeEach
    public void setUp() {
        courtApplicationRepository.findAll().forEach(entity -> {
            courtApplicationRepository.remove(entity);
        });
    }

    @Test
    public void shouldDeleteByApplicationId() {
        final UUID applicationId = randomUUID();
        saveApplication(applicationId);
        courtApplicationRepository.removeByApplicationId(applicationId);

        final List<CourtApplicationEntity> actual = courtApplicationRepository.findAll();
        assertThat(actual.size(), is(0));
    }

    @Test
    public void shouldGetApplicationsByListOfApplicationIds() {
        final UUID applicationId1 = randomUUID();
        saveApplication(applicationId1);

        final UUID applicationId2 = randomUUID();
        saveApplication(applicationId2);

        final List<CourtApplicationEntity> actual = courtApplicationRepository.findByApplicationIds(List.of(applicationId1, applicationId2));

        assertThat(actual.size(), is(2));
    }

    @Test
    public void shouldFindAnApplicationByItsId() {
        final UUID applicationId = randomUUID();
        saveApplication(applicationId);

        final CourtApplicationEntity found = courtApplicationRepository.findByApplicationId(applicationId);

        assertThat(found.getApplicationId(), is(applicationId));
    }

    /**
     * findByApplicationId uses getSingleResult, so an unknown id throws rather than returning null.
     * Callers have to handle that, which is only obvious if it is pinned here.
     */
    @Test
    public void shouldThrowRatherThanReturnNothingWhenNoApplicationHasThatId() {
        assertThrows(NoResultException.class, () -> courtApplicationRepository.findByApplicationId(randomUUID()));
    }

    @Test
    public void shouldFindTheApplicationsRaisedUnderAParentApplication() {
        final UUID parentApplicationId = randomUUID();
        saveChildApplication(randomUUID(), parentApplicationId);
        saveChildApplication(randomUUID(), parentApplicationId);
        saveChildApplication(randomUUID(), randomUUID());

        final List<CourtApplicationEntity> found =
                courtApplicationRepository.findByParentApplicationId(parentApplicationId);

        assertThat(found.size(), is(2));
        assertThat(found.stream().map(CourtApplicationEntity::getParentApplicationId).distinct().toList(),
                is(List.of(parentApplicationId)));
    }

    @Test
    public void shouldReturnNothingWhenNoApplicationHasThatParent() {
        assertThat(courtApplicationRepository.findByParentApplicationId(randomUUID()).size(), is(0));
    }

    private void saveApplication(final UUID applicationId) {
        courtApplicationEntity = new CourtApplicationEntity();
        courtApplicationEntity.setPayload(createObjectBuilder().build().toString());
        courtApplicationEntity.setApplicationId(applicationId);
        courtApplicationRepository.save(courtApplicationEntity);
    }

    private void saveChildApplication(final UUID applicationId, final UUID parentApplicationId) {
        final CourtApplicationEntity childApplication = new CourtApplicationEntity();
        childApplication.setPayload(createObjectBuilder().build().toString());
        childApplication.setApplicationId(applicationId);
        childApplication.setParentApplicationId(parentApplicationId);
        courtApplicationRepository.saveAndFlush(childApplication);
    }
}
