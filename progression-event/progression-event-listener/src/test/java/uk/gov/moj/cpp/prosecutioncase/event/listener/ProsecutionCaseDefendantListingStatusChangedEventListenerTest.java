package uk.gov.moj.cpp.prosecutioncase.event.listener;

import static java.util.Arrays.asList;
import static java.util.UUID.randomUUID;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.moj.cpp.progression.test.CoreTestTemplates.defaultArguments;


import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import uk.gov.justice.core.courts.CourtApplication;
import uk.gov.justice.core.courts.Defendant;
import uk.gov.justice.core.courts.Hearing;
import uk.gov.justice.core.courts.HearingListingStatus;
import uk.gov.justice.core.courts.JudicialResult;
import uk.gov.justice.core.courts.JurisdictionType;
import uk.gov.justice.core.courts.Offence;
import uk.gov.justice.core.courts.ProsecutionCase;
import uk.gov.justice.core.courts.ProsecutionCaseDefendantListingStatusChanged;
import uk.gov.justice.core.courts.ProsecutionCaseDefendantListingStatusChangedV2;
import uk.gov.justice.progression.courts.CaseAddedToHearingBdf;
import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.common.converter.ObjectToJsonObjectConverter;
import uk.gov.justice.services.common.converter.StringToJsonObjectConverter;
import uk.gov.justice.services.common.converter.jackson.ObjectMapperProducer;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.progression.test.CoreTestTemplates;
import uk.gov.moj.cpp.prosecutioncase.persistence.entity.CaseDefendantHearingEntity;
import uk.gov.moj.cpp.prosecutioncase.persistence.entity.HearingEntity;
import uk.gov.moj.cpp.prosecutioncase.persistence.entity.MatchDefendantCaseHearingEntity;
import uk.gov.moj.cpp.prosecutioncase.persistence.repository.CaseDefendantHearingRepository;
import uk.gov.moj.cpp.prosecutioncase.persistence.repository.HearingRepository;
import uk.gov.moj.cpp.prosecutioncase.persistence.repository.MatchDefendantCaseHearingRepository;
import uk.gov.moj.cpp.util.FileUtil;

import java.util.Arrays;
import java.util.Map;
import java.util.UUID;

import javax.json.Json;
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
public class ProsecutionCaseDefendantListingStatusChangedEventListenerTest {

    private UUID hearingId;
    private UUID caseId;
    private UUID defendantId;

    @Mock
    private JsonObjectToObjectConverter jsonObjectToObjectConverter;

    @Spy
    private final ObjectMapper objectMapper = new ObjectMapperProducer().objectMapper();

    @Spy
    private final ObjectToJsonObjectConverter objectToJsonObjectConverter = new ObjectToJsonObjectConverter(objectMapper);

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

    @InjectMocks
    private ProsecutionCaseDefendantListingStatusChangedListener eventListener;

    @Mock
    private HearingRepository hearingRepository;

    @Mock
    private MatchDefendantCaseHearingRepository matchDefendantCaseHearingRepository;
    @Spy
    private StringToJsonObjectConverter stringToJsonObjectConverter;

    @BeforeEach
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
        when(jsonObjectToObjectConverter.convert(payload, ProsecutionCaseDefendantListingStatusChanged.class)).thenReturn(getEnvelope(HearingListingStatus.HEARING_INITIALISED));

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
        when(jsonObjectToObjectConverter.convert(payload, ProsecutionCaseDefendantListingStatusChanged.class)).thenReturn(getEnvelope(HearingListingStatus.HEARING_INITIALISED));

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
        when(jsonObjectToObjectConverter.convert(payload, ProsecutionCaseDefendantListingStatusChangedV2.class)).thenReturn(getEnvelopeForV2(HearingListingStatus.HEARING_INITIALISED));

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
        when(jsonObjectToObjectConverter.convert(payload, ProsecutionCaseDefendantListingStatusChangedV2.class)).thenReturn(getEnvelopeForV2(HearingListingStatus.HEARING_INITIALISED));

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

    @Test
    public void ShouldRemoveNowsResultFromHearing(){
        final HearingEntity hearingEntity = new HearingEntity();
        hearingEntity.setHearingId(hearingId);
       hearingEntity.setListingStatus(HearingListingStatus.HEARING_INITIALISED);

        when(hearingRepository.findBy(hearingId)).thenReturn(hearingEntity);
        when(envelope.payloadAsJsonObject()).thenReturn(payload);
        when(jsonObjectToObjectConverter.convert(payload, ProsecutionCaseDefendantListingStatusChangedV2.class)).thenReturn(getEnvelopeForV2WithNows(HearingListingStatus.HEARING_RESULTED));


        eventListener.processV2(envelope);

        assertThat(hearingEntity.getPayload().contains("publishedForNows\":true"), is(false));


    }

    @Test
    void shouldAddWholeCaseToTableIfTheCaseIsNotThere(){
        final UUID case1 = randomUUID();
        final UUID defendant1 = randomUUID();
        final UUID offenceId1 = randomUUID();

        final UUID case2 = randomUUID();
        final UUID defendant2 = randomUUID();
        final UUID offenceId2 = randomUUID();

        final Hearing hearing = CoreTestTemplates.hearing(defaultArguments()
                        .setJurisdictionType(JurisdictionType.CROWN)
                        .setStructure(Map.of(case1, Map.of(defendant1, asList(offenceId1))))
                        .setConvicted(false))
                .build();
        final HearingEntity hearingEntity = new HearingEntity();
        hearingEntity.setHearingId(hearingId);
        hearingEntity.setListingStatus(HearingListingStatus.HEARING_INITIALISED);
        hearingEntity.setPayload(objectToJsonObjectConverter.convert(hearing).toString());

        final MatchDefendantCaseHearingEntity matchDefendantCaseHearingEntity = getMatchDefendantCaseHearingEntity();

        when(hearingRepository.findBy(hearingId)).thenReturn(hearingEntity);
        when(envelope.payloadAsJsonObject()).thenReturn(payload);
        when(matchDefendantCaseHearingRepository.findByProsecutionCaseIdAndDefendantId(case1, defendant1)).thenReturn(Arrays.asList(matchDefendantCaseHearingEntity));
        when(matchDefendantCaseHearingRepository.findByProsecutionCaseIdAndDefendantId(case2, defendant2)).thenReturn(Arrays.asList(matchDefendantCaseHearingEntity));

        final CaseAddedToHearingBdf caseAddedToHearingBdf = CaseAddedToHearingBdf.caseAddedToHearingBdf()
                .withHearingId(hearingId)
                .withProsecutionCases( CoreTestTemplates.hearing(defaultArguments()
                                .setJurisdictionType(JurisdictionType.CROWN)
                                .setStructure(Map.of(case2, Map.of(defendant2, asList(offenceId2))))
                                .setConvicted(false))
                        .build().getProsecutionCases())
                .build();

        when(jsonObjectToObjectConverter.convert(payload, CaseAddedToHearingBdf.class)).thenReturn(caseAddedToHearingBdf);
        when(jsonObjectToObjectConverter.convert(any(), eq(Hearing.class))).thenReturn(hearing);

        eventListener.handlerCaseAddedToHearingBdf(envelope);
        final  JsonObjectToObjectConverter jsonObjectToObjectConverter2 = new JsonObjectToObjectConverter(objectMapper);
        final Hearing updatedHearing = jsonObjectToObjectConverter2.convert(stringToJsonObjectConverter.convert(hearingEntity.getPayload()), Hearing.class);

        assertThat(updatedHearing.getProsecutionCases().size(), is(2));

        verify(caseDefendantHearingRepository, times(2)).save(argumentCaptorCaseDefendantHearingEntity.capture());
        assertThat(argumentCaptorCaseDefendantHearingEntity.getAllValues().get(0).getId().getCaseId(), is(case1));
        assertThat(argumentCaptorCaseDefendantHearingEntity.getAllValues().get(0).getId().getDefendantId(), is(defendant1));
        assertThat(argumentCaptorCaseDefendantHearingEntity.getAllValues().get(0).getId().getHearingId(), is(hearingId));
        assertThat(argumentCaptorCaseDefendantHearingEntity.getAllValues().get(0).getHearing(), is(hearingEntity));

        assertThat(argumentCaptorCaseDefendantHearingEntity.getAllValues().get(1).getId().getCaseId(), is(case2));
        assertThat(argumentCaptorCaseDefendantHearingEntity.getAllValues().get(1).getId().getDefendantId(), is(defendant2));
        assertThat(argumentCaptorCaseDefendantHearingEntity.getAllValues().get(1).getId().getHearingId(), is(hearingId));
        assertThat(argumentCaptorCaseDefendantHearingEntity.getAllValues().get(1).getHearing(), is(hearingEntity));

        verify(matchDefendantCaseHearingRepository, times(2)).save(argumentCaptorMatchDefendantCaseHearingEntity.capture());
        assertThat(argumentCaptorMatchDefendantCaseHearingEntity.getAllValues().get(0).getHearingId(), is(hearingId));
        assertThat(argumentCaptorMatchDefendantCaseHearingEntity.getAllValues().get(0).getHearing(), is(hearingEntity));
        assertThat(argumentCaptorMatchDefendantCaseHearingEntity.getAllValues().get(1).getHearingId(), is(hearingId));
        assertThat(argumentCaptorMatchDefendantCaseHearingEntity.getAllValues().get(1).getHearing(), is(hearingEntity));
    }

    @Test
    void shouldAddWholeCaseToTableIfTheCaseIsNull(){
        final UUID case2 = randomUUID();
        final UUID defendant2 = randomUUID();
        final UUID offenceId2 = randomUUID();

        Hearing hearing = CoreTestTemplates.hearing(defaultArguments()
                        .setJurisdictionType(JurisdictionType.CROWN)
                        .setConvicted(false))
                .build();
        hearing = Hearing.hearing().withValuesFrom(hearing).withProsecutionCases(null).build();
        final HearingEntity hearingEntity = new HearingEntity();
        hearingEntity.setHearingId(hearingId);
        hearingEntity.setListingStatus(HearingListingStatus.HEARING_INITIALISED);
        hearingEntity.setPayload(objectToJsonObjectConverter.convert(hearing).toString());

        final MatchDefendantCaseHearingEntity matchDefendantCaseHearingEntity = getMatchDefendantCaseHearingEntity();

        when(hearingRepository.findBy(hearingId)).thenReturn(hearingEntity);
        when(envelope.payloadAsJsonObject()).thenReturn(payload);
        when(matchDefendantCaseHearingRepository.findByProsecutionCaseIdAndDefendantId(case2, defendant2)).thenReturn(Arrays.asList(matchDefendantCaseHearingEntity));

        final CaseAddedToHearingBdf caseAddedToHearingBdf = CaseAddedToHearingBdf.caseAddedToHearingBdf()
                .withHearingId(hearingId)
                .withProsecutionCases( CoreTestTemplates.hearing(defaultArguments()
                                .setJurisdictionType(JurisdictionType.CROWN)
                                .setStructure(Map.of(case2, Map.of(defendant2, asList(offenceId2))))
                                .setConvicted(false))
                        .build().getProsecutionCases())
                .build();

        when(jsonObjectToObjectConverter.convert(payload, CaseAddedToHearingBdf.class)).thenReturn(caseAddedToHearingBdf);
        when(jsonObjectToObjectConverter.convert(any(), eq(Hearing.class))).thenReturn(hearing);

        eventListener.handlerCaseAddedToHearingBdf(envelope);
        final  JsonObjectToObjectConverter jsonObjectToObjectConverter2 = new JsonObjectToObjectConverter(objectMapper);
        final Hearing updatedHearing = jsonObjectToObjectConverter2.convert(stringToJsonObjectConverter.convert(hearingEntity.getPayload()), Hearing.class);

        assertThat(updatedHearing.getProsecutionCases().size(), is(1));

        verify(caseDefendantHearingRepository, times(1)).save(argumentCaptorCaseDefendantHearingEntity.capture());

        assertThat(argumentCaptorCaseDefendantHearingEntity.getAllValues().get(0).getId().getCaseId(), is(case2));
        assertThat(argumentCaptorCaseDefendantHearingEntity.getAllValues().get(0).getId().getDefendantId(), is(defendant2));
        assertThat(argumentCaptorCaseDefendantHearingEntity.getAllValues().get(0).getId().getHearingId(), is(hearingId));
        assertThat(argumentCaptorCaseDefendantHearingEntity.getAllValues().get(0).getHearing(), is(hearingEntity));

        verify(matchDefendantCaseHearingRepository, times(1)).save(argumentCaptorMatchDefendantCaseHearingEntity.capture());
        assertThat(argumentCaptorMatchDefendantCaseHearingEntity.getAllValues().get(0).getHearingId(), is(hearingId));
        assertThat(argumentCaptorMatchDefendantCaseHearingEntity.getAllValues().get(0).getHearing(), is(hearingEntity));

    }

    @Test
    void shouldAddOnlySelectedDefendantsToAggregateIfTheDefendantIsNotThere(){
        final UUID case1 = randomUUID();
        final UUID defendant1 = randomUUID();
        final UUID offenceId1 = randomUUID();

        final UUID defendant2 = randomUUID();
        final UUID offenceId2 = randomUUID();

        final Hearing hearing = CoreTestTemplates.hearing(defaultArguments()
                        .setJurisdictionType(JurisdictionType.CROWN)
                        .setStructure(Map.of(case1, Map.of(defendant1, asList(offenceId1))))
                        .setConvicted(false))
                .build();
        final HearingEntity hearingEntity = new HearingEntity();
        hearingEntity.setHearingId(hearingId);
        hearingEntity.setListingStatus(HearingListingStatus.HEARING_INITIALISED);
        hearingEntity.setPayload(objectToJsonObjectConverter.convert(hearing).toString());

        final MatchDefendantCaseHearingEntity matchDefendantCaseHearingEntity = getMatchDefendantCaseHearingEntity();

        when(hearingRepository.findBy(hearingId)).thenReturn(hearingEntity);
        when(envelope.payloadAsJsonObject()).thenReturn(payload);
        when(matchDefendantCaseHearingRepository.findByProsecutionCaseIdAndDefendantId(case1, defendant1)).thenReturn(Arrays.asList(matchDefendantCaseHearingEntity));
        when(matchDefendantCaseHearingRepository.findByProsecutionCaseIdAndDefendantId(case1, defendant2)).thenReturn(Arrays.asList(matchDefendantCaseHearingEntity));

        final CaseAddedToHearingBdf caseAddedToHearingBdf = CaseAddedToHearingBdf.caseAddedToHearingBdf()
                .withHearingId(hearingId)
                .withProsecutionCases( CoreTestTemplates.hearing(defaultArguments()
                                .setJurisdictionType(JurisdictionType.CROWN)
                                .setStructure(Map.of(case1, Map.of(defendant1, asList(offenceId1), defendant2, asList(offenceId2))))
                                .setConvicted(false))
                        .build().getProsecutionCases())
                .build();

        when(jsonObjectToObjectConverter.convert(payload, CaseAddedToHearingBdf.class)).thenReturn(caseAddedToHearingBdf);
        when(jsonObjectToObjectConverter.convert(any(), eq(Hearing.class))).thenReturn(hearing);

        eventListener.handlerCaseAddedToHearingBdf(envelope);
        final  JsonObjectToObjectConverter jsonObjectToObjectConverter2 = new JsonObjectToObjectConverter(objectMapper);
        final Hearing updatedHearing = jsonObjectToObjectConverter2.convert(stringToJsonObjectConverter.convert(hearingEntity.getPayload()), Hearing.class);

        assertThat(updatedHearing.getProsecutionCases().size(), is(1));
        assertThat(updatedHearing.getProsecutionCases().get(0).getDefendants().size(), is(2));

        verify(caseDefendantHearingRepository, times(2)).save(argumentCaptorCaseDefendantHearingEntity.capture());
        assertThat(argumentCaptorCaseDefendantHearingEntity.getAllValues().get(0).getId().getCaseId(), is(case1));
        assertThat(argumentCaptorCaseDefendantHearingEntity.getAllValues().get(0).getId().getDefendantId(), is(defendant1));
        assertThat(argumentCaptorCaseDefendantHearingEntity.getAllValues().get(0).getId().getHearingId(), is(hearingId));
        assertThat(argumentCaptorCaseDefendantHearingEntity.getAllValues().get(0).getHearing(), is(hearingEntity));

        assertThat(argumentCaptorCaseDefendantHearingEntity.getAllValues().get(1).getId().getCaseId(), is(case1));
        assertThat(argumentCaptorCaseDefendantHearingEntity.getAllValues().get(1).getId().getDefendantId(), is(defendant2));
        assertThat(argumentCaptorCaseDefendantHearingEntity.getAllValues().get(1).getId().getHearingId(), is(hearingId));
        assertThat(argumentCaptorCaseDefendantHearingEntity.getAllValues().get(1).getHearing(), is(hearingEntity));

        verify(matchDefendantCaseHearingRepository, times(2)).save(argumentCaptorMatchDefendantCaseHearingEntity.capture());
        assertThat(argumentCaptorMatchDefendantCaseHearingEntity.getAllValues().get(0).getHearingId(), is(hearingId));
        assertThat(argumentCaptorMatchDefendantCaseHearingEntity.getAllValues().get(0).getHearing(), is(hearingEntity));
        assertThat(argumentCaptorMatchDefendantCaseHearingEntity.getAllValues().get(1).getHearingId(), is(hearingId));
        assertThat(argumentCaptorMatchDefendantCaseHearingEntity.getAllValues().get(1).getHearing(), is(hearingEntity));

    }

    @Test
    void shouldAddOnlySelectedOffencesToAggregateIfTheOffenceIsNotThere(){
        final UUID case1 = randomUUID();
        final UUID defendant1 = randomUUID();
        final UUID offenceId1 = randomUUID();
        final UUID offenceId2 = randomUUID();

        final Hearing hearing = CoreTestTemplates.hearing(defaultArguments()
                        .setJurisdictionType(JurisdictionType.CROWN)
                        .setStructure(Map.of(case1, Map.of(defendant1, asList(offenceId1))))
                        .setConvicted(false))
                .build();
        final HearingEntity hearingEntity = new HearingEntity();
        hearingEntity.setHearingId(hearingId);
        hearingEntity.setListingStatus(HearingListingStatus.HEARING_INITIALISED);
        hearingEntity.setPayload(objectToJsonObjectConverter.convert(hearing).toString());

        final MatchDefendantCaseHearingEntity matchDefendantCaseHearingEntity = getMatchDefendantCaseHearingEntity();

        when(hearingRepository.findBy(hearingId)).thenReturn(hearingEntity);
        when(envelope.payloadAsJsonObject()).thenReturn(payload);
        when(matchDefendantCaseHearingRepository.findByProsecutionCaseIdAndDefendantId(case1, defendant1)).thenReturn(Arrays.asList(matchDefendantCaseHearingEntity));

        final CaseAddedToHearingBdf caseAddedToHearingBdf = CaseAddedToHearingBdf.caseAddedToHearingBdf()
                .withHearingId(hearingId)
                .withProsecutionCases( CoreTestTemplates.hearing(defaultArguments()
                                .setJurisdictionType(JurisdictionType.CROWN)
                                .setStructure(Map.of(case1, Map.of(defendant1, asList(offenceId1, offenceId2))))
                                .setConvicted(false))
                        .build().getProsecutionCases())
                .build();

        when(jsonObjectToObjectConverter.convert(payload, CaseAddedToHearingBdf.class)).thenReturn(caseAddedToHearingBdf);
        when(jsonObjectToObjectConverter.convert(any(), eq(Hearing.class))).thenReturn(hearing);

        eventListener.handlerCaseAddedToHearingBdf(envelope);
        final  JsonObjectToObjectConverter jsonObjectToObjectConverter2 = new JsonObjectToObjectConverter(objectMapper);
        final Hearing updatedHearing = jsonObjectToObjectConverter2.convert(stringToJsonObjectConverter.convert(hearingEntity.getPayload()), Hearing.class);

        assertThat(updatedHearing.getProsecutionCases().size(), is(1));
        assertThat(updatedHearing.getProsecutionCases().get(0).getDefendants().size(), is(1));
        assertThat(updatedHearing.getProsecutionCases().get(0).getDefendants().get(0).getOffences().size(), is(2));

        verify(caseDefendantHearingRepository).save(argumentCaptorCaseDefendantHearingEntity.capture());
        assertThat(argumentCaptorCaseDefendantHearingEntity.getValue().getId().getCaseId(), is(case1));
        assertThat(argumentCaptorCaseDefendantHearingEntity.getValue().getId().getDefendantId(), is(defendant1));
        assertThat(argumentCaptorCaseDefendantHearingEntity.getValue().getId().getHearingId(), is(hearingId));
        assertThat(argumentCaptorCaseDefendantHearingEntity.getValue().getHearing(), is(hearingEntity));

        verify(matchDefendantCaseHearingRepository).save(argumentCaptorMatchDefendantCaseHearingEntity.capture());
        assertThat(argumentCaptorMatchDefendantCaseHearingEntity.getValue().getHearingId(), is(hearingId));
        assertThat(argumentCaptorMatchDefendantCaseHearingEntity.getValue().getHearing(), is(hearingEntity));

    }

    @Test
    void shouldNotUpdateAggregateIfAllOfTheAreInAggregate(){
        final UUID case1 = randomUUID();
        final UUID defendant1 = randomUUID();
        final UUID offenceId1 = randomUUID();
        final UUID offenceId2 = randomUUID();

        final Hearing hearing = CoreTestTemplates.hearing(defaultArguments()
                        .setJurisdictionType(JurisdictionType.CROWN)
                        .setStructure(Map.of(case1, Map.of(defendant1, asList(offenceId1, offenceId2))))
                        .setConvicted(false))
                .build();
        final HearingEntity hearingEntity = new HearingEntity();
        hearingEntity.setHearingId(hearingId);
        hearingEntity.setListingStatus(HearingListingStatus.HEARING_INITIALISED);
        hearingEntity.setPayload(objectToJsonObjectConverter.convert(hearing).toString());

        final MatchDefendantCaseHearingEntity matchDefendantCaseHearingEntity = getMatchDefendantCaseHearingEntity();

        when(hearingRepository.findBy(hearingId)).thenReturn(hearingEntity);
        when(envelope.payloadAsJsonObject()).thenReturn(payload);
        when(matchDefendantCaseHearingRepository.findByProsecutionCaseIdAndDefendantId(case1, defendant1)).thenReturn(Arrays.asList(matchDefendantCaseHearingEntity));

        final CaseAddedToHearingBdf caseAddedToHearingBdf = CaseAddedToHearingBdf.caseAddedToHearingBdf()
                .withHearingId(hearingId)
                .withProsecutionCases( CoreTestTemplates.hearing(defaultArguments()
                                .setJurisdictionType(JurisdictionType.CROWN)
                                .setStructure(Map.of(case1, Map.of(defendant1, asList(offenceId2))))
                                .setConvicted(false))
                        .build().getProsecutionCases())
                .build();

        when(jsonObjectToObjectConverter.convert(payload, CaseAddedToHearingBdf.class)).thenReturn(caseAddedToHearingBdf);
        when(jsonObjectToObjectConverter.convert(any(), eq(Hearing.class))).thenReturn(hearing);

        eventListener.handlerCaseAddedToHearingBdf(envelope);
        final  JsonObjectToObjectConverter jsonObjectToObjectConverter2 = new JsonObjectToObjectConverter(objectMapper);
        final Hearing updatedHearing = jsonObjectToObjectConverter2.convert(stringToJsonObjectConverter.convert(hearingEntity.getPayload()), Hearing.class);

        assertThat(updatedHearing.getProsecutionCases().size(), is(1));
        assertThat(updatedHearing.getProsecutionCases().get(0).getDefendants().size(), is(1));
        assertThat(updatedHearing.getProsecutionCases().get(0).getDefendants().get(0).getOffences().size(), is(2));

        verify(caseDefendantHearingRepository).save(argumentCaptorCaseDefendantHearingEntity.capture());
        assertThat(argumentCaptorCaseDefendantHearingEntity.getValue().getId().getCaseId(), is(case1));
        assertThat(argumentCaptorCaseDefendantHearingEntity.getValue().getId().getDefendantId(), is(defendant1));
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
                                .withDefendants(Arrays.asList(Defendant.defendant().withId(defendantId)
                                        .withOffences(Arrays.asList(Offence.offence().build())).build()))
                                .build()))
                        .build())
                .withHearingListingStatus(hearingListingStatus)
                .build();
    }

    private ProsecutionCaseDefendantListingStatusChangedV2 getEnvelopeForV2(final HearingListingStatus hearingListingStatus) {
        return ProsecutionCaseDefendantListingStatusChangedV2.prosecutionCaseDefendantListingStatusChangedV2()
                .withHearing(Hearing.hearing().withId(hearingId)
                        .withProsecutionCases(Arrays.asList(ProsecutionCase.prosecutionCase().withId(caseId)
                                .withDefendants(Arrays.asList(Defendant.defendant().withId(defendantId)
                                        .withOffences(Arrays.asList(Offence.offence().build())).build()))
                                .build()))
                        .build())
                .withHearingListingStatus(hearingListingStatus)
                .build();
    }

    private ProsecutionCaseDefendantListingStatusChangedV2 getEnvelopeForV2WithNows(final HearingListingStatus hearingListingStatus) {
        return ProsecutionCaseDefendantListingStatusChangedV2.prosecutionCaseDefendantListingStatusChangedV2()
                .withHearing(Hearing.hearing().withId(hearingId)
                        .withProsecutionCases(Arrays.asList(ProsecutionCase.prosecutionCase().withId(caseId)
                                .withDefendants(Arrays.asList(Defendant.defendant().withId(defendantId)
                                        .withOffences(Arrays.asList(Offence.offence()
                                                .withId(randomUUID())
                                                .withJudicialResults(new ArrayList<>(Arrays.asList(JudicialResult.judicialResult()
                                                        .withPublishedForNows(false)
                                                        .build(), JudicialResult.judicialResult()
                                                        .withPublishedForNows(true)
                                                        .build())))
                                                .build()))
                                        .build(),Defendant.defendant().withId(defendantId)
                                        .withOffences(Arrays.asList(Offence.offence()
                                                .withId(randomUUID())
                                                .withJudicialResults(new ArrayList<>(Arrays.asList(JudicialResult.judicialResult()
                                                        .withPublishedForNows(false)
                                                        .build(), JudicialResult.judicialResult()
                                                        .withPublishedForNows(true)
                                                        .build())))
                                                .build()))
                                        .build()))
                                .build(), ProsecutionCase.prosecutionCase().withId(caseId)
                                .withDefendants(Arrays.asList(Defendant.defendant().withId(defendantId)
                                        .withOffences(Arrays.asList(Offence.offence()
                                                .withId(randomUUID())
                                                .withJudicialResults(new ArrayList<>(Arrays.asList(JudicialResult.judicialResult()
                                                        .withPublishedForNows(false)
                                                        .build(), JudicialResult.judicialResult()
                                                        .withPublishedForNows(true)
                                                        .build())))
                                                .build()))
                                        .build(),Defendant.defendant().withId(defendantId)
                                        .withOffences(Arrays.asList(Offence.offence()
                                                .withId(randomUUID())
                                                .withJudicialResults(new ArrayList<>(Arrays.asList(JudicialResult.judicialResult()
                                                        .withPublishedForNows(false)
                                                        .build(), JudicialResult.judicialResult()
                                                        .withPublishedForNows(true)
                                                        .build())))
                                                .build()))
                                        .build()))
                                .build()))
                        .withCourtApplications(Arrays.asList(CourtApplication.courtApplication()
                                .withJudicialResults(new ArrayList<>(Arrays.asList(JudicialResult.judicialResult()
                                        .withPublishedForNows(false)
                                        .build(), JudicialResult.judicialResult()
                                        .withPublishedForNows(true)
                                        .build())))
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
        when(jsonObjectToObjectConverter.convert(eventPayloadJsonObject, ProsecutionCaseDefendantListingStatusChangedV2.class)).thenReturn(getEnvelopeForV2(HearingListingStatus.HEARING_INITIALISED));


        eventListener.processV2(envelope);
        verify(caseDefendantHearingRepository).save(argumentCaptorCaseDefendantHearingEntity.capture());
    }
}