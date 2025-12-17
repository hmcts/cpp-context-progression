package uk.gov.moj.cpp.progression.handler;

import static java.util.Objects.nonNull;
import static java.util.Optional.ofNullable;
import static java.util.UUID.randomUUID;
import static uk.gov.justice.services.core.annotation.Component.COMMAND_HANDLER;
import static uk.gov.justice.services.core.enveloper.Enveloper.toEnvelopeWithMetadataFrom;


import java.util.Collections;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.inject.Inject;
import javax.json.JsonValue;
import uk.gov.justice.core.courts.CourtHearingRequest;
import uk.gov.justice.core.courts.Defendant;
import uk.gov.justice.core.courts.HearingListingNeeds;
import uk.gov.justice.core.courts.ListDefendantRequest;
import uk.gov.justice.core.courts.ListNewHearing;
import uk.gov.justice.core.courts.ProsecutionCase;
import uk.gov.justice.progression.courts.RequestRelatedHearingForAdhocHearing;
import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.core.aggregate.AggregateService;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.eventsourcing.source.core.EventSource;
import uk.gov.justice.services.eventsourcing.source.core.EventStream;
import uk.gov.justice.services.eventsourcing.source.core.exception.EventStreamException;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.progression.aggregate.CaseAggregate;
import uk.gov.moj.cpp.progression.aggregate.HearingAggregate;
import uk.gov.moj.cpp.progression.service.ProsecutionCaseQueryService;

@ServiceComponent(COMMAND_HANDLER)
public class ListNewHearingHandler {

    @Inject
    private EventSource eventSource;

    @Inject
    private AggregateService aggregateService;

    @Inject
    private ProsecutionCaseQueryService prosecutionCaseQueryService;

    @Inject
    private JsonObjectToObjectConverter jsonObjectToObjectConverter;

    @Handles("progression.command.list-new-hearing")
    public void handle(final Envelope<ListNewHearing> listNewHearingEnvelope) throws EventStreamException {
        final JsonEnvelope jsonEnvelope = JsonEnvelope.envelopeFrom(listNewHearingEnvelope.metadata(), JsonValue.NULL);

        final ListNewHearing listNewHearing = listNewHearingEnvelope.payload();

        if (! hasValidDefendants(jsonEnvelope, listNewHearing)){
            return;
        }

        final boolean isRelatedHearing = nonNull(listNewHearing.getListNewHearing().getId());

        final Stream<Object> events;
        if(isRelatedHearing){
            final EventStream eventStreamForCase = eventSource.getStreamById(listNewHearing.getListNewHearing().getListDefendantRequests().get(0).getProsecutionCaseId());
            final CaseAggregate caseAggregate = aggregateService.get(eventStreamForCase, CaseAggregate.class);
            events = caseAggregate.extendCaseToExistingHearingForAdhocHearing(listNewHearing.getListNewHearing(), listNewHearing.getSendNotificationToParties());
            appendEventsToStream(listNewHearingEnvelope, eventStreamForCase, events);

        } else {
            final UUID hearingId =  randomUUID();

            final EventStream eventStream = eventSource.getStreamById(hearingId);
            final HearingAggregate hearingAggregate = aggregateService.get(eventStream, HearingAggregate.class);

            events = hearingAggregate.listNewHearing(hearingId, listNewHearing.getListNewHearing(), listNewHearing.getSendNotificationToParties());
            appendEventsToStream(listNewHearingEnvelope, eventStream, events);
        }

    }

    @Handles("progression.command.request-related-hearing-for-adhoc-hearing")
    public void handleRequestRelatedHearingForAdhocHearing(final Envelope<RequestRelatedHearingForAdhocHearing> requestRelatedHearingForAdhocHearingEnvelope) throws EventStreamException {
        final Stream<Object> events;
        final EventStream eventStream = eventSource.getStreamById(requestRelatedHearingForAdhocHearingEnvelope.payload().getListNewHearing().getId());
        final HearingAggregate hearingAggregate = aggregateService.get(eventStream, HearingAggregate.class);

        final CourtHearingRequest listNewHearing = requestRelatedHearingForAdhocHearingEnvelope.payload().getListNewHearing();

        final HearingListingNeeds existingHearingListingNeeds = HearingListingNeeds.hearingListingNeeds()
                .withId(listNewHearing.getId())
                .withProsecutionCases(Collections.singletonList(requestRelatedHearingForAdhocHearingEnvelope.payload().getProsecutionCase()))
                .withType(listNewHearing.getHearingType())
                .withJurisdictionType(listNewHearing.getJurisdictionType())
                .withCourtCentre(listNewHearing.getCourtCentre())
                .withEstimatedMinutes(listNewHearing.getEstimatedMinutes())
                .withEarliestStartDateTime(listNewHearing.getEarliestStartDateTime())
                .build();

        events = hearingAggregate.requestRelatedHearingForAdhocHearing(existingHearingListingNeeds, requestRelatedHearingForAdhocHearingEnvelope.payload().getSendNotificationToParties());

        appendEventsToStream(requestRelatedHearingForAdhocHearingEnvelope, eventStream, events);
    }

    private boolean hasValidDefendants(final JsonEnvelope jsonEnvelope, final ListNewHearing listNewHearing) {
        final Set<UUID> caseIds = listNewHearing.getListNewHearing().getListDefendantRequests().stream()
                .map(ListDefendantRequest::getProsecutionCaseId)
                .collect(Collectors.toSet());

        final Set<UUID> defendants = caseIds.stream().map(caseId -> prosecutionCaseQueryService.getProsecutionCase(jsonEnvelope, caseId.toString()))
                .filter(Optional::isPresent)
                .map(Optional::get)
                .map(jsonObject -> jsonObjectToObjectConverter.convert(jsonObject.getJsonObject("prosecutionCase"), ProsecutionCase.class))
                .flatMap(c -> c.getDefendants().stream())
                .map(Defendant::getId)
                .collect(Collectors.toSet());

        return listNewHearing.getListNewHearing().getListDefendantRequests().stream()
                .map(listDefendantRequest -> ofNullable(listDefendantRequest.getDefendantId()).orElseGet(() -> listDefendantRequest.getReferralReason().getDefendantId()))
                .anyMatch(defendants::contains) ;
    }

    private void appendEventsToStream(final Envelope<?> envelope, final EventStream eventStream, final Stream<Object> events) throws EventStreamException {
        final JsonEnvelope jsonEnvelope = JsonEnvelope.envelopeFrom(envelope.metadata(), JsonValue.NULL);
        eventStream.append(events.map(toEnvelopeWithMetadataFrom(jsonEnvelope)));
    }
}
