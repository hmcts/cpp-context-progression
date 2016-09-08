package uk.gov.moj.cpp.progression.command.controller;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static java.util.Optional.of;

import java.util.List;
import java.util.UUID;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.JsonValue;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import uk.gov.justice.services.core.dispatcher.Requester;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.progression.command.controller.service.StructureReadService;
import uk.gov.moj.cpp.systemusers.ServiceContextSystemUserProvider;

@RunWith(MockitoJUnitRunner.class)
public class StructureReadServiceTest {


    private static final String CASE_ID = UUID.randomUUID().toString();

    private static final UUID SYSTEM_USER_ID = UUID.randomUUID();

    @InjectMocks
    private StructureReadService service;

    @Mock
    private ServiceContextSystemUserProvider serviceContextSystemUserProvider;
    
    @Mock
    private Requester requester;

    @Mock
    private JsonEnvelope jsonEnvelope;

    @Mock
    private JsonArray jsonArray;

    @Mock
    private JsonObject jsonObject;

    @Captor
    private ArgumentCaptor<JsonEnvelope> jsonCapture;

    @Before
    public void setUp(){
    when(serviceContextSystemUserProvider.getContextSystemUserId()).thenReturn(of(SYSTEM_USER_ID));
    }
    
    @Test
    public void shouldReturnListOfDefendantsForGivenCaseTest() {
        String defendantId = UUID.randomUUID().toString();
        JsonArray jsonArray =
                        Json.createArrayBuilder()
                                        .add(Json.createObjectBuilder()
                                                        .add(StructureReadService.DEF_ID,
                                                                        defendantId)
                                                        .build())
                                        .build();
        when(requester.request(any())).thenReturn(jsonEnvelope);
        when(jsonEnvelope.payloadAsJsonObject()).thenReturn(jsonObject);
        when(jsonObject.getJsonArray(StructureReadService.DEFENDANTS)).thenReturn(jsonArray);

        List<String> defendantIds = service.getStructureCaseDefendentsId(CASE_ID);
        verify(requester).request(jsonCapture.capture());
        assertThat(jsonCapture.getValue().metadata().name(),
                        is(StructureReadService.GET_CASE_DEFENDANT_QUERY));
        assertThat(jsonCapture.getValue().payloadAsJsonObject()
                        .getJsonString(StructureReadService.CASE_ID).getString(), is(CASE_ID));

        assertThat("defendant id does not match ", defendantIds, hasItem(equalTo(defendantId)));
    }


    @Test
    public void shouldReturnEmptyListOfDefendantsForGivenCaseTest() {

        when(requester.request(any())).thenReturn(jsonEnvelope);
        when(jsonEnvelope.payload()).thenReturn(JsonValue.NULL);

        List<String> defendantIds = service.getStructureCaseDefendentsId(CASE_ID);
        verify(requester).request(jsonCapture.capture());
        assertThat(jsonCapture.getValue().metadata().name(),
                        is(StructureReadService.GET_CASE_DEFENDANT_QUERY));
        assertThat("Case id does not match ",
                        jsonCapture.getValue().payloadAsJsonObject()
                                        .getJsonString(StructureReadService.CASE_ID).getString(),
                        is(CASE_ID));

        assertThat(defendantIds.size(), equalTo(0));
    }


}
