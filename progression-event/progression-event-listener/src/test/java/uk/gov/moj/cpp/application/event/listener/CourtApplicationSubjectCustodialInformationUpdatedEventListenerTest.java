package uk.gov.moj.cpp.application.event.listener;


import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import net.minidev.json.JSONValue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.justice.core.courts.CourtApplicationSubjectCustodialInformationUpdated;
import uk.gov.justice.core.courts.CustodialEstablishment;
import uk.gov.justice.core.courts.DefendantUpdate;
import uk.gov.justice.core.courts.Person;
import uk.gov.justice.core.courts.PersonDefendant;
import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.common.converter.ObjectToJsonObjectConverter;
import uk.gov.justice.services.common.converter.StringToJsonObjectConverter;
import uk.gov.justice.services.common.converter.jackson.ObjectMapperProducer;
import uk.gov.moj.cpp.prosecutioncase.persistence.entity.CourtApplicationEntity;
import uk.gov.moj.cpp.prosecutioncase.persistence.repository.CourtApplicationRepository;

import javax.json.JsonObject;
import java.util.UUID;

import static java.util.UUID.randomUUID;
import static javax.json.Json.createObjectBuilder;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.justice.services.test.utils.core.messaging.MetadataBuilderFactory.metadataWithRandomUUID;
import static uk.gov.justice.services.test.utils.core.reflection.ReflectionUtil.setField;

@ExtendWith(MockitoExtension.class)
public class CourtApplicationSubjectCustodialInformationUpdatedEventListenerTest {


    @Spy
    private JsonObjectToObjectConverter jsonObjectToObjectConverter;

    @Spy
    private ObjectToJsonObjectConverter objectToJsonObjectConverter;

    @Mock
    private StringToJsonObjectConverter stringToJsonObjectConverter;

    @Mock
    private CourtApplicationRepository repository;

    @Captor
    private ArgumentCaptor<CourtApplicationEntity> argumentCaptor;


    @Mock
    private JsonObject payload;


    @InjectMocks
    private CourtApplicationSubjectCustodialInformationUpdatedEventListener eventListener;

    @BeforeEach
    public void initMocks() {
        setField(this.objectToJsonObjectConverter, "mapper", new ObjectMapperProducer().objectMapper());
        setField(this.jsonObjectToObjectConverter, "objectMapper", new ObjectMapperProducer().objectMapper());

    }

    @Test
    public void testSubjectCustodialInformationUpdatedWhenRemoved() {

        final UUID applicationId = randomUUID();
        final CourtApplicationEntity persistedEntity = new CourtApplicationEntity();
        persistedEntity.setApplicationId(applicationId);
        persistedEntity.setPayload(payload.toString());


        when(repository.findBy(applicationId)).thenReturn(persistedEntity);
        when(stringToJsonObjectConverter.convert(persistedEntity.getPayload())).thenReturn(buildCourtApplication(applicationId.toString()));

        final CourtApplicationSubjectCustodialInformationUpdated courtApplicationSubjectCustodialInformationUpdated = CourtApplicationSubjectCustodialInformationUpdated.courtApplicationSubjectCustodialInformationUpdated()
                .withApplicationId(applicationId)
                .withDefendant(DefendantUpdate.defendantUpdate()
                        .withPersonDefendant(PersonDefendant.personDefendant()
                                .withPersonDetails(Person.person()
                                        .withFirstName("XYZ")
                                        .withLastName("ABC")
                                        .build())
                                .build())
                        .build())
                .build();

        eventListener.processCourtApplicationSubjectCustodialInformationUpdated(envelopeFrom(metadataWithRandomUUID("progression.event.court-application-subject-custodial-information-updated"),
                objectToJsonObjectConverter.convert(courtApplicationSubjectCustodialInformationUpdated)));
        verify(repository).save(argumentCaptor.capture());
        final CourtApplicationEntity updatedApplicationEntiry = argumentCaptor.getValue();
        final ObjectMapper mapper = new ObjectMapperProducer().objectMapper();
        final JsonNode applicationNode = mapper.valueToTree(JSONValue.parse(updatedApplicationEntiry.getPayload()));
        assertEquals(true, applicationNode.findPath("custodialEstablishment").isMissingNode());
    }

    @Test
    public void testSubjectCustodialInformationUpdatedWhenAdded() {

        final UUID applicationId = randomUUID();
        final CourtApplicationEntity persistedEntity = new CourtApplicationEntity();
        persistedEntity.setApplicationId(applicationId);
        persistedEntity.setPayload(payload.toString());


        when(repository.findBy(applicationId)).thenReturn(persistedEntity);
        when(stringToJsonObjectConverter.convert(persistedEntity.getPayload())).thenReturn(buildCourtApplicationWithoutCustodyEstablishment(applicationId.toString()));

        final CourtApplicationSubjectCustodialInformationUpdated courtApplicationSubjectCustodialInformationUpdated = CourtApplicationSubjectCustodialInformationUpdated.courtApplicationSubjectCustodialInformationUpdated()
                .withApplicationId(applicationId)
                .withDefendant(DefendantUpdate.defendantUpdate()
                        .withPersonDefendant(PersonDefendant.personDefendant()
                                .withPersonDetails(Person.person()
                                        .withFirstName("XYZ")
                                        .withLastName("ABC")
                                        .build())
                                .withCustodialEstablishment(CustodialEstablishment.custodialEstablishment()
                                        .withId(randomUUID())
                                        .withCustody("Police")
                                        .withName("Custody")
                                        .build())
                                .build())
                        .build())
                .build();

        eventListener.processCourtApplicationSubjectCustodialInformationUpdated(envelopeFrom(metadataWithRandomUUID("progression.event.court-application-subject-custodial-information-updated"),
                objectToJsonObjectConverter.convert(courtApplicationSubjectCustodialInformationUpdated)));
        verify(repository).save(argumentCaptor.capture());
        final CourtApplicationEntity updatedApplicationEntiry = argumentCaptor.getValue();
        final ObjectMapper mapper = new ObjectMapperProducer().objectMapper();
        final JsonNode applicationNode = mapper.valueToTree(JSONValue.parse(updatedApplicationEntiry.getPayload()));
        assertEquals("Police", applicationNode.findPath("custodialEstablishment").path("custody").asText());
        assertEquals("Custody", applicationNode.findPath("custodialEstablishment").path("name").asText());

    }

    @Test
    public void testSubjectCustodialInformationUpdatedWhenUpdated() {

        final UUID applicationId = randomUUID();
        final CourtApplicationEntity persistedEntity = new CourtApplicationEntity();
        persistedEntity.setApplicationId(applicationId);
        persistedEntity.setPayload(payload.toString());


        when(repository.findBy(applicationId)).thenReturn(persistedEntity);
        when(stringToJsonObjectConverter.convert(persistedEntity.getPayload())).thenReturn(buildCourtApplication(applicationId.toString()));

        final CourtApplicationSubjectCustodialInformationUpdated courtApplicationSubjectCustodialInformationUpdated = CourtApplicationSubjectCustodialInformationUpdated.courtApplicationSubjectCustodialInformationUpdated()
                .withApplicationId(applicationId)
                .withDefendant(DefendantUpdate.defendantUpdate()
                        .withPersonDefendant(PersonDefendant.personDefendant()
                                .withPersonDetails(Person.person()
                                        .withFirstName("XYZ")
                                        .withLastName("ABC")
                                        .build())
                                .withCustodialEstablishment(CustodialEstablishment.custodialEstablishment()
                                        .withId(randomUUID())
                                        .withCustody("Police1")
                                        .withName("Custody1")
                                        .build())
                                .build())
                        .build())
                .build();

        eventListener.processCourtApplicationSubjectCustodialInformationUpdated(envelopeFrom(metadataWithRandomUUID("progression.event.court-application-subject-custodial-information-updated"),
                objectToJsonObjectConverter.convert(courtApplicationSubjectCustodialInformationUpdated)));
        verify(repository).save(argumentCaptor.capture());
        final CourtApplicationEntity updatedApplicationEntiry = argumentCaptor.getValue();
        final ObjectMapper mapper = new ObjectMapperProducer().objectMapper();
        final JsonNode applicationNode = mapper.valueToTree(JSONValue.parse(updatedApplicationEntiry.getPayload()));
        assertEquals("Police1", applicationNode.findPath("custodialEstablishment").path("custody").asText());
        assertEquals("Custody1", applicationNode.findPath("custodialEstablishment").path("name").asText());

    }


    @Test
    public void testSubjectCustodialInformationNotUpdatedWhenSubjectIsNotPresentInApplication() {

        final UUID applicationId = randomUUID();
        final CourtApplicationEntity persistedEntity = new CourtApplicationEntity();
        persistedEntity.setApplicationId(applicationId);
        persistedEntity.setPayload(payload.toString());


        when(repository.findBy(applicationId)).thenReturn(persistedEntity);
        when(stringToJsonObjectConverter.convert(persistedEntity.getPayload())).thenReturn(buildCourtApplicationWithoutSubject(applicationId.toString()));

        final CourtApplicationSubjectCustodialInformationUpdated courtApplicationSubjectCustodialInformationUpdated = CourtApplicationSubjectCustodialInformationUpdated.courtApplicationSubjectCustodialInformationUpdated()
                .withApplicationId(applicationId)
                .withDefendant(DefendantUpdate.defendantUpdate()
                        .withPersonDefendant(PersonDefendant.personDefendant()
                                .withPersonDetails(Person.person()
                                        .withFirstName("XYZ")
                                        .withLastName("ABC")
                                        .build())
                                .withCustodialEstablishment(CustodialEstablishment.custodialEstablishment()
                                        .withId(randomUUID())
                                        .withCustody("Police")
                                        .withName("Custody")
                                        .build())
                                .build())
                        .build())
                .build();

        eventListener.processCourtApplicationSubjectCustodialInformationUpdated(envelopeFrom(metadataWithRandomUUID("progression.event.court-application-subject-custodial-information-updated"),
                objectToJsonObjectConverter.convert(courtApplicationSubjectCustodialInformationUpdated)));
        verify(repository, never()).save(argumentCaptor.capture());

    }

    private JsonObject buildCourtApplication(final String applicationId) {
        return createObjectBuilder()
                        .add("id", applicationId)
                        .add("applicant", createObjectBuilder().add("prosecutingAuthority", createObjectBuilder()
                                .build()).build())
                        .add("subject", createObjectBuilder()
                                .add("id", randomUUID().toString())
                                .add("summonsRequired", false)
                                .add("notificationRequired", false)
                                .add("masterDefendant", createObjectBuilder()
                                        .add("masterDefendantId", randomUUID().toString())
                                        .add("personDefendant", createObjectBuilder()
                                                .add("personDetails", createObjectBuilder()
                                                        .add("firstName", "XXYZ")
                                                        .add("lastName", "ABC")
                                                        .build()
                                                )
                                                .add("custodialEstablishment", createObjectBuilder()
                                                        .add("name", "Custody")
                                                        .add("id", randomUUID().toString())
                                                        .add("custody", "Police")
                                                .build())
                                                .build())
                                        .build())
                                .build())
                        .build();
    }

    private JsonObject buildCourtApplicationWithoutCustodyEstablishment(final String applicationId) {
        return createObjectBuilder()
                .add("id", applicationId)
                .add("applicant", createObjectBuilder().add("prosecutingAuthority", createObjectBuilder()
                        .build()).build())
                .add("subject", createObjectBuilder()
                        .add("id", randomUUID().toString())
                        .add("summonsRequired", false)
                        .add("notificationRequired", false)
                        .add("masterDefendant", createObjectBuilder()
                                .add("masterDefendantId", randomUUID().toString())
                                .add("personDefendant", createObjectBuilder()
                                        .add("personDetails", createObjectBuilder()
                                                .add("firstName", "XXYZ")
                                                .add("lastName", "ABC")
                                                .build()
                                        ))

                                .build())
                        .build())
                .build();
    }

    private JsonObject buildCourtApplicationWithoutSubject(final String applicationId) {
        return createObjectBuilder()
                .add("id", applicationId)
                .add("applicant", createObjectBuilder().add("prosecutingAuthority", createObjectBuilder()
                        .build()).build())
                .build();
    }
}
