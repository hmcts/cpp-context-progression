package uk.gov.moj.cpp.progression.handler;


import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static org.apache.commons.collections.CollectionUtils.isEmpty;
import static uk.gov.moj.cpp.progression.domain.helper.CourtRegisterHelper.getPrisonCourtRegisterStreamId;

import uk.gov.justice.core.courts.RecordPrisonCourtRegisterDocumentSent;
import uk.gov.justice.core.courts.RecordPrisonCourtRegisterFailed;
import uk.gov.justice.core.courts.CourtApplication;
import uk.gov.justice.core.courts.prisonCourtRegisterDocument.PrisonCourtRegisterDocumentRequest;
import uk.gov.justice.progression.courts.NotifyPrisonCourtRegister;
import uk.gov.justice.services.core.aggregate.AggregateService;
import uk.gov.justice.services.core.annotation.Component;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.eventsourcing.source.core.EventSource;
import uk.gov.justice.services.eventsourcing.source.core.EventStream;
import uk.gov.justice.services.eventsourcing.source.core.exception.EventStreamException;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.moj.cpp.progression.aggregate.ApplicationAggregate;
import uk.gov.moj.cpp.progression.aggregate.CourtCentreAggregate;

import java.time.ZonedDateTime;
import java.util.UUID;
import java.util.stream.Stream;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ServiceComponent(Component.COMMAND_HANDLER)
public class PrisonCourtRegisterHandler extends AbstractCommandHandler {

    @Inject
    private EventSource eventSource;

    @Inject
    private AggregateService aggregateService;

    private static final Logger LOGGER = LoggerFactory.getLogger(PrisonCourtRegisterHandler.class.getName());

    @Handles("progression.command.add-prison-court-register")
    public void handleAddPrisonCourtRegister(final Envelope<PrisonCourtRegisterDocumentRequest> envelope) throws EventStreamException {
        LOGGER.info("progression.command.add-prison-court-register {}", envelope.payload());

        final PrisonCourtRegisterDocumentRequest prisonCourtRegisterDocumentRequest = envelope.payload();

        final UUID courtCentreId = prisonCourtRegisterDocumentRequest.getCourtCentreId();
        final ZonedDateTime hearingDateTime = prisonCourtRegisterDocumentRequest.getHearingDate();
        String defendantType = "Defendant";
        final UUID courtApplicationId = getCourtApplicationId(prisonCourtRegisterDocumentRequest);
        if (nonNull(courtApplicationId)) {
            final EventStream applicationEventStream = eventSource.getStreamById(courtApplicationId);
            final ApplicationAggregate applicationAggregate = aggregateService.get(applicationEventStream, ApplicationAggregate.class);

            final CourtApplication courtApplication = applicationAggregate.getCourtApplication();
            defendantType = getDefendantType(prisonCourtRegisterDocumentRequest, courtApplication);
        }

        final UUID prisonCourtRegisterStreamId = getPrisonCourtRegisterStreamId(courtCentreId.toString(), hearingDateTime.toLocalDate().toString());

        final EventStream eventStream = eventSource.getStreamById(prisonCourtRegisterStreamId);

        final CourtCentreAggregate courtCentreAggregate = aggregateService.get(eventStream, CourtCentreAggregate.class);

        final Stream<Object> events = courtCentreAggregate.createPrisonCourtRegister(courtCentreId, prisonCourtRegisterDocumentRequest, defendantType, prisonCourtRegisterStreamId);

        appendEventsToStream(envelope, eventStream, events);
    }

    private UUID getCourtApplicationId(final PrisonCourtRegisterDocumentRequest prisonCourtRegisterDocumentRequest) {
        if (isNull(prisonCourtRegisterDocumentRequest.getDefendant()) ||
                isEmpty(prisonCourtRegisterDocumentRequest.getDefendant().getProsecutionCasesOrApplications())
        ) {
            return null;
        }

        return prisonCourtRegisterDocumentRequest.getDefendant().getProsecutionCasesOrApplications().get(0).getCourtApplicationId();
    }

    @Handles("progression.command.record-prison-court-register-document-sent")
    public void recordPrisonCourtRegisterDocumentSent(final Envelope<RecordPrisonCourtRegisterDocumentSent> envelope) throws EventStreamException {

        LOGGER.info("progression.command.record-prison-court-register-document-sent {}", envelope.payload());

        final RecordPrisonCourtRegisterDocumentSent recordPrisonCourtRegisterDocumentSent = envelope.payload();

        final UUID courtCentreId = recordPrisonCourtRegisterDocumentSent.getCourtCentreId();

        final UUID prisonCourtRegisterStreamId = recordPrisonCourtRegisterDocumentSent.getPrisonCourtRegisterStreamId();
        final EventStream eventStream = eventSource.getStreamById(prisonCourtRegisterStreamId);

        final CourtCentreAggregate courtCentreAggregate = aggregateService.get(eventStream, CourtCentreAggregate.class);

        final Stream<Object> events = courtCentreAggregate.recordPrisonCourtRegisterDocumentSent(courtCentreId, recordPrisonCourtRegisterDocumentSent);

        appendEventsToStream(envelope, eventStream, events);
    }

    @Handles("progression.command.notify-prison-court-register")
    public void handleNotifyCourtCentre(final Envelope<NotifyPrisonCourtRegister> envelope) throws EventStreamException {

        LOGGER.info("progression.command.notify-prison-court-register {}", envelope.payload());

        final NotifyPrisonCourtRegister notifyPrisonCourtRegister = envelope.payload();

        final UUID courtCentreId = notifyPrisonCourtRegister.getCourtCentreId();

        final UUID prisonCourtRegisterStreamId = notifyPrisonCourtRegister.getPrisonCourtRegisterStreamId();

        final EventStream eventStream = eventSource.getStreamById(prisonCourtRegisterStreamId);

        final CourtCentreAggregate courtCentreAggregate = aggregateService.get(eventStream, CourtCentreAggregate.class);

        final Stream<Object> events = courtCentreAggregate.recordPrisonCourtRegisterGenerated(courtCentreId, notifyPrisonCourtRegister);

        appendEventsToStream(envelope, eventStream, events);

    }

    @Handles("progression.command.record-prison-court-register-failed")
    public void handlePrisonCourtRegisterFailed(final Envelope<RecordPrisonCourtRegisterFailed> envelope) throws EventStreamException {

        LOGGER.info("progression.command.record-prison-court-register-failed {}", envelope.payload());

        final RecordPrisonCourtRegisterFailed recordPrisonCourtRegisterFailed = envelope.payload();

        final UUID courtCentreId = recordPrisonCourtRegisterFailed.getCourtCentreId();

        final UUID prisonCourtRegisterStreamId = recordPrisonCourtRegisterFailed.getPrisonCourtRegisterStreamId();
        final EventStream eventStream = eventSource.getStreamById(prisonCourtRegisterStreamId);

        final CourtCentreAggregate courtCentreAggregate = aggregateService.get(eventStream, CourtCentreAggregate.class);

        final Stream<Object> events = courtCentreAggregate.recordPrisonCourtRegisterFailed(courtCentreId, recordPrisonCourtRegisterFailed);

        appendEventsToStream(envelope, eventStream, events);

    }

    private String getDefendantType(final PrisonCourtRegisterDocumentRequest prisonCourtRegisterDocumentRequest, final CourtApplication courtApplication) {
        String defendantType = "Applicant";
        if (nonNull(courtApplication.getApplicant().getMasterDefendant())) {
            defendantType = "Applicant";
            if (courtApplication.getType().getAppealFlag() && courtApplication.getType().getApplicantAppellantFlag()) {
                defendantType = "Appellant";
            }
        } else {
            if (courtApplication
                    .getRespondents().stream()
                    .filter(respondent -> nonNull(respondent.getMasterDefendant()))
                    .anyMatch(respondent -> respondent.getMasterDefendant().getMasterDefendantId().equals(prisonCourtRegisterDocumentRequest.getDefendant().getMasterDefendantId()))) {
                defendantType = "Respondent";
            }
        }

        return defendantType;
    }
}
