package uk.gov.moj.cpp.application.event.listener;

import static java.util.UUID.randomUUID;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.justice.core.courts.ApplicationDefenceOrganisationChanged.applicationDefenceOrganisationChanged;

import uk.gov.justice.core.courts.ApplicationDefenceOrganisationChanged;
import uk.gov.justice.core.courts.AssociatedDefenceOrganisation;
import uk.gov.justice.core.courts.CourtApplication;
import uk.gov.justice.core.courts.CourtApplicationParty;
import uk.gov.justice.core.courts.DefenceOrganisation;
import uk.gov.justice.core.courts.FundingType;
import uk.gov.justice.core.courts.InitiateCourtApplicationProceedings;
import uk.gov.justice.core.courts.Organisation;
import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.common.converter.ObjectToJsonObjectConverter;
import uk.gov.justice.services.common.converter.StringToJsonObjectConverter;
import uk.gov.justice.services.common.converter.jackson.ObjectMapperProducer;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.prosecutioncase.persistence.entity.CourtApplicationEntity;
import uk.gov.moj.cpp.prosecutioncase.persistence.entity.InitiateCourtApplicationEntity;
import uk.gov.moj.cpp.prosecutioncase.persistence.repository.CourtApplicationRepository;
import uk.gov.moj.cpp.prosecutioncase.persistence.repository.InitiateCourtApplicationRepository;

import java.time.LocalDate;
import java.util.UUID;

import javax.json.JsonObject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class ApplicationDefenceOrganisationChangedListenerTest {

    @InjectMocks
    private ApplicationDefenceOrganisationChangedEventListener eventListener;

    @Mock
    private JsonEnvelope envelope;

    @Mock
    private CourtApplicationRepository courtApplicationRepository;

    @Mock
    private InitiateCourtApplicationRepository initiateCourtApplicationRepository;

    @Spy
    private JsonObjectToObjectConverter jsonObjectToObjectConverter = new JsonObjectToObjectConverter(new ObjectMapperProducer().objectMapper());

    @Spy
    private ObjectToJsonObjectConverter objectToJsonObjectConverter = new ObjectToJsonObjectConverter(new ObjectMapperProducer().objectMapper());

    @Spy
    private StringToJsonObjectConverter stringToJsonObjectConverter = new StringToJsonObjectConverter();

    @Captor
    private ArgumentCaptor<CourtApplicationEntity> courtApplicationEntityArgumentCaptor;

    @Captor
    private ArgumentCaptor<InitiateCourtApplicationEntity> initiateCourtApplicationEntityArgumentCaptor;

    @Test
    void shouldHandleApplicationDefenceOrganisationChangedAndPersistIncomingAssociatedDefenceOrganisation() {

        final UUID applicationId = randomUUID();
        final UUID subjectId = randomUUID();
        final ApplicationDefenceOrganisationChanged applicationDefenceOrganisationChanged = applicationDefenceOrganisationChanged()
                .withApplicationId(applicationId)
                .withSubjectId(subjectId)
                .withAssociatedDefenceOrganisation(AssociatedDefenceOrganisation.associatedDefenceOrganisation()
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
                        .build())
                .build();

        //associatedDefenceOrganisation is null in view store
        final CourtApplication applicationReturnedFromEntity = getCourtApplication(applicationId, subjectId, null);

        final InitiateCourtApplicationProceedings initiateCourtApplicationProceedingsFromEntity = InitiateCourtApplicationProceedings.initiateCourtApplicationProceedings()
                .withCourtApplication(applicationReturnedFromEntity)
                .build();

        final InitiateCourtApplicationEntity initiateApplicationEntity = new InitiateCourtApplicationEntity();
        initiateApplicationEntity.setApplicationId(applicationId);
        initiateApplicationEntity.setPayload(objectToJsonObjectConverter.convert(initiateCourtApplicationProceedingsFromEntity).toString());

        final CourtApplicationEntity applicationEntity = new CourtApplicationEntity();
        applicationEntity.setApplicationId(applicationId);
        applicationEntity.setPayload(objectToJsonObjectConverter.convert(applicationReturnedFromEntity).toString());

        final JsonObject envelopeBody = objectToJsonObjectConverter.convert(applicationDefenceOrganisationChanged);

        when(envelope.payloadAsJsonObject()).thenReturn(envelopeBody);
        when(courtApplicationRepository.findByApplicationId(applicationId)).thenReturn(applicationEntity);
        when(initiateCourtApplicationRepository.findBy(applicationId)).thenReturn(initiateApplicationEntity);


        eventListener.processApplicationDefenceOrganisationChanged(envelope);

        verify(courtApplicationRepository).save(courtApplicationEntityArgumentCaptor.capture());
        final CourtApplicationEntity courtApplicationEntity = courtApplicationEntityArgumentCaptor.getValue();
        final JsonObject courtApplicationJson = stringToJsonObjectConverter.convert(courtApplicationEntity.getPayload());
        final CourtApplication savedCourtApplication = jsonObjectToObjectConverter.convert(courtApplicationJson, CourtApplication.class);

        assertThat(savedCourtApplication.getSubject().getAssociatedDefenceOrganisation().getDefenceOrganisation().getLaaContractNumber(), equalTo("LAA123"));
        assertThat(savedCourtApplication.getSubject().getAssociatedDefenceOrganisation().getApplicationReference(), equalTo("ABC1234"));
        assertThat(savedCourtApplication.getSubject().getAssociatedDefenceOrganisation().getDefenceOrganisation().getOrganisation().getName(), equalTo("Org1"));
        assertThat(savedCourtApplication.getSubject().getAssociatedDefenceOrganisation().getFundingType(), equalTo(FundingType.REPRESENTATION_ORDER));

        verify(initiateCourtApplicationRepository).save(initiateCourtApplicationEntityArgumentCaptor.capture());
        final InitiateCourtApplicationEntity savedInitiateCourtApplication = initiateCourtApplicationEntityArgumentCaptor.getValue();

        final JsonObject applicationJson = stringToJsonObjectConverter.convert(savedInitiateCourtApplication.getPayload());
        final InitiateCourtApplicationProceedings savedInitiateCourtApplicationProceedings = jsonObjectToObjectConverter.convert(applicationJson, InitiateCourtApplicationProceedings.class);

        assertThat(savedInitiateCourtApplicationProceedings.getCourtApplication().getSubject().getAssociatedDefenceOrganisation().getDefenceOrganisation().getLaaContractNumber(), equalTo("LAA123"));
        assertThat(savedInitiateCourtApplicationProceedings.getCourtApplication().getSubject().getAssociatedDefenceOrganisation().getApplicationReference(), equalTo("ABC1234"));
        assertThat(savedInitiateCourtApplicationProceedings.getCourtApplication().getSubject().getAssociatedDefenceOrganisation().getDefenceOrganisation().getOrganisation().getName(), equalTo("Org1"));
        assertThat(savedInitiateCourtApplicationProceedings.getCourtApplication().getSubject().getAssociatedDefenceOrganisation().getFundingType(), equalTo(FundingType.REPRESENTATION_ORDER));

    }

    @Test
    void shouldHandleApplicationDefenceOrganisationChangedAndRemoveAssociatedDefenceOrganisationWhenIncomingValueIsNull() {

        final UUID applicationId = randomUUID();
        final UUID subjectId = randomUUID();
        final ApplicationDefenceOrganisationChanged applicationDefenceOrganisationChanged = applicationDefenceOrganisationChanged()
                .withApplicationId(applicationId)
                .withSubjectId(subjectId)
                .withAssociatedDefenceOrganisation(null)//associatedDefenceOrganisation is null in incoming event
                .build();

        final AssociatedDefenceOrganisation associatedDefenceOrganisationInViewStore = AssociatedDefenceOrganisation.associatedDefenceOrganisation()
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

        //associatedDefenceOrganisation is not null in view store
        final CourtApplication applicationReturnedFromEntity = getCourtApplication(applicationId, subjectId, associatedDefenceOrganisationInViewStore);

        final InitiateCourtApplicationProceedings initiateCourtApplicationProceedingsFromEntity = InitiateCourtApplicationProceedings.initiateCourtApplicationProceedings()
                .withCourtApplication(applicationReturnedFromEntity)
                .build();

        final InitiateCourtApplicationEntity initiateApplicationEntity = new InitiateCourtApplicationEntity();
        initiateApplicationEntity.setApplicationId(applicationId);
        initiateApplicationEntity.setPayload(objectToJsonObjectConverter.convert(initiateCourtApplicationProceedingsFromEntity).toString());

        final CourtApplicationEntity applicationEntity = new CourtApplicationEntity();
        applicationEntity.setApplicationId(applicationId);
        applicationEntity.setPayload(objectToJsonObjectConverter.convert(applicationReturnedFromEntity).toString());

        final JsonObject envelopeBody = objectToJsonObjectConverter.convert(applicationDefenceOrganisationChanged);

        when(envelope.payloadAsJsonObject()).thenReturn(envelopeBody);
        when(courtApplicationRepository.findByApplicationId(applicationId)).thenReturn(applicationEntity);
        when(initiateCourtApplicationRepository.findBy(applicationId)).thenReturn(initiateApplicationEntity);

        eventListener.processApplicationDefenceOrganisationChanged(envelope);

        verify(courtApplicationRepository).save(courtApplicationEntityArgumentCaptor.capture());
        final CourtApplicationEntity courtApplicationEntity = courtApplicationEntityArgumentCaptor.getValue();
        final JsonObject courtApplicationJson = stringToJsonObjectConverter.convert(courtApplicationEntity.getPayload());
        final CourtApplication savedCourtApplication = jsonObjectToObjectConverter.convert(courtApplicationJson, CourtApplication.class);

        assertThat(savedCourtApplication.getSubject().getAssociatedDefenceOrganisation(), nullValue());

        verify(initiateCourtApplicationRepository).save(initiateCourtApplicationEntityArgumentCaptor.capture());
        final InitiateCourtApplicationEntity savedInitiateCourtApplication = initiateCourtApplicationEntityArgumentCaptor.getValue();

        final JsonObject applicationJson = stringToJsonObjectConverter.convert(savedInitiateCourtApplication.getPayload());
        final InitiateCourtApplicationProceedings savedInitiateCourtApplicationProceedings = jsonObjectToObjectConverter.convert(applicationJson, InitiateCourtApplicationProceedings.class);

        assertThat(savedInitiateCourtApplicationProceedings.getCourtApplication().getSubject().getAssociatedDefenceOrganisation(), nullValue());
    }

    private CourtApplication getCourtApplication(final UUID applicationId, final UUID subjectId, final AssociatedDefenceOrganisation associatedDefenceOrganisation) {
        return CourtApplication.courtApplication()
                .withId(applicationId)
                .withSubject(CourtApplicationParty.courtApplicationParty()
                        .withId(subjectId)
                        .withAssociatedDefenceOrganisation(associatedDefenceOrganisation)
                        .build())
                .build();
    }
}
