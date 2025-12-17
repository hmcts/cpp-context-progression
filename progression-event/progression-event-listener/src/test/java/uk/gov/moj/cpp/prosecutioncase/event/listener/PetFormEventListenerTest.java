package uk.gov.moj.cpp.prosecutioncase.event.listener;

import static java.util.Arrays.asList;
import static java.util.UUID.randomUUID;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.justice.services.test.utils.core.messaging.MetadataBuilderFactory.metadataWithRandomUUID;
import static uk.gov.justice.services.test.utils.core.reflection.ReflectionUtil.setField;

import uk.gov.justice.core.courts.PetDefendants;
import uk.gov.justice.core.courts.PetDetailReceived;
import uk.gov.justice.core.courts.PetDetailUpdated;
import uk.gov.justice.core.courts.PetFormCreated;
import uk.gov.justice.core.courts.PetFormFinalised;
import uk.gov.justice.core.courts.PetFormReceived;
import uk.gov.justice.core.courts.PetFormUpdated;
import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.common.converter.ObjectToJsonObjectConverter;
import uk.gov.justice.services.common.converter.jackson.ObjectMapperProducer;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.prosecutioncase.persistence.entity.PetCaseDefendantOffence;
import uk.gov.moj.cpp.prosecutioncase.persistence.repository.PetCaseDefendantOffenceRepository;

import java.util.ArrayList;
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
public class PetFormEventListenerTest {

    @Spy
    private JsonObjectToObjectConverter jsonObjectToObjectConverter;

    @Spy
    private ObjectToJsonObjectConverter objectToJsonObjectConverter;

    @Mock
    private PetCaseDefendantOffenceRepository petCaseDefendantOffenceRepository;

    @Captor
    private ArgumentCaptor<PetCaseDefendantOffence> petCaseDefendantOffenceCaptor;

    @InjectMocks
    private PetFormEventListener petFormEventListener;

    @BeforeEach
    public void setup() {
        setField(this.jsonObjectToObjectConverter, "objectMapper", new ObjectMapperProducer().objectMapper());
        setField(this.objectToJsonObjectConverter, "mapper", new ObjectMapperProducer().objectMapper());
    }

    @Test
    public void shouldCreatePetForm() {

        final PetFormCreated event = PetFormCreated.petFormCreated()
                .withPetId(randomUUID())
                .withCaseId(randomUUID())
                .withPetDefendants(asList(PetDefendants.petDefendants()
                        .withDefendantId(randomUUID())
                        .build()))
                .withFormId(randomUUID())
                .withPetFormData("Test Data")
                .withUserId(randomUUID())
                .build();

        JsonEnvelope jsonEnvelope = envelopeFrom(metadataWithRandomUUID("progression.event.pet-form-created"),
                objectToJsonObjectConverter.convert(event));

        petFormEventListener.petFormCreated(jsonEnvelope);

        verify(this.petCaseDefendantOffenceRepository, times(1)).save(any());
    }

    @Test
    public void shouldNotSetYouthTrueWhenNotPresentInPetForm() {

        final PetFormCreated event = PetFormCreated.petFormCreated()
                .withPetId(randomUUID())
                .withCaseId(randomUUID())
                .withPetDefendants(asList(PetDefendants.petDefendants()
                        .withDefendantId(randomUUID())
                        .build()))
                .withFormId(randomUUID())
                .withPetFormData("Test Data")
                .withUserId(randomUUID())
                .build();

        JsonEnvelope jsonEnvelope = envelopeFrom(metadataWithRandomUUID("progression.event.pet-form-created"),
                objectToJsonObjectConverter.convert(event));

        petFormEventListener.petFormCreated(jsonEnvelope);

        verify(this.petCaseDefendantOffenceRepository).save(petCaseDefendantOffenceCaptor.capture());
        PetCaseDefendantOffence actualPetCaseDefendentOffence = petCaseDefendantOffenceCaptor.getValue();
        assertThat(actualPetCaseDefendentOffence.getIsYouth(), is(false));
    }

    @Test
    public void shouldSetYouthFalseWhenSetToFalseInPetForm() {

        final PetFormCreated event = PetFormCreated.petFormCreated()
                .withPetId(randomUUID())
                .withCaseId(randomUUID())
                .withIsYouth(Boolean.FALSE)
                .withPetDefendants(asList(PetDefendants.petDefendants()
                        .withDefendantId(randomUUID())
                        .build()))
                .withFormId(randomUUID())
                .withPetFormData("Test Data")
                .withUserId(randomUUID())
                .build();

        JsonEnvelope jsonEnvelope = envelopeFrom(metadataWithRandomUUID("progression.event.pet-form-created"),
                objectToJsonObjectConverter.convert(event));

        petFormEventListener.petFormCreated(jsonEnvelope);

        verify(this.petCaseDefendantOffenceRepository).save(petCaseDefendantOffenceCaptor.capture());
        PetCaseDefendantOffence actualPetCaseDefendentOffence = petCaseDefendantOffenceCaptor.getValue();
        assertThat(actualPetCaseDefendentOffence.getIsYouth(), is(false));
    }

    @Test
    public void shouldSetYouthTrueWhenSetToTrueInPetForm() {

        final PetFormCreated event = PetFormCreated.petFormCreated()
                .withPetId(randomUUID())
                .withCaseId(randomUUID())
                .withIsYouth(Boolean.TRUE)
                .withPetDefendants(asList(PetDefendants.petDefendants()
                        .withDefendantId(randomUUID())
                        .build()))
                .withFormId(randomUUID())
                .withPetFormData("Test Data")
                .withUserId(randomUUID())
                .build();

        JsonEnvelope jsonEnvelope = envelopeFrom(metadataWithRandomUUID("progression.event.pet-form-created"),
                objectToJsonObjectConverter.convert(event));

        petFormEventListener.petFormCreated(jsonEnvelope);

        verify(this.petCaseDefendantOffenceRepository).save(petCaseDefendantOffenceCaptor.capture());
        PetCaseDefendantOffence actualPetCaseDefendentOffence = petCaseDefendantOffenceCaptor.getValue();
        assertThat(actualPetCaseDefendentOffence.getIsYouth(), is(true));
    }

    @Test
    public void shouldReceivePetForm() {

        final PetFormReceived event = PetFormReceived.petFormReceived()
                .withPetId(randomUUID())
                .withCaseId(randomUUID())
                .withPetDefendants(asList(PetDefendants.petDefendants()
                        .withDefendantId(randomUUID())

                        .build()))
                .withFormId(randomUUID())
                .build();

        JsonEnvelope jsonEnvelope = envelopeFrom(metadataWithRandomUUID("progression.event.pet-form-received"),
                objectToJsonObjectConverter.convert(event));

        petFormEventListener.petFormReceived(jsonEnvelope);

        verify(this.petCaseDefendantOffenceRepository, times(1)).save(any());
    }

    @Test
    public void shouldUpdatePetDetails() {

        final PetDetailUpdated event = PetDetailUpdated.petDetailUpdated()
                .withPetId(randomUUID())
                .withCaseId(randomUUID())
                .withPetDefendants(asList(PetDefendants.petDefendants()
                        .withDefendantId(randomUUID())
                        .build()))
                .build();

        JsonEnvelope jsonEnvelope = envelopeFrom(metadataWithRandomUUID("progression.event.pet-detail-updated"),
                objectToJsonObjectConverter.convert(event));

        petFormEventListener.petDetailsUpdated(jsonEnvelope);

        verify(this.petCaseDefendantOffenceRepository, times(1)).save(any());
        verify(this.petCaseDefendantOffenceRepository).save(petCaseDefendantOffenceCaptor.capture());
        PetCaseDefendantOffence actualPetCaseDefendentOffence = petCaseDefendantOffenceCaptor.getValue();
        assertThat(actualPetCaseDefendentOffence.getIsYouth(), is(false));
    }

    @Test
    public void shouldSetYouthTrueWhenSetToTrueInUpdatePetDetails() {

        final PetDetailUpdated event = PetDetailUpdated.petDetailUpdated()
                .withPetId(randomUUID())
                .withCaseId(randomUUID())
                .withIsYouth(Boolean.TRUE)
                .withPetDefendants(asList(PetDefendants.petDefendants()
                        .withDefendantId(randomUUID())
                        .build()))
                .build();

        JsonEnvelope jsonEnvelope = envelopeFrom(metadataWithRandomUUID("progression.event.pet-detail-updated"),
                objectToJsonObjectConverter.convert(event));

        petFormEventListener.petDetailsUpdated(jsonEnvelope);

        verify(this.petCaseDefendantOffenceRepository).save(petCaseDefendantOffenceCaptor.capture());
        PetCaseDefendantOffence actualPetCaseDefendentOffence = petCaseDefendantOffenceCaptor.getValue();
        assertThat(actualPetCaseDefendentOffence.getIsYouth(), is(true));
    }

    @Test
    public void shouldSetYouthFalseWhenSetToFalseInUpdatePetDetails() {

        final PetDetailUpdated event = PetDetailUpdated.petDetailUpdated()
                .withPetId(randomUUID())
                .withCaseId(randomUUID())
                .withIsYouth(Boolean.FALSE)
                .withPetDefendants(asList(PetDefendants.petDefendants()
                        .withDefendantId(randomUUID())
                        .build()))
                .build();

        JsonEnvelope jsonEnvelope = envelopeFrom(metadataWithRandomUUID("progression.event.pet-detail-updated"),
                objectToJsonObjectConverter.convert(event));

        petFormEventListener.petDetailsUpdated(jsonEnvelope);

        verify(this.petCaseDefendantOffenceRepository).save(petCaseDefendantOffenceCaptor.capture());
        PetCaseDefendantOffence actualPetCaseDefendentOffence = petCaseDefendantOffenceCaptor.getValue();
        assertThat(actualPetCaseDefendentOffence.getIsYouth(), is(false));
    }

    @Test
    public void shouldUpdatePetDetailsRecevied() {

        final PetDetailReceived event = PetDetailReceived.petDetailReceived()
                .withPetId(randomUUID())
                .withCaseId(randomUUID())
                .withPetDefendants(asList(PetDefendants.petDefendants()
                        .withDefendantId(randomUUID())
                        .build()))
                .build();

        JsonEnvelope jsonEnvelope = envelopeFrom(metadataWithRandomUUID("progression.event.pet-detail-received"),
                objectToJsonObjectConverter.convert(event));

        petFormEventListener.petDetailReceived(jsonEnvelope);

        verify(this.petCaseDefendantOffenceRepository, times(1)).save(any());
    }

    @Test
    public void shouldUpdatePetForm() {
        final UUID caseId = randomUUID();
        final UUID petId = randomUUID();

        final PetFormUpdated petFormUpdated = PetFormUpdated.petFormUpdated()
                .withPetId(petId)
                .withCaseId(caseId)
                .withPetFormData("Test Data")
                .withUserId(randomUUID())
                .build();

        JsonEnvelope jsonEnvelope = envelopeFrom(metadataWithRandomUUID("progression.event.pet-form-updated"),
                objectToJsonObjectConverter.convert(petFormUpdated));
        final List<PetCaseDefendantOffence> petCaseDefendantOffences = new ArrayList<>();
        petCaseDefendantOffences.add(PetCaseDefendantOffence.builder()
                .withCaseId(caseId)
                .withPetId(petId)
                .build());

        when(petCaseDefendantOffenceRepository.findByPetId(petFormUpdated.getPetId())).thenReturn(petCaseDefendantOffences);

        petFormEventListener.petFormUpdated(jsonEnvelope);

        verify(this.petCaseDefendantOffenceRepository, times(1)).save(any());
    }

    @Test
    public void shouldFinalisePetForm() {
        final UUID caseId = randomUUID();
        final UUID petId = randomUUID();

        final PetFormFinalised petFormFinalised = PetFormFinalised.petFormFinalised()
                .withPetId(petId)
                .withCaseId(caseId)
                .withUserId(randomUUID())
                .build();

        JsonEnvelope jsonEnvelope = envelopeFrom(metadataWithRandomUUID("progression.event.pet-form-finalised"),
                objectToJsonObjectConverter.convert(petFormFinalised));
        final List<PetCaseDefendantOffence> petCaseDefendantOffences = new ArrayList<>();
        petCaseDefendantOffences.add(PetCaseDefendantOffence.builder()
                .withCaseId(caseId)
                .withPetId(petId)
                .build());

        when(petCaseDefendantOffenceRepository.findByPetId(petFormFinalised.getPetId())).thenReturn(petCaseDefendantOffences);

        petFormEventListener.petFormFinalised(jsonEnvelope);

        verify(this.petCaseDefendantOffenceRepository, times(1)).save(any());
    }
}