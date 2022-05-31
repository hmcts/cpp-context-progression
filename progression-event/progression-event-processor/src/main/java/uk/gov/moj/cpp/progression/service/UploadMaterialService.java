package uk.gov.moj.cpp.progression.service;

import uk.gov.justice.core.courts.MaterialDetails;
import uk.gov.justice.core.courts.RecordNowsMaterialRequest;
import uk.gov.justice.services.common.converter.ObjectToJsonObjectConverter;
import uk.gov.justice.services.core.enveloper.Enveloper;

import javax.inject.Inject;
import javax.json.JsonObject;

public class UploadMaterialService {

    public static final String PROGRESSION_COMMAND_RECORD_NOWS_MATERIAL_REQUEST = "progression.command.record-nows-material-request";

    @Inject
    private ObjectToJsonObjectConverter objectToJsonObjectConverter;

    @Inject
    private Enveloper enveloper;

    public void uploadFile(final UploadMaterialContext uploadMaterialContext) {
        final RecordNowsMaterialRequest recordNowsMaterialRequest = RecordNowsMaterialRequest.recordNowsMaterialRequest()
                .withContext(MaterialDetails.materialDetails()
                        .withMaterialId(uploadMaterialContext.getMaterialId())
                        .withFileId(uploadMaterialContext.getFileId())
                        .withHearingId(uploadMaterialContext.getHearingId())
                        .withUserId(uploadMaterialContext.getUserId())
                        .withCaseId(uploadMaterialContext.getCaseId())
                        .withApplicationId(uploadMaterialContext.getApplicationId())
                        .withFirstClassLetter(uploadMaterialContext.isFirstClassLetter())
                        .withSecondClassLetter(uploadMaterialContext.isSecondClassLetter())
                        .withEmailNotifications(uploadMaterialContext.getEmailNotifications())
                        .withIsNotificationApi(uploadMaterialContext.getIsNotificationApi())
                        .withIsCps(uploadMaterialContext.getIsCps())
                        .build())
                .build();
        final JsonObject payload = objectToJsonObjectConverter.convert(recordNowsMaterialRequest);
        uploadMaterialContext.getSender().send(enveloper.withMetadataFrom(uploadMaterialContext.getOriginatingEnvelope(), PROGRESSION_COMMAND_RECORD_NOWS_MATERIAL_REQUEST).apply(payload));
    }

}
