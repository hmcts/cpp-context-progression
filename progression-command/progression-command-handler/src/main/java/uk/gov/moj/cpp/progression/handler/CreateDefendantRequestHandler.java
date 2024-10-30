package uk.gov.moj.cpp.progression.handler;

import uk.gov.justice.core.courts.CreateDefendantRequest;
import uk.gov.justice.services.core.aggregate.AggregateService;
import uk.gov.justice.services.core.annotation.Component;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.eventsourcing.source.core.EventSource;
import uk.gov.justice.services.messaging.Envelope;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ServiceComponent(Component.COMMAND_HANDLER)
public class CreateDefendantRequestHandler {

    @Inject
    private EventSource eventSource;

    @Inject
    private AggregateService aggregateService;

    @Inject
    private Enveloper enveloper;

    private static final Logger LOGGER =
            LoggerFactory.getLogger(CreateDefendantRequestHandler.class.getName());


    @Handles("progression.command.create-defendant-request")
    public void handle(final Envelope<CreateDefendantRequest> createDefendantRequestEnvelope) {
        LOGGER.debug("progression.command.create-defendant-request {}", createDefendantRequestEnvelope);
    }


}
