package uk.gov.moj.cpp.progression.processor;

import static java.util.UUID.fromString;
import static javax.json.Json.createObjectBuilder;
import static uk.gov.justice.services.core.annotation.Component.EVENT_PROCESSOR;

import uk.gov.justice.core.courts.CourtDocument;
import uk.gov.justice.core.courts.DocumentCategory;
import uk.gov.justice.core.courts.Material;
import uk.gov.justice.core.courts.NowDocument;
import uk.gov.justice.core.courts.nowdocument.NowDocumentRequest;
import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.common.converter.ObjectToJsonObjectConverter;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.progression.service.DocumentGeneratorService;
import uk.gov.moj.cpp.progression.service.ReferenceDataService;

import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.inject.Inject;
import javax.json.Json;
import javax.json.JsonObject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ServiceComponent(EVENT_PROCESSOR)
@SuppressWarnings("squid:S00112")
public class NowsRequestedEventProcessor {

    public static final UUID NOW_DOCUMENT_TYPE_ID = UUID.fromString("460fbc00-c002-11e8-a355-529269fb1459");

    protected static final String PROGRESSION_COMMAND_CREATE_COURT_DOCUMENT = "progression.command.create-court-document";
    private static final Logger LOGGER = LoggerFactory.getLogger(NowsRequestedEventProcessor.class);
    private final Enveloper enveloper;
    private final Sender sender;
    private final JsonObjectToObjectConverter jsonObjectToObjectConverter;
    private final ObjectToJsonObjectConverter objectToJsonObjectConverter;

    private DocumentGeneratorService documentGeneratorService;

    @Inject
    private ReferenceDataService refDataService;

    @Inject
    public NowsRequestedEventProcessor(final Enveloper enveloper, final Sender sender,
                                       final DocumentGeneratorService documentGeneratorService,
                                       final JsonObjectToObjectConverter jsonObjectToObjectConverter,
                                       final ObjectToJsonObjectConverter objectToJsonObjectConverter,
                                       final ReferenceDataService refDataService
    ) {
        this.enveloper = enveloper;
        this.sender = sender;
        this.jsonObjectToObjectConverter = jsonObjectToObjectConverter;
        this.objectToJsonObjectConverter = objectToJsonObjectConverter;
        this.documentGeneratorService = documentGeneratorService;
        this.refDataService = refDataService;
    }

    //handle the public now document request from the hearing service
    @Handles("public.hearing.now-document-requested")
    public void processPublicNowDocumentRequested(final JsonEnvelope event) {
        final UUID userId = fromString(event.metadata().userId().orElseThrow(() -> new RuntimeException("UserId missing from event.")));

        final JsonObject requestJson = event.payloadAsJsonObject();
        final NowDocumentRequest nowDocumentRequest = jsonObjectToObjectConverter.convert(requestJson, NowDocumentRequest.class);

        if (LOGGER.isInfoEnabled()) {
            LOGGER.info("Nows requested payload - {}", requestJson);
        }
        addAsCourtDocuments(event, nowDocumentRequest);
        documentGeneratorService.generateNow(sender, event, userId, nowDocumentRequest);
    }

    private void addAsCourtDocuments(final JsonEnvelope incomingEvent, final NowDocumentRequest nowDocumentRequest) {
        // GPE-6752 could also iterate resultLines to discover case and hearing associations
        // this is assuming that all cases in the hearing relate to this document
        final List<UUID> prosecutionIds = Arrays.asList(nowDocumentRequest.getCaseId());

        final String orderName = nowDocumentRequest.getNowContent().getOrderName();

        final Optional<JsonObject> documentTypeData = refDataService.getDocumentTypeData(NOW_DOCUMENT_TYPE_ID, incomingEvent);
        if (!documentTypeData.isPresent()) {
            throw new RuntimeException("failed to look up nows document type " + NOW_DOCUMENT_TYPE_ID);
        }
        if (!documentTypeData.get().containsKey("documentAccess")) {
            throw new RuntimeException("no document access specified for  nows document type " + NOW_DOCUMENT_TYPE_ID);
        }
        final List<String> permittedGroups = Stream.concat(documentTypeData.get().getJsonArray("documentAccess").stream()
                .map(el -> el.toString().replace("\"", "")), nowDocumentRequest.getUsergroups().stream())
                .collect(Collectors.toList());

        final CourtDocument courtDocument =
                courtDocument(nowDocumentRequest, prosecutionIds, orderName, permittedGroups);

        final JsonObject jsonObject = Json.createObjectBuilder()
                .add("courtDocument", objectToJsonObjectConverter
                        .convert(courtDocument)).build();
        sender.send(enveloper.withMetadataFrom(incomingEvent, PROGRESSION_COMMAND_CREATE_COURT_DOCUMENT).apply(jsonObject));
    }

    @Handles("hearing.events.nows-material-status-updated")
    public void propagateNowsMaterialStatusUpdated(final JsonEnvelope envelope) {
        this.sender.send(this.enveloper.withMetadataFrom(envelope, "public.hearing.events.nows-material-status-updated")
                .apply(createObjectBuilder()
                        .add("materialId", envelope.payloadAsJsonObject().getJsonString("materialId"))
                        .build()
                ));
    }

    private NowDocument nowDocument(final UUID defendantId, final List<UUID> prosecutionCaseIds, final UUID hearingId) {
        return NowDocument.nowDocument()
                .withDefendantId(defendantId)
                .withOrderHearingId(hearingId)
                .withProsecutionCases(prosecutionCaseIds)
                .build();
    }

    private CourtDocument courtDocument(final NowDocumentRequest nowDocumentRequest, final List<UUID> prosecutionCaseIds, final String orderName,
                                        List<String> permittedGroups) {
        final NowDocument nowDocument = nowDocument(nowDocumentRequest.getDefendantId(), prosecutionCaseIds, nowDocumentRequest.getHearingId());
        return CourtDocument.courtDocument()
                .withCourtDocumentId(UUID.randomUUID())
                .withDocumentTypeId(UUID.randomUUID())
                .withDocumentTypeDescription("Court Final orders")
                .withMaterials(Arrays.asList(Material.material()
                        .withId(nowDocumentRequest.getMaterialId())
                        .withGenerationStatus(null)
                        .withUserGroups(permittedGroups)
                        .withUploadDateTime(ZonedDateTime.now())
                        .withName(orderName)
                        .build())
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

