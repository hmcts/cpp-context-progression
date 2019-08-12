package uk.gov.moj.cpp.progression.it.framework.util;

import uk.gov.justice.services.test.utils.persistence.DatabaseCleaner;

public class ViewStoreCleaner {

    private final DatabaseCleaner databaseCleaner = new DatabaseCleaner();

    public void cleanViewstoreTables() {
        databaseCleaner.cleanViewStoreTables("progression",
                "address",
                "case_defendant_hearing",
                "offence",
                "defendant_bail_document",
                "defendant",
                "caseprogressiondetail",
                "court_application",
                "material_usergroup",
                "court_document_material",
                "court_document_index",
                "court_document",
                "defendant_request",
                "hearing",
                "hearing_application",
                "hearing_result_line",
                "notification_status",
                "offence_plea",
                "person",
                "prosecution_case",
                "search_prosecution_case",
                "processed_event");
    }
}
