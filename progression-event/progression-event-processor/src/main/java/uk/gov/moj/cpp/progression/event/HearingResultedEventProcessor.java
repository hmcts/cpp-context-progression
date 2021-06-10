package uk.gov.moj.cpp.progression.event;

import static java.util.Objects.nonNull;
import static java.util.Optional.ofNullable;
import static java.util.stream.Collectors.toMap;
import static javax.json.Json.createObjectBuilder;
import static org.apache.commons.collections.CollectionUtils.isNotEmpty;
import static uk.gov.justice.services.core.annotation.Component.EVENT_PROCESSOR;

import uk.gov.justice.core.courts.CommittingCourt;
import uk.gov.justice.core.courts.Hearing;
import uk.gov.justice.core.courts.HearingListingNeeds;
import uk.gov.justice.core.courts.HearingUnscheduledListingNeeds;
import uk.gov.justice.core.courts.ListUnscheduledNextHearings;
import uk.gov.justice.core.courts.NextHearingsRequested;
import uk.gov.justice.core.courts.ProsecutionCasesResultedV2;
import uk.gov.justice.core.courts.SeedingHearing;
import uk.gov.justice.core.courts.UnscheduledNextHearingsRequested;
import uk.gov.justice.listing.courts.ListNextHearings;
import uk.gov.justice.progression.courts.BookingReferenceCourtScheduleIds;
import uk.gov.justice.progression.courts.StoreBookingReferenceCourtScheduleIds;
import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.core.annotation.FeatureControl;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.progression.helper.HearingResultUnscheduledListingHelper;
import uk.gov.moj.cpp.progression.helper.SummonsHelper;
import uk.gov.moj.cpp.progression.helper.UnscheduledCourtHearingListTransformer;
import uk.gov.moj.cpp.progression.service.ListingService;
import uk.gov.moj.cpp.progression.service.ProgressionService;
import uk.gov.moj.cpp.progression.transformer.HearingToHearingListingNeedsTransformer;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ServiceComponent(EVENT_PROCESSOR)
public class HearingResultedEventProcessor {

    private static final Logger LOGGER = LoggerFactory.getLogger(HearingResultedEventProcessor.class.getName());
    private static final String SHADOW_LISTED_OFFENCES = "shadowListedOffences";

    @Inject
    private Sender sender;

    @Inject
    private JsonObjectToObjectConverter jsonObjectToObjectConverter;

    @Inject
    private ListingService listingService;

    @Inject
    private ProgressionService progressionService;

    @Inject
    private HearingToHearingListingNeedsTransformer hearingToHearingListingNeedsTransformer;

    @Inject
    private HearingResultUnscheduledListingHelper hearingResultUnscheduledListingHelper;

    @Inject
    private UnscheduledCourtHearingListTransformer unscheduledCourtHearingListTransformer;

    @Inject
    private SummonsHelper summonsHelper;

    @Handles("public.events.hearing.hearing-resulted")
    @FeatureControl("amendReshare")
    public void handlePublicHearingResulted(final JsonEnvelope event) {

        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Received 'public.events.hearing.hearing-resulted' event with payload: {}", event.toObfuscatedDebugString());
        }

        final JsonObject eventPayload = event.payloadAsJsonObject();
        final JsonObject hearingJson = eventPayload.getJsonObject("hearing");

        if (hearingJson.getBoolean("isSJPHearing", false)) {
            return;
        }

        final JsonObjectBuilder commandPayloadBuilder = createObjectBuilder()
                .add("hearing", hearingJson)
                .add("sharedTime", eventPayload.getJsonString("sharedTime"))
                .add("hearingDay", eventPayload.getJsonString("hearingDay"));

        if (eventPayload.containsKey(SHADOW_LISTED_OFFENCES)) {
            commandPayloadBuilder.add(SHADOW_LISTED_OFFENCES, eventPayload.getJsonArray(SHADOW_LISTED_OFFENCES));
        }

        this.sender.send(Enveloper.envelop(commandPayloadBuilder.build())
                .withName("progression.command.process-hearing-results")
                .withMetadataFrom(event));

        final Hearing hearing = jsonObjectToObjectConverter.convert(hearingJson, Hearing.class);
        summonsHelper.initiateSummonsProcess(event, hearing);
    }

    @Handles("progression.event.prosecution-cases-resulted-v2")
    public void handleProsecutionCasesResultedV2(final JsonEnvelope event) {
        final ProsecutionCasesResultedV2 prosecutionCasesResulted = jsonObjectToObjectConverter.convert(event.payloadAsJsonObject(), ProsecutionCasesResultedV2.class);
        final Hearing hearing = prosecutionCasesResulted.getHearing();

        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Sending commands to update cases following hearing results shared for hearing id: {}", hearing.getId());
        }

        hearing.getProsecutionCases().forEach(prosecutionCase -> progressionService.updateCase(event, prosecutionCase, hearing.getCourtApplications()));
    }

    @Handles("progression.event.next-hearings-requested")
    public void handleNextHearingsRequested(final JsonEnvelope event) {
        final NextHearingsRequested nextHearingsRequested = jsonObjectToObjectConverter.convert(event.payloadAsJsonObject(), NextHearingsRequested.class);
        final Hearing hearing = nextHearingsRequested.getHearing();
        final List<UUID> shadowListedOffences = nextHearingsRequested.getShadowListedOffences();
        final Optional<CommittingCourt> committingCourt = ofNullable(nextHearingsRequested.getCommittingCourt());
        final SeedingHearing seedingHearing = nextHearingsRequested.getSeedingHearing();

        final Map<UUID, Set<UUID>> combinedBookingReferencesWithCourtScheduleIds = getCombinedBookingReferencesWithCourtScheduleIds(nextHearingsRequested, hearing);

        final List<HearingListingNeeds> nextHearingsList = hearingToHearingListingNeedsTransformer.transformWithSeedHearing(hearing, committingCourt, seedingHearing, combinedBookingReferencesWithCourtScheduleIds);
        if (isNotEmpty(nextHearingsList)) {
            final ListNextHearings listNextHearings = ListNextHearings.listNextHearings()
                    .withHearingId(seedingHearing.getSeedingHearingId())
                    .withSeedingHearing(seedingHearing)
                    .withAdjournedFromDate(LocalDate.now())
                    .withHearings(nextHearingsList)
                    .withShadowListedOffences(shadowListedOffences)
                    .build();

            listingService.listNextCourtHearings(event, listNextHearings);
            progressionService.updateHearingListingStatusToSentForListing(event, listNextHearings.getHearings(), seedingHearing);
        }

        if (!combinedBookingReferencesWithCourtScheduleIds.isEmpty()) {

            sendStoreBookingReferencesCommand(event, hearing, seedingHearing, combinedBookingReferencesWithCourtScheduleIds);

        }

    }

    @Handles("progression.event.unscheduled-next-hearings-requested")
    public void handUnscheduledNextHearingsRequested(final JsonEnvelope event) {
        final UnscheduledNextHearingsRequested unscheduledHearingListingRequested = jsonObjectToObjectConverter.convert(event.payloadAsJsonObject(), UnscheduledNextHearingsRequested.class);

        final Hearing hearing = unscheduledHearingListingRequested.getHearing();
        final SeedingHearing seedingHearing = unscheduledHearingListingRequested.getSeedingHearing();

        final List<HearingUnscheduledListingNeeds> unscheduledListingNeeds = unscheduledCourtHearingListTransformer.transformWithSeedHearing(hearing, seedingHearing);

        if (!unscheduledListingNeeds.isEmpty()) {
            listUnscheduledHearings(event, seedingHearing, unscheduledListingNeeds);

            final List<Hearing> hearingList = unscheduledListingNeeds.stream()
                    .filter(uln -> nonNull(uln.getProsecutionCases()))
                    .map(uln -> hearingResultUnscheduledListingHelper.convertToHearing(uln, hearing.getHearingDays()))
                    .collect(Collectors.toList());

            if (!hearingList.isEmpty()) {
                final Set<UUID> hearingsToBeSentNotification = hearingResultUnscheduledListingHelper.getHearingIsToBeSentNotification(unscheduledListingNeeds);
                progressionService.sendUpdateDefendantListingStatusForUnscheduledListing(event, hearingList, hearingsToBeSentNotification);
                progressionService.recordUnlistedHearing(event, hearing.getId(), hearingList);
            }
        }
    }

    private void listUnscheduledHearings(final JsonEnvelope event, final SeedingHearing seedingHearing, final List<HearingUnscheduledListingNeeds> unscheduledListingNeeds) {
        final ListUnscheduledNextHearings listUnscheduledNextHearings = ListUnscheduledNextHearings.listUnscheduledNextHearings()
                .withHearingId(seedingHearing.getSeedingHearingId())
                .withSeedingHearing(seedingHearing)
                .withHearings(unscheduledListingNeeds)
                .build();

        listingService.listUnscheduledNextHearings(event, listUnscheduledNextHearings);
    }

    /**
     * This method returns map of booking references and list of court schedule ids by combining
     * both previous booking references and current booking references.
     *
     * @param nextHearingsRequested previous booking references.
     * @param hearing               has current booking references.
     * @return
     */
    private Map<UUID, Set<UUID>> getCombinedBookingReferencesWithCourtScheduleIds(final NextHearingsRequested nextHearingsRequested, final Hearing hearing) {
        final Map<UUID, List<UUID>> previousBookingReferencesWithCourtScheduleIds = nextHearingsRequested.getPreviousBookingReferencesWithCourtScheduleIds().stream()
                .collect(
                        toMap(BookingReferenceCourtScheduleIds::getBookingId, BookingReferenceCourtScheduleIds::getCourtScheduleIds)
                );
        return hearingToHearingListingNeedsTransformer.getCombinedBookingReferencesAndCourtScheduleIds(hearing, previousBookingReferencesWithCourtScheduleIds);
    }

    /**
     * Whenever we share or re-share hearing, we get the court schedule ids for booking slots from
     * rota. When we re-share, we need to free the booking slots available for hearing. On re-share,
     * we are making two calls, one to free the slots and another to query the booking slots.
     * Booking slots should be freed before we make a call to rota for the booking slots. Race
     * condition is happening where call to retrieve the booking slots is happening before freeing
     * the slots. To avoid this scenario, We are storing all the booking slots for hearing and make
     * call only to new booking slots.
     * <p>
     * This command stores all the booking slots and associated court schedule ids. When the hearing
     * is re-shared the stored booking slots with associated court schedule ids will be sent along
     * with the event NextHearingsRequested. When re-sharing the event processor will make a call to
     * rota service only for new booking slots.
     *
     * @param event
     * @param hearing
     * @param seedingHearing
     * @param combinedBookingReferencesWithCourtScheduleIdsMap
     */
    private void sendStoreBookingReferencesCommand(final JsonEnvelope event, final Hearing hearing, final SeedingHearing seedingHearing, final Map<UUID, Set<UUID>> combinedBookingReferencesWithCourtScheduleIdsMap) {

        final List<BookingReferenceCourtScheduleIds> combinedBookingReferencesWithCourtScheduleIdsList = new ArrayList<>();
        combinedBookingReferencesWithCourtScheduleIdsMap.forEach((bookingId, courtScheduleIds) -> combinedBookingReferencesWithCourtScheduleIdsList.add(BookingReferenceCourtScheduleIds.bookingReferenceCourtScheduleIds()
                .withBookingId(bookingId)
                .withCourtScheduleIds(new ArrayList<>(courtScheduleIds))
                .build()));

        final StoreBookingReferenceCourtScheduleIds command = StoreBookingReferenceCourtScheduleIds.storeBookingReferenceCourtScheduleIds()
                .withHearingId(hearing.getId())
                .withHearingDay(LocalDate.parse(seedingHearing.getSittingDay()))
                .withBookingReferenceCourtScheduleIds(combinedBookingReferencesWithCourtScheduleIdsList)
                .build();

        progressionService.storeBookingReferencesWithCourtScheduleIds(event, command);
    }

}
