package uk.gov.moj.cpp.progression.service;


import static uk.gov.justice.services.messaging.Envelope.envelopeFrom;
import static uk.gov.moj.cpp.progression.service.MetadataUtil.metadataWithNewActionName;

import uk.gov.justice.core.courts.ProsecutionCase;
import uk.gov.justice.services.core.annotation.Component;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.requester.Requester;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.messaging.Metadata;

import java.util.UUID;

import javax.inject.Inject;
import javax.json.Json;
import javax.json.JsonObject;

public class SjpService {

    private static final String GET_SJP_PROSECUTION_CASE =  "sjp.query.prosecution-case";

    @ServiceComponent(Component.EVENT_PROCESSOR)
    @Inject
    private Requester requester;

    public ProsecutionCase getProsecutionCase(final JsonEnvelope envelope, final UUID caseId){

        final Metadata metadata = metadataWithNewActionName(envelope.metadata(), GET_SJP_PROSECUTION_CASE);
        final JsonObject jsonPayLoad = Json.createObjectBuilder()
                .add("caseId", caseId.toString())
                .build();
        return requester.requestAsAdmin(envelopeFrom(metadata, jsonPayLoad), ProsecutionCase.class).payload();
    }
}
