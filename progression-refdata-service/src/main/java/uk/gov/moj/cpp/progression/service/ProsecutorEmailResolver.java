package uk.gov.moj.cpp.progression.service;

import static java.util.Objects.nonNull;

import javax.json.JsonObject;

/**
 * Resolves the email address used for prosecutor/informant/respondent notifications from a
 * prosecutor reference-data JSON object.
 *
 * <p>For CPS prosecutors ({@code cpsFlag == true}) the CPS Crown Court email
 * ({@code cpsCcEmailAddress}) is used when present; otherwise it falls back to the standard
 * {@code contactEmailAddress}.</p>
 */
public final class ProsecutorEmailResolver {

    public static final String PROSECUTOR_CONTACT_EMAIL_ADDRESS_KEY = "contactEmailAddress";
    public static final String PROSECUTOR_CPS_FLAG_KEY = "cpsFlag";
    public static final String PROSECUTOR_CPS_CC_EMAIL_ADDRESS_KEY = "cpsCcEmailAddress";

    private ProsecutorEmailResolver() {
    }

    /**
     * @param prosecutorJson prosecutor reference-data JSON object
     * @return {@code true} when the prosecutor is a CPS prosecutor ({@code cpsFlag == true})
     */
    public static boolean isCps(final JsonObject prosecutorJson) {
        return prosecutorJson.containsKey(PROSECUTOR_CPS_FLAG_KEY)
                && prosecutorJson.getBoolean(PROSECUTOR_CPS_FLAG_KEY);
    }

    /**
     * @param prosecutorJson prosecutor reference-data JSON object
     * @return the email address to notify, or {@code null} when none is available
     */
    public static String resolveEmailAddress(final JsonObject prosecutorJson) {
        if (isCps(prosecutorJson)) {
            final String cpsCcEmailAddress = prosecutorJson.getString(PROSECUTOR_CPS_CC_EMAIL_ADDRESS_KEY, null);
            if (nonNull(cpsCcEmailAddress) && !cpsCcEmailAddress.isBlank()) {
                return cpsCcEmailAddress;
            }
        }

        final String contactEmailAddress = prosecutorJson.getString(PROSECUTOR_CONTACT_EMAIL_ADDRESS_KEY, null);
        return (nonNull(contactEmailAddress) && !contactEmailAddress.isBlank()) ? contactEmailAddress : null;
    }
}
