package uk.gov.moj.cpp.progression.it.framework.util;

import static uk.gov.moj.cpp.progression.it.framework.ContextNameProvider.CONTEXT_NAME;

import uk.gov.justice.services.test.utils.persistence.DatabaseCleaner;

public class ViewStoreCleaner {

    public static void cleanViewStoreTables() {
        final DatabaseCleaner databaseCleaner = new DatabaseCleaner();
        databaseCleaner.cleanViewStoreTables(CONTEXT_NAME,
                "defendant_request",
                "offence",
                "defendant_bail_document",
                "case_defendant_hearing",
                "hearing",
                "search_prosecution_case",
                "person",
                "processed_event",
                "address",
                "offence_plea",
                "offence_indicated_plea",
                "court_document_index",
                "hearing_result_line",
                "court_application",
                "material_usergroup",
                "notification_status",
                "defendant",
                "caseprogressiondetail",
                "court_document_material",
                "court_document",
                "hearing_application",
                "prosecution_case",
                "case_link_split_merge",
                "defendant_partial_match",
                "match_defendant_case_hearing");
    }

    public static void cleanEventStoreTables() {
        final DatabaseCleaner databaseCleaner = new DatabaseCleaner();
        databaseCleaner.cleanEventStoreTables(CONTEXT_NAME);
    }
}
