package uk.gov.moj.cpp.prosecutioncase.persistence.repository;

import static java.util.UUID.randomUUID;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

import uk.gov.moj.cpp.prosecutioncase.persistence.entity.CourtApplicationEntity;

import java.util.List;
import java.util.UUID;

import javax.inject.Inject;
import uk.gov.justice.services.messaging.JsonObjects;

import org.apache.deltaspike.testcontrol.api.junit.CdiTestRunner;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(CdiTestRunner.class)
public class CourtApplicationRepositoryTest {

    private CourtApplicationEntity courtApplicationEntity;

    @Inject
    private CourtApplicationRepository courtApplicationRepository;

    @Before
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

    private void saveApplication(final UUID applicationId) {
        courtApplicationEntity = new CourtApplicationEntity();
        courtApplicationEntity.setPayload(JsonObjects.createObjectBuilder().build().toString());
        courtApplicationEntity.setApplicationId(applicationId);
        courtApplicationRepository.save(courtApplicationEntity);
    }
}
