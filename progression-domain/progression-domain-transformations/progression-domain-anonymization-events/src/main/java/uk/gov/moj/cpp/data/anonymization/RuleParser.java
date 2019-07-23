package uk.gov.moj.cpp.data.anonymization;


import org.apache.commons.io.IOUtils;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import static java.nio.charset.StandardCharsets.UTF_8;

public class RuleParser {

    public static final String EVENT_NAME = "eventName";
    public static final String FIELDS_TO_BE_ANONYMISED = "fieldsToBeAnonymised";

    public Map<String, Map<String, String>> loadAnanymisationRules(String ruleFileName) throws IOException {

        final StringWriter writer = new StringWriter();

        final InputStream stream = RuleParser.class.getResourceAsStream(ruleFileName);
        IOUtils.copy(stream, writer, UTF_8);
        SchemaValidatorUtil.validateAgainstSchema("/schema/data-anon.json", writer.toString());

        final JSONObject rules = new JSONObject(writer.toString());
        final JSONArray array = rules.getJSONArray("events");
        final Map<String, Map<String, String>> rulesMap = new HashMap();

        for(int i = 0; i < array.length(); i++) {
            final JSONObject eventObject = array.getJSONObject(i);
            final String eventName = eventObject.getString(EVENT_NAME);
            final JSONArray fieldsToBeAnonymisedArray = eventObject.getJSONArray(FIELDS_TO_BE_ANONYMISED);
            final Map<String, String> ananymisationRules = new HashMap();

            for (int j = 0; j < fieldsToBeAnonymisedArray.length(); j++) {
                final JSONObject fieldToBeAnonymised = fieldsToBeAnonymisedArray.getJSONObject(j);
                final Set<String> keys = fieldToBeAnonymised.keySet();
                for(final String key : keys) {
                    final String keyValue = (String) fieldToBeAnonymised.get(key);
                    ananymisationRules.put(key, keyValue);
                }
                rulesMap.put(eventName, ananymisationRules);
            }
        }
        return rulesMap;
    }
}
