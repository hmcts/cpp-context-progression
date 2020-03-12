package uk.gov.moj.cpp.progression.query;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static java.util.UUID.randomUUID;
import static org.hamcrest.Matchers.allOf;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.test.utils.common.reflection.ReflectionUtils.setField;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeMatcher.jsonEnvelope;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeMetadataMatcher.metadata;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopePayloadMatcher.payload;

import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.common.converter.StringToJsonObjectConverter;
import uk.gov.justice.services.common.converter.ZonedDateTimes;
import uk.gov.justice.services.common.converter.jackson.ObjectMapperProducer;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.prosecutioncase.persistence.entity.CaseNoteEntity;
import uk.gov.moj.cpp.prosecutioncase.persistence.repository.CaseNoteRepository;

import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.UUID;

import javax.json.Json;
import javax.json.JsonObject;

import org.hamcrest.Matchers;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class CaseNotesQueryViewTest {

    @Mock
    private CaseNoteRepository caseNoteRepository;

    @Mock
    private StringToJsonObjectConverter stringToJsonObjectConverter ;

    @Mock
    private JsonObjectToObjectConverter jsonObjectToObjectConverter;



    @InjectMocks
    private CaseNotesQueryView caseNotesQueryView;


    @Before
    public void setup() {
        setField(this.jsonObjectToObjectConverter, "objectMapper", new ObjectMapperProducer().objectMapper());
    }

    @Test
    public void shouldfindByCaseIdOrderByCreatedDateTimeDesc() throws Exception {
        ZonedDateTime createdDateTime = ZonedDateTime.now();
        //Given
        final JsonEnvelope jsonEnvelope = createJsonEnvelope(createdDateTime);

        //When
        final JsonEnvelope envelope = caseNotesQueryView.getCaseNotes(jsonEnvelope);

        //Then
        Assert.assertThat(envelope, jsonEnvelope(metadata().withName("progression.query.case-notes"),
                payload().isJson(allOf(
                        withJsonPath("$.caseNotes[0].author.firstName", Matchers.equalTo("Bob")),
                        withJsonPath("$.caseNotes[0].author.lastName", Matchers.equalTo("Marley")),
                        withJsonPath("$.caseNotes[0].note", Matchers.equalTo("Test Note")),
                        withJsonPath("$.caseNotes[0].createdDateTime", Matchers.equalTo(ZonedDateTimes.toString(createdDateTime)))
                ))
        ));

    }

    private JsonEnvelope createJsonEnvelope(final ZonedDateTime createdDateTime) {
        final UUID caseId = randomUUID();

        final JsonObject jsonObject = Json.createObjectBuilder()
                .add("caseId", caseId.toString())
                .build();

        final JsonEnvelope jsonEnvelope = JsonEnvelope.envelopeFrom(
                JsonEnvelope.metadataBuilder().withId(randomUUID()).withName("progression.query.case-notes").build(),
                jsonObject);

        CaseNoteEntity caseNoteEntity = new CaseNoteEntity();
        caseNoteEntity.setCaseId(caseId);
        caseNoteEntity.setFirstName("Bob");
        caseNoteEntity.setLastName("Marley");
        caseNoteEntity.setNote("Test Note");
        caseNoteEntity.setCreatedDateTime(createdDateTime);
        when(caseNoteRepository.findByCaseIdOrderByCreatedDateTimeDesc(caseId)).thenReturn(Arrays.asList(caseNoteEntity));
        return jsonEnvelope;
    }


}
