package uk.gov.moj.cpp.progression.query.api;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.when;

import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.progression.query.CaseLsmInfoQuery;

import org.junit.jupiter.api.Test;

import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
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