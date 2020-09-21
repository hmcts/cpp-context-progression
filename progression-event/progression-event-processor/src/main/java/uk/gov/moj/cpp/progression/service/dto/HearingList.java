package uk.gov.moj.cpp.progression.service.dto;

import uk.gov.moj.cpp.listing.domain.Hearing;

import java.util.List;

public class HearingList {
    private List<Hearing> hearings;

    public HearingList() {
    }

    public HearingList(final List<Hearing> hearings) {
        this.hearings = hearings;
    }

    public List<Hearing> getHearings() {
        return hearings;
    }

    public void setHearings(final List<Hearing> hearings) {
        this.hearings = hearings;
    }
}
