package uk.gov.moj.cpp.progression.query.api;


import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.justice.core.courts.Defendant;
import uk.gov.justice.core.courts.Person;
import uk.gov.justice.core.courts.PersonDefendant;
import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.common.converter.ObjectToJsonObjectConverter;
import uk.gov.justice.services.common.converter.jackson.ObjectMapperProducer;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.core.requester.Requester;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.test.utils.core.enveloper.EnveloperFactory;
import uk.gov.justice.services.test.utils.core.messaging.MetadataBuilderFactory;
import uk.gov.moj.cpp.progression.domain.pojo.Direction;
import uk.gov.moj.cpp.progression.domain.pojo.RefDataDirection;
import uk.gov.moj.cpp.progression.domain.pojo.ReferenceDataDirectionManagementType;
import uk.gov.moj.cpp.progression.query.api.service.ProgressionService;
import uk.gov.moj.cpp.progression.query.view.DirectionQueryView;
import uk.gov.moj.cpp.progression.query.view.service.DefendantService;
import uk.gov.moj.cpp.progression.query.view.service.transformer.AssigneeTransformer;
import uk.gov.moj.cpp.progression.query.view.service.transformer.WitnessPetTransformer;
import uk.gov.moj.cpp.progression.query.view.service.transformer.WitnessPtphTransformer;
import uk.gov.moj.cpp.progression.service.RefDataService;


import uk.gov.justice.services.messaging.JsonObjects;
import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.json.JsonReader;
import javax.json.JsonValue;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;


import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.UUID.randomUUID;
import static uk.gov.justice.services.messaging.JsonObjects.createArrayBuilder;
import static uk.gov.justice.services.messaging.JsonObjects.createObjectBuilder;
import static uk.gov.justice.services.messaging.JsonObjects.createReader;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.anyBoolean;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.justice.services.messaging.JsonEnvelope.metadataBuilder;
import static uk.gov.justice.services.test.utils.core.reflection.ReflectionUtil.setField;

@ExtendWith(MockitoExtension.class)
public class DirectionsManagementQueryApiTest {

    @Mock
    private RefDataService refDataService;

    @Mock
    private DefendantService defendantService;

    @Mock
    private ProgressionService progressionService;

    @Mock
    private DirectionQueryView directionQueryView;

    @Mock
    private AssigneeTransformer assigneeTransformer;

    @Mock
    private WitnessPtphTransformer witnessPtphTransformer;

    @Mock
    private WitnessPetTransformer witnessPetTransformer;

    @Spy
    private Enveloper enveloper = EnveloperFactory.createEnveloper();


    @Spy
    private JsonObjectToObjectConverter jsonObjectToObjectConverter;

    @Spy
    private ObjectToJsonObjectConverter objectToJsonObjectConverter;

    @InjectMocks
    private DirectionsManagementQueryApi directionsManagementQueryApi;

    @Captor
    private ArgumentCaptor<JsonEnvelope> envelopeCaptor;

    @Mock
    private Requester requester;

    @BeforeEach
    public void setup() {
        setField(this.jsonObjectToObjectConverter, "objectMapper", new ObjectMapperProducer().objectMapper());
        setField(this.objectToJsonObjectConverter, "mapper", new ObjectMapperProducer().objectMapper());
    }


    @Test
    public void shouldReturnPetCaseDirections() {
        final UUID caseId = randomUUID();
        final UUID formId = randomUUID();

        final JsonEnvelope query = JsonEnvelope.envelopeFrom(
                MetadataBuilderFactory.metadataWithRandomUUID("directionsmanagement.query.form-directions"),
                JsonObjects.createObjectBuilder()
                        .add("categories", "test1,test2")
                        .add("caseId", caseId.toString())
                        .add("formId", formId.toString())
                        .add("formType", "PET")
                        .build());
        final UUID directionManagementId = randomUUID();

        when(refDataService.getDirectionManagementTypes()).thenReturn(asList(
                new ReferenceDataDirectionManagementType(directionManagementId, "name1", 1, "PET", "", "test1", "")));

        when(refDataService.getDirections()).thenReturn(asList(
                Direction.direction().withDirectionId(directionManagementId).build()));

        when(defendantService.getDefendantList(query)).thenReturn(asList(Defendant.defendant().build()));


        buildMaterialResponse(formId);

        final Map<UUID, String> assignees = new HashMap<>();
        final Map<UUID, String> witness = new HashMap<>();
        when(witnessPetTransformer.transform(any())).thenReturn(witness);
        when(directionQueryView.getTransformedDirections(any(), any(), any(), any(), any(), anyBoolean(), anyString())).thenReturn(RefDataDirection.refDataDirection().withSequenceNumber(1).build());

        final JsonEnvelope petCaseDirections = directionsManagementQueryApi.getPetCaseDirections(query);

        assertThat(petCaseDirections, is(notNullValue())); // Ensure the response is not null
        assertThat(petCaseDirections.metadata().name(), is("directionsmanagement.query.form-directions"));
        assertThat(petCaseDirections.payloadAsJsonObject().getJsonArray("directions").getJsonObject(0).getInt("sequenceNumber"), is(0));
    }

    @Test
    public void shouldReturnPtphCaseDirections() {
        final UUID caseId = randomUUID();
        final UUID formId = randomUUID();

        final JsonEnvelope query = JsonEnvelope.envelopeFrom(
                MetadataBuilderFactory.metadataWithRandomUUID("directionsmanagement.query.form-directions"),
                JsonObjects.createObjectBuilder()
                        .add("categories", "test1,test2")
                        .add("caseId", caseId.toString())
                        .add("formId", formId.toString())
                        .add("formType", "PTPH")
                        .build());
        final UUID directionManagementId = randomUUID();

        when(refDataService.getDirectionManagementTypes()).thenReturn(asList(
                new ReferenceDataDirectionManagementType(directionManagementId, "name1", 1, "PTPH", "", "test1", "")));

        when(refDataService.getDirections()).thenReturn(asList(
                Direction.direction().withDirectionId(directionManagementId).build()));

        when(defendantService.getDefendantList(query)).thenReturn(asList(Defendant.defendant().build()));


        buildMaterialResponse(formId);

        final Map<UUID, String> assignees = new HashMap<>();
        final Map<UUID, String> witness = new HashMap<>();
        when(assigneeTransformer.transform(any())).thenReturn(assignees);
        when(witnessPtphTransformer.transform(any())).thenReturn(witness);
        when(directionQueryView.getTransformedDirections(any(), any(), any(), any(), any(), anyBoolean(), anyString())).thenReturn(RefDataDirection.refDataDirection().withSequenceNumber(1).build());

        final JsonEnvelope petCaseDirections = directionsManagementQueryApi.getPetCaseDirections(query);

        assertThat(petCaseDirections.metadata().name(), is("directionsmanagement.query.form-directions"));
        assertThat(petCaseDirections.payloadAsJsonObject().getJsonArray("directions").getJsonObject(0).getInt("sequenceNumber"), is(1));
    }


    @Test
    public void shouldGetPetCaseDirectionsReturnNoRefDataDirectionWhenFormTypeDoesNotMatch() {
        final JsonObject mockDirectionType = createReader(getClass().getClassLoader().
                getResourceAsStream("refdata-direction-type-response.json")).
                readObject();

        final ReferenceDataDirectionManagementType referenceDataDirectionManagementType = jsonObjectToObjectConverter.convert(mockDirectionType,
                ReferenceDataDirectionManagementType.class);
        when(refDataService.getDirectionManagementTypes()).thenReturn(asList(referenceDataDirectionManagementType));

        final Defendant defendant = Defendant.defendant().withId(randomUUID())
                .withPersonDefendant(PersonDefendant.personDefendant()
                        .withPersonDetails(Person.person().withFirstName("Harry").withLastName("Junior")
                                .build())
                        .build())
                .build();
        final List<Defendant> defendants = new ArrayList<Defendant>() {{
            add(defendant);
        }};

//        when(defendantService.getDefendantList(any())).thenReturn(defendants);

        Direction direction = Direction.direction().withDirectionId(UUID.fromString("2cd85752-8329-48f5-8708-a396c8e8835f")).build();

//        when(refDataService.getDirections()).thenReturn(Arrays.asList(direction));

        RefDataDirection refDataDirection = RefDataDirection.refDataDirection().withSequenceNumber(1).build();
//        when(directionQueryView.getTransformedDirections(any(), any(), any(), any(), any(), anyBoolean(), anyString())).thenReturn(refDataDirection);

        final JsonObjectBuilder queryPayload = JsonObjects.createObjectBuilder().add("categories", "cat1,cat2")
                .add("formType", "PET")
                .add("caseId", randomUUID().toString())
                .add("formId", randomUUID().toString());

        final JsonEnvelope query = JsonEnvelope.envelopeFrom(
                MetadataBuilderFactory.metadataWithRandomUUID("directionsmanagement.query.form-directions"),
                queryPayload);

        final JsonEnvelope transformedResponse = directionsManagementQueryApi.getPetCaseDirections(query);

        assertThat(transformedResponse.metadata().name(), is("directionsmanagement.query.form-directions"));
        assertThat(((JsonArray)transformedResponse.payloadAsJsonObject().get("directions")).size(), is((0)));

    }

    @Test
    public void shouldReturnEmptyDirectionsWhenNoDirectionManagementTypes() {
        final UUID caseId = randomUUID();
        final UUID formId = randomUUID();
        final JsonEnvelope query = JsonEnvelope.envelopeFrom(
                MetadataBuilderFactory.metadataWithRandomUUID("directionsmanagement.query.form-directions"),
                JsonObjects.createObjectBuilder()
                        .add("categories", "test1,test2")
                        .add("caseId", caseId.toString())
                        .add("formId", formId.toString())
                        .add("formType", "PET")
                        .build());

        when(refDataService.getDirectionManagementTypes()).thenReturn(emptyList());

        final JsonEnvelope petCaseDirections = directionsManagementQueryApi.getPetCaseDirections(query);

        assertThat(petCaseDirections.metadata().name(), is("directionsmanagement.query.form-directions"));
        assertThat(petCaseDirections.payloadAsJsonObject().getJsonArray("directions").size(), is(0));
    }

    @Test
    public void shouldReturnEmptyDirectionsWhenNoMatchingCategories() {
        final UUID caseId = randomUUID();
        final UUID formId = randomUUID();
        final JsonEnvelope query = JsonEnvelope.envelopeFrom(
                MetadataBuilderFactory.metadataWithRandomUUID("directionsmanagement.query.form-directions"),
                JsonObjects.createObjectBuilder()
                        .add("categories", "nonexistent")
                        .add("caseId", caseId.toString())
                        .add("formId", formId.toString())
                        .add("formType", "PET")
                        .build());
        final UUID directionManagementId = randomUUID();

        when(refDataService.getDirectionManagementTypes()).thenReturn(asList(
                new ReferenceDataDirectionManagementType(directionManagementId, "name1", 1, "PET", "", "test1", "")));

        final JsonEnvelope petCaseDirections = directionsManagementQueryApi.getPetCaseDirections(query);

        assertThat(petCaseDirections.metadata().name(), is("directionsmanagement.query.form-directions"));
        assertThat(petCaseDirections.payloadAsJsonObject().getJsonArray("directions").size(), is(0));
    }

    @Test
    public void shouldReturnEmptyDirectionsWhenNoDefendants() {
        final UUID caseId = randomUUID();
        final UUID formId = randomUUID();
        final JsonEnvelope query = JsonEnvelope.envelopeFrom(
                MetadataBuilderFactory.metadataWithRandomUUID("directionsmanagement.query.form-directions"),
                JsonObjects.createObjectBuilder()
                        .add("categories", "test1,test2")
                        .add("caseId", caseId.toString())
                        .add("formId", formId.toString())
                        .add("formType", "PET")
                        .build());
        final UUID directionManagementId = randomUUID();

        when(refDataService.getDirectionManagementTypes()).thenReturn(asList(
                new ReferenceDataDirectionManagementType(directionManagementId, "name1", 1, "PET", "", "test1", "")));

        when(refDataService.getDirections()).thenReturn(asList(
                Direction.direction().withDirectionId(directionManagementId).build()));

        when(defendantService.getDefendantList(query)).thenReturn(emptyList());


        buildMaterialResponse(formId);

        final JsonEnvelope petCaseDirections = directionsManagementQueryApi.getPetCaseDirections(query);

        assertThat(petCaseDirections.metadata().name(), is("directionsmanagement.query.form-directions"));
        assertThat(petCaseDirections.payloadAsJsonObject().getJsonArray("directions").size(), is(0));
    }

    @Test
    public void shouldReturnEmptyDirectionsWhenNoMatchingDirections() {
        final UUID caseId = randomUUID();
        final UUID formId = randomUUID();
        final JsonEnvelope query = JsonEnvelope.envelopeFrom(
                MetadataBuilderFactory.metadataWithRandomUUID("directionsmanagement.query.form-directions"),
                JsonObjects.createObjectBuilder()
                        .add("categories", "test1,test2")
                        .add("caseId", caseId.toString())
                        .add("formId", formId.toString())
                        .add("formType", "PET")
                        .build());
        final UUID directionManagementId = randomUUID();

        when(refDataService.getDirectionManagementTypes()).thenReturn(asList(
                new ReferenceDataDirectionManagementType(directionManagementId, "name1", 1, "PET", "", "test1", "")));

        when(refDataService.getDirections()).thenReturn(emptyList());

        when(defendantService.getDefendantList(query)).thenReturn(asList(Defendant.defendant().build()));

        buildMaterialResponse(formId);

        final JsonEnvelope petCaseDirections = directionsManagementQueryApi.getPetCaseDirections(query);

        assertThat(petCaseDirections.metadata().name(), is("directionsmanagement.query.form-directions"));
        assertThat(petCaseDirections.payloadAsJsonObject().getJsonArray("directions").size(), is(0));
    }

    private void buildMaterialResponse(UUID formId) {
        final JsonEnvelope materialResponse = envelopeFrom(metadataBuilder().withId(randomUUID())
                        .withName("material.query.structured-form"),
                createObjectBuilder()
                        .add("structuredFormId", formId.toString())
                        .add("data", "this is form data as a string")
                        .add("lastUpdated", "12/10/2021")
                        .build());

        when(requester.request(any(Envelope.class)))
                .thenReturn(materialResponse);
    }

    private JsonEnvelope getJsonEnvelope(final Envelope envelop) {
        return envelopeFrom(envelop.metadata(), (JsonValue) envelop.payload());
    }

    private JsonObject getJsonPayload(final String fileName) throws IOException {
        final ClassLoader loader = Thread.currentThread().getContextClassLoader();
        try (final InputStream stream = loader.getResourceAsStream(fileName);
             final JsonReader jsonReader = JsonObjects.createReader(stream)) {
            final JsonObject payload = jsonReader.readObject();
            return payload;
        }
    }
}
