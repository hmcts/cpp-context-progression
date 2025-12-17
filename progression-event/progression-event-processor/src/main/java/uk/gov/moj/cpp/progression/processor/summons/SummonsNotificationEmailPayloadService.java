package uk.gov.moj.cpp.progression.processor.summons;

import static com.google.common.collect.ImmutableList.of;
import static java.lang.String.format;
import static java.lang.System.lineSeparator;
import static java.time.format.DateTimeFormatter.ofPattern;
import static java.util.Optional.empty;
import static java.util.Optional.ofNullable;
import static java.util.UUID.fromString;
import static java.util.stream.Collectors.joining;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static uk.gov.justice.core.courts.Personalisation.personalisation;
import static uk.gov.justice.core.courts.SummonsType.SJP_REFERRAL;
import static uk.gov.justice.core.courts.notification.EmailChannel.emailChannel;
import static uk.gov.moj.cpp.progression.domain.constant.DateTimeFormats.SPACE_SEPARATED_3_CHAR_MONTH;

import com.google.common.collect.ImmutableList;
import uk.gov.justice.core.courts.Defendant;
import uk.gov.justice.core.courts.SummonsDataPrepared;
import uk.gov.justice.core.courts.SummonsType;
import uk.gov.justice.core.courts.notification.EmailChannel;
import uk.gov.justice.core.courts.summons.SummonsDocumentContent;
import uk.gov.moj.cpp.material.url.MaterialUrlGenerator;
import uk.gov.moj.cpp.progression.processor.exceptions.InvalidHearingDateException;
import uk.gov.moj.cpp.progression.service.ApplicationParameters;

import java.time.DateTimeException;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import javax.inject.Inject;

import org.apache.commons.lang3.StringUtils;

public class SummonsNotificationEmailPayloadService {

    private static final DateTimeFormatter DATE_FORMATTER = ofPattern(SPACE_SEPARATED_3_CHAR_MONTH.getValue());
    private static final ZoneId UK_TIME_ZONE = ZoneId.of("Europe/London");

    private static final String PROPERTY_CASE_REFERENCE = "caseReference";
    private static final String PROPERTY_DEFENDANT_DETAILS = "defendantDetails";
    private static final String PROPERTY_COURT_LOCATION = "courtLocation";
    private static final String PROPERTY_HEARING_DATE = "hearingDate";
    private static final String PROPERTY_HEARING_TIME = "hearingTime";
    private static final String PROPERTY_DEFENDANT_IS_YOUTH = "defendantIsYouth";
    private static final String PROPERTY_REJECTED_REASONS = "rejectedReasons";

    @Inject
    private ApplicationParameters applicationParameters;

    @Inject
    private MaterialUrlGenerator materialUrlGenerator;

    public Optional<EmailChannel> getEmailChannelForCaseDefendant(final SummonsDataPrepared summonsDataPrepared, final SummonsDocumentContent summonsDocumentContent,
                                                                  final String emailAddress, final List<UUID> confirmedDefendantIds,
                                                                  final Defendant defendant, final List<String> defendantDetails,
                                                                  final boolean sendForRemotePrinting, final boolean addresseeIsYouth, final UUID materialId,
                                                                  final SummonsType summonsRequired) {
        return getEmailChannelForCaseDefendant(summonsDataPrepared, summonsDocumentContent, emailAddress, confirmedDefendantIds,
                defendant, defendantDetails, sendForRemotePrinting, addresseeIsYouth, materialId, summonsRequired, false);
    }

    public Optional<EmailChannel> getEmailChannelForCaseDefendantParent(final SummonsDataPrepared summonsDataPrepared, final SummonsDocumentContent summonsDocumentContent,
                                                                        final String emailAddress, final List<UUID> confirmedDefendantIds, final Defendant defendant,
                                                                        final List<String> defendantDetails, final boolean sendForRemotePrinting, final UUID materialId,
                                                                        final SummonsType summonsRequired) {
        return getEmailChannelForCaseDefendant(summonsDataPrepared, summonsDocumentContent, emailAddress, confirmedDefendantIds,
                defendant, defendantDetails, sendForRemotePrinting, false, materialId, summonsRequired, true);
    }

    private Optional<EmailChannel> getEmailChannelForCaseDefendant(final SummonsDataPrepared summonsDataPrepared, final SummonsDocumentContent summonsDocumentContent,
                                                                   final String emailAddress, final List<UUID> confirmedDefendantIds, final Defendant defendant,
                                                                   final List<String> defendantDetails, final boolean sendForRemotePrinting, final boolean addresseeIsYouth,
                                                                   final UUID materialId, final SummonsType summonsRequired, final boolean notificationForParentOrGuardian) {
        if (conditionNotMetToRaiseEmailNotification(summonsRequired, emailAddress, sendForRemotePrinting, notificationForParentOrGuardian)) {
            return empty();
        }

        if (sendForRemotePrinting) {
            joinCaseDefendantDetails(defendantDetails, summonsDocumentContent, defendant, notificationForParentOrGuardian);
            if (lastItemInList(defendant.getId(), confirmedDefendantIds)) {
                return Optional.of(buildEmailNotificationForSummonsNotSuppressed(summonsDataPrepared, emailAddress, summonsDocumentContent, defendantDetails));
            }
        } else {
            return Optional.of(buildEmailNotificationForCaseSummonsSuppressed(summonsDataPrepared, emailAddress, summonsDocumentContent, defendant, addresseeIsYouth,
                    materialId, notificationForParentOrGuardian));
        }
        return empty();
    }

    public Optional<EmailChannel> getEmailChannelForApplicationAddressee(final SummonsDataPrepared summonsDataPrepared, final SummonsDocumentContent summonsDocumentContent,
                                                                         final String applicantEmailAddress, final boolean sendForRemotePrinting, final boolean addresseeIsYouth,
                                                                         final UUID materialId, final SummonsType summonsRequired) {
        return getEmailChannelForApplicationAddressee(summonsDataPrepared, summonsDocumentContent, applicantEmailAddress, sendForRemotePrinting,
                addresseeIsYouth, materialId, summonsRequired, false);
    }

    public Optional<EmailChannel> getEmailChannelForApplicationAddresseeParent(final SummonsDataPrepared summonsDataPrepared, final SummonsDocumentContent summonsDocumentContent,
                                                                               final String applicantEmailAddress, final boolean sendForRemotePrinting, final UUID materialId,
                                                                               final SummonsType summonsRequired) {
        return getEmailChannelForApplicationAddressee(summonsDataPrepared, summonsDocumentContent, applicantEmailAddress, sendForRemotePrinting,
                false, materialId, summonsRequired, true);
    }

    private Optional<EmailChannel> getEmailChannelForApplicationAddressee(final SummonsDataPrepared summonsDataPrepared, final SummonsDocumentContent summonsDocumentContent,
                                                                          final String applicantEmailAddress, final boolean sendForRemotePrinting, final boolean addresseeIsYouth,
                                                                          final UUID materialId, final SummonsType summonsRequired, final boolean notificationForParentOrGuardian) {
        if (conditionNotMetToRaiseEmailNotification(summonsRequired, applicantEmailAddress, sendForRemotePrinting, notificationForParentOrGuardian)) {
            return empty();
        }

        if (sendForRemotePrinting) {
            return Optional.of(buildEmailNotificationForSummonsNotSuppressed(summonsDataPrepared, applicantEmailAddress, summonsDocumentContent,
                    of(getAddresseeDetailsForApplication(summonsDocumentContent, notificationForParentOrGuardian))));
        }
        return Optional.of(buildEmailNotificationForApplicationSummonsSuppressed(summonsDataPrepared, applicantEmailAddress, summonsDocumentContent,
                addresseeIsYouth, materialId, notificationForParentOrGuardian));
    }

    public EmailChannel getEmailChannelForSummonsRejected(final String emailAddress,
                                                          final String applicationReference,
                                                          final List<String> partyDetails,
                                                          final List<String> rejectionReason) {
        return emailChannel()
                .withSendToAddress(emailAddress)
                .withTemplateId(fromString(applicationParameters.getSummonsRejectedTemplateId()))
                .withPersonalisation(personalisation()
                        .withAdditionalProperty(PROPERTY_CASE_REFERENCE, applicationReference)
                        .withAdditionalProperty(PROPERTY_DEFENDANT_DETAILS, partyDetails.stream().collect(joining(lineSeparator())))
                        .withAdditionalProperty(PROPERTY_REJECTED_REASONS, rejectionReason.stream().collect(joining(lineSeparator())))
                        .build())
                .build();
    }

    private EmailChannel buildEmailNotificationForCaseSummonsSuppressed(final SummonsDataPrepared summonsDataPrepared,
                                                                        final String emailAddress,
                                                                        final SummonsDocumentContent summonsDocumentContent,
                                                                        final Defendant defendant,
                                                                        final boolean defendantIsYouth,
                                                                        final UUID materialId,
                                                                        final boolean notificationForParentOrGuardian) {
        return emailChannel()
                .withSendToAddress(emailAddress)
                .withMaterialUrl(materialUrlGenerator.pdfFileStreamUrlFor(materialId))
                .withTemplateId(fromString(applicationParameters.getSummonsApprovedAndSuppressedTemplateId()))
                .withPersonalisation(personalisation()
                        .withAdditionalProperty(PROPERTY_CASE_REFERENCE, summonsDocumentContent.getCaseReference())
                        .withAdditionalProperty(PROPERTY_DEFENDANT_DETAILS, getCaseDefendantDetails(summonsDocumentContent, defendant, notificationForParentOrGuardian))
                        .withAdditionalProperty(PROPERTY_COURT_LOCATION, summonsDocumentContent.getHearingCourtDetails().getCourtName())
                        .withAdditionalProperty(PROPERTY_HEARING_DATE, getHearingDateForEmailNotification(summonsDataPrepared.getSummonsData().getHearingDateTime()))
                        .withAdditionalProperty(PROPERTY_HEARING_TIME, summonsDocumentContent.getHearingCourtDetails().getHearingTime())
                        .withAdditionalProperty(PROPERTY_DEFENDANT_IS_YOUTH, defendantIsYouth)
                        .build())
                .build();
    }

    private EmailChannel buildEmailNotificationForApplicationSummonsSuppressed(final SummonsDataPrepared summonsDataPrepared,
                                                                               final String emailAddress,
                                                                               final SummonsDocumentContent summonsDocumentContent,
                                                                               final boolean addresseeIsYouth,
                                                                               final UUID materialId,
                                                                               final boolean notificationForParentOrGuardian) {
        return emailChannel()
                .withSendToAddress(emailAddress)
                .withMaterialUrl(materialUrlGenerator.pdfFileStreamUrlFor(materialId))
                .withTemplateId(fromString(applicationParameters.getSummonsApprovedAndSuppressedTemplateId()))
                .withPersonalisation(personalisation()
                        .withAdditionalProperty(PROPERTY_CASE_REFERENCE, summonsDocumentContent.getCaseReference())
                        .withAdditionalProperty(PROPERTY_DEFENDANT_DETAILS, getAddresseeDetailsForApplication(summonsDocumentContent, notificationForParentOrGuardian))
                        .withAdditionalProperty(PROPERTY_COURT_LOCATION, summonsDocumentContent.getHearingCourtDetails().getCourtName())
                        .withAdditionalProperty(PROPERTY_HEARING_DATE, getHearingDateForEmailNotification(summonsDataPrepared.getSummonsData().getHearingDateTime()))
                        .withAdditionalProperty(PROPERTY_HEARING_TIME, summonsDocumentContent.getHearingCourtDetails().getHearingTime())
                        .withAdditionalProperty(PROPERTY_DEFENDANT_IS_YOUTH, addresseeIsYouth)
                        .build())
                .build();
    }

    private EmailChannel buildEmailNotificationForSummonsNotSuppressed(final SummonsDataPrepared summonsDataPrepared,
                                                                       final String emailAddress,
                                                                       final SummonsDocumentContent summonsDocumentContent,
                                                                       final List<String> defendantDetails) {
        return emailChannel()
                .withSendToAddress(emailAddress)
                .withTemplateId(fromString(applicationParameters.getSummonsApprovedAndNotSuppressedTemplateId()))
                .withPersonalisation(personalisation()
                        .withAdditionalProperty(PROPERTY_CASE_REFERENCE, summonsDocumentContent.getCaseReference())
                        .withAdditionalProperty(PROPERTY_DEFENDANT_DETAILS, defendantDetails.stream().collect(joining(lineSeparator())))
                        .withAdditionalProperty(PROPERTY_COURT_LOCATION, summonsDocumentContent.getHearingCourtDetails().getCourtName())
                        .withAdditionalProperty(PROPERTY_HEARING_DATE, getHearingDateForEmailNotification(summonsDataPrepared.getSummonsData().getHearingDateTime()))
                        .withAdditionalProperty(PROPERTY_HEARING_TIME, summonsDocumentContent.getHearingCourtDetails().getHearingTime())
                        .build())
                .build();
    }

    private void joinCaseDefendantDetails(final List<String> existingDefendantDetails, final SummonsDocumentContent summonsDocumentContent,
                                          final Defendant defendant, final boolean notificationForParentOrGuardian) {
        existingDefendantDetails.add(getCaseDefendantDetails(summonsDocumentContent, defendant, notificationForParentOrGuardian));
    }

    private boolean conditionNotMetToRaiseEmailNotification(final SummonsType summonsRequired, final String emailAddress, final boolean sendForRemotePrinting, final boolean notificationForParentOrGuardian) {
        return summonsRequired == SJP_REFERRAL ||
                isBlank(emailAddress) ||
                (sendForRemotePrinting && notificationForParentOrGuardian);
    }

    private String getCaseDefendantDetails(final SummonsDocumentContent summonsDocumentContent, final Defendant defendant, final boolean notificationForParentOrGuardian) {
        final ImmutableList.Builder<String> builder = ImmutableList.builder();
        if (notificationForParentOrGuardian) {
            final String defendantName = format("%s (parent/guardian of %s)", summonsDocumentContent.getAddressee().getName(), summonsDocumentContent.getDefendant().getName());
            builder.add(defendantName);
            ofNullable(defendant.getProsecutionAuthorityReference()).ifPresent(builder::add);
            return builder.build().stream()
                    .filter(StringUtils::isNotBlank)
                    .collect(joining(", "));
        }
        ofNullable(summonsDocumentContent.getDefendant().getName()).ifPresent(builder::add);
        ofNullable(defendant.getProsecutionAuthorityReference()).ifPresent(builder::add);
        return builder.build().stream()
                .filter(StringUtils::isNotBlank)
                .collect(joining(", "));
    }

    private String getAddresseeDetailsForApplication(final SummonsDocumentContent summonsDocumentContent, final boolean notificationForParentOrGuardian) {
        if (notificationForParentOrGuardian) {
            return format("%s (parent/guardian of %s)", summonsDocumentContent.getAddressee().getName(), summonsDocumentContent.getDefendant().getName());
        }
        return summonsDocumentContent.getDefendant().getName();
    }

    private static String getHearingDateForEmailNotification(final ZonedDateTime hearingDateTime) {
        try {
            return DATE_FORMATTER.format(hearingDateTime.withZoneSameInstant(UK_TIME_ZONE));
        } catch (DateTimeException dte) {
            throw new InvalidHearingDateException(format("Invalid hearing date time [ %s ] for generating notification / summons ", hearingDateTime), dte);
        }
    }

    private <T> boolean lastItemInList(final T item, final List<T> list) {
        return list.indexOf(item) == list.size() - 1;
    }

}
