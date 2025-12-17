package uk.gov.moj.cpp.prosecutioncase.event.listener;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.UUID.randomUUID;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.test.utils.core.reflection.ReflectionUtil.setField;

import uk.gov.justice.core.courts.DefenceCounsel;
import uk.gov.justice.core.courts.Defendant;
import uk.gov.justice.core.courts.DefendantsToRemove;
import uk.gov.justice.core.courts.Hearing;
import uk.gov.justice.core.courts.HearingListingStatus;
import uk.gov.justice.core.courts.HearingUpdatedForPartialAllocation;
import uk.gov.justice.core.courts.Offence;
import uk.gov.justice.core.courts.OffencesToRemove;
import uk.gov.justice.core.courts.ProsecutionCase;
import uk.gov.justice.core.courts.ProsecutionCasesToRemove;
import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.common.converter.ObjectToJsonObjectConverter;
import uk.gov.justice.services.common.converter.StringToJsonObjectConverter;
import uk.gov.justice.services.common.converter.jackson.ObjectMapperProducer;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.application.event.listener.CourtApplicationEventListenerTest;
import uk.gov.moj.cpp.prosecutioncase.persistence.entity.CaseDefendantHearingEntity;
import uk.gov.moj.cpp.prosecutioncase.persistence.entity.CaseDefendantHearingKey;
import uk.gov.moj.cpp.prosecutioncase.persistence.entity.HearingEntity;
import uk.gov.moj.cpp.prosecutioncase.persistence.repository.CaseDefendantHearingRepository;
import uk.gov.moj.cpp.prosecutioncase.persistence.repository.HearingRepository;
import uk.gov.moj.cpp.util.FileUtil;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.UUID;

import javax.json.JsonObject;

import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class HearingUpdatedForPartialAllocationEventListenerTest {

    @Mock
    private HearingRepository hearingRepository;
    @Mock
    private CaseDefendantHearingRepository caseDefendantHearingRepository;
    @InjectMocks
    private HearingUpdatedForPartialAllocationEventListener hearingUpdatedForPartialAllocationEventListener;
    @Mock
    private JsonObjectToObjectConverter jsonObjectToObjectConverter;
    @Mock
    private ObjectToJsonObjectConverter objectToJsonObjectConverter;
    @Mock
    private JsonEnvelope jsonEnvelope;
    @Mock
    private JsonObject jsonObject;

    private Hearing hearing;
    private Hearing hearingWithMultipleCases;
    private UUID hearingId;
    private UUID caseId;
    private UUID defendantId;
    private UUID offenceId;
    private UUID case2Id;
    private UUID defendant2Id;
    private UUID defendant3Id;
    private UUID offence2Id;
    private UUID offence3Id;
    private UUID offence4Id;
    private UUID offence5Id;
    private UUID offence6Id;
    private String hearingPayload;
    private String hearingPayloadWithMultipleCases;
    private String hearingUpdatedForPartialAllocationEventPayload;
    private String hearingUpdatedForPartialAllocationEventPayloadWithDuplicateCaseIdsInPayload;
    private final StringToJsonObjectConverter converter = new StringToJsonObjectConverter();

    @BeforeEach
    public void setup() throws IOException {
        hearingId = randomUUID();
        caseId = randomUUID();
        defendantId = randomUUID();
        offenceId = randomUUID();
        case2Id = randomUUID();
        defendant2Id = randomUUID();
        offence2Id = randomUUID();
        defendant3Id = randomUUID();
        offence3Id = randomUUID();
        offence4Id = randomUUID();
        offence5Id = randomUUID();
        offence6Id = randomUUID();

        hearing =
                createHearing();

        hearingWithMultipleCases = createHearingWithMultipleCases();

        hearingPayload = createPayload("/json/hearingDataForPartialAllocation.json")
                .replace("HEARING_ID", hearingId.toString())
                .replace("CASE_ID", caseId.toString())
                .replace("DEFENDANT_ID", defendantId.toString())
                .replace("OFFENCE_ID", offenceId.toString())
                .replace("CASE2_ID", case2Id.toString())
                .replace("DEFENDANT2_ID", defendant2Id.toString())
                .replace("OFFENCE2_ID", offence2Id.toString());

        hearingPayloadWithMultipleCases = createPayload("/json/hearingDataForPartialAllocationWithMultipleCases.json")
                .replace("HEARING_ID", hearingId.toString())
                .replace("CASE_ID", caseId.toString())
                .replace("DEFENDANT_ID", defendantId.toString())
                .replace("OFFENCE_ID", offenceId.toString())
                .replace("CASE2_ID", case2Id.toString())
                .replace("DEFENDANT2_ID", defendant2Id.toString())
                .replace("OFFENCE2_ID", offence2Id.toString())
                .replace("DEFENDANT3_ID", defendant3Id.toString())
                .replace("OFFENCE3_ID", offence3Id.toString())
                .replace("OFFENCE4_ID", offence4Id.toString())
                .replace("OFFENCE5_ID", offence5Id.toString())
                .replace("OFFENCE6_ID", offence6Id.toString());

        hearingUpdatedForPartialAllocationEventPayload = createPayload("/json/hearingUpdatedForPartialAllocationEventPayload.json")
                .replace("HEARING_ID", hearingId.toString())
                .replace("CASE_ID", caseId.toString())
                .replace("DEFENDANT_ID", defendantId.toString())
                .replace("OFFENCE_ID", offenceId.toString());

        hearingUpdatedForPartialAllocationEventPayloadWithDuplicateCaseIdsInPayload = createPayload("/json/hearingUpdatedForPartialAllocationEventPayload-duplicate-caseid.json")
                .replace("HEARING_ID", hearingId.toString())
                .replace("CASE_ID_1", caseId.toString())
                .replace("CASE_ID_2", case2Id.toString())
                .replace("DEFENDANT_ID_1", defendantId.toString())
                .replace("DEFENDANT_ID_2", defendant2Id.toString())
                .replace("DEFENDANT_ID_3", defendant3Id.toString())
                .replace("OFFENCE_ID_1", offenceId.toString())
                .replace("OFFENCE_ID_2", offence2Id.toString())
                .replace("OFFENCE_ID_3", offence3Id.toString())
                .replace("OFFENCE_ID_4", offence4Id.toString());


        setField(this.jsonObjectToObjectConverter, "objectMapper", new ObjectMapperProducer().objectMapper());
        setField(this.objectToJsonObjectConverter, "mapper", new ObjectMapperProducer().objectMapper());
    }

    @Test
    public void hearingExtendedForCase() {

        final JsonObject payload = converter.convert(hearingUpdatedForPartialAllocationEventPayload);
        final HearingUpdatedForPartialAllocation hearingUpdatedForPartialAllocation = createHearingUpdatedForPartialAllocation(payload);
        final HearingEntity hearingEntity = createHearingEntity();
        final CaseDefendantHearingEntity caseDefendantHearingEntity = createCaseDefendantHearingEntity();

        when(jsonEnvelope.payloadAsJsonObject()).thenReturn(payload);
        when(jsonObjectToObjectConverter.convert(any(JsonObject.class), any())).thenReturn(hearingUpdatedForPartialAllocation).thenReturn(hearing);
        when(objectToJsonObjectConverter.convert(hearing)).thenReturn(this.jsonObject);
        when(caseDefendantHearingRepository.findByHearingIdAndCaseIdAndDefendantId(any(UUID.class), any(UUID.class), any(UUID.class))).thenReturn(caseDefendantHearingEntity);
        when(hearingRepository.findBy(hearingId)).thenReturn(hearingEntity);
        hearingUpdatedForPartialAllocationEventListener.hearingUpdatedForPartialAllocation(jsonEnvelope);

        verify(hearingRepository, times(1)).findBy(hearingId);
        verify(hearingRepository, times(1)).save(hearingEntity);
        verify(objectToJsonObjectConverter, times(1)).convert(hearing);
        verify(caseDefendantHearingRepository, times(1)).findByHearingIdAndCaseIdAndDefendantId(hearingId, caseId, defendantId);
        verify(caseDefendantHearingRepository, times(1)).remove(caseDefendantHearingEntity);
    }

    @Test
    public void notHearingExtendedForCaseIfThereIsNoHearing() {

        final JsonObject payload = converter.convert(hearingUpdatedForPartialAllocationEventPayload);
        final HearingUpdatedForPartialAllocation hearingUpdatedForPartialAllocation = createHearingUpdatedForPartialAllocation(payload);
        final HearingEntity hearingEntity = createHearingEntity();
        final CaseDefendantHearingEntity caseDefendantHearingEntity = createCaseDefendantHearingEntity();

        when(jsonEnvelope.payloadAsJsonObject()).thenReturn(payload);
        when(jsonObjectToObjectConverter.convert(any(JsonObject.class), any())).thenReturn(hearingUpdatedForPartialAllocation).thenReturn(hearing);
        when(hearingRepository.findBy(hearingId)).thenReturn(null);
        hearingUpdatedForPartialAllocationEventListener.hearingUpdatedForPartialAllocation(jsonEnvelope);

        verify(hearingRepository, times(1)).findBy(hearingId);
        verify(hearingRepository, never()).save(hearingEntity);
    }


    @Test
    public void hearingWithDefenceCounselsExtendedForCase() throws IOException {
        hearing = createHearingWithDefenceCounsels();
        final JsonObject payload = converter.convert(hearingUpdatedForPartialAllocationEventPayload);
        final HearingUpdatedForPartialAllocation hearingUpdatedForPartialAllocation = createHearingUpdatedForPartialAllocation(payload);
        final HearingEntity hearingEntity = createHearingEntityWithDefenceCounsels();

        when(jsonEnvelope.payloadAsJsonObject()).thenReturn(payload);
        when(jsonObjectToObjectConverter.convert(any(JsonObject.class), any())).thenReturn(hearingUpdatedForPartialAllocation).thenReturn(hearing);
        when(objectToJsonObjectConverter.convert(hearing)).thenReturn(this.jsonObject);
        when(hearingRepository.findBy(hearingId)).thenReturn(hearingEntity);
        hearingUpdatedForPartialAllocationEventListener.hearingUpdatedForPartialAllocation(jsonEnvelope);

        verify(hearingRepository, times(1)).findBy(hearingId);
        verify(hearingRepository, times(1)).save(hearingEntity);
        verify(objectToJsonObjectConverter, times(1)).convert(hearing);
        verify(caseDefendantHearingRepository, times(1)).findByHearingIdAndCaseIdAndDefendantId(hearingId, caseId, defendantId);
    }

    @Test
    public void testHearingExtendForCaseWithDuplicateCaseIdsInPayload() {

        final JsonObject payload = converter.convert(hearingUpdatedForPartialAllocationEventPayloadWithDuplicateCaseIdsInPayload);
        final HearingUpdatedForPartialAllocation hearingUpdatedForPartialAllocation = createHearingUpdatedForPartialAllocationWithMultipleCasesAndDuplicateCaseIdsInPayload(payload);
        final HearingEntity hearingEntity = createHearingEntityWithMultipleCases();
        final CaseDefendantHearingEntity caseDefendantHearingEntity = createCaseDefendantHearingEntity();

        when(jsonEnvelope.payloadAsJsonObject()).thenReturn(payload);
        when(jsonObjectToObjectConverter.convert(any(JsonObject.class), any())).thenReturn(hearingUpdatedForPartialAllocation).thenReturn(hearingWithMultipleCases);
        when(objectToJsonObjectConverter.convert(hearingWithMultipleCases)).thenReturn(this.jsonObject);
        when(caseDefendantHearingRepository.findByHearingIdAndCaseIdAndDefendantId(any(UUID.class), any(UUID.class), any(UUID.class))).thenReturn(caseDefendantHearingEntity);
        when(hearingRepository.findBy(hearingId)).thenReturn(hearingEntity);
        hearingUpdatedForPartialAllocationEventListener.hearingUpdatedForPartialAllocation(jsonEnvelope);

        verify(hearingRepository, times(1)).findBy(hearingId);
        verify(hearingRepository, times(1)).save(hearingEntity);
        verify(objectToJsonObjectConverter, times(1)).convert(hearingWithMultipleCases);
        verify(caseDefendantHearingRepository, times(1)).findByHearingIdAndCaseIdAndDefendantId(hearingId, caseId, defendantId);
        verify(caseDefendantHearingRepository, times(1)).remove(caseDefendantHearingEntity);

    }

    private HearingEntity createHearingEntity() {
        final HearingEntity hearingEntity = new HearingEntity();
        hearingEntity.setListingStatus(HearingListingStatus.HEARING_INITIALISED);
        hearingEntity.setHearingId(hearingId);
        hearingEntity.setResultLines(new HashSet<>());
        hearingEntity.setPayload(hearingPayload);
        return hearingEntity;

    }

    private HearingEntity createHearingEntityWithDefenceCounsels() throws IOException {
        hearingPayload =  FileUtil.getPayload("json/hearing-payload-coming-from-db-with-defence-counsels.json")
                .replaceAll("HEARING_ID", hearingId.toString())
                .replaceAll("CASE_ID", caseId.toString())
                .replaceAll("DEFENDANT_ID1", defendantId.toString())
                .replaceAll("DEFENDANT_ID2", offenceId.toString())
                .replaceAll("OFFENCE_ID1", defendant2Id.toString())
                .replaceAll("OFFENCE_ID2", offence2Id.toString());

        final HearingEntity hearingEntity = new HearingEntity();
        hearingEntity.setListingStatus(HearingListingStatus.HEARING_INITIALISED);
        hearingEntity.setHearingId(hearingId);
        hearingEntity.setResultLines(new HashSet<>());
        hearingEntity.setPayload(hearingPayload);
        return hearingEntity;
    }

    private HearingEntity createHearingEntityWithMultipleCases() {
        final HearingEntity hearingEntity = new HearingEntity();
        hearingEntity.setListingStatus(HearingListingStatus.HEARING_INITIALISED);
        hearingEntity.setHearingId(hearingId);
        hearingEntity.setResultLines(new HashSet<>());
        hearingEntity.setPayload(hearingPayloadWithMultipleCases);
        return hearingEntity;
    }

    private Hearing createHearing() {
        final Hearing hearing = Hearing.hearing()
                .withId(hearingId)
                .withProsecutionCases(new ArrayList<>(Arrays.asList(ProsecutionCase.prosecutionCase()
                                .withId(caseId)
                                .withDefendants(new ArrayList<>(Arrays.asList(Defendant.defendant()
                                        .withId(defendantId)
                                        .withOffences(new ArrayList<>(Arrays.asList(Offence.offence()
                                                .withId(offenceId)
                                                .build())))
                                        .build())))
                                .build(),
                        ProsecutionCase.prosecutionCase()
                                .withId(case2Id)
                                .withDefendants(new ArrayList<>(Arrays.asList(Defendant.defendant()
                                        .withId(defendant2Id)
                                        .withOffences(new ArrayList<>(Arrays.asList(Offence.offence()
                                                .withId(offenceId)
                                                .build())))
                                        .build())))
                                .build()))).build();

        return hearing;
    }

    private Hearing createHearingWithDefenceCounsels() {
        final Hearing hearing = Hearing.hearing()
                .withId(hearingId)
                .withProsecutionCases(new ArrayList<>(Arrays.asList(ProsecutionCase.prosecutionCase()
                                .withId(caseId)
                                .withDefendants(new ArrayList<>(Arrays.asList(Defendant.defendant()
                                        .withId(defendantId)
                                        .withOffences(new ArrayList<>(Arrays.asList(Offence.offence()
                                                .withId(offenceId)
                                                .build())))
                                        .build())))
                                .build())))
                .withDefenceCounsels(new ArrayList<>(Arrays.asList(DefenceCounsel.defenceCounsel()
                        .withId(UUID.randomUUID())
                        .withDefendants(new ArrayList<>(Arrays.asList(defendantId)))
                        .build(), DefenceCounsel.defenceCounsel()
                        .withId(UUID.randomUUID())
                        .withDefendants(new ArrayList<>(Arrays.asList(defendant2Id)))
                        .build()))).build();
        return hearing;
    }

    private Hearing createHearingWithMultipleCases() {
        final Hearing hearing = Hearing.hearing()
                .withId(hearingId)
                .withProsecutionCases(new ArrayList<>((Arrays.asList(ProsecutionCase.prosecutionCase()
                                .withId(caseId)
                                .withDefendants(new ArrayList<>(Arrays.asList(Defendant.defendant()
                                        .withId(defendantId)
                                        .withOffences(new ArrayList<>(Arrays.asList(Offence.offence()
                                                .withId(offenceId)
                                                .build())))
                                        .build())))
                                .build(),
                        ProsecutionCase.prosecutionCase()
                                .withId(case2Id)
                                .withDefendants(new ArrayList<>(Arrays.asList(Defendant.defendant()
                                        .withId(defendant2Id)
                                        .withOffences(new ArrayList<>((Arrays.asList(Offence.offence()
                                                .withId(offence2Id)
                                                .build(),Offence.offence()
                                                .withId(offence3Id)
                                                .build(),Offence.offence()
                                                .withId(offence5Id)
                                                .build()))))
                                        .build(),Defendant.defendant()
                                        .withId(defendant3Id)
                                        .withOffences(new ArrayList<>(Arrays.asList(Offence.offence()
                                                .withId(offence4Id).build(),Offence.offence()
                                                .withId(offence6Id).build())
                                        ))
                                        .build())))
                                .build()))))
                .build();


        return hearing;
    }

    private String createPayload(final String payloadPath) throws IOException {
        final StringWriter writer = new StringWriter();
        final InputStream inputStream = CourtApplicationEventListenerTest.class.getResourceAsStream(payloadPath);
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

    private HearingUpdatedForPartialAllocation createHearingUpdatedForPartialAllocation(final JsonObject payload) {

        return HearingUpdatedForPartialAllocation.hearingUpdatedForPartialAllocation()
                .withHearingId(UUID.fromString(payload.getString("hearingId")))
                .withProsecutionCasesToRemove(Arrays.asList(ProsecutionCasesToRemove.prosecutionCasesToRemove()
                        .withCaseId(UUID.fromString(payload.getJsonArray("prosecutionCasesToRemove").getJsonObject(0).getString("caseId")))
                        .withDefendantsToRemove(Arrays.asList(DefendantsToRemove.defendantsToRemove()
                                .withDefendantId(UUID.fromString(payload.getJsonArray("prosecutionCasesToRemove").getJsonObject(0).getJsonArray("defendantsToRemove").getJsonObject(0).getString("defendantId")))
                                .withOffencesToRemove(Arrays.asList(OffencesToRemove.offencesToRemove().withOffenceId(UUID.fromString(payload.getJsonArray("prosecutionCasesToRemove").getJsonObject(0).getJsonArray("defendantsToRemove").getJsonObject(0).getJsonArray("offencesToRemove").getJsonObject(0).getString("offenceId"))).build()))
                                .build()))
                        .build()))
                .build();

    }

    public HearingEntity buildHearingEntityProperties() throws IOException {
        final HearingEntity hearingEntity = new HearingEntity();
        hearingEntity.setHearingId(hearingId);
        hearingEntity.setListingStatus(HearingListingStatus.HEARING_INITIALISED);
        hearingEntity.setPayload(FileUtil.getPayload("json/hearing-payload-coming-from-db-with-defence-counsels.json")
                .replaceAll("HEARING_ID", hearingId.toString())
                .replaceAll("CASE_ID", caseId.toString())
                .replaceAll("DEFENDANT_ID1", defendantId.toString())
                .replaceAll("DEFENDANT_ID2", offenceId.toString())
                .replaceAll("OFFENCE_ID1", defendant2Id.toString())
                .replaceAll("OFFENCE_ID2", offence2Id.toString()));

        return hearingEntity;
    }

    private HearingUpdatedForPartialAllocation createHearingUpdatedForPartialAllocationWithMultipleCasesAndDuplicateCaseIdsInPayload(final JsonObject payload) {
        final HearingUpdatedForPartialAllocation.Builder hearingUpdatedForPartialAllocationBuilder = HearingUpdatedForPartialAllocation.hearingUpdatedForPartialAllocation()
                .withHearingId(UUID.fromString(payload.getString("hearingId")));
        List<ProsecutionCasesToRemove> prosecutionCasesToRemoveList = new ArrayList<>();
        payload.getJsonArray("prosecutionCasesToRemove").forEach(prosecutionCasesToRemoveJson -> {
            final ProsecutionCasesToRemove.Builder prosecutionCasesToRemoveBuilder = ProsecutionCasesToRemove.prosecutionCasesToRemove();
            final JsonObject prosecutionCaseToRemoveJsonObject = (JsonObject) prosecutionCasesToRemoveJson;
            prosecutionCasesToRemoveBuilder.withCaseId(UUID.fromString(prosecutionCaseToRemoveJsonObject.getString("caseId")));
            List<DefendantsToRemove> defendantsToRemoveList = new ArrayList<>();
            prosecutionCaseToRemoveJsonObject.getJsonArray("defendantsToRemove").forEach(defendantsToRemoveJson -> {
                final DefendantsToRemove.Builder defendantToRemoveBuilder = DefendantsToRemove.defendantsToRemove();
                final JsonObject DefendantsToRemoveJsonObject = (JsonObject) defendantsToRemoveJson;
                defendantToRemoveBuilder.withDefendantId(UUID.fromString(DefendantsToRemoveJsonObject.getString("defendantId")));
                List<OffencesToRemove> offencesToRemoveList = new ArrayList<>();
                DefendantsToRemoveJsonObject.getJsonArray("offencesToRemove").forEach(offencesToRemoveJson -> {
                    final OffencesToRemove.Builder OffencesToRemoveBuilder = OffencesToRemove.offencesToRemove();
                    final JsonObject OffencesToRemoveJsonObject = (JsonObject) offencesToRemoveJson;
                    offencesToRemoveList.add(OffencesToRemoveBuilder
                            .withOffenceId(UUID.fromString(OffencesToRemoveJsonObject.getString("offenceId")))
                            .build());
                });
                defendantToRemoveBuilder.withOffencesToRemove(offencesToRemoveList);
                defendantsToRemoveList.add(defendantToRemoveBuilder.build());
            });
            prosecutionCasesToRemoveBuilder.withDefendantsToRemove(defendantsToRemoveList);

            prosecutionCasesToRemoveList.add(prosecutionCasesToRemoveBuilder.build());


        });

        return hearingUpdatedForPartialAllocationBuilder
                .withProsecutionCasesToRemove(prosecutionCasesToRemoveList)
                .build();
    }
}
