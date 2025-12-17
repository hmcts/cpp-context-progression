package uk.gov.moj.cpp.progression.helper;

import static java.util.Objects.nonNull;
import static org.apache.commons.lang3.StringUtils.isNotEmpty;

import uk.gov.justice.core.courts.Address;
import uk.gov.justice.core.courts.Cases;
import uk.gov.justice.core.courts.Defendant;
import uk.gov.justice.core.courts.Person;
import uk.gov.justice.core.courts.ProsecutionCase;
import uk.gov.justice.services.common.converter.ListToJsonArrayConverter;

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;

import javax.inject.Inject;
import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;

public class MatchedDefendantHelper {
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter ZONE_DATETIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
    private static final String DEFENDANT_ID = "defendantId";
    private static final String MASTER_DEFENDANT_ID = "masterDefendantId";
    private static final String PROSECUTION_CASE_ID = "prosecutionCaseId";
    private static final String CASE_REFERENCE = "caseReference";
    private static final String COURT_PROCEEDINGS_INITIATED = "courtProceedingsInitiated";
    private static final String MIDDLE_NAME = "middleName";
    private static final String DEFENDANTS_MATCHED_COUNT = "defendantsMatchedCount";
    private static final String DEFENDANTS_MATCHED = "defendantsMatched";
    private static final String ADDRESS_LINE_1 = "addressLine1";
    private static final String ADDRESS_LINE_2 = "addressLine2";
    private static final String ADDRESS_LINE_3 = "addressLine3";
    private static final String ADDRESS_LINE_4 = "addressLine4";
    private static final String ADDRESS_LINE_5 = "addressLine5";
    private static final String POSTCODE = "postcode";
    private static final String ADDRESS = "address";
    private static final String PNC_ID = "pncId";
    private static final String CRO_NUMBER = "croNumber";
    private static final String LAST_NAME = "lastName";
    private static final String FIRST_NAME = "firstName";
    private static final String DATE_OF_BIRTH = "dateOfBirth";

    @Inject
    ListToJsonArrayConverter listToJsonArrayConverter;

    public  String transformToPartialMatchDefendantPayload(final Defendant defendant, final ProsecutionCase prosecutionCase, final List<Cases> casesList ) {
        final JsonObjectBuilder jsonObjectBuilder = Json.createObjectBuilder();
        jsonObjectBuilder.add(DEFENDANT_ID, defendant.getId().toString());
        addToJsonObjectNullSafe(jsonObjectBuilder,MASTER_DEFENDANT_ID, defendant.getMasterDefendantId());
        addToJsonObjectNullSafe(jsonObjectBuilder,PROSECUTION_CASE_ID, prosecutionCase.getId());
        addToJsonObjectNullSafe(jsonObjectBuilder,CASE_REFERENCE, nonNull(prosecutionCase.getProsecutionCaseIdentifier().getCaseURN()) ?
                prosecutionCase.getProsecutionCaseIdentifier().getCaseURN() :
                prosecutionCase.getProsecutionCaseIdentifier().getProsecutionAuthorityReference());
        addToJsonObjectNullSafe(jsonObjectBuilder,COURT_PROCEEDINGS_INITIATED, defendant.getCourtProceedingsInitiated());

        final Person personDetails = defendant.getPersonDefendant().getPersonDetails();
        addToJsonObjectNullSafe(jsonObjectBuilder, FIRST_NAME, personDetails.getFirstName());
        addToJsonObjectNullSafe(jsonObjectBuilder, MIDDLE_NAME, personDetails.getMiddleName());
        jsonObjectBuilder.add(LAST_NAME, personDetails.getLastName());
        if (nonNull(personDetails.getDateOfBirth())) {
            addToJsonObjectNullSafe(jsonObjectBuilder, DATE_OF_BIRTH, FORMATTER.format(personDetails.getDateOfBirth()));
        }

        addToJsonObjectNullSafe(jsonObjectBuilder, PNC_ID, defendant.getPncId());
        addToJsonObjectNullSafe(jsonObjectBuilder, CRO_NUMBER, defendant.getCroNumber());
        addAddress(defendant, jsonObjectBuilder);
        jsonObjectBuilder.add(DEFENDANTS_MATCHED_COUNT, casesList.size());

        final JsonArrayBuilder jsonDefendantsMatchedBuilder = Json.createArrayBuilder();
        casesList.stream()
                .forEach(cases -> {
                    final JsonArray jsonArray = listToJsonArrayConverter.convert(cases.getDefendants());
                    transformToDefendantsMatched(jsonArray, cases.getProsecutionCaseId(), cases.getCaseReference(), jsonDefendantsMatchedBuilder);
                });
        jsonObjectBuilder.add(DEFENDANTS_MATCHED, jsonDefendantsMatchedBuilder.build());

        return jsonObjectBuilder.build().toString();
    }

    private  void addAddress(final Defendant defendant, final JsonObjectBuilder jsonObjectBuilder) {
        if (nonNull(defendant.getPersonDefendant().getPersonDetails().getAddress())) {
            final JsonObjectBuilder addressJsonObjectBuilder = Json.createObjectBuilder();
            final Address address = defendant.getPersonDefendant().getPersonDetails().getAddress();
            addToJsonObjectNullSafe(addressJsonObjectBuilder, ADDRESS_LINE_1, address.getAddress1());
            addToJsonObjectNullSafe(addressJsonObjectBuilder, ADDRESS_LINE_2, address.getAddress2());
            addToJsonObjectNullSafe(addressJsonObjectBuilder, ADDRESS_LINE_3, address.getAddress3());
            addToJsonObjectNullSafe(addressJsonObjectBuilder, ADDRESS_LINE_4, address.getAddress4());
            addToJsonObjectNullSafe(addressJsonObjectBuilder, ADDRESS_LINE_5, address.getAddress5());
            addToJsonObjectNullSafe(addressJsonObjectBuilder, POSTCODE, address.getPostcode());
            jsonObjectBuilder.add(ADDRESS, addressJsonObjectBuilder.build());
        }
    }

    private  void transformToDefendantsMatched(final JsonArray defendantsArray, final String prosecutionCaseId,
                                                    final String caseReference, final JsonArrayBuilder jsonArrayBuilder) {
        defendantsArray.stream()
                .map(j -> (JsonObject) j)
                .forEach(jsonObject -> {
                            final JsonObjectBuilder jsonObjectBuilder = Json.createObjectBuilder();
                            jsonObject.forEach(jsonObjectBuilder::add);
                            jsonObjectBuilder.add(PROSECUTION_CASE_ID, prosecutionCaseId);
                            jsonObjectBuilder.add(CASE_REFERENCE, caseReference);
                            jsonArrayBuilder.add(jsonObjectBuilder.build());
                        }
                );
    }

    public  String getDefendantName(Defendant defendant) {
        if (isNotEmpty(defendant.getPersonDefendant().getPersonDetails().getMiddleName())) {
            return defendant.getPersonDefendant().getPersonDetails().getFirstName() + " " + defendant.getPersonDefendant().getPersonDetails().getMiddleName() + " " + defendant.getPersonDefendant().getPersonDetails().getLastName();
        }

        return defendant.getPersonDefendant().getPersonDetails().getFirstName() + " " + defendant.getPersonDefendant().getPersonDetails().getLastName();
    }

    public  void addToJsonObjectNullSafe(final JsonObjectBuilder jsonObjectBuilder, String key, String value) {
        if (nonNull(value)) {
            jsonObjectBuilder.add(key, value);
        }
    }

    public  void addToJsonObjectNullSafe(final JsonObjectBuilder jsonObjectBuilder, String key, UUID value) {
        if (nonNull(value)) {
            jsonObjectBuilder.add(key, value.toString());
        }
    }

    public  void addToJsonObjectNullSafe(final JsonObjectBuilder jsonObjectBuilder, String key, ZonedDateTime value) {
        if (nonNull(value)) {
            jsonObjectBuilder.add(key, ZONE_DATETIME_FORMATTER.format(value));
        }
    }
}
