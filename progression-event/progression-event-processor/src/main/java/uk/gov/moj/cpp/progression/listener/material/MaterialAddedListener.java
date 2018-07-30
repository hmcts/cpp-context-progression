package uk.gov.moj.cpp.progression.listener.material;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import uk.gov.justice.services.core.annotation.Component;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.messaging.JsonEnvelope;

@ServiceComponent(Component.EVENT_PROCESSOR)
public class MaterialAddedListener {

    private static final Logger LOGGER =
                    LoggerFactory.getLogger(MaterialAddedListener.class.getName());


    @Handles("material.material-added")
    public void processEvent(final JsonEnvelope envelope) {

        LOGGER.info("Received MaterialAddedEvent {}", envelope);

    }
}
