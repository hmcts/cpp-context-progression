package uk.gov.moj.cpp.progression.service;

import static javax.json.Json.createObjectBuilder;

import uk.gov.justice.services.fileservice.api.FileRetriever;
import uk.gov.justice.services.fileservice.api.FileServiceException;
import uk.gov.justice.services.fileservice.api.FileStorer;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.UUID;

import javax.inject.Inject;
import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonReader;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@SuppressWarnings({"squid:S2139", "squid:S00112"})
public class FileService {

    private static final Logger LOGGER = LoggerFactory.getLogger(FileService.class);
    public static final String PAYLOAD_FILE_SERVICE_ID = "payloadFileServiceId";

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
                if (metadata != null && metadata.containsKey(PAYLOAD_FILE_SERVICE_ID)) {
                    final UUID jsonPayloadFileId = UUID.fromString(metadata.getString(PAYLOAD_FILE_SERVICE_ID));
                    LOGGER.info("fileId {} is a PDF; navigating to JSON payload via payloadFileServiceId {}", fileId, jsonPayloadFileId);
                    return retrieveJsonFromFileId(jsonPayloadFileId);
                }
                return readJson(ref.getContentStream(), fileId);
            });
        } catch (FileServiceException fileServiceException) {
            LOGGER.error("Failed to retrieve payload from file service for fileId {}", fileId, fileServiceException);
            throw new RuntimeException(fileServiceException.getMessage());
        }
    }

    private JsonObject retrieveJsonFromFileId(final UUID fileId) {
        try {
            return fileRetriever.retrieve(fileId)
                    .map(ref -> readJson(ref.getContentStream(), fileId))
                    .orElseThrow(() -> new RuntimeException("No file found for fileId: " + fileId));
        } catch (FileServiceException fileServiceException) {
            LOGGER.error("Failed to retrieve JSON payload for fileId {}", fileId, fileServiceException);
            throw new RuntimeException(fileServiceException.getMessage());
        }
    }

    public Optional<String> retrieveRawPayload(final UUID fileId) {
        try {
            return fileRetriever.retrieve(fileId).map(ref -> {
                final JsonObject metadata = ref.getMetadata();
                if (metadata != null && metadata.containsKey(PAYLOAD_FILE_SERVICE_ID)) {
                    final UUID jsonPayloadFileId = UUID.fromString(metadata.getString(PAYLOAD_FILE_SERVICE_ID));
                    LOGGER.info("fileId {} is a PDF; navigating to JSON payload via payloadFileServiceId {}", fileId, jsonPayloadFileId);
                    return retrieveRawStringFromFileId(jsonPayloadFileId);
                }
                return readRawString(ref.getContentStream(), fileId);
            });
        } catch (FileServiceException fileServiceException) {
            LOGGER.error("Failed to retrieve raw payload from file service for fileId {}", fileId, fileServiceException);
            throw new RuntimeException(fileServiceException.getMessage());
        }
    }

    private String retrieveRawStringFromFileId(final UUID fileId) {
        try {
            return fileRetriever.retrieve(fileId)
                    .map(ref -> readRawString(ref.getContentStream(), fileId))
                    .orElseThrow(() -> new RuntimeException("No file found for fileId: " + fileId));
        } catch (FileServiceException fileServiceException) {
            LOGGER.error("Failed to retrieve raw JSON for fileId {}", fileId, fileServiceException);
            throw new RuntimeException(fileServiceException.getMessage());
        }
    }

    private String readRawString(final InputStream stream, final UUID fileId) {
        try (final InputStream is = stream) {
            return new String(is.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            LOGGER.error("Failed to read content stream for fileId {}", fileId, e);
            throw new RuntimeException(e);
        }
    }

    private JsonObject readJson(final InputStream stream, final UUID fileId) {
        try (final InputStream is = stream;
             final JsonReader reader = Json.createReader(new StringReader(new String(is.readAllBytes(), StandardCharsets.UTF_8)))) {
            return reader.readObject();
        } catch (IOException e) {
            LOGGER.error("Failed to read content stream for fileId {}", fileId, e);
            throw new RuntimeException(e);
        }
    }
}
