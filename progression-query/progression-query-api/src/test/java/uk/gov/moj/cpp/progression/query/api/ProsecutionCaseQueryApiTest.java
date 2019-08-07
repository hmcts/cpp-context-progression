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
public class ProsecutionCaseQueryApiTest {
    @Mock
    private JsonEnvelope query;
    @Mock
    private JsonEnvelope response;

    @Mock
    private Requester requester;

    @InjectMocks
    private ProsecutionCaseQueryApi prosecutionCaseQueryApi;

    @Test
    public void shouldHandleProsecutionCaseQuery() {
        when(requester.request(query)).thenReturn(response);
        assertThat(prosecutionCaseQueryApi.getCaseProsecutionCase(query), equalTo(response));
    }

    @Test
    public void shouldHandleSearchProsecutionCaseQuery() {
        when(requester.request(query)).thenReturn(response);
        assertThat(prosecutionCaseQueryApi.searchCaseProsecutionCase(query), equalTo(response));
    }

    @Test
    public void shouldHandleSearchForUserGroupsByMaterialId() {
        when(requester.request(query)).thenReturn(response);
        assertThat(prosecutionCaseQueryApi.searchForUserGroupsByMaterialId(query), equalTo(response));
    }

}
