package uk.gov.moj.cpp.progression.processor;

import static java.util.Collections.singletonList;
import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static java.util.UUID.fromString;
import static java.util.UUID.randomUUID;
import static java.util.stream.Collectors.toList;
import static uk.gov.justice.services.messaging.JsonObjects.createArrayBuilder;
import static uk.gov.justice.services.messaging.JsonObjects.createObjectBuilder;
import static org.apache.commons.collections.CollectionUtils.isNotEmpty;
import static org.apache.commons.lang3.StringUtils.EMPTY;
import static org.apache.commons.lang3.StringUtils.isNotEmpty;
import static org.slf4j.LoggerFactory.getLogger;
import static uk.gov.justice.core.courts.CaseDocument.caseDocument;
import static uk.gov.justice.core.courts.DefendantDocument.defendantDocument;
import static uk.gov.justice.services.core.annotation.Component.EVENT_PROCESSOR;
import static uk.gov.justice.services.core.enveloper.Enveloper.envelop;
import static uk.gov.justice.services.messaging.Envelope.metadataFrom;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;

import uk.gov.justice.core.courts.CourtDocument;
import uk.gov.justice.core.courts.Defendant;
import uk.gov.justice.core.courts.DocumentCategory;
import uk.gov.justice.core.courts.FormType;
import uk.gov.justice.core.courts.Material;
import uk.gov.justice.core.courts.ProsecutionCase;
import uk.gov.justice.core.courts.ProsecutionCaseIdentifier;
import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.common.converter.ObjectToJsonObjectConverter;
import uk.gov.justice.services.common.converter.StringToJsonObjectConverter;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.requester.Requester;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.progression.command.UpdateCpsDefendantId;
import uk.gov.moj.cpp.progression.service.CpsApiService;
import uk.gov.moj.cpp.progression.service.DefenceService;
import uk.gov.moj.cpp.progression.service.DocumentGeneratorService;
import uk.gov.moj.cpp.progression.service.ProgressionService;
import uk.gov.moj.cpp.progression.service.UsersGroupService;

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import javax.inject.Inject;
import uk.gov.justice.services.messaging.JsonObjects;
import javax.json.JsonArray;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.json.JsonString;
import javax.json.JsonValue;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;

@SuppressWarnings({"squid:S134", "squid:S3776", "squid:S1188", "squid:S3655"})
@ServiceComponent(EVENT_PROCESSOR)
public class FormEventProcessor {

    private static final Logger LOGGER = getLogger(FormEventProcessor.class);
    public static final String COURT_FORM_ID = "courtFormId";
    public static final String FORM_TYPE = "formType";
    public static final String FINALISED_FORM_DATA = "finalisedFormData";
    public static final String FORM_DEFENDANTS = "formDefendants";
    public static final String FORM_ID = "formId";
    public static final String FORM_DATA = "formData";
    public static final String UPDATED_BY = "updatedBy";
    public static final String ID = "id";
    public static final String CASE_ID = "caseId";
    public static final String CASE_URN = "caseURN";
    public static final String HEARING_DATE_TIME = "hearingDateTime";
    public static final String DOCUMENT_META_DATA = "documentMetaData";
    public static final String MATERIAL_ID = "materialId";
    public static final String USER_ID = "userId";
    public static final String USER_NAME = "userName";
    public static final String IS_WELSH = "isWelsh";
    public static final String DOCUMENT_FILE_NAME = "fileName";
    public static final String LOCK_STATUS = "lockStatus";
    public static final String EXPIRY_TIME = "expiryTime";
    public static final String IS_LOCKED = "isLocked";
    public static final String LOCKED_BY = "lockedBy";
    public static final String LOCK_REQUESTED_BY = "lockRequestedBy";
    public static final String FIRST_NAME = "firstName";
    public static final String LAST_NAME = "lastName";
    public static final String EMAIL = "email";

    public static final String PUBLIC_PROGRESSION_FORM_CREATED = "public.progression.form-created";
    public static final String PUBLIC_PROGRESSION_FORM_FINALISED = "public.progression.form-finalised";
    public static final String PUBLIC_PROGRESSION_FORM_OPERATION_FAILED = "public.progression.form-operation-failed";
    public static final String PUBLIC_PROGRESSION_FORM_UPDATED = "public.progression.form-updated";
    public static final String PUBLIC_PROGRESSION_EDIT_FORM_REQUESTED = "public.progression.edit-form-requested";

    public static final String PROGRESSION_COMMAND_ADD_COURT_DOCUMENT = "progression.command.add-court-document";

    public static final UUID CASE_DOCUMENT_TYPE_ID = fromString("6b9df1fb-7bce-4e33-88dd-db91f75adeb8");

    public static final String COURT_DOCUMENT = "courtDocument";
    public static final String PUBLIC_PROGRESSION_BCM_DEFENDANTS_UPDATED = "public.progression.event.form-defendants-updated";
    public static final String DEFENDANT_ID = "defendantId";
    public static final String DEFENDANT_IDS = "defendantIds";
    public static final String APPLICATION_PDF = "application/pdf";
    public static final String DOCUMENT_TYPE_DESCRIPTION = "Case Management";

    public static final String BCM = "BCM";
    public static final String PTPH = "PTPH";
    public static final String OTHER_LINKED_CASES = "otherLinkedCases";
    public static final String DEFENDING = "defending";
    private static final String PROSECUTION_CASE = "prosecutionCase";
    private static final String CPS_DEFENDANT_ID = "cpsDefendantId";
    private static final String ASN = "asn";
    private static final String CJS_OFFENCE_CODE = "cjsOffenceCode";
    private static final String OFFENCE_SEQUENCE_NO = "offenceSequenceNo";
    private static final String OFFENCE_TITLE = "offenceTitle";
    private static final String PLEA_VALUE = "pleaValue";
    private static final String REAL_ISSUES = "realIssues";
    private static final String OTHER_EVIDENCE = "otherEvidencePriorPtph";
    private static final String ANY_OTHER = "anyOther";
    public static final String CMS_USER = "CMS user";
    public static final String SUBMISSION_ID = "submissionId";
    private static final String IS_ADVOCATE_DEFENDING = "isAdvocateDefendingOrProsecuting";
    private static final String GROUP_ADVOCATES_USER = "Advocates";

    private static final DateTimeFormatter ZONE_DATETIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");

    @Inject
    private Sender sender;

    @Inject
    private Requester requester;

    @Inject
    private DocumentGeneratorService documentGeneratorService;

    @Inject
    private StringToJsonObjectConverter stringToJsonObjectConverter;

    @Inject
    private ObjectToJsonObjectConverter objectToJsonObjectConverter;

    @Inject
    private CpsApiService cpsApiService;

    @Inject
    private UsersGroupService usersGroupService;

    @Inject
    private ProgressionService progressionService;

    @Inject
    private JsonObjectToObjectConverter jsonObjectToObjectConverter;

    @Inject
    private DefenceService defenceService;

    @Handles("progression.event.form-created")
    public void formCreated(final JsonEnvelope event) {
        LOGGER.info("progression.event.form-created event received with id: {} for case: {}", event.payloadAsJsonObject().getString(COURT_FORM_ID), event.payloadAsJsonObject().getString(CASE_ID));

        final JsonObject publicEventPayload = buildFormCreatedPublicEventPayload(event);

        sender.send(envelopeFrom(
                metadataFrom(event.metadata()).withName(PUBLIC_PROGRESSION_FORM_CREATED),
                publicEventPayload
        ));

        final String caseId = publicEventPayload.getString(CASE_ID);
        final String formType = publicEventPayload.getString(FORM_TYPE);

        LOGGER.info("Case Id: {}, Form Type: {}", caseId, formType);

        if (BCM.equalsIgnoreCase(formType) && isNotEmpty(caseId) && nonNull(event.metadata()) && event.metadata().userId().isPresent()) {
            final boolean isUserPartOfAdvocatesGroup = usersGroupService.isUserPartOfGroup(event, GROUP_ADVOCATES_USER);
            LOGGER.info("Is User part of Advocates group : {}", isUserPartOfAdvocatesGroup);
            if (isUserPartOfAdvocatesGroup) {
                final JsonObject roleObject = defenceService.getRoleInCaseByCaseId(event, caseId);
                if (nonNull(roleObject) && roleObject.containsKey(IS_ADVOCATE_DEFENDING) && DEFENDING.equalsIgnoreCase(roleObject.getString(IS_ADVOCATE_DEFENDING))) {
                    LOGGER.info("Notifying CPS for Form Creation for Case Id: {}, Form Type: {}, Advocate Role {}", caseId, formType, roleObject.getString(IS_ADVOCATE_DEFENDING));

                    final ProsecutionCase prosecutionCase = fetchProsecutionCase(event, caseId);
                    try {
                        notifyCPS(publicEventPayload, prosecutionCase);
                    } catch (final RuntimeException e) {
                        LOGGER.error("bcm-form-updated notification to CPS failed on bcm creation", e);
                    }
                }
            }
        }
    }

    private JsonObject buildFormCreatedPublicEventPayload(final JsonEnvelope event) {
        final JsonObject formCreated = event.payloadAsJsonObject();
        final UUID courtFormId = fromString(formCreated.getString(COURT_FORM_ID));
        final UUID caseId = fromString(formCreated.getString(CASE_ID));
        final FormType formType = FormType.valueOf(formCreated.getString(FORM_TYPE));
        final JsonArray formDefendants = formCreated.getJsonArray(FORM_DEFENDANTS);
        final String submissionId = formCreated.getString(SUBMISSION_ID, null);

        final JsonObjectBuilder publicEventBuilder = createObjectBuilder()
                .add(COURT_FORM_ID, courtFormId.toString())
                .add(FORM_TYPE, formType.name())
                .add(CASE_ID, caseId.toString())
                .add(FORM_DEFENDANTS, formDefendants);
        if (isNotEmpty(submissionId)) {
            publicEventBuilder.add(SUBMISSION_ID, submissionId);
        }
        if (formCreated.containsKey(FORM_DATA)) {
            publicEventBuilder.add(FORM_DATA, formCreated.getString(FORM_DATA));
        }

        if (formCreated.containsKey(FORM_ID)) {
            publicEventBuilder.add(FORM_ID, formCreated.getString(FORM_ID));
        }

        publicEventBuilder.add(UPDATED_BY, getUpdatedBy(event, formCreated.getString(USER_ID, null), formCreated.getString(USER_NAME, null)));

        return publicEventBuilder.build();
    }

    private void notifyCPS(final JsonObject creationPayload, final ProsecutionCase prosecutionCase) {
        LOGGER.info("notifyCPS()");
        final JsonArray formDefendantsArray = creationPayload.getJsonArray(FORM_DEFENDANTS);

        if (nonNull(prosecutionCase)) {
            LOGGER.info("Fetched ProsecutionCase details with id {}", prosecutionCase.getId());
            final ProsecutionCaseIdentifier prosecutionCaseIdentifier = prosecutionCase.getProsecutionCaseIdentifier();
            final List<Defendant> defendants = prosecutionCase.getDefendants();

            final JsonObject formDataObject = stringToJsonObjectConverter.convert(creationPayload.getString(FORM_DATA));
            final JsonObject data = formDataObject.getJsonObject("data");
            final JsonArray formDataDefendantsArray = nonNull(data) ? data.getJsonArray("defendants") : null;

            final JsonArrayBuilder pleasBuilder = createArrayBuilder();
            final JsonObjectBuilder defendantSubjectBuilder = createObjectBuilder();

            if (nonNull(formDefendantsArray) && nonNull(defendants)) {
                for (final JsonObject formDefendant : formDefendantsArray.getValuesAs(JsonObject.class)) {
                    final String finalOtherLinkedCases = nonNull(formDataObject.getJsonString(OTHER_LINKED_CASES)) ? formDataObject.getJsonString(OTHER_LINKED_CASES).getString() : EMPTY;
                    LOGGER.info("otherLinkedCases: {}", finalOtherLinkedCases);

                    Optional<JsonObject> anyDefendantData = Optional.empty();
                    if (isNotEmpty(formDataDefendantsArray)) {
                        anyDefendantData = formDataDefendantsArray.getValuesAs(JsonObject.class).stream()
                                .filter(defendant -> formDefendant.getString(DEFENDANT_ID).contentEquals(defendant.getString("id")))
                                .findAny();
                    }
                    final Optional<JsonObject> matchedFormDefendantFromData = anyDefendantData;
                    defendants.forEach(caseDefendant -> notifyPerDefendant(prosecutionCaseIdentifier, pleasBuilder, defendantSubjectBuilder, formDefendant, finalOtherLinkedCases, caseDefendant, matchedFormDefendantFromData));
                }
            } else {
                LOGGER.info("BCM Notification not send due to absence of defendant information");
            }
        } else {
            LOGGER.info("BCM Notification not send due to absence of prosecution case information");
        }
    }

    private void notifyPerDefendant(final ProsecutionCaseIdentifier prosecutionCaseIdentifier, final JsonArrayBuilder pleasBuilder, final JsonObjectBuilder defendantSubjectBuilder, final JsonObject formDefendant, final String finalOtherLinkedCases, final Defendant caseDefendant, final Optional<JsonObject> matchedFormDefendantFromData) {
        final UUID defendantId = caseDefendant.getId();
        LOGGER.info("Fetching details for defendant id : {}", defendantId);
        final String asn = nonNull(caseDefendant.getPersonDefendant()) ? caseDefendant.getPersonDefendant().getArrestSummonsNumber() : null;
        LOGGER.info("Defendant ASN: {}", asn);

        if (nonNull(defendantId) && defendantId.toString().equals(formDefendant.getString(DEFENDANT_ID))) {
            final String cpsDefendantId = caseDefendant.getCpsDefendantId();
            LOGGER.info("cpsDefendantid {} for defendantId {}", cpsDefendantId, defendantId);

            if (nonNull(asn)) {
                defendantSubjectBuilder.add(ASN, asn);
            } else if (nonNull(cpsDefendantId)) {
                defendantSubjectBuilder.add(CPS_DEFENDANT_ID, cpsDefendantId);
            } else {
                LOGGER.error("asn or cpsDefendantId not found for defendant {}", defendantId);
            }

            final JsonObject defendantFromData = matchedFormDefendantFromData.orElse(null);
            final JsonArray formOffences = nonNull(defendantFromData) ? defendantFromData.getJsonArray("formOffences") : null;
            final JsonArray prosecutorOffences = formDefendant.getJsonArray("prosecutorOffences");
            buildPleasFromFormData(formOffences, prosecutorOffences, pleasBuilder);

            final JsonArray pleas = pleasBuilder.build();

            if (!pleas.isEmpty()) {
                final JsonObject jsonObject = buildBcmNotificationPayload(prosecutionCaseIdentifier, defendantSubjectBuilder, pleas, finalOtherLinkedCases, defendantFromData);
                cpsApiService.sendNotification(jsonObject);
            }
        }
    }

    private JsonObject buildBcmNotificationPayload(final ProsecutionCaseIdentifier prosecutionCaseIdentifier, final JsonObjectBuilder defendantSubjectBuilder,
                                                   final JsonArray pleas, final String linkedCases, final JsonObject defendantFromData) {
        LOGGER.info("Build BcmNotification Payload");
        final JsonObjectBuilder payloadBuilder = createObjectBuilder()
                .add("notificationDate", ZONE_DATETIME_FORMATTER.format(ZonedDateTime.now()))
                .add("notificationType", "bcm-form-updated");

        final String caseUrn = nonNull(prosecutionCaseIdentifier.getCaseURN()) ? prosecutionCaseIdentifier.getCaseURN() : prosecutionCaseIdentifier.getProsecutionAuthorityReference();

        final JsonObjectBuilder bcmPayloadBuilder = createObjectBuilder()
                .add("prosecutionCaseSubject", createObjectBuilder()
                        .add(CASE_URN, caseUrn)
                        .add("prosecutingAuthority", prosecutionCaseIdentifier.getProsecutionAuthorityOUCode())
                )
                .add("defendantSubject", defendantSubjectBuilder.build())
                .add("pleas", pleas);

        if (isNotEmpty(linkedCases)) {
            bcmPayloadBuilder.add("linkedCaseDetails", linkedCases);
        }

        if (nonNull(defendantFromData)) {
            if (isNotEmpty(defendantFromData.getString(REAL_ISSUES, ""))) {
                bcmPayloadBuilder.add("realIssuesInCase", defendantFromData.getString(REAL_ISSUES));
            }
            if (isNotEmpty(defendantFromData.getString(OTHER_EVIDENCE, ""))) {
                bcmPayloadBuilder.add("evidenceNeededForEffectivePTPH", defendantFromData.getString(OTHER_EVIDENCE));
            }
            if (isNotEmpty(defendantFromData.getString(ANY_OTHER, ""))) {
                bcmPayloadBuilder.add("otherInformation", defendantFromData.getString(ANY_OTHER));
            }
        }

        payloadBuilder.add("bcmNotification", bcmPayloadBuilder.build());

        return payloadBuilder.build();
    }

    @Handles("progression.event.form-operation-failed")
    public void formOperationFailed(final JsonEnvelope event) {
        LOGGER.info("progression.event.form-operation-failed event received: {}", event);
        sender.send(envelopeFrom(
                metadataFrom(event.metadata()).withName(PUBLIC_PROGRESSION_FORM_OPERATION_FAILED),
                event.payload()
        ));
    }

    @Handles("progression.event.form-updated")
    public void formUpdated(final JsonEnvelope event) {
        LOGGER.info("progression.event.form-updated event received with courtFormId: {} for case: {}", event.payloadAsJsonObject().getString(COURT_FORM_ID), event.payloadAsJsonObject().getString(CASE_ID));

        final JsonObject publicEventPayload = buildFormUpdatedPublicEventPayload(event);

        sender.send(envelopeFrom(
                metadataFrom(event.metadata()).withName(PUBLIC_PROGRESSION_FORM_UPDATED),
                publicEventPayload
        ));

        final JsonObject formUpdated = event.payloadAsJsonObject();
        final String caseId = formUpdated.getString(CASE_ID);
        final String formType = formUpdated.getString(FORM_TYPE);

        if (BCM.equalsIgnoreCase(formType) && isNotEmpty(caseId) && nonNull(event.metadata()) && event.metadata().userId().isPresent()) {
            final boolean isUserPartOfAdvocatesGroup = usersGroupService.isUserPartOfGroup(event, GROUP_ADVOCATES_USER);
            LOGGER.info("Is User part of Advocates group : {}", isUserPartOfAdvocatesGroup);
            if (isUserPartOfAdvocatesGroup) {
                final JsonObject roleObject = defenceService.getRoleInCaseByCaseId(event, caseId);

                if (nonNull(roleObject) && roleObject.containsKey(IS_ADVOCATE_DEFENDING) && DEFENDING.equalsIgnoreCase(roleObject.getString(IS_ADVOCATE_DEFENDING))) {
                    LOGGER.info("Notifying CPS on Form updation for Case Id: {}, Form Type: {}, Advocate Role {}", caseId, formType, roleObject.getString(IS_ADVOCATE_DEFENDING));
                    try {
                        notifyCPSOnUpdateBcm(event, caseId);
                    } catch (final RuntimeException e) {
                        LOGGER.error("bcm-form-updated notification to CPS failed on bcm update", e);
                    }
                }
            }
        }
    }

    private JsonObject buildFormUpdatedPublicEventPayload(final JsonEnvelope event) {
        final JsonObject formUpdated = event.payloadAsJsonObject();
        final UUID courtFormId = fromString(formUpdated.getString(COURT_FORM_ID));
        final UUID caseId = fromString(formUpdated.getString(CASE_ID));
        final String formData = formUpdated.getString(FORM_DATA);

        final JsonObjectBuilder publicEventBuilder = createObjectBuilder()
                .add(COURT_FORM_ID, courtFormId.toString())
                .add(CASE_ID, caseId.toString())
                .add(FORM_DATA, formData);

        publicEventBuilder.add(UPDATED_BY, getUpdatedBy(event, formUpdated.getString(USER_ID, null), formUpdated.getString(USER_NAME, null)));

        return publicEventBuilder.build();
    }

    private ProsecutionCase fetchProsecutionCase(final JsonEnvelope event, final String caseId) {
        final Optional<JsonObject> optionalProsecutionCase = progressionService.getProsecutionCaseDetailById(event, caseId);
        if (!optionalProsecutionCase.isPresent()) {
            throw new IllegalStateException(String.format("Unable to find the case %s", caseId));
        }

        return jsonObjectToObjectConverter.convert(optionalProsecutionCase.get().getJsonObject(PROSECUTION_CASE), ProsecutionCase.class);
    }

    private void buildPleasFromFormData(final JsonArray formOffences, final JsonArray prosecutorOffences, final JsonArrayBuilder pleasBuilder) {
        final AtomicInteger seqNo = new AtomicInteger(1);
        if (isNotEmpty(formOffences)) {
            for (final JsonObject formOffence : formOffences.getValuesAs(JsonObject.class)) {
                final JsonObjectBuilder pleaObjectBuilder = createObjectBuilder();

                pleaObjectBuilder.add(CJS_OFFENCE_CODE, StringUtils.EMPTY)
                        .add(OFFENCE_SEQUENCE_NO, seqNo.getAndIncrement())
                        .add(OFFENCE_TITLE, formOffence.getString("description"))
                        .add(PLEA_VALUE, formOffence.getString("plea"));

                pleasBuilder.add(pleaObjectBuilder.build());
            }
        }

        if (isNotEmpty(prosecutorOffences)) {
            for (final JsonObject prosecutorOffence : prosecutorOffences.getValuesAs(JsonObject.class)) {
                final JsonObjectBuilder pleaObjectBuilder = createObjectBuilder();

                pleaObjectBuilder.add(CJS_OFFENCE_CODE, prosecutorOffence.getString("offenceCode", ""))
                        .add(OFFENCE_SEQUENCE_NO, seqNo.getAndIncrement())
                        .add(OFFENCE_TITLE, prosecutorOffence.getString("wording", ""))
                        .add(PLEA_VALUE, StringUtils.EMPTY);

                pleasBuilder.add(pleaObjectBuilder.build());
            }
        }
    }

    private void notifyCPSOnUpdateBcm(final JsonEnvelope event, final String caseId) {
        LOGGER.info("notifyCPSOnUpdateBcm()");

        final ProsecutionCase prosecutionCase = fetchProsecutionCase(event, caseId);
        if (nonNull(prosecutionCase)) {
            LOGGER.info("Fetched ProsecutionCase details with id {}", prosecutionCase.getId());
            final ProsecutionCaseIdentifier prosecutionCaseIdentifier = prosecutionCase.getProsecutionCaseIdentifier();
            final List<Defendant> defendants = prosecutionCase.getDefendants();

            final JsonObject publicEventPayload = buildFormUpdatedPublicEventPayload(event);
            final String data = publicEventPayload.getString(FORM_DATA);
            LOGGER.info("Form Data {}", data);

            final JsonObject formDataObject = stringToJsonObjectConverter.convert(data);
            final JsonArray formDataDefendantsArray = formDataObject.getJsonObject("data").getJsonArray("defendants");

            final JsonArrayBuilder pleasBuilder = createArrayBuilder();
            final JsonObjectBuilder defendantSubjectBuilder = createObjectBuilder();

            if (nonNull(formDataDefendantsArray) && nonNull(defendants)) {
                for (final JsonObject formDefendant : formDataDefendantsArray.getValuesAs(JsonObject.class)) {
                    final String finalOtherLinkedCases = nonNull(formDataObject.getJsonString(OTHER_LINKED_CASES)) ? formDataObject.getJsonString(OTHER_LINKED_CASES).getString() : StringUtils.EMPTY;
                    LOGGER.info("otherLinkedCases: {}", finalOtherLinkedCases);

                    defendants.forEach(defendant -> {
                        final String defendantId = defendant.getId().toString();
                        LOGGER.info("Defendant Id: {}", defendantId);

                        final String asn = nonNull(defendant.getPersonDefendant()) ? defendant.getPersonDefendant().getArrestSummonsNumber() : null;
                        LOGGER.info("Defendant ASN: {}", asn);

                        final String cpsDefendantId = defendant.getCpsDefendantId();
                        LOGGER.info("cpsDefendantid {} for defendantId {}", cpsDefendantId, defendantId);

                        if (nonNull(defendantId) && nonNull(formDefendant) && (formDefendant.size() > 0) && defendantId.equals(formDefendant.getString(ID))) {
                            LOGGER.info("defendantId and formDefendant matched {}", defendantId);
                            if (nonNull(asn)) {
                                defendantSubjectBuilder.add(ASN, asn);
                            } else if (nonNull(cpsDefendantId)) {
                                defendantSubjectBuilder.add(CPS_DEFENDANT_ID, cpsDefendantId);
                            } else {
                                LOGGER.error("asn or cpsDefendantId not found for defendant {}", defendantId);
                            }

                            final JsonArray formOffences = formDefendant.getJsonArray("formOffences");
                            final JsonArray prosecutorOffences = formDefendant.getJsonArray("prosecutorOffences");
                            if (nonNull(formOffences) || nonNull(prosecutorOffences)) {
                                buildPleasFromFormData(formOffences, prosecutorOffences, pleasBuilder);
                                final JsonArray pleas = pleasBuilder.build();

                                if (!pleas.isEmpty()) {
                                    final JsonObject jsonObject = buildBcmNotificationPayload(prosecutionCaseIdentifier, defendantSubjectBuilder, pleas,
                                            finalOtherLinkedCases, formDefendant);

                                    cpsApiService.sendNotification(jsonObject);
                                }
                            } else {
                                LOGGER.info("BCM Notification not send due to absence of offence information");
                            }
                        }
                    });
                }
            } else {
                LOGGER.info("BCM Notification not send due to absence of defendant information");
            }
        }
    }

    @Handles("progression.event.edit-form-requested")
    public void formEditRequested(final JsonEnvelope event) {
        LOGGER.info("progression.event.edit-form-requested event received: {}", event);
        final JsonObject editFormRequestedEventPayload = event.payloadAsJsonObject();
        final JsonObject lockStatus = editFormRequestedEventPayload.getJsonObject(LOCK_STATUS);
        final boolean isLocked = lockStatus.getBoolean(IS_LOCKED);
        JsonObject userDetailsPayload = null;
        if (isLocked) {
            userDetailsPayload = getUserDetailsAsAdmin(event, lockStatus.getString(LOCKED_BY));
        }
        final JsonObject publicEventPayload = buildPublicEvent(editFormRequestedEventPayload, userDetailsPayload);
        sender.send(envelopeFrom(
                metadataFrom(event.metadata()).withName(PUBLIC_PROGRESSION_EDIT_FORM_REQUESTED),
                publicEventPayload
        ));
    }


    @Handles("progression.event.form-finalised")
    public void formFinalised(final JsonEnvelope event) {
        LOGGER.info("progression.event.form-finalised event received with courtFormId: {} for case: {}", event.payloadAsJsonObject().getString(COURT_FORM_ID), event.payloadAsJsonObject().getString(CASE_ID));
        final JsonObject formFinalised = event.payloadAsJsonObject();

        final UUID courtFormId = fromString(formFinalised.getString(COURT_FORM_ID));
        final UUID caseId = fromString(formFinalised.getString(CASE_ID));
        final String caseURN = formFinalised.getString(CASE_URN, null);
        final String hearingDateTime = formFinalised.getString(HEARING_DATE_TIME, null);
        final FormType formType = FormType.valueOf(formFinalised.getString(FORM_TYPE));
        final String submissionId = formFinalised.getString(SUBMISSION_ID, null);
        final UUID materialId = fromString(formFinalised.getString(MATERIAL_ID));

        final JsonArray formArray = formFinalised.getJsonArray(FINALISED_FORM_DATA);

        if (isNull(formArray) || formArray.isEmpty()) {
            LOGGER.error("No finalised data found for forRefId {} and caseId {} in api", courtFormId, caseId);
            return;
        }


        final JsonArrayBuilder documentMetadataArrayBuilder = createArrayBuilder();
        formArray.forEach(formDataPerDefendant ->
                documentMetadataArrayBuilder.add(processFinalisedFormData(event, formType, formDataPerDefendant, courtFormId, caseId, materialId)));

        final JsonObjectBuilder payload = createObjectBuilder()
                .add(COURT_FORM_ID, courtFormId.toString())
                .add(FORM_TYPE, formType.name())
                .add(CASE_ID, caseId.toString())
                .add(UPDATED_BY, getUpdatedBy(event, formFinalised.getString(USER_ID, null), formFinalised.getString(USER_NAME, null)))
                .add(CASE_URN, nonNull(caseURN) ? caseURN : "")
                .add(DOCUMENT_META_DATA, documentMetadataArrayBuilder.build());

        if (isNotEmpty(hearingDateTime)) {
            payload.add(HEARING_DATE_TIME, hearingDateTime);
        }
        if (isNotEmpty(submissionId)) {
            payload.add(SUBMISSION_ID, submissionId);
        }
        sender.send(envelopeFrom(
                metadataFrom(event.metadata()).withName(PUBLIC_PROGRESSION_FORM_FINALISED),
                payload
        ));
    }

    private JsonObject buildPublicEvent(final JsonObject editFormRequestedEventPayload, final JsonObject userDetailsPayload) {
        final JsonObjectBuilder publicEventBuilder = createObjectBuilder()
                .add(COURT_FORM_ID, editFormRequestedEventPayload.getString(COURT_FORM_ID))
                .add(CASE_ID, editFormRequestedEventPayload.getString(CASE_ID));
        final JsonObject privateEventLockStatus = editFormRequestedEventPayload.getJsonObject(LOCK_STATUS);
        final JsonObjectBuilder lockStatusBuilder = createObjectBuilder().add(IS_LOCKED, privateEventLockStatus.getBoolean(IS_LOCKED));
        final JsonObjectBuilder lockedBy = createObjectBuilder();

        if (isNotEmpty(privateEventLockStatus.getString(EXPIRY_TIME, ""))) {
            lockStatusBuilder.add(EXPIRY_TIME, privateEventLockStatus.getString(EXPIRY_TIME));
        }

        if (isNotEmpty(privateEventLockStatus.getString(LOCK_REQUESTED_BY, ""))) {
            lockStatusBuilder.add(LOCK_REQUESTED_BY, createObjectBuilder().add(USER_ID, privateEventLockStatus.getString(LOCK_REQUESTED_BY)));
        }

        if (isNotEmpty(privateEventLockStatus.getString(LOCKED_BY, ""))) {
            lockedBy.add(USER_ID, privateEventLockStatus.getString(LOCKED_BY));
        }

        if (nonNull(userDetailsPayload)) {
            lockedBy.add(FIRST_NAME, userDetailsPayload.getString(FIRST_NAME))
                    .add(LAST_NAME, userDetailsPayload.getString(LAST_NAME))
                    .add(EMAIL, userDetailsPayload.getString(EMAIL));
        }
        lockStatusBuilder.add(LOCKED_BY, lockedBy.build());

        return publicEventBuilder.add(LOCK_STATUS, lockStatusBuilder.build()).build();
    }

    private JsonObject getUserDetailsAsAdmin(final JsonEnvelope envelope, final String userId) {
        final JsonObject getUserRequest = createObjectBuilder().add(USER_ID, userId).build();
        final Envelope<JsonObject> response = requester.requestAsAdmin(envelop(getUserRequest).withName("usersgroups.get-user-details").withMetadataFrom(envelope), JsonObject.class);
        return response.payload();
    }

    private JsonObject processFinalisedFormData(final JsonEnvelope event, final FormType formType, final JsonValue formDataPerDefendant, final UUID courtFormId, final UUID caseId, final UUID materialId) {
        final JsonObjectBuilder documentMetaDataBuilder = createObjectBuilder();
        final JsonObject documentData = stringToJsonObjectConverter.convert(((JsonString) formDataPerDefendant).getString());
        LOGGER.info("Generating Form Document courtFormId: {}, MaterialId: {}", courtFormId, materialId);
        final String filename = documentGeneratorService.generateFormDocument(event, formType, documentData, materialId);
        documentMetaDataBuilder.add(DOCUMENT_FILE_NAME, filename);
        documentMetaDataBuilder.add(IS_WELSH, documentData.getBoolean(IS_WELSH, false));

        final JsonObject jsonObject = createObjectBuilder()
                .add(MATERIAL_ID, materialId.toString())
                .add(COURT_DOCUMENT, objectToJsonObjectConverter
                        .convert(buildCourtDocument(caseId, materialId, filename, formType, documentData))).build();

        LOGGER.info("court document is being created '{}' ", jsonObject);

        sender.send(envelopeFrom(
                metadataFrom(event.metadata()).withName(PROGRESSION_COMMAND_ADD_COURT_DOCUMENT),
                jsonObject
        ));
        return documentMetaDataBuilder.add(MATERIAL_ID, materialId.toString()).build();
    }


    private CourtDocument buildCourtDocument(final UUID caseId, final UUID materialId, final String filename, final FormType formType, final JsonObject documentData) {

        final DocumentCategory.Builder categoryBuilder = DocumentCategory.documentCategory();
        final CourtDocument.Builder builder = CourtDocument.courtDocument()
                .withCourtDocumentId(randomUUID())
                .withDocumentTypeDescription(DOCUMENT_TYPE_DESCRIPTION)
                .withDocumentTypeId(CASE_DOCUMENT_TYPE_ID)
                .withMimeType(APPLICATION_PDF)
                .withName(filename);

        if (FormType.BCM.equals(formType)) {
            if (nonNull(documentData.getString(DEFENDANT_ID, null))) {
                categoryBuilder.withDefendantDocument(defendantDocument()
                        .withDefendants(singletonList(fromString(documentData.getString(DEFENDANT_ID))))
                        .withProsecutionCaseId(caseId)
                        .build());
            } else {
                LOGGER.error("defendantId is not present for BCM form finalised, caseId {}", caseId);
            }

            builder.withSendToCps(true).withNotificationType("bcm-form-finalised");
        } else if (FormType.PTPH.equals(formType)) {
            if (isNotEmpty(documentData.getJsonArray(DEFENDANT_IDS))) {
                final List<UUID> defendantIdList = documentData.getJsonArray(DEFENDANT_IDS).getValuesAs(JsonString.class).stream()
                        .map(x -> UUID.fromString(x.getString()))
                        .collect(toList());
                categoryBuilder.withDefendantDocument(defendantDocument()
                        .withDefendants(defendantIdList)
                        .withProsecutionCaseId(caseId)
                        .build());
            } else {
                LOGGER.error("defendantId is not present for PTPH form finalised, caseId {}", caseId);
            }
        } else {
            categoryBuilder.withCaseDocument(caseDocument()
                    .withProsecutionCaseId(caseId)
                    .build());
        }

        final Material material = Material.material().withId(materialId)
                .withUploadDateTime(ZonedDateTime.now())
                .build();

        builder.withDocumentCategory(categoryBuilder.build())
                .withMaterials(singletonList(material));

        return builder.build();
    }

    @Handles("progression.event.form-defendants-updated")
    public void bcmDefendantsUpdated(final JsonEnvelope event) {
        LOGGER.info("progression.event.form-operation-failed event received: {}", event);
        sender.send(envelopeFrom(
                metadataFrom(event.metadata()).withName(PUBLIC_PROGRESSION_BCM_DEFENDANTS_UPDATED),
                event.payload()
        ));
    }

    private JsonObject getUpdatedBy(final JsonEnvelope event, final String userId, final String userName) {
        final JsonObjectBuilder builder = createObjectBuilder();

        if (isNotEmpty(userName)) {
            builder.add("name", userName);
            return builder.build();
        }

        final JsonObject userDetails = usersGroupService.getUserById(event, userId);
        return builder
                .add(ID, userId)
                .add(FIRST_NAME, userDetails.getString(FIRST_NAME))
                .add(LAST_NAME, userDetails.getString(LAST_NAME))
                .build();
    }

    @Handles("public.prosecutioncasefile.cps-serve-bcm-submitted")
    public void handleServeFormSubmittedPublicEvent(final JsonEnvelope envelope) {

        LOGGER.info("prosecutioncasefile.event.cps-serve-bcm-submitted");

        final JsonObject payload = envelope.payloadAsJsonObject();

        final JsonArrayBuilder defendantIdArray = JsonObjects.createArrayBuilder();
        final List<JsonObject> formDefendantList = payload.getJsonArray(FORM_DEFENDANTS).getValuesAs(JsonObject.class);
        formDefendantList.forEach(defendant -> defendantIdArray.add(JsonObjects.createObjectBuilder()
                        .add(DEFENDANT_ID, defendant.getString(DEFENDANT_ID))
                        .build()
                )
        );

        final JsonObject createPetFormPayload = JsonObjects.createObjectBuilder().add(CASE_ID, payload.get(CASE_ID))
                .add(SUBMISSION_ID, payload.getString(SUBMISSION_ID))
                .add(COURT_FORM_ID, String.valueOf(randomUUID()))
                .add(FORM_DEFENDANTS, defendantIdArray.build())
                .add(FORM_DATA, appendDataElement(payload.getString(FORM_DATA)))
                .add(FORM_TYPE, BCM)
                .add(FORM_ID, String.valueOf(randomUUID()))
                .add(USER_NAME, CMS_USER)
                .build();
        this.sender.send(Envelope.envelopeFrom(metadataFrom(envelope.metadata())
                .withName("progression.command.create-form")
                .build(), createPetFormPayload));

        if(isNotEmpty(formDefendantList)){
            formDefendantList.forEach( defendant -> updateCpsDefendantId(envelope, payload.getString(CASE_ID), defendant));
        }

        LOGGER.info("prosecutioncasefile.event.cps-serve-bcm-submitted");

    }

    @Handles("public.prosecutioncasefile.cps-serve-ptph-submitted")
    public void handleServePtphFormSubmittedPublicEvent(final JsonEnvelope envelope) {

        LOGGER.info("prosecutioncasefile.event.cps-serve-ptph-submitted");

        final JsonObject payload = envelope.payloadAsJsonObject();

        final JsonArrayBuilder defendantIdArray = JsonObjects.createArrayBuilder();
        final List<JsonObject> formDefendantList = payload.getJsonArray(FORM_DEFENDANTS).getValuesAs(JsonObject.class);
        formDefendantList.forEach(defendant -> defendantIdArray.add(JsonObjects.createObjectBuilder()
                        .add(DEFENDANT_ID, defendant.getString(DEFENDANT_ID))
                        .build()
                )
        );

        final JsonObject createPetFormPayload = JsonObjects.createObjectBuilder().add(CASE_ID, payload.get(CASE_ID))
                .add(SUBMISSION_ID, payload.getString(SUBMISSION_ID))
                .add(COURT_FORM_ID, String.valueOf(randomUUID()))
                .add(FORM_DEFENDANTS, defendantIdArray.build())
                .add(FORM_DATA, appendDataElement(payload.getString(FORM_DATA)))
                .add(FORM_TYPE, PTPH)
                .add(USER_NAME, payload.getString(USER_NAME))
                .add(FORM_ID, String.valueOf(randomUUID()))
                .build();
        this.sender.send(Envelope.envelopeFrom(metadataFrom(envelope.metadata())
                .withName("progression.command.create-form")
                .build(), createPetFormPayload));

        if(isNotEmpty(formDefendantList)){
            formDefendantList.forEach( defendant -> updateCpsDefendantId(envelope, payload.getString(CASE_ID), defendant));
        }

        LOGGER.info("prosecutioncasefile.event.cps-serve-ptph-submitted");

    }

    private String appendDataElement(final String formData) {
        final StringBuilder data = new StringBuilder();
        data.append("{\"data\":");
        data.append(formData);
        data.append("}");
        return data.toString();
    }

    private void updateCpsDefendantId(final JsonEnvelope envelope, final String caseId, final JsonObject defendant) {
        if (isNotEmpty(defendant.getString(CPS_DEFENDANT_ID, null))) {
            final String defendantId = defendant.getString(DEFENDANT_ID);
            final String cpsDefendantId = defendant.getString(CPS_DEFENDANT_ID);
            LOGGER.info("updating defendant {} with cpsDefendantId {} in case {}", defendantId, cpsDefendantId, caseId);
            final UpdateCpsDefendantId updateCpsDefendantId = UpdateCpsDefendantId.updateCpsDefendantId()
                    .withCpsDefendantId(cpsDefendantId)
                    .withCaseId(fromString(caseId))
                    .withDefendantId(fromString(defendantId))
                    .build();

            sender.send(Envelope.envelopeFrom(metadataFrom(envelope.metadata()).withName("progression.command.update-cps-defendant-id").build(), updateCpsDefendantId));
        }
    }
}
