package uk.gov.moj.cpp.progression.query;

import static java.util.UUID.fromString;
import static java.util.stream.Collectors.toList;
import static uk.gov.justice.services.messaging.JsonObjects.createArrayBuilder;
import static uk.gov.justice.services.messaging.JsonObjects.createObjectBuilder;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;

import uk.gov.justice.services.common.converter.ZonedDateTimes;
import uk.gov.justice.services.core.annotation.Component;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.prosecutioncase.persistence.entity.CaseNoteEntity;
import uk.gov.moj.cpp.prosecutioncase.persistence.repository.CaseNoteRepository;

import java.util.List;
import java.util.UUID;

import javax.inject.Inject;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;

@ServiceComponent(Component.QUERY_VIEW)
public class CaseNotesQueryView {

    private static final String NOTE = "note";
    private static final String CREATED_DATE_TIME = "createdDateTime";
    private static final String AUTHOR = "author";
    private static final String FIRST_NAME = "firstName";
    private static final String LAST_NAME = "lastName";
    private static final String IS_PINNED = "isPinned";
    private static final String ID = "id";

    @Inject
    private CaseNoteRepository caseNoteRepository;

    private static JsonObject apply(CaseNoteEntity caseNote) {
        final JsonObjectBuilder authorBuilder = createObjectBuilder()
                .add(FIRST_NAME, caseNote.getFirstName())
                .add(LAST_NAME, caseNote.getLastName());

        final JsonObjectBuilder caseNoteJsonBuilder = createObjectBuilder()
                .add(NOTE, caseNote.getNote())
                .add(CREATED_DATE_TIME, ZonedDateTimes.toString(caseNote.getCreatedDateTime()))
                .add(IS_PINNED, caseNote.getPinned())
                .add(ID, caseNote.getId().toString())
                .add(AUTHOR, authorBuilder.build());

        return caseNoteJsonBuilder.build();
    }

    @Handles("progression.query.case-notes")
    public JsonEnvelope getCaseNotes(final JsonEnvelope envelope) {

        final UUID caseId = fromString(envelope.asJsonObject().getString("caseId"));

        final List<CaseNoteEntity> caseNotes = caseNoteRepository.findByCaseIdOrderByCreatedDateTimeDesc(caseId);

        final JsonArrayBuilder arrayBuilder = createArrayBuilder();
        final List<JsonObject> caseNotesJson = caseNotes.stream().map(CaseNotesQueryView::apply).collect(toList());
        caseNotesJson.forEach(arrayBuilder::add);
        final JsonObjectBuilder jsonObjectBuilder = createObjectBuilder()
                .add("caseNotes", arrayBuilder.build());

        return envelopeFrom(
                envelope.metadata(),
                jsonObjectBuilder.build());
    }
}
