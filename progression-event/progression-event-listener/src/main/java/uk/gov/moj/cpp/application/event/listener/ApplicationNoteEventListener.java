package uk.gov.moj.cpp.application.event.listener;

import static uk.gov.justice.services.core.annotation.Component.EVENT_LISTENER;

import uk.gov.justice.core.courts.ApplicationNoteAdded;
import uk.gov.justice.core.courts.ApplicationNoteEdited;
import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.prosecutioncase.persistence.entity.ApplicationNoteEntity;
import uk.gov.moj.cpp.prosecutioncase.persistence.repository.ApplicationNoteRepository;

import javax.inject.Inject;

@ServiceComponent(EVENT_LISTENER)
public class ApplicationNoteEventListener {

    @Inject
    private JsonObjectToObjectConverter jsonObjectConverter;

    @Inject
    private ApplicationNoteRepository applicationNoteRepository;

    @Handles("progression.event.application-note-added")
    public void handleApplicationNoteAdded(final JsonEnvelope event) {
        final ApplicationNoteAdded note = jsonObjectConverter.convert(event.payloadAsJsonObject(), ApplicationNoteAdded.class);
        applicationNoteRepository.save(new ApplicationNoteEntity(note.getApplicationNoteId(), note.getApplicationId(),
                note.getNote(), note.getIsPinned(), note.getFirstName(), note.getLastName(), note.getCreatedDateTime()));
    }

    @Handles("progression.event.application-note-edited")
    public void handleApplicationNoteEdited(final JsonEnvelope event) {
        final ApplicationNoteEdited note = jsonObjectConverter.convert(event.payloadAsJsonObject(), ApplicationNoteEdited.class);
        final ApplicationNoteEntity entity = applicationNoteRepository.findBy(note.getApplicationNoteId());
        entity.setPinned(note.getIsPinned());
        applicationNoteRepository.save(entity);
    }
}
