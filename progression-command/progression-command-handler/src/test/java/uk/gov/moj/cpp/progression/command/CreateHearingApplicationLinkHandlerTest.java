package uk.gov.moj.cpp.progression.command;

import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.core.annotation.Component.COMMAND_HANDLER;
import static uk.gov.justice.services.test.utils.core.matchers.HandlerMatcher.isHandler;
import static uk.gov.justice.services.test.utils.core.matchers.HandlerMethodMatcher.method;

import uk.gov.justice.core.courts.CourtApplicationCreated;
import uk.gov.justice.services.core.aggregate.AggregateService;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.eventsourcing.source.core.EventSource;
import uk.gov.justice.services.eventsourcing.source.core.EventStream;
import uk.gov.justice.services.test.utils.core.enveloper.EnveloperFactory;
import uk.gov.moj.cpp.progression.aggregate.ApplicationAggregate;
import uk.gov.moj.cpp.progression.handler.CreateHearingApplicationLinkHandler;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class CreateHearingApplicationLinkHandlerTest {

    @Mock
    private EventSource eventSource;

    @Mock
    private EventStream eventStream;

    @Mock
    private AggregateService aggregateService;

    @Spy
    private final Enveloper enveloper = EnveloperFactory.createEnveloperWithEvents(
            CourtApplicationCreated.class);

    @InjectMocks
    private CreateHearingApplicationLinkHandler createHearingApplicationLinkHandler;


    private ApplicationAggregate aggregate;

    private static final String PROGRESSION_COMMAND_CREATE_HEARING_APPLICATION_LINK
            = "progression.command.create-hearing-application-link";

    @Before
    public void setup() {
        aggregate = new ApplicationAggregate();
        when(eventSource.getStreamById(any())).thenReturn(eventStream);
        when(aggregateService.get(eventStream, ApplicationAggregate.class)).thenReturn(aggregate);
    }

    @Test
    public void shouldHandleCommand() {
        assertThat(new CreateHearingApplicationLinkHandler(), isHandler(COMMAND_HANDLER)
                .with(method("handle")
                        .thatHandles(PROGRESSION_COMMAND_CREATE_HEARING_APPLICATION_LINK)
                ));
    }

    @Test
    public void shouldhandleForAllocationFieldsCommand() {
        assertThat(new CreateHearingApplicationLinkHandler(), isHandler(COMMAND_HANDLER)
                .with(method("handleForAllocationFields")
                        .thatHandles("progression.command.update-hearing-for-allocation-fields")
                ));
    }
}
