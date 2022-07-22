package uk.gov.moj.cpp.progression.service.utils;

import static java.util.Objects.nonNull;

import uk.gov.justice.services.fileservice.api.FileRetriever;
import uk.gov.justice.services.fileservice.api.FileServiceException;
import uk.gov.justice.services.fileservice.domain.FileReference;

import java.util.Optional;
import java.util.UUID;

import javax.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@SuppressWarnings({"squid:S1166", "squid:S2221"})
public class FileUtil {

    private static final Logger LOGGER = LoggerFactory.getLogger(FileUtil.class.getCanonicalName());

    @Inject
    private FileRetriever fileRetriever;

    public String retrieveFileName(final UUID fileId) throws FileServiceException {
        String fileName = StringUtils.EMPTY;
        final Optional<FileReference> fileReferenceOptional = fileRetriever.retrieve(fileId);

        if(nonNull(fileReferenceOptional) && fileReferenceOptional.isPresent()){
            try(final FileReference fileReference = fileReferenceOptional.get() ) {
                fileName = nonNull(fileReference) ? fileReferenceOptional.get().getMetadata().getString("fileName"): StringUtils.EMPTY;
            } catch (Exception e) {
               LOGGER.error("Exception while retrieving file name {}", e.getMessage());
            }
        }
        return fileName;
    }
}
