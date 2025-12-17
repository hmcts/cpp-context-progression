package uk.gov.moj.cpp.progression.listener.material;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.gov.justice.core.courts.UpdateNowsMaterialStatus;
import uk.gov.justice.services.common.converter.ObjectToJsonObjectConverter;
import uk.gov.justice.services.core.annotation.Component;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.progression.Originator;

import javax.inject.Inject;
import java.util.UUID;

import static uk.gov.moj.cpp.progression.processor.NowsMaterialStatusEventProcessor.GENERATED_STATUS_VALUE;

@ServiceComponent(Component.EVENT_PROCESSOR)
public class MaterialAddedProcessor {

    public static final String MATERIAL_ID = "materialId";
    public static final String PROGRESSION_COMMAND_UPDATE_NOWS_MATERIAL_STATUS = "progression.command.update-nows-material-status";
    public static final String ORIGINATOR = Originator.SOURCE;
    public static final String ORIGINATOR_VALUE = Originator.ORIGINATOR_VALUE;
    private static final Logger LOGGER =
            LoggerFactory.getLogger(MaterialAddedProcessor.class.getName());
    @Inject
    private Sender sender;

    @Inject
    private Enveloper enveloper;

    @Inject
    private ObjectToJsonObjectConverter objectToJsonObjectConverter;

    @Handles("material.material-added")
    public void processEvent(final JsonEnvelope event) {
        LOGGER.info("Received MaterialAddedEvent {}", event);
        if (event.metadata().asJsonObject().containsKey(ORIGINATOR)
                && ORIGINATOR_VALUE.equalsIgnoreCase(event.metadata().asJsonObject().getString(ORIGINATOR))) {
            processNowsMaterialNotificationRequest(event);
        }
    }
    @SuppressWarnings("squid:CallToDeprecatedMethod")
    private void processNowsMaterialNotificationRequest(JsonEnvelope event) {
        final UUID materialId = UUID.fromString(event.payloadAsJsonObject().getString(MATERIAL_ID));
        final UpdateNowsMaterialStatus updateNowsMaterialStatusCommand = UpdateNowsMaterialStatus.updateNowsMaterialStatus()
                .withStatus(GENERATED_STATUS_VALUE)
                .withMaterialId(materialId)
                .build();
        this.sender.send(this.enveloper.withMetadataFrom(event, PROGRESSION_COMMAND_UPDATE_NOWS_MATERIAL_STATUS)
                .apply(this.objectToJsonObjectConverter.convert(updateNowsMaterialStatusCommand)));
    }

}
