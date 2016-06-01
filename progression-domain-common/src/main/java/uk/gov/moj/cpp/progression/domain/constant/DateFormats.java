package uk.gov.moj.cpp.progression.domain.constant;

public enum DateFormats {
    SLASHED_DDMM("dd/MM"), SLASHED_DAY_DDMM("EEE dd/MM"), STANDARD("yyyy-MM-dd"), HYP_DAY_TIME_YYYY_MM_DD("yyyy-MM-dd HH:mm:ss");

    private String dateFormat;

    private DateFormats(String dateFormat) {
        this.dateFormat = dateFormat;
    }

    public String getValue() {
        return this.dateFormat;
    }

    public static String[] getPatterns() {
        return new String[] { DateFormats.STANDARD.getValue(), DateFormats.SLASHED_DDMM.getValue(),
                DateFormats.HYP_DAY_TIME_YYYY_MM_DD.getValue() };
    }

}
