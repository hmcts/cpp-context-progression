package uk.gov.moj.cpp.progression.aggregate;

import uk.gov.justice.core.courts.CourtRegisterRecorded;
import uk.gov.justice.core.courts.PrisonCourtRegisterFailed;
import uk.gov.justice.core.courts.PrisonCourtRegisterGenerated;
import uk.gov.justice.core.courts.PrisonCourtRegisterGeneratedV2;
import uk.gov.justice.core.courts.PrisonCourtRegisterRecorded;
import uk.gov.justice.core.courts.PrisonCourtRegisterSent;
import uk.gov.justice.core.courts.PrisonCourtRegisterWithoutRecipientsRecorded;
import uk.gov.justice.core.courts.RecordPrisonCourtRegisterDocumentSent;
import uk.gov.justice.core.courts.RecordPrisonCourtRegisterFailed;
import uk.gov.justice.core.courts.courtRegisterDocument.CourtRegisterDocumentRequest;
import uk.gov.justice.core.courts.courtRegisterDocument.CourtRegisterRecipient;
import uk.gov.justice.core.courts.prisonCourtRegisterDocument.PrisonCourtRegisterDocumentRequest;
import uk.gov.justice.domain.aggregate.Aggregate;
import uk.gov.justice.listing.courts.CourtListPublished;
import uk.gov.justice.listing.courts.PublishCourtList;
import uk.gov.justice.progression.courts.CourtRegisterGenerated;
import uk.gov.justice.progression.courts.CourtRegisterNotificationIgnored;
import uk.gov.justice.progression.courts.CourtRegisterNotifiedV2;
import uk.gov.justice.progression.courts.NotifyCourtRegister;
import uk.gov.justice.progression.courts.NotifyPrisonCourtRegister;
import uk.gov.justice.services.core.featurecontrol.FeatureControlGuard;

import javax.inject.Inject;
import java.time.ZonedDateTime;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static org.apache.commons.collections.CollectionUtils.isEmpty;
import static org.apache.commons.collections.CollectionUtils.isNotEmpty;
import static uk.gov.justice.domain.aggregate.matcher.EventSwitcher.match;
import static uk.gov.justice.domain.aggregate.matcher.EventSwitcher.otherwiseDoNothing;
import static uk.gov.justice.domain.aggregate.matcher.EventSwitcher.when;
import static uk.gov.justice.listing.courts.CourtListPublished.courtListPublished;
import static uk.gov.moj.cpp.progression.domain.constant.FeatureGuardNames.FEATURE_AMP_SEND_PCR;

public class CourtCentreAggregate implements Aggregate {
    private static final long serialVersionUID = 1054L;
    private List<CourtRegisterRecipient> courtRegisterRecipients;

    private UUID courtCentreId;
    private UUID prisonCourtCentreId;
    private ZonedDateTime registerDate;

    private final Map<UUID, PrisonCourtRegisterDocumentRequest> prisonCourtRegisterMap = new HashMap<>();

    @Inject
    private transient FeatureControlGuard featureControlGuard;

    @Override
    public Object apply(final Object event) {
        return match(event).with(
                when(CourtRegisterRecorded.class).apply(e -> {
                    this.courtCentreId = e.getCourtCentreId();
                    this.registerDate = e.getCourtRegister().getRegisterDate();
                }),
                when(PrisonCourtRegisterRecorded.class).apply(e -> {
                    prisonCourtCentreId = e.getCourtCentreId();
                }),
                when(CourtListPublished.class).apply(e -> {
                    // Do something here if needed.
                }),
                when(CourtRegisterGenerated.class).apply(e -> {
                    final List<CourtRegisterDocumentRequest> courtRegisterWithRecipients = e.getCourtRegisterDocumentRequests().stream().filter(
                                    courtRegisterDocumentRequest -> nonNull(courtRegisterDocumentRequest.getRecipients()) && !courtRegisterDocumentRequest.getRecipients().isEmpty())
                            .collect(Collectors.toList());

                    if (isNotEmpty(courtRegisterWithRecipients)) {
                        this.courtRegisterRecipients = courtRegisterWithRecipients.stream().findFirst().get().getRecipients();
                        if (isNull(courtCentreId)) {
                            this.courtCentreId = courtRegisterWithRecipients.stream().findFirst().get().getCourtCentreId();
                            this.registerDate = courtRegisterWithRecipients.stream().findFirst().get().getRegisterDate();
                        }
                    }
                }),
                when(PrisonCourtRegisterSent.class).apply(e -> {
                    final PrisonCourtRegisterDocumentRequest prisonCourtRegister = e.getPrisonCourtRegister();
                    prisonCourtRegisterMap.put(e.getPayloadFileId(), prisonCourtRegister);
                }),
                otherwiseDoNothing()
        );
    }

    public Stream<Object> createPrisonCourtRegister(final UUID courtCentreId, final PrisonCourtRegisterDocumentRequest prisonCourtRegisterDocumentRequest,
                                                    final String defendantType, final UUID prisonCourtRegisterStreamId) {

        if (prisonCourtRegisterDocumentRequest.getRecipients() == null || prisonCourtRegisterDocumentRequest.getRecipients().isEmpty()) {
            return apply(Stream.of(PrisonCourtRegisterWithoutRecipientsRecorded.prisonCourtRegisterWithoutRecipientsRecorded()
                    .withCourtCentreId(courtCentreId)
                    .withPrisonCourtRegisterStreamId(prisonCourtRegisterStreamId)
                    .withPrisonCourtRegister(prisonCourtRegisterDocumentRequest).build()));
        } else {
            final PrisonCourtRegisterRecorded.Builder prisonCourtRegisterBuilder = PrisonCourtRegisterRecorded.prisonCourtRegisterRecorded()
                    .withCourtCentreId(courtCentreId)
                    .withPrisonCourtRegister(prisonCourtRegisterDocumentRequest)
                    .withPrisonCourtRegisterStreamId(prisonCourtRegisterStreamId)
                    .withId(UUID.randomUUID());

            if (nonNull(defendantType)) {
                prisonCourtRegisterBuilder.withDefendantType(defendantType);
            }

            return apply(Stream.of(prisonCourtRegisterBuilder.build()));
        }
    }

    public Stream<Object> notifyCourt(final NotifyCourtRegister notifyCourtRegister) {
        if (isEmpty(courtRegisterRecipients)) {
            return apply(Stream.of(CourtRegisterNotificationIgnored.courtRegisterNotificationIgnored()
                    .withMaterialId(notifyCourtRegister.getSystemDocGeneratorId())
                    .withCourtCentreId(courtCentreId).build()));
        }
        return apply(Stream.of(CourtRegisterNotifiedV2.courtRegisterNotifiedV2()
                .withRecipients(courtRegisterRecipients)
                .withSystemDocGeneratorId(notifyCourtRegister.getSystemDocGeneratorId())
                .withCourtCentreId(courtCentreId)
                .withRegisterDate(registerDate.toLocalDate())
                .build()));
    }


    public Stream<Object> recordPrisonCourtRegisterGenerated(final UUID courtCentreId, final NotifyPrisonCourtRegister notifyPrisonCourtRegister) {
        final UUID payloadFileId = notifyPrisonCourtRegister.getPayloadFileId();

        final PrisonCourtRegisterDocumentRequest prisonCourtRegisterDocumentRequest = prisonCourtRegisterMap.get(payloadFileId);
        PrisonCourtRegisterGenerated pcrEvent1 = PrisonCourtRegisterGenerated.prisonCourtRegisterGenerated()
                .withRecipients(prisonCourtRegisterDocumentRequest.getRecipients())
                .withDefendant(prisonCourtRegisterDocumentRequest.getDefendant())
                .withFileId(notifyPrisonCourtRegister.getSystemDocGeneratorId())
                .withHearingVenue(prisonCourtRegisterDocumentRequest.getHearingVenue())
                .withHearingDate(prisonCourtRegisterDocumentRequest.getHearingDate())
                .withHearingId(prisonCourtRegisterDocumentRequest.getHearingId())
                .withId(notifyPrisonCourtRegister.getId())
                .withCourtCentreId(nonNull(courtCentreId) ? courtCentreId : this.prisonCourtCentreId)
                .build();

        if (featureControlGuard.isFeatureEnabled(FEATURE_AMP_SEND_PCR)) {
            PrisonCourtRegisterGeneratedV2 pcrEvent2 = PrisonCourtRegisterGeneratedV2.prisonCourtRegisterGeneratedV2()
                    .withRecipients(prisonCourtRegisterDocumentRequest.getRecipients())
                    .withDefendant(prisonCourtRegisterDocumentRequest.getDefendant())
                    .withFileId(notifyPrisonCourtRegister.getSystemDocGeneratorId())
                    .withHearingVenue(prisonCourtRegisterDocumentRequest.getHearingVenue())
                    .withHearingDate(prisonCourtRegisterDocumentRequest.getHearingDate())
                    .withHearingId(prisonCourtRegisterDocumentRequest.getHearingId())
                    .withMaterialId(notifyPrisonCourtRegister.getMaterialId())
                    .withId(notifyPrisonCourtRegister.getId())
                    .withCourtCentreId(nonNull(courtCentreId) ? courtCentreId : this.prisonCourtCentreId)
                    .build();
            return apply(Stream.of(pcrEvent1, pcrEvent2));
        } else {
            return apply(Stream.of(pcrEvent1));
        }
    }

    public Stream<Object> recordPrisonCourtRegisterDocumentSent(final UUID courtCentreId, final RecordPrisonCourtRegisterDocumentSent recordPrisonCourtRegisterDocumentSent) {
        return apply(Stream.of(PrisonCourtRegisterSent.prisonCourtRegisterSent()
                .withPrisonCourtRegister(PrisonCourtRegisterDocumentRequest.prisonCourtRegisterDocumentRequest()
                        .withCourtCentreId(courtCentreId)
                        .withRecipients(recordPrisonCourtRegisterDocumentSent.getRecipients())
                        .withDefendant(recordPrisonCourtRegisterDocumentSent.getDefendant())
                        .withHearingId(recordPrisonCourtRegisterDocumentSent.getHearingId())
                        .withHearingVenue(recordPrisonCourtRegisterDocumentSent.getHearingVenue())
                        .withHearingDate(recordPrisonCourtRegisterDocumentSent.getHearingDate())
                        .build())
                .withPayloadFileId(recordPrisonCourtRegisterDocumentSent.getPayloadFileId())
                .withCourtCentreId(courtCentreId)
                .withPrisonCourtRegisterStreamId(recordPrisonCourtRegisterDocumentSent.getPrisonCourtRegisterStreamId())
                .build()));
    }

    public Stream<Object> recordPrisonCourtRegisterFailed(final UUID courtCentreId, RecordPrisonCourtRegisterFailed recordPrisonCourtRegisterFailed) {
        return apply(Stream.of(PrisonCourtRegisterFailed.prisonCourtRegisterFailed()
                .withPayloadFileId(recordPrisonCourtRegisterFailed.getPayloadFileId())
                .withReason(recordPrisonCourtRegisterFailed.getReason())
                .withConversionFormat(recordPrisonCourtRegisterFailed.getConversionFormat())
                .withFailedTime(recordPrisonCourtRegisterFailed.getFailedTime())
                .withOriginatingSource(recordPrisonCourtRegisterFailed.getOriginatingSource())
                .withRequestedTime(recordPrisonCourtRegisterFailed.getRequestedTime())
                .withTemplateIdentifier(recordPrisonCourtRegisterFailed.getTemplateIdentifier())
                .withCourtCentreId(nonNull(courtCentreId) ? courtCentreId : this.prisonCourtCentreId)
                .withId(recordPrisonCourtRegisterFailed.getId())
                .build()));
    }

    //should be used only in test
    public void setCourtRegisterRecipients(final List<CourtRegisterRecipient> courtRegisterRecipients) {
        this.courtRegisterRecipients = Collections.unmodifiableList(courtRegisterRecipients);
    }

    public Stream<Object> publishCourtList(final UUID courtCentreId, final PublishCourtList publishCourtList) {
        return apply(Stream.of(courtListPublished()
                .withCourtCentreId(courtCentreId)
                .withCourtListId(publishCourtList.getCourtListId())
                .withPublishCourtListType(publishCourtList.getPublishCourtListType())
                .withCourtLists(publishCourtList.getCourtLists())
                .withStartDate(publishCourtList.getStartDate())
                .withEndDate(publishCourtList.getEndDate())
                .withRequestedTime(publishCourtList.getRequestedTime())
                .withWeekCommencing(publishCourtList.getWeekCommencing())
                .withSendNotificationToParties(publishCourtList.getSendNotificationToParties())
                .build()));
    }
}