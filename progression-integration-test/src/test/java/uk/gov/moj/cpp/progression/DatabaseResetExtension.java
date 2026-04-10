package uk.gov.moj.cpp.progression;

import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import uk.gov.justice.services.test.utils.persistence.DatabaseCleaner;

import static uk.gov.moj.cpp.progression.it.framework.ContextNameProvider.CONTEXT_NAME;

public class DatabaseResetExtension implements BeforeEachCallback {

    private final DatabaseCleaner databaseCleaner = new DatabaseCleaner();

    @Override
    public void beforeEach(final ExtensionContext context) {
        databaseCleaner.cleanEventStoreTables(CONTEXT_NAME);
        databaseCleaner.resetEventSubscriptionStatusTable(CONTEXT_NAME);
        databaseCleaner.cleanSystemTables(CONTEXT_NAME);

        databaseCleaner.cleanViewStoreTables(
                CONTEXT_NAME,
                "stream_buffer",
                "stream_status",
                "processed_event",
                "stream_error_hash",
                "stream_error",
                "stream_error_retry");
    }
}
