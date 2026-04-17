package uk.gov.moj.cpp.progression.processor;

import static com.google.common.collect.Lists.newArrayList;
import static java.util.Objects.nonNull;
import static java.util.Optional.empty;
import static java.util.UUID.randomUUID;
import static org.apache.commons.lang3.StringUtils.EMPTY;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static uk.gov.justice.core.courts.summons.SummonsDocumentContent.summonsDocumentContent;
import static uk.gov.moj.cpp.progression.processor.summons.SummonsCode.getSummonsCode;

import uk.gov.justice.core.courts.CourtApplication;
import uk.gov.justice.core.courts.CourtCentre;
import uk.gov.justice.core.courts.Defendant;
import uk.gov.justice.core.courts.FutureSummonsHearing;
import uk.gov.justice.core.courts.LjaDetails;
import uk.gov.justice.core.courts.ProsecutionCase;
import uk.gov.justice.core.courts.SummonsApprovedOutcome;
import uk.gov.justice.core.courts.SummonsType;
import uk.gov.justice.core.courts.notification.EmailChannel;
import uk.gov.justice.core.courts.summons.SummonsDocumentContent;
import uk.gov.justice.core.courts.summons.SummonsProsecutor;
import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.core.annotation.Component;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.requester.Requester;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.progression.processor.summons.CaseDefendantSummonsService;
import uk.gov.moj.cpp.progression.processor.summons.PublishSummonsDocumentService;
import uk.gov.moj.cpp.progression.processor.summons.SummonsNotificationEmailPayloadService;
import uk.gov.moj.cpp.progression.processor.summons.SummonsService;
import uk.gov.moj.cpp.progression.processor.summons.SummonsTemplateNameService;
import uk.gov.moj.cpp.progression.service.ProgressionService;
import uk.gov.moj.cpp.progression.service.RefDataService;

import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import javax.inject.Inject;
import javax.json.JsonObject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Handles re-share of a First Hearing boxwork result with "Summons Approved" when prosecution costs
 * have been amended. Generates an amended summons document with the updated prosecution costs and
 * an "amended on" date, without creating a duplicate case or hearing.
 */
@ServiceComponent(Component.EVENT_PROCESSOR)
public class SummonsAmendmentRequestedEventProcessor {

    private static final Logger LOGGER = LoggerFactory.getLogger(SummonsAmendmentRequestedEventProcessor.class.getName());

    @Inject
    private ProgressionService progressionService;

    @Inject
    private RefDataService referenceDataService;

    @ServiceComponent(Component.EVENT_PROCESSOR)
    @Inject
    private Requester requester;

    @Inject
    private JsonObjectToObjectConverter jsonObjectToObjectConverter;

    @Inject
    private CaseDefendantSummonsService caseDefendantSummonsService;

    @Inject
    private SummonsService summonsService;

    @Inject
    private SummonsTemplateNameService summonsTemplateNameService;

    @Inject
    private PublishSummonsDocumentService publishSummonsDocumentService;

    @Inject
    private SummonsNotificationEmailPayloadService summonsNotificationEmailPayloadService;

    @Handles("progression.event.court-application-summons-amendment-requested")
    public void handleSummonsAmendmentRequested(final JsonEnvelope event) {
        final UUID applicationId = UUID.fromString(event.payloadAsJsonObject().getString("applicationId"));
        final UUID caseId = UUID.fromString(event.payloadAsJsonObject().getString("caseId"));
        final SummonsApprovedOutcome newSummonsApprovedOutcome = jsonObjectToObjectConverter.convert(
                event.payloadAsJsonObject().getJsonObject("summonsApprovedOutcome"), SummonsApprovedOutcome.class);

        LOGGER.info("Processing summons amendment for applicationId: {}, caseId: {}", applicationId, caseId);

        final CourtApplication courtApplication = getCourtApplication(event, applicationId);
        final FutureSummonsHearing futureSummonsHearing = courtApplication.getFutureSummonsHearing();

        if (futureSummonsHearing == null) {
            LOGGER.warn("No futureSummonsHearing on application {} — cannot generate amended summons", applicationId);
            return;
        }

        final CourtCentre courtCentre = futureSummonsHearing.getCourtCentre();
        final ZonedDateTime hearingDateTime = futureSummonsHearing.getEarliestStartDateTime();

        final Optional<JsonObject> courtCentreJsonOptional = referenceDataService.getCourtCentreWithCourtRoomsById(
                courtCentre.getId(), event, requester);
        final JsonObject courtCentreJson = courtCentreJsonOptional.orElseThrow(() ->
                new IllegalArgumentException("Court centre not found: " + courtCentre.getId()));

        final String ljaCode = courtCentreJson.getString("lja", EMPTY);
        final Optional<LjaDetails> optionalLjaDetails = isNotBlank(ljaCode)
                ? summonsService.getLjaDetails(event, ljaCode) : empty();

        final boolean isWelsh = courtCentreJson.getBoolean("isWelsh", false);

        final ProsecutionCase prosecutionCase = getProsecutionCase(event, caseId);
        final SummonsProsecutor summonsProsecutor = summonsService.getProsecutor(
                event, prosecutionCase.getProsecutionCaseIdentifier());

        final boolean sendForRemotePrinting = !(nonNull(newSummonsApprovedOutcome.getSummonsSuppressed())
                && newSummonsApprovedOutcome.getSummonsSuppressed());

        final String templateName = summonsTemplateNameService.getCaseSummonsTemplateName(
                SummonsType.FIRST_HEARING, getSummonsCode(prosecutionCase.getSummonsCode()), isWelsh);

        final List<UUID> defendantIds = new ArrayList<>();
        prosecutionCase.getDefendants().forEach(d -> defendantIds.add(d.getId()));

        prosecutionCase.getDefendants().forEach(defendant -> generateAmendedSummonsForDefendant(
                event, courtCentreJson, optionalLjaDetails, courtCentre, hearingDateTime,
                prosecutionCase, defendant, newSummonsApprovedOutcome, summonsProsecutor,
                templateName, caseId, sendForRemotePrinting, defendantIds));
    }

    private void generateAmendedSummonsForDefendant(final JsonEnvelope event,
                                                     final JsonObject courtCentreJson,
                                                     final Optional<LjaDetails> optionalLjaDetails,
                                                     final CourtCentre courtCentre,
                                                     final ZonedDateTime hearingDateTime,
                                                     final ProsecutionCase prosecutionCase,
                                                     final Defendant defendant,
                                                     final SummonsApprovedOutcome newSummonsApprovedOutcome,
                                                     final SummonsProsecutor summonsProsecutor,
                                                     final String templateName,
                                                     final UUID caseId,
                                                     final boolean sendForRemotePrinting,
                                                     final List<UUID> defendantIds) {

        final SummonsDocumentContent baseContent = caseDefendantSummonsService.generateSummonsPayloadForDefendant(
                event, courtCentre, hearingDateTime, prosecutionCase, defendant,
                newSummonsApprovedOutcome, courtCentreJson, optionalLjaDetails, summonsProsecutor);

        final SummonsDocumentContent amendedContent = summonsDocumentContent()
                .withValuesFrom(baseContent)
                .withAmendedDate(LocalDate.now())
                .build();

        final UUID materialId = randomUUID();
        final String prosecutorEmailAddress = newSummonsApprovedOutcome.getProsecutorEmailAddress();
        final Optional<EmailChannel> emailChannel = summonsNotificationEmailPayloadService
                .getEmailChannelForCaseDefendant(null, amendedContent, prosecutorEmailAddress,
                        defendantIds, defendant, newArrayList(),
                        sendForRemotePrinting, false, materialId, SummonsType.FIRST_HEARING);

        LOGGER.info("Generating amended {} summons for defendant {} on case {}", templateName, defendant.getId(), caseId);
        publishSummonsDocumentService.generateCaseSummonsCourtDocument(
                event, defendant.getId(), caseId, amendedContent,
                templateName, sendForRemotePrinting, emailChannel.orElse(null), materialId);
    }

    private CourtApplication getCourtApplication(final JsonEnvelope event, final UUID applicationId) {
        final Optional<JsonObject> appJson = progressionService.getCourtApplicationById(event, applicationId.toString());
        final JsonObject appJsonObject = appJson.orElseThrow(() ->
                new IllegalArgumentException("Unable to get application with ID: " + applicationId));
        return jsonObjectToObjectConverter.convert(appJsonObject.getJsonObject("courtApplication"), CourtApplication.class);
    }

    private ProsecutionCase getProsecutionCase(final JsonEnvelope event, final UUID caseId) {
        final Optional<JsonObject> caseJson = progressionService.getProsecutionCaseDetailById(event, caseId.toString());
        final JsonObject caseJsonObject = caseJson.orElseThrow(() ->
                new IllegalArgumentException("Unable to get case with ID: " + caseId));
        return jsonObjectToObjectConverter.convert(caseJsonObject.getJsonObject("prosecutionCase"), ProsecutionCase.class);
    }
}
