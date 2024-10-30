package uk.gov.moj.cpp.progression.processor;

import static uk.gov.justice.services.core.annotation.Component.EVENT_PROCESSOR;
import static uk.gov.moj.cpp.progression.helper.LinkSplitMergeHelper.CASES;
import static uk.gov.moj.cpp.progression.helper.LinkSplitMergeHelper.CASE_ID;
import static uk.gov.moj.cpp.progression.helper.LinkSplitMergeHelper.CASE_URN;
import static uk.gov.moj.cpp.progression.helper.LinkSplitMergeHelper.LINK_ACTION_TYPE;
import static uk.gov.moj.cpp.progression.helper.LinkSplitMergeHelper.MERGED_CASES;
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
import uk.gov.moj.cpp.progression.events.ValidateMergeCases;
import uk.gov.moj.cpp.progression.service.ProgressionService;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.inject.Inject;
import javax.json.Json;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@SuppressWarnings({"squid:S3655", "squid:S1188", "squid:S3776"})
@ServiceComponent(EVENT_PROCESSOR)
public class MergeCasesEventProcessor {

    public static final String IMPLICIT_MERGED_CASE = "implicitMergedCase";
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

    @Handles("progression.event.validate-merge-cases")
    public void handleMergeCasesValidations(final JsonEnvelope envelope) {
        final ValidateMergeCases validateMergeCases = jsonObjectToObjectConverter.convert(envelope.payloadAsJsonObject(), ValidateMergeCases.class);

        if (LOGGER.isInfoEnabled()) {
            LOGGER.info("Merge cases validation payload - {}", envelope.payloadAsJsonObject());
        }

        final AtomicBoolean failed = new AtomicBoolean(false);
        validateMergeCases.getCaseUrns().forEach(
                e -> {
                    //check if given URN actually exists in a case
                    final Optional<JsonObject> existingCase = progressionService.caseExistsByCaseUrn(envelope, e);
                    if (existingCase.get().isEmpty()) {
                        sender.send(Enveloper.envelop(createResponsePayload(LinkResponseResults.REFERENCE_NOT_FOUND)).withName(PUBLIC_PROGRESSION_LINK_CASES_RESPONSE).withMetadataFrom(envelope));
                        failed.set(true);
                        LOGGER.error("Merge cases failed. Reference not found - {}", envelope.payloadAsJsonObject());
                    } else {
                        if (existingCase.get().getString(CASE_ID).equals(validateMergeCases.getProsecutionCaseId().toString())) { //get(0) is safe here
                            sender.send(Enveloper.envelop(createResponsePayload(LinkResponseResults.REFERENCE_NOT_VALID)).withName(PUBLIC_PROGRESSION_LINK_CASES_RESPONSE).withMetadataFrom(envelope));
                            failed.set(true);
                            LOGGER.error("Merge cases failed. Reference not valid, can not merge case to itself - {}", envelope.payloadAsJsonObject());
                        }
                    }
                    if (!failed.get()) {
                        //check if given URN is already linked before
                        final Optional<JsonObject> alreadyMergedCases = progressionService.searchLinkedCases(envelope, validateMergeCases.getProsecutionCaseId().toString());
                        if (alreadyMergedCases.get().size() > 0 && alreadyMergedCases.get().containsKey(MERGED_CASES) && !alreadyMergedCases.get().getJsonArray(MERGED_CASES).isEmpty()) {
                            alreadyMergedCases.get().getJsonArray(MERGED_CASES).stream().forEach(
                                    mc -> {
                                        final JsonObject mergedCase = Json.createObjectBuilder().add("mergedCase", mc).build();
                                        if (mergedCase.getJsonObject("mergedCase").getString(CASE_ID).equals(existingCase.get().getString(CASE_ID))) {
                                            sender.send(Enveloper.envelop(createResponsePayload(LinkResponseResults.REFERENCE_ALREADY_LINKED)).withName(PUBLIC_PROGRESSION_LINK_CASES_RESPONSE).withMetadataFrom(envelope));
                                            failed.set(true);
                                            LOGGER.error("Merge cases failed. Reference already merged - {}", envelope.payloadAsJsonObject());
                                        }
                                    });
                        }
                    }

                }
        );

        if (!failed.get()) {
            // raise a new command to do the actual linking
            final Envelope<JsonObject> processMergeEnvelope = Enveloper.envelop(buildLSMCommandPayload(envelope, LinkType.MERGE))
                    .withName(PROGRESSION_COMMAND_PROCESS_LINK_CASES)
                    .withMetadataFrom(envelope);
            sender.send(processMergeEnvelope);
            LOGGER.info("Merge cases process payload - {}", processMergeEnvelope.payload());

            //raise  public event for other contexts (listing)
            final Envelope<JsonObject> listingEventEnvelope = Enveloper.envelop(buildCasesMergedEventPayload(envelope, validateMergeCases.getProsecutionCaseId(), validateMergeCases.getCaseUrns()))
                    .withName(PUBLIC_PROGRESSION_CASE_LINKED)
                    .withMetadataFrom(envelope);
            sender.send(listingEventEnvelope);
            LOGGER.info("Case merged public event payload - {}", listingEventEnvelope.payload());

            // finally success event for UI
            final Envelope<JsonObject> responseEventPayload = Enveloper.envelop(createResponsePayload(LinkResponseResults.SUCCESS))
                    .withName(PUBLIC_PROGRESSION_LINK_CASES_RESPONSE)
                    .withMetadataFrom(envelope);
            sender.send(responseEventPayload);
            LOGGER.info("Merge cases response event payload - {}", responseEventPayload.payload());
        }

    }

    private JsonObject buildCasesMergedEventPayload(final JsonEnvelope envelope, final UUID leadCaseId, final List<String> caseUrns) {
        final JsonObjectBuilder payloadBuilder = Json.createObjectBuilder().add(LINK_ACTION_TYPE, LinkType.LINK.toString()); // type is LINK in the listing public event, even for merge
        final JsonArrayBuilder arrayBuilder = Json.createArrayBuilder();
        // for case reference; caseURN is used  for spi cases, and prosecutionAuthorityReference is used for sjp cases
        final ProsecutionCaseIdentifier pci = jsonObjectToObjectConverter.convert(progressionService.getProsecutionCaseDetailById(envelope, leadCaseId.toString()).get().getJsonObject("prosecutionCase"), ProsecutionCase.class).getProsecutionCaseIdentifier();
        final String leadCaseUrn = pci.getCaseURN() != null ? pci.getCaseURN() : pci.getProsecutionAuthorityReference();

        // build public message for listing context
        caseUrns.forEach(
                mergeCaseUrn -> {
                    //find caseId for given caseURN in the request
                    final String mergeCaseId = progressionService.caseExistsByCaseUrn(envelope, mergeCaseUrn).get().getString(CASE_ID);
                    final JsonObject previousMergeSearchResult = progressionService.searchLinkedCases(envelope, mergeCaseId).get();
                    buildCaseLinkedOrUnlinkedEventJson(arrayBuilder, leadCaseId, leadCaseUrn, mergeCaseId, mergeCaseUrn);

                    // don't forget to add implicitly merged cases in the public event
                    if (!previousMergeSearchResult.isEmpty() && previousMergeSearchResult.containsKey(MERGED_CASES)) {
                        previousMergeSearchResult.getJsonArray(MERGED_CASES).forEach(
                                pmc -> {
                                    final JsonObject implicitMergedCase = Json.createObjectBuilder().add(IMPLICIT_MERGED_CASE, pmc).build();
                                    buildCaseLinkedOrUnlinkedEventJson(arrayBuilder, UUID.fromString(mergeCaseId), mergeCaseUrn, implicitMergedCase.getJsonObject(IMPLICIT_MERGED_CASE).getString(CASE_ID), implicitMergedCase.getJsonObject(IMPLICIT_MERGED_CASE).getString(CASE_URN));

                                }
                        );
                    }

                }
        );

        return payloadBuilder.add(CASES, arrayBuilder.build()).build();
    }
}
