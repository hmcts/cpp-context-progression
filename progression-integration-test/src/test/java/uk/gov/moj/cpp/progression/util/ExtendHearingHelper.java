package uk.gov.moj.cpp.progression.util;

import uk.gov.moj.cpp.progression.helper.AbstractTestHelper;
import uk.gov.moj.cpp.progression.helper.QueueUtil;

public class ExtendHearingHelper extends AbstractTestHelper {

    public ExtendHearingHelper() {
        privateEventsConsumer = QueueUtil.privateEvents.createConsumer("listing.command.list-court-hearing");
    }
}
