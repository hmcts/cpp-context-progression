package uk.gov.moj.cpp.progression.handler;

import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.core.annotation.Component.COMMAND_HANDLER;
import static uk.gov.justice.services.test.utils.core.matchers.HandlerMatcher.isHandler;
import static uk.gov.justice.services.test.utils.core.matchers.HandlerMethodMatcher.method;

import uk.gov.justice.services.core.aggregate.AggregateService;
import uk.gov.justice.services.eventsourcing.source.core.EventSource;
import uk.gov.justice.services.eventsourcing.source.core.EventStream;
import uk.gov.moj.cpp.progression.aggregate.HearingAggregate;
import uk.gov.moj.cpp.progression.command.helper.FileResourceObjectMapper;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class PleaUpdatedHandlerTest {

    private static final String UPDATE_HEARING_OFFENCE_PLEA = "progression.command.update-hearing-offence-plea";

    private final FileResourceObjectMapper handlerTestHelper = new FileResourceObjectMapper();

    @Mock
    private EventSource eventSource;

    @Mock
    private EventStream eventStream;

    @Mock
    private AggregateService aggregateService;

    @Mock
    private HearingAggregate hearingAggregate;

   @InjectMocks
    private PleaUpdatedHandler pleaUpdatedHandler;

    @Before
    public void setup() {
        when(eventSource.getStreamById(any())).thenReturn(eventStream);
        when(aggregateService.get(eventStream, HearingAggregate.class)).thenReturn(hearingAggregate);
    }

    @Test
    public void shouldHandlePleaUpdatedCommand() {
        assertThat(new PleaUpdatedHandler(), isHandler(COMMAND_HANDLER)
                .with(method("handleUpdatePlea")
                        .thatHandles(UPDATE_HEARING_OFFENCE_PLEA)
                ));
    }


}
