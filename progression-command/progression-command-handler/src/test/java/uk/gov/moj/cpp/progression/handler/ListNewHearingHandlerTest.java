package uk.gov.moj.cpp.progression.handler;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static java.util.Arrays.asList;
import static java.util.UUID.randomUUID;
import static javax.json.Json.createObjectBuilder;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.test.utils.core.helper.EventStreamMockHelper.verifyAppendAndGetArgumentFrom;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeMatcher.jsonEnvelope;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeMetadataMatcher.metadata;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeStreamMatcher.streamContaining;

import uk.gov.justice.core.courts.CourtCentre;
import uk.gov.justice.core.courts.CourtHearingRequest;
import uk.gov.justice.core.courts.HearingType;
import uk.gov.justice.core.courts.InitiationCode;
import uk.gov.justice.core.courts.JurisdictionType;
import uk.gov.justice.core.courts.ListDefendantRequest;
import uk.gov.justice.core.courts.ListHearingRequested;
import uk.gov.justice.core.courts.ListNewHearing;
import uk.gov.justice.core.courts.Offence;
import uk.gov.justice.core.courts.PersonDefendant;
import uk.gov.justice.core.courts.ProsecutionCase;
import uk.gov.justice.core.courts.ProsecutionCaseCreated;
import uk.gov.justice.core.courts.ProsecutionCaseIdentifier;
import uk.gov.justice.core.courts.ReferralReason;
import uk.gov.justice.progression.courts.RelatedCaseRequestedForAdhocHearing;
import uk.gov.justice.progression.courts.RelatedHearingRequestedForAdhocHearing;
import uk.gov.justice.progression.courts.RequestRelatedHearingForAdhocHearing;
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
import uk.gov.justice.services.messaging.Metadata;
import uk.gov.justice.services.test.utils.core.enveloper.EnveloperFactory;
import uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopePayloadMatcher;
import uk.gov.moj.cpp.progression.aggregate.CaseAggregate;
import uk.gov.moj.cpp.progression.aggregate.HearingAggregate;
import uk.gov.moj.cpp.progression.service.ProsecutionCaseQueryService;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;

import javax.json.JsonObject;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class ListNewHearingHandlerTest {

    private static final UUID CASE_ID = randomUUID();
    private static final UUID MULTI_OFFENCE_DEFENDANT_ID = randomUUID();
    private static final UUID SAME_DEFENDANT_OFFENCE_ID_1 = randomUUID();
    private static final UUID SAME_DEFENDANT_OFFENCE_ID_2 = randomUUID();

    @Mock
    private EventSource eventSource;

    @Mock
    private EventStream eventStream;

    @Mock
    private AggregateService aggregateService;

    @Mock
    ProsecutionCaseQueryService prosecutionCaseQueryService;

    @InjectMocks
    private ListNewHearingHandler listNewHearingHandler;

    @Spy
    private final ObjectMapper objectMapper = new ObjectMapperProducer().objectMapper();

    @Spy
    private final JsonObjectToObjectConverter jsonObjectToObjectConverter = new JsonObjectToObjectConverter(new ObjectMapperProducer().objectMapper());

    @Spy
    private final ObjectToJsonObjectConverter objectToJsonObjectConverter = new ObjectToJsonObjectConverter(new ObjectMapperProducer().objectMapper());

    private HearingAggregate aggregate;

    private CaseAggregate caseAggregate;

    @Spy
    private final Enveloper enveloper = EnveloperFactory.createEnveloperWithEvents(ListHearingRequested.class, RelatedHearingRequestedForAdhocHearing.class, RelatedCaseRequestedForAdhocHearing.class);

    static final List<Offence> offences = new ArrayList<Offence>() {{
        add(Offence.offence().withId(SAME_DEFENDANT_OFFENCE_ID_1).build());
        add(Offence.offence().withId(SAME_DEFENDANT_OFFENCE_ID_2).build());
    }};

    private static final uk.gov.justice.core.courts.Defendant multiOffenceDefendant = uk.gov.justice.core.courts.Defendant.defendant().withId(MULTI_OFFENCE_DEFENDANT_ID)
            .withOffences(offences)
            .withProsecutionCaseId(CASE_ID)
            .withPersonDefendant(PersonDefendant.personDefendant().build()).build();

    @BeforeEach
    public void setup() {
        aggregate = new HearingAggregate();
        caseAggregate = new CaseAggregate();

    }

    @Test
    public void shouldNotRaiseEventWhenCaseDoesNotExist() throws EventStreamException {
        final Envelope<ListNewHearing> envelope = Envelope.envelopeFrom(getProgressionCommandHandlerListNewHearing("progression.command.handler.receive-representationOrder-for-defendant"), receivePayloadOfListNewHearing(randomUUID(), randomUUID(), false, null));

        when(prosecutionCaseQueryService.getProsecutionCase(any(JsonEnvelope.class),any(String.class))).thenReturn(Optional.empty());

        listNewHearingHandler.handle(envelope);

        verify(eventSource, never()).getStreamById(any());
    }

    @Test
    public void shouldNotRaiseEventWhenDefendantNotExist() throws EventStreamException {
        final Envelope<ListNewHearing> envelope = Envelope.envelopeFrom(getProgressionCommandHandlerListNewHearing("progression.command.handler.receive-representationOrder-for-defendant"), receivePayloadOfListNewHearing(CASE_ID, randomUUID(), false, null));
        final ProsecutionCase prosecutionCase = getProsecutionCaseWithMultiOffence();
        final JsonObject prosecutionCaseJson = objectToJsonObjectConverter.convert(prosecutionCase);

        when(prosecutionCaseQueryService.getProsecutionCase(any(JsonEnvelope.class),any(String.class))).thenReturn(Optional.of(createObjectBuilder().add("prosecutionCase", prosecutionCaseJson).build()));

        listNewHearingHandler.handle(envelope);

        verify(eventSource, never()).getStreamById(any());
    }

    @Test
    public void shouldRaiseEventWhenOneCaseOneDefendant() throws EventStreamException {
        final Envelope<ListNewHearing> envelope = Envelope.envelopeFrom(getProgressionCommandHandlerListNewHearing("progression.command.handler.receive-representationOrder-for-defendant"), receivePayloadOfListNewHearing(CASE_ID, MULTI_OFFENCE_DEFENDANT_ID, false, null));
        final ProsecutionCase prosecutionCase = getProsecutionCaseWithMultiOffence();
        final JsonObject prosecutionCaseJson = objectToJsonObjectConverter.convert(prosecutionCase);

        final HearingAggregate hearingAggregate = new HearingAggregate();
        when(eventSource.getStreamById(any())).thenReturn(eventStream);
        when(aggregateService.get(eventStream, HearingAggregate.class)).thenReturn(hearingAggregate);

        when(prosecutionCaseQueryService.getProsecutionCase(any(JsonEnvelope.class),any(String.class))).thenReturn(Optional.of(createObjectBuilder().add("prosecutionCase", prosecutionCaseJson).build()));

        listNewHearingHandler.handle(envelope);

        final Stream<JsonEnvelope> envelopeStream = verifyAppendAndGetArgumentFrom(eventStream);
        Optional<JsonEnvelope>  events = envelopeStream.filter
                (a->a.metadata().name().equals("progression.event.list-hearing-requested") )
                .findAny();

        assertTrue(events.isPresent());
        assertEquals("true", events.get().payloadAsJsonObject().get("sendNotificationToParties").toString());
        assertThat( events.get().payloadAsJsonObject().getBoolean("sendNotificationToParties"), is(true));

    }

    @Test
    public void shouldRaiseEventWhenOneCaseOneDefendantWithReferralReason() throws EventStreamException {
        final Envelope<ListNewHearing> envelope = Envelope.envelopeFrom(getProgressionCommandHandlerListNewHearing("progression.command.handler.receive-representationOrder-for-defendant"), receivePayloadOfListNewHearing(CASE_ID, MULTI_OFFENCE_DEFENDANT_ID, true, null));
        final ProsecutionCase prosecutionCase = getProsecutionCaseWithMultiOffence();
        final JsonObject prosecutionCaseJson = objectToJsonObjectConverter.convert(prosecutionCase);

        final HearingAggregate hearingAggregate = new HearingAggregate();
        when(eventSource.getStreamById(any())).thenReturn(eventStream);
        when(aggregateService.get(eventStream, HearingAggregate.class)).thenReturn(hearingAggregate);

        when(prosecutionCaseQueryService.getProsecutionCase(any(JsonEnvelope.class),any(String.class))).thenReturn(Optional.of(createObjectBuilder().add("prosecutionCase", prosecutionCaseJson).build()));

        listNewHearingHandler.handle(envelope);

        final Stream<JsonEnvelope> envelopeStream = verifyAppendAndGetArgumentFrom(eventStream);
        Optional<JsonEnvelope>  events = envelopeStream.filter
                (a->a.metadata().name().equals("progression.event.list-hearing-requested") )
                .findAny();

        assertTrue(events.isPresent());
    }

    @Test
    public void shouldRaiseEventWhenSelectedExistingHearing() throws EventStreamException {
        final UUID hearingId = randomUUID();
        final Envelope<ListNewHearing> envelope = Envelope.envelopeFrom(getProgressionCommandHandlerListNewHearing("progression.command.handler.receive-representationOrder-for-defendant"), receivePayloadOfListNewHearing(CASE_ID, MULTI_OFFENCE_DEFENDANT_ID, false, hearingId));
        final ProsecutionCase prosecutionCase = getProsecutionCaseWithMultiOffence();
        final JsonObject prosecutionCaseJson = objectToJsonObjectConverter.convert(prosecutionCase);
        when(prosecutionCaseQueryService.getProsecutionCase(any(JsonEnvelope.class),any(String.class))).thenReturn(Optional.of(createObjectBuilder().add("prosecutionCase", prosecutionCaseJson).build()));

        caseAggregate.apply(ProsecutionCaseCreated.prosecutionCaseCreated()
                .withProsecutionCase(prosecutionCase)
                .build());
        when(eventSource.getStreamById(any())).thenReturn(eventStream);
        when(aggregateService.get(eventStream, CaseAggregate.class)).thenReturn(caseAggregate);
        listNewHearingHandler.handle(envelope);

        final Stream<JsonEnvelope> envelopeStream = verifyAppendAndGetArgumentFrom(eventStream);
        assertThat(envelopeStream, streamContaining(
                jsonEnvelope(
                        metadata()
                                .withName("progression.event.related-case-requested-for-adhoc-hearing"),
                        JsonEnvelopePayloadMatcher.payload().isJson(Matchers.allOf(
                                withJsonPath("$.prosecutionCase.defendants.length()", is(1)),
                                withJsonPath("$.prosecutionCase.defendants[0].offences.length()", is(2)),
                                withJsonPath("$.listNewHearing.id", is(hearingId.toString()))
                                )
                        ))));
    }


    @Test
    public void shouldRaiseHearingEventWhenSelectedExistingHearing() throws EventStreamException {
        final UUID hearingId = randomUUID();

        final Envelope<RequestRelatedHearingForAdhocHearing> envelope =
                Envelope.envelopeFrom(getProgressionCommandHandlerListNewHearing("progression.command.request-related-hearing-for-adhoc-hearing"),
                        RequestRelatedHearingForAdhocHearing.requestRelatedHearingForAdhocHearing()
                                .withListNewHearing(receivePayloadOfListNewHearing(CASE_ID, MULTI_OFFENCE_DEFENDANT_ID, false, hearingId).getListNewHearing())
                                .withProsecutionCase(getProsecutionCaseWithMultiOffence())
                                .withSendNotificationToParties(true)
                                .build());
        when(eventSource.getStreamById(any())).thenReturn(eventStream);
        when(aggregateService.get(eventStream, HearingAggregate.class)).thenReturn(aggregate);
        listNewHearingHandler.handleRequestRelatedHearingForAdhocHearing(envelope);

        final Stream<JsonEnvelope> envelopeStream = verifyAppendAndGetArgumentFrom(eventStream);
        assertThat(envelopeStream, streamContaining(
                jsonEnvelope(
                        metadata()
                                .withName("progression.event.related-hearing-requested-for-adhoc-hearing"),
                        JsonEnvelopePayloadMatcher.payload().isJson(Matchers.allOf(
                                withJsonPath("$.hearingRequest.prosecutionCases[0].defendants.length()", is(1)),
                                withJsonPath("$.hearingRequest.prosecutionCases[0].defendants[0].offences.length()", is(2)),
                                withJsonPath("$.sendNotificationToParties", is(true))
                                )
                        ))));
    }

    private static Metadata getProgressionCommandHandlerListNewHearing(final String command) {
        return Envelope
                .metadataBuilder()
                .withName(command)
                .withId(randomUUID())
                .build();
    }

    private ListNewHearing receivePayloadOfListNewHearing(final UUID caseId, final UUID defendantId, final boolean referralReasonExists, final UUID existingHearingId){
        return ListNewHearing.listNewHearing()
                .withListNewHearing(CourtHearingRequest.courtHearingRequest()
                        .withId(existingHearingId)
                        .withHearingType(HearingType.hearingType()
                                .withId(randomUUID())
                                .withDescription("typeDescription")
                                .build())
                        .withCourtCentre(CourtCentre.courtCentre()
                                .withId(randomUUID())
                                .withName("Court 1")
                                .build())
                        .withJurisdictionType(JurisdictionType.MAGISTRATES)
                        .withListDefendantRequests(asList(ListDefendantRequest.listDefendantRequest()
                                .withProsecutionCaseId(caseId)
                                .withDefendantOffences(asList(SAME_DEFENDANT_OFFENCE_ID_1, SAME_DEFENDANT_OFFENCE_ID_2))
                                .withDefendantId(referralReasonExists ? null : defendantId)
                                .withReferralReason(referralReasonExists ?  ReferralReason.referralReason()
                                        .withDefendantId(MULTI_OFFENCE_DEFENDANT_ID)
                                        .build() : null)
                                .build(), ListDefendantRequest.listDefendantRequest()
                                .withProsecutionCaseId(caseId)
                                .withDefendantOffences(asList(randomUUID(), randomUUID()))
                                .withDefendantId(randomUUID())
                                .build()))
                        .build())
                .withSendNotificationToParties(true)
                .build();
    }

    private ProsecutionCase getProsecutionCaseWithMultiOffence() {
        return ProsecutionCase.prosecutionCase()
                .withCaseStatus("caseStatus")
                .withId(CASE_ID)
                .withOriginatingOrganisation("originatingOrganisation")
                .withDefendants(Arrays.asList(multiOffenceDefendant))
                .withInitiationCode(InitiationCode.C)
                .withProsecutionCaseIdentifier(ProsecutionCaseIdentifier.prosecutionCaseIdentifier()
                        .withProsecutionAuthorityReference("reference")
                        .withProsecutionAuthorityCode("code")
                        .withProsecutionAuthorityId(randomUUID())
                        .build())
                .build();
    }
}
