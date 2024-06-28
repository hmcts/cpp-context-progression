package uk.gov.moj.cpp.progression.handler;

import static java.util.UUID.randomUUID;
import static java.util.stream.Collectors.toList;
import static javax.json.Json.createArrayBuilder;
import static javax.json.Json.createObjectBuilder;
import static org.codehaus.groovy.runtime.InvokerHelper.asList;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.core.annotation.Component.COMMAND_HANDLER;
import static uk.gov.justice.services.test.utils.core.helper.EventStreamMockHelper.verifyAppendAndGetArgumentFrom;
import static uk.gov.justice.services.test.utils.core.matchers.HandlerMatcher.isHandler;
import static uk.gov.justice.services.test.utils.core.matchers.HandlerMethodMatcher.method;

import uk.gov.justice.core.courts.CourtCentre;
import uk.gov.justice.core.courts.Defendant;
import uk.gov.justice.core.courts.DefendantsAddedToCourtProceedings;
import uk.gov.justice.core.courts.HearingType;
import uk.gov.justice.core.courts.JurisdictionType;
import uk.gov.justice.core.courts.ListDefendantRequest;
import uk.gov.justice.core.courts.ListHearingRequest;
import uk.gov.justice.core.courts.Offence;
import uk.gov.justice.core.courts.ProsecutionCase;
import uk.gov.justice.core.courts.ProsecutionCaseCreated;
import uk.gov.justice.core.courts.ProsecutionCaseIdentifier;
import uk.gov.justice.core.courts.ReferralReason;
import uk.gov.justice.core.courts.ReportingRestriction;
import uk.gov.justice.progression.courts.OffencesForDefendantChanged;
import uk.gov.justice.services.core.aggregate.AggregateService;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.eventsourcing.source.core.EventSource;
import uk.gov.justice.services.eventsourcing.source.core.EventStream;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.messaging.Metadata;
import uk.gov.justice.services.test.utils.core.enveloper.EnveloperFactory;
import uk.gov.moj.cpp.progression.aggregate.CaseAggregate;
import uk.gov.moj.cpp.progression.service.LegalStatusReferenceDataService;

import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;

import javax.json.Json;
import javax.json.JsonObject;

import com.google.common.collect.Lists;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class RecordLAAReferenceToOffenceHandlerTest {

    @Mock
    private EventSource eventSource;

    @Mock
    private EventStream eventStream;

    @Mock
    private AggregateService aggregateService;

    @Mock
    private LegalStatusReferenceDataService legalStatusReferenceDataService;


    @Spy
    private final Enveloper enveloper = EnveloperFactory.createEnveloperWithEvents(
            OffencesForDefendantChanged.class);

    @InjectMocks
    private RecordLAAReferenceToOffenceHandler recordLAAReferenceToOffenceHandler;


    private CaseAggregate aggregate;

    private static final UUID CASE_ID = randomUUID();

    private static final UUID DEFENDANT_ID = randomUUID();

    private static final UUID DEFENDANT_ID_2 = randomUUID();


    private static final UUID OFFENCE_ID = randomUUID();

    private static final UUID LEGAL_STATUS_ID = randomUUID();



    @Before
    public void setup() {
        aggregate = new CaseAggregate();
        when(eventSource.getStreamById(any())).thenReturn(eventStream);
        when(aggregateService.get(eventStream, CaseAggregate.class)).thenReturn(aggregate);
    }

    @Test
    public void shouldHandleCommand() {
        assertThat(new RecordLAAReferenceToOffenceHandler(), isHandler(COMMAND_HANDLER)
                .with(method("handle")
                        .thatHandles("progression.command.handler.record-laareference-for-offence")
                ));
    }

    @Test
    public void shouldProcessCommand() throws Exception {

        final DefendantsAddedToCourtProceedings defendantsAddedToCourtProceedings = buildDefendantsAddedToCourtProceedings(
                CASE_ID, DEFENDANT_ID, DEFENDANT_ID_2, OFFENCE_ID);

        aggregate.apply(new ProsecutionCaseCreated(getProsecutionCase(), null));
        aggregate.defendantsAddedToCourtProceedings(defendantsAddedToCourtProceedings.getDefendants(), defendantsAddedToCourtProceedings.getListHearingRequests(),  Optional.of(createJsonList())).collect(toList());

        final JsonObject jsonObject = generateRecordLAAReferenceForOffence();

        final Metadata metadata = Envelope
                .metadataBuilder()
                .withName("progression.command.handler.record-laareference-for-offence")
                .withId(randomUUID())
                .build();

        final JsonEnvelope envelope = JsonEnvelope.envelopeFrom(metadata,jsonObject);

        when(legalStatusReferenceDataService.getLegalStatusByStatusIdAndStatusCode(any(JsonEnvelope.class), any(String.class))).thenReturn(Optional.of(getLegalStatus()));

        recordLAAReferenceToOffenceHandler.handle(envelope);
        ArgumentCaptor<Stream> events = ArgumentCaptor.forClass(Stream.class);
       verify(eventStream).append(events.capture());

       verifyAppendAndGetArgumentFrom(eventStream);

    }

    private static JsonObject getLegalStatus() {
        return Json.createObjectBuilder()
                .add("id", LEGAL_STATUS_ID.toString())
                .add("statusDescription", "description")
                .add("defendantLevelStatus","Granted")
                .build();
    }

    private static JsonObject generateRecordLAAReferenceForOffence() {
        return Json.createObjectBuilder()
                .add("caseId", CASE_ID.toString())
                .add("defendantId", DEFENDANT_ID.toString())
                .add("offenceId", OFFENCE_ID.toString())
                .add("statusId", randomUUID().toString())
                .add("statusCode", "statusCode")
                .add("applicationReference", "AB746921")
                .add("statusDate", "2019-07-01")
                .add("defendantLevelStatus","Granted")
                .build();
    }

    private DefendantsAddedToCourtProceedings buildDefendantsAddedToCourtProceedings(
            final UUID caseId, final UUID defendantId, final UUID defendantId2, final UUID offenceId) {

        ReportingRestriction reportingRestriction = ReportingRestriction.reportingRestriction().withLabel("Victim offence").build();
        uk.gov.justice.core.courts.Offence offence = uk.gov.justice.core.courts.Offence.offence()
                .withId(offenceId)
                .withOffenceDefinitionId(UUID.randomUUID())
                .withOffenceCode("TFL123")
                .withOffenceTitle("TFL Ticket Dodger")
                .withReportingRestrictions(Lists.newArrayList(reportingRestriction))
                .withWording("TFL ticket dodged")
                .withStartDate(LocalDate.of(2019, 05, 01))
                .withCount(0)
                .build();
        uk.gov.justice.core.courts.Defendant defendant = uk.gov.justice.core.courts.Defendant.defendant()
                .withId(defendantId)
                .withProsecutionCaseId(caseId)
                .withOffences(new ArrayList<>(asList(offence)))
                .build();

        //Add duplicate defendant
        uk.gov.justice.core.courts.Defendant defendant1 = uk.gov.justice.core.courts.Defendant.defendant()
                .withId(defendantId)
                .withProsecutionCaseId(caseId)
                .withOffences(new ArrayList<>(asList(offence)))
                .build();

        uk.gov.justice.core.courts.Defendant defendant2 = uk.gov.justice.core.courts.Defendant.defendant()
                .withId(defendantId2)
                .withProsecutionCaseId(caseId)
                .withOffences(new ArrayList<>(asList(offence)))
                .build();

        ReferralReason referralReason = ReferralReason.referralReason()
                .withId(UUID.randomUUID())
                .withDefendantId(defendantId)
                .withDescription("Dodged TFL tickets with passion")
                .build();

        ReferralReason referralReason2 = ReferralReason.referralReason()
                .withId(UUID.randomUUID())
                .withDefendantId(defendantId2)
                .withDescription("Dodged TFL tickets with passion")
                .build();

        ListDefendantRequest listDefendantRequest = ListDefendantRequest.listDefendantRequest()
                .withProsecutionCaseId(caseId)
                .withDefendantOffences(Collections.singletonList(offenceId))
                .withReferralReason(referralReason)
                .build();
        ListDefendantRequest listDefendantRequest2 = ListDefendantRequest.listDefendantRequest()
                .withProsecutionCaseId(caseId)
                .withDefendantOffences(Collections.singletonList(offenceId))
                .withReferralReason(referralReason2)
                .build();

        HearingType hearingType = HearingType.hearingType().withId(UUID.randomUUID()).withDescription("TO_JAIL").build();
        CourtCentre courtCentre = CourtCentre.courtCentre().withId(UUID.randomUUID()).build();

        ListHearingRequest listHearingRequest = ListHearingRequest.listHearingRequest()
                .withCourtCentre(courtCentre).withHearingType(hearingType)
                .withJurisdictionType(JurisdictionType.MAGISTRATES)
                .withListDefendantRequests(Arrays.asList(listDefendantRequest, listDefendantRequest2))
                .withListedStartDateTime(ZonedDateTime.now())
                .build();

        return DefendantsAddedToCourtProceedings
                .defendantsAddedToCourtProceedings()
                .withDefendants(new ArrayList<>(Arrays.asList(defendant, defendant1, defendant2)))
                .withListHearingRequests(Collections.singletonList(listHearingRequest))
                .build();

    }

    private ProsecutionCase getProsecutionCase() {
        final List<Defendant> defendants = new ArrayList<>();
        defendants.add(Defendant.defendant().withId(randomUUID()).withOffences(Arrays.asList(Offence.offence().withId(randomUUID()).build())).build());
        return ProsecutionCase.prosecutionCase()
                .withProsecutionCaseIdentifier(ProsecutionCaseIdentifier.prosecutionCaseIdentifier()
                        .build())
                .withDefendants(defendants)
                .build();
    }

    private List<JsonObject> createJsonList() {
        return Arrays.asList(createArrayBuilder().add(
                createObjectBuilder()
                        .add("cjsOffenceCode", "TTH105HY")
                        .build()).build().getJsonObject(0));
    }

}
