package uk.gov.moj.cpp.progression.query.api;

import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import uk.gov.justice.services.core.dispatcher.Requester;
import uk.gov.justice.services.messaging.JsonEnvelope;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.spi.JsonProvider;

import static javax.json.Json.createArrayBuilder;
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
    public void shouldHandleCaseaQuery() {
        when(requester.request(query)).thenReturn(response);
        assertThat(progressionHearingsQueryApi.getCases(query), equalTo(response));
    }

    @Test
    public void shouldReturnListOfMagistrateCourtsForLCC() {
        // given
        final JsonObject queryPayload = JsonProvider.provider().createObjectBuilder().add("crownCourtId", "LCC").build();
        when(query.payloadAsJsonObject()).thenReturn(queryPayload);

        // when
        final JsonEnvelope jsonEnvelope = progressionHearingsQueryApi.getMagistratesCourts(query);

        // then
        final JsonObject expectedPayload = JsonProvider.provider().createObjectBuilder()
                .add("values", createArrayBuilder()
                        .add("Liverpool & Knowsley Magistrates Court")
                        .add("Ormskirk Magistrates Court")
                        .add("Sefton Magistrates Court")
                        .add("St Helens Magistrates Court")
                        .add("Wigan Magistrates Court")
                        .add("Wirral Magistrates Court")
                        .add("Other")
                        .build())
                .build();
        assertThat(jsonEnvelope.payloadAsJsonObject(), is(expectedPayload));
    }
}
