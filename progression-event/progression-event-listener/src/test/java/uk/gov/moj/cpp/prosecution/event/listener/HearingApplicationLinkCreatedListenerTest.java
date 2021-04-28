package uk.gov.moj.cpp.prosecution.event.listener;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.test.utils.core.reflection.ReflectionUtil.setField;

import uk.gov.justice.core.courts.CourtApplication;
import uk.gov.justice.core.courts.Hearing;
import uk.gov.justice.core.courts.HearingApplicationLinkCreated;
import uk.gov.justice.core.courts.HearingListingStatus;
import uk.gov.justice.core.courts.JudicialResult;
import uk.gov.justice.progression.courts.HearingDeletedForCourtApplication;
import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.common.converter.ListToJsonArrayConverter;
import uk.gov.justice.services.common.converter.ObjectToJsonObjectConverter;
import uk.gov.justice.services.common.converter.StringToJsonObjectConverter;
import uk.gov.justice.services.common.converter.jackson.ObjectMapperProducer;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.prosecutioncase.event.listener.HearingApplicationLinkCreatedListener;
import uk.gov.moj.cpp.prosecutioncase.persistence.entity.HearingApplicationEntity;
import uk.gov.moj.cpp.prosecutioncase.persistence.entity.HearingEntity;
import uk.gov.moj.cpp.prosecutioncase.persistence.repository.HearingApplicationRepository;
import uk.gov.moj.cpp.prosecutioncase.persistence.repository.HearingRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import javax.json.Json;
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
public class HearingApplicationLinkCreatedListenerTest {

    @Mock
    private JsonObjectToObjectConverter jsonObjectToObjectConverter;

    @Spy
    private ObjectToJsonObjectConverter objectToJsonObjectConverter;

    @Mock
    private HearingApplicationRepository repository;

    @Mock
    private JsonEnvelope envelope;

    @Captor
    private ArgumentCaptor<HearingApplicationEntity> argumentCaptor;

    @Mock
    private JsonObject payload;

    @InjectMocks
    private HearingApplicationLinkCreatedListener eventListener;

    @Spy
    private ListToJsonArrayConverter jsonConverter;

    @Mock
    private HearingRepository hearingRepository;

    private static final UUID HEARING_ID = UUID.randomUUID();
    private static final UUID APPLICATION_ID = UUID.randomUUID();

    @Before
    public void initMocks() {

        setField(this.jsonConverter, "mapper",
                new ObjectMapperProducer().objectMapper());
        setField(this.jsonConverter, "stringToJsonObjectConverter",
                new StringToJsonObjectConverter());
        setField(this.objectToJsonObjectConverter, "mapper", new ObjectMapperProducer().objectMapper());
    }

    @Test
    public void shouldHandleHearingApplicationLinkCreatedEvent() {

        final Hearing hearing = Hearing.hearing().withId(HEARING_ID).build();

        HearingApplicationLinkCreated hearingApplicationLinkCreated
                = HearingApplicationLinkCreated.hearingApplicationLinkCreated()
                .withApplicationId(APPLICATION_ID)
                .withHearing(hearing)
                .build();
        final HearingEntity hearingEntity = new HearingEntity();
        hearingEntity.setHearingId(HEARING_ID);
        hearingEntity.setPayload(Json.createObjectBuilder().build().toString());
        hearingEntity.setListingStatus(HearingListingStatus.HEARING_INITIALISED);

        when(envelope.payloadAsJsonObject()).thenReturn(payload);
        when(jsonObjectToObjectConverter.convert(payload, HearingApplicationLinkCreated.class))
                .thenReturn(hearingApplicationLinkCreated);
        when(hearingRepository.findBy(HEARING_ID)).thenReturn(hearingEntity);

        eventListener.process(envelope);

        verify(repository).save(argumentCaptor.capture());
    }

    @Test
    public void shouldRemoveNowsSpecificJudicialResultsBeforeSaving() {

        List<JudicialResult> judicialResults = new ArrayList<>();
        judicialResults.add(JudicialResult.judicialResult().withLabel("PublishedForNowsTrue").withPublishedForNows(Boolean.TRUE).build());
        judicialResults.add(JudicialResult.judicialResult().withLabel("PublishedForNowsFalse").withPublishedForNows(Boolean.FALSE).build());

        final Hearing hearing = Hearing.hearing().withCourtApplications(getCourtApplications(judicialResults)).withId(HEARING_ID).build();

        HearingApplicationLinkCreated hearingApplicationLinkCreated
                = HearingApplicationLinkCreated.hearingApplicationLinkCreated()
                .withApplicationId(APPLICATION_ID)
                .withHearing(hearing)
                .build();
        final HearingEntity hearingEntity = new HearingEntity();
        hearingEntity.setHearingId(HEARING_ID);
        hearingEntity.setPayload(Json.createObjectBuilder().build().toString());
        hearingEntity.setListingStatus(HearingListingStatus.HEARING_INITIALISED);

        when(envelope.payloadAsJsonObject()).thenReturn(payload);
        when(jsonObjectToObjectConverter.convert(payload, HearingApplicationLinkCreated.class))
                .thenReturn(hearingApplicationLinkCreated);

        eventListener.process(envelope);
        verify(repository).save(argumentCaptor.capture());

        assertThat(argumentCaptor.getValue().getHearing().getPayload().contains("PublishedForNowsTrue"), is(false));
        assertThat(argumentCaptor.getValue().getHearing().getPayload().contains("PublishedForNowsFalse"), is(true));
    }

    @Test
    public void shouldDeleteHearingForCourtApplication() {

        HearingDeletedForCourtApplication hearingDeletedForCourtApplication
                = HearingDeletedForCourtApplication.hearingDeletedForCourtApplication()
                .withCourtApplicationId(APPLICATION_ID)
                .withHearingId(HEARING_ID)
                .build();

        when(envelope.payloadAsJsonObject()).thenReturn(payload);
        when(jsonObjectToObjectConverter.convert(payload, HearingDeletedForCourtApplication.class))
                .thenReturn(hearingDeletedForCourtApplication);

        eventListener.processHearingDeletedForCourtApplicationEvent(envelope);
        ArgumentCaptor<UUID> hearingIdArgumentCaptor = ArgumentCaptor.forClass(UUID.class);
        ArgumentCaptor<UUID> courtApplicationIdArgumentCaptor = ArgumentCaptor.forClass(UUID.class);
        verify(repository, times(1)).removeByHearingIdAndCourtApplicationId(hearingIdArgumentCaptor.capture(), courtApplicationIdArgumentCaptor.capture());

        assertThat(hearingIdArgumentCaptor.getValue(), is(HEARING_ID));
        assertThat(courtApplicationIdArgumentCaptor.getValue(), is(APPLICATION_ID));
    }

    private List<CourtApplication> getCourtApplications(final List<JudicialResult> judicialResults) {
        List<CourtApplication> courtApplications = new ArrayList<>();
        courtApplications.add(CourtApplication.courtApplication()
                .withJudicialResults(judicialResults)
                .build());

        return courtApplications;
    }
}
