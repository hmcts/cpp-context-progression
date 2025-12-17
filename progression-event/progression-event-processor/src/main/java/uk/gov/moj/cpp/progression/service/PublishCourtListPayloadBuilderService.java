package uk.gov.moj.cpp.progression.service;

import static java.lang.Boolean.TRUE;
import static java.lang.String.format;
import static java.time.format.DateTimeFormatter.ofPattern;
import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static java.util.Optional.ofNullable;
import static java.util.UUID.fromString;
import static org.apache.commons.lang3.StringUtils.isNotEmpty;
import static uk.gov.justice.core.courts.LjaDetails.ljaDetails;
import static uk.gov.justice.listing.courts.PublishCourtListType.FIRM;
import static uk.gov.justice.listing.courts.PublishCourtListType.WARN;
import static uk.gov.justice.services.core.annotation.Component.EVENT_PROCESSOR;
import static uk.gov.moj.cpp.progression.domain.constant.DateTimeFormats.DATE_WITH_TIME;
import static uk.gov.moj.cpp.progression.domain.constant.DateTimeFormats.TIME_HMMA;
import static uk.gov.moj.cpp.progression.service.payloads.PublishCourtListAddress.publishCourtListAddressBuilder;
import static uk.gov.moj.cpp.progression.service.payloads.PublishCourtListAddressee.publishCourtListAddresseeBuilder;
import static uk.gov.moj.cpp.progression.service.payloads.PublishCourtListByCourtroom.publishCourtListHearingByCourtroomBuilder;
import static uk.gov.moj.cpp.progression.service.payloads.PublishCourtListByDate.publishCourtListHearingByDateBuilder;
import static uk.gov.moj.cpp.progression.service.payloads.PublishCourtListByTime.publishCourtListHearingByTimeBuilder;
import static uk.gov.moj.cpp.progression.service.payloads.PublishCourtListByWeekCommencingDate.publishCourtListHearingByWeekCommencingDateBuilder;
import static uk.gov.moj.cpp.progression.service.payloads.PublishCourtListCourtAddress.publishCourtListCourtAddressBuilder;
import static uk.gov.moj.cpp.progression.service.payloads.PublishCourtListDefendant.publishCourtListDefendantBuilder;
import static uk.gov.moj.cpp.progression.service.payloads.PublishCourtListForWeekCommencingByCourtroom.publishCourtListForWeekCommencingByCourtroomBuilder;
import static uk.gov.moj.cpp.progression.service.payloads.PublishCourtListHearing.publishCourtListHearingBuilder;
import static uk.gov.moj.cpp.progression.service.payloads.PublishCourtListPayload.publishCourtListPayloadBuilder;
import static uk.gov.moj.cpp.progression.service.utils.DefendantDetailsExtractor.getDefendantDateOfBirth;
import static uk.gov.moj.cpp.progression.service.utils.DefendantDetailsExtractor.getDefendantFullName;
import static uk.gov.moj.cpp.progression.service.utils.DefendantDetailsExtractor.getDefendantLastName;

import uk.gov.justice.core.courts.Defendant;
import uk.gov.justice.core.courts.LjaDetails;
import uk.gov.justice.core.courts.ProsecutionCase;
import uk.gov.justice.listing.courts.CourtListPublished;
import uk.gov.justice.listing.courts.CourtLists;
import uk.gov.justice.listing.courts.Hearings;
import uk.gov.justice.listing.courts.PublishCourtListType;
import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.common.converter.LocalDates;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.requester.Requester;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.progression.processor.exceptions.InvalidHearingTimeException;
import uk.gov.moj.cpp.progression.service.payloads.AssociatedDefenceOrganisation;
import uk.gov.moj.cpp.progression.service.payloads.PublishCourtListAddress;
import uk.gov.moj.cpp.progression.service.payloads.PublishCourtListAddressee;
import uk.gov.moj.cpp.progression.service.payloads.PublishCourtListByWeekCommencingDate;
import uk.gov.moj.cpp.progression.service.payloads.PublishCourtListCourtAddress;
import uk.gov.moj.cpp.progression.service.payloads.PublishCourtListCourtAddress.PublishCourtListCourtAddressBuilder;
import uk.gov.moj.cpp.progression.service.payloads.PublishCourtListDefendant.PublishCourtListDefendantBuilder;
import uk.gov.moj.cpp.progression.service.payloads.PublishCourtListHearing;
import uk.gov.moj.cpp.progression.service.payloads.PublishCourtListHearing.PublishCourtListHearingBuilder;
import uk.gov.moj.cpp.progression.service.payloads.PublishCourtListPayload.PublishCourtListPayloadBuilder;

import java.time.DateTimeException;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import javax.inject.Inject;
import javax.json.JsonArray;
import javax.json.JsonObject;

import org.apache.commons.collections.CollectionUtils;

@SuppressWarnings({"squid:S00107"})
public class PublishCourtListPayloadBuilderService {

    public static final String ADDRESS_2 = "address2";
    private static final ZoneId UK_TIME_ZONE = ZoneId.of("Europe/London");
    private static final DateTimeFormatter TIME_FORMATTER = ofPattern(TIME_HMMA.getValue());
    private static final DateTimeFormatter LOCAL_DATE_TIME_FORMATTER = ofPattern(DATE_WITH_TIME.getValue()).withZone(UK_TIME_ZONE);
    private static final String DEFAULT_HEARING_START_TIME = "10:30 AM";

    private static final String LJA = "lja";
    private static final String LOCAL_JUSTICE_AREA = "localJusticeArea";
    private static final String NATIONAL_COURT_CODE = "nationalCourtCode";
    private static final String NAME = "name";
    private static final String NAME_WELSH = "welshName";
    private static final String EMPTY = "";

    public static final String CONTACT_EMAIL_ADDRESS = "contactEmailAddress";
    public static final String CONTACT_PERSON_NAME = "contactPersonName";
    public static final String CONTACT_ORGANISATION_NAME = "contactOrganisationName";
    public static final String PRIMARY_EMAIL = "primaryEmail";
    public static final String DEFENCE_ADVOCATE = "Defence advocate";
    public static final String CONTACTS = "contacts";
    public static final String CONTACT_TYPE = "contactType";
    public static final String CONTACT_TYPE_MAPPING = "contactTypeMapping";
    public static final String DEFENDANT_IDS = "defendantIds";
    public static final String ADDRESS = "address";
    public static final String ADDRESS_5 = "address5";
    public static final String ADDRESS_1 = "address1";
    public static final String ADDRESS_3 = "address3";
    public static final String ADDRESS_4 = "address4";
    public static final String POSTCODE = "postcode";

    @Inject
    private RefDataService referenceDataService;

    @Inject
    private ProgressionService progressionService;

    @Inject
    private DefenceService defenceService;

    @Inject
    private CorrespondenceService correspondenceService;

    @Inject
    private JsonObjectToObjectConverter jsonObjectToObjectConverter;

    @Inject
    @ServiceComponent(EVENT_PROCESSOR)
    private Requester requester;

    public void buildPayloadForInterestedParties(final JsonEnvelope event, final CourtListPublished courtListPublished,
                                                 final Map<String, PublishCourtListPayloadBuilder> defenceOrganisationPayloadBuilderByName,
                                                 final Map<String, PublishCourtListPayloadBuilder> prosecutionPayloadBuilderByName,
                                                 final Map<String, PublishCourtListPayloadBuilder> defenceAdvocatePayloadBuilderByName) {
        final PublishCourtListType publishCourtListType = courtListPublished.getPublishCourtListType();

        final JsonObject courtCentreJson = referenceDataService.getCourtCentreWithCourtRoomsById(courtListPublished.getCourtCentreId(), event, requester)
                .orElseThrow(() -> new IllegalArgumentException(format("Organisation unit details not found for court centre id %s from reference data", courtListPublished.getCourtCentreId())));
        final JsonObject enforcementArea = referenceDataService.getEnforcementAreaByLjaCode(event, courtCentreJson.getString(LJA), requester);
        final LjaDetails ljaDetails = buildLjaDetails(enforcementArea);

        final Map<UUID, ProsecutionCase> prosecutionCasesByCaseId = new HashMap<>();
        final Map<UUID, JsonObject> courtroomsById = buildCourtroomsByIdMap(courtCentreJson);
        final Map<UUID, JsonObject> prosecutorDetailsById = new HashMap<>();
        final Map<UUID, UUID> prosecutorIdByCaseId = new HashMap<>();
        final Map<UUID, AssociatedDefenceOrganisation> defenceOrganisationByDefendantId = new HashMap<>();
        final Map<UUID, JsonObject> caseContactsByCaseId = new HashMap<>();

        courtListPublished.getCourtLists().forEach(courtList -> {
            final String courtroomName = isNull(courtList.getCourtRoomId()) ? null : courtroomsById.get(courtList.getCourtRoomId()).getString("courtroomName");

            courtList.getHearings().stream().filter(hearing ->nonNull(hearing.getCaseIdentifier())).forEach(hearing -> {

                final String hearingType = hearing.getHearingType().getDescription();
                final String caseUrn = hearing.getCaseIdentifier().getCaseReference();

                for (final Defendant defendant : hearing.getDefendants()) {
                    processNotificationForDefendantObject(event, courtListPublished, defenceOrganisationPayloadBuilderByName, prosecutionPayloadBuilderByName, defenceAdvocatePayloadBuilderByName, publishCourtListType, courtCentreJson, ljaDetails, prosecutionCasesByCaseId,
                            prosecutorDetailsById, prosecutorIdByCaseId, defenceOrganisationByDefendantId, caseContactsByCaseId, courtList, courtroomName, hearing, hearingType, caseUrn, defendant);
                }
            });
        });
    }

    private void processNotificationForDefendantObject(final JsonEnvelope event, final CourtListPublished courtListPublished, final Map<String, PublishCourtListPayloadBuilder> defenceOrganisationPayloadBuilderByName, final Map<String, PublishCourtListPayloadBuilder> prosecutionPayloadBuilderByName,
                                                       final Map<String, PublishCourtListPayloadBuilder> defenceAdvocatePayloadBuilderByName, final PublishCourtListType publishCourtListType, final JsonObject courtCentreJson, final LjaDetails ljaDetails,
                                                       final Map<UUID, ProsecutionCase> prosecutionCasesByCaseId, final Map<UUID, JsonObject> prosecutorDetailsById, final Map<UUID, UUID> prosecutorIdByCaseId, final Map<UUID, AssociatedDefenceOrganisation> defenceOrganisationByDefendantId,
                                                       final Map<UUID, JsonObject> caseContactsByCaseId, final CourtLists courtList, final String courtroomName, final Hearings hearing, final String hearingType, final String caseUrn, final uk.gov.justice.core.courts.Defendant defendant) {
        final UUID caseId = defendant.getProsecutionCaseId();

        if (prosecutorNeedsToBeNotified(publishCourtListType)) {
            prosecutionCasesByCaseId.computeIfAbsent(caseId, key -> getProsecutionCaseByCaseId(event, key));
            final UUID prosecutorId = getProsecutorIdForCase(prosecutionCasesByCaseId.get(caseId));
            prosecutorDetailsById.computeIfAbsent(prosecutorId, key -> getProsecutorDetailsById(event, key));
            prosecutorIdByCaseId.put(caseId, prosecutorId);
        }

        final PublishCourtListDefendantBuilder defendantBuilder = publishCourtListDefendantBuilder().withDefendantName(getDefendantFullName(defendant))
                .withDefendantLastName(getDefendantLastName(defendant));
        getDefendantDateOfBirth(defendant).ifPresent(dateOfBirth -> defendantBuilder.withDefendantDateOfBirth(LocalDates.to(dateOfBirth)));
        final PublishCourtListHearingBuilder hearingBuilder = publishCourtListHearingBuilder()
                .withHearingType(hearingType)
                .withCaseReference(caseUrn)
                .addDefendant(defendantBuilder.build());

        final UUID defendantId = defendant.getId();
        defenceOrganisationByDefendantId.computeIfAbsent(defendantId, key -> defenceService.getDefenceOrganisationByDefendantId(event, key));
        if (defenceOrganisationByDefendantId.containsKey(defendantId)) {
            final AssociatedDefenceOrganisation defenceOrganisation = defenceOrganisationByDefendantId.get(defendantId);
            final PublishCourtListAddressee addressee = publishCourtListAddresseeBuilder().withName(defenceOrganisation.getOrganisationName())
                    .withEmail(defenceOrganisation.getEmail())
                    .withAddress(getDefenceOrganisationAddress(defenceOrganisation))
                    .build();

            buildPublishCourtListDocumentPayload(courtListPublished, publishCourtListType, defenceOrganisationPayloadBuilderByName, courtCentreJson, ljaDetails,
                    addressee, courtList, courtroomName, hearing, hearingBuilder.build());

            caseContactsByCaseId.computeIfAbsent(defendant.getProsecutionCaseId(), key -> correspondenceService.getCaseContacts(event, key));
            final Optional<JsonObject> matchedCaseContact = findMatchedDefendantAdvocate(defendant, caseContactsByCaseId);
            buildDefenceAdvocateNotifications(courtListPublished, defenceAdvocatePayloadBuilderByName, publishCourtListType, courtCentreJson, ljaDetails, courtList, courtroomName, hearing, hearingBuilder, matchedCaseContact);

        }
        if ((WARN.equals(publishCourtListType) || FIRM.equals(publishCourtListType)) && !isProsecutorCps(prosecutorDetailsById, prosecutorIdByCaseId.get(caseId))) {
            final JsonObject prosecutor = prosecutorDetailsById.get(prosecutorIdByCaseId.get(caseId));
            final PublishCourtListAddressee addressee = publishCourtListAddresseeBuilder().withName(prosecutor.getString("fullName"))
                    .withEmail(prosecutor.getString(CONTACT_EMAIL_ADDRESS, EMPTY))
                    .withAddress(getProsecutorAddress(prosecutor))
                    .build();

            buildPublishCourtListDocumentPayload(courtListPublished, publishCourtListType, prosecutionPayloadBuilderByName, courtCentreJson, ljaDetails,
                    addressee, courtList, courtroomName, hearing, hearingBuilder.build());
        }
    }

    private void buildDefenceAdvocateNotifications(final CourtListPublished courtListPublished, final Map<String, PublishCourtListPayloadBuilder> defenceAdvocatePayloadBuilderByName, final PublishCourtListType publishCourtListType,
                                                   final JsonObject courtCentreJson, final LjaDetails ljaDetails, final CourtLists courtList, final String courtroomName, final Hearings hearing,
                                                   final PublishCourtListHearingBuilder hearingBuilder, final Optional<JsonObject> matchedCaseContact) {

        if (!matchedCaseContact.isPresent()) {
            return;
        }
        final JsonObject caseContact = matchedCaseContact.get();
        final PublishCourtListAddressee.PublishCourtListAddresseeBuilder defenceAdvocateAddresseeBuilder = publishCourtListAddresseeBuilder()
                .withName(isNotEmpty(caseContact.getString(CONTACT_PERSON_NAME, null)) ? caseContact.getString(CONTACT_PERSON_NAME) : caseContact.getString(CONTACT_ORGANISATION_NAME))
                .withEmail(caseContact.getString(PRIMARY_EMAIL, null));
        if (caseContact.containsKey(ADDRESS)) {
            final JsonObject jsonAddress = caseContact.getJsonObject(ADDRESS);

            final PublishCourtListAddress address = publishCourtListAddressBuilder()
                    .withLine1(jsonAddress.getString(ADDRESS_1, null))
                    .withLine2(jsonAddress.getString(ADDRESS_2, null))
                    .withLine3(jsonAddress.getString(ADDRESS_3, null))
                    .withLine4(jsonAddress.getString(ADDRESS_4, null))
                    .withPostCode(jsonAddress.getString(POSTCODE, null))
                    .build();
            defenceAdvocateAddresseeBuilder.withAddress(address);
        }

        buildPublishCourtListDocumentPayload(courtListPublished, publishCourtListType, defenceAdvocatePayloadBuilderByName, courtCentreJson, ljaDetails,
                defenceAdvocateAddresseeBuilder.build(), courtList, courtroomName, hearing, hearingBuilder.build());
    }

    private Optional<JsonObject> findMatchedDefendantAdvocate(final Defendant defendant, final Map<UUID, JsonObject> caseContactsByCaseId) {
        if (caseContactsByCaseId.containsKey(defendant.getProsecutionCaseId())) {
            final JsonObject caseContacts = caseContactsByCaseId.get(defendant.getProsecutionCaseId());
            return caseContacts.getJsonArray(CONTACTS).getValuesAs(JsonObject.class).stream()
                    .filter(caseContact -> DEFENCE_ADVOCATE.equalsIgnoreCase(caseContact.getJsonObject(CONTACT_TYPE).getString(CONTACT_TYPE_MAPPING)))
                    .filter(caseContact -> checkIfDefendantIdPresent(defendant.getId().toString(), caseContact.getJsonArray(DEFENDANT_IDS)))
                    .findAny();
        }
        return Optional.empty();
    }

    private boolean checkIfDefendantIdPresent(final String defendantId, final JsonArray defendantIdsArray) {
        final List<String> defendantIdList = new ArrayList<>();
        if (CollectionUtils.isNotEmpty(defendantIdsArray)) {
            for (int i = 0; i < defendantIdsArray.size(); i++) {
                defendantIdList.add(defendantIdsArray.getString(i));
            }
        }
        return defendantIdList.contains(defendantId);
    }

    private void buildPublishCourtListDocumentPayload(final CourtListPublished courtListPublished, final PublishCourtListType publishCourtListType,
                                                      final Map<String, PublishCourtListPayloadBuilder> payloadBuilderByAddresseeName, final JsonObject courtCentreJson,
                                                      final LjaDetails ljaDetails, final PublishCourtListAddressee addressee, final CourtLists courtList,
                                                      final String courtroomName, final Hearings hearing, final PublishCourtListHearing publishCourtListHearing) {
        final PublishCourtListPayloadBuilder payloadBuilder = buildWithAddresseeAndCourtDetailsIfNotExists(courtListPublished, publishCourtListType, payloadBuilderByAddresseeName, courtCentreJson, ljaDetails, addressee);

        if (TRUE.equals(hearing.getWeekCommencing())) {
            buildHearingWithWeekCommencingDate(courtListPublished, courtroomName, publishCourtListHearing, payloadBuilder);
        } else {
            buildHearingWithFixedDate(courtList, courtroomName, hearing, publishCourtListHearing, payloadBuilder);
        }
    }

    private PublishCourtListPayloadBuilder buildWithAddresseeAndCourtDetailsIfNotExists(final CourtListPublished courtListPublished, final PublishCourtListType publishCourtListType,
                                                                                        final Map<String, PublishCourtListPayloadBuilder> payloadBuilderByAddresseeName, final JsonObject courtCentreJson,
                                                                                        final LjaDetails ljaDetails, final PublishCourtListAddressee addressee) {
        final String addresseeName = isNotEmpty(addressee.getEmail()) ? addressee.getEmail() : addressee.getName();

        if (!payloadBuilderByAddresseeName.containsKey(addresseeName)) {
            final PublishCourtListPayloadBuilder payloadBuilder = publishCourtListPayloadBuilder()
                    .withPublishCourtListType(publishCourtListType)
                    .withIssueDate(courtListPublished.getRequestedTime().toLocalDate().toString())
                    .withLjaCode(ljaDetails.getLjaCode())
                    .withLjaName(ljaDetails.getLjaName())
                    .withLjaNameWelsh(ljaDetails.getWelshLjaName())
                    .withCourtName(courtCentreJson.getString("oucodeL3Name", EMPTY))
                    .withCourtNameWelsh(courtCentreJson.getString("oucodeL3WelshName", EMPTY))
                    .withCourtAddress(buildCourtCentreAddress(courtCentreJson));

            if (TRUE.equals(courtListPublished.getWeekCommencing())) {
                payloadBuilder.withIsWeekCommencing(true)
                        .withWeekCommencingStartDate(courtListPublished.getStartDate())
                        .withWeekCommencingEndDate(courtListPublished.getEndDate());
            } else {
                payloadBuilder.withListDate(courtListPublished.getStartDate());
            }
            payloadBuilder.withAddressee(addressee);

            payloadBuilderByAddresseeName.put(addresseeName, payloadBuilder);
            return payloadBuilder;
        }
        return payloadBuilderByAddresseeName.get(addresseeName);
    }

    private void buildHearingWithFixedDate(final CourtLists courtList, final String courtroomName, final Hearings hearing,
                                           final PublishCourtListHearing publishCourtListHearing, final PublishCourtListPayloadBuilder payloadBuilder) {
        final String hearingDate = courtList.getSittingDate();
        final String hearingTime = getHearingTime(hearing.getStartTime());
        if (nonNull(courtroomName)) {
            buildHearingWithFixedDateAndCourtroomAllocated(courtroomName, publishCourtListHearing, payloadBuilder, hearingDate, hearingTime);
            return;
        }
        buildHearingWithFixedDateAndNoCourtroomAllocated(publishCourtListHearing, payloadBuilder, hearingDate, hearingTime);
    }

    private void buildHearingWithFixedDateAndCourtroomAllocated(final String courtroomName, final PublishCourtListHearing publishCourtListHearing,
                                                                final PublishCourtListPayloadBuilder payloadBuilder, final String hearingDate, final String hearingTime) {
        if (isNull(payloadBuilder.getHearingByDates()) || isNull(payloadBuilder.getHearingByDate(hearingDate))) {
            payloadBuilder
                    .addHearingByDate(publishCourtListHearingByDateBuilder()
                            .withHearingDate(hearingDate)
                            .addHearingByCourtroom(publishCourtListHearingByCourtroomBuilder()
                                    .withCourtroomName(courtroomName)
                                    .addHearingByTime(publishCourtListHearingByTimeBuilder()
                                            .withHearingTime(hearingTime)
                                            .addHearing(publishCourtListHearingBuilder()
                                                    .withCaseReference(publishCourtListHearing.getCaseReference())
                                                    .withHearingType(publishCourtListHearing.getHearingType())
                                                    .addDefendant(publishCourtListDefendantBuilder()
                                                            .withDefendantName(publishCourtListHearing.getDefendants().get(0).getDefendantName())
                                                            .withDefendantLastName(publishCourtListHearing.getDefendants().get(0).getDefendantLastName())
                                                            .withDefendantDateOfBirth(publishCourtListHearing.getDefendants().get(0).getDefendantDateOfBirth())
                                                            .build())
                                                    .build())
                                            .build())
                                    .build())
                            .build());
        } else if (isNull(payloadBuilder.getHearingByDate(hearingDate).getHearingByCourtroom(courtroomName))) {
            payloadBuilder.getHearingByDate(hearingDate)
                    .addHearingByCourtroom(publishCourtListHearingByCourtroomBuilder()
                            .withCourtroomName(courtroomName)
                            .addHearingByTime(publishCourtListHearingByTimeBuilder()
                                    .withHearingTime(hearingTime)
                                    .addHearing(publishCourtListHearingBuilder()
                                            .withCaseReference(publishCourtListHearing.getCaseReference())
                                            .withHearingType(publishCourtListHearing.getHearingType())
                                            .addDefendant(publishCourtListDefendantBuilder()
                                                    .withDefendantName(publishCourtListHearing.getDefendants().get(0).getDefendantName())
                                                    .withDefendantLastName(publishCourtListHearing.getDefendants().get(0).getDefendantLastName())
                                                    .withDefendantDateOfBirth(publishCourtListHearing.getDefendants().get(0).getDefendantDateOfBirth())
                                                    .build())
                                            .build())
                                    .build())
                            .build());
        } else if (isNull(payloadBuilder.getHearingByDate(hearingDate).getHearingByCourtroom(courtroomName).getHearingByTime(hearingTime))) {
            payloadBuilder.getHearingByDate(hearingDate).getHearingByCourtroom(courtroomName)
                    .addHearingByTime(publishCourtListHearingByTimeBuilder()
                            .withHearingTime(hearingTime)
                            .addHearing(publishCourtListHearingBuilder()
                                    .withCaseReference(publishCourtListHearing.getCaseReference())
                                    .withHearingType(publishCourtListHearing.getHearingType())
                                    .addDefendant(publishCourtListDefendantBuilder()
                                            .withDefendantName(publishCourtListHearing.getDefendants().get(0).getDefendantName())
                                            .withDefendantLastName(publishCourtListHearing.getDefendants().get(0).getDefendantLastName())
                                            .withDefendantDateOfBirth(publishCourtListHearing.getDefendants().get(0).getDefendantDateOfBirth())
                                            .build())
                                    .build())
                            .build());
        } else if (isNull(payloadBuilder.getHearingByDate(hearingDate).getHearingByCourtroom(courtroomName).getHearingByTime(hearingTime).getHearing(publishCourtListHearing.getCaseReference(), publishCourtListHearing.getHearingType()))) {
            payloadBuilder.getHearingByDate(hearingDate).getHearingByCourtroom(courtroomName).getHearingByTime(hearingTime)
                    .addHearing(publishCourtListHearingBuilder()
                            .withCaseReference(publishCourtListHearing.getCaseReference())
                            .withHearingType(publishCourtListHearing.getHearingType())
                            .addDefendant(publishCourtListDefendantBuilder()
                                    .withDefendantName(publishCourtListHearing.getDefendants().get(0).getDefendantName())
                                    .withDefendantLastName(publishCourtListHearing.getDefendants().get(0).getDefendantLastName())
                                    .withDefendantDateOfBirth(publishCourtListHearing.getDefendants().get(0).getDefendantDateOfBirth())
                                    .build())
                            .build());
        } else {
            payloadBuilder.getHearingByDate(hearingDate).getHearingByCourtroom(courtroomName).getHearingByTime(hearingTime)
                    .getHearing(publishCourtListHearing.getCaseReference(), publishCourtListHearing.getHearingType())
                    .addDefendant(publishCourtListDefendantBuilder()
                            .withDefendantName(publishCourtListHearing.getDefendants().get(0).getDefendantName())
                            .withDefendantLastName(publishCourtListHearing.getDefendants().get(0).getDefendantLastName())
                            .withDefendantDateOfBirth(publishCourtListHearing.getDefendants().get(0).getDefendantDateOfBirth())
                            .build());
        }
    }

    private void buildHearingWithFixedDateAndNoCourtroomAllocated(final PublishCourtListHearing publishCourtListHearing, final PublishCourtListPayloadBuilder payloadBuilder,
                                                                  final String hearingDate, final String hearingTime) {
        if (isNull(payloadBuilder.getHearingByDatesNoCourtroom()) || isNull(payloadBuilder.getHearingByDateNoCourtroom(hearingDate))) {
            payloadBuilder.addHearingByDateNoCourtroom(
                    publishCourtListHearingByDateBuilder()
                            .withHearingDate(hearingDate)
                            .addHearingByCourtroom(publishCourtListHearingByCourtroomBuilder()
                                    .addHearingByTime(publishCourtListHearingByTimeBuilder()
                                            .withHearingTime(hearingTime)
                                            .addHearing(publishCourtListHearingBuilder()
                                                    .withCaseReference(publishCourtListHearing.getCaseReference())
                                                    .withHearingType(publishCourtListHearing.getHearingType())
                                                    .addDefendant(publishCourtListDefendantBuilder()
                                                            .withDefendantName(publishCourtListHearing.getDefendants().get(0).getDefendantName())
                                                            .withDefendantLastName(publishCourtListHearing.getDefendants().get(0).getDefendantLastName())
                                                            .withDefendantDateOfBirth(publishCourtListHearing.getDefendants().get(0).getDefendantDateOfBirth())
                                                            .build())
                                                    .build())
                                            .build())
                                    .build())
                            .build());
        } else if (isNull(payloadBuilder.getHearingByDateNoCourtroom(hearingDate).getHearingByCourtrooms().get(0).getHearingByTime(hearingTime))) {
            payloadBuilder.getHearingByDateNoCourtroom(hearingDate).getHearingByCourtrooms().get(0)
                    .addHearingByTime(publishCourtListHearingByTimeBuilder()
                            .withHearingTime(hearingTime)
                            .addHearing(publishCourtListHearingBuilder()
                                    .withCaseReference(publishCourtListHearing.getCaseReference())
                                    .withHearingType(publishCourtListHearing.getHearingType())
                                    .addDefendant(publishCourtListDefendantBuilder()
                                            .withDefendantName(publishCourtListHearing.getDefendants().get(0).getDefendantName())
                                            .withDefendantLastName(publishCourtListHearing.getDefendants().get(0).getDefendantLastName())
                                            .withDefendantDateOfBirth(publishCourtListHearing.getDefendants().get(0).getDefendantDateOfBirth())
                                            .build())
                                    .build())
                            .build());
        } else if (isNull(payloadBuilder.getHearingByDateNoCourtroom(hearingDate).getHearingByCourtrooms().get(0).getHearingByTime(hearingTime).getHearing(publishCourtListHearing.getCaseReference(), publishCourtListHearing.getHearingType()))) {
            payloadBuilder.getHearingByDateNoCourtroom(hearingDate).getHearingByCourtrooms().get(0).getHearingByTime(hearingTime)
                    .addHearing(publishCourtListHearingBuilder()
                            .withCaseReference(publishCourtListHearing.getCaseReference())
                            .withHearingType(publishCourtListHearing.getHearingType())
                            .addDefendant(publishCourtListDefendantBuilder()
                                    .withDefendantName(publishCourtListHearing.getDefendants().get(0).getDefendantName())
                                    .withDefendantLastName(publishCourtListHearing.getDefendants().get(0).getDefendantLastName())
                                    .withDefendantDateOfBirth(publishCourtListHearing.getDefendants().get(0).getDefendantDateOfBirth())
                                    .build())
                            .build());
        } else {
            payloadBuilder.getHearingByDateNoCourtroom(hearingDate).getHearingByCourtrooms().get(0).getHearingByTime(hearingTime)
                    .getHearing(publishCourtListHearing.getCaseReference(), publishCourtListHearing.getHearingType())
                    .addDefendant(publishCourtListDefendantBuilder()
                            .withDefendantName(publishCourtListHearing.getDefendants().get(0).getDefendantName())
                            .withDefendantLastName(publishCourtListHearing.getDefendants().get(0).getDefendantLastName())
                            .withDefendantDateOfBirth(publishCourtListHearing.getDefendants().get(0).getDefendantDateOfBirth())
                            .build());
        }
    }

    private void buildHearingWithWeekCommencingDate(final CourtListPublished courtListPublished, final String courtroomName,
                                                    final PublishCourtListHearing publishCourtListHearing, final PublishCourtListPayloadBuilder payloadBuilder) {
        if (nonNull(courtroomName)) {
            buildHearingWithWeekCommencingDateAndCourtroomAllocated(courtListPublished, courtroomName, publishCourtListHearing, payloadBuilder);
            return;
        }
        buildHearingWithWeekCommencingDateAndNoCourtroomAllocated(courtListPublished, publishCourtListHearing, payloadBuilder);
    }

    private void buildHearingWithWeekCommencingDateAndCourtroomAllocated(final CourtListPublished courtListPublished, final String courtroomName, final PublishCourtListHearing publishCourtListHearing, final PublishCourtListPayloadBuilder payloadBuilder) {
        if (isNull(payloadBuilder.getHearingByWeekCommencingDate())) {

            final PublishCourtListByWeekCommencingDate hearingByWeekCommencingDate =
                    publishCourtListHearingByWeekCommencingDateBuilder()
                            .withWeekCommencingStartDate(courtListPublished.getStartDate())
                            .withWeekCommencingEndDate(courtListPublished.getEndDate())
                            .addHearingByCourtroom(publishCourtListForWeekCommencingByCourtroomBuilder()
                                    .withCourtroomName(courtroomName)
                                    .addHearing(publishCourtListHearingBuilder()
                                            .withCaseReference(publishCourtListHearing.getCaseReference())
                                            .withHearingType(publishCourtListHearing.getHearingType())
                                            .addDefendant(publishCourtListDefendantBuilder()
                                                    .withDefendantName(publishCourtListHearing.getDefendants().get(0).getDefendantName())
                                                    .withDefendantLastName(publishCourtListHearing.getDefendants().get(0).getDefendantLastName())
                                                    .withDefendantDateOfBirth(publishCourtListHearing.getDefendants().get(0).getDefendantDateOfBirth())
                                                    .build())
                                            .build())
                                    .build())
                            .build();
            payloadBuilder.withHearingByWeekCommencingDate(hearingByWeekCommencingDate);
        } else if (isNull(payloadBuilder.getHearingByWeekCommencingDate().getHearingByCourtroom(courtroomName))) {

            payloadBuilder.getHearingByWeekCommencingDate().addHearingByCourtroom(
                    publishCourtListForWeekCommencingByCourtroomBuilder()
                            .withCourtroomName(courtroomName)
                            .addHearing(publishCourtListHearingBuilder()
                                    .withCaseReference(publishCourtListHearing.getCaseReference())
                                    .withHearingType(publishCourtListHearing.getHearingType())
                                    .addDefendant(publishCourtListDefendantBuilder()
                                            .withDefendantName(publishCourtListHearing.getDefendants().get(0).getDefendantName())
                                            .withDefendantLastName(publishCourtListHearing.getDefendants().get(0).getDefendantLastName())
                                            .withDefendantDateOfBirth(publishCourtListHearing.getDefendants().get(0).getDefendantDateOfBirth())
                                            .build())
                                    .build())
                            .build());
        } else if (isNull(payloadBuilder.getHearingByWeekCommencingDate().getHearingByCourtroom(courtroomName).getHearing(publishCourtListHearing.getCaseReference(), publishCourtListHearing.getHearingType()))) {
            payloadBuilder.getHearingByWeekCommencingDate().getHearingByCourtroom(courtroomName)
                    .addHearing(publishCourtListHearingBuilder()
                            .withCaseReference(publishCourtListHearing.getCaseReference())
                            .withHearingType(publishCourtListHearing.getHearingType())
                            .addDefendant(publishCourtListDefendantBuilder()
                                    .withDefendantName(publishCourtListHearing.getDefendants().get(0).getDefendantName())
                                    .withDefendantLastName(publishCourtListHearing.getDefendants().get(0).getDefendantLastName())
                                    .withDefendantDateOfBirth(publishCourtListHearing.getDefendants().get(0).getDefendantDateOfBirth())
                                    .build())
                            .build());
        } else {
            payloadBuilder.getHearingByWeekCommencingDate().getHearingByCourtroom(courtroomName).getHearing(publishCourtListHearing.getCaseReference(), publishCourtListHearing.getHearingType())
                    .addDefendant(publishCourtListDefendantBuilder()
                            .withDefendantName(publishCourtListHearing.getDefendants().get(0).getDefendantName())
                            .withDefendantLastName(publishCourtListHearing.getDefendants().get(0).getDefendantLastName())
                            .withDefendantDateOfBirth(publishCourtListHearing.getDefendants().get(0).getDefendantDateOfBirth())
                            .build());
        }
    }

    private void buildHearingWithWeekCommencingDateAndNoCourtroomAllocated(final CourtListPublished courtListPublished, final PublishCourtListHearing publishCourtListHearing,
                                                                           final PublishCourtListPayloadBuilder payloadBuilder) {
        if (isNull(payloadBuilder.getHearingByWeekCommencingDateNoCourtroom())) {
            final PublishCourtListByWeekCommencingDate hearingByWeekCommencingDate =
                    publishCourtListHearingByWeekCommencingDateBuilder()
                            .withWeekCommencingStartDate(courtListPublished.getStartDate())
                            .withWeekCommencingEndDate(courtListPublished.getEndDate())
                            .addHearingByCourtroom(publishCourtListForWeekCommencingByCourtroomBuilder()
                                    .addHearing(publishCourtListHearingBuilder()
                                            .withCaseReference(publishCourtListHearing.getCaseReference())
                                            .withHearingType(publishCourtListHearing.getHearingType())
                                            .addDefendant(publishCourtListDefendantBuilder()
                                                    .withDefendantName(publishCourtListHearing.getDefendants().get(0).getDefendantName())
                                                    .withDefendantLastName(publishCourtListHearing.getDefendants().get(0).getDefendantLastName())
                                                    .withDefendantDateOfBirth(publishCourtListHearing.getDefendants().get(0).getDefendantDateOfBirth())
                                                    .build())
                                            .build())
                                    .build())
                            .build();
            payloadBuilder.withHearingByWeekCommencingDateNoCourtroom(hearingByWeekCommencingDate);
        } else if (isNull(payloadBuilder.getHearingByWeekCommencingDateNoCourtroom().getHearingByCourtrooms().get(0).getHearing(publishCourtListHearing.getCaseReference(), publishCourtListHearing.getHearingType()))) {
            payloadBuilder.getHearingByWeekCommencingDateNoCourtroom().getHearingByCourtrooms().get(0)
                    .addHearing(publishCourtListHearingBuilder()
                            .withCaseReference(publishCourtListHearing.getCaseReference())
                            .withHearingType(publishCourtListHearing.getHearingType())
                            .addDefendant(publishCourtListDefendantBuilder()
                                    .withDefendantName(publishCourtListHearing.getDefendants().get(0).getDefendantName())
                                    .withDefendantLastName(publishCourtListHearing.getDefendants().get(0).getDefendantLastName())
                                    .withDefendantDateOfBirth(publishCourtListHearing.getDefendants().get(0).getDefendantDateOfBirth())
                                    .build())
                            .build());
        } else {
            payloadBuilder.getHearingByWeekCommencingDateNoCourtroom().getHearingByCourtrooms().get(0).getHearing(publishCourtListHearing.getCaseReference(), publishCourtListHearing.getHearingType())
                    .addDefendant(publishCourtListDefendantBuilder()
                            .withDefendantName(publishCourtListHearing.getDefendants().get(0).getDefendantName())
                            .withDefendantLastName(publishCourtListHearing.getDefendants().get(0).getDefendantLastName())
                            .withDefendantDateOfBirth(publishCourtListHearing.getDefendants().get(0).getDefendantDateOfBirth())
                            .build());
        }
    }

    private LjaDetails buildLjaDetails(final JsonObject enforcementArea) {
        final JsonObject localJusticeArea = enforcementArea.getJsonObject(LOCAL_JUSTICE_AREA);

        return ljaDetails()
                .withLjaCode(ofNullable(localJusticeArea).map(area -> area.getString(NATIONAL_COURT_CODE, EMPTY)).orElse(EMPTY))
                .withLjaName(ofNullable(localJusticeArea).map(area -> area.getString(NAME, EMPTY)).orElse(EMPTY))
                .withWelshLjaName(ofNullable(localJusticeArea).map(area -> area.getString(NAME_WELSH, EMPTY)).orElse(EMPTY))
                .build();
    }

    private Map<UUID, JsonObject> buildCourtroomsByIdMap(final JsonObject courtCentre) {
        final Map<UUID, JsonObject> courtroomsById = new HashMap<>();
        courtCentre.getJsonArray("courtrooms").getValuesAs(JsonObject.class).forEach(courtroom -> courtroomsById.put(fromString(courtroom.getString("id")), courtroom));
        return courtroomsById;
    }

    private ProsecutionCase getProsecutionCaseByCaseId(final JsonEnvelope envelope, final UUID caseId) {
        final JsonObject caseJsonObject = progressionService.getProsecutionCase(envelope, caseId.toString()).orElseThrow(() -> new IllegalArgumentException(format("Case details not found for case id %s", caseId)));
        return jsonObjectToObjectConverter.convert(caseJsonObject.getJsonObject("prosecutionCase"), ProsecutionCase.class);
    }

    private UUID getProsecutorIdForCase(final ProsecutionCase prosecutionCase) {
        final UUID prosecutorId;
        if (nonNull(prosecutionCase.getProsecutor()) && nonNull(prosecutionCase.getProsecutor().getProsecutorId())) {
            prosecutorId = prosecutionCase.getProsecutor().getProsecutorId();
        } else if (nonNull(prosecutionCase.getProsecutionCaseIdentifier()) && nonNull(prosecutionCase.getProsecutionCaseIdentifier().getProsecutionAuthorityId())) {
            prosecutorId = prosecutionCase.getProsecutionCaseIdentifier().getProsecutionAuthorityId();
        } else {
            throw new IllegalArgumentException(format("Could not find prosecutor id for case with id %s", prosecutionCase.getId()));
        }
        return prosecutorId;
    }

    private JsonObject getProsecutorDetailsById(final JsonEnvelope envelope, final UUID prosecutorId) {
        return referenceDataService.getProsecutor(envelope, prosecutorId, requester)
                .orElseThrow(() -> new IllegalArgumentException(format("Prosecutor details not found in reference data for prosecutor id %s", prosecutorId)));
    }

    private PublishCourtListAddress getDefenceOrganisationAddress(final AssociatedDefenceOrganisation defenceOrganisation) {
        return publishCourtListAddressBuilder()
                .withLine1(defenceOrganisation.getAddress().getAddress1())
                .withLine2(defenceOrganisation.getAddress().getAddress2())
                .withLine3(defenceOrganisation.getAddress().getAddress3())
                .withLine4(defenceOrganisation.getAddress().getAddress4())
                .withPostCode(defenceOrganisation.getAddress().getAddressPostcode())
                .build();
    }

    private PublishCourtListAddress getProsecutorAddress(final JsonObject prosecutor) {
        if (prosecutor.containsKey(ADDRESS)) {
            final JsonObject prosecutorAddress = prosecutor.getJsonObject(ADDRESS);
            return publishCourtListAddressBuilder()
                    .withLine1(prosecutorAddress.getString(ADDRESS_1))
                    .withLine2(prosecutorAddress.getString(ADDRESS_2, EMPTY))
                    .withLine3(prosecutorAddress.getString(ADDRESS_3, EMPTY))
                    .withLine4(prosecutorAddress.getString(ADDRESS_4, EMPTY))
                    .withLine5(prosecutorAddress.getString(ADDRESS_5, EMPTY))
                    .withPostCode(prosecutorAddress.getString(POSTCODE))
                    .build();
        }
        return null;
    }

    private String getHearingTime(final String hearingDateTime) {
        try {
            return nonNull(hearingDateTime) ? TIME_FORMATTER.format(LOCAL_DATE_TIME_FORMATTER.parse(hearingDateTime)) : DEFAULT_HEARING_START_TIME;
        } catch (final DateTimeException dte) {
            throw new InvalidHearingTimeException(format("Invalid hearing date time [ %s ] for generating publish court list notification ", hearingDateTime), dte);
        }
    }

    private PublishCourtListCourtAddress buildCourtCentreAddress(final JsonObject courtCentre) {
        final PublishCourtListCourtAddressBuilder builder = publishCourtListCourtAddressBuilder()
                .withLine1(courtCentre.getString(ADDRESS_1, EMPTY))
                .withLine2(courtCentre.getString(ADDRESS_2, EMPTY))
                .withLine3(courtCentre.getString(ADDRESS_3, EMPTY))
                .withLine4(courtCentre.getString(ADDRESS_4, EMPTY))
                .withLine5(courtCentre.getString(ADDRESS_5, EMPTY))
                .withPostCode(courtCentre.getString(POSTCODE, EMPTY));
        if (courtCentre.getBoolean("isWelsh", false)) {
            builder.withIsWelsh(true)
                    .withLine1Welsh(courtCentre.getString("welshAddress1", EMPTY))
                    .withLine2Welsh(courtCentre.getString("welshAddress2", EMPTY))
                    .withLine3Welsh(courtCentre.getString("welshAddress3", EMPTY))
                    .withLine4Welsh(courtCentre.getString("welshAddress4", EMPTY))
                    .withLine5Welsh(courtCentre.getString("welshAddress5", EMPTY));
        }

        return builder.build();
    }

    private boolean prosecutorNeedsToBeNotified(final PublishCourtListType publishCourtListType) {
        return WARN.equals(publishCourtListType) || FIRM.equals(publishCourtListType);
    }

    private static boolean isProsecutorCps(final Map<UUID, JsonObject> prosecutorDetailsById, final UUID prosecutorId) {
        return prosecutorDetailsById.get(prosecutorId).getBoolean("cpsFlag", false);
    }

}
