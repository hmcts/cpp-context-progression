package uk.gov.moj.cpp.data.anonymization.generator;

public class PhoneNumberGenerator implements Generator<String> {
    @Override
    public String convert() {
        return "0123456789";
    }
}
