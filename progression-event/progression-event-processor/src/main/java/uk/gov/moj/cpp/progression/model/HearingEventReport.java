package uk.gov.moj.cpp.progression.model;

import static java.util.Objects.isNull;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@SuppressWarnings("squid:S2384")
public class HearingEventReport {
    private UUID hearingId;
    private String courtCentre;
    private String courtRoom;
    private String hearingType;
    private LocalDate startDate;
    private LocalDate endDate;
    private Set<String> judiciary;
    private Set<UUID> caseIds;
    private Set<String> caseUrns;
    private Set<String> defendants;
    private Set<String> defendantAttendees;
    private Set<String> prosecutionAttendees;
    private Set<LocalDate> hearingDates;
    private Set<UUID> applicationIds;
    private Set<String> applicationReferences;
    private Set<String> applicants;
    private Set<String> respondents;
    private Set<String> thirdParties;
    private List<LoggedHearingEvent> loggedHearingEvents;

    public List<LoggedHearingEvent> getLoggedHearingEvents() {
        return loggedHearingEvents;
    }

    public void setLoggedHearingEvents(final List<LoggedHearingEvent> loggedHearingEvents) {
        this.loggedHearingEvents = loggedHearingEvents;
    }

    public UUID getHearingId() {
        return hearingId;
    }

    public void setHearingId(final UUID hearingId) {
        this.hearingId = hearingId;
    }

    public String getCourtCentre() {
        return courtCentre;
    }

    public void setCourtCentre(final String courtCentre) {
        this.courtCentre = courtCentre;
    }

    public String getCourtRoom() {
        return courtRoom;
    }

    public void setCourtRoom(final String courtRoom) {
        this.courtRoom = courtRoom;
    }

    public String getHearingType() {
        return hearingType;
    }

    public void setHearingType(final String hearingType) {
        this.hearingType = hearingType;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public void setStartDate(final LocalDate startDate) {
        this.startDate = startDate;
    }

    public LocalDate getEndDate() {
        return endDate;
    }

    public void setEndDate(final LocalDate endDate) {
        this.endDate = endDate;
    }

    public Set<String> getJudiciary() {
        if (isNull(judiciary)) {
            judiciary = new HashSet<>();
        }
        return judiciary;
    }

    public void setJudiciary(final Set<String> judiciary) {
        this.judiciary = judiciary;
    }

    public Set<UUID> getCaseIds() {
        if (isNull(caseIds)) {
            caseIds = new HashSet<>();
        }
        return caseIds;
    }

    public void setCaseIds(final Set<UUID> caseIds) {
        this.caseIds = caseIds;
    }

    public Set<String> getCaseUrns() {
        if (isNull(caseUrns)) {
            caseUrns = new HashSet<>();
        }
        return caseUrns;
    }

    public void setCaseUrns(final Set<String> caseUrns) {
        this.caseUrns = caseUrns;
    }

    public Set<String> getDefendants() {
        if (isNull(defendants)) {
            defendants = new HashSet<>();
        }
        return defendants;
    }

    public void setDefendants(final Set<String> defendants) {
        this.defendants = defendants;
    }

    public Set<String> getDefendantAttendees() {
        if (isNull(defendantAttendees)) {
            defendantAttendees = new HashSet<>();
        }
        return defendantAttendees;
    }

    public void setDefendantAttendees(final Set<String> defendantAttendees) {
        this.defendantAttendees = defendantAttendees;
    }

    public Set<String> getProsecutionAttendees() {
        if (isNull(prosecutionAttendees)) {
            prosecutionAttendees = new HashSet<>();
        }
        return prosecutionAttendees;
    }

    public void setProsecutionAttendees(final Set<String> prosecutionAttendees) {
        this.prosecutionAttendees = prosecutionAttendees;
    }

    public Set<LocalDate> getHearingDates() {
        if (isNull(hearingDates)) {
            hearingDates = new HashSet<>();
        }
        return hearingDates;
    }

    public void setHearingDates(final Set<LocalDate> hearingDates) {
        this.hearingDates = hearingDates;
    }

    public Set<UUID> getApplicationIds() {
        if (isNull(applicationIds)) {
            applicationIds = new HashSet<>();
        }
        return applicationIds;
    }

    public void setApplicationIds(final Set<UUID> applicationIds) {
        this.applicationIds = applicationIds;
    }

    public Set<String> getApplicationReferences() {
        if (isNull(applicationReferences)) {
            applicationReferences = new HashSet<>();
        }
        return applicationReferences;
    }

    public void setApplicationReferences(final Set<String> applicationReferences) {
        this.applicationReferences = applicationReferences;
    }

    public Set<String> getApplicants() {
        if (isNull(applicants)) {
            applicants = new HashSet<>();
        }
        return applicants;
    }

    public void setApplicants(final Set<String> applicants) {
        this.applicants = applicants;
    }

    public Set<String> getRespondents() {
        if (isNull(respondents)) {
            respondents = new HashSet<>();
        }
        return respondents;
    }

    public void setRespondents(final Set<String> respondents) {
        this.respondents = respondents;
    }

    public Set<String> getThirdParties() {
        if (isNull(thirdParties)) {
            thirdParties = new HashSet<>();
        }
        return thirdParties;
    }

    public void setThirdParties(final Set<String> thirdParties) {
        this.thirdParties = thirdParties;
    }
}
