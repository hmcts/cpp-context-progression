package uk.gov.moj.cpp.application.event.listener;

import static uk.gov.justice.services.messaging.JsonObjects.createObjectBuilder;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.justice.core.courts.ApplicationRepOrderUpdatedForHearing.applicationRepOrderUpdatedForHearing;
import static uk.gov.justice.services.test.utils.core.reflection.ReflectionUtil.setField;

import uk.gov.justice.core.courts.ApplicationRepOrderUpdatedForHearing;
import uk.gov.justice.core.courts.AssociatedDefenceOrganisation;
import uk.gov.justice.core.courts.CourtApplication;
import uk.gov.justice.core.courts.CourtApplicationParty;
import uk.gov.justice.core.courts.DefenceOrganisation;
import uk.gov.justice.core.courts.FundingType;
import uk.gov.justice.core.courts.Hearing;
import uk.gov.justice.core.courts.Organisation;
import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.common.converter.ListToJsonArrayConverter;
import uk.gov.justice.services.common.converter.ObjectToJsonObjectConverter;
import uk.gov.justice.services.common.converter.StringToJsonObjectConverter;
import uk.gov.justice.services.common.converter.jackson.ObjectMapperProducer;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.messaging.Metadata;
import uk.gov.justice.services.test.utils.framework.api.JsonObjectConvertersFactory;
import uk.gov.moj.cpp.prosecutioncase.persistence.entity.HearingEntity;
import uk.gov.moj.cpp.prosecutioncase.persistence.repository.HearingRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import javax.json.JsonObject;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class ApplicationRepOrderUpdatedForHearingListenerTest {

    @Mock
    private JsonObjectToObjectConverter jsonObjectToObjectConverter;

    @Mock
    private ObjectToJsonObjectConverter objectToJsonObjectConverter;

    @Mock
    private StringToJsonObjectConverter stringToJsonObjectConverter;

    @Mock
    private JsonEnvelope envelope;

    @Mock
    private HearingRepository hearingRepository;

    @Mock
    private JsonObject jsonObject;

    @Mock
    private JsonObject payload;

    @Mock
    private Metadata metadata;

    @InjectMocks
    private ApplicationRepOrderUpdatedForHearingEventListener eventListener;

    @Mock
    private HearingEntity hearingEntity;

    @Spy
    private ListToJsonArrayConverter<?> jsonConverter;

    @BeforeEach
    public void initMocks() {
        setField(this.jsonConverter, "mapper", new ObjectMapperProducer().objectMapper());
        setField(this.jsonConverter, "stringToJsonObjectConverter", new JsonObjectConvertersFactory().stringToJsonObjectConverter());
    }

    @Test
    void testUpdateApplicationLaaReferenceForHearing() {
        // Mock data
        final UUID hearingId = UUID.randomUUID();
        final UUID applicationId = UUID.randomUUID();
        final UUID subjectId = UUID.randomUUID();
        final AssociatedDefenceOrganisation associatedDefenceOrganisation = AssociatedDefenceOrganisation.associatedDefenceOrganisation()
                .withDefenceOrganisation(DefenceOrganisation.defenceOrganisation()
                        .withLaaContractNumber("LAA123")
                        .withOrganisation(Organisation.organisation()
                                .withName("Org1")
                                .build())
                        .build())
                .withApplicationReference("ABC1234")
                .withAssociationStartDate(LocalDate.now())
                .withAssociationEndDate(LocalDate.now().plusYears(1))
                .withFundingType(FundingType.REPRESENTATION_ORDER)
                .withIsAssociatedByLAA(true)
                .build();
        final ApplicationRepOrderUpdatedForHearing applicationRepOrderUpdatedForHearing = applicationRepOrderUpdatedForHearing()
                .withApplicationId(applicationId)
                .withSubjectId(subjectId)
                .withHearingId(hearingId)
                .withAssociatedDefenceOrganisation(associatedDefenceOrganisation)
                .build();

        Hearing hearing = Hearing.hearing()
                .withId(hearingId)
                .withCourtApplications(List.of(getCourtApplication(applicationId, subjectId, associatedDefenceOrganisation)))
                .build();

        final JsonObject updatedJsonObject = mock(JsonObject.class);
        final JsonObject entityPayload = createObjectBuilder().build();
        when(hearingEntity.getPayload()).thenReturn(entityPayload.toString());
        when(stringToJsonObjectConverter.convert(hearingEntity.getPayload())).thenReturn(entityPayload);
        when(jsonObjectToObjectConverter.convert(entityPayload, Hearing.class)).thenReturn(hearing);
        when(hearingRepository.findBy(hearingId)).thenReturn(hearingEntity);



        when(envelope.payloadAsJsonObject()).thenReturn(payload);
        when(jsonObjectToObjectConverter.convert(payload, ApplicationRepOrderUpdatedForHearing.class)).thenReturn(applicationRepOrderUpdatedForHearing);
        when(objectToJsonObjectConverter.convert(any(Hearing.class))).thenReturn(updatedJsonObject);

        eventListener.processApplicationDefenceOrganisationChanged(envelope);

        verify(hearingRepository).save(hearingEntity);
        verify(hearingEntity).setPayload(updatedJsonObject.toString());
    }

    @Test
    void testNoInteractionUpdateApplicationLaaReferenceForHearing() {
        // Mock data
        final UUID hearingId = UUID.randomUUID();
        final UUID applicationId = UUID.randomUUID();
        final UUID subjectId = UUID.randomUUID();
        final AssociatedDefenceOrganisation associatedDefenceOrganisation = AssociatedDefenceOrganisation.associatedDefenceOrganisation()
                .withDefenceOrganisation(DefenceOrganisation.defenceOrganisation()
                        .withLaaContractNumber("LAA123")
                        .withOrganisation(Organisation.organisation()
                                .withName("Org1")
                                .build())
                        .build())
                .withApplicationReference("ABC1234")
                .withAssociationStartDate(LocalDate.now())
                .withAssociationEndDate(LocalDate.now().plusYears(1))
                .withFundingType(FundingType.REPRESENTATION_ORDER)
                .withIsAssociatedByLAA(true)
                .build();
        final ApplicationRepOrderUpdatedForHearing applicationRepOrderUpdatedForHearing = applicationRepOrderUpdatedForHearing()
                .withHearingId(hearingId)
                .withApplicationId(applicationId)
                .withSubjectId(subjectId)
                .withAssociatedDefenceOrganisation(associatedDefenceOrganisation)
                .build();
        when(hearingRepository.findBy(hearingId)).thenReturn(null);
        when(envelope.payloadAsJsonObject()).thenReturn(payload);
        when(jsonObjectToObjectConverter.convert(payload, ApplicationRepOrderUpdatedForHearing.class)).thenReturn(applicationRepOrderUpdatedForHearing);

        eventListener.processApplicationDefenceOrganisationChanged(envelope);

        verify(hearingRepository, never()).save(hearingEntity);
    }


    private CourtApplication getCourtApplication(final UUID applicationId, final UUID subjectId, final AssociatedDefenceOrganisation associatedDefenceOrganisation) {
        return CourtApplication.courtApplication()
                .withId(applicationId)
                .withSubject(CourtApplicationParty.courtApplicationParty()
                        .withId(subjectId)
                        .withAssociatedDefenceOrganisation(associatedDefenceOrganisation)
                        .build())
                .build();
    }
}
