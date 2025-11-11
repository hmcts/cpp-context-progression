package uk.gov.moj.cpp.progression.handler;

import static java.util.Objects.nonNull;
import static uk.gov.justice.services.core.annotation.Component.COMMAND_HANDLER;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;

import uk.gov.justice.core.courts.AddConvictingCourt;
import uk.gov.justice.core.courts.AddConvictingInformation;
import uk.gov.justice.core.courts.CourtCentre;
import uk.gov.justice.core.courts.Offence;
import uk.gov.justice.core.courts.ProsecutionCase;
import uk.gov.justice.services.core.aggregate.AggregateService;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.requester.Requester;
import uk.gov.justice.services.eventsourcing.source.core.EventSource;
import uk.gov.justice.services.eventsourcing.source.core.EventStream;
import uk.gov.justice.services.eventsourcing.source.core.exception.EventStreamException;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.moj.cpp.progression.aggregate.CaseAggregate;
import uk.gov.moj.cpp.progression.service.ReferenceDataOffenceService;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.inject.Inject;
import javax.json.JsonObject;
import javax.json.JsonValue;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ServiceComponent(COMMAND_HANDLER)
public class AddConvictingCourtCommandHandler extends AbstractCommandHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger(AddConvictingCourtCommandHandler.class.getName());

    @Inject
    private EventSource eventSource;

    @Inject
    private AggregateService aggregateService;

    @Inject
    @ServiceComponent(COMMAND_HANDLER)
    private Requester requester;

    @Inject
    private ReferenceDataOffenceService referenceDataOffenceService;

    private static final String SOW_REF_VALUE = "MoJ";

    @Handles("progression.command.add-convicting-court")
    public void handle(final Envelope<AddConvictingCourt> addConvictionCourtEnv) throws EventStreamException {

            LOGGER.info("Handling progression.command.add-convicting-court {}", addConvictionCourtEnv.payload());

        final EventStream eventStream = eventSource.getStreamById(addConvictionCourtEnv.payload().getCaseId());
        final CaseAggregate caseAggregate = aggregateService.get(eventStream, CaseAggregate.class);
        final ProsecutionCase prosecutionCase = caseAggregate.getProsecutionCase();
        final UUID defendantId = prosecutionCase.getDefendants().get(0).getId(); //TODO::
        final UUID prosecutionCaseId = prosecutionCase.getId();
        final List<AddConvictingInformation> addConvictingInfoList = addConvictionCourtEnv.payload().getAddConvictingInformation();
        final List<uk.gov.justice.core.courts.Offence> existingOffences = caseAggregate.getDefendantCaseOffences().get(defendantId);
        final List<String> offenceCodes = existingOffences.stream().map(Offence::getOffenceCode).collect(Collectors.toList());
        LOGGER.info("offenceCodes {}", offenceCodes);
        final Optional<String> sowRef = getSowRef(prosecutionCase);
        final Optional<List<JsonObject>> referenceDataOffences = referenceDataOffenceService
                .getMultipleOffencesByOffenceCodeList(offenceCodes, envelopeFrom(addConvictionCourtEnv.metadata(), JsonValue.NULL), requester, sowRef);
        LOGGER.info("referenceDataOffences {}", referenceDataOffences);
        final List<uk.gov.justice.core.courts.Offence> updatedOffences = existingOffences.stream().map(existingOffence -> {
           final uk.gov.justice.core.courts.Offence updatedOffence;
           final  Optional <CourtCentre> courtCentre = getCourtCentreByOffenceId(addConvictingInfoList, existingOffence.getId());
            if (courtCentre.isPresent()) {
                updatedOffence = uk.gov.justice.core.courts.Offence.offence().withValuesFrom(existingOffence).withConvictingCourt(courtCentre.get()).build();
            } else {
                updatedOffence = existingOffence;
            }
            return updatedOffence;
        }).collect(Collectors.toList());
        LOGGER.info("updatedOffences {}", updatedOffences);
        final Stream<Object> events = caseAggregate.updateOffences(updatedOffences, existingOffences, prosecutionCaseId, defendantId, referenceDataOffences);
        appendEventsToStream(addConvictionCourtEnv, eventStream, events);
    }
    private Optional <CourtCentre> getCourtCentreByOffenceId(List<AddConvictingInformation> addConvictingInfoList, UUID offenceId ){
        return addConvictingInfoList.stream().filter(item-> item.getOffenceId().equals(offenceId))
                .map(AddConvictingInformation::getConvictingCourt).findFirst();
    }

    private static Optional<String> getSowRef(final ProsecutionCase prosecutionCase) {
        boolean isCivil = nonNull(prosecutionCase.getIsCivil()) && prosecutionCase.getIsCivil();
        return isCivil ? Optional.of(SOW_REF_VALUE) : Optional.empty();
    }
}
