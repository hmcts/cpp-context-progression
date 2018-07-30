package uk.gov.moj.cpp.progression.activiti.workflow.initiatehearing.service;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static org.hamcrest.core.AllOf.allOf;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.messaging.JsonObjectMetadata.metadataWithRandomUUID;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeMatcher.jsonEnvelope;
import static uk.gov.justice.services.test.utils.core.messaging.JsonEnvelopeBuilder.envelope;

import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.core.requester.Requester;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.test.utils.core.enveloper.EnveloperFactory;
import uk.gov.justice.services.test.utils.core.messaging.JsonEnvelopeBuilder;
import uk.gov.moj.cpp.progression.service.ProgressionService;

import java.io.ByteArrayInputStream;
import java.util.Optional;
import java.util.UUID;

import javax.json.Json;
import javax.json.JsonObject;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.runners.MockitoJUnitRunner;

@SuppressWarnings("unused")
@RunWith(MockitoJUnitRunner.class)
public class ProgressionServiceTest {

    @Mock
    private Requester requester;

    @Spy
    Enveloper enveloper = EnveloperFactory.createEnveloper();;

    @InjectMocks
    private ProgressionService progressionService;

    @Captor
    private ArgumentCaptor<JsonEnvelope> envelopeArgumentCaptor;

    @Test
    public void shouldRequestDefendantByDefendantId() {
        //given

        final String userId = UUID.randomUUID().toString();
        final String caseId = UUID.randomUUID().toString();
        final String defendantId = UUID.randomUUID().toString();
        final JsonObject payload = Json.createReader(
                new ByteArrayInputStream(getJsonPayload(userId, caseId, defendantId).getBytes()))
                .readObject();

        when(requester.request(any()))
                .thenReturn(JsonEnvelopeBuilder.envelopeFrom(metadataWithRandomUUID("progression.query.defendant"), payload));


        //when
        final JsonEnvelope envelope = envelope().with(metadataWithRandomUUID("progression.query.defendant"))
                .build();
        final Optional<JsonObject> result = progressionService.getDefendantByDefendantId(userId, caseId, defendantId);

        //then
        verify(requester).request(envelopeArgumentCaptor.capture());
        assertThat(result.get().getString("caseId"), is(caseId));
        assertThat(result.get().getString("defendantId"), is(defendantId));
        verifyNoMoreInteractions(requester);
    }


    private String getJsonPayload(String userId, String caseId, String defendantId) {

        return "{\n" +
                "\"defendantId\": \"" + defendantId + "\",\n" +
                "\"caseId\": \"" + caseId + "\",\n" +
                "  \"offences\": [\n" +
                "    {\n" +
                "      \"id\": \"f8254db1-1683-483e-afb3-b87fde5a0a26\"\n" +
                "    }\n" +
                "  ]\n" +
                "}";
    }

}
