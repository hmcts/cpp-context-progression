package uk.gov.moj.cpp.progression.activiti.common;

/**
 * This class only made for activiti process constant to avoid any naming issues across different
 * delegate
 */
public class ProcessMapConstant {
    public static final String USER_ID = "userId";
    public static final String CASE_ID = "caseId";
    public static final String PLEA = "PLEA";
    public static final String HEARING_ID = "hearingId";
    public static final String HEARING_IDS = "hearingIds";
    public static final String WHEN = "WHEN";
    public static final String SEND_CASE_FOR_LISTING_PAYLOAD = "sendCaseForlistingPayload";
    public static final String INITIATE_HEARING_PAYLOAD = "initiateHearingPayload";

    private ProcessMapConstant() {
    }
}
