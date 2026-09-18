package uk.gov.moj.cpp.prosecutioncase.persistence;

import static java.util.Arrays.asList;
import static java.util.UUID.randomUUID;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;

import uk.gov.justice.services.test.utils.persistence.HibernateTestEntityManagerProvider;
import uk.gov.moj.cpp.progression.domain.constant.FeeStatus;
import uk.gov.moj.cpp.progression.domain.constant.FeeType;
import uk.gov.moj.cpp.prosecutioncase.persistence.entity.CivilFeeEntity;
import uk.gov.moj.cpp.prosecutioncase.persistence.repository.CivilFeeRepository;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

/**
 * Runs CivilFeeRepository's queries against in-memory H2, so the JPQL the DeltaSpike migration
 * produced is executed rather than mocked.
 */
public class CivilFeeRepositoryTest {

    @RegisterExtension
    static HibernateTestEntityManagerProvider hibernateTestEntityManagerProvider =
            new HibernateTestEntityManagerProvider("progression-test-persistence-unit");

    private CivilFeeRepository civilFeeRepository;

    @BeforeEach
    void createRepositoryWithATestEntityManager() {
        civilFeeRepository = new CivilFeeRepository();
        hibernateTestEntityManagerProvider.injectEntityManagerInto(civilFeeRepository);
    }

    private CivilFeeEntity civilFee(final UUID feeId, final FeeType feeType,
                                    final FeeStatus feeStatus, final String paymentReference) {
        final CivilFeeEntity civilFee = new CivilFeeEntity(feeId, feeType, feeStatus, paymentReference);
        civilFee.setProsecutorId(randomUUID());
        return civilFee;
    }

    @Test
    public void shouldFindTheFeesMatchingTheSuppliedIds() {
        final UUID firstFeeId = randomUUID();
        final UUID secondFeeId = randomUUID();
        final UUID unrelatedFeeId = randomUUID();

        civilFeeRepository.save(civilFee(firstFeeId, FeeType.INITIAL, FeeStatus.OUTSTANDING, "REF1"));
        civilFeeRepository.save(civilFee(secondFeeId, FeeType.CONTESTED, FeeStatus.SATISFIED, "REF2"));
        civilFeeRepository.save(civilFee(unrelatedFeeId, FeeType.INITIAL, FeeStatus.WAIVED, "REF3"));

        final List<CivilFeeEntity> found = civilFeeRepository.findByFeeIds(asList(firstFeeId, secondFeeId));

        assertThat(found, hasSize(2));
        assertThat(found.stream().map(CivilFeeEntity::getFeeId).toList(),
                containsInAnyOrder(firstFeeId, secondFeeId));
    }

    @Test
    public void shouldReturnNothingWhenNoFeeIdMatches() {
        assertThat(civilFeeRepository.findByFeeIds(asList(randomUUID())), is(empty()));
    }

    @Test
    public void shouldSaveAndReadBackASingleFee() {
        final UUID feeId = randomUUID();
        civilFeeRepository.save(civilFee(feeId, FeeType.INITIAL, FeeStatus.OUTSTANDING, "REF"));

        final CivilFeeEntity found = civilFeeRepository.findBy(feeId);

        assertThat(found, is(notNullValue()));
        assertThat(found.getFeeType(), is(FeeType.INITIAL));
        assertThat(found.getFeeStatus(), is(FeeStatus.OUTSTANDING));
        assertThat(found.getPaymentReference(), is("REF"));
    }
}
