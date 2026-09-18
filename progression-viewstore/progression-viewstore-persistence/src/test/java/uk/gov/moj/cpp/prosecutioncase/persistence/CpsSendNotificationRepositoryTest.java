package uk.gov.moj.cpp.prosecutioncase.persistence;

import static java.util.UUID.randomUUID;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;

import uk.gov.justice.services.test.utils.persistence.HibernateTestEntityManagerProvider;
import uk.gov.moj.cpp.prosecutioncase.persistence.entity.CpsSendNotificationEntity;
import uk.gov.moj.cpp.prosecutioncase.persistence.repository.CpsSendNotificationRepository;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

/**
 * Runs CpsSendNotificationRepository's queries against in-memory H2 so the JPQL produced by the
 * DeltaSpike migration is actually executed rather than mocked.
 */
public class CpsSendNotificationRepositoryTest {

    @RegisterExtension
    static HibernateTestEntityManagerProvider hibernateTestEntityManagerProvider =
            new HibernateTestEntityManagerProvider("progression-test-persistence-unit");

    private CpsSendNotificationRepository cpsSendNotificationRepository;

    @BeforeEach
    void createRepositoryWithATestEntityManager() {
        cpsSendNotificationRepository = new CpsSendNotificationRepository();
        hibernateTestEntityManagerProvider.injectEntityManagerInto(cpsSendNotificationRepository);
    }

    @Test
    public void shouldSaveAndFindByCourtDocumentId() {
        final UUID courtDocumentId = randomUUID();
        final CpsSendNotificationEntity entity = new CpsSendNotificationEntity();
        entity.setCourtDocumentId(courtDocumentId);
        entity.setSendToCps(true);

        cpsSendNotificationRepository.save(entity);

        final CpsSendNotificationEntity found = cpsSendNotificationRepository.findBy(courtDocumentId);
        assertThat(found, is(notNullValue()));
        assertThat(found.getSendToCps(), is(true));
    }

    @Test
    public void shouldReturnNullWhenTheDocumentIsUnknown() {
        assertThat(cpsSendNotificationRepository.findBy(randomUUID()), is(nullValue()));
    }

    @Test
    public void shouldRemoveAnEntity() {
        final UUID courtDocumentId = randomUUID();
        final CpsSendNotificationEntity entity = new CpsSendNotificationEntity();
        entity.setCourtDocumentId(courtDocumentId);
        entity.setSendToCps(false);
        cpsSendNotificationRepository.save(entity);

        cpsSendNotificationRepository.remove(cpsSendNotificationRepository.findBy(courtDocumentId));

        assertThat(cpsSendNotificationRepository.findBy(courtDocumentId), is(nullValue()));
    }
}
