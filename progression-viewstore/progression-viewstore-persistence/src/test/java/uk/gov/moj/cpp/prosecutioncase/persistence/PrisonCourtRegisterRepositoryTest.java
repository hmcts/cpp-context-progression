package uk.gov.moj.cpp.prosecutioncase.persistence;

import static java.util.UUID.randomUUID;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertThrows;

import uk.gov.justice.services.test.utils.persistence.HibernateTestEntityManagerProvider;
import uk.gov.moj.cpp.prosecutioncase.persistence.entity.PrisonCourtRegisterEntity;
import uk.gov.moj.cpp.prosecutioncase.persistence.repository.PrisonCourtRegisterRepository;

import java.time.LocalDate;
import java.util.UUID;

import jakarta.persistence.NoResultException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

/**
 * findByCourtCentreIdAndHearingIdAndDefendantId is deliberately not covered here: it is native
 * PostgreSQL that casts the payload to jsonb, which H2 cannot parse. It is exercised by the
 * integration tests against a real database instead.
 */
public class PrisonCourtRegisterRepositoryTest {

    @RegisterExtension
    static HibernateTestEntityManagerProvider hibernateTestEntityManagerProvider =
            new HibernateTestEntityManagerProvider("progression-test-persistence-unit");

    private PrisonCourtRegisterRepository prisonCourtRegisterRepository;

    @BeforeEach
    void createRepositoryWithATestEntityManager() {
        prisonCourtRegisterRepository = new PrisonCourtRegisterRepository();
        hibernateTestEntityManagerProvider.injectEntityManagerInto(prisonCourtRegisterRepository);
    }

    private PrisonCourtRegisterEntity prisonCourtRegister(final UUID id, final UUID courtCentreId) {
        final PrisonCourtRegisterEntity entity = new PrisonCourtRegisterEntity();
        entity.setId(id);
        entity.setCourtCentreId(courtCentreId);
        entity.setRecordedDate(LocalDate.of(2026, 9, 17));
        entity.setPayload("{}");
        return entity;
    }

    @Test
    public void shouldFindEveryRegisterForACourtCentre() {
        final UUID courtCentreId = randomUUID();
        prisonCourtRegisterRepository.save(prisonCourtRegister(randomUUID(), courtCentreId));
        prisonCourtRegisterRepository.save(prisonCourtRegister(randomUUID(), courtCentreId));
        prisonCourtRegisterRepository.save(prisonCourtRegister(randomUUID(), randomUUID()));

        assertThat(prisonCourtRegisterRepository.findByCourtCentreId(courtCentreId), hasSize(2));
    }

    @Test
    public void shouldReturnNothingForACourtCentreWithNoRegisters() {
        assertThat(prisonCourtRegisterRepository.findByCourtCentreId(randomUUID()), is(empty()));
    }

    @Test
    public void shouldFindASingleRegisterById() {
        final UUID id = randomUUID();
        prisonCourtRegisterRepository.save(prisonCourtRegister(id, randomUUID()));

        assertThat(prisonCourtRegisterRepository.findById(id), is(notNullValue()));
    }

    /**
     * findById was a DeltaSpike derived finder, which defaulted to SingleResultType.JPA and so threw
     * rather than returning null. That contract is preserved, and this pins it.
     */
    @Test
    public void shouldThrowWhenNoRegisterMatchesTheId() {
        assertThrows(NoResultException.class, () -> prisonCourtRegisterRepository.findById(randomUUID()));
    }
}
