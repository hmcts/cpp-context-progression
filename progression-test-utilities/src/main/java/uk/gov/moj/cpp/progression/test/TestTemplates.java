package uk.gov.moj.cpp.progression.test;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static java.util.UUID.randomUUID;
import static uk.gov.justice.services.test.utils.core.random.RandomGenerator.STRING;
import static uk.gov.moj.cpp.progression.test.CoreTestTemplates.CoreTemplateArguments.toMap;
import static uk.gov.moj.cpp.progression.test.CoreTestTemplates.defaultArguments;

import uk.gov.justice.core.courts.Hearing;
import uk.gov.justice.core.courts.JurisdictionType;
import uk.gov.justice.core.courts.Personalisation;
import uk.gov.justice.core.courts.nces.Defendant;
import uk.gov.justice.core.courts.nces.DocumentContent;
import uk.gov.justice.core.courts.nces.NcesNotificationRequested;
import uk.gov.justice.core.courts.notification.EmailChannel;
import uk.gov.justice.core.courts.nowdocument.DefendantCaseOffence;
import uk.gov.justice.core.courts.nowdocument.FinancialOrderDetails;
import uk.gov.justice.core.courts.nowdocument.NextHearingCourtDetails;
import uk.gov.justice.core.courts.nowdocument.NowDocumentContent;
import uk.gov.justice.core.courts.nowdocument.NowDocumentRequest;
import uk.gov.justice.core.courts.nowdocument.NowResultDefinitionsText;
import uk.gov.justice.core.courts.nowdocument.Nowaddress;
import uk.gov.justice.core.courts.nowdocument.Nowdefendant;
import uk.gov.justice.core.courts.nowdocument.OrderAddressee;
import uk.gov.justice.core.courts.nowdocument.Prompt;
import uk.gov.justice.core.courts.nowdocument.ProsecutionCase;
import uk.gov.justice.core.courts.nowdocument.Result;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

@SuppressWarnings("squid:S1188")
public class TestTemplates {

    private final static String additional_property = "Additional_Property";

    private TestTemplates() {
    }


    public static NowDocumentRequest generateNowDocumentRequestTemplate(final UUID defendantId) {
        return generateNowDocumentRequestTemplate(defendantId, JurisdictionType.CROWN, false);
    }

    public static NowDocumentRequest generateNowDocumentRequestTemplate(final UUID defendantId,
                                                                        final JurisdictionType jurisdictionType, boolean convicted) {
        return generateNowDocumentRequestTemplate(defendantId, jurisdictionType, convicted, false);
    }

    public static NowDocumentRequest generateNowDocumentRequestTemplate(final UUID defendantId, final JurisdictionType jurisdictionType, boolean convicted, boolean remotePrintingRequired) {
        final UUID caseId = UUID.randomUUID();
        final UUID offenceId = UUID.randomUUID();
        final UUID nowTypeId = UUID.randomUUID();
        final UUID materialId = UUID.randomUUID();
        final UUID applicationId = UUID.randomUUID();

        final Hearing hearing = CoreTestTemplates.hearing(defaultArguments()
                .setJurisdictionType(jurisdictionType)
                .setStructure(toMap(caseId, toMap(defendantId, singletonList(offenceId))))
                .setConvicted(convicted))
                .build();

        return NowDocumentRequest.nowDocumentRequest()
                .withHearingId(hearing.getId())
                .withMaterialId(materialId)
                .withCaseId(caseId)
                .withApplicationId(applicationId)
                .withNowTypeId(nowTypeId)
                .withNowContent(nowContentTemplate())
                .withIsRemotePrintingRequired(remotePrintingRequired)
                .withEmailNotifications(emailChannelTemplate())
                .withDefendantId(defendantId)
                .withUsergroups(userGroupsTemplate())
                .withPriority("0.5 hours")
                .withTemplateName("TestTemplate")
                .build();

    }

    public static NowDocumentContent nowContentTemplate() {
        return NowDocumentContent.nowDocumentContent()
                .withCases(asList(prosecutionCaseTemplate(1), prosecutionCaseTemplate(2)))
                .withCaseUrns(asList("URN_1", "URN_2"))
                .withCourtCentreName("Sample Court Centre")
                .withCourtClerkName("Sample Court Clerk")
                .withDefendant(nowDefendantTemplate())
                .withFinancialOrderDetails(financialOrderDetailsTemplate())
                .withLjaCode("Sample LJA Code")
                .withLjaName("Sample LJA Name")
                .withNextHearingCourtDetails(nextHearingCourtDetailsTemplate())
                .withNowResultDefinitionsText(nowResultDefinitionsTextTemplate())
                .withNowText("Sample NOW Text")
                .withOrderAddressee(orderAddresseeTemplate())
                .withOrderDate(LocalDate.of(2019, 3, 21).toString())
                .withOrderName("Now_Content_Template_Order_Name")
                .withSubTemplateName("Now_Content_SubTemplate_Name")
                .build();
    }

    public static Nowdefendant nowDefendantTemplate() {
        return Nowdefendant.nowdefendant()
                .withAddress(nowAddressTemplate())
                .withDateOfBirth(LocalDate.of(1970, 1, 1).toString())
                .withName("Defendant_Name")
                .withAdditionalProperty(additional_property, Boolean.FALSE)
                .build();
    }

    public static Nowaddress nowAddressTemplate() {
        return Nowaddress.nowaddress()
                .withLine1("Line_1")
                .withLine2("Line_2")
                .withLine3("Line_3")
                .withLine4("Line_4")
                .withLine5("Line_5")
                .withPostCode("AB CD4")
                .build();
    }

    public static FinancialOrderDetails financialOrderDetailsTemplate() {
        return FinancialOrderDetails.financialOrderDetails()
                .withBacsAccountNumber("BACS1234567")
                .withAccountPaymentReference("Sample_Account_Payment_Reference")
                .withAccountingDivisionCode("Sample_Division_Code")
                .withTotalBalance("100.00")
                .withTotalAmountImposed("95.00")
                .withIsCrownCourt(Boolean.TRUE)
                .withEnforcementPhoneNumber("1234567890")
                .withEnforcementEmail("sample@sample.com")
                .withBacsSortCode("12-34-56")
                .withBacsBankName("45615975")
                .withAccountingDivisionCode("Sample_Accounting_Division_Code")
                .withEnforcementAddress(nowAddressTemplate())
                .build();
    }

    public static NextHearingCourtDetails nextHearingCourtDetailsTemplate() {
        return NextHearingCourtDetails.nextHearingCourtDetails()
                .withHearingTime("11:00")
                .withHearingDate(LocalDate.of(2019, 3, 25).toString())
                .withCourtName("Next_Hearing_Sample_Court_Name")
                .withCourtAddress(nowAddressTemplate())
                .build();
    }

    public static NowResultDefinitionsText nowResultDefinitionsTextTemplate() {
        return NowResultDefinitionsText.nowResultDefinitionsText()
                .withAdditionalProperty(additional_property, Boolean.FALSE)
                .build();
    }

    public static List<EmailChannel> emailChannelTemplate() {
        return asList(EmailChannel.emailChannel()
                        .withReplyToAddress("Sample of Reply to Address")
                        .withSendToAddress("Sample of the send to address")
                        .withTemplateId(UUID.randomUUID())
                        .withPersonalisation(Personalisation.personalisation().withAdditionalProperty(STRING.next(), STRING.next()).build())
                        .build(),
                EmailChannel.emailChannel()
                        .withReplyToAddress("Sample of Reply to Address")
                        .withSendToAddress("Sample of the send to address")
                        .withTemplateId(UUID.randomUUID())
                        .withPersonalisation(Personalisation.personalisation().withAdditionalProperty(STRING.next(), STRING.next()).build())
                        .build()
        );

    }

    public static OrderAddressee orderAddresseeTemplate() {
        return OrderAddressee.orderAddressee()
                .withAddress(nowAddressTemplate())
                .withName("Order_Addresse_Template")
                .build();
    }

    public static List<String> userGroupsTemplate() {
        return asList("UserGroup1", "UserGroup2", "Listing Officers");
    }

    public static ProsecutionCase prosecutionCaseTemplate(final int value) {
        return ProsecutionCase.prosecutionCase()
                .withDefendantCaseOffences(asList(defendantCaseOffenceTemplate(1), defendantCaseOffenceTemplate(2)))
                .withDefendantCaseResults(asList(resultTemplate(1), resultTemplate(2)))
                .withUrn("URN_" + value)
                .build();
    }

    public static DefendantCaseOffence defendantCaseOffenceTemplate(final int value) {
        return DefendantCaseOffence.defendantCaseOffence()
                .withConvictionDate(LocalDate.of(2019, 3, 21).toString())
                .withResults(asList(resultTemplate(1), resultTemplate(2)))
                .withStartDate(LocalDate.of(2019, 3, 25).toString())
                .withWording("DefendantCaseOffence_Wordings_" + value)
                .build();
    }

    public static Result resultTemplate(final int value) {
        return Result.result()
                .withLabel("Result_Label_" + value)
                .withAdditionalProperty("Additional_Property", Boolean.FALSE)
                .withPrompts(asList(promptTemplate(1), promptTemplate(2)))
                .build();
    }

    public static Prompt promptTemplate(final int value) {
        return Prompt.prompt()
                .withLabel("Test_Label_" + value)
                .withValue("Test_value_" + value)
                .build();
    }

    public static NcesNotificationRequested generateNcesNotificationRequested(){
        return  NcesNotificationRequested.ncesNotificationRequested()
                        .withCaseId(randomUUID())
                        .withDefendantId(randomUUID())
                        .withHearingId(randomUUID())
                        .withMaterialId(randomUUID())
                        .withDocumentContent(
                                DocumentContent.documentContent()
                                        .withUrn("232CASE34")
                                        .withDefendant(Defendant.defendant().build())
                                        .withGobAccountNumber("gobnumber")
                                        .withAmendmentType("Granted refused")
                                        .withDivisionCode("203")
                                        .withDefendantCaseOffences(
                                                Arrays.asList(defendantCaseOffenceTemplate(12)))

                                        .build())
                        .withEmailNotifications(asList(
                                EmailChannel.emailChannel()
                                        .withReplyToAddress("Sample of Reply to Address")
                                        .withSendToAddress("Sample of the send to address")
                                        .withTemplateId(UUID.randomUUID())
                                        .withPersonalisation(Personalisation.personalisation().withAdditionalProperty(STRING.next(), STRING.next()).build())
                                        .build()
                        ))
                        .build();
    }


}
