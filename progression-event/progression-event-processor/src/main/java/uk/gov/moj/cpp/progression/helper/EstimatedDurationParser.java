package uk.gov.moj.cpp.progression.helper;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Parses the {@code estimatedDuration} String field on a Hearing into an Integer number of minutes.
 *
 * <p>The Hearing schema stores the user-entered duration as a String of the form
 * {@code "<number> <UNIT>"} where {@code UNIT} is either {@code MINUTES} or {@code HOURS}
 * (e.g. {@code "30 MINUTES"}, {@code "1 HOURS"}, {@code "2 HOURS"}).
 *
 * <p>When transforming a Hearing into a {@code HearingUnscheduledListingNeeds} the listing
 * context expects the duration as an Integer ({@code estimatedMinutes}). Returning {@code null}
 * for a missing or malformed value is intentional: the listing-side fallback in
 * {@code HearingDurationDefaults} substitutes a safe default
 * (preserving the SPRDT-806/807 "never 0 / never null" guarantee).
 */
final class EstimatedDurationParser {

    private static final Logger LOGGER = LoggerFactory.getLogger(EstimatedDurationParser.class);

    private static final String MINUTES = "MINUTES";
    private static final String HOURS = "HOURS";

    private EstimatedDurationParser() {
    }

    /**
     * Convert {@code "<number> MINUTES"} or {@code "<number> HOURS"} to a count of minutes.
     * Returns {@code null} for null, blank, or unrecognised input — letting downstream defaults
     * decide the final value.
     */
    static Integer toMinutes(final String estimatedDuration) {
        if (estimatedDuration == null || estimatedDuration.isBlank()) {
            return null;
        }
        final String[] parts = estimatedDuration.trim().split("\\s+");
        if (parts.length != 2) {
            LOGGER.warn("Unrecognised estimatedDuration format '{}', expected '<number> <MINUTES|HOURS>'", estimatedDuration);
            return null;
        }
        final int amount;
        try {
            amount = Integer.parseInt(parts[0]);
        } catch (final NumberFormatException e) {
            LOGGER.warn("Unrecognised estimatedDuration amount '{}'", estimatedDuration);
            return null;
        }
        final String unit = parts[1].toUpperCase();
        if (MINUTES.equals(unit)) {
            return amount;
        }
        if (HOURS.equals(unit)) {
            return amount * 60;
        }
        LOGGER.warn("Unrecognised estimatedDuration unit '{}' in '{}', expected MINUTES or HOURS", unit, estimatedDuration);
        return null;
    }
}
