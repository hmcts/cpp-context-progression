package uk.gov.moj.cpp.progression.processor;

import static com.google.common.collect.Lists.newArrayList;
import static java.util.Collections.singletonList;
import static java.util.Optional.of;
import static java.util.UUID.randomUUID;
import static uk.gov.justice.services.messaging.JsonObjects.createObjectBuilder;
import static org.apache.commons.lang3.RandomStringUtils.randomAlphabetic;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.notNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static uk.gov.justice.core.courts.Address.address;
import static uk.gov.justice.core.courts.AssociatedPerson.associatedPerson;
import static uk.gov.justice.core.courts.ConfirmedProsecutionCaseId.confirmedProsecutionCaseId;
import static uk.gov.justice.core.courts.CourtApplicationParty.courtApplicationParty;
import static uk.gov.justice.core.courts.CourtCentre.courtCentre;
import static uk.gov.justice.core.courts.Defendant.defendant;
import static uk.gov.justice.core.courts.ListDefendantRequest.listDefendantRequest;
import static uk.gov.justice.core.courts.LjaDetails.ljaDetails;
import static uk.gov.justice.core.courts.MasterDefendant.masterDefendant;
import static uk.gov.justice.core.courts.Person.person;
import static uk.gov.justice.core.courts.ProsecutingAuthority.prosecutingAuthority;
import static uk.gov.justice.core.courts.ProsecutionCase.prosecutionCase;
import static uk.gov.justice.core.courts.ProsecutionCaseIdentifier.prosecutionCaseIdentifier;
import static uk.gov.justice.core.courts.SummonsApprovedOutcome.summonsApprovedOutcome;
import static uk.gov.justice.core.courts.SummonsData.summonsData;
import static uk.gov.justice.core.courts.SummonsDataPrepared.summonsDataPrepared;
import static uk.gov.justice.core.courts.SummonsType.APPLICATION;
import static uk.gov.justice.core.courts.SummonsType.BREACH;
import static uk.gov.justice.core.courts.SummonsType.FIRST_HEARING;
import static uk.gov.justice.core.courts.SummonsType.SJP_REFERRAL;
import static uk.gov.justice.core.courts.notification.EmailChannel.emailChannel;
import static uk.gov.justice.core.courts.summons.SummonsDocumentContent.summonsDocumentContent;
import static uk.gov.justice.core.courts.summons.SummonsProsecutor.summonsProsecutor;
import static uk.gov.justice.services.test.utils.core.random.RandomGenerator.BOOLEAN;
import static uk.gov.moj.cpp.progression.processor.summons.SummonsCode.BREACH_OFFENCES;
import static uk.gov.moj.cpp.progression.processor.summons.SummonsCode.EITHER_WAY;
import static uk.gov.moj.cpp.progression.processor.summons.SummonsCode.MCA;
import static uk.gov.moj.cpp.progression.processor.summons.SummonsCode.WITNESS;

import uk.gov.justice.core.courts.AssociatedPerson;
import uk.gov.justice.core.courts.CourtApplication;
import uk.gov.justice.core.courts.CourtApplicationParty;
import uk.gov.justice.core.courts.CourtApplicationPartyListingNeeds;
import uk.gov.justice.core.courts.Defendant;
import uk.gov.justice.core.courts.InitiationCode;
import uk.gov.justice.core.courts.ListDefendantRequest;
import uk.gov.justice.core.courts.LjaDetails;
import uk.gov.justice.core.courts.ProsecutingAuthority;
import uk.gov.justice.core.courts.ProsecutionCase;
import uk.gov.justice.core.courts.ProsecutionCaseIdentifier;
import uk.gov.justice.core.courts.SummonsApprovedOutcome;
import uk.gov.justice.core.courts.SummonsData;
import uk.gov.justice.core.courts.SummonsDataPrepared;
import uk.gov.justice.core.courts.SummonsType;
import uk.gov.justice.core.courts.notification.EmailChannel;
import uk.gov.justice.core.courts.summons.SummonsDefendant;
import uk.gov.justice.core.courts.summons.SummonsDocumentContent;
import uk.gov.justice.core.courts.summons.SummonsProsecutor;
import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.common.converter.ObjectToJsonObjectConverter;
import uk.gov.justice.services.common.converter.ZonedDateTimes;
import uk.gov.justice.services.common.converter.jackson.ObjectMapperProducer;
import uk.gov.justice.services.core.requester.Requester;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.test.utils.core.random.RandomGenerator;
import uk.gov.moj.cpp.progression.processor.summons.ApplicantEmailAddressUtil;
import uk.gov.moj.cpp.progression.processor.summons.ApplicationSummonsService;
import uk.gov.moj.cpp.progression.processor.summons.CaseDefendantSummonsService;
import uk.gov.moj.cpp.progression.processor.summons.PublishSummonsDocumentService;
import uk.gov.moj.cpp.progression.processor.summons.SummonsCode;
import uk.gov.moj.cpp.progression.processor.summons.SummonsNotificationEmailPayloadService;
import uk.gov.moj.cpp.progression.processor.summons.SummonsService;
import uk.gov.moj.cpp.progression.processor.summons.SummonsTemplateNameService;
import uk.gov.moj.cpp.progression.service.ProgressionService;
import uk.gov.moj.cpp.progression.service.RefDataService;

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;

import javax.json.JsonObject;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class SummonsDataPreparedEventProcessorTest {

    private static final UUID CASE_ID = randomUUID();
    private static final UUID DEFENDANT_ID = randomUUID();
    private static final UUID SUBJECT_ID = randomUUID();
    private static final UUID APPLICATION_ID = randomUUID();
    private static final UUID COURT_CENTRE_ID = UUID.fromString("f8254db1-1683-483e-afb3-b87fde5a0a26");
    private static final UUID COURT_ROOM_ID = randomUUID();
    private static final UUID PROSECUTION_AUTHORITY_ID = randomUUID();
    private static final String EMAIL_ADDRESS = RandomGenerator.EMAIL_ADDRESS.next();
    private static final String SUMMONS_APPROVED_EMAIL_ADDRESS = RandomGenerator.EMAIL_ADDRESS.next();
    private static final EmailChannel EMAIL_CHANNEL = emailChannel().build();

    private static final ZonedDateTime HEARING_DATE_TIME = ZonedDateTimes.fromString("2018-04-01T13:00:00.000Z");
    private static final String LJA_CODE = "ljaCode";

    @Spy
    private final ObjectToJsonObjectConverter objectToJsonObjectConverter = new ObjectToJsonObjectConverter(new ObjectMapperProducer().objectMapper());

    @Spy
    private final JsonObjectToObjectConverter jsonObjectToObjectConverter = new JsonObjectToObjectConverter(new ObjectMapperProducer().objectMapper());

    @Mock
    private RefDataService referenceDataService;

    @Mock
    private Requester requester;

    @Mock
    private ProgressionService progressionService;

    @Mock
    private JsonEnvelope envelope;

    @Mock
    private SummonsNotificationEmailPayloadService summonsNotificationEmailPayloadService;

    @InjectMocks
    private SummonsDataPreparedEventProcessor summonsDataPreparedEventProcessor;

    @Captor
    private ArgumentCaptor<SummonsDocumentContent> parentSummonsDocumentContentArgumentCaptor;

    @Mock
    private SummonsService summonsService;

    @Mock
    private SummonsTemplateNameService summonsTemplateNameService;

    @Mock
    private CaseDefendantSummonsService caseDefendantSummonsService;

    @Mock
    private ApplicationSummonsService applicationSummonsService;

    @Mock
    private PublishSummonsDocumentService publishSummonsDocumentService;

    @Mock
    private ApplicantEmailAddressUtil applicantEmailAddressUtil;

    // if suppressed, document not sent for remote printing
    private boolean summonsSuppressed;

    public static Stream<Arguments> firstHearingSummonsSpecifications() {
        return Stream.of(
                Arguments.of(MCA, false, 1),
                Arguments.of(WITNESS, false, 1),
                Arguments.of(EITHER_WAY, false, 1),
                Arguments.of(SummonsCode.APPLICATION, false, 1),
                Arguments.of(BREACH_OFFENCES, false, 1)
        );
    }

    public static Stream<Arguments> firstHearingSummonsSpecificationsForYouth() {
        return Stream.of(
                Arguments.of(MCA, true, 2),
                Arguments.of(WITNESS, true, 2),
                Arguments.of(EITHER_WAY, true, 2),
                Arguments.of(SummonsCode.APPLICATION, true, 2),
                Arguments.of(BREACH_OFFENCES, true, 2)
        );
    }

    public static Stream<Arguments> sjpReferralSummonsSpecifications() {
        return Stream.of(
                Arguments.of(false, 1),
                Arguments.of(true, 1)
        );
    }

    public static Stream<Arguments> applicationSummonsSpecifications() {
        return Stream.of(
                // summons required, is youth, number of documents
                Arguments.of(APPLICATION, false, 1),
                Arguments.of(BREACH, false, 1)
                );
    }

    public static Stream<Arguments> applicationSummonsSpecificationsForYouth() {
        return Stream.of(
                // summons required, is youth, number of documents
                Arguments.of(APPLICATION, true, 1),
                Arguments.of(BREACH, true, 1)
        );
    }

    @BeforeEach
    public void setup() {
        summonsSuppressed = BOOLEAN.next();
    }

    @MethodSource("firstHearingSummonsSpecifications")
    @ParameterizedTest
    public void shouldGenerateEnglishSummonsPayloadForFirstHearing(final SummonsCode summonsCode, final boolean isYouth, final int numberOfDocuments) {
        final boolean sendForRemotePrinting =  !summonsSuppressed;

        //Given
        final SummonsDataPrepared summonsDataPrepared = getSummonsDataPreparedForCase(FIRST_HEARING);
        final JsonObject summonsDataPreparedAsJsonObject = objectToJsonObjectConverter.convert(summonsDataPrepared);
        final ProsecutionCase prosecutionCase = getProsecutionCase(InitiationCode.S, summonsCode);
        final ProsecutionCaseIdentifier prosecutionCaseIdentifier = prosecutionCase.getProsecutionCaseIdentifier();
        final Optional<JsonObject> optionalCase = of(createObjectBuilder().add("prosecutionCase", objectToJsonObjectConverter.convert(prosecutionCase)).build());
        final Optional<JsonObject> courtCentreJson = getCourtCentreJson(false);
        final SummonsProsecutor summonsProsecutor = getSummonsProsecutor();
        final Optional<LjaDetails> ljaDetails = getLjaDetails();
        final String defendantTemplateName = randomAlphabetic(15);
        final String parentGuardianTemplateName = randomAlphabetic(15);
        final SummonsDocumentContent defendantTemplatePayload = getDefendantTemplatePayload(isYouth);

        when(envelope.payloadAsJsonObject()).thenReturn(summonsDataPreparedAsJsonObject);

        when(progressionService.getProsecutionCaseDetailById(envelope, CASE_ID.toString())).thenReturn(optionalCase);
        when(referenceDataService.getCourtCentreWithCourtRoomsById(COURT_CENTRE_ID, envelope, requester)).thenReturn(courtCentreJson);
        when(summonsService.getLjaDetails(envelope, LJA_CODE)).thenReturn(ljaDetails);
        when(summonsService.getProsecutor(eq(envelope), eq(prosecutionCaseIdentifier))).thenReturn(summonsProsecutor);
        when(summonsTemplateNameService.getCaseSummonsTemplateName(FIRST_HEARING, summonsCode, false)).thenReturn(defendantTemplateName);
        when(caseDefendantSummonsService.generateSummonsPayloadForDefendant(eq(envelope), any(SummonsDataPrepared.class), any(ProsecutionCase.class), any(Defendant.class), any(ListDefendantRequest.class), eq(courtCentreJson.get()), eq(ljaDetails), eq(summonsProsecutor))).thenReturn(defendantTemplatePayload);
        when(summonsNotificationEmailPayloadService.getEmailChannelForCaseDefendant(any(SummonsDataPrepared.class), any(SummonsDocumentContent.class), eq(SUMMONS_APPROVED_EMAIL_ADDRESS), any(), any(Defendant.class), anyList(), eq(sendForRemotePrinting), anyBoolean(), any(UUID.class), eq(FIRST_HEARING))).thenReturn(of(EMAIL_CHANNEL));

        summonsDataPreparedEventProcessor.requestSummons(envelope);

        //When
        verify(progressionService).getProsecutionCaseDetailById(envelope, CASE_ID.toString());
        verify(referenceDataService).getCourtCentreWithCourtRoomsById(COURT_CENTRE_ID, envelope, requester);
        verify(summonsService).getLjaDetails(envelope, LJA_CODE);
        verify(summonsService).getProsecutor(eq(envelope), eq(prosecutionCaseIdentifier));
        verify(summonsTemplateNameService).getCaseSummonsTemplateName(FIRST_HEARING, summonsCode, false);
        verify(caseDefendantSummonsService).generateSummonsPayloadForDefendant(eq(envelope), any(SummonsDataPrepared.class), any(ProsecutionCase.class), any(Defendant.class), any(ListDefendantRequest.class), eq(courtCentreJson.get()), eq(ljaDetails), eq(summonsProsecutor));
        verify(publishSummonsDocumentService).generateCaseSummonsCourtDocument(eq(envelope), eq(DEFENDANT_ID), eq(CASE_ID), eq(defendantTemplatePayload), eq(defendantTemplateName), eq(sendForRemotePrinting), eq(EMAIL_CHANNEL), notNull(UUID.class));
        verify(summonsNotificationEmailPayloadService).getEmailChannelForCaseDefendant(any(SummonsDataPrepared.class), eq(defendantTemplatePayload), eq(SUMMONS_APPROVED_EMAIL_ADDRESS), any(), any(Defendant.class), anyList(), eq(sendForRemotePrinting), eq(isYouth), any(UUID.class), eq(FIRST_HEARING));

        // verification for parent document (if applicable)
        if (numberOfDocuments > 1) {
            verify(summonsTemplateNameService).getCaseSummonsParentTemplateName(false);
            verify(publishSummonsDocumentService).generateCaseSummonsCourtDocument(eq(envelope), eq(DEFENDANT_ID), eq(CASE_ID), parentSummonsDocumentContentArgumentCaptor.capture(), eq(parentGuardianTemplateName), eq(sendForRemotePrinting), eq(EMAIL_CHANNEL), any(UUID.class));
            final SummonsDocumentContent parentDocumentPayload = parentSummonsDocumentContentArgumentCaptor.getValue();
            verify(summonsNotificationEmailPayloadService).getEmailChannelForCaseDefendantParent(any(SummonsDataPrepared.class), eq(parentDocumentPayload), eq(SUMMONS_APPROVED_EMAIL_ADDRESS), anyList(), any(Defendant.class), anyList(), eq(sendForRemotePrinting), any(UUID.class), eq(FIRST_HEARING));
            assertThat(parentDocumentPayload.getAddressee(), notNullValue());
            assertThat(parentDocumentPayload.getAddressee().getName(), is("parent first name parent middle name parent last name"));
            assertThat(parentDocumentPayload.getAddressee().getAddress().getLine1(), is("parent address 1"));
        }

        verifyNoMoreInteractions(progressionService, referenceDataService, summonsService, summonsTemplateNameService, caseDefendantSummonsService, publishSummonsDocumentService, summonsNotificationEmailPayloadService);
    }

    @MethodSource("firstHearingSummonsSpecificationsForYouth")
    @ParameterizedTest
    public void shouldGenerateEnglishSummonsPayloadForFirstHearingForYouth(final SummonsCode summonsCode, final boolean isYouth, final int numberOfDocuments) {
        verifySummonsPayloadGeneratedForCaseSummons(InitiationCode.S, FIRST_HEARING, summonsCode, false, isYouth, numberOfDocuments, !summonsSuppressed);
    }


    @MethodSource("firstHearingSummonsSpecifications")
    @ParameterizedTest
    public void shouldGenerateBilingualSummonsPayloadForFirstHearing(final SummonsCode summonsCode, final boolean isYouth, final int numberOfDocuments) {
        final boolean sendForRemotePrinting =  !summonsSuppressed;

        //Given
        final SummonsDataPrepared summonsDataPrepared = getSummonsDataPreparedForCase(FIRST_HEARING);
        final JsonObject summonsDataPreparedAsJsonObject = objectToJsonObjectConverter.convert(summonsDataPrepared);
        final ProsecutionCase prosecutionCase = getProsecutionCase(InitiationCode.S, summonsCode);
        final ProsecutionCaseIdentifier prosecutionCaseIdentifier = prosecutionCase.getProsecutionCaseIdentifier();
        final Optional<JsonObject> optionalCase = of(createObjectBuilder().add("prosecutionCase", objectToJsonObjectConverter.convert(prosecutionCase)).build());
        final Optional<JsonObject> courtCentreJson = getCourtCentreJson(false);
        final SummonsProsecutor summonsProsecutor = getSummonsProsecutor();
        final Optional<LjaDetails> ljaDetails = getLjaDetails();
        final String defendantTemplateName = randomAlphabetic(15);
        final String parentGuardianTemplateName = randomAlphabetic(15);
        final SummonsDocumentContent defendantTemplatePayload = getDefendantTemplatePayload(isYouth);

        when(envelope.payloadAsJsonObject()).thenReturn(summonsDataPreparedAsJsonObject);

        when(progressionService.getProsecutionCaseDetailById(envelope, CASE_ID.toString())).thenReturn(optionalCase);
        when(referenceDataService.getCourtCentreWithCourtRoomsById(COURT_CENTRE_ID, envelope, requester)).thenReturn(courtCentreJson);
        when(summonsService.getLjaDetails(envelope, LJA_CODE)).thenReturn(ljaDetails);
        when(summonsService.getProsecutor(eq(envelope), eq(prosecutionCaseIdentifier))).thenReturn(summonsProsecutor);
        when(summonsTemplateNameService.getCaseSummonsTemplateName(FIRST_HEARING, summonsCode, false)).thenReturn(defendantTemplateName);
        when(caseDefendantSummonsService.generateSummonsPayloadForDefendant(eq(envelope), any(SummonsDataPrepared.class), any(ProsecutionCase.class), any(Defendant.class), any(ListDefendantRequest.class), eq(courtCentreJson.get()), eq(ljaDetails), eq(summonsProsecutor))).thenReturn(defendantTemplatePayload);
        when(summonsNotificationEmailPayloadService.getEmailChannelForCaseDefendant(any(SummonsDataPrepared.class), any(SummonsDocumentContent.class), eq(SUMMONS_APPROVED_EMAIL_ADDRESS), any(), any(Defendant.class), anyList(), eq(sendForRemotePrinting), anyBoolean(), any(UUID.class), eq(FIRST_HEARING))).thenReturn(of(EMAIL_CHANNEL));

        summonsDataPreparedEventProcessor.requestSummons(envelope);

        //When
        verify(progressionService).getProsecutionCaseDetailById(envelope, CASE_ID.toString());
        verify(referenceDataService).getCourtCentreWithCourtRoomsById(COURT_CENTRE_ID, envelope, requester);
        verify(summonsService).getLjaDetails(envelope, LJA_CODE);
        verify(summonsService).getProsecutor(eq(envelope), eq(prosecutionCaseIdentifier));
        verify(summonsTemplateNameService).getCaseSummonsTemplateName(FIRST_HEARING, summonsCode, false);
        verify(caseDefendantSummonsService).generateSummonsPayloadForDefendant(eq(envelope), any(SummonsDataPrepared.class), any(ProsecutionCase.class), any(Defendant.class), any(ListDefendantRequest.class), eq(courtCentreJson.get()), eq(ljaDetails), eq(summonsProsecutor));
        verify(publishSummonsDocumentService).generateCaseSummonsCourtDocument(eq(envelope), eq(DEFENDANT_ID), eq(CASE_ID), eq(defendantTemplatePayload), eq(defendantTemplateName), eq(sendForRemotePrinting), eq(EMAIL_CHANNEL), notNull(UUID.class));
        verify(summonsNotificationEmailPayloadService).getEmailChannelForCaseDefendant(any(SummonsDataPrepared.class), eq(defendantTemplatePayload), eq(SUMMONS_APPROVED_EMAIL_ADDRESS), any(), any(Defendant.class), anyList(), eq(sendForRemotePrinting), eq(isYouth), any(UUID.class), eq(FIRST_HEARING));

        // verification for parent document (if applicable)
        if (numberOfDocuments > 1) {
            verify(summonsTemplateNameService).getCaseSummonsParentTemplateName(false);
            verify(publishSummonsDocumentService).generateCaseSummonsCourtDocument(eq(envelope), eq(DEFENDANT_ID), eq(CASE_ID), parentSummonsDocumentContentArgumentCaptor.capture(), eq(parentGuardianTemplateName), eq(sendForRemotePrinting), eq(EMAIL_CHANNEL), any(UUID.class));
            final SummonsDocumentContent parentDocumentPayload = parentSummonsDocumentContentArgumentCaptor.getValue();
            verify(summonsNotificationEmailPayloadService).getEmailChannelForCaseDefendantParent(any(SummonsDataPrepared.class), eq(parentDocumentPayload), eq(SUMMONS_APPROVED_EMAIL_ADDRESS), anyList(), any(Defendant.class), anyList(), eq(sendForRemotePrinting), any(UUID.class), eq(FIRST_HEARING));
            assertThat(parentDocumentPayload.getAddressee(), notNullValue());
            assertThat(parentDocumentPayload.getAddressee().getName(), is("parent first name parent middle name parent last name"));
            assertThat(parentDocumentPayload.getAddressee().getAddress().getLine1(), is("parent address 1"));
        }

        verifyNoMoreInteractions(progressionService, referenceDataService, summonsService, summonsTemplateNameService, caseDefendantSummonsService, publishSummonsDocumentService, summonsNotificationEmailPayloadService);
    }

    @MethodSource("firstHearingSummonsSpecificationsForYouth")
    @ParameterizedTest
    public void shouldGenerateBilingualSummonsPayloadForFirstHearingForYouth(final SummonsCode summonsCode, final boolean isYouth, final int numberOfDocuments) {
        verifySummonsPayloadGeneratedForCaseSummons(InitiationCode.S, FIRST_HEARING, summonsCode, true, isYouth, numberOfDocuments, !summonsSuppressed);
    }

    @MethodSource("sjpReferralSummonsSpecifications")
    @ParameterizedTest
    public void shouldGenerateEnglishSummonsForSjpReferral(final boolean isYouth, final int numberOfDocuments) {
        boolean sendForRemotePrinting = true;
        //Given
        final SummonsDataPrepared summonsDataPrepared = getSummonsDataPreparedForCase(SJP_REFERRAL);
        final JsonObject summonsDataPreparedAsJsonObject = objectToJsonObjectConverter.convert(summonsDataPrepared);
        final ProsecutionCase prosecutionCase = getProsecutionCase(InitiationCode.J, null);
        final ProsecutionCaseIdentifier prosecutionCaseIdentifier = prosecutionCase.getProsecutionCaseIdentifier();
        final Optional<JsonObject> optionalCase = of(createObjectBuilder().add("prosecutionCase", objectToJsonObjectConverter.convert(prosecutionCase)).build());
        final Optional<JsonObject> courtCentreJson = getCourtCentreJson(false);
        final SummonsProsecutor summonsProsecutor = getSummonsProsecutor();
        final Optional<LjaDetails> ljaDetails = getLjaDetails();
        final String defendantTemplateName = randomAlphabetic(15);
        final String parentGuardianTemplateName = randomAlphabetic(15);
        final SummonsDocumentContent defendantTemplatePayload = getDefendantTemplatePayload(isYouth);

        when(envelope.payloadAsJsonObject()).thenReturn(summonsDataPreparedAsJsonObject);

        when(progressionService.getProsecutionCaseDetailById(envelope, CASE_ID.toString())).thenReturn(optionalCase);
        when(referenceDataService.getCourtCentreWithCourtRoomsById(COURT_CENTRE_ID, envelope, requester)).thenReturn(courtCentreJson);
        when(summonsService.getLjaDetails(envelope, LJA_CODE)).thenReturn(ljaDetails);
        when(summonsService.getProsecutor(eq(envelope), eq(prosecutionCaseIdentifier))).thenReturn(summonsProsecutor);
        when(summonsTemplateNameService.getCaseSummonsTemplateName(SJP_REFERRAL, null, false)).thenReturn(defendantTemplateName);
        when(caseDefendantSummonsService.generateSummonsPayloadForDefendant(eq(envelope), any(SummonsDataPrepared.class), any(ProsecutionCase.class), any(Defendant.class), any(ListDefendantRequest.class), eq(courtCentreJson.get()), eq(ljaDetails), eq(summonsProsecutor))).thenReturn(defendantTemplatePayload);
        when(summonsNotificationEmailPayloadService.getEmailChannelForCaseDefendant(any(SummonsDataPrepared.class), any(SummonsDocumentContent.class), eq(SUMMONS_APPROVED_EMAIL_ADDRESS), any(), any(Defendant.class), anyList(), eq(sendForRemotePrinting), anyBoolean(), any(UUID.class), eq(SJP_REFERRAL))).thenReturn(of(EMAIL_CHANNEL));

        summonsDataPreparedEventProcessor.requestSummons(envelope);

        //When
        verify(progressionService).getProsecutionCaseDetailById(envelope, CASE_ID.toString());
        verify(referenceDataService).getCourtCentreWithCourtRoomsById(COURT_CENTRE_ID, envelope, requester);
        verify(summonsService).getLjaDetails(envelope, LJA_CODE);
        verify(summonsService).getProsecutor(eq(envelope), eq(prosecutionCaseIdentifier));
        verify(summonsTemplateNameService).getCaseSummonsTemplateName(SJP_REFERRAL, null, false);
        verify(caseDefendantSummonsService).generateSummonsPayloadForDefendant(eq(envelope), any(SummonsDataPrepared.class), any(ProsecutionCase.class), any(Defendant.class), any(ListDefendantRequest.class), eq(courtCentreJson.get()), eq(ljaDetails), eq(summonsProsecutor));
        verify(publishSummonsDocumentService).generateCaseSummonsCourtDocument(eq(envelope), eq(DEFENDANT_ID), eq(CASE_ID), eq(defendantTemplatePayload), eq(defendantTemplateName), eq(sendForRemotePrinting), eq(EMAIL_CHANNEL), notNull(UUID.class));
        verify(summonsNotificationEmailPayloadService).getEmailChannelForCaseDefendant(any(SummonsDataPrepared.class), eq(defendantTemplatePayload), eq(SUMMONS_APPROVED_EMAIL_ADDRESS), any(), any(Defendant.class), anyList(), eq(sendForRemotePrinting), eq(isYouth), any(UUID.class), eq(SJP_REFERRAL));

        // verification for parent document (if applicable)
        if (numberOfDocuments > 1) {
            verify(summonsTemplateNameService).getCaseSummonsParentTemplateName(false);
            verify(publishSummonsDocumentService).generateCaseSummonsCourtDocument(eq(envelope), eq(DEFENDANT_ID), eq(CASE_ID), parentSummonsDocumentContentArgumentCaptor.capture(), eq(parentGuardianTemplateName), eq(sendForRemotePrinting), eq(EMAIL_CHANNEL), any(UUID.class));
            final SummonsDocumentContent parentDocumentPayload = parentSummonsDocumentContentArgumentCaptor.getValue();
            verify(summonsNotificationEmailPayloadService).getEmailChannelForCaseDefendantParent(any(SummonsDataPrepared.class), eq(parentDocumentPayload), eq(SUMMONS_APPROVED_EMAIL_ADDRESS), anyList(), any(Defendant.class), anyList(), eq(sendForRemotePrinting), any(UUID.class), eq(SJP_REFERRAL));
            assertThat(parentDocumentPayload.getAddressee(), notNullValue());
            assertThat(parentDocumentPayload.getAddressee().getName(), is("parent first name parent middle name parent last name"));
            assertThat(parentDocumentPayload.getAddressee().getAddress().getLine1(), is("parent address 1"));
        }

        verifyNoMoreInteractions(progressionService, referenceDataService, summonsService, summonsTemplateNameService, caseDefendantSummonsService, publishSummonsDocumentService, summonsNotificationEmailPayloadService);
    }

    @MethodSource("sjpReferralSummonsSpecifications")
    @ParameterizedTest
    public void shouldGenerateBilingualSummonsForSjpReferral(final boolean isYouth, final int numberOfDocuments) {
        final boolean sendForRemotePrinting = true;
        //Given
        final SummonsDataPrepared summonsDataPrepared = getSummonsDataPreparedForCase(SJP_REFERRAL);
        final JsonObject summonsDataPreparedAsJsonObject = objectToJsonObjectConverter.convert(summonsDataPrepared);
        final ProsecutionCase prosecutionCase = getProsecutionCase(InitiationCode.J, null);
        final ProsecutionCaseIdentifier prosecutionCaseIdentifier = prosecutionCase.getProsecutionCaseIdentifier();
        final Optional<JsonObject> optionalCase = of(createObjectBuilder().add("prosecutionCase", objectToJsonObjectConverter.convert(prosecutionCase)).build());
        final Optional<JsonObject> courtCentreJson = getCourtCentreJson(false);
        final SummonsProsecutor summonsProsecutor = getSummonsProsecutor();
        final Optional<LjaDetails> ljaDetails = getLjaDetails();
        final String defendantTemplateName = randomAlphabetic(15);
        final String parentGuardianTemplateName = randomAlphabetic(15);
        final SummonsDocumentContent defendantTemplatePayload = getDefendantTemplatePayload(isYouth);

        when(envelope.payloadAsJsonObject()).thenReturn(summonsDataPreparedAsJsonObject);

        when(progressionService.getProsecutionCaseDetailById(envelope, CASE_ID.toString())).thenReturn(optionalCase);
        when(referenceDataService.getCourtCentreWithCourtRoomsById(COURT_CENTRE_ID, envelope, requester)).thenReturn(courtCentreJson);
        when(summonsService.getLjaDetails(envelope, LJA_CODE)).thenReturn(ljaDetails);
        when(summonsService.getProsecutor(eq(envelope), eq(prosecutionCaseIdentifier))).thenReturn(summonsProsecutor);
        when(summonsTemplateNameService.getCaseSummonsTemplateName(SJP_REFERRAL, null, false)).thenReturn(defendantTemplateName);
        when(caseDefendantSummonsService.generateSummonsPayloadForDefendant(eq(envelope), any(SummonsDataPrepared.class), any(ProsecutionCase.class), any(Defendant.class), any(ListDefendantRequest.class), eq(courtCentreJson.get()), eq(ljaDetails), eq(summonsProsecutor))).thenReturn(defendantTemplatePayload);
        when(summonsNotificationEmailPayloadService.getEmailChannelForCaseDefendant(any(SummonsDataPrepared.class), any(SummonsDocumentContent.class), eq(SUMMONS_APPROVED_EMAIL_ADDRESS), any(), any(Defendant.class), anyList(), eq(sendForRemotePrinting), anyBoolean(), any(UUID.class), eq(SJP_REFERRAL))).thenReturn(of(EMAIL_CHANNEL));

        summonsDataPreparedEventProcessor.requestSummons(envelope);

        //When
        verify(progressionService).getProsecutionCaseDetailById(envelope, CASE_ID.toString());
        verify(referenceDataService).getCourtCentreWithCourtRoomsById(COURT_CENTRE_ID, envelope, requester);
        verify(summonsService).getLjaDetails(envelope, LJA_CODE);
        verify(summonsService).getProsecutor(eq(envelope), eq(prosecutionCaseIdentifier));
        verify(summonsTemplateNameService).getCaseSummonsTemplateName(SJP_REFERRAL, null, false);
        verify(caseDefendantSummonsService).generateSummonsPayloadForDefendant(eq(envelope), any(SummonsDataPrepared.class), any(ProsecutionCase.class), any(Defendant.class), any(ListDefendantRequest.class), eq(courtCentreJson.get()), eq(ljaDetails), eq(summonsProsecutor));
        verify(publishSummonsDocumentService).generateCaseSummonsCourtDocument(eq(envelope), eq(DEFENDANT_ID), eq(CASE_ID), eq(defendantTemplatePayload), eq(defendantTemplateName), eq(sendForRemotePrinting), eq(EMAIL_CHANNEL), notNull(UUID.class));
        verify(summonsNotificationEmailPayloadService).getEmailChannelForCaseDefendant(any(SummonsDataPrepared.class), eq(defendantTemplatePayload), eq(SUMMONS_APPROVED_EMAIL_ADDRESS), any(), any(Defendant.class), anyList(), eq(sendForRemotePrinting), eq(isYouth), any(UUID.class), eq(SJP_REFERRAL));

        // verification for parent document (if applicable)
        if (numberOfDocuments > 1) {
            verify(summonsTemplateNameService).getCaseSummonsParentTemplateName(false);
            verify(publishSummonsDocumentService).generateCaseSummonsCourtDocument(eq(envelope), eq(DEFENDANT_ID), eq(CASE_ID), parentSummonsDocumentContentArgumentCaptor.capture(), eq(parentGuardianTemplateName), eq(sendForRemotePrinting), eq(EMAIL_CHANNEL), any(UUID.class));
            final SummonsDocumentContent parentDocumentPayload = parentSummonsDocumentContentArgumentCaptor.getValue();
            verify(summonsNotificationEmailPayloadService).getEmailChannelForCaseDefendantParent(any(SummonsDataPrepared.class), eq(parentDocumentPayload), eq(SUMMONS_APPROVED_EMAIL_ADDRESS), anyList(), any(Defendant.class), anyList(), eq(sendForRemotePrinting), any(UUID.class), eq(SJP_REFERRAL));
            assertThat(parentDocumentPayload.getAddressee(), notNullValue());
            assertThat(parentDocumentPayload.getAddressee().getName(), is("parent first name parent middle name parent last name"));
            assertThat(parentDocumentPayload.getAddressee().getAddress().getLine1(), is("parent address 1"));
        }

        verifyNoMoreInteractions(progressionService, referenceDataService, summonsService, summonsTemplateNameService, caseDefendantSummonsService, publishSummonsDocumentService, summonsNotificationEmailPayloadService);
    }

    @MethodSource("applicationSummonsSpecifications")
    @ParameterizedTest
    public void shouldGenerateEnglishSummonsForApplications(final SummonsType summonsRequired, final boolean isYouth, final int numberOfDocuments) {
        final boolean sendForRemotePrinting = !summonsSuppressed;
        //Given
        final SummonsDataPrepared summonsDataPrepared = getSummonsDataPreparedForApplication(summonsRequired);
        final JsonObject summonsDataPreparedAsJsonObject = objectToJsonObjectConverter.convert(summonsDataPrepared);
        final Optional<JsonObject> optionalApplication = of(createObjectBuilder().add("courtApplication", objectToJsonObjectConverter.convert(getCourtApplication())).build());
        final Optional<JsonObject> courtCentreJson = getCourtCentreJson(false);
        final Optional<LjaDetails> ljaDetails = getLjaDetails();
        final String subjectTemplateName = randomAlphabetic(15);
        final String parentGuardianTemplateName = randomAlphabetic(15);
        final SummonsDocumentContent subjectTemplatePayload = getDefendantTemplatePayload(isYouth);

        when(envelope.payloadAsJsonObject()).thenReturn(summonsDataPreparedAsJsonObject);

        when(progressionService.getCourtApplicationById(envelope, APPLICATION_ID.toString())).thenReturn(optionalApplication);
        when(referenceDataService.getCourtCentreWithCourtRoomsById(COURT_CENTRE_ID, envelope, requester)).thenReturn(courtCentreJson);
        when(summonsService.getLjaDetails(envelope, LJA_CODE)).thenReturn(ljaDetails);
        when(summonsTemplateNameService.getApplicationTemplateName(summonsRequired, false)).thenReturn(subjectTemplateName);
        when(applicationSummonsService.generateSummonsDocumentContent(any(SummonsDataPrepared.class), any(CourtApplication.class), any(CourtApplicationPartyListingNeeds.class), eq(courtCentreJson.get()), eq(ljaDetails))).thenReturn(subjectTemplatePayload);
        when(summonsNotificationEmailPayloadService.getEmailChannelForApplicationAddressee(any(SummonsDataPrepared.class), any(SummonsDocumentContent.class), eq(SUMMONS_APPROVED_EMAIL_ADDRESS), eq(sendForRemotePrinting), anyBoolean(), any(UUID.class), eq(summonsRequired))).thenReturn(of(EMAIL_CHANNEL));

        summonsDataPreparedEventProcessor.requestSummons(envelope);

        //When
        verify(progressionService).getCourtApplicationById(envelope, APPLICATION_ID.toString());
        verify(referenceDataService).getCourtCentreWithCourtRoomsById(COURT_CENTRE_ID, envelope, requester);
        verify(summonsService).getLjaDetails(envelope, LJA_CODE);
        verify(summonsTemplateNameService).getApplicationTemplateName(summonsRequired, false);
        verify(applicationSummonsService).generateSummonsDocumentContent(any(SummonsDataPrepared.class), any(CourtApplication.class), any(CourtApplicationPartyListingNeeds.class), eq(courtCentreJson.get()), eq(ljaDetails));
        verify(publishSummonsDocumentService).generateApplicationSummonsCourtDocument(eq(envelope), eq(APPLICATION_ID), eq(subjectTemplatePayload), eq(subjectTemplateName), eq(sendForRemotePrinting), eq(EMAIL_CHANNEL), notNull(UUID.class));
        verify(summonsNotificationEmailPayloadService).getEmailChannelForApplicationAddressee(any(SummonsDataPrepared.class), eq(subjectTemplatePayload), eq(SUMMONS_APPROVED_EMAIL_ADDRESS), eq(sendForRemotePrinting), eq(isYouth), any(UUID.class), eq(summonsRequired));

        // verification for parent document (if applicable)
        if (numberOfDocuments > 1) {
            verify(summonsTemplateNameService).getBreachSummonsParentTemplateName(false);
            verify(publishSummonsDocumentService).generateApplicationSummonsCourtDocument(eq(envelope), eq(APPLICATION_ID), parentSummonsDocumentContentArgumentCaptor.capture(), eq(parentGuardianTemplateName), eq(sendForRemotePrinting), eq(EMAIL_CHANNEL), notNull(UUID.class));
            final SummonsDocumentContent parentDocumentPayload = parentSummonsDocumentContentArgumentCaptor.getValue();
            verify(summonsNotificationEmailPayloadService).getEmailChannelForApplicationAddresseeParent(any(SummonsDataPrepared.class), eq(parentDocumentPayload), eq(SUMMONS_APPROVED_EMAIL_ADDRESS), eq(sendForRemotePrinting), any(UUID.class), eq(summonsRequired));
            assertThat(parentDocumentPayload.getAddressee(), notNullValue());
            assertThat(parentDocumentPayload.getAddressee().getName(), is("parent first name parent middle name parent last name"));
            assertThat(parentDocumentPayload.getAddressee().getAddress().getLine1(), is("parent address 1"));

        }

        verifyNoMoreInteractions(progressionService, referenceDataService, summonsService, summonsTemplateNameService, caseDefendantSummonsService, publishSummonsDocumentService, summonsNotificationEmailPayloadService);
    }

    @MethodSource("applicationSummonsSpecificationsForYouth")
    @ParameterizedTest
    public void shouldGenerateEnglishSummonsForApplicationsForYouth(final SummonsType summonsRequired, final boolean isYouth, final int numberOfDocuments) {
        final boolean sendForRemotePrinting = !summonsSuppressed;

        //Given
        final SummonsDataPrepared summonsDataPrepared = getSummonsDataPreparedForApplication(summonsRequired);
        final JsonObject summonsDataPreparedAsJsonObject = objectToJsonObjectConverter.convert(summonsDataPrepared);
        final Optional<JsonObject> optionalApplication = of(createObjectBuilder().add("courtApplication", objectToJsonObjectConverter.convert(getCourtApplication())).build());
        final Optional<JsonObject> courtCentreJson = getCourtCentreJson(false);
        final Optional<LjaDetails> ljaDetails = getLjaDetails();
        final String subjectTemplateName = randomAlphabetic(15);
        final String parentGuardianTemplateName = randomAlphabetic(15);
        final SummonsDocumentContent subjectTemplatePayload = getDefendantTemplatePayload(isYouth);

        when(envelope.payloadAsJsonObject()).thenReturn(summonsDataPreparedAsJsonObject);

        when(progressionService.getCourtApplicationById(envelope, APPLICATION_ID.toString())).thenReturn(optionalApplication);
        when(referenceDataService.getCourtCentreWithCourtRoomsById(COURT_CENTRE_ID, envelope, requester)).thenReturn(courtCentreJson);
        when(summonsService.getLjaDetails(envelope, LJA_CODE)).thenReturn(ljaDetails);
        when(summonsTemplateNameService.getApplicationTemplateName(summonsRequired, false)).thenReturn(subjectTemplateName);
        when(applicationSummonsService.generateSummonsDocumentContent(any(SummonsDataPrepared.class), any(CourtApplication.class), any(CourtApplicationPartyListingNeeds.class), eq(courtCentreJson.get()), eq(ljaDetails))).thenReturn(subjectTemplatePayload);
        when(summonsNotificationEmailPayloadService.getEmailChannelForApplicationAddressee(any(SummonsDataPrepared.class), any(SummonsDocumentContent.class), eq(SUMMONS_APPROVED_EMAIL_ADDRESS), eq(sendForRemotePrinting), anyBoolean(), any(UUID.class), eq(summonsRequired))).thenReturn(of(EMAIL_CHANNEL));

        summonsDataPreparedEventProcessor.requestSummons(envelope);

        //When
        verify(progressionService).getCourtApplicationById(envelope, APPLICATION_ID.toString());
        verify(referenceDataService).getCourtCentreWithCourtRoomsById(COURT_CENTRE_ID, envelope, requester);
        verify(summonsService).getLjaDetails(envelope, LJA_CODE);
        verify(summonsTemplateNameService).getApplicationTemplateName(summonsRequired, false);
        verify(applicationSummonsService).generateSummonsDocumentContent(any(SummonsDataPrepared.class), any(CourtApplication.class), any(CourtApplicationPartyListingNeeds.class), eq(courtCentreJson.get()), eq(ljaDetails));
        verify(publishSummonsDocumentService).generateApplicationSummonsCourtDocument(eq(envelope), eq(APPLICATION_ID), eq(subjectTemplatePayload), eq(subjectTemplateName), eq(sendForRemotePrinting), any(), notNull(UUID.class));
        verify(summonsNotificationEmailPayloadService).getEmailChannelForApplicationAddressee(any(SummonsDataPrepared.class), eq(subjectTemplatePayload), eq(SUMMONS_APPROVED_EMAIL_ADDRESS), eq(sendForRemotePrinting), eq(isYouth), any(UUID.class), eq(summonsRequired));

        // verification for parent document (if applicable)
        if (numberOfDocuments > 1) {
            verify(summonsTemplateNameService).getBreachSummonsParentTemplateName(false);
            verify(publishSummonsDocumentService).generateApplicationSummonsCourtDocument(eq(envelope), eq(APPLICATION_ID), parentSummonsDocumentContentArgumentCaptor.capture(), eq(parentGuardianTemplateName), eq(sendForRemotePrinting), eq(EMAIL_CHANNEL), notNull(UUID.class));
            final SummonsDocumentContent parentDocumentPayload = parentSummonsDocumentContentArgumentCaptor.getValue();
            verify(summonsNotificationEmailPayloadService).getEmailChannelForApplicationAddresseeParent(any(SummonsDataPrepared.class), eq(parentDocumentPayload), eq(SUMMONS_APPROVED_EMAIL_ADDRESS), eq(sendForRemotePrinting), any(UUID.class), eq(summonsRequired));
            assertThat(parentDocumentPayload.getAddressee(), notNullValue());
            assertThat(parentDocumentPayload.getAddressee().getName(), is("parent first name parent middle name parent last name"));
            assertThat(parentDocumentPayload.getAddressee().getAddress().getLine1(), is("parent address 1"));

        }
    }

    public void verifySummonsPayloadGeneratedForCaseSummons(final InitiationCode initiationCode, final SummonsType summonsRequired, final SummonsCode summonsCode, final boolean isWelsh, final boolean isYouth, final int numberOfDocuments, final boolean sendForRemotePrinting) {

        //Given
        final SummonsDataPrepared summonsDataPrepared = getSummonsDataPreparedForCase(summonsRequired);
        final JsonObject summonsDataPreparedAsJsonObject = objectToJsonObjectConverter.convert(summonsDataPrepared);
        final ProsecutionCase prosecutionCase = getProsecutionCase(initiationCode, summonsCode);
        final ProsecutionCaseIdentifier prosecutionCaseIdentifier = prosecutionCase.getProsecutionCaseIdentifier();
        final Optional<JsonObject> optionalCase = of(createObjectBuilder().add("prosecutionCase", objectToJsonObjectConverter.convert(prosecutionCase)).build());
        final Optional<JsonObject> courtCentreJson = getCourtCentreJson(isWelsh);
        final SummonsProsecutor summonsProsecutor = getSummonsProsecutor();
        final Optional<LjaDetails> ljaDetails = getLjaDetails();
        final String defendantTemplateName = randomAlphabetic(15);
        final String parentGuardianTemplateName = randomAlphabetic(15);
        final SummonsDocumentContent defendantTemplatePayload = getDefendantTemplatePayload(isYouth);

        when(envelope.payloadAsJsonObject()).thenReturn(summonsDataPreparedAsJsonObject);

        when(progressionService.getProsecutionCaseDetailById(envelope, CASE_ID.toString())).thenReturn(optionalCase);
        when(referenceDataService.getCourtCentreWithCourtRoomsById(COURT_CENTRE_ID, envelope, requester)).thenReturn(courtCentreJson);
        when(summonsService.getLjaDetails(envelope, LJA_CODE)).thenReturn(ljaDetails);
        when(summonsService.getProsecutor(eq(envelope), eq(prosecutionCaseIdentifier))).thenReturn(summonsProsecutor);
        when(summonsTemplateNameService.getCaseSummonsTemplateName(summonsRequired, summonsCode, isWelsh)).thenReturn(defendantTemplateName);
        when(summonsTemplateNameService.getCaseSummonsParentTemplateName(isWelsh)).thenReturn(parentGuardianTemplateName);
        when(caseDefendantSummonsService.generateSummonsPayloadForDefendant(eq(envelope), any(SummonsDataPrepared.class), any(ProsecutionCase.class), any(Defendant.class), any(ListDefendantRequest.class), eq(courtCentreJson.get()), eq(ljaDetails), eq(summonsProsecutor))).thenReturn(defendantTemplatePayload);
        when(summonsNotificationEmailPayloadService.getEmailChannelForCaseDefendant(any(SummonsDataPrepared.class), any(SummonsDocumentContent.class), eq(SUMMONS_APPROVED_EMAIL_ADDRESS), any(), any(Defendant.class), anyList(), eq(sendForRemotePrinting), anyBoolean(), any(UUID.class), eq(summonsRequired))).thenReturn(of(EMAIL_CHANNEL));
        when(summonsNotificationEmailPayloadService.getEmailChannelForCaseDefendantParent(any(SummonsDataPrepared.class), any(SummonsDocumentContent.class), eq(SUMMONS_APPROVED_EMAIL_ADDRESS), any(), any(Defendant.class), anyList(), eq(sendForRemotePrinting), any(UUID.class), eq(summonsRequired))).thenReturn(of(EMAIL_CHANNEL));

        summonsDataPreparedEventProcessor.requestSummons(envelope);

        //When
        verify(progressionService).getProsecutionCaseDetailById(envelope, CASE_ID.toString());
        verify(referenceDataService).getCourtCentreWithCourtRoomsById(COURT_CENTRE_ID, envelope, requester);
        verify(summonsService).getLjaDetails(envelope, LJA_CODE);
        verify(summonsService).getProsecutor(eq(envelope), eq(prosecutionCaseIdentifier));
        verify(summonsTemplateNameService).getCaseSummonsTemplateName(summonsRequired, summonsCode, isWelsh);
        verify(caseDefendantSummonsService).generateSummonsPayloadForDefendant(eq(envelope), any(SummonsDataPrepared.class), any(ProsecutionCase.class), any(Defendant.class), any(ListDefendantRequest.class), eq(courtCentreJson.get()), eq(ljaDetails), eq(summonsProsecutor));
        verify(publishSummonsDocumentService).generateCaseSummonsCourtDocument(eq(envelope), eq(DEFENDANT_ID), eq(CASE_ID), eq(defendantTemplatePayload), eq(defendantTemplateName), eq(sendForRemotePrinting), eq(EMAIL_CHANNEL), notNull(UUID.class));
        verify(summonsNotificationEmailPayloadService).getEmailChannelForCaseDefendant(any(SummonsDataPrepared.class), eq(defendantTemplatePayload), eq(SUMMONS_APPROVED_EMAIL_ADDRESS), any(), any(Defendant.class), anyList(), eq(sendForRemotePrinting), eq(isYouth), any(UUID.class), eq(summonsRequired));

        // verification for parent document (if applicable)
        if (numberOfDocuments > 1) {
            verify(summonsTemplateNameService).getCaseSummonsParentTemplateName(isWelsh);
            verify(publishSummonsDocumentService).generateCaseSummonsCourtDocument(eq(envelope), eq(DEFENDANT_ID), eq(CASE_ID), parentSummonsDocumentContentArgumentCaptor.capture(), eq(parentGuardianTemplateName), eq(sendForRemotePrinting), eq(EMAIL_CHANNEL), any(UUID.class));
            final SummonsDocumentContent parentDocumentPayload = parentSummonsDocumentContentArgumentCaptor.getValue();
            verify(summonsNotificationEmailPayloadService).getEmailChannelForCaseDefendantParent(any(SummonsDataPrepared.class), eq(parentDocumentPayload), eq(SUMMONS_APPROVED_EMAIL_ADDRESS), anyList(), any(Defendant.class), anyList(), eq(sendForRemotePrinting), any(UUID.class), eq(summonsRequired));
            assertThat(parentDocumentPayload.getAddressee(), notNullValue());
            assertThat(parentDocumentPayload.getAddressee().getName(), is("parent first name parent middle name parent last name"));
            assertThat(parentDocumentPayload.getAddressee().getAddress().getLine1(), is("parent address 1"));
        }

        verifyNoMoreInteractions(progressionService, referenceDataService, summonsService, summonsTemplateNameService, caseDefendantSummonsService, publishSummonsDocumentService, summonsNotificationEmailPayloadService);
    }

    private void verifySummonsPayloadGeneratedForApplications(final SummonsType summonsRequired, final boolean isWelsh, final boolean isYouth, final int numberOfDocuments, final boolean sendForRemotePrinting) {

        //Given
        final SummonsDataPrepared summonsDataPrepared = getSummonsDataPreparedForApplication(summonsRequired);
        final JsonObject summonsDataPreparedAsJsonObject = objectToJsonObjectConverter.convert(summonsDataPrepared);
        final Optional<JsonObject> optionalApplication = of(createObjectBuilder().add("courtApplication", objectToJsonObjectConverter.convert(getCourtApplication())).build());
        final Optional<JsonObject> courtCentreJson = getCourtCentreJson(isWelsh);
        final Optional<LjaDetails> ljaDetails = getLjaDetails();
        final String subjectTemplateName = randomAlphabetic(15);
        final String parentGuardianTemplateName = randomAlphabetic(15);
        final SummonsDocumentContent subjectTemplatePayload = getDefendantTemplatePayload(isYouth);

        when(envelope.payloadAsJsonObject()).thenReturn(summonsDataPreparedAsJsonObject);

        when(progressionService.getCourtApplicationById(envelope, APPLICATION_ID.toString())).thenReturn(optionalApplication);
        when(referenceDataService.getCourtCentreWithCourtRoomsById(COURT_CENTRE_ID, envelope, requester)).thenReturn(courtCentreJson);
        when(summonsService.getLjaDetails(envelope, LJA_CODE)).thenReturn(ljaDetails);
        when(applicantEmailAddressUtil.getApplicantEmailAddress(any(CourtApplication.class))).thenReturn(Optional.of(EMAIL_ADDRESS));
        when(summonsTemplateNameService.getApplicationTemplateName(summonsRequired, isWelsh)).thenReturn(subjectTemplateName);
        when(summonsTemplateNameService.getBreachSummonsParentTemplateName(isWelsh)).thenReturn(parentGuardianTemplateName);
        when(applicationSummonsService.generateSummonsDocumentContent(any(SummonsDataPrepared.class), any(CourtApplication.class), any(CourtApplicationPartyListingNeeds.class), eq(courtCentreJson.get()), eq(ljaDetails))).thenReturn(subjectTemplatePayload);
        when(summonsNotificationEmailPayloadService.getEmailChannelForApplicationAddressee(any(SummonsDataPrepared.class), any(SummonsDocumentContent.class), eq(SUMMONS_APPROVED_EMAIL_ADDRESS), eq(sendForRemotePrinting), anyBoolean(), any(UUID.class), eq(summonsRequired))).thenReturn(of(EMAIL_CHANNEL));
        when(summonsNotificationEmailPayloadService.getEmailChannelForApplicationAddresseeParent(any(SummonsDataPrepared.class), any(SummonsDocumentContent.class), eq(SUMMONS_APPROVED_EMAIL_ADDRESS), eq(sendForRemotePrinting), any(UUID.class), eq(summonsRequired))).thenReturn(of(EMAIL_CHANNEL));

        summonsDataPreparedEventProcessor.requestSummons(envelope);

        //When
        verify(progressionService).getCourtApplicationById(envelope, APPLICATION_ID.toString());
        verify(referenceDataService).getCourtCentreWithCourtRoomsById(COURT_CENTRE_ID, envelope, requester);
        verify(summonsService).getLjaDetails(envelope, LJA_CODE);
        verify(summonsTemplateNameService).getApplicationTemplateName(summonsRequired, isWelsh);
        verify(applicationSummonsService).generateSummonsDocumentContent(any(SummonsDataPrepared.class), any(CourtApplication.class), any(CourtApplicationPartyListingNeeds.class), eq(courtCentreJson.get()), eq(ljaDetails));
        verify(publishSummonsDocumentService).generateApplicationSummonsCourtDocument(eq(envelope), eq(APPLICATION_ID), eq(subjectTemplatePayload), eq(subjectTemplateName), eq(sendForRemotePrinting), eq(EMAIL_CHANNEL), notNull(UUID.class));
        verify(summonsNotificationEmailPayloadService).getEmailChannelForApplicationAddressee(any(SummonsDataPrepared.class), eq(subjectTemplatePayload), eq(SUMMONS_APPROVED_EMAIL_ADDRESS), eq(sendForRemotePrinting), eq(isYouth), any(UUID.class), eq(summonsRequired));

        // verification for parent document (if applicable)
        if (numberOfDocuments > 1) {
            verify(summonsTemplateNameService).getBreachSummonsParentTemplateName(isWelsh);
            verify(publishSummonsDocumentService).generateApplicationSummonsCourtDocument(eq(envelope), eq(APPLICATION_ID), parentSummonsDocumentContentArgumentCaptor.capture(), eq(parentGuardianTemplateName), eq(sendForRemotePrinting), eq(EMAIL_CHANNEL), notNull(UUID.class));
            final SummonsDocumentContent parentDocumentPayload = parentSummonsDocumentContentArgumentCaptor.getValue();
            verify(summonsNotificationEmailPayloadService).getEmailChannelForApplicationAddresseeParent(any(SummonsDataPrepared.class), eq(parentDocumentPayload), eq(SUMMONS_APPROVED_EMAIL_ADDRESS), eq(sendForRemotePrinting), any(UUID.class), eq(summonsRequired));
            assertThat(parentDocumentPayload.getAddressee(), notNullValue());
            assertThat(parentDocumentPayload.getAddressee().getName(), is("parent first name parent middle name parent last name"));
            assertThat(parentDocumentPayload.getAddressee().getAddress().getLine1(), is("parent address 1"));

        }

        verifyNoMoreInteractions(progressionService, referenceDataService, summonsService, summonsTemplateNameService, caseDefendantSummonsService, publishSummonsDocumentService, summonsNotificationEmailPayloadService);
    }

    private SummonsProsecutor getSummonsProsecutor() {
        return summonsProsecutor()
                .withName("pros name")
                .withEmailAddress("random email")
                .build();
    }

    private ProsecutionCase getProsecutionCase(final InitiationCode initiationCode, final SummonsCode summonsCode) {
        final AssociatedPerson associatedPerson = getAssociatedPerson();
        final Defendant defendant = defendant()
                .withId(DEFENDANT_ID)
                .withAssociatedPersons(newArrayList(associatedPerson))
                .build();
        return prosecutionCase()
                .withInitiationCode(initiationCode)
                .withSummonsCode(Optional.ofNullable(summonsCode).map(SummonsCode::getCode).orElse(null))
                .withProsecutionCaseIdentifier(prosecutionCaseIdentifier()
                        .withProsecutionAuthorityId(PROSECUTION_AUTHORITY_ID)
                        .build())
                .withDefendants(newArrayList(defendant))
                .build();
    }

    private CourtApplication getCourtApplication() {
        final ProsecutingAuthority prosecutingAuthority = prosecutingAuthority().withProsecutionAuthorityId(PROSECUTION_AUTHORITY_ID).build();
        final CourtApplicationParty applicant = courtApplicationParty().withProsecutingAuthority(prosecutingAuthority).build();
        final CourtApplicationParty subject = courtApplicationParty()
                .withMasterDefendant(masterDefendant().withAssociatedPersons(newArrayList(getAssociatedPerson())).build())
                .withId(SUBJECT_ID)
                .build();
        return CourtApplication.courtApplication()
                .withId(APPLICATION_ID)
                .withApplicant(applicant)
                .withSubject(subject)
                .build();

    }

    private AssociatedPerson getAssociatedPerson() {
        return associatedPerson().withPerson(
                person()
                        .withFirstName("parent first name")
                        .withMiddleName("parent middle name")
                        .withLastName("parent last name")
                        .withAddress(address()
                                .withAddress1("parent address 1")
                                .build())
                        .build()).build();
    }

    private SummonsDataPrepared getSummonsDataPreparedForCase(final SummonsType summonsRequired) {

        final SummonsData summonsData = summonsData()
                .withConfirmedProsecutionCaseIds(newArrayList(confirmedProsecutionCaseId()
                        .withId(CASE_ID)
                        .withConfirmedDefendantIds(singletonList(DEFENDANT_ID))
                        .build()))
                .withCourtCentre(courtCentre()
                        .withId(COURT_CENTRE_ID)
                        .withRoomId(COURT_ROOM_ID)
                        .build())
                .withHearingDateTime(HEARING_DATE_TIME)
                .withListDefendantRequests(newArrayList(listDefendantRequest()
                        .withSummonsRequired(summonsRequired)
                        .withProsecutionCaseId(CASE_ID)
                        .withDefendantId(DEFENDANT_ID)
                        .withSummonsApprovedOutcome(getSummonsApprovedOutcome())
                        .build()))
                .build();

        return summonsDataPrepared().withSummonsData(summonsData).build();
    }

    private SummonsDataPrepared getSummonsDataPreparedForApplication(final SummonsType summonsRequired) {

        final SummonsData summonsData = summonsData()
                .withConfirmedApplicationIds(newArrayList(APPLICATION_ID))
                .withCourtCentre(courtCentre()
                        .withId(COURT_CENTRE_ID)
                        .withRoomId(COURT_ROOM_ID)
                        .build())
                .withHearingDateTime(HEARING_DATE_TIME)
                .withCourtApplicationPartyListingNeeds(newArrayList(CourtApplicationPartyListingNeeds.courtApplicationPartyListingNeeds()
                        .withSummonsRequired(summonsRequired)
                        .withCourtApplicationId(APPLICATION_ID)
                        .withCourtApplicationPartyId(SUBJECT_ID)
                        .withSummonsApprovedOutcome(getSummonsApprovedOutcome())
                        .build()))
                .build();

        return summonsDataPrepared().withSummonsData(summonsData).build();
    }

    private SummonsApprovedOutcome getSummonsApprovedOutcome() {
        return summonsApprovedOutcome()
                .withProsecutorCost("£300.00")
                .withPersonalService(true)
                .withSummonsSuppressed(summonsSuppressed)
                .withProsecutorEmailAddress(SUMMONS_APPROVED_EMAIL_ADDRESS)
                .build();
    }

    private Optional<JsonObject> getCourtCentreJson(final boolean isWelsh) {
        return of(createObjectBuilder()
                .add("lja", LJA_CODE)
                .add("isWelsh", isWelsh)
                .build()
        );
    }

    private Optional<LjaDetails> getLjaDetails() {
        return of(ljaDetails().withLjaCode(LJA_CODE).withLjaName("ljaName").withWelshLjaName("welshLjaName").build());
    }

    private SummonsDocumentContent getDefendantTemplatePayload(final boolean isYouth) {
        final ZonedDateTime defendantDateOfBirth = isYouth ? HEARING_DATE_TIME.minusYears(16) : HEARING_DATE_TIME.minusYears(20);
        return summonsDocumentContent().withDefendant(SummonsDefendant.summonsDefendant().withDateOfBirth(DateTimeFormatter.ofPattern("yyyy-MM-dd").format(defendantDateOfBirth)).build()).build();
    }

}
