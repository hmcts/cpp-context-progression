package uk.gov.moj.cpp.progression.command;

import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.core.annotation.Component.COMMAND_HANDLER;
import static uk.gov.justice.services.test.utils.core.matchers.HandlerMatcher.isHandler;
import static uk.gov.justice.services.test.utils.core.matchers.HandlerMethodMatcher.method;

import uk.gov.justice.progression.courts.LinkCases;
import uk.gov.justice.services.core.aggregate.AggregateService;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.eventsourcing.source.core.EventSource;
import uk.gov.justice.services.eventsourcing.source.core.EventStream;
import uk.gov.justice.services.test.utils.core.enveloper.EnveloperFactory;
import uk.gov.moj.cpp.progression.aggregate.CaseAggregate;
import uk.gov.moj.cpp.progression.handler.LinkSplitMergeCasesHandler;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class LinkSplitMergeCaseHandlerTest {

    @Mock
    private EventSource eventSource;

    @Mock
    private EventStream eventStream;

    @Mock
    private AggregateService aggregateService;

    @Spy
    private final Enveloper enveloper = EnveloperFactory.createEnveloperWithEvents(
            LinkCases.class);

    @InjectMocks
    private LinkSplitMergeCasesHandler linkCasesHandler;


    private CaseAggregate caseAggregate;

    @Before
    public void setup() {
        caseAggregate = new CaseAggregate();
        when(eventSource.getStreamById(any())).thenReturn(eventStream);
        when(aggregateService.get(eventStream, CaseAggregate.class)).thenReturn(caseAggregate);
    }

    @Test
    public void shouldHandleLinkCommand() {
        assertThat(linkCasesHandler, isHandler(COMMAND_HANDLER)
                .with(method("handle")
                        .thatHandles("progression.command.link-cases")
                ));
    }

    @Test
    public void shouldHandleValidateLinkCommand() {
        assertThat(linkCasesHandler, isHandler(COMMAND_HANDLER)
                .with(method("handleValidation")
                        .thatHandles("progression.command.validate-link-cases")
                ));
    }

}
