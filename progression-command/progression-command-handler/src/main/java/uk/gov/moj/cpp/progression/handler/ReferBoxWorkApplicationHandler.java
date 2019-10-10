package uk.gov.moj.cpp.progression.handler;

import static java.util.Optional.ofNullable;

import uk.gov.justice.core.courts.ApplicationStatus;
import uk.gov.justice.core.courts.CourtApplication;
import uk.gov.justice.core.courts.ReferBoxworkApplication;
import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
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

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.inject.Inject;
import javax.json.JsonValue;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@SuppressWarnings({"squid:S00112", "squid:S2629"})
@ServiceComponent(Component.COMMAND_HANDLER)
public class ReferBoxWorkApplicationHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(ReferBoxWorkApplicationHandler.class.getName());

    @Inject
    private EventSource eventSource;

    @Inject
    private AggregateService aggregateService;

    @Inject
    private Enveloper enveloper;

    @Inject
    private JsonObjectToObjectConverter jsonObjectToObjectConverter;

    @Handles("progression.command.refer-boxwork-application")
    public void handle(final JsonEnvelope envelope) throws EventStreamException {
        LOGGER.debug("progression.command.refer-boxwork-application {}", envelope.payloadAsJsonObject());
        final ReferBoxworkApplication referBoxworkApplication = jsonObjectToObjectConverter.convert(envelope.payloadAsJsonObject(), ReferBoxworkApplication.class);
        final List<CourtApplication> courtApplications = ofNullable(referBoxworkApplication.getHearingRequest().getCourtApplications().stream().map(app -> getCourtApplication(app)).collect(Collectors.toList())).orElse(new ArrayList<>());
        referBoxworkApplication.getHearingRequest().getCourtApplications().clear();
        referBoxworkApplication.getHearingRequest().getCourtApplications().addAll(courtApplications);
        for (CourtApplication courtApplication : courtApplications) {
            processEventStream(referBoxworkApplication, envelope, courtApplication.getId());
        }

    }

    private void processEventStream(final ReferBoxworkApplication referBoxworkApplication, final JsonEnvelope envelope, final UUID courtApplicationId) throws EventStreamException {
        final EventStream eventStream = eventSource.getStreamById(courtApplicationId);
        final ApplicationAggregate applicationAggregate = aggregateService.get(eventStream, ApplicationAggregate.class);
        final Stream<Object> events = applicationAggregate.referBoxWorkApplication(referBoxworkApplication.getHearingRequest());
        appendEventsToStream(envelope, eventStream, events);
    }


    private void appendEventsToStream(final Envelope<?> envelope, final EventStream eventStream, final Stream<Object> events) throws EventStreamException {
        final JsonEnvelope jsonEnvelope = JsonEnvelope.envelopeFrom(envelope.metadata(), JsonValue.NULL);
        eventStream.append(events.map(enveloper.withMetadataFrom(jsonEnvelope)));
    }

    private CourtApplication getCourtApplication(final CourtApplication courtApplication) {
        return new CourtApplication(courtApplication.getApplicant(),
                courtApplication.getApplicationDecisionSoughtByDate(),
                courtApplication.getApplicationOutcome(),
                courtApplication.getApplicationParticulars(),
                courtApplication.getApplicationReceivedDate(),
                courtApplication.getApplicationReference(),
                ApplicationStatus.IN_PROGRESS,
                courtApplication.getBreachedOrder(),
                courtApplication.getBreachedOrderDate(),
                courtApplication.getCourtApplicationPayment(),
                courtApplication.getDueDate(),
                courtApplication.getId(),
                courtApplication.getJudicialResults(),
                courtApplication.getLinkedCaseId(),
                courtApplication.getOrderingCourt(),
                courtApplication.getOutOfTimeReasons(),
                courtApplication.getParentApplicationId(),
                courtApplication.getRemovalReason(),
                courtApplication.getRespondents(),
                courtApplication.getType());
    }

}
