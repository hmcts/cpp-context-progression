package uk.gov.moj.cpp.progression.helper;

import uk.gov.justice.services.test.utils.core.random.StringGenerator;

public class CourtApplicationsHelper {
    
    public class CourtApplicationRandomValues {
        
        final public String APPLICANT_FIRSTNAME = new StringGenerator().next();
        final public String APPLICANT_MIDDLENAME = new StringGenerator().next();
        final public String APPLICANT_LASTNAME = new StringGenerator().next();
        final public String APPLICANT_ORGANISATION_NAME = new StringGenerator().next();
        
        final public String RESPONDENT_FIRSTNAME = new StringGenerator().next();
        final public String RESPONDENT_MIDDLENAME = new StringGenerator().next();
        final public String RESPONDENT_LASTNAME = new StringGenerator().next();
        final public String RESPONDENT_ORGANISATION_NAME = new StringGenerator().next();
    }
    
}
