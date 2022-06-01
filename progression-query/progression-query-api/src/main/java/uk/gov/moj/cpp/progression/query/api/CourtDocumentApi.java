package uk.gov.moj.cpp.progression.query.api;

import static uk.gov.moj.cpp.progression.query.api.helper.ProgressionQueryHelper.isPermitted;

import uk.gov.justice.api.resource.service.DefenceQueryService;
import uk.gov.justice.services.adapter.rest.exception.BadRequestException;
import uk.gov.justice.services.common.exception.ForbiddenRequestException;
import uk.gov.justice.services.core.annotation.Component;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.core.requester.Requester;
import uk.gov.justice.services.messaging.JsonEnvelope;

import javax.inject.Inject;

@ServiceComponent(Component.QUERY_API)
public class CourtDocumentApi {

    private static final String DEFENDANT_ID = "defendantId";
    private static final String CASE_ID = "caseId";
    public static final String MATERIAL_CONTENT_FOR_PROSECUTION = "progression.query.material-content-for-prosecution";
    public static final String APPLICATION_ID = "applicationId";

    @Inject
    private UserDetailsLoader userDetailsLoader;

    @Inject
    private Enveloper enveloper;

    @Inject
    private Requester requester;

    @Inject
    private DefenceQueryService defenceQueryService;

    @Handles("progression.query.material-metadata")
    public JsonEnvelope getCaseDocumentMetadata(final JsonEnvelope query) {
        return requester.requestAsAdmin(enveloper.withMetadataFrom(query, "material.query.material-metadata").apply(query.payloadAsJsonObject()));

    }

    /**
     * Handler returns document details and not document content. This is consequence of non
     * framework endpoint which uses standard framework interceptors. Handler is invoked at the end
     * of programmatically invoked interceptor chain, see DefaultQueryApiCasesCaseIdDocumentsDocumentIdContentResource.
     */
    @Handles("progression.query.material-content")
    public JsonEnvelope getCaseDocumentDetails(final JsonEnvelope envelope) {

        return envelope;

    }

    @Handles("progression.query.material-content-for-defence")
    public JsonEnvelope getCaseDocumentDetailsForDefence(final JsonEnvelope envelope) {

        if (!envelope.payloadAsJsonObject().containsKey(APPLICATION_ID) && envelope.payloadAsJsonObject().containsKey(DEFENDANT_ID) && !isPermitted(envelope, userDetailsLoader, requester, envelope.payloadAsJsonObject().getString(DEFENDANT_ID))) {
            throw new ForbiddenRequestException("User has neither associated or granted permission to view");
        }

        return envelope;

    }

    @Handles(MATERIAL_CONTENT_FOR_PROSECUTION)
    public JsonEnvelope getCaseDocumentDetailsForProsecution(final JsonEnvelope envelope) {

        if (!envelope.payloadAsJsonObject().containsKey(CASE_ID) && !envelope.payloadAsJsonObject().containsKey(APPLICATION_ID)) {
            throw new BadRequestException(String.format("%s no caseId or applicationId search parameter specified ", MATERIAL_CONTENT_FOR_PROSECUTION));
        }

        if (!envelope.payloadAsJsonObject().containsKey(APPLICATION_ID) && !defenceQueryService.isUserProsecutingCase(envelope, envelope.payloadAsJsonObject().getString(CASE_ID))) {
            throw new ForbiddenRequestException("Forbidden!! Cannot view court documents, user not prosecuting the case");
        }

        return envelope;
    }


}
