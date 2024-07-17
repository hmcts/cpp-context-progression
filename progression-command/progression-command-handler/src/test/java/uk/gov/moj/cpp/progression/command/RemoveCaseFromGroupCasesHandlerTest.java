package uk.gov.moj.cpp.progression.command;

import static java.util.Arrays.asList;
import static java.util.Objects.nonNull;
import static java.util.UUID.randomUUID;
import static java.util.stream.Collectors.toList;
import static javax.json.Json.createObjectBuilder;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.core.annotation.Component.COMMAND_HANDLER;
import static uk.gov.justice.services.test.utils.core.matchers.HandlerMatcher.isHandler;
import static uk.gov.justice.services.test.utils.core.matchers.HandlerMethodMatcher.method;
import static uk.gov.justice.services.test.utils.core.messaging.MetadataBuilderFactory.metadataWithRandomUUID;
import static uk.gov.justice.services.test.utils.core.reflection.ReflectionUtil.setField;

import uk.gov.justice.core.courts.CaseGroupInfoUpdated;
import uk.gov.justice.core.courts.CourtProceedingsInitiated;
import uk.gov.justice.core.courts.CourtReferral;
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
import uk.gov.justice.services.eventsourcing.source.core.exception.EventStreamException;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.test.utils.core.enveloper.EnveloperFactory;
import uk.gov.moj.cpp.progression.aggregate.CaseAggregate;
import uk.gov.moj.cpp.progression.aggregate.GroupCaseAggregate;
import uk.gov.moj.cpp.progression.events.CaseRemovedFromGroupCases;
import uk.gov.moj.cpp.progression.events.LastCaseToBeRemovedFromGroupCasesRejected;
import uk.gov.moj.cpp.progression.handler.RemoveCaseFromGroupCasesHandler;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

import org.hamcrest.CoreMatchers;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.Spy;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class RemoveCaseFromGroupCasesHandlerTest {

    private static final UUID GROUP_ID = randomUUID();
    private static final UUID CASE1_ID = randomUUID();
    private static final UUID CASE2_ID = randomUUID();
    private static final UUID CASE3_ID = randomUUID();
    private static final UUID CASE4_ID = randomUUID();

    @InjectMocks
    private RemoveCaseFromGroupCasesHandler handler;

    @Mock
    private EventSource eventSource;

    @Mock
    private EventStream groupEventStream;

    @Mock
    private EventStream caseEventStream1;

    @Mock
    private EventStream caseEventStream2;

    @Mock
    private EventStream caseEventStream3;

    @Mock
    private EventStream caseEventStream4;

    @Mock
    private AggregateService aggregateService;

    @Spy
    private final Enveloper enveloper = EnveloperFactory.createEnveloperWithEvents(CaseGroupInfoUpdated.class, CaseRemovedFromGroupCases.class, LastCaseToBeRemovedFromGroupCasesRejected.class);

    @Spy
    private JsonObjectToObjectConverter jsonObjectToObjectConverter;

    @Spy
    private ObjectToJsonObjectConverter objectToJsonObjectConverter;

    private GroupCaseAggregate groupCaseAggregate;
    private CaseAggregate caseAggregate1;
    private CaseAggregate caseAggregate2;
    private CaseAggregate caseAggregate3;
    private CaseAggregate caseAggregate4;

    @Before
    public void setup() {
        setField(this.jsonObjectToObjectConverter, "objectMapper", new ObjectMapperProducer().objectMapper());
        setField(this.objectToJsonObjectConverter, "mapper", new ObjectMapperProducer().objectMapper());
        groupCaseAggregate = new GroupCaseAggregate();
        caseAggregate1 = new CaseAggregate();
        caseAggregate2 = new CaseAggregate();
        caseAggregate3 = new CaseAggregate();
        caseAggregate4 = new CaseAggregate();
        when(eventSource.getStreamById(GROUP_ID)).thenReturn(groupEventStream);
        when(eventSource.getStreamById(CASE1_ID)).thenReturn(caseEventStream1);
        when(eventSource.getStreamById(CASE2_ID)).thenReturn(caseEventStream2);
        when(eventSource.getStreamById(CASE3_ID)).thenReturn(caseEventStream3);
        when(eventSource.getStreamById(CASE4_ID)).thenReturn(caseEventStream4);
        when(aggregateService.get(groupEventStream, GroupCaseAggregate.class)).thenReturn(groupCaseAggregate);
        when(aggregateService.get(caseEventStream1, CaseAggregate.class)).thenReturn(caseAggregate1);
        when(aggregateService.get(caseEventStream2, CaseAggregate.class)).thenReturn(caseAggregate2);
        when(aggregateService.get(caseEventStream3, CaseAggregate.class)).thenReturn(caseAggregate3);
        when(aggregateService.get(caseEventStream4, CaseAggregate.class)).thenReturn(caseAggregate4);
    }

    @Test
    public void shouldHandle_RemoveCivilCaseFromGroupCommand() {
        assertThat(handler, isHandler(COMMAND_HANDLER)
                .with(method("handle")
                        .thatHandles("progression.command.remove-case-from-group-cases")
                ));
    }

    @Test
    public void shouldHandle_WhenMemberCaseRemoved() throws Exception {
        createCases(GROUP_ID, asList(CASE1_ID, CASE2_ID, CASE3_ID, CASE4_ID), CASE4_ID);
        addCivilCasesToGroup(GROUP_ID, asList(CASE1_ID, CASE2_ID, CASE3_ID, CASE4_ID), CASE4_ID);

        assertThat(groupCaseAggregate.getMemberCases().size(), is(4));
        assertThat(groupCaseAggregate.getGroupMaster(), is(CASE4_ID));

        handler.handle(createRemoveCaseFromGroupCases(GROUP_ID, CASE1_ID));

        verifyCaseRemovedFromGroupCasesEventCreated(GROUP_ID, CASE4_ID, CASE1_ID, null);
        assertThat(groupCaseAggregate.getMemberCases().size(), is(3));
        assertThat(groupCaseAggregate.getGroupMaster(), is(CASE4_ID));
    }

    @Test
    public void shouldHandle_WhenGroupMasterRemoved() throws Exception {
        createCases(GROUP_ID, asList(CASE1_ID, CASE2_ID, CASE3_ID), CASE3_ID);
        addCivilCasesToGroup(GROUP_ID, asList(CASE1_ID, CASE2_ID, CASE3_ID), CASE3_ID);

        assertThat(groupCaseAggregate.getMemberCases().size(), is(3));
        assertThat(groupCaseAggregate.getGroupMaster(), is(CASE3_ID));

        handler.handle(createRemoveCaseFromGroupCases(GROUP_ID, CASE3_ID));

        verifyCaseRemovedFromGroupCasesEventCreated(GROUP_ID, CASE3_ID, CASE3_ID, groupCaseAggregate.getGroupMaster());
        assertThat(groupCaseAggregate.getMemberCases().size(), is(2));
        assertTrue(asList(CASE1_ID, CASE2_ID).contains(groupCaseAggregate.getGroupMaster()));
    }

    @Test
    public void shouldHandle_WhenSecondLastCaseRemoved() throws Exception {
        createCases(GROUP_ID, asList(CASE1_ID, CASE2_ID), CASE2_ID);
        addCivilCasesToGroup(GROUP_ID, asList(CASE1_ID, CASE2_ID), CASE2_ID);

        assertThat(groupCaseAggregate.getMemberCases().size(), is(2));
        assertThat(groupCaseAggregate.getGroupMaster(), is(CASE2_ID));

        handler.handle(createRemoveCaseFromGroupCases(GROUP_ID, CASE1_ID));

        verifyCaseRemovedFromGroupCasesEventCreated(GROUP_ID, CASE2_ID, CASE1_ID, null);
        assertThat(groupCaseAggregate.getMemberCases().size(), is(1));
        assertThat(groupCaseAggregate.getGroupMaster(), is(CASE2_ID));
    }

    @Test
    public void shouldHandle_WhenLastCaseRemoved() throws Exception {
        createCases(GROUP_ID, asList(CASE1_ID), CASE1_ID);
        addCivilCasesToGroup(GROUP_ID, asList(CASE1_ID), CASE1_ID);

        assertThat(groupCaseAggregate.getMemberCases().size(), is(1));
        assertThat(groupCaseAggregate.getGroupMaster(), is(CASE1_ID));

        handler.handle(createRemoveCaseFromGroupCases(GROUP_ID, CASE1_ID));

        verifyLastCaseToBeRemovedFromGroupCasesRejectedEventCreated(GROUP_ID, CASE1_ID);
        verifyZeroInteractions(caseEventStream1, caseEventStream2, caseEventStream3, caseEventStream4);
        assertThat(groupCaseAggregate.getMemberCases().size(), is(1));
        assertThat(groupCaseAggregate.getGroupMaster(), is(CASE1_ID));
    }

    private void createCases(final UUID groupId, final List<UUID> caseIds, final UUID groupMaster) {
        for (final UUID caseId : caseIds) {
            final ProsecutionCase prosecutionCase = ProsecutionCase.prosecutionCase()
                    .withId(caseId)
                    .withGroupId(groupId)
                    .withIsCivil(true)
                    .withIsGroupMember(true)
                    .withIsGroupMaster(caseId.equals(groupMaster))
                    .withProsecutionCaseIdentifier(ProsecutionCaseIdentifier.prosecutionCaseIdentifier().build())
                    .withDefendants(new ArrayList<>())
                    .build();
            final CaseAggregate caseAggregate = getCaseAggregate(caseId);
            final List<Object> eventStream = caseAggregate.createProsecutionCase(prosecutionCase).collect(toList());
            assertThat(eventStream.size(), is(1));
            final Object event = eventStream.get(0);
            assertThat(event.getClass(), is(CoreMatchers.equalTo((ProsecutionCaseCreated.class))));
            final ProsecutionCaseCreated prosecutionCaseCreated = (ProsecutionCaseCreated) event;
            assertThat(prosecutionCaseCreated.getProsecutionCase().getId(), is(caseId));
            assertThat(prosecutionCaseCreated.getProsecutionCase().getGroupId(), is(groupId));
            assertThat(prosecutionCaseCreated.getProsecutionCase().getIsCivil(), is(true));
            assertThat(prosecutionCaseCreated.getProsecutionCase().getIsGroupMember(), is(true));
            assertThat(prosecutionCaseCreated.getProsecutionCase().getIsGroupMaster(), is(caseId.equals(groupMaster)));
        }
    }

    private EventStream getCaseEventStream(final UUID caseId) {
        if (CASE1_ID.equals(caseId)) {
            return caseEventStream1;
        } else if (CASE2_ID.equals(caseId)) {
            return caseEventStream2;
        } else if (CASE3_ID.equals(caseId)) {
            return caseEventStream3;
        } else {
            return caseEventStream4;
        }
    }

    private CaseAggregate getCaseAggregate(final UUID caseId) {
        if (CASE1_ID.equals(caseId)) {
            return caseAggregate1;
        } else if (CASE2_ID.equals(caseId)) {
            return caseAggregate2;
        } else if (CASE3_ID.equals(caseId)) {
            return caseAggregate3;
        } else {
            return caseAggregate4;
        }
    }

    private void addCivilCasesToGroup(final UUID groupId, final List<UUID> caseIds, final UUID groupMaster) {
        final List<ProsecutionCase> prosecutionCases = new ArrayList<>();
        for (UUID caseId : caseIds) {
            final ProsecutionCase prosecutionCase = ProsecutionCase.prosecutionCase()
                    .withId(caseId)
                    .withGroupId(groupId)
                    .withIsCivil(true)
                    .withIsGroupMember(true)
                    .withIsGroupMaster(caseId.equals(groupMaster))
                    .withProsecutionCaseIdentifier(ProsecutionCaseIdentifier.prosecutionCaseIdentifier().build())
                    .withDefendants(new ArrayList<>())
                    .build();
            prosecutionCases.add(prosecutionCase);
        }

        final CourtReferral courtReferral = CourtReferral.courtReferral()
                .withProsecutionCases(prosecutionCases)
                .build();

        final List<Object> eventStream = groupCaseAggregate.initiateCourtProceedings(courtReferral).collect(toList());
        assertThat(eventStream.size(), is(1));
        final Object event = eventStream.get(0);
        assertThat(event.getClass(), is(CoreMatchers.equalTo((CourtProceedingsInitiated.class))));
        final CourtProceedingsInitiated courtProceedingsInitiated = (CourtProceedingsInitiated) event;
        assertThat(courtProceedingsInitiated.getCourtReferral().getProsecutionCases().size(), is(caseIds.size()));
    }

    private Envelope<RemoveCaseFromGroupCases> createRemoveCaseFromGroupCases(final UUID groupId, final UUID caseId) {
        RemoveCaseFromGroupCases removeCaseFromGroupCases = RemoveCaseFromGroupCases.removeCaseFromGroupCases()
                .withGroupId(groupId)
                .withProsecutionCaseId(caseId)
                .build();

        final JsonEnvelope requestEnvelope = JsonEnvelope.envelopeFrom(
                metadataWithRandomUUID(randomUUID().toString()),
                createObjectBuilder().build());

        return Enveloper.envelop(removeCaseFromGroupCases)
                .withName("progression.command.remove-case-from-group-cases")
                .withMetadataFrom(requestEnvelope);
    }

    private void verifyCaseRemovedFromGroupCasesEventCreated(final UUID groupId, final UUID masterCaseId,
                                                             final UUID removedCaseId, final UUID newGroupMasterId) throws EventStreamException {
        ArgumentCaptor<Stream> caseArgumentCaptor = ArgumentCaptor.forClass(Stream.class);
        ArgumentCaptor<Stream> groupArgumentCaptor = ArgumentCaptor.forClass(Stream.class);

        Mockito.verify(getCaseEventStream(removedCaseId)).append(caseArgumentCaptor.capture());
        if (nonNull(newGroupMasterId)) {
            Mockito.verify(getCaseEventStream(newGroupMasterId)).append(caseArgumentCaptor.capture());
        }
        Mockito.verify(groupEventStream, times(1)).append(groupArgumentCaptor.capture());

        final JsonEnvelope caseGroupInfoUpdatedEnvelope = (JsonEnvelope) caseArgumentCaptor.getAllValues().get(0).findFirst().orElse(null);
        CaseGroupInfoUpdated caseGroupInfoUpdated = jsonObjectToObjectConverter.convert(caseGroupInfoUpdatedEnvelope.payloadAsJsonObject(), CaseGroupInfoUpdated.class);
        assertThat("progression.event.case-group-info-updated", is(caseGroupInfoUpdatedEnvelope.metadata().name()));
        assertCase(caseGroupInfoUpdated.getProsecutionCase(), removedCaseId, groupId, Boolean.FALSE, Boolean.FALSE);

        if (nonNull(newGroupMasterId)) {
            final JsonEnvelope caseGroupInfoUpdatedEnvelope2 = (JsonEnvelope) caseArgumentCaptor.getAllValues().get(1).findFirst().orElse(null);
            CaseGroupInfoUpdated caseGroupInfoUpdated2 = jsonObjectToObjectConverter.convert(caseGroupInfoUpdatedEnvelope2.payloadAsJsonObject(), CaseGroupInfoUpdated.class);
            assertThat(caseGroupInfoUpdatedEnvelope2.metadata().name(), is("progression.event.case-group-info-updated"));
            assertCase(caseGroupInfoUpdated2.getProsecutionCase(), newGroupMasterId, groupId, Boolean.TRUE, Boolean.TRUE);
        }

        final JsonEnvelope caseRemovedFromGroupCasesEnvelope = (JsonEnvelope) groupArgumentCaptor.getAllValues().get(0).findFirst().orElse(null);
        CaseRemovedFromGroupCases caseRemovedFromGroupCases = jsonObjectToObjectConverter.convert(caseRemovedFromGroupCasesEnvelope.payloadAsJsonObject(), CaseRemovedFromGroupCases.class);
        assertThat(caseRemovedFromGroupCasesEnvelope.metadata().name(), is("progression.event.case-removed-from-group-cases"));
        assertThat(caseRemovedFromGroupCases.getGroupId(), is(groupId));
        assertThat(caseRemovedFromGroupCases.getMasterCaseId(), is(masterCaseId));

        assertCase(caseRemovedFromGroupCases.getRemovedCase(), removedCaseId, groupId, Boolean.FALSE, Boolean.FALSE);
        if (nonNull(newGroupMasterId)) {
            assertCase(caseRemovedFromGroupCases.getNewGroupMaster(), newGroupMasterId, groupId, Boolean.TRUE, Boolean.TRUE);
        }
    }

    private void assertCase(final ProsecutionCase prosecutionCase, final UUID caseId, final UUID groupId, final Boolean isGroupMember, final Boolean isGroupMaster) {
        assertThat(prosecutionCase, notNullValue());
        assertThat(prosecutionCase.getId(), is(caseId));
        assertThat(prosecutionCase.getGroupId(), is(groupId));
        assertThat(prosecutionCase.getIsCivil(), is(Boolean.TRUE));
        assertThat(prosecutionCase.getIsGroupMember(), is(isGroupMember));
        assertThat(prosecutionCase.getIsGroupMaster(), is(isGroupMaster));
    }

    private void verifyLastCaseToBeRemovedFromGroupCasesRejectedEventCreated(final UUID groupId, final UUID removedCaseId) throws EventStreamException {
        ArgumentCaptor<Stream> groupArgumentCaptor = ArgumentCaptor.forClass(Stream.class);
        Mockito.verify(groupEventStream, times(1)).append(groupArgumentCaptor.capture());

        final JsonEnvelope jsonEnvelope = (JsonEnvelope) groupArgumentCaptor.getAllValues().get(0).findFirst().orElse(null);
        LastCaseToBeRemovedFromGroupCasesRejected lastCaseToBeRemovedFromGroupCasesRejected = jsonObjectToObjectConverter.convert(jsonEnvelope.payloadAsJsonObject(), LastCaseToBeRemovedFromGroupCasesRejected.class);
        assertThat(jsonEnvelope.metadata().name(), is("progression.event.last-case-to-be-removed-from-group-cases-rejected"));
        assertThat(lastCaseToBeRemovedFromGroupCasesRejected.getGroupId(), is(groupId));
        assertThat(lastCaseToBeRemovedFromGroupCasesRejected.getCaseId(), is(removedCaseId));
    }
}