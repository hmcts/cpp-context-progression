package uk.gov.moj.cpp.progression.service;

import static org.apache.commons.text.CharacterPredicates.DIGITS;
import static org.apache.commons.text.CharacterPredicates.LETTERS;

import uk.gov.justice.services.core.dispatcher.SystemUserProvider;
import uk.gov.moj.cpp.systemidmapper.client.SystemIdMap;
import uk.gov.moj.cpp.systemidmapper.client.SystemIdMapperClient;
import uk.gov.moj.cpp.systemidmapper.client.SystemIdMapping;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Optional;
import java.util.UUID;

import javax.inject.Inject;

import org.apache.commons.text.RandomStringGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ApplicationDetailsEnrichmentService {

    // Define a reasonable limit
    public static final int MAX_RETRIES = 10;
    private static final Logger LOGGER = LoggerFactory.getLogger(ApplicationDetailsEnrichmentService.class);

    private static final String APPLICATION_PREFIX = "A";
    private static final int LAA_ID_RANDOM_LENGTH = 9;
    private static final String TARGET_TYPE_APPLICATION = "APPLICATION_ID_LAA";
    private static final String SOURCE_TYPE_APPLICATION = "LAA_APP_SHORT_ID";

    private final SystemIdMapperClient systemIdMapperClient;
    private final SystemUserProvider systemUserProvider;

    @Inject
    public ApplicationDetailsEnrichmentService(
            final SystemIdMapperClient systemIdMapperClient,
            final SystemUserProvider systemUserProvider) {
        this.systemIdMapperClient = systemIdMapperClient;
        this.systemUserProvider = systemUserProvider;
    }


    public String createAndStoreLaaApplicationShortIdWithSystemIdMapper(final UUID applicationId) {
        LOGGER.info("Creating LAA application short ID for application ID: {}", applicationId);

        final UUID systemUserId = systemUserProvider.getContextSystemUserId()
                .orElseThrow(() -> {
                    LOGGER.error("Failed to retrieve context system user ID.");
                    return new IllegalStateException("Invalid context system user id");
                });

        String laaApplicationShortId;
        Optional<SystemIdMapping> existingMapping;
        int retryCount = 0;

        do {
            if (retryCount++ >= MAX_RETRIES) {
                LOGGER.info("Max retries ({}) reached while generating laaApplicationShortId for application ID: {}", MAX_RETRIES, applicationId);
                throw new IllegalStateException("Unable to generate a unique LAA application short ID after " + MAX_RETRIES + " attempts.");
            }
            laaApplicationShortId = generateLaaApplicationShortId();
            LOGGER.info("Generated potential laaApplicationShortId: {} for the applicationId : {} (Attempt: {})", laaApplicationShortId, applicationId, retryCount);
            existingMapping = systemIdMapperClient.findBy(laaApplicationShortId, SOURCE_TYPE_APPLICATION, TARGET_TYPE_APPLICATION, systemUserId);

        } while (existingMapping.isPresent());

        try {
            systemIdMapperClient.add(new SystemIdMap(laaApplicationShortId, SOURCE_TYPE_APPLICATION, applicationId, TARGET_TYPE_APPLICATION), systemUserId);
            LOGGER.info("Successfully created and stored laaApplicationShortId: {} for applicationId: {}", laaApplicationShortId, applicationId);
            return laaApplicationShortId;
        } catch (RuntimeException e) {
            throw new IllegalStateException("Failed to create mapping for applicationId: " + applicationId, e);
        }
    }

    String generateLaaApplicationShortId() {
        final String yearSuffix = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yy"));
        final RandomStringGenerator generator = new RandomStringGenerator.Builder()
                .withinRange('0', 'Z')
                .filteredBy(LETTERS, DIGITS)
                .build();

        String generatedId = APPLICATION_PREFIX + yearSuffix + generator.generate(LAA_ID_RANDOM_LENGTH);
        LOGGER.info("Generated LAA application short ID: {}", generatedId);
        return generatedId;
    }
}
