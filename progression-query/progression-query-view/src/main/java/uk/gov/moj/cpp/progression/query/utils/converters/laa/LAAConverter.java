package uk.gov.moj.cpp.progression.query.utils.converters.laa;

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

public class LAAConverter {
    public static final String DATE_TIME_FORMAT = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'";

    protected static String formatZonedDateTime(final ZonedDateTime sittingDay) {
        return sittingDay.format(DateTimeFormatter.ofPattern(DATE_TIME_FORMAT));
    }
}
