package uk.gov.moj.cpp.progression.handler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.core.aggregate.AggregateService;
import uk.gov.justice.services.core.annotation.Component;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.eventsourcing.source.core.EventSource;
import uk.gov.justice.services.eventsourcing.source.core.EventStream;
import uk.gov.justice.services.eventsourcing.source.core.exception.EventStreamException;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.progression.aggregate.MaterialAggregate;
import uk.gov.moj.cpp.progression.command.enforcement.EnforceFinancialImpositionAcknowledgement;

import javax.inject.Inject;
import java.util.UUID;
import java.util.stream.Stream;

@ServiceComponent(Component.COMMAND_HANDLER)
public class StagingEnforcementResponseHandler extends AbstractCommandHandler {

    private static final Logger LOGGER =
            LoggerFactory.getLogger(StagingEnforcementResponseHandler.class.getName());

    @Inject
    private EventSource eventSource;

    @Inject
    private AggregateService aggregateService;

    @Inject
    private JsonObjectToObjectConverter jsonObjectToObjectConverter;

    @Handles("progression.command.apply-enforcement-acknowledgement")
    public void applyEnforcementAcknowledgement(final JsonEnvelope envelope) throws EventStreamException {
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("progression.command.apply-enforcement-acknowledgement event received {}", envelope.toObfuscatedDebugString());
        }

        final EnforceFinancialImpositionAcknowledgement command = this.jsonObjectToObjectConverter.convert(envelope.payloadAsJsonObject(), EnforceFinancialImpositionAcknowledgement.class);
        final UUID materialId = command.getMaterialId();

        final EventStream eventStream = eventSource.getStreamById(materialId);
        final MaterialAggregate materialAggregate = aggregateService.get(eventStream, MaterialAggregate.class);
        final Stream<Object> events = materialAggregate.saveAccountNumber(command.getMaterialId(), command.getRequestId(), command.getAcknowledgement().getAccountNumber());
        appendEventsToStream(envelope, eventStream, events);
    }

    @Handles("progression.command.enforcement-acknowledgement-error")
    public void enforcementAcknowledgmentError(final JsonEnvelope envelope) throws EventStreamException {
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("progression.command.enforcement-acknowledgement-error event received {}", envelope.toObfuscatedDebugString());
        }

        final EnforceFinancialImpositionAcknowledgement command = this.jsonObjectToObjectConverter.convert(envelope.payloadAsJsonObject(), EnforceFinancialImpositionAcknowledgement.class);
        final UUID materialId = command.getMaterialId();

        final EventStream eventStream = eventSource.getStreamById(materialId);
        final MaterialAggregate materialAggregate = aggregateService.get(eventStream, MaterialAggregate.class);
        final Stream<Object> events = materialAggregate.recordEnforcementError(command.getRequestId(), command.getAcknowledgement().getErrorCode(), command.getAcknowledgement().getErrorMessage());
        appendEventsToStream(envelope, eventStream, events);
    }

}
