package uk.gov.moj.cpp.progression.handler;

import static java.util.UUID.randomUUID;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.when;
import static uk.gov.justice.core.courts.Address.address;
import static uk.gov.justice.core.courts.ProsecutionCase.prosecutionCase;
import static uk.gov.justice.core.courts.ProsecutionCaseIdentifier.prosecutionCaseIdentifier;
import static uk.gov.justice.services.core.annotation.Component.COMMAND_HANDLER;
import static uk.gov.justice.services.test.utils.core.helper.EventStreamMockHelper.verifyAppendAndGetArgumentFrom;
import static uk.gov.justice.services.test.utils.core.matchers.HandlerMatcher.isHandler;
import static uk.gov.justice.services.test.utils.core.matchers.HandlerMethodMatcher.method;

import uk.gov.justice.core.courts.Address;
import uk.gov.justice.core.courts.DefenceOrganisation;
import uk.gov.justice.core.courts.Defendant;
import uk.gov.justice.core.courts.DefendantDefenceOrganisationChanged;
import uk.gov.justice.core.courts.InitiationCode;
import uk.gov.justice.core.courts.Offence;
import uk.gov.justice.core.courts.Organisation;
import uk.gov.justice.core.courts.PersonDefendant;
import uk.gov.justice.core.courts.ProsecutionCase;
import uk.gov.justice.core.courts.ProsecutionCaseDefendantOrganisationUpdatedByLaa;
import uk.gov.justice.core.courts.ProsecutionCaseDefendantUpdated;
import uk.gov.justice.core.courts.ProsecutionCaseOffencesUpdated;
import uk.gov.justice.core.courts.Prosecutor;
import uk.gov.justice.core.courts.ReceiveRepresentationOrderForDefendant;
import uk.gov.justice.cpp.progression.events.DefendantDefenceAssociationLocked;
import uk.gov.justice.progression.courts.DefendantLegalaidStatusUpdated;
import uk.gov.justice.progression.courts.OffencesForDefendantChanged;
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
import uk.gov.moj.cpp.progression.aggregate.CaseAggregate;
import uk.gov.moj.cpp.progression.command.handler.service.UsersGroupService;
import uk.gov.moj.cpp.progression.domain.pojo.OrganisationDetails;
import uk.gov.moj.cpp.progression.events.DefendantDefenceOrganisationAssociated;
import uk.gov.moj.cpp.progression.events.DefendantDefenceOrganisationDisassociated;
import uk.gov.moj.cpp.progression.events.DefendantLaaAssociated;
import uk.gov.moj.cpp.progression.events.RepresentationType;
import uk.gov.moj.cpp.progression.service.LegalStatusReferenceDataService;
import uk.gov.moj.cpp.progression.service.ProsecutionCaseQueryService;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.json.Json;
import javax.json.JsonObject;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class ReceiveRepresentationOrderHandlerTest {

    @Mock
    private EventSource eventSource;

    @Mock
    private EventStream eventStream;

    @Mock
    private AggregateService aggregateService;

    @Mock
    private LegalStatusReferenceDataService legalStatusReferenceDataService;

    @Mock
    private UsersGroupService usersGroupService;

    @Mock
    private ProsecutionCaseQueryService prosecutionCaseQueryService;

    @Spy
    private final JsonObjectToObjectConverter jsonObjectToObjectConverter = new JsonObjectToObjectConverter(new ObjectMapperProducer().objectMapper());

    @Spy
    private final ObjectToJsonObjectConverter objectToJsonObjectConverter = new ObjectToJsonObjectConverter(new ObjectMapperProducer().objectMapper());

    @Spy
    private final Enveloper enveloper = EnveloperFactory.createEnveloperWithEvents(
            OffencesForDefendantChanged.class,
            ProsecutionCaseOffencesUpdated.class,
            DefendantDefenceOrganisationChanged.class,
            DefendantDefenceOrganisationAssociated.class,
            DefendantDefenceOrganisationDisassociated.class,
            DefendantLegalaidStatusUpdated.class,
            DefendantLaaAssociated.class,
            DefendantDefenceAssociationLocked.class,
            ProsecutionCaseDefendantUpdated.class,
            ProsecutionCaseDefendantOrganisationUpdatedByLaa.class);

    @InjectMocks
    private ReceiveRepresentationOrderHandler receiveRepresentationOrderHandler;

    private static final UUID CASE_ID = randomUUID();
    private static final UUID PROSECUTOR_ID = randomUUID();

    private static final UUID DEFENDANT_ID = randomUUID();

    private static final UUID DEFENDANT_ID_2 = randomUUID();

    private static final UUID OFFENCE_ID = randomUUID();

    private static final UUID LEGAL_STATUS_ID = randomUUID();

    private static final String ASSOCIATION = "association";

    public static final String ORGANISATION_ID = "organisationId";
    public static final String PROSECUTOR_CODE = "D24AW";

    public static final String REPRESENTATION_TYPE = "representationType";

    private static final uk.gov.justice.core.courts.Defendant defendant1 = Defendant.defendant().withId(DEFENDANT_ID)
            .withPersonDefendant(PersonDefendant.personDefendant().build())
            .withOffences(Arrays.asList(Offence.offence().withId(OFFENCE_ID).build()))
            .withProsecutionCaseId(CASE_ID)
            .build();
    private static final uk.gov.justice.core.courts.Defendant defendant2 = uk.gov.justice.core.courts.Defendant.defendant().withId(DEFENDANT_ID_2)
            .withOffences(Arrays.asList(Offence.offence().withId(OFFENCE_ID).build()))
            .withProsecutionCaseId(CASE_ID)
            .withPersonDefendant(PersonDefendant.personDefendant().build()).build();

    static final List<Defendant> defendants = new ArrayList<Defendant>() {{
        add(defendant1);
        add(defendant2);
    }};

    private static final UUID SAME_DEFENDANT_OFFENCE_ID_1 = randomUUID();

    private static final UUID SAME_DEFENDANT_OFFENCE_ID_2 = randomUUID();

    private static final UUID MULTI_OFFENCE_DEFENDANT_ID = randomUUID();

    static final List<Offence> offences = new ArrayList<Offence>() {{
        add(Offence.offence().withId(SAME_DEFENDANT_OFFENCE_ID_1).build());
        add(Offence.offence().withId(SAME_DEFENDANT_OFFENCE_ID_2).build());
    }};

    private static final uk.gov.justice.core.courts.Defendant multiOffenceDefendant = uk.gov.justice.core.courts.Defendant.defendant().withId(MULTI_OFFENCE_DEFENDANT_ID)
            .withOffences(offences)
            .withProsecutionCaseId(CASE_ID)
            .withPersonDefendant(PersonDefendant.personDefendant().build()).build();

    private static final ProsecutionCase prosecutionCase = prosecutionCase()
            .withCaseStatus("caseStatus")
            .withId(CASE_ID)
            .withOriginatingOrganisation("originatingOrganisation")
            .withDefendants(defendants)
            .withInitiationCode(InitiationCode.C)
            .withProsecutionCaseIdentifier(prosecutionCaseIdentifier()
                    .withProsecutionAuthorityReference("reference")
                    .withProsecutionAuthorityCode("code")
                    .withProsecutionAuthorityId(randomUUID())
                    .withCaseURN("90GD8989122")
                    .build())
            .build();

    private ProsecutionCase getProsecutionCaseWithMultiOffence() {
        return prosecutionCase()
                .withCaseStatus("caseStatus")
                .withId(CASE_ID)
                .withOriginatingOrganisation("originatingOrganisation")
                .withDefendants(Arrays.asList(multiOffenceDefendant))
                .withInitiationCode(InitiationCode.C)
                .withProsecutionCaseIdentifier(prosecutionCaseIdentifier()
                        .withProsecutionAuthorityReference("reference")
                        .withProsecutionAuthorityCode("code")
                        .withProsecutionAuthorityId(randomUUID())
                        .build())
                .build();
    }

    private Address buildAddress() {
        return address()
                .withPostcode("CR03FH")
                .withAddress4("Address4")
                .withAddress3("Address3")
                .withAddress2("Address2")
                .withAddress1("Address1")
                .withWelshAddress1("WelshAddress1")
                .withWelshAddress2("WelshAddress2")
                .build();
    }

    private Prosecutor buildProsecutor(UUID prosecutorId){
        return Prosecutor.prosecutor()
                .withProsecutorId(prosecutorId)
                .withProsecutorName("ProsecutorName")
                .withProsecutorCode("ProsecutorCode")
                .withAddress(buildAddress())
                .build();
    }

    private ProsecutionCase getProsecutionCaseWithProsecutor(Prosecutor prosecutor, UUID prosecutionAuthorityId) {
        return prosecutionCase()
                .withCaseStatus("caseStatus")
                .withId(CASE_ID)
                .withOriginatingOrganisation("originatingOrganisation")
                .withDefendants(defendants)
                .withInitiationCode(InitiationCode.C)
                .withProsecutionCaseIdentifier(prosecutionCaseIdentifier()
                        .withProsecutionAuthorityReference("reference")
                        .withProsecutionAuthorityCode("code")
                        .withProsecutionAuthorityId(prosecutionAuthorityId)
                        .withCaseURN("90GD8989122")
                        .build())
                .withProsecutor(prosecutor)
                .build();
    }

    @Test
    public void shouldHandleCommand() {
        assertThat(new ReceiveRepresentationOrderHandler(), isHandler(COMMAND_HANDLER)
                .with(method("handle")
                        .thatHandles("progression.command.handler.receive-representationOrder-for-defendant")
                ));
    }

    @Test
    public void shouldProcessCommandWhenOrganisationIsNotSetupAndNoAssociatedOrgExpectNoAssociationOrDisassociationEvent() throws EventStreamException {

        final CaseAggregate caseAggregate = new CaseAggregate();
        when(eventSource.getStreamById(any())).thenReturn(eventStream);
        when(aggregateService.get(eventStream, CaseAggregate.class)).thenReturn(caseAggregate);

        caseAggregate.createProsecutionCase(prosecutionCase);
        final ReceiveRepresentationOrderForDefendant receiveRepresentationOrderForDefendant = payloadForReceiveRepresentationOrder(null, null, DEFENDANT_ID);

        final JsonObject prosecutionCaseJson = objectToJsonObjectConverter.convert(prosecutionCase);
        final Envelope<ReceiveRepresentationOrderForDefendant> envelope = Envelope.envelopeFrom(getProgressionCommandHandlerReceiveRepresentationOrderForDefendant(), receiveRepresentationOrderForDefendant);
        when(legalStatusReferenceDataService.getLegalStatusByStatusIdAndStatusCode(any(JsonEnvelope.class), any())).thenReturn(Optional.of(getLegalStatus()));
        when(usersGroupService.getOrganisationDetailsForLAAContractNumber(any(), any())).thenReturn(OrganisationDetails.newBuilder().build());
        receiveRepresentationOrderHandler.handle(envelope);

        final Stream<JsonEnvelope> envelopeStream = verifyAppendAndGetArgumentFrom(eventStream);

        Optional<JsonEnvelope> associatedOrDisAssociatedEnvelope = envelopeStream.filter
                (a -> a.metadata().name().equals("progression.event.defendant-defence-organisation-associated") || a.metadata().name().equals("progression.event.defendant-defence-organisation-disassociated"))
                .findAny();

        assertFalse(associatedOrDisAssociatedEnvelope.isPresent());

    }

    @Test
    public void shouldProcessCommand_whenOrganisationIsSetupAndAssociated_expectAssociationEvent() throws EventStreamException {

        final CaseAggregate caseAggregate = new CaseAggregate();
        when(eventSource.getStreamById(any())).thenReturn(eventStream);
        when(aggregateService.get(eventStream, CaseAggregate.class)).thenReturn(caseAggregate);

        caseAggregate.createProsecutionCase(prosecutionCase);
        final UUID organisationId = randomUUID();
        final ReceiveRepresentationOrderForDefendant receiveRepresentationOrderForDefendant = payloadForReceiveRepresentationOrder(organisationId, OFFENCE_ID, DEFENDANT_ID);
        final Envelope<ReceiveRepresentationOrderForDefendant> envelope = Envelope.envelopeFrom(getProgressionCommandHandlerReceiveRepresentationOrderForDefendant(), receiveRepresentationOrderForDefendant);
        when(legalStatusReferenceDataService.getLegalStatusByStatusIdAndStatusCode(any(), any())).thenReturn(Optional.of(getLegalStatus()));
        when(usersGroupService.getOrganisationDetailsForLAAContractNumber(any(), any())).thenReturn(OrganisationDetails.newBuilder()
                .withId(organisationId)
                .withName("Test")
                .build());

        receiveRepresentationOrderHandler.handle(envelope);

        assertFirstReceiveRepresentationOrderHandlerCommand();

        receiveRepresentationOrderHandler.handle(envelope);
        assertSecondSendReceiveRepresentationOrderHandlerCommand();
    }

    @Test
    public void shouldProcessCommand_whenOrganisationIsSetupAndAssociated_expectCaseDefendantUpdatedEventWithProsecutorId() throws EventStreamException {
        UUID prosecutorId = randomUUID();
        UUID prosecutorAuthorityId = randomUUID();
        ProsecutionCase prosecutionCase = getProsecutionCaseWithProsecutor(buildProsecutor(prosecutorId), prosecutorAuthorityId);

        final CaseAggregate caseAggregate = new CaseAggregate();
        when(eventSource.getStreamById(any())).thenReturn(eventStream);
        when(aggregateService.get(eventStream, CaseAggregate.class)).thenReturn(caseAggregate);

        caseAggregate.createProsecutionCase(prosecutionCase);
        final UUID organisationId = randomUUID();
        final ReceiveRepresentationOrderForDefendant receiveRepresentationOrderForDefendant = payloadForReceiveRepresentationOrder(organisationId, OFFENCE_ID, DEFENDANT_ID);
        final Envelope<ReceiveRepresentationOrderForDefendant> envelope = Envelope.envelopeFrom(getProgressionCommandHandlerReceiveRepresentationOrderForDefendant(), receiveRepresentationOrderForDefendant);
        when(legalStatusReferenceDataService.getLegalStatusByStatusIdAndStatusCode(any(), any())).thenReturn(Optional.of(getLegalStatus()));
        when(usersGroupService.getOrganisationDetailsForLAAContractNumber(any(), any())).thenReturn(OrganisationDetails.newBuilder()
                .withId(organisationId)
                .withName("Test")
                .build());

        receiveRepresentationOrderHandler.handle(envelope);

        JsonObject event = getEventAsJsonObjectFromStreamInGivenTimes(1, "progression.event.prosecution-case-defendant-updated");
        assertThat(event, notNullValue());
        assertThat(event.getString("prosecutionAuthorityId"), is(prosecutorId.toString()));
        assertThat(event.getString("caseUrn"), is("90GD8989122"));
    }

    @Test
    public void shouldProcessCommand_whenOrganisationIsSetupAndAssociated_expectCaseDefendantUpdatedEventWithProsecutorAuthorityId() throws EventStreamException {
        UUID prosecutorId = randomUUID();
        UUID prosecutorAuthorityId = randomUUID();
        ProsecutionCase prosecutionCase = getProsecutionCaseWithProsecutor(null, prosecutorAuthorityId);

        final CaseAggregate caseAggregate = new CaseAggregate();
        when(eventSource.getStreamById(any())).thenReturn(eventStream);
        when(aggregateService.get(eventStream, CaseAggregate.class)).thenReturn(caseAggregate);

        caseAggregate.createProsecutionCase(prosecutionCase);
        final UUID organisationId = randomUUID();
        final ReceiveRepresentationOrderForDefendant receiveRepresentationOrderForDefendant = payloadForReceiveRepresentationOrder(organisationId, OFFENCE_ID, DEFENDANT_ID);
        final Envelope<ReceiveRepresentationOrderForDefendant> envelope = Envelope.envelopeFrom(getProgressionCommandHandlerReceiveRepresentationOrderForDefendant(), receiveRepresentationOrderForDefendant);
        when(legalStatusReferenceDataService.getLegalStatusByStatusIdAndStatusCode(any(), any())).thenReturn(Optional.of(getLegalStatus()));
        when(usersGroupService.getOrganisationDetailsForLAAContractNumber(any(), any())).thenReturn(OrganisationDetails.newBuilder()
                .withId(organisationId)
                .withName("Test")
                .build());

        receiveRepresentationOrderHandler.handle(envelope);

        JsonObject event = getEventAsJsonObjectFromStreamInGivenTimes(1, "progression.event.prosecution-case-defendant-updated");
        assertThat(event, notNullValue());
        assertThat(event.getString("prosecutionAuthorityId"), is(prosecutorAuthorityId.toString()));
        assertThat(event.getString("caseUrn"), is("90GD8989122"));
    }

    @Test
    public void shouldProcessCommand_whenOrganisationIsSetupAndAssociated_expectCaseDefendantUpdatedEventWithNullProsecutorAuthorityId() throws EventStreamException {
        UUID prosecutorId = randomUUID();
        UUID prosecutorAuthorityId = randomUUID();
        ProsecutionCase prosecutionCase = getProsecutionCaseWithProsecutor(null, null);

        final CaseAggregate caseAggregate = new CaseAggregate();
        when(eventSource.getStreamById(any())).thenReturn(eventStream);
        when(aggregateService.get(eventStream, CaseAggregate.class)).thenReturn(caseAggregate);

        caseAggregate.createProsecutionCase(prosecutionCase);
        final UUID organisationId = randomUUID();
        final ReceiveRepresentationOrderForDefendant receiveRepresentationOrderForDefendant = payloadForReceiveRepresentationOrder(organisationId, OFFENCE_ID, DEFENDANT_ID);
        final Envelope<ReceiveRepresentationOrderForDefendant> envelope = Envelope.envelopeFrom(getProgressionCommandHandlerReceiveRepresentationOrderForDefendant(), receiveRepresentationOrderForDefendant);
        when(legalStatusReferenceDataService.getLegalStatusByStatusIdAndStatusCode(any(), any())).thenReturn(Optional.of(getLegalStatus()));
        when(usersGroupService.getOrganisationDetailsForLAAContractNumber(any(), any())).thenReturn(OrganisationDetails.newBuilder()
                .withId(organisationId)
                .withName("Test")
                .build());

        receiveRepresentationOrderHandler.handle(envelope);

        JsonObject event = getEventAsJsonObjectFromStreamInGivenTimes(1, "progression.event.prosecution-case-defendant-updated");
        assertThat(event, notNullValue());
        assertThat(event.getString("prosecutionAuthorityId", null), nullValue());
        assertThat(event.getString("caseUrn"), is("90GD8989122"));
    }

    @Test
    public void shouldTestDefenceOrganisationAssociated_Event_whenOrganisationIsSetupAndAssociatedWithMultiOffence_expectOneAssociationEvent() throws EventStreamException {
        final ProsecutionCase prosecutionCase = getProsecutionCaseWithMultiOffence();

        final CaseAggregate caseAggregate = new CaseAggregate();
        when(eventSource.getStreamById(any())).thenReturn(eventStream);
        when(aggregateService.get(eventStream, CaseAggregate.class)).thenReturn(caseAggregate);

        caseAggregate.createProsecutionCase(prosecutionCase);
        ReceiveRepresentationOrderForDefendant receiveRepresentationOrderForDefendant = payloadForReceiveRepresentationOrder(null, SAME_DEFENDANT_OFFENCE_ID_1, MULTI_OFFENCE_DEFENDANT_ID);

        final Envelope<ReceiveRepresentationOrderForDefendant> envelope = Envelope.envelopeFrom(getProgressionCommandHandlerReceiveRepresentationOrderForDefendant(), receiveRepresentationOrderForDefendant);
        when(legalStatusReferenceDataService.getLegalStatusByStatusIdAndStatusCode(any(), any())).thenReturn(Optional.of(getLegalStatus()));
        final UUID organisationId = randomUUID();
        when(usersGroupService.getOrganisationDetailsForLAAContractNumber(any(), any())).thenReturn(OrganisationDetails.newBuilder()
                .withId(organisationId)
                .withName("Test")
                .build());

        receiveRepresentationOrderHandler.handle(envelope);

        assertFirstReceiveRepresentationOrderHandlerCommand();

        receiveRepresentationOrderForDefendant = payloadForReceiveRepresentationOrder(null, SAME_DEFENDANT_OFFENCE_ID_2, MULTI_OFFENCE_DEFENDANT_ID);

        final Envelope<ReceiveRepresentationOrderForDefendant> newEnvelope = Envelope.envelopeFrom(getProgressionCommandHandlerReceiveRepresentationOrderForDefendant(), receiveRepresentationOrderForDefendant);

        receiveRepresentationOrderHandler.handle(newEnvelope);

        assertSecondSendReceiveRepresentationOrderHandlerCommand();
    }

    @Test
    public void shouldTestDefenceOrganisationAssociated_Event_whenDefenceHasMultiOffence_expectTwoTimesAssociationEvent() throws EventStreamException {
        final ProsecutionCase prosecutionCase = getProsecutionCaseWithMultiOffence();

        final CaseAggregate caseAggregate = new CaseAggregate();
        when(eventSource.getStreamById(any())).thenReturn(eventStream);
        when(aggregateService.get(eventStream, CaseAggregate.class)).thenReturn(caseAggregate);

        caseAggregate.createProsecutionCase(prosecutionCase);
        ReceiveRepresentationOrderForDefendant receiveRepresentationOrderForDefendant = payloadForReceiveRepresentationOrder(null, SAME_DEFENDANT_OFFENCE_ID_1, MULTI_OFFENCE_DEFENDANT_ID);

        final Envelope<ReceiveRepresentationOrderForDefendant> envelope = Envelope.envelopeFrom(getProgressionCommandHandlerReceiveRepresentationOrderForDefendant(), receiveRepresentationOrderForDefendant);
        when(legalStatusReferenceDataService.getLegalStatusByStatusIdAndStatusCode(any(), any())).thenReturn(Optional.of(getLegalStatus()));
        final UUID organisationId = randomUUID();
        when(usersGroupService.getOrganisationDetailsForLAAContractNumber(any(), any())).thenReturn(OrganisationDetails.newBuilder()
                .withId(organisationId)
                .withName("Test")
                .build());

        receiveRepresentationOrderHandler.handle(envelope);

        assertFirstReceiveRepresentationOrderHandlerCommand();

        receiveRepresentationOrderForDefendant = payloadForReceiveRepresentationOrder(organisationId, SAME_DEFENDANT_OFFENCE_ID_2, MULTI_OFFENCE_DEFENDANT_ID);

        final Envelope<ReceiveRepresentationOrderForDefendant> newEnvelope = Envelope.envelopeFrom(getProgressionCommandHandlerReceiveRepresentationOrderForDefendant(), receiveRepresentationOrderForDefendant);

        receiveRepresentationOrderHandler.handle(newEnvelope);

        assertSecondSendReceiveRepresentationOrderHandlerCommand();

    }

    private void assertFirstReceiveRepresentationOrderHandlerCommand() throws EventStreamException {
        final Stream<JsonEnvelope> envelopeStream = verifyAppendAndGetArgumentFrom(eventStream);

        Optional<JsonEnvelope> associatedOrDisAssociatedEnvelope = envelopeStream.filter
                (a -> a.metadata().name().equals("progression.event.defendant-defence-organisation-associated"))
                .findAny();

        assertTrue(associatedOrDisAssociatedEnvelope.isPresent());
    }

    private void assertSecondSendReceiveRepresentationOrderHandlerCommand() throws EventStreamException {
        final ArgumentCaptor<Stream> argumentCaptor2 = ArgumentCaptor.forClass(Stream.class);
        (Mockito.verify(eventStream, times(2))).append(argumentCaptor2.capture());
        final List<Stream> streams2 = argumentCaptor2.getAllValues();
        final Envelope record1JsonEnvelope = (JsonEnvelope) streams2.get(1).findFirst().orElse(null);

        assertThat(record1JsonEnvelope.metadata().name(), not("progression.event.defendant-defence-organisation-associated"));
    }


    private JsonObject getEventAsJsonObjectFromStreamInGivenTimes(int times, String eventName) throws EventStreamException {
        final ArgumentCaptor<Stream> argumentCaptor = ArgumentCaptor.forClass(Stream.class);
        (Mockito.verify(eventStream, times(times))).append(argumentCaptor.capture());
        final List<Stream> streams2 = argumentCaptor.getAllValues();
        final List<JsonEnvelope> eventsList = (List<JsonEnvelope>) (streams2.get(0).collect(Collectors.toList()));
        Optional<JsonEnvelope> eventEnvelope = eventsList.stream().filter(x -> x.metadata().name().equalsIgnoreCase(eventName)).findFirst();
        assertThat(eventEnvelope.isPresent(), is(true));
        return eventEnvelope.get().payloadAsJsonObject();
    }


    private static JsonObject getLegalStatus() {
        return Json.createObjectBuilder()
                .add("id", LEGAL_STATUS_ID.toString())
                .add("statusDescription", "description")
                .add("defendantLevelStatus", "Granted")
                .build();
    }


    private static JsonObject getAssociationWithOutAnyOrganisation() {
        return Json.createObjectBuilder()
                .add(ASSOCIATION, Json.createObjectBuilder())
                .build();
    }

    private static JsonObject getAssociationWithMatchingOrganisation(final String organisationId) {
        return Json.createObjectBuilder()
                .add(ORGANISATION_ID, organisationId)
                .build();
    }

    private static JsonObject getAssociationWithoutMatchingOrganisation() {
        return Json.createObjectBuilder()
                .add(ORGANISATION_ID, randomUUID().toString())
                .add(REPRESENTATION_TYPE, RepresentationType.REPRESENTATION_ORDER.toString())
                .build();
    }


    private static ReceiveRepresentationOrderForDefendant payloadForReceiveRepresentationOrder(final UUID associatedOrganisationId, final UUID offenceId
            , final UUID defendantId) {
        return ReceiveRepresentationOrderForDefendant.receiveRepresentationOrderForDefendant()
                .withApplicationReference("AB746921")
                .withDefendantId(defendantId)
                .withProsecutionCaseId(CASE_ID)
                .withOffenceId(offenceId)
                .withStatusCode("GR")
                .withStatusDate(LocalDate.parse("2019-07-01"))
                .withEffectiveStartDate(LocalDate.parse("2019-09-01"))
                .withEffectiveEndDate(LocalDate.parse("2019-12-01"))
                .withAssociatedOrganisationId(associatedOrganisationId)
                .withDefenceOrganisation(DefenceOrganisation.defenceOrganisation()
                        .withLaaContractNumber("LAA1234")
                        .withOrganisation(Organisation.organisation()
                                .withName("Test1")
                                .withIncorporationNumber("LAAINC1")
                                .build())
                        .build())
                .build();


    }

    private static Metadata getProgressionCommandHandlerReceiveRepresentationOrderForDefendant() {
        return Envelope
                .metadataBuilder()
                .withName("progression.command.handler.receive-representationOrder-for-defendant")
                .withId(randomUUID())
                .build();
    }
}