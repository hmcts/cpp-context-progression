package uk.gov.moj.cpp.progression.query.api;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import uk.gov.justice.services.core.requester.Requester;
import uk.gov.justice.services.messaging.JsonEnvelope;

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
    private Requester requester;

    @InjectMocks
    private CaseLsmInfoQueryApi caseLsmInfoQueryApi;

    @Test
    public void shouldHandleCaseLsmInfo() {
        when(requester.request(query)).thenReturn(response);
        assertThat(caseLsmInfoQueryApi.getProsecutionCaseLsmInfo(query), equalTo(response));
    }
}
