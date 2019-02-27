package uk.gov.moj.cpp.progression.query.view;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import javax.inject.Inject;
import javax.json.Json;

import uk.gov.justice.services.common.converter.ListToJsonArrayConverter;
import uk.gov.justice.services.core.annotation.Component;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.messaging.JsonObjects;
import uk.gov.moj.cpp.progression.query.view.response.CaseProgressionDetailView;
import uk.gov.moj.cpp.progression.query.view.service.CaseProgressionDetailService;
import uk.gov.moj.cpp.progression.query.view.service.OffencesService;
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
    static final String FIELD_DEFENDANT_ID = "defendantId";
    static final String FIELD_STATUS = "status";
    static final String FIELD_URN = "urn";
    static final String CASE_PROGRESSION_DETAILS_RESPONSE ="progression.query.caseprogressiondetails-response";
    static final String CASES_RESPONSE_LIST = "progression.query.cases-response-list";
    static final String DEFENDANT_RESPONSE_LIST = "progression.query.defendant-response-list";
    static final String DEFENDANT_RESPONSE = "progression.query.defendant-response";
    static final String DEFENDANT_DOCUMENT_RESPONSE = "progression.query.defendant.document-response";
    private static final String NAME_RESPONSE_DEFENDANT_OFFENCES = "progression.query.defendant-offences-response";
    private static final String NAME_RESPONSE_CASES_SEARCH_BY_MATERIAL_ID = "progression.query.cases-search-by-material-id-response";
    static final String FIELD_QUERY = "q";

    @Inject
    Enveloper enveloper;
    @Inject
    private CaseProgressionDetailService caseProgressionDetailService;
    @Inject
    private ListToJsonArrayConverter listToJsonArrayConverter;

    @Inject
    OffencesService offencesService;

    @Handles("progression.query.caseprogressiondetail")
    public JsonEnvelope getCaseProgressionDetails(final JsonEnvelope envelope) {
        final Optional<UUID> caseId =
                JsonObjects.getUUID(envelope.payloadAsJsonObject(), FIELD_CASE_ID);

        return enveloper.withMetadataFrom(envelope, CASE_PROGRESSION_DETAILS_RESPONSE)
                .apply(caseProgressionDetailService.getCaseProgressionDetail(caseId.get()));
    }

    @Handles("progression.query.cases")
    public JsonEnvelope getCases(final JsonEnvelope envelope) {
        final Optional<UUID> caseId =
                JsonObjects.getUUID(envelope.payloadAsJsonObject(), FIELD_CASE_ID);
        final Optional<String> status =
                JsonObjects.getString(envelope.payloadAsJsonObject(), FIELD_STATUS);

        List<CaseProgressionDetailView> casesView = new ArrayList<>();
        if (caseId.isPresent() && status.isPresent()) {
            casesView = caseProgressionDetailService.getCases(status, caseId);
        } else if (caseId.isPresent()) {
            casesView.add(caseProgressionDetailService.getCaseProgressionDetail(caseId.get()));
        } else {
            casesView = caseProgressionDetailService.getCases(status);
        }

        return enveloper.withMetadataFrom(envelope, CASES_RESPONSE_LIST)
                .apply(Json.createObjectBuilder()
                        .add("cases", listToJsonArrayConverter
                                .convert(casesView))
                        .build());
    }

    @Handles("progression.query.case-by-urn")
    public JsonEnvelope findCaseByUrn(final JsonEnvelope envelope) {
        return enveloper.withMetadataFrom(envelope, CASES_RESPONSE_LIST).apply( caseProgressionDetailService.findCaseByCaseUrn(envelope.payloadAsJsonObject().getString(FIELD_URN)));
    }

    @Handles("progression.query.defendant")
    public JsonEnvelope getDefendant(final JsonEnvelope envelope) {
        final Optional<String> defendantId =
                JsonObjects.getString(envelope.payloadAsJsonObject(), FIELD_DEFENDANT_ID);
        return enveloper.withMetadataFrom(envelope, DEFENDANT_RESPONSE).apply(caseProgressionDetailService.getDefendant(defendantId));
    }

    @Handles("progression.query.defendant.document")
    public JsonEnvelope getDefendantDocument(final JsonEnvelope envelope) {
        final Optional<String> caseId = JsonObjects.getString(envelope.payloadAsJsonObject(), "caseId");
        final Optional<String> defendantId = JsonObjects.getString(envelope.payloadAsJsonObject(), "defendantId");
        return enveloper.withMetadataFrom(envelope, DEFENDANT_DOCUMENT_RESPONSE).apply(caseProgressionDetailService.getDefendantDocument(caseId, defendantId));
    }

    @Handles("progression.query.defendants")
    public JsonEnvelope getDefendants(final JsonEnvelope envelope) {
        final Optional<UUID> caseId =
                JsonObjects.getUUID(envelope.payloadAsJsonObject(), FIELD_CASE_ID);

        return enveloper.withMetadataFrom(envelope, DEFENDANT_RESPONSE_LIST)
                .apply(caseProgressionDetailService.getDefendantsByCase(caseId.get()));
    }

    @Handles("progression.query.defendant-offences")
    public JsonEnvelope findOffences(final JsonEnvelope envelope) {
        return enveloper.withMetadataFrom(envelope, NAME_RESPONSE_DEFENDANT_OFFENCES).apply(
                offencesService.findOffences(envelope.payloadAsJsonObject().getString(FIELD_CASE_ID), envelope.payloadAsJsonObject().getString(FIELD_DEFENDANT_ID)));
}

    @Handles("progression.query.cases-search-by-material-id")
    public JsonEnvelope searchCaseByMaterialId(final JsonEnvelope envelope) {
        return enveloper.withMetadataFrom(envelope, NAME_RESPONSE_CASES_SEARCH_BY_MATERIAL_ID).apply(
                caseProgressionDetailService.searchCaseByMaterialId(envelope.payloadAsJsonObject().getString(FIELD_QUERY)));

    }

}
