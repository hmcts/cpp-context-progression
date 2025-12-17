package uk.gov.moj.cpp.progression.handler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import uk.gov.justice.core.courts.Hearing;
import uk.gov.justice.core.courts.ProsecutionCase;
import uk.gov.justice.core.courts.UpdateDefendantListingStatusV2;
import uk.gov.justice.core.courts.UpdateDefendantListingStatusV3;
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
import uk.gov.moj.cpp.progression.aggregate.GroupCaseAggregate;
import uk.gov.moj.cpp.progression.aggregate.HearingAggregate;

import javax.inject.Inject;
import javax.json.JsonValue;

import static uk.gov.justice.services.core.enveloper.Enveloper.toEnvelopeWithMetadataFrom;
import static org.apache.commons.collections.CollectionUtils.isNotEmpty;
import static java.util.Objects.nonNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

@ServiceComponent(Component.COMMAND_HANDLER)
public class UpdateDefendantListingStatusHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(UpdateDefendantListingStatusHandler.class.getName());

    @Inject
    private EventSource eventSource;

    @Inject
    private AggregateService aggregateService;

    @Handles("progression.command.update-defendant-listing-status")
    public void handle(final Envelope<UpdateDefendantListingStatusV2> updateDefendantListingStatusEnvelope) throws EventStreamException {
        LOGGER.debug("progression.command.update-defendant-listing-status {}", updateDefendantListingStatusEnvelope.payload());

        final UpdateDefendantListingStatusV2 updateDefendantListingStatus = updateDefendantListingStatusEnvelope.payload();
        final EventStream eventStream = eventSource.getStreamById(updateDefendantListingStatus.getHearing().getId());
        final HearingAggregate hearingAggregate = aggregateService.get(eventStream, HearingAggregate.class);
        Stream<Object> events = Stream.empty();
        if (isGroupProceedings(updateDefendantListingStatus.getHearing())) {
            final Optional<ProsecutionCase> groupMasterProsecutionCase = updateDefendantListingStatus.getHearing().getProsecutionCases().stream().filter(ProsecutionCase::getIsGroupMaster).findFirst();
            if (groupMasterProsecutionCase.isPresent()) {
                final UpdateDefendantListingStatusV2 updateDefendantListingStatusWithMemberCases = getUpdateDefendantListingStatusWithMemberCases(groupMasterProsecutionCase.get(), updateDefendantListingStatus);
                events = hearingAggregate.updateDefendantListingStatus(updateDefendantListingStatusWithMemberCases.getHearing(), updateDefendantListingStatusWithMemberCases.getHearingListingStatus(), updateDefendantListingStatusWithMemberCases.getNotifyNCES(), updateDefendantListingStatusWithMemberCases.getListHearingRequests());
            }
        } else {
            events = hearingAggregate.updateDefendantListingStatus(updateDefendantListingStatus.getHearing(), updateDefendantListingStatus.getHearingListingStatus(), updateDefendantListingStatus.getNotifyNCES(), updateDefendantListingStatus.getListHearingRequests());
        }
        appendEventsToStream(updateDefendantListingStatusEnvelope, eventStream, events);
    }

    private UpdateDefendantListingStatusV2 getUpdateDefendantListingStatusWithMemberCases(final ProsecutionCase groupMasterProsecutionCase, final UpdateDefendantListingStatusV2 updateDefendantListingStatus) {
        final EventStream stream = eventSource.getStreamById(groupMasterProsecutionCase.getGroupId());
        final GroupCaseAggregate groupCaseAggregate = aggregateService.get(stream, GroupCaseAggregate.class);
        final Hearing.Builder updatedHearingBuilder = Hearing.hearing().withValuesFrom(updateDefendantListingStatus.getHearing());
        final List<ProsecutionCase> prosecutionCases = new ArrayList<>(updateDefendantListingStatus.getHearing().getProsecutionCases());
        groupCaseAggregate.getMemberCases().stream().filter(caseId -> groupMasterProsecutionCase.getId().compareTo(caseId) != 0).forEach(caseId -> {
            final EventStream eventStream = eventSource.getStreamById(caseId);
            final CaseAggregate caseAggregate = aggregateService.get(eventStream, CaseAggregate.class);
            prosecutionCases.add(caseAggregate.getProsecutionCase());
        });
        updatedHearingBuilder.withProsecutionCases(prosecutionCases);
        return UpdateDefendantListingStatusV2.updateDefendantListingStatusV2()
                .withValuesFrom(updateDefendantListingStatus)
                .withHearing(updatedHearingBuilder.build()).build();
    }

    @Handles("progression.command.update-defendant-listing-status-v3")
    public void handleV3(final Envelope<UpdateDefendantListingStatusV3> updateDefendantListingStatusEnvelope) throws EventStreamException {
        LOGGER.debug("progression.command.update-defendant-listing-status-v3 {}", updateDefendantListingStatusEnvelope.payload());

        final UpdateDefendantListingStatusV3 updateDefendantListingStatus = updateDefendantListingStatusEnvelope.payload();
        final EventStream eventStream = eventSource.getStreamById(updateDefendantListingStatus.getHearing().getId());
        final HearingAggregate hearingAggregate = aggregateService.get(eventStream, HearingAggregate.class);
        final Stream<Object> events = hearingAggregate.updateDefendantListingStatusV3                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                   (updateDefendantListingStatus.getHearing(), updateDefendantListingStatus.getHearingListingStatus(),
                updateDefendantListingStatus.getNotifyNCES(), updateDefendantListingStatus.getListNextHearings());
        appendEventsToStream(updateDefendantListingStatusEnvelope, eventStream, events);
    }

    private void appendEventsToStream(final Envelope<?> envelope, final EventStream eventStream, final Stream<Object> events) throws EventStreamException {
        final JsonEnvelope jsonEnvelope = JsonEnvelope.envelopeFrom(envelope.metadata(), JsonValue.NULL);
        eventStream.append(events.map(toEnvelopeWithMetadataFrom(jsonEnvelope)));
    }

    private boolean isGroupProceedings(final Hearing hearing) {
        return nonNull(hearing.getIsGroupProceedings()) && hearing.getIsGroupProceedings() && isNotEmpty(hearing.getProsecutionCases());
    }
}
