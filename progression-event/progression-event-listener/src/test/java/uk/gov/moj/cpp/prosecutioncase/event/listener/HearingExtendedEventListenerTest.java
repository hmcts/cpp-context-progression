package uk.gov.moj.cpp.prosecutioncase.event.listener;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.UUID.randomUUID;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;

import uk.gov.justice.core.courts.Defendant;
import uk.gov.justice.core.courts.Hearing;
import uk.gov.justice.core.courts.HearingExtended;
import uk.gov.justice.core.courts.HearingListingNeeds;
import uk.gov.justice.core.courts.HearingListingStatus;
import uk.gov.justice.core.courts.Offence;
import uk.gov.justice.core.courts.ProsecutionCase;
import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.common.converter.ObjectToJsonObjectConverter;
import uk.gov.justice.services.common.converter.StringToJsonObjectConverter;
import uk.gov.justice.services.common.converter.jackson.ObjectMapperProducer;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.test.utils.core.messaging.MetadataBuilderFactory;
import uk.gov.moj.cpp.application.event.listener.CourtApplicationEventListenerTest;
import uk.gov.moj.cpp.prosecutioncase.persistence.entity.CaseDefendantHearingEntity;
import uk.gov.moj.cpp.prosecutioncase.persistence.entity.HearingEntity;
import uk.gov.moj.cpp.prosecutioncase.persistence.repository.CaseDefendantHearingRepository;
import uk.gov.moj.cpp.prosecutioncase.persistence.repository.HearingRepository;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class HearingExtendedEventListenerTest {

    @Spy
    private final ObjectMapper objectMapper = new ObjectMapperProducer().objectMapper();

    @Mock
    private HearingRepository hearingRepository;
    @Mock
    CaseDefendantHearingRepository caseDefendantHearingRepository;
    @Spy
    private JsonObjectToObjectConverter jsonObjectToObjectConverter = new JsonObjectToObjectConverter(objectMapper);
    @Spy
    private ObjectToJsonObjectConverter objectToJsonObjectConverter = new ObjectToJsonObjectConverter(objectMapper);

    private final StringToJsonObjectConverter stringToJsonObjectConverter = new StringToJsonObjectConverter();

    @InjectMocks
    private HearingExtendedEventListener hearingExtendedEventListener;

    @Captor
    private ArgumentCaptor<HearingEntity> hearingArgumentCaptor;

    private UUID hearingId;
    private UUID prosecutionCaseId;
    private UUID defendantId;
    private String hearingPayload;
    private String hearingPayloadWithSameCaseWithDifferentDefendant;


    @BeforeEach
    public void setup() throws IOException {
        hearingId = randomUUID();
        prosecutionCaseId = randomUUID();
        defendantId = randomUUID();
        hearingPayload = createPayload("/json/hearingDataProsecutionCase.json");
        hearingPayloadWithSameCaseWithDifferentDefendant = createPayload("/json/hearingDataWithSameCaseWithDifferentDefendant.json");
    }

    @Test
    public void shouldCallRemoveForHearingExtendedForCase() {

        final UUID extendedFromHearingId = randomUUID();
        final HearingExtended hearingExtended = createHearingExtended(hearingId, extendedFromHearingId, prosecutionCaseId, defendantId, false, false);
        final HearingEntity hearingEntity = createHearingEntity();

        when(hearingRepository.findBy(hearingId)).thenReturn(hearingEntity);
        final JsonEnvelope jsonEnvelope = envelopeFrom(
                MetadataBuilderFactory.metadataWithRandomUUID("progression.event.hearing-extended"),
                objectToJsonObjectConverter.convert(hearingExtended));
        hearingExtendedEventListener.hearingExtendedForCase(jsonEnvelope);

        verify(hearingRepository, times(1)).findBy(hearingId);
        verify(caseDefendantHearingRepository, times(1)).findByHearingIdAndCaseIdAndDefendantId(extendedFromHearingId, prosecutionCaseId, defendantId);
        verify(caseDefendantHearingRepository, times(1)).remove(any());
        verify(caseDefendantHearingRepository, times(1)).save(any());
    }

    @Test
    public void shouldAddNewDefendantWhenHearingExtended() {
        final UUID case1Id = randomUUID();
        final UUID def1_1Id = randomUUID();
        final UUID def1_2Id = randomUUID();
        final UUID case2Id = randomUUID();
        final UUID def2_1Id = randomUUID();

        final Hearing dbHearing = createHearing(hearingId, Map.of(case1Id, asList(Map.of(def1_1Id, asList(randomUUID(), randomUUID()))),
                case2Id, asList(Map.of(def2_1Id, asList(randomUUID(), randomUUID())))
        ));

        final HearingEntity hearingEntity = createHearingEntity(objectToJsonObjectConverter.convert(dbHearing).toString());


        final UUID extendedFromHearingId = randomUUID();
        final HearingExtended hearingExtended = createHearingExtended(hearingId, extendedFromHearingId,
                Map.of(case1Id, asList(Map.of(def1_1Id, asList(randomUUID(), randomUUID()))),
                case2Id, asList(Map.of(def2_1Id, asList(randomUUID(), randomUUID())))),
                Map.of(case1Id, asList(Map.of(def1_2Id, asList(randomUUID(), randomUUID())))) , false, false);

        final JsonEnvelope jsonEnvelope = envelopeFrom(
                MetadataBuilderFactory.metadataWithRandomUUID("progression.event.hearing-extended"),
                objectToJsonObjectConverter.convert(hearingExtended));

        when(hearingRepository.findBy(hearingId)).thenReturn(hearingEntity);
        hearingExtendedEventListener.hearingExtendedForCase(jsonEnvelope);

        verify(hearingRepository, times(1)).findBy(hearingId);
        verify(hearingRepository).save(hearingArgumentCaptor.capture());

        final Hearing savedHearing = jsonObjectToObjectConverter.convert(stringToJsonObjectConverter.convert(hearingArgumentCaptor.getValue().getPayload()), Hearing.class);

        assertThat(savedHearing.getProsecutionCases().size(), is(2));
        final ProsecutionCase case1 = savedHearing.getProsecutionCases().stream().filter(pc -> pc.getId().equals(case1Id)).findFirst().get();
        final ProsecutionCase case2 = savedHearing.getProsecutionCases().stream().filter(pc -> pc.getId().equals(case2Id)).findFirst().get();
        final Defendant def1_1 = case1.getDefendants().stream().filter(def -> def.getId().equals(def1_1Id)).findFirst().get();
        final Defendant def1_2 = case1.getDefendants().stream().filter(def -> def.getId().equals(def1_2Id)).findFirst().get();
        final Defendant def2_1 = case2.getDefendants().stream().filter(def -> def.getId().equals(def2_1Id)).findFirst().get();

        assertThat(case1.getDefendants().size(), is(2));
        assertThat(def1_1.getOffences().size(), is(4));
        assertThat(def1_2.getOffences().size(), is(2));
        assertThat(case2.getDefendants().size(), is(1));
        assertThat(def2_1.getOffences().size(), is(4));
    }

    @Test
    public void shouldAddNewCaseWhenHearingExtended() {
        final UUID case1Id = randomUUID();
        final UUID def1_1Id = randomUUID();
        final UUID def1_2Id = randomUUID();
        final UUID case2Id = randomUUID();
        final UUID def2_1Id = randomUUID();
        final UUID case3Id = randomUUID();
        final UUID def3_1Id = randomUUID();

        final Hearing dbHearing = createHearing(hearingId, Map.of(case1Id, asList(Map.of(def1_1Id, asList(randomUUID(), randomUUID()))),
                case2Id, asList(Map.of(def2_1Id, asList(randomUUID(), randomUUID())))
        ));

        final HearingEntity hearingEntity = createHearingEntity(objectToJsonObjectConverter.convert(dbHearing).toString());


        final UUID extendedFromHearingId = randomUUID();
        final HearingExtended hearingExtended = createHearingExtended(hearingId, extendedFromHearingId,
                Map.of(case1Id, asList(Map.of(def1_1Id, asList(randomUUID(), randomUUID()))),
                        case2Id, asList(Map.of(def2_1Id, asList(randomUUID(), randomUUID())))),
                Map.of(case1Id, asList(Map.of(def1_2Id, asList(randomUUID(), randomUUID()))),
                        case3Id, asList(Map.of(def3_1Id, asList(randomUUID(), randomUUID())))) , false, false);

        final JsonEnvelope jsonEnvelope = envelopeFrom(
                MetadataBuilderFactory.metadataWithRandomUUID("progression.event.hearing-extended"),
                objectToJsonObjectConverter.convert(hearingExtended));

        when(hearingRepository.findBy(hearingId)).thenReturn(hearingEntity);
        hearingExtendedEventListener.hearingExtendedForCase(jsonEnvelope);

        verify(hearingRepository, times(1)).findBy(hearingId);
        verify(hearingRepository).save(hearingArgumentCaptor.capture());

        final Hearing savedHearing = jsonObjectToObjectConverter.convert(stringToJsonObjectConverter.convert(hearingArgumentCaptor.getValue().getPayload()), Hearing.class);

        assertThat(savedHearing.getProsecutionCases().size(), is(3));
        final ProsecutionCase case1 = savedHearing.getProsecutionCases().stream().filter(pc -> pc.getId().equals(case1Id)).findFirst().get();
        final ProsecutionCase case2 = savedHearing.getProsecutionCases().stream().filter(pc -> pc.getId().equals(case2Id)).findFirst().get();
        final ProsecutionCase case3 = savedHearing.getProsecutionCases().stream().filter(pc -> pc.getId().equals(case3Id)).findFirst().get();
        final Defendant def1_1 = case1.getDefendants().stream().filter(def -> def.getId().equals(def1_1Id)).findFirst().get();
        final Defendant def1_2 = case1.getDefendants().stream().filter(def -> def.getId().equals(def1_2Id)).findFirst().get();
        final Defendant def2_1 = case2.getDefendants().stream().filter(def -> def.getId().equals(def2_1Id)).findFirst().get();
        final Defendant def3_1 = case3.getDefendants().stream().filter(def -> def.getId().equals(def3_1Id)).findFirst().get();

        assertThat(case1.getDefendants().size(), is(2));
        assertThat(def1_1.getOffences().size(), is(4));
        assertThat(def1_2.getOffences().size(), is(2));
        assertThat(case2.getDefendants().size(), is(1));
        assertThat(def2_1.getOffences().size(), is(4));
        assertThat(case3.getDefendants().size(), is(1));
        assertThat(def3_1.getOffences().size(), is(2));
    }


    @Test
    public void shouldKeepCaseIfTheCaseIsNotInRequestWhenHearingExtended() {
        final UUID case1Id = randomUUID();
        final UUID def1_1Id = randomUUID();
        final UUID def1_2Id = randomUUID();
        final UUID case2Id = randomUUID();
        final UUID def2_1Id = randomUUID();
        final UUID case3Id = randomUUID();
        final UUID def3_1Id = randomUUID();

        final Hearing dbHearing = createHearing(hearingId, Map.of(case1Id, asList(Map.of(def1_1Id, asList(randomUUID(), randomUUID()))),
                case2Id, asList(Map.of(def2_1Id, asList(randomUUID(), randomUUID())))
        ));

        final HearingEntity hearingEntity = createHearingEntity(objectToJsonObjectConverter.convert(dbHearing).toString());


        final UUID extendedFromHearingId = randomUUID();
        final HearingExtended hearingExtended = createHearingExtended(hearingId, extendedFromHearingId,
                Map.of(case1Id, asList(Map.of(def1_1Id, asList(randomUUID(), randomUUID())))),
                Map.of(case1Id, asList(Map.of(def1_2Id, asList(randomUUID(), randomUUID()))),
                        case3Id, asList(Map.of(def3_1Id, asList(randomUUID(), randomUUID())))) , false, false);

        final JsonEnvelope jsonEnvelope = envelopeFrom(
                MetadataBuilderFactory.metadataWithRandomUUID("progression.event.hearing-extended"),
                objectToJsonObjectConverter.convert(hearingExtended));

        when(hearingRepository.findBy(hearingId)).thenReturn(hearingEntity);
        hearingExtendedEventListener.hearingExtendedForCase(jsonEnvelope);

        verify(hearingRepository, times(1)).findBy(hearingId);
        verify(hearingRepository).save(hearingArgumentCaptor.capture());

        final Hearing savedHearing = jsonObjectToObjectConverter.convert(stringToJsonObjectConverter.convert(hearingArgumentCaptor.getValue().getPayload()), Hearing.class);

        assertThat(savedHearing.getProsecutionCases().size(), is(3));
        final ProsecutionCase case1 = savedHearing.getProsecutionCases().stream().filter(pc -> pc.getId().equals(case1Id)).findFirst().get();
        final ProsecutionCase case2 = savedHearing.getProsecutionCases().stream().filter(pc -> pc.getId().equals(case2Id)).findFirst().get();
        final ProsecutionCase case3 = savedHearing.getProsecutionCases().stream().filter(pc -> pc.getId().equals(case3Id)).findFirst().get();
        final Defendant def1_1 = case1.getDefendants().stream().filter(def -> def.getId().equals(def1_1Id)).findFirst().get();
        final Defendant def1_2 = case1.getDefendants().stream().filter(def -> def.getId().equals(def1_2Id)).findFirst().get();
        final Defendant def2_1 = case2.getDefendants().stream().filter(def -> def.getId().equals(def2_1Id)).findFirst().get();
        final Defendant def3_1 = case3.getDefendants().stream().filter(def -> def.getId().equals(def3_1Id)).findFirst().get();

        assertThat(case1.getDefendants().size(), is(2));
        assertThat(def1_1.getOffences().size(), is(4));
        assertThat(def1_2.getOffences().size(), is(2));
        assertThat(case2.getDefendants().size(), is(1));
        assertThat(def2_1.getOffences().size(), is(2));
        assertThat(case3.getDefendants().size(), is(1));
        assertThat(def3_1.getOffences().size(), is(2));    }

    @Test
    public void shouldKeepDefendantIfTheDefendantIsNotInRequestWhenHearingExtended() {
        final UUID case1Id = randomUUID();
        final UUID def1_1Id = randomUUID();
        final UUID def1_2Id = randomUUID();
        final UUID case2Id = randomUUID();
        final UUID def2_1Id = randomUUID();
        final UUID case3Id = randomUUID();
        final UUID def3_1Id = randomUUID();

        final Hearing dbHearing = createHearing(hearingId, Map.of(case1Id, asList(Map.of(def1_1Id, asList(randomUUID(), randomUUID())), Map.of(def1_2Id, asList(randomUUID(), randomUUID()))),
                case2Id, asList(Map.of(def2_1Id, asList(randomUUID(), randomUUID())))
        ));

        final HearingEntity hearingEntity = createHearingEntity(objectToJsonObjectConverter.convert(dbHearing).toString());


        final UUID extendedFromHearingId = randomUUID();
        final HearingExtended hearingExtended = createHearingExtended(hearingId, extendedFromHearingId,
                Map.of(case1Id, asList(Map.of(def1_1Id, asList(randomUUID(), randomUUID())))),
                Map.of(case3Id, asList(Map.of(def3_1Id, asList(randomUUID(), randomUUID())))) , false, false);

        final JsonEnvelope jsonEnvelope = envelopeFrom(
                MetadataBuilderFactory.metadataWithRandomUUID("progression.event.hearing-extended"),
                objectToJsonObjectConverter.convert(hearingExtended));

        when(hearingRepository.findBy(hearingId)).thenReturn(hearingEntity);
        hearingExtendedEventListener.hearingExtendedForCase(jsonEnvelope);

        verify(hearingRepository, times(1)).findBy(hearingId);
        verify(hearingRepository).save(hearingArgumentCaptor.capture());

        final Hearing savedHearing = jsonObjectToObjectConverter.convert(stringToJsonObjectConverter.convert(hearingArgumentCaptor.getValue().getPayload()), Hearing.class);

        assertThat(savedHearing.getProsecutionCases().size(), is(3));
        final ProsecutionCase case1 = savedHearing.getProsecutionCases().stream().filter(pc -> pc.getId().equals(case1Id)).findFirst().get();
        final ProsecutionCase case2 = savedHearing.getProsecutionCases().stream().filter(pc -> pc.getId().equals(case2Id)).findFirst().get();
        final ProsecutionCase case3 = savedHearing.getProsecutionCases().stream().filter(pc -> pc.getId().equals(case3Id)).findFirst().get();
        final Defendant def1_1 = case1.getDefendants().stream().filter(def -> def.getId().equals(def1_1Id)).findFirst().get();
        final Defendant def1_2 = case1.getDefendants().stream().filter(def -> def.getId().equals(def1_2Id)).findFirst().get();
        final Defendant def2_1 = case2.getDefendants().stream().filter(def -> def.getId().equals(def2_1Id)).findFirst().get();
        final Defendant def3_1 = case3.getDefendants().stream().filter(def -> def.getId().equals(def3_1Id)).findFirst().get();

        assertThat(case1.getDefendants().size(), is(2));
        assertThat(def1_1.getOffences().size(), is(4));
        assertThat(def1_2.getOffences().size(), is(2));
        assertThat(case2.getDefendants().size(), is(1));
        assertThat(def2_1.getOffences().size(), is(2));
        assertThat(case3.getDefendants().size(), is(1));
        assertThat(def3_1.getOffences().size(), is(2));
    }

    @Test
    public void shouldKeepOffenceIfTheOffenceIsNotInRequestWhenHearingExtended() {
        final UUID case1Id = randomUUID();
        final UUID def1_1Id = randomUUID();
        final UUID def1_off1Id = randomUUID();
        final UUID def1_off2Id = randomUUID();
        final UUID def1_off3Id = randomUUID();
        final UUID def1_off4Id = randomUUID();
        final UUID def1_2Id = randomUUID();
        final UUID case2Id = randomUUID();
        final UUID def2_1Id = randomUUID();
        final UUID case3Id = randomUUID();
        final UUID def3_1Id = randomUUID();

        final Hearing dbHearing = createHearing(hearingId, Map.of(case1Id, asList(Map.of(def1_1Id, asList(def1_off1Id,def1_off2Id)), Map.of(def1_2Id, asList(randomUUID(), randomUUID()))),
                case2Id, asList(Map.of(def2_1Id, asList(randomUUID(), randomUUID())))
        ));

        final HearingEntity hearingEntity = createHearingEntity(objectToJsonObjectConverter.convert(dbHearing).toString());


        final UUID extendedFromHearingId = randomUUID();
        final HearingExtended hearingExtended = createHearingExtended(hearingId, extendedFromHearingId,
                Map.of(case1Id, asList(Map.of(def1_1Id, asList(def1_off1Id, def1_off1Id)))),
                Map.of(case3Id, asList(Map.of(def3_1Id, asList(randomUUID(), randomUUID()))),case1Id, asList(Map.of(def1_1Id, asList(def1_off3Id,def1_off4Id)))) , false, false);

        final JsonEnvelope jsonEnvelope = envelopeFrom(
                MetadataBuilderFactory.metadataWithRandomUUID("progression.event.hearing-extended"),
                objectToJsonObjectConverter.convert(hearingExtended));

        when(hearingRepository.findBy(hearingId)).thenReturn(hearingEntity);
        hearingExtendedEventListener.hearingExtendedForCase(jsonEnvelope);

        verify(hearingRepository, times(1)).findBy(hearingId);
        verify(hearingRepository).save(hearingArgumentCaptor.capture());

        final Hearing savedHearing = jsonObjectToObjectConverter.convert(stringToJsonObjectConverter.convert(hearingArgumentCaptor.getValue().getPayload()), Hearing.class);

        assertThat(savedHearing.getProsecutionCases().size(), is(3));
        final ProsecutionCase case1 = savedHearing.getProsecutionCases().stream().filter(pc -> pc.getId().equals(case1Id)).findFirst().get();
        final ProsecutionCase case2 = savedHearing.getProsecutionCases().stream().filter(pc -> pc.getId().equals(case2Id)).findFirst().get();
        final ProsecutionCase case3 = savedHearing.getProsecutionCases().stream().filter(pc -> pc.getId().equals(case3Id)).findFirst().get();
        final Defendant def1_1 = case1.getDefendants().stream().filter(def -> def.getId().equals(def1_1Id)).findFirst().get();
        final Defendant def1_2 = case1.getDefendants().stream().filter(def -> def.getId().equals(def1_2Id)).findFirst().get();
        final Defendant def2_1 = case2.getDefendants().stream().filter(def -> def.getId().equals(def2_1Id)).findFirst().get();
        final Defendant def3_1 = case3.getDefendants().stream().filter(def -> def.getId().equals(def3_1Id)).findFirst().get();

        assertThat(case1.getDefendants().size(), is(2));
        assertThat(def1_1.getOffences().size(), is(4));
        assertThat(def1_1.getOffences().stream().anyMatch(off -> off.getId().equals(def1_off1Id)), is(true));
        assertThat(def1_1.getOffences().stream().anyMatch(off -> off.getId().equals(def1_off2Id)), is(true));
        assertThat(def1_1.getOffences().stream().anyMatch(off -> off.getId().equals(def1_off3Id)), is(true));
        assertThat(def1_1.getOffences().stream().anyMatch(off -> off.getId().equals(def1_off4Id)), is(true));
        assertThat(def1_2.getOffences().size(), is(2));
        assertThat(case2.getDefendants().size(), is(1));
        assertThat(def2_1.getOffences().size(), is(2));
        assertThat(case3.getDefendants().size(), is(1));
        assertThat(def3_1.getOffences().size(), is(2));
    }

    @Test
    public void shouldNotCallRemoveForHearingExtendedForCaseIsAdjournedIsTrue() {

        final UUID extendedFromHearingId = randomUUID();
        HearingExtended hearingExtended = createHearingExtended(hearingId, extendedFromHearingId, prosecutionCaseId, defendantId, true, false);
        final HearingEntity hearingEntity = createHearingEntity();

        when(hearingRepository.findBy(hearingId)).thenReturn(hearingEntity);
        final JsonEnvelope jsonEnvelope = envelopeFrom(
                MetadataBuilderFactory.metadataWithRandomUUID("progression.event.hearing-extended"),
                objectToJsonObjectConverter.convert(hearingExtended));
        hearingExtendedEventListener.hearingExtendedForCase(jsonEnvelope);

        verify(hearingRepository, times(1)).findBy(hearingId);
        verify(caseDefendantHearingRepository, never()).findByHearingIdAndCaseIdAndDefendantId(extendedFromHearingId, prosecutionCaseId, defendantId);
        verify(caseDefendantHearingRepository, never()).remove(any(CaseDefendantHearingEntity.class));
        verify(caseDefendantHearingRepository, times(1)).save(any(CaseDefendantHearingEntity.class));
    }

    @Test
    public void shouldNotCallRemoveForHearingExtendedForCaseIsPartiallyAllocatedIsTrue() {

        final UUID extendedFromHearingId = randomUUID();
        HearingExtended hearingExtended = createHearingExtended(hearingId, extendedFromHearingId, prosecutionCaseId, defendantId, false, true);
        final HearingEntity hearingEntity = createHearingEntity();

        when(hearingRepository.findBy(hearingId)).thenReturn(hearingEntity);
        final JsonEnvelope jsonEnvelope = envelopeFrom(
                MetadataBuilderFactory.metadataWithRandomUUID("progression.event.hearing-extended"),
                objectToJsonObjectConverter.convert(hearingExtended));
        hearingExtendedEventListener.hearingExtendedForCase(jsonEnvelope);

        verify(hearingRepository, times(1)).findBy(hearingId);
        verify(caseDefendantHearingRepository, never()).findByHearingIdAndCaseIdAndDefendantId(extendedFromHearingId, prosecutionCaseId, defendantId);
        verify(caseDefendantHearingRepository, never()).remove(any(CaseDefendantHearingEntity.class));
        verify(caseDefendantHearingRepository, times(1)).save(any(CaseDefendantHearingEntity.class));
    }

    @Test
    public void shouldNotCallRemoveForHearingExtendedForCaseIsAdjournedAndIsPartiallyAllocatedAreTrue() {

        final UUID extendedFromHearingId = randomUUID();
        HearingExtended hearingExtended = createHearingExtended(hearingId, extendedFromHearingId, prosecutionCaseId, defendantId, true, true);
        final HearingEntity hearingEntity = createHearingEntity();

        when(hearingRepository.findBy(hearingId)).thenReturn(hearingEntity);
        final JsonEnvelope jsonEnvelope = envelopeFrom(
                MetadataBuilderFactory.metadataWithRandomUUID("progression.event.hearing-extended"),
                objectToJsonObjectConverter.convert(hearingExtended));

        hearingExtendedEventListener.hearingExtendedForCase(jsonEnvelope);

        verify(hearingRepository, times(1)).findBy(hearingId);
        verify(caseDefendantHearingRepository, never()).findByHearingIdAndCaseIdAndDefendantId(extendedFromHearingId, prosecutionCaseId, defendantId);
        verify(caseDefendantHearingRepository, never()).remove(any(CaseDefendantHearingEntity.class));
        verify(caseDefendantHearingRepository, times(1)).save(any(CaseDefendantHearingEntity.class));
    }

    @Test
    public void shouldNotCallRemoveForHearingExtendedForCaseIsPartiallyAllocatedIsNull() {

        final UUID extendedFromHearingId = randomUUID();
        HearingExtended hearingExtended = createHearingExtended(hearingId, extendedFromHearingId, prosecutionCaseId, defendantId, false, null);
        final HearingEntity hearingEntity = createHearingEntity();

        when(hearingRepository.findBy(hearingId)).thenReturn(hearingEntity);
        final JsonEnvelope jsonEnvelope = envelopeFrom(
                MetadataBuilderFactory.metadataWithRandomUUID("progression.event.hearing-extended"),
                objectToJsonObjectConverter.convert(hearingExtended));
        hearingExtendedEventListener.hearingExtendedForCase(jsonEnvelope);

        verify(hearingRepository, times(1)).findBy(hearingId);
        verify(caseDefendantHearingRepository, never()).findByHearingIdAndCaseIdAndDefendantId(extendedFromHearingId, prosecutionCaseId, defendantId);
        verify(caseDefendantHearingRepository, never()).remove(any(CaseDefendantHearingEntity.class));
        verify(caseDefendantHearingRepository, times(1)).save(any(CaseDefendantHearingEntity.class));
    }

    @Test
    public void shouldNotCallRemoveForHearingExtendedForCaseIsAdjournedIsNull() {

        final UUID extendedFromHearingId = randomUUID();
        HearingExtended hearingExtended = createHearingExtended(hearingId, extendedFromHearingId, prosecutionCaseId, defendantId, null, false);
        final HearingEntity hearingEntity = createHearingEntity();

        when(hearingRepository.findBy(hearingId)).thenReturn(hearingEntity);
        final JsonEnvelope jsonEnvelope = envelopeFrom(
                MetadataBuilderFactory.metadataWithRandomUUID("progression.event.hearing-extended"),
                objectToJsonObjectConverter.convert(hearingExtended));
        hearingExtendedEventListener.hearingExtendedForCase(jsonEnvelope);

        verify(hearingRepository, times(1)).findBy(hearingId);
        verify(caseDefendantHearingRepository, never()).findByHearingIdAndCaseIdAndDefendantId(extendedFromHearingId, prosecutionCaseId, defendantId);
        verify(caseDefendantHearingRepository, never()).remove(any(CaseDefendantHearingEntity.class));
        verify(caseDefendantHearingRepository, times(1)).save(any(CaseDefendantHearingEntity.class));
    }

    @Test
    public void shouldHandleHearingExtendedForCaseWithAddedOffencesInSameDefendantAndCaseWhenCaseSplitAtOffenceLevelWithMultipleOffencesAndMergedBack() {

        final UUID caseId = UUID.fromString("d8161e2b-4aa7-4a8e-992a-11ff01c471e6");

        final UUID extendedFromHearingId = randomUUID();
        final HearingExtended hearingExtended = createHearingExtended(hearingId, extendedFromHearingId, caseId, defendantId, false, false);

        final Hearing dbHearing = Hearing.hearing()
                .withProsecutionCases(asList(
                        ProsecutionCase.prosecutionCase()
                                .withId(UUID.fromString("d8161e2b-4aa7-4a8e-992a-11ff01c471e6"))
                                .withDefendants(asList(Defendant.defendant().withId(defendantId)
                                        .withOffences(asList(Offence.offence().withId(randomUUID()).build()))
                                        .build()))

                                .build())).build();
        final HearingEntity hearingEntity = createHearingEntity(objectToJsonObjectConverter.convert(dbHearing).toString());


        when(hearingRepository.findBy(hearingId)).thenReturn(hearingEntity);
        final JsonEnvelope jsonEnvelope = envelopeFrom(
                MetadataBuilderFactory.metadataWithRandomUUID("progression.event.hearing-extended"),
                objectToJsonObjectConverter.convert(hearingExtended));
        hearingExtendedEventListener.hearingExtendedForCase(jsonEnvelope);

        verify(hearingRepository, times(1)).findBy(hearingId);
        verify(caseDefendantHearingRepository, times(1)).findByHearingIdAndCaseIdAndDefendantId(extendedFromHearingId,  caseId, defendantId);
        verify(caseDefendantHearingRepository, times(1)).remove(any());
        verify(caseDefendantHearingRepository, times(1)).save(any());
    }

    @Test
    public void shouldHandleHearingExtendedForCaseWithAddedDefendantsInSameDefendantAndCaseWhenCaseSplitAtDefendantLevelAndMergedBack() {

        final UUID caseId = UUID.fromString("d8161e2b-4aa7-4a8e-992a-11ff01c471e6");
        final UUID defendantId2 = randomUUID();

        final UUID extendedFromHearingId = randomUUID();
        final HearingExtended hearingExtended = createHearingExtended(hearingId, extendedFromHearingId, caseId, defendantId2, false, false);

        final Hearing dbHearing = Hearing.hearing()
                .withProsecutionCases(asList(
                        ProsecutionCase.prosecutionCase()
                                .withId(UUID.fromString("d8161e2b-4aa7-4a8e-992a-11ff01c471e6"))
                                .withDefendants(asList(Defendant.defendant().withId(defendantId)
                                        .withOffences(asList(Offence.offence().withId(randomUUID()).build()))
                                        .build()))

                                .build())).build();

        final HearingEntity hearingEntity = createHearingEntity(objectToJsonObjectConverter.convert(dbHearing).toString());
        when(hearingRepository.findBy(hearingId)).thenReturn(hearingEntity);
        final JsonEnvelope jsonEnvelope = envelopeFrom(
                MetadataBuilderFactory.metadataWithRandomUUID("progression.event.hearing-extended"),
                objectToJsonObjectConverter.convert(hearingExtended));
        hearingExtendedEventListener.hearingExtendedForCase(jsonEnvelope);

        verify(hearingRepository, times(1)).findBy(hearingId);
        verify(caseDefendantHearingRepository, times(1)).findByHearingIdAndCaseIdAndDefendantId(extendedFromHearingId,  caseId, defendantId2);
        verify(caseDefendantHearingRepository, times(1)).remove(any());
        verify(caseDefendantHearingRepository, times(1)).save(any());
    }

    @Test
    public void shouldHandleHearingExtendedForCaseWithAddedDefendantsInDifferentCaseWhenHearingSplitAtCaseLevelAndMergedBack() {

        final UUID caseId = randomUUID();
        final UUID defendantId2 = randomUUID();
        final UUID extendedFromHearingId = randomUUID();
        final HearingExtended hearingExtended = createHearingExtended(hearingId, extendedFromHearingId, prosecutionCaseId, defendantId, false, false);

        final Hearing dbHearing = Hearing.hearing()
                .withProsecutionCases(asList(
                        ProsecutionCase.prosecutionCase()
                                .withId(caseId)
                                .withDefendants(asList(Defendant.defendant().withId(defendantId2)
                                        .withOffences(asList(Offence.offence().withId(randomUUID()).build()))
                                        .build()))

                                .build())).build();
        final HearingEntity hearingEntity = createHearingEntity(objectToJsonObjectConverter.convert(dbHearing).toString());

        when(hearingRepository.findBy(hearingId)).thenReturn(hearingEntity);
        final JsonEnvelope jsonEnvelope = envelopeFrom(
                MetadataBuilderFactory.metadataWithRandomUUID("progression.event.hearing-extended"),
                objectToJsonObjectConverter.convert(hearingExtended));
        hearingExtendedEventListener.hearingExtendedForCase(jsonEnvelope);

        verify(hearingRepository, times(1)).findBy(hearingId);
        verify(caseDefendantHearingRepository, times(1)).findByHearingIdAndCaseIdAndDefendantId(extendedFromHearingId,  prosecutionCaseId, defendantId);
        verify(caseDefendantHearingRepository, times(1)).remove(any());
        verify(caseDefendantHearingRepository, times(1)).save(any());
    }


    private HearingEntity createHearingEntity() {
        final HearingEntity hearingEntity = new HearingEntity();
        hearingEntity.setListingStatus(HearingListingStatus.HEARING_INITIALISED);
        hearingEntity.setHearingId(UUID.randomUUID());
        hearingEntity.setResultLines(new HashSet<>());
        hearingEntity.setPayload(hearingPayload);
        return hearingEntity;

    }

    private HearingEntity createHearingEntity(final String  hearingPayload) {
        final HearingEntity hearingEntity = new HearingEntity();
        hearingEntity.setListingStatus(HearingListingStatus.HEARING_INITIALISED);
        hearingEntity.setHearingId(UUID.randomUUID());
        hearingEntity.setResultLines(new HashSet<>());
        hearingEntity.setPayload(hearingPayload);
        return hearingEntity;

    }

    private HearingExtended createHearingExtended(final UUID hearingId, final UUID extendedFromHearingId, final UUID prosecutionCaseId, final UUID defendantId,
                                                  final Boolean isAdjourned, final Boolean isPartiallyAllocated) {
        final Defendant defendant = Defendant.defendant()
                .withId(defendantId)
                .withOffences(asList(Offence.offence().withId(randomUUID()).build()))
                .build();

        final List<Defendant> defendantList = new ArrayList<>();
        defendantList.add(defendant);

        final ProsecutionCase prosecutionCase = ProsecutionCase.prosecutionCase()
                .withId(prosecutionCaseId)
                .withDefendants(defendantList)
                .build();

        final List<ProsecutionCase> prosecutionCaseList = new ArrayList<>();
        prosecutionCaseList.add(prosecutionCase);

        final HearingListingNeeds hearingListingNeeds = HearingListingNeeds.hearingListingNeeds()
                .withId(hearingId)
                .withProsecutionCases(prosecutionCaseList)
                .build();

       return HearingExtended.hearingExtended()
                .withHearingRequest(hearingListingNeeds)
                .withExtendedHearingFrom(extendedFromHearingId)
                .withIsAdjourned(isAdjourned)
                .withIsPartiallyAllocated(isPartiallyAllocated)
                .build();
    }

    private String createPayload(final String payloadPath) throws IOException {
        final StringWriter writer = new StringWriter();
        final InputStream inputStream = CourtApplicationEventListenerTest.class.getResourceAsStream(payloadPath);
        IOUtils.copy(inputStream, writer, UTF_8);
        inputStream.close();
        return writer.toString();
    }

    private Hearing createHearing(final UUID hearingId, final Map<UUID,List<Map<UUID, List<UUID>>>> cases){
        return Hearing.hearing()
                .withId(hearingId)
                .withProsecutionCases(cases.entrySet().stream().map(pCase -> ProsecutionCase.prosecutionCase()
                                .withId(pCase.getKey())
                                .withDefendants(pCase.getValue().stream().flatMap(v -> v.entrySet().stream())
                                        .map(defendant ->Defendant.defendant()
                                                .withId(defendant.getKey())
                                                .withOffences( defendant.getValue().stream().map(off -> Offence.offence()
                                                        .withId(off)
                                                        .build()).toList())
                                                .withProsecutionCaseId(pCase.getKey())
                                                .build())
                                        .toList())
                                .build())
                        .toList())
                .build();
    }

    private HearingExtended createHearingExtended(final UUID hearingId, final UUID extendedFromHearingId, final Map<UUID,List<Map<UUID, List<UUID>>>> cases,
                                                  final Map<UUID,List<Map<UUID, List<UUID>>>> newCases,
                                                   final Boolean isAdjourned, final Boolean isPartiallyAllocated) {

        final List<ProsecutionCase> caseList = new ArrayList<>();
        cases.entrySet().stream().map(pCase -> ProsecutionCase.prosecutionCase()
                        .withId(pCase.getKey())
                        .withDefendants(pCase.getValue().stream().flatMap(v -> v.entrySet().stream())
                                .map(defendant ->Defendant.defendant()
                                        .withId(defendant.getKey())
                                        .withOffences( defendant.getValue().stream().map(off -> Offence.offence()
                                                .withId(off)
                                                .build()).toList())
                                        .withProsecutionCaseId(pCase.getKey())
                                        .build())
                                .toList())
                        .build())
                .forEach(caseList::add);

        newCases.entrySet().stream().map(pCase -> ProsecutionCase.prosecutionCase()
                        .withId(pCase.getKey())
                        .withDefendants(pCase.getValue().stream().flatMap(v -> v.entrySet().stream())
                                .map(defendant ->Defendant.defendant()
                                        .withId(defendant.getKey())
                                        .withOffences( defendant.getValue().stream().map(off -> Offence.offence()
                                                .withId(off)
                                                .build()).toList())
                                        .withProsecutionCaseId(pCase.getKey())
                                        .build())
                                .toList())
                        .build())
                .forEach(caseList::add);

        final HearingListingNeeds hearingListingNeeds = HearingListingNeeds.hearingListingNeeds()
                .withId(hearingId)
                .withProsecutionCases(caseList)
                .build();

        return HearingExtended.hearingExtended()
                .withHearingRequest(hearingListingNeeds)
                .withExtendedHearingFrom(extendedFromHearingId)
                .withIsAdjourned(isAdjourned)
                .withIsPartiallyAllocated(isPartiallyAllocated)
                .build();
    }

    private <T> List<T> asList(T... a) {
        return new ArrayList<>(java.util.Arrays.asList(a));
    }
}
