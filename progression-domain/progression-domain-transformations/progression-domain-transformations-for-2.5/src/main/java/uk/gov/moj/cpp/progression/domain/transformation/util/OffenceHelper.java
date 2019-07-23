package uk.gov.moj.cpp.progression.domain.transformation.util;


import javax.json.JsonArray;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import java.util.Map;

import static java.util.Objects.nonNull;
import static javax.json.Json.createArrayBuilder;
import static javax.json.Json.createObjectBuilder;
import static uk.gov.moj.cpp.progression.domain.transformation.util.JudicialResultHelper.transformJudicialResults;

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
                    .add(CommonHelper.ID, offence.getString(CommonHelper.ID))
                    .add(CommonHelper.OFFENCE_DEFINITION_ID, offence.getString(CommonHelper.OFFENCE_DEFINITION_ID))
                    .add(CommonHelper.OFFENCE_CODE, offence.getString(CommonHelper.OFFENCE_CODE))
                    .add(CommonHelper.OFFENCE_TITLE, offence.getString(CommonHelper.OFFENCE_TITLE))
                    .add(CommonHelper.WORDING, offence.getString(CommonHelper.WORDING))
                    .add(CommonHelper.START_DATE, offence.getString(CommonHelper.START_DATE))
                    .add(CommonHelper.COUNT, offence.getInt(CommonHelper.COUNT));

            // add optional fields
            if (offence.containsKey(CommonHelper.END_DATE)) {
                offenceBuilder.add(CommonHelper.END_DATE, offence.getString(CommonHelper.END_DATE));
            }
            if (offence.containsKey(CommonHelper.ARREST_DATE)) {
                offenceBuilder.add(CommonHelper.ARREST_DATE, offence.getString(CommonHelper.ARREST_DATE));
            }
            if (offence.containsKey(CommonHelper.CHARGE_DATE)) {
                offenceBuilder.add(CommonHelper.CHARGE_DATE, offence.getString(CommonHelper.CHARGE_DATE));
            }
            if (offence.containsKey(CommonHelper.ORDER_INDEX)) {
                offenceBuilder.add(CommonHelper.ORDER_INDEX, offence.getInt(CommonHelper.ORDER_INDEX));
            }

            if (offence.containsKey(CommonHelper.WORDING_WELSH)) {
                offenceBuilder.add(CommonHelper.WORDING_WELSH, offence.getString(CommonHelper.WORDING_WELSH));
            }
            if (offence.containsKey(CommonHelper.OFFENCE_TITLE_WELSH)) {
                offenceBuilder.add(CommonHelper.OFFENCE_TITLE_WELSH, offence.getString(CommonHelper.OFFENCE_TITLE_WELSH));
            }
            if (offence.containsKey(CommonHelper.OFFENCE_LEGISLATION)) {
                offenceBuilder.add(CommonHelper.OFFENCE_LEGISLATION, offence.getString(CommonHelper.OFFENCE_LEGISLATION));
            }
            if (offence.containsKey(CommonHelper.OFFENCE_LEGISLATION_WELSH)) {
                offenceBuilder.add(CommonHelper.OFFENCE_LEGISLATION_WELSH, offence.getString(CommonHelper.OFFENCE_LEGISLATION_WELSH));
            }
            if (offence.containsKey(CommonHelper.MODE_OF_TRIAL)) {
                offenceBuilder.add(CommonHelper.MODE_OF_TRIAL, offence.getString(CommonHelper.MODE_OF_TRIAL));
            }

            if (offence.containsKey(CommonHelper.DATE_OF_INFORMATION)) {
                offenceBuilder.add(CommonHelper.DATE_OF_INFORMATION, offence.getString(CommonHelper.DATE_OF_INFORMATION));
            }
            if (offence.containsKey(CommonHelper.CONVICTION_DATE)) {
                offenceBuilder.add(CommonHelper.CONVICTION_DATE, offence.getString(CommonHelper.CONVICTION_DATE));
            }
            if (offence.containsKey(CommonHelper.NOTIFIED_PLEA)) {
                offenceBuilder.add(CommonHelper.NOTIFIED_PLEA, offence.getJsonObject(CommonHelper.NOTIFIED_PLEA));
            }
            if (offence.containsKey(CommonHelper.INDICATED_PLEA)) {
                offenceBuilder.add(CommonHelper.INDICATED_PLEA, offence.getJsonObject(CommonHelper.INDICATED_PLEA));
            }
            if (offence.containsKey(CommonHelper.VERDICT)) {
                offenceBuilder.add(CommonHelper.VERDICT, transformVerdict(offence.getJsonObject(CommonHelper.VERDICT)));
            }

            if (offence.containsKey(CommonHelper.OFFENCE_FACTS)) {
                offenceBuilder.add(CommonHelper.OFFENCE_FACTS, transformOffenceFacts(offence.getJsonObject(CommonHelper.OFFENCE_FACTS)));
            }
            if (offence.containsKey(CommonHelper.PLEA)) {
                offenceBuilder.add(CommonHelper.PLEA, offence.getJsonObject(CommonHelper.PLEA));
            }
            offenceList.add(offenceBuilder.build());
        });
        return offenceList.build();

    }

    public static JsonArray transformOffences(final JsonArray offenceJsonObjects,
                                              final JsonObject hearing) {
        final Map<String, JsonArray> resultLines = CommonHelper.arrangeSharedResultLineByLevel(hearing.getJsonArray(CommonHelper.SHARED_HEARING_LINES));
        final JsonArrayBuilder offenceList = createArrayBuilder();
        offenceJsonObjects.forEach(o -> {
            final JsonObject offence = (JsonObject) o;

            //add required fields,
            final JsonObjectBuilder offenceBuilder = createObjectBuilder()
                    .add(CommonHelper.ID, offence.getString(CommonHelper.ID))
                    .add(CommonHelper.OFFENCE_DEFINITION_ID, offence.getString(CommonHelper.OFFENCE_DEFINITION_ID))
                    .add(CommonHelper.OFFENCE_CODE, offence.getString(CommonHelper.OFFENCE_CODE))
                    .add(CommonHelper.OFFENCE_TITLE, offence.getString(CommonHelper.OFFENCE_TITLE))
                    .add(CommonHelper.WORDING, offence.getString(CommonHelper.WORDING))
                    .add(CommonHelper.START_DATE, offence.getString(CommonHelper.START_DATE))
                    .add(CommonHelper.COUNT, offence.getInt(CommonHelper.COUNT));

            // add optional fields
            if (offence.containsKey(CommonHelper.END_DATE)) {
                offenceBuilder.add(CommonHelper.END_DATE, offence.getString(CommonHelper.END_DATE));
            }
            if (offence.containsKey(CommonHelper.ARREST_DATE)) {
                offenceBuilder.add(CommonHelper.ARREST_DATE, offence.getString(CommonHelper.ARREST_DATE));
            }
            if (offence.containsKey(CommonHelper.CHARGE_DATE)) {
                offenceBuilder.add(CommonHelper.CHARGE_DATE, offence.getString(CommonHelper.CHARGE_DATE));
            }
            if (offence.containsKey(CommonHelper.ORDER_INDEX)) {
                offenceBuilder.add(CommonHelper.ORDER_INDEX, offence.getInt(CommonHelper.ORDER_INDEX));
            }

            if (offence.containsKey(CommonHelper.WORDING_WELSH)) {
                offenceBuilder.add(CommonHelper.WORDING_WELSH, offence.getString(CommonHelper.WORDING_WELSH));
            }
            if (offence.containsKey(CommonHelper.OFFENCE_TITLE_WELSH)) {
                offenceBuilder.add(CommonHelper.OFFENCE_TITLE_WELSH, offence.getString(CommonHelper.OFFENCE_TITLE_WELSH));
            }
            if (offence.containsKey(CommonHelper.OFFENCE_LEGISLATION)) {
                offenceBuilder.add(CommonHelper.OFFENCE_LEGISLATION, offence.getString(CommonHelper.OFFENCE_LEGISLATION));
            }
            if (offence.containsKey(CommonHelper.OFFENCE_LEGISLATION_WELSH)) {
                offenceBuilder.add(CommonHelper.OFFENCE_LEGISLATION_WELSH, offence.getString(CommonHelper.OFFENCE_LEGISLATION_WELSH));
            }
            if (offence.containsKey(CommonHelper.MODE_OF_TRIAL)) {
                offenceBuilder.add(CommonHelper.MODE_OF_TRIAL, offence.getString(CommonHelper.MODE_OF_TRIAL));
            }

            if (offence.containsKey(CommonHelper.DATE_OF_INFORMATION)) {
                offenceBuilder.add(CommonHelper.DATE_OF_INFORMATION, offence.getString(CommonHelper.DATE_OF_INFORMATION));
            }
            if (offence.containsKey(CommonHelper.CONVICTION_DATE)) {
                offenceBuilder.add(CommonHelper.CONVICTION_DATE, offence.getString(CommonHelper.CONVICTION_DATE));
            }
            if (offence.containsKey(CommonHelper.NOTIFIED_PLEA)) {
                offenceBuilder.add(CommonHelper.NOTIFIED_PLEA, offence.getJsonObject(CommonHelper.NOTIFIED_PLEA));
            }
            if (offence.containsKey(CommonHelper.INDICATED_PLEA)) {
                offenceBuilder.add(CommonHelper.INDICATED_PLEA, offence.getJsonObject(CommonHelper.INDICATED_PLEA));
            }
            if (offence.containsKey(CommonHelper.VERDICT)) {
                offenceBuilder.add(CommonHelper.VERDICT, transformVerdict(offence.getJsonObject(CommonHelper.VERDICT)));
            }

            if (offence.containsKey(CommonHelper.OFFENCE_FACTS)) {
                offenceBuilder.add(CommonHelper.OFFENCE_FACTS, transformOffenceFacts(offence.getJsonObject(CommonHelper.OFFENCE_FACTS)));
            }
            if (offence.containsKey(CommonHelper.PLEA)) {
                offenceBuilder.add(CommonHelper.PLEA, offence.getJsonObject(CommonHelper.PLEA));
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
        if (jsonObject.containsKey(CommonHelper.VEHICLE_REGISTRATION)) {
            jsonObjectBuilder.add(CommonHelper.VEHICLE_REGISTRATION, jsonObject.getString(CommonHelper.VEHICLE_REGISTRATION));
        }
        if (jsonObject.containsKey(CommonHelper.ALCOHOL_READING_AMOUNT)) {
            jsonObjectBuilder.add(CommonHelper.ALCOHOL_READING_AMOUNT, jsonObject.getString(CommonHelper.ALCOHOL_READING_AMOUNT));
        }
        if (jsonObject.containsKey(CommonHelper.ALCOHOL_READING_METHOD)) {
            jsonObjectBuilder.add("alcoholReadingMethodCode", jsonObject.getString(CommonHelper.ALCOHOL_READING_METHOD));
        }

        return jsonObjectBuilder.build();
    }

    public static JsonObject transformVerdict(final JsonObject jsonObject) {
        final JsonObjectBuilder jsonObjectBuilder = createObjectBuilder();
        //required
        jsonObjectBuilder
                .add(CommonHelper.ORIGINATING_HEARING_ID, jsonObject.getString(CommonHelper.ORIGINATING_HEARING_ID))
                .add(CommonHelper.OFFENCE_ID, jsonObject.getString(CommonHelper.OFFENCE_ID))
                .add(CommonHelper.VERDICT_DATE, jsonObject.getString(CommonHelper.VERDICT_DATE))
                .add(CommonHelper.VERDICT_TYPE, transformVerdictType(jsonObject.getJsonObject(CommonHelper.VERDICT_TYPE)));

        // add optional attribute
        if (jsonObject.containsKey(CommonHelper.JURORS)) {
            jsonObjectBuilder.add(CommonHelper.JURORS, jsonObject.getJsonObject(CommonHelper.JURORS));
        }
        if (jsonObject.containsKey(CommonHelper.LESSER_OR_ALTERNATIVE_OFFENCE)) {
            jsonObjectBuilder.add(CommonHelper.LESSER_OR_ALTERNATIVE_OFFENCE, jsonObject.getJsonObject(CommonHelper.LESSER_OR_ALTERNATIVE_OFFENCE));
        }
        return jsonObjectBuilder.build();
    }

    public static JsonObject transformVerdictType(final JsonObject jsonObject) {
        final JsonObjectBuilder jsonObjectBuilder = createObjectBuilder();
        //required
        jsonObjectBuilder
                .add(CommonHelper.CATEGORY, jsonObject.getString(CommonHelper.CATEGORY))
                .add(CommonHelper.CATEGORY_TYPE, jsonObject.getString(CommonHelper.CATEGORY_TYPE));

        if(jsonObject.containsKey(CommonHelper.VERDICT_TYPE_ID)) {
            jsonObjectBuilder.add(CommonHelper.ID, jsonObject.getString(CommonHelper.VERDICT_TYPE_ID));
        } else {
            jsonObjectBuilder.add(CommonHelper.ID, jsonObject.getString(CommonHelper.ID));
        }

        // add optional attribute
        if (jsonObject.containsKey(CommonHelper.SEQUENCE)) {
            jsonObjectBuilder.add(CommonHelper.SEQUENCE, jsonObject.getInt(CommonHelper.SEQUENCE));
        }
        if (jsonObject.containsKey(CommonHelper.DESCRIPTION)) {
            jsonObjectBuilder.add(CommonHelper.DESCRIPTION, jsonObject.getString(CommonHelper.DESCRIPTION));
        }
        return jsonObjectBuilder.build();
    }
}