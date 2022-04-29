package uk.gov.moj.cpp.progression.handler;

import static uk.gov.justice.services.core.annotation.Component.COMMAND_HANDLER;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.gov.justice.core.courts.AddOrStoreDefendantsAndListingHearingRequests;
import uk.gov.justice.services.core.aggregate.AggregateService;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.eventsourcing.source.core.EventSource;
import uk.gov.justice.services.eventsourcing.source.core.EventStream;
import uk.gov.justice.services.eventsourcing.source.core.exception.EventStreamException;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.moj.cpp.progression.aggregate.CaseAggregate;
import javax.inject.Inject;
import java.util.stream.Stream;

@ServiceComponent(COMMAND_HANDLER)
public class AddOrStoreDefendantsAndListingHearingRequestsCommandHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(AddOrStoreDefendantsAndListingHearingRequestsCommandHandler.class);

    @Inject
    private EventSource eventSource;

    @Inject
    private AggregateService aggregateService;

    @Handles("progression.command.add-or-store-defendants-and-listing-hearing-requests")
    public void addOrStoreDefendantsAndListingHearingRequests(final Envelope<AddOrStoreDefendantsAndListingHearingRequests> addOrStoreDefendantsAndListingHearingRequestsEnvelope) throws EventStreamException {

        final AddOrStoreDefendantsAndListingHearingRequests addOrStoreDefendantsAndListingHearingRequests = addOrStoreDefendantsAndListingHearingRequestsEnvelope.payload();

        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("'progression.command.add-or-store-defendants-and-listing-hearing-requests' received with payload {}", addOrStoreDefendantsAndListingHearingRequests);
        }

        final EventStream eventStream = eventSource.getStreamById(addOrStoreDefendantsAndListingHearingRequests.getDefendants().get(0).getProsecutionCaseId());
        final CaseAggregate caseAggregate = aggregateService.get(eventStream, CaseAggregate.class);

        final Stream<Object> events = caseAggregate.addOrStoreDefendantsAndListingHearingRequests(addOrStoreDefendantsAndListingHearingRequests.getDefendants(), addOrStoreDefendantsAndListingHearingRequests.getListHearingRequests());

        eventStream.append(events.map(Enveloper.toEnvelopeWithMetadataFrom(addOrStoreDefendantsAndListingHearingRequestsEnvelope)));

    }

}