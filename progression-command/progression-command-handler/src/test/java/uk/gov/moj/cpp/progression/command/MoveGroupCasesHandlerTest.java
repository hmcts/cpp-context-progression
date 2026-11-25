package uk.gov.moj.cpp.progression.command;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.UUID.randomUUID;
import static java.util.stream.Collectors.toList;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.mockito.quality.Strictness.LENIENT;
import static uk.gov.justice.services.core.annotation.Component.COMMAND_HANDLER;
import static uk.gov.justice.services.messaging.JsonObjects.createObjectBuilder;
import static uk.gov.justice.services.test.utils.core.matchers.HandlerMatcher.isHandler;
import static uk.gov.justice.services.test.utils.core.matchers.HandlerMethodMatcher.method;
import static uk.gov.justice.services.test.utils.core.messaging.MetadataBuilderFactory.metadataWithRandomUUID;
import static uk.gov.justice.services.test.utils.core.reflection.ReflectionUtil.setField;

import uk.gov.justice.core.courts.CaseGroupInfoUpdated;
import uk.gov.justice.core.courts.CivilFees;
import uk.gov.justice.core.courts.CivilFeesAdded;
import uk.gov.justice.core.courts.CourtReferral;
import uk.gov.justice.core.courts.FeeStatus;
import uk.gov.justice.core.courts.FeeType;
import uk.gov.justice.core.courts.ProsecutionCase;
import uk.gov.justice.core.courts.ProsecutionCaseCreated;
import uk.gov.justice.core.courts.ProsecutionCaseIdentifier;
import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.common.converter.ObjectToJsonObjectConverter;
import uk.gov.justice.services.common.converter.jackson.ObjectMapperProducer;
import uk.gov.justice.services.core.aggregate.AggregateService;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.eventsourcing.source.core.EventSource;
import uk.gov.justice.services.eventsourcing.source.core.EventStream;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.test.utils.core.enveloper.EnveloperFactory;
import uk.gov.moj.cpp.progression.aggregate.CaseAggregate;
import uk.gov.moj.cpp.progression.aggregate.FeeAggregate;
import uk.gov.moj.cpp.progression.aggregate.GroupCaseAggregate;
import uk.gov.moj.cpp.progression.events.CaseRemovedFromGroupCases;
import uk.gov.moj.cpp.progression.events.GroupCreatedFromMovedCases;
import uk.gov.moj.cpp.progression.events.LastCasesToBeMovedFromGroupRejected;
import uk.gov.moj.cpp.progression.handler.MoveGroupCasesHandler;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = LENIENT)
public class MoveGroupCasesHandlerTest {

    private static final UUID OLD_GROUP_ID = randomUUID();
    private static final UUID NEW_GROUP_ID = randomUUID();
    private static final UUID CASE1_ID = randomUUID();
    private static final UUID CASE2_ID = randomUUID();
    private static final UUID CASE3_ID = randomUUID();
    private static final UUID CIVIL_FEE_ID = randomUUID();

    @InjectMocks
    private MoveGroupCasesHandler handler;

    @Mock
    private EventSource eventSource;

    @Mock
    private EventStream oldGroupEventStream;

    @Mock
    private EventStream newGroupEventStream;

    @Mock
    private EventStream caseEventStream1;

    @Mock
    private EventStream caseEventStream2;

    @Mock
    private EventStream feeEventStream;

    @Mock
    private EventStream newFeeEventStream;

    @Mock
    private AggregateService aggregateService;

    @Spy
    private final Enveloper enveloper = EnveloperFactory.createEnveloperWithEvents(
            CaseGroupInfoUpdated.class,
            CaseRemovedFromGroupCases.class,
            LastCasesToBeMovedFromGroupRejected.class,
            GroupCreatedFromMovedCases.class,
            CivilFeesAdded.class,
            ProsecutionCaseCreated.class);

    @Spy
    private JsonObjectToObjectConverter jsonObjectToObjectConverter;

    @Spy
    private ObjectToJsonObjectConverter objectToJsonObjectConverter;

    @BeforeEach
    public void setup() {
        setField(this.jsonObjectToObjectConverter, "objectMapper", new ObjectMapperProducer().objectMapper());
        setField(this.objectToJsonObjectConverter, "mapper", new ObjectMapperProducer().objectMapper());
    }

    @Test
    public void shouldHandle_MoveGroupCasesCommand() {
        assertThat(handler, isHandler(COMMAND_HANDLER)
                .with(method("handle")
                        .thatHandles("progression.command.move-group-cases")
                ));
    }

    @Test
    public void shouldMoveSubsetOfCasesIntoNewGroup_AndCloneCivilFeesOnce() throws Exception {
        final CaseAggregate caseAggregate1 = createCase(CASE1_ID, OLD_GROUP_ID, false, CIVIL_FEE_ID);
        final CaseAggregate caseAggregate2 = createCase(CASE2_ID, OLD_GROUP_ID, false, CIVIL_FEE_ID);
        final GroupCaseAggregate oldGroupAggregate = createOldGroup(OLD_GROUP_ID, asList(CASE1_ID, CASE2_ID, CASE3_ID), CASE3_ID, CIVIL_FEE_ID);
        final GroupCaseAggregate newGroupAggregate = new GroupCaseAggregate();
        final FeeAggregate feeAggregate = createFeeAggregate(CIVIL_FEE_ID);
        final FeeAggregate newFeeAggregate = new FeeAggregate();

        // fallback for the cloned fee's own (randomly-generated, so unknown in advance) stream -
        // specific stubs below override it for every other, already-known id
        when(eventSource.getStreamById(any(UUID.class))).thenReturn(newFeeEventStream);
        when(aggregateService.get(newFeeEventStream, FeeAggregate.class)).thenReturn(newFeeAggregate);

        when(eventSource.getStreamById(OLD_GROUP_ID)).thenReturn(oldGroupEventStream);
        when(eventSource.getStreamById(NEW_GROUP_ID)).thenReturn(newGroupEventStream);
        when(eventSource.getStreamById(CASE1_ID)).thenReturn(caseEventStream1);
        when(eventSource.getStreamById(CASE2_ID)).thenReturn(caseEventStream2);
        when(eventSource.getStreamById(CIVIL_FEE_ID)).thenReturn(feeEventStream);
        when(aggregateService.get(oldGroupEventStream, GroupCaseAggregate.class)).thenReturn(oldGroupAggregate);
        when(aggregateService.get(newGroupEventStream, GroupCaseAggregate.class)).thenReturn(newGroupAggregate);
        when(aggregateService.get(caseEventStream1, CaseAggregate.class)).thenReturn(caseAggregate1);
        when(aggregateService.get(caseEventStream2, CaseAggregate.class)).thenReturn(caseAggregate2);
        when(aggregateService.get(feeEventStream, FeeAggregate.class)).thenReturn(feeAggregate);

        handler.handle(createMoveGroupCases(OLD_GROUP_ID, null, NEW_GROUP_ID, asList(CASE1_ID, CASE2_ID), CASE1_ID));

        // old group: both moved cases removed in a single append call, master unchanged (CASE3 not moved)
        final ArgumentCaptor<Stream> oldGroupCaptor = ArgumentCaptor.forClass(Stream.class);
        verify(oldGroupEventStream, times(1)).append(oldGroupCaptor.capture());
        final List<JsonEnvelope> oldGroupEnvelopes = toJsonEnvelopeList(oldGroupCaptor.getValue());
        assertThat(oldGroupEnvelopes.size(), is(2));

        final CaseRemovedFromGroupCases removed1 = jsonObjectToObjectConverter.convert(oldGroupEnvelopes.get(0).payloadAsJsonObject(), CaseRemovedFromGroupCases.class);
        assertThat(removed1.getGroupId(), is(OLD_GROUP_ID));
        assertThat(removed1.getMasterCaseId(), is(CASE3_ID));
        assertThat(removed1.getRemovedCase().getId(), is(CASE1_ID));
        assertThat(removed1.getNewGroupMaster(), nullValue());

        final CaseRemovedFromGroupCases removed2 = jsonObjectToObjectConverter.convert(oldGroupEnvelopes.get(1).payloadAsJsonObject(), CaseRemovedFromGroupCases.class);
        assertThat(removed2.getRemovedCase().getId(), is(CASE2_ID));
        assertThat(removed2.getNewGroupMaster(), nullValue());

        assertThat(oldGroupAggregate.getMemberCases(), is(Set.of(CASE3_ID)));
        assertThat(oldGroupAggregate.getGroupMaster(), is(CASE3_ID));

        // new group: created once from the moved cases, lead case carries the new groupId
        final ArgumentCaptor<Stream> newGroupCaptor = ArgumentCaptor.forClass(Stream.class);
        verify(newGroupEventStream, times(1)).append(newGroupCaptor.capture());
        final JsonEnvelope newGroupEnvelope = firstEnvelope(newGroupCaptor.getValue());
        assertThat(newGroupEnvelope.metadata().name(), is("progression.event.group-created-from-moved-cases"));
        final GroupCreatedFromMovedCases created = jsonObjectToObjectConverter.convert(newGroupEnvelope.payloadAsJsonObject(), GroupCreatedFromMovedCases.class);
        assertThat(created.getGroupId(), is(NEW_GROUP_ID));
        assertThat(created.getCaseIds(), is(asList(CASE1_ID, CASE2_ID)));
        assertThat(created.getLeadCase().getId(), is(CASE1_ID));
        assertThat(created.getLeadCase().getGroupId(), is(NEW_GROUP_ID));
        assertThat(created.getLeadCase().getIsGroupMaster(), is(true));

        assertThat(newGroupAggregate.getGroupMaster(), is(CASE1_ID));
        assertThat(newGroupAggregate.getMemberCases(), is(Set.of(CASE1_ID, CASE2_ID)));

        // civil fee cloned exactly once (shared by the whole new group), with a brand new feeId,
        // appended to its own fresh stream - the source fee's stream must be left untouched
        final ArgumentCaptor<Stream> feeCaptor = ArgumentCaptor.forClass(Stream.class);
        verify(newFeeEventStream, times(1)).append(feeCaptor.capture());
        verify(feeEventStream, never()).append(any());
        final JsonEnvelope feeEnvelope = firstEnvelope(feeCaptor.getValue());
        final CivilFeesAdded civilFeesAdded = jsonObjectToObjectConverter.convert(feeEnvelope.payloadAsJsonObject(), CivilFeesAdded.class);
        assertThat(civilFeesAdded.getFeeId(), is(not(CIVIL_FEE_ID)));
        assertThat(civilFeesAdded.getFeeType(), is(FeeType.INITIAL.name()));
        assertThat(civilFeesAdded.getFeeStatus(), is(FeeStatus.OUTSTANDING));
        assertThat(civilFeesAdded.getPaymentReference(), is("PaymentRef01"));
        final UUID clonedFeeId = civilFeesAdded.getFeeId();

        // both moved cases: group-association flip, then fee reassignment to the single cloned feeId
        verifyCaseMovedToNewGroup(caseEventStream1, CASE1_ID, true, clonedFeeId);
        verifyCaseMovedToNewGroup(caseEventStream2, CASE2_ID, false, clonedFeeId);
    }

    /**
     * When the old group's master case is itself one of the moved cases, the caller-provided
     * {@code currentGroup.newLeadCaseId} must be promoted on its own {@code CaseAggregate} stream —
     * mirroring {@code RemoveCaseFromGroupCasesHandler}'s equivalent new-master promotion — before its
     * snapshot is embedded in the {@code CaseRemovedFromGroupCases} event, so both its own
     * {@code isGroupMaster} flag and the embedded snapshot reflect the promotion.
     */
    @Test
    public void shouldPromoteProvidedOldGroupNewMaster_AndCarryItOntoRemovalEvent() throws Exception {
        final CaseAggregate caseAggregate1 = createCase(CASE1_ID, OLD_GROUP_ID, true, CIVIL_FEE_ID);
        final CaseAggregate caseAggregate2 = createCase(CASE2_ID, OLD_GROUP_ID, false, CIVIL_FEE_ID);
        final GroupCaseAggregate oldGroupAggregate = createOldGroup(OLD_GROUP_ID, asList(CASE1_ID, CASE2_ID), CASE1_ID, CIVIL_FEE_ID);
        final GroupCaseAggregate newGroupAggregate = new GroupCaseAggregate();
        final FeeAggregate feeAggregate = createFeeAggregate(CIVIL_FEE_ID);
        final FeeAggregate newFeeAggregate = new FeeAggregate();

        when(eventSource.getStreamById(any(UUID.class))).thenReturn(newFeeEventStream);
        when(aggregateService.get(newFeeEventStream, FeeAggregate.class)).thenReturn(newFeeAggregate);

        when(eventSource.getStreamById(OLD_GROUP_ID)).thenReturn(oldGroupEventStream);
        when(eventSource.getStreamById(NEW_GROUP_ID)).thenReturn(newGroupEventStream);
        when(eventSource.getStreamById(CASE1_ID)).thenReturn(caseEventStream1);
        when(eventSource.getStreamById(CASE2_ID)).thenReturn(caseEventStream2);
        when(eventSource.getStreamById(CIVIL_FEE_ID)).thenReturn(feeEventStream);
        when(aggregateService.get(oldGroupEventStream, GroupCaseAggregate.class)).thenReturn(oldGroupAggregate);
        when(aggregateService.get(newGroupEventStream, GroupCaseAggregate.class)).thenReturn(newGroupAggregate);
        when(aggregateService.get(caseEventStream1, CaseAggregate.class)).thenReturn(caseAggregate1);
        when(aggregateService.get(caseEventStream2, CaseAggregate.class)).thenReturn(caseAggregate2);
        when(aggregateService.get(feeEventStream, FeeAggregate.class)).thenReturn(feeAggregate);

        handler.handle(createMoveGroupCases(OLD_GROUP_ID, CASE2_ID, NEW_GROUP_ID, asList(CASE1_ID), CASE1_ID));

        final ArgumentCaptor<Stream> oldGroupCaptor = ArgumentCaptor.forClass(Stream.class);
        verify(oldGroupEventStream, times(1)).append(oldGroupCaptor.capture());
        final JsonEnvelope removedEnvelope = firstEnvelope(oldGroupCaptor.getValue());
        final CaseRemovedFromGroupCases removed = jsonObjectToObjectConverter.convert(removedEnvelope.payloadAsJsonObject(), CaseRemovedFromGroupCases.class);
        assertThat(removed.getRemovedCase().getId(), is(CASE1_ID));
        assertThat(removed.getMasterCaseId(), is(CASE1_ID));
        assertThat(removed.getNewGroupMaster().getId(), is(CASE2_ID));
        assertThat(removed.getNewGroupMaster().getIsGroupMaster(), is(true));
        assertThat(removed.getNewGroupMaster().getIsGroupMember(), is(true));
        assertThat(removed.getNewGroupMaster().getGroupId(), is(OLD_GROUP_ID));

        assertThat(oldGroupAggregate.getGroupMaster(), is(CASE2_ID));
        assertThat(oldGroupAggregate.getMemberCases(), is(Set.of(CASE2_ID)));

        // CASE2's own stream is promoted: one CaseGroupInfoUpdated event, flipping isGroupMaster to true
        verify(eventSource, times(1)).getStreamById(CASE2_ID);
        final ArgumentCaptor<Stream> caseEventStream2Captor = ArgumentCaptor.forClass(Stream.class);
        verify(caseEventStream2, times(1)).append(caseEventStream2Captor.capture());
        final JsonEnvelope caseEventStream2Envelope = firstEnvelope(caseEventStream2Captor.getValue());
        assertThat(caseEventStream2Envelope.metadata().name(), is("progression.event.case-group-info-updated"));
        final CaseGroupInfoUpdated caseGroupInfoUpdated = jsonObjectToObjectConverter.convert(caseEventStream2Envelope.payloadAsJsonObject(), CaseGroupInfoUpdated.class);
        assertThat(caseGroupInfoUpdated.getProsecutionCase().getId(), is(CASE2_ID));
        assertThat(caseGroupInfoUpdated.getProsecutionCase().getGroupId(), is(OLD_GROUP_ID));
        assertThat(caseGroupInfoUpdated.getProsecutionCase().getIsGroupMaster(), is(true));
        assertThat(caseGroupInfoUpdated.getProsecutionCase().getIsGroupMember(), is(true));
    }

    @Test
    public void shouldRejectMove_WhenItWouldLeaveOldGroupWithZeroMembers() throws Exception {
        final GroupCaseAggregate oldGroupAggregate = createOldGroup(OLD_GROUP_ID, asList(CASE1_ID, CASE2_ID), CASE2_ID, CIVIL_FEE_ID);
        when(eventSource.getStreamById(OLD_GROUP_ID)).thenReturn(oldGroupEventStream);
        when(aggregateService.get(oldGroupEventStream, GroupCaseAggregate.class)).thenReturn(oldGroupAggregate);

        handler.handle(createMoveGroupCases(OLD_GROUP_ID, CASE2_ID, NEW_GROUP_ID, asList(CASE1_ID, CASE2_ID), CASE1_ID));

        final ArgumentCaptor<Stream> captor = ArgumentCaptor.forClass(Stream.class);
        verify(oldGroupEventStream, times(1)).append(captor.capture());
        final JsonEnvelope envelope = firstEnvelope(captor.getValue());
        assertThat(envelope.metadata().name(), is("progression.event.last-cases-to-be-moved-from-group-rejected"));
        final LastCasesToBeMovedFromGroupRejected rejected = jsonObjectToObjectConverter.convert(envelope.payloadAsJsonObject(), LastCasesToBeMovedFromGroupRejected.class);
        assertThat(rejected.getGroupId(), is(OLD_GROUP_ID));
        assertThat(new HashSet<>(rejected.getCaseIds()), is(Set.of(CASE1_ID, CASE2_ID)));

        assertThat(oldGroupAggregate.getMemberCases(), is(Set.of(CASE1_ID, CASE2_ID)));

        verify(eventSource, never()).getStreamById(NEW_GROUP_ID);
        verify(eventSource, never()).getStreamById(CASE1_ID);
        verify(eventSource, never()).getStreamById(CASE2_ID);
        Mockito.verify(aggregateService, never()).get(any(), eq(CaseAggregate.class));
        Mockito.verify(aggregateService, never()).get(any(), eq(FeeAggregate.class));
    }

    @Test
    public void shouldThrowIllegalArgumentException_WhenNewLeadCaseIdNotAmongstMovedCaseIds() {
        final UUID notAMovedCase = randomUUID();
        final Envelope<MoveGroupCases> envelope = createMoveGroupCases(OLD_GROUP_ID, null, NEW_GROUP_ID, asList(CASE1_ID, CASE2_ID), notAMovedCase);

        final IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> handler.handle(envelope));
        assertThat(exception.getMessage(), is("newGroup.leadCaseId must be one of newGroup.caseIds"));
        verifyNoInteractions(eventSource);
    }

    @Test
    public void shouldThrowIllegalArgumentException_WhenOldMasterMovedButNoNewLeadCaseIdProvided() {
        final GroupCaseAggregate oldGroupAggregate = createOldGroup(OLD_GROUP_ID, asList(CASE1_ID, CASE2_ID), CASE1_ID, CIVIL_FEE_ID);
        when(eventSource.getStreamById(OLD_GROUP_ID)).thenReturn(oldGroupEventStream);
        when(aggregateService.get(oldGroupEventStream, GroupCaseAggregate.class)).thenReturn(oldGroupAggregate);

        final Envelope<MoveGroupCases> envelope = createMoveGroupCases(OLD_GROUP_ID, null, NEW_GROUP_ID, asList(CASE1_ID), CASE1_ID);

        final IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> handler.handle(envelope));
        assertThat(exception.getMessage(), is("currentGroup.newLeadCaseId is required when the current lead is among the moved cases"));
    }

    @Test
    public void shouldThrowIllegalArgumentException_WhenOldMasterNotMovedButNewLeadCaseIdProvided() {
        final GroupCaseAggregate oldGroupAggregate = createOldGroup(OLD_GROUP_ID, asList(CASE1_ID, CASE2_ID, CASE3_ID), CASE3_ID, CIVIL_FEE_ID);
        when(eventSource.getStreamById(OLD_GROUP_ID)).thenReturn(oldGroupEventStream);
        when(aggregateService.get(oldGroupEventStream, GroupCaseAggregate.class)).thenReturn(oldGroupAggregate);

        final Envelope<MoveGroupCases> envelope = createMoveGroupCases(OLD_GROUP_ID, randomUUID(), NEW_GROUP_ID, asList(CASE1_ID, CASE2_ID), CASE1_ID);

        final IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> handler.handle(envelope));
        assertThat(exception.getMessage(), is("currentGroup.newLeadCaseId must not be supplied unless the current lead is among the moved cases"));
    }

    private void verifyCaseMovedToNewGroup(final EventStream caseEventStream, final UUID caseId,
                                            final boolean expectedIsGroupMaster, final UUID expectedClonedFeeId) throws Exception {
        final ArgumentCaptor<Stream> captor = ArgumentCaptor.forClass(Stream.class);
        verify(caseEventStream, times(2)).append(captor.capture());
        final List<Stream> captured = captor.getAllValues();

        final JsonEnvelope groupInfoEnvelope = firstEnvelope(captured.get(0));
        assertThat(groupInfoEnvelope.metadata().name(), is("progression.event.case-group-info-updated"));
        final CaseGroupInfoUpdated caseGroupInfoUpdated = jsonObjectToObjectConverter.convert(groupInfoEnvelope.payloadAsJsonObject(), CaseGroupInfoUpdated.class);
        assertThat(caseGroupInfoUpdated.getProsecutionCase().getId(), is(caseId));
        assertThat(caseGroupInfoUpdated.getProsecutionCase().getGroupId(), is(NEW_GROUP_ID));
        assertThat(caseGroupInfoUpdated.getProsecutionCase().getIsGroupMember(), is(true));
        assertThat(caseGroupInfoUpdated.getProsecutionCase().getIsGroupMaster(), is(expectedIsGroupMaster));

        final JsonEnvelope feeAssignmentEnvelope = firstEnvelope(captured.get(1));
        assertThat(feeAssignmentEnvelope.metadata().name(), is("progression.event.prosecution-case-created"));
        final ProsecutionCaseCreated prosecutionCaseCreated = jsonObjectToObjectConverter.convert(feeAssignmentEnvelope.payloadAsJsonObject(), ProsecutionCaseCreated.class);
        assertThat(prosecutionCaseCreated.getProsecutionCase().getId(), is(caseId));
        assertThat(prosecutionCaseCreated.getProsecutionCase().getCivilFees().size(), is(1));
        assertThat(prosecutionCaseCreated.getProsecutionCase().getCivilFees().get(0).getFeeId(), is(expectedClonedFeeId));
    }

    private JsonEnvelope firstEnvelope(final Stream<?> stream) {
        return (JsonEnvelope) stream.findFirst().orElse(null);
    }

    @SuppressWarnings("unchecked")
    private List<JsonEnvelope> toJsonEnvelopeList(final Stream<?> stream) {
        return (List<JsonEnvelope>) (List<?>) stream.collect(toList());
    }

    private CaseAggregate createCase(final UUID caseId, final UUID groupId, final boolean isGroupMaster, final UUID civilFeeId) {
        final CaseAggregate caseAggregate = new CaseAggregate();
        final ProsecutionCase prosecutionCase = ProsecutionCase.prosecutionCase()
                .withId(caseId)
                .withGroupId(groupId)
                .withIsCivil(true)
                .withIsGroupMember(true)
                .withIsGroupMaster(isGroupMaster)
                .withProsecutionCaseIdentifier(ProsecutionCaseIdentifier.prosecutionCaseIdentifier().build())
                .withDefendants(new ArrayList<>())
                .withCivilFees(List.of(buildCivilFee(civilFeeId)))
                .build();
        caseAggregate.createProsecutionCase(prosecutionCase, emptyList());
        return caseAggregate;
    }

    private GroupCaseAggregate createOldGroup(final UUID groupId, final List<UUID> caseIds, final UUID groupMasterId, final UUID civilFeeId) {
        final GroupCaseAggregate groupCaseAggregate = new GroupCaseAggregate();
        final List<ProsecutionCase> prosecutionCases = new ArrayList<>();
        for (final UUID caseId : caseIds) {
            prosecutionCases.add(ProsecutionCase.prosecutionCase()
                    .withId(caseId)
                    .withGroupId(groupId)
                    .withIsCivil(true)
                    .withIsGroupMember(true)
                    .withIsGroupMaster(caseId.equals(groupMasterId))
                    .withProsecutionCaseIdentifier(ProsecutionCaseIdentifier.prosecutionCaseIdentifier().build())
                    .withDefendants(new ArrayList<>())
                    .withCivilFees(List.of(buildCivilFee(civilFeeId)))
                    .build());
        }
        groupCaseAggregate.initiateCourtProceedings(CourtReferral.courtReferral().withProsecutionCases(prosecutionCases).build());
        return groupCaseAggregate;
    }

    private FeeAggregate createFeeAggregate(final UUID feeId) {
        final FeeAggregate feeAggregate = new FeeAggregate();
        setField(feeAggregate, "civilFees", buildCivilFee(feeId));
        return feeAggregate;
    }

    private CivilFees buildCivilFee(final UUID feeId) {
        return CivilFees.civilFees()
                .withFeeId(feeId)
                .withFeeType(FeeType.INITIAL)
                .withFeeStatus(FeeStatus.OUTSTANDING)
                .withPaymentReference("PaymentRef01")
                .build();
    }

    private Envelope<MoveGroupCases> createMoveGroupCases(final UUID currentGroupId, final UUID currentGroupNewLeadCaseId,
                                                           final UUID newGroupId, final List<UUID> movedCaseIds, final UUID newGroupLeadCaseId) {
        final MoveGroupCases moveGroupCases = MoveGroupCases.moveGroupCases()
                .withCurrentGroup(CurrentGroup.currentGroup()
                        .withGroupId(currentGroupId)
                        .withNewLeadCaseId(currentGroupNewLeadCaseId)
                        .build())
                .withNewGroup(NewGroup.newGroup()
                        .withGroupId(newGroupId)
                        .withCaseIds(movedCaseIds)
                        .withLeadCaseId(newGroupLeadCaseId)
                        .build())
                .build();

        final JsonEnvelope requestEnvelope = JsonEnvelope.envelopeFrom(
                metadataWithRandomUUID(randomUUID().toString()),
                createObjectBuilder().build());

        return Enveloper.envelop(moveGroupCases)
                .withName("progression.command.move-group-cases")
                .withMetadataFrom(requestEnvelope);
    }
}
