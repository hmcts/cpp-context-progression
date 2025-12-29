package uk.gov.moj.cpp.progression.query.view.service.transformer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.gov.justice.services.common.converter.StringToJsonObjectConverter;

import javax.inject.Inject;
import uk.gov.justice.services.messaging.JsonObjects;
import javax.json.JsonArray;
import javax.json.JsonObject;
import java.util.HashMap;
import java.util.Map;
import java.util.LinkedHashMap;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static java.util.Objects.nonNull;
import static java.util.Optional.ofNullable;

@SuppressWarnings("squid:S1612 ")
public class AssigneeTransformer {

    private static final Logger LOGGER = LoggerFactory.getLogger(AssigneeTransformer.class);

    public static final String FORM_DATA = "formData";

    public static final String DATA = "data";

    public static final String ASSIGNEES = "defendants";

    @Inject
    private StringToJsonObjectConverter stringToJsonObjectConverter;

    public Map<UUID, String> transform(final JsonObject payload) {
        final Map<UUID, String> assignees = new HashMap<>();


        if(nonNull(payload.getString(FORM_DATA, null))) {
            final JsonObject jsonObject = stringToJsonObjectConverter.convert(payload.getString(FORM_DATA));

            final JsonObject assigneeForm = jsonObject.getJsonObject(DATA);

            if (nonNull(assigneeForm.getJsonArray(ASSIGNEES))) {
                final JsonArray cpsAssignees = ofNullable(assigneeForm.getJsonArray(ASSIGNEES)).orElse(JsonObjects.createArrayBuilder().build());

                LOGGER.info("cpsAssignees >> {}", cpsAssignees);

                IntStream.range(0, cpsAssignees.size()).mapToObj(
                                cpsAssignees::getJsonObject)
                        .forEach(prosecutionAssignee -> mapCpsAssignee(assignees, prosecutionAssignee));
            }
        }
        return assignees.entrySet().stream().sorted(Map.Entry.comparingByKey())
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue,
                        (lastValue, currentValue) -> lastValue, LinkedHashMap::new));
    }

    private String mapCpsAssignee(final Map<UUID, String> assignee, final JsonObject prosecutionAssignee) {
        return assignee.putIfAbsent(nonNull(prosecutionAssignee.getString("id")) ? UUID.fromString(prosecutionAssignee.getString("id")) : UUID.randomUUID(),
                prosecutionAssignee.getString("firstName", "")
                        .concat(" ").concat(prosecutionAssignee.getString("lastName", ""))
        );
    }
}
