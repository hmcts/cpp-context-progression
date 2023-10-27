package uk.gov.moj.cpp.progression.processor;

import static java.lang.String.CASE_INSENSITIVE_ORDER;
import static java.lang.String.format;
import static java.util.Objects.isNull;
import static uk.gov.justice.listing.courts.PublishCourtListType.DRAFT;
import static uk.gov.justice.listing.courts.PublishCourtListType.FINAL;
import static uk.gov.justice.listing.courts.PublishCourtListType.FIRM;
import static uk.gov.justice.listing.courts.PublishCourtListType.WARN;
import static uk.gov.justice.services.core.annotation.Component.EVENT_PROCESSOR;
import static uk.gov.justice.services.core.enveloper.Enveloper.envelop;

import uk.gov.justice.listing.courts.CourtListPublished;
import uk.gov.justice.listing.courts.PublishCourtListType;
import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.progression.service.PublishCourtListNotificationService;
import uk.gov.moj.cpp.progression.service.PublishCourtListPayloadBuilderService;
import uk.gov.moj.cpp.progression.service.payloads.PublishCourtListPayload;
import uk.gov.moj.cpp.progression.service.payloads.PublishCourtListPayload.PublishCourtListPayloadBuilder;

import java.util.Map;
import java.util.TreeMap;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ServiceComponent(EVENT_PROCESSOR)
public class PublishCourtListEventProcessor {

    private static final Logger LOGGER = LoggerFactory.getLogger(PublishCourtListEventProcessor.class.getName());

    private static final String PUBLISH_DRAFT_OR_FINAL_COURT_LIST_FOR_SINGLE_HEARING = "PublishDraftOrFinalCourtListForSingleHearing";
    private static final String PUBLISH_FIRM_COURT_LIST_FOR_SINGLE_HEARING = "PublishFirmCourtListForSingleHearing";
    private static final String PUBLISH_WARN_COURT_LIST_FOR_SINGLE_HEARING = "PublishWarnCourtListForSingleHearing";
    private static final String PUBLISH_DRAFT_OR_FINAL_COURT_LIST_FOR_MULTIPLE_HEARING = "PublishDraftOrFinalCourtListForMultipleHearing";
    private static final String PUBLISH_FIRM_COURT_LIST_FOR_MULTIPLE_HEARING = "PublishFirmCourtListForMultipleHearing";
    private static final String PUBLISH_WARN_COURT_LIST_FOR_MULTIPLE_HEARING = "PublishWarnCourtListForMultipleHearing";

    @Inject
    private PublishCourtListPayloadBuilderService publishCourtListPayloadBuilderService;
    @Inject
    private PublishCourtListNotificationService publishCourtListNotificationService;
    @Inject
    private JsonObjectToObjectConverter jsonObjectToObjectConverter;
    @Inject
    @ServiceComponent(EVENT_PROCESSOR)
    private Sender sender;

    @Handles("public.listing.court-list-published")
    public void processListingCourtListPublished(final JsonEnvelope event) {
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Received 'public.listing.court-list-published' event with payload: {}", event.toObfuscatedDebugString());
        }
        sender.send(envelop(event.payloadAsJsonObject()).withName("progression.command.publish-court-list").withMetadataFrom(event));
    }

    @Handles("progression.event.court-list-published")
    public void processCourtListPublished(final JsonEnvelope event) {
        final CourtListPublished courtListPublished = jsonObjectToObjectConverter.convert(event.payloadAsJsonObject(), CourtListPublished.class);
        final Boolean sendNotificationToParties = courtListPublished.getSendNotificationToParties();

        if(isNull(sendNotificationToParties) || Boolean.FALSE.equals(sendNotificationToParties)) {
            LOGGER.info("skipping notification sending to parties since notification flag is null or false");
            return;
        }

        final Map<String, PublishCourtListPayloadBuilder> defenceOrganisationPayloadBuilderByName = new TreeMap<>(CASE_INSENSITIVE_ORDER);
        final Map<String, PublishCourtListPayloadBuilder> defenceAdvocatePayloadBuilderByName = new TreeMap<>(CASE_INSENSITIVE_ORDER);
        final Map<String, PublishCourtListPayloadBuilder> prosecutionPayloadBuilderByName = new TreeMap<>(CASE_INSENSITIVE_ORDER);

        publishCourtListPayloadBuilderService.buildPayloadForInterestedParties(event, courtListPublished, defenceOrganisationPayloadBuilderByName, prosecutionPayloadBuilderByName, defenceAdvocatePayloadBuilderByName);

        sendNotificationToInterestedParties(event, defenceOrganisationPayloadBuilderByName);
        sendNotificationToInterestedParties(event, defenceAdvocatePayloadBuilderByName);
        sendNotificationToInterestedParties(event, prosecutionPayloadBuilderByName);
    }

    private void sendNotificationToInterestedParties(final JsonEnvelope event, final Map<String, PublishCourtListPayloadBuilder> publishCourtListPayloadBuilderByName) {
        publishCourtListPayloadBuilderByName.values().forEach(documentPayloadBuilder -> {
            final PublishCourtListPayload publishCourtListPayload = documentPayloadBuilder.build();
            final String documentTemplateName = getDocumentTemplateName(publishCourtListPayload.getPublishCourtListType(),
                    publishCourtListPayload.getHearingByDates().size() + (isNull(publishCourtListPayload.getHearingByWeekCommencingDate()) ? 0 : publishCourtListPayload.getHearingByWeekCommencingDate().getHearingByCourtrooms().size()));
            publishCourtListNotificationService.sendNotification(event, publishCourtListPayload, documentTemplateName);
        });
    }

    private String getDocumentTemplateName(final PublishCourtListType publishCourtListType, final int hearingCount) {
        if (publishCourtListType == DRAFT || publishCourtListType == FINAL) {
            return hearingCount == 1 ? PUBLISH_DRAFT_OR_FINAL_COURT_LIST_FOR_SINGLE_HEARING : PUBLISH_DRAFT_OR_FINAL_COURT_LIST_FOR_MULTIPLE_HEARING;
        } else if (publishCourtListType == FIRM) {
            return hearingCount == 1 ? PUBLISH_FIRM_COURT_LIST_FOR_SINGLE_HEARING : PUBLISH_FIRM_COURT_LIST_FOR_MULTIPLE_HEARING;
        } else if (publishCourtListType == WARN) {
            return hearingCount == 1 ? PUBLISH_WARN_COURT_LIST_FOR_SINGLE_HEARING : PUBLISH_WARN_COURT_LIST_FOR_MULTIPLE_HEARING;
        }

        throw new IllegalArgumentException(format("Notification template cannot be found for publish court list type %s", publishCourtListType.name()));
    }

}
