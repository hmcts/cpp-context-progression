package uk.gov.moj.cpp.progression.domain.transformation.ctl;

import uk.gov.moj.cpp.coredomain.transform.TransformQuery;
import uk.gov.moj.cpp.coredomain.transform.TransformReport;

import java.io.IOException;
import java.util.Arrays;

import org.junit.Ignore;
import org.junit.Test;

@Ignore //This no longer works as the event processor RAML has been converted to the new YAML format for subscriptions.
// Will be fix by GPE-11673
public class ProgressionMIReportDataTest {

    @Test
    public void testCoverage() throws IOException {
        TransformReport report = (new TransformQuery()).compare("../../..",
                Arrays.asList(
                        "/progression-command/progression-command-handler/src/raml/progression_command_handler.messaging.raml",
                        "/progression-event/progression-event-processor/src/raml/progression-event-processor.messaging.raml"),
                EVENT_PACKAGES, MASTER_PACKAGE_PREFIX);
        System.out.println("\r\n\r\n*****************results::");
        report.printOut();
    }

    public final static String[] EVENT_PACKAGES = new String[]{
            "uk.gov.moj.cpp.progression.domain.event",
            "uk.gov.moj.cpp.progression.domain.event.nows.order",
            "uk.gov.justice.progression.courts"
    };

    public final static String MASTER_PACKAGE_PREFIX = "progressionmaster";
}


