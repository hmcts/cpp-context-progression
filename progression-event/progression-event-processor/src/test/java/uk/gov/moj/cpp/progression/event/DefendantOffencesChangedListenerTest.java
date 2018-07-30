package uk.gov.moj.cpp.progression.event;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static java.lang.String.format;
import static java.util.UUID.randomUUID;
import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static uk.gov.justice.services.messaging.JsonObjectMetadata.metadataWithDefaults;
import static uk.gov.justice.services.test.utils.core.enveloper.EnveloperFactory.createEnveloper;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeMatcher.jsonEnvelope;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeMetadataMatcher.metadata;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopePayloadMatcher.payloadIsJson;

import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.common.converter.jackson.ObjectMapperProducer;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.DefaultJsonEnvelope;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.messaging.Metadata;
import uk.gov.moj.cpp.progression.service.ReferenceDataService;

import java.io.IOException;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import javax.json.Json;
import javax.json.JsonObject;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;
import org.mockito.runners.MockitoJUnitRunner;

@SuppressWarnings("unchecked")
@RunWith(MockitoJUnitRunner.class)
public class DefendantOffencesChangedListenerTest {



    @InjectMocks
    private DefendantOffencesChangedListener listener;

    @Mock
    private Sender sender;

    @Mock
    private ReferenceDataService referenceDataService;

    @Spy
    @InjectMocks
    private final JsonObjectToObjectConverter jsonObjectToObjectConverter = new JsonObjectToObjectConverter();

    @Spy
    private final ObjectMapper objectMapper = new ObjectMapperProducer().objectMapper();

    @Spy
    private final Enveloper enveloper = createEnveloper();

    @Before
    public void initMocks() {
        MockitoAnnotations.initMocks(this);
    }

    @Captor
    private ArgumentCaptor<JsonEnvelope> envelopeArgumentCaptor;
    private static final UUID CASE_ID = randomUUID();
    private static final UUID DEFENDANT_ID = randomUUID();
    private static final UUID UPDATED_OFFENCE_ID = randomUUID();
    private static final UUID OFFENCE_ID = randomUUID();
    private static final UUID ADDED_OFFENCE_ID = randomUUID();
    private static final UUID DELETED_OFFENCE_ID = randomUUID();
    private static final String MODIFIED_DATE = LocalDate.now().toString();
    private static final String START_DATE = LocalDate.now().toString();
    private static final String END_DATE = LocalDate.now().toString();
    private static final String CONVICTION_DATE = LocalDate.now().toString();
    private static final String OFFENCE_CODE = "OFF11";
    private static final String WORDING = "Robbery";
    private static final int COUNT = 1;
    private static final String FIELD_CASE_ID = "caseId";
    private static final String FIELD_DEFENDANT_ID = "defendantId";
    private static final String FIELD_MODIFIED_DATE = "modifiedDate";
    private static final String FIELD_ID = "id";
    private static final String FIELD_OFFENCE_CODE = "offenceCode";
    private static final String FIELD_WORDING = "wording";
    private static final String FIELD_START_DATE = "startDate";
    private static final String FIELD_END_DATE = "endDate";
    private static final String FIELD_COUNT = "count";
    private static final String FIELD_CONVICTION_DATE = "convictionDate";
    private static final String ARRAY_UPDATED_OFFENCES = "updatedOffences";
    private static final String ARRAY_ADDED_OFFENCES = "addedOffences";
    private static final String ARRAY_DELETED_OFFENCES = "deletedOffences";
    private static final String ARRAY_OFFENCES = "offences";
    private static final String FIELD_STATEMENT_OF_OFFENCE = "statementOfOffence";
    private static final String FIELD_TITLE = "title";
    private static final String FIELD_LEGISLATION = "legislation";
    private static final String LEGISLATION = "legislation";
    private static final String TITLE = "Wounding with intent";



    @Test
    public void testPublishCaseDefendantChanged() throws IOException {

        JsonEnvelope eventJsonForDefendantUpdateConfirmed = getEventJsonForDefendantUpdateConfirmed();
        given(referenceDataService.getOffenceByCjsCode(eventJsonForDefendantUpdateConfirmed, OFFENCE_CODE)).willReturn(Optional.of(createOffence()));
        this.listener.publicDefendantOffencesChanged(eventJsonForDefendantUpdateConfirmed);
        verify(this.sender).send(this.envelopeArgumentCaptor.capture());
        assertPublicMessageAllFields();
    }

    private JsonObject createOffence() {
        return Json.createObjectBuilder()
                .add(FIELD_ID, OFFENCE_ID.toString())
                .add(FIELD_TITLE, TITLE)
                .add(FIELD_LEGISLATION, LEGISLATION)
                .build();
    }

    private void assertPublicMessageAllFields() {
        assertThat(this.envelopeArgumentCaptor.getValue(), jsonEnvelope(metadata().withName(
                DefendantOffencesChangedListener.PUBLIC_DEFENDANT_OFFENCES_CHANGED),


                payloadIsJson(allOf(
                        withJsonPath(format("$.%s", FIELD_MODIFIED_DATE),
                                equalTo(MODIFIED_DATE)),

                        withJsonPath(format("$.%s[0].%s", ARRAY_UPDATED_OFFENCES,
                                FIELD_DEFENDANT_ID),
                                equalTo(DEFENDANT_ID.toString())),
                        withJsonPath(format("$.%s[0].%s", ARRAY_UPDATED_OFFENCES,
                                FIELD_CASE_ID),
                                equalTo(CASE_ID.toString())),
                        withJsonPath(format("$.%s[0].%s[0].%s",
                                ARRAY_UPDATED_OFFENCES,  ARRAY_OFFENCES,
                                FIELD_ID),
                                equalTo(UPDATED_OFFENCE_ID.toString())),
                        withJsonPath(format("$.%s[0].%s[0].%s",
                                ARRAY_UPDATED_OFFENCES,  ARRAY_OFFENCES,
                                FIELD_OFFENCE_CODE), equalTo(OFFENCE_CODE)),
                        withJsonPath(format("$.%s[0].%s[0].%s",
                                ARRAY_UPDATED_OFFENCES,  ARRAY_OFFENCES,
                                FIELD_WORDING), equalTo(WORDING)),
                        withJsonPath(format("$.%s[0].%s[0].%s",
                                ARRAY_UPDATED_OFFENCES,  ARRAY_OFFENCES,
                                FIELD_START_DATE), equalTo(START_DATE)),
                        withJsonPath(format("$.%s[0].%s[0].%s",
                                ARRAY_UPDATED_OFFENCES,  ARRAY_OFFENCES,
                                FIELD_END_DATE), equalTo(END_DATE)),
                        withJsonPath(format("$.%s[0].%s[0].%s",
                                ARRAY_UPDATED_OFFENCES,  ARRAY_OFFENCES,
                                FIELD_COUNT), equalTo(COUNT)),
                        withJsonPath(format("$.%s[0].%s[0].%s",
                                ARRAY_UPDATED_OFFENCES,  ARRAY_OFFENCES,
                                FIELD_CONVICTION_DATE),
                                equalTo(CONVICTION_DATE)),
                        withJsonPath(format("$.%s[0].%s[0].%s.%s",
                                ARRAY_UPDATED_OFFENCES, ARRAY_OFFENCES,
                                FIELD_STATEMENT_OF_OFFENCE, FIELD_TITLE),
                                equalTo(TITLE)),
                        withJsonPath(format("$.%s[0].%s[0].%s.%s",
                                ARRAY_UPDATED_OFFENCES, ARRAY_OFFENCES,
                                FIELD_STATEMENT_OF_OFFENCE, FIELD_LEGISLATION),
                                equalTo(LEGISLATION)),

                        withJsonPath(format("$.%s[0].%s", ARRAY_DELETED_OFFENCES,
                                FIELD_DEFENDANT_ID),
                                equalTo(DEFENDANT_ID.toString())),
                        withJsonPath(format("$.%s[0].%s", ARRAY_DELETED_OFFENCES,
                                FIELD_CASE_ID),
                                equalTo(CASE_ID.toString())),
                        withJsonPath(format("$.%s[0].%s[0]",
                                ARRAY_DELETED_OFFENCES,  ARRAY_OFFENCES),
                                equalTo(DELETED_OFFENCE_ID.toString())),

                        withJsonPath(format("$.%s[0].%s", ARRAY_ADDED_OFFENCES,
                                FIELD_DEFENDANT_ID),
                                equalTo(DEFENDANT_ID.toString())),
                        withJsonPath(format("$.%s[0].%s", ARRAY_ADDED_OFFENCES,
                                FIELD_CASE_ID),
                                equalTo(CASE_ID.toString())),

                        withJsonPath(format("$.%s[0].%s[0].%s",
                                ARRAY_ADDED_OFFENCES, ARRAY_OFFENCES,
                                FIELD_ID),
                                equalTo(ADDED_OFFENCE_ID.toString())),
                        withJsonPath(format("$.%s[0].%s[0].%s",
                                ARRAY_ADDED_OFFENCES, ARRAY_OFFENCES,
                                FIELD_OFFENCE_CODE), equalTo(OFFENCE_CODE)),
                        withJsonPath(format("$.%s[0].%s[0].%s",
                                ARRAY_ADDED_OFFENCES, ARRAY_OFFENCES,
                                FIELD_WORDING), equalTo(WORDING)),
                        withJsonPath(format("$.%s[0].%s[0].%s",
                                ARRAY_ADDED_OFFENCES, ARRAY_OFFENCES,
                                FIELD_START_DATE), equalTo(START_DATE)),
                        withJsonPath(format("$.%s[0].%s[0].%s",
                                ARRAY_ADDED_OFFENCES, ARRAY_OFFENCES,
                                FIELD_END_DATE), equalTo(END_DATE)),
                        withJsonPath(format("$.%s[0].%s[0].%s",
                                ARRAY_ADDED_OFFENCES, ARRAY_OFFENCES,
                                FIELD_COUNT), equalTo(COUNT)),
                        withJsonPath(format("$.%s[0].%s[0].%s",
                                ARRAY_ADDED_OFFENCES, ARRAY_OFFENCES,
                                FIELD_CONVICTION_DATE),
                                equalTo(CONVICTION_DATE)),
                        withJsonPath(format("$.%s[0].%s[0].%s.%s",
                                ARRAY_ADDED_OFFENCES, ARRAY_OFFENCES,
                                FIELD_STATEMENT_OF_OFFENCE, FIELD_TITLE),
                                equalTo(TITLE)),
                        withJsonPath(format("$.%s[0].%s[0].%s.%s",
                                ARRAY_ADDED_OFFENCES, ARRAY_OFFENCES,
                                FIELD_STATEMENT_OF_OFFENCE, FIELD_LEGISLATION),
                                equalTo(LEGISLATION))


                ))));
    }

    public JsonEnvelope getEventJsonForDefendantUpdateConfirmed() throws IOException {
        final Metadata metadata = metadataWithDefaults().build();
        return new DefaultJsonEnvelope(metadata, createDefendantUpdateConfirmedEvent());
    }

    private JsonObject createDefendantUpdateConfirmedEvent() {
        return Json.createObjectBuilder().add(FIELD_MODIFIED_DATE, MODIFIED_DATE)
                .add(ARRAY_UPDATED_OFFENCES, Json.createArrayBuilder()
                        .add(Json.createObjectBuilder()
                                .add(FIELD_DEFENDANT_ID, DEFENDANT_ID.toString())
                                .add(FIELD_CASE_ID, CASE_ID.toString())
                                .add(ARRAY_OFFENCES, Json.createArrayBuilder()
                                        .add(Json.createObjectBuilder()
                                                .add(FIELD_ID, UPDATED_OFFENCE_ID.toString())
                                                .add(FIELD_OFFENCE_CODE, OFFENCE_CODE)
                                                .add(FIELD_WORDING, WORDING)
                                                .add(FIELD_START_DATE, START_DATE)
                                                .add(FIELD_END_DATE, END_DATE).add(FIELD_COUNT, COUNT)
                                                .add(FIELD_CONVICTION_DATE, CONVICTION_DATE)))))
                .add(ARRAY_DELETED_OFFENCES, Json.createArrayBuilder()
                        .add(Json.createObjectBuilder()
                                .add(FIELD_DEFENDANT_ID, DEFENDANT_ID.toString())
                                .add(FIELD_CASE_ID, CASE_ID.toString())
                                .add(ARRAY_OFFENCES, Json.createArrayBuilder()
                                        .add(Json.createObjectBuilder()
                                                .add(FIELD_ID, DELETED_OFFENCE_ID.toString())
                                                .add(FIELD_OFFENCE_CODE, OFFENCE_CODE)
                                                .add(FIELD_WORDING, WORDING)
                                                .add(FIELD_START_DATE, START_DATE)
                                                .add(FIELD_END_DATE, END_DATE).add(FIELD_COUNT, COUNT)
                                                .add(FIELD_CONVICTION_DATE, CONVICTION_DATE)))))
                .add(ARRAY_ADDED_OFFENCES, Json.createArrayBuilder()
                        .add(Json.createObjectBuilder()
                                .add(FIELD_DEFENDANT_ID, DEFENDANT_ID.toString())
                                .add(FIELD_CASE_ID, CASE_ID.toString())
                                .add(ARRAY_OFFENCES, Json.createArrayBuilder()
                                        .add(Json.createObjectBuilder()
                                                .add(FIELD_ID, ADDED_OFFENCE_ID.toString())
                                                .add(FIELD_OFFENCE_CODE, OFFENCE_CODE)
                                                .add(FIELD_WORDING, WORDING)
                                                .add(FIELD_START_DATE, START_DATE)
                                                .add(FIELD_END_DATE, END_DATE)
                                                .add(FIELD_COUNT, COUNT)
                                                .add(FIELD_CONVICTION_DATE,
                                                        CONVICTION_DATE)))))

                .build();
    }

}
