package uk.gov.moj.cpp.progression.service.disqualificationreferral;

import java.util.Locale;

import javax.inject.Inject;

public class ReferralDisqualifyWarningDataAggregatorFactory {

    @Inject
    private ReferralDisqualifyWarningEnglishDataAggregator referralDisqualifyWarningEnglishDataAggregator;

    @Inject
    private ReferralDisqualifyWarningWelshDataAggregator referralDisqualifyWarningWelshDataAggregator;

    public ReferralDisqualifyWarningDataAggregator getAggregator(Locale locale) {
        if (new Locale("CY").getLanguage().equals(locale.getLanguage())) {
            return referralDisqualifyWarningWelshDataAggregator;
        } else {
            return referralDisqualifyWarningEnglishDataAggregator;
        }
    }
}