package uk.gov.moj.cpp.prosecution.event.listener;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import uk.gov.justice.core.courts.AssociatedDefenceOrganisation;
import uk.gov.justice.core.courts.DefenceOrganisation;
import uk.gov.justice.core.courts.Defendant;
import uk.gov.justice.core.courts.DefendantDefenceOrganisationChanged;
import uk.gov.justice.core.courts.FundingType;
import uk.gov.justice.core.courts.Hearing;
import uk.gov.justice.core.courts.Organisation;
import uk.gov.justice.core.courts.ProsecutionCase;
import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.common.converter.ObjectToJsonObjectConverter;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.messaging.Metadata;
import uk.gov.moj.cpp.progression.domain.constant.CaseStatusEnum;
import uk.gov.moj.cpp.prosecutioncase.event.listener.DefendantDefenceOrganisationChangedListener;
import uk.gov.moj.cpp.prosecutioncase.persistence.entity.CaseDefendantHearingEntity;
import uk.gov.moj.cpp.prosecutioncase.persistence.entity.CaseDefendantHearingKey;
import uk.gov.moj.cpp.prosecutioncase.persistence.entity.HearingEntity;
import uk.gov.moj.cpp.prosecutioncase.persistence.entity.ProsecutionCaseEntity;
import uk.gov.moj.cpp.prosecutioncase.persistence.repository.CaseDefendantHearingRepository;
import uk.gov.moj.cpp.prosecutioncase.persistence.repository.HearingRepository;
import uk.gov.moj.cpp.prosecutioncase.persistence.repository.ProsecutionCaseRepository;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonReader;
import java.io.StringReader;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import static java.util.UUID.randomUUID;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class DefendantDefenceOrganisationChangedListenerTest {

    private UUID hearingId;
    private UUID caseId;
    private UUID defendantId;

    @Mock
    private JsonObjectToObjectConverter jsonObjectToObjectConverter;

    @Mock
    private ObjectToJsonObjectConverter objectToJsonObjectConverter;

    @Mock
    private JsonEnvelope envelope;

    @Mock
    private ProsecutionCaseRepository repository;

    @Captor
    private ArgumentCaptor<CaseDefendantHearingEntity> argumentCaptorHearingResultLineEntity;

    @Mock
    private JsonObject payload;

    @Mock
    private Metadata metadata;

    @Mock
    private DefendantDefenceOrganisationChanged defendantDefenceOrganisationChanged;

    @InjectMocks
    private DefendantDefenceOrganisationChangedListener eventListener;

    @Mock
    private HearingRepository hearingRepository;

    @Mock
    private CaseDefendantHearingRepository caseDefendantHearingRepository;

    @Mock
    private ProsecutionCaseEntity prosecutionCaseEntity;

    @Mock
    private HearingEntity hearingEntity;

    @Captor
    private ArgumentCaptor<ProsecutionCaseEntity> argumentCaptor;

    @Captor
    private ArgumentCaptor<HearingEntity> hearingEntityArgumentCaptor;

    @Before
    public void setUp() {
        hearingId = randomUUID();
        caseId = randomUUID();
        defendantId = randomUUID();
    }

    @Test
    public void shouldHandleDefendantDefenceOrganisationChanged() throws Exception {

        when(envelope.payloadAsJsonObject()).thenReturn(payload);
        when(jsonObjectToObjectConverter.convert(payload, DefendantDefenceOrganisationChanged.class)).thenReturn(defendantDefenceOrganisationChanged);
        when(defendantDefenceOrganisationChanged.getProsecutionCaseId()).thenReturn(caseId);
        when(defendantDefenceOrganisationChanged.getDefendantId()).thenReturn(defendantId);
        final AssociatedDefenceOrganisation associatedDefenceOrganisation = AssociatedDefenceOrganisation.associatedDefenceOrganisation()
                .withDefenceOrganisation(DefenceOrganisation.defenceOrganisation()
                        .withLaaContractNumber("LAA123")
                        .withOrganisation(Organisation.organisation()
                                .withName("Org1")
                                .build())
                        .build())
                .withApplicationReference("ABC1234")
                .withAssociationStartDate(LocalDate.now())
                .withAssociationEndDate(LocalDate.now().plusYears(1))
                .withFundingType(FundingType.REPRESENTATION_ORDER)
                .withIsAssociatedByLAA(true)
                .build();
        when(defendantDefenceOrganisationChanged.getAssociatedDefenceOrganisation()).thenReturn(associatedDefenceOrganisation);

        when(envelope.metadata()).thenReturn(metadata);

        final UUID prosecutionCaseId = randomUUID();



        final JsonObject jsonObject = Json.createObjectBuilder()
                .add("payload", Json.createObjectBuilder()
                        .add("defendants", Json.createArrayBuilder().add(Json.createObjectBuilder()
                                .add("id", defendantId.toString()).build())
                                .build())
                        .build()).build();

        final ProsecutionCase prosCase = ProsecutionCase.prosecutionCase()
                .withDefendants(getDefendants(defendantId, prosecutionCaseId, associatedDefenceOrganisation))
                .withCaseStatus(CaseStatusEnum.INACTIVE.getDescription())
                .build();
        final Hearing hearing = Hearing.hearing()
                .withProsecutionCases(Arrays.asList(prosCase))
                .withId(hearingId)
                .build();

        final List<CaseDefendantHearingEntity> caseDefendantHearingEntityList = new ArrayList<>();
        final CaseDefendantHearingEntity caseDefendantHearingEntity = new CaseDefendantHearingEntity();
        caseDefendantHearingEntity.setId(new CaseDefendantHearingKey(caseId, defendantId, hearingId));
        caseDefendantHearingEntity.setHearing(hearingEntity);
        caseDefendantHearingEntityList.add(caseDefendantHearingEntity);

        when(jsonObjectToObjectConverter.convert(jsonObject, ProsecutionCase.class)).thenReturn(prosCase);
        when(hearingEntity.getPayload()).thenReturn(jsonObject.toString());
        when(jsonObjectToObjectConverter.convert(jsonObject, Hearing.class)).thenReturn(hearing);
        when(repository.findByCaseId(caseId)).thenReturn(prosecutionCaseEntity);

        when(prosecutionCaseEntity.getPayload()).thenReturn(jsonObject.toString());
        when(prosecutionCaseEntity.getCaseId()).thenReturn(prosecutionCaseId);
        when(objectToJsonObjectConverter.convert(prosCase)).thenReturn(jsonObject);
        when(objectToJsonObjectConverter.convert(hearing)).thenReturn(jsonObject);
        when(repository.findByCaseId(prosecutionCaseId)).thenReturn(prosecutionCaseEntity);
        when(caseDefendantHearingRepository.findByDefendantId(defendantId)).thenReturn(caseDefendantHearingEntityList);

        eventListener.processDefendantDefenceOrganisationChanged(envelope);
        verify(repository).save(argumentCaptor.capture());
        verify(hearingRepository).save(hearingEntityArgumentCaptor.capture());


        final ProsecutionCase prosecutionCase = this.jsonObjectToObjectConverter.convert
                (jsonFromString(argumentCaptor.getValue().getPayload()), ProsecutionCase.class);
        assertThat(prosecutionCase.getCaseStatus(), equalTo(CaseStatusEnum.INACTIVE.getDescription()));
        assertThat(prosecutionCase.getDefendants().get(0).getAssociatedDefenceOrganisation().getDefenceOrganisation().getLaaContractNumber(), equalTo("LAA123"));
        assertThat(prosecutionCase.getDefendants().get(0).getAssociatedDefenceOrganisation().getApplicationReference(), equalTo("ABC1234"));

        assertThat(prosecutionCase.getDefendants().get(0).getAssociatedDefenceOrganisation().getDefenceOrganisation().getOrganisation().getName(), equalTo("Org1"));
        assertThat(prosecutionCase.getDefendants().get(0).getAssociatedDefenceOrganisation().getFundingType(), equalTo(FundingType.REPRESENTATION_ORDER));

        final Hearing resultedHearing = this.jsonObjectToObjectConverter.convert
                (jsonFromString(hearingEntityArgumentCaptor.getValue().getPayload()), Hearing.class);
        assertThat(resultedHearing.getProsecutionCases().get(0).getCaseStatus(), equalTo(CaseStatusEnum.INACTIVE.getDescription()));
        assertThat(resultedHearing.getProsecutionCases().get(0).getDefendants().get(0).getAssociatedDefenceOrganisation().getDefenceOrganisation().getLaaContractNumber(), equalTo("LAA123"));
        assertThat(resultedHearing.getProsecutionCases().get(0).getDefendants().get(0).getAssociatedDefenceOrganisation().getDefenceOrganisation().getOrganisation().getName(), equalTo("Org1"));
        assertThat(resultedHearing.getProsecutionCases().get(0).getDefendants().get(0).getAssociatedDefenceOrganisation().getFundingType(), equalTo(FundingType.REPRESENTATION_ORDER));

    }

    private List<Defendant> getDefendants(final UUID defendantId, final UUID prosecutionCaseId, final AssociatedDefenceOrganisation associatedDefenceOrganisation) {
        List<Defendant> defendantList = new ArrayList<>();
         defendantList.add(Defendant.defendant().
                withId(defendantId)
                .withProsecutionCaseId(prosecutionCaseId)
                .withAssociatedDefenceOrganisation(associatedDefenceOrganisation)
                .withLegalAidStatus("Granted")
                .build());
         return defendantList;
    }

    private JsonObject jsonFromString(final String jsonObjectStr) {

        JsonObject object;
        try (JsonReader jsonReader = Json.createReader(new StringReader(jsonObjectStr))) {
            object = jsonReader.readObject();
        }

        return object;
    }


}
