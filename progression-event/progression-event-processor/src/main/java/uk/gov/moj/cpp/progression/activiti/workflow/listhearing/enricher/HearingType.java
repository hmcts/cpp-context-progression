package uk.gov.moj.cpp.progression.activiti.workflow.listhearing.enricher;

public enum HearingType {
    PTP("Plea & Trial Preparation", 15), SENTENCE("Sentence", 30);

    private final String name;
    private final Integer estimateMinutes;


    HearingType(final String name, final Integer estimateMinutes) {
        this.name = name;
        this.estimateMinutes = estimateMinutes;
    }

    public Integer getEstimateMinutes() {
        return estimateMinutes;
    }

    public String getName() {
        return name;
    }
}
