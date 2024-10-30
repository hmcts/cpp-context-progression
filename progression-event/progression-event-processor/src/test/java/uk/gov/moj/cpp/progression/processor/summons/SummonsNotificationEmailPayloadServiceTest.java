package uk.gov.moj.cpp.progression.processor.summons;

import static com.google.common.collect.Lists.newArrayList;
import static java.lang.String.format;
import static java.lang.System.lineSeparator;
import static java.time.ZoneOffset.UTC;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static java.util.UUID.fromString;
import static java.util.UUID.randomUUID;
import static java.util.stream.Collectors.joining;
import static org.apache.commons.lang3.RandomStringUtils.randomAlphabetic;
import static org.apache.commons.lang3.RandomStringUtils.randomAlphanumeric;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.hamcrest.core.IsNull.nullValue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static uk.gov.justice.core.courts.ConfirmedProsecutionCaseId.confirmedProsecutionCaseId;
import static uk.gov.justice.core.courts.CourtCentre.courtCentre;
import static uk.gov.justice.core.courts.Defendant.defendant;
import static uk.gov.justice.core.courts.ListDefendantRequest.listDefendantRequest;
import static uk.gov.justice.core.courts.SummonsApprovedOutcome.summonsApprovedOutcome;
import static uk.gov.justice.core.courts.SummonsData.summonsData;
import static uk.gov.justice.core.courts.SummonsDataPrepared.summonsDataPrepared;
import static uk.gov.justice.core.courts.SummonsType.APPLICATION;
import static uk.gov.justice.core.courts.SummonsType.BREACH;
import static uk.gov.justice.core.courts.SummonsType.FIRST_HEARING;
import static uk.gov.justice.core.courts.SummonsType.SJP_REFERRAL;
import static uk.gov.justice.core.courts.SummonsType.YOUTH;
import static uk.gov.justice.core.courts.summons.SummonsAddressee.summonsAddressee;
import static uk.gov.justice.core.courts.summons.SummonsDefendant.summonsDefendant;
import static uk.gov.justice.core.courts.summons.SummonsDocumentContent.summonsDocumentContent;
import static uk.gov.justice.core.courts.summons.SummonsHearingCourtDetails.summonsHearingCourtDetails;
import static uk.gov.justice.core.courts.summons.SummonsProsecutor.summonsProsecutor;
import static uk.gov.justice.services.test.utils.core.random.RandomGenerator.BOOLEAN;
import static uk.gov.justice.services.test.utils.core.random.RandomGenerator.STRING;
import static uk.gov.justice.services.test.utils.core.random.RandomGenerator.values;

import uk.gov.justice.core.courts.CourtApplicationPartyListingNeeds;
import uk.gov.justice.core.courts.Defendant;
import uk.gov.justice.core.courts.SummonsData;
import uk.gov.justice.core.courts.SummonsDataPrepared;
import uk.gov.justice.core.courts.SummonsType;
import uk.gov.justice.core.courts.notification.EmailChannel;
import uk.gov.justice.core.courts.summons.SummonsDocumentContent;
import uk.gov.justice.core.courts.summons.SummonsProsecutor;
import uk.gov.justice.services.test.utils.core.random.RandomGenerator;
import uk.gov.moj.cpp.material.url.MaterialUrlGenerator;
import uk.gov.moj.cpp.progression.service.ApplicationParameters;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import com.google.common.collect.ImmutableList;
import org.junit.jupiter.api.Test;

import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class SummonsNotificationEmailPayloadServiceTest {

    private static final UUID CASE_ID = randomUUID();
    private static final UUID DEFENDANT_ID = randomUUID();
    private static final UUID DEFENDANT_ID_2 = randomUUID();
    private static final UUID DEFENDANT_ID_3 = randomUUID();
    private static final UUID APPLICATION_ID = randomUUID();
    private static final UUID SUBJECT_ID = randomUUID();
    private static final String CASE_URN = STRING.next();
    private static final String APPLICATION_REFERENCE_NUMBER = STRING.next();
    private static final String DEFENDANT_FIRST_NAME = STRING.next();
    private static final String DEFENDANT_MIDDLE_NAME = STRING.next();
    private static final String DEFENDANT_LAST_NAME = STRING.next();
    private static final String ADDRESSEE_FIRST_NAME = STRING.next();
    private static final String ADDRESSEE_MIDDLE_NAME = STRING.next();
    private static final String ADDRESSEE_LAST_NAME = STRING.next();
    private static final String PARENT_FIRST_NAME = STRING.next();
    private static final String PARENT_MIDDLE_NAME = STRING.next();
    private static final String PARENT_LAST_NAME = STRING.next();
    private static final UUID COURT_CENTRE_ID = UUID.fromString("f8254db1-1683-483e-afb3-b87fde5a0a26");
    private static final UUID COURT_ROOM_ID = randomUUID();
    private static final String PROSECUTION_AUTHORITY_REFERENCE = STRING.next();
    private static final String PROSECUTION_AUTHORITY_REFERENCE_2 = STRING.next();
    private static final String PROSECUTION_AUTHORITY_REFERENCE_3 = STRING.next();
    private static final boolean SUMMONS_SUPPRESSED = BOOLEAN.next();
    private static final ZonedDateTime HEARING_DATE_TIME = ZonedDateTime.of(2013, 10, 26, 23, 30, 0, 0, UTC);
    private static final boolean DEFENDANT_IS_YOUTH = BOOLEAN.next();
    private static final UUID MATERIAL_ID = randomUUID();
    private static final String MATERIAL_URL = STRING.next();
    private static final String COURT_NAME = STRING.next();
    private static final String EMAIL_ADDRESS = RandomGenerator.EMAIL_ADDRESS.next();
    private static final String HEARING_TIME = "10:30 AM";
    private static final String DEFENDANT_2_FIRST_NAME = STRING.next();
    private static final String DEFENDANT_2_MIDDLE_NAME = STRING.next();
    private static final String DEFENDANT_2_LAST_NAME = STRING.next();
    private static final String DEFENDANT_3_FIRST_NAME = STRING.next();
    private static final String DEFENDANT_3_MIDDLE_NAME = STRING.next();
    private static final String DEFENDANT_3_LAST_NAME = STRING.next();
    private static final SummonsType SUMMONS_REQUIRED = values(FIRST_HEARING, BREACH, APPLICATION, YOUTH).next();
    private static final String PROSECUTOR_NAME = STRING.next();

    private static final String SUMMONS_APPROVED_AND_NOT_SUPPRESSED_TEMPLATE_ID = randomUUID().toString();
    private static final String SUMMONS_APPROVED_AND_SUPPRESSED_TEMPLATE_ID = randomUUID().toString();
    private static final String SUMMONS_REJECTED_TEMPLATE_ID = randomUUID().toString();

    private static final String PROPERTY_CASE_REFERENCE = "caseReference";
    private static final String PROPERTY_DEFENDANT_DETAILS = "defendantDetails";
    private static final String PROPERTY_COURT_LOCATION = "courtLocation";
    private static final String PROPERTY_HEARING_DATE = "hearingDate";
    private static final String PROPERTY_HEARING_TIME = "hearingTime";
    private static final String PROPERTY_DEFENDANT_IS_YOUTH = "defendantIsYouth";
    private static final String PROPERTY_REJECTED_REASONS = "rejectedReasons";
    private static final String LINE_SEPARATOR = System.lineSeparator();

    @Mock
    private ApplicationParameters applicationParameters;

    @Mock
    private MaterialUrlGenerator materialUrlGenerator;

    @InjectMocks
    private final SummonsNotificationEmailPayloadService summonsNotificationEmailPayloadService = new SummonsNotificationEmailPayloadService();

    @Test
    public void shouldNotGenerateEmailNotificationForCaseDefendantWhenSummonsRequestedForSJPReferral() {
        final SummonsDataPrepared summonsDataPrepared = getSummonsDataPreparedForCase();
        final SummonsDocumentContent summonsDocumentContent = getCaseSummonsDocumentContentForDefendant();
        final Defendant defendant = getDefendant(DEFENDANT_ID, PROSECUTION_AUTHORITY_REFERENCE);
        final List<UUID> defendantIds = ImmutableList.of(DEFENDANT_ID);
        
        final Optional<EmailChannel> result = summonsNotificationEmailPayloadService.getEmailChannelForCaseDefendant(
                summonsDataPrepared, summonsDocumentContent, EMAIL_ADDRESS, defendantIds, defendant, emptyList(), BOOLEAN.next(),
                DEFENDANT_IS_YOUTH, MATERIAL_ID, SJP_REFERRAL);

        assertThat(result.isPresent(), is(false));
    }

    @Test
    public void shouldNotGenerateEmailNotificationForApplicationWhenSummonsRequestedForSJPReferral() {
        final SummonsDataPrepared summonsDataPrepared = getSummonsDataPreparedForApplication();
        final SummonsDocumentContent summonsDocumentContent = getApplicationSummonsDocumentContentForAddressee();
        
        final Optional<EmailChannel> result = summonsNotificationEmailPayloadService.getEmailChannelForApplicationAddressee(
                summonsDataPrepared, summonsDocumentContent, EMAIL_ADDRESS, BOOLEAN.next(),
                DEFENDANT_IS_YOUTH, MATERIAL_ID, SJP_REFERRAL);

        assertThat(result.isPresent(), is(false));
    }

    @Test
    public void shouldNotGenerateEmailNotificationForCaseDefendantWhenEmailNotPresent() {
        final SummonsDataPrepared summonsDataPrepared = getSummonsDataPreparedForCase();
        final SummonsDocumentContent summonsDocumentContent = getCaseSummonsDocumentContentForDefendant();
        final Defendant defendant = getDefendant(DEFENDANT_ID, PROSECUTION_AUTHORITY_REFERENCE);
        final List<UUID> defendantIds = ImmutableList.of(DEFENDANT_ID);

        final Optional<EmailChannel> result = summonsNotificationEmailPayloadService.getEmailChannelForCaseDefendant(
                summonsDataPrepared, summonsDocumentContent, null, defendantIds, defendant, emptyList(), BOOLEAN.next(),
                DEFENDANT_IS_YOUTH, MATERIAL_ID, SUMMONS_REQUIRED);

        assertThat(result.isPresent(), is(false));
    }

    @Test
    public void shouldNotGenerateEmailNotificationForCaseDefendantWhenEmailIsEmptyForProsecutor() {
        final SummonsDataPrepared summonsDataPrepared = getSummonsDataPreparedForCase();
        final SummonsDocumentContent summonsDocumentContent = getCaseSummonsDocumentContentForDefendant();
        final Defendant defendant = getDefendant(DEFENDANT_ID, PROSECUTION_AUTHORITY_REFERENCE);
        final List<UUID> defendantIds = ImmutableList.of(DEFENDANT_ID);

        final Optional<EmailChannel> result = summonsNotificationEmailPayloadService.getEmailChannelForCaseDefendant(
                summonsDataPrepared, summonsDocumentContent, "", defendantIds, defendant, emptyList(), BOOLEAN.next(),
                DEFENDANT_IS_YOUTH, MATERIAL_ID, SUMMONS_REQUIRED);

        assertThat(result.isPresent(), is(false));
    }

    @Test
    public void shouldNotGenerateEmailNotificationForApplicationWhenEmailNotPresentForProsecutor() {
        final SummonsDataPrepared summonsDataPrepared = getSummonsDataPreparedForApplication();
        final SummonsDocumentContent summonsDocumentContent = getApplicationSummonsDocumentContentForAddressee();

        final Optional<EmailChannel> result = summonsNotificationEmailPayloadService.getEmailChannelForApplicationAddressee(
                summonsDataPrepared, summonsDocumentContent, "", BOOLEAN.next(),
                DEFENDANT_IS_YOUTH, MATERIAL_ID, SUMMONS_REQUIRED);

        assertThat(result.isPresent(), is(false));
    }

    @Test
    public void shouldNotGenerateEmailNotificationForApplicationWhenEmailIsEmptyForProsecutor() {
        final SummonsDataPrepared summonsDataPrepared = getSummonsDataPreparedForApplication();
        final SummonsDocumentContent summonsDocumentContent = getApplicationSummonsDocumentContentForAddressee();
        
        final Optional<EmailChannel> result = summonsNotificationEmailPayloadService.getEmailChannelForApplicationAddressee(
                summonsDataPrepared, summonsDocumentContent, "", BOOLEAN.next(),
                DEFENDANT_IS_YOUTH, MATERIAL_ID, SUMMONS_REQUIRED);

        assertThat(result.isPresent(), is(false));
    }

    @Test
    public void shouldNotGenerateParentOrGuardianEmailNotificationForCaseDefendantWhenSendingForRemotePrintingOrSummonsNotSuppressed() {
        final SummonsDataPrepared summonsDataPrepared = getSummonsDataPreparedForCase();
        final SummonsDocumentContent parentSummonsDocumentContent = getCaseSummonsDocumentContentForDefendantParent();
        final Defendant defendant = getDefendant(DEFENDANT_ID, PROSECUTION_AUTHORITY_REFERENCE);
        final List<UUID> defendantIds = ImmutableList.of(DEFENDANT_ID);
        final boolean sendForRemotePrinting = true;

        final Optional<EmailChannel> result = summonsNotificationEmailPayloadService.getEmailChannelForCaseDefendantParent(
                summonsDataPrepared, parentSummonsDocumentContent, EMAIL_ADDRESS, defendantIds, defendant, emptyList(),
                sendForRemotePrinting, MATERIAL_ID, SUMMONS_REQUIRED);

        assertThat(result.isPresent(), is(false));
    }

    @Test
    public void shouldNotGenerateParentOrGuardianEmailNotificationForApplicationWhenSendingForRemotePrintingOrSummonsNotSuppressed() {
        final SummonsDataPrepared summonsDataPrepared = getSummonsDataPreparedForApplication();
        final SummonsDocumentContent parentSummonsDocumentContent = getApplicationSummonsDocumentContentForAddresseeParent();
        final boolean sendForRemotePrinting = true;
        
        final Optional<EmailChannel> result = summonsNotificationEmailPayloadService.getEmailChannelForApplicationAddresseeParent(
                summonsDataPrepared, parentSummonsDocumentContent, EMAIL_ADDRESS, sendForRemotePrinting,
                MATERIAL_ID, SUMMONS_REQUIRED);

        assertThat(result.isPresent(), is(false));
    }

    @Test
    public void shouldGenerateEmailNotificationForCaseSummonsWhenSuppressed() {
        final SummonsDataPrepared summonsDataPrepared = getSummonsDataPreparedForCase();
        final SummonsDocumentContent summonsDocumentContent = getCaseSummonsDocumentContentForDefendant();
        final Defendant defendant = getDefendant(DEFENDANT_ID, PROSECUTION_AUTHORITY_REFERENCE);
        final List<UUID> defendantIds = ImmutableList.of(DEFENDANT_ID);
        final boolean sendForRemotePrinting = false;

        when(applicationParameters.getSummonsApprovedAndSuppressedTemplateId()).thenReturn(SUMMONS_APPROVED_AND_SUPPRESSED_TEMPLATE_ID);
        when(materialUrlGenerator.pdfFileStreamUrlFor(MATERIAL_ID)).thenReturn(MATERIAL_URL);

        final Optional<EmailChannel> optionalResult = summonsNotificationEmailPayloadService.getEmailChannelForCaseDefendant(
                summonsDataPrepared, summonsDocumentContent, EMAIL_ADDRESS, defendantIds, defendant, emptyList(), sendForRemotePrinting,
                DEFENDANT_IS_YOUTH, MATERIAL_ID, SUMMONS_REQUIRED);

        assertThat(optionalResult.isPresent(), is(true));
        final EmailChannel result = optionalResult.get();
        assertThat(result.getTemplateId(), is(fromString(SUMMONS_APPROVED_AND_SUPPRESSED_TEMPLATE_ID)));
        assertThat(result.getMaterialUrl(), is(MATERIAL_URL));
        assertThat(result.getSendToAddress(), is(EMAIL_ADDRESS));
        assertThat(result.getPersonalisation(), is(notNullValue()));
        final Map<String, Object> additionalProperties = result.getPersonalisation().getAdditionalProperties();
        assertThat(additionalProperties, is(notNullValue()));
        assertThat(additionalProperties.entrySet(), hasSize(6));
        assertThat(additionalProperties.get(PROPERTY_CASE_REFERENCE), is(CASE_URN));
        assertThat(additionalProperties.get(PROPERTY_DEFENDANT_DETAILS), is(format("%s %s %s, %s", DEFENDANT_FIRST_NAME, DEFENDANT_MIDDLE_NAME, DEFENDANT_LAST_NAME, PROSECUTION_AUTHORITY_REFERENCE)));
        assertThat(additionalProperties.get(PROPERTY_COURT_LOCATION), is(COURT_NAME));
        assertThat(additionalProperties.get(PROPERTY_HEARING_DATE), is("27 Oct 2013"));
        assertThat(additionalProperties.get(PROPERTY_HEARING_TIME), is(HEARING_TIME));
        assertThat(additionalProperties.get(PROPERTY_DEFENDANT_IS_YOUTH), is(DEFENDANT_IS_YOUTH));

        assertThat(result.getReplyToAddress(), is(nullValue()));
        assertThat(result.getReplyToAddressId(), is(nullValue()));
        verify(applicationParameters).getSummonsApprovedAndSuppressedTemplateId();
        verify(materialUrlGenerator).pdfFileStreamUrlFor(eq(MATERIAL_ID));
    }

    @Test
    public void shouldGenerateEmailNotificationForCaseSummonsWhenSuppressedAndWithEmptyProsecutionAuthorityReference() {
        final SummonsDataPrepared summonsDataPrepared = getSummonsDataPreparedForCase();
        final SummonsDocumentContent summonsDocumentContent = getCaseSummonsDocumentContentForDefendant();
        final Defendant defendant = getDefendant(DEFENDANT_ID, null);
        final List<UUID> defendantIds = ImmutableList.of(DEFENDANT_ID);
        final boolean sendForRemotePrinting = false;

        when(applicationParameters.getSummonsApprovedAndSuppressedTemplateId()).thenReturn(SUMMONS_APPROVED_AND_SUPPRESSED_TEMPLATE_ID);
        when(materialUrlGenerator.pdfFileStreamUrlFor(MATERIAL_ID)).thenReturn(MATERIAL_URL);

        final Optional<EmailChannel> optionalResult = summonsNotificationEmailPayloadService.getEmailChannelForCaseDefendant(
                summonsDataPrepared, summonsDocumentContent, EMAIL_ADDRESS, defendantIds, defendant, emptyList(), sendForRemotePrinting,
                DEFENDANT_IS_YOUTH, MATERIAL_ID, SUMMONS_REQUIRED);

        assertThat(optionalResult.isPresent(), is(true));
        final EmailChannel result = optionalResult.get();
        assertThat(result.getTemplateId(), is(fromString(SUMMONS_APPROVED_AND_SUPPRESSED_TEMPLATE_ID)));
        assertThat(result.getMaterialUrl(), is(MATERIAL_URL));
        assertThat(result.getSendToAddress(), is(EMAIL_ADDRESS));
        assertThat(result.getPersonalisation(), is(notNullValue()));
        final Map<String, Object> additionalProperties = result.getPersonalisation().getAdditionalProperties();
        assertThat(additionalProperties, is(notNullValue()));
        assertThat(additionalProperties.entrySet(), hasSize(6));
        assertThat(additionalProperties.get(PROPERTY_CASE_REFERENCE), is(CASE_URN));
        assertThat(additionalProperties.get(PROPERTY_DEFENDANT_DETAILS), is(format("%s %s %s", DEFENDANT_FIRST_NAME, DEFENDANT_MIDDLE_NAME, DEFENDANT_LAST_NAME)));
        assertThat(additionalProperties.get(PROPERTY_COURT_LOCATION), is(COURT_NAME));
        assertThat(additionalProperties.get(PROPERTY_HEARING_DATE), is("27 Oct 2013"));
        assertThat(additionalProperties.get(PROPERTY_HEARING_TIME), is(HEARING_TIME));
        assertThat(additionalProperties.get(PROPERTY_DEFENDANT_IS_YOUTH), is(DEFENDANT_IS_YOUTH));

        assertThat(result.getReplyToAddress(), is(nullValue()));
        assertThat(result.getReplyToAddressId(), is(nullValue()));
        verify(applicationParameters).getSummonsApprovedAndSuppressedTemplateId();
        verify(materialUrlGenerator).pdfFileStreamUrlFor(eq(MATERIAL_ID));
    }

    @Test
    public void shouldGenerateEmailNotificationForDefendantParentWhenCaseSummonsIsSuppressed() {
        final SummonsDataPrepared summonsDataPrepared = getSummonsDataPreparedForCase();
        final SummonsDocumentContent parentSummonsDocumentContent = getCaseSummonsDocumentContentForDefendantParent();
        final Defendant defendant = getDefendant(DEFENDANT_ID, PROSECUTION_AUTHORITY_REFERENCE);
        final List<UUID> defendantIds = ImmutableList.of(DEFENDANT_ID);
        final boolean sendForRemotePrinting = false;

        when(applicationParameters.getSummonsApprovedAndSuppressedTemplateId()).thenReturn(SUMMONS_APPROVED_AND_SUPPRESSED_TEMPLATE_ID);
        when(materialUrlGenerator.pdfFileStreamUrlFor(MATERIAL_ID)).thenReturn(MATERIAL_URL);

        final Optional<EmailChannel> optionalResult = summonsNotificationEmailPayloadService.getEmailChannelForCaseDefendantParent(
                summonsDataPrepared, parentSummonsDocumentContent, EMAIL_ADDRESS, defendantIds, defendant,
                emptyList(), sendForRemotePrinting, MATERIAL_ID, SUMMONS_REQUIRED);

        assertThat(optionalResult.isPresent(), is(true));
        final EmailChannel result = optionalResult.get();
        assertThat(result.getTemplateId(), is(fromString(SUMMONS_APPROVED_AND_SUPPRESSED_TEMPLATE_ID)));
        assertThat(result.getMaterialUrl(), is(MATERIAL_URL));
        assertThat(result.getSendToAddress(), is(EMAIL_ADDRESS));
        assertThat(result.getPersonalisation(), is(notNullValue()));
        final Map<String, Object> additionalProperties = result.getPersonalisation().getAdditionalProperties();
        assertThat(additionalProperties, is(notNullValue()));
        assertThat(additionalProperties.entrySet(), hasSize(6));
        assertThat(additionalProperties.get(PROPERTY_CASE_REFERENCE), is(CASE_URN));
        assertThat(additionalProperties.get(PROPERTY_DEFENDANT_DETAILS), is(format("%s %s %s (parent/guardian of %s %s %s), %s",
                PARENT_FIRST_NAME, PARENT_MIDDLE_NAME, PARENT_LAST_NAME, DEFENDANT_FIRST_NAME, DEFENDANT_MIDDLE_NAME, DEFENDANT_LAST_NAME, PROSECUTION_AUTHORITY_REFERENCE)));
        assertThat(additionalProperties.get(PROPERTY_COURT_LOCATION), is(COURT_NAME));
        assertThat(additionalProperties.get(PROPERTY_HEARING_DATE), is("27 Oct 2013"));
        assertThat(additionalProperties.get(PROPERTY_HEARING_TIME), is(HEARING_TIME));
        assertThat(additionalProperties.get(PROPERTY_DEFENDANT_IS_YOUTH), is(false));

        assertThat(result.getReplyToAddress(), is(nullValue()));
        assertThat(result.getReplyToAddressId(), is(nullValue()));
        verify(applicationParameters).getSummonsApprovedAndSuppressedTemplateId();
        verify(materialUrlGenerator).pdfFileStreamUrlFor(eq(MATERIAL_ID));
    }

    @Test
    public void shouldGenerateEmailNotificationForDefendantParentWhenCaseSummonsIsSuppressedAndWithEmptyProsecutionAuthorityReference() {
        final SummonsDataPrepared summonsDataPrepared = getSummonsDataPreparedForCase();
        final SummonsDocumentContent parentSummonsDocumentContent = getCaseSummonsDocumentContentForDefendantParent();
        final Defendant defendant = getDefendant(DEFENDANT_ID, null);
        final List<UUID> defendantIds = ImmutableList.of(DEFENDANT_ID);
        final boolean sendForRemotePrinting = false;

        when(applicationParameters.getSummonsApprovedAndSuppressedTemplateId()).thenReturn(SUMMONS_APPROVED_AND_SUPPRESSED_TEMPLATE_ID);
        when(materialUrlGenerator.pdfFileStreamUrlFor(MATERIAL_ID)).thenReturn(MATERIAL_URL);

        final Optional<EmailChannel> optionalResult = summonsNotificationEmailPayloadService.getEmailChannelForCaseDefendantParent(
                summonsDataPrepared, parentSummonsDocumentContent, EMAIL_ADDRESS, defendantIds, defendant,
                emptyList(), sendForRemotePrinting, MATERIAL_ID, SUMMONS_REQUIRED);

        assertThat(optionalResult.isPresent(), is(true));
        final EmailChannel result = optionalResult.get();
        assertThat(result.getTemplateId(), is(fromString(SUMMONS_APPROVED_AND_SUPPRESSED_TEMPLATE_ID)));
        assertThat(result.getMaterialUrl(), is(MATERIAL_URL));
        assertThat(result.getSendToAddress(), is(EMAIL_ADDRESS));
        assertThat(result.getPersonalisation(), is(notNullValue()));
        final Map<String, Object> additionalProperties = result.getPersonalisation().getAdditionalProperties();
        assertThat(additionalProperties, is(notNullValue()));
        assertThat(additionalProperties.entrySet(), hasSize(6));
        assertThat(additionalProperties.get(PROPERTY_CASE_REFERENCE), is(CASE_URN));
        assertThat(additionalProperties.get(PROPERTY_DEFENDANT_DETAILS), is(format("%s %s %s (parent/guardian of %s %s %s)",
                PARENT_FIRST_NAME, PARENT_MIDDLE_NAME, PARENT_LAST_NAME, DEFENDANT_FIRST_NAME, DEFENDANT_MIDDLE_NAME, DEFENDANT_LAST_NAME)));
        assertThat(additionalProperties.get(PROPERTY_COURT_LOCATION), is(COURT_NAME));
        assertThat(additionalProperties.get(PROPERTY_HEARING_DATE), is("27 Oct 2013"));
        assertThat(additionalProperties.get(PROPERTY_HEARING_TIME), is(HEARING_TIME));
        assertThat(additionalProperties.get(PROPERTY_DEFENDANT_IS_YOUTH), is(false));

        assertThat(result.getReplyToAddress(), is(nullValue()));
        assertThat(result.getReplyToAddressId(), is(nullValue()));
        verify(applicationParameters).getSummonsApprovedAndSuppressedTemplateId();
        verify(materialUrlGenerator).pdfFileStreamUrlFor(eq(MATERIAL_ID));
    }

    @Test
    public void shouldBuildEmailNotificationForApplicationSummonsWhenSuppressed() {
        final SummonsDataPrepared summonsDataPrepared = getSummonsDataPreparedForApplication();
        final SummonsDocumentContent summonsDocumentContent = getApplicationSummonsDocumentContentForAddressee();
        final boolean sendForRemotePrinting = false;

        when(applicationParameters.getSummonsApprovedAndSuppressedTemplateId()).thenReturn(SUMMONS_APPROVED_AND_SUPPRESSED_TEMPLATE_ID);
        when(materialUrlGenerator.pdfFileStreamUrlFor(MATERIAL_ID)).thenReturn(MATERIAL_URL);

        final Optional<EmailChannel> optionalResult = summonsNotificationEmailPayloadService.getEmailChannelForApplicationAddressee(
                summonsDataPrepared, summonsDocumentContent, EMAIL_ADDRESS, sendForRemotePrinting,
                DEFENDANT_IS_YOUTH, MATERIAL_ID, SUMMONS_REQUIRED);

        assertThat(optionalResult.isPresent(), is(true));
        final EmailChannel result = optionalResult.get();
        assertThat(result.getTemplateId(), is(fromString(SUMMONS_APPROVED_AND_SUPPRESSED_TEMPLATE_ID)));
        assertThat(result.getMaterialUrl(), is(MATERIAL_URL));
        assertThat(result.getSendToAddress(), is(EMAIL_ADDRESS));
        assertThat(result.getPersonalisation(), is(notNullValue()));
        final Map<String, Object> additionalProperties = result.getPersonalisation().getAdditionalProperties();
        assertThat(additionalProperties, is(notNullValue()));
        assertThat(additionalProperties.entrySet(), hasSize(6));
        assertThat(additionalProperties.get(PROPERTY_CASE_REFERENCE), is(APPLICATION_REFERENCE_NUMBER));
        assertThat(additionalProperties.get(PROPERTY_DEFENDANT_DETAILS), is(format("%s %s %s", ADDRESSEE_FIRST_NAME, ADDRESSEE_MIDDLE_NAME, ADDRESSEE_LAST_NAME)));
        assertThat(additionalProperties.get(PROPERTY_COURT_LOCATION), is(COURT_NAME));
        assertThat(additionalProperties.get(PROPERTY_HEARING_DATE), is("27 Oct 2013"));
        assertThat(additionalProperties.get(PROPERTY_HEARING_TIME), is(HEARING_TIME));
        assertThat(additionalProperties.get(PROPERTY_DEFENDANT_IS_YOUTH), is(DEFENDANT_IS_YOUTH));

        assertThat(result.getReplyToAddress(), is(nullValue()));
        assertThat(result.getReplyToAddressId(), is(nullValue()));
        verify(applicationParameters).getSummonsApprovedAndSuppressedTemplateId();
        verify(materialUrlGenerator).pdfFileStreamUrlFor(eq(MATERIAL_ID));
    }

    @Test
    public void shouldBuildEmailNotificationForAddresseeParentWhenApplicationSummonsIsSuppressed() {
        final SummonsDataPrepared summonsDataPrepared = getSummonsDataPreparedForApplication();
        final SummonsDocumentContent parentSummonsDocumentContent = getApplicationSummonsDocumentContentForAddresseeParent();
        final boolean sendForRemotePrinting = false;

        when(applicationParameters.getSummonsApprovedAndSuppressedTemplateId()).thenReturn(SUMMONS_APPROVED_AND_SUPPRESSED_TEMPLATE_ID);
        when(materialUrlGenerator.pdfFileStreamUrlFor(MATERIAL_ID)).thenReturn(MATERIAL_URL);

        final Optional<EmailChannel> optionalResult = summonsNotificationEmailPayloadService.getEmailChannelForApplicationAddresseeParent(
                summonsDataPrepared, parentSummonsDocumentContent, EMAIL_ADDRESS, sendForRemotePrinting,
                MATERIAL_ID, SUMMONS_REQUIRED);

        assertThat(optionalResult.isPresent(), is(true));
        final EmailChannel result = optionalResult.get();
        assertThat(result.getTemplateId(), is(fromString(SUMMONS_APPROVED_AND_SUPPRESSED_TEMPLATE_ID)));
        assertThat(result.getMaterialUrl(), is(MATERIAL_URL));
        assertThat(result.getSendToAddress(), is(EMAIL_ADDRESS));
        assertThat(result.getPersonalisation(), is(notNullValue()));
        final Map<String, Object> additionalProperties = result.getPersonalisation().getAdditionalProperties();
        assertThat(additionalProperties, is(notNullValue()));
        assertThat(additionalProperties.entrySet(), hasSize(6));
        assertThat(additionalProperties.get(PROPERTY_CASE_REFERENCE), is(APPLICATION_REFERENCE_NUMBER));
        assertThat(additionalProperties.get(PROPERTY_DEFENDANT_DETAILS), is(format("%s %s %s (parent/guardian of %s %s %s)",
                PARENT_FIRST_NAME, PARENT_MIDDLE_NAME, PARENT_LAST_NAME, ADDRESSEE_FIRST_NAME, ADDRESSEE_MIDDLE_NAME, ADDRESSEE_LAST_NAME)));
        assertThat(additionalProperties.get(PROPERTY_COURT_LOCATION), is(COURT_NAME));
        assertThat(additionalProperties.get(PROPERTY_HEARING_DATE), is("27 Oct 2013"));
        assertThat(additionalProperties.get(PROPERTY_HEARING_TIME), is(HEARING_TIME));
        assertThat(additionalProperties.get(PROPERTY_DEFENDANT_IS_YOUTH), is(false));

        assertThat(result.getReplyToAddress(), is(nullValue()));
        assertThat(result.getReplyToAddressId(), is(nullValue()));
        verify(applicationParameters).getSummonsApprovedAndSuppressedTemplateId();
        verify(materialUrlGenerator).pdfFileStreamUrlFor(eq(MATERIAL_ID));
    }

    @Test
    public void shouldBuildEmailNotificationForCaseSummonsWhenNotSuppressed() {
        final SummonsDataPrepared summonsDataPrepared = getSummonsDataPreparedForCase();
        final SummonsDocumentContent summonsDocumentContent = getCaseSummonsDocumentContentForDefendant();
        final Defendant defendant = getDefendant(DEFENDANT_ID, PROSECUTION_AUTHORITY_REFERENCE);
        final List<UUID> defendantIds = ImmutableList.of(DEFENDANT_ID);
        final boolean sendForRemotePrinting = true;

        when(applicationParameters.getSummonsApprovedAndNotSuppressedTemplateId()).thenReturn(SUMMONS_APPROVED_AND_NOT_SUPPRESSED_TEMPLATE_ID);

        final Optional<EmailChannel> optionalResult = summonsNotificationEmailPayloadService.getEmailChannelForCaseDefendant(
                summonsDataPrepared, summonsDocumentContent, EMAIL_ADDRESS, defendantIds, defendant, newArrayList(), sendForRemotePrinting,
                DEFENDANT_IS_YOUTH, MATERIAL_ID, SUMMONS_REQUIRED);

        assertThat(optionalResult.isPresent(), is(true));
        final EmailChannel result = optionalResult.get();
        assertThat(result.getTemplateId(), is(fromString(SUMMONS_APPROVED_AND_NOT_SUPPRESSED_TEMPLATE_ID)));
        assertThat(result.getMaterialUrl(), is(nullValue()));
        assertThat(result.getSendToAddress(), is(EMAIL_ADDRESS));
        assertThat(result.getPersonalisation(), is(notNullValue()));
        final Map<String, Object> additionalProperties = result.getPersonalisation().getAdditionalProperties();
        assertThat(additionalProperties, is(notNullValue()));
        assertThat(additionalProperties.entrySet(), hasSize(5));
        assertThat(additionalProperties.get(PROPERTY_CASE_REFERENCE), is(CASE_URN));
        assertThat(additionalProperties.get(PROPERTY_DEFENDANT_DETAILS), is(format("%s %s %s, %s", DEFENDANT_FIRST_NAME, DEFENDANT_MIDDLE_NAME, DEFENDANT_LAST_NAME, PROSECUTION_AUTHORITY_REFERENCE)));
        assertThat(additionalProperties.get(PROPERTY_COURT_LOCATION), is(COURT_NAME));
        assertThat(additionalProperties.get(PROPERTY_HEARING_DATE), is("27 Oct 2013"));
        assertThat(additionalProperties.get(PROPERTY_HEARING_TIME), is(HEARING_TIME));

        assertThat(result.getReplyToAddress(), is(nullValue()));
        assertThat(result.getReplyToAddressId(), is(nullValue()));
        verify(applicationParameters).getSummonsApprovedAndNotSuppressedTemplateId();
        verifyNoInteractions(materialUrlGenerator);
    }

    @Test
    public void shouldBuildEmailNotificationForApplicationWhenNotSuppressed() {
        final SummonsDataPrepared summonsDataPrepared = getSummonsDataPreparedForApplication();
        final SummonsDocumentContent summonsDocumentContent = getApplicationSummonsDocumentContentForAddressee();
        final boolean sendForRemotePrinting = true;

        when(applicationParameters.getSummonsApprovedAndNotSuppressedTemplateId()).thenReturn(SUMMONS_APPROVED_AND_NOT_SUPPRESSED_TEMPLATE_ID);

        final Optional<EmailChannel> optionalResult = summonsNotificationEmailPayloadService.getEmailChannelForApplicationAddressee(
                summonsDataPrepared, summonsDocumentContent, EMAIL_ADDRESS, sendForRemotePrinting,
                DEFENDANT_IS_YOUTH, MATERIAL_ID, SUMMONS_REQUIRED);

        assertThat(optionalResult.isPresent(), is(true));
        final EmailChannel result = optionalResult.get();
        assertThat(result.getTemplateId(), is(fromString(SUMMONS_APPROVED_AND_NOT_SUPPRESSED_TEMPLATE_ID)));
        assertThat(result.getMaterialUrl(), is(nullValue()));
        assertThat(result.getSendToAddress(), is(EMAIL_ADDRESS));
        assertThat(result.getPersonalisation(), is(notNullValue()));
        final Map<String, Object> additionalProperties = result.getPersonalisation().getAdditionalProperties();
        assertThat(additionalProperties, is(notNullValue()));
        assertThat(additionalProperties.entrySet(), hasSize(5));
        assertThat(additionalProperties.get(PROPERTY_CASE_REFERENCE), is(APPLICATION_REFERENCE_NUMBER));
        assertThat(additionalProperties.get(PROPERTY_DEFENDANT_DETAILS), is(format("%s %s %s", ADDRESSEE_FIRST_NAME, ADDRESSEE_MIDDLE_NAME, ADDRESSEE_LAST_NAME)));
        assertThat(additionalProperties.get(PROPERTY_COURT_LOCATION), is(COURT_NAME));
        assertThat(additionalProperties.get(PROPERTY_HEARING_DATE), is("27 Oct 2013"));
        assertThat(additionalProperties.get(PROPERTY_HEARING_TIME), is(HEARING_TIME));

        assertThat(result.getReplyToAddress(), is(nullValue()));
        assertThat(result.getReplyToAddressId(), is(nullValue()));
        verify(applicationParameters).getSummonsApprovedAndNotSuppressedTemplateId();
        verifyNoInteractions(materialUrlGenerator);
    }

    @Test
    public void shouldBuildOnlyOneEmailNotificationWithAllDefendantDetailsForCaseSummonsWhenNotSuppressedAndCaseHasMultipleDefendants() {
        final SummonsDataPrepared summonsDataPrepared = getSummonsDataPreparedForCaseWithMultipleDefendants();
        final List<UUID> defendantIds = ImmutableList.of(DEFENDANT_ID, DEFENDANT_ID_2, DEFENDANT_ID_3);
        final boolean sendForRemotePrinting = true;
        SummonsDocumentContent summonsDocumentContent = getCaseSummonsDocumentContentForDefendant();
        Defendant defendant = getDefendant(DEFENDANT_ID, PROSECUTION_AUTHORITY_REFERENCE);
        final List<String> defendantDetails = newArrayList();

        when(applicationParameters.getSummonsApprovedAndNotSuppressedTemplateId()).thenReturn(SUMMONS_APPROVED_AND_NOT_SUPPRESSED_TEMPLATE_ID);

        //email notification not generated for first defendant
        Optional<EmailChannel> optionalResult = summonsNotificationEmailPayloadService.getEmailChannelForCaseDefendant(
                summonsDataPrepared, summonsDocumentContent, EMAIL_ADDRESS, defendantIds, defendant, defendantDetails, sendForRemotePrinting,
                DEFENDANT_IS_YOUTH, MATERIAL_ID, SUMMONS_REQUIRED);

        assertThat(optionalResult.isPresent(), is(false));
        assertThat(defendantDetails, hasSize(1));
        assertThat(defendantDetails.get(0), is(format("%s %s %s, %s", DEFENDANT_FIRST_NAME, DEFENDANT_MIDDLE_NAME, DEFENDANT_LAST_NAME, PROSECUTION_AUTHORITY_REFERENCE)));

        summonsDocumentContent = getCaseSummonsDocumentContentForDefendant2();
        defendant = getDefendant(DEFENDANT_ID_2, PROSECUTION_AUTHORITY_REFERENCE_2);

        //email notification not generated for second defendant
        optionalResult = summonsNotificationEmailPayloadService.getEmailChannelForCaseDefendant(
                summonsDataPrepared, summonsDocumentContent, EMAIL_ADDRESS, defendantIds, defendant, defendantDetails, sendForRemotePrinting,
                DEFENDANT_IS_YOUTH, MATERIAL_ID, SUMMONS_REQUIRED);

        assertThat(optionalResult.isPresent(), is(false));
        assertThat(defendantDetails, hasSize(2));
        assertThat(defendantDetails.get(1), is(format("%s %s %s, %s", DEFENDANT_2_FIRST_NAME, DEFENDANT_2_MIDDLE_NAME, DEFENDANT_2_LAST_NAME, PROSECUTION_AUTHORITY_REFERENCE_2)));

        summonsDocumentContent = getCaseSummonsDocumentContentForDefendant3();
        defendant = getDefendant(DEFENDANT_ID_3, PROSECUTION_AUTHORITY_REFERENCE_3);

        //combined email notification generated for third/last defendant
        optionalResult = summonsNotificationEmailPayloadService.getEmailChannelForCaseDefendant(
                summonsDataPrepared, summonsDocumentContent, EMAIL_ADDRESS, defendantIds, defendant, defendantDetails, sendForRemotePrinting,
                DEFENDANT_IS_YOUTH, MATERIAL_ID, SUMMONS_REQUIRED);

        assertThat(optionalResult.isPresent(), is(true));
        assertThat(defendantDetails, hasSize(3));
        assertThat(defendantDetails.get(2), is(format("%s %s %s, %s", DEFENDANT_3_FIRST_NAME, DEFENDANT_3_MIDDLE_NAME, DEFENDANT_3_LAST_NAME, PROSECUTION_AUTHORITY_REFERENCE_3)));

        final EmailChannel result = optionalResult.get();
        assertThat(result.getTemplateId(), is(fromString(SUMMONS_APPROVED_AND_NOT_SUPPRESSED_TEMPLATE_ID)));
        assertThat(result.getMaterialUrl(), is(nullValue()));
        assertThat(result.getSendToAddress(), is(EMAIL_ADDRESS));
        assertThat(result.getPersonalisation(), is(notNullValue()));
        final Map<String, Object> additionalProperties = result.getPersonalisation().getAdditionalProperties();
        assertThat(additionalProperties, is(notNullValue()));
        assertThat(additionalProperties.entrySet(), hasSize(5));
        assertThat(additionalProperties.get(PROPERTY_CASE_REFERENCE), is(CASE_URN));
        assertThat(additionalProperties.get(PROPERTY_DEFENDANT_DETAILS), is(format("%s %s %s, %s" + LINE_SEPARATOR + "%s %s %s, %s" + LINE_SEPARATOR + "%s %s %s, %s",
                DEFENDANT_FIRST_NAME, DEFENDANT_MIDDLE_NAME, DEFENDANT_LAST_NAME, PROSECUTION_AUTHORITY_REFERENCE,
                DEFENDANT_2_FIRST_NAME, DEFENDANT_2_MIDDLE_NAME, DEFENDANT_2_LAST_NAME, PROSECUTION_AUTHORITY_REFERENCE_2,
                DEFENDANT_3_FIRST_NAME, DEFENDANT_3_MIDDLE_NAME, DEFENDANT_3_LAST_NAME, PROSECUTION_AUTHORITY_REFERENCE_3)));
        assertThat(additionalProperties.get(PROPERTY_COURT_LOCATION), is(COURT_NAME));
        assertThat(additionalProperties.get(PROPERTY_HEARING_DATE), is("27 Oct 2013"));
        assertThat(additionalProperties.get(PROPERTY_HEARING_TIME), is(HEARING_TIME));

        assertThat(result.getReplyToAddress(), is(nullValue()));
        assertThat(result.getReplyToAddressId(), is(nullValue()));
        verify(applicationParameters).getSummonsApprovedAndNotSuppressedTemplateId();
        verifyNoInteractions(materialUrlGenerator);
    }

    @Test
    public void shouldBuildEmailNotificationForEachDefendantForCaseSummonsWhenSuppressedAndCaseHasMultipleDefendants() {
        final SummonsDataPrepared summonsDataPrepared = getSummonsDataPreparedForCaseWithMultipleDefendants();
        final List<UUID> defendantIds = ImmutableList.of(DEFENDANT_ID, DEFENDANT_ID_2, DEFENDANT_ID_3);
        final boolean sendForRemotePrinting = false;
        SummonsDocumentContent summonsDocumentContent = getCaseSummonsDocumentContentForDefendant();
        Defendant defendant = getDefendant(DEFENDANT_ID, PROSECUTION_AUTHORITY_REFERENCE);
        final List<String> defendantDetails = newArrayList();

        when(applicationParameters.getSummonsApprovedAndSuppressedTemplateId()).thenReturn(SUMMONS_APPROVED_AND_SUPPRESSED_TEMPLATE_ID);
        when(materialUrlGenerator.pdfFileStreamUrlFor(MATERIAL_ID)).thenReturn(MATERIAL_URL);

        //email notification generated for first defendant
        Optional<EmailChannel> optionalResult = summonsNotificationEmailPayloadService.getEmailChannelForCaseDefendant(
                summonsDataPrepared, summonsDocumentContent, EMAIL_ADDRESS, defendantIds, defendant, defendantDetails, sendForRemotePrinting,
                DEFENDANT_IS_YOUTH, MATERIAL_ID, SUMMONS_REQUIRED);

        assertThat(optionalResult.isPresent(), is(true));
        assertThat(defendantDetails, hasSize(0));
        Map<String, Object> additionalProperties = optionalResult.get().getPersonalisation().getAdditionalProperties();
        assertThat(additionalProperties.get(PROPERTY_DEFENDANT_DETAILS), is(format("%s %s %s, %s", DEFENDANT_FIRST_NAME, DEFENDANT_MIDDLE_NAME, DEFENDANT_LAST_NAME, PROSECUTION_AUTHORITY_REFERENCE)));

        summonsDocumentContent = getCaseSummonsDocumentContentForDefendant2();
        defendant = getDefendant(DEFENDANT_ID_2, PROSECUTION_AUTHORITY_REFERENCE_2);

        //email notification generated for second defendant
        optionalResult = summonsNotificationEmailPayloadService.getEmailChannelForCaseDefendant(
                summonsDataPrepared, summonsDocumentContent, EMAIL_ADDRESS, defendantIds, defendant, defendantDetails, sendForRemotePrinting,
                DEFENDANT_IS_YOUTH, MATERIAL_ID, SUMMONS_REQUIRED);

        assertThat(optionalResult.isPresent(), is(true));
        assertThat(defendantDetails, hasSize(0));
        additionalProperties = optionalResult.get().getPersonalisation().getAdditionalProperties();
        assertThat(additionalProperties.get(PROPERTY_DEFENDANT_DETAILS), is(format("%s %s %s, %s", DEFENDANT_2_FIRST_NAME, DEFENDANT_2_MIDDLE_NAME, DEFENDANT_2_LAST_NAME, PROSECUTION_AUTHORITY_REFERENCE_2)));

        summonsDocumentContent = getCaseSummonsDocumentContentForDefendant3();
        defendant = getDefendant(DEFENDANT_ID_3, PROSECUTION_AUTHORITY_REFERENCE_3);

        //email notification generated for third defendant
        optionalResult = summonsNotificationEmailPayloadService.getEmailChannelForCaseDefendant(
                summonsDataPrepared, summonsDocumentContent, EMAIL_ADDRESS, defendantIds, defendant, defendantDetails, sendForRemotePrinting,
                DEFENDANT_IS_YOUTH, MATERIAL_ID, SUMMONS_REQUIRED);

        assertThat(optionalResult.isPresent(), is(true));
        assertThat(defendantDetails, hasSize(0));
        additionalProperties = optionalResult.get().getPersonalisation().getAdditionalProperties();
        assertThat(additionalProperties.get(PROPERTY_DEFENDANT_DETAILS), is(format("%s %s %s, %s", DEFENDANT_3_FIRST_NAME, DEFENDANT_3_MIDDLE_NAME, DEFENDANT_3_LAST_NAME, PROSECUTION_AUTHORITY_REFERENCE_3)));

        verify(applicationParameters, times(3)).getSummonsApprovedAndSuppressedTemplateId();
        verify(materialUrlGenerator, times(3)).pdfFileStreamUrlFor(eq(MATERIAL_ID));
    }

    @Test
    public void shouldBuildEmailNotificationForSummonsWhenRejected() {
        final String applicationReference = randomAlphanumeric(8);
        final List<String> rejectionReasons = newArrayList(randomAlphabetic(20), randomAlphabetic(20));
        final List<String> defendantDetails = ImmutableList.of(
                format("%s %s %s, %s", DEFENDANT_FIRST_NAME, DEFENDANT_MIDDLE_NAME, DEFENDANT_LAST_NAME, PROSECUTION_AUTHORITY_REFERENCE),
                format("%s %s %s, %s", DEFENDANT_2_FIRST_NAME, DEFENDANT_2_MIDDLE_NAME, DEFENDANT_2_LAST_NAME, PROSECUTION_AUTHORITY_REFERENCE_2)
        );

        when(applicationParameters.getSummonsRejectedTemplateId()).thenReturn(SUMMONS_REJECTED_TEMPLATE_ID);

        final EmailChannel result = summonsNotificationEmailPayloadService.getEmailChannelForSummonsRejected(EMAIL_ADDRESS, applicationReference, defendantDetails, rejectionReasons);

        assertThat(result.getTemplateId(), is(fromString(SUMMONS_REJECTED_TEMPLATE_ID)));
        assertThat(result.getMaterialUrl(), is(nullValue()));
        assertThat(result.getSendToAddress(), is(EMAIL_ADDRESS));
        assertThat(result.getPersonalisation(), is(notNullValue()));
        final Map<String, Object> additionalProperties = result.getPersonalisation().getAdditionalProperties();
        assertThat(additionalProperties, is(notNullValue()));
        assertThat(additionalProperties.entrySet(), hasSize(3));
        assertThat(additionalProperties.get(PROPERTY_CASE_REFERENCE), is(applicationReference));
        assertThat(additionalProperties.get(PROPERTY_DEFENDANT_DETAILS), is(
                format("%s %s %s, %s" + LINE_SEPARATOR + "%s %s %s, %s", DEFENDANT_FIRST_NAME, DEFENDANT_MIDDLE_NAME, DEFENDANT_LAST_NAME, PROSECUTION_AUTHORITY_REFERENCE,
                        DEFENDANT_2_FIRST_NAME, DEFENDANT_2_MIDDLE_NAME, DEFENDANT_2_LAST_NAME, PROSECUTION_AUTHORITY_REFERENCE_2)));
        assertThat(additionalProperties.get(PROPERTY_REJECTED_REASONS), is(rejectionReasons.stream().collect(joining(lineSeparator()))));

        assertThat(result.getReplyToAddress(), is(nullValue()));
        assertThat(result.getReplyToAddressId(), is(nullValue()));
        verify(applicationParameters).getSummonsRejectedTemplateId();
        verifyNoInteractions(materialUrlGenerator);
    }

    private SummonsDataPrepared getSummonsDataPreparedForCase() {
        final SummonsData summonsData = summonsData()
                .withConfirmedProsecutionCaseIds(newArrayList(confirmedProsecutionCaseId()
                        .withId(CASE_ID)
                        .withConfirmedDefendantIds(singletonList(DEFENDANT_ID))
                        .build()))
                .withCourtCentre(courtCentre()
                        .withId(COURT_CENTRE_ID)
                        .withRoomId(COURT_ROOM_ID)
                        .build())
                .withHearingDateTime(HEARING_DATE_TIME)
                .withListDefendantRequests(newArrayList(listDefendantRequest()
                        .withSummonsRequired(SUMMONS_REQUIRED)
                        .withProsecutionCaseId(CASE_ID)
                        .withDefendantId(DEFENDANT_ID)
                        .withSummonsApprovedOutcome(summonsApprovedOutcome()
                                .withProsecutorCost("£300.00")
                                .withPersonalService(true)
                                .withSummonsSuppressed(SUMMONS_SUPPRESSED)
                                .withProsecutorEmailAddress("test@test.com")
                                .build())
                        .build()))
                .build();

        return summonsDataPrepared().withSummonsData(summonsData).build();
    }

    private SummonsDataPrepared getSummonsDataPreparedForCaseWithMultipleDefendants() {
        final SummonsData summonsData = summonsData()
                .withConfirmedProsecutionCaseIds(newArrayList(confirmedProsecutionCaseId()
                        .withId(CASE_ID)
                        .withConfirmedDefendantIds(ImmutableList.of(DEFENDANT_ID, DEFENDANT_ID_2, DEFENDANT_ID_3))
                        .build()))
                .withCourtCentre(courtCentre()
                        .withId(COURT_CENTRE_ID)
                        .withRoomId(COURT_ROOM_ID)
                        .build())
                .withHearingDateTime(HEARING_DATE_TIME)
                .withListDefendantRequests(newArrayList(
                        listDefendantRequest()
                                .withSummonsRequired(SUMMONS_REQUIRED)
                                .withProsecutionCaseId(CASE_ID)
                                .withDefendantId(DEFENDANT_ID)
                                .withSummonsApprovedOutcome(summonsApprovedOutcome()
                                        .withProsecutorCost("£300.00")
                                        .withPersonalService(true)
                                        .withSummonsSuppressed(SUMMONS_SUPPRESSED)
                                        .withProsecutorEmailAddress("test@test.com")
                                        .build())
                                .build(),
                        listDefendantRequest()
                                .withSummonsRequired(SUMMONS_REQUIRED)
                                .withProsecutionCaseId(CASE_ID)
                                .withDefendantId(DEFENDANT_ID_2)
                                .withSummonsApprovedOutcome(summonsApprovedOutcome()
                                        .withProsecutorCost("£300.00")
                                        .withPersonalService(true)
                                        .withSummonsSuppressed(SUMMONS_SUPPRESSED)
                                        .withProsecutorEmailAddress("test@test.com")
                                        .build())
                                .build(),
                        listDefendantRequest()
                                .withSummonsRequired(SUMMONS_REQUIRED)
                                .withProsecutionCaseId(CASE_ID)
                                .withDefendantId(DEFENDANT_ID_3)
                                .withSummonsApprovedOutcome(summonsApprovedOutcome()
                                        .withProsecutorCost("£300.00")
                                        .withPersonalService(true)
                                        .withSummonsSuppressed(SUMMONS_SUPPRESSED)
                                        .withProsecutorEmailAddress("test@test.com")
                                        .build())
                                .build()
                ))
                .build();

        return summonsDataPrepared().withSummonsData(summonsData).build();
    }

    private SummonsDataPrepared getSummonsDataPreparedForApplication() {

        final SummonsData summonsData = summonsData()
                .withConfirmedApplicationIds(newArrayList(APPLICATION_ID))
                .withCourtCentre(courtCentre()
                        .withId(COURT_CENTRE_ID)
                        .withRoomId(COURT_ROOM_ID)
                        .build())
                .withHearingDateTime(HEARING_DATE_TIME)
                .withCourtApplicationPartyListingNeeds(newArrayList(CourtApplicationPartyListingNeeds.courtApplicationPartyListingNeeds()
                        .withSummonsRequired(SUMMONS_REQUIRED)
                        .withCourtApplicationId(APPLICATION_ID)
                        .withCourtApplicationPartyId(SUBJECT_ID)
                        .withSummonsApprovedOutcome(summonsApprovedOutcome()
                                .withProsecutorCost("£300.00")
                                .withPersonalService(true)
                                .withSummonsSuppressed(SUMMONS_SUPPRESSED)
                                .withProsecutorEmailAddress("test@test.com")
                                .build())
                        .build()))
                .build();

        return summonsDataPrepared().withSummonsData(summonsData).build();
    }

    private SummonsProsecutor getSummonsProsecutorWithoutEmailAddress() {
        return summonsProsecutor()
                .withName(PROSECUTOR_NAME)
                .withEmailAddress(null)
                .build();
    }

    private SummonsProsecutor getSummonsProsecutorWithEmptyEmailAddress() {
        return summonsProsecutor()
                .withName(PROSECUTOR_NAME)
                .withEmailAddress(" ")
                .build();
    }

    private SummonsDocumentContent getCaseSummonsDocumentContentForDefendant() {
        return summonsDocumentContent()
                .withCaseReference(CASE_URN)
                .withDefendant(summonsDefendant().withName(format("%s %s %s", DEFENDANT_FIRST_NAME, DEFENDANT_MIDDLE_NAME, DEFENDANT_LAST_NAME)).build())
                .withAddressee(summonsAddressee().withName(format("%s %s %s", DEFENDANT_FIRST_NAME, DEFENDANT_MIDDLE_NAME, DEFENDANT_LAST_NAME)).build())
                .withHearingCourtDetails(summonsHearingCourtDetails().withCourtName(COURT_NAME).withHearingTime(HEARING_TIME).build())
                .build();
    }

    private SummonsDocumentContent getCaseSummonsDocumentContentForDefendantParent() {
        return summonsDocumentContent()
                .withCaseReference(CASE_URN)
                .withDefendant(summonsDefendant().withName(format("%s %s %s", DEFENDANT_FIRST_NAME, DEFENDANT_MIDDLE_NAME, DEFENDANT_LAST_NAME)).build())
                .withAddressee(summonsAddressee().withName(format("%s %s %s", PARENT_FIRST_NAME, PARENT_MIDDLE_NAME, PARENT_LAST_NAME)).build())
                .withHearingCourtDetails(summonsHearingCourtDetails().withCourtName(COURT_NAME).withHearingTime(HEARING_TIME).build())
                .build();
    }

    private SummonsDocumentContent getCaseSummonsDocumentContentForDefendant2() {
        return summonsDocumentContent()
                .withCaseReference(CASE_URN)
                .withDefendant(summonsDefendant().withName(format("%s %s %s", DEFENDANT_2_FIRST_NAME, DEFENDANT_2_MIDDLE_NAME, DEFENDANT_2_LAST_NAME)).build())
                .withAddressee(summonsAddressee().withName(format("%s %s %s", DEFENDANT_2_FIRST_NAME, DEFENDANT_2_MIDDLE_NAME, DEFENDANT_2_LAST_NAME)).build())
                .withHearingCourtDetails(summonsHearingCourtDetails().withCourtName(COURT_NAME).withHearingTime(HEARING_TIME).build())
                .build();
    }

    private SummonsDocumentContent getCaseSummonsDocumentContentForDefendant3() {
        return summonsDocumentContent()
                .withCaseReference(CASE_URN)
                .withDefendant(summonsDefendant().withName(format("%s %s %s", DEFENDANT_3_FIRST_NAME, DEFENDANT_3_MIDDLE_NAME, DEFENDANT_3_LAST_NAME)).build())
                .withAddressee(summonsAddressee().withName(format("%s %s %s", DEFENDANT_3_FIRST_NAME, DEFENDANT_3_MIDDLE_NAME, DEFENDANT_3_LAST_NAME)).build())
                .withHearingCourtDetails(summonsHearingCourtDetails().withCourtName(COURT_NAME).withHearingTime(HEARING_TIME).build())
                .build();
    }

    private SummonsDocumentContent getApplicationSummonsDocumentContentForAddressee() {
        return summonsDocumentContent()
                .withCaseReference(APPLICATION_REFERENCE_NUMBER)
                .withDefendant(summonsDefendant().withName(format("%s %s %s", ADDRESSEE_FIRST_NAME, ADDRESSEE_MIDDLE_NAME, ADDRESSEE_LAST_NAME)).build())
                .withAddressee(summonsAddressee().withName(format("%s %s %s", ADDRESSEE_FIRST_NAME, ADDRESSEE_MIDDLE_NAME, ADDRESSEE_LAST_NAME)).build())
                .withHearingCourtDetails(summonsHearingCourtDetails().withCourtName(COURT_NAME).withHearingTime(HEARING_TIME).build())
                .build();
    }

    private SummonsDocumentContent getApplicationSummonsDocumentContentForAddresseeParent() {
        return summonsDocumentContent()
                .withCaseReference(APPLICATION_REFERENCE_NUMBER)
                .withDefendant(summonsDefendant().withName(format("%s %s %s", ADDRESSEE_FIRST_NAME, ADDRESSEE_MIDDLE_NAME, ADDRESSEE_LAST_NAME)).build())
                .withAddressee(summonsAddressee().withName(format("%s %s %s", PARENT_FIRST_NAME, PARENT_MIDDLE_NAME, PARENT_LAST_NAME)).build())
                .withHearingCourtDetails(summonsHearingCourtDetails().withCourtName(COURT_NAME).withHearingTime(HEARING_TIME).build())
                .build();
    }

    private Defendant getDefendant(final UUID id, final String reference) {
        return defendant()
                .withId(id)
                .withProsecutionAuthorityReference(reference)
                .build();
    }
}
