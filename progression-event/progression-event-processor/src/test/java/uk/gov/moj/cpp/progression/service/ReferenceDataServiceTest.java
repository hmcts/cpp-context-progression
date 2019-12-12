package uk.gov.moj.cpp.progression.service;

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
import static uk.gov.justice.services.messaging.JsonEnvelope.metadataBuilder;
import static uk.gov.justice.services.messaging.JsonObjectMetadata.metadataWithRandomUUID;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeMatcher.jsonEnvelope;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeMetadataMatcher.metadata;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeMetadataMatcher.withMetadataEnvelopedFrom;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopePayloadMatcher.payload;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopePayloadMatcher.payloadIsJson;
import static uk.gov.justice.services.test.utils.core.messaging.JsonEnvelopeBuilder.envelope;
import static uk.gov.justice.services.test.utils.core.random.RandomGenerator.STRING;
import static uk.gov.moj.cpp.progression.service.ReferenceDataService.REFERENCEDATA_GET_COURTCENTER;
import static uk.gov.moj.cpp.progression.service.ReferenceDataService.REFERENCEDATA_GET_DOCUMENT_TYPE;
import static uk.gov.moj.cpp.progression.service.ReferenceDataService.REFERENCEDATA_GET_OUCODE;
import static uk.gov.moj.cpp.progression.service.ReferenceDataService.REFERENCEDATA_GET_REFERRAL_REASONS;
import static uk.gov.moj.cpp.progression.service.ReferenceDataService.REFERENCEDATA_QUERY_JUDICIARIES;
import static uk.gov.moj.cpp.progression.service.ReferenceDataService.REFERENCEDATA_QUERY_LOCAL_JUSTICE_AREAS;

import uk.gov.justice.core.courts.CourtCentre;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.core.requester.Requester;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.messaging.MetadataBuilder;
import uk.gov.justice.services.test.utils.core.enveloper.EnveloperFactory;
import uk.gov.justice.services.test.utils.core.messaging.JsonEnvelopeBuilder;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.StringReader;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonReader;
import javax.json.JsonString;

import com.google.common.io.Resources;
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

    @Mock
    private Requester requester;

    @Spy
    Enveloper enveloper = EnveloperFactory.createEnveloper();
    ;

    @InjectMocks
    private ReferenceDataService referenceDataService;

    @Captor
    private ArgumentCaptor<JsonEnvelope> envelopeArgumentCaptor;

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

        when(requester.requestAsAdmin(any()))
                .thenReturn(JsonEnvelopeBuilder.envelopeFrom(metadataWithRandomUUID("referencedata.query.offences"), payload));


        //when
        final JsonEnvelope envelope = envelope().with(metadataWithRandomUUID("referencedata.query.offences"))
                .build();
        final Optional<JsonObject> result = referenceDataService.getOffenceByCjsCode(
                envelope, PS_90010);

        //then
        verify(requester).requestAsAdmin(envelopeArgumentCaptor.capture());

        assertThat(envelopeArgumentCaptor.getValue(), is(jsonEnvelope(
                withMetadataEnvelopedFrom(envelope)
                        .withName("referencedata.query.offences"),
                payloadIsJson(
                        withJsonPath("$.cjsoffencecode", equalTo(PS_90010))
                ))
        ));
        assertThat(result.get().getString("cjsoffencecode"), is(PS_90010));
        assertThat(result.get().getString("title"), is("Public service vehicle - passenger use altered / defaced ticket"));
        assertThat(result.get().getString("legislation"), is("Contrary to regulation 7(1)(a) of the Public Service Vehicles (Conduct of Drivers, Inspectors, Conductors and Passengers) Regulations 1990 and section 25 of the Public Passenger Vehicles Act 1981."));
        verifyNoMoreInteractions(requester);
    }

    @Test
    public void shouldReturnEmptyJsonObjectWhenCjsOffenceCodeIsNotFound() {
        //given
        final JsonEnvelope envelope = envelope().with(metadataWithRandomUUID("referencedata.query.offences"))
                .build();
        final JsonObject payload = Json.createReader(
                new ByteArrayInputStream("{\"offences\":[]}".getBytes()))
                .readObject();

        when(requester.requestAsAdmin(any()))
                .thenReturn(JsonEnvelopeBuilder.envelopeFrom(metadataWithRandomUUID("referencedata.query.offences"), payload));


        //when
        final Optional<JsonObject> result = referenceDataService.getOffenceByCjsCode(envelope, PS_90010);

        //then
        verify(requester).requestAsAdmin(envelopeArgumentCaptor.capture());

        assertThat(envelopeArgumentCaptor.getValue(), is(jsonEnvelope(
                withMetadataEnvelopedFrom(envelope)
                        .withName("referencedata.query.offences"),
                payloadIsJson(
                        withJsonPath("$.cjsoffencecode", equalTo(PS_90010))
                ))
        ));
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
                .thenReturn(JsonEnvelopeBuilder.envelopeFrom(metadataWithRandomUUID("referencedata.get.judge"), payload));


        //when
        final JsonEnvelope envelope = envelope().with(metadataWithRandomUUID("referencedata.get.judge"))
                .build();
        final Optional<JsonObject> result = referenceDataService.getJudgeById(judgeId, envelope);

        //then
        verify(requester).request(envelopeArgumentCaptor.capture());

        assertThat(envelopeArgumentCaptor.getValue(), is(jsonEnvelope(
                withMetadataEnvelopedFrom(envelope)
                        .withName("referencedata.get.judge"),
                payloadIsJson(
                        withJsonPath("$.id", equalTo(judgeId.toString()))
                ))
        ));
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
                .thenReturn(JsonEnvelopeBuilder.envelopeFrom(metadataWithRandomUUID("referencedata.get.court-centre"), payload));


        //when
        final JsonEnvelope envelope = envelope().with(metadataWithRandomUUID("referencedata.get.court-centre"))
                .build();
        final Optional<JsonObject> result = referenceDataService.getCourtCentreById(courtCentreId, envelope);

        //then
        verify(requester).request(envelopeArgumentCaptor.capture());

        assertThat(envelopeArgumentCaptor.getValue(), is(jsonEnvelope(
                withMetadataEnvelopedFrom(envelope)
                        .withName("referencedata.get.court-centre"),
                payloadIsJson(
                        withJsonPath("$.id", equalTo(courtCentreId.toString()))
                ))
        ));
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

        when(requester.request(any()))
                .thenReturn(JsonEnvelopeBuilder.envelopeFrom(metadataWithRandomUUID(ORGANISATION_UNIT), payload));


        //when
        final JsonEnvelope envelope = envelope().with(metadataWithRandomUUID(ORGANISATION_UNIT))
                .build();
        final Optional<JsonObject> result = referenceDataService.getOrganisationUnitById(courtCentreId, envelope);

        //then
        verify(requester).request(envelopeArgumentCaptor.capture());

        assertThat(envelopeArgumentCaptor.getValue(), is(jsonEnvelope(
                withMetadataEnvelopedFrom(envelope)
                        .withName(ORGANISATION_UNIT),
                payloadIsJson(
                        withJsonPath("$.id", equalTo(courtCentreId.toString()))
                ))
        ));
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
    public void shouldRequestDocumentTypeDataById() {
        //given

        final UUID documentTypeId = randomUUID();
        final JsonObject payload = Json.createReader(
                new ByteArrayInputStream(getDocumentTypeDataById(documentTypeId).getBytes()))
                .readObject();
        when(requester.request(any()))
                .thenReturn(JsonEnvelopeBuilder.envelopeFrom(metadataWithRandomUUID(REFERENCEDATA_GET_DOCUMENT_TYPE), payload));


        //when
        final JsonEnvelope envelope = envelope().with(metadataWithRandomUUID(REFERENCEDATA_GET_DOCUMENT_TYPE))
                .build();
        final Optional<JsonObject> result = referenceDataService.getDocumentTypeData(documentTypeId, envelope);

        //then
        verify(requester).request(envelopeArgumentCaptor.capture());

        assertThat(envelopeArgumentCaptor.getValue(), is(jsonEnvelope(
                withMetadataEnvelopedFrom(envelope)
                        .withName(REFERENCEDATA_GET_DOCUMENT_TYPE),
                payloadIsJson(
                        withJsonPath("$.id", equalTo(documentTypeId.toString()))
                ))
        ));
        assertThat(result.get().getString("documentCategory"), is("Defendant level"));
        assertThat(result.get().getString("documentType"), is("Magistrate's Sending sheet"));
        assertThat(((JsonString) result.get().getJsonArray("documentAccess").stream().findFirst().get()).getString(), is("Legal advisors"));
        verifyNoMoreInteractions(requester);
    }

    @Test
    public void shouldRequestCourtOuCodeByPostcodeAndProsecutingAthority() {
        //given
        final String postCode = "CR11111";
        final String prosecutingAuth = "CPS";


        final JsonObject payload = getPayloadForCourts();

        when(requester.request(any()))
                .thenReturn(JsonEnvelope.envelopeFrom(
                        JsonEnvelope.metadataBuilder().withId(randomUUID()).withName(REFERENCEDATA_GET_OUCODE).build(),
                        payload));
        //when
        final JsonEnvelope envelope = JsonEnvelope.envelopeFrom(
                JsonEnvelope.metadataBuilder().withId(randomUUID()).withName(REFERENCEDATA_GET_OUCODE).build(),
                payload);

        final Optional<JsonObject> result = referenceDataService.getCourtsByPostCodeAndProsecutingAuthority(envelope, postCode, prosecutingAuth);

        //then
        verify(requester).request(envelopeArgumentCaptor.capture());

        assertThat(envelopeArgumentCaptor.getValue(), is(jsonEnvelope(
                withMetadataEnvelopedFrom(envelope)
                        .withName(REFERENCEDATA_GET_OUCODE),
                payloadIsJson(
                        withJsonPath("$.postcode", equalTo(postCode))
                ))
        ));
        assertThat(((JsonObject) result.get().getJsonArray("courts").get(0)).getString("oucodeL3Code"), is("B22KS00"));
        verifyNoMoreInteractions(requester);
    }

    @Test
    public void shouldGetLocalJusticeAreas() {

        final UUID id = randomUUID();
        final JsonEnvelope responseEnvelope = generateLocalJusticeResponseEnvelope(id);

        when(requester.request(any()))
                .thenReturn(responseEnvelope);

        final Optional<JsonObject> result = referenceDataService.getLocalJusticeArea(responseEnvelope, NATIONAL_COURT_CODE);

        verify(requester).request(welshEnvelopeArgumentCaptor.capture());

        assertThat(welshEnvelopeArgumentCaptor.getValue().metadata(), metadata().withName(REFERENCEDATA_QUERY_LOCAL_JUSTICE_AREAS));
        assertThat(welshEnvelopeArgumentCaptor.getValue().payload(), payload().isJson(withJsonPath("$.nationalCourtCode", equalTo(NATIONAL_COURT_CODE))));
    }

    @Test
    public void shouldRequestCourtByOuCode() {
        //given
        final String oucode = "CR11111";
        final String prosecutingAuth = "CPS";
        final UUID id = randomUUID();

        final JsonObject payload = getPayloadForOrgUnits(id.toString());

        when(requester.request(any()))
                .thenReturn(JsonEnvelope.envelopeFrom(
                        JsonEnvelope.metadataBuilder().withId(id).withName(REFERENCEDATA_GET_COURTCENTER).build(),
                        payload));
        //when
        final JsonEnvelope envelope = JsonEnvelope.envelopeFrom(
                JsonEnvelope.metadataBuilder().withId(randomUUID()).withName(REFERENCEDATA_GET_COURTCENTER).build(),
                payload);

        final Optional<JsonObject> result = referenceDataService.getCourtsOrganisationUnitsByOuCode(envelope, oucode);

        //then
        verify(requester).request(envelopeArgumentCaptor.capture());

        assertThat(envelopeArgumentCaptor.getValue(), is(jsonEnvelope(
                withMetadataEnvelopedFrom(envelope)
                        .withName(REFERENCEDATA_GET_COURTCENTER),
                payloadIsJson(
                        withJsonPath("$.oucode", equalTo(oucode))
                ))
        ));
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

        when(enveloper.withMetadataFrom(envelope, REFERENCEDATA_GET_OUCODE)).thenReturn(objectJsonEnvelopeFunction);
        when(objectJsonEnvelopeFunction.apply(any(JsonObject.class))).thenReturn(envelope);
        when(requester.request(envelope))
                .thenReturn(getEnvelope(REFERENCEDATA_GET_OUCODE, payloadForCourts));

        final JsonEnvelope envelopeForCourts = getEnvelope(REFERENCEDATA_GET_COURTCENTER);

        when(enveloper.withMetadataFrom(envelope, REFERENCEDATA_GET_COURTCENTER)).thenReturn(objectJsonEnvelopeFunctionForCourts);
        when(objectJsonEnvelopeFunctionForCourts.apply(any(JsonObject.class))).thenReturn(envelopeForCourts);
        when(requester.request(envelopeForCourts))
                .thenReturn(getEnvelope(REFERENCEDATA_GET_OUCODE, payloadForOrgUnits));

        final CourtCentre result = referenceDataService.getCourtCentre(envelope, postcode, prosecutingAuth);

        assertThat(result.getId(), is(id));
    }

    @Test
    public void shouldRequestReferralReasons() {
        JsonObject payload = getReferralReasonsPayload();

        when(requester.request(any())).thenReturn(getEnvelope(REFERENCEDATA_GET_REFERRAL_REASONS, payload));

        final JsonEnvelope envelope = getEnvelope(REFERENCEDATA_GET_REFERRAL_REASONS);
        final Optional<JsonObject> result = referenceDataService.getReferralReasons(envelope);

        JsonObject referralReasonsJson = result.get().getJsonArray("referralReasons").getJsonObject(0);
        JsonObject payloadReferralReasonJson = payload.getJsonArray("referralReasons").getJsonObject(0);
        assertThat(referralReasonsJson.getString("id"), is(payloadReferralReasonJson.getString("id")));
        assertThat(referralReasonsJson.getInt("seqId"), is(payloadReferralReasonJson.getInt("seqId")));
        assertThat(referralReasonsJson.getString("reason"), is(payloadReferralReasonJson.getString("reason")));
        assertThat(referralReasonsJson.getString("welshReason"), is(payloadReferralReasonJson.getString("welshReason")));
        assertThat(referralReasonsJson.getString("hearingCode"), is(payloadReferralReasonJson.getString("hearingCode")));

    }

    @Test
    public void shouldGetJudiciariesByJudiciaryIdList() throws Exception {
        JsonObject payload = generateJudiciariesJson();
        when(requester.request(any())).thenReturn(getEnvelope(REFERENCEDATA_QUERY_JUDICIARIES, payload));

        final JsonEnvelope envelope = getEnvelope(REFERENCEDATA_QUERY_JUDICIARIES);
        final Optional<JsonObject> result = referenceDataService.getJudiciariesByJudiciaryIdList(Arrays.asList(JUDICIARY_ID_1, JUDICIARY_ID_2), envelope);
        final JsonObject judiciariesJson = result.get();

        JsonObject judiciaryJson = judiciariesJson.getJsonArray("judiciaries").getJsonObject(0);
        assertThat(judiciaryJson.getString("id"), is(JUDICIARY_ID_1.toString()));
        assertThat(judiciaryJson.getString("titlePrefix"), is(JUDICIARY_TITLE_1));
        assertThat(judiciaryJson.getString("surname"), is(JUDICIARY_LAST_NAME_1));
        assertThat(judiciaryJson.getString("forenames"), is(JUDICIARY_FIRST_NAME_1));

        JsonObject judiciaryJson2 = judiciariesJson.getJsonArray("judiciaries").getJsonObject(1);
        assertThat(judiciaryJson2.getString("id"), is(JUDICIARY_ID_2.toString()));
        assertThat(judiciaryJson2.getString("titlePrefix"), is(JUDICIARY_TITLE_2));
        assertThat(judiciaryJson2.getString("surname"), is(JUDICIARY_LAST_NAME_2));
        assertThat(judiciaryJson2.getString("forenames"), is(JUDICIARY_FIRST_NAME_2));


    }

    private JsonObject generateJudiciariesJson() throws IOException {
        String jsonString = Resources.toString(Resources.getResource("referenceData.getJudiciariesByIdList.json"), Charset.defaultCharset())
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
        try (JsonReader jsonReader = createReader(new StringReader(jsonString))) {
            return jsonReader.readObject();
        }
    }

    private JsonEnvelope getEnvelope(final String name) {
        return JsonEnvelope.envelopeFrom(
                JsonEnvelope.metadataBuilder().withId(randomUUID()).withName(name).build(),
                Json.createObjectBuilder().build());
    }

    private JsonEnvelope getEnvelope(final String name, JsonObject jsonObject) {
        return JsonEnvelope.envelopeFrom(
                JsonEnvelope.metadataBuilder().withId(randomUUID()).withName(name).build(),
                jsonObject);
    }

    private JsonObject getPayloadForOrgUnits(String id) {
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
                "\"documentAccess\": [\n" +
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
}
