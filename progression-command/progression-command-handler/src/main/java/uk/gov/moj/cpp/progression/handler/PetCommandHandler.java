package uk.gov.moj.cpp.progression.handler;

import static java.util.Optional.ofNullable;
import static java.util.UUID.fromString;
import static uk.gov.justice.core.courts.FormType.PET;
import static uk.gov.justice.services.core.annotation.Component.COMMAND_HANDLER;
import static uk.gov.justice.services.core.enveloper.Enveloper.toEnvelopeWithMetadataFrom;

import uk.gov.justice.core.courts.CreatePetForm;
import uk.gov.justice.core.courts.FinalisePetForm;
import uk.gov.justice.core.courts.PetDefendants;
import uk.gov.justice.core.courts.ReceivePetDetail;
import uk.gov.justice.core.courts.ReceivePetForm;
import uk.gov.justice.core.courts.UpdatePetDetail;
import uk.gov.justice.core.courts.UpdatePetForm;
import uk.gov.justice.core.courts.UpdatePetFormForDefendant;
import uk.gov.justice.services.core.aggregate.AggregateService;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.eventsourcing.source.core.EventSource;
import uk.gov.justice.services.eventsourcing.source.core.EventStream;
import uk.gov.justice.services.eventsourcing.source.core.exception.EventStreamException;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.moj.cpp.progression.aggregate.CaseAggregate;

import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ServiceComponent(COMMAND_HANDLER)
public class PetCommandHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(PetCommandHandler.class);

    @Inject
    EventSource eventSource;

    @Inject
    AggregateService aggregateService;


    @Handles("progression.command.create-pet-form")
    public void handleCreatePetForm(final Envelope<CreatePetForm> envelope) throws EventStreamException {
        final CreatePetForm createPetForm = envelope.payload();

        LOGGER.info("progression.command.create-pet-form with petId: {} for case: {}", createPetForm.getPetId(), createPetForm.getCaseId());

        final Optional<String> userId = envelope.metadata().userId();
        final UUID userIdUUID = userId.isPresent() ? fromString(userId.get()) : null;
        final EventStream eventStream = eventSource.getStreamById(createPetForm.getCaseId());

        final CaseAggregate caseAggregate = aggregateService.get(eventStream, CaseAggregate.class);
        final Stream<Object> events = caseAggregate.createPetForm(createPetForm.getPetId(),
                createPetForm.getCaseId(),
                createPetForm.getFormId(),
                ofNullable(createPetForm.getIsYouth()),
                createPetForm.getPetDefendants().stream().map(PetDefendants::getDefendantId).collect(Collectors.toList()),
                createPetForm.getPetFormData(),
                userIdUUID,
                createPetForm.getSubmissionId(),
                createPetForm.getUserName(),
                PET);

        eventStream.append(events.map(toEnvelopeWithMetadataFrom(envelope)));
    }

    @Handles("progression.command.update-pet-form")
    public void handleUpdatePetForm(final Envelope<UpdatePetForm> envelope) throws EventStreamException {
        final UpdatePetForm updatePetForm = envelope.payload();

        LOGGER.info("progression.command.handler.update-pet-form with petId: {} for case: {}", updatePetForm.getPetId(), updatePetForm.getCaseId());

        final Optional<String> userId = envelope.metadata().userId();
        UUID userIdUUID = null;
        if (userId.isPresent()) {
            userIdUUID = fromString(userId.get());
        }
        final EventStream eventStream = eventSource.getStreamById(updatePetForm.getCaseId());

        final CaseAggregate caseAggregate = aggregateService.get(eventStream, CaseAggregate.class);
        final Stream<Object> events = caseAggregate.updatePetForm(updatePetForm.getCaseId(),
                updatePetForm.getPetFormData(),
                updatePetForm.getPetId(),
                userIdUUID);

        eventStream.append(events.map(toEnvelopeWithMetadataFrom(envelope)));
    }

    @Handles("progression.command.receive-pet-form")
    public void handleReceivePetForm(final Envelope<ReceivePetForm> envelope) throws EventStreamException {
        final ReceivePetForm receivePetForm = envelope.payload();

        LOGGER.info("progression.command.receive-pet-form with petId: {} for case: {}", receivePetForm.getPetId(), receivePetForm.getCaseId());

        final EventStream eventStream = eventSource.getStreamById(receivePetForm.getCaseId());

        final CaseAggregate caseAggregate = aggregateService.get(eventStream, CaseAggregate.class);
        final Stream<Object> events = caseAggregate.receivePetForm(receivePetForm.getPetId(),
                receivePetForm.getCaseId(),
                receivePetForm.getFormId(),
                receivePetForm.getPetDefendants().stream().map(PetDefendants::getDefendantId).collect(Collectors.toList()));

        eventStream.append(events.map(toEnvelopeWithMetadataFrom(envelope)));
    }

    @Handles("progression.command.update-pet-form-for-defendant")
    public void handleUpdatePetFormForDefendant(final Envelope<UpdatePetFormForDefendant> envelope) throws EventStreamException {
        final UpdatePetFormForDefendant updatePetFormForDefendant = envelope.payload();

        LOGGER.info("progression.command.update-pet-form-for-defendant with petId: {} for case: {}", updatePetFormForDefendant.getPetId(), updatePetFormForDefendant.getCaseId());

        final Optional<String> userId = envelope.metadata().userId();
        final UUID userIdUUID = userId.isPresent() ? fromString(userId.get()) : null;

        final EventStream eventStream = eventSource.getStreamById(updatePetFormForDefendant.getCaseId());

        final CaseAggregate caseAggregate = aggregateService.get(eventStream, CaseAggregate.class);
        final Stream<Object> events = caseAggregate.updatePetFormForDefendant(updatePetFormForDefendant.getPetId(),
                updatePetFormForDefendant.getCaseId(),
                updatePetFormForDefendant.getDefendantId(),
                updatePetFormForDefendant.getDefendantData(),
                userIdUUID
        );

        eventStream.append(events.map(toEnvelopeWithMetadataFrom(envelope)));
    }

    @Handles("progression.command.finalise-pet-form")
    public void handleFinalisePetForm(final Envelope<FinalisePetForm> envelope) throws EventStreamException {
        final FinalisePetForm finalisePetForm = envelope.payload();

        LOGGER.info("progression.command.finalise-pet-form with petId: {} for case: {}", finalisePetForm.getPetId(), finalisePetForm.getCaseId());

        final Optional<String> userId = envelope.metadata().userId();
        UUID userIdUUID = null;
        if (userId.isPresent()) {
            userIdUUID = fromString(userId.get());
        }
        final EventStream eventStream = eventSource.getStreamById(finalisePetForm.getCaseId());

        final CaseAggregate caseAggregate = aggregateService.get(eventStream, CaseAggregate.class);
        final Stream<Object> events = caseAggregate.finalisePetForm(finalisePetForm.getCaseId(),
                finalisePetForm.getPetId(),
                userIdUUID,
                finalisePetForm.getFinalisedFormData());

        eventStream.append(events.map(toEnvelopeWithMetadataFrom(envelope)));
    }

    @Handles("progression.command.update-pet-detail")
    public void handleUpdatePetDetail(final Envelope<UpdatePetDetail> envelope) throws EventStreamException {
        final UpdatePetDetail updatePetDetail = envelope.payload();

        LOGGER.info("progression.command.update-pet-detail with petId: {} for case: {} with isYouth: {}", updatePetDetail.getPetId(), updatePetDetail.getCaseId(), updatePetDetail.getIsYouth());
        final Optional<String> userId = envelope.metadata().userId();
        UUID userIdUUID = null;
        if (userId.isPresent()) {
            userIdUUID = fromString(userId.get());
        }
        final EventStream eventStream = eventSource.getStreamById(updatePetDetail.getCaseId());

        final CaseAggregate caseAggregate = aggregateService.get(eventStream, CaseAggregate.class);
        final Stream<Object> events = caseAggregate.updatePetDetail(updatePetDetail.getCaseId(),
                updatePetDetail.getPetId(),
                ofNullable(updatePetDetail.getIsYouth()),
                updatePetDetail.getPetDefendants(),
                userIdUUID);

        eventStream.append(events.map(toEnvelopeWithMetadataFrom(envelope)));
    }

    @Handles("progression.command.receive-pet-detail")
    public void handleReceivePetDetail(final Envelope<ReceivePetDetail> envelope) throws EventStreamException {
        final ReceivePetDetail receivePetDetail = envelope.payload();

        LOGGER.info("progression.command.receive-pet-detail with petId: {} for case: {}", receivePetDetail.getPetId(), receivePetDetail.getCaseId());

        final Optional<String> userId = envelope.metadata().userId();
        UUID userIdUUID = null;
        if (userId.isPresent()) {
            userIdUUID = fromString(userId.get());
        }
        final EventStream eventStream = eventSource.getStreamById(receivePetDetail.getCaseId());

        final CaseAggregate caseAggregate = aggregateService.get(eventStream, CaseAggregate.class);
        final Stream<Object> events = caseAggregate.receivePetDetail(receivePetDetail.getCaseId(),
                receivePetDetail.getPetId(),
                receivePetDetail.getPetDefendants(),
                userIdUUID);

        eventStream.append(events.map(toEnvelopeWithMetadataFrom(envelope)));
    }
}
