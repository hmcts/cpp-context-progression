package uk.gov.moj.cpp.progression.processor.summons;

import static com.google.common.collect.Lists.newArrayList;
import static java.util.Collections.singletonList;
import static java.util.Objects.nonNull;
import static java.util.stream.Collectors.joining;
import static org.apache.commons.lang3.StringUtils.EMPTY;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static uk.gov.justice.core.courts.InitiationCode.S;
import static uk.gov.justice.core.courts.SummonsType.FIRST_HEARING;
import static uk.gov.justice.core.courts.SummonsType.SJP_REFERRAL;
import static uk.gov.justice.core.courts.summons.SummonsDocument.summonsDocument;
import static uk.gov.moj.cpp.progression.processor.summons.SummonsCode.generateSummons;
import static uk.gov.moj.cpp.progression.processor.summons.SummonsPayloadUtil.getFullName;
import static uk.gov.moj.cpp.progression.processor.summons.SummonsPayloadUtil.getSummonsHearingDetails;

import uk.gov.justice.core.courts.CourtApplication;
import uk.gov.justice.core.courts.CourtApplicationCase;
import uk.gov.justice.core.courts.Defendant;
import uk.gov.justice.core.courts.LegalEntityDefendant;
import uk.gov.justice.core.courts.ListDefendantRequest;
import uk.gov.justice.core.courts.Person;
import uk.gov.justice.core.courts.ProsecutionCase;
import uk.gov.justice.core.courts.ProsecutionCaseIdentifier;
import uk.gov.justice.core.courts.SummonsApprovedOutcome;
import uk.gov.justice.core.courts.SummonsDataPrepared;
import uk.gov.justice.core.courts.SummonsType;
import uk.gov.justice.core.courts.notification.EmailChannel;
import uk.gov.justice.core.courts.summons.SummonsDocument;
import uk.gov.justice.core.courts.summons.SummonsHearingCourtDetails;
import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.progression.service.NotificationService;
import uk.gov.moj.cpp.progression.service.ProgressionService;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import javax.inject.Inject;
import javax.json.JsonObject;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BulkCivilCaseSummonsNotificationService {

    private static final Logger LOGGER = LoggerFactory.getLogger(BulkCivilCaseSummonsNotificationService.class.getName());

    @Inject
    private ProgressionService progressionService;

    @Inject
    private JsonObjectToObjectConverter jsonObjectToObjectConverter;

    @Inject
    private SummonsNotificationEmailPayloadService summonsNotificationEmailPayloadService;

    @Inject
    private NotificationService notificationService;

    public boolean isBulkCivilCase(final ProsecutionCase prosecutionCase) {
        final boolean isCivil = nonNull(prosecutionCase.getIsCivil()) && prosecutionCase.getIsCivil();
        final boolean isGroupMaster = nonNull(prosecutionCase.getIsGroupMaster()) && prosecutionCase.getIsGroupMaster();
        final boolean isGroupMember = nonNull(prosecutionCase.getIsGroupMember()) && prosecutionCase.getIsGroupMember();
        return isCivil && (isGroupMaster || isGroupMember);
    }

    public void handleBulkCivilCaseSummons(final JsonEnvelope jsonEnvelope, final SummonsDataPrepared summonsDataPrepared, final JsonObject courtCentreJson,
                                           final ProsecutionCase prosecutionCase, final List<UUID> confirmedDefendantIds, final Set<UUID> notifiedBulkCivilCaseGroupIds) {
        final UUID caseId = prosecutionCase.getId();
        final UUID groupId = prosecutionCase.getGroupId();

        final Optional<SummonsApprovedOutcome> validSummonsApprovedOutcome = findValidSummonsApprovedOutcome(summonsDataPrepared, prosecutionCase, confirmedDefendantIds);
        if (!validSummonsApprovedOutcome.isPresent()) {
            LOGGER.info("Not generating bulk-summons-approved notification for case '{}' as none of its confirmed defendants match a required scenario", caseId);
            return;
        }

        if (!notifiedBulkCivilCaseGroupIds.add(groupId)) {
            LOGGER.info("Case '{}' belongs to bulk civil case group '{}' which has already been notified for this summons request - skipping duplicate notification", caseId, groupId);
            return;
        }

        sendBulkSummonsApprovedNotification(jsonEnvelope, summonsDataPrepared, courtCentreJson, groupId, validSummonsApprovedOutcome.get());
    }

    public boolean isBulkCivilCaseApplication(final CourtApplication courtApplication) {
        final boolean isGroupCaseApplication = nonNull(courtApplication.getIsGroupCaseApplication()) && courtApplication.getIsGroupCaseApplication();
        final boolean isCivil = nonNull(courtApplication.getCourtCivilApplication()) && nonNull(courtApplication.getCourtCivilApplication().getIsCivil())
                && courtApplication.getCourtCivilApplication().getIsCivil();
        return isGroupCaseApplication && isCivil;
    }

    public void handleBulkCivilCaseApplicationSummons(final JsonEnvelope jsonEnvelope, final SummonsDataPrepared summonsDataPrepared, final JsonObject courtCentreJson,
                                                       final CourtApplication courtApplication, final SummonsApprovedOutcome summonsApprovedOutcome,
                                                       final Set<UUID> notifiedBulkCivilCaseGroupIds) {
        final UUID applicationId = courtApplication.getId();
        final Optional<UUID> groupIdOptional = resolveGroupIdForApplication(jsonEnvelope, courtApplication);
        if (!groupIdOptional.isPresent()) {
            LOGGER.warn("Unable to resolve group for bulk civil application '{}' - no linked case with a group found", applicationId);
            return;
        }
        final UUID groupId = groupIdOptional.get();

        if (!notifiedBulkCivilCaseGroupIds.add(groupId)) {
            LOGGER.info("Application '{}' belongs to bulk civil case group '{}' which has already been notified for this summons request - skipping duplicate notification", applicationId, groupId);
            return;
        }

        sendBulkSummonsApprovedNotification(jsonEnvelope, summonsDataPrepared, courtCentreJson, groupId, summonsApprovedOutcome);
    }

    private Optional<UUID> resolveGroupIdForApplication(final JsonEnvelope jsonEnvelope, final CourtApplication courtApplication) {
        if (isEmptyApplicationCases(courtApplication.getCourtApplicationCases())) {
            return Optional.empty();
        }
        final UUID linkedCaseId = courtApplication.getCourtApplicationCases().get(0).getProsecutionCaseId();
        final Optional<JsonObject> linkedCaseJsonOptional = progressionService.getProsecutionCaseDetailById(jsonEnvelope, linkedCaseId.toString());
        return linkedCaseJsonOptional.map(json -> jsonObjectToObjectConverter.convert(json.getJsonObject("prosecutionCase"), ProsecutionCase.class).getGroupId());
    }

    private boolean isEmptyApplicationCases(final List<CourtApplicationCase> courtApplicationCases) {
        return courtApplicationCases == null || courtApplicationCases.isEmpty();
    }

    private Optional<SummonsApprovedOutcome> findValidSummonsApprovedOutcome(final SummonsDataPrepared summonsDataPrepared, final ProsecutionCase prosecutionCase, final List<UUID> confirmedDefendantIds) {
        return confirmedDefendantIds.stream()
                .map(defendantId -> extractDefendantRequest(summonsDataPrepared.getSummonsData().getListDefendantRequests(), defendantId))
                .filter(optionalDefendantRequest -> optionalDefendantRequest.isPresent() && isValidCaseSummonsScenario(optionalDefendantRequest, prosecutionCase))
                .map(optionalDefendantRequest -> optionalDefendantRequest.get().getSummonsApprovedOutcome())
                .findFirst();
    }

    private Optional<ListDefendantRequest> extractDefendantRequest(final List<ListDefendantRequest> listDefendantRequests, final UUID defendantId) {
        return listDefendantRequests.stream()
                .filter(e -> defendantId.equals(nonNull(e.getReferralReason()) ? e.getReferralReason().getDefendantId() : e.getDefendantId()))
                .findFirst();
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

    private void sendBulkSummonsApprovedNotification(final JsonEnvelope jsonEnvelope, final SummonsDataPrepared summonsDataPrepared, final JsonObject courtCentreJson,
                                                      final UUID groupId, final SummonsApprovedOutcome summonsApprovedOutcome) {
        LOGGER.info("Group '{}' is a bulk civil case group - suppressing summons generation, sending one bulk-summons-approved notification for the group", groupId);

        final Optional<JsonObject> masterCaseJsonOptional = progressionService.getMasterProsecutionCaseByGroupId(jsonEnvelope, groupId);
        if (!masterCaseJsonOptional.isPresent()) {
            LOGGER.warn("No group master case found for groupId '{}' - unable to send bulk-summons-approved notification", groupId);
            return;
        }
        final ProsecutionCase masterCase = jsonObjectToObjectConverter.convert(masterCaseJsonOptional.get(), ProsecutionCase.class);

        // minimal content only - case reference and hearing/court details, both taken from the
        // group master case so they stay consistent with the lead defendant shown in the email;
        // no offences/prosecutor-costs/LJA details are needed since no summons document is ever generated.
        final SummonsHearingCourtDetails hearingCourtDetails = getSummonsHearingDetails(courtCentreJson,
                summonsDataPrepared.getSummonsData().getCourtCentre().getRoomId(), summonsDataPrepared.getSummonsData().getHearingDateTime());
        final SummonsDocument bulkSummonsDocument = summonsDocument()
                .withCaseReference(extractCaseReference(masterCase.getProsecutionCaseIdentifier()))
                .withHearingCourtDetails(hearingCourtDetails)
                .build();

        final String prosecutorEmailAddress = nonNull(summonsApprovedOutcome) ? summonsApprovedOutcome.getProsecutorEmailAddress() : null;
        final String leadDefendantDetails = getLeadDefendantDetails(masterCase);
        final Optional<EmailChannel> emailChannel = summonsNotificationEmailPayloadService.getEmailChannelForBulkCaseSummonsApproved(
                summonsDataPrepared, bulkSummonsDocument, prosecutorEmailAddress, leadDefendantDetails);

        emailChannel.ifPresent(channel -> notificationService.sendEmail(jsonEnvelope, masterCase.getId(), null, null, singletonList(channel)));
    }

    private String extractCaseReference(final ProsecutionCaseIdentifier prosecutionCaseIdentifier) {
        return isNotBlank(prosecutionCaseIdentifier.getProsecutionAuthorityReference()) ? prosecutionCaseIdentifier.getProsecutionAuthorityReference() :
                prosecutionCaseIdentifier.getCaseURN();
    }

    private String getLeadDefendantDetails(final ProsecutionCase masterCase) {
        if (isEmptyList(masterCase.getDefendants())) {
            LOGGER.warn("Group master case '{}' has no defendants - unable to resolve lead defendant", masterCase.getId());
            return EMPTY;
        }

        final Defendant leadDefendant = masterCase.getDefendants().get(0);
        return newArrayList(getLeadDefendantName(leadDefendant), leadDefendant.getProsecutionAuthorityReference())
                .stream()
                .filter(StringUtils::isNotBlank)
                .collect(joining(", "));
    }

    private String getLeadDefendantName(final Defendant leadDefendant) {
        if (nonNull(leadDefendant.getPersonDefendant()) && nonNull(leadDefendant.getPersonDefendant().getPersonDetails())) {
            final Person person = leadDefendant.getPersonDefendant().getPersonDetails();
            return getFullName(person.getFirstName(), person.getMiddleName(), person.getLastName());
        }
        if (nonNull(leadDefendant.getLegalEntityDefendant()) && nonNull(leadDefendant.getLegalEntityDefendant().getOrganisation())) {
            final LegalEntityDefendant legalEntityDefendant = leadDefendant.getLegalEntityDefendant();
            return legalEntityDefendant.getOrganisation().getName();
        }
        return EMPTY;
    }

    private boolean isEmptyList(final List<Defendant> defendants) {
        return defendants == null || defendants.isEmpty();
    }
}
