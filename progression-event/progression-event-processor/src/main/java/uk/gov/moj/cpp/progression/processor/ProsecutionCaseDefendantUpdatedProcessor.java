package uk.gov.moj.cpp.progression.processor;

import static java.util.Objects.nonNull;
import static java.util.Optional.empty;
import static java.util.Optional.ofNullable;
import static java.util.UUID.fromString;
import static uk.gov.justice.services.messaging.JsonObjects.createObjectBuilder;
import static org.apache.commons.collections.CollectionUtils.isNotEmpty;
import static uk.gov.justice.services.core.annotation.Component.EVENT_PROCESSOR;
import static uk.gov.justice.services.core.enveloper.Enveloper.envelop;
import static uk.gov.moj.cpp.progression.helper.LinkSplitMergeHelper.CASE_ID;

import uk.gov.justice.core.courts.DefendantUpdate;
import uk.gov.justice.core.courts.HearingDay;
import uk.gov.justice.core.courts.LegalEntityDefendant;
import uk.gov.justice.core.courts.Person;
import uk.gov.justice.core.courts.PersonDefendant;
import uk.gov.justice.core.courts.ProsecutionCaseDefendantUpdated;
import uk.gov.justice.core.courts.UpdatedOrganisation;
import uk.gov.justice.core.courts.HearingListingStatus;
import uk.gov.justice.progression.courts.GetHearingsAtAGlance;
import uk.gov.justice.progression.courts.Hearings;
import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.common.converter.ObjectToJsonObjectConverter;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.core.requester.Requester;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.progression.domain.constant.CaseStatusEnum;
import uk.gov.moj.cpp.progression.domain.constant.DateTimeFormats;
import uk.gov.moj.cpp.progression.events.DefendantCustodialInformationUpdateRequested;
import uk.gov.moj.cpp.progression.service.NotificationService;
import uk.gov.moj.cpp.progression.service.ProgressionService;
import uk.gov.moj.cpp.progression.service.RefDataService;
import uk.gov.moj.cpp.progression.value.object.CPSNotificationVO;
import uk.gov.moj.cpp.progression.value.object.CaseVO;
import uk.gov.moj.cpp.progression.value.object.DefenceOrganisationVO;
import uk.gov.moj.cpp.progression.value.object.DefendantVO;
import uk.gov.moj.cpp.progression.value.object.EmailTemplateType;
import uk.gov.moj.cpp.progression.value.object.HearingVO;

import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.inject.Inject;
import uk.gov.justice.services.messaging.JsonObjects;
import javax.json.JsonArray;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;

import org.apache.commons.collections.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@SuppressWarnings({"squid:S3457", "squid:S3655"})
@ServiceComponent(EVENT_PROCESSOR)
public class ProsecutionCaseDefendantUpdatedProcessor {

    protected static final String PUBLIC_CASE_DEFENDANT_CHANGED = "public.progression.case-defendant-changed";
    protected static final String COMMAND_UPDATE_DEFENDANT_FOR_HEARING = "progression.command.update-defendant-for-hearing";
    private static final Logger LOGGER = LoggerFactory.getLogger(ProsecutionCaseDefendantUpdatedProcessor.class.getCanonicalName());
    private static final String HEARING_ID = "hearingId";
    private static final String CPS_FLAG = "cpsFlag";
    public static final String MATCHED_DEFENDANT_CASES = "matchedDefendantCases";
    public static final String PROGRESSION_COMMAND_UPDATE_DEFENDANT_CUSTODIAL_INFORMATION = "progression.command.update-matched-defendant-custodial-information";
    public static final String PROGRESSION_COMMAND_UPDATE_DEFENDANT_CUSTODIAL_INFORMATION_FOR_APPLICATION = "progression.command.update-defendant-custodial-information-for-application";
    public static final String MASTER_DEFENDANT_ID = "masterDefendantId";
    public static final String MATCHED_MASTER_DEFENDANT_ID = "matchedMasterDefendantId";
    public static final String DEFENDANTS = "defendants";
    public static final String CUSTODIAL_ESTABLISHMENT = "custodialEstablishment";
    public static final String LINKED_APPLICATIONS = "linkedApplications";
    public static final String APPLICATION_ID = "applicationId";
    public static final String DEFENDANT = "defendant";
    public static final String PROGRESSION_COMMAND_UPDATE_DEFENDANT_ADDRESS_ON_APPLICATION = "progression.command.update-defendant-address-on-application";
    public static final String HEARING_IDS = "hearingIds";

    @Inject
    private Sender sender;

    @Inject
    private Enveloper enveloper;

    @Inject
    private JsonObjectToObjectConverter jsonObjectConverter;

    @Inject
    private ObjectToJsonObjectConverter objectToJsonObjectConverter;

    @Inject
    private ProgressionService progressionService;

    @Inject
    private RefDataService referenceDataService;

    @Inject
    private Requester requester;

    @Inject
    private JsonObjectToObjectConverter jsonObjectToObjectConverter;

    @Inject
    private NotificationService notificationService;


    @Handles("progression.event.prosecution-case-defendant-updated")
    public void handleProsecutionCaseDefendantUpdatedEvent(final JsonEnvelope jsonEnvelope) {
        final ProsecutionCaseDefendantUpdated prosecutionCaseDefendantUpdated = jsonObjectConverter.convert(jsonEnvelope.payloadAsJsonObject(), ProsecutionCaseDefendantUpdated.class);
        final DefendantUpdate defendant = prosecutionCaseDefendantUpdated.getDefendant();
        final List<UUID> hearingIds = prosecutionCaseDefendantUpdated.getHearingIds();
        LOGGER.debug("Received prosecution case defendant updated for caseId: " + defendant.getProsecutionCaseId());

        final JsonObject publicEventPayload = createObjectBuilder()
                .add(DEFENDANT, objectToJsonObjectConverter.convert(updateDefendant(defendant))).build();
        sender.send(
                envelop(publicEventPayload)
                        .withName(PUBLIC_CASE_DEFENDANT_CHANGED)
                        .withMetadataFrom(jsonEnvelope));
        if (nonNull(hearingIds)) {
            hearingIds.stream().collect(Collectors.toSet()).forEach(hearingId ->
                    sendDefendantUpdate(jsonEnvelope, defendant, hearingId));
        }

        final Optional<JsonObject> prosecutionCaseOptional = progressionService.getProsecutionCaseDetailById(jsonEnvelope, defendant.getProsecutionCaseId().toString());


        if (nonNull(prosecutionCaseDefendantUpdated.getProsecutionAuthorityId()) && nonNull(prosecutionCaseDefendantUpdated.getUpdatedOrganisation())) {
            final UUID prosecutorId = fromString(prosecutionCaseDefendantUpdated.getProsecutionAuthorityId());
            final Optional<JsonObject> prosecutorDetails = getProsecutorById(prosecutorId, jsonEnvelope);
            if (prosecutorDetails.isPresent()) {
                final JsonObject prosecutorsJsonObject = prosecutorDetails.get();
                final boolean isCpsProsecutor = prosecutorsJsonObject.getBoolean(CPS_FLAG, false);
                if (isCpsProsecutor) {
                    sendDefendantAssociationCPSNotification(jsonEnvelope, prosecutionCaseDefendantUpdated, prosecutionCaseOptional, EmailTemplateType.ASSOCIATION);
                }
            }
        }
        handleUpdateDefendantCustodialInformationForApplication(jsonEnvelope, defendant, prosecutionCaseOptional);
        handleUpdateActiveApplicationsOnCase(jsonEnvelope, defendant.getProsecutionCaseId().toString(), defendant);
    }

    public void handleUpdateActiveApplicationsOnCase(final JsonEnvelope jsonEnvelope, final String caseId, DefendantUpdate defendant) {
        final Optional<JsonObject> activeApplicationsOnCaseOptional = progressionService.getActiveApplicationsOnCase(jsonEnvelope, caseId);
        if (activeApplicationsOnCaseOptional.isPresent() && activeApplicationsOnCaseOptional.get().containsKey(LINKED_APPLICATIONS)){
        activeApplicationsOnCaseOptional.get().getJsonArray(LINKED_APPLICATIONS).forEach(linkedApplicationJson->{
                    final JsonObject linkedApplicationJsonObject = (JsonObject) linkedApplicationJson;
                    final String applicationId = linkedApplicationJsonObject.getString(APPLICATION_ID);
                    final JsonObjectBuilder updateDefendantAddressOnApplicationBuilder = JsonObjects.createObjectBuilder();
                    if(nonNull(applicationId) && nonNull(linkedApplicationJsonObject.getJsonArray(HEARING_IDS))){
                        updateDefendantAddressOnApplicationBuilder
                                .add(APPLICATION_ID, applicationId)
                                .add(DEFENDANT, objectToJsonObjectConverter.convert(updateDefendant(defendant)))
                                .add(HEARING_IDS,linkedApplicationJsonObject.getJsonArray(HEARING_IDS));
                        sender.send(
                                envelop(updateDefendantAddressOnApplicationBuilder.build())
                                        .withName(PROGRESSION_COMMAND_UPDATE_DEFENDANT_ADDRESS_ON_APPLICATION)
                                        .withMetadataFrom(jsonEnvelope));
                    }
                });
        }
    }

    private void handleUpdateDefendantCustodialInformationForApplication(JsonEnvelope jsonEnvelope, DefendantUpdate defendant, Optional<JsonObject> prosecutionCaseOptional) {
        prosecutionCaseOptional.ifPresent(prosecutionCaseJson -> {
            final String caseStatus = prosecutionCaseJson.getJsonObject("prosecutionCase").getString("caseStatus", null);
            if (prosecutionCaseJson.containsKey("linkedApplicationsSummary") && caseStatus.equalsIgnoreCase(CaseStatusEnum.ACTIVE.name())) {
                prosecutionCaseJson.getJsonArray("linkedApplicationsSummary").forEach(linkedApplicationSummaryJson -> {
                    final JsonObject linkedApplicationJsonObject = (JsonObject) linkedApplicationSummaryJson;
                    final JsonObjectBuilder updateCustodialInformationForApplicationBuilder = JsonObjects.createObjectBuilder();
                    final String subjectId = linkedApplicationJsonObject.getString("subjectId", null);
                    if (nonNull(subjectId) && nonNull(defendant.getMasterDefendantId()) && subjectId.equalsIgnoreCase(defendant.getMasterDefendantId().toString())) {
                        updateCustodialInformationForApplicationBuilder.add(APPLICATION_ID, linkedApplicationJsonObject.getString(APPLICATION_ID));
                        updateCustodialInformationForApplicationBuilder.add(DEFENDANT, objectToJsonObjectConverter.convert(updateDefendant(defendant)));
                        sender.send(
                                Enveloper.envelop(updateCustodialInformationForApplicationBuilder.build())
                                        .withName(PROGRESSION_COMMAND_UPDATE_DEFENDANT_CUSTODIAL_INFORMATION_FOR_APPLICATION)
                                        .withMetadataFrom(jsonEnvelope));
                    }

                });
            }

        });

    }

    @Handles("progression.event.defendant-custodial-information-update-requested")
    public void handleDefendantCustodialInformationUpdatedEvent(final JsonEnvelope jsonEnvelope) {
        final DefendantCustodialInformationUpdateRequested defendantCustodialInformationUpdateRequested = jsonObjectConverter.convert(jsonEnvelope.payloadAsJsonObject(), DefendantCustodialInformationUpdateRequested.class);
        final Optional<JsonObject> matchedCases = progressionService.searchLinkedCases(jsonEnvelope, defendantCustodialInformationUpdateRequested.getProsecutionCaseId().toString());
        if (matchedCases.isPresent() && nonNull(matchedCases.get())) {
            final JsonObject matchedCasesJsonObject = matchedCases.get();
            if (matchedCasesJsonObject.containsKey(MATCHED_DEFENDANT_CASES)) {
                matchedCasesJsonObject.getJsonArray(MATCHED_DEFENDANT_CASES).getValuesAs(JsonObject.class).stream()
                        .filter(matchedCase -> defendantCustodialInformationUpdateRequested.getMasterDefendantId().toString().equalsIgnoreCase(matchedCase.getString(MATCHED_MASTER_DEFENDANT_ID)))
                        .forEach(matchedCase -> updateMatchedDefendantCustodialInformation(jsonEnvelope, defendantCustodialInformationUpdateRequested, matchedCase));
            }
        }
    }

    @Handles("progression.event.hearing-defendant-updated")
    public void handleHearingDefendantUpdatedEvent(final JsonEnvelope jsonEnvelope) {
        progressionService.populateHearingToProbationCaseworker(jsonEnvelope, fromString(jsonEnvelope.payloadAsJsonObject().getString(HEARING_ID)));
    }


    private void sendDefendantAssociationCPSNotification(final JsonEnvelope jsonEnvelope, final ProsecutionCaseDefendantUpdated prosecutionCaseDefendantUpdated, final Optional<JsonObject> prosecutionCaseOptional, final EmailTemplateType templateType) {
        final String caseId = prosecutionCaseDefendantUpdated.getDefendant().getProsecutionCaseId().toString();
        final Optional<HearingVO> hearingVO = getHearingDetails(prosecutionCaseOptional);
        final boolean isHearingPresent = hearingVO.isPresent() && hearingVO.get().getHearingDate() != null;

        if (isHearingPresent) {
            populateCPSNotificationAndSendEmail(jsonEnvelope, prosecutionCaseDefendantUpdated,
                    hearingVO.get(), templateType);
        } else {
            LOGGER.info("Future hearing is not found for the case : {}", caseId);
        }
    }

    private Optional<HearingVO> getHearingDetails(final Optional<JsonObject> prosecutionCaseOptional) {

        final Optional<HearingVO> hearingVO = empty();

        if (prosecutionCaseOptional.isPresent()) {

            final JsonObject hearingAtAGlanceJsonObject = prosecutionCaseOptional.get().getJsonObject("hearingsAtAGlance");
            final GetHearingsAtAGlance hearingAtAGlance = jsonObjectToObjectConverter.convert(hearingAtAGlanceJsonObject, GetHearingsAtAGlance.class);

            if (CollectionUtils.isEmpty(hearingAtAGlance.getHearings())) {
                return hearingVO;
            }

            final List<Hearings> futureHearings = getFutureHearings(hearingAtAGlance);
            final Optional<Map.Entry<UUID, ZonedDateTime>> resultMap = getEarliestHearing(futureHearings);

            if (resultMap.isPresent()) {
                LOGGER.info("Found result hearing {} with earliest date : {}", resultMap.get().getKey(), resultMap.get().getValue());
                final LocalDate localHearingDate = resultMap.get().getValue().toLocalDate();
                return getHearingVO(localHearingDate.format(DateTimeFormatter.ofPattern(DateTimeFormats.DATE_SLASHED_DD_MM_YYYY.getValue())), futureHearings, resultMap);
            }
        }
        return hearingVO;
    }

    private Optional<HearingVO> getHearingVO(final String hearingDate, List<Hearings> futureHearings, final Optional<Map.Entry<UUID, ZonedDateTime>> resultMap) {
        final List<Hearings> resultHearing = futureHearings.stream()
                .filter(hearing -> hearing.getId().equals(resultMap.get().getKey()))
                .collect(Collectors.toList());

        final Hearings hearing = resultHearing.get(0);
        final String courtName = hearing.getCourtCentre().getName();
        final UUID courtCentreId = hearing.getCourtCentre().getId();
        return Optional.of(HearingVO.builder()
                .hearingDate(hearingDate)
                .courtCenterId(courtCentreId)
                .courtName(courtName)
                .build());
    }

    private List<Hearings> getFutureHearings(GetHearingsAtAGlance hearingsAtAGlance) {

        List<Hearings> hearingsWithSentForListingStatus = hearingsAtAGlance.getHearings()
                .stream()
                .filter(t -> t.getHearingListingStatus().equals(HearingListingStatus.SENT_FOR_LISTING))
                .toList();

        List<Hearings> hearings = hearingsAtAGlance.getHearings().stream()
                .filter(t -> !t.getHearingListingStatus().equals(HearingListingStatus.SENT_FOR_LISTING))
                .filter(h -> h.getHearingDays().stream().anyMatch(hearingDay -> hearingDay.getSittingDay().toLocalDate().compareTo(LocalDate.now()) >= 0))
                .toList();

        return Stream.concat(hearingsWithSentForListingStatus.stream(), hearings.stream()).toList();
    }

    private Optional<Map.Entry<UUID, ZonedDateTime>> getEarliestHearing(List<Hearings> futureHearings) {
        final Map<UUID, ZonedDateTime> hearingDaysMap = new HashMap<>();

        for (final Hearings hearing : futureHearings) {
            if (!(hearing.getHearingListingStatus() == HearingListingStatus.SENT_FOR_LISTING)) {
                final List<HearingDay> futureHearingDays = hearing
                        .getHearingDays().stream()
                        .filter(hd -> hd.getSittingDay().toLocalDate().isAfter(LocalDate.now()))
                        .collect(Collectors.toList());

                if (!futureHearingDays.isEmpty()) {
                    hearingDaysMap.put(hearing.getId(), getEarliestHearingDay(futureHearingDays));
                }
                return hearingDaysMap.entrySet()
                        .stream().min(Map.Entry.comparingByValue());
            }
        }
        return Optional.empty();
    }

    private static ZonedDateTime getEarliestHearingDay(final List<HearingDay> hearingDays) {
        return hearingDays.stream()
                .map(HearingDay::getSittingDay)
                .sorted()
                .findFirst()
                .orElseThrow(IllegalArgumentException::new);
    }

    private void populateCPSNotificationAndSendEmail(final JsonEnvelope jsonEnvelope, final ProsecutionCaseDefendantUpdated prosecutionCaseDefendantUpdated,
                                                     final HearingVO hearingVO, final EmailTemplateType templateType) {

        final Optional<String> cpsEmailAddress = getCPSEmail(jsonEnvelope, hearingVO.getCourtCenterId());
        final UpdatedOrganisation updatedOrganisation = prosecutionCaseDefendantUpdated.getUpdatedOrganisation();
        final String caseUrn = prosecutionCaseDefendantUpdated.getCaseUrn();

        if (cpsEmailAddress.isPresent()) {
            final CPSNotificationVO cpsNotificationVO = CPSNotificationVO.builder()
                    .defendantVO(getDefendantDetails(prosecutionCaseDefendantUpdated))
                    .defenceOrganisationVO(Optional.of(
                            DefenceOrganisationVO.builder()
                                    .addressLine1(updatedOrganisation.getAddressLine1())
                                    .email(updatedOrganisation.getEmail())
                                    .name(updatedOrganisation.getName())
                                    .addressLine2(updatedOrganisation.getAddressLine2())
                                    .addressLine3(updatedOrganisation.getAddressLine3())
                                    .addressLine4(updatedOrganisation.getAddressLine4())
                                    .phoneNumber(updatedOrganisation.getPhoneNumber())
                                    .postcode(updatedOrganisation.getAddressPostcode())
                                    .build()
                            )
                    )
                    .caseVO(Optional.of(CaseVO.builder()
                            .caseId(prosecutionCaseDefendantUpdated.getDefendant().getProsecutionCaseId())
                            .caseURN(nonNull(caseUrn) ? caseUrn : prosecutionCaseDefendantUpdated.getDefendant().getProsecutionAuthorityReference())
                            .build()))
                    .hearingVO(hearingVO)
                    .cpsEmailAddress(cpsEmailAddress.get())
                    .templateType(templateType)
                    .build();
            notificationService.sendCPSNotification(jsonEnvelope, cpsNotificationVO);
        } else {
            LOGGER.error("CPS notification email not found");
        }
    }


    private Optional<String> getCPSEmail(final JsonEnvelope jsonEnvelope, final UUID courtCentreId) {

        Optional<String> cpsEmail = Optional.empty();

        final Optional<JsonObject> organisationUnitJsonOptional = referenceDataService
                .getOrganisationUnitById(courtCentreId, jsonEnvelope, requester);

        if (organisationUnitJsonOptional.isPresent()) {
            cpsEmail = Optional.ofNullable(organisationUnitJsonOptional.get().getString("cpsEmailAddress", null));
            if (cpsEmail.isPresent()) {
                LOGGER.info("Found CPS email: {}", cpsEmail);
            }
            return cpsEmail;
        }
        return cpsEmail;
    }

    private Optional<DefendantVO> getDefendantDetails(final ProsecutionCaseDefendantUpdated prosecutionCaseDefendantUpdated) {

        final Optional<DefendantVO> defendantVO = empty();
        final Optional<PersonDefendant> personDefendantOptional = ofNullable(prosecutionCaseDefendantUpdated.getDefendant().getPersonDefendant());
        final Optional<LegalEntityDefendant> legalEntityDefendantOptional = ofNullable(prosecutionCaseDefendantUpdated.getDefendant().getLegalEntityDefendant());

        if (personDefendantOptional.isPresent()) {
            final Person person = personDefendantOptional.get().getPersonDetails();
            return Optional.of(DefendantVO.builder()
                    .firstName(person.getFirstName())
                    .lastName(person.getLastName())
                    .middleName(person.getMiddleName())
                    .build());
        } else if (legalEntityDefendantOptional.isPresent()) {
            return Optional.of(DefendantVO.builder()
                    .legalEntityName(legalEntityDefendantOptional.get()
                            .getOrganisation().getName())
                    .build());
        }
        return defendantVO;
    }

    private void sendDefendantUpdate(final JsonEnvelope envelope, final DefendantUpdate defendantUpdate, final UUID hearingId) {
        final JsonObject updateDefendantPayload = createObjectBuilder()
                .add(DEFENDANT, objectToJsonObjectConverter.convert(defendantUpdate))
                .add(HEARING_ID, hearingId.toString())
                .build();
        sender.send(
                Enveloper.envelop(updateDefendantPayload)
                        .withName(COMMAND_UPDATE_DEFENDANT_FOR_HEARING)
                        .withMetadataFrom(envelope));
    }

    private DefendantUpdate updateDefendant(final DefendantUpdate defendant) {
        return DefendantUpdate.defendantUpdate()
                .withId(defendant.getId())
                .withMasterDefendantId(defendant.getMasterDefendantId())
                .withProsecutionCaseId(defendant.getProsecutionCaseId())
                .withNumberOfPreviousConvictionsCited(defendant.getNumberOfPreviousConvictionsCited())
                .withProsecutionAuthorityReference(defendant.getProsecutionAuthorityReference())
                .withWitnessStatement(defendant.getWitnessStatement())
                .withWitnessStatementWelsh(defendant.getWitnessStatementWelsh())
                .withMitigation(defendant.getMitigation())
                .withMitigationWelsh(defendant.getMitigationWelsh())
                .withAssociatedPersons(defendant.getAssociatedPersons())
                .withDefenceOrganisation(defendant.getDefenceOrganisation())
                .withAssociatedDefenceOrganisation(defendant.getAssociatedDefenceOrganisation())
                .withPersonDefendant(defendant.getPersonDefendant())
                .withLegalEntityDefendant(defendant.getLegalEntityDefendant())
                .withPncId(defendant.getPncId())
                .withAliases(defendant.getAliases())
                .withIsYouth(defendant.getIsYouth())
                .build();
    }

    private Optional<JsonObject> getProsecutorById(final UUID prosecutorId, final JsonEnvelope envelope) {
        return referenceDataService.getProsecutor(envelope, prosecutorId, requester);
    }

    private void updateMatchedDefendantCustodialInformation(final JsonEnvelope jsonEnvelope, final DefendantCustodialInformationUpdateRequested defendantCustodialInformationUpdateRequested, final JsonObject matchedCases) {
        final JsonObjectBuilder updateMatchedDefendantCustodialInformationBuilder = JsonObjects.createObjectBuilder();
        final String matchedCaseIdString = matchedCases.getString(CASE_ID);
        updateMatchedDefendantCustodialInformationBuilder.add(CASE_ID, matchedCaseIdString);
        updateMatchedDefendantCustodialInformationBuilder.add(MASTER_DEFENDANT_ID, matchedCases.getString(MATCHED_MASTER_DEFENDANT_ID));
        if (nonNull(defendantCustodialInformationUpdateRequested.getCustodialEstablishment())) {
            updateMatchedDefendantCustodialInformationBuilder.add(CUSTODIAL_ESTABLISHMENT, objectToJsonObjectConverter.convert(defendantCustodialInformationUpdateRequested.getCustodialEstablishment()));
        }
        final JsonArrayBuilder defendantsArrayBuilder = JsonObjects.createArrayBuilder();
        matchedCases.getJsonArray(DEFENDANTS).getValuesAs(JsonObject.class).stream()
                .filter(defendant -> defendantCustodialInformationUpdateRequested.getMasterDefendantId().toString().equalsIgnoreCase(defendant.getString(MASTER_DEFENDANT_ID)))
                .filter(defendant -> shouldAvoidSameCaseSameDefendantId(matchedCaseIdString, defendant.getString("id"), defendantCustodialInformationUpdateRequested))
                .forEach(filteredDefendant -> defendantsArrayBuilder.add(filteredDefendant.getString("id")));
        final JsonArray defendantArray = defendantsArrayBuilder.build();
        if (isNotEmpty(defendantArray)) {
            updateMatchedDefendantCustodialInformationBuilder.add(DEFENDANTS, defendantArray);
            final JsonObject payload = updateMatchedDefendantCustodialInformationBuilder.build();
            sender.send(
                    Enveloper.envelop(payload)
                            .withName(PROGRESSION_COMMAND_UPDATE_DEFENDANT_CUSTODIAL_INFORMATION)
                            .withMetadataFrom(jsonEnvelope));
        }
    }

    private boolean shouldAvoidSameCaseSameDefendantId(final String matchedCaseIdString, final String matchedDefendantIdString, final DefendantCustodialInformationUpdateRequested defendantCustodialInformationUpdateRequested) {
        if (defendantCustodialInformationUpdateRequested.getProsecutionCaseId().toString().equalsIgnoreCase(matchedCaseIdString)) {
            return !matchedDefendantIdString.equalsIgnoreCase(defendantCustodialInformationUpdateRequested.getDefendantId().toString());
        }
        return true;
    }

}
