package uk.gov.moj.cpp.progression.event;

import static java.lang.Integer.parseInt;
import static java.util.Collections.emptyList;
import static java.util.Objects.nonNull;
import static java.util.Optional.ofNullable;
import static javax.json.Json.createObjectBuilder;
import static org.apache.commons.collections.CollectionUtils.isNotEmpty;
import static uk.gov.justice.core.courts.JudicialResultCategory.FINAL;
import static uk.gov.justice.services.core.annotation.Component.EVENT_PROCESSOR;
import static uk.gov.justice.services.core.enveloper.Enveloper.envelop;
import static uk.gov.moj.cpp.progression.processor.utils.RetryHelper.retryHelper;

import uk.gov.justice.core.courts.ApplicationStatus;
import uk.gov.justice.core.courts.CommittingCourt;
import uk.gov.justice.core.courts.CourtApplication;
import uk.gov.justice.core.courts.ExtendHearing;
import uk.gov.justice.core.courts.Hearing;
import uk.gov.justice.core.courts.HearingListingNeeds;
import uk.gov.justice.core.courts.JudicialResult;
import uk.gov.justice.core.courts.ListCourtHearing;
import uk.gov.justice.core.courts.ProsecutionCasesResulted;
import uk.gov.justice.core.courts.SeedingHearing;
import uk.gov.justice.progression.courts.ApplicationsResulted;
import uk.gov.justice.progression.courts.HearingResulted;
import uk.gov.justice.progression.courts.exract.LaaDefendantProceedingConcludedChanged;
import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.common.converter.ObjectToJsonObjectConverter;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.progression.converter.SeedingHearingConverter;
import uk.gov.moj.cpp.progression.exception.LaaAzureApimInvocationException;
import uk.gov.moj.cpp.progression.helper.HearingResultHelper;
import uk.gov.moj.cpp.progression.helper.HearingResultUnscheduledListingHelper;
import uk.gov.moj.cpp.progression.helper.SummonsHelper;
import uk.gov.moj.cpp.progression.service.ApplicationParameters;
import uk.gov.moj.cpp.progression.service.AzureFunctionService;
import uk.gov.moj.cpp.progression.service.ListingService;
import uk.gov.moj.cpp.progression.service.NextHearingService;
import uk.gov.moj.cpp.progression.service.ProgressionService;
import uk.gov.moj.cpp.progression.service.dto.NextHearingDetails;
import uk.gov.moj.cpp.progression.transformer.DefendantProceedingConcludedTransformer;
import uk.gov.moj.cpp.progression.transformer.HearingToHearingListingNeedsTransformer;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Stream;

import javax.inject.Inject;
import javax.json.JsonObjectBuilder;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@SuppressWarnings("java:S1874")
@ServiceComponent(EVENT_PROCESSOR)
public class HearingResultEventProcessor {

    private static final Logger LOGGER = LoggerFactory.getLogger(HearingResultEventProcessor.class.getName());
    private static final String COMMITTED_TO_CC = "CommittedToCC";
    private static final String SENT_TO_CC = "SentToCC";

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
    private DefendantProceedingConcludedTransformer proceedingConcludedConverter;

    @Inject
    private NextHearingService nextHearingService;

    @Inject
    private HearingResultHelper hearingResultHelper;

    @Inject
    private SeedingHearingConverter seedingHearingConverter;

    @Inject
    private SummonsHelper summonsHelper;

    @Inject
    private AzureFunctionService azureFunctionService;

    @Inject
    private ApplicationParameters applicationParameters;

    @Handles("public.hearing.resulted")
    public void handleHearingResultedPublicEvent(final JsonEnvelope event) {

        final HearingResulted hearingResulted = jsonObjectToObjectConverter.convert(event.payloadAsJsonObject(), HearingResulted.class);
        if (LOGGER.isInfoEnabled()) {
            LOGGER.info("public.hearing.resulted event arrived with hearingResulted json : {}", event.toObfuscatedDebugString());
        }

        final Hearing hearing = hearingResulted.getHearing();
        if (Boolean.TRUE.equals(hearing.getIsSJPHearing())) {
            LOGGER.info("Hearing resulted event originating from SJP.  Hence ignoring");
            return;
        }

        LOGGER.info("Hearing resulted for hearing id :: {}", hearing.getId());

        this.sender.send(Enveloper.envelop(event.payloadAsJsonObject())
                .withName("progression.command.hearing-result")
                .withMetadataFrom(event));

        if (hearingResultUnscheduledListingHelper.checksIfUnscheduledHearingNeedsToBeCreated(hearing)) {
            progressionService.listUnscheduledHearings(event, hearing);
        }

        summonsHelper.initiateSummonsProcess(event, hearing);
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
        LOGGER.info("Hearing contains applications resulted for hearing id :: {}", hearing.getId());
        hearing.getCourtApplications().forEach(courtApplication -> {
            final JsonObjectBuilder payloadBuilder = createObjectBuilder().add("courtApplication", objectToJsonObjectConverter.convert(courtApplication));
            sender.send(envelop(payloadBuilder.build()).withName("progression.command.hearing-resulted-update-application").withMetadataFrom(event));
            try {
                laaProceedingConcluded(courtApplication, hearing.getId());
            } catch (InterruptedException e) {
                LOGGER.error("InterruptedException: ", e);
                Thread.currentThread().interrupt();
            }
        });

        //updateApplicationStatus
        if (isNotEmpty(hearing.getCourtApplications())) {
            LOGGER.info("Hearing contains court applications resulted for hearing id :: {}", hearing.getId());
            hearing.getCourtApplications()
                    .forEach(courtApplication -> {
                        final ApplicationStatus applicationStatus = ofNullable(courtApplication.getJudicialResults()).map(ArrayList::new).orElseGet(ArrayList::new).stream()
                                .filter(Objects::nonNull).map(JudicialResult::getCategory).anyMatch(FINAL::equals)
                                ? ApplicationStatus.FINALISED
                                : courtApplication.getApplicationStatus();
                        progressionService.updateCourtApplicationStatus(event, courtApplication.getId(), applicationStatus);
                    });
        }

    }

    private void laaProceedingConcluded(final CourtApplication courtApplication, final UUID hearingId) throws InterruptedException {

        if (notEligibleForLAA(courtApplication)) {
            LOGGER.info("Application result not eligible to be sent to LAA. applicationId = {} ", courtApplication.getId());
            return;
        }

        final LaaDefendantProceedingConcludedChanged laaDefendantProceedingConcludedChanged = LaaDefendantProceedingConcludedChanged.laaDefendantProceedingConcludedChanged()
                .withProsecutionConcludedRequest(proceedingConcludedConverter.getApplicationConcludedRequest(courtApplication, hearingId))
                .build();
        final String payload = objectToJsonObjectConverter.convert(laaDefendantProceedingConcludedChanged.getProsecutionConcludedRequest()).toString();

        LOGGER.info("calling azure function for sending the concluded proceedings for applications to LAA. Payload = {} ", payload);

        retryHelper()
                .withSupplier(() -> azureFunctionService.concludeDefendantProceeding(payload))
                .withApimUrl(applicationParameters.getDefendantProceedingsConcludedApimUrl())
                .withPayload(payload)
                .withRetryTimes(parseInt(applicationParameters.getRetryTimes()))
                .withRetryInterval(parseInt(applicationParameters.getRetryInterval()))
                .withExceptionSupplier(() -> new LaaAzureApimInvocationException(courtApplication.getId(), hearingId, applicationParameters.getDefendantProceedingsConcludedApimUrl()))
                .withPredicate(statusCode -> statusCode > 429)
                .build()
                .postWithRetry();
    }

    private boolean notEligibleForLAA(final CourtApplication courtApplication) {
        final boolean isEligible = ofNullable(courtApplication.getCourtApplicationCases()).orElse(emptyList()).stream()
                .flatMap(courtApplicationCase ->
                        ofNullable(courtApplicationCase.getOffences()).orElse(emptyList()).stream()
                )
                .anyMatch(offence -> nonNull(offence.getLaaApplnReference()));
        return !isEligible;
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
        ofNullable(hearing.getProsecutionCases()).map(Collection::stream).orElseGet(Stream::empty)
                .forEach(prosecutionCase -> progressionService.updateCase(event, prosecutionCase, hearing.getCourtApplications(),
                        hearing.getDefendantJudicialResults(), hearing.getCourtCentre(),
                        hearing.getId(), hearing.getType(), hearing.getJurisdictionType(), hearing.getIsBoxHearing()));
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
                .map(courtApplication -> hearingResultHelper.getAllJudicialResultsFromApplication(courtApplication)).flatMap(List::stream).toList();

        judicialResultList.forEach(judicialResult -> {
            if (hasCommittingCourt(judicialResult)) {
                shouldPopulateCommittingCourt.set(true);
            }
        });

        return shouldPopulateCommittingCourt.get();
    }

    private boolean hasCommittingCourt(final JudicialResult judicialResult) {
        if (Objects.nonNull(judicialResult.getResultDefinitionGroup())) {
            return Arrays.asList(judicialResult.getResultDefinitionGroup()
                            .toLowerCase().replace(" ", "")
                            .split(","))
                    .stream()
                    .anyMatch(value -> COMMITTED_TO_CC.equalsIgnoreCase(value) || SENT_TO_CC.equalsIgnoreCase(value));
        }
        return false;
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
            progressionService.updateHearingListingStatusToSentForListing(event, listCourtHearing.getHearings(), seedingHearing);
        }
    }
}
