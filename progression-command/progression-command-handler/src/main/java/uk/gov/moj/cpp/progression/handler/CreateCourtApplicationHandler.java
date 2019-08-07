package uk.gov.moj.cpp.progression.handler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.gov.justice.core.courts.AddCourtApplicationToCase;
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
import uk.gov.moj.cpp.progression.aggregate.ApplicationAggregate;
import uk.gov.moj.cpp.progression.aggregate.CaseAggregate;
import uk.gov.justice.core.courts.CreateCourtApplication;

import javax.inject.Inject;
import javax.json.JsonValue;
import java.util.stream.Stream;

@SuppressWarnings({"squid:S00112", "squid:S2629"})
@ServiceComponent(Component.COMMAND_HANDLER)
public class CreateCourtApplicationHandler {


    private static final Logger LOGGER =
            LoggerFactory.getLogger(CreateCourtApplicationHandler.class.getName());

    @Inject
    private EventSource eventSource;

    @Inject
    private AggregateService aggregateService;

    @Inject
    private Enveloper enveloper;

    @Handles("progression.command.create-court-application")
    public void handle(final Envelope<CreateCourtApplication> createCourtApplicationEnvelope) throws EventStreamException {
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("progression.command.create-court-application {}", createCourtApplicationEnvelope.payload());
        }

        final CreateCourtApplication courtApplication = createCourtApplicationEnvelope.payload();
        EventStream eventStream;
        if (null != courtApplication.getApplication().getLinkedCaseId()) {
            eventStream = eventSource.getStreamById(courtApplication.getApplication().getLinkedCaseId());
            final CaseAggregate caseAggregate = aggregateService.get(eventStream, CaseAggregate.class);
            final Stream<Object> events = caseAggregate.createCourtApplication(courtApplication.getApplication());
            appendEventsToStream(createCourtApplicationEnvelope, eventStream, events);
        } else {
            eventStream = eventSource.getStreamById(courtApplication.getApplication().getId());
            final ApplicationAggregate applicationAggregate = aggregateService.get(eventStream, ApplicationAggregate.class);
            final Stream<Object> events = applicationAggregate.createCourtApplication(courtApplication.getApplication());
            appendEventsToStream(createCourtApplicationEnvelope, eventStream, events);
        }
    }

    @Handles("progression.command.add-court-application-to-case")
    public void courtApplicationAddedToCase(final Envelope<AddCourtApplicationToCase> addCourtApplicationToCaseEnvelope) throws EventStreamException {
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("progression.command.add-court-application-to-case {}", addCourtApplicationToCaseEnvelope.payload());
        }

        final AddCourtApplicationToCase addCourtApplicationToCase = addCourtApplicationToCaseEnvelope.payload();
        final EventStream eventStream = eventSource.getStreamById(addCourtApplicationToCase.getCourtApplication().getId());
        final ApplicationAggregate applicationAggregate = aggregateService.get(eventStream, ApplicationAggregate.class);
        final Stream<Object> events = applicationAggregate.addApplicationToCase(addCourtApplicationToCase.getCourtApplication());
        appendEventsToStream(addCourtApplicationToCaseEnvelope, eventStream, events);

    }

    private void appendEventsToStream(final Envelope<?> envelope, final EventStream eventStream, final Stream<Object> events) throws EventStreamException {
        final JsonEnvelope jsonEnvelope = JsonEnvelope.envelopeFrom(envelope.metadata(), JsonValue.NULL);
        eventStream.append(
                events
                        .map(enveloper.withMetadataFrom(jsonEnvelope)));
    }

}
