package uk.gov.moj.cpp.progression.processor.summons;

import static com.google.common.collect.Lists.newArrayList;

import java.util.Arrays;

public enum SummonsCode {

    MCA("M", "MCA"),
    EITHER_WAY("E", "EW"),
    WITNESS("W", "WS"),
    APPLICATION("A", "A"),
    BREACH_OFFENCES("B", "B");

    private String code;
    private String subType;

    SummonsCode(final String code, final String subType) {
        this.code = code;
        this.subType = subType;
    }

    public static boolean generateSummons(final String code) {
        return newArrayList(MCA, EITHER_WAY, WITNESS, APPLICATION, BREACH_OFFENCES).stream().anyMatch(sc -> sc.code.equalsIgnoreCase(code));
    }

    public static SummonsCode getSummonsCode(final String code) {
        return Arrays.stream(SummonsCode.values()).filter(sc -> sc.code.equalsIgnoreCase(code)).findFirst().orElse(null);
    }

    public String getCode() {
        return code;
    }

    public String getSubType() {
        return subType;
    }
}
