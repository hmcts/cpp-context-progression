package uk.gov.moj.cpp.prosecutioncase.persistence;

import static java.util.UUID.randomUUID;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;

import uk.gov.justice.services.test.utils.persistence.HibernateTestEntityManagerProvider;
import uk.gov.moj.cpp.prosecutioncase.persistence.entity.HearingResultLineEntity;
import uk.gov.moj.cpp.prosecutioncase.persistence.repository.HearingResultLineEntityRepository;

import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

/**
 * The repository adds no queries of its own, so what is worth testing is that the identity it hands
 * the base class is the one the entity is stored under - get that wrong and save() takes the
 * persist path for an entity that already exists.
 */
public class HearingResultLineEntityRepositoryTest {

    @RegisterExtension
    static HibernateTestEntityManagerProvider hibernateTestEntityManagerProvider =
            new HibernateTestEntityManagerProvider("progression-test-persistence-unit");

    private HearingResultLineEntityRepository hearingResultLineEntityRepository;

    @BeforeEach
    void createRepositoryWithATestEntityManager() {
        hearingResultLineEntityRepository = new HearingResultLineEntityRepository();
        hibernateTestEntityManagerProvider.injectEntityManagerInto(hearingResultLineEntityRepository);
    }

    @Test
    public void shouldSaveAndReadBackAResultLine() {
        final UUID id = randomUUID();

        hearingResultLineEntityRepository.saveAndFlush(new HearingResultLineEntity(id, "{\"result\":\"line\"}", null));

        final HearingResultLineEntity found = hearingResultLineEntityRepository.findBy(id);

        assertThat(found, is(notNullValue()));
        assertThat(found.getId(), is(id));
        assertThat(found.getPayload(), is("{\"result\":\"line\"}"));
    }

    @Test
    public void shouldUpdateRatherThanDuplicateWhenSavingAResultLineThatAlreadyExists() {
        final UUID id = randomUUID();
        hearingResultLineEntityRepository.saveAndFlush(new HearingResultLineEntity(id, "first", null));

        hearingResultLineEntityRepository.saveAndFlush(new HearingResultLineEntity(id, "second", null));

        assertThat(hearingResultLineEntityRepository.findBy(id).getPayload(), is("second"));
    }

    @Test
    public void shouldReturnNullWhenNoResultLineHasThatId() {
        assertThat(hearingResultLineEntityRepository.findBy(randomUUID()), is(nullValue()));
    }
}
