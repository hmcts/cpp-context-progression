package uk.gov.moj.cpp.progression.handler.cotr;

import static uk.gov.justice.services.core.annotation.Component.COMMAND_HANDLER;

import uk.gov.justice.core.courts.CreateCotr;
import uk.gov.justice.core.courts.RequestCotrTask;
import uk.gov.justice.core.courts.ServeDefendantCotr;
import uk.gov.justice.cpp.progression.event.UpdateProsecutionCotr;
import uk.gov.justice.progression.courts.AddFurtherInfoDefenceCotrCommand;
import uk.gov.justice.progression.courts.AddFurtherInfoProsecutionCotr;
import uk.gov.justice.progression.courts.ChangeDefendantsCotr;
import uk.gov.justice.progression.courts.UpdateReviewNotes;
import uk.gov.justice.services.core.aggregate.AggregateService;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.eventsourcing.source.core.EventSource;
import uk.gov.justice.services.eventsourcing.source.core.EventStream;
import uk.gov.justice.services.eventsourcing.source.core.exception.EventStreamException;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.moj.cpp.progression.aggregate.CotrAggregate;
import uk.gov.moj.cpp.progression.command.ServeProsecutionCotr;
import uk.gov.moj.cpp.progression.handler.AbstractCommandHandler;

import java.util.UUID;
import java.util.stream.Stream;

import javax.inject.Inject;
import javax.json.JsonObject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ServiceComponent(COMMAND_HANDLER)
public class CotrCommandHandler extends AbstractCommandHandler {

    private static final Logger LOGGER =
            LoggerFactory.getLogger(CotrCommandHandler.class.getName());

    @Inject
    private EventSource eventSource;

    @Inject
    private AggregateService aggregateService;

    @Handles("progression.command.create-cotr")
    public void createCotr(final Envelope<CreateCotr> envelope) throws EventStreamException {
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("progression.command.create-cotr command received {}", envelope.payload());
        }
        final CreateCotr createCotr = envelope.payload();
        final EventStream eventStream = eventSource.getStreamById(createCotr.getCotrId());
        final CotrAggregate cotrAggregate = aggregateService.get(eventStream, CotrAggregate.class);
        final Stream<Object> events = cotrAggregate.createCotr(createCotr);
        appendEventsToStream(envelope, eventStream, events);
    }

    @Handles("progression.command.serve-prosecution-cotr")
    public void serveProsecutionCotr(final Envelope<ServeProsecutionCotr> envelope) throws EventStreamException {
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("progression.command.serve-prosecution-cotr command received {}", envelope.payload());
        }
        final ServeProsecutionCotr serveProsecutionCotr = envelope.payload();
        final EventStream eventStream = eventSource.getStreamById(serveProsecutionCotr.getCotrId());
        final CotrAggregate cotrAggregate = aggregateService.get(eventStream, CotrAggregate.class);
        final Stream<Object> events = cotrAggregate.serveProsecutionCotr(serveProsecutionCotr);
        appendEventsToStream(envelope, eventStream, events);
    }

    @Handles("progression.command.update-prosecution-cotr")
    public void updateProsecutionCotr(final Envelope<UpdateProsecutionCotr> envelope) throws EventStreamException {
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("progression.command.update-prosecution-cotr command received {}", envelope.payload());
        }
        final UpdateProsecutionCotr updateProsecutionCotr = envelope.payload();
        final EventStream eventStream = eventSource.getStreamById(updateProsecutionCotr.getCotrId());
        final CotrAggregate cotrAggregate = aggregateService.get(eventStream, CotrAggregate.class);
        final Stream<Object> events = cotrAggregate.updateProsecutionCotr(updateProsecutionCotr);
        appendEventsToStream(envelope, eventStream, events);
    }

    @Handles("progression.command.archive-cotr")
    public void archiveCotr(final Envelope<JsonObject> envelope) throws EventStreamException {
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("progression.command.archive-cotr command received {}", envelope.payload());
        }
        final JsonObject jsonObject = envelope.payload();
        final UUID cotrId = UUID.fromString(jsonObject.getString("cotrId"));

        final EventStream eventStream = eventSource.getStreamById(cotrId);
        final CotrAggregate cotrAggregate = aggregateService.get(eventStream, CotrAggregate.class);
        final Stream<Object> events = cotrAggregate.archiveCotr(cotrId);
        appendEventsToStream(envelope, eventStream, events);
    }

    @Handles("progression.command.serve-defendant-cotr")
    public void serveDefendantCotr(final Envelope<ServeDefendantCotr> envelope) throws EventStreamException {
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("progression.command.serve-defendant-cotr command received {}", envelope.payload());
        }
        final ServeDefendantCotr serveDefendentCotr = envelope.payload();
        final EventStream eventStream = eventSource.getStreamById(serveDefendentCotr.getCotrId());
        final CotrAggregate cotrAggregate = aggregateService.get(eventStream, CotrAggregate.class);
        final Stream<Object> events = cotrAggregate.serveDefendantCotr(serveDefendentCotr);
        appendEventsToStream(envelope, eventStream, events);
    }

    @Handles("progression.command.request-cotr-task")
    public void requestCotrTask(final Envelope<RequestCotrTask> envelope) throws EventStreamException {
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("progression.command.request-cotr-task command received {}", envelope.payload());
        }
        final RequestCotrTask requestCotrTask = envelope.payload();
        final EventStream eventStream = eventSource.getStreamById(requestCotrTask.getCotrId());
        final CotrAggregate cotrAggregate = aggregateService.get(eventStream, CotrAggregate.class);
        final Stream<Object> events = cotrAggregate.requestCotrTask(requestCotrTask);
        appendEventsToStream(envelope, eventStream, events);
    }

    @Handles("progression.command.change-defendants-cotr")
    public void changeDefendantsCotr(final Envelope<ChangeDefendantsCotr> envelope) throws EventStreamException {
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("progression.command.change-defendants-cotr command received {}", envelope.payload());
        }
        final ChangeDefendantsCotr changeDefendantsCotr = envelope.payload();
        final EventStream eventStream = eventSource.getStreamById(changeDefendantsCotr.getCotrId());
        final CotrAggregate cotrAggregate = aggregateService.get(eventStream, CotrAggregate.class);
        final Stream<Object> events = cotrAggregate.changeDefendantsCotr(changeDefendantsCotr);
        appendEventsToStream(envelope, eventStream, events);
    }

    @Handles("progression.command.add-further-info-prosecution-cotr")
    public void addFurtherInfoForProsecutionCotr(final Envelope<AddFurtherInfoProsecutionCotr> envelope) throws EventStreamException {
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("progression.command.add-further-info-prosecution-cotr received {}", envelope.payload());
        }
        final AddFurtherInfoProsecutionCotr addFurtherInfoProsecutionCotr = envelope.payload();
        final EventStream eventStream = eventSource.getStreamById(addFurtherInfoProsecutionCotr.getCotrId());
        final CotrAggregate cotrAggregate = aggregateService.get(eventStream, CotrAggregate.class);
        final Stream<Object> events = cotrAggregate.addFurtherInfoForProsecutionCotr(addFurtherInfoProsecutionCotr);
        appendEventsToStream(envelope, eventStream, events);
    }

    @Handles("progression.command.add-further-info-defence-cotr")
    public void addFurtherInfoForDefenceCotr(final Envelope<AddFurtherInfoDefenceCotrCommand> envelope) throws EventStreamException {
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("progression.command.add-further-info-defence-cotr received {}", envelope.payload());
        }
        final AddFurtherInfoDefenceCotrCommand addFurtherInfoDefenceCotrCommand = envelope.payload();
        final EventStream eventStream = eventSource.getStreamById(addFurtherInfoDefenceCotrCommand.getCotrId());
        final CotrAggregate cotrAggregate = aggregateService.get(eventStream, CotrAggregate.class);
        final Stream<Object> events = cotrAggregate.addFurtherInfoForDefenceCotr(addFurtherInfoDefenceCotrCommand);
        appendEventsToStream(envelope, eventStream, events);
    }

    @Handles("progression.command.update-review-notes")
    public void updateReviewNotes(final Envelope<UpdateReviewNotes> envelope) throws EventStreamException {
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("progression.command.update-review-notes received {}", envelope.payload());
        }
        final UpdateReviewNotes updateReviewNotes = envelope.payload();
        final EventStream eventStream = eventSource.getStreamById(updateReviewNotes.getCotrId());
        final CotrAggregate cotrAggregate = aggregateService.get(eventStream, CotrAggregate.class);
        final Stream<Object> events = cotrAggregate.updateReviewNotes(updateReviewNotes);
        appendEventsToStream(envelope, eventStream, events);
    }
}
