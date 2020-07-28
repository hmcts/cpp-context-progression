package uk.gov.moj.cpp.progression.domain.transformation.prosecutionCaseDdefendantListingStatusChanged;

import static java.util.stream.Stream.of;
import static org.slf4j.LoggerFactory.getLogger;
import static uk.gov.justice.tools.eventsourcing.transformation.api.Action.NO_ACTION;
import static uk.gov.justice.tools.eventsourcing.transformation.api.Action.TRANSFORM;

import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.messaging.JsonObjects;
import uk.gov.justice.tools.eventsourcing.transformation.api.Action;
import uk.gov.justice.tools.eventsourcing.transformation.api.EventTransformation;
import uk.gov.justice.tools.eventsourcing.transformation.api.annotation.Transformation;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import javax.json.JsonObject;

import org.slf4j.Logger;

@Transformation
public class    ProsecutionCaseDefendantListingStatusChangedTransformer implements EventTransformation {
    private static final Logger LOGGER = getLogger(ProsecutionCaseDefendantListingStatusChangedTransformer.class);
    public static final String PROGRESSION_CASE_DEFENDANT_LISTING_STATUS_CHANGED = "progression.event.prosecutionCase-defendant-listing-status-changed";
    public static final String HEARING_LISTING_STATUS_KEY = "hearingListingStatus";
    public static final String HEARING_LISTING_STATUS_VALUE = "HEARING_RESULTED";
    public static final String HEARING_KEY = "hearing";
    public static final String STREAM_ID_KEY = "id";

    private List<String> streamIdsToBeTransformed = new ArrayList();

    public ProsecutionCaseDefendantListingStatusChangedTransformer() {
        streamIdsToBeTransformed.add("81dff79a-da70-4919-8eb1-aeeb1306f7df");
        streamIdsToBeTransformed.add("496dabe7-b8ed-4c97-a2be-ff94efb30d27");
        streamIdsToBeTransformed.add("248783d7-78e1-4555-a2f1-259e964429ca");
        streamIdsToBeTransformed.add("bb49bd43-ce6f-4948-9c06-34c6171b453f");
        streamIdsToBeTransformed.add("99c672cc-ffe4-4004-a20e-a1051db5409a");
        streamIdsToBeTransformed.add("39c40e04-1d4d-4de8-8321-e27a8b877991");
        streamIdsToBeTransformed.add("b0876535-5e4a-4126-92fd-2a86c7029338");
        streamIdsToBeTransformed.add("3a948aa3-65ae-40eb-b8ef-b440d4ed803d");
        streamIdsToBeTransformed.add("4be2d0f3-0790-4a10-a820-db18431f19e7");
        streamIdsToBeTransformed.add("420ac905-6ab7-478e-97ca-6a6a2663f0da");
        streamIdsToBeTransformed.add("88cb11b2-05c8-4ded-a307-fe9b8a697142");
        streamIdsToBeTransformed.add("2af70e0e-ae88-4d5a-a7c7-5f15417d3f63");
        streamIdsToBeTransformed.add("f216deea-ce6b-4d9e-908f-45f8889fdd23");
        streamIdsToBeTransformed.add("fc3de32c-d0cc-4ee6-a93c-0084c8a1554e");
        streamIdsToBeTransformed.add("18c7b87e-2de5-4cd9-949f-a82078690fd3");
        streamIdsToBeTransformed.add("057b40eb-98e7-4dbd-ac4f-c7d82f7d487c");
        streamIdsToBeTransformed.add("768cfc5c-0084-41c8-8bd3-71c3efe82115");
        streamIdsToBeTransformed.add("9f2cff01-d708-4cf3-b68a-19aaa4ccaee5");
        streamIdsToBeTransformed.add("b8c2db6b-30a8-45b8-afaa-437a1d1433c5");
        streamIdsToBeTransformed.add("39195a7b-050a-4dd2-826a-d510bf4f0a14");
        streamIdsToBeTransformed.add("a7831869-f2c2-4191-a5b2-04990ebc095c");
        streamIdsToBeTransformed.add("edb0d703-71aa-4b6f-816b-c53621d84a14");
        streamIdsToBeTransformed.add("7b39a209-19c2-476f-ad7c-6bb7f683c27b");
        streamIdsToBeTransformed.add("bd56f6fa-824c-4dd9-9eb3-027a5b19d6eb");
        streamIdsToBeTransformed.add("d7cb84dd-cbcc-4f3c-a620-e6c120948284");
        streamIdsToBeTransformed.add("0a97ef8f-64a1-4d3a-a26a-b22cc46e81b5");
        streamIdsToBeTransformed.add("3db4eaa8-8c8e-410a-b027-c90ee89f3475");
        streamIdsToBeTransformed.add("5e6cbb46-1cca-4d67-bc35-399cc560370d");
        streamIdsToBeTransformed.add("4aff8978-fb52-4816-a33f-2b0cd20c0884");
        streamIdsToBeTransformed.add("f486d8b9-85af-4eb2-a9df-2df87564f354");
        streamIdsToBeTransformed.add("3da22949-9d6f-4511-82af-ae55ec95995f");
        streamIdsToBeTransformed.add("5e1d7237-3af8-421f-a7ce-d2990c174c21");
        streamIdsToBeTransformed.add("5b2e672f-6e9c-4445-9eec-06dea46c4640");
        streamIdsToBeTransformed.add("99f72110-4e98-4f12-b711-d7a76e4828d6");
        streamIdsToBeTransformed.add("a0483af4-eba1-490a-ba23-0d48dee2ebc9");
        streamIdsToBeTransformed.add("bd4fcb32-bd27-43fe-a443-8d7f307a6bca");
    }

    @Override
    public Action actionFor(final JsonEnvelope eventEnvelope) {
        final JsonObject payloadAsJsonObject = eventEnvelope.payloadAsJsonObject();
        final JsonObject hearing = payloadAsJsonObject.getJsonObject(HEARING_KEY);
        if (hearing != null && hearing.get(STREAM_ID_KEY) != null) {
            final String id = hearing.getString(STREAM_ID_KEY);
            if (isProsecutionCaseDdefendantListingStatusChanged(eventEnvelope) && eventRequiresTransformation(payloadAsJsonObject, id)) {
                return TRANSFORM;
            }
        }
        return NO_ACTION;
    }


    @Override
    @SuppressWarnings("squid:S2250")
    public Stream<JsonEnvelope> apply(final JsonEnvelope eventEnvelope) {
        final JsonObject payload = eventEnvelope.payloadAsJsonObject();
        final JsonObject hearing = payload.getJsonObject(HEARING_KEY);

        final String hearingId = hearing.getString(STREAM_ID_KEY);
        if (null == hearingId) {
            if (LOGGER.isWarnEnabled()) {
                LOGGER.warn("Error condition no hearingId for the case ");
            }
            return of(eventEnvelope);
        }

        if (!eventRequiresTransformation(payload, hearingId)) {
            return of(eventEnvelope);
        }

        if (streamIdsToBeTransformed.contains(hearingId)) {

            LOGGER.debug("Transforming event with hearingId {}", hearingId);
            final JsonObject eventPayload = eventEnvelope.payloadAsJsonObject();
            final JsonObject hearingPayload = JsonObjects.createObjectBuilder(eventPayload)
                    .add(HEARING_LISTING_STATUS_KEY, HEARING_LISTING_STATUS_VALUE)
                    .build();
            final JsonEnvelope transformedEnvelope = JsonEnvelope.envelopeFrom(
                    eventEnvelope.metadata(), hearingPayload);
            LOGGER.info("Transforming hearingId: {} ", hearingId);
            return of(transformedEnvelope);
        }
        return of(eventEnvelope);

    }

    @Override
    public void setEnveloper(final Enveloper enveloper) {
        //Not used
    }

    private boolean isProsecutionCaseDdefendantListingStatusChanged(final JsonEnvelope eventEnvelope) {
        return PROGRESSION_CASE_DEFENDANT_LISTING_STATUS_CHANGED.equalsIgnoreCase(eventEnvelope.metadata().name());
    }

    @SuppressWarnings("squid:S2250")
    private boolean eventRequiresTransformation(JsonObject payloadAsJsonObject, String id) {
        return streamIdsToBeTransformed.contains(id) &&
                !payloadAsJsonObject.containsKey(HEARING_LISTING_STATUS_KEY);
    }

}
