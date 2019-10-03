package uk.gov.moj.cpp.data.anonymization.generator;

import java.security.SecureRandom;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Random;

public class PastDateGenerator implements Generator<String> {
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    @Override
    public String convert() {
        final LocalDate localDate = createRandomDate(new SecureRandom(), LocalDate.now().getYear());
        return localDate.format(FORMATTER);

    }

    private  int createRandomIntBetween(Random random, int start, int end) {
        return random.nextInt(end - start) + start;
    }

    public  LocalDate createRandomDate(Random random, int startYear) {
        final int day = createRandomIntBetween(random, 1, 28);
        final int month = createRandomIntBetween(random,1, 12);
        final int year = startYear -20 ;
        return LocalDate.of(year, month, day);
    }
}
