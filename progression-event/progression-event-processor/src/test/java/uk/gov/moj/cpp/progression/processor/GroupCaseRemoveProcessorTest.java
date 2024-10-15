package uk.gov.moj.cpp.progression.processor;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;

import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.common.converter.ObjectToJsonObjectConverter;
import uk.gov.justice.services.common.converter.jackson.ObjectMapperProducer;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.test.utils.core.messaging.MetadataBuilderFactory;
import uk.gov.moj.cpp.progression.events.LastCaseToBeRemovedFromGroupCasesRejected;

import java.util.UUID;

import javax.json.JsonObject;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class GroupCaseRemoveProcessorTest {

    @Mock
    private Sender sender;

    @Captor
    private ArgumentCaptor<Envelope<JsonObject>> envelopeCaptor;

    @InjectMocks
    private CaseRemoveFromGroupCaseProcessor caseRemoveFromGroupCaseProcessor;

    final ObjectMapper objectMapper = new ObjectMapperProducer().objectMapper();

    @Spy
    private JsonObjectToObjectConverter jsonObjectToObjectConverter = new JsonObjectToObjectConverter(objectMapper);

    @Spy
    private ObjectToJsonObjectConverter objectToJsonObjectConverter = new ObjectToJsonObjectConverter(objectMapper);

    @Test
    public void shouldCaseRemoveFromGroupCase () {
        final UUID groupId = UUID.randomUUID();
        final UUID caseId = UUID.randomUUID();
        final LastCaseToBeRemovedFromGroupCasesRejected lastCaseToBeRemovedFromGroupCasesRejected = LastCaseToBeRemovedFromGroupCasesRejected
                                            .lastCaseToBeRemovedFromGroupCasesRejected().withGroupId(groupId).withCaseId(caseId).build();
        final JsonObject payload = objectToJsonObjectConverter.convert(lastCaseToBeRemovedFromGroupCasesRejected);
        final JsonEnvelope requestMessage = envelopeFrom(
                MetadataBuilderFactory.metadataWithRandomUUID("progression.event.last-case-to-be-removed-from-group-cases-rejected"),
                payload);

        caseRemoveFromGroupCaseProcessor.processEvent(requestMessage);

        verify(sender, times(1)).send(envelopeCaptor.capture());

        final Envelope<JsonObject> event = envelopeCaptor.getAllValues().get(0);
        assertThat(event.metadata().name(), is("public.progression.remove-last-case-in-group-cases-rejected"));
        assertTrue(event.payload().getString("groupId").equals(groupId.toString()));
        assertTrue(event.payload().getString("caseId").equals(caseId.toString()));
    }
}
