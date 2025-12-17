package uk.gov.moj.cpp.progression.ingester.verificationHelpers;

import static java.lang.String.format;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

import java.util.logging.Logger;

public class BaseVerificationCountHelper {
    private static final Logger logger = Logger.getLogger(BaseVerificationCountHelper.class.getName());
    private int caseDocumentsCount;
    private int partiesCount;
    private int offencesCount;
    private int aliasesCount;
    private int personDefendantCount;
    private int legalEntityDefendantCount;
    private int exceptionCount;
    private int hearingsCount;


    public void verifyCounts(final int caseDocumentsCountExpected,
                             final int partiesCountExpected,
                             final int personDefendantCountExpected,
                             final int legalEntityDefendantCountExpected,
                             final int aliasesCountExpected,
                             final int offencesCountExpected,
                             final int exceptionCountExpected) {
        logger.info(format("Case Documents count: %s", caseDocumentsCount));
        logger.info(format("Parties count: %s" , partiesCount));
        logger.info(format("Person Defendant count: %s" , personDefendantCount));
        logger.info(format("Legal Entity Defendant count: %s" , legalEntityDefendantCount));
        logger.info(format("Aliases count: %s" , aliasesCount));
        logger.info(format("Offences count: %s" , offencesCount));
        logger.info(format("Exception count: %s" , exceptionCount));

        assertThat("Incorrect Case count", caseDocumentsCount, is(caseDocumentsCountExpected));
        assertThat("Incorrect Parties count", partiesCount, is(partiesCountExpected));
        assertThat("Incorrect personDefendant count", personDefendantCount, is(personDefendantCountExpected));
        assertThat("Incorrect legalEntityDefendant count", legalEntityDefendantCount, is(legalEntityDefendantCountExpected));
        assertThat("Incorrect Aliases count", aliasesCount, is(aliasesCountExpected));
        assertThat("Incorrect Offences count", offencesCount, is(offencesCountExpected));
        assertThat("Incorrect exceptions count", exceptionCount, is(exceptionCountExpected));
    }

    public void verifyCounts(final int caseDocumentsCountExpected,
                             final int hearingsCountExpected,
                             final int exceptionCountExpected) {
        logger.info(format("Case Documents count: %s", caseDocumentsCount));
        logger.info(format("Offences count: %s", hearingsCount));
        logger.info(format("Exception count: %s", exceptionCount));

        assertThat("Incorrect Case count", caseDocumentsCount, is(caseDocumentsCountExpected));
        assertThat("Incorrect Hearings count", hearingsCount, is(hearingsCountExpected));
        assertThat("Incorrect exceptions count", exceptionCount, is(exceptionCountExpected));
    }

    public int incrementHearingsCount() {
        return hearingsCount++;
    }

    public void incrementCaseDocumentsCount() {
        caseDocumentsCount++;
    }

    public void incrementPartiesCount() {
        partiesCount++;
    }

    public void incrementOffencesCount() {
        offencesCount++;
    }


    public void incrementAliasesCount() {
        aliasesCount++;
    }

    public void incrementPersonDefendantCount() {
        personDefendantCount++;
    }

    public void incrementLegalEntityCount() {
        legalEntityDefendantCount++;
    }

    public void incrementExceptionCount() {
        exceptionCount++;
    }
}
