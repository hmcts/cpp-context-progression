package uk.gov.moj.cpp.progression.aggregate;

import uk.gov.justice.core.courts.PrisonCourtRegisterFailed;
import uk.gov.justice.core.courts.PrisonCourtRegisterGenerated;
import uk.gov.justice.core.courts.PrisonCourtRegisterGeneratedV2;
import uk.gov.justice.core.courts.PrisonCourtRegisterRecorded;
import uk.gov.justice.core.courts.PrisonCourtRegisterSent;
import uk.gov.justice.core.courts.PrisonCourtRegisterWithoutRecipientsRecorded;
import uk.gov.justice.core.courts.RecordPrisonCourtRegisterDocumentSent;
import uk.gov.justice.core.courts.RecordPrisonCourtRegisterFailed;
import uk.gov.justice.core.courts.prisonCourtRegisterDocument.PrisonCourtRegisterDocumentRequest;
import uk.gov.justice.domain.aggregate.Aggregate;
import uk.gov.justice.listing.courts.CourtListPublished;
import uk.gov.justice.listing.courts.PublishCourtList;
import uk.gov.justice.progression.courts.NotifyPrisonCourtRegister;
import uk.gov.justice.services.core.featurecontrol.FeatureControlGuard;

import javax.inject.Inject;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Stream;

import static java.util.Objects.nonNull;
import static uk.gov.justice.domain.aggregate.matcher.EventSwitcher.match;
import static uk.gov.justice.domain.aggregate.matcher.EventSwitcher.otherwiseDoNothing;
import static uk.gov.justice.domain.aggregate.matcher.EventSwitcher.when;
import static uk.gov.justice.listing.courts.CourtListPublished.courtListPublished;
import static uk.gov.moj.cpp.progression.domain.constant.FeatureGuardNames.FEATURE_HEARING_RESULTS_DOCUMENT_SUBSCRIPTION_ENABLED;

public class CourtCentreAggregate implements Aggregate {
    private static final long serialVersionUID = 1056L;
    private UUID prisonCourtCentreId;

    private final Map<UUID, PrisonCourtRegisterDocumentRequest> prisonCourtRegisterMap = new HashMap<>();

    @Inject
    private transient FeatureControlGuard featureControlGuard;

    @Override
    public Object apply(final Object event) {
        return match(event).with(
                when(PrisonCourtRegisterRecorded.class).apply(e -> {
                    prisonCourtCentreId = e.getCourtCentreId();
                }),
                when(CourtListPublished.class).apply(e -> {
                    // Do something here if needed.
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

        if (nonNull(featureControlGuard) && featureControlGuard.isFeatureEnabled(FEATURE_HEARING_RESULTS_DOCUMENT_SUBSCRIPTION_ENABLED)) {
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

    //should be used to inject featureControlGuard after aggregate is loaded from event store
    public void setFeatureControlGuard(final FeatureControlGuard featureControlGuard) {
        this.featureControlGuard = featureControlGuard;
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