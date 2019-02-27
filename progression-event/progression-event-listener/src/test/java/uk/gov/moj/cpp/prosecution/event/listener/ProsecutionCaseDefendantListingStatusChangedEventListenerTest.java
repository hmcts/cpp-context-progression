package uk.gov.moj.cpp.prosecution.event.listener;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import uk.gov.justice.core.courts.Defendant;
import uk.gov.justice.core.courts.Hearing;
import uk.gov.justice.core.courts.HearingListingStatus;
import uk.gov.justice.core.courts.ProsecutionCase;
import uk.gov.justice.core.courts.ProsecutionCaseDefendantListingStatusChanged;
import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.common.converter.ObjectToJsonObjectConverter;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.messaging.Metadata;
import uk.gov.moj.cpp.prosecutioncase.event.listener.ProsecutionCaseDefendantListingStatusChangedListener;
import uk.gov.moj.cpp.prosecutioncase.persistence.entity.CaseDefendantHearingEntity;
import uk.gov.moj.cpp.prosecutioncase.persistence.entity.CaseDefendantHearingKey;
import uk.gov.moj.cpp.prosecutioncase.persistence.entity.HearingEntity;
import uk.gov.moj.cpp.prosecutioncase.persistence.repository.CaseDefendantHearingRepository;
import uk.gov.moj.cpp.prosecutioncase.persistence.repository.HearingRepository;

import javax.json.Json;
import javax.json.JsonObject;
import java.util.Arrays;
import java.util.UUID;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;


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
    private CaseDefendantHearingRepository repository;

    @Captor
    private ArgumentCaptor<CaseDefendantHearingEntity> argumentCaptorHearingResultLineEntity;

    @Mock
    private JsonObject payload;

    @Mock
    private Metadata metadata;

    @InjectMocks
    private ProsecutionCaseDefendantListingStatusChangedListener eventListener;

    @Mock
    private CaseDefendantHearingEntity caseDefendantHearingEntity;

    @Mock
    private HearingRepository hearingRepository;

    @Before
    public void setUp() {
        hearingId = UUID.randomUUID();
        caseId = UUID.randomUUID();
        defendantId = UUID.randomUUID();
    }

    @Test
    public void shouldHandleProsecutionCaseDefendantHearingResultEvent() throws Exception {

        final HearingEntity hearingEntity = new HearingEntity();
        hearingEntity.setHearingId(hearingId);
        hearingEntity.setPayload(Json.createObjectBuilder().build().toString());
        hearingEntity.setListingStatus(HearingListingStatus.HEARING_INITIALISED);

        final CaseDefendantHearingEntity hearingResultLineEntity = new CaseDefendantHearingEntity();
        hearingResultLineEntity.setId(new CaseDefendantHearingKey(caseId, defendantId, hearingId));
        hearingResultLineEntity.setHearing(hearingEntity);

        when(hearingRepository.findBy(hearingId)).thenReturn(hearingEntity);
        when(envelope.payloadAsJsonObject()).thenReturn(payload);
        when(objectToJsonObjectConverter.convert(any())).thenReturn(Json.createObjectBuilder().build());
        when(jsonObjectToObjectConverter.convert(payload, ProsecutionCaseDefendantListingStatusChanged.class))
                .thenReturn(ProsecutionCaseDefendantListingStatusChanged.prosecutionCaseDefendantListingStatusChanged()
                        .withHearing(Hearing.hearing().withId(hearingId)
                                .withProsecutionCases(Arrays.asList(ProsecutionCase.prosecutionCase().withId(caseId)
                                        .withDefendants(Arrays.asList(Defendant.defendant().withId(defendantId).build()))
                                        .build()))
                                .build())
                        .withHearingListingStatus(HearingListingStatus.HEARING_INITIALISED)
                        .build());

        when(envelope.metadata()).thenReturn(metadata);

        eventListener.process(envelope);
        verify(repository).save(argumentCaptorHearingResultLineEntity.capture());
    }
}