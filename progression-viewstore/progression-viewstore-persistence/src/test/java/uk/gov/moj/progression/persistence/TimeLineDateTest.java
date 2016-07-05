package uk.gov.moj.progression.persistence;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;

import java.time.LocalDate;

import org.junit.Test;

import uk.gov.moj.cpp.progression.domain.constant.TimeLineDateType;
import uk.gov.moj.cpp.progression.persistence.entity.TimeLineDate;

public class TimeLineDateTest {

    @Test
    public void deadlineHasNotPassed() throws Exception {

        LocalDate startDate = LocalDate.now().minusDays(5);
        LocalDate referenceDate = LocalDate.now();
        int daysFromStartDate = 10;
        TimeLineDate tld = new TimeLineDate(TimeLineDateType.cmiSubmissionDeadline, startDate, referenceDate, daysFromStartDate);

        assertThat(tld.getDaysToDeadline(), equalTo(5l));
        assertThat(tld.getDeadLineDate(), equalTo(LocalDate.now().plusDays(5)));
    }

    @Test
    public void deadlineHasPassed() throws Exception {

        LocalDate startDate = LocalDate.now().minusDays(5);
        LocalDate referenceDate = LocalDate.now();
        int daysFromStartDate = 3;
        TimeLineDate tld = new TimeLineDate(TimeLineDateType.cmiSubmissionDeadline, startDate, referenceDate, daysFromStartDate);

        assertThat(tld.getDaysToDeadline(), equalTo(-2l));
        assertThat(tld.getDeadLineDate(), equalTo(LocalDate.now().minusDays(2)));
    }

    @Test
    public void deadlineIsToday() throws Exception {

        LocalDate startDate = LocalDate.now().minusDays(5);
        LocalDate referenceDate = LocalDate.now();
        int daysFromStartDate = 5;
        TimeLineDate tld = new TimeLineDate(TimeLineDateType.cmiSubmissionDeadline, startDate, referenceDate, daysFromStartDate);

        assertThat(tld.getDaysToDeadline(), equalTo(0l));
        assertThat(tld.getDeadLineDate(), equalTo(LocalDate.now()));
    }

}
