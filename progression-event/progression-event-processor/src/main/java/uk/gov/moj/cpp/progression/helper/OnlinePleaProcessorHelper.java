package uk.gov.moj.cpp.progression.helper;

import static java.util.Arrays.asList;
import static java.util.UUID.fromString;

import java.util.List;
import java.util.UUID;

public class OnlinePleaProcessorHelper {

    public static final UUID PLEA_DOCUMENT_TYPE_ID = fromString("460f77c2-c002-11e8-a355-529269fb1459");
    public static final UUID SENTENCE_DOCUMENT_TYPE_ID = fromString("460fb0ca-c002-11e8-a355-529269fb1459");
    public static final String PLEA_DOCUMENT_TYPE_DESCRIPTION = "Plea";
    public static final String SENTENCE_DOCUMENT_TYPE_DESCRIPTION = "Sentence";

    private static final List<String> pleaDocumentOriginatingSources = asList("CompanyOnlinePlea",
            "IndividualOnlinePlea",
            "CompanyFinanceData",
            "IndividualFinanceData");
    private static final List<String> pleaFinancialDocumentOriginatingSources = asList(
            "CompanyFinanceData",
            "IndividualFinanceData");


    private OnlinePleaProcessorHelper() {
    }

    public static boolean isForPleaDocument(final String originatingSource) {
        return pleaDocumentOriginatingSources.contains(originatingSource);
    }

    public static boolean isForPleaFinancialDocument(final String originatingSource) {
        return pleaFinancialDocumentOriginatingSources.contains(originatingSource);
    }

}
