package uk.gov.moj.cpp.prosecutioncase.event.listener;

import static java.util.UUID.randomUUID;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import uk.gov.justice.core.courts.Address;
import uk.gov.justice.core.courts.AssociatedDefenceOrganisation;
import uk.gov.justice.core.courts.ContactNumber;
import uk.gov.justice.core.courts.DefenceOrganisation;
import uk.gov.justice.core.courts.Defendant;
import uk.gov.justice.core.courts.FundingType;
import uk.gov.justice.core.courts.Hearing;
import uk.gov.justice.core.courts.Organisation;
import uk.gov.justice.core.courts.ProsecutionCase;
import uk.gov.justice.cpp.progression.events.DefendantDefenceOrganisationSaved;
import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.common.converter.ObjectToJsonObjectConverter;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.messaging.Metadata;
import uk.gov.moj.cpp.progression.domain.constant.CaseStatusEnum;
import uk.gov.moj.cpp.prosecutioncase.persistence.entity.CaseDefendantHearingEntity;
import uk.gov.moj.cpp.prosecutioncase.persistence.entity.CaseDefendantHearingKey;
import uk.gov.moj.cpp.prosecutioncase.persistence.entity.HearingEntity;
import uk.gov.moj.cpp.prosecutioncase.persistence.entity.ProsecutionCaseEntity;
import uk.gov.moj.cpp.prosecutioncase.persistence.repository.CaseDefendantHearingRepository;
import uk.gov.moj.cpp.prosecutioncase.persistence.repository.HearingRepository;
import uk.gov.moj.cpp.prosecutioncase.persistence.repository.ProsecutionCaseRepository;

import java.io.StringReader;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonReader;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class DefendantDefenceOrganisationSavedListenerTest {

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


    @Mock
    private JsonObject payload;

    @Mock
    private Metadata metadata;


    @InjectMocks
    private DefendantDefenceOrganisationSavedListener eventListener;

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

    @BeforeEach
    public void setUp() {
        hearingId = randomUUID();
        caseId = randomUUID();
        defendantId = randomUUID();
    }

    @Test
    public void shouldHandleDefendantDefenceOrganisationSaved() throws Exception {

        when(envelope.payloadAsJsonObject()).thenReturn(payload);
        final DefendantDefenceOrganisationSaved defendantDefenceOrganisationSaved = defendantDefenceOrganisationSaved();
        when(jsonObjectToObjectConverter.convert(payload, DefendantDefenceOrganisationSaved.class)).thenReturn(defendantDefenceOrganisationSaved);
        final AssociatedDefenceOrganisation associatedDefenceOrganisation = AssociatedDefenceOrganisation.associatedDefenceOrganisation()
                .withDefenceOrganisation(DefenceOrganisation.defenceOrganisation()
                        .withLaaContractNumber(defendantDefenceOrganisationSaved.getLaaContractNumber())
                        .withOrganisation(Organisation.organisation()
                                .withName(defendantDefenceOrganisationSaved.getOrganisationName())
                                .withContact(ContactNumber.contactNumber()
                                        .withPrimaryEmail(defendantDefenceOrganisationSaved.getEmail())
                                        .build())
                                .withAddress(Address.address()
                                        .withAddress1(defendantDefenceOrganisationSaved.getAddressLine1())
                                        .withPostcode(defendantDefenceOrganisationSaved.getPostCode())
                                        .build())
                                .build())
                        .build())
                .withAssociationStartDate(LocalDate.now())
                .withAssociationEndDate(LocalDate.now().plusYears(1))
                .withFundingType(FundingType.REPRESENTATION_ORDER)
                .withIsAssociatedByLAA(false)
                .build();

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
        when(objectToJsonObjectConverter.convert(prosCase)).thenReturn(jsonObject);
        when(objectToJsonObjectConverter.convert(hearing)).thenReturn(jsonObject);
        when(caseDefendantHearingRepository.findByDefendantId(defendantId)).thenReturn(caseDefendantHearingEntityList);

        eventListener.processDefendantDefenceOrganisationSaved(envelope);
        verify(repository).save(argumentCaptor.capture());
        verify(hearingRepository).save(hearingEntityArgumentCaptor.capture());


        final ProsecutionCase prosecutionCase = this.jsonObjectToObjectConverter.convert
                (jsonFromString(argumentCaptor.getValue().getPayload()), ProsecutionCase.class);
        assertThat(prosecutionCase.getCaseStatus(), equalTo(CaseStatusEnum.INACTIVE.getDescription()));
        assertThat(prosecutionCase.getDefendants().get(0).getAssociatedDefenceOrganisation().getDefenceOrganisation().getLaaContractNumber(), equalTo(defendantDefenceOrganisationSaved.getLaaContractNumber()));
        assertThat(prosecutionCase.getDefendants().get(0).getAssociatedDefenceOrganisation().getDefenceOrganisation().getOrganisation().getName(), equalTo(defendantDefenceOrganisationSaved.getOrganisationName()));
        assertThat(prosecutionCase.getDefendants().get(0).getAssociatedDefenceOrganisation().getDefenceOrganisation().getOrganisation().getContact().getPrimaryEmail(), equalTo(defendantDefenceOrganisationSaved.getEmail()));
        assertThat(prosecutionCase.getDefendants().get(0).getAssociatedDefenceOrganisation().getDefenceOrganisation().getOrganisation().getAddress().getAddress1(), equalTo(defendantDefenceOrganisationSaved.getAddressLine1()));
        assertThat(prosecutionCase.getDefendants().get(0).getAssociatedDefenceOrganisation().getDefenceOrganisation().getOrganisation().getAddress().getPostcode(), equalTo(defendantDefenceOrganisationSaved.getPostCode()));



        final Hearing resultedHearing = this.jsonObjectToObjectConverter.convert
                (jsonFromString(hearingEntityArgumentCaptor.getValue().getPayload()), Hearing.class);
        assertThat(resultedHearing.getProsecutionCases().get(0).getCaseStatus(), equalTo(CaseStatusEnum.INACTIVE.getDescription()));
        assertThat(resultedHearing.getProsecutionCases().get(0).getDefendants().get(0).getAssociatedDefenceOrganisation().getDefenceOrganisation().getLaaContractNumber(), equalTo(defendantDefenceOrganisationSaved.getLaaContractNumber()));
        assertThat(resultedHearing.getProsecutionCases().get(0).getDefendants().get(0).getAssociatedDefenceOrganisation().getDefenceOrganisation().getOrganisation().getName(), equalTo(defendantDefenceOrganisationSaved.getOrganisationName()));
        assertThat(resultedHearing.getProsecutionCases().get(0).getDefendants().get(0).getAssociatedDefenceOrganisation().getDefenceOrganisation().getOrganisation().getContact().getPrimaryEmail(), equalTo(defendantDefenceOrganisationSaved.getEmail()));
        assertThat(resultedHearing.getProsecutionCases().get(0).getDefendants().get(0).getAssociatedDefenceOrganisation().getDefenceOrganisation().getOrganisation().getAddress().getAddress1(), equalTo(defendantDefenceOrganisationSaved.getAddressLine1()));
        assertThat(resultedHearing.getProsecutionCases().get(0).getDefendants().get(0).getAssociatedDefenceOrganisation().getDefenceOrganisation().getOrganisation().getAddress().getPostcode(), equalTo(defendantDefenceOrganisationSaved.getPostCode()));

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

    private DefendantDefenceOrganisationSaved defendantDefenceOrganisationSaved() {
        return DefendantDefenceOrganisationSaved.defendantDefenceOrganisationSaved()
                .withProsecutionCaseId(caseId)
                .withDefendantId(defendantId)
                .withLaaContractNumber("LAA123")
                .withOrganisationName("Org1")
                .withAddressLine1("10 Downing Street")
                .withPostCode("IG1 1NL")
                .withEmail("abc@xyz.com")
                .build();
    }


}
