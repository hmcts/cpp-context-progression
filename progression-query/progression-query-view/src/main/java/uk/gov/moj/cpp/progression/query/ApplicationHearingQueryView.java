package uk.gov.moj.cpp.progression.query;


import static java.util.Objects.nonNull;
import static java.util.Optional.ofNullable;
import static java.util.UUID.fromString;
import static org.apache.commons.lang3.StringUtils.isNotEmpty;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.moj.cpp.progression.query.view.service.HearingAtAGlanceService.getDefendantName;

import uk.gov.justice.core.courts.CourtApplication;
import uk.gov.justice.core.courts.Hearing;
import uk.gov.justice.core.courts.MasterDefendant;
import uk.gov.justice.core.courts.ProsecutionCase;
import uk.gov.justice.core.courts.ProsecutionCaseIdentifier;
import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.common.converter.ObjectToJsonObjectConverter;
import uk.gov.justice.services.common.converter.StringToJsonObjectConverter;
import uk.gov.justice.services.core.annotation.Component;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.messaging.JsonEnvelope;

import uk.gov.moj.cpp.prosecutioncase.persistence.entity.HearingEntity;
import uk.gov.moj.cpp.prosecutioncase.persistence.entity.ProsecutionCaseEntity;
import uk.gov.moj.cpp.prosecutioncase.persistence.repository.HearingRepository;
import uk.gov.moj.cpp.prosecutioncase.persistence.repository.ProsecutionCaseRepository;


import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import javax.inject.Inject;
import javax.json.Json;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;


@ServiceComponent(Component.QUERY_VIEW)
public class ApplicationHearingQueryView {


    private static final String CASE_ID = "caseId";
    private static final String CASE_URN = "caseURN";
    private static final String CASE_STATUS = "caseStatus";
    private static final String COURT_APPLICATION_CASES_SUMMARY = "courtApplicationCasesSummary";
    private static final String COURT_ORDER_CASES_SUMMARY = "courtOrderCasesSummary";

    @Inject
    private HearingRepository hearingRepository;

    @Inject
    private ProsecutionCaseRepository prosecutionCaseRepository;

    @Inject
    private StringToJsonObjectConverter stringToJsonObjectConverter;

    @Inject
    private JsonObjectToObjectConverter jsonObjectToObjectConverter;

    @Inject
    private ObjectToJsonObjectConverter objectToJsonObjectConverter;

    @Handles("progression.query.application-hearing-case-details")
    public JsonEnvelope getApplicationHearingCaseDetails(final JsonEnvelope envelope) {
        final JsonObject payload = envelope.payloadAsJsonObject();
        final UUID applicationId = fromString(payload.getString("applicationId"));
        final UUID hearingId = fromString(payload.getString("hearingId"));

        final HearingEntity hearingEntity = hearingRepository.findBy(hearingId);
        final Hearing hearing = jsonObjectToObjectConverter.convert(stringToJsonObjectConverter.convert(hearingEntity.getPayload()), Hearing.class);

        final Optional<CourtApplication> optionalCourtApplication = ofNullable(hearing.getCourtApplications()).orElse(Collections.emptyList()).stream()
                .filter(c -> c.getId().equals(applicationId))
                .findFirst();
        final JsonObjectBuilder responseBuilder = Json.createObjectBuilder();
        if (optionalCourtApplication.isPresent()) {
            final CourtApplication courtApplication = optionalCourtApplication.get();
            responseBuilder.add("hearingId", hearingId.toString())
                    .add("applicationId", applicationId.toString())
                    .add("hearingType", objectToJsonObjectConverter.convert(hearing.getType()));
            getIsBoxHearing(hearing, responseBuilder);
            getMasterDefendant(courtApplication, responseBuilder);
            getCourtApplicationCasesSummary(courtApplication, responseBuilder);
            getCourtOrderCasesSummary(courtApplication, responseBuilder);
        }


        return envelopeFrom(
                envelope.metadata(), responseBuilder.build());
    }

    private static void getIsBoxHearing(final Hearing hearing, final JsonObjectBuilder responseBuilder) {
        if (nonNull(hearing.getIsBoxHearing())) {
            responseBuilder.add("isBoxHearing", hearing.getIsBoxHearing());
        }
    }

    private static void getMasterDefendant(final CourtApplication courtApplication, final JsonObjectBuilder responseBuilder) {
        if (nonNull(courtApplication.getSubject()) && nonNull(courtApplication.getSubject().getMasterDefendant())) {
            final MasterDefendant masterDefendant = courtApplication.getSubject().getMasterDefendant();
            responseBuilder.add("masterDefendantId", masterDefendant.getMasterDefendantId().toString())
                    .add("masterDefendantName", getDefendantName(masterDefendant.getPersonDefendant(), masterDefendant.getLegalEntityDefendant()));
        }
    }

    private void getCourtOrderCasesSummary(final CourtApplication courtApplication, final JsonObjectBuilder responseBuilder) {
        if (nonNull(courtApplication.getCourtOrder())) {
            final JsonArrayBuilder caseDetails = Json.createArrayBuilder();
            final List<UUID> addedCaseIds = new ArrayList<>();
            courtApplication.getCourtOrder().getCourtOrderOffences().forEach(courtOrderOffence -> {
                        if (!addedCaseIds.contains(courtOrderOffence.getProsecutionCaseId())) {
                            caseDetails.add(
                                    Json.createObjectBuilder()
                                            .add(CASE_ID, courtOrderOffence.getProsecutionCaseId().toString())
                                            .add(CASE_URN, getCaseURN(courtOrderOffence.getProsecutionCaseIdentifier()))
                                            .add(CASE_STATUS, getCaseStatus(courtOrderOffence.getProsecutionCaseId()))
                                            .build());
                            addedCaseIds.add(courtOrderOffence.getProsecutionCaseId());
                        }
                    }
            );
            responseBuilder.add(COURT_ORDER_CASES_SUMMARY, caseDetails);
        }
    }

    private void getCourtApplicationCasesSummary(final CourtApplication courtApplication, final JsonObjectBuilder responseBuilder) {
        if (nonNull(courtApplication.getCourtApplicationCases())) {
            final JsonArrayBuilder caseDetails = Json.createArrayBuilder();
            courtApplication.getCourtApplicationCases().forEach(courtApplicationCase ->
                    caseDetails.add(
                            Json.createObjectBuilder()
                                    .add(CASE_ID, courtApplicationCase.getProsecutionCaseId().toString())
                                    .add(CASE_URN, getCaseURN(courtApplicationCase.getProsecutionCaseIdentifier()))
                                    .add(CASE_STATUS, courtApplicationCase.getCaseStatus())
                                    .build()
                    ));
            responseBuilder.add(COURT_APPLICATION_CASES_SUMMARY, caseDetails);
        }
    }

    private String getCaseURN(final ProsecutionCaseIdentifier prosecutionCaseIdentifier) {
        return isNotEmpty(prosecutionCaseIdentifier.getCaseURN()) ? prosecutionCaseIdentifier.getCaseURN() : prosecutionCaseIdentifier.getProsecutionAuthorityReference();
    }

    private String getCaseStatus(final UUID caseId) {
        final ProsecutionCaseEntity prosecutionCaseEntity = prosecutionCaseRepository.findByCaseId(caseId);
        final ProsecutionCase prosecutionCase = jsonObjectToObjectConverter.convert(stringToJsonObjectConverter.convert(prosecutionCaseEntity.getPayload()), ProsecutionCase.class);
        return prosecutionCase.getCaseStatus();

    }

}
