package uk.gov.moj.cpp.progression.query;

import static java.util.Arrays.asList;
import static java.util.UUID.randomUUID;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.test.utils.core.reflection.ReflectionUtil.setField;

import uk.gov.justice.core.courts.Hearing;
import uk.gov.justice.core.courts.HearingListingStatus;
import uk.gov.justice.core.courts.JurisdictionType;
import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.common.converter.StringToJsonObjectConverter;
import uk.gov.justice.services.common.converter.jackson.ObjectMapperProducer;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.prosecutioncase.persistence.entity.HearingEntity;
import uk.gov.moj.cpp.prosecutioncase.persistence.repository.HearingRepository;

import java.util.List;
import java.util.UUID;

import javax.json.Json;
import javax.json.JsonObject;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;


@ExtendWith(MockitoExtension.class)
public class HearingQueryViewTest {

    @Mock
    private HearingRepository hearingRepository;

    @Mock
    private StringToJsonObjectConverter stringToJsonObjectConverter;

    @Mock
    private JsonObjectToObjectConverter jsonObjectToObjectConverter;


    @InjectMocks
    private HearingQueryView hearingQueryView;

    @BeforeEach
    public void setup() {
        setField(this.jsonObjectToObjectConverter, "objectMapper", new ObjectMapperProducer().objectMapper());
    }

    @Test
    public void shouldFindHearingById() {
        final UUID hearingId = randomUUID();

        final JsonObject jsonObject = Json.createObjectBuilder()
                .add("hearingId", hearingId.toString())
                .add("jurisdictionType", JurisdictionType.CROWN.toString())
                .build();

        Hearing hearing = Hearing.hearing()
                .withId(hearingId)
                .withJurisdictionType(JurisdictionType.CROWN)
                .build();
        final JsonEnvelope jsonEnvelope = JsonEnvelope.envelopeFrom(
                JsonEnvelope.metadataBuilder().withId(randomUUID()).withName("progression.query.hearing").build(),
                jsonObject);

        HearingEntity hearingEntity = new HearingEntity();
        hearingEntity.setHearingId(hearingId);
        hearingEntity.setPayload("{}");
        hearingEntity.setListingStatus(HearingListingStatus.HEARING_INITIALISED);

        when(hearingRepository.findBy(hearingId)).thenReturn(hearingEntity);
        when(stringToJsonObjectConverter.convert(any(String.class))).thenReturn(jsonObject);

        final JsonEnvelope response = hearingQueryView.getHearing(jsonEnvelope);

        assertThat(response.payloadAsJsonObject().getJsonObject("hearing").getString("hearingId"), is(hearing.getId().toString()));
        assertThat(response.payloadAsJsonObject().getJsonObject("hearing").getString("jurisdictionType"), is(hearing.getJurisdictionType().toString()));
    }

    @Test
    public void shouldFindHearingsByIds() {
        final UUID hearingId1 = randomUUID();
        final UUID hearingId2 = randomUUID();
        final List<UUID> hearingIds = asList(hearingId1, hearingId2);

        final JsonObject jsonObject1 = Json.createObjectBuilder()
                .add("hearingId", hearingId1.toString())
                .build();

        final JsonObject jsonObject2 = Json.createObjectBuilder()
                .add("hearingId", hearingId2.toString())
                .build();

        final HearingEntity hearingEntity1 = new HearingEntity();
        hearingEntity1.setHearingId(hearingId1);
        hearingEntity1.setPayload("{1}");
        final HearingEntity hearingEntity2 = new HearingEntity();
        hearingEntity2.setHearingId(hearingId2);
        hearingEntity2.setPayload("{2}");

        final Hearing hearing1 = Hearing.hearing()
                .withId(hearingId1)
                .build();
        final Hearing hearing2 = Hearing.hearing()
                .withId(hearingId2)
                .build();

        final List<HearingEntity> hearingEntities = asList(hearingEntity1, hearingEntity2);

        when(hearingRepository.findByHearingIds(hearingIds)).thenReturn(hearingEntities);
        when(stringToJsonObjectConverter.convert(hearingEntity1.getPayload())).thenReturn(jsonObject1);
        when(stringToJsonObjectConverter.convert(hearingEntity2.getPayload())).thenReturn(jsonObject2);
        when(jsonObjectToObjectConverter.convert(jsonObject1, Hearing.class)).thenReturn(hearing1);
        when(jsonObjectToObjectConverter.convert(jsonObject2, Hearing.class)).thenReturn(hearing2);


        final List<Hearing> hearings = hearingQueryView.getHearings(hearingIds);

        assertThat(hearings.get(0).getId(), is(hearingId1));
        assertThat(hearings.get(1).getId(), is(hearingId2));
    }

    @Test
    public void shouldReturnEmpty_FindHearingById() {
        final JsonObject jsonObject = Json.createObjectBuilder()
                .add("hearingId", randomUUID().toString())
                .add("jurisdictionType", JurisdictionType.CROWN.toString())
                .build();

        final JsonEnvelope jsonEnvelope = JsonEnvelope.envelopeFrom(
                JsonEnvelope.metadataBuilder().withId(randomUUID()).withName("progression.query.hearing").build(),
                jsonObject);

        final JsonEnvelope response = hearingQueryView.getHearing(jsonEnvelope);

        assertThat(response.payloadAsJsonObject().size(), is(0));
    }

}


