package uk.gov.moj.cpp.progression.handler;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static java.util.Collections.singletonList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.codehaus.groovy.runtime.InvokerHelper.asList;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
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
import static uk.gov.moj.cpp.progression.test.FileUtil.getPayload;

import uk.gov.justice.core.courts.AddDefendantsToCourtProceedings;
import uk.gov.justice.core.courts.CourtCentre;
import uk.gov.justice.core.courts.Defendant;
import uk.gov.justice.core.courts.DefendantsAddedToCourtProceedings;
import uk.gov.justice.core.courts.HearingType;
import uk.gov.justice.core.courts.JurisdictionType;
import uk.gov.justice.core.courts.ListDefendantRequest;
import uk.gov.justice.core.courts.ListHearingRequest;
import uk.gov.justice.core.courts.Offence;
import uk.gov.justice.core.courts.Person;
import uk.gov.justice.core.courts.PersonDefendant;
import uk.gov.justice.core.courts.ProsecutionCase;
import uk.gov.justice.core.courts.ProsecutionCaseCreated;
import uk.gov.justice.core.courts.ProsecutionCaseIdentifier;
import uk.gov.justice.core.courts.ReferralReason;
import uk.gov.justice.services.core.aggregate.AggregateService;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.core.requester.Requester;
import uk.gov.justice.services.eventsourcing.source.core.EventSource;
import uk.gov.justice.services.eventsourcing.source.core.EventStream;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.messaging.Metadata;
import uk.gov.justice.services.test.utils.core.enveloper.EnveloperFactory;
import uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopePayloadMatcher;
import uk.gov.moj.cpp.progression.aggregate.CaseAggregate;
import uk.gov.moj.cpp.progression.service.MatchedDefendantLoadService;

import java.io.StringReader;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.moj.cpp.progression.service.ReferenceDataOffenceService;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonReader;

@ExtendWith(MockitoExtension.class)
public class AddDefendantsToCourtProceedingsHandlerTest {

    @Mock
    private EventSource eventSource;

    @Mock
    private EventStream eventStream;

    @Mock
    private AggregateService aggregateService;

    @Mock
    private MatchedDefendantLoadService matchedDefendantLoadService;

    @Mock
    private ReferenceDataOffenceService referenceDataOffenceService;

    @Mock
    private Requester requester;

    @Spy
    private Enveloper enveloper = EnveloperFactory.createEnveloperWithEvents(DefendantsAddedToCourtProceedings.class);

    @InjectMocks
    private AddDefendantsToCourtProceedingsHandler addDefendantsToCourtProceedingsHandler;

    private static final String SEXUAL_OFFENCE_RR_DESCRIPTION = "Complainant's anonymity protected by virtue of Section 1 of the Sexual Offences Amendment Act 1992";
    private static final String YOUTH_RESTRICTION = "Section 49 of the Children and Young Persons Act 1933 applies";

    @Test
    public void shouldHandleCommand() {
        assertThat(new AddDefendantsToCourtProceedingsHandler(), isHandler(COMMAND_HANDLER)
                .with(method("handle")
                        .thatHandles("progression.command.add-defendants-to-court-proceedings")
                ));
    }

    @Test
    public void shouldProcessCommandDefendantAdded() throws Exception {
        final UUID offenceId = UUID.randomUUID();

        final Defendant defendant =
                Defendant.defendant().withPersonDefendant(PersonDefendant.personDefendant()
                        .withPersonDetails(Person.person()
                                .withDateOfBirth(ZonedDateTime.now().minusYears(16).toLocalDate())
                                .build())
                        .build())
                        .withProsecutionCaseId(UUID.randomUUID())
                        .withId(UUID.randomUUID())
                        .withOffences(new ArrayList<>(asList(Offence.offence().withOffenceCode("offenceCode").build())))
                        .build();
        ReferralReason referralReason = ReferralReason.referralReason()
                .withId(UUID.randomUUID())
                .withDefendantId(defendant.getId())
                .withDescription("Dodged TFL tickets with passion")
                .build();

        final ListDefendantRequest listDefendantRequest = ListDefendantRequest.listDefendantRequest()
                .withProsecutionCaseId(defendant.getProsecutionCaseId())
                .withDefendantOffences(new ArrayList<>(asList(UUID.randomUUID())))
                .withReferralReason(referralReason)
                .build();

        final HearingType hearingType = HearingType.hearingType().withId(UUID.randomUUID()).withDescription("TO_JAIL").build();
        final CourtCentre courtCentre = CourtCentre.courtCentre().withId(UUID.randomUUID()).build();

        final ListHearingRequest listHearingRequest = ListHearingRequest.listHearingRequest()
                .withCourtCentre(courtCentre).withHearingType(hearingType)
                .withJurisdictionType(JurisdictionType.MAGISTRATES)
                .withListDefendantRequests(Arrays.asList(listDefendantRequest))
                .withListedStartDateTime(ZonedDateTime.now())
                .build();

        AddDefendantsToCourtProceedings addDefendantsToCourtProceedings = AddDefendantsToCourtProceedings.addDefendantsToCourtProceedings()
                .withDefendants(singletonList(defendant))
                .withListHearingRequests(new ArrayList<>(asList(listHearingRequest)))
                .build();

        final Metadata metadata = Envelope
                .metadataBuilder()
                .withName("progression.command.add-defendants-to-court-proceedings")
                .withId(UUID.randomUUID())
                .build();



        final List<JsonObject> referencedataOffencesJsonObject = prepareReferenceDataOffencesJsonObject(offenceId, "offenceCode",
                SEXUAL_OFFENCE_RR_DESCRIPTION,
                "json/referencedataoffences.offences-list.json");
        when(referenceDataOffenceService.getMultipleOffencesByOffenceCodeList(anyList(), any(), eq(requester), any())).thenReturn(Optional.of(referencedataOffencesJsonObject));


        final Envelope<AddDefendantsToCourtProceedings> envelope = envelopeFrom(metadata, addDefendantsToCourtProceedings);

        final CaseAggregate caseAggregate = new CaseAggregate();
        when(eventSource.getStreamById(any())).thenReturn(eventStream);
        when(aggregateService.get(eventStream, CaseAggregate.class)).thenReturn(caseAggregate);

        caseAggregate.apply(new ProsecutionCaseCreated(getProsecutionCase(), null));
        addDefendantsToCourtProceedingsHandler.handle(envelope);

        final Stream<JsonEnvelope> envelopeStream = verifyAppendAndGetArgumentFrom(eventStream);


        assertThat(envelopeStream, streamContaining(
                jsonEnvelope(
                        metadata()
                                .withName("progression.event.defendants-added-to-court-proceedings"),
                        JsonEnvelopePayloadMatcher.payload().isJson(allOf(
                                withJsonPath("$.defendants", notNullValue()))
                        ).isJson(allOf(
                                withJsonPath("$.defendants[0].isYouth", equalTo(true)))
                        ).isJson(allOf(
                                withJsonPath("$.listHearingRequests", notNullValue()))
                        ).isJson(allOf(
                                withJsonPath("$.defendants[0].offences[0].reportingRestrictions[0].label", equalTo(YOUTH_RESTRICTION)))
                        ).isJson(allOf(
                                        withJsonPath("$.defendants[0].offences[0].reportingRestrictions[1].label", equalTo(SEXUAL_OFFENCE_RR_DESCRIPTION)))
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

    private List<JsonObject> prepareReferenceDataOffencesJsonObject(final UUID offenceId,
                                                                    final String offenceCode,
                                                                    final String legislation,
                                                                    final String payloadPath) {
        final String referenceDataOffenceJsonString = getPayload(payloadPath)
                .replace("OFFENCE_ID", offenceId.toString())
                .replace("OFFENCE_CODE", offenceCode)
                .replace("LEGISLATION", legislation);
        final JsonReader jsonReader = Json.createReader(new StringReader(referenceDataOffenceJsonString));


        final List<JsonObject> referencedataOffencesJsonObject = jsonReader.readObject().getJsonArray("offences").getValuesAs(JsonObject.class);
        return referencedataOffencesJsonObject;
    }
}
