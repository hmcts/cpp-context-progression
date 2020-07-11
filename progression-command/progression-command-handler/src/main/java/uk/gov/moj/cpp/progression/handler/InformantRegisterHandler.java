package uk.gov.moj.cpp.progression.handler;

import static java.util.UUID.fromString;
import static javax.json.Json.createObjectBuilder;
import static org.apache.commons.collections.CollectionUtils.isNotEmpty;
import static uk.gov.justice.services.messaging.Envelope.envelopeFrom;

import uk.gov.justice.core.courts.informantRegisterDocument.InformantRegisterDocumentRequest;
import uk.gov.justice.progression.courts.GenerateInformantRegister;
import uk.gov.justice.progression.courts.NotifyInformantRegister;
import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.common.converter.StringToJsonObjectConverter;
import uk.gov.justice.services.core.aggregate.AggregateService;
import uk.gov.justice.services.core.annotation.Component;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.requester.Requester;
import uk.gov.justice.services.eventsourcing.source.core.EventSource;
import uk.gov.justice.services.eventsourcing.source.core.EventStream;
import uk.gov.justice.services.eventsourcing.source.core.exception.EventStreamException;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.Metadata;
import uk.gov.moj.cpp.progression.aggregate.ProsecutionAuthorityAggregate;
import uk.gov.moj.cpp.progression.command.GenerateInformantRegisterByDate;
import uk.gov.moj.cpp.progression.domain.constant.RegisterStatus;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.inject.Inject;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ServiceComponent(Component.COMMAND_HANDLER)
public class InformantRegisterHandler extends AbstractCommandHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger(InformantRegisterHandler.class.getName());
    private static final String INFORMANT_REGISTER_QUERY_BY_STATUS = "progression.query.informant-register-document-request";
    private static final String INFORMANT_REGISTER_QUERY_BY_DATE = "progression.query.informant-register-document-by-request-date";
    private static final String FIELD_INFORMANT_REGISTER_DOCUMENTS = "informantRegisterDocumentRequests";
    private static final String FIELD_PROSECUTION_AUTHORITY_ID = "prosecutionAuthorityId";
    private static final String FIELD_PAYLOAD = "payload";
    private static final String FIELD_REQUEST_STATUS = "requestStatus";
    private static final String FIELD_REGISTER_DATE = "registerDate";
    private static final String FIELD_PROSECUTION_AUTHORITY_CODE = "prosecutionAuthorityCode";

    @Inject
    private EventSource eventSource;

    @Inject
    private AggregateService aggregateService;

    @Inject
    private Requester requester;

    @Inject
    private JsonObjectToObjectConverter jsonObjectToObjectConverter;

    @Inject
    private StringToJsonObjectConverter stringToJsonObjectConverter;

    @Handles("progression.command.add-informant-register")
    public void handleAddInformantRegisterToEventStream(final Envelope<InformantRegisterDocumentRequest> envelope) throws EventStreamException {
        LOGGER.debug("progression.command.add-informant-register {}", envelope);

        final InformantRegisterDocumentRequest informantRegisterDocumentRequest = envelope.payload();

        final UUID prosecutionAuthorityId = informantRegisterDocumentRequest.getProsecutionAuthorityId();

        final EventStream eventStream = eventSource.getStreamById(informantRegisterDocumentRequest.getProsecutionAuthorityId());

        final ProsecutionAuthorityAggregate prosecutionAuthorityAggregate = aggregateService.get(eventStream, ProsecutionAuthorityAggregate.class);

        final Stream<Object> events = prosecutionAuthorityAggregate.createInformantRegister(prosecutionAuthorityId, informantRegisterDocumentRequest);

        appendEventsToStream(envelope, eventStream, events);
    }

    @Handles("progression.command.generate-informant-register")
    public void handleGenerateInformantRegister(final Envelope<GenerateInformantRegister> jsonEnvelope) {
        final Map<UUID, List<JsonObject>> informantRegisterDocumentRequests = getInformantRegisterDocumentRequests(jsonEnvelope);
        informantRegisterDocumentRequests.forEach((prosecutionAuthorityId, informantRegisterRequest) -> processRequests(prosecutionAuthorityId, informantRegisterRequest, jsonEnvelope, true));
    }

    @Handles("progression.command.generate-informant-register-by-date")
    public void handleGenerateInformantRegisterByDate(final Envelope<GenerateInformantRegisterByDate> jsonEnvelope) {
        final GenerateInformantRegisterByDate generateInformantRegisterByDate = jsonEnvelope.payload();
        final Map<UUID, List<JsonObject>> informantRegisterDocumentRequests = getInformantRegisterDocumentRequestsByDate(generateInformantRegisterByDate, jsonEnvelope);
        informantRegisterDocumentRequests.forEach((prosecutionAuthorityId, informantRegisterRequest) -> processRequests(prosecutionAuthorityId, informantRegisterRequest, jsonEnvelope, false));
    }


    @Handles("progression.command.notify-informant-register")
    public void handleNotifyInformantRegister(final Envelope<NotifyInformantRegister> jsonEnvelope) throws
            EventStreamException {

        final NotifyInformantRegister notifyInformantRegister = jsonEnvelope.payload();
        final EventStream eventStream = eventSource.getStreamById(notifyInformantRegister.getProsecutionAuthorityId());
        final ProsecutionAuthorityAggregate prosecutionAuthorityAggregate = aggregateService.get(eventStream, ProsecutionAuthorityAggregate.class);
        final Stream<Object> events = prosecutionAuthorityAggregate.notifyProsecutingAuthority(notifyInformantRegister);
        appendEventsToStream(jsonEnvelope, eventStream, events);
    }


    private void processRequests(final UUID prosecutionAuthorityId, final List<JsonObject> informantRegisterRequest, final Envelope jsonEnvelope, final boolean systemGenerated) {
        try {
            final EventStream eventStream = eventSource.getStreamById(prosecutionAuthorityId);
            final ProsecutionAuthorityAggregate prosecutionAuthorityAggregate = aggregateService.get(eventStream, ProsecutionAuthorityAggregate.class);
            final List<InformantRegisterDocumentRequest> informantRegisterDocumentRequests = informantRegisterRequest.stream().map(informantRegister -> stringToJsonObjectConverter.convert(informantRegister.getString((FIELD_PAYLOAD))))
                    .map(informantRegister -> jsonObjectToObjectConverter.convert(informantRegister, InformantRegisterDocumentRequest.class))
                    .collect(Collectors.toList());

            final Stream<Object> events = prosecutionAuthorityAggregate.generateInformantRegister(informantRegisterDocumentRequests, systemGenerated);

            appendEventsToStream(jsonEnvelope, eventStream, events);
        } catch (EventStreamException e) {
            LOGGER.error("Generate informant register stream exception -->>", e);
        }
    }

    private Map<UUID, List<JsonObject>> getInformantRegisterDocumentRequests(final Envelope envelope) {
        final List<JsonObject> informantRegisterDocumentRequests = queryInformantRegistersByStatus(envelope);

        return informantRegisterDocumentRequests.stream().collect(Collectors.groupingBy(request -> fromString(request.getString(FIELD_PROSECUTION_AUTHORITY_ID))));
    }

    private Map<UUID, List<JsonObject>> getInformantRegisterDocumentRequestsByDate(final GenerateInformantRegisterByDate generateInformantRegisterByDate, final Envelope envelope) {
        final List<JsonObject> informantRegisterDocumentRequests = queryInformantRegistersByDate(generateInformantRegisterByDate, envelope);

        return informantRegisterDocumentRequests.stream().collect(Collectors.groupingBy(request -> fromString(request.getString(FIELD_PROSECUTION_AUTHORITY_ID))));
    }

    private List<JsonObject> queryInformantRegistersByStatus(final Envelope envelope) {
        final Metadata metadata = Envelope.metadataFrom(envelope.metadata()).withName(INFORMANT_REGISTER_QUERY_BY_STATUS).build();
        final Envelope<JsonObject> requestEnvelope = envelopeFrom(metadata, createObjectBuilder().add(FIELD_REQUEST_STATUS, RegisterStatus.RECORDED.name()).build());
        return requester.request(requestEnvelope)
                .payloadAsJsonObject()
                .getJsonArray(FIELD_INFORMANT_REGISTER_DOCUMENTS)
                .getValuesAs(JsonObject.class);
    }

    private List<JsonObject> queryInformantRegistersByDate(final GenerateInformantRegisterByDate generateInformantRegisterByDate, final Envelope envelope) {
        final Metadata metadata = Envelope.metadataFrom(envelope.metadata()).withName(INFORMANT_REGISTER_QUERY_BY_DATE).build();

        final JsonObjectBuilder queryParameters = createObjectBuilder().add(FIELD_REGISTER_DATE, generateInformantRegisterByDate.getRegisterDate());
        if (isNotEmpty(generateInformantRegisterByDate.getProsecutionAuthorities())) {
            final String prosecutionAuthoritiesAsString = generateInformantRegisterByDate.getProsecutionAuthorities().stream().map(String::valueOf)
                    .collect(Collectors.joining(","));
            queryParameters.add(FIELD_PROSECUTION_AUTHORITY_CODE, prosecutionAuthoritiesAsString);
        }

        final Envelope<JsonObject> requestEnvelope = envelopeFrom(metadata, queryParameters.build());
        return requester.request(requestEnvelope)
                .payloadAsJsonObject()
                .getJsonArray(FIELD_INFORMANT_REGISTER_DOCUMENTS)
                .getValuesAs(JsonObject.class);
    }
}
