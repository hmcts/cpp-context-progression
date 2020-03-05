package uk.gov.moj.cpp.progression.domain.transformation.ancillarydocs;

import static javax.json.Json.createObjectBuilder;
import static uk.gov.moj.cpp.progression.domain.transformation.ancillarydocs.core.SchemaVariableConstants.PROGRESSION_EVENT_HEARING_APPLICATION_LINK_CREATED;
import static uk.gov.moj.cpp.progression.domain.transformation.ancillarydocs.core.SchemaVariableConstants.PROGRESSION_EVENT_HEARING_RESULTED;
import static uk.gov.moj.cpp.progression.domain.transformation.ancillarydocs.core.SchemaVariableConstants.PROGRESSION_EVENT_NOWS_REQUESTED;
import static uk.gov.moj.cpp.progression.domain.transformation.ancillarydocs.core.SchemaVariableConstants.PROGRESSION_EVENT_PROSECUTIONCASE_DEFENDANT_LISTING_STATUS_CHANGED;

import uk.gov.moj.cpp.progression.domain.transformation.ancillarydocs.core.EventTransformationException;
import uk.gov.moj.cpp.progression.domain.transformation.ancillarydocs.core.SchemaVariableConstants;

import java.util.Collections;
import java.util.Deque;
import java.util.Map;
import java.util.Spliterators;
import java.util.function.BiFunction;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.json.JsonValue;

public class AttendanceDayEventTransformer {
    protected static final Map<String, Pattern> eventAndJsonPaths = Collections.unmodifiableMap(
            Stream.of(new String[][]{
                    {PROGRESSION_EVENT_HEARING_RESULTED, "hearing\\.defendantAttendance\\.\\d\\.attendanceDays\\.\\d"},
                    {PROGRESSION_EVENT_NOWS_REQUESTED, "createNowsRequest\\.hearing\\.defendantAttendance\\.\\d\\.attendanceDays\\.\\d"},
                    {PROGRESSION_EVENT_HEARING_APPLICATION_LINK_CREATED, "hearing\\.defendantAttendance\\.\\d\\.attendanceDays\\.\\d"},
                    {PROGRESSION_EVENT_PROSECUTIONCASE_DEFENDANT_LISTING_STATUS_CHANGED, "hearing\\.defendantAttendance\\.\\d\\.attendanceDays\\.\\d"}
            }).collect(Collectors.toMap(data -> data[0], data -> Pattern.compile(data[1]))));

    public JsonObject transform(final String name, final JsonObject payload) {
        final JsonObjectBuilder transformedPayloadObjectBuilder;
        Pattern jsonPathPattern = eventAndJsonPaths.get(name);

        final BiFunction<JsonValue, Deque<String>, Object> filter = (jsonValue, path) -> {
            if (!path.isEmpty() && match(jsonPathPattern, path) && (jsonValue instanceof JsonObject)) {
                return attendanceDayTransform((JsonObject) jsonValue);
            } else {
                return jsonValue;
            }
        };

        transformedPayloadObjectBuilder = TransformUtil.cloneObjectWithPathFilter(payload, filter);

        return transformedPayloadObjectBuilder.build();
    }

    public boolean match(final Pattern jsonPathPattern, final Deque<String> path) {
        String pathMerged = StreamSupport
                .stream(Spliterators.spliteratorUnknownSize(path.descendingIterator(), 0),
                        false)
                .collect(Collectors.joining("."));
        return jsonPathPattern.matcher(String.join(".", pathMerged)).matches();
    }


    private Object attendanceDayTransform(final JsonObject replace) {
        final JsonObjectBuilder jsonObjectBuilder = createObjectBuilder();
        jsonObjectBuilder.add(SchemaVariableConstants.FIELD_DAY, replace.get(SchemaVariableConstants.FIELD_DAY));

        if (replace.getBoolean(SchemaVariableConstants.FIELD_IS_IN_ATTENDANCE) == Boolean.TRUE) {
            jsonObjectBuilder.add(SchemaVariableConstants.FIELD_ATTENDANCE_TYPE, SchemaVariableConstants.FIELD_IN_PERSON);
        } else if (replace.getBoolean(SchemaVariableConstants.FIELD_IS_IN_ATTENDANCE) == Boolean.FALSE) {
            jsonObjectBuilder.add(SchemaVariableConstants.FIELD_ATTENDANCE_TYPE, SchemaVariableConstants.FIELD_NOT_PRESENT);
        } else {
            throw new EventTransformationException(String.format("unexpected property value for defendantAttendance %s ", replace.toString()));
        }

        return jsonObjectBuilder;
    }

}
