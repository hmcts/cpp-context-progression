package uk.gov.moj.cpp.progression.processor;

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
import uk.gov.moj.cpp.progression.service.PrintService;
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

import static javax.json.Json.createArrayBuilder;
import static javax.json.Json.createObjectBuilder;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.test.utils.core.enveloper.EnveloperFactory.createEnveloper;

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
    private PrintService printService;
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
    private ArgumentCaptor<UUID> uuidArgumentCaptor;

    @Test
    public void shouldHandleEventAndSendSummonsRequestedMessage() {
        //Given
        final UUID systemUserid = UUID.randomUUID();
        when(systemUserProvider.getContextSystemUserId()).thenReturn(Optional.of(systemUserid));
        when(envelope.payloadAsJsonObject()).thenReturn(payload);
        when(jsonObjectToObjectConverter.convert(payload, SummonsDataPrepared.class)).thenReturn(summonsDataPrepared);
        final SummonsData summonsData = generateSummonsData();
        when(summonsDataPrepared.getSummonsData()).thenReturn(summonsData);
        when(progressionService.getProsecutionCaseDetailById(envelope, CASE_ID.toString())).thenReturn(Optional.of(generateProsecutionCaseJson()));
        when(refDataService.getReferralReasons(envelope)).thenReturn(Optional.of(generateReferralReasonsJson()));
        when(refDataService.getOrganisationUnitById(COURT_CENTRE_ID, envelope)).thenReturn(Optional.of(generateCourtCentreJson()));
        when(refDataService.getEnforcementAreaByLjaCode(envelope,"1810")).thenReturn(generateLjaDetails());
        when(progressionService.getDefendantRequestByDefendantId(envelope, DEFENDANT_ID.toString()))
                .thenReturn(Optional.of(generateDefendantRequest()));
        when(enveloper.withMetadataFrom(envelope, "progression.command.create-court-document")).thenReturn(enveloperFunction);
        when(enveloperFunction.apply(any(JsonObject.class))).thenReturn(finalEnvelope);

        summonsDataPreparedEventProcessor.requestSummons(envelope);

        verify(documentGeneratorService).generateSummons(envelopeArgumentCaptor.capture(), jsonObjectArgumentCaptor.capture(), senderArgumentCaptor.capture(), uuidArgumentCaptor.capture());

        final JsonObject summonsDataJson = jsonObjectArgumentCaptor.getValue();
        assertThat(envelopeArgumentCaptor.getValue(), is(envelope));
        assertThat(senderArgumentCaptor.getValue(), is(sender));
        assertThat(summonsDataJson.getString("caseReference").startsWith("TFL"), is(true));
        assertThat(summonsDataJson.getString("summonsType"), is("SJP_REFERRAL"));
        assertThat(summonsDataJson.getString("ljaCode"), is("2577"));
        assertThat(summonsDataJson.getString("ljaName"), is("South West London Magistrates' Court"));
        assertThat(summonsDataJson.getString("summonsCourtTime"), is("11:00"));
        assertThat(uuidArgumentCaptor.getValue(), is(CASE_ID));

    }

    private static JsonObject generateDefendantRequest() {
        return createObjectBuilder()
                .add("defendantId", DEFENDANT_ID.toString())
                .add("prosecutionCaseId", CASE_ID.toString())
                .add("summonsType", "SJP_REFERRAL")
                .add("referralReasonId", REFERRAL_ID.toString())
                .build();
    }

    private JsonObject generateProsecutionCaseJson() {
        return createObjectBuilder()
                .add("prosecutionCase",
                        createObjectBuilder()
                                .add("id", CASE_ID.toString())
                                .add("defendants", generateDefendantArray())
                                .add("prosecutionCaseIdentifier", generateProsecutionCaseIdentifier())
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
                )
                .build();
    }

    private static JsonObject generateReferralReasonsJson() {
        return createObjectBuilder()
                .add("referralReasons", createArrayBuilder()
                        .add(Json.createObjectBuilder()
                                .add("id", REFERRAL_ID.toString())
                                .add("reason", "Sections 135")
                                .add("welshReason", "Reasson for Welsh")
                                .add("subReason", "reason text")
                                .add("welshSubReason", "welsh reason text")
                        ))
                .build();
    }

    private static JsonObject generateCourtCentreJson() {
        return createObjectBuilder()
                .add("id", "f8254db1-1683-483e-afb3-b87fde5a0a26")
                .add("oucodeL1Name", "Magistrates' Courts")
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

    private SummonsData generateSummonsData() {
        return SummonsData.summonsData()
                .withConfirmedProsecutionCaseIds(Arrays.asList(generateConfirmedProsecutionId()))
                .withCourtCentre(generateCourtCentre())
                .withHearingDateTime(ZonedDateTimes.fromString("2018-06-01T10:00:00.000Z"))
                .withListDefendantRequests(Arrays.asList(ListDefendantRequest.listDefendantRequest()
                        .withSummonsRequired(SummonsRequired.SJP_REFERRAL)
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
