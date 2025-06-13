package uk.gov.moj.cpp.progression.handler;

import static uk.gov.justice.services.core.enveloper.Enveloper.toEnvelopeWithMetadataFrom;

import uk.gov.justice.core.courts.Defendant;
import uk.gov.justice.core.courts.ProsecutionCase;
import uk.gov.justice.progression.courts.AssociateDefenceOrganisation;
import uk.gov.justice.progression.courts.DisassociateDefenceOrganisation;
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
import uk.gov.moj.cpp.progression.aggregate.ApplicationAggregate;
import uk.gov.moj.cpp.progression.aggregate.CaseAggregate;
import uk.gov.moj.cpp.progression.command.handler.DisassociateDefenceOrganisationForApplication;
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

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ServiceComponent(Component.COMMAND_HANDLER)
public class DefenceOrganisationHandler {
    private static final Logger LOGGER =
            LoggerFactory.getLogger(DefenceOrganisationHandler.class.getName());
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

    @Handles("progression.command.handler.associate-defence-organisation")
    public void handleAssociation(final Envelope<AssociateDefenceOrganisation> envelope) throws EventStreamException {
        LOGGER.debug("progression.command.handler.associate-defence-organisation {}", envelope.payload());

        final AssociateDefenceOrganisation associateDefenceOrganisation = envelope.payload();
        final JsonEnvelope jsonEnvelope = JsonEnvelope.envelopeFrom(envelope.metadata(), JsonValue.NULL);
        final UUID caseId = associateDefenceOrganisation.getCaseId();
        final UUID defendantId = associateDefenceOrganisation.getDefendantId();
        String laaContractNumber = associateDefenceOrganisation.getLaaContractNumber();

        final OrganisationDetails organisationDetails = usersGroupService.getOrganisationDetailsForOrganisationId(envelope, associateDefenceOrganisation.getOrganisationId().toString());
        if(StringUtils.isEmpty(laaContractNumber)){
            laaContractNumber = organisationDetails.getLaaContractNumber();
        }

        final Optional<JsonObject> optionalProsecutionCase = prosecutionCaseQueryService.getProsecutionCase(jsonEnvelope, caseId.toString());
        if (!optionalProsecutionCase.isPresent()) {
            throw new IllegalStateException(String.format("Unable to find the case %s", caseId));
        }

        final ProsecutionCase prosecutionCase = jsonObjectToObjectConverter.convert(optionalProsecutionCase.get().getJsonObject(PROSECUTION_CASE), ProsecutionCase.class);
        final Optional<Defendant> optionalDefendant = prosecutionCase.getDefendants().stream().filter(def -> def.getId().equals(defendantId)).findFirst();

        if (!optionalDefendant.isPresent()) {
            throw new IllegalStateException(String.format("Unable to find the defendant %s", defendantId));
        }

        final EventStream eventStream = eventSource.getStreamById(caseId);
        final CaseAggregate caseAggregate = aggregateService.get(eventStream, CaseAggregate.class);
        final Stream<Object> events = caseAggregate.receiveAssociateDefenceOrganisation(associateDefenceOrganisation.getOrganisationName(), defendantId, caseId, laaContractNumber, associateDefenceOrganisation.getStartDate(), associateDefenceOrganisation.getRepresentationType().toString(), organisationDetails);
        appendEventsToStream(envelope, eventStream, events);
    }

    @Handles("progression.command.handler.disassociate-defence-organisation")
    public void handleDisassociation(final Envelope<DisassociateDefenceOrganisation> envelope) throws EventStreamException {
        LOGGER.debug("progression.command.handler.disassociate-defence-organisation {}", envelope.payload());

        final DisassociateDefenceOrganisation disassociateDefenceOrganisation = envelope.payload();
        final JsonEnvelope jsonEnvelope = JsonEnvelope.envelopeFrom(envelope.metadata(), JsonValue.NULL);
        final UUID caseId = disassociateDefenceOrganisation.getCaseId();
        final UUID defendantId = disassociateDefenceOrganisation.getDefendantId();

        final Optional<JsonObject> optionalProsecutionCase = prosecutionCaseQueryService.getProsecutionCase(jsonEnvelope, caseId.toString());
        if (!optionalProsecutionCase.isPresent()) {
            throw new IllegalStateException(String.format("Unable to find the case %s", caseId));
        }

        final ProsecutionCase prosecutionCase = jsonObjectToObjectConverter.convert(optionalProsecutionCase.get().getJsonObject(PROSECUTION_CASE), ProsecutionCase.class);
        final Optional<Defendant> optionalDefendant = prosecutionCase.getDefendants().stream().filter(def -> def.getId().equals(defendantId)).findFirst();

        if (!optionalDefendant.isPresent()) {
            throw new IllegalStateException(String.format("Unable to find the defendant %s", defendantId));
        }

        final EventStream eventStream = eventSource.getStreamById(caseId);
        final CaseAggregate caseAggregate = aggregateService.get(eventStream, CaseAggregate.class);
        final Stream<Object> events = caseAggregate.receiveDisAssociateDefenceOrganisation(defendantId, caseId, disassociateDefenceOrganisation.getOrganisationId());
        appendEventsToStream(envelope, eventStream, events);
    }

    @Handles("progression.command.handler.disassociate-defence-organisation-for-application")
    public void handleDisassociationForApplication(final Envelope<DisassociateDefenceOrganisationForApplication> envelope) throws EventStreamException {
        LOGGER.info("progression.command.handler.disassociate-defence-organisation-for-application {}", envelope.payload());

        final DisassociateDefenceOrganisationForApplication disassociateDefenceOrganisationForApplication = envelope.payload();
        final UUID applicationId = disassociateDefenceOrganisationForApplication.getApplicationId();
        final UUID defendantId = disassociateDefenceOrganisationForApplication.getDefendantId();
        final UUID organisationId = disassociateDefenceOrganisationForApplication.getOrganisationId();

        final EventStream eventStream = eventSource.getStreamById(applicationId);
        final ApplicationAggregate applicationAggregate = aggregateService.get(eventStream, ApplicationAggregate.class);
        final Stream<Object> events = applicationAggregate.disassociateDefenceOrganisationForApplication(defendantId, organisationId);
        appendEventsToStream(envelope, eventStream, events);
    }

    private void appendEventsToStream(final Envelope<?> envelope, final EventStream eventStream, final Stream<Object> events) throws EventStreamException {
        final JsonEnvelope jsonEnvelope = JsonEnvelope.envelopeFrom(envelope.metadata(), JsonValue.NULL);
        eventStream.append(
                events
                        .map(toEnvelopeWithMetadataFrom(jsonEnvelope)));
    }

}