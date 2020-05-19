package uk.gov.moj.cpp.progression.query;

import static java.util.UUID.randomUUID;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.test.utils.common.reflection.ReflectionUtils.setField;

import uk.gov.justice.core.courts.Hearing;
import uk.gov.justice.core.courts.HearingListingStatus;
import uk.gov.justice.core.courts.JurisdictionType;
import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.common.converter.StringToJsonObjectConverter;
import uk.gov.justice.services.common.converter.jackson.ObjectMapperProducer;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.prosecutioncase.persistence.entity.HearingEntity;
import uk.gov.moj.cpp.prosecutioncase.persistence.repository.HearingRepository;

import java.util.UUID;

import javax.json.Json;
import javax.json.JsonObject;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;


@RunWith(MockitoJUnitRunner.class)
public class HearingQueryViewTest {

    @Mock
    private HearingRepository hearingRepository;

    @Mock
    private StringToJsonObjectConverter stringToJsonObjectConverter ;

    @Mock
    private JsonObjectToObjectConverter jsonObjectToObjectConverter;


    @InjectMocks
    private HearingQueryView hearingQueryView;

    @Before
    public void setup() {
        setField(this.jsonObjectToObjectConverter, "objectMapper", new ObjectMapperProducer().objectMapper());
    }

    @Test
    public void shouldFindHearingById() throws Exception {
        final UUID hearingId = randomUUID();

        final JsonObject jsonObject = Json.createObjectBuilder()
                .add("hearingId", hearingId.toString())
                .add("jurisdictionType",JurisdictionType.CROWN.toString())
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

}


