package uk.gov.moj.cpp.progression.domain.transformation.util;

import static javax.json.Json.createArrayBuilder;
import static javax.json.Json.createObjectBuilder;
import static uk.gov.moj.cpp.progression.domain.transformation.util.ApplicantHelper.transformApplicant;
import static uk.gov.moj.cpp.progression.domain.transformation.util.CommonHelper.APPLICANT;
import static uk.gov.moj.cpp.progression.domain.transformation.util.CommonHelper.APPLICATION_DECISION_SOUGHT_BY_DATE;
import static uk.gov.moj.cpp.progression.domain.transformation.util.CommonHelper.APPLICATION_OUTCOME;
import static uk.gov.moj.cpp.progression.domain.transformation.util.CommonHelper.APPLICATION_PARTICULARS;
import static uk.gov.moj.cpp.progression.domain.transformation.util.CommonHelper.APPLICATION_RECEIVED_DATE;
import static uk.gov.moj.cpp.progression.domain.transformation.util.CommonHelper.APPLICATION_REFERENCE;
import static uk.gov.moj.cpp.progression.domain.transformation.util.CommonHelper.APPLICATION_RESPONSE;
import static uk.gov.moj.cpp.progression.domain.transformation.util.CommonHelper.APPLICATION_STATUS;
import static uk.gov.moj.cpp.progression.domain.transformation.util.CommonHelper.BREACHED_ORDER;
import static uk.gov.moj.cpp.progression.domain.transformation.util.CommonHelper.BREACHED_ORDER_DATE;
import static uk.gov.moj.cpp.progression.domain.transformation.util.CommonHelper.COURT_APPLICATION_PAYMENT;
import static uk.gov.moj.cpp.progression.domain.transformation.util.CommonHelper.DEFENDANT;
import static uk.gov.moj.cpp.progression.domain.transformation.util.CommonHelper.ID;
import static uk.gov.moj.cpp.progression.domain.transformation.util.CommonHelper.JUDICIAL_RESULTS;
import static uk.gov.moj.cpp.progression.domain.transformation.util.CommonHelper.LINKED_CASE_ID;
import static uk.gov.moj.cpp.progression.domain.transformation.util.CommonHelper.ORDERING_COURT;
import static uk.gov.moj.cpp.progression.domain.transformation.util.CommonHelper.ORGANISATION;
import static uk.gov.moj.cpp.progression.domain.transformation.util.CommonHelper.ORGANISATION_PERSONS;
import static uk.gov.moj.cpp.progression.domain.transformation.util.CommonHelper.OUT_OF_TIME_REASONS;
import static uk.gov.moj.cpp.progression.domain.transformation.util.CommonHelper.PARENT_APPLICATION_ID;
import static uk.gov.moj.cpp.progression.domain.transformation.util.CommonHelper.PARTY_DETAILS;
import static uk.gov.moj.cpp.progression.domain.transformation.util.CommonHelper.PERSON_DETAILS;
import static uk.gov.moj.cpp.progression.domain.transformation.util.CommonHelper.PROSECUTING_AUTHORITY;
import static uk.gov.moj.cpp.progression.domain.transformation.util.CommonHelper.REPRESENTATION_ORGANISATION;
import static uk.gov.moj.cpp.progression.domain.transformation.util.CommonHelper.RESPONDENTS;
import static uk.gov.moj.cpp.progression.domain.transformation.util.CommonHelper.SYNONYM;
import static uk.gov.moj.cpp.progression.domain.transformation.util.CommonHelper.TYPE;
import static uk.gov.moj.cpp.progression.domain.transformation.util.DefendantHelper.transformDefendant;

import javax.json.JsonArray;
import javax.json.JsonArrayBuilder;
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
                .add(TYPE, courtApplication.getJsonObject(TYPE))
                .add(APPLICATION_RECEIVED_DATE, courtApplication.getString(APPLICATION_RECEIVED_DATE))
                .add(APPLICANT, transformApplicant(courtApplication.getJsonObject(APPLICANT)))
                .add(APPLICATION_STATUS, courtApplication.getString(APPLICATION_STATUS));

        //Add Optional Field
        if (courtApplication.containsKey(APPLICATION_REFERENCE)) {
            transformedPayloadObjectBuilder.add(APPLICATION_REFERENCE, courtApplication.getString(APPLICATION_REFERENCE));
        }

        if (courtApplication.containsKey(RESPONDENTS)) {
            transformedPayloadObjectBuilder.add(RESPONDENTS, transformRespondents(courtApplication.getJsonArray(RESPONDENTS)));
        }

        if (courtApplication.containsKey(APPLICATION_OUTCOME)) {
            transformedPayloadObjectBuilder.add(APPLICATION_OUTCOME, courtApplication.getJsonObject(APPLICATION_OUTCOME));
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

    @SuppressWarnings({"squid:S3776", "squid:S1188"})
    public static JsonArray transformCourtApplications(final JsonArray courtApplications) {
        final JsonArrayBuilder transformedPayloadArrayBuilder = createArrayBuilder();
        courtApplications.forEach(o -> {
            final JsonObject courtApplication = (JsonObject) o;
            final JsonObjectBuilder transformedPayloadObjectBuilder = createObjectBuilder()
                    .add(ID, courtApplication.getString(ID))
                    .add(TYPE, courtApplication.getJsonObject(TYPE))
                    .add(APPLICATION_RECEIVED_DATE, courtApplication.getString(APPLICATION_RECEIVED_DATE))
                    .add(APPLICANT, transformApplicant(courtApplication.getJsonObject(APPLICANT)))
                    .add(APPLICATION_STATUS, courtApplication.getString(APPLICATION_STATUS));

            //Add Optional Field
            if (courtApplication.containsKey(APPLICATION_REFERENCE)) {
                transformedPayloadObjectBuilder.add(APPLICATION_REFERENCE, courtApplication.getString(APPLICATION_REFERENCE));
            }

            if (courtApplication.containsKey(RESPONDENTS)) {
                transformedPayloadObjectBuilder.add(RESPONDENTS, transformRespondents(courtApplication.getJsonArray(RESPONDENTS)));
            }

            if (courtApplication.containsKey(APPLICATION_OUTCOME)) {
                transformedPayloadObjectBuilder.add(APPLICATION_OUTCOME, courtApplication.getJsonObject(APPLICATION_OUTCOME));
            }

            if (courtApplication.containsKey(LINKED_CASE_ID)) {
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
            transformedPayloadArrayBuilder.add(transformedPayloadObjectBuilder);
        });
        return transformedPayloadArrayBuilder.build();
    }

    private static JsonArray transformRespondents(final JsonArray respondents) {
        final JsonArrayBuilder transformedPayloadObjectBuilder = createArrayBuilder();
        respondents.forEach(o -> {
            final JsonObject respondent = (JsonObject) o;
            final JsonObjectBuilder transformDefendantBuilder = createObjectBuilder()
                    .add(PARTY_DETAILS, transformPartyDetails(respondent.getJsonObject(PARTY_DETAILS)));


            if (respondent.containsKey(APPLICATION_RESPONSE)) {
                transformDefendantBuilder.add(APPLICATION_RESPONSE, respondent.getJsonObject(APPLICATION_RESPONSE));
            }

            transformedPayloadObjectBuilder.add(transformDefendantBuilder);
        });
        return transformedPayloadObjectBuilder.build();
    }

    private static JsonObject transformPartyDetails(final JsonObject partyDetails) {

        final JsonObjectBuilder transformedPayloadObjectBuilder = createObjectBuilder()
                .add(ID, partyDetails.getString(ID));


        if (partyDetails.containsKey(SYNONYM)) {
            transformedPayloadObjectBuilder.add(SYNONYM, partyDetails.getString(SYNONYM));
        }
        if (partyDetails.containsKey(PERSON_DETAILS)) {
            transformedPayloadObjectBuilder.add(PERSON_DETAILS, partyDetails.getJsonObject(PERSON_DETAILS));
        }
        if (partyDetails.containsKey(ORGANISATION)) {
            transformedPayloadObjectBuilder.add(ORGANISATION, partyDetails.getJsonObject(ORGANISATION));
        }
        if (partyDetails.containsKey(ORGANISATION_PERSONS)) {
            transformedPayloadObjectBuilder.add(ORGANISATION_PERSONS, partyDetails.getJsonArray(ORGANISATION_PERSONS));
        }
        if (partyDetails.containsKey(PROSECUTING_AUTHORITY)) {
            transformedPayloadObjectBuilder.add(PROSECUTING_AUTHORITY, partyDetails.getJsonObject(PROSECUTING_AUTHORITY));
        }
        if (partyDetails.containsKey(DEFENDANT)) {
            transformedPayloadObjectBuilder.add(DEFENDANT, transformDefendant(partyDetails.getJsonObject(DEFENDANT)));
        }
        if (partyDetails.containsKey(REPRESENTATION_ORGANISATION)) {
            transformedPayloadObjectBuilder.add(REPRESENTATION_ORGANISATION, partyDetails.getJsonObject(REPRESENTATION_ORGANISATION));
        }
        return transformedPayloadObjectBuilder.build();
    }
}