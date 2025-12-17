package uk.gov.moj.cpp.prosecutioncase.event.listener;


import static java.util.UUID.randomUUID;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.test.utils.core.reflection.ReflectionUtil.setField;

import uk.gov.justice.core.courts.Defendant;
import uk.gov.justice.core.courts.Hearing;
import uk.gov.justice.core.courts.Offence;
import uk.gov.justice.core.courts.ProsecutionCase;
import uk.gov.justice.progression.courts.OffencesRemovedFromHearing;
import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.common.converter.ObjectToJsonObjectConverter;
import uk.gov.justice.services.common.converter.StringToJsonObjectConverter;
import uk.gov.justice.services.common.converter.jackson.ObjectMapperProducer;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.moj.cpp.prosecutioncase.persistence.entity.HearingEntity;
import uk.gov.moj.cpp.prosecutioncase.persistence.repository.CaseDefendantHearingRepository;
import uk.gov.moj.cpp.prosecutioncase.persistence.repository.HearingRepository;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;


@ExtendWith(MockitoExtension.class)
public class HearingUnallocatedEventListenerTest {

    @Captor
    ArgumentCaptor<HearingEntity> hearingEntityArgumentCaptor;
    @Mock
    private Envelope<OffencesRemovedFromHearing> offencesRemovedFromHearingEnvelope;
    @Mock
    private HearingRepository hearingRepository;
    @Mock
    private CaseDefendantHearingRepository caseDefendantHearingRepository;
    @InjectMocks
    private HearingUnallocatedEventListener hearingUnallocatedEventListener;
    @Spy
    private JsonObjectToObjectConverter jsonObjectToObjectConverter;
    @Spy
    private ObjectToJsonObjectConverter objectToJsonObjectConverter;
    @Spy
    private StringToJsonObjectConverter stringToJsonObjectConverter;
    @InjectMocks
    private StringToJsonObjectConverter stringToJsonObjectConverterLocal;

    @BeforeEach
    public void setUp() {
        setField(this.objectToJsonObjectConverter, "mapper", new ObjectMapperProducer().objectMapper());
        setField(this.jsonObjectToObjectConverter, "objectMapper", new ObjectMapperProducer().objectMapper());
    }

    @Test
    public void shouldHandleOffencesRemovedFromHearingEvent() {
        final UUID hearingId = randomUUID();
        final UUID case1Id = randomUUID();
        final UUID case1sDefendant1Id = randomUUID();
        final UUID case1sDefendant2Id = randomUUID();
        final UUID case1sDefendant1sOffenceId = randomUUID();
        final UUID case1sDefendant2sOffenceId = randomUUID();
        final UUID case2Id = randomUUID();
        final UUID case2sDefendant1Id = randomUUID();
        final UUID case2sDefendant2Id = randomUUID();
        final UUID case2sDefendant1sOffenceId = randomUUID();
        final UUID case2sDefendant2sOffence1Id = randomUUID();
        final UUID case2sDefendant2sOffence2Id = randomUUID();
        final Hearing hearing = Hearing.hearing().withId((hearingId))
                .withProsecutionCases(new ArrayList<>(Arrays.asList(ProsecutionCase.prosecutionCase()
                                .withId(case1Id)
                                .withDefendants(new ArrayList<>(Arrays.asList(Defendant.defendant()
                                        .withId(case1sDefendant1Id)
                                        .withOffences(new ArrayList<>(Arrays.asList(Offence.offence().withId(case1sDefendant1sOffenceId).build())))
                                                .build(),
                                        Defendant.defendant()
                                                .withId(case1sDefendant2Id)
                                                .withOffences(new ArrayList<>(Arrays.asList(Offence.offence().withId(case1sDefendant2sOffenceId).build())))
                                                .build())))
                                .build(),
                        ProsecutionCase.prosecutionCase()
                                .withId(case2Id)
                                .withDefendants(new ArrayList<>(Arrays.asList(Defendant.defendant()
                                                .withId(case2sDefendant1Id)
                                                .withOffences(new ArrayList<>(Arrays.asList(Offence.offence().withId(case2sDefendant1sOffenceId).build())))
                                                .build(),
                                        Defendant.defendant()
                                                .withId(case2sDefendant2Id)
                                                .withOffences(new ArrayList<>(Arrays.asList(
                                                        Offence.offence().withId(case2sDefendant2sOffence1Id).build(),
                                                        Offence.offence().withId(case2sDefendant2sOffence2Id).build())))
                                                .build())))
                                .build()
                ))).build();

        final String payload = objectToJsonObjectConverter.convert(hearing).toString();
        final HearingEntity hearingEntity = new HearingEntity();
        hearingEntity.setHearingId(hearingId);
        hearingEntity.setPayload(payload);

        final OffencesRemovedFromHearing offencesRemovedFromHearing = OffencesRemovedFromHearing.offencesRemovedFromHearing()
                .withHearingId(hearingId)
                .withProsecutionCaseIds(Arrays.asList(case1Id))
                .withDefendantIds(Arrays.asList(case1sDefendant1Id, case1sDefendant2Id, case2sDefendant1Id))
                .withOffenceIds(Arrays.asList(case1sDefendant1sOffenceId,case1sDefendant2sOffenceId,case2sDefendant1sOffenceId, case2sDefendant2sOffence1Id))
                .build();

        when(hearingRepository.findBy(hearingId)).thenReturn(hearingEntity);
        when(offencesRemovedFromHearingEnvelope.payload()).thenReturn(offencesRemovedFromHearing);

        hearingUnallocatedEventListener.handleOffencesRemovedFromHearingEvent(offencesRemovedFromHearingEnvelope);

        verify(hearingRepository).save(hearingEntityArgumentCaptor.capture());

        final HearingEntity hearingEntityArgumentCaptorValue = hearingEntityArgumentCaptor.getValue();
        final Hearing hearingCaptor = jsonObjectToObjectConverter.convert(stringToJsonObjectConverterLocal.convert(hearingEntityArgumentCaptorValue.getPayload()), Hearing.class);

        assertThat(hearingCaptor.getProsecutionCases().size(), is(1));
        final ProsecutionCase prosecutionCase = hearingCaptor.getProsecutionCases().get(0);
        assertThat(prosecutionCase.getId(), is(case2Id));
        assertThat(prosecutionCase.getDefendants().size(), is(1));
        final Defendant defendant = prosecutionCase.getDefendants().get(0);
        assertThat(defendant.getId(), is(case2sDefendant2Id));
        assertThat(defendant.getOffences().get(0).getId(), is(case2sDefendant2sOffence2Id));
        ArgumentCaptor<UUID> defendantIdsArgumentCaptor = ArgumentCaptor.forClass(UUID.class);
        verify(caseDefendantHearingRepository, times(3)).removeByHearingIdAndDefendantId(eq(hearingId), defendantIdsArgumentCaptor.capture());

        assertThat(defendantIdsArgumentCaptor.getAllValues().containsAll(Arrays.asList(case1sDefendant1Id, case1sDefendant2Id, case2sDefendant1Id)), is(true));
    }

}
