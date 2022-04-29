package uk.gov.moj.cpp.progression.handler;

import static java.util.Objects.nonNull;

import uk.gov.justice.core.courts.ConfirmedHearing;
import uk.gov.justice.core.courts.ExtendHearingDefendantRequestUpdateRequested;
import uk.gov.justice.core.courts.PrepareSummonsData;
import uk.gov.justice.core.courts.PrepareSummonsDataForExtendedHearing;
import uk.gov.justice.services.core.aggregate.AggregateService;
import uk.gov.justice.services.core.annotation.Component;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.eventsourcing.source.core.EventSource;
import uk.gov.justice.services.eventsourcing.source.core.EventStream;
import uk.gov.justice.services.eventsourcing.source.core.exception.EventStreamException;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.progression.aggregate.HearingAggregate;

import java.util.UUID;
import java.util.stream.Stream;

import javax.inject.Inject;
import javax.json.JsonValue;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@SuppressWarnings("squid:S3655")
@ServiceComponent(Component.COMMAND_HANDLER)
public class PrepareSummonsDataHandler {

    @Inject
    private EventSource eventSource;

    @Inject
    private AggregateService aggregateService;

    @Inject
    private Enveloper enveloper;

    private static final Logger LOGGER = LoggerFactory.getLogger(PrepareSummonsDataHandler.class.getName());

    @Handles("progression.command.prepare-summons-data")
    public void prepareSummonsData(final Envelope<PrepareSummonsData> prepareSummonsDataEnvelope) throws EventStreamException {
        LOGGER.debug("progression.command.prepare-summons-data {}", prepareSummonsDataEnvelope);
        final PrepareSummonsData requestSummons = prepareSummonsDataEnvelope.payload();
        final EventStream eventStream = eventSource.getStreamById(requestSummons.getHearingId());
        final HearingAggregate hearingAggregate = aggregateService.get(eventStream, HearingAggregate.class);
        final Stream<Object> events = hearingAggregate.createSummonsData(requestSummons.getCourtCentre(), requestSummons.getHearingDateTime(), requestSummons.getConfirmedProsecutionCaseIds(), requestSummons.getConfirmedApplicationIds());
        if (events != null) {
            appendEventsToStream(prepareSummonsDataEnvelope, eventStream, events);
        }
    }

    @Handles("progression.command.prepare-summons-data-for-extended-hearing")
    public void handlePrepareSummonsDataForExtendedHearingEvent(final Envelope<PrepareSummonsDataForExtendedHearing> envelope)
            throws EventStreamException {

        LOGGER.debug("progression.command.prepare-summons-data-for-extended-hearing {}", envelope);

        final PrepareSummonsDataForExtendedHearing prepareSummonsDataForExtendedHearing = envelope.payload();
        final ConfirmedHearing confirmedHearing = prepareSummonsDataForExtendedHearing.getConfirmedHearing();

        // aggregating based on unallocated hearing id
        final EventStream eventStream = eventSource.getStreamById(confirmedHearing.getId());
        final HearingAggregate hearingAggregate = aggregateService.get(eventStream, HearingAggregate.class);
        final Stream<Object> events = hearingAggregate.createListDefendantRequest(confirmedHearing);

        if (nonNull(events)) {
            appendEventsToStream(envelope, eventStream, events);
        }
    }

    @Handles("progression.command.extend-hearing-defendant-request-update-requested")
    public void handleExtendHearingDefendantRequestUpdateRequestedEvent(final Envelope<ExtendHearingDefendantRequestUpdateRequested> envelope)
            throws EventStreamException {

        LOGGER.debug("progression.command.extend-hearing-defendant-request-update-requested {}", envelope);

        final ExtendHearingDefendantRequestUpdateRequested extendHearingDefendantRequestUpdateRequested = envelope.payload();

        // aggregating based on allocated hearing id
        final UUID hearingId = extendHearingDefendantRequestUpdateRequested.getConfirmedHearing().getExistingHearingId();
        final EventStream eventStream = eventSource.getStreamById(hearingId);
        final HearingAggregate hearingAggregate = aggregateService.get(eventStream, HearingAggregate.class);

        final Stream<Object> events =
                hearingAggregate.updateListDefendantRequest(extendHearingDefendantRequestUpdateRequested.getDefendantRequests(),
                        extendHearingDefendantRequestUpdateRequested.getConfirmedHearing());

        if (nonNull(events)) {
            appendEventsToStream(envelope, eventStream, events);
        }
    }

    private void appendEventsToStream(final Envelope<?> envelope, final EventStream eventStream, final Stream<Object> events) throws EventStreamException {
        final JsonEnvelope jsonEnvelope = JsonEnvelope.envelopeFrom(envelope.metadata(), JsonValue.NULL);
        eventStream.append(
                events
                        .map(enveloper.withMetadataFrom(jsonEnvelope)));
    }

}
