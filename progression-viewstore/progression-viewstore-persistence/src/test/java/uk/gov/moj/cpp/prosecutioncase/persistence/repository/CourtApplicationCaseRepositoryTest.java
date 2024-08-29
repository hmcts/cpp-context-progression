package uk.gov.moj.cpp.prosecutioncase.persistence.repository;

import static java.util.UUID.randomUUID;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

import uk.gov.moj.cpp.prosecutioncase.persistence.entity.CourtApplicationCaseEntity;
import uk.gov.moj.cpp.prosecutioncase.persistence.entity.CourtApplicationCaseKey;
import uk.gov.moj.cpp.prosecutioncase.persistence.entity.CourtApplicationEntity;
import uk.gov.moj.cpp.prosecutioncase.persistence.entity.ProsecutionCaseEntity;

import java.util.List;
import java.util.UUID;

import javax.inject.Inject;
import javax.json.Json;

import org.apache.deltaspike.testcontrol.api.junit.CdiTestRunner;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(CdiTestRunner.class)
public class CourtApplicationCaseRepositoryTest {

    @Inject
    private CourtApplicationCaseRepository courtApplicationCaseRepository;

    @Inject
    private ProsecutionCaseRepository prosecutionCaseRepository;

    @Inject CourtApplicationRepository courtApplicationRepository;

    private CourtApplicationCaseKey courtApplicationCaseKey;
    private CourtApplicationCaseEntity courtApplicationCaseEntity;
    private CourtApplicationEntity courtApplicationEntity;
    private ProsecutionCaseEntity prosecutionCaseEntity;

    private static UUID APPLICATION_ID;
    private static UUID CASE_ID;

    @Before
    public void setUp(){

        APPLICATION_ID = randomUUID();
        CASE_ID = randomUUID();

        prosecutionCaseEntity = new ProsecutionCaseEntity();
        prosecutionCaseEntity.setCaseId(CASE_ID);
        prosecutionCaseEntity.setPayload(Json.createObjectBuilder()
                .add("defendants", Json.createArrayBuilder().add(Json.createObjectBuilder()
                                .add("id", randomUUID().toString()).build())
                        .build()).add("caseStatus", "INACTIVE")
                .build().toString());
        prosecutionCaseRepository.save(prosecutionCaseEntity);


        courtApplicationEntity = new CourtApplicationEntity();
        courtApplicationEntity.setPayload(Json.createObjectBuilder().build().toString());
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

}
