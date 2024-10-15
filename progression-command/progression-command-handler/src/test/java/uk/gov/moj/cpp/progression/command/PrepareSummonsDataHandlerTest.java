package uk.gov.moj.cpp.progression.command;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static java.util.UUID.randomUUID;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.notNullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.core.annotation.Component.COMMAND_HANDLER;
import static uk.gov.justice.services.messaging.Envelope.envelopeFrom;
import static uk.gov.justice.services.test.utils.core.helper.EventStreamMockHelper.verifyAppendAndGetArgumentFrom;
import static uk.gov.justice.services.test.utils.core.matchers.HandlerMatcher.isHandler;
import static uk.gov.justice.services.test.utils.core.matchers.HandlerMethodMatcher.method;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeMatcher.jsonEnvelope;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeMetadataMatcher.metadata;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeStreamMatcher.streamContaining;

import uk.gov.justice.core.courts.ConfirmedHearing;
import uk.gov.justice.core.courts.ExtendHearingDefendantRequestCreated;
import uk.gov.justice.core.courts.ExtendHearingDefendantRequestUpdateRequested;
import uk.gov.justice.core.courts.ExtendHearingDefendantRequestUpdated;
import uk.gov.justice.core.courts.ListDefendantRequest;
import uk.gov.justice.core.courts.PrepareSummonsDataForExtendedHearing;
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
import uk.gov.moj.cpp.progression.handler.PrepareSummonsDataHandler;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class PrepareSummonsDataHandlerTest {

    @Mock
    private EventSource eventSource;

    @Mock
    private EventStream eventStream;

    @Mock
    private AggregateService aggregateService;

    @Spy
    private final Enveloper enveloper = EnveloperFactory.createEnveloperWithEvents(ExtendHearingDefendantRequestCreated.class);
    @Spy
    private final Enveloper enveloper1 = EnveloperFactory.createEnveloperWithEvents(ExtendHearingDefendantRequestUpdated.class);


    @InjectMocks
    private PrepareSummonsDataHandler prepareSummonsDataHandler;
    
    @Test
    public void shouldHandleCommandPrepareSummons() {
        assertThat(new PrepareSummonsDataHandler(), isHandler(COMMAND_HANDLER)
                .with(method("handlePrepareSummonsDataForExtendedHearingEvent")
                        .thatHandles("progression.command.prepare-summons-data-for-extended-hearing")
                ));
    }


    @Test
    public void shouldHandleCommandDefedantRequestUpdate() {
        assertThat(new PrepareSummonsDataHandler(), isHandler(COMMAND_HANDLER)
                .with(method("handleExtendHearingDefendantRequestUpdateRequestedEvent")
                        .thatHandles("progression.command.extend-hearing-defendant-request-update-requested")
                ));
    }

    @Test
    public void shouldHandlePrepareSummonsDataForExtendedHearingEvent() throws EventStreamException {
        final ListDefendantRequest listDefendantRequest = ListDefendantRequest.listDefendantRequest()
                .withDefendantId(randomUUID())
                .build();

        final HearingAggregate hearingAggregate = new HearingAggregate();
        when(eventSource.getStreamById(any())).thenReturn(eventStream);
        when(aggregateService.get(eventStream, HearingAggregate.class)).thenReturn(hearingAggregate);

        final List<ListDefendantRequest> listDefendantRequests = new ArrayList<>();
        listDefendantRequests.add(listDefendantRequest);

        hearingAggregate.assignDefendantRequestToExtendHearing(randomUUID(), listDefendantRequests);

        final PrepareSummonsDataForExtendedHearing prepareSummonsDataForExtendedHearing = createPrepareSummonsDataForExtendedHearing();
        hearingAggregate.createListDefendantRequest(prepareSummonsDataForExtendedHearing.getConfirmedHearing());

        final Metadata metadata = Envelope
                .metadataBuilder()
                .withName("progression.command.prepare-summons-data-for-extended-hearing")
                .withId(randomUUID())
                .build();

        final Envelope<PrepareSummonsDataForExtendedHearing> envelope = envelopeFrom(metadata, prepareSummonsDataForExtendedHearing);
        prepareSummonsDataHandler.handlePrepareSummonsDataForExtendedHearingEvent(envelope);
        final Stream<JsonEnvelope> envelopeStream = verifyAppendAndGetArgumentFrom(eventStream);

        assertThat(envelopeStream, streamContaining(
                jsonEnvelope(
                        metadata()
                                .withName("progression.event.extend-hearing-defendant-request-created"),
                        JsonEnvelopePayloadMatcher.payload().isJson(allOf(
                                withJsonPath("$.confirmedHearing", notNullValue()),
                                withJsonPath("$.defendantRequests", notNullValue()))
                        ))
                )
        );
    }


    @Test
    public void shouldHandleExtendHearingDefendantRequestUpdateRequestedEvent() throws EventStreamException {

        final ExtendHearingDefendantRequestUpdateRequested event = createExtendHearingDefendantRequestUpdateRequested();

        final HearingAggregate hearingAggregate = new HearingAggregate();
        when(eventSource.getStreamById(any())).thenReturn(eventStream);
        when(aggregateService.get(eventStream, HearingAggregate.class)).thenReturn(hearingAggregate);

        hearingAggregate.updateListDefendantRequest(event.getDefendantRequests(), event.getConfirmedHearing());

        final Metadata metadata = Envelope
                .metadataBuilder()
                .withName("progression.command.extend-hearing-defendant-request-update-requested")
                .withId(randomUUID())
                .build();

        final Envelope<ExtendHearingDefendantRequestUpdateRequested> envelope = envelopeFrom(metadata, event);
        prepareSummonsDataHandler.handleExtendHearingDefendantRequestUpdateRequestedEvent(envelope);
        final Stream<JsonEnvelope> envelopeStream = verifyAppendAndGetArgumentFrom(eventStream);

        assertThat(envelopeStream, streamContaining(
                jsonEnvelope(
                        metadata()
                                .withName("progression.event.extend-hearing-defendant-request-updated"),
                        JsonEnvelopePayloadMatcher.payload().isJson(allOf(
                                withJsonPath("$.confirmedHearing", notNullValue()),
                                withJsonPath("$.defendantRequests", notNullValue()))
                        ))
                )
        );
    }

    private static ExtendHearingDefendantRequestUpdateRequested createExtendHearingDefendantRequestUpdateRequested() {

        final ListDefendantRequest listDefendantRequest = ListDefendantRequest.listDefendantRequest()
                .withDefendantId(randomUUID())
                .build();

        final List<ListDefendantRequest> listDefendantRequests = new ArrayList<>();
        listDefendantRequests.add(listDefendantRequest);

        return ExtendHearingDefendantRequestUpdateRequested.extendHearingDefendantRequestUpdateRequested()
                .withDefendantRequests(listDefendantRequests)
                .withConfirmedHearing(ConfirmedHearing.confirmedHearing()
                        .withId(randomUUID())
                        .build()).build();
    }


    private static PrepareSummonsDataForExtendedHearing createPrepareSummonsDataForExtendedHearing() {

        return PrepareSummonsDataForExtendedHearing.prepareSummonsDataForExtendedHearing()
                .withConfirmedHearing(ConfirmedHearing.confirmedHearing()
                        .withId(randomUUID())
                        .withExistingHearingId(randomUUID())
                        .build())
                .build();
    }


}

