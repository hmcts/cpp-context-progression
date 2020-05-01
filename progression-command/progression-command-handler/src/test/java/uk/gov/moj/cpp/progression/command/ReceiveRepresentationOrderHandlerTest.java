package uk.gov.moj.cpp.progression.command;

import static java.util.UUID.randomUUID;
import static javax.json.Json.createObjectBuilder;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.core.annotation.Component.COMMAND_HANDLER;
import static uk.gov.justice.services.test.utils.core.helper.EventStreamMockHelper.verifyAppendAndGetArgumentFrom;
import static uk.gov.justice.services.test.utils.core.matchers.HandlerMatcher.isHandler;
import static uk.gov.justice.services.test.utils.core.matchers.HandlerMethodMatcher.method;

import uk.gov.justice.core.courts.DefenceOrganisation;
import uk.gov.justice.core.courts.Defendant;
import uk.gov.justice.core.courts.DefendantDefenceOrganisationChanged;
import uk.gov.justice.core.courts.InitiationCode;
import uk.gov.justice.core.courts.Offence;
import uk.gov.justice.core.courts.Organisation;
import uk.gov.justice.core.courts.PersonDefendant;
import uk.gov.justice.core.courts.ProsecutionCase;
import uk.gov.justice.core.courts.ProsecutionCaseDefendantUpdated;
import uk.gov.justice.core.courts.ProsecutionCaseIdentifier;
import uk.gov.justice.core.courts.ProsecutionCaseOffencesUpdated;
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
import uk.gov.moj.cpp.progression.command.handler.service.payloads.OrganisationDetails;
import uk.gov.moj.cpp.progression.events.DefendantDefenceOrganisationAssociated;
import uk.gov.moj.cpp.progression.events.DefendantDefenceOrganisationDisassociated;
import uk.gov.moj.cpp.progression.events.DefendantLaaAssociated;
import uk.gov.moj.cpp.progression.events.RepresentationType;
import uk.gov.moj.cpp.progression.handler.ReceiveRepresentationOrderHandler;
import uk.gov.moj.cpp.progression.service.LegalStatusReferenceDataService;
import uk.gov.moj.cpp.progression.service.ProsecutionCaseQueryService;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;

import javax.json.Json;
import javax.json.JsonObject;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
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
    private final ObjectMapper objectMapper = new ObjectMapperProducer().objectMapper();

    @Spy
    @InjectMocks
    private final JsonObjectToObjectConverter jsonObjectToObjectConverter = new JsonObjectToObjectConverter();

    @Spy
    @InjectMocks
    private final ObjectToJsonObjectConverter objectToJsonObjectConverter = new ObjectToJsonObjectConverter();


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
            ProsecutionCaseDefendantUpdated.class);

    @InjectMocks
    private ReceiveRepresentationOrderHandler receiveRepresentationOrderHandler;


    private CaseAggregate aggregate;

    private static final UUID CASE_ID = randomUUID();

    private static final UUID DEFENDANT_ID = randomUUID();

    private static final UUID DEFENDANT_ID_2 = randomUUID();

    private static final UUID OFFENCE_ID = randomUUID();

    private static final UUID LEGAL_STATUS_ID = randomUUID();

    private static final String ASSOCIATION = "association";

    public static final String ORGANISATION_ID = "organisationId";

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
    private static final ProsecutionCase prosecutionCase = ProsecutionCase.prosecutionCase()
            .withCaseStatus("caseStatus")
            .withId(CASE_ID)
            .withOriginatingOrganisation("originatingOrganisation")
            .withDefendants(defendants)
            .withInitiationCode(InitiationCode.C)
            .withProsecutionCaseIdentifier(ProsecutionCaseIdentifier.prosecutionCaseIdentifier()
                    .withProsecutionAuthorityReference("reference")
                    .withProsecutionAuthorityCode("code")
                    .withProsecutionAuthorityId(randomUUID())
                    .build())
            .build();


    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);
        aggregate = new CaseAggregate();
        when(eventSource.getStreamById(any())).thenReturn(eventStream);
        when(aggregateService.get(eventStream, CaseAggregate.class)).thenReturn(aggregate);
    }

    @Test
    public void shouldHandleCommand() {
        assertThat(new ReceiveRepresentationOrderHandler(), isHandler(COMMAND_HANDLER)
                .with(method("handle")
                        .thatHandles("progression.command.handler.receive-representationOrder-for-defendant")
                ));
    }

    @Test
    public void shouldProcessCommandWhenOrganisationIsNotSetupAndNoAssoicatedOrgExpectNoAssociationOrDisassociationEvent() throws EventStreamException {
        aggregate.createProsecutionCase(prosecutionCase);
        final ReceiveRepresentationOrderForDefendant receiveRepresentationOrderForDefendant = payloadForReceiveRepresentationOrder(null);
        final Metadata metadata = Envelope
                .metadataBuilder()
                .withName("progression.command.handler.receive-representationOrder-for-defendant")
                .withId(randomUUID())
                .build();
        final JsonObject prosecutionCaseJson = objectToJsonObjectConverter.convert(prosecutionCase);
        final Envelope<ReceiveRepresentationOrderForDefendant> envelope = Envelope.envelopeFrom(metadata, receiveRepresentationOrderForDefendant);
        when(legalStatusReferenceDataService.getLegalStatusByStatusIdAndStatusCode(any(JsonEnvelope.class), any(String.class))).thenReturn(Optional.of(getLegalStatus()));
        when(usersGroupService.getOrganisationDetailsForLAAContractNumber(any(JsonEnvelope.class), any(String.class))).thenReturn(OrganisationDetails.newBuilder().build());
        when(prosecutionCaseQueryService.getProsecutionCase(any(JsonEnvelope.class),any(String.class))).thenReturn(Optional.of(createObjectBuilder().add("prosecutionCase", prosecutionCaseJson).build()));
        receiveRepresentationOrderHandler.handle(envelope);

        final Stream<JsonEnvelope> envelopeStream = verifyAppendAndGetArgumentFrom(eventStream);

        Optional<JsonEnvelope>  associatedOrDisAssociatedEnvelope = envelopeStream.filter
                (a->a.metadata().name().equals("progression.event.defendant-defence-organisation-associated") || a.metadata().name().equals("progression.event.defendant-defence-organisation-disassociated"))
                .findAny();

        assertFalse(associatedOrDisAssociatedEnvelope.isPresent());

    }


    @Test
    public void shouldProcessCommand_whenOrganisationIsSetupAndAssociated_expectNoAssociationEvent() throws EventStreamException {
        aggregate.createProsecutionCase(prosecutionCase);
        final UUID organisationId = randomUUID();
        final ReceiveRepresentationOrderForDefendant receiveRepresentationOrderForDefendant = payloadForReceiveRepresentationOrder(organisationId);
        final Metadata metadata = Envelope
                .metadataBuilder()
                .withName("progression.command.handler.receive-representationOrder-for-defendant")
                .withId(randomUUID())
                .build();
        final JsonObject prosecutionCaseJson = objectToJsonObjectConverter.convert(prosecutionCase);
        final Envelope<ReceiveRepresentationOrderForDefendant> envelope = Envelope.envelopeFrom(metadata, receiveRepresentationOrderForDefendant);
        when(legalStatusReferenceDataService.getLegalStatusByStatusIdAndStatusCode(any(JsonEnvelope.class), any(String.class))).thenReturn(Optional.of(getLegalStatus()));
        when(usersGroupService.getOrganisationDetailsForLAAContractNumber(any(JsonEnvelope.class), any(String.class))).thenReturn(OrganisationDetails.newBuilder()
                .withId(organisationId)
                .withName("Test")
                .build());
        when(prosecutionCaseQueryService.getProsecutionCase(any(JsonEnvelope.class),any(String.class))).thenReturn(Optional.of(createObjectBuilder().add("prosecutionCase", prosecutionCaseJson).build()));

        receiveRepresentationOrderHandler.handle(envelope);

        final Stream<JsonEnvelope> envelopeStream = verifyAppendAndGetArgumentFrom(eventStream);

        Optional<JsonEnvelope>  associatedOrDisAssociatedEnvelope = envelopeStream.filter
                (a->a.metadata().name().equals("progression.event.defendant-defence-organisation-associated"))
                .findAny();

        assertFalse(associatedOrDisAssociatedEnvelope.isPresent());
    }

    private static JsonObject getLegalStatus() {
        return Json.createObjectBuilder()
                .add("id", LEGAL_STATUS_ID.toString())
                .add("statusDescription", "description")
                .add("defendantLevelStatus","Granted")
                .build();
    }


    private static JsonObject getAssociationWithOutAnyOrganisation() {
        return Json.createObjectBuilder()
                .add(ASSOCIATION, Json.createObjectBuilder())
                .build();
    }

    private static JsonObject getAssociationWithMatchingOrganisation(final String organisationId) {
        return Json.createObjectBuilder()
                .add(ORGANISATION_ID,organisationId)
                .build();
    }

    private static JsonObject getAssociationWithoutMatchingOrganisation() {
        return Json.createObjectBuilder()
                .add(ORGANISATION_ID,randomUUID().toString())
                .add(REPRESENTATION_TYPE, RepresentationType.REPRESENTATION_ORDER.toString())
                .build();
    }


    private static ReceiveRepresentationOrderForDefendant payloadForReceiveRepresentationOrder(final UUID organisationId) {
        return ReceiveRepresentationOrderForDefendant.receiveRepresentationOrderForDefendant()
                .withApplicationReference("AB746921")
                .withDefendantId(DEFENDANT_ID)
                .withProsecutionCaseId(CASE_ID)
                .withOffenceId(OFFENCE_ID)
                .withStatusCode("GR")
                .withStatusDate(LocalDate.parse("2019-07-01"))
                .withEffectiveStartDate(LocalDate.parse("2019-09-01"))
                .withEffectiveEndDate(LocalDate.parse("2019-12-01"))
                .withAssociatedOrganisationId(organisationId)
                .withDefenceOrganisation(DefenceOrganisation.defenceOrganisation()
                        .withLaaContractNumber("LAA1234")
                        .withOrganisation(Organisation.organisation()
                                .withName("Test1")
                                .withIncorporationNumber("LAAINC1")
                                .build())
                        .build())
                .build();


    }
}