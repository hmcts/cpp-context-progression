package uk.gov.moj.cpp.progression.service.disqualificationreferral;

import static org.apache.commons.lang3.StringUtils.isBlank;

import uk.gov.justice.core.courts.CourtCentre;
import uk.gov.justice.core.courts.LjaDetails;

import java.time.format.DateTimeFormatter;
import java.util.Locale;

public class ReferralDisqualifyWarningWelshDataAggregator extends ReferralDisqualifyWarningDataAggregator {

    private static final Locale WELSH_LOCALE = new Locale("cy");

    @Override
    public String getCourtHouseName(final CourtCentre organisationUnit) {

        return isBlank(organisationUnit.getWelshName()) ? organisationUnit.getName(): organisationUnit.getWelshName() ;
    }

    @Override
    public String getLjaName(final LjaDetails ljaDetails) {

        return isBlank(ljaDetails.getWelshLjaName()) ? ljaDetails.getLjaName() : ljaDetails.getWelshLjaName();
    }

    @Override
    protected DateTimeFormatter getDateTimeFormatter() {
        return super.getDateTimeFormatter().withLocale(WELSH_LOCALE);
    }
}