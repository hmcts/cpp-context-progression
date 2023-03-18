package uk.gov.moj.cpp.progression.handler;

import static java.lang.String.format;
import static javax.json.JsonValue.NULL;
import static uk.gov.justice.services.core.enveloper.Enveloper.toEnvelopeWithMetadataFrom;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;

import uk.gov.justice.core.courts.LaaDefendantProceedingConcludedChanged;
import uk.gov.justice.services.common.converter.jackson.ObjectMapperProducer;
import uk.gov.justice.services.core.aggregate.AggregateService;
import uk.gov.justice.services.core.annotation.Component;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.eventsourcing.source.core.EventSource;
import uk.gov.justice.services.eventsourcing.source.core.EventStream;
import uk.gov.justice.services.eventsourcing.source.core.exception.EventStreamException;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.progression.aggregate.CaseAggregate;
import uk.gov.moj.cpp.progression.command.handler.courts.ResendLaaOutcomeConcluded;

import java.io.IOException;
import java.util.UUID;
import java.util.stream.Stream;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@SuppressWarnings("squid:S1160")
@ServiceComponent(Component.COMMAND_HANDLER)
public class ResendLaaCaseOutcomeHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(ResendLaaCaseOutcomeHandler.class.getName());

    @Inject
    private EventSource eventSource;

    @Inject
    private AggregateService aggregateService;


    @Handles("progression.command.handler.resend-laa-outcome-concluded")
    public void handle(final Envelope<ResendLaaOutcomeConcluded> resendLaaOutcomeConcludedEnvelope) throws EventStreamException, IOException {
        LOGGER.debug("progression.command.handler.resend-laa-outcome-concluded {}", resendLaaOutcomeConcludedEnvelope);


        final ResendLaaOutcomeConcluded resendLaaOutcomeConcluded = resendLaaOutcomeConcludedEnvelope.payload();

        final UUID caseId = resendLaaOutcomeConcluded.getCaseId();
        EventStream eventStream = eventSource.getStreamById(caseId);
        final Stream<JsonEnvelope> jsonEnvelopeStream = eventStream.read();
        final JsonEnvelope jsonEnvelope = jsonEnvelopeStream.filter(e -> "progression.event.laa-defendant-proceeding-concluded-changed".equals(e.metadata().name())).findFirst().orElse(null);

        eventStream = eventSource.getStreamById(caseId);
        final CaseAggregate caseAggregate = aggregateService.get(eventStream, CaseAggregate.class);

        LaaDefendantProceedingConcludedChanged laaDefendantProceedingConcludedChanged = null;
        if (jsonEnvelope != null) {
            laaDefendantProceedingConcludedChanged = new ObjectMapperProducer().objectMapper().readValue(jsonEnvelope.payloadAsJsonObject().toString(), LaaDefendantProceedingConcludedChanged.class);
        } else {
            throw new IllegalArgumentException(format("event progression.event.laa-defendant-proceeding-concluded-changed not found for caseId : %s", caseId));
        }

        final Stream<Object> events = caseAggregate.resendLaaOutcomeConcluded(laaDefendantProceedingConcludedChanged);


        appendEventsToStream(resendLaaOutcomeConcludedEnvelope, eventStream, events);

    }


    private void appendEventsToStream(final Envelope<?> envelope, final EventStream eventStream, final Stream<Object> events) throws EventStreamException {
        final JsonEnvelope jsonEnvelope = envelopeFrom(envelope.metadata(), NULL);
        final Stream<JsonEnvelope> envelopeStream = events.map(toEnvelopeWithMetadataFrom(jsonEnvelope));
        eventStream.append(envelopeStream);
    }

}

