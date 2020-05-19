package uk.gov.moj.cpp.progression.domain.transformation.corechanges.transform;

import static javax.json.Json.createObjectBuilder;
import static org.slf4j.LoggerFactory.getLogger;
import static uk.gov.moj.cpp.progression.domain.transformation.corechanges.core.SchemaVariableConstants.FIELD_ID;
import static uk.gov.moj.cpp.progression.domain.transformation.corechanges.core.SchemaVariableConstants.FIELD_MASTER_DEFENDANT_ID;
import static uk.gov.moj.cpp.progression.domain.transformation.corechanges.core.SchemaVariableConstants.PROGRESSION_APPLICATION_REFERRED_TO_COURT;
import static uk.gov.moj.cpp.progression.domain.transformation.corechanges.core.SchemaVariableConstants.PROGRESSION_BOXWORK_APPLICATION_REFERRED;
import static uk.gov.moj.cpp.progression.domain.transformation.corechanges.core.SchemaVariableConstants.PROGRESSION_COURT_APPLICATION_ADDED_TO_CASE;
import static uk.gov.moj.cpp.progression.domain.transformation.corechanges.core.SchemaVariableConstants.PROGRESSION_COURT_APPLICATION_CREATED;
import static uk.gov.moj.cpp.progression.domain.transformation.corechanges.core.SchemaVariableConstants.PROGRESSION_COURT_APPLICATION_UPDATED;
import static uk.gov.moj.cpp.progression.domain.transformation.corechanges.core.SchemaVariableConstants.PROGRESSION_COURT_PROCEEDINGS_INITIATED;
import static uk.gov.moj.cpp.progression.domain.transformation.corechanges.core.SchemaVariableConstants.PROGRESSION_HEARING_APPLICATION_LINK_CREATED;
import static uk.gov.moj.cpp.progression.domain.transformation.corechanges.core.SchemaVariableConstants.PROGRESSION_HEARING_EXTENDED;
import static uk.gov.moj.cpp.progression.domain.transformation.corechanges.core.SchemaVariableConstants.PROGRESSION_HEARING_INITIATE_ENRICHED;
import static uk.gov.moj.cpp.progression.domain.transformation.corechanges.core.SchemaVariableConstants.PROGRESSION_HEARING_RESULTED;
import static uk.gov.moj.cpp.progression.domain.transformation.corechanges.core.SchemaVariableConstants.PROGRESSION_HEARING_RESULTED_CASE_UPDATED;
import static uk.gov.moj.cpp.progression.domain.transformation.corechanges.core.SchemaVariableConstants.PROGRESSION_LISTED_COURT_APPLICATION_CHANGED;
import static uk.gov.moj.cpp.progression.domain.transformation.corechanges.core.SchemaVariableConstants.PROGRESSION_NOWS_REQUESTED;
import static uk.gov.moj.cpp.progression.domain.transformation.corechanges.core.SchemaVariableConstants.PROGRESSION_PROSECUTIONCASE_DEFENDANT_LISTING_STATUS_CHANGED;
import static uk.gov.moj.cpp.progression.domain.transformation.corechanges.core.SchemaVariableConstants.PROGRESSION_PROSECUTION_CASE_CREATED;
import static uk.gov.moj.cpp.progression.domain.transformation.corechanges.core.SchemaVariableConstants.PROGRESSION_PROSECUTION_CASE_DEFENDANT_UPDATED;
import static uk.gov.moj.cpp.progression.domain.transformation.corechanges.core.SchemaVariableConstants.PROGRESSION_REFERRED_TO_COURT;
import static uk.gov.moj.cpp.progression.domain.transformation.corechanges.core.SchemaVariableConstants.PROGRESSION_SENDING_SHEET_COMPLETED;

import uk.gov.justice.services.messaging.Metadata;
import uk.gov.moj.cpp.progression.domain.transformation.corechanges.TransformUtil;

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

import org.slf4j.Logger;

public class MasterDefendantIdEventTransformer implements ProgressionEventTransformer {

    private static final Map<String, Pattern> eventAndJsonPaths = Collections.unmodifiableMap(
            Stream.of(new String[][]{
                    {PROGRESSION_APPLICATION_REFERRED_TO_COURT, "hearingRequest\\.prosecutionCases\\.\\d\\.defendants\\.\\d|hearingRequest\\.courtApplications\\.\\d\\.applicant\\.defendant|hearingRequest\\.courtApplications\\.\\d\\.respondents\\.\\d\\.partyDetails\\.defendant"},
                    {PROGRESSION_BOXWORK_APPLICATION_REFERRED, "hearingRequest\\.prosecutionCases\\.\\d\\.defendants\\.\\d|hearingRequest\\.courtApplications\\.\\d\\.applicant\\.defendant|hearingRequest\\.courtApplications\\.\\d\\.respondents\\.\\d\\.partyDetails\\.defendant"},
                    {PROGRESSION_REFERRED_TO_COURT, "courtReferral\\.prosecutionCases\\.\\d\\.defendants\\.\\d"},
                    {PROGRESSION_COURT_APPLICATION_CREATED, "courtApplication\\.applicant\\.defendant|courtApplication\\.respondents\\.\\d\\.partyDetails\\.defendant"},
                    {PROGRESSION_COURT_APPLICATION_ADDED_TO_CASE, "courtApplication\\.applicant\\.defendant|courtApplication\\.respondents\\.\\d\\.partyDetails\\.defendant"},
                    {PROGRESSION_COURT_APPLICATION_UPDATED, "courtApplication\\.applicant\\.defendant|courtApplication\\.respondents\\.\\d\\.partyDetails\\.defendant"},
                    {PROGRESSION_SENDING_SHEET_COMPLETED, "hearing\\.defendants\\.\\d"},
                    {PROGRESSION_HEARING_INITIATE_ENRICHED, "hearing\\.prosecutionCases\\.\\d\\.defendants\\.\\d"},
                    {PROGRESSION_HEARING_APPLICATION_LINK_CREATED, "hearing\\.prosecutionCases\\.\\d\\.defendants\\.\\d|hearing\\.courtApplications\\.\\d\\.applicant\\.defendant|hearing\\.courtApplications\\.\\d\\.respondents\\.\\d\\.partyDetails\\.defendant"},
                    {PROGRESSION_HEARING_EXTENDED, "hearingRequest\\.prosecutionCases\\.\\d\\.defendants\\.\\d|hearingRequest\\.courtApplications\\.\\d\\.applicant\\.defendant|hearingRequest\\.courtApplications\\.\\d\\.respondents\\.\\d\\.partyDetails\\.defendant"},
                    {PROGRESSION_HEARING_RESULTED, "hearing\\.prosecutionCases\\.\\d\\.defendants\\.\\d|hearing\\.courtApplications\\.\\d\\.applicant\\.defendant|hearing\\.courtApplications\\.\\d\\.respondents\\.\\d\\.partyDetails\\.defendant"},
                    {PROGRESSION_HEARING_RESULTED_CASE_UPDATED, "prosecutionCase\\.defendants\\.\\d"},
                    {PROGRESSION_NOWS_REQUESTED, "createNowsRequest\\.hearing\\.defenceCounsels\\.\\d\\.defendants\\.\\d|createNowsRequest\\.hearing\\.defenceCounsels\\.\\d\\.defendant|createNowsRequest\\.hearing\\.prosecutionCases\\.\\d\\.defendants\\.\\d"},
                    {PROGRESSION_PROSECUTION_CASE_CREATED, "prosecutionCase\\.defendants\\.\\d"},
                    {PROGRESSION_PROSECUTIONCASE_DEFENDANT_LISTING_STATUS_CHANGED, "hearing\\.prosecutionCases\\.\\d\\.defendants\\.\\d"},
                    {PROGRESSION_PROSECUTION_CASE_DEFENDANT_UPDATED, "defendant"},
                    {PROGRESSION_COURT_PROCEEDINGS_INITIATED, "courtReferral\\.prosecutionCases\\.\\d\\.defendants\\.\\d"},
                    {PROGRESSION_LISTED_COURT_APPLICATION_CHANGED, "courtApplication\\.applicant\\.defendant|courtApplication.respondents\\.\\d\\.partyDetails\\.defendant"},
            }).collect(Collectors.toMap(data -> data[0], data -> Pattern.compile(data[1]))));


    private static final Logger LOGGER = getLogger(MasterDefendantIdEventTransformer.class);

    public static Map<String, Pattern> getEventAndJsonPaths() {

        return eventAndJsonPaths;
    }

    @Override
    public JsonObject transform(final Metadata eventMetadata, final JsonObject payload) {
        final JsonObjectBuilder transformedPayloadObjectBuilder;
        final Pattern jsonPath = eventAndJsonPaths.get(eventMetadata.name());

        final BiFunction<JsonValue, Deque<String>, Object> filter = (jsonValue, path) -> {
            if (!path.isEmpty() && match(jsonPath, path) && (jsonValue instanceof JsonObject)) {
                return defendantTransform((JsonObject) jsonValue);
            } else {
                return jsonValue;
            }
        };

        transformedPayloadObjectBuilder = TransformUtil.cloneObjectWithPathFilter(payload, filter);

        return transformedPayloadObjectBuilder.build();
    }

    public boolean match(final Pattern jsonPath, final Deque<String> path) {
        final String pathMerged = StreamSupport
                .stream(Spliterators.spliteratorUnknownSize(path.descendingIterator(), 0),
                        false)
                .collect(Collectors.joining("."));
        return jsonPath.matcher(String.join(".", pathMerged)).matches();
    }


    private Object defendantTransform(final JsonObject jsonObject) {
        final JsonObjectBuilder result = createObjectBuilder();
        for (final Map.Entry<String, JsonValue> property : jsonObject.entrySet()) {
            final String key = property.getKey();
            final JsonValue value = property.getValue();
            result.add(key, value);
            if (key.equalsIgnoreCase(FIELD_ID)) {
                if (jsonObject.containsKey(FIELD_MASTER_DEFENDANT_ID)) {
                    LOGGER.warn("Defendant {} already have {} with value {} ", value, FIELD_MASTER_DEFENDANT_ID, jsonObject.getString(FIELD_MASTER_DEFENDANT_ID));
                } else {
                    result.add(FIELD_MASTER_DEFENDANT_ID, value);
                }
            }
        }
        return result;
    }

}
