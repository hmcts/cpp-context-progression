package uk.gov.moj.cpp.progression.query.api;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.progression.query.CaseLsmInfoQuery;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class CaseLsmInfoQueryApiTest {
    @Mock
    private JsonEnvelope query;
    @Mock
    private JsonEnvelope response;

    @Mock
    private CaseLsmInfoQuery caseLsmInfoQuery;

    @InjectMocks
    private CaseLsmInfoQueryApi caseLsmInfoQueryApi;

    @Test
    public void shouldHandleCaseLsmInfo() {
        when(caseLsmInfoQuery.getCaseLsmInfo(query)).thenReturn(response);
        assertThat(caseLsmInfoQueryApi.getProsecutionCaseLsmInfo(query), equalTo(response));
    }
}