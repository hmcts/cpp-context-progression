package uk.gov.justice.api.resource.service;

import static java.util.UUID.randomUUID;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.verify;

import uk.gov.justice.api.resource.utils.FileUtil;
import uk.gov.justice.services.core.requester.Requester;
import uk.gov.justice.services.messaging.JsonEnvelope;

import java.io.IOException;
import java.util.UUID;

import javax.json.Json;
import javax.json.JsonObject;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class StagingPubHubServiceTest {

    @Mock
    private Requester requester;
    @Captor
    private ArgumentCaptor<JsonEnvelope> envelopeArgumentCaptor;
    @InjectMocks
    private StagingPubHubService stagingPubHubService;

    @Test
    public void shouldReturnOrganisationDetails() throws IOException {

        //given
        final UUID userId = randomUUID();
        final JsonObject standardList = FileUtil.jsonFromPath("stub-data/stagingpubhub.command.publish-standard-list.json");

        //when
        stagingPubHubService.publishStandardList(standardList, userId);
        final JsonObject expectedJson = Json.createObjectBuilder()
                .add("standardList", standardList)
                .build();


        //then
        verify(requester).request(envelopeArgumentCaptor.capture());
        final JsonEnvelope envelope = envelopeArgumentCaptor.getValue();
        assertEquals("stagingpubhub.command.publish-standard-list", envelope.metadata().name());
        assertEquals(expectedJson, envelope.payloadAsJsonObject());

    }

}
