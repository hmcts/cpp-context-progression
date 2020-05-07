package uk.gov.moj.cpp.progression.command;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static java.util.UUID.randomUUID;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.core.annotation.Component.COMMAND_HANDLER;
import static uk.gov.justice.services.messaging.Envelope.envelopeFrom;
import static uk.gov.justice.services.test.utils.core.matchers.EventStreamMatcher.eventStreamAppendedWith;
import static uk.gov.justice.services.test.utils.core.matchers.HandlerMatcher.isHandler;
import static uk.gov.justice.services.test.utils.core.matchers.HandlerMethodMatcher.method;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeMatcher.jsonEnvelope;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeMetadataMatcher.withMetadataEnvelopedFrom;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopePayloadMatcher.payloadIsJson;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeStreamMatcher.streamContaining;
import static uk.gov.moj.cpp.progression.command.helper.HandlerTestHelper.metadataFor;

import uk.gov.justice.core.courts.AssociatedDefenceOrganisation;
import uk.gov.justice.core.courts.Category;
import uk.gov.justice.core.courts.DefenceOrganisation;
import uk.gov.justice.core.courts.Defendant;
import uk.gov.justice.core.courts.FundingType;
import uk.gov.justice.core.courts.HearingResultedCaseUpdated;
import uk.gov.justice.core.courts.HearingResultedUpdateCase;
import uk.gov.justice.core.courts.JudicialResult;
import uk.gov.justice.core.courts.Offence;
import uk.gov.justice.core.courts.ProsecutionCase;
import uk.gov.justice.services.core.aggregate.AggregateService;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.eventsourcing.source.core.EventSource;
import uk.gov.justice.services.eventsourcing.source.core.EventStream;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.test.utils.core.enveloper.EnveloperFactory;
import uk.gov.moj.cpp.progression.aggregate.CaseAggregate;
import uk.gov.moj.cpp.progression.command.helper.FileResourceObjectMapper;
import uk.gov.moj.cpp.progression.domain.constant.CaseStatusEnum;
import uk.gov.moj.cpp.progression.handler.UpdateCaseHandler;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class UpdateCaseHandlerTest {

    @Mock
    private EventSource eventSource;

    @Mock
    private EventStream eventStream;

    @Mock
    private AggregateService aggregateService;

    @Spy
    private Enveloper enveloper = EnveloperFactory.createEnveloperWithEvents(HearingResultedUpdateCase.class, HearingResultedCaseUpdated.class);

    @InjectMocks
    private UpdateCaseHandler updateCaseHandler;

    private CaseAggregate aggregate;

    private final FileResourceObjectMapper handlerTestHelper = new FileResourceObjectMapper();


    @Before
    public void setup() {
        aggregate = new CaseAggregate();
        when(eventSource.getStreamById(any())).thenReturn(eventStream);
        when(aggregateService.get(eventStream, CaseAggregate.class)).thenReturn(aggregate);
    }

    @Test
    public void shouldHandleCommand() {
        assertThat(new UpdateCaseHandler(), isHandler(COMMAND_HANDLER)
                .with(method("handle")
                        .thatHandles("progression.command.hearing-resulted-update-case")
                ));
    }

    @Test
    public void shouldProcessHearingResultedUpdateCaseCommand_whenAtLeastOneofOffencesOfDefendantHaveFinalCategory_expectProceedingConcludedAsTrue() throws Exception {

        final Envelope<HearingResultedUpdateCase> envelope =
                envelopeFrom(metadataFor("progression.command.hearing-resulted-update-case",
                        randomUUID()),
                        handlerTestHelper.convertFromFile("json/hearing-resulted-update-case.json", HearingResultedUpdateCase.class));

        updateCaseHandler.handle(envelope);

        assertThat(eventStream, eventStreamAppendedWith(
                streamContaining(
                        jsonEnvelope(
                                withMetadataEnvelopedFrom(envelope)
                                        .withName("progression.event.hearing-resulted-case-updated"),
                                payloadIsJson(
                                        allOf(
                                                withJsonPath("$.prosecutionCase.defendants[0].proceedingsConcluded", is(true)),
                                                withJsonPath("$.prosecutionCase.defendants[0].offences[0].proceedingsConcluded", is(true)),
                                                withJsonPath("$.prosecutionCase.caseStatus", is(CaseStatusEnum.CLOSED.getDescription())),
                                                withJsonPath("$.prosecutionCase.defendants[0].associatedDefenceOrganisation.fundingType", notNullValue())

                                        )
                                ))
                )));


    }

    @Test
    public void shouldProcessHearingResultedUpdateCaseCommand_whenAllOffencesOfDefendantHaveNoFinalCategory_expectProceedingConcludedAsFalse() throws Exception {
        Offence offence1 = Offence.offence()
                .withId(randomUUID())
                .withJudicialResults(Arrays.asList(JudicialResult.judicialResult()
                        .withCategory(Category.INTERMEDIARY).build(), JudicialResult.judicialResult()
                        .withCategory(Category.ANCILLARY).build()))
                .build();
        Offence offence2 = Offence.offence()
                .withId(randomUUID())
                .withJudicialResults(Arrays.asList(JudicialResult.judicialResult()
                        .withCategory(Category.INTERMEDIARY).build(), JudicialResult.judicialResult()
                        .withCategory(Category.ANCILLARY).build()))
                .build();

        Defendant defendant = Defendant.defendant()
                .withId(randomUUID())
                .withAssociatedDefenceOrganisation(AssociatedDefenceOrganisation.associatedDefenceOrganisation()
                        .withFundingType(FundingType.REPRESENTATION_ORDER)
                        .withAssociationStartDate(LocalDate.now())
                        .withDefenceOrganisation(DefenceOrganisation.defenceOrganisation().build())
                        .build()
                )
                .withOffences(Arrays.asList(offence1, offence2))
                .build();
        List<Defendant> defendantList = new ArrayList<Defendant>();
        defendantList.add(defendant);
        final ProsecutionCase prosecutionCase = ProsecutionCase.prosecutionCase()
                .withId(randomUUID())
                .withCaseStatus(CaseStatusEnum.READY_FOR_REVIEW.getDescription())

                .withDefendants(defendantList).build();


        HearingResultedUpdateCase hearingResultedUpdateCase = HearingResultedUpdateCase.hearingResultedUpdateCase()
                .withProsecutionCase(prosecutionCase)
                .build();

        final Envelope<HearingResultedUpdateCase> envelope =
                envelopeFrom(metadataFor("progression.command.hearing-resulted-update-case",
                        randomUUID()),
                        hearingResultedUpdateCase);

        updateCaseHandler.handle(envelope);

        assertThat(eventStream, eventStreamAppendedWith(
                streamContaining(
                        jsonEnvelope(
                                withMetadataEnvelopedFrom(envelope)
                                        .withName("progression.event.hearing-resulted-case-updated"),
                                payloadIsJson(
                                        allOf(
                                                withJsonPath("$.prosecutionCase.defendants[0].proceedingsConcluded", is(false)),
                                                withJsonPath("$.prosecutionCase.defendants[0].offences[0].proceedingsConcluded", is(false)),
                                                withJsonPath("$.prosecutionCase.defendants[0].offences[1].proceedingsConcluded", is(false)),
                                                withJsonPath("$.prosecutionCase.caseStatus", is(CaseStatusEnum.READY_FOR_REVIEW.getDescription())),
                                                withJsonPath("$.prosecutionCase.defendants[0].associatedDefenceOrganisation.fundingType", notNullValue())


                                        )
                                ))
                )));


    }

    @Test
    public void shouldProcessHearingResultedUpdateCaseCommand_whenOneOfOffenceHasNoJudicialResults_expectProceedingConcludedAsFalse() throws Exception {
        Offence offence1 = Offence.offence()
                .withId(randomUUID())
                .withJudicialResults(Arrays.asList(JudicialResult.judicialResult()
                        .withCategory(Category.FINAL).build(), JudicialResult.judicialResult()
                        .withCategory(Category.ANCILLARY).build()))
                .build();
        Offence offence2 = Offence.offence()
                .withId(randomUUID())
                .build();

        Defendant defendant = Defendant.defendant()
                .withId(randomUUID())
                .withOffences(Arrays.asList(offence1, offence2))
                .withAssociatedDefenceOrganisation(AssociatedDefenceOrganisation.associatedDefenceOrganisation()
                        .withFundingType(FundingType.REPRESENTATION_ORDER)
                        .withAssociationStartDate(LocalDate.now())
                        .withDefenceOrganisation(DefenceOrganisation.defenceOrganisation().build())
                        .build()
                )
                .build();
        List<Defendant> defendantList = new ArrayList<Defendant>();
        defendantList.add(defendant);
        final ProsecutionCase prosecutionCase = ProsecutionCase.prosecutionCase()
                .withId(randomUUID())
                .withCaseStatus(CaseStatusEnum.READY_FOR_REVIEW.getDescription())

                .withDefendants(defendantList).build();


        HearingResultedUpdateCase hearingResultedUpdateCase = HearingResultedUpdateCase.hearingResultedUpdateCase()
                .withProsecutionCase(prosecutionCase)
                .build();

        final Envelope<HearingResultedUpdateCase> envelope =
                envelopeFrom(metadataFor("progression.command.hearing-resulted-update-case",
                        randomUUID()),
                        hearingResultedUpdateCase);

        updateCaseHandler.handle(envelope);

        assertThat(eventStream, eventStreamAppendedWith(
                streamContaining(
                        jsonEnvelope(
                                withMetadataEnvelopedFrom(envelope)
                                        .withName("progression.event.hearing-resulted-case-updated"),
                                payloadIsJson(
                                        allOf(
                                                withJsonPath("$.prosecutionCase.defendants[0].proceedingsConcluded", is(false)),
                                                withJsonPath("$.prosecutionCase.defendants[0].offences[0].proceedingsConcluded", is(true)),
                                                withJsonPath("$.prosecutionCase.caseStatus", is(CaseStatusEnum.READY_FOR_REVIEW.getDescription())),
                                                withJsonPath("$.prosecutionCase.defendants[0].associatedDefenceOrganisation.fundingType", notNullValue())


                                        )
                                ))
                )));


    }
}
