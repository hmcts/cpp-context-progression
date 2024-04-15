package uk.gov.moj.cpp.progression.aggregate;

import static java.lang.Boolean.TRUE;
import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static java.util.Optional.ofNullable;
import static java.util.stream.Stream.empty;
import static org.apache.commons.collections.CollectionUtils.isNotEmpty;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static uk.gov.justice.core.courts.ApplicationStatus.DRAFT;
import static uk.gov.justice.core.courts.ApplicationStatus.EJECTED;
import static uk.gov.justice.core.courts.ApplicationStatus.FINALISED;
import static uk.gov.justice.core.courts.ApplicationStatus.IN_PROGRESS;
import static uk.gov.justice.core.courts.ApplicationStatus.LISTED;
import static uk.gov.justice.core.courts.CourtApplication.courtApplication;
import static uk.gov.justice.core.courts.CourtApplicationCreated.courtApplicationCreated;
import static uk.gov.justice.core.courts.CourtApplicationSummonsApproved.courtApplicationSummonsApproved;
import static uk.gov.justice.core.courts.CourtApplicationSummonsRejected.courtApplicationSummonsRejected;
import static uk.gov.justice.core.courts.HearingResultedApplicationUpdated.hearingResultedApplicationUpdated;
import static uk.gov.justice.core.courts.InitiateCourtHearingAfterSummonsApproved.initiateCourtHearingAfterSummonsApproved;
import static uk.gov.justice.domain.aggregate.matcher.EventSwitcher.match;
import static uk.gov.justice.domain.aggregate.matcher.EventSwitcher.otherwiseDoNothing;
import static uk.gov.justice.domain.aggregate.matcher.EventSwitcher.when;
import static uk.gov.justice.progression.courts.SendStatdecAppointmentLetter.sendStatdecAppointmentLetter;
import static uk.gov.moj.cpp.progression.domain.aggregate.utils.CourtApplicationHelper.getCourtApplicationWithConvictionDate;
import static uk.gov.moj.cpp.progression.util.ReportingRestrictionHelper.dedupAllReportingRestrictions;

import uk.gov.justice.core.courts.ApplicationEjected;
import uk.gov.justice.core.courts.ApplicationReferredIgnored;
import uk.gov.justice.core.courts.ApplicationReferredToBoxwork;
import uk.gov.justice.core.courts.ApplicationReferredToCourt;
import uk.gov.justice.core.courts.ApplicationReferredToCourtHearing;
import uk.gov.justice.core.courts.ApplicationReferredToExistingHearing;
import uk.gov.justice.core.courts.ApplicationStatus;
import uk.gov.justice.core.courts.BoxHearingRequest;
import uk.gov.justice.core.courts.ConvictionDateAdded;
import uk.gov.justice.core.courts.ConvictionDateRemoved;
import uk.gov.justice.core.courts.CourtApplication;
import uk.gov.justice.core.courts.CourtApplicationAddedToCase;
import uk.gov.justice.core.courts.CourtApplicationCase;
import uk.gov.justice.core.courts.CourtApplicationCreated;
import uk.gov.justice.core.courts.CourtApplicationProceedingsEdited;
import uk.gov.justice.core.courts.CourtApplicationProceedingsInitiateIgnored;
import uk.gov.justice.core.courts.CourtApplicationProceedingsInitiated;
import uk.gov.justice.core.courts.CourtApplicationStatusChanged;
import uk.gov.justice.core.courts.CourtApplicationSummonsRejected;
import uk.gov.justice.core.courts.CourtHearingRequest;
import uk.gov.justice.core.courts.CourtOrderOffence;
import uk.gov.justice.core.courts.DefendantUpdate;
import uk.gov.justice.core.courts.EditCourtApplicationProceedings;
import uk.gov.justice.core.courts.FutureSummonsHearing;
import uk.gov.justice.core.courts.Hearing;
import uk.gov.justice.core.courts.HearingApplicationLinkCreated;
import uk.gov.justice.core.courts.HearingExtended;
import uk.gov.justice.core.courts.HearingListingNeeds;
import uk.gov.justice.core.courts.HearingListingStatus;
import uk.gov.justice.core.courts.HearingResultedApplicationUpdated;
import uk.gov.justice.core.courts.InitiateCourtApplicationProceedings;
import uk.gov.justice.core.courts.LinkType;
import uk.gov.justice.core.courts.SendNotificationForApplicationIgnored;
import uk.gov.justice.core.courts.SendNotificationForApplicationInitiated;
import uk.gov.justice.core.courts.SlotsBookedForApplication;
import uk.gov.justice.core.courts.SummonsApprovedOutcome;
import uk.gov.justice.core.courts.SummonsRejectedOutcome;
import uk.gov.justice.domain.aggregate.Aggregate;
import uk.gov.justice.progression.courts.HearingDeletedForCourtApplication;
import uk.gov.moj.cpp.progression.domain.Notification;
import uk.gov.moj.cpp.progression.domain.NotificationRequestAccepted;
import uk.gov.moj.cpp.progression.domain.NotificationRequestFailed;
import uk.gov.moj.cpp.progression.domain.NotificationRequestSucceeded;
import uk.gov.moj.cpp.progression.domain.event.email.EmailRequested;
import uk.gov.moj.cpp.progression.domain.event.print.PrintRequested;
import uk.gov.justice.core.courts.CourtApplicationSubjectCustodialInformationUpdated;

import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.lang3.RandomStringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@SuppressWarnings({"squid:S1948"})
public class ApplicationAggregate implements Aggregate {

    private static final Logger LOGGER = LoggerFactory.getLogger(ApplicationAggregate.class);
    private static final long serialVersionUID = 1331113876243908496L;
    private static final String APPEARANCE_TO_MAKE_STATUTORY_DECLARATION_CODE = "MC80527";
    private static final String APPEARANCE_TO_MAKE_STATUTORY_DECLARATION_CODE_SJP = "MC80528";
    private ApplicationStatus applicationStatus = DRAFT;
    private InitiateCourtApplicationProceedings initiateCourtApplicationProceedings;
    private CourtApplication courtApplication;
    private UUID boxHearingId;
    private boolean applicationReferredToNewHearing;
    public static final String CREATE_HEARING_APPLICATION_LINK_ERROR = "Hearing application link failed because application status is either finalized or ejected!";

    @Override
    public Object apply(final Object event) {
        return match(event).with(
                when(CourtApplicationProceedingsInitiated.class).apply(this::handleCourtApplicationProceedings),
                when(ApplicationReferredToCourt.class).apply(e -> this.applicationStatus = LISTED),
                when(CourtApplicationCreated.class).apply(e -> this.applicationStatus = DRAFT),
                when(HearingApplicationLinkCreated.class).apply(
                        e -> {
                            if (isBoxWorkHearing(e)) {
                                boxHearingId = e.getHearing().getId();
                            }
                        }
                ),
                when(ApplicationReferredToBoxwork.class).apply(e -> this.applicationStatus = IN_PROGRESS),
                when(ApplicationReferredToExistingHearing.class).apply(e -> this.applicationStatus = LISTED),
                when(CourtApplicationProceedingsEdited.class).apply(this::handleEditCourtApplicationProceedings),
                when(ConvictionDateAdded.class).apply(e -> handleConvictionDateChanged(e.getOffenceId(), e.getConvictionDate())),
                when(ConvictionDateRemoved.class).apply(e -> handleConvictionDateChanged(e.getOffenceId(), null)),
                when(CourtApplicationSummonsRejected.class).apply(e -> this.applicationStatus = FINALISED),
                when(HearingResultedApplicationUpdated.class).apply(e -> {
                    setCourtApplication(e.getCourtApplication());
                    this.applicationStatus = FINALISED;
                }),
                when(ApplicationEjected.class).apply(
                        e ->
                                this.applicationStatus = EJECTED

                ),
                otherwiseDoNothing());
    }


    public Stream<Object> referApplicationToCourt(final HearingListingNeeds hearingListingNeeds) {
        LOGGER.debug("application has been referred to court");
        return apply(Stream.of(
                ApplicationReferredToCourt.applicationReferredToCourt()
                        .withHearingRequest(hearingListingNeeds)
                        .build()));
    }

    public Stream<Object> extendHearing(final HearingListingNeeds hearingListingNeeds) {
        LOGGER.debug("hearing has been extended");
        return apply(Stream.of(
                HearingExtended.hearingExtended()
                        .withHearingRequest(hearingListingNeeds)
                        .withIsAdjourned(Boolean.FALSE)
                        .build()));
    }

    public Stream<Object> bookSlotsForApplication(final HearingListingNeeds hearingListingNeeds) {
        LOGGER.debug("slots has been requested to book for application");
        return apply(Stream.of(
                SlotsBookedForApplication.slotsBookedForApplication()
                        .withHearingRequest(hearingListingNeeds)
                        .build()));
    }

    public Stream<Object> updateApplicationStatus(final UUID applicationId, final ApplicationStatus applicationStatus) {
        LOGGER.debug("Application status being updated");
        return apply(Stream.of(
                CourtApplicationStatusChanged.courtApplicationStatusChanged()
                        .withId(applicationId)
                        .withApplicationStatus(applicationStatus)
                        .build()));
    }

    @SuppressWarnings({"squid:S1067"})
    public Stream<Object> createCourtApplication(final CourtApplication courtApplication) {
        LOGGER.debug("Court application has been created");
        final Stream.Builder<Object> streamBuilder = Stream.builder();
        streamBuilder.add(
                courtApplicationCreated()
                        .withCourtApplication(courtApplication)
                        .build());
        final BoxHearingRequest boxHearingRequest = nonNull(initiateCourtApplicationProceedings) ? initiateCourtApplicationProceedings.getBoxHearing() : null;

        if (nonNull(courtApplication.getType()) && isNotBlank(courtApplication.getType().getCode())
                && (APPEARANCE_TO_MAKE_STATUTORY_DECLARATION_CODE.equalsIgnoreCase(courtApplication.getType().getCode())
                || APPEARANCE_TO_MAKE_STATUTORY_DECLARATION_CODE_SJP.equalsIgnoreCase(courtApplication.getType().getCode()))
                && nonNull(boxHearingRequest) && TRUE.equals(boxHearingRequest.getSendAppointmentLetter())
                && nonNull(boxHearingRequest.getVirtualAppointmentTime())) {
            streamBuilder.add(sendStatdecAppointmentLetter()
                    .withCourtApplication(courtApplication)
                    .withBoxHearing(boxHearingRequest)
                    .build());

        }
        return apply(streamBuilder.build());
    }

    public Stream<Object> addApplicationToCase(final CourtApplication application) {
        LOGGER.debug("Court application has been added to case");
        return apply(Stream.of(CourtApplicationAddedToCase.courtApplicationAddedToCase().withCourtApplication(application).build()));
    }

    public Stream<Object> ejectApplication(final UUID courtApplicationId, final String removalReason) {
        if (EJECTED.equals(applicationStatus)) {
            LOGGER.info("Application with id {} already ejected", courtApplicationId);
            return empty();
        }

        return apply(Stream.of(ApplicationEjected.applicationEjected()
                .withApplicationId(courtApplicationId).withRemovalReason(removalReason).build()));
    }

    public Stream<Object> createHearingApplicationLink(final Hearing hearing, final UUID applicationId, final HearingListingStatus hearingListingStatus) {
        LOGGER.debug("Hearing Application link been created");
        return apply(Stream.of(
                HearingApplicationLinkCreated.hearingApplicationLinkCreated()
                        .withHearing(hearing)
                        .withApplicationId(applicationId)
                        .withHearingListingStatus(hearingListingStatus)
                        .build()));
    }

    public Stream<Object> recordEmailRequest(final UUID applicationId, final UUID materialId, final List<Notification> notifications) {
        return apply(Stream.of(new EmailRequested(null, materialId, applicationId, notifications)));
    }

    public Stream<Object> recordNotificationRequestAccepted(final UUID applicationId, final UUID materialId, final UUID notificationId, final ZonedDateTime acceptedTime) {
        return apply(Stream.of(new NotificationRequestAccepted(null, applicationId, materialId, notificationId, acceptedTime)));
    }

    public Stream<Object> recordNotificationRequestFailure(final UUID applicationId, final UUID notificationId, final ZonedDateTime failedTime, final String errorMessage, final Optional<Integer> statusCode) {
        return apply(Stream.of(new NotificationRequestFailed(null, applicationId, null, notificationId, failedTime, errorMessage, statusCode)));
    }

    public Stream<Object> recordNotificationRequestSuccess(final UUID applicationId, final UUID notificationId, final ZonedDateTime sentTime, final ZonedDateTime completedAt) {
        return apply(Stream.of(new NotificationRequestSucceeded(null, applicationId, null, notificationId, sentTime, completedAt)));
    }

    public Stream<Object> recordPrintRequest(final UUID applicationId,
                                             final UUID notificationId,
                                             final UUID materialId,
                                             boolean postage) {
        return apply(Stream.of(new PrintRequested(notificationId, applicationId, null, materialId, postage)));
    }

    public Stream<Object> initiateCourtApplicationProceedings(final InitiateCourtApplicationProceedings initiateCourtApplicationProceedings, final boolean applicationReferredToNewHearing, final boolean applicationCreatedForSJPCase) {
        LOGGER.debug("Initiated Court Application Proceedings");
        if (isNull(this.courtApplication)) {
            CourtApplication updatedCourtApplication = updateCourtApplicationReference(initiateCourtApplicationProceedings.getCourtApplication());
            if (initiateCourtApplicationProceedings.getSummonsApprovalRequired() && nonNull(initiateCourtApplicationProceedings.getCourtHearing())) {
                updatedCourtApplication = updateCourtApplicationWithFutureSummonsHearing(updatedCourtApplication, initiateCourtApplicationProceedings.getCourtHearing());
            }
            return apply(
                    Stream.of(CourtApplicationProceedingsInitiated.courtApplicationProceedingsInitiated()
                            .withCourtApplication(updatedCourtApplication)
                            .withCourtHearing(initiateCourtApplicationProceedings.getCourtHearing())
                            .withBoxHearing(initiateCourtApplicationProceedings.getBoxHearing())
                            .withSummonsApprovalRequired(initiateCourtApplicationProceedings.getSummonsApprovalRequired())
                            .withApplicationReferredToNewHearing(applicationReferredToNewHearing)
                            .withIsSJP(applicationCreatedForSJPCase)
                            .build()));
        } else {
            LOGGER.debug("Initiated Court Application Event not raised as it is a duplicate request");
            return ignoreApplicationProceedings(initiateCourtApplicationProceedings);
        }
    }

    public Stream<Object> ignoreApplicationProceedings(InitiateCourtApplicationProceedings initiateCourtProceedingsForApplication) {
        return apply(
                Stream.of(CourtApplicationProceedingsInitiateIgnored.courtApplicationProceedingsInitiateIgnored()
                        .withCourtApplication(initiateCourtProceedingsForApplication.getCourtApplication())
                        .withCourtHearing(initiateCourtProceedingsForApplication.getCourtHearing())
                        .withBoxHearing(initiateCourtProceedingsForApplication.getBoxHearing())
                        .build()));
    }


    public Stream<Object> editCourtApplicationProceedings(final EditCourtApplicationProceedings editCourtApplicationProceedings) {
        final CourtApplication updatedCourtApplication;
        if (editCourtApplicationProceedings.getSummonsApprovalRequired() && nonNull(editCourtApplicationProceedings.getCourtHearing())) {
            updatedCourtApplication = updateCourtApplicationWithFutureSummonsHearing(editCourtApplicationProceedings.getCourtApplication(), editCourtApplicationProceedings.getCourtHearing());
        } else {
            updatedCourtApplication = editCourtApplicationProceedings.getCourtApplication();
        }
        return apply(
                Stream.of(
                        CourtApplicationProceedingsEdited.courtApplicationProceedingsEdited()
                                .withCourtApplication(updatedCourtApplication)
                                .withCourtHearing(editCourtApplicationProceedings.getCourtHearing())
                                .withBoxHearing(editCourtApplicationProceedings.getBoxHearing())
                                .withSummonsApprovalRequired(editCourtApplicationProceedings.getSummonsApprovalRequired())
                                .build()));

    }

    public Stream<Object> referApplication() {
        final BoxHearingRequest boxHearing = initiateCourtApplicationProceedings.getBoxHearing();
        final CourtHearingRequest courtHearing = initiateCourtApplicationProceedings.getCourtHearing();
        final Stream.Builder<Object> streams = Stream.builder();

        if (isApplicationLinkedToCase()) {
            streams.add(CourtApplicationAddedToCase.courtApplicationAddedToCase().withCourtApplication(courtApplication).build());
        }

        if (nonNull(boxHearing)) {
            streams.add(ApplicationReferredToBoxwork.applicationReferredToBoxwork()
                    .withApplication(courtApplication().withValuesFrom(courtApplication)
                            .withApplicationStatus(IN_PROGRESS)
                            .build())
                    .withBoxHearing(boxHearing)
                    .build());
        } else if (applicationReferredToNewHearing) {
            streams.add(ApplicationReferredToCourtHearing.applicationReferredToCourtHearing()
                    .withApplication(courtApplication().withValuesFrom(courtApplication)
                            .withApplicationStatus(LISTED)
                            .build())
                    .withCourtHearing(courtHearing)
                    .build());
        } else if (isApplicationReferredToExistingHearing(courtHearing)) {
            streams.add(ApplicationReferredToExistingHearing.applicationReferredToExistingHearing()
                    .withApplication(courtApplication().withValuesFrom(courtApplication)
                            .withApplicationStatus(LISTED)
                            .build())
                    .withCourtHearing(courtHearing)
                    .build());
        } else {
            streams.add(ApplicationReferredIgnored.applicationReferredIgnored()
                    .withApplication(courtApplication)
                    .build());
        }

        return streams.build();
    }

    public CourtApplication getCourtApplication() {
        return courtApplication;
    }

    public Stream<Object> referApplicationToCourtHearing() {
        return apply(
                Stream.of(ApplicationReferredToCourtHearing.applicationReferredToCourtHearing()
                        .withApplication(courtApplication)
                        .withCourtHearing(initiateCourtApplicationProceedings.getCourtHearing())
                        .build()));
    }

    public Stream<Object> approveSummons(final SummonsApprovedOutcome summonsApprovedOutcome) {
        final Stream.Builder<Object> streams = Stream.builder();
        final CourtHearingRequest courtHearing = initiateCourtApplicationProceedings.getCourtHearing();

        if (courtApplication.getType().getLinkType() != LinkType.FIRST_HEARING && nonNull(courtHearing)) {
            streams.add(initiateCourtHearingAfterSummonsApproved()
                    .withApplication(courtApplication().withValuesFrom(courtApplication)
                            .withApplicationStatus(LISTED)
                            .withFutureSummonsHearing(null)
                            .build())
                    .withCourtHearing(courtHearing)
                    .withSummonsApprovedOutcome(summonsApprovedOutcome)
                    .build());
        }

        streams.add(courtApplicationSummonsApproved()
                .withApplicationId(courtApplication.getId())
                .withLinkType(courtApplication.getType().getLinkType())
                .withCaseIds(getCaseIds())
                .withSummonsApprovedOutcome(summonsApprovedOutcome)
                .build());

        return streams.build();
    }

    public Stream<Object> rejectSummons(final SummonsRejectedOutcome summonsRejectedOutcome) {
        return apply(Stream.of(courtApplicationSummonsRejected()
                .withCourtApplication(courtApplication)
                .withCaseIds(getCaseIds())
                .withSummonsRejectedOutcome(summonsRejectedOutcome)
                .build()));
    }

    public Stream<Object> addConvictionDate(final UUID applicationId, final UUID offenceId, final LocalDate convictionDate) {
        return apply(Stream.of(ConvictionDateAdded.convictionDateAdded()
                .withConvictionDate(convictionDate)
                .withCourtApplicationId(applicationId)
                .withOffenceId(offenceId)
                .build()));
    }

    public Stream<Object> removeConvictionDate(final UUID applicationId, final UUID offenceId) {
        return apply(Stream.of(ConvictionDateRemoved.convictionDateRemoved()
                .withCourtApplicationId(applicationId)
                .withOffenceId(offenceId)
                .build()));
    }

    public Stream<Object> hearingResulted(CourtApplication courtApplication) {
        return apply(Stream.of(hearingResultedApplicationUpdated().withCourtApplication(courtApplication().withValuesFrom(courtApplication).withApplicationStatus(FINALISED).build()).build()));
    }

    public Stream<Object> deleteHearingRelatedToCourtApplication(final UUID hearingId, final UUID courtApplicationId) {
        return apply(Stream.of(HearingDeletedForCourtApplication.hearingDeletedForCourtApplication()
                .withCourtApplicationId(courtApplicationId)
                .withHearingId(hearingId)
                .build()));
    }

    public Stream<Object> updateCustodialInfomrationForApplicatioNSubject(final DefendantUpdate defendantUpdate, final UUID applicationId) {
        return apply(Stream.of(CourtApplicationSubjectCustodialInformationUpdated.courtApplicationSubjectCustodialInformationUpdated()
                .withApplicationId(applicationId)
                .withDefendant(defendantUpdate)
                .build()));
    }

    public Stream<Object> sendNotificationForApplication(final SendNotificationForApplicationInitiated sendNotificationForApplicationInitiated) {
        if(isNull(this.initiateCourtApplicationProceedings) || Optional.ofNullable(sendNotificationForApplicationInitiated.getIsBoxWorkRequest()).orElse(false)){
            return apply(
                    Stream.of(SendNotificationForApplicationIgnored.sendNotificationForApplicationIgnored()
                            .withCourtApplication(sendNotificationForApplicationInitiated.getCourtApplication())
                            .withCourtHearing(sendNotificationForApplicationInitiated.getCourtHearing())
                            .withBoxHearing(sendNotificationForApplicationInitiated.getBoxHearing())
                            .withIsWelshTranslationRequired(sendNotificationForApplicationInitiated.getIsWelshTranslationRequired())
                            .build()));
        }
        return apply(
                Stream.of(SendNotificationForApplicationInitiated.sendNotificationForApplicationInitiated()
                        .withCourtApplication(this.initiateCourtApplicationProceedings.getCourtApplication())
                        .withCourtHearing(this.initiateCourtApplicationProceedings.getCourtHearing())
                        .withBoxHearing(this.initiateCourtApplicationProceedings.getBoxHearing())
                        .withSummonsApprovalRequired(this.initiateCourtApplicationProceedings.getSummonsApprovalRequired())
                        .withIsBoxWorkRequest(sendNotificationForApplicationInitiated.getIsBoxWorkRequest())
                        .withApplicationReferredToNewHearing(this.applicationReferredToNewHearing)
                        .withIsWelshTranslationRequired(sendNotificationForApplicationInitiated.getIsWelshTranslationRequired())
                        .build()));
    }

    public UUID getBoxHearingId() {
        return boxHearingId;
    }

    private List<UUID> getCaseIds() {
        final List<UUID> caseIds = ofNullable(courtApplication.getCourtApplicationCases()).map(Collection::stream).orElseGet(Stream::empty)
                .map(CourtApplicationCase::getProsecutionCaseId)
                .collect(Collectors.toList());

        final List<UUID> courtOrderCaseIds = ofNullable(courtApplication.getCourtOrder()).map(courtOrder -> courtOrder.getCourtOrderOffences().stream()).orElseGet(Stream::empty)
                .map(CourtOrderOffence::getProsecutionCaseId).collect(Collectors.toList());

        return Stream.of(caseIds, courtOrderCaseIds).flatMap(Collection::stream).collect(Collectors.collectingAndThen(Collectors.toList(), list -> list.isEmpty() ? null : list));
    }

    private CourtApplication updateCourtApplicationWithFutureSummonsHearing(CourtApplication courtApplication, CourtHearingRequest courtHearingRequest) {
        return CourtApplication.courtApplication().withValuesFrom(courtApplication)
                .withFutureSummonsHearing(FutureSummonsHearing.futureSummonsHearing()
                        .withCourtCentre(courtHearingRequest.getCourtCentre())
                        .withJudiciary(courtHearingRequest.getJudiciary())
                        .withEarliestStartDateTime(courtHearingRequest.getEarliestStartDateTime())
                        .withEstimatedMinutes(courtHearingRequest.getEstimatedMinutes())
                        .withJurisdictionType(courtHearingRequest.getJurisdictionType())
                        .withWeekCommencingDate(courtHearingRequest.getWeekCommencingDate())
                        .build()).build();
    }

    private CourtApplication updateCourtApplicationReference(CourtApplication courtApplication) {
        final String applicationReference = generateApplicationReference(courtApplication);
        return courtApplication()
                .withValuesFrom(courtApplication)
                .withApplicationReference(applicationReference)
                .build();
    }

    private String generateApplicationReference(final CourtApplication courtApplication) {

        if (isNotEmpty(courtApplication.getCourtApplicationCases())) {
            return courtApplication.getCourtApplicationCases().stream().map(courtApplicationCase ->
                            nonNull(courtApplicationCase.getProsecutionCaseIdentifier().getCaseURN()) ? courtApplicationCase.getProsecutionCaseIdentifier().getCaseURN() : courtApplicationCase.getProsecutionCaseIdentifier().getProsecutionAuthorityReference())
                    .distinct().collect(Collectors.joining(","));
        }

        if (nonNull(courtApplication.getCourtOrder())) {
            return courtApplication.getCourtOrder().getCourtOrderOffences().stream().map(courtOrderOffence ->
                            nonNull(courtOrderOffence.getProsecutionCaseIdentifier().getCaseURN()) ? courtOrderOffence.getProsecutionCaseIdentifier().getCaseURN() : courtOrderOffence.getProsecutionCaseIdentifier().getProsecutionAuthorityReference())
                    .distinct().collect(Collectors.joining(","));
        }

        final int ARN_LENGTH = 10;
        return RandomStringUtils.randomAlphanumeric(ARN_LENGTH).toUpperCase();
    }

    private boolean isApplicationReferredToExistingHearing(final CourtHearingRequest courtHearing) {
        return nonNull(courtHearing) && nonNull(courtHearing.getId());
    }

    private boolean isApplicationReferredToNewHearing(CourtApplicationProceedingsInitiated courtApplicationProceedingsInitiated) {
        return !isNull(courtApplicationProceedingsInitiated.getApplicationReferredToNewHearing()) && courtApplicationProceedingsInitiated.getApplicationReferredToNewHearing();
    }

    private boolean isApplicationLinkedToCase() {
        return (courtApplication.getType().getLinkType() != LinkType.FIRST_HEARING) && (nonNull(courtApplication.getCourtApplicationCases()) || nonNull(courtApplication.getCourtOrder()));
    }

    private void handleConvictionDateChanged(final UUID offenceId, final LocalDate convictionDate) {
        if (offenceId == null) {
            this.courtApplication = getCourtApplicationWithConvictionDate(this.courtApplication, convictionDate);
        } else {
            this.courtApplication = getCourtApplicationWithConvictionDate(this.courtApplication, offenceId, convictionDate);
        }
        this.initiateCourtApplicationProceedings = InitiateCourtApplicationProceedings.initiateCourtApplicationProceedings()
                .withValuesFrom(this.initiateCourtApplicationProceedings)
                .withCourtApplication(courtApplication)
                .build();
    }

    private void setCourtApplication(final CourtApplication courtApplication) {
        this.courtApplication = dedupAllReportingRestrictions(courtApplication);
    }

    private void handleEditCourtApplicationProceedings(CourtApplicationProceedingsEdited courtApplicationProceedingsEdited) {
        setCourtApplication(courtApplicationProceedingsEdited.getCourtApplication());
        this.initiateCourtApplicationProceedings = InitiateCourtApplicationProceedings.initiateCourtApplicationProceedings()
                .withCourtApplication(this.courtApplication)
                .withCourtHearing(courtApplicationProceedingsEdited.getCourtHearing())
                .withBoxHearing(courtApplicationProceedingsEdited.getBoxHearing())
                .withSummonsApprovalRequired(courtApplicationProceedingsEdited.getSummonsApprovalRequired())
                .build();
    }

    private boolean isBoxWorkHearing(final HearingApplicationLinkCreated e) {
        return nonNull(e.getHearing()) && nonNull(e.getHearing().getIsBoxHearing()) && e.getHearing().getIsBoxHearing();
    }

    private void handleCourtApplicationProceedings(final CourtApplicationProceedingsInitiated courtApplicationProceedingsInitiated) {
        setCourtApplication(courtApplicationProceedingsInitiated.getCourtApplication());
        this.applicationReferredToNewHearing = isApplicationReferredToNewHearing(courtApplicationProceedingsInitiated);
        this.initiateCourtApplicationProceedings = InitiateCourtApplicationProceedings.initiateCourtApplicationProceedings()
                .withCourtApplication(this.courtApplication)
                .withCourtHearing(courtApplicationProceedingsInitiated.getCourtHearing())
                .withBoxHearing(courtApplicationProceedingsInitiated.getBoxHearing())
                .withSummonsApprovalRequired(courtApplicationProceedingsInitiated.getSummonsApprovalRequired())
                .build();
    }

}
