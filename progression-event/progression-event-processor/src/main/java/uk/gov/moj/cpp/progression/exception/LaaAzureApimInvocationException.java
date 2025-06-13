package uk.gov.moj.cpp.progression.exception;

import java.util.List;
import java.util.UUID;

public class LaaAzureApimInvocationException extends RuntimeException  {

    public LaaAzureApimInvocationException(List<UUID> defendantList, String hearingId, String url) {
        super("Error: Something wrong with Azure APIM invocation with url: " + url + " for proceedings concluded for defendant(s): "+ defendantList + " in hearing: [" + hearingId + "]");
    }

    public LaaAzureApimInvocationException(final UUID applicationId, final UUID hearingId, final String url) {
        super("Error: Something wrong with Azure APIM invocation with url: " + url + " for application proceedings concluded. applicationId: "+ applicationId + " in hearing: [" + hearingId + "]");
    }

}
