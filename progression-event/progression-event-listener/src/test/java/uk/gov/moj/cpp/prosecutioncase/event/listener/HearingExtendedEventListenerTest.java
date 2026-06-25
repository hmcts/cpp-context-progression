package uk.gov.moj.cpp.prosecutioncase.event.listener;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.UUID.randomUUID;
import static org.codehaus.groovy.runtime.InvokerHelper.asList;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import uk.gov.justice.core.courts.Defendant;
import uk.gov.justice.core.courts.Hearing;
import uk.gov.justice.core.courts.HearingExtended;
import uk.gov.justice.core.courts.HearingListingNeeds;
import uk.gov.justice.core.courts.HearingListingStatus;
import uk.gov.justice.core.courts.Offence;
import uk.gov.justice.core.courts.ProsecutionCase;
import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.common.converter.ObjectToJsonObjectConverter;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.application.event.listener.CourtApplicationEventListenerTest;
import uk.gov.moj.cpp.prosecutioncase.persistence.entity.CaseDefendantHearingEntity;
import uk.gov.moj.cpp.prosecutioncase.persistence.entity.CaseDefendantHearingKey;
import uk.gov.moj.cpp.prosecutioncase.persistence.entity.HearingEntity;
import uk.gov.moj.cpp.prosecutioncase.persistence.repository.CaseDefendantHearingRepository;
import uk.gov.moj.cpp.prosecutioncase.persistence.repository.HearingRepository;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.UUID;

import uk.gov.justice.services.messaging.JsonObjects;
import javax.json.JsonObject;
import javax.json.JsonReader;

import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class HearingExtendedEventListenerTest {

    @Mock
    private HearingRepository hearingRepository;
    @Mock
    CaseDefendantHearingRepository caseDefendantHearingRepository;
    @InjectMocks
    private HearingExtendedEventListener hearingExtendedEventListener;
    @Mock
    private JsonObjectToObjectConverter jsonObjectToObjectConverter;
    @Mock
    private ObjectToJsonObjectConverter objectToJsonObjectConverter;
    @Mock
    private JsonEnvelope jsonEnvelope;
    @Mock
    private JsonObject jsonObject;
    @Mock
    private Hearing hearing;
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
        final List<CaseDefendantHearingEntity> caseDefendantHearingEntityList = new ArrayList<>();
        caseDefendantHearingEntityList.add(createCaseDefendantHearingEntity());

        when(jsonEnvelope.payloadAsJsonObject()).thenReturn(jsonObject);
        when(jsonObjectToObjectConverter.convert(any(JsonObject.class), any())).thenReturn(hearingExtended).thenReturn(hearing);
        when(hearingRepository.findBy(hearingId)).thenReturn(hearingEntity);
        when( objectToJsonObjectConverter.convert(any())).thenReturn(jsonObject);
        hearingExtendedEventListener.hearingExtendedForCase(jsonEnvelope);

        verify(hearingRepository, times(1)).findBy(hearingId);
        verify(jsonObjectToObjectConverter, times(1)).convert(jsonObject, HearingExtended.class);
        verify(caseDefendantHearingRepository, times(1)).findByHearingIdAndCaseIdAndDefendantId(extendedFromHearingId, prosecutionCaseId, defendantId);
        verify(caseDefendantHearingRepository, times(1)).remove(any());
        verify(caseDefendantHearingRepository, times(1)).save(any());
    }

    @Test
    public void shouldNotCallRemoveForHearingExtendedForCaseIsAdjournedIsTrue() {

        final UUID extendedFromHearingId = randomUUID();
        HearingExtended hearingExtended = createHearingExtended(hearingId, extendedFromHearingId, prosecutionCaseId, defendantId, true, false);
        final HearingEntity hearingEntity = createHearingEntity();
        final List<CaseDefendantHearingEntity> caseDefendantHearingEntityList = new ArrayList<>();
        caseDefendantHearingEntityList.add(createCaseDefendantHearingEntity());

        when(jsonEnvelope.payloadAsJsonObject()).thenReturn(jsonObject);
        when(jsonObjectToObjectConverter.convert(any(JsonObject.class), any())).thenReturn(hearingExtended).thenReturn(hearing);
        when(hearingRepository.findBy(hearingId)).thenReturn(hearingEntity);
        when( objectToJsonObjectConverter.convert(any())).thenReturn(jsonObject);
        hearingExtendedEventListener.hearingExtendedForCase(jsonEnvelope);

        verify(hearingRepository, times(1)).findBy(hearingId);
        verify(jsonObjectToObjectConverter, times(1)).convert(jsonObject, HearingExtended.class);
        verify(caseDefendantHearingRepository, never()).findByHearingIdAndCaseIdAndDefendantId(extendedFromHearingId, prosecutionCaseId, defendantId);
        verify(caseDefendantHearingRepository, never()).remove(any(CaseDefendantHearingEntity.class));
        verify(caseDefendantHearingRepository, times(1)).save(any(CaseDefendantHearingEntity.class));
    }

    @Test
    public void shouldNotCallRemoveForHearingExtendedForCaseIsPartiallyAllocatedIsTrue() {

        final UUID extendedFromHearingId = randomUUID();
        HearingExtended hearingExtended = createHearingExtended(hearingId, extendedFromHearingId, prosecutionCaseId, defendantId, false, true);
        final HearingEntity hearingEntity = createHearingEntity();
        final List<CaseDefendantHearingEntity> caseDefendantHearingEntityList = new ArrayList<>();
        caseDefendantHearingEntityList.add(createCaseDefendantHearingEntity());

        when(jsonEnvelope.payloadAsJsonObject()).thenReturn(jsonObject);
        when(jsonObjectToObjectConverter.convert(any(JsonObject.class), any())).thenReturn(hearingExtended).thenReturn(hearing);
        when(hearingRepository.findBy(hearingId)).thenReturn(hearingEntity);
        when( objectToJsonObjectConverter.convert(any())).thenReturn(jsonObject);
        hearingExtendedEventListener.hearingExtendedForCase(jsonEnvelope);

        verify(hearingRepository, times(1)).findBy(hearingId);
        verify(jsonObjectToObjectConverter, times(1)).convert(jsonObject, HearingExtended.class);
        verify(caseDefendantHearingRepository, never()).findByHearingIdAndCaseIdAndDefendantId(extendedFromHearingId, prosecutionCaseId, defendantId);
        verify(caseDefendantHearingRepository, never()).remove(any(CaseDefendantHearingEntity.class));
        verify(caseDefendantHearingRepository, times(1)).save(any(CaseDefendantHearingEntity.class));
    }

    @Test
    public void shouldNotCallRemoveForHearingExtendedForCaseIsAdjournedAndIsPartiallyAllocatedAreTrue() {

        final UUID extendedFromHearingId = randomUUID();
        HearingExtended hearingExtended = createHearingExtended(hearingId, extendedFromHearingId, prosecutionCaseId, defendantId, true, true);
        final HearingEntity hearingEntity = createHearingEntity();
        final List<CaseDefendantHearingEntity> caseDefendantHearingEntityList = new ArrayList<>();
        caseDefendantHearingEntityList.add(createCaseDefendantHearingEntity());

        when(jsonEnvelope.payloadAsJsonObject()).thenReturn(jsonObject);
        when(jsonObjectToObjectConverter.convert(any(JsonObject.class), any())).thenReturn(hearingExtended).thenReturn(hearing);
        when(hearingRepository.findBy(hearingId)).thenReturn(hearingEntity);
        when( objectToJsonObjectConverter.convert(any())).thenReturn(jsonObject);
        hearingExtendedEventListener.hearingExtendedForCase(jsonEnvelope);

        verify(hearingRepository, times(1)).findBy(hearingId);
        verify(jsonObjectToObjectConverter, times(1)).convert(jsonObject, HearingExtended.class);
        verify(caseDefendantHearingRepository, never()).findByHearingIdAndCaseIdAndDefendantId(extendedFromHearingId, prosecutionCaseId, defendantId);
        verify(caseDefendantHearingRepository, never()).remove(any(CaseDefendantHearingEntity.class));
        verify(caseDefendantHearingRepository, times(1)).save(any(CaseDefendantHearingEntity.class));
    }

    @Test
    public void shouldNotCallRemoveForHearingExtendedForCaseIsPartiallyAllocatedIsNull() {

        final UUID extendedFromHearingId = randomUUID();
        HearingExtended hearingExtended = createHearingExtended(hearingId, extendedFromHearingId, prosecutionCaseId, defendantId, false, null);
        final HearingEntity hearingEntity = createHearingEntity();
        final List<CaseDefendantHearingEntity> caseDefendantHearingEntityList = new ArrayList<>();
        caseDefendantHearingEntityList.add(createCaseDefendantHearingEntity());

        when(jsonEnvelope.payloadAsJsonObject()).thenReturn(jsonObject);
        when(jsonObjectToObjectConverter.convert(any(JsonObject.class), any())).thenReturn(hearingExtended).thenReturn(hearing);
        when(hearingRepository.findBy(hearingId)).thenReturn(hearingEntity);
        when( objectToJsonObjectConverter.convert(any())).thenReturn(jsonObject);
        hearingExtendedEventListener.hearingExtendedForCase(jsonEnvelope);

        verify(hearingRepository, times(1)).findBy(hearingId);
        verify(jsonObjectToObjectConverter, times(1)).convert(jsonObject, HearingExtended.class);
        verify(caseDefendantHearingRepository, never()).findByHearingIdAndCaseIdAndDefendantId(extendedFromHearingId, prosecutionCaseId, defendantId);
        verify(caseDefendantHearingRepository, never()).remove(any(CaseDefendantHearingEntity.class));
        verify(caseDefendantHearingRepository, times(1)).save(any(CaseDefendantHearingEntity.class));
    }

    @Test
    public void shouldNotCallRemoveForHearingExtendedForCaseIsAdjournedIsNull() {

        final UUID extendedFromHearingId = randomUUID();
        HearingExtended hearingExtended = createHearingExtended(hearingId, extendedFromHearingId, prosecutionCaseId, defendantId, null, false);
        final HearingEntity hearingEntity = createHearingEntity();
        final List<CaseDefendantHearingEntity> caseDefendantHearingEntityList = new ArrayList<>();
        caseDefendantHearingEntityList.add(createCaseDefendantHearingEntity());

        when(jsonEnvelope.payloadAsJsonObject()).thenReturn(jsonObject);
        when(jsonObjectToObjectConverter.convert(any(JsonObject.class), any())).thenReturn(hearingExtended).thenReturn(hearing);
        when(hearingRepository.findBy(hearingId)).thenReturn(hearingEntity);
        when( objectToJsonObjectConverter.convert(any())).thenReturn(jsonObject);
        hearingExtendedEventListener.hearingExtendedForCase(jsonEnvelope);

        verify(hearingRepository, times(1)).findBy(hearingId);
        verify(jsonObjectToObjectConverter, times(1)).convert(jsonObject, HearingExtended.class);
        verify(caseDefendantHearingRepository, never()).findByHearingIdAndCaseIdAndDefendantId(extendedFromHearingId, prosecutionCaseId, defendantId);
        verify(caseDefendantHearingRepository, never()).remove(any(CaseDefendantHearingEntity.class));
        verify(caseDefendantHearingRepository, times(1)).save(any(CaseDefendantHearingEntity.class));
    }

    @Test
    public void shouldHandleHearingExtendedForCaseWithAddedOffencesInSameDefendantAndCaseWhenCaseSplitAtOffenceLevelWithMultipleOffencesAndMergedBack() {

        final UUID caseId = UUID.fromString("d8161e2b-4aa7-4a8e-992a-11ff01c471e6");

        final UUID extendedFromHearingId = randomUUID();
        final HearingExtended hearingExtended = createHearingExtended(hearingId, extendedFromHearingId, caseId, defendantId, false, false);
        final HearingEntity hearingEntity = createHearingEntity(hearingPayloadWithSameCaseWithDifferentDefendant);
        final List<CaseDefendantHearingEntity> caseDefendantHearingEntityList = new ArrayList<>();
        caseDefendantHearingEntityList.add(createCaseDefendantHearingEntity(hearingPayloadWithSameCaseWithDifferentDefendant));

        final Hearing dbHearing = Hearing.hearing()
                .withProsecutionCases(asList(
                        ProsecutionCase.prosecutionCase()
                                .withId(UUID.fromString("d8161e2b-4aa7-4a8e-992a-11ff01c471e6"))
                                .withDefendants(asList(Defendant.defendant().withId(defendantId)
                                        .withOffences(asList(Offence.offence().withId(randomUUID()).build()))
                                        .build()))

                                .build())).build();


        when(jsonEnvelope.payloadAsJsonObject()).thenReturn(jsonObject);
        when(jsonObjectToObjectConverter.convert(jsonObject, HearingExtended.class)).thenReturn(hearingExtended);
        when(hearingRepository.findBy(hearingId)).thenReturn(hearingEntity);
        when(jsonObjectToObjectConverter.convert(jsonFromString(hearingEntity.getPayload()), Hearing.class)).thenReturn(dbHearing);
        when( objectToJsonObjectConverter.convert(any())).thenReturn(jsonObject);
        hearingExtendedEventListener.hearingExtendedForCase(jsonEnvelope);

        verify(hearingRepository, times(1)).findBy(hearingId);
        verify(jsonObjectToObjectConverter, times(1)).convert(jsonObject, HearingExtended.class);
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
        final HearingEntity hearingEntity = createHearingEntity(hearingPayloadWithSameCaseWithDifferentDefendant);
        final List<CaseDefendantHearingEntity> caseDefendantHearingEntityList = new ArrayList<>();
        caseDefendantHearingEntityList.add(createCaseDefendantHearingEntity(hearingPayloadWithSameCaseWithDifferentDefendant));

        final Hearing dbHearing = Hearing.hearing()
                .withProsecutionCases(asList(
                        ProsecutionCase.prosecutionCase()
                                .withId(UUID.fromString("d8161e2b-4aa7-4a8e-992a-11ff01c471e6"))
                                .withDefendants(asList(Defendant.defendant().withId(defendantId)
                                        .withOffences(asList(Offence.offence().withId(randomUUID()).build()))
                                        .build()))

                                .build())).build();


        when(jsonEnvelope.payloadAsJsonObject()).thenReturn(jsonObject);
        when(jsonObjectToObjectConverter.convert(jsonObject, HearingExtended.class)).thenReturn(hearingExtended);
        when(hearingRepository.findBy(hearingId)).thenReturn(hearingEntity);
        when(jsonObjectToObjectConverter.convert(jsonFromString(hearingEntity.getPayload()), Hearing.class)).thenReturn(dbHearing);
        when( objectToJsonObjectConverter.convert(any())).thenReturn(jsonObject);
        hearingExtendedEventListener.hearingExtendedForCase(jsonEnvelope);

        verify(hearingRepository, times(1)).findBy(hearingId);
        verify(jsonObjectToObjectConverter, times(1)).convert(jsonObject, HearingExtended.class);
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
        final HearingEntity hearingEntity = createHearingEntity(hearingPayloadWithSameCaseWithDifferentDefendant);
        final List<CaseDefendantHearingEntity> caseDefendantHearingEntityList = new ArrayList<>();
        caseDefendantHearingEntityList.add(createCaseDefendantHearingEntity(hearingPayloadWithSameCaseWithDifferentDefendant));

        final Hearing dbHearing = Hearing.hearing()
                .withProsecutionCases(asList(
                        ProsecutionCase.prosecutionCase()
                                .withId(caseId)
                                .withDefendants(asList(Defendant.defendant().withId(defendantId2)
                                        .withOffences(asList(Offence.offence().withId(randomUUID()).build()))
                                        .build()))

                                .build())).build();


        when(jsonEnvelope.payloadAsJsonObject()).thenReturn(jsonObject);
        when(jsonObjectToObjectConverter.convert(jsonObject, HearingExtended.class)).thenReturn(hearingExtended);
        when(hearingRepository.findBy(hearingId)).thenReturn(hearingEntity);
        when(jsonObjectToObjectConverter.convert(jsonFromString(hearingEntity.getPayload()), Hearing.class)).thenReturn(dbHearing);
        when( objectToJsonObjectConverter.convert(any())).thenReturn(jsonObject);
        hearingExtendedEventListener.hearingExtendedForCase(jsonEnvelope);

        verify(hearingRepository, times(1)).findBy(hearingId);
        verify(jsonObjectToObjectConverter, times(1)).convert(jsonObject, HearingExtended.class);
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

        final HearingExtended hearingExtended = HearingExtended.hearingExtended()
                .withHearingRequest(hearingListingNeeds)
                .withExtendedHearingFrom(extendedFromHearingId)
                .withIsAdjourned(isAdjourned)
                .withIsPartiallyAllocated(isPartiallyAllocated)
                .build();

        return hearingExtended;
    }

    private String createPayload(final String payloadPath) throws IOException {
        final StringWriter writer = new StringWriter();
        InputStream inputStream = CourtApplicationEventListenerTest.class.getResourceAsStream(payloadPath);
        IOUtils.copy(inputStream, writer, UTF_8);
        inputStream.close();
        return writer.toString();
    }

    private CaseDefendantHearingEntity createCaseDefendantHearingEntity() {
        final CaseDefendantHearingKey caseDefendantHearingKey = new CaseDefendantHearingKey();
        final CaseDefendantHearingEntity caseDefendantHearingEntity = new CaseDefendantHearingEntity();
        caseDefendantHearingEntity.setHearing(createHearingEntity());
        caseDefendantHearingEntity.setId(caseDefendantHearingKey);
        return caseDefendantHearingEntity;
    }

    private CaseDefendantHearingEntity createCaseDefendantHearingEntity(final String hearingPayload) {
        final CaseDefendantHearingKey caseDefendantHearingKey = new CaseDefendantHearingKey();
        final CaseDefendantHearingEntity caseDefendantHearingEntity = new CaseDefendantHearingEntity();
        caseDefendantHearingEntity.setHearing(createHearingEntity(hearingPayload));
        caseDefendantHearingEntity.setId(caseDefendantHearingKey);
        return caseDefendantHearingEntity;
    }

    private static JsonObject jsonFromString(final String jsonObjectStr) {
        final JsonReader jsonReader = JsonObjects.createReader(new StringReader(jsonObjectStr));
        final JsonObject object = jsonReader.readObject();
        jsonReader.close();
        return object;
    }
}
