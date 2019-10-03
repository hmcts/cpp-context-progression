package uk.gov.moj.cpp.data.anonymization.generator;


public enum AnonymizerType {

 STRING_RULE_PREFIX("StringAnonymised"),
    STRING_ANONYMISED_PARSED_DATA ("StringAnonymisedParsedData"),
 DUMMY_NUMBER_PREFIX("DummyNumber");

    private final String value;

    AnonymizerType(String value) {
        this.value = value;
    }

    @Override
    public String toString() {
        return value;
    }
}