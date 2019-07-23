package uk.gov.moj.cpp.data.anonymization.generator;


public class NIGenerator implements Generator<String> {
@Override public String convert(String fieldValue) { return "QQ123456C"; }
}
