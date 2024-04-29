package uk.gov.moj.cpp.progression.handler;

import uk.gov.justice.core.courts.RecordPrisonCourtRegisterDocumentSent;
import uk.gov.justice.core.courts.RecordPrisonCourtRegisterFailed;
import uk.gov.justice.core.courts.prisonCourtRegisterDocument.PrisonCourtRegisterDocumentRequest;
import uk.gov.justice.progression.courts.NotifyPrisonCourtRegister;
import uk.gov.justice.services.core.aggregate.AggregateService;
import uk.gov.justice.services.core.annotation.Component;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.eventsourcing.source.core.EventSource;
import uk.gov.justice.services.eventsourcing.source.core.EventStream;
import uk.gov.justice.services.eventsourcing.source.core.exception.EventStreamException;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.moj.cpp.progression.aggregate.CourtCentreAggregate;

import java.util.UUID;
import java.util.stream.Stream;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ServiceComponent(Component.COMMAND_HANDLER)
public class PrisonCourtRegisterHandler extends AbstractCommandHandler {

    @Inject
    private EventSource eventSource;

    @Inject
    private AggregateService aggregateService;

    private static final Logger LOGGER = LoggerFactory.getLogger(PrisonCourtRegisterHandler.class.getName());

    @Handles("progression.command.add-prison-court-register")
    public void handleAddPrisonCourtRegister(final Envelope<PrisonCourtRegisterDocumentRequest> envelope) throws EventStreamException {
        LOGGER.info("progression.command.add-prison-court-register {}", envelope.payload());

        final PrisonCourtRegisterDocumentRequest prisonCourtRegisterDocumentRequest = envelope.payload();

        final UUID courtCentreId = prisonCourtRegisterDocumentRequest.getCourtCentreId();

        final EventStream eventStream = eventSource.getStreamById(prisonCourtRegisterDocumentRequest.getCourtCentreId());

        final CourtCentreAggregate courtCentreAggregate = aggregateService.get(eventStream, CourtCentreAggregate.class);

        final Stream<Object> events = courtCentreAggregate.createPrisonCourtRegister(courtCentreId, prisonCourtRegisterDocumentRequest);

        appendEventsToStream(envelope, eventStream, events);
    }


    @Handles("progression.command.record-prison-court-register-document-sent")
    public void recordPrisonCourtRegisterDocumentSent(final Envelope<RecordPrisonCourtRegisterDocumentSent> envelope) throws EventStreamException {

        LOGGER.info("progression.command.record-prison-court-register-document-sent {}", envelope.payload());

        final RecordPrisonCourtRegisterDocumentSent recordPrisonCourtRegisterDocumentSent = envelope.payload();

        final UUID courtCentreId = recordPrisonCourtRegisterDocumentSent.getCourtCentreId();

        final EventStream eventStream = eventSource.getStreamById(courtCentreId);

        final CourtCentreAggregate courtCentreAggregate = aggregateService.get(eventStream, CourtCentreAggregate.class);

        final Stream<Object> events = courtCentreAggregate.recordPrisonCourtRegisterDocumentSent(courtCentreId, recordPrisonCourtRegisterDocumentSent);

        appendEventsToStream(envelope, eventStream, events);

    }

    @Handles("progression.command.notify-prison-court-register")
    public void handleNotifyCourtCentre(final Envelope<NotifyPrisonCourtRegister> envelope) throws EventStreamException {

        LOGGER.info("progression.command.notify-prison-court-register {}", envelope.payload());

        final NotifyPrisonCourtRegister notifyPrisonCourtRegister = envelope.payload();

        final UUID courtCentreId = notifyPrisonCourtRegister.getCourtCentreId();

        final EventStream eventStream = eventSource.getStreamById(courtCentreId);

        final CourtCentreAggregate courtCentreAggregate = aggregateService.get(eventStream, CourtCentreAggregate.class);

        final Stream<Object> events = courtCentreAggregate.recordPrisonCourtRegisterGenerated(courtCentreId, notifyPrisonCourtRegister);

        appendEventsToStream(envelope, eventStream, events);

    }

    @Handles("progression.command.record-prison-court-register-failed")
    public void handlePrisonCourtRegisterFailed(final Envelope<RecordPrisonCourtRegisterFailed> envelope) throws EventStreamException {

        LOGGER.info("progression.command.record-prison-court-register-failed {}", envelope.payload());

        final RecordPrisonCourtRegisterFailed recordPrisonCourtRegisterFailed = envelope.payload();

        final UUID courtCentreId = recordPrisonCourtRegisterFailed.getCourtCentreId();

        final EventStream eventStream = eventSource.getStreamById(courtCentreId);

        final CourtCentreAggregate courtCentreAggregate = aggregateService.get(eventStream, CourtCentreAggregate.class);

        final Stream<Object> events = courtCentreAggregate.recordPrisonCourtRegisterFailed(courtCentreId, recordPrisonCourtRegisterFailed);

        appendEventsToStream(envelope, eventStream, events);

    }
}
