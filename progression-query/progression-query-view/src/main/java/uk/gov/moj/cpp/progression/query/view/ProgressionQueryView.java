package uk.gov.moj.cpp.progression.query.view;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.json.Json;
import javax.persistence.NoResultException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;

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
import uk.gov.moj.cpp.progression.persistence.entity.IndicateStatement;
import uk.gov.moj.cpp.progression.query.view.converter.CaseProgressionDetailToViewConverter;
import uk.gov.moj.cpp.progression.query.view.converter.DefendantToDefendantViewConverter;
import uk.gov.moj.cpp.progression.query.view.converter.IndicateStatementsDetailToViewConverter;
import uk.gov.moj.cpp.progression.query.view.converter.TimelineDateToTimeLineDateViewConverter;
import uk.gov.moj.cpp.progression.query.view.response.CaseProgressionDetailView;
import uk.gov.moj.cpp.progression.query.view.response.DefendantView;
import uk.gov.moj.cpp.progression.query.view.response.IndicateStatementsDetailView;
import uk.gov.moj.cpp.progression.query.view.response.TimeLineDateView;
import uk.gov.moj.cpp.progression.query.view.service.CaseProgressionDetailService;
import uk.gov.moj.cpp.progression.query.view.service.IndicateStatementsDetailService;

@ServiceComponent(Component.QUERY_VIEW)
public class ProgressionQueryView {

    static final String DEFENDANT_ID = "defendantId";

    static final Logger logger = LoggerFactory.getLogger(ProgressionQueryView.class);

    static final String FIELD_CASE_ID = "caseId";
    static final String FIELD_INDICATE_STATEMENT_ID = "indicatestatementId";
    static final String TIMELINE_RESPONSE = "progression.query.timeline-response";
    static final String CASE_PROGRESSION_DETAILS_RESPONSE =
                    "progression.query.caseprogressiondetails-response";
    static final String INDICATE_STATEMENT_RESPONSE_LIST =
                    "progression.query.indicatestatement-response-list";
    static final String INDICATE_STATEMENT_RESPONSE =
                    "progression.query.indicatestatement-response";
    static final String FIELD_STATUS = "status";
    static final String CASES_RESPONSE_LIST = "progression.query.cases-response-list";
    static final String DEFENDANT_RESPONSE_LIST = "progression.query.defendant-response-list";
    static final String DEFENDANT_RESPONSE = "progression.query.defendant-response";

    @Inject
    private CaseProgressionDetailService caseProgressionDetailService;

    @Inject
    private CaseProgressionDetailToViewConverter caseProgressionDetailToViewConverter;

    @Inject
    private DefendantToDefendantViewConverter defendantToDefendantViewConverter;

    @Inject
    private TimelineDateToTimeLineDateViewConverter timelineDateToTimeLineDateVOConverter;

    @Inject
    StringToJsonObjectConverter stringToJsonObjectConverter;

    @Inject
    ObjectMapper objectMapper;

    @Inject
    Enveloper enveloper;

    @Inject
    private ListToJsonArrayConverter listToJsonArrayConverter;

    @Inject
    private IndicateStatementsDetailToViewConverter indicateStatementsDetailToVOConverter;

    @Inject
    private IndicateStatementsDetailService indicateStatementsDetailService;

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

    @Handles("progression.query.timeline")
    public JsonEnvelope getTimeLineForProgression(final JsonEnvelope envelope) {
        final Optional<UUID> caseId =
                        JsonObjects.getUUID(envelope.payloadAsJsonObject(), FIELD_CASE_ID);

        final CaseProgressionDetail caseProgressionDetail;
        try {
            caseProgressionDetail =
                            caseProgressionDetailService.getCaseProgressionDetail(caseId.get());
        } catch (final NoResultException nre) {
            logger.error("No CaseProgressionDetail found for caseId: "+ caseId, nre);
            return enveloper.withMetadataFrom(envelope, TIMELINE_RESPONSE).apply(null);
        }
        final List<TimeLineDateView> listTimeLineDateVO =
                        caseProgressionDetail.getTimeLine().stream()
                                        .map(timeLineDate -> timelineDateToTimeLineDateVOConverter
                                                        .convert(timeLineDate))
                                        .collect(Collectors.toList());
        return enveloper.withMetadataFrom(envelope,
                        TIMELINE_RESPONSE).apply(Json
                                        .createObjectBuilder().add("timeline",
                                                        listToJsonArrayConverter
                                                                        .convert(listTimeLineDateVO))
                                        .build());

    }

    @Handles("progression.query.indicatestatementsdetails")
    public JsonEnvelope getIndicatestatementsdetails(final JsonEnvelope envelope) {
        final Optional<UUID> caseId =
                        JsonObjects.getUUID(envelope.payloadAsJsonObject(), FIELD_CASE_ID);
        final List<IndicateStatement> indicateStatements =
                        indicateStatementsDetailService.getIndicateStatements(caseId.get());
        final List<IndicateStatementsDetailView> listIndicateStatementsDetailVO =
                        indicateStatements.stream()
                                        .map(indicateStatementObj -> indicateStatementsDetailToVOConverter
                                                        .convert(indicateStatementObj))
                                        .collect(Collectors.toList());
        return enveloper.withMetadataFrom(envelope, INDICATE_STATEMENT_RESPONSE_LIST)
                        .apply(Json.createObjectBuilder()
                                        .add("indicatestatements",
                                                        listToJsonArrayConverter
                                                                        .convert(listIndicateStatementsDetailVO))
                                        .build());

    }

    @Handles("progression.query.indicatestatementsdetail")
    public JsonEnvelope getIndicatestatementsdetail(final JsonEnvelope envelope) {
        final Optional<UUID> statementId = JsonObjects.getUUID(envelope.payloadAsJsonObject(),
                        FIELD_INDICATE_STATEMENT_ID);
        final Optional<IndicateStatement> indicateStatement =
                        indicateStatementsDetailService.getIndicateStatementById(statementId.get());
        final IndicateStatementsDetailView indicateStatementView =
                        indicateStatementsDetailToVOConverter.convert(indicateStatement.get());
        return enveloper.withMetadataFrom(envelope, INDICATE_STATEMENT_RESPONSE)
                        .apply(indicateStatementView);

    }

    @Handles("progression.query.cases")
    public JsonEnvelope getCases(final JsonEnvelope envelope) {
        final Optional<String> status =
                        JsonObjects.getString(envelope.payloadAsJsonObject(), FIELD_STATUS);

        final List<CaseProgressionDetail> cases = caseProgressionDetailService.getCases(status);

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

    @Handles("progression.query.defendants")
    public JsonEnvelope getDefendants(final JsonEnvelope envelope) {
        final Optional<UUID> caseId =
                        JsonObjects.getUUID(envelope.payloadAsJsonObject(), FIELD_CASE_ID);

        final List<Defendant> defendants =
                        caseProgressionDetailService.getDefendantsByCase(caseId.get());

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
