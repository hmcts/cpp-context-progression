package uk.gov.moj.cpp.progression.aggregate;

import static java.util.Objects.nonNull;
import static org.apache.commons.collections.CollectionUtils.isEmpty;
import static org.apache.commons.collections.CollectionUtils.isNotEmpty;
import static uk.gov.justice.domain.aggregate.matcher.EventSwitcher.match;
import static uk.gov.justice.domain.aggregate.matcher.EventSwitcher.otherwiseDoNothing;
import static uk.gov.justice.domain.aggregate.matcher.EventSwitcher.when;
import static uk.gov.justice.listing.courts.CourtListPublished.courtListPublished;

import uk.gov.justice.core.courts.CourtRegisterRecorded;
import uk.gov.justice.core.courts.PrisonCourtRegisterFailed;
import uk.gov.justice.core.courts.PrisonCourtRegisterGenerated;
import uk.gov.justice.core.courts.PrisonCourtRegisterRecorded;
import uk.gov.justice.core.courts.PrisonCourtRegisterSent;
import uk.gov.justice.core.courts.PrisonCourtRegisterWithoutRecipientsRecorded;
import uk.gov.justice.core.courts.RecordPrisonCourtRegisterDocumentSent;
import uk.gov.justice.core.courts.RecordPrisonCourtRegisterFailed;
import uk.gov.justice.core.courts.courtRegisterDocument.CourtRegisterDocumentRequest;
import uk.gov.justice.core.courts.courtRegisterDocument.CourtRegisterRecipient;
import uk.gov.justice.core.courts.prisonCourtRegisterDocument.PrisonCourtRegisterDocumentRequest;
import uk.gov.justice.core.courts.prisonCourtRegisterDocument.RecordPrisonCourtRegisterDocumentGenerated;
import uk.gov.justice.domain.aggregate.Aggregate;
import uk.gov.justice.listing.courts.CourtListPublished;
import uk.gov.justice.listing.courts.PublishCourtList;
import uk.gov.justice.progression.courts.CourtRegisterGenerated;
import uk.gov.justice.progression.courts.CourtRegisterNotificationIgnored;
import uk.gov.justice.progression.courts.CourtRegisterNotified;
import uk.gov.justice.progression.courts.NotifyCourtRegister;
import uk.gov.justice.progression.courts.NotifyPrisonCourtRegister;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class CourtCentreAggregate implements Aggregate {
    private static final long serialVersionUID = 102L;
    private List<CourtRegisterRecipient> courtRegisterRecipients;

    private final Map<UUID, PrisonCourtRegisterDocumentRequest> prisonCourtRegisterMap = new HashMap<>();

    @Override
    public Object apply(final Object event) {
        return match(event).with(
                when(CourtRegisterRecorded.class).apply(e -> {
                    // Do something here if needed.
                }),
                when(PrisonCourtRegisterRecorded.class).apply(e -> {
                    // Do something here if needed.
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
                    }
                }),
                when(PrisonCourtRegisterSent.class).apply(e -> {
                    final PrisonCourtRegisterDocumentRequest prisonCourtRegister = e.getPrisonCourtRegister();
                    prisonCourtRegisterMap.put(e.getPayloadFileId(), prisonCourtRegister);
                }),
                otherwiseDoNothing()
        );
    }

    public Stream<Object> createCourtRegister(final UUID courtCentreId, final CourtRegisterDocumentRequest courtRegisterDocumentRequest) {
        return apply(Stream.of(new CourtRegisterRecorded(courtCentreId, courtRegisterDocumentRequest)));
    }

    public Stream<Object> createPrisonCourtRegister(final UUID courtCentreId, final PrisonCourtRegisterDocumentRequest prisonCourtRegisterDocumentRequest) {

        if (prisonCourtRegisterDocumentRequest.getRecipients() == null || prisonCourtRegisterDocumentRequest.getRecipients().isEmpty()) {
            return apply(Stream.of(PrisonCourtRegisterWithoutRecipientsRecorded.prisonCourtRegisterWithoutRecipientsRecorded()
                    .withCourtCentreId(courtCentreId)
                    .withPrisonCourtRegister(prisonCourtRegisterDocumentRequest).build()));
        } else {
            return apply(Stream.of(PrisonCourtRegisterRecorded.prisonCourtRegisterRecorded()
                    .withCourtCentreId(courtCentreId)
                    .withPrisonCourtRegister(prisonCourtRegisterDocumentRequest).build()));
        }
    }

    public Stream<Object> generateDocument(final List<CourtRegisterDocumentRequest> courtRegisterDocumentRequests, final boolean systemGenerated) {
        return apply(Stream.of(CourtRegisterGenerated.courtRegisterGenerated()
                .withCourtRegisterDocumentRequests(courtRegisterDocumentRequests)
                .withSystemGenerated(systemGenerated)
                .build()));
    }

    public Stream<Object> notifyCourt(final NotifyCourtRegister notifyCourtRegister) {
        if (isEmpty(courtRegisterRecipients)) {
            return apply(Stream.of(CourtRegisterNotificationIgnored.courtRegisterNotificationIgnored()
                    .withMaterialId(notifyCourtRegister.getSystemDocGeneratorId())
                    .withCourtCentreId(notifyCourtRegister.getCourtCentreId()).build()));
        }
        return apply(Stream.of(CourtRegisterNotified.courtRegisterNotified().withRecipients(courtRegisterRecipients)
                .withSystemDocGeneratorId(notifyCourtRegister.getSystemDocGeneratorId())
                .withCourtCentreId(notifyCourtRegister.getCourtCentreId()).build()));
    }


    public Stream<Object> recordPrisonCourtRegisterGenerated(final UUID courtCentreId, final NotifyPrisonCourtRegister notifyPrisonCourtRegister) {

        final UUID payloadFileId = notifyPrisonCourtRegister.getPayloadFileId();

        final PrisonCourtRegisterDocumentRequest prisonCourtRegisterDocumentRequest = prisonCourtRegisterMap.get(payloadFileId);

        return apply(Stream.of(PrisonCourtRegisterGenerated.prisonCourtRegisterGenerated()
                .withRecipients(prisonCourtRegisterDocumentRequest.getRecipients())
                .withDefendant(prisonCourtRegisterDocumentRequest.getDefendant())
                .withFileId(notifyPrisonCourtRegister.getSystemDocGeneratorId())
                .withHearingVenue(prisonCourtRegisterDocumentRequest.getHearingVenue())
                .withHearingDate(prisonCourtRegisterDocumentRequest.getHearingDate())
                .withHearingId(prisonCourtRegisterDocumentRequest.getHearingId())
                .withCourtCentreId(courtCentreId).build()));
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
                .withCourtCentreId(courtCentreId)
                .build()));
    }


    //should be used only in test
    public void setCourtRegisterRecipients(final List<CourtRegisterRecipient> courtRegisterRecipients) {
        this.courtRegisterRecipients = Collections.unmodifiableList(courtRegisterRecipients);
    }

    public Stream<Object> recordPrisonCourtRegisterGenerated(final UUID courtCentreId, final RecordPrisonCourtRegisterDocumentGenerated prisonCourtRegisterDocumentGenerated) {
        return apply(Stream.of(PrisonCourtRegisterGenerated.prisonCourtRegisterGenerated()
                .withRecipients(prisonCourtRegisterDocumentGenerated.getRecipients())
                .withDefendant(prisonCourtRegisterDocumentGenerated.getDefendant())
                .withFileId(prisonCourtRegisterDocumentGenerated.getFileId())
                .withHearingVenue(prisonCourtRegisterDocumentGenerated.getHearingVenue())
                .withHearingDate(prisonCourtRegisterDocumentGenerated.getHearingDate())
                .withHearingId(prisonCourtRegisterDocumentGenerated.getHearingId())
                .withCourtCentreId(courtCentreId).build()));
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