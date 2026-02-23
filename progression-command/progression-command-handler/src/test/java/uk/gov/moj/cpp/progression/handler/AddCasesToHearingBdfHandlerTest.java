package uk.gov.moj.cpp.progression.handler;

import static java.util.UUID.randomUUID;
import static javax.json.Json.createObjectBuilder;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.messaging.Envelope.envelopeFrom;
import static uk.gov.justice.services.test.utils.core.helper.EventStreamMockHelper.verifyAppendAndGetArgumentFrom;

import uk.gov.justice.core.courts.CourtApplication;
import uk.gov.justice.core.courts.Defendant;
import uk.gov.justice.core.courts.Hearing;
import uk.gov.justice.core.courts.HearingInitiateEnriched;
import uk.gov.justice.core.courts.Offence;
import uk.gov.justice.core.courts.ProsecutionCase;
import uk.gov.justice.progression.courts.RemoveDuplicateApplicationBdf;
import uk.gov.justice.progression.courts.application.AddCaseToHearingBdf;
import uk.gov.justice.progression.courts.application.CasesBdf;
import uk.gov.justice.progression.courts.application.DefendantsBdf;
import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.common.converter.ObjectToJsonObjectConverter;
import uk.gov.justice.services.common.converter.jackson.ObjectMapperProducer;
import uk.gov.justice.services.core.aggregate.AggregateService;
import uk.gov.justice.services.eventsourcing.source.core.EventSource;
import uk.gov.justice.services.eventsourcing.source.core.EventStream;
import uk.gov.justice.services.eventsourcing.source.core.exception.EventStreamException;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.messaging.Metadata;
import uk.gov.moj.cpp.progression.aggregate.HearingAggregate;
import uk.gov.moj.cpp.progression.service.ProsecutionCaseQueryService;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;

import javax.json.JsonObject;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AddCasesToHearingBdfHandlerTest {

    @Mock
    private EventSource eventSource;

    @Mock
    private EventStream eventStream;

    @Mock
    private AggregateService aggregateService;

    @Mock
    private HearingAggregate hearingAggregate;

    @Mock
    ProsecutionCaseQueryService prosecutionCaseQueryService;

    @InjectMocks
    private AddCasesToHearingBdfHandler addCasesToHearingBdfHandler;

    private ObjectMapper mapper = new ObjectMapperProducer().objectMapper();

    @Spy
    private final JsonObjectToObjectConverter jsonToObjectConverter = new JsonObjectToObjectConverter(mapper);



    @BeforeEach
    public void setup() {
        when(eventSource.getStreamById(any())).thenReturn(eventStream);
        when(aggregateService.get(eventStream, HearingAggregate.class)).thenReturn(hearingAggregate);
    }


    @Test
    void shouldNotAddCaseIfTheCaseNotInViewStore() throws EventStreamException {
        final UUID caseId = randomUUID();
        final AddCaseToHearingBdf addCaseToHearingBdf = createAddCaseToHearingBdf(Map.of(caseId, asList(Map.of(randomUUID(), asList(randomUUID()))) ));

        final Metadata metadata = Envelope
                .metadataBuilder()
                .withName("progression.command.add-case-to-hearing-bdf")
                .withId(randomUUID())
                .build();

        when(prosecutionCaseQueryService.getProsecutionCase(any(), eq(caseId.toString()))).thenReturn(Optional.empty());

        final Envelope<AddCaseToHearingBdf> envelope = envelopeFrom(metadata, addCaseToHearingBdf);
        addCasesToHearingBdfHandler.handleAddCaseToHearing(envelope);

        verify(hearingAggregate, never()).addCasesToHearingBdf(any(), any());

    }

    @Test
    void shouldNotAddCaseIfTheDefendantNotInViewStore() throws EventStreamException {
        final UUID caseId = randomUUID();
        final AddCaseToHearingBdf addCaseToHearingBdf = createAddCaseToHearingBdf(Map.of(caseId, asList(Map.of(randomUUID(), asList(randomUUID()))) ));

        final Metadata metadata = Envelope
                .metadataBuilder()
                .withName("progression.command.add-case-to-hearing-bdf")
                .withId(randomUUID())
                .build();

        when(prosecutionCaseQueryService.getProsecutionCase(any(), eq(caseId.toString()))).thenReturn(createProsecutionCase(caseId, asList(Map.of(randomUUID(), asList(randomUUID())))));

        final Envelope<AddCaseToHearingBdf> envelope = envelopeFrom(metadata, addCaseToHearingBdf);
        addCasesToHearingBdfHandler.handleAddCaseToHearing(envelope);

        verify(hearingAggregate, never()).addCasesToHearingBdf(any(), any());

    }

    @Test
    void shouldNotAddCaseIfTheOffenceNotInViewStore() throws EventStreamException {
        final UUID caseId = randomUUID();
        final UUID defendant = randomUUID();
        final AddCaseToHearingBdf addCaseToHearingBdf = createAddCaseToHearingBdf(Map.of(caseId, asList(Map.of(defendant, asList(randomUUID()))) ));

        final Metadata metadata = Envelope
                .metadataBuilder()
                .withName("progression.command.add-case-to-hearing-bdf")
                .withId(randomUUID())
                .build();

        when(prosecutionCaseQueryService.getProsecutionCase(any(), eq(caseId.toString()))).thenReturn(createProsecutionCase(caseId, asList(Map.of(defendant, asList(randomUUID())))));

        final Envelope<AddCaseToHearingBdf> envelope = envelopeFrom(metadata, addCaseToHearingBdf);
        addCasesToHearingBdfHandler.handleAddCaseToHearing(envelope);

        verify(hearingAggregate, never()).addCasesToHearingBdf(any(), any());

    }

    @Test
    void shouldAddWholeCaseIfTheCaseNotInTheHearing() throws EventStreamException {
        final UUID caseId = randomUUID();
        final UUID defendantId = randomUUID();
        final UUID offenceId = randomUUID();
        final AddCaseToHearingBdf addCaseToHearingBdf =createAddCaseToHearingBdf(Map.of(caseId, asList(Map.of(defendantId, asList(offenceId))) ));

        final Metadata metadata = Envelope
                .metadataBuilder()
                .withName("progression.command.add-case-to-hearing-bdf")
                .withId(randomUUID())
                .build();

        when(prosecutionCaseQueryService.getProsecutionCase(any(), eq(caseId.toString()))).thenReturn(createProsecutionCase(caseId, asList(Map.of(defendantId, asList(offenceId)))));

        final Envelope<AddCaseToHearingBdf> envelope = envelopeFrom(metadata, addCaseToHearingBdf);
        addCasesToHearingBdfHandler.handleAddCaseToHearing(envelope);

        ArgumentCaptor<List<ProsecutionCase>> argumentCaptor = ArgumentCaptor.forClass(List.class);

        verify(hearingAggregate).addCasesToHearingBdf(any(), argumentCaptor.capture());

        assertThat(argumentCaptor.getValue().size(), is(1));
        assertThat(argumentCaptor.getValue().get(0).getId(), is(caseId));
        assertThat(argumentCaptor.getValue().get(0).getDefendants().size(), is(1));
        assertThat(argumentCaptor.getValue().get(0).getDefendants().get(0).getId(), is(defendantId));
        assertThat(argumentCaptor.getValue().get(0).getDefendants().get(0).getOffences().size(), is(1));
        assertThat(argumentCaptor.getValue().get(0).getDefendants().get(0).getOffences().get(0).getId(), is(offenceId));
    }

    @Test
    void shouldAddWholeMultipleCaseIfTheCasesNotInTheHearing() throws EventStreamException {
        final UUID caseId = randomUUID();
        final UUID defendantId = randomUUID();
        final UUID offenceId = randomUUID();
        final UUID caseId2 = randomUUID();
        final UUID defendantId2 = randomUUID();
        final UUID offenceId2 = randomUUID();
        final AddCaseToHearingBdf addCaseToHearingBdf =createAddCaseToHearingBdf(Map.of(caseId, asList(Map.of(defendantId, asList(offenceId))),
                caseId2, asList(Map.of(defendantId2, asList(offenceId2)))));

        final Metadata metadata = Envelope
                .metadataBuilder()
                .withName("progression.command.add-case-to-hearing-bdf")
                .withId(randomUUID())
                .build();

        when(prosecutionCaseQueryService.getProsecutionCase(any(), eq(caseId.toString()))).thenReturn(createProsecutionCase(caseId, asList(Map.of(defendantId, asList(offenceId)))));
        when(prosecutionCaseQueryService.getProsecutionCase(any(), eq(caseId2.toString()))).thenReturn(createProsecutionCase(caseId2, asList(Map.of(defendantId2, asList(offenceId2)))));

        final Envelope<AddCaseToHearingBdf> envelope = envelopeFrom(metadata, addCaseToHearingBdf);
        addCasesToHearingBdfHandler.handleAddCaseToHearing(envelope);

        ArgumentCaptor<List<ProsecutionCase>> argumentCaptor = ArgumentCaptor.forClass(List.class);

        verify(hearingAggregate).addCasesToHearingBdf(any(), argumentCaptor.capture());

        assertThat(argumentCaptor.getValue().size(), is(2));
        assertThat(argumentCaptor.getValue().get(0).getId(), is(addCaseToHearingBdf.getCasesBdf().get(0).getCaseId()));
        assertThat(argumentCaptor.getValue().get(0).getDefendants().size(), is(1));
        assertThat(argumentCaptor.getValue().get(0).getDefendants().get(0).getId(), is(addCaseToHearingBdf.getCasesBdf().get(0).getDefendantsBdf().get(0).getDefendantId()));
        assertThat(argumentCaptor.getValue().get(0).getDefendants().get(0).getOffences().size(), is(1));
        assertThat(argumentCaptor.getValue().get(0).getDefendants().get(0).getOffences().get(0).getId(), is(addCaseToHearingBdf.getCasesBdf().get(0).getDefendantsBdf().get(0).getOffences().get(0)));


        assertThat(argumentCaptor.getValue().get(1).getId(), is(addCaseToHearingBdf.getCasesBdf().get(1).getCaseId()));
        assertThat(argumentCaptor.getValue().get(1).getDefendants().size(), is(1));
        assertThat(argumentCaptor.getValue().get(1).getDefendants().get(0).getId(), is(addCaseToHearingBdf.getCasesBdf().get(1).getDefendantsBdf().get(0).getDefendantId()));
        assertThat(argumentCaptor.getValue().get(1).getDefendants().get(0).getOffences().size(), is(1));
        assertThat(argumentCaptor.getValue().get(1).getDefendants().get(0).getOffences().get(0).getId(), is(addCaseToHearingBdf.getCasesBdf().get(1).getDefendantsBdf().get(0).getOffences().get(0)));

    }

    @Test
    void shouldAddOnlySelectedDefendantsOfTheCase() throws EventStreamException {
        final UUID caseId = randomUUID();
        final UUID defendantId = randomUUID();
        final UUID offenceId = randomUUID();
        final UUID defendantId2 = randomUUID();
        final UUID offenceId2 = randomUUID();
        final AddCaseToHearingBdf addCaseToHearingBdf =createAddCaseToHearingBdf(Map.of(caseId, asList(Map.of(defendantId, asList(offenceId))) ));

        final Metadata metadata = Envelope
                .metadataBuilder()
                .withName("progression.command.add-case-to-hearing-bdf")
                .withId(randomUUID())
                .build();

        when(prosecutionCaseQueryService.getProsecutionCase(any(), eq(caseId.toString()))).thenReturn(createProsecutionCase(caseId, asList(Map.of(defendantId, asList(offenceId), defendantId2, asList(offenceId2)))));

        final Envelope<AddCaseToHearingBdf> envelope = envelopeFrom(metadata, addCaseToHearingBdf);
        addCasesToHearingBdfHandler.handleAddCaseToHearing(envelope);

        ArgumentCaptor<List<ProsecutionCase>> argumentCaptor = ArgumentCaptor.forClass(List.class);

        verify(hearingAggregate).addCasesToHearingBdf(any(), argumentCaptor.capture());

        assertThat(argumentCaptor.getValue().size(), is(1));
        assertThat(argumentCaptor.getValue().get(0).getId(), is(caseId));
        assertThat(argumentCaptor.getValue().get(0).getDefendants().size(), is(1));
        assertThat(argumentCaptor.getValue().get(0).getDefendants().get(0).getId(), is(defendantId));
        assertThat(argumentCaptor.getValue().get(0).getDefendants().get(0).getOffences().size(), is(1));
        assertThat(argumentCaptor.getValue().get(0).getDefendants().get(0).getOffences().get(0).getId(), is(offenceId));
    }

    @Test
    void shouldAddOnlySelectedDefendantsAndOffencesOfTheCase() throws EventStreamException {
        final UUID caseId = randomUUID();
        final UUID defendantId = randomUUID();
        final UUID offenceId = randomUUID();
        final UUID defendantId2 = randomUUID();
        final UUID offenceId2 = randomUUID();
        final UUID offenceId3 = randomUUID();
        final AddCaseToHearingBdf addCaseToHearingBdf =createAddCaseToHearingBdf(Map.of(caseId, asList(Map.of(defendantId, asList(offenceId))) ));

        final Metadata metadata = Envelope
                .metadataBuilder()
                .withName("progression.command.add-case-to-hearing-bdf")
                .withId(randomUUID())
                .build();

        when(prosecutionCaseQueryService.getProsecutionCase(any(), eq(caseId.toString()))).thenReturn(createProsecutionCase(caseId, asList(Map.of(defendantId, asList(offenceId, offenceId2), defendantId2, asList(offenceId3)))));

        final Envelope<AddCaseToHearingBdf> envelope = envelopeFrom(metadata, addCaseToHearingBdf);
        addCasesToHearingBdfHandler.handleAddCaseToHearing(envelope);

        ArgumentCaptor<List<ProsecutionCase>> argumentCaptor = ArgumentCaptor.forClass(List.class);

        verify(hearingAggregate).addCasesToHearingBdf(any(), argumentCaptor.capture());

        assertThat(argumentCaptor.getValue().size(), is(1));
        assertThat(argumentCaptor.getValue().get(0).getId(), is(caseId));
        assertThat(argumentCaptor.getValue().get(0).getDefendants().size(), is(1));
        assertThat(argumentCaptor.getValue().get(0).getDefendants().get(0).getId(), is(defendantId));
        assertThat(argumentCaptor.getValue().get(0).getDefendants().get(0).getOffences().size(), is(1));
        assertThat(argumentCaptor.getValue().get(0).getDefendants().get(0).getOffences().get(0).getId(), is(offenceId));
    }

//    @Test
//    void shouldRemoveDuplicateCourtApplication() throws EventStreamException {
//        final UUID hearingId = randomUUID();
//        final RemoveDuplicateApplicationBdf removeDuplicateApplicationBdf = createAddCaseToHearingBdf(Map.of(caseId, asList(Map.of(randomUUID(), asList(randomUUID()))) ));
//
//        final Metadata metadata = Envelope
//                .metadataBuilder()
//                .withName("progression.command.remove-duplicate-application-bdf")
//                .withId(randomUUID())
//                .build();
//
//        when(prosecutionCaseQueryService.getProsecutionCase(any(), eq(caseId.toString()))).thenReturn(Optional.empty());
//
//        final Envelope<AddCaseToHearingBdf> envelope = envelopeFrom(metadata, addCaseToHearingBdf);
//        hearingDataFixByBdfHandler.handleAddCaseToHearing(envelope);
//
//        verify(hearingAggregate, never()).addCasesToHearingBdf(any(), any());
//
//    }

//    @Test
//    public void shouldRemoveDuplicateApplicationsByBdf() throws EventStreamException {
//        final UUID hearingId = UUID.randomUUID();
//
//        final Metadata metadata = Envelope
//                .metadataBuilder()
//                .withName("progression.command.handler.remove-duplicate-application-bdf")
//                .withId(randomUUID())
//                .build();
//
//        final Envelope<RemoveDuplicateApplicationBdf> envelope = envelopeFrom(metadata, RemoveDuplicateApplicationBdf.removeDuplicateApplicationBdf()
//                .withHearingId(hearingId)
//                .build());
//        when(eventSource.getStreamById(any())).thenReturn(eventStream);
//        when(aggregateService.get(eventStream, HearingAggregate.class)).thenReturn(hearingAggregate);
//        when(hearingAggregate.removeDuplicateApplicationByBdf())
//                .thenReturn(Stream.of(HearingRemoveDuplicateApplicationBdf.hearingRemoveDuplicateApplicationBdf()
//                        .withHearing(getHearing(hearingId))
//                        .build()));
//
//        addCasesToHearingBdfHandler.removeDuplicateApplication(envelope);
//
//
//        verify(hearingAggregate).removeDuplicateApplicationByBdf();
//
//
//        final List<JsonEnvelope> events = verifyAppendAndGetArgumentFrom(eventStream).collect(Collectors.toList());
//
//        assertThat(events.size(), is(1));
//        assertThat(events.get(0).metadata().name(), is("progression.event-hearing-remove-duplicate-application-bdf"));
//        System.out.println("AAA");
//
//    }
//
    @Test
    public void shouldRemoveDuplicateApplicationsByBdf2() throws EventStreamException {
        final UUID hearingId = UUID.randomUUID();
        hearingAggregate = new HearingAggregate();
        when(eventSource.getStreamById(any())).thenReturn(eventStream);
        when(aggregateService.get(eventStream, HearingAggregate.class)).thenReturn(hearingAggregate);
        final Hearing hearing = getHearing(hearingId);

        // Set the hearing
        hearingAggregate.apply(HearingInitiateEnriched.hearingInitiateEnriched()
                .withHearing(hearing)
                .build());

        //Meta data
        final Metadata metadata = Envelope
                .metadataBuilder()
                .withName("progression.command.handler.remove-duplicate-application-bdf")
                .withId(randomUUID())
                .build();

        final Envelope<RemoveDuplicateApplicationBdf> envelope = envelopeFrom(metadata, RemoveDuplicateApplicationBdf.removeDuplicateApplicationBdf()
                .withHearingId(hearingId)
                .build());

        addCasesToHearingBdfHandler.removeDuplicateApplication(envelope);

        final Stream<JsonEnvelope> envelopeStream = verifyAppendAndGetArgumentFrom(eventStream);
        Optional<JsonEnvelope> hearingDeletedEnvelope = envelopeStream
                .filter(jsonEnvelope -> jsonEnvelope.metadata().name().equals("progression.event-hearing-remove-duplicate-application-bdf"))
                .findAny();

        assertTrue(hearingDeletedEnvelope.isPresent());
        System.out.println("AAA");

    }


    private Hearing getHearing(final UUID hearingId) {
        final List<ProsecutionCase> prosecutionCases = Arrays.asList(ProsecutionCase.prosecutionCase()
                .withId(UUID.randomUUID()).build());
        final List<CourtApplication> courtApplications = Arrays.asList(CourtApplication.courtApplication()
                .withId(UUID.randomUUID()).build());

        final Hearing hearing = Hearing.hearing()
                .withId(hearingId)
                .withProsecutionCases(prosecutionCases)
                .withCourtApplications(courtApplications)
                .build();
        return hearing;
    }

    private Optional<JsonObject> createProsecutionCase(final UUID caseId, final List<Map<UUID, List<UUID>>> defendants) {
        ProsecutionCase prosecutionCase = ProsecutionCase.prosecutionCase()
                .withId(caseId)
                .withDefendants(defendants.stream().flatMap(v -> v.entrySet().stream())
                        .map(defendant -> Defendant.defendant()
                                .withId(defendant.getKey())
                                .withOffences(defendant.getValue().stream().map(off -> Offence.offence()
                                                .withId(off)
                                                .build())
                                        .toList())
                                .build())
                        .toList())
                .build();

       final ObjectToJsonObjectConverter objectToJsonObjectConverter = new ObjectToJsonObjectConverter(mapper);

        return Optional.of(createObjectBuilder().add("prosecutionCase", objectToJsonObjectConverter.convert(prosecutionCase)).build());
    }

    private AddCaseToHearingBdf createAddCaseToHearingBdf(final Map<UUID,List<Map<UUID, List<UUID>>>> cases){
        return AddCaseToHearingBdf.addCaseToHearingBdf()
                .withHearingId(randomUUID())
                .withCasesBdf(cases.entrySet().stream().map(pCase -> CasesBdf.casesBdf()
                                .withCaseId(pCase.getKey())
                                .withDefendantsBdf(pCase.getValue().stream().flatMap(v -> v.entrySet().stream())
                                        .map(defendant ->DefendantsBdf.defendantsBdf()
                                                .withDefendantId(defendant.getKey())
                                                .withOffences(defendant.getValue())
                                                .build())
                                        .toList())
                        .build())
                        .toList())
                .build();
    }

    private <T> List<T> asList(T... a) {
        return new ArrayList<>(java.util.Arrays.asList(a));
    }
}
