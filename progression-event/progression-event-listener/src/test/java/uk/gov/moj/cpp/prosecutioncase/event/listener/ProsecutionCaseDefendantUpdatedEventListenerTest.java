package uk.gov.moj.cpp.prosecutioncase.event.listener;

import static java.util.Collections.singletonList;
import static java.util.UUID.randomUUID;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.ArgumentCaptor.forClass;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.test.utils.core.reflection.ReflectionUtil.setField;


import java.util.Arrays;
import uk.gov.justice.core.courts.Defendant;
import uk.gov.justice.core.courts.Ethnicity;
import uk.gov.justice.core.courts.InitiationCode;
import uk.gov.justice.core.courts.ProsecutionCaseListingNumberUpdated;
import uk.gov.justice.core.courts.ProsecutionCaseListingNumberDecreased;
import uk.gov.justice.core.courts.Marker;
import uk.gov.justice.core.courts.Offence;
import uk.gov.justice.core.courts.OffenceListingNumbers;
import uk.gov.justice.core.courts.Person;
import uk.gov.justice.core.courts.PersonDefendant;
import uk.gov.justice.core.courts.PoliceOfficerInCase;
import uk.gov.justice.core.courts.ProsecutionCase;
import uk.gov.justice.core.courts.ProsecutionCaseIdentifier;
import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.common.converter.ObjectToJsonObjectConverter;
import uk.gov.justice.services.common.converter.StringToJsonObjectConverter;
import uk.gov.justice.services.common.converter.jackson.ObjectMapperProducer;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.prosecutioncase.persistence.entity.ProsecutionCaseEntity;
import uk.gov.moj.cpp.prosecutioncase.persistence.repository.ProsecutionCaseRepository;
import uk.gov.moj.cpp.prosecutioncase.persistence.repository.mapping.SearchProsecutionCase;
import uk.gov.moj.cpp.util.FileUtil;

import java.util.Arrays;
import java.util.UUID;

import javax.json.JsonArray;
import javax.json.JsonObject;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class ProsecutionCaseDefendantUpdatedEventListenerTest {

    @InjectMocks
    private ProsecutionCaseDefendantUpdatedEventListener prosecutionCaseDefendantUpdatedEventListener;

    @Spy
    private JsonObjectToObjectConverter jsonObjectToObjectConverter;

    @Spy
    private ObjectToJsonObjectConverter objectToJsonObjectConverter;

    @Spy
    private StringToJsonObjectConverter stringToJsonObjectConverter;

    @Mock
    private JsonEnvelope jsonEnvelope;

    @Mock
    private ProsecutionCaseRepository prosecutionCaseRepository;

    @Captor
    private ArgumentCaptor<ProsecutionCaseEntity> argumentCaptor = forClass(ProsecutionCaseEntity.class);

    @Mock
    private SearchProsecutionCase searchCase;

    private final UUID prosecutionCaseId = randomUUID();
    private final UUID defendantId = randomUUID();
    private final UUID masterDefendantId = randomUUID();
    private final UUID policeBailStatusId = randomUUID();
    private final String policeBailStatusDesc = "Remanded into Custody";
    private final String policeBailConditions = "Police bail condition explanation";
    private final String hearingLanguage = "WELSH";

    @Before
    public void setUp() {
        setField(this.objectToJsonObjectConverter, "mapper", new ObjectMapperProducer().objectMapper());
        setField(this.jsonObjectToObjectConverter, "objectMapper", new ObjectMapperProducer().objectMapper());
    }

    @Test
    public void shouldProcessProsecutionCaseDefendantUpdated() {
        final ProsecutionCase prosecutionCase = getProsecutionCase(prosecutionCaseId, defendantId, masterDefendantId);
        final String eventPayload = FileUtil.getPayload("json/prosecution-case-defendant-updated.json")
                .replace("PROSECUTION_CASE_ID", prosecutionCaseId.toString())
                .replace("DEFENDANT_ID", defendantId.toString())
                .replace("HEARING_LANGUAGE_NEEDS", hearingLanguage)
                .replace("POLICE_BAIL_STATUS_ID", policeBailStatusId.toString())
                .replace("POLICE_BAIL_STATUS_DESC", policeBailStatusDesc)
                .replace("POLICE_BAIL_CONDITIONS", policeBailConditions);

        final ProsecutionCaseEntity prosecutionCaseEntity = new ProsecutionCaseEntity();
        prosecutionCaseEntity.setCaseId(prosecutionCaseId);
        prosecutionCaseEntity.setPayload(objectToJsonObjectConverter.convert(prosecutionCase).toString());

        final JsonObject eventPayloadJsonObject = stringToJsonObjectConverter.convert(eventPayload);

        when(jsonEnvelope.payloadAsJsonObject()).thenReturn(eventPayloadJsonObject);

        when(prosecutionCaseRepository.findByCaseId(prosecutionCaseId)).thenReturn(prosecutionCaseEntity);

        doNothing().when(searchCase).updateSearchable(any());

        prosecutionCaseDefendantUpdatedEventListener.processProsecutionCaseDefendantUpdated(jsonEnvelope);

        verify(prosecutionCaseRepository).save(argumentCaptor.capture());
        assertThat(argumentCaptor.getValue(), is(notNullValue()));

        final ProsecutionCaseEntity updatedProsecutionCaseEntity = argumentCaptor.getValue();
        final JsonObject payload =  stringToJsonObjectConverter.convert(updatedProsecutionCaseEntity.getPayload());
        final JsonArray defendantProperties = payload.getJsonArray("defendants");
        assertThat(defendantProperties.getJsonObject(0).getJsonObject("personDefendant").getString("policeBailConditions"), is(policeBailConditions));

        final JsonObject policeBailStatusJsonObject = defendantProperties.getJsonObject(0).getJsonObject("personDefendant").getJsonObject("policeBailStatus");
        final JsonObject personDetailsJsonObject = defendantProperties.getJsonObject(0).getJsonObject("personDefendant").getJsonObject("personDetails");

        assertThat(policeBailStatusJsonObject.getString("id"), is(policeBailStatusId.toString()));
        assertThat(policeBailStatusJsonObject.getString("description"), is(policeBailStatusDesc));
        assertThat(personDetailsJsonObject.getString("hearingLanguageNeeds"), is(hearingLanguage));

        assertThat(defendantProperties.getJsonObject(0).getString("id"), is(defendantId.toString()));
        assertThat(defendantProperties.getJsonObject(0).getString("masterDefendantId"), is(masterDefendantId.toString()));
        assertThat(updatedProsecutionCaseEntity.getCaseId(), is(prosecutionCaseId));
    }

    @Test
    public void shouldProcessProsecutionCaseUpdated() {
        final ProsecutionCase prosecutionCase = getProsecutionCase(prosecutionCaseId, defendantId, defendantId);
        final String eventPayload = FileUtil.getPayload("json/hearing-resulted-case-updated.json")
                .replaceAll("PROSECUTION_CASE_ID", prosecutionCaseId.toString())
                .replaceAll("DEFENDANT_ID", defendantId.toString())
                .replace("POLICE_BAIL_STATUS_ID", policeBailStatusId.toString())
                .replace("POLICE_BAIL_STATUS_DESC", policeBailStatusDesc)
                .replace("POLICE_BAIL_CONDITIONS", policeBailConditions);

        final ProsecutionCaseEntity prosecutionCaseEntity = new ProsecutionCaseEntity();
        prosecutionCaseEntity.setCaseId(prosecutionCaseId);
        prosecutionCaseEntity.setPayload(objectToJsonObjectConverter.convert(prosecutionCase).toString());

        final JsonObject eventPayloadJsonObject = stringToJsonObjectConverter.convert(eventPayload);

        when(jsonEnvelope.payloadAsJsonObject()).thenReturn(eventPayloadJsonObject);

        when(prosecutionCaseRepository.findByCaseId(prosecutionCaseId)).thenReturn(prosecutionCaseEntity);

        doNothing().when(searchCase).updateSearchable(any());

        prosecutionCaseDefendantUpdatedEventListener.processProsecutionCaseUpdated(jsonEnvelope);

        verify(prosecutionCaseRepository).save(argumentCaptor.capture());
        assertThat(argumentCaptor.getValue(), is(notNullValue()));

        final ProsecutionCaseEntity updatedProsecutionCaseEntity = argumentCaptor.getValue();
        final JsonObject payload =  stringToJsonObjectConverter.convert(updatedProsecutionCaseEntity.getPayload());
        final JsonArray defendantProperties = payload.getJsonArray("defendants");
        assertThat(defendantProperties.getJsonObject(0).getJsonObject("personDefendant").getString("policeBailConditions"), is(policeBailConditions));

        final JsonObject policeBailStatusJsonObject = defendantProperties.getJsonObject(0).getJsonObject("personDefendant").getJsonObject("policeBailStatus");

        assertThat(policeBailStatusJsonObject.getString("id"), is(policeBailStatusId.toString()));
        assertThat(policeBailStatusJsonObject.getString("description"), is(policeBailStatusDesc));

        assertThat(defendantProperties.getJsonObject(0).getString("id"), is(defendantId.toString()));
        assertThat(defendantProperties.getJsonObject(0).getString("masterDefendantId"), is(defendantId.toString()));
        assertThat(updatedProsecutionCaseEntity.getCaseId(), is(prosecutionCaseId));
    }

    @Test
    public void shouldUpdateViewStoreWithListingNumber(){
        final UUID offenceId = randomUUID();

        ProsecutionCaseListingNumberUpdated event = ProsecutionCaseListingNumberUpdated.prosecutionCaseListingNumberUpdated()
                .withProsecutionCaseId(prosecutionCaseId)
                .withOffenceListingNumbers(singletonList(OffenceListingNumbers.offenceListingNumbers()
                        .withOffenceId(offenceId)
                        .withListingNumber(10)
                        .build()))
                .build();

        final JsonObject eventPayloadJsonObject = objectToJsonObjectConverter.convert(event);
        when(jsonEnvelope.payloadAsJsonObject()).thenReturn(eventPayloadJsonObject);
        ProsecutionCase caseInDb = ProsecutionCase.prosecutionCase()
                .withDefendants(singletonList(Defendant.defendant()
                        .withOffences(Arrays.asList(Offence.offence()
                                .withId(offenceId)
                                .build(), Offence.offence()
                                .withId(randomUUID())
                                .build()))
                        .build()))
                .build();
        final ProsecutionCaseEntity prosecutionCaseEntity = new ProsecutionCaseEntity();
        prosecutionCaseEntity.setCaseId(prosecutionCaseId);
        prosecutionCaseEntity.setPayload(objectToJsonObjectConverter.convert(caseInDb).toString());
        when(prosecutionCaseRepository.findByCaseId(prosecutionCaseId)).thenReturn(prosecutionCaseEntity);

        prosecutionCaseDefendantUpdatedEventListener.processProsecutionCaseUpdatedWithListingNumber(jsonEnvelope);

        verify(prosecutionCaseRepository).save(argumentCaptor.capture());
        final ProsecutionCaseEntity updatedProsecutionCaseEntity = argumentCaptor.getValue();
        final ProsecutionCase updatedCase = jsonObjectToObjectConverter.convert(stringToJsonObjectConverter.convert(updatedProsecutionCaseEntity.getPayload()), ProsecutionCase.class);

        assertThat(updatedCase.getDefendants().get(0).getOffences().get(0).getId(), is(offenceId));
        assertThat(updatedCase.getDefendants().get(0).getOffences().get(0).getListingNumber(), is(10));
        assertThat(updatedCase.getDefendants().get(0).getOffences().get(1).getListingNumber(), is(nullValue()));
    }

    @Test
    public void shouldUpdateViewStoreWithDecreasedListingNumber(){
        final UUID offenceId = randomUUID();

        ProsecutionCaseListingNumberDecreased event = ProsecutionCaseListingNumberDecreased.prosecutionCaseListingNumberDecreased()
                .withProsecutionCaseId(prosecutionCaseId)
                .withOffenceIds(singletonList(offenceId))
                .build();

        final JsonObject eventPayloadJsonObject = objectToJsonObjectConverter.convert(event);
        when(jsonEnvelope.payloadAsJsonObject()).thenReturn(eventPayloadJsonObject);
        ProsecutionCase caseInDb = ProsecutionCase.prosecutionCase()
                .withDefendants(singletonList(Defendant.defendant()
                        .withOffences(Arrays.asList(Offence.offence()
                                .withId(offenceId)
                                .withListingNumber(5)
                                .build(), Offence.offence()
                                .withId(randomUUID())
                                .build()))
                        .build()))
                .build();
        final ProsecutionCaseEntity prosecutionCaseEntity = new ProsecutionCaseEntity();
        prosecutionCaseEntity.setCaseId(prosecutionCaseId);
        prosecutionCaseEntity.setPayload(objectToJsonObjectConverter.convert(caseInDb).toString());
        when(prosecutionCaseRepository.findByCaseId(prosecutionCaseId)).thenReturn(prosecutionCaseEntity);

        prosecutionCaseDefendantUpdatedEventListener.processProsecutionCaseListingNumberDecreased(jsonEnvelope);

        verify(prosecutionCaseRepository).save(argumentCaptor.capture());
        final ProsecutionCaseEntity updatedProsecutionCaseEntity = argumentCaptor.getValue();
        final ProsecutionCase updatedCase = jsonObjectToObjectConverter.convert(stringToJsonObjectConverter.convert(updatedProsecutionCaseEntity.getPayload()), ProsecutionCase.class);

        assertThat(updatedCase.getDefendants().get(0).getOffences().get(0).getId(), is(offenceId));
        assertThat(updatedCase.getDefendants().get(0).getOffences().get(0).getListingNumber(), is(4));
        assertThat(updatedCase.getDefendants().get(0).getOffences().get(1).getListingNumber(), is(nullValue()));
    }

    private ProsecutionCase getProsecutionCase(final UUID prosecutionCaseId,
                                               final UUID defendantId,
                                               final UUID masterDefendantId) {
        String classOfCase = "ProsecutionCase";
        String originatingOrganisation = "originatingOrganisation";
        String removalReason = "removal Reason";
        String statementOfFacts = "Statement of facts";
        String statementOfFactsWelsh = "Statement of Facts Welsh";
        String policeOfficerRank = "1";
        return ProsecutionCase.prosecutionCase()
                .withId(prosecutionCaseId)
                .withAppealProceedingsPending(true)
                .withCaseStatus(null)
                .withBreachProceedingsPending(true)
                .withClassOfCase(classOfCase)
                .withPoliceOfficerInCase(PoliceOfficerInCase.policeOfficerInCase().withPoliceOfficerRank(policeOfficerRank).build())
                .withCaseMarkers(singletonList(Marker.marker().build()))
                .withProsecutionCaseIdentifier(ProsecutionCaseIdentifier.prosecutionCaseIdentifier().withCaseURN("123").build())
                .withDefendants(singletonList(
                        Defendant.defendant()
                                .withId(defendantId)
                                .withMasterDefendantId(masterDefendantId)
                                .withIsYouth(true)
                                .withOffences(singletonList(Offence.offence().withId(randomUUID()).build()))
                                .withPersonDefendant(PersonDefendant.personDefendant()
                                        .withPersonDetails(Person.person().withEthnicity(Ethnicity.ethnicity().build())
                                                .build())
                                        .build())
                                .build())
                )
                .withInitiationCode(InitiationCode.C)
                .withOriginatingOrganisation(originatingOrganisation)
                .withRemovalReason(removalReason)
                .withStatementOfFacts(statementOfFacts)
                .withStatementOfFactsWelsh(statementOfFactsWelsh)
                .build();
    }
}
