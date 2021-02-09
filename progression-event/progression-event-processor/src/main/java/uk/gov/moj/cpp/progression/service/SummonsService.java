package uk.gov.moj.cpp.progression.service;

import static java.util.Objects.nonNull;
import static org.apache.commons.lang3.StringUtils.EMPTY;
import static uk.gov.justice.services.core.annotation.Component.EVENT_PROCESSOR;
import static uk.gov.moj.cpp.progression.helper.SummonsDataHelper.getCourtTime;
import static uk.gov.moj.cpp.progression.helper.SummonsDataHelper.getDocumentTypeData;

import uk.gov.justice.core.courts.Address;
import uk.gov.justice.core.courts.ApplicationDocument;
import uk.gov.justice.core.courts.ApplicationSummonsRecipientType;
import uk.gov.justice.core.courts.ApplicationSummonsTemplateType;
import uk.gov.justice.core.courts.ConfirmedHearing;
import uk.gov.justice.core.courts.CourtApplication;
import uk.gov.justice.core.courts.CourtApplicationParty;
import uk.gov.justice.core.courts.CourtApplicationRespondent;
import uk.gov.justice.core.courts.CourtApplicationType;
import uk.gov.justice.core.courts.CourtDocument;
import uk.gov.justice.core.courts.Defendant;
import uk.gov.justice.core.courts.DocumentCategory;
import uk.gov.justice.core.courts.HearingDay;
import uk.gov.justice.core.courts.LegalEntityDefendant;
import uk.gov.justice.core.courts.Material;
import uk.gov.justice.core.courts.Organisation;
import uk.gov.justice.core.courts.Person;
import uk.gov.justice.core.courts.PersonDefendant;
import uk.gov.justice.core.courts.ProsecutingAuthority;
import uk.gov.justice.core.courts.ProsecutionCase;
import uk.gov.justice.core.courts.summons.ApplicationSummonsDocumentContent;
import uk.gov.justice.core.courts.summons.BreachSummonsDocumentContent;
import uk.gov.justice.core.courts.summons.SummonsAddress;
import uk.gov.justice.core.courts.summons.SummonsAddressee;
import uk.gov.justice.core.courts.summons.SummonsDefendant;
import uk.gov.justice.core.courts.summons.SummonsDocumentContent;
import uk.gov.justice.core.courts.summons.SummonsHearingCourtDetails;
import uk.gov.justice.progression.courts.Type;
import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.common.converter.ObjectToJsonObjectConverter;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.core.requester.Requester;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.progression.domain.constant.DateTimeFormats;

import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.inject.Inject;
import javax.json.Json;
import javax.json.JsonObject;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@SuppressWarnings({"squid:S1166", "squid:S3776", "squid:S1188", "squid:S00107", "squid:CallToDeprecatedMethod"})
public class SummonsService {

    private static final Logger LOGGER = LoggerFactory.getLogger(SummonsService.class);

    private static final String SUMMONS = "Summons";
    private static final String SUMMONS_TEMPLATE = "Summons";
    private static final String PROGRESSION_COMMAND_CREATE_COURT_DOCUMENT = "progression.command.create-court-document";
    public static final UUID SUMMONS_DOCUMENT_TYPE_ID = UUID.fromString("460f7ec0-c002-11e8-a355-529269fb1459");
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern(DateTimeFormats.STANDARD.getValue());
    private static final String L3_NAME = "oucodeL3Name";
    private static final String ADDRESS_1 = "address1";
    private static final String ADDRESS_2 = "address2";
    private static final String ADDRESS_3 = "address3";
    private static final String ADDRESS_4 = "address4";
    private static final String ADDRESS_5 = "address5";
    private static final String POSTCODE = "postcode";

    @Inject
    private Enveloper enveloper;

    @Inject
    private ObjectToJsonObjectConverter objectToJsonConverter;

    @Inject
    private JsonObjectToObjectConverter jsonToObjectConverter;

    @Inject
    private ReferenceDataService referenceDataService;

    @Inject
    @ServiceComponent(EVENT_PROCESSOR)
    private Requester requester;

    @Inject
    private ProgressionService progressionService;

    @Inject
    @ServiceComponent(EVENT_PROCESSOR)
    private Sender sender;

    @Inject
    private DocumentGeneratorService documentGeneratorService;

    private static ZonedDateTime getEarliestDate(final List<HearingDay> hearingDays) {
        return hearingDays.stream()
                .map(HearingDay::getSittingDay)
                .sorted()
                .findFirst()
                .orElseThrow(IllegalArgumentException::new);
    }

    private static String getCourtDate(final ZonedDateTime hearingDateTime) {
        return hearingDateTime.toLocalDate().toString();
    }

    private static SummonsAddressee populateAddresse(final CourtApplicationParty partyDetails) {
        final Defendant defendant = partyDetails.getDefendant();
        if (nonNull(defendant)) {
            final PersonDefendant personDefendant = defendant.getPersonDefendant();
            if (nonNull(personDefendant)) {
                final Person person = personDefendant.getPersonDetails();
                return SummonsAddressee.summonsAddressee()
                        .withName(getFullName(person))
                        .withAddress(getSummonsAddress(person.getAddress()))
                        .build();
            }

            final LegalEntityDefendant legalEntityDefendant = defendant.getLegalEntityDefendant();
            if (nonNull(legalEntityDefendant)) {
                return SummonsAddressee.summonsAddressee()
                        .withName(legalEntityDefendant.getOrganisation().getName())
                        .withAddress(getSummonsAddress(legalEntityDefendant.getOrganisation().getAddress()))
                        .build();
            }

            return null;
        }
        final Person person = partyDetails.getPersonDetails();
        if (nonNull(person)) {
            return SummonsAddressee.summonsAddressee()
                    .withName(getFullName(person))
                    .withAddress(getSummonsAddress(person.getAddress()))
                    .build();
        }
        final ProsecutingAuthority prosecutingAuthority = partyDetails.getProsecutingAuthority();
        if (nonNull(prosecutingAuthority)) {
            return SummonsAddressee.summonsAddressee()
                    .withName(prosecutingAuthority.getProsecutionAuthorityCode())
                    .withAddress(getSummonsAddress(prosecutingAuthority.getAddress()))
                    .build();
        }
        final Organisation organisation = partyDetails.getOrganisation();
        if (nonNull(organisation)) {
            return SummonsAddressee.summonsAddressee()
                    .withName(organisation.getName())
                    .withAddress(getSummonsAddress(organisation.getAddress()))
                    .build();
        }
        return null;
    }

    private static String getFullName(final Person person) {
        return Stream.of(person.getFirstName(), person.getMiddleName(), person.getLastName())
                .filter(StringUtils::isNotEmpty)
                .collect(Collectors.joining(" "));
    }

    private static SummonsAddress getSummonsAddress(final Address address) {
        if (nonNull(address)) {
            return SummonsAddress.summonsAddress()
                    .withLine1(address.getAddress1())
                    .withLine2(address.getAddress2())
                    .withLine3(address.getAddress3())
                    .withLine4(address.getAddress4())
                    .withLine5(address.getAddress5())
                    .withPostCode(address.getPostcode())
                    .build();
        }
        return null;
    }

    private static boolean validTemplateType(final ApplicationSummonsTemplateType summonsTemplateType) {
        try {
            return nonNull(summonsTemplateType) &&
                    Arrays.asList(ApplicationSummonsTemplateType.values()).contains(summonsTemplateType);
        } catch (final IllegalArgumentException e) {
            return false;
        }
    }

    private static boolean respondentsPresent(final CourtApplication courtApplication) {
        return nonNull(courtApplication.getRespondents()) && courtApplication.getType().getApplicationSummonsRecipientType() == ApplicationSummonsRecipientType.RESPONDENT;
    }

    public void generateSummonsPayload(final JsonEnvelope jsonEnvelope, final ConfirmedHearing confirmedHearing) {
        LOGGER.info("generate summons payload for hearing id : {}", confirmedHearing.getId());

        final List<UUID> applicationIds = confirmedHearing.getCourtApplicationIds();

        if (CollectionUtils.isNotEmpty(applicationIds)) {
            applicationIds.forEach(applicationId -> {
                final JsonObject courtCentreJson = getCourtCentreFromReferenceData(jsonEnvelope, confirmedHearing.getCourtCentre().getId());
                final JsonObject applicationJson = getCourtApplicationFromQuery(jsonEnvelope, applicationId.toString());
                final JsonObject ljaDetails = referenceDataService.getEnforcementAreaByLjaCode(jsonEnvelope, courtCentreJson.getString("lja"), requester);

                final CourtApplication courtApplication = jsonToObjectConverter.convert(applicationJson, CourtApplication.class);
                final ZonedDateTime earliestDate = getEarliestDate(confirmedHearing.getHearingDays());
                final String caseReference = getCaseReferenceNumber(jsonEnvelope, courtApplication);
                final CourtApplicationType courtApplicationType = courtApplication.getType();

                if (validTemplateType(courtApplicationType.getApplicationSummonsTemplateType()) && respondentsPresent(courtApplication)) {
                    LOGGER.info("Using summons template type: {} for application summons generation", courtApplicationType.getApplicationSummonsTemplateType());

                    if (courtApplicationType.getApplicationSummonsTemplateType() == ApplicationSummonsTemplateType.GENERIC_APPLICATION) {

                        generateGenericApplicationSummons(jsonEnvelope, applicationId, courtCentreJson, courtApplication, earliestDate, caseReference, ljaDetails);
                    }
                    if (courtApplicationType.getApplicationSummonsTemplateType() == ApplicationSummonsTemplateType.BREACH) {

                        generateBreachApplicationSummons(jsonEnvelope, applicationId, courtCentreJson, courtApplication, earliestDate, caseReference, ljaDetails);
                    }
                } else {
                    LOGGER.info("No application summons generated, unsupported summons template type : {}", courtApplicationType.getApplicationSummonsTemplateType());
                }
            });
        }
    }

    private void generateBreachApplicationSummons(final JsonEnvelope jsonEnvelope, final UUID applicationId, final JsonObject courtCentreJson, final CourtApplication courtApplication, final ZonedDateTime earliestDate, final String caseReference, final JsonObject localJusticeArea) {
        if (courtApplication.getBreachedOrderDate() != null) {
            for (final CourtApplicationRespondent respondent : courtApplication.getRespondents()) {
                final BreachSummonsDocumentContent breachSummonsDocumentContent = BreachSummonsDocumentContent.breachSummonsDocumentContent()
                        .withBreachedOrder(courtApplication.getBreachedOrder())
                        .withBreachedOrderDate(courtApplication.getBreachedOrderDate())
                        .withOrderingCourt(courtApplication.getOrderingCourt() != null ? courtApplication.getOrderingCourt().getName() : EMPTY)
                        .build();
                final SummonsDocumentContent summonsDocumentContent = generateSummonsDocumentContent(Type.BREACH, courtCentreJson, courtApplication, earliestDate, caseReference, respondent, localJusticeArea, breachSummonsDocumentContent);
                generateSummonsCourtDocument(jsonEnvelope, courtApplication, summonsDocumentContent, applicationId);
            }
        }
    }

    private void generateGenericApplicationSummons(final JsonEnvelope jsonEnvelope, final UUID applicationId, final JsonObject courtCentreJson, final CourtApplication courtApplication, final ZonedDateTime earliestDate, final String caseReference, final JsonObject localJusticeArea) {
        for (final CourtApplicationRespondent respondent : courtApplication.getRespondents()) {
            final SummonsDocumentContent summonsDocumentContent = generateSummonsDocumentContent(Type.APPLICATION, courtCentreJson, courtApplication, earliestDate, caseReference, respondent, localJusticeArea, null);
            generateSummonsCourtDocument(jsonEnvelope, courtApplication, summonsDocumentContent, applicationId);
        }
    }

    private void generateSummonsCourtDocument(final JsonEnvelope jsonEnvelope, final CourtApplication courtApplication, final SummonsDocumentContent summonsDocumentContent, final UUID applicationId) {
        final JsonObject summonsPayload = objectToJsonConverter.convert(summonsDocumentContent);
        LOGGER.info("summons generated for application id: {} with payload: {}", applicationId, summonsPayload);
        final UUID materialId = documentGeneratorService.generateDocument(jsonEnvelope, summonsPayload, SUMMONS_TEMPLATE, sender, null, courtApplication.getId());
        final CourtDocument courtDocument = createApplicationDocument(courtApplication, materialId, jsonEnvelope);
        final JsonObject jsonObject = Json.createObjectBuilder()
                .add("courtDocument", objectToJsonConverter.convert(courtDocument))
                .build();
        sender.send(enveloper.withMetadataFrom(jsonEnvelope, PROGRESSION_COMMAND_CREATE_COURT_DOCUMENT).apply(jsonObject));
    }

    private SummonsDocumentContent generateSummonsDocumentContent(final Type summonsType, final JsonObject courtCentreJson, final CourtApplication courtApplication, final ZonedDateTime earliestDate, final String caseReference, final CourtApplicationRespondent respondent, final JsonObject localJusticeArea, final BreachSummonsDocumentContent breachSummonsDocumentContent) {

        final CourtApplicationParty courtApplicationParty = Optional.ofNullable(respondent)
                .map(CourtApplicationRespondent::getPartyDetails)
                .orElse(null);

        final SummonsDefendant summonsDefendant = Optional.ofNullable(courtApplicationParty)
                .map(courtApplicationParty1 -> populateDefendant(courtApplicationParty1.getDefendant()))
                .orElse(null);

        final SummonsAddressee summonsAddressee = Optional.ofNullable(courtApplicationParty)
                .map(SummonsService::populateAddresse)
                .orElse(null);

        final JsonObject ljaDetails = localJusticeArea.getJsonObject("localJusticeArea");

        return SummonsDocumentContent.summonsDocumentContent()
                .withSubTemplateName(summonsType.toString())
                .withDefendant(summonsDefendant)
                .withLjaCode(extractValueFromJson(ljaDetails, "nationalCourtCode"))
                .withLjaName(extractValueFromJson(ljaDetails, "name"))
                .withCaseReference(caseReference)
                .withApplicationContent(populateApplicationContent(courtApplication))
                .withCourtCentreName(courtCentreJson.getString(L3_NAME))
                .withBreachContent(breachSummonsDocumentContent)
                .withAddressee(summonsAddressee)
                .withHearingCourtDetails(populateHearingCourtDetails(earliestDate, courtCentreJson))
                .withIssueDate(LocalDate.now())
                .withType(summonsType)
                .build();
    }

    private String extractValueFromJson(final JsonObject object, final String key) {
        return nonNull(object) ? object.getString(key, EMPTY) : EMPTY;
    }

    private String getCaseReferenceNumber(final JsonEnvelope jsonEnvelope, final CourtApplication courtApplication) {
        String caseReference = null;
        if (nonNull(courtApplication.getLinkedCaseId())) {
            final Optional<JsonObject> prosecutionCaseOptional = progressionService.getProsecutionCaseDetailById(jsonEnvelope, courtApplication.getLinkedCaseId().toString());
            final JsonObject prosecutionCaseJson = prosecutionCaseOptional.orElseThrow(() -> new RuntimeException("Prosecution Case not found")).getJsonObject("prosecutionCase");
            final ProsecutionCase prosecutionCase = jsonToObjectConverter.convert(prosecutionCaseJson, ProsecutionCase.class);
            if (nonNull(prosecutionCase.getProsecutionCaseIdentifier().getCaseURN())) {
                caseReference = prosecutionCase.getProsecutionCaseIdentifier().getCaseURN();
            } else {
                caseReference = prosecutionCase.getProsecutionCaseIdentifier().getProsecutionAuthorityReference();
            }
        }
        return caseReference;
    }

    private JsonObject getCourtApplicationFromQuery(final JsonEnvelope jsonEnvelope, final String applicationId) {
        final Optional<JsonObject> applicationJsonOptional = progressionService.getCourtApplicationById(jsonEnvelope, applicationId);
        return applicationJsonOptional.orElseThrow(IllegalArgumentException::new).getJsonObject("courtApplication");
    }

    private JsonObject getCourtCentreFromReferenceData(final JsonEnvelope jsonEnvelope, final UUID courtCentreId) {
        final Optional<JsonObject> courtCentreJsonOptional = referenceDataService.getOrganisationUnitById(courtCentreId, jsonEnvelope, requester);
        return courtCentreJsonOptional.orElseThrow(IllegalArgumentException::new);
    }

    private CourtDocument createApplicationDocument(final CourtApplication courtApplication, final UUID materialId, final JsonEnvelope jsonEnvelope)  {

        final ApplicationDocument applicationDocument = ApplicationDocument.applicationDocument()
                .withApplicationId(courtApplication.getId())
                .withProsecutionCaseId(courtApplication.getLinkedCaseId())
                .build();

        final DocumentCategory documentCategory = DocumentCategory.documentCategory()
                .withApplicationDocument(applicationDocument)
                .build();

        return CourtDocument.courtDocument()
                .withCourtDocumentId(UUID.randomUUID())
                .withDocumentTypeId(SUMMONS_DOCUMENT_TYPE_ID)
                .withDocumentTypeDescription(getDocumentTypeData(SUMMONS_DOCUMENT_TYPE_ID, referenceDataService, jsonEnvelope, requester).getString("section"))
                .withMaterials(Collections.singletonList(Material.material()
                                .withId(materialId)
                                .withGenerationStatus(null)
                                .withUploadDateTime(ZonedDateTime.now())
                                .withName(SUMMONS)
                                .build()
                        )
                )
                .withDocumentCategory(documentCategory)
                .withName(SUMMONS)
                .withMimeType("application/pdf")
                .build();
    }

    private SummonsHearingCourtDetails populateHearingCourtDetails(final ZonedDateTime earliestDate, final JsonObject courtCentreJson) {
        return SummonsHearingCourtDetails.summonsHearingCourtDetails()
                .withCourtName(courtCentreJson.getString(L3_NAME))
                .withHearingDate(getCourtDate(earliestDate))
                .withHearingTime(getCourtTime(earliestDate))
                .withCourtAddress(SummonsAddress.summonsAddress()
                        .withLine1(courtCentreJson.getString(ADDRESS_1))
                        .withLine2(courtCentreJson.getString(ADDRESS_2, EMPTY))
                        .withLine3(courtCentreJson.getString(ADDRESS_3, EMPTY))
                        .withLine4(courtCentreJson.getString(ADDRESS_4, EMPTY))
                        .withLine5(courtCentreJson.getString(ADDRESS_5, EMPTY))
                        .withPostCode(courtCentreJson.getString(POSTCODE, null))
                        .build())
                .build();
    }

    private ApplicationSummonsDocumentContent populateApplicationContent(final CourtApplication courtApplication) {
        return ApplicationSummonsDocumentContent.applicationSummonsDocumentContent()
                .withApplicationType(courtApplication.getType().getApplicationType())
                .withApplicationLegislation(courtApplication.getType().getApplicationLegislation())
                .withApplicationParticulars(courtApplication.getApplicationParticulars())
                .withApplicationReference(courtApplication.getApplicationReference())
                .build();
    }

    private SummonsDefendant populateDefendant(final Defendant defendant) {
        if (nonNull(defendant)) {
            final PersonDefendant personDefendant = defendant.getPersonDefendant();
            if (nonNull(personDefendant)) {
                final Person person = personDefendant.getPersonDetails();

                return SummonsDefendant.summonsDefendant()
                        .withName(getFullName(person))
                        .withDateOfBirth(populateDateOfBirth(person.getDateOfBirth()))
                        .withAddress(getSummonsAddress(person.getAddress()))
                        .build();
            }
            final LegalEntityDefendant legalEntityDefendant = defendant.getLegalEntityDefendant();
            if (nonNull(legalEntityDefendant)) {
                return SummonsDefendant.summonsDefendant()
                        .withName(legalEntityDefendant.getOrganisation().getName())
                        .withAddress(getSummonsAddress(legalEntityDefendant.getOrganisation().getAddress()))
                        .build();
            }
        }
        return null;
    }

    private String populateDateOfBirth(final LocalDate dob) {
        String dateOfBirth = null;
        if (nonNull(dob)) {
            dateOfBirth = DATE_FORMATTER.format(dob);
        }
        return dateOfBirth;
    }
}
