package uk.gov.moj.cpp.progression.service;

import static java.lang.String.format;
import static java.util.Objects.isNull;
import static java.util.UUID.fromString;
import static java.util.concurrent.TimeUnit.SECONDS;
import static javax.json.Json.createObjectBuilder;
import static org.apache.commons.lang3.StringUtils.EMPTY;
import static org.awaitility.Awaitility.with;
import static uk.gov.justice.services.core.annotation.Component.EVENT_PROCESSOR;
import static uk.gov.justice.services.messaging.Envelope.metadataFrom;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.moj.cpp.progression.Originator.assembleEnvelopeWithPayloadAndMetaDetails;
import static uk.gov.moj.cpp.progression.service.MetadataUtil.metadataWithNewActionName;

import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.core.requester.Requester;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.messaging.Metadata;
import uk.gov.moj.cpp.material.client.MaterialClient;
import uk.gov.moj.cpp.progression.processor.exceptions.MaterialNotFoundException;

import java.io.IOException;
import java.io.InputStream;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import javax.inject.Inject;
import javax.json.Json;
import javax.json.JsonObject;
import javax.ws.rs.core.Response;

import org.apache.tika.io.IOUtils;
import org.awaitility.core.ConditionTimeoutException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@SuppressWarnings({"squid:S2139", "squid:S00112", "squid:S2142"})
public class MaterialService {
    protected static final String UPLOAD_MATERIAL = "material.command.upload-file";
    protected static final String MATERIAL_METADETA_QUERY = "material.query.material-metadata";
    protected static final String MATERIAL_STRUCTURED_FORM_QUERY = "material.query.structured-form";
    private static final Logger LOGGER = LoggerFactory.getLogger(MaterialService.class.getCanonicalName());
    private static final String FIELD_MATERIAL_ID = "materialId";
    private static final String FILE_NAME = "fileName";
    private static final String FIELD_STRUCTURED_FORM_ID = "structuredFormId";
    @Inject
    @ServiceComponent(EVENT_PROCESSOR)
    private Sender sender;

    @Inject
    @ServiceComponent(EVENT_PROCESSOR)
    private Requester requester;

    @Inject
    private Enveloper enveloper;

    @Inject
    private MaterialClient materialClient;

    public void uploadMaterial(final UUID fileServiceId, final UUID materialId, final JsonEnvelope envelope) {
        final UUID userId = fromString(envelope.metadata().userId().orElseThrow(() -> new RuntimeException("UserId missing from event.")));
        uploadMaterial(fileServiceId, materialId, userId);
    }

    public void uploadMaterial(final UUID fileServiceId, final UUID materialId, final UUID userId) {
        if (isNull(userId)) {
            throw new RuntimeException("UserId missing from event.");
        }
        LOGGER.info("material being uploaded '{}' file service id '{}'", materialId, fileServiceId);
        final JsonObject uploadMaterialPayload = Json.createObjectBuilder()
                .add(FIELD_MATERIAL_ID, materialId.toString())
                .add("fileServiceId", fileServiceId.toString())
                .build();

        LOGGER.info("requesting material service to upload file id {} for material {}", fileServiceId, materialId);

        sender.send(assembleEnvelopeWithPayloadAndMetaDetails(uploadMaterialPayload, UPLOAD_MATERIAL, userId.toString()));
    }

    public Optional<JsonObject> getMaterialMetadata(final JsonEnvelope envelope, final UUID materialId) {

        final JsonObject requestParameter = createObjectBuilder()
                .add(FIELD_MATERIAL_ID, materialId.toString()).build();

        LOGGER.info("materialId {} material metadata request ", materialId);


        final JsonEnvelope materialMetadata = requester.requestAsAdmin(enveloper
                .withMetadataFrom(envelope, MATERIAL_METADETA_QUERY)
                .apply(requestParameter));

        if (LOGGER.isInfoEnabled()) {
            LOGGER.info("materialId {} material metadata ref data payload {}", materialId, materialMetadata.toObfuscatedDebugString());
        }
        return Optional.ofNullable(materialMetadata.payloadAsJsonObject());
    }

    public String getMaterialMetadataV2(final JsonEnvelope envelope, final UUID materialId) {
        LOGGER.info("Material metadata requesting with materialId {}", materialId);
        final JsonObject requestParameter = createObjectBuilder().add(FIELD_MATERIAL_ID, materialId.toString()).build();
        final Metadata metadata = metadataWithNewActionName(envelope.metadata(), MATERIAL_METADETA_QUERY);

        final AtomicReference<JsonObject> materialMetadataView = new AtomicReference<>();
        with()
                .ignoreException(ConditionTimeoutException.class)
                .pollInterval(3, SECONDS)
                .atMost(15, SECONDS)
                .await()
                .until(() -> {
                    materialMetadataView.set(requester.requestAsAdmin(envelopeFrom(metadata, requestParameter), JsonObject.class).payload());
                    return null != materialMetadataView.get();
                });

        if (isNull(materialMetadataView.get())) {
            throw new MaterialNotFoundException(format("Material with materialId %s is not found so will be retrying", materialId));
        }
        LOGGER.info("Material metadata got response with materialId {} and payload {}", materialId, materialMetadataView);
        return materialMetadataView.get().getString(FILE_NAME, EMPTY);
    }

    public byte[] getDocumentContent(final UUID materialId, final UUID userId) {
        LOGGER.info("material context is invoking to get material");
        final Response documentContentResponse = materialClient.getMaterial(fromString(materialId.toString()), userId);
        final InputStream inputStream = documentContentResponse.readEntity(InputStream.class);
        try {
            return IOUtils.toByteArray(inputStream);
        } catch (final IOException ioException) {
            LOGGER.info("Exception in converting inputstream to bytes {}", ioException);
            throw new RuntimeException("Converting inputStream to bytes failed", ioException);
        }
    }

    public Optional<JsonObject> getStructuredForm(final JsonEnvelope envelope, final UUID structuredFormId) {

        final JsonObject requestParameter = createObjectBuilder()
                .add(FIELD_STRUCTURED_FORM_ID, structuredFormId.toString()).build();

        LOGGER.info("structuredFormId {} material structured form request ", structuredFormId);

        final JsonEnvelope structuredForm = requester.request(envelopeFrom(metadataFrom(envelope.metadata())
                .withName(MATERIAL_STRUCTURED_FORM_QUERY), requestParameter));

        return Optional.ofNullable(structuredForm.payloadAsJsonObject());
    }
}
