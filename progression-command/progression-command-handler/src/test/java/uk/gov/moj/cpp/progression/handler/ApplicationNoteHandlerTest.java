package uk.gov.moj.cpp.progression.handler;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static java.lang.Boolean.FALSE;
import static java.lang.Boolean.TRUE;
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

import uk.gov.justice.core.courts.ApplicationNoteAdded;
import uk.gov.justice.core.courts.ApplicationNoteEdited;
import uk.gov.justice.services.core.aggregate.AggregateService;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.eventsourcing.source.core.EventSource;
import uk.gov.justice.services.eventsourcing.source.core.EventStream;
import uk.gov.justice.services.eventsourcing.source.core.exception.EventStreamException;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.test.utils.core.enveloper.EnveloperFactory;
import uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopePayloadMatcher;
import uk.gov.moj.cpp.progression.aggregate.ApplicationAggregate;
import uk.gov.moj.cpp.progression.command.handler.AddApplicationNote;
import uk.gov.moj.cpp.progression.command.handler.EditApplicationNote;
import uk.gov.moj.cpp.progression.command.handler.service.UsersGroupService;
import uk.gov.moj.cpp.progression.command.handler.service.payloads.UserDetails;

import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class ApplicationNoteHandlerTest {

    private static final String PROGRESSION_COMMAND_ADD_APPLICATION_NOTE = "progression.command.add-application-note";
    private static final String PROGRESSION_COMMAND_EDIT_APPLICATION_NOTE = "progression.command.edit-application-note";
    private static final String PROGRESSION_COMMAND_HANDLER_ADD_APPLICATION_NOTE = "progression.command.handler.add-application-note";
    private static final String PROGRESSION_COMMAND_HANDLER_EDIT_APPLICATION_NOTE = "progression.command.handler.edit-application-note";
    private static final String PROGRESSION_EVENT_APPLICATION_NOTE_ADDED = "progression.event.application-note-added";
    private static final String PROGRESSION_EVENT_APPLICATION_NOTE_EDITED = "progression.event.application-note-edited";
    private static final String VALUE_NOTE = "Just an application note text";
    private static final UUID VALUE_APPLICATION_ID = UUID.randomUUID();
    private static final UUID VALUE_APPLICATION_NOTE_ID = UUID.randomUUID();
    public static final UUID VALUE_USER_ID = UUID.randomUUID();

    @Mock
    private EventSource eventSource;

    @Mock
    private EventStream eventStream;

    @Mock
    private ApplicationAggregate aggregate;

    @Mock
    private UsersGroupService usersGroupService;

    @Mock
    private AggregateService aggregateService;

    @InjectMocks
    private ApplicationNoteHandler applicationNoteHandler;

    @Spy
    private final Enveloper enveloperNoteAdded = EnveloperFactory.createEnveloperWithEvents(ApplicationNoteAdded.class);

    @Spy
    private final Enveloper enveloperNoteEdited = EnveloperFactory.createEnveloperWithEvents(ApplicationNoteEdited.class);

    private void mockAggregate() {
        aggregate = new ApplicationAggregate();
        when(eventSource.getStreamById(any())).thenReturn(eventStream);
        when(aggregateService.get(eventStream, ApplicationAggregate.class)).thenReturn(aggregate);
    }

    private void mockUsersGroups() {
        when(usersGroupService.getUserDetails(any())).thenReturn(Optional.of(new UserDetails("Bob", "Marley")));
    }

    @Test
    public void shouldHandleAddNoteCommand() {
        assertThat(applicationNoteHandler, isHandler(COMMAND_HANDLER)
                .with(method("handleAddApplicationNote")
                        .thatHandles(PROGRESSION_COMMAND_HANDLER_ADD_APPLICATION_NOTE)));
    }

    @Test
    public void shouldHandleEditNoteCommand() {
        assertThat(applicationNoteHandler, isHandler(COMMAND_HANDLER)
                .with(method("handleEditApplicationNote")
                        .thatHandles(PROGRESSION_COMMAND_HANDLER_EDIT_APPLICATION_NOTE)));
    }

    @Test
    public void shouldProcessAddApplicationNoteWithApplicationId() throws Exception {
        //given
        mockAggregate();
        mockUsersGroups();

        //when
        applicationNoteHandler.handleAddApplicationNote(createAddApplicationNoteHandlerEnvelope());

        //then
        verifyAddApplicationNoteHandlerResults();
    }

    @Test
    public void shouldProcessAddApplicationNoteWithApplicationIdAndIsPinned() throws Exception {
        //given
        mockAggregate();
        mockUsersGroups();

        //when
        applicationNoteHandler.handleAddApplicationNote(createAddApplicationNoteWithIsPinnedHandlerEnvelope());

        //then
        verifyAddApplicationNoteWithIsPinnedHandlerResults();
    }

    @Test
    public void shouldHandleEditApplicationNote() throws EventStreamException {
        //given
        mockAggregate();

        //when
        applicationNoteHandler.handleEditApplicationNote(createEditApplicationNoteHandlerEnvelope());

        //then
        verifyEditApplicationNoteHandlerResults();
    }

    private Envelope<AddApplicationNote> createAddApplicationNoteHandlerEnvelope() {
        return Envelope.envelopeFrom(
                metadataWithRandomUUID(PROGRESSION_COMMAND_ADD_APPLICATION_NOTE)
                        .withUserId(VALUE_USER_ID.toString()),
                AddApplicationNote.addApplicationNote()
                        .withApplicationId(VALUE_APPLICATION_ID)
                        .withNote(VALUE_NOTE).build());
    }

    private Envelope<AddApplicationNote> createAddApplicationNoteWithIsPinnedHandlerEnvelope() {
        return Envelope.envelopeFrom(metadataWithRandomUUID(PROGRESSION_COMMAND_ADD_APPLICATION_NOTE)
                        .withUserId(VALUE_USER_ID.toString()),
                AddApplicationNote.addApplicationNote()
                        .withApplicationId(VALUE_APPLICATION_ID)
                        .withNote(VALUE_NOTE)
                        .withIsPinned(TRUE).build());
    }

    private Envelope<EditApplicationNote> createEditApplicationNoteHandlerEnvelope() {
        return Envelope.envelopeFrom(metadataWithRandomUUID(PROGRESSION_COMMAND_EDIT_APPLICATION_NOTE)
                        .withUserId(VALUE_USER_ID.toString()),
                EditApplicationNote.editApplicationNote()
                        .withApplicationNoteId(VALUE_APPLICATION_NOTE_ID)
                        .withApplicationId(VALUE_APPLICATION_ID)
                        .withIsPinned(FALSE).build());
    }

    private void verifyAddApplicationNoteHandlerResults() throws EventStreamException {
        final Stream<JsonEnvelope> envelopeStream = verifyAppendAndGetArgumentFrom(eventStream);

        assertThat(envelopeStream, streamContaining(
                jsonEnvelope(metadata().withName(PROGRESSION_EVENT_APPLICATION_NOTE_ADDED),
                        JsonEnvelopePayloadMatcher.payload().isJson(allOf(
                                withJsonPath("$.applicationId", is(VALUE_APPLICATION_ID.toString())),
                                withJsonPath("$.note", is(VALUE_NOTE)),
                                withJsonPath("$.isPinned", is(FALSE)),
                                withJsonPath("$.firstName", is("Bob")),
                                withJsonPath("$.lastName", is("Marley"))
                        )))));
    }

    private void verifyAddApplicationNoteWithIsPinnedHandlerResults() throws EventStreamException {
        final Stream<JsonEnvelope> envelopeStream = verifyAppendAndGetArgumentFrom(eventStream);

        assertThat(envelopeStream, streamContaining(
                jsonEnvelope(metadata().withName(PROGRESSION_EVENT_APPLICATION_NOTE_ADDED),
                        JsonEnvelopePayloadMatcher.payload().isJson(allOf(
                                withJsonPath("$.applicationId", is(VALUE_APPLICATION_ID.toString())),
                                withJsonPath("$.note", is(VALUE_NOTE)),
                                withJsonPath("$.isPinned", is(TRUE))
                        )))));
    }

    private void verifyEditApplicationNoteHandlerResults() throws EventStreamException {
        final Stream<JsonEnvelope> envelopeStream = verifyAppendAndGetArgumentFrom(eventStream);

        assertThat(envelopeStream, streamContaining(
                jsonEnvelope(metadata().withName(PROGRESSION_EVENT_APPLICATION_NOTE_EDITED),
                        JsonEnvelopePayloadMatcher.payload().isJson(allOf(
                                withJsonPath("$.applicationNoteId", is(VALUE_APPLICATION_NOTE_ID.toString())),
                                withJsonPath("$.applicationId", is(VALUE_APPLICATION_ID.toString())),
                                withJsonPath("$.isPinned", is(FALSE))
                        )))));
    }
}
