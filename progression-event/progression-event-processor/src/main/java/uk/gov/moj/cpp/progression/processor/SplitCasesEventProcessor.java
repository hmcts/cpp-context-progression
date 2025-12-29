package uk.gov.moj.cpp.progression.processor;

import static uk.gov.justice.services.core.annotation.Component.EVENT_PROCESSOR;
import static uk.gov.moj.cpp.progression.helper.LinkSplitMergeHelper.CASE_URN;
import static uk.gov.moj.cpp.progression.helper.LinkSplitMergeHelper.PROGRESSION_COMMAND_PROCESS_LINK_CASES;
import static uk.gov.moj.cpp.progression.helper.LinkSplitMergeHelper.PUBLIC_PROGRESSION_LINK_CASES_RESPONSE;
import static uk.gov.moj.cpp.progression.helper.LinkSplitMergeHelper.SPLIT_CASES;
import static uk.gov.moj.cpp.progression.helper.LinkSplitMergeHelper.buildLSMCommandPayload;
import static uk.gov.moj.cpp.progression.helper.LinkSplitMergeHelper.createResponsePayload;

import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.common.converter.ObjectToJsonObjectConverter;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.progression.domain.event.link.LinkType;
import uk.gov.moj.cpp.progression.events.LinkResponseResults;
import uk.gov.moj.cpp.progression.events.ValidateSplitCases;
import uk.gov.moj.cpp.progression.service.ProgressionService;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.inject.Inject;
import uk.gov.justice.services.messaging.JsonObjects;
import javax.json.JsonObject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@SuppressWarnings({"squid:S3655", "squid:S1188"})
@ServiceComponent(EVENT_PROCESSOR)
public class SplitCasesEventProcessor {

    @Inject
    private JsonObjectToObjectConverter jsonObjectToObjectConverter;

    @Inject
    private ObjectToJsonObjectConverter objectToJsonObjectConverter;

    @Inject
    private Sender sender;

    @Inject
    private Enveloper enveloper;

    @Inject
    ProgressionService progressionService;

    private static final Logger LOGGER = LoggerFactory.getLogger(SplitCasesEventProcessor.class);

    @Handles("progression.event.validate-split-cases")
    public void handleSplitCasesValidations(final JsonEnvelope envelope) {
        final ValidateSplitCases validateSplitCases = jsonObjectToObjectConverter.convert(envelope.payloadAsJsonObject(), ValidateSplitCases.class);

        if (LOGGER.isInfoEnabled()) {
            LOGGER.info("Split cases validation payload - {}", envelope.payloadAsJsonObject());
        }

        final AtomicBoolean failed = new AtomicBoolean(false);
        final List<String> invalidCaseUrns = new ArrayList();
        validateSplitCases.getCaseUrns().stream().forEach(
                e -> {
                    //check if given modified URN is already linked before
                    final Optional<JsonObject> alreadyLinkedCases = progressionService.searchLinkedCases(envelope, validateSplitCases.getProsecutionCaseId().toString());
                    if (alreadyLinkedCases.get().size() > 0 && alreadyLinkedCases.get().containsKey(SPLIT_CASES) && !alreadyLinkedCases.get().getJsonArray(SPLIT_CASES).isEmpty()) {
                        alreadyLinkedCases.get().getJsonArray(SPLIT_CASES).stream().forEach(
                                sc -> {
                                    final JsonObject splitCase = JsonObjects.createObjectBuilder().add("splitCase", sc).build();
                                    if (splitCase.getJsonObject("splitCase").getString(CASE_URN).contains(e)) {
                                        invalidCaseUrns.add(e);
                                        failed.set(true);
                                    }
                                });
                    }
                }
        );

        if (failed.get()) {
            // raise error message for UI
            sender.send(Enveloper.envelop(createResponsePayload(LinkResponseResults.REFERENCE_ALREADY_LINKED, invalidCaseUrns)).withName(PUBLIC_PROGRESSION_LINK_CASES_RESPONSE).withMetadataFrom(envelope));
            LOGGER.error("Split cases failed. Reference already exists - {}", envelope.payloadAsJsonObject());
        } else {
            // raise a new command to do the actual splitting
            final Envelope<JsonObject> processLinkEnvelope = Enveloper.envelop(buildLSMCommandPayload(envelope, LinkType.SPLIT))
                    .withName(PROGRESSION_COMMAND_PROCESS_LINK_CASES)
                    .withMetadataFrom(envelope);
            sender.send(processLinkEnvelope);
            LOGGER.info("Split cases process payload - {}", processLinkEnvelope.payload());

            // finally success event for UI
            final Envelope<JsonObject> responseEventPayload = Enveloper.envelop(createResponsePayload(LinkResponseResults.SUCCESS))
                    .withName(PUBLIC_PROGRESSION_LINK_CASES_RESPONSE)
                    .withMetadataFrom(envelope);
            sender.send(responseEventPayload);
            LOGGER.info("Link cases response event payload - {}", responseEventPayload.payload());
        }
    }


}
