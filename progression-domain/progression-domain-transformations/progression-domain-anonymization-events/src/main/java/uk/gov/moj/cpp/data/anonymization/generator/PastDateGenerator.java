package uk.gov.moj.cpp.data.anonymization.generator;

import java.security.SecureRandom;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Random;

public class PastDateGenerator implements Generator<String> {
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    @Override
    public String convert(final String fieldValue) {
        final LocalDate localDate = createRandomDate(new SecureRandom(), LocalDate.now().getYear()-100, LocalDate.now().getYear());
        return localDate.format(FORMATTER);

    }

    private  int createRandomIntBetween(Random random, int start, int end) {
        return random.nextInt((end - start) + 1) + start;
    }

    public  LocalDate createRandomDate(Random random, int startYear, int endYear) {
        final int day = createRandomIntBetween(random, 1, 28);
        final int month = createRandomIntBetween(random,1, 12);
        final int year = createRandomIntBetween(random,startYear, endYear);
        return LocalDate.of(year, month, day);
    }
}
