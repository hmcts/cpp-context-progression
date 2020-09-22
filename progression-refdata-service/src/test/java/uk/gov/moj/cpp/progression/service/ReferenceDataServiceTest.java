package uk.gov.moj.cpp.progression.service;

import static com.jayway.jsonassert.JsonAssert.with;
import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static java.util.UUID.randomUUID;
import static javax.json.Json.createArrayBuilder;
import static javax.json.Json.createObjectBuilder;
import static org.apache.activemq.artemis.utils.JsonLoader.createReader;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.justice.services.messaging.JsonEnvelope.metadataBuilder;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeMetadataMatcher.metadata;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopePayloadMatcher.payload;
import static uk.gov.justice.services.test.utils.core.random.RandomGenerator.STRING;
import static uk.gov.moj.cpp.progression.service.ReferenceDataService.REFERENCEDATA_GET_ALL_RESULT_DEFINITIONS;
import static uk.gov.moj.cpp.progression.service.ReferenceDataService.REFERENCEDATA_GET_COURTCENTER;
import static uk.gov.moj.cpp.progression.service.ReferenceDataService.REFERENCEDATA_GET_DOCUMENT_ACCESS;
import static uk.gov.moj.cpp.progression.service.ReferenceDataService.REFERENCEDATA_GET_OUCODE;
import static uk.gov.moj.cpp.progression.service.ReferenceDataService.REFERENCEDATA_GET_REFERRAL_REASONS;
import static uk.gov.moj.cpp.progression.service.ReferenceDataService.REFERENCEDATA_QUERY_JUDICIARIES;
import static uk.gov.moj.cpp.progression.service.ReferenceDataService.REFERENCEDATA_QUERY_LOCAL_JUSTICE_AREAS;

import uk.gov.justice.core.courts.CourtCentre;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.core.requester.Requester;
import uk.gov.justice.services.messaging.DefaultJsonObjectEnvelopeConverter;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.messaging.MetadataBuilder;
import uk.gov.justice.services.messaging.spi.DefaultEnvelope;
import uk.gov.justice.services.messaging.spi.DefaultJsonMetadata;
import uk.gov.justice.services.test.utils.core.enveloper.EnveloperFactory;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.StringReader;
import java.nio.charset.Charset;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.JsonReader;
import javax.json.JsonString;
import javax.json.JsonValue;

import com.google.common.io.Resources;
import org.hamcrest.CoreMatchers;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.runners.MockitoJUnitRunner;

@SuppressWarnings("unused")
@RunWith(MockitoJUnitRunner.class)
public class ReferenceDataServiceTest {

    public static final String PS_90010 = "PS90010";
    private static final UUID JUDICIARY_ID_1 = UUID.randomUUID();
    private static final UUID JUDICIARY_ID_2 = UUID.randomUUID();
    private static final String JUDICIARY_TITLE_1 = STRING.next();
    private static final String JUDICIARY_FIRST_NAME_1 = STRING.next();
    private static final String JUDICIARY_LAST_NAME_1 = STRING.next();
    private static final String JUDICIARY_TITLE_2 = STRING.next();
    private static final String JUDICIARY_FIRST_NAME_2 = STRING.next();
    private static final String JUDICIARY_LAST_NAME_2 = STRING.next();
    private static final String NATIONAL_COURT_CODE = "3109";
    private static final String ORGANISATION_UNIT = "referencedata.query.organisation-unit.v2";
    @Spy
    Enveloper enveloper = EnveloperFactory.createEnveloper();
    @Mock
    private Requester requester;
    @InjectMocks
    private ReferenceDataService referenceDataService;

    @Captor
    private ArgumentCaptor<DefaultEnvelope> envelopeArgumentCaptor;

    @Captor
    private ArgumentCaptor<Envelope> standardEnvelopeArgumentCaptor;

    @Captor
    private ArgumentCaptor<Envelope<JsonObject>> welshEnvelopeArgumentCaptor;

    @Mock
    private Function<Object, JsonEnvelope> objectJsonEnvelopeFunction;

    @Mock
    private Function<Object, JsonEnvelope> objectJsonEnvelopeFunctionForCourts;

    @Test
    public void shouldRequestForOffenceByCjsOffenceCode() {
        //given

        final JsonObject payload = Json.createReader(
                new ByteArrayInputStream(getJsonPayload().getBytes()))
                .readObject();

        final JsonEnvelope inputEnvelope = JsonEnvelope.envelopeFrom(DefaultJsonMetadata.metadataBuilder().withId(randomUUID()).withName("referencedata.query.offences"),
                payload);
        //when
        when(requester.request(any())).thenReturn(inputEnvelope);

        final JsonEnvelope envelope = JsonEnvelope.envelopeFrom(DefaultJsonMetadata.metadataBuilder().withId(randomUUID()).withName("referencedata.query.offences"), JsonValue.NULL);

        final Optional<JsonObject> result = referenceDataService.getOffenceByCjsCode(
                envelope, PS_90010, requester);

        //then
        verify(requester).request(envelopeArgumentCaptor.capture());
        final DefaultJsonObjectEnvelopeConverter defaultJsonObjectEnvelopeConverter = new DefaultJsonObjectEnvelopeConverter();


        final DefaultEnvelope capturedEnvelope = envelopeArgumentCaptor.getValue();
        MatcherAssert.assertThat(capturedEnvelope.metadata().name(), Matchers.is("referencedata.query.offences"));

        with(envelopeArgumentCaptor.getValue().payload().toString())
                .assertThat("$.cjsoffencecode", CoreMatchers.is(PS_90010));

        assertThat(result.get().getString("cjsoffencecode"), is(PS_90010));
        assertThat(result.get().getString("title"), is("Public service vehicle - passenger use altered / defaced ticket"));
        assertThat(result.get().getString("legislation"), is("Contrary to regulation 7(1)(a) of the Public Service Vehicles (Conduct of Drivers, Inspectors, Conductors and Passengers) Regulations 1990 and section 25 of the Public Passenger Vehicles Act 1981."));
        verifyNoMoreInteractions(requester);
    }

    @Test
    public void shouldReturnEmptyJsonObjectWhenCjsOffenceCodeIsNotFound() {
        //given

        final JsonEnvelope envelope = JsonEnvelope.envelopeFrom(DefaultJsonMetadata.metadataBuilder().withId(randomUUID()).withName("referencedata.query.offences"),
                JsonValue.NULL);
        final JsonObject payload = Json.createReader(
                new ByteArrayInputStream("{\"offences\":[]}".getBytes()))
                .readObject();

        when(requester.request(any()))
                .thenReturn(JsonEnvelope.envelopeFrom(DefaultJsonMetadata.metadataBuilder().withId(randomUUID()).withName("referencedata.query.offences"), payload));


        //when
        final Optional<JsonObject> result = referenceDataService.getOffenceByCjsCode(envelope, PS_90010, requester);

        //then
        verify(requester).request(envelopeArgumentCaptor.capture());

        final DefaultEnvelope capturedEnvelope = envelopeArgumentCaptor.getValue();

        MatcherAssert.assertThat(capturedEnvelope.metadata().name(), Matchers.is("referencedata.query.offences"));

        with(envelopeArgumentCaptor.getValue().payload().toString())
                .assertThat("$.cjsoffencecode", CoreMatchers.is(PS_90010));
        assertThat(result.isPresent(), is(false));
        verifyNoMoreInteractions(requester);
    }

    @Test
    public void shouldRequestForJudgeById() {
        //given

        final UUID judgeId = randomUUID();
        final JsonObject payload = Json.createReader(
                new ByteArrayInputStream(getJudgePayload(judgeId).getBytes()))
                .readObject();

        when(requester.request(any()))
                .thenReturn(JsonEnvelope.envelopeFrom(DefaultJsonMetadata.metadataBuilder().withId(randomUUID()).withName("referencedata.get.judge"), payload));

        final JsonEnvelope envelope = JsonEnvelope.envelopeFrom(DefaultJsonMetadata.metadataBuilder().withId(randomUUID()).withName("referencedata.get.judge"), JsonValue.NULL);
        final Optional<JsonObject> result = referenceDataService.getJudgeById(judgeId, envelope, requester);

        //then
        verify(requester).request(envelopeArgumentCaptor.capture());


        final DefaultEnvelope capturedEnvelope = envelopeArgumentCaptor.getValue();

        MatcherAssert.assertThat(capturedEnvelope.metadata().name(), Matchers.is("referencedata.get.judge"));

        with(envelopeArgumentCaptor.getValue().payload().toString())
                .assertThat("$.id", CoreMatchers.is(judgeId.toString()));

        assertThat(result.get().getString("id"), is(judgeId.toString()));
        assertThat(result.get().getString("title"), is("HSS"));
        assertThat(result.get().getString("firstName"), is("John"));
        assertThat(result.get().getString("lastName"), is("SMITH"));
        verifyNoMoreInteractions(requester);
    }

    @Test
    public void shouldRequestForCourtCentreById() {
        //given

        final UUID courtCentreId = randomUUID();
        final JsonObject payload = Json.createReader(
                new ByteArrayInputStream(getCourtCentrePayload(courtCentreId).getBytes()))
                .readObject();

        when(requester.request(any()))
                .thenReturn(JsonEnvelope.envelopeFrom(DefaultJsonMetadata.metadataBuilder().withId(randomUUID()).withName("referencedata.get.court-centre"), payload));

        //when

        final JsonEnvelope envelope = JsonEnvelope.envelopeFrom(DefaultJsonMetadata.metadataBuilder().withId(randomUUID()).withName("referencedata.get.court-centre"), JsonValue.NULL);


        final Optional<JsonObject> result = referenceDataService.getCourtCentreById(courtCentreId, envelope, requester);

        //then
        verify(requester).request(envelopeArgumentCaptor.capture());


        final DefaultEnvelope capturedEnvelope = envelopeArgumentCaptor.getValue();

        MatcherAssert.assertThat(capturedEnvelope.metadata().name(), Matchers.is("referencedata.get.court-centre"));

        with(envelopeArgumentCaptor.getValue().payload().toString())
                .assertThat("$.id", CoreMatchers.is(courtCentreId.toString()));

        assertThat(result.get().getString("id"), is(courtCentreId.toString()));
        assertThat(result.get().getString("name"), is("Liverpool Crown Court"));
        verifyNoMoreInteractions(requester);
    }

    @Test
    public void shouldRequestForOrganisationByCourtCentreId() {

        final UUID courtCentreId = randomUUID();
        final JsonObject payload = Json.createReader(
                new ByteArrayInputStream(getOrganisationPayload(courtCentreId).getBytes()))
                .readObject();
        when(requester.requestAsAdmin(any(), any()))
                .thenReturn(Envelope.envelopeFrom(DefaultJsonMetadata.metadataBuilder().withId(randomUUID()).withName(ORGANISATION_UNIT), payload));


        //when
        final JsonEnvelope envelope = JsonEnvelope.envelopeFrom(DefaultJsonMetadata.metadataBuilder().withId(randomUUID()).withName(ORGANISATION_UNIT), JsonValue.NULL);


        final Optional<JsonObject> result = referenceDataService.getOrganisationUnitById(courtCentreId, envelope, requester);

        //then
        verify(requester).requestAsAdmin(envelopeArgumentCaptor.capture(), any());


        final DefaultEnvelope capturedEnvelope = envelopeArgumentCaptor.getValue();

        MatcherAssert.assertThat(capturedEnvelope.metadata().name(), Matchers.is(ORGANISATION_UNIT));

        with(envelopeArgumentCaptor.getValue().payload().toString())
                .assertThat("$.id", CoreMatchers.is(courtCentreId.toString()));

        assertThat(result.get().getString("id"), is(courtCentreId.toString()));
        assertThat(result.get().getString("oucodeL1Name"), is("Magistrates' Courts"));
        assertThat(result.get().getString("oucodeL3WelshName"), is("welshName_Test"));
        assertThat(result.get().getString("address1"), is("176a Lavender Hill"));
        assertThat(result.get().getString("address2"), is("London"));
        assertThat(result.get().getString("address3"), is("address line 3"));
        assertThat(result.get().getString("address4"), is("address line 4"));
        assertThat(result.get().getString("address5"), is("address line 5"));
        assertThat(result.get().getString("postcode"), is("SW11 1JU"));
        verifyNoMoreInteractions(requester);
    }

    @Test
    public void shouldRequestDocumentTypeAccessDataById() {
        //given

        final UUID documentTypeId = randomUUID();
        final JsonObject payload = Json.createReader(
                new ByteArrayInputStream(getDocumentTypeDataById(documentTypeId).getBytes()))
                .readObject();
        when(requester.request(any())).thenReturn(JsonEnvelope.envelopeFrom(DefaultJsonMetadata.metadataBuilder().withId(randomUUID()).withName(REFERENCEDATA_GET_DOCUMENT_ACCESS), payload));


        //when
        final JsonEnvelope envelope = JsonEnvelope.envelopeFrom(DefaultJsonMetadata.metadataBuilder().withId(randomUUID()).withName(REFERENCEDATA_GET_DOCUMENT_ACCESS), JsonValue.NULL);
        final Optional<JsonObject> result = referenceDataService.getDocumentTypeAccessData(documentTypeId, envelope, requester);

        //then
        verify(requester).request(envelopeArgumentCaptor.capture());


        final DefaultEnvelope capturedEnvelope = envelopeArgumentCaptor.getValue();

        MatcherAssert.assertThat(capturedEnvelope.metadata().name(), Matchers.is(REFERENCEDATA_GET_DOCUMENT_ACCESS));

        with(envelopeArgumentCaptor.getValue().payload().toString())
                .assertThat("$.id", CoreMatchers.is(documentTypeId.toString()));

        assertThat(result.get().getString("documentCategory"), is("Defendant level"));
        assertThat(result.get().getString("documentType"), is("Magistrate's Sending sheet"));
        assertThat(((JsonString) result.get().getJsonArray("readUserGroups").stream().findFirst().get()).getString(), is("Legal advisors"));
        verifyNoMoreInteractions(requester);
    }

    @Test
    public void shouldRequestCourtOuCodeByPostcodeAndProsecutingAthority() {
        //given
        final String postCode = "CR11111";
        final String prosecutingAuth = "CPS";


        final JsonObject payload = getPayloadForCourts();

        when(requester.request(any()))
                .thenReturn(JsonEnvelope.envelopeFrom(DefaultJsonMetadata.metadataBuilder().withId(randomUUID()).withName(REFERENCEDATA_GET_OUCODE), payload));
        //when
        final JsonEnvelope envelope = envelopeFrom(
                JsonEnvelope.metadataBuilder().withId(randomUUID()).withName(REFERENCEDATA_GET_OUCODE).build(),
                payload);

        final Optional<JsonObject> result = referenceDataService.getCourtsByPostCodeAndProsecutingAuthority(envelope, postCode, prosecutingAuth, requester);

        //then
        verify(requester).request(envelopeArgumentCaptor.capture());

        final DefaultEnvelope capturedEnvelope = envelopeArgumentCaptor.getValue();

        MatcherAssert.assertThat(capturedEnvelope.metadata().name(), Matchers.is(REFERENCEDATA_GET_OUCODE));

        with(envelopeArgumentCaptor.getValue().payload().toString())
                .assertThat("$.postcode", CoreMatchers.is(postCode));


        assertThat(((JsonObject) result.get().getJsonArray("courts").get(0)).getString("oucodeL3Code"), is("B22KS00"));
        verifyNoMoreInteractions(requester);
    }

    @Test
    public void shouldGetLocalJusticeAreas() {

        final UUID id = randomUUID();
        final JsonEnvelope responseEnvelope = generateLocalJusticeResponseEnvelope(id);

        when(requester.request(any()))
                .thenReturn(responseEnvelope);

        final Optional<JsonObject> result = referenceDataService.getLocalJusticeArea(responseEnvelope, NATIONAL_COURT_CODE, requester);

        verify(requester).request(welshEnvelopeArgumentCaptor.capture());

        assertThat(welshEnvelopeArgumentCaptor.getValue().metadata(), metadata().withName(REFERENCEDATA_QUERY_LOCAL_JUSTICE_AREAS));
        assertThat(welshEnvelopeArgumentCaptor.getValue().payload(), payload().isJson(withJsonPath("$.nationalCourtCode", equalTo(NATIONAL_COURT_CODE))));
    }

    @Test
    public void shouldGetAllResultDefinitions() throws IOException {
        final JsonEnvelope jsonEnvelope = generateResultDefinitionsJson();

        when(requester.request(any())).thenReturn(jsonEnvelope);

        final JsonEnvelope envelope = getEnvelope(REFERENCEDATA_GET_ALL_RESULT_DEFINITIONS);
        final JsonObject responseJson = referenceDataService.getAllResultDefinitions(envelope, LocalDate.now(), requester).payloadAsJsonObject();

        final JsonArray resultDefinitionsArray = responseJson.getJsonArray("resultDefinitions");

        verify(requester).request(envelopeArgumentCaptor.capture());

        assertThat(envelopeArgumentCaptor.getValue().metadata(), metadata().withName(REFERENCEDATA_GET_ALL_RESULT_DEFINITIONS));
        assertThat(envelopeArgumentCaptor.getValue().payload(), Matchers.notNullValue());

        assertThat(resultDefinitionsArray.size(), Matchers.greaterThan(0));
    }

    @Test
    public void shouldRequestCourtByOuCode() {
        //given
        final String oucode = "CR11111";
        final String prosecutingAuth = "CPS";
        final UUID id = randomUUID();

        final JsonObject payload = getPayloadForOrgUnits(id.toString());

        when(requester.request(any())).thenReturn(JsonEnvelope.envelopeFrom(DefaultJsonMetadata.metadataBuilder().withId(randomUUID()).withName(REFERENCEDATA_GET_COURTCENTER), payload));
        //when
        final JsonEnvelope envelope = envelopeFrom(
                JsonEnvelope.metadataBuilder().withId(randomUUID()).withName(REFERENCEDATA_GET_COURTCENTER).build(),
                payload);

        final Optional<JsonObject> result = referenceDataService.getCourtsOrganisationUnitsByOuCode(envelope, oucode, requester);

        //then
        verify(requester).request(envelopeArgumentCaptor.capture());

        final DefaultEnvelope capturedEnvelope = envelopeArgumentCaptor.getValue();

        MatcherAssert.assertThat(capturedEnvelope.metadata().name(), Matchers.is(REFERENCEDATA_GET_COURTCENTER));

        with(envelopeArgumentCaptor.getValue().payload().toString())
                .assertThat("$.oucode", CoreMatchers.is(oucode));

        assertThat(((JsonObject) result.get().getJsonArray("organisationunits").get(0)).getString("id"), is(id.toString()));
        verifyNoMoreInteractions(requester);
    }


    @Test
    public void shouldRequestCourtCenter() {
        //given
        final String postcode = "CR7 1A11";
        final String prosecutingAuth = "CPS";
        final UUID id = randomUUID();

        final JsonObject payloadForCourts = getPayloadForCourts();

        final JsonObject payloadForOrgUnits = getPayloadForOrgUnits(id.toString());
        //when
        final JsonEnvelope envelope = getEnvelope(REFERENCEDATA_GET_OUCODE);

        when(requester.request(any()))
                .thenReturn(getEnvelope(REFERENCEDATA_GET_OUCODE, payloadForCourts), getEnvelope(REFERENCEDATA_GET_OUCODE, payloadForOrgUnits));


        final CourtCentre result = referenceDataService.getCourtCentre(envelope, postcode, prosecutingAuth, requester);

        assertThat(result.getId(), is(id));
    }

    @Test
    public void shouldRequestCourtCenterWithNoPostCode() {
        //given
        final String postcode = "";
        final String prosecutingAuth = "CPS";
        final UUID id = randomUUID();
        final String oucode = "B01LY00";

        final JsonObject payloadForCourts = getPayloadForCourts();
        final JsonObject payloadForOrgUnits = getPayloadForOrgUnits(id.toString());
        final JsonEnvelope envelope = getEnvelope(REFERENCEDATA_GET_COURTCENTER);

        when(requester.request(any()))
                .thenReturn(getEnvelope(REFERENCEDATA_GET_COURTCENTER, payloadForOrgUnits), getEnvelope(REFERENCEDATA_GET_OUCODE, payloadForOrgUnits));

        //when
        final CourtCentre result = referenceDataService.getCourtCentre(oucode, envelope, requester);
        //then
        assertThat(result.getId(), is(id));
    }

    @Test
    public void shouldRequestReferralReasons() {
        final JsonObject payload = getReferralReasonsPayload();

        when(requester.request(any())).thenReturn(getEnvelope(REFERENCEDATA_GET_REFERRAL_REASONS, payload));

        final JsonEnvelope envelope = getEnvelope(REFERENCEDATA_GET_REFERRAL_REASONS);
        final Optional<JsonObject> result = referenceDataService.getReferralReasons(envelope, requester);

        final JsonObject referralReasonsJson = result.get().getJsonArray("referralReasons").getJsonObject(0);
        final JsonObject payloadReferralReasonJson = payload.getJsonArray("referralReasons").getJsonObject(0);
        assertThat(referralReasonsJson.getString("id"), is(payloadReferralReasonJson.getString("id")));
        assertThat(referralReasonsJson.getInt("seqId"), is(payloadReferralReasonJson.getInt("seqId")));
        assertThat(referralReasonsJson.getString("reason"), is(payloadReferralReasonJson.getString("reason")));
        assertThat(referralReasonsJson.getString("welshReason"), is(payloadReferralReasonJson.getString("welshReason")));
        assertThat(referralReasonsJson.getString("hearingCode"), is(payloadReferralReasonJson.getString("hearingCode")));

    }

    @Test
    public void shouldGetJudiciariesByJudiciaryIdList() throws Exception {
        final JsonObject payload = generateJudiciariesJson();
        when(requester.request(any())).thenReturn(getEnvelope(REFERENCEDATA_QUERY_JUDICIARIES, payload));

        final JsonEnvelope envelope = getEnvelope(REFERENCEDATA_QUERY_JUDICIARIES);
        final Optional<JsonObject> result = referenceDataService.getJudiciariesByJudiciaryIdList(Arrays.asList(JUDICIARY_ID_1, JUDICIARY_ID_2), envelope, requester);
        final JsonObject judiciariesJson = result.get();

        final JsonObject judiciaryJson = judiciariesJson.getJsonArray("judiciaries").getJsonObject(0);
        assertThat(judiciaryJson.getString("id"), is(JUDICIARY_ID_1.toString()));
        assertThat(judiciaryJson.getString("titlePrefix"), is(JUDICIARY_TITLE_1));
        assertThat(judiciaryJson.getString("surname"), is(JUDICIARY_LAST_NAME_1));
        assertThat(judiciaryJson.getString("forenames"), is(JUDICIARY_FIRST_NAME_1));

        final JsonObject judiciaryJson2 = judiciariesJson.getJsonArray("judiciaries").getJsonObject(1);
        assertThat(judiciaryJson2.getString("id"), is(JUDICIARY_ID_2.toString()));
        assertThat(judiciaryJson2.getString("titlePrefix"), is(JUDICIARY_TITLE_2));
        assertThat(judiciaryJson2.getString("surname"), is(JUDICIARY_LAST_NAME_2));
        assertThat(judiciaryJson2.getString("forenames"), is(JUDICIARY_FIRST_NAME_2));


    }

    private JsonObject generateJudiciariesJson() throws IOException {
        final String jsonString = Resources.toString(Resources.getResource("referenceData.getJudiciariesByIdList.json"), Charset.defaultCharset())
                .replace("JUDICIARY_ID_1", JUDICIARY_ID_1.toString())
                .replace("JUDICIARY_TITLE_1", JUDICIARY_TITLE_1)
                .replace("JUDICIARY_FIRST_NAME_1", JUDICIARY_FIRST_NAME_1)
                .replace("JUDICIARY_LAST_NAME_1", JUDICIARY_LAST_NAME_1)
                .replace("JUDICIARY_ID_2", JUDICIARY_ID_2.toString())
                .replace("JUDICIARY_TITLE_2", JUDICIARY_TITLE_2)
                .replace("JUDICIARY_FIRST_NAME_2", JUDICIARY_FIRST_NAME_2)
                .replace("JUDICIARY_LAST_NAME_2", JUDICIARY_LAST_NAME_2);

        return returnAsJson(jsonString);
    }

    private JsonObject returnAsJson(final String jsonString) {
        try (final JsonReader jsonReader = createReader(new StringReader(jsonString))) {
            return jsonReader.readObject();
        }
    }

    private JsonEnvelope getEnvelope(final String name) {
        return envelopeFrom(
                JsonEnvelope.metadataBuilder().withId(randomUUID()).withName(name).build(),
                Json.createObjectBuilder().build());
    }

    private JsonEnvelope getEnvelope(final String name, final JsonObject jsonObject) {
        return JsonEnvelope.envelopeFrom(
                JsonEnvelope.metadataBuilder().withId(randomUUID()).withName(name).build(),
                jsonObject);
    }

    private JsonObject getPayloadForOrgUnits(final String id) {
        return Json.createObjectBuilder()
                .add("organisationunits", createArrayBuilder()
                        .add(Json.createObjectBuilder()
                                .add("id", id)
                                .add("oucodeL3Name", "South Western (Lavender Hill)")
                                .add("oucodeL3WelshName", "welshName_Test")
                                .build())
                        .build())
                .build();
    }

    private JsonObject getPayloadForCourts() {
        return Json.createObjectBuilder()
                .add("courts", createArrayBuilder()
                        .add(Json.createObjectBuilder().add("oucode", "Redditch").add("oucodeL3Code", "B22KS00").build())
                        .build())
                .build();
    }

    private JsonObject getReferralReasonsPayload() {
        return Json.createObjectBuilder()
                .add("referralReasons", createArrayBuilder()
                        .add(Json.createObjectBuilder()
                                .add("id", "7e2f843e-d639-40b3-8611-8015f3a18957")
                                .add("seqId", 1)
                                .add("reason", "Sections 135")
                                .add("welshReason", "Ple amhendant")
                                .add("hearingCode", "ANP")
                        ))
                .build();
    }


    private String getDocumentTypeDataById(final UUID documentTypeId) {
        return "{\n" +
                " \"uuid\": \"" + documentTypeId + "\",\n" +
                "\"documentCategory\": \"Defendant level\",\n" +
                "\"documentType\": \"Magistrate's Sending sheet\",\n" +
                "\"readUserGroups\": [\n" +
                "\"Legal advisors\"\n" +
                "]}";
    }

    private String getCourtCentrePayload(final UUID courtCentreId) {
        return "{\n" +
                " \"id\": \"" + courtCentreId + "\",\n" +
                "\"name\": \"Liverpool Crown Court\",\n" +
                "\"courtRooms\": [\n" +
                "{\n" +
                "\"id\": \"47834e9d-0bca-4f26-aa30-270580496e6e\",\n" +
                "\"name\": \"1\"\n" +
                "}]}";
    }

    private String getOrganisationPayload(final UUID courtCentreId) {
        return "{\n" +
                "  \"id\": \"" + courtCentreId + "\",\n" +
                "  \"oucode\": \"B01LY00\",\n" +
                "  \"oucodeL3Code\": \"LY\",\n" +
                "  \"oucodeL3Name\": \"South Western (Lavender Hill)\",\n" +
                "  \"oucodeL2Code\": \"01\",\n" +
                "  \"oucodeL2Name\": \"London\",\n" +
                "  \"oucodeL1Code\": \"B\",\n" +
                "  \"oucodeL1Name\": \"Magistrates' Courts\",\n" +
                "  \"oucodeEffectiveFromDate\": \"2006-01-27\",\n" +
                "  \"oucodeEffectiveToDate\": \"2020-01-27\",\n" +
                "  \"lja\": \"2577\",\n" +
                "  \"phone\": \"020 7805 1447\",\n" +
                "  \"fax\": \"020 7805 1447\",\n" +
                "  \"email\": \"swglondonmc@hmcts.gsi.gov.uk\",\n" +
                "  \"address1\": \"176a Lavender Hill\",\n" +
                "  \"address2\": \"London\",\n" +
                "  \"address3\": \"address line 3\",\n" +
                "  \"address4\": \"address line 4\",\n" +
                "  \"address5\": \"address line 5\",\n" +
                "  \"postcode\": \"SW11 1JU\",\n" +
                "  \"isWelsh\": false,\n" +
                "  \"oucodeL3WelshName\": \"welshName_Test\",\n" +
                "  \"welshAddress1\": \"176a Lavender Hill\",\n" +
                "  \"welshAddress2\": \"London\",\n" +
                "  \"welshAddress3\": \"address line 3\",\n" +
                "  \"welshAddress4\": \"address line 4\",\n" +
                "  \"welshAddress5\": \"address line 5\",\n" +
                "  \"defaultStartTime\": \"10:30\",\n" +
                "  \"defaultDuration\": \"6\"\n" +
                "}";
    }

    private String getJudgePayload(final UUID judgeId) {

        return "{\n" +
                " \"id\": \"" + judgeId + "\",\n" +
                "\"title\": \"HSS\",\n" +
                "\"firstName\": \"John\",\n" +
                "\"lastName\": \"SMITH\"" +
                "}";
    }

    private String getJsonPayload() {
        return "{\n" +
                "  \"offences\": [\n" +
                "    {\n" +
                "      \"id\": \"f8254db1-1683-483e-afb3-b87fde5a0a26\",\n" +
                "      \"cjsoffencecode\": \"" +
                PS_90010 +
                "\",\n" +
                "      \"title\": \"Public service vehicle - passenger use altered / defaced ticket\",\n" +
                "      \"pnldref\": \"H3188\",\n" +
                "      \"offencestartdate\": \"2006-01-27\",\n" +
                "      \"offenceenddate\": \"2017-03-06\",\n" +
                "      \"standardoffencewording\": \"On **(..SPECIFY DATE..) at **(..SPECIFY TOWNSHIP..), being a passenger on a vehicle, namely **(..SPECIFY VEHICLE MAKE AND INDEX NUMBER..), used    for the carriage of passengers at separate fares, used a ticket which had been altered or    defaced  .\",\n" +
                "      \"standardstatementoffacts\": \"At **(..SPECIFY TIME..) on **(..SPECIFY DATE..) the defendant was a passenger on a public service vehicle, namely **(..SPECIFY VEHICLE MAKE AND INDEX NUMBER..), **(..SPECIFY BRIEF DETAILS OF ROUTE..) and used a ticket which had been altered or defaced in that **(..SPECIFY NATURE OF ALTERATION/DEFACEMENT..).\",\n" +
                "      \"welshstandardoffencewording\": \"\",\n" +
                "      \"welshstandardstatementoffacts\": \"\",\n" +
                "      \"policeandcpschargingresponsibilities\": \"D27968\",\n" +
                "      \"timelimitforprosecutions\": \"6 months\",\n" +
                "      \"maxFineLevel\": \"3\",\n" +
                "      \"misCode\": \"SNM\",\n" +
                "      \"maxfinetypemagct\": \"Y\",\n" +
                "      \"maxfinetypecrownct\": \"N\",\n" +
                "      \"legislation\": \"Contrary to regulation 7(1)(a) of the Public Service Vehicles (Conduct of Drivers, Inspectors, Conductors and Passengers) Regulations 1990 and section 25 of the Public Passenger Vehicles Act 1981.\",\n" +
                "      \"welshOffenceTitle\": \"\",\n" +
                "      \"welshLegislation\": \"\",\n" +
                "      \"libraCategoryCode\": \"CM\",\n" +
                "      \"custodialIndicatorCode\": \"N\",\n" +
                "      \"dateOfLastUpdate\": \"2009-08-20\",\n" +
                "      \"modeoftrial\": \"STRAFF\",\n" +
                "      \"modeoftrialdescription\": \"SUMMARY MINOR TRAFFIC\"\n" +
                "    }\n" +
                "  ]\n" +
                "}";
    }

    private JsonEnvelope generateLocalJusticeResponseEnvelope(final UUID userId) {
        final MetadataBuilder metadataBuilder = metadataBuilder()
                .withId(randomUUID())
                .withName(REFERENCEDATA_QUERY_LOCAL_JUSTICE_AREAS)
                .withUserId(userId.toString());
        final JsonObject payload = createObjectBuilder()
                .add("localJusticeAreas",
                        createArrayBuilder().add(createObjectBuilder()
                                .add("nationalCourtCode", NATIONAL_COURT_CODE)
                                .add("name", "Cardiff Magistrates' Court")
                                .add("welshName", "Caerdydd")))
                .build();
        return JsonEnvelope.envelopeFrom(
                metadataBuilder, payload);
    }

    private JsonEnvelope generateResultDefinitionsJson() throws IOException {

        final String jsonString = Resources.toString(Resources.getResource("referencedata.get-all-result-definitions.json"), Charset.defaultCharset());
        final MetadataBuilder metadataBuilder = metadataBuilder()
                .withId(UUID.randomUUID())
                .withName(REFERENCEDATA_GET_ALL_RESULT_DEFINITIONS);

        final JsonObject payload = Json.createReader(
                new ByteArrayInputStream(jsonString.getBytes()))
                .readObject();

        return envelopeFrom(metadataBuilder, payload);

    }
}
