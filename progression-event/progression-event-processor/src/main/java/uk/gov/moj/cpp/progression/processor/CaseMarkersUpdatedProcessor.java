package uk.gov.moj.cpp.progression.processor;

import static uk.gov.justice.services.core.annotation.Component.EVENT_PROCESSOR;

import uk.gov.justice.core.courts.CaseMarkersUpdated;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.JsonEnvelope;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ServiceComponent(EVENT_PROCESSOR)
@SuppressWarnings({"squid:CommentedOutCodeLine", "squid:S2789", "squid:CallToDeprecatedMethod"})
public class CaseMarkersUpdatedProcessor {
    private static final String CASE_MARKER_UPDATED = "public.progression.case-markers-updated";
    private static final Logger LOGGER = LoggerFactory.getLogger(CaseMarkersUpdated.class);

    @Inject
    private Sender sender;

    @Inject
    private Enveloper enveloper;

    @Handles("progression.event.case-markers-updated")
    public void processCaseMarkerUpdated(final JsonEnvelope event) {
        if (LOGGER.isInfoEnabled()) {
            LOGGER.info("Received '{}' event with payload {}", "progression.event.case-markers-updated", event.toObfuscatedDebugString());
        }
        sender.send(enveloper.withMetadataFrom(event, CASE_MARKER_UPDATED).apply(event.payloadAsJsonObject()));
    }
}
