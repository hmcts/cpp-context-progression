package uk.gov.moj.cpp.progression.query.view.converter;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;

import java.time.LocalDate;

import org.junit.Before;
import org.junit.Test;

import uk.gov.moj.cpp.progression.domain.constant.TimeLineDateType;
import uk.gov.moj.cpp.progression.persistence.entity.TimeLineDate;
import uk.gov.moj.cpp.progression.query.view.converter.TimelineDateToTimeLineDateViewConverter;
import uk.gov.moj.cpp.progression.query.view.response.TimeLineDateView;

public class TimelineDateToTimeLineDateViewConverterTest {

    private TimelineDateToTimeLineDateViewConverter converter;

    @Before
    public void setup() {
        converter = new TimelineDateToTimeLineDateViewConverter();
    }

    @Test
    public void canConvertFromTimelineDateToTimeLineDateCO() throws Exception {
        LocalDate startDate = LocalDate.now().minusDays(5);
        LocalDate referenceDate = LocalDate.now();
        int daysFromStartDate = 10;
        TimeLineDate tld = new TimeLineDate(TimeLineDateType.cmiSubmissionDeadline, startDate, referenceDate, daysFromStartDate);

        TimeLineDateView tldvo = converter.convert(tld);

        assertThat(tldvo.getDaysFromStartDate(), equalTo(tld.getDaysFromStartDate()));
        assertThat(tldvo.getDaysToDeadline(), equalTo(tld.getDaysToDeadline()));
        assertThat(tldvo.getDeadlineDate(), equalTo(tld.getDeadLineDate()));
        assertThat(tldvo.getStartDate(), equalTo(tld.getStartDate()));
    }

}
