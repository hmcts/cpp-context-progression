package uk.gov.moj.cpp.progression.processor;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.verify;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeMetadataMatcher.withMetadataEnvelopedFrom;
import static uk.gov.justice.services.test.utils.core.reflection.ReflectionUtil.setField;

import uk.gov.justice.core.courts.CaseNoteAdded;
import uk.gov.justice.services.common.converter.ObjectToJsonObjectConverter;
import uk.gov.justice.services.common.converter.jackson.ObjectMapperProducer;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.test.utils.core.messaging.MetadataBuilderFactory;

import java.time.ZonedDateTime;
import java.util.UUID;

import javax.json.JsonObject;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class CaseNoteAddedProcessorTest {

    @Mock
    private Sender sender;

    @Captor
    private ArgumentCaptor<Envelope<JsonObject>> envelopeCaptor;

    @InjectMocks
    private CaseNoteAddedProcessor processor;

    private ObjectToJsonObjectConverter objectToJsonObjectConverter = new ObjectToJsonObjectConverter();

    @Before
    public void setup() {
        setField(this.objectToJsonObjectConverter, "mapper", new ObjectMapperProducer().objectMapper());
    }

    @Test
    public void processCaseNotesAdded() {
        final CaseNoteAdded caseNoteAdded = CaseNoteAdded.caseNoteAdded()
                .withCaseId(UUID.randomUUID())
                .withNote("Note")
                .withFirstName("FirstName")
                .withLastName("LastName")
                .withCreatedDateTime(ZonedDateTime.now())
                .build();

        final JsonObject caseNoteAddedPayload = objectToJsonObjectConverter.convert(caseNoteAdded);

        final JsonEnvelope requestMessage = envelopeFrom(
                MetadataBuilderFactory.metadataWithRandomUUID("progression.event.case-note-added"),
                caseNoteAddedPayload);

        processor.processCaseNoteAdded(requestMessage);

        verify(sender).send(envelopeCaptor.capture());

        final Envelope<JsonObject> publicEvent = envelopeCaptor.getValue();
        assertThat(publicEvent.metadata(),
                withMetadataEnvelopedFrom(requestMessage).withName("public.progression.case-note-added"));
        JsonObject actualPayload = publicEvent.payload();
        assertThat(actualPayload.getString("note"), equalTo(caseNoteAdded.getNote()));
        assertThat(actualPayload.getString("caseId"), equalTo(caseNoteAdded.getCaseId().toString()));
        assertThat(actualPayload.getString("firstName"), equalTo(caseNoteAdded.getFirstName()));
        assertThat(actualPayload.getString("lastName"), equalTo(caseNoteAdded.getLastName()));
        assertThat(actualPayload.getString("createdDateTime"), equalTo(caseNoteAddedPayload.getString("createdDateTime")));

    }

}
