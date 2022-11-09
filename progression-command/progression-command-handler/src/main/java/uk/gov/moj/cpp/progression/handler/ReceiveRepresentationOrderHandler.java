package uk.gov.moj.cpp.progression.handler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.gov.justice.core.courts.LaaReference;
import uk.gov.justice.core.courts.ReceiveRepresentationOrderForDefendant;
import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.core.aggregate.AggregateService;
import uk.gov.justice.services.core.annotation.Component;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.eventsourcing.source.core.EventSource;
import uk.gov.justice.services.eventsourcing.source.core.EventStream;
import uk.gov.justice.services.eventsourcing.source.core.exception.EventStreamException;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.progression.aggregate.CaseAggregate;
import uk.gov.moj.cpp.progression.command.handler.service.UsersGroupService;
import uk.gov.moj.cpp.progression.domain.pojo.OrganisationDetails;
import uk.gov.moj.cpp.progression.service.LegalStatusReferenceDataService;
import uk.gov.moj.cpp.progression.service.ProsecutionCaseQueryService;

import javax.inject.Inject;
import javax.json.JsonObject;
import javax.json.JsonValue;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;

import static java.util.Objects.nonNull;
import static java.util.UUID.fromString;
import static uk.gov.justice.services.core.enveloper.Enveloper.toEnvelopeWithMetadataFrom;

@ServiceComponent(Component.COMMAND_HANDLER)
public class ReceiveRepresentationOrderHandler {
    private static final Logger LOGGER =
            LoggerFactory.getLogger(ReceiveRepresentationOrderHandler.class.getName());
    @Inject
    private EventSource eventSource;

    @Inject
    private AggregateService aggregateService;

    @Inject
    LegalStatusReferenceDataService legalStatusReferenceDataService;

    @Inject
    UsersGroupService usersGroupService;

    @Inject
    ProsecutionCaseQueryService prosecutionCaseQueryService;

    @Inject
    private JsonObjectToObjectConverter jsonObjectToObjectConverter;

    @Handles("progression.command.handler.receive-representationOrder-for-defendant")
    public void handle(final Envelope<ReceiveRepresentationOrderForDefendant> envelope) throws EventStreamException {

        LOGGER.debug("progression.command.handler.receive-representationOrder-for-defendant {}", envelope.payload());
        final ReceiveRepresentationOrderForDefendant receiveRepresentationOrderForDefendant = envelope.payload();
        final JsonEnvelope jsonEnvelope = JsonEnvelope.envelopeFrom(envelope.metadata(), JsonValue.NULL);
        final String statusCode = receiveRepresentationOrderForDefendant.getStatusCode();
        final String laaContractNumber = receiveRepresentationOrderForDefendant.getDefenceOrganisation().getLaaContractNumber();
        final OrganisationDetails organisationDetails = usersGroupService.getOrganisationDetailsForLAAContractNumber(envelope, laaContractNumber);
        String associatedOrganisationId = null;
        if(nonNull(receiveRepresentationOrderForDefendant.getAssociatedOrganisationId())) {
            associatedOrganisationId = receiveRepresentationOrderForDefendant.getAssociatedOrganisationId().toString();
        }
        final Optional<JsonObject> optionalLegalStatus = legalStatusReferenceDataService.getLegalStatusByStatusIdAndStatusCode
                (jsonEnvelope, statusCode);
        if (optionalLegalStatus.isPresent()) {
                final JsonObject legalStatus = optionalLegalStatus.get();
                final LaaReference laaReference = LaaReference.laaReference()
                        .withStatusCode(statusCode)
                        .withStatusId(fromString(legalStatus.getString("id")))
                        .withStatusDescription(legalStatus.getString("statusDescription"))
                        .withStatusDate(receiveRepresentationOrderForDefendant.getStatusDate())
                        .withApplicationReference(receiveRepresentationOrderForDefendant.getApplicationReference())
                        .withOffenceLevelStatus(legalStatus.getString("defendantLevelStatus", null))
                        .withEffectiveStartDate(receiveRepresentationOrderForDefendant.getEffectiveStartDate())
                        .withEffectiveEndDate(receiveRepresentationOrderForDefendant.getEffectiveEndDate())
                        .withLaaContractNumber(receiveRepresentationOrderForDefendant.getDefenceOrganisation().getLaaContractNumber())
                        .build();
                final UUID prosecutionCaseId = receiveRepresentationOrderForDefendant.getProsecutionCaseId();
                final EventStream eventStream = eventSource.getStreamById(prosecutionCaseId);
                final CaseAggregate caseAggregate = aggregateService.get(eventStream, CaseAggregate.class);
                final Stream<Object> events = caseAggregate.receiveRepresentationOrderForDefendant(receiveRepresentationOrderForDefendant, laaReference, organisationDetails , associatedOrganisationId);
                appendEventsToStream(envelope, eventStream, events);

        } else {
            LOGGER.error("Unable to get Ref Data for Legal Status by Status Code {}", statusCode);
        }
    }

    private void appendEventsToStream(final Envelope<?> envelope, final EventStream eventStream, final Stream<Object> events) throws EventStreamException {
        final JsonEnvelope jsonEnvelope = JsonEnvelope.envelopeFrom(envelope.metadata(), JsonValue.NULL);
        eventStream.append(
                events
                        .map(toEnvelopeWithMetadataFrom(jsonEnvelope)));
    }

}