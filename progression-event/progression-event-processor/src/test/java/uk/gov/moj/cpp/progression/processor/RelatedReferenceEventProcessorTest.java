package uk.gov.moj.cpp.progression.processor;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static uk.gov.moj.cpp.progression.utils.FileUtil.jsonFromString;

import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.common.converter.jackson.ObjectMapperProducer;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.test.utils.core.messaging.MetadataBuilderFactory;
import uk.gov.moj.cpp.progression.events.RelatedReferenceAdded;
import uk.gov.moj.cpp.progression.events.RelatedReferenceDeleted;

import java.util.UUID;

import javax.json.JsonObject;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class RelatedReferenceEventProcessorTest {

    private final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    @Mock
    private Sender sender;
    @Spy
    private JsonObjectToObjectConverter jsonObjectToObjectConverter = new JsonObjectToObjectConverter(new ObjectMapperProducer().objectMapper());
    @Captor
    private ArgumentCaptor<Envelope<JsonObject>> envelopeCaptor;
    @InjectMocks
    private RelatedReferenceEventProcessor relateCaseReferenceEventProcessor;

    @Test
    public void shouldEmitPublicEventForRelatedReferenceAddition() throws JsonProcessingException {

        final UUID prosecutionCaseId = UUID.randomUUID();
        final UUID relatedReferenceId = UUID.randomUUID();
        final String relatedReference = "reference123";

        RelatedReferenceAdded relatedReferenceAdded = new RelatedReferenceAdded(prosecutionCaseId, relatedReference, relatedReferenceId);
        final JsonObject payload = jsonFromString(OBJECT_MAPPER.writeValueAsString(relatedReferenceAdded));

        final JsonEnvelope jsonEnvelope =
                JsonEnvelope.envelopeFrom(MetadataBuilderFactory
                                .metadataWithRandomUUID("progression.event.related-reference-added"),
                        payload);

        relateCaseReferenceEventProcessor.handleRelatedReferenceAdded(jsonEnvelope);

        verify(sender, times(1)).send(envelopeCaptor.capture());
        final Envelope<JsonObject> publicEvent = envelopeCaptor.getAllValues().get(0);

        assertThat(publicEvent.metadata().name(), is("public.progression.related-reference-added"));
        assertThat(publicEvent.payload().getString("relatedReference"), is("reference123"));
        assertThat(publicEvent.payload().getString("relatedReferenceId"), is(relatedReferenceId.toString()));
        assertThat(publicEvent.payload().getString("prosecutionCaseId"), is(prosecutionCaseId.toString()));
    }

    @Test
    public void shouldEmitPublicEventForRelatedReferenceDeletion() throws JsonProcessingException {

        final UUID prosecutionCaseId = UUID.randomUUID();
        final UUID relatedReferenceId = UUID.randomUUID();

        RelatedReferenceDeleted relatedReferenceDeleted = new RelatedReferenceDeleted(prosecutionCaseId, relatedReferenceId);
        final JsonObject payload = jsonFromString(OBJECT_MAPPER.writeValueAsString(relatedReferenceDeleted));

        final JsonEnvelope jsonEnvelope =
                JsonEnvelope.envelopeFrom(MetadataBuilderFactory
                                .metadataWithRandomUUID("progression.event.related-reference-deleted"),
                        payload);

        relateCaseReferenceEventProcessor.handleRelatedReferenceDeleted(jsonEnvelope);

        verify(sender, times(1)).send(envelopeCaptor.capture());
        final Envelope<JsonObject> publicEvent = envelopeCaptor.getAllValues().get(0);

        assertThat(publicEvent.metadata().name(), is("public.progression.related-reference-deleted"));
        assertThat(publicEvent.payload().getString("relatedReferenceId"), is(relatedReferenceId.toString()));
        assertThat(publicEvent.payload().getString("prosecutionCaseId"), is(prosecutionCaseId.toString()));
    }
}