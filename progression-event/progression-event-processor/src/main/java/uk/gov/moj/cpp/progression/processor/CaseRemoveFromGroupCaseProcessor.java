package uk.gov.moj.cpp.progression.processor;

import static uk.gov.justice.services.messaging.JsonObjects.createObjectBuilder;
import static uk.gov.justice.services.core.annotation.Component.EVENT_PROCESSOR;

import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.progression.events.LastCaseToBeRemovedFromGroupCasesRejected;
import uk.gov.moj.cpp.progression.events.LastCasesToBeMovedFromGroupRejected;

import java.util.UUID;

import javax.inject.Inject;
import javax.json.Json;
import javax.json.JsonArrayBuilder;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ServiceComponent(EVENT_PROCESSOR)
public class CaseRemoveFromGroupCaseProcessor {

    private static final Logger LOGGER = LoggerFactory.getLogger(CaseRemoveFromGroupCaseProcessor.class);

    @Inject
    @ServiceComponent(EVENT_PROCESSOR)
    private Sender sender;

    @Inject
    private JsonObjectToObjectConverter jsonObjectConverter;

    @Handles("progression.event.last-case-to-be-removed-from-group-cases-rejected")
    public void processEvent(final JsonEnvelope event) {
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("received event progression.event.last-case-to-be-removed-from-group-cases-rejected {} ", event.toObfuscatedDebugString());
        }
        final LastCaseToBeRemovedFromGroupCasesRejected lastCaseToBeRemovedFromGroupCasesRejected = jsonObjectConverter.convert(event.payloadAsJsonObject(), LastCaseToBeRemovedFromGroupCasesRejected.class);
        sender.send(Enveloper.envelop(createObjectBuilder().add("groupId", lastCaseToBeRemovedFromGroupCasesRejected.getGroupId().toString())
                                                            .add("caseId", lastCaseToBeRemovedFromGroupCasesRejected.getCaseId().toString())
                                                            .build())
                .withName("public.progression.remove-last-case-in-group-cases-rejected")
                .withMetadataFrom(event));
    }

    @Handles("progression.event.last-cases-to-be-moved-from-group-rejected")
    public void processMoveRejectedEvent(final JsonEnvelope event) {
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("received event progression.event.last-cases-to-be-moved-from-group-rejected {} ", event.toObfuscatedDebugString());
        }
        final LastCasesToBeMovedFromGroupRejected lastCasesToBeMovedFromGroupRejected = jsonObjectConverter.convert(event.payloadAsJsonObject(), LastCasesToBeMovedFromGroupRejected.class);
        final JsonArrayBuilder caseIdsArrayBuilder = Json.createArrayBuilder();
        for (final UUID caseId : lastCasesToBeMovedFromGroupRejected.getCaseIds()) {
            caseIdsArrayBuilder.add(caseId.toString());
        }
        sender.send(Enveloper.envelop(createObjectBuilder().add("groupId", lastCasesToBeMovedFromGroupRejected.getGroupId().toString())
                                                            .add("caseIds", caseIdsArrayBuilder.build())
                                                            .build())
                .withName("public.progression.move-last-cases-from-group-rejected")
                .withMetadataFrom(event));
    }
}
