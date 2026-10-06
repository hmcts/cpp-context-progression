package uk.gov.moj.cpp.progression.command.service;

import static java.util.Collections.emptyList;
import static java.util.Optional.ofNullable;
import static java.util.stream.Collectors.toList;
import static uk.gov.justice.services.core.annotation.Component.COMMAND_API;
import static uk.gov.justice.services.core.enveloper.Enveloper.envelop;
import static uk.gov.justice.services.messaging.JsonObjects.createObjectBuilder;

import uk.gov.justice.core.courts.CourtApplication;
import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.requester.Requester;
import uk.gov.justice.services.messaging.Envelope;

import java.util.List;
import java.util.UUID;

import javax.inject.Inject;
import javax.json.JsonObject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HearingQueryService {

    private static final Logger LOGGER = LoggerFactory.getLogger(HearingQueryService.class);
    private static final String HEARING_QUERY_GET_HEARING = "hearing.get.hearing";
    private static final String HEARING_ID = "hearingId";
    private static final String HEARING = "hearing";
    private static final String COURT_APPLICATIONS = "courtApplications";

    @Inject
    @ServiceComponent(COMMAND_API)
    private Requester requester;

    @Inject
    private JsonObjectToObjectConverter jsonObjectToObjectConverter;

    /**
     * Returns the court applications listed on the hearing; empty when the hearing has no
     * applications or is not found.
     */
    public List<CourtApplication> getCourtApplications(final UUID hearingId, final Envelope<?> envelope) {
        final JsonObject payload = createObjectBuilder().add(HEARING_ID, hearingId.toString()).build();
        final Envelope<JsonObject> requestEnvelope = envelop(payload)
                .withName(HEARING_QUERY_GET_HEARING).withMetadataFrom(envelope);

        LOGGER.info("Calling {} for hearingId {}", HEARING_QUERY_GET_HEARING, hearingId);
        final Envelope<JsonObject> response = requester.requestAsAdmin(requestEnvelope, JsonObject.class);

        return ofNullable(response.payload())
                .filter(json -> json.containsKey(HEARING))
                .map(json -> json.getJsonObject(HEARING))
                .filter(hearing -> hearing.containsKey(COURT_APPLICATIONS))
                .map(hearing -> hearing.getJsonArray(COURT_APPLICATIONS).getValuesAs(JsonObject.class).stream()
                        .map(courtApplication -> jsonObjectToObjectConverter.convert(courtApplication, CourtApplication.class))
                        .collect(toList()))
                .orElse(emptyList());
    }
}
