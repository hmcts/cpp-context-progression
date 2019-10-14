package uk.gov.moj.cpp.progression.domain.transformation.ctl;

import org.slf4j.Logger;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.tools.eventsourcing.transformation.api.Action;
import uk.gov.justice.tools.eventsourcing.transformation.api.EventTransformation;
import uk.gov.justice.tools.eventsourcing.transformation.api.annotation.Transformation;

import javax.json.JsonObject;

import java.util.List;
import java.util.stream.Stream;

import static com.google.common.collect.Lists.newArrayList;
import static java.util.stream.Stream.of;
import static org.slf4j.LoggerFactory.getLogger;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.justice.services.messaging.JsonEnvelope.metadataFrom;
import static uk.gov.justice.tools.eventsourcing.transformation.api.Action.NO_ACTION;
import static uk.gov.justice.tools.eventsourcing.transformation.api.Action.TRANSFORM;



@Transformation
public class ProgressionEventTransformer implements EventTransformation {

    private Enveloper enveloper;

    private BailStatusEnum2ObjectTransformer bailStatusTransformer;

    private static final Logger LOGGER = getLogger(ProgressionEventTransformer.class);

    private static final String PROGRESSION_EVENT_HEARING_RESULTED = "progression.event.hearing-resulted";
    private static final String PROGRESSION_EVENT_COURT_PROCEEDINGS_INITIATED    = "progression.event.court-proceedings-initiated";
    private static final String PROGRESSION_EVENT_NOWS_REQUESTED = "progression.event.nows-requested";
    private static final String PROGRESSION_EVENT_PROSECUTION_CASE_DEFENDANT_UPDATED = "progression.event.prosecution-case-defendant-updated";
    private static final String PROGRESSION_EVENTS_DEFENDANT_UPDATED = "progression.events.defendant-updated";
    private static final String PROGRESSION_EVENT_SENDING_SHEET_COMPLETED = "progression.events.sending-sheet-completed";
    private static final String PROGRESSION_HEARING_INITIATE_ENRICHED = "progression.hearing-initiate-enriched";
    private static final String PROGRESSION_EVENT_PROSECUTION_CASE_DEFENDANT_LISTING_STATUS_CHANGED = "progression.event.prosecutionCase-defendant-listing-status-changed";
    private static final String PROGRESSION_EVENT_PROSECUTION_CASE_CREATED = "progression.event.prosecution-case-created";
    private static final String PROGRESSION_EVENT_HEARING_APPLICATION_LINK_CREATED = "progression.event.hearing-application-link-created";
    private static final String PROGRESSION_EVENT_COURT_APPLICATION_ADDED_TO_CASE = "progression.event.court-application-added-to-case";
    private static final String PROGRESSION_EVENT_APPLICATION_REFERRED_TO_COURT = "progression.event.application-referred-to-court";
    private static final String PROGRESSION_EVENT_COURT_APPLICATION_CREATED = "progression.event.court-application-created";
    private static final String PROGRESSION_EVENT_COURT_APPLICATION_UPDATED = "progression.event.court-application-updated";

    private static final String PROGRESSION_EVENT_LISTED_COURT_APPLICATION_CHANGED = "progression.event.listed-court-application-changed";
    private static final String PROGRESSION_EVENT_HEARING_EXTENDED = "progression.event.hearing-extended";


    protected final static  List<String> eventsToTransform = newArrayList(
            PROGRESSION_EVENT_APPLICATION_REFERRED_TO_COURT,
            PROGRESSION_EVENT_COURT_APPLICATION_ADDED_TO_CASE,
            PROGRESSION_EVENT_COURT_APPLICATION_CREATED,
            PROGRESSION_EVENT_COURT_APPLICATION_UPDATED,
            PROGRESSION_EVENT_COURT_PROCEEDINGS_INITIATED,
            PROGRESSION_EVENTS_DEFENDANT_UPDATED,
            PROGRESSION_EVENT_HEARING_APPLICATION_LINK_CREATED,
            PROGRESSION_EVENT_HEARING_EXTENDED,
            PROGRESSION_EVENT_HEARING_RESULTED,
            PROGRESSION_HEARING_INITIATE_ENRICHED,
            PROGRESSION_EVENT_LISTED_COURT_APPLICATION_CHANGED,
            PROGRESSION_EVENT_NOWS_REQUESTED,
            PROGRESSION_EVENT_PROSECUTION_CASE_DEFENDANT_UPDATED,
            PROGRESSION_EVENT_PROSECUTION_CASE_CREATED,
            PROGRESSION_EVENT_PROSECUTION_CASE_DEFENDANT_LISTING_STATUS_CHANGED,
            PROGRESSION_EVENT_SENDING_SHEET_COMPLETED
    );

    public ProgressionEventTransformer() {
        bailStatusTransformer = new BailStatusEnum2ObjectTransformer();
    }

    @Override
    public Action actionFor(final JsonEnvelope event) {
        if (eventsToTransform.stream().anyMatch(eventToTransform -> event.metadata().name().equalsIgnoreCase(eventToTransform))) {
            return TRANSFORM;
        }

        return NO_ACTION;
    }

    @Override
    public Stream<JsonEnvelope> apply(final JsonEnvelope event) {

        final JsonObject payload = event.payloadAsJsonObject();

        if (LOGGER.isInfoEnabled()) {
            LOGGER.info("----------------------event name------------ {}", event.metadata().name());
        }

        final JsonObject transformedPayload = bailStatusTransformer.transform(payload);
        if (LOGGER.isInfoEnabled()) {
            LOGGER.info("-------------------transformedPayload---------------{} ", transformedPayload);
        }

        return of(envelopeFrom(metadataFrom(event.metadata()), transformedPayload));

    }

    @Override
    public void setEnveloper(final Enveloper enveloper) {
        this.enveloper = enveloper;
    }


    public void setBailStatusTransformer(final BailStatusEnum2ObjectTransformer value) {
        this.bailStatusTransformer = value;
    }



}
