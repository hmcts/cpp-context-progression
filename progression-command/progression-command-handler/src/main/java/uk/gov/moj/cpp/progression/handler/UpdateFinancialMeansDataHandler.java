package uk.gov.moj.cpp.progression.handler;

import static uk.gov.moj.cpp.progression.helper.CourtDocumentHelper.setDefaults;

import uk.gov.justice.core.courts.CourtDocument;
import uk.gov.justice.core.courts.CreateCourtDocument;
import uk.gov.justice.core.courts.Material;
import uk.gov.justice.services.core.aggregate.AggregateService;
import uk.gov.justice.services.core.annotation.Component;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.eventsourcing.source.core.EventSource;
import uk.gov.justice.services.eventsourcing.source.core.EventStream;
import uk.gov.justice.services.eventsourcing.source.core.exception.EventStreamException;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.moj.cpp.progression.aggregate.CaseAggregate;

import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ServiceComponent(Component.COMMAND_HANDLER)
public class UpdateFinancialMeansDataHandler {

    @Inject
    private EventSource eventSource;

    @Inject
    private AggregateService aggregateService;

    private static final Logger LOGGER =
            LoggerFactory.getLogger(UpdateFinancialMeansDataHandler.class.getName());

    @Handles("progression.command.update-financial-means-data")
    public void handle(final Envelope<CreateCourtDocument> createCourtDocumentEnvelope) throws EventStreamException {
        LOGGER.debug("progression.command.update-financial-means-data {}", createCourtDocumentEnvelope);
        final CourtDocument courtDocument = setDefaults(createCourtDocumentEnvelope.payload().getCourtDocument());

        if (isDefendantsFinancialData(courtDocument)) {
            updateEventStream(getDefendantProsecutionCaseId(courtDocument), getDefendantId(courtDocument), null,
                    courtDocument.getMaterials(), createCourtDocumentEnvelope);
        } else if (isApplicationFinancialData(courtDocument)) {
            updateEventStream(getApplicationProsecutionCaseId(courtDocument), null, getApplicationId(courtDocument),
                    courtDocument.getMaterials(), createCourtDocumentEnvelope);
        }
    }

    private UUID getApplicationId(final CourtDocument courtDocument) {
        return courtDocument.getDocumentCategory().getApplicationDocument().getApplicationId();
    }

    private UUID getDefendantId(final CourtDocument courtDocument) {
        return courtDocument.getDocumentCategory().getDefendantDocument().getDefendants().get(0);
    }

    private UUID getApplicationProsecutionCaseId(final CourtDocument courtDocument) {
        return courtDocument.getDocumentCategory().getApplicationDocument().getProsecutionCaseId();
    }

    private UUID getDefendantProsecutionCaseId(final CourtDocument courtDocument) {
        return courtDocument.getDocumentCategory().getDefendantDocument().getProsecutionCaseId();
    }

    private boolean isApplicationFinancialData(final CourtDocument courtDocument) {
        return courtDocument.getContainsFinancialMeans() && courtDocument.getDocumentCategory().getApplicationDocument() != null;
    }

    private boolean isDefendantsFinancialData(final CourtDocument courtDocument) {
        return courtDocument.getContainsFinancialMeans() && courtDocument.getDocumentCategory().getDefendantDocument() != null;
    }

    private void updateEventStream(final UUID prosecutionCaseId, final UUID defendantId, final UUID applicationId,
                                   final List<Material> materials,
                                   final Envelope<CreateCourtDocument> createCourtDocumentEnvelope)
            throws EventStreamException {

        final EventStream eventStream = eventSource.getStreamById(prosecutionCaseId);
        final CaseAggregate caseAggregate = aggregateService.get(eventStream, CaseAggregate.class);
        final Stream<Object> events = caseAggregate.addFinancialMeansData(prosecutionCaseId, defendantId, applicationId, materials.get(0));
        eventStream.append(events.map(Enveloper.toEnvelopeWithMetadataFrom(createCourtDocumentEnvelope)));
    }

}
