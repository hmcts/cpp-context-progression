package uk.gov.moj.cpp.progression.service;

import static java.lang.String.format;
import static javax.json.Json.createArrayBuilder;
import static javax.json.Json.createObjectBuilder;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import uk.gov.justice.core.courts.Personalisation;
import uk.gov.justice.core.courts.notification.EmailChannel;
import uk.gov.justice.services.common.converter.jackson.ObjectMapperProducer;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.test.utils.core.messaging.MetadataBuilderFactory;
import uk.gov.moj.cpp.progression.service.payloads.CaseDefendantsOrganisations;
import uk.gov.moj.cpp.progression.service.payloads.CaseDefendantsWithOrganisation;
import uk.gov.moj.cpp.progression.service.payloads.Defendants;

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@SuppressWarnings({"squid:S1607"})
public class EmailServiceTest {
    private static final UUID TEMPLATE_ID = UUID.randomUUID();
    private static final UUID CASE_ID = UUID.randomUUID();
    private static final String URN_VALUE = "urn-123456";
    private static final UUID NOTIFICATION_ID = UUID.randomUUID();
    private static final UUID MATERIAL_ID = UUID.fromString("5e1cc18c-76dc-47dd-99c1-d6f87385edf1");
    private static final String URN = "URN";
    private static final String MATERIAL_SECTIONS_URL = "MATERIAL_SECTIONS_URL";
    private static final String URI_TO_MATERIAL = "defence/case/materials/%s/%s/defending?advocate=true";
    private static final String DEFENDANT_PATH_PARAM = "&defendantId=";

    /*
     * Defendant - AssociatedOrganisation
     * ----------------------------------
     * DEFENDANT1 - ORG1
     * DEFENDANT2 - ORG1
     * DEFENDANT3 - ORG2 (not exists in defendantDocument)
     * DEFENDANT4 - null
     * */
    private static final UUID ORG1 = UUID.randomUUID();
    private static final UUID ORG2 = UUID.randomUUID();
    private static final UUID DEFENDANT1 = UUID.randomUUID();
    private static final UUID DEFENDANT2 = UUID.randomUUID();
    private static final UUID DEFENDANT3 = UUID.randomUUID();
    private static final UUID DEFENDANT4 = UUID.randomUUID();
    public static final String DEFENDANT_LIST = "defendant_list";
    public static final String DOCUMENT_TITLE = "DOCUMENT_TITLE";
    public static final String DOCUMENT_SECTION = "DOCUMENT_SECTION";

    @Mock
    private ApplicationParameters applicationParameters;

    @Mock
    private NotificationService notificationService;

    @Mock
    private RefDataService referenceDataService;

    @Mock
    private DefenceService defenceService;

    @InjectMocks
    private EmailService emailService;

    @Captor
    private ArgumentCaptor<JsonEnvelope> sourceEnvelopeCaptor;

    @Captor
    private ArgumentCaptor<UUID> applicationIdCaptor;

    @Captor
    private ArgumentCaptor<UUID> caseIdCaptor;

    @Captor
    private ArgumentCaptor<UUID> materialIdCaptor;

    @Captor
    private ArgumentCaptor<List<EmailChannel>> emailNotificationsCaptor;

    @BeforeEach
    public void setUp() {
    }

    @Test
    public void shouldSendEmailsForDefendantDocument() {
        final ObjectMapper mapper = new ObjectMapperProducer().objectMapper();
        final JsonObject courtDocumentPayload = buildDocumentCategoryWithDefendantDocument("0bb7b276-9dc0-4af2-83b9-f4acef0c7898");
        final JsonEnvelope requestMessage = JsonEnvelope.envelopeFrom(
                MetadataBuilderFactory.metadataWithRandomUUID("progression.event.court-document-added"),
                courtDocumentPayload);

        final Map<Defendants, String> defendantAndRelatedOrganisationEmail = new HashMap<>();
        final String emailAddress1 = "email@abc.com";
        final String documentSection = "Case Summary";
        final String documentName = "Smith John 53NP7934321.pdf";
        defendantAndRelatedOrganisationEmail.put(new Defendants(null, DEFENDANT1, "defendantFirstName1", "defendantLastName1", "organisationName"), emailAddress1);
        when(applicationParameters.getNotifyDefenceOfNewMaterialTemplateId()).thenReturn(TEMPLATE_ID.toString());
        when(applicationParameters.getEndClientHost()).thenReturn("EndClientHost");

        final List<EmailChannel> emailChannels = new ArrayList<>();
        emailChannels.add(emailChannel(emailAddress1, personalisation(DEFENDANT1.toString())));

        emailService.sendEmailNotifications(requestMessage, MATERIAL_ID,
                URN_VALUE, CASE_ID, defendantAndRelatedOrganisationEmail, documentSection, documentName);

        verify(notificationService, times(1)).sendEmail(
                sourceEnvelopeCaptor.capture(),
                caseIdCaptor.capture(), applicationIdCaptor.capture(),
                materialIdCaptor.capture(), emailNotificationsCaptor.capture());

        assertThat(caseIdCaptor.getValue(), is(CASE_ID));
        assertThat(materialIdCaptor.getValue(), is(MATERIAL_ID));
        assertThat(applicationIdCaptor.getValue(), Matchers.nullValue());

        assertThat(emailNotificationsCaptor.getValue(), notNullValue());
        assertThat(emailNotificationsCaptor.getValue().size(), is(1));
        assertThat(emailNotificationsCaptor.getValue().get(0).getSendToAddress(), is("email@abc.com"));
        assertThat(emailNotificationsCaptor.getValue().get(0).getMaterialUrl(), nullValue());
        assertThat(emailNotificationsCaptor.getValue().get(0).getPersonalisation().getAdditionalProperties().get(URN), is(URN_VALUE));
        final String expectedURL = "EndClientHost".concat(format(URI_TO_MATERIAL, URN_VALUE, CASE_ID).concat(DEFENDANT_PATH_PARAM).concat(DEFENDANT1.toString()));
        assertThat(emailNotificationsCaptor.getValue().get(0).getPersonalisation().getAdditionalProperties().get(MATERIAL_SECTIONS_URL), is(expectedURL));
    }

    @Test
    public void shouldSendEmailByOrganisationName() {
        final ObjectMapper mapper = new ObjectMapperProducer().objectMapper();
        final JsonObject courtDocumentPayload = buildDocumentCategoryWithCaseDocument("0bb7b276-9dc0-4af2-83b9-f4acef0c7898");
        final JsonEnvelope requestMessage = JsonEnvelope.envelopeFrom(
                MetadataBuilderFactory.metadataWithRandomUUID("progression.event.court-document-added"),
                courtDocumentPayload);
        final String documentSection = "Case Summary";
        final String documentName = "Smith John 53NP7934321.pdf";

        final List<Defendants> defendants = Arrays.asList(
                new Defendants(ORG1, DEFENDANT1, null, null, "Organisation Name Test"),
                new Defendants(ORG1, DEFENDANT2, "defendantFirstName2", "defendantLastName2", "organisationName Test")
        );


        final CaseDefendantsWithOrganisation caseDefendantsWithOrg = CaseDefendantsWithOrganisation.caseDefendantsWithOrganisation()
                .withCaseId(CASE_ID)
                .withUrn("urn-123456")
                .withDefendants(defendants)
                .build();
        final CaseDefendantsOrganisations caseDefendantsOrganisations = CaseDefendantsOrganisations.caseDefendantsOrganisations().withCaseDefendantOrganisation(caseDefendantsWithOrg).build();

        final List<String> emails = Arrays.asList("email1@abc.com", "email2@abc.com");

        final Map<Defendants, String> defendantAndRelatedOrganisationEmail = new HashMap<>();
        defendantAndRelatedOrganisationEmail.put(defendants.get(0), emails.get(0));
        defendantAndRelatedOrganisationEmail.put(defendants.get(1), emails.get(0));
        when(applicationParameters.getEndClientHost()).thenReturn("EndClientHost");
        when(applicationParameters.getNotifyDefenceOfNewMaterialTemplateId()).thenReturn(TEMPLATE_ID.toString());

        emailService.sendEmailNotifications(requestMessage, MATERIAL_ID, URN_VALUE, CASE_ID, defendantAndRelatedOrganisationEmail, documentSection, documentName);
        verify(notificationService, times(1)).sendEmail(
                sourceEnvelopeCaptor.capture(),
                caseIdCaptor.capture(), applicationIdCaptor.capture(),
                materialIdCaptor.capture(), emailNotificationsCaptor.capture());

        final List<EmailChannel> emailChannels = new ArrayList<>();
        final String expectedDefendantList1 = "Organisation Name Test, defendantFirstName2 defendantLastName2";
        final String expectedDefendantList2 = "defendantFirstName2 defendantLastName2";

        emailChannels.add(emailChannel(emails.get(0), personalisation(DEFENDANT1.toString(),expectedDefendantList1,documentName,documentSection)));
        emailChannels.add(emailChannel(emails.get(1), personalisation(DEFENDANT2.toString(),expectedDefendantList2,documentName,documentSection)));

        assertThat(caseIdCaptor.getValue(), is(CASE_ID));
        assertThat(applicationIdCaptor.getValue(), Matchers.nullValue());
        assertThat(materialIdCaptor.getValue(), is(MATERIAL_ID));
        assertThat(emailNotificationsCaptor.getValue(), notNullValue());
        assertThat(emailNotificationsCaptor.getValue().size(), is(2));

        containsInAnyOrder(emailChannels, emailNotificationsCaptor.getValue());
    }

    @Test
    public void shouldSendEmailsForCaseDocument() {
        final ObjectMapper mapper = new ObjectMapperProducer().objectMapper();
        final JsonObject courtDocumentPayload = buildDocumentCategoryWithCaseDocument("0bb7b276-9dc0-4af2-83b9-f4acef0c7898");
        final JsonEnvelope requestMessage = JsonEnvelope.envelopeFrom(
                MetadataBuilderFactory.metadataWithRandomUUID("progression.event.court-document-added"),
                courtDocumentPayload);
        final String documentSection = "Case Summary";
        final String documentName = "Smith John 53NP7934321.pdf";

        final List<Defendants> defendants = Arrays.asList(
                new Defendants(ORG1, DEFENDANT1, "defendantFirstName1", "defendantLastName1", "organisationName"), new Defendants(ORG1, DEFENDANT2, "defendantFirstName2", "defendantLastName2", "organisationName"),
                new Defendants(ORG2, DEFENDANT3, "defendantFirstName3", "defendantLastName3", "organisationName"), new Defendants(null, DEFENDANT4, "defendantFirstName4", "defendantLastName4", "organisationName")
        );


        final CaseDefendantsWithOrganisation caseDefendantsWithOrg = CaseDefendantsWithOrganisation.caseDefendantsWithOrganisation()
                .withCaseId(CASE_ID)
                .withUrn("urn-123456")
                .withDefendants(defendants)
                .build();
        final CaseDefendantsOrganisations caseDefendantsOrganisations = CaseDefendantsOrganisations.caseDefendantsOrganisations().withCaseDefendantOrganisation(caseDefendantsWithOrg).build();

        final List<String> emails = Arrays.asList("email1@abc.com", "email2@abc.com");

        final Map<Defendants, String> defendantAndRelatedOrganisationEmail = new HashMap<>();
        defendantAndRelatedOrganisationEmail.put(defendants.get(0), emails.get(0));
        defendantAndRelatedOrganisationEmail.put(defendants.get(1), emails.get(0));
        defendantAndRelatedOrganisationEmail.put(defendants.get(2), emails.get(1));
        defendantAndRelatedOrganisationEmail.put(defendants.get(2), emails.get(1));
        when(applicationParameters.getEndClientHost()).thenReturn("EndClientHost");
        when(applicationParameters.getNotifyDefenceOfNewMaterialTemplateId()).thenReturn(TEMPLATE_ID.toString());

        emailService.sendEmailNotifications(requestMessage, MATERIAL_ID, URN_VALUE, CASE_ID, defendantAndRelatedOrganisationEmail, documentSection, documentName);
        verify(notificationService, times(1)).sendEmail(
                sourceEnvelopeCaptor.capture(),
                caseIdCaptor.capture(), applicationIdCaptor.capture(),
                materialIdCaptor.capture(), emailNotificationsCaptor.capture());

        final List<EmailChannel> emailChannels = new ArrayList<>();
        emailChannels.add(emailChannel(emails.get(0), personalisation(DEFENDANT1.toString())));
        emailChannels.add(emailChannel(emails.get(1), personalisation(DEFENDANT2.toString())));

        assertThat(caseIdCaptor.getValue(), is(CASE_ID));
        assertThat(applicationIdCaptor.getValue(), Matchers.nullValue());
        assertThat(materialIdCaptor.getValue(), is(MATERIAL_ID));
        assertThat(emailNotificationsCaptor.getValue(), notNullValue());
        assertThat(emailNotificationsCaptor.getValue().size(), is(3));

        containsInAnyOrder(emailChannels, emailNotificationsCaptor.getValue());

    }

    @Test
    public void shouldSkipNotificationsWhenNoDefendants() {
        final String documentSection = "Case Summary";
        final String documentName = "Smith John 53NP7934321.pdf";
        final ObjectMapper mapper = new ObjectMapperProducer().objectMapper();
        final JsonObject courtDocumentPayload = buildDocumentCategoryWith2Documents("0bb7b276-9dc0-4af2-83b9-f4acef0c7898");
        final JsonEnvelope requestMessage = JsonEnvelope.envelopeFrom(
                MetadataBuilderFactory.metadataWithRandomUUID("progression.event.court-document-added"),
                courtDocumentPayload);
        final List<String> emails = Arrays.asList("email1@abc.com");

        final List<EmailChannel> emailChannels = new ArrayList<>();
        emailChannels.add(emailChannel(emails.get(0), personalisation(DEFENDANT1.toString())));
        final Map<Defendants, String> defendantAndRelatedOrganisationEmail = new HashMap<>();


        emailService.sendEmailNotifications(requestMessage, MATERIAL_ID, URN_VALUE, CASE_ID, defendantAndRelatedOrganisationEmail, documentSection, documentName);
        verify(notificationService, never()).sendEmail(any(), any(), any(), any(), any());
    }

    private EmailChannel emailChannel(final String emailAddress1,
                                      final Personalisation personalisation1) {
        final EmailChannel emailChannel = EmailChannel.emailChannel()
                .withPersonalisation(personalisation1)
                .withSendToAddress(emailAddress1)
                .withTemplateId(TEMPLATE_ID)
                .build();
        return emailChannel;
    }

    private Personalisation personalisation(final String defendantId) {
        final String materialLink = format(URI_TO_MATERIAL, CASE_ID, MATERIAL_ID);
        final String url = materialLink.concat(DEFENDANT_PATH_PARAM).concat(defendantId);
        return Personalisation.personalisation()
                .withAdditionalProperty(URN, URN_VALUE)
                .withAdditionalProperty(MATERIAL_SECTIONS_URL, url)
                .build();
    }

    private Personalisation personalisation(final String defendantId,final String defendantList,final String  documentName, final String documentSection) {
        final String materialLink = format(URI_TO_MATERIAL, CASE_ID, MATERIAL_ID);
        final String url = materialLink.concat(DEFENDANT_PATH_PARAM).concat(defendantId);
        return Personalisation.personalisation()
                .withAdditionalProperty(URN, URN_VALUE)
                .withAdditionalProperty(MATERIAL_SECTIONS_URL, url)
                .withAdditionalProperty(DEFENDANT_LIST, defendantList)
                .withAdditionalProperty(DOCUMENT_TITLE, documentName)
                .withAdditionalProperty(DOCUMENT_SECTION, documentSection)
                .build();
    }

    private static JsonObject buildDefendantDocument() {
        return createObjectBuilder().add("defendantDocument",
                createObjectBuilder()
                        .add("prosecutionCaseId", CASE_ID.toString())
                        .add("defendants", createArrayBuilder()
                                .add(DEFENDANT1.toString())
                                .add(DEFENDANT2.toString())
                                .add(DEFENDANT4.toString())))
                .build();
    }

    private static JsonObject buildCaseDocument() {
        return createObjectBuilder().add("caseDocument",
                createObjectBuilder()
                        .add("prosecutionCaseId", CASE_ID.toString()))
                .build();
    }

    private static JsonObject buildCompositeDocument() {
        return createObjectBuilder()
                .add("defendantDocument",
                        createObjectBuilder()
                                .add("prosecutionCaseId", CASE_ID.toString())
                                .add("defendants", createArrayBuilder()
                                        .add(DEFENDANT1.toString())
                                        .add(DEFENDANT2.toString())
                                        .add(DEFENDANT4.toString())))
                .add("caseDocument",
                        createObjectBuilder()
                                .add("prosecutionCaseId", CASE_ID.toString()))
                .build();
    }

    private static JsonObjectBuilder buildCourtDocument() {
        return createObjectBuilder()
                .add("courtDocumentId", "2279b2c3-b0d3-4889-ae8e-1ecc20c39e27")
                .add("name", "SJP Notice")
                .add("documentTypeDescription", "SJP Notice")
                .add("mimeType", "pdf")
                .add("materials", createArrayBuilder().add(buildMaterial()))
                .add("containsFinancialMeans", false)
                .add("documentTypeRBAC", buildDocumentTypeDataWithRBAC());
    }

    private static JsonObject buildDocumentCategoryWithDefendantDocument(String documentTypeId) {
        final JsonObject documentCategory = buildDefendantDocument();

        return createObjectBuilder().add("courtDocument",
                buildCourtDocument()
                        .add("documentCategory", documentCategory)
                        .add("documentTypeId", documentTypeId))
                .build();
    }

    private static JsonObject buildDocumentCategoryWithCaseDocument(String documentTypeId) {
        final JsonObject documentCategory = buildCaseDocument();

        return createObjectBuilder().add("courtDocument",
                buildCourtDocument()
                        .add("documentCategory", documentCategory)
                        .add("documentTypeId", documentTypeId))
                .build();
    }

    private static JsonObject buildDocumentCategoryWith2Documents(String documentTypeId) {
        final JsonObject documentCategory = buildCompositeDocument();

        return createObjectBuilder().add("courtDocument",
                buildCourtDocument()
                        .add("documentCategory", documentCategory)
                        .add("documentTypeId", documentTypeId))
                .build();
    }

    private static JsonObject buildMaterial() {
        return createObjectBuilder().add("id", MATERIAL_ID.toString()).add("receivedDateTime", ZonedDateTime.now().toString()).build();
    }

    private static JsonObject buildDocumentTypeDataWithRBAC() {
        return Json.createObjectBuilder()
                .add("documentAccess", Json.createArrayBuilder().add("Listing Officer"))
                .add("canCreateUserGroups", Json.createArrayBuilder().add("Listing Officer"))
                .add("canReadUserGroups", Json.createArrayBuilder().add("Listing Officer").add("Magistrates"))
                .add("canDownloadUserGroups", Json.createArrayBuilder().add("Listing Officer").add("Magistrates"))
                .build();
    }

}