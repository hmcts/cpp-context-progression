package uk.gov.moj.cpp.prosecutioncase.event.listener;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.runners.MockitoJUnitRunner;
import uk.gov.justice.core.courts.PetDefendants;
import uk.gov.justice.core.courts.PetDetailReceived;
import uk.gov.justice.core.courts.PetDetailUpdated;
import uk.gov.justice.core.courts.PetFormCreated;
import uk.gov.justice.core.courts.PetFormReceived;
import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.common.converter.ObjectToJsonObjectConverter;
import uk.gov.justice.services.common.converter.jackson.ObjectMapperProducer;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.prosecutioncase.persistence.entity.PetCaseDefendantOffence;
import uk.gov.moj.cpp.prosecutioncase.persistence.repository.PetCaseDefendantOffenceRepository;

import static java.util.Arrays.asList;
import static java.util.UUID.randomUUID;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.justice.services.test.utils.core.messaging.MetadataBuilderFactory.metadataWithRandomUUID;
import static uk.gov.justice.services.test.utils.core.reflection.ReflectionUtil.setField;

@RunWith(MockitoJUnitRunner.class)
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

    @Before
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
}