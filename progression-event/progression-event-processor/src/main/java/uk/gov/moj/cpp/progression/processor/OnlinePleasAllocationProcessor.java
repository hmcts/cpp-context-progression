package uk.gov.moj.cpp.progression.processor;

import static uk.gov.justice.services.core.annotation.Component.EVENT_PROCESSOR;
import static uk.gov.justice.services.messaging.Envelope.envelopeFrom;
import static uk.gov.justice.services.messaging.Envelope.metadataFrom;

import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.featurecontrol.FeatureControlGuard;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.JsonEnvelope;

import javax.inject.Inject;

@ServiceComponent(EVENT_PROCESSOR)
public class OnlinePleasAllocationProcessor {

    private static final String FEATURE_OPA = "OPA";
    private static final String PROGRESSION_COMMAND_ALLOCATION_PLEAS_ADDED = "progression.command.add-online-plea-allocation";
    private static final String PROGRESSION_COMMAND_ALLOCATION_PLEAS_UPDATED = "progression.command.update-online-plea-allocation";

    @Inject
    private FeatureControlGuard featureControlGuard;

    @Inject
    private Sender sender;

    @Handles("public.defence.allocation-pleas-added")
    public void defenceOnlinePleaAllocationAdded(final JsonEnvelope envelope) {
        if (featureControlGuard.isFeatureEnabled(FEATURE_OPA)) {
            sender.send(envelopeFrom(metadataFrom(envelope.metadata()).withName(PROGRESSION_COMMAND_ALLOCATION_PLEAS_ADDED),
                    envelope.payloadAsJsonObject()));
        }

    }

    @Handles("public.defence.allocation-pleas-updated")
    public void defenceOnlinePleaAllocationUpdated(final JsonEnvelope envelope) {
        if (featureControlGuard.isFeatureEnabled(FEATURE_OPA)) {
            sender.send(envelopeFrom(metadataFrom(envelope.metadata()).withName(PROGRESSION_COMMAND_ALLOCATION_PLEAS_UPDATED),
                    envelope.payloadAsJsonObject()));
        }
    }
}
