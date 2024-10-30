package uk.gov.moj.cpp.progression.handler;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static javax.json.Json.createObjectBuilder;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.core.annotation.Component.COMMAND_HANDLER;
import static uk.gov.justice.services.test.utils.core.helper.EventStreamMockHelper.verifyAppendAndGetArgumentFrom;
import static uk.gov.justice.services.test.utils.core.matchers.HandlerMatcher.isHandler;
import static uk.gov.justice.services.test.utils.core.matchers.HandlerMethodMatcher.method;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeMatcher.jsonEnvelope;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeMetadataMatcher.metadata;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeStreamMatcher.streamContaining;
import static uk.gov.justice.services.test.utils.core.messaging.MetadataBuilderFactory.metadataWithRandomUUID;

import uk.gov.justice.core.courts.AddCaseNote;
import uk.gov.justice.core.courts.CaseNoteAddedV2;
import uk.gov.justice.services.core.aggregate.AggregateService;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.eventsourcing.source.core.EventSource;
import uk.gov.justice.services.eventsourcing.source.core.EventStream;
import uk.gov.justice.services.eventsourcing.source.core.exception.EventStreamException;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.test.utils.core.enveloper.EnveloperFactory;
import uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopePayloadMatcher;
import uk.gov.moj.cpp.progression.aggregate.CaseAggregate;
import uk.gov.moj.cpp.progression.command.handler.service.UsersGroupService;
import uk.gov.moj.cpp.progression.command.handler.service.payloads.UserDetails;

import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class AddCaseNoteHandlerTest {

    private static final UUID CASE_ID = UUID.randomUUID();
    public static final UUID USER_ID = UUID.randomUUID();

    @Mock
    private EventSource eventSource;

    @Mock
    private EventStream eventStream;

    @Mock
    private UsersGroupService usersGroupService;

    @Mock
    private AggregateService aggregateService;

    @InjectMocks
    private AddCaseNoteHandler addCaseNoteHandler;

    @Spy
    private final Enveloper enveloper = EnveloperFactory.createEnveloperWithEvents(
            CaseNoteAddedV2.class);

    @Test
    public void shouldHandleCommand() {
        assertThat(addCaseNoteHandler, isHandler(COMMAND_HANDLER)
                .with(method("handle")
                        .thatHandles("progression.command.add-case-note")));
    }

    @Test
    public void shouldProcessAddCaseNoteWithCaseId() throws Exception {

        final CaseAggregate caseAggregate = new CaseAggregate();
        when(eventSource.getStreamById(any())).thenReturn(eventStream);
        when(aggregateService.get(eventStream, CaseAggregate.class)).thenReturn(caseAggregate);
        when(usersGroupService.getUserDetails(any())).thenReturn(Optional.of(new UserDetails("Bob", "Marley")));

        final Envelope<AddCaseNote> envelope = createAddCaseNoteHandlerEnvelope();

        addCaseNoteHandler.handle(envelope);

        verifyAddCaseNoteHandlerResults();
    }

    @Test
    public void shouldProcessAddCaseNoteWithCaseIdAndIsPinned() throws Exception {

        final Envelope<AddCaseNote> envelope = createAddCaseNoteWithIsPinnedHandlerEnvelope();
        final CaseAggregate caseAggregate = new CaseAggregate();
        when(eventSource.getStreamById(any())).thenReturn(eventStream);
        when(aggregateService.get(eventStream, CaseAggregate.class)).thenReturn(caseAggregate);
        when(usersGroupService.getUserDetails(any())).thenReturn(Optional.of(new UserDetails("Bob", "Marley")));

        addCaseNoteHandler.handle(envelope);

        verifyAddCaseNoteWithIsPinnedHandlerResults();
    }

    private void verifyAddCaseNoteHandlerResults() throws EventStreamException {
        final Stream<JsonEnvelope> envelopeStream = verifyAppendAndGetArgumentFrom(eventStream);

        assertThat(envelopeStream, streamContaining(
                jsonEnvelope(
                        metadata()
                                .withName("progression.event.case-note-added-v2"),
                        JsonEnvelopePayloadMatcher.payload().isJson(allOf(
                                withJsonPath("$.caseId", is(CASE_ID.toString())),
                                withJsonPath("$.note", is("Test Note Added")),
                                withJsonPath("$.isPinned", is(false))
                                )
                        ))

                )
        );
    }

    private void verifyAddCaseNoteWithIsPinnedHandlerResults() throws EventStreamException {
        final Stream<JsonEnvelope> envelopeStream = verifyAppendAndGetArgumentFrom(eventStream);

        assertThat(envelopeStream, streamContaining(
                jsonEnvelope(
                        metadata()
                                .withName("progression.event.case-note-added-v2"),
                        JsonEnvelopePayloadMatcher.payload().isJson(allOf(
                                withJsonPath("$.caseId", is(CASE_ID.toString())),
                                withJsonPath("$.note", is("Test Note Added")),
                                withJsonPath("$.isPinned", is(true))
                                )
                        ))

                )
        );
    }

    private Envelope<AddCaseNote> createAddCaseNoteHandlerEnvelope() {
        AddCaseNote addCaseNote = AddCaseNote.addCaseNote().withCaseId(CASE_ID).withNote("Test Note Added").build();

        final JsonEnvelope requestEnvelope = JsonEnvelope.envelopeFrom(
                metadataWithRandomUUID("usersgroups.get-user-details").withUserId(USER_ID.toString()),
                createObjectBuilder().build());


        return Enveloper.envelop(addCaseNote)
                .withName("usersgroups.get-user-details")
                .withMetadataFrom(requestEnvelope);
    }
  private Envelope<AddCaseNote> createAddCaseNoteWithIsPinnedHandlerEnvelope() {
        AddCaseNote addCaseNote = AddCaseNote.addCaseNote().withCaseId(CASE_ID).withNote("Test Note Added").withIsPinned(true).build();

        final JsonEnvelope requestEnvelope = JsonEnvelope.envelopeFrom(
                metadataWithRandomUUID("usersgroups.get-user-details").withUserId(USER_ID.toString()),
                createObjectBuilder().build());


        return Enveloper.envelop(addCaseNote)
                .withName("usersgroups.get-user-details")
                .withMetadataFrom(requestEnvelope);
    }

}
