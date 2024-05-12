package uk.gov.moj.cpp.progression.aggregate;

import static java.lang.String.format;
import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static java.util.Optional.ofNullable;
import static org.apache.commons.collections.CollectionUtils.isNotEmpty;
import static org.apache.commons.lang3.BooleanUtils.isTrue;
import static org.apache.commons.lang3.BooleanUtils.negate;
import static org.apache.commons.lang3.StringUtils.isEmpty;
import static uk.gov.justice.domain.aggregate.matcher.EventSwitcher.match;
import static uk.gov.justice.domain.aggregate.matcher.EventSwitcher.otherwiseDoNothing;
import static uk.gov.justice.domain.aggregate.matcher.EventSwitcher.when;

import uk.gov.justice.core.courts.CaseSubjects;
import uk.gov.justice.core.courts.EnforcementAcknowledgmentError;
import uk.gov.justice.core.courts.MaterialDetails;
import uk.gov.justice.core.courts.NowDocumentRequestToBeAcknowledged;
import uk.gov.justice.core.courts.NowDocumentRequested;
import uk.gov.justice.core.courts.NowsDocumentFailed;
import uk.gov.justice.core.courts.NowsDocumentGenerated;
import uk.gov.justice.core.courts.NowsDocumentSent;
import uk.gov.justice.core.courts.NowsMaterialRequestRecorded;
import uk.gov.justice.core.courts.NowsMaterialStatusUpdated;
import uk.gov.justice.core.courts.NowsRequestWithAccountNumberIgnored;
import uk.gov.justice.core.courts.NowsRequestWithAccountNumberUpdated;
import uk.gov.justice.core.courts.RecordNowsDocumentFailed;
import uk.gov.justice.core.courts.RecordNowsDocumentSent;
import uk.gov.justice.core.courts.nowdocument.FinancialOrderDetails;
import uk.gov.justice.core.courts.nowdocument.NowDistribution;
import uk.gov.justice.core.courts.nowdocument.NowDocumentContent;
import uk.gov.justice.core.courts.nowdocument.NowDocumentRequest;
import uk.gov.justice.core.courts.nowdocument.NowNotificationSuppressed;
import uk.gov.justice.core.courts.nowdocument.OrderAddressee;
import uk.gov.justice.core.courts.nowdocument.ProsecutionCase;
import uk.gov.justice.domain.aggregate.Aggregate;
import uk.gov.justice.progression.courts.RecordNowsDocumentGenerated;
import uk.gov.moj.cpp.progression.domain.Notification;
import uk.gov.moj.cpp.progression.domain.NotificationRequestAccepted;
import uk.gov.moj.cpp.progression.domain.NotificationRequestFailed;
import uk.gov.moj.cpp.progression.domain.NotificationRequestSucceeded;
import uk.gov.moj.cpp.progression.domain.event.MaterialStatusUpdateIgnored;
import uk.gov.moj.cpp.progression.domain.event.email.EmailRequestNotSent;
import uk.gov.moj.cpp.progression.domain.event.email.EmailRequested;
import uk.gov.moj.cpp.progression.domain.event.print.PrintRequested;
import uk.gov.moj.cpp.progression.events.NowDocumentNotificationSuppressed;

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@SuppressWarnings("squid:S1948")
public class MaterialAggregate implements Aggregate {

    private static final Logger LOGGER = LoggerFactory.getLogger(MaterialAggregate.class);

    private static final long serialVersionUID = 101L;

    private static final DateTimeFormatter TIMESTAMP_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    private MaterialDetails details;
    private final List<UUID> caseIds = new ArrayList<>();
    private NowDocumentRequest nowDocumentRequest;
    private boolean isAccountNumberSavedBefore = false;
    private UUID hearingId;
    private Boolean cpsProsecutionCase;
    private String fileName;
    private NowDistribution nowDistribution;
    private OrderAddressee orderAddressee;
    private UUID userId;

    @Override
    public Object apply(final Object event) {
        return match(event).with(
                when(NowsMaterialRequestRecorded.class).apply(e ->
                        details = e.getContext()
                ),
                when(NowDocumentRequestToBeAcknowledged.class).apply(e ->
                        nowDocumentRequest = e.getNowDocumentRequest()
                ),
                when(NowsRequestWithAccountNumberUpdated.class).apply(e ->
                        isAccountNumberSavedBefore = true
                ),
                when(NowDocumentRequested.class).apply(e -> {
                        nowDocumentRequest = e.getNowDocumentRequest();
                        hearingId = nowDocumentRequest.getHearingId();
                        fileName = e.getFileName();
                        cpsProsecutionCase = e.getCpsProsecutionCase();

                        if (nonNull(e.getUserId())) {
                            userId = e.getUserId();
                        }

                        if (isNotEmpty(e.getNowDocumentRequest().getCases())) {
                            caseIds.addAll(e.getNowDocumentRequest().getCases());
                        }
                 }),
                when(NowsDocumentSent.class).apply(e -> {
                    hearingId = e.getHearingId();
                    cpsProsecutionCase = e.getCpsProsecutionCase();
                    fileName = e.getFileName();
                    nowDistribution = e.getNowDistribution();
                    orderAddressee = e.getOrderAddressee();
                    userId =  e.getUserId();
                }),
                otherwiseDoNothing()
        );
    }

    public Stream<Object> create(final MaterialDetails materialDetails) {
        return apply(Stream.of(NowsMaterialRequestRecorded
                .nowsMaterialRequestRecorded()
                .withContext(materialDetails).build()));
    }

    public Stream<Object> nowsMaterialStatusUpdated(final UUID materialId, final String status, final List<CaseSubjects> caseSubjects,
                                                     final List<String> cpsDefendantIds) {
        final Stream.Builder streamBuilder = Stream.builder();
        if (checkMaterialHasValidNotification(details)) {

            final boolean welshTranslationRequired =
                    ofNullable(nowDocumentRequest)
                            .map(e -> ofNullable(e.getWelshTranslationRequired())
                                    .orElse(false)).orElse(false);
            final List<String> defendantAsn = new ArrayList<>();
            if (nonNull(nowDocumentRequest) && nonNull(nowDocumentRequest.getNowContent().getDefendant().getProsecutingAuthorityReference())) {
                defendantAsn.add(nowDocumentRequest.getNowContent().getDefendant().getProsecutingAuthorityReference());
            }

            streamBuilder.add(new NowsMaterialStatusUpdated(caseSubjects, cpsDefendantIds, defendantAsn, this.details, status, welshTranslationRequired));

            if (nonNull(nowDocumentRequest) && welshTranslationRequired) {
                final List<String> caseUrns = this.nowDocumentRequest.getNowContent().getCases().stream().map(ProsecutionCase::getReference).collect(Collectors.toList());
                streamBuilder.add(new NowDocumentNotificationSuppressed(new NowNotificationSuppressed(caseUrns, this.nowDocumentRequest.getNowContent().getDefendant().getName(), this.nowDocumentRequest.getMasterDefendantId(), this.details.getMaterialId(), this.nowDocumentRequest.getTemplateName())));
            }

            return streamBuilder.build();
        }
        return Stream.of(new MaterialStatusUpdateIgnored(materialId, status));
    }

    public Stream<Object> createNowDocumentRequest(final UUID materialId, final NowDocumentRequest nowDocumentRequest, final UUID userId) {
        if (isEmpty(nowDocumentRequest.getRequestId())) {
            final boolean isCps = isCpsProsecutionCase(nowDocumentRequest.getNowContent());
            final String orderName = nowDocumentRequest.getNowContent().getOrderName();
            final String strFileName = getTimeStampAmendedFileName(orderName);
            final String templateName = getTemplateName(nowDocumentRequest);

            return apply(Stream.of(new NowDocumentRequested(isCps, strFileName, materialId, nowDocumentRequest, templateName, userId)));
        }
        return apply(Stream.of(new NowDocumentRequestToBeAcknowledged(materialId, nowDocumentRequest)));
    }

    public Stream<Object> saveAccountNumber(final UUID materialId, final UUID requestId, final String accountNumber, final UUID userId) {
        if (isAccountNumberSavedBefore) {
            return Stream.of(new NowsRequestWithAccountNumberIgnored(accountNumber, requestId));
        }
        final NowDocumentRequest updatedNowDocumentRequest = updateFinancialOrderDetails(nowDocumentRequest, accountNumber);

        final boolean isCps = isCpsProsecutionCase(updatedNowDocumentRequest.getNowContent());
        final String orderName = updatedNowDocumentRequest.getNowContent().getOrderName();
        final String strFileName = getTimeStampAmendedFileName(orderName);
        final String templateName = getTemplateName(updatedNowDocumentRequest);


        return Stream.of(new NowsRequestWithAccountNumberUpdated(accountNumber, requestId), new NowDocumentRequested(isCps, strFileName, materialId, updatedNowDocumentRequest, templateName, userId));
    }

    public Stream<Object> recordPrintRequest(final UUID materialId, final UUID notificationId, final boolean postage) {
        return apply(Stream.of(new PrintRequested(notificationId, null, null, materialId, postage)));
    }

    private boolean isCpsProsecutionCase(final NowDocumentContent nowContent) {
        return nowContent.getCases().stream()
                .filter(pc -> nonNull(pc.getIsCps()))
                .filter(ProsecutionCase::getIsCps)
                .findFirst()
                .map(ProsecutionCase::getIsCps)
                .orElse(false);
    }

    private String getTimeStampAmendedFileName(final String fileName) {
        return format("%s_%s.pdf", fileName, ZonedDateTime.now().format(TIMESTAMP_FORMATTER));
    }

    private String getTemplateName(NowDocumentRequest nowDocumentRequest) {

        final Boolean isWelshCourtCentre = nowDocumentRequest.getNowContent().getOrderingCourt().getWelshCourtCentre();
        if (nonNull(isWelshCourtCentre) && isWelshCourtCentre
                && nonNull(nowDocumentRequest.getBilingualTemplateName())
                && !nowDocumentRequest.getBilingualTemplateName().isEmpty()) {
            return nowDocumentRequest.getBilingualTemplateName();
        }
        return nowDocumentRequest.getTemplateName();
    }

    private NowDocumentRequest updateFinancialOrderDetails(final NowDocumentRequest nowDocumentRequest, final String accountNumber) {
        final NowDocumentContent nowDocumentContent = nowDocumentRequest.getNowContent();
        final FinancialOrderDetails financialOrderDetails = nowDocumentContent.getFinancialOrderDetails();
        return uk.gov.justice.core.courts.nowdocument.NowDocumentRequest.nowDocumentRequest()
                .withNowContent(uk.gov.justice.core.courts.nowdocument.NowDocumentContent.nowDocumentContent()
                        .withFinancialOrderDetails(uk.gov.justice.core.courts.nowdocument.FinancialOrderDetails.financialOrderDetails()
                                .withAccountingDivisionCode(financialOrderDetails.getAccountingDivisionCode())
                                .withAccountPaymentReference(accountNumber)
                                .withBacsAccountNumber(financialOrderDetails.getBacsAccountNumber())
                                .withBacsBankName(financialOrderDetails.getBacsBankName())
                                .withBacsSortCode(financialOrderDetails.getBacsSortCode())
                                .withEnforcementAddress(financialOrderDetails.getEnforcementAddress())
                                .withEnforcementEmail(financialOrderDetails.getEnforcementEmail())
                                .withEnforcementPhoneNumber(financialOrderDetails.getEnforcementPhoneNumber())
                                .withPaymentTerms(financialOrderDetails.getPaymentTerms())
                                .withTotalAmountImposed(financialOrderDetails.getTotalAmountImposed())
                                .withTotalBalance(financialOrderDetails.getTotalBalance())
                                .build())
                        .withAmendmentDate(nowDocumentContent.getAmendmentDate())
                        .withCases(nowDocumentContent.getCases())
                        .withCourtClerkName(nowDocumentContent.getCourtClerkName())
                        .withDefendant(nowDocumentContent.getDefendant())
                        .withNextHearingCourtDetails(nowDocumentContent.getNextHearingCourtDetails())
                        .withNowText(nowDocumentContent.getNowText())
                        .withOrderAddressee(nowDocumentContent.getOrderAddressee())
                        .withOrderDate(nowDocumentContent.getOrderDate())
                        .withOrderName(nowDocumentContent.getOrderName())
                        .withApplicants(nowDocumentContent.getApplicants())
                        .withCaseApplicationReferences(nowDocumentContent.getCaseApplicationReferences())
                        .withCivilNow(nowDocumentContent.getCivilNow())
                        .withRespondents(nowDocumentContent.getRespondents())
                        .withOrderingCourt(nowDocumentContent.getOrderingCourt())
                        .withWelshOrderName(nowDocumentContent.getWelshOrderName())
                        .withNowRequirementText(nowDocumentContent.getNowRequirementText())
                        .withDistinctPrompts(nowDocumentContent.getDistinctPrompts())
                        .withDistinctResults(nowDocumentContent.getDistinctResults())
                        .withThirdParties(nowDocumentContent.getThirdParties())
                        .withParentGuardian(nowDocumentContent.getParentGuardian())
                        .build())
                .withNowTypeId(nowDocumentRequest.getNowTypeId())
                .withHearingId(nowDocumentRequest.getHearingId())
                .withMaterialId(nowDocumentRequest.getMaterialId())
                .withRequestId(nowDocumentRequest.getRequestId())
                .withApplications(nowDocumentRequest.getApplications())
                .withBilingualTemplateName(nowDocumentRequest.getBilingualTemplateName())
                .withCases(nowDocumentRequest.getCases())
                .withMasterDefendantId(nowDocumentRequest.getMasterDefendantId())
                .withNotVisibleToUserGroups(nowDocumentRequest.getNotVisibleToUserGroups())
                .withNowDistribution(nowDocumentRequest.getNowDistribution())
                .withStorageRequired(nowDocumentRequest.getStorageRequired())
                .withSubscriberName(nowDocumentRequest.getSubscriberName())
                .withSubTemplateName(nowDocumentRequest.getSubTemplateName())
                .withTemplateName(nowDocumentRequest.getTemplateName())
                .withVisibleToUserGroups(nowDocumentRequest.getVisibleToUserGroups())
                .withWelshTranslationRequired(nowDocumentRequest.getWelshTranslationRequired())
                .build();
    }

    public Stream<Object> recordEnforcementError(final UUID requestId, final String errorCode, final String errorMessage) {
        return Stream.of(new EnforcementAcknowledgmentError(errorCode, errorMessage, requestId));
    }

    public Stream<Object> recordNotificationRequestAccepted(final UUID materialId, final UUID notificationId, final ZonedDateTime acceptedTime) {
        return apply(Stream.of(new NotificationRequestAccepted(null, null, materialId, notificationId, acceptedTime)));
    }

    public Stream<Object> recordNotificationRequestFailure(final UUID materialId, final UUID notificationId, final ZonedDateTime failedTime, final String errorMessage, final Optional<Integer> statusCode) {
        return apply(Stream.of(new NotificationRequestFailed(null, null, materialId, notificationId, failedTime, errorMessage, statusCode)));
    }

    public Stream<Object> recordNotificationRequestSuccess(final UUID materialId, final UUID notificationId, final ZonedDateTime sentTime, final ZonedDateTime completedAt) {
        return apply(Stream.of(new NotificationRequestSucceeded(null, null, materialId, notificationId, sentTime, completedAt)));
    }

    public Stream<Object> recordEmailRequest(final UUID materialId, final List<Notification> notifications) {

        final Stream.Builder<Object> streamBuilder = Stream.builder();

        for (final Notification notification : notifications) {
            final Object event;
            if (notification.getSendToAddress() != null) {
                event = new EmailRequested(null, materialId, null, Collections.singletonList(notification));
            } else {
                event = new EmailRequestNotSent(null, materialId, null, notification);
            }
            streamBuilder.add(event);
        }

        return apply(streamBuilder.build());
    }

    private boolean checkMaterialHasValidNotification(final MaterialDetails details) {
        if (isNull(details)) {
            return false;
        }
        return isTrue(details.getFirstClassLetter()) ||
                isTrue(details.getSecondClassLetter()) ||
                isTrue(details.getIsNotificationApi()) ||
                (nonNull(details.getEmailNotifications()) && negate(details.getEmailNotifications().isEmpty()));
    }

    public MaterialDetails getMaterialDetails(){
        return details;
    }

    public List<UUID> fetchCases(){
       LOGGER.info("fetchCases : {}", caseIds);
       return new ArrayList<>(caseIds);
    }

    public Stream<Object> recordNowsDocumentSent(final UUID materialId, final UUID userId, final RecordNowsDocumentSent recordNowsDocumentSent) {
        return apply(Stream.of(NowsDocumentSent.nowsDocumentSent()
                        .withMaterialId(materialId)
                        .withHearingId(recordNowsDocumentSent.getHearingId())
                        .withPayloadFileId(recordNowsDocumentSent.getPayloadFileId())
                        .withCpsProsecutionCase(recordNowsDocumentSent.getCpsProsecutionCase())
                        .withFileName(recordNowsDocumentSent.getFileName())
                        .withNowDistribution(recordNowsDocumentSent.getNowDistribution())
                        .withOrderAddressee(recordNowsDocumentSent.getOrderAddressee())
                        .withUserId(userId)
                .build()));
    }

    public Stream<Object> recordNowsDocumentFailed(final UUID materialId, final RecordNowsDocumentFailed recordNowsDocumentFailed) {
        return apply(Stream.of(NowsDocumentFailed.nowsDocumentFailed()
                .withPayloadFileId(recordNowsDocumentFailed.getPayloadFileId())
                .withReason(recordNowsDocumentFailed.getReason())
                .withConversionFormat(recordNowsDocumentFailed.getConversionFormat())
                .withFailedTime(recordNowsDocumentFailed.getFailedTime())
                .withOriginatingSource(recordNowsDocumentFailed.getOriginatingSource())
                .withRequestedTime(recordNowsDocumentFailed.getRequestedTime())
                .withTemplateIdentifier(recordNowsDocumentFailed.getTemplateIdentifier())
                .withMaterialId(materialId)
                .build()));
    }

    public Stream<Object> recordNowsDocumentGenerated(final UUID materialId, final RecordNowsDocumentGenerated recordNowsDocumentGenerated) {
        return apply(Stream.of(NowsDocumentGenerated.nowsDocumentGenerated()
                .withMaterialId(materialId)
                .withHearingId(hearingId)
                .withSystemDocGeneratorId(recordNowsDocumentGenerated.getSystemDocGeneratorId())
                .withCpsProsecutionCase(cpsProsecutionCase)
                .withFileName(fileName)
                .withNowDistribution(nowDistribution)
                .withOrderAddressee(orderAddressee)
                .withUserId(userId)
                .build()));
    }
}
