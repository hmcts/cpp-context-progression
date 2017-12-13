package uk.gov.moj.cpp.progression.command.handler;

import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Stream;

import javax.inject.Inject;
import javax.json.JsonObject;

import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.core.aggregate.AggregateService;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.eventsourcing.source.core.EventSource;
import uk.gov.justice.services.eventsourcing.source.core.EventStream;
import uk.gov.justice.services.eventsourcing.source.core.exception.EventStreamException;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.progression.aggregate.CaseProgressionAggregate;


public  class CaseProgressionCommandHandler {

    static final String FIELD_STREAM_ID = "caseId";

    @Inject
    EventSource eventSource;

    @Inject
    Enveloper enveloper;

    @Inject
    AggregateService aggregateService;

    @Inject
    JsonObjectToObjectConverter converter;


    protected void applyToCaseProgressionAggregate(final JsonEnvelope command,
                    final Function<CaseProgressionAggregate, Stream<Object>> function)
                    throws EventStreamException {
        EventStream eventStream =
                        eventSource.getStreamById(getCaseProgressionId(command.payloadAsJsonObject()));
        CaseProgressionAggregate aCaseProgression =
                        aggregateService.get(eventStream, CaseProgressionAggregate.class);

        Stream<Object> events = function.apply(aCaseProgression);
        eventStream.append(events.map(enveloper.withMetadataFrom(command)));
    }

    protected UUID getCaseProgressionId(final JsonObject payload) {
        return UUID.fromString(payload.getString(FIELD_STREAM_ID));
    }

}
