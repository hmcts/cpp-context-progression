package uk.gov.moj.cpp.progression.handler;


import static uk.gov.justice.progression.event.OpaPressListNoticeRequested.opaPressListNoticeRequested;
import static uk.gov.justice.progression.event.OpaPublicListNoticeRequested.opaPublicListNoticeRequested;
import static uk.gov.justice.progression.event.OpaResultListNoticeRequested.opaResultListNoticeRequested;
import static uk.gov.justice.services.core.annotation.Component.COMMAND_HANDLER;
import static uk.gov.justice.services.core.enveloper.Enveloper.toEnvelopeWithMetadataFrom;

import uk.gov.justice.core.courts.OnlinePleasAllocation;
import uk.gov.justice.core.courts.ProsecutionCase;
import uk.gov.justice.progression.courts.AddOnlinePleaAllocation;
import uk.gov.justice.progression.courts.OpaNoticeSent;
import uk.gov.justice.progression.courts.UpdateOnlinePleaAllocation;
import uk.gov.justice.progression.event.OpaPressListNoticeRequested;
import uk.gov.justice.progression.event.OpaPublicListNoticeRequested;
import uk.gov.justice.progression.event.OpaResultListNoticeRequested;
import uk.gov.justice.services.core.aggregate.AggregateService;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.eventsourcing.source.core.EventSource;
import uk.gov.justice.services.eventsourcing.source.core.EventStream;
import uk.gov.justice.services.eventsourcing.source.core.exception.EventStreamException;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.progression.GenerateOpaNotice;
import uk.gov.moj.cpp.progression.aggregate.CaseAggregate;
import uk.gov.moj.cpp.progression.aggregate.HearingAggregate;
import uk.gov.moj.cpp.progression.plea.json.schemas.PleasAllocationDetails;

import java.time.LocalDate;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Stream;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ServiceComponent(COMMAND_HANDLER)
public class OnlinePleasAllocationHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(OnlinePleasAllocationHandler.class);

    private static final String FIRST_HEARING = "First hearing";

    private static final String TRIGGER_DATE = "triggerDate";

    private static final String PROGRESSION_COMMAND_ALLOCATION_PLEAS_ADDED = "progression.command.add-online-plea-allocation";
    private static final String PROGRESSION_COMMAND_ALLOCATION_PLEAS_UPDATED = "progression.command.update-online-plea-allocation";
    private static final String PROGRESSION_COMMAND_REQUEST_OPA_PUBLIC_LIST_NOTICE = "progression.command.request-opa-public-list-notice";
    private static final String PROGRESSION_COMMAND_REQUEST_OPA_PRESS_LIST_NOTICE = "progression.command.request-opa-press-list-notice";
    private static final String PROGRESSION_COMMAND_REQUEST_OPA_RESULT_LIST_NOTICE = "progression.command.request-opa-result-list-notice";
    private static final String PROGRESSION_COMMAND_GENERATE_OPA_PUBLIC_LIST_NOTICE = "progression.command.generate-opa-public-list-notice";
    private static final String PROGRESSION_COMMAND_GENERATE_OPA_PRESS_LIST_NOTICE = "progression.command.generate-opa-press-list-notice";
    private static final String PROGRESSION_COMMAND_GENERATE_OPA_RESULT_LIST_NOTICE = "progression.command.generate-opa-result-list-notice";
    private static final String PROGRESSION_COMMAND_OPA_PUBLIC_LIST_NOTICE_SENT = "progression.command.opa-public-list-notice-sent";
    private static final String PROGRESSION_COMMAND_OPA_PRESS_LIST_NOTICE_SENT = "progression.command.opa-press-list-notice-sent";
    private static final String PROGRESSION_COMMAND_OPA_RESULT_LIST_NOTICE_SENT = "progression.command.opa-result-list-notice-sent";

    @Inject
    private EventSource eventSource;

    @Inject
    private AggregateService aggregateService;

    @Handles(PROGRESSION_COMMAND_ALLOCATION_PLEAS_ADDED)
    public void handleAddOnlinePleasAllocation(final Envelope<AddOnlinePleaAllocation> envelope) throws EventStreamException {
        final PleasAllocationDetails pleasAllocation = envelope.payload().getPleasAllocation();

        LOGGER.info("progression.command.add-online-plea-allocation with caseId={} for defendantId={}", pleasAllocation.getCaseId(), pleasAllocation.getDefendantId());

        final EventStream eventStream = eventSource.getStreamById(pleasAllocation.getCaseId());
        final CaseAggregate caseAggregate = aggregateService.get(eventStream, CaseAggregate.class);

        final Set<UUID> hearingIds = caseAggregate.getLinkedHearingIds();
        final Optional<UUID> firstHearingId = getFirstHearing(hearingIds);

        if (firstHearingId.isPresent()) {
            final Stream<Object> events = caseAggregate.addOnlinePleaAllocation(pleasAllocation, firstHearingId.get());

            eventStream.append(events.map(toEnvelopeWithMetadataFrom(envelope)));
        }
    }

    @Handles(PROGRESSION_COMMAND_ALLOCATION_PLEAS_UPDATED)
    public void handleUpdateOnlinePleasAllocation(final Envelope<UpdateOnlinePleaAllocation> envelope) throws EventStreamException {
        final PleasAllocationDetails pleasAllocation = envelope.payload().getPleasAllocation();

        LOGGER.info("progression.command.update-online-plea-allocation with caseId={} for defendantId={}", pleasAllocation.getCaseId(), pleasAllocation.getDefendantId());

        final EventStream eventStream = eventSource.getStreamById(pleasAllocation.getCaseId());
        final CaseAggregate caseAggregate = aggregateService.get(eventStream, CaseAggregate.class);

        final Optional<Stream<Object>> events = caseAggregate.updateOnlinePleaAllocation(pleasAllocation);

        if (events.isPresent()) {
            eventStream.append(events.get().map(toEnvelopeWithMetadataFrom(envelope)));
        }
    }

    @Handles(PROGRESSION_COMMAND_REQUEST_OPA_PUBLIC_LIST_NOTICE)
    public void handleRequestOpaPublicListNotice(final JsonEnvelope envelope) throws EventStreamException {
        final String triggerDate = envelope.payloadAsJsonObject().getString(TRIGGER_DATE);

        LOGGER.info("progression.command.request-opa-public-list-notice at {} ", triggerDate);

        final EventStream eventStream = eventSource.getStreamById(UUID.randomUUID());

        final OpaPublicListNoticeRequested event = opaPublicListNoticeRequested().withTriggerDate(LocalDate.parse(triggerDate)).build();

        eventStream.append(Stream.of(event).map(toEnvelopeWithMetadataFrom(envelope)));
    }

    @Handles(PROGRESSION_COMMAND_REQUEST_OPA_PRESS_LIST_NOTICE)
    public void handleRequestOpaPressListNotice(final JsonEnvelope envelope) throws EventStreamException {
        final String triggerDate = envelope.payloadAsJsonObject().getString(TRIGGER_DATE);

        LOGGER.info("progression.command.request-opa-press-list-notice triggered at {} ", triggerDate);

        final EventStream eventStream = eventSource.getStreamById(UUID.randomUUID());

        final OpaPressListNoticeRequested event = opaPressListNoticeRequested().withTriggerDate(LocalDate.parse(triggerDate)).build();

        eventStream.append(Stream.of(event).map(toEnvelopeWithMetadataFrom(envelope)));
    }

    @Handles(PROGRESSION_COMMAND_REQUEST_OPA_RESULT_LIST_NOTICE)
    public void handleRequestOpaResultListNotice(final JsonEnvelope envelope) throws EventStreamException {
        final String triggerDate = envelope.payloadAsJsonObject().getString(TRIGGER_DATE);

        LOGGER.info("progression.command.request-opa-result-list-notice triggered at {} ", triggerDate);

        final EventStream eventStream = eventSource.getStreamById(UUID.randomUUID());

        final OpaResultListNoticeRequested event = opaResultListNoticeRequested().withTriggerDate(LocalDate.parse(triggerDate)).build();

        eventStream.append(Stream.of(event).map(toEnvelopeWithMetadataFrom(envelope)));
    }

    @Handles(PROGRESSION_COMMAND_OPA_PUBLIC_LIST_NOTICE_SENT)
    public void handleOpaPublicListNoticeSent(final Envelope<OpaNoticeSent> envelope) throws EventStreamException {
        final OpaNoticeSent payload = envelope.payload();

        LOGGER.info("progression.command.opa-public-list-notice-sent for hearingId {}, triggerDate {} ", payload.getHearingId(), payload.getTriggerDate());

        final EventStream eventStream = eventSource.getStreamById(payload.getHearingId());
        final HearingAggregate hearingAggregate = aggregateService.get(eventStream, HearingAggregate.class);
        final Stream<Object> events = hearingAggregate.opaPublicListNoticeSent(payload.getNotificationId(),
                payload.getHearingId(),
                payload.getDefendantId(),
                payload.getTriggerDate());

        eventStream.append(events.map(toEnvelopeWithMetadataFrom(envelope)));
    }

    @Handles(PROGRESSION_COMMAND_OPA_PRESS_LIST_NOTICE_SENT)
    public void handleOpaPressListNoticeSent(final Envelope<OpaNoticeSent> envelope) throws EventStreamException {
        final OpaNoticeSent payload = envelope.payload();

        LOGGER.info("progression.command.opa-press-list-notice-sent for hearingId {}, triggerDate {} ", payload.getHearingId(), payload.getTriggerDate());

        final EventStream eventStream = eventSource.getStreamById(payload.getHearingId());
        final HearingAggregate hearingAggregate = aggregateService.get(eventStream, HearingAggregate.class);
        final Stream<Object> events = hearingAggregate.opaPressListNoticeSent(payload.getNotificationId(),
                payload.getHearingId(),
                payload.getDefendantId(),
                payload.getTriggerDate());

        eventStream.append(events.map(toEnvelopeWithMetadataFrom(envelope)));
    }

    @Handles(PROGRESSION_COMMAND_OPA_RESULT_LIST_NOTICE_SENT)
    public void handleOpaResultListNoticeSent(final Envelope<OpaNoticeSent> envelope) throws EventStreamException {
        final OpaNoticeSent payload = envelope.payload();

        LOGGER.info("progression.command.opa-result-list-notice-sent for hearingId {}, triggerDate {} ", payload.getHearingId(), payload.getTriggerDate());

        final EventStream eventStream = eventSource.getStreamById(payload.getHearingId());
        final HearingAggregate hearingAggregate = aggregateService.get(eventStream, HearingAggregate.class);
        final Stream<Object> events = hearingAggregate.opaResultListNoticeSent(payload.getNotificationId(),
                payload.getHearingId(),
                payload.getDefendantId(),
                payload.getTriggerDate());

        eventStream.append(events.map(toEnvelopeWithMetadataFrom(envelope)));
    }

    @Handles(PROGRESSION_COMMAND_GENERATE_OPA_PUBLIC_LIST_NOTICE)
    public void handleGenerateOpaPublicListNotice(final Envelope<GenerateOpaNotice> envelope) throws EventStreamException {
        final GenerateOpaNotice event = envelope.payload();

        LOGGER.info("progression.command.generate-opa-public-list-notice with caseId={} for defendantId={}", event.getCaseId(), event.getDefendantId());

        final EventStream eventStream = eventSource.getStreamById(event.getHearingId());
        final HearingAggregate hearingAggregate = aggregateService.get(eventStream, HearingAggregate.class);

        if (!hearingAggregate.isPublicListNoticeAlreadySent(event.getDefendantId(), event.getTriggerDate())) {
            if (hearingAggregate.checkOpaPublicListCriteria(event.getDefendantId(), event.getTriggerDate())) {
                final EventStream caseEventStream = eventSource.getStreamById(event.getCaseId());
                final CaseAggregate caseAggregate = aggregateService.get(caseEventStream, CaseAggregate.class);
                final ProsecutionCase prosecutionCase = caseAggregate.getProsecutionCase();
                final Stream<Object> events = hearingAggregate.generateOpaPublicListNotice(prosecutionCase, event.getDefendantId(), event.getTriggerDate());

                eventStream.append(events.map(toEnvelopeWithMetadataFrom(envelope)));
            } else {
                final Stream<Object> events = hearingAggregate.generateDeactivateOpaPublicListNotice(event.getCaseId(), event.getDefendantId(), event.getHearingId());
                eventStream.append(events.map(toEnvelopeWithMetadataFrom(envelope)));
            }
        } else {
            LOGGER.warn("Already progression.command.generate-opa-public-list-notice with caseId={} for defendantId={}, hence ignoring this request.", event.getCaseId(), event.getDefendantId());
        }
    }

    @Handles(PROGRESSION_COMMAND_GENERATE_OPA_PRESS_LIST_NOTICE)
    public void handleGenerateOpaPressListNotice(final Envelope<GenerateOpaNotice> envelope) throws EventStreamException {
        final GenerateOpaNotice event = envelope.payload();
        LOGGER.info("progression.command.generate-opa-press-list-notice with caseId={} for defendantId={}", event.getCaseId(), event.getDefendantId());
        final EventStream eventStream = eventSource.getStreamById(event.getHearingId());
        final HearingAggregate hearingAggregate = aggregateService.get(eventStream, HearingAggregate.class);

        if (!hearingAggregate.isPressListNoticeAlreadySent(event.getDefendantId(), event.getTriggerDate())) {
            if (hearingAggregate.checkOpaPressListCriteria(event.getTriggerDate())) {
                final EventStream caseEventStream = eventSource.getStreamById(event.getCaseId());
                final CaseAggregate caseAggregate = aggregateService.get(caseEventStream, CaseAggregate.class);
                final ProsecutionCase prosecutionCase = caseAggregate.getProsecutionCase();
                final OnlinePleasAllocation onlinePleasAllocation = caseAggregate.getOnlinePleasAllocation(event.getDefendantId());
                final Stream<Object> events = hearingAggregate.generateOpaPressListNotice(prosecutionCase, event.getDefendantId(), onlinePleasAllocation, event.getTriggerDate());

                eventStream.append(events.map(toEnvelopeWithMetadataFrom(envelope)));
            } else {
                final Stream<Object> events = hearingAggregate.generateDeactivateOpaPressListNotice(event.getCaseId(), event.getDefendantId(), event.getHearingId());
                eventStream.append(events.map(toEnvelopeWithMetadataFrom(envelope)));
            }
        } else {
            LOGGER.warn("Already generated progression.command.generate-opa-press-list-notice with caseId={} for defendantId={}, hence ignoring this request.", event.getCaseId(), event.getDefendantId());
        }
    }

    @Handles(PROGRESSION_COMMAND_GENERATE_OPA_RESULT_LIST_NOTICE)
    public void handleGenerateOpaResultListNotice(final Envelope<GenerateOpaNotice> envelope) throws EventStreamException {
        final GenerateOpaNotice event = envelope.payload();
        LOGGER.info("progression.command.generate-opa-result-list-notice with caseId={} for defendantId={}", event.getCaseId(), event.getDefendantId());
        final EventStream eventStream = eventSource.getStreamById(event.getHearingId());
        final HearingAggregate hearingAggregate = aggregateService.get(eventStream, HearingAggregate.class);

        if (!hearingAggregate.isResultListNoticeAlreadySent(event.getDefendantId(), event.getTriggerDate())) {
            if (hearingAggregate.checkOpaResultListCriteria(event.getDefendantId(), event.getTriggerDate())) {
                final EventStream caseEventStream = eventSource.getStreamById(event.getCaseId());
                final CaseAggregate caseAggregate = aggregateService.get(caseEventStream, CaseAggregate.class);
                final ProsecutionCase prosecutionCase = caseAggregate.getProsecutionCase();
                final Stream<Object> events = hearingAggregate.generateOpaResultListNotice(prosecutionCase, event.getDefendantId(), event.getTriggerDate());

                eventStream.append(events.map(toEnvelopeWithMetadataFrom(envelope)));
            } else {
                final Stream<Object> events = hearingAggregate.generateDeactivateOpaResultListNotice(event.getCaseId(), event.getDefendantId(), event.getHearingId());
                eventStream.append(events.map(toEnvelopeWithMetadataFrom(envelope)));
            }
        } else {
            LOGGER.warn("Already generated progression.command.generate-opa-result-list-notice with caseId={} for defendantId={}, hence ignoring this request.", event.getCaseId(), event.getDefendantId());
        }
    }

    private Optional<UUID> getFirstHearing(final Set<UUID> hearingIds) {
        return hearingIds.stream().filter(this::isFirstHearingType).findFirst();
    }

    private boolean isFirstHearingType(final UUID hearingId) {
        final EventStream eventStream = eventSource.getStreamById(hearingId);
        final HearingAggregate hearingAggregate = aggregateService.get(eventStream, HearingAggregate.class);

        return hearingAggregate.getHearingType().getDescription().equals(FIRST_HEARING);
    }
}
