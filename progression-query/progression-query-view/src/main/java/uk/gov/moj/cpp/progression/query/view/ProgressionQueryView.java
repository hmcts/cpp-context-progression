package uk.gov.moj.cpp.progression.query.view;

import uk.gov.justice.services.common.converter.ListToJsonArrayConverter;
import uk.gov.justice.services.common.converter.StringToJsonObjectConverter;
import uk.gov.justice.services.core.annotation.Component;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.messaging.JsonObjects;
import uk.gov.moj.cpp.progression.persistence.entity.CaseProgressionDetail;
import uk.gov.moj.cpp.progression.persistence.entity.Defendant;
import uk.gov.moj.cpp.progression.persistence.entity.DefendantDocument;
import uk.gov.moj.cpp.progression.query.view.converter.CaseProgressionDetailToViewConverter;
import uk.gov.moj.cpp.progression.query.view.converter.DefendantToDefendantViewConverter;
import uk.gov.moj.cpp.progression.query.view.response.CaseProgressionDetailView;
import uk.gov.moj.cpp.progression.query.view.response.DefendantDocumentView;
import uk.gov.moj.cpp.progression.query.view.response.DefendantView;
import uk.gov.moj.cpp.progression.query.view.service.CaseProgressionDetailService;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.json.Json;
import javax.persistence.NoResultException;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ServiceComponent(Component.QUERY_VIEW)
public class ProgressionQueryView {

    static final String DEFENDANT_ID = "defendantId";

    static final Logger logger = LoggerFactory.getLogger(ProgressionQueryView.class);

    static final String FIELD_CASE_ID = "caseId";
    static final String CASE_PROGRESSION_DETAILS_RESPONSE =
            "progression.query.caseprogressiondetails-response";
    static final String FIELD_STATUS = "status";
    static final String CASES_RESPONSE_LIST = "progression.query.cases-response-list";
    static final String DEFENDANT_RESPONSE_LIST = "progression.query.defendant-response-list";
    static final String DEFENDANT_RESPONSE = "progression.query.defendant-response";
    static final String DEFENDANT_DOCUMENT_RESPONSE = "progression.query.defendant.document-response";
    @Inject
    StringToJsonObjectConverter stringToJsonObjectConverter;
    @Inject
    ObjectMapper objectMapper;
    @Inject
    Enveloper enveloper;
    @Inject
    private CaseProgressionDetailService caseProgressionDetailService;
    @Inject
    private CaseProgressionDetailToViewConverter caseProgressionDetailToViewConverter;
    @Inject
    private DefendantToDefendantViewConverter defendantToDefendantViewConverter;
    @Inject
    private ListToJsonArrayConverter listToJsonArrayConverter;

    @Handles("progression.query.caseprogressiondetail")
    public JsonEnvelope getCaseProgressionDetails(final JsonEnvelope envelope) {
        final Optional<UUID> caseId =
                JsonObjects.getUUID(envelope.payloadAsJsonObject(), FIELD_CASE_ID);

        final CaseProgressionDetail caseProgressionDetail;
        try {
            caseProgressionDetail =
                    caseProgressionDetailService.getCaseProgressionDetail(caseId.get());
        } catch (final NoResultException nre) {
            logger.error("No CaseProgressionDetail found for caseId: " + caseId, nre);
            return enveloper.withMetadataFrom(envelope, CASE_PROGRESSION_DETAILS_RESPONSE)
                    .apply(null);
        }

        return enveloper.withMetadataFrom(envelope, CASE_PROGRESSION_DETAILS_RESPONSE)
                .apply(caseProgressionDetailToViewConverter.convert(caseProgressionDetail));
    }

    @Handles("progression.query.cases")
    public JsonEnvelope getCases(final JsonEnvelope envelope) {
        final Optional<UUID> caseId =
                JsonObjects.getUUID(envelope.payloadAsJsonObject(), FIELD_CASE_ID);
        final Optional<String> status =
                JsonObjects.getString(envelope.payloadAsJsonObject(), FIELD_STATUS);

        List<CaseProgressionDetail> cases = new ArrayList<CaseProgressionDetail>();
        if(caseId.isPresent() && status.isPresent() ){
            cases = caseProgressionDetailService.getCases(status,caseId);
        }else if(caseId.isPresent()){
            try {
                CaseProgressionDetail caseProgressionDetail =
                        caseProgressionDetailService.getCaseProgressionDetail(caseId.get());
                cases.add(caseProgressionDetail);
            } catch (final NoResultException nre) {
                logger.error("No CaseProgressionDetail found for caseId: " + caseId, nre);
            }
        }else {
            cases = caseProgressionDetailService.getCases(status);
        }

        final List<CaseProgressionDetailView> caseProgressionDetailView = cases.stream()
                .map(caseProgressionDetail -> caseProgressionDetailToViewConverter
                        .convert(caseProgressionDetail))
                .collect(Collectors.toList());

        return enveloper.withMetadataFrom(envelope, CASES_RESPONSE_LIST)
                .apply(Json.createObjectBuilder()
                        .add("cases", listToJsonArrayConverter
                                .convert(caseProgressionDetailView))
                        .build());

    }

    @Handles("progression.query.defendant")
    public JsonEnvelope getDefendant(final JsonEnvelope envelope) {
        final Optional<String> defendantId =
                JsonObjects.getString(envelope.payloadAsJsonObject(), DEFENDANT_ID);

        final Optional<Defendant> defendant =
                caseProgressionDetailService.getDefendant(defendantId);

        DefendantView defendantView = null;

        if (defendant.isPresent()) {
            defendantView = defendantToDefendantViewConverter.convert(defendant.get());
        }

        return enveloper.withMetadataFrom(envelope, DEFENDANT_RESPONSE).apply(defendantView);

    }

    @Handles("progression.query.defendant.document")
    public JsonEnvelope getDefendantDocument(final JsonEnvelope envelope) {
        final Optional<String> caseId = JsonObjects.getString(envelope.payloadAsJsonObject(), "caseId");
        final Optional<String> defendantId = JsonObjects.getString(envelope.payloadAsJsonObject(), "defendantId");

        Optional<DefendantDocument> defendantDocument = caseProgressionDetailService.getDefendantDocument(caseId, defendantId);


        DefendantDocumentView defendantDocumentView = null;

        if (defendantDocument.isPresent()) {
            defendantDocumentView =
                    new DefendantDocumentView(defendantDocument.get().getFileId(),
                            defendantDocument.get().getFileName(), defendantDocument.get().getLastModified());
        }

        return enveloper.withMetadataFrom(envelope, DEFENDANT_DOCUMENT_RESPONSE).apply(defendantDocumentView);

    }

    @Handles("progression.query.defendants")
    public JsonEnvelope getDefendants(final JsonEnvelope envelope) {
        final Optional<UUID> caseId =
                JsonObjects.getUUID(envelope.payloadAsJsonObject(), FIELD_CASE_ID);

        final List<Defendant> defendants;
        try {
            defendants = caseProgressionDetailService.getDefendantsByCase(caseId.get());
        } catch (final NoResultException nre) {
            logger.error("No CaseProgressionDetail found for caseId: " + caseId, nre);
            return enveloper.withMetadataFrom(envelope, CASE_PROGRESSION_DETAILS_RESPONSE)
                    .apply(null);
        }

        final List<DefendantView> defendentView = defendants.stream()
                .map(caseProgressionDetail -> defendantToDefendantViewConverter
                        .convert(caseProgressionDetail))
                .collect(Collectors.toList());

        return enveloper.withMetadataFrom(envelope, DEFENDANT_RESPONSE_LIST)
                .apply(Json.createObjectBuilder()
                        .add("defendants",
                                listToJsonArrayConverter
                                        .convert(defendentView))
                        .build());

    }

}
