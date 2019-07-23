package uk.gov.moj.cpp.data.anonymization.generator;


public class SimpleStringGenerator implements Generator<String> {


    @Override
    public String convert(String fieldValue) {
        return "XXXXX";
    }
}
