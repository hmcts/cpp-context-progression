package uk.gov.moj.cpp.progression.processor;

import static java.util.Optional.of;
import static java.util.UUID.randomUUID;
import static javax.json.Json.createObjectBuilder;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.justice.services.test.utils.core.messaging.MetadataBuilderFactory.metadataWithRandomUUID;
import static uk.gov.moj.cpp.progression.utils.TestUtils.LJA_CODE;

import uk.gov.justice.core.courts.Address;
import uk.gov.justice.core.courts.BoxHearingRequest;
import uk.gov.justice.core.courts.ContactNumber;
import uk.gov.justice.core.courts.CourtApplication;
import uk.gov.justice.core.courts.CourtApplicationParty;
import uk.gov.justice.core.courts.CourtApplicationType;
import uk.gov.justice.core.courts.CourtCentre;
import uk.gov.justice.core.courts.JurisdictionType;
import uk.gov.justice.core.courts.MasterDefendant;
import uk.gov.justice.core.courts.Person;
import uk.gov.justice.core.courts.PersonDefendant;
import uk.gov.justice.progression.courts.SendStatdecAppointmentLetter;
import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.common.converter.ObjectToJsonObjectConverter;
import uk.gov.justice.services.common.converter.jackson.ObjectMapperProducer;
import uk.gov.justice.services.core.requester.Requester;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.progression.service.RefDataService;
import uk.gov.moj.cpp.progression.service.StatDecNotificationService;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import javax.json.JsonObject;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class SendStatDecAppointmentLetterProcessorTest {

    private static final String USER_ID = UUID.randomUUID().toString();

    @Mock
    private StatDecNotificationService statDecNotificationService;

    @Mock
    private RefDataService referenceDataService;
    @Mock
    private Requester requester;

    @Spy
    private final ObjectMapper objectMapper = new ObjectMapperProducer().objectMapper();

    @Mock
    private JsonObjectToObjectConverter jsonObjectToObjectConverter;

    @Spy
    @InjectMocks
    private final ObjectToJsonObjectConverter objectToJsonObjectConverter = new ObjectToJsonObjectConverter(objectMapper);

    @InjectMocks
    private SendStatDecAppointmentLetterProcessor eventProcessor;

    @Captor
    private ArgumentCaptor<String> captor;

    @Test
    public void shouldGenerateNotificationForStatDecAppointmentLetter()  {

        final CourtApplication courtApplication = CourtApplication.courtApplication()
                .withId(UUID.randomUUID())
                .withType(CourtApplicationType.courtApplicationType().build())
                .withApplicationReference("05PP1000915-01")
                .withApplicant(CourtApplicationParty.courtApplicationParty()
                        .withMasterDefendant(MasterDefendant.masterDefendant().withPersonDefendant(PersonDefendant.personDefendant().withPersonDetails(
                                Person.person()
                                        .withFirstName("John")
                                        .withLastName("Edward")
                                        .withDateOfBirth(LocalDate.of(1998, 8, 10))
                                        .withContact(
                                                ContactNumber.contactNumber()
                                                        .withPrimaryEmail("applicant@test.com")
                                                        .build())
                                        .withAddress(Address.address()
                                                .withAddress1("22 Acacia Avenue")
                                                .withAddress2("Acacia Town")
                                                .withAddress3("Acacia City")
                                                .withAddress4("Test")
                                                .withPostcode("AC1 4AC")
                                                .build()
                                        )
                                        .build()).build()).build())
                        .build())
                .build();
        final CourtCentre courtCentre = CourtCentre.courtCentre()
                .withId(randomUUID())
                .withName("Lavendar Hill Magistrates' Court")
                .withRoomName("Room 1")
                .withAddress(Address.address()
                        .withAddress1("Court Road")
                        .withAddress2("Court Town")
                        .withAddress3("Lavendar Hill, London")
                        .withPostcode("EA22 5TF")
                        .build())
                .build();
        final BoxHearingRequest boxHearing = BoxHearingRequest.boxHearingRequest()
                .withCourtCentre(courtCentre)
                .withJurisdictionType(JurisdictionType.MAGISTRATES)
                .withApplicationDueDate(LocalDate.now())
                .withSendAppointmentLetter(Boolean.TRUE)
                .build();
        final SendStatdecAppointmentLetter sendStatdecAppointmentLetter = SendStatdecAppointmentLetter.sendStatdecAppointmentLetter()
                .withCourtApplication(courtApplication)
                .withBoxHearing(boxHearing)
                .build();

        final Optional<JsonObject> courtCentreJson = getCourtCentreJson(false);

        final JsonEnvelope eventEnvelope = envelope(sendStatdecAppointmentLetter);

        when(jsonObjectToObjectConverter.convert(eventEnvelope.payloadAsJsonObject(), SendStatdecAppointmentLetter.class)).thenReturn(sendStatdecAppointmentLetter);
        when(referenceDataService.getCourtCentreWithCourtRoomsById(any(),any(),any())).thenReturn(courtCentreJson);
        this.eventProcessor.process(eventEnvelope);
        verify(statDecNotificationService).sendNotification(any(),any(),any(),any(),any(),any(), any());
    }


    @Test
    public void shouldGenerateNotificationForWelshStatDecAppointmentLetter()  {

        final CourtApplication courtApplication = CourtApplication.courtApplication()
                .withId(UUID.randomUUID())
                .withType(CourtApplicationType.courtApplicationType().build())
                .withApplicationReference("05PP1000915-01")
                .withApplicant(CourtApplicationParty.courtApplicationParty()
                        .withMasterDefendant(MasterDefendant.masterDefendant().withPersonDefendant(PersonDefendant.personDefendant().withPersonDetails(
                                Person.person()
                                        .withFirstName("John")
                                        .withLastName("Edward")
                                        .withDateOfBirth(LocalDate.of(1998, 8, 10))
                                        .withContact(
                                                ContactNumber.contactNumber()
                                                        .withPrimaryEmail("applicant@test.com")
                                                        .build())
                                        .withAddress(Address.address()
                                                .withAddress1("22 Acacia Avenue")
                                                .withAddress2("Acacia Town")
                                                .withAddress3("Acacia City")
                                                .withAddress4("Test")
                                                .withPostcode("AC1 4AC")
                                                .build()
                                        )
                                        .build()).build()).build())
                        .build())
                .build();
        final CourtCentre courtCentre = CourtCentre.courtCentre()
                .withId(randomUUID())
                .withName("Lavendar Hill Magistrates' Court")
                .withRoomName("Room 1")
                .withAddress(Address.address()
                        .withAddress1("Court Road")
                        .withAddress2("Court Town")
                        .withAddress3("Lavendar Hill, London")
                        .withPostcode("EA22 5TF")
                        .build())
                .build();
        final BoxHearingRequest boxHearing = BoxHearingRequest.boxHearingRequest()
                .withCourtCentre(courtCentre)
                .withJurisdictionType(JurisdictionType.MAGISTRATES)
                .withApplicationDueDate(LocalDate.now())
                .withSendAppointmentLetter(Boolean.TRUE)
                .build();
        final SendStatdecAppointmentLetter sendStatdecAppointmentLetter = SendStatdecAppointmentLetter.sendStatdecAppointmentLetter()
                .withCourtApplication(courtApplication)
                .withBoxHearing(boxHearing)
                .build();

        final Optional<JsonObject> courtCentreJson = getCourtCentreJson(true);

        final JsonEnvelope eventEnvelope = envelope(sendStatdecAppointmentLetter);

        when(jsonObjectToObjectConverter.convert(eventEnvelope.payloadAsJsonObject(), SendStatdecAppointmentLetter.class)).thenReturn(sendStatdecAppointmentLetter);
        when(referenceDataService.getCourtCentreWithCourtRoomsById(any(),any(),any())).thenReturn(courtCentreJson);
        this.eventProcessor.process(eventEnvelope);
        verify(statDecNotificationService).sendNotification(any(),any(),any(),any(),any(),any(), captor.capture());
        assertThat(captor.getValue(), equalTo("NPB_StatutoryDeclarationHearing"));
    }

    private Optional<JsonObject> getCourtCentreJson(final boolean isWelsh) {
        return of(createObjectBuilder()
                .add("lja", LJA_CODE)
                .add("isWelsh", isWelsh)
                .build()
        );
    }

    public JsonEnvelope envelope(final SendStatdecAppointmentLetter sendStatdecAppointmentLetter) {
        final JsonObject jsonObject = objectToJsonObjectConverter.convert(sendStatdecAppointmentLetter);
        return envelopeFrom(
                metadataWithRandomUUID("progression.event.send-statdec-appointment-letter").withUserId(USER_ID),
                objectToJsonObjectConverter.convert(jsonObject)
        );
    }


}
