package uk.gov.moj.cpp.prosecutioncase.event.listener;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.UUID.randomUUID;
import static org.mockito.Matchers.any;
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
import java.util.UUID;

import javax.json.JsonObject;

import org.apache.commons.io.IOUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
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
    private UUID hearingId;
    private UUID caseId;
    private UUID defendantId;
    private UUID offenceId;
    private UUID case2Id;
    private UUID defendant2Id;
    private UUID offence2Id;
    private String hearingPayload;
    private String hearingUpdatedForPartialAllocationEventPayload;
    private final StringToJsonObjectConverter converter = new StringToJsonObjectConverter();

    @Before
    public void setup() throws IOException {
        hearingId = randomUUID();
        caseId = randomUUID();
        defendantId = randomUUID();
        offenceId = randomUUID();
        case2Id = randomUUID();
        defendant2Id = randomUUID();
        offence2Id = randomUUID();

        hearing = createHearing();

        hearingPayload = createPayload("/json/hearingDataForPartialAllocation.json")
                .replace("HEARING_ID", hearingId.toString())
                .replace("CASE_ID", caseId.toString())
                .replace("DEFENDANT_ID", defendantId.toString())
                .replace("OFFENCE_ID", offenceId.toString())
                .replace("CASE2_ID", case2Id.toString())
                .replace("DEFENDANT2_ID", defendant2Id.toString())
                .replace("OFFENCE2_ID", offence2Id.toString());

        hearingUpdatedForPartialAllocationEventPayload = createPayload("/json/hearingUpdatedForPartialAllocationEventPayload.json")
                .replace("HEARING_ID", hearingId.toString())
                .replace("CASE_ID", caseId.toString())
                .replace("DEFENDANT_ID", defendantId.toString())
                .replace("OFFENCE_ID", offenceId.toString());


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
}
