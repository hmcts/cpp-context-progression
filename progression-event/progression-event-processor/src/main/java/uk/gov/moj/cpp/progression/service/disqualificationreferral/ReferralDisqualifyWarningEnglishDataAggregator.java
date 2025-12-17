package uk.gov.moj.cpp.progression.service.disqualificationreferral;

import uk.gov.justice.core.courts.CourtCentre;
import uk.gov.justice.core.courts.LjaDetails;

public class ReferralDisqualifyWarningEnglishDataAggregator extends ReferralDisqualifyWarningDataAggregator {

    @Override
    public String getCourtHouseName(final CourtCentre organisationUnit) {
        return organisationUnit.getName();
    }

    @Override
    public String getLjaName(final LjaDetails ljaDetails) {
        return ljaDetails.getLjaName();
    }
}