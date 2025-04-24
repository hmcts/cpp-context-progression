package uk.gov.moj.cpp.progression.query;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static java.util.UUID.randomUUID;
import static javax.json.Json.createObjectBuilder;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.justice.services.test.utils.core.messaging.MetadataBuilderFactory.metadataWithRandomUUIDAndName;
import static uk.gov.justice.services.test.utils.core.random.RandomGenerator.string;
import static uk.gov.justice.services.test.utils.core.reflection.ReflectionUtil.setField;

import uk.gov.justice.core.courts.CourtApplication;
import uk.gov.justice.core.courts.CourtApplicationCase;
import uk.gov.justice.core.courts.CourtApplicationParty;
import uk.gov.justice.core.courts.CourtOrder;
import uk.gov.justice.core.courts.CourtOrderOffence;
import uk.gov.justice.core.courts.Hearing;
import uk.gov.justice.core.courts.HearingType;
import uk.gov.justice.core.courts.MasterDefendant;
import uk.gov.justice.core.courts.Offence;
import uk.gov.justice.core.courts.Person;
import uk.gov.justice.core.courts.PersonDefendant;
import uk.gov.justice.core.courts.ProsecutionCaseIdentifier;
import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.common.converter.ObjectToJsonObjectConverter;
import uk.gov.justice.services.common.converter.StringToJsonObjectConverter;
import uk.gov.justice.services.common.converter.jackson.ObjectMapperProducer;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.prosecutioncase.persistence.entity.HearingEntity;
import uk.gov.moj.cpp.prosecutioncase.persistence.entity.ProsecutionCaseEntity;
import uk.gov.moj.cpp.prosecutioncase.persistence.repository.HearingRepository;
import uk.gov.moj.cpp.prosecutioncase.persistence.repository.ProsecutionCaseRepository;


import java.util.UUID;

import javax.json.JsonArray;
import javax.json.JsonObject;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ApplicationHearingQueryViewTest {

    @Mock
    private HearingRepository hearingRepository;

    @Mock
    private ProsecutionCaseRepository prosecutionCaseRepository;

    @Spy
    private StringToJsonObjectConverter stringToJsonObjectConverter;

    @Spy
    private JsonObjectToObjectConverter jsonObjectToObjectConverter;

    @Spy
    private ObjectToJsonObjectConverter objectToJsonObjectConverter;

    @InjectMocks
    private ApplicationHearingQueryView applicationHearingQueryView;

    @BeforeEach
    void setUp() {
        setField(this.jsonObjectToObjectConverter, "objectMapper", new ObjectMapperProducer().objectMapper());
        setField(this.objectToJsonObjectConverter, "mapper", new ObjectMapperProducer().objectMapper());
    }

    @ParameterizedTest
    @ValueSource(booleans =  {true, false})
    void shouldGetApplicationHearingCaseDetailsWhenCourtApplicationCaseIsPresent(final Boolean isBoxHearing) {
        final UUID hearingId = randomUUID();
        final UUID applicationId = randomUUID();
        final UUID caseId1 = randomUUID();
        final UUID caseId2 = randomUUID();
        final UUID masterDefendantId = randomUUID();
        final String defendantName = string(6).next();
        final String defendantLastName = string(6).next();
        final String caseStatus1 = "ACTIVE";
        final String caseStatus2 = "INACTIVE";
        final String caseURN1 = string(12).next();
        final String caseURN2 = string(12).next();
        final JsonEnvelope jsonEnvelope = envelopeFrom(metadataWithRandomUUIDAndName(), createObjectBuilder().add("hearingId",
                hearingId.toString()).add("applicationId", applicationId.toString()));
        final HearingType hearingType = HearingType.hearingType()
                .withId(randomUUID())
                .withDescription("application")
                .build();
        final Hearing hearing = Hearing.hearing()
                .withType(hearingType)
                .withIsBoxHearing(isBoxHearing)
                .withCourtApplications(singletonList(CourtApplication.courtApplication()
                        .withId(applicationId)
                        .withSubject(CourtApplicationParty.courtApplicationParty()
                                .withMasterDefendant(MasterDefendant.masterDefendant()
                                        .withMasterDefendantId(masterDefendantId)
                                        .withPersonDefendant(PersonDefendant.personDefendant().withPersonDetails(
                                                Person.person().withFirstName(defendantName).withLastName(defendantLastName).build()).build())
                                        .build())
                                .build())
                        .withCourtApplicationCases(asList(CourtApplicationCase.courtApplicationCase()
                                        .withProsecutionCaseId(caseId1)
                                        .withCaseStatus(caseStatus1)
                                        .withProsecutionCaseIdentifier(ProsecutionCaseIdentifier.prosecutionCaseIdentifier().withCaseURN(caseURN1).build())
                                        .build(),
                                CourtApplicationCase.courtApplicationCase()
                                        .withProsecutionCaseId(caseId2)
                                        .withCaseStatus(caseStatus2)
                                        .withProsecutionCaseIdentifier(ProsecutionCaseIdentifier.prosecutionCaseIdentifier().withProsecutionAuthorityReference(caseURN2).build())
                                        .build())
                        )
                        .build()))
                .build();
        final HearingEntity hearingEntity = new HearingEntity();
        hearingEntity.setHearingId(hearingId);
        hearingEntity.setPayload(objectToJsonObjectConverter.convert(hearing).toString());
        when(hearingRepository.findBy(hearingId)).thenReturn(hearingEntity);

        final JsonObject response = applicationHearingQueryView.getApplicationHearingCaseDetails(jsonEnvelope).payloadAsJsonObject();

        assertThat(response.getString("hearingId"), is(hearingId.toString()));
        assertThat(response.getString("applicationId"), is(applicationId.toString()));
        assertThat(response.getJsonObject("hearingType"), is(objectToJsonObjectConverter.convert(hearingType)));
        assertThat(response.getString("masterDefendantId"), is(masterDefendantId.toString()));
        assertThat(response.getString("masterDefendantName"), is(defendantName + " "+ defendantLastName));
        assertThat(response.getBoolean("isBoxHearing"), is(isBoxHearing));

        final JsonArray courtApplicationCasesSummary = response.getJsonArray("courtApplicationCasesSummary");
        assertThat(courtApplicationCasesSummary.size(), is(2));
        final JsonObject courtApplicationCase1Summary = courtApplicationCasesSummary.get(0).asJsonObject();
        assertThat(courtApplicationCase1Summary.getString("caseId"), is(caseId1.toString()));
        assertThat(courtApplicationCase1Summary.getString("caseURN"), is(caseURN1));
        assertThat(courtApplicationCase1Summary.getString("caseStatus"), is(caseStatus1));
        final JsonObject courtApplicationCase2Summary = courtApplicationCasesSummary.get(1).asJsonObject();
        assertThat(courtApplicationCase2Summary.getString("caseId"), is(caseId2.toString()));
        assertThat(courtApplicationCase2Summary.getString("caseURN"), is(caseURN2));
        assertThat(courtApplicationCase2Summary.getString("caseStatus"), is(caseStatus2));

        assertThat(response.containsKey("courtOrderCasesSummary"), is(false));
    }

    @Test
    void shouldGetApplicationHearingCaseDetailsWhenCourtOrderIsPresent() {
        final UUID hearingId = randomUUID();
        final UUID applicationId = randomUUID();
        final UUID caseId1 = randomUUID();
        final UUID caseId2 = randomUUID();
        final UUID masterDefendantId = randomUUID();
        final String defendantName = string(6).next();
        final String defendantLastName = string(6).next();
        final String caseStatus1 = "ACTIVE";
        final String caseStatus2 = "INACTIVE";
        final String caseURN1 = string(12).next();
        final String caseURN2 = string(12).next();
        final JsonEnvelope jsonEnvelope = envelopeFrom(metadataWithRandomUUIDAndName(), createObjectBuilder().add("hearingId",
                hearingId.toString()).add("applicationId", applicationId.toString()));
        final HearingType hearingType = HearingType.hearingType()
                .withId(randomUUID())
                .withDescription("application")
                .build();
        final Hearing hearing =Hearing.hearing()
                .withType(hearingType)
                .withCourtApplications(singletonList(CourtApplication.courtApplication()
                        .withId(applicationId)
                        .withSubject(CourtApplicationParty.courtApplicationParty()
                                .withMasterDefendant(MasterDefendant.masterDefendant()
                                        .withMasterDefendantId(masterDefendantId)
                                        .withPersonDefendant(PersonDefendant.personDefendant().withPersonDetails(
                                                Person.person().withFirstName(defendantName).withLastName(defendantLastName).build()).build())
                                        .build())
                                .build())
                        .withCourtOrder(CourtOrder.courtOrder().withCourtOrderOffences(asList(
                                CourtOrderOffence.courtOrderOffence()
                                        .withOffence(Offence.offence().withId(randomUUID()).build())
                                        .withProsecutionCaseId(caseId1)
                                        .withProsecutionCaseIdentifier(ProsecutionCaseIdentifier.prosecutionCaseIdentifier().withCaseURN(caseURN1).build())
                                        .build(),
                                CourtOrderOffence.courtOrderOffence()
                                        .withOffence(Offence.offence().withId(randomUUID()).build())
                                        .withProsecutionCaseId(caseId1)
                                        .withProsecutionCaseIdentifier(ProsecutionCaseIdentifier.prosecutionCaseIdentifier().withCaseURN(caseURN1).build())
                                        .build(),
                                CourtOrderOffence.courtOrderOffence()
                                        .withOffence(Offence.offence().withId(randomUUID()).build())
                                        .withProsecutionCaseId(caseId2)
                                        .withProsecutionCaseIdentifier(ProsecutionCaseIdentifier.prosecutionCaseIdentifier().withProsecutionAuthorityReference(caseURN2).build())
                                        .build()))
                                .build())
                        .build()))
                .build();
        final HearingEntity hearingEntity = new HearingEntity();
        hearingEntity.setHearingId(hearingId);
        hearingEntity.setPayload(objectToJsonObjectConverter.convert(hearing).toString());
        when(hearingRepository.findBy(hearingId)).thenReturn(hearingEntity);
        final ProsecutionCaseEntity prosecutionCaseEntity1 = new ProsecutionCaseEntity();
        prosecutionCaseEntity1.setPayload("{\"caseStatus\":\""+caseStatus1+"\"}");
        final ProsecutionCaseEntity prosecutionCaseEntity2 = new ProsecutionCaseEntity();
        prosecutionCaseEntity2.setPayload("{\"caseStatus\":\""+caseStatus2+"\"}");
        when(prosecutionCaseRepository.findByCaseId(caseId1)).thenReturn(prosecutionCaseEntity1);
        when(prosecutionCaseRepository.findByCaseId(caseId2)).thenReturn(prosecutionCaseEntity2);

        final JsonObject response = applicationHearingQueryView.getApplicationHearingCaseDetails(jsonEnvelope).payloadAsJsonObject();

        assertThat(response.getString("hearingId"), is(hearingId.toString()));
        assertThat(response.getString("applicationId"), is(applicationId.toString()));
        assertThat(response.getJsonObject("hearingType"), is(objectToJsonObjectConverter.convert(hearingType)));
        assertThat(response.getString("masterDefendantId"), is(masterDefendantId.toString()));
        assertThat(response.getString("masterDefendantName"), is(defendantName + " "+ defendantLastName));
        assertThat(response.containsKey("isBoxHearing"), is(false));

        final JsonArray courtOrderCasesSummary = response.getJsonArray("courtOrderCasesSummary");
        assertThat(courtOrderCasesSummary.size(), is(2));
        final JsonObject courtOrderCase1Summary = courtOrderCasesSummary.get(0).asJsonObject();
        assertThat(courtOrderCase1Summary.getString("caseId"), is(caseId1.toString()));
        assertThat(courtOrderCase1Summary.getString("caseURN"), is(caseURN1));
        assertThat(courtOrderCase1Summary.getString("caseStatus"), is(caseStatus1));
        final JsonObject courtOrderCase2Summary = courtOrderCasesSummary.get(1).asJsonObject();
        assertThat(courtOrderCase2Summary.getString("caseId"), is(caseId2.toString()));
        assertThat(courtOrderCase2Summary.getString("caseURN"), is(caseURN2));
        assertThat(courtOrderCase2Summary.getString("caseStatus"), is(caseStatus2));

        assertThat(response.containsKey("courtApplicationCasesSummary"), is(false));

        verify(prosecutionCaseRepository).findByCaseId(caseId1);
        verify(prosecutionCaseRepository).findByCaseId(caseId2);
    }
}