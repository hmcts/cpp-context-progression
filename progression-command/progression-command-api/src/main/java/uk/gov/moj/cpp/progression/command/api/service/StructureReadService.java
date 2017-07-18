package uk.gov.moj.cpp.progression.command.api.service;


import static uk.gov.justice.services.messaging.DefaultJsonEnvelope.envelopeFrom;

import uk.gov.justice.services.core.annotation.Component;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.requester.Requester;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.messaging.JsonObjectMetadata;
import uk.gov.justice.services.messaging.Metadata;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonValue;



public class StructureReadService {

    public static final String DEF_ID = "id";
    public static final String CASE_ID = "caseId";
    public static final String DEFENDANTS = "defendants";
    public static final String GET_CASE_DEFENDANT_QUERY = "structure.query.case-defendants";

    @Inject
    @ServiceComponent(Component.COMMAND_API)
    private Requester requester;

    public List<String> getStructureCaseDefendantsId(final String caseId, final String userId) {

        Metadata metadata = JsonObjectMetadata.metadataOf(UUID.randomUUID(), GET_CASE_DEFENDANT_QUERY)
                        .withUserId(userId)
                        .build();
        final JsonEnvelope requestEnvelope = envelopeFrom(metadata, buildRequestPayload(caseId));


        JsonEnvelope responseEnvelope = requester.request(requestEnvelope);

        if (JsonValue.NULL.equals(responseEnvelope.payload())) {
            return new ArrayList<>();
        }

        JsonObject payload = responseEnvelope.payloadAsJsonObject();
        return payload.getJsonArray(DEFENDANTS).stream()
                        .map(s -> ((JsonObject) s).getString(DEF_ID)).collect(Collectors.toList());
    }

    private JsonObject buildRequestPayload(final String caseId) {
        return Json.createObjectBuilder().add(CASE_ID, caseId).build();
    }



}
