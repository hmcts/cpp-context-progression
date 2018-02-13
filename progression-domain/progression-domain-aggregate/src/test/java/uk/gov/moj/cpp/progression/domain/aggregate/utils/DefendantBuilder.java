package uk.gov.moj.cpp.progression.domain.aggregate.utils;


import org.apache.commons.lang3.RandomStringUtils;
import uk.gov.moj.cpp.progression.command.defendant.AddDefendant;
import uk.gov.moj.cpp.progression.command.defendant.AdditionalInformationCommand;
import uk.gov.moj.cpp.progression.command.defendant.AncillaryOrdersCommand;
import uk.gov.moj.cpp.progression.command.defendant.DefenceCommand;
import uk.gov.moj.cpp.progression.command.defendant.DefendantCommand;
import uk.gov.moj.cpp.progression.command.defendant.MedicalDocumentationCommand;
import uk.gov.moj.cpp.progression.command.defendant.PreSentenceReportCommand;
import uk.gov.moj.cpp.progression.command.defendant.ProbationCommand;
import uk.gov.moj.cpp.progression.domain.event.defendant.CPR;
import uk.gov.moj.cpp.progression.domain.event.defendant.DefendantOffenderDomain;
import uk.gov.moj.cpp.progression.domain.event.defendant.Offence;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Random;
import java.util.UUID;


public class DefendantBuilder {

    private static final Long VERSION = 2l;
    private static final UUID PERSON_ID = UUID.randomUUID();
    private static final String POLICE_DEFENDANT_ID = UUID.randomUUID().toString();


    private static final UUID OFFENCE_ID = UUID.randomUUID();
    private static final String POLICE_OFFENCE_ID = "ABC12345";
    private static final String ASN_SEQUENCE_NUMBER = "ABC12345";
    private static final String CJS_CODE = "12345";
    private static final String REASON = "Reason";
    private static final String DESCRIPTION = "Description";
    private static final String WORDING = "Wording";
    private static final String CATEGORY = "Category";
    private static final LocalDate ARREST_DATE = LocalDate.now();
    private static final LocalDate START_DATE = LocalDate.now();
    private static final LocalDate END_DATE = LocalDate.now().plusMonths(12);
    private static final LocalDate CHARGE_DATE = LocalDate.now();

    private static final String OFFENCE_SEQUENCE = "1";

    private static final String YEAR = "2016";
    private static final String ORGANISATION_UNIT = "OrganisationUnit";
    private static final String NUMBER = "2";
    private static final String CHECK_DIGIT = "12345";
    public static final String CASEURN = "CASEURN";
    private static UUID CASE_ID= UUID.randomUUID();;
    private static UUID DEFENDANT_ID= UUID.randomUUID();;

    public static AddDefendant defaultAddDefendant() {
        DefendantOffenderDomain defendantOffender = new DefendantOffenderDomain(YEAR, ORGANISATION_UNIT, NUMBER, CHECK_DIGIT);
        CPR cpr = new CPR(defendantOffender, CJS_CODE, OFFENCE_SEQUENCE);
        Offence offence = new Offence(OFFENCE_ID, POLICE_OFFENCE_ID, cpr, ASN_SEQUENCE_NUMBER, CJS_CODE, REASON, DESCRIPTION, WORDING, CATEGORY, ARREST_DATE, START_DATE, END_DATE, CHARGE_DATE);

        return new AddDefendant(CASE_ID, DEFENDANT_ID, VERSION, PERSON_ID, POLICE_DEFENDANT_ID, new ArrayList<Offence>() {{ add(offence); }}, CASEURN);
    }

    public static DefendantCommand defaultDefendant() {
        UUID defendantId = randomUUID();
        return defaultDefendantWith(defendantId);
    }

    public static DefendantCommand defaultDefendantWith(UUID defendantId) {
        MedicalDocumentationCommand medicalDocumentation = new MedicalDocumentationCommand();
        medicalDocumentation.setDetails(randomString());

        DefenceCommand defence = new DefenceCommand();
        defence.setMedicalDocumentation(medicalDocumentation);

        AncillaryOrdersCommand ancillaryOrders = new AncillaryOrdersCommand();
        ancillaryOrders.setDetails(randomString());

        AdditionalInformationCommand additionalInformation = new AdditionalInformationCommand();
        additionalInformation.setDefence(defence);

        PreSentenceReportCommand preSentenceReport = new PreSentenceReportCommand();
        preSentenceReport.setDrugAssessment(randomBoolean());
        preSentenceReport.setProvideGuidance(randomString());

        ProbationCommand probation = new ProbationCommand();
        probation.setDangerousnessAssessment(randomBoolean());
        probation.setPreSentenceReport(preSentenceReport);

        additionalInformation.setProbation(probation);

        DefendantCommand defendant = new DefendantCommand();
        defendant.setDefendantId(defendantId);
        defendant.setAdditionalInformation(additionalInformation);
        return defendant;
    }

    public static DefendantCommand defaultDefendantWithoutAdditionalInfo(UUID defendantId) {
        MedicalDocumentationCommand medicalDocumentation = new MedicalDocumentationCommand();
        medicalDocumentation.setDetails(randomString());

        DefenceCommand defence = new DefenceCommand();
        defence.setMedicalDocumentation(medicalDocumentation);

        AncillaryOrdersCommand ancillaryOrders = new AncillaryOrdersCommand();
        ancillaryOrders.setDetails(randomString());

        PreSentenceReportCommand preSentenceReport = new PreSentenceReportCommand();
        preSentenceReport.setDrugAssessment(randomBoolean());
        preSentenceReport.setProvideGuidance(randomString());

        ProbationCommand probation = new ProbationCommand();
        probation.setDangerousnessAssessment(randomBoolean());
        probation.setPreSentenceReport(preSentenceReport);

        DefendantCommand defendant = new DefendantCommand();
        defendant.setDefendantId(defendantId);

        return defendant;
    }

    private static UUID randomUUID() {
        return UUID.randomUUID();
    }

    private static String randomString() {
        return RandomStringUtils.randomAlphanumeric(5);
    }

    private static Boolean randomBoolean() {
        return new Random().nextBoolean();
    }
}
