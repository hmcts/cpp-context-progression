package uk.gov.moj.cpp.progression.handler;

import static java.util.UUID.randomUUID;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.core.annotation.Component.COMMAND_HANDLER;
import static uk.gov.justice.services.messaging.Envelope.envelopeFrom;
import static uk.gov.justice.services.test.utils.core.matchers.HandlerMatcher.isHandler;
import static uk.gov.justice.services.test.utils.core.matchers.HandlerMethodMatcher.method;

import uk.gov.justice.core.courts.CourtHearingRequest;
import uk.gov.justice.core.courts.DefendantsToRemove;
import uk.gov.justice.core.courts.HearingUpdatedForPartialAllocation;
import uk.gov.justice.core.courts.OffencesToRemove;
import uk.gov.justice.core.courts.ProsecutionCasesToRemove;
import uk.gov.justice.core.courts.SplitHearingCommand;
import uk.gov.justice.services.core.aggregate.AggregateService;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.eventsourcing.source.core.EventSource;
import uk.gov.justice.services.eventsourcing.source.core.EventStream;
import uk.gov.justice.services.eventsourcing.source.core.exception.EventStreamException;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.Metadata;
import uk.gov.justice.services.test.utils.core.enveloper.EnveloperFactory;
import uk.gov.moj.cpp.progression.aggregate.CaseAggregate;
import uk.gov.moj.cpp.progression.aggregate.HearingAggregate;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class SplitHearingHandlerTest {

    private static final UUID SOURCE_HEARING_ID = randomUUID();
    private static final UUID CASE_ID = randomUUID();
    private static final UUID DEFENDANT_ID = randomUUID();
    private static final UUID OFFENCE_ID = randomUUID();

    @Mock
    private EventSource eventSource;

    @Mock
    private EventStream newHearingStream;

    @Mock
    private EventStream sourceHearingStream;

    @Mock
    private AggregateService aggregateService;

    @Spy
    private final Enveloper enveloper =
            EnveloperFactory.createEnveloperWithEvents(HearingUpdatedForPartialAllocation.class);

    @InjectMocks
    private SplitHearingHandler splitHearingHandler;

    @Test
    public void shouldHandleCommand() {
        assertThat(new SplitHearingHandler(), isHandler(COMMAND_HANDLER)
                .with(method("handle").thatHandles("progression.command.split-hearing")));
    }

    /**
     * The whole point of the ordering: the new hearing is raised before the offences are removed
     * from the source, so a failure between the two leaves them listed twice rather than not at all.
     */
    @Test
    public void shouldRaiseTheNewHearingBeforeRemovingOffencesFromTheSource() throws EventStreamException {
        final ArgumentCaptor<UUID> streamIds = ArgumentCaptor.forClass(UUID.class);
        when(eventSource.getStreamById(streamIds.capture()))
                .thenReturn(newHearingStream, sourceHearingStream);
        when(aggregateService.get(newHearingStream, HearingAggregate.class)).thenReturn(new HearingAggregate());
        when(aggregateService.get(sourceHearingStream, CaseAggregate.class)).thenReturn(new CaseAggregate());

        splitHearingHandler.handle(splitCommandEnvelope());

        // Two streams, in order: a fresh id for the new hearing, then the source hearing.
        assertThat(streamIds.getAllValues().size(), is(2));
        assertThat(streamIds.getAllValues().get(0).equals(SOURCE_HEARING_ID), is(false));
        assertThat(streamIds.getAllValues().get(1), is(SOURCE_HEARING_ID));

        final InOrder inOrder = inOrder(newHearingStream, sourceHearingStream);
        inOrder.verify(newHearingStream).append(any());
        inOrder.verify(sourceHearingStream).append(any());
    }

    @Test
    public void shouldAppendToAFreshStreamForEverySplit() throws EventStreamException {
        final ArgumentCaptor<UUID> streamIds = ArgumentCaptor.forClass(UUID.class);
        when(eventSource.getStreamById(streamIds.capture()))
                .thenReturn(newHearingStream, sourceHearingStream, newHearingStream, sourceHearingStream);
        when(aggregateService.get(newHearingStream, HearingAggregate.class)).thenReturn(new HearingAggregate());
        when(aggregateService.get(sourceHearingStream, CaseAggregate.class)).thenReturn(new CaseAggregate());

        splitHearingHandler.handle(splitCommandEnvelope());
        splitHearingHandler.handle(splitCommandEnvelope());

        assertThat(streamIds.getAllValues().get(0).equals(streamIds.getAllValues().get(2)), is(false));
    }

    private static Envelope<SplitHearingCommand> splitCommandEnvelope() {
        final Metadata metadata = Envelope.metadataBuilder()
                .withName("progression.command.split-hearing")
                .withId(randomUUID())
                .build();
        return envelopeFrom(metadata, splitCommand());
    }

    private static SplitHearingCommand splitCommand() {
        return SplitHearingCommand.splitHearingCommand()
                .withHearingId(SOURCE_HEARING_ID)
                .withListNewHearing(CourtHearingRequest.courtHearingRequest()
                        .withJurisdictionType(uk.gov.justice.core.courts.JurisdictionType.CROWN)
                        .withEstimatedMinutes(1080)
                        .build())
                .withSendNotificationToParties(false)
                .withProsecutionCasesToRemove(List.of(
                        ProsecutionCasesToRemove.prosecutionCasesToRemove()
                                .withCaseId(CASE_ID)
                                .withDefendantsToRemove(List.of(
                                        DefendantsToRemove.defendantsToRemove()
                                                .withDefendantId(DEFENDANT_ID)
                                                .withOffencesToRemove(List.of(
                                                        OffencesToRemove.offencesToRemove()
                                                                .withOffenceId(OFFENCE_ID).build()))
                                                .build()))
                                .build()))
                .build();
    }
}
