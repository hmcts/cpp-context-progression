package uk.gov.moj.cpp.progression.applications;

import static uk.gov.moj.cpp.progression.util.FileUtil.getPayload;

public class SummonsResultUtil {

    public static final String DEFAULT_PROSECUTION_COST = "£245.56";


    public static String getSummonsApprovedResult(final boolean personalService, final boolean summonsSuppressed, final String prosecutorEmailAddress) {
        return getSummonsApprovedResult(DEFAULT_PROSECUTION_COST, personalService, summonsSuppressed, prosecutorEmailAddress);
    }

    public static String getSummonsApprovedResult(final String prosecutionCost, final boolean personalService, final boolean summonsSuppressed, final String prosecutorEmailAddress) {
        return getPayload("applications/summons.approved.results.json")
                .replace("PROSECUTOR_EMAIL", prosecutorEmailAddress)
                .replace("PERSONAL_SERVICE", personalService + "")
                .replace("SUMMONS_SUPPRESSED", summonsSuppressed + "")
                .replace("PROSECUTION_COST", prosecutionCost);
    }

    public static String getSummonsRejectedResult(final String rejectionReason, final String prosecutorEmailAddress) {
        return getPayload("applications/summons.rejected.results.json")
                .replace("PROSECUTOR_EMAIL", prosecutorEmailAddress)
                .replace("REJECTION_REASON", rejectionReason);
    }
}
