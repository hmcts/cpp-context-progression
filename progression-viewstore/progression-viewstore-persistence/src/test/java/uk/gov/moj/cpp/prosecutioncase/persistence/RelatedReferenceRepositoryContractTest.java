package uk.gov.moj.cpp.prosecutioncase.persistence;

import static java.util.UUID.randomUUID;
import static java.util.stream.Collectors.toList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;

import uk.gov.moj.cpp.prosecutioncase.persistence.entity.RelatedReference;
import uk.gov.moj.cpp.prosecutioncase.persistence.repository.RelatedReferenceRepository;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import javax.inject.Inject;

import org.apache.deltaspike.testcontrol.api.junit.CdiTestRunner;
import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Result contracts for the repository's current callers, before replacing its implementation.
 * These tests use the existing container harness and do not establish production transaction
 * boundaries or whether another injection approach would share the same persistence context.
 */
@RunWith(CdiTestRunner.class)
public class RelatedReferenceRepositoryContractTest {

    private final Set<UUID> createdIds = new HashSet<>();

    @Inject
    private RelatedReferenceRepository relatedReferenceRepository;

    @After
    public void removeOnlyThisTestsReferences() {
        for (final UUID id : createdIds) {
            final RelatedReference reference = relatedReferenceRepository.findBy(id);
            if (reference != null) {
                relatedReferenceRepository.remove(reference);
            }
        }
    }

    @Test
    public void shouldSaveAndFindAnAssignedIdReference() {
        final UUID id = randomUUID();
        final UUID caseId = randomUUID();
        saveReference(id, caseId, "SYNTHETIC-REFERENCE-A");

        final RelatedReference found = relatedReferenceRepository.findBy(id);

        assertThat(found, is(notNullValue()));
        assertThat(found.getId(), is(id));
        assertThat(found.getProsecutionCaseId(), is(caseId));
        assertThat(found.getReference(), is("SYNTHETIC-REFERENCE-A"));
    }

    @Test
    public void shouldReturnNullForAnUnknownId() {
        assertThat(relatedReferenceRepository.findBy(randomUUID()), is(nullValue()));
    }

    @Test
    public void shouldReturnAllReferencesForTheRequestedCaseAndExcludeAnotherCase() {
        final UUID requestedCaseId = randomUUID();
        final UUID firstId = randomUUID();
        final UUID secondId = randomUUID();
        saveReference(firstId, requestedCaseId, "SYNTHETIC-REFERENCE-A");
        saveReference(secondId, requestedCaseId, "SYNTHETIC-REFERENCE-B");
        saveReference(randomUUID(), randomUUID(), "SYNTHETIC-EXCLUDED-REFERENCE");

        final List<RelatedReference> found =
                relatedReferenceRepository.findByProsecutionCaseId(requestedCaseId);

        assertThat(found.size(), is(2));
        assertThat(found.stream().map(RelatedReference::getId).collect(toList()),
                containsInAnyOrder(firstId, secondId));
        assertThat(found.stream().allMatch(reference -> requestedCaseId.equals(reference.getProsecutionCaseId())),
                is(true));
    }

    @Test
    public void shouldReturnAnEmptyListForAnUnknownCase() {
        saveReference(randomUUID(), randomUUID(), "SYNTHETIC-EXCLUDED-REFERENCE");

        assertThat(relatedReferenceRepository.findByProsecutionCaseId(randomUUID()).isEmpty(), is(true));
    }

    @Test
    public void shouldUpdateAnExistingIdWithoutAddingAnotherCaseReference() {
        final UUID id = randomUUID();
        final UUID caseId = randomUUID();
        saveReference(id, caseId, "SYNTHETIC-ORIGINAL-REFERENCE");

        // A distinct instance with the same assigned id exercises the existing update contract.
        saveReference(id, caseId, "SYNTHETIC-UPDATED-REFERENCE");

        assertThat(relatedReferenceRepository.findBy(id).getReference(), is("SYNTHETIC-UPDATED-REFERENCE"));
        final List<RelatedReference> found = relatedReferenceRepository.findByProsecutionCaseId(caseId);
        assertThat(found.size(), is(1));
        assertThat(found.get(0).getId(), is(id));
        assertThat(found.get(0).getReference(), is("SYNTHETIC-UPDATED-REFERENCE"));
    }

    @Test
    public void shouldRemoveASavedReference() {
        final UUID id = randomUUID();
        final UUID caseId = randomUUID();
        saveReference(id, caseId, "SYNTHETIC-REFERENCE-TO-REMOVE");

        relatedReferenceRepository.remove(relatedReferenceRepository.findBy(id));

        assertThat(relatedReferenceRepository.findBy(id), is(nullValue()));
        assertThat(relatedReferenceRepository.findByProsecutionCaseId(caseId).isEmpty(), is(true));
    }

    private void saveReference(final UUID id, final UUID caseId, final String text) {
        final RelatedReference reference = new RelatedReference();
        reference.setId(id);
        reference.setProsecutionCaseId(caseId);
        reference.setReference(text);
        createdIds.add(id);
        relatedReferenceRepository.save(reference);
    }
}
