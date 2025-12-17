package uk.gov.justice.api.resource.utils;

import static java.lang.String.format;
import static uk.gov.justice.api.resource.utils.ResultPromptValueHelper.ResultPromptType.getType;

import java.text.DecimalFormat;
import java.text.ParseException;
import java.util.Arrays;
import java.util.stream.Collectors;

import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.JsonString;
import javax.json.JsonValue;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ResultPromptValueHelper {

    public static final String SPACE_DELIMITER = " ";
    public static final String SERIALIZATION_DELIMITER = "###";
    public static DecimalFormat decimalFormat = new DecimalFormat("0.00");
    public static String durationFormat = "%s %s";
    private static final Logger LOGGER = LoggerFactory.getLogger(ResultPromptValueHelper.class);

    public static String getValue(final String type, final JsonValue value) {
        return getType(type).toValue(value);
    }

    public enum ResultPromptType {

        YESBOX {
            @Override
            public String toValue(JsonValue fromValue) {
                return fromValue.toString();
            }
        },
        BOOLEAN {
            @Override
            public String toValue(JsonValue fromValue) {
                return fromValue.toString();
            }
        },
        CURR {
            @Override
            public String toValue(final JsonValue fromValue) {
                try {
                    return decimalFormat.format(decimalFormat.parse(fromValue.toString()));
                } catch (ParseException e) {
                    LOGGER.warn("Failed to parse currency value: {}, returning value as is", fromValue);
                    return ((JsonString) fromValue).getString();
                }
            }
        },
        DURATION {
            @Override
            public String toValue(final JsonValue fromValue) {
                JsonArray durationElements = (JsonArray) fromValue;
                return durationElements.stream()
                        .map(JsonValue::asJsonObject)
                        .map(jsonObject -> format(durationFormat, jsonObject.get("value"),
                                jsonObject.getString("label")))
                        .collect(Collectors.joining(SPACE_DELIMITER));
            }
        }, FIXLM {
            @Override
            public String toValue(final JsonValue fromValue) {
                JsonArray durationElements = (JsonArray) fromValue;
                return durationElements.stream()
                        .map(js -> ((JsonString) js).getString())
                        .collect(Collectors.joining(SERIALIZATION_DELIMITER));
            }
        },
        FIXLOM {
            @Override
            public String toValue(final JsonValue fromValue) {
                JsonArray durationElements = (JsonArray) fromValue;
                return durationElements.stream()
                        .map(js -> ((JsonString) js).getString())
                        .collect(Collectors.joining(SERIALIZATION_DELIMITER));
            }
        },
        ONEOF {
            @Override
            public String toValue(final JsonValue fromValue) {
                if (fromValue instanceof final JsonObject jsonObject) {
                    return getType(jsonObject.getString("type")).toValue(jsonObject.get("value"));
                }
                return fromValue.toString();
            }
        },
        DEFAULT {
            @Override
            public String toValue(final JsonValue fromValue) {
                if (fromValue.getValueType() == JsonValue.ValueType.STRING){
                    return ((JsonString) fromValue).getString();
                }

                LOGGER.error("Unhandled Json ValueType {} found, returning value as string", fromValue.getValueType());
                return fromValue.toString();
            }
        };

        public abstract String toValue(JsonValue fromValue);

        public static ResultPromptType getType(String typeStr) {
            return Arrays.stream(ResultPromptType.values())
                    .filter(rpt -> rpt.name().equals(typeStr))
                    .findFirst()
                    .orElse(DEFAULT);
        }
    }
}
