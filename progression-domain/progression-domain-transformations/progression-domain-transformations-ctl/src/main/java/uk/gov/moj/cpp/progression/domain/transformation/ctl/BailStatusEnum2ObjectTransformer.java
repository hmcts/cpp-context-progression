package uk.gov.moj.cpp.progression.domain.transformation.ctl;

import static javax.json.Json.createObjectBuilder;

import java.util.Arrays;
import java.util.Deque;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.function.BiFunction;

import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.json.JsonValue;

public class BailStatusEnum2ObjectTransformer {

    public static final String BAIL_STATUS_PROPERTYNAME = "bailStatus";
    public static final String CONDITIONAL_STATUS = "CONDITIONAL";
    public static final String INCUSTODY_STATUS = "INCUSTODY";
    public static final String UNCONDITIONAL_STATUS = "UNCONDITIONAL";
    protected static final Set<String> ALL_STATII = new HashSet(Arrays.asList(CONDITIONAL_STATUS, INCUSTODY_STATUS, UNCONDITIONAL_STATUS)) ;

    public JsonObject transform(final JsonObject nowsRequested) {
        final BiFunction<JsonValue, Deque<String>, Object> filter = (jsonValue, path) -> {
            if (!path.isEmpty() && path.peek().equals(BAIL_STATUS_PROPERTYNAME) && !(jsonValue instanceof JsonObject)) {
                return bailStatusEnumToObject(jsonValue.toString().replace("\"", ""));
            } else {
                return jsonValue;
            }
        };

        final JsonObjectBuilder result = TransformUtil.cloneObjectWithPathFilter(nowsRequested, filter);
        return result.build();
    }

    /**
     *  80    R    Re-arrested after release on bail    7dc36d1c-a739-3792-8579-372b177d1268         4
     *  30    C    Custody or remanded into custody    12e69486-4d01-3403-a50a-7419ca040635         2
     *  60    P    Conditional Bail with Pre-Release conditions    34443c87-fa6f-34c0-897f-0cce45773df5        5
     * 50    L    Remanded into care of Local Authority    4dc146db-9d89-30bf-93b3-b22bc072d666         3
     *  70    S    Remanded into Secure Accommodation    549336f9-2a07-3767-960f-107da761a698         1
     *  40    A    Not applicable    86009c70-759d-3308-8de4-194886ff9a77         8
     *  10    B    Conditional Bail    dd4073b6-22be-3875-9d63-5da286bb3ece         6
     *  20    U    Unconditional Bail    eaf18bf8-9569-3656-a4ab-64299f9bd513         7
     * @param enumName
     * @return
     */

    final JsonObjectBuilder bailStatusEnumToObject(final String enumName) {
        UUID id;
        String description;
        String code;
        final String normalizedEnumName =  enumName.toUpperCase().replace("_", "").trim();

        switch (normalizedEnumName) {
            case CONDITIONAL_STATUS:
                id = UUID.fromString("d4073b6-22be-3875-9d63-5da286bb3ece");
                description = "Conditional Bail";
                code = "B";
                break;
            case  INCUSTODY_STATUS:
                id = UUID.fromString("2e69486-4d01-3403-a50a-7419ca040635");
                description = "Custody or remanded into custody";
                code = "C";
                break;
            case UNCONDITIONAL_STATUS:
                id = UUID.fromString("eaf18bf8-9569-3656-a4ab-64299f9bd513");
                description = "Unconditional Bail";
                code = "U";
                break;
            default:
                id = UUID.randomUUID();
                description = "Unknown bail status '" + normalizedEnumName + "' not one of " + ALL_STATII;
                code = "X";
        }
        final JsonObjectBuilder jsonObjectBuilder = createObjectBuilder();
        jsonObjectBuilder.add("id", id.toString());
        jsonObjectBuilder.add("code", code);
        jsonObjectBuilder.add("description", description);
        return jsonObjectBuilder;


    }

}
