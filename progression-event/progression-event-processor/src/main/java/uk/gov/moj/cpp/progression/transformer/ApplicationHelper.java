package uk.gov.moj.cpp.progression.transformer;

import static uk.gov.justice.services.messaging.JsonObjects.createArrayBuilder;
import static uk.gov.justice.services.messaging.JsonObjects.createObjectBuilder;
import static uk.gov.moj.cpp.progression.transformer.HearingHelper.filterJudicialResults;
import static uk.gov.moj.cpp.progression.transformer.SchemaVariableConstants.ALLEGATION_OR_COMPLAINT_END_DATE;
import static uk.gov.moj.cpp.progression.transformer.SchemaVariableConstants.ALLEGATION_OR_COMPLAINT_START_DATE;
import static uk.gov.moj.cpp.progression.transformer.SchemaVariableConstants.APPLICANT;
import static uk.gov.moj.cpp.progression.transformer.SchemaVariableConstants.APPLICATION_DECISION_SOUGHT_BY_DATE;
import static uk.gov.moj.cpp.progression.transformer.SchemaVariableConstants.APPLICATION_PARTICULARS;
import static uk.gov.moj.cpp.progression.transformer.SchemaVariableConstants.APPLICATION_RECEIVED_DATE;
import static uk.gov.moj.cpp.progression.transformer.SchemaVariableConstants.APPLICATION_REFERENCE;
import static uk.gov.moj.cpp.progression.transformer.SchemaVariableConstants.APPLICATION_STATUS;
import static uk.gov.moj.cpp.progression.transformer.SchemaVariableConstants.APPOINTMENT_NOTIFICATION_REQUIRED;
import static uk.gov.moj.cpp.progression.transformer.SchemaVariableConstants.ASSOCIATED_PERSONS;
import static uk.gov.moj.cpp.progression.transformer.SchemaVariableConstants.CAN_BE_SUBJECT_OF_BREACH_PROCEEDINGS;
import static uk.gov.moj.cpp.progression.transformer.SchemaVariableConstants.CAN_BE_SUBJECT_OF_VARIATION_PROCEEDINGS;
import static uk.gov.moj.cpp.progression.transformer.SchemaVariableConstants.CASE_STATUS;
import static uk.gov.moj.cpp.progression.transformer.SchemaVariableConstants.CONVICTION_DATE;
import static uk.gov.moj.cpp.progression.transformer.SchemaVariableConstants.COURT_APPLICATION_CASES;
import static uk.gov.moj.cpp.progression.transformer.SchemaVariableConstants.COURT_APPLICATION_PAYMENT;
import static uk.gov.moj.cpp.progression.transformer.SchemaVariableConstants.COURT_ORDER;
import static uk.gov.moj.cpp.progression.transformer.SchemaVariableConstants.COURT_ORDER_OFFENCES;
import static uk.gov.moj.cpp.progression.transformer.SchemaVariableConstants.DEFENDANT_ASN;
import static uk.gov.moj.cpp.progression.transformer.SchemaVariableConstants.DEFENDANT_IDS;
import static uk.gov.moj.cpp.progression.transformer.SchemaVariableConstants.END_DATE;
import static uk.gov.moj.cpp.progression.transformer.SchemaVariableConstants.ID;
import static uk.gov.moj.cpp.progression.transformer.SchemaVariableConstants.IS_SJP;
import static uk.gov.moj.cpp.progression.transformer.SchemaVariableConstants.IS_SJP_ORDER;
import static uk.gov.moj.cpp.progression.transformer.SchemaVariableConstants.IS_YOUTH;
import static uk.gov.moj.cpp.progression.transformer.SchemaVariableConstants.JUDICIAL_RESULTS;
import static uk.gov.moj.cpp.progression.transformer.SchemaVariableConstants.JUDICIAL_RESULT_TYPE_ID;
import static uk.gov.moj.cpp.progression.transformer.SchemaVariableConstants.LABEL;
import static uk.gov.moj.cpp.progression.transformer.SchemaVariableConstants.LEGAL_ENTITY_DEFENDANT;
import static uk.gov.moj.cpp.progression.transformer.SchemaVariableConstants.MASTER_DEFENDANT;
import static uk.gov.moj.cpp.progression.transformer.SchemaVariableConstants.MASTER_DEFENDANT_ID;
import static uk.gov.moj.cpp.progression.transformer.SchemaVariableConstants.NOTIFICATION_REQUIRED;
import static uk.gov.moj.cpp.progression.transformer.SchemaVariableConstants.OFFENCES;
import static uk.gov.moj.cpp.progression.transformer.SchemaVariableConstants.ORDERING_COURT;
import static uk.gov.moj.cpp.progression.transformer.SchemaVariableConstants.ORDERING_HEARING_ID;
import static uk.gov.moj.cpp.progression.transformer.SchemaVariableConstants.ORDER_DATE;
import static uk.gov.moj.cpp.progression.transformer.SchemaVariableConstants.ORGANISATION;
import static uk.gov.moj.cpp.progression.transformer.SchemaVariableConstants.ORGANISATION_PERSONS;
import static uk.gov.moj.cpp.progression.transformer.SchemaVariableConstants.OUT_OF_TIME_REASONS;
import static uk.gov.moj.cpp.progression.transformer.SchemaVariableConstants.PARENT_APPLICATION_ID;
import static uk.gov.moj.cpp.progression.transformer.SchemaVariableConstants.PERSON_DEFENDANT;
import static uk.gov.moj.cpp.progression.transformer.SchemaVariableConstants.PERSON_DETAILS;
import static uk.gov.moj.cpp.progression.transformer.SchemaVariableConstants.PLEA;
import static uk.gov.moj.cpp.progression.transformer.SchemaVariableConstants.PNC_ID;
import static uk.gov.moj.cpp.progression.transformer.SchemaVariableConstants.PROSECUTING_AUTHORITY;
import static uk.gov.moj.cpp.progression.transformer.SchemaVariableConstants.PROSECUTION_AUTHORITY_REFERENCE;
import static uk.gov.moj.cpp.progression.transformer.SchemaVariableConstants.PROSECUTION_CASE_ID;
import static uk.gov.moj.cpp.progression.transformer.SchemaVariableConstants.PROSECUTION_CASE_IDENTIFIER;
import static uk.gov.moj.cpp.progression.transformer.SchemaVariableConstants.REMOVAL_REASON;
import static uk.gov.moj.cpp.progression.transformer.SchemaVariableConstants.REPRESENTATION_ORGANISATION;
import static uk.gov.moj.cpp.progression.transformer.SchemaVariableConstants.RESPONDENTS;
import static uk.gov.moj.cpp.progression.transformer.SchemaVariableConstants.START_DATE;
import static uk.gov.moj.cpp.progression.transformer.SchemaVariableConstants.SUBJECT;
import static uk.gov.moj.cpp.progression.transformer.SchemaVariableConstants.SUMMONS_REQUIRED;
import static uk.gov.moj.cpp.progression.transformer.SchemaVariableConstants.SYNONYM;
import static uk.gov.moj.cpp.progression.transformer.SchemaVariableConstants.THIRD_PARTIES;
import static uk.gov.moj.cpp.progression.transformer.SchemaVariableConstants.TYPE;
import static uk.gov.moj.cpp.progression.transformer.SchemaVariableConstants.VERDICT;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.json.JsonArray;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import java.util.stream.IntStream;

@SuppressWarnings({"squid:MethodCyclomaticComplexity"})
public class ApplicationHelper {

    private static final Logger LOGGER = LoggerFactory.getLogger(ApplicationHelper.class);

    private ApplicationHelper() {
    }

    public static JsonArray transformApplications(final JsonArray applications) {

        final JsonArrayBuilder transformedPayloadObjectBuilder = createArrayBuilder();

        IntStream.range(
                0,
                applications.size()
        ).mapToObj(
                index -> transformApplication(applications.getJsonObject(index))
        ).forEach(transformedPayloadObjectBuilder::add);

        return transformedPayloadObjectBuilder.build();
    }

    @SuppressWarnings({"squid:S3776"})
    private static JsonObjectBuilder transformApplication(final JsonObject application) {
        LOGGER.info("Transforming Application {}", application.getString("id"));

        final JsonObjectBuilder transformApplicationBuilder = createObjectBuilder()
                .add(ID, application.getString(ID))
                .add(TYPE, application.getJsonObject(TYPE))
                .add(APPLICATION_RECEIVED_DATE, application.getString(APPLICATION_RECEIVED_DATE))
                .add(APPLICANT, application.getJsonObject(APPLICANT))
                .add(APPLICATION_STATUS, application.getString(APPLICATION_STATUS))
                .add(SUBJECT,application.getJsonObject(SUBJECT));

        if (application.containsKey(APPLICATION_REFERENCE)) {
            transformApplicationBuilder.add(APPLICATION_REFERENCE, application.getString(APPLICATION_REFERENCE));
        }

        if (application.containsKey(RESPONDENTS)) {
            transformApplicationBuilder.add(RESPONDENTS, application.getJsonArray(RESPONDENTS));
        }

        if (application.containsKey(PARENT_APPLICATION_ID)) {
            transformApplicationBuilder.add(PARENT_APPLICATION_ID, application.getString(PARENT_APPLICATION_ID));
        }

        if (application.containsKey(APPLICATION_PARTICULARS)) {
            transformApplicationBuilder.add(APPLICATION_PARTICULARS, application.getString(APPLICATION_PARTICULARS));
        }

        if (application.containsKey(COURT_APPLICATION_PAYMENT)) {
            transformApplicationBuilder.add(COURT_APPLICATION_PAYMENT, application.getJsonObject(COURT_APPLICATION_PAYMENT));
        }

        if (application.containsKey(APPLICATION_DECISION_SOUGHT_BY_DATE)) {
            transformApplicationBuilder.add(APPLICATION_DECISION_SOUGHT_BY_DATE, application.getString(APPLICATION_DECISION_SOUGHT_BY_DATE));
        }

        if (application.containsKey(OUT_OF_TIME_REASONS)) {
            transformApplicationBuilder.add(OUT_OF_TIME_REASONS, application.getString(OUT_OF_TIME_REASONS));
        }

        if (application.containsKey(REMOVAL_REASON)) {
            transformApplicationBuilder.add(REMOVAL_REASON, application.getString(REMOVAL_REASON));
        }

        if (application.containsKey(JUDICIAL_RESULTS)) {
            transformApplicationBuilder.add(JUDICIAL_RESULTS, filterJudicialResults(application.getJsonArray(JUDICIAL_RESULTS)));
        }

        if (application.containsKey(DEFENDANT_ASN)) {
            transformApplicationBuilder.add(DEFENDANT_ASN, application.getString(DEFENDANT_ASN));
        }

        if(application.containsKey(CONVICTION_DATE)){
            transformApplicationBuilder.add(CONVICTION_DATE, application.getString(CONVICTION_DATE));

        }

        if (application.containsKey(ALLEGATION_OR_COMPLAINT_END_DATE)) {
            transformApplicationBuilder.add(ALLEGATION_OR_COMPLAINT_END_DATE, application.getString(ALLEGATION_OR_COMPLAINT_END_DATE));
        }

        if (application.containsKey(ALLEGATION_OR_COMPLAINT_START_DATE)) {
            transformApplicationBuilder.add(ALLEGATION_OR_COMPLAINT_START_DATE, application.getString(ALLEGATION_OR_COMPLAINT_START_DATE));
        }

        if (application.containsKey(COURT_APPLICATION_CASES)) {
            transformApplicationBuilder.add(COURT_APPLICATION_CASES, transformCourtApplicationCases(application.getJsonArray(COURT_APPLICATION_CASES)));
        }

        if (application.containsKey(COURT_ORDER)) {
            transformApplicationBuilder.add(COURT_ORDER, transformCourtOrder(application.getJsonObject(COURT_ORDER)));
        }

        if (application.containsKey(PLEA)) {
            transformApplicationBuilder.add(PLEA, application.getJsonObject(PLEA));
        }

        if (application.containsKey(VERDICT)) {
            transformApplicationBuilder.add(VERDICT, application.getJsonObject(VERDICT));
        }

        if (application.containsKey(THIRD_PARTIES)) {
            transformApplicationBuilder.add(THIRD_PARTIES, transformCourtApplicationParties(application.getJsonArray(THIRD_PARTIES)));
        }

        return transformApplicationBuilder;
    }

    private static JsonArray transformCourtApplicationParties(final JsonArray courtApplicationParties) {
        final JsonArrayBuilder transformedPayloadObjectBuilder = createArrayBuilder();
        IntStream.range(0, courtApplicationParties.size()).mapToObj(index -> transformCourtApplicationParty(courtApplicationParties.getJsonObject(index))).forEach(transformedPayloadObjectBuilder::add);
        return transformedPayloadObjectBuilder.build();
    }

    private static JsonObjectBuilder transformCourtApplicationParty(final JsonObject courtApplicationParty) {
        final JsonObjectBuilder transformCourtApplicationPartyBuilder = createObjectBuilder()
                .add(ID, courtApplicationParty.getJsonString(ID))
                .add(SUMMONS_REQUIRED, courtApplicationParty.getBoolean(SUMMONS_REQUIRED))
                .add(NOTIFICATION_REQUIRED, courtApplicationParty.getBoolean(NOTIFICATION_REQUIRED));

        if (courtApplicationParty.containsKey(SYNONYM)) {
            transformCourtApplicationPartyBuilder.add(SYNONYM, courtApplicationParty.getString(SYNONYM));
        }

        if (courtApplicationParty.containsKey(APPOINTMENT_NOTIFICATION_REQUIRED)) {
            transformCourtApplicationPartyBuilder.add(APPOINTMENT_NOTIFICATION_REQUIRED, courtApplicationParty.getBoolean(APPOINTMENT_NOTIFICATION_REQUIRED));
        }

        if (courtApplicationParty.containsKey(PERSON_DETAILS)) {
            transformCourtApplicationPartyBuilder.add(PERSON_DETAILS, courtApplicationParty.getJsonObject(PERSON_DETAILS));
        }

        if (courtApplicationParty.containsKey(ORGANISATION)) {
            transformCourtApplicationPartyBuilder.add(ORGANISATION, courtApplicationParty.getJsonObject(ORGANISATION));
        }

        if (courtApplicationParty.containsKey(ORGANISATION_PERSONS)) {
            transformCourtApplicationPartyBuilder.add(ORGANISATION_PERSONS, courtApplicationParty.getJsonArray(ORGANISATION_PERSONS));
        }

        if (courtApplicationParty.containsKey(PROSECUTING_AUTHORITY)) {
            transformCourtApplicationPartyBuilder.add(PROSECUTING_AUTHORITY, courtApplicationParty.getJsonObject(PROSECUTING_AUTHORITY));
        }

        if (courtApplicationParty.containsKey(MASTER_DEFENDANT)) {
            transformCourtApplicationPartyBuilder.add(MASTER_DEFENDANT, transformMasterDefendant(courtApplicationParty.getJsonObject(MASTER_DEFENDANT)));
        }

        if (courtApplicationParty.containsKey(REPRESENTATION_ORGANISATION)) {
            transformCourtApplicationPartyBuilder.add(REPRESENTATION_ORGANISATION, courtApplicationParty.getJsonObject(REPRESENTATION_ORGANISATION));
        }

        return transformCourtApplicationPartyBuilder;
    }


    private static JsonObject transformMasterDefendant(final JsonObject masterDefendant) {

        final JsonObjectBuilder transformDefendantBuilder = createObjectBuilder()
                .add(MASTER_DEFENDANT_ID, masterDefendant.getJsonString(MASTER_DEFENDANT_ID));

        if(masterDefendant.containsKey(PERSON_DEFENDANT)) {
             transformDefendantBuilder.add(PERSON_DEFENDANT, masterDefendant.getJsonObject(PERSON_DEFENDANT));
        }

        if (masterDefendant.containsKey(LEGAL_ENTITY_DEFENDANT)) {
            transformDefendantBuilder.add(LEGAL_ENTITY_DEFENDANT, masterDefendant.getJsonObject(LEGAL_ENTITY_DEFENDANT));
        }

        if (masterDefendant.containsKey(PROSECUTION_AUTHORITY_REFERENCE)) {
            transformDefendantBuilder.add(PROSECUTION_AUTHORITY_REFERENCE, masterDefendant.getString(PROSECUTION_AUTHORITY_REFERENCE));
        }

        if (masterDefendant.containsKey(ASSOCIATED_PERSONS)) {
            transformDefendantBuilder.add(ASSOCIATED_PERSONS, masterDefendant.getJsonArray(ASSOCIATED_PERSONS));
        }


        if (masterDefendant.containsKey(PNC_ID)) {
            transformDefendantBuilder.add(PNC_ID, masterDefendant.getString(PNC_ID));
        }


        if (masterDefendant.containsKey(IS_YOUTH)) {
            transformDefendantBuilder.add(IS_YOUTH, masterDefendant.getBoolean(IS_YOUTH));
        }

        return transformDefendantBuilder.build();
    }

    private static JsonObject transformCourtOrder(final JsonObject courtOrder) {
        final JsonObjectBuilder transformCourtOrderBuilder = createObjectBuilder()
                .add(ID, courtOrder.getString(ID))
                .add(JUDICIAL_RESULT_TYPE_ID, courtOrder.getString(JUDICIAL_RESULT_TYPE_ID))
                .add(LABEL, courtOrder.getString(LABEL))
                .add(ORDER_DATE, courtOrder.getString(ORDER_DATE))
                .add(START_DATE, courtOrder.getString(START_DATE))
                .add(ORDERING_COURT, courtOrder.getJsonObject(ORDERING_COURT))
                .add(ORDERING_HEARING_ID, courtOrder.getString(ORDERING_HEARING_ID))
                .add(IS_SJP_ORDER, courtOrder.getBoolean(IS_SJP_ORDER))
                .add(CAN_BE_SUBJECT_OF_BREACH_PROCEEDINGS, courtOrder.getBoolean(CAN_BE_SUBJECT_OF_BREACH_PROCEEDINGS))
                .add(CAN_BE_SUBJECT_OF_VARIATION_PROCEEDINGS, courtOrder.getBoolean(CAN_BE_SUBJECT_OF_VARIATION_PROCEEDINGS))
                .add(COURT_ORDER_OFFENCES, courtOrder.getJsonArray(COURT_ORDER_OFFENCES));

        if (courtOrder.containsKey(DEFENDANT_IDS)) {
            transformCourtOrderBuilder.add(DEFENDANT_IDS, courtOrder.getJsonArray(DEFENDANT_IDS));
        }

        if (courtOrder.containsKey(END_DATE)) {
            transformCourtOrderBuilder.add(END_DATE, courtOrder.getString(END_DATE));
        }

        if (courtOrder.containsKey(MASTER_DEFENDANT_ID)) {
            transformCourtOrderBuilder.add(MASTER_DEFENDANT_ID, courtOrder.getString(MASTER_DEFENDANT_ID));
        }

        return transformCourtOrderBuilder.build();
    }

    private static JsonArray transformCourtApplicationCases(final JsonArray courtApplicationCases) {
        final JsonArrayBuilder transformedPayloadObjectBuilder = createArrayBuilder();
        IntStream.range(0, courtApplicationCases.size()).mapToObj(index -> transCourtApplicationCases(courtApplicationCases.getJsonObject(index))).forEach(transformedPayloadObjectBuilder::add);
        return transformedPayloadObjectBuilder.build();
    }

    private static JsonObjectBuilder transCourtApplicationCases(final JsonObject courtApplicationCase) {
        final JsonObjectBuilder transformCourtApplicationCaseBuilder = createObjectBuilder()
                .add(PROSECUTION_CASE_ID, courtApplicationCase.getJsonString(PROSECUTION_CASE_ID))
                .add(IS_SJP, courtApplicationCase.getBoolean(IS_SJP))
                .add(CASE_STATUS, courtApplicationCase.getString(CASE_STATUS))
                .add(PROSECUTION_CASE_IDENTIFIER, courtApplicationCase.getJsonObject(PROSECUTION_CASE_IDENTIFIER));
        if(courtApplicationCase.containsKey(OFFENCES)) {
            transformCourtApplicationCaseBuilder.add(OFFENCES, courtApplicationCase.getJsonArray(OFFENCES));
        }
        return transformCourtApplicationCaseBuilder;
    }


}
