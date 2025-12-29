package uk.gov.moj.cpp.progression.command;

import static uk.gov.justice.services.core.annotation.Component.COMMAND_API;
import static uk.gov.justice.services.messaging.Envelope.envelopeFrom;
import static uk.gov.justice.services.messaging.Envelope.metadataFrom;

import uk.gov.justice.core.courts.Address;
import uk.gov.justice.core.courts.ContactNumber;
import uk.gov.justice.core.courts.ProsecutionCaseIdentifier;
import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.common.converter.ObjectToJsonObjectConverter;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.requester.Requester;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.messaging.Metadata;
import uk.gov.moj.cpp.progression.service.RefDataService;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import javax.inject.Inject;
import uk.gov.justice.services.messaging.JsonObjects;
import javax.json.JsonArray;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.json.JsonValue;

@ServiceComponent(COMMAND_API)
public class ReferCasesToCourtCommandApi {
    public static final String INFORMANT_EMAIL_ADDRESS = "informantEmailAddress";
    public static final String MAJOR_CREDITOR_CODE = "majorCreditorCode";
    public static final String SHORT_NAME = "shortName";
    public static final String FULL_NAME = "fullName";
    public static final String OUCODE = "oucode";
    public static final String ID = "id";
    public static final String ADDRESS = "address";
    public static final String COURT_REFERRAL = "courtReferral";
    public static final String PROSECUTION_CASES = "prosecutionCases";
    public static final String PROSECUTION_CASE_IDENTIFIER = "prosecutionCaseIdentifier";
    @Inject
    private Sender sender;
    @Inject
    private Requester requester;
    @Inject
    private RefDataService referenceDataService;
    @Inject
    private ObjectToJsonObjectConverter objectToJsonObjectConverter;
    @Inject
    private JsonObjectToObjectConverter jsonObjectToObjectConverter;

    @Handles("progression.refer-cases-to-court")
    public void handle(final JsonEnvelope envelope) {
        final Metadata metadata = metadataFrom(envelope.metadata()).withName("progression.command.refer-cases-to-court").build();
        final JsonObject referCasesToCourt = getReferredCases(envelope.payloadAsJsonObject(), envelope);
        sender.send(envelopeFrom(metadata, referCasesToCourt));
    }

    private JsonObject getReferredCases(final JsonObject payload, final JsonEnvelope envelope) {
        final JsonObject referCasesToCourt = payload.getJsonObject(COURT_REFERRAL);
        return addProperty(payload, COURT_REFERRAL, getUpdatedCourtReferral(referCasesToCourt, envelope));
    }

    private JsonObject getUpdatedCourtReferral(final JsonObject referCasesToCourt, final JsonEnvelope envelope) {
        final JsonArray prosecutionCasesJsonArray = referCasesToCourt.getJsonArray(PROSECUTION_CASES);
        return addArrayToObject(referCasesToCourt, PROSECUTION_CASES, getUpdatedProsecutionCases(prosecutionCasesJsonArray, envelope));
    }

    private JsonArray getUpdatedProsecutionCases(final JsonArray originProsecutionCases, final JsonEnvelope envelope) {
        final JsonArrayBuilder builder = JsonObjects.createArrayBuilder();
        originProsecutionCases.forEach(jsonValue -> {
            final JsonObject jsonObject = (JsonObject) jsonValue;
            builder.add(addProperty(jsonObject, PROSECUTION_CASE_IDENTIFIER, getUpdatedCaseIdentifier(jsonObject.getJsonObject(PROSECUTION_CASE_IDENTIFIER), envelope)));
        });
        return builder.build();
    }

    private JsonObject addArrayToObject(final JsonObject origin, final String key, final JsonArray value) {
        final JsonObjectBuilder builder = createObjectBuilder(origin);
        builder.add(key, value);
        return builder.build();
    }

    public static JsonObject addProperty(final JsonObject origin, final String key, final JsonObject value) {
        final JsonObjectBuilder builder = createObjectBuilder(origin);
        builder.add(key, value);
        return builder.build();
    }

    private static JsonObjectBuilder createObjectBuilder(final JsonObject origin) {
        final JsonObjectBuilder builder = JsonObjects.createObjectBuilder();
        for (final Map.Entry<String, JsonValue> entry : origin.entrySet()) {
            builder.add(entry.getKey(), entry.getValue());
        }
        return builder;
    }

    private JsonObject getUpdatedCaseIdentifier(final JsonObject caseIdentifier, final JsonEnvelope envelope) {
        final ProsecutionCaseIdentifier originalProsecutionCaseIdentifier = jsonObjectToObjectConverter.convert(caseIdentifier, ProsecutionCaseIdentifier.class);
        final ProsecutionCaseIdentifier enrichedProsecutionCaseIdentifier = getEnrichedCaseIdentifier(originalProsecutionCaseIdentifier, envelope);
        return objectToJsonObjectConverter.convert(enrichedProsecutionCaseIdentifier);
    }

    private ProsecutionCaseIdentifier getEnrichedCaseIdentifier(final ProsecutionCaseIdentifier prosecutionCaseIdentifier, final JsonEnvelope envelope) {
        final Optional<JsonObject> cpsProsecutor = getProsecutorById(prosecutionCaseIdentifier.getProsecutionAuthorityId(), envelope);
        if (cpsProsecutor.isPresent()) {
            return getProsecutionCaseIdentifier(cpsProsecutor.get(), prosecutionCaseIdentifier);
        } else {
            return prosecutionCaseIdentifier;
        }
    }

    private Optional<JsonObject> getProsecutorById(final UUID authorityId, final JsonEnvelope envelope) {
        return referenceDataService.getProsecutor(envelope, authorityId, requester);
    }

    private ProsecutionCaseIdentifier getProsecutionCaseIdentifier(final JsonObject cpsProsecutor, final ProsecutionCaseIdentifier prosecutionCaseIdentifier) {

        final ProsecutionCaseIdentifier.Builder builder = ProsecutionCaseIdentifier.prosecutionCaseIdentifier();
        builder.withValuesFrom(prosecutionCaseIdentifier);
        if (cpsProsecutor.containsKey(INFORMANT_EMAIL_ADDRESS)) {
            builder.withContact(ContactNumber.contactNumber().withPrimaryEmail(cpsProsecutor.getString(INFORMANT_EMAIL_ADDRESS)).build());
        }
        if (cpsProsecutor.containsKey(MAJOR_CREDITOR_CODE)) {
            builder.withMajorCreditorCode(cpsProsecutor.getString(MAJOR_CREDITOR_CODE));
        }
        if (cpsProsecutor.containsKey(SHORT_NAME)) {
            builder.withProsecutionAuthorityCode(cpsProsecutor.getString(SHORT_NAME));
        }
        if (cpsProsecutor.containsKey(FULL_NAME)) {
            builder.withProsecutionAuthorityName(cpsProsecutor.getString(FULL_NAME));
        }
        if (cpsProsecutor.containsKey(OUCODE)) {
            builder.withProsecutionAuthorityOUCode(cpsProsecutor.getString(OUCODE));
        }

        if(cpsProsecutor.containsKey(ADDRESS)){
            final Address address = jsonObjectToObjectConverter.convert((JsonObject) cpsProsecutor.get(ADDRESS), Address.class);
            builder.withAddress(address);
        }

        return builder.build();
    }

}
