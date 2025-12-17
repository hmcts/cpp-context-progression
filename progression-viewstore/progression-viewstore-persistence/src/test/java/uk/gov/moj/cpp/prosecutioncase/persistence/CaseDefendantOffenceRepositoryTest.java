package uk.gov.moj.cpp.prosecutioncase.persistence;


import static java.util.UUID.randomUUID;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

import uk.gov.justice.core.courts.FormType;
import uk.gov.moj.cpp.prosecutioncase.persistence.entity.CaseDefendantOffence;
import uk.gov.moj.cpp.prosecutioncase.persistence.repository.CaseDefendantOffenceRepository;

import java.time.ZonedDateTime;

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

        final CaseDefendantOffence caseDefendantOffence1 = new CaseDefendantOffence(randomUUID(), randomUUID(), randomUUID(), randomUUID(), FormType.BCM, ZonedDateTime.now());
        final CaseDefendantOffence persisted1 = repository.save(caseDefendantOffence1);
        assertPersistedEntity(persisted1, caseDefendantOffence1);
    }

    @Test
    public void shouldSaveAndReadCaseDefendantOffenceForBCMWithNullOffenceId() {
        final CaseDefendantOffence caseDefendantOffence = new CaseDefendantOffence();
        caseDefendantOffence.setId(randomUUID());
        caseDefendantOffence.setCaseId(randomUUID());
        caseDefendantOffence.setCourtFormId(randomUUID());
        caseDefendantOffence.setDefendantId(randomUUID());
        caseDefendantOffence.setFormType(FormType.BCM);
        caseDefendantOffence.setLastUpdated(ZonedDateTime.now());

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
