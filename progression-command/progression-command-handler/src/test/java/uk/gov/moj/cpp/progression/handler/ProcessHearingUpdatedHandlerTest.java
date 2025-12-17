package uk.gov.moj.cpp.progression.handler;


import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static java.util.Arrays.asList;
import static java.util.UUID.randomUUID;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.messaging.Envelope.envelopeFrom;
import static uk.gov.justice.services.test.utils.core.helper.EventStreamMockHelper.verifyAppendAndGetArgumentFrom;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeMatcher.jsonEnvelope;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeMetadataMatcher.metadata;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeStreamMatcher.streamContaining;

import uk.gov.justice.core.courts.ConfirmedHearing;
import uk.gov.justice.core.courts.CourtApplication;
import uk.gov.justice.core.courts.CourtCentre;
import uk.gov.justice.core.courts.DefenceCounsel;
import uk.gov.justice.core.courts.Defendant;
import uk.gov.justice.core.courts.Hearing;
import uk.gov.justice.core.courts.HearingDay;
import uk.gov.justice.core.courts.HearingInitiateEnriched;
import uk.gov.justice.core.courts.HearingListingNumberUpdated;
import uk.gov.justice.core.courts.HearingUpdatedProcessed;
import uk.gov.justice.core.courts.Offence;
import uk.gov.justice.core.courts.OffenceListingNumbers;
import uk.gov.justice.core.courts.ProcessHearingUpdated;
import uk.gov.justice.core.courts.ProsecutionCase;
import uk.gov.justice.core.courts.UpdateListingNumberToHearing;
import uk.gov.justice.progression.courts.HearingResulted;
import uk.gov.justice.services.core.aggregate.AggregateService;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.eventsourcing.source.core.EventSource;
import uk.gov.justice.services.eventsourcing.source.core.EventStream;
import uk.gov.justice.services.eventsourcing.source.core.exception.EventStreamException;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.messaging.Metadata;
import uk.gov.justice.services.test.utils.core.enveloper.EnveloperFactory;
import uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopePayloadMatcher;
import uk.gov.moj.cpp.progression.aggregate.HearingAggregate;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class ProcessHearingUpdatedHandlerTest {

    @Mock
    private AggregateService aggregateService;

    @Mock
    private EventSource eventSource;

    @Mock
    private EventStream eventStream;

    @Spy
    private final Enveloper enveloper = EnveloperFactory.createEnveloperWithEvents(HearingUpdatedProcessed.class,
            HearingListingNumberUpdated.class);


    private HearingAggregate hearingAggregate;

    @InjectMocks
    private ProcessHearingUpdatedHandler processHearingUpdatedHandler;

    @BeforeEach
    public void setup() {

        hearingAggregate = new HearingAggregate();
        when(eventSource.getStreamById(any())).thenReturn(eventStream);
        when(aggregateService.get(eventStream, HearingAggregate.class)).thenReturn(hearingAggregate);
    }

    @Test
    public void shouldHandProcessHearingUpdated() throws EventStreamException {
        final UUID hearingId = randomUUID();
        final ProcessHearingUpdated processHearingUpdated = ProcessHearingUpdated.processHearingUpdated()
                .withConfirmedHearing(ConfirmedHearing.confirmedHearing().withId(hearingId)
                        .build())
                .withUpdatedHearing(Hearing.hearing().withId(hearingId)
                        .withIsBoxHearing(true)
                        .withCourtApplications(Collections.singletonList(CourtApplication.courtApplication().build()))
                        .build())
                .build();

        final Metadata metadata = Envelope
                .metadataBuilder()
                .withName("progression.command.process-hearing-updated")
                .withId(randomUUID())
                .build();
        hearingAggregate.apply(HearingResulted.hearingResulted().withHearing(Hearing.hearing().withId(hearingId).build()).build());

        final Envelope<ProcessHearingUpdated> envelope = envelopeFrom(metadata, processHearingUpdated);
        processHearingUpdatedHandler.handle(envelope);
        final Stream<JsonEnvelope> envelopeStream = verifyAppendAndGetArgumentFrom(eventStream);

        assertThat(envelopeStream, streamContaining(
                        jsonEnvelope(
                                metadata()
                                        .withName("progression.event.hearing-updated-processed"),
                                JsonEnvelopePayloadMatcher.payload().isJson(allOf(
                                        withJsonPath("$.confirmedHearing.id", is(hearingId.toString())),
                                        withJsonPath("$.hearing.id", is(hearingId.toString())))
                                ))
                )
        );
    }

    @Test
    public void shouldKeepListingNumbersWhenProcessHearingUpdated() throws EventStreamException {
        final UUID hearingId = randomUUID();
        final UUID offenceId1 = randomUUID();
        final UUID offenceId2 = randomUUID();

        final Hearing originalHearing = Hearing.hearing().withId(hearingId)
                .withProsecutionCases(Collections.singletonList(ProsecutionCase.prosecutionCase()
                        .withDefendants(Collections.singletonList(Defendant.defendant().withId(randomUUID())
                                .withOffences(asList(Offence.offence()
                                                .withId(offenceId1)
                                                .withListingNumber(2)
                                                .build(),
                                        Offence.offence()
                                                .withId(offenceId2)
                                                .withListingNumber(2)
                                                .build()))
                                .build()))
                        .build()))
                .withIsBoxHearing(true)
                .withCourtApplications(Collections.singletonList(CourtApplication.courtApplication().build()))
                .build();

        final Hearing updatedHearing = Hearing.hearing().withId(hearingId)
                .withProsecutionCases(Collections.singletonList(ProsecutionCase.prosecutionCase()
                        .withDefendants(Collections.singletonList(Defendant.defendant().withId(randomUUID())
                                .withOffences(asList(Offence.offence()
                                                .withId(offenceId1)
                                                .withListingNumber(1)
                                                .build(),
                                        Offence.offence()
                                                .withId(offenceId2)
                                                .withListingNumber(1)
                                                .build()))
                                .build()))
                        .build()))
                .withIsBoxHearing(true)
                .withCourtApplications(Collections.singletonList(CourtApplication.courtApplication().build()))
                .build();

        final ProcessHearingUpdated processHearingUpdated = ProcessHearingUpdated.processHearingUpdated()
                .withConfirmedHearing(ConfirmedHearing.confirmedHearing().withId(hearingId).build())
                .withUpdatedHearing(updatedHearing)
                .build();

        final Metadata metadata = Envelope
                .metadataBuilder()
                .withName("progression.command.process-hearing-updated")
                .withId(randomUUID())
                .build();
        hearingAggregate.apply(HearingResulted.hearingResulted().withHearing(originalHearing).build());

        final Envelope<ProcessHearingUpdated> envelope = envelopeFrom(metadata, processHearingUpdated);
        processHearingUpdatedHandler.handle(envelope);
        final Stream<JsonEnvelope> envelopeStream = verifyAppendAndGetArgumentFrom(eventStream);

        assertThat(envelopeStream, streamContaining(
                jsonEnvelope(
                        metadata()
                                .withName("progression.event.hearing-updated-processed"),
                        JsonEnvelopePayloadMatcher.payload().isJson(allOf(
                                withJsonPath("$.confirmedHearing.id", is(hearingId.toString())),
                                withJsonPath("$.hearing.id", is(hearingId.toString())),
                                withJsonPath("$.hearing.prosecutionCases[0].defendants[0].offences[0].listingNumber", is(2)),
                                withJsonPath("$.hearing.prosecutionCases[0].defendants[0].offences[0].listingNumber", is(2))
                        ))
                )
        ));
    }

    @Test
    public void shoudHandleListingNumber() throws EventStreamException {
        final List<DefenceCounsel> defenceCounsels = new ArrayList<>();
        defenceCounsels.add(DefenceCounsel.defenceCounsel()
                .withId(randomUUID())
                .build());

        Hearing hearing = Hearing.hearing()
                .withCourtCentre(CourtCentre.courtCentre().build())
                .withDefenceCounsels(defenceCounsels)
                .withId(randomUUID())
                .withHearingDays(asList(
                        HearingDay.hearingDay()
                                .withCourtRoomId(randomUUID())
                                .build(),
                        HearingDay.hearingDay()
                                .build()))
                .withProsecutionCases(asList(ProsecutionCase.prosecutionCase()
                                .withId(randomUUID())
                                .withDefendants(asList(Defendant.defendant()
                                        .withId(randomUUID())
                                        .withOffences(new ArrayList<>(asList(Offence.offence()
                                                        .withId(randomUUID())
                                                        .build(),
                                                Offence.offence()
                                                        .withId(randomUUID())
                                                        .build())))
                                        .build()))
                                .build(),
                        ProsecutionCase.prosecutionCase()
                                .withId(randomUUID())
                                .withDefendants(asList(Defendant.defendant()
                                        .withId(randomUUID())
                                        .withOffences(asList(Offence.offence()
                                                .withId(randomUUID())
                                                .build()))
                                        .build()))
                                .build()))
                .build();
        hearingAggregate.apply(HearingInitiateEnriched.hearingInitiateEnriched()
                .withHearing(hearing)
                .build());
        final List<OffenceListingNumbers> offenceListingNumbers = new ArrayList<>();
        offenceListingNumbers.add(OffenceListingNumbers.offenceListingNumbers()
                .withListingNumber(Integer.valueOf(3))
                .withOffenceId(randomUUID())
        .build());
        final UpdateListingNumberToHearing updateListingNumberToHearing = UpdateListingNumberToHearing.updateListingNumberToHearing()
                .withHearingId(randomUUID())
                .withOffenceListingNumbers(offenceListingNumbers)
                .build();
        final Metadata metadata = Envelope
                .metadataBuilder()
                .withName("progression.command.process-hearing-updated")
                .withId(randomUUID())
                .build();

        final Envelope<UpdateListingNumberToHearing> envelope = envelopeFrom(metadata, updateListingNumberToHearing);
        processHearingUpdatedHandler.handleListingNumber(envelope);

        final Stream<JsonEnvelope> envelopeStream = verifyAppendAndGetArgumentFrom(eventStream);

        final List<Envelope> envelopes = envelopeStream.map(value -> (Envelope) value).collect(Collectors.toList());
        final JsonEnvelope  resultEnvelope = (JsonEnvelope)envelopes.stream().filter(
                env -> env.metadata().name().equals("progression.event.hearing-listing-number-updated")).findFirst().get();

        assertThat(resultEnvelope.payloadAsJsonObject()
                , notNullValue());
    }
}
