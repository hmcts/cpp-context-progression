package uk.gov.moj.cpp.prosecutioncase.event.listener;

import static java.util.Collections.singletonList;
import static java.util.UUID.randomUUID;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.ArgumentCaptor.forClass;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.test.utils.core.reflection.ReflectionUtil.setField;

import uk.gov.justice.core.courts.Defendant;
import uk.gov.justice.core.courts.DefendantCaseOffences;
import uk.gov.justice.core.courts.InitiationCode;
import uk.gov.justice.core.courts.Marker;
import uk.gov.justice.core.courts.Offence;
import uk.gov.justice.core.courts.PoliceOfficerInCase;
import uk.gov.justice.core.courts.ProsecutionCase;
import uk.gov.justice.core.courts.ProsecutionCaseIdentifier;
import uk.gov.justice.core.courts.ProsecutionCaseOffencesUpdated;
import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.common.converter.ObjectToJsonObjectConverter;
import uk.gov.justice.services.common.converter.StringToJsonObjectConverter;
import uk.gov.justice.services.common.converter.jackson.ObjectMapperProducer;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.prosecutioncase.persistence.entity.ProsecutionCaseEntity;
import uk.gov.moj.cpp.prosecutioncase.persistence.repository.CaseDefendantHearingRepository;
import uk.gov.moj.cpp.prosecutioncase.persistence.repository.ProsecutionCaseRepository;

import java.util.Arrays;
import java.util.UUID;

import javax.json.JsonArray;
import javax.json.JsonObject;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class ProsecutionCaseOffencesUpdatedEventListenerTest {

    private final UUID id = randomUUID();
    private final UUID defendantId = randomUUID();
    private final UUID masterDefendantId = randomUUID();
    private final UUID prosecutionCaseId = randomUUID();

    @Spy
    private JsonObjectToObjectConverter jsonObjectToObjectConverter;

    @Spy
    private StringToJsonObjectConverter stringToJsonObjectConverter;

    @Spy
    private ObjectToJsonObjectConverter objectToJsonObjectConverter;

    @Mock
    private ProsecutionCaseRepository prosecutionCaseRepository;

    @Mock
    private JsonEnvelope jsonEnvelope;

    @Captor
    private ArgumentCaptor<ProsecutionCaseEntity> argumentCaptor = forClass(ProsecutionCaseEntity.class);

    @Mock
    private CaseDefendantHearingRepository caseDefendantHearingRepository;

    @InjectMocks
    private ProsecutionCaseOffencesUpdatedEventListener prosecutionCaseOffencesUpdatedEventListener;
    private UUID OffenceId11 = UUID.fromString("47e5462b-6565-4648-a905-393d62ad39e2");
    private UUID OffenceId12 = UUID.fromString("ff3c0de8-972f-44cb-accb-8028e2561fa5");

    @BeforeEach
    public void setUp() {
        setField(this.objectToJsonObjectConverter, "mapper", new ObjectMapperProducer().objectMapper());
        setField(this.jsonObjectToObjectConverter, "objectMapper", new ObjectMapperProducer().objectMapper());
    }

    @Test
    public void testProcessProsecutionCaseOffencesUpdatedWhenOffenceLegalAidStatusIsNull() {


        final ProsecutionCase prosecutionCase = getProsecutionCase();

        final DefendantCaseOffences defendantCaseOffences = getDefendantCaseOffencesWithNullOffenceLegalAidStatus();
        final ProsecutionCaseOffencesUpdated prosecutionCaseOffencesUpdated = getProsecutionCaseOffencesUpdated(defendantCaseOffences);
        final JsonObject eventPayload = objectToJsonObjectConverter.convert(prosecutionCaseOffencesUpdated);
        when(jsonEnvelope.payloadAsJsonObject()).thenReturn(eventPayload);

        final ProsecutionCaseEntity prosecutionCaseEntity = new ProsecutionCaseEntity();
        prosecutionCaseEntity.setCaseId(id);
        prosecutionCaseEntity.setPayload(objectToJsonObjectConverter.convert(prosecutionCase).toString());
        when(prosecutionCaseRepository.findByCaseId(defendantCaseOffences.getProsecutionCaseId())).thenReturn(prosecutionCaseEntity);

        prosecutionCaseOffencesUpdatedEventListener.processProsecutionCaseOffencesUpdated(jsonEnvelope);

        verify(prosecutionCaseRepository).save(argumentCaptor.capture());
        assertThat(argumentCaptor.getValue(), is(notNullValue()));
        final ProsecutionCaseEntity updatedProsecutionCaseEntity = argumentCaptor.getValue();
        final JsonObject payload =  stringToJsonObjectConverter.convert(updatedProsecutionCaseEntity.getPayload());
        final JsonArray defendantProperties = payload.getJsonArray("defendants");
        assertThat(defendantProperties.getJsonObject(0).get("isYouth").toString(), is("true"));
        assertThat(defendantProperties.getJsonObject(0).getString("id"), is(defendantId.toString()));
        assertThat(defendantProperties.getJsonObject(0).getString("masterDefendantId"), is(masterDefendantId.toString()));
        assertThat(defendantProperties.getJsonObject(0).getJsonArray("offences").size(), is(2));
        assertThat(updatedProsecutionCaseEntity.getCaseId(), is(id));
    }

    private ProsecutionCaseOffencesUpdated getProsecutionCaseOffencesUpdated(DefendantCaseOffences defendantCaseOffences) {
        return ProsecutionCaseOffencesUpdated.prosecutionCaseOffencesUpdated()
                .withDefendantCaseOffences(defendantCaseOffences)
                .build();
    }

    private DefendantCaseOffences getDefendantCaseOffencesWithNullOffenceLegalAidStatus() {
        return DefendantCaseOffences.defendantCaseOffences()
                .withProsecutionCaseId(prosecutionCaseId)
                .withDefendantId(defendantId)
                .withOffences(Arrays.asList(
                        Offence.offence().withId(OffenceId11).build(),
                        Offence.offence().withId(OffenceId12).build(),
                        Offence.offence().withId(OffenceId11).build(),
                        Offence.offence().withId(OffenceId12).build()
                ))
                .build();
    }

    private ProsecutionCase getProsecutionCase() {
        String classOfCase = "ProsecutionCase";
        String originatingOrganisation = "originatingOrganisation";
        String removalReason = "removal Reason";
        String statementOfFacts = "Statement of facts";
        String statementOfFactsWelsh = "Statement of Facts Welsh";
        String policeOfficerRank = "1";
        return ProsecutionCase.prosecutionCase()
                .withId(id)
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
                                .withOffences(Arrays.asList(
                                        Offence.offence().withId(OffenceId11).build(),
                                        Offence.offence().withId(OffenceId12).build(),
                                        Offence.offence().withId(OffenceId11).build(),
                                        Offence.offence().withId(OffenceId12).build()
                                ))
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