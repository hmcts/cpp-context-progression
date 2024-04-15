package uk.gov.moj.cpp.progression.service;

import org.hamcrest.Matchers;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.Spy;
import org.mockito.runners.MockitoJUnitRunner;
import uk.gov.justice.core.courts.Address;
import uk.gov.justice.core.courts.ContactNumber;
import uk.gov.justice.core.courts.CourtApplication;
import uk.gov.justice.core.courts.CourtApplicationParty;
import uk.gov.justice.core.courts.CourtApplicationType;
import uk.gov.justice.core.courts.CourtCentre;
import uk.gov.justice.core.courts.DefendantCase;
import uk.gov.justice.core.courts.JurisdictionType;
import uk.gov.justice.core.courts.MasterDefendant;
import uk.gov.justice.core.courts.Person;
import uk.gov.justice.core.courts.PersonDefendant;
import uk.gov.justice.core.courts.notification.EmailChannel;
import uk.gov.justice.services.common.converter.ObjectToJsonObjectConverter;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.test.utils.core.enveloper.EnveloperFactory;
import uk.gov.moj.cpp.material.url.MaterialUrlGenerator;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;

import static java.time.format.DateTimeFormatter.ofPattern;
import static java.util.UUID.randomUUID;
import static javax.json.Json.createObjectBuilder;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.justice.services.test.utils.core.messaging.MetadataBuilderFactory.metadataWithRandomUUIDAndName;
import static uk.gov.moj.cpp.progression.domain.constant.DateTimeFormats.TIME_HMMA;
import static java.util.Arrays.asList;
import static java.util.Optional.ofNullable;

@RunWith(MockitoJUnitRunner.class)
public class StatDecNotificationServiceTest {

    private final JsonEnvelope envelope = envelopeFrom(metadataWithRandomUUIDAndName(), createObjectBuilder().build());

    private UUID materialId;

    private UUID notificationId;

    private UUID applicationId;

    @Spy
    private Enveloper enveloper = EnveloperFactory.createEnveloper();

    @Mock
    private NotificationService notificationService;

    @Mock
    private ApplicationParameters applicationParameters;


    @Mock
    private StatDecLetterService statDecLetterService;


    @Mock
    private MaterialUrlGenerator materialUrlGenerator;

    @InjectMocks
    private StatDecNotificationService statDecNotificationService;

    @Captor
    private ArgumentCaptor<JsonEnvelope> envelopeArgumentCaptor;

    @Captor
    private ArgumentCaptor<UUID> applicationIdCaptor;

    @Captor
    private ArgumentCaptor<UUID> caseIdCaptor;

    @Captor
    private ArgumentCaptor<UUID> materialIdCaptor;

    @Captor
    private ArgumentCaptor<UUID> notificationIdCaptor;

    @Captor
    private ArgumentCaptor<List<EmailChannel>> emailNotificationsCaptor;

    @Mock
    private RefDataService referenceDataService;

    @Mock
    ObjectToJsonObjectConverter objectToJsonObjectConverter;

    @Mock
    private Sender sender;


    private CourtCentre courtCentre = CourtCentre.courtCentre().withName("Test Court Centre").withAddress(Address.address()
            .withAddress1("Test Address 1")
            .withAddress2("Test Address 2")
            .withAddress3("Test Address 3")
            .withPostcode("AS1 1DF").build()).build();

    private final ZonedDateTime hearingDateTime = ZonedDateTime.of(
            LocalDate.of(2021, 4, 19),
            LocalTime.of(10, 0),
            ZoneId.of("UTC"));

    private static final String STAT_DEC_VIRTUAL_HEARING = "StatDecVirtualHearing";

    private static final DateTimeFormatter TIME_FORMATTER = ofPattern(TIME_HMMA.getValue());

    private static final ZoneId UK_TIME_ZONE = ZoneId.of("Europe/London");




    @Before
    public void setUp() {
        this.materialId = randomUUID();
        this.notificationId = randomUUID();
        this.applicationId = randomUUID();
    }

    @Test
    public void testSendNotificationWhenEmailIsNotPresentButAddressIsPresentForApplicant() {

        final DefendantCase defendantCase = DefendantCase.defendantCase()
                .withCaseId(randomUUID())
                .withDefendantId(randomUUID()).build();

        final CourtApplication courtApplication = CourtApplication.courtApplication()
                .withId(applicationId)
                .withType(CourtApplicationType.courtApplicationType().build())
                .withApplicationReference("05PP1000915-01")
                .withApplicant(CourtApplicationParty.courtApplicationParty()
                        .withMasterDefendant(MasterDefendant.masterDefendant()
                                .withDefendantCase(asList(defendantCase))
                                .withPersonDefendant(PersonDefendant.personDefendant().withPersonDetails(
                                Person.person()
                                        .withFirstName("John")
                                        .withLastName("Edward")
                                        .withDateOfBirth(LocalDate.of(1998, 8, 10))
                                        .withContact(
                                                ContactNumber.contactNumber()
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

        when(applicationParameters.getStatDecSendAppointmentLetterTemplateId()).thenReturn("9b4421bc-687a-43e2-8397-10c21935900a");

        when(statDecLetterService.generateAppointmentLetterDocument(envelope,hearingDateTime, courtApplication,
                courtCentre, courtApplication.getApplicant(), JurisdictionType.MAGISTRATES, STAT_DEC_VIRTUAL_HEARING))
                .thenReturn(materialId);

        when(referenceDataService.getDocumentTypeAccessData(any(), any(), any())).thenReturn(ofNullable(createObjectBuilder().add("section","Orders, Notices & Directions").build()));
        when(objectToJsonObjectConverter.convert(any())).thenReturn(createObjectBuilder().build());

        statDecNotificationService.sendNotification(envelope, notificationId, courtApplication, courtCentre, hearingDateTime, JurisdictionType.MAGISTRATES, STAT_DEC_VIRTUAL_HEARING);

        verify(sender).send(any());
        verify(notificationService,times(1)).sendLetter(Mockito.eq(envelope), Mockito.eq(notificationId), Mockito.eq(null), Mockito.eq(applicationId), Mockito.eq(materialId), Mockito.eq(true));

    }


    @Test
    public void testSendNotificationWhenEmailIsPresentForApplicant() {

        final DefendantCase defendantCase = DefendantCase.defendantCase()
                .withCaseId(randomUUID())
                .withDefendantId(randomUUID()).build();

        final CourtApplication courtApplication = CourtApplication.courtApplication()
                .withId(applicationId)
                .withType(CourtApplicationType.courtApplicationType().build())
                .withApplicationReference("05PP1000915-01")
                .withApplicant(CourtApplicationParty.courtApplicationParty()
                        .withMasterDefendant(MasterDefendant.masterDefendant()
                                .withDefendantCase(asList(defendantCase))
                                .withPersonDefendant(PersonDefendant.personDefendant().withPersonDetails(
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

        when(applicationParameters.getStatDecSendAppointmentLetterTemplateId()).thenReturn("9b4421bc-687a-43e2-8397-10c21935900a");

        when(materialUrlGenerator.pdfFileStreamUrlFor(any())).thenReturn("http://localhost:8080/material-query-api/query/api/rest/material/material/9b4421bc-687a-43e1-8397-10c21935900a");

        when(statDecLetterService.generateAppointmentLetterDocument(envelope, hearingDateTime, courtApplication,
                courtCentre, courtApplication.getApplicant(), JurisdictionType.MAGISTRATES, STAT_DEC_VIRTUAL_HEARING))
                .thenReturn(materialId);

        when(referenceDataService.getDocumentTypeAccessData(any(), any(), any())).thenReturn(ofNullable(createObjectBuilder().add("section","Orders, Notices & Directions").build()));
        when(objectToJsonObjectConverter.convert(any())).thenReturn(createObjectBuilder().build());


        statDecNotificationService.sendNotification(envelope, notificationId, courtApplication, courtCentre, hearingDateTime, JurisdictionType.MAGISTRATES, STAT_DEC_VIRTUAL_HEARING);

        verify(sender).send(any());

        verify(notificationService, times(1)).sendEmail(
                envelopeArgumentCaptor.capture(),notificationIdCaptor.capture(),
                caseIdCaptor.capture(), applicationIdCaptor.capture(),
                materialIdCaptor.capture(), emailNotificationsCaptor.capture());

        assertThat(caseIdCaptor.getValue(), Matchers.nullValue());
        assertThat(materialIdCaptor.getValue(), is(materialId));
        assertThat(applicationIdCaptor.getValue(), is(applicationId));

        assertThat(emailNotificationsCaptor.getValue(), notNullValue());
        assertThat(emailNotificationsCaptor.getValue().size(), is(1));
        assertThat(emailNotificationsCaptor.getValue().get(0).getSendToAddress(), is("applicant@test.com"));
        assertThat(emailNotificationsCaptor.getValue().get(0).getTemplateId().toString(), is("9b4421bc-687a-43e2-8397-10c21935900a"));

    }



}
