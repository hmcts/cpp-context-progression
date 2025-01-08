package uk.gov.moj.cpp.progression;

public interface DMConstants {

    String DIRECTION_TYPE_ID_1 = "0a18eadf-0970-42ff-b980-b7f383391391";

    String DIRECTION_TYPE_ID_2 = "29be8453-b3c6-4f5c-815f-7f6b41ac1ac1";

    String DIRECTION_TYPE_ID_5 = "44a07637-d501-4604-9716-1bc664ce2e69";

    String DIRECTION_REF_DATA_ID = "7e2f843e-d639-40b3-8611-8015f3a13333";

    String DIRECTION_TWELVE_DOT_FOUR = "34dd0d71-1c9b-44b2-8961-c3532faa67bc";

    String DIRECTION_TWELVE_DOT_SEVEN = "a52e3146-5f3a-461f-827e-7b1be9b81a9a";

    String DIRECTION_TWELVE_DOT_EIGHT = "b69988fa-72a0-49b4-adc9-8282147e6285";

    String DIRECTION_TWELVE_DOT_NINE = "d1066637-2313-4706-989d-1112be159225";

    String CASE_ID = "b28a3caf-c90f-49e8-9bd4-129feb106c22";

    String REFERENCE_DATA_SERVICE_NAME = "referencedata-service";

    String REFERENCE_DATA_DIRECTION_URL = "/referencedata-service/query/api/rest/referencedata/directions/{0}?.*";

    String REFERENCE_DATA_ALL_DIRECTION_URL = "/referencedata-service/query/api/rest/referencedata/directions/?.*";

    String REFERENCE_DATA_DIRECTION_MANAGEMENT_TYPE_URL = "/referencedata-service/query/api/rest/referencedata/direction-management-types";

    String REFERENCE_DATA_DIRECTION_MEDIA_TYPE = "application/vnd.referencedata.get-direction+json";

    String REFERENCE_DATA_ALL_DIRECTION_MEDIA_TYPE = "application/vnd.referencedata.get-all-directions+json";

    String REFERENCE_DATA_DIRECTION_TYPE_MEDIA_TYPE = "application/vnd.referencedata.direction-management-types+json";

    String PROSECUTION_CASE_SERVICE_NAME = "progression-service";


    String PROSECUTION_CASE_MEDIA_TYPE = "application/vnd.progression.query.case+json";
    String PROSECUTION_PET_FORM_MEDIA_TYPE = "application/vnd.progression.query.pet+json";
    String PROSECUTION_PTPH_FORM_MEDIA_TYPE = "application/vnd.progression.query.form+json";

    String DIRECTION_TWELVE_DOT_ELEVEN = "eccb40a3-a13d-4228-8331-d6db9d398af3";

    int TIMEOUT_IN_SECONDS = 20;

    String PROMPT_1 = "3b81321f-c98b-473a-a3c0-ff5b55654138";

    String ID = "id";
    String PROSECUTION_CASE_URL = "/progression-service/query/api/rest/progression/prosecutioncases/.*";
    String PROSECUTION_CASE_URL_WITH_CASE_ID = "/progression-/query/api/rest/progression/prosecutioncases/{0}";
    String PROSECUTION_PET_FORM_URL = "/progression-service/query/api/rest/progression/pet/{0}?.*";
    String PROSECUTION_PTPH_FORM_URL = "/progression-service/query/api/rest/progression/prosecutioncases/{0}/form/{1}?.*";

}
