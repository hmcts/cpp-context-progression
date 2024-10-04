package uk.gov.moj.cpp.progression.processor;

import static java.lang.String.format;
import static java.lang.String.join;
import static java.util.Collections.singletonList;
import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static java.util.Optional.ofNullable;
import static java.util.UUID.randomUUID;
import static javax.json.Json.createObjectBuilder;
import static org.slf4j.LoggerFactory.getLogger;
import static uk.gov.justice.services.core.annotation.Component.EVENT_PROCESSOR;
import static uk.gov.justice.services.messaging.Envelope.envelopeFrom;
import static uk.gov.justice.services.messaging.Envelope.metadataFrom;
import static uk.gov.moj.cpp.progression.domain.constant.OnlinePleaNotificationType.COMPANY_FINANCE_DATA;
import static uk.gov.moj.cpp.progression.domain.constant.OnlinePleaNotificationType.COMPANY_ONLINE_PLEA;
import static uk.gov.moj.cpp.progression.domain.constant.OnlinePleaNotificationType.INDIVIDUAL_FINANCE_DATA;
import static uk.gov.moj.cpp.progression.domain.constant.OnlinePleaNotificationType.INDIVIDUAL_ONLINE_PLEA;
import static uk.gov.moj.cpp.progression.domain.helper.JsonHelper.createBuilder;
import static uk.gov.moj.cpp.progression.helper.OnlinePleaProcessorHelper.PLEA_DOCUMENT_TYPE_DESCRIPTION;
import static uk.gov.moj.cpp.progression.helper.OnlinePleaProcessorHelper.PLEA_DOCUMENT_TYPE_ID;
import static uk.gov.moj.cpp.progression.helper.OnlinePleaProcessorHelper.SENTENCE_DOCUMENT_TYPE_DESCRIPTION;
import static uk.gov.moj.cpp.progression.helper.OnlinePleaProcessorHelper.SENTENCE_DOCUMENT_TYPE_ID;
import static uk.gov.moj.cpp.progression.helper.OnlinePleaProcessorHelper.isForPleaFinancialDocument;

import uk.gov.justice.core.courts.CourtDocument;
import uk.gov.justice.core.courts.DefendantDocument;
import uk.gov.justice.core.courts.DocumentCategory;
import uk.gov.justice.core.courts.Material;
import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.common.converter.ObjectToJsonObjectConverter;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.dispatcher.SystemUserProvider;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.fileservice.api.FileRetriever;
import uk.gov.justice.services.fileservice.api.FileServiceException;
import uk.gov.justice.services.fileservice.api.FileStorer;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.progression.events.OnlinePleaDocumentUploadedAsCaseMaterial;
import uk.gov.moj.cpp.progression.plea.json.schemas.Outgoing;
import uk.gov.moj.cpp.progression.plea.json.schemas.PleaNotificationType;
import uk.gov.moj.cpp.progression.plea.json.schemas.PleadOnline;
import uk.gov.moj.cpp.progression.service.MaterialService;

import java.io.ByteArrayInputStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.inject.Inject;
import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;


@ServiceComponent(EVENT_PROCESSOR)
public class OnlinePleaEventProcessor {

    public static final String PDF = "pdf";
    public static final String DATE_OF_PLEA = "dateOfPlea";
    public static final String TIME_OF_PLEA = "timeOfPlea";
    public static final String PLEA_DOC_SUFFIX = "Online Plea";
    public static final String FINANCE_DOC_SUFFIX = "MC100";
    public static final String URN = "urn";
    public static final String COMPANY_NAME = "companyName";
    public static final String PERSON_NAME = "personName";
    public static final String OUTSTANDING_FINES = "outstandingFines";
    public static final String FINANCIAL_DETAILS = "provideFinancialDetails";
    public static final String TRADING = "tradingMoreThan12Months";
    public static final String EMPLOYEES = "numberOfEmployees";
    public static final String GROSS_TURNOVER = "grossTurnover";
    public static final String NET_TURNOVER = "netTurnover";
    public static final String OUTGOINGS = "outgoings";
    public static final String MONTHLY_OUTGOINGS = "monthlyOutgoings";
    public static final String MONTHLY_OUTGOINGS_TOTAL = "monthlyAmountTotal";
    public static final String INCOME_BENEFITS = "incomeOrBenefits";
    public static final String EMPLOYMENT_STATUS = "employmentStatus";
    public static final String INCOME_FREQUENCY = "incomeFrequency";
    public static final String INCOME_AFTER_TAX = "income (after tax)";
    public static final String INCOME_AMOUNT = "incomeAmount";
    public static final String EMPLOYER_NAME = "employerName";
    public static final String EMPLOYER_REFERENCE = "employerReference";
    public static final String EMPLOYER_TELEPHONE = "employerTelephone";
    public static final String CLAIM_BENEFITS = "claimBenefits";
    public static final String DEDUCT_FROM_BENEFITS = "deductFromBenefits";
    public static final String DEDUCT_FROM_EARNINGS = "deductFromEarnings";
    public static final String BENEFITS_TYPE = "benefitsType";
    public static final String NATIONAL_INSURANCE_NUMBER = "nationalInsuranceNumber";
    private static final String CASE_ID = "caseId";
    private static final String PLEAD_ONLINE_REQUEST = "pleadOnline";
    private static final String FILE_NAME = "fileName";
    private static final String MATERIAL_ID = "materialId";
    private static final String COURT_DOCUMENT = "courtDocument";
    private static final String PROGRESSION_COMMAND_ADD_COURT_DOCUMENT = "progression.command.add-court-document";
    private static final String APPLICATION_PDF = "application/pdf";

    private static final Logger LOGGER = getLogger(OnlinePleaEventProcessor.class);
    public static final String DEFENDANT_ID = "defendantId";

    @Inject
    private FileStorer fileStorer;
    @Inject
    private FileRetriever fileRetriever;
    @Inject
    private Sender sender;
    @Inject
    private JsonObjectToObjectConverter jsonObjectToObjectConverter;
    @Inject
    private ObjectToJsonObjectConverter objectToJsonObjectConverter;
    @Inject
    private MaterialService materialService;
    @Inject
    private SystemUserProvider userProvider;

    public static byte[] jsonObjectAsByteArray(final JsonObject jsonObject) {
        return jsonObject.toString().getBytes(StandardCharsets.UTF_8);
    }

    @SuppressWarnings({"squid:S1160", "squid:S3655"})
    @Handles("progression.event.plea-document-for-online-plea-submitted")
    public void generateOnlinePleaDocument(final JsonEnvelope envelope) throws FileServiceException {
        final JsonObject payload = envelope.payloadAsJsonObject();
        final String caseId = payload.getString(CASE_ID);

        final JsonObject jsonObject = payload.getJsonObject(PLEAD_ONLINE_REQUEST);

        final JsonObject docGeneratorPayload = getOnlinePleaDocGeneratorPayload(payload, jsonObject);
        final PleadOnline pleadOnline = jsonObjectToObjectConverter.convert(jsonObject, PleadOnline.class);

        if (nonNull(pleadOnline.getPersonalDetails())) {
            final UUID fileId = storeOnlinePleaDocumentGeneratorPayload(docGeneratorPayload,
                    constructPersonDefendantFileName(pleadOnline, PLEA_DOC_SUFFIX), INDIVIDUAL_ONLINE_PLEA.getDescription());
            this.requestOnlinePleaDocumentGeneration(envelope, caseId, fileId, INDIVIDUAL_ONLINE_PLEA.getDescription(), INDIVIDUAL_ONLINE_PLEA.getDescription());
        } else if (nonNull(pleadOnline.getLegalEntityDefendant())) {
            final UUID fileId = storeOnlinePleaDocumentGeneratorPayload(docGeneratorPayload,
                    constructCompanyDefendantFileName(pleadOnline, PLEA_DOC_SUFFIX), COMPANY_ONLINE_PLEA.getDescription());
            this.requestOnlinePleaDocumentGeneration(envelope, caseId, fileId, COMPANY_ONLINE_PLEA.getDescription(), COMPANY_ONLINE_PLEA.getDescription());
        }
    }

    @SuppressWarnings({"squid:S1160", "squid:S3655"})
    @Handles("progression.event.finance-document-for-online-plea-submitted")
    public void generateFinanceOnlinePleaDocument(final JsonEnvelope envelope) throws FileServiceException {

        final JsonObject payload = envelope.payloadAsJsonObject();
        final String caseId = payload.getString(CASE_ID);

        final JsonObject jsonObject = payload.getJsonObject(PLEAD_ONLINE_REQUEST);
        final PleadOnline pleadOnline = jsonObjectToObjectConverter.convert(jsonObject, PleadOnline.class);

        if (nonNull(pleadOnline.getFinancialMeans())) {
            final UUID fileId = storeOnlinePleaDocumentGeneratorPayload(getIndividualFinanceDocGeneratorPayload(payload, pleadOnline),
                    constructPersonDefendantFileName(pleadOnline, FINANCE_DOC_SUFFIX), INDIVIDUAL_FINANCE_DATA.getDescription());
            this.requestOnlinePleaDocumentGeneration(envelope, caseId, fileId, INDIVIDUAL_FINANCE_DATA.getDescription(), INDIVIDUAL_FINANCE_DATA.getDescription());
        } else if (nonNull(pleadOnline.getLegalEntityFinancialMeans())) {
            final UUID fileId = storeOnlinePleaDocumentGeneratorPayload(getCompanyFinanceDocGeneratorPayload(payload, pleadOnline),
                    constructCompanyDefendantFileName(pleadOnline, FINANCE_DOC_SUFFIX), COMPANY_FINANCE_DATA.getDescription());
            this.requestOnlinePleaDocumentGeneration(envelope, caseId, fileId, COMPANY_FINANCE_DATA.getDescription(), COMPANY_FINANCE_DATA.getDescription());
        }
    }

    @Handles("progression.event.online-plea-document-uploaded-as-case-material")
    public void processOnlinePleaMaterialUploadRequest(final JsonEnvelope event) throws FileServiceException {

        final Optional<UUID> contextSystemUserId = userProvider.getContextSystemUserId();
        final OnlinePleaDocumentUploadedAsCaseMaterial uploadedAsCaseMaterial = jsonObjectToObjectConverter.convert(event.payloadAsJsonObject(), OnlinePleaDocumentUploadedAsCaseMaterial.class);

        final Optional<JsonObject> fileMetaData = fileRetriever.retrieveMetadata(uploadedAsCaseMaterial.getFileId());
        if (fileMetaData.isPresent()) {
            final JsonObject fileMetaDataJsonObject = fileMetaData.get();
            final String fileName = fileMetaDataJsonObject.getJsonString(FILE_NAME).getString();
            materialService.uploadMaterial(uploadedAsCaseMaterial.getFileId(), uploadedAsCaseMaterial.getMaterialId(), contextSystemUserId.orElse(null));

            final JsonObject jsonObject = Json.createObjectBuilder()
                    .add(MATERIAL_ID, uploadedAsCaseMaterial.getMaterialId().toString())
                    .add(COURT_DOCUMENT, objectToJsonObjectConverter
                            .convert(buildCourtDocument(uploadedAsCaseMaterial.getCaseId(), uploadedAsCaseMaterial.getMaterialId(), fileName, uploadedAsCaseMaterial.getDefendantId(), uploadedAsCaseMaterial.getPleaNotificationType()))).build();

            LOGGER.info("court document is being created '{}' ", jsonObject);

            sender.sendAsAdmin(envelopeFrom(
                    metadataFrom(event.metadata()).withName(PROGRESSION_COMMAND_ADD_COURT_DOCUMENT),
                    jsonObject
            ));
        }

    }

    private String constructCompanyDefendantFileName(final PleadOnline pleadOnline, final String suffix) {
        return join(" ", pleadOnline.getLegalEntityDefendant().getName(), suffix);
    }

    private void requestOnlinePleaDocumentGeneration(final JsonEnvelope eventEnvelope,
                                                     final String caseId,
                                                     final UUID payloadFileServiceUUID,
                                                     final String originatingSource,
                                                     final String templateIdentifier) {

        final JsonObject docGeneratorPayload = createObjectBuilder()
                .add("originatingSource", originatingSource)
                .add("templateIdentifier", templateIdentifier)
                .add("conversionFormat", PDF)
                .add("sourceCorrelationId", caseId)
                .add("payloadFileServiceId", payloadFileServiceUUID.toString())
                .build();

        sender.sendAsAdmin(
                Envelope.envelopeFrom(
                        metadataFrom(eventEnvelope.metadata()).withName("systemdocgenerator.generate-document"),
                        docGeneratorPayload
                )
        );
    }

    private UUID storeOnlinePleaDocumentGeneratorPayload(final JsonObject docGeneratorPayload, final String fileName, final String templateName) throws FileServiceException {
        final byte[] jsonPayloadInBytes = jsonObjectAsByteArray(docGeneratorPayload);

        final JsonObject metadata = createObjectBuilder()
                .add(FILE_NAME, fileName)
                .add("conversionFormat", PDF)
                .add("templateName", templateName)
                .add("numberOfPages", 1)
                .add("fileSize", jsonPayloadInBytes.length)
                .build();
        return fileStorer.store(metadata, new ByteArrayInputStream(jsonPayloadInBytes));
    }

    private JsonObject getOnlinePleaDocGeneratorPayload(final JsonObject payload, final JsonObject jsonObject) {
        return createBuilder(jsonObject)
                .add(DATE_OF_PLEA, payload.getString(DATE_OF_PLEA))
                .add(TIME_OF_PLEA, payload.getString(TIME_OF_PLEA))
                .build();
    }

    private JsonObject getIndividualFinanceDocGeneratorPayload(final JsonObject payload, final PleadOnline pleadOnline) {
        final JsonObjectBuilder builder = Json.createObjectBuilder();
        builder.add(URN, pleadOnline.getUrn())
                .add(CASE_ID, pleadOnline.getCaseId().toString())
                .add(DEFENDANT_ID, pleadOnline.getDefendantId().toString())
                .add(DATE_OF_PLEA, payload.getString(DATE_OF_PLEA))
                .add(DATE_OF_PLEA, payload.getString(DATE_OF_PLEA))
                .add(TIME_OF_PLEA, payload.getString(TIME_OF_PLEA))
                .add(OUTGOINGS, false)
                .add(INCOME_BENEFITS, false)
                .add(DEDUCT_FROM_EARNINGS, false);

        ofNullable(pleadOnline.getOutstandingFines()).ifPresent(outstandingFines -> builder.add(OUTSTANDING_FINES, outstandingFines));
        ofNullable(pleadOnline.getPersonalDetails().getNationalInsuranceNumber()).ifPresent(insuranceNumber -> builder.add(NATIONAL_INSURANCE_NUMBER, insuranceNumber));
        ofNullable(pleadOnline.getPersonalDetails()).ifPresent(personalDetails -> builder.add(PERSON_NAME, getFullName(personalDetails.getFirstName(), personalDetails.getLastName())));
        ofNullable(pleadOnline.getFinancialMeans()).ifPresent(financialMeans -> {
            ofNullable(financialMeans.getEmploymentStatus()).ifPresent(employmentStatus ->
                    builder.add(EMPLOYMENT_STATUS, employmentStatus));

            ofNullable(financialMeans.getIncome()).ifPresent(income -> {
                builder.add(INCOME_BENEFITS, true);
                ofNullable(income.getFrequency()).ifPresent(frequency -> builder.add(INCOME_FREQUENCY, format("%s %s", income.getFrequency().toString(), INCOME_AFTER_TAX)));
                ofNullable(income.getAmount()).ifPresent(amount -> builder.add(INCOME_AMOUNT, amount));

            });

            ofNullable(financialMeans.getBenefits()).ifPresent(benefits -> {
                builder.add(INCOME_BENEFITS, true);
                ofNullable(benefits.getClaimed()).ifPresent(claimed -> builder.add(CLAIM_BENEFITS, claimed));
                ofNullable(benefits.getDeductPenaltyPreference()).ifPresent(deductPenaltyPreference -> builder.add(DEDUCT_FROM_BENEFITS, deductPenaltyPreference));
                ofNullable(benefits.getType()).ifPresent(type -> builder.add(BENEFITS_TYPE, type));
            });
        });

        ofNullable(pleadOnline.getOutgoings()).ifPresent(outgoings -> {
            builder.add(OUTGOINGS, true);
            ofNullable(calculateTotalOutgoings(outgoings)).ifPresent(totalOutgoings -> builder.add(MONTHLY_OUTGOINGS_TOTAL, totalOutgoings));
            ofNullable(payload.getJsonObject(PLEAD_ONLINE_REQUEST).getJsonArray(OUTGOINGS)).ifPresent(monthlyOutgoings -> builder.add(MONTHLY_OUTGOINGS, monthlyOutgoings));
        });

        ofNullable(pleadOnline.getEmployer()).ifPresent(employer -> {
            builder.add(DEDUCT_FROM_EARNINGS, true);
            ofNullable(employer.getName()).ifPresent(name -> builder.add(EMPLOYER_NAME, name));
            ofNullable(employer.getEmployeeReference()).ifPresent(employeeReference -> builder.add(EMPLOYER_REFERENCE, employeeReference));
            ofNullable(employer.getPhone()).ifPresent(phone -> builder.add(EMPLOYER_TELEPHONE, phone));
        });

        return builder.build();
    }

    private JsonObject getCompanyFinanceDocGeneratorPayload(final JsonObject payload, final PleadOnline pleadOnline) {
        final JsonObjectBuilder builder = Json.createObjectBuilder();
        builder.add(URN, pleadOnline.getUrn())
                .add(CASE_ID, pleadOnline.getCaseId().toString())
                .add(DEFENDANT_ID, pleadOnline.getDefendantId().toString())
                .add(DATE_OF_PLEA, payload.getString(DATE_OF_PLEA))
                .add(TIME_OF_PLEA, payload.getString(TIME_OF_PLEA))
                .add(FINANCIAL_DETAILS, false);

        ofNullable(pleadOnline.getLegalEntityDefendant()).ifPresent(legalEntity -> builder.add(COMPANY_NAME, legalEntity.getName()));

        ofNullable(pleadOnline.getLegalEntityFinancialMeans()).ifPresent(financialMeans -> {
            builder.add(OUTSTANDING_FINES, financialMeans.getOutstandingFines())
                    .add(FINANCIAL_DETAILS, true)
                    .add(GROSS_TURNOVER, financialMeans.getGrossTurnover())
                    .add(NET_TURNOVER, financialMeans.getNetTurnover());
            if(nonNull(financialMeans.getNumberOfEmployees())){
                builder.add(EMPLOYEES, financialMeans.getNumberOfEmployees());
            }
            ofNullable(financialMeans.getTradingMoreThan12Months()).ifPresent(tradingMoreThan12Months -> builder.add(TRADING, tradingMoreThan12Months.toString()));
        });

        return builder.build();
    }

    private String constructPersonDefendantFileName(final PleadOnline pleadOnline, final String suffix) {
        return join(" ", pleadOnline.getPersonalDetails().getFirstName().concat(pleadOnline.getPersonalDetails().getLastName()), suffix);
    }

    private BigDecimal calculateTotalOutgoings(final List<Outgoing> outgoings) {
        if (isNull(outgoings)) {
            return null;
        } else {
            return outgoings.stream()
                    .map(Outgoing::getAmount)
                    .filter(Objects::nonNull)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
        }
    }

    private String getFullName(final String firstName, final String lastName) {
        return Stream.of(firstName, lastName)
                .filter(StringUtils::isNotBlank)
                .collect(Collectors.joining(" "));
    }

    private CourtDocument buildCourtDocument(final UUID caseId, final UUID materialId, final String filename, final UUID defendantId, final PleaNotificationType pleaNotificationType) {

        final DocumentCategory documentCategory = DocumentCategory.documentCategory()
                .withDefendantDocument(DefendantDocument.defendantDocument()
                        .withProsecutionCaseId(caseId)
                        .withDefendants(singletonList(defendantId))
                        .build())
                .build();

        final Material material = Material.material()
                .withId(materialId)
                .withUploadDateTime(ZonedDateTime.now())
                .build();
        final CourtDocument.Builder courtDocumentBuilder = CourtDocument.courtDocument();

        courtDocumentBuilder
                .withCourtDocumentId(randomUUID())
                .withDocumentCategory(documentCategory)
                .withMimeType(APPLICATION_PDF)
                .withName(filename)
                .withMaterials(singletonList(material));

        if (isForPleaFinancialDocument(pleaNotificationType.toString())) {
            courtDocumentBuilder.withDocumentTypeDescription(SENTENCE_DOCUMENT_TYPE_DESCRIPTION)
                    .withContainsFinancialMeans(true)
                    .withDocumentTypeId(SENTENCE_DOCUMENT_TYPE_ID);
        } else {
            courtDocumentBuilder.withDocumentTypeDescription(PLEA_DOCUMENT_TYPE_DESCRIPTION)
                    .withDocumentTypeId(PLEA_DOCUMENT_TYPE_ID);
        }

        return courtDocumentBuilder.build();
    }

}
