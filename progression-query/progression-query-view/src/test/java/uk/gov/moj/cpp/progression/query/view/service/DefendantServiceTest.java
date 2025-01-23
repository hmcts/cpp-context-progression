package uk.gov.moj.cpp.progression.query.view.service;


import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.justice.core.courts.Defendant;
import uk.gov.justice.core.courts.Person;
import uk.gov.justice.core.courts.PersonDefendant;
import uk.gov.justice.core.courts.ProsecutionCase;
import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.common.converter.ObjectToJsonObjectConverter;
import uk.gov.justice.services.common.converter.jackson.ObjectMapperProducer;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.messaging.Metadata;
import uk.gov.justice.services.test.utils.framework.api.JsonObjectConvertersFactory;
import uk.gov.moj.cpp.progression.query.ProsecutionCaseQuery;

import javax.json.JsonObject;
import java.util.List;
import java.util.UUID;

import static java.util.Collections.emptyList;
import static java.util.UUID.randomUUID;
import static javax.json.Json.createObjectBuilder;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.justice.services.messaging.spi.DefaultJsonMetadata.metadataBuilder;
import static uk.gov.justice.services.test.utils.core.reflection.ReflectionUtil.setField;

@ExtendWith(MockitoExtension.class)
public class DefendantServiceTest {


    @Mock
    private ProsecutionCaseQuery prosecutionCaseQuery;


    @Spy
    private JsonObjectToObjectConverter jsonObjectToObjectConverter  = new JsonObjectConvertersFactory().jsonObjectToObjectConverter();;

    @Spy
    private final ObjectToJsonObjectConverter objectToJsonObjectConverter = new ObjectToJsonObjectConverter(new ObjectMapperProducer().objectMapper());

    @InjectMocks
    private DefendantService defendantService;

    private static final String QUERY_DIRECTION = "directionsmanagement.query.direction";
    private static final UUID directionId = UUID.fromString("0a18eadf-0970-42ff-b980-b7f383391391");
    private static final UUID caseId = UUID.fromString("3277f30a-f51a-489f-926e-7ecf84236d98");
    private static final String PROSECUTION_CASE_QUERY = "progression.query.prosecutioncase";

    @BeforeEach
    public void setup() {
        setField(this.jsonObjectToObjectConverter, "objectMapper", new ObjectMapperProducer().objectMapper());
    }


    @Test
    public void shouldReturnDefendantListWhenProsecutionCaseHasDefendants() {

      final List<Defendant> defendants = List.of(
    Defendant.defendant().withId(UUID.randomUUID())
        .withPersonDefendant(PersonDefendant.personDefendant()
            .withPersonDetails(Person.person().withFirstName("Harry").withLastName("Junior").build())
            .build())
        .build(),
    Defendant.defendant().withId(UUID.randomUUID())
        .withPersonDefendant(PersonDefendant.personDefendant()
            .withPersonDetails(Person.person().withFirstName("John").withLastName("Smith").build())
            .build())
        .build()
);
        final JsonEnvelope query = createQueryDirectionEnvelope();
        final ProsecutionCase prosecutionCase = ProsecutionCase.prosecutionCase().withDefendants(defendants).build();
        var prosecutionCaseJson  =  createObjectBuilder().add("prosecutionCase", objectToJsonObjectConverter.convert(prosecutionCase)).build();

        final Metadata metadata = metadataFor(PROSECUTION_CASE_QUERY, randomUUID());
        final JsonEnvelope envelope = JsonEnvelope.envelopeFrom(metadata, prosecutionCaseJson);

        when(prosecutionCaseQuery.getCase(query)).thenReturn(envelope);

        final List<Defendant> result = defendantService.getDefendantList(query);

        assertThat(result.size(), is(2));
        assertThat(result, is(defendants));
    }

    @Test
    public void shouldReturnEmptyListWhenProsecutionCaseHasNoDefendants() {

        final JsonEnvelope query = createQueryDirectionEnvelope();
        final ProsecutionCase prosecutionCase = ProsecutionCase.prosecutionCase().build();
        var prosecutionCaseJson  =  createObjectBuilder().add("prosecutionCase", objectToJsonObjectConverter.convert(prosecutionCase)).build();

        final Metadata metadata = metadataFor(PROSECUTION_CASE_QUERY, randomUUID());
        final JsonEnvelope envelope = JsonEnvelope.envelopeFrom(metadata, prosecutionCaseJson);

        when(prosecutionCaseQuery.getCase(query)).thenReturn(envelope);

        final List<Defendant> result = defendantService.getDefendantList(query);

        assertThat(result.size(), is(0));
        assertThat(result, is(emptyList()));
    }


    private JsonEnvelope createQueryDirectionEnvelope() {
        final JsonObject jsonObject = createObjectBuilder()
                .add("directionId", directionId.toString())
                .add("caseId", caseId.toString())
                .add("orderDate", "2021-06-28")
                .build();
        return envelopeFrom(
                metadataBuilder().withId(randomUUID()).withName(QUERY_DIRECTION),
                jsonObject);
    }


    public static Metadata metadataFor(final String commandName, final UUID commandId) {
        return Envelope.metadataBuilder()
                .withName(commandName)
                .withId(commandId)
                .withUserId(randomUUID().toString())
                .build();
    }
}