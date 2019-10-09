package uk.gov.moj.cpp.data.anonymization.generator;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.ThreadLocalRandom;

import static java.time.temporal.ChronoField.EPOCH_DAY;
import static java.time.temporal.ChronoUnit.DAYS;

public class RandomDateOfBirthGenerator implements Generator<String> {
    private static final int MINAGE = 20;
    private static final int NAXAGE = 100;
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");


    @Override
    public String convert() {
        final LocalDate dateUpperLimit = LocalDate.now().minusYears(MINAGE);
        final LocalDate dateLowerLimit = LocalDate.now().minusYears(NAXAGE);
        return dateLowerLimit.plusDays(ThreadLocalRandom.current().longs(0,
                DAYS.between(dateLowerLimit, dateUpperLimit)).findFirst().orElse(dateUpperLimit.getLong(EPOCH_DAY)) + 1).format(FORMATTER);
    }
}
