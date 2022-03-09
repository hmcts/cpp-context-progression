package uk.gov.moj.cpp.progression.handler;

import org.slf4j.Logger;
import uk.gov.justice.core.courts.AddCourtDocument;
import uk.gov.justice.core.courts.AddCourtDocumentV2;
import uk.gov.justice.core.courts.AddMaterialV2;
import uk.gov.justice.core.courts.CourtDocument;
import uk.gov.justice.services.common.converter.ObjectToJsonObjectConverter;
import uk.gov.justice.services.core.aggregate.AggregateService;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.eventsourcing.source.core.EventSource;
import uk.gov.justice.services.eventsourcing.source.core.EventStream;
import uk.gov.justice.services.eventsourcing.source.core.exception.EventStreamException;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.progression.aggregate.CaseAggregate;
import uk.gov.moj.cpp.progression.aggregate.CourtDocumentAggregate;
import uk.gov.moj.cpp.progression.command.handler.service.UsersGroupService;
import uk.gov.moj.cpp.progression.handler.courts.document.CourtDocumentEnricher;
import uk.gov.moj.cpp.progression.handler.courts.document.DefaultCourtDocumentFactory;
import uk.gov.moj.cpp.progression.handler.courts.document.DocumentTypeAccessProvider;
import uk.gov.moj.cpp.progression.helper.EnvelopeHelper;
import uk.gov.moj.cpp.referencedata.json.schemas.DocumentTypeAccess;

import javax.inject.Inject;
import javax.json.JsonObject;
import java.util.stream.Stream;


import static java.util.Objects.nonNull;
import static java.util.Optional.ofNullable;
import static uk.gov.justice.services.core.annotation.Component.COMMAND_HANDLER;
import static uk.gov.justice.services.core.enveloper.Enveloper.toEnvelopeWithMetadataFrom;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.justice.services.messaging.JsonEnvelope.metadataFrom;

@SuppressWarnings("squid:S1168")
@ServiceComponent(COMMAND_HANDLER)
public class AddCourtDocumentHandler {

    @Inject
    private EventSource eventSource;

    @Inject
    private AggregateService aggregateService;

    @Inject
    private CourtDocumentEnricher courtDocumentEnricher;

    @Inject
    private DocumentTypeAccessProvider documentTypeAccessProvider;

    @Inject
    private EnvelopeHelper envelopeHelper;

    @Inject
    private ObjectToJsonObjectConverter objectToJsonObjectConverter;

    @Inject
    private DefaultCourtDocumentFactory defaultCourtDocumentFactory;

    @Inject
    private UsersGroupService usersGroupService;

    @SuppressWarnings("squid:S1312")
    @Inject
    private Logger logger;

    @Handles("progression.command.add-court-document")
    public void handle(final Envelope<AddCourtDocument> addCourtDocumentEnvelope) throws EventStreamException {
        logger.debug("progression.command.add-court-document {}", addCourtDocumentEnvelope);

            final CourtDocument courtDocument = defaultCourtDocumentFactory.createDefaultCourtDocument(

                addCourtDocumentEnvelope
                        .payload()
                        .getCourtDocument());

        final JsonObject payload = objectToJsonObjectConverter.convert(courtDocument);
        final JsonEnvelope defaultCourtDocumentEnvelope = envelopeHelper.withMetadataInPayload(
                envelopeFrom(metadataFrom(addCourtDocumentEnvelope.metadata())
                        .withName("progression.command.add-court-document"), payload));

        final DocumentTypeAccess documentTypeAccess = documentTypeAccessProvider.getDocumentTypeAccess(courtDocument, defaultCourtDocumentEnvelope);

        final CourtDocument enrichedCourtDocument = courtDocumentEnricher.enrichWithMaterialUserGroups(
                courtDocument,
                documentTypeAccess);

        final EventStream eventStream = eventSource.getStreamById(enrichedCourtDocument.getCourtDocumentId());
        final CourtDocumentAggregate courtDocumentAggregate = aggregateService.get(
                eventStream,
                CourtDocumentAggregate.class);

        final JsonObject organisationDetailsForUser = usersGroupService.getOrganisationDetailsForUser(addCourtDocumentEnvelope).payload();

        final Stream<Object> events = courtDocumentAggregate.addCourtDocument(enrichedCourtDocument,  documentTypeAccess.getActionRequired(), addCourtDocumentEnvelope.payload().getMaterialId(), documentTypeAccess.getSection(),
                addCourtDocumentEnvelope.payload().getIsCpsCase(), addCourtDocumentEnvelope.payload().getIsUnbundledDocument(), organisationDetailsForUser);

        eventStream.append(events.map(toEnvelopeWithMetadataFrom(addCourtDocumentEnvelope)));
    }

    @Handles("progression.command.add-court-document-v2")
    public void handleV2(final Envelope<AddCourtDocumentV2> addCourtDocumentEnvelope) throws EventStreamException {
        logger.debug("progression.command.add-court-document-v2 {}", addCourtDocumentEnvelope);

        final CourtDocument courtDocument = defaultCourtDocumentFactory.createDefaultCourtDocument(
                addCourtDocumentEnvelope
                        .payload()
                        .getCourtDocument());

        final JsonObject payload = objectToJsonObjectConverter.convert(courtDocument);
        final JsonEnvelope defaultCourtDocumentEnvelope = envelopeHelper.withMetadataInPayload(
                envelopeFrom(metadataFrom(addCourtDocumentEnvelope.metadata())
                        .withName("progression.command.add-court-document"), payload));

        final DocumentTypeAccess documentTypeAccess = documentTypeAccessProvider.getDocumentTypeAccess(courtDocument, defaultCourtDocumentEnvelope);

        final CourtDocument enrichedCourtDocument = courtDocumentEnricher.enrichWithMaterialUserGroups(
                courtDocument,
                documentTypeAccess);

        final AddCourtDocumentV2 addCourtDocumentV2 = addCourtDocumentEnvelope.payload();

        if (nonNull(addCourtDocumentV2) && nonNull(addCourtDocumentV2.getMaterialSubmittedV2()) &&
                Boolean.TRUE.equals(addCourtDocumentV2.getMaterialSubmittedV2().getIsCpsCase())) {

            final AddMaterialV2 addMaterialV2 = addCourtDocumentV2.getMaterialSubmittedV2();

            if (nonNull(addMaterialV2) && nonNull(addMaterialV2.getProsecutionCaseSubject())) {
                final EventStream cpsUpdateEventStream = eventSource.getStreamById(addMaterialV2.getCaseId());
                final CaseAggregate caseAggregate = aggregateService.get(cpsUpdateEventStream, CaseAggregate.class);

                final Stream<Object> cpsEvents = caseAggregate.updateCpsDetails(addMaterialV2.getCaseId(), addMaterialV2.getDefendantId(), addMaterialV2.getProsecutionCaseSubject(), ofNullable(addCourtDocumentV2.getCpsFlag()).orElse(false) ? addMaterialV2.getProsecutionCaseSubject().getOuCode() : null);
                cpsUpdateEventStream.append(cpsEvents.map(toEnvelopeWithMetadataFrom(addCourtDocumentEnvelope)));
            }
        }


        final EventStream eventStream = eventSource.getStreamById(enrichedCourtDocument.getCourtDocumentId());
        final CourtDocumentAggregate courtDocumentAggregate = aggregateService.get(
                eventStream,
                CourtDocumentAggregate.class);

        final JsonObject organisationDetailsForUser = usersGroupService.getOrganisationDetailsForUser(addCourtDocumentEnvelope).payload();

        final Stream<Object> events = courtDocumentAggregate.addCourtDocumentV2(enrichedCourtDocument, documentTypeAccess.getActionRequired(), documentTypeAccess.getSection()
                , addCourtDocumentEnvelope.payload(), organisationDetailsForUser);

        eventStream.append(events.map(toEnvelopeWithMetadataFrom(addCourtDocumentEnvelope)));
    }
}
