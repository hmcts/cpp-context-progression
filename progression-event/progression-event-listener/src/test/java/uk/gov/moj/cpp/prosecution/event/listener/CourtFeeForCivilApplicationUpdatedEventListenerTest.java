package uk.gov.moj.cpp.prosecution.event.listener;

import static javax.json.Json.createObjectBuilder;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.justice.core.courts.CourtApplication.courtApplication;
import static uk.gov.justice.core.courts.CourtApplicationPayment.courtApplicationPayment;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.justice.services.test.utils.core.messaging.MetadataBuilderFactory.metadataWithRandomUUID;
import static uk.gov.justice.services.test.utils.core.reflection.ReflectionUtil.setField;

import uk.gov.justice.core.courts.ApplicationStatus;
import uk.gov.justice.core.courts.BoxHearingRequest;
import uk.gov.justice.core.courts.CourtApplication;
import uk.gov.justice.core.courts.CourtApplicationParty;
import uk.gov.justice.core.courts.CourtApplicationPayment;
import uk.gov.justice.core.courts.CourtApplicationType;
import uk.gov.justice.core.courts.CourtFeeForCivilApplicationUpdated;
import uk.gov.justice.core.courts.CourtHearingRequest;
import uk.gov.justice.core.courts.FeeStatus;
import uk.gov.justice.core.courts.InitiateCourtApplicationProceedings;
import uk.gov.justice.core.courts.Organisation;
import uk.gov.justice.core.courts.Person;
import uk.gov.justice.core.courts.ProsecutingAuthority;
import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.common.converter.ObjectToJsonObjectConverter;
import uk.gov.justice.services.common.converter.StringToJsonObjectConverter;
import uk.gov.justice.services.common.converter.jackson.ObjectMapperProducer;
import uk.gov.justice.services.test.utils.core.random.StringGenerator;
import uk.gov.moj.cpp.prosecutioncase.event.listener.CourtFeeForCivilApplicationUpdatedEventListener;
import uk.gov.moj.cpp.prosecutioncase.persistence.entity.CourtApplicationEntity;
import uk.gov.moj.cpp.prosecutioncase.persistence.entity.InitiateCourtApplicationEntity;
import uk.gov.moj.cpp.prosecutioncase.persistence.repository.CourtApplicationRepository;
import uk.gov.moj.cpp.prosecutioncase.persistence.repository.InitiateCourtApplicationRepository;

import java.util.Arrays;
import java.util.UUID;

import javax.json.JsonObject;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class CourtFeeForCivilApplicationUpdatedEventListenerTest {

    final static private UUID APPLICATION_ID = UUID.randomUUID();
    final static private String APPLICATION_ARN = new StringGenerator().next();
    final static private String APPLICANT_FIRST_NAME = new StringGenerator().next();
    final static private String APPLICANT_LAST_NAME = new StringGenerator().next();
    final static private String APPLICANT_MIDDLE_NAME = new StringGenerator().next();
    final static private String RESPONDENTS_ORG_NAME = new StringGenerator().next();
    final static private String APPLICATION_PROSECUTOR_NAME = new StringGenerator().next();

    private static final String COURT_APPLICATION_PAYMENT = "courtApplicationPayment";

    @Mock
    private CourtApplicationRepository courtApplicationRepository;

    @InjectMocks
    private CourtFeeForCivilApplicationUpdatedEventListener listener;

    @Spy
    private JsonObjectToObjectConverter jsonObjectToObjectConverter;

    @Spy
    private ObjectToJsonObjectConverter objectToJsonObjectConverter;

    @Spy
    private StringToJsonObjectConverter stringToJsonObjectConverter;

    @Mock
    private InitiateCourtApplicationRepository initiateCourtApplicationRepository;

    @BeforeEach
    public void setup() {
        setField(this.jsonObjectToObjectConverter, "objectMapper", new ObjectMapperProducer().objectMapper());
        setField(this.objectToJsonObjectConverter, "mapper", new ObjectMapperProducer().objectMapper());
    }

    @Test
    public void shouldEditCivilApplicationFees() {
        final CourtFeeForCivilApplicationUpdated courtFeeForCivilApplicationUpdated = CourtFeeForCivilApplicationUpdated.courtFeeForCivilApplicationUpdated()
                .withApplicationId(APPLICATION_ID)
                .withCourtApplicationPayment(courtApplicationPayment()
                        .withFeeStatus(FeeStatus.NOT_APPLICABLE)
                        .withPaymentReference("Updated Fee status")
                        .withContestedFeeStatus(FeeStatus.NOT_APPLICABLE)
                        .withPaymentReference("Updated Contested fee status")
                        .build())
                .build();

        final CourtApplication courtApplication = getCourtApplication();
        final CourtApplicationEntity courtApplicationEntity = new CourtApplicationEntity();
        courtApplicationEntity.setAssignedUserId(UUID.randomUUID());
        courtApplicationEntity.setApplicationId(APPLICATION_ID);
        final JsonObject courtApplicationJson = createObjectBuilder().build();
        courtApplicationEntity.setPayload(objectToJsonObjectConverter.convert(courtFeeForCivilApplicationUpdated).toString());

        final InitiateCourtApplicationProceedings initiateCourtApplicationProceedings = getInitiateCourtApplicationProceedings(courtApplication);
        final InitiateCourtApplicationEntity initiateCourtApplicationEntity = new InitiateCourtApplicationEntity();
        initiateCourtApplicationEntity.setApplicationId(APPLICATION_ID);
        final JsonObject initiateCourtApplicationJson = objectToJsonObjectConverter.convert(initiateCourtApplicationProceedings);
        createObjectBuilder().build();
        initiateCourtApplicationEntity.setPayload("{}");

        when(stringToJsonObjectConverter.convert(courtApplicationEntity.getPayload())).thenReturn(courtApplicationJson);
        when(courtApplicationRepository.findByApplicationId(any())).thenReturn(courtApplicationEntity);
        when(stringToJsonObjectConverter.convert(initiateCourtApplicationEntity.getPayload())).thenReturn(initiateCourtApplicationJson);
        when(initiateCourtApplicationRepository.findBy(any())).thenReturn(initiateCourtApplicationEntity);

        listener.processEvent(envelopeFrom(metadataWithRandomUUID("progression.event.court-fee-for-civil-application-updated"),
                objectToJsonObjectConverter.convert(courtFeeForCivilApplicationUpdated)));

        final ArgumentCaptor<CourtApplicationEntity> argumentCaptor = ArgumentCaptor.forClass(CourtApplicationEntity.class);
        verify(this.courtApplicationRepository).save(argumentCaptor.capture());
        final CourtApplicationEntity savedEntity = argumentCaptor.getValue();

        assertThat(savedEntity.getApplicationId(), is(APPLICATION_ID));
        JsonObject courtApplicationResponse = stringToJsonObjectConverter.convert(savedEntity.getPayload());
        assertTrue(courtApplicationResponse.containsKey(COURT_APPLICATION_PAYMENT));
        final JsonObject courtApplicationPayment = courtApplicationResponse.getJsonObject(COURT_APPLICATION_PAYMENT);
        assertTrue(courtApplicationPayment.containsKey("feeStatus"));
        assertTrue(courtApplicationPayment.containsKey("contestedFeeStatus"));

        final ArgumentCaptor<InitiateCourtApplicationEntity> argCaptor = ArgumentCaptor.forClass(InitiateCourtApplicationEntity.class);
        verify(this.initiateCourtApplicationRepository).save(argCaptor.capture());
        final InitiateCourtApplicationEntity initiateCourtApplicationEntity1 = argCaptor.getValue();

        assertThat(initiateCourtApplicationEntity1.getApplicationId(), is(APPLICATION_ID));
        JsonObject initiateCourtApplicationResponse = stringToJsonObjectConverter.convert(initiateCourtApplicationEntity1.getPayload());
        JsonObject courtApplicationObj = initiateCourtApplicationResponse.getJsonObject("courtApplication");
        assertTrue(courtApplicationObj.containsKey(COURT_APPLICATION_PAYMENT));
        final JsonObject courtApplicationPayment1 = courtApplicationObj.getJsonObject(COURT_APPLICATION_PAYMENT);
        assertTrue(courtApplicationPayment1.containsKey("feeStatus"));
        assertTrue(courtApplicationPayment1.containsKey("contestedFeeStatus"));
    }


    @Test
    public void shouldEditOlderStructureCivilApplicationFees() {
        final CourtFeeForCivilApplicationUpdated courtFeeForCivilApplicationUpdated = CourtFeeForCivilApplicationUpdated.courtFeeForCivilApplicationUpdated()
                .withApplicationId(APPLICATION_ID)
                .withCourtApplicationPayment(courtApplicationPayment()
                        .withFeeStatus(FeeStatus.NOT_APPLICABLE)
                        .withPaymentReference("Updated Fee status")
                        .withContestedFeeStatus(FeeStatus.NOT_APPLICABLE)
                        .withPaymentReference("Updated Contested fee status")
                        .build())
                .build();

        final CourtApplication courtApplicationWithOldFeeStructure = courtApplication()
                .withValuesFrom(getCourtApplication())
                .withCourtApplicationPayment(courtApplicationPayment()
                        .withIsFeeExempt(true)
                        .withIsFeePaid(true)
                        .withIsFeeUndertakingAttached(true)
                        .build())
                .build();
        final CourtApplicationEntity courtApplicationEntity = new CourtApplicationEntity();
        courtApplicationEntity.setAssignedUserId(UUID.randomUUID());
        courtApplicationEntity.setApplicationId(APPLICATION_ID);
        final JsonObject courtApplicationJson = createObjectBuilder().build();
        courtApplicationEntity.setPayload(objectToJsonObjectConverter.convert(courtFeeForCivilApplicationUpdated).toString());

        final InitiateCourtApplicationProceedings initiateCourtApplicationProceedings = getInitiateCourtApplicationProceedings(courtApplicationWithOldFeeStructure);
        final InitiateCourtApplicationEntity initiateCourtApplicationEntity = new InitiateCourtApplicationEntity();
        initiateCourtApplicationEntity.setApplicationId(APPLICATION_ID);
        final JsonObject initiateCourtApplicationJson = objectToJsonObjectConverter.convert(initiateCourtApplicationProceedings);
        createObjectBuilder().build();
        initiateCourtApplicationEntity.setPayload("{}");

        when(stringToJsonObjectConverter.convert(courtApplicationEntity.getPayload())).thenReturn(courtApplicationJson);
        when(courtApplicationRepository.findByApplicationId(any())).thenReturn(courtApplicationEntity);
        when(stringToJsonObjectConverter.convert(initiateCourtApplicationEntity.getPayload())).thenReturn(initiateCourtApplicationJson);
        when(initiateCourtApplicationRepository.findBy(any())).thenReturn(initiateCourtApplicationEntity);

        listener.processEvent(envelopeFrom(metadataWithRandomUUID("progression.event.court-fee-for-civil-application-updated"),
                objectToJsonObjectConverter.convert(courtFeeForCivilApplicationUpdated)));

        final ArgumentCaptor<CourtApplicationEntity> argumentCaptor = ArgumentCaptor.forClass(CourtApplicationEntity.class);
        verify(this.courtApplicationRepository).save(argumentCaptor.capture());
        final CourtApplicationEntity savedEntity = argumentCaptor.getValue();

        assertThat(savedEntity.getApplicationId(), is(APPLICATION_ID));
        JsonObject courtApplicationResponse = stringToJsonObjectConverter.convert(savedEntity.getPayload());
        assertTrue(courtApplicationResponse.containsKey(COURT_APPLICATION_PAYMENT));
        final JsonObject courtApplicationPayment = courtApplicationResponse.getJsonObject(COURT_APPLICATION_PAYMENT);
        assertTrue(courtApplicationPayment.containsKey("feeStatus"));
        assertTrue(courtApplicationPayment.containsKey("contestedFeeStatus"));

        final ArgumentCaptor<InitiateCourtApplicationEntity> argCaptor = ArgumentCaptor.forClass(InitiateCourtApplicationEntity.class);
        verify(this.initiateCourtApplicationRepository).save(argCaptor.capture());
        final InitiateCourtApplicationEntity initiateCourtApplicationEntity1 = argCaptor.getValue();

        assertThat(initiateCourtApplicationEntity1.getApplicationId(), is(APPLICATION_ID));
        JsonObject initiateCourtApplicationResponse = stringToJsonObjectConverter.convert(initiateCourtApplicationEntity1.getPayload());
        JsonObject courtApplicationObj = initiateCourtApplicationResponse.getJsonObject("courtApplication");
        assertTrue(courtApplicationObj.containsKey(COURT_APPLICATION_PAYMENT));
        final JsonObject courtApplicationPayment1 = courtApplicationObj.getJsonObject(COURT_APPLICATION_PAYMENT);
        assertTrue(courtApplicationPayment1.containsKey("feeStatus"));
        assertTrue(courtApplicationPayment1.containsKey("contestedFeeStatus"));
    }

    private static CourtApplication getCourtApplication() {
        return courtApplication()
                .withId(APPLICATION_ID)
                .withApplicationStatus(ApplicationStatus.DRAFT)
                .withType(CourtApplicationType.courtApplicationType().withType("Test").build())
                .withApplicationReference(APPLICATION_ARN)
                .withCourtApplicationPayment(courtApplicationPayment()
                        .withFeeStatus(FeeStatus.REDUCED)
                        .withPaymentReference("Initial")
                        .build())
                .withApplicant(CourtApplicationParty.courtApplicationParty()
                        .withPersonDetails(Person.person()
                                .withFirstName(APPLICANT_FIRST_NAME)
                                .withLastName(APPLICANT_LAST_NAME)
                                .withMiddleName(APPLICANT_MIDDLE_NAME)
                                .build())
                        .build())
                .withRespondents(Arrays.asList(CourtApplicationParty.courtApplicationParty()
                        .withProsecutingAuthority(ProsecutingAuthority.prosecutingAuthority()
                                .withName(APPLICATION_PROSECUTOR_NAME)
                                .build())
                        .build(), CourtApplicationParty.courtApplicationParty()
                        .withOrganisation(Organisation.organisation()
                                .withName(RESPONDENTS_ORG_NAME)
                                .build())
                        .build()))
                .build();
    }

    private static InitiateCourtApplicationProceedings getInitiateCourtApplicationProceedings(CourtApplication courtApplication) {
        return InitiateCourtApplicationProceedings.initiateCourtApplicationProceedings()
                .withCourtApplication(courtApplication)
                .withBoxHearing(BoxHearingRequest.boxHearingRequest().build())
                .withCourtHearing(CourtHearingRequest.courtHearingRequest().build())
                .withSummonsApprovalRequired(false)
                .build();
    }

}
