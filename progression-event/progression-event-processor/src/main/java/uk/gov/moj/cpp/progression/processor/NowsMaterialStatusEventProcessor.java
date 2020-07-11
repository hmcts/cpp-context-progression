package uk.gov.moj.cpp.progression.processor;

import static java.util.Objects.nonNull;
import static java.util.Optional.ofNullable;
import static uk.gov.justice.services.core.annotation.Component.EVENT_PROCESSOR;

import uk.gov.justice.core.courts.MaterialDetails;
import uk.gov.justice.core.courts.NowsMaterialRequestRecorded;
import uk.gov.justice.core.courts.NowsMaterialStatusUpdated;
import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.progression.service.MaterialService;
import uk.gov.moj.cpp.progression.service.NotificationService;

import java.util.UUID;

import javax.inject.Inject;

@ServiceComponent(EVENT_PROCESSOR)
public class NowsMaterialStatusEventProcessor {

    public static final String GENERATED_STATUS_VALUE = "generated";

    @Inject
    private NotificationService notificationService;

    @Inject
    private JsonObjectToObjectConverter jsonObjectToObjectConverter;

    @Inject
    private MaterialService materialService;

    @Handles("progression.event.nows-material-status-updated")
    public void processStatusUpdated(final JsonEnvelope event) {

        final NowsMaterialStatusUpdated nowsMaterialStatusUpdated = jsonObjectToObjectConverter.convert(event.payloadAsJsonObject(), NowsMaterialStatusUpdated.class);

        ofNullable(nowsMaterialStatusUpdated).map(NowsMaterialStatusUpdated::getDetails)
                .filter(materialDetails -> nonNull(materialDetails.getEmailNotifications()))
                .ifPresent(materialDetails ->
                        notificationService.sendEmail(event, UUID.randomUUID(), materialDetails.getCaseId(), materialDetails.getApplicationId(), materialDetails.getMaterialId(), materialDetails.getEmailNotifications(), null));

        ofNullable(nowsMaterialStatusUpdated).map(NowsMaterialStatusUpdated::getDetails)
                .filter(MaterialDetails::getSecondClassLetter)
                .ifPresent(materialDetails -> notificationService.sendLetter(event, UUID.randomUUID(), materialDetails.getCaseId(), materialDetails.getApplicationId(), materialDetails.getMaterialId(), false));

        ofNullable(nowsMaterialStatusUpdated).map(NowsMaterialStatusUpdated::getDetails)
                .filter(MaterialDetails::getFirstClassLetter)
                .ifPresent(materialDetails -> notificationService.sendLetter(event, UUID.randomUUID(), materialDetails.getCaseId(), materialDetails.getApplicationId(), materialDetails.getMaterialId(), true));
    }

    @Handles("progression.event.nows-material-request-recorded")
    public void processRequestRecorded(final JsonEnvelope event) {
        final NowsMaterialRequestRecorded nowsMaterialRequestRecorded = jsonObjectToObjectConverter.convert(event.payloadAsJsonObject(), NowsMaterialRequestRecorded.class);
        materialService.uploadMaterial(nowsMaterialRequestRecorded.getContext().getFileId(), nowsMaterialRequestRecorded.getContext().getMaterialId(), event);
    }

}
