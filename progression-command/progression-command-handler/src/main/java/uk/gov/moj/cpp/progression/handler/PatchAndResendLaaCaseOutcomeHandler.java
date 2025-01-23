package uk.gov.moj.cpp.progression.handler;

import static java.lang.String.format;
import static java.util.stream.Collectors.toList;
import static javax.json.JsonValue.NULL;
import static org.apache.commons.collections.CollectionUtils.isNotEmpty;
import static org.apache.commons.lang3.builder.ToStringBuilder.reflectionToString;
import static org.apache.commons.lang3.builder.ToStringStyle.SHORT_PREFIX_STYLE;
import static uk.gov.justice.services.core.enveloper.Enveloper.toEnvelopeWithMetadataFrom;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;

import uk.gov.justice.core.courts.LaaDefendantProceedingConcludedChanged;
import uk.gov.justice.services.common.converter.jackson.ObjectMapperProducer;
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
import uk.gov.moj.cpp.progression.command.handler.PatchAndResendLaaOutcomeConcluded;

import java.io.IOException;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

import javax.inject.Inject;

import com.fasterxml.jackson.databind.ObjectMapper;

@SuppressWarnings("squid:S1160")
@ServiceComponent(Component.COMMAND_HANDLER)
public class PatchAndResendLaaCaseOutcomeHandler {
    private static final String EVENT_LAA_DEFENDANT_PROCEEDING_CONCLUDED_CHANGED = "progression.event.laa-defendant-proceeding-concluded-changed";
    private final ObjectMapper objectMapper = new ObjectMapperProducer().objectMapper();

    @Inject
    private EventSource eventSource;

    @Inject
    private AggregateService aggregateService;

    /**
     * Handles a specific command for patching and resending the outcome concluded for a Legal Aid Application.
     * Retrieves relevant events from the event stream based on caseId and resultDate and with null hearingId,
     * then applies the aggregate patch method if a single matching event is found to generate new patched event.
     *
     * @param envelope Envelope containing PatchAndResendLaaOutcomeConcluded payload
     * @throws EventStreamException if there is an issue with the event stream
     */
    @Handles("progression.command.handler.patch-and-resend-laa-outcome-concluded")
    public void handle(final Envelope<PatchAndResendLaaOutcomeConcluded> envelope) throws EventStreamException {
        final PatchAndResendLaaOutcomeConcluded payload = envelope.payload();
        final UUID caseId = payload.getCaseId();
        final LocalDate resultDate = payload.getResultDate();
        final UUID hearingID = payload.getHearingId();

        EventStream eventStream = eventSource.getStreamById(caseId);
        final Stream<JsonEnvelope> jsonEnvelopeStream = eventStream.read();
        final List<LaaDefendantProceedingConcludedChanged> matchingEvents = jsonEnvelopeStream
                .filter(e -> EVENT_LAA_DEFENDANT_PROCEEDING_CONCLUDED_CHANGED.equals(e.metadata().name()))
                .filter(e -> e.metadata().createdAt().map(ZonedDateTime::toLocalDate).orElse(LocalDate.MIN).equals(resultDate))
                .map(e -> {
                    try {
                        return objectMapper.readValue(e.payloadAsJsonObject().toString(), LaaDefendantProceedingConcludedChanged.class);
                    } catch (IOException ex) {
                        throw new RuntimeException(format("unable to parse event for: %s ", reflectionToString(payload, SHORT_PREFIX_STYLE)), ex);
                    }
                })
                .filter(e -> e.getHearingId() == null)
                .collect(toList());

        if (isNotEmpty(matchingEvents) && matchingEvents.size() == 1) {
            eventStream = eventSource.getStreamById(caseId);
            final CaseAggregate caseAggregate = aggregateService.get(eventStream, CaseAggregate.class);
            final Stream<Object> events = caseAggregate.patchAndResendLaaOutcomeConcluded(matchingEvents.get(0), hearingID);
            appendEventsToStream(envelope, eventStream, events);
        } else {
            throw new RuntimeException(format("single %s event was expected but no of events found:%d, for request: %s",
                    EVENT_LAA_DEFENDANT_PROCEEDING_CONCLUDED_CHANGED, matchingEvents.size(), reflectionToString(payload, SHORT_PREFIX_STYLE)));
        }
    }

    private void appendEventsToStream(final Envelope<?> envelope, final EventStream eventStream, final Stream<Object> events) throws EventStreamException {
        final JsonEnvelope jsonEnvelope = envelopeFrom(envelope.metadata(), NULL);
        final Stream<JsonEnvelope> envelopeStream = events.map(toEnvelopeWithMetadataFrom(jsonEnvelope));
        eventStream.append(envelopeStream);
    }
}

