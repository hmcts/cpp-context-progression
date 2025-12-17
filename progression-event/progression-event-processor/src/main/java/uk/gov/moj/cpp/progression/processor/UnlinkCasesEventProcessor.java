package uk.gov.moj.cpp.progression.processor;

import static uk.gov.justice.services.core.annotation.Component.EVENT_PROCESSOR;
import static uk.gov.justice.services.core.enveloper.Enveloper.envelop;
import static uk.gov.moj.cpp.progression.helper.LinkSplitMergeHelper.CASES;
import static uk.gov.moj.cpp.progression.helper.LinkSplitMergeHelper.LINK_ACTION_TYPE;
import static uk.gov.moj.cpp.progression.helper.LinkSplitMergeHelper.UNLINK;
import static uk.gov.moj.cpp.progression.helper.LinkSplitMergeHelper.buildCaseLinkedOrUnlinkedEventJson;

import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.progression.events.CasesUnlinked;

import javax.inject.Inject;
import javax.json.Json;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ServiceComponent(EVENT_PROCESSOR)
public class UnlinkCasesEventProcessor {
    private static final Logger LOGGER = LoggerFactory.getLogger(UnlinkCasesEventProcessor.class);


    @Inject
    private Sender sender;

    @Inject
    private JsonObjectToObjectConverter jsonObjectConverter;

    @Handles("progression.event.cases-unlinked")
    public void casesUnlinked(final JsonEnvelope event) {
        if (LOGGER.isInfoEnabled()) {
            LOGGER.info("Received '{}' event with payload {}", "progression.event.cases-unlinked", event.toObfuscatedDebugString());
        }

        final CasesUnlinked casesUnlinked = jsonObjectConverter.convert(event.payloadAsJsonObject(), CasesUnlinked.class);
        final JsonObject eventPayload = buildCaseUnlinkedEventPayload(casesUnlinked);

        sender.send(
                envelop(eventPayload)
                        .withName("public.progression.case-linked")
                        .withMetadataFrom(event));
    }

    private JsonObject buildCaseUnlinkedEventPayload(final CasesUnlinked casesUnlinked) {

        final JsonObjectBuilder payloadBuilder = Json.createObjectBuilder();
        payloadBuilder.add(LINK_ACTION_TYPE, UNLINK);
        final JsonArrayBuilder casesArrayBuilder = Json.createArrayBuilder();

        casesUnlinked.getUnlinkedCases().forEach(
                unlinkedCases ->
                        buildCaseLinkedOrUnlinkedEventJson(casesArrayBuilder, casesUnlinked.getProsecutionCaseId(), casesUnlinked.getProsecutionCaseUrn(),
                                unlinkedCases.getCaseId().toString(), unlinkedCases.getCaseUrn())
        );
        payloadBuilder.add(CASES, casesArrayBuilder.build());
        return payloadBuilder.build();
    }

}
