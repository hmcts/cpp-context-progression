package uk.gov.moj.cpp.prosecution.event.listener;

import static java.util.UUID.randomUUID;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.test.utils.core.reflection.ReflectionUtil.setField;

import uk.gov.justice.core.courts.Address;
import uk.gov.justice.core.courts.AssociatedPerson;
import uk.gov.justice.core.courts.ContactNumber;
import uk.gov.justice.core.courts.CourtApplication;
import uk.gov.justice.core.courts.CourtApplicationParty;
import uk.gov.justice.core.courts.CourtApplicationRespondent;
import uk.gov.justice.core.courts.Defendant;
import uk.gov.justice.core.courts.DefendantUpdate;
import uk.gov.justice.core.courts.Ethnicity;
import uk.gov.justice.core.courts.HearingResultedCaseUpdated;
import uk.gov.justice.core.courts.Offence;
import uk.gov.justice.core.courts.Person;
import uk.gov.justice.core.courts.PersonDefendant;
import uk.gov.justice.core.courts.ProsecutionCase;
import uk.gov.justice.core.courts.ProsecutionCaseDefendantUpdated;
import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.common.converter.ListToJsonArrayConverter;
import uk.gov.justice.services.common.converter.ObjectToJsonObjectConverter;
import uk.gov.justice.services.common.converter.StringToJsonObjectConverter;
import uk.gov.justice.services.common.converter.jackson.ObjectMapperProducer;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.messaging.Metadata;
import uk.gov.moj.cpp.progression.domain.constant.CaseStatusEnum;
import uk.gov.moj.cpp.prosecutioncase.event.listener.ProsecutionCaseDefendantUpdatedEventListener;
import uk.gov.moj.cpp.prosecutioncase.persistence.entity.CourtApplicationEntity;
import uk.gov.moj.cpp.prosecutioncase.persistence.entity.ProsecutionCaseEntity;
import uk.gov.moj.cpp.prosecutioncase.persistence.repository.CourtApplicationRepository;
import uk.gov.moj.cpp.prosecutioncase.persistence.repository.ProsecutionCaseRepository;
import uk.gov.moj.cpp.prosecutioncase.persistence.repository.mapping.SearchProsecutionCase;

import java.io.StringReader;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonReader;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.runners.MockitoJUnitRunner;


@RunWith(MockitoJUnitRunner.class)
public class ProsecutionDefendantUpdatedEventListenerTest {

    @Mock
    private JsonObjectToObjectConverter jsonObjectToObjectConverter;

    @Mock
    private ObjectToJsonObjectConverter objectToJsonObjectConverter;

    @Mock
    private ProsecutionCaseRepository repository;

    @Mock
    private CourtApplicationRepository courtApplicationRepository;

    @Mock
    private JsonEnvelope envelope;

    @Mock
    private ProsecutionCaseDefendantUpdated prosecutionCaseDefendantUpdated;

    @Mock
    private HearingResultedCaseUpdated hearingResultedCaseUpdated;

    @Mock
    private DefendantUpdate defendant;


    @Mock
    private List<Defendant> defandantsList;

    @Mock
    private ProsecutionCaseEntity prosecutionCaseEntity;

    @Mock
    private List<CourtApplicationEntity> applicationEntities;

    @Captor
    private ArgumentCaptor<ProsecutionCaseEntity> argumentCaptor;

    @Mock
    private JsonObject payload;

    @Mock
    private Metadata metadata;

    @Mock
    private ProsecutionCase prosecutionCase;

    @InjectMocks
    private ProsecutionCaseDefendantUpdatedEventListener eventListener;

    @Mock
    private StringToJsonObjectConverter stringToJsonObjectConverterMock;

    @Mock
    private ObjectMapper objectMapper;

    @Spy
    private ListToJsonArrayConverter jsonConverter;

    @Mock
    private SearchProsecutionCase searchProsecutionCase;

    @Before
    public void initMocks() {

        setField(this.jsonConverter, "mapper",
                new ObjectMapperProducer().objectMapper());
        setField(this.jsonConverter, "stringToJsonObjectConverter",
                new StringToJsonObjectConverter());
    }


    @Test
    public void shouldHandleProsecutionCaseDefendantUpdatedEvent() throws Exception {

        when(envelope.payloadAsJsonObject()).thenReturn(payload);
        when(jsonObjectToObjectConverter.convert(payload, ProsecutionCaseDefendantUpdated.class))
                .thenReturn(prosecutionCaseDefendantUpdated);
        when(envelope.metadata()).thenReturn(metadata);
        when(defendant.getId()).thenReturn(randomUUID());
        when(prosecutionCaseDefendantUpdated.getDefendant()).thenReturn(defendant);
        final JsonObject jsonObject = Json.createObjectBuilder()
                .add("payload", Json.createObjectBuilder()
                        .add("defendants", Json.createArrayBuilder().add(Json.createObjectBuilder()
                                .add("id", defendant.getId().toString()).build())
                                .build())
                        .build()).build();

        when(jsonObjectToObjectConverter.convert(jsonObject, ProsecutionCase.class))
                .thenReturn(prosecutionCase);
        when(prosecutionCaseEntity.getPayload()).thenReturn(jsonObject.toString());
        when(objectToJsonObjectConverter.convert(defendant)).thenReturn(jsonObject);
        when(courtApplicationRepository.findByLinkedCaseId(defendant.getProsecutionCaseId())).thenReturn(applicationEntities);
        when(objectToJsonObjectConverter.convert(prosecutionCase)).thenReturn(jsonObject);
        when(repository.findByCaseId(defendant.getProsecutionCaseId())).thenReturn(prosecutionCaseEntity);
        eventListener.processProsecutionCaseDefendantUpdated(envelope);
        verify(repository).save(argumentCaptor.capture());
    }

    @Test
    public void shouldUpdateMatchedRespondents() {

        List<CourtApplicationRespondent> courtApplicationRespondentList = new ArrayList<>();
        UUID commonUUID = randomUUID();
        Defendant defendant1 = Defendant.defendant().withId(commonUUID).build();
        Defendant defendant2 = Defendant.defendant().withId(commonUUID).build();
        Defendant defendant3 = Defendant.defendant().withId(randomUUID()).build();

        List<Defendant> defendantList = getDefendants(commonUUID, commonUUID, commonUUID, randomUUID(), Arrays.asList(randomUUID()));
        defendantList.add(defendant1);
        defendantList.add(defendant2);
        defendantList.add(defendant3);

        CourtApplicationParty party1 = CourtApplicationParty.courtApplicationParty().withDefendant(defendant1).build();
        CourtApplicationParty party2 = CourtApplicationParty.courtApplicationParty().withDefendant(defendant2).build();
        CourtApplicationParty party3 = CourtApplicationParty.courtApplicationParty().withDefendant(defendant3).build();

        CourtApplicationRespondent courtApplicationRespondent1 = CourtApplicationRespondent.courtApplicationRespondent().withPartyDetails(party1).build();
        CourtApplicationRespondent courtApplicationRespondent2 = CourtApplicationRespondent.courtApplicationRespondent().withPartyDetails(party2).build();
        CourtApplicationRespondent courtApplicationRespondent3 = CourtApplicationRespondent.courtApplicationRespondent().withPartyDetails(party3).build();

        courtApplicationRespondentList.add(courtApplicationRespondent1);
        courtApplicationRespondentList.add(courtApplicationRespondent2);
        courtApplicationRespondentList.add(courtApplicationRespondent3);

        CourtApplication courtApplication = CourtApplication.courtApplication().withRespondents(courtApplicationRespondentList).build();

        DefendantUpdate defendantUpdate = DefendantUpdate.defendantUpdate().withId(commonUUID).build();

        when(envelope.payloadAsJsonObject()).thenReturn(payload);
        when(jsonObjectToObjectConverter.convert(payload, ProsecutionCaseDefendantUpdated.class)).thenReturn(prosecutionCaseDefendantUpdated);
        when(envelope.metadata()).thenReturn(metadata);
        when(defendant.getId()).thenReturn(randomUUID());
        when(prosecutionCaseDefendantUpdated.getDefendant()).thenReturn(defendantUpdate);
        final JsonObject jsonObject = Json.createObjectBuilder()
                .add("payload", Json.createObjectBuilder()
                        .add("defendants", Json.createArrayBuilder().add(Json.createObjectBuilder()
                                .add("id", defendant.getId().toString()).build())
                                .build())
                        .build()).build();

        ProsecutionCase prosecutionCase1 = ProsecutionCase.prosecutionCase().withDefendants(defendantList).build();
        List<CourtApplicationEntity> courtApplicationEntityList = new ArrayList<>();
        CourtApplicationEntity courtApplicationEntity = new CourtApplicationEntity();
        courtApplicationEntityList.add(courtApplicationEntity);

        when(jsonObjectToObjectConverter.convert(jsonObject, ProsecutionCase.class)).thenReturn(prosecutionCase1);
        when(jsonObjectToObjectConverter.convert(jsonObject, CourtApplication.class)).thenReturn(courtApplication);
        when(stringToJsonObjectConverterMock.convert(courtApplicationEntity.getPayload())).thenReturn(jsonObject);
        when(prosecutionCaseEntity.getPayload()).thenReturn(jsonObject.toString());
        when(objectToJsonObjectConverter.convert(defendant)).thenReturn(jsonObject);
        when(courtApplicationRepository.findByLinkedCaseId(defendant1.getProsecutionCaseId())).thenReturn(courtApplicationEntityList);
        when(objectToJsonObjectConverter.convert(prosecutionCase1)).thenReturn(jsonObject);
        when(objectToJsonObjectConverter.convert(any(CourtApplication.class))).thenReturn(jsonObject);
        when(repository.findByCaseId(defendant.getProsecutionCaseId())).thenReturn(prosecutionCaseEntity);
        when(searchProsecutionCase.makeSearchable(prosecutionCase1, defendant1)).thenReturn(null);
        eventListener.processProsecutionCaseDefendantUpdated(envelope);
        verify(repository).save(argumentCaptor.capture());

    }

    @Test
    public void shouldUpdateDefendantButRetainOriginalValuesThatAreNotPassedFromUI() {

        final UUID defendantId = randomUUID();
        final UUID masterDefendantId = randomUUID();
        final UUID prosecutionCaseId = randomUUID();
        final UUID selfDefinedEthnicityId = randomUUID();
        final UUID observedEthnicityId = randomUUID();
        final Defendant defendant1 = prepareDefendantWithAssociatedPerson(defendantId, masterDefendantId, prosecutionCaseId, selfDefinedEthnicityId, observedEthnicityId);

        CourtApplicationRespondent courtApplicationRespondent1 = CourtApplicationRespondent.courtApplicationRespondent()
                .withPartyDetails(CourtApplicationParty.courtApplicationParty().withDefendant(defendant1).build())
                .build();

        List<CourtApplicationRespondent> courtApplicationRespondentList = new ArrayList<>();
        courtApplicationRespondentList.add(courtApplicationRespondent1);
        CourtApplication courtApplication = CourtApplication.courtApplication().withRespondents(courtApplicationRespondentList).build();

        final LocalDate updatedDoB = LocalDate.of(2005, 12, 27);
        DefendantUpdate defendantUpdate = prepareDefendantUpdate(selfDefinedEthnicityId, updatedDoB, defendantId);

        when(envelope.payloadAsJsonObject()).thenReturn(payload);
        when(jsonObjectToObjectConverter.convert(payload, ProsecutionCaseDefendantUpdated.class)).thenReturn(prosecutionCaseDefendantUpdated);
        when(envelope.metadata()).thenReturn(metadata);
        when(defendant.getId()).thenReturn(defendantId);
        when(prosecutionCaseDefendantUpdated.getDefendant()).thenReturn(defendantUpdate);

        final JsonObject jsonObject = Json.createObjectBuilder()
                .add("payload", Json.createObjectBuilder()
                        .add("defendants", Json.createArrayBuilder().add(Json.createObjectBuilder()
                                .add("id", defendant.getId().toString()).build())
                                .build())
                        .build()).build();

        List<Defendant> defendantList = getDefendants(defendantId, defendantId, defendantId, randomUUID(), Arrays.asList(randomUUID()));
        defendantList.add(defendant1);

        List<Defendant> defendants = new ArrayList<>();
        defendants.add(defendant1);
        ProsecutionCase prosecutionCase1 = ProsecutionCase.prosecutionCase().withDefendants(defendants).build();
        List<CourtApplicationEntity> courtApplicationEntityList = new ArrayList<>();
        CourtApplicationEntity courtApplicationEntity = new CourtApplicationEntity();
        courtApplicationEntityList.add(courtApplicationEntity);

        when(jsonObjectToObjectConverter.convert(jsonObject, ProsecutionCase.class)).thenReturn(prosecutionCase1);
        when(jsonObjectToObjectConverter.convert(jsonObject, CourtApplication.class)).thenReturn(courtApplication);
        when(stringToJsonObjectConverterMock.convert(courtApplicationEntity.getPayload())).thenReturn(jsonObject);
        when(prosecutionCaseEntity.getPayload()).thenReturn(jsonObject.toString());
        when(courtApplicationRepository.findByLinkedCaseId(defendant1.getProsecutionCaseId())).thenReturn(courtApplicationEntityList);
        when(objectToJsonObjectConverter.convert(prosecutionCase1)).thenReturn(jsonObject);
        when(objectToJsonObjectConverter.convert(defendant)).thenReturn(jsonObject);
        when(repository.findByCaseId(defendant.getProsecutionCaseId())).thenReturn(prosecutionCaseEntity);
        when(searchProsecutionCase.makeSearchable(prosecutionCase1, defendant1)).thenReturn(null);

        final JsonObject updatedProsecutionCaseJson = prepareUpdatedProsecutionCase();
        ArgumentCaptor<ProsecutionCase> prosecutionCaseArgumentCaptor = ArgumentCaptor.forClass(ProsecutionCase.class);
        when(objectToJsonObjectConverter.convert(prosecutionCaseArgumentCaptor.capture())).thenReturn(updatedProsecutionCaseJson);

        eventListener.processProsecutionCaseDefendantUpdated(envelope);

        verify(repository).save(argumentCaptor.capture());
        final ProsecutionCaseEntity value = argumentCaptor.getValue();
        verifyProsecutionCaseHasBeenUpdated(value);

        final ProsecutionCase allValues = prosecutionCaseArgumentCaptor.getValue();

        verifyProsecutionCaseAllValues(allValues, masterDefendantId, prosecutionCaseId, selfDefinedEthnicityId, observedEthnicityId, updatedDoB);

    }

    @Test
    public void shouldUpdateDefendantWithoutExistingAssociatedPeopleButRetainOriginalValuesThatAreNotPassedFromUI() {

        final UUID defendantId = randomUUID();
        final UUID masterDefendantId = randomUUID();
        final UUID prosecutionCaseId = randomUUID();
        final UUID selfDefinedEthnicityId = randomUUID();
        final UUID observedEthnicityId = randomUUID();
        final Defendant defendant1 = prepareDefendant(defendantId, masterDefendantId, prosecutionCaseId, observedEthnicityId);


        CourtApplicationRespondent courtApplicationRespondent1 = CourtApplicationRespondent.courtApplicationRespondent()
                .withPartyDetails(CourtApplicationParty.courtApplicationParty()
                        .withDefendant(defendant1).
                                build())
                .build();
        List<CourtApplicationRespondent> courtApplicationRespondentList = new ArrayList<>();
        courtApplicationRespondentList.add(courtApplicationRespondent1);
        CourtApplication courtApplication = CourtApplication.courtApplication().withRespondents(courtApplicationRespondentList).build();

        final LocalDate newAssociatedPersonDoB = LocalDate.of(1986, 1, 4);
        DefendantUpdate defendantUpdate = prepareDefendantUpdateForAssociatedPerson(selfDefinedEthnicityId, newAssociatedPersonDoB, defendantId);

        when(envelope.payloadAsJsonObject()).thenReturn(payload);
        when(jsonObjectToObjectConverter.convert(payload, ProsecutionCaseDefendantUpdated.class)).thenReturn(prosecutionCaseDefendantUpdated);
        when(envelope.metadata()).thenReturn(metadata);
        when(defendant.getId()).thenReturn(defendantId);
        when(prosecutionCaseDefendantUpdated.getDefendant()).thenReturn(defendantUpdate);
        final JsonObject jsonObject = Json.createObjectBuilder()
                .add("payload", Json.createObjectBuilder()
                        .add("defendants", Json.createArrayBuilder().add(Json.createObjectBuilder()
                                .add("id", defendant.getId().toString()).build())
                                .build())
                        .build()).build();

        List<Defendant> defendantList = getDefendants(defendantId, defendantId, defendantId, randomUUID(), Arrays.asList(randomUUID()));
        defendantList.add(defendant1);

        List<Defendant> defendants = new ArrayList<>();
        defendants.add(defendant1);
        ProsecutionCase prosecutionCase1 = ProsecutionCase.prosecutionCase().withDefendants(defendants).build();
        List<CourtApplicationEntity> courtApplicationEntityList = new ArrayList<>();
        CourtApplicationEntity courtApplicationEntity = new CourtApplicationEntity();
        courtApplicationEntityList.add(courtApplicationEntity);

        when(jsonObjectToObjectConverter.convert(jsonObject, ProsecutionCase.class)).thenReturn(prosecutionCase1);
        when(jsonObjectToObjectConverter.convert(jsonObject, CourtApplication.class)).thenReturn(courtApplication);
        when(stringToJsonObjectConverterMock.convert(courtApplicationEntity.getPayload())).thenReturn(jsonObject);
        when(prosecutionCaseEntity.getPayload()).thenReturn(jsonObject.toString());
        when(courtApplicationRepository.findByLinkedCaseId(defendant1.getProsecutionCaseId())).thenReturn(courtApplicationEntityList);
        when(objectToJsonObjectConverter.convert(prosecutionCase1)).thenReturn(jsonObject);
        when(objectToJsonObjectConverter.convert(defendant)).thenReturn(jsonObject);
        when(repository.findByCaseId(defendant.getProsecutionCaseId())).thenReturn(prosecutionCaseEntity);
        when(searchProsecutionCase.makeSearchable(prosecutionCase1, defendant1)).thenReturn(null);

        final JsonObject updatedProsecutionCaseJson = prepareUpdatedProsecutionCase();

        ArgumentCaptor<ProsecutionCase> prosecutionCaseArgumentCaptor = ArgumentCaptor.forClass(ProsecutionCase.class);
        when(objectToJsonObjectConverter.convert(prosecutionCaseArgumentCaptor.capture())).thenReturn(updatedProsecutionCaseJson);

        eventListener.processProsecutionCaseDefendantUpdated(envelope);

        verify(repository).save(argumentCaptor.capture());
        final ProsecutionCaseEntity value = argumentCaptor.getValue();
        verifyProsecutionCaseHasBeenUpdated(value);

        final ProsecutionCase allValues = prosecutionCaseArgumentCaptor.getValue();

        verifyProsecutionCaseAllValuesWithoutExistingAssociatedPerson(allValues, masterDefendantId, prosecutionCaseId, selfDefinedEthnicityId, observedEthnicityId, newAssociatedPersonDoB);

    }

    @Test
    public void shouldHandleProsecutionCaseUpdatedEvent() throws Exception {

        when(envelope.payloadAsJsonObject()).thenReturn(payload);
        when(jsonObjectToObjectConverter.convert(payload, HearingResultedCaseUpdated.class)).thenReturn(hearingResultedCaseUpdated);
        when(envelope.metadata()).thenReturn(metadata);
        when(defendant.getId()).thenReturn(randomUUID());

        final UUID def1 = randomUUID();
        final UUID def2 = randomUUID();
        final UUID def3 = randomUUID();
        final UUID prosecutionCaseId = randomUUID();

        final UUID of1 = randomUUID();
        final UUID of2= randomUUID();

        final List<Defendant> defsList = getDefendants(def1, def2, def3, prosecutionCaseId, Arrays.asList(of1));

        when(hearingResultedCaseUpdated.getProsecutionCase()).thenReturn(prosecutionCase);
        when(prosecutionCase.getDefendants()).thenReturn(defsList);

        final JsonObject jsonObject = Json.createObjectBuilder()
                .add("payload", Json.createObjectBuilder()
                        .add("defendants", Json.createArrayBuilder().add(Json.createObjectBuilder()
                                .add("id", defendant.getId().toString()).build())
                                .build())
                        .build()).build();

        final ProsecutionCase prosCase = ProsecutionCase.prosecutionCase()
                .withDefendants(getDefendants(def1, def2, def3, prosecutionCaseId, Arrays.asList(of1, of2)))
                .withCaseStatus(CaseStatusEnum.INACTIVE.getDescription())
                .withCpsOrganisation("A01")
                .build();
        when(jsonObjectToObjectConverter.convert(jsonObject, ProsecutionCase.class)).thenReturn(prosCase);

        when(objectToJsonObjectConverter.convert(any(CourtApplication.class))).thenReturn(jsonObject);

        when(repository.findByCaseId(defendant.getProsecutionCaseId())).thenReturn(prosecutionCaseEntity);

        when(prosecutionCaseEntity.getPayload()).thenReturn(jsonObject.toString());
        when(prosecutionCaseEntity.getCaseId()).thenReturn(prosecutionCaseId);
        when(objectToJsonObjectConverter.convert(defendant)).thenReturn(jsonObject);
        when(repository.findByCaseId(prosecutionCaseId)).thenReturn(prosecutionCaseEntity);

        eventListener.processProsecutionCaseUpdated(envelope);
        verify(repository).save(argumentCaptor.capture());
        final ProsecutionCase prosecutionCase = this.jsonObjectToObjectConverter.convert
                (jsonFromString(argumentCaptor.getValue().getPayload()), ProsecutionCase.class);
        assertThat(prosecutionCase.getCpsOrganisation(), is("A01"));
        assertThat(prosecutionCase.getDefendants().get(0).getProceedingsConcluded(), equalTo(true));
        assertThat(prosecutionCase.getDefendants().get(0).getAssociationLockedByRepOrder(), equalTo(true));
        final Optional<Defendant> defendant = prosecutionCase.getDefendants().stream().filter(def -> def.getId().equals(def1)).findFirst();
        assertThat(defendant.isPresent(), is(true));
        assertThat(defendant.get().getOffences().size(), is(2));
        assertThat(defendant.get().getOffences().get(0).getProceedingsConcluded(), equalTo(true));
        assertThat(prosecutionCase.getCaseStatus(), equalTo(CaseStatusEnum.INACTIVE.getDescription()));


    }

    private List<Defendant> getDefendants(final UUID defandantId1, final UUID defandantId2, final UUID defandantId3, final UUID prosecutionCaseId, final List<UUID> offenceIds) {
        final List<Offence> offences = offenceIds.stream().map(id -> Offence.offence().withId(id).withProceedingsConcluded(true).build()).collect(Collectors.toList());
        final Defendant defendant1 = Defendant.defendant().withId(defandantId1).withProceedingsConcluded(true)
                .withOffences(offences)
                .withProsecutionCaseId(prosecutionCaseId)
                .withAssociationLockedByRepOrder(true)
                .build();
        final Defendant defendant2 = Defendant.defendant().withId(defandantId2).withProsecutionCaseId(prosecutionCaseId).build();
        final Defendant defendant3 = Defendant.defendant().withId(defandantId3).withProsecutionCaseId(prosecutionCaseId).build();


        final List<Defendant> defsList = new ArrayList<>();
        defsList.add(defendant1);
        defsList.add(defendant2);
        defsList.add(defendant3);
        return defsList;
    }

    private JsonObject jsonFromString(final String jsonObjectStr) {

        JsonObject object;
        try (JsonReader jsonReader = Json.createReader(new StringReader(jsonObjectStr))) {
            object = jsonReader.readObject();
        }

        return object;
    }

    private Defendant prepareDefendantWithAssociatedPerson(final UUID defendantId, final UUID masterDefendantId, final UUID prosecutionCaseId , final UUID selfDefinedEthnicityId, final UUID observedEthnicityId){
        return Defendant.defendant()
                .withPersonDefendant(PersonDefendant.personDefendant()
                        .withPersonDetails(Person.person()
                                .withLastName("Leeks")
                                .withOccupation("Plumber")
                                .withOccupationCode("PL01")
                                .withEthnicity(Ethnicity.ethnicity()
                                        .withObservedEthnicityId(observedEthnicityId)
                                        .withObservedEthnicityDescription("observedEthnicityDescription")
                                        .withObservedEthnicityCode("observedEthnicityCode")
                                        .build())
                                .withDateOfBirth(LocalDate.of(1965, 12, 27))
                                .build())
                        .withPerceivedBirthYear(1984)
                        .withBailConditions("bailConditions")
                        .build())
                .withId(defendantId)
                .withMasterDefendantId(masterDefendantId)
                .withProsecutionCaseId(prosecutionCaseId)
                .withAssociatedPersons(Arrays.asList(AssociatedPerson.associatedPerson()
                        .withRole("role")
                        .withPerson(Person.person()
                                .withAddress(Address.address()
                                        .withAddress1("address1")
                                        .withPostcode("BA1 1AA")
                                        .build())
                                .withDateOfBirth(LocalDate.of(2001, 04, 02))
                                .withTitle("Mrs")
                                .withContact(ContactNumber.contactNumber()
                                        .withPrimaryEmail("associated@hmcts.net")
                                        .withHome("01234 567890")
                                        .build())
                                .withEthnicity(Ethnicity.ethnicity()
                                        .withSelfDefinedEthnicityId(selfDefinedEthnicityId)
                                        .withSelfDefinedEthnicityCode("selfDefinedEthnicityCode")
                                        .withSelfDefinedEthnicityDescription("selfDefinedEthnicityDescription")
                                        .withObservedEthnicityId(observedEthnicityId)
                                        .withObservedEthnicityDescription("observedEthnicityDescription")
                                        .withObservedEthnicityCode("observedEthnicityCode")
                                        .build())
                                .build())
                        .build()))
                .build();
    }

    private Defendant prepareDefendant(final UUID defendantId, final UUID masterDefendantId, final UUID prosecutionCaseId, final UUID observedEthnicityId) {

        return Defendant.defendant()
                .withPersonDefendant(PersonDefendant.personDefendant()
                        .withPersonDetails(Person.person()
                                .withLastName("Leeks")
                                .withOccupation("Plumber")
                                .withOccupationCode("PL01")
                                .withEthnicity(Ethnicity.ethnicity()
                                        .withObservedEthnicityId(observedEthnicityId)
                                        .withObservedEthnicityDescription("observedEthnicityDescription")
                                        .withObservedEthnicityCode("observedEthnicityCode")
                                        .build())
                                .build())
                        .withPerceivedBirthYear(1984)
                        .withBailConditions("bailConditions")
                        .build())
                .withId(defendantId)
                .withMasterDefendantId(masterDefendantId)
                .withProsecutionCaseId(prosecutionCaseId)
                .build();

    }


    private DefendantUpdate prepareDefendantUpdate(final UUID selfDefinedEthnicityId, final LocalDate updatedDoB, final UUID defendantId){

        return DefendantUpdate.defendantUpdate()
                .withPersonDefendant(PersonDefendant.personDefendant()
                        .withDriverNumber("newDriverNumber")
                        .withPersonDetails(Person.person()
                                .withFirstName("newFirstName")
                                .withLastName("UpdatedLastName")
                                .withEthnicity(Ethnicity.ethnicity()
                                        .withSelfDefinedEthnicityId(selfDefinedEthnicityId)
                                        .build())
                                .withDateOfBirth(updatedDoB)
                                .build())
                        .build())
                .withAssociatedPersons(Arrays.asList(AssociatedPerson.associatedPerson()
                        .withRole("role")
                        .withPerson(Person.person()
                                .withMiddleName("Lara")
                                .build())
                        .build()))
                .withId(defendantId)
                .build();

    }

    private DefendantUpdate prepareDefendantUpdateForAssociatedPerson(final UUID selfDefinedEthnicityId, final LocalDate updatedDoB, final UUID defendantId){

        return DefendantUpdate.defendantUpdate()
                .withPersonDefendant(PersonDefendant.personDefendant()
                        .withDriverNumber("newDriverNumber")
                        .withPersonDetails(Person.person()
                                .withFirstName("newFirstName")
                                .withLastName("UpdatedLastName")
                                .withEthnicity(Ethnicity.ethnicity()
                                        .withSelfDefinedEthnicityId(selfDefinedEthnicityId)
                                        .build())
                                .build())
                        .build())
                .withAssociatedPersons(Arrays.asList(AssociatedPerson.associatedPerson()
                        .withRole("role")
                        .withPerson(Person.person()
                                .withMiddleName("Lara")
                                .withDateOfBirth(updatedDoB)
                                .build())
                        .build()))
                .withId(defendantId)
                .build();

    }

    private JsonObject prepareUpdatedProsecutionCase() {
        return Json.createObjectBuilder()
                .add("payload", Json.createObjectBuilder()
                        .add("defendants", Json.createArrayBuilder().add(Json.createObjectBuilder()
                                .add("id", defendant.getId().toString())
                                .add("personDefendant", Json.createObjectBuilder()
                                        .add("personDetails", Json.createObjectBuilder()
                                                .add("occupation", "Plumber")
                                                .add("occupationCode", "PL01")
                                                .add("firstName", "newFirstName")
                                                .add("lastName", "UpdatedLastName")
                                                .build())
                                        .add("driverNumber", "newDriverNumber")
                                        .add("bailConditions", "bailConditions")
                                        .build()))
                                .build())
                        .build()).build();
    }

    private void verifyProsecutionCaseHasBeenUpdated(ProsecutionCaseEntity value){
        assertTrue(value.getPayload().contains("\"occupation\":\"Plumber\""));
        assertTrue(value.getPayload().contains("\"occupationCode\":\"PL01\""));
        assertTrue(value.getPayload().contains("\"lastName\":\"UpdatedLastName\""));
        assertTrue(value.getPayload().contains("\"firstName\":\"newFirstName\""));
        assertTrue(value.getPayload().contains("\"driverNumber\":\"newDriverNumber\""));
        assertTrue(value.getPayload().contains("\"bailConditions\":\"bailConditions\""));
    }

    private void verifyProsecutionCaseAllValues(final ProsecutionCase allValues, final UUID masterDefendantId, final UUID prosecutionCaseId, final UUID selfDefinedEthnicityId, final UUID observedEthnicityId,  final LocalDate updatedDoB){
        assertThat("New driver number field should be set.", allValues.getDefendants().get(0).getMasterDefendantId(), is(masterDefendantId));
        assertThat("New driver number field should be set.", allValues.getDefendants().get(0).getProsecutionCaseId(), is(prosecutionCaseId));
        assertThat("New driver number field should be set.", allValues.getDefendants().get(0).getPersonDefendant().getDriverNumber(), is("newDriverNumber"));
        assertThat("New firstName field should be set.", allValues.getDefendants().get(0).getPersonDefendant().getPersonDetails().getFirstName(), is("newFirstName"));
        assertThat("New date of birth field should be set.", allValues.getDefendants().get(0).getPersonDefendant().getPersonDetails().getDateOfBirth(), is(updatedDoB));
        assertThat("New selfDefinedEthnicityId field should be set.", allValues.getDefendants().get(0).getPersonDefendant().getPersonDetails().getEthnicity().getSelfDefinedEthnicityId(), is(selfDefinedEthnicityId));
        assertThat("New associated person middle name should be set.", allValues.getDefendants().get(0).getAssociatedPersons().get(0).getPerson().getMiddleName(), is("Lara"));
        assertThat("Original observedEthnicityId should have been retained.", allValues.getDefendants().get(0).getPersonDefendant().getPersonDetails().getEthnicity().getObservedEthnicityId(), is(observedEthnicityId));
        assertThat("Original observedEthnicityCode should have been retained.", allValues.getDefendants().get(0).getPersonDefendant().getPersonDetails().getEthnicity().getObservedEthnicityCode(), is("observedEthnicityCode"));
        assertThat("Original observedEthnicityDescription should have been retained.", allValues.getDefendants().get(0).getPersonDefendant().getPersonDetails().getEthnicity().getObservedEthnicityDescription(), is("observedEthnicityDescription"));
        assertThat("Last name field should be updated", allValues.getDefendants().get(0).getPersonDefendant().getPersonDetails().getLastName(), is("UpdatedLastName"));
        assertThat("Birth Year was deleted in new event so should be null.", allValues.getDefendants().get(0).getPersonDefendant().getPerceivedBirthYear(), is(nullValue()));
        assertThat("Original bailConditions should have been retained.", allValues.getDefendants().get(0).getPersonDefendant().getBailConditions(), is("bailConditions"));
        assertThat("Original occupation should have been retained.", allValues.getDefendants().get(0).getPersonDefendant().getPersonDetails().getOccupation(), is("Plumber"));
        assertThat("Original occupationCode should have been retained.", allValues.getDefendants().get(0).getPersonDefendant().getPersonDetails().getOccupationCode(), is("PL01"));
        assertThat("Original associated person role should have been retained.", allValues.getDefendants().get(0).getAssociatedPersons().get(0).getRole(), is("role"));
        assertThat("Original associated person title should have been retained.", allValues.getDefendants().get(0).getAssociatedPersons().get(0).getPerson().getTitle(), is("Mrs"));
        assertThat("Original associated person dob should have been retained.", allValues.getDefendants().get(0).getAssociatedPersons().get(0).getPerson().getDateOfBirth(), is(LocalDate.of(2001, 04, 02)));
        assertThat("Original associated person contact should have been retained.", allValues.getDefendants().get(0).getAssociatedPersons().get(0).getPerson().getContact().getPrimaryEmail(), is("associated@hmcts.net"));
        assertThat("Original associated person contact should have been retained.", allValues.getDefendants().get(0).getAssociatedPersons().get(0).getPerson().getContact().getHome(), is("01234 567890"));
        assertThat("Original associated person ethnicity should have been retained.", allValues.getDefendants().get(0).getAssociatedPersons().get(0).getPerson().getEthnicity().getSelfDefinedEthnicityId(), is(selfDefinedEthnicityId));
        assertThat("Original associated person ethnicity should have been retained.", allValues.getDefendants().get(0).getAssociatedPersons().get(0).getPerson().getEthnicity().getSelfDefinedEthnicityCode(), is("selfDefinedEthnicityCode"));
        assertThat("Original associated person ethnicity should have been retained.", allValues.getDefendants().get(0).getAssociatedPersons().get(0).getPerson().getEthnicity().getSelfDefinedEthnicityDescription(), is("selfDefinedEthnicityDescription"));
        assertThat("Original associated person ethnicity should have been retained.", allValues.getDefendants().get(0).getAssociatedPersons().get(0).getPerson().getEthnicity().getObservedEthnicityId(), is(observedEthnicityId));
        assertThat("Original associated person ethnicity should have been retained.", allValues.getDefendants().get(0).getAssociatedPersons().get(0).getPerson().getEthnicity().getObservedEthnicityCode(), is("observedEthnicityCode"));
        assertThat("Original associated person ethnicity should have been retained.", allValues.getDefendants().get(0).getAssociatedPersons().get(0).getPerson().getEthnicity().getObservedEthnicityDescription(), is("observedEthnicityDescription"));
        assertThat("Associated person address was deleted so should be null.", allValues.getDefendants().get(0).getAssociatedPersons().get(0).getPerson().getAddress(), is(nullValue()));
    }

    private void verifyProsecutionCaseAllValuesWithoutExistingAssociatedPerson(final ProsecutionCase allValues, final UUID masterDefendantId, final UUID prosecutionCaseId, final UUID selfDefinedEthnicityId, final UUID observedEthnicityId,  final LocalDate updatedDoB){
        assertThat("New driver number field should be set.", allValues.getDefendants().get(0).getMasterDefendantId(), is(masterDefendantId));
        assertThat("New driver number field should be set.", allValues.getDefendants().get(0).getProsecutionCaseId(), is(prosecutionCaseId));
        assertThat("New driver number field should be set.", allValues.getDefendants().get(0).getPersonDefendant().getDriverNumber(), is("newDriverNumber"));
        assertThat("New firstName field should be set.", allValues.getDefendants().get(0).getPersonDefendant().getPersonDetails().getFirstName(), is("newFirstName"));
        assertThat("New selfDefinedEthnicityId field should be set.", allValues.getDefendants().get(0).getPersonDefendant().getPersonDetails().getEthnicity().getSelfDefinedEthnicityId(), is(selfDefinedEthnicityId));
        assertThat("New associated person middle name should be set.", allValues.getDefendants().get(0).getAssociatedPersons().get(0).getPerson().getMiddleName(), is("Lara"));
        assertThat("New associated person middle name should be set.", allValues.getDefendants().get(0).getAssociatedPersons().get(0).getPerson().getDateOfBirth(), is(updatedDoB));
        assertThat("Original observedEthnicityId should have been retained.", allValues.getDefendants().get(0).getPersonDefendant().getPersonDetails().getEthnicity().getObservedEthnicityId(), is(observedEthnicityId));
        assertThat("Original observedEthnicityCode should have been retained.", allValues.getDefendants().get(0).getPersonDefendant().getPersonDetails().getEthnicity().getObservedEthnicityCode(), is("observedEthnicityCode"));
        assertThat("Original observedEthnicityDescription should have been retained.", allValues.getDefendants().get(0).getPersonDefendant().getPersonDetails().getEthnicity().getObservedEthnicityDescription(), is("observedEthnicityDescription"));
        assertThat("Last name field should be updated", allValues.getDefendants().get(0).getPersonDefendant().getPersonDetails().getLastName(), is("UpdatedLastName"));
        assertThat("Birth Year was deleted in new event so should be null.", allValues.getDefendants().get(0).getPersonDefendant().getPerceivedBirthYear(), is(nullValue()));
        assertThat("Original bailConditions should have been retained.", allValues.getDefendants().get(0).getPersonDefendant().getBailConditions(), is("bailConditions"));
        assertThat("Original occupation should have been retained.", allValues.getDefendants().get(0).getPersonDefendant().getPersonDetails().getOccupation(), is("Plumber"));
        assertThat("Original occupationCode should have been retained.", allValues.getDefendants().get(0).getPersonDefendant().getPersonDetails().getOccupationCode(), is("PL01"));
        assertThat("Original associated person role should have been retained.", allValues.getDefendants().get(0).getAssociatedPersons().get(0).getRole(), is("role"));
    }




}