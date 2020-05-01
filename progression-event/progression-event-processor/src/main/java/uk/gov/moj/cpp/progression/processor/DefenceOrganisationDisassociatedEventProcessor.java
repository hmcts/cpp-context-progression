package uk.gov.moj.cpp.progression.processor;

import uk.gov.justice.services.common.converter.ObjectToJsonObjectConverter;
import uk.gov.justice.services.core.annotation.Component;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.JsonEnvelope;

import javax.inject.Inject;

@ServiceComponent(Component.EVENT_PROCESSOR)
public class DefenceOrganisationDisassociatedEventProcessor {

    @Inject
    private Sender sender;

    @Inject
    private Enveloper enveloper;

    @Inject
    private ObjectToJsonObjectConverter objectToJsonObjectConverter;

    @Handles("progression.event.defence-organisation-disassociated")
    public void processEvent(final JsonEnvelope event) {
        sender.send(Enveloper.envelop(event.payloadAsJsonObject())
                .withName("public.defence.defence-organisation-disassociated")
                .withMetadataFrom(event));
    }

}
