package uk.gov.moj.cpp.progression.helper;

import static java.util.UUID.fromString;
import static java.util.UUID.randomUUID;
import static javax.json.Json.createObjectBuilder;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.justice.services.test.utils.core.reflection.ReflectionUtil.setField;
import static uk.gov.moj.cpp.progression.helper.HearingNotificationHelper.HEARING_DATE_PATTERN;

import uk.gov.justice.core.courts.Address;
import uk.gov.justice.core.courts.CourtCentre;
import uk.gov.justice.core.courts.LjaDetails;
import uk.gov.justice.core.courts.notification.EmailChannel;
import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.common.converter.ObjectToJsonObjectConverter;
import uk.gov.justice.services.common.converter.StringToJsonObjectConverter;
import uk.gov.justice.services.common.converter.jackson.ObjectMapperProducer;
import uk.gov.justice.services.core.requester.Requester;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.test.utils.core.messaging.MetadataBuilderFactory;
import uk.gov.moj.cpp.material.url.MaterialUrlGenerator;
import uk.gov.moj.cpp.progression.eventprocessorstore.persistence.repository.NotificationInfoJdbcRepository;
import uk.gov.moj.cpp.progression.service.ApplicationParameters;
import uk.gov.moj.cpp.progression.service.DefenceService;
import uk.gov.moj.cpp.progression.service.DocumentGeneratorService;
import uk.gov.moj.cpp.progression.service.ListingService;
import uk.gov.moj.cpp.progression.service.NotificationService;
import uk.gov.moj.cpp.progression.service.ProgressionService;
import uk.gov.moj.cpp.progression.service.RefDataService;
import uk.gov.moj.cpp.progression.service.dto.HearingNotificationInputData;
import uk.gov.moj.cpp.progression.service.payloads.AssociatedDefenceOrganisation;
import uk.gov.moj.cpp.progression.service.payloads.DefenceOrganisationAddress;
import uk.gov.moj.cpp.progression.transformer.ListCourtHearingTransformer;
import uk.gov.moj.cpp.progression.utils.FileUtil;

import java.nio.charset.Charset;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import javax.json.Json;
import javax.json.JsonObject;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableMap;
import com.google.common.io.Resources;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

@ExtendWith(MockitoExtension.class)
public class HearingNotificationHelperTest {

    public static final String TEMPLATE_ID = "e4648583-eb0f-438e-aab5-5eff29f3f7b4";
    private static final String TEMPLATE_NAME = "NewHearingNotificationTemplate";
    private static final String HEARING_TYPE = "Plea";
    private static final String HEARING_NOTIFICATION_DATE = "hearing_notification_date";

    @Mock
    private ProgressionService progressionService;

    @Mock
    private ListingService listingService;

    @Mock
    private DocumentGeneratorService documentGeneratorService;

    @Mock
    private NotificationService notificationService;

    @Mock
    private RefDataService refDataService;

    @Mock
    private DefenceService defenceService;

    @Mock
    private ListCourtHearingTransformer listCourtHearingTransformer;

    @Mock
    private MaterialUrlGenerator materialUrlGenerator;

    @Mock
    private ApplicationParameters applicationParameters;

    private ObjectMapper objectMapper = new ObjectMapperProducer().objectMapper();

    @Spy
    private JsonObjectToObjectConverter jsonObjectToObjectConverter = new JsonObjectToObjectConverter(objectMapper);

    @Spy
    private ObjectToJsonObjectConverter objectToJsonObjectConverter = new ObjectToJsonObjectConverter(objectMapper);

    @Spy
    private StringToJsonObjectConverter stringToJsonObjectConverter;

    @Mock
    private Sender sender;

    @Mock
    private Requester requester;

    @Mock
    private NotificationInfoJdbcRepository notificationInfoRepository;

    @Captor
    private ArgumentCaptor<List<EmailChannel>> prosecutorEmailCapture;

    @Captor
    private ArgumentCaptor<List<EmailChannel>> defendantEmailCapture;

    @Captor
    private ArgumentCaptor<UUID> caseIdCapture;

    @Captor
    private ArgumentCaptor<JsonObject> docPayloadCaptor;

    @InjectMocks
    private HearingNotificationHelper hearingNotificationHelper;

    private JsonEnvelope jsonEnvelope;

    private UUID caseId;
    private UUID defendantId;
    private UUID hearingId;
    private UUID offenceId1;
    private UUID offenceId2;
    private CourtCentre enrichedCourtCenter;

    @BeforeEach
    void initMocks() {
        caseId = randomUUID();
        defendantId = randomUUID();
        hearingId = randomUUID();
        offenceId1 = randomUUID();
        offenceId2 = randomUUID();

        final Address address = Address.address()
                .withAddress1("testAddress1")
                .withAddress2("testAddress2")
                .withAddress3("address3")
                .withAddress4("address4")
                .withAddress5("address5")
                .withPostcode("sl6 1nb")
                .build();
        final Address addressWelsh = Address.address()
                .withWelshAddress1("Y Llysoedd Barn")
                .withWelshAddress2("Yr Wyddgrug")
                .withWelshAddress3("Sir y Fflint")
                .withPostcode("sl6 1nb")
                .build();
        final LjaDetails ljaDetails = LjaDetails.ljaDetails()
                .withLjaCode("testLja")
                .withWelshLjaName("testWalesLja")
                .withLjaName("ljaName")
                .build();
        enrichedCourtCenter = CourtCentre.courtCentre()
                .withCourtHearingLocation("Burmimgham")
                .withId(randomUUID())
                .withLja((ljaDetails)).withName("Lavender Court")
                .withAddress(address)
                .withWelshCourtCentre(false)
                .withWelshAddress(addressWelsh)
                .withWelshRoomName("Ystafell Llys 1")
                .withWelshName("Llys y Goron yr Wyddgrug")
                .build();
        when(progressionService.transformCourtCentreV2(any(), any())).thenReturn(enrichedCourtCenter);
        setField(this.hearingNotificationHelper, "jsonObjectToObjectConverter", jsonObjectToObjectConverter);
        setField(this.hearingNotificationHelper, "objectToJsonObjectConverter", objectToJsonObjectConverter);
        when(applicationParameters.getNotifyHearingTemplateId()).thenReturn(TEMPLATE_ID);
        jsonEnvelope = envelopeFrom(
                MetadataBuilderFactory.metadataWithRandomUUID("progression.event.list-hearing-requested"),
                objectToJsonObjectConverter.convert(Json.createObjectBuilder().build()));

    }

    @Test
    void sendHearingNotifications_EmailToAllRelevantParties() {

        final UUID caseId = randomUUID();
        final UUID defendantId = randomUUID();
        final UUID hearingId = randomUUID();
        final ZonedDateTime hearingTime = ZonedDateTime.now().plusDays(5);
        HearingNotificationInputData inputData = getInputData(caseId, defendantId, TEMPLATE_NAME, hearingId, hearingTime);

        final JsonObject prosecutionCase = FileUtil.jsonFromString(FileUtil.getPayload("progressioncase.json")
                .replaceAll("%CASE_ID%", caseId.toString())
                .replaceAll("OFFENCE_ID_1", offenceId1.toString())
                .replaceAll("OFFENCE_ID_2", offenceId2.toString())
                .replaceAll("%DEFENDANT_ID%", defendantId.toString()));
        when(progressionService.transformCourtCentreV2(any(), any())).thenReturn(CourtCentre.courtCentre().withValuesFrom(enrichedCourtCenter).withWelshCourtCentre(true).build());
        when(progressionService.getProsecutionCaseDetailById(any(), any())).thenReturn(Optional.of(createObjectBuilder().
                add("prosecutionCase", prosecutionCase)
                .build()
        ));

        when(refDataService.getProsecutor(any(), any(), any())).thenReturn(Optional.of(getPayload("prosecutor.json")));
        AssociatedDefenceOrganisation associatedDefenceOrganisation = AssociatedDefenceOrganisation.associatedDefenceOrganisationBuilder()
                .withOrganisationId(randomUUID())
                .withAddress(DefenceOrganisationAddress.defenceOrganisationAddressBuilder()
                        .withAddress1("addressLine1")
                        .withAddress2("addressLine2")
                        .withAddress3("addressLine3")
                        .withAddress4("addressLine4")
                        .withAddressPostcode("CR01JS")
                        .build())
                .withEmail("organisation@org.com")
                .withOrganisationName("defence Organisation")
                .build();
        when(defenceService.getDefenceOrganisationByDefendantId(any(), any())).thenReturn(associatedDefenceOrganisation);

        hearingNotificationHelper.sendHearingNotificationsToRelevantParties(jsonEnvelope, inputData);

        verify(notificationService, times(2)).sendEmail(any(), any(), any(), any(), any(), prosecutorEmailCapture.capture());
        verify(documentGeneratorService, times(2)).generateNonNowDocument(any(), any(), any(), any(), any());

    }

    @Test
    void sendHearingNotifications_EmailToAllRelevantParties_WhenCivilCaseExparteFalse() {

        final UUID caseId = randomUUID();
        final UUID defendantId = randomUUID();
        final UUID hearingId = randomUUID();
        final ZonedDateTime hearingTime = ZonedDateTime.now().plusDays(5);
        HearingNotificationInputData inputData = getInputData(caseId, defendantId, TEMPLATE_NAME, hearingId, hearingTime);

        final JsonObject prosecutionCase = FileUtil.jsonFromString(FileUtil.getPayload("progressioncase-civil-exparte-false.json")
                .replaceAll("%CASE_ID%", caseId.toString())
                .replaceAll("OFFENCE_ID_1", offenceId1.toString())
                .replaceAll("OFFENCE_ID_2", offenceId2.toString())
                .replaceAll("%DEFENDANT_ID%", defendantId.toString()));

        when(progressionService.getProsecutionCaseDetailById(any(), any())).thenReturn(Optional.of(createObjectBuilder().
                add("prosecutionCase", prosecutionCase)
                .build()
        ));

        when(refDataService.getProsecutor(any(), any(), any())).thenReturn(Optional.of(getPayload("prosecutor.json")));
        AssociatedDefenceOrganisation associatedDefenceOrganisation = AssociatedDefenceOrganisation.associatedDefenceOrganisationBuilder()
                .withOrganisationId(randomUUID())
                .withAddress(DefenceOrganisationAddress.defenceOrganisationAddressBuilder()
                        .withAddress1("addressLine1")
                        .withAddress2("addressLine2")
                        .withAddress3("addressLine3")
                        .withAddress4("addressLine4")
                        .withAddressPostcode("CR01JS")
                        .build())
                .withEmail("organisation@org.com")
                .withOrganisationName("defence Organisation")
                .build();
        when(defenceService.getDefenceOrganisationByDefendantId(any(), any())).thenReturn(associatedDefenceOrganisation);

        hearingNotificationHelper.sendHearingNotificationsToRelevantParties(jsonEnvelope, inputData);

        verify(notificationService, times(2)).sendEmail(any(), any(), any(), any(), any(), prosecutorEmailCapture.capture());
        verify(documentGeneratorService, times(2)).generateNonNowDocument(any(), any(), any(), any(), any());

    }

    @Test
    void shouldNotSendHearingNotifications_NoNotificationSentToAllRelevantParties_WhenCivilCaseExparteTrue() {

        final UUID caseId = randomUUID();
        final UUID defendantId = randomUUID();
        final UUID hearingId = randomUUID();
        final ZonedDateTime hearingTime = ZonedDateTime.now().plusDays(5);
        HearingNotificationInputData inputData = getInputData(caseId, defendantId, TEMPLATE_NAME, hearingId, hearingTime);

        final JsonObject prosecutionCase = FileUtil.jsonFromString(FileUtil.getPayload("progressioncase-civil-exparte-true.json")
                .replaceAll("%CASE_ID%", caseId.toString())
                .replaceAll("OFFENCE_ID_1", offenceId1.toString())
                .replaceAll("OFFENCE_ID_2", offenceId2.toString())
                .replaceAll("%DEFENDANT_ID%", defendantId.toString()));

        when(progressionService.getProsecutionCaseDetailById(any(), any())).thenReturn(Optional.of(createObjectBuilder().
                add("prosecutionCase", prosecutionCase)
                .build()
        ));

        when(refDataService.getProsecutor(any(), any(), any())).thenReturn(Optional.of(getPayload("prosecutor.json")));
        AssociatedDefenceOrganisation associatedDefenceOrganisation = AssociatedDefenceOrganisation.associatedDefenceOrganisationBuilder()
                .withOrganisationId(randomUUID())
                .withAddress(DefenceOrganisationAddress.defenceOrganisationAddressBuilder()
                        .withAddress1("addressLine1")
                        .withAddress2("addressLine2")
                        .withAddress3("addressLine3")
                        .withAddress4("addressLine4")
                        .withAddressPostcode("CR01JS")
                        .build())
                .withEmail("organisation@org.com")
                .withOrganisationName("defence Organisation")
                .build();
        when(defenceService.getDefenceOrganisationByDefendantId(any(), any())).thenReturn(associatedDefenceOrganisation);

        hearingNotificationHelper.sendHearingNotificationsToRelevantParties(jsonEnvelope, inputData);

        verifyNoInteractions(notificationService);
        verify(documentGeneratorService, times(2)).generateNonNowDocument(any(), any(), any(), any(), any());

    }


    @Test
    void sendHearingNotifications_LetterToAllRelevantParties() {

        final UUID caseId = randomUUID();
        final UUID defendantId = randomUUID();
        final UUID hearingId = randomUUID();
        final ZonedDateTime hearingTime = ZonedDateTime.now().plusDays(5);
        HearingNotificationInputData inputData = getInputData(caseId, defendantId, TEMPLATE_NAME, hearingId, hearingTime);

        final JsonObject prosecutionCase = FileUtil.jsonFromString(FileUtil.getPayload("progressioncase.json")
                .replaceAll("%CASE_ID%", caseId.toString())
                .replaceAll("OFFENCE_ID_1", offenceId1.toString())
                .replaceAll("OFFENCE_ID_2", offenceId2.toString())
                .replaceAll("%DEFENDANT_ID%", defendantId.toString()));

        when(progressionService.getProsecutionCaseDetailById(any(), any())).thenReturn(Optional.of(createObjectBuilder().
                add("prosecutionCase", prosecutionCase)
                .build()
        ));

        when(refDataService.getProsecutor(any(), any(), any())).thenReturn(Optional.of(getPayload("prosecutor-with-no-email.json")));
        AssociatedDefenceOrganisation associatedDefenceOrganisation = AssociatedDefenceOrganisation.associatedDefenceOrganisationBuilder()
                .withOrganisationId(randomUUID())
                .withAddress(DefenceOrganisationAddress.defenceOrganisationAddressBuilder()
                        .withAddress1("addressLine1")
                        .withAddress2("addressLine2")
                        .withAddress3("addressLine3")
                        .withAddress4("addressLine4")
                        .withAddressPostcode("CR01JS")
                        .build())
                .withOrganisationName("defence Organisation")
                .build();
        when(defenceService.getDefenceOrganisationByDefendantId(any(), any())).thenReturn(associatedDefenceOrganisation);

        hearingNotificationHelper.sendHearingNotificationsToRelevantParties(jsonEnvelope, inputData);

        verify(notificationService, never()).sendEmail(any(), any(), any(), any(), any(), prosecutorEmailCapture.capture());
        verify(notificationService, times(2)).sendLetter(any(), any(), any(), any(), any(), anyBoolean());

    }

    @Test
    void sendHearingNotifications_LetterToDefendantOrganisation_EmailToProsecutor() {

        final UUID caseId = randomUUID();
        final UUID defendantId = randomUUID();
        final UUID hearingId = randomUUID();
        final ZonedDateTime hearingTime = ZonedDateTime.now().plusDays(5);
        HearingNotificationInputData inputData = getInputData(caseId, defendantId, TEMPLATE_NAME, hearingId, hearingTime);

        final JsonObject prosecutionCase = FileUtil.jsonFromString(FileUtil.getPayload("progressioncase.json")
                .replaceAll("%CASE_ID%", caseId.toString())
                .replaceAll("OFFENCE_ID_1", offenceId1.toString())
                .replaceAll("OFFENCE_ID_2", offenceId2.toString())
                .replaceAll("%DEFENDANT_ID%", defendantId.toString()));

        when(progressionService.getProsecutionCaseDetailById(any(), any())).thenReturn(Optional.of(createObjectBuilder().
                add("prosecutionCase", prosecutionCase)
                .build()
        ));

        when(refDataService.getProsecutor(any(), any(), any())).thenReturn(Optional.of(getPayload("prosecutor.json")));
        AssociatedDefenceOrganisation associatedDefenceOrganisation = AssociatedDefenceOrganisation.associatedDefenceOrganisationBuilder()
                .withOrganisationId(randomUUID())
                .withAddress(DefenceOrganisationAddress.defenceOrganisationAddressBuilder()
                        .withAddress1("addressLine1")
                        .withAddress2("addressLine2")
                        .withAddress3("addressLine3")
                        .withAddress4("addressLine4")
                        .withAddressPostcode("CR01JS")
                        .build())
                .withOrganisationName("defence Organisation")
                .build();
        when(defenceService.getDefenceOrganisationByDefendantId(any(), any())).thenReturn(associatedDefenceOrganisation);

        hearingNotificationHelper.sendHearingNotificationsToRelevantParties(jsonEnvelope, inputData);

        verify(notificationService, times(1)).sendEmail(any(), any(), any(), any(), any(), prosecutorEmailCapture.capture());
        final List<EmailChannel> emailChannels = prosecutorEmailCapture.getValue();
        verifyEmailChannel(emailChannels, "Crown.Court.Results@merseyside.police.uk", fromString(TEMPLATE_ID));
        verify(notificationService, times(1)).sendLetter(any(), any(), caseIdCapture.capture(), any(), any(), anyBoolean());
        assertThat(caseIdCapture.getValue(), is(caseId));

    }

    @Test
    void sendHearingNotifications_LetterToPersonDefendant_EmailToProsecutor() {

        final UUID caseId = randomUUID();
        final UUID defendantId = randomUUID();
        final UUID hearingId = randomUUID();
        final ZonedDateTime hearingTime = ZonedDateTime.now().plusDays(5);
        HearingNotificationInputData inputData = getInputData(caseId, defendantId, TEMPLATE_NAME, hearingId, hearingTime);

        final JsonObject prosecutionCase = FileUtil.jsonFromString(FileUtil.getPayload("progressioncase-no-defendant-email.json")
                .replaceAll("%CASE_ID%", caseId.toString())
                .replaceAll("%DEFENDANT_ID%", defendantId.toString()));

        when(progressionService.getProsecutionCaseDetailById(any(), any())).thenReturn(Optional.of(createObjectBuilder().
                add("prosecutionCase", prosecutionCase)
                .build()
        ));

        when(refDataService.getProsecutor(any(), any(), any())).thenReturn(Optional.of(getPayload("prosecutor.json")));
        AssociatedDefenceOrganisation associatedDefenceOrganisation = null;
        when(defenceService.getDefenceOrganisationByDefendantId(any(), any())).thenReturn(associatedDefenceOrganisation);

        hearingNotificationHelper.sendHearingNotificationsToRelevantParties(jsonEnvelope, inputData);

        verify(notificationService, times(1)).sendEmail(any(), any(), any(), any(), any(), prosecutorEmailCapture.capture());
        final List<EmailChannel> emailChannels = prosecutorEmailCapture.getValue();
        verifyEmailChannel(emailChannels, "Crown.Court.Results@merseyside.police.uk", fromString(TEMPLATE_ID));
        verify(notificationService, times(1)).sendLetter(any(), any(), caseIdCapture.capture(), any(), any(), anyBoolean());
        assertThat(caseIdCapture.getValue(), is(caseId));

    }

    @Test
    void sendHearingNotifications_EmailToPersonDefendant_LetterToProsecutor() {

        final UUID caseId = randomUUID();
        final UUID defendantId = randomUUID();
        final UUID hearingId = randomUUID();
        final ZonedDateTime hearingTime = ZonedDateTime.now().plusDays(5);
        HearingNotificationInputData inputData = getInputData(caseId, defendantId, TEMPLATE_NAME, hearingId, hearingTime);

        final JsonObject prosecutionCase = FileUtil.jsonFromString(FileUtil.getPayload("progressioncase.json")
                .replaceAll("%CASE_ID%", caseId.toString())
                .replaceAll("OFFENCE_ID_1", offenceId1.toString())
                .replaceAll("OFFENCE_ID_2", offenceId2.toString())
                .replaceAll("%DEFENDANT_ID%", defendantId.toString()));

        when(progressionService.getProsecutionCaseDetailById(any(), any())).thenReturn(Optional.of(createObjectBuilder().
                add("prosecutionCase", prosecutionCase)
                .build()
        ));

        when(refDataService.getProsecutor(any(), any(), any())).thenReturn(Optional.of(getPayload("prosecutor-with-no-email.json")));
        AssociatedDefenceOrganisation associatedDefenceOrganisation = null;
        when(defenceService.getDefenceOrganisationByDefendantId(any(), any())).thenReturn(associatedDefenceOrganisation);


        hearingNotificationHelper.sendHearingNotificationsToRelevantParties(jsonEnvelope, inputData);

        verify(notificationService, times(1)).sendEmail(any(), any(), any(), any(), any(), defendantEmailCapture.capture());
        final List<EmailChannel> emailChannels = defendantEmailCapture.getValue();
        verifyEmailChannel(emailChannels, "defendant_email@email.com", fromString(TEMPLATE_ID));
        verify(notificationService, times(1)).sendLetter(any(), any(), caseIdCapture.capture(), any(), any(), anyBoolean());
        assertThat(caseIdCapture.getValue(), is(caseId));

    }

    @Test
    void sendHearingNotifications_LetterToOrganisationDefendant_EmailToProsecutor() {

        final UUID caseId = randomUUID();
        final UUID defendantId = randomUUID();
        final UUID hearingId = randomUUID();
        final ZonedDateTime hearingTime = ZonedDateTime.now().plusDays(5);
        HearingNotificationInputData inputData = getInputData(caseId, defendantId, TEMPLATE_NAME, hearingId, hearingTime);

        final JsonObject prosecutionCase = FileUtil.jsonFromString(FileUtil.getPayload("progressioncase-no-orgdefendant-email.json")
                .replaceAll("%CASE_ID%", caseId.toString())
                .replaceAll("%DEFENDANT_ID%", defendantId.toString()));

        when(progressionService.getProsecutionCaseDetailById(any(), any())).thenReturn(Optional.of(createObjectBuilder().
                add("prosecutionCase", prosecutionCase)
                .build()
        ));

        when(refDataService.getProsecutor(any(), any(), any())).thenReturn(Optional.of(getPayload("prosecutor.json")));
        AssociatedDefenceOrganisation associatedDefenceOrganisation = null;
        when(defenceService.getDefenceOrganisationByDefendantId(any(), any())).thenReturn(associatedDefenceOrganisation);


        hearingNotificationHelper.sendHearingNotificationsToRelevantParties(jsonEnvelope, inputData);

        verify(notificationService, times(1)).sendEmail(any(), any(), any(), any(), any(), prosecutorEmailCapture.capture());
        final List<EmailChannel> emailChannels = prosecutorEmailCapture.getValue();
        verifyEmailChannel(emailChannels, "Crown.Court.Results@merseyside.police.uk", fromString(TEMPLATE_ID));
        verify(notificationService, times(1)).sendLetter(any(), any(), caseIdCapture.capture(), any(), any(), anyBoolean());
        assertThat(caseIdCapture.getValue(), is(caseId));

    }

    @Test
    void sendHearingNotifications_EmailToOrganisationDefendant_LetterToProsecutor() {

        final UUID caseId = randomUUID();
        final UUID defendantId = randomUUID();
        final UUID hearingId = randomUUID();
        final ZonedDateTime hearingTime = ZonedDateTime.now().plusDays(5);
        HearingNotificationInputData inputData = getInputData(caseId, defendantId, TEMPLATE_NAME, hearingId, hearingTime);

        final JsonObject prosecutionCase = FileUtil.jsonFromString(FileUtil.getPayload("progressioncase-with-orgdefendant-email.json")
                .replaceAll("%CASE_ID%", caseId.toString())
                .replaceAll("%DEFENDANT_ID%", defendantId.toString()));

        when(progressionService.getProsecutionCaseDetailById(any(), any())).thenReturn(Optional.of(createObjectBuilder().
                add("prosecutionCase", prosecutionCase)
                .build()
        ));

        when(refDataService.getProsecutor(any(), any(), any())).thenReturn(Optional.of(getPayload("prosecutor-with-no-email.json")));
        AssociatedDefenceOrganisation associatedDefenceOrganisation = null;
        when(defenceService.getDefenceOrganisationByDefendantId(any(), any())).thenReturn(associatedDefenceOrganisation);


        hearingNotificationHelper.sendHearingNotificationsToRelevantParties(jsonEnvelope, inputData);

        verify(notificationService, times(1)).sendEmail(any(), any(), any(), any(), any(), defendantEmailCapture.capture());
        final List<EmailChannel> emailChannels = defendantEmailCapture.getValue();
        verifyEmailChannel(emailChannels, "orgdefendantemail@email.com", fromString(TEMPLATE_ID));
        verify(notificationService, times(1)).sendLetter(any(), any(), caseIdCapture.capture(), any(), any(), anyBoolean());
        assertThat(caseIdCapture.getValue(), is(caseId));

    }

    @Test
    void sendHearingNotifications_EmailToDefendantOrganisation_LetterToProsecutor() {

        final UUID caseId = randomUUID();
        final UUID defendantId = randomUUID();
        final UUID hearingId = randomUUID();
        final ZonedDateTime hearingTime = ZonedDateTime.now().plusDays(5);
        HearingNotificationInputData inputData = getInputData(caseId, defendantId, TEMPLATE_NAME, hearingId, hearingTime);

        final JsonObject prosecutionCase = FileUtil.jsonFromString(FileUtil.getPayload("progressioncase.json")
                .replaceAll("%CASE_ID%", caseId.toString())
                .replaceAll("OFFENCE_ID_1", offenceId1.toString())
                .replaceAll("OFFENCE_ID_2", offenceId2.toString())
                .replaceAll("%DEFENDANT_ID%", defendantId.toString()));

        when(progressionService.getProsecutionCaseDetailById(any(), any())).thenReturn(Optional.of(createObjectBuilder().
                add("prosecutionCase", prosecutionCase)
                .build()
        ));

        when(refDataService.getProsecutor(any(), any(), any())).thenReturn(Optional.of(getPayload("prosecutor-with-no-email.json")));
        AssociatedDefenceOrganisation associatedDefenceOrganisation = AssociatedDefenceOrganisation.associatedDefenceOrganisationBuilder()
                .withOrganisationId(randomUUID())
                .withAddress(DefenceOrganisationAddress.defenceOrganisationAddressBuilder()
                        .withAddress1("addressLine1")
                        .withAddress2("addressLine2")
                        .withAddress3("addressLine3")
                        .withAddress4("addressLine4")
                        .withAddressPostcode("CR01JS")
                        .build())
                .withEmail("organisation@org.com")
                .withOrganisationName("defence Organisation")
                .build();
        when(defenceService.getDefenceOrganisationByDefendantId(any(), any())).thenReturn(associatedDefenceOrganisation);


        hearingNotificationHelper.sendHearingNotificationsToRelevantParties(jsonEnvelope, inputData);

        verify(notificationService, times(1)).sendEmail(any(), any(), any(), any(), any(), defendantEmailCapture.capture());
        final List<EmailChannel> emailChannels = defendantEmailCapture.getValue();
        verifyEmailChannel(emailChannels, "organisation@org.com", fromString(TEMPLATE_ID));
        verify(notificationService, times(1)).sendLetter(any(), any(), caseIdCapture.capture(), any(), any(), anyBoolean());
        assertThat(caseIdCapture.getValue(), is(caseId));

    }

    @Test
    void sendHearingNotifications_EmailToDefendantOrganisation_NoNotificationToCpsProsecutor() {

        final UUID caseId = randomUUID();
        final UUID defendantId = randomUUID();
        final UUID hearingId = randomUUID();
        final ZonedDateTime hearingTime = ZonedDateTime.now().plusDays(5);
        HearingNotificationInputData inputData = getInputData(caseId, defendantId, TEMPLATE_NAME, hearingId, hearingTime);

        final JsonObject prosecutionCase = FileUtil.jsonFromString(FileUtil.getPayload("progressioncase.json")
                .replaceAll("%CASE_ID%", caseId.toString())
                .replaceAll("OFFENCE_ID_1", offenceId1.toString())
                .replaceAll("OFFENCE_ID_2", offenceId2.toString())
                .replaceAll("%DEFENDANT_ID%", defendantId.toString()));

        when(progressionService.getProsecutionCaseDetailById(any(), any())).thenReturn(Optional.of(createObjectBuilder().
                add("prosecutionCase", prosecutionCase)
                .build()
        ));

        when(refDataService.getProsecutor(any(), any(), any())).thenReturn(Optional.of(getPayload("prosecutor-with-no-email-and-cpsFlag-on.json")));
        AssociatedDefenceOrganisation associatedDefenceOrganisation = AssociatedDefenceOrganisation.associatedDefenceOrganisationBuilder()
                .withOrganisationId(randomUUID())
                .withAddress(DefenceOrganisationAddress.defenceOrganisationAddressBuilder()
                        .withAddress1("addressLine1")
                        .withAddress2("addressLine2")
                        .withAddress3("addressLine3")
                        .withAddress4("addressLine4")
                        .withAddressPostcode("CR01JS")
                        .build())
                .withEmail("organisation@org.com")
                .withOrganisationName("defence Organisation")
                .build();
        when(defenceService.getDefenceOrganisationByDefendantId(any(), any())).thenReturn(associatedDefenceOrganisation);


        hearingNotificationHelper.sendHearingNotificationsToRelevantParties(jsonEnvelope, inputData);

        verify(notificationService, times(1)).sendEmail(any(), any(), any(), any(), any(), defendantEmailCapture.capture());
        final List<EmailChannel> emailChannels = defendantEmailCapture.getValue();
        verifyEmailChannel(emailChannels, "organisation@org.com", fromString(TEMPLATE_ID));
        verify(notificationService, never()).sendLetter(any(), any(), caseIdCapture.capture(), any(), any(), anyBoolean());
    }

    @Test
    void sendHearingNotifications_LetterToDefendantOrganisation_NoNotificationToCpsProsecutor() {

        final UUID caseId = randomUUID();
        final UUID defendantId = randomUUID();
        final UUID hearingId = randomUUID();
        final ZonedDateTime hearingTime = ZonedDateTime.now().plusDays(5);
        HearingNotificationInputData inputData = getInputData(caseId, defendantId, TEMPLATE_NAME, hearingId, hearingTime);

        final JsonObject prosecutionCase = FileUtil.jsonFromString(FileUtil.getPayload("progressioncase.json")
                .replaceAll("%CASE_ID%", caseId.toString())
                .replaceAll("OFFENCE_ID_1", offenceId1.toString())
                .replaceAll("OFFENCE_ID_2", offenceId2.toString())
                .replaceAll("%DEFENDANT_ID%", defendantId.toString()));

        when(progressionService.getProsecutionCaseDetailById(any(), any())).thenReturn(Optional.of(createObjectBuilder().
                add("prosecutionCase", prosecutionCase)
                .build()
        ));

        when(refDataService.getProsecutor(any(), any(), any())).thenReturn(Optional.of(getPayload("prosecutor-with-no-email-and-cpsFlag-on.json")));
        AssociatedDefenceOrganisation associatedDefenceOrganisation = AssociatedDefenceOrganisation.associatedDefenceOrganisationBuilder()
                .withOrganisationId(randomUUID())
                .withAddress(DefenceOrganisationAddress.defenceOrganisationAddressBuilder()
                        .withAddress1("addressLine1")
                        .withAddress2("addressLine2")
                        .withAddress3("addressLine3")
                        .withAddress4("addressLine4")
                        .withAddressPostcode("CR01JS")
                        .build())
                .withOrganisationName("defence Organisation")
                .build();
        when(defenceService.getDefenceOrganisationByDefendantId(any(), any())).thenReturn(associatedDefenceOrganisation);


        hearingNotificationHelper.sendHearingNotificationsToRelevantParties(jsonEnvelope, inputData);

        verify(notificationService, never()).sendEmail(any(), any(), any(), any(), any(), prosecutorEmailCapture.capture());
        verify(notificationService, times(1)).sendLetter(any(), any(), caseIdCapture.capture(), any(), any(), anyBoolean());
        assertThat(caseIdCapture.getValue(), is(caseId));
    }

    private void verifyEmailChannel(final List<EmailChannel> emailChannels, final String sendToEmail, final UUID templateId) {
        assertThat(emailChannels, hasSize(1));
        final EmailChannel emailChannel = emailChannels.get(0);
        assertThat(emailChannel.getSendToAddress(), is(sendToEmail));
        assertThat(emailChannel.getTemplateId(), is(templateId));
        assertThat(emailChannel.getPersonalisation(), notNullValue());
        assertThat(emailChannel.getPersonalisation().getAdditionalProperties().containsKey(HEARING_NOTIFICATION_DATE),is(true));
    }

    public JsonObject getPayload(final String path) {
        String response = null;
        try {
            response = Resources.toString(
                    Resources.getResource(path),
                    Charset.defaultCharset()
            );
        } catch (final Exception e) {
            e.printStackTrace();
        }

        return new StringToJsonObjectConverter().convert(response);
    }

    private HearingNotificationInputData getInputData(final UUID caseId, final UUID defendantId, final String templateName, final UUID hearingId,
                                                      final ZonedDateTime hearingTime){
        final HearingNotificationInputData hearingNotificationInputData = new HearingNotificationInputData();
        hearingNotificationInputData.setHearingType(HEARING_TYPE);
        hearingNotificationInputData.setCaseIds(List.of(caseId));
        hearingNotificationInputData.setDefendantIds(List.of(defendantId));
        hearingNotificationInputData.setDefendantOffenceListMap(ImmutableMap.of(defendantId, List.of(offenceId1,offenceId2)));
        hearingNotificationInputData.setTemplateName(templateName);
        hearingNotificationInputData.setHearingId(hearingId);
        hearingNotificationInputData.setHearingDateTime(hearingTime);
        hearingNotificationInputData.setEmailNotificationTemplateId(fromString(applicationParameters.getNotifyHearingTemplateId()));
        hearingNotificationInputData.setCourtCenterId(randomUUID());
        hearingNotificationInputData.setCourtRoomId(randomUUID());
        return hearingNotificationInputData;
    }

    @Test
    void shouldConvertHearingTimeToUKTimeZone() {
        // Given - Create a hearing time in a different timezone (UTC+5)
        ZonedDateTime hearingDateTimeUTC = ZonedDateTime.of(2025, 8, 12, 14, 30, 0, 0, ZoneId.of("UTC"));
        ZonedDateTime hearingDateTimeUTCPlus5 = hearingDateTimeUTC.withZoneSameInstant(ZoneId.of("UTC+5"));

        // Expected UK time (UTC+1 during summer time)
        String expectedUKTime = LocalTime.of(15, 30).toString(); // 14:30 UTC + 1 hour = 15:30 UK time

        HearingNotificationInputData inputData = getInputData(caseId, defendantId, TEMPLATE_NAME, hearingId, hearingDateTimeUTCPlus5);

        final JsonObject prosecutionCase = FileUtil.jsonFromString(FileUtil.getPayload("progressioncase.json")
                .replaceAll("%CASE_ID%", caseId.toString())
                .replaceAll("OFFENCE_ID_1", offenceId1.toString())
                .replaceAll("OFFENCE_ID_2", offenceId2.toString())
                .replaceAll("%DEFENDANT_ID%", defendantId.toString()));

        when(progressionService.getProsecutionCaseDetailById(any(), any())).thenReturn(Optional.of(createObjectBuilder().
                add("prosecutionCase", prosecutionCase)
                .build()
        ));
        when(refDataService.getProsecutor(any(), any(), any())).thenReturn(Optional.of(getPayload("prosecutor.json")));
        when(defenceService.getDefenceOrganisationByDefendantId(any(), any())).thenReturn(null);


        // When
        hearingNotificationHelper.sendHearingNotificationsToRelevantParties(jsonEnvelope, inputData);

        // Then - Verify that the hearing time was converted to UK timezone
        // The method sends notifications to both defendant and prosecutor, so expect 2 calls
        verify(documentGeneratorService, times(2)).generateNonNowDocument(any(), docPayloadCaptor.capture(), any(), any(), any());
        assertEquals(expectedUKTime, docPayloadCaptor.getValue().getJsonObject("hearingCourtDetails").getString("hearingTime"));

    }

    @Test
    void shouldHandleHearingTimeInUKTimeZone() {
        // Given - Create a hearing time already in UK timezone
        ZonedDateTime hearingDateTimeUK = ZonedDateTime.of(2025, 8, 12, 15, 30, 0, 0, ZoneId.of("Europe/London"));
        String expectedUKTime = hearingDateTimeUK.getHour() + ":" + hearingDateTimeUK.getMinute();

        HearingNotificationInputData inputData = getInputData(caseId, defendantId, TEMPLATE_NAME, hearingId, hearingDateTimeUK);

        final JsonObject prosecutionCase = FileUtil.jsonFromString(FileUtil.getPayload("progressioncase.json")
                .replaceAll("%CASE_ID%", caseId.toString())
                .replaceAll("OFFENCE_ID_1", offenceId1.toString())
                .replaceAll("OFFENCE_ID_2", offenceId2.toString())
                .replaceAll("%DEFENDANT_ID%", defendantId.toString()));

        when(progressionService.getProsecutionCaseDetailById(any(), any())).thenReturn(Optional.of(createObjectBuilder().
                add("prosecutionCase", prosecutionCase)
                .build()
        ));
        when(refDataService.getProsecutor(any(), any(), any())).thenReturn(Optional.of(getPayload("prosecutor.json")));
        when(defenceService.getDefenceOrganisationByDefendantId(any(), any())).thenReturn(null);


        // When
        hearingNotificationHelper.sendHearingNotificationsToRelevantParties(jsonEnvelope, inputData);

        // Then - Verify processing continues normally
        // The method sends notifications to both defendant and prosecutor, so expect 2 calls
        verify(documentGeneratorService, times(2)).generateNonNowDocument(any(), docPayloadCaptor.capture(), any(), any(), any());
        assertEquals(expectedUKTime, docPayloadCaptor.getValue().getJsonObject("hearingCourtDetails").getString("hearingTime"));
    }


    @Test
    void shouldHandleHearingTimeAtMidnight() {
        // Given - Create a hearing time at midnight in UTC
        ZonedDateTime hearingDateTimeUTC = ZonedDateTime.of(2025, 8, 12, 0, 0, 0, 0, ZoneId.of("UTC"));

        // Expected UK time (UTC+1 during summer time)
        // 00:00 UTC = 01:00 UK time
        String expectedUKTime = LocalTime.of(1, 0).toString();

        HearingNotificationInputData inputData = getInputData(caseId, defendantId, TEMPLATE_NAME, hearingId, hearingDateTimeUTC);

        final JsonObject prosecutionCase = FileUtil.jsonFromString(FileUtil.getPayload("progressioncase.json")
                .replaceAll("%CASE_ID%", caseId.toString())
                .replaceAll("OFFENCE_ID_1", offenceId1.toString())
                .replaceAll("OFFENCE_ID_2", offenceId2.toString())
                .replaceAll("%DEFENDANT_ID%", defendantId.toString()));

        when(progressionService.getProsecutionCaseDetailById(any(), any())).thenReturn(Optional.of(createObjectBuilder().
                add("prosecutionCase", prosecutionCase)
                .build()
        ));
        when(refDataService.getProsecutor(any(), any(), any())).thenReturn(Optional.of(getPayload("prosecutor.json")));
        when(defenceService.getDefenceOrganisationByDefendantId(any(), any())).thenReturn(null);


        // When
        hearingNotificationHelper.sendHearingNotificationsToRelevantParties(jsonEnvelope, inputData);

        // Then - Verify processing continues with timezone conversion
        // The method sends notifications to both defendant and prosecutor, so expect 2 calls
        verify(documentGeneratorService, times(2)).generateNonNowDocument(any(), docPayloadCaptor.capture(), any(), any(), any());
        assertEquals(expectedUKTime, docPayloadCaptor.getValue().getJsonObject("hearingCourtDetails").getString("hearingTime"));
    }

    @Test
    void shouldHandleHearingTimeDuringDaylightSavingTransition() {
        // Given - Create a hearing time during daylight saving transition
        // March 30, 2025 - clocks go forward (UTC+0 to UTC+1)
        ZonedDateTime hearingDateTimeUTC = ZonedDateTime.of(2025, 3, 30, 2, 30, 0, 0, ZoneId.of("UTC"));
        final ZonedDateTime ukDateTime = hearingDateTimeUTC.withZoneSameInstant(ZoneId.of("Europe/London"));
        final String expectedUKTime = LocalTime.of(ukDateTime.getHour(), ukDateTime.getMinute()).toString();

        HearingNotificationInputData inputData = getInputData(caseId, defendantId, TEMPLATE_NAME, hearingId, hearingDateTimeUTC);

        final JsonObject prosecutionCase = FileUtil.jsonFromString(FileUtil.getPayload("progressioncase.json")
                .replaceAll("%CASE_ID%", caseId.toString())
                .replaceAll("OFFENCE_ID_1", offenceId1.toString())
                .replaceAll("OFFENCE_ID_2", offenceId2.toString())
                .replaceAll("%DEFENDANT_ID%", defendantId.toString()));

        when(progressionService.getProsecutionCaseDetailById(any(), any())).thenReturn(Optional.of(createObjectBuilder().
                add("prosecutionCase", prosecutionCase)
                .build()
        ));
        when(refDataService.getProsecutor(any(), any(), any())).thenReturn(Optional.of(getPayload("prosecutor.json")));
        when(defenceService.getDefenceOrganisationByDefendantId(any(), any())).thenReturn(null);


        // When
        hearingNotificationHelper.sendHearingNotificationsToRelevantParties(jsonEnvelope, inputData);

        // Then - Verify processing continues with timezone conversion
        // The method sends notifications to both defendant and prosecutor, so expect 2 calls
        verify(documentGeneratorService, times(2)).generateNonNowDocument(any(), docPayloadCaptor.capture(), any(), any(), any());
        assertEquals(expectedUKTime, docPayloadCaptor.getValue().getJsonObject("hearingCourtDetails").getString("hearingTime"));
    }

    @Test
    void shouldHandleHearingTimeDuringWinterTime() {
        // Given - Create a hearing time during winter time (UTC+0)
        // January 15, 2025 - UK is on UTC+0
        ZonedDateTime hearingDateTimeUTC = ZonedDateTime.of(2025, 1, 15, 14, 30, 0, 0, ZoneId.of("UTC"));

        // Expected UK time (UTC+0 during winter time)
        // 14:30 UTC = 14:30 UK time (no offset)
        String expectedUKTime = LocalTime.of(14, 30).toString();

        HearingNotificationInputData inputData = getInputData(caseId, defendantId, TEMPLATE_NAME, hearingId, hearingDateTimeUTC);

        final JsonObject prosecutionCase = FileUtil.jsonFromString(FileUtil.getPayload("progressioncase.json")
                .replaceAll("%CASE_ID%", caseId.toString())
                .replaceAll("OFFENCE_ID_1", offenceId1.toString())
                .replaceAll("OFFENCE_ID_2", offenceId2.toString())
                .replaceAll("%DEFENDANT_ID%", defendantId.toString()));

        when(progressionService.getProsecutionCaseDetailById(any(), any())).thenReturn(Optional.of(createObjectBuilder().
                add("prosecutionCase", prosecutionCase)
                .build()
        ));
        when(refDataService.getProsecutor(any(), any(), any())).thenReturn(Optional.of(getPayload("prosecutor.json")));
        when(defenceService.getDefenceOrganisationByDefendantId(any(), any())).thenReturn(null);

        // When
        hearingNotificationHelper.sendHearingNotificationsToRelevantParties(jsonEnvelope, inputData);

        // Then - Verify processing continues with timezone conversion
        // The method sends notifications to both defendant and prosecutor, so expect 2 calls
        verify(documentGeneratorService, times(2)).generateNonNowDocument(any(), docPayloadCaptor.capture(), any(), any(), any());
        assertEquals(expectedUKTime, docPayloadCaptor.getValue().getJsonObject("hearingCourtDetails").getString("hearingTime"));
    }

    @Test
    void sendHearingNotifications_VerifyUTCTimeConvertedToUKTimeZone() {
        final UUID caseId = randomUUID();
        final UUID defendantId = randomUUID();
        final UUID hearingId = randomUUID();

        // Given - Create a hearing time in a different timezone (UTC+5)
        ZonedDateTime hearingDateTimeUTC = ZonedDateTime.of(2025, 8, 12, 14, 30, 0, 0, ZoneId.of("UTC"));

        // Expected UK time (UTC+1 during summer time)
        String expectedUKTime = hearingDateTimeUTC.withZoneSameInstant(ZoneId.of("Europe/London")).format(DateTimeFormatter.ofPattern(HEARING_DATE_PATTERN));

        HearingNotificationInputData inputData = getInputData(caseId, defendantId, TEMPLATE_NAME, hearingId, hearingDateTimeUTC);

        final JsonObject prosecutionCase = FileUtil.jsonFromString(FileUtil.getPayload("progressioncase.json")
                .replaceAll("%CASE_ID%", caseId.toString())
                .replaceAll("OFFENCE_ID_1", offenceId1.toString())
                .replaceAll("OFFENCE_ID_2", offenceId2.toString())
                .replaceAll("%DEFENDANT_ID%", defendantId.toString()));

        when(progressionService.getProsecutionCaseDetailById(any(), any())).thenReturn(Optional.of(createObjectBuilder().
                add("prosecutionCase", prosecutionCase)
                .build()
        ));

        when(refDataService.getProsecutor(any(), any(), any())).thenReturn(Optional.of(getPayload("prosecutor-with-no-email.json")));
        AssociatedDefenceOrganisation associatedDefenceOrganisation = null;
        when(defenceService.getDefenceOrganisationByDefendantId(any(), any())).thenReturn(associatedDefenceOrganisation);


        hearingNotificationHelper.sendHearingNotificationsToRelevantParties(jsonEnvelope, inputData);

        verify(notificationService, times(1)).sendEmail(any(), any(), any(), any(), any(), defendantEmailCapture.capture());
        final List<EmailChannel> emailChannels = defendantEmailCapture.getValue();
        verifyEmailChannel(emailChannels, "defendant_email@email.com", fromString(TEMPLATE_ID));
        assertThat(emailChannels, hasSize(1));
        final EmailChannel emailChannel = emailChannels.get(0);
        assertThat(emailChannel.getSendToAddress(), is("defendant_email@email.com"));

        assertThat(emailChannel.getTemplateId(), is(fromString(TEMPLATE_ID)));
        assertThat(emailChannel.getPersonalisation(), notNullValue());
        assertThat(emailChannel.getPersonalisation().getAdditionalProperties().containsKey(HEARING_NOTIFICATION_DATE),is(true));
        assertThat(emailChannel.getPersonalisation().getAdditionalProperties().get(HEARING_NOTIFICATION_DATE), is(expectedUKTime));
    }

    @Test
    @MockitoSettings(strictness = Strictness.LENIENT)
    void shouldGetEarliestStartDateTimeNonNull() {
        final ZonedDateTime nowTime = ZonedDateTime.now();
        ZonedDateTime result = hearingNotificationHelper.getEarliestStartDateTime(nowTime);
        assertThat("Europe/London", is(result.getZone().getId()));
    }

    @Test
    @MockitoSettings(strictness = Strictness.LENIENT)
    void shouldGetEarliestStartDateTimeNull() {
        ZonedDateTime result = hearingNotificationHelper.getEarliestStartDateTime(null);
        assertThat(null, is(result));
    }
}