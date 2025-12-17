package uk.gov.moj.cpp.prosecutioncase.event.listener;

import static java.util.UUID.randomUUID;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.justice.services.test.utils.core.messaging.MetadataBuilderFactory.metadataWithRandomUUID;
import static uk.gov.justice.services.test.utils.core.reflection.ReflectionUtil.setField;

import uk.gov.justice.core.courts.ConvictionDateAdded;
import uk.gov.justice.core.courts.ConvictionDateRemoved;
import uk.gov.justice.core.courts.CourtApplication;
import uk.gov.justice.core.courts.Defendant;
import uk.gov.justice.core.courts.InitiateCourtApplicationProceedings;
import uk.gov.justice.core.courts.JudicialResult;
import uk.gov.justice.core.courts.LaaReference;
import uk.gov.justice.core.courts.Offence;
import uk.gov.justice.core.courts.ProsecutionCase;
import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.common.converter.ObjectToJsonObjectConverter;
import uk.gov.justice.services.common.converter.StringToJsonObjectConverter;
import uk.gov.justice.services.common.converter.jackson.ObjectMapperProducer;
import uk.gov.moj.cpp.progression.test.TestHelper;
import uk.gov.moj.cpp.prosecutioncase.persistence.entity.CourtApplicationEntity;
import uk.gov.moj.cpp.prosecutioncase.persistence.entity.InitiateCourtApplicationEntity;
import uk.gov.moj.cpp.prosecutioncase.persistence.entity.ProsecutionCaseEntity;
import uk.gov.moj.cpp.prosecutioncase.persistence.repository.CourtApplicationRepository;
import uk.gov.moj.cpp.prosecutioncase.persistence.repository.InitiateCourtApplicationRepository;
import uk.gov.moj.cpp.prosecutioncase.persistence.repository.ProsecutionCaseRepository;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class ConvictionDateEventListenerTest {

    @Mock
    private ProsecutionCaseRepository prosecutionCaseRepository;

    @InjectMocks
    private ConvictionDateEventListener convictionDateEventListener;

    @Spy
    private JsonObjectToObjectConverter jsonObjectToObjectConverter;

    @Spy
    private ObjectToJsonObjectConverter objectToJsonObjectConverter;

    @Spy
    private StringToJsonObjectConverter stringToJsonObjectConverter;

    @Mock
    private CourtApplicationRepository courtApplicationRepository;

    @Mock
    private InitiateCourtApplicationRepository initiateCourtApplicationRepository;

    @Captor
    private ArgumentCaptor<CourtApplicationEntity> courtApplicationEntityArgumentCaptor;

    @Captor
    private ArgumentCaptor<InitiateCourtApplicationEntity> initiateCourtApplicationEntityArgumentCaptor;

    @Captor
    private ArgumentCaptor<String> updatedStr;

    @Captor
    private ArgumentCaptor<String> updatedCourtApplicationStr;

    @Mock
    private InitiateCourtApplicationEntity initiateCourtApplicationEntity;
    @Mock
    private CourtApplicationEntity courtApplicationEntity;

    @BeforeEach
    public void setup() {
        setField(this.jsonObjectToObjectConverter, "objectMapper", new ObjectMapperProducer().objectMapper());
        setField(this.objectToJsonObjectConverter, "mapper", new ObjectMapperProducer().objectMapper());
    }

    @Test
    public void addConvictionDate() throws Exception {

        final UUID prosecutionCaseId = randomUUID();
        final UUID offenceId = randomUUID();
        final LocalDate convictionDate = LocalDate.now();

        final ConvictionDateAdded convictionDateAdded = ConvictionDateAdded.convictionDateAdded()
                .withCaseId(prosecutionCaseId)
                .withOffenceId(offenceId)
                .withConvictionDate(convictionDate)
                .build();

        final List<JudicialResult> judicialResults = Collections.singletonList(JudicialResult.judicialResult().withApprovedDate(LocalDate.now()).build());

        final ProsecutionCase prosecutionCase = ProsecutionCase.prosecutionCase()
                .withId(prosecutionCaseId)
                .withDefendants(Collections.singletonList(Defendant.defendant()
                        .withOffences(Collections.singletonList(Offence.offence()
                                .withId(offenceId)
                                .withLaaApplnReference(LaaReference
                                        .laaReference()
                                        .withApplicationReference("ABC123")
                                        .withStatusCode("statusCode")
                                        .withStatusId(randomUUID())
                                        .withStatusDescription("description")
                                        .build()).withJudicialResults(judicialResults)
                                .build()))
                        .build()))
                .build();

        ProsecutionCaseEntity prosecutionCaseEntity = new ProsecutionCaseEntity();
        prosecutionCaseEntity.setCaseId(prosecutionCaseId);
        prosecutionCaseEntity.setPayload(objectToJsonObjectConverter.convert(prosecutionCase).toString());

        when(prosecutionCaseRepository.findByCaseId(prosecutionCaseId)).thenReturn(prosecutionCaseEntity);

        convictionDateEventListener.addConvictionDate(envelopeFrom(metadataWithRandomUUID("progression.event.conviction-date-added"),
                objectToJsonObjectConverter.convert(convictionDateAdded)));

        final ArgumentCaptor<ProsecutionCaseEntity> prosecutionCaseArgumentCaptor = ArgumentCaptor.forClass(ProsecutionCaseEntity.class);

        verify(this.prosecutionCaseRepository).save(prosecutionCaseArgumentCaptor.capture());

        final ProsecutionCase prosecutionCaseResponse = jsonObjectToObjectConverter.convert(stringToJsonObjectConverter.convert(prosecutionCaseArgumentCaptor.getValue().getPayload()), ProsecutionCase.class);

        assertThat(prosecutionCaseArgumentCaptor.getValue().getCaseId(), is(prosecutionCaseId));
        assertThat(prosecutionCaseResponse.getDefendants().get(0).getOffences().get(0).getId(), is(offenceId));
        assertThat(prosecutionCaseResponse.getDefendants().get(0).getOffences().get(0).getConvictionDate(), is(convictionDate));
        assertThat(prosecutionCaseResponse.getDefendants().get(0).getOffences().get(0).getLaaApplnReference().getApplicationReference(), is("ABC123"));
        assertThat(prosecutionCaseResponse.getDefendants().get(0).getOffences().get(0).getLaaApplnReference().getStatusCode(), is("statusCode"));
        assertThat(prosecutionCaseResponse.getDefendants().get(0).getOffences().get(0).getLaaApplnReference().getStatusDescription(), is("description"));
        assertThat(prosecutionCaseResponse.getDefendants().get(0).getOffences().get(0).getJudicialResults().size(), is(1));

    }

    @Test
    public void addConvictionDateToOffenceUnderCourtAplicationCase() throws Exception {

        final UUID courtApplicationId = randomUUID();
        final UUID offenceId = randomUUID();
        final LocalDate convictionDate = LocalDate.now();

        final ConvictionDateAdded convictionDateAdded = ConvictionDateAdded.convictionDateAdded()
                .withCourtApplicationId(courtApplicationId)
                .withOffenceId(offenceId)
                .withConvictionDate(convictionDate)
                .build();

        final CourtApplication courtApplication = TestHelper.buildCourtapplicationWithOffenceUnderCase(courtApplicationId, offenceId, null);

        convictionDateTest(courtApplicationId, courtApplication, convictionDateAdded, null);

        final InitiateCourtApplicationProceedings updatedCourtApplicationProceedings = jsonObjectToObjectConverter.convert(stringToJsonObjectConverter.convert(updatedStr.getValue()), InitiateCourtApplicationProceedings.class);
        final CourtApplication updatedCourtApplication = jsonObjectToObjectConverter.convert(stringToJsonObjectConverter.convert(updatedCourtApplicationStr.getValue()), CourtApplication.class);

        assertThat(updatedCourtApplicationProceedings.getCourtApplication().getId(), is(courtApplicationId));
        assertThat(updatedCourtApplicationProceedings.getCourtApplication().getCourtApplicationCases().get(0).getIsSJP(), is(false));
        assertThat(updatedCourtApplicationProceedings.getCourtApplication().getCourtApplicationCases().get(0).getOffences().get(0).getId(), is(offenceId));
        assertThat(updatedCourtApplicationProceedings.getCourtApplication().getCourtApplicationCases().get(0).getOffences().get(0).getConvictionDate(), is(convictionDate));

        assertThat(updatedCourtApplication.getId(), is(courtApplicationId));
        assertThat(updatedCourtApplication.getCourtApplicationCases().get(0).getIsSJP(), is(false));
        assertThat(updatedCourtApplication.getCourtApplicationCases().get(0).getOffences().get(0).getId(), is(offenceId));
        assertThat(updatedCourtApplication.getCourtApplicationCases().get(0).getOffences().get(0).getConvictionDate(), is(convictionDate));

    }

    @Test
    public void removeConvictionDateToOffenceUnderCourtAplicationCase() throws Exception {

        final UUID courtApplicationId = randomUUID();
        final UUID offenceId = randomUUID();
        final LocalDate convictionDate = LocalDate.now();

        final ConvictionDateRemoved convictionDateRemoved = ConvictionDateRemoved.convictionDateRemoved()
                .withCourtApplicationId(courtApplicationId)
                .withOffenceId(offenceId)
                .build();

        final CourtApplication courtApplication = TestHelper.buildCourtapplicationWithOffenceUnderCase(courtApplicationId, offenceId, convictionDate);


        convictionDateTest(courtApplicationId, courtApplication, null , convictionDateRemoved);

        final InitiateCourtApplicationProceedings updatedCourtApplicationProceedings = jsonObjectToObjectConverter.convert(stringToJsonObjectConverter.convert(updatedStr.getValue()), InitiateCourtApplicationProceedings.class);
        final CourtApplication updatedCourtApplication = jsonObjectToObjectConverter.convert(stringToJsonObjectConverter.convert(updatedCourtApplicationStr.getValue()), CourtApplication.class);

        assertThat(updatedCourtApplicationProceedings.getCourtApplication().getId(), is(courtApplicationId));
        assertThat(updatedCourtApplicationProceedings.getCourtApplication().getCourtApplicationCases().get(0).getIsSJP(), is(false));
        assertThat(updatedCourtApplicationProceedings.getCourtApplication().getCourtApplicationCases().get(0).getOffences().get(0).getId(), is(offenceId));
        assertNull(updatedCourtApplicationProceedings.getCourtApplication().getCourtApplicationCases().get(0).getOffences().get(0).getConvictionDate() );

        assertThat(updatedCourtApplication.getId(), is(courtApplicationId));
        assertThat(updatedCourtApplication.getCourtApplicationCases().get(0).getIsSJP(), is(false));
        assertThat(updatedCourtApplication.getCourtApplicationCases().get(0).getOffences().get(0).getId(), is(offenceId));
        assertNull(updatedCourtApplication.getCourtApplicationCases().get(0).getOffences().get(0).getConvictionDate());

    }

    @Test
    public void addConvictionDateToOffenceUnderCourtaplicationCourtOrder() throws Exception {

        final UUID courtApplicationId = randomUUID();
        final UUID offenceId = randomUUID();
        final LocalDate convictionDate = LocalDate.now();

        final ConvictionDateAdded convictionDateAdded = ConvictionDateAdded.convictionDateAdded()
                .withCourtApplicationId(courtApplicationId)
                .withOffenceId(offenceId)
                .withConvictionDate(convictionDate)
                .build();

        final CourtApplication courtApplication = TestHelper.buildCourtapplicationWithOffenceUnderCourtOrder(courtApplicationId, offenceId, null);

        convictionDateTest(courtApplicationId, courtApplication, convictionDateAdded, null);

        final InitiateCourtApplicationProceedings updatedCourtApplicationProceedings = jsonObjectToObjectConverter.convert(stringToJsonObjectConverter.convert(updatedStr.getValue()), InitiateCourtApplicationProceedings.class);
        final CourtApplication updatedCourtApplication = jsonObjectToObjectConverter.convert(stringToJsonObjectConverter.convert(updatedCourtApplicationStr.getValue()), CourtApplication.class);

        assertThat(updatedCourtApplicationProceedings.getCourtApplication().getId(), is(courtApplicationId));
        assertThat(updatedCourtApplicationProceedings.getCourtApplication().getCourtOrder().getCourtOrderOffences().get(0).getOffence().getId(), is(offenceId));
        assertThat(updatedCourtApplicationProceedings.getCourtApplication().getCourtOrder().getCourtOrderOffences().get(0).getOffence().getConvictionDate(), is(convictionDate));

        assertThat(updatedCourtApplication.getId(), is(courtApplicationId));
        assertThat(updatedCourtApplication.getCourtOrder().getCourtOrderOffences().get(0).getOffence().getId(), is(offenceId));
        assertThat(updatedCourtApplication.getCourtOrder().getCourtOrderOffences().get(0).getOffence().getConvictionDate(), is(convictionDate));


    }

    @Test
    public void removeConvictionDateToOffenceUnderCourtaplicationCourtOrder() throws Exception {

        final UUID courtApplicationId = randomUUID();
        final UUID offenceId = randomUUID();
        final LocalDate convictionDate = LocalDate.now();

        final ConvictionDateRemoved convictionDateRemoved = ConvictionDateRemoved.convictionDateRemoved()
                .withCourtApplicationId(courtApplicationId)
                .withOffenceId(offenceId)
                .build();

        final CourtApplication courtApplication = TestHelper.buildCourtapplicationWithOffenceUnderCourtOrder(courtApplicationId, offenceId, convictionDate);

        convictionDateTest(courtApplicationId, courtApplication, null, convictionDateRemoved);

        final InitiateCourtApplicationProceedings updatedCourtApplicationProceedings = jsonObjectToObjectConverter.convert(stringToJsonObjectConverter.convert(updatedStr.getValue()), InitiateCourtApplicationProceedings.class);
        final CourtApplication updatedCourtApplication = jsonObjectToObjectConverter.convert(stringToJsonObjectConverter.convert(updatedCourtApplicationStr.getValue()), CourtApplication.class);

        assertThat(updatedCourtApplicationProceedings.getCourtApplication().getId(), is(courtApplicationId));
        assertThat(updatedCourtApplicationProceedings.getCourtApplication().getCourtOrder().getCourtOrderOffences().get(0).getOffence().getId(), is(offenceId));
        assertNull(updatedCourtApplicationProceedings.getCourtApplication().getCourtOrder().getCourtOrderOffences().get(0).getOffence().getConvictionDate());

        assertThat(updatedCourtApplication.getId(), is(courtApplicationId));
        assertThat(updatedCourtApplication.getCourtOrder().getCourtOrderOffences().get(0).getOffence().getId(), is(offenceId));
        assertNull(updatedCourtApplication.getCourtOrder().getCourtOrderOffences().get(0).getOffence().getConvictionDate());


    }

    @Test
    public void addConvictionDateToCourtaplication() throws Exception {

        final UUID courtApplicationId = randomUUID();
        final LocalDate convictionDate = LocalDate.now();

        final ConvictionDateAdded convictionDateAdded = ConvictionDateAdded.convictionDateAdded()
                .withCourtApplicationId(courtApplicationId)
                .withConvictionDate(convictionDate)
                .build();

        final CourtApplication courtApplication = TestHelper.buildCourtapplication(courtApplicationId, null);

        convictionDateTest(courtApplicationId, courtApplication, convictionDateAdded, null);

        final InitiateCourtApplicationProceedings updatedCourtApplicationProceedings = jsonObjectToObjectConverter.convert(stringToJsonObjectConverter.convert(updatedStr.getValue()), InitiateCourtApplicationProceedings.class);
        final CourtApplication updatedCourtApplication = jsonObjectToObjectConverter.convert(stringToJsonObjectConverter.convert(updatedCourtApplicationStr.getValue()), CourtApplication.class);

        assertThat(updatedCourtApplicationProceedings.getCourtApplication().getId(), is(courtApplicationId));
        assertThat(updatedCourtApplicationProceedings.getCourtApplication().getConvictionDate(), is(convictionDate));

        assertThat(updatedCourtApplication.getId(), is(courtApplicationId));
        assertThat(updatedCourtApplication.getConvictionDate(), is(convictionDate));


    }

    @Test
    public void RemovedConvictionDateToCourtaplication() throws Exception {

        final UUID courtApplicationId = randomUUID();
        final LocalDate convictionDate = LocalDate.now();

        final ConvictionDateRemoved convictionDateRemoved = ConvictionDateRemoved.convictionDateRemoved()
                .withCourtApplicationId(courtApplicationId)
                .build();

        final CourtApplication courtApplication = TestHelper.buildCourtapplication(courtApplicationId, convictionDate);

        convictionDateTest(courtApplicationId, courtApplication, null, convictionDateRemoved);

        final InitiateCourtApplicationProceedings updatedCourtApplicationProceedings = jsonObjectToObjectConverter.convert(stringToJsonObjectConverter.convert(updatedStr.getValue()), InitiateCourtApplicationProceedings.class);
        final CourtApplication updatedCourtApplication = jsonObjectToObjectConverter.convert(stringToJsonObjectConverter.convert(updatedCourtApplicationStr.getValue()), CourtApplication.class);

        assertThat(updatedCourtApplicationProceedings.getCourtApplication().getId(), is(courtApplicationId));
        assertNull(updatedCourtApplicationProceedings.getCourtApplication().getConvictionDate());

        assertThat(updatedCourtApplication.getId(), is(courtApplicationId));
        assertNull(updatedCourtApplication.getConvictionDate());


    }

    @Test
    public void removeConvictionDate() throws Exception {

        final UUID prosecutionCaseId = randomUUID();
        final UUID offenceId = randomUUID();

        final ConvictionDateRemoved convictionDateRemoved = ConvictionDateRemoved.convictionDateRemoved()
                .withCaseId(prosecutionCaseId)
                .withOffenceId(offenceId)
                .build();

        final ProsecutionCase prosecutionCase = ProsecutionCase.prosecutionCase()
                .withId(prosecutionCaseId)
                .withDefendants(Arrays.asList(Defendant.defendant()
                        .withOffences(Arrays.asList(Offence.offence()
                                .withId(offenceId)
                                .withConvictionDate(LocalDate.now())
                                .build()))
                        .build()))
                .build();

        ProsecutionCaseEntity prosecutionCaseEntity = new ProsecutionCaseEntity();
        prosecutionCaseEntity.setCaseId(prosecutionCaseId);
        prosecutionCaseEntity.setPayload(objectToJsonObjectConverter.convert(prosecutionCase).toString());

        when(prosecutionCaseRepository.findByCaseId(prosecutionCaseId)).thenReturn(prosecutionCaseEntity);

        convictionDateEventListener.removeConvictionDate(envelopeFrom(metadataWithRandomUUID("progression.event.conviction-date-removed"),
                objectToJsonObjectConverter.convert(convictionDateRemoved)));

        final ArgumentCaptor<ProsecutionCaseEntity> prosecutionCaseArgumentCaptor = ArgumentCaptor.forClass(ProsecutionCaseEntity.class);

        verify(this.prosecutionCaseRepository).save(prosecutionCaseArgumentCaptor.capture());

        final ProsecutionCase prosecutionCaseResponse = jsonObjectToObjectConverter.convert(stringToJsonObjectConverter.convert(prosecutionCaseArgumentCaptor.getValue().getPayload()), ProsecutionCase.class);

        assertThat(prosecutionCaseArgumentCaptor.getValue().getCaseId(), is(prosecutionCaseId));
        assertThat(prosecutionCaseResponse.getDefendants().get(0).getOffences().get(0).getId(), is(offenceId));
        assertNull(prosecutionCaseResponse.getDefendants().get(0).getOffences().get(0).getConvictionDate());

    }


    private void convictionDateTest(final UUID courtApplicationId, final CourtApplication courtApplication, final ConvictionDateAdded convictionDateAdded, final ConvictionDateRemoved convictionDateRemoved) {
        final InitiateCourtApplicationProceedings initiateCourtApplicationProceedings = InitiateCourtApplicationProceedings.initiateCourtApplicationProceedings()
                .withCourtApplication(courtApplication)
                .build();

        final String str = objectToJsonObjectConverter.convert(initiateCourtApplicationProceedings).toString();
        when(initiateCourtApplicationEntity.getPayload()).thenReturn(str);
        when(initiateCourtApplicationRepository.findBy(courtApplicationId)).thenReturn(initiateCourtApplicationEntity);
        when(courtApplicationRepository.findByApplicationId(courtApplicationId)).thenReturn(courtApplicationEntity);

        if(convictionDateAdded != null) {
            convictionDateEventListener.addConvictionDate(envelopeFrom(metadataWithRandomUUID("progression.event.conviction-date-added"),
                    objectToJsonObjectConverter.convert(convictionDateAdded)));
        }else{
            convictionDateEventListener.removeConvictionDate(envelopeFrom(metadataWithRandomUUID("progression.event.conviction-date-removed"),
                    objectToJsonObjectConverter.convert(convictionDateRemoved)));
        }

        verify(initiateCourtApplicationEntity).setPayload(updatedStr.capture());
        verify(initiateCourtApplicationRepository).save(initiateCourtApplicationEntity);
        verify(courtApplicationEntity).setPayload(updatedCourtApplicationStr.capture());
        verify(courtApplicationRepository).save(courtApplicationEntity);
    }
}
