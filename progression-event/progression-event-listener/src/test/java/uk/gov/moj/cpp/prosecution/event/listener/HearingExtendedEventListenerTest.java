package uk.gov.moj.cpp.prosecution.event.listener;

import org.apache.commons.io.IOUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import uk.gov.justice.core.courts.Defendant;
import uk.gov.justice.core.courts.Hearing;
import uk.gov.justice.core.courts.HearingExtended;
import uk.gov.justice.core.courts.HearingListingNeeds;
import uk.gov.justice.core.courts.HearingListingStatus;
import uk.gov.justice.core.courts.ProsecutionCase;
import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.common.converter.ObjectToJsonObjectConverter;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.application.event.listener.CourtApplicationEventListenerTest;
import uk.gov.moj.cpp.prosecutioncase.event.listener.HearingExtendedEventListener;
import uk.gov.moj.cpp.prosecutioncase.persistence.entity.CaseDefendantHearingEntity;
import uk.gov.moj.cpp.prosecutioncase.persistence.entity.CaseDefendantHearingKey;
import uk.gov.moj.cpp.prosecutioncase.persistence.entity.HearingEntity;
import uk.gov.moj.cpp.prosecutioncase.persistence.repository.CaseDefendantHearingRepository;
import uk.gov.moj.cpp.prosecutioncase.persistence.repository.HearingRepository;

import javax.json.JsonObject;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.UUID;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.UUID.randomUUID;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
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


    @Before
    public void setup() throws IOException {
        hearingId = randomUUID();
        prosecutionCaseId = randomUUID();
        defendantId = randomUUID();
        hearingPayload = createPayload("/json/hearingDataProsecutionCase.json");
    }

    @Test
    public void hearingExtendedForCase()  {

        final UUID extendedFromHearingId = randomUUID();
        final HearingExtended hearingExtended = createHearingExtended(hearingId, extendedFromHearingId, prosecutionCaseId, defendantId);
        final HearingEntity hearingEntity = createHearingEntity();
        final List<CaseDefendantHearingEntity> caseDefendantHearingEntityList = new ArrayList<>();
        caseDefendantHearingEntityList.add(createCaseDefendantHearingEntity());

        when(jsonEnvelope.payloadAsJsonObject()).thenReturn(jsonObject);
        when(jsonObjectToObjectConverter.convert(any(JsonObject.class), any())).thenReturn(hearingExtended).thenReturn(hearing);
        when(objectToJsonObjectConverter.convert(hearing)).thenReturn(jsonObject);
        when(caseDefendantHearingRepository.findByCaseIdAndDefendantId(any(UUID.class), any(UUID.class))).thenReturn(caseDefendantHearingEntityList);
        when(hearingRepository.findBy(hearingId)).thenReturn(hearingEntity);
        hearingExtendedEventListener.hearingExtendedForCase(jsonEnvelope);

        verify(hearingRepository, times(1)).findBy(hearingId);
        verify(jsonObjectToObjectConverter, times(1)).convert(jsonObject, HearingExtended.class);
        verify(objectToJsonObjectConverter, times(1)).convert(hearing);
        verify(caseDefendantHearingRepository,times(1)).findByHearingIdAndCaseIdAndDefendantId(extendedFromHearingId, prosecutionCaseId, defendantId);
        verify(caseDefendantHearingRepository,times(1)).remove(any(CaseDefendantHearingEntity.class));
        verify(caseDefendantHearingRepository,times(1)).save(any(CaseDefendantHearingEntity.class));
    }

    private HearingEntity createHearingEntity() {
        final HearingEntity hearingEntity = new HearingEntity();
        hearingEntity.setListingStatus(HearingListingStatus.HEARING_INITIALISED);
        hearingEntity.setHearingId(UUID.randomUUID());
        hearingEntity.setResultLines(new HashSet<>());
        hearingEntity.setPayload(hearingPayload);
        return hearingEntity;

    }

    private HearingExtended createHearingExtended(final UUID hearingId, final UUID extendedFromHearingId, final UUID prosecutionCaseId, final UUID defendantId) {
        final Defendant defendant = Defendant.defendant()
                .withId(defendantId)
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

        final  HearingExtended hearingExtended = HearingExtended.hearingExtended()
                .withHearingRequest(hearingListingNeeds)
                .withExtendedHearingFrom(extendedFromHearingId)
                .withIsAdjourned(false)
                .withIsPartiallyAllocated(false)
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
}
