package uk.gov.moj.cpp.progression.query.api;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import uk.gov.justice.services.core.dispatcher.Requester;
import uk.gov.justice.services.messaging.JsonEnvelope;

import javax.json.JsonObject;

import static javax.json.Json.createArrayBuilder;
import static javax.json.Json.createObjectBuilder;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.when;

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
    public void shouldReturnListOfMagistrateCourtsForLCC() {
        // given
        final JsonObject queryPayload = createObjectBuilder().add("crownCourtId", "LCC").build();
        when(query.payloadAsJsonObject()).thenReturn(queryPayload);

        // when
        final JsonEnvelope jsonEnvelope = progressionHearingsQueryApi.getMagistratesCourts(query);

        // then
        final JsonObject expectedPayload = createObjectBuilder()
                .add("values", createArrayBuilder()
                        .add(createObjectBuilder().add("name", "Liverpool & Knowsley Magistrates Court"))
                        .add(createObjectBuilder().add("name", "Ormskirk Magistrates Court"))
                        .add(createObjectBuilder().add("name", "Sefton Magistrates Court"))
                        .add(createObjectBuilder().add("name", "St Helens Magistrates Court"))
                        .add(createObjectBuilder().add("name", "Wigan Magistrates Court"))
                        .add(createObjectBuilder().add("name", "Wirral Magistrates Court"))
                        .add(createObjectBuilder().add("name", "Other"))
                        .build())
                .build();
        assertThat(jsonEnvelope.payloadAsJsonObject(), is(expectedPayload));
    }
}
