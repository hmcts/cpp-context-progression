package uk.gov.moj.cpp.progression.query;

import static java.util.Arrays.asList;
import static java.util.UUID.randomUUID;
import static uk.gov.justice.services.messaging.JsonObjects.createArrayBuilder;
import static uk.gov.justice.services.messaging.JsonObjects.createObjectBuilder;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.test.utils.core.messaging.MetadataBuilderFactory.metadataWithDefaults;
import static uk.gov.justice.services.test.utils.core.reflection.ReflectionUtil.setField;

import uk.gov.justice.services.common.converter.ObjectToJsonObjectConverter;
import uk.gov.justice.services.common.converter.StringToJsonObjectConverter;
import uk.gov.justice.services.common.converter.jackson.ObjectMapperProducer;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.test.utils.core.enveloper.EnveloperFactory;
import uk.gov.moj.cpp.prosecutioncase.persistence.entity.PetCaseDefendantOffence;
import uk.gov.moj.cpp.prosecutioncase.persistence.repository.PetCaseDefendantOffenceRepository;

import java.util.UUID;

import javax.json.JsonObject;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class PetQueryViewTest {

    @InjectMocks
    private PetQueryView petQueryView;

    @Spy
    private final Enveloper enveloper = EnveloperFactory.createEnveloper();

    @Mock
    private PetCaseDefendantOffenceRepository petCaseDefendantOffenceRepository;

    @Spy
    private ObjectToJsonObjectConverter objectToJsonObjectConverter;

    @Spy
    private StringToJsonObjectConverter stringToJsonObjectConverter;

    @BeforeEach
    public void setUp() throws Exception {
        setField(this.objectToJsonObjectConverter, "mapper", new ObjectMapperProducer().objectMapper());
    }

    @Test
    public void shouldReturnPetsForCase() {
        final UUID petId1 = randomUUID();
        final UUID petId2 = randomUUID();
        final UUID formId1 = randomUUID();
        final UUID formId2 = randomUUID();
        final UUID caseId = randomUUID();
        final UUID defendantId1 = randomUUID();
        final UUID defendantId2 = randomUUID();
        final UUID petInternalId1 = randomUUID();
        final UUID petInternalId2 = randomUUID();
        final boolean isYouth = false;

        when(petCaseDefendantOffenceRepository.findByCaseId(caseId)).thenReturn(asList(
                new PetCaseDefendantOffence(petInternalId1, caseId, petId1, isYouth, defendantId1),
                new PetCaseDefendantOffence(petInternalId2, caseId, petId2, isYouth, defendantId2)
        ));


        final JsonEnvelope envelope = JsonEnvelope.envelopeFrom(metadataWithDefaults().withName("cpscasemanagement.query.pets-for-case"), createObjectBuilder().add("caseId", caseId.toString()).build());
        final JsonEnvelope result = petQueryView.getPetsForCase(envelope);


        final JsonObject petsForCaseResponse = createObjectBuilder().add("pets", createArrayBuilder()
                        .add(createPetResponse(petId1, formId1, defendantId1, caseId, createData("Bill", "Turner"), false))
                        .add(createPetResponse(petId2, formId2, defendantId2, caseId, createData("Davy", "Jones"), false)))
                .build();

        assertThat(result.payloadAsJsonObject(), is(petsForCaseResponse));
    }

    private JsonObject createPetResponse(final UUID petId, final UUID formId, final UUID defendantId, final UUID caseId, final JsonObject data, boolean isYouth) {

        return createObjectBuilder()
                .add("petId", petId.toString())
                .add("isYouth", isYouth)
                .add("defendants", createArrayBuilder()
                        .add(createObjectBuilder()
                                .add("defendantId", defendantId.toString())
                                .add("caseId", caseId.toString())
                        ))
                .build();
    }

    private JsonObject createData(final String firstName, final String lastName) {
        return createObjectBuilder()
                .add("firstName", firstName)
                .add("lastName", lastName)
                .build();
    }
}