package uk.gov.moj.cpp.progression.query.view.service;

import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.when;
import static uk.gov.moj.cpp.progression.query.view.service.ReferenceDataService.REFERENCEDATA_GET_PROSECUTOR;

import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.core.requester.Requester;
import uk.gov.justice.services.messaging.JsonEnvelope;

import java.util.Optional;
import java.util.UUID;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonObject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class ReferenceDataServiceTest {

    @Mock
    private Requester requester;

    @Mock
    private Enveloper enveloper;

    @Mock
    private JsonEnvelope jsonEnvelope;

    @Mock
    private JsonEnvelope queryEnvelope;

    @InjectMocks
    private ReferenceDataService referenceDataService;

    @Mock
    private JsonObject responsePayload;

    @Captor
    private ArgumentCaptor<JsonEnvelope> requestJsonEnvelope;

    private String prosecutorId = UUID.randomUUID().toString();

    @Test
    public void shouldReturnProsecutorJson() {

        when(requester.requestAsAdmin(requestJsonEnvelope.capture())).thenReturn(jsonEnvelope);
        when(jsonEnvelope.payloadAsJsonObject()).thenReturn(responsePayload);

        Optional<JsonObject> prosecutorJson = referenceDataService.getProsecutor(prosecutorId);

        assertThat(prosecutorJson.isPresent(), is(true));
        assertThat(requestJsonEnvelope.getValue().payloadAsJsonObject().getString("id"), is(prosecutorId));
        assertThat(requestJsonEnvelope.getValue().metadata().id(), notNullValue());
        assertThat(requestJsonEnvelope.getValue().metadata().name(), is(REFERENCEDATA_GET_PROSECUTOR));
    }

    @Test
    public void shouldNotReturnProsecutorJson() {

        when(requester.requestAsAdmin(requestJsonEnvelope.capture())).thenReturn(jsonEnvelope);
        when(jsonEnvelope.payloadAsJsonObject()).thenReturn(null);

        Optional<JsonObject> prosecutorJson = referenceDataService.getProsecutor(prosecutorId);

        assertThat(prosecutorJson.isPresent(), is(false));
        assertThat(requestJsonEnvelope.getValue().payloadAsJsonObject().getString("id"), is(prosecutorId));
        assertThat(requestJsonEnvelope.getValue().metadata().id(), notNullValue());
        assertThat(requestJsonEnvelope.getValue().metadata().name(), is(REFERENCEDATA_GET_PROSECUTOR));
    }

    @Test
    void shouldGetHearingTypes() {
        when(requester.request(requestJsonEnvelope.capture())).thenReturn(jsonEnvelope);
        final JsonArray hearingTypes = Json.createArrayBuilder()
                .add(Json.createObjectBuilder().add("id", UUID.randomUUID().toString()).build())
                .build();
        when(queryEnvelope.metadata()).thenReturn(JsonEnvelope.metadataBuilder().withId(UUID.randomUUID()).withName("name").build());
        when(jsonEnvelope.payloadAsJsonObject()).thenReturn(Json.createObjectBuilder()
                .add("hearingTypes", hearingTypes)
                .build()
        );

        final JsonArray result = referenceDataService.getHearingTypes(queryEnvelope);

        assertThat(result, is(hearingTypes));
        assertThat(requestJsonEnvelope.getValue().metadata().name(), is("referencedata.query.hearing-types"));

    }
}
