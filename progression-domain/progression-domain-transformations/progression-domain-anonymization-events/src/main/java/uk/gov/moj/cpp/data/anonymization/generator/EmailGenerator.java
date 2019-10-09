package uk.gov.moj.cpp.data.anonymization.generator;


public class EmailGenerator implements Generator<String> {


    @Override
    public String convert() {
        return "xyz@mail.com";
    }
}
