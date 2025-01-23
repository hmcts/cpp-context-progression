package uk.gov.moj.cpp.progression.handler;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static java.util.UUID.randomUUID;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.core.annotation.Component.COMMAND_HANDLER;
import static uk.gov.justice.services.messaging.Envelope.envelopeFrom;
import static uk.gov.justice.services.test.utils.core.helper.EventStreamMockHelper.verifyAppendAndGetArgumentFrom;
import static uk.gov.justice.services.test.utils.core.matchers.HandlerMatcher.isHandler;
import static uk.gov.justice.services.test.utils.core.matchers.HandlerMethodMatcher.method;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeMatcher.jsonEnvelope;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeMetadataMatcher.metadata;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeStreamMatcher.streamContaining;

import uk.gov.justice.core.courts.AddOrStoreDefendantsAndListingHearingRequests;
import uk.gov.justice.core.courts.CourtCentre;
import uk.gov.justice.core.courts.Defendant;
import uk.gov.justice.core.courts.DefendantsAndListingHearingRequestsAdded;
import uk.gov.justice.core.courts.HearingType;
import uk.gov.justice.core.courts.JurisdictionType;
import uk.gov.justice.core.courts.ListDefendantRequest;
import uk.gov.justice.core.courts.ListHearingRequest;
import uk.gov.justice.core.courts.PersonDefendant;
import uk.gov.justice.core.courts.ProsecutionCaseCreatedInHearing;
import uk.gov.justice.core.courts.ReferralReason;
import uk.gov.justice.progression.courts.DefendantsAndListingHearingRequestsStored;
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

import java.util.Arrays;
import java.util.Collections;
import java.util.UUID;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class AddOrStoreDefendantsAndListingHearingRequestsCommandHandlerTest {

    @Mock
    private EventSource eventSource;

    @Mock
    private EventStream eventStream;

    @Mock
    private AggregateService aggregateService;

    @Spy
    private Enveloper enveloper = EnveloperFactory.createEnveloperWithEvents(DefendantsAndListingHearingRequestsStored.class, DefendantsAndListingHearingRequestsAdded.class);

    @InjectMocks
    private AddOrStoreDefendantsAndListingHearingRequestsCommandHandler addOrStoreDefendantsAndListingHearingRequestsCommandHandler;

    @Test
    public void shouldHandleCommand() {
        assertThat(new AddOrStoreDefendantsAndListingHearingRequestsCommandHandler(), isHandler(COMMAND_HANDLER)
                .with(method("addOrStoreDefendantsAndListingHearingRequests")
                        .thatHandles("progression.command.add-or-store-defendants-and-listing-hearing-requests")
                ));
    }

    @Test
    public void shouldCreateDefendantsAndListingHearingRequestsStoredEvent() throws Exception {

        final UUID prosecutionCaseId = randomUUID();
        final UUID defendantId = randomUUID();

        final Defendant defendant = Defendant.defendant()
                .withPersonDefendant(PersonDefendant.personDefendant().build())
                        .withProsecutionCaseId(prosecutionCaseId)
                        .withId(defendantId)
                        .build();

        final ReferralReason referralReason = ReferralReason.referralReason()
                .withId(randomUUID())
                .withDefendantId(defendant.getId())
                .build();

        final ListDefendantRequest listDefendantRequest = ListDefendantRequest.listDefendantRequest()
                .withProsecutionCaseId(defendant.getProsecutionCaseId())
                .withDefendantOffences(Collections.singletonList(randomUUID()))
                .withReferralReason(referralReason)
                .build();

        final HearingType hearingType = HearingType.hearingType().withId(randomUUID()).withDescription("TO_JAIL").build();
        final CourtCentre courtCentre = CourtCentre.courtCentre().withId(randomUUID()).build();

        final ListHearingRequest listHearingRequest = ListHearingRequest.listHearingRequest()
                .withCourtCentre(courtCentre).withHearingType(hearingType)
                .withJurisdictionType(JurisdictionType.MAGISTRATES)
                .withListDefendantRequests(Arrays.asList(listDefendantRequest))
                .build();

        final AddOrStoreDefendantsAndListingHearingRequests addOrStoreDefendantsAndListingHearingRequests = AddOrStoreDefendantsAndListingHearingRequests.addOrStoreDefendantsAndListingHearingRequests()
                .withDefendants(Collections.singletonList(defendant))
                .withListHearingRequests(Collections.singletonList(listHearingRequest))
                .build();

        final Metadata metadata = Envelope.metadataBuilder()
                .withName("progression.command.add-or-store-defendants-and-listing-hearing-requests")
                .withId(randomUUID())
                .build();

        final Envelope<AddOrStoreDefendantsAndListingHearingRequests> envelope = envelopeFrom(metadata, addOrStoreDefendantsAndListingHearingRequests);

        final CaseAggregate caseAggregate = new CaseAggregate();
        when(eventSource.getStreamById(any())).thenReturn(eventStream);
        when(aggregateService.get(eventStream, CaseAggregate.class)).thenReturn(caseAggregate);

        addOrStoreDefendantsAndListingHearingRequestsCommandHandler.addOrStoreDefendantsAndListingHearingRequests(envelope);

        final Stream<JsonEnvelope> envelopeStream = verifyAppendAndGetArgumentFrom(eventStream);

        assertThat(envelopeStream, streamContaining(
                jsonEnvelope(
                        metadata()
                                .withName("progression.event.defendants-and-listing-hearing-requests-stored"),
                        JsonEnvelopePayloadMatcher.payload().isJson(allOf(
                                withJsonPath("$.defendants[0].prosecutionCaseId", equalTo(prosecutionCaseId.toString())),
                                withJsonPath("$.defendants[0].defendantId", equalTo(defendantId.toString())))
                        ).isJson(allOf(
                                withJsonPath("$.listHearingRequests", notNullValue()))
                        )

                )
        ));

    }

    @Test
    public void shouldCreateDefendantsAndListingHearingRequestsAddedEvent() throws Exception {

        final UUID prosecutionCaseId = randomUUID();
        final UUID defendantId = randomUUID();

        final Defendant defendant = Defendant.defendant()
                .withPersonDefendant(PersonDefendant.personDefendant().build())
                .withProsecutionCaseId(prosecutionCaseId)
                .withId(defendantId)
                .build();

        final ReferralReason referralReason = ReferralReason.referralReason()
                .withId(randomUUID())
                .withDefendantId(defendant.getId())
                .build();

        final ListDefendantRequest listDefendantRequest = ListDefendantRequest.listDefendantRequest()
                .withProsecutionCaseId(defendant.getProsecutionCaseId())
                .withDefendantOffences(Collections.singletonList(randomUUID()))
                .withReferralReason(referralReason)
                .build();

        final HearingType hearingType = HearingType.hearingType().withId(randomUUID()).withDescription("TO_JAIL").build();
        final CourtCentre courtCentre = CourtCentre.courtCentre().withId(randomUUID()).build();

        final ListHearingRequest listHearingRequest = ListHearingRequest.listHearingRequest()
                .withCourtCentre(courtCentre).withHearingType(hearingType)
                .withJurisdictionType(JurisdictionType.MAGISTRATES)
                .withListDefendantRequests(Arrays.asList(listDefendantRequest))
                .build();

        final AddOrStoreDefendantsAndListingHearingRequests addOrStoreDefendantsAndListingHearingRequests = AddOrStoreDefendantsAndListingHearingRequests.addOrStoreDefendantsAndListingHearingRequests()
                .withDefendants(Collections.singletonList(defendant))
                .withListHearingRequests(Collections.singletonList(listHearingRequest))
                .build();

        final Metadata metadata = Envelope.metadataBuilder()
                .withName("progression.command.add-or-store-defendants-and-listing-hearing-requests")
                .withId(randomUUID())
                .build();

        final Envelope<AddOrStoreDefendantsAndListingHearingRequests> envelope = envelopeFrom(metadata, addOrStoreDefendantsAndListingHearingRequests);

        final CaseAggregate caseAggregate = new CaseAggregate();
        when(eventSource.getStreamById(any())).thenReturn(eventStream);
        when(aggregateService.get(eventStream, CaseAggregate.class)).thenReturn(caseAggregate);

        caseAggregate.apply(new ProsecutionCaseCreatedInHearing(prosecutionCaseId));
        addOrStoreDefendantsAndListingHearingRequestsCommandHandler.addOrStoreDefendantsAndListingHearingRequests(envelope);

        final Stream<JsonEnvelope> envelopeStream = verifyAppendAndGetArgumentFrom(eventStream);

        assertThat(envelopeStream, streamContaining(
                jsonEnvelope(
                        metadata()
                                .withName("progression.event.defendants-and-listing-hearing-requests-added"),
                        JsonEnvelopePayloadMatcher.payload().isJson(allOf(
                                withJsonPath("$.defendants[0].prosecutionCaseId", equalTo(prosecutionCaseId.toString())),
                                withJsonPath("$.defendants[0].defendantId", equalTo(defendantId.toString())))
                        ).isJson(allOf(
                                withJsonPath("$.listHearingRequests", notNullValue()))
                        )
                )
        ));
    }
}
