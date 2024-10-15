package uk.gov.moj.cpp.progression.handler;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static java.util.UUID.randomUUID;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static uk.gov.justice.core.courts.CourtApplication.courtApplication;
import static uk.gov.justice.core.courts.CourtApplicationType.courtApplicationType;
import static uk.gov.justice.core.courts.InitiateCourtApplicationProceedings.initiateCourtApplicationProceedings;
import static uk.gov.justice.services.test.utils.core.helper.EventStreamMockHelper.verifyAppendAndGetArgumentFrom;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeMatcher.jsonEnvelope;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeMetadataMatcher.metadata;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopePayloadMatcher.payload;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeStreamMatcher.streamContaining;
import static uk.gov.justice.services.test.utils.core.messaging.MetadataBuilderFactory.metadataWithRandomUUID;
import static uk.gov.justice.services.test.utils.core.reflection.ReflectionUtil.setField;

import uk.gov.justice.core.courts.Address;
import uk.gov.justice.core.courts.CourtApplicationParty;
import uk.gov.justice.core.courts.CourtHearingRequest;
import uk.gov.justice.core.courts.DefendantAddressOnApplicationUpdated;
import uk.gov.justice.core.courts.DefendantUpdate;
import uk.gov.justice.core.courts.InitiateCourtApplicationProceedings;
import uk.gov.justice.core.courts.LegalEntityDefendant;
import uk.gov.justice.core.courts.LinkType;
import uk.gov.justice.core.courts.MasterDefendant;
import uk.gov.justice.core.courts.Organisation;
import uk.gov.justice.core.courts.Person;
import uk.gov.justice.core.courts.PersonDefendant;
import uk.gov.justice.core.courts.UpdateDefendantAddressOnApplication;
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
import uk.gov.justice.services.test.utils.core.enveloper.EnveloperFactory;
import uk.gov.moj.cpp.progression.aggregate.ApplicationAggregate;

import java.util.Arrays;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;


import com.fasterxml.jackson.databind.ObjectMapper;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;


@ExtendWith(MockitoExtension.class)
public class UpdateApplicationHandlerTest {
    private static final UUID applicationId = randomUUID();
    private static final UUID masterDefendantId = randomUUID();
    @Mock
    private EventSource eventSource;
    @Mock
    private AggregateService aggregateService;
    @Mock
    protected EventStream eventStream;

    protected ApplicationAggregate applicationAggregate;
    @Mock
    protected Stream<Object> events;
    @InjectMocks
    private UpdateApplicationHandler updateApplicationHandler;
    @Spy
    private JsonObjectToObjectConverter jsonObjectToObjectConverter;
    @Spy
    private final ObjectMapper objectMapper = new ObjectMapperProducer().objectMapper();
    @Mock
    private ObjectToJsonObjectConverter objectToJsonObjectConverter;

    @Spy
    private final Enveloper enveloper = EnveloperFactory.createEnveloperWithEvents(
            DefendantAddressOnApplicationUpdated.class
    );

    @BeforeEach
    public void setup(){
        setField(this.jsonObjectToObjectConverter, "objectMapper", new ObjectMapperProducer().objectMapper());
        applicationAggregate = new ApplicationAggregate();
        when(eventSource.getStreamById(any())).thenReturn(eventStream);
        when(aggregateService.get(eventStream, ApplicationAggregate.class)).thenReturn(applicationAggregate);
    }

    @Test
    public void updateDefendantAddressOnApplication_DefendantIsSubject_PersonDefendant() throws EventStreamException {
        final CourtApplicationParty courtApplicationParty = CourtApplicationParty.courtApplicationParty()
                .withId(masterDefendantId)
                .withMasterDefendant(MasterDefendant.masterDefendant()
                        .withMasterDefendantId(masterDefendantId)
                        .withPersonDefendant(PersonDefendant.personDefendant()
                                .withPersonDetails(Person.person()
                                        .withAddress(Address.address()
                                                .withAddress1("Old Address 1")
                                                .withAddress2("Old Address 2")
                                                .withPostcode("RG2 3WQ").build())
                                        .build())
                                .build())
                        .build())
                .build();
        createApplicationInAggregateForPersonDefendantAsSubject(courtApplicationParty);
        final DefendantUpdate defendantUpdate = DefendantUpdate.defendantUpdate()
                .withId(masterDefendantId)
                .withMasterDefendantId(masterDefendantId)
                .withPersonDefendant(PersonDefendant.personDefendant()
                        .withPersonDetails(Person.person()
                                .withAddress(Address.address()
                                        .withAddress1("New Address 1")
                                        .withAddress2("New Address 2")
                                        .withPostcode("RG1 2PQ")
                                        .build())
                                .build())
                        .build())
                .build();
        UpdateDefendantAddressOnApplication updateDefendantAddressOnApplication = UpdateDefendantAddressOnApplication
                .updateDefendantAddressOnApplication()
                .withApplicationId(applicationId)
                .withDefendant(defendantUpdate)
                .build();

        updateApplicationHandler.handle(Envelope.envelopeFrom(metadataWithRandomUUID("progression.command.update-defendant-address-on-application"),
                updateDefendantAddressOnApplication));

        final Stream<JsonEnvelope> envelopeStream = verifyAppendAndGetArgumentFrom(eventStream);

        assertThat(envelopeStream, streamContaining(
                jsonEnvelope(
                        metadata()
                                .withName("progression.event.defendant-address-on-application-updated"),
                        payload().isJson(allOf(
                                withJsonPath("$.applicationId", is(applicationId.toString())))
                        )
                )
        ));
    }

    @Test
    public void updateDefendantAddressOnApplication_DefendantIsApplicant_LegalEntityDefendant() throws EventStreamException {
        final CourtApplicationParty courtApplicationParty = CourtApplicationParty.courtApplicationParty()
                .withId(masterDefendantId)
                .withMasterDefendant(MasterDefendant.masterDefendant()
                        .withMasterDefendantId(masterDefendantId)
                        .withLegalEntityDefendant(LegalEntityDefendant.legalEntityDefendant()
                                .withOrganisation(Organisation.organisation()
                                        .withAddress(Address.address()
                                                .withAddress1("New Address 1")
                                                .withAddress2("New Address 2")
                                                .withPostcode("RG1 2PQ")
                                                .build())
                                        .build())
                                .build())
                        .build())
                .build();
        createApplicationInAggregateForLegalEntityDefendantAsApplicant(courtApplicationParty);
        final DefendantUpdate defendantUpdate = DefendantUpdate.defendantUpdate()
                .withId(masterDefendantId)
                .withMasterDefendantId(masterDefendantId)
                .withLegalEntityDefendant(LegalEntityDefendant.legalEntityDefendant()
                        .withOrganisation(Organisation.organisation()
                                .withAddress(Address.address()
                                        .withAddress1("Old Address 1")
                                        .withAddress2("Old Address 2")
                                        .withPostcode("RG2 3WQ")
                                        .build())
                                .build())
                        .build())
                .build();
        UpdateDefendantAddressOnApplication updateDefendantAddressOnApplication = UpdateDefendantAddressOnApplication
                .updateDefendantAddressOnApplication()
                .withApplicationId(applicationId)
                .withDefendant(defendantUpdate)
                .build();

        updateApplicationHandler.handle(Envelope.envelopeFrom(metadataWithRandomUUID("progression.command.update-defendant-address-on-application"),
                updateDefendantAddressOnApplication));

        final Stream<JsonEnvelope> envelopeStream = verifyAppendAndGetArgumentFrom(eventStream);

        assertThat(envelopeStream, streamContaining(
                jsonEnvelope(
                        metadata()
                                .withName("progression.event.defendant-address-on-application-updated"),
                        payload().isJson(allOf(
                                withJsonPath("$.applicationId", is(applicationId.toString())))
                        )
                )
        ));
    }

    @Test
    public void updateDefendantAddressOnApplication_DefendantIsRespondent_PersonDefendant() throws EventStreamException {
        final CourtApplicationParty courtApplicationParty = CourtApplicationParty.courtApplicationParty().withId(randomUUID())
                .withMasterDefendant(MasterDefendant.masterDefendant()
                        .withMasterDefendantId(randomUUID())
                        .withPersonDefendant(PersonDefendant.personDefendant()
                                .withPersonDetails(Person.person()
                                        .withAddress(Address.address()
                                                .withAddress1("Old Address 1")
                                                .withAddress2("Old Address 2")
                                                .withPostcode("RG2 3WQ").build())
                                        .build())
                                .build())
                        .build())
                .build();
        createApplicationInAggregateForPersonDefendantAsRespondent(courtApplicationParty);
        final DefendantUpdate defendantUpdate = DefendantUpdate.defendantUpdate()
                .withId(masterDefendantId)
                .withMasterDefendantId(masterDefendantId)
                .withPersonDefendant(PersonDefendant.personDefendant()
                        .withPersonDetails(Person.person()
                                .withAddress(Address.address()
                                        .withAddress1("New Address 1")
                                        .withAddress2("New Address 2")
                                        .withPostcode("RG1 2PQ")
                                        .build())
                                .build())
                        .build())
                .build();
        UpdateDefendantAddressOnApplication updateDefendantAddressOnApplication = UpdateDefendantAddressOnApplication
                .updateDefendantAddressOnApplication()
                .withApplicationId(applicationId)
                .withDefendant(defendantUpdate)
                .build();

        updateApplicationHandler.handle(Envelope.envelopeFrom(metadataWithRandomUUID("progression.command.update-defendant-address-on-application"),
                updateDefendantAddressOnApplication));

        final Stream<JsonEnvelope> envelopeStream = verifyAppendAndGetArgumentFrom(eventStream);

        assertThat(envelopeStream, streamContaining(
                jsonEnvelope(
                        metadata()
                                .withName("progression.event.defendant-address-on-application-updated"),
                        payload().isJson(allOf(
                                withJsonPath("$.applicationId", is(applicationId.toString())))
                        )
                )
        ));
    }

    @Test
    public void shouldNotUpdateDefendantAddressOnApplication_DefendantNotPartOfApplication() throws EventStreamException {
        final CourtApplicationParty courtApplicationParty = CourtApplicationParty.courtApplicationParty()
                .withId(randomUUID())
                .withMasterDefendant(MasterDefendant.masterDefendant()
                        .withMasterDefendantId(randomUUID())
                        .withPersonDefendant(PersonDefendant.personDefendant()
                                .withPersonDetails(Person.person()
                                        .withAddress(Address.address()
                                                .withAddress1("Old Address 1")
                                                .withAddress2("Old Address 2")
                                                .withPostcode("RG2 3WQ").build())
                                        .build())
                                .build())
                        .build())
                .build();
        createApplicationInAggregateForPersonDefendantAsRespondent(courtApplicationParty);
        final DefendantUpdate defendantUpdate = DefendantUpdate.defendantUpdate()
                .withId(randomUUID())
                .withMasterDefendantId(randomUUID())
                .withPersonDefendant(PersonDefendant.personDefendant()
                        .withPersonDetails(Person.person()
                                .withAddress(Address.address()
                                        .withAddress1("New Address 1")
                                        .withAddress2("New Address 2")
                                        .withPostcode("RG1 2PQ")
                                        .build())
                                .build())
                        .build())
                .build();
        UpdateDefendantAddressOnApplication updateDefendantAddressOnApplication = UpdateDefendantAddressOnApplication
                .updateDefendantAddressOnApplication()
                .withApplicationId(applicationId)
                .withDefendant(defendantUpdate)
                .build();

        updateApplicationHandler.handle(Envelope.envelopeFrom(metadataWithRandomUUID("progression.command.update-defendant-address-on-application"),
                updateDefendantAddressOnApplication));

        final Stream<JsonEnvelope> envelopeStream = verifyAppendAndGetArgumentFrom(eventStream);

        assertThat(envelopeStream.collect(Collectors.toList()), hasSize(0));
    }

    @Test
    public void shouldNotUpdateDefendantAddressOnApplication_DefendantAddressIsSameOnApplication() throws EventStreamException {
        final CourtApplicationParty courtApplicationParty = CourtApplicationParty.courtApplicationParty()
                .withId(randomUUID())
                .withMasterDefendant(MasterDefendant.masterDefendant()
                        .withMasterDefendantId(masterDefendantId)
                        .withPersonDefendant(PersonDefendant.personDefendant()
                                .withPersonDetails(Person.person()
                                        .withAddress(Address.address()
                                                .withAddress1("New Address 1")
                                                .withAddress2("New Address 2")
                                                .withPostcode("RG1 2PQ")
                                                .build())
                                        .build())
                                .build())
                        .build())
                .build();
        createApplicationInAggregateForPersonDefendantAsSubject(courtApplicationParty);
        final DefendantUpdate defendantUpdate = DefendantUpdate.defendantUpdate()
                .withId(randomUUID())
                .withMasterDefendantId(masterDefendantId)
                .withPersonDefendant(PersonDefendant.personDefendant()
                        .withPersonDetails(Person.person()
                                .withAddress(Address.address()
                                        .withAddress1("New Address 1")
                                        .withAddress2("New Address 2")
                                        .withPostcode("RG1 2PQ")
                                        .build())
                                .build())
                        .build())
                .build();
        UpdateDefendantAddressOnApplication updateDefendantAddressOnApplication = UpdateDefendantAddressOnApplication
                .updateDefendantAddressOnApplication()
                .withApplicationId(applicationId)
                .withDefendant(defendantUpdate)
                .build();

        updateApplicationHandler.handle(Envelope.envelopeFrom(metadataWithRandomUUID("progression.command.update-defendant-address-on-application"),
                updateDefendantAddressOnApplication));

        final Stream<JsonEnvelope> envelopeStream = verifyAppendAndGetArgumentFrom(eventStream);
        assertThat(envelopeStream.collect(Collectors.toList()), hasSize(0));
    }

    private void createApplicationInAggregateForLegalEntityDefendantAsApplicant(final CourtApplicationParty courtApplicationParty) {
        final InitiateCourtApplicationProceedings initiateCourtApplicationProceedings =
                initiateCourtApplicationProceedings()
                        .withCourtApplication(courtApplication()
                                .withId(applicationId)
                                .withType(courtApplicationType().withLinkType(LinkType.LINKED).build())
                                .withApplicant(courtApplicationParty)
                                .build())
                        .withCourtHearing(CourtHearingRequest.courtHearingRequest().build())
                        .withSummonsApprovalRequired(false)
                        .build();

        applicationAggregate.initiateCourtApplicationProceedings(initiateCourtApplicationProceedings, false, false);

        applicationAggregate.createCourtApplication(initiateCourtApplicationProceedings.getCourtApplication(), null);
    }

    private void createApplicationInAggregateForPersonDefendantAsSubject(final CourtApplicationParty courtApplicationParty) {
        final InitiateCourtApplicationProceedings initiateCourtApplicationProceedings =
                initiateCourtApplicationProceedings()
                        .withCourtApplication(courtApplication()
                                .withId(applicationId)
                                .withType(courtApplicationType().withLinkType(LinkType.LINKED).build())
                                .withSubject(courtApplicationParty)
                                .build())
                        .withCourtHearing(CourtHearingRequest.courtHearingRequest().build())
                        .withSummonsApprovalRequired(false)
                        .build();

        applicationAggregate.initiateCourtApplicationProceedings(initiateCourtApplicationProceedings, false, false);

        applicationAggregate.createCourtApplication(initiateCourtApplicationProceedings.getCourtApplication(), null);
    }

    private void createApplicationInAggregateForPersonDefendantAsRespondent(final CourtApplicationParty courtApplicationParty) {
        final CourtApplicationParty respondents = CourtApplicationParty.courtApplicationParty()
                .withId(masterDefendantId)
                .withMasterDefendant(MasterDefendant.masterDefendant()
                        .withMasterDefendantId(masterDefendantId)
                        .withPersonDefendant(PersonDefendant.personDefendant()
                                .withPersonDetails(Person.person()
                                        .withAddress(Address.address()
                                                .withAddress1("Old Address 1")
                                                .withAddress2("Old Address 2")
                                                .withPostcode("RG2 3WQ").build())
                                        .build())
                                .build())
                        .build())
                .build();
        final InitiateCourtApplicationProceedings initiateCourtApplicationProceedings =
                initiateCourtApplicationProceedings()
                        .withCourtApplication(courtApplication()
                                .withId(applicationId)
                                .withType(courtApplicationType().withLinkType(LinkType.LINKED).build())
                                .withApplicant(courtApplicationParty)
                                .withRespondents(Arrays.asList(respondents))
                                .build())
                        .withCourtHearing(CourtHearingRequest.courtHearingRequest().build())
                        .withSummonsApprovalRequired(false)
                        .build();

        applicationAggregate.initiateCourtApplicationProceedings(initiateCourtApplicationProceedings, false, false);

        applicationAggregate.createCourtApplication(initiateCourtApplicationProceedings.getCourtApplication(), null);
    }
}
