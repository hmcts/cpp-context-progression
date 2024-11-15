package uk.gov.moj.cpp.progression.processor;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.isJson;
import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static com.jayway.jsonpath.matchers.JsonPathMatchers.withoutJsonPath;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static java.util.Optional.of;
import static java.util.UUID.randomUUID;
import static javax.json.Json.createObjectBuilder;
import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.justice.core.courts.Hearing.hearing;
import static uk.gov.justice.core.courts.ProsecutionCase.prosecutionCase;
import static uk.gov.justice.services.messaging.Envelope.metadataBuilder;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.justice.services.test.utils.core.messaging.MetadataBuilderFactory.metadataWithRandomUUID;
import static uk.gov.justice.services.test.utils.core.reflection.ReflectionUtil.setField;
import static uk.gov.moj.cpp.progression.service.ReferenceDataOffenceService.CJS_OFFENCE_CODE;
import static uk.gov.moj.cpp.progression.service.ReferenceDataOffenceService.LEGISLATION;
import static uk.gov.moj.cpp.progression.service.ReferenceDataOffenceService.LEGISLATION_WELSH;
import static uk.gov.moj.cpp.progression.service.ReferenceDataOffenceService.MODEOFTRIAL_CODE;
import static uk.gov.moj.cpp.progression.service.ReferenceDataOffenceService.OFFENCE_TITLE;
import static uk.gov.moj.cpp.progression.service.ReferenceDataOffenceService.WELSH_OFFENCE_TITLE;


import uk.gov.justice.core.courts.AllHearingOffencesUpdatedV2;
import uk.gov.justice.core.courts.ConfirmedHearing;
import uk.gov.justice.core.courts.ConfirmedProsecutionCase;
import uk.gov.justice.core.courts.Defendant;
import uk.gov.justice.core.courts.Hearing;
import uk.gov.justice.core.courts.HearingListingStatus;
import uk.gov.justice.core.courts.HearingOffencesUpdated;
import uk.gov.justice.core.courts.HearingUpdated;
import uk.gov.justice.core.courts.HearingUpdatedProcessed;
import uk.gov.justice.core.courts.JudicialResult;
import uk.gov.justice.core.courts.Offence;
import uk.gov.justice.core.courts.notification.EmailChannel;
import uk.gov.justice.cpp.progression.events.NewDefendantAddedToHearing;
import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.common.converter.ObjectToJsonObjectConverter;
import uk.gov.justice.services.common.converter.StringToJsonObjectConverter;
import uk.gov.justice.services.common.converter.jackson.ObjectMapperProducer;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.messaging.spi.DefaultEnvelope;
import uk.gov.justice.services.test.utils.core.messaging.MetadataBuilderFactory;
import uk.gov.moj.cpp.progression.helper.HearingNotificationHelper;
import uk.gov.moj.cpp.progression.service.ApplicationParameters;
import uk.gov.moj.cpp.progression.service.DefenceService;
import uk.gov.moj.cpp.progression.service.DocumentGeneratorService;
import uk.gov.moj.cpp.progression.service.NotificationService;
import uk.gov.moj.cpp.progression.service.ProgressionService;
import uk.gov.moj.cpp.progression.service.RefDataService;
import uk.gov.moj.cpp.progression.service.ReferenceDataOffenceService;
import uk.gov.moj.cpp.progression.service.dto.HearingNotificationInputData;
import uk.gov.moj.cpp.progression.utils.FileUtil;

import java.nio.charset.Charset;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import javax.json.Json;
import javax.json.JsonObject;

import com.google.common.collect.ImmutableList;
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

@ExtendWith(MockitoExtension.class)
public class HearingUpdatedEventProcessorTest {

    @Captor
    private ArgumentCaptor<ConfirmedHearing> confirmedHearingArgumentCaptor;

    @Captor
    private ArgumentCaptor<JsonEnvelope> envelopeArgumentCaptor;

    @Captor
    private ArgumentCaptor<Hearing> hearingArgumentCaptor;

    @Captor
    private ArgumentCaptor<List<UUID>> applicationIdsArgumentCaptor;

    @Captor
    private ArgumentCaptor<HearingListingStatus> hearingListingStatusArgumentCaptor;

    @Captor
    private ArgumentCaptor<DefaultEnvelope> senderJsonEnvelopeCaptor;

    @Captor
    private ArgumentCaptor<List<EmailChannel>> prosecutorEmailCapture;

    @InjectMocks
    private HearingUpdatedEventProcessor eventProcessor;

    @Mock
    private ProgressionService progressionService;

    @Mock
    private NotificationService notificationService;

    @Mock
    private Sender sender;
    @Mock
    private ApplicationParameters applicationParameters;

    @Mock
    private HearingNotificationHelper hearingNotificationHelper;

    @Mock
    private RefDataService refDataService;

    @Mock
    private DefenceService defenceService;

    @Mock
    private ReferenceDataOffenceService referenceDataOffenceService;

    @Mock
    private DocumentGeneratorService documentGeneratorService;

    @Spy
    private JsonObjectToObjectConverter jsonObjectToObjectConverter;

    @Spy
    private ObjectToJsonObjectConverter objectToJsonObjectConverter;

    @Captor
    private ArgumentCaptor<HearingNotificationInputData> hearingInputDataEnvelopeCaptor;

    private static final UUID materialId = randomUUID();

    @BeforeEach
    public void initMocks() {
        setField(this.jsonObjectToObjectConverter, "objectMapper", new ObjectMapperProducer().objectMapper());
        setField(this.objectToJsonObjectConverter, "mapper", new ObjectMapperProducer().objectMapper());
    }

    @Test
    public void shouldHandleHearingConfirmedEventMessage() {
        final UUID hearingId = randomUUID();
        final Hearing hearing = hearing()
                .withId(hearingId)
                .build();
        final HearingUpdatedProcessed hearingUpdatedProcessed = HearingUpdatedProcessed.hearingUpdatedProcessed()
                .withConfirmedHearing(ConfirmedHearing.confirmedHearing()
                        .withId(hearingId)
                        .withProsecutionCases(ImmutableList.of(ConfirmedProsecutionCase.confirmedProsecutionCase()
                                .withId(randomUUID())
                                .build()))
                        .build())
                .withHearing(hearing)
                .build();


        JsonEnvelope jsonEnvelope = getJsonEnvelopeForHearingUpdatedProcessed(hearingUpdatedProcessed);
        when(progressionService.updateHearingForHearingUpdated(any(), any(), any())).thenReturn(hearing);

        eventProcessor.publishHearingDetailChangedPublicEvent(jsonEnvelope);

        verify(progressionService, times(1)).updateHearingListingStatusToHearingUpdate(envelopeArgumentCaptor.capture(), hearingArgumentCaptor.capture());
        assertEquals(hearing, hearingArgumentCaptor.getValue());

        verify(progressionService, times(1)).publishHearingDetailChangedPublicEvent(envelopeArgumentCaptor.capture(), confirmedHearingArgumentCaptor.capture());
        assertEquals(hearingUpdatedProcessed.getConfirmedHearing().getId(), confirmedHearingArgumentCaptor.getValue().getId());
    }

    @Test
    public void shouldHandleHearingOffenceUpdated() {
        final UUID hearingId = randomUUID();
        final HearingOffencesUpdated hearingOffencesUpdated = HearingOffencesUpdated.hearingOffencesUpdated().withHearingId(hearingId).build();
        final JsonEnvelope jsonEnvelope = envelopeFrom(metadataWithRandomUUID("progression.event.hearing-offences-updated-v2"),
                objectToJsonObjectConverter.convert(hearingOffencesUpdated));
        eventProcessor.handleHearingOffenceUpdated(jsonEnvelope);
        ArgumentCaptor<UUID> uuidArgumentCaptor = ArgumentCaptor.forClass(UUID.class);
        verify(progressionService, times(1)).populateHearingToProbationCaseworker(envelopeArgumentCaptor.capture(), uuidArgumentCaptor.capture());
        assertThat(uuidArgumentCaptor.getValue(), is(hearingId));
    }

    @Test
    public void shouldHandleAllHearingUpdatedWhenOffenceOfCaseUpdated(){

        final AllHearingOffencesUpdatedV2 allHearingOffencesUpdatedV2 = AllHearingOffencesUpdatedV2.allHearingOffencesUpdatedV2()
                .withUpdatedOffences(singletonList(Offence.offence().withId(randomUUID()).build()))
                .withHearingIds(singletonList(randomUUID()))
                .withDefendantId(randomUUID())
                .build();
        final JsonEnvelope jsonEnvelope = envelopeFrom(metadataWithRandomUUID("progression.event.all-hearing-offences-updated-v2"),
                objectToJsonObjectConverter.convert(allHearingOffencesUpdatedV2));

        eventProcessor.handleAllHearingOffenceUpdated(jsonEnvelope);

        verify(sender).send(senderJsonEnvelopeCaptor.capture());

        final DefaultEnvelope captorValue = senderJsonEnvelopeCaptor.getValue();
        assertThat(captorValue.metadata().name(), is("progression.command.update-offences-for-hearing"));
        assertThat(captorValue.payload().toString(), isJson(allOf(
                withJsonPath("$.updatedOffences.length()", equalTo(1)),
                withoutJsonPath("$.newOffences")
        )));
    }

    @Test
    public void shouldHandleAllHearingUpdatedWhenOffenceOfCaseAdded(){

        final AllHearingOffencesUpdatedV2 allHearingOffencesUpdatedV2 = AllHearingOffencesUpdatedV2.allHearingOffencesUpdatedV2()
                .withNewOffences(singletonList(Offence.offence().withId(randomUUID()).build()))
                .withHearingIds(singletonList(randomUUID()))
                .withDefendantId(randomUUID())
                .build();
        final JsonEnvelope jsonEnvelope = envelopeFrom(metadataWithRandomUUID("progression.event.all-hearing-offences-updated-v2"),
                objectToJsonObjectConverter.convert(allHearingOffencesUpdatedV2));

        eventProcessor.handleAllHearingOffenceUpdated(jsonEnvelope);

        verify(sender).send(senderJsonEnvelopeCaptor.capture());

        final DefaultEnvelope captorValue = senderJsonEnvelopeCaptor.getValue();
        assertThat(captorValue.metadata().name(), is("progression.command.update-offences-for-hearing"));
        assertThat(captorValue.payload().toString(), isJson(allOf(
                withJsonPath("$.newOffences.length()", equalTo(1)),
                withoutJsonPath("$.updatedOffences")
        )));
    }

    @Test
    public void shouldHandleAllHearingUpdated(){

        final AllHearingOffencesUpdatedV2 allHearingOffencesUpdatedV2 = AllHearingOffencesUpdatedV2.allHearingOffencesUpdatedV2()
                .withNewOffences(singletonList(Offence.offence().withId(randomUUID()).build()))
                .withUpdatedOffences(singletonList(Offence.offence().withId(randomUUID()).build()))
                .withHearingIds(singletonList(randomUUID()))
                .withDefendantId(randomUUID())
                .build();
        final JsonEnvelope jsonEnvelope = envelopeFrom(metadataWithRandomUUID("progression.event.all-hearing-offences-updated-v2"),
                objectToJsonObjectConverter.convert(allHearingOffencesUpdatedV2));

        eventProcessor.handleAllHearingOffenceUpdated(jsonEnvelope);

        verify(sender).send(senderJsonEnvelopeCaptor.capture());

        final DefaultEnvelope captorValue = senderJsonEnvelopeCaptor.getValue();
        assertThat(captorValue.metadata().name(), is("progression.command.update-offences-for-hearing"));
        assertThat(captorValue.payload().toString(), isJson(allOf(
                withJsonPath("$.newOffences.length()", equalTo(1)),
                withJsonPath("$.updatedOffences.length()", equalTo(1))
        )));
    }

    @Test
    public void shouldHandleAddedNewDefendantToHearing() {
        final UUID hearingId = randomUUID();
        final NewDefendantAddedToHearing newDefendantAddedToHearing = NewDefendantAddedToHearing.newDefendantAddedToHearing().withHearingId(hearingId).build();
        final JsonEnvelope jsonEnvelope = envelopeFrom(metadataWithRandomUUID("progression.event.new-defendant-added-to-hearing"),
                objectToJsonObjectConverter.convert(newDefendantAddedToHearing));
        eventProcessor.addedNewDefendantToHearing(jsonEnvelope);
        ArgumentCaptor<UUID> uuidArgumentCaptor = ArgumentCaptor.forClass(UUID.class);
        verify(progressionService, times(1)).populateHearingToProbationCaseworker(envelopeArgumentCaptor.capture(), uuidArgumentCaptor.capture());
        assertThat(uuidArgumentCaptor.getValue(), is(hearingId));
    }


    @Test
    public void shouldProcessHearingUpdatedWhenProsecutionCaseListed() {

        final UUID hearingId = randomUUID();
        final JsonObject payload = Json.createObjectBuilder()
                .add("updatedHearing", objectToJsonObjectConverter.convert(ConfirmedHearing.confirmedHearing()
                        .withId(hearingId).build()))
                .build();

        final JsonEnvelope event = envelopeFrom(metadataWithRandomUUID("public.listing.hearing-updated"),
                payload);
        final Hearing hearing = hearing()
                .withId(hearingId)
                .withProsecutionCases(Collections.singletonList(prosecutionCase()
                        .withDefendants(Collections.singletonList(Defendant.defendant()
                                .withOffences(Collections.singletonList(Offence.offence()
                                        .withJudicialResults(Collections.singletonList(JudicialResult.judicialResult().build()))
                                        .build()))
                                .build()))
                        .build()))
                .build();
        final Optional<JsonObject> hearingJson = of(createObjectBuilder()
                .add("hearing", objectToJsonObjectConverter.convert(hearing))
                .add("hearingListingStatus", "SENT_FOR_LISTING")
                .build());
        when(progressionService.getHearing(event, hearingId.toString())).thenReturn(hearingJson);
        when(progressionService.updateHearingForHearingUpdated(any(), any(), any())).thenReturn(hearing);

        this.eventProcessor.processHearingUpdated(event);

        verify(sender).send(senderJsonEnvelopeCaptor.capture());

        final DefaultEnvelope captorValue = senderJsonEnvelopeCaptor.getValue();
        assertThat(captorValue.metadata().name(), is("progression.command.process-hearing-updated"));
        assertThat(captorValue.payload().toString(), isJson(allOf(
                withJsonPath("$.confirmedHearing.id", equalTo(hearingId.toString())),
                withJsonPath("$.updatedHearing.id", equalTo(hearingId.toString())),
                withoutJsonPath("$.updatedHearing.prosecutionCases[0].defendants[0].offences[0].judicialResults")
        )));
    }

    @Test
    public void shouldProcessHearingUpdatedWhenProsecutionCaseListedAndProgressionCasesRemoved() {

        final UUID hearingId = randomUUID();
        final JsonObject payload = Json.createObjectBuilder()
                .add("updatedHearing", objectToJsonObjectConverter.convert(ConfirmedHearing.confirmedHearing()
                        .withId(hearingId).build()))
                .build();

        final JsonEnvelope event = envelopeFrom(metadataWithRandomUUID("public.listing.hearing-updated"),
                payload);
        final Hearing hearing = hearing()
                .withId(hearingId)
                .withProsecutionCases(emptyList())
                .build();
        final Optional<JsonObject> hearingJson = of(createObjectBuilder()
                .add("hearing", objectToJsonObjectConverter.convert(hearing))
                .add("hearingListingStatus", "SENT_FOR_LISTING")
                .build());
        when(progressionService.getHearing(event, hearingId.toString())).thenReturn(hearingJson);
        when(progressionService.updateHearingForHearingUpdated(any(), any(), any())).thenReturn(hearing);

        this.eventProcessor.processHearingUpdated(event);

        verify(sender).send(senderJsonEnvelopeCaptor.capture());

        final DefaultEnvelope captorValue = senderJsonEnvelopeCaptor.getValue();
        assertThat(captorValue.metadata().name(), is("progression.command.process-hearing-updated"));
        assertThat(captorValue.payload().toString(), isJson(allOf(
                withJsonPath("$.confirmedHearing.id", equalTo(hearingId.toString())),
                withJsonPath("$.updatedHearing.id", equalTo(hearingId.toString())),
                withoutJsonPath("$.updatedHearing.prosecutionCases")
        )));
    }

    @Test
    public void shouldProcessHearingUpdatedWhenProsecutionCaseInitialised() {

        final UUID hearingId = randomUUID();
        final JsonObject payload = Json.createObjectBuilder()
                .add("updatedHearing", objectToJsonObjectConverter.convert(ConfirmedHearing.confirmedHearing()
                        .withId(hearingId).build()))
                .build();

        final JsonEnvelope event = envelopeFrom(metadataWithRandomUUID("public.listing.hearing-updated"),
                payload);
        final Hearing hearing = hearing()
                .withId(hearingId)
                .withProsecutionCases(Collections.singletonList(prosecutionCase()
                        .withDefendants(Collections.singletonList(Defendant.defendant()
                                .withOffences(Collections.singletonList(Offence.offence()
                                        .withJudicialResults(Collections.singletonList(JudicialResult.judicialResult().build()))
                                        .build()))
                                .build()))
                        .build()))
                .build();
        final Optional<JsonObject> hearingJson = of(createObjectBuilder()
                .add("hearing", objectToJsonObjectConverter.convert(hearing))
                .add("hearingListingStatus", "HEARING_INITIALISED")
                .build());
        when(progressionService.getHearing(event, hearingId.toString())).thenReturn(hearingJson);
        when(progressionService.updateHearingForHearingUpdated(any(), any(), any())).thenReturn(hearing);

        this.eventProcessor.processHearingUpdated(event);

        verify(sender).send(senderJsonEnvelopeCaptor.capture());

        final DefaultEnvelope captorValue = senderJsonEnvelopeCaptor.getValue();
        assertThat(captorValue.metadata().name(), is("progression.command.process-hearing-updated"));
        assertThat(captorValue.payload().toString(), isJson(allOf(
                withJsonPath("$.confirmedHearing.id", equalTo(hearingId.toString())),
                withJsonPath("$.updatedHearing.id", equalTo(hearingId.toString()))
        )));
    }

    @Test
    public void shouldHandleHearingConfirmedEventMessageWithCourtApplication() {
        final UUID hearingId = UUID.randomUUID();
        final HearingUpdated hearingUpdated = HearingUpdated.hearingUpdated()
                .withUpdatedHearing(ConfirmedHearing.confirmedHearing()
                        .withId(hearingId)
                        .withProsecutionCases(ImmutableList.of(ConfirmedProsecutionCase.confirmedProsecutionCase()
                                .withId(randomUUID())
                                .build()))
                        .withCourtApplicationIds(ImmutableList.of(randomUUID()))
                        .build())
                .build();

        final Hearing hearing = hearing()
                .withId(hearingId)
                .build();

        final Optional<JsonObject> hearingJson = of(createObjectBuilder()
                .add("hearing", objectToJsonObjectConverter.convert(hearing))
                .add("hearingListingStatus", "SENT_FOR_LISTING")
                .build());

        JsonEnvelope jsonEnvelope = getJsonEnvelope(hearingUpdated);
        when(progressionService.getHearing(jsonEnvelope, hearingId.toString())).thenReturn(hearingJson);
        when(progressionService.updateHearingForHearingUpdated(any(), any(), any())).thenReturn(hearing);

        eventProcessor.processHearingUpdated(jsonEnvelope);

        verify(progressionService, times(1)).updateHearingListingStatusToHearingUpdate(envelopeArgumentCaptor.capture(), hearingArgumentCaptor.capture());
        assertEquals(hearing, hearingArgumentCaptor.getValue());

        verify(progressionService, times(1)).linkApplicationsToHearing(envelopeArgumentCaptor.capture(), hearingArgumentCaptor.capture(), applicationIdsArgumentCaptor.capture(), hearingListingStatusArgumentCaptor.capture());

        verify(progressionService, times(1)).publishHearingDetailChangedPublicEvent(envelopeArgumentCaptor.capture(), confirmedHearingArgumentCaptor.capture());
        assertEquals(hearingUpdated.getUpdatedHearing().getId(), confirmedHearingArgumentCaptor.getValue().getId());

        verify(progressionService, never()).populateHearingToProbationCaseworker(eq(jsonEnvelope), eq(hearingId));
    }

    @Test
    public void shouldHearingOffenceVerdictUpdated() {
        final JsonObject payload = createObjectBuilder().build();
        final JsonEnvelope jsonEnvelope = envelopeFrom(metadataWithRandomUUID("public.hearing.hearing-offence-verdict-updated"),
                payload);
        eventProcessor.hearingOffenceVerdictUpdated(jsonEnvelope);
        verify(sender).send(senderJsonEnvelopeCaptor.capture());

        final DefaultEnvelope captorValue = senderJsonEnvelopeCaptor.getValue();
        assertThat(captorValue.metadata().name(), is("progression.command.update-hearing-offence-verdict"));

    }

    @Test
    public void shouldUpdateDefendantOnApplicationHearing() {
        final UUID applicationId1 = randomUUID();
        final UUID applicationId2 = randomUUID();
        final JsonObject payload = FileUtil.jsonFromString(FileUtil.getPayload("progression.event.application-hearing-defendant-updated.json")
                .replaceAll("%APPLICATION_ID1%", applicationId1.toString())
                .replaceAll("%APPLICATION_ID2%", applicationId2.toString()));
        eventProcessor.processUpdateDefendantOnApplicationHearing(envelopeFrom(metadataWithRandomUUID("progression.event.application-hearing-defendant-updated"),
                payload));
        verify(this.sender, times(1)).send(this.senderJsonEnvelopeCaptor.capture());
        final List<DefaultEnvelope> defaultEnvelopes = this.senderJsonEnvelopeCaptor.getAllValues();
        final DefaultEnvelope firstCommandEvent = defaultEnvelopes.get(0);

        assertThat(firstCommandEvent.metadata().name(), is("progression.command.update-application-defendant"));
        assertThat(firstCommandEvent.payload().toString(), isJson(allOf(
                withJsonPath("$.courtApplication", is(notNullValue())))));
    }

    @Test
    public void shouldSendHearingNotificationsToDefenceAndProsecutor() {
        final UUID hearingId = UUID.randomUUID();
        final UUID caseId = randomUUID();
        final UUID defendantId = randomUUID();

        final Hearing hearing = hearing()
                .withId(hearingId)
                .build();

        final Optional<JsonObject> hearingJson = of(createObjectBuilder()
                .add("hearing", objectToJsonObjectConverter.convert(hearing))
                .add("hearingListingStatus", "SENT_FOR_LISTING")
                .build());

        final JsonObject payload = FileUtil.jsonFromString(FileUtil.getPayload("public.listing.hearing-updated.json")
                .replaceAll("%CASE_ID%", caseId.toString())
                .replaceAll("%HEARING_ID%", hearingId.toString())
                .replaceAll("%HEARING_TYPE%", "Plea")
                .replaceAll("%DEFENDANT_ID%", defendantId.toString()));

        JsonEnvelope jsonEnvelope = envelopeFrom(metadataBuilder()
                        .withName("public.listing.hearing-updated")
                        .withId(randomUUID())
                        .build(),
                payload);

        when(progressionService.getHearing(any(), any())).thenReturn(hearingJson);
        when(progressionService.updateHearingForHearingUpdated(any(), any(), any())).thenReturn(hearing);
        when(applicationParameters.getNotifyHearingTemplateId()).thenReturn(("e4648583-eb0f-438e-aab5-5eff29f3f7b4"));


        final JsonEnvelope requestMessage = envelopeFrom(
                MetadataBuilderFactory.metadataWithRandomUUID("progression.event.hearing-updated-processed"),
                objectToJsonObjectConverter.convert(payload));

        eventProcessor.processHearingUpdated(jsonEnvelope);

        verify(progressionService, never()).populateHearingToProbationCaseworker(eq(jsonEnvelope), eq(hearingId));
        verify(hearingNotificationHelper, times(1)).sendHearingNotificationsToRelevantParties(any(), hearingInputDataEnvelopeCaptor.capture());
        HearingNotificationInputData inputData = hearingInputDataEnvelopeCaptor.getValue();
        assertThat(inputData.getHearingId(), is(hearingId));
        assertThat(inputData.getHearingType(), is("Plea"));

    }

    @Test
    public void shouldNotSendHearingNotificationsToDefenceAndProsecutor_NotificationFlagFalse() {
        final UUID hearingId = UUID.randomUUID();
        final UUID caseId = randomUUID();
        final UUID defendantId = randomUUID();

        final Hearing hearing = hearing()
                .withId(hearingId)
                .build();

        final Optional<JsonObject> hearingJson = of(createObjectBuilder()
                .add("hearing", objectToJsonObjectConverter.convert(hearing))
                .add("hearingListingStatus", "SENT_FOR_LISTING")
                .build());

        final JsonObject payload = FileUtil.jsonFromString(FileUtil.getPayload("public.listing.hearing-updated-notification-false.json")
                .replaceAll("%CASE_ID%", caseId.toString())
                .replaceAll("%DEFENDANT_ID%", defendantId.toString()));

        JsonEnvelope jsonEnvelope = envelopeFrom(metadataBuilder()
                        .withName("public.listing.hearing-updated")
                        .withId(randomUUID())
                        .build(),
                payload);

        when(progressionService.getHearing(any(), any())).thenReturn(hearingJson);
        when(progressionService.updateHearingForHearingUpdated(any(), any(), any())).thenReturn(hearing);


        final JsonEnvelope requestMessage = envelopeFrom(
                MetadataBuilderFactory.metadataWithRandomUUID("progression.event.hearing-updated-processed"),
                objectToJsonObjectConverter.convert(payload));

        eventProcessor.processHearingUpdated(jsonEnvelope);

        verify(progressionService, never()).populateHearingToProbationCaseworker(eq(jsonEnvelope), eq(hearingId));
        verify(hearingNotificationHelper, times(0)).sendHearingNotificationsToRelevantParties(any(), any());
    }


    private JsonEnvelope getJsonEnvelopeForHearingUpdatedProcessed(final HearingUpdatedProcessed hearingUpdatedProcessed) {
        return JsonEnvelope.envelopeFrom(
                metadataWithRandomUUID("progression.event.hearing-updated-processed"),
                objectToJsonObjectConverter.convert(hearingUpdatedProcessed));
    }

    private JsonEnvelope getJsonEnvelope(final HearingUpdated hearingUpdated) {
        return JsonEnvelope.envelopeFrom(
                MetadataBuilderFactory.metadataWithRandomUUID("public.listing.hearing-updated"),
                objectToJsonObjectConverter.convert(hearingUpdated));
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

    private static JsonObject getOffence(final String modeoftrial) {
        return Json.createObjectBuilder().add(LEGISLATION, "E12")
                .add(LEGISLATION_WELSH, "123")
                .add(OFFENCE_TITLE, "title-of-offence")
                .add(WELSH_OFFENCE_TITLE, "welsh-title")
                .add(MODEOFTRIAL_CODE, modeoftrial)
                .add(CJS_OFFENCE_CODE, "British").build();
    }

}
