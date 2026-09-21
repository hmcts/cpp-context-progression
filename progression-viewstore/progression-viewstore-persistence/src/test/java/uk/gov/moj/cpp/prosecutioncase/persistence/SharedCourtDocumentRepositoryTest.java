package uk.gov.moj.cpp.prosecutioncase.persistence;

import static java.util.Arrays.asList;
import static java.util.UUID.randomUUID;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;

import uk.gov.justice.services.test.utils.persistence.HibernateTestEntityManagerProvider;
import uk.gov.moj.cpp.prosecutioncase.persistence.entity.SharedCourtDocumentEntity;
import uk.gov.moj.cpp.prosecutioncase.persistence.repository.SharedCourtDocumentRepository;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

/**
 * Runs SharedCourtDocumentRepository's queries against in-memory H2, so the JPQL the DeltaSpike migration
 * produced is executed rather than mocked.
 */
public class SharedCourtDocumentRepositoryTest {

    @RegisterExtension
    static HibernateTestEntityManagerProvider hibernateTestEntityManagerProvider =
            new HibernateTestEntityManagerProvider("progression-test-persistence-unit");

    private SharedCourtDocumentRepository sharedCourtDocumentRepository;

    @BeforeEach
    void createRepositoryWithATestEntityManager() {
        sharedCourtDocumentRepository = new SharedCourtDocumentRepository();
        hibernateTestEntityManagerProvider.injectEntityManagerInto(sharedCourtDocumentRepository);
    }

    private static final UUID CASE_ID = randomUUID();
    private static final UUID HEARING_ID = randomUUID();
    private static final UUID USER_GROUP_ID = randomUUID();
    private static final UUID DEFENDANT_ID = randomUUID();

    private SharedCourtDocumentEntity sharedDocument(final UUID defendantId, final int seqNum) {
        return new SharedCourtDocumentEntity(randomUUID(), randomUUID(), HEARING_ID, USER_GROUP_ID,
                randomUUID(), CASE_ID, randomUUID(), defendantId, seqNum);
    }

    @Test
    public void shouldFindDocumentsForTheDefendantAndThoseSharedWithNoDefendant() {
        sharedCourtDocumentRepository.save(sharedDocument(DEFENDANT_ID, 2));
        sharedCourtDocumentRepository.save(sharedDocument(null, 1));
        sharedCourtDocumentRepository.save(sharedDocument(randomUUID(), 3));

        final List<SharedCourtDocumentEntity> found = sharedCourtDocumentRepository
                .findByHearingIdAndDefendantIdForSelectedCaseForUserGroup(
                        CASE_ID, HEARING_ID, USER_GROUP_ID, DEFENDANT_ID);

        assertThat(found, hasSize(2));
        assertThat(found.get(0).getSeqNum(), is(1));
        assertThat(found.get(1).getSeqNum(), is(2));
    }

    @Test
    public void shouldReturnNothingForAnUnrelatedUserGroup() {
        sharedCourtDocumentRepository.save(sharedDocument(DEFENDANT_ID, 1));

        assertThat(sharedCourtDocumentRepository.findByHearingIdAndDefendantIdForSelectedCaseForUserGroup(
                CASE_ID, HEARING_ID, randomUUID(), DEFENDANT_ID), is(empty()));
    }
}
