package uk.gov.moj.cpp.progression.domain.transformation;

import com.google.common.io.Resources;
import org.everit.json.schema.Schema;
import org.json.JSONObject;
import org.json.JSONTokener;
import org.slf4j.Logger;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.tools.eventsourcing.transformation.api.Action;
import uk.gov.justice.tools.eventsourcing.transformation.api.EventTransformation;
import uk.gov.justice.tools.eventsourcing.transformation.api.annotation.Transformation;
import uk.gov.moj.cpp.progression.domain.transformation.exception.TransformationException;
import uk.gov.moj.cpp.progression.domain.transformation.util.TransformationEventHelper;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.stream.Stream;

import static java.util.stream.Stream.of;
import static org.everit.json.schema.loader.SchemaLoader.load;
import static org.slf4j.LoggerFactory.getLogger;
import static uk.gov.justice.tools.eventsourcing.transformation.api.Action.NO_ACTION;
import static uk.gov.justice.tools.eventsourcing.transformation.api.Action.TRANSFORM;

@SuppressWarnings("squid:S2259")
@Transformation
public class ProgressionEventTransformer implements EventTransformation {

    private static final String EVENT_PROSECUTION_CASE_OFFENCE_UPDATED = "progression.event.prosecution-case-offences-updated";
    private static final String EVENT_PROSECUTION_CASE_DEFENDANT_LISTING_STATUS_CHANGED = "prosecutionCase-defendant-listing-status-changed";
    private static final String EVENT_PROGRESSION_EVENT_PROSECUTION_CASE_CREATED = "progression.event.prosecution-case-created";
    private static final String EVENT_PROGRESSION_EVENT_HEARING_RESULTED = "progression.event.hearing-resulted";

    private Enveloper enveloper;
    private static final Logger LOGGER = getLogger(ProgressionEventTransformer.class);

    @Override
    public Action actionFor(final JsonEnvelope event) {
        final String name = event.metadata().name();
        if (name.equalsIgnoreCase(EVENT_PROSECUTION_CASE_OFFENCE_UPDATED) ||
                name.equalsIgnoreCase(EVENT_PROSECUTION_CASE_DEFENDANT_LISTING_STATUS_CHANGED) ||
                name.equalsIgnoreCase(EVENT_PROGRESSION_EVENT_PROSECUTION_CASE_CREATED) ||
                name.equalsIgnoreCase(EVENT_PROGRESSION_EVENT_HEARING_RESULTED)
                ) {
            return TRANSFORM;
        }
        return NO_ACTION;
    }

    @Override
    public Stream<JsonEnvelope> apply(final JsonEnvelope event) {
        JsonEnvelope transformedEvent = null;
        final String name = event.metadata().name();
        if (name.equalsIgnoreCase(EVENT_PROSECUTION_CASE_OFFENCE_UPDATED)) {
            transformedEvent = new TransformationEventHelper().buildDefendantCaseOffencesTransformedPayload(event, EVENT_PROSECUTION_CASE_OFFENCE_UPDATED);
        } else if (name.equalsIgnoreCase(EVENT_PROSECUTION_CASE_DEFENDANT_LISTING_STATUS_CHANGED)) {
            transformedEvent = new TransformationEventHelper().buildListingStatusChangedTransformedPayload(event, EVENT_PROSECUTION_CASE_DEFENDANT_LISTING_STATUS_CHANGED);
        } else if (name.equalsIgnoreCase(EVENT_PROGRESSION_EVENT_PROSECUTION_CASE_CREATED)) {
            transformedEvent = new TransformationEventHelper().buildProsecutionCaseCreatedTransformedPayload(event, EVENT_PROGRESSION_EVENT_PROSECUTION_CASE_CREATED);
        }else if (name.equalsIgnoreCase(EVENT_PROGRESSION_EVENT_HEARING_RESULTED)) {
            transformedEvent = new TransformationEventHelper().buildHearingResultedTransformedPayload(event, EVENT_PROGRESSION_EVENT_HEARING_RESULTED);
        }
        final JsonEnvelope transformedEnvelope = enveloper.withMetadataFrom(event, transformedEvent.metadata().asJsonObject().getString("name")).apply(transformedEvent.payload());

        if (LOGGER.isInfoEnabled()) {
            LOGGER.info("TransformedEnvelope: {}", transformedEnvelope);
        }
        return of(transformedEnvelope);
    }

    public static void validateAgainstSchema(final String schemaFileName, final String jsonString) {
        final URL resource = Resources.getResource("raml/json/schema/" + schemaFileName);
        try (final InputStream inputStream = resource.openStream()) {
            final JSONObject rawSchema = new JSONObject(new JSONTokener(inputStream));
            final Schema schema = load(rawSchema);
            schema.validate(new JSONObject(jsonString));

        } catch (IOException e) {
            throw new TransformationException("Error validating payload against schema", e);
        }
    }

    @Override
    public void setEnveloper(final Enveloper enveloper) {
        this.enveloper = enveloper;
    }
}
