package uk.gov.moj.cpp.progression.handler;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static java.util.UUID.randomUUID;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.is;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.core.annotation.Component.COMMAND_HANDLER;
import static uk.gov.justice.services.messaging.Envelope.envelopeFrom;
import static uk.gov.justice.services.test.utils.core.helper.EventStreamMockHelper.verifyAppendAndGetArgumentFrom;
import static uk.gov.justice.services.test.utils.core.matchers.HandlerMatcher.isHandler;
import static uk.gov.justice.services.test.utils.core.matchers.HandlerMethodMatcher.method;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeMatcher.jsonEnvelope;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeMetadataMatcher.metadata;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeStreamMatcher.streamContaining;
import static uk.gov.justice.services.test.utils.core.reflection.ReflectionUtil.setField;
import static uk.gov.moj.cpp.progression.command.UpdateMatchedDefendantCustodialInformation.updateMatchedDefendantCustodialInformation;

import uk.gov.justice.core.courts.CaseDefendantUpdatedWithDriverNumber;
import uk.gov.justice.core.courts.Defendant;
import uk.gov.justice.core.courts.DefendantUpdate;
import uk.gov.justice.core.courts.Hearing;
import uk.gov.justice.core.courts.HearingDefendantUpdated;
import uk.gov.justice.core.courts.HearingListingStatus;
import uk.gov.justice.core.courts.Person;
import uk.gov.justice.core.courts.PersonDefendant;
import uk.gov.justice.core.courts.ProsecutionCase;
import uk.gov.justice.core.courts.ProsecutionCaseDefendantListingStatusChanged;
import uk.gov.justice.core.courts.ProsecutionCaseDefendantUpdated;
import uk.gov.justice.core.courts.ProsecutionCaseUpdateDefendantsWithMatchedRequested;
import uk.gov.justice.core.courts.ProsecutionCaseUpdateDefendantsWithMatchedRequestedV2;
import uk.gov.justice.core.courts.UpdateCaseDefendantWithDriverNumber;
import uk.gov.justice.core.courts.UpdateDefendantForHearing;
import uk.gov.justice.core.courts.UpdateDefendantForMatchedDefendant;
import uk.gov.justice.core.courts.UpdateDefendantForProsecutionCase;
import uk.gov.justice.core.courts.UpdateHearingWithNewDefendant;
import uk.gov.justice.cpp.progression.events.NewDefendantAddedToHearing;
import uk.gov.justice.progression.courts.HearingResulted;
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
import uk.gov.moj.cpp.progression.command.CustodialEstablishment;
import uk.gov.moj.cpp.progression.command.UpdateMatchedDefendantCustodialInformation;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.google.common.collect.Lists;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class UpdateDefendantHandlerTest {

    @Spy
    private final Enveloper enveloper = EnveloperFactory.createEnveloperWithEvents(
            ProsecutionCaseDefendantUpdated.class,
            ProsecutionCaseUpdateDefendantsWithMatchedRequested.class,
            ProsecutionCaseUpdateDefendantsWithMatchedRequestedV2.class,
            HearingDefendantUpdated.class,
            NewDefendantAddedToHearing.class,
            UpdateMatchedDefendantCustodialInformation.class,
            CaseDefendantUpdatedWithDriverNumber.class);
    @Mock
    private EventSource eventSource;
    @Mock
    private EventStream eventStream;
    @Mock
    private AggregateService aggregateService;
    @InjectMocks
    private UpdateDefendantHandler updateDefendantHandler;

    private CaseAggregate aggregate;
    private HearingAggregate hearingAggregate;

    @Before
    public void setup() {
        aggregate = new CaseAggregate();
        hearingAggregate = new HearingAggregate();
        when(eventSource.getStreamById(any())).thenReturn(eventStream);
        when(aggregateService.get(eventStream, CaseAggregate.class)).thenReturn(aggregate);
        when(aggregateService.get(eventStream, HearingAggregate.class)).thenReturn(hearingAggregate);
    }

    @Test
    public void shouldHandleCommand() {
        assertThat(new UpdateDefendantHandler(), isHandler(COMMAND_HANDLER)
                .with(method("handle")
                        .thatHandles("progression.command.update-defendant-for-prosecution-case")
                ));
    }

    @Test
    public void shouldHandleCommandUpdateMatchedDefendantCustodialInformation() throws Exception {

        final UUID caseId = randomUUID();
        final Map<UUID, Defendant> defendantsMap = new HashMap<>();
        final UUID defendantId1 = randomUUID();
        final UUID defendantId2 = randomUUID();
        final UUID masterDefendantId = randomUUID();
        final UUID custodialId = randomUUID();

        final Defendant defendant1 = Defendant.defendant()
                .withId(defendantId1)
                .withProsecutionCaseId(caseId)
                .withMasterDefendantId(masterDefendantId)
                .withPersonDefendant(PersonDefendant.personDefendant()
                        .withCustodialEstablishment(uk.gov.justice.core.courts.CustodialEstablishment.custodialEstablishment()
                                .withName("name1")
                                .withId(randomUUID())
                                .withCustody("custody1")
                                .build())
                        .build())
                .build();
        final Defendant defendant2 = Defendant.defendant()
                .withId(defendantId2)
                .withProsecutionCaseId(caseId)
                .withMasterDefendantId(masterDefendantId)
                .withPersonDefendant(PersonDefendant.personDefendant()
                        .withCustodialEstablishment(uk.gov.justice.core.courts.CustodialEstablishment.custodialEstablishment()
                                .withName("name2")
                                .withId(custodialId)
                                .withCustody("custody2")
                                .build())
                        .build())
                .build();
        defendantsMap.put(defendantId1, defendant1);
        defendantsMap.put(defendantId2, defendant2);
        setField(aggregate, "defendantsMap", defendantsMap);
        final UpdateMatchedDefendantCustodialInformation updateMatchedDefendantCustodialInformation = updateMatchedDefendantCustodialInformation()
                .withCaseId(caseId)
                .withCustodialEstablishment(CustodialEstablishment.custodialEstablishment()
                        .withCustody("custody")
                        .withId(randomUUID())
                        .withName("name")
                        .build())
                .withDefendants(Arrays.asList(defendantId1))
                .withMasterDefendantId(masterDefendantId)
                .build();

        final Metadata metadata = Envelope
                .metadataBuilder()
                .withName("progression.command.update-matched-defendant-custodial-information")
                .withId(randomUUID())
                .build();

        final Envelope<UpdateMatchedDefendantCustodialInformation> envelope = envelopeFrom(metadata, updateMatchedDefendantCustodialInformation);

        updateDefendantHandler.handleCommandUpdateMatchedDefendantCustodialInformation(envelope);

        final Stream<JsonEnvelope> envelopeStream = verifyAppendAndGetArgumentFrom(eventStream);

        assertThat(envelopeStream, streamContaining(
                jsonEnvelope(
                        metadata()
                                .withName("progression.event.prosecution-case-defendant-updated"),
                        JsonEnvelopePayloadMatcher.payload().isJson(allOf(
                                withJsonPath("$.defendant.id", is(defendantId1.toString()))
                                )
                        ))

                )
        );
    }

    @Test
    @SuppressWarnings("squid:S00112")
    public void shouldProcessCommand() throws Exception {

        UUID defendantId = randomUUID();
        final DefendantUpdate defendant =
                DefendantUpdate.defendantUpdate().withPersonDefendant(PersonDefendant.personDefendant().build())
                        .withProsecutionCaseId(randomUUID())
                        .withId(defendantId)
                        .build();
        UpdateDefendantForProsecutionCase updateDefendant = UpdateDefendantForProsecutionCase.updateDefendantForProsecutionCase().withDefendant(defendant).build();


        final Metadata metadata = Envelope
                .metadataBuilder()
                .withName("progression.command.update-defendant-for-prosecution-case")
                .withId(randomUUID())
                .build();

        final Envelope<UpdateDefendantForProsecutionCase> envelope = envelopeFrom(metadata, updateDefendant);

        updateDefendantHandler.handle(envelope);

        Stream<JsonEnvelope> envelopeStream = verifyAppendAndGetArgumentFrom(eventStream);

        assertThat(envelopeStream, streamContaining(
                jsonEnvelope(
                        metadata()
                                .withName("progression.event.prosecution-case-defendant-updated"),
                        JsonEnvelopePayloadMatcher.payload().isJson(allOf(
                                withJsonPath("$.defendant.id", is(defendantId.toString()))
                                )
                        ))

                )
        );
    }

    @Test
    @SuppressWarnings("squid:S00112")
    public void shouldKeepTitleWhenNewTitleDoesNotExist() throws Exception {

        UUID defendantId = randomUUID();

        UpdateDefendantForProsecutionCase updateDefendant = UpdateDefendantForProsecutionCase.updateDefendantForProsecutionCase()
                .withDefendant(getDefendantUpdate(defendantId, null))
                .build();

        aggregate.apply(ProsecutionCaseDefendantUpdated.prosecutionCaseDefendantUpdated()
                .withDefendant(getDefendantUpdate(defendantId, "Mr"))
                .build());

        final Metadata metadata = Envelope
                .metadataBuilder()
                .withName("progression.command.update-defendant-for-prosecution-case")
                .withId(randomUUID())
                .build();

        final Envelope<UpdateDefendantForProsecutionCase> envelope = envelopeFrom(metadata, updateDefendant);

        updateDefendantHandler.handle(envelope);

        Stream<JsonEnvelope> envelopeStream = verifyAppendAndGetArgumentFrom(eventStream);

        assertThat(envelopeStream, streamContaining(
                jsonEnvelope(
                        metadata()
                                .withName("progression.event.prosecution-case-defendant-updated"),
                        JsonEnvelopePayloadMatcher.payload().isJson(allOf(
                                withJsonPath("$.defendant.id", is(defendantId.toString())),
                                withJsonPath("$.defendant.personDefendant.personDetails.title", is("Mr")),
                                withJsonPath("$.defendant.personDefendant.personDetails.firstName", is("FirstName"))
                                )
                        ))

                )
        );
    }

    @Test
    @SuppressWarnings("squid:S00112")
    public void shouldChangeTitleWhenNewTitleExists() throws Exception {

        UUID defendantId = randomUUID();

        UpdateDefendantForProsecutionCase updateDefendant = UpdateDefendantForProsecutionCase.updateDefendantForProsecutionCase()
                .withDefendant(getDefendantUpdate(defendantId, "Baron"))
                .build();

        aggregate.apply(ProsecutionCaseDefendantUpdated.prosecutionCaseDefendantUpdated()
                .withDefendant(getDefendantUpdate(defendantId, "Mr"))
                .build());

        final Metadata metadata = Envelope
                .metadataBuilder()
                .withName("progression.command.update-defendant-for-prosecution-case")
                .withId(randomUUID())
                .build();

        final Envelope<UpdateDefendantForProsecutionCase> envelope = envelopeFrom(metadata, updateDefendant);

        updateDefendantHandler.handle(envelope);

        Stream<JsonEnvelope> envelopeStream = verifyAppendAndGetArgumentFrom(eventStream);

        assertThat(envelopeStream, streamContaining(
                jsonEnvelope(
                        metadata()
                                .withName("progression.event.prosecution-case-defendant-updated"),
                        JsonEnvelopePayloadMatcher.payload().isJson(allOf(
                                withJsonPath("$.defendant.id", is(defendantId.toString())),
                                withJsonPath("$.defendant.personDefendant.personDetails.title", is("Baron")),
                                withJsonPath("$.defendant.personDefendant.personDetails.firstName", is("FirstName"))
                                )
                        ))

                )
        );
    }

    @Test
    @SuppressWarnings("squid:S00112")
    public void shouldProcessCommandWithMatchedDefendants() throws Exception {
        final UUID masterDefendantId = randomUUID();
        final UUID defendantId = randomUUID();
        final UUID defendant2Id = randomUUID();
        final UUID matchedDefendantHearingId = randomUUID();
        final DefendantUpdate defendant =
                DefendantUpdate.defendantUpdate().withPersonDefendant(PersonDefendant.personDefendant().build())
                        .withProsecutionCaseId(randomUUID())
                        .withId(defendantId)
                        .withMasterDefendantId(masterDefendantId)
                        .build();

        final UpdateDefendantForMatchedDefendant updateDefendant = UpdateDefendantForMatchedDefendant.updateDefendantForMatchedDefendant()
                .withDefendant(defendant)
                .withMatchedDefendantHearingId(matchedDefendantHearingId)
                .build();

        final Metadata metadata = Envelope
                .metadataBuilder()
                .withName("progression.command.update-defendant-for-matched-defendant")
                .withId(randomUUID())
                .build();

        final Envelope<UpdateDefendantForMatchedDefendant> envelope = envelopeFrom(metadata, updateDefendant);


        hearingAggregate.apply(HearingResulted.hearingResulted()
                .withHearing(Hearing.hearing()
                        .withProsecutionCases(Arrays.asList(ProsecutionCase.prosecutionCase()
                                        .withDefendants(Arrays.asList(Defendant.defendant()
                                                .withId(defendantId)
                                                .withMasterDefendantId(masterDefendantId)
                                                .build()))
                                        .build(),
                                ProsecutionCase.prosecutionCase()
                                        .withDefendants(Arrays.asList(Defendant.defendant()
                                                .withId(defendant2Id)
                                                .withMasterDefendantId(masterDefendantId)
                                                .build()))
                                        .build()))
                        .build())
                .build());

        updateDefendantHandler.handleUpdateDefendantForMatchedDefendant(envelope);

        Stream<JsonEnvelope> envelopeStream = verifyAppendAndGetArgumentFrom(eventStream);

        assertThat(envelopeStream, streamContaining(
                jsonEnvelope(
                        metadata()
                                .withName("progression.event.prosecution-case-update-defendants-with-matched-requested-v2"),
                        JsonEnvelopePayloadMatcher.payload().isJson(allOf(
                                withJsonPath("$.defendant.id", is(defendantId.toString())),
                                withJsonPath("$.matchedDefendants[0].id", is(defendant2Id.toString()))
                                )
                        ))

                )
        );
    }

    @Test
    public void shouldHandleUpdateDefendantForHearing() throws EventStreamException {
        final UUID defendantId = randomUUID();
        final UUID hearingId = randomUUID();
        final UUID prosecutionCaseId = randomUUID();
        final UUID masterDefendantId = randomUUID();
        final DefendantUpdate defendant =
                DefendantUpdate.defendantUpdate().withPersonDefendant(PersonDefendant.personDefendant().build())
                        .withProsecutionCaseId(prosecutionCaseId)
                        .withId(defendantId)
                        .withMasterDefendantId(masterDefendantId)
                        .build();

        final UpdateDefendantForHearing updateDefendant = UpdateDefendantForHearing.updateDefendantForHearing()
                .withDefendant(defendant)
                .withHearingId(hearingId)
                .build();

        final Metadata metadata = Envelope
                .metadataBuilder()
                .withName("progression.command.update-defendant-for-hearing")
                .withId(randomUUID())
                .build();

        hearingAggregate.apply(HearingResulted.hearingResulted()
                .withHearing(Hearing.hearing()
                        .withProsecutionCases(Arrays.asList(ProsecutionCase.prosecutionCase()
                                .withDefendants(new ArrayList<>(Arrays.asList(Defendant.defendant()
                                        .withId(defendantId)
                                        .withMasterDefendantId(masterDefendantId)
                                        .build())))
                                .build()))
                        .build())
                .build());


        final Envelope<UpdateDefendantForHearing> envelope = envelopeFrom(metadata, updateDefendant);
        updateDefendantHandler.handleUpdateDefendantForHearing(envelope);

        final Stream<JsonEnvelope> envelopeStream = verifyAppendAndGetArgumentFrom(eventStream);

        assertThat(envelopeStream, streamContaining(
                jsonEnvelope(
                        metadata()
                                .withName("progression.event.hearing-defendant-updated"),
                        JsonEnvelopePayloadMatcher.payload().isJson(allOf(
                                withJsonPath("$.defendant.id", is(defendantId.toString())),
                                withJsonPath("$.hearingId", is(hearingId.toString()))
                                )
                        ))

                )
        );
    }

    @Test
    public void shouldHNotUpdateDefendantForResultedHearing() throws EventStreamException {
        final UUID defendantId = randomUUID();
        final UUID hearingId = randomUUID();
        final UUID prosecutionCaseId = randomUUID();
        final UUID masterDefendantId = randomUUID();
        final DefendantUpdate defendant =
                DefendantUpdate.defendantUpdate().withPersonDefendant(PersonDefendant.personDefendant().build())
                        .withProsecutionCaseId(prosecutionCaseId)
                        .withId(defendantId)
                        .withMasterDefendantId(masterDefendantId)
                        .build();

        final UpdateDefendantForHearing updateDefendant = UpdateDefendantForHearing.updateDefendantForHearing()
                .withDefendant(defendant)
                .withHearingId(hearingId)
                .withUpdateOnlyNonResulted(true)
                .build();

        final Metadata metadata = Envelope
                .metadataBuilder()
                .withName("progression.command.update-defendant-for-hearing")
                .withId(randomUUID())
                .build();

        final Hearing hearing = Hearing.hearing()
                .withProsecutionCases(Arrays.asList(ProsecutionCase.prosecutionCase()
                        .withDefendants(new ArrayList<>(Arrays.asList(Defendant.defendant()
                                .withId(defendantId)
                                .withMasterDefendantId(masterDefendantId)
                                .build())))
                        .build()))
                .build();
        hearingAggregate.apply(HearingResulted.hearingResulted()
                .withHearing(hearing)
                .build());

        hearingAggregate.apply(ProsecutionCaseDefendantListingStatusChanged.prosecutionCaseDefendantListingStatusChanged()
                .withHearing(hearing)
                .withHearingListingStatus(HearingListingStatus.HEARING_RESULTED)
                .build());

        final Envelope<UpdateDefendantForHearing> envelope = envelopeFrom(metadata, updateDefendant);
        updateDefendantHandler.handleUpdateDefendantForHearing(envelope);

        final Stream<JsonEnvelope> envelopeStream = verifyAppendAndGetArgumentFrom(eventStream);

        assertThat(envelopeStream.collect(Collectors.toList()).isEmpty(), is(true));
    }

    @Test
    public void shouldHandleAddDefendantForHearing() throws EventStreamException {
        final UUID defendantId = randomUUID();
        final UUID hearingId = randomUUID();
        final UUID prosecutionCaseId = randomUUID();
        final UUID masterDefendantId = randomUUID();
        final DefendantUpdate defendant =
                DefendantUpdate.defendantUpdate().withPersonDefendant(PersonDefendant.personDefendant().build())
                        .withProsecutionCaseId(prosecutionCaseId)
                        .withId(defendantId)
                        .withMasterDefendantId(masterDefendantId)
                        .build();

        final UpdateHearingWithNewDefendant updateDefendant = UpdateHearingWithNewDefendant.updateHearingWithNewDefendant()
                .withDefendants(Lists.newArrayList(Defendant.defendant().withId(masterDefendantId).build()))
                .withProsecutionCaseId(prosecutionCaseId)
                .withHearingId(hearingId)
                .build();

        final Metadata metadata = Envelope
                .metadataBuilder()
                .withName("progression.command.update-hearing-with-new-defendant")
                .withId(randomUUID())
                .build();

        hearingAggregate.apply(HearingResulted.hearingResulted()
                .withHearing(Hearing.hearing()
                        .withProsecutionCases(Arrays.asList(ProsecutionCase.prosecutionCase()
                                        .withId(prosecutionCaseId)
                                .withDefendants(new ArrayList<>(Arrays.asList(Defendant.defendant()
                                        .withId(defendantId)
                                        .withMasterDefendantId(masterDefendantId)
                                        .build())))
                                .build()))
                        .build())
                .build());


        final Envelope<UpdateHearingWithNewDefendant> envelope = envelopeFrom(metadata, updateDefendant);
        updateDefendantHandler.handleUpdateHearingWithNewDefendant(envelope);

        final Stream<JsonEnvelope> envelopeStream = verifyAppendAndGetArgumentFrom(eventStream);

        assertThat(envelopeStream, streamContaining(
                jsonEnvelope(
                        metadata()
                                .withName("progression.event.new-defendant-added-to-hearing"),
                        JsonEnvelopePayloadMatcher.payload().isJson(allOf(
                                withJsonPath("$.defendants[0].id", is(masterDefendantId.toString())),
                                withJsonPath("$.hearingId", is(hearingId.toString()))
                                )
                        ))

                )
        );
    }

    @Test
    @SuppressWarnings("squid:S00112")
    public void shouldUpdatedriverNumber() throws Exception {

        UUID defendantId = randomUUID();

        UpdateCaseDefendantWithDriverNumber updateDefendant = UpdateCaseDefendantWithDriverNumber.updateCaseDefendantWithDriverNumber()
                .withDefendantId(defendantId)
                .withProsecutionCaseId(randomUUID())
                .withDriverNumber("DVL1234")
                .build();

        aggregate.apply(ProsecutionCaseDefendantUpdated.prosecutionCaseDefendantUpdated()
                .withDefendant(getDefendantUpdate(defendantId, "Mr"))
                .build());

        final Metadata metadata = Envelope
                .metadataBuilder()
                .withName("progression.update-case-defendant-with-driver-number")
                .withId(randomUUID())
                .build();

        final Envelope<UpdateCaseDefendantWithDriverNumber> envelope = envelopeFrom(metadata, updateDefendant);

        updateDefendantHandler.handlerUpdateDefendantWithDriverNumber(envelope);

        List<JsonEnvelope> envelopeStream = verifyAppendAndGetArgumentFrom(eventStream).collect(Collectors.toList());

        assertThat(Stream.of(envelopeStream.get(0)), streamContaining(
                jsonEnvelope(
                        metadata()
                                .withName("progression.event.prosecution-case-defendant-updated"),
                        JsonEnvelopePayloadMatcher.payload().isJson(allOf(
                                withJsonPath("$.defendant.id", is(defendantId.toString())),
                                withJsonPath("$.defendant.personDefendant.personDetails.title", is("Mr")),
                                withJsonPath("$.defendant.personDefendant.personDetails.firstName", is("FirstName")),
                                withJsonPath("$.defendant.personDefendant.driverNumber", is("DVL1234"))
                                )
                        ))

                )
        );

        assertThat(Stream.of(envelopeStream.get(1)), streamContaining(
                jsonEnvelope(
                        metadata()
                                .withName("progression.event.case-defendant-updated-with-driver-number"),
                        JsonEnvelopePayloadMatcher.payload().isJson(allOf(
                                withJsonPath("$.defendantId", is(defendantId.toString())),
                                withJsonPath("$.driverNumber", is("DVL1234"))
                                )
                        ))

                )
        );
    }

    private DefendantUpdate getDefendantUpdate(final UUID defendantId, final String title){
        return DefendantUpdate.defendantUpdate().withPersonDefendant(PersonDefendant.personDefendant().build())
                        .withProsecutionCaseId(randomUUID())
                        .withId(defendantId)
                        .withPersonDefendant(PersonDefendant.personDefendant()
                                .withPersonDetails(Person.person()
                                        .withFirstName("FirstName")
                                        .withTitle(title)
                                        .build())
                                .build())
                        .build();

    }
}
