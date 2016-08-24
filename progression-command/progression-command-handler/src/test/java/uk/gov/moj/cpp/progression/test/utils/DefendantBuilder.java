package uk.gov.moj.cpp.progression.test.utils;

import java.util.Random;
import java.util.UUID;

import org.apache.commons.lang3.RandomStringUtils;

import uk.gov.moj.cpp.progression.command.defendant.AdditionalInformationCommand;
import uk.gov.moj.cpp.progression.command.defendant.AncillaryOrdersCommand;
import uk.gov.moj.cpp.progression.command.defendant.DefenceCommand;
import uk.gov.moj.cpp.progression.command.defendant.DefendantCommand;
import uk.gov.moj.cpp.progression.command.defendant.MedicalDocumentationCommand;
import uk.gov.moj.cpp.progression.command.defendant.PreSentenceReportCommand;
import uk.gov.moj.cpp.progression.command.defendant.ProbationCommand;

public class DefendantBuilder {


    public static DefendantCommand defaultDefendant() {
        UUID defendantId = randomUUID();
        UUID defendantProgressionId = randomUUID();
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
        defendant.setDefendantProgressionId(defendantProgressionId);
        defendant.setAdditionalInformation(additionalInformation);
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
