package uk.gov.moj.cpp.progression.service;

import static uk.gov.justice.services.messaging.JsonObjects.createObjectBuilder;
import static uk.gov.justice.services.core.annotation.Component.EVENT_PROCESSOR;
import static uk.gov.justice.services.messaging.Envelope.envelopeFrom;
import static uk.gov.moj.cpp.progression.service.MetadataUtil.metadataWithNewActionName;

import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.requester.Requester;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.messaging.Metadata;

import java.util.UUID;

import javax.inject.Inject;
import javax.json.JsonObject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CorrespondenceService {

    private static final Logger LOGGER = LoggerFactory.getLogger(CorrespondenceService.class.getCanonicalName());

    private static final String CORRESPONDENCE_QUERY_CONTACTS = "correspondence.query.contacts";
    private static final String CASE_ID = "caseId";
    private static final String CASE_CONTEXT_NAME = "caseContext";

    //This value is hardcoded in UI while creating contact hence parameter value is fixed.
    private static final String CASE_CONTEXT_VALUE = "HMCTS";

    @Inject
    @ServiceComponent(EVENT_PROCESSOR)
    private Requester requester;


    public JsonObject getCaseContacts(final JsonEnvelope jsonEnvelope, final UUID caseId) {
        LOGGER.info("Getting contacts from correspondence for case {}", caseId);
        final Metadata metadata = metadataWithNewActionName(jsonEnvelope.metadata(), CORRESPONDENCE_QUERY_CONTACTS);
        final JsonObject jsonPayLoad = createObjectBuilder()
                .add(CASE_ID, caseId.toString())
                .add(CASE_CONTEXT_NAME, CASE_CONTEXT_VALUE)
                .build();

        return requester.request(envelopeFrom(metadata, jsonPayLoad), JsonObject.class).payload();
    }
}
