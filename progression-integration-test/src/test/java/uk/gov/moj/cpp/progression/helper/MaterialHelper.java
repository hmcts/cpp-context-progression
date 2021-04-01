package uk.gov.moj.cpp.progression.helper;

import static java.util.UUID.randomUUID;
import static javax.json.Json.createObjectBuilder;
import static uk.gov.justice.services.messaging.Envelope.metadataFrom;
import static uk.gov.justice.services.messaging.JsonMetadata.ID;
import static uk.gov.justice.services.messaging.JsonMetadata.NAME;
import static uk.gov.moj.cpp.progression.helper.QueueUtil.publicEvents;
import static uk.gov.moj.cpp.progression.helper.QueueUtil.sendMessage;

import uk.gov.justice.services.messaging.JsonMetadata;
import uk.gov.justice.services.messaging.Metadata;

import java.util.UUID;

import javax.json.JsonObject;

public class MaterialHelper {

    private static final String ORIGINATOR = "originator";
    private static final String ORIGINATOR_VALUE = "court";

    public static void sendEventToConfirmMaterialAdded(final UUID materialId) {
        final JsonObject materialAddedPublicEventPayload = createObjectBuilder()
                .add("materialId", materialId.toString())
                .add("fileServiceId", randomUUID().toString())
                .build();

        final Metadata metadata = metadataFrom(createObjectBuilder()
                .add(ID, randomUUID().toString())
                .add(NAME, "material.material-added")
                .add(ORIGINATOR, ORIGINATOR_VALUE)
                .add("context", createObjectBuilder().add(JsonMetadata.USER_ID, randomUUID().toString()))
                .build()).build();

        sendMessage(publicEvents.createProducer(),
                "material.material-added",
                materialAddedPublicEventPayload,
                metadata);
    }
}
