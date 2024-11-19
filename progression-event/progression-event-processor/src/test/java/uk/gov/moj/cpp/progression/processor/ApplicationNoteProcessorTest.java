package uk.gov.moj.cpp.progression.processor;

import static java.lang.Boolean.FALSE;
import static java.lang.Boolean.TRUE;
import static java.time.ZonedDateTime.now;
import static java.util.UUID.randomUUID;
import static org.apache.commons.lang3.RandomStringUtils.randomAlphabetic;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.verify;
import static uk.gov.justice.core.courts.ApplicationNoteAdded.applicationNoteAdded;
import static uk.gov.justice.core.courts.ApplicationNoteEdited.applicationNoteEdited;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeMetadataMatcher.withMetadataEnvelopedFrom;
import static uk.gov.justice.services.test.utils.core.messaging.MetadataBuilderFactory.metadataWithRandomUUID;
import static uk.gov.justice.services.test.utils.core.reflection.ReflectionUtil.setField;

import uk.gov.justice.core.courts.ApplicationNoteAdded;
import uk.gov.justice.core.courts.ApplicationNoteEdited;
import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.common.converter.ObjectToJsonObjectConverter;
import uk.gov.justice.services.common.converter.jackson.ObjectMapperProducer;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.JsonEnvelope;

import java.util.UUID;

import javax.json.JsonObject;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class ApplicationNoteProcessorTest {

    @Mock
    private Sender sender;

    @Captor
    private ArgumentCaptor<Envelope<JsonObject>> envelopeCaptor;

    @InjectMocks
    private ApplicationNoteProcessor processor;

    private final ObjectMapper objectMapper = new ObjectMapperProducer().objectMapper();

    private ObjectToJsonObjectConverter objectToJsonObjectConverter = new ObjectToJsonObjectConverter(objectMapper);
    private JsonObjectToObjectConverter jsonObjectToObjectConverter = new JsonObjectToObjectConverter(objectMapper);

    private static final String PROGRESSION_EVENT_APPLICATION_NOTE_ADDED = "progression.event.application-note-added";
    private static final String PROGRESSION_EVENT_APPLICATION_NOTE_EDITED = "progression.event.application-note-edited";
    private static final String PUBLIC_PROGRESSION_APPLICATION_NOTE_ADDED = "public.progression.application-note-added";
    private static final String PUBLIC_PROGRESSION_APPLICATION_NOTE_EDITED = "public.progression.application-note-edited";
    private static UUID VALUE_APPLICATION_NOTE_ID;
    private static UUID VALUE_APPLICATION_ID;
    private static String VALUE_NOTE;
    private static String VALUE_FIRST_NAME;
    private static String VALUE_LAST_NAME;

    @BeforeEach
    public void initialize() {
        VALUE_APPLICATION_NOTE_ID = randomUUID();
        VALUE_APPLICATION_ID = randomUUID();
        VALUE_NOTE = randomAlphabetic(10);
        VALUE_FIRST_NAME = randomAlphabetic(5);
        VALUE_LAST_NAME = randomAlphabetic(5);
        setField(this.objectToJsonObjectConverter, "mapper", new ObjectMapperProducer().objectMapper());
        setField(this.jsonObjectToObjectConverter, "objectMapper", new ObjectMapperProducer().objectMapper());
    }

    @Test
    public void processApplicationNotesAdded() {
        //Given
        final JsonEnvelope requestMessage = envelopeFrom(metadataWithRandomUUID(PROGRESSION_EVENT_APPLICATION_NOTE_ADDED),
                objectToJsonObjectConverter.convert(buildNoteAdded(VALUE_APPLICATION_NOTE_ID, VALUE_APPLICATION_ID)));

        //When
        processor.processApplicationNoteAdded(requestMessage);

        //Then
        verifyNoteAdded(requestMessage);
    }

    @Test
    public void processApplicationNotesEdited() {
        //Given
        final JsonEnvelope requestMessage = envelopeFrom(metadataWithRandomUUID(PROGRESSION_EVENT_APPLICATION_NOTE_EDITED),
                objectToJsonObjectConverter.convert(buildNoteEdited(VALUE_APPLICATION_NOTE_ID, VALUE_APPLICATION_ID, TRUE)));

        //When
        processor.processApplicationNoteEdited(requestMessage);

        //Then
        verifyNoteEdited(requestMessage);
    }

    private ApplicationNoteAdded buildNoteAdded(final UUID applicationNoteId, final UUID applicationId) {
        return applicationNoteAdded()
                .withApplicationNoteId(applicationNoteId)
                .withApplicationId(applicationId)
                .withNote(VALUE_NOTE)
                .withIsPinned(FALSE)
                .withFirstName(VALUE_FIRST_NAME)
                .withLastName(VALUE_LAST_NAME)
                .withCreatedDateTime(now())
                .build();
    }

    private ApplicationNoteEdited buildNoteEdited(final UUID applicationNoteId, final UUID applicationId,
                                                  final Boolean isPinned) {
        return applicationNoteEdited()
                .withApplicationId(applicationId)
                .withApplicationNoteId(applicationNoteId)
                .withIsPinned(isPinned)
                .build();
    }

    private void verifyNoteAdded(final JsonEnvelope requestMessage) {
        verify(sender).send(envelopeCaptor.capture());
        final Envelope<JsonObject> publicEvent = envelopeCaptor.getValue();

        assertThat(publicEvent.metadata(), withMetadataEnvelopedFrom(requestMessage).withName(PUBLIC_PROGRESSION_APPLICATION_NOTE_ADDED));

        final ApplicationNoteAdded noteAdded = jsonObjectToObjectConverter.convert(publicEvent.payload(), ApplicationNoteAdded.class);
        assertThat(noteAdded.getApplicationNoteId(), equalTo(VALUE_APPLICATION_NOTE_ID));
        assertThat(noteAdded.getApplicationId(), equalTo(VALUE_APPLICATION_ID));
        assertThat(noteAdded.getNote(), equalTo(VALUE_NOTE));
        assertThat(noteAdded.getIsPinned(), equalTo(FALSE));
        assertThat(noteAdded.getFirstName(), equalTo(VALUE_FIRST_NAME));
        assertThat(noteAdded.getLastName(), equalTo(VALUE_LAST_NAME));
        assertNotNull(noteAdded.getCreatedDateTime());
    }

    private void verifyNoteEdited(final JsonEnvelope requestMessage) {
        verify(sender).send(envelopeCaptor.capture());
        final Envelope<JsonObject> publicEvent = envelopeCaptor.getValue();

        assertThat(publicEvent.metadata(), withMetadataEnvelopedFrom(requestMessage).withName(PUBLIC_PROGRESSION_APPLICATION_NOTE_EDITED));

        final ApplicationNoteEdited noteEdited = jsonObjectToObjectConverter.convert(publicEvent.payload(), ApplicationNoteEdited.class);
        assertThat(noteEdited.getApplicationNoteId(), equalTo(VALUE_APPLICATION_NOTE_ID));
        assertThat(noteEdited.getApplicationId(), equalTo(VALUE_APPLICATION_ID));
        assertThat(noteEdited.getIsPinned(), equalTo(TRUE));
    }
}
