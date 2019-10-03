package uk.gov.moj.cpp.results.domain.transformation.util;


import javax.json.JsonArray;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import java.util.Map;

import static java.util.Objects.nonNull;
import static javax.json.Json.createArrayBuilder;
import static javax.json.Json.createObjectBuilder;
import static uk.gov.moj.cpp.results.domain.transformation.util.CommonHelper.*;
import static uk.gov.moj.cpp.results.domain.transformation.util.JudicialResultHelper.transformJudicialResults;

@SuppressWarnings({"squid:S1188", "squid:S3776"})
public class OffenceHelper {
    private OffenceHelper() {
    }

    public static JsonArray transformOffences(final JsonArray offenceJsonObjects) {
        final JsonArrayBuilder offenceList = createArrayBuilder();
        offenceJsonObjects.forEach(o -> {
            final JsonObject offence = (JsonObject) o;

            //add required fields,
            final JsonObjectBuilder offenceBuilder = createObjectBuilder()
                    .add(ID, offence.getString(ID))
                    .add(OFFENCE_DEFINITION_ID, offence.getString(OFFENCE_DEFINITION_ID))
                    .add(OFFENCE_CODE, offence.getString(OFFENCE_CODE))
                    .add(OFFENCE_TITLE, offence.getString(OFFENCE_TITLE))
                    .add(WORDING, offence.getString(WORDING))
                    .add(START_DATE, offence.getString(START_DATE))
                    .add(COUNT, offence.getInt(COUNT));

            // add optional fields
            if (offence.containsKey(END_DATE)) {
                offenceBuilder.add(END_DATE, offence.getString(END_DATE));
            }
            if (offence.containsKey(ARREST_DATE)) {
                offenceBuilder.add(ARREST_DATE, offence.getString(ARREST_DATE));
            }
            if (offence.containsKey(CHARGE_DATE)) {
                offenceBuilder.add(CHARGE_DATE, offence.getString(CHARGE_DATE));
            }
            if (offence.containsKey(ORDER_INDEX)) {
                offenceBuilder.add(ORDER_INDEX, offence.getInt(ORDER_INDEX));
            }

            if (offence.containsKey(WORDING_WELSH)) {
                offenceBuilder.add(WORDING_WELSH, offence.getString(WORDING_WELSH));
            }
            if (offence.containsKey(OFFENCE_TITLE_WELSH)) {
                offenceBuilder.add(OFFENCE_TITLE_WELSH, offence.getString(OFFENCE_TITLE_WELSH));
            }
            if (offence.containsKey(OFFENCE_LEGISLATION)) {
                offenceBuilder.add(OFFENCE_LEGISLATION, offence.getString(OFFENCE_LEGISLATION));
            }
            if (offence.containsKey(OFFENCE_LEGISLATION_WELSH)) {
                offenceBuilder.add(OFFENCE_LEGISLATION_WELSH, offence.getString(OFFENCE_LEGISLATION_WELSH));
            }
            if (offence.containsKey(MODE_OF_TRIAL)) {
                offenceBuilder.add(MODE_OF_TRIAL, offence.getString(MODE_OF_TRIAL));
            }

            if (offence.containsKey(DATE_OF_INFORMATION)) {
                offenceBuilder.add(DATE_OF_INFORMATION, offence.getString(DATE_OF_INFORMATION));
            }
            if (offence.containsKey(CONVICTION_DATE)) {
                offenceBuilder.add(CONVICTION_DATE, offence.getString(CONVICTION_DATE));
            }
            if (offence.containsKey(NOTIFIED_PLEA)) {
                offenceBuilder.add(NOTIFIED_PLEA, offence.getJsonObject(NOTIFIED_PLEA));
            }
            if (offence.containsKey(INDICATED_PLEA)) {
                offenceBuilder.add(INDICATED_PLEA, offence.getJsonObject(INDICATED_PLEA));
            }
            if (offence.containsKey(VERDICT)) {
                offenceBuilder.add(VERDICT, transformVerdict(offence.getJsonObject(VERDICT)));
            }

            if (offence.containsKey(OFFENCE_FACTS)) {
                offenceBuilder.add(OFFENCE_FACTS, transformOffenceFacts(offence.getJsonObject(OFFENCE_FACTS)));
            }
            if (offence.containsKey(PLEA)) {
                offenceBuilder.add(PLEA, offence.getJsonObject(PLEA));
            }
            offenceList.add(offenceBuilder.build());
        });
        return offenceList.build();

    }

    public static JsonArray transformOffences(final JsonArray offenceJsonObjects,
                                              final JsonObject hearing) {
        final Map<String, JsonArray> resultLines = arrangeSharedResultLineByLevel(hearing.getJsonArray(SHARED_HEARING_LINES));
        final JsonArrayBuilder offenceList = createArrayBuilder();
        offenceJsonObjects.forEach(o -> {
            final JsonObject offence = (JsonObject) o;

            //add required fields,
            final JsonObjectBuilder offenceBuilder = createObjectBuilder()
                    .add(ID, offence.getString(ID))
                    .add(OFFENCE_DEFINITION_ID, offence.getString(OFFENCE_DEFINITION_ID))
                    .add(OFFENCE_CODE, offence.getString(OFFENCE_CODE))
                    .add(OFFENCE_TITLE, offence.getString(OFFENCE_TITLE))
                    .add(WORDING, offence.getString(WORDING))
                    .add(START_DATE, offence.getString(START_DATE))
                    .add(COUNT, offence.getInt(COUNT));

            // add optional fields
            if (offence.containsKey(END_DATE)) {
                offenceBuilder.add(END_DATE, offence.getString(END_DATE));
            }
            if (offence.containsKey(ARREST_DATE)) {
                offenceBuilder.add(ARREST_DATE, offence.getString(ARREST_DATE));
            }
            if (offence.containsKey(CHARGE_DATE)) {
                offenceBuilder.add(CHARGE_DATE, offence.getString(CHARGE_DATE));
            }
            if (offence.containsKey(ORDER_INDEX)) {
                offenceBuilder.add(ORDER_INDEX, offence.getInt(ORDER_INDEX));
            }

            if (offence.containsKey(WORDING_WELSH)) {
                offenceBuilder.add(WORDING_WELSH, offence.getString(WORDING_WELSH));
            }
            if (offence.containsKey(OFFENCE_TITLE_WELSH)) {
                offenceBuilder.add(OFFENCE_TITLE_WELSH, offence.getString(OFFENCE_TITLE_WELSH));
            }
            if (offence.containsKey(OFFENCE_LEGISLATION)) {
                offenceBuilder.add(OFFENCE_LEGISLATION, offence.getString(OFFENCE_LEGISLATION));
            }
            if (offence.containsKey(OFFENCE_LEGISLATION_WELSH)) {
                offenceBuilder.add(OFFENCE_LEGISLATION_WELSH, offence.getString(OFFENCE_LEGISLATION_WELSH));
            }
            if (offence.containsKey(MODE_OF_TRIAL)) {
                offenceBuilder.add(MODE_OF_TRIAL, offence.getString(MODE_OF_TRIAL));
            }

            if (offence.containsKey(DATE_OF_INFORMATION)) {
                offenceBuilder.add(DATE_OF_INFORMATION, offence.getString(DATE_OF_INFORMATION));
            }
            if (offence.containsKey(CONVICTION_DATE)) {
                offenceBuilder.add(CONVICTION_DATE, offence.getString(CONVICTION_DATE));
            }
            if (offence.containsKey(NOTIFIED_PLEA)) {
                offenceBuilder.add(NOTIFIED_PLEA, offence.getJsonObject(NOTIFIED_PLEA));
            }
            if (offence.containsKey(INDICATED_PLEA)) {
                offenceBuilder.add(INDICATED_PLEA, offence.getJsonObject(INDICATED_PLEA));
            }
            if (offence.containsKey(VERDICT)) {
                offenceBuilder.add(VERDICT, transformVerdict(offence.getJsonObject(VERDICT)));
            }

            if (offence.containsKey(OFFENCE_FACTS)) {
                offenceBuilder.add(OFFENCE_FACTS, transformOffenceFacts(offence.getJsonObject(OFFENCE_FACTS)));
            }
            if (offence.containsKey(PLEA)) {
                offenceBuilder.add(PLEA, offence.getJsonObject(PLEA));
            }

            if (nonNull(resultLines.get("offence"))) {
                offenceBuilder.add("judicialResults", transformJudicialResults(resultLines.get("offence"), hearing));
            }
            offenceList.add(offenceBuilder.build());
        });
        return offenceList.build();

    }

    public static JsonObject transformOffenceFacts(final JsonObject jsonObject) {
        final JsonObjectBuilder jsonObjectBuilder = createObjectBuilder();
        if (jsonObject.containsKey(VEHICLE_REGISTRATION)) {
            jsonObjectBuilder.add(VEHICLE_REGISTRATION, jsonObject.getString(VEHICLE_REGISTRATION));
        }
        if (jsonObject.containsKey(ALCOHOL_READING_AMOUNT)) {
            jsonObjectBuilder.add(ALCOHOL_READING_AMOUNT, jsonObject.getString(ALCOHOL_READING_AMOUNT));
        }
        if (jsonObject.containsKey(ALCOHOL_READING_METHOD)) {
            jsonObjectBuilder.add("alcoholReadingMethodCode", jsonObject.getString(ALCOHOL_READING_METHOD));
        }

        return jsonObjectBuilder.build();
    }

    public static JsonObject transformVerdict(final JsonObject jsonObject) {
        final JsonObjectBuilder jsonObjectBuilder = createObjectBuilder();
        //required
        jsonObjectBuilder
                .add(ORIGINATING_HEARING_ID, jsonObject.getString(ORIGINATING_HEARING_ID))
                .add(OFFENCE_ID, jsonObject.getString(OFFENCE_ID))
                .add(VERDICT_DATE, jsonObject.getString(VERDICT_DATE))
                .add(VERDICT_TYPE, transformVerdictType(jsonObject.getJsonObject(VERDICT_TYPE)));

        // add optional attribute
        if (jsonObject.containsKey(JURORS)) {
            jsonObjectBuilder.add(JURORS, jsonObject.getJsonObject(JURORS));
        }
        if (jsonObject.containsKey(LESSER_OR_ALTERNATIVE_OFFENCE)) {
            jsonObjectBuilder.add(LESSER_OR_ALTERNATIVE_OFFENCE, jsonObject.getJsonObject(LESSER_OR_ALTERNATIVE_OFFENCE));
        }
        return jsonObjectBuilder.build();
    }

    public static JsonObject transformVerdictType(final JsonObject jsonObject) {
        final JsonObjectBuilder jsonObjectBuilder = createObjectBuilder();
        //required
        jsonObjectBuilder
                .add(ID, jsonObject.getString(VERDICT_TYPE_ID))
                .add(CATEGORY, jsonObject.getString(CATEGORY))
                .add(CATEGORY_TYPE, jsonObject.getString(CATEGORY_TYPE));
        // add optional attribute
        if (jsonObject.containsKey(SEQUENCE)) {
            jsonObjectBuilder.add(SEQUENCE, jsonObject.getInt(SEQUENCE));
        }
        if (jsonObject.containsKey(DESCRIPTION)) {
            jsonObjectBuilder.add(DESCRIPTION, jsonObject.getString(DESCRIPTION));
        }
        return jsonObjectBuilder.build();
    }
}