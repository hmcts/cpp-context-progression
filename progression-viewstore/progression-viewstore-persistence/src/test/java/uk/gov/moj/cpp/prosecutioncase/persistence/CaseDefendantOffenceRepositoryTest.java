package uk.gov.moj.cpp.prosecutioncase.persistence;

import static org.hamcrest.MatcherAssert.assertThat;
import static java.util.UUID.randomUUID;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.core.Is.is;

import uk.gov.justice.services.test.utils.persistence.HibernateTestEntityManagerProvider;
import uk.gov.justice.core.courts.FormType;
import uk.gov.moj.cpp.prosecutioncase.persistence.entity.CaseDefendantOffence;
import uk.gov.moj.cpp.prosecutioncase.persistence.repository.CaseDefendantOffenceRepository;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

public class CaseDefendantOffenceRepositoryTest {

    @RegisterExtension
    static HibernateTestEntityManagerProvider hibernateTestEntityManagerProvider =
            new HibernateTestEntityManagerProvider("progression-test-persistence-unit");

    private CaseDefendantOffenceRepository repository;

    @BeforeEach
    void createRepositoriesWithATestEntityManager() {
        repository = new CaseDefendantOffenceRepository();
        hibernateTestEntityManagerProvider.injectEntityManagerInto(repository);
    }

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

    private CaseDefendantOffence caseDefendantOffence(final UUID id, final UUID caseId,
                                                      final UUID courtFormId, final FormType formType) {
        final CaseDefendantOffence caseDefendantOffence = new CaseDefendantOffence();
        caseDefendantOffence.setId(id);
        caseDefendantOffence.setCaseId(caseId);
        caseDefendantOffence.setCourtFormId(courtFormId);
        caseDefendantOffence.setDefendantId(randomUUID());
        caseDefendantOffence.setFormType(formType);
        caseDefendantOffence.setLastUpdated(ZonedDateTime.now());
        return caseDefendantOffence;
    }

    @Test
    public void shouldFindTheOffencesMatchingTheGivenId() {
        final UUID id = randomUUID();
        repository.saveAndFlush(caseDefendantOffence(id, randomUUID(), randomUUID(), FormType.BCM));
        repository.saveAndFlush(caseDefendantOffence(randomUUID(), randomUUID(), randomUUID(), FormType.BCM));

        final List<CaseDefendantOffence> found = repository.findById(id);

        assertThat(found, hasSize(1));
        assertThat(found.get(0).getId(), is(id));
    }

    @Test
    public void shouldReturnNothingWhenNoOffenceHasThatId() {
        assertThat(repository.findById(randomUUID()), is(empty()));
    }

    @Test
    public void shouldFindEveryOffenceOnACase() {
        final UUID caseId = randomUUID();
        repository.saveAndFlush(caseDefendantOffence(randomUUID(), caseId, randomUUID(), FormType.BCM));
        repository.saveAndFlush(caseDefendantOffence(randomUUID(), caseId, randomUUID(), FormType.BCM));
        repository.saveAndFlush(caseDefendantOffence(randomUUID(), randomUUID(), randomUUID(), FormType.BCM));

        final List<CaseDefendantOffence> found = repository.findByCaseId(caseId);

        assertThat(found, hasSize(2));
        assertThat(found.stream().map(CaseDefendantOffence::getCaseId).distinct().toList(), contains(caseId));
    }

    @Test
    public void shouldNarrowTheOffencesOnACaseToOneFormType() {
        final UUID caseId = randomUUID();
        final UUID bcmId = randomUUID();
        repository.saveAndFlush(caseDefendantOffence(bcmId, caseId, randomUUID(), FormType.BCM));
        repository.saveAndFlush(caseDefendantOffence(randomUUID(), caseId, randomUUID(), FormType.PET));

        final List<CaseDefendantOffence> found = repository.findByCaseIdAndFormType(caseId, FormType.BCM);

        assertThat(found, hasSize(1));
        assertThat(found.get(0).getId(), is(bcmId));
        assertThat(found.get(0).getFormType(), is(FormType.BCM));
    }

    @Test
    public void shouldReturnNothingWhenTheCaseHasNoOffenceOfThatFormType() {
        final UUID caseId = randomUUID();
        repository.saveAndFlush(caseDefendantOffence(randomUUID(), caseId, randomUUID(), FormType.BCM));

        assertThat(repository.findByCaseIdAndFormType(caseId, FormType.PET), is(empty()));
    }

    @Test
    public void shouldFindEveryOffenceOnACourtForm() {
        final UUID courtFormId = randomUUID();
        repository.saveAndFlush(caseDefendantOffence(randomUUID(), randomUUID(), courtFormId, FormType.BCM));
        repository.saveAndFlush(caseDefendantOffence(randomUUID(), randomUUID(), courtFormId, FormType.BCM));
        repository.saveAndFlush(caseDefendantOffence(randomUUID(), randomUUID(), randomUUID(), FormType.BCM));

        final List<CaseDefendantOffence> found = repository.findByCourtFormId(courtFormId);

        assertThat(found, hasSize(2));
        assertThat(found.stream().map(CaseDefendantOffence::getCourtFormId).distinct().toList(), contains(courtFormId));
    }

    @Test
    public void shouldReturnNothingWhenNoOffenceIsOnThatCourtForm() {
        assertThat(repository.findByCourtFormId(randomUUID()), is(empty()));
    }

    private void assertPersistedEntity(CaseDefendantOffence persisted, CaseDefendantOffence caseDefendantOffence) {
        assertThat(persisted, is(notNullValue()));
        assertThat(persisted.getId(), is(caseDefendantOffence.getId()));
        assertThat(persisted.getCaseId(), is(caseDefendantOffence.getCaseId()));
        assertThat(persisted.getFormType(), is(FormType.BCM));
    }

}
