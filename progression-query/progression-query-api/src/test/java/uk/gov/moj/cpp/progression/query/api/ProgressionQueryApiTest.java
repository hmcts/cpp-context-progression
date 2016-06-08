package uk.gov.moj.cpp.progression.query.api;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.when;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import uk.gov.justice.services.core.dispatcher.Requester;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.progression.query.api.ProgressionQueryApi;

@RunWith(MockitoJUnitRunner.class)
public class ProgressionQueryApiTest {
    @Mock
    private JsonEnvelope query;
    @Mock
    private JsonEnvelope response;

    @Mock
    private Requester requester;

    @InjectMocks
    private ProgressionQueryApi progressionHearingsQueryApi;

    @Test
    public void shouldHandleCaseprogressiondetailQuery() {
        when(requester.request(query)).thenReturn(response);
        assertThat(progressionHearingsQueryApi.getCaseprogressiondetail(query), equalTo(response));
    }

    @Test
    public void shouldHandleTimelineQuery() {
        when(requester.request(query)).thenReturn(response);
        assertThat(progressionHearingsQueryApi.getTimeline(query), equalTo(response));
    }
    
    @Test
    public void shouldHandleIndicatestatementsdetailQuery() {
        when(requester.request(query)).thenReturn(response);
        assertThat(progressionHearingsQueryApi.getIndicatestatementsdetails(query), equalTo(response));
    }
    
    @Test
    public void shouldHandleIndicatestatementdetailQuery() {
        when(requester.request(query)).thenReturn(response);
        assertThat(progressionHearingsQueryApi.getIndicatestatementsdetail(query), equalTo(response));
    }
    
    @Test
    public void shouldHandleCaseaQuery() {
        when(requester.request(query)).thenReturn(response);
        assertThat(progressionHearingsQueryApi.getCases(query), equalTo(response));
    }
}
