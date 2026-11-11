package uk.gov.moj.cpp.progression.handler;

import static java.util.Collections.singletonList;
import static java.util.UUID.randomUUID;
import static uk.gov.justice.services.messaging.JsonObjects.createArrayBuilder;
import static uk.gov.justice.services.messaging.JsonObjects.createObjectBuilder;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static uk.gov.justice.core.courts.CourtApplication.courtApplication;
import static uk.gov.justice.core.courts.Offence.offence;
import static uk.gov.justice.core.courts.ProsecutionCase.prosecutionCase;
import static uk.gov.justice.services.messaging.Envelope.envelopeFrom;
import static uk.gov.justice.services.test.utils.core.helper.EventStreamMockHelper.verifyAppendAndGetArgumentFrom;

import uk.gov.justice.core.courts.ApplicationDefenceOrganisationChanged;
import uk.gov.justice.core.courts.CourtApplicationParty;
import uk.gov.justice.core.courts.CourtApplicationProceedingsInitiated;
import uk.gov.justice.core.courts.Defendant;
import uk.gov.justice.core.courts.DefendantDefenceOrganisationChanged;
import uk.gov.justice.core.courts.InitiationCode;
import uk.gov.justice.core.courts.MasterDefendant;
import uk.gov.justice.core.courts.PersonDefendant;
import uk.gov.justice.core.courts.ProsecutionCase;
import uk.gov.justice.core.courts.ProsecutionCaseCreated;
import uk.gov.justice.core.courts.ProsecutionCaseIdentifier;
import uk.gov.justice.progression.courts.AssociateDefenceOrganisation;
import uk.gov.justice.progression.courts.DisassociateDefenceOrganisation;
import uk.gov.justice.progression.courts.RepresentationType;
import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.core.aggregate.AggregateService;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.eventsourcing.source.core.EventSource;
import uk.gov.justice.services.eventsourcing.source.core.EventStream;
import uk.gov.justice.services.eventsourcing.source.core.exception.EventStreamException;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.messaging.Metadata;
import uk.gov.justice.services.test.utils.core.enveloper.EnveloperFactory;
import uk.gov.moj.cpp.progression.aggregate.ApplicationAggregate;
import uk.gov.moj.cpp.progression.aggregate.CaseAggregate;
import uk.gov.moj.cpp.progression.command.handler.DisassociateDefenceOrganisationForApplication;
import uk.gov.moj.cpp.progression.command.handler.service.UsersGroupService;
import uk.gov.moj.cpp.progression.domain.pojo.OrganisationDetails;
import uk.gov.moj.cpp.progression.events.DefenceOrganisationAssociatedByDefenceContext;
import uk.gov.moj.cpp.progression.events.DefenceOrganisationDissociatedByDefenceContext;
import uk.gov.moj.cpp.progression.events.DefenceOrganisationDissociatedForApplicationByDefenceContext;
import uk.gov.moj.cpp.progression.events.DefendantDefenceOrganisationAssociated;
import uk.gov.moj.cpp.progression.service.ProsecutionCaseQueryService;

import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.json.JsonObject;


import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class DefenceOrganisationHandlerTest {

    @InjectMocks
    private DefenceOrganisationHandler defenceOrganisationHandler;

    @Mock
    private EventStream eventStream;

    @Mock
    private AggregateService aggregateService;

    @Mock
    private EventSource eventSource;

    @Mock
    private UsersGroupService usersGroupService;

    @Mock
    private ProsecutionCaseQueryService prosecutionCaseQueryService;

    @Mock
    private JsonObjectToObjectConverter jsonObjectToObjectConverter;

    @Mock
    private CaseAggregate aggregate;

    @Mock
    private ApplicationAggregate applicationAggregate = new ApplicationAggregate();

    @Spy
    private Enveloper enveloper = EnveloperFactory.createEnveloperWithEvents(
            DefendantDefenceOrganisationChanged.class, DefenceOrganisationAssociatedByDefenceContext.class,
            DefenceOrganisationDissociatedByDefenceContext.class,
            ApplicationDefenceOrganisationChanged.class,
            DefenceOrganisationDissociatedForApplicationByDefenceContext.class
            );

    private static ProsecutionCase createProsecutionCase(final List<Defendant> defendants) {
        return prosecutionCase()
                .withCaseStatus("caseStatus")
                .withId(randomUUID())
                .withOriginatingOrganisation("originatingOrganisation")
                .withDefendants(defendants)
                .withInitiationCode(InitiationCode.C)
                .withProsecutionCaseIdentifier(ProsecutionCaseIdentifier.prosecutionCaseIdentifier()
                        .withProsecutionAuthorityReference("reference")
                        .withProsecutionAuthorityCode("code")
                        .withProsecutionAuthorityId(randomUUID())
                        .withCaseURN("caseUrn")
                        .build())
                .build();
    }

    private static final Defendant defendant = Defendant.defendant()
            .withId(randomUUID())
            .withMasterDefendantId(randomUUID())
            .withPersonDefendant(PersonDefendant.personDefendant()
                    .withPersonDetails(uk.gov.justice.core.courts.Person.person()
                            .withFirstName("firstName")
                            .withLastName("lastName")
                            .withDateOfBirth(LocalDate.now().minusYears(20))
                            .build())
                    .build())
            .withCourtProceedingsInitiated(ZonedDateTime.now())
            .withOffences(singletonList(offence().withId(randomUUID()).build()))
            .build();

    private static final List<Defendant> defendants = new ArrayList<Defendant>() {{
        add(defendant);
    }};

    private static final ProsecutionCase prosecutionCase = createProsecutionCase(defendants);

    @BeforeEach
    public void setup() {
        when(eventSource.getStreamById(any())).thenReturn(eventStream);
    }

    @Test
    public void shouldHandleAssociationReturnEmpty() throws EventStreamException {
        final UUID orgId = randomUUID();
        final UUID caseId = randomUUID();
        final UUID defendantId = randomUUID();

        final AssociateDefenceOrganisation associateDefenceOrganisation = AssociateDefenceOrganisation.associateDefenceOrganisation()
                .withCaseId(caseId)
                .withOrganisationId(orgId)
                .withOrganisationName("orgName")
                .withStartDate(ZonedDateTime.now())
                .withRepresentationType(RepresentationType.PRIVATE)
                .withDefendantId(defendantId)
                .build();
        final Metadata metadata = Envelope
                .metadataBuilder()
                .withName("progression.command.handler.associate-defence-organisation")
                .withId(randomUUID())
                .build();
        final Envelope<AssociateDefenceOrganisation> envelope = envelopeFrom(metadata, associateDefenceOrganisation);

        when(aggregateService.get(eventStream, CaseAggregate.class)).thenReturn(aggregate);
        when(usersGroupService.getOrganisationDetailsForOrganisationId(envelope, orgId.toString())).thenReturn(new OrganisationDetails(randomUUID(),"Org1", "test"));
        when(prosecutionCaseQueryService.getProsecutionCase(any(),any())).thenReturn(Optional.ofNullable(createProsecutionCase(caseId,defendantId)));

        final List<Defendant> defendants = new ArrayList<>();
        defendants.add(Defendant.defendant()
                .withId(defendantId)
                .build());
        final ProsecutionCase prosecutionCase = ProsecutionCase.prosecutionCase()
                .withId(caseId)
                .withDefendants(defendants)
                .build();

        when(jsonObjectToObjectConverter.convert(any(), any())).thenReturn(prosecutionCase);

        defenceOrganisationHandler.handleAssociation(envelope);

        final Stream<JsonEnvelope> envelopeStream = verifyAppendAndGetArgumentFrom(eventStream);


        final List<Envelope> envelopes = envelopeStream.map(value -> (Envelope) value).collect(Collectors.toList());

        assertThat(envelopes.size(), is(0));
    }

    @Test
    public void shouldHandleAssociation() throws EventStreamException {
        aggregate = new CaseAggregate();
        aggregate.apply(new ProsecutionCaseCreated(prosecutionCase, null));

        final UUID orgId = randomUUID();

        final AssociateDefenceOrganisation associateDefenceOrganisation = AssociateDefenceOrganisation.associateDefenceOrganisation()
                .withCaseId(prosecutionCase.getId())
                .withOrganisationId(orgId)
                .withOrganisationName("orgName")
                .withStartDate(ZonedDateTime.now())
                .withRepresentationType(RepresentationType.PRIVATE)
                .withDefendantId(defendant.getId())
                .build();
        final Metadata metadata = Envelope
                .metadataBuilder()
                .withName("progression.command.handler.associate-defence-organisation")
                .withId(randomUUID())
                .build();
        final Envelope<AssociateDefenceOrganisation> envelope = envelopeFrom(metadata, associateDefenceOrganisation);

        when(aggregateService.get(eventStream, CaseAggregate.class)).thenReturn(aggregate);
        when(usersGroupService.getOrganisationDetailsForOrganisationId(envelope, orgId.toString())).thenReturn(new OrganisationDetails(randomUUID(),"Org1", "test"));
        when(prosecutionCaseQueryService.getProsecutionCase(any(),any())).thenReturn(Optional.ofNullable(createProsecutionCase(prosecutionCase.getId(), defendant.getId())));

        when(jsonObjectToObjectConverter.convert(any(), any())).thenReturn(prosecutionCase);

        defenceOrganisationHandler.handleAssociation(envelope);

        final Stream<JsonEnvelope> envelopeStream = verifyAppendAndGetArgumentFrom(eventStream);


        final List<Envelope> envelopes = envelopeStream.map(value -> (Envelope) value).collect(Collectors.toList());

        assertThat(envelopes.size(), is(2));

        final JsonEnvelope  defendantDefenceOrgChanged = (JsonEnvelope)envelopes.stream().filter(env -> env.metadata().name().equals("progression.event.defendant-defence-organisation-changed")).findFirst().get();

        assertThat(defendantDefenceOrgChanged.payloadAsJsonObject(), notNullValue());
        assertThat(defendantDefenceOrgChanged.metadata().name(), is("progression.event.defendant-defence-organisation-changed"));

    }

    @Test
    public void shouldHandleDisassociation() throws EventStreamException {
        aggregate = new CaseAggregate();
        aggregate.apply(new ProsecutionCaseCreated(prosecutionCase, null));

        final UUID orgId = randomUUID();
        final DisassociateDefenceOrganisation disassociateDefenceOrganisation = DisassociateDefenceOrganisation.disassociateDefenceOrganisation()
                .withCaseId(prosecutionCase.getId())
                .withOrganisationId(orgId)
                .withDefendantId(defendant.getId())
                .build();
        final Metadata metadata = Envelope
                .metadataBuilder()
                .withName("progression.command.handler.associate-defence-organisation")
                .withId(randomUUID())
                .build();
        final Envelope<DisassociateDefenceOrganisation> envelope = envelopeFrom(metadata, disassociateDefenceOrganisation);

        when(aggregateService.get(eventStream, CaseAggregate.class)).thenReturn(aggregate);
        when(prosecutionCaseQueryService.getProsecutionCase(any(),any())).thenReturn(Optional.ofNullable(createProsecutionCase(prosecutionCase.getId(), defendant.getId())));

        when(jsonObjectToObjectConverter.convert(any(), any())).thenReturn(prosecutionCase);

        defenceOrganisationHandler.handleDisassociation(envelope);

        final Stream<JsonEnvelope> envelopeStream = verifyAppendAndGetArgumentFrom(eventStream);


        final List<Envelope> envelopes = envelopeStream.map(value -> (Envelope) value).collect(Collectors.toList());

        assertThat(envelopes.size(), is(2));

        final JsonEnvelope  defendantDefenceOrgChanged = (JsonEnvelope)envelopes.stream().filter(env -> env.metadata().name().equals("progression.event.defendant-defence-organisation-changed")).findFirst().get();

        assertThat(defendantDefenceOrgChanged.payloadAsJsonObject(), notNullValue());
        assertThat(defendantDefenceOrgChanged.metadata().name(), is("progression.event.defendant-defence-organisation-changed"));
    }


    @Test
    public void shouldHandleDisassociationForApplication() throws EventStreamException {
        final UUID defendantId = randomUUID();
        final UUID organisationId = randomUUID();
        final UUID applicationId = randomUUID();
        final UUID subjectId = randomUUID();

        applicationAggregate = new ApplicationAggregate();
        final CourtApplicationProceedingsInitiated courtApplicationProceedingsInitiated = CourtApplicationProceedingsInitiated.courtApplicationProceedingsInitiated()
                .withCourtApplication(courtApplication()
                        .withId(applicationId)
                        .withSubject(CourtApplicationParty.courtApplicationParty()
                                .withId(subjectId)
                                .withMasterDefendant(MasterDefendant.masterDefendant()
                                        .withMasterDefendantId(defendantId)
                                        .build())
                                .build())
                        .build())
                .build();
        final DefendantDefenceOrganisationAssociated defendantDefenceOrganisationAssociated = DefendantDefenceOrganisationAssociated.defendantDefenceOrganisationAssociated()
                .withDefendantId(defendantId)
                .withOrganisationId(organisationId)
                .build();

        applicationAggregate.apply(courtApplicationProceedingsInitiated);
        applicationAggregate.apply(defendantDefenceOrganisationAssociated);

        final DisassociateDefenceOrganisationForApplication disassociateDefenceOrganisationForApplication = DisassociateDefenceOrganisationForApplication.disassociateDefenceOrganisationForApplication()
                .withApplicationId(applicationId)
                .withOrganisationId(organisationId)
                .withDefendantId(defendantId)
                .build();
        final Metadata metadata = Envelope
                .metadataBuilder()
                .withName("progression.command.handler.disassociate-defence-organisation-for-application")
                .withId(randomUUID())
                .build();
        final Envelope<DisassociateDefenceOrganisationForApplication> envelope = envelopeFrom(metadata, disassociateDefenceOrganisationForApplication);

        when(aggregateService.get(eventStream, ApplicationAggregate.class)).thenReturn(applicationAggregate);

        defenceOrganisationHandler.handleDisassociationForApplication(envelope);

        final Stream<JsonEnvelope> envelopeStream = verifyAppendAndGetArgumentFrom(eventStream);


        final List<Envelope> envelopes = envelopeStream.map(value -> (Envelope) value).collect(Collectors.toList());

        assertThat(envelopes.size(), is(2));

        assertThat(envelopes.get(0).metadata().name(),is("progression.event.application-defence-organisation-changed"));
        assertThat(envelopes.get(1).metadata().name(),is("progression.event.defence-organisation-dissociated-for-application-by-defence-context"));
    }


    private JsonObject createProsecutionCase(final UUID caseId, final UUID defendantId){
        return createObjectBuilder()
                .add("prosecutionCase",
                        createObjectBuilder()
                                .add("id", caseId.toString())
                                .add("defendants", createArrayBuilder()
                                        .add(
                                                createObjectBuilder()
                                                        .add("id", defendantId.toString())
                                                        .add("prosecutionCaseId", caseId.toString())
                                        )
                                        .build())
                                .add("prosecutionCaseIdentifier", createObjectBuilder()
                                        .add("prosecutionAuthorityReference", "TFL12345")
                                        .build())
                                .add("summonsCode", "summonsCode")
                                .add("initiationCode", "S")
                                .add("statementOfFacts", "dummy statement of facts")
                                .add("statementOfFactsWelsh", "dummy statement of facts in welsh")

                ).build();
    }
}
