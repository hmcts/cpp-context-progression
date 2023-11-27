package uk.gov.moj.cpp.progression.domain.transformation.judicialresult.domain;

import java.util.Arrays;

public enum EventToTransform {

    BOXWORK_APPLICATION_REFERRED("progression.event.boxwork-application-referred"),
    HEARING_EXTENDED("progression.event.hearing-extended"),
    HEARING_RESULTED("progression.event.hearing-resulted"),
    COURT_APPLICATION_UPDATED("progression.event.court-application-updated"),
    PROSECUTION_CASE_OFFENCES_UPDATED("progression.event.prosecution-case-offences-updated"),
    PROSECUTION_CASE_DEFENDANT_UPDATED("progression.event.prosecution-case-defendant-updated"),
    HEARING_RESULTED_CASE_UPDATED("progression.event.hearing-resulted-case-updated"),
    CASE_RETENTION_LENGTH_CALCULATED("progression.events.case-retention-length-calculated"),
    HEARING_INITIATE_ENRICHED("progression.hearing-initiate-enriched"),
    COURT_APPLICATION_CREATED("progression.event.court-application-created"),
    COURT_APPLICATION_ADDED_TO_CASE("progression.event.court-application-added-to-case"),
    LISTED_COURT_APPLICATION_CHANGED("progression.event.listed-court-application-changed"),
    APPLICATION_REFERRED_TO_COURT("progression.event.application-referred-to-court"),
    HEARING_APPLICATION_LINK_CREATED("progression.event.hearing-application-link-created"),
    PROSECUTION_CASE_DEFENDANT_LISTING_STATUS_CHANGED("progression.event.prosecutionCase-defendant-listing-status-changed"),
    PROSECUTION_CASE_DEFENDANT_LISTING_STATUS_CHANGED_V2("progression.event.prosecutionCase-defendant-listing-status-changed-v2"),
    PROSECUTION_CASE_DEFENDANT_LISTING_STATUS_CHANGED_V3("progression.event.prosecutionCase-defendant-listing-status-changed-v3"),
    HEARING_CONFIRMED_CASE_STATUS_UPDATED("progression.event.hearing-confirmed-case-status-updated"),
    UNSCHEDULED_HEARING_LISTING_REQUESTED("progression.event.unscheduled-hearing-listing-requested"),
    SLOTS_BOOKED_FOR_APPLICATION("progression.event.slots-booked-for-application");

    private final String eventName;

    EventToTransform(final String eventName) {
        this.eventName = eventName;
    }

    public String getEventName() {
        return eventName;
    }

    public static boolean isEventToTransform(final String eventName) {
        return Arrays.stream(values()).anyMatch(event -> event.eventName.equals(eventName));
    }
}
