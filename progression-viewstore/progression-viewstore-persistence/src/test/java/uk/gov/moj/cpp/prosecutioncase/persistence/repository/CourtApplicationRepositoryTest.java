package uk.gov.moj.cpp.prosecutioncase.persistence.repository;

import static java.util.UUID.randomUUID;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

import uk.gov.moj.cpp.prosecutioncase.persistence.entity.CourtApplicationEntity;

import java.util.List;
import java.util.UUID;

import javax.inject.Inject;
import javax.json.Json;

import org.apache.deltaspike.testcontrol.api.junit.CdiTestRunner;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(CdiTestRunner.class)
public class CourtApplicationRepositoryTest {

    private UUID APPLICATION_ID;
    private CourtApplicationEntity courtApplicationEntity;

    @Inject
    private CourtApplicationRepository courtApplicationRepository;

    @Before
    public void setUp() {

        APPLICATION_ID = randomUUID();
        courtApplicationEntity = new CourtApplicationEntity();
        courtApplicationEntity.setPayload(Json.createObjectBuilder().build().toString());
        courtApplicationEntity.setApplicationId(APPLICATION_ID);
        courtApplicationRepository.save(courtApplicationEntity);
    }

    @Test
    public void shouldDeleteByApplicationId() {
        courtApplicationRepository.removeByApplicationId(APPLICATION_ID);

        final List<CourtApplicationEntity> actual = courtApplicationRepository.findAll();
        assertThat(actual.size(), is(0));
    }
}
