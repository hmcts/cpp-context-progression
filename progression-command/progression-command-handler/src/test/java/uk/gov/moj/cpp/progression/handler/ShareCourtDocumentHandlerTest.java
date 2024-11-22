package uk.gov.moj.cpp.progression.handler;

import static java.util.UUID.randomUUID;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.ArgumentMatchers.any;
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

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class ShareCourtDocumentHandlerTest {

    @Mock
    private EventSource eventSource;

    @Mock
    private EventStream eventStream;

    @Mock
    private AggregateService aggregateService;

    @InjectMocks
    private ShareCourtDocumentHandler shareCourtDocumentHandler;


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

        final CourtDocumentAggregate courtDocumentAggregate = new CourtDocumentAggregate();
        final UUID caseId = randomUUID();
        CourtDocument courtDocument = CourtDocument.courtDocument()
                .withDocumentCategory(DocumentCategory.documentCategory()
                        .withCaseDocument(CaseDocument.caseDocument().withProsecutionCaseId(caseId).build()).build())
                .withSendToCps(false)
                .build();
        courtDocumentAggregate.createCourtDocument(courtDocument, true);
        when(eventSource.getStreamById(any())).thenReturn(eventStream);
        when(aggregateService.get(eventStream, CourtDocumentAggregate.class)).thenReturn(courtDocumentAggregate);

        shareCourtDocumentHandler.handle(envelope);

        verifyAppendAndGetArgumentFrom(eventStream);
    }
}
