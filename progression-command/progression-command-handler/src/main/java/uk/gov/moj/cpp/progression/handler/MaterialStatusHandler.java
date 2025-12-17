package uk.gov.moj.cpp.progression.handler;

import static java.util.Objects.nonNull;
import static org.apache.commons.collections.CollectionUtils.isNotEmpty;

import uk.gov.justice.core.courts.CaseSubjects;
import uk.gov.justice.core.courts.Defendant;
import uk.gov.justice.core.courts.MaterialDetails;
import uk.gov.justice.core.courts.ProsecutionCase;
import uk.gov.justice.core.courts.ProsecutionCaseIdentifier;
import uk.gov.justice.core.courts.RecordNowsMaterialRequest;
import uk.gov.justice.core.courts.UpdateNowsMaterialStatus;
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
import uk.gov.moj.cpp.progression.aggregate.MaterialAggregate;
import uk.gov.moj.cpp.progression.service.ProsecutionCaseQueryService;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;

import javax.inject.Inject;
import javax.json.JsonObject;
import javax.json.JsonValue;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@SuppressWarnings({"squid:MethodCyclomaticComplexity", "squid:S134", "squid:S3776"})
@ServiceComponent(Component.COMMAND_HANDLER)
public class MaterialStatusHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(MaterialStatusHandler.class);
    private static final String PROSECUTION_CASE = "prosecutionCase";

    public static final String PROGRESSION_COMMAND_RECORD_NOWS_MATERIAL_REQUEST = "progression.command.record-nows-material-request";
    public static final String PROGRESSION_COMMAND_UPDATE_NOWS_MATERIAL_STATUS = "progression.command.update-nows-material-status";
    @Inject
    private EventSource eventSource;

    @Inject
    private AggregateService aggregateService;

    @Inject
    private ProsecutionCaseQueryService prosecutionCaseQueryService;

    @Inject
    private JsonObjectToObjectConverter jsonObjectToObjectConverter;

    @Inject
    private Enveloper enveloper;

    @Handles(PROGRESSION_COMMAND_RECORD_NOWS_MATERIAL_REQUEST)
    public void recordNowsMaterial(final JsonEnvelope envelope) throws EventStreamException {
        final RecordNowsMaterialRequest recordNowsMaterialRequest = jsonObjectToObjectConverter.convert(envelope.payloadAsJsonObject(), RecordNowsMaterialRequest.class);
        final EventStream eventStream = eventSource.getStreamById(recordNowsMaterialRequest.getContext().getMaterialId());
        final MaterialAggregate materialAggregate = aggregateService.get(eventStream, MaterialAggregate.class);
        final Stream<Object> events = materialAggregate.create(recordNowsMaterialRequest.getContext());
        appendEventsToStream(envelope, eventStream, events);
    }

    @Handles(PROGRESSION_COMMAND_UPDATE_NOWS_MATERIAL_STATUS)
    public void updateStatus(final JsonEnvelope envelope) throws EventStreamException {
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("{} {}", PROGRESSION_COMMAND_UPDATE_NOWS_MATERIAL_STATUS, envelope.toObfuscatedDebugString());
        }
        final UpdateNowsMaterialStatus update = jsonObjectToObjectConverter.convert(envelope.payloadAsJsonObject(), UpdateNowsMaterialStatus.class);
        final EventStream eventStream = eventSource.getStreamById(update.getMaterialId());
        final MaterialAggregate nowsAggregate = aggregateService.get(eventStream, MaterialAggregate.class);

       final  MaterialDetails materialDetails = nowsAggregate.getMaterialDetails();
        if(nonNull(materialDetails)&& nonNull(materialDetails.getIsNotificationApi()) && nonNull(materialDetails.getIsCps()) && materialDetails.getIsNotificationApi() && materialDetails.getIsCps()){
            final List<UUID> cases = nowsAggregate.fetchCases();
            if(nonNull(cases)) {
                final Optional<JsonObject> optionalProsecutionCase = prosecutionCaseQueryService.getProsecutionCase(envelope, cases.get(0).toString());
                if (!optionalProsecutionCase.isPresent()) {
                    throw new IllegalStateException(String.format("Unable to find the case %s", cases.get(0)));
                }

                final List<CaseSubjects> caseSubjects = fetchCaseUrns(envelope,cases);

                final ProsecutionCase prosecutionCase = jsonObjectToObjectConverter.convert(optionalProsecutionCase.get().getJsonObject(PROSECUTION_CASE), ProsecutionCase.class);
                if(nonNull(prosecutionCase)) {
                    final List<Defendant> defendants = prosecutionCase.getDefendants();
                    final List<String> cpsDefendantIds = new ArrayList<>();
                    if(nonNull(defendants)) {
                        defendants.forEach(defendant -> {
                            if (nonNull(defendant.getCpsDefendantId())) {
                                cpsDefendantIds.add(defendant.getCpsDefendantId().toString());
                            }
                        });
                    }

                    final Stream<Object> events = nowsAggregate.nowsMaterialStatusUpdated(update.getMaterialId(), update.getStatus(),
                            caseSubjects, isNotEmpty(cpsDefendantIds)?cpsDefendantIds:null);
                    appendEventsToStream(envelope, eventStream, events);
                }
            }
        }
        else {
            final Stream<Object> events = nowsAggregate.nowsMaterialStatusUpdated(update.getMaterialId(), update.getStatus(),
                    null, null);
            appendEventsToStream(envelope, eventStream, events);
        }
    }

    private List<CaseSubjects> fetchCaseUrns(final JsonEnvelope envelope, final List<UUID> cases) {
        final List<CaseSubjects> caseSubjects = new ArrayList<>();
        if (isNotEmpty(cases)) {
            cases.forEach(c -> {
                final Optional<JsonObject> optionalProsecutionCase = prosecutionCaseQueryService.getProsecutionCase(envelope, c.toString());
                if (optionalProsecutionCase.isPresent()) {
                    final ProsecutionCase prosecutionCase = jsonObjectToObjectConverter.convert(optionalProsecutionCase.get().getJsonObject(PROSECUTION_CASE), ProsecutionCase.class);
                    if (nonNull(prosecutionCase)) {
                        final ProsecutionCaseIdentifier prosecutionCaseIdentifier = prosecutionCase.getProsecutionCaseIdentifier();
                        final CaseSubjects caseSubject = CaseSubjects.caseSubjects()
                                .withUrn(nonNull(prosecutionCaseIdentifier.getCaseURN()) ? prosecutionCaseIdentifier.getCaseURN():prosecutionCaseIdentifier.getProsecutionAuthorityReference())
                                .withProsecutingAuthorityOUCode(prosecutionCaseIdentifier.getProsecutionAuthorityOUCode())
                                .build();
                        caseSubjects.add(caseSubject);
                    }
                }
            });
        }
        return caseSubjects;
    }

    private void appendEventsToStream(final Envelope<?> envelope, final EventStream eventStream, final Stream<Object> events) throws EventStreamException {
        final JsonEnvelope jsonEnvelope = JsonEnvelope.envelopeFrom(envelope.metadata(), JsonValue.NULL);
        eventStream.append(events.map(enveloper.withMetadataFrom(jsonEnvelope)));
    }

}
