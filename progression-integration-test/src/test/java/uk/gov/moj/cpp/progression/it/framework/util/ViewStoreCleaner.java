package uk.gov.moj.cpp.progression.it.framework.util;

import static uk.gov.moj.cpp.progression.it.framework.ContextNameProvider.CONTEXT_NAME;

import uk.gov.justice.services.test.utils.persistence.DatabaseCleaner;

import java.util.List;

public class ViewStoreCleaner {

    public static void cleanViewStoreTables(List<String> tablesNames) {
        final DatabaseCleaner databaseCleaner = new DatabaseCleaner();
        databaseCleaner.cleanViewStoreTables(CONTEXT_NAME, tablesNames);
    }

    public static void cleanEventStoreTables() {
        final DatabaseCleaner databaseCleaner = new DatabaseCleaner();
        databaseCleaner.cleanEventStoreTables(CONTEXT_NAME);
    }
}
