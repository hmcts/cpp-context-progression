package uk.gov.moj.cpp.progression.processor;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static java.util.UUID.randomUUID;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.justice.services.test.utils.core.enveloper.EnveloperFactory.createEnveloper;
import static uk.gov.justice.services.test.utils.core.messaging.MetadataBuilderFactory.metadataWithRandomUUID;

import uk.gov.justice.core.courts.ConfirmedDefendant;
import uk.gov.justice.core.courts.ConfirmedOffence;
import uk.gov.justice.core.courts.ConfirmedProsecutionCase;
import uk.gov.justice.core.courts.CourtApplication;
import uk.gov.justice.core.courts.Defendant;
import uk.gov.justice.core.courts.Hearing;
import uk.gov.justice.core.courts.HearingDay;
import uk.gov.justice.core.courts.HearingExtended;
import uk.gov.justice.core.courts.HearingExtendedProcessed;
import uk.gov.justice.core.courts.HearingListingNeeds;
import uk.gov.justice.core.courts.Offence;
import uk.gov.justice.core.courts.ProsecutionCase;
import uk.gov.justice.listing.events.CaseAddedToHearing;
import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.common.converter.ObjectToJsonObjectConverter;
import uk.gov.justice.services.common.converter.jackson.ObjectMapperProducer;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.messaging.spi.DefaultEnvelope;
import uk.gov.moj.cpp.progression.service.PartialHearingConfirmService;
import uk.gov.moj.cpp.progression.service.ProgressionService;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.UUID;

import javax.json.JsonObject;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@SuppressWarnings({"squid:S1607"})
public class ExtendedHearingProcessorTest {

    @InjectMocks
    private ExtendedHearingProcessor eventProcessor;

    @Mock
    private HearingExtendedProcessed hearingExtendedProcessed;

    @Mock
    private HearingExtended hearingExtended;

    @Mock
    private JsonEnvelope jsonEnvelope;

    @Mock
    private JsonObject payload;

    private final ObjectMapper objectMapper = new ObjectMapperProducer().objectMapper();

    @Mock
    private JsonObjectToObjectConverter jsonObjectToObjectConverter;

    @Mock
    private Sender sender;
    @Mock
    private ProgressionService progressionService;
    @Mock
    private CourtApplication courtApplication;
    @Mock
    private ProsecutionCase prosecutionCase;
    @Mock
    private ObjectToJsonObjectConverter objectToJsonObjectConverter;

    private ObjectToJsonObjectConverter objectToJsonObjectConverterForTest = new ObjectToJsonObjectConverter(objectMapper);

    @Spy
    private final Enveloper enveloper = createEnveloper();

    @Spy
    private PartialHearingConfirmService partialHearingConfirmService = new PartialHearingConfirmService();

    @BeforeEach
    public void initMocks() {
        MockitoAnnotations.initMocks(this);
    }

    @Captor
    private ArgumentCaptor<DefaultEnvelope> senderJsonEnvelopeCaptor;

    @Test
    public void shouldHandleHearingExtendedEventMessageForExistingHearingForApplication() {
        HearingListingNeeds hearingListingNeeds = HearingListingNeeds.hearingListingNeeds()
                .withId(randomUUID())
                .withCourtApplications(asList(courtApplication)).build();

        when(jsonEnvelope.payloadAsJsonObject()).thenReturn(payload);
        when(jsonObjectToObjectConverter.convert(jsonEnvelope.payloadAsJsonObject(), HearingExtendedProcessed.class)).thenReturn(hearingExtendedProcessed);
        when(objectToJsonObjectConverter.convert(Mockito.any(CourtApplication.class))).thenReturn(payload);
        when(hearingExtendedProcessed.getHearingRequest()).thenReturn(hearingListingNeeds);
        when(hearingExtendedProcessed.getHearing()).thenReturn(Hearing.hearing().build());

        final JsonEnvelope event = envelopeFrom(metadataWithRandomUUID("progression.event.hearing-extended-processed"),
                jsonEnvelope.payloadAsJsonObject());
        this.eventProcessor.processed(event);
        verify(sender).send(senderJsonEnvelopeCaptor.capture());
        verify(progressionService).updateDefendantYouthForProsecutionCase(any(), any());

        assertThat(senderJsonEnvelopeCaptor.getValue().metadata().name(), is("public.progression.events.hearing-extended"));
    }

    @Test
    public void shouldHandleHearingExtendedEventMessageForExistingHearingForCase() {


        final UUID hearingId = randomUUID();
        final HearingListingNeeds hearingListingNeeds = HearingListingNeeds.hearingListingNeeds()
                .withId(hearingId)
                .withProsecutionCases(asList(prosecutionCase)).build();

        when(jsonEnvelope.payloadAsJsonObject()).thenReturn(payload);
        when(jsonObjectToObjectConverter.convert(jsonEnvelope.payloadAsJsonObject(), HearingExtendedProcessed.class)).thenReturn(hearingExtendedProcessed);
        when(hearingExtendedProcessed.getHearingRequest()).thenReturn(hearingListingNeeds);
        when(hearingExtendedProcessed.getHearing()).thenReturn(Hearing.hearing().build());
        final JsonEnvelope event = envelopeFrom(metadataWithRandomUUID("progression.event.hearing-extended-processed"),
                jsonEnvelope.payloadAsJsonObject());
        this.eventProcessor.processed(event);
        verify(sender).send(senderJsonEnvelopeCaptor.capture());
        verify(progressionService, times(1)).linkProsecutionCasesToHearing(any(JsonEnvelope.class),any(UUID.class),any(List.class));
        verify(progressionService).updateDefendantYouthForProsecutionCase(any(), anyList());
        verify(progressionService).populateHearingToProbationCaseworker(any(JsonEnvelope.class), eq(hearingId));
        assertThat(senderJsonEnvelopeCaptor.getValue().metadata().name(), is("public.progression.events.hearing-extended"));
    }

    @Test
    public void shouldProcessHearingExtended() {
        final UUID hearingId = randomUUID();
        final UUID shadowOffence = randomUUID();
        final HearingListingNeeds hearingListingNeeds = HearingListingNeeds.hearingListingNeeds()
                .withId(hearingId)
                .withProsecutionCases(asList(prosecutionCase)).build();

        when(jsonEnvelope.payloadAsJsonObject()).thenReturn(payload);
        when(jsonObjectToObjectConverter.convert(jsonEnvelope.payloadAsJsonObject(), HearingExtended.class)).thenReturn(hearingExtended);
        when(hearingExtended.getHearingRequest()).thenReturn(hearingListingNeeds);

        when(hearingExtended.getShadowListedOffences()).thenReturn(asList(shadowOffence));
        when(objectToJsonObjectConverter.convert(Mockito.any(HearingListingNeeds.class))).thenReturn(payload);

        final JsonEnvelope event = envelopeFrom(metadataWithRandomUUID("progression.event.hearing-extended"),
                jsonEnvelope.payloadAsJsonObject());
        this.eventProcessor.process(event);

        verify(sender).send(senderJsonEnvelopeCaptor.capture());
        assertThat(senderJsonEnvelopeCaptor.getValue().metadata().name(), is("progression.command.process-hearing-extended"));
        final JsonObject commandPayload = (JsonObject) senderJsonEnvelopeCaptor.getValue().payload();
        assertThat(commandPayload.getJsonArray("shadowListedOffences").getString(0), is(shadowOffence.toString()));

    }

    @Test
    public void shouldAddCaseToUnAllocatedHearing(){
        final UUID hearingId = randomUUID();
        final UUID hearingId2 = randomUUID();
        final UUID caseId = randomUUID();
        final UUID defendantId = randomUUID();
        final UUID offenceId = randomUUID();

        final UUID caseId2 = randomUUID();
        final UUID defendantId2 = randomUUID();
        final UUID offenceId2 = randomUUID();

        final CaseAddedToHearing caseAddedToHearing = CaseAddedToHearing.caseAddedToHearing()
                .withHearingId(hearingId)
                .withExistingHearingId(hearingId)
                .withConfirmedProsecutionCase(asList(ConfirmedProsecutionCase.confirmedProsecutionCase()
                        .withId(caseId)
                        .withDefendants(singletonList(ConfirmedDefendant.confirmedDefendant()
                                .withId(defendantId)
                                .withOffences(singletonList(ConfirmedOffence.confirmedOffence()
                                        .withId(offenceId)
                                        .build()))
                                .build()))
                        .build(), ConfirmedProsecutionCase.confirmedProsecutionCase()
                        .withId(caseId2)
                        .withDefendants(singletonList(ConfirmedDefendant.confirmedDefendant()
                                .withId(defendantId2)
                                .withOffences(singletonList(ConfirmedOffence.confirmedOffence()
                                        .withId(offenceId2)
                                        .build()))
                                .build()))
                        .build()))
                .build();
        final JsonObject payload = objectToJsonObjectConverterForTest.convert(caseAddedToHearing);

        final JsonEnvelope event = envelopeFrom(metadataWithRandomUUID("public.listing.case-added-to-hearing"),
                payload);
        final Hearing storedHearing = Hearing.hearing().withId(hearingId)
                .withProsecutionCases(singletonList(ProsecutionCase.prosecutionCase()
                        .withId(caseId2)
                        .withDefendants(singletonList(Defendant.defendant()
                                .withId(defendantId2)
                                .withOffences(singletonList(Offence.offence().withId(offenceId2).build()))
                                .build()))
                        .build()))
                .withHearingDays(singletonList(HearingDay.hearingDay()
                        .withSittingDay(ZonedDateTime.now())
                        .build()))
                .build();
        when(progressionService.retrieveHearing(any(), any())).thenReturn(storedHearing);
        when(jsonObjectToObjectConverter.convert(any(), eq(CaseAddedToHearing.class))).thenReturn(caseAddedToHearing);
        this.eventProcessor.addCasesToUnAllocatedHearing(event);

        verify(sender, times(2)).send(senderJsonEnvelopeCaptor.capture());
        assertThat(senderJsonEnvelopeCaptor.getAllValues().get(0).metadata().name(), is("progression.command-link-prosecution-cases-to-hearing"));


        JsonObject commandPayload = (JsonObject) senderJsonEnvelopeCaptor.getAllValues().get(0).payload();
        assertThat(commandPayload.getString("hearingId"), is(hearingId.toString()));
        assertThat(commandPayload.getString("caseId"), is(caseId.toString()));

        assertThat(senderJsonEnvelopeCaptor.getAllValues().get(1).metadata().name(), is("progression.command.extend-hearing"));

    }

}

