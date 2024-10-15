package uk.gov.moj.cpp.progression.handler;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.UUID.randomUUID;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.core.annotation.Component.COMMAND_HANDLER;
import static uk.gov.justice.services.messaging.Envelope.envelopeFrom;
import static uk.gov.justice.services.test.utils.core.matchers.HandlerMatcher.isHandler;
import static uk.gov.justice.services.test.utils.core.matchers.HandlerMethodMatcher.method;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeMatcher.jsonEnvelope;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeMetadataMatcher.metadata;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeStreamMatcher.streamContaining;

import org.junit.jupiter.api.BeforeEach;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import uk.gov.justice.core.courts.CaseLinkedToHearing;
import uk.gov.justice.core.courts.CourtReferral;
import uk.gov.justice.core.courts.LinkCaseToHearing;
import uk.gov.justice.core.courts.ProsecutionCase;
import uk.gov.justice.core.courts.ProsecutionCaseIdentifier;
import uk.gov.justice.services.core.aggregate.AggregateService;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.eventsourcing.source.core.EventSource;
import uk.gov.justice.services.eventsourcing.source.core.EventStream;
import uk.gov.justice.services.eventsourcing.source.core.exception.EventStreamException;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.messaging.Metadata;
import uk.gov.justice.services.test.utils.core.enveloper.EnveloperFactory;
import uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopePayloadMatcher;
import uk.gov.moj.cpp.progression.aggregate.CaseAggregate;
import uk.gov.moj.cpp.progression.aggregate.GroupCaseAggregate;
import java.util.UUID;
import java.util.stream.Stream;


import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class LinkCaseToHearingHandlerTest {

    @Mock
    private EventSource eventSource;

    @Mock
    private EventStream eventStream;

    @Mock
    private AggregateService aggregateService;

    @Spy
    private final Enveloper enveloper = EnveloperFactory.createEnveloperWithEvents(
            CaseLinkedToHearing.class);

    @InjectMocks
    private LinkCaseToHearingHandler linkCaseToHearingHandler;


    private CaseAggregate caseAggregate;
    private GroupCaseAggregate groupCaseAggregate;

    private final UUID HEARING_ID = randomUUID();
    private final UUID CASE_ID = randomUUID();
    private final UUID MEMBER_CASE_ID = randomUUID();
    private final UUID GROUP_ID = randomUUID();

    @BeforeEach
    public void setup() {
        caseAggregate = new CaseAggregate();
        groupCaseAggregate = new GroupCaseAggregate();
    }

    @Test
    public void shouldHandleCommand() {
        assertThat(new LinkCaseToHearingHandler(), isHandler(COMMAND_HANDLER)
                .with(method("handle")
                        .thatHandles("progression.command-link-prosecution-cases-to-hearing")
                ));
    }

    @Test
    public void shouldProcessCommandWithCaseAndHearingId() throws Exception {
        when(eventSource.getStreamById(any())).thenReturn(eventStream);
        when(aggregateService.get(eventStream, CaseAggregate.class)).thenReturn(caseAggregate);

        caseAggregate.createProsecutionCase(createProsecutionCase());

        LinkCaseToHearing linkCaseToHearing = LinkCaseToHearing.linkCaseToHearing().withCaseId(CASE_ID).withHearingId(HEARING_ID).build();
        final Metadata metadata = Envelope
                .metadataBuilder()
                .withName("progression.command-link-prosecution-cases-to-hearing")
                .withId(randomUUID())
                .build();

        final Envelope<LinkCaseToHearing> envelope = envelopeFrom(metadata, linkCaseToHearing);

        linkCaseToHearingHandler.handle(envelope);

        final Stream<JsonEnvelope> envelopeStream = verifyAppendAndGetArgumentFrom(eventStream, 1);

        assertThat(envelopeStream, streamContaining(
                jsonEnvelope(
                        metadata()
                                .withName("progression.event-link-prosecution-cases-to-hearing"),
                        JsonEnvelopePayloadMatcher.payload().isJson(allOf(
                                withJsonPath("$.hearingId", is(HEARING_ID.toString())),
                                withJsonPath("$.caseId", is(CASE_ID.toString()))
                                )
                        ))

                )

        );
    }


    @Test
    public void shouldProcessCommandWithCaseAndHearingIdForGroupCases() throws Exception {
        when(eventSource.getStreamById(any())).thenReturn(eventStream);
        when(aggregateService.get(eventStream, CaseAggregate.class)).thenReturn(caseAggregate);
        when(aggregateService.get(eventStream, GroupCaseAggregate.class)).thenReturn(groupCaseAggregate);
        caseAggregate.createProsecutionCase(createProsecutionCaseForGroupCases());
        groupCaseAggregate.initiateCourtProceedings(createCourtReferral());
        LinkCaseToHearing linkCaseToHearing = LinkCaseToHearing.linkCaseToHearing().withCaseId(CASE_ID).withHearingId(HEARING_ID).build();
        final Metadata metadata = Envelope
                .metadataBuilder()
                .withName("progression.command-link-prosecution-cases-to-hearing")
                .withId(randomUUID())
                .build();

        final Envelope<LinkCaseToHearing> envelope = envelopeFrom(metadata, linkCaseToHearing);

        linkCaseToHearingHandler.handle(envelope);

        final Stream<JsonEnvelope> envelopeStream = verifyAppendAndGetArgumentFrom(eventStream, 2);

        assertThat(envelopeStream, streamContaining(
                jsonEnvelope(
                        metadata()
                                .withName("progression.event-link-prosecution-cases-to-hearing"),
                        JsonEnvelopePayloadMatcher.payload().isJson(allOf(
                                withJsonPath("$.hearingId", is(HEARING_ID.toString())),
                                withJsonPath("$.caseId", is(CASE_ID.toString()))
                                )
                        )),
                jsonEnvelope(
                        metadata()
                                .withName("progression.event-link-prosecution-cases-to-hearing"),
                        JsonEnvelopePayloadMatcher.payload().isJson(allOf(
                                withJsonPath("$.hearingId", is(HEARING_ID.toString())),
                                withJsonPath("$.caseId", is(MEMBER_CASE_ID.toString()))
                                )
                        ))

                )

        );
    }

    private Stream verifyAppendAndGetArgumentFrom(final EventStream eventStream, final int times) throws EventStreamException {
        ArgumentCaptor<Stream> argumentCaptor = ArgumentCaptor.forClass(Stream.class);
        Mockito.verify(eventStream, times(times)).append(argumentCaptor.capture());
        return argumentCaptor.getAllValues().stream().flatMap(s -> s);
    }

    private ProsecutionCase createProsecutionCase(){
        return ProsecutionCase.prosecutionCase()
                .withProsecutionCaseIdentifier(ProsecutionCaseIdentifier.prosecutionCaseIdentifier()
                        .withCaseURN("CaseURN")
                        .build())
                .withDefendants(emptyList())
                .build();
    }

    private ProsecutionCase createProsecutionCaseForGroupCases(){
        return ProsecutionCase.prosecutionCase()
                .withGroupId(GROUP_ID)
                .withProsecutionCaseIdentifier(ProsecutionCaseIdentifier.prosecutionCaseIdentifier()
                        .withCaseURN("CaseURN")
                        .build())
                .withDefendants(emptyList())
                .build();
    }

    private CourtReferral createCourtReferral(){
        return CourtReferral.courtReferral()
                .withProsecutionCases(
                        asList(
                                ProsecutionCase.prosecutionCase()
                                        .withId(CASE_ID)
                                        .withGroupId(GROUP_ID)
                                        .withIsGroupMaster(true)
                                        .withIsGroupMember(true)
                                        .build(),
                                ProsecutionCase.prosecutionCase()
                                        .withId(MEMBER_CASE_ID)
                                        .withGroupId(GROUP_ID)
                                        .withIsGroupMaster(false)
                                        .withIsGroupMember(true)
                                        .build()
                                ))
                .build();
    }


}
