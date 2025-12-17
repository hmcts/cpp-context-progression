package uk.gov.moj.cpp.progression.model;

import java.util.List;

@SuppressWarnings("squid:S2384")
public class HearingEventLog {

    private String requestedTime;
    private String requestedUser;
    private List<HearingEventReport> hearings;

    public String getRequestedTime() {
        return requestedTime;
    }

    public void setRequestedTime(final String requestedTime) {
        this.requestedTime = requestedTime;
    }

    public String getRequestedUser() {
        return requestedUser;
    }

    public void setRequestedUser(final String requestedUser) {
        this.requestedUser = requestedUser;
    }

    public List<HearingEventReport> getHearings() {
        return hearings;
    }

    public void setHearings(final List<HearingEventReport> hearings) {
        this.hearings = hearings;
    }
}
