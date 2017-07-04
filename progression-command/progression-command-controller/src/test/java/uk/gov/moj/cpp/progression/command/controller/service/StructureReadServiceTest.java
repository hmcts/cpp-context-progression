package uk.gov.moj.cpp.progression.command.controller.service;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.when;

import java.util.List;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonValue;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import uk.gov.justice.services.core.dispatcher.Requester;
import uk.gov.justice.services.messaging.JsonEnvelope;

@RunWith(MockitoJUnitRunner.class)
public class StructureReadServiceTest {

    @Mock
    Requester requester;

    @InjectMocks
    private StructureReadService structureReadService;

    @Mock
    JsonEnvelope responseEnvelope;


    @Test
    public void shouldHandleEmptyPayload() {
        when(requester.request(any(JsonEnvelope.class))).thenReturn(responseEnvelope);
        when(responseEnvelope.payload()).thenReturn(JsonValue.NULL);
        final List<String> l = structureReadService.getStructureCaseDefendantsId("caseId", "userId");

        assertThat(l.isEmpty(), equalTo(true));
    }

    @Test
    public void shouldHandleDefendantPayload() {
        final JsonObject jo = Json.createObjectBuilder().add("defendants",
                        Json.createArrayBuilder().add(Json.createObjectBuilder()
                                        .add("id", "4daefec6-5f77-4109-82d9-1e60544a6c05").build())
                                        .build()).build();
        when(requester.request(any(JsonEnvelope.class))).thenReturn(responseEnvelope);
        when(responseEnvelope.payloadAsJsonObject()).thenReturn(jo);
        final List<String> l = structureReadService.getStructureCaseDefendantsId("caseId", "userId");

        assertThat(l.get(0), equalTo("4daefec6-5f77-4109-82d9-1e60544a6c05"));
    }
   
}
