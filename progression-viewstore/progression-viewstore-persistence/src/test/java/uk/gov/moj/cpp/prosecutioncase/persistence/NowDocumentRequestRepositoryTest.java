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
import uk.gov.moj.cpp.prosecutioncase.persistence.entity.NowDocumentRequestEntity;
import uk.gov.moj.cpp.prosecutioncase.persistence.repository.NowDocumentRequestRepository;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

/**
 * Runs NowDocumentRequestRepository's queries against in-memory H2, so the JPQL the DeltaSpike migration
 * produced is executed rather than mocked.
 */
public class NowDocumentRequestRepositoryTest {

    @RegisterExtension
    static HibernateTestEntityManagerProvider hibernateTestEntityManagerProvider =
            new HibernateTestEntityManagerProvider("progression-test-persistence-unit");

    private NowDocumentRequestRepository nowDocumentRequestRepository;

    @BeforeEach
    void createRepositoryWithATestEntityManager() {
        nowDocumentRequestRepository = new NowDocumentRequestRepository();
        hibernateTestEntityManagerProvider.injectEntityManagerInto(nowDocumentRequestRepository);
    }

    private NowDocumentRequestEntity nowDocumentRequest(final UUID materialId,
                                                        final UUID requestId,
                                                        final UUID hearingId) {
        final NowDocumentRequestEntity entity = new NowDocumentRequestEntity();
        entity.setMaterialId(materialId);
        entity.setRequestId(requestId);
        entity.setHearingId(hearingId);
        entity.setPayload("{}");
        return entity;
    }

    @Test
    public void shouldFindEveryDocumentRequestForARequestId() {
        final UUID requestId = randomUUID();
        nowDocumentRequestRepository.save(nowDocumentRequest(randomUUID(), requestId, randomUUID()));
        nowDocumentRequestRepository.save(nowDocumentRequest(randomUUID(), requestId, randomUUID()));
        nowDocumentRequestRepository.save(nowDocumentRequest(randomUUID(), randomUUID(), randomUUID()));

        assertThat(nowDocumentRequestRepository.findByRequestId(requestId), hasSize(2));
    }

    @Test
    public void shouldFindEveryDocumentRequestForAHearing() {
        final UUID hearingId = randomUUID();
        nowDocumentRequestRepository.save(nowDocumentRequest(randomUUID(), randomUUID(), hearingId));

        assertThat(nowDocumentRequestRepository.findByHearingId(hearingId), hasSize(1));
    }

    @Test
    public void shouldReturnNothingForAnUnknownRequestOrHearing() {
        assertThat(nowDocumentRequestRepository.findByRequestId(randomUUID()), is(empty()));
        assertThat(nowDocumentRequestRepository.findByHearingId(randomUUID()), is(empty()));
    }
}
