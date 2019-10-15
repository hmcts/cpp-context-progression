package uk.gov.moj.cpp.progression.domain.transformation.ctl;

import uk.gov.moj.cpp.coredomain.transform.TransformQuery;
import uk.gov.moj.cpp.coredomain.transform.TransformReport;

import java.io.IOException;
import java.util.Arrays;

public class ProgressionTransformReport {


    public final static String[] EVENT_PACKAGES =  new String [] {
            "uk.gov.moj.cpp.progression.domain.event",
            "uk.gov.moj.cpp.progression.domain.event.nows.order",
            "uk.gov.justice.progression.courts"} ;
    public final static String MASTER_PACKAGE_PREFIX="progressionmaster";

    public TransformReport query() throws IOException {
        return query("../../..");
    }

    public TransformReport query(String projectRoot) throws IOException {
        TransformReport report = (new TransformQuery()).compare(projectRoot,
                Arrays.asList(
                        "/progression-command/progression-command-handler/src/raml/progression_command_handler.messaging.raml",
                        "/progression-event/progression-event-processor/src/raml/progression-event-processor.messaging.raml"),
                EVENT_PACKAGES, MASTER_PACKAGE_PREFIX);
        System.out.println("\r\n\r\n*****************results::");
        report.printOut();
        return report;

    }

    public static void main(String[] args) throws IOException{
        (new ProgressionTransformReport()).query(".");
    }

}
