package uk.gov.moj.cpp.progression.helper;

import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.progression.domain.event.link.LinkType;
import uk.gov.moj.cpp.progression.events.LinkResponseResults;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import uk.gov.justice.services.messaging.JsonObjects;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;

public class LinkSplitMergeHelper {

    public static final String PROGRESSION_COMMAND_PROCESS_LINK_CASES = "progression.command.link-cases";
    public static final String PUBLIC_PROGRESSION_LINK_CASES_RESPONSE = "public.progression.link-cases-response";
    public static final String PUBLIC_PROGRESSION_CASE_LINKED = "public.progression.case-linked";
    public static final String CASE_ID = "caseId";
    public static final String CASE_URN = "caseUrn";
    public static final String LINKED_TO_CASES = "linkedToCases";
    public static final String CASES = "cases";
    public static final String LINKED_CASES = "linkedCases";
    public static final String MERGED_CASES = "mergedCases";
    public static final String SPLIT_CASES = "splitCases";
    public static final String SEARCH_RESULTS = "searchResults";
    public static final String LINK_ACTION_TYPE = "linkActionType";
    public static final String UNLINK = "UNLINK";

    private LinkSplitMergeHelper() {
        throw new IllegalStateException("Utility class");
    }

    public static void buildCaseLinkedOrUnlinkedEventJson(final JsonArrayBuilder arrayBuilder, final UUID leadCaseId, final String leadCaseUrn, final String linkedCaseId, final String linkCaseUrn) {

        arrayBuilder.add(buildLinkOrUnlinkCaseJson(leadCaseId, leadCaseUrn,
                UUID.fromString(linkedCaseId), linkCaseUrn));

        arrayBuilder.add(buildLinkOrUnlinkCaseJson(UUID.fromString(linkedCaseId), linkCaseUrn,
                leadCaseId, leadCaseUrn));
    }

    public static JsonObject buildLinkOrUnlinkCaseJson(final UUID caseId, final String caseUrn, final UUID linkedCaseId, final String linkedCaseUrn) {
        final JsonObjectBuilder caseJsonBuilder = JsonObjects.createObjectBuilder()
                .add(CASE_ID, caseId.toString())
                .add(CASE_URN, caseUrn);

        final JsonArrayBuilder linkedCasesArrayBuilder = JsonObjects.createArrayBuilder();
        final JsonObjectBuilder linkedCaseJsonBuilder = JsonObjects.createObjectBuilder()
                .add(CASE_ID, linkedCaseId.toString())
                .add(CASE_URN, linkedCaseUrn);

        linkedCasesArrayBuilder.add(linkedCaseJsonBuilder.build());
        caseJsonBuilder.add(LINKED_TO_CASES, linkedCasesArrayBuilder.build());
        return caseJsonBuilder.build();
    }

    public static JsonObject buildLSMCommandPayload(final JsonEnvelope envelope, final LinkType linkType) {
        return JsonObjects.createObjectBuilder()
                .add("prosecutionCaseId", envelope.payloadAsJsonObject().get("prosecutionCaseId"))
                .add("casesToLink", JsonObjects.createArrayBuilder().add(JsonObjects.createObjectBuilder().add("caseUrns", envelope.payloadAsJsonObject().get("caseUrns"))
                        .add("caseLinkType", linkType.toString()))
                        .build())
                .build();
    }

    public static JsonObject createResponsePayload(final LinkResponseResults response) {
        return createResponsePayload(response, new ArrayList());
    }

    public static JsonObject createResponsePayload(final LinkResponseResults response,
                                                   final List<String> invalidCaseUrns) {

        final JsonObjectBuilder objectBuilder = JsonObjects.createObjectBuilder()
                .add("linkResponseResults", response.toString());

        if (!invalidCaseUrns.isEmpty()) {
            final JsonArrayBuilder invalidCaseUrnsArray = JsonObjects.createArrayBuilder();
            for (final String caseUrn : invalidCaseUrns) {
                invalidCaseUrnsArray.add(caseUrn);
            }
            objectBuilder.add("invalidCaseUrns", invalidCaseUrnsArray);
        }
        return objectBuilder.build();
    }
}
