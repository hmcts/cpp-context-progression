package uk.gov.moj.cpp.progression.helper;


import uk.gov.justice.services.common.http.HeaderConstants;
import uk.gov.justice.services.test.utils.core.http.RequestParamsBuilder;

import static java.lang.String.format;
import static uk.gov.justice.services.test.utils.core.http.RequestParamsBuilder.requestParams;
import static uk.gov.moj.cpp.progression.helper.AbstractTestHelper.USER_ID;
import static uk.gov.moj.cpp.progression.helper.AbstractTestHelper.getReadUrl;


public class DefaultRequests {
    public static final String GET_CASE_DEFENDANTS = "application/vnd.progression.query.defendants+json";
    public static final String GET_CASE_BY_ID_MEDIA_TYPE = "application/vnd.progression.query.case+json";
    public static final String SEARCH_BY_MATERIAL_ID = "application/vnd.progression.query.cases-search-by-material-id+json";
    public static final String GET_CASE_BY_URN_MEDIA_TYPE = "application/vnd.progression.query.case-by-urn+json";
    public static final String GET_OFFENCES_BY_CASE_ID_MEDIA_TYPE = "application/vnd.progression.query.defendant-offences+json";


    public static RequestParamsBuilder getCaseByUrn(final String caseUrn) {
        return getCaseByUrn(caseUrn , USER_ID);
    }

    public static RequestParamsBuilder getCaseById(final String caseId) {
        return getCaseById(caseId, USER_ID);
    }

    public static RequestParamsBuilder getCaseById(final String caseId, String userId) {
        return requestParams(getReadUrl("/cases/" + caseId), GET_CASE_BY_ID_MEDIA_TYPE)
                .withHeader(HeaderConstants.USER_ID, userId);
    }

    public static RequestParamsBuilder getCaseByUrn(final String caseUrn, String userId) {
        return requestParams(getReadUrl("/cases?urn="+caseUrn ), GET_CASE_BY_URN_MEDIA_TYPE)
                .withHeader(HeaderConstants.USER_ID, userId);
    }

    public static RequestParamsBuilder getDefendantsByCaseId(final String caseId) {
        return requestParams(getReadUrl(format("/cases/%s/defendants", caseId)), GET_CASE_DEFENDANTS)
                .withHeader(HeaderConstants.USER_ID, USER_ID);
    }

    public static RequestParamsBuilder searchByMaterialId(final String materialId) {
        return requestParams(getReadUrl("/search?q="+ materialId), SEARCH_BY_MATERIAL_ID)
                .withHeader(HeaderConstants.USER_ID, USER_ID);
    }

    public static RequestParamsBuilder getOffencesForDefendantId(final String caseId, final String defendantId) {
        return requestParams(getReadUrl(format("/cases/%s/defendant/%s/offences", caseId, defendantId)), GET_OFFENCES_BY_CASE_ID_MEDIA_TYPE)
                .withHeader(HeaderConstants.USER_ID, USER_ID);
    }


}
