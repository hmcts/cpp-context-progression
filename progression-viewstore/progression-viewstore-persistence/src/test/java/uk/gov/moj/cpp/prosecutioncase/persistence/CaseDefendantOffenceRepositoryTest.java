package uk.gov.moj.cpp.prosecutioncase.persistence;


import static java.util.UUID.randomUUID;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

import uk.gov.justice.core.courts.FormType;
import uk.gov.moj.cpp.prosecutioncase.persistence.entity.CaseDefendantOffence;
import uk.gov.moj.cpp.prosecutioncase.persistence.repository.CaseDefendantOffenceRepository;

import javax.inject.Inject;

import org.apache.deltaspike.testcontrol.api.junit.CdiTestRunner;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(CdiTestRunner.class)
public class CaseDefendantOffenceRepositoryTest {

    @Inject
    private CaseDefendantOffenceRepository repository;

    @Test
    public void shouldSaveAndReadCaseDefendantOffenceForBCM() {
        final CaseDefendantOffence caseDefendantOffence = new CaseDefendantOffence(randomUUID(), randomUUID(), randomUUID(), randomUUID(), FormType.BCM);

        final CaseDefendantOffence persisted = repository.save(caseDefendantOffence);
        assertPersistedEntity(persisted, caseDefendantOffence);
    }

    @Test
    public void shouldSaveAndReadCaseDefendantOffenceForBCMWithNullOffenceId() {
        final CaseDefendantOffence caseDefendantOffence = new CaseDefendantOffence(randomUUID(), randomUUID(), randomUUID(), randomUUID(), FormType.BCM);

        final CaseDefendantOffence persisted = repository.save(caseDefendantOffence);
        assertPersistedEntity(persisted, caseDefendantOffence);
    }

    private void assertPersistedEntity(CaseDefendantOffence persisted, CaseDefendantOffence caseDefendantOffence) {
        assertThat(persisted, is(notNullValue()));
        assertThat(persisted.getId(), is(caseDefendantOffence.getId()));
        assertThat(persisted.getCaseId(), is(caseDefendantOffence.getCaseId()));
        assertThat(persisted.getFormType(), is(FormType.BCM));
    }

}
