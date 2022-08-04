package uk.gov.moj.cpp.progression.domain.transformation;

import static com.google.common.collect.Lists.newArrayList;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class EventMapper {

    private EventMapper() {
    }

    private static final Map<String, List<String>> EVENT_MAP = new HashMap();

    static {
        EVENT_MAP.put("progression.event.prosecutionCase-defendant-listing-status-changed", newArrayList(
            "$.hearing.id"));

        EVENT_MAP.put("progression.event.prosecutionCase-defendant-listing-status-changed-v2", newArrayList(
                "$.hearing.id"));

        EVENT_MAP.put("progression.event.hearing-resulted", newArrayList(
                "$.hearing.id"));

        EVENT_MAP.put("progression.event.hearing-resulted-case-updated", newArrayList(
        ));

        EVENT_MAP.put("progression.hearing-initiate-enriched", newArrayList(
                "$.hearing.id"
        ));

        EVENT_MAP.put("progression.event.court-application-added-to-case", newArrayList(
        ));

        EVENT_MAP.put("progression.event.boxwork-application-referred", newArrayList(
        ));

        EVENT_MAP.put("progression.event.application-referred-to-court", newArrayList(
                "$.hearingRequest.id"
        ));

        EVENT_MAP.put("progression.event.hearing-extended", newArrayList(
                "$.hearingRequest.id"
        ));

        EVENT_MAP.put("progression.event.court-application-updated", newArrayList(
        ));

        EVENT_MAP.put("progression.event.court-application-created", newArrayList(
        ));

        EVENT_MAP.put("progression.event.hearing-application-link-created", newArrayList(
                "$.hearing.id"
        ));
    }

    public static Collection getEventNames() {
        return EVENT_MAP.keySet();
    }

    public static List<String> getMappedJsonPaths(String eventName) {
        return EVENT_MAP.get(eventName);
    }

}
