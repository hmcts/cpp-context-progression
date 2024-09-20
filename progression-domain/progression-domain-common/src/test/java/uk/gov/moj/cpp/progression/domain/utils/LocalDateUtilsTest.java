package uk.gov.moj.cpp.progression.domain.utils;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;

import org.junit.jupiter.api.Test;

public class LocalDateUtilsTest {

    @Test
    public void isYouth() {
        LocalDate dob = LocalDate.of(2011, 12, 23);
        LocalDate currentDate = LocalDate.of(2019, 12, 23);
        assertTrue(LocalDateUtils.isYouth(dob, currentDate));
    }

    @Test
    public void isYouthBoundaryCase() {
        LocalDate dob = LocalDate.of(2002, 01, 01);
        LocalDate currentDate = LocalDate.of(2019, 12, 31);
        assertTrue(LocalDateUtils.isYouth(dob, currentDate));
    }

    @Test
    public void isNotYouth() {
        LocalDate dob = LocalDate.of(1990, 12, 23);
        LocalDate currentDate = LocalDate.of(2019, 12, 23);
        assertTrue(!LocalDateUtils.isYouth(dob, currentDate));
    }
}