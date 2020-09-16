package uk.gov.moj.cpp.progression.domain.transformation.deactivestream;

import static org.slf4j.LoggerFactory.getLogger;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;

public class CsvFileHelper {
    private static final Logger LOGGER = getLogger(CsvFileHelper.class);

    private CsvFileHelper() {
    }

    public static List<UUID> getCsvRecords(final String csvFilename) {
        LOGGER.info("Processing : {} ...", csvFilename);
        final List<UUID> uuidList = new ArrayList<>();
        String row;
        try (final InputStream resource = Thread.currentThread().getContextClassLoader().getResourceAsStream(csvFilename)) {
            final BufferedReader csvReader = new BufferedReader(new InputStreamReader(resource));
            while ((row = csvReader.readLine()) != null) {
                final String[] data = row.split(",");
                if (StringUtils.isNotEmpty(data[0]) && !data[0].startsWith("#")) {
                    uuidList.add(UUID.fromString(data[0]));
                } else {
                    LOGGER.info("Cannot processes this data : {} ", data[0]);
                }

            }
        } catch (final IOException e) {
            throw new EventTransformationException("Cannot read Csv File " + csvFilename + " Exception:" + e.getMessage(), e);
        }

        return uuidList;
    }

}
