package uk.gov.moj.cpp.progression.query.api;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.when;

import uk.gov.justice.services.core.requester.Requester;
import uk.gov.justice.services.messaging.JsonEnvelope;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class CourtlistQueryApiTest {


    @Mock
    private JsonEnvelope query;

    @Mock
    private JsonEnvelope response;

    @Mock
    private Requester requester;

    @InjectMocks
    private CourtlistQueryApi courtListQueryApi;


    @Test
    public void shouldHandleApplicationQuery() {
        when(requester.request(query)).thenReturn(response);
        assertThat(courtListQueryApi.searchCourtlist(query), equalTo(response));
    }
}
