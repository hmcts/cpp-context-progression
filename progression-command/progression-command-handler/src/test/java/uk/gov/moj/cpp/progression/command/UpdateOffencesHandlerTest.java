package uk.gov.moj.cpp.progression.command;

import static java.util.Collections.singletonList;
import static java.util.UUID.randomUUID;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyList;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.core.annotation.Component.COMMAND_HANDLER;
import static uk.gov.justice.services.messaging.Envelope.envelopeFrom;
import static uk.gov.justice.services.test.utils.core.helper.EventStreamMockHelper.verifyAppendAndGetArgumentFrom;
import static uk.gov.justice.services.test.utils.core.matchers.HandlerMatcher.isHandler;
import static uk.gov.justice.services.test.utils.core.matchers.HandlerMethodMatcher.method;
import static uk.gov.moj.cpp.progression.test.FileUtil.getPayload;

import uk.gov.justice.core.courts.CasesReferredToCourt;
import uk.gov.justice.core.courts.Defendant;
import uk.gov.justice.core.courts.DefendantCaseOffences;
import uk.gov.justice.core.courts.Offence;
import uk.gov.justice.core.courts.ProsecutionCase;
import uk.gov.justice.core.courts.ProsecutionCaseCreated;
import uk.gov.justice.core.courts.ProsecutionCaseIdentifier;
import uk.gov.justice.core.courts.ReportingRestriction;
import uk.gov.justice.core.courts.UpdateOffencesForProsecutionCase;
import uk.gov.justice.services.core.aggregate.AggregateService;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.core.requester.Requester;
import uk.gov.justice.services.eventsourcing.source.core.EventSource;
import uk.gov.justice.services.eventsourcing.source.core.EventStream;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.messaging.Metadata;
import uk.gov.justice.services.test.utils.core.enveloper.EnveloperFactory;
import uk.gov.justice.services.test.utils.core.random.StringGenerator;
import uk.gov.moj.cpp.progression.aggregate.CaseAggregate;
import uk.gov.moj.cpp.progression.handler.UpdateOffencesHandler;
import uk.gov.moj.cpp.progression.service.ReferenceDataOffenceService;

import java.io.StringReader;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonReader;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class UpdateOffencesHandlerTest {

    @Spy
    private final Enveloper enveloper = EnveloperFactory.createEnveloperWithEvents(CasesReferredToCourt.class);
    @Mock
    private EventSource eventSource;
    @Mock
    private EventStream eventStream;
    @Mock
    private AggregateService aggregateService;
    @InjectMocks
    private UpdateOffencesHandler updateOffencesHandler;

    private CaseAggregate aggregate;

    @Mock
    private Requester requester;

    @Mock
    private ReferenceDataOffenceService referenceDataOffenceService;

    private UUID caseId;
    private UUID defendantId;
    private UUID offenceId;
    private String offenceCode;

    private static final String SEXUAL_OFFENCE_RR_DESCRIPTION = "Complainant's anonymity protected by virtue of Section 1 of the Sexual Offences Amendment Act 1992";
    private  static final String YOUTH_RESTRICTION = "Section 49 of the Children and Young Persons Act 1933 applies";

    @Before
    public void setup() {
         caseId = randomUUID();
         defendantId = randomUUID();
         offenceId = randomUUID();
         offenceCode = new StringGenerator().next();
        aggregate = new CaseAggregate();
        when(eventSource.getStreamById(any())).thenReturn(eventStream);
        when(aggregateService.get(eventStream, CaseAggregate.class)).thenReturn(aggregate);
    }

    @Test
    public void shouldHandleCommand() {
        assertThat(new UpdateOffencesHandler(), isHandler(COMMAND_HANDLER)
                .with(method("handle")
                        .thatHandles("progression.command.update-offences-for-prosecution-case")
                ));
    }

    @Test
    public void shouldProcessCommand() throws Exception {

        UpdateOffencesForProsecutionCase updateDefendantCaseOffences = prepareData(caseId,defendantId,offenceId, offenceCode);

        aggregate = getEventStreamReady(caseId,defendantId);
        when(this.aggregateService.get(this.eventStream, CaseAggregate.class)).thenReturn(aggregate);

        final List<JsonObject> referencedataOffencesJsonObject = prepareReferenceDataOffencesJsonObject(offenceId, offenceCode,
                SEXUAL_OFFENCE_RR_DESCRIPTION,
                "json/referencedataoffences.offences-list.json");

        when(referenceDataOffenceService.getMultipleOffencesByOffenceCodeList(anyList(), any(), eq(requester))).thenReturn(Optional.of(referencedataOffencesJsonObject));

        final Metadata metadata = Envelope
                .metadataBuilder()
                .withName("progression.command.update-offences-for-prosecution-case")
                .withId(randomUUID())
                .build();

        final Envelope<UpdateOffencesForProsecutionCase> envelope = envelopeFrom(metadata, updateDefendantCaseOffences);

        updateOffencesHandler.handle(envelope);

        Stream<JsonEnvelope> events =  verifyAppendAndGetArgumentFrom(eventStream);
    }

    @Test
    public void shouldNotAddDuplicateYouthFlag() throws  Exception {
        final List<Offence> offences  =  Stream.of(
                Offence.offence().withId(randomUUID()).withOffenceCode(randomUUID().toString()).withOffenceTitle("RegularOffence1").withReportingRestrictions(Collections.singletonList(ReportingRestriction.reportingRestriction().withId(randomUUID()).withLabel(YOUTH_RESTRICTION).withOrderedDate(LocalDate.now()).build())).build(),
                Offence.offence().withId(offenceId).withOffenceCode(offenceCode).withOffenceTitle("SexualOffence1").build()
        ).collect(Collectors.toList());

        UpdateOffencesForProsecutionCase updateOffencesForProsecutionCase = prepareDatabByOffences (caseId,defendantId,offences);

        aggregate = getEventStreamReady(caseId,defendantId);
        when(this.aggregateService.get(this.eventStream, CaseAggregate.class)).thenReturn(aggregate);
        final List<JsonObject> referencedataOffencesJsonObject = prepareReferenceDataOffencesJsonObject(offenceId, offenceCode,
                SEXUAL_OFFENCE_RR_DESCRIPTION,
                "json/referencedataoffences.offences-list.json");

        when(referenceDataOffenceService.getMultipleOffencesByOffenceCodeList(anyList(), any(), eq(requester))).thenReturn(Optional.of(referencedataOffencesJsonObject));

        final Metadata metadata = Envelope
                .metadataBuilder()
                .withName("progression.command.update-offences-for-prosecution-case")
                .withId(randomUUID())
                .build();

        final Envelope<UpdateOffencesForProsecutionCase> envelope = envelopeFrom(metadata, updateOffencesForProsecutionCase);

        updateOffencesHandler.handle(envelope);

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


        final List<JsonObject> referencedataOffencesJsonObject = jsonReader.readObject().getJsonArray("offences") .getValuesAs(JsonObject.class);
        return referencedataOffencesJsonObject;
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
                                            .withId(randomUUID())
                                            .withOrderIndex(1)
                                            .build()))
                                    .build()))
                            .build())
                    .build());
        }};
    }

    private UpdateOffencesForProsecutionCase  prepareData(final UUID caseId,final UUID defendantId, final UUID offenceId, final String offenceCode) {
        final List<Offence> offences = Stream.of(
                Offence.offence().withId(randomUUID()).withOffenceCode(randomUUID().toString()).withOffenceTitle("RegularOffence1").build(),
                Offence.offence().withId(offenceId).withOffenceCode(offenceCode).withOffenceTitle("SexualOffence1").build()
        ).collect(Collectors.toList()
        );

        final DefendantCaseOffences defendantCaseOffences =
                DefendantCaseOffences.defendantCaseOffences().withOffences(offences)
                        .withProsecutionCaseId(caseId)
                        .withDefendantId(defendantId)
                        .build();
        return  UpdateOffencesForProsecutionCase.updateOffencesForProsecutionCase().withSwitchedToYouth(true).withDefendantCaseOffences(defendantCaseOffences).build();
    }

    private UpdateOffencesForProsecutionCase prepareDatabByOffences(final UUID caseId, final UUID defendantId, final List<Offence> offences) {
        final DefendantCaseOffences defendantCaseOffences =
                DefendantCaseOffences.defendantCaseOffences().withOffences(offences)
                        .withProsecutionCaseId(caseId)
                        .withDefendantId(defendantId)
                        .build();
        return  UpdateOffencesForProsecutionCase.updateOffencesForProsecutionCase().withSwitchedToYouth(true).withDefendantCaseOffences(defendantCaseOffences).build();
    }
}
