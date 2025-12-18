package uk.gov.moj.cpp.progression.handler;

import static java.util.Collections.singletonList;
import static java.util.UUID.randomUUID;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.when;
import static uk.gov.justice.core.courts.CourtApplicationCase.courtApplicationCase;
import static uk.gov.justice.core.courts.CourtApplicationParty.courtApplicationParty;
import static uk.gov.justice.core.courts.DefendantCase.defendantCase;
import static uk.gov.justice.services.core.annotation.Component.COMMAND_HANDLER;
import static uk.gov.justice.services.test.utils.core.helper.EventStreamMockHelper.verifyAppendAndGetArgumentFrom;
import static uk.gov.justice.services.test.utils.core.matchers.HandlerMatcher.isHandler;
import static uk.gov.justice.services.test.utils.core.matchers.HandlerMethodMatcher.method;

import uk.gov.justice.core.courts.ApplicationDefenceOrganisationChanged;
import uk.gov.justice.core.courts.ApplicationReporderOffencesUpdated;
import uk.gov.justice.core.courts.CourtApplication;
import uk.gov.justice.core.courts.DefenceOrganisation;
import uk.gov.justice.core.courts.DefendantCase;
import uk.gov.justice.core.courts.MasterDefendant;
import uk.gov.justice.core.courts.Offence;
import uk.gov.justice.core.courts.Organisation;
import uk.gov.justice.core.courts.ReceiveRepresentationOrderForApplication;
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
import uk.gov.moj.cpp.progression.command.handler.service.UsersGroupService;
import uk.gov.moj.cpp.progression.domain.pojo.OrganisationDetails;
import uk.gov.moj.cpp.progression.events.ApplicationLaaAssociated;
import uk.gov.moj.cpp.progression.events.DefendantDefenceOrganisationAssociated;
import uk.gov.moj.cpp.progression.events.DefendantDefenceOrganisationDisassociated;
import uk.gov.moj.cpp.progression.service.LegalStatusReferenceDataService;
import uk.gov.moj.cpp.progression.service.OrganisationService;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonObject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class ReceiveRepresentationOrderHandlerForApplicationTest {

    private static final String ORG_OFFENCE_WORDING = "On 12/10/2020 at 10:100am on the corner of the hugh street outside the dog and duck in Croydon you did something wrong";
    private static final String ORG_OFFENCE_WORDING_WELSH = "On 12/10/2020 at 10:100am on the corner of the hugh street outside the";
    private static final String ORG_OFFENCE_CODE = "OFC0001";

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
    private OrganisationService organisationService;

    @Spy
    private final Enveloper enveloper = EnveloperFactory.createEnveloperWithEvents(
            ApplicationReporderOffencesUpdated.class,
            ApplicationDefenceOrganisationChanged.class,
            DefendantDefenceOrganisationAssociated.class,
            DefendantDefenceOrganisationDisassociated.class,
            ApplicationLaaAssociated.class);

    @InjectMocks
    private ReceiveRepresentationOrderForApplicationHandler receiveRepresentationOrderHandler;

    private static final UUID APPLICATION_ID = randomUUID();

    private static final UUID SUBJECT_ID = randomUUID();

    private static final UUID DEFENDANT_ID_1 = randomUUID();

    private static final UUID DEFENDANT_ID_2 = randomUUID();

    private static final UUID CASE_ID_1 = randomUUID();

    private static final UUID CASE_ID_2 = randomUUID();

    private static final UUID OFFENCE_ID = randomUUID();

    private static final String APPLICATION_REFERENCE = "APP00001";
    private static final String LAA_CONTRACT_NUMBER = "LAA1234";
    private static final String INCORPORATION_NUMBER = "LAAINC1";
    private static final String ORG_NAME = "Test1";
    private static final String STATUS_CODE = "FM";
    private static final String STATUS = "Refused";
    private static final String STATUS_DESCRIPTION = "Refused Description";
    private static final UUID STATUS_ID = randomUUID();

    private static final String UPDATED_STATUS_CODE = "G2";
    private static final String UPDATED_STATUS = "Granted";
    private static final String UPDATED_STATUS_DESCRIPTION = "Granted Description";
    private static final UUID UPDATED_STATUS_ID = randomUUID();

    private static final String UPDATED_STATUS_CODE_1 = "G2";

    private static final List<DefendantCase> defendantCases = new ArrayList<>();
    static {
        defendantCases.add(defendantCase()
                .withCaseId(CASE_ID_1)
                .withDefendantId(DEFENDANT_ID_1)
                .build());
        defendantCases.add(defendantCase()
                .withCaseId(CASE_ID_2)
                .withDefendantId(DEFENDANT_ID_2)
                .build());
    }
    private static final CourtApplication courtApplication = CourtApplication.courtApplication()
            .withId(APPLICATION_ID)
            .withApplicationReference(APPLICATION_REFERENCE)
            .withSubject(courtApplicationParty()
                    .withId(SUBJECT_ID)
                    .withMasterDefendant(MasterDefendant.masterDefendant()
                            .withDefendantCase(defendantCases)
                            .build())
                    .build())
            .withCourtApplicationCases(singletonList(courtApplicationCase()
                    .withOffences(singletonList(Offence.offence()
                            .withOffenceCode(ORG_OFFENCE_CODE)
                            .withWording(ORG_OFFENCE_WORDING)
                            .withWordingWelsh(ORG_OFFENCE_WORDING_WELSH)
                            .withId(OFFENCE_ID)
                            .build()))
                    .build()))
            .build();

    @Test
    void shouldHandleCommand() {
        assertThat(new ReceiveRepresentationOrderForApplicationHandler(), isHandler(COMMAND_HANDLER)
                .with(method("handle")
                        .thatHandles("progression.command.handler.receive-representationOrder-for-application")
                ));
    }

    @Test
    void shouldProcessCommandWhenOrganisationIsNotSetupAndNoAssociatedOrgExpectNoAssociationOrDisassociationEvent() throws EventStreamException {

        final ApplicationAggregate applicationAggregate = new ApplicationAggregate();
        applicationAggregate.createCourtApplication(courtApplication, null);
        when(eventSource.getStreamById(any())).thenReturn(eventStream);
        when(aggregateService.get(eventStream, ApplicationAggregate.class)).thenReturn(applicationAggregate);


        final JsonObject jsonObjectPayload = Json.createObjectBuilder().build();
        when(organisationService.getAssociatedOrganisationForApplication(any(), any())).thenReturn(jsonObjectPayload);

        final ReceiveRepresentationOrderForApplication receiveRepresentationOrderForApplication = payloadForReceiveRepresentationOrder(STATUS_CODE, APPLICATION_ID, SUBJECT_ID, OFFENCE_ID);

        final Envelope<ReceiveRepresentationOrderForApplication> envelope = Envelope.envelopeFrom(getProgressionCommandHandlerReceiveRepresentationOrderForApplication(), receiveRepresentationOrderForApplication);
        when(legalStatusReferenceDataService.getLegalStatusByStatusIdAndStatusCode(any(JsonEnvelope.class), any())).thenReturn(Optional.of(getLegalStatus(STATUS_ID,STATUS_DESCRIPTION, STATUS)));
        when(usersGroupService.getOrganisationDetailsForLAAContractNumber(any(), any())).thenReturn(OrganisationDetails.newBuilder().build());
        receiveRepresentationOrderHandler.handle(envelope);

        final Stream<JsonEnvelope> envelopeStream = verifyAppendAndGetArgumentFrom(eventStream);

        Optional<JsonEnvelope> associatedOrDisAssociatedEnvelope = envelopeStream.filter
                        (a -> a.metadata().name().equals("progression.event.defendant-defence-organisation-associated") || a.metadata().name().equals("progression.event.defendant-defence-organisation-disassociated"))
                .findAny();

        assertFalse(associatedOrDisAssociatedEnvelope.isPresent());
    }

    @Test
    void shouldProcessCommandWhenOffenceAttachedAndUpdatedWithLAAReferenceEvent() throws EventStreamException {

        final ApplicationAggregate applicationAggregate = new ApplicationAggregate();
        applicationAggregate.createCourtApplication(courtApplication, null);
        when(eventSource.getStreamById(any())).thenReturn(eventStream);
        when(aggregateService.get(eventStream, ApplicationAggregate.class)).thenReturn(applicationAggregate);

        final JsonObject jsonObjectPayload = Json.createObjectBuilder().build();
        when(organisationService.getAssociatedOrganisationForApplication(any(), any())).thenReturn(jsonObjectPayload);

        final ReceiveRepresentationOrderForApplication receiveRepresentationOrderForApplication = payloadForReceiveRepresentationOrder(STATUS_CODE, APPLICATION_ID, SUBJECT_ID, OFFENCE_ID);

        final Envelope<ReceiveRepresentationOrderForApplication> envelope = Envelope.envelopeFrom(getProgressionCommandHandlerReceiveRepresentationOrderForApplication(), receiveRepresentationOrderForApplication);
        when(legalStatusReferenceDataService.getLegalStatusByStatusIdAndStatusCode(any(JsonEnvelope.class), any())).thenReturn(Optional.of(getLegalStatus(STATUS_ID,STATUS_DESCRIPTION, STATUS)));
        when(usersGroupService.getOrganisationDetailsForLAAContractNumber(any(), any())).thenReturn(OrganisationDetails.newBuilder().build());
        receiveRepresentationOrderHandler.handle(envelope);

        JsonObject event = getEventAsJsonObjectFromStreamInGivenTimes(1, "progression.event.application-reporder-offences-updated", true);
        assertThat(event.getString("applicationId"), is(APPLICATION_ID.toString()));
        assertThat(event.getString("subjectId"), is(SUBJECT_ID.toString()));

        verifyApplicationCaseOffenceLAAReference(event.getJsonObject("laaReference"),STATUS_ID, STATUS_DESCRIPTION, STATUS, STATUS_CODE);
        verifyApplicationCaseDefendantOrganisations(event.getJsonArray("applicationCaseDefendantOrganisations"));

        //Verifying updated values
        ReceiveRepresentationOrderForApplication updateReceiveRepresentationOrderForApplication = payloadForReceiveRepresentationOrder(UPDATED_STATUS_CODE, APPLICATION_ID, SUBJECT_ID, OFFENCE_ID);
        Envelope<ReceiveRepresentationOrderForApplication> updateEnvelope = Envelope.envelopeFrom(getProgressionCommandHandlerReceiveRepresentationOrderForApplication(), updateReceiveRepresentationOrderForApplication);
        when(legalStatusReferenceDataService.getLegalStatusByStatusIdAndStatusCode(any(JsonEnvelope.class), any())).thenReturn(Optional.of(getLegalStatus(UPDATED_STATUS_ID,UPDATED_STATUS_DESCRIPTION, UPDATED_STATUS)));

        receiveRepresentationOrderHandler.handle(updateEnvelope);

        JsonObject updateEvent = getEventAsJsonObjectFromStreamInGivenTimes(2, "progression.event.application-reporder-offences-updated", true);
        assertThat(updateEvent.getString("applicationId"), is(APPLICATION_ID.toString()));
        assertThat(updateEvent.getString("subjectId"), is(SUBJECT_ID.toString()));

        verifyApplicationCaseOffenceLAAReference(updateEvent.getJsonObject("laaReference"),UPDATED_STATUS_ID, UPDATED_STATUS_DESCRIPTION, UPDATED_STATUS, UPDATED_STATUS_CODE);
        verifyApplicationCaseDefendantOrganisations(updateEvent.getJsonArray("applicationCaseDefendantOrganisations"));
    }

    @Test
    void shouldProcessCommandWhenOffenceAttachedAndUpdatedWithLAAReferenceEventTwice() throws EventStreamException {

        final ApplicationAggregate applicationAggregate = new ApplicationAggregate();
        applicationAggregate.createCourtApplication(courtApplication, null);
        when(eventSource.getStreamById(any())).thenReturn(eventStream);
        when(aggregateService.get(eventStream, ApplicationAggregate.class)).thenReturn(applicationAggregate);

        final JsonObject jsonObjectPayload = Json.createObjectBuilder().build();
        when(organisationService.getAssociatedOrganisationForApplication(any(), any())).thenReturn(jsonObjectPayload);

        final ReceiveRepresentationOrderForApplication receiveRepresentationOrderForApplication = payloadForReceiveRepresentationOrder(STATUS_CODE, APPLICATION_ID, SUBJECT_ID, OFFENCE_ID);

        final Envelope<ReceiveRepresentationOrderForApplication> envelope = Envelope.envelopeFrom(getProgressionCommandHandlerReceiveRepresentationOrderForApplication(), receiveRepresentationOrderForApplication);
        when(legalStatusReferenceDataService.getLegalStatusByStatusIdAndStatusCode(any(JsonEnvelope.class), any())).thenReturn(Optional.of(getLegalStatus(STATUS_ID,STATUS_DESCRIPTION, STATUS)));
        when(usersGroupService.getOrganisationDetailsForLAAContractNumber(any(), any())).thenReturn(OrganisationDetails.newBuilder().build());
        receiveRepresentationOrderHandler.handle(envelope);

        JsonObject event = getEventAsJsonObjectFromStreamInGivenTimes(1, "progression.event.application-reporder-offences-updated", true);
        assertThat(event.getString("applicationId"), is(APPLICATION_ID.toString()));
        assertThat(event.getString("subjectId"), is(SUBJECT_ID.toString()));

        verifyApplicationCaseOffenceLAAReference(event.getJsonObject("laaReference"),STATUS_ID, STATUS_DESCRIPTION, STATUS, STATUS_CODE);
        verifyApplicationCaseDefendantOrganisations(event.getJsonArray("applicationCaseDefendantOrganisations"));

        // Same envelope again
        receiveRepresentationOrderHandler.handle(envelope);

        JsonObject updateEvent = getEventAsJsonObjectFromStreamInGivenTimes(2, "progression.event.application-reporder-offences-updated", false);
        assertNull(updateEvent);
    }

    @Test
    void shouldProcessCommand_whenOrganisationIsSetupAndAssociated_expectAssociationEvent() throws EventStreamException {

        final ApplicationAggregate applicationAggregate = new ApplicationAggregate();
        when(eventSource.getStreamById(any())).thenReturn(eventStream);
        when(aggregateService.get(eventStream, ApplicationAggregate.class)).thenReturn(applicationAggregate);
        final JsonObject jsonObjectPayload = Json.createObjectBuilder().add("organisationId", randomUUID().toString()).build();
        when(organisationService.getAssociatedOrganisationForApplication(any(), any())).thenReturn(jsonObjectPayload);
        applicationAggregate.createCourtApplication(courtApplication, null);
        final UUID organisationId = randomUUID();
        final ReceiveRepresentationOrderForApplication receiveRepresentationOrderForApplication = payloadForReceiveRepresentationOrder(STATUS_CODE, APPLICATION_ID, SUBJECT_ID, OFFENCE_ID);
        final Envelope<ReceiveRepresentationOrderForApplication> envelope = Envelope.envelopeFrom(getProgressionCommandHandlerReceiveRepresentationOrderForApplication(), receiveRepresentationOrderForApplication);
        when(legalStatusReferenceDataService.getLegalStatusByStatusIdAndStatusCode(any(), any())).thenReturn(Optional.of(getLegalStatus(STATUS_ID,STATUS_DESCRIPTION, STATUS)));
        when(usersGroupService.getOrganisationDetailsForLAAContractNumber(any(), any())).thenReturn(getOrganisationDetails(organisationId));

        receiveRepresentationOrderHandler.handle(envelope);
        assertFirstReceiveRepresentationOrderHandlerCommand();

        receiveRepresentationOrderHandler.handle(envelope);

        final ArgumentCaptor<Stream> argumentCaptor2 = ArgumentCaptor.forClass(Stream.class);
        (Mockito.verify(eventStream, times(2))).append(argumentCaptor2.capture());
        final List<Stream> streams2 = argumentCaptor2.getAllValues();
        final Envelope record1JsonEnvelope = (JsonEnvelope) streams2.get(1).findFirst().orElse(null);

        assertThat(record1JsonEnvelope.metadata().name(), is("progression.event.defendant-defence-organisation-associated"));
    }

    @Test
    void shouldProcessCommand_whenOrganisationIsSetupAndAssociated_expectDisassociationEvent() throws EventStreamException {

        final ApplicationAggregate applicationAggregate = new ApplicationAggregate();
        when(eventSource.getStreamById(any())).thenReturn(eventStream);
        when(aggregateService.get(eventStream, ApplicationAggregate.class)).thenReturn(applicationAggregate);
        final JsonObject jsonObjectPayload = Json.createObjectBuilder().add("organisationId", randomUUID().toString()).build();
        when(organisationService.getAssociatedOrganisationForApplication(any(), any())).thenReturn(jsonObjectPayload);
        applicationAggregate.createCourtApplication(courtApplication, null);
        final UUID organisationId = randomUUID();
        final ReceiveRepresentationOrderForApplication receiveRepresentationOrderForApplication = payloadForReceiveRepresentationOrder(STATUS_CODE, APPLICATION_ID, SUBJECT_ID, OFFENCE_ID);
        final Envelope<ReceiveRepresentationOrderForApplication> envelope = Envelope.envelopeFrom(getProgressionCommandHandlerReceiveRepresentationOrderForApplication(), receiveRepresentationOrderForApplication);
        when(legalStatusReferenceDataService.getLegalStatusByStatusIdAndStatusCode(any(), any())).thenReturn(Optional.of(getLegalStatus(STATUS_ID,STATUS_DESCRIPTION, STATUS)));
        when(usersGroupService.getOrganisationDetailsForLAAContractNumber(any(), any())).thenReturn(getOrganisationDetails(null));

        receiveRepresentationOrderHandler.handle(envelope);
        final Stream<JsonEnvelope> envelopeStream = verifyAppendAndGetArgumentFrom(eventStream);

        Optional<JsonEnvelope> laaAssociatedOrDisassociatedEnvelope = envelopeStream.filter(a -> a.metadata().name().equals("progression.event.application-laa-associated") || a.metadata().name().equals("progression.event.defendant-defence-organisation-disassociated"))
                .findAny();

        assertTrue(laaAssociatedOrDisassociatedEnvelope.isPresent());
    }

    @Test
    void shouldProcessCommand_whenOrganisationIsSetupAndAssociated_isNotAlreadyAssociated() throws EventStreamException {

        final ApplicationAggregate applicationAggregate = new ApplicationAggregate();
        when(eventSource.getStreamById(any())).thenReturn(eventStream);
        when(aggregateService.get(eventStream, ApplicationAggregate.class)).thenReturn(applicationAggregate);
        final JsonObject jsonObjectPayload = Json.createObjectBuilder().build();
        when(organisationService.getAssociatedOrganisationForApplication(any(), any())).thenReturn(jsonObjectPayload);
        applicationAggregate.createCourtApplication(courtApplication, null);
        final UUID organisationId = randomUUID();
        final ReceiveRepresentationOrderForApplication receiveRepresentationOrderForApplication = payloadForReceiveRepresentationOrder(STATUS_CODE, APPLICATION_ID, SUBJECT_ID, OFFENCE_ID);
        final Envelope<ReceiveRepresentationOrderForApplication> envelope = Envelope.envelopeFrom(getProgressionCommandHandlerReceiveRepresentationOrderForApplication(), receiveRepresentationOrderForApplication);
        when(legalStatusReferenceDataService.getLegalStatusByStatusIdAndStatusCode(any(), any())).thenReturn(Optional.of(getLegalStatus(STATUS_ID,STATUS_DESCRIPTION, STATUS)));
        when(usersGroupService.getOrganisationDetailsForLAAContractNumber(any(), any())).thenReturn(getOrganisationDetails(organisationId));

        receiveRepresentationOrderHandler.handle(envelope);
        final Stream<JsonEnvelope> envelopeStream = verifyAppendAndGetArgumentFrom(eventStream);

        Optional<JsonEnvelope> organisationAssociatedEnvelope = envelopeStream.filter
                        (a -> a.metadata().name().equals("progression.event.defendant-defence-organisation-associated"))
                .findAny();

        assertTrue(organisationAssociatedEnvelope.isPresent());
    }

    @Test
    void shouldProcessCommand_whenOrganisationIsSetupAndAssociated_isAlreadyAssociated() throws EventStreamException {

        final ApplicationAggregate applicationAggregate = new ApplicationAggregate();
        when(eventSource.getStreamById(any())).thenReturn(eventStream);
        when(aggregateService.get(eventStream, ApplicationAggregate.class)).thenReturn(applicationAggregate);
        final UUID organisationId = randomUUID();
        final JsonObject jsonObjectPayload = Json.createObjectBuilder().add("organisationId", organisationId.toString()).build();
        when(organisationService.getAssociatedOrganisationForApplication(any(), any())).thenReturn(jsonObjectPayload);
        applicationAggregate.createCourtApplication(courtApplication, null);
        final ReceiveRepresentationOrderForApplication receiveRepresentationOrderForApplication = payloadForReceiveRepresentationOrder(STATUS_CODE, APPLICATION_ID, SUBJECT_ID, OFFENCE_ID);
        final Envelope<ReceiveRepresentationOrderForApplication> envelope = Envelope.envelopeFrom(getProgressionCommandHandlerReceiveRepresentationOrderForApplication(), receiveRepresentationOrderForApplication);
        when(legalStatusReferenceDataService.getLegalStatusByStatusIdAndStatusCode(any(), any())).thenReturn(Optional.of(getLegalStatus(STATUS_ID,STATUS_DESCRIPTION, STATUS)));
        when(usersGroupService.getOrganisationDetailsForLAAContractNumber(any(), any())).thenReturn(getOrganisationDetails(organisationId));

        receiveRepresentationOrderHandler.handle(envelope);
        final Stream<JsonEnvelope> envelopeStream = verifyAppendAndGetArgumentFrom(eventStream);

        Optional<JsonEnvelope> organisationAssociatedEnvelope = envelopeStream.filter
                        (a -> a.metadata().name().equals("progression.event.defendant-defence-organisation-associated"))
                .findAny();

        assertTrue(organisationAssociatedEnvelope.isPresent());
    }

    @Test
    void shouldProcessCommand_whenOrganisationIsSetupAndAssociated_expectApplicationUpdatedEventWithAllDetails() throws EventStreamException {

        final ApplicationAggregate applicationAggregate = new ApplicationAggregate();
        when(eventSource.getStreamById(any())).thenReturn(eventStream);
        when(aggregateService.get(eventStream, ApplicationAggregate.class)).thenReturn(applicationAggregate);
        final JsonObject jsonObjectPayload = Json.createObjectBuilder().add("organisationId", randomUUID().toString()).build();
        when(organisationService.getAssociatedOrganisationForApplication(any(), any())).thenReturn(jsonObjectPayload);
        applicationAggregate.createCourtApplication(courtApplication, null);
        final UUID organisationId = randomUUID();
        final ReceiveRepresentationOrderForApplication receiveRepresentationOrderForApplication = payloadForReceiveRepresentationOrder(STATUS_CODE, APPLICATION_ID, SUBJECT_ID, OFFENCE_ID);
        final Envelope<ReceiveRepresentationOrderForApplication> envelope = Envelope.envelopeFrom(getProgressionCommandHandlerReceiveRepresentationOrderForApplication(), receiveRepresentationOrderForApplication);
        when(legalStatusReferenceDataService.getLegalStatusByStatusIdAndStatusCode(any(), any())).thenReturn(Optional.of(getLegalStatus(STATUS_ID,STATUS_DESCRIPTION, STATUS)));
        when(usersGroupService.getOrganisationDetailsForLAAContractNumber(any(), any())).thenReturn(getOrganisationDetails(organisationId));

        receiveRepresentationOrderHandler.handle(envelope);

        JsonObject event = getEventAsJsonObjectFromStreamInGivenTimes(1, "progression.event.application-defence-organisation-changed", true);
        assertThat(event, notNullValue());
        assertThat(event.getString("applicationId"), is(APPLICATION_ID.toString()));
        assertThat(event.getString("subjectId"), is(SUBJECT_ID.toString()));

        JsonObject associatedDefenceOrganisationObject = event.getJsonObject("associatedDefenceOrganisation");
        assertThat(associatedDefenceOrganisationObject.getString("applicationReference"), is(APPLICATION_REFERENCE));
        assertThat(associatedDefenceOrganisationObject.getString("fundingType"), is("REPRESENTATION_ORDER"));
        assertThat(associatedDefenceOrganisationObject.getBoolean("isAssociatedByLAA"), is(Boolean.TRUE));

        JsonObject defenceOrganisationObject = associatedDefenceOrganisationObject.getJsonObject("defenceOrganisation");
        assertThat(defenceOrganisationObject.getString("laaContractNumber"), is(LAA_CONTRACT_NUMBER));

        JsonObject organisationObject = defenceOrganisationObject.getJsonObject("organisation");
        assertThat(organisationObject.getString("incorporationNumber"), is(INCORPORATION_NUMBER));
        assertThat(organisationObject.getString("name"), is(ORG_NAME));

        verifyApplicationCaseDefendantOrganisations(event.getJsonArray("applicationCaseDefendantOrganisations"));
    }

    @Test
    void shouldProcessCommand_whenOrganisationIsSetupAndAssociated_expectOneEventApplicationUpdatedEventWithAllDetails() throws EventStreamException {

        final ApplicationAggregate applicationAggregate = new ApplicationAggregate();
        when(eventSource.getStreamById(any())).thenReturn(eventStream);
        when(aggregateService.get(eventStream, ApplicationAggregate.class)).thenReturn(applicationAggregate);
        final JsonObject jsonObjectPayload = Json.createObjectBuilder().add("organisationId", randomUUID().toString()).build();
        when(organisationService.getAssociatedOrganisationForApplication(any(), any())).thenReturn(jsonObjectPayload);
        applicationAggregate.createCourtApplication(courtApplication, null);
        final UUID organisationId = randomUUID();
        final ReceiveRepresentationOrderForApplication receiveRepresentationOrderForApplication = payloadForReceiveRepresentationOrder(STATUS_CODE, APPLICATION_ID, SUBJECT_ID, OFFENCE_ID);
        final Envelope<ReceiveRepresentationOrderForApplication> envelope = Envelope.envelopeFrom(getProgressionCommandHandlerReceiveRepresentationOrderForApplication(), receiveRepresentationOrderForApplication);
        when(legalStatusReferenceDataService.getLegalStatusByStatusIdAndStatusCode(any(), any())).thenReturn(Optional.of(getLegalStatus(STATUS_ID,STATUS_DESCRIPTION, STATUS)));
        when(usersGroupService.getOrganisationDetailsForLAAContractNumber(any(), any())).thenReturn(getOrganisationDetails(organisationId));

        receiveRepresentationOrderHandler.handle(envelope);

        JsonObject event = getEventAsJsonObjectFromStreamInGivenTimes(1, "progression.event.application-defence-organisation-changed", true);
        assertThat(event, notNullValue());
        assertThat(event.getString("applicationId"), is(APPLICATION_ID.toString()));
        assertThat(event.getString("subjectId"), is(SUBJECT_ID.toString()));

        JsonObject associatedDefenceOrganisationObject = event.getJsonObject("associatedDefenceOrganisation");
        assertThat(associatedDefenceOrganisationObject.getString("applicationReference"), is(APPLICATION_REFERENCE));
        assertThat(associatedDefenceOrganisationObject.getString("fundingType"), is("REPRESENTATION_ORDER"));
        assertThat(associatedDefenceOrganisationObject.getBoolean("isAssociatedByLAA"), is(Boolean.TRUE));

        JsonObject defenceOrganisationObject = associatedDefenceOrganisationObject.getJsonObject("defenceOrganisation");
        assertThat(defenceOrganisationObject.getString("laaContractNumber"), is(LAA_CONTRACT_NUMBER));

        JsonObject organisationObject = defenceOrganisationObject.getJsonObject("organisation");
        assertThat(organisationObject.getString("incorporationNumber"), is(INCORPORATION_NUMBER));
        assertThat(organisationObject.getString("name"), is(ORG_NAME));

        verifyApplicationCaseDefendantOrganisations(event.getJsonArray("applicationCaseDefendantOrganisations"));

        receiveRepresentationOrderHandler.handle(envelope);

        JsonObject secondEvent = getEventAsJsonObjectFromStreamInGivenTimes(2, "progression.event.application-defence-organisation-changed", false);
        assertThat(secondEvent, nullValue());
    }

    @Test
    void shouldTestDefenceOrganisationAssociated_Event_whenOrganisationIsSetupAndAssociatedWithMultiOffence_expectOneAssociationEvent() throws EventStreamException {

        final ApplicationAggregate applicationAggregate = new ApplicationAggregate();
        when(eventSource.getStreamById(any())).thenReturn(eventStream);
        when(aggregateService.get(eventStream, ApplicationAggregate.class)).thenReturn(applicationAggregate);
        final JsonObject jsonObjectPayload = Json.createObjectBuilder().build();
        when(organisationService.getAssociatedOrganisationForApplication(any(), any())).thenReturn(jsonObjectPayload);
        applicationAggregate.createCourtApplication(courtApplication, null);
        ReceiveRepresentationOrderForApplication receiveRepresentationOrderForApplication = payloadForReceiveRepresentationOrder(STATUS_CODE, APPLICATION_ID, SUBJECT_ID, OFFENCE_ID);

        final Envelope<ReceiveRepresentationOrderForApplication> envelope = Envelope.envelopeFrom(getProgressionCommandHandlerReceiveRepresentationOrderForApplication(), receiveRepresentationOrderForApplication);
        when(legalStatusReferenceDataService.getLegalStatusByStatusIdAndStatusCode(any(), any())).thenReturn(Optional.of(getLegalStatus(STATUS_ID,STATUS_DESCRIPTION, STATUS)));
        final UUID organisationId = randomUUID();
        when(usersGroupService.getOrganisationDetailsForLAAContractNumber(any(), any())).thenReturn(getOrganisationDetails(organisationId));

        receiveRepresentationOrderHandler.handle(envelope);

        assertFirstReceiveRepresentationOrderHandlerCommand();

        receiveRepresentationOrderForApplication = payloadForReceiveRepresentationOrder(STATUS_CODE, APPLICATION_ID, SUBJECT_ID, OFFENCE_ID);

        final Envelope<ReceiveRepresentationOrderForApplication> newEnvelope = Envelope.envelopeFrom(getProgressionCommandHandlerReceiveRepresentationOrderForApplication(), receiveRepresentationOrderForApplication);

        receiveRepresentationOrderHandler.handle(newEnvelope);

        assertSecondSendReceiveRepresentationOrderHandlerCommand();
    }

    @Test
    void shouldTestDefenceOrganisationAssociated_Event_whenDefenceHasMultiOffence_expectTwoTimesAssociationEvent() throws EventStreamException {

        final ApplicationAggregate applicationAggregate = new ApplicationAggregate();
        when(eventSource.getStreamById(any())).thenReturn(eventStream);
        when(aggregateService.get(eventStream, ApplicationAggregate.class)).thenReturn(applicationAggregate);
        final JsonObject jsonObjectPayload = Json.createObjectBuilder().build();
        when(organisationService.getAssociatedOrganisationForApplication(any(), any())).thenReturn(jsonObjectPayload);
        applicationAggregate.createCourtApplication(courtApplication, null);
        ReceiveRepresentationOrderForApplication receiveRepresentationOrderForApplication = payloadForReceiveRepresentationOrder(STATUS_CODE, APPLICATION_ID, SUBJECT_ID, OFFENCE_ID);

        final Envelope<ReceiveRepresentationOrderForApplication> envelope = Envelope.envelopeFrom(getProgressionCommandHandlerReceiveRepresentationOrderForApplication(), receiveRepresentationOrderForApplication);
        when(legalStatusReferenceDataService.getLegalStatusByStatusIdAndStatusCode(any(), any())).thenReturn(Optional.of(getLegalStatus(STATUS_ID,STATUS_DESCRIPTION, STATUS)));
        final UUID organisationId = randomUUID();
        when(usersGroupService.getOrganisationDetailsForLAAContractNumber(any(), any())).thenReturn(getOrganisationDetails(organisationId));

        receiveRepresentationOrderHandler.handle(envelope);

        assertFirstReceiveRepresentationOrderHandlerCommand();

        receiveRepresentationOrderForApplication = payloadForReceiveRepresentationOrder(STATUS_CODE, APPLICATION_ID, SUBJECT_ID, OFFENCE_ID);

        final Envelope<ReceiveRepresentationOrderForApplication> newEnvelope = Envelope.envelopeFrom(getProgressionCommandHandlerReceiveRepresentationOrderForApplication(), receiveRepresentationOrderForApplication);

        receiveRepresentationOrderHandler.handle(newEnvelope);

        assertSecondSendReceiveRepresentationOrderHandlerCommand();

    }

    @Test
    void shouldProcessCommand_whenSubjectIdNotFound_expectBothDefenceOrganisationAndLAAReferenceWithOffenceEventsNotTriggered() throws EventStreamException {

        final ApplicationAggregate applicationAggregate = new ApplicationAggregate();
        when(eventSource.getStreamById(any())).thenReturn(eventStream);
        when(aggregateService.get(eventStream, ApplicationAggregate.class)).thenReturn(applicationAggregate);
        final JsonObject jsonObjectPayload = Json.createObjectBuilder().add("organisationId", randomUUID().toString()).build();
        when(organisationService.getAssociatedOrganisationForApplication(any(), any())).thenReturn(jsonObjectPayload);
        applicationAggregate.createCourtApplication(courtApplication, null);
        final UUID organisationId = randomUUID();
        final ReceiveRepresentationOrderForApplication receiveRepresentationOrderForApplication = payloadForReceiveRepresentationOrder(STATUS_CODE, APPLICATION_ID, UUID.randomUUID(), OFFENCE_ID);
        final Envelope<ReceiveRepresentationOrderForApplication> envelope = Envelope.envelopeFrom(getProgressionCommandHandlerReceiveRepresentationOrderForApplication(), receiveRepresentationOrderForApplication);
        when(legalStatusReferenceDataService.getLegalStatusByStatusIdAndStatusCode(any(), any())).thenReturn(Optional.of(getLegalStatus(STATUS_ID,STATUS_DESCRIPTION, STATUS)));
        when(usersGroupService.getOrganisationDetailsForLAAContractNumber(any(), any())).thenReturn(getOrganisationDetails(organisationId));

        receiveRepresentationOrderHandler.handle(envelope);

        final Stream<JsonEnvelope> envelopeStream = verifyAppendAndGetArgumentFrom(eventStream);

        Optional<JsonEnvelope> defenceOrganisationOrOffenceEnvelope = envelopeStream.filter
                        (a -> a.metadata().name().equals("progression.event.application-defence-organisation-changed") || a.metadata().name().equals("progression.event.application-reporder-offences-updated"))
                .findAny();

        assertFalse(defenceOrganisationOrOffenceEnvelope.isPresent());
    }

    @Test
    void shouldProcessCommand_whenOffenceIdNotFound_expectBothDefenceOrganisationAndLAAReferenceWithOffenceEventsNotTriggered() throws EventStreamException {

        final ApplicationAggregate applicationAggregate = new ApplicationAggregate();
        when(eventSource.getStreamById(any())).thenReturn(eventStream);
        when(aggregateService.get(eventStream, ApplicationAggregate.class)).thenReturn(applicationAggregate);
        final JsonObject jsonObjectPayload = Json.createObjectBuilder().add("organisationId", randomUUID().toString()).build();
        when(organisationService.getAssociatedOrganisationForApplication(any(), any())).thenReturn(jsonObjectPayload);
        applicationAggregate.createCourtApplication(courtApplication, null);
        final UUID organisationId = randomUUID();
        final ReceiveRepresentationOrderForApplication receiveRepresentationOrderForApplication = payloadForReceiveRepresentationOrder(STATUS_CODE, APPLICATION_ID, SUBJECT_ID, UUID.randomUUID());
        final Envelope<ReceiveRepresentationOrderForApplication> envelope = Envelope.envelopeFrom(getProgressionCommandHandlerReceiveRepresentationOrderForApplication(), receiveRepresentationOrderForApplication);
        when(legalStatusReferenceDataService.getLegalStatusByStatusIdAndStatusCode(any(), any())).thenReturn(Optional.of(getLegalStatus(STATUS_ID,STATUS_DESCRIPTION, STATUS)));
        when(usersGroupService.getOrganisationDetailsForLAAContractNumber(any(), any())).thenReturn(getOrganisationDetails(organisationId));

        receiveRepresentationOrderHandler.handle(envelope);
        final Stream<JsonEnvelope> envelopeStream = verifyAppendAndGetArgumentFrom(eventStream);
        Optional<JsonEnvelope> defenceOrganisationOrOffenceEnvelope = envelopeStream.filter
                        (a -> a.metadata().name().equals("progression.event.application-defence-organisation-changed") || a.metadata().name().equals("progression.event.application-reporder-offences-updated"))
                .findAny();

        assertFalse(defenceOrganisationOrOffenceEnvelope.isPresent());
    }

    private static JsonObject getLegalStatus(final UUID statusId, final String statusDescription, final String status) {
        return Json.createObjectBuilder()
                .add("id", statusId.toString())
                .add("statusDescription", statusDescription)
                .add("defendantLevelStatus", status)
                .build();
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


    private JsonObject getEventAsJsonObjectFromStreamInGivenTimes(int times, String eventName, boolean isEventPresent) throws EventStreamException {
        final ArgumentCaptor<Stream> argumentCaptor = ArgumentCaptor.forClass(Stream.class);
        (Mockito.verify(eventStream, times(times))).append(argumentCaptor.capture());
        final List<Stream> streams2 = argumentCaptor.getAllValues();
        final List<JsonEnvelope> eventsList = (List<JsonEnvelope>) (streams2.get(times-1).collect(Collectors.toList()));
        Optional<JsonEnvelope> eventEnvelope = eventsList.stream().filter(x -> x.metadata().name().equalsIgnoreCase(eventName)).findFirst();
        assertThat(eventEnvelope.isPresent(), is(isEventPresent));
        if (isEventPresent) {
            return eventEnvelope.get().payloadAsJsonObject();
        }
        return null;
    }



    private static ReceiveRepresentationOrderForApplication payloadForReceiveRepresentationOrder(final String statusCode, final UUID applicationId, final UUID subjectId, final UUID offenceId) {
        return ReceiveRepresentationOrderForApplication.receiveRepresentationOrderForApplication()
                .withApplicationReference(APPLICATION_REFERENCE)
                .withSubjectId(subjectId)
                .withApplicationId(applicationId)
                .withOffenceId(offenceId)
                .withStatusCode(statusCode)
                .withStatusDate(LocalDate.parse("2019-07-01"))
                .withEffectiveStartDate(LocalDate.parse("2019-09-01"))
                .withEffectiveEndDate(LocalDate.parse("2019-12-01"))
                .withDefenceOrganisation(DefenceOrganisation.defenceOrganisation()
                        .withLaaContractNumber(LAA_CONTRACT_NUMBER)
                        .withOrganisation(Organisation.organisation()
                                .withName(ORG_NAME)
                                .withIncorporationNumber(INCORPORATION_NUMBER)
                                .build())
                        .build())
                .build();


    }

    private void verifyApplicationCaseDefendantOrganisations(final JsonArray applicationCaseDefendants) {
        JsonObject applicationCaseDefendantOrganisation = applicationCaseDefendants.getJsonObject(0);
        assertThat(applicationCaseDefendantOrganisation.getString("caseId"), is(CASE_ID_1.toString()));
        assertThat(applicationCaseDefendantOrganisation.getString("defendantId"), is(DEFENDANT_ID_1.toString()));

        applicationCaseDefendantOrganisation = applicationCaseDefendants.getJsonObject(1);
        assertThat(applicationCaseDefendantOrganisation.getString("caseId"), is(CASE_ID_2.toString()));
        assertThat(applicationCaseDefendantOrganisation.getString("defendantId"), is(DEFENDANT_ID_2.toString()));
    }

    private void verifyApplicationCaseOffenceLAAReference(final JsonObject laaReferenceObject, final UUID statusId, final String statusDescription, final String status, final String statusCode) {
        assertThat(laaReferenceObject.getString("applicationReference"), is(APPLICATION_REFERENCE));
        assertThat(laaReferenceObject.getString("laaContractNumber"), is(LAA_CONTRACT_NUMBER));
        assertThat(laaReferenceObject.getString("statusCode"), is(statusCode));
        assertThat(laaReferenceObject.getString("offenceLevelStatus"), is(status));
        assertThat(laaReferenceObject.getString("statusDescription"), is(statusDescription));
        assertThat(laaReferenceObject.getString("statusId"), is(statusId.toString()));
    }

    private OrganisationDetails getOrganisationDetails(final UUID organisationId) {
        return OrganisationDetails.newBuilder()
                .withId(organisationId)
                .withName("Test")
                .build();
    }

    private static Metadata getProgressionCommandHandlerReceiveRepresentationOrderForApplication() {
        return Envelope
                .metadataBuilder()
                .withName("progression.command.handler.receive-representationOrder-for-application")
                .withId(randomUUID())
                .build();
    }
}