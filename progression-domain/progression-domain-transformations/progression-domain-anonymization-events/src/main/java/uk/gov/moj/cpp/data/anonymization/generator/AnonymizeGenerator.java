package uk.gov.moj.cpp.data.anonymization.generator;

@SuppressWarnings({"squid:S3776", "squid:S134", "squid:MethodCyclomaticComplexity"})
public class AnonymizeGenerator implements Generator<String> {

    Generator<String> generator;
    private static final String EMAIL_RULE = "StringAnonymisedEmail";
    private static final String URI_RULE = "StringAnonymisedURI";
    private static final String PAST_DATE_RULE = "StringAnonymisedPastDate";
    private static final String FUTURE_DATE_RULE = "StringAnonymisedFutureDate";
    private static final String DOB_RULE = "StringAnonymisedDOB";
    private static final String SIMPLE_STRING_RULE = "StringAnonymisedString";
    private static final String PHONE_RULE = "StringAnonymisedPhoneNumber";
    private static final String POST_CODE_RULE = "StringAnonymisedPostcode";
    private static final String NI_RULE = "StringAnonymisedNI";

    public Generator<String> getGenerator(String rule){
        switch (rule) {
            case EMAIL_RULE:
                generator = new EmailGenerator();
                break;
            case URI_RULE:
                generator = new UriGenerator();
                break;
            case PAST_DATE_RULE:
                generator = new PastDateGenerator();
                break;
            case FUTURE_DATE_RULE:
                generator = new FutureDateGenerator();
                break;
            case DOB_RULE:
                generator = new RandomDateOfBirthGenerator();
                break;
            case SIMPLE_STRING_RULE:
                generator = new SimpleStringGenerator();
                break;
            case PHONE_RULE:
                generator = new PhoneNumberGenerator();
                break;
            case POST_CODE_RULE:
                generator = new PostCodeGenerator();
                break;
            case NI_RULE:
                generator = new NIGenerator();
                break;
            default:
                break;
        }
        return generator;
    }



    @Override
    public String convert() {
        return generator.convert();
    }
}
