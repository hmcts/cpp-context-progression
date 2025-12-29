package uk.gov.moj.cpp.progression.processor;

import static uk.gov.justice.services.core.annotation.Component.EVENT_PROCESSOR;
import static uk.gov.moj.cpp.progression.helper.LinkSplitMergeHelper.CASES;
import static uk.gov.moj.cpp.progression.helper.LinkSplitMergeHelper.CASE_ID;
import static uk.gov.moj.cpp.progression.helper.LinkSplitMergeHelper.LINKED_CASES;
import static uk.gov.moj.cpp.progression.helper.LinkSplitMergeHelper.LINK_ACTION_TYPE;
import static uk.gov.moj.cpp.progression.helper.LinkSplitMergeHelper.PROGRESSION_COMMAND_PROCESS_LINK_CASES;
import static uk.gov.moj.cpp.progression.helper.LinkSplitMergeHelper.PUBLIC_PROGRESSION_CASE_LINKED;
import static uk.gov.moj.cpp.progression.helper.LinkSplitMergeHelper.PUBLIC_PROGRESSION_LINK_CASES_RESPONSE;
import static uk.gov.moj.cpp.progression.helper.LinkSplitMergeHelper.buildCaseLinkedOrUnlinkedEventJson;
import static uk.gov.moj.cpp.progression.helper.LinkSplitMergeHelper.buildLSMCommandPayload;
import static uk.gov.moj.cpp.progression.helper.LinkSplitMergeHelper.createResponsePayload;

import uk.gov.justice.core.courts.ProsecutionCase;
import uk.gov.justice.core.courts.ProsecutionCaseIdentifier;
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
import uk.gov.moj.cpp.progression.events.ValidateLinkCases;
import uk.gov.moj.cpp.progression.service.ProgressionService;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.inject.Inject;
import uk.gov.justice.services.messaging.JsonObjects;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@SuppressWarnings({"squid:S3655", "squid:S1188", "squid:S3776"})
@ServiceComponent(EVENT_PROCESSOR)
public class LinkCasesEventProcessor {

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

    private static final Logger LOGGER = LoggerFactory.getLogger(LinkCasesEventProcessor.class);

    @Handles("progression.event.validate-link-cases")
    public void handleLinkCasesValidations(final JsonEnvelope envelope) {
        final ValidateLinkCases validateLinkCases = jsonObjectToObjectConverter.convert(envelope.payloadAsJsonObject(), ValidateLinkCases.class);

        if (LOGGER.isInfoEnabled()) {
            LOGGER.info("Link cases validation payload - {}", envelope.payloadAsJsonObject());
        }

        final AtomicBoolean failed = new AtomicBoolean(false);
        validateLinkCases.getCaseUrns().stream().forEach(
                e -> {
                    //check if given URN actually exists in a case
                    final Optional<JsonObject> existingCase = progressionService.caseExistsByCaseUrn(envelope, e);
                    if (existingCase.get().isEmpty()) {
                        sender.send(Enveloper.envelop(createResponsePayload(LinkResponseResults.REFERENCE_NOT_FOUND)).withName(PUBLIC_PROGRESSION_LINK_CASES_RESPONSE).withMetadataFrom(envelope));
                        failed.set(true);
                        LOGGER.error("Link cases failed. Reference not found - {}", envelope.payloadAsJsonObject());
                    } else {
                        if (existingCase.get().getString(CASE_ID).equals(validateLinkCases.getProsecutionCaseId().toString())) {
                            sender.send(Enveloper.envelop(createResponsePayload(LinkResponseResults.REFERENCE_NOT_VALID)).withName(PUBLIC_PROGRESSION_LINK_CASES_RESPONSE).withMetadataFrom(envelope));
                            failed.set(true);
                            LOGGER.error("Link cases failed. Reference not valid, can not link case to itself - {}", envelope.payloadAsJsonObject());
                        }
                    }

                    if (!failed.get()) {
                        //check if given URN is already linked before
                        final Optional<JsonObject> alreadyLinkedCases = progressionService.searchLinkedCases(envelope, validateLinkCases.getProsecutionCaseId().toString());
                        if (alreadyLinkedCases.get().size() > 0 && alreadyLinkedCases.get().containsKey(LINKED_CASES) && !alreadyLinkedCases.get().getJsonArray(LINKED_CASES).isEmpty()) {
                            alreadyLinkedCases.get().getJsonArray(LINKED_CASES).stream().forEach(
                                    lc -> {
                                        final JsonObject linkedCase = JsonObjects.createObjectBuilder().add("linkedCase", lc).build();
                                        if (linkedCase.getJsonObject("linkedCase").getString(CASE_ID).equals(existingCase.get().getString(CASE_ID))) {
                                            sender.send(Enveloper.envelop(createResponsePayload(LinkResponseResults.REFERENCE_ALREADY_LINKED)).withName(PUBLIC_PROGRESSION_LINK_CASES_RESPONSE).withMetadataFrom(envelope));
                                            failed.set(true);
                                            LOGGER.error("Link cases failed. Reference already linked - {}", envelope.payloadAsJsonObject());
                                        }
                                    });
                        }
                    }

                }
        );

        if (!failed.get()) {
            // raise a new command to do the actual linking
            final Envelope<JsonObject> processLinkEnvelope = Enveloper.envelop(buildLSMCommandPayload(envelope, LinkType.LINK))
                    .withName(PROGRESSION_COMMAND_PROCESS_LINK_CASES)
                    .withMetadataFrom(envelope);
            sender.send(processLinkEnvelope);
            LOGGER.info("Link cases process payload - {}", processLinkEnvelope.payload());

            //raise  public event for other contexts (listing)
            final Envelope<JsonObject> listingEventEnvelope = Enveloper.envelop(buildCasesLinkedEventPayload(envelope, validateLinkCases.getProsecutionCaseId(), validateLinkCases.getCaseUrns()))
                    .withName(PUBLIC_PROGRESSION_CASE_LINKED)
                    .withMetadataFrom(envelope);
            sender.send(listingEventEnvelope);
            LOGGER.info("Case linked public event payload - {}", listingEventEnvelope.payload());

            // finally success event for UI
            final Envelope<JsonObject> responseEventPayload = Enveloper.envelop(createResponsePayload(LinkResponseResults.SUCCESS))
                    .withName(PUBLIC_PROGRESSION_LINK_CASES_RESPONSE)
                    .withMetadataFrom(envelope);
            sender.send(responseEventPayload);
            LOGGER.info("Link cases response event payload - {}", responseEventPayload.payload());
        }
    }

    private JsonObject buildCasesLinkedEventPayload(final JsonEnvelope envelope, final UUID leadCaseId, final List<String> caseUrns) {
        final JsonObjectBuilder payloadBuilder = JsonObjects.createObjectBuilder().add(LINK_ACTION_TYPE, LinkType.LINK.toString());
        final JsonArrayBuilder arrayBuilder = JsonObjects.createArrayBuilder();
        // for case reference; caseURN is used  for spi cases, and prosecutionAuthorityReference is used for sjp cases
        final ProsecutionCaseIdentifier pci = jsonObjectToObjectConverter.convert(progressionService.getProsecutionCaseDetailById(envelope, leadCaseId.toString()).get().getJsonObject("prosecutionCase"), ProsecutionCase.class).getProsecutionCaseIdentifier();
        final String leadCaseUrn = pci.getCaseURN() != null ? pci.getCaseURN() : pci.getProsecutionAuthorityReference();

        //  build public message for listing context
        caseUrns.forEach(
                linkCaseUrn -> {
                    //find caseId for given caseURN in the request
                    final String linkedCaseId = progressionService.caseExistsByCaseUrn(envelope, linkCaseUrn).get().getString(CASE_ID);
                    buildCaseLinkedOrUnlinkedEventJson(arrayBuilder, leadCaseId, leadCaseUrn, linkedCaseId, linkCaseUrn);
                }
        );

        return payloadBuilder.add(CASES, arrayBuilder.build()).build();
    }


}
