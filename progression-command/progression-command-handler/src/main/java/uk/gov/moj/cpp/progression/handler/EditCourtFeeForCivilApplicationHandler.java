package uk.gov.moj.cpp.progression.handler;

import uk.gov.justice.core.courts.CourtApplicationPayment;
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
import uk.gov.moj.cpp.progression.command.EditCourtFeeForCivilApplication;

import java.util.stream.Stream;

import javax.inject.Inject;
import javax.json.JsonValue;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ServiceComponent(Component.COMMAND_HANDLER)
public class EditCourtFeeForCivilApplicationHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(EditCourtFeeForCivilApplicationHandler.class.getName());

    private static final String PROGRESSION_COMMAND_HANDLER_CIVIL_APPLICATION_FEE = "progression.command.edit-court-fee-for-civil-application";

    @Inject
    private EventSource eventSource;

    @Inject
    private AggregateService aggregateService;

    @Inject
    private Enveloper enveloper;

    @Handles(PROGRESSION_COMMAND_HANDLER_CIVIL_APPLICATION_FEE)
    public void handleCivilFee(final Envelope<EditCourtFeeForCivilApplication> editCourtFeeForCivilApplicationEnvelope) throws EventStreamException {
        LOGGER.debug("progression.command.edit-court-fee-for-civil-application {} ", editCourtFeeForCivilApplicationEnvelope.payload());

        final EditCourtFeeForCivilApplication editCourtFeeForCivilApplication = editCourtFeeForCivilApplicationEnvelope.payload();
        final EventStream eventStream = eventSource.getStreamById(editCourtFeeForCivilApplication.getApplicationId());
        final ApplicationAggregate applicationAggregate = aggregateService.get(eventStream, ApplicationAggregate.class);
        final Stream<Object> events = applicationAggregate.handleEditCourtFeeForCivilApplication(editCourtFeeForCivilApplication.getApplicationId(),
                getCourtApplicationPayment(editCourtFeeForCivilApplication.getCourtApplicationPayment()));

        appendEventsToStream(editCourtFeeForCivilApplicationEnvelope, eventStream, events);
    }

    private CourtApplicationPayment getCourtApplicationPayment(uk.gov.moj.cpp.progression.command.CourtApplicationPayment courtApplicationPayment){
        final CourtApplicationPayment.Builder builder = CourtApplicationPayment.courtApplicationPayment();
        builder.withPaymentReference(courtApplicationPayment.getPaymentReference());
        builder.withFeeStatus(courtApplicationPayment.getFeeStatus());
        builder.withContestedFeeStatus(courtApplicationPayment.getContestedFeeStatus());
        builder.withContestedPaymentReference(courtApplicationPayment.getContestedPaymentReference());
        return builder.build();
    }

    private void appendEventsToStream(final Envelope<?> envelope, final EventStream eventStream, final Stream<Object> events) throws EventStreamException {
        final JsonEnvelope jsonEnvelope = JsonEnvelope.envelopeFrom(envelope.metadata(), JsonValue.NULL);
        eventStream.append(
                events.map(Enveloper.toEnvelopeWithMetadataFrom(jsonEnvelope)));
    }
}
