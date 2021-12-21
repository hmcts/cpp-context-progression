package uk.gov.moj.cpp.progression.handler;

import static java.util.Objects.nonNull;
import static java.util.UUID.fromString;
import static uk.gov.justice.services.core.enveloper.Enveloper.toEnvelopeWithMetadataFrom;

import uk.gov.justice.core.courts.Defendant;
import uk.gov.justice.core.courts.LaaReference;
import uk.gov.justice.core.courts.ProsecutionCase;
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

import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;

import javax.inject.Inject;
import javax.json.JsonObject;
import javax.json.JsonValue;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ServiceComponent(Component.COMMAND_HANDLER)
public class ReceiveRepresentationOrderHandler {
    private static final Logger LOGGER =
            LoggerFactory.getLogger(ReceiveRepresentationOrderHandler.class.getName());
    private static final String PROSECUTION_CASE = "prosecutionCase";

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
        final Optional<JsonObject> optionalProsecutionCase = prosecutionCaseQueryService.getProsecutionCase(jsonEnvelope, receiveRepresentationOrderForDefendant.getProsecutionCaseId().toString());
        if (optionalLegalStatus.isPresent() && optionalProsecutionCase.isPresent()) {
            final JsonObject prosecutionCaseJson = optionalProsecutionCase.get().getJsonObject(PROSECUTION_CASE);
            final ProsecutionCase prosecutionCase = jsonObjectToObjectConverter.convert(prosecutionCaseJson, ProsecutionCase.class);
            final Optional<Defendant> optionalDefendant = prosecutionCase.getDefendants().stream().filter(def -> def.getId().equals(receiveRepresentationOrderForDefendant.getDefendantId()))
                    .findFirst();
            if (optionalDefendant.isPresent()) {
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
                final Stream<Object> events = caseAggregate.receiveRepresentationOrderForDefendant(receiveRepresentationOrderForDefendant, laaReference, organisationDetails , associatedOrganisationId, optionalDefendant.get(), prosecutionCase);
                appendEventsToStream(envelope, eventStream, events);
            }
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