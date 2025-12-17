package uk.gov.moj.cpp.progression.processor;

import static java.lang.Boolean.parseBoolean;
import static java.util.Objects.nonNull;
import static java.util.UUID.fromString;
import static uk.gov.justice.services.core.enveloper.Enveloper.envelop;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.justice.services.messaging.JsonEnvelope.metadataFrom;

import uk.gov.justice.core.courts.Defendant;
import uk.gov.justice.core.courts.HearingDay;
import uk.gov.justice.core.courts.LegalEntityDefendant;
import uk.gov.justice.core.courts.Person;
import uk.gov.justice.core.courts.PersonDefendant;
import uk.gov.justice.core.courts.ProsecutionCase;
import uk.gov.justice.core.courts.HearingListingStatus;
import uk.gov.justice.progression.courts.GetHearingsAtAGlance;
import uk.gov.justice.progression.courts.Hearings;
import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.core.annotation.Component;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.requester.Requester;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.messaging.Metadata;
import uk.gov.moj.cpp.progression.domain.constant.DateTimeFormats;
import uk.gov.moj.cpp.progression.service.NotificationService;
import uk.gov.moj.cpp.progression.service.ProgressionService;
import uk.gov.moj.cpp.progression.service.RefDataService;
import uk.gov.moj.cpp.progression.service.UsersGroupService;
import uk.gov.moj.cpp.progression.value.object.CPSNotificationVO;
import uk.gov.moj.cpp.progression.value.object.CaseVO;
import uk.gov.moj.cpp.progression.value.object.DefendantVO;
import uk.gov.moj.cpp.progression.value.object.EmailTemplateType;
import uk.gov.moj.cpp.progression.value.object.HearingVO;

import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.json.JsonValue;

import lombok.extern.slf4j.Slf4j;

@SuppressWarnings({"java:S6204"})
@Slf4j
@ServiceComponent(Component.EVENT_PROCESSOR)
public class CPSEmailNotificationProcessor {

    private static final String PROGRESSION_COMMAND_FOR_DEFENCE_ORGANISATION_DISASSOCIATED = "progression.command.handler.disassociate-defence-organisation";
    private static final String IS_LAA = "isLAA";
    public static final String LINKED_APPLICATIONS = "linkedApplications";
    public static final String APPLICATION_ID = "applicationId";
    public static final String DEFENDANT_ID = "defendantId";
    public static final String ORGANISATION_ID = "organisationId";

    @Inject
    private JsonObjectToObjectConverter jsonObjectToObjectConverter;

    @Inject
    private ProgressionService progressionService;

    @Inject
    private RefDataService referenceDataService;

    @Inject
    private NotificationService notificationService;

    @Inject
    private UsersGroupService usersGroupService;

    @Inject
    private Requester requester;

    @Inject
    private Sender sender;

    @Handles("public.defence.defence-organisation-disassociated")
    public void processDisassociatedEmailNotification(final JsonEnvelope jsonEnvelope) {
        final JsonObject requestJson = jsonEnvelope.payloadAsJsonObject();
        final boolean isLAA = parseBoolean(requestJson.containsKey(IS_LAA) ? requestJson.get(IS_LAA).toString() : "false");
        final UUID caseId = fromString(requestJson.getString("caseId"));

        if (!isLAA) {
            sendCommandDisassociateDefenceOrganisation(jsonEnvelope, requestJson);
            sendCommandDisassociateDefenceOrganisationForApplication(jsonEnvelope, requestJson, caseId);
        }

        populateCPSNotification(jsonEnvelope, requestJson, EmailTemplateType.DISASSOCIATION);
    }

    private void sendCommandDisassociateDefenceOrganisation(final JsonEnvelope jsonEnvelope, final JsonObject requestJson) {
        final Metadata metadata = metadataFrom(jsonEnvelope.metadata()).withName(PROGRESSION_COMMAND_FOR_DEFENCE_ORGANISATION_DISASSOCIATED).build();
        sender.send(envelopeFrom(metadata, removeProperty(requestJson, IS_LAA)));
    }

    private void sendCommandDisassociateDefenceOrganisationForApplication(final JsonEnvelope jsonEnvelope, final JsonObject requestJson, final UUID caseId) {
        final Optional<JsonObject> activeApplicationsOnCaseOptional = progressionService.getActiveApplicationsOnCase(jsonEnvelope, caseId.toString());
        if (activeApplicationsOnCaseOptional.isPresent() && activeApplicationsOnCaseOptional.get().containsKey(LINKED_APPLICATIONS)){
            activeApplicationsOnCaseOptional.get().getJsonArray(LINKED_APPLICATIONS).forEach(linkedApplicationJson->{
                final JsonObject linkedApplicationJsonObject = (JsonObject) linkedApplicationJson;
                final String applicationId = linkedApplicationJsonObject.getString(APPLICATION_ID);
                final JsonObjectBuilder disassociateDefenceOrganisationForApplicationBuilder = Json.createObjectBuilder();
                if(nonNull(applicationId)){
                    disassociateDefenceOrganisationForApplicationBuilder
                            .add(APPLICATION_ID, applicationId)
                            .add(DEFENDANT_ID, requestJson.getString(DEFENDANT_ID))
                            .add(ORGANISATION_ID, requestJson.getString(ORGANISATION_ID));
                    sender.send(
                            envelop(disassociateDefenceOrganisationForApplicationBuilder.build())
                                    .withName("progression.command.handler.disassociate-defence-organisation-for-application")
                                    .withMetadataFrom(jsonEnvelope));
                }
            });
        }
    }

    @Handles("public.defence.event.record-instruction-details")
    public void processInstructedEmailNotification(final JsonEnvelope jsonEnvelope) {
        final JsonObject requestJson = jsonEnvelope.payloadAsJsonObject();
        final boolean isFirstInstruction = parseBoolean(requestJson.get("firstInstruction").toString());
        if (isFirstInstruction) {
            populateCPSNotification(jsonEnvelope, requestJson, EmailTemplateType.INSTRUCTION);
        }
    }

    private void populateCPSNotification(final JsonEnvelope jsonEnvelope, final JsonObject requestJson, final EmailTemplateType templateType) {
        final String caseId = requestJson.getString("caseId");
        final String defendantId = requestJson.getString(DEFENDANT_ID);
        final UUID organisationId = fromString(requestJson.getString(ORGANISATION_ID));

        final Optional<JsonObject> prosecutionCaseOptional = progressionService.getProsecutionCaseDetailById(jsonEnvelope, caseId);
        final Optional<HearingVO> hearingVO = getHearingDetails(prosecutionCaseOptional);
        final boolean isHearingPresent = hearingVO.isPresent() && hearingVO.get().getHearingDate() != null;

        if (isHearingPresent) {
            populateCPSNotificationAndSendEmail(jsonEnvelope, defendantId, organisationId, prosecutionCaseOptional,
                    hearingVO.get(), templateType);
        } else {
            log.info("Future hearing is not found for the case : {}", caseId);
        }
    }

    private void populateCPSNotificationAndSendEmail(final JsonEnvelope jsonEnvelope, final String defendantId,
                                                     final UUID organisationId, final Optional<JsonObject> prosecutionCaseOptional,
                                                     final HearingVO hearingVO,
                                                     final EmailTemplateType templateType) {

        final Optional<String> cpsEmailAddress = getCPSEmail(jsonEnvelope, hearingVO.getCourtCenterId());

        if (cpsEmailAddress.isPresent()) {

            final CPSNotificationVO cpsNotificationVO = CPSNotificationVO.builder()
                    .defendantVO(getDefendantDetails(defendantId, prosecutionCaseOptional))
                    .defenceOrganisationVO(usersGroupService.getDefenceOrganisationDetails(organisationId, jsonEnvelope.metadata()))
                    .caseVO(getCaseDetails(prosecutionCaseOptional))
                    .hearingVO(hearingVO)
                    .cpsEmailAddress(cpsEmailAddress.get())
                    .templateType(templateType)
                    .build();
            notificationService.sendCPSNotification(jsonEnvelope, cpsNotificationVO);
        } else {
            log.error("CPS notification email not found");
        }
    }

    private Optional<DefendantVO> getDefendantDetails(final String defendantId, final Optional<JsonObject> prosecutionCaseOptional) {

        final Optional<DefendantVO> defendantVO = Optional.empty();

        final JsonObject prosecutionCaseJson = prosecutionCaseOptional
                .orElseThrow(() -> new RuntimeException("Prosecution Case not found")).getJsonObject("prosecutionCase");

        final JsonObject defendantJson = getDefendantJson(prosecutionCaseJson, fromString(defendantId));

        final Defendant defendant = jsonObjectToObjectConverter.convert(defendantJson, Defendant.class);

        final Optional<PersonDefendant> personDefendantOptional = Optional.ofNullable(defendant.getPersonDefendant());
        final Optional<LegalEntityDefendant> legalEntityDefendantOptional = Optional.ofNullable(defendant.getLegalEntityDefendant());

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

    private Optional<CaseVO> getCaseDetails(final Optional<JsonObject> prosecutionCaseOptional) {

        final JsonObject prosecutionCaseJson = prosecutionCaseOptional
                .orElseThrow(() -> new RuntimeException("Prosecution Case not found")).getJsonObject("prosecutionCase");

        final ProsecutionCase prosecutionCase = jsonObjectToObjectConverter.convert(prosecutionCaseJson, ProsecutionCase.class);
        final String caseURN = prosecutionCase.getProsecutionCaseIdentifier().getCaseURN();
        final String prosecutionAuthorityReference = prosecutionCase.getProsecutionCaseIdentifier().getProsecutionAuthorityReference();

        return Optional.ofNullable(CaseVO.builder()
                .caseId(prosecutionCase.getId())
                .caseURN(Objects.nonNull(caseURN) ? caseURN : prosecutionAuthorityReference)
                .build());
    }

    private Optional<HearingVO> getHearingDetails(final Optional<JsonObject> prosecutionCaseOptional) {

        final Optional<HearingVO> hearingVO = Optional.empty();

        if (prosecutionCaseOptional.isPresent()) {

            final JsonObject hearingAtAGlanceJsonObject = prosecutionCaseOptional.get().getJsonObject("hearingsAtAGlance");
            final GetHearingsAtAGlance hearingAtAGlance = jsonObjectToObjectConverter.convert(hearingAtAGlanceJsonObject, GetHearingsAtAGlance.class);

            final List<Hearings> futureHearings = getFutureHearings(hearingAtAGlance);

            final Optional<Entry<UUID, ZonedDateTime>> resultMap = getEarliestHearing(futureHearings);

            if (resultMap.isPresent()) {
                log.info("Found result hearing {} with earliest date : {}", resultMap.get().getKey(), resultMap.get().getValue());
                final LocalDate localHearingDate = resultMap.get().getValue().toLocalDate();
                return getHearingVO(localHearingDate.format(DateTimeFormatter.ofPattern(DateTimeFormats.DATE_SLASHED_DD_MM_YYYY.getValue())), futureHearings, resultMap);
            }

        }
        return hearingVO;
    }

    private List<Hearings> getFutureHearings(GetHearingsAtAGlance hearingsAtAGlance) {

        long sendForListingStatusHearings = hearingsAtAGlance.getHearings().stream().filter(t -> t.getHearingListingStatus().equals(HearingListingStatus.SENT_FOR_LISTING))
                .count();
        log.info("Found SENT_FOR_LISTING status hearings size {} ", sendForListingStatusHearings );

        return hearingsAtAGlance.getHearings().stream()
                .filter(h -> (!h.getHearingListingStatus().equals(HearingListingStatus.SENT_FOR_LISTING) &&
                        h.getHearingDays().stream().anyMatch(hearingDay -> hearingDay.getSittingDay().toLocalDate().compareTo(LocalDate.now()) >= 0)))
                .toList();
    }

    private Optional<HearingVO> getHearingVO(final String hearingDate, List<Hearings> futureHearings, final Optional<Entry<UUID, ZonedDateTime>> resultMap) {
        final List<Hearings> resultHearing = futureHearings.stream()
                .filter(hearing -> hearing.getId().equals(resultMap.get().getKey()))
                .collect(Collectors.toList());

        final Hearings hearing = resultHearing.get(0);
        final String courtName = hearing.getCourtCentre().getName();
        final UUID courtCenterId = hearing.getCourtCentre().getId();
        return Optional.of(HearingVO.builder()
                .hearingDate(hearingDate)
                .courtCenterId(courtCenterId)
                .courtName(courtName)
                .build());
    }

    private Optional<Entry<UUID, ZonedDateTime>> getEarliestHearing(List<Hearings> futureHearings) {
        final Map<UUID, ZonedDateTime> hearingDaysMap = new HashMap<>();

        for (final Hearings hearings : futureHearings) {

            final List<HearingDay> futureHearingDays = hearings.getHearingDays().stream()
                    .filter(hd -> hd.getSittingDay().toLocalDate().isAfter(LocalDate.now()))
                    .collect(Collectors.toList());

            if (!futureHearingDays.isEmpty()) {
                hearingDaysMap.put(hearings.getId(), getEarliestHearingDay(futureHearingDays));
            }
        }
        return hearingDaysMap.entrySet()
                .stream().min(Entry.comparingByValue());
    }


    private Optional<String> getCPSEmail(final JsonEnvelope jsonEnvelope, final UUID courtCenterId) {

        Optional<String> cpsEmail = Optional.empty();

        final Optional<JsonObject> organisationUnitJsonOptional = referenceDataService
                .getOrganisationUnitById(courtCenterId, jsonEnvelope, requester);

        if (organisationUnitJsonOptional.isPresent()) {
            cpsEmail = Optional.ofNullable(organisationUnitJsonOptional.get().getString("cpsEmailAddress", null));
            if (cpsEmail.isPresent()) {
                log.info("Found CPS email: {}", cpsEmail);
            }
            return cpsEmail;
        }
        return cpsEmail;
    }

    private static ZonedDateTime getEarliestHearingDay(final List<HearingDay> hearingDays) {
        return hearingDays.stream()
                .map(HearingDay::getSittingDay)
                .sorted()
                .findFirst()
                .orElseThrow(IllegalArgumentException::new);
    }

    private JsonObject getDefendantJson(final JsonObject prosecutionCaseJson, final UUID defendantId) {
        return prosecutionCaseJson.getJsonArray("defendants").getValuesAs(JsonObject.class).stream()
                .filter(e -> defendantId.toString().equals(e.getString("id")))
                .findFirst()
                .orElseThrow(IllegalArgumentException::new);
    }

    private JsonObject removeProperty(final JsonObject origin, final String key) {
        final JsonObjectBuilder builder = Json.createObjectBuilder();
        for (final Map.Entry<String, JsonValue> entry : origin.entrySet()) {
            if (!entry.getKey().equals(key)) {
                builder.add(entry.getKey(), entry.getValue());
            }
        }
        return builder.build();
    }

}



