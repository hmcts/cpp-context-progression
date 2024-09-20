package uk.gov.moj.cpp.prosecutioncase.event.listener;

import static java.util.UUID.randomUUID;
import static org.codehaus.groovy.runtime.InvokerHelper.asList;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.test.utils.core.reflection.ReflectionUtil.setField;

import uk.gov.justice.core.courts.CustodyTimeLimit;
import uk.gov.justice.core.courts.Defendant;
import uk.gov.justice.core.courts.Hearing;
import uk.gov.justice.core.courts.Offence;
import uk.gov.justice.core.courts.ProsecutionCase;
import uk.gov.justice.progression.courts.CustodyTimeLimitClockStopped;
import uk.gov.justice.progression.courts.CustodyTimeLimitExtended;
import uk.gov.justice.progression.courts.ExtendCustodyTimeLimitResulted;
import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.common.converter.ObjectToJsonObjectConverter;
import uk.gov.justice.services.common.converter.jackson.ObjectMapperProducer;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.moj.cpp.prosecutioncase.persistence.entity.HearingEntity;
import uk.gov.moj.cpp.prosecutioncase.persistence.entity.ProsecutionCaseEntity;
import uk.gov.moj.cpp.prosecutioncase.persistence.repository.HearingRepository;
import uk.gov.moj.cpp.prosecutioncase.persistence.repository.ProsecutionCaseRepository;

import java.io.IOException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class CustodyTimeLimitEventListenerTest {

    @Mock
    private Envelope<CustodyTimeLimitClockStopped> custodyTimeLimitClockStoppedEnvelope;

    @Mock
    private Envelope<ExtendCustodyTimeLimitResulted> extendCustodyTimeLimitResultedEnvelope;

    @Mock
    private Envelope<CustodyTimeLimitExtended> custodyTimeLimitExtendedEnvelope;

    @Mock
    private HearingRepository hearingRepository;

    @Mock
    private ProsecutionCaseRepository prosecutionCaseRepository;

    @Captor
    private ArgumentCaptor<HearingEntity> hearingEntityArgumentCaptor;

    @Captor
    private ArgumentCaptor<ProsecutionCaseEntity> prosecutionCaseEntityArgumentCaptor;

    @InjectMocks
    private CustodyTimeLimitEventListener custodyTimeLimitEventListener;

    @Spy
    private final ObjectMapper objectMapper = new ObjectMapperProducer().objectMapper();

    @Spy
    private JsonObjectToObjectConverter jsonObjectToObjectConverter;

    @Spy
    private ObjectToJsonObjectConverter objectToJsonObjectConverter;

    @BeforeEach
    public void setUp() {
        setField(this.objectToJsonObjectConverter, "mapper", objectMapper);
        setField(this.jsonObjectToObjectConverter, "objectMapper", objectMapper);
    }

    @Test
    public void shouldProcessCustodyTimeLimitClockStopped() throws IOException {
        final UUID hearingId = randomUUID();
        final UUID offence1Id = randomUUID();
        final UUID offence2Id = randomUUID();
        final UUID caseId = randomUUID();
        final HearingEntity hearingEntity = new HearingEntity();
        hearingEntity.setHearingId(hearingId);
        final ProsecutionCaseEntity prosecutionCaseEntity = new ProsecutionCaseEntity();
        prosecutionCaseEntity.setCaseId(caseId);
        final LocalDate timeLimit = LocalDate.now();
        final ProsecutionCase prosecutionCase = ProsecutionCase.prosecutionCase()
                .withDefendants(new ArrayList<>(Arrays.asList(Defendant.defendant()
                        .withOffences(new ArrayList<>(Arrays.asList(Offence.offence()
                                .withId(offence1Id)
                                .withCustodyTimeLimit(CustodyTimeLimit.custodyTimeLimit()
                                        .withTimeLimit(timeLimit)
                                        .build())
                                .build(), Offence.offence()
                                .withId(offence2Id)
                                .withCustodyTimeLimit(CustodyTimeLimit.custodyTimeLimit()
                                        .withTimeLimit(timeLimit)
                                        .build())
                                .build())))
                        .build())))
                .build();
        hearingEntity.setPayload(objectMapper.writeValueAsString(Hearing.hearing()
                .withId(hearingId)
                .withProsecutionCases(Arrays.asList(prosecutionCase))
                .build()));
        prosecutionCaseEntity.setPayload(objectMapper.writeValueAsString(prosecutionCase));

        when(custodyTimeLimitClockStoppedEnvelope.payload()).thenReturn(CustodyTimeLimitClockStopped.custodyTimeLimitClockStopped()
                .withHearingId(hearingId)
                .withOffenceIds(Arrays.asList(offence1Id))
                .withCaseIds(Arrays.asList(caseId))
                .build());

        when(hearingRepository.findBy(hearingId)).thenReturn(hearingEntity);
        when(prosecutionCaseRepository.findByCaseId(caseId)).thenReturn(prosecutionCaseEntity);

        custodyTimeLimitEventListener.processCustodyTimeLimitClockStopped(custodyTimeLimitClockStoppedEnvelope);

        verify(hearingRepository).save(hearingEntityArgumentCaptor.capture());
        verify(prosecutionCaseRepository).save(prosecutionCaseEntityArgumentCaptor.capture());

        final HearingEntity savedHearingEntity = hearingEntityArgumentCaptor.getValue();
        final Hearing dbHearing = objectMapper.readValue(savedHearingEntity.getPayload(), Hearing.class);
        final Offence hearingsOffence1 = dbHearing.getProsecutionCases().get(0).getDefendants().get(0).getOffences().stream().filter(o -> o.getId().equals(offence1Id)).findFirst().get();
        final Offence hearingsOffence2 = dbHearing.getProsecutionCases().get(0).getDefendants().get(0).getOffences().stream().filter(o -> o.getId().equals(offence2Id)).findFirst().get();
        assertThat(hearingsOffence1.getCustodyTimeLimit(), nullValue());
        assertThat(hearingsOffence1.getCtlClockStopped(), is(true));
        assertThat(hearingsOffence2.getCustodyTimeLimit(), notNullValue());
        assertThat(hearingsOffence2.getCtlClockStopped(), nullValue());

        final ProsecutionCaseEntity savedProsecutionCaseEntity = prosecutionCaseEntityArgumentCaptor.getValue();
        final ProsecutionCase dbProsecutionCase = objectMapper.readValue(savedProsecutionCaseEntity.getPayload(), ProsecutionCase.class);
        final Offence prosecutionCasesOffence1 = dbProsecutionCase.getDefendants().get(0).getOffences().get(0);
        final Offence prosecutionCasesOffence2 = dbProsecutionCase.getDefendants().get(0).getOffences().get(1);
        assertThat(prosecutionCasesOffence1.getCustodyTimeLimit(), nullValue());
        assertThat(prosecutionCasesOffence1.getCtlClockStopped(), is(true));
        assertThat(prosecutionCasesOffence2.getCustodyTimeLimit(), notNullValue());
        assertThat(prosecutionCasesOffence2.getCtlClockStopped(), nullValue());

    }

    @Test
    public void shouldProcessCustodyTimeLimitClockStoppedWhenCaseIdsIsEmpty() throws IOException {
        final UUID hearingId = randomUUID();
        final UUID offence1Id = randomUUID();
        final UUID offence2Id = randomUUID();
        final UUID caseId = randomUUID();
        final HearingEntity hearingEntity = new HearingEntity();
        hearingEntity.setHearingId(hearingId);
        final ProsecutionCaseEntity prosecutionCaseEntity = new ProsecutionCaseEntity();
        prosecutionCaseEntity.setCaseId(caseId);
        final LocalDate timeLimit = LocalDate.now();
        final ProsecutionCase prosecutionCase = ProsecutionCase.prosecutionCase()
                .withDefendants(new ArrayList<>(Arrays.asList(Defendant.defendant()
                        .withOffences(new ArrayList<>(Arrays.asList(Offence.offence()
                                .withId(offence1Id)
                                .withCustodyTimeLimit(CustodyTimeLimit.custodyTimeLimit()
                                        .withTimeLimit(timeLimit)
                                        .build())
                                .build(), Offence.offence()
                                .withId(offence2Id)
                                .withCustodyTimeLimit(CustodyTimeLimit.custodyTimeLimit()
                                        .withTimeLimit(timeLimit)
                                        .build())
                                .build())))
                        .build())))
                .build();
        hearingEntity.setPayload(objectMapper.writeValueAsString(Hearing.hearing()
                .withId(hearingId)
                .withProsecutionCases(asList(prosecutionCase))
                .build()));
        prosecutionCaseEntity.setPayload(objectMapper.writeValueAsString(prosecutionCase));

        when(custodyTimeLimitClockStoppedEnvelope.payload()).thenReturn(CustodyTimeLimitClockStopped.custodyTimeLimitClockStopped()
                .withHearingId(hearingId)
                .withOffenceIds(Arrays.asList(offence1Id))
                .withCaseIds(Collections.EMPTY_LIST)
                .build());

        when(hearingRepository.findBy(hearingId)).thenReturn(hearingEntity);

        custodyTimeLimitEventListener.processCustodyTimeLimitClockStopped(custodyTimeLimitClockStoppedEnvelope);

        verify(hearingRepository).save(hearingEntityArgumentCaptor.capture());

        final HearingEntity savedHearingEntity = hearingEntityArgumentCaptor.getValue();
        final Hearing dbHearing = objectMapper.readValue(savedHearingEntity.getPayload(), Hearing.class);
        final Offence hearingsOffence1 = dbHearing.getProsecutionCases().get(0).getDefendants().get(0).getOffences().stream().filter(o -> o.getId().equals(offence1Id)).findFirst().get();
        final Offence hearingsOffence2 = dbHearing.getProsecutionCases().get(0).getDefendants().get(0).getOffences().stream().filter(o -> o.getId().equals(offence2Id)).findFirst().get();
        assertThat(hearingsOffence1.getCustodyTimeLimit(), nullValue());
        assertThat(hearingsOffence1.getCtlClockStopped(), is(true));
        assertThat(hearingsOffence2.getCustodyTimeLimit(), notNullValue());
        assertThat(hearingsOffence2.getCtlClockStopped(), nullValue());


    }

    @Test
    public void shouldProcessExtendCustodyTimeLimitResulted() throws IOException {
        final UUID hearingId = randomUUID();
        final UUID offence1Id = randomUUID();
        final UUID offence2Id = randomUUID();
        final UUID caseId = randomUUID();
        final LocalDate extendedCTL = LocalDate.now().plusDays(56);
        final LocalDate timeLimit = LocalDate.now();

        final HearingEntity hearingEntity = new HearingEntity();
        hearingEntity.setHearingId(hearingId);
        final ProsecutionCaseEntity prosecutionCaseEntity = new ProsecutionCaseEntity();
        prosecutionCaseEntity.setCaseId(caseId);

        final ProsecutionCase prosecutionCase = ProsecutionCase.prosecutionCase()
                .withDefendants(new ArrayList<>(Arrays.asList(Defendant.defendant()
                        .withOffences(new ArrayList<>(Arrays.asList(Offence.offence()
                                .withId(offence1Id)
                                .build(), Offence.offence()
                                .withId(offence2Id)
                                .withCustodyTimeLimit(CustodyTimeLimit.custodyTimeLimit()
                                        .withTimeLimit(timeLimit)
                                        .build())
                                .build())))
                        .build())))
                .build();
        hearingEntity.setPayload(objectMapper.writeValueAsString(Hearing.hearing()
                .withId(hearingId)
                .withProsecutionCases(Arrays.asList(prosecutionCase))
                .build()));
        prosecutionCaseEntity.setPayload(objectMapper.writeValueAsString(prosecutionCase));

        when(extendCustodyTimeLimitResultedEnvelope.payload()).thenReturn(ExtendCustodyTimeLimitResulted.extendCustodyTimeLimitResulted()
                .withHearingId(hearingId)
                .withOffenceId(offence1Id)
                .withCaseId(caseId)
                .withExtendedTimeLimit(extendedCTL)
                .build());

        when(hearingRepository.findBy(hearingId)).thenReturn(hearingEntity);
        when(prosecutionCaseRepository.findByCaseId(caseId)).thenReturn(prosecutionCaseEntity);

        custodyTimeLimitEventListener.processExtendCustodyTimeLimitResulted(extendCustodyTimeLimitResultedEnvelope);

        verify(hearingRepository).save(hearingEntityArgumentCaptor.capture());
        verify(prosecutionCaseRepository).save(prosecutionCaseEntityArgumentCaptor.capture());

        final HearingEntity savedHearingEntity = hearingEntityArgumentCaptor.getValue();
        final Hearing dbHearing = objectMapper.readValue(savedHearingEntity.getPayload(), Hearing.class);
        final Offence hearingsOffence1 = dbHearing.getProsecutionCases().get(0).getDefendants().get(0).getOffences().get(0);
        final Offence hearingsOffence2 = dbHearing.getProsecutionCases().get(0).getDefendants().get(0).getOffences().get(1);
        assertThat(hearingsOffence1.getCustodyTimeLimit().getTimeLimit(), is(extendedCTL));
        assertThat(hearingsOffence2.getCustodyTimeLimit().getTimeLimit(), is(timeLimit));


        final ProsecutionCaseEntity savedProsecutionCaseEntity = prosecutionCaseEntityArgumentCaptor.getValue();
        final ProsecutionCase dbProsecutionCase = objectMapper.readValue(savedProsecutionCaseEntity.getPayload(), ProsecutionCase.class);
        final Offence prosecutionCasesOffence1 = dbProsecutionCase.getDefendants().get(0).getOffences().get(0);
        final Offence prosecutionCasesOffence2 = dbProsecutionCase.getDefendants().get(0).getOffences().get(1);
        assertThat(prosecutionCasesOffence1.getCustodyTimeLimit().getTimeLimit(), is(extendedCTL));
        assertThat(prosecutionCasesOffence1.getCustodyTimeLimit().getIsCtlExtended(), is(true));
        assertThat(prosecutionCasesOffence2.getCustodyTimeLimit().getTimeLimit(), is(timeLimit));
        assertThat(prosecutionCasesOffence2.getCustodyTimeLimit().getIsCtlExtended(), nullValue());


    }

    @Test
    public void shouldProcessCustodyTimeLimitExtended() throws IOException {
        final UUID hearing1Id = randomUUID();
        final UUID hearing2Id = randomUUID();
        final UUID offence1Id = randomUUID();
        final UUID offence2Id = randomUUID();
        final UUID caseId = randomUUID();
        final LocalDate extendedCTL = LocalDate.now().plusDays(56);
        final LocalDate timeLimit = LocalDate.now();

        final HearingEntity hearingEntity1 = new HearingEntity();
        hearingEntity1.setHearingId(hearing1Id);
        final HearingEntity hearingEntity2 = new HearingEntity();
        hearingEntity2.setHearingId(hearing2Id);
        final ProsecutionCaseEntity prosecutionCaseEntity = new ProsecutionCaseEntity();
        prosecutionCaseEntity.setCaseId(caseId);

        final ProsecutionCase prosecutionCase = ProsecutionCase.prosecutionCase()
                .withDefendants(new ArrayList<>(Arrays.asList(Defendant.defendant()
                        .withOffences(new ArrayList<>(Arrays.asList(Offence.offence()
                                .withId(offence1Id)
                                .build(), Offence.offence()
                                .withId(offence2Id)
                                .withCustodyTimeLimit(CustodyTimeLimit.custodyTimeLimit()
                                        .withTimeLimit(timeLimit)
                                        .build())
                                .build())))
                        .build())))
                .build();
        hearingEntity1.setPayload(objectMapper.writeValueAsString(Hearing.hearing()
                .withId(hearing1Id)
                .withProsecutionCases(Arrays.asList(prosecutionCase))
                .build()));
        hearingEntity2.setPayload(objectMapper.writeValueAsString(Hearing.hearing()
                .withId(hearing2Id)
                .withProsecutionCases(Arrays.asList(prosecutionCase))
                .build()));
        prosecutionCaseEntity.setPayload(objectMapper.writeValueAsString(prosecutionCase));

        when(custodyTimeLimitExtendedEnvelope.payload()).thenReturn(CustodyTimeLimitExtended.custodyTimeLimitExtended()
                .withHearingIds(Arrays.asList(hearing1Id, hearing2Id))
                .withOffenceId(offence1Id)
                .withExtendedTimeLimit(extendedCTL)
                .build());

        when(hearingRepository.findBy(hearing1Id)).thenReturn(hearingEntity1);
        when(hearingRepository.findBy(hearing2Id)).thenReturn(hearingEntity2);

        custodyTimeLimitEventListener.processCustodyTimeLimitExtended(custodyTimeLimitExtendedEnvelope);

        verify(hearingRepository, times(2)).save(hearingEntityArgumentCaptor.capture());

        final List<HearingEntity> allCaptorValues = hearingEntityArgumentCaptor.getAllValues();
        final HearingEntity savedHearingEntity1 = allCaptorValues.get(0);
        final HearingEntity savedHearingEntity2 = allCaptorValues.get(1);

        final Hearing dbHearing1 = objectMapper.readValue(savedHearingEntity1.getPayload(), Hearing.class);
        final Offence hearing1sOffence1 = dbHearing1.getProsecutionCases().get(0).getDefendants().get(0).getOffences().get(0);
        final Offence hearings1Offence2 = dbHearing1.getProsecutionCases().get(0).getDefendants().get(0).getOffences().get(1);
        assertThat(hearing1sOffence1.getCustodyTimeLimit().getTimeLimit(), is(extendedCTL));
        assertThat(hearings1Offence2.getCustodyTimeLimit().getTimeLimit(), is(timeLimit));

        final Hearing dbHearing2 = objectMapper.readValue(savedHearingEntity2.getPayload(), Hearing.class);
        final Offence hearing2sOffence1 = dbHearing2.getProsecutionCases().get(0).getDefendants().get(0).getOffences().get(0);
        final Offence hearings2Offence2 = dbHearing2.getProsecutionCases().get(0).getDefendants().get(0).getOffences().get(1);
        assertThat(hearing2sOffence1.getCustodyTimeLimit().getTimeLimit(), is(extendedCTL));
        assertThat(hearing2sOffence1.getCustodyTimeLimit().getIsCtlExtended(), is(true));
        assertThat(hearings2Offence2.getCustodyTimeLimit().getTimeLimit(), is(timeLimit));
        assertThat(hearings2Offence2.getCustodyTimeLimit().getIsCtlExtended(), nullValue());
    }

    @Test
    public void shouldProcessCustodyTimeLimitExtendedWhenHearingDeletedToReplayDLQ() throws IOException {
        final UUID hearing1Id = randomUUID();
        final UUID hearing2Id = randomUUID();
        final UUID offence1Id = randomUUID();
        final UUID offence2Id = randomUUID();
        final UUID caseId = randomUUID();
        final LocalDate extendedCTL = LocalDate.now().plusDays(56);
        final LocalDate timeLimit = LocalDate.now();

        final HearingEntity hearingEntity1 = new HearingEntity();
        hearingEntity1.setHearingId(hearing1Id);
        final HearingEntity hearingEntity2 = new HearingEntity();
        hearingEntity2.setHearingId(hearing2Id);
        final ProsecutionCaseEntity prosecutionCaseEntity = new ProsecutionCaseEntity();
        prosecutionCaseEntity.setCaseId(caseId);

        final ProsecutionCase prosecutionCase = ProsecutionCase.prosecutionCase()
                .withDefendants(new ArrayList<>(Arrays.asList(Defendant.defendant()
                        .withOffences(new ArrayList<>(Arrays.asList(Offence.offence()
                                .withId(offence1Id)
                                .build(), Offence.offence()
                                .withId(offence2Id)
                                .withCustodyTimeLimit(CustodyTimeLimit.custodyTimeLimit()
                                        .withTimeLimit(timeLimit)
                                        .build())
                                .build())))
                        .build())))
                .build();
        hearingEntity1.setPayload(objectMapper.writeValueAsString(Hearing.hearing()
                .withId(hearing1Id)
                .withProsecutionCases(Arrays.asList(prosecutionCase))
                .build()));

        prosecutionCaseEntity.setPayload(objectMapper.writeValueAsString(prosecutionCase));

        when(custodyTimeLimitExtendedEnvelope.payload()).thenReturn(CustodyTimeLimitExtended.custodyTimeLimitExtended()
                .withHearingIds(Arrays.asList(hearing1Id, hearing2Id))
                .withOffenceId(offence1Id)
                .withExtendedTimeLimit(extendedCTL)
                .build());

        when(hearingRepository.findBy(hearing1Id)).thenReturn(hearingEntity1);
        when(hearingRepository.findBy(hearing2Id)).thenReturn(null);

        custodyTimeLimitEventListener.processCustodyTimeLimitExtended(custodyTimeLimitExtendedEnvelope);

        verify(hearingRepository, times(1)).save(hearingEntityArgumentCaptor.capture());

        final List<HearingEntity> allCaptorValues = hearingEntityArgumentCaptor.getAllValues();
        final HearingEntity savedHearingEntity1 = allCaptorValues.get(0);

        final Hearing dbHearing1 = objectMapper.readValue(savedHearingEntity1.getPayload(), Hearing.class);
        final Offence hearing1sOffence1 = dbHearing1.getProsecutionCases().get(0).getDefendants().get(0).getOffences().get(0);
        final Offence hearings1Offence2 = dbHearing1.getProsecutionCases().get(0).getDefendants().get(0).getOffences().get(1);
        assertThat(hearing1sOffence1.getCustodyTimeLimit().getTimeLimit(), is(extendedCTL));
        assertThat(hearings1Offence2.getCustodyTimeLimit().getTimeLimit(), is(timeLimit));

    }
}
