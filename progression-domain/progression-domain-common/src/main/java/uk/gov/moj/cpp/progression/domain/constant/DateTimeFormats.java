package uk.gov.moj.cpp.progression.domain.constant;

public enum DateTimeFormats {
    SLASHED_DDMM("dd/MM"),
    SLASHED_DAY_DDMM("EEE dd/MM"),
    STANDARD("yyyy-MM-dd"),
    HYP_DAY_TIME_YYYY_MM_DD("yyyy-MM-dd HH:mm:ss"),
    TIME_HMMA("h:mm a");

    private String dateFormat;

    private DateTimeFormats(String dateFormat) {
        this.dateFormat = dateFormat;
    }

    public String getValue() {
        return this.dateFormat;
    }

    public static String[] getPatterns() {
        return new String[] { DateTimeFormats.STANDARD.getValue(), DateTimeFormats.SLASHED_DDMM.getValue(),
                DateTimeFormats.HYP_DAY_TIME_YYYY_MM_DD.getValue() };
    }

}
