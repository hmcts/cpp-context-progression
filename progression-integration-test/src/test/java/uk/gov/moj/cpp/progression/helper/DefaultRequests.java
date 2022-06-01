package uk.gov.moj.cpp.progression.helper;


import static java.lang.String.format;
import static uk.gov.justice.services.test.utils.core.http.RequestParamsBuilder.requestParams;
import static uk.gov.moj.cpp.progression.helper.AbstractTestHelper.USER_ID;
import static uk.gov.moj.cpp.progression.helper.AbstractTestHelper.getReadUrl;

import uk.gov.justice.services.common.http.HeaderConstants;
import uk.gov.justice.services.test.utils.core.http.RequestParamsBuilder;


public class DefaultRequests {
    public static final String GET_CASE_DEFENDANTS = "application/vnd.progression.query.defendants+json";
    public static final String GET_DEFENDANT_BY_DEFENDANT_ID = "application/vnd.progression.query.defendant+json";
    public static final String GET_CASE_BY_URN_MEDIA_TYPE = "application/vnd.progression.query.case-by-urn+json";
    public static final String GET_OFFENCES_BY_CASE_ID_MEDIA_TYPE = "application/vnd.progression.query.defendant-offences+json";
    public static final String PROGRESSION_QUERY_PROSECUTION_CASE_JSON = "application/vnd.progression.query.prosecutioncase+json";
    public static final String PROGRESSION_QUERY_PROSECUTION_CASE_CAAG_JSON = "application/vnd.progression.query.prosecutioncase.caag+json";
    public static final String PROGRESSION_QUERY_APPLICATION_AAAG_JSON = "application/vnd.progression.query.application.aaag+json";
    public static final String PROGRESSION_QUERY_APPLICATION_AAAG_FOR_DEFENCE_JSON = "application/vnd.progression.query.application.aaag-for-defence+json";


    public static RequestParamsBuilder getCaseByUrn(final String caseUrn) {
        return getCaseByUrn(caseUrn, USER_ID);
    }

    public static RequestParamsBuilder getCaseByUrn(final String caseUrn, final String userId) {
        return requestParams(getReadUrl("/cases?urn=" + caseUrn), GET_CASE_BY_URN_MEDIA_TYPE)
                .withHeader(HeaderConstants.USER_ID, userId);
    }

    public static RequestParamsBuilder getDefendantsByCaseId(final String caseId) {
        return requestParams(getReadUrl(format("/cases/%s/defendants", caseId)), GET_CASE_DEFENDANTS)
                .withHeader(HeaderConstants.USER_ID, USER_ID);
    }

    public static RequestParamsBuilder getOffencesForDefendantId(final String caseId, final String defendantId) {
        return requestParams(getReadUrl(format("/cases/%s/defendant/%s/offences", caseId, defendantId)), GET_OFFENCES_BY_CASE_ID_MEDIA_TYPE)
                .withHeader(HeaderConstants.USER_ID, USER_ID);
    }


    public static RequestParamsBuilder getDefendantForDefendantId(final String caseId, final String defendantId) {
        return requestParams(getReadUrl(format("/cases/%s/defendants/%s", caseId, defendantId)), GET_DEFENDANT_BY_DEFENDANT_ID)
                .withHeader(HeaderConstants.USER_ID, USER_ID);
    }

}
