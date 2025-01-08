package uk.gov.moj.cpp.progression.service;

import static javax.json.Json.createObjectBuilder;

import uk.gov.justice.services.fileservice.api.FileServiceException;
import uk.gov.justice.services.fileservice.api.FileStorer;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

import javax.inject.Inject;
import javax.json.JsonObject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@SuppressWarnings({"squid:S2139", "squid:S00112"})
public class FileService {

    private static final Logger LOGGER = LoggerFactory.getLogger(FileService.class);

    @Inject
    private FileStorer fileStorer;

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
}
