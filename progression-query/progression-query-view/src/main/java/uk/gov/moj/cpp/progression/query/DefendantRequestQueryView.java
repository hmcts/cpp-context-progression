package uk.gov.moj.cpp.progression.query;

import uk.gov.justice.services.common.converter.StringToJsonObjectConverter;
import uk.gov.justice.services.core.annotation.Component;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.messaging.JsonObjects;
import uk.gov.moj.cpp.prosecutioncase.persistence.entity.DefendantRequestEntity;
import uk.gov.moj.cpp.prosecutioncase.persistence.repository.DefendantRequestRepository;

import java.util.Optional;
import java.util.UUID;

import javax.inject.Inject;
import javax.json.JsonObject;

@SuppressWarnings({"squid:S3655"})
@ServiceComponent(Component.QUERY_VIEW)
public class DefendantRequestQueryView {

    private static final String ID = "defendantId";

    @Inject
    private DefendantRequestRepository defendantRequestRepository;

    @Inject
    private StringToJsonObjectConverter stringToJsonObjectConverter;


    @Handles("progression.query.defendant-request")
    public JsonEnvelope getDefendantRequest(final JsonEnvelope envelope) {
        final Optional<UUID> defendantId = JsonObjects.getUUID(envelope.payloadAsJsonObject(), ID);
        final DefendantRequestEntity defendantRequestEntity = defendantRequestRepository.findBy(defendantId.get());
        final JsonObject defendantRequest = stringToJsonObjectConverter.convert(defendantRequestEntity.getPayload());
        return JsonEnvelope.envelopeFrom(envelope.metadata(), defendantRequest);
    }

}
