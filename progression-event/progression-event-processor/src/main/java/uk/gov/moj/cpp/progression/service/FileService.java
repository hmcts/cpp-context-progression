package uk.gov.moj.cpp.progression.service;

import static javax.json.Json.createObjectBuilder;

import uk.gov.justice.services.fileservice.api.FileRetriever;
import uk.gov.justice.services.fileservice.api.FileServiceException;
import uk.gov.justice.services.fileservice.api.FileStorer;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.UUID;

import javax.inject.Inject;
import javax.json.Json;
import javax.json.JsonObject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@SuppressWarnings({"squid:S2139", "squid:S00112"})
public class FileService {

    private static final Logger LOGGER = LoggerFactory.getLogger(FileService.class);

    @Inject
    private FileStorer fileStorer;

    @Inject
    private FileRetriever fileRetriever;

    public UUID storePayload(final JsonObject payload, final String fileName, final String templateName) {
        try {
            final byte[] jsonPayloadInBytes = payload.toString().getBytes(StandardCharsets.UTF_8);

            final JsonObject metadata = createObjectBuilder()
                    .add("fileName", fileName)
                    .add("conversionFormat", ConversionFormat.PDF.toString())
                    .add("templateName", templateName)
                    .add("numberOfPages", 1)
                    .add("fileSize", jsonPayloadInBytes.length)
                    .build();

            return fileStorer.store(metadata, new ByteArrayInputStream(jsonPayloadInBytes));

        } catch (FileServiceException fileServiceException) {
            LOGGER.error("failed to store json payload metadata into file service", fileServiceException);
            throw new RuntimeException(fileServiceException.getMessage());
        }
    }

    public Optional<JsonObject> retrievePayload(final UUID fileId) {
        try {
            return fileRetriever.retrieve(fileId).map(ref -> {
                final JsonObject metadata = ref.getMetadata();
                if (metadata != null && metadata.containsKey("payloadFileServiceId")) {
                    final UUID jsonPayloadFileId = UUID.fromString(metadata.getString("payloadFileServiceId"));
                    LOGGER.info("fileId {} is a PDF; navigating to JSON payload via payloadFileServiceId {}", fileId, jsonPayloadFileId);
                    return retrieveJsonFromFileId(jsonPayloadFileId);
                }
                try (InputStream stream = ref.getContentStream()) {
                    final String json = new String(stream.readAllBytes(), StandardCharsets.UTF_8);
                    return Json.createReader(new StringReader(json)).readObject();
                } catch (java.io.IOException e) {
                    LOGGER.error("Failed to read content stream for fileId {}", fileId, e);
                    throw new RuntimeException(e);
                }
            });
        } catch (FileServiceException e) {
            LOGGER.error("Failed to retrieve payload from file service for fileId {}", fileId, e);
            throw new RuntimeException(e);
        }
    }

    private JsonObject retrieveJsonFromFileId(final UUID fileId) {
        try {
            return fileRetriever.retrieve(fileId)
                    .map(ref -> {
                        try (InputStream stream = ref.getContentStream()) {
                            final String json = new String(stream.readAllBytes(), StandardCharsets.UTF_8);
                            return Json.createReader(new StringReader(json)).readObject();
                        } catch (java.io.IOException e) {
                            LOGGER.error("Failed to read content stream for fileId {}", fileId, e);
                            throw new RuntimeException(e);
                        }
                    })
                    .orElseThrow(() -> new RuntimeException("No file found for fileId: " + fileId));
        } catch (FileServiceException e) {
            LOGGER.error("Failed to retrieve JSON payload for fileId {}", fileId, e);
            throw new RuntimeException(e);
        }
    }
}
