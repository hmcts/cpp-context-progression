package uk.gov.moj.cpp.progression.query.api.service;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static uk.gov.justice.services.test.utils.common.reflection.ReflectionUtils.setField;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Spy;
import org.mockito.runners.MockitoJUnitRunner;
import uk.gov.justice.core.courts.HearingDay;
import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.common.converter.ObjectToJsonObjectConverter;
import uk.gov.justice.services.common.converter.jackson.ObjectMapperProducer;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;

@RunWith(MockitoJUnitRunner.class)
public class CotrQueryApiServiceTest {

    @InjectMocks
    private CotrQueryApiService cotrQueryApiService;

    @Spy
    private JsonObjectToObjectConverter jsonObjectToObjectConverter;

    @Spy
    private ObjectToJsonObjectConverter objectToJsonObjectConverter;

    @Before
    public void setup() {
        setField(this.jsonObjectToObjectConverter, "objectMapper", new ObjectMapperProducer().objectMapper());
        setField(this.objectToJsonObjectConverter, "mapper", new ObjectMapperProducer().objectMapper());
    }

    @Test
    public void returnsNullWhenThereAreNoHearingDays() {
        final HearingDay earliestHearingDay = cotrQueryApiService.getEarliestHearingDay(new ArrayList<>());
        assertThat(earliestHearingDay, is(nullValue()));
    }

    @Test
    public void returnsEarliestHearingDayWhenThereIsOnlyOneHearingDay() {
        final List<HearingDay> hearingDays = new ArrayList<>();
        HearingDay day1 = HearingDay.hearingDay()
                .withSittingDay(ZonedDateTime.now())
                .withListingSequence(1)
                .build();
        hearingDays.add(day1);
        final HearingDay earliestHearingDay = cotrQueryApiService.getEarliestHearingDay(hearingDays);

        assertThat(earliestHearingDay.getListingSequence(), is(1));
    }

    @Test
    public void returnsEarliestHearingDayWhenThereAreTwoHearingDays() {
        final List<HearingDay> hearingDays = new ArrayList<>();
        HearingDay day1 = HearingDay.hearingDay()
                .withSittingDay(ZonedDateTime.now())
                .withListingSequence(1)
                .build();
        hearingDays.add(day1);
        HearingDay day2 = HearingDay.hearingDay()
                .withSittingDay(ZonedDateTime.now().plusDays(1))
                .withListingSequence(2)
                .build();
        hearingDays.add(day2);
        final HearingDay earliestHearingDay = cotrQueryApiService.getEarliestHearingDay(hearingDays);

        assertThat(earliestHearingDay.getListingSequence(), is(1));
    }


}
