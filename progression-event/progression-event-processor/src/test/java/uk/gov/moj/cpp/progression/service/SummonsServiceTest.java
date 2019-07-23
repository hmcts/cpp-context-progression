package uk.gov.moj.cpp.progression.service;

import static java.util.UUID.randomUUID;
import static org.apache.activemq.artemis.utils.JsonLoader.createObjectBuilder;
import static org.apache.activemq.artemis.utils.JsonLoader.createReader;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.test.utils.core.enveloper.EnveloperFactory.createEnveloper;
import static uk.gov.justice.services.test.utils.core.reflection.ReflectionUtil.setField;

import uk.gov.justice.core.courts.ConfirmedHearing;
import uk.gov.justice.core.courts.CourtCentre;
import uk.gov.justice.core.courts.HearingDay;
import uk.gov.justice.core.courts.summons.BreachSummonsDocumentContent;
import uk.gov.justice.core.courts.summons.SummonsAddress;
import uk.gov.justice.core.courts.summons.SummonsDefendant;
import uk.gov.justice.core.courts.summons.SummonsDocumentContent;
import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.common.converter.ListToJsonArrayConverter;
import uk.gov.justice.services.common.converter.ObjectToJsonObjectConverter;
import uk.gov.justice.services.common.converter.ZonedDateTimes;
import uk.gov.justice.services.common.converter.jackson.ObjectMapperProducer;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.core.requester.Requester;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.JsonEnvelope;

import java.io.IOException;
import java.io.StringReader;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonReader;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.io.Resources;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class SummonsServiceTest {

    @InjectMocks
    private SummonsService summonsService;

    @Mock
    private Sender sender;

    @Mock
    private Requester requester;

    @Mock
    private ReferenceDataService referenceDataService;

    @Mock
    private ProgressionService progressionService;

    @Mock
    private DocumentGeneratorService documentGeneratorService;

    @Spy
    private final Enveloper enveloper = createEnveloper();

    @Spy
    private ListToJsonArrayConverter listToJsonArrayConverter;

    @Spy
    private final ObjectMapper objectMapper = new ObjectMapperProducer().objectMapper();

    @Spy
    private ObjectToJsonObjectConverter objectToJsonConverter;

    @Spy
    private JsonObjectToObjectConverter jsonToObjectConverter;

    @Captor
    private ArgumentCaptor<JsonEnvelope> envelopeArgumentCaptor;

    @Captor
    private ArgumentCaptor<JsonObject> jsonObjectArgumentCaptor;

    @Captor
    private ArgumentCaptor<String> templateArgumentCaptor;

    @Captor
    private ArgumentCaptor<Sender> senderArgumentCaptor;

    @Captor
    private ArgumentCaptor<UUID> applicationIdArgumentCaptor;

    @Captor
    private ArgumentCaptor<UUID> prosecutionIdArgumentCaptor;

    private static final UUID APPLICATION_ID = UUID.randomUUID();
    private static final UUID COURT_CENTRE_ID = UUID.randomUUID();
    private static final String HEARING_DATE_1 = "2018-06-01T10:00:00.000Z";
    private static final String HEARING_DATE_2 = "2018-06-04T10:00:00.000Z";
    private static final String HEARING_DATE_3 = "2018-07-01T10:00:00.000Z";
    private static final String PROGRESSION_COMMAND_PREPARE_SUMMONS_DATA = "progression.command.prepare-summons-data";

    @Before
    public void initMocks() {
        setField(this.objectToJsonConverter, "mapper", new ObjectMapperProducer().objectMapper());
        setField(this.jsonToObjectConverter, "mapper", new ObjectMapperProducer().objectMapper());
    }

    @Test
    public void shouldGenerateGenericSummons() throws Exception {

        SummonsDocumentContent generatedSummonsPayload = prepareAndRunTest("progression.query.generic-application.json");

        assertThat(generatedSummonsPayload.getSubTemplateName(), is("APPLICATION"));

        SummonsDefendant defendant = generatedSummonsPayload.getDefendant();
        SummonsAddress address = defendant.getAddress();

        assertThat(defendant.getName(), is("Robin Maxwell Stuart"));
        assertThat(address.getLine1(), is("22 Acacia Avenue"));
        assertThat(address.getLine2(), is("Acacia Town"));
        assertThat(address.getLine3(), is("Acacia City"));
        assertThat(address.getLine4(), is("Acacia District"));
        assertThat(address.getLine5(), is("Acacia County"));
        assertThat(address.getPostCode(), is("AC1 4AC"));

    }

    @Test
    public void shouldGenerateBreachSummons() throws Exception {
        SummonsDocumentContent generatedSummonsPayload = prepareAndRunTest("progression.query.breach-application.json");

        assertThat(generatedSummonsPayload.getSubTemplateName(), is("BREACH"));

        SummonsDefendant defendant = generatedSummonsPayload.getDefendant();
        SummonsAddress address = defendant.getAddress();

        assertThat(defendant.getName(), is("Robin Maxwell Stuart"));
        assertThat(address.getLine1(), is("22 Acacia Avenue"));
        assertThat(address.getLine2(), is("Acacia Town"));
        assertThat(address.getLine3(), is("Acacia City"));
        assertThat(address.getLine4(), is("Acacia District"));
        assertThat(address.getLine5(), is("Acacia County"));
        assertThat(address.getPostCode(), is("AC1 4AC"));

        BreachSummonsDocumentContent breachSummonsDocumentContent = generatedSummonsPayload.getBreachContent();
        assertThat(breachSummonsDocumentContent.getBreachedOrder(), is("Community Order"));
        assertThat(breachSummonsDocumentContent.getBreachedOrderDate(), is("2019-01-12"));
        assertThat(breachSummonsDocumentContent.getOrderingCourt(), is("Liverpool Crown Court"));
    }

    @Test (expected = java.lang.IllegalArgumentException.class)
    public void shouldNotGenerateGenericApplicationSummons() throws Exception {
        SummonsDocumentContent generatedSummonsPayload = prepareAndRunTest("unsupported-summons-template-type.json");
    }

    private SummonsDocumentContent prepareAndRunTest(String resource) throws Exception {
        final ConfirmedHearing confirmedHearing = generateConfirmedHearing();
        final JsonEnvelope prepareSummonsEnvelope = getEnvelope(PROGRESSION_COMMAND_PREPARE_SUMMONS_DATA);
        final JsonObject courtCentreJson = createCourtCentre();

        when(referenceDataService.getOrganisationUnitById(confirmedHearing.getCourtCentre().getId(), prepareSummonsEnvelope))
                .thenReturn(Optional.of(courtCentreJson));
        when(progressionService.getCourtApplicationById(prepareSummonsEnvelope, confirmedHearing.getCourtApplicationIds().get(0).toString()))
                .thenReturn(Optional.of(createCourtApplication(resource)));
        when(referenceDataService.getEnforcementAreaByLjaCode(prepareSummonsEnvelope, courtCentreJson.getString("lja"))).thenReturn(createLocalJusticeArea());

        summonsService.generateSummonsPayload(prepareSummonsEnvelope, confirmedHearing);

        verify(documentGeneratorService).generateDocument(envelopeArgumentCaptor.capture(), jsonObjectArgumentCaptor.capture(), templateArgumentCaptor.capture(),
                senderArgumentCaptor.capture(), prosecutionIdArgumentCaptor.capture(), applicationIdArgumentCaptor.capture());
        final JsonObject generatedSummonsJsonPayload = jsonObjectArgumentCaptor.getValue();
        return jsonToObjectConverter.convert(generatedSummonsJsonPayload, SummonsDocumentContent.class);
    }

    private JsonObject createLocalJusticeArea() {
        return createObjectBuilder()
                .add("nationalCourtCode", "1234")
                .add("name", "Mag")
                .build();
    }

    private JsonObject createCourtCentre() throws IOException {
        String jsonString = Resources.toString(Resources.getResource("referencedata.query.organisationunits.json"), Charset.defaultCharset())
                .replace("COURT_CENTRE_ID", COURT_CENTRE_ID.toString())
                .replace("COURT_CENTRE_NAME", "Liverpool Crown Court");

        return returnAsJson(jsonString);
    }

    private JsonObject createCourtApplication(String name) throws IOException {
        String jsonString = Resources.toString(Resources.getResource(name), Charset.defaultCharset());
        return returnAsJson(jsonString);
    }

    private JsonObject returnAsJson(final String jsonString) {
        try (JsonReader jsonReader = createReader(new StringReader(jsonString))) {
            return jsonReader.readObject();
        }
    }

    private ConfirmedHearing generateConfirmedHearing() {
        return ConfirmedHearing.confirmedHearing()
                .withId(randomUUID())
                .withCourtCentre(generateCourtCentre())
                .withHearingDays(generateHearingDays())
                .withCourtApplicationIds(generateCourtApplicationId(APPLICATION_ID))
                .build();
    }

    private List<UUID> generateCourtApplicationId(UUID applicationId) {
        return Arrays.asList(applicationId);
    }

    private List<HearingDay> generateHearingDays() {
        return Arrays.asList(
                HearingDay.hearingDay()
                        .withSittingDay(ZonedDateTimes.fromString(HEARING_DATE_1))
                        .build(),
                HearingDay.hearingDay()
                        .withSittingDay(ZonedDateTimes.fromString(HEARING_DATE_2))
                        .build(),
                HearingDay.hearingDay()
                        .withSittingDay(ZonedDateTimes.fromString(HEARING_DATE_3))
                        .build()
        );
    }

    private JsonEnvelope getEnvelope(final String name) {
        return JsonEnvelope.envelopeFrom(
                JsonEnvelope.metadataBuilder().withId(randomUUID()).withName(name).build(),
                Json.createObjectBuilder().build());
    }

    private CourtCentre generateCourtCentre() {
        return CourtCentre.courtCentre()
                .withId(UUID.randomUUID())
                .withName("Liverpool Crown Court")
                .withRoomId(UUID.randomUUID())
                .withRoomName("Legal room 1")
                .withWelshName("Liverpool Crown Court")
                .withWelshRoomName("Legal room 1")
                .build();
    }
}
