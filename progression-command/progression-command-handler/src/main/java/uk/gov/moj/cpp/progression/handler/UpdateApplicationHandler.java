package uk.gov.moj.cpp.progression.handler;

import static uk.gov.justice.services.core.annotation.Component.COMMAND_HANDLER;

import uk.gov.justice.core.courts.UpdateDefendantAddressOnApplication;
import uk.gov.justice.services.common.converter.ObjectToJsonObjectConverter;
import uk.gov.justice.services.core.aggregate.AggregateService;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.eventsourcing.source.core.EventSource;
import uk.gov.justice.services.eventsourcing.source.core.EventStream;
import uk.gov.justice.services.eventsourcing.source.core.exception.EventStreamException;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.moj.cpp.progression.aggregate.ApplicationAggregate;

import java.util.stream.Stream;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ServiceComponent(COMMAND_HANDLER)
public class UpdateApplicationHandler {
    @Inject
    private EventSource eventSource;

    @Inject
    private AggregateService aggregateService;
    @Inject
    private ObjectToJsonObjectConverter objectToJsonObjectConverter;

    private static final Logger LOGGER = LoggerFactory.getLogger(ApplicationCustodialInformationHandler.class);

    @Handles("progression.command.update-defendant-address-on-application")
    public void handle(final Envelope<UpdateDefendantAddressOnApplication> envelope) throws EventStreamException {
        if (LOGGER.isInfoEnabled()) {
            LOGGER.info("Received command progression.command.update-defendant-address-on-application, applicationId : {}", envelope.payload().getApplicationId());
        }
        final UpdateDefendantAddressOnApplication updateDefendantAddressOnApplicationPayload = envelope.payload();
        final EventStream eventStream = eventSource.getStreamById(updateDefendantAddressOnApplicationPayload.getApplicationId());
        final ApplicationAggregate applicationAggregate = aggregateService.get(eventStream, ApplicationAggregate.class);
        final Stream<Object> events = applicationAggregate.updateDefendantAddressOnApplication(
                updateDefendantAddressOnApplicationPayload.getApplicationId(), updateDefendantAddressOnApplicationPayload.getDefendant(),
                updateDefendantAddressOnApplicationPayload.getHearingIds());
        eventStream.append(events.map(Enveloper.toEnvelopeWithMetadataFrom(envelope)));
    }
}
