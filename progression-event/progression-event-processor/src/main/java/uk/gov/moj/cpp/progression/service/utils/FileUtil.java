package uk.gov.moj.cpp.progression.service.utils;

import com.azure.storage.blob.BlobContainerClient;

import java.util.UUID;

import javax.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FileUtil {

    private static final Logger LOGGER = LoggerFactory.getLogger(FileUtil.class.getCanonicalName());

    @Inject
    private BlobContainerClient blobContainerClient;

    public String retrieveFileName(final UUID fileId) {
        try {
            return blobContainerClient.getBlobClient(fileId.toString())
                    .getProperties()
                    .getMetadata()
                    .getOrDefault("fileName", StringUtils.EMPTY);
        } catch (final Exception e) {
            LOGGER.error("Exception while retrieving file name for blob '{}': {}", fileId, e.getMessage());
            return StringUtils.EMPTY;
        }
    }
}
