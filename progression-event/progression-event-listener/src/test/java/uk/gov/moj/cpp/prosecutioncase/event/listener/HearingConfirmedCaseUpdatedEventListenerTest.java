package uk.gov.moj.cpp.prosecutioncase.event.listener;

import static java.util.Collections.singletonList;
import static java.util.UUID.randomUUID;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.ArgumentCaptor.forClass;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.test.utils.core.reflection.ReflectionUtil.setField;
import static uk.gov.moj.cpp.progression.domain.constant.CaseStatusEnum.SJP_REFERRAL;

import uk.gov.justice.core.courts.Defendant;
import uk.gov.justice.core.courts.Hearing;
import uk.gov.justice.core.courts.HearingListingStatus;
import uk.gov.justice.core.courts.InitiationCode;
import uk.gov.justice.core.courts.Marker;
import uk.gov.justice.core.courts.PoliceOfficerInCase;
import uk.gov.justice.core.courts.ProsecutionCase;
import uk.gov.justice.core.courts.ProsecutionCaseIdentifier;
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

import java.util.UUID;

import uk.gov.justice.services.messaging.JsonObjects;
import javax.json.JsonArray;
import javax.json.JsonObject;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class HearingConfirmedCaseUpdatedEventListenerTest {
    private final UUID id = randomUUID();
    private final UUID defendantId = randomUUID();
    private final String classOfCase = "ProsecutionCase";
    private final String originatingOrganisation = "originatingOrganisation";
    private final String removalReason = "removal Reason";
    private final String statementOfFacts = "Statement of facts";
    private final String statementOfFactsWelsh = "Statement of Facts Welsh";
    private final String policeOfficerRank = "1";
    private final String initiationCode = "C";
    private final String cpsOrganisation = "A01";

    @Spy
    private JsonObjectToObjectConverter jsonObjectToObjectConverter;

    @Spy
    private StringToJsonObjectConverter stringToJsonObjectConverter;

    @Spy
    private ObjectToJsonObjectConverter objectToJsonObjectConverter;

    @Mock
    private ProsecutionCaseRepository prosecutionCaseRepository;

    @Mock
    private JsonEnvelope jsonEnvelope;

    @Captor
    private ArgumentCaptor<ProsecutionCaseEntity> argumentCaptor = forClass(ProsecutionCaseEntity.class);

    @Captor
    private ArgumentCaptor<HearingEntity> hearingEntityArgumentCaptor = forClass(HearingEntity.class);

    @Mock
    private HearingRepository hearingRepository;

    @Mock
    private CaseDefendantHearingRepository caseDefendantHearingRepository;

    @InjectMocks
    private HearingConfirmedCaseUpdatedEventListener hearingConfirmedCaseUpdatedEventListener;

    @BeforeEach
    public void setUp() {
        setField(this.objectToJsonObjectConverter, "mapper",
                new ObjectMapperProducer().objectMapper());
        setField(this.jsonObjectToObjectConverter, "objectMapper",
                new ObjectMapperProducer().objectMapper());
    }

    @Test
    public void updateProsecutionCaseStatus() throws Exception {
        final ProsecutionCase prosecutionCase = getProsecutionCase();
        final JsonObject payload = getPayload(prosecutionCase);

        final HearingEntity hearingEntity = new HearingEntity();
        hearingEntity.setHearingId(randomUUID());
        hearingEntity.setListingStatus(HearingListingStatus.HEARING_INITIALISED);
        final Hearing hearing = Hearing.hearing().withProsecutionCases(singletonList(prosecutionCase))
                .withHasSharedResults(false)
                .build();
        final ObjectMapper objectMapper = new ObjectMapper();
        hearingEntity.setPayload(objectMapper.writeValueAsString(hearing));

        when(jsonEnvelope.payloadAsJsonObject()).thenReturn(payload);

        final ProsecutionCaseEntity prosecutionCaseEntity = new ProsecutionCaseEntity();
        prosecutionCaseEntity.setCaseId(id);
        prosecutionCaseEntity.setPayload(objectToJsonObjectConverter.convert(prosecutionCase).toString());

        final CaseDefendantHearingEntity caseDefendantHearingEntity = new CaseDefendantHearingEntity();
        caseDefendantHearingEntity.setHearing(hearingEntity);
        when(caseDefendantHearingRepository.findByCaseId(prosecutionCase.getId())).thenReturn(singletonList(caseDefendantHearingEntity));
        when(prosecutionCaseRepository.findByCaseId(prosecutionCase.getId())).thenReturn(prosecutionCaseEntity);

        hearingConfirmedCaseUpdatedEventListener.updateProsecutionCaseStatus(jsonEnvelope);

        verify(prosecutionCaseRepository).save(argumentCaptor.capture());
        assertThat(argumentCaptor.getValue(), is(notNullValue()));
        final ProsecutionCaseEntity updatedProsecutionCaseEntity = argumentCaptor.getValue();
        assertThat(updatedProsecutionCaseEntity.getCaseId(), is(id));
        verifyProsecutionCaseData(updatedProsecutionCaseEntity);
        verify(hearingRepository).save(hearingEntityArgumentCaptor.capture());
        HearingEntity savedHearingEntity = hearingEntityArgumentCaptor.getValue();

        final JsonObject jsonObject = stringToJsonObjectConverter.convert(savedHearingEntity.getPayload());
        final Hearing hearing1 = jsonObjectToObjectConverter.convert(jsonObject, Hearing.class);
        assertThat(hearing1.getProsecutionCases().get(0).getCaseStatus(), is(SJP_REFERRAL.getDescription()));
        assertThat(hearing1.getProsecutionCases().get(0).getTrialReceiptType(), is("Voluntary bill"));
    }

    @Test
    public void shouldNotUpdate() throws Exception {
        final ProsecutionCase prosecutionCase = getProsecutionCase();
        final JsonObject payload = getPayload(prosecutionCase);

        final HearingEntity hearingEntity = new HearingEntity();
        hearingEntity.setHearingId(randomUUID());
        hearingEntity.setListingStatus(HearingListingStatus.HEARING_INITIALISED);
        final Hearing hearing = Hearing.hearing().withProsecutionCases(singletonList(prosecutionCase))
                .withHasSharedResults(false)
                .build();
        final ObjectMapper objectMapper = new ObjectMapper();
        hearingEntity.setPayload(objectMapper.writeValueAsString(hearing));

        when(jsonEnvelope.payloadAsJsonObject()).thenReturn(payload);

        final ProsecutionCaseEntity prosecutionCaseEntity = new ProsecutionCaseEntity();
        prosecutionCaseEntity.setCaseId(id);
        prosecutionCaseEntity.setPayload(objectToJsonObjectConverter.convert(prosecutionCase).toString());

        final CaseDefendantHearingEntity caseDefendantHearingEntity = new CaseDefendantHearingEntity();
        caseDefendantHearingEntity.setHearing(hearingEntity);
        when(caseDefendantHearingRepository.findByCaseId(prosecutionCase.getId())).thenReturn(singletonList(caseDefendantHearingEntity));
        when(prosecutionCaseRepository.findByCaseId(prosecutionCase.getId())).thenReturn(prosecutionCaseEntity);

        hearingConfirmedCaseUpdatedEventListener.updateProsecutionCaseStatus(jsonEnvelope);

        verify(prosecutionCaseRepository).save(argumentCaptor.capture());
        assertThat(argumentCaptor.getValue(), is(notNullValue()));
        final ProsecutionCaseEntity updatedProsecutionCaseEntity = argumentCaptor.getValue();
        assertThat(updatedProsecutionCaseEntity.getCaseId(), is(id));
        verifyProsecutionCaseData(updatedProsecutionCaseEntity);
        verify(hearingRepository).save(hearingEntityArgumentCaptor.capture());
        HearingEntity savedHearingEntity = hearingEntityArgumentCaptor.getValue();

        final JsonObject jsonObject = stringToJsonObjectConverter.convert(savedHearingEntity.getPayload());
        final Hearing hearing1 = jsonObjectToObjectConverter.convert(jsonObject, Hearing.class);
        assertThat(hearing1.getProsecutionCases().get(0).getCaseStatus(), is(SJP_REFERRAL.getDescription()));
    }

    private JsonObject getPayload(final ProsecutionCase prosecutionCase) {
        return JsonObjects.createObjectBuilder()
                .add("prosecutionCase", objectToJsonObjectConverter.convert(prosecutionCase))
                .add("caseStatus", SJP_REFERRAL.getDescription())
                .build();
    }

    private ProsecutionCase getProsecutionCase() {
        return ProsecutionCase.prosecutionCase()
                .withId(id)
                .withAppealProceedingsPending(true)
                .withCaseStatus("INACTIVE")
                .withBreachProceedingsPending(true)
                .withClassOfCase(classOfCase)
                .withPoliceOfficerInCase(PoliceOfficerInCase.policeOfficerInCase().withPoliceOfficerRank(policeOfficerRank).build())
                .withCaseMarkers(singletonList(Marker.marker().build()))
                .withProsecutionCaseIdentifier(ProsecutionCaseIdentifier.prosecutionCaseIdentifier().withCaseURN("123").build())
                .withDefendants(singletonList(Defendant.defendant().withId(defendantId).build()))
                .withInitiationCode(InitiationCode.C)
                .withOriginatingOrganisation(originatingOrganisation)
                .withCpsOrganisation(cpsOrganisation)
                .withRemovalReason(removalReason)
                .withStatementOfFacts(statementOfFacts)
                .withStatementOfFactsWelsh(statementOfFactsWelsh)
                .withTrialReceiptType("Voluntary bill")
                .build();
    }

    private void verifyProsecutionCaseData(final ProsecutionCaseEntity updatedProsecutionCaseEntity) {
        final JsonObject convert = stringToJsonObjectConverter.convert(updatedProsecutionCaseEntity.getPayload());

        assertThat(convert.getString("caseStatus"), is(SJP_REFERRAL.getDescription()));
        assertThat(convert.getString("classOfCase"), is(classOfCase));
        assertThat(convert.getBoolean("appealProceedingsPending"), is(true));
        assertThat(convert.getBoolean("breachProceedingsPending"), is(true));
        assertThat(convert.getString("initiationCode"), is(initiationCode));
        assertThat(convert.getString("removalReason"), is(removalReason));
        assertThat(convert.getString("statementOfFactsWelsh"), is(statementOfFactsWelsh));
        assertThat(convert.getString("statementOfFacts"), is(statementOfFacts));
        assertThat(convert.getString("cpsOrganisation"), is(cpsOrganisation));

        final JsonObject policeOfficerInCase = convert.getJsonObject("policeOfficerInCase");
        assertThat(policeOfficerInCase, is(notNullValue()));

        final PoliceOfficerInCase policeOfficerInCase1 = jsonObjectToObjectConverter.convert(policeOfficerInCase, PoliceOfficerInCase.class);
        assertThat(policeOfficerInCase1.getPoliceOfficerRank(), is(policeOfficerRank));

        final JsonArray defendants = convert.getJsonArray("defendants");
        assertThat(defendants, is(notNullValue()));
        assertThat(defendants.size(), is(1));
        assertThat(defendants.getJsonObject(0).getString("id"), is(defendantId.toString()));
    }
}