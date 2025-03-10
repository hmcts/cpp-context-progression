package uk.gov.moj.cpp.prosecutioncase.event.listener;

import static java.util.UUID.randomUUID;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.justice.core.courts.HearingListingStatus.HEARING_RESULTED;

import uk.gov.justice.core.courts.HearingListingStatus;
import uk.gov.justice.core.courts.ProsecutionCaseDefendantListingStatusChangedV2;
import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.common.converter.ObjectToJsonObjectConverter;
import uk.gov.justice.services.common.converter.StringToJsonObjectConverter;
import uk.gov.justice.services.common.converter.jackson.ObjectMapperProducer;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.prosecutioncase.persistence.entity.HearingApplicationEntity;
import uk.gov.moj.cpp.prosecutioncase.persistence.entity.HearingEntity;
import uk.gov.moj.cpp.prosecutioncase.persistence.repository.HearingApplicationRepository;
import uk.gov.moj.cpp.prosecutioncase.persistence.repository.HearingRepository;
import uk.gov.moj.cpp.util.FileUtil;

import java.util.UUID;

import javax.json.Json;
import javax.json.JsonObject;

import com.fasterxml.jackson.databind.ObjectMapper;
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
public class ApplicationHearingToInActiveProsecutionCaseListingStatusChangedEventListenerTest {

    private UUID hearingId;
    private UUID applicationId;

    @Spy
    private final ObjectMapper objectMapper = new ObjectMapperProducer().objectMapper();

    @Spy
    private JsonObjectToObjectConverter jsonObjectToObjectConverter = new JsonObjectToObjectConverter(objectMapper);

    @Spy
    private final ObjectToJsonObjectConverter objectToJsonObjectConverter = new ObjectToJsonObjectConverter(objectMapper);

    @Mock
    private JsonEnvelope envelope;


    @Mock
    private HearingApplicationRepository hearingApplicationRepository;

    @Captor
    private ArgumentCaptor<HearingApplicationEntity> argumentCaptorHearingApplicationEntity;

    @InjectMocks
    private ProsecutionCaseDefendantListingStatusChangedListener eventListener;

    @Mock
    private HearingRepository hearingRepository;

    @Captor
    private ArgumentCaptor<HearingEntity> argumentCaptorHearingEntity;

    @Spy
    private StringToJsonObjectConverter stringToJsonObjectConverter;

    @BeforeEach
    public void setUp() {
        hearingId = randomUUID();
        applicationId = randomUUID();
    }

    @Test
    public void shouldHandleApplicationHearingForInactiveCaseListingStatusChanged_Sent_For_Listing_EventV2() {

        final String eventPayload = FileUtil.getPayload("json/progression.event.application-hearing-for-INACTIVE-Case-listing-status-changed-SENT-FOR-LISTING.json")
                .replaceAll("APPLICATION_ID", applicationId.toString()).replaceAll("HEARING_ID", hearingId.toString());
        final JsonObject eventPayloadJsonObject = stringToJsonObjectConverter.convert(eventPayload);

        final ProsecutionCaseDefendantListingStatusChangedV2 updatedProsecutionCaseDefendantListingStatusChangedV2 = jsonObjectToObjectConverter.convert(eventPayloadJsonObject, ProsecutionCaseDefendantListingStatusChangedV2.class);

        when(hearingRepository.findBy(hearingId)).thenReturn(null);
        when(envelope.payloadAsJsonObject()).thenReturn(eventPayloadJsonObject);
        when(jsonObjectToObjectConverter.convert(eventPayloadJsonObject, ProsecutionCaseDefendantListingStatusChangedV2.class)).thenReturn(updatedProsecutionCaseDefendantListingStatusChangedV2);


        eventListener.processV2(envelope);
        verify(hearingApplicationRepository).save(argumentCaptorHearingApplicationEntity.capture());
        assertThat(argumentCaptorHearingApplicationEntity.getValue().getId().getHearingId(), is(hearingId));
        assertThat(argumentCaptorHearingApplicationEntity.getValue().getId().getApplicationId(), is(applicationId));
    }

    @Test
    public void shouldHandleApplicationHearingForInactiveCaseListingStatusChanged_Hearing_Resulted_EventV2() {
        final String eventPayload = FileUtil.getPayload("json/progression.event.application-hearing-for-INACTIVE-Case-listing-status-changed-HEARING_RESULTED.json")
                .replaceAll("APPLICATION_ID", applicationId.toString()).replaceAll("HEARING_ID", hearingId.toString());
        final JsonObject eventPayloadJsonObject = stringToJsonObjectConverter.convert(eventPayload);

        final ProsecutionCaseDefendantListingStatusChangedV2 updatedProsecutionCaseDefendantListingStatusChangedV2 = jsonObjectToObjectConverter.convert(eventPayloadJsonObject, ProsecutionCaseDefendantListingStatusChangedV2.class);

        final HearingEntity hearingEntity = new HearingEntity();
        hearingEntity.setHearingId(hearingId);
        hearingEntity.setPayload(Json.createObjectBuilder().build().toString());
        hearingEntity.setListingStatus(HearingListingStatus.SENT_FOR_LISTING);

        when(hearingRepository.findBy(hearingId)).thenReturn(hearingEntity);
        when(envelope.payloadAsJsonObject()).thenReturn(eventPayloadJsonObject);
        when(jsonObjectToObjectConverter.convert(eventPayloadJsonObject, ProsecutionCaseDefendantListingStatusChangedV2.class)).thenReturn(updatedProsecutionCaseDefendantListingStatusChangedV2);


        eventListener.processV2(envelope);
        verify(hearingRepository).save(argumentCaptorHearingEntity.capture());
        assertThat(argumentCaptorHearingEntity.getValue().getListingStatus(), is(HEARING_RESULTED));
        assertThat(argumentCaptorHearingEntity.getValue().getHearingId(), is(hearingId));
    }
}