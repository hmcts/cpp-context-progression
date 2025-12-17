package uk.gov.moj.cpp.progression.handler;

import static java.util.UUID.randomUUID;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.core.annotation.Component.COMMAND_HANDLER;
import static uk.gov.justice.services.messaging.Envelope.envelopeFrom;
import static uk.gov.justice.services.test.utils.core.helper.EventStreamMockHelper.verifyAppendAndGetArgumentFrom;
import static uk.gov.justice.services.test.utils.core.matchers.HandlerMatcher.isHandler;
import static uk.gov.justice.services.test.utils.core.matchers.HandlerMethodMatcher.method;

import uk.gov.justice.core.courts.ApplicationDocument;
import uk.gov.justice.core.courts.CourtDocument;
import uk.gov.justice.core.courts.CourtsDocumentAdded;
import uk.gov.justice.core.courts.CreateCourtDocument;
import uk.gov.justice.core.courts.DefendantDocument;
import uk.gov.justice.core.courts.DocumentCategory;
import uk.gov.justice.core.courts.Material;
import uk.gov.justice.services.core.aggregate.AggregateService;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.eventsourcing.source.core.EventSource;
import uk.gov.justice.services.eventsourcing.source.core.EventStream;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.Metadata;
import uk.gov.justice.services.test.utils.core.enveloper.EnveloperFactory;
import uk.gov.moj.cpp.progression.aggregate.CaseAggregate;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class UpdateFinancialMeansDataHandlerTest {

    @Mock
    private EventSource eventSource;

    @Mock
    private EventStream eventStream;

    @Mock
    private AggregateService aggregateService;

    @Spy
    private final Enveloper enveloper = EnveloperFactory.createEnveloperWithEvents(
            CourtsDocumentAdded.class);


    @InjectMocks
    private UpdateFinancialMeansDataHandler updateFinancialMeansDataHandler;


    private static final UUID CASE_ID = UUID.fromString("5002d600-af66-11e8-b568-0800200c9a77");

    private static final UUID DEFENDANT_ID = UUID.fromString("5002d600-af66-11e8-b568-0800200c9a88");

    private static final UUID MATERIAL_ID = UUID.fromString("5002d600-af66-11e8-b568-0800200c9a99");

    @Test
    public void shouldHandleCommand() {
        assertThat(new UpdateFinancialMeansDataHandler(), isHandler(COMMAND_HANDLER)
                .with(method("handle")
                        .thatHandles("progression.command.update-financial-means-data")
                ));
    }

    @Test
    public void shouldProcessCommand_DefendantDocument() throws Exception {
        List<UUID> defendantlist = new ArrayList<>();
        defendantlist.add(DEFENDANT_ID);

        List<Material> materials = new ArrayList<>();
        Material material = Material.material().withId(MATERIAL_ID).withName("MC100").build();
        materials.add(material);

        final CourtDocument courtDocument = CourtDocument.courtDocument()
                .withContainsFinancialMeans(true)
                .withMaterials(materials)
                .withDocumentCategory(DocumentCategory.documentCategory()
                        .withDefendantDocument(DefendantDocument.defendantDocument().withDefendants(defendantlist).withProsecutionCaseId(CASE_ID).build())
                        .build()
                ).build();

        final CreateCourtDocument createCourtDocument = CreateCourtDocument.createCourtDocument()
                .withCourtDocument(courtDocument)
                .build();

        final Metadata metadata = Envelope
                .metadataBuilder()
                .withName("progression.command.update-financial-means-data")
                .withId(randomUUID())
                .build();

        final Envelope<CreateCourtDocument> envelope = envelopeFrom(metadata, createCourtDocument);
        final CaseAggregate caseAggregate = new CaseAggregate();
        when(eventSource.getStreamById(any())).thenReturn(eventStream);
        when(aggregateService.get(eventStream, CaseAggregate.class)).thenReturn(caseAggregate);
        updateFinancialMeansDataHandler.handle(envelope);

        verifyAppendAndGetArgumentFrom(eventStream);

    }

    @Test
    public void shouldProcessCommand_ApplicationDocument() throws Exception {
        List<UUID> defendantlist = new ArrayList<>();
        defendantlist.add(DEFENDANT_ID);

        List<Material> materials = new ArrayList<>();
        Material material = Material.material().withId(MATERIAL_ID).withName("MC100").build();
        materials.add(material);

        final CourtDocument courtDocument = CourtDocument.courtDocument()
                .withContainsFinancialMeans(true)
                .withMaterials(materials)
                .withDocumentCategory(DocumentCategory.documentCategory()
                        .withApplicationDocument(ApplicationDocument.applicationDocument().withApplicationId(randomUUID()).build())
                        .build()
                ).build();

        final CreateCourtDocument createCourtDocument = CreateCourtDocument.createCourtDocument()
                .withCourtDocument(courtDocument)
                .build();

        final Metadata metadata = Envelope
                .metadataBuilder()
                .withName("progression.command.update-financial-means-data")
                .withId(randomUUID())
                .build();

        final Envelope<CreateCourtDocument> envelope = envelopeFrom(metadata, createCourtDocument);

        final CaseAggregate caseAggregate = new CaseAggregate();
        when(eventSource.getStreamById(any())).thenReturn(eventStream);
        when(aggregateService.get(eventStream, CaseAggregate.class)).thenReturn(caseAggregate);

        updateFinancialMeansDataHandler.handle(envelope);

        verifyAppendAndGetArgumentFrom(eventStream);
    }
}
