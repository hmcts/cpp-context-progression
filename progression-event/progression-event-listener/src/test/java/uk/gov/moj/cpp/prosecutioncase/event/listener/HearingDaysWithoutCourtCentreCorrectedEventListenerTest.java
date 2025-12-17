package uk.gov.moj.cpp.prosecutioncase.event.listener;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.test.utils.core.reflection.ReflectionUtil.setField;

import uk.gov.justice.core.courts.Hearing;
import uk.gov.justice.core.courts.HearingDay;
import uk.gov.justice.progression.events.HearingDaysWithoutCourtCentreCorrected;
import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.common.converter.ObjectToJsonObjectConverter;
import uk.gov.justice.services.common.converter.ZonedDateTimes;
import uk.gov.justice.services.common.converter.jackson.ObjectMapperProducer;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.prosecutioncase.persistence.entity.HearingEntity;
import uk.gov.moj.cpp.prosecutioncase.persistence.repository.HearingRepository;
import uk.gov.moj.cpp.util.FileUtil;

import java.io.StringReader;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.UUID;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonReader;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class HearingDaysWithoutCourtCentreCorrectedEventListenerTest {

    @Mock
    private HearingRepository hearingRepository;

    @Mock
    private JsonEnvelope jsonEnvelope;

    @InjectMocks
    private HearingDaysWithoutCourtCentreCorrectedEventListener hearingDaysWithoutCourtCentreCorrectedEventListener;

    @Spy
    private JsonObjectToObjectConverter jsonObjectToObjectConverter;

    @Spy
    private ObjectToJsonObjectConverter objectToJsonObjectConverter;

    @Captor
    private ArgumentCaptor<HearingEntity> hearingEntityArgumentCaptor;

    private static final DateTimeFormatter ZONE_DATETIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");

    @BeforeEach
    public void setUp() {
        setField(this.objectToJsonObjectConverter, "mapper", new ObjectMapperProducer().objectMapper());
        setField(this.jsonObjectToObjectConverter, "objectMapper", new ObjectMapperProducer().objectMapper());
    }

    @Test
    public void shouldCorrectHearingDaysWithoutCourtCentre() {
        final String hearingEntitySittingDay = ZONE_DATETIME_FORMATTER.format(ZonedDateTimes.fromString("2020-10-06T10:30:00.000Z"));
        final Integer hearingEntityListedDurationMinutes = 20;
        final Integer hearingEntityListingSequence = 0;

        final HearingDaysWithoutCourtCentreCorrected domainObject = createHearingDaysWithoutCourtCentreCorrected();

        doReturn(getPayload(domainObject)).when(jsonEnvelope).payloadAsJsonObject();

        final HearingEntity hearingEntity = new HearingEntity();
        hearingEntity.setHearingId(domainObject.getId());
        hearingEntity.setPayload(FileUtil.getPayload("json/hearing-payload-coming-from-db-without-court-centre.json")
        .replaceAll("SITTING_DAY", hearingEntitySittingDay)
        .replaceAll("LISTED_DURATION_MINUTES", String.valueOf(hearingEntityListedDurationMinutes))
        .replaceAll("LISTING_SEQUENCE", String.valueOf(hearingEntityListingSequence)));

        when(hearingRepository.findBy(domainObject.getId())).thenReturn(hearingEntity);

        hearingDaysWithoutCourtCentreCorrectedEventListener.correctHearingDaysWithoutCourtCentre(jsonEnvelope);

        Mockito.verify(hearingRepository).save(hearingEntityArgumentCaptor.capture());

        final JsonObject dbHearingJsonObject = jsonFromString(hearingEntityArgumentCaptor.getValue().getPayload());
        final Hearing dbHearingAfterSaved = jsonObjectToObjectConverter.convert(dbHearingJsonObject, Hearing.class);

        assertThat(dbHearingAfterSaved.getHearingDays().size(), is(domainObject.getHearingDays().size()));
        assertThat(dbHearingAfterSaved.getHearingDays().get(0).getCourtCentreId(), is(domainObject.getHearingDays().get(0).getCourtCentreId()));
        assertThat(dbHearingAfterSaved.getHearingDays().get(0).getCourtRoomId(), is(domainObject.getHearingDays().get(0).getCourtRoomId()));
        assertThat(dbHearingAfterSaved.getHearingDays().get(0).getListedDurationMinutes(), is(hearingEntityListedDurationMinutes));
        assertThat(dbHearingAfterSaved.getHearingDays().get(0).getListingSequence(), is(hearingEntityListingSequence));
        assertThat(ZONE_DATETIME_FORMATTER.format(dbHearingAfterSaved.getHearingDays().get(0).getSittingDay()), is(hearingEntitySittingDay));

    }

    private static JsonObject jsonFromString(final String jsonObjectStr) {
        final JsonReader jsonReader = Json.createReader(new StringReader(jsonObjectStr));
        final JsonObject object = jsonReader.readObject();
        jsonReader.close();
        return object;
    }

    private JsonObject getPayload(final HearingDaysWithoutCourtCentreCorrected hearingDaysWithoutCourtCentreCorrected) {

        return Json.createObjectBuilder()
                .add("hearingDays", Json.createArrayBuilder().add(objectToJsonObjectConverter.convert(hearingDaysWithoutCourtCentreCorrected.getHearingDays().get(0))).build())
                .add("id", hearingDaysWithoutCourtCentreCorrected.getId().toString())
                .build();
    }

    private HearingDaysWithoutCourtCentreCorrected createHearingDaysWithoutCourtCentreCorrected() {
        return HearingDaysWithoutCourtCentreCorrected.hearingDaysWithoutCourtCentreCorrected()
                .withHearingDays(Arrays.asList(HearingDay.hearingDay()
                .withCourtCentreId(UUID.randomUUID())
                .withCourtRoomId(UUID.randomUUID())
                .withListedDurationMinutes(30)
                .withListingSequence(1)
                .withSittingDay(ZonedDateTime.now())
                .build()))
                .withId(UUID.randomUUID())
                .build();
    }
}
