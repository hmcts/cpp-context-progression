package uk.gov.moj.cpp.progression.handler;

import static java.util.UUID.fromString;
import static javax.json.Json.createObjectBuilder;
import static uk.gov.justice.services.core.enveloper.Enveloper.toEnvelopeWithMetadataFrom;
import static uk.gov.moj.cpp.progression.application.ApplicationCaseDefendantOrganisation.applicationCaseDefendantOrganisation;

import uk.gov.justice.core.courts.LaaReference;
import uk.gov.justice.core.courts.ReceiveRepresentationOrderForApplication;
import uk.gov.justice.core.courts.ReceiveRepresentationOrderForApplicationOnApplication;
import uk.gov.justice.services.core.aggregate.AggregateService;
import uk.gov.justice.services.core.annotation.Component;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.eventsourcing.source.core.EventSource;
import uk.gov.justice.services.eventsourcing.source.core.EventStream;
import uk.gov.justice.services.eventsourcing.source.core.exception.EventStreamException;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.progression.aggregate.ApplicationAggregate;
import uk.gov.moj.cpp.progression.application.ApplicationCaseDefendantOrganisation;
import uk.gov.moj.cpp.progression.command.handler.service.UsersGroupService;
import uk.gov.moj.cpp.progression.domain.pojo.OrganisationDetails;
import uk.gov.moj.cpp.progression.laa.LaaRepresentationOrder;
import uk.gov.moj.cpp.progression.service.LegalStatusReferenceDataService;
import uk.gov.moj.cpp.progression.service.OrganisationService;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;

import javax.inject.Inject;
import javax.json.JsonObject;
import javax.json.JsonValue;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ServiceComponent(Component.COMMAND_HANDLER)
public class ReceiveRepresentationOrderForApplicationHandler {
    private static final Logger LOGGER =
            LoggerFactory.getLogger(ReceiveRepresentationOrderForApplicationHandler.class.getName());
    @Inject
    private EventSource eventSource;

    @Inject
    private AggregateService aggregateService;

    @Inject
    LegalStatusReferenceDataService legalStatusReferenceDataService;

    @Inject
    UsersGroupService usersGroupService;

    @Inject
    private OrganisationService organisationService;

    @Handles("progression.command.handler.receive-representationOrder-for-application")
    public void handle(final Envelope<ReceiveRepresentationOrderForApplication> envelope) throws EventStreamException {
        if(LOGGER.isDebugEnabled()) {
            LOGGER.debug("progression.command.handler.receive-representationOrder-for-application {}", envelope.payload());
        }
        final ReceiveRepresentationOrderForApplication receiveRepresentationOrderForApplication = envelope.payload();
        final JsonEnvelope jsonEnvelope = JsonEnvelope.envelopeFrom(envelope.metadata(), createObjectBuilder().build());
        final String statusCode = receiveRepresentationOrderForApplication.getStatusCode();
        final UUID applicationId = receiveRepresentationOrderForApplication.getApplicationId();
        final EventStream eventStream = eventSource.getStreamById(applicationId);
        final ApplicationAggregate applicationAggregate = aggregateService.get(eventStream, ApplicationAggregate.class);
        final List<ApplicationCaseDefendantOrganisation> currentDefenceOrganisationForSubjectsCase =
                applicationAggregate.getApplicationCaseDefendantOrganisations().stream()
                        .map(applicationCaseDefendantOrganisation -> mapToUpdatedOrganisation(applicationCaseDefendantOrganisation, envelope))
                        .toList();

        final String laaContractNumber = receiveRepresentationOrderForApplication.getDefenceOrganisation().getLaaContractNumber();
        final OrganisationDetails organisationDetailsForLaaContractNumber = usersGroupService.getOrganisationDetailsForLAAContractNumber(envelope, laaContractNumber);
        final Optional<JsonObject> optionalLegalStatus = legalStatusReferenceDataService.getLegalStatusByStatusIdAndStatusCode
                (jsonEnvelope, statusCode);

        if (optionalLegalStatus.isEmpty()) {
            LOGGER.error("Unable to get Ref Data for Legal Status by Status Code {}", statusCode);
            return;
        }

        final JsonObject legalStatus = optionalLegalStatus.get();
        final LaaReference laaReference = LaaReference.laaReference()
                .withStatusCode(statusCode)
                .withStatusId(fromString(legalStatus.getString("id")))
                .withStatusDescription(legalStatus.getString("statusDescription"))
                .withStatusDate(receiveRepresentationOrderForApplication.getStatusDate())
                .withApplicationReference(receiveRepresentationOrderForApplication.getApplicationReference())
                .withOffenceLevelStatus(legalStatus.getString("defendantLevelStatus", null))
                .withEffectiveStartDate(receiveRepresentationOrderForApplication.getEffectiveStartDate())
                .withEffectiveEndDate(receiveRepresentationOrderForApplication.getEffectiveEndDate())
                .withLaaContractNumber(receiveRepresentationOrderForApplication.getDefenceOrganisation().getLaaContractNumber())
                .build();

        final LaaRepresentationOrder laaRepresentationOrder = LaaRepresentationOrder.laaRepresentationOrder()
                .withLaaReference(laaReference)
                .withDefenceOrganisation(receiveRepresentationOrderForApplication.getDefenceOrganisation())
                .build();
        final Stream<Object> events = applicationAggregate.receiveRepresentationOrderForApplication(applicationId, receiveRepresentationOrderForApplication.getSubjectId(), receiveRepresentationOrderForApplication.getOffenceId(), laaRepresentationOrder, organisationDetailsForLaaContractNumber, currentDefenceOrganisationForSubjectsCase);
        appendEventsToStream(envelope, eventStream, events);
    }

    @Handles("progression.command.handler.receive-representationOrder-for-application-on-application")
    public void handleOnApplication(final Envelope<ReceiveRepresentationOrderForApplicationOnApplication> envelope) throws EventStreamException {
        if(LOGGER.isDebugEnabled()) {
            LOGGER.debug("progression.command.handler.receive-representationOrder-for-application-on-application {}", envelope.payload());
        }
        final ReceiveRepresentationOrderForApplicationOnApplication receiveRepresentationOrderForApplicationOnApplication = envelope.payload();
        final JsonEnvelope jsonEnvelope = JsonEnvelope.envelopeFrom(envelope.metadata(), createObjectBuilder().build());
        final String statusCode = receiveRepresentationOrderForApplicationOnApplication.getStatusCode();
        final UUID applicationId = receiveRepresentationOrderForApplicationOnApplication.getApplicationId();
        final EventStream eventStream = eventSource.getStreamById(applicationId);
        final ApplicationAggregate applicationAggregate = aggregateService.get(eventStream, ApplicationAggregate.class);
        final List<ApplicationCaseDefendantOrganisation> currentDefenceOrganisationForSubjectsCase =
                applicationAggregate.getApplicationCaseDefendantOrganisations().stream()
                        .map(applicationCaseDefendantOrganisation -> mapToUpdatedOrganisation(applicationCaseDefendantOrganisation, envelope))
                        .toList();

        final String laaContractNumber = receiveRepresentationOrderForApplicationOnApplication.getDefenceOrganisation().getLaaContractNumber();
        final OrganisationDetails organisationDetailsForLaaContractNumber = usersGroupService.getOrganisationDetailsForLAAContractNumber(envelope, laaContractNumber);
        final Optional<JsonObject> optionalLegalStatus = legalStatusReferenceDataService.getLegalStatusByStatusIdAndStatusCode
                (jsonEnvelope, statusCode);

        if (optionalLegalStatus.isEmpty()) {
            LOGGER.error("Unable to get Ref Data for Legal Status by Status Code {}", statusCode);
            return;
        }

        final JsonObject legalStatus = optionalLegalStatus.get();
        final LaaReference laaReference = LaaReference.laaReference()
                .withStatusCode(statusCode)
                .withStatusId(fromString(legalStatus.getString("id")))
                .withStatusDescription(legalStatus.getString("statusDescription"))
                .withStatusDate(receiveRepresentationOrderForApplicationOnApplication.getStatusDate())
                .withApplicationReference(receiveRepresentationOrderForApplicationOnApplication.getApplicationReference())
                .withOffenceLevelStatus(legalStatus.getString("defendantLevelStatus", null))
                .withEffectiveStartDate(receiveRepresentationOrderForApplicationOnApplication.getEffectiveStartDate())
                .withEffectiveEndDate(receiveRepresentationOrderForApplicationOnApplication.getEffectiveEndDate())
                .withLaaContractNumber(receiveRepresentationOrderForApplicationOnApplication.getDefenceOrganisation().getLaaContractNumber())
                .build();

        final LaaRepresentationOrder laaRepresentationOrder = LaaRepresentationOrder.laaRepresentationOrder()
                .withLaaReference(laaReference)
                .withDefenceOrganisation(receiveRepresentationOrderForApplicationOnApplication.getDefenceOrganisation())
                .build();
        final Stream<Object> events = applicationAggregate.receiveRepresentationOrderForApplicationOnApplication(applicationId, laaRepresentationOrder, organisationDetailsForLaaContractNumber, currentDefenceOrganisationForSubjectsCase);
        appendEventsToStream(envelope, eventStream, events);
    }

    private void appendEventsToStream(final Envelope<?> envelope, final EventStream eventStream, final Stream<Object> events) throws EventStreamException {
        final JsonEnvelope jsonEnvelope = JsonEnvelope.envelopeFrom(envelope.metadata(), JsonValue.NULL);
        eventStream.append(
                events
                        .map(toEnvelopeWithMetadataFrom(jsonEnvelope)));
    }

    private ApplicationCaseDefendantOrganisation mapToUpdatedOrganisation(final ApplicationCaseDefendantOrganisation organisation, final Envelope<?> envelope) {
        final UUID defendantId = organisation.getDefendantId();

        if (defendantId == null) {
            return organisation;
        }

        JsonObject associatedOrganisation = organisationService.getAssociatedOrganisationForApplication(envelope, defendantId.toString());
        String associatedOrganisationIdString = associatedOrganisation.getString("organisationId", null);

        return (associatedOrganisationIdString != null)
                ? applicationCaseDefendantOrganisation()
                .withValuesFrom(organisation)
                .withOrganisationId(UUID.fromString(associatedOrganisationIdString))
                .build()
                : organisation;
    }

}