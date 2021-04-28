package uk.gov.moj.cpp.progression.domain.aggregate.utils;

import uk.gov.justice.core.courts.HearingListingNeeds;
import uk.gov.justice.core.courts.NextHearing;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class NextHearingDetails {

    private Map<UUID, Map<UUID, Map<UUID, Set<UUID>>>> hearingsMap;

    private List<HearingListingNeeds> hearingListingNeedsList;

    private Map<UUID, NextHearing> nextHearings;

    public NextHearingDetails(Map<UUID, Map<UUID, Map<UUID, Set<UUID>>>> hearingsMap, List<HearingListingNeeds> hearingListingNeedsList, Map<UUID, NextHearing> nextHearings) {
        this.hearingsMap = hearingsMap;
        this.hearingListingNeedsList = hearingListingNeedsList;
        this.nextHearings = nextHearings;
    }

    public Map<UUID, Map<UUID, Map<UUID, Set<UUID>>>> getHearingsMap() {
        return hearingsMap;
    }

    public void setHearingsMap(Map<UUID, Map<UUID, Map<UUID, Set<UUID>>>> hearingsMap) {
        this.hearingsMap = hearingsMap;
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
