package uk.gov.moj.cpp.prosecutioncase.event.listener;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.test.utils.core.reflection.ReflectionUtil.setField;

import uk.gov.justice.core.courts.Address;
import uk.gov.justice.core.courts.CaseCpsProsecutorUpdated;
import uk.gov.justice.core.courts.ContactNumber;
import uk.gov.justice.core.courts.Defendant;
import uk.gov.justice.core.courts.Hearing;
import uk.gov.justice.core.courts.HearingListingStatus;
import uk.gov.justice.core.courts.Offence;
import uk.gov.justice.core.courts.ProsecutionCase;
import uk.gov.justice.core.courts.Prosecutor;
import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.common.converter.ObjectToJsonObjectConverter;
import uk.gov.justice.services.common.converter.StringToJsonObjectConverter;
import uk.gov.justice.services.common.converter.jackson.ObjectMapperProducer;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.prosecutioncase.persistence.entity.CaseDefendantHearingEntity;
import uk.gov.moj.cpp.prosecutioncase.persistence.entity.HearingEntity;
import uk.gov.moj.cpp.prosecutioncase.persistence.entity.ProsecutionCaseEntity;
import uk.gov.moj.cpp.prosecutioncase.persistence.repository.CaseDefendantHearingRepository;
import uk.gov.moj.cpp.prosecutioncase.persistence.repository.HearingRepository;
import uk.gov.moj.cpp.prosecutioncase.persistence.repository.ProsecutionCaseRepository;
import uk.gov.moj.cpp.prosecutioncase.persistence.repository.mapping.SearchProsecutionCase;

import javax.json.JsonObject;
import javax.json.JsonValue;
import java.util.Collections;
import java.util.UUID;
import static java.util.Arrays.asList;
import javax.json.JsonObject;
import javax.json.JsonValue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class UpdateProsecutionCaseCpsProsecutorEventListenerTest {

    @InjectMocks
    UpdateProsecutionCaseCpsProsecutorEventListener updateProsecutionCaseCpsProsecutorEventListener;

    @Mock
    private JsonEnvelope envelope;

    @Mock
    private ProsecutionCaseRepository repository;

    @Mock
    private HearingRepository hearingRepository;

    @Mock
    private CaseDefendantHearingRepository caseDefendantHearingRepository;

    @Mock
    private SearchProsecutionCase searchCase;

    @Spy
    private ObjectToJsonObjectConverter objectToJsonObjectConverter;

    @Spy
    private JsonObjectToObjectConverter jsonObjectToObjectConverter;

    @Spy
    private StringToJsonObjectConverter stringToJsonObjectConverter;

    @Captor
    ArgumentCaptor<ProsecutionCaseEntity> entityArgumentCaptor;

    @Captor
    ArgumentCaptor<HearingEntity> hearingEntityArgumentCaptor;

    @BeforeEach
    public void setUp() {
        setField(this.objectToJsonObjectConverter, "mapper", new ObjectMapperProducer().objectMapper());
        setField(this.jsonObjectToObjectConverter, "objectMapper", new ObjectMapperProducer().objectMapper());
    }


    @Test
    public void shouldUpdateCpsProsecutorWhenCpsValid(){
        final UUID prosecutionCaseId = UUID.randomUUID();
        final CaseCpsProsecutorUpdated caseCpsProsecutorUpdated = CaseCpsProsecutorUpdated.caseCpsProsecutorUpdated()
                .withProsecutionCaseId(prosecutionCaseId)
                .withProsecutionAuthorityOUCode("oucode")
                .withMajorCreditorCode("major")
                .withContact(ContactNumber.contactNumber().withPrimaryEmail("aaa@aaa").build())
                .withAddress(Address.address().withAddress1("aaaaa").build())
                .withCaseURN("caseUrn")
                .withProsecutionAuthorityCode("ProsecutionAuthorityCode")
                .withProsecutionAuthorityId(UUID.randomUUID())
                .withProsecutionAuthorityName("ProsecutionAuthorityName")
                .withProsecutionAuthorityReference("withProsecutionAuthorityReference")
                .build();

        ProsecutionCase prosecutionCase = ProsecutionCase.prosecutionCase()
                .withTrialReceiptType("Transfer")
                .withId(prosecutionCaseId).build();
        final Hearing hearing = Hearing.hearing().withProsecutionCases(Collections.singletonList(prosecutionCase)).build();
        String payload = objectToJsonObjectConverter.convert(prosecutionCase).toString();
        String hearingPayload = objectToJsonObjectConverter.convert(hearing).toString();
        ProsecutionCaseEntity prosecutionCaseEntity = new ProsecutionCaseEntity();
        prosecutionCaseEntity.setPayload(payload);
        JsonObject jsonObject = objectToJsonObjectConverter.convert(caseCpsProsecutorUpdated);
        when(envelope.payloadAsJsonObject()).thenReturn(jsonObject);
        when(repository.findByCaseId(any())).thenReturn(prosecutionCaseEntity);
        CaseDefendantHearingEntity caseDefendantHearingEntity = new CaseDefendantHearingEntity();
        HearingEntity hearingEntity = new HearingEntity();
        hearingEntity.setPayload(hearingPayload);
        hearingEntity.setListingStatus(HearingListingStatus.HEARING_INITIALISED);
        caseDefendantHearingEntity.setHearing(hearingEntity);
        when(caseDefendantHearingRepository.findByCaseId(any())).thenReturn(Collections.singletonList(caseDefendantHearingEntity));
        doNothing().when(searchCase).updateSearchable(any());


        updateProsecutionCaseCpsProsecutorEventListener.handleUpdateCaseCpsProsecutor(envelope);
        verify(searchCase, times(1)).updateSearchable(any());
        verify(repository).save(entityArgumentCaptor.capture());

        assertThat(stringToJsonObjectConverter.convert(entityArgumentCaptor.getValue().getPayload()).get("prosecutor"), is(objectToJsonObjectConverter.convert(expectedEntity(caseCpsProsecutorUpdated))));
        assertThat(stringToJsonObjectConverter.convert(entityArgumentCaptor.getValue().getPayload()).getString("trialReceiptType"), is("Transfer"));
    }

    @Test
        public void shouldUpdateCpsProsecutorWhenNoHearing(){
        final UUID prosecutionCaseId = UUID.randomUUID();
        final CaseCpsProsecutorUpdated caseCpsProsecutorUpdated = CaseCpsProsecutorUpdated.caseCpsProsecutorUpdated()
                .withProsecutionCaseId(prosecutionCaseId)
                .withProsecutionAuthorityOUCode("oucode")
                .withMajorCreditorCode("major")
                .withContact(ContactNumber.contactNumber().withPrimaryEmail("aaa@aaa").build())
                .withAddress(Address.address().withAddress1("aaaaa").build())
                .withCaseURN("caseUrn")
                .withProsecutionAuthorityCode("ProsecutionAuthorityCode")
                .withProsecutionAuthorityId(UUID.randomUUID())
                .withProsecutionAuthorityName("ProsecutionAuthorityName")
                .withProsecutionAuthorityReference("withProsecutionAuthorityReference")
                .build();

        ProsecutionCase prosecutionCase = ProsecutionCase.prosecutionCase()
                .withTrialReceiptType("Transfer")
                .withId(prosecutionCaseId).build();
        String payload = objectToJsonObjectConverter.convert(prosecutionCase).toString();
        ProsecutionCaseEntity prosecutionCaseEntity = new ProsecutionCaseEntity();
        prosecutionCaseEntity.setPayload(payload);
        JsonObject jsonObject = objectToJsonObjectConverter.convert(caseCpsProsecutorUpdated);
        when(envelope.payloadAsJsonObject()).thenReturn(jsonObject);
        when(repository.findByCaseId(any())).thenReturn(prosecutionCaseEntity);
        CaseDefendantHearingEntity caseDefendantHearingEntity = new CaseDefendantHearingEntity();
        when(caseDefendantHearingRepository.findByCaseId(any())).thenReturn(Collections.singletonList(caseDefendantHearingEntity));
        doNothing().when(searchCase).updateSearchable(any());
        updateProsecutionCaseCpsProsecutorEventListener.handleUpdateCaseCpsProsecutor(envelope);
        verify(searchCase, times(1)).updateSearchable(any());
        verify(repository).save(entityArgumentCaptor.capture());

        assertThat(stringToJsonObjectConverter.convert(entityArgumentCaptor.getValue().getPayload()).get("prosecutor"), is(objectToJsonObjectConverter.convert(expectedEntity(caseCpsProsecutorUpdated))));
        assertThat(stringToJsonObjectConverter.convert(entityArgumentCaptor.getValue().getPayload()).getString("trialReceiptType"), is("Transfer"));
    }

    @Test
    public void shouldUpdateCpsProsecutorWhenHearingWithNoProsecutionCase() {
        final UUID prosecutionCaseId = UUID.randomUUID();
        final CaseCpsProsecutorUpdated caseCpsProsecutorUpdated = CaseCpsProsecutorUpdated.caseCpsProsecutorUpdated()
                .withProsecutionCaseId(prosecutionCaseId)
                .withProsecutionAuthorityOUCode("oucode")
                .withMajorCreditorCode("major")
                .withContact(ContactNumber.contactNumber().withPrimaryEmail("aaa@aaa").build())
                .withAddress(Address.address().withAddress1("aaaaa").build())
                .withCaseURN("caseUrn")
                .withProsecutionAuthorityCode("ProsecutionAuthorityCode")
                .withProsecutionAuthorityId(UUID.randomUUID())
                .withProsecutionAuthorityName("ProsecutionAuthorityName")
                .withProsecutionAuthorityReference("withProsecutionAuthorityReference")
                .build();

        ProsecutionCase prosecutionCase = ProsecutionCase.prosecutionCase()
                .withTrialReceiptType("Transfer")
                .withId(prosecutionCaseId).build();
        final Hearing hearing = Hearing.hearing().build();
        String payload = objectToJsonObjectConverter.convert(prosecutionCase).toString();
        String hearingPayload = objectToJsonObjectConverter.convert(hearing).toString();
        ProsecutionCaseEntity prosecutionCaseEntity = new ProsecutionCaseEntity();
        prosecutionCaseEntity.setPayload(payload);
        JsonObject jsonObject = objectToJsonObjectConverter.convert(caseCpsProsecutorUpdated);
        when(envelope.payloadAsJsonObject()).thenReturn(jsonObject);
        when(repository.findByCaseId(any())).thenReturn(prosecutionCaseEntity);
        CaseDefendantHearingEntity caseDefendantHearingEntity = new CaseDefendantHearingEntity();
        HearingEntity hearingEntity = new HearingEntity();
        hearingEntity.setPayload(hearingPayload);
        hearingEntity.setListingStatus(HearingListingStatus.HEARING_INITIALISED);
        caseDefendantHearingEntity.setHearing(hearingEntity);
        when(caseDefendantHearingRepository.findByCaseId(any())).thenReturn(Collections.singletonList(caseDefendantHearingEntity));
        doNothing().when(searchCase).updateSearchable(any());

        updateProsecutionCaseCpsProsecutorEventListener.handleUpdateCaseCpsProsecutor(envelope);
        verify(searchCase, times(1)).updateSearchable(any());
        verify(repository).save(entityArgumentCaptor.capture());

        assertThat(stringToJsonObjectConverter.convert(entityArgumentCaptor.getValue().getPayload()).get("prosecutor"), is(objectToJsonObjectConverter.convert(expectedEntity(caseCpsProsecutorUpdated))));
        assertThat(stringToJsonObjectConverter.convert(entityArgumentCaptor.getValue().getPayload()).getString("trialReceiptType"), is("Transfer"));
    }

    @Test
    public void  shouldNotUpdateHearingWhichAreNotResulted() {

        final UUID prosecutionCaseId = UUID.randomUUID();
        final CaseCpsProsecutorUpdated caseCpsProsecutorUpdated = CaseCpsProsecutorUpdated.caseCpsProsecutorUpdated()
                .withProsecutionCaseId(prosecutionCaseId)
                .withProsecutionAuthorityOUCode("oucode")
                .withMajorCreditorCode("major")
                .withContact(ContactNumber.contactNumber().withPrimaryEmail("aaa@aaa").build())
                .withAddress(Address.address().withAddress1("aaaaa").build())
                .withCaseURN("caseUrn")
                .withProsecutionAuthorityCode("ProsecutionAuthorityCode")
                .withProsecutionAuthorityId(UUID.randomUUID())
                .withProsecutionAuthorityName("ProsecutionAuthorityName")
                .withProsecutionAuthorityReference("withProsecutionAuthorityReference")
                .build();

        ProsecutionCase prosecutionCase = ProsecutionCase.prosecutionCase()
                .withTrialReceiptType("Transfer")
                .withId(prosecutionCaseId).build();
        final Hearing hearing = Hearing.hearing().build();
        String payload = objectToJsonObjectConverter.convert(prosecutionCase).toString();
        String hearingPayload = objectToJsonObjectConverter.convert(hearing).toString();
        ProsecutionCaseEntity prosecutionCaseEntity = new ProsecutionCaseEntity();
        prosecutionCaseEntity.setPayload(payload);
        JsonObject jsonObject = objectToJsonObjectConverter.convert(caseCpsProsecutorUpdated);
        when(envelope.payloadAsJsonObject()).thenReturn(jsonObject);
        when(repository.findByCaseId(any())).thenReturn(prosecutionCaseEntity);
        CaseDefendantHearingEntity caseDefendantHearingEntity = new CaseDefendantHearingEntity();
        HearingEntity hearingEntity = new HearingEntity();
        hearingEntity.setPayload(hearingPayload);
        hearingEntity.setListingStatus(HearingListingStatus.HEARING_RESULTED);
        when(caseDefendantHearingRepository.findByCaseId(any())).thenReturn(Collections.singletonList(caseDefendantHearingEntity));
        doNothing().when(searchCase).updateSearchable(any());

        updateProsecutionCaseCpsProsecutorEventListener.handleUpdateCaseCpsProsecutor(envelope);
        verify(searchCase, times(1)).updateSearchable(any());
        verify(repository).save(entityArgumentCaptor.capture());

        assertThat(stringToJsonObjectConverter.convert(entityArgumentCaptor.getValue().getPayload()).get("prosecutor"), is(objectToJsonObjectConverter.convert(expectedEntity(caseCpsProsecutorUpdated))));
        assertThat(stringToJsonObjectConverter.convert(entityArgumentCaptor.getValue().getPayload()).getString("trialReceiptType"), is("Transfer"));
        verify(hearingRepository, never()).save(any(HearingEntity.class));
    }

    @Test
    public void shouldUpdateCaseAndHearingCaseWhichAreNotResultedWhereHearingCaseDoesNotHaveAllOffencesFromCase() {

        final UUID prosecutionCaseId = UUID.randomUUID();
        final UUID defendantId = UUID.randomUUID();
        final UUID offenceId1 = UUID.randomUUID();
        final UUID offenceId2 = UUID.randomUUID();
        final CaseCpsProsecutorUpdated caseCpsProsecutorUpdated = CaseCpsProsecutorUpdated.caseCpsProsecutorUpdated()
                .withProsecutionCaseId(prosecutionCaseId)
                .withProsecutionAuthorityOUCode("oucode")
                .withMajorCreditorCode("major")
                .withContact(ContactNumber.contactNumber().withPrimaryEmail("aaa@aaa").build())
                .withAddress(Address.address().withAddress1("aaaaa").build())
                .withCaseURN("caseUrn")
                .withProsecutionAuthorityCode("ProsecutionAuthorityCode")
                .withProsecutionAuthorityId(UUID.randomUUID())
                .withProsecutionAuthorityName("ProsecutionAuthorityName")
                .withProsecutionAuthorityReference("withProsecutionAuthorityReference")
                .build();

        ProsecutionCase prosecutionCase = ProsecutionCase.prosecutionCase()
                .withTrialReceiptType("Transfer")
                .withId(prosecutionCaseId)
                .withDefendants(asList(Defendant.defendant()
                        .withId(defendantId)
                        .withOffences(asList(Offence.offence()
                                .withId(offenceId1)
                                .build(),
                                Offence.offence()
                                        .withId(offenceId2)
                                        .build()))
                        .build()))
                        .build();
        ProsecutionCase hearingProsecutionCase = ProsecutionCase.prosecutionCase()
                .withTrialReceiptType("Transfer")
                .withId(prosecutionCaseId)
                .withDefendants(asList(Defendant.defendant()
                        .withId(defendantId)
                        .withOffences(asList(Offence.offence()
                                .withId(offenceId1)
                                .build()))
                        .build()))
                .build();
        final Hearing hearing = Hearing.hearing()
                .withProsecutionCases(asList(hearingProsecutionCase))
                .build();
        String payload = objectToJsonObjectConverter.convert(prosecutionCase).toString();
        String hearingPayload = objectToJsonObjectConverter.convert(hearing).toString();
        ProsecutionCaseEntity prosecutionCaseEntity = new ProsecutionCaseEntity();
        prosecutionCaseEntity.setPayload(payload);
        JsonObject jsonObject = objectToJsonObjectConverter.convert(caseCpsProsecutorUpdated);
        when(envelope.payloadAsJsonObject()).thenReturn(jsonObject);
        when(repository.findByCaseId(any())).thenReturn(prosecutionCaseEntity);
        CaseDefendantHearingEntity caseDefendantHearingEntity = new CaseDefendantHearingEntity();
        HearingEntity hearingEntity = new HearingEntity();
        hearingEntity.setPayload(hearingPayload);
        hearingEntity.setListingStatus(HearingListingStatus.HEARING_INITIALISED);
        caseDefendantHearingEntity.setHearing(hearingEntity);
        when(caseDefendantHearingRepository.findByCaseId(any())).thenReturn(Collections.singletonList(caseDefendantHearingEntity));
        doNothing().when(searchCase).updateSearchable(any());

        updateProsecutionCaseCpsProsecutorEventListener.handleUpdateCaseCpsProsecutor(envelope);
        verify(searchCase, times(1)).updateSearchable(any());
        verify(repository).save(entityArgumentCaptor.capture());

        final JsonObject prosecutionCaseJson = stringToJsonObjectConverter.convert(entityArgumentCaptor.getValue().getPayload());

        assertThat(prosecutionCaseJson.get("prosecutor"), is(objectToJsonObjectConverter.convert(expectedEntity(caseCpsProsecutorUpdated))));
        assertThat(prosecutionCaseJson.getString("trialReceiptType"), is("Transfer"));
        assertThat(prosecutionCaseJson
                .getJsonArray("defendants").getJsonObject(0)
                .getJsonArray("offences").size(), is(2));
        assertThat(prosecutionCaseJson.getJsonArray("defendants").getJsonObject(0)
                .getJsonArray("offences").getJsonObject(0).getString("id"), is(offenceId1.toString()));
        assertThat(prosecutionCaseJson.getJsonArray("defendants").getJsonObject(0)
                .getJsonArray("offences").getJsonObject(1).getString("id"), is(offenceId2.toString()));

        verify(hearingRepository).save(hearingEntityArgumentCaptor.capture());
        final JsonObject hearingJsonObject = stringToJsonObjectConverter.convert(hearingEntityArgumentCaptor.getValue().getPayload());

        assertThat(hearingJsonObject.getJsonArray("prosecutionCases").getJsonObject(0).get("prosecutor"), is(objectToJsonObjectConverter.convert(expectedEntity(caseCpsProsecutorUpdated))));
        assertThat(hearingJsonObject.getJsonArray("prosecutionCases").getJsonObject(0).getString("trialReceiptType"), is("Transfer"));

        assertThat(hearingJsonObject.getJsonArray("prosecutionCases").getJsonObject(0)
                .getJsonArray("defendants").getJsonObject(0)
                .getJsonArray("offences").getJsonObject(0).getString("id"), is(offenceId1.toString()));
        assertThat(hearingJsonObject.getJsonArray("prosecutionCases").getJsonObject(0)
                .getJsonArray("defendants").getJsonObject(0)
                .getJsonArray("offences").size(), is(1));



    }

    @Test
    public void shouldUpdateCaseAndHearingCaseWhichAreNotResultedWhereHearingCaseDoesHaveAllOffencesFromCase() {

        final UUID prosecutionCaseId = UUID.randomUUID();
        final UUID defendantId = UUID.randomUUID();
        final UUID offenceId1 = UUID.randomUUID();
        final UUID offenceId2 = UUID.randomUUID();
        final CaseCpsProsecutorUpdated caseCpsProsecutorUpdated = CaseCpsProsecutorUpdated.caseCpsProsecutorUpdated()
                .withProsecutionCaseId(prosecutionCaseId)
                .withProsecutionAuthorityOUCode("oucode")
                .withMajorCreditorCode("major")
                .withContact(ContactNumber.contactNumber().withPrimaryEmail("aaa@aaa").build())
                .withAddress(Address.address().withAddress1("aaaaa").build())
                .withCaseURN("caseUrn")
                .withProsecutionAuthorityCode("ProsecutionAuthorityCode")
                .withProsecutionAuthorityId(UUID.randomUUID())
                .withProsecutionAuthorityName("ProsecutionAuthorityName")
                .withProsecutionAuthorityReference("withProsecutionAuthorityReference")
                .build();

        ProsecutionCase prosecutionCase = ProsecutionCase.prosecutionCase()
                .withTrialReceiptType("Transfer")
                .withId(prosecutionCaseId)
                .withDefendants(asList(Defendant.defendant()
                        .withId(defendantId)
                        .withOffences(asList(Offence.offence()
                                        .withId(offenceId1)
                                        .build(),
                                Offence.offence()
                                        .withId(offenceId2)
                                        .build()))
                        .build()))
                .build();
        ProsecutionCase hearingProsecutionCase = ProsecutionCase.prosecutionCase()
                .withTrialReceiptType("Transfer")
                .withId(prosecutionCaseId)
                .withDefendants(asList(Defendant.defendant()
                        .withId(defendantId)
                        .withOffences(asList(Offence.offence()
                                .withId(offenceId1)
                                .build(),
                                Offence.offence()
                                        .withId(offenceId2)
                                        .build()))
                        .build()))
                .build();
        final Hearing hearing = Hearing.hearing()
                .withProsecutionCases(asList(hearingProsecutionCase))
                .build();
        String payload = objectToJsonObjectConverter.convert(prosecutionCase).toString();
        String hearingPayload = objectToJsonObjectConverter.convert(hearing).toString();
        ProsecutionCaseEntity prosecutionCaseEntity = new ProsecutionCaseEntity();
        prosecutionCaseEntity.setPayload(payload);
        JsonObject jsonObject = objectToJsonObjectConverter.convert(caseCpsProsecutorUpdated);
        when(envelope.payloadAsJsonObject()).thenReturn(jsonObject);
        when(repository.findByCaseId(any())).thenReturn(prosecutionCaseEntity);
        CaseDefendantHearingEntity caseDefendantHearingEntity = new CaseDefendantHearingEntity();
        HearingEntity hearingEntity = new HearingEntity();
        hearingEntity.setPayload(hearingPayload);
        hearingEntity.setListingStatus(HearingListingStatus.HEARING_INITIALISED);
        caseDefendantHearingEntity.setHearing(hearingEntity);
        when(caseDefendantHearingRepository.findByCaseId(any())).thenReturn(Collections.singletonList(caseDefendantHearingEntity));
        doNothing().when(searchCase).updateSearchable(any());

        updateProsecutionCaseCpsProsecutorEventListener.handleUpdateCaseCpsProsecutor(envelope);
        verify(searchCase, times(1)).updateSearchable(any());
        verify(repository).save(entityArgumentCaptor.capture());

        final JsonObject prosecutionCaseJson = stringToJsonObjectConverter.convert(entityArgumentCaptor.getValue().getPayload());

        assertThat(prosecutionCaseJson.get("prosecutor"), is(objectToJsonObjectConverter.convert(expectedEntity(caseCpsProsecutorUpdated))));
        assertThat(prosecutionCaseJson.getString("trialReceiptType"), is("Transfer"));
        assertThat(prosecutionCaseJson
                .getJsonArray("defendants").getJsonObject(0)
                .getJsonArray("offences").size(), is(2));
        assertThat(prosecutionCaseJson.getJsonArray("defendants").getJsonObject(0)
                .getJsonArray("offences").getJsonObject(0).getString("id"), is(offenceId1.toString()));
        assertThat(prosecutionCaseJson.getJsonArray("defendants").getJsonObject(0)
                .getJsonArray("offences").getJsonObject(1).getString("id"), is(offenceId2.toString()));

        verify(hearingRepository).save(hearingEntityArgumentCaptor.capture());
        final JsonObject hearingJsonObject = stringToJsonObjectConverter.convert(hearingEntityArgumentCaptor.getValue().getPayload());

        assertThat(hearingJsonObject.getJsonArray("prosecutionCases").getJsonObject(0).get("prosecutor"), is(objectToJsonObjectConverter.convert(expectedEntity(caseCpsProsecutorUpdated))));
        assertThat(hearingJsonObject.getJsonArray("prosecutionCases").getJsonObject(0).getString("trialReceiptType"), is("Transfer"));

        assertThat(hearingJsonObject.getJsonArray("prosecutionCases").getJsonObject(0)
                .getJsonArray("defendants").getJsonObject(0)
                .getJsonArray("offences").getJsonObject(0).getString("id"), is(offenceId1.toString()));
        assertThat(hearingJsonObject.getJsonArray("prosecutionCases").getJsonObject(0)
                .getJsonArray("defendants").getJsonObject(0)
                .getJsonArray("offences").size(), is(2));

    }

    @Test
    public void shouldUpdateCaseAndHearingCaseWhichAreNotResultedWhereHearingHaveOtherCases() {

        final UUID prosecutionCaseId = UUID.randomUUID();
        final UUID prosecutionCaseId2 = UUID.randomUUID();
        final UUID defendantId = UUID.randomUUID();
        final UUID defendantId2 = UUID.randomUUID();
        final UUID offenceId1 = UUID.randomUUID();
        final UUID offenceId2 = UUID.randomUUID();
        final UUID offenceId3 = UUID.randomUUID();
        final CaseCpsProsecutorUpdated caseCpsProsecutorUpdated = CaseCpsProsecutorUpdated.caseCpsProsecutorUpdated()
                .withProsecutionCaseId(prosecutionCaseId)
                .withProsecutionAuthorityOUCode("oucode")
                .withMajorCreditorCode("major")
                .withContact(ContactNumber.contactNumber().withPrimaryEmail("aaa@aaa").build())
                .withAddress(Address.address().withAddress1("aaaaa").build())
                .withCaseURN("caseUrn")
                .withProsecutionAuthorityCode("ProsecutionAuthorityCode")
                .withProsecutionAuthorityId(UUID.randomUUID())
                .withProsecutionAuthorityName("ProsecutionAuthorityName")
                .withProsecutionAuthorityReference("withProsecutionAuthorityReference")
                .build();

        ProsecutionCase prosecutionCase = ProsecutionCase.prosecutionCase()
                .withTrialReceiptType("Transfer")
                .withId(prosecutionCaseId)
                .withDefendants(asList(Defendant.defendant()
                        .withId(defendantId)
                        .withOffences(asList(Offence.offence()
                                        .withId(offenceId1)
                                        .build(),
                                Offence.offence()
                                        .withId(offenceId2)
                                        .build()))
                        .build()))
                .build();
        ProsecutionCase hearingProsecutionCase = ProsecutionCase.prosecutionCase()
                .withTrialReceiptType("Transfer")
                .withId(prosecutionCaseId)
                .withDefendants(asList(Defendant.defendant()
                        .withId(defendantId)
                        .withOffences(asList(Offence.offence()
                                        .withId(offenceId1)
                                        .build(),
                                Offence.offence()
                                        .withId(offenceId2)
                                        .build()))
                        .build()))
                .build();

        ProsecutionCase hearingProsecutionCase2 = ProsecutionCase.prosecutionCase()
                .withTrialReceiptType("Transfer")
                .withId(prosecutionCaseId2)
                .withDefendants(asList(Defendant.defendant()
                        .withId(defendantId2)
                        .withOffences(asList(Offence.offence()
                                        .withId(offenceId3)
                                        .build()))
                        .build()))
                .build();
        final Hearing hearing = Hearing.hearing()
                .withProsecutionCases(asList(hearingProsecutionCase, hearingProsecutionCase2))
                .build();
        String payload = objectToJsonObjectConverter.convert(prosecutionCase).toString();
        String hearingPayload = objectToJsonObjectConverter.convert(hearing).toString();
        ProsecutionCaseEntity prosecutionCaseEntity = new ProsecutionCaseEntity();
        prosecutionCaseEntity.setPayload(payload);
        JsonObject jsonObject = objectToJsonObjectConverter.convert(caseCpsProsecutorUpdated);
        when(envelope.payloadAsJsonObject()).thenReturn(jsonObject);
        when(repository.findByCaseId(any())).thenReturn(prosecutionCaseEntity);
        CaseDefendantHearingEntity caseDefendantHearingEntity = new CaseDefendantHearingEntity();
        HearingEntity hearingEntity = new HearingEntity();
        hearingEntity.setPayload(hearingPayload);
        hearingEntity.setListingStatus(HearingListingStatus.HEARING_INITIALISED);
        caseDefendantHearingEntity.setHearing(hearingEntity);
        when(caseDefendantHearingRepository.findByCaseId(any())).thenReturn(Collections.singletonList(caseDefendantHearingEntity));
        doNothing().when(searchCase).updateSearchable(any());

        updateProsecutionCaseCpsProsecutorEventListener.handleUpdateCaseCpsProsecutor(envelope);
        verify(searchCase, times(1)).updateSearchable(any());
        verify(repository).save(entityArgumentCaptor.capture());

        final JsonObject prosecutionCaseJson = stringToJsonObjectConverter.convert(entityArgumentCaptor.getValue().getPayload());

        assertThat(prosecutionCaseJson.get("prosecutor"), is(objectToJsonObjectConverter.convert(expectedEntity(caseCpsProsecutorUpdated))));
        assertThat(prosecutionCaseJson.getString("trialReceiptType"), is("Transfer"));
        assertThat(prosecutionCaseJson
                .getJsonArray("defendants").getJsonObject(0)
                .getJsonArray("offences").size(), is(2));
        assertThat(prosecutionCaseJson.getJsonArray("defendants").getJsonObject(0)
                .getJsonArray("offences").getJsonObject(0).getString("id"), is(offenceId1.toString()));
        assertThat(prosecutionCaseJson.getJsonArray("defendants").getJsonObject(0)
                .getJsonArray("offences").getJsonObject(1).getString("id"), is(offenceId2.toString()));

        verify(hearingRepository).save(hearingEntityArgumentCaptor.capture());
        final JsonObject hearingJsonObject = stringToJsonObjectConverter.convert(hearingEntityArgumentCaptor.getValue().getPayload());

        assertThat(hearingJsonObject.getJsonArray("prosecutionCases").size(), is(2));
        assertThat(hearingJsonObject.getJsonArray("prosecutionCases").getJsonObject(0).get("prosecutor"), is(objectToJsonObjectConverter.convert(expectedEntity(caseCpsProsecutorUpdated))));
        assertThat(hearingJsonObject.getJsonArray("prosecutionCases").getJsonObject(0).getString("trialReceiptType"), is("Transfer"));

        assertThat(hearingJsonObject.getJsonArray("prosecutionCases").getJsonObject(0)
                .getJsonArray("defendants").getJsonObject(0)
                .getJsonArray("offences").getJsonObject(0).getString("id"), is(offenceId1.toString()));
        assertThat(hearingJsonObject.getJsonArray("prosecutionCases").getJsonObject(0)
                .getJsonArray("defendants").getJsonObject(0)
                .getJsonArray("offences").size(), is(2));

        assertThat(hearingJsonObject.getJsonArray("prosecutionCases").getJsonObject(1)
                .getJsonArray("defendants").getJsonObject(0)
                .getJsonArray("offences").getJsonObject(0).getString("id"), is(offenceId3.toString()));
        assertThat(hearingJsonObject.getJsonArray("prosecutionCases").getJsonObject(1)
                .getJsonArray("defendants").getJsonObject(0)
                .getJsonArray("offences").size(), is(1));

    }



    @Test
    public void shouldUpdateProsecutionCaseErrorFlagWhenCpsInValid(){
        final CaseCpsProsecutorUpdated caseCpsProsecutorUpdated = CaseCpsProsecutorUpdated.caseCpsProsecutorUpdated()
                .withProsecutionCaseId(UUID.randomUUID())
                .withIsCpsOrgVerifyError(true)
                .build();
        ProsecutionCase prosecutionCase = ProsecutionCase.prosecutionCase().build();
        String payload = objectToJsonObjectConverter.convert(prosecutionCase).toString();
        ProsecutionCaseEntity prosecutionCaseEntity = new ProsecutionCaseEntity();
        prosecutionCaseEntity.setPayload(payload);
        JsonObject jsonObject = objectToJsonObjectConverter.convert(caseCpsProsecutorUpdated);
        when(envelope.payloadAsJsonObject()).thenReturn(jsonObject);
        when(repository.findByCaseId(any())).thenReturn(prosecutionCaseEntity);

        updateProsecutionCaseCpsProsecutorEventListener.handleUpdateCaseCpsProsecutor(envelope);

        verify(repository).save(entityArgumentCaptor.capture());

        JsonObject actualEntity = stringToJsonObjectConverter.convert(entityArgumentCaptor.getValue().getPayload());
        assertNull(actualEntity.get("prosecutionCaseIdentifier"));
        assertThat(actualEntity.get("isCpsOrgVerifyError"), is(JsonValue.TRUE));
    }

    private Prosecutor expectedEntity(CaseCpsProsecutorUpdated caseCpsProsecutorUpdated) {
        return Prosecutor.prosecutor()
                .withProsecutorCode(caseCpsProsecutorUpdated.getProsecutionAuthorityCode())
                .withProsecutorId(caseCpsProsecutorUpdated.getProsecutionAuthorityId())
                .withAddress(caseCpsProsecutorUpdated.getAddress())
                .withProsecutorName(caseCpsProsecutorUpdated.getProsecutionAuthorityName())
                .withIsCps(true)
                .build();
    }
}
