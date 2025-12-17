package uk.gov.justice.api.resource.utils;

import static java.lang.Boolean.TRUE;
import static java.lang.String.format;
import static java.lang.System.lineSeparator;
import static java.util.Comparator.comparing;
import static java.util.Comparator.naturalOrder;
import static java.util.Comparator.nullsLast;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toList;
import static javax.json.Json.createValue;
import static javax.json.JsonValue.ValueType.STRING;
import static uk.gov.justice.api.resource.utils.ResultPromptValueHelper.getValue;

import uk.gov.justice.api.resource.dto.ResultDefinition;
import uk.gov.justice.api.resource.dto.ResultDefinitionPrompt;
import uk.gov.justice.api.resource.dto.ResultPrompt;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import javax.json.JsonArray;
import javax.json.JsonObject;

import org.apache.commons.lang3.StringUtils;

public class ResultTextHelper {
    private static final String EXCLUDED_PROMPT_REFERENCE = "hmiSlots";
    private static final Predicate<ResultPrompt> PROMPT_PREDICATE = p -> !EXCLUDED_PROMPT_REFERENCE.equals(p.getPromptRef());
    private static final String ONEOF_PROMPT_TYPE = "ONEOF";
    private static final String NAMEADDRESS_PROMPT_TYPE = "NAMEADDRESS";
    private static final String ADDRESS_PROMPT_TYPE = "ADDRESS";
    private static final String BOOLEAN_PROMPT_TYPE = "BOOLEAN";
    private static final String VALUE_KEY = "value";

    public static String getResultText(final ResultDefinition resultDefinition, final List<ResultPrompt> resultLinePrompts) {

        final List<ResultDefinitionPrompt> referencePromptList = resultDefinition
                .getPrompts()
                .stream()
                .filter(p -> !TRUE.equals(p.isHidden()))
                .sorted(comparing(ResultDefinitionPrompt::getSequence, nullsLast(naturalOrder())))
                .filter(Objects::nonNull)
                .collect(toList());

        final List<UUID> referenceList = referencePromptList
                .stream()
                .map(ResultDefinitionPrompt::getId)
                .collect(toList());

        final List<ResultPrompt> sortedPromptList = resultLinePrompts
                .stream()
                .filter(p -> referenceList.contains(p.getPromptId()))
                .sorted(new UUIDComparator(referenceList))
                .toList();

        final String sortedPrompts = sortedPromptList
                .stream()
                .filter(PROMPT_PREDICATE)
                .map(ResultTextHelper::toResultPrompts)
                .map(p -> format("%s %s", p.getLabel(), getPromptValue(p, referencePromptList)))
                .collect(joining(lineSeparator()));

        return format("%s%s%s", resultDefinition.getLabel(), lineSeparator(), sortedPrompts);
    }

    private static ResultPrompt toResultPrompts(final ResultPrompt resultPrompt) {
        if (ONEOF_PROMPT_TYPE.equalsIgnoreCase(resultPrompt.getType())) {
            JsonObject jsonObject = (JsonObject) resultPrompt.getValue();
            if (jsonObject.get("value") instanceof final JsonArray jsonArray) {
                return jsonArrayToResultPrompt(resultPrompt, jsonArray);
            }
            return jsonObjectToResultPrompt(jsonObject);

        } else if (NAMEADDRESS_PROMPT_TYPE.equalsIgnoreCase(resultPrompt.getType())
                || ADDRESS_PROMPT_TYPE.equalsIgnoreCase(resultPrompt.getType())) {

            final JsonArray nameAddressArray = (JsonArray) resultPrompt.getValue();
            return jsonArrayToResultPrompt(resultPrompt, nameAddressArray);
        }

        return resultPrompt;
    }

    private static ResultPrompt jsonArrayToResultPrompt(final ResultPrompt resultPrompt, final JsonArray jsonArray) {
        final String value = !jsonArray.isEmpty() ? jsonArray.stream()
                .map(jv -> (JsonObject) jv)
                .filter(Objects::nonNull)
                .map(jObj -> jObj.get(VALUE_KEY).getValueType() == STRING
                        ? jObj.getString(VALUE_KEY)
                        : jObj.get(VALUE_KEY).toString())
                .collect(Collectors.joining(",")) : StringUtils.EMPTY;

        return ResultPrompt.prompt()
                .withId(resultPrompt.getPromptId())
                .withPromptRef(resultPrompt.getPromptRef())
                .withType(resultPrompt.getType())
                .withLabel(resultPrompt.getLabel())
                .withValue(createValue(value))
                .build();
    }

    private static ResultPrompt jsonObjectToResultPrompt(final JsonObject jsonObject) {
        return ResultPrompt.prompt()
                .withId(UUID.fromString(jsonObject.getString("promptId")))
                .withPromptRef(jsonObject.getString("promptRef"))
                .withType(jsonObject.getString("type"))
                .withLabel(jsonObject.getString("label"))
                .withValue(jsonObject.get("value"))
                .build();
    }

    private static String getPromptValue(final ResultPrompt prompt, final List<ResultDefinitionPrompt> referencePromptList) {
        final Optional<ResultDefinitionPrompt> optionalPrompt = referencePromptList.stream().filter(p -> p.getId().equals(prompt.getPromptId())).findFirst();
        final String originalValue = getValue(prompt.getType(), prompt.getValue());

        if (optionalPrompt.isPresent() && BOOLEAN_PROMPT_TYPE.equalsIgnoreCase(optionalPrompt.get().getType())) {
            return "true".equalsIgnoreCase(originalValue) ? "Yes" : "No";
        }
        return originalValue;
    }

}
