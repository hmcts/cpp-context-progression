package uk.gov.moj.cpp.progression.model;

import static java.util.Objects.isNull;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@SuppressWarnings("squid:S2384")
public class LoggedHearingEvent {

    private LocalDate hearingDate;
    private Set<String> courtClerks;
    private List<EventLog> eventLogs;

    public LocalDate getHearingDate() {
        return hearingDate;
    }

    public void setHearingDate(final LocalDate hearingDate) {
        this.hearingDate = hearingDate;
    }

    public Set<String> getCourtClerks() {
        if (isNull(courtClerks)) {
            courtClerks = new HashSet<>();
        }
        return courtClerks;
    }

    public void setCourtClerks(final Set<String> courtClerks) {
        this.courtClerks = courtClerks;
    }

    public List<EventLog> getEventLogs() {
        return eventLogs;
    }

    public void setEventLogs(final List<EventLog> eventLogs) {
        this.eventLogs = eventLogs;
    }
}
