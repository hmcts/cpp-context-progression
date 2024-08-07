package uk.gov.moj.cpp.progression.service;

import static javax.json.Json.createObjectBuilder;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.test.utils.core.messaging.JsonEnvelopeBuilder.envelope;
import static uk.gov.justice.services.test.utils.core.messaging.MetadataBuilderFactory.metadataWithRandomUUID;

import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.core.requester.Requester;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.test.utils.core.enveloper.EnveloperFactory;
import uk.gov.justice.services.test.utils.core.messaging.JsonEnvelopeBuilder;

import java.util.Optional;
import java.util.UUID;

import javax.json.JsonObject;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class ProsecutionCaseQueryServiceTest {
    @Mock
    private Requester requester;

    @Spy
    Enveloper enveloper = EnveloperFactory.createEnveloper();

    @InjectMocks
    private ProsecutionCaseQueryService prosecutionCaseQueryService;

    @Captor
    private ArgumentCaptor<JsonEnvelope> envelopeArgumentCaptor;

    private static final String PROGRESSION_QUERY_PROSECUTION_CASES = "progression.query.prosecutioncase";

    @Test
    public void shouldRequestForProsecutionCase() {
        final String caseId = UUID.randomUUID().toString();
        final JsonObject sampleJsonObject = createObjectBuilder().add("prosecutionCase", createObjectBuilder()
                .add("id", caseId)
                .add("prosecutionCaseIdentifier", createObjectBuilder()
                        .add("caseURN" , "case123")
                        .add("prosecutionAuthorityReference", "ref")
                        .add("prosecutionAuthorityOUCode", "ouCode")
                        .build())
                .build())
                .build();

        when(requester.requestAsAdmin(any()))
                .thenReturn(JsonEnvelopeBuilder.envelope().withPayloadFrom(sampleJsonObject).with(metadataWithRandomUUID(PROGRESSION_QUERY_PROSECUTION_CASES)).build());

        //when
        final JsonEnvelope envelope = envelope().with(metadataWithRandomUUID(PROGRESSION_QUERY_PROSECUTION_CASES))
                .build();
        final Optional<JsonObject> result = prosecutionCaseQueryService.getProsecutionCase(
                envelope, UUID.randomUUID().toString());

        //then
        verify(requester).requestAsAdmin(envelopeArgumentCaptor.capture());

        assertThat(result.get().getJsonObject("prosecutionCase").getString("id"), is(caseId));

        verifyNoMoreInteractions(requester);
    }
}
