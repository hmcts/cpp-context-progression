package uk.gov.moj.cpp.progression.query.view.service.transformer;


import uk.gov.moj.cpp.progression.domain.pojo.FixList;
import uk.gov.moj.cpp.progression.domain.pojo.Prompt;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class TransformerUtil {
    private TransformerUtil() {
    }

    public static List<FixList> generateFixList(final Map<UUID, String> values) {
        final List<FixList> fixListValueList = new ArrayList<>();
        values.forEach((key, value) -> fixListValueList.add(new FixList(key.toString(), value, false)));
        return fixListValueList;
    }

    public static List<FixList> generateFixList(final Map<UUID, String> values, final Boolean isDefendant) {
        final List<FixList> fixListValueList = new ArrayList<>();
        values.forEach((key, value) -> fixListValueList.add(new FixList(key.toString(), value, isDefendant)));
        return fixListValueList;
    }

    public static List<FixList> generateFixList(final List<String> values, final String key) {
        final List<FixList> fixListValueList = new ArrayList<>();
        values.forEach(value -> fixListValueList.add(new FixList(key, value, false)));
        return fixListValueList;
    }

    public static List<FixList> generateFixList(final List<String> values, final String key, final Boolean isDefendant) {
        final List<FixList> fixListValueList = new ArrayList<>();
        values.forEach(value -> fixListValueList.add(new FixList(key, value, isDefendant)));
        return fixListValueList;
    }

    public static List<Prompt> generatePromptList(final Map<UUID, String> values, final Integer promptOrder, final Boolean isDefendant) {
        final List<Prompt> promptListValueList = new ArrayList<>();
        final Integer[] index = {promptOrder};
        values.forEach((key, value) -> promptListValueList.add(Prompt.prompt()
                .withPromptOrder(++index[0])
                .withName(key.toString())
                .withLabel(value)
                .withType("option")
                .withIsDefendant(isDefendant)
                .withLabelId(key.toString())
                .withId(UUID.randomUUID())
                .build()));
        return promptListValueList;
    }
}
