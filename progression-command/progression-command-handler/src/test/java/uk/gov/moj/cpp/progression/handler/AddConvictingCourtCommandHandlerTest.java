package uk.gov.moj.cpp.progression.handler;

import static java.util.Collections.singletonList;
import static java.util.UUID.randomUUID;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.core.annotation.Component.COMMAND_HANDLER;
import static uk.gov.justice.services.messaging.Envelope.envelopeFrom;
import static uk.gov.justice.services.test.utils.core.helper.EventStreamMockHelper.verifyAppendAndGetArgumentFrom;
import static uk.gov.justice.services.test.utils.core.matchers.HandlerMatcher.isHandler;
import static uk.gov.justice.services.test.utils.core.matchers.HandlerMethodMatcher.method;
import static uk.gov.moj.cpp.progression.test.FileUtil.getPayload;

import uk.gov.justice.core.courts.AddConvictingCourt;
import uk.gov.justice.core.courts.AddConvictingInformation;
import uk.gov.justice.core.courts.CourtCentre;
import uk.gov.justice.core.courts.Defendant;
import uk.gov.justice.core.courts.Offence;
import uk.gov.justice.core.courts.ProsecutionCase;
import uk.gov.justice.core.courts.ProsecutionCaseCreated;
import uk.gov.justice.core.courts.ProsecutionCaseIdentifier;
import uk.gov.justice.core.courts.ReportingRestriction;
import uk.gov.justice.services.common.util.UtcClock;
import uk.gov.justice.services.core.aggregate.AggregateService;
import uk.gov.justice.services.core.requester.Requester;
import uk.gov.justice.services.eventsourcing.source.core.EventSource;
import uk.gov.justice.services.eventsourcing.source.core.EventStream;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.Metadata;
import uk.gov.justice.services.test.utils.core.random.StringGenerator;
import uk.gov.moj.cpp.progression.aggregate.CaseAggregate;
import uk.gov.moj.cpp.progression.service.ReferenceDataOffenceService;

import java.io.StringReader;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonReader;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class AddConvictingCourtCommandHandlerTest {

    @Mock
    private EventSource eventSource;
    @Mock
    private EventStream eventStream;
    @Mock
    private AggregateService aggregateService;
    @InjectMocks
    private AddConvictingCourtCommandHandler addConvictingCourtCommandHandler;

    private CaseAggregate aggregate;

    @Mock
    private Requester requester;

    @Mock
    private ReferenceDataOffenceService referenceDataOffenceService;

    private UUID caseId;
    private UUID defendantId;
    private UUID offenceId;
    private UUID courtCentreId;
    private String offenceCode;
    private String courtCode;

    private static final String SEXUAL_OFFENCE_RR_DESCRIPTION = "Complainant's anonymity protected by virtue of Section 1 of the Sexual Offences Amendment Act 1992";


    @BeforeEach
    public void setup() {
        caseId = randomUUID();
        defendantId = randomUUID();
        offenceId = randomUUID();
        courtCentreId = randomUUID();
        offenceCode = new StringGenerator().next();
        courtCode = new StringGenerator().next();
        aggregate = new CaseAggregate();
    }

    @Test
    public void shouldHandleCommand() {
        assertThat(new AddConvictingCourtCommandHandler(), isHandler(COMMAND_HANDLER)
                .with(method("handle")
                        .thatHandles("progression.command.add-convicting-court")
                ));
    }

    @Test
    public void shouldProcessCommand() throws Exception {

        AddConvictingCourt addConvictingCourt = prepareData(caseId, offenceId, courtCentreId, courtCode);
        aggregate = getEventStreamReady(caseId, defendantId);

        when(eventSource.getStreamById(any())).thenReturn(eventStream);
        when(aggregateService.get(eventStream, CaseAggregate.class)).thenReturn(aggregate);
        when(aggregateService.get(this.eventStream, CaseAggregate.class)).thenReturn(aggregate);

        final List<JsonObject> referencedataOffencesJsonObject = prepareReferenceDataOffencesJsonObject(offenceId, offenceCode,
                SEXUAL_OFFENCE_RR_DESCRIPTION,
                "json/referencedataoffences.offences-list.json");

        when(referenceDataOffenceService.getMultipleOffencesByOffenceCodeList(anyList(), any(), eq(requester))).thenReturn(Optional.of(referencedataOffencesJsonObject));

        final Metadata metadata = Envelope
                .metadataBuilder()
                .withName("progression.command.add-convicting-court")
                .withId(randomUUID())
                .build();

        final Envelope<AddConvictingCourt> envelope = envelopeFrom(metadata, addConvictingCourt);
        addConvictingCourtCommandHandler.handle(envelope);
        verifyAppendAndGetArgumentFrom(eventStream);

    }

    @Test
    public void shouldProcessCommandWhenOrderIndexDoesNotExist() throws Exception {

        AddConvictingCourt addConvictingCourt = prepareData(caseId, offenceId, courtCentreId, courtCode);
        aggregate = getEventStreamReadyWithOutOrderIndex(caseId, defendantId);

        when(eventSource.getStreamById(any())).thenReturn(eventStream);
        when(aggregateService.get(eventStream, CaseAggregate.class)).thenReturn(aggregate);
        when(aggregateService.get(this.eventStream, CaseAggregate.class)).thenReturn(aggregate);

        final List<JsonObject> referencedataOffencesJsonObject = prepareReferenceDataOffencesJsonObject(offenceId, offenceCode,
                SEXUAL_OFFENCE_RR_DESCRIPTION,
                "json/referencedataoffences.offences-list.json");

        when(referenceDataOffenceService.getMultipleOffencesByOffenceCodeList(anyList(), any(), eq(requester))).thenReturn(Optional.of(referencedataOffencesJsonObject));

        final Metadata metadata = Envelope
                .metadataBuilder()
                .withName("progression.command.add-convicting-court")
                .withId(randomUUID())
                .build();

        final Envelope<AddConvictingCourt> envelope = envelopeFrom(metadata, addConvictingCourt);
        addConvictingCourtCommandHandler.handle(envelope);
        verifyAppendAndGetArgumentFrom(eventStream);

    }

    @Test
    public void withAggregateOffenceAndWithRR() throws Exception {

        AddConvictingCourt addConvictingCourt = prepareDataWithMultipleConvictingCourts(caseId, offenceId, courtCentreId, courtCode);
        aggregate = getEventStreamWithOffenceandMutlipleRR(caseId, defendantId, offenceId);

        when(eventSource.getStreamById(any())).thenReturn(eventStream);
        when(aggregateService.get(eventStream, CaseAggregate.class)).thenReturn(aggregate);
        when(aggregateService.get(this.eventStream, CaseAggregate.class)).thenReturn(aggregate);

        final List<JsonObject> referencedataOffencesJsonObject = prepareReferenceDataOffencesJsonObject(offenceId, offenceCode,
                SEXUAL_OFFENCE_RR_DESCRIPTION,
                "json/referencedataoffences.offences-list.json");

        when(referenceDataOffenceService.getMultipleOffencesByOffenceCodeList(anyList(), any(), eq(requester))).thenReturn(Optional.of(referencedataOffencesJsonObject));

        final Metadata metadata = Envelope
                .metadataBuilder()
                .withName("progression.command.add-convicting-court")
                .withId(randomUUID())
                .build();

        final Envelope<AddConvictingCourt> envelope = envelopeFrom(metadata, addConvictingCourt);
        addConvictingCourtCommandHandler.handle(envelope);
        verifyAppendAndGetArgumentFrom(eventStream);
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
        return jsonReader.readObject().getJsonArray("offences").getValuesAs(JsonObject.class);
    }

    private CaseAggregate getEventStreamWithOffenceandMutlipleRR(final UUID caseId, final UUID defendantId, final UUID offenceId) {
        return new CaseAggregate() {{
            apply(ProsecutionCaseCreated.prosecutionCaseCreated()
                    .withProsecutionCase(ProsecutionCase.prosecutionCase()
                            .withId(caseId)
                            .withProsecutionCaseIdentifier(ProsecutionCaseIdentifier.prosecutionCaseIdentifier().build())
                            .withDefendants(singletonList(Defendant.defendant()
                                    .withId(defendantId)
                                    .withOffences(getOneOffenceWithMultipleRR(offenceId))
                                    .build()))
                            .build())
                    .build());
        }};
    }

    private CaseAggregate getEventStreamReady(final UUID caseId, final UUID defendantId) {
        return new CaseAggregate() {{
            apply(ProsecutionCaseCreated.prosecutionCaseCreated()
                    .withProsecutionCase(ProsecutionCase.prosecutionCase()
                            .withId(caseId)
                            .withProsecutionCaseIdentifier(ProsecutionCaseIdentifier.prosecutionCaseIdentifier().build())
                            .withDefendants(singletonList(Defendant.defendant()
                                    .withId(defendantId)
                                    .withOffences(singletonList(Offence.offence()
                                            .withId(offenceId)
                                            .withOrderIndex(1)
                                            .build()))
                                    .build()))
                            .build())
                    .build());
        }};
    }

    private CaseAggregate getEventStreamReadyWithOutOrderIndex(final UUID caseId, final UUID defendantId) {
        return new CaseAggregate() {{
            apply(ProsecutionCaseCreated.prosecutionCaseCreated()
                    .withProsecutionCase(ProsecutionCase.prosecutionCase()
                            .withId(caseId)
                            .withProsecutionCaseIdentifier(ProsecutionCaseIdentifier.prosecutionCaseIdentifier().build())
                            .withDefendants(singletonList(Defendant.defendant()
                                    .withId(defendantId)
                                    .withOffences(singletonList(Offence.offence()
                                            .withId(offenceId)
                                            .build()))
                                    .build()))
                            .build())
                    .build());
        }};
    }

    private List<Offence> getOneOffenceWithMultipleRR(final UUID offenceId) {
        return singletonList(Offence.offence().withId(offenceId).withOrderIndex(1)
                .withReportingRestrictions(Stream.of(
                        ReportingRestriction.reportingRestriction()
                                .withId(randomUUID())
                                .withLabel("Youth Restriction")
                                .withOrderedDate(LocalDate.now()).build(),
                        ReportingRestriction.reportingRestriction().withId(randomUUID())
                                .withLabel("Sexual Offence Restriction")
                                .withOrderedDate(LocalDate.now()).build()).collect(Collectors.toList())).build());
    }

    private AddConvictingCourt prepareData(final UUID caseId, final UUID offenceId, final UUID courtCentreId, String courtCode) {

        CourtCentre convictingCourt = CourtCentre.courtCentre().withId(courtCentreId).withCode(courtCode).build();
        final List<AddConvictingInformation> addConvictingInformation = Stream.of(
                AddConvictingInformation.addConvictingInformation()
                        .withConvictingCourt(convictingCourt)
                        .withConvictionDate(new UtcClock().now())
                        .withOffenceId(offenceId)
                        .build()
                ).collect(Collectors.toList());

        return  AddConvictingCourt.addConvictingCourt().withCaseId(caseId).withAddConvictingInformation(addConvictingInformation).build();
    }

    private AddConvictingCourt prepareDataWithMultipleConvictingCourts(final UUID caseId, final UUID offenceId, final UUID courtCentreId, String courtCode) {

        CourtCentre convictingCourt1 = CourtCentre.courtCentre().withId(courtCentreId).withCode(courtCode).build();
        CourtCentre convictingCourt2 = CourtCentre.courtCentre().withId(randomUUID()).withCode("SampleCourtCode").build();
        final List<AddConvictingInformation> addConvictingInformation = Stream.of(
                AddConvictingInformation.addConvictingInformation()
                        .withConvictingCourt(convictingCourt1)
                        .withConvictionDate(new UtcClock().now())
                        .withOffenceId(offenceId)
                        .build(), AddConvictingInformation.addConvictingInformation()
                        .withConvictingCourt(convictingCourt2)
                        .withConvictionDate(new UtcClock().now())
                        .withOffenceId(randomUUID())
                        .build()
        ).collect(Collectors.toList());

        return  AddConvictingCourt.addConvictingCourt().withCaseId(caseId).withAddConvictingInformation(addConvictingInformation).build();
    }

}