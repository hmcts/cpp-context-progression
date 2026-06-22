package uk.gov.moj.cpp.progression.query;

import static java.util.Objects.nonNull;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;

import uk.gov.justice.services.core.annotation.Component;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.messaging.JsonObjects;
import uk.gov.moj.cpp.prosecutioncase.persistence.entity.NowDocumentRequestEntity;
import uk.gov.moj.cpp.prosecutioncase.persistence.repository.NowDocumentRequestRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import javax.inject.Inject;
import uk.gov.justice.services.messaging.JsonObjects;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObjectBuilder;

@ServiceComponent(Component.QUERY_VIEW)
public class NowDocumentRequestQueryView {
    @Inject
    private NowDocumentRequestRepository nowDocumentRequestRepository;

    private static final String NOW_DOCUMENT_REQUESTS_BY_REQUEST_ID_QUERY = "progression.query.now-document-requests-by-request-id";
    private static final String NOW_DOCUMENT_REQUEST_BY_HEARING_QUERY = "progression.query.now-document-request-by-hearing";
    private static final String REQUEST_ID_PARAM = "requestId";
    private static final String MATERIAL_ID_PARAM = "materialId";
    private static final String HEARING_ID_PARAM = "hearingId";
    private static final String PAYLOAD_PARAM = "payload";
    private static final String NOW_DOCUMENT_REQUESTS_PARAM = "nowDocumentRequests";

    @Handles(NOW_DOCUMENT_REQUESTS_BY_REQUEST_ID_QUERY)
    public JsonEnvelope getNowDocumentRequestsByRequestId(final JsonEnvelope envelope) {
        final Optional<UUID> requestId = JsonObjects.getUUID(envelope.payloadAsJsonObject(), REQUEST_ID_PARAM);
        final JsonArrayBuilder jsonArrayBuilder = JsonObjects.createArrayBuilder();
        final JsonObjectBuilder jsonObjectBuilder = JsonObjects.createObjectBuilder();
        if (requestId.isPresent()) {
            final List<NowDocumentRequestEntity> nowDocumentRequests = nowDocumentRequestRepository.findByRequestId(requestId.get());

            nowDocumentRequests.forEach(nowDocumentRequestEntity ->
                    jsonArrayBuilder.add(
                            JsonObjects.createObjectBuilder()
                                    .add(MATERIAL_ID_PARAM, nowDocumentRequestEntity.getMaterialId().toString())
                                    .add(REQUEST_ID_PARAM, nowDocumentRequestEntity.getRequestId().toString())
                                    .add(HEARING_ID_PARAM, nowDocumentRequestEntity.getHearingId().toString())
                                    .add(PAYLOAD_PARAM, nowDocumentRequestEntity.getPayload())
                    )
            );

            jsonObjectBuilder.add(NOW_DOCUMENT_REQUESTS_PARAM, jsonArrayBuilder.build());
        }
        return envelopeFrom(envelope.metadata(), jsonObjectBuilder.build());
    }

    @Handles(NOW_DOCUMENT_REQUEST_BY_HEARING_QUERY)
    public JsonEnvelope getNowDocumentRequestByHearing(final JsonEnvelope envelope) {
        final Optional<UUID> hearingId = JsonObjects.getUUID(envelope.payloadAsJsonObject(), HEARING_ID_PARAM);
        final JsonArrayBuilder jsonArrayBuilder = JsonObjects.createArrayBuilder();
        final JsonObjectBuilder jsonObjectBuilder = JsonObjects.createObjectBuilder();
        if (hearingId.isPresent()) {
            final List<NowDocumentRequestEntity> nowDocumentRequests = nowDocumentRequestRepository.findByHearingId(hearingId.get());
            nowDocumentRequests.forEach(nowDocumentRequestEntity -> {
                final UUID requestId = nowDocumentRequestEntity.getRequestId();
                final JsonObjectBuilder builder = JsonObjects.createObjectBuilder()
                        .add(MATERIAL_ID_PARAM, nowDocumentRequestEntity.getMaterialId().toString())
                        .add(HEARING_ID_PARAM, nowDocumentRequestEntity.getHearingId().toString())
                        .add(PAYLOAD_PARAM, nowDocumentRequestEntity.getPayload());
                if (nonNull(requestId)) {
                    builder.add(REQUEST_ID_PARAM, nowDocumentRequestEntity.getRequestId().toString());
                }
                jsonArrayBuilder.add(builder);
            });
            jsonObjectBuilder.add(NOW_DOCUMENT_REQUESTS_PARAM, jsonArrayBuilder.build());
        }
        return envelopeFrom(envelope.metadata(), jsonObjectBuilder.build());
    }
}
