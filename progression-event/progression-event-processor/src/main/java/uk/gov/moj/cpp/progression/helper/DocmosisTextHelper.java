package uk.gov.moj.cpp.progression.helper;

import java.util.HashMap;
import java.util.Map;

public class DocmosisTextHelper {
    private Map<String, String> replaceChars = new HashMap();

    public DocmosisTextHelper(){
        replaceChars.put("’", "'");
        replaceChars.put("–", "-");
        replaceChars.put("“", "\\\"");
        replaceChars.put("”", "\\\"");
    }

    public String replaceEscapeCharForDocmosis(final String value) {

        String updated = value;

        for (final Map.Entry<String, String> entry : replaceChars.entrySet()) {
            updated = updated.replace(entry.getKey(), entry.getValue());
        }
        return updated;
    }

}
