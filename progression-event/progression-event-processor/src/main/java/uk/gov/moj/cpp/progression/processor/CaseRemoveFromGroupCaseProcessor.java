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

import javax.inject.Inject;

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
}
