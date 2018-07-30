package uk.gov.moj.cpp.progression.domain.aggregate.utils;


import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Random;
import java.util.UUID;

import org.apache.commons.lang3.RandomStringUtils;

import uk.gov.moj.cpp.progression.command.defendant.AddDefendant;
import uk.gov.moj.cpp.progression.command.defendant.AdditionalInformationCommand;
import uk.gov.moj.cpp.progression.command.defendant.AncillaryOrdersCommand;
import uk.gov.moj.cpp.progression.command.defendant.DefenceCommand;
import uk.gov.moj.cpp.progression.command.defendant.DefendantCommand;
import uk.gov.moj.cpp.progression.command.defendant.MedicalDocumentationCommand;
import uk.gov.moj.cpp.progression.command.defendant.PreSentenceReportCommand;
import uk.gov.moj.cpp.progression.command.defendant.ProbationCommand;
import uk.gov.moj.cpp.progression.command.defendant.UpdateDefendantCommand;
import uk.gov.moj.cpp.progression.domain.event.defendant.Address;
import uk.gov.moj.cpp.progression.domain.event.defendant.CPR;
import uk.gov.moj.cpp.progression.domain.event.defendant.DefendantOffenderDomain;
import uk.gov.moj.cpp.progression.domain.event.defendant.Interpreter;
import uk.gov.moj.cpp.progression.domain.event.defendant.Offence;
import uk.gov.moj.cpp.progression.domain.event.defendant.Person;



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
        final DefendantOffenderDomain defendantOffender = new DefendantOffenderDomain(YEAR, ORGANISATION_UNIT, NUMBER, CHECK_DIGIT);
        final CPR cpr = new CPR(defendantOffender, CJS_CODE, OFFENCE_SEQUENCE);
        final Offence offence = new Offence(OFFENCE_ID, POLICE_OFFENCE_ID, cpr, ASN_SEQUENCE_NUMBER, CJS_CODE, REASON, DESCRIPTION, WORDING, CATEGORY, ARREST_DATE, START_DATE, END_DATE, CHARGE_DATE);

        return new AddDefendant(CASE_ID, DEFENDANT_ID, VERSION, new Person(), POLICE_DEFENDANT_ID, new ArrayList<Offence>() {{
            add(offence);
        }}, CASEURN);
    }

    public static DefendantCommand defaultDefendant() {
        final UUID defendantId = randomUUID();
        return defaultDefendantWith(defendantId);
    }

    public static DefendantCommand defaultDefendantWith(final UUID defendantId) {
        final MedicalDocumentationCommand medicalDocumentation = new MedicalDocumentationCommand();
        medicalDocumentation.setDetails(randomString());

        final DefenceCommand defence = new DefenceCommand();
        defence.setMedicalDocumentation(medicalDocumentation);

        final AncillaryOrdersCommand ancillaryOrders = new AncillaryOrdersCommand();
        ancillaryOrders.setDetails(randomString());

        final AdditionalInformationCommand additionalInformation = new AdditionalInformationCommand();
        additionalInformation.setDefence(defence);

        final PreSentenceReportCommand preSentenceReport = new PreSentenceReportCommand();
        preSentenceReport.setDrugAssessment(randomBoolean());
        preSentenceReport.setProvideGuidance(randomString());

        final ProbationCommand probation = new ProbationCommand();
        probation.setDangerousnessAssessment(randomBoolean());
        probation.setPreSentenceReport(preSentenceReport);

        additionalInformation.setProbation(probation);

        final DefendantCommand defendant = new DefendantCommand();
        defendant.setDefendantId(defendantId);
        defendant.setAdditionalInformation(additionalInformation);
        return defendant;
    }

    public static DefendantCommand defaultDefendantWithoutAdditionalInfo(final UUID defendantId) {
        final MedicalDocumentationCommand medicalDocumentation = new MedicalDocumentationCommand();
        medicalDocumentation.setDetails(randomString());

        final DefenceCommand defence = new DefenceCommand();
        defence.setMedicalDocumentation(medicalDocumentation);

        final AncillaryOrdersCommand ancillaryOrders = new AncillaryOrdersCommand();
        ancillaryOrders.setDetails(randomString());

        final PreSentenceReportCommand preSentenceReport = new PreSentenceReportCommand();
        preSentenceReport.setDrugAssessment(randomBoolean());
        preSentenceReport.setProvideGuidance(randomString());

        final ProbationCommand probation = new ProbationCommand();
        probation.setDangerousnessAssessment(randomBoolean());
        probation.setPreSentenceReport(preSentenceReport);

        final DefendantCommand defendant = new DefendantCommand();
        defendant.setDefendantId(defendantId);

        return defendant;
    }

    public static UpdateDefendantCommand defaultUpdateDefendant() {
        return new UpdateDefendantCommand(CASE_ID, DEFENDANT_ID, new Person(), new Interpreter(),
                        "bail", randomUUID(), LocalDate.now(), "defenceSolicitorFirm");
    }

    public static UpdateDefendantCommand updateDefendantPersonOnlyNoUpdate() {
        return new UpdateDefendantCommand(CASE_ID, DEFENDANT_ID, new Person(), null, null, null,
                        null, null);
    }

    public static UpdateDefendantCommand updateDefendantPersonBailStatusUpdate(
                    final Person person, final String bailStatus) {
        return new UpdateDefendantCommand(CASE_ID, DEFENDANT_ID,
                        person,
                        null, bailStatus, null, null, null);
    }

    public static AddDefendant addDefendantWithPersonDetails(final String homePhone,
                    final String workPhone, final String mobile, final String fax,
                    final String email) {
        final DefendantOffenderDomain defendantOffender =
                        new DefendantOffenderDomain(YEAR, ORGANISATION_UNIT, NUMBER, CHECK_DIGIT);
        final CPR cpr = new CPR(defendantOffender, CJS_CODE, OFFENCE_SEQUENCE);
        final Offence offence = new Offence(OFFENCE_ID, POLICE_OFFENCE_ID, cpr, ASN_SEQUENCE_NUMBER,
                        CJS_CODE, REASON, DESCRIPTION, WORDING, CATEGORY, ARREST_DATE, START_DATE,
                        END_DATE, CHARGE_DATE);

        return new AddDefendant(CASE_ID, DEFENDANT_ID, VERSION, new Person(randomUUID(), "Mr", "John", "Humpries", LocalDate.now(),
                                        "British", "Male", homePhone, workPhone, mobile, fax, email,
                        new Address(
                                        "14", "Brunswick Road", "River Avenue", "London", "E16")),
                        POLICE_DEFENDANT_ID,
                        new ArrayList<Offence>() {
                            {
                                add(offence);
                            }
                        }, CASEURN);
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
