package uk.gov.moj.cpp.progression.handler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.gov.justice.core.courts.UpdateDefendantCustodialInformationForApplication;
import uk.gov.justice.services.core.aggregate.AggregateService;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.eventsourcing.source.core.EventSource;
import uk.gov.justice.services.eventsourcing.source.core.EventStream;
import uk.gov.justice.services.eventsourcing.source.core.exception.EventStreamException;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.moj.cpp.progression.aggregate.ApplicationAggregate;

import javax.inject.Inject;

import java.util.stream.Stream;

import static uk.gov.justice.services.core.annotation.Component.COMMAND_HANDLER;

@ServiceComponent(COMMAND_HANDLER)
public class ApplicationCustodialInformationHandler {

    @Inject
    private EventSource eventSource;

    @Inject
    private AggregateService aggregateService;

    private static final Logger LOGGER = LoggerFactory.getLogger(ApplicationCustodialInformationHandler.class);



    @Handles("progression.command.update-defendant-custodial-information-for-application")
    public void handle(final Envelope<UpdateDefendantCustodialInformationForApplication> envelope) throws EventStreamException {
        if (LOGGER.isInfoEnabled()) {
            LOGGER.info("Received command progression.command.update-defendant-custodial-information-for-application");
        }
        final UpdateDefendantCustodialInformationForApplication updateDefendantCustodialInformationForApplication = envelope.payload();
        final EventStream eventStream = eventSource.getStreamById(updateDefendantCustodialInformationForApplication.getApplicationId());
        final ApplicationAggregate  applicationAggregate = aggregateService.get(eventStream, ApplicationAggregate.class);
        final Stream<Object> events = applicationAggregate.updateCustodialInfomrationForApplicatioNSubject(
                updateDefendantCustodialInformationForApplication.getDefendant(), updateDefendantCustodialInformationForApplication.getApplicationId());

        eventStream.append(events.map(Enveloper.toEnvelopeWithMetadataFrom(envelope)));


    }
}
