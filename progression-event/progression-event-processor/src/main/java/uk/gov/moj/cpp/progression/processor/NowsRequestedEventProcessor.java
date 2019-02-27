package uk.gov.moj.cpp.progression.processor;

import static java.util.UUID.fromString;
import static javax.json.Json.createObjectBuilder;
import static uk.gov.justice.services.core.annotation.Component.EVENT_PROCESSOR;

import uk.gov.justice.core.courts.CourtDocument;
import uk.gov.justice.core.courts.CreateNowsRequest;
import uk.gov.justice.core.courts.DocumentCategory;
import uk.gov.justice.core.courts.Material;
import uk.gov.justice.core.courts.Now;
import uk.gov.justice.core.courts.NowDocument;
import uk.gov.justice.core.courts.NowType;
import uk.gov.justice.core.courts.ProsecutionCase;
import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.common.converter.ObjectToJsonObjectConverter;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.progression.event.nows.order.NowsDocumentOrder;
import uk.gov.moj.cpp.progression.service.DocumentGeneratorService;

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.json.Json;
import javax.json.JsonObject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ServiceComponent(EVENT_PROCESSOR)
public class NowsRequestedEventProcessor {

    protected static final String PROGRESSION_COMMAND_CREATE_COURT_DOCUMENT = "progression.command.create-court-document";
    private static final Logger LOGGER = LoggerFactory.getLogger(NowsRequestedEventProcessor.class);
    private final Enveloper enveloper;
    private final Sender sender;
    private final JsonObjectToObjectConverter jsonObjectToObjectConverter;
    private final ObjectToJsonObjectConverter objectToJsonObjectConverter;

    private DocumentGeneratorService documentGeneratorService;

    @Inject
    public NowsRequestedEventProcessor(final Enveloper enveloper, final Sender sender,
                                       final DocumentGeneratorService documentGeneratorService,
                                       final JsonObjectToObjectConverter jsonObjectToObjectConverter,
                                       final ObjectToJsonObjectConverter objectToJsonObjectConverter
    ) {
        this.enveloper = enveloper;
        this.sender = sender;
        this.jsonObjectToObjectConverter = jsonObjectToObjectConverter;
        this.objectToJsonObjectConverter = objectToJsonObjectConverter;
        this.documentGeneratorService = documentGeneratorService;
    }

    @Handles("progression.event.nows-requested")
    public void processNowsRequested(final JsonEnvelope event) {
        final UUID userId = fromString(event.metadata().userId().orElseThrow(() -> new RuntimeException("UserId missing from event.")));

        final JsonObject requestJson = event.payloadAsJsonObject().getJsonObject("createNowsRequest");
        final CreateNowsRequest createNowsRequest = jsonObjectToObjectConverter.convert(requestJson, CreateNowsRequest.class);
        final String hearingId = createNowsRequest.getHearing().getId().toString();
        LOGGER.info("Nows requested for hearing id {}", hearingId);

        final Map<NowsDocumentOrder, NowsNotificationDocumentState> nowsDocumentOrderToNotificationState = NowsRequestedToOrderConverter.convert(createNowsRequest);
        final List<NowsDocumentOrder> nowsDocumentOrdersList = new ArrayList<>(nowsDocumentOrderToNotificationState.keySet());

        addAsCourtDocuments(event, createNowsRequest);

        nowsDocumentOrdersList.stream().sorted(Comparator.comparing(NowsDocumentOrder::getPriority)).forEach(nowsDocumentOrder -> {
            LOGGER.info("Input for docmosis order {}", objectToJsonObjectConverter.convert(nowsDocumentOrder));
            documentGeneratorService.generateNow(sender, event, userId, createNowsRequest, hearingId, nowsDocumentOrderToNotificationState, nowsDocumentOrder);
        });
    }

    private void addAsCourtDocuments(final JsonEnvelope incomingEvent, final CreateNowsRequest createNowsRequest) {
        // GPE-6752 could also iterate resultLines to discover case and hearing associations
        // this is assuming that all cases in the hearing relate to this document
        final List<UUID> prosecutionIds = createNowsRequest.getHearing()
                .getProsecutionCases().stream()
                .map(ProsecutionCase::getId).collect(Collectors.toList());

        createNowsRequest.getNows().forEach(
                now -> {
                    final String orderName = createNowsRequest.getNowTypes().stream()
                            .filter(nowType -> nowType.getId().equals(now.getNowsTypeId()))
                            .map(NowType::getDescription).findFirst()
                            .orElse(now.getRequestedMaterials().get(0).getTemplateName());

                    final CourtDocument courtDocument = courtDocument(now, prosecutionIds, createNowsRequest.getHearing().getId(), orderName);

                    final JsonObject jsonObject = Json.createObjectBuilder()
                            .add("courtDocument", objectToJsonObjectConverter
                                    .convert(courtDocument)).build();
                    sender.send(enveloper.withMetadataFrom(incomingEvent, PROGRESSION_COMMAND_CREATE_COURT_DOCUMENT).apply(jsonObject));
                }
        );
    }

    @Handles("hearing.events.nows-material-status-updated")
    public void propagateNowsMaterialStatusUpdated(final JsonEnvelope envelope) {
        this.sender.send(this.enveloper.withMetadataFrom(envelope, "public.hearing.events.nows-material-status-updated")
                .apply(createObjectBuilder()
                        .add("materialId", envelope.payloadAsJsonObject().getJsonString("materialId"))
                        .build()
                ));
    }

    private final NowDocument nowDocument(final Now nows, final List<UUID> prosecutionCaseIds, final UUID hearingId) {
        return NowDocument.nowDocument()
                .withDefendantId(nows.getDefendantId())
                .withOrderHearingId(hearingId)
                .withProsecutionCases(prosecutionCaseIds)
                .build();
    }

    private final CourtDocument courtDocument(final Now nows, final List<UUID> prosecutionCaseIds, final UUID hearingId, final String orderName) {
        final NowDocument nowDocument = nowDocument(nows, prosecutionCaseIds, hearingId);
        return CourtDocument.courtDocument()
                .withCourtDocumentId(nows.getId())
                //this doesnt seem necessary
                //GPE-6752 this could be hard code to a nows document type
                .withDocumentTypeId(UUID.randomUUID())
                .withDocumentTypeDescription("Court Final orders")
                .withMaterials(
                        nows.getRequestedMaterials().stream().map(
                                variant -> Material.material()
                                        .withId(variant.getMaterialId())
                                        .withGenerationStatus(null)
                                        .withUserGroups(variant.getKey().getUsergroups())
                                        .withUploadDateTime(ZonedDateTime.now())
                                        .withName(orderName)
                                        .build()
                        ).collect(Collectors.toList())
                )
                .withDocumentCategory(
                        DocumentCategory.documentCategory()
                                .withNowDocument(nowDocument)
                                .build()
                )
                .withName(orderName)
                .build();
    }
}

