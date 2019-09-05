package uk.gov.moj.cpp.progression.domain.transformation.util;

import static javax.json.Json.createArrayBuilder;
import static javax.json.Json.createObjectBuilder;
import static uk.gov.moj.cpp.progression.domain.transformation.util.CommonHelper.ALLOCATION_DECISION;
import static uk.gov.moj.cpp.progression.domain.transformation.util.CommonHelper.ALLOCATION_DECISION_DATE;
import static uk.gov.moj.cpp.progression.domain.transformation.util.CommonHelper.AQUITTAL_DATE;
import static uk.gov.moj.cpp.progression.domain.transformation.util.CommonHelper.ARREST_DATE;
import static uk.gov.moj.cpp.progression.domain.transformation.util.CommonHelper.CHARGE_DATE;
import static uk.gov.moj.cpp.progression.domain.transformation.util.CommonHelper.DATE_OF_INFORMATION;
import static uk.gov.moj.cpp.progression.domain.transformation.util.CommonHelper.DEFENDANT_ID;
import static uk.gov.moj.cpp.progression.domain.transformation.util.CommonHelper.EITHER_WAY;
import static uk.gov.moj.cpp.progression.domain.transformation.util.CommonHelper.END_DATE;
import static uk.gov.moj.cpp.progression.domain.transformation.util.CommonHelper.ID;
import static uk.gov.moj.cpp.progression.domain.transformation.util.CommonHelper.INDICATED_PLEA;
import static uk.gov.moj.cpp.progression.domain.transformation.util.CommonHelper.INDICATED_PLEA_DATE;
import static uk.gov.moj.cpp.progression.domain.transformation.util.CommonHelper.INDICATED_PLEA_VALUE;
import static uk.gov.moj.cpp.progression.domain.transformation.util.CommonHelper.INDICTABLE;
import static uk.gov.moj.cpp.progression.domain.transformation.util.CommonHelper.INDICTABLE_ONLY_OFFENCE;
import static uk.gov.moj.cpp.progression.domain.transformation.util.CommonHelper.JUDICIAL_RESULTS;
import static uk.gov.moj.cpp.progression.domain.transformation.util.CommonHelper.MODE_OF_TRIAL;
import static uk.gov.moj.cpp.progression.domain.transformation.util.CommonHelper.MOT_REASON_CODE;
import static uk.gov.moj.cpp.progression.domain.transformation.util.CommonHelper.MOT_REASON_DESCRIPTION;
import static uk.gov.moj.cpp.progression.domain.transformation.util.CommonHelper.MOT_REASON_ID;
import static uk.gov.moj.cpp.progression.domain.transformation.util.CommonHelper.MOT_REASON_ID_1;
import static uk.gov.moj.cpp.progression.domain.transformation.util.CommonHelper.MOT_REASON_ID_2;
import static uk.gov.moj.cpp.progression.domain.transformation.util.CommonHelper.MOT_REASON_ID_3;
import static uk.gov.moj.cpp.progression.domain.transformation.util.CommonHelper.NOTIFIED_PLEA;
import static uk.gov.moj.cpp.progression.domain.transformation.util.CommonHelper.NO_MODE_OF_TRIAL_EITHER_WAY_OFFENCE;
import static uk.gov.moj.cpp.progression.domain.transformation.util.CommonHelper.OFFENCES;
import static uk.gov.moj.cpp.progression.domain.transformation.util.CommonHelper.OFFENCE_CODE;
import static uk.gov.moj.cpp.progression.domain.transformation.util.CommonHelper.OFFENCE_DEFINITION_ID;
import static uk.gov.moj.cpp.progression.domain.transformation.util.CommonHelper.OFFENCE_FACTS;
import static uk.gov.moj.cpp.progression.domain.transformation.util.CommonHelper.OFFENCE_ID;
import static uk.gov.moj.cpp.progression.domain.transformation.util.CommonHelper.OFFENCE_LEGISLATION;
import static uk.gov.moj.cpp.progression.domain.transformation.util.CommonHelper.OFFENCE_LEGISLATION_WELSH;
import static uk.gov.moj.cpp.progression.domain.transformation.util.CommonHelper.OFFENCE_TITLE;
import static uk.gov.moj.cpp.progression.domain.transformation.util.CommonHelper.OFFENCE_TITLE_WELSH;
import static uk.gov.moj.cpp.progression.domain.transformation.util.CommonHelper.ORDER_INDEX;
import static uk.gov.moj.cpp.progression.domain.transformation.util.CommonHelper.ORIGINATING_HEARING_ID;
import static uk.gov.moj.cpp.progression.domain.transformation.util.CommonHelper.PLEA;
import static uk.gov.moj.cpp.progression.domain.transformation.util.CommonHelper.PROSECUTION_CASE_ID;
import static uk.gov.moj.cpp.progression.domain.transformation.util.CommonHelper.SEQUENCE_NUMBER;
import static uk.gov.moj.cpp.progression.domain.transformation.util.CommonHelper.SOURCE;
import static uk.gov.moj.cpp.progression.domain.transformation.util.CommonHelper.START_DATE;
import static uk.gov.moj.cpp.progression.domain.transformation.util.CommonHelper.SUMMARY;
import static uk.gov.moj.cpp.progression.domain.transformation.util.CommonHelper.SUMMARY_ONLY_OFFENCE;
import static uk.gov.moj.cpp.progression.domain.transformation.util.CommonHelper.VERDICT;
import static uk.gov.moj.cpp.progression.domain.transformation.util.CommonHelper.VICTIMS;
import static uk.gov.moj.cpp.progression.domain.transformation.util.CommonHelper.WORDING_WELSH;

import java.util.HashMap;
import java.util.Map;

import javax.json.JsonArray;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;

@SuppressWarnings({"squid:S1188", "squid:S3776"})
public class OffenceHelper {

    private OffenceHelper() {
    }

    public static JsonArray transformOffences(final JsonArray offenceJsonObjects,
                                              final JsonObject hearing) {
        final JsonArrayBuilder offenceList = createArrayBuilder();
        offenceJsonObjects.forEach(o -> {
            final JsonObject offence = (JsonObject) o;

            //add required fields,
            final JsonObjectBuilder offenceBuilder = createObjectBuilder()
                    .add(ID, offence.getString(ID))
                    .add(OFFENCE_DEFINITION_ID, offence.getString(OFFENCE_DEFINITION_ID))
                    .add(OFFENCE_CODE, offence.getString(OFFENCE_CODE))
                    .add(OFFENCE_TITLE, offence.getString(OFFENCE_TITLE))
                    .add(CommonHelper.WORDING, offence.getString(CommonHelper.WORDING))
                    .add(START_DATE, offence.getString(START_DATE));

            // add optional fields
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
                offenceBuilder.add(MODE_OF_TRIAL, derivedModeOfTrial(offence.getString(MODE_OF_TRIAL)));
            }

            if (offence.containsKey(WORDING_WELSH)) {
                offenceBuilder.add(WORDING_WELSH, offence.getString(WORDING_WELSH));
            }

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

            if (offence.containsKey(DATE_OF_INFORMATION)) {
                offenceBuilder.add(DATE_OF_INFORMATION, offence.getString(DATE_OF_INFORMATION));
            }

            if (offence.containsKey(CommonHelper.COUNT)) {
                offenceBuilder.add(CommonHelper.COUNT, offence.getInt(CommonHelper.COUNT));
            }

            if (offence.containsKey(CommonHelper.CONVICTION_DATE)) {
                offenceBuilder.add(CommonHelper.CONVICTION_DATE, offence.getString(CommonHelper.CONVICTION_DATE));
            }

            if (offence.containsKey(NOTIFIED_PLEA)) {
                offenceBuilder.add(NOTIFIED_PLEA, offence.getJsonObject(NOTIFIED_PLEA));
            }

            if (offence.containsKey(VERDICT)) {
                offenceBuilder.add(VERDICT, offence.getJsonObject(VERDICT));
            }

            if (offence.containsKey(OFFENCE_FACTS)) {
                offenceBuilder.add(OFFENCE_FACTS, offence.getJsonObject(OFFENCE_FACTS));
            }

            if (offence.containsKey(AQUITTAL_DATE)) {
                offenceBuilder.add(AQUITTAL_DATE, offence.getString(AQUITTAL_DATE));
            }

            if (offence.containsKey(JUDICIAL_RESULTS)) {
                offenceBuilder.add(JUDICIAL_RESULTS, offence.getJsonArray(JUDICIAL_RESULTS));
            }

            if (offence.containsKey(VICTIMS)) {
                offenceBuilder.add(VICTIMS, offence.getJsonArray(VICTIMS));
            }

            if (offence.containsKey(PLEA)) {
                offenceBuilder.add(PLEA, offence.getJsonObject(PLEA));
                if (offence.containsKey(MODE_OF_TRIAL)) {
                    if ("IND".equalsIgnoreCase(offence.getString(MODE_OF_TRIAL))) {
                        offenceBuilder.add(ALLOCATION_DECISION, transformAllocationDecision(offence.getJsonObject(PLEA), hearing.getString(ID), MOT_REASON_ID_1, 20, "2", INDICTABLE_ONLY_OFFENCE));
                    } else if ("EWAY".equalsIgnoreCase(offence.getString(MODE_OF_TRIAL))) {
                        offenceBuilder.add(ALLOCATION_DECISION, transformAllocationDecision(offence.getJsonObject(PLEA), hearing.getString(ID), MOT_REASON_ID_2, 70, "7", NO_MODE_OF_TRIAL_EITHER_WAY_OFFENCE));
                    } else {
                        offenceBuilder.add(ALLOCATION_DECISION, transformAllocationDecision(offence.getJsonObject(PLEA), hearing.getString(ID), MOT_REASON_ID_3, 10, "1", SUMMARY_ONLY_OFFENCE));
                    }
                } else {
                    offenceBuilder.add(ALLOCATION_DECISION, transformAllocationDecision(offence.getJsonObject(PLEA), hearing.getString(ID), MOT_REASON_ID_3, 10, "1", SUMMARY_ONLY_OFFENCE));
                }
            }

            if (offence.containsKey(INDICATED_PLEA)) {
                offenceBuilder.add(INDICATED_PLEA, transformIndicatedPlea(offence.getJsonObject(INDICATED_PLEA)));
                if (offence.getJsonObject(INDICATED_PLEA).containsKey(ALLOCATION_DECISION)) {
                    offenceBuilder.add(ALLOCATION_DECISION, transformAllocationDecision(offence.getJsonObject(INDICATED_PLEA), hearing.getString(ID)));
                }
            }

            offenceList.add(offenceBuilder.build());
        });
        return offenceList.build();

    }

    private static String derivedModeOfTrial(final String modeOfTrial) {
        return getDerivedModOfTrial().getOrDefault(modeOfTrial.toUpperCase(), SUMMARY);
    }

    private static Map<String, String> getDerivedModOfTrial() {
        final Map<String, String> derivedModeOfTrial = new HashMap<>();
        derivedModeOfTrial.put("EWAY", EITHER_WAY);
        derivedModeOfTrial.put("IND", INDICTABLE);
        derivedModeOfTrial.put("SIMP", SUMMARY);
        derivedModeOfTrial.put("STRAFF", SUMMARY);
        derivedModeOfTrial.put("SNONIMP", SUMMARY);
        derivedModeOfTrial.put("CIVIL", SUMMARY);
        return derivedModeOfTrial;
    }


    private static JsonObject transformAllocationDecision(final JsonObject jsonObject, final String hearingId) {
        final JsonObjectBuilder jsonObjectBuilder = createObjectBuilder();
        if (jsonObject.containsKey(ORIGINATING_HEARING_ID)) {
            jsonObjectBuilder.add(ORIGINATING_HEARING_ID, jsonObject.getString(ORIGINATING_HEARING_ID));
        } else {
            jsonObjectBuilder.add(ORIGINATING_HEARING_ID, hearingId);
        }
        //required
        jsonObjectBuilder
                .add(ALLOCATION_DECISION_DATE, jsonObject.getString(INDICATED_PLEA_DATE))
                .add(MOT_REASON_ID, "f8eb278a-8bce-373e-b365-b45e939da38a")
                .add(MOT_REASON_DESCRIPTION, "Defendant chooses trial by jury")
                .add(MOT_REASON_CODE, "4")
                .add(SEQUENCE_NUMBER, 40);
        // add optional attributen
        if (jsonObject.getJsonObject(ALLOCATION_DECISION).containsKey("indicationOfSentence")) {
            jsonObjectBuilder.add("courtIndicatedSentence", createObjectBuilder()
                    .add("courtIndicatedSentenceTypeId", "d3d94468-02a4-3259-b55d-38e6d163e820")
                    .add("courtIndicatedSentenceDescription", jsonObject.getJsonObject(ALLOCATION_DECISION).getString("indicationOfSentence"))
                    .build());
        }
        return jsonObjectBuilder.build();
    }


    private static JsonObject transformAllocationDecision(final JsonObject jsonObject, final String hearingId, final String motReasonId, final int sequenceNumber, final String motReasonCode, final String motReasonDescription) {
        final JsonObjectBuilder jsonObjectBuilder = createObjectBuilder();
        if (jsonObject.containsKey(ORIGINATING_HEARING_ID)) {
            jsonObjectBuilder.add(ORIGINATING_HEARING_ID, jsonObject.getString(ORIGINATING_HEARING_ID));
        } else {
            jsonObjectBuilder.add(ORIGINATING_HEARING_ID, hearingId);
        }
        //required
        jsonObjectBuilder
                .add(ALLOCATION_DECISION_DATE, jsonObject.getString("pleaDate"))
                .add(MOT_REASON_ID, motReasonId)
                .add(MOT_REASON_DESCRIPTION, motReasonDescription)
                .add(MOT_REASON_CODE, motReasonCode)
                .add(SEQUENCE_NUMBER, sequenceNumber);

        return jsonObjectBuilder.build();
    }

    public static JsonObject transformIndicatedPlea(final JsonObject jsonObject) {
        final JsonObjectBuilder jsonObjectBuilder = createObjectBuilder();
        //required
        jsonObjectBuilder
                .add(OFFENCE_ID, jsonObject.getString(OFFENCE_ID))
                .add(INDICATED_PLEA_DATE, jsonObject.getString(INDICATED_PLEA_DATE))
                .add(INDICATED_PLEA_VALUE, jsonObject.getString(INDICATED_PLEA_VALUE))
                .add(SOURCE, jsonObject.getString(SOURCE));
        // add optional attributen
        if (jsonObject.containsKey(ORIGINATING_HEARING_ID)) {
            jsonObjectBuilder.add(ORIGINATING_HEARING_ID, jsonObject.getString(ORIGINATING_HEARING_ID));
        }
        return jsonObjectBuilder.build();
    }

    @SuppressWarnings({"squid:S1066","squid:S134"})
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
                    .add(CommonHelper.WORDING, offence.getString(CommonHelper.WORDING))
                    .add(START_DATE, offence.getString(START_DATE));

            // add optional fields
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
                offenceBuilder.add(MODE_OF_TRIAL, derivedModeOfTrial(offence.getString(MODE_OF_TRIAL)));
            }

            if (offence.containsKey(WORDING_WELSH)) {
                offenceBuilder.add(WORDING_WELSH, offence.getString(WORDING_WELSH));
            }

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

            if (offence.containsKey(DATE_OF_INFORMATION)) {
                offenceBuilder.add(DATE_OF_INFORMATION, offence.getString(DATE_OF_INFORMATION));
            }

            if (offence.containsKey(CommonHelper.COUNT)) {
                offenceBuilder.add(CommonHelper.COUNT, offence.getInt(CommonHelper.COUNT));
            }

            if (offence.containsKey(CommonHelper.CONVICTION_DATE)) {
                offenceBuilder.add(CommonHelper.CONVICTION_DATE, offence.getString(CommonHelper.CONVICTION_DATE));
            }

            if (offence.containsKey(NOTIFIED_PLEA)) {
                offenceBuilder.add(NOTIFIED_PLEA, offence.getJsonObject(NOTIFIED_PLEA));
            }

            if (offence.containsKey(VERDICT)) {
                offenceBuilder.add(VERDICT, offence.getJsonObject(VERDICT));
            }

            if (offence.containsKey(OFFENCE_FACTS)) {
                offenceBuilder.add(OFFENCE_FACTS, offence.getJsonObject(OFFENCE_FACTS));
            }

            if (offence.containsKey(AQUITTAL_DATE)) {
                offenceBuilder.add(AQUITTAL_DATE, offence.getString(AQUITTAL_DATE));
            }

            if (offence.containsKey(JUDICIAL_RESULTS)) {
                offenceBuilder.add(JUDICIAL_RESULTS, offence.getJsonArray(JUDICIAL_RESULTS));
            }

            if (offence.containsKey(VICTIMS)) {
                offenceBuilder.add(VICTIMS, offence.getJsonArray(VICTIMS));
            }

            if (offence.containsKey(PLEA)) {
                offenceBuilder.add(PLEA, offence.getJsonObject(PLEA));
                if(offence.getJsonObject(PLEA).containsKey(ORIGINATING_HEARING_ID)){
                    if (offence.containsKey(MODE_OF_TRIAL)) {
                        if ("IND".equalsIgnoreCase(offence.getString(MODE_OF_TRIAL))) {
                            offenceBuilder.add(ALLOCATION_DECISION, transformAllocationDecision(offence.getJsonObject(PLEA), offence.getJsonObject(PLEA).getString(ORIGINATING_HEARING_ID), MOT_REASON_ID_1, 20, "2", INDICTABLE_ONLY_OFFENCE));
                        } else if ("EWAY".equalsIgnoreCase(offence.getString(MODE_OF_TRIAL))) {
                            offenceBuilder.add(ALLOCATION_DECISION, transformAllocationDecision(offence.getJsonObject(PLEA), offence.getJsonObject(PLEA).getString(ORIGINATING_HEARING_ID), MOT_REASON_ID_2, 70, "7", NO_MODE_OF_TRIAL_EITHER_WAY_OFFENCE));
                        }else {
                            offenceBuilder.add(ALLOCATION_DECISION, transformAllocationDecision(offence.getJsonObject(PLEA), offence.getJsonObject(PLEA).getString(ORIGINATING_HEARING_ID), MOT_REASON_ID_3, 10, "1", SUMMARY_ONLY_OFFENCE));
                        }
                    }
                }
            }


            if (offence.containsKey(INDICATED_PLEA)) {
                offenceBuilder.add(INDICATED_PLEA, transformIndicatedPlea(offence.getJsonObject(INDICATED_PLEA)));
                if (offence.getJsonObject(INDICATED_PLEA).containsKey(ALLOCATION_DECISION) && offence.getJsonObject(INDICATED_PLEA).containsKey(ORIGINATING_HEARING_ID)) {
                    offenceBuilder.add(ALLOCATION_DECISION, transformAllocationDecision(offence.getJsonObject(INDICATED_PLEA), offence.getJsonObject(INDICATED_PLEA).getString(ORIGINATING_HEARING_ID)));
                }
            }

            offenceList.add(offenceBuilder.build());
        });
        return offenceList.build();

    }

    @SuppressWarnings({"squid:S1066","squid:S134"})
    public static JsonArray transformOffencesforDefendents(final JsonArray offenceJsonObjects) {
        final JsonArrayBuilder offenceList = createArrayBuilder();
        offenceJsonObjects.forEach(o -> {
            final JsonObject offence = (JsonObject) o;

            //add required fields,
            final JsonObjectBuilder offenceBuilder = createObjectBuilder()
                    .add(CommonHelper.ID, offence.getString(CommonHelper.ID))
                    .add(CommonHelper.OFFENCE_CODE, offence.getString(CommonHelper.OFFENCE_CODE))
                    .add(CommonHelper.START_DATE, offence.getString(CommonHelper.START_DATE));

            // add optional fields
            if (offence.containsKey(CommonHelper.END_DATE)) {
                offenceBuilder.add(CommonHelper.END_DATE, offence.getString(CommonHelper.END_DATE));
            }
            if (offence.containsKey(PLEA)) {
                offenceBuilder.add(PLEA, offence.getJsonObject(PLEA));
                if(offence.getJsonObject(PLEA).containsKey(ORIGINATING_HEARING_ID)){
                    if (offence.containsKey(MODE_OF_TRIAL)) {
                        if ("IND".equalsIgnoreCase(offence.getString(MODE_OF_TRIAL))) {
                            offenceBuilder.add(ALLOCATION_DECISION, transformAllocationDecision(offence.getJsonObject(PLEA), offence.getJsonObject(PLEA).getString(ORIGINATING_HEARING_ID), MOT_REASON_ID_1, 20, "2", INDICTABLE_ONLY_OFFENCE));
                        } else if ("EWAY".equalsIgnoreCase(offence.getString(MODE_OF_TRIAL))) {
                            offenceBuilder.add(ALLOCATION_DECISION, transformAllocationDecision(offence.getJsonObject(PLEA), offence.getJsonObject(PLEA).getString(ORIGINATING_HEARING_ID), MOT_REASON_ID_2, 70, "7", NO_MODE_OF_TRIAL_EITHER_WAY_OFFENCE));
                        }else {
                            offenceBuilder.add(ALLOCATION_DECISION, transformAllocationDecision(offence.getJsonObject(PLEA), offence.getJsonObject(PLEA).getString(ORIGINATING_HEARING_ID), MOT_REASON_ID_3, 10, "1", SUMMARY_ONLY_OFFENCE));
                        }
                    }
                }
            }


            if (offence.containsKey(INDICATED_PLEA)) {
                offenceBuilder.add(INDICATED_PLEA, transformIndicatedPlea(offence.getJsonObject(INDICATED_PLEA)));
                if (offence.getJsonObject(INDICATED_PLEA).containsKey(ALLOCATION_DECISION) && offence.getJsonObject(INDICATED_PLEA).containsKey(ORIGINATING_HEARING_ID)) {
                    offenceBuilder.add(ALLOCATION_DECISION, transformAllocationDecision(offence.getJsonObject(INDICATED_PLEA), offence.getJsonObject(INDICATED_PLEA).getString(ORIGINATING_HEARING_ID)));
                }
            }
            if (offence.containsKey(CommonHelper.ORDER_INDEX)) {
                offenceBuilder.add(CommonHelper.ORDER_INDEX, offence.getInt(CommonHelper.ORDER_INDEX));
            }
            if (offence.containsKey(CommonHelper.WORDING)) {
                offenceBuilder.add(CommonHelper.WORDING, offence.getString(CommonHelper.WORDING));
            }
            if (offence.containsKey(CommonHelper.SECTION)) {
                offenceBuilder.add(CommonHelper.SECTION, offence.getString(CommonHelper.SECTION));
            }

            if (offence.containsKey(CommonHelper.COUNT)) {
                offenceBuilder.add(CommonHelper.COUNT, offence.getString(CommonHelper.COUNT));
            }
            if (offence.containsKey(CommonHelper.CONVICTION_DATE)) {
                offenceBuilder.add(CommonHelper.CONVICTION_DATE, offence.getString(CommonHelper.CONVICTION_DATE));
            }

            offenceList.add(offenceBuilder.build());
        });
        return offenceList.build();

    }


    public static JsonArray transformOffencesForDefendantCaseOffences(final JsonArray offenceJsonObjects) {
        final JsonArrayBuilder offenceList = createArrayBuilder();
        offenceJsonObjects.forEach(o -> {
            final JsonObject offence = (JsonObject) o;

            //add required fields,
            final JsonObjectBuilder offenceBuilder = createObjectBuilder()
                    .add(DEFENDANT_ID, offence.getString(DEFENDANT_ID))
                    .add(PROSECUTION_CASE_ID, offence.getString(PROSECUTION_CASE_ID))
                    .add(OFFENCES, transformOffences(offence.getJsonArray(OFFENCES)));


            offenceList.add(offenceBuilder.build());
        });
        return offenceList.build();

    }

}