package uk.gov.moj.cpp.progression.domain.transformation.deactivestream;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.tools.eventsourcing.transformation.api.Action;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
@ExtendWith(MockitoExtension.class)
import org.junit.runners.Parameterized;


@RunWith(Parameterized.class)
public class EventStreamTransformTest {

    private final String streamId;
    private final Action expectedAction;

    public EventStreamTransformTest(final String streamId, final boolean expectedAction) {
        this.streamId = streamId;
        this.expectedAction = expectedAction ? Action.DEACTIVATE : Action.NO_ACTION;
    }

    @Parameterized.Parameters
    public static Collection<Object[]> data() {
        return Arrays.asList(new Object[][]{
                {"41a15ddf-c913-45e4-86dc-40d3c659b36e", true},
                {"eb7231f3-9cb5-4baa-9e9c-5fad78ad3a92", true},
                {"36ac57aa-01e5-463f-ac6a-51276fcfa90b", true},
                {"be2e2c78-66e5-42c6-b641-1d8e0f13b48d", false},
                {"1ec7adb4-cf0e-45c3-9654-d1ebbdfd43c4", true},
                {"39fe39ea-a823-41fd-bb0b-0c63d4069343", false},

        });
    }

    @Test
    public void testActionFor() throws IOException {
        final JsonEnvelope jsonEnvelope = mock(JsonEnvelope.class, RETURNS_DEEP_STUBS);
        when(jsonEnvelope.metadata().streamId()).thenReturn(Optional.of(UUID.fromString(streamId)));

        final Action action = new EventStreamTransform("test-data-1.csv", "test-data-2.csv").actionFor(jsonEnvelope);

        assertThat(action, is(expectedAction));
    }


}