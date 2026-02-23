package uk.gov.moj.cpp.progression.handler;

import uk.gov.justice.core.courts.Defendant;
import uk.gov.justice.core.courts.ProsecutionCase;
import uk.gov.justice.progression.courts.DeleteHearing;
import uk.gov.justice.progression.courts.InsertCaseBdf;
import uk.gov.justice.progression.courts.RemoveDuplicateApplicationBdf;
import uk.gov.justice.progression.courts.application.AddCaseToHearingBdf;
import uk.gov.justice.progression.courts.application.CasesBdf;
import uk.gov.justice.progression.courts.application.DefendantsBdf;
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
import uk.gov.moj.cpp.progression.aggregate.CaseAggregate;
import uk.gov.moj.cpp.progression.aggregate.HearingAggregate;
import uk.gov.moj.cpp.progression.service.ProsecutionCaseQueryService;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;

import javax.inject.Inject;
import javax.json.JsonValue;

@ServiceComponent(Component.COMMAND_HANDLER)
public class AddCasesToHearingBdfHandler {

    @Inject
    private EventSource eventSource;

    @Inject
    private AggregateService aggregateService;

    @Inject
    ProsecutionCaseQueryService prosecutionCaseQueryService;

    @Inject
    private JsonObjectToObjectConverter jsonObjectToObjectConverter;

    @Handles("progression.command.add-case-to-hearing-bdf")
    public void handleAddCaseToHearing(final Envelope<AddCaseToHearingBdf> addCaseToHearingBdfEnvelope) throws EventStreamException {
        AddCaseToHearingBdf addCaseToHearingBdf = addCaseToHearingBdfEnvelope.payload();
        final JsonEnvelope jsonEnvelope = JsonEnvelope.envelopeFrom(addCaseToHearingBdfEnvelope.metadata(), JsonValue.NULL);

        final List<ProsecutionCase> cases = addCaseToHearingBdf.getCasesBdf().stream().map(CasesBdf::getCaseId).map(caseId -> prosecutionCaseQueryService.getProsecutionCase(jsonEnvelope, caseId.toString()))
                .filter(Optional::isPresent)
                .map(Optional::get)
                .map(jsonObject -> jsonObjectToObjectConverter.convert(jsonObject.getJsonObject("prosecutionCase"), ProsecutionCase.class))
                .map(pc -> filterCase(pc, addCaseToHearingBdf))
                .filter(Objects::nonNull)
                .toList();

        if(cases.isEmpty()){
            return;
        }
        final EventStream eventStream = eventSource.getStreamById(addCaseToHearingBdf.getHearingId());
        final HearingAggregate hearingAggregate = aggregateService.get(eventStream, HearingAggregate.class);
        final Stream<Object> events = hearingAggregate.addCasesToHearingBdf(addCaseToHearingBdf.getHearingId(), cases );
        appendEventsToStream(addCaseToHearingBdfEnvelope, eventStream, events);

    }

    @Handles("progression.command.insert-case-bdf")
    public void handleInsertCase(final Envelope<InsertCaseBdf> insertCaseBdfEnvelope) throws EventStreamException {
        final EventStream eventStream = eventSource.getStreamById(insertCaseBdfEnvelope.payload().getProsecutionCaseId());
        final CaseAggregate caseAggregate = aggregateService.get(eventStream, CaseAggregate.class);
        final Stream<Object> events = caseAggregate.insertCase(insertCaseBdfEnvelope.payload().getProsecutionCase());
        appendEventsToStream(insertCaseBdfEnvelope, eventStream, events);

    }

    @Handles("progression.command.handler.remove-duplicate-application-bdf")
    public void removeDuplicateApplication(final Envelope<RemoveDuplicateApplicationBdf> removeDuplicateApplicationBdf) throws EventStreamException {

        final RemoveDuplicateApplicationBdf removeDuplicateApplicationFromHearing = removeDuplicateApplicationBdf.payload();
        final EventStream eventStream = eventSource.getStreamById(removeDuplicateApplicationFromHearing.getHearingId());
        final HearingAggregate hearingAggregate = aggregateService.get(eventStream, HearingAggregate.class);
        final Stream<Object> events = hearingAggregate.removeDuplicateApplicationByBdf();
        //appendEventsToStream(removeDuplicateApplicationBdf, eventStream, events);
        eventStream.append(events.map(Enveloper.toEnvelopeWithMetadataFrom(removeDuplicateApplicationBdf)));
    }

    private static ProsecutionCase filterCase(final ProsecutionCase pc, final AddCaseToHearingBdf addCaseToHearingBdf) {
        final ProsecutionCase prosecutionCase = ProsecutionCase.prosecutionCase().withValuesFrom(pc)
                .withDefendants(filterDefendants(pc, addCaseToHearingBdf.getCasesBdf().stream().filter(c -> c.getCaseId().equals(pc.getId())).flatMap(c -> c.getDefendantsBdf().stream()).toList()))
                .build();
        if(prosecutionCase.getDefendants().isEmpty()){
            return null;
        }else{
            return prosecutionCase;
        }
    }

    private static List<Defendant> filterDefendants(final ProsecutionCase pc, final List<DefendantsBdf> defendantsBdf) {
        return pc.getDefendants().stream()
                .filter(def -> defendantsBdf.stream().anyMatch(d -> d.getDefendantId().equals(def.getId())))
                .map(def -> Defendant.defendant().withValuesFrom(def)
                        .withOffences(def.getOffences().stream()
                                .filter(off -> defendantsBdf.stream().filter(d -> d.getDefendantId().equals(def.getId())).flatMap(d -> d.getOffences().stream()).anyMatch(o -> o.equals(off.getId())))
                                .toList())
                        .build())
                .filter(def -> !def.getOffences().isEmpty())
                .toList();
    }


    private void appendEventsToStream(final Envelope<?> envelope, final EventStream eventStream, final Stream<Object> events) throws EventStreamException {
        final JsonEnvelope jsonEnvelope = JsonEnvelope.envelopeFrom(envelope.metadata(), JsonValue.NULL);
        eventStream.append(
                events
                        .map(Enveloper.toEnvelopeWithMetadataFrom(jsonEnvelope)));
    }
}
