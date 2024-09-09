package uk.gov.moj.cpp.progression.query.api;

import static java.util.Objects.nonNull;
import static java.util.UUID.fromString;
import static uk.gov.moj.cpp.progression.query.api.helper.ProgressionQueryHelper.isPermitted;

import uk.gov.justice.api.resource.service.DefenceQueryService;
import uk.gov.justice.core.courts.ProsecutionCase;
import uk.gov.justice.services.adapter.rest.exception.BadRequestException;
import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.common.converter.StringToJsonObjectConverter;
import uk.gov.justice.services.common.exception.ForbiddenRequestException;
import uk.gov.justice.services.core.annotation.Component;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.core.requester.Requester;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.progression.query.ProsecutionCaseQuery;
import uk.gov.moj.cpp.progression.query.api.service.UsersGroupQueryService;
import uk.gov.moj.cpp.prosecutioncase.persistence.repository.ProsecutionCaseRepository;

import java.util.Optional;
import java.util.UUID;

import javax.inject.Inject;
import javax.json.JsonObject;

@ServiceComponent(Component.QUERY_API)
public class CourtDocumentApi {

    private static final String DEFENDANT_ID = "defendantId";
    private static final String CASE_ID = "caseId";
    public static final String MATERIAL_CONTENT_FOR_PROSECUTION = "progression.query.material-content-for-prosecution";
    public static final String APPLICATION_ID = "applicationId";
    public static final String NON_CPS_PROSECUTORS = "Non CPS Prosecutors";
    public static final String ORGANISATION_MIS_MATCH = "OrganisationMisMatch";

    @Inject
    private UsersGroupQueryService usersGroupQueryService;

    @Inject
    private ProsecutionCaseRepository prosecutionCaseRepository;

    @Inject
    private StringToJsonObjectConverter stringToJsonObjectConverter;

    @Inject
    private ProsecutionCaseQuery prosecutionCaseQuery;

    @Inject
    private JsonObjectToObjectConverter jsonObjectToObjectConverter;

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

    @SuppressWarnings("squid:S3655")
    @Handles(MATERIAL_CONTENT_FOR_PROSECUTION)
    public JsonEnvelope getCaseDocumentDetailsForProsecution(final JsonEnvelope envelope) {

        boolean isProsecutingCase = true;
        if (!envelope.payloadAsJsonObject().containsKey(CASE_ID) && !envelope.payloadAsJsonObject().containsKey(APPLICATION_ID)) {
            throw new BadRequestException(String.format("%s no caseId or applicationId search parameter specified ", MATERIAL_CONTENT_FOR_PROSECUTION));
        }

        final UUID userId = fromString(envelope.metadata().userId().orElseThrow(() -> new RuntimeException("UserId missing from query.")));
        final JsonEnvelope appQueryResponse = prosecutionCaseQuery.getProsecutionCase(envelope);
        final JsonObject prosecutionCase = appQueryResponse.payloadAsJsonObject().getJsonObject("prosecutionCase");
        final ProsecutionCase prosecutionCaseObj = jsonObjectToObjectConverter.convert(prosecutionCase, ProsecutionCase.class);
        final Optional<String> orgMatch  = usersGroupQueryService.validateNonCPSUserOrg(envelope.metadata(), userId, NON_CPS_PROSECUTORS, getShortName(prosecutionCaseObj));
        if(orgMatch.isPresent()) {
            if (ORGANISATION_MIS_MATCH.equals(orgMatch.get())) {
                throw new ForbiddenRequestException("Forbidden!! Non CPS Prosecutor user cannot view court documents if it is not belongs to the same Prosecuting Authority of the user logged in");
            }
        } else {
            isProsecutingCase = defenceQueryService.isUserProsecutingCase(envelope, envelope.payloadAsJsonObject().getString(CASE_ID));
        }

        if (!envelope.payloadAsJsonObject().containsKey(APPLICATION_ID) && !isProsecutingCase) {
            throw new ForbiddenRequestException("Forbidden!! Cannot view court documents, user not prosecuting the case");
        }

        return envelope;
    }

    private String getShortName(final ProsecutionCase prosecutionCaseObj) {
        return nonNull(prosecutionCaseObj.getProsecutor()) && nonNull(prosecutionCaseObj.getProsecutor().getProsecutorCode()) ?
                prosecutionCaseObj.getProsecutor().getProsecutorCode() :
                prosecutionCaseObj.getProsecutionCaseIdentifier().getProsecutionAuthorityCode();
    }


}
