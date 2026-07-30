package uk.gov.moj.cpp.progression.processor;

import static java.util.Optional.ofNullable;
import static java.util.UUID.randomUUID;
import static javax.json.Json.createArrayBuilder;
import static javax.json.Json.createObjectBuilder;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.test.utils.core.reflection.ReflectionUtil.setField;

import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.common.converter.jackson.ObjectMapperProducer;
import uk.gov.justice.services.core.requester.Requester;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.test.utils.core.messaging.MetadataBuilderFactory;
import uk.gov.moj.cpp.progression.service.NotificationService;
import uk.gov.moj.cpp.progression.service.ProgressionService;
import uk.gov.moj.cpp.progression.service.RefDataService;
import uk.gov.moj.cpp.progression.service.UsersGroupService;
import uk.gov.moj.cpp.progression.value.object.DefenceOrganisationVO;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import javax.json.JsonObject;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CPSEmailNotificationProcessorTest {

    @Spy
    private JsonObjectToObjectConverter jsonObjectToObjectConverter;

    @InjectMocks
    private CPSEmailNotificationProcessor cpsEmailNotificationProcessor;

    @Mock
    private UsersGroupService usersGroupService;

    @Mock
    private RefDataService referenceDataService;

    @Mock
    private NotificationService notificationService;

    @Mock
    private ProgressionService progressionService;

    @Mock
    private Requester requester;

    @Mock
    private Sender sender;

    @Captor
    private ArgumentCaptor<Envelope<JsonObject>> envelopeCaptor;

    @BeforeEach
    void initMocks() {
        setField(this.jsonObjectToObjectConverter, "objectMapper", new ObjectMapperProducer().objectMapper());
    }

    @Nested
    class CivilCase {

        @Test
        void shouldSendCPSNotificationUsingMcEmailWhenProsecutorIsCpsAndJurisdictionIsMagistrates() {
            final UUID caseId = randomUUID();
            final UUID defendantId = randomUUID();
            final UUID organisationId = randomUUID();
            final UUID hearingId = randomUUID();
            final UUID courtCentreId = randomUUID();

            final JsonObject cpsProsecutionCaseResponse = createObjectBuilder()
                    .add("prosecutionCase", createObjectBuilder()
                            .add("id", caseId.toString())
                            .add("isCivil", true)
                            .add("prosecutor", createObjectBuilder()
                                    .add("isCps", true)
                                    .add("cpsMcEmailAddress", "cps-mc@cps.gov.uk")
                                    .build())
                            .add("prosecutionCaseIdentifier", createObjectBuilder()
                                    .add("prosecutionAuthorityReference", "TVL12345")
                                    .add("prosecutionAuthorityCode", "TVL")
                                    .build())
                            .add("defendants", createArrayBuilder()
                                    .add(createObjectBuilder()
                                            .add("id", defendantId.toString())
                                            .add("personDefendant", createObjectBuilder()
                                                    .add("personDetails", createObjectBuilder()
                                                            .add("firstName", "Fred")
                                                            .add("lastName", "Smith")
                                                            .build())
                                                    .build())
                                            .build())
                                    .build())
                            .build())
                    .add("hearingsAtAGlance", createObjectBuilder()
                            .add("latestHearingJurisdictionType", "MAGISTRATES")
                            .add("hearings", createArrayBuilder()
                                    .add(createObjectBuilder()
                                            .add("id", hearingId.toString())
                                            .add("hearingListingStatus", "HEARING_INITIALISED")
                                            .add("courtCentre", createObjectBuilder()
                                                    .add("id", courtCentreId.toString())
                                                    .add("name", "Test Court")
                                                    .build())
                                            .add("hearingDays", createArrayBuilder()
                                                    .add(createObjectBuilder()
                                                            .add("sittingDay", "2099-12-31T09:00:00.000Z")
                                                            .build())
                                                    .build())
                                            .build())
                                    .build())
                            .build())
                    .build();

            final JsonObject instructionPayload = createObjectBuilder()
                    .add("firstInstruction", true)
                    .add("caseId", caseId.toString())
                    .add("defendantId", defendantId.toString())
                    .add("organisationId", organisationId.toString())
                    .build();

            final JsonEnvelope envelope = JsonEnvelope.envelopeFrom(
                    MetadataBuilderFactory.metadataWithRandomUUID("public.defence.event.record-instruction-details"),
                    instructionPayload);

            when(progressionService.getProsecutionCaseDetailById(any(), any()))
                    .thenReturn(Optional.of(cpsProsecutionCaseResponse));
            when(usersGroupService.getDefenceOrganisationDetails(any(), any()))
                    .thenReturn(buildDefenceOrganisationVO());

            cpsEmailNotificationProcessor.processInstructedEmailNotification(envelope);

            verify(notificationService, times(1)).sendCPSNotification(any(), any());
        }

        @Test
        void shouldSendCPSNotificationUsingCcEmailWhenJurisdictionIsCrownAndProsecutorIdAvailable() {
            final UUID caseId = randomUUID();
            final UUID defendantId = randomUUID();
            final UUID organisationId = randomUUID();
            final UUID hearingId = randomUUID();
            final UUID courtCentreId = randomUUID();
            final UUID prosecutorId = randomUUID();

            final JsonObject cpsProsecutionCaseResponse = createObjectBuilder()
                    .add("prosecutionCase", createObjectBuilder()
                            .add("id", caseId.toString())
                            .add("isCivil", true)
                            .add("prosecutor", createObjectBuilder()
                                    .add("isCps", true)
                                    .add("prosecutorId", prosecutorId.toString())
                                    .add("prosecutorCode", "CPS-LN")
                                    .add("prosecutorName", "CPS London North")
                                    .build())
                            .add("prosecutionCaseIdentifier", createObjectBuilder()
                                    .add("prosecutionAuthorityReference", "CPS12345")
                                    .add("prosecutionAuthorityCode", "CPS")
                                    .build())
                            .add("defendants", createArrayBuilder()
                                    .add(createObjectBuilder()
                                            .add("id", defendantId.toString())
                                            .add("personDefendant", createObjectBuilder()
                                                    .add("personDetails", createObjectBuilder()
                                                            .add("firstName", "Fred")
                                                            .add("lastName", "Smith")
                                                            .build())
                                                    .build())
                                            .build())
                                    .build())
                            .build())
                    .add("hearingsAtAGlance", createObjectBuilder()
                            .add("latestHearingJurisdictionType", "CROWN")
                            .add("hearings", createArrayBuilder()
                                    .add(createObjectBuilder()
                                            .add("id", hearingId.toString())
                                            .add("hearingListingStatus", "HEARING_INITIALISED")
                                            .add("courtCentre", createObjectBuilder()
                                                    .add("id", courtCentreId.toString())
                                                    .add("name", "Test Court")
                                                    .build())
                                            .add("hearingDays", createArrayBuilder()
                                                    .add(createObjectBuilder()
                                                            .add("sittingDay", "2099-12-31T09:00:00.000Z")
                                                            .build())
                                                    .build())
                                            .build())
                                    .build())
                            .build())
                    .build();

            final JsonObject instructionPayload = createObjectBuilder()
                    .add("firstInstruction", true)
                    .add("caseId", caseId.toString())
                    .add("defendantId", defendantId.toString())
                    .add("organisationId", organisationId.toString())
                    .build();

            final JsonEnvelope envelope = JsonEnvelope.envelopeFrom(
                    MetadataBuilderFactory.metadataWithRandomUUID("public.defence.event.record-instruction-details"),
                    instructionPayload);

            when(progressionService.getProsecutionCaseDetailById(any(), any()))
                    .thenReturn(Optional.of(cpsProsecutionCaseResponse));
            when(referenceDataService.getProsecutor(any(), eq(prosecutorId), any()))
                    .thenReturn(Optional.of(createObjectBuilder()
                            .add("cpsFlag", true)
                            .add("cpsCcEmailAddress", "prosecutor-cc@cps.gov.uk")
                            .build()));
            when(usersGroupService.getDefenceOrganisationDetails(any(), any()))
                    .thenReturn(buildDefenceOrganisationVO());

            cpsEmailNotificationProcessor.processInstructedEmailNotification(envelope);

            verify(referenceDataService).getProsecutor(any(), eq(prosecutorId), any());
            verify(referenceDataService, never()).getOrganisationUnitById(any(), any(), any());
            verify(notificationService, times(1)).sendCPSNotification(any(), any());
        }

        @Test
        void shouldSendCPSNotificationUsingMcEmailWhenJurisdictionIsMagistratesAndProsecutorIdAvailable() {
            final UUID caseId = randomUUID();
            final UUID defendantId = randomUUID();
            final UUID organisationId = randomUUID();
            final UUID hearingId = randomUUID();
            final UUID courtCentreId = randomUUID();
            final UUID prosecutorId = randomUUID();

            final JsonObject cpsProsecutionCaseResponse = createObjectBuilder()
                    .add("prosecutionCase", createObjectBuilder()
                            .add("id", caseId.toString())
                            .add("isCivil", true)
                            .add("prosecutor", createObjectBuilder()
                                    .add("isCps", true)
                                    .add("prosecutorId", prosecutorId.toString())
                                    .build())
                            .add("prosecutionCaseIdentifier", createObjectBuilder()
                                    .add("prosecutionAuthorityReference", "CPS12345")
                                    .add("prosecutionAuthorityCode", "CPS")
                                    .build())
                            .add("defendants", createArrayBuilder()
                                    .add(createObjectBuilder()
                                            .add("id", defendantId.toString())
                                            .add("personDefendant", createObjectBuilder()
                                                    .add("personDetails", createObjectBuilder()
                                                            .add("firstName", "Jane")
                                                            .add("lastName", "Doe")
                                                            .build())
                                                    .build())
                                            .build())
                                    .build())
                            .build())
                    .add("hearingsAtAGlance", createObjectBuilder()
                            .add("latestHearingJurisdictionType", "MAGISTRATES")
                            .add("hearings", createArrayBuilder()
                                    .add(createObjectBuilder()
                                            .add("id", hearingId.toString())
                                            .add("hearingListingStatus", "HEARING_INITIALISED")
                                            .add("courtCentre", createObjectBuilder()
                                                    .add("id", courtCentreId.toString())
                                                    .add("name", "Magistrates Court")
                                                    .build())
                                            .add("hearingDays", createArrayBuilder()
                                                    .add(createObjectBuilder()
                                                            .add("sittingDay", "2099-12-31T09:00:00.000Z")
                                                            .build())
                                                    .build())
                                            .build())
                                    .build())
                            .build())
                    .build();

            final JsonObject instructionPayload = createObjectBuilder()
                    .add("firstInstruction", true)
                    .add("caseId", caseId.toString())
                    .add("defendantId", defendantId.toString())
                    .add("organisationId", organisationId.toString())
                    .build();

            final JsonEnvelope envelope = JsonEnvelope.envelopeFrom(
                    MetadataBuilderFactory.metadataWithRandomUUID("public.defence.event.record-instruction-details"),
                    instructionPayload);

            when(progressionService.getProsecutionCaseDetailById(any(), any()))
                    .thenReturn(Optional.of(cpsProsecutionCaseResponse));
            when(referenceDataService.getProsecutor(any(), eq(prosecutorId), any()))
                    .thenReturn(Optional.of(createObjectBuilder()
                            .add("cpsFlag", true)
                            .add("cpsMcEmailAddress", "prosecutor-mc@cps.gov.uk")
                            .build()));
            when(usersGroupService.getDefenceOrganisationDetails(any(), any()))
                    .thenReturn(buildDefenceOrganisationVO());

            cpsEmailNotificationProcessor.processInstructedEmailNotification(envelope);

            verify(referenceDataService).getProsecutor(any(), eq(prosecutorId), any());
            verify(referenceDataService, never()).getOrganisationUnitById(any(), any(), any());
            verify(notificationService, times(1)).sendCPSNotification(any(), any());
        }

        @Test
        void shouldSkipCPSNotificationWhenProsecutorIsNotCps() {
            final UUID caseId = randomUUID();
            final UUID defendantId = randomUUID();
            final UUID organisationId = randomUUID();

            final JsonObject tflProsecutionCaseResponse = createObjectBuilder()
                    .add("prosecutionCase", createObjectBuilder()
                            .add("id", caseId.toString())
                            .add("isCivil", true)
                            .add("prosecutor", createObjectBuilder()
                                    .add("isCps", false)
                                    .build())
                            .add("prosecutionCaseIdentifier", createObjectBuilder()
                                    .add("prosecutionAuthorityReference", "TVL12345")
                                    .add("prosecutionAuthorityCode", "TVL")
                                    .build())
                            .build())
                    .build();

            final JsonObject instructionPayload = createObjectBuilder()
                    .add("firstInstruction", true)
                    .add("caseId", caseId.toString())
                    .add("defendantId", defendantId.toString())
                    .add("organisationId", organisationId.toString())
                    .build();

            final JsonEnvelope envelope = JsonEnvelope.envelopeFrom(
                    MetadataBuilderFactory.metadataWithRandomUUID("public.defence.event.record-instruction-details"),
                    instructionPayload);

            when(progressionService.getProsecutionCaseDetailById(any(), any()))
                    .thenReturn(Optional.of(tflProsecutionCaseResponse));

            cpsEmailNotificationProcessor.processInstructedEmailNotification(envelope);

            verify(notificationService, never()).sendCPSNotification(any(), any());
        }

        @Test
        void shouldSendCPSNotificationUsingCcEmailWhenNoProsecutorBlockAndJurisdictionIsCrown() {
            final UUID caseId = randomUUID();
            final UUID defendantId = randomUUID();
            final UUID organisationId = randomUUID();
            final UUID hearingId = randomUUID();
            final UUID courtCentreId = randomUUID();
            final UUID prosecutionAuthorityId = randomUUID();

            final JsonObject prosecutionCaseResponse = createObjectBuilder()
                    .add("prosecutionCase", createObjectBuilder()
                            .add("id", caseId.toString())
                            .add("isCivil", true)
                            .add("prosecutionCaseIdentifier", createObjectBuilder()
                                    .add("prosecutionAuthorityReference", "CPS12345")
                                    .add("prosecutionAuthorityCode", "CPS")
                                    .add("prosecutionAuthorityId", prosecutionAuthorityId.toString())
                                    .build())
                            .add("defendants", createArrayBuilder()
                                    .add(createObjectBuilder()
                                            .add("id", defendantId.toString())
                                            .add("personDefendant", createObjectBuilder()
                                                    .add("personDetails", createObjectBuilder()
                                                            .add("firstName", "John")
                                                            .add("lastName", "Doe")
                                                            .build())
                                                    .build())
                                            .build())
                                    .build())
                            .build())
                    .add("hearingsAtAGlance", createObjectBuilder()
                            .add("latestHearingJurisdictionType", "CROWN")
                            .add("hearings", createArrayBuilder()
                                    .add(createObjectBuilder()
                                            .add("id", hearingId.toString())
                                            .add("hearingListingStatus", "HEARING_INITIALISED")
                                            .add("courtCentre", createObjectBuilder()
                                                    .add("id", courtCentreId.toString())
                                                    .add("name", "Crown Court")
                                                    .build())
                                            .add("hearingDays", createArrayBuilder()
                                                    .add(createObjectBuilder()
                                                            .add("sittingDay", "2099-12-31T09:00:00.000Z")
                                                            .build())
                                                    .build())
                                            .build())
                                    .build())
                            .build())
                    .build();

            final JsonObject instructionPayload = createObjectBuilder()
                    .add("firstInstruction", true)
                    .add("caseId", caseId.toString())
                    .add("defendantId", defendantId.toString())
                    .add("organisationId", organisationId.toString())
                    .build();

            final JsonEnvelope envelope = JsonEnvelope.envelopeFrom(
                    MetadataBuilderFactory.metadataWithRandomUUID("public.defence.event.record-instruction-details"),
                    instructionPayload);

            when(progressionService.getProsecutionCaseDetailById(any(), any()))
                    .thenReturn(Optional.of(prosecutionCaseResponse));
            when(referenceDataService.getProsecutor(any(), eq(prosecutionAuthorityId), any()))
                    .thenReturn(Optional.of(createObjectBuilder()
                            .add("cpsFlag", true)
                            .add("cpsCcEmailAddress", "cps-cc@prosecutor.gov.uk")
                            .build()));
            when(usersGroupService.getDefenceOrganisationDetails(any(), any()))
                    .thenReturn(buildDefenceOrganisationVO());

            cpsEmailNotificationProcessor.processInstructedEmailNotification(envelope);

            verify(referenceDataService).getProsecutor(any(), eq(prosecutionAuthorityId), any());
            verify(notificationService, times(1)).sendCPSNotification(any(), any());
        }

        @Test
        void shouldSendCPSNotificationUsingMcEmailWhenNoProsecutorBlockAndJurisdictionIsMagistrates() {
            final UUID caseId = randomUUID();
            final UUID defendantId = randomUUID();
            final UUID organisationId = randomUUID();
            final UUID hearingId = randomUUID();
            final UUID courtCentreId = randomUUID();
            final UUID prosecutionAuthorityId = randomUUID();

            final JsonObject prosecutionCaseResponse = createObjectBuilder()
                    .add("prosecutionCase", createObjectBuilder()
                            .add("id", caseId.toString())
                            .add("isCivil", true)
                            .add("prosecutionCaseIdentifier", createObjectBuilder()
                                    .add("prosecutionAuthorityReference", "CPS67890")
                                    .add("prosecutionAuthorityCode", "CPS")
                                    .add("prosecutionAuthorityId", prosecutionAuthorityId.toString())
                                    .build())
                            .add("defendants", createArrayBuilder()
                                    .add(createObjectBuilder()
                                            .add("id", defendantId.toString())
                                            .add("personDefendant", createObjectBuilder()
                                                    .add("personDetails", createObjectBuilder()
                                                            .add("firstName", "Alice")
                                                            .add("lastName", "Brown")
                                                            .build())
                                                    .build())
                                            .build())
                                    .build())
                            .build())
                    .add("hearingsAtAGlance", createObjectBuilder()
                            .add("latestHearingJurisdictionType", "MAGISTRATES")
                            .add("hearings", createArrayBuilder()
                                    .add(createObjectBuilder()
                                            .add("id", hearingId.toString())
                                            .add("hearingListingStatus", "HEARING_INITIALISED")
                                            .add("courtCentre", createObjectBuilder()
                                                    .add("id", courtCentreId.toString())
                                                    .add("name", "Magistrates Court")
                                                    .build())
                                            .add("hearingDays", createArrayBuilder()
                                                    .add(createObjectBuilder()
                                                            .add("sittingDay", "2099-12-31T09:00:00.000Z")
                                                            .build())
                                                    .build())
                                            .build())
                                    .build())
                            .build())
                    .build();

            final JsonObject instructionPayload = createObjectBuilder()
                    .add("firstInstruction", true)
                    .add("caseId", caseId.toString())
                    .add("defendantId", defendantId.toString())
                    .add("organisationId", organisationId.toString())
                    .build();

            final JsonEnvelope envelope = JsonEnvelope.envelopeFrom(
                    MetadataBuilderFactory.metadataWithRandomUUID("public.defence.event.record-instruction-details"),
                    instructionPayload);

            when(progressionService.getProsecutionCaseDetailById(any(), any()))
                    .thenReturn(Optional.of(prosecutionCaseResponse));
            when(referenceDataService.getProsecutor(any(), eq(prosecutionAuthorityId), any()))
                    .thenReturn(Optional.of(createObjectBuilder()
                            .add("cpsFlag", true)
                            .add("cpsMcEmailAddress", "cps-mc@prosecutor.gov.uk")
                            .build()));
            when(usersGroupService.getDefenceOrganisationDetails(any(), any()))
                    .thenReturn(buildDefenceOrganisationVO());

            cpsEmailNotificationProcessor.processInstructedEmailNotification(envelope);

            verify(referenceDataService).getProsecutor(any(), eq(prosecutionAuthorityId), any());
            verify(notificationService, times(1)).sendCPSNotification(any(), any());
        }

        @Test
        void shouldSkipCPSNotificationWhenNoProsecutorBlockAndRefDataReturnsCpsFlagFalse() {
            final UUID caseId = randomUUID();
            final UUID defendantId = randomUUID();
            final UUID organisationId = randomUUID();
            final UUID prosecutionAuthorityId = randomUUID();

            final JsonObject prosecutionCaseResponse = createObjectBuilder()
                    .add("prosecutionCase", createObjectBuilder()
                            .add("id", caseId.toString())
                            .add("isCivil", true)
                            .add("prosecutionCaseIdentifier", createObjectBuilder()
                                    .add("prosecutionAuthorityReference", "COL12345")
                                    .add("prosecutionAuthorityCode", "COLCC")
                                    .add("prosecutionAuthorityId", prosecutionAuthorityId.toString())
                                    .build())
                            .build())
                    .build();

            final JsonObject instructionPayload = createObjectBuilder()
                    .add("firstInstruction", true)
                    .add("caseId", caseId.toString())
                    .add("defendantId", defendantId.toString())
                    .add("organisationId", organisationId.toString())
                    .build();

            final JsonEnvelope envelope = JsonEnvelope.envelopeFrom(
                    MetadataBuilderFactory.metadataWithRandomUUID("public.defence.event.record-instruction-details"),
                    instructionPayload);

            when(progressionService.getProsecutionCaseDetailById(any(), any()))
                    .thenReturn(Optional.of(prosecutionCaseResponse));
            when(referenceDataService.getProsecutor(any(), eq(prosecutionAuthorityId), any()))
                    .thenReturn(Optional.of(createObjectBuilder().add("cpsFlag", false).build()));

            cpsEmailNotificationProcessor.processInstructedEmailNotification(envelope);

            verify(referenceDataService).getProsecutor(any(), eq(prosecutionAuthorityId), any());
            verify(notificationService, never()).sendCPSNotification(any(), any());
        }
    }

    @Nested
    class CriminalCase {

        @Test
        void shouldNotSendDisassociationCommandWhenIsLAA() {
            final JsonObject payload = createObjectBuilder()
                    .add("caseId", randomUUID().toString())
                    .add("defendantId", randomUUID().toString())
                    .add("organisationId", randomUUID().toString())
                    .add("isLAA", true)
                    .build();

            final JsonEnvelope envelope = JsonEnvelope.envelopeFrom(
                    MetadataBuilderFactory.metadataWithRandomUUID("public.defence.defence-organisation-disassociated"),
                    payload);

            when(progressionService.getProsecutionCaseDetailById(any(), any())).thenReturn(Optional.empty());

            cpsEmailNotificationProcessor.processDisassociatedEmailNotification(envelope);

            verify(sender, never()).send(any());
        }

        @Test
        void shouldCallDisassociationCommandForCaseAndApplicationWhenApplicationFound() {
            final UUID defendantId = randomUUID();
            final UUID organisationId = randomUUID();
            final UUID caseId = randomUUID();
            final UUID applicationId = randomUUID();

            final JsonObject defencePublicEventPayload = createObjectBuilder()
                    .add("defendantId", defendantId.toString())
                    .add("organisationId", organisationId.toString())
                    .add("caseId", caseId.toString())
                    .build();

            final JsonObject applicationQueryResponsePayload = createObjectBuilder()
                    .add("linkedApplications", createArrayBuilder().add(
                            createObjectBuilder().add("applicationId", applicationId.toString())
                                    .build()
                    ))
                    .build();

            final JsonEnvelope publicEventEnvelope = JsonEnvelope.envelopeFrom(
                    MetadataBuilderFactory.metadataWithRandomUUID("public.defence.defence-organisation-disassociated"),
                    defencePublicEventPayload);

            when(progressionService.getActiveApplicationsOnCase(any(), any())).thenReturn(ofNullable(applicationQueryResponsePayload));

            cpsEmailNotificationProcessor.processDisassociatedEmailNotification(publicEventEnvelope);
            verify(sender, times(2)).send(envelopeCaptor.capture());

            final List<Envelope<JsonObject>> capturedEvents = envelopeCaptor.getAllValues();
            assertThat(capturedEvents.get(0).metadata().name(), is("progression.command.handler.disassociate-defence-organisation"));
            JsonObject capturedEventPayload = capturedEvents.get(0).payload();
            assertThat(capturedEventPayload.getString("defendantId"), is(defendantId.toString()));
            assertThat(capturedEventPayload.getString("organisationId"), is(organisationId.toString()));
            assertThat(capturedEventPayload.getString("caseId"), is(caseId.toString()));

            assertThat(capturedEvents.get(1).metadata().name(), is("progression.command.handler.disassociate-defence-organisation-for-application"));
            capturedEventPayload = capturedEvents.get(1).payload();
            assertThat(capturedEventPayload.getString("defendantId"), is(defendantId.toString()));
            assertThat(capturedEventPayload.getString("organisationId"), is(organisationId.toString()));
            assertThat(capturedEventPayload.getString("applicationId"), is(applicationId.toString()));
        }

        @Test
        void shouldCallDisassociationCommandForCaseOnlyWhenNoApplicationFound() {
            final UUID defendantId = randomUUID();
            final UUID organisationId = randomUUID();
            final UUID caseId = randomUUID();

            final JsonObject defencePublicEventPayload = createObjectBuilder()
                    .add("defendantId", defendantId.toString())
                    .add("organisationId", organisationId.toString())
                    .add("caseId", caseId.toString())
                    .build();

            final JsonEnvelope publicEventEnvelope = JsonEnvelope.envelopeFrom(
                    MetadataBuilderFactory.metadataWithRandomUUID("public.defence.defence-organisation-disassociated"),
                    defencePublicEventPayload);

            when(progressionService.getActiveApplicationsOnCase(any(), any())).thenReturn(ofNullable(createObjectBuilder().build()));

            cpsEmailNotificationProcessor.processDisassociatedEmailNotification(publicEventEnvelope);
            verify(sender, times(1)).send(envelopeCaptor.capture());

            final List<Envelope<JsonObject>> capturedEvents = envelopeCaptor.getAllValues();
            assertThat(capturedEvents.get(0).metadata().name(), is("progression.command.handler.disassociate-defence-organisation"));
            final JsonObject capturedEventPayload = capturedEvents.get(0).payload();
            assertThat(capturedEventPayload.getString("defendantId"), is(defendantId.toString()));
            assertThat(capturedEventPayload.getString("organisationId"), is(organisationId.toString()));
            assertThat(capturedEventPayload.getString("caseId"), is(caseId.toString()));
        }
    }

    private Optional<DefenceOrganisationVO> buildDefenceOrganisationVO() {
        return Optional.of(DefenceOrganisationVO.builder()
                .postcode("POSTCODE")
                .addressLine1("line1")
                .addressLine2("line2")
                .addressLine3("line3")
                .addressLine4("line4")
                .name("organisation name")
                .phoneNumber("12345668")
                .email("abc@xyz.com").build());
    }
}
