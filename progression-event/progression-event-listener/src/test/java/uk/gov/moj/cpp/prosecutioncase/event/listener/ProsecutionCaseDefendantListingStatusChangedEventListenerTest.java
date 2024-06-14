package uk.gov.moj.cpp.prosecutioncase.event.listener;

import static java.util.UUID.randomUUID;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import uk.gov.justice.core.courts.Defendant;
import uk.gov.justice.core.courts.Hearing;
import uk.gov.justice.core.courts.HearingListingStatus;
import uk.gov.justice.core.courts.ProsecutionCase;
import uk.gov.justice.core.courts.ProsecutionCaseDefendantListingStatusChanged;
import uk.gov.justice.core.courts.ProsecutionCaseDefendantListingStatusChangedV2;
import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.common.converter.ObjectToJsonObjectConverter;
import uk.gov.justice.services.common.converter.StringToJsonObjectConverter;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.messaging.Metadata;
import uk.gov.moj.cpp.prosecutioncase.persistence.entity.CaseDefendantHearingEntity;
import uk.gov.moj.cpp.prosecutioncase.persistence.entity.HearingEntity;
import uk.gov.moj.cpp.prosecutioncase.persistence.entity.MatchDefendantCaseHearingEntity;
import uk.gov.moj.cpp.prosecutioncase.persistence.repository.CaseDefendantHearingRepository;
import uk.gov.moj.cpp.prosecutioncase.persistence.repository.HearingRepository;
import uk.gov.moj.cpp.prosecutioncase.persistence.repository.MatchDefendantCaseHearingRepository;
import uk.gov.moj.cpp.util.FileUtil;

import java.util.Arrays;
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
public class ProsecutionCaseDefendantListingStatusChangedEventListenerTest {

    private UUID hearingId;
    private UUID caseId;
    private UUID defendantId;

    @Mock
    private JsonObjectToObjectConverter jsonObjectToObjectConverter;

    @Mock
    private ObjectToJsonObjectConverter objectToJsonObjectConverter;

    @Mock
    private JsonEnvelope envelope;

    @Mock
    private CaseDefendantHearingRepository caseDefendantHearingRepository;

    @Captor
    private ArgumentCaptor<CaseDefendantHearingEntity> argumentCaptorCaseDefendantHearingEntity;

    @Captor
    private ArgumentCaptor<MatchDefendantCaseHearingEntity> argumentCaptorMatchDefendantCaseHearingEntity;

    @Mock
    private JsonObject payload;

    @Mock
    private Metadata metadata;

    @InjectMocks
    private ProsecutionCaseDefendantListingStatusChangedListener eventListener;

    @Mock
    private HearingRepository hearingRepository;

    @Mock
    private MatchDefendantCaseHearingRepository matchDefendantCaseHearingRepository;
    @Spy
    private StringToJsonObjectConverter stringToJsonObjectConverter;

    @Before
    public void setUp() {
        hearingId = randomUUID();
        caseId = randomUUID();
        defendantId = randomUUID();
    }

    @Test
    public void shouldHandleProsecutionCaseDefendantHearingResultEvent() throws Exception {

        final HearingEntity hearingEntity = new HearingEntity();
        hearingEntity.setHearingId(hearingId);
        hearingEntity.setPayload(Json.createObjectBuilder().build().toString());
        hearingEntity.setListingStatus(HearingListingStatus.HEARING_INITIALISED);

        final MatchDefendantCaseHearingEntity matchDefendantCaseHearingEntity = getMatchDefendantCaseHearingEntity();

        when(hearingRepository.findBy(hearingId)).thenReturn(hearingEntity);
        when(matchDefendantCaseHearingRepository.findByProsecutionCaseIdAndDefendantId(caseId, defendantId)).thenReturn(Arrays.asList(matchDefendantCaseHearingEntity));
        when(envelope.payloadAsJsonObject()).thenReturn(payload);
        when(objectToJsonObjectConverter.convert(any())).thenReturn(Json.createObjectBuilder().build());
        when(jsonObjectToObjectConverter.convert(payload, ProsecutionCaseDefendantListingStatusChanged.class)).thenReturn(getEnvelope(HearingListingStatus.HEARING_INITIALISED));

        when(envelope.metadata()).thenReturn(metadata);

        eventListener.process(envelope);
        verify(caseDefendantHearingRepository).save(argumentCaptorCaseDefendantHearingEntity.capture());
        assertThat(argumentCaptorCaseDefendantHearingEntity.getValue().getId().getCaseId(), is(caseId));
        assertThat(argumentCaptorCaseDefendantHearingEntity.getValue().getId().getDefendantId(), is(defendantId));
        assertThat(argumentCaptorCaseDefendantHearingEntity.getValue().getId().getHearingId(), is(hearingId));
        assertThat(argumentCaptorCaseDefendantHearingEntity.getValue().getHearing(), notNullValue());
        verify(matchDefendantCaseHearingRepository).save(argumentCaptorMatchDefendantCaseHearingEntity.capture());
        assertThat(argumentCaptorMatchDefendantCaseHearingEntity.getValue().getHearingId(), is(hearingId));
        assertThat(argumentCaptorMatchDefendantCaseHearingEntity.getValue().getHearing(), notNullValue());
    }

    @Test
    public void shouldNotOverwriteHearingWhenStatusIsAlreadyResulted() throws Exception {

        final HearingEntity hearingEntity = mock(HearingEntity.class);
        when(hearingEntity.getHearingId()).thenReturn(hearingId);
        when(hearingEntity.getListingStatus()).thenReturn(HearingListingStatus.HEARING_RESULTED);

        final MatchDefendantCaseHearingEntity matchDefendantCaseHearingEntity = getMatchDefendantCaseHearingEntity();

        when(hearingRepository.findBy(hearingId)).thenReturn(hearingEntity);
        when(matchDefendantCaseHearingRepository.findByProsecutionCaseIdAndDefendantId(caseId, defendantId)).thenReturn(Arrays.asList(matchDefendantCaseHearingEntity));
        when(envelope.payloadAsJsonObject()).thenReturn(payload);
        when(objectToJsonObjectConverter.convert(any())).thenReturn(Json.createObjectBuilder().build());
        when(jsonObjectToObjectConverter.convert(payload, ProsecutionCaseDefendantListingStatusChanged.class)).thenReturn(getEnvelope(HearingListingStatus.HEARING_INITIALISED));

        when(envelope.metadata()).thenReturn(metadata);

        eventListener.process(envelope);
        verify(caseDefendantHearingRepository).save(argumentCaptorCaseDefendantHearingEntity.capture());
        assertThat(argumentCaptorCaseDefendantHearingEntity.getValue().getId().getCaseId(), is(caseId));
        assertThat(argumentCaptorCaseDefendantHearingEntity.getValue().getId().getDefendantId(), is(defendantId));
        assertThat(argumentCaptorCaseDefendantHearingEntity.getValue().getId().getHearingId(), is(hearingId));
        assertThat(argumentCaptorCaseDefendantHearingEntity.getValue().getHearing(), is(hearingEntity));

        verify(matchDefendantCaseHearingRepository).save(argumentCaptorMatchDefendantCaseHearingEntity.capture());
        assertThat(argumentCaptorMatchDefendantCaseHearingEntity.getValue().getHearingId(), is(hearingId));
        assertThat(argumentCaptorMatchDefendantCaseHearingEntity.getValue().getHearing(), is(hearingEntity));
    }

    @Test
    public void processV2ShouldHandleProsecutionCaseDefendantHearingResultEvent() throws Exception {

        final HearingEntity hearingEntity = new HearingEntity();
        hearingEntity.setHearingId(hearingId);
        hearingEntity.setPayload(Json.createObjectBuilder().build().toString());
        hearingEntity.setListingStatus(HearingListingStatus.HEARING_INITIALISED);

        final MatchDefendantCaseHearingEntity matchDefendantCaseHearingEntity = getMatchDefendantCaseHearingEntity();

        when(hearingRepository.findBy(hearingId)).thenReturn(hearingEntity);
        when(matchDefendantCaseHearingRepository.findByProsecutionCaseIdAndDefendantId(caseId, defendantId)).thenReturn(Arrays.asList(matchDefendantCaseHearingEntity));
        when(envelope.payloadAsJsonObject()).thenReturn(payload);
        when(objectToJsonObjectConverter.convert(any())).thenReturn(Json.createObjectBuilder().build());
        when(jsonObjectToObjectConverter.convert(payload, ProsecutionCaseDefendantListingStatusChangedV2.class)).thenReturn(getEnvelopeForV2(HearingListingStatus.HEARING_INITIALISED));

        when(envelope.metadata()).thenReturn(metadata);

        eventListener.processV2(envelope);
        verify(caseDefendantHearingRepository).save(argumentCaptorCaseDefendantHearingEntity.capture());
        assertThat(argumentCaptorCaseDefendantHearingEntity.getValue().getId().getCaseId(), is(caseId));
        assertThat(argumentCaptorCaseDefendantHearingEntity.getValue().getId().getDefendantId(), is(defendantId));
        assertThat(argumentCaptorCaseDefendantHearingEntity.getValue().getId().getHearingId(), is(hearingId));
        assertThat(argumentCaptorCaseDefendantHearingEntity.getValue().getHearing(), notNullValue());
        verify(matchDefendantCaseHearingRepository).save(argumentCaptorMatchDefendantCaseHearingEntity.capture());
        assertThat(argumentCaptorMatchDefendantCaseHearingEntity.getValue().getHearingId(), is(hearingId));
        assertThat(argumentCaptorMatchDefendantCaseHearingEntity.getValue().getHearing(), notNullValue());
    }

    @Test
    public void processV2shouldNotOverwriteHearingWhenStatusIsAlreadyResulted() throws Exception {

        final HearingEntity hearingEntity = mock(HearingEntity.class);
        when(hearingEntity.getHearingId()).thenReturn(hearingId);
        when(hearingEntity.getListingStatus()).thenReturn(HearingListingStatus.HEARING_RESULTED);

        final MatchDefendantCaseHearingEntity matchDefendantCaseHearingEntity = getMatchDefendantCaseHearingEntity();

        when(hearingRepository.findBy(hearingId)).thenReturn(hearingEntity);
        when(matchDefendantCaseHearingRepository.findByProsecutionCaseIdAndDefendantId(caseId, defendantId)).thenReturn(Arrays.asList(matchDefendantCaseHearingEntity));
        when(envelope.payloadAsJsonObject()).thenReturn(payload);
        when(objectToJsonObjectConverter.convert(any())).thenReturn(Json.createObjectBuilder().build());
        when(jsonObjectToObjectConverter.convert(payload, ProsecutionCaseDefendantListingStatusChangedV2.class)).thenReturn(getEnvelopeForV2(HearingListingStatus.HEARING_INITIALISED));

        when(envelope.metadata()).thenReturn(metadata);

        eventListener.processV2(envelope);
        verify(caseDefendantHearingRepository).save(argumentCaptorCaseDefendantHearingEntity.capture());
        assertThat(argumentCaptorCaseDefendantHearingEntity.getValue().getId().getCaseId(), is(caseId));
        assertThat(argumentCaptorCaseDefendantHearingEntity.getValue().getId().getDefendantId(), is(defendantId));
        assertThat(argumentCaptorCaseDefendantHearingEntity.getValue().getId().getHearingId(), is(hearingId));
        assertThat(argumentCaptorCaseDefendantHearingEntity.getValue().getHearing(), is(hearingEntity));

        verify(matchDefendantCaseHearingRepository).save(argumentCaptorMatchDefendantCaseHearingEntity.capture());
        assertThat(argumentCaptorMatchDefendantCaseHearingEntity.getValue().getHearingId(), is(hearingId));
        assertThat(argumentCaptorMatchDefendantCaseHearingEntity.getValue().getHearing(), is(hearingEntity));
    }

    private ProsecutionCaseDefendantListingStatusChanged getEnvelope(final HearingListingStatus hearingListingStatus) {
        return ProsecutionCaseDefendantListingStatusChanged.prosecutionCaseDefendantListingStatusChanged()
                .withHearing(Hearing.hearing().withId(hearingId)
                        .withProsecutionCases(Arrays.asList(ProsecutionCase.prosecutionCase().withId(caseId)
                                .withDefendants(Arrays.asList(Defendant.defendant().withId(defendantId).build()))
                                .build()))
                        .build())
                .withHearingListingStatus(hearingListingStatus)
                .build();
    }

    private ProsecutionCaseDefendantListingStatusChangedV2 getEnvelopeForV2(final HearingListingStatus hearingListingStatus) {
        return ProsecutionCaseDefendantListingStatusChangedV2.prosecutionCaseDefendantListingStatusChangedV2()
                .withHearing(Hearing.hearing().withId(hearingId)
                        .withProsecutionCases(Arrays.asList(ProsecutionCase.prosecutionCase().withId(caseId)
                                .withDefendants(Arrays.asList(Defendant.defendant().withId(defendantId).build()))
                                .build()))
                        .build())
                .withHearingListingStatus(hearingListingStatus)
                .build();
    }

    private MatchDefendantCaseHearingEntity getMatchDefendantCaseHearingEntity(){
        final MatchDefendantCaseHearingEntity matchDefendantCaseHearingEntity = new MatchDefendantCaseHearingEntity();
        matchDefendantCaseHearingEntity.setId(randomUUID());
        matchDefendantCaseHearingEntity.setDefendantId(defendantId);
        return matchDefendantCaseHearingEntity;
    }

    @Test
    public void processV2ShouldHandleProsecutionCaseDefendantHearingResultEvent1() throws Exception {

        final String eventPayload = FileUtil.getPayload("json/progression.event.prosecutionCase-defendant-listing-status-changed-v2.json");
        final JsonObject eventPayloadJsonObject = stringToJsonObjectConverter.convert(eventPayload);

        final HearingEntity hearingEntity = new HearingEntity();
        hearingEntity.setHearingId(hearingId);
        hearingEntity.setPayload(Json.createObjectBuilder().build().toString());
        hearingEntity.setListingStatus(HearingListingStatus.HEARING_INITIALISED);

        final MatchDefendantCaseHearingEntity matchDefendantCaseHearingEntity = getMatchDefendantCaseHearingEntity();

        when(hearingRepository.findBy(hearingId)).thenReturn(hearingEntity);
        when(matchDefendantCaseHearingRepository.findByProsecutionCaseIdAndDefendantId(caseId, defendantId)).thenReturn(Arrays.asList(matchDefendantCaseHearingEntity));
        when(envelope.payloadAsJsonObject()).thenReturn(eventPayloadJsonObject);
        when(objectToJsonObjectConverter.convert(any())).thenReturn(Json.createObjectBuilder().build());
        when(jsonObjectToObjectConverter.convert(eventPayloadJsonObject, ProsecutionCaseDefendantListingStatusChangedV2.class)).thenReturn(getEnvelopeForV2(HearingListingStatus.HEARING_INITIALISED));

        when(envelope.metadata()).thenReturn(metadata);

        eventListener.processV2(envelope);
        verify(caseDefendantHearingRepository).save(argumentCaptorCaseDefendantHearingEntity.capture());
    }
}