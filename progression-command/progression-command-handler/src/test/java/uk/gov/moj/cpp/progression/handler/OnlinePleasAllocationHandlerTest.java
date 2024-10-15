package uk.gov.moj.cpp.progression.handler;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static java.util.Collections.singletonMap;
import static java.util.Optional.of;
import static java.util.UUID.randomUUID;
import static javax.json.Json.createObjectBuilder;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.messaging.Envelope.envelopeFrom;
import static uk.gov.justice.services.test.utils.core.helper.EventStreamMockHelper.verifyAppendAndGetArgumentFrom;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeMatcher.jsonEnvelope;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeMetadataMatcher.metadata;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopePayloadMatcher.payload;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeStreamMatcher.streamContaining;

import uk.gov.justice.core.courts.HearingType;
import uk.gov.justice.core.courts.ProsecutionCase;
import uk.gov.justice.progression.courts.AddOnlinePleaAllocation;
import uk.gov.justice.progression.courts.OnlinePleaAllocationAdded;
import uk.gov.justice.progression.courts.OnlinePleaAllocationUpdated;
import uk.gov.justice.progression.courts.OpaNoticeSent;
import uk.gov.justice.progression.courts.UpdateOnlinePleaAllocation;
import uk.gov.justice.progression.event.OpaPressListNoticeDeactivated;
import uk.gov.justice.progression.event.OpaPressListNoticeGenerated;
import uk.gov.justice.progression.event.OpaPressListNoticeRequested;
import uk.gov.justice.progression.event.OpaPressListNoticeSent;
import uk.gov.justice.progression.event.OpaPublicListNoticeDeactivated;
import uk.gov.justice.progression.event.OpaPublicListNoticeGenerated;
import uk.gov.justice.progression.event.OpaPublicListNoticeRequested;
import uk.gov.justice.progression.event.OpaPublicListNoticeSent;
import uk.gov.justice.progression.event.OpaResultListNoticeDeactivated;
import uk.gov.justice.progression.event.OpaResultListNoticeGenerated;
import uk.gov.justice.progression.event.OpaResultListNoticeRequested;
import uk.gov.justice.progression.event.OpaResultListNoticeSent;
import uk.gov.justice.services.common.converter.ObjectToJsonObjectConverter;
import uk.gov.justice.services.common.converter.jackson.ObjectMapperProducer;
import uk.gov.justice.services.core.aggregate.AggregateService;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.eventsourcing.source.core.EventSource;
import uk.gov.justice.services.eventsourcing.source.core.EventStream;
import uk.gov.justice.services.eventsourcing.source.core.exception.EventStreamException;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.messaging.Metadata;
import uk.gov.justice.services.messaging.spi.DefaultJsonEnvelopeProvider;
import uk.gov.justice.services.test.utils.core.enveloper.EnveloperFactory;
import uk.gov.moj.cpp.progression.GenerateOpaNotice;
import uk.gov.moj.cpp.progression.aggregate.CaseAggregate;
import uk.gov.moj.cpp.progression.aggregate.HearingAggregate;
import uk.gov.moj.cpp.progression.plea.json.schemas.OffencePleaDetails;
import uk.gov.moj.cpp.progression.plea.json.schemas.OpaNoticeDocument;
import uk.gov.moj.cpp.progression.plea.json.schemas.PleasAllocationDetails;

import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Stream;

import javax.json.JsonObjectBuilder;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class OnlinePleasAllocationHandlerTest {
    @Spy
    private final Enveloper enveloper = EnveloperFactory.createEnveloperWithEvents(AddOnlinePleaAllocation.class, UpdateOnlinePleaAllocation.class,
            OnlinePleaAllocationAdded.class, OnlinePleaAllocationUpdated.class, OpaPublicListNoticeRequested.class, OpaPressListNoticeRequested.class,
            OpaResultListNoticeRequested.class, OpaPublicListNoticeGenerated.class, OpaPressListNoticeGenerated.class, OpaResultListNoticeGenerated.class,
            OpaPublicListNoticeDeactivated.class, OpaPressListNoticeDeactivated.class, OpaResultListNoticeDeactivated.class, OpaPublicListNoticeSent.class,
            OpaPressListNoticeSent.class, OpaResultListNoticeSent.class);

    private static final String FIRST_HEARING = "First hearing";

    private static final String TRIGGER_DATE = "triggerDate";
    private static final String PROGRESSION_COMMAND_ADD_ONLINE_PLEA_ALLOCATION = "progression.command.add-online-plea-allocation";
    private static final String PROGRESSION_COMMAND_UPDATE_ONLINE_PLEA_ALLOCATION = "progression.command.update-online-plea-allocation";
    private static final String PROGRESSION_EVENT_ONLINE_PLEA_ALLOCATION_ADDED = "progression.event.online-plea-allocation-added";
    private static final String PROGRESSION_EVENT_ONLINE_PLEA_ALLOCATION_UPDATED = "progression.event.online-plea-allocation-updated";
    private static final String PROGRESSION_EVENT_OPA_PUBLIC_LIST_NOTICE_REQUESTED = "progression.event.opa-public-list-notice-requested";
    private static final String PROGRESSION_EVENT_OPA_PRESS_LIST_NOTICE_REQUESTED = "progression.event.opa-press-list-notice-requested";
    private static final String PROGRESSION_EVENT_OPA_RESULT_LIST_NOTICE_REQUESTED = "progression.event.opa-result-list-notice-requested";
    private static final String PROGRESSION_EVENT_OPA_PUBLIC_LIST_NOTICE_GENERATED = "progression.event.opa-public-list-notice-generated";
    private static final String PROGRESSION_EVENT_OPA_PUBLIC_LIST_NOTICE_DEACTIVATED = "progression.event.opa-public-list-notice-deactivated";
    private static final String PROGRESSION_EVENT_OPA_PRESS_LIST_NOTICE_GENERATED = "progression.event.opa-press-list-notice-generated";
    private static final String PROGRESSION_EVENT_OPA_PRESS_LIST_NOTICE_DEACTIVATED = "progression.event.opa-press-list-notice-deactivated";
    private static final String PROGRESSION_EVENT_OPA_RESULT_LIST_NOTICE_GENERATED = "progression.event.opa-result-list-notice-generated";
    private static final String PROGRESSION_EVENT_OPA_RESULT_LIST_NOTICE_DEACTIVATED = "progression.event.opa-result-list-notice-deactivated";
    private static final String PROGRESSION_EVENT_OPA_PUBLIC_LIST_NOTICE_SENT = "progression.event.opa-public-list-notice-sent";
    private static final String PROGRESSION_EVENT_OPA_PRESS_LIST_NOTICE_SENT = "progression.event.opa-press-list-notice-sent";
    private static final String PROGRESSION_EVENT_OPA_RESULT_LIST_NOTICE_SENT = "progression.event.opa-result-list-notice-sent";

    private static final UUID defendantId = randomUUID();
    private static final UUID caseId = randomUUID();
    private static final UUID firstHearingId = randomUUID();
    private static final UUID trialHearingId = randomUUID();

    @Mock
    private EventSource eventSource;

    @Mock
    private EventStream eventStream;

    @Mock
    private EventStream trialHearingEventStream;

    @Mock
    private AggregateService aggregateService;

    @Mock
    private CaseAggregate caseAggregate;

    @Mock
    private HearingAggregate hearingAggregate;

    @Mock
    private HearingAggregate trialtHearingAggregate;

    private final ObjectToJsonObjectConverter objectToJsonObjectConverter = new ObjectToJsonObjectConverter(new ObjectMapperProducer().objectMapper());

    @InjectMocks
    @Spy
    private OnlinePleasAllocationHandler onlinePleasAllocationHandler;


    @Test
    public void shouldHandleAddOnlinePleaAllocation() throws EventStreamException {
        final HearingType firstHearing = new HearingType(FIRST_HEARING, randomUUID(), "");
        final HearingType trial = new HearingType("Trial", randomUUID(), "");

        final Envelope<AddOnlinePleaAllocation> pleaAllocationEnvelope = getAddOnlinePleaAllocationEnvelope();
        final AddOnlinePleaAllocation pleaAllocation = pleaAllocationEnvelope.payload();
        final OnlinePleaAllocationAdded pleaAllocationEvent = getOnlinePleaAllocationAddedEvent(pleaAllocation.getPleasAllocation(), firstHearingId);
        final Stream<Object> stream = Stream.builder().add(pleaAllocationEvent).build();
        final ZonedDateTime now = ZonedDateTime.now();
        when(eventSource.getStreamById(caseId)).thenReturn(eventStream);
        when(aggregateService.get(eventStream, CaseAggregate.class)).thenReturn(caseAggregate);
        when(eventSource.getStreamById(firstHearingId)).thenReturn(eventStream);
        when(aggregateService.get(eventStream, HearingAggregate.class)).thenReturn(hearingAggregate);
        when(caseAggregate.addOnlinePleaAllocation(any(), any(UUID.class))).thenReturn(stream);
        when(caseAggregate.getLinkedHearingIds()).thenReturn(new HashSet<UUID>() {{
            add(firstHearingId);
        }});

        when(hearingAggregate.getHearingType()).thenReturn(firstHearing);
        onlinePleasAllocationHandler.handleAddOnlinePleasAllocation(pleaAllocationEnvelope);

        verify(caseAggregate).getLinkedHearingIds();
        verify(caseAggregate).addOnlinePleaAllocation(pleaAllocation.getPleasAllocation(), firstHearingId);
        verify(eventStream).append(any());

        verifyAddOnlinePleaEventStreamContents(pleaAllocationEvent);
    }

    @Test
    public void shouldHandleAddOnlinePleaAllocationWithoutFirstHearing() throws EventStreamException {
        final HearingType trailHearingType = new HearingType("Trial", randomUUID(), "");
        final Envelope<AddOnlinePleaAllocation> pleaAllocationEnvelope = getAddOnlinePleaAllocationEnvelope();
        final AddOnlinePleaAllocation pleaAllocation = pleaAllocationEnvelope.payload();
        when(eventSource.getStreamById(caseId)).thenReturn(eventStream);
        when(aggregateService.get(eventStream, CaseAggregate.class)).thenReturn(caseAggregate);
        when(caseAggregate.getLinkedHearingIds()).thenReturn(Collections.singleton(trialHearingId));
        when(eventSource.getStreamById(trialHearingId)).thenReturn(trialHearingEventStream);
        when(aggregateService.get(trialHearingEventStream, HearingAggregate.class)).thenReturn(trialtHearingAggregate);
        when(trialtHearingAggregate.getHearingType()).thenReturn(trailHearingType);

        onlinePleasAllocationHandler.handleAddOnlinePleasAllocation(pleaAllocationEnvelope);

        verify(eventSource).getStreamById(pleaAllocation.getPleasAllocation().getCaseId());
        verify(eventSource).getStreamById(trialHearingId);
        verify(aggregateService).get(eventStream, CaseAggregate.class);
        verify(aggregateService).get(trialHearingEventStream, HearingAggregate.class);
        verify(caseAggregate).getLinkedHearingIds();
        verifyNoMoreInteractions(caseAggregate);
        verifyNoMoreInteractions(eventStream);
        verifyNoMoreInteractions(trialHearingEventStream);
    }

    @Test
    public void shouldHandleUpdateOnlinePleaAllocation() throws EventStreamException {
        final UUID hearingId = randomUUID();
        final Envelope<UpdateOnlinePleaAllocation> pleaAllocationEnvelope = getUpdateOnlinePleaAllocationEnvelope();
        final UpdateOnlinePleaAllocation pleaAllocation = pleaAllocationEnvelope.payload();
        final OnlinePleaAllocationUpdated pleaAllocationEvent = getOnlinePleaAllocationUpdatedEvent(pleaAllocation.getPleasAllocation(), hearingId);
        when(eventSource.getStreamById(caseId)).thenReturn(eventStream);
        when(aggregateService.get(eventStream, CaseAggregate.class)).thenReturn(caseAggregate);
        when(caseAggregate.updateOnlinePleaAllocation(any())).thenReturn(of(Stream.of(pleaAllocationEvent)));

        onlinePleasAllocationHandler.handleUpdateOnlinePleasAllocation(pleaAllocationEnvelope);

        verify(eventSource).getStreamById(pleaAllocation.getPleasAllocation().getCaseId());
        verify(aggregateService).get(eventStream, CaseAggregate.class);
        verify(caseAggregate).updateOnlinePleaAllocation(pleaAllocation.getPleasAllocation());
        verify(eventStream).append(any());

        verifyUpdateOnlinePleaEventStreamContents(pleaAllocationEvent);
    }

    @Test
    public void shouldHandleRequestOpaPublicListNotice() throws EventStreamException {
        final JsonEnvelope jsonEnvelope = getJsonEnvelope("progression.command.request-opa-public-list-notice", singletonMap(TRIGGER_DATE, LocalDate.now().toString()));

        when(eventSource.getStreamById(any(UUID.class))).thenReturn(eventStream);

        onlinePleasAllocationHandler.handleRequestOpaPublicListNotice(jsonEnvelope);

        verify(eventSource).getStreamById(any(UUID.class));
        assertThat(verifyAppendAndGetArgumentFrom(eventStream), streamContaining(
                jsonEnvelope(
                        metadata()
                                .withName(PROGRESSION_EVENT_OPA_PUBLIC_LIST_NOTICE_REQUESTED),
                        payload().isJson(allOf(
                                withJsonPath("$.triggerDate", is(LocalDate.now().toString())))))));
    }

    @Test
    public void shouldHandleRequestOpaPressListNotice() throws EventStreamException {
        final JsonEnvelope jsonEnvelope = getJsonEnvelope("progression.command.request-opa-press-list-notice", singletonMap(TRIGGER_DATE, LocalDate.now().toString()));

        when(eventSource.getStreamById(any(UUID.class))).thenReturn(eventStream);

        onlinePleasAllocationHandler.handleRequestOpaPressListNotice(jsonEnvelope);

        verify(eventSource).getStreamById(any(UUID.class));
        assertThat(verifyAppendAndGetArgumentFrom(eventStream), streamContaining(
                jsonEnvelope(
                        metadata()
                                .withName(PROGRESSION_EVENT_OPA_PRESS_LIST_NOTICE_REQUESTED),
                        payload().isJson(allOf(
                                withJsonPath("$.triggerDate", is(LocalDate.now().toString())))))));
    }

    @Test
    public void shouldHandleRequestOpaResultListNotice() throws EventStreamException {
        final JsonEnvelope jsonEnvelope = getJsonEnvelope("progression.command.request-opa-result-list-notice", singletonMap(TRIGGER_DATE, LocalDate.now().toString()));

        when(eventSource.getStreamById(any(UUID.class))).thenReturn(eventStream);

        onlinePleasAllocationHandler.handleRequestOpaResultListNotice(jsonEnvelope);

        verify(eventSource).getStreamById(any(UUID.class));
        assertThat(verifyAppendAndGetArgumentFrom(eventStream), streamContaining(
                jsonEnvelope(
                        metadata()
                                .withName(PROGRESSION_EVENT_OPA_RESULT_LIST_NOTICE_REQUESTED),
                        payload().isJson(allOf(
                                withJsonPath("$.triggerDate", is(LocalDate.now().toString())))))));
    }

    @Test
    public void shouldHandleGenerateOpaPublicListNotice() throws EventStreamException {
        final GenerateOpaNotice generateOpaNotice = getGenerateOpaNotice();
        final Envelope<GenerateOpaNotice> envelope = getEnvelope("progression.command.generate-opa-public-list-notice", generateOpaNotice);
        final OpaNoticeDocument document = OpaNoticeDocument.opaNoticeDocument().build();
        final OpaPublicListNoticeGenerated event = OpaPublicListNoticeGenerated.opaPublicListNoticeGenerated()
                .withNotificationId(randomUUID())
                .withHearingId(generateOpaNotice.getHearingId())
                .withDefendantId(generateOpaNotice.getDefendantId())
                .withTriggerDate(generateOpaNotice.getTriggerDate())
                .withOpaNotice(document).build();
        final ProsecutionCase prosecutionCase = ProsecutionCase.prosecutionCase().withId(caseId).build();
        when(eventSource.getStreamById(firstHearingId)).thenReturn(eventStream);
        when(aggregateService.get(eventStream, HearingAggregate.class)).thenReturn(hearingAggregate);
        when(hearingAggregate.isPublicListNoticeAlreadySent(generateOpaNotice.getDefendantId(), generateOpaNotice.getTriggerDate())).thenReturn(false);
        when(hearingAggregate.checkOpaPublicListCriteria(generateOpaNotice.getDefendantId(), generateOpaNotice.getTriggerDate())).thenReturn(true);
        when(hearingAggregate.generateOpaPublicListNotice(any(ProsecutionCase.class), eq(defendantId), eq(generateOpaNotice.getTriggerDate()))).thenReturn(Stream.of(event));
        when(eventSource.getStreamById(caseId)).thenReturn(eventStream);
        when(aggregateService.get(eventStream, CaseAggregate.class)).thenReturn(caseAggregate);
        when(caseAggregate.getProsecutionCase()).thenReturn(prosecutionCase);

        onlinePleasAllocationHandler.handleGenerateOpaPublicListNotice(envelope);

        verify(eventSource).getStreamById(caseId);
        verify(eventSource).getStreamById(firstHearingId);
        verify(aggregateService).get(eventStream, CaseAggregate.class);
        verify(aggregateService).get(eventStream, HearingAggregate.class);
        verify(hearingAggregate).isPublicListNoticeAlreadySent(defendantId, generateOpaNotice.getTriggerDate());
        verify(hearingAggregate).checkOpaPublicListCriteria(defendantId, generateOpaNotice.getTriggerDate());
        verify(caseAggregate).getProsecutionCase();
        verify(hearingAggregate).generateOpaPublicListNotice(eq(prosecutionCase), eq(defendantId), eq(generateOpaNotice.getTriggerDate()));
        assertTrue(verifyAppendAndGetArgumentFrom(eventStream).anyMatch(env -> env.metadata().name().equals(PROGRESSION_EVENT_OPA_PUBLIC_LIST_NOTICE_GENERATED)));
        verify(eventStream).append(any());

        verifyNoMoreInteractions(caseAggregate);
        verifyNoMoreInteractions(eventStream);
    }

    @Test
    public void shouldHandleGenerateOpaPublicListNoticeWhenNoticeAlreadySent() throws EventStreamException {
        final GenerateOpaNotice generateOpaNotice = getGenerateOpaNotice();
        final Envelope<GenerateOpaNotice> envelope = getEnvelope("progression.command.generate-opa-public-list-notice", generateOpaNotice);

        when(eventSource.getStreamById(firstHearingId)).thenReturn(eventStream);
        when(aggregateService.get(eventStream, HearingAggregate.class)).thenReturn(hearingAggregate);
        when(hearingAggregate.isPublicListNoticeAlreadySent(generateOpaNotice.getDefendantId(), generateOpaNotice.getTriggerDate())).thenReturn(true);

        onlinePleasAllocationHandler.handleGenerateOpaPublicListNotice(envelope);

        verify(eventSource).getStreamById(firstHearingId);
        verify(aggregateService).get(eventStream, HearingAggregate.class);
        verify(hearingAggregate).isPublicListNoticeAlreadySent(defendantId, generateOpaNotice.getTriggerDate());

        verifyNoMoreInteractions(caseAggregate);
        verifyNoMoreInteractions(eventStream);
    }

    @Test
    public void shouldHandleGenerateOpaPublicListNoticeWhenCriteriaDontMatch() throws EventStreamException {
        final GenerateOpaNotice generateOpaNotice = getGenerateOpaNotice();
        final Envelope<GenerateOpaNotice> envelope = getEnvelope("progression.command.generate-opa-public-list-notice", generateOpaNotice);
        final OpaPublicListNoticeDeactivated event = OpaPublicListNoticeDeactivated.opaPublicListNoticeDeactivated().withDefendantId(defendantId).withCaseId(caseId).withHearingId(firstHearingId).build();
        when(eventSource.getStreamById(firstHearingId)).thenReturn(eventStream);
        when(aggregateService.get(eventStream, HearingAggregate.class)).thenReturn(hearingAggregate);
        when(hearingAggregate.isPublicListNoticeAlreadySent(generateOpaNotice.getDefendantId(), generateOpaNotice.getTriggerDate())).thenReturn(false);
        when(hearingAggregate.checkOpaPublicListCriteria(generateOpaNotice.getDefendantId(), generateOpaNotice.getTriggerDate())).thenReturn(false);
        when(hearingAggregate.generateDeactivateOpaPublicListNotice(caseId, defendantId, firstHearingId)).thenReturn(Stream.of(event));

        onlinePleasAllocationHandler.handleGenerateOpaPublicListNotice(envelope);

        verify(eventSource).getStreamById(firstHearingId);
        verify(aggregateService).get(eventStream, HearingAggregate.class);
        verify(hearingAggregate).isPublicListNoticeAlreadySent(defendantId, generateOpaNotice.getTriggerDate());
        verify(hearingAggregate).checkOpaPublicListCriteria(defendantId, generateOpaNotice.getTriggerDate());
        verify(hearingAggregate).generateDeactivateOpaPublicListNotice(caseId, defendantId, firstHearingId);
        assertTrue(verifyAppendAndGetArgumentFrom(eventStream).anyMatch(env -> env.metadata().name().equals(PROGRESSION_EVENT_OPA_PUBLIC_LIST_NOTICE_DEACTIVATED)));
        verify(eventStream).append(any());

        verifyNoMoreInteractions(caseAggregate);
        verifyNoMoreInteractions(eventStream);
    }

    @Test
    public void shouldHandleGenerateOpaPressListNotice() throws EventStreamException {
        when(eventSource.getStreamById(caseId)).thenReturn(eventStream);
        when(aggregateService.get(eventStream, CaseAggregate.class)).thenReturn(caseAggregate);
        when(eventSource.getStreamById(firstHearingId)).thenReturn(eventStream);
        when(aggregateService.get(eventStream, HearingAggregate.class)).thenReturn(hearingAggregate);

        final GenerateOpaNotice generateOpaNotice = getGenerateOpaNotice();
        final Envelope<GenerateOpaNotice> envelope = getEnvelope("progression.command.generate-opa-press-list-notice", generateOpaNotice);
        final OpaNoticeDocument document = OpaNoticeDocument.opaNoticeDocument().build();
        final OpaPressListNoticeGenerated event = OpaPressListNoticeGenerated.opaPressListNoticeGenerated()
                .withNotificationId(randomUUID())
                .withHearingId(generateOpaNotice.getHearingId())
                .withDefendantId(generateOpaNotice.getDefendantId())
                .withTriggerDate(generateOpaNotice.getTriggerDate())
                .withOpaNotice(document)
                .build();

        final ProsecutionCase prosecutionCase = ProsecutionCase.prosecutionCase().withId(caseId).build();

        when(hearingAggregate.isPressListNoticeAlreadySent(generateOpaNotice.getDefendantId(), generateOpaNotice.getTriggerDate())).thenReturn(false);
        when(hearingAggregate.checkOpaPressListCriteria(generateOpaNotice.getTriggerDate())).thenReturn(true);
        when(hearingAggregate.generateOpaPressListNotice(any(ProsecutionCase.class), eq(defendantId), any(), eq(generateOpaNotice.getTriggerDate()))).thenReturn(Stream.of(event));
        when(caseAggregate.getProsecutionCase()).thenReturn(prosecutionCase);

        onlinePleasAllocationHandler.handleGenerateOpaPressListNotice(envelope);

        verify(eventSource).getStreamById(caseId);
        verify(eventSource).getStreamById(firstHearingId);
        verify(aggregateService).get(eventStream, CaseAggregate.class);
        verify(aggregateService).get(eventStream, HearingAggregate.class);
        verify(hearingAggregate).isPressListNoticeAlreadySent(defendantId, generateOpaNotice.getTriggerDate());
        verify(hearingAggregate).checkOpaPressListCriteria(generateOpaNotice.getTriggerDate());
        verify(caseAggregate).getProsecutionCase();
        verify(caseAggregate).getOnlinePleasAllocation(defendantId);
        assertTrue(verifyAppendAndGetArgumentFrom(eventStream).anyMatch(env -> env.metadata().name().equals(PROGRESSION_EVENT_OPA_PRESS_LIST_NOTICE_GENERATED)));
        verify(eventStream).append(any());

        verifyNoMoreInteractions(caseAggregate);
        verifyNoMoreInteractions(eventStream);
    }

    @Test
    public void shouldHandleGenerateOpaPressListNoticeWhenNoticeAlreadySent() throws EventStreamException {
        final GenerateOpaNotice generateOpaNotice = getGenerateOpaNotice();
        final Envelope<GenerateOpaNotice> envelope = getEnvelope("progression.command.generate-opa-press-list-notice", generateOpaNotice);

        when(eventSource.getStreamById(firstHearingId)).thenReturn(eventStream);
        when(aggregateService.get(eventStream, HearingAggregate.class)).thenReturn(hearingAggregate);
        when(hearingAggregate.isPressListNoticeAlreadySent(generateOpaNotice.getDefendantId(), generateOpaNotice.getTriggerDate())).thenReturn(true);

        onlinePleasAllocationHandler.handleGenerateOpaPressListNotice(envelope);

        verify(eventSource).getStreamById(firstHearingId);
        verify(aggregateService).get(eventStream, HearingAggregate.class);
        verify(hearingAggregate).isPressListNoticeAlreadySent(defendantId, generateOpaNotice.getTriggerDate());

        verifyNoMoreInteractions(caseAggregate);
        verifyNoMoreInteractions(eventStream);
    }

    @Test
    public void shouldHandleGenerateOpaPressListNoticeWhenCriteriaDontMatch() throws EventStreamException {
        final GenerateOpaNotice generateOpaNotice = getGenerateOpaNotice();
        final Envelope<GenerateOpaNotice> envelope = getEnvelope("progression.command.generate-opa-press-list-notice", generateOpaNotice);
        final OpaPressListNoticeDeactivated event = OpaPressListNoticeDeactivated.opaPressListNoticeDeactivated().withDefendantId(defendantId).withCaseId(caseId).withHearingId(firstHearingId).build();
        when(eventSource.getStreamById(firstHearingId)).thenReturn(eventStream);
        when(aggregateService.get(eventStream, HearingAggregate.class)).thenReturn(hearingAggregate);

        when(hearingAggregate.isPressListNoticeAlreadySent(generateOpaNotice.getDefendantId(), generateOpaNotice.getTriggerDate())).thenReturn(false);
        when(hearingAggregate.checkOpaPressListCriteria(generateOpaNotice.getTriggerDate())).thenReturn(false);
        when(hearingAggregate.generateDeactivateOpaPressListNotice(caseId, defendantId, firstHearingId)).thenReturn(Stream.of(event));

        onlinePleasAllocationHandler.handleGenerateOpaPressListNotice(envelope);

        verify(eventSource).getStreamById(firstHearingId);
        verify(aggregateService).get(eventStream, HearingAggregate.class);
        verify(hearingAggregate).isPressListNoticeAlreadySent(defendantId, generateOpaNotice.getTriggerDate());
        verify(hearingAggregate).checkOpaPressListCriteria(generateOpaNotice.getTriggerDate());
        verify(hearingAggregate).generateDeactivateOpaPressListNotice(caseId, defendantId, firstHearingId);
        assertTrue(verifyAppendAndGetArgumentFrom(eventStream).anyMatch(env -> env.metadata().name().equals(PROGRESSION_EVENT_OPA_PRESS_LIST_NOTICE_DEACTIVATED)));
        verify(eventStream).append(any());

        verifyNoMoreInteractions(caseAggregate);
        verifyNoMoreInteractions(eventStream);
    }

    @Test
    public void shouldHandleGenerateOpaResultListNotice() throws EventStreamException {
        final GenerateOpaNotice generateOpaNotice = getGenerateOpaNotice();
        final Envelope<GenerateOpaNotice> envelope = getEnvelope("progression.command.generate-opa-result-list-notice", generateOpaNotice);
        final OpaNoticeDocument document = OpaNoticeDocument.opaNoticeDocument().build();
        final OpaResultListNoticeGenerated event = OpaResultListNoticeGenerated.opaResultListNoticeGenerated()
                .withNotificationId(randomUUID())
                .withHearingId(generateOpaNotice.getHearingId())
                .withDefendantId(generateOpaNotice.getDefendantId())
                .withTriggerDate(generateOpaNotice.getTriggerDate())
                .withOpaNotice(document)
                .build();

        final ProsecutionCase prosecutionCase = ProsecutionCase.prosecutionCase().withId(caseId).build();
        when(eventSource.getStreamById(firstHearingId)).thenReturn(eventStream);
        when(aggregateService.get(eventStream, HearingAggregate.class)).thenReturn(hearingAggregate);

        when(hearingAggregate.isResultListNoticeAlreadySent(generateOpaNotice.getDefendantId(), generateOpaNotice.getTriggerDate())).thenReturn(false);
        when(hearingAggregate.checkOpaResultListCriteria(defendantId, generateOpaNotice.getTriggerDate())).thenReturn(true);
        when(hearingAggregate.generateOpaResultListNotice(any(ProsecutionCase.class), eq(defendantId), eq(generateOpaNotice.getTriggerDate()))).thenReturn(Stream.of(event));
        when(eventSource.getStreamById(caseId)).thenReturn(eventStream);
        when(aggregateService.get(eventStream, CaseAggregate.class)).thenReturn(caseAggregate);
        when(caseAggregate.getProsecutionCase()).thenReturn(prosecutionCase);

        onlinePleasAllocationHandler.handleGenerateOpaResultListNotice(envelope);

        verify(eventSource).getStreamById(caseId);
        verify(eventSource).getStreamById(firstHearingId);
        verify(aggregateService).get(eventStream, CaseAggregate.class);
        verify(aggregateService).get(eventStream, HearingAggregate.class);
        verify(hearingAggregate).isResultListNoticeAlreadySent(defendantId, generateOpaNotice.getTriggerDate());
        verify(hearingAggregate).checkOpaResultListCriteria(defendantId, generateOpaNotice.getTriggerDate());
        verify(caseAggregate).getProsecutionCase();
        verify(hearingAggregate).generateOpaResultListNotice(eq(prosecutionCase), eq(defendantId), eq(generateOpaNotice.getTriggerDate()));
        assertTrue(verifyAppendAndGetArgumentFrom(eventStream).anyMatch(env -> env.metadata().name().equals(PROGRESSION_EVENT_OPA_RESULT_LIST_NOTICE_GENERATED)));
        verify(eventStream).append(any());

        verifyNoMoreInteractions(caseAggregate);
        verifyNoMoreInteractions(eventStream);
    }

    @Test
    public void shouldHandleGenerateOpaResultListNoticeWhenNoticeAlreadySent() throws EventStreamException {
        final GenerateOpaNotice generateOpaNotice = getGenerateOpaNotice();
        final Envelope<GenerateOpaNotice> envelope = getEnvelope("progression.command.generate-opa-result-list-notice", generateOpaNotice);

        when(eventSource.getStreamById(firstHearingId)).thenReturn(eventStream);
        when(aggregateService.get(eventStream, HearingAggregate.class)).thenReturn(hearingAggregate);

        when(hearingAggregate.isResultListNoticeAlreadySent(generateOpaNotice.getDefendantId(), generateOpaNotice.getTriggerDate())).thenReturn(true);

        onlinePleasAllocationHandler.handleGenerateOpaResultListNotice(envelope);

        verify(eventSource).getStreamById(firstHearingId);
        verify(aggregateService).get(eventStream, HearingAggregate.class);
        verify(hearingAggregate).isResultListNoticeAlreadySent(defendantId, generateOpaNotice.getTriggerDate());

        verifyNoMoreInteractions(caseAggregate);
        verifyNoMoreInteractions(eventStream);
    }

    @Test
    public void shouldHandleGenerateOpaResultListNoticeWhenCriteriaDontMatch() throws EventStreamException {
        final GenerateOpaNotice generateOpaNotice = getGenerateOpaNotice();
        final Envelope<GenerateOpaNotice> envelope = getEnvelope("progression.command.generate-opa-result-list-notice", generateOpaNotice);
        final OpaResultListNoticeDeactivated event = OpaResultListNoticeDeactivated.opaResultListNoticeDeactivated().withDefendantId(defendantId).withCaseId(caseId).withHearingId(firstHearingId).build();

        when(eventSource.getStreamById(firstHearingId)).thenReturn(eventStream);
        when(aggregateService.get(eventStream, HearingAggregate.class)).thenReturn(hearingAggregate);
        when(hearingAggregate.isResultListNoticeAlreadySent(generateOpaNotice.getDefendantId(), generateOpaNotice.getTriggerDate())).thenReturn(false);
        when(hearingAggregate.checkOpaResultListCriteria(defendantId, generateOpaNotice.getTriggerDate())).thenReturn(false);
        when(hearingAggregate.generateDeactivateOpaResultListNotice(caseId, defendantId, firstHearingId)).thenReturn(Stream.of(event));

        onlinePleasAllocationHandler.handleGenerateOpaResultListNotice(envelope);

        verify(eventSource).getStreamById(firstHearingId);
        verify(aggregateService).get(eventStream, HearingAggregate.class);
        verify(hearingAggregate).isResultListNoticeAlreadySent(defendantId, generateOpaNotice.getTriggerDate());
        verify(hearingAggregate).checkOpaResultListCriteria(defendantId, generateOpaNotice.getTriggerDate());
        verify(hearingAggregate).generateDeactivateOpaResultListNotice(caseId, defendantId, firstHearingId);
        assertTrue(verifyAppendAndGetArgumentFrom(eventStream).anyMatch(env -> env.metadata().name().equals(PROGRESSION_EVENT_OPA_RESULT_LIST_NOTICE_DEACTIVATED)));
        verify(eventStream).append(any());

        verifyNoMoreInteractions(caseAggregate);
        verifyNoMoreInteractions(eventStream);
    }

    @Test
    public void testHandleOpaPublicListNoticeSent() throws EventStreamException {
        final OpaNoticeSent opaNoticeSent = getOpaNoticeSent();
        final Envelope<OpaNoticeSent> envelope = getEnvelope("progression.command.opa-public-list-notice-sent", opaNoticeSent);
        final OpaPublicListNoticeSent event = OpaPublicListNoticeSent.opaPublicListNoticeSent()
                .withDefendantId(opaNoticeSent.getDefendantId())
                .withHearingId(opaNoticeSent.getHearingId())
                .withNotificationId(opaNoticeSent.getHearingId())
                .withTriggerDate(opaNoticeSent.getTriggerDate())
                .build();
        when(eventSource.getStreamById(firstHearingId)).thenReturn(eventStream);
        when(aggregateService.get(eventStream, HearingAggregate.class)).thenReturn(hearingAggregate);

        when(hearingAggregate.opaPublicListNoticeSent(opaNoticeSent.getNotificationId(), opaNoticeSent.getHearingId(), opaNoticeSent.getDefendantId(), opaNoticeSent.getTriggerDate())).thenReturn(Stream.of(event));

        onlinePleasAllocationHandler.handleOpaPublicListNoticeSent(envelope);

        verify(eventSource).getStreamById(opaNoticeSent.getHearingId());
        verify(aggregateService).get(eventStream, HearingAggregate.class);
        verify(hearingAggregate).opaPublicListNoticeSent(opaNoticeSent.getNotificationId(), opaNoticeSent.getHearingId(), opaNoticeSent.getDefendantId(), opaNoticeSent.getTriggerDate());
        assertTrue(verifyAppendAndGetArgumentFrom(eventStream).anyMatch(env -> env.metadata().name().equals(PROGRESSION_EVENT_OPA_PUBLIC_LIST_NOTICE_SENT)));
        verify(eventStream).append(any());

        verifyNoMoreInteractions(caseAggregate);
        verifyNoMoreInteractions(eventStream);
    }

    @Test
    public void testHandleOpaPressListNoticeSent() throws EventStreamException {
        final OpaNoticeSent opaNoticeSent = getOpaNoticeSent();
        final Envelope<OpaNoticeSent> envelope = getEnvelope("progression.command.opa-press-list-notice-sent", opaNoticeSent);
        final OpaPressListNoticeSent event = OpaPressListNoticeSent.opaPressListNoticeSent()
                .withDefendantId(opaNoticeSent.getDefendantId())
                .withHearingId(opaNoticeSent.getHearingId())
                .withNotificationId(opaNoticeSent.getHearingId())
                .withTriggerDate(opaNoticeSent.getTriggerDate())
                .build();
        when(eventSource.getStreamById(firstHearingId)).thenReturn(eventStream);
        when(aggregateService.get(eventStream, HearingAggregate.class)).thenReturn(hearingAggregate);
        when(hearingAggregate.opaPressListNoticeSent(opaNoticeSent.getNotificationId(), opaNoticeSent.getHearingId(), opaNoticeSent.getDefendantId(), opaNoticeSent.getTriggerDate())).thenReturn(Stream.of(event));

        onlinePleasAllocationHandler.handleOpaPressListNoticeSent(envelope);

        verify(eventSource).getStreamById(opaNoticeSent.getHearingId());
        verify(aggregateService).get(eventStream, HearingAggregate.class);
        verify(hearingAggregate).opaPressListNoticeSent(opaNoticeSent.getNotificationId(), opaNoticeSent.getHearingId(), opaNoticeSent.getDefendantId(), opaNoticeSent.getTriggerDate());
        assertTrue(verifyAppendAndGetArgumentFrom(eventStream).anyMatch(env -> env.metadata().name().equals(PROGRESSION_EVENT_OPA_PRESS_LIST_NOTICE_SENT)));
        verify(eventStream).append(any());

        verifyNoMoreInteractions(caseAggregate);
        verifyNoMoreInteractions(eventStream);
    }

    @Test
    public void testHandleOpaResultListNoticeSent() throws EventStreamException {
        final OpaNoticeSent opaNoticeSent = getOpaNoticeSent();
        final Envelope<OpaNoticeSent> envelope = getEnvelope("progression.command.opa-result-list-notice-sent", opaNoticeSent);
        final OpaResultListNoticeSent event = OpaResultListNoticeSent.opaResultListNoticeSent()
                .withDefendantId(opaNoticeSent.getDefendantId())
                .withHearingId(opaNoticeSent.getHearingId())
                .withNotificationId(opaNoticeSent.getHearingId())
                .withTriggerDate(opaNoticeSent.getTriggerDate())
                .build();
        when(eventSource.getStreamById(firstHearingId)).thenReturn(eventStream);
        when(aggregateService.get(eventStream, HearingAggregate.class)).thenReturn(hearingAggregate);
        when(hearingAggregate.opaResultListNoticeSent(opaNoticeSent.getNotificationId(), opaNoticeSent.getHearingId(), opaNoticeSent.getDefendantId(), opaNoticeSent.getTriggerDate())).thenReturn(Stream.of(event));

        onlinePleasAllocationHandler.handleOpaResultListNoticeSent(envelope);

        verify(eventSource).getStreamById(opaNoticeSent.getHearingId());
        verify(aggregateService).get(eventStream, HearingAggregate.class);
        verify(hearingAggregate).opaResultListNoticeSent(opaNoticeSent.getNotificationId(), opaNoticeSent.getHearingId(), opaNoticeSent.getDefendantId(), opaNoticeSent.getTriggerDate());
        assertTrue(verifyAppendAndGetArgumentFrom(eventStream).anyMatch(env -> env.metadata().name().equals(PROGRESSION_EVENT_OPA_RESULT_LIST_NOTICE_SENT)));
        verify(eventStream).append(any());

        verifyNoMoreInteractions(caseAggregate);
        verifyNoMoreInteractions(eventStream);
    }

    private void verifyAddOnlinePleaEventStreamContents(final OnlinePleaAllocationAdded pleaAllocationEvent) throws EventStreamException {
        final Stream<JsonEnvelope> envelopeStream = verifyAppendAndGetArgumentFrom(eventStream);

        assertThat(envelopeStream, streamContaining(
                jsonEnvelope(
                        metadata()
                                .withName(PROGRESSION_EVENT_ONLINE_PLEA_ALLOCATION_ADDED),
                        payload().isJson(allOf(
                                withJsonPath("$.caseId", is(pleaAllocationEvent.getCaseId().toString())),
                                withJsonPath("$.defendantId", is(pleaAllocationEvent.getDefendantId().toString())),
                                withJsonPath("$.hearingId", is(pleaAllocationEvent.getHearingId().toString())),
                                withJsonPath("$.offences[0].indicatedPlea", is(pleaAllocationEvent.getOffences().get(0).getIndicatedPlea())),
                                withJsonPath("$.offences[0].offenceId", is(pleaAllocationEvent.getOffences().get(0).getOffenceId().toString())),
                                withJsonPath("$.offences[0].pleaDate", is(pleaAllocationEvent.getOffences().get(0).getPleaDate().toString())))))
        ));
    }

    private void verifyUpdateOnlinePleaEventStreamContents(final OnlinePleaAllocationUpdated pleaAllocationEvent) throws EventStreamException {
        final Stream<JsonEnvelope> envelopeStream = verifyAppendAndGetArgumentFrom(eventStream);

        assertThat(envelopeStream, streamContaining(
                jsonEnvelope(
                        metadata()
                                .withName(PROGRESSION_EVENT_ONLINE_PLEA_ALLOCATION_UPDATED),
                        payload().isJson(allOf(
                                withJsonPath("$.caseId", is(pleaAllocationEvent.getCaseId().toString())),
                                withJsonPath("$.defendantId", is(pleaAllocationEvent.getDefendantId().toString())),
                                withJsonPath("$.hearingId", is(pleaAllocationEvent.getHearingId().toString())),
                                withJsonPath("$.offences[0].indicatedPlea", is(pleaAllocationEvent.getOffences().get(0).getIndicatedPlea())),
                                withJsonPath("$.offences[0].offenceId", is(pleaAllocationEvent.getOffences().get(0).getOffenceId().toString())),
                                withJsonPath("$.offences[0].pleaDate", is(pleaAllocationEvent.getOffences().get(0).getPleaDate().toString())))))
        ));
    }

    private OnlinePleaAllocationAdded getOnlinePleaAllocationAddedEvent(final PleasAllocationDetails pleasAllocation, final UUID hearingId) {
        return OnlinePleaAllocationAdded.onlinePleaAllocationAdded()
                .withCaseId(pleasAllocation.getCaseId())
                .withDefendantId(pleasAllocation.getDefendantId())
                .withHearingId(hearingId)
                .withOffences(pleasAllocation.getOffencePleas())
                .build();
    }

    private OnlinePleaAllocationUpdated getOnlinePleaAllocationUpdatedEvent(final PleasAllocationDetails pleasAllocation, final UUID hearingId) {
        return OnlinePleaAllocationUpdated.onlinePleaAllocationUpdated()
                .withCaseId(pleasAllocation.getCaseId())
                .withDefendantId(pleasAllocation.getDefendantId())
                .withHearingId(hearingId)
                .withOffences(pleasAllocation.getOffencePleas())
                .build();
    }

    private Envelope<AddOnlinePleaAllocation> getAddOnlinePleaAllocationEnvelope() {
        final AddOnlinePleaAllocation pleaAllocationAdded = AddOnlinePleaAllocation.addOnlinePleaAllocation()
                .withPleasAllocation(getPleasAllocationDetails())
                .build();

        final Metadata metadata = getMetadata(PROGRESSION_COMMAND_ADD_ONLINE_PLEA_ALLOCATION);

        return envelopeFrom(metadata, pleaAllocationAdded);
    }

    private Envelope<UpdateOnlinePleaAllocation> getUpdateOnlinePleaAllocationEnvelope() {
        final UpdateOnlinePleaAllocation pleaAllocationAdded = UpdateOnlinePleaAllocation.updateOnlinePleaAllocation()
                .withPleasAllocation(getPleasAllocationDetails())
                .build();

        final Metadata metadata = getMetadata(PROGRESSION_COMMAND_UPDATE_ONLINE_PLEA_ALLOCATION);

        return envelopeFrom(metadata, pleaAllocationAdded);
    }

    private PleasAllocationDetails getPleasAllocationDetails() {
        final OffencePleaDetails offence = OffencePleaDetails.offencePleaDetails()
                .withOffenceId(UUID.randomUUID())
                .withIndicatedPlea("GUILTY")
                .withPleaDate(LocalDate.now())
                .build();
        final List<OffencePleaDetails> offences = Collections.singletonList(offence);

        return PleasAllocationDetails.pleasAllocationDetails()
                .withCaseId(caseId)
                .withDefendantId(defendantId)
                .withOffencePleas(offences)
                .build();
    }

    private GenerateOpaNotice getGenerateOpaNotice() {
        return GenerateOpaNotice.generateOpaNotice()
                .withCaseId(caseId)
                .withDefendantId(defendantId)
                .withHearingId(firstHearingId)
                .withTriggerDate(LocalDate.now()).build();
    }

    private OpaNoticeSent getOpaNoticeSent() {
        return OpaNoticeSent.opaNoticeSent()
                .withHearingId(firstHearingId)
                .withDefendantId(defendantId)
                .withNotificationId(firstHearingId)
                .withTriggerDate(LocalDate.now()).build();
    }

    private JsonEnvelope getJsonEnvelope(final String event, final Map<String, String> values) {
        final JsonObjectBuilder builder = createObjectBuilder();
        values.forEach(builder::add);

        return new DefaultJsonEnvelopeProvider().envelopeFrom(getMetadata(event), builder.build());
    }

    private Envelope<GenerateOpaNotice> getEnvelope(final String event, final GenerateOpaNotice payload) {
        return  envelopeFrom(getMetadata(event), payload);
    }

    private Envelope<OpaNoticeSent> getEnvelope(final String event, final OpaNoticeSent opaNoticeSent) {
        return  envelopeFrom(getMetadata(event), opaNoticeSent);
    }
    private static Metadata getMetadata(final String command) {
        return Envelope
                .metadataBuilder()
                .withName(command)
                .withId(randomUUID())
                .build();
    }
}
