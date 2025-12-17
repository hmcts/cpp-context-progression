package uk.gov.moj.cpp.progression.processor;

import static com.google.common.collect.Lists.newArrayList;
import static java.util.Objects.nonNull;
import static java.util.Optional.empty;
import static java.util.UUID.randomUUID;
import static org.apache.commons.collections.CollectionUtils.isNotEmpty;
import static org.apache.commons.lang3.StringUtils.EMPTY;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static uk.gov.justice.core.courts.InitiationCode.S;
import static uk.gov.justice.core.courts.SummonsType.APPLICATION;
import static uk.gov.justice.core.courts.SummonsType.BREACH;
import static uk.gov.justice.core.courts.SummonsType.FIRST_HEARING;
import static uk.gov.justice.core.courts.SummonsType.SJP_REFERRAL;
import static uk.gov.justice.core.courts.summons.SummonsDocumentContent.summonsDocumentContent;
import static uk.gov.moj.cpp.progression.processor.summons.SummonsCode.generateSummons;
import static uk.gov.moj.cpp.progression.processor.summons.SummonsCode.getSummonsCode;
import static uk.gov.moj.cpp.progression.processor.summons.SummonsPayloadUtil.populateSummonsAddressee;

import uk.gov.justice.core.courts.ConfirmedProsecutionCaseId;
import uk.gov.justice.core.courts.CourtApplication;
import uk.gov.justice.core.courts.CourtApplicationParty;
import uk.gov.justice.core.courts.CourtApplicationPartyListingNeeds;
import uk.gov.justice.core.courts.Defendant;
import uk.gov.justice.core.courts.ListDefendantRequest;
import uk.gov.justice.core.courts.LjaDetails;
import uk.gov.justice.core.courts.Person;
import uk.gov.justice.core.courts.ProsecutionCase;
import uk.gov.justice.core.courts.SummonsApprovedOutcome;
import uk.gov.justice.core.courts.SummonsDataPrepared;
import uk.gov.justice.core.courts.SummonsType;
import uk.gov.justice.core.courts.notification.EmailChannel;
import uk.gov.justice.core.courts.summons.SummonsAddressee;
import uk.gov.justice.core.courts.summons.SummonsDocumentContent;
import uk.gov.justice.core.courts.summons.SummonsProsecutor;
import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.core.annotation.Component;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.requester.Requester;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.progression.domain.utils.LocalDateUtils;
import uk.gov.moj.cpp.progression.processor.summons.ApplicantEmailAddressUtil;
import uk.gov.moj.cpp.progression.processor.summons.ApplicationSummonsService;
import uk.gov.moj.cpp.progression.processor.summons.CaseDefendantSummonsService;
import uk.gov.moj.cpp.progression.processor.summons.PublishSummonsDocumentService;
import uk.gov.moj.cpp.progression.processor.summons.SummonsNotificationEmailPayloadService;
import uk.gov.moj.cpp.progression.processor.summons.SummonsService;
import uk.gov.moj.cpp.progression.processor.summons.SummonsTemplateNameService;
import uk.gov.moj.cpp.progression.service.ProgressionService;
import uk.gov.moj.cpp.progression.service.RefDataService;

import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import javax.inject.Inject;
import javax.json.JsonObject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@SuppressWarnings({"squid:S1188", "squid:S2250"})
@ServiceComponent(Component.EVENT_PROCESSOR)
public class SummonsDataPreparedEventProcessor {

    private static final Logger LOGGER = LoggerFactory.getLogger(SummonsDataPreparedEventProcessor.class.getName());

    private static final List<SummonsType> PARENT_TEMPLATE_APPLICABLE_FOR = newArrayList(FIRST_HEARING, BREACH);

    @Inject
    private ProgressionService progressionService;

    @Inject
    private RefDataService referenceDataService;

    @Inject
    private Requester requester;

    @Inject
    private JsonObjectToObjectConverter jsonObjectToObjectConverter;

    @Inject
    private CaseDefendantSummonsService caseDefendantSummonsService;

    @Inject
    private ApplicationSummonsService applicationSummonsService;

    @Inject
    private ApplicantEmailAddressUtil applicantEmailAddressUtil;

    @Inject
    private SummonsService summonsService;

    @Inject
    private SummonsTemplateNameService summonsTemplateNameService;

    @Inject
    private PublishSummonsDocumentService publishSummonsDocumentService;

    @Inject
    private SummonsNotificationEmailPayloadService summonsNotificationEmailPayloadService;

    /**
     * The payload supports a collection of cases and application but summons will only be generated
     * for either a case or an application at a time in a single transaction.
     * <p>
     * If case or application, the following sequence of steps take place
     * <p>
     * <ul>
     *     <li>generate summons document content</li>
     *     <li>generate template name</li>
     *     <li>create court document object and add document to CDES</li>
     *     <li>documentGeneratorService.generateSummonsDocument for case ID or application ID and initiate upload material flow</li>
     * </ul>
     *
     * @param jsonEnvelope envelope with metadata and payload
     */
    @Handles("progression.event.summons-data-prepared")
    public void requestSummons(final JsonEnvelope jsonEnvelope) {

        final SummonsDataPrepared summonsDataPrepared = jsonObjectToObjectConverter.convert(jsonEnvelope.payloadAsJsonObject(), SummonsDataPrepared.class);

        final UUID courtCentreId = summonsDataPrepared.getSummonsData().getCourtCentre().getId();
        final Optional<JsonObject> courtCentreJsonOptional = referenceDataService.getCourtCentreWithCourtRoomsById(courtCentreId, jsonEnvelope, requester);
        final JsonObject courtCentreJson = courtCentreJsonOptional.orElseThrow(() -> new IllegalArgumentException(String.format("Court centre '%s' not found", courtCentreId)));
        final String ljaCode = courtCentreJson.getString("lja", EMPTY);
        final Optional<LjaDetails> optionalLjaDetails = isNotBlank(ljaCode) ? summonsService.getLjaDetails(jsonEnvelope, ljaCode) : empty();

        final boolean isWelsh = courtCentreJson.getBoolean("isWelsh", false);

        final List<ConfirmedProsecutionCaseId> confirmedProsecutionCaseIds = summonsDataPrepared.getSummonsData().getConfirmedProsecutionCaseIds();
        if (isNotEmpty(confirmedProsecutionCaseIds) && isNotEmpty(summonsDataPrepared.getSummonsData().getListDefendantRequests())) {
            processCaseSummons(jsonEnvelope, summonsDataPrepared, courtCentreJson, optionalLjaDetails, isWelsh, confirmedProsecutionCaseIds);
        }

        final List<UUID> confirmedApplicationIds = summonsDataPrepared.getSummonsData().getConfirmedApplicationIds();
        if (isNotEmpty(confirmedApplicationIds) && isNotEmpty(summonsDataPrepared.getSummonsData().getCourtApplicationPartyListingNeeds())) {
            processApplicationSummons(jsonEnvelope, summonsDataPrepared, courtCentreJson, optionalLjaDetails, isWelsh, confirmedApplicationIds);
        }
    }

    private void processApplicationSummons(final JsonEnvelope jsonEnvelope, final SummonsDataPrepared summonsDataPrepared, final JsonObject courtCentreJson, final Optional<LjaDetails> optionalLjaDetails, final boolean isWelsh, final List<UUID> confirmedApplicationIds) {
        for (final UUID applicationId : confirmedApplicationIds) {

            final CourtApplication courtApplicationQueried = getCourtApplication(jsonEnvelope, applicationId);
            final List<CourtApplicationPartyListingNeeds> courtApplicationPartyListingNeeds = summonsDataPrepared.getSummonsData().getCourtApplicationPartyListingNeeds();
            final Optional<CourtApplicationPartyListingNeeds> optionalSubjectNeeds = extractApplicationListingNeeds(courtApplicationPartyListingNeeds, courtApplicationQueried.getSubject().getId());

            if (!optionalSubjectNeeds.isPresent() || !isValidApplicationScenario(optionalSubjectNeeds)) {
                LOGGER.info("Not generating summons for subject on application with ID '{}'", applicationId);
                return;
            }

            final CourtApplicationPartyListingNeeds subjectNeeds = optionalSubjectNeeds.get();
            final String applicantEmailAddress = getProsecutorEmailAddress(subjectNeeds.getSummonsApprovedOutcome());
            final SummonsType summonsRequired = subjectNeeds.getSummonsRequired();
            final boolean sendForRemotePrinting = !(nonNull(subjectNeeds.getSummonsApprovedOutcome().getSummonsSuppressed()) && subjectNeeds.getSummonsApprovedOutcome().getSummonsSuppressed());
            final String subjectTemplateName = summonsTemplateNameService.getApplicationTemplateName(summonsRequired, isWelsh);
            final SummonsDocumentContent subjectSummonsDocumentContent = applicationSummonsService.generateSummonsDocumentContent(summonsDataPrepared, courtApplicationQueried, subjectNeeds, courtCentreJson, optionalLjaDetails);
            final boolean addresseeIsYouth = addresseeIsYouth(summonsDataPrepared.getSummonsData().getHearingDateTime(), subjectSummonsDocumentContent.getDefendant().getDateOfBirth());
            final UUID materialId = randomUUID();
            final Optional<EmailChannel> emailChannel = summonsNotificationEmailPayloadService.getEmailChannelForApplicationAddressee(
                    summonsDataPrepared, subjectSummonsDocumentContent, applicantEmailAddress, sendForRemotePrinting, addresseeIsYouth, materialId, summonsRequired);

            LOGGER.info("Generating {} summons for subject on application '{}'", subjectTemplateName, applicationId);
            publishSummonsDocumentService.generateApplicationSummonsCourtDocument(jsonEnvelope, applicationId, subjectSummonsDocumentContent, subjectTemplateName, sendForRemotePrinting, emailChannel.orElse(null), materialId);

            if (PARENT_TEMPLATE_APPLICABLE_FOR.contains(summonsRequired) && addresseeIsYouth) {
                final String parentGuardianTemplateName = summonsTemplateNameService.getBreachSummonsParentTemplateName(isWelsh);
                final SummonsAddressee parentGuardianAddressee = populateSummonsAddressee(getApplicationSubjectParentGuardian(courtApplicationQueried.getSubject()));
                final SummonsDocumentContent parentGuardianSummonsDocumentContent = summonsDocumentContent().withValuesFrom(subjectSummonsDocumentContent).withAddressee(parentGuardianAddressee).build();
                final UUID materialIdForParentGuardian = randomUUID();
                final Optional<EmailChannel> emailChannelForParentGuardian = summonsNotificationEmailPayloadService.getEmailChannelForApplicationAddresseeParent(
                        summonsDataPrepared, parentGuardianSummonsDocumentContent, applicantEmailAddress, sendForRemotePrinting,
                        materialIdForParentGuardian, summonsRequired);

                LOGGER.info("Generating {} summons for parent/guardian of subject on application '{}'", parentGuardianTemplateName, applicationId);
                publishSummonsDocumentService.generateApplicationSummonsCourtDocument(jsonEnvelope, applicationId, parentGuardianSummonsDocumentContent, parentGuardianTemplateName, sendForRemotePrinting, emailChannelForParentGuardian.orElse(null), materialIdForParentGuardian);
            }
        }
    }

    private CourtApplication getCourtApplication(final JsonEnvelope jsonEnvelope, final UUID applicationId) {
        final Optional<JsonObject> applicationJsonOptional = progressionService.getCourtApplicationById(jsonEnvelope, applicationId.toString());
        final JsonObject applicationJsonObject = applicationJsonOptional.orElseThrow(() -> new IllegalArgumentException("Unable to get application with ID : " + applicationId));
        return jsonObjectToObjectConverter.convert(applicationJsonObject.getJsonObject("courtApplication"), CourtApplication.class);
    }

    private void processCaseSummons(final JsonEnvelope jsonEnvelope, final SummonsDataPrepared summonsDataPrepared, final JsonObject courtCentreJson, final Optional<LjaDetails> ljaDetails, final boolean isWelsh, final List<ConfirmedProsecutionCaseId> confirmedProsecutionCaseIds) {
        for (final ConfirmedProsecutionCaseId confirmedProsecutionCaseId : confirmedProsecutionCaseIds) {
            final UUID caseId = confirmedProsecutionCaseId.getId();
            final ProsecutionCase prosecutionCase = getProsecutionCase(jsonEnvelope, caseId);
            final SummonsProsecutor summonProsecutor = summonsService.getProsecutor(jsonEnvelope, prosecutionCase.getProsecutionCaseIdentifier());
            final List<UUID> confirmedDefendantIds = confirmedProsecutionCaseId.getConfirmedDefendantIds();
            final List<String> combinedDefendantDetailsForEmailChannel = newArrayList();

            confirmedDefendantIds.forEach(defendantId -> {
                final Optional<ListDefendantRequest> optionalDefendantRequest = extractDefendantRequest(summonsDataPrepared.getSummonsData().getListDefendantRequests(), defendantId);

                if (!optionalDefendantRequest.isPresent() || !isValidCaseSummonsScenario(optionalDefendantRequest, prosecutionCase)) {
                    LOGGER.info("Not generating summons for defendant with ID '{}' on case '{}' as its not a required scenario", defendantId, caseId);
                    return;
                }
                final ListDefendantRequest defendantRequest = optionalDefendantRequest.get();
                final SummonsType summonsRequired = optionalDefendantRequest.get().getSummonsRequired();
                final SummonsApprovedOutcome summonsApprovedOutcome = defendantRequest.getSummonsApprovedOutcome();
                final boolean sendForRemotePrinting = !(FIRST_HEARING == summonsRequired
                        && nonNull(summonsApprovedOutcome.getSummonsSuppressed()) && summonsApprovedOutcome.getSummonsSuppressed());

                final Defendant defendant = prosecutionCase.getDefendants().stream().filter(d -> d.getId().equals(defendantId)).findFirst()
                        .orElseThrow(() -> new IllegalArgumentException(String.format("Unable to locate defendant '%s' on case '%s'", defendantId, caseId)));

                final String defendantTemplateName = summonsTemplateNameService.getCaseSummonsTemplateName(summonsRequired, getSummonsCode(prosecutionCase.getSummonsCode()), isWelsh);
                final SummonsDocumentContent defendantSummonsDocumentContent = caseDefendantSummonsService.generateSummonsPayloadForDefendant(jsonEnvelope, summonsDataPrepared, prosecutionCase, defendant, defendantRequest, courtCentreJson, ljaDetails, summonProsecutor);
                final boolean addresseeIsYouth = addresseeIsYouth(summonsDataPrepared.getSummonsData().getHearingDateTime(), defendantSummonsDocumentContent.getDefendant().getDateOfBirth());
                final UUID materialId = randomUUID();
                final String prosecutorEmailAddress = getProsecutorEmailAddress(summonsApprovedOutcome);
                final Optional<EmailChannel> emailChannel = summonsNotificationEmailPayloadService.getEmailChannelForCaseDefendant(
                        summonsDataPrepared, defendantSummonsDocumentContent, prosecutorEmailAddress, confirmedDefendantIds, defendant, combinedDefendantDetailsForEmailChannel,
                        sendForRemotePrinting, addresseeIsYouth, materialId, summonsRequired);

                LOGGER.info("Generating {} summons for for defendant '{}' on case '{}'", defendantTemplateName, defendantId, caseId);
                publishSummonsDocumentService.generateCaseSummonsCourtDocument(jsonEnvelope, defendantId, caseId, defendantSummonsDocumentContent,
                        defendantTemplateName, sendForRemotePrinting, emailChannel.orElse(null), materialId);

                // check if first hearing and defendant is youth requiring document generation for parent / guardian
                if (PARENT_TEMPLATE_APPLICABLE_FOR.contains(summonsRequired) && addresseeIsYouth) {
                    // only addressee is different for parent payload, rest of the payload is same as defendants
                    final String parentGuardianTemplateName = summonsTemplateNameService.getCaseSummonsParentTemplateName(isWelsh);
                    final SummonsDocumentContent parentGuardianSummonsDocumentContent = summonsDocumentContent().withValuesFrom(defendantSummonsDocumentContent).withAddressee(populateSummonsAddressee(getDefendantParentGuardian(defendant))).build();
                    final UUID materialIdForParentGuardian = randomUUID();
                    final Optional<EmailChannel> emailChannelForParentGuardian = summonsNotificationEmailPayloadService.getEmailChannelForCaseDefendantParent(
                            summonsDataPrepared, parentGuardianSummonsDocumentContent, prosecutorEmailAddress, confirmedDefendantIds, defendant,
                            combinedDefendantDetailsForEmailChannel, sendForRemotePrinting, materialIdForParentGuardian, summonsRequired);

                    LOGGER.info("Generating {} summons for parent / guardian of defendant '{}' on case '{}'", parentGuardianTemplateName, defendantId, caseId);
                    publishSummonsDocumentService.generateCaseSummonsCourtDocument(jsonEnvelope, defendantId, caseId, parentGuardianSummonsDocumentContent,
                            parentGuardianTemplateName, sendForRemotePrinting, emailChannelForParentGuardian.orElse(null), materialIdForParentGuardian);
                }
            });
        }
    }

    private String getProsecutorEmailAddress(final SummonsApprovedOutcome summonsApprovedOutcome) {
        return nonNull(summonsApprovedOutcome) ? summonsApprovedOutcome.getProsecutorEmailAddress() : null;
    }

    private ProsecutionCase getProsecutionCase(final JsonEnvelope jsonEnvelope, final UUID caseId) {
        final Optional<JsonObject> prosecutionCaseOptional = progressionService.getProsecutionCaseDetailById(jsonEnvelope, caseId.toString());
        final JsonObject prosecutionCaseJson = prosecutionCaseOptional.orElseThrow(() -> new IllegalArgumentException("Unable to get case with ID : " + caseId));
        return jsonObjectToObjectConverter.convert(prosecutionCaseJson.getJsonObject("prosecutionCase"), ProsecutionCase.class);
    }

    private Optional<ListDefendantRequest> extractDefendantRequest(final List<ListDefendantRequest> listDefendantRequests, final UUID defendantId) {
        return listDefendantRequests.stream()
                .filter(e -> defendantId.equals(nonNull(e.getReferralReason()) ? e.getReferralReason().getDefendantId() : e.getDefendantId()))
                .findFirst();
    }

    private Optional<CourtApplicationPartyListingNeeds> extractApplicationListingNeeds(final List<CourtApplicationPartyListingNeeds> listingNeeds, final UUID subjectId) {
        return listingNeeds.stream()
                .filter(e -> subjectId.equals(e.getCourtApplicationPartyId()))
                .findFirst();
    }

    private boolean addresseeIsYouth(final ZonedDateTime hearingDateTime, final String partyDateOfBirth) {
        if (nonNull(hearingDateTime) && isNotBlank(partyDateOfBirth)) {
            return LocalDateUtils.isYouth(LocalDate.parse(partyDateOfBirth), hearingDateTime.toLocalDate());
        }

        return false;
    }

    private boolean isValidCaseSummonsScenario(final Optional<ListDefendantRequest> listDefendantRequest, final ProsecutionCase prosecutionCase) {
        if (!listDefendantRequest.isPresent()) {
            return false;
        }

        final SummonsType summonsRequired = listDefendantRequest.get().getSummonsRequired();
        final boolean summonsInitiationCode = (S == prosecutionCase.getInitiationCode());
        final boolean validFirstHearingSummonsScenario = FIRST_HEARING == summonsRequired && generateSummons(prosecutionCase.getSummonsCode()) && summonsInitiationCode;
        final boolean validSjpReferralScenario = (SJP_REFERRAL == summonsRequired);
        return validFirstHearingSummonsScenario || validSjpReferralScenario;
    }


    private boolean isValidApplicationScenario(final Optional<CourtApplicationPartyListingNeeds> optionalCourtApplicationPartyListingNeeds) {
        if (!optionalCourtApplicationPartyListingNeeds.isPresent()) {
            return false;
        }

        final SummonsType summonsRequired = optionalCourtApplicationPartyListingNeeds.get().getSummonsRequired();
        return APPLICATION == summonsRequired || BREACH == summonsRequired;
    }

    private Person getDefendantParentGuardian(final Defendant defendantQueried) {
        return isNotEmpty(defendantQueried.getAssociatedPersons()) ? defendantQueried.getAssociatedPersons().get(0).getPerson() : null;
    }

    private Person getApplicationSubjectParentGuardian(final CourtApplicationParty courtApplicationParty) {
        return nonNull(courtApplicationParty.getMasterDefendant()) && isNotEmpty(courtApplicationParty.getMasterDefendant().getAssociatedPersons()) ? courtApplicationParty.getMasterDefendant().getAssociatedPersons().get(0).getPerson() : null;
    }

}
