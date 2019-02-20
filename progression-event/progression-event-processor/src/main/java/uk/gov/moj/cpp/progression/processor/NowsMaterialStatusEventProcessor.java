package uk.gov.moj.cpp.progression.processor;

import static uk.gov.justice.services.core.annotation.Component.EVENT_PROCESSOR;

import uk.gov.justice.core.courts.NowsMaterialRequestRecorded;
import uk.gov.justice.core.courts.NowsMaterialStatusUpdated;
import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.progression.activiti.common.JsonHelper;
import uk.gov.moj.cpp.progression.nows.VariantSubscriptionProcessor;
import uk.gov.moj.cpp.progression.service.MaterialService;
import uk.gov.moj.cpp.progression.service.PrintService;

import java.util.Objects;
import java.util.UUID;

import javax.inject.Inject;
import javax.json.Json;
import javax.json.JsonObject;

@ServiceComponent(EVENT_PROCESSOR)
public class NowsMaterialStatusEventProcessor {

    public static final String RESULTS_UPDATE_NOWS_MATERIAL_STATUS = "results.update-nows-material-status";
    public static final String GENERATED_STATUS_VALUE = "generated";
    @Inject
    PrintService printService;
    @Inject
    private JsonObjectToObjectConverter jsonObjectToObjectConverter;
    @Inject
    private VariantSubscriptionProcessor variantSubscriptionProcessor;
    @Inject
    private Sender sender;
    @Inject
    private MaterialService materialService;

    @Handles("progression.event.nows-material-status-updated")
    public void processStatusUpdated(final JsonEnvelope event) {
        final NowsMaterialStatusUpdated nowsMaterialStatusUpdated = jsonObjectToObjectConverter.convert(event.payloadAsJsonObject(),
                NowsMaterialStatusUpdated.class);
        if (Objects.nonNull(nowsMaterialStatusUpdated.getDetails().getNowsNotificationDocumentState())) {
            variantSubscriptionProcessor.notifyVariantCreated(sender, event, nowsMaterialStatusUpdated.getDetails().getNowsNotificationDocumentState());
            notifyResultsContext(nowsMaterialStatusUpdated.getDetails().getUserId(),
                    nowsMaterialStatusUpdated.getDetails().getHearingId(),
                    nowsMaterialStatusUpdated.getDetails().getMaterialId());
        }
        if (nowsMaterialStatusUpdated.getDetails().getIsRemotePrintingRequired()) {
            printService.print(event, nowsMaterialStatusUpdated.getDetails().getCaseId(), UUID.randomUUID(), nowsMaterialStatusUpdated.getDetails().getMaterialId());
        }
    }

    @Handles("progression.event.nows-material-request-recorded")
    public void processRequestRecorded(final JsonEnvelope event) {
        final NowsMaterialRequestRecorded nowsMaterialRequestRecorded = jsonObjectToObjectConverter.convert(event.payloadAsJsonObject(), NowsMaterialRequestRecorded.class);
        materialService.uploadMaterial(nowsMaterialRequestRecorded.getContext().getFileId(), nowsMaterialRequestRecorded.getContext().getMaterialId(), event);
    }

    private void notifyResultsContext(final UUID userId, final UUID hearingId, final UUID materialId) {

        final JsonObject payload = Json.createObjectBuilder()
                .add("hearingId", hearingId.toString())
                .add("materialId", materialId.toString())
                .add("status", GENERATED_STATUS_VALUE).build();


        final JsonEnvelope postRequestEnvelope = JsonHelper.assembleEnvelopeWithPayloadAndMetaDetails(payload,
                RESULTS_UPDATE_NOWS_MATERIAL_STATUS, materialId.toString(), userId.toString());

        sender.send(postRequestEnvelope);
    }


}
