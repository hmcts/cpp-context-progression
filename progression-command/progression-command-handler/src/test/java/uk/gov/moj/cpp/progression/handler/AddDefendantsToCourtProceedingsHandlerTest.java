package uk.gov.moj.cpp.progression.handler;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static java.util.Collections.singletonList;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.core.annotation.Component.COMMAND_HANDLER;
import static uk.gov.justice.services.messaging.Envelope.envelopeFrom;
import static uk.gov.justice.services.test.utils.core.helper.EventStreamMockHelper.verifyAppendAndGetArgumentFrom;
import static uk.gov.justice.services.test.utils.core.matchers.HandlerMatcher.isHandler;
import static uk.gov.justice.services.test.utils.core.matchers.HandlerMethodMatcher.method;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeMatcher.jsonEnvelope;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeMetadataMatcher.metadata;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeStreamMatcher.streamContaining;

import uk.gov.justice.core.courts.AddDefendantsToCourtProceedings;
import uk.gov.justice.core.courts.CourtCentre;
import uk.gov.justice.core.courts.Defendant;
import uk.gov.justice.core.courts.DefendantsAddedToCourtProceedings;
import uk.gov.justice.core.courts.HearingType;
import uk.gov.justice.core.courts.JurisdictionType;
import uk.gov.justice.core.courts.ListDefendantRequest;
import uk.gov.justice.core.courts.ListHearingRequest;
import uk.gov.justice.core.courts.Offence;
import uk.gov.justice.core.courts.PersonDefendant;
import uk.gov.justice.core.courts.ProsecutionCase;
import uk.gov.justice.core.courts.ProsecutionCaseCreated;
import uk.gov.justice.core.courts.ProsecutionCaseIdentifier;
import uk.gov.justice.core.courts.ReferralReason;
import uk.gov.justice.services.core.aggregate.AggregateService;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.eventsourcing.source.core.EventSource;
import uk.gov.justice.services.eventsourcing.source.core.EventStream;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.messaging.Metadata;
import uk.gov.justice.services.test.utils.core.enveloper.EnveloperFactory;
import uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopePayloadMatcher;
import uk.gov.moj.cpp.progression.aggregate.CaseAggregate;
import uk.gov.moj.cpp.progression.service.MatchedDefendantLoadService;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class AddDefendantsToCourtProceedingsHandlerTest {

    @Mock
    private EventSource eventSource;

    @Mock
    private EventStream eventStream;

    @Mock
    private AggregateService aggregateService;

    @Mock
    private MatchedDefendantLoadService matchedDefendantLoadService;

    @Spy
    private Enveloper enveloper = EnveloperFactory.createEnveloperWithEvents(DefendantsAddedToCourtProceedings.class);

    @InjectMocks
    private AddDefendantsToCourtProceedingsHandler addDefendantsToCourtProceedingsHandler;

    private CaseAggregate aggregate;

    @Before
    public void setup() {
        aggregate = new CaseAggregate();
        when(eventSource.getStreamById(any())).thenReturn(eventStream);
        when(aggregateService.get(eventStream, CaseAggregate.class)).thenReturn(aggregate);
    }

    @Test
    public void shouldHandleCommand() {
        assertThat(new AddDefendantsToCourtProceedingsHandler(), isHandler(COMMAND_HANDLER)
                .with(method("handle")
                        .thatHandles("progression.command.add-defendants-to-court-proceedings")
                ));
    }

    @Test
    public void shouldProcessCommandDefendantAdded() throws Exception {

        final Defendant defendant =
                Defendant.defendant().withPersonDefendant(PersonDefendant.personDefendant().build())
                        .withProsecutionCaseId(UUID.randomUUID())
                        .withId(UUID.randomUUID())
                        .withOffences(singletonList(Offence.offence().build()))
                        .build();
        ReferralReason referralReason = ReferralReason.referralReason()
                .withId(UUID.randomUUID())
                .withDefendantId(defendant.getId())
                .withDescription("Dodged TFL tickets with passion")
                .build();

        final ListDefendantRequest listDefendantRequest = ListDefendantRequest.listDefendantRequest()
                .withProsecutionCaseId(defendant.getProsecutionCaseId())
                .withDefendantOffences(singletonList(UUID.randomUUID()))
                .withReferralReason(referralReason)
                .build();

        final HearingType hearingType = HearingType.hearingType().withId(UUID.randomUUID()).withDescription("TO_JAIL").build();
        final CourtCentre courtCentre = CourtCentre.courtCentre().withId(UUID.randomUUID()).build();

        final ListHearingRequest listHearingRequest = ListHearingRequest.listHearingRequest()
                .withCourtCentre(courtCentre).withHearingType(hearingType)
                .withJurisdictionType(JurisdictionType.MAGISTRATES)
                .withListDefendantRequests(Arrays.asList(listDefendantRequest))
                .build();

        AddDefendantsToCourtProceedings addDefendantsToCourtProceedings = AddDefendantsToCourtProceedings.addDefendantsToCourtProceedings()
                .withDefendants(singletonList(defendant))
                .withListHearingRequests(singletonList(listHearingRequest))
                .build();

        final Metadata metadata = Envelope
                .metadataBuilder()
                .withName("progression.command.add-defendants-to-court-proceedings")
                .withId(UUID.randomUUID())
                .build();

        final Envelope<AddDefendantsToCourtProceedings> envelope = envelopeFrom(metadata, addDefendantsToCourtProceedings);

        aggregate.apply(new ProsecutionCaseCreated(getProsecutionCase(), null));
        addDefendantsToCourtProceedingsHandler.handle(envelope);

        final Stream<JsonEnvelope> envelopeStream = verifyAppendAndGetArgumentFrom(eventStream);

        assertThat(envelopeStream, streamContaining(
                jsonEnvelope(
                        metadata()
                                .withName("progression.event.defendants-added-to-court-proceedings"),
                        JsonEnvelopePayloadMatcher.payload().isJson(allOf(
                                withJsonPath("$.defendants", notNullValue()))
                        ).isJson(allOf(
                                withJsonPath("$.listHearingRequests", notNullValue()))
                        )

                )
        ));

        verify(matchedDefendantLoadService).aggregateDefendantsSearchResultForAProsecutionCase(any(),any());
    }

    private ProsecutionCase getProsecutionCase() {
        final List<Defendant> defendants = new ArrayList<>();
        defendants.add(Defendant.defendant().withOffences(singletonList(Offence.offence().build())).build());
        return ProsecutionCase.prosecutionCase()
                .withProsecutionCaseIdentifier(ProsecutionCaseIdentifier.prosecutionCaseIdentifier()
                        .build())
                .withDefendants(defendants)
                .build();
    }
}
