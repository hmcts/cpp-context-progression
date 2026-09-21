package uk.gov.moj.cpp.prosecutioncase.persistence;

import static java.util.UUID.randomUUID;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;

import uk.gov.justice.services.test.utils.persistence.HibernateTestEntityManagerProvider;
import uk.gov.moj.cpp.prosecutioncase.persistence.entity.ResultListOpaNotice;
import uk.gov.moj.cpp.prosecutioncase.persistence.repository.ResultListOpaNoticeRepository;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

/**
 * Runs ResultListOpaNoticeRepository's queries against in-memory H2. deleteByDefendantId was a DeltaSpike
 * derived delete, so the generated JPQL is worth executing rather than assuming.
 */
public class ResultListOpaNoticeRepositoryTest {

    @RegisterExtension
    static HibernateTestEntityManagerProvider hibernateTestEntityManagerProvider =
            new HibernateTestEntityManagerProvider("progression-test-persistence-unit");

    private ResultListOpaNoticeRepository resultListOpaNoticeRepository;

    @BeforeEach
    void createRepositoryWithATestEntityManager() {
        resultListOpaNoticeRepository = new ResultListOpaNoticeRepository();
        hibernateTestEntityManagerProvider.injectEntityManagerInto(resultListOpaNoticeRepository);
    }

    @Test
    public void shouldSaveAndReadBackANotice() {
        final UUID defendantId = randomUUID();
        resultListOpaNoticeRepository.save(new ResultListOpaNotice(randomUUID(), defendantId, randomUUID()));

        final ResultListOpaNotice found = resultListOpaNoticeRepository.findBy(defendantId);

        assertThat(found, is(notNullValue()));
        assertThat(found.getDefendantId(), is(defendantId));
    }

    @Test
    public void shouldDeleteOnlyTheNoticeForTheGivenDefendant() {
        final UUID defendantId = randomUUID();
        final UUID otherDefendantId = randomUUID();
        resultListOpaNoticeRepository.save(new ResultListOpaNotice(randomUUID(), defendantId, randomUUID()));
        resultListOpaNoticeRepository.save(new ResultListOpaNotice(randomUUID(), otherDefendantId, randomUUID()));

        resultListOpaNoticeRepository.deleteByDefendantId(defendantId);

        // a bulk delete does not evict the first-level cache, so the database is queried rather
        // than findBy, which would hand back the still-cached instance
        final List<UUID> remaining = resultListOpaNoticeRepository.findAll().stream()
                .map(ResultListOpaNotice::getDefendantId)
                .toList();
        assertThat(remaining, not(hasItem(defendantId)));
        assertThat(remaining, hasItem(otherDefendantId));
    }

    @Test
    public void shouldDeleteNothingWhenTheDefendantHasNoNotice() {
        resultListOpaNoticeRepository.deleteByDefendantId(randomUUID());
    }
}
