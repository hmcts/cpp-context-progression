package uk.gov.moj.cpp.prosecution.event.listener;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.justice.services.test.utils.core.messaging.MetadataBuilderFactory.metadataWithRandomUUID;
import static uk.gov.justice.services.test.utils.core.reflection.ReflectionUtil.setField;

import uk.gov.justice.core.courts.CourtCentre;
import uk.gov.justice.core.courts.Defendant;
import uk.gov.justice.core.courts.Hearing;
import uk.gov.justice.core.courts.HearingListingStatus;
import uk.gov.justice.core.courts.Offence;
import uk.gov.justice.core.courts.ProsecutionCase;
import uk.gov.justice.core.courts.JurisdictionType;
import uk.gov.justice.core.courts.HearingLanguage;
import uk.gov.justice.hearing.courts.HearingResulted;
import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.common.converter.ObjectToJsonObjectConverter;
import uk.gov.justice.services.common.converter.jackson.ObjectMapperProducer;
import uk.gov.moj.cpp.prosecutioncase.event.listener.HearingResultEventListener;
import uk.gov.moj.cpp.prosecutioncase.persistence.entity.HearingEntity;
import uk.gov.moj.cpp.prosecutioncase.persistence.repository.HearingRepository;

import java.util.Arrays;
import java.util.UUID;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class HearingResultEventListenerTest {

    @Mock
    private HearingRepository hearingRepository;

    @InjectMocks
    private HearingResultEventListener hearingEventEventListener;

    @Spy
    private JsonObjectToObjectConverter jsonObjectToObjectConverter;

    @Spy
    private ObjectToJsonObjectConverter objectToJsonObjectConverter;

    @Before
    public void setup() {
        setField(this.jsonObjectToObjectConverter, "objectMapper", new ObjectMapperProducer().objectMapper());
        setField(this.objectToJsonObjectConverter, "mapper", new ObjectMapperProducer().objectMapper());
    }

    @Test
    public void hearingResult() {

        final UUID hearingId = UUID.randomUUID();
        final UUID courtCentreId = UUID.randomUUID();
        final UUID prosecutionCaseId = UUID.randomUUID();
        final UUID defendantId = UUID.randomUUID();
        final UUID offenceId = UUID.randomUUID();

        final HearingResulted hearingResulted = HearingResulted.hearingResulted()
                .withHearing(Hearing.hearing()
                        .withId(hearingId)
                        .withJurisdictionType(JurisdictionType.CROWN)
                        .withHearingLanguage(HearingLanguage.ENGLISH)
                        .withCourtCentre(CourtCentre.courtCentre()
                                .withId(courtCentreId)
                                .build())
                        .withProsecutionCases(Arrays.asList(ProsecutionCase.prosecutionCase()
                                .withId(prosecutionCaseId)
                                .withDefendants(Arrays.asList(Defendant.defendant()
                                        .withId(defendantId)
                                        .withOffences(Arrays.asList(Offence.offence()
                                                .withId(offenceId)
                                                .build()))
                                        .build()))
                                .build()))

                        .build())
                .build();

        final Hearing hearing = Hearing.hearing()
                .withId(hearingId)
                .build();

        HearingEntity hearingEntity = new HearingEntity();
        hearingEntity.setHearingId(hearingId);
        hearingEntity.setPayload(objectToJsonObjectConverter.convert(hearing).toString());

        when(hearingRepository.findBy(hearingId)).thenReturn(hearingEntity);

        hearingEventEventListener.updateHearingResult(envelopeFrom(metadataWithRandomUUID("progression.event.hearing-resulted"),
                objectToJsonObjectConverter.convert(hearingResulted)));

        final ArgumentCaptor<HearingEntity> hearingEntityArgumentCaptor = ArgumentCaptor.forClass(HearingEntity.class);

        verify(this.hearingRepository).save(hearingEntityArgumentCaptor.capture());

        HearingEntity savedHearingEntity = hearingEntityArgumentCaptor.getValue();

        assertThat(savedHearingEntity.getHearingId(), equalTo(hearingId));
        assertThat(savedHearingEntity.getPayload(), notNullValue());
        assertThat(savedHearingEntity.getListingStatus(), equalTo(HearingListingStatus.HEARING_RESULTED));

    }
}
