package uk.gov.moj.cpp.progression.event;

import static java.lang.Boolean.FALSE;
import static java.lang.Boolean.TRUE;
import static java.util.Collections.singletonList;
import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static java.util.Optional.ofNullable;
import static org.apache.commons.collections.CollectionUtils.isNotEmpty;
import static uk.gov.justice.core.courts.Category.FINAL;
import static uk.gov.justice.core.courts.HearingListingStatus.HEARING_INITIALISED;
import static uk.gov.justice.services.core.annotation.Component.EVENT_PROCESSOR;
import static uk.gov.justice.services.core.enveloper.Enveloper.envelop;

import uk.gov.justice.core.courts.ApplicationStatus;
import uk.gov.justice.core.courts.ConfirmedHearing;
import uk.gov.justice.core.courts.ConfirmedProsecutionCase;
import uk.gov.justice.core.courts.CourtApplication;
import uk.gov.justice.core.courts.ExtendHearing;
import uk.gov.justice.core.courts.Hearing;
import uk.gov.justice.core.courts.HearingConfirmed;
import uk.gov.justice.core.courts.HearingDay;
import uk.gov.justice.core.courts.HearingListingNeeds;
import uk.gov.justice.core.courts.HearingListingStatus;
import uk.gov.justice.core.courts.JudicialResult;
import uk.gov.justice.core.courts.ListCourtHearing;
import uk.gov.justice.core.courts.NextHearing;
import uk.gov.justice.core.courts.ProsecutionCase;
import uk.gov.justice.hearing.courts.HearingResulted;
import uk.gov.justice.progression.courts.HearingExtended;
import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.common.converter.ObjectToJsonObjectConverter;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.progression.helper.HearingResultHelper;
import uk.gov.moj.cpp.progression.helper.HearingResultUnscheduledListingHelper;
import uk.gov.moj.cpp.progression.service.ListingService;
import uk.gov.moj.cpp.progression.service.NextHearingService;
import uk.gov.moj.cpp.progression.service.ProgressionService;
import uk.gov.moj.cpp.progression.service.dto.NextHearingDetails;
import uk.gov.moj.cpp.progression.transformer.HearingListingNeedsTransformer;
import uk.gov.moj.cpp.progression.transformer.HearingToHearingListingNeedsTransformer;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import javax.inject.Inject;
import javax.json.JsonObject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ServiceComponent(EVENT_PROCESSOR)
public class HearingResultEventProcessor {

    private static final Logger LOGGER = LoggerFactory.getLogger(HearingResultEventProcessor.class.getName());
    private static final String PUBLIC_PROGRESSION_EVENTS_HEARING_EXTENDED = "public.progression.events.hearing-extended";

    @Inject
    private Sender sender;

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
    private HearingListingNeedsTransformer hearingListingNeedsTransformer;

    @Inject
    private HearingResultUnscheduledListingHelper hearingResultUnscheduledListingHelper;

    @Inject
    private NextHearingService nextHearingService;

    @Inject
    private HearingResultHelper hearingResultHelper;

    @Handles("public.hearing.resulted")
    public void handleHearingResultedPublicEvent(final JsonEnvelope event) {

        final HearingResulted hearingResulted = jsonObjectToObjectConverter.convert(event.payloadAsJsonObject(), HearingResulted.class);
        final Hearing hearing = hearingResulted.getHearing();
        final List<UUID> shadowListedOffences = hearingResulted.getShadowListedOffences();

        LOGGER.info("Hearing resulted for hearing id :: {}", hearing.getId());

        final Hearing hearingInProgression = retrieveHearing(event, hearing.getId());

        this.sender.send(Enveloper.envelop(event.payloadAsJsonObject())
                .withName("progression.command.hearing-result")
                .withMetadataFrom(event));

        final boolean isHearingAdjournedAlreadyForProsecutionCases = hearingResultHelper.doProsecutionCasesContainNextHearingResults(hearingInProgression.getProsecutionCases());
        LOGGER.info("Hearing with hearing id :: {} already adjourned for prosecution cases :: {}", hearing.getId(), isHearingAdjournedAlreadyForProsecutionCases);
        final boolean isHearingAdjournedAlreadyForCourtApplications = hearingResultHelper.doCourtApplicationsContainNextHearingResults(hearingInProgression.getCourtApplications());
        LOGGER.info("Hearing with hearing id :: {} already adjourned for court applications :: {}", hearing.getId(), isHearingAdjournedAlreadyForCourtApplications);
        resultProsecutionCases(event, hearing, isHearingAdjournedAlreadyForProsecutionCases, shadowListedOffences);
        resultApplications(event, hearing, isHearingAdjournedAlreadyForCourtApplications, shadowListedOffences);

        if (hearingResultUnscheduledListingHelper.checksIfUnscheduledHearingNeedsToBeCreated(hearing)) {
            progressionService.listUnscheduledHearings(event, hearing);
        }
    }

    private void resultProsecutionCases(final JsonEnvelope event, final Hearing hearing, final boolean isHearingAdjournedAlreadyForProsecutionCases, final List<UUID> shadowListedOffences) {
        if (isNotEmpty(hearing.getProsecutionCases())) {
            LOGGER.info("Hearing contains prosecution cases resulted for hearing id :: {}", hearing.getId());
            hearing.getProsecutionCases().forEach(prosecutionCase -> progressionService.updateCase(event, prosecutionCase, hearing.getCourtApplications()));

            final boolean isHearingAdjournedForProsecutionCases = hearingResultHelper.doProsecutionCasesContainNextHearingResults(hearing.getProsecutionCases());
            LOGGER.info("Hearing with hearing id containing prosecution cases :: {} resulted with next hearing :: {}", hearing.getId(), isHearingAdjournedForProsecutionCases);
            if (!isHearingAdjournedAlreadyForProsecutionCases && isHearingAdjournedForProsecutionCases) {
                adjournProsecutionCasesToExistingHearings(event, hearing, shadowListedOffences);
                adjournProsecutionCasesToNewHearings(event, hearing, shadowListedOffences);
            } else {
                LOGGER.info("Hearing contains prosecution cases adjourned already or does not contain next hearing details for hearing id :: {}", hearing.getId());
            }
        }
    }

    private void resultApplications(final JsonEnvelope event, final Hearing hearing, final boolean isHearingAdjournedAlreadyForCourtApplications, final List<UUID> shadowListedOffences) {
        if (isNotEmpty(hearing.getCourtApplications())) {
            LOGGER.info("Hearing contains court applications resulted for hearing id :: {}", hearing.getId());
            final List<UUID> applicationIdsResultCategoryFinal = new ArrayList<>();
            final List<UUID> allApplicationIds = new ArrayList<>();
            hearing.getCourtApplications().forEach(courtApplication -> {
                allApplicationIds.add(courtApplication.getId());
                final boolean isResultCategoryFinal = ofNullable(courtApplication.getJudicialResults()).isPresent() ? courtApplication.getJudicialResults().stream().filter(Objects::nonNull).map(JudicialResult::getCategory).anyMatch(FINAL::equals) : FALSE;
                if ( isResultCategoryFinal) {
                    applicationIdsResultCategoryFinal.add(courtApplication.getId());
                }
            });
            progressionService.linkApplicationsToHearing(event, hearing, allApplicationIds, HearingListingStatus.HEARING_RESULTED);
            progressionService.updateCourtApplicationStatus(event, applicationIdsResultCategoryFinal, ApplicationStatus.FINALISED);

            final boolean isHearingAdjournedForCourtApplications = hearingResultHelper.doCourtApplicationsContainNextHearingResults(hearing.getCourtApplications());
            LOGGER.info("Hearing with hearing id containing court applications :: {} resulted with next hearing :: {}", hearing.getId(), isHearingAdjournedForCourtApplications);
            if (!isHearingAdjournedAlreadyForCourtApplications && isHearingAdjournedForCourtApplications) {
                // adjourn court applications
                adjournCourtApplicationsToExistingHearing(event, hearing, shadowListedOffences);
                adjournCourtApplicationsToNewHearing(event, hearing, shadowListedOffences);
            } else {
                LOGGER.info("Hearing contains court applications adjourned already or does not contain next hearing details for hearing id :: {}", hearing.getId());
            }
        }
    }

    private void adjournProsecutionCasesToNewHearings(final JsonEnvelope event, final Hearing hearing, final List<UUID> shadowListedOffences) {
        LOGGER.info("Hearing adjourned to new hearing or hearings :: {}", hearing.getId());
        listCourtHearings(event, hearingToHearingListingNeedsTransformer.transform(hearing), shadowListedOffences);
    }

    private void adjournProsecutionCasesToExistingHearings(JsonEnvelope jsonEnvelope, final Hearing hearing, final List<UUID> shadowListedOffences) {
        LOGGER.info("Hearing adjourned to exiting hearing or hearings :: {}", hearing.getId());
        final NextHearingDetails nextHearingDetails = nextHearingService.getNextHearingDetails(hearing);

        nextHearingDetails.getHearingListingNeedsList().forEach(hearingListingNeeds -> {
            final ExtendHearing extendHearing = ExtendHearing.extendHearing()
                    .withHearingRequest(hearingListingNeeds)
                    .withIsAdjourned(TRUE)
                    .withShadowListedOffences(shadowListedOffences)
                    .build();
            sender.send(
                    envelop(objectToJsonObjectConverter.convert(extendHearing))
                            .withName("progression.command.extend-hearing")
                            .withMetadataFrom(jsonEnvelope));
        });
        generateSummons(nextHearingDetails.getHearingsMap(), hearing, nextHearingDetails.getNextHearings(), jsonEnvelope);
    }

    private void adjournCourtApplicationsToNewHearing(final JsonEnvelope event, final Hearing hearing, final List<UUID> shadowListedOffences) {
        LOGGER.info("Hearing containing court applications adjourned to new hearing or hearings :: {}", hearing.getId());
        listCourtHearings(event, hearingListingNeedsTransformer.transform(hearing), shadowListedOffences);
    }

    private void adjournCourtApplicationsToExistingHearing(JsonEnvelope event, Hearing hearing, List<UUID> shadowListedOffences) {
        hearing.getCourtApplications().forEach(courtApplication -> {
            if (isNotEmpty(courtApplication.getJudicialResults())) {
                courtApplication.getJudicialResults().forEach(judicialResult -> {
                    final NextHearing nextHearing = judicialResult.getNextHearing();
                    if (nonNull(nextHearing) && nonNull(nextHearing.getExistingHearingId())) {
                        final HearingExtended hearingExtended = getHearingExtended(shadowListedOffences, courtApplication, nextHearing);
                        final Hearing existingHearing = retrieveHearing(event, nextHearing.getExistingHearingId());

                        progressionService.updateCourtApplicationStatus(event, courtApplication.getId(), ApplicationStatus.LISTED);
                        final Hearing updatedHearing = updateHearingWithApplication(existingHearing, courtApplication);
                        progressionService.linkApplicationsToHearing(event, updatedHearing, singletonList(courtApplication.getId()), HEARING_INITIALISED);
                        sender.send(envelop(objectToJsonObjectConverter.convert(hearingExtended))
                                .withName(PUBLIC_PROGRESSION_EVENTS_HEARING_EXTENDED)
                                .withMetadataFrom(event));
                    }
                });
            }
        });
    }

    private HearingExtended getHearingExtended(final List<UUID> shadowListedOffences, final CourtApplication courtApplication, final NextHearing nextHearing) {
        return HearingExtended.hearingExtended()
                                    .withHearingId(nextHearing.getExistingHearingId())
                                    .withCourtApplication(courtApplication)
                                    .withShadowListedOffences(shadowListedOffences)
                                    .build();
    }


    private void listCourtHearings(JsonEnvelope event, List<HearingListingNeeds> hearingListingNeeds, final List<UUID> shadowListedOffences) {
        if (isNotEmpty(hearingListingNeeds)) {
            final ListCourtHearing listCourtHearing = ListCourtHearing.listCourtHearing()
                    .withHearings(hearingListingNeeds)
                    .withAdjournedFromDate(LocalDate.now())
                    .withShadowListedOffences(shadowListedOffences)
                    .build();
            listingService.listCourtHearing(event, listCourtHearing);
            progressionService.updateHearingListingStatusToSentForListing(event, listCourtHearing);
        }
    }

    private Hearing updateHearingWithApplication(final Hearing hearing, final CourtApplication courtApplication) {
        List<CourtApplication> courtApplications = hearing.getCourtApplications();
        if (isNull(courtApplications)) {
            courtApplications = new ArrayList<>();
        }
        courtApplications.add(courtApplication);
        return Hearing.hearing()
                .withType(hearing.getType())
                .withCourtApplications(courtApplications)
                .withHearingCaseNotes(hearing.getHearingCaseNotes())
                .withDefendantReferralReasons(hearing.getDefendantReferralReasons())
                .withJudiciary(hearing.getJudiciary())
                .withProsecutionCases(hearing.getProsecutionCases())
                .withDefenceCounsels(hearing.getDefenceCounsels())
                .withHearingDays(hearing.getHearingDays())
                .withCourtCentre(hearing.getCourtCentre())
                .withProsecutionCounsels(hearing.getProsecutionCounsels())
                .withDefendantAttendance(hearing.getDefendantAttendance())
                .withJurisdictionType(hearing.getJurisdictionType())
                .withId(hearing.getId())
                .withApplicantCounsels(hearing.getApplicantCounsels())
                .withApplicationPartyCounsels(hearing.getApplicationPartyCounsels())
                .withCourtApplicationPartyAttendance(hearing.getCourtApplicationPartyAttendance())
                .withCrackedIneffectiveTrial(hearing.getCrackedIneffectiveTrial())
                .withHasSharedResults(hearing.getHasSharedResults())
                .withHearingLanguage(hearing.getHearingLanguage())
                .withIsBoxHearing(hearing.getIsBoxHearing())
                .withReportingRestrictionReason(hearing.getReportingRestrictionReason())
                .withRespondentCounsels(hearing.getRespondentCounsels())
                .build();
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

    private Hearing retrieveHearing(final JsonEnvelope event, final UUID hearingId) {
        final Optional<JsonObject> hearingPayloadOptional = progressionService.getHearing(event, hearingId.toString());
        final JsonObject hearingJson = hearingPayloadOptional.orElseThrow(() -> new RuntimeException("Hearing not found")).getJsonObject("hearing");
        return jsonObjectToObjectConverter.convert(hearingJson, Hearing.class);
    }
}