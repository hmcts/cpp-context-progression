package uk.gov.moj.cpp.progression.handler;

import static javax.json.Json.createObjectBuilder;
import static uk.gov.justice.services.messaging.Envelope.envelopeFrom;

import uk.gov.justice.core.courts.courtRegisterDocument.CourtRegisterDocumentRequest;
import uk.gov.justice.progression.courts.GenerateCourtRegister;
import uk.gov.justice.progression.courts.NotifyCourtRegister;
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
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.messaging.Metadata;
import uk.gov.moj.cpp.progression.aggregate.CourtCentreAggregate;
import uk.gov.moj.cpp.progression.command.GenerateCourtRegisterByDate;
import uk.gov.moj.cpp.progression.domain.constant.RegisterStatus;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.inject.Inject;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ServiceComponent(Component.COMMAND_HANDLER)
public class CourtRegisterHandler extends AbstractCommandHandler {
    private static final String QUERY_COURT_REGISTER_DOCUMENT_REQUEST = "progression.query.court-register-document-request";
    private static final String QUERY_COURT_REGISTER_DOCUMENT_REQUEST_BY_DATE = "progression.query.court-register-document-by-request-date";
    private static final String FIELD_COURT_REGISTER_DOCUMENT_REQUESTS = "courtRegisterDocumentRequests";
    private static final String FIELD_COURT_CENTRE_ID = "courtCentreId";
    private static final String FIELD_REQUEST_STATUS = "requestStatus";
    private static final String FIELD_REGISTER_DATE = "registerDate";
    private static final String FIELD_COURT_HOUSE = "courtHouse";

    @Inject
    private JsonObjectToObjectConverter jsonObjectToObjectConverter;

    @Inject
    private StringToJsonObjectConverter stringToJsonObjectConverter;

    @Inject
    private EventSource eventSource;

    @Inject
    private AggregateService aggregateService;

    @Inject
    private Requester requester;

    private static final Logger LOGGER = LoggerFactory.getLogger(CourtRegisterHandler.class.getName());

    @Handles("progression.command.add-court-register")
    public void handleAddCourtRegister(final Envelope<CourtRegisterDocumentRequest> courtRegisterDocumentRequestEnvelope) throws EventStreamException {
        LOGGER.debug("progression.command.add-court-register {}", courtRegisterDocumentRequestEnvelope);

        final CourtRegisterDocumentRequest courtRegisterDocumentRequest = courtRegisterDocumentRequestEnvelope.payload();

        final UUID courtCentreId = courtRegisterDocumentRequest.getCourtCentreId();

        final EventStream eventStream = eventSource.getStreamById(courtCentreId);

        final CourtCentreAggregate courtCentreAggregate = aggregateService.get(eventStream, CourtCentreAggregate.class);

        final Stream<Object> events = courtCentreAggregate.createCourtRegister(courtCentreId, courtRegisterDocumentRequest);

        appendEventsToStream(courtRegisterDocumentRequestEnvelope, eventStream, events);
    }

    @Handles("progression.command.generate-court-register")
    public void handleGenerateCourtRegister(final Envelope<GenerateCourtRegister> jsonEnvelope) {
        final Map<String, List<JsonObject>> courtRegisterDocumentRequests = this.getCourtRegisterDocumentRequests(RegisterStatus.RECORDED.name(), jsonEnvelope);
        courtRegisterDocumentRequests.forEach((courtCentreId, courtRegisterRequest) -> processRequests(courtCentreId, courtRegisterRequest, jsonEnvelope, true));
    }

    @Handles("progression.command.generate-court-register-by-date")
    public void handleGenerateCourtRegisterByDate(final Envelope<GenerateCourtRegisterByDate> jsonEnvelope) {
        final GenerateCourtRegisterByDate generateCourtRegisterByDate = jsonEnvelope.payload();
        final Map<String, List<JsonObject>> courtRegisterDocumentRequests = this.getCourtRegisterDocumentRequestsByDate(generateCourtRegisterByDate, jsonEnvelope);
        courtRegisterDocumentRequests.forEach((courtCentreId, courtRegisterRequest) -> processRequests(courtCentreId, courtRegisterRequest, jsonEnvelope, false));
    }

    @Handles("progression.command.notify-court-register")
    public void handleNotifyCourtCentre(final Envelope<NotifyCourtRegister> jsonEnvelope) throws EventStreamException {

        final NotifyCourtRegister notifyCourtRegister = jsonEnvelope.payload();
        final EventStream eventStream = eventSource.getStreamById(notifyCourtRegister.getCourtCentreId());
        final CourtCentreAggregate courtRegisterAggregate = aggregateService.get(eventStream, CourtCentreAggregate.class);

        final Stream<Object> events = courtRegisterAggregate.notifyCourt(notifyCourtRegister);

        appendEventsToStream(jsonEnvelope, eventStream, events);

    }

    private void processRequests(final String courtCentreId, final List<JsonObject> courtRegisterRequest, final Envelope jsonEnvelope, final boolean systemGenerated) {
        try {
            final EventStream eventStream = eventSource.getStreamById(UUID.fromString(courtCentreId));
            final CourtCentreAggregate courtRegisterAggregate = aggregateService.get(eventStream, CourtCentreAggregate.class);
            final List<CourtRegisterDocumentRequest> courtRegisterDocumentRequests = courtRegisterRequest.stream().map(r -> stringToJsonObjectConverter.convert(r.getString(("payload"))))
                    .map(r -> jsonObjectToObjectConverter.convert(r, CourtRegisterDocumentRequest.class))
                    .collect(Collectors.toList());
            final Stream<Object> events = courtRegisterAggregate.generateDocument(courtRegisterDocumentRequests, systemGenerated);
            appendEventsToStream(jsonEnvelope, eventStream, events);
        } catch (EventStreamException e) {
            LOGGER.error("Generate court register stream exception -->>", e);
        }
    }

    private Map<String, List<JsonObject>> getCourtRegisterDocumentRequests(final String requestStatus, final Envelope envelope) {
        final JsonObject courtRegisterDocumentRequests = this.queryCourtRegisterDocumentRequests(requestStatus, envelope);

        return courtRegisterDocumentRequests.getJsonArray(FIELD_COURT_REGISTER_DOCUMENT_REQUESTS).stream()
                .map(r -> (JsonObject) r).collect(Collectors.groupingBy(request -> request.getString(FIELD_COURT_CENTRE_ID)));
    }

    private Map<String, List<JsonObject>> getCourtRegisterDocumentRequestsByDate(final GenerateCourtRegisterByDate generateCourtRegisterByDate, final Envelope envelope) {
        final JsonObject courtRegisterDocumentRequests = this.queryCourtRegisterDocumentRequestsByDate(generateCourtRegisterByDate, envelope);

        return courtRegisterDocumentRequests.getJsonArray(FIELD_COURT_REGISTER_DOCUMENT_REQUESTS).stream()
                .map(r -> (JsonObject) r).collect(Collectors.groupingBy(request -> request.getString(FIELD_COURT_CENTRE_ID)));
    }

    private JsonObject queryCourtRegisterDocumentRequests(final String requestStatus, final Envelope envelope) {
        final Metadata metadata = Envelope.metadataFrom(envelope.metadata()).withName(QUERY_COURT_REGISTER_DOCUMENT_REQUEST).build();
        final Envelope<JsonObject> requestEnvelope = envelopeFrom(metadata, createObjectBuilder().add(FIELD_REQUEST_STATUS, requestStatus).build());
        final JsonEnvelope courtRegisterRequestEnvelope = requester.request(requestEnvelope);
        return courtRegisterRequestEnvelope.payloadAsJsonObject();
    }

    private JsonObject queryCourtRegisterDocumentRequestsByDate(final GenerateCourtRegisterByDate generateCourtRegisterByDate, final Envelope envelope) {
        final Metadata metadata = Envelope.metadataFrom(envelope.metadata()).withName(QUERY_COURT_REGISTER_DOCUMENT_REQUEST_BY_DATE).build();

        final JsonObjectBuilder queryParameters = createObjectBuilder().add(FIELD_REGISTER_DATE, generateCourtRegisterByDate.getRegisterDate());
        if(StringUtils.isNotBlank(generateCourtRegisterByDate.getCourtHouse())) {
            queryParameters.add(FIELD_COURT_HOUSE, generateCourtRegisterByDate.getCourtHouse());
        }

        final Envelope<JsonObject> requestEnvelope = envelopeFrom(metadata, queryParameters.build());
        final JsonEnvelope courtRegisterRequestEnvelope = requester.request(requestEnvelope);
        return courtRegisterRequestEnvelope.payloadAsJsonObject();
    }
}
