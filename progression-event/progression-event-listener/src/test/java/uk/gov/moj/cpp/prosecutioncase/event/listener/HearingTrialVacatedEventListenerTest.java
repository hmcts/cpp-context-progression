package uk.gov.moj.cpp.prosecutioncase.event.listener;

import static java.util.UUID.randomUUID;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.justice.services.test.utils.core.messaging.MetadataBuilderFactory.metadataWithRandomUUID;

import uk.gov.justice.core.courts.CourtCentre;
import uk.gov.justice.core.courts.Hearing;
import uk.gov.justice.progression.courts.HearingTrialVacated;
import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.common.converter.ObjectToJsonObjectConverter;
import uk.gov.justice.services.common.converter.StringToJsonObjectConverter;
import uk.gov.justice.services.test.utils.framework.api.JsonObjectConvertersFactory;
import uk.gov.moj.cpp.progression.test.matchers.BeanMatcher;
import uk.gov.moj.cpp.prosecutioncase.persistence.entity.HearingEntity;
import uk.gov.moj.cpp.prosecutioncase.persistence.repository.HearingRepository;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;


@ExtendWith(MockitoExtension.class)
public class HearingTrialVacatedEventListenerTest {

    @Captor
    private ArgumentCaptor<HearingEntity> saveHearingCaptor;

    @Mock
    private HearingRepository hearingRepository;

    @InjectMocks
    private HearingTrialVacatedEventListener hearingTrialVacatedEventListener;
    @Spy
    private JsonObjectToObjectConverter jsonObjectToObjectConverter = new JsonObjectConvertersFactory().jsonObjectToObjectConverter();

    @Spy
    private ObjectToJsonObjectConverter objectToJsonObjectConverter = new JsonObjectConvertersFactory().objectToJsonObjectConverter();


    @Test
    public void shouldSaveIsVacatedField() {

        final UUID hearingId = randomUUID();
        final UUID vacateTrialTypeId = randomUUID();
        final Hearing hearing = Hearing.hearing().withId(hearingId).withCourtCentre(CourtCentre.courtCentre().withCode("code").build()).build();
        final HearingEntity hearingEntity = new HearingEntity();
        hearingEntity.setHearingId(hearingId);
        hearingEntity.setPayload(objectToJsonObjectConverter.convert(hearing).toString());

        final HearingTrialVacated hearingTrialVacated = new HearingTrialVacated(hearingId, vacateTrialTypeId);
        when(hearingRepository.findBy(hearingId)).thenReturn(hearingEntity);

        hearingTrialVacatedEventListener.handleHearingTrialVacatedEvent(envelopeFrom(metadataWithRandomUUID("progression.event.hearing-trial-vacated"),
                objectToJsonObjectConverter.convert(hearingTrialVacated)
        ));

        verify(this.hearingRepository).save(saveHearingCaptor.capture());

        HearingEntity savedEntity = saveHearingCaptor.getValue();
        Hearing payload = jsonObjectToObjectConverter.convert(new StringToJsonObjectConverter().convert(savedEntity.getPayload()), Hearing.class);
        assertThat(saveHearingCaptor.getValue(), BeanMatcher.isBean(HearingEntity.class)
                .with(HearingEntity::getHearingId, is(hearingId)));
        assertThat(payload.getIsVacatedTrial(), is(true));

    }
}
