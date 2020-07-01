package uk.gov.moj.cpp.progression.event;

import static java.lang.Boolean.TRUE;
import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static org.apache.commons.collections.CollectionUtils.isNotEmpty;
import static uk.gov.justice.services.core.annotation.Component.EVENT_PROCESSOR;
import static uk.gov.justice.services.core.enveloper.Enveloper.envelop;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.gov.justice.core.courts.ApplicationStatus;
import uk.gov.justice.core.courts.ConfirmedHearing;
import uk.gov.justice.core.courts.ConfirmedProsecutionCase;
import uk.gov.justice.core.courts.ExtendHearing;
import uk.gov.justice.core.courts.Hearing;
import uk.gov.justice.core.courts.HearingConfirmed;
import uk.gov.justice.core.courts.HearingDay;
import uk.gov.justice.core.courts.HearingListingNeeds;
import uk.gov.justice.core.courts.HearingListingStatus;
import uk.gov.justice.core.courts.ListCourtHearing;
import uk.gov.justice.core.courts.NextHearing;
import uk.gov.justice.core.courts.ProsecutionCase;
import uk.gov.justice.hearing.courts.HearingResulted;
import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.common.converter.ObjectToJsonObjectConverter;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.progression.helper.HearingResultUnscheduledListingHelper;
import uk.gov.moj.cpp.progression.service.ListingService;
import uk.gov.moj.cpp.progression.service.NextHearingService;
import uk.gov.moj.cpp.progression.service.ProgressionService;
import uk.gov.moj.cpp.progression.service.dto.NextHearingDetails;
import uk.gov.moj.cpp.progression.transformer.HearingToHearingListingNeedsTransformer;
import javax.inject.Inject;
import javax.json.JsonObject;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@ServiceComponent(EVENT_PROCESSOR)
public class HearingResultEventProcessor {

    private static final Logger LOGGER = LoggerFactory.getLogger(HearingResultEventProcessor.class.getName());

    @Inject
    private Sender sender;

    @Inject
    private Enveloper enveloper;

    @Inject
    private JsonObjectToObjectConverter jsonObjectToObjectConverter;

    @Inject
    private ObjectToJsonObjectConverter objectToJsonObjectConverter;

    @Inject
    private ProgressionService progressionService;

    @Inject
    private ListingService listingService;

    @Inject
    private HearingToHearingListingNeedsTransformer hearingToHearingListingNeedsTransformer;

    @Inject
    private HearingResultUnscheduledListingHelper hearingResultUnscheduledListingHelper;

    @Inject
    private NextHearingService nextHearingService;

    @Handles("public.hearing.resulted")
    public void handleHearingResultedPublicEvent(final JsonEnvelope event) {
        this.sender.send(Enveloper.envelop(event.payloadAsJsonObject())
                .withName("progression.command.hearing-result")
                .withMetadataFrom(event));

        final HearingResulted hearingResulted = jsonObjectToObjectConverter.convert(event.payloadAsJsonObject(), HearingResulted.class);
        final Hearing hearing = hearingResulted.getHearing();

        resultProsecutionCases(event, hearing);
        resultApplications(event, hearing);

        if (hearingResultUnscheduledListingHelper.checksIfUnscheduledHearingNeedsToBeCreated(hearing)) {
            progressionService.listUnscheduledHearings(event, hearing);
        }
    }

    private void resultApplications(final JsonEnvelope event, final Hearing hearing) {
        if (isNotEmpty(hearing.getCourtApplications())) {
            final List<UUID> applicationIdsHaveOutcome = new ArrayList<>();
            final List<UUID> allApplicationIds = new ArrayList<>();
            hearing.getCourtApplications().forEach(courtApplication -> {
                allApplicationIds.add(courtApplication.getId());
                if (nonNull(courtApplication.getApplicationOutcome())){
                    applicationIdsHaveOutcome.add(courtApplication.getId());
                }
            });
            progressionService.linkApplicationsToHearing(event, hearing, allApplicationIds, HearingListingStatus.HEARING_RESULTED);
            progressionService.updateCourtApplicationStatus(event, applicationIdsHaveOutcome, ApplicationStatus.FINALISED);
        }
    }

    private void resultProsecutionCases(final JsonEnvelope event, final Hearing hearing) {
        LOGGER.info("Hearing resulted for the prosecution cases in the hearing id :: {}", hearing.getId());
        final Optional<JsonObject> hearingPayloadOptional = progressionService.getHearing(event, hearing.getId().toString());
        final JsonObject hearingJson = hearingPayloadOptional.orElseThrow(() -> new RuntimeException("Hearing not found")).getJsonObject("hearing");
        final Hearing hearingInProgression = jsonObjectToObjectConverter.convert(hearingJson, Hearing.class);
        if (isNotEmpty(hearing.getProsecutionCases())) {
            hearing.getProsecutionCases().forEach(prosecutionCase -> progressionService.updateCase(event, prosecutionCase, hearing.getCourtApplications()));
            if (isNull(hearingInProgression.getHasSharedResults()) || !hearingInProgression.getHasSharedResults()) {
                adjournToExistingHearings(event, hearing);
                adjournToNewHearings(event, hearing);
            } else {
                LOGGER.info("Hearing already resulted for hearing id :: {}", hearing.getId());
            }
        }
    }

    private void adjournToNewHearings(final JsonEnvelope event, final Hearing hearing) {
        LOGGER.info("Hearing adjourned to new hearing or hearings :: {}", hearing.getId());
        final List<HearingListingNeeds> hearingListingNeeds = hearingToHearingListingNeedsTransformer.transform(hearing);
        if (isNotEmpty(hearingListingNeeds)) {
            final ListCourtHearing listCourtHearing = ListCourtHearing.listCourtHearing()
                    .withHearings(hearingListingNeeds)
                    .withAdjournedFromDate(LocalDate.now())
                    .build();
            listingService.listCourtHearing(event, listCourtHearing);
            progressionService.updateHearingListingStatusToSentForListing(event, listCourtHearing);
        }
    }

    private void adjournToExistingHearings(JsonEnvelope jsonEnvelope, final Hearing hearing) {
        LOGGER.info("Hearing adjourned to exiting hearing or hearings :: {}", hearing.getId());
        final NextHearingDetails nextHearingDetails = nextHearingService.getNextHearingDetails(hearing);

        nextHearingDetails.getHearingListingNeedsList().forEach(hearingListingNeeds -> {
            final ExtendHearing extendHearing = ExtendHearing.extendHearing()
                    .withHearingRequest(hearingListingNeeds)
                    .withIsAdjourned(TRUE)
                    .build();
            sender.send(
                    envelop(objectToJsonObjectConverter.convert(extendHearing))
                            .withName("progression.command.extend-hearing")
                            .withMetadataFrom(jsonEnvelope));
        });
        generateSummons(nextHearingDetails.getHearingsMap(), hearing, nextHearingDetails.getNextHearings(), jsonEnvelope);
    }

    private void generateSummons(Map<UUID, Map<UUID, Map<UUID, Set<UUID>>>> hearingsMap, Hearing hearing, Map<UUID, NextHearing> nextHearings, JsonEnvelope jsonEnvelope) {
        LOGGER.info("Summons for extended or existing hearing from the current hearing :: {}", hearing.getId());
        final Map<UUID, List<ConfirmedProsecutionCase>> confirmedHearings = nextHearingService.getConfirmedHearings(hearingsMap);
        for (final Map.Entry<UUID, List<ConfirmedProsecutionCase>> hearingEntry : confirmedHearings.entrySet()) {
            final UUID adjournedHearingId = hearingEntry.getKey();
            final List<ConfirmedProsecutionCase> confirmedProsecutionCases = hearingEntry.getValue();
            final NextHearing nextHearing = nextHearings.get(adjournedHearingId);
            final List<ProsecutionCase> cases = progressionService.transformProsecutionCase(confirmedProsecutionCases, nextHearing.getListedStartDateTime().toLocalDate(), jsonEnvelope);

            // update youth summons
            progressionService.updateDefendantYouthForProsecutionCase(jsonEnvelope, cases);

            // prepare summons data
            final HearingConfirmed hearingConfirmed = HearingConfirmed.hearingConfirmed()
                    .withConfirmedHearing(ConfirmedHearing.confirmedHearing()
                            .withId(hearing.getId())
                            .withType(nextHearing.getType())
                            .withJurisdictionType(nextHearing.getJurisdictionType())
                            .withExistingHearingId(adjournedHearingId)
                            .withHearingDays(createHearingDays(nextHearing))
                            .withCourtCentre(nextHearing.getCourtCentre())
                            .withProsecutionCases(confirmedProsecutionCases)
                            .build())
                    .build();
            progressionService.prepareSummonsDataForExtendHearing(jsonEnvelope, hearingConfirmed);
        }
    }

    private List<HearingDay> createHearingDays(final NextHearing nextHearing) {
        final List<HearingDay> hearingDays = new ArrayList<>();
        if (nonNull(nextHearing.getListedStartDateTime())) {
            hearingDays.add(HearingDay.hearingDay()
                    .withSittingDay(nextHearing.getListedStartDateTime())
                    .withListedDurationMinutes(nextHearing.getEstimatedMinutes())
                    .build());
        }
        return hearingDays;
    }
 }