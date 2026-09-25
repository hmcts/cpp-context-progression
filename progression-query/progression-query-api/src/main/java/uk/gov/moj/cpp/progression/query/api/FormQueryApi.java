package uk.gov.moj.cpp.progression.query.api;

import static uk.gov.justice.services.messaging.JsonObjects.createArrayBuilder;
import static uk.gov.justice.services.messaging.JsonObjects.createObjectBuilder;
import static uk.gov.justice.services.core.enveloper.Enveloper.envelop;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;

import uk.gov.justice.services.core.annotation.Component;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.requester.Requester;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.progression.query.FormQueryView;

import javax.inject.Inject;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ServiceComponent(Component.QUERY_API)
public class FormQueryApi {
    private static final Logger LOGGER = LoggerFactory.getLogger(FormQueryApi.class);

    private static final String STRUCTURED_FORM_ID = "structuredFormId";
    private static final String COURT_FORM_ID = "courtFormId";
    private static final String DATA = "data";
    private static final String LAST_UPDATED = "lastUpdated";
    private static final String CASE_ID = "caseId";
    private static final String ID = "id";
    private static final String DATE = "date";
    private static final String UPDATED_BY = "updatedBy";
    private static final String STATUS = "status";
    private static final String MATERIAL_ID = "materialId";
    private static final String FIRST_NAME = "firstName";
    private static final String LAST_NAME = "lastName";
    private static final String FORM_DATA = "formData";
    private static final String DEFENDANTS = "defendants";
    private static final String FORM_TYPE = "formType";
    public static final String NAME = "name";


    @Inject
    private Requester requester;

    @Inject
    private FormQueryView formQueryView;

    @Handles("progression.query.forms-for-case")
    public JsonEnvelope getFormsForCase(final JsonEnvelope query) {
        return formQueryView.getFormsForCase(query);
    }

    @Handles("progression.query.form")
    public JsonEnvelope getForm(final JsonEnvelope query) {
        final String courtFormId = query.payloadAsJsonObject().getString(COURT_FORM_ID);
        final JsonEnvelope materialResponse = requester.request(envelop(
                createObjectBuilder().add(STRUCTURED_FORM_ID, courtFormId)
                        .build()).withName("material.query.structured-form").withMetadataFrom(query));

        final JsonObject formDataFromMaterial = materialResponse.payloadAsJsonObject();
        LOGGER.info("structured form from material: {}", formDataFromMaterial);

        final JsonEnvelope envelope = formQueryView.getForm(query);
        final JsonObject response = envelope.payloadAsJsonObject();
        final JsonObjectBuilder formResponseAsJsonObject = createObjectBuilder().add(CASE_ID, query.payloadAsJsonObject().getString(CASE_ID))
                .add(COURT_FORM_ID, courtFormId)
                .add(FORM_DATA, formDataFromMaterial.getString(DATA))
                .add(LAST_UPDATED, formDataFromMaterial.getString(LAST_UPDATED))
                .add(DEFENDANTS, response.getJsonArray(DEFENDANTS))
                .add(FORM_TYPE, response.getString(FORM_TYPE));

        return envelopeFrom(query.metadata(), formResponseAsJsonObject.build());
    }

    @Handles("progression.query.form-change-history")
    public JsonEnvelope getFormChangeHistory(final JsonEnvelope query) {
        final JsonEnvelope materialResponse = requester.request(envelop(createObjectBuilder().add(STRUCTURED_FORM_ID, query.payloadAsJsonObject().getString(COURT_FORM_ID)).build()).withName("material.query.structured-form-change-history").withMetadataFrom(query));
        final JsonArrayBuilder materialArrayBuilder = createArrayBuilder();

        materialResponse.payloadAsJsonObject().getJsonArray("structuredFormChangeHistory").forEach(
                ch -> materialArrayBuilder.add(convertChangeHistory((JsonObject) ch))
        );

        return envelopeFrom(query.metadata(), createObjectBuilder().add("formChangeHistory", materialArrayBuilder).build());

    }

    private JsonObject convertChangeHistory(final JsonObject changeHistory) {
        final JsonObjectBuilder historyBuilder = createObjectBuilder()
                .add(ID, changeHistory.getString(ID))
                .add(COURT_FORM_ID, changeHistory.getString(STRUCTURED_FORM_ID))
                .add(DATE, changeHistory.getString(DATE))
                .add(UPDATED_BY, buildUpdatedResponse(changeHistory))
                .add(DATA, changeHistory.getString(DATA))
                .add(STATUS, changeHistory.getString(STATUS));

        if (changeHistory.containsKey(MATERIAL_ID)) {
            historyBuilder.add(MATERIAL_ID, changeHistory.getString(MATERIAL_ID));
        }
        return historyBuilder.build();
    }

    private JsonObject buildUpdatedResponse(final JsonObject changeHistoryFromMaterialQuery) {
        final JsonObjectBuilder updatedUserBuilder = createObjectBuilder();
        final JsonObject updatedByFromMaterial = changeHistoryFromMaterialQuery.getJsonObject(UPDATED_BY);

        if (updatedByFromMaterial.containsKey(NAME)) {
            updatedUserBuilder.add(NAME, updatedByFromMaterial.getString(NAME));
        } else {
            updatedUserBuilder
                    .add(ID, updatedByFromMaterial.getString(ID))
                    .add(FIRST_NAME, updatedByFromMaterial.getString(FIRST_NAME))
                    .add(LAST_NAME, updatedByFromMaterial.getString(LAST_NAME));
        }
        return updatedUserBuilder.build();
    }
}
