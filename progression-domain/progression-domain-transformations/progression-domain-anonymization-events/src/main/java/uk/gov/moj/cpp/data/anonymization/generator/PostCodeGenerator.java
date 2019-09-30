package uk.gov.moj.cpp.data.anonymization.generator;

public class PostCodeGenerator implements Generator<String> {
    @Override
    public String convert() {
        return "AA1 1AA";
    }
}
