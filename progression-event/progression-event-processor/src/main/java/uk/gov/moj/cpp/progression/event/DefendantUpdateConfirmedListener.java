package uk.gov.moj.cpp.progression.event;

import static uk.gov.justice.services.core.annotation.Component.EVENT_PROCESSOR;

import javax.inject.Inject;
import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.progression.helper.NullAwareJsonObjectBuilder;

@ServiceComponent(EVENT_PROCESSOR)
public class DefendantUpdateConfirmedListener {

    private static final String POST_CODE = "postCode";
    private static final String ADDRESS4 = "address4";
    private static final String ADDRESS3 = "address3";
    private static final String ADDRESS2 = "address2";
    private static final String ADDRESS1 = "address1";
    private static final String LANGUAGE = "language";
    private static final String NAME = "name";
    private static final String EMAIL = "email";
    private static final String FAX = "fax";
    private static final String MOBILE = "mobile";
    private static final String WORK_TELEPHONE = "workTelephone";
    private static final String HOME_TELEPHONE = "homeTelephone";
    private static final String GENDER = "gender";
    private static final String NATIONALITY = "nationality";
    private static final String DATE_OF_BIRTH = "dateOfBirth";
    private static final String LAST_NAME = "lastName";
    private static final String FIRST_NAME = "firstName";
    private static final String TITLE = "title";
    private static final String ADDRESS = "address";
    private static final String INTERPRETER = "interpreter";
    private static final String DEFENCE_ORGANISATION = "defenceOrganisation";
    private static final String CUSTODY_TIME_LIMIT_DATE = "custodyTimeLimitDate";
    private static final String BAIL_STATUS = "bailStatus";
    private static final String ID = "id";
    private static final String CASE_ID = "caseId";
    private static final String DEFENDANT_ID = "defendantId";
    private static final String PERSON = "person";
    protected static final String PUBLIC_CASE_DEFENDANT_CHANGED =
                    "public.progression.case-defendant-changed";
    private static final Logger LOGGER = LoggerFactory
                    .getLogger(DefendantUpdateConfirmedListener.class.getCanonicalName());


    @Inject
    private Sender sender;

    @Inject
    private Enveloper enveloper;

    @Handles("progression.events.defendant-update-confirmed")
    public void publishCaseDefendantChanged(final JsonEnvelope event) {

        final JsonObject eventPayload = event.payloadAsJsonObject();
        final String caseId = eventPayload.getString(CASE_ID);
        final String defendantId = eventPayload.getString(DEFENDANT_ID);
        LOGGER.trace("Received progression.events.defendant-update-confirmed event for caseId {} , defendantId {} ",
                        caseId, defendantId);
        final JsonObject payload = Json.createObjectBuilder()
                        .add(CASE_ID, caseId)
                        .add("defendants", createDefendants(eventPayload, defendantId)).build();
        this.sender.send(this.enveloper.withMetadataFrom(event, PUBLIC_CASE_DEFENDANT_CHANGED)
                        .apply(payload));
    }

    private JsonArray createDefendants(final JsonObject payload, final String defendantId) {
        final JsonObject interpreter = payload.getJsonObject(INTERPRETER);
        final JsonObjectBuilder nullAwareBuilder =
                        NullAwareJsonObjectBuilder.wrap(Json.createObjectBuilder());
        final JsonObjectBuilder nullAwareInterpreterBuilder =
                        NullAwareJsonObjectBuilder.wrap(Json.createObjectBuilder());
        return Json.createArrayBuilder().add(nullAwareBuilder
                        .add(ID, defendantId)
                        .add(PERSON, createDefendantPerson(payload))
                        .add(BAIL_STATUS, payload.getString(BAIL_STATUS, null))
                        .add(CUSTODY_TIME_LIMIT_DATE,
                                        payload.getString(CUSTODY_TIME_LIMIT_DATE, null))
                        .add(DEFENCE_ORGANISATION, payload.getString(DEFENCE_ORGANISATION, null))
                        .add(INTERPRETER, interpreter == null ? null :
                                        nullAwareInterpreterBuilder
                                                        .add(NAME, interpreter.getString(
                                                                                        NAME,
                                                                        null))
                                                        .add(LANGUAGE, interpreter
                                                                        .getString(LANGUAGE,
                                                                                        null))))
                        .build();
    }

    private JsonObjectBuilder createDefendantPerson(final JsonObject payload) {
        final JsonObject person = payload.getJsonObject(PERSON);
        final JsonObject address = person.getJsonObject(ADDRESS);
        final JsonObjectBuilder nullAwareBuilder =
                        NullAwareJsonObjectBuilder.wrap(Json.createObjectBuilder());
        final JsonObjectBuilder nullAwareAddressBuilder =
                        NullAwareJsonObjectBuilder.wrap(Json.createObjectBuilder());
        return nullAwareBuilder.add(ID, person.getString(ID))
                        .add(TITLE, person.getString(TITLE, null))
                        .add(FIRST_NAME, person.getString(FIRST_NAME, null))
                        .add(LAST_NAME, person.getString(LAST_NAME, null))
                        .add(DATE_OF_BIRTH, person.getString(DATE_OF_BIRTH, null))
                        .add(NATIONALITY, person.getString(NATIONALITY, null))
                        .add(GENDER, person.getString(GENDER, null))
                        .add(HOME_TELEPHONE, person.getString(HOME_TELEPHONE, null))
                        .add(WORK_TELEPHONE, person.getString(WORK_TELEPHONE, null))
                        .add(MOBILE, person.getString(MOBILE, null))
                        .add(FAX, person.getString(FAX, null))
                        .add(EMAIL, person.getString(EMAIL, null))
                        .add(ADDRESS, address == null ? null
                                        : nullAwareAddressBuilder
                                        .add(ADDRESS1, address.getString(ADDRESS1, null))
                                        .add(ADDRESS2, address.getString(ADDRESS2, null))
                                        .add(ADDRESS3, address.getString(ADDRESS3, null))
                                        .add(ADDRESS4, address.getString(ADDRESS4, null))
                                        .add(POST_CODE, address.getString(POST_CODE, null)));

    }


}
