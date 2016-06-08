package uk.gov.moj.cpp.progression.query.controller;

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

@RunWith(MockitoJUnitRunner.class)
public class ProgressionQueryControllerTest {
    @Mock
    private JsonEnvelope query;
    @Mock
    private JsonEnvelope response;

    @Mock
    private Requester requester;

    @InjectMocks
    private ProgressionQueryController progressionQueryController;

    @Test
    public void shouldHandleCaseprogressiondetailQuery() {
        when(requester.request(query)).thenReturn(response);
        assertThat(progressionQueryController.getCaseprogressiondetail(query),
                equalTo(response));
    }

    @Test
    public void shouldHandleTimelineQuery() {
        when(requester.request(query)).thenReturn(response);
        assertThat(progressionQueryController.getTimeline(query),
                equalTo(response));
    }

    @Test
    public void shouldHandleIndicatestatementsdetailsQuery() {
        when(requester.request(query)).thenReturn(response);
        assertThat(
                progressionQueryController.getIndicatestatementsdetails(query),
                equalTo(response));
    }

    @Test
    public void shouldHandleIndicatestatementsdetailQuery() {
        when(requester.request(query)).thenReturn(response);
        assertThat(
                progressionQueryController.getIndicatestatementsdetail(query),
                equalTo(response));
    }
    
    @Test
    public void shouldHandleCaseaQuery() {
        when(requester.request(query)).thenReturn(response);
        assertThat(progressionQueryController.getCases(query), equalTo(response));
    }

}
