package uk.gov.moj.cpp.progression.service;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.core.requester.Requester;
import uk.gov.justice.services.messaging.JsonEnvelope;

import javax.inject.Inject;
import javax.json.JsonArray;
import javax.json.JsonObject;
import java.util.Optional;

import static uk.gov.justice.services.messaging.JsonObjects.createObjectBuilder;
import static uk.gov.justice.services.core.annotation.Component.COMMAND_HANDLER;

public class LegalStatusReferenceDataService {
    private static final Logger LOGGER = LoggerFactory.getLogger(LegalStatusReferenceDataService.class);
    private static final String REFERENCEDATA_QUERY_LEGAL_STATUSES = "referencedata.query.legal-statuses";

    @Inject
    private Enveloper enveloper;

    @Inject
    @ServiceComponent(COMMAND_HANDLER)
    private Requester requester;

    public Optional<JsonObject> getLegalStatusByStatusIdAndStatusCode(final JsonEnvelope event,  final String statusCode) {
        if(LOGGER.isDebugEnabled()) {
            LOGGER.debug("Reference Data Query payload to get Legal Status {}" , event.toObfuscatedDebugString());
        }
        final JsonEnvelope request = enveloper.withMetadataFrom(event, REFERENCEDATA_QUERY_LEGAL_STATUSES).apply(createObjectBuilder().build());
        final JsonEnvelope actualLegalStatuses = requester.request(request);
        final JsonArray legalStatusArray = actualLegalStatuses.payloadAsJsonObject().getJsonArray("legalStatuses");
        return legalStatusArray.stream()
                .map(legalStatus->(JsonObject)legalStatus)
                .filter(legalStatusJsonObject-> legalStatusJsonObject.getString("statusCode").equals(statusCode)).findFirst();

    }
}
