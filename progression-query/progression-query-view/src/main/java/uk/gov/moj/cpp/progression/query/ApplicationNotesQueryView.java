package uk.gov.moj.cpp.progression.query;

import static java.util.UUID.fromString;
import static java.util.stream.Collectors.toList;
import static javax.json.Json.createArrayBuilder;
import static javax.json.Json.createObjectBuilder;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;

import uk.gov.justice.services.common.converter.ZonedDateTimes;
import uk.gov.justice.services.core.annotation.Component;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.prosecutioncase.persistence.entity.ApplicationNoteEntity;
import uk.gov.moj.cpp.prosecutioncase.persistence.repository.ApplicationNoteRepository;

import java.util.List;
import java.util.UUID;

import javax.inject.Inject;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;

@ServiceComponent(Component.QUERY_VIEW)
public class ApplicationNotesQueryView {

    public static final String NOTE = "note";
    public static final String CREATED_DATE_TIME = "createdDateTime";
    public static final String AUTHOR = "author";
    public static final String FIRST_NAME = "firstName";
    public static final String LAST_NAME = "lastName";
    public static final String IS_PINNED = "isPinned";
    public static final String ID = "id";

    @Inject
    private ApplicationNoteRepository applicationNoteRepository;

    private static JsonObject apply(ApplicationNoteEntity applicationNote) {
        final JsonObjectBuilder authorBuilder = createObjectBuilder()
                .add(FIRST_NAME, applicationNote.getFirstName())
                .add(LAST_NAME, applicationNote.getLastName());

        final JsonObjectBuilder applicationNoteJsonBuilder = createObjectBuilder()
                .add(NOTE, applicationNote.getNote())
                .add(CREATED_DATE_TIME, ZonedDateTimes.toString(applicationNote.getCreatedDateTime()))
                .add(IS_PINNED, applicationNote.getPinned())
                .add(ID, applicationNote.getId().toString())
                .add(AUTHOR, authorBuilder.build());

        return applicationNoteJsonBuilder.build();
    }

    @Handles("progression.query.application-notes")
    public JsonEnvelope getApplicationNotes(final JsonEnvelope envelope) {

        final UUID applicationId = fromString(envelope.asJsonObject().getString("applicationId"));

        final List<ApplicationNoteEntity> applicationNotes = applicationNoteRepository.findByApplicationIdOrderByCreatedDateTimeDesc(applicationId);

        final JsonArrayBuilder arrayBuilder = createArrayBuilder();
        final List<JsonObject> applicationNotesJson = applicationNotes.stream().map(ApplicationNotesQueryView::apply).collect(toList());
        applicationNotesJson.forEach(arrayBuilder::add);
        final JsonObjectBuilder jsonObjectBuilder = createObjectBuilder()
                .add("applicationNotes", arrayBuilder.build());

        return envelopeFrom(
                envelope.metadata(),
                jsonObjectBuilder.build());
    }
}
