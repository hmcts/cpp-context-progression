package uk.gov.moj.cpp.progression.command;

import static java.util.UUID.randomUUID;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.when;
import static uk.gov.justice.core.courts.ShareCourtDocument.shareCourtDocument;
import static uk.gov.justice.core.courts.SharedCourtDocument.sharedCourtDocument;
import static uk.gov.justice.services.core.annotation.Component.COMMAND_HANDLER;
import static uk.gov.justice.services.messaging.Envelope.envelopeFrom;
import static uk.gov.justice.services.test.utils.core.helper.EventStreamMockHelper.verifyAppendAndGetArgumentFrom;
import static uk.gov.justice.services.test.utils.core.matchers.HandlerMatcher.isHandler;
import static uk.gov.justice.services.test.utils.core.matchers.HandlerMethodMatcher.method;
import static uk.gov.moj.cpp.progression.command.helper.HandlerTestHelper.metadataFor;

import uk.gov.justice.core.courts.CaseDocument;
import uk.gov.justice.core.courts.CourtDocument;
import uk.gov.justice.core.courts.DocumentCategory;
import uk.gov.justice.core.courts.ShareCourtDocument;
import uk.gov.justice.services.core.aggregate.AggregateService;
import uk.gov.justice.services.eventsourcing.source.core.EventSource;
import uk.gov.justice.services.eventsourcing.source.core.EventStream;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.moj.cpp.progression.aggregate.CourtDocumentAggregate;
import uk.gov.moj.cpp.progression.handler.ShareCourtDocumentHandler;

import java.util.UUID;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class ShareCourtDocumentHandlerTest {

    @Mock
    private EventSource eventSource;

    @Mock
    private EventStream eventStream;

    @Mock
    private AggregateService aggregateService;

    @InjectMocks
    private ShareCourtDocumentHandler shareCourtDocumentHandler;

    private CourtDocumentAggregate aggregate;


    @Before
    public void setup() {
        aggregate = new CourtDocumentAggregate();
        final UUID caseId = randomUUID();
        CourtDocument courtDocument = CourtDocument.courtDocument()
                .withDocumentCategory(DocumentCategory.documentCategory()
                        .withCaseDocument(CaseDocument.caseDocument().withProsecutionCaseId(caseId).build()).build())
                .withSendToCps(false)
                .build();
        this.aggregate.createCourtDocument(courtDocument, true);
        when(eventSource.getStreamById(any())).thenReturn(eventStream);
        when(aggregateService.get(eventStream, CourtDocumentAggregate.class)).thenReturn(aggregate);
    }

    @Test
    public void shouldHandleCommand() {
        assertThat(new ShareCourtDocumentHandler(), isHandler(COMMAND_HANDLER)
                .with(method("handle")
                        .thatHandles("progression.command.share-court-document")
                ));
    }

    @Test
    public void shouldProcessShareCourtDocumentCommand() throws Exception {

        final ShareCourtDocument shareCourtDocument = shareCourtDocument()
                .withShareCourtDocumentDetails(sharedCourtDocument()
                        .withCourtDocumentId(randomUUID())
                        .withUserGroupId(randomUUID())
                        .build())
                .build();

        final Envelope<ShareCourtDocument> envelope =
                envelopeFrom(metadataFor("progression.command.share-court-document", randomUUID()),
                        shareCourtDocument);

        shareCourtDocumentHandler.handle(envelope);

        verifyAppendAndGetArgumentFrom(eventStream);

    }
}
