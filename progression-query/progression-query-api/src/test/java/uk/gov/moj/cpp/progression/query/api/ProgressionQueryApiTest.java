package uk.gov.moj.cpp.progression.query.api;

import static javax.json.Json.createArrayBuilder;
import static javax.json.Json.createObjectBuilder;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.when;

import javax.json.JsonObject;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import uk.gov.justice.services.core.requester.Requester;
import uk.gov.justice.services.messaging.JsonEnvelope;

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
    public void shouldHandleCaseQuery() {
        when(requester.request(query)).thenReturn(response);
        assertThat(progressionHearingsQueryApi.getCases(query), equalTo(response));
    }

    @Test
    public void shouldHandleDefendantQuery() {
        when(requester.request(query)).thenReturn(response);
        assertThat(progressionHearingsQueryApi.getDefendant(query), equalTo(response));
    }

    @Test
    public void shouldHandleDefendantsQuery() {
        when(requester.request(query)).thenReturn(response);
        assertThat(progressionHearingsQueryApi.getDefendants(query), equalTo(response));
    }

    @Test
    public void shouldGetDefendantsOffenceQuery() {
        when(requester.request(query)).thenReturn(response);
        assertThat(progressionHearingsQueryApi.findOffences(query), equalTo(response));
    }

    @Test
    public void shouldGetDefendantDocumentQuery() {
        when(requester.request(query)).thenReturn(response);
        assertThat(progressionHearingsQueryApi.getDefendantDocument(query), equalTo(response));
    }

    @Test
    public void shouldGetCaseSearchByMaterialId() {
        when(requester.request(query)).thenReturn(response);
        assertThat(progressionHearingsQueryApi.getCaseSearchByMaterialId(query), equalTo(response));
    }

    @Test
    public void shouldGetCaseByUrn() {
        when(requester.request(query)).thenReturn(response);
        assertThat(progressionHearingsQueryApi.getCaseByUrn(query), equalTo(response));
    }

    @Test
    public void shouldGetHearingByHearingId(){
        when(requester.request(query)).thenReturn(response);
        assertThat(progressionHearingsQueryApi.getHearing(query), equalTo(response));
    }


}
