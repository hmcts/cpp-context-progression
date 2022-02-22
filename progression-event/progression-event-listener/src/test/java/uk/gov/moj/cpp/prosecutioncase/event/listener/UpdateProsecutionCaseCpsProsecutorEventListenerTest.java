package uk.gov.moj.cpp.prosecutioncase.event.listener;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.test.utils.core.reflection.ReflectionUtil.setField;

import uk.gov.justice.core.courts.Address;
import uk.gov.justice.core.courts.CaseCpsProsecutorUpdated;
import uk.gov.justice.core.courts.ContactNumber;
import uk.gov.justice.core.courts.Hearing;
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

import java.util.Collections;
import java.util.UUID;

import javax.json.JsonObject;
import javax.json.JsonValue;

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

    @Before
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
                .build();
    }
}
