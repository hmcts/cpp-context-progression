package uk.gov.moj.cpp.progression.domain.constant;

public enum HearingTypeEnum {

    PRETRIAL("Pre trial"), PRELIMINARY("Preliminary hearing"), TRIAL("Trial"), VEREDICT("VEREDICT"), APPEAL("Appleal"), PTP(
            "Pre-Trail Preparation");

    private String description;

    private HearingTypeEnum(final String description) {
        this.description = description;
    }

    /**
     * Gets the description.
     * 
     * @return the description
     */
    public String getDescription() {
        return description;
    }

    public static HearingTypeEnum getHearingType(final String value) {
        final HearingTypeEnum[] hearingTypeArray = HearingTypeEnum.values();
        for (final HearingTypeEnum hearingType : hearingTypeArray) {
            if (hearingType.getDescription().equals(value)) {
                return hearingType;
            }
        }
        return null;
    }

}
