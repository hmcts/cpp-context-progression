package uk.gov.moj.cpp.progression.command;


import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static java.util.UUID.randomUUID;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static uk.gov.justice.core.courts.HearingListingStatus.SENT_FOR_LISTING;
import static uk.gov.justice.services.core.annotation.Component.COMMAND_HANDLER;
import static uk.gov.justice.services.messaging.Envelope.envelopeFrom;
import static uk.gov.justice.services.test.utils.core.helper.EventStreamMockHelper.verifyAppendAndGetArgumentFrom;
import static uk.gov.justice.services.test.utils.core.matchers.HandlerMatcher.isHandler;
import static uk.gov.justice.services.test.utils.core.matchers.HandlerMethodMatcher.method;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeMatcher.jsonEnvelope;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeMetadataMatcher.metadata;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopePayloadMatcher.payload;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeStreamMatcher.streamContaining;

import uk.gov.justice.core.courts.Hearing;
import uk.gov.justice.core.courts.HearingType;
import uk.gov.justice.core.courts.ProsecutionCase;
import uk.gov.justice.core.courts.ProsecutionCaseDefendantListingStatusChanged;
import uk.gov.justice.core.courts.ProsecutionCaseDefendantListingStatusChangedV2;
import uk.gov.justice.core.courts.UpdateDefendantListingStatusV2;
import uk.gov.justice.services.core.aggregate.AggregateService;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.eventsourcing.source.core.EventSource;
import uk.gov.justice.services.eventsourcing.source.core.EventStream;
import uk.gov.justice.services.eventsourcing.source.core.exception.EventStreamException;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.messaging.Metadata;
import uk.gov.justice.services.test.utils.core.enveloper.EnveloperFactory;
import uk.gov.moj.cpp.progression.aggregate.CaseAggregate;
import uk.gov.moj.cpp.progression.aggregate.GroupCaseAggregate;
import uk.gov.moj.cpp.progression.aggregate.HearingAggregate;
import uk.gov.moj.cpp.progression.handler.UpdateDefendantListingStatusHandler;

import java.util.Arrays;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;


import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class UpdateDefendantListingStatusHandlerTest {

    private static final UUID MASTER_CASE_ID = randomUUID();

    private static final UUID MEMBER_CASE_ID = randomUUID();

    private static final UUID GROUP_ID = randomUUID();

    private static final UUID HEARING_ID = randomUUID();

    private static final String COMMAND = "progression.command.update-defendant-listing-status";

    private static final String HEARING_TYPE = "origHearingType";


    @InjectMocks
    private UpdateDefendantListingStatusHandler handler;

    @Mock
    private EventSource eventSource;

    @Mock
    private EventStream eventStream;

    @Mock
    private AggregateService aggregateService;

    @Mock
    private HearingAggregate hearingAggregate;

    @Mock
    private GroupCaseAggregate groupCaseAggregate;

    @Mock
    private CaseAggregate caseAggregate;

    private Set<UUID> memberCases;

    @Spy
    private final Enveloper enveloper = EnveloperFactory.createEnveloperWithEvents(ProsecutionCaseDefendantListingStatusChanged.class,
            ProsecutionCaseDefendantListingStatusChangedV2.class);

    @BeforeEach
    public void setup() {
        hearingAggregate = new HearingAggregate();
        memberCases = Stream.generate(UUID::randomUUID).limit(1).collect(Collectors.toSet());
        memberCases.add(MASTER_CASE_ID);
    }

    @Test
    public void shouldHandleCommand() {
        assertThat(new UpdateDefendantListingStatusHandler(), isHandler(COMMAND_HANDLER)
                .with(method("handle")
                        .thatHandles(COMMAND)
                ));
    }

    @Test
    public void shouldHandleProcessUpdateDefendantListingStatus() throws EventStreamException {
        final Metadata metadata = Envelope
                .metadataBuilder()
                .withName(COMMAND)
                .withId(randomUUID())
                .build();
        final UpdateDefendantListingStatusV2 updateDefendantListingStatus = getUpdateDefendantListingStatus();
        final Envelope<UpdateDefendantListingStatusV2> envelope = envelopeFrom(metadata, updateDefendantListingStatus);
        when(eventSource.getStreamById(any())).thenReturn(eventStream);
        when(aggregateService.get(eventStream, HearingAggregate.class)).thenReturn(hearingAggregate);
        handler.handle(envelope);

        final Stream<JsonEnvelope> envelopeStream = verifyAppendAndGetArgumentFrom(eventStream);

        assertThat(envelopeStream, streamContaining(
                jsonEnvelope(
                        metadata()
                                .withName("progression.event.prosecutionCase-defendant-listing-status-changed-v2"),
                        payload().isJson(allOf(
                                withJsonPath("$.hearing.id", is(HEARING_ID.toString())),
                                withJsonPath("$.hearing.isGroupProceedings", is(false)),
                                withJsonPath("$.hearing.prosecutionCases[*]", hasSize(1)),
                                withJsonPath("$.hearingListingStatus", is(SENT_FOR_LISTING.toString())),
                                withJsonPath("$.hearing.type.description", is(HEARING_TYPE)),
                                withJsonPath("$.notifyNCES", is(false))
                        ))
                )
        ));
    }

    @Test
    public void shouldHandleProcessUpdateDefendantListingStatusForGroupCases() throws EventStreamException {
        final Metadata metadata = Envelope
                .metadataBuilder()
                .withName(COMMAND)
                .withId(randomUUID())
                .build();
        final UpdateDefendantListingStatusV2 updateDefendantListingStatus = getUpdateDefendantListingStatusWithGroupMasterCase();
        final Envelope<UpdateDefendantListingStatusV2> envelope = envelopeFrom(metadata, updateDefendantListingStatus);
        when(eventSource.getStreamById(any())).thenReturn(eventStream);
        when(groupCaseAggregate.getMemberCases()).thenReturn(memberCases);
        when(aggregateService.get(eventStream, HearingAggregate.class)).thenReturn(hearingAggregate);
        when(aggregateService.get(eventStream, GroupCaseAggregate.class)).thenReturn(groupCaseAggregate);
        when(aggregateService.get(eventStream, CaseAggregate.class)).thenReturn(caseAggregate);
        when(caseAggregate.getProsecutionCase()).thenReturn(ProsecutionCase.prosecutionCase()
                .withId(MEMBER_CASE_ID)
                .withIsGroupMaster(false)
                .withGroupId(GROUP_ID).build());
        handler.handle(envelope);

        final Stream<JsonEnvelope> envelopeStream = verifyAppendAndGetArgumentFrom(eventStream);

        assertThat(envelopeStream, streamContaining(
                jsonEnvelope(
                        metadata()
                                .withName("progression.event.prosecutionCase-defendant-listing-status-changed-v2"),
                        payload().isJson(allOf(
                                withJsonPath("$.hearing.id", is(HEARING_ID.toString())),
                                withJsonPath("$.hearing.isGroupProceedings", is(true)),
                                withJsonPath("$.hearing.prosecutionCases[0].isGroupMaster", is(true)),
                                withJsonPath("$.hearing.prosecutionCases[0].id", is(MASTER_CASE_ID.toString())),
                                withJsonPath("$.hearing.prosecutionCases[1].isGroupMaster", is(false)),
                                withJsonPath("$.hearing.prosecutionCases[1].id", is(MEMBER_CASE_ID.toString())),
                                withJsonPath("$.hearingListingStatus", is(SENT_FOR_LISTING.toString())),
                                withJsonPath("$.hearing.type.description", is(HEARING_TYPE)),
                                withJsonPath("$.notifyNCES", is(false))
                        ))
                )
        ));
    }

    private UpdateDefendantListingStatusV2 getUpdateDefendantListingStatusWithGroupMasterCase() {
        return UpdateDefendantListingStatusV2.updateDefendantListingStatusV2()
                .withHearing(Hearing.hearing()
                        .withId(HEARING_ID).withIsGroupProceedings(true)
                        .withType(HearingType.hearingType().withDescription(HEARING_TYPE).build())
                        .withProsecutionCases(Arrays.asList(ProsecutionCase.prosecutionCase()
                                .withId(MASTER_CASE_ID)
                                .withIsGroupMaster(true)
                                .withGroupId(GROUP_ID).build())
                        ).build())
                .withHearingListingStatus(SENT_FOR_LISTING)
                .withNotifyNCES(false)
                .build();
    }

    private UpdateDefendantListingStatusV2 getUpdateDefendantListingStatus() {
        return UpdateDefendantListingStatusV2.updateDefendantListingStatusV2()
                .withHearing(Hearing.hearing()
                        .withId(HEARING_ID).withIsGroupProceedings(false)
                        .withType(HearingType.hearingType().withDescription(HEARING_TYPE).build())
                        .withProsecutionCases(Arrays.asList(ProsecutionCase.prosecutionCase().withId(MASTER_CASE_ID)
                               .build())
                        ).build())
                .withHearingListingStatus(SENT_FOR_LISTING)
                .withNotifyNCES(false)
                .build();
    }

    @Test
    public void shouldHandleV3Command() {
        assertThat(new UpdateDefendantListingStatusHandler(), isHandler(COMMAND_HANDLER)
                .with(method("handleV3")
                        .thatHandles("progression.command.update-defendant-listing-status-v3")
                ));
    }

}
