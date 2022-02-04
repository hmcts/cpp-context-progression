package uk.gov.moj.cpp.progression.query;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.runners.MockitoJUnitRunner;
import uk.gov.justice.services.common.converter.ObjectToJsonObjectConverter;
import uk.gov.justice.services.common.converter.StringToJsonObjectConverter;
import uk.gov.justice.services.common.converter.jackson.ObjectMapperProducer;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.test.utils.core.enveloper.EnveloperFactory;
import uk.gov.moj.cpp.prosecutioncase.persistence.entity.PetCaseDefendantOffence;
import uk.gov.moj.cpp.prosecutioncase.persistence.entity.PetCaseDefendantOffenceKey;
import uk.gov.moj.cpp.prosecutioncase.persistence.repository.PetCaseDefendantOffenceRepository;

import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;
import java.util.List;
import java.util.UUID;

import static java.util.Arrays.asList;
import static java.util.UUID.randomUUID;
import static javax.json.Json.createArrayBuilder;
import static javax.json.Json.createObjectBuilder;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.test.utils.core.messaging.MetadataBuilderFactory.metadataWithDefaults;
import static uk.gov.justice.services.test.utils.core.reflection.ReflectionUtil.setField;

@RunWith(MockitoJUnitRunner.class)
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

    @Before
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
        final UUID offenceId1 = randomUUID();
        final UUID offenceId2 = randomUUID();

        when(petCaseDefendantOffenceRepository.findByCaseId(caseId)).thenReturn(asList(
                new PetCaseDefendantOffence(new PetCaseDefendantOffenceKey(defendantId1, offenceId1), caseId,  petId1),
                new PetCaseDefendantOffence(new PetCaseDefendantOffenceKey(defendantId2, offenceId2), caseId,  petId2)
        ));


        final JsonEnvelope envelope = JsonEnvelope.envelopeFrom(metadataWithDefaults().withName("cpscasemanagement.query.pets-for-case"), createObjectBuilder().add("caseId", caseId.toString()).build());
        final JsonEnvelope result = petQueryView.getPetsForCase(envelope);


        final JsonObject petsForCaseResponse = createObjectBuilder().add("pets", createArrayBuilder()
                .add(createPetResponse(petId1, formId1, defendantId1, caseId, asList(offenceId1), createData("Bill", "Turner")))
                .add(createPetResponse(petId2, formId2, defendantId2, caseId, asList(offenceId2), createData("Davy", "Jones"))))
                .build();

        assertThat(result.payloadAsJsonObject(), is(petsForCaseResponse));
    }

    private JsonObject createPetResponse(final UUID petId, final UUID formId, final UUID defendantId, final UUID caseId, final List<UUID> offences, final JsonObject data){
        final JsonArrayBuilder offencesJson = createArrayBuilder();
        offences.forEach(offence -> offencesJson.add(offence.toString()));

        return createObjectBuilder()
                .add("petId", petId.toString())
                .add("defendants", createArrayBuilder()
                        .add(createObjectBuilder()
                                .add("defendantId", defendantId.toString())
                                .add("caseId", caseId.toString())
                                .add("offences", offencesJson.build())
                        ))
                .build();
    }

    private JsonObject createData(final String firstName, final String lastName){
        return createObjectBuilder()
                .add("firstName", firstName)
                .add("lastName", lastName)
                .build();
    }
}