package uk.gov.moj.cpp.progression.processor;

import static com.google.common.io.Resources.getResource;
import static java.nio.charset.Charset.defaultCharset;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.justice.services.test.utils.core.reflection.ReflectionUtil.setField;
import static uk.gov.moj.cpp.progression.transformer.SchemaVariableConstants.PROSECUTION_CASES;

import uk.gov.justice.services.common.converter.StringToJsonObjectConverter;
import uk.gov.justice.services.core.requester.Requester;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.test.utils.core.messaging.MetadataBuilderFactory;
import uk.gov.moj.cpp.progression.service.RefDataService;
import uk.gov.moj.cpp.progression.service.RestEasyClientService;

import java.io.IOException;
import java.io.StringReader;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.JsonReader;
import javax.ws.rs.core.Response;

import com.google.common.io.Resources;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class VejCaseworkerProcessorTest {

    private static final String VEJ_HEARING_DETAILS_URL = "https://spnl-apim-int-gw.cpp.nonlive/vej/api/v1/hearing/details";
    private static final String VEJ_HEARING_DELETED_URL = "https://spnl-apim-int-gw.cpp.nonlive/vej/api/v1/hearing/deleted";

    @Mock
    private RestEasyClientService restEasyClientService;

    @Mock
    private Response response;

    @Mock
    private Requester requester;

    @Spy
    private StringToJsonObjectConverter stringToJsonObjectConverter;

    @InjectMocks
    private VejCaseworkerProcessor vejCaseworkerProcessor;

    @Mock
    private RefDataService referenceDataService;

    @Captor
    private ArgumentCaptor<String> envelopeArgumentCaptor;

    private static final String HEARING = "hearing";


    @BeforeEach
    public void setUp() {
        setField(vejCaseworkerProcessor, "vejHearingDetailsUrl", VEJ_HEARING_DETAILS_URL);
        setField(vejCaseworkerProcessor, "vejHearingDeleteUrl", VEJ_HEARING_DELETED_URL);
        setField(vejCaseworkerProcessor, "vejEnabled", "true");
    }


    @Test
    public void shouldProcessVejHearingPopulatedToProbationCaseworker() throws IOException {
        final JsonObject hearing = stringToJsonObjectConverter.convert(Resources.toString(getResource("vep-hearing.json"), defaultCharset()));

        final JsonEnvelope jsonEnvelope = envelopeFrom(
                MetadataBuilderFactory.metadataWithRandomUUID("progression.events.vej-hearing-populated-to-probation-caseworker"),
                hearing);

        when(referenceDataService.getPoliceFlag(anyString(), anyString(), eq(requester))).thenReturn(true);
        when(restEasyClientService.post(eq(VEJ_HEARING_DETAILS_URL), any(), any())).thenReturn(response);

        vejCaseworkerProcessor.processVejHearingPopulatedToProbationCaseworker(jsonEnvelope);

        verify(restEasyClientService).post(eq(VEJ_HEARING_DETAILS_URL), envelopeArgumentCaptor.capture(), any());
        final String argumentCaptor = envelopeArgumentCaptor.getValue();
        final JsonReader jsonReader = Json.createReader(new StringReader(argumentCaptor));
        final JsonObject externalPayload = jsonReader.readObject();
        jsonReader.close();
        final JsonObject hearingObj = (JsonObject) externalPayload.get(HEARING);
        final JsonArray policeProsecutionCases = hearingObj.getJsonArray(PROSECUTION_CASES);
        final JsonArray hearingProsecutionCases = ((JsonObject) hearing.get("hearing")).getJsonArray(PROSECUTION_CASES);
        assertThat(((JsonObject) hearing.get("hearing")).get("id"), is(hearingObj.get("id")));
        assertThat(((JsonObject) hearingProsecutionCases.get(0)).get("id"), is(((JsonObject) policeProsecutionCases.get(0)).get("id")));
    }

    @Test
    public void shouldProcessVejHearingPopulatedToProbationCaseworkerNegative() throws IOException {
        final JsonObject hearing = stringToJsonObjectConverter.convert(Resources.toString(getResource("vep-non-hearing.json"), defaultCharset()));
        final JsonObject payload = Json.createObjectBuilder().add("hearing", hearing).build();

        final JsonEnvelope jsonEnvelope = envelopeFrom(
                MetadataBuilderFactory.metadataWithRandomUUID("progression.events.vej-hearing-populated-to-probation-caseworker"),
                payload);

        when(referenceDataService.getPoliceFlag(anyString(), anyString(), eq(requester))).thenReturn(false);

        vejCaseworkerProcessor.processVejHearingPopulatedToProbationCaseworker(jsonEnvelope);

    }

    @Test
    public void shouldProcessVejDeletedHearingPopulatedToProbationCaseworkerNegative() throws IOException {
        final JsonObject hearing = stringToJsonObjectConverter.convert(Resources.toString(getResource("vep-non-hearing.json"), defaultCharset()));
        final JsonObject payload = Json.createObjectBuilder().add("hearing", hearing).build();

        when(referenceDataService.getPoliceFlag(anyString(), anyString(), eq(requester))).thenReturn(false);

        final JsonEnvelope jsonEnvelope = envelopeFrom(
                MetadataBuilderFactory.metadataWithRandomUUID("progression.events.hearing-populated-to-probation-caseworker"),
                payload);
        vejCaseworkerProcessor.processVejDeletedHearingPopulatedToProbationCaseworker(jsonEnvelope);

        verify(restEasyClientService, never()).post(eq(VEJ_HEARING_DELETED_URL), envelopeArgumentCaptor.capture(), any());
    }

    @Test
    public void shouldProcessVejDeletedHearingPopulatedToProbationCaseworker() throws IOException {
        final JsonObject hearing = stringToJsonObjectConverter.convert(Resources.toString(getResource("vep-hearing.json"), defaultCharset()));

        final JsonEnvelope jsonEnvelope = envelopeFrom(
                MetadataBuilderFactory.metadataWithRandomUUID("progression.events.vej-deleted-hearing-populated-to-probation-caseworker"),
                hearing);

        when(referenceDataService.getPoliceFlag(anyString(), anyString(), eq(requester))).thenReturn(true);
        when(restEasyClientService.post(eq(VEJ_HEARING_DELETED_URL), any(), any())).thenReturn(response);

        vejCaseworkerProcessor.processVejDeletedHearingPopulatedToProbationCaseworker(jsonEnvelope);

        verify(restEasyClientService).post(eq(VEJ_HEARING_DELETED_URL), envelopeArgumentCaptor.capture(), any());
        final String argumentCaptor = envelopeArgumentCaptor.getValue();
        final JsonReader jsonReader = Json.createReader(new StringReader(argumentCaptor));
        final JsonObject externalPayload = jsonReader.readObject();
        jsonReader.close();
        final JsonObject hearingObj = (JsonObject) externalPayload.get(HEARING);
        final JsonArray policeProsecutionCases = hearingObj.getJsonArray(PROSECUTION_CASES);
        final JsonArray hearingProsecutionCases = ((JsonObject) hearing.get("hearing")).getJsonArray(PROSECUTION_CASES);
        assertThat(((JsonObject) hearing.get("hearing")).get("id"), is(hearingObj.get("id")));
        assertThat(((JsonObject) hearingProsecutionCases.get(0)).get("id"), is(((JsonObject) policeProsecutionCases.get(0)).get("id")));
    }

}
