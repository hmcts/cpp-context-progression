package uk.gov.moj.cpp.progression.processor;

import static java.util.Collections.singletonList;
import static java.util.UUID.randomUUID;
import static javax.json.Json.createObjectBuilder;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyList;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.justice.core.courts.ApplicationStatus.LISTED;
import static uk.gov.justice.core.courts.HearingListingStatus.HEARING_INITIALISED;
import static uk.gov.justice.services.test.utils.core.enveloper.EnveloperFactory.createEnveloper;

import uk.gov.justice.core.courts.ConfirmedDefendant;
import uk.gov.justice.core.courts.ConfirmedHearing;
import uk.gov.justice.core.courts.ConfirmedOffence;
import uk.gov.justice.core.courts.ConfirmedProsecutionCase;
import uk.gov.justice.core.courts.Defendant;
import uk.gov.justice.core.courts.ExtendHearingDefendantRequestCreated;
import uk.gov.justice.core.courts.ExtendHearingDefendantRequestUpdated;
import uk.gov.justice.core.courts.Hearing;
import uk.gov.justice.core.courts.HearingConfirmed;
import uk.gov.justice.core.courts.HearingDay;
import uk.gov.justice.core.courts.HearingListingNeeds;
import uk.gov.justice.core.courts.ListCourtHearing;
import uk.gov.justice.core.courts.Offence;
import uk.gov.justice.core.courts.ProsecutionCase;
import uk.gov.justice.core.courts.UpdateHearingForPartialAllocation;
import uk.gov.justice.hearing.courts.Initiate;
import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.common.converter.ObjectToJsonObjectConverter;
import uk.gov.justice.services.common.converter.jackson.ObjectMapperProducer;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.progression.service.ListingService;
import uk.gov.moj.cpp.progression.service.PartialHearingConfirmService;
import uk.gov.moj.cpp.progression.service.ProgressionService;
import uk.gov.moj.cpp.progression.service.SummonsService;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;

import javax.json.Json;
import javax.json.JsonObject;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;
import org.mockito.runners.MockitoJUnitRunner;


@RunWith(MockitoJUnitRunner.class)
public class HearingConfirmedEventProcessorTest {

    @Spy
    private final Enveloper enveloper = createEnveloper();
    @Spy
    private final ObjectMapper objectMapper = new ObjectMapperProducer().objectMapper();
    @Spy
    @InjectMocks
    private final ObjectToJsonObjectConverter objectToJsonObjectConverter = new ObjectToJsonObjectConverter();
    @InjectMocks
    private HearingConfirmedEventProcessor eventProcessor;
    @Mock
    private Sender sender;
    @Mock
    private HearingConfirmed hearingConfirmed;
    @Mock
    private JsonEnvelope envelope;
    @Mock
    private JsonObject payload;
    @Mock
    private JsonEnvelope finalEnvelope;
    @Mock
    private ProgressionService progressionService;
    @Mock
    private PartialHearingConfirmService partialHearingConfirmService;
    @Mock
    private ListingService listingService;
    @Mock
    private SummonsService summonsService;
    @Mock
    private Function<Object, JsonEnvelope> enveloperFunction;
    @Mock
    private JsonObjectToObjectConverter jsonObjectToObjectConverter;
    @Mock
    private HearingListingNeeds hearingListingNeeds;


    @Before
    public void initMocks() {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void shouldHandleHearingConfirmedWithCasesEventMessage() throws Exception {
        final UUID offenceId = randomUUID();
        final UUID defendantId = randomUUID();
        final UUID caseId = randomUUID();

        ConfirmedProsecutionCase confirmedProsecutionCase = createConfirmedProsecutionCase(caseId, defendantId, offenceId);
        ConfirmedHearing confirmedHearing = ConfirmedHearing.confirmedHearing()
                .withId(randomUUID())
                .withProsecutionCases(singletonList(confirmedProsecutionCase))
                .build();
        JsonObject prosecutionCaseJson = createProsecutionCaseJson(offenceId, defendantId, caseId);
        ProsecutionCase prosecutionCase = createProsecutionCase(offenceId, defendantId, caseId);

        when(hearingConfirmed.getConfirmedHearing()).thenReturn(confirmedHearing);
        when(envelope.payloadAsJsonObject()).thenReturn(payload);
        when(jsonObjectToObjectConverter.convert(envelope.payloadAsJsonObject(), HearingConfirmed.class)).thenReturn(hearingConfirmed);
        when(progressionService.getProsecutionCaseDetailById(any(), any())).thenReturn(Optional.of(prosecutionCaseJson));
        doNothing().when(progressionService).prepareSummonsData(anyObject(), anyObject());
        when(jsonObjectToObjectConverter.convert(prosecutionCaseJson, ProsecutionCase.class)).thenReturn(prosecutionCase);
        when(enveloperFunction.apply(any(JsonObject.class))).thenReturn(finalEnvelope);
        when(progressionService.transformConfirmedHearing(any(), any())).thenReturn(
                Hearing.hearing()
                        .withId(randomUUID())
                        .withHearingDays(singletonList(HearingDay.hearingDay().withSittingDay(ZonedDateTime.now(ZoneId.of("UTC"))).build()))
                        .withProsecutionCases(singletonList(ProsecutionCase.prosecutionCase()
                                .withDefendants(singletonList(Defendant.defendant()
                                        .withId(randomUUID())
                                        .withOffences(singletonList(Offence.offence()
                                                .withId(randomUUID())
                                                .build()))
                                        .build()))
                                .build()))
                        .build());
        when(enveloper.withMetadataFrom(envelope, "hearing.initiate")).thenReturn(enveloperFunction);
        when(enveloper.withMetadataFrom(envelope, "progression.command-enrich-hearing-initiate")).thenReturn(enveloperFunction);
        when(enveloper.withMetadataFrom(envelope, "progression.command.update-defendant-listing-status")).thenReturn(enveloperFunction);
        when(enveloper.withMetadataFrom(envelope, "public.progression.prosecution-cases-referred-to-court")).thenReturn(enveloperFunction);
        when(enveloper.withMetadataFrom(envelope, "progression.command-link-prosecution-cases-to-hearing")).thenReturn(enveloperFunction);
        when(progressionService.getHearing(anyObject(), anyString())).thenReturn(Optional.empty());

        eventProcessor.processEvent(envelope);

        verify(sender, times(2)).send(finalEnvelope);
        verify(progressionService, times(1)).prepareSummonsData(any(), any());
    }

    @Test
    public void shouldHandleHearingConfirmedWithCasesEventMessageWhenPartialHearingConfirm() throws Exception {
        final UUID offenceId = randomUUID();
        final UUID defendantId = randomUUID();
        final UUID caseId = randomUUID();

        ConfirmedProsecutionCase confirmedProsecutionCase = createConfirmedProsecutionCase(caseId, defendantId, offenceId);
        final UUID hearingId = randomUUID();
        ConfirmedHearing confirmedHearing = ConfirmedHearing.confirmedHearing()
                .withId(hearingId)
                .withProsecutionCases(singletonList(confirmedProsecutionCase))
                .build();
        JsonObject prosecutionCaseJson = createProsecutionCaseJson(offenceId, defendantId, caseId);
        ProsecutionCase prosecutionCase = createProsecutionCase(offenceId, defendantId, caseId);
        final List<ProsecutionCase> deltaProsecutionCases = Arrays.asList(createProsecutionCase(offenceId, defendantId, caseId));

        when(hearingConfirmed.getConfirmedHearing()).thenReturn(confirmedHearing);
        when(envelope.payloadAsJsonObject()).thenReturn(payload);
        when(jsonObjectToObjectConverter.convert(envelope.payloadAsJsonObject(), HearingConfirmed.class)).thenReturn(hearingConfirmed);
        when(progressionService.getProsecutionCaseDetailById(any(), any())).thenReturn(Optional.of(prosecutionCaseJson));
        doNothing().when(progressionService).prepareSummonsData(anyObject(), anyObject());
        when(jsonObjectToObjectConverter.convert(prosecutionCaseJson, ProsecutionCase.class)).thenReturn(prosecutionCase);
        when(partialHearingConfirmService.getDifferences(envelope,confirmedHearing)).thenReturn(deltaProsecutionCases);
        when(enveloperFunction.apply(any(JsonObject.class))).thenReturn(finalEnvelope);
        when(progressionService.transformConfirmedHearing(any(), any())).thenReturn(
                Hearing.hearing()
                        .withId(randomUUID())
                        .withHearingDays(singletonList(HearingDay.hearingDay().withSittingDay(ZonedDateTime.now(ZoneId.of("UTC"))).build()))
                        .withProsecutionCases(singletonList(ProsecutionCase.prosecutionCase()
                                .withDefendants(singletonList(Defendant.defendant()
                                        .withId(randomUUID())
                                        .withOffences(singletonList(Offence.offence()
                                                .withId(randomUUID())
                                                .build()))
                                        .build()))
                                .build()))
                        .build());
        when(enveloper.withMetadataFrom(envelope, "hearing.initiate")).thenReturn(enveloperFunction);
        when(enveloper.withMetadataFrom(envelope, "progression.command-enrich-hearing-initiate")).thenReturn(enveloperFunction);
        when(enveloper.withMetadataFrom(envelope, "progression.command.update-defendant-listing-status")).thenReturn(enveloperFunction);
        when(enveloper.withMetadataFrom(envelope, "public.progression.prosecution-cases-referred-to-court")).thenReturn(enveloperFunction);
        when(enveloper.withMetadataFrom(envelope, "progression.command-link-prosecution-cases-to-hearing")).thenReturn(enveloperFunction);
        when(progressionService.getHearing(anyObject(), anyString())).thenReturn(Optional.empty());
        final UpdateHearingForPartialAllocation updateHearingForPartialAllocation = buildUpdateHearingForPartialAllocation(hearingId);
        final ListCourtHearing listCourtHearing =buildListCourtHearing(randomUUID());
        when(partialHearingConfirmService.transformToUpdateHearingForPartialAllocation(hearingId, deltaProsecutionCases)).thenReturn(updateHearingForPartialAllocation);
        when(partialHearingConfirmService.transformToListCourtHearing(eq(deltaProsecutionCases),any())).thenReturn(listCourtHearing);

        eventProcessor.processEvent(envelope);

        verify(progressionService).updateHearingForPartialAllocation(envelope,updateHearingForPartialAllocation);
        verify(progressionService).updateHearingForPartialAllocation(envelope,updateHearingForPartialAllocation);
        verify(listingService).listCourtHearing(envelope,listCourtHearing);


    }

    private UpdateHearingForPartialAllocation buildUpdateHearingForPartialAllocation(UUID hearingId){
       return UpdateHearingForPartialAllocation.updateHearingForPartialAllocation()
                .withHearingId(hearingId)
                .build();
    }

    private ListCourtHearing buildListCourtHearing(UUID hearingId){
        return ListCourtHearing.listCourtHearing()
                .withHearings(Arrays.asList(HearingListingNeeds.hearingListingNeeds()
                        .withId(hearingId)
                        .build()))
                .build();
    }

    @Test
    public void shouldProcessHearingConfirmedForExtendHearing()  {
        final UUID offenceId = randomUUID();
        final UUID defendantId = randomUUID();
        final UUID caseId = randomUUID();

        final ConfirmedProsecutionCase confirmedProsecutionCase = createConfirmedProsecutionCase(caseId, defendantId, offenceId);
        final ConfirmedHearing confirmedHearing = ConfirmedHearing.confirmedHearing()
                .withId(randomUUID())
                .withProsecutionCases(singletonList(confirmedProsecutionCase))
                .withExistingHearingId(randomUUID())
                .build();

        final Hearing hearing = Hearing.hearing()
                .withProsecutionCases(Arrays.asList(ProsecutionCase.prosecutionCase().withId(randomUUID()).build())).build();

        hearingListingNeeds = HearingListingNeeds.hearingListingNeeds()
                .withId(randomUUID())
                        .build();

        when(hearingConfirmed.getConfirmedHearing()).thenReturn(confirmedHearing);
        when(envelope.payloadAsJsonObject()).thenReturn(payload);
        when(jsonObjectToObjectConverter.convert(any(JsonObject.class), any())).thenReturn(hearingConfirmed).thenReturn(hearing);
        when(progressionService.getHearing(any(), any())).thenReturn(Optional.of(Json.
                        createObjectBuilder().add("hearing", Json.createObjectBuilder().build())
                        .add("hearingListingStatus", "HEARING_INITIALISED")
                        .build()));
        when(progressionService.transformConfirmedHearing(any(), any())).thenReturn(
                Hearing.hearing()
                        .withId(UUID.randomUUID())
                        .withHearingDays(Arrays.asList(HearingDay.hearingDay().build()))
                        .withProsecutionCases(Arrays.asList(ProsecutionCase.prosecutionCase()
                                .withDefendants(Arrays.asList(Defendant.defendant()
                                        .withId(UUID.randomUUID())
                                        .withOffences(Arrays.asList(Offence.offence()
                                                .withId(UUID.randomUUID())
                                                .build()))
                                        .build()))
                                .build()))
                        .build());
        when(enveloper.withMetadataFrom(envelope, "progression.command.extend-hearing")).thenReturn(enveloperFunction);
        when(enveloper.withMetadataFrom(envelope, "progression.command.prepare-summons-data-for-extended-hearing")).thenReturn(enveloperFunction);
        when(enveloper.withMetadataFrom(envelope, "progression.command.extend-hearing-defendant-request-update-requested")).thenReturn(enveloperFunction);
        when(progressionService.transformHearingToHearingListingNeeds(any(Hearing.class), any(UUID.class))).thenReturn(hearingListingNeeds);
        when(partialHearingConfirmService.getDifferences(any(), any())).thenReturn(new ArrayList<>());

        doNothing().when(progressionService).prepareSummonsData(anyObject(), anyObject());

        eventProcessor.processEvent(envelope);

        verify(sender,times(2)).send(any(JsonEnvelope.class));
        verify(objectToJsonObjectConverter,times(2)).convert(any());
        verify(progressionService, times(1)).transformHearingToHearingListingNeeds(any(), any());
    }

    @Test
    public void shouldProcessExtendHearingDefendantRequestCreated()  {
        final UUID offenceId = randomUUID();
        final UUID defendantId = randomUUID();
        final UUID caseId = randomUUID();

        final ConfirmedProsecutionCase confirmedProsecutionCase = createConfirmedProsecutionCase(caseId, defendantId, offenceId);
        final ConfirmedHearing confirmedHearing = ConfirmedHearing.confirmedHearing()
                .withId(randomUUID())
                .withProsecutionCases(singletonList(confirmedProsecutionCase))
                .withExistingHearingId(randomUUID())
                .build();

        final ExtendHearingDefendantRequestCreated extendHearingDefendantRequestCreated = ExtendHearingDefendantRequestCreated.extendHearingDefendantRequestCreated()
                .withConfirmedHearing(confirmedHearing)
                .build();


        when(hearingConfirmed.getConfirmedHearing()).thenReturn(confirmedHearing);
        when(envelope.payloadAsJsonObject()).thenReturn(payload);
        when(jsonObjectToObjectConverter.convert(any(JsonObject.class), any())).thenReturn(extendHearingDefendantRequestCreated);
        when(enveloper.withMetadataFrom(envelope, "progression.command.extend-hearing-defendant-request-update-requested")).thenReturn(enveloperFunction);

        eventProcessor.processExtendHearingDefendantRequestCreated(envelope);

        verify(sender,times(1)).send(any(JsonEnvelope.class));
        verify(objectToJsonObjectConverter,times(1)).convert(any());
    }

    @Test
    public void shouldProcessExtendHearingDefendantRequestUpdated()  {
        final UUID offenceId = randomUUID();
        final UUID defendantId = randomUUID();
        final UUID caseId = randomUUID();

        final ConfirmedProsecutionCase confirmedProsecutionCase = createConfirmedProsecutionCase(caseId, defendantId, offenceId);
        final ConfirmedHearing confirmedHearing = ConfirmedHearing.confirmedHearing()
                .withId(randomUUID())
                .withProsecutionCases(singletonList(confirmedProsecutionCase))
                .withExistingHearingId(randomUUID())
                .build();

        final ExtendHearingDefendantRequestUpdated extendHearingDefendantRequestUpdated = ExtendHearingDefendantRequestUpdated.extendHearingDefendantRequestUpdated()
                .withConfirmedHearing(confirmedHearing)
                .build();


        when(hearingConfirmed.getConfirmedHearing()).thenReturn(confirmedHearing);
        when(envelope.payloadAsJsonObject()).thenReturn(payload);
        when(jsonObjectToObjectConverter.convert(any(JsonObject.class), any())).thenReturn(extendHearingDefendantRequestUpdated);
        when(enveloper.withMetadataFrom(envelope, "progression.command.prepare-summons-data-for-extended-hearing")).thenReturn(enveloperFunction);
        doNothing().when(progressionService).prepareSummonsData(anyObject(), anyObject());

        eventProcessor.processExtendHearingDefendantRequestUpdated(envelope);

        verify(progressionService,times(1)).prepareSummonsData(any(JsonEnvelope.class), any(ConfirmedHearing.class));
        verify(jsonObjectToObjectConverter,times(1)).convert(any(),any());
    }


    @Test
    public void shouldHandleHearingConfirmedWithApplicationsEventMessage() {
        final ConfirmedHearing confirmedHearing = ConfirmedHearing.confirmedHearing()
                .withCourtApplicationIds(singletonList(randomUUID()))
                .build();

        when(hearingConfirmed.getConfirmedHearing()).thenReturn(confirmedHearing);
        doNothing().when(progressionService).prepareSummonsData(anyObject(), anyObject());
        doNothing().when(summonsService).generateSummonsPayload(anyObject(), anyObject());
        when(enveloperFunction.apply(any(JsonObject.class))).thenReturn(finalEnvelope);
        when(progressionService.transformConfirmedHearing(any(), any())).thenReturn(Hearing.hearing()
                .withId(randomUUID())
                .withHearingDays(singletonList(HearingDay.hearingDay().withSittingDay(ZonedDateTime.now(ZoneId.of("UTC"))).build()))
                .withProsecutionCases(singletonList(ProsecutionCase.prosecutionCase()
                        .withDefendants(singletonList(Defendant.defendant()
                                .withId(randomUUID())
                                .withOffences(singletonList(Offence.offence()
                                        .withId(randomUUID())
                                        .build()))
                                .build()))
                        .build()))
                .build());
        doNothing().when(progressionService).updateCourtApplicationStatus(anyObject(), any(UUID.class), anyObject());
        when(enveloper.withMetadataFrom(envelope, "progression.command.update-court-application-status")).thenReturn(enveloperFunction);
        when(enveloper.withMetadataFrom(envelope, "hearing.initiate")).thenReturn(enveloperFunction);
        when(enveloper.withMetadataFrom(envelope, "progression.command-enrich-hearing-initiate")).thenReturn(enveloperFunction);
        when(enveloper.withMetadataFrom(envelope, "progression.command.update-defendant-listing-status")).thenReturn(enveloperFunction);
        when(enveloper.withMetadataFrom(envelope, "public.progression.prosecution-cases-referred-to-court")).thenReturn(enveloperFunction);
        when(envelope.payloadAsJsonObject()).thenReturn(payload);
        when(jsonObjectToObjectConverter.convert(envelope.payloadAsJsonObject(), HearingConfirmed.class))
                .thenReturn(hearingConfirmed);
        when(progressionService.getHearing(anyObject(), anyString())).thenReturn(Optional.empty());
        eventProcessor.processEvent(envelope);

        verify(sender, times(1)).send(finalEnvelope);
        verify(progressionService, times(1)).linkApplicationsToHearing(any(), any(), any(), any());
        verify(progressionService, times(1)).updateCourtApplicationStatus(any(), anyList(), any());
        verify(summonsService, times(1)).generateSummonsPayload(any(), any());
    }

    @Test
    public void shouldHandleHearingConfirmedWithCaseAndApplicationsEventMessage() {
        final ConfirmedProsecutionCase confirmedProsecutionCase = createConfirmedProsecutionCase(randomUUID(), randomUUID(), randomUUID());
        final List<UUID> courtApplicationIds = singletonList(randomUUID());
        final ConfirmedHearing confirmedHearing = ConfirmedHearing.confirmedHearing()
                .withCourtApplicationIds(courtApplicationIds)
                .withProsecutionCases(singletonList(confirmedProsecutionCase))
                .build();

        when(envelope.payloadAsJsonObject()).thenReturn(payload);
        when(jsonObjectToObjectConverter.convert(envelope.payloadAsJsonObject(), HearingConfirmed.class))
                .thenReturn(hearingConfirmed);
        when(hearingConfirmed.getConfirmedHearing()).thenReturn(confirmedHearing);

        doNothing().when(progressionService).prepareSummonsData(anyObject(), anyObject());
        doNothing().when(summonsService).generateSummonsPayload(anyObject(), anyObject());
        when(enveloperFunction.apply(any(JsonObject.class))).thenReturn(finalEnvelope);
        final Hearing hearing = Hearing.hearing()
                .withId(randomUUID())
                .withHearingDays(singletonList(HearingDay.hearingDay().withSittingDay(ZonedDateTime.now(ZoneId.of("UTC"))).build()))
                .withProsecutionCases(singletonList(ProsecutionCase.prosecutionCase()
                        .withDefendants(singletonList(Defendant.defendant()
                                .withId(randomUUID())
                                .withOffences(singletonList(Offence.offence()
                                        .withId(randomUUID())
                                        .build()))
                                .build()))
                        .build()))
                .build();
        when(progressionService.transformConfirmedHearing(confirmedHearing, envelope)).thenReturn(hearing);
        doNothing().when(progressionService).updateCourtApplicationStatus(anyObject(), any(UUID.class), anyObject());
        when(enveloper.withMetadataFrom(envelope, "progression.command.update-court-application-status")).thenReturn(enveloperFunction);
        when(enveloper.withMetadataFrom(envelope, "hearing.initiate")).thenReturn(enveloperFunction);
        when(enveloper.withMetadataFrom(envelope, "progression.command-enrich-hearing-initiate")).thenReturn(enveloperFunction);
        when(enveloper.withMetadataFrom(envelope, "progression.command.update-defendant-listing-status")).thenReturn(enveloperFunction);
        when(enveloper.withMetadataFrom(envelope, "public.progression.prosecution-cases-referred-to-court"))
                .thenReturn(enveloperFunction);
        when(enveloper.withMetadataFrom(envelope, "progression.command-link-prosecution-cases-to-hearing")).thenReturn(enveloperFunction);
        when(progressionService.getHearing(anyObject(), anyString())).thenReturn(Optional.empty());

        eventProcessor.processEvent(envelope);

        verify(sender, times(2)).send(finalEnvelope);
        verify(progressionService, times(1)).linkApplicationsToHearing(envelope, hearing, courtApplicationIds, HEARING_INITIALISED);
        verify(progressionService, times(1)).updateCourtApplicationStatus(envelope, courtApplicationIds, LISTED);
        verify(summonsService, times(1)).generateSummonsPayload(envelope, confirmedHearing);
        verify(progressionService, times(1)).prepareSummonsData(envelope, confirmedHearing);
        verify(progressionService, times(1)).updateCaseStatus(envelope, hearing, courtApplicationIds);
    }

    @Test
    public void shouldCallInitiateHearing() throws Exception {
        final Initiate arbitraryInitiateObj = Initiate.initiate().withHearing(
                Hearing.hearing()
                        .withId(UUID.randomUUID())
                        .withHearingDays(Arrays.asList(HearingDay.hearingDay().build()))
                        .withProsecutionCases(Arrays.asList(ProsecutionCase.prosecutionCase()
                                .withDefendants(Arrays.asList(Defendant.defendant()
                                        .withId(UUID.randomUUID())
                                        .withOffences(Arrays.asList(Offence.offence()
                                                .withId(UUID.randomUUID())
                                                .build()))
                                        .build()))
                                .build()))
                        .build()
        ).build();
        //Given
        when(envelope.payloadAsJsonObject()).thenReturn(payload);
        //When
        when(jsonObjectToObjectConverter.convert(envelope.payloadAsJsonObject(), Initiate.class))
                .thenReturn(arbitraryInitiateObj);


        when(enveloperFunction.apply(any(JsonObject.class))).thenReturn(finalEnvelope);
        when(progressionService.transformConfirmedHearing(any(), any())).thenReturn(
                Hearing.hearing()
                        .withId(UUID.randomUUID())
                        .withHearingDays(Arrays.asList(HearingDay.hearingDay().build()))
                        .withProsecutionCases(Arrays.asList(ProsecutionCase.prosecutionCase()
                                .withDefendants(Arrays.asList(Defendant.defendant()
                                        .withId(UUID.randomUUID())
                                        .withOffences(Arrays.asList(Offence.offence()
                                                .withId(UUID.randomUUID())
                                                .build()))
                                        .build()))
                                .build()))
                        .build());

        when(enveloper.withMetadataFrom(envelope, "hearing.initiate")).thenReturn(enveloperFunction);
        when(enveloper.withMetadataFrom(envelope, "progression.command-enrich-hearing-initiate")).thenReturn(enveloperFunction);
        when(enveloper.withMetadataFrom(envelope, "progression.command.update-defendant-listing-status")).thenReturn(enveloperFunction);
        when(enveloper.withMetadataFrom(envelope, HearingConfirmedEventProcessor.PUBLIC_PROGRESSION_EVENT_PROSECUTION_CASES_REFERRED_TO_COURT))
                .thenReturn(enveloperFunction);

        eventProcessor.processHearingInitiatedEnrichedEvent(envelope);

        //Then
        verify(sender, times(2)).send(finalEnvelope);
    }

    private ConfirmedProsecutionCase createConfirmedProsecutionCase(final UUID prosecutionCaseId, final UUID defendantId, final UUID offenceId) {
        return ConfirmedProsecutionCase.confirmedProsecutionCase()
                .withDefendants(singletonList(createConfirmedDefendant(defendantId, offenceId)))
                .withId(prosecutionCaseId)
                .build();
    }

    private ConfirmedDefendant createConfirmedDefendant(final UUID defendantId, final UUID offenceId) {
        return ConfirmedDefendant.confirmedDefendant()
                .withId(defendantId)
                .withOffences(singletonList(createConfirmedOffence(offenceId)))
                .build();
    }

    private ConfirmedOffence createConfirmedOffence(final UUID offenceId) {
        return ConfirmedOffence.confirmedOffence()
                .withId(offenceId)
                .build();
    }

    private ProsecutionCase createProsecutionCase(final UUID offenceId, final UUID defendantId, final UUID caseId) {
        return ProsecutionCase.prosecutionCase().withId(caseId)
                .withDefendants(singletonList(createDefendant(defendantId, offenceId)))
                .build();
    }

    private Defendant createDefendant(final UUID defendantId, final UUID offenceId) {
        return Defendant.defendant()
                .withId(defendantId)
                .withOffences(singletonList(createOffence(offenceId)))
                .build();
    }

    private Offence createOffence(final UUID offenceId) {
        return Offence.offence()
                .withOffenceCode("one")
                .withId(offenceId)
                .build();
    }


    private JsonObject createProsecutionCaseJson(final UUID offenceId, final UUID defendantId, final UUID caseId) {
        return createObjectBuilder()
                .add("id", caseId.toString())
                .add("defendants", Json.createArrayBuilder().add(createObjectBuilder()
                        .add("id", defendantId.toString())
                        .add("offences", Json.createArrayBuilder().add(createObjectBuilder()
                                .add("id", offenceId.toString())
                                .build())
                                .build())
                        .build())
                        .build())
                .build();
    }
}