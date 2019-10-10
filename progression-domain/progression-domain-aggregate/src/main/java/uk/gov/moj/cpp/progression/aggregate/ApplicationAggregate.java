package uk.gov.moj.cpp.progression.aggregate;

import static java.util.stream.Stream.empty;
import static uk.gov.justice.domain.aggregate.matcher.EventSwitcher.match;
import static uk.gov.justice.domain.aggregate.matcher.EventSwitcher.otherwiseDoNothing;
import static uk.gov.justice.domain.aggregate.matcher.EventSwitcher.when;

import uk.gov.justice.core.courts.ApplicationEjected;
import uk.gov.justice.core.courts.ApplicationReferredToCourt;
import uk.gov.justice.core.courts.ApplicationStatus;
import uk.gov.justice.core.courts.BoxworkApplicationReferred;
import uk.gov.justice.core.courts.BoxworkAssignmentChanged;
import uk.gov.justice.core.courts.CourtApplication;
import uk.gov.justice.core.courts.CourtApplicationAddedToCase;
import uk.gov.justice.core.courts.CourtApplicationCreated;
import uk.gov.justice.core.courts.CourtApplicationStatusChanged;
import uk.gov.justice.core.courts.CourtApplicationUpdated;
import uk.gov.justice.core.courts.Hearing;
import uk.gov.justice.core.courts.HearingApplicationLinkCreated;
import uk.gov.justice.core.courts.HearingExtended;
import uk.gov.justice.core.courts.HearingListingNeeds;
import uk.gov.justice.core.courts.HearingListingStatus;
import uk.gov.justice.core.courts.ListedCourtApplicationChanged;
import uk.gov.justice.domain.aggregate.Aggregate;
import uk.gov.moj.cpp.progression.domain.Notification;
import uk.gov.moj.cpp.progression.domain.NotificationRequestAccepted;
import uk.gov.moj.cpp.progression.domain.NotificationRequestFailed;
import uk.gov.moj.cpp.progression.domain.NotificationRequestSucceeded;
import uk.gov.moj.cpp.progression.domain.event.email.EmailRequested;
import uk.gov.moj.cpp.progression.domain.event.print.PrintRequested;

import java.time.ZonedDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Stream;

import org.apache.commons.lang3.RandomStringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@SuppressWarnings({"squid:S1948", "squid:S1068", "squid:S1450"})
public class ApplicationAggregate implements Aggregate {

    private static final Logger LOGGER = LoggerFactory.getLogger(ApplicationAggregate.class);
    private static final long serialVersionUID = 281032067089771390L;
    private ApplicationStatus applicationStatus = ApplicationStatus.DRAFT;
    private CourtApplication courtApplication;
    private UUID boxHearingId;
    private Set<UUID> hearingIds = new HashSet<>();

    @Override
    public Object apply(final Object event) {
        return match(event).with(
                when(ApplicationReferredToCourt.class).apply(e ->
                    this.applicationStatus = ApplicationStatus.LISTED
                ),
                when(BoxworkApplicationReferred.class).apply(e ->
                        this.applicationStatus = ApplicationStatus.IN_PROGRESS
                ),
                when(CourtApplicationStatusChanged.class).apply(e ->
                        this.applicationStatus = e.getApplicationStatus()
                ),
                when(CourtApplicationCreated.class).apply(
                        e -> {
                            this.courtApplication = e.getCourtApplication();
                            this.applicationStatus = ApplicationStatus.DRAFT;
                        }
                ),
                when(CourtApplicationUpdated.class).apply(
                        e -> {
                        }
                ),
                when(CourtApplicationAddedToCase.class).apply(
                        e -> {
                            this.courtApplication = e.getCourtApplication();
                            this.applicationStatus = ApplicationStatus.DRAFT;
                        }
                ),
                when(HearingApplicationLinkCreated.class).apply(
                        e -> {
                            hearingIds.add(e.getHearing().getId());
                            if(e.getHearing() !=null && e.getHearing().getIsBoxHearing() != null && e.getHearing().getIsBoxHearing()){
                                boxHearingId = e.getHearing().getId();
                            }
                        }
                ),
                when(ApplicationEjected.class).apply(
                        e ->
                                this.applicationStatus = ApplicationStatus.EJECTED

                ),
                otherwiseDoNothing());

    }

    public Stream<Object> referApplicationToCourt(HearingListingNeeds hearingListingNeeds) {
        LOGGER.debug("application has been referred to court");
        return apply(Stream.of(
                ApplicationReferredToCourt.applicationReferredToCourt()
                        .withHearingRequest(hearingListingNeeds)
                        .build()));
    }

    public Stream<Object> extendHearing(HearingListingNeeds hearingListingNeeds) {
        LOGGER.debug("hearing has been extended");
        return apply(Stream.of(
                HearingExtended.hearingExtended()
                        .withHearingRequest(hearingListingNeeds)
                        .build()));
    }

    public Stream<Object> referBoxWorkApplication(final HearingListingNeeds hearingListingNeeds) {
        LOGGER.debug("Box work application has been referred");
        final Stream.Builder<Object> eventStreamBuilder = Stream.builder();
        for (CourtApplication ca : hearingListingNeeds.getCourtApplications()) {
            updateCourtApplication(ca).forEach(o -> eventStreamBuilder.accept(o));
        }
        eventStreamBuilder.accept(BoxworkApplicationReferred.boxworkApplicationReferred()
                .withHearingRequest(hearingListingNeeds)
                .build());
        return apply(eventStreamBuilder.build());
    }

    public Stream<Object> updateApplicationStatus(final UUID applicationId, final ApplicationStatus applicationStatus) {
        LOGGER.debug("Application status being updated");
        return apply(Stream.of(
                CourtApplicationStatusChanged.courtApplicationStatusChanged()
                        .withId(applicationId)
                        .withApplicationStatus(applicationStatus)
                        .build()));
    }

    public Stream<Object> updateCourtApplication(final CourtApplication updatedCourtApplication) {
        LOGGER.debug("court application has been updated");
        final Stream.Builder<Object> streamBuilder = Stream.builder();
        if (this.applicationStatus.equals(ApplicationStatus.LISTED)) {
            streamBuilder.add(ListedCourtApplicationChanged.listedCourtApplicationChanged()
                    .withCourtApplication(updatedCourtApplication)
                    .build());
        }
        streamBuilder.add(CourtApplicationUpdated.courtApplicationUpdated()
                .withCourtApplication(updatedCourtApplication)
                .build());
        return apply(streamBuilder.build());
    }

    public Stream<Object> createCourtApplication(final CourtApplication courtApplication) {
        LOGGER.debug("Standalone court application has been created");
        final int ARN_LENGTH = 10;
        return apply(
                Stream.of(
                        CourtApplicationCreated.courtApplicationCreated()
                                .withCourtApplication(courtApplication)
                                .withArn(RandomStringUtils.randomAlphanumeric(ARN_LENGTH).toUpperCase())
                                .build()));
    }

    public Stream<Object> addApplicationToCase(CourtApplication application) {
        LOGGER.debug("Court application has been added to case");
        return apply(Stream.of(CourtApplicationAddedToCase.courtApplicationAddedToCase().withCourtApplication(application).build()));
    }

    public Stream<Object> ejectApplication(final UUID courtApplicationId, final String removalReason) {
        if (ApplicationStatus.EJECTED.equals(applicationStatus)) {
            LOGGER.info("Application with id {} already ejected", courtApplicationId);
            return empty();
        }
        return apply(Stream.of(ApplicationEjected.applicationEjected()
                .withApplicationId(courtApplicationId).withRemovalReason(removalReason).build()));
    }
    public Stream<Object> createHearingApplicationLink(final Hearing hearing, final UUID applicationId,
                                                       HearingListingStatus hearingListingStatus) {
        LOGGER.debug("Hearing Application link been created");
        return apply(Stream.of(
                HearingApplicationLinkCreated.hearingApplicationLinkCreated()
                        .withHearing(hearing)
                        .withApplicationId(applicationId)
                        .withHearingListingStatus(hearingListingStatus)
                        .build()));
    }

    public Stream<Object> recordEmailRequest(final UUID applicationId, final List<Notification> notifications) {
        return apply(Stream.of(new EmailRequested(null, null, applicationId, notifications)));
    }

    public Stream<Object> recordNotificationRequestAccepted(final UUID applicationId, final UUID notificationId, final ZonedDateTime acceptedTime) {
            return apply(Stream.of(new NotificationRequestAccepted(null, applicationId, notificationId, acceptedTime)));
    }

    public Stream<Object> recordNotificationRequestFailure(final UUID applicationId, final UUID notificationId, final ZonedDateTime failedTime, final String errorMessage, final Optional<Integer> statusCode) {
        return apply(Stream.of(new NotificationRequestFailed(null, applicationId, notificationId, failedTime, errorMessage, statusCode)));
    }

    public Stream<Object> recordNotificationRequestSuccess(final UUID applicationId, final UUID notificationId, final ZonedDateTime sentTime) {
        return apply(Stream.of(new NotificationRequestSucceeded(null, applicationId, notificationId, sentTime)));
    }

    public Stream<Object> recordPrintRequest(final UUID applicationId,
                                             final UUID notificationId,
                                             final UUID materialId) {
        return apply(Stream.of(new PrintRequested(notificationId, applicationId, null, materialId)));
    }

    public Stream<Object> changeBoxWorkAssignment(final UUID applicationId, final UUID userId) {
        return apply(Stream.of(new BoxworkAssignmentChanged(applicationId, userId)));

    }

    public UUID getBoxHearingId() {
        return boxHearingId;
    }
}
