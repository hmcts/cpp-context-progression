package uk.gov.justice.api.resource.service;

import static java.time.LocalDate.parse;
import static java.util.Objects.nonNull;
import static java.util.UUID.randomUUID;
import static java.util.stream.Collectors.toList;
import static javax.json.Json.createObjectBuilder;
import static javax.json.JsonValue.EMPTY_JSON_OBJECT;
import static org.apache.commons.collections.CollectionUtils.isNotEmpty;
import static uk.gov.justice.services.core.annotation.Component.QUERY_API;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.justice.services.messaging.spi.DefaultJsonMetadata.metadataBuilder;

import uk.gov.justice.api.resource.dto.DraftResultsWrapper;
import uk.gov.justice.api.resource.dto.ResultLine;
import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.requester.Requester;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.messaging.Metadata;
import uk.gov.justice.services.messaging.MetadataBuilder;

import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import javax.inject.Inject;
import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonValue.ValueType;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HearingQueryService {
    public static final String HEARING_GET_DRAFT_RESULT_V2 = "hearing.get-draft-result-v2";
    public static final String RESULT_LINES = "resultLines";
    private static final String AMENDMENTS_LOG_KEY = "amendmentsLog";
    private static final Logger LOGGER = LoggerFactory.getLogger(HearingQueryService.class);
    private static final String METADATA_STR = "__metadata__";
    private static final String LAST_SHARED_TIME = "lastSharedTime";

    @Inject
    @ServiceComponent(QUERY_API)
    private Requester requester;

    @Inject
    private JsonObjectToObjectConverter jsonToObjectConverter;

    public List<DraftResultsWrapper> getDraftResultsWithAmendments(final UUID userId, final UUID hearingId, final List<LocalDate> hearingDayList) {

        final List<DraftResultsWrapper> amendedResultLineList = new ArrayList<>();
        hearingDayList.forEach(hearingDay -> {
            final JsonObject draftResults = getDraftResults(getHearingQueryJsonEnvelop(userId), hearingId, hearingDay);
            if (nonNull(draftResults) && draftResults.containsKey(RESULT_LINES)) {
                amendedResultLineList.add(filterResultsForValidAmendments(draftResults));
            }
        });

        return amendedResultLineList;
    }

    private DraftResultsWrapper filterResultsForValidAmendments(final JsonObject draftResults) {

        final UUID hearingId = UUID.fromString(draftResults.getString("hearingId"));
        final LocalDate hearingDay = parse(draftResults.getString("hearingDay"));
        ZonedDateTime lastSharedTime = null;
        if (draftResults.containsKey(METADATA_STR) && draftResults.get(METADATA_STR).asJsonObject().containsKey(LAST_SHARED_TIME)) {
            lastSharedTime = ZonedDateTime.parse(draftResults.get(METADATA_STR).asJsonObject().getString(LAST_SHARED_TIME));
        }

        final List<ResultLine> resultLines = draftResults.getJsonObject(RESULT_LINES).values().stream()
                .filter(jsonValue -> jsonValue.asJsonObject().containsKey(AMENDMENTS_LOG_KEY)
                        && jsonValue.asJsonObject().get(AMENDMENTS_LOG_KEY).getValueType() == ValueType.OBJECT)
                .map(jsonValue -> jsonToObjectConverter.convert(jsonValue.asJsonObject(), ResultLine.class))
                .filter(resultLine -> nonNull(resultLine.getAmendmentsLog()) && isNotEmpty(resultLine.getAmendmentsLog().getAmendmentsRecord()))
                .collect(toList());

        return new DraftResultsWrapper(hearingId, hearingDay, resultLines, lastSharedTime);
    }

    private JsonObject getDraftResults(final JsonEnvelope jsonEnvelope, final UUID hearingId, final LocalDate hearingDay) {
        final Metadata metadata = metadataWithNewActionName(jsonEnvelope.metadata(), HEARING_GET_DRAFT_RESULT_V2);
        final JsonObject jsonPayLoad = Json.createObjectBuilder()
                .add("hearingId", hearingId.toString())
                .add("hearingDay", hearingDay.toString())
                .build();

        final JsonEnvelope responseJson = requester.request(JsonEnvelope.envelopeFrom(metadata, jsonPayLoad));

        if (!responseJson.payloadIsNull()) {
            LOGGER.info("{} response payload {}", HEARING_GET_DRAFT_RESULT_V2, responseJson.toObfuscatedDebugString());
            return responseJson.payloadAsJsonObject();
        }

        return EMPTY_JSON_OBJECT;
    }

    private static Metadata metadataWithNewActionName(final Metadata metadata, final String actionName) {
        final MetadataBuilder metadataBuilder = Envelope.metadataBuilder().withId(UUID.randomUUID())
                .withName(actionName)
                .createdAt(ZonedDateTime.now())
                .withCausation(metadata.causation().toArray(new UUID[metadata.causation().size()]));

        metadata.clientCorrelationId().ifPresent(metadataBuilder::withClientCorrelationId);
        metadata.sessionId().ifPresent(metadataBuilder::withSessionId);
        metadata.streamId().ifPresent(metadataBuilder::withStreamId);
        metadata.userId().ifPresent(metadataBuilder::withUserId);
        metadata.version().ifPresent(metadataBuilder::withVersion);

        return metadataBuilder.build();
    }

    private JsonEnvelope getHearingQueryJsonEnvelop(final UUID userId) {
        return envelopeFrom(
                metadataBuilder()
                        .withId(randomUUID())
                        .withName(HEARING_GET_DRAFT_RESULT_V2)
                        .withUserId(userId.toString())
                        .build(),
                createObjectBuilder()
                        .build()
        );
    }
}
