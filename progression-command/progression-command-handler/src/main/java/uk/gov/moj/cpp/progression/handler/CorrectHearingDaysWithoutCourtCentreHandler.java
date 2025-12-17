package uk.gov.moj.cpp.progression.handler;

import static javax.json.JsonValue.NULL;
import static uk.gov.justice.services.core.annotation.Component.COMMAND_HANDLER;
import static uk.gov.justice.services.core.enveloper.Enveloper.toEnvelopeWithMetadataFrom;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;

import uk.gov.justice.listing.courts.CorrectHearingDaysWithoutCourtCentre;
import uk.gov.justice.progression.events.HearingDaysWithoutCourtCentreCorrected;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.eventsourcing.source.core.EventSource;
import uk.gov.justice.services.eventsourcing.source.core.EventStream;
import uk.gov.justice.services.eventsourcing.source.core.exception.EventStreamException;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.JsonEnvelope;

import java.util.UUID;
import java.util.stream.Stream;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ServiceComponent(COMMAND_HANDLER)
public class CorrectHearingDaysWithoutCourtCentreHandler {

    @Inject
    private EventSource eventSource;

    @Inject
    private Enveloper enveloper;

    private static final Logger LOGGER = LoggerFactory.getLogger(CorrectHearingDaysWithoutCourtCentreHandler.class);

    @Handles("progression.command.correct-hearing-days-without-court-centre")
    public void handleCorrectHearingDaysWithoutCourtCentre(final Envelope<CorrectHearingDaysWithoutCourtCentre> commandEnvelope) throws EventStreamException {
        final CorrectHearingDaysWithoutCourtCentre correctHearingDaysWithoutCourtCentre = commandEnvelope.payload();

        LOGGER.debug("progression.command.correct-hearing-days-without-court-centre payload {}", correctHearingDaysWithoutCourtCentre);

        final UUID hearingId = correctHearingDaysWithoutCourtCentre.getId();
        final EventStream eventStream = eventSource.getStreamById(hearingId);

        final HearingDaysWithoutCourtCentreCorrected hearingDaysWithoutCourtCentreCorrectedEvent = HearingDaysWithoutCourtCentreCorrected
                .hearingDaysWithoutCourtCentreCorrected()
                .withId(hearingId)
                .withHearingDays(correctHearingDaysWithoutCourtCentre.getHearingDays())
                .build();

        final Stream<Object> events = Stream.of(hearingDaysWithoutCourtCentreCorrectedEvent);
        appendEventsToStream(commandEnvelope, eventStream, events);
    }

    private void appendEventsToStream(final Envelope<?> envelope, final EventStream eventStream, final Stream<Object> events) throws EventStreamException {
        final JsonEnvelope jsonEnvelope = envelopeFrom(envelope.metadata(), NULL);
        eventStream.append(events.map(toEnvelopeWithMetadataFrom(jsonEnvelope)));
    }
}
