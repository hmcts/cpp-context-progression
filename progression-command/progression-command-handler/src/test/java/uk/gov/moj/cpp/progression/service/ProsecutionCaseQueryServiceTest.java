package uk.gov.moj.cpp.progression.service;

import static javax.json.Json.createObjectBuilder;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.test.utils.core.messaging.JsonEnvelopeBuilder.envelope;
import static uk.gov.justice.services.test.utils.core.messaging.MetadataBuilderFactory.metadataWithRandomUUID;

import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.core.requester.Requester;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.test.utils.core.enveloper.EnveloperFactory;
import uk.gov.justice.services.test.utils.core.messaging.JsonEnvelopeBuilder;
import uk.gov.moj.cpp.progression.command.helper.FileResourceObjectMapper;

import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;

import javax.json.JsonObject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class ProsecutionCaseQueryServiceTest {
    @Mock
    private Requester requester;

    @Spy
    Enveloper enveloper = EnveloperFactory.createEnveloper();

    @InjectMocks
    private ProsecutionCaseQueryService prosecutionCaseQueryService;
    @Mock
    private Function<Object, JsonEnvelope> function;

    @Captor
    private ArgumentCaptor<JsonEnvelope> envelopeArgumentCaptor;
    private final FileResourceObjectMapper handlerTestHelper = new FileResourceObjectMapper();

    private static final String PROGRESSION_QUERY_PROSECUTION_CASES = "progression.query.prosecutioncase";
    private static final String PROGRESSION_QUERY_ALL_CASE_HEARINGS = "progression.query.case.allhearings";

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

    @Test
    public void shouldGetAllHearingIdsFromCase() throws IOException {
        final UUID caseId = UUID.randomUUID();
        final JsonEnvelope envelope = envelope().with(metadataWithRandomUUID(PROGRESSION_QUERY_PROSECUTION_CASES))
                .build();
        final JsonObject sampleJsonObject = handlerTestHelper.convertFromFile("json/progression.query.case.allhearings.json", JsonObject.class);
        when(requester.request(any(Envelope.class))).thenReturn(JsonEnvelopeBuilder.envelope()
                .withPayloadFrom(sampleJsonObject).with(metadataWithRandomUUID(PROGRESSION_QUERY_ALL_CASE_HEARINGS)).build());
        List<UUID> hearingIds =  prosecutionCaseQueryService.getAllHearingIdsForCase(envelope, caseId);
        assertThat(hearingIds.size(), is(2));

    }
}
