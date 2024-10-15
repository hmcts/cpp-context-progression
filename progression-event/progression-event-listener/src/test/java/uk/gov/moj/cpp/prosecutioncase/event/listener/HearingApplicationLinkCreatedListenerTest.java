package uk.gov.moj.cpp.prosecutioncase.event.listener;

import static java.util.Collections.singletonList;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.justice.core.courts.CourtApplication.courtApplication;
import static uk.gov.justice.core.courts.CourtApplicationCase.courtApplicationCase;
import static uk.gov.justice.core.courts.HearingApplicationLinkCreated.hearingApplicationLinkCreated;

import uk.gov.justice.core.courts.CourtApplication;
import uk.gov.justice.core.courts.Hearing;
import uk.gov.justice.core.courts.HearingApplicationLinkCreated;
import uk.gov.justice.core.courts.HearingListingStatus;
import uk.gov.justice.core.courts.JudicialResult;
import uk.gov.justice.core.courts.ProsecutionCase;
import uk.gov.justice.progression.courts.HearingDeletedForCourtApplication;
import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.common.converter.ObjectToJsonObjectConverter;
import uk.gov.justice.services.common.converter.StringToJsonObjectConverter;
import uk.gov.justice.services.common.converter.jackson.ObjectMapperProducer;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.prosecutioncase.persistence.entity.HearingApplicationEntity;
import uk.gov.moj.cpp.prosecutioncase.persistence.entity.HearingEntity;
import uk.gov.moj.cpp.prosecutioncase.persistence.repository.HearingApplicationRepository;
import uk.gov.moj.cpp.prosecutioncase.persistence.repository.HearingRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import javax.json.Json;
import javax.json.JsonObject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class HearingApplicationLinkCreatedListenerTest {

    public static final String PROSECUTION_CASES = "prosecutionCases";
    public static final String ID = "id";
    public static final String COURT_APPLICATIONS = "courtApplications";
    public static final String COURT_APPLICATION_CASES = "courtApplicationCases";
    public static final String PROSECUTION_CASE_ID = "prosecutionCaseId";
    public static final String JUDICIAL_RESULTS = "judicialResults";
    @Spy
    private final JsonObjectToObjectConverter jsonObjectToObjectConverter = new JsonObjectToObjectConverter(new ObjectMapperProducer().objectMapper());

    @Spy
    private final ObjectToJsonObjectConverter objectToJsonObjectConverter = new ObjectToJsonObjectConverter(new ObjectMapperProducer().objectMapper());

    @Spy
    private StringToJsonObjectConverter stringToJsonObjectConverter;

    @Mock
    private HearingApplicationRepository repository;

    @Mock
    private JsonEnvelope envelope;

    @Captor
    private ArgumentCaptor<HearingApplicationEntity> argumentCaptor;


    @InjectMocks
    private HearingApplicationLinkCreatedListener eventListener;

    @Mock
    private HearingRepository hearingRepository;

    private static final UUID HEARING_ID = UUID.randomUUID();
    private static final UUID HEARING_ID2 = UUID.randomUUID();
    private static final UUID APPLICATION_ID = UUID.randomUUID();
    private static final UUID CASE_ID = UUID.randomUUID();
    private static final UUID CASE_ID2 = UUID.randomUUID();

    @Test
    public void shouldHandleHearingApplicationLinkCreatedEventWithExistingHearing() {
        final Hearing hearingFromDb = Hearing.hearing()
                .withId(HEARING_ID)
                .withProsecutionCases(singletonList(ProsecutionCase.prosecutionCase()
                        .withId(CASE_ID)
                        .build()))
                .build();

        final Hearing hearing = Hearing.hearing().withId(HEARING_ID)
                .withCourtApplications(singletonList(courtApplication()
                        .withId(APPLICATION_ID)
                        .withCourtApplicationCases(singletonList(courtApplicationCase()
                                .withProsecutionCaseId(CASE_ID).build())).build()))
                .build();

        HearingApplicationLinkCreated hearingApplicationLinkCreated
                = hearingApplicationLinkCreated()
                .withApplicationId(APPLICATION_ID)
                .withHearing(hearing)
                .build();
        final HearingEntity hearingEntity = new HearingEntity();
        hearingEntity.setHearingId(HEARING_ID);
        hearingEntity.setPayload(objectToJsonObjectConverter.convert(hearingFromDb).toString());
        hearingEntity.setListingStatus(HearingListingStatus.HEARING_INITIALISED);

        final JsonObject payload = objectToJsonObjectConverter.convert(hearingApplicationLinkCreated);
        when(envelope.payloadAsJsonObject()).thenReturn(payload);
        when(hearingRepository.findBy(HEARING_ID)).thenReturn(hearingEntity);

        eventListener.process(envelope);

        verify(repository).save(argumentCaptor.capture());
        final HearingApplicationEntity hearingApplicationEntity = argumentCaptor.getValue();
        assertThat(hearingApplicationEntity.getHearing().getHearingId(), is(HEARING_ID));
        final JsonObject payloadJson = stringToJsonObjectConverter.convert(hearingApplicationEntity.getHearing().getPayload());
        assertThat(payloadJson.getJsonArray(PROSECUTION_CASES).getJsonObject(0).getString(ID), is(CASE_ID.toString()));
        assertThat(payloadJson.getJsonArray(COURT_APPLICATIONS).getJsonObject(0).getString(ID), is(APPLICATION_ID.toString()));
        assertThat(payloadJson.getJsonArray(COURT_APPLICATIONS).getJsonObject(0).getJsonArray(COURT_APPLICATION_CASES).getJsonObject(0).getString(PROSECUTION_CASE_ID), is(CASE_ID.toString()));
    }


    @Test
    public void shouldHandleHearingApplicationLinkCreatedEventWithExistingHearingAndExistingCase() {
        final Hearing hearingFromDb = Hearing.hearing()
                .withId(HEARING_ID)
                .withProsecutionCases(singletonList(ProsecutionCase.prosecutionCase()
                        .withId(CASE_ID2)
                        .build()))
                .build();

        final Hearing hearing = Hearing.hearing().withId(HEARING_ID)
                .withCourtApplications(singletonList(courtApplication()
                        .withId(APPLICATION_ID)
                        .withCourtApplicationCases(singletonList(courtApplicationCase()
                                .withProsecutionCaseId(CASE_ID2).build())).build()))
                .withProsecutionCases(singletonList(ProsecutionCase.prosecutionCase()
                        .withId(CASE_ID2)
                        .build()))
                .build();

        HearingApplicationLinkCreated hearingApplicationLinkCreated
                = hearingApplicationLinkCreated()
                .withApplicationId(APPLICATION_ID)
                .withHearing(hearing)
                .build();
        final HearingEntity hearingEntity = new HearingEntity();
        hearingEntity.setHearingId(HEARING_ID);
        hearingEntity.setPayload(objectToJsonObjectConverter.convert(hearingFromDb).toString());
        hearingEntity.setListingStatus(HearingListingStatus.HEARING_INITIALISED);

        final JsonObject payload = objectToJsonObjectConverter.convert(hearingApplicationLinkCreated);
        when(envelope.payloadAsJsonObject()).thenReturn(payload);
        when(hearingRepository.findBy(HEARING_ID)).thenReturn(hearingEntity);

        eventListener.process(envelope);

        verify(repository).save(argumentCaptor.capture());
        final HearingApplicationEntity hearingApplicationEntity = argumentCaptor.getValue();
        assertThat(hearingApplicationEntity.getHearing().getHearingId(), is(HEARING_ID));
        final JsonObject payloadJson = stringToJsonObjectConverter.convert(hearingApplicationEntity.getHearing().getPayload());
        assertThat(payloadJson.getJsonArray(PROSECUTION_CASES).getJsonObject(0).getString(ID), is(CASE_ID2.toString()));
        assertThat((Integer) (payloadJson.getJsonArray(PROSECUTION_CASES).size()), is(1));
        assertThat(payloadJson.getJsonArray(COURT_APPLICATIONS).getJsonObject(0).getString(ID), is(APPLICATION_ID.toString()));
        assertThat(payloadJson.getJsonArray(COURT_APPLICATIONS).getJsonObject(0).getJsonArray(COURT_APPLICATION_CASES).getJsonObject(0).getString(PROSECUTION_CASE_ID), is(CASE_ID2.toString()));
    }

    @Test
    public void shouldHandleHearingApplicationLinkCreatedEventWithExistingHearingAndNewCase() {
        final Hearing hearingFromDb = Hearing.hearing()
                .withId(HEARING_ID)
                .withProsecutionCases(singletonList(ProsecutionCase.prosecutionCase()
                        .withId(CASE_ID)
                        .build()))
                .build();

        final List<JudicialResult> judicialResults = new ArrayList<>();
        final JudicialResult judicialResult = JudicialResult.judicialResult()
                .withJudicialResultId(UUID.randomUUID())
                .withPublishedForNows(false)
                .withResultText("Sample")
                .build();
        judicialResults.add(judicialResult);
        judicialResults.add(judicialResult);
        judicialResults.add(judicialResult);
        judicialResults.add(judicialResult);

        final Hearing hearing = Hearing.hearing().withId(HEARING_ID)
                .withCourtApplications(singletonList(courtApplication()
                        .withId(APPLICATION_ID)
                        .withJudicialResults(judicialResults)
                        .withCourtApplicationCases(singletonList(courtApplicationCase()
                                .withProsecutionCaseId(CASE_ID2).build())).build()))
                .withProsecutionCases(singletonList(ProsecutionCase.prosecutionCase()
                        .withId(CASE_ID2)
                        .build()))
                .build();

        HearingApplicationLinkCreated hearingApplicationLinkCreated
                = hearingApplicationLinkCreated()
                .withApplicationId(APPLICATION_ID)
                .withHearing(hearing)
                .build();
        final HearingEntity hearingEntity = new HearingEntity();
        hearingEntity.setHearingId(HEARING_ID);
        hearingEntity.setPayload(objectToJsonObjectConverter.convert(hearingFromDb).toString());
        hearingEntity.setListingStatus(HearingListingStatus.HEARING_INITIALISED);

        final JsonObject payload = objectToJsonObjectConverter.convert(hearingApplicationLinkCreated);
        when(envelope.payloadAsJsonObject()).thenReturn(payload);
        when(hearingRepository.findBy(HEARING_ID)).thenReturn(hearingEntity);

        eventListener.process(envelope);

        verify(repository).save(argumentCaptor.capture());
        final HearingApplicationEntity hearingApplicationEntity = argumentCaptor.getValue();
        assertThat(hearingApplicationEntity.getHearing().getHearingId(), is(HEARING_ID));
        final JsonObject payloadJson = stringToJsonObjectConverter.convert(hearingApplicationEntity.getHearing().getPayload());
        assertThat(payloadJson.getJsonArray(PROSECUTION_CASES).getJsonObject(0).getString(ID), is(CASE_ID.toString()));
        assertThat(payloadJson.getJsonArray(PROSECUTION_CASES).getJsonObject(1).getString(ID), is(CASE_ID2.toString()));
        assertThat(payloadJson.getJsonArray(COURT_APPLICATIONS).getJsonObject(0).getString(ID), is(APPLICATION_ID.toString()));
        assertThat(payloadJson.getJsonArray(COURT_APPLICATIONS).getJsonObject(0).getJsonArray(JUDICIAL_RESULTS).size(), is(1));
        assertThat(payloadJson.getJsonArray(COURT_APPLICATIONS).getJsonObject(0).getJsonArray(COURT_APPLICATION_CASES).getJsonObject(0).getString(PROSECUTION_CASE_ID), is(CASE_ID2.toString()));
    }

    @Test
    public void shouldHandleHearingApplicationLinkCreatedEventWithNewHearing() {

        final Hearing hearing = Hearing.hearing().withId(HEARING_ID)
                .withCourtApplications(singletonList(courtApplication()
                        .withId(APPLICATION_ID)
                        .withCourtApplicationCases(singletonList(courtApplicationCase()
                                .withProsecutionCaseId(CASE_ID).build())).build()))
                .withProsecutionCases(singletonList(ProsecutionCase.prosecutionCase()
                        .withId(CASE_ID)
                        .build()))
                .build();

        HearingApplicationLinkCreated hearingApplicationLinkCreated
                = hearingApplicationLinkCreated()
                .withApplicationId(APPLICATION_ID)
                .withHearing(hearing)
                .build();

        final JsonObject payload = objectToJsonObjectConverter.convert(hearingApplicationLinkCreated);
        when(envelope.payloadAsJsonObject()).thenReturn(payload);

        eventListener.process(envelope);

        verify(repository).save(argumentCaptor.capture());
        final HearingApplicationEntity hearingApplicationEntity = argumentCaptor.getValue();
        assertThat(hearingApplicationEntity.getHearing().getHearingId(), is(HEARING_ID));
        final JsonObject payloadJson = stringToJsonObjectConverter.convert(hearingApplicationEntity.getHearing().getPayload());
        assertThat(payloadJson.getJsonArray(PROSECUTION_CASES).getJsonObject(0).getString(ID), is(CASE_ID.toString()));
        assertThat(payloadJson.getJsonArray(COURT_APPLICATIONS).getJsonObject(0).getString(ID), is(APPLICATION_ID.toString()));
        assertThat(payloadJson.getJsonArray(COURT_APPLICATIONS).getJsonObject(0).getJsonArray(COURT_APPLICATION_CASES).getJsonObject(0).getString(PROSECUTION_CASE_ID), is(CASE_ID.toString()));
    }

    @Test
    public void shouldRemoveNowsSpecificJudicialResultsBeforeSaving() {

        List<JudicialResult> judicialResults = new ArrayList<>();
        judicialResults.add(JudicialResult.judicialResult().withLabel("PublishedForNowsTrue").withPublishedForNows(Boolean.TRUE).build());
        judicialResults.add(JudicialResult.judicialResult().withLabel("PublishedForNowsFalse").withPublishedForNows(Boolean.FALSE).build());

        final Hearing hearing = Hearing.hearing().withCourtApplications(getCourtApplications(judicialResults)).withId(HEARING_ID).build();

        HearingApplicationLinkCreated hearingApplicationLinkCreated
                = hearingApplicationLinkCreated()
                .withApplicationId(APPLICATION_ID)
                .withHearing(hearing)
                .build();
        final HearingEntity hearingEntity = new HearingEntity();
        hearingEntity.setHearingId(HEARING_ID);
        hearingEntity.setPayload(Json.createObjectBuilder().build().toString());
        hearingEntity.setListingStatus(HearingListingStatus.HEARING_INITIALISED);

        final JsonObject payload = objectToJsonObjectConverter.convert(hearingApplicationLinkCreated);
        when(envelope.payloadAsJsonObject()).thenReturn(payload);

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

        final JsonObject payload = objectToJsonObjectConverter.convert(hearingDeletedForCourtApplication);
        when(envelope.payloadAsJsonObject()).thenReturn(payload);

        eventListener.processHearingDeletedForCourtApplicationEvent(envelope);
        ArgumentCaptor<UUID> hearingIdArgumentCaptor = ArgumentCaptor.forClass(UUID.class);
        ArgumentCaptor<UUID> courtApplicationIdArgumentCaptor = ArgumentCaptor.forClass(UUID.class);
        verify(repository, times(1)).removeByHearingIdAndCourtApplicationId(hearingIdArgumentCaptor.capture(), courtApplicationIdArgumentCaptor.capture());

        assertThat(hearingIdArgumentCaptor.getValue(), is(HEARING_ID));
        assertThat(courtApplicationIdArgumentCaptor.getValue(), is(APPLICATION_ID));
    }

    private List<CourtApplication> getCourtApplications(final List<JudicialResult> judicialResults) {
        List<CourtApplication> courtApplications = new ArrayList<>();
        courtApplications.add(courtApplication()
                .withJudicialResults(judicialResults)
                .build());

        return courtApplications;
    }
}
