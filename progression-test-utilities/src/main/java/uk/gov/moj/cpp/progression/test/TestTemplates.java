package uk.gov.moj.cpp.progression.test;

import static com.google.common.collect.Lists.newArrayList;
import static java.lang.Boolean.TRUE;
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
import uk.gov.justice.core.courts.nowdocument.NowDistribution;
import uk.gov.justice.core.courts.nowdocument.NowDocumentContent;
import uk.gov.justice.core.courts.nowdocument.NowDocumentRequest;
import uk.gov.justice.core.courts.nowdocument.Nowaddress;
import uk.gov.justice.core.courts.nowdocument.Nowdefendant;
import uk.gov.justice.core.courts.nowdocument.OrderAddressee;
import uk.gov.justice.core.courts.nowdocument.OrderCourt;
import uk.gov.justice.core.courts.nowdocument.Prompt;
import uk.gov.justice.core.courts.nowdocument.ProsecutionCase;
import uk.gov.justice.core.courts.nowdocument.Result;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

@SuppressWarnings("squid:S1188")
public class TestTemplates {

    public static final String SAMPLE_OF_REPLY_TO_ADDRESS = "test.reply@hmcts.net";
    public static final String SAMPLE_OF_THE_SEND_TO_ADDRESS = "test.sendTo@hmcts.net";
    private static final List<String> VISIBLE_USER_GROUP_LIST = newArrayList("Defence");
    private static final List<String> NON_VISIBLE_USER_GROUP_LIST = newArrayList("Defence", "Probation");
    private TestTemplates() {
    }

    public static NowDocumentRequest generateNowDocumentRequestTemplate(final UUID defendantId) {
        return generateNowDocumentRequestTemplate(defendantId, JurisdictionType.CROWN, false);
    }

    public static NowDocumentRequest generateNowDocumentRequestTemplateWithVisibleUserList(final UUID defendantId) {
        return generateNowDocumentRequestTemplate(defendantId, JurisdictionType.CROWN, false, VISIBLE_USER_GROUP_LIST, null);
    }

    public static NowDocumentRequest generateNowDocumentRequestTemplateWithNonVisibleUserList(final UUID defendantId) {
        return generateNowDocumentRequestTemplate(defendantId, JurisdictionType.CROWN, false, null, NON_VISIBLE_USER_GROUP_LIST);
    }

    public static NowDocumentRequest generateNowDocumentRequestTemplate(final UUID defendantId, final JurisdictionType jurisdictionType, boolean convicted) {
        return generateNowDocumentRequestTemplate(defendantId, jurisdictionType, convicted, null, null);
    }

    public static NowDocumentRequest generateNowDocumentRequestTemplate(final UUID defendantId, final JurisdictionType jurisdictionType, boolean convicted, List<String> visibleUserList, List<String> nonVisibleUserList) {
        final UUID caseId = UUID.randomUUID();
        final UUID offenceId = UUID.randomUUID();
        final UUID nowTypeId = UUID.randomUUID();
        final UUID materialId = UUID.randomUUID();

        final Hearing hearing = CoreTestTemplates.hearing(defaultArguments()
                .setJurisdictionType(jurisdictionType)
                .setStructure(toMap(caseId, toMap(defendantId, singletonList(offenceId))))
                .setConvicted(convicted))
                .build();

        return NowDocumentRequest.nowDocumentRequest()
                .withHearingId(hearing.getId())
                .withMaterialId(materialId)
                .withNowTypeId(nowTypeId)
                .withNowContent(nowContentTemplate())
                .withMasterDefendantId(defendantId)
                .withTemplateName("EDT_TestTemplate")
                .withBilingualTemplateName("BilingualTestTemplate")
                .withCases(asList(caseId))
                .withStorageRequired(true)
                .withVisibleToUserGroups(visibleUserList)
                .withNotVisibleToUserGroups(nonVisibleUserList)
                .withNowDistribution(nowDistribution())
                .build();

    }

    public static NowDistribution nowDistribution() {
        return NowDistribution.nowDistribution()
                .withEmail(true)
                .withBilingualEmailTemplateName("Email Template")
                .withFirstClassLetter(TRUE)
                .withSecondClassLetter(TRUE)
                .build();
    }

    public static NowDocumentContent nowContentTemplate() {
        return NowDocumentContent.nowDocumentContent()
                .withCases(asList(prosecutionCaseTemplate(1), prosecutionCaseTemplate(2)))
                .withCourtClerkName("Sample Court Clerk")
                .withDefendant(nowDefendantTemplate())
                .withFinancialOrderDetails(financialOrderDetailsTemplate())
                .withNextHearingCourtDetails(nextHearingCourtDetailsTemplate())
                .withOrderAddressee(orderAddresseeTemplate())
                .withOrderDate(LocalDate.of(2019, 3, 21).toString())
                .withOrderName("Now_Content_Template_Order_Name")
                .withOrderingCourt(OrderCourt.orderCourt().withWelshCourtCentre(true).build())
                .build();
    }

    public static Nowdefendant nowDefendantTemplate() {
        return Nowdefendant.nowdefendant()
                .withAddress(nowAddressTemplate())
                .withDateOfBirth(LocalDate.of(1970, 1, 1).toString())
                .withName("Defendant_Name")
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
                .withEmailAddress1("emailAddress1@test.com")
                .withEmailAddress2("emailAddress2@test.com")
                .build();
    }

    public static FinancialOrderDetails financialOrderDetailsTemplate() {
        return FinancialOrderDetails.financialOrderDetails()
                .withBacsAccountNumber("BACS1234567")
                .withAccountPaymentReference("Sample_Account_Payment_Reference")
                .withAccountingDivisionCode(77)
                .withTotalBalance("100.00")
                .withTotalAmountImposed("95.00")
                .withEnforcementPhoneNumber("1234567890")
                .withEnforcementEmail("sample@sample.com")
                .withBacsSortCode("12-34-56")
                .withBacsBankName("45615975")
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
                .withReference("URN_" + value)
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
                .withPrompts(asList(promptTemplate(1), promptTemplate(2)))
                .build();
    }

    public static Prompt promptTemplate(final int value) {
        return Prompt.prompt()
                .withLabel("Test_Label_" + value)
                .withValue("Test_value_" + value)
                .build();
    }

    public static NcesNotificationRequested generateNcesNotificationRequested() {
        return NcesNotificationRequested.ncesNotificationRequested()
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
                                .withDivisionCode("77")
                                .withDefendantCaseOffences(
                                        Arrays.asList(defendantCaseOffenceTemplate(12)))

                                .build())
                .withEmailNotifications(asList(
                        EmailChannel.emailChannel()
                                .withReplyToAddress(SAMPLE_OF_REPLY_TO_ADDRESS)
                                .withSendToAddress(SAMPLE_OF_THE_SEND_TO_ADDRESS)
                                .withTemplateId(UUID.randomUUID())
                                .withPersonalisation(Personalisation.personalisation().withAdditionalProperty(STRING.next(), STRING.next()).build())
                                .build()
                ))
                .build();
    }
}
