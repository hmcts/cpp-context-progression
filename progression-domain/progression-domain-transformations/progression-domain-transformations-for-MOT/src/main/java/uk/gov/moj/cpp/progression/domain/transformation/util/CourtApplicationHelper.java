package uk.gov.moj.cpp.progression.domain.transformation.util;

import static javax.json.Json.createObjectBuilder;
import static uk.gov.moj.cpp.progression.domain.transformation.util.ApplicantHelper.transformApplicant;
import static uk.gov.moj.cpp.progression.domain.transformation.util.CommonHelper.APPLICANT;
import static uk.gov.moj.cpp.progression.domain.transformation.util.CommonHelper.APPLICATION_DECISION_SOUGHT_BY_DATE;
import static uk.gov.moj.cpp.progression.domain.transformation.util.CommonHelper.APPLICATION_PARTICULARS;
import static uk.gov.moj.cpp.progression.domain.transformation.util.CommonHelper.APPLICATION_RECEIVED_DATE;
import static uk.gov.moj.cpp.progression.domain.transformation.util.CommonHelper.APPLICATION_STATUS;
import static uk.gov.moj.cpp.progression.domain.transformation.util.CommonHelper.BREACHED_ORDER;
import static uk.gov.moj.cpp.progression.domain.transformation.util.CommonHelper.BREACHED_ORDER_DATE;
import static uk.gov.moj.cpp.progression.domain.transformation.util.CommonHelper.COURT_APPLICATION_PAYMENT;
import static uk.gov.moj.cpp.progression.domain.transformation.util.CommonHelper.ID;
import static uk.gov.moj.cpp.progression.domain.transformation.util.CommonHelper.JUDICIAL_RESULTS;
import static uk.gov.moj.cpp.progression.domain.transformation.util.CommonHelper.LINKED_CASE_ID;
import static uk.gov.moj.cpp.progression.domain.transformation.util.CommonHelper.ORDERING_COURT;
import static uk.gov.moj.cpp.progression.domain.transformation.util.CommonHelper.OUT_OF_TIME_REASONS;
import static uk.gov.moj.cpp.progression.domain.transformation.util.CommonHelper.PARENT_APPLICATION_ID;
import static uk.gov.moj.cpp.progression.domain.transformation.util.CommonHelper.TYPE;

import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;

public class CourtApplicationHelper {


    private CourtApplicationHelper() {
    }
    @SuppressWarnings({"squid:S00108", "squid:MethodCyclomaticComplexity"})
    public static JsonObject transformCourtApplication(final JsonObject courtApplication) {
        //Add Mandatory Fields
        final JsonObjectBuilder transformedPayloadObjectBuilder = createObjectBuilder()
                .add(ID, courtApplication.getString(ID))
                .add(TYPE, courtApplication.getString(TYPE))
                .add(APPLICATION_RECEIVED_DATE, courtApplication.getString(APPLICATION_RECEIVED_DATE))
                .add(APPLICANT, transformApplicant(courtApplication.getJsonObject(APPLICANT)))
                .add(APPLICATION_STATUS, courtApplication.getString(APPLICATION_STATUS));

        //Add Optional Field
        if (courtApplication.containsKey(CommonHelper.APPLICATION_REFERENCE)) {
            transformedPayloadObjectBuilder.add(CommonHelper.APPLICATION_REFERENCE, courtApplication.getString(CommonHelper.APPLICATION_REFERENCE));
        }

        if (courtApplication.containsKey(CommonHelper.RESPONDENTS)) {
            transformedPayloadObjectBuilder.add(CommonHelper.RESPONDENTS, courtApplication.getJsonArray(CommonHelper.RESPONDENTS));
        }

        if (courtApplication.containsKey(CommonHelper.APPLICATION_OUTCOME)) {
            transformedPayloadObjectBuilder.add(CommonHelper.APPLICATION_OUTCOME, courtApplication.getJsonObject(CommonHelper.APPLICATION_OUTCOME));
        }

        if (!courtApplication.containsKey(LINKED_CASE_ID)) {
        } else {
            transformedPayloadObjectBuilder.add(LINKED_CASE_ID, courtApplication.getString(LINKED_CASE_ID));
        }

        if (courtApplication.containsKey(PARENT_APPLICATION_ID)) {
            transformedPayloadObjectBuilder.add(PARENT_APPLICATION_ID, courtApplication.getString(PARENT_APPLICATION_ID));
        }

        if (courtApplication.containsKey(APPLICATION_PARTICULARS)) {
            transformedPayloadObjectBuilder.add(APPLICATION_PARTICULARS, courtApplication.getString(APPLICATION_PARTICULARS));
        }
        if (courtApplication.containsKey(COURT_APPLICATION_PAYMENT)) {
            transformedPayloadObjectBuilder.add(COURT_APPLICATION_PAYMENT, courtApplication.getJsonObject(COURT_APPLICATION_PAYMENT));
        }
        if (courtApplication.containsKey(APPLICATION_DECISION_SOUGHT_BY_DATE)) {
            transformedPayloadObjectBuilder.add(APPLICATION_DECISION_SOUGHT_BY_DATE, courtApplication.getString(APPLICATION_DECISION_SOUGHT_BY_DATE));
        }
        if (courtApplication.containsKey(OUT_OF_TIME_REASONS)) {
            transformedPayloadObjectBuilder.add(OUT_OF_TIME_REASONS, courtApplication.getString(OUT_OF_TIME_REASONS));
        }
        if (courtApplication.containsKey(BREACHED_ORDER)) {
            transformedPayloadObjectBuilder.add(BREACHED_ORDER, courtApplication.getString(BREACHED_ORDER));
        }
        if (courtApplication.containsKey(BREACHED_ORDER_DATE)) {
            transformedPayloadObjectBuilder.add(BREACHED_ORDER_DATE, courtApplication.getString(BREACHED_ORDER_DATE));
        }

        if (courtApplication.containsKey(ORDERING_COURT)) {
            transformedPayloadObjectBuilder.add(ORDERING_COURT, courtApplication.getJsonObject(ORDERING_COURT));
        }

        if (courtApplication.containsKey(JUDICIAL_RESULTS)) {
            transformedPayloadObjectBuilder.add(JUDICIAL_RESULTS, courtApplication.getJsonArray(JUDICIAL_RESULTS));
        }

        return transformedPayloadObjectBuilder.build();
    }
}