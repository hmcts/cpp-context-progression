package uk.gov.moj.cpp.progression.command.service;



import static uk.gov.justice.services.messaging.JsonObjects.createObjectBuilder;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.test.utils.core.messaging.JsonEnvelopeBuilder.envelope;
import static uk.gov.justice.services.test.utils.core.messaging.MetadataBuilderFactory.metadataWithRandomUUID;

import uk.gov.justice.services.core.requester.Requester;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.test.utils.core.messaging.JsonEnvelopeBuilder;

import java.util.Optional;
import java.util.UUID;

import javax.json.JsonObject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class ProsecutionCaseQueryServiceTest {

    @Mock
    private Requester requester;

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

        when(requester.request(any()))
                .thenReturn(JsonEnvelopeBuilder.envelope().withPayloadFrom(sampleJsonObject).with(metadataWithRandomUUID(PROGRESSION_QUERY_PROSECUTION_CASES)).build());

        //when
        final JsonEnvelope envelope = envelope().with(metadataWithRandomUUID(PROGRESSION_QUERY_PROSECUTION_CASES))
                .build();
        final Optional<JsonObject> result = prosecutionCaseQueryService.getProsecutionCase(
                envelope, UUID.randomUUID());

        //then
        verify(requester).request(envelopeArgumentCaptor.capture());

        assertThat(result.get().getJsonObject("prosecutionCase").getString("id"), is(caseId));

        verifyNoMoreInteractions(requester);
    }
}
