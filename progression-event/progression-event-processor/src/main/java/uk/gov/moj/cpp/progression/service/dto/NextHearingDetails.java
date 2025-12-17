package uk.gov.moj.cpp.progression.service.dto;

import uk.gov.justice.core.courts.HearingListingNeeds;
import uk.gov.justice.core.courts.NextHearing;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public class NextHearingDetails {

    private List<HearingListingNeeds> hearingListingNeedsList;

    private Map<UUID, NextHearing> nextHearings;

    public NextHearingDetails(List<HearingListingNeeds> hearingListingNeedsList, Map<UUID, NextHearing> nextHearings) {
        this.hearingListingNeedsList = hearingListingNeedsList;
        this.nextHearings = nextHearings;
    }

    public List<HearingListingNeeds> getHearingListingNeedsList() {
        return hearingListingNeedsList;
    }

    public void setHearingListingNeedsList(List<HearingListingNeeds> hearingListingNeedsList) {
        this.hearingListingNeedsList = hearingListingNeedsList;
    }

    public Map<UUID, NextHearing> getNextHearings() {
        return nextHearings;
    }

    public void setNextHearings(Map<UUID, NextHearing> nextHearings) {
        this.nextHearings = nextHearings;
    }
}
