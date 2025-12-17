package uk.gov.moj.cpp.progression.processor;

import static uk.gov.justice.services.core.annotation.Component.EVENT_PROCESSOR;

import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.JsonEnvelope;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ServiceComponent(EVENT_PROCESSOR)
public class CaseRemovedFromGroupCasesProcessor {

    private static final Logger LOGGER = LoggerFactory.getLogger(CaseRemovedFromGroupCasesProcessor.class);

    @Inject
    private Sender sender;

    @Handles("progression.event.case-removed-from-group-cases")
    public void processEvent(final JsonEnvelope event) {
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("received event progression.event.case-removed-from-group-cases {} ", event.toObfuscatedDebugString());
        }

        sender.send(Enveloper.envelop(event.payloadAsJsonObject())
                .withName("public.progression.case-removed-from-group-cases")
                .withMetadataFrom(event));
    }
}
