package uk.gov.moj.cpp.progression.query.view.service;

import static java.util.Comparator.comparing;
import static java.util.Objects.nonNull;
import static org.apache.commons.collections.CollectionUtils.isNotEmpty;
import static org.apache.commons.lang3.StringUtils.EMPTY;

import uk.gov.justice.core.courts.Defendant;
import uk.gov.justice.core.courts.Hearing;
import uk.gov.justice.core.courts.HearingDay;
import uk.gov.justice.core.courts.PersonDefendant;
import uk.gov.justice.core.courts.ProsecutionCase;
import uk.gov.justice.progression.query.CotrDefendant;
import uk.gov.justice.progression.query.CotrDetail;
import uk.gov.justice.progression.query.DefenceAdditionalInfo;
import uk.gov.justice.progression.query.ProsecutionAdditionalInfo;
import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.common.converter.ListToJsonArrayConverter;
import uk.gov.justice.services.common.converter.StringToJsonObjectConverter;
import uk.gov.moj.cpp.progression.query.utils.StringToJsonArray;
import uk.gov.moj.cpp.prosecutioncase.persistence.entity.COTRDefenceFurtherInfoEntity;
import uk.gov.moj.cpp.prosecutioncase.persistence.entity.COTRDefendantEntity;
import uk.gov.moj.cpp.prosecutioncase.persistence.entity.COTRDetailsEntity;
import uk.gov.moj.cpp.prosecutioncase.persistence.entity.COTRProsecutionFurtherInfoEntity;
import uk.gov.moj.cpp.prosecutioncase.persistence.entity.HearingEntity;
import uk.gov.moj.cpp.prosecutioncase.persistence.entity.ProsecutionCaseEntity;
import uk.gov.moj.cpp.prosecutioncase.persistence.repository.COTRDefenceFurtherInfoRepository;
import uk.gov.moj.cpp.prosecutioncase.persistence.repository.COTRDefendantRepository;
import uk.gov.moj.cpp.prosecutioncase.persistence.repository.COTRDetailsRepository;
import uk.gov.moj.cpp.prosecutioncase.persistence.repository.COTRProsecutionFurtherInfoRepository;
import uk.gov.moj.cpp.prosecutioncase.persistence.repository.HearingRepository;
import uk.gov.moj.cpp.prosecutioncase.persistence.repository.ProsecutionCaseRepository;

import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import javax.inject.Inject;
import uk.gov.justice.services.messaging.JsonObjects;
import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;

import com.google.common.base.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CotrQueryService {

    private static final Logger LOGGER = LoggerFactory.getLogger(CotrQueryService.class.getCanonicalName());

    @Inject
    private COTRDetailsRepository cotrDetailsRepository;

    @Inject
    private COTRDefendantRepository cotrDefendantRepository;

    @Inject
    private ProsecutionCaseRepository prosecutionCaseRepository;

    @Inject
    private HearingRepository hearingRepository;

    @Inject
    private COTRProsecutionFurtherInfoRepository cotrProsecutionFurtherInfoRepository;

    @Inject
    private COTRDefenceFurtherInfoRepository cotrDefenceFurtherInfoRepository;

    @Inject
    private StringToJsonObjectConverter stringToJsonObjectConverter;

    @Inject
    private StringToJsonArray stringToJsonArray;

    @Inject
    private ListToJsonArrayConverter listToJsonArrayConverter;

    @Inject
    private JsonObjectToObjectConverter jsonObjectToObjectConverter;

    public List<CotrDetail> getCotrDetailsForAProsecutionCaseByLatestHearingDate(final UUID prosecutionCaseId) {

        final List<CotrDetail> cotrDetails = new ArrayList<>();
        final List<COTRDetailsEntity> cotrDetailsEntities = cotrDetailsRepository.findByProsecutionCaseId(prosecutionCaseId);
        for (final COTRDetailsEntity cotrDetailsEntity : cotrDetailsEntities) {
            final HearingEntity hearingEntity = hearingRepository.findBy(cotrDetailsEntity.getHearingId());
            final JsonObject hearingJson = stringToJsonObjectConverter.convert(hearingEntity.getPayload());
            final Hearing hearing = jsonObjectToObjectConverter.convert(hearingJson, Hearing.class);
            final ZonedDateTime hearingDate = getLatestHearingDate(hearing.getHearingDays());

            final List<COTRDefendantEntity> defendantEntities = cotrDefendantRepository.findByCotrId(cotrDetailsEntity.getId());
            final List<UUID> defendantsInCotr = defendantEntities.stream()
                    .map(COTRDefendantEntity::getDefendantId)
                    .collect(Collectors.toList());

            final CotrDetail cotrDetail = CotrDetail.cotrDetail()
                    .withId(cotrDetailsEntity.getId())
                    .withHearingId(cotrDetailsEntity.getHearingId())
                    .withHearingDay(hearingDate)
                    .withCotrDefendants(createCotrDefendant(prosecutionCaseId, hearing.getProsecutionCases(), defendantsInCotr))
                    .withIsArchived(nonNull(cotrDetailsEntity.getArchived()))
                    .build();
            cotrDetails.add(cotrDetail);
        }
        return cotrDetails;

    }

    public List<CotrDetail> getCotrDetailsForAProsecutionCase(final UUID prosecutionCaseId) {

        final List<CotrDetail> cotrDetails = new ArrayList<>();
        final List<COTRDetailsEntity> cotrDetailsEntities = cotrDetailsRepository.findByProsecutionCaseId(prosecutionCaseId);
        for (final COTRDetailsEntity cotrDetailsEntity : cotrDetailsEntities) {
            final HearingEntity hearingEntity = hearingRepository.findBy(cotrDetailsEntity.getHearingId());
            final JsonObject hearingJson = stringToJsonObjectConverter.convert(hearingEntity.getPayload());
            final Hearing hearing = jsonObjectToObjectConverter.convert(hearingJson, Hearing.class);
            final ZonedDateTime hearingDate = getEarliestHearingDate(hearing.getHearingDays());

            final List<COTRDefendantEntity> defendantEntities = cotrDefendantRepository.findByCotrId(cotrDetailsEntity.getId());
            final List<UUID> defendantsInCotr = defendantEntities.stream()
                    .map(COTRDefendantEntity::getDefendantId)
                    .collect(Collectors.toList());

            final CotrDetail cotrDetail = CotrDetail.cotrDetail()
                    .withId(cotrDetailsEntity.getId())
                    .withHearingId(cotrDetailsEntity.getHearingId())
                    .withHearingDay(hearingDate)
                    .withCotrDefendants(createCotrDefendant(prosecutionCaseId, hearing.getProsecutionCases(), defendantsInCotr))
                    .withIsArchived(nonNull(cotrDetailsEntity.getArchived()) ? cotrDetailsEntity.getArchived() : false)
                    .build();
            cotrDetails.add(cotrDetail);
        }
        return cotrDetails;

    }

    public JsonObject getCotrFormForAProsecutionCaseAndCotr(final UUID prosecutionCaseId, final UUID cotrId) {
        final COTRDetailsEntity cotrDetailsEntity = cotrDetailsRepository.findBy(cotrId);
        if (nonNull(cotrDetailsEntity)) {
            final HearingEntity hearingEntity = hearingRepository.findBy(cotrDetailsEntity.getHearingId());
            final JsonObject hearingJson = stringToJsonObjectConverter.convert(hearingEntity.getPayload());
            final Hearing hearing = jsonObjectToObjectConverter.convert(hearingJson, Hearing.class);
            final ZonedDateTime hearingDate = getEarliestHearingDate(hearing.getHearingDays());

            final List<COTRDefendantEntity> defendantEntities = cotrDefendantRepository.findByCotrId(cotrDetailsEntity.getId());
            final List<UUID> defendantsInCotr = defendantEntities.stream()
                    .map(COTRDefendantEntity::getDefendantId)
                    .collect(Collectors.toList());
            final ProsecutionCaseEntity prosecutionCaseEntity = prosecutionCaseRepository.findByCaseId(prosecutionCaseId);
            final ProsecutionCase prosecutionCase = jsonObjectToObjectConverter.convert(stringToJsonObjectConverter.convert(prosecutionCaseEntity.getPayload()), ProsecutionCase.class);
            final JsonObjectBuilder jsonObjectBuilder = JsonObjects.createObjectBuilder();
            final JsonObject prosecutionFormData = Strings.isNullOrEmpty(cotrDetailsEntity.getProsecutionFormData()) ? null : stringToJsonObjectConverter.convert(cotrDetailsEntity.getProsecutionFormData());

            addAttribute(jsonObjectBuilder, "id", cotrDetailsEntity.getId().toString());
            addAttribute(jsonObjectBuilder, "caseId", cotrDetailsEntity.getProsecutionCaseId().toString());
            addAttribute(jsonObjectBuilder, "hearingId", cotrDetailsEntity.getHearingId().toString());
            addAttribute(jsonObjectBuilder, "caseUrn", getCaseUrn(prosecutionCase));
            addAttribute(jsonObjectBuilder, "hearingDay", hearingDate.toLocalDate().toString());
            addAttribute(jsonObjectBuilder, "listedDurationMinutes", getListedDurationMinutes(hearing.getHearingDays()));
            addAttribute(jsonObjectBuilder, "cotrDefendants", listToJsonArrayConverter.convert(createCotrDefendantWithAdditionalInfo(prosecutionCaseId, hearing.getProsecutionCases(), defendantsInCotr, cotrId)));
            addAttribute(jsonObjectBuilder, "prosecutionFormData", prosecutionFormData);
            addAttribute(jsonObjectBuilder, "prosecutionAdditionalInfo", listToJsonArrayConverter.convert(addProsecutionAdditionalInfo(cotrId)));
            addAttribute(jsonObjectBuilder, "caseProgressionReviewNotes", stringToJsonArray.convert(cotrDetailsEntity.getCaseProgressionReviewNote()));
            addAttribute(jsonObjectBuilder, "judgeReviewNotes", stringToJsonArray.convert(cotrDetailsEntity.getJudgeReviewNotes()));
            addAttribute(jsonObjectBuilder, "listingReviewNotes", stringToJsonArray.convert(cotrDetailsEntity.getListingReviewNotes()));
            return jsonObjectBuilder.build();
        }
        return null;
    }

    private void addAttribute(JsonObjectBuilder jsonObjectBuilder, String key, String value) {
        if (!Strings.isNullOrEmpty(value)) {
            jsonObjectBuilder.add(key, value);
        }
    }

    private void addAttribute(JsonObjectBuilder jsonObjectBuilder, String key, Integer value) {
        if (value != null) {
            jsonObjectBuilder.add(key, value);
        }
    }

    private void addAttribute(JsonObjectBuilder jsonObjectBuilder, String key, JsonArray value) {
        if (value != null) {
            jsonObjectBuilder.add(key, value);
        }
    }

    private void addAttribute(JsonObjectBuilder jsonObjectBuilder, String key, JsonObject value) {
        if (value != null) {
            jsonObjectBuilder.add(key, value);
        }
    }

    private String getCaseUrn(final ProsecutionCase prosecutionCase) {
        if (nonNull(prosecutionCase)) {
            final String urn = prosecutionCase.getProsecutionCaseIdentifier().getCaseURN();
            return Strings.isNullOrEmpty(urn) ? prosecutionCase.getProsecutionCaseIdentifier().getProsecutionAuthorityReference() : urn;
        }
        return EMPTY;
    }

    private List<ProsecutionAdditionalInfo> addProsecutionAdditionalInfo(final UUID cotrId) {
        final List<COTRProsecutionFurtherInfoEntity> cotrProsecutionFurtherInfoEntities = cotrProsecutionFurtherInfoRepository.findByCotrId(cotrId);
        return cotrProsecutionFurtherInfoEntities.stream()
                .map(cotrProsecutionFurtherInfoEntity ->
                        ProsecutionAdditionalInfo.prosecutionAdditionalInfo()
                                .withId(cotrProsecutionFurtherInfoEntity.getId())
                                .withAddedBy(cotrProsecutionFurtherInfoEntity.getInfoAddedBy())
                                .withAddedOn(cotrProsecutionFurtherInfoEntity.getAddedOn().toLocalDate())
                                .withAddedByName(cotrProsecutionFurtherInfoEntity.getInfoAddedByName())
                                .withIsCertificationReady(cotrProsecutionFurtherInfoEntity.getIsCertificationReady())
                                .withInformation(cotrProsecutionFurtherInfoEntity.getFurtherInformation())
                                .build())
                .collect(Collectors.toList());
    }

    private List<CotrDefendant> createCotrDefendantWithAdditionalInfo(final UUID prosecutionCaseId, final List<ProsecutionCase> prosecutionCases
            , final List<UUID> defendantsInCotr, final UUID cotrId) {
        LOGGER.info("Create defendants for prosecution case {}", prosecutionCaseId);
        final List<CotrDefendant> cotrDefendants = new ArrayList<>();
        if (isNotEmpty(prosecutionCases)) {
            final ProsecutionCase prosecutionCase = prosecutionCases.stream()
                    .filter(pc -> nonNull(pc) && prosecutionCaseId.equals(pc.getId()))
                    .findFirst()
                    .orElse(null);
            if (nonNull(prosecutionCase)) {
                addDefendantsToCotrForm(prosecutionCase.getDefendants(), cotrDefendants, defendantsInCotr, cotrId);
            }
        }
        return cotrDefendants;
    }

    private void addDefendantsToCotrForm(final List<Defendant> defendants, final List<CotrDefendant> defendantsList, final List<UUID> defendantsInCotr, final UUID cotrId) {
        defendants.forEach(defendant -> {
            if (defendantsInCotr.contains(defendant.getId())) {
                final List<COTRDefendantEntity> cotrDefendantEntities = cotrDefendantRepository.findByCotrIdAndDefendantId(cotrId, defendant.getId());
                final COTRDefendantEntity cotrDefendantEntity = cotrDefendantEntities.stream()
                        .filter(p -> p.getDefendantId().equals(defendant.getId()))
                        .findFirst()
                        .orElse(null);
                defendantsList.add(getCotrDefendant(defendant, cotrDefendantEntity));
            }
        });
    }

    private CotrDefendant getCotrDefendant(final Defendant defendant, final COTRDefendantEntity cotrDefendantEntity) {
        return CotrDefendant.cotrDefendant()
                .withId(defendant.getId())
                .withFirstName(getDefendantFirstName(defendant.getPersonDefendant()))
                .withLastName(getDefendantLastName(defendant.getPersonDefendant()))
                .withDefendantNumber(cotrDefendantEntity.getdNumber())
                .withDateOfBirth(getDefendantDateOfBirth(defendant.getPersonDefendant()))
                .withDefenceFormData(cotrDefendantEntity.getDefendantForm())
                .withServedBy(cotrDefendantEntity.getServedByName())
                .withServedOn(cotrDefendantEntity.getServedOn() != null ? cotrDefendantEntity.getServedOn().toLocalDate().toString() : null)
                .withDefenceAdditionalInfo(addDefenceAdditionalInfo(defendant.getId()))
                .build();
    }

    private List<DefenceAdditionalInfo> addDefenceAdditionalInfo(final UUID defendantId) {
        final List<COTRDefenceFurtherInfoEntity> cotrDefenceFurtherInfoEntities = cotrDefenceFurtherInfoRepository.findByCotrDefendantId(defendantId);
        return cotrDefenceFurtherInfoEntities.stream()
                .map(cotrDefenceFurtherInfoEntity -> DefenceAdditionalInfo.defenceAdditionalInfo()
                        .withId(cotrDefenceFurtherInfoEntity.getId())
                        .withInformation(cotrDefenceFurtherInfoEntity.getFurtherInformation())
                        .withIsCertificationReady(cotrDefenceFurtherInfoEntity.getIsCertificationReady())
                        .withAddedOn(cotrDefenceFurtherInfoEntity.getAddedOn().toLocalDate())
                        .withAddedBy(cotrDefenceFurtherInfoEntity.getInfoAddedBy())
                        .withAddedByName(cotrDefenceFurtherInfoEntity.getInfoAddedByName())
                        .build())
                .collect(Collectors.toList());
    }

    private List<CotrDefendant> createCotrDefendant(final UUID prosecutionCaseId, final List<ProsecutionCase> prosecutionCases, final List<UUID> defendantsInCotr) {
        LOGGER.info("Create defendants for prosecution case {}", prosecutionCaseId);
        final List<CotrDefendant> cotrDefendants = new ArrayList<>();
        if (isNotEmpty(prosecutionCases)) {
            final ProsecutionCase prosecutionCase = prosecutionCases.stream()
                    .filter(pc -> nonNull(pc) && prosecutionCaseId.equals(pc.getId()))
                    .findFirst()
                    .orElse(null);
            if (nonNull(prosecutionCase)) {
                addDefendantsToCotrDetail(prosecutionCase.getDefendants(), cotrDefendants, defendantsInCotr);
            }
        }
        return cotrDefendants;
    }

    private static void addDefendantsToCotrDetail(final List<Defendant> defendants, final List<CotrDefendant> defendantsList, final List<UUID> defendantsInCotr) {
        defendants.forEach(defendant -> {
            if (defendantsInCotr.contains(defendant.getId())) {
                final CotrDefendant cotrDefendants = CotrDefendant.cotrDefendant()
                        .withId(defendant.getId())
                        .withFirstName(getDefendantFirstName(defendant.getPersonDefendant()))
                        .withLastName(getDefendantLastName(defendant.getPersonDefendant()))
                        .withDateOfBirth(getDefendantDateOfBirth(defendant.getPersonDefendant()))
                        .build();
                defendantsList.add(cotrDefendants);
            }
        });
    }

    @SuppressWarnings("squid:S3655")
    private static ZonedDateTime getEarliestHearingDate(final List<HearingDay> hearingDays) {
        LOGGER.info("Get earliest hearing date");
        return hearingDays.stream()
                .min(comparing(HearingDay::getSittingDay))
                .get()
                .getSittingDay();
    }

    @SuppressWarnings("squid:S3655")
    private static ZonedDateTime getLatestHearingDate(final List<HearingDay> hearingDays) {
        LOGGER.info("Get latest hearing date");
        return hearingDays.stream()
                .max(comparing(HearingDay::getSittingDay))
                .get()
                .getSittingDay();
    }

    @SuppressWarnings("squid:S3655")
    private static Integer getListedDurationMinutes(final List<HearingDay> hearingDays) {
        LOGGER.info("Get listed duration in minutes");
        return hearingDays.stream()
                .min(comparing(HearingDay::getSittingDay))
                .get()
                .getListedDurationMinutes();
    }

    private static LocalDate getDefendantDateOfBirth(final PersonDefendant personDefendant) {
        if (nonNull(personDefendant) && nonNull(personDefendant.getPersonDetails())) {
            return personDefendant.getPersonDetails().getDateOfBirth();
        }
        return null;
    }

    private static String getDefendantFirstName(final PersonDefendant personDefendant) {
        if (nonNull(personDefendant) && nonNull(personDefendant.getPersonDetails())) {
            return personDefendant.getPersonDetails().getFirstName();
        }
        return EMPTY;
    }

    private static String getDefendantLastName(final PersonDefendant personDefendant) {
        if (nonNull(personDefendant) && nonNull(personDefendant.getPersonDetails())) {
            return personDefendant.getPersonDetails().getLastName();
        }
        return EMPTY;
    }

}
