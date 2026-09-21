package uk.gov.moj.cpp.prosecutioncase.persistence;

import static java.util.UUID.randomUUID;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.sameInstance;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.test.utils.core.reflection.ReflectionUtil.setField;

import uk.gov.moj.cpp.prosecutioncase.persistence.entity.PrisonCourtRegisterEntity;
import uk.gov.moj.cpp.prosecutioncase.persistence.repository.PrisonCourtRegisterRepository;

import java.util.UUID;

import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * findByCourtCentreIdAndHearingIdAndDefendantId is native PostgreSQL: it reaches into the jsonb
 * payload with `cast(p.payload as jsonb)->>'hearingId'`, which H2 cannot parse, so it is the one
 * query in this repository that the H2-backed test cannot execute.
 *
 * Mocking the EntityManager is not as good as running it, and does not claim to be: it proves the
 * repository passes the right SQL and binds the right parameters in the right positions, not that
 * PostgreSQL accepts the SQL. That is still worth having, because the DeltaSpike migration rewrote
 * the parameter binding - DeltaSpike's `?1..?3` became JPA's 1-based positional parameters - and
 * nothing else checks that they did not get transposed.
 *
 * The SQL is asserted verbatim on purpose. Rebuilding the expected string from the same pieces the
 * production code uses would agree with whatever the code does, including a mistake.
 */
@ExtendWith(MockitoExtension.class)
public class PrisonCourtRegisterNativeQueryTest {

    private static final String EXPECTED_SQL =
            "select * FROM prison_court_register p WHERE p.court_centre_id = ?1"
                    + " and cast(p.payload as jsonb)->>'hearingId' = ?2"
                    + " and (cast(p.payload as jsonb)->'defendant')->>'masterDefendantId' = ?3"
                    + " and p.file_id is null";

    @Mock
    private EntityManager entityManager;

    @Mock
    private Query query;

    private PrisonCourtRegisterRepository prisonCourtRegisterRepository;

    @BeforeEach
    void createRepositoryWithAMockedEntityManager() {
        prisonCourtRegisterRepository = new PrisonCourtRegisterRepository();
        setField(prisonCourtRegisterRepository, "entityManager", entityManager);
    }

    @Test
    public void shouldQueryTheJsonbPayloadForOneCourtCentreHearingAndDefendant() {
        final UUID courtCentreId = randomUUID();
        final String hearingId = randomUUID().toString();
        final String defendantId = randomUUID().toString();
        final PrisonCourtRegisterEntity expected = new PrisonCourtRegisterEntity();

        when(entityManager.createNativeQuery(any(String.class), any(Class.class))).thenReturn(query);
        when(query.setParameter(anyInt(), any())).thenReturn(query);
        when(query.getSingleResult()).thenReturn(expected);

        final PrisonCourtRegisterEntity found = prisonCourtRegisterRepository
                .findByCourtCentreIdAndHearingIdAndDefendantId(courtCentreId, hearingId, defendantId);

        assertThat(found, is(sameInstance(expected)));

        final ArgumentCaptor<String> sql = ArgumentCaptor.forClass(String.class);
        verify(entityManager).createNativeQuery(sql.capture(), any(Class.class));
        assertThat(sql.getValue(), is(EXPECTED_SQL));

        verify(query).setParameter(1, courtCentreId);
        verify(query).setParameter(2, hearingId);
        verify(query).setParameter(3, defendantId);
    }

    @Test
    public void shouldAskForThePrisonCourtRegisterEntityTypeSoTheRowIsMapped() {
        when(entityManager.createNativeQuery(any(String.class), any(Class.class))).thenReturn(query);
        when(query.setParameter(anyInt(), any())).thenReturn(query);
        when(query.getSingleResult()).thenReturn(new PrisonCourtRegisterEntity());

        prisonCourtRegisterRepository.findByCourtCentreIdAndHearingIdAndDefendantId(
                randomUUID(), randomUUID().toString(), randomUUID().toString());

        final ArgumentCaptor<Class> mappedType = ArgumentCaptor.forClass(Class.class);
        verify(entityManager).createNativeQuery(any(String.class), mappedType.capture());
        assertThat(mappedType.getValue(), is(PrisonCourtRegisterEntity.class));
    }
}
