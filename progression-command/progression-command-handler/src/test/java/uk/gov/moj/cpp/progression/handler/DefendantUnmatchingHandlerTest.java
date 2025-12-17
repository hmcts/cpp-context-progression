package uk.gov.moj.cpp.progression.handler;

import static java.util.Collections.singletonList;
import static java.util.UUID.randomUUID;
import static org.hamcrest.Matchers.notNullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static uk.gov.justice.core.courts.Offence.offence;
import static uk.gov.justice.core.courts.ProsecutionCase.prosecutionCase;
import static uk.gov.justice.services.messaging.Envelope.envelopeFrom;
import static uk.gov.justice.services.test.utils.core.helper.EventStreamMockHelper.verifyAppendAndGetArgumentFrom;

import uk.gov.justice.core.courts.Defendant;
import uk.gov.justice.core.courts.InitiationCode;
import uk.gov.justice.core.courts.PersonDefendant;
import uk.gov.justice.core.courts.ProsecutionCase;
import uk.gov.justice.core.courts.ProsecutionCaseCreated;
import uk.gov.justice.core.courts.ProsecutionCaseIdentifier;
import uk.gov.justice.core.courts.UnmatchDefendant;
import uk.gov.justice.core.courts.UnmatchDefendants;
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
import uk.gov.moj.cpp.progression.events.DefendantUnmatched;

import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
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
public class DefendantUnmatchingHandlerTest {

    @InjectMocks
    private DefendantUnmatchingHandler defendantUnmatchingHandler;

    @Mock
    private EventSource eventSource;

    @Mock
    private EventStream eventStream;

    @Mock
    private AggregateService aggregateService;

    @Mock
    private CaseAggregate caseAggregate;

    @Spy
    private final Enveloper enveloper = EnveloperFactory.createEnveloperWithEvents(
            DefendantUnmatched.class);

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

    @BeforeEach
    public void setup() {
        caseAggregate = new CaseAggregate();
        when(eventSource.getStreamById(any())).thenReturn(eventStream);
        when(aggregateService.get(eventStream, CaseAggregate.class)).thenReturn(caseAggregate);
    }

    @Test
    public void shouldHandleEditCaseNote() throws EventStreamException {
        final List<UnmatchDefendants> unmatchDefendants = new ArrayList<>();
        unmatchDefendants.add(UnmatchDefendants.unmatchDefendants()
                .withDefendantId(randomUUID())
                .withProsecutionCaseId(prosecutionCase.getId())
        .build());

        caseAggregate.apply(new ProsecutionCaseCreated(prosecutionCase, null));
        final UnmatchDefendant unmatchDefendant = UnmatchDefendant.unmatchDefendant()
                .withProsecutionCaseId(prosecutionCase.getId())
                .withDefendantId(defendant.getId())
                .withUnmatchDefendants(unmatchDefendants)
                .build();
        final Metadata metadata = Envelope
                .metadataBuilder()
                .withName("progression.command.unmatch-defendant")
                .withId(randomUUID())
                .build();

        final Envelope<UnmatchDefendant> envelope = envelopeFrom(metadata, unmatchDefendant);
        defendantUnmatchingHandler.handle(envelope);

        final Stream<JsonEnvelope> envelopeStream = verifyAppendAndGetArgumentFrom(eventStream);

        final List<Envelope> envelopes = envelopeStream.map(value -> (Envelope) value).collect(Collectors.toList());
        final JsonEnvelope  defendantPartialMatchCreatedEnvelope = (JsonEnvelope)envelopes.stream().filter(
                env -> env.metadata().name().equals("progression.event.defendant-unmatched")).findFirst().get();

        MatcherAssert.assertThat(defendantPartialMatchCreatedEnvelope.payloadAsJsonObject()
                , notNullValue());
    }
}
