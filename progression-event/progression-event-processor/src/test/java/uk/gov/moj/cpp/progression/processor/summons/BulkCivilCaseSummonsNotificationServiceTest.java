package uk.gov.moj.cpp.progression.processor.summons;

import static com.google.common.collect.Lists.newArrayList;
import static java.util.Collections.singletonList;
import static java.util.UUID.randomUUID;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static uk.gov.justice.core.courts.ConfirmedProsecutionCaseId.confirmedProsecutionCaseId;
import static uk.gov.justice.core.courts.CourtApplication.courtApplication;
import static uk.gov.justice.core.courts.CourtApplicationCase.courtApplicationCase;
import static uk.gov.justice.core.courts.CourtCentre.courtCentre;
import static uk.gov.justice.core.courts.CourtCivilApplication.courtCivilApplication;
import static uk.gov.justice.core.courts.Defendant.defendant;
import static uk.gov.justice.core.courts.ListDefendantRequest.listDefendantRequest;
import static uk.gov.justice.core.courts.Person.person;
import static uk.gov.justice.core.courts.PersonDefendant.personDefendant;
import static uk.gov.justice.core.courts.ProsecutionCase.prosecutionCase;
import static uk.gov.justice.core.courts.ProsecutionCaseIdentifier.prosecutionCaseIdentifier;
import static uk.gov.justice.core.courts.SummonsApprovedOutcome.summonsApprovedOutcome;
import static uk.gov.justice.core.courts.SummonsData.summonsData;
import static uk.gov.justice.core.courts.SummonsDataPrepared.summonsDataPrepared;
import static uk.gov.justice.core.courts.SummonsType.FIRST_HEARING;
import static uk.gov.justice.core.courts.notification.EmailChannel.emailChannel;
import static uk.gov.justice.services.messaging.JsonObjects.createObjectBuilder;

import uk.gov.justice.core.courts.CourtApplication;
import uk.gov.justice.core.courts.Defendant;
import uk.gov.justice.core.courts.InitiationCode;
import uk.gov.justice.core.courts.ProsecutionCase;
import uk.gov.justice.core.courts.SummonsApprovedOutcome;
import uk.gov.justice.core.courts.SummonsData;
import uk.gov.justice.core.courts.SummonsDataPrepared;
import uk.gov.justice.core.courts.notification.EmailChannel;
import uk.gov.justice.core.courts.summons.SummonsDocument;
import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.common.converter.ObjectToJsonObjectConverter;
import uk.gov.justice.services.common.converter.ZonedDateTimes;
import uk.gov.justice.services.common.converter.jackson.ObjectMapperProducer;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.progression.service.NotificationService;
import uk.gov.moj.cpp.progression.service.ProgressionService;

import java.time.ZonedDateTime;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import javax.json.JsonObject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class BulkCivilCaseSummonsNotificationServiceTest {

    private static final UUID COURT_CENTRE_ID = randomUUID();
    private static final UUID COURT_ROOM_ID = randomUUID();
    private static final ZonedDateTime HEARING_DATE_TIME = ZonedDateTimes.fromString("2018-04-01T13:00:00.000Z");
    private static final String SUMMONS_APPROVED_EMAIL_ADDRESS = "prosecutor@test.com";

    @Spy
    private final ObjectToJsonObjectConverter objectToJsonObjectConverter = new ObjectToJsonObjectConverter(new ObjectMapperProducer().objectMapper());

    @Spy
    private final JsonObjectToObjectConverter jsonObjectToObjectConverter = new JsonObjectToObjectConverter(new ObjectMapperProducer().objectMapper());

    @Mock
    private ProgressionService progressionService;

    @Mock
    private SummonsNotificationEmailPayloadService summonsNotificationEmailPayloadService;

    @Mock
    private NotificationService notificationService;

    @Mock
    private JsonEnvelope envelope;

    @Captor
    private ArgumentCaptor<SummonsDocument> summonsDocumentArgumentCaptor;

    @InjectMocks
    private BulkCivilCaseSummonsNotificationService bulkCivilCaseSummonsNotificationService;

    @Test
    public void shouldIdentifyBulkCivilCase() {
        final ProsecutionCase groupMember = prosecutionCase().withIsCivil(true).withIsGroupMember(true).build();
        final ProsecutionCase groupMaster = prosecutionCase().withIsCivil(true).withIsGroupMaster(true).build();

        assertThat(bulkCivilCaseSummonsNotificationService.isBulkCivilCase(groupMember), is(true));
        assertThat(bulkCivilCaseSummonsNotificationService.isBulkCivilCase(groupMaster), is(true));
    }

    @Test
    public void shouldNotIdentifyNonCivilGroupCaseAsBulkCivilCase() {
        final ProsecutionCase nonCivilGroupMember = prosecutionCase().withIsCivil(false).withIsGroupMember(true).build();
        assertThat(bulkCivilCaseSummonsNotificationService.isBulkCivilCase(nonCivilGroupMember), is(false));
    }

    @Test
    public void shouldNotIdentifyStandaloneCivilCaseAsBulkCivilCase() {
        final ProsecutionCase standaloneCivilCase = prosecutionCase().withIsCivil(true).build();
        assertThat(bulkCivilCaseSummonsNotificationService.isBulkCivilCase(standaloneCivilCase), is(false));
    }

    @Test
    public void shouldNotNotifyWhenNoConfirmedDefendantMatchesARequiredScenario() {
        final UUID caseId = randomUUID();
        final UUID defendantId = randomUUID();
        final UUID groupId = randomUUID();

        final SummonsDataPrepared summonsDataPrepared = summonsDataPrepared().withSummonsData(
                summonsData().withListDefendantRequests(newArrayList()).build()).build();
        final ProsecutionCase bulkProsecutionCase = prosecutionCase().withId(caseId).withGroupId(groupId).withIsCivil(true).withIsGroupMember(true).build();

        bulkCivilCaseSummonsNotificationService.handleBulkCivilCaseSummons(envelope, summonsDataPrepared, createObjectBuilder().build(),
                bulkProsecutionCase, singletonList(defendantId), new HashSet<>());

        verifyNoInteractions(progressionService, summonsNotificationEmailPayloadService, notificationService);
    }

    @Test
    public void shouldSuppressSummonsAndSendOneBulkNotificationUsingTheGroupMasterCaseContent() {
        final UUID caseId = randomUUID();
        final UUID defendantId = randomUUID();
        final UUID groupId = randomUUID();
        final UUID masterCaseId = randomUUID();
        final UUID leadDefendantId = randomUUID();

        final SummonsDataPrepared summonsDataPrepared = getSummonsDataPreparedForCase(caseId, defendantId);
        final ProsecutionCase bulkProsecutionCase = prosecutionCase()
                .withId(caseId)
                .withInitiationCode(InitiationCode.S)
                .withSummonsCode(SummonsCode.APPLICATION.getCode())
                .withIsCivil(true)
                .withIsGroupMember(true)
                .withGroupId(groupId)
                .withProsecutionCaseIdentifier(prosecutionCaseIdentifier().withCaseURN("MEMBER-URN").build())
                .withDefendants(newArrayList(defendant().withId(defendantId).build()))
                .build();

        final JsonObject masterCaseJson = objectToJsonObjectConverter.convert(getMasterCase(masterCaseId, leadDefendantId));
        final EmailChannel bulkEmailChannel = emailChannel().build();
        final JsonObject courtCentreJson = createObjectBuilder().build();

        when(progressionService.getMasterProsecutionCaseByGroupId(envelope, groupId)).thenReturn(Optional.of(masterCaseJson));
        when(summonsNotificationEmailPayloadService.getEmailChannelForBulkCaseSummonsApproved(any(SummonsDataPrepared.class), any(SummonsDocument.class),
                eq(SUMMONS_APPROVED_EMAIL_ADDRESS), eq("Lead Defendant, LEAD-REF"))).thenReturn(Optional.of(bulkEmailChannel));

        bulkCivilCaseSummonsNotificationService.handleBulkCivilCaseSummons(envelope, summonsDataPrepared, courtCentreJson,
                bulkProsecutionCase, singletonList(defendantId), new HashSet<>());

        verify(progressionService).getMasterProsecutionCaseByGroupId(envelope, groupId);
        verify(summonsNotificationEmailPayloadService).getEmailChannelForBulkCaseSummonsApproved(any(SummonsDataPrepared.class),
                summonsDocumentArgumentCaptor.capture(), eq(SUMMONS_APPROVED_EMAIL_ADDRESS), eq("Lead Defendant, LEAD-REF"));
        // case reference comes from the GROUP MASTER case, not the triggering member case -
        // kept consistent with the lead defendant, which is also the master's.
        assertThat(summonsDocumentArgumentCaptor.getValue().getCaseReference(), is("MASTER-URN"));
        verify(notificationService).sendEmail(eq(envelope), eq(masterCaseId), isNull(), isNull(), eq(singletonList(bulkEmailChannel)));
    }

    @Test
    public void shouldNotifyOnlyOnceWhenTheSameGroupIsHandledTwice() {
        final UUID caseId1 = randomUUID();
        final UUID caseId2 = randomUUID();
        final UUID defendantId1 = randomUUID();
        final UUID defendantId2 = randomUUID();
        final UUID groupId = randomUUID();
        final UUID masterCaseId = randomUUID();
        final UUID leadDefendantId = randomUUID();

        final SummonsDataPrepared summonsDataPrepared = summonsDataPrepared().withSummonsData(
                summonsData()
                        .withCourtCentre(courtCentre().withId(COURT_CENTRE_ID).withRoomId(COURT_ROOM_ID).build())
                        .withHearingDateTime(HEARING_DATE_TIME)
                        .withListDefendantRequests(newArrayList(
                                listDefendantRequest().withSummonsRequired(FIRST_HEARING).withProsecutionCaseId(caseId1).withDefendantId(defendantId1)
                                        .withSummonsApprovedOutcome(getSummonsApprovedOutcome()).build(),
                                listDefendantRequest().withSummonsRequired(FIRST_HEARING).withProsecutionCaseId(caseId2).withDefendantId(defendantId2)
                                        .withSummonsApprovedOutcome(getSummonsApprovedOutcome()).build()))
                        .withConfirmedProsecutionCaseIds(newArrayList(
                                confirmedProsecutionCaseId().withId(caseId1).withConfirmedDefendantIds(singletonList(defendantId1)).build(),
                                confirmedProsecutionCaseId().withId(caseId2).withConfirmedDefendantIds(singletonList(defendantId2)).build()))
                        .build()
        ).build();

        final ProsecutionCase memberCase1 = prosecutionCase()
                .withId(caseId1).withInitiationCode(InitiationCode.S).withSummonsCode(SummonsCode.APPLICATION.getCode())
                .withIsCivil(true).withIsGroupMember(true).withGroupId(groupId)
                .withProsecutionCaseIdentifier(prosecutionCaseIdentifier().withCaseURN("MEMBER-URN-1").build())
                .withDefendants(newArrayList(defendant().withId(defendantId1).build()))
                .build();
        final ProsecutionCase memberCase2 = prosecutionCase()
                .withId(caseId2).withInitiationCode(InitiationCode.S).withSummonsCode(SummonsCode.APPLICATION.getCode())
                .withIsCivil(true).withIsGroupMember(true).withGroupId(groupId)
                .withProsecutionCaseIdentifier(prosecutionCaseIdentifier().withCaseURN("MEMBER-URN-2").build())
                .withDefendants(newArrayList(defendant().withId(defendantId2).build()))
                .build();

        final JsonObject masterCaseJson = objectToJsonObjectConverter.convert(getMasterCase(masterCaseId, leadDefendantId));
        final EmailChannel bulkEmailChannel = emailChannel().build();
        final JsonObject courtCentreJson = createObjectBuilder().build();

        when(progressionService.getMasterProsecutionCaseByGroupId(envelope, groupId)).thenReturn(Optional.of(masterCaseJson));
        when(summonsNotificationEmailPayloadService.getEmailChannelForBulkCaseSummonsApproved(any(SummonsDataPrepared.class), any(SummonsDocument.class),
                eq(SUMMONS_APPROVED_EMAIL_ADDRESS), eq("Lead Defendant, LEAD-REF"))).thenReturn(Optional.of(bulkEmailChannel));

        final Set<UUID> notifiedGroupIds = new HashSet<>();
        bulkCivilCaseSummonsNotificationService.handleBulkCivilCaseSummons(envelope, summonsDataPrepared, courtCentreJson, memberCase1, singletonList(defendantId1), notifiedGroupIds);
        bulkCivilCaseSummonsNotificationService.handleBulkCivilCaseSummons(envelope, summonsDataPrepared, courtCentreJson, memberCase2, singletonList(defendantId2), notifiedGroupIds);

        verify(progressionService, times(1)).getMasterProsecutionCaseByGroupId(envelope, groupId);
        verify(summonsNotificationEmailPayloadService, times(1)).getEmailChannelForBulkCaseSummonsApproved(any(SummonsDataPrepared.class), any(SummonsDocument.class),
                eq(SUMMONS_APPROVED_EMAIL_ADDRESS), eq("Lead Defendant, LEAD-REF"));
        verify(notificationService, times(1)).sendEmail(eq(envelope), eq(masterCaseId), isNull(), isNull(), eq(singletonList(bulkEmailChannel)));
    }

    @Test
    public void shouldNotSendNotificationWhenGroupMasterCaseNotFound() {
        final UUID caseId = randomUUID();
        final UUID defendantId = randomUUID();
        final UUID groupId = randomUUID();

        final SummonsDataPrepared summonsDataPrepared = getSummonsDataPreparedForCase(caseId, defendantId);
        final ProsecutionCase bulkProsecutionCase = prosecutionCase()
                .withId(caseId).withInitiationCode(InitiationCode.S).withSummonsCode(SummonsCode.APPLICATION.getCode())
                .withIsCivil(true).withIsGroupMember(true).withGroupId(groupId)
                .withDefendants(newArrayList(defendant().withId(defendantId).build()))
                .build();

        when(progressionService.getMasterProsecutionCaseByGroupId(envelope, groupId)).thenReturn(Optional.empty());

        bulkCivilCaseSummonsNotificationService.handleBulkCivilCaseSummons(envelope, summonsDataPrepared, createObjectBuilder().build(),
                bulkProsecutionCase, singletonList(defendantId), new HashSet<>());

        verify(progressionService).getMasterProsecutionCaseByGroupId(envelope, groupId);
        verify(notificationService, never()).sendEmail(any(), any(), any(), any(), any());
    }

    @Test
    public void shouldIdentifyBulkCivilCaseApplication() {
        final CourtApplication bulkCivilApplication = courtApplication()
                .withIsGroupCaseApplication(true)
                .withCourtCivilApplication(courtCivilApplication().withIsCivil(true).build())
                .build();

        assertThat(bulkCivilCaseSummonsNotificationService.isBulkCivilCaseApplication(bulkCivilApplication), is(true));
    }

    @Test
    public void shouldNotIdentifyNonCivilGroupApplicationAsBulkCivilCaseApplication() {
        final CourtApplication nonCivilGroupApplication = courtApplication()
                .withIsGroupCaseApplication(true)
                .withCourtCivilApplication(courtCivilApplication().withIsCivil(false).build())
                .build();

        assertThat(bulkCivilCaseSummonsNotificationService.isBulkCivilCaseApplication(nonCivilGroupApplication), is(false));
    }

    @Test
    public void shouldNotIdentifyStandaloneCivilApplicationAsBulkCivilCaseApplication() {
        final CourtApplication standaloneCivilApplication = courtApplication()
                .withCourtCivilApplication(courtCivilApplication().withIsCivil(true).build())
                .build();

        assertThat(bulkCivilCaseSummonsNotificationService.isBulkCivilCaseApplication(standaloneCivilApplication), is(false));
    }

    @Test
    public void shouldSuppressSummonsAndSendOneBulkNotificationForBulkCivilCaseApplicationUsingTheGroupMasterCaseContent() {
        final UUID applicationId = randomUUID();
        final UUID linkedCaseId = randomUUID();
        final UUID groupId = randomUUID();
        final UUID masterCaseId = randomUUID();
        final UUID leadDefendantId = randomUUID();

        final SummonsDataPrepared summonsDataPrepared = getSummonsDataPreparedForCase(randomUUID(), randomUUID());
        final CourtApplication bulkCivilApplication = courtApplication()
                .withId(applicationId)
                .withIsGroupCaseApplication(true)
                .withCourtCivilApplication(courtCivilApplication().withIsCivil(true).build())
                .withCourtApplicationCases(newArrayList(courtApplicationCase().withProsecutionCaseId(linkedCaseId).build()))
                .build();
        final SummonsApprovedOutcome summonsApprovedOutcome = getSummonsApprovedOutcome();

        final JsonObject linkedCaseJson = objectToJsonObjectConverter.convert(
                prosecutionCase().withId(linkedCaseId).withGroupId(groupId).build());
        final JsonObject prosecutionCaseEnvelope = createObjectBuilder().add("prosecutionCase", linkedCaseJson).build();
        final JsonObject masterCaseJson = objectToJsonObjectConverter.convert(getMasterCase(masterCaseId, leadDefendantId));
        final EmailChannel bulkEmailChannel = emailChannel().build();
        final JsonObject courtCentreJson = createObjectBuilder().build();

        when(progressionService.getProsecutionCaseDetailById(envelope, linkedCaseId.toString())).thenReturn(Optional.of(prosecutionCaseEnvelope));
        when(progressionService.getMasterProsecutionCaseByGroupId(envelope, groupId)).thenReturn(Optional.of(masterCaseJson));
        when(summonsNotificationEmailPayloadService.getEmailChannelForBulkCaseSummonsApproved(any(SummonsDataPrepared.class), any(SummonsDocument.class),
                eq(SUMMONS_APPROVED_EMAIL_ADDRESS), eq("Lead Defendant, LEAD-REF"))).thenReturn(Optional.of(bulkEmailChannel));

        bulkCivilCaseSummonsNotificationService.handleBulkCivilCaseApplicationSummons(envelope, summonsDataPrepared, courtCentreJson,
                bulkCivilApplication, summonsApprovedOutcome, new HashSet<>());

        verify(progressionService).getMasterProsecutionCaseByGroupId(envelope, groupId);
        verify(summonsNotificationEmailPayloadService).getEmailChannelForBulkCaseSummonsApproved(any(SummonsDataPrepared.class),
                summonsDocumentArgumentCaptor.capture(), eq(SUMMONS_APPROVED_EMAIL_ADDRESS), eq("Lead Defendant, LEAD-REF"));
        assertThat(summonsDocumentArgumentCaptor.getValue().getCaseReference(), is("MASTER-URN"));
        verify(notificationService).sendEmail(eq(envelope), eq(masterCaseId), isNull(), isNull(), eq(singletonList(bulkEmailChannel)));
    }

    @Test
    public void shouldNotifyOnlyOnceForBulkCivilCaseApplicationWhenTheSameGroupIsHandledTwice() {
        final UUID linkedCaseId = randomUUID();
        final UUID groupId = randomUUID();
        final UUID masterCaseId = randomUUID();
        final UUID leadDefendantId = randomUUID();

        final SummonsDataPrepared summonsDataPrepared = getSummonsDataPreparedForCase(randomUUID(), randomUUID());
        final CourtApplication application1 = courtApplication()
                .withId(randomUUID()).withIsGroupCaseApplication(true)
                .withCourtCivilApplication(courtCivilApplication().withIsCivil(true).build())
                .withCourtApplicationCases(newArrayList(courtApplicationCase().withProsecutionCaseId(linkedCaseId).build()))
                .build();
        final CourtApplication application2 = courtApplication()
                .withId(randomUUID()).withIsGroupCaseApplication(true)
                .withCourtCivilApplication(courtCivilApplication().withIsCivil(true).build())
                .withCourtApplicationCases(newArrayList(courtApplicationCase().withProsecutionCaseId(linkedCaseId).build()))
                .build();
        final SummonsApprovedOutcome summonsApprovedOutcome = getSummonsApprovedOutcome();

        final JsonObject linkedCaseJson = objectToJsonObjectConverter.convert(
                prosecutionCase().withId(linkedCaseId).withGroupId(groupId).build());
        final JsonObject prosecutionCaseEnvelope = createObjectBuilder().add("prosecutionCase", linkedCaseJson).build();
        final JsonObject masterCaseJson = objectToJsonObjectConverter.convert(getMasterCase(masterCaseId, leadDefendantId));
        final EmailChannel bulkEmailChannel = emailChannel().build();
        final JsonObject courtCentreJson = createObjectBuilder().build();

        when(progressionService.getProsecutionCaseDetailById(envelope, linkedCaseId.toString())).thenReturn(Optional.of(prosecutionCaseEnvelope));
        when(progressionService.getMasterProsecutionCaseByGroupId(envelope, groupId)).thenReturn(Optional.of(masterCaseJson));
        when(summonsNotificationEmailPayloadService.getEmailChannelForBulkCaseSummonsApproved(any(SummonsDataPrepared.class), any(SummonsDocument.class),
                eq(SUMMONS_APPROVED_EMAIL_ADDRESS), eq("Lead Defendant, LEAD-REF"))).thenReturn(Optional.of(bulkEmailChannel));

        final Set<UUID> notifiedGroupIds = new HashSet<>();
        bulkCivilCaseSummonsNotificationService.handleBulkCivilCaseApplicationSummons(envelope, summonsDataPrepared, courtCentreJson, application1, summonsApprovedOutcome, notifiedGroupIds);
        bulkCivilCaseSummonsNotificationService.handleBulkCivilCaseApplicationSummons(envelope, summonsDataPrepared, courtCentreJson, application2, summonsApprovedOutcome, notifiedGroupIds);

        verify(progressionService, times(1)).getMasterProsecutionCaseByGroupId(envelope, groupId);
        verify(notificationService, times(1)).sendEmail(eq(envelope), eq(masterCaseId), isNull(), isNull(), eq(singletonList(bulkEmailChannel)));
    }

    @Test
    public void shouldNotSendNotificationForBulkCivilCaseApplicationWhenNoLinkedCaseFound() {
        final CourtApplication applicationWithNoLinkedCase = courtApplication()
                .withId(randomUUID()).withIsGroupCaseApplication(true)
                .withCourtCivilApplication(courtCivilApplication().withIsCivil(true).build())
                .build();

        bulkCivilCaseSummonsNotificationService.handleBulkCivilCaseApplicationSummons(envelope, getSummonsDataPreparedForCase(randomUUID(), randomUUID()),
                createObjectBuilder().build(), applicationWithNoLinkedCase, getSummonsApprovedOutcome(), new HashSet<>());

        verifyNoInteractions(summonsNotificationEmailPayloadService, notificationService);
        verify(progressionService, never()).getMasterProsecutionCaseByGroupId(any(), any());
    }

    private ProsecutionCase getMasterCase(final UUID masterCaseId, final UUID leadDefendantId) {
        final Defendant leadDefendant = defendant()
                .withId(leadDefendantId)
                .withProsecutionAuthorityReference("LEAD-REF")
                .withPersonDefendant(personDefendant().withPersonDetails(person().withFirstName("Lead").withLastName("Defendant").build()).build())
                .build();
        return prosecutionCase()
                .withId(masterCaseId)
                .withProsecutionCaseIdentifier(prosecutionCaseIdentifier().withCaseURN("MASTER-URN").build())
                .withDefendants(newArrayList(leadDefendant))
                .build();
    }

    private SummonsDataPrepared getSummonsDataPreparedForCase(final UUID caseId, final UUID defendantId) {
        final SummonsData summonsData = summonsData()
                .withCourtCentre(courtCentre().withId(COURT_CENTRE_ID).withRoomId(COURT_ROOM_ID).build())
                .withHearingDateTime(HEARING_DATE_TIME)
                .withListDefendantRequests(newArrayList(listDefendantRequest()
                        .withSummonsRequired(FIRST_HEARING)
                        .withProsecutionCaseId(caseId)
                        .withDefendantId(defendantId)
                        .withSummonsApprovedOutcome(getSummonsApprovedOutcome())
                        .build()))
                .build();
        return summonsDataPrepared().withSummonsData(summonsData).build();
    }

    private SummonsApprovedOutcome getSummonsApprovedOutcome() {
        return summonsApprovedOutcome()
                .withProsecutorCost("£300.00")
                .withPersonalService(true)
                .withProsecutorEmailAddress(SUMMONS_APPROVED_EMAIL_ADDRESS)
                .build();
    }
}
