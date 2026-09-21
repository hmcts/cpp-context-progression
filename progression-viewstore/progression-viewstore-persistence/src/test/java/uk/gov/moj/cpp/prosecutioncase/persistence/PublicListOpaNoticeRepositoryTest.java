package uk.gov.moj.cpp.prosecutioncase.persistence;

import static java.util.UUID.randomUUID;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;

import uk.gov.justice.services.test.utils.persistence.HibernateTestEntityManagerProvider;
import uk.gov.moj.cpp.prosecutioncase.persistence.entity.PublicListOpaNotice;
import uk.gov.moj.cpp.prosecutioncase.persistence.repository.PublicListOpaNoticeRepository;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

/**
 * Runs PublicListOpaNoticeRepository's queries against in-memory H2. deleteByDefendantId was a DeltaSpike
 * derived delete, so the generated JPQL is worth executing rather than assuming.
 */
public class PublicListOpaNoticeRepositoryTest {

    @RegisterExtension
    static HibernateTestEntityManagerProvider hibernateTestEntityManagerProvider =
            new HibernateTestEntityManagerProvider("progression-test-persistence-unit");

    private PublicListOpaNoticeRepository publicListOpaNoticeRepository;

    @BeforeEach
    void createRepositoryWithATestEntityManager() {
        publicListOpaNoticeRepository = new PublicListOpaNoticeRepository();
        hibernateTestEntityManagerProvider.injectEntityManagerInto(publicListOpaNoticeRepository);
    }

    @Test
    public void shouldSaveAndReadBackANotice() {
        final UUID defendantId = randomUUID();
        publicListOpaNoticeRepository.save(new PublicListOpaNotice(randomUUID(), defendantId, randomUUID()));

        final PublicListOpaNotice found = publicListOpaNoticeRepository.findBy(defendantId);

        assertThat(found, is(notNullValue()));
        assertThat(found.getDefendantId(), is(defendantId));
    }

    @Test
    public void shouldDeleteOnlyTheNoticeForTheGivenDefendant() {
        final UUID defendantId = randomUUID();
        final UUID otherDefendantId = randomUUID();
        publicListOpaNoticeRepository.save(new PublicListOpaNotice(randomUUID(), defendantId, randomUUID()));
        publicListOpaNoticeRepository.save(new PublicListOpaNotice(randomUUID(), otherDefendantId, randomUUID()));

        publicListOpaNoticeRepository.deleteByDefendantId(defendantId);

        // a bulk delete does not evict the first-level cache, so the database is queried rather
        // than findBy, which would hand back the still-cached instance
        final List<UUID> remaining = publicListOpaNoticeRepository.findAll().stream()
                .map(PublicListOpaNotice::getDefendantId)
                .toList();
        assertThat(remaining, not(hasItem(defendantId)));
        assertThat(remaining, hasItem(otherDefendantId));
    }

    @Test
    public void shouldDeleteNothingWhenTheDefendantHasNoNotice() {
        publicListOpaNoticeRepository.deleteByDefendantId(randomUUID());
    }
}
