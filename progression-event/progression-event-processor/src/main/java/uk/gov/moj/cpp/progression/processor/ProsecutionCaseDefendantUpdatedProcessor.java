package uk.gov.moj.cpp.progression.processor;

import static java.util.Objects.nonNull;
import static java.util.Optional.empty;
import static java.util.Optional.ofNullable;
import static java.util.UUID.fromString;
import static javax.json.Json.createObjectBuilder;
import static uk.gov.justice.services.core.annotation.Component.EVENT_PROCESSOR;

import uk.gov.justice.core.courts.DefendantUpdate;
import uk.gov.justice.core.courts.HearingDay;
import uk.gov.justice.core.courts.LegalEntityDefendant;
import uk.gov.justice.core.courts.Person;
import uk.gov.justice.core.courts.PersonDefendant;
import uk.gov.justice.core.courts.ProsecutionCaseDefendantUpdated;
import uk.gov.justice.core.courts.UpdatedOrganisation;
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
import uk.gov.moj.cpp.progression.domain.constant.DateTimeFormats;
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

import javax.inject.Inject;
import javax.json.JsonObject;

import org.apache.commons.collections.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.gov.moj.cpp.progression.service.ProgressionService;

@SuppressWarnings({"squid:S3457", "squid:S3655"})
@ServiceComponent(EVENT_PROCESSOR)
public class ProsecutionCaseDefendantUpdatedProcessor {

    protected static final String PUBLIC_CASE_DEFENDANT_CHANGED = "public.progression.case-defendant-changed";
    protected static final String COMMAND_UPDATE_DEFENDANT_FOR_HEARING = "progression.command.update-defendant-for-hearing";
    private static final Logger LOGGER = LoggerFactory.getLogger(ProsecutionCaseDefendantUpdatedProcessor.class.getCanonicalName());
    private static final String HEARING_ID = "hearingId";
    private static final String CPS_FLAG = "cpsFlag";

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
                .add("defendant", objectToJsonObjectConverter.convert(updateDefendant(defendant))).build();
        sender.send(enveloper.withMetadataFrom(jsonEnvelope, PUBLIC_CASE_DEFENDANT_CHANGED).apply(publicEventPayload));

        if (nonNull(hearingIds)) {
            hearingIds.forEach(hearingId ->
                    sendDefendantUpdate(jsonEnvelope, defendant, hearingId));
        }

        if (nonNull(prosecutionCaseDefendantUpdated.getProsecutionAuthorityId()) && nonNull(prosecutionCaseDefendantUpdated.getUpdatedOrganisation())) {
            final UUID prosecutorId = fromString(prosecutionCaseDefendantUpdated.getProsecutionAuthorityId());
            final Optional<JsonObject> prosecutorDetails = getProsecutorById(prosecutorId, jsonEnvelope);
            if (prosecutorDetails.isPresent()) {
                final JsonObject prosecutorsJsonObject = prosecutorDetails.get();
                final boolean isCpsProsecutor = prosecutorsJsonObject.getBoolean(CPS_FLAG, false);
                if (isCpsProsecutor) {
                    sendDefendantAssociationCPSNotification(jsonEnvelope, prosecutionCaseDefendantUpdated, EmailTemplateType.ASSOCIATION);
                }
            }
        }
    }

    @Handles("progression.event.hearing-defendant-updated")
    public void handleHearingDefendantUpdatedEvent(final JsonEnvelope jsonEnvelope) {
        progressionService.populateHearingToProbationCaseworker(jsonEnvelope, fromString(jsonEnvelope.payloadAsJsonObject().getString(HEARING_ID)));
    }


    private void sendDefendantAssociationCPSNotification(final JsonEnvelope jsonEnvelope, final ProsecutionCaseDefendantUpdated prosecutionCaseDefendantUpdatedfinal, final EmailTemplateType templateType) {
        final String caseId = prosecutionCaseDefendantUpdatedfinal.getDefendant().getProsecutionCaseId().toString();
        final Optional<JsonObject> prosecutionCaseOptional = progressionService.getProsecutionCaseDetailById(jsonEnvelope, caseId);
        final Optional<HearingVO> hearingVO = getHearingDetails(prosecutionCaseOptional);
        final boolean isHearingPresent = hearingVO.isPresent() && hearingVO.get().getHearingDate() != null;

        if (isHearingPresent) {
            populateCPSNotificationAndSendEmail(jsonEnvelope, prosecutionCaseDefendantUpdatedfinal,
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
        return hearingsAtAGlance.getHearings().stream().filter(h -> h.getHearingDays().stream()
                .anyMatch(hearingDay -> hearingDay.getSittingDay().toLocalDate().compareTo(LocalDate.now()) >= 0
                )).collect(Collectors.toList());
    }

    private Optional<Map.Entry<UUID, ZonedDateTime>> getEarliestHearing(List<Hearings> futureHearings) {
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
                .stream().min(Map.Entry.comparingByValue());
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
                .add("defendant", objectToJsonObjectConverter.convert(defendantUpdate))
                .add(HEARING_ID, hearingId.toString())
                .build();
        sender.send(enveloper.withMetadataFrom(envelope, COMMAND_UPDATE_DEFENDANT_FOR_HEARING).apply(updateDefendantPayload));
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

}
