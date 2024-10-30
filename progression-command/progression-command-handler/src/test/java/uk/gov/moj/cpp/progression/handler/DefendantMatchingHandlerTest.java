package uk.gov.moj.cpp.progression.handler;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static java.util.UUID.randomUUID;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static uk.gov.justice.core.courts.Offence.offence;
import static uk.gov.justice.core.courts.ProsecutionCase.prosecutionCase;
import static uk.gov.justice.services.core.annotation.Component.COMMAND_HANDLER;
import static uk.gov.justice.services.messaging.Envelope.envelopeFrom;
import static uk.gov.justice.services.test.utils.core.helper.EventStreamMockHelper.verifyAppendAndGetArgumentFrom;
import static uk.gov.justice.services.test.utils.core.matchers.HandlerMatcher.isHandler;
import static uk.gov.justice.services.test.utils.core.matchers.HandlerMethodMatcher.method;
import static uk.gov.justice.services.test.utils.core.reflection.ReflectionUtil.setField;

import uk.gov.justice.core.courts.Address;
import uk.gov.justice.core.courts.Cases;
import uk.gov.justice.core.courts.Defendant;
import uk.gov.justice.core.courts.DefendantPartialMatchCreated;
import uk.gov.justice.core.courts.Defendants;
import uk.gov.justice.core.courts.InitiationCode;
import uk.gov.justice.core.courts.MatchDefendant;
import uk.gov.justice.core.courts.MatchedDefendants;
import uk.gov.justice.core.courts.PartialMatchedDefendantSearchResultStored;
import uk.gov.justice.core.courts.PersonDefendant;
import uk.gov.justice.core.courts.ProcessMatchedDefendants;
import uk.gov.justice.core.courts.ProsecutionCase;
import uk.gov.justice.core.courts.ProsecutionCaseCreated;
import uk.gov.justice.core.courts.ProsecutionCaseIdentifier;
import uk.gov.justice.core.courts.UpdateMatchedDefendant;
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
import uk.gov.justice.services.test.utils.core.random.RandomGenerator;
import uk.gov.moj.cpp.progression.aggregate.CaseAggregate;
import uk.gov.moj.cpp.progression.events.DefendantMatched;
import uk.gov.moj.cpp.progression.events.DefendantsMasterDefendantIdUpdated;
import uk.gov.moj.cpp.progression.events.MasterDefendantIdUpdated;
import uk.gov.moj.cpp.progression.events.MasterDefendantIdUpdatedIntoHearings;
import uk.gov.moj.cpp.progression.events.MasterDefendantIdUpdatedV2;

import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.hamcrest.MatcherAssert;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class DefendantMatchingHandlerTest {

    @Mock
    private EventSource eventSource;

    @Mock
    private EventStream eventStream;

    @Mock
    private AggregateService aggregateService;

    @Spy
    private JsonObjectToObjectConverter jsonObjectToObjectConverter;

    @Spy
    private ObjectToJsonObjectConverter objectToJsonObjectConverter;

    @Spy
    private final Enveloper enveloper = EnveloperFactory.createEnveloperWithEvents(
            DefendantMatched.class, MasterDefendantIdUpdated.class,
            DefendantPartialMatchCreated.class, DefendantsMasterDefendantIdUpdated.class,
            MasterDefendantIdUpdatedV2.class, MasterDefendantIdUpdatedIntoHearings.class);

    @InjectMocks
    private DefendantMatchingHandler defendantMatchingHandler;

    private CaseAggregate caseAggregate;

    @BeforeEach
    public void setup() {
        setField(this.jsonObjectToObjectConverter, "objectMapper", new ObjectMapperProducer().objectMapper());
        setField(this.objectToJsonObjectConverter, "mapper", new ObjectMapperProducer().objectMapper());
        caseAggregate = new CaseAggregate();
    }

    @Test
    public void shouldHandleCommand() {
        assertThat(new DefendantMatchingHandler(), isHandler(COMMAND_HANDLER)
                .with(method("handle")
                        .thatHandles("progression.command.match-defendant")
                ));
    }

    @Test
    public void shouldStoreMatchedDefendants() throws EventStreamException {
        when(eventSource.getStreamById(any())).thenReturn(eventStream);
        when(aggregateService.get(eventStream, CaseAggregate.class)).thenReturn(caseAggregate);
        caseAggregate.apply(new ProsecutionCaseCreated(prosecutionCase, null));
        final UUID defendantId = prosecutionCase.getDefendants().get(0).getId();

        final String defendantName = RandomGenerator.STRING.next();
        final PartialMatchedDefendantSearchResultStored partialMatchedDefendantSearchResultStored = PartialMatchedDefendantSearchResultStored.partialMatchedDefendantSearchResultStored()
                .withDefendantId(defendantId)
                .withCases(asList(Cases.cases()
                        .withProsecutionCaseId(prosecutionCase.getId().toString())
                        .withDefendants(asList(Defendants.defendants()
                                .withDefendantId(defendantId.toString())
                                .withMasterDefendantId(defendantId.toString())
                                .withFirstName(defendantName)
                                .withLastName(defendantName)
                                .withAddress(Address.address()
                                        .withAddress1("Address1")
                                        .build())
                                .withCourtProceedingsInitiated(ZonedDateTime.now())
                                .build()))
                        .withCaseReference("REF")
                        .withProsecutionCaseId("caseId")
                        .build()))
                .build();

        this.caseAggregate.apply(partialMatchedDefendantSearchResultStored);

        final ProcessMatchedDefendants processMatchedDefendants = ProcessMatchedDefendants.processMatchedDefendants()
                .withProsecutionCaseId(prosecutionCase.getId())
                .build();
        final Metadata metadata = Envelope
                .metadataBuilder()
                .withName("progression.command.process-matched-defendants")
                .withId(randomUUID())
                .build();

        final Envelope<ProcessMatchedDefendants> envelope = envelopeFrom(metadata, processMatchedDefendants);
        defendantMatchingHandler.storeMatchedDefendants(envelope);
        final Stream<JsonEnvelope > envelopeStream = verifyAppendAndGetArgumentFrom(eventStream);

        final List<Envelope> envelopes = envelopeStream.map(value -> (Envelope) value).collect(Collectors.toList());
        final JsonEnvelope  defendantPartialMatchCreatedEnvelope = (JsonEnvelope)envelopes.stream().filter(
                env -> env.metadata().name().equals("progression.event.defendant-partial-match-created")).findFirst().get();

        MatcherAssert.assertThat(defendantPartialMatchCreatedEnvelope.payloadAsJsonObject()
                , notNullValue());
    }

    @Test
    public void shouldUpdateMatchedDefendant() throws EventStreamException {
        when(eventSource.getStreamById(any())).thenReturn(eventStream);
        when(aggregateService.get(eventStream, CaseAggregate.class)).thenReturn(caseAggregate);

        caseAggregate.apply(new ProsecutionCaseCreated(prosecutionCase, null));
        final UUID defendantId = prosecutionCase.getDefendants().get(0).getId();

        final UpdateMatchedDefendant updateMatchedDefendant = UpdateMatchedDefendant.updateMatchedDefendant()
                .withDefendantId(defendantId)
                .withMasterDefendantId(randomUUID())
                .withProsecutionCaseId(prosecutionCase.getId())
                .build();

        final Metadata metadata = Envelope
                .metadataBuilder()
                .withName("progression.command.process-matched-defendants")
                .withId(randomUUID())
                .build();

        final Envelope<UpdateMatchedDefendant> envelope = envelopeFrom(metadata, updateMatchedDefendant);
        defendantMatchingHandler.updateMatchedDefendant(envelope);

        final Stream<JsonEnvelope > envelopeStream = verifyAppendAndGetArgumentFrom(eventStream);

        final List<Envelope> envelopes = envelopeStream.map(value -> (Envelope) value).collect(Collectors.toList());
        final JsonEnvelope  defendantPartialMatchCreatedEnvelope = (JsonEnvelope)envelopes.stream().filter(
                env -> env.metadata().name().equals("progression.event.defendants-master-defendant-id-updated")).findFirst().get();

        MatcherAssert.assertThat(defendantPartialMatchCreatedEnvelope.payloadAsJsonObject()
                , notNullValue());
    }

    @Test
    public void shouldHandleMatchedDefendant() throws EventStreamException {
        when(eventSource.getStreamById(any())).thenReturn(eventStream);
        when(aggregateService.get(eventStream, CaseAggregate.class)).thenReturn(caseAggregate);

        caseAggregate.apply(new ProsecutionCaseCreated(prosecutionCase, null));
        final UUID defendantId = prosecutionCase.getDefendants().get(0).getId();

        final List<MatchedDefendants> matchedDefendants = new ArrayList<>();
        matchedDefendants.add(MatchedDefendants.matchedDefendants()
                .withDefendantId(defendantId)
                .withMasterDefendantId(randomUUID())
                .withCourtProceedingsInitiated(ZonedDateTime.now())
                .withProsecutionCaseId(prosecutionCase.getId())
                .build());

        final MatchDefendant matchDefendant = MatchDefendant.matchDefendant()
                .withDefendantId(defendantId)
                .withProsecutionCaseId(prosecutionCase.getId())
                .withMatchedDefendants(matchedDefendants)
                .build();

        final Metadata metadata = Envelope
                .metadataBuilder()
                .withName("progression.command.process-matched-defendants")
                .withId(randomUUID())
                .build();

        final Envelope<MatchDefendant> envelope = envelopeFrom(metadata, matchDefendant);
        defendantMatchingHandler.handle(envelope);

        final Stream<JsonEnvelope > envelopeStream = verifyAppendAndGetArgumentFrom(eventStream);

        final List<Envelope> envelopes = envelopeStream.map(value -> (Envelope) value).collect(Collectors.toList());
        final JsonEnvelope  defendantMatchEnvelope = (JsonEnvelope)envelopes.stream().filter(
                env -> env.metadata().name().equals("progression.event.defendant-matched")).findFirst().get();

        MatcherAssert.assertThat(defendantMatchEnvelope.payloadAsJsonObject()
                , notNullValue());
    }

    private static ProsecutionCase createProsecutionCase(final List<Defendant> defendants) {
        return prosecutionCase()
                .withCaseStatus("caseStatus")
                .withId(randomUUID())
                .withOriginatingOrganisation("originatingOrganisation")
                .withDefendants(defendants)
                .withInitiationCode(InitiationCode.C)
                .withProsecutionCaseIdentifier(ProsecutionCaseIdentifier.prosecutionCaseIdentifier()
                        .withProsecutionAuthorityReference("reference")
                        .withProsecutionAuthorityCode("code")
                        .withProsecutionAuthorityId(randomUUID())
                        .withCaseURN("caseUrn")
                        .build())
                .build();
    }

    private static final Defendant defendant = Defendant.defendant()
            .withId(randomUUID())
            .withMasterDefendantId(randomUUID())
            .withPersonDefendant(PersonDefendant.personDefendant()
                    .withPersonDetails(uk.gov.justice.core.courts.Person.person()
                            .withFirstName("firstName")
                            .withLastName("lastName")
                            .withDateOfBirth(LocalDate.now().minusYears(20))
                            .build())
                    .build())
            .withCourtProceedingsInitiated(ZonedDateTime.now())
            .withOffences(singletonList(offence().withId(randomUUID()).build()))
            .build();

    private static final List<Defendant> defendants = new ArrayList<Defendant>() {{
        add(defendant);
    }};

    private static final ProsecutionCase prosecutionCase = createProsecutionCase(defendants);
}
