package uk.gov.moj.cpp.progression.processor;

import static javax.json.Json.createArrayBuilder;
import static javax.json.Json.createObjectBuilder;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.test.utils.core.enveloper.EnveloperFactory.createEnveloper;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.runners.MockitoJUnitRunner;
import uk.gov.justice.core.courts.ConfirmedProsecutionCaseId;
import uk.gov.justice.core.courts.CourtCentre;
import uk.gov.justice.core.courts.ListDefendantRequest;
import uk.gov.justice.core.courts.ReferralReason;
import uk.gov.justice.core.courts.SummonsData;
import uk.gov.justice.core.courts.SummonsDataPrepared;
import uk.gov.justice.core.courts.SummonsRequired;
import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.common.converter.ObjectToJsonObjectConverter;
import uk.gov.justice.services.common.converter.ZonedDateTimes;
import uk.gov.justice.services.common.converter.jackson.ObjectMapperProducer;
import uk.gov.justice.services.core.dispatcher.SystemUserProvider;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.progression.service.DocumentGeneratorService;
import uk.gov.moj.cpp.progression.service.NotificationService;
import uk.gov.moj.cpp.progression.service.ProgressionService;
import uk.gov.moj.cpp.progression.service.ReferenceDataService;
import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonObject;
import java.util.Arrays;
import java.util.Collections;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;

@RunWith(MockitoJUnitRunner.class)
public class SummonsDataPreparedEventProcessorTest {

    private static final UUID CASE_ID = UUID.randomUUID();
    private static final UUID REFERRAL_ID = UUID.randomUUID();
    private static final UUID DEFENDANT_ID = UUID.randomUUID();
    private static final UUID COURT_CENTRE_ID = UUID.fromString("89b10041-b44d-43c8-9b1e-d1b9fee15c93");

    @Spy
    private final ObjectMapper objectMapper = new ObjectMapperProducer().objectMapper();

    @Spy
    @InjectMocks
    private final ObjectToJsonObjectConverter objectToJsonObjectConverter = new ObjectToJsonObjectConverter();

    @Spy
    private final Enveloper enveloper = createEnveloper();

    @Mock
    private ReferenceDataService refDataService;

    @Mock
    private SummonsDataPrepared summonsDataPrepared;

    @Mock
    private ProgressionService progressionService;

    @Mock
    private SystemUserProvider systemUserProvider;

    @Mock
    private JsonObjectToObjectConverter jsonObjectToObjectConverter;

    @Mock
    private Sender sender;

    @Mock
    private JsonEnvelope envelope;

    @Mock
    private JsonObject payload;

    @Mock
    private JsonEnvelope finalEnvelope;

    @Mock
    private NotificationService notificationService;

    @Mock
    private DocumentGeneratorService documentGeneratorService;

    @Mock
    private Function<Object, JsonEnvelope> enveloperFunction;

    @InjectMocks
    private SummonsDataPreparedEventProcessor summonsDataPreparedEventProcessor;

    @Captor
    private ArgumentCaptor<JsonEnvelope> envelopeArgumentCaptor;

    @Captor
    private ArgumentCaptor<JsonObject> jsonObjectArgumentCaptor;

    @Captor
    private ArgumentCaptor<Sender> senderArgumentCaptor;

    @Captor
    private ArgumentCaptor<String> templateArgumentCaptor;

    @Captor
    private ArgumentCaptor<UUID> caseIdArgumentCaptor;

    @Captor
    private ArgumentCaptor<UUID> applicationIdArgumentCaptor;

    @Test
    public void shouldGenerateSummonsPayloadForFirstHearing() {
        when(systemUserProvider.getContextSystemUserId()).thenReturn(Optional.of(UUID.randomUUID()));
        when(envelope.payloadAsJsonObject()).thenReturn(payload);
        when(jsonObjectToObjectConverter.convert(payload, SummonsDataPrepared.class)).thenReturn(summonsDataPrepared);
        final SummonsData summonsData = generateSummonsData(SummonsRequired.FIRST_HEARING);
        when(summonsDataPrepared.getSummonsData()).thenReturn(summonsData);
        when(progressionService.getProsecutionCaseDetailById(envelope, CASE_ID.toString())).thenReturn(Optional.of(generateProsecutionCaseJson()));
        when(refDataService.getReferralReasons(envelope)).thenReturn(Optional.of(generateReferralReasonsJson()));
        when(refDataService.getOrganisationUnitById(COURT_CENTRE_ID, envelope)).thenReturn(Optional.of(generateCourtCentreJson()));
        when(refDataService.getEnforcementAreaByLjaCode(envelope, "1810")).thenReturn(generateLjaDetails());
        when(enveloper.withMetadataFrom(envelope, "progression.command.create-court-document")).thenReturn(enveloperFunction);
        when(enveloperFunction.apply(any(JsonObject.class))).thenReturn(finalEnvelope);

        summonsDataPreparedEventProcessor.requestSummons(envelope);

        verify(documentGeneratorService).generateDocument(envelopeArgumentCaptor.capture(), jsonObjectArgumentCaptor.capture(), templateArgumentCaptor.capture(), senderArgumentCaptor.capture(), caseIdArgumentCaptor.capture(), applicationIdArgumentCaptor.capture());

        assertThat(caseIdArgumentCaptor.getValue(), is(CASE_ID));
        final JsonObject summonsDataJson = jsonObjectArgumentCaptor.getValue();
        assertThat(envelopeArgumentCaptor.getValue(), is(envelope));

        assertThat(summonsDataJson, notNullValue());
        assertThat(summonsDataJson.getString("subTemplateName"), is(SummonsRequired.FIRST_HEARING.toString()));
        assertThat(summonsDataJson.getString("type"), is(SummonsRequired.FIRST_HEARING.toString()));
        assertThat(summonsDataJson.getString("ljaCode"), is("2577"));
        assertThat(summonsDataJson.getString("ljaName"), is("South West London Magistrates' Court"));
        assertThat(summonsDataJson.getString("caseReference"), is("TFL12345"));
        assertThat(summonsDataJson.getString("courtCentreName"), is("Liverpool Mag Court"));

        final JsonObject defendantJson = summonsDataJson.getJsonObject("defendant");
        assertThat(defendantJson, notNullValue());
        assertThat(defendantJson.getString("name"), is("Harry Jack Kane"));
        assertThat(defendantJson.getString("dateOfBirth"), is("2010-01-01"));

        final JsonObject defendantAddressJson = defendantJson.getJsonObject("address");
        assertThat(defendantAddressJson, notNullValue());
        assertThat(defendantAddressJson.getString("line1"), is("22 Acacia Avenue"));
        assertThat(defendantAddressJson.getString("line2"), is("Acacia Town"));
        assertThat(defendantAddressJson.getString("line3"), is("Acacia City"));
        assertThat(defendantAddressJson.getString("line4"), is("Acacia District"));
        assertThat(defendantAddressJson.getString("line5"), is("Acacia County"));
        assertThat(defendantAddressJson.getString("postCode"), is("AC1 4AC"));

        final JsonArray offencesJson = summonsDataJson.getJsonArray("offences");
        final JsonObject offenceJson = offencesJson.getJsonObject(0);
        assertThat(offenceJson.getString("offenceTitle"), is("off title"));
        assertThat(offenceJson.getString("offenceTitleWelsh"), is("off title welsh"));
        assertThat(offenceJson.getString("offenceLegislation"), is("off legis"));
        assertThat(offenceJson.getString("offenceLegislationWelsh"), is("off legis welsh"));

        final JsonObject addresseeJson = summonsDataJson.getJsonObject("addressee");
        assertThat(addresseeJson, notNullValue());
        assertThat(addresseeJson.getString("name"), is("Harry Jack Kane"));

        final JsonObject addresseeAddressJson = addresseeJson.getJsonObject("address");
        assertThat(addresseeAddressJson, notNullValue());
        assertThat(addresseeAddressJson.getString("line1"), is("22 Acacia Avenue"));
        assertThat(addresseeAddressJson.getString("line2"), is("Acacia Town"));
        assertThat(addresseeAddressJson.getString("line3"), is("Acacia City"));
        assertThat(addresseeAddressJson.getString("line4"), is("Acacia District"));
        assertThat(addresseeAddressJson.getString("line5"), is("Acacia County"));
        assertThat(addresseeAddressJson.getString("postCode"), is("AC1 4AC"));


        final JsonObject hearingCourtDetails = summonsDataJson.getJsonObject("hearingCourtDetails");
        assertThat(hearingCourtDetails.getString("courtName"), is("Liverpool Mag Court"));
        assertThat(hearingCourtDetails.getString("hearingDate"), is("2018-04-01"));
        assertThat(hearingCourtDetails.getString("hearingTime"), is("2:00 PM"));

        final JsonObject courtAddress = hearingCourtDetails.getJsonObject("courtAddress");
        assertThat(courtAddress.getString("line1"), is("176a Lavender Hill"));
        assertThat(courtAddress.getString("line2"), is("London"));
        assertThat(courtAddress.getString("line3"), is("address line 3"));
        assertThat(courtAddress.getString("line4"), is("address line 4"));
        assertThat(courtAddress.getString("line5"), is("address line 5"));
        assertThat(courtAddress.getString("postCode"), is("SW11 1JU"));
    }

    @Test
    public void shouldGenerateSummonsPayloadForSjpReferral() {
        when(systemUserProvider.getContextSystemUserId()).thenReturn(Optional.of(UUID.randomUUID()));
        when(envelope.payloadAsJsonObject()).thenReturn(payload);
        when(jsonObjectToObjectConverter.convert(payload, SummonsDataPrepared.class)).thenReturn(summonsDataPrepared);
        final SummonsData summonsData = generateSummonsData(SummonsRequired.SJP_REFERRAL);
        when(summonsDataPrepared.getSummonsData()).thenReturn(summonsData);
        when(progressionService.getProsecutionCaseDetailById(envelope, CASE_ID.toString())).thenReturn(Optional.of(generateProsecutionCaseJson()));
        when(refDataService.getReferralReasons(envelope)).thenReturn(Optional.of(generateReferralReasonsJson()));
        when(refDataService.getOrganisationUnitById(COURT_CENTRE_ID, envelope)).thenReturn(Optional.of(generateCourtCentreJson()));
        when(refDataService.getEnforcementAreaByLjaCode(envelope, "1810")).thenReturn(generateLjaDetails());
        when(enveloper.withMetadataFrom(envelope, "progression.command.create-court-document")).thenReturn(enveloperFunction);
        when(enveloperFunction.apply(any(JsonObject.class))).thenReturn(finalEnvelope);

        summonsDataPreparedEventProcessor.requestSummons(envelope);

        verify(documentGeneratorService).generateDocument(envelopeArgumentCaptor.capture(), jsonObjectArgumentCaptor.capture(), templateArgumentCaptor.capture(), senderArgumentCaptor.capture(), caseIdArgumentCaptor.capture(), applicationIdArgumentCaptor.capture());

        assertThat(caseIdArgumentCaptor.getValue(), is(CASE_ID));
        final JsonObject summonsDataJson = jsonObjectArgumentCaptor.getValue();
        assertThat(envelopeArgumentCaptor.getValue(), is(envelope));

        assertThat(summonsDataJson, notNullValue());
        assertThat(summonsDataJson.getString("subTemplateName"), is(SummonsRequired.SJP_REFERRAL.toString()));
        assertThat(summonsDataJson.getString("type"), is(SummonsRequired.SJP_REFERRAL.toString()));
        assertThat(summonsDataJson.getString("ljaCode"), is("2577"));
        assertThat(summonsDataJson.getString("ljaName"), is("South West London Magistrates' Court"));
        assertThat(summonsDataJson.getString("caseReference"), is("TFL12345"));
        assertThat(summonsDataJson.getString("courtCentreName"), is("Liverpool Mag Court"));

        final JsonObject defendantJson = summonsDataJson.getJsonObject("defendant");
        assertThat(defendantJson, notNullValue());
        assertThat(defendantJson.getString("name"), is("Harry Jack Kane"));
        assertThat(defendantJson.getString("dateOfBirth"), is("2010-01-01"));

        final JsonObject defendantAddressJson = defendantJson.getJsonObject("address");
        assertThat(defendantAddressJson, notNullValue());
        assertThat(defendantAddressJson.getString("line1"), is("22 Acacia Avenue"));
        assertThat(defendantAddressJson.getString("line2"), is("Acacia Town"));
        assertThat(defendantAddressJson.getString("line3"), is("Acacia City"));
        assertThat(defendantAddressJson.getString("line4"), is("Acacia District"));
        assertThat(defendantAddressJson.getString("line5"), is("Acacia County"));
        assertThat(defendantAddressJson.getString("postCode"), is("AC1 4AC"));

        final JsonArray offencesJson = summonsDataJson.getJsonArray("offences");
        final JsonObject offenceJson = offencesJson.getJsonObject(0);
        assertThat(offenceJson.getString("offenceTitle"), is("off title"));
        assertThat(offenceJson.getString("offenceTitleWelsh"), is("off title welsh"));
        assertThat(offenceJson.getString("offenceLegislation"), is("off legis"));
        assertThat(offenceJson.getString("offenceLegislationWelsh"), is("off legis welsh"));

        final JsonObject addresseeJson = summonsDataJson.getJsonObject("addressee");
        assertThat(addresseeJson, notNullValue());
        assertThat(addresseeJson.getString("name"), is("Harry Jack Kane"));

        final JsonObject addresseeAddressJson = addresseeJson.getJsonObject("address");
        assertThat(addresseeAddressJson, notNullValue());
        assertThat(addresseeAddressJson.getString("line1"), is("22 Acacia Avenue"));
        assertThat(addresseeAddressJson.getString("line2"), is("Acacia Town"));
        assertThat(addresseeAddressJson.getString("line3"), is("Acacia City"));
        assertThat(addresseeAddressJson.getString("line4"), is("Acacia District"));
        assertThat(addresseeAddressJson.getString("line5"), is("Acacia County"));
        assertThat(addresseeAddressJson.getString("postCode"), is("AC1 4AC"));

        final JsonObject hearingCourtDetails = summonsDataJson.getJsonObject("hearingCourtDetails");
        assertThat(hearingCourtDetails.getString("courtName"), is("Liverpool Mag Court"));
        assertThat(hearingCourtDetails.getString("hearingDate"), is("2018-04-01"));
        assertThat(hearingCourtDetails.getString("hearingTime"), is("2:00 PM"));

        final JsonObject courtAddress = hearingCourtDetails.getJsonObject("courtAddress");
        assertThat(courtAddress.getString("line1"), is("176a Lavender Hill"));
        assertThat(courtAddress.getString("line2"), is("London"));
        assertThat(courtAddress.getString("line3"), is("address line 3"));
        assertThat(courtAddress.getString("line4"), is("address line 4"));
        assertThat(courtAddress.getString("line5"), is("address line 5"));
        assertThat(courtAddress.getString("postCode"), is("SW11 1JU"));

        final JsonObject referralReason = summonsDataJson.getJsonObject("referralContent");
        assertThat(referralReason, notNullValue());
        assertThat(referralReason.getString("referralReason"), is("Sections 135"));
        assertThat(referralReason.getString("referralReasonWelsh"), is("Reason for Welsh"));
        assertThat(referralReason.getString("referralText"), is("reason text"));
        assertThat(referralReason.getString("referralTextWelsh"), is("welsh reason text"));
    }

    @Test
    public void shouldGenerateSummonsPayloadForYouth() {
        when(systemUserProvider.getContextSystemUserId()).thenReturn(Optional.of(UUID.randomUUID()));
        when(envelope.payloadAsJsonObject()).thenReturn(payload);
        when(jsonObjectToObjectConverter.convert(payload, SummonsDataPrepared.class)).thenReturn(summonsDataPrepared);
        final SummonsData summonsData = generateSummonsData(SummonsRequired.YOUTH);
        when(summonsDataPrepared.getSummonsData()).thenReturn(summonsData);
        when(progressionService.getProsecutionCaseDetailById(envelope, CASE_ID.toString())).thenReturn(Optional.of(generateProsecutionCaseJson()));
        when(refDataService.getReferralReasons(envelope)).thenReturn(Optional.of(generateReferralReasonsJson()));
        when(refDataService.getOrganisationUnitById(COURT_CENTRE_ID, envelope)).thenReturn(Optional.of(generateCourtCentreJson()));
        when(refDataService.getEnforcementAreaByLjaCode(envelope, "1810")).thenReturn(generateLjaDetails());
        when(enveloper.withMetadataFrom(envelope, "progression.command.create-court-document")).thenReturn(enveloperFunction);
        when(enveloperFunction.apply(any(JsonObject.class))).thenReturn(finalEnvelope);

        summonsDataPreparedEventProcessor.requestSummons(envelope);

        verify(documentGeneratorService, times(2)).generateDocument(envelopeArgumentCaptor.capture(), jsonObjectArgumentCaptor.capture(), templateArgumentCaptor.capture(), senderArgumentCaptor.capture(), caseIdArgumentCaptor.capture(), applicationIdArgumentCaptor.capture());

        assertThat(caseIdArgumentCaptor.getValue(), is(CASE_ID));
        final JsonObject summonsDataJson = jsonObjectArgumentCaptor.getValue();
        assertThat(envelopeArgumentCaptor.getValue(), is(envelope));

        assertThat(summonsDataJson, notNullValue());
        assertThat(summonsDataJson.getString("subTemplateName"), is(SummonsRequired.YOUTH.toString()));
        assertThat(summonsDataJson.getString("type"), is(SummonsRequired.YOUTH.toString()));
        assertThat(summonsDataJson.getString("ljaCode"), is("2577"));
        assertThat(summonsDataJson.getString("ljaName"), is("South West London Magistrates' Court"));
        assertThat(summonsDataJson.getString("caseReference"), is("TFL12345"));
        assertThat(summonsDataJson.getString("courtCentreName"), is("Liverpool Mag Court"));

        final JsonObject defendantJson = summonsDataJson.getJsonObject("defendant");
        assertThat(defendantJson, notNullValue());
        assertThat(defendantJson.getString("name"), is("Harry Jack Kane"));
        assertThat(defendantJson.getString("dateOfBirth"), is("2010-01-01"));

        final JsonObject defendantAddressJson = defendantJson.getJsonObject("address");
        assertThat(defendantAddressJson, notNullValue());
        assertThat(defendantAddressJson.getString("line1"), is("22 Acacia Avenue"));
        assertThat(defendantAddressJson.getString("line2"), is("Acacia Town"));
        assertThat(defendantAddressJson.getString("line3"), is("Acacia City"));
        assertThat(defendantAddressJson.getString("line4"), is("Acacia District"));
        assertThat(defendantAddressJson.getString("line5"), is("Acacia County"));
        assertThat(defendantAddressJson.getString("postCode"), is("AC1 4AC"));

        final JsonArray offencesJson = summonsDataJson.getJsonArray("offences");
        final JsonObject offenceJson = offencesJson.getJsonObject(0);
        assertThat(offenceJson.getString("offenceTitle"), is("off title"));
        assertThat(offenceJson.getString("offenceTitleWelsh"), is("off title welsh"));
        assertThat(offenceJson.getString("offenceLegislation"), is("off legis"));
        assertThat(offenceJson.getString("offenceLegislationWelsh"), is("off legis welsh"));

        final JsonObject addresseeJson = summonsDataJson.getJsonObject("addressee");
        assertThat(addresseeJson, notNullValue());

        final JsonObject addresseeAddressJson = addresseeJson.getJsonObject("address");
        assertThat(addresseeAddressJson, notNullValue());
        assertThat(addresseeAddressJson.getString("line1"), is("22 Acacia Avenue"));
        assertThat(addresseeAddressJson.getString("line2"), is("Acacia Town"));
        assertThat(addresseeAddressJson.getString("line3"), is("Acacia City"));
        assertThat(addresseeAddressJson.getString("line4"), is("Acacia District"));
        assertThat(addresseeAddressJson.getString("line5"), is("Acacia County"));
        assertThat(addresseeAddressJson.getString("postCode"), is("AC1 4AC"));

        final JsonObject youthJson = summonsDataJson.getJsonObject("youthContent");
        assertThat(youthJson, notNullValue());

        final JsonObject guardianAddressJson = youthJson.getJsonObject("address");
        assertThat(guardianAddressJson, notNullValue());
        assertThat(guardianAddressJson.getString("line1"), is("22 Acacia Avenue"));
        assertThat(guardianAddressJson.getString("line2"), is("Acacia Town"));
        assertThat(guardianAddressJson.getString("line3"), is("Acacia City"));
        assertThat(guardianAddressJson.getString("line4"), is("Acacia District"));
        assertThat(guardianAddressJson.getString("line5"), is("Acacia County"));
        assertThat(guardianAddressJson.getString("postCode"), is("AC1 4AC"));

        final JsonObject hearingCourtDetails = summonsDataJson.getJsonObject("hearingCourtDetails");
        assertThat(hearingCourtDetails.getString("courtName"), is("Liverpool Mag Court"));
        assertThat(hearingCourtDetails.getString("hearingDate"), is("2018-04-01"));
        assertThat(hearingCourtDetails.getString("hearingTime"), is("2:00 PM"));

        final JsonObject courtAddress = hearingCourtDetails.getJsonObject("courtAddress");
        assertThat(courtAddress.getString("line1"), is("176a Lavender Hill"));
        assertThat(courtAddress.getString("line2"), is("London"));
        assertThat(courtAddress.getString("line3"), is("address line 3"));
        assertThat(courtAddress.getString("line4"), is("address line 4"));
        assertThat(courtAddress.getString("line5"), is("address line 5"));
        assertThat(courtAddress.getString("postCode"), is("SW11 1JU"));
    }

    private JsonObject generateProsecutionCaseJson() {
        return createObjectBuilder()
                .add("prosecutionCase",
                        createObjectBuilder()
                                .add("id", CASE_ID.toString())
                                .add("defendants", generateDefendantArray())
                                .add("prosecutionCaseIdentifier", generateProsecutionCaseIdentifier())
                )
                .build();
    }

    private static JsonObject generateProsecutionCaseIdentifier() {
        return createObjectBuilder()
                .add("prosecutionAuthorityReference", "TFL12345")
                .build();
    }

    private JsonArray generateDefendantArray() {
        return createArrayBuilder()
                .add(
                        createObjectBuilder()
                                .add("id", DEFENDANT_ID.toString())
                                .add("prosecutionCaseId", CASE_ID.toString())
                                .add("personDefendant", generatePersonDefendant())
                                .add("associatedPersons", generateAssociatedPersonsArray())
                                .add("offences", generateOffenceArray())
                )
                .build();
    }

    private static JsonObject generatePersonDefendant() {
        return createObjectBuilder()
                .add("personDetails",
                        Json.createObjectBuilder()
                                .add("firstName", "Harry")
                                .add("middleName", "Jack")
                                .add("lastName", "Kane")
                                .add("dateOfBirth", "2010-01-01")
                                .add("address", createObjectBuilder()
                                        .add("address1", "22 Acacia Avenue")
                                        .add("address2", "Acacia Town")
                                        .add("address3", "Acacia City")
                                        .add("address4", "Acacia District")
                                        .add("address5", "Acacia County")
                                        .add("postcode", "AC1 4AC")
                                )
                )
                .build();
    }

    private JsonArray generateOffenceArray() {
        return createArrayBuilder()
                .add(
                        createObjectBuilder()
                                .add("offenceTitle", "off title")
                                .add("offenceTitleWelsh", "off title welsh")
                                .add("offenceLegislation", "off legis")
                                .add("offenceLegislationWelsh", "off legis welsh")
                )
                .build();
    }

    private JsonArray generateAssociatedPersonsArray() {
        return createArrayBuilder()
                .add(
                        createObjectBuilder()
                                .add("person",
                                        Json.createObjectBuilder()
                                                .add("firstName", "William")
                                                .add("middleName", "Senior")
                                                .add("lastName", "Kane")
                                                .add("dateOfBirth", "2010-01-01")
                                                .add("address", createObjectBuilder()
                                                        .add("address1", "22 Acacia Avenue")
                                                        .add("address2", "Acacia Town")
                                                        .add("address3", "Acacia City")
                                                        .add("address4", "Acacia District")
                                                        .add("address5", "Acacia County")
                                                        .add("postcode", "AC1 4AC")
                                                )
                                )
                                .add("role", "role text")
                )
                .build();
    }

    private static JsonObject generateReferralReasonsJson() {
        return createObjectBuilder()
                .add("referralReasons", createArrayBuilder()
                        .add(Json.createObjectBuilder()
                                .add("id", REFERRAL_ID.toString())
                                .add("reason", "Sections 135")
                                .add("welshReason", "Reason for Welsh")
                                .add("subReason", "reason text")
                                .add("welshSubReason", "welsh reason text")
                        ))
                .build();
    }

    private static JsonObject generateCourtCentreJson() {
        return createObjectBuilder()
                .add("id", "f8254db1-1683-483e-afb3-b87fde5a0a26")
                .add("oucodeL1Name", "Magistrates' Courts")
                .add("oucodeL3Name", "Liverpool Mag Court")
                .add("lja", "1810")
                .add("oucodeL3WelshName", "welshName_Test")
                .add("address1", "176a Lavender Hill")
                .add("address2", "London")
                .add("address3", "address line 3")
                .add("address4", "address line 4")
                .add("address5", "address line 5")
                .add("postcode", "SW11 1JU")
                .build();
    }

    private static JsonObject generateLjaDetails() {
        return createObjectBuilder()
                .add("localJusticeArea",
                        createObjectBuilder()
                                .add("name", "South West London Magistrates' Court")
                                .add("nationalCourtCode", "2577")
                )
                .build();
    }

    private SummonsData generateSummonsData(final SummonsRequired summonsRequired) {
        return SummonsData.summonsData()
                .withConfirmedProsecutionCaseIds(Arrays.asList(generateConfirmedProsecutionId()))
                .withCourtCentre(generateCourtCentre())
                .withHearingDateTime(ZonedDateTimes.fromString("2018-04-01T13:00:00.000Z"))
                .withListDefendantRequests(Arrays.asList(ListDefendantRequest.listDefendantRequest()
                        .withSummonsRequired(summonsRequired)
                        .withProsecutionCaseId(CASE_ID)
                        .withReferralReason(ReferralReason.referralReason()
                                .withDefendantId(DEFENDANT_ID)
                                .withId(REFERRAL_ID)
                                .build())
                        .build()))
                .build();
    }

    private ConfirmedProsecutionCaseId generateConfirmedProsecutionId() {
        return ConfirmedProsecutionCaseId.confirmedProsecutionCaseId()
                .withId(CASE_ID)
                .withConfirmedDefendantIds(Collections.singletonList(DEFENDANT_ID))
                .build();
    }

    private static CourtCentre generateCourtCentre() {
        return CourtCentre.courtCentre()
                .withId(COURT_CENTRE_ID)
                .withName("00ObpXuu51")
                .withRoomId(UUID.fromString("d7020fe0-cd97-4ce0-84c2-fd00ff0bc48a"))
                .withRoomName("JK2Y7hu0Tc")
                .withWelshName("3IpJDfdfhS")
                .withWelshRoomName("hm60SAXokc")
                .build();
    }
}
