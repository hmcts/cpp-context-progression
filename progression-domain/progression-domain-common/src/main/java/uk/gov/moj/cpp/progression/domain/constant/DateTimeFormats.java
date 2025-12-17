package uk.gov.moj.cpp.progression.domain.constant;

public enum DateTimeFormats {
    SLASHED_DDMM("dd/MM"),
    SLASHED_DAY_DDMM("EEE dd/MM"),
    STANDARD("yyyy-MM-dd"),
    HYP_DAY_TIME_YYYY_MM_DD("yyyy-MM-dd HH:mm:ss"),
    TIME_HMMA("h:mm a"),
    DATE_SLASHED_DD_MM_YYYY("dd/MM/yyyy"),
    SPACE_SEPARATED_3_CHAR_MONTH("dd MMM yyyy"),
    DATE_WITH_TIME("yyyy-MM-dd'T'HH:mm");

    private final String dateFormat;

    DateTimeFormats(String dateFormat) {
        this.dateFormat = dateFormat;
    }

    public String getValue() {
        return this.dateFormat;
    }

    public static String[] getPatterns() {
        return new String[] { DateTimeFormats.STANDARD.getValue(), DateTimeFormats.SLASHED_DDMM.getValue(),
                DateTimeFormats.HYP_DAY_TIME_YYYY_MM_DD.getValue(), DateTimeFormats.DATE_SLASHED_DD_MM_YYYY.getValue() };
    }

}
