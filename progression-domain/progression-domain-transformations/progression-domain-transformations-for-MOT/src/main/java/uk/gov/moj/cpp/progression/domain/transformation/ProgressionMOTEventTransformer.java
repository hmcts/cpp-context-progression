package uk.gov.moj.cpp.progression.domain.transformation;

import static java.util.stream.Stream.of;
import static org.slf4j.LoggerFactory.getLogger;
import static uk.gov.justice.tools.eventsourcing.transformation.api.Action.NO_ACTION;
import static uk.gov.justice.tools.eventsourcing.transformation.api.Action.TRANSFORM;

import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.tools.eventsourcing.transformation.api.Action;
import uk.gov.justice.tools.eventsourcing.transformation.api.EventTransformation;
import uk.gov.justice.tools.eventsourcing.transformation.api.annotation.Transformation;
import uk.gov.moj.cpp.progression.domain.transformation.util.TransformationEventHelper;

import java.util.stream.Stream;

import org.slf4j.Logger;

@SuppressWarnings("squid:S2259")
@Transformation
public class ProgressionMOTEventTransformer implements EventTransformation {

    private static final String EVENT_PROGRESSION_HEARING_RESULTED = "progression.event.hearing-resulted";
    private static final String EVENT_PROGRESSION_OFFENCES_FOR_DEFENDANT_UPDATED = "progression.events.offences-for-defendant-updated";
    private static final String EVENT_PROGRESSION_COURT_APPLICATION_UPDATED = "progression.event.court-application-updated";
    private static final String EVENT_PROGRESSION_SENDING_SHEET_COMPLETED = "progression.events.sending-sheet-completed";
    private static final String EVENT_PROGRESSION_HEARING_EXTENDED = "progression.event.hearing-extended";
    private static final String EVENT_PROGRESSION_APPLICATION_REFERRED_TO_COURT = "progression.event.application-referred-to-court";
    private static final String PUBLIC_EVENT_PROGRESSION_SENDING_SHEET_COMPLETED = "public.progression.events.sending-sheet-completed";
    private static final String EVENT_PUBLIC_HEARING_RESULTED = "public.hearing.resulted";
    private static final String EVENT_PROGRESSION_COURT_APPLICATION_CREATED = "progression.event.court-application-created";
    public static final String PROGRESSION_HEARING_INITIATE_ENRICHED = "progression.hearing-initiate-enriched";
    public static final String PROGRESSION_EVENT_PROSECUTION_CASE_DEFENDANT_LISTING_STATUS_CHANGED = "progression.event.prosecutionCase-defendant-listing-status-changed";
    public static final String PROGRESSION_EVENTS_OFFENCES_FOR_DEFENDANT_CHANGED = "progression.events.offences-for-defendant-changed";
    public static final String PROGRESSION_EVENT_PROSECUTION_CASE_CREATED = "progression.event.prosecution-case-created";
    public static final String PROGRESSION_EVENT_HEARING_RESULTED = "progression.event.hearing-resulted";
    public static final String PROGRESSION_EVENT_HEARING_APPLICATION_LINK_CREATED = "progression.event.hearing-application-link-created";
    public static final String PROGRESSION_EVENT_PROSECUTION_CASE_OFFENCES_UPDATED = "progression.event.prosecution-case-offences-updated";
    public static final String PROGRESSION_EVENT_CASES_REFERRED_TO_COURT = "progression.event.cases-referred-to-court";
    public static final String PROGRESSION_EVENT_COURT_PROCEEDINGS_INITIATED = "progression.event.court-proceedings-initiated";

    private Enveloper enveloper;
    private static final Logger LOGGER = getLogger(ProgressionMOTEventTransformer.class);

    @SuppressWarnings({"squid:S1067","squid:MethodCyclomaticComplexity"})
    @Override
    public Action actionFor(final JsonEnvelope event) {
        final String name = event.metadata().name();
        if (name.equalsIgnoreCase(EVENT_PROGRESSION_HEARING_RESULTED) ||
                name.equalsIgnoreCase(EVENT_PROGRESSION_SENDING_SHEET_COMPLETED)||
                name.equalsIgnoreCase(EVENT_PROGRESSION_COURT_APPLICATION_UPDATED)||
                name.equalsIgnoreCase(EVENT_PROGRESSION_OFFENCES_FOR_DEFENDANT_UPDATED)||
                name.equalsIgnoreCase(EVENT_PROGRESSION_HEARING_EXTENDED)||
                name.equalsIgnoreCase(EVENT_PROGRESSION_APPLICATION_REFERRED_TO_COURT)||
                name.equalsIgnoreCase(PUBLIC_EVENT_PROGRESSION_SENDING_SHEET_COMPLETED)||
                name.equalsIgnoreCase(EVENT_PUBLIC_HEARING_RESULTED)||
                name.equalsIgnoreCase(EVENT_PROGRESSION_COURT_APPLICATION_CREATED) ||
                name.equalsIgnoreCase(PROGRESSION_HEARING_INITIATE_ENRICHED)||
                name.equalsIgnoreCase(PROGRESSION_EVENT_PROSECUTION_CASE_DEFENDANT_LISTING_STATUS_CHANGED)||
                name.equalsIgnoreCase(PROGRESSION_EVENTS_OFFENCES_FOR_DEFENDANT_CHANGED)||
                name.equalsIgnoreCase(PROGRESSION_EVENT_CASES_REFERRED_TO_COURT)||
                name.equalsIgnoreCase(PROGRESSION_EVENT_COURT_PROCEEDINGS_INITIATED)||
                name.equalsIgnoreCase(PROGRESSION_EVENT_PROSECUTION_CASE_CREATED)||
                name.equalsIgnoreCase(PROGRESSION_EVENT_HEARING_RESULTED)||
               // name.equalsIgnoreCase(PROGRESSION_EVENT_HEARING_DEFENDANT_REQUEST_CREATED)||
                name.equalsIgnoreCase(PROGRESSION_EVENT_HEARING_APPLICATION_LINK_CREATED)||
               // name.equalsIgnoreCase(PROGRESSION_EVENT_SUMMONS_DATA_PREPARED)||
                name.equalsIgnoreCase(PROGRESSION_EVENT_PROSECUTION_CASE_OFFENCES_UPDATED)) {
            return TRANSFORM;
        }
        return NO_ACTION;
    }

    @SuppressWarnings({"squid:S1067","squid:S3776","squid:MethodCyclomaticComplexity"})
    @Override
    public Stream<JsonEnvelope> apply(final JsonEnvelope event) {
        JsonEnvelope transformedEvent = null;
        final String name = event.metadata().name();
        if (name.equalsIgnoreCase(EVENT_PROGRESSION_HEARING_RESULTED)) {
            transformedEvent = new TransformationEventHelper().buildTransformedPayloadForHearing(event, EVENT_PROGRESSION_HEARING_RESULTED);
        } else if (name.equalsIgnoreCase(EVENT_PROGRESSION_OFFENCES_FOR_DEFENDANT_UPDATED)) {
            transformedEvent = new TransformationEventHelper().buildOffencesForDefendentPayload(event, EVENT_PROGRESSION_OFFENCES_FOR_DEFENDANT_UPDATED);
        } else if (name.equalsIgnoreCase(EVENT_PROGRESSION_COURT_APPLICATION_UPDATED)) {
            transformedEvent = new TransformationEventHelper().buildCourtApplicationPayLoad(event, EVENT_PROGRESSION_COURT_APPLICATION_UPDATED);
        } else if (name.equalsIgnoreCase(EVENT_PROGRESSION_SENDING_SHEET_COMPLETED)) {
            transformedEvent = new TransformationEventHelper().buildSendingSheetPayLoad(event, EVENT_PROGRESSION_SENDING_SHEET_COMPLETED);
        } else if (name.equalsIgnoreCase(EVENT_PROGRESSION_HEARING_EXTENDED)) {
            transformedEvent = new TransformationEventHelper().buildHearingRequestPayload(event, EVENT_PROGRESSION_HEARING_EXTENDED);
        } else if (name.equalsIgnoreCase(EVENT_PROGRESSION_APPLICATION_REFERRED_TO_COURT)) {
            transformedEvent = new TransformationEventHelper().buildHearingRequestPayload(event, EVENT_PROGRESSION_APPLICATION_REFERRED_TO_COURT);
        } else if (name.equalsIgnoreCase(PUBLIC_EVENT_PROGRESSION_SENDING_SHEET_COMPLETED)) {
            transformedEvent = new TransformationEventHelper().buildSendingSheetPayLoad(event, PUBLIC_EVENT_PROGRESSION_SENDING_SHEET_COMPLETED);
        } else if (name.equalsIgnoreCase(EVENT_PUBLIC_HEARING_RESULTED)) {
            transformedEvent = new TransformationEventHelper().buildPublicHearingPayload(event, EVENT_PUBLIC_HEARING_RESULTED);
        } else if (name.equalsIgnoreCase(EVENT_PROGRESSION_COURT_APPLICATION_CREATED)) {
            transformedEvent = new TransformationEventHelper().buildCourtApplicationPayload(event, EVENT_PROGRESSION_COURT_APPLICATION_CREATED);
        } else if (name.equalsIgnoreCase(PROGRESSION_HEARING_INITIATE_ENRICHED)) {
            transformedEvent = new TransformationEventHelper().buildHearingPayload(event, PROGRESSION_HEARING_INITIATE_ENRICHED);
        } else if (name.equalsIgnoreCase(PROGRESSION_EVENTS_OFFENCES_FOR_DEFENDANT_CHANGED)) {
            transformedEvent = new TransformationEventHelper().buildOffencesForDefendebtPayload(event, PROGRESSION_EVENTS_OFFENCES_FOR_DEFENDANT_CHANGED);
        } else if (name.equalsIgnoreCase(PROGRESSION_EVENT_PROSECUTION_CASE_DEFENDANT_LISTING_STATUS_CHANGED)) {
            transformedEvent = new TransformationEventHelper().buildHearingListingStatusPayload(event, PROGRESSION_EVENT_PROSECUTION_CASE_DEFENDANT_LISTING_STATUS_CHANGED);
        }else if (name.equalsIgnoreCase(PROGRESSION_EVENT_PROSECUTION_CASE_CREATED)) {
            transformedEvent = new TransformationEventHelper().buildProsecutionCasePayload(event, PROGRESSION_EVENT_PROSECUTION_CASE_CREATED);
        }else if (name.equalsIgnoreCase(PROGRESSION_EVENT_HEARING_RESULTED)) {
            transformedEvent = new TransformationEventHelper().buildPublicHearingPayload(event, PROGRESSION_EVENT_HEARING_RESULTED);
        }else if (name.equalsIgnoreCase(PROGRESSION_EVENT_HEARING_APPLICATION_LINK_CREATED)) {
            transformedEvent = new TransformationEventHelper().buildHearingListingStatusPayload(event, PROGRESSION_EVENT_HEARING_APPLICATION_LINK_CREATED);
        }else if (name.equalsIgnoreCase(PROGRESSION_EVENT_PROSECUTION_CASE_OFFENCES_UPDATED)) {
            transformedEvent = new TransformationEventHelper().buildProsecutionCaseOffencesPayload(event, PROGRESSION_EVENT_PROSECUTION_CASE_OFFENCES_UPDATED);
        }else if (name.equalsIgnoreCase(PROGRESSION_EVENT_CASES_REFERRED_TO_COURT)) {
            transformedEvent = new TransformationEventHelper().buildCaseReferToCourtPayload(event, PROGRESSION_EVENT_CASES_REFERRED_TO_COURT);
        }else if (name.equalsIgnoreCase(PROGRESSION_EVENT_COURT_PROCEEDINGS_INITIATED)) {
            transformedEvent = new TransformationEventHelper().buildCourtProceedingPayload(event, PROGRESSION_EVENT_COURT_PROCEEDINGS_INITIATED);
        }

            final JsonEnvelope transformedEnvelope = enveloper.withMetadataFrom(event, transformedEvent.metadata().asJsonObject().getString("name")).apply(transformedEvent.payload());

            if (LOGGER.isInfoEnabled()) {
                LOGGER.info("TransformedEnvelope: {}", transformedEnvelope);
            }

            return of(transformedEnvelope);
            }


    @Override
    public void setEnveloper(final Enveloper enveloper) {
        this.enveloper = enveloper;
    }//
}
