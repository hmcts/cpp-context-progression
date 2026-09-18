package uk.gov.moj.cpp.prosecutioncase.persistence;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;

import uk.gov.justice.services.test.utils.persistence.HibernateTestEntityManagerProvider;
import uk.gov.moj.cpp.prosecutioncase.persistence.entity.CaseCpsProsecutorEntity;
import uk.gov.moj.cpp.prosecutioncase.persistence.repository.CaseCpsProsecutorRepository;

import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

/**
 * DB integration tests for {CaseCpsProsecutorRepository} class
 */

public class CaseCpsProsecutorRepositoryTest {

    @RegisterExtension
    static HibernateTestEntityManagerProvider hibernateTestEntityManagerProvider =
            new HibernateTestEntityManagerProvider("progression-test-persistence-unit");

    private static final UUID ID = UUID.randomUUID();

    private static final String PROSECUTOR = "";

    private static final String OLD_PROSECUTOR = "";

    private CaseCpsProsecutorRepository caseCpsProsecutorRepository;

    @BeforeEach
    void createRepositoriesWithATestEntityManager() {
        caseCpsProsecutorRepository = new CaseCpsProsecutorRepository();
        hibernateTestEntityManagerProvider.injectEntityManagerInto(caseCpsProsecutorRepository);
    }

    private CaseCpsProsecutorEntity caseCpsProsecutorEntity;
    @BeforeEach
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
