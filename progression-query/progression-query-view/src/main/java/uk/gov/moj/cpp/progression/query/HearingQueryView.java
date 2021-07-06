package uk.gov.moj.cpp.progression.query;

import static java.util.Collections.emptyList;
import static java.util.Objects.isNull;
import static java.util.stream.Collectors.toList;

import uk.gov.justice.core.courts.Hearing;
import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.common.converter.StringToJsonObjectConverter;
import uk.gov.justice.services.core.annotation.Component;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.messaging.JsonObjects;
import uk.gov.moj.cpp.prosecutioncase.persistence.entity.HearingEntity;
import uk.gov.moj.cpp.prosecutioncase.persistence.repository.HearingRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import javax.inject.Inject;
import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@SuppressWarnings({"squid:S3655"})
@ServiceComponent(Component.QUERY_VIEW)
public class HearingQueryView {

    private static final String ID = "hearingId";
    private static final Logger LOGGER = LoggerFactory.getLogger(HearingQueryView.class);
    @Inject
    private HearingRepository hearingRepository;

    @Inject
    private StringToJsonObjectConverter stringToJsonObjectConverter;

    @Inject
    private JsonObjectToObjectConverter jsonObjectToObjectConverter;


    @Handles("progression.query.hearing")
    public JsonEnvelope getHearing(final JsonEnvelope envelope) {
        final JsonObjectBuilder jsonObjectBuilder = Json.createObjectBuilder();
        final Optional<UUID> hearingId = JsonObjects.getUUID(envelope.payloadAsJsonObject(), ID);


        final HearingEntity hearingRequestEntity = hearingRepository.findBy(hearingId.get());
        if (isNull(hearingRequestEntity)) {
            LOGGER.info("### No hearing found with hearingId='{}'", hearingId);
            return JsonEnvelope.envelopeFrom(
                    envelope.metadata(),
                    jsonObjectBuilder.build());
        }
        final JsonObject hearingRequest = stringToJsonObjectConverter.convert(hearingRequestEntity.getPayload());
        jsonObjectBuilder.add("hearing", hearingRequest);
        jsonObjectBuilder.add("hearingListingStatus", hearingRequestEntity.getListingStatus().name());

        return JsonEnvelope.envelopeFrom(
                envelope.metadata(),
                jsonObjectBuilder.build());
    }

    public List<Hearing> getHearings(final List<UUID> hearingIds) {

        final List<HearingEntity> hearingEntities = hearingRepository.findByHearingIds(hearingIds);
        if (isNull(hearingEntities)) {
            LOGGER.info("### No hearing found with hearingIds='{}'", hearingIds);
            return emptyList();
        }
        return hearingEntities.stream()
                .map(hearingEntity -> stringToJsonObjectConverter.convert(hearingEntity.getPayload()))
                .map(jsonObject -> jsonObjectToObjectConverter.convert(jsonObject, Hearing.class))
                .collect(toList());
    }
}
