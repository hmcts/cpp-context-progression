package uk.gov.moj.cpp.progression.util;

import com.jayway.restassured.response.Response;
import uk.gov.moj.cpp.progression.helper.AbstractTestHelper;

import java.io.IOException;

import static uk.gov.moj.cpp.progression.helper.RestHelper.postCommand;
import static uk.gov.moj.cpp.progression.util.FileUtil.getPayload;

public class PleadOnlineHelper extends AbstractTestHelper {

    public Response submitOnlinePlea(final String caseId, final String defendantId, final String resource) throws IOException {

        final String commandUri = getWriteUrl("/cases/" + caseId + "/defendants/" + defendantId + "/plead-online");
        return postCommand(commandUri,
                "application/vnd.progression.plead-online+json",
                getPayload(resource));
    }
}
