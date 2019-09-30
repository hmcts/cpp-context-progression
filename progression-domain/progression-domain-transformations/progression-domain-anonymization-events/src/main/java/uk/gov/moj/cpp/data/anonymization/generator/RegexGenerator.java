package uk.gov.moj.cpp.data.anonymization.generator;

import com.mifmif.common.regex.Generex;

import java.util.regex.Pattern;

public class RegexGenerator implements Generator<String> {

    private final Generex generex;

    public RegexGenerator(final Pattern pattern) {
        generex = new Generex(pattern.toString());
    }

    @Override
    public String convert() {
        return generex.random();
    }
}