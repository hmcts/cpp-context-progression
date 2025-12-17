package uk.gov.moj.cpp.application.event.listener;

import static java.lang.Boolean.FALSE;
import static java.lang.Boolean.TRUE;
import static java.time.ZonedDateTime.now;
import static java.util.UUID.randomUUID;
import static org.apache.commons.lang3.RandomStringUtils.randomAlphabetic;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.justice.core.courts.ApplicationNoteAdded.applicationNoteAdded;
import static uk.gov.justice.core.courts.ApplicationNoteEdited.applicationNoteEdited;

import uk.gov.justice.core.courts.ApplicationNoteAdded;
import uk.gov.justice.core.courts.ApplicationNoteEdited;
import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.prosecutioncase.persistence.entity.ApplicationNoteEntity;
import uk.gov.moj.cpp.prosecutioncase.persistence.repository.ApplicationNoteRepository;

import java.util.UUID;

import javax.json.JsonObject;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;


@ExtendWith(MockitoExtension.class)
public class ApplicationNoteEventListenerTest {

    @Mock
    private JsonObjectToObjectConverter jsonObjectToObjectConverter;

    @Mock
    private ApplicationNoteRepository applicationNoteRepository;

    @Mock
    private JsonEnvelope envelope;

    @Captor
    private ArgumentCaptor<ApplicationNoteEntity> applicationNoteArgumentCaptor;

    @Mock
    private JsonObject payload;

    @InjectMocks
    private ApplicationNoteEventListener eventListener;

    private static UUID VALUE_APPLICATION_NOTE_ID;
    private static UUID VALUE_APPLICATION_ID;
    private static String VALUE_NOTE;
    private static String VALUE_FIRST_NAME;
    private static String VALUE_LAST_NAME;

    @BeforeEach
    private void initialize() {
        VALUE_APPLICATION_NOTE_ID = randomUUID();
        VALUE_APPLICATION_ID = randomUUID();
        VALUE_NOTE = randomAlphabetic(10);
        VALUE_FIRST_NAME = randomAlphabetic(5);
        VALUE_LAST_NAME = randomAlphabetic(5);
    }

    @Test
    public void shouldHandleApplicationNoteAddedEvent() {
        //Given
        final ApplicationNoteAdded noteAdded = getNoteAdded(VALUE_APPLICATION_NOTE_ID, VALUE_APPLICATION_ID);
        when(envelope.payloadAsJsonObject()).thenReturn(payload);
        when(jsonObjectToObjectConverter.convert(payload, ApplicationNoteAdded.class)).thenReturn(noteAdded);

        //When
        eventListener.handleApplicationNoteAdded(envelope);

        //Then
        verifyNoteAdded(noteAdded);
    }

    @Test
    public void shouldHandleApplicationNoteEditedEvent() {
        //Given
        final ApplicationNoteEntity entity = new ApplicationNoteEntity(VALUE_APPLICATION_NOTE_ID, VALUE_APPLICATION_ID,
                VALUE_NOTE, FALSE, VALUE_FIRST_NAME, VALUE_LAST_NAME, now());
        final ApplicationNoteEdited noteEdited = getNoteEdited(VALUE_APPLICATION_NOTE_ID, VALUE_APPLICATION_ID, TRUE);
        when(envelope.payloadAsJsonObject()).thenReturn(payload);
        when(jsonObjectToObjectConverter.convert(payload, ApplicationNoteEdited.class)).thenReturn(noteEdited);
        when(applicationNoteRepository.findBy(entity.getId())).thenReturn(entity);

        //When
        eventListener.handleApplicationNoteEdited(envelope);

        //Then
        verifyNoteEdited(noteEdited);
    }

    private ApplicationNoteAdded getNoteAdded(final UUID applicationNoteId, final UUID applicationId) {
        return applicationNoteAdded()
                .withApplicationNoteId(applicationNoteId)
                .withApplicationId(applicationId)
                .withNote(randomAlphabetic(10))
                .withIsPinned(TRUE)
                .withFirstName(randomAlphabetic(5))
                .withLastName(randomAlphabetic(5))
                .withCreatedDateTime(now())
                .build();
    }

    private ApplicationNoteEdited getNoteEdited(final UUID applicationNoteId, final UUID applicationId,
                                                final Boolean isPinned) {
        return applicationNoteEdited()
                .withApplicationNoteId(applicationNoteId)
                .withApplicationId(applicationId)
                .withIsPinned(isPinned)
                .build();
    }

    private void verifyNoteAdded(final ApplicationNoteAdded noteAdded) {
        verify(applicationNoteRepository).save(applicationNoteArgumentCaptor.capture());

        final ApplicationNoteEntity entity = applicationNoteArgumentCaptor.getValue();
        assertThat(entity.getId(), equalTo(noteAdded.getApplicationNoteId()));
        assertThat(entity.getApplicationId(), equalTo(noteAdded.getApplicationId()));
        assertThat(entity.getNote(), equalTo(noteAdded.getNote()));
        assertThat(entity.getCreatedDateTime(), equalTo(noteAdded.getCreatedDateTime()));
        assertThat(entity.getFirstName(), equalTo(noteAdded.getFirstName()));
        assertThat(entity.getLastName(), equalTo(noteAdded.getLastName()));
        assertNotNull(entity.getCreatedDateTime());
    }

    private void verifyNoteEdited(final ApplicationNoteEdited noteEdited) {
        verify(applicationNoteRepository).save(applicationNoteArgumentCaptor.capture());

        final ApplicationNoteEntity entity = applicationNoteArgumentCaptor.getValue();
        assertThat(entity.getApplicationId(), equalTo(noteEdited.getApplicationId()));
        assertThat(entity.getId(), equalTo(noteEdited.getApplicationNoteId()));
        assertThat(entity.getPinned(), equalTo(noteEdited.getIsPinned()));
    }
}
