package uk.gov.moj.cpp.progression.event;

import static java.util.Objects.nonNull;
import static java.util.Optional.empty;
import static java.util.Optional.ofNullable;
import static java.util.stream.Collectors.toList;
import static javax.json.Json.createArrayBuilder;
import static javax.json.Json.createObjectBuilder;
import static org.apache.commons.collections.CollectionUtils.isEmpty;
import static org.apache.commons.collections.CollectionUtils.isNotEmpty;
import static uk.gov.justice.core.courts.Category.FINAL;
import static uk.gov.justice.services.core.annotation.Component.EVENT_PROCESSOR;
import static uk.gov.justice.services.core.enveloper.Enveloper.envelop;

import uk.gov.justice.core.courts.ApplicationStatus;
import uk.gov.justice.core.courts.CommittingCourt;
import uk.gov.justice.core.courts.CourtApplication;
import uk.gov.justice.core.courts.DefendantJudicialResult;
import uk.gov.justice.core.courts.ExtendHearing;
import uk.gov.justice.core.courts.Hearing;
import uk.gov.justice.core.courts.HearingListingNeeds;
import uk.gov.justice.core.courts.HearingListingStatus;
import uk.gov.justice.core.courts.JudicialResult;
import uk.gov.justice.core.courts.JudicialResultPrompt;
import uk.gov.justice.core.courts.ListCourtHearing;
import uk.gov.justice.core.courts.SeedingHearing;
import uk.gov.justice.hearing.courts.HearingResulted;
import uk.gov.justice.progression.courts.ApplicationsResulted;
import uk.gov.justice.progression.courts.ProsecutionCasesResulted;
import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.common.converter.ObjectToJsonObjectConverter;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.progression.converter.SeedingHearingConverter;
import uk.gov.moj.cpp.progression.helper.HearingResultHelper;
import uk.gov.moj.cpp.progression.helper.HearingResultUnscheduledListingHelper;
import uk.gov.moj.cpp.progression.service.ListingService;
import uk.gov.moj.cpp.progression.service.NextHearingService;
import uk.gov.moj.cpp.progression.service.ProgressionService;
import uk.gov.moj.cpp.progression.service.dto.NextHearingDetails;
import uk.gov.moj.cpp.progression.transformer.HearingToHearingListingNeedsTransformer;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.inject.Inject;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ServiceComponent(EVENT_PROCESSOR)
public class HearingResultEventProcessor {

    private static final Logger LOGGER = LoggerFactory.getLogger(HearingResultEventProcessor.class.getName());
    private static final String COMMITTED_TO_CC = "CommittedToCC";
    private static final String SENT_TO_CC = "SentToCC";
    private static final String PROSECUTOR_COST = "prosecutorCost";
    public static final String SUMMONS_SUPPRESSED = "summonsSuppressed";
    public static final String PERSONAL_SERVICE = "personalService";
    public static final String PROSECUTOR_EMAIL_ADDRESS = "prosecutorEmailAddress";
    public static final String REASONS = "reasons";
    public static final String APPLICATION_ID = "applicationId";
    public static final String REASONS_FOR_REJECTION = "reasonsForRejection";
    public static final String PROSECUTION_COSTS = "prosecutionCosts";
    public static final String THIS_SUMMONS_WILL_BE_SERVED_BY_A_PROSECUTOR = "thisSummonsWillBeServedByAProsecutor";
    public static final String THIS_SUMMONS_IS_FOR_PERSONAL_SERVICE = "tHISSUMMONSISFORPERSONALSERVICE";
    public static final String PROSECUTORS_EMAIL_ADDRESS_USED_SUMMONS_NOTIFICATION = "prosecutorsEmailAddressUsedSummonsNotification";
    public static final String TRUE = "true";
    public static final String PROGRESSION_COMMAND_APPROVE_APPLICATION_SUMMONS = "progression.command.approve-application-summons";
    public static final String PROGRESSION_COMMAND_REJECT_APPLICATION_SUMMONS = "progression.command.reject-application-summons";
    public static final  UUID SUMMONS_APPROVED = UUID.fromString("0f44eeb9-2c81-430d-9a60-bbdaf8c4a093");
    public static final  UUID SUMMONS_REJECTED = UUID.fromString("d8837a45-8281-49b3-8349-49b423193148");

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
    private HearingResultUnscheduledListingHelper hearingResultUnscheduledListingHelper;

    @Inject
    private NextHearingService nextHearingService;

    @Inject
    private HearingResultHelper hearingResultHelper;

    @Inject
    private SeedingHearingConverter seedingHearingConverter;

    @Handles("public.hearing.resulted")
    public void handleHearingResultedPublicEvent(final JsonEnvelope event) {

        final HearingResulted hearingResulted = jsonObjectToObjectConverter.convert(event.payloadAsJsonObject(), HearingResulted.class);
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("public.hearing.resulted event arrived with hearingResulted json : {}", event.toObfuscatedDebugString());
        }

        final Hearing hearing = hearingResulted.getHearing();

        LOGGER.info("Hearing resulted for hearing id :: {}", hearing.getId());

        this.sender.send(Enveloper.envelop(event.payloadAsJsonObject())
                .withName("progression.command.hearing-result")
                .withMetadataFrom(event));

        if (hearingResultUnscheduledListingHelper.checksIfUnscheduledHearingNeedsToBeCreated(hearing)) {
            progressionService.listUnscheduledHearings(event, hearing);
        }

        initiateSummonsProcess(event, hearing);
    }

    @Handles("progression.event.prosecution-cases-resulted")
    public void handleProsecutionCasesResulted(final JsonEnvelope event) {
        final ProsecutionCasesResulted prosecutionCasesResulted = jsonObjectToObjectConverter.convert(event.payloadAsJsonObject(), ProsecutionCasesResulted.class);
        final Hearing hearing = prosecutionCasesResulted.getHearing();
        final List<UUID> shadowListedOffences = prosecutionCasesResulted.getShadowListedOffences();
        final CommittingCourt committingCourt = prosecutionCasesResulted.getCommittingCourt();
        updateProsecutionCase(event, hearing);

        initiateHearingAdjournment(event, hearing, shadowListedOffences, ofNullable(committingCourt));
    }

    @Handles("progression.event.applications-resulted")
    public void processHandleApplicationsResulted(final JsonEnvelope event) {
        final ApplicationsResulted applicationsResulted = jsonObjectToObjectConverter.convert(event.payloadAsJsonObject(), ApplicationsResulted.class);
        final Hearing hearing = applicationsResulted.getHearing();
        final List<UUID> shadowListedOffences = applicationsResulted.getShadowListedOffences();
        LOGGER.info("Hearing contains applications resulted for hearing id :: {}", hearing.getId());
        hearing.getCourtApplications().forEach(courtApplication -> {
            final JsonObjectBuilder payloadBuilder = createObjectBuilder().add("courtApplication", objectToJsonObjectConverter.convert(courtApplication));
            sender.send(envelop(payloadBuilder.build()).withName("progression.command.hearing-resulted-update-application").withMetadataFrom(event));
        });

        updateApplications(event, hearing);

        if(isEmpty(hearing.getProsecutionCases())) {
            initiateHearingAdjournment(event, hearing, shadowListedOffences, empty());
        }
    }

    private void initiateHearingAdjournment(JsonEnvelope event, Hearing hearing, final List<UUID> shadowListedOffences, final Optional<CommittingCourt> committingCourt) {
        LOGGER.info("Hearing contains prosecution cases resulted for hearing id :: {}", hearing.getId());

        final boolean isHearingAdjourned = hearingResultHelper.doHearingContainNextHearingResults(hearing);

        if (isHearingAdjourned) {
            final boolean shouldPopulateCommittingCourt = checkResultLinesForCommittingCourt(hearing);
            adjournHearingToExistingHearings(event, hearing, shadowListedOffences, shouldPopulateCommittingCourt, committingCourt);
            adjournHearingToNewHearings(event, hearing, shadowListedOffences, shouldPopulateCommittingCourt, committingCourt);
        } else {
            LOGGER.info("Hearing contains prosecution cases does not contain next hearing details for hearing id :: {}", hearing.getId());
        }
    }

    private void updateProsecutionCase(JsonEnvelope event, Hearing hearing) {
        ofNullable(hearing.getProsecutionCases()).map(Collection::stream).orElseGet(Stream::empty).forEach(prosecutionCase -> progressionService.updateCase(event, prosecutionCase, hearing.getCourtApplications()));
    }

    private void updateApplications(final JsonEnvelope event, final Hearing hearing) {
        if (isNotEmpty(hearing.getCourtApplications())) {
            LOGGER.info("Hearing contains court applications resulted for hearing id :: {}", hearing.getId());
            final List<UUID> applicationIdsResultCategoryFinal = new ArrayList<>();
            final List<UUID> allApplicationIds = new ArrayList<>();
            hearing.getCourtApplications().forEach(courtApplication -> {
                final List<JudicialResult> judicialResults = hearingResultHelper.getAllJudicialResultsFromApplication(courtApplication);
                allApplicationIds.add(courtApplication.getId());
                final boolean isResultCategoryFinal = judicialResults.stream().filter(Objects::nonNull).map(JudicialResult::getCategory).anyMatch(FINAL::equals);
                if (isResultCategoryFinal) {
                    applicationIdsResultCategoryFinal.add(courtApplication.getId());
                }
            });
            progressionService.linkApplicationsToHearing(event, hearing, allApplicationIds, HearingListingStatus.HEARING_RESULTED);
            progressionService.updateCourtApplicationStatus(event, applicationIdsResultCategoryFinal, ApplicationStatus.FINALISED);
        }
    }

    /**
     * Check if any of the judicial result's group matches the specified result definition groups
     *
     * @param hearing - hearing
     * @return - return true if judicial result's group has CommittedToCC or SENTTOCC
     */
    private boolean checkResultLinesForCommittingCourt(final Hearing hearing) {
        final AtomicBoolean shouldPopulateCommittingCourt = new AtomicBoolean(false);

        ofNullable(hearing.getProsecutionCases()).map(Collection::stream).orElseGet(Stream::empty).forEach(
                prosecutionCase -> prosecutionCase.getDefendants().stream().filter(d -> nonNull(d.getOffences())).forEach(
                        defendant -> defendant.getOffences().stream().filter(o -> nonNull(o.getJudicialResults())).forEach(
                                offence -> offence.getJudicialResults().forEach(
                                        judicialResult -> {
                                            if (hasCommittingCourt(judicialResult)) {
                                                shouldPopulateCommittingCourt.set(true);
                                            }
                                        }
                                )
                        )
                )
        );

        final List<JudicialResult> judicialResultList = ofNullable(hearing.getCourtApplications()).map(Collection::stream).orElseGet(Stream::empty)
                .map(courtApplication -> hearingResultHelper.getAllJudicialResultsFromApplication(courtApplication)).flatMap(List::stream).collect(toList());

        judicialResultList.forEach(judicialResult -> {
            if (hasCommittingCourt(judicialResult)) {
                shouldPopulateCommittingCourt.set(true);
            }
        });

        return shouldPopulateCommittingCourt.get();
    }

    private boolean hasCommittingCourt(JudicialResult judicialResult) {
        return nonNull(judicialResult.getResultDefinitionGroup()) &&
                (judicialResult.getResultDefinitionGroup().contains(COMMITTED_TO_CC) ||
                        judicialResult.getResultDefinitionGroup().contains(SENT_TO_CC));
    }

    private void adjournHearingToExistingHearings(final JsonEnvelope jsonEnvelope, final Hearing hearing, final List<UUID> shadowListedOffences, final boolean shouldPopulateCommittingCourt, final Optional<CommittingCourt> committingCourt) {
        LOGGER.info("Hearing adjourned to exiting hearing or hearings :: {}", hearing.getId());
        final NextHearingDetails nextHearingDetails = nextHearingService.getNextHearingDetails(hearing, shouldPopulateCommittingCourt, committingCourt);
        nextHearingDetails.getHearingListingNeedsList().forEach(hearingListingNeeds -> {
            final ExtendHearing extendHearing = ExtendHearing.extendHearing()
                    .withHearingRequest(hearingListingNeeds)
                    .withIsAdjourned(true)
                    .withShadowListedOffences(shadowListedOffences)
                    .build();
            sender.send(
                    envelop(objectToJsonObjectConverter.convert(extendHearing))
                            .withName("progression.command.extend-hearing")
                            .withMetadataFrom(jsonEnvelope));
        });
    }

    private void adjournHearingToNewHearings(final JsonEnvelope event, final Hearing hearing, final List<UUID> shadowListedOffences, final Boolean shouldPopulateCommittingCourt, final Optional<CommittingCourt> committingCourt) {
        LOGGER.info("Hearing adjourned to new hearing or hearings :: {}", hearing.getId());

        final SeedingHearing seedingHearing = seedingHearingConverter.convert(hearing);

        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Created seeding hearing: {}", seedingHearing);
        }
        final List<HearingListingNeeds> hearingListingNeedsList = hearingToHearingListingNeedsTransformer.transform(hearing, shouldPopulateCommittingCourt, committingCourt);
        listCourtHearings(event, hearingListingNeedsList, shadowListedOffences, seedingHearing);
    }

    private void listCourtHearings(final JsonEnvelope event, final List<HearingListingNeeds> hearingListingNeeds, final List<UUID> shadowListedOffences, final SeedingHearing seedingHearing) {
        if (isNotEmpty(hearingListingNeeds)) {
            final ListCourtHearing listCourtHearing = ListCourtHearing.listCourtHearing()
                    .withHearings(hearingListingNeeds)
                    .withAdjournedFromDate(LocalDate.now())
                    .withShadowListedOffences(shadowListedOffences)
                    .build();
            listingService.listCourtHearing(event, listCourtHearing);
            progressionService.updateHearingListingStatusToSentForListing(event, listCourtHearing, seedingHearing);
        }
    }

    private void initiateSummonsProcess(JsonEnvelope event, Hearing hearing) {
        final boolean boxWorkHearing = nonNull(hearing.getIsBoxHearing()) && hearing.getIsBoxHearing();

        final List<CourtApplication> courtApplications = ofNullable(hearing.getCourtApplications()).map(Collection::stream).orElseGet(Stream::empty).collect(Collectors.toList());

        if (boxWorkHearing) {
            final List<JudicialResult> summonsApprovedJudicialResults = getJudicialResults(hearing, SUMMONS_APPROVED);
            final List<JudicialResult> summonsRejectedJudicialResults = getJudicialResults(hearing, SUMMONS_REJECTED);

            if (isNotEmpty(courtApplications)) {
                courtApplications.forEach(courtApplication -> {
                    if (isNotEmpty(summonsApprovedJudicialResults)) {
                        final JsonObject summonsApprovedPayload = createSummonsApprovedJsonObject(courtApplication, summonsApprovedJudicialResults);
                        sendSummonsEvent(event, summonsApprovedPayload, PROGRESSION_COMMAND_APPROVE_APPLICATION_SUMMONS);
                    }
                    if (isEmpty(summonsApprovedJudicialResults) && isNotEmpty(summonsRejectedJudicialResults)) {
                        final JsonObject summonsRejectedPayload = createSummonsRejectedJsonObject(courtApplication, summonsRejectedJudicialResults);
                        sendSummonsEvent(event, summonsRejectedPayload, PROGRESSION_COMMAND_REJECT_APPLICATION_SUMMONS);
                    }
                });
            }
        }
    }

    private JsonObject createSummonsRejectedJsonObject(final CourtApplication courtApplication, final List<JudicialResult> summonsRejectedJudicialResults) {
        final List<JudicialResultPrompt> judicialResultPrompts = summonsRejectedJudicialResults.get(0).getJudicialResultPrompts();
        return createObjectBuilder()
                .add(APPLICATION_ID, courtApplication.getId().toString())
                .add("summonsRejectedOutcome", createObjectBuilder()
                        .add(REASONS, createArrayBuilder().add(getPromptValue(judicialResultPrompts, REASONS_FOR_REJECTION)))
                        .add(PROSECUTOR_EMAIL_ADDRESS, getPromptValue(judicialResultPrompts, PROSECUTORS_EMAIL_ADDRESS_USED_SUMMONS_NOTIFICATION))
                ).build();
    }

    private JsonObject createSummonsApprovedJsonObject(final CourtApplication courtApplication, final List<JudicialResult> summonsApprovedJudicialResults) {
        final List<JudicialResultPrompt> judicialResultPrompts = summonsApprovedJudicialResults.get(0).getJudicialResultPrompts();
        return createObjectBuilder()
                .add(APPLICATION_ID, courtApplication.getId().toString())
                .add("summonsApprovedOutcome", createObjectBuilder()
                        .add(PROSECUTOR_COST, getPromptValue(judicialResultPrompts, PROSECUTION_COSTS))
                        .add(SUMMONS_SUPPRESSED, getPromptValue(judicialResultPrompts, THIS_SUMMONS_WILL_BE_SERVED_BY_A_PROSECUTOR).equalsIgnoreCase(TRUE))
                        .add(PERSONAL_SERVICE, getPromptValue(judicialResultPrompts, THIS_SUMMONS_IS_FOR_PERSONAL_SERVICE).equalsIgnoreCase(TRUE))
                        .add(PROSECUTOR_EMAIL_ADDRESS, getPromptValue(judicialResultPrompts, PROSECUTORS_EMAIL_ADDRESS_USED_SUMMONS_NOTIFICATION))
                ).build();
    }

    public void sendSummonsEvent(final JsonEnvelope jsonEnvelope, final JsonObject payload, final String eventName) {
        sender.send(Enveloper.envelop(payload).withName(eventName).withMetadataFrom(jsonEnvelope));
    }

    private String getPromptValue(final List<JudicialResultPrompt> judicialResultPrompts, final String promptReference) {
        final Optional<String> value = judicialResultPrompts.stream().
                filter(judicialResultPrompt -> judicialResultPrompt.getPromptReference().equals(promptReference))
                .map(JudicialResultPrompt::getValue)
                .findFirst();

        return value.orElse("");
    }

    private List<JudicialResult> getJudicialResults(CourtApplication courtApplication, final UUID resultDefinitionId) {

        final List<JudicialResult> judicialResults1 = ofNullable(courtApplication.getCourtApplicationCases()).map(Collection::stream).orElseGet(Stream::empty)
                .flatMap(courtApplicationCase -> ofNullable(courtApplicationCase.getOffences()).map(Collection::stream).orElseGet(Stream::empty))
                .flatMap(offence -> ofNullable(offence.getJudicialResults()).map(Collection::stream).orElseGet(Stream::empty))
                .filter(judicialResult -> resultDefinitionId.equals(judicialResult.getJudicialResultTypeId()))
                .collect(toList());

        final List<JudicialResult> judicialResults2 = ofNullable(courtApplication.getCourtOrder()).map(courtOrder -> courtOrder.getCourtOrderOffences().stream()).orElseGet(Stream::empty)
                .flatMap(courtOrderOffence -> ofNullable(courtOrderOffence.getOffence()).map(offence -> ofNullable(offence.getJudicialResults()).map(Collection::stream).orElseGet(Stream::empty)).orElseGet(Stream::empty))
                .filter(judicialResult -> resultDefinitionId.equals(judicialResult.getJudicialResultTypeId()))
                .collect(toList());

        final List<JudicialResult> applicationJudicialResults = ofNullable(courtApplication.getJudicialResults()).map(Collection::stream).orElseGet(Stream::empty)
                .filter(judicialResult -> resultDefinitionId.equals(judicialResult.getJudicialResultTypeId()))
                .collect(Collectors.toList());

        return Stream.of(applicationJudicialResults, judicialResults1, judicialResults2).flatMap(Collection::stream).collect(Collectors.toList());

    }

    private List<JudicialResult> getJudicialResults(Hearing hearing, final UUID resultDefinitionId) {

        final List<CourtApplication> courtApplications = ofNullable(hearing.getCourtApplications()).map(Collection::stream).orElseGet(Stream::empty).collect(Collectors.toList());

        final List<JudicialResult> applicationJudicialResults = courtApplications.stream().map(courtApplication -> getJudicialResults(courtApplication, resultDefinitionId)).flatMap(Collection::stream).collect(Collectors.toList());

        final List<JudicialResult> defendantJudicialResults = ofNullable(hearing.getDefendantJudicialResults())
                .map(Collection::stream)
                .orElseGet(Stream::empty)
                .map(DefendantJudicialResult::getJudicialResult)
                .filter(judicialResult -> judicialResult.getJudicialResultTypeId().equals(resultDefinitionId))
                .collect(Collectors.toList());

        final List<JudicialResult> defendantCaseJudicialResults = ofNullable(hearing.getProsecutionCases()).map(Collection::stream).orElseGet(Stream::empty)
                .flatMap(prosecutionCase -> prosecutionCase.getDefendants().stream())
                .map(defendant -> ofNullable(defendant.getDefendantCaseJudicialResults()).map(Collection::stream).orElseGet(Stream::empty).collect(Collectors.toList()))
                .flatMap(Collection::stream)
                .filter(judicialResult -> judicialResult.getJudicialResultTypeId().equals(resultDefinitionId))
                .collect(Collectors.toList());

        final List<JudicialResult> judicialResults = ofNullable(hearing.getProsecutionCases()).map(Collection::stream).orElseGet(Stream::empty)
                .flatMap(prosecutionCase -> prosecutionCase.getDefendants().stream())
                .flatMap(defendant -> defendant.getOffences().stream())
                .map(offence -> ofNullable(offence.getJudicialResults()).map(Collection::stream).orElseGet(Stream::empty).collect(Collectors.toList()))
                .flatMap(Collection::stream)
                .filter(judicialResult -> judicialResult.getJudicialResultTypeId().equals(resultDefinitionId))
                .collect(Collectors.toList());

        return Stream.of(applicationJudicialResults, defendantJudicialResults, defendantCaseJudicialResults, judicialResults).flatMap(Collection::stream).collect(Collectors.toList());
    }
}
