package uk.gov.justice.api.resource.utils;

import static java.lang.String.format;
import static org.apache.commons.lang3.StringUtils.EMPTY;
import static org.apache.commons.lang3.StringUtils.isNotBlank;

import javax.json.JsonObject;

public class RequestedNameMapper {

    private static final String REQUESTED_NAME = "requestedName";
    private static final String TITLE_PREFIX = "titlePrefix";
    private static final String SURNAME = "surname";
    private static final String TITLE_JUDICIAL_PREFIX = "titleJudicialPrefix";
    private static final String TITLE_SUFFIX = "titleSuffix";

    public String getRequestedJudgeName(final JsonObject judiciary) {
        final String requestedName = judiciary.getString(REQUESTED_NAME, EMPTY);
        if (isNotBlank(requestedName)) {
            return requestedName;
        }
        return format("%s %s %s", judiciary.getString(TITLE_JUDICIAL_PREFIX, judiciary.getString(TITLE_PREFIX, EMPTY)), judiciary.getString(SURNAME),
                judiciary.getString(TITLE_SUFFIX, EMPTY)).trim();
    }

}

