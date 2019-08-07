package uk.gov.moj.cpp.data.anonymization.generator;


import java.math.BigInteger;
import static uk.gov.moj.cpp.data.anonymization.generator.AnonymizerType.DUMMY_NUMBER_PREFIX;

public class DummyNumberReplacer {
    private DummyNumberReplacer() {}

    public static BigInteger replace(String dummyValue) {

        return new BigInteger(dummyValue.replace(DUMMY_NUMBER_PREFIX.toString(), ""));
    }
}
