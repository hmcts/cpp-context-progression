package uk.gov.moj.cpp.prosecutioncase.persistence;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;

import uk.gov.moj.cpp.prosecutioncase.persistence.entity.CaseCpsProsecutorEntity;
import uk.gov.moj.cpp.prosecutioncase.persistence.repository.CaseCpsProsecutorRepository;

import java.util.UUID;

import javax.inject.Inject;

import org.apache.deltaspike.testcontrol.api.junit.CdiTestRunner;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * DB integration tests for {CaseCpsProsecutorRepository} class
 */

@RunWith(CdiTestRunner.class)
public class CaseCpsProsecutorRepositoryTest {

    private static final UUID ID = UUID.randomUUID();

    private static final String PROSECUTOR = "";

    private static final String OLD_PROSECUTOR = "";

    @Inject
    private CaseCpsProsecutorRepository caseCpsProsecutorRepository;

    private CaseCpsProsecutorEntity caseCpsProsecutorEntity;

    @Before
    public void setUp() {
        caseCpsProsecutorEntity = new CaseCpsProsecutorEntity(ID, PROSECUTOR, OLD_PROSECUTOR);
        caseCpsProsecutorRepository.save(caseCpsProsecutorEntity);

        final CaseCpsProsecutorEntity caseCpsProsecutorEntity1 = new CaseCpsProsecutorEntity();
        caseCpsProsecutorEntity1.setCaseId(UUID.randomUUID());
        caseCpsProsecutorEntity1.setCpsProsecutor("CpsProsecutor");
        caseCpsProsecutorEntity1.setOldCpsProsecutor("OldCpsProsecutor");
        caseCpsProsecutorRepository.save(caseCpsProsecutorEntity1);
    }

    @Test
    public void shouldFindCaseDefendantHearingEntityByCaseIdAndDefendantId() {

        final CaseCpsProsecutorEntity actual = caseCpsProsecutorRepository.findBy(ID);
        assertThat(actual, is(notNullValue()));
        assertThat(actual.getCaseId(), is(ID));
        assertThat(actual.getCpsProsecutor(), is(PROSECUTOR));
        assertThat(actual.getOldCpsProsecutor(), is(OLD_PROSECUTOR));
    }
}
