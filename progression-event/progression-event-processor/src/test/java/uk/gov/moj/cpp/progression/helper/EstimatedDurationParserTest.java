package uk.gov.moj.cpp.progression.helper;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;

import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link EstimatedDurationParser}. The Hearing schema stores user-entered
 * durations as strings of the form {@code "<n> MINUTES"} or {@code "<n> HOURS"}; the parser
 * converts those into integer minutes for the listing schema. Anything it can't recognise
 * (null, blank, missing unit, garbage) returns null so the listing-side
 * HearingDurationDefaults fallback kicks in.
 */
public class EstimatedDurationParserTest {

    @Test
    public void shouldParseMinutesFormat() {
        assertThat(EstimatedDurationParser.toMinutes("30 MINUTES"), is(30));
    }

    @Test
    public void shouldParseHoursFormatAndConvertToMinutes() {
        assertThat(EstimatedDurationParser.toMinutes("2 HOURS"), is(120));
    }

    @Test
    public void shouldParseSingleHour() {
        assertThat(EstimatedDurationParser.toMinutes("1 HOURS"), is(60));
    }

    @Test
    public void shouldParseSingleMinute() {
        assertThat(EstimatedDurationParser.toMinutes("1 MINUTES"), is(1));
    }

    @Test
    public void shouldHandleLowercaseUnit() {
        assertThat(EstimatedDurationParser.toMinutes("45 minutes"), is(45));
        assertThat(EstimatedDurationParser.toMinutes("3 hours"), is(180));
    }

    @Test
    public void shouldHandleMixedCaseUnit() {
        assertThat(EstimatedDurationParser.toMinutes("90 Minutes"), is(90));
    }

    @Test
    public void shouldTrimSurroundingWhitespace() {
        assertThat(EstimatedDurationParser.toMinutes("   30 MINUTES   "), is(30));
    }

    @Test
    public void shouldReturnNullForNull() {
        assertThat(EstimatedDurationParser.toMinutes(null), is(nullValue()));
    }

    @Test
    public void shouldReturnNullForEmptyString() {
        assertThat(EstimatedDurationParser.toMinutes(""), is(nullValue()));
    }

    @Test
    public void shouldReturnNullForBlankString() {
        assertThat(EstimatedDurationParser.toMinutes("   "), is(nullValue()));
    }

    @Test
    public void shouldReturnNullForMissingUnit() {
        assertThat(EstimatedDurationParser.toMinutes("30"), is(nullValue()));
    }

    @Test
    public void shouldReturnNullForUnrecognisedUnit() {
        // "1 week" is a duration found in some fixtures but not a unit the listing
        // schema's Integer minutes can express precisely; returning null lets the
        // listing-side fallback substitute a sane default.
        assertThat(EstimatedDurationParser.toMinutes("1 week"), is(nullValue()));
        assertThat(EstimatedDurationParser.toMinutes("2 days"), is(nullValue()));
    }

    @Test
    public void shouldReturnNullForNonNumericAmount() {
        assertThat(EstimatedDurationParser.toMinutes("abc MINUTES"), is(nullValue()));
        assertThat(EstimatedDurationParser.toMinutes("- HOURS"), is(nullValue()));
    }

    @Test
    public void shouldReturnNullForExtraTokens() {
        assertThat(EstimatedDurationParser.toMinutes("30 MINUTES extra"), is(nullValue()));
    }

    @Test
    public void shouldHandleZero() {
        // Zero is technically valid arithmetic; it'll then fall through the listing-side
        // user-priority check (>1) and trigger the type-default fallback.
        assertThat(EstimatedDurationParser.toMinutes("0 MINUTES"), is(0));
    }
}
