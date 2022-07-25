package uk.gov.moj.cpp.progression.handler;

import static java.util.Objects.nonNull;
import static uk.gov.justice.services.core.annotation.Component.COMMAND_HANDLER;
import static uk.gov.moj.cpp.progression.service.MatchedDefendantLoadService.appendEventsToStream;

import uk.gov.justice.core.courts.Address;
import uk.gov.justice.core.courts.ContactNumber;
import uk.gov.justice.core.courts.ProsecutionCase;
import uk.gov.justice.core.courts.ProsecutionCaseIdentifier;
import uk.gov.justice.core.courts.UpdateCpsProsecutorDetails;
import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.core.aggregate.AggregateService;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.requester.Requester;
import uk.gov.justice.services.eventsourcing.source.core.EventSource;
import uk.gov.justice.services.eventsourcing.source.core.EventStream;
import uk.gov.justice.services.eventsourcing.source.core.exception.EventStreamException;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.progression.aggregate.CaseAggregate;
import uk.gov.moj.cpp.progression.service.RefDataService;

import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;

import javax.inject.Inject;
import javax.json.JsonObject;

@ServiceComponent(COMMAND_HANDLER)
public class UpdateCpsProsecutorHandler {

    @Inject
    private EventSource eventSource;
    @Inject
    private AggregateService aggregateService;
    @Inject
    private JsonObjectToObjectConverter jsonObjectToObjectConverter;
    @Inject
    private RefDataService referenceDataService;
    @Inject
    @ServiceComponent(COMMAND_HANDLER)
    private Requester requester;

    @Handles("progression.command.update-cps-prosecutor-details")
    public void handleUpdateCpsProsecutor(final Envelope<UpdateCpsProsecutorDetails> envelope) throws EventStreamException {

        final UpdateCpsProsecutorDetails prosecutorDetails = envelope.payload();
        final UUID caseId = prosecutorDetails.getProsecutionCaseId();
        final EventStream streamById = eventSource.getStreamById(caseId);
        final CaseAggregate caseAggregate = aggregateService.get(streamById, CaseAggregate.class);
        final ProsecutionCaseIdentifier prosecutionCaseIdentifier = getProsecutionCaseIdentifier(prosecutorDetails);
        final Stream<Object> events = caseAggregate.updateCaseProsecutorDetails(prosecutionCaseIdentifier,
                prosecutorDetails.getOldCpsProsecutor());
        appendEventsToStream(envelope, streamById, events);
    }

    @Handles("progression.command.update-case-for-cps-prosecutor")
    public void handleUpdateCpsProsecutorFromReferenceData(final JsonEnvelope envelope) throws EventStreamException {
        final String caseId = envelope.payloadAsJsonObject().getString("caseId");
        final EventStream eventStream = eventSource.getStreamById(UUID.fromString(caseId));
        final CaseAggregate caseAggregate = aggregateService.get(eventStream, CaseAggregate.class);
        final ProsecutionCase prosecutionCase = caseAggregate.getProsecutionCase();
        if (prosecutionCase.getIsCpsOrgVerifyError() != null && !prosecutionCase.getIsCpsOrgVerifyError().booleanValue()) {
            return;
        }

        final Optional<JsonObject> prosecutor = getProsecutor(prosecutionCase.getCpsOrganisation(), prosecutionCase.getCpsOrganisationId(), envelope);
        final Stream<Object> events = caseAggregate.updateCaseProsecutorDetails(getProsecutionCaseIdentifier(prosecutor));
        appendEventsToStream(envelope, eventStream, events);
    }

    private Optional<JsonObject> getProsecutor(final String cpsOrganisation, final UUID cpsOrganisationId, final JsonEnvelope envelope) {
        if (nonNull(cpsOrganisation)) {
            return referenceDataService.getCPSProsecutorByOuCode(envelope, cpsOrganisation, requester);
        } else if (nonNull(cpsOrganisationId)) {
            return referenceDataService.getProsecutor(envelope, cpsOrganisationId, requester);
        }
        return Optional.empty();
    }

    private ProsecutionCaseIdentifier getProsecutionCaseIdentifier(Optional<JsonObject> prosecutor) {
        if (prosecutor.isPresent()) {
            return buildProsecutionCaseIdentifier(prosecutor.get());
        }
        return null;
    }

    private ProsecutionCaseIdentifier getProsecutionCaseIdentifier(UpdateCpsProsecutorDetails prosecutorDetails) {
        return ProsecutionCaseIdentifier.prosecutionCaseIdentifier().
                withCaseURN(prosecutorDetails.getCaseURN()).
                withAddress(prosecutorDetails.getAddress())
                .withProsecutionAuthorityReference(prosecutorDetails.getProsecutionAuthorityReference())
                .withProsecutionAuthorityCode(prosecutorDetails.getProsecutionAuthorityCode())
                .withProsecutionAuthorityId(prosecutorDetails.getProsecutionAuthorityId())
                .withProsecutionAuthorityName(prosecutorDetails.getProsecutionAuthorityName())
                .withContact(ContactNumber.contactNumber().withPrimaryEmail(prosecutorDetails.getContact()).build())
                .withMajorCreditorCode(prosecutorDetails.getMajorCreditorCode())
                .withProsecutionAuthorityOUCode(prosecutorDetails.getProsecutionAuthorityOUCode())
                .build();

    }


    private ProsecutionCaseIdentifier buildProsecutionCaseIdentifier(JsonObject cpsProsecutor) {

        final ProsecutionCaseIdentifier.Builder builder = ProsecutionCaseIdentifier.prosecutionCaseIdentifier();
        if (cpsProsecutor.get("informantEmailAddress") != null) {
            builder.withContact(ContactNumber.contactNumber().withPrimaryEmail(cpsProsecutor.getString("informantEmailAddress")).build());
        }
        if (cpsProsecutor.get("majorCreditorCode") != null) {
            builder.withMajorCreditorCode(cpsProsecutor.getString("majorCreditorCode"));
        }
        if (cpsProsecutor.get("shortName") != null) {
            builder.withProsecutionAuthorityCode(cpsProsecutor.getString("shortName"));
        }
        if (cpsProsecutor.get("fullName") != null) {
            builder.withProsecutionAuthorityName(cpsProsecutor.getString("fullName"));
        }
        if (cpsProsecutor.get("oucode") != null) {
            builder.withProsecutionAuthorityOUCode(cpsProsecutor.getString("oucode"));
        }
        if (cpsProsecutor.get("address") != null) {
            final Address address = jsonObjectToObjectConverter.convert((JsonObject) cpsProsecutor.get("address"), Address.class);
            builder.withAddress(address);
        }
        return builder.withProsecutionAuthorityId(UUID.fromString(cpsProsecutor.getString("id")))
                .build();
    }
}
