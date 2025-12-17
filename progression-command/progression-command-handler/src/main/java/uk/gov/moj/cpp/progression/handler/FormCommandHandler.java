package uk.gov.moj.cpp.progression.handler;

import static java.lang.Integer.parseInt;
import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static java.util.UUID.fromString;
import static uk.gov.justice.core.courts.FormType.BCM;
import static uk.gov.justice.core.courts.FormType.PET;
import static uk.gov.justice.core.courts.FormType.PTPH;
import static uk.gov.justice.services.core.annotation.Component.COMMAND_HANDLER;
import static uk.gov.justice.services.core.enveloper.Enveloper.toEnvelopeWithMetadataFrom;

import uk.gov.justice.core.courts.CreateForm;
import uk.gov.justice.core.courts.FinaliseForm;
import uk.gov.justice.core.courts.FormDefendants;
import uk.gov.justice.core.courts.FormType;
import uk.gov.justice.core.courts.RequestEditForm;
import uk.gov.justice.core.courts.UpdateForm;
import uk.gov.justice.core.courts.UpdateFormDefendants;
import uk.gov.justice.services.common.configuration.Value;
import uk.gov.justice.services.common.util.UtcClock;
import uk.gov.justice.services.core.aggregate.AggregateService;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.eventsourcing.source.core.EventSource;
import uk.gov.justice.services.eventsourcing.source.core.EventStream;
import uk.gov.justice.services.eventsourcing.source.core.exception.EventStreamException;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.moj.cpp.progression.aggregate.CaseAggregate;
import uk.gov.moj.cpp.progression.aggregate.HearingAggregate;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.inject.Inject;

import com.google.common.collect.ImmutableMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@SuppressWarnings("squid:S2696")
@ServiceComponent(COMMAND_HANDLER)
public class FormCommandHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(FormCommandHandler.class);
    private static final String NO_USER_ID_PRESENT = "No userId present";
    private static final int DEFAULT_LOCK_EXTEND_TIME_IN_MINUTES = 10;

    @Inject
    private EventSource eventSource;

    @Inject
    private AggregateService aggregateService;

    @Inject
    @Value(key = "bcmFormLockDurationInMinutes", defaultValue = "60")
    private String bcmFormLockDurationInMinutes;

    @Inject
    @Value(key = "ptphFormLockDurationInMinutes", defaultValue = "60")
    private String ptphFormLockDurationInMinutes;

    private static Map<FormType, Integer> lockDurationMapByFormType;


    @Handles("progression.command.create-form")
    public void handleCreateForm(final Envelope<CreateForm> envelope) throws EventStreamException {
        final CreateForm createForm = envelope.payload();

        LOGGER.info("progression.command.create-form with case form id courtFormId: {} for case: {}", createForm.getCourtFormId(), createForm.getCaseId());

        final Optional<String> userId = envelope.metadata().userId();
        final String userName = nonNull(createForm.getUserName()) ? createForm.getUserName() : null;
        final UUID userIdUUID = userId.isPresent() ? fromString(userId.get()) : null;

        final EventStream eventStream = eventSource.getStreamById(createForm.getCaseId());

        final CaseAggregate caseAggregate = aggregateService.get(eventStream, CaseAggregate.class);
        final Stream<Object> events = caseAggregate.createForm(createForm.getCourtFormId(),
                createForm.getCaseId(),
                createForm.getFormId(),
                extractDefendantsWithOffencesMap(createForm.getFormDefendants()),
                createForm.getFormData(),
                userIdUUID,
                createForm.getFormType(),
                createForm.getSubmissionId(),
                userName);

        eventStream.append(events.map(toEnvelopeWithMetadataFrom(envelope)));
    }

    @Handles("progression.command.update-form")
    public void handleUpdateForm(final Envelope<UpdateForm> envelope) throws EventStreamException {
        final UpdateForm updateForm = envelope.payload();

        LOGGER.info("progression.command.handler.update-form with courtFormId: {} for case: {}", updateForm.getCourtFormId(), updateForm.getCaseId());

        final String userId = envelope.metadata().userId()
                .orElseThrow(() -> new IllegalStateException(NO_USER_ID_PRESENT));

        final EventStream eventStream = eventSource.getStreamById(updateForm.getCaseId());

        final CaseAggregate caseAggregate = aggregateService.get(eventStream, CaseAggregate.class);
        final Stream<Object> events = caseAggregate.updateForm(updateForm.getCaseId(),
                updateForm.getFormData(),
                updateForm.getCourtFormId(),
                fromString(userId));

        eventStream.append(events.map(toEnvelopeWithMetadataFrom(envelope)));
    }

    @Handles("progression.command.finalise-form")
    public void handleFinaliseForm(final Envelope<FinaliseForm> envelope) throws EventStreamException {
        final FinaliseForm finaliseForm = envelope.payload();

        LOGGER.info("progression.command.finalise-form with courtFormId: {} for case: {}", finaliseForm.getCourtFormId(), finaliseForm.getCaseId());

        final String userId = envelope.metadata().userId()
                .orElseThrow(() -> new IllegalStateException(NO_USER_ID_PRESENT));

        final EventStream eventStream = eventSource.getStreamById(finaliseForm.getCaseId());

        final CaseAggregate caseAggregate = aggregateService.get(eventStream, CaseAggregate.class);
        final Stream<Object> events = caseAggregate.finaliseForm(finaliseForm.getCaseId(),
                finaliseForm.getCourtFormId(),
                fromString(userId),
                finaliseForm.getFinalisedFormData(),
                getLatestSittingDay(caseAggregate.getLatestHearingId()));

        eventStream.append(events.map(toEnvelopeWithMetadataFrom(envelope)));
    }

    private ZonedDateTime getLatestSittingDay(final UUID hearingId) {
        if (nonNull(hearingId)) {
            final EventStream eventStream = eventSource.getStreamById(hearingId);
            final HearingAggregate hearingAggregate = aggregateService.get(eventStream, HearingAggregate.class);
            if (nonNull(hearingAggregate)) {
                return hearingAggregate.getLatestSittingDay();
            }
        }
        return null;
    }

    @Handles("progression.command.update-form-defendants")
    public void updateFormDefendants(final Envelope<UpdateFormDefendants> envelope) throws EventStreamException {
        final UpdateFormDefendants payload = envelope.payload();

        LOGGER.info("progression.command.update-form-defendants with form ref id: {} for case: {}", payload.getCourtFormId(), payload.getCaseId());

        final String userId = envelope.metadata().userId()
                .orElseThrow(() -> new IllegalStateException(NO_USER_ID_PRESENT));

        final EventStream eventStream = eventSource.getStreamById(payload.getCaseId());

        final CaseAggregate caseAggregate = aggregateService.get(eventStream, CaseAggregate.class);
        final Stream<Object> events = caseAggregate.updateFormDefendants(payload.getCourtFormId(),
                payload.getCaseId(),
                extractDefendantsWithOffencesMap(payload.getFormDefendants()),
                fromString(userId),
                payload.getFormType());

        eventStream.append(events.map(toEnvelopeWithMetadataFrom(envelope)));
    }


    @Handles("progression.command.request-edit-form")
    public void handleRequestEditForm(final Envelope<RequestEditForm> envelope) throws EventStreamException {
        final RequestEditForm requestEditForm = envelope.payload();
        final String userId = envelope.metadata().userId()
                .orElseThrow(() -> new IllegalStateException(NO_USER_ID_PRESENT));

        LOGGER.info("progression.command.request-edit-form with courtFormId: {} for case: {}", requestEditForm.getCourtFormId(), requestEditForm.getCaseId());

        final EventStream eventStream = eventSource.getStreamById(requestEditForm.getCaseId());

        final CaseAggregate caseAggregate = aggregateService.get(eventStream, CaseAggregate.class);
        final Stream<Object> events = caseAggregate.requestEditForm(requestEditForm.getCaseId(),
                requestEditForm.getCourtFormId(),
                fromString(userId),
                getDurationMapByFormType(),
                new UtcClock().now(),
                nonNull(requestEditForm.getExtend()) ? requestEditForm.getExtend() : Boolean.FALSE.booleanValue(),
                nonNull(requestEditForm.getExtendTime()) && requestEditForm.getExtendTime() > 0 ? requestEditForm.getExtendTime() : DEFAULT_LOCK_EXTEND_TIME_IN_MINUTES);

        eventStream.append(events.map(toEnvelopeWithMetadataFrom(envelope)));
    }

    private List<UUID> extractDefendantsWithOffencesMap(List<FormDefendants> defendants) {
        return defendants.stream().map(FormDefendants::getDefendantId).collect(Collectors.toList());
    }

    private Map getDurationMapByFormType() {
        if (isNull(lockDurationMapByFormType)) {
            lockDurationMapByFormType = ImmutableMap.of(
                    PET, parseInt(bcmFormLockDurationInMinutes) ,
                    BCM, parseInt(bcmFormLockDurationInMinutes),
                    PTPH, parseInt(ptphFormLockDurationInMinutes));
        }
        return lockDurationMapByFormType;
    }
}
