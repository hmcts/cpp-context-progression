package uk.gov.moj.cpp.progression.processor;


import static javax.json.Json.createArrayBuilder;
import static javax.json.Json.createObjectBuilder;
import static uk.gov.justice.services.core.annotation.Component.EVENT_PROCESSOR;
import static uk.gov.moj.cpp.progression.transformer.HearingHelper.transformedHearing;
import static uk.gov.moj.cpp.progression.transformer.SchemaVariableConstants.COURT_APPLICATIONS;
import static uk.gov.moj.cpp.progression.transformer.SchemaVariableConstants.COURT_APPLICATION_CASES;
import static uk.gov.moj.cpp.progression.transformer.SchemaVariableConstants.COURT_ORDER;
import static uk.gov.moj.cpp.progression.transformer.SchemaVariableConstants.COURT_ORDER_OFFENCES;
import static uk.gov.moj.cpp.progression.transformer.SchemaVariableConstants.DEFENDANTS;
import static uk.gov.moj.cpp.progression.transformer.SchemaVariableConstants.ID;
import static uk.gov.moj.cpp.progression.transformer.SchemaVariableConstants.LAA_APPLN_REFERENCE;
import static uk.gov.moj.cpp.progression.transformer.SchemaVariableConstants.OFFENCE;
import static uk.gov.moj.cpp.progression.transformer.SchemaVariableConstants.OFFENCES;
import static uk.gov.moj.cpp.progression.transformer.SchemaVariableConstants.OU_CODE;
import static uk.gov.moj.cpp.progression.transformer.SchemaVariableConstants.PROSECUTION_CASES;
import static uk.gov.moj.cpp.progression.transformer.SchemaVariableConstants.PROSECUTION_CASE_ID;
import static uk.gov.moj.cpp.progression.transformer.SchemaVariableConstants.PROSECUTION_CASE_IDENTIFIER;
import static uk.gov.moj.cpp.progression.transformer.SchemaVariableConstants.PROSECUTOR_CODE;

import uk.gov.justice.services.common.configuration.Value;
import uk.gov.justice.services.core.annotation.Component;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.requester.Requester;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.progression.service.RefDataService;
import uk.gov.moj.cpp.progression.service.RestEasyClientService;

import java.util.Map;

import javax.inject.Inject;
import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.json.JsonValue;
import javax.ws.rs.core.Response;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ServiceComponent(Component.EVENT_PROCESSOR)
public class VejCaseworkerProcessor {

    private static final Logger LOGGER = LoggerFactory.getLogger(VejCaseworkerProcessor.class.getName());

    @Inject
    @ServiceComponent(EVENT_PROCESSOR)
    private Requester requester;

    @Inject
    RefDataService referenceDataService;

    @Inject
    @Value(key = "vejHearingDetailsUrl", defaultValue = "http://localhost:8080/vep/api/v1/hearing/details")
    private String vejHearingDetailsUrl;

    @Inject
    @Value(key = "vejHearingDeleteUrl", defaultValue = "http://localhost:8080/vep/api/v1/hearing/deleted")
    private String vejHearingDeleteUrl;

    @Inject
    @Value(key = "vejCaseWorker.subscription.key", defaultValue = "3674a16507104b749a76b29b6c837352")
    private String subscriptionKey;

    @Inject
    @Value(key = "vejEnabled", defaultValue = "true")
    private String vejEnabled;

    @Inject
    private RestEasyClientService restEasyClientService;

    private static final String HEARING = "hearing";


    @Handles("progression.events.vej-hearing-populated-to-probation-caseworker")
    public void processVejHearingPopulatedToProbationCaseworker(final JsonEnvelope jsonEnvelope) {
        if (!Boolean.parseBoolean(vejEnabled)) {
            return;
        }
        final JsonObject payload = jsonEnvelope.payloadAsJsonObject();
        LOGGER.info("progression.events.vej-hearing-populated-to-probation-caseworker event received with metadata {} and payload {}",
                jsonEnvelope.metadata(), payload);
        final JsonObject transformedHearingPayload = transformedHearing(payload);
        final JsonObject externalPayload = createObjectBuilder()
                .add(HEARING, extractPoliceCases(transformedHearingPayload).get(HEARING))
                .build();
        final JsonObject hearing = (JsonObject) externalPayload.get(HEARING);
        final JsonArray policeCaseProsecutors = (JsonArray) hearing.get(PROSECUTION_CASES);
        if (null != policeCaseProsecutors && !policeCaseProsecutors.isEmpty()) {
            final Response response = restEasyClientService.post(vejHearingDetailsUrl, externalPayload.toString(), subscriptionKey);
            LOGGER.info("Azure Function VEP hearing populated API {} invoked with Request: {} Received response: {}",
                    vejHearingDetailsUrl, externalPayload, response.getStatus());
        } else {
            LOGGER.info("Azure Function VEP hearing populated API is not invoked as the prosecutionPoliceCases is Empty {}",
                    policeCaseProsecutors);
        }
    }

    @Handles("progression.events.vej-deleted-hearing-populated-to-probation-caseworker")
    public void processVejDeletedHearingPopulatedToProbationCaseworker(final JsonEnvelope jsonEnvelope) {
        if (!Boolean.parseBoolean(vejEnabled)) {
            return;
        }
        final JsonObject payload = jsonEnvelope.payloadAsJsonObject();
        LOGGER.info("progression.events.vej-deleted-hearing-populated-to-probation-caseworker event received with metadata {} and payload {}",
                jsonEnvelope.metadata(), payload);
        LOGGER.info("CHECK: progression.events.vej-deleted-hearing-populated-to-probation-caseworker:vejHearingDeleteUrl:{}, subscriptionKey:{}",
                vejHearingDeleteUrl, subscriptionKey);
        final JsonObject transformedHearingPayload = transformedHearing(payload);
        final JsonObject externalPayload = createObjectBuilder()
                .add(HEARING, extractPoliceCases(transformedHearingPayload).get(HEARING))
                .build();
        final JsonObject hearing = (JsonObject) externalPayload.get(HEARING);
        final JsonArray policeCaseProsecutors = (JsonArray) hearing.get(PROSECUTION_CASES);
        if (null != policeCaseProsecutors && !policeCaseProsecutors.isEmpty()) {
            final Response response = restEasyClientService.post(vejHearingDeleteUrl, externalPayload.toString(), subscriptionKey);
            LOGGER.info("Azure Function VEP hearing deleted API {} invoked with Request: {} Received response: {}",
                    vejHearingDeleteUrl, externalPayload, response.getStatus());
        } else {
            LOGGER.info("Azure Function VEP hearing deleted API is not invoked as the prosecutionPoliceCases is Empty {}",
                    policeCaseProsecutors);
        }
    }

    private JsonObject extractPoliceCases(final JsonObject hearingParent) {
        final JsonObject hearingObj = ((JsonObject) hearingParent.get(HEARING));
        LOGGER.info("Starting extractPoliceCases prosecution cases from hearingParent {}", hearingParent);
        final JsonArray prosecutionCases = (JsonArray) hearingObj.get(PROSECUTION_CASES);
        final JsonArrayBuilder policeCaseProsecutionCasesBuilder = Json.createArrayBuilder();
        final JsonArrayBuilder policeCasesBuilder = Json.createArrayBuilder();
        final JsonArray courtApplications = (JsonArray) hearingObj.get(COURT_APPLICATIONS);
        final JsonArrayBuilder courtApplicationsBuilder = Json.createArrayBuilder();
        if (null != prosecutionCases && !prosecutionCases.isEmpty()) {
            extractPoliceProsecutionCases(prosecutionCases, policeCaseProsecutionCasesBuilder);
        }
        JsonObject hearingObj1 = Json.createObjectBuilder().build();
        final JsonArray policeCaseProsecutionCases1 = policeCaseProsecutionCasesBuilder.build();
        if (null != policeCaseProsecutionCases1 && !policeCaseProsecutionCases1.isEmpty()) {
            hearingObj1 = removeProperty(hearingObj, PROSECUTION_CASES);
            hearingObj1 = addProperty(hearingObj1, PROSECUTION_CASES, policeCaseProsecutionCases1);
        }
        if (null != policeCaseProsecutionCases1 && !policeCaseProsecutionCases1.isEmpty()) {
            extractedPoliceCaseLogics(policeCasesBuilder,
                    courtApplications,
                    courtApplicationsBuilder,
                    hearingObj1,
                    policeCaseProsecutionCases1);
            hearingObj1 = removeAndAddPoliceProsecutionCases(hearingObj1, hearingObj, policeCasesBuilder);
        }

        final JsonArray policeCourtCases = courtApplicationsBuilder.build();
        if (null != policeCourtCases && !policeCourtCases.isEmpty()) {
            hearingObj1 = removeProperty(hearingObj1, COURT_APPLICATIONS);
            hearingObj1 = addProperty(hearingObj1, COURT_APPLICATIONS, policeCourtCases);
        }

        JsonObject hearingParent1 = removeProperty(hearingParent, HEARING);
        hearingParent1 = addProperty(hearingParent1, HEARING, hearingObj1);
        LOGGER.info("Ending extractPoliceCases prosecution cases from hearingParent1 {}", hearingParent1);
        return hearingParent1;
    }

    private JsonObject removeAndAddPoliceProsecutionCases(JsonObject hearingObj1, final JsonObject hearingObj, final JsonArrayBuilder policeCases) {
        final JsonArray policeCases1 = policeCases.build();
        if (null != policeCases1 && !policeCases1.isEmpty()) {
            hearingObj1 = removeProperty(hearingObj, PROSECUTION_CASES);
            hearingObj1 = addProperty(hearingObj1, PROSECUTION_CASES, policeCases1);
        }
        return hearingObj1;
    }

    private void extractedPoliceCaseLogics(final JsonArrayBuilder policeCases,
                                           final JsonArray courtApplications,
                                           final JsonArrayBuilder policeCourtApplications,
                                           JsonObject hearingObj1,
                                           final JsonArray policeCaseProsecutors1) {
        for (int i = 0; i < policeCaseProsecutors1.size(); i++) {
            final JsonObject item = policeCaseProsecutors1.getJsonObject(i);
            final JsonArray defendants = (JsonArray) item.get(DEFENDANTS);
            final JsonArrayBuilder laaRmDefendants = createArrayBuilder();
            iterateDefendants(policeCases, item, defendants, laaRmDefendants);
            if (null != courtApplications && !courtApplications.isEmpty()) {
                hearingObj1 = iterateCourtApplications(courtApplications, policeCourtApplications, hearingObj1, item);
            }
        }
    }

    private JsonObject iterateCourtApplications(final JsonArray courtApplications, final JsonArrayBuilder policeCourtApplications, JsonObject hearingObj1, final JsonObject item) {
        for (int ic = 0; ic < courtApplications.size(); ic++) {
            final JsonObject itemCourt = courtApplications.getJsonObject(ic);
            JsonObject newItem = Json.createObjectBuilder().build();
            final JsonArray courtApplicationCases = (JsonArray) itemCourt.get(COURT_APPLICATION_CASES);
            final JsonArrayBuilder policeCourtApplicationCases = Json.createArrayBuilder();
            if (null != courtApplicationCases && !courtApplicationCases.isEmpty()) {
                iterateCourtApplicationCasesForOffences(item, courtApplicationCases, policeCourtApplicationCases);
            }
            final JsonObject courtOrder = itemCourt.getJsonObject(COURT_ORDER);
            JsonObject newCourtOrder = Json.createObjectBuilder().build();
            if (null != courtOrder) {
                final JsonArray courtOrderOffences = (JsonArray) courtOrder.get(COURT_ORDER_OFFENCES);
                final JsonArrayBuilder laaCourtOrderOffences = Json.createArrayBuilder();
                if (null != courtOrderOffences && !courtOrderOffences.isEmpty()) {
                    iterateCourtOrderOffencesForOffences(item, courtOrderOffences, laaCourtOrderOffences);
                    newCourtOrder = removeProperty(courtOrder, COURT_ORDER_OFFENCES);
                    newCourtOrder = addProperty(newCourtOrder, COURT_ORDER_OFFENCES, laaCourtOrderOffences.build());
                }
                newItem = removeProperty(itemCourt, COURT_ORDER);
                newItem = addProperty(newItem, COURT_ORDER, newCourtOrder);
                hearingObj1 = removeProperty(hearingObj1, COURT_APPLICATIONS);
                hearingObj1 = addProperty(hearingObj1, COURT_APPLICATIONS, newItem);
            }
            final JsonArray policeCourtApplicationCases1 = policeCourtApplicationCases.build();
            if (null != policeCourtApplicationCases1 && !policeCourtApplicationCases1.isEmpty()) {
                JsonObject item1 = removeProperty(newItem, COURT_APPLICATION_CASES);
                item1 = addProperty(item1, COURT_APPLICATION_CASES, policeCourtApplicationCases1);
                policeCourtApplications.add(item1);
            }
        }
        return hearingObj1;
    }

    private void iterateDefendants(final JsonArrayBuilder policeCases, final JsonObject item, final JsonArray defendants, final JsonArrayBuilder laaRmDefendents) {
        if (null != defendants && !defendants.isEmpty()) {
            iterateDefendantsForOffences(defendants, laaRmDefendents);
            JsonObject newItem = removeProperty(item, DEFENDANTS);
            newItem = addProperty(newItem, DEFENDANTS, laaRmDefendents.build());
            policeCases.add(newItem);
        }
    }

    private void iterateCourtOrderOffencesForOffences(final JsonObject item, final JsonArray courtOrderOffences, final JsonArrayBuilder laaCourtOrderOffences) {
        for (int j = 0; j < courtOrderOffences.size(); j++) {
            final JsonObject courtOrderOffence = courtOrderOffences.getJsonObject(j);
            final JsonObject offence = courtOrderOffence.getJsonObject(OFFENCE);
            if (offence.containsKey(LAA_APPLN_REFERENCE)) {
                final JsonObject newOffence = removeProperty(offence, LAA_APPLN_REFERENCE);
                JsonObject newCourtOrderOffence = removeProperty(courtOrderOffence, OFFENCE);
                newCourtOrderOffence = addProperty(newCourtOrderOffence, OFFENCE, newOffence);
                if (item.get(ID).equals(newCourtOrderOffence.get(PROSECUTION_CASE_ID))) {
                    laaCourtOrderOffences.add(newCourtOrderOffence);
                }
            } else {
                if (item.get(ID).equals(courtOrderOffence.get(PROSECUTION_CASE_ID))) {
                    laaCourtOrderOffences.add(courtOrderOffence);
                }
            }
        }
    }

    private void iterateCourtApplicationCasesForOffences(final JsonObject item, final JsonArray courtApplicationCases, final JsonArrayBuilder policeCourtApplicationCases) {
        for (int j = 0; j < courtApplicationCases.size(); j++) {
            final JsonObject courtApplicationCase = courtApplicationCases.getJsonObject(j);

            final JsonArray offences = (JsonArray) courtApplicationCase.get(OFFENCES);
            final JsonArrayBuilder offenceLaa = createArrayBuilder();
            if (null != offences && !offences.isEmpty()) {
                for (int k = 0; k < offences.size(); k++) {
                    final JsonObject offence = offences.getJsonObject(k);
                    deleteLaaReferenceFromOffence(offence, offenceLaa);
                }
                JsonObject laaCourtApplicationCase = removeProperty(courtApplicationCase, OFFENCES);
                laaCourtApplicationCase = addProperty(laaCourtApplicationCase, OFFENCES, offenceLaa.build());

                if (item.get(ID).equals(laaCourtApplicationCase.get(PROSECUTION_CASE_ID))) {
                    policeCourtApplicationCases.add(laaCourtApplicationCase);
                }
            }
        }
    }

    private void iterateDefendantsForOffences(final JsonArray defendants, final JsonArrayBuilder laaRmDefendents) {
        for (int j = 0; j < defendants.size(); j++) {
            final JsonArray offences = (JsonArray) defendants.getJsonObject(j).get(OFFENCES);
            final JsonArrayBuilder offenceLaa = createArrayBuilder();
            if (null != offences && !offences.isEmpty()) {
                for (int k = 0; k < offences.size(); k++) {
                    final JsonObject offence = offences.getJsonObject(k);
                    deleteLaaReferenceFromOffence(offence, offenceLaa);
                }
                laaRmDefendents.add(addProperty(defendants.getJsonObject(j), OFFENCES, offenceLaa.build()));
            }
        }
    }

    private void deleteLaaReferenceFromOffence(JsonObject offence, final JsonArrayBuilder offenceLaa) {
        if (offence.containsKey(LAA_APPLN_REFERENCE)) {
            offence = removeProperty(offence, LAA_APPLN_REFERENCE);
            offenceLaa.add(offence);
        } else {
            offenceLaa.add(offence);
        }
    }

    private void extractPoliceProsecutionCases(final JsonArray prosecutionCases, final JsonArrayBuilder policeCaseProsecutionCases) {
        for (int i = 0; i < prosecutionCases.size(); i++) {
            final JsonObject prosecutionCase = prosecutionCases.getJsonObject(i);
            final JsonObject prosecutionCaseIdentifier = prosecutionCase.getJsonObject(PROSECUTION_CASE_IDENTIFIER);
            final boolean policeFlag = referenceDataService.getPoliceFlag(prosecutionCaseIdentifier.getString(OU_CODE, null), prosecutionCaseIdentifier.getString(PROSECUTOR_CODE, null), requester);
            LOGGER.info("prosecutionCase ouCode {} and policeFlag {}", prosecutionCaseIdentifier.getString(OU_CODE, null), policeFlag);
            if (policeFlag) {
                policeCaseProsecutionCases.add(prosecutionCase);
            }
        }
    }

    public JsonObject removeProperty(final JsonObject origin, final String key) {
        final JsonObjectBuilder builder = getJsonObjectBuilder();

        for (final Map.Entry<String, JsonValue> entry : origin.entrySet()) {
            if (!entry.getKey().equals(key)) {
                builder.add(entry.getKey(), entry.getValue());
            }
        }
        return builder.build();
    }

    public static JsonObject addProperty(final JsonObject origin, final String key, final JsonValue value) {
        final JsonObjectBuilder builder = getJsonObjectBuilder();

        for (final Map.Entry<String, JsonValue> entry : origin.entrySet()) {
            builder.add(entry.getKey(), entry.getValue());
        }

        builder.add(key, value);

        return builder.build();
    }

    private static JsonObjectBuilder getJsonObjectBuilder() {
        return Json.createObjectBuilder();
    }

}