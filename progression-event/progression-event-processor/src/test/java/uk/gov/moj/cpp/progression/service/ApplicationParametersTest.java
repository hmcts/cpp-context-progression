package uk.gov.moj.cpp.progression.service;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static uk.gov.justice.services.test.utils.core.reflection.ReflectionUtil.setField;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ApplicationParametersTest {

    private ApplicationParameters applicationParameters;

    private static final String APPLICATION_TEMPLATE_ID = "app-template-id";
    private static final String DEFENCE_INSTRUCTION_TEMPLATE_ID = "defence-instruction-id";
    private static final String DEFENCE_DISASSOCIATION_TEMPLATE_ID = "defence-disassociation-id";
    private static final String DEFENCE_ASSOCIATION_TEMPLATE_ID = "defence-association-id";
    private static final String NCES_EMAIL_TEMPLATE_ID = "nces-email-id";
    private static final String CPS_COURT_DOCUMENT_TEMPLATE_ID = "cps-court-doc-id";
    private static final String CPS_DEFENDANT_COURT_DOCUMENT_TEMPLATE_ID = "cps-defendant-court-doc-id";
    private static final String AZURE_FUNCTION_HOST_NAME = "https://azure-host.example.com";
    private static final String AZURE_SCAN_MANAGER_CONTAINER_NAME = "scan-manager-container";
    private static final String SET_CASE_EJECTED_FUNCTION_PATH = "/api/setCaseEjected";
    private static final String RELAY_CASE_ON_CPP_FUNCTION_PATH = "/api/relayCaseOnCpp";
    private static final String DEFENDANT_PROCEEDINGS_CONCLUDED_APIM_URL = "http://apim.example.com/conclude";
    private static final String SUBSCRIPTION_KEY = "subscription-key-123";
    private static final String NOTIFY_DEFENCE_OF_NEW_MATERIAL_TEMPLATE_ID = "notify-defence-id";
    private static final String NOTIFY_HEARING_TEMPLATE_ID = "notify-hearing-id";
    private static final String END_CLIENT_HOST = "https://end-client.example.com";
    private static final String PRISON_COURT_REGISTER_EMAIL_TEMPLATE_ID = "pcr-email-id";
    private static final String COURT_REGISTER_EMAIL_TEMPLATE_ID = "cr-email-id";
    private static final String INFORMANT_REGISTER_EMAIL_TEMPLATE_ID = "ir-email-id";
    private static final String NOW_EMAIL_TEMPLATE_ID = "now-email-id";
    private static final String NOW_SLA_EMAIL_TEMPLATE_ID = "now-sla-email-id";
    private static final String NOW_EXTRADITION_EMAIL_TEMPLATE_ID = "now-extradition-email-id";
    private static final String NOW_EXTRADITION_SLA_EMAIL_TEMPLATE_ID = "now-extradition-sla-id";
    private static final String UNSCHEDULED_HEARING_ALLOCATION_EMAIL_TEMPLATE_ID = "unscheduled-hearing-id";
    private static final String CASE_ATA_GLANCE_URI = "prosecution-casefile/case-at-a-glance/";
    private static final String SUMMONS_APPROVED_AND_SUPPRESSED_TEMPLATE_ID = "summons-suppressed-id";
    private static final String SUMMONS_APPROVED_AND_NOT_SUPPRESSED_TEMPLATE_ID = "summons-not-suppressed-id";
    private static final String SUMMONS_REJECTED_TEMPLATE_ID = "summons-rejected-id";
    private static final String STATDEC_SEND_APPOINTMENT_LETTER_TEMPLATE_ID = "statdec-appointment-id";
    private static final String ONLINE_GUILTY_PLEA_COURT_HEARING_ENGLISH_ID = "guilty-court-english-id";
    private static final String ONLINE_GUILTY_PLEA_COURT_HEARING_WELSH_ID = "guilty-court-welsh-id";
    private static final String ONLINE_GUILTY_PLEA_NO_COURT_HEARING_ENGLISH_ID = "guilty-no-court-english-id";
    private static final String ONLINE_GUILTY_PLEA_NO_COURT_HEARING_WELSH_ID = "guilty-no-court-welsh-id";
    private static final String ONLINE_NOT_GUILTY_PLEA_ENGLISH_ID = "not-guilty-english-id";
    private static final String ONLINE_NOT_GUILTY_PLEA_WELSH_ID = "not-guilty-welsh-id";
    private static final String ONLINE_PLEA_PROSECUTOR_TEMPLATE_ID = "plea-prosecutor-id";
    private static final String RETRY_TIMES = "5";
    private static final String RETRY_INTERVAL = "2000";
    private static final String ADD_DEFENDANT_RETRY_INTERVALS = "1-5-10-30-60";

    @BeforeEach
    void setUp() {
        applicationParameters = new ApplicationParameters();
        setField(applicationParameters, "applicationTemplateId", APPLICATION_TEMPLATE_ID);
        setField(applicationParameters, "defenceInstructionTemplateId", DEFENCE_INSTRUCTION_TEMPLATE_ID);
        setField(applicationParameters, "defenceDisassociationTemplateId", DEFENCE_DISASSOCIATION_TEMPLATE_ID);
        setField(applicationParameters, "defenceAssociationTemplateId", DEFENCE_ASSOCIATION_TEMPLATE_ID);
        setField(applicationParameters, "ncesEmailTemplateId", NCES_EMAIL_TEMPLATE_ID);
        setField(applicationParameters, "cpsCourtDocumentTemplateId", CPS_COURT_DOCUMENT_TEMPLATE_ID);
        setField(applicationParameters, "cpsDefendantCourtDocumentTemplateId", CPS_DEFENDANT_COURT_DOCUMENT_TEMPLATE_ID);
        setField(applicationParameters, "azureFunctionHostName", AZURE_FUNCTION_HOST_NAME);
        setField(applicationParameters, "azureScanManagerContainerName", AZURE_SCAN_MANAGER_CONTAINER_NAME);
        setField(applicationParameters, "setCaseEjectedFunctionPath", SET_CASE_EJECTED_FUNCTION_PATH);
        setField(applicationParameters, "relayCaseOnCppFunctionPath", RELAY_CASE_ON_CPP_FUNCTION_PATH);
        setField(applicationParameters, "defendantProceedingsConcludedApimUrl", DEFENDANT_PROCEEDINGS_CONCLUDED_APIM_URL);
        setField(applicationParameters, "subscriptionKey", SUBSCRIPTION_KEY);
        setField(applicationParameters, "notifyDefenceOfNewMaterialTemplateId", NOTIFY_DEFENCE_OF_NEW_MATERIAL_TEMPLATE_ID);
        setField(applicationParameters, "notifyHearingTemplateId", NOTIFY_HEARING_TEMPLATE_ID);
        setField(applicationParameters, "endClientHost", END_CLIENT_HOST);
        setField(applicationParameters, "prisonCourtRegisterEmailTemplateId", PRISON_COURT_REGISTER_EMAIL_TEMPLATE_ID);
        setField(applicationParameters, "courtRegisterEmailTemplateId", COURT_REGISTER_EMAIL_TEMPLATE_ID);
        setField(applicationParameters, "informantRegisterEmailTemplateId", INFORMANT_REGISTER_EMAIL_TEMPLATE_ID);
        setField(applicationParameters, "nowEmailTemplateId", NOW_EMAIL_TEMPLATE_ID);
        setField(applicationParameters, "nowSlaEmailTemplateId", NOW_SLA_EMAIL_TEMPLATE_ID);
        setField(applicationParameters, "nowExtraditionEmailTemplateId", NOW_EXTRADITION_EMAIL_TEMPLATE_ID);
        setField(applicationParameters, "nowExtraditionSlaEmailTemplateId", NOW_EXTRADITION_SLA_EMAIL_TEMPLATE_ID);
        setField(applicationParameters, "unscheduledHearingAllocationEmailTemplateId", UNSCHEDULED_HEARING_ALLOCATION_EMAIL_TEMPLATE_ID);
        setField(applicationParameters, "caseAtaGlanceURI", CASE_ATA_GLANCE_URI);
        setField(applicationParameters, "summonsApprovedAndSuppressedTemplateId", SUMMONS_APPROVED_AND_SUPPRESSED_TEMPLATE_ID);
        setField(applicationParameters, "summonsApprovedAndNotSuppressedTemplateId", SUMMONS_APPROVED_AND_NOT_SUPPRESSED_TEMPLATE_ID);
        setField(applicationParameters, "summonsRejectedTemplateId", SUMMONS_REJECTED_TEMPLATE_ID);
        setField(applicationParameters, "statDecSendAppointmentLetterTemplateId", STATDEC_SEND_APPOINTMENT_LETTER_TEMPLATE_ID);
        setField(applicationParameters, "onlineGuiltyPleaCourtHearingEnglishTemplateId", ONLINE_GUILTY_PLEA_COURT_HEARING_ENGLISH_ID);
        setField(applicationParameters, "onlineGuiltyPleaCourtHearingWelshTemplateId", ONLINE_GUILTY_PLEA_COURT_HEARING_WELSH_ID);
        setField(applicationParameters, "onlineGuiltyPleaNoCourtHearingEnglishTemplateId", ONLINE_GUILTY_PLEA_NO_COURT_HEARING_ENGLISH_ID);
        setField(applicationParameters, "onlineGuiltyPleaNoCourtHearingWelshTemplateId", ONLINE_GUILTY_PLEA_NO_COURT_HEARING_WELSH_ID);
        setField(applicationParameters, "onlineNotGuiltyPleaEnglishTemplateId", ONLINE_NOT_GUILTY_PLEA_ENGLISH_ID);
        setField(applicationParameters, "onlineNotGuiltyPleaWelshTemplateId", ONLINE_NOT_GUILTY_PLEA_WELSH_ID);
        setField(applicationParameters, "onlinePleaProsecutorTemplateId", ONLINE_PLEA_PROSECUTOR_TEMPLATE_ID);
        setField(applicationParameters, "retryTimes", RETRY_TIMES);
        setField(applicationParameters, "retryInterval", RETRY_INTERVAL);
        setField(applicationParameters, "addDefendantRetryIntervals", ADD_DEFENDANT_RETRY_INTERVALS);
    }

    @Test
    void shouldReturnApplicationTemplateId() {
        assertThat(applicationParameters.getApplicationTemplateId(), is(APPLICATION_TEMPLATE_ID));
    }

    @Test
    void shouldReturnDefenceInstructionTemplateId() {
        assertThat(applicationParameters.getDefenceInstructionTemplateId(), is(DEFENCE_INSTRUCTION_TEMPLATE_ID));
    }

    @Test
    void shouldReturnDefenceDisassociationTemplateId() {
        assertThat(applicationParameters.getDefenceDisassociationTemplateId(), is(DEFENCE_DISASSOCIATION_TEMPLATE_ID));
    }

    @Test
    void shouldReturnDefenceAssociationTemplateId() {
        assertThat(applicationParameters.getDefenceAssociationTemplateId(), is(DEFENCE_ASSOCIATION_TEMPLATE_ID));
    }

    @Test
    void shouldReturnNcesEmailTemplateId() {
        assertThat(applicationParameters.getNcesEmailTemplateId(), is(NCES_EMAIL_TEMPLATE_ID));
    }

    @Test
    void shouldReturnCpsCourtDocumentTemplateId() {
        assertThat(applicationParameters.getCpsCourtDocumentTemplateId(), is(CPS_COURT_DOCUMENT_TEMPLATE_ID));
    }

    @Test
    void shouldReturnCpsDefendantCourtDocumentTemplateId() {
        assertThat(applicationParameters.getCpsDefendantCourtDocumentTemplateId(), is(CPS_DEFENDANT_COURT_DOCUMENT_TEMPLATE_ID));
    }

    @Test
    void shouldReturnAzureFunctionHostName() {
        assertThat(applicationParameters.getAzureFunctionHostName(), is(AZURE_FUNCTION_HOST_NAME));
    }

    @Test
    void shouldReturnAzureScanManagerContainerName() {
        assertThat(applicationParameters.getAzureScanManagerContainerName(), is(AZURE_SCAN_MANAGER_CONTAINER_NAME));
    }

    @Test
    void shouldReturnSetCaseEjectedFunctionPath() {
        assertThat(applicationParameters.getSetCaseEjectedFunctionPath(), is(SET_CASE_EJECTED_FUNCTION_PATH));
    }

    @Test
    void shouldReturnRelayCaseOnCppFunctionPath() {
        assertThat(applicationParameters.getRelayCaseOnCppFunctionPath(), is(RELAY_CASE_ON_CPP_FUNCTION_PATH));
    }

    @Test
    void shouldReturnDefendantProceedingsConcludedApimUrl() {
        assertThat(applicationParameters.getDefendantProceedingsConcludedApimUrl(), is(DEFENDANT_PROCEEDINGS_CONCLUDED_APIM_URL));
    }

    @Test
    void shouldReturnSubscriptionKey() {
        assertThat(applicationParameters.getSubscriptionKey(), is(SUBSCRIPTION_KEY));
    }

    @Test
    void shouldReturnNotifyDefenceOfNewMaterialTemplateId() {
        assertThat(applicationParameters.getNotifyDefenceOfNewMaterialTemplateId(), is(NOTIFY_DEFENCE_OF_NEW_MATERIAL_TEMPLATE_ID));
    }

    @Test
    void shouldReturnNotifyHearingTemplateId() {
        assertThat(applicationParameters.getNotifyHearingTemplateId(), is(NOTIFY_HEARING_TEMPLATE_ID));
    }

    @Test
    void shouldReturnEndClientHost() {
        assertThat(applicationParameters.getEndClientHost(), is(END_CLIENT_HOST));
    }

    @Test
    void shouldReturnUnscheduledHearingAllocationEmailTemplateId() {
        assertThat(applicationParameters.getUnscheduledHearingAllocationEmailTemplateId(), is(UNSCHEDULED_HEARING_ALLOCATION_EMAIL_TEMPLATE_ID));
    }

    @Test
    void shouldReturnCaseAtaGlanceURI() {
        assertThat(applicationParameters.getCaseAtaGlanceURI(), is(CASE_ATA_GLANCE_URI));
    }

    @Test
    void shouldReturnSummonsApprovedAndSuppressedTemplateId() {
        assertThat(applicationParameters.getSummonsApprovedAndSuppressedTemplateId(), is(SUMMONS_APPROVED_AND_SUPPRESSED_TEMPLATE_ID));
    }

    @Test
    void shouldReturnSummonsApprovedAndNotSuppressedTemplateId() {
        assertThat(applicationParameters.getSummonsApprovedAndNotSuppressedTemplateId(), is(SUMMONS_APPROVED_AND_NOT_SUPPRESSED_TEMPLATE_ID));
    }

    @Test
    void shouldReturnSummonsRejectedTemplateId() {
        assertThat(applicationParameters.getSummonsRejectedTemplateId(), is(SUMMONS_REJECTED_TEMPLATE_ID));
    }

    @Test
    void shouldReturnStatDecSendAppointmentLetterTemplateId() {
        assertThat(applicationParameters.getStatDecSendAppointmentLetterTemplateId(), is(STATDEC_SEND_APPOINTMENT_LETTER_TEMPLATE_ID));
    }

    @Test
    void shouldReturnOnlineGuiltyPleaCourtHearingEnglishTemplateId() {
        assertThat(applicationParameters.getOnlineGuiltyPleaCourtHearingEnglishTemplateId(), is(ONLINE_GUILTY_PLEA_COURT_HEARING_ENGLISH_ID));
    }

    @Test
    void shouldReturnOnlineGuiltyPleaCourtHearingWelshTemplateId() {
        assertThat(applicationParameters.getOnlineGuiltyPleaCourtHearingWelshTemplateId(), is(ONLINE_GUILTY_PLEA_COURT_HEARING_WELSH_ID));
    }

    @Test
    void shouldReturnOnlineGuiltyPleaNoCourtHearingEnglishTemplateId() {
        assertThat(applicationParameters.getOnlineGuiltyPleaNoCourtHearingEnglishTemplateId(), is(ONLINE_GUILTY_PLEA_NO_COURT_HEARING_ENGLISH_ID));
    }

    @Test
    void shouldReturnOnlineGuiltyPleaNoCourtHearingWelshTemplateId() {
        assertThat(applicationParameters.getOnlineGuiltyPleaNoCourtHearingWelshTemplateId(), is(ONLINE_GUILTY_PLEA_NO_COURT_HEARING_WELSH_ID));
    }

    @Test
    void shouldReturnOnlineNotGuiltyPleaEnglishTemplateId() {
        assertThat(applicationParameters.getOnlineNotGuiltyPleaEnglishTemplateId(), is(ONLINE_NOT_GUILTY_PLEA_ENGLISH_ID));
    }

    @Test
    void shouldReturnOnlineNotGuiltyPleaWelshTemplateId() {
        assertThat(applicationParameters.getOnlineNotGuiltyPleaWelshTemplateId(), is(ONLINE_NOT_GUILTY_PLEA_WELSH_ID));
    }

    @Test
    void shouldReturnOnlinePleaProsecutorTemplateId() {
        assertThat(applicationParameters.getOnlinePleaProsecutorTemplateId(), is(ONLINE_PLEA_PROSECUTOR_TEMPLATE_ID));
    }

    @Test
    void shouldReturnRetryTimes() {
        assertThat(applicationParameters.getRetryTimes(), is(RETRY_TIMES));
    }

    @Test
    void shouldReturnRetryInterval() {
        assertThat(applicationParameters.getRetryInterval(), is(RETRY_INTERVAL));
    }

    @Test
    void shouldReturnAddDefendantRetryIntervals() {
        assertThat(applicationParameters.getAddDefendantRetryIntervals(), is(ADD_DEFENDANT_RETRY_INTERVALS));
    }

    @Test
    void getEmailTemplateIdShouldReturnPrisonCourtRegisterTemplateIdForPcrStandard() {
        assertThat(applicationParameters.getEmailTemplateId("pcr_standard"), is(PRISON_COURT_REGISTER_EMAIL_TEMPLATE_ID));
    }

    @Test
    void getEmailTemplateIdShouldReturnInformantRegisterTemplateIdForIrStandard() {
        assertThat(applicationParameters.getEmailTemplateId("ir_standard"), is(INFORMANT_REGISTER_EMAIL_TEMPLATE_ID));
    }

    @Test
    void getEmailTemplateIdShouldReturnCourtRegisterTemplateIdForCrStandard() {
        assertThat(applicationParameters.getEmailTemplateId("cr_standard"), is(COURT_REGISTER_EMAIL_TEMPLATE_ID));
    }

    @Test
    void getEmailTemplateIdShouldReturnNowEmailTemplateIdForNowStandardTemplate() {
        assertThat(applicationParameters.getEmailTemplateId("now_standard_template"), is(NOW_EMAIL_TEMPLATE_ID));
    }

    @Test
    void getEmailTemplateIdShouldReturnNowSlaEmailTemplateIdForNowSlaTemplate() {
        assertThat(applicationParameters.getEmailTemplateId("now_sla_template"), is(NOW_SLA_EMAIL_TEMPLATE_ID));
    }

    @Test
    void getEmailTemplateIdShouldReturnNowExtraditionEmailTemplateIdForNowExtraditionStandardTemplate() {
        assertThat(applicationParameters.getEmailTemplateId("now_extradition_standard_template"), is(NOW_EXTRADITION_EMAIL_TEMPLATE_ID));
    }

    @Test
    void getEmailTemplateIdShouldReturnNowExtraditionSlaEmailTemplateIdForNowExtraditionSlaTemplate() {
        assertThat(applicationParameters.getEmailTemplateId("now_extradition_sla_template"), is(NOW_EXTRADITION_SLA_EMAIL_TEMPLATE_ID));
    }

    @Test
    void getEmailTemplateIdShouldReturnEmptyStringForUnknownTemplateName() {
        assertThat(applicationParameters.getEmailTemplateId("unknown_template"), is(""));
    }

    @Test
    void getEmailTemplateIdShouldReturnEmptyStringForNullTemplateName() {
        assertThat(applicationParameters.getEmailTemplateId(null), is(""));
    }
}
