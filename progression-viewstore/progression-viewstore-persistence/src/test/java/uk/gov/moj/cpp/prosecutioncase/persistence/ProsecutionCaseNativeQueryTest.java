package uk.gov.moj.cpp.prosecutioncase.persistence;

import static java.util.List.of;
import static java.util.UUID.randomUUID;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.test.utils.core.reflection.ReflectionUtil.setField;

import uk.gov.moj.cpp.prosecutioncase.persistence.repository.ProsecutionCaseRepository;

import java.util.List;
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
 * findInactiveMigratedCaseSummaries is native PostgreSQL built on jsonb_build_object, jsonb_agg and
 * LATERAL jsonb_array_elements. H2 has none of those, so this is the one query in the repository
 * the H2-backed test cannot execute.
 *
 * Mocking the EntityManager proves the repository issues the query and binds the case ids, not that
 * PostgreSQL accepts the SQL - it is weaker than running it, and does not pretend otherwise. What it
 * does catch is the shape of the query changing: the assertions below name the specific PostgreSQL
 * constructs the summary depends on, so quietly dropping the LATERAL join or the INACTIVE filter
 * while "tidying" the SQL fails here rather than in an integration test.
 */
@ExtendWith(MockitoExtension.class)
public class ProsecutionCaseNativeQueryTest {

    @Mock
    private EntityManager entityManager;

    @Mock
    private Query query;

    private ProsecutionCaseRepository prosecutionCaseRepository;

    @BeforeEach
    void createRepositoryWithAMockedEntityManager() {
        prosecutionCaseRepository = new ProsecutionCaseRepository();
        setField(prosecutionCaseRepository, "entityManager", entityManager);
    }

    @Test
    public void shouldReturnTheSummariesTheQueryProduces() {
        final List<UUID> caseIds = of(randomUUID(), randomUUID());
        final List<String> summaries = of("{\"inactiveCaseSummary\":{}}");

        when(entityManager.createNativeQuery(anyString())).thenReturn(query);
        when(query.setParameter(anyString(), any())).thenReturn(query);
        when(query.getResultList()).thenReturn(summaries);

        assertThat(prosecutionCaseRepository.findInactiveMigratedCaseSummaries(caseIds), is(summaries));

        verify(query).setParameter("caseIds", caseIds);
    }

    @Test
    public void shouldKeepThePostgresConstructsTheSummaryDependsOn() {
        when(entityManager.createNativeQuery(anyString())).thenReturn(query);
        when(query.setParameter(anyString(), any())).thenReturn(query);
        when(query.getResultList()).thenReturn(of());

        prosecutionCaseRepository.findInactiveMigratedCaseSummaries(of(randomUUID()));

        final ArgumentCaptor<String> sql = ArgumentCaptor.forClass(String.class);
        verify(entityManager).createNativeQuery(sql.capture());
        final String issued = sql.getValue();

        assertThat(issued, containsString("jsonb_build_object"));
        assertThat(issued, containsString("jsonb_agg"));
        assertThat(issued, containsString("LATERAL jsonb_array_elements"));
        assertThat(issued, containsString("'migrationCaseStatus' = 'INACTIVE'"));
        assertThat(issued, containsString("WHERE p.id IN (:caseIds)"));
        assertThat(issued, containsString("GROUP BY p.id, p.payload"));
    }
}
