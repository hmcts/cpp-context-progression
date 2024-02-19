package uk.gov.moj.cpp.progression.query.view;

import java.util.Optional;
import java.util.UUID;

import javax.inject.Inject;

import uk.gov.justice.services.common.converter.ListToJsonArrayConverter;
import uk.gov.justice.services.core.annotation.Component;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.messaging.JsonObjects;
import uk.gov.moj.cpp.progression.query.view.service.CaseProgressionDetailService;
/**
 *
 * @deprecated
 *
 */
@Deprecated

@SuppressWarnings({"squid:S3655", "squid:S1133"})
@ServiceComponent(Component.QUERY_VIEW)
public class ProgressionQueryView {

    static final String FIELD_CASE_ID = "caseId";
    static final String CASE_PROGRESSION_DETAILS_RESPONSE ="progression.query.caseprogressiondetails-response";
    static final String DEFENDANT_DOCUMENT_RESPONSE = "progression.query.defendant.document-response";
    private static final String NAME_RESPONSE_CASES_SEARCH_BY_MATERIAL_ID = "progression.query.cases-search-by-material-id-response";
    static final String FIELD_QUERY = "q";

    @Inject
    Enveloper enveloper;
    @Inject
    private CaseProgressionDetailService caseProgressionDetailService;
    @Inject
    private ListToJsonArrayConverter listToJsonArrayConverter;

    @Handles("progression.query.caseprogressiondetail")
    public JsonEnvelope getCaseProgressionDetails(final JsonEnvelope envelope) {
        final Optional<UUID> caseId =
                JsonObjects.getUUID(envelope.payloadAsJsonObject(), FIELD_CASE_ID);

        return enveloper.withMetadataFrom(envelope, CASE_PROGRESSION_DETAILS_RESPONSE)
                .apply(caseProgressionDetailService.getCaseProgressionDetail(caseId.get()));
    }

    @Handles("progression.query.defendant.document")
    public JsonEnvelope getDefendantDocument(final JsonEnvelope envelope) {
        final Optional<String> caseId = JsonObjects.getString(envelope.payloadAsJsonObject(), "caseId");
        final Optional<String> defendantId = JsonObjects.getString(envelope.payloadAsJsonObject(), "defendantId");
        return enveloper.withMetadataFrom(envelope, DEFENDANT_DOCUMENT_RESPONSE).apply(caseProgressionDetailService.getDefendantDocument(caseId, defendantId));
    }

    @Handles("progression.query.cases-search-by-material-id")
    public JsonEnvelope searchCaseByMaterialId(final JsonEnvelope envelope) {
        return enveloper.withMetadataFrom(envelope, NAME_RESPONSE_CASES_SEARCH_BY_MATERIAL_ID).apply(
                caseProgressionDetailService.searchCaseByMaterialId(envelope.payloadAsJsonObject().getString(FIELD_QUERY)));

    }

}
