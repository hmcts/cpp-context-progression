package uk.gov.moj.cpp.progression.service;

import static com.azure.core.util.BinaryData.fromBytes;
import static com.azure.core.util.Context.NONE;
import static java.util.Map.of;
import static java.util.UUID.randomUUID;

import uk.gov.moj.cpp.progression.blobstore.AzureBlobConfiguration;

import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.options.BlobParallelUploadOptions;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

import javax.inject.Inject;
import javax.json.JsonObject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@SuppressWarnings("squid:S2139")
public class FileService {

    private static final Logger LOGGER = LoggerFactory.getLogger(FileService.class);

    @Inject
    private BlobContainerClient blobContainerClient;

    @Inject
    private AzureBlobConfiguration configuration;

    public UUID storePayload(final JsonObject payload, final String fileName, final String templateName) {
        final byte[] jsonPayloadInBytes = payload.toString().getBytes(StandardCharsets.UTF_8);
        final UUID fileId = randomUUID();
        try {
            blobContainerClient.getBlobClient(fileId.toString())
                    .uploadWithResponse(
                            new BlobParallelUploadOptions(fromBytes(jsonPayloadInBytes))
                                    .setMetadata(of(
                                            "fileName", fileName.strip(),
                                            "templateName", templateName,
                                            "conversionFormat", ConversionFormat.PDF.toString())),
                            configuration.getTransferTimeout(), NONE);
        } catch (final Exception e) {
            LOGGER.error("failed to store json payload into blob store", e);
            throw new RuntimeException(e.getMessage(), e);
        }
        return fileId;
    }
}
