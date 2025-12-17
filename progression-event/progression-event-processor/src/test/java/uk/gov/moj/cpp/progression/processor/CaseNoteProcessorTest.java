package uk.gov.moj.cpp.progression.processor;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.verify;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeMetadataMatcher.withMetadataEnvelopedFrom;
import static uk.gov.justice.services.test.utils.core.reflection.ReflectionUtil.setField;

import uk.gov.justice.core.courts.CaseNoteAdded;
import uk.gov.justice.core.courts.CaseNoteAddedV2;
import uk.gov.justice.core.courts.CaseNoteEdited;
import uk.gov.justice.core.courts.CaseNoteEditedV2;
import uk.gov.justice.services.common.converter.ObjectToJsonObjectConverter;
import uk.gov.justice.services.common.converter.jackson.ObjectMapperProducer;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.test.utils.core.messaging.MetadataBuilderFactory;

import java.time.ZonedDateTime;
import java.util.UUID;

import javax.json.JsonObject;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class CaseNoteProcessorTest {

    @Mock
    private Sender sender;

    @Captor
    private ArgumentCaptor<Envelope<JsonObject>> envelopeCaptor;

    @InjectMocks
    private CaseNoteProcessor processor;

    private final ObjectMapper objectMapper = new ObjectMapperProducer().objectMapper();

    private ObjectToJsonObjectConverter objectToJsonObjectConverter = new ObjectToJsonObjectConverter(objectMapper);

    @BeforeEach
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
    }

    @Test
    public void processCaseNotesAddedV2() {
        final CaseNoteAddedV2 caseNoteAdded = CaseNoteAddedV2.caseNoteAddedV2()
                .withCaseNoteId(UUID.randomUUID())
                .withCaseId(UUID.randomUUID())
                .withNote("Note")
                .withFirstName("FirstName")
                .withLastName("LastName")
                .withCreatedDateTime(ZonedDateTime.now())
                .build();

        final JsonObject caseNoteAddedPayload = objectToJsonObjectConverter.convert(caseNoteAdded);

        final JsonEnvelope requestMessage = envelopeFrom(
                MetadataBuilderFactory.metadataWithRandomUUID("progression.event.case-note-added-v2"),
                caseNoteAddedPayload);

        processor.processCaseNoteAddedV2(requestMessage);

        verify(sender).send(envelopeCaptor.capture());

        final Envelope<JsonObject> publicEvent = envelopeCaptor.getValue();
        assertThat(publicEvent.metadata(),
                withMetadataEnvelopedFrom(requestMessage).withName("public.progression.case-note-added"));
        JsonObject actualPayload = publicEvent.payload();
    }

    @Test
    public void processCaseNotesEdited() {
        final CaseNoteEdited caseNoteEdited = CaseNoteEdited.caseNoteEdited()
                .withCaseId(UUID.randomUUID())
                .withCaseNoteId(UUID.randomUUID())
                .withIsPinned(true)
                .build();

        final JsonObject caseNoteEditedPayload = objectToJsonObjectConverter.convert(caseNoteEdited);

        final JsonEnvelope requestMessage = envelopeFrom(
                MetadataBuilderFactory.metadataWithRandomUUID("progression.event.case-note-edited"),
                caseNoteEditedPayload);

        processor.processCaseNoteEdited(requestMessage);

        verify(sender).send(envelopeCaptor.capture());

        final Envelope<JsonObject> publicEvent = envelopeCaptor.getValue();
        assertThat(publicEvent.metadata(),
                withMetadataEnvelopedFrom(requestMessage).withName("public.progression.case-note-edited"));
    }

    @Test
    public void processCaseNotesEditedV2() {
        final CaseNoteEditedV2 caseNoteEdited = CaseNoteEditedV2.caseNoteEditedV2()
                .withCaseId(UUID.randomUUID())
                .withCaseNoteId(UUID.randomUUID())
                .withIsPinned(true)
                .build();

        final JsonObject caseNoteEditedPayload = objectToJsonObjectConverter.convert(caseNoteEdited);

        final JsonEnvelope requestMessage = envelopeFrom(
                MetadataBuilderFactory.metadataWithRandomUUID("progression.event.case-note-edited-v2"),
                caseNoteEditedPayload);

        processor.processCaseNoteEditedV2(requestMessage);

        verify(sender).send(envelopeCaptor.capture());

        final Envelope<JsonObject> publicEvent = envelopeCaptor.getValue();
        assertThat(publicEvent.metadata(),
                withMetadataEnvelopedFrom(requestMessage).withName("public.progression.case-note-edited"));
    }
}
