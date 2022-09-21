package uk.gov.moj.cpp.progression.cotr;

import static java.util.Comparator.comparing;
import static java.util.Objects.nonNull;
import static java.util.Optional.ofNullable;
import static java.util.UUID.fromString;
import static java.util.UUID.randomUUID;
import static java.util.stream.Collectors.toList;
import static javax.json.Json.createArrayBuilder;
import static javax.json.Json.createObjectBuilder;
import static org.apache.commons.collections.CollectionUtils.isNotEmpty;
import static uk.gov.justice.services.core.annotation.Component.EVENT_PROCESSOR;
import static uk.gov.justice.services.core.enveloper.Enveloper.envelop;
import static uk.gov.justice.services.messaging.Envelope.metadataFrom;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;

import uk.gov.justice.core.courts.CotrPdfContent;
import uk.gov.justice.core.courts.CourtDocument;
import uk.gov.justice.core.courts.DefendantCotrServed;
import uk.gov.justice.core.courts.DefendantDocument;
import uk.gov.justice.core.courts.DocumentCategory;
import uk.gov.justice.core.courts.HearingDay;
import uk.gov.justice.core.courts.Material;
import uk.gov.justice.core.courts.RequestCotrTask;
import uk.gov.justice.cpp.progression.event.CotrCreated;
import uk.gov.justice.cpp.progression.event.ProsecutionCotrServed;
import uk.gov.justice.cpp.progression.event.ProsecutionCotrUpdated;
import uk.gov.justice.progression.courts.Hearings;
import uk.gov.justice.progression.event.DefendantAddedToCotr;
import uk.gov.justice.progression.event.DefendantRemovedFromCotr;
import uk.gov.justice.progression.event.FurtherInfoForDefenceCotrAdded;
import uk.gov.justice.progression.event.FurtherInfoForProsecutionCotrAdded;
import uk.gov.justice.progression.event.ReviewNotesUpdated;
import uk.gov.justice.progression.query.CotrDetail;
import uk.gov.justice.referencedata.query.ReferenceDataCotrReviewNotes;
import uk.gov.justice.referencedata.query.ReviewNotes;
import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.common.converter.ObjectToJsonObjectConverter;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.requester.Requester;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.messaging.Metadata;
import uk.gov.moj.cpp.progression.json.schemas.event.CotrTaskRequested;
import uk.gov.moj.cpp.progression.service.DocumentGeneratorService;
import uk.gov.moj.cpp.progression.service.RefDataService;
import uk.gov.moj.cpp.progression.service.UsersGroupService;

import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.json.JsonValue;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ServiceComponent(EVENT_PROCESSOR)

@SuppressWarnings({"squid:S1155"})
public class CotrEventsProcessor {

    public static final String SUBMISSION_ID = "submissionId";
    public static final String LAST_RECORDED_TIME_ESTIMATE = "lastRecordedTimeEstimate";
    public static final String HAS_ALL_EVIDENCE_TO_BE_RELIED_ON_BEEN_SERVED = "hasAllEvidenceToBeReliedOnBeenServed";
    public static final String HAS_ALL_DISCLOSURE_BEEN_PROVIDED = "hasAllDisclosureBeenProvided";
    public static final String HAVE_OTHER_DIRECTIONS_BEEN_COMPLIED_WITH = "haveOtherDirectionsBeenCompliedWith";
    public static final String HAVE_THE_PROSECUTION_WITNESSES_REQUIRED_TO_ATTEND_ACKNOWLEDGED_THAT_THEY_WILL_ATTEND = "haveTheProsecutionWitnessesRequiredToAttendAcknowledgedThatTheyWillAttend";
    public static final String HEARING_ID = "hearingId";

    public static final String HEARING_ID_NOT_FOUND = "HEARING_ID_NOT_FOUND";
    public static final String COTR_ID_NOT_FOUND = "COTR_ID_NOT_FOUND";

    public static final String HAVE_ANY_WITNESS_SUMMONSES_REQUIRED_BEEN_RECEIVED_AND_SERVED = "haveAnyWitnessSummonsesRequiredBeenReceivedAndServed";
    public static final String HAS_ALL_EVIDENCE_TO_BE_RELIED_ON_BEEN_SERVED_DETAILS = "hasAllEvidenceToBeReliedOnBeenServedDetails";
    public static final String HAS_ALL_DISCLOSURE_BEEN_PROVIDED_DETAILS = "hasAllDisclosureBeenProvidedDetails";
    public static final String HAVE_OTHER_DIRECTIONS_BEEN_COMPLIED_WITH_DETAILS = "haveOtherDirectionsBeenCompliedWithDetails";
    public static final String HAVE_THE_PROSECUTION_WITNESSES_REQUIRED_TO_ATTEND_ACKNOWLEDGED_THAT_THEY_WILL_ATTEND_DETAILS = "haveTheProsecutionWitnessesRequiredToAttendAcknowledgedThatTheyWillAttendDetails";
    public static final String HAVE_ANY_WITNESS_SUMMONSES_REQUIRED_BEEN_RECEIVED_AND_SERVED_DETAILS = "haveAnyWitnessSummonsesRequiredBeenReceivedAndServedDetails";
    public static final String HAVE_SPECIAL_MEASURES_OR_REMOTE_ATTENDANCE_ISSUES_FOR_WITNESSES_BEEN_RESOLVED_DETAILS = "haveSpecialMeasuresOrRemoteAttendanceIssuesForWitnessesBeenResolvedDetails";
    public static final String HAVE_SPECIAL_MEASURES_OR_REMOTE_ATTENDANCE_ISSUES_FOR_WITNESSES_BEEN_RESOLVED = "haveSpecialMeasuresOrRemoteAttendanceIssuesForWitnessesBeenResolved";
    public static final String HAVE_INTERPRETERS_FOR_WITNESSES_BEEN_ARRANGED = "haveInterpretersForWitnessesBeenArranged";
    public static final String HAVE_EDITED_ABE_INTERVIEWS_BEEN_PREPARED_AND_AGREED = "haveEditedAbeInterviewsBeenPreparedAndAgreed";
    public static final String HAVE_ARRANGEMENTS_BEEN_MADE_FOR_STATEMENT_OF_POINTS_OF_AGREEMENT_AND_DISAGREEMENT = "haveArrangementsBeenMadeForStatementOfPointsOfAgreementAndDisagreement";
    public static final String IS_THE_CASE_READY_TO_PROCEED_WITHOUT_DELAY_BEFORE_THE_JURY = "isTheCaseReadyToProceedWithoutDelayBeforeTheJury";
    public static final String IS_THE_TIME_ESTIMATE_CORRECT = "isTheTimeEstimateCorrect";
    public static final String CERTIFY_THAT_THE_PROSECUTION_IS_TRIAL_READY = "certifyThatTheProsecutionIsTrialReady";
    public static final String APPLY_FOR_THE_PTR_TO_BE_VACATED = "applyForThePtrToBeVacated";
    public static final String FORM_COMPLETED_ON_BEHALF_OF_THE_PROSECUTION_BY = "formCompletedOnBehalfOfTheProsecutionBy";
    public static final String CERTIFICATION_DATE = "certificationDate";
    public static final String FURTHER_INFORMATION_TO_ASSIST_THE_COURT = "furtherInformationToAssistTheCourt";
    public static final String HAVE_INTERPRETERS_FOR_WITNESSES_BEEN_ARRANGED_DETAILS = "haveInterpretersForWitnessesBeenArrangedDetails";
    public static final String HAVE_EDITED_ABE_INTERVIEWS_BEEN_PREPARED_AND_AGREED_DETAILS = "haveEditedAbeInterviewsBeenPreparedAndAgreedDetails";
    public static final String HAVE_ARRANGEMENTS_BEEN_MADE_FOR_STATEMENT_OF_POINTS_OF_AGREEMENT_AND_DISAGREEMENT_DETAILS = "haveArrangementsBeenMadeForStatementOfPointsOfAgreementAndDisagreementDetails";
    public static final String IS_THE_CASE_READY_TO_PROCEED_WITHOUT_DELAY_BEFORE_THE_JURY_DETAILS = "isTheCaseReadyToProceedWithoutDelayBeforeTheJuryDetails";
    public static final String IS_THE_TIME_ESTIMATE_CORRECT_DETAILS = "isTheTimeEstimateCorrectDetails";
    public static final String CERTIFY_THAT_THE_PROSECUTION_IS_TRIAL_READY_DETAILS = "certifyThatTheProsecutionIsTrialReadyDetails";
    public static final String APPLY_FOR_THE_PTR_TO_BE_VACATED_DETAILS = "applyForThePtrToBeVacatedDetails";
    public static final String FORM_DEFENDANTS = "formDefendants";
    public static final String TRIAL_DATE = "trialDate";
    public static final String FORM_COMPLETED_ON_BEHALF_OF_PROSECUTION_BY = "formCompletedOnBehalfOfProsecutionBy";
    public static final String FURTHER_PROSECUTION_INFORMATION_PROVIDED_AFTER_CERTIFICATION = "furtherProsecutionInformationProvidedAfterCertification";
    public static final String ANSWER = "answer";
    public static final String DETAILS = "details";
    public static final String HEARINGS = "hearings";
    public static final String COTR_DETAILS = "cotrDetails";
    public static final String MESSAGE = "message";
    private static final Logger LOGGER = LoggerFactory.getLogger(CotrEventsProcessor.class);
    private static final String COTR_ID = "cotrId";
    private static final String CASE_ID = "caseId";
    private static final String CASE_URN = "caseUrn";
    private static final String JURISDICTION_TYPE = "jurisdictionType";
    private static final String COURT_CENTER = "courtCenter";
    private static final String DEFENDANT_IDS = "defendantIds";
    private static final String HEARING_DATE = "hearingDate";
    private static final String MATERIAL_ID = "materialId";
    private static final String DEFENDANT_ID = "defendantId";
    private static final String RECEIVED_EVENT_WITH_PAYLOAD = "Received '{}' event with payload {}";
    private static final String COTR_DOCUMENT_TEMPLATE_NAME = "ServeDefendantCotr";
    private static final String COURT_DOCUMENT = "courtDocument";
    private static final String PROGRESSION_COMMAND_ADD_COURT_DOCUMENT = "progression.command.add-court-document";
    private static final UUID CASE_DOCUMENT_TYPE_ID = fromString("6b9df1fb-7bce-4e33-88dd-db91f75adeb8");
    private static final String APPLICATION_PDF = "application/pdf";
    private static final String DOCUMENT_TYPE_DESCRIPTION = "Case Management";
    private static final String PROGRESSION_COMMAND_CREATE_COTR = "progression.command.create-cotr";
    private static final String PROGRESSION_COMMAND_SERVE_COTR = "progression.command.serve-prosecution-cotr";
    private static final String PROGRESSION_OPERATION_FAILED = "public.progression.cotr-operation-failed";
    private static final String PROGRESSION_COMMAND_UPDATE_PROSECUTION_COTR = "progression.command.update-prosecution-cotr";
    private static final String PROGRESSION_QUERY_CASE_HEARINGS = "progression.query.casehearings";
    private static final String PROGRESSION_QUERY_COTR_DETAILS_PROSECUTION_CASE = "progression.query.cotr.details.prosecutioncase";
    public static final String UPDATE_COTR_FORM = "update-cotr-form";
    public static final String OPERATION = "operation";
    public static final String CREATE_COTR_FORM = "create-cotr-form";
    public static final String PROSECUTION_CASE_ID = "prosecutionCaseId";
    public static final String Y = "Y";
    public static final String N = "N";
    public static final String YES = "Yes";
    public static final String NO = "No";

    @Inject
    private Requester requester;
    @Inject
    private Sender sender;
    @Inject
    private DocumentGeneratorService documentGeneratorService;
    @Inject
    private ObjectToJsonObjectConverter objectToJsonObjectConverter;
    @Inject
    private JsonObjectToObjectConverter jsonObjectToObjectConverter;
    @Inject
    private RefDataService referenceDataService;
    @Inject
    private UsersGroupService usersGroupService;

    private static Optional<Map.Entry<UUID, ZonedDateTime>> getRecentHearing(List<Hearings> nonResultedHearing) {
        final Map<UUID, ZonedDateTime> hearingDaysMap = new HashMap<>();
        for (final Hearings hearings : nonResultedHearing) {
            if (!hearings.getHearingDays().isEmpty()) {
                hearingDaysMap.put(hearings.getId(), getRecenttDate(hearings.getHearingDays()));
            }
        }
        return hearingDaysMap.entrySet()
                .stream().max(Map.Entry.comparingByValue());
    }

    @SuppressWarnings("squid:S3655")
    private static ZonedDateTime getRecenttDate(final List<HearingDay> hearingDays) {
        return hearingDays.stream()
                .max(comparing(HearingDay::getSittingDay))
                .get()
                .getSittingDay();
    }

    @Handles("progression.event.cotr-created")
    public void cotrCreated(final Envelope<CotrCreated> event) {
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug(RECEIVED_EVENT_WITH_PAYLOAD, "progression.event.cotr-created", event.payload().getCotrId());
        }
        final JsonObject jsonObject = buildPayload(event);

        sender.send(
                envelop(jsonObject)
                        .withName("public.progression.cotr-created")
                        .withMetadataFrom(event));
    }

    private JsonObject buildPayload(final Envelope<CotrCreated> event) {

        final JsonObjectBuilder eventPayload = Json.createObjectBuilder()
                .add(COTR_ID, event.payload().getCotrId().toString());

        if (nonNull(event.payload().getSubmissionId())) {
            eventPayload.add(SUBMISSION_ID, event.payload().getSubmissionId().toString());
        }

        return eventPayload.build();
    }

    @Handles("progression.event.prosecution-cotr-served")
    public void serveProsecutionCotr(final Envelope<ProsecutionCotrServed> event) {
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug(RECEIVED_EVENT_WITH_PAYLOAD, "progression.event.prosecution-cotr-served", event.payload().getCotrId());
        }

        final JsonObject eventPayload = createObjectBuilder()
                .add(COTR_ID, event.payload().getCotrId().toString())
                .add(SUBMISSION_ID, event.payload().getSubmissionId().toString())
                .build();

        sender.send(
                envelop(eventPayload)
                        .withName("public.progression.prosecution-cotr-served")
                        .withMetadataFrom(event));

    }

    @Handles("progression.event.cotr-archived")
    public void archiveCotr(final JsonEnvelope event) {
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug(RECEIVED_EVENT_WITH_PAYLOAD, "progression.event.cotr-archived", event.payload());
        }
        final JsonObject privateEventPayload = event.payloadAsJsonObject();
        final JsonObject publicEventPayload = createObjectBuilder()
                .add(COTR_ID, privateEventPayload.getString(COTR_ID))
                .build();

        sender.send(
                envelop(publicEventPayload)
                        .withName("public.progression.cotr-archived")
                        .withMetadataFrom(event));
    }

    @Handles("progression.event.cotr-task-requested")
    public void cotrTaskRequested(final Envelope<CotrTaskRequested> event) {
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug(RECEIVED_EVENT_WITH_PAYLOAD, "progression.cotr-task-requested", event.payload());
        }
        final UUID organisationId = usersGroupService.getOrganisationByType(event.metadata());
        final CotrTaskRequested cotrTaskRequested = CotrTaskRequested.cotrTaskRequested()
                .withValuesFrom(event.payload()).withOrganisationId(organisationId).build();
        final JsonObject jsonObject = objectToJsonObjectConverter.convert(cotrTaskRequested);
        sender.send(
                envelop(jsonObject)
                        .withName("public.progression.cotr-task-requested")
                        .withMetadataFrom(event));
    }

    @Handles("progression.event.defendant-cotr-served")
    public void defendantCotrServed(final Envelope<DefendantCotrServed> event) {
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug(RECEIVED_EVENT_WITH_PAYLOAD, "progression.event.defendant-cotr-served", event.payload().getCotrId());
        }

        this.uploadCotrPdfToCdes(event.metadata(), event.payload().getCaseId(), event.payload().getDefendantId(), event.payload().getPdfContent(), event.payload().getTrailDate(), event.payload().getIsWelshForm());

        final JsonObject eventPayload = createObjectBuilder()
                .add(COTR_ID, event.payload().getCotrId().toString())
                .build();

        sender.send(
                envelop(eventPayload)
                        .withName("public.progression.serve-defendant-cotr")
                        .withMetadataFrom(event));
    }

    private void uploadCotrPdfToCdes(final Metadata metadata, UUID caseId, UUID defendantId, CotrPdfContent cotrPdfContent, String trialDate, Boolean isWelsh) {
        final UUID materialId = randomUUID();

        final JsonObject documentData = objectToJsonObjectConverter.convert(cotrPdfContent);
        final JsonEnvelope requestEnvelope = envelopeFrom(
                metadataFrom(metadata),
                createObjectBuilder()
                        .add(CASE_ID, caseId.toString())
        );

        final String pdfFileName = String.format("%s %s, %s%s", "COTR", trialDate, cotrPdfContent.getSubHeading(), isWelsh != null && isWelsh ? "(welsh)" : "");

        final String filename = documentGeneratorService.generateCotrDocument(requestEnvelope, documentData, COTR_DOCUMENT_TEMPLATE_NAME, materialId, pdfFileName);

        final JsonObject jsonObject = createObjectBuilder()
                .add(MATERIAL_ID, materialId.toString())
                .add(COURT_DOCUMENT, objectToJsonObjectConverter
                        .convert(buildCourtDocument(caseId, materialId, defendantId, filename))).build();

        LOGGER.info("court document is being created '{}' ", jsonObject);

        sender.send(envelopeFrom(
                metadataFrom(metadata).withName(PROGRESSION_COMMAND_ADD_COURT_DOCUMENT),
                jsonObject
        ));
    }

    private CourtDocument buildCourtDocument(final UUID caseId, final UUID materialId, final UUID defendantId, final String filename) {

        final DocumentCategory documentCategory = DocumentCategory.documentCategory()
                .withDefendantDocument(DefendantDocument.defendantDocument()
                        .withDefendants(Arrays.asList(defendantId))
                        .withProsecutionCaseId(caseId)
                        .build())
                .build();

        final Material material = Material.material().withId(materialId)
                .withUploadDateTime(ZonedDateTime.now())
                .build();

        return CourtDocument.courtDocument()
                .withCourtDocumentId(randomUUID())
                .withDocumentCategory(documentCategory)
                .withDocumentTypeDescription(DOCUMENT_TYPE_DESCRIPTION)
                .withDocumentTypeId(CASE_DOCUMENT_TYPE_ID)
                .withMimeType(APPLICATION_PDF)
                .withName(filename)
                .withMaterials(Collections.singletonList(material))
                .withNotificationType("cotr-form-served")
                .withSendToCps(true)
                .build();
    }

    @Handles("progression.event.defendant-added-to-cotr")
    public void handleDefendantAddedToCotrEvent(final Envelope<DefendantAddedToCotr> event) {
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug(RECEIVED_EVENT_WITH_PAYLOAD, "progression.event.defendant-added-to-cotr", event.payload().getCotrId());
        }

        final JsonObject eventPayload = createObjectBuilder()
                .add(COTR_ID, event.payload().getCotrId().toString())
                .add(DEFENDANT_ID, event.payload().getDefendantId().toString())
                .build();

        sender.send(
                envelop(eventPayload)
                        .withName("public.progression.defendants-changed-in-cotr")
                        .withMetadataFrom(event));
    }

    @Handles("progression.event.defendant-removed-from-cotr")
    public void handleDefendantRemovedFromCotrEvent(final Envelope<DefendantRemovedFromCotr> event) {
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug(RECEIVED_EVENT_WITH_PAYLOAD, "progression.event.defendant-removed-from-cotr", event.payload().getCotrId());
        }

        final JsonObject eventPayload = createObjectBuilder()
                .add(COTR_ID, event.payload().getCotrId().toString())
                .add(DEFENDANT_ID, event.payload().getDefendantId().toString())
                .build();

        sender.send(
                envelop(eventPayload)
                        .withName("public.progression.defendants-changed-in-cotr")
                        .withMetadataFrom(event));
    }

    @Handles("progression.event.further-info-for-prosecution-cotr-added")
    public void handleFurtherInfoForProsecutionCotrAddedEvent(final Envelope<FurtherInfoForProsecutionCotrAdded> event) {
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug(RECEIVED_EVENT_WITH_PAYLOAD, "progression.event.further-info-for-prosecution-cotr-added", event.payload().getCotrId());
        }

        final JsonObject eventPayload = createObjectBuilder()
                .add(COTR_ID, event.payload().getCotrId().toString())
                .build();

        sender.send(
                envelop(eventPayload)
                        .withName("public.progression.further-info-for-prosecution-cotr-added")
                        .withMetadataFrom(event));
    }

    @Handles("progression.event.further-info-for-defence-cotr-added")
    public void handleFurtherInfoForDefenceCotrAddedEvent(final Envelope<FurtherInfoForDefenceCotrAdded> event) {
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug(RECEIVED_EVENT_WITH_PAYLOAD, "progression.event.further-info-for-defence-cotr-added", event.payload().getCotrId());
        }

        this.uploadCotrPdfToCdes(event.metadata(), event.payload().getCaseId(), event.payload().getDefendantId(), event.payload().getPdfContent(), event.payload().getTrailDate(), event.payload().getIsWelshForm());

        final JsonObject eventPayload = createObjectBuilder()
                .add(COTR_ID, event.payload().getCotrId().toString())
                .build();

        sender.send(
                envelop(eventPayload)
                        .withName("public.progression.further-info-for-defence-cotr-added")
                        .withMetadataFrom(event));
    }

    @Handles("progression.event.review-notes-updated")
    public void handleReviewNotesUpdateEvent(final Envelope<ReviewNotesUpdated> event) {
        final ReviewNotesUpdated reviewNotesUpdated = event.payload();
        final JsonObject eventPayload = createObjectBuilder()
                .add(COTR_ID, reviewNotesUpdated.getCotrId().toString())
                .build();

        sender.send(
                envelop(eventPayload)
                        .withName("public.progression.cotr-review-notes-updated")
                        .withMetadataFrom(event));

        final Map<UUID, ReviewNotes> reviewNotesMap = new HashMap<>();
        final Optional<JsonObject> optionalCotrReviewNotes = referenceDataService.getCotrReviewNotes(event.metadata(), requester);
        final JsonObject cotrReviewNotesRefDataResponsePayload = optionalCotrReviewNotes.orElseThrow(() -> new IllegalArgumentException("No Cotr review notes present"));
        if (cotrReviewNotesRefDataResponsePayload.equals(JsonValue.NULL)) {
            throw new IllegalArgumentException("No Cotr review notes present");
        }

        final ReferenceDataCotrReviewNotes referenceDataCotrReviewNotes = jsonObjectToObjectConverter.convert(cotrReviewNotesRefDataResponsePayload, ReferenceDataCotrReviewNotes.class);
        referenceDataCotrReviewNotes.getReviewNotes().forEach(reviewNotes ->
                reviewNotesMap.put(UUID.fromString(reviewNotes.getId()), reviewNotes)
        );

        reviewNotesUpdated.getCotrNotes().forEach(cotrNote ->
                cotrNote.getReviewNotes().stream().forEach(s -> {
                    final ReviewNotes reviewNotesReferenceData = reviewNotesMap.get(s.getId());
                    if (StringUtils.isNotEmpty(reviewNotesReferenceData.getRoles())
                            && StringUtils.isNotEmpty(reviewNotesReferenceData.getTaskName())
                            && reviewNotesReferenceData.getNumberOfDays() != null) {
                        final RequestCotrTask requestCotrTask = RequestCotrTask.requestCotrTask()
                                .withComments(s.getComment())
                                .withCotrId(event.payload().getCotrId())
                                .withNumberOfDays(reviewNotesReferenceData.getNumberOfDays())
                                .withRoles(reviewNotesReferenceData.getRoles())
                                .withTaskName(reviewNotesReferenceData.getTaskName())
                                .build();
                        sender.send(
                                envelop(requestCotrTask)
                                        .withName("progression.command.request-cotr-task")
                                        .withMetadataFrom(event));
                    }
                })
        );
    }

    private JsonObject buildCreateCotrPayload(final String courtCenterName, final String jurisdictionType, final UUID hearingId, final UUID cotrId, final JsonObject payload) {

        return createObjectBuilder().add(CASE_ID, payload.get(CASE_ID))
                .add(SUBMISSION_ID, payload.getString(SUBMISSION_ID))
                .add(COTR_ID, String.valueOf(cotrId))
                .add(HEARING_ID, String.valueOf(hearingId))
                .add(CASE_URN, payload.getString(CASE_URN))
                .add(HEARING_DATE, payload.getString(TRIAL_DATE))
                .add(JURISDICTION_TYPE, jurisdictionType)
                .add(COURT_CENTER, courtCenterName)
                .add(DEFENDANT_IDS, getDefendants(payload))
                .build();

    }

    @Handles("public.prosecutioncasefile.cps-serve-cotr-submitted")
    public void handleServeCotrReceivedPublicEvent(final JsonEnvelope envelope) {

        LOGGER.info("public.prosecutioncasefile.cps-serve-cotr-submitted");
        final UUID cotrId = randomUUID();
        final JsonObject payload = envelope.payloadAsJsonObject();
        final Optional<Hearings> hearings = getCaseHearing(envelope, payload);

        if (hearings.isPresent()) {
            final String courtCenterName = hearings.get().getCourtCentre().getName();
            final String jurisdictionType = String.valueOf(hearings.get().getJurisdictionType());
            final UUID hearingId = hearings.get().getId();

            LOGGER.info(PROGRESSION_COMMAND_CREATE_COTR);

            final JsonObject creatCotrPayload = buildCreateCotrPayload(courtCenterName, jurisdictionType, hearingId, cotrId, payload);

            this.sender.send(Envelope.envelopeFrom(metadataFrom(envelope.metadata())
                    .withName(PROGRESSION_COMMAND_CREATE_COTR)
                    .build(), creatCotrPayload));

            LOGGER.info(PROGRESSION_COMMAND_SERVE_COTR);
            final JsonObject serveCotrPayload = buildServeCotrPayload(hearingId, cotrId, payload);
            LOGGER.info("serveCotrPayload - {}", serveCotrPayload);

            this.sender.send(Envelope.envelopeFrom(metadataFrom(envelope.metadata())
                    .withName(PROGRESSION_COMMAND_SERVE_COTR)
                    .build(), serveCotrPayload));
        } else {
            sendOperationFailed(envelope, payload, HEARING_ID_NOT_FOUND, CREATE_COTR_FORM);
        }
    }

    private Optional<Hearings> getCaseHearing(final JsonEnvelope envelope, final JsonObject payload) {
        List<Hearings> hearingList;
        final Optional<JsonObject> caseHearingsResponse = this.getCaseHearings(payload.getString(CASE_ID), envelope);
        if (caseHearingsResponse.isPresent()) {
            hearingList = caseHearingsResponse.get().getJsonArray(HEARINGS).
                    getValuesAs(JsonObject.class).stream().map(hearing ->
                            jsonObjectToObjectConverter.convert(hearing, Hearings.class)).
                    collect(Collectors.toList());

            final Optional<Map.Entry<UUID, ZonedDateTime>> recentHearing = getRecentHearing(hearingList);

            if (recentHearing.isPresent()) {
                LOGGER.info("Found resulted hearing {} with recent  date : {}", recentHearing.get().getKey(), recentHearing.get().getValue());
                return hearingList.stream().filter(hearings1 -> hearings1.getId().equals(recentHearing.get().getKey())).findFirst();
            }
        }
        return Optional.empty();
    }

    public Optional<JsonObject> getCaseHearings(final String caseId, final JsonEnvelope event) {
        final JsonObject payload = Json.createObjectBuilder().add(CASE_ID, caseId).build();
        final JsonObject caseHearings = requester.request(envelop(payload)
                .withName(PROGRESSION_QUERY_CASE_HEARINGS)
                .withMetadataFrom(event)).payloadAsJsonObject();

        return ofNullable(caseHearings);
    }

    private JsonArray getDefendants(final JsonObject payload) {
        final JsonArrayBuilder defendantArrayBuilder = createArrayBuilder();

        if (!(payload.getJsonArray(FORM_DEFENDANTS).isEmpty()) && payload.getJsonArray(FORM_DEFENDANTS).size() > 0) {
            final List<UUID> defendantIdList = payload.getJsonArray(FORM_DEFENDANTS).getValuesAs(JsonObject.class).stream()
                    .map(defendant -> fromString(defendant.getString(DEFENDANT_ID))).collect(toList());

            for (final UUID defendantId : defendantIdList) {
                defendantArrayBuilder.add(String.valueOf(defendantId));
            }
        }
        return defendantArrayBuilder.build();
    }

    private JsonObject buildServeCotrPayload(final UUID hearingId, final UUID cotrId, final JsonObject payload) {

        return createObjectBuilder()
                .add(COTR_ID, String.valueOf(cotrId))
                .add(HEARING_ID, String.valueOf(hearingId))
                .add(SUBMISSION_ID, payload.getString(SUBMISSION_ID))
                .add(LAST_RECORDED_TIME_ESTIMATE, payload.getInt(LAST_RECORDED_TIME_ESTIMATE))
                .add(HAS_ALL_EVIDENCE_TO_BE_RELIED_ON_BEEN_SERVED, buildPayloadQuestions(payload.getString(HAS_ALL_EVIDENCE_TO_BE_RELIED_ON_BEEN_SERVED), payload.getString(HAS_ALL_EVIDENCE_TO_BE_RELIED_ON_BEEN_SERVED_DETAILS)))
                .add(HAS_ALL_DISCLOSURE_BEEN_PROVIDED, buildPayloadQuestions(payload.getString(HAS_ALL_DISCLOSURE_BEEN_PROVIDED), payload.getString(HAS_ALL_DISCLOSURE_BEEN_PROVIDED_DETAILS)))
                .add(HAVE_OTHER_DIRECTIONS_BEEN_COMPLIED_WITH, buildPayloadQuestions(payload.getString(HAVE_OTHER_DIRECTIONS_BEEN_COMPLIED_WITH), payload.getString(HAVE_OTHER_DIRECTIONS_BEEN_COMPLIED_WITH_DETAILS)))
                .add(HAVE_THE_PROSECUTION_WITNESSES_REQUIRED_TO_ATTEND_ACKNOWLEDGED_THAT_THEY_WILL_ATTEND, buildPayloadQuestions(payload.getString(HAVE_THE_PROSECUTION_WITNESSES_REQUIRED_TO_ATTEND_ACKNOWLEDGED_THAT_THEY_WILL_ATTEND), payload.getString(HAVE_THE_PROSECUTION_WITNESSES_REQUIRED_TO_ATTEND_ACKNOWLEDGED_THAT_THEY_WILL_ATTEND_DETAILS)))
                .add(HAVE_ANY_WITNESS_SUMMONSES_REQUIRED_BEEN_RECEIVED_AND_SERVED, buildPayloadQuestions(payload.getString(HAVE_ANY_WITNESS_SUMMONSES_REQUIRED_BEEN_RECEIVED_AND_SERVED), payload.getString(HAVE_ANY_WITNESS_SUMMONSES_REQUIRED_BEEN_RECEIVED_AND_SERVED_DETAILS)))
                .add(HAVE_SPECIAL_MEASURES_OR_REMOTE_ATTENDANCE_ISSUES_FOR_WITNESSES_BEEN_RESOLVED, buildPayloadQuestions(payload.getString(HAVE_SPECIAL_MEASURES_OR_REMOTE_ATTENDANCE_ISSUES_FOR_WITNESSES_BEEN_RESOLVED), payload.getString(HAVE_SPECIAL_MEASURES_OR_REMOTE_ATTENDANCE_ISSUES_FOR_WITNESSES_BEEN_RESOLVED_DETAILS)))
                .add(HAVE_INTERPRETERS_FOR_WITNESSES_BEEN_ARRANGED, buildPayloadQuestions(payload.getString(HAVE_INTERPRETERS_FOR_WITNESSES_BEEN_ARRANGED), payload.getString(HAVE_INTERPRETERS_FOR_WITNESSES_BEEN_ARRANGED_DETAILS)))
                .add(HAVE_EDITED_ABE_INTERVIEWS_BEEN_PREPARED_AND_AGREED, buildPayloadQuestions(payload.getString(HAVE_EDITED_ABE_INTERVIEWS_BEEN_PREPARED_AND_AGREED), payload.getString(HAVE_EDITED_ABE_INTERVIEWS_BEEN_PREPARED_AND_AGREED_DETAILS)))
                .add(HAVE_ARRANGEMENTS_BEEN_MADE_FOR_STATEMENT_OF_POINTS_OF_AGREEMENT_AND_DISAGREEMENT, buildPayloadQuestions(payload.getString(HAVE_ARRANGEMENTS_BEEN_MADE_FOR_STATEMENT_OF_POINTS_OF_AGREEMENT_AND_DISAGREEMENT), payload.getString(HAVE_ARRANGEMENTS_BEEN_MADE_FOR_STATEMENT_OF_POINTS_OF_AGREEMENT_AND_DISAGREEMENT_DETAILS)))
                .add(IS_THE_CASE_READY_TO_PROCEED_WITHOUT_DELAY_BEFORE_THE_JURY, buildPayloadQuestions(payload.getString(IS_THE_CASE_READY_TO_PROCEED_WITHOUT_DELAY_BEFORE_THE_JURY), payload.getString(IS_THE_CASE_READY_TO_PROCEED_WITHOUT_DELAY_BEFORE_THE_JURY_DETAILS)))
                .add(IS_THE_TIME_ESTIMATE_CORRECT, buildPayloadQuestions(payload.getString(IS_THE_TIME_ESTIMATE_CORRECT), payload.getString(IS_THE_TIME_ESTIMATE_CORRECT_DETAILS)))
                .add(CERTIFY_THAT_THE_PROSECUTION_IS_TRIAL_READY, buildPayloadQuestions(payload.getString(CERTIFY_THAT_THE_PROSECUTION_IS_TRIAL_READY), payload.getString(CERTIFY_THAT_THE_PROSECUTION_IS_TRIAL_READY_DETAILS)))
                .add(APPLY_FOR_THE_PTR_TO_BE_VACATED, buildPayloadQuestions(payload.getString(APPLY_FOR_THE_PTR_TO_BE_VACATED), payload.getString(APPLY_FOR_THE_PTR_TO_BE_VACATED_DETAILS)))
                .add(FORM_COMPLETED_ON_BEHALF_OF_THE_PROSECUTION_BY, buildPayloadQuestions(StringUtils.EMPTY, payload.getString(FORM_COMPLETED_ON_BEHALF_OF_THE_PROSECUTION_BY)))
                .add(CERTIFICATION_DATE, buildPayloadQuestions(StringUtils.EMPTY, payload.getString(CERTIFICATION_DATE)))
                .add(FURTHER_INFORMATION_TO_ASSIST_THE_COURT, buildPayloadQuestions(StringUtils.EMPTY, payload.getString(FURTHER_INFORMATION_TO_ASSIST_THE_COURT))).build();

    }

    private JsonObject buildPayloadQuestions(final String answer, final String details) {
        String response = StringUtils.EMPTY;

        if(nonNull(answer)) {
            if (Y.equalsIgnoreCase(answer)) {
                response = YES;
            } else if (N.equalsIgnoreCase(answer)) {
                response = NO;
            } else {
                response = answer;
            }
        }

        return createObjectBuilder().add(ANSWER, response)
                .add(DETAILS, details).build();
    }

    @Handles("public.prosecutioncasefile.cps-update-cotr-submitted")
    public void handleUpdateCotrReceivedPublicEvent(final JsonEnvelope envelope) {


        final JsonObject payload = envelope.payloadAsJsonObject();
        final String caseId = payload.getString(CASE_ID);

        final Optional<JsonObject> cotr = getCotrCaseDetails(caseId, envelope);
        if (cotr.isPresent()) {
            final List<CotrDetail> cotrList = cotr.get().getJsonArray(COTR_DETAILS).
                    getValuesAs(JsonObject.class).stream().map(cotrDetails ->
                            jsonObjectToObjectConverter.convert(cotrDetails, CotrDetail.class))
                    .collect(Collectors.toList());
            final Optional<CotrDetail> cotrDetail = getCotrDetail(cotrList);
            if (cotrDetail.isPresent()) {
                final JsonObject updateCotRFormPayload = buildUpdateCotr(payload, cotrDetail.get());
                this.sender.send(Envelope.envelopeFrom(metadataFrom(envelope.metadata())
                        .withName(PROGRESSION_COMMAND_UPDATE_PROSECUTION_COTR)
                        .build(), updateCotRFormPayload));
            } else {
                LOGGER.info("sendOperationFailed");
                sendOperationFailed(envelope, payload, COTR_ID_NOT_FOUND, UPDATE_COTR_FORM);
            }
        } else {
            LOGGER.info("sendOperationFailed");

            sendOperationFailed(envelope, payload, COTR_ID_NOT_FOUND, UPDATE_COTR_FORM);
        }
    }

    private void sendOperationFailed(final JsonEnvelope envelope, final JsonObject payload, final String message, final String command) {

        LOGGER.info("progression.progression.cotr-operation-failed event received: {}", payload);

        final Metadata metadata = JsonEnvelope.metadataFrom(envelope.metadata())
                .withName(PROGRESSION_OPERATION_FAILED)
                .build();

        final JsonObject cpsServeMaterialStatusUpdated = Json.createObjectBuilder().add(SUBMISSION_ID, payload.getString(SUBMISSION_ID))
                .add(CASE_ID, payload.getString(CASE_ID))
                .add(MESSAGE, message)
                .add(OPERATION, command).build();

        LOGGER.info("cpsServeMaterialStatusUpdated - {}", cpsServeMaterialStatusUpdated);

        this.sender.send(Envelope.envelopeFrom(metadata, cpsServeMaterialStatusUpdated));
    }

    @Handles("progression.event.prosecution-cotr-updated")
    public void handleEventProsecutionCotrUpdated(final Envelope<ProsecutionCotrUpdated> event) {
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug(RECEIVED_EVENT_WITH_PAYLOAD, "progression.event.prosecution-cotr-updated", event.payload().getCotrId());
        }
        final JsonObject eventPayload = createObjectBuilder()
                .add(COTR_ID, event.payload().getCotrId().toString())
                .add(HEARING_ID, event.payload().getHearingId().toString())
                .add(SUBMISSION_ID, event.payload().getSubmissionId().toString())
                .build();
        sender.send(
                envelop(eventPayload)
                        .withName("public.progression.cotr-updated")
                        .withMetadataFrom(event));
    }

    private Optional<CotrDetail> getCotrDetail(final List<CotrDetail> cotrList) {
        // Consider single cotrDetail as of now
        return (isNotEmpty(cotrList) ? Optional.of(cotrList.get(0)) : Optional.empty());
    }

    private JsonObject buildUpdateCotr(final JsonObject payload, final CotrDetail cotrDetail) {
        return Json.createObjectBuilder()
                .add(COTR_ID, String.valueOf(cotrDetail.getId()))
                .add(HEARING_ID, String.valueOf(cotrDetail.getHearingId()))
                .add(SUBMISSION_ID, payload.getString(SUBMISSION_ID))
                .add(FURTHER_PROSECUTION_INFORMATION_PROVIDED_AFTER_CERTIFICATION, buildPayloadQuestions(StringUtils.EMPTY, payload.getString(FURTHER_PROSECUTION_INFORMATION_PROVIDED_AFTER_CERTIFICATION)))
                .add(CERTIFY_THAT_THE_PROSECUTION_IS_TRIAL_READY, buildPayloadQuestions(StringUtils.EMPTY, payload.getString(CERTIFY_THAT_THE_PROSECUTION_IS_TRIAL_READY)))
                .add(FORM_COMPLETED_ON_BEHALF_OF_PROSECUTION_BY, buildPayloadQuestions(StringUtils.EMPTY, payload.getString(FORM_COMPLETED_ON_BEHALF_OF_PROSECUTION_BY)))
                .add(CERTIFICATION_DATE, buildPayloadQuestions(StringUtils.EMPTY, payload.getString(CERTIFICATION_DATE)))
                .build();

    }

    public Optional<JsonObject> getCotrCaseDetails(final String caseId, final JsonEnvelope event) {
        final JsonObject payload = Json.createObjectBuilder().add(PROSECUTION_CASE_ID, caseId).build();
        final JsonObject caseHearings = requester.request(envelop(payload)
                .withName(PROGRESSION_QUERY_COTR_DETAILS_PROSECUTION_CASE)
                .withMetadataFrom(event)).payloadAsJsonObject();

        LOGGER.info("getCotrCaseDetails - caseHearings {} ", caseHearings);

        return ofNullable(caseHearings);
    }

}
