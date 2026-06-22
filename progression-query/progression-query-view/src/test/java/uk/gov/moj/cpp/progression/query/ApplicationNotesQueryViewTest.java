package uk.gov.moj.cpp.progression.query;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static java.lang.Boolean.FALSE;
import static java.lang.Boolean.TRUE;
import static java.util.Arrays.asList;
import static java.util.UUID.randomUUID;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeMatcher.jsonEnvelope;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeMetadataMatcher.metadata;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopePayloadMatcher.payload;
import static uk.gov.justice.services.test.utils.core.reflection.ReflectionUtil.setField;
import static uk.gov.moj.cpp.progression.query.ApplicationNotesQueryView.AUTHOR;
import static uk.gov.moj.cpp.progression.query.ApplicationNotesQueryView.CREATED_DATE_TIME;
import static uk.gov.moj.cpp.progression.query.ApplicationNotesQueryView.FIRST_NAME;
import static uk.gov.moj.cpp.progression.query.ApplicationNotesQueryView.ID;
import static uk.gov.moj.cpp.progression.query.ApplicationNotesQueryView.IS_PINNED;
import static uk.gov.moj.cpp.progression.query.ApplicationNotesQueryView.LAST_NAME;
import static uk.gov.moj.cpp.progression.query.ApplicationNotesQueryView.NOTE;

import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.common.converter.ZonedDateTimes;
import uk.gov.justice.services.common.converter.jackson.ObjectMapperProducer;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.prosecutioncase.persistence.entity.ApplicationNoteEntity;
import uk.gov.moj.cpp.prosecutioncase.persistence.repository.ApplicationNoteRepository;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.UUID;

import uk.gov.justice.services.messaging.JsonObjects;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class ApplicationNotesQueryViewTest {

    @Mock
    private ApplicationNoteRepository applicationNoteRepository;
    @Mock
    private JsonObjectToObjectConverter jsonObjectToObjectConverter;
    @InjectMocks
    private ApplicationNotesQueryView applicationNotesQueryView;

    private static final String PROGRESSION_QUERY_APPLICATION_NOTES = "progression.query.application-notes";
    private static final String APPLICATION_ID = "applicationId";
    private static final String APPLICATION_NOTES = "applicationNotes";

    private static final UUID VALUE_APPLICATION_ID = randomUUID();
    private static final UUID VALUE_APPLICATION_NOTE_ID_1 = randomUUID();
    private static final UUID VALUE_APPLICATION_NOTE_ID_2 = randomUUID();
    private static final String VALUE_NOTE_1 = "First sample note";
    private static final String VALUE_NOTE_2 = "Second sample note";
    private static final String VALUE_FIRST_NAME_1 = "John";
    private static final String VALUE_LAST_NAME_1 = "Doe";
    private static final String VALUE_FIRST_NAME_2 = "Edgar";
    private static final String VALUE_LAST_NAME_2 = "Poe";
    private static final ZonedDateTime VALUE_CREATED_DATE_TIME_1 = ZonedDateTime.now();
    private static final ZonedDateTime VALUE_CREATED_DATE_TIME_2 = ZonedDateTime.now().minusDays(1);

    @BeforeEach
    public void setup() {
        setField(this.jsonObjectToObjectConverter, "objectMapper", new ObjectMapperProducer().objectMapper());
    }

    @Test
    public void shouldFindByApplicationIdOrderByCreatedDateTimeDesc() {
        //Given
        when(applicationNoteRepository.findByApplicationIdOrderByCreatedDateTimeDesc(VALUE_APPLICATION_ID))
                .thenReturn(getApplicationNoteEntities());

        //When
        final JsonEnvelope envelope = applicationNotesQueryView.getApplicationNotes(
                JsonEnvelope.envelopeFrom(JsonEnvelope.metadataBuilder().withId(randomUUID())
                                .withName(PROGRESSION_QUERY_APPLICATION_NOTES).build(),
                        JsonObjects.createObjectBuilder().add(APPLICATION_ID, VALUE_APPLICATION_ID.toString()).build()));

        //Then
        verifyResponseList(envelope);
    }

    private List<ApplicationNoteEntity> getApplicationNoteEntities() {
        return asList(getApplicationNoteEntity(VALUE_APPLICATION_NOTE_ID_1, VALUE_APPLICATION_ID,
                        VALUE_NOTE_1, TRUE, VALUE_FIRST_NAME_1, VALUE_LAST_NAME_1, VALUE_CREATED_DATE_TIME_1),
                getApplicationNoteEntity(VALUE_APPLICATION_NOTE_ID_2, VALUE_APPLICATION_ID,
                        VALUE_NOTE_2, FALSE, VALUE_FIRST_NAME_2, VALUE_LAST_NAME_2, VALUE_CREATED_DATE_TIME_2));
    }

    private static ApplicationNoteEntity getApplicationNoteEntity(final UUID applicationNoteId, final UUID applicationId,
                                                                  final String note, final Boolean isPinned,
                                                                  final String firstName, final String lastName,
                                                                  final ZonedDateTime createdDateTime) {
        ApplicationNoteEntity noteEntity = new ApplicationNoteEntity();
        noteEntity.setId(applicationNoteId);
        noteEntity.setApplicationId(applicationId);
        noteEntity.setNote(note);
        noteEntity.setPinned(isPinned);
        noteEntity.setFirstName(firstName);
        noteEntity.setLastName(lastName);
        noteEntity.setCreatedDateTime(createdDateTime);
        return noteEntity;
    }

    private void verifyResponseList(JsonEnvelope envelope) {
        final String APPLICATION_NOTES_0 = "$.".concat(APPLICATION_NOTES).concat("[0].");
        final String APPLICATION_NOTES_1 = "$.".concat(APPLICATION_NOTES).concat("[1].");

        assertThat(envelope, jsonEnvelope(metadata().withName(PROGRESSION_QUERY_APPLICATION_NOTES),
                payload().isJson(allOf(
                        withJsonPath(APPLICATION_NOTES_0.concat(ID), Matchers.equalTo(VALUE_APPLICATION_NOTE_ID_1.toString())),
                        withJsonPath(APPLICATION_NOTES_0.concat(NOTE), Matchers.equalTo(VALUE_NOTE_1)),
                        withJsonPath(APPLICATION_NOTES_0.concat(IS_PINNED), Matchers.equalTo(TRUE)),
                        withJsonPath(APPLICATION_NOTES_0.concat(AUTHOR).concat(".").concat(FIRST_NAME), Matchers.equalTo(VALUE_FIRST_NAME_1)),
                        withJsonPath(APPLICATION_NOTES_0.concat(AUTHOR).concat(".").concat(LAST_NAME), Matchers.equalTo(VALUE_LAST_NAME_1)),
                        withJsonPath(APPLICATION_NOTES_0.concat(CREATED_DATE_TIME), Matchers.equalTo(ZonedDateTimes.toString(VALUE_CREATED_DATE_TIME_1))),

                        withJsonPath(APPLICATION_NOTES_1.concat(ID), Matchers.equalTo(VALUE_APPLICATION_NOTE_ID_2.toString())),
                        withJsonPath(APPLICATION_NOTES_1.concat(NOTE), Matchers.equalTo(VALUE_NOTE_2)),
                        withJsonPath(APPLICATION_NOTES_1.concat(IS_PINNED), Matchers.equalTo(FALSE)),
                        withJsonPath(APPLICATION_NOTES_1.concat(AUTHOR).concat(".").concat(FIRST_NAME), Matchers.equalTo(VALUE_FIRST_NAME_2)),
                        withJsonPath(APPLICATION_NOTES_1.concat(AUTHOR).concat(".").concat(LAST_NAME), Matchers.equalTo(VALUE_LAST_NAME_2)),
                        withJsonPath(APPLICATION_NOTES_1.concat(CREATED_DATE_TIME), Matchers.equalTo(ZonedDateTimes.toString(VALUE_CREATED_DATE_TIME_2)))
                ))));
    }
}
