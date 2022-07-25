package uk.gov.moj.cpp.progression.service;

import static java.util.Comparator.comparing;
import static java.util.Objects.nonNull;
import static java.util.UUID.fromString;
import static java.util.UUID.randomUUID;

import uk.gov.justice.core.courts.CourtDocument;
import uk.gov.justice.core.courts.Defendant;
import uk.gov.justice.core.courts.HearingDay;
import uk.gov.justice.core.courts.HearingListingStatus;
import uk.gov.justice.core.courts.Personalisation;
import uk.gov.justice.core.courts.ProsecutionCase;
import uk.gov.justice.core.courts.notification.EmailChannel;
import uk.gov.justice.progression.courts.GetHearingsAtAGlance;
import uk.gov.justice.progression.courts.Hearings;
import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.core.annotation.Component;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.requester.Requester;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.material.url.MaterialUrlGenerator;
import uk.gov.moj.cpp.progression.domain.constant.DateTimeFormats;
import uk.gov.moj.cpp.progression.value.object.CPSNotificationVO;
import uk.gov.moj.cpp.progression.value.object.CaseVO;
import uk.gov.moj.cpp.progression.value.object.EmailTemplateType;
import uk.gov.moj.cpp.progression.value.object.HearingVO;

import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.json.JsonObject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CpsEmailNotificationService {

    private static final String HEARINGS_AT_A_GLANCE = "hearingsAtAGlance";
    private static final String URN = "URN";

    private static final Logger LOGGER = LoggerFactory.getLogger(CpsEmailNotificationService.class);

    @Inject
    private JsonObjectToObjectConverter jsonObjectConverter;

    @Inject
    private NotificationService notificationService;

    @Inject
    private MaterialUrlGenerator materialUrlGenerator;

    @Inject
    private ApplicationParameters applicationParameters;

    @Inject
    private RefDataService referenceDataService;

    @ServiceComponent(Component.EVENT_PROCESSOR)
    @Inject
    private Requester requester;


    public void sendEmailToCps(final JsonEnvelope envelope, final CourtDocument courtDocument, final UUID prosecutionCaseId, final JsonObject prosecutionCase) {
        final GetHearingsAtAGlance hearingsAtAGlance = jsonObjectConverter.
                convert(prosecutionCase.getJsonObject(HEARINGS_AT_A_GLANCE),
                        GetHearingsAtAGlance.class);
        final Optional<HearingVO> hearingVO = getHearingDetails(hearingsAtAGlance);
        final boolean isHearingPresent = hearingVO.isPresent() && hearingVO.get().getHearingDate() != null;

        if (isHearingPresent) {
            final UUID materialId = courtDocument.getMaterials().get(0).getId();
            final String materialUrl = materialUrlGenerator.fileStreamUrlFor(materialId);
            populateCPSNotificationAndSendEmail(envelope, courtDocument, prosecutionCase, materialUrl,
                    hearingVO.get(), prosecutionCaseId, materialId, EmailTemplateType.COURT_DOCUMENT);
        } else {
            if (LOGGER.isInfoEnabled()) {
                LOGGER.info("No hearing available for the case : {}", prosecutionCaseId);
            }
        }
    }

    private Optional<HearingVO> getHearingDetails(GetHearingsAtAGlance hearingsAtAGlance) {
        final List<Hearings> pastResultedHearings = getPastResultedHearings(hearingsAtAGlance);
        final Optional<Map.Entry<UUID, ZonedDateTime>> resultMapForPastHearings = getRecentHearing(pastResultedHearings);
        if (resultMapForPastHearings.isPresent()) {
            if (LOGGER.isInfoEnabled()) {
                LOGGER.info("Found resulted hearing {} with recent  date : {}", resultMapForPastHearings.get().getKey(), resultMapForPastHearings.get().getValue());
            }
            final LocalDate localHearingDate = resultMapForPastHearings.get().getValue().toLocalDate();
            return getHearingValueObject(localHearingDate.format(DateTimeFormatter.ofPattern(DateTimeFormats.DATE_SLASHED_DD_MM_YYYY.getValue())), pastResultedHearings, resultMapForPastHearings);
        } else {
            final List<Hearings> nonResultedHearing = getNonResultedHearing(hearingsAtAGlance);
            final Optional<Map.Entry<UUID, ZonedDateTime>> resultMapForEarliestHearings = getEarliestHearing(nonResultedHearing);
            if (resultMapForEarliestHearings.isPresent()) {
                final LocalDate localHearingDate = resultMapForEarliestHearings.get().getValue().toLocalDate();
                return getHearingValueObject(localHearingDate.format(DateTimeFormatter.ofPattern(DateTimeFormats.DATE_SLASHED_DD_MM_YYYY.getValue())), nonResultedHearing, resultMapForEarliestHearings);
            }

        }
        return Optional.empty();
    }

    private List<Hearings> getNonResultedHearing(GetHearingsAtAGlance hearingsAtAGlance) {
        return hearingsAtAGlance.getHearings().stream()
                .filter(h -> nonNull(h.getHearingListingStatus()))
                .filter(h -> !h.getHearingListingStatus().equals(HearingListingStatus.HEARING_RESULTED))
                .collect(Collectors.toList());
    }

    private List<Hearings> getPastResultedHearings(GetHearingsAtAGlance hearingsAtAGlance) {
        return hearingsAtAGlance.getHearings().stream()
                .filter(h -> nonNull(h.getHearingListingStatus()))
                .filter(h -> h.getHearingListingStatus().equals(HearingListingStatus.HEARING_RESULTED))
                .filter(h -> h.getHearingDays().stream()
                        .anyMatch(hearingDay -> hearingDay.getSittingDay().compareTo(ZonedDateTime.now()) <= 0
                        )).collect(Collectors.toList());
    }

    private Optional<HearingVO> getHearingValueObject(final String hearingDate, List<Hearings> hearings, final Optional<Map.Entry<UUID, ZonedDateTime>> resultMap) {
        final List<Hearings> resultHearing = hearings.stream()
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


    private Optional<Map.Entry<UUID, ZonedDateTime>> getRecentHearing(List<Hearings> pastHearings) {
        final Map<UUID, ZonedDateTime> hearingDaysMap = new HashMap<>();

        for (final Hearings hearings : pastHearings) {

            final List<HearingDay> pastHearingDays = hearings.getHearingDays().stream()
                    .filter(hd -> hd.getSittingDay().isBefore(ZonedDateTime.now()))
                    .collect(Collectors.toList());

            if (!pastHearingDays.isEmpty()) {
                hearingDaysMap.put(hearings.getId(), getRecentDate(pastHearingDays));
            }
        }
        return hearingDaysMap.entrySet()
                .stream().max(Map.Entry.comparingByValue());
    }


    private void populateCPSNotificationAndSendEmail(final JsonEnvelope jsonEnvelope, final CourtDocument courtDocument, final JsonObject prosecutionCase,
                                                     final String materialUrl, final HearingVO hearingVO,
                                                     final UUID prosecutionCaseId, final UUID materialId,
                                                     final EmailTemplateType templateType) {

        final Optional<String> cpsEmailAddress = getCPSEmail(jsonEnvelope, hearingVO.getCourtCenterId());

        if (cpsEmailAddress.isPresent()) {
            final UUID notificationId = randomUUID();

            final CPSNotificationVO cpsNotificationVO = CPSNotificationVO.builder()
                    .caseVO(getCaseDetails(prosecutionCase, courtDocument))
                    .hearingVO(hearingVO)
                    .cpsEmailAddress(cpsEmailAddress.get())
                    .templateType(templateType)
                    .build();
            notificationService.sendEmail(jsonEnvelope, notificationId, prosecutionCaseId, null, materialId, Collections.singletonList(buildEmailChannel(cpsNotificationVO, materialUrl)));
        } else {
            if (LOGGER.isErrorEnabled()) {
                LOGGER.error("CPS notification email not found");
            }
        }
    }

    private EmailChannel buildEmailChannel(final CPSNotificationVO cpsNotification, String materialUrl) {
        final EmailChannel.Builder emailChannelBuilder = EmailChannel.emailChannel();
        final Map<String, Object> personalisation = new HashMap<>();
        emailChannelBuilder.withSendToAddress(cpsNotification.getCpsEmailAddress());
        emailChannelBuilder.withMaterialUrl(materialUrl);
        cpsNotification.getCaseVO().ifPresent(caseVO -> {
            personalisation.put(URN, caseVO.getCaseURN());
            if (nonNull(caseVO.getDefendantList())) {
                personalisation.put("defendant_list", caseVO.getDefendantList());
                emailChannelBuilder.withTemplateId(fromString(applicationParameters.getCpsDefendantCourtDocumentTemplateId()));

            } else {
                emailChannelBuilder.withTemplateId(fromString(applicationParameters.getCpsCourtDocumentTemplateId()));

            }
        });
        emailChannelBuilder.withPersonalisation(new Personalisation(personalisation));
        return emailChannelBuilder.build();
    }

    private Optional<CaseVO> getCaseDetails(final JsonObject prosecutionCaseAsJsonObject, final CourtDocument courtDocument) {

        final ProsecutionCase prosecutionCase = jsonObjectConverter.convert(prosecutionCaseAsJsonObject.getJsonObject("prosecutionCase"), ProsecutionCase.class);
        final String caseURN = prosecutionCase.getProsecutionCaseIdentifier().getCaseURN();
        String defendantList = null;
        final String prosecutionAuthorityReference = prosecutionCase.getProsecutionCaseIdentifier().getProsecutionAuthorityReference();

        if (nonNull(courtDocument.getDocumentCategory().getDefendantDocument())) {
            final List<UUID> defendantIds = courtDocument.getDocumentCategory().getDefendantDocument().getDefendants();
            defendantList = prosecutionCase.getDefendants().stream()
                    .filter(defendant -> defendantIds.contains(defendant.getId()))
                    .map(this::concatenateDefendantFirstAndLastName)
                    .collect(Collectors.joining(","));

        }

        return Optional.ofNullable(CaseVO.builder()
                .caseId(prosecutionCase.getId())
                .caseURN(nonNull(caseURN) ? caseURN : prosecutionAuthorityReference)
                .defendantList(defendantList)
                .build());
    }

    private Optional<Map.Entry<UUID, ZonedDateTime>> getEarliestHearing(List<Hearings> nonResultedHearing) {
        final Map<UUID, ZonedDateTime> hearingDaysMap = new HashMap<>();

        for (final Hearings hearings : nonResultedHearing) {


            if (!hearings.getHearingDays().isEmpty()) {
                hearingDaysMap.put(hearings.getId(), getEarliestDate(hearings.getHearingDays()));
            }
        }
        return hearingDaysMap.entrySet()
                .stream().min(Map.Entry.comparingByValue());
    }

    private String concatenateDefendantFirstAndLastName(final Defendant defendant) {
        if (nonNull(defendant.getPersonDefendant()) && nonNull(defendant.getPersonDefendant().getPersonDetails())) {
            return defendant.getPersonDefendant().getPersonDetails().getFirstName() + " " + defendant.getPersonDefendant().getPersonDetails().getLastName();
        }
        return "";
    }

    private Optional<String> getCPSEmail(final JsonEnvelope jsonEnvelope, final UUID courtCenterId) {

        Optional<String> cpsEmail = Optional.empty();

        final Optional<JsonObject> organisationUnitJsonOptional = referenceDataService
                .getOrganisationUnitById(courtCenterId, jsonEnvelope, requester);

        if (organisationUnitJsonOptional.isPresent()) {
            cpsEmail = Optional.ofNullable(organisationUnitJsonOptional.get().getString("cpsEmailAddress"));
            if (LOGGER.isInfoEnabled()) {
                LOGGER.info("Found CPS email : {}", cpsEmail);
            }
            return cpsEmail;
        }
        return cpsEmail;
    }

    private static ZonedDateTime getEarliestDate(final List<HearingDay> hearingDays) {
        return hearingDays.stream()
                .map(HearingDay::getSittingDay)
                .sorted()
                .findFirst()
                .orElseThrow(IllegalArgumentException::new);
    }

    private static ZonedDateTime getRecentDate(final List<HearingDay> hearingDays) {
        return hearingDays.stream()
                .map(HearingDay::getSittingDay)
                .sorted(comparing(CpsEmailNotificationService::sittingDay).reversed())
                .findFirst()
                .orElseThrow(IllegalArgumentException::new);
    }

    private static ZonedDateTime sittingDay(ZonedDateTime sittingDay) {
        return sittingDay;
    }
}
