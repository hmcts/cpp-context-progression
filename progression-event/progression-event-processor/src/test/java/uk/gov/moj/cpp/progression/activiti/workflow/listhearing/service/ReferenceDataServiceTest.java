package uk.gov.moj.cpp.progression.activiti.workflow.listhearing.service;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.messaging.JsonObjectMetadata.metadataWithRandomUUID;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeMatcher.jsonEnvelope;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeMetadataMatcher.withMetadataEnvelopedFrom;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopePayloadMatcher.payloadIsJson;
import static uk.gov.justice.services.test.utils.core.messaging.JsonEnvelopeBuilder.envelope;

import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.core.requester.Requester;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.test.utils.core.enveloper.EnveloperFactory;
import uk.gov.justice.services.test.utils.core.messaging.JsonEnvelopeBuilder;
import uk.gov.moj.cpp.progression.service.ReferenceDataService;

import java.io.ByteArrayInputStream;
import java.util.Optional;
import java.util.UUID;

import javax.json.Json;
import javax.json.JsonObject;

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

    @Mock
    private Requester requester;

    @Spy
    Enveloper enveloper = EnveloperFactory.createEnveloper();;

    @InjectMocks
    private ReferenceDataService referenceDataService;

    @Captor
    private ArgumentCaptor<JsonEnvelope> envelopeArgumentCaptor;

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

        final UUID judgeId = UUID.randomUUID();
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

        final UUID courtCentreId = UUID.randomUUID();
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

    private String getCourtCentrePayload(UUID courtCentreId) {
        return "{\n" +
               " \"id\": \"" + courtCentreId + "\",\n"+
               "\"name\": \"Liverpool Crown Court\",\n"+
               "\"courtRooms\": [\n"+
               "{\n"+
               "\"id\": \"47834e9d-0bca-4f26-aa30-270580496e6e\",\n"+
               "\"name\": \"1\"\n"+
               "}]}";
    }

    private String getJudgePayload(UUID judgeId) {

        return "{\n" +
               " \"id\": \"" + judgeId + "\",\n"+
               "\"title\": \"HSS\",\n"+
               "\"firstName\": \"John\",\n"+
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

}
