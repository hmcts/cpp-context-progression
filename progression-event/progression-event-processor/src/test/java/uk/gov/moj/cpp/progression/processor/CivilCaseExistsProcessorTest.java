package uk.gov.moj.cpp.progression.processor;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.common.converter.ObjectToJsonObjectConverter;
import uk.gov.justice.services.common.converter.jackson.ObjectMapperProducer;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.test.utils.core.messaging.MetadataBuilderFactory;
import uk.gov.moj.cpp.progression.events.CivilCaseExists;
import javax.json.JsonObject;
import java.util.UUID;

@ExtendWith(MockitoExtension.class)
public class CivilCaseExistsProcessorTest {

    @Mock
    private Sender sender;

    @Captor
    private ArgumentCaptor<Envelope<JsonObject>> envelopeCaptor;

    @InjectMocks
    private CivilCaseExistsProcessor civilCaseExistsProcessor;

    final ObjectMapper objectMapper = new ObjectMapperProducer().objectMapper();

    @Spy
    private JsonObjectToObjectConverter jsonObjectToObjectConverter = new JsonObjectToObjectConverter(objectMapper);

    @Spy
    private ObjectToJsonObjectConverter objectToJsonObjectConverter = new ObjectToJsonObjectConverter(objectMapper);

    @Test
    public void shouldRaiseCivilCaseExistsForGroupCases () {

        final UUID groupId = UUID.randomUUID();
        final UUID prosecutionCaseId = UUID.randomUUID();
        final String caseUrn = "CIVILURN";

        final CivilCaseExists groupCaseExists = CivilCaseExists.civilCaseExists()
                .withGroupId(groupId)
                .withProsecutionCaseId(prosecutionCaseId)
                .withCaseUrn(caseUrn)
                .build();

        final JsonObject payload = objectToJsonObjectConverter.convert(groupCaseExists);
        final JsonEnvelope requestMessage = envelopeFrom(
                MetadataBuilderFactory.metadataWithRandomUUID("progression.event.civil-case-exists"),
                payload);

        civilCaseExistsProcessor.processEvent(requestMessage);

        verify(sender, times(1)).send(envelopeCaptor.capture());

        final Envelope<JsonObject> event = envelopeCaptor.getAllValues().get(0);
        assertThat(event.metadata().name(), is("public.progression.events.civil-case-exists"));
        assertThat(event.payload().getString("groupId"), is(groupId.toString()));
        assertThat(event.payload().getString("prosecutionCaseId"), is(prosecutionCaseId.toString()));
        assertThat(event.payload().getString("caseUrn"), is(caseUrn));

    }

    @Test
    public void shouldRaiseCivilCaseExistsForSingleCase () {

        final UUID prosecutionCaseId = UUID.randomUUID();
        final String caseUrn = "CIVILURN";

        final CivilCaseExists groupCaseExists = CivilCaseExists.civilCaseExists()
                .withProsecutionCaseId(prosecutionCaseId)
                .withCaseUrn(caseUrn)
                .build();

        final JsonObject payload = objectToJsonObjectConverter.convert(groupCaseExists);
        final JsonEnvelope requestMessage = envelopeFrom(
                MetadataBuilderFactory.metadataWithRandomUUID("progression.event.civil-case-exists"),
                payload);

        civilCaseExistsProcessor.processEvent(requestMessage);

        verify(sender, times(1)).send(envelopeCaptor.capture());

        final Envelope<JsonObject> event = envelopeCaptor.getAllValues().get(0);
        assertThat(event.metadata().name(), is("public.progression.events.civil-case-exists"));
        assertThat(event.payload().containsKey("groupId"), is(false));
        assertThat(event.payload().getString("prosecutionCaseId"), is(prosecutionCaseId.toString()));
        assertThat(event.payload().getString("caseUrn"), is(caseUrn));

    }

}
