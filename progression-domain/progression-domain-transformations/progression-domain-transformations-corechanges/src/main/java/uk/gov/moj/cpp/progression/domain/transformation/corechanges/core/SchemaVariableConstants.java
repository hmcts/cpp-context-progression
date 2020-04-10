package uk.gov.moj.cpp.progression.domain.transformation.corechanges.core;

public final class SchemaVariableConstants {


    public static final String PROGRESSION_APPLICATION_REFERRED_TO_COURT = "progression.event.application-referred-to-court";
    public static final String PROGRESSION_BOXWORK_APPLICATION_REFERRED = "progression.event.boxwork-application-referred";
    public static final String PROGRESSION_REFERRED_TO_COURT = "progression.event.cases-referred-to-court";
    public static final String PROGRESSION_COURT_APPLICATION_CREATED = "progression.event.court-application-created";
    public static final String PROGRESSION_COURT_APPLICATION_ADDED_TO_CASE = "progression.event.court-application-added-to-case";
    public static final String PROGRESSION_COURT_APPLICATION_UPDATED = "progression.event.court-application-updated";
    public static final String PROGRESSION_HEARING_INITIATE_ENRICHED = "progression.hearing-initiate-enriched";
    public static final String PROGRESSION_HEARING_APPLICATION_LINK_CREATED = "progression.event.hearing-application-link-created";
    public static final String PROGRESSION_HEARING_EXTENDED = "progression.event.hearing-extended";
    public static final String PROGRESSION_HEARING_RESULTED = "progression.event.hearing-resulted";
    public static final String PROGRESSION_HEARING_RESULTED_CASE_UPDATED = "progression.event.hearing-resulted-case-updated";
    public static final String PROGRESSION_NOWS_REQUESTED = "progression.event.nows-requested";
    public static final String PROGRESSION_PROSECUTION_CASE_CREATED = "progression.event.prosecution-case-created";
    public static final String PROGRESSION_PROSECUTIONCASE_DEFENDANT_LISTING_STATUS_CHANGED = "progression.event.prosecutionCase-defendant-listing-status-changed";
    public static final String PROGRESSION_PROSECUTION_CASE_DEFENDANT_UPDATED = "progression.event.prosecution-case-defendant-updated";
    public static final String PROGRESSION_COURT_PROCEEDINGS_INITIATED= "progression.event.court-proceedings-initiated";
    public static final String PROGRESSION_LISTED_COURT_APPLICATION_CHANGED= "progression.event.listed-court-application-changed";


    public static final String FIELD_ID = "id";
    public static final String FIELD_MASTER_DEFENDANT_ID = "masterDefendantId";
    public static final String FIELD_COURT_PROCEEDINGS_INITIATED = "courtProceedingsInitiated";

    private SchemaVariableConstants() {
    }
}
